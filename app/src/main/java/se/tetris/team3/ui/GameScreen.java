package se.tetris.team3.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.core.Settings;


public class GameScreen implements Screen {

    private final AppFrame app;
    private final Settings settings;
    private final GameManager manager;
    private Timer timer;
    private int blockSize;

    private int gameOverSelection = 0;
    private final String[] gameOverOptions = {"다시하기", "나가기"};
    private Font gameOverOptionFont = new Font("SansSerif", Font.PLAIN, 24);


    private static final int REGION_COLS = 10;
    private static final int REGION_ROWS = 20;

    private Block lastBlockRef = null;

    public GameScreen(AppFrame app) {
        this(app, new GameManager());
    }

    public GameScreen(AppFrame app, GameManager manager) {
        this.app = app;
        this.manager = manager;
        this.settings = app.getSettings();
    }

    @Override public void onShow() {
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!manager.isGameOver()) {
                    manager.stepDownOrFix();
                } else {
                    if (timer != null) timer.stop();
                }
                app.repaint();
            }
        });
        timer.start();

    }
    @Override public void onHide() {
        if (timer != null) timer.stop();
    }

    /** Checks if shape fully inside 10x20 region */
    private boolean fitsRegion(int gx, int gy, int[][] shape) {
        if (shape == null) return false;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int x = gx + c, y = gy + r;
                    if (x < 0 || x >= REGION_COLS) return false;
                    if (y < 0 || y >= REGION_ROWS) return false;
                }
            }
        }
        return true;
    }

    /** bottom-most row index of shape */
    private static int bottomRowIndex(int[][] shape) {
        for (int r = shape.length - 1; r >= 0; r--) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) return r;
            }
        }
        return shape.length - 1;
    }

    /** Align spawn so that bottom touches frame top (y=0 line) */

    // **** i블록 누워있을때는 되고 s나z처럼 최소 높이가 2칸 이상인 블록은 한칸이 남아도 게임오버가 된다. 이걸 수정해야 하는데.... (기존 메인에도 있는거 같음)
    private void alignSpawnIfNewBlock() {
        Block cur = manager.getCurrentBlock();
        if (cur == null || cur == lastBlockRef) return;

        int[][] s = cur.getShape();
        int w = s[0].length;
        int br = bottomRowIndex(s);

        int x = manager.getBlockX();
        int minX = 0, maxX = Math.max(0, REGION_COLS - w);
        if (x < minX || x > maxX) x = (REGION_COLS - w) / 2;

        int y = -br;
        manager.tryMove(x, y);
        lastBlockRef = cur;
    }

    @Override
    public void render(Graphics2D g2) {
        int blockSize = settings.resolveBlockSize();
        int padding = 18;

        // background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, app.getWidth(), app.getHeight());

        // frame
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(padding, padding, blockSize * 10, blockSize * 20);
        g2.setStroke(new BasicStroke(1));

        // align spawn
        alignSpawnIfNewBlock();

        // fixed blocks
        for (int r = 0; r < REGION_ROWS; r++) {
            for (int c = 0; c < REGION_COLS; c++) {
                int colorValue = manager.getFieldValue(r, c);
                if (colorValue != 0) {
                    //Color color = new Color(colorValue, true);
                    int x = padding + c * blockSize;
                    int y = padding + r * blockSize;
                    PatternPainter.drawCell(g2, x, y, blockSize, Color.GRAY, null, settings.isColorBlindMode());
                }
            }
        }

        // current block
        if (!manager.isGameOver()) {
            Block cur = manager.getCurrentBlock();
            if (cur != null) {
                int[][] shape = cur.getShape();
                Color base = cur.getColor();
                int bx = manager.getBlockX();
                int by = manager.getBlockY();
                for (int r = 0; r < shape.length; r++) {
                    for (int c = 0; c < shape[r].length; c++) {
                        if (shape[r][c] != 0) {
                            int gx = bx + c, gy = by + r;
                            if (gx >= 0 && gx < REGION_COLS && gy >= 0 && gy < REGION_ROWS) {
                                int x = padding + gx * blockSize;
                                int y = padding + gy * blockSize;
                                PatternPainter.drawCell(g2, x, y, blockSize, base, cur, settings.isColorBlindMode());
                            }
                        }
                    }
                }
            }

            // draw score/next-block HUD
            manager.renderHUD(g2, padding, blockSize);
        } else {
            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 48));
            String msg = "GAME OVER";
            FontMetrics fm = g2.getFontMetrics();
            int x = (app.getWidth() - fm.stringWidth(msg)) / 2;
            int y = app.getHeight() / 2 - 50;
            g2.drawString(msg, x, y);

            g2.setFont(gameOverOptionFont);
            for (int i = 0; i < gameOverOptions.length; i++) {
                String text = (i == gameOverSelection ? "> " : "  ") + gameOverOptions[i];
                int optionX = (app.getWidth() - g2.getFontMetrics().stringWidth(text)) / 2;
                int optionY = y + 50 + i * 30;
                g2.setColor(i == gameOverSelection ? Color.YELLOW : Color.LIGHT_GRAY);
                g2.drawString(text, optionX, optionY);
            }

        }
        
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        Block cur = manager.getCurrentBlock();
        int[][] shape = (cur != null ? cur.getShape() : null);

        if (manager.isGameOver()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP, KeyEvent.VK_DOWN -> {
                    gameOverSelection = 1 - gameOverSelection;
                    //서보성 추가
                    app.repaint();
                }
                case KeyEvent.VK_ENTER -> {
                    if (gameOverSelection == 0) {
                        manager.resetGame();
                        if (timer != null) timer.start();
                        app.repaint();
                    } else {
                        app.showScreen(new MenuScreen(app));
                    }
                }
                // 서보성 추가
                default -> {/* no-op */}
            }

            return; // ★ 게임오버면 여기서 종료(아래 일반 조작으로 내려가지 않도록)

        }


        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> {
                if (shape != null) {
                    int nx = manager.getBlockX() - 1;
                    int ny = manager.getBlockY();
                    if (fitsRegion(nx, ny, shape)) manager.tryMove(nx, ny);
                }
            }
            case KeyEvent.VK_RIGHT -> {
                if (shape != null) {
                    int nx = manager.getBlockX() + 1;
                    int ny = manager.getBlockY();
                    if (fitsRegion(nx, ny, shape)) manager.tryMove(nx, ny);
                }
            }
            case KeyEvent.VK_UP -> {
                if (shape != null) {
                    int sh = shape.length, sw = shape[0].length;
                    int[][] rotated = new int[sw][sh];
                    for (int r = 0; r < sh; r++)
                        for (int c = 0; c < sw; c++)
                            rotated[c][sh - 1 - r] = shape[r][c];
                    int bx = manager.getBlockX(), by = manager.getBlockY();
                    if (fitsRegion(bx, by, rotated)) {
                        manager.rotateBlock();
                    } else {
                        int[] kicks = {-1, 1, -2, 2};
                        for (int dx : kicks) {
                            if (fitsRegion(bx + dx, by, rotated) && manager.tryMove(bx + dx, by)) {
                                manager.rotateBlock();
                                break;
                            }
                        }
                    }
                }
            }
            case KeyEvent.VK_DOWN -> {
                if (shape != null) {
                    int nx = manager.getBlockX();
                    int ny = manager.getBlockY() + 1;
                    if (fitsRegion(nx, ny, shape)) manager.tryMove(nx, ny);
                    else manager.tryMove(nx, ny);
                }
            }
            case KeyEvent.VK_ESCAPE -> app.showScreen(new MenuScreen(app));
        }
        app.repaint();
    }
}
