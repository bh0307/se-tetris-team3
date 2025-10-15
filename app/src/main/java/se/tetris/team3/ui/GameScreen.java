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

    private static final int REGION_COLS = 10;
    private static final int REGION_ROWS = 20;

    private Block lastBlockRef = null;

    private boolean isPaused = false;  // 일시정지 상태 변수

    public GameScreen(AppFrame app) {
        this(app, new GameManager());
    }

    public GameScreen(AppFrame app, GameManager manager) {
        this.app = app;
        this.manager = manager;
        this.settings = app.getSettings();
        this.manager.attachSettings(app.getSettings());
    }

    @Override public void onShow() {
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!manager.isGameOver() && !isPaused) {
                    manager.stepDownOrFix();
                    updateTimerDelay();
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

    private void updateTimerDelay() {
        int delay = Math.max(100, 1000 - (manager.getLevel() - 1) * 100);
        timer.setDelay(delay);
    }

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

    private static int bottomRowIndex(int[][] shape) {
        for (int r = shape.length - 1; r >= 0; r--) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) return r;
            }
        }
        return shape.length - 1;
    }

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

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, app.getWidth(), app.getHeight());

        if (isPaused) {
            int width = app.getWidth();
            int height = app.getHeight();
            g2.setColor(new Color(255, 255, 255, 160));
            g2.fillRect(0, 0, width, height);

            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 72));
            String msg = "PAUSED";
            int msgWidth = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (width - msgWidth) / 2, height / 2);
            return;
        }

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(padding, padding, blockSize * 10, blockSize * 20);
        g2.setStroke(new BasicStroke(1));

        alignSpawnIfNewBlock();

        for (int r = 0; r < REGION_ROWS; r++) {
            for (int c = 0; c < REGION_COLS; c++) {
                int colorValue = manager.getFieldValue(r, c);
                if (colorValue != 0) {
                    int x = padding + c * blockSize;
                    int y = padding + r * blockSize;
                    PatternPainter.drawCell(g2, x, y, blockSize, Color.GRAY, null, settings.isColorBlindMode());
                }
            }
        }

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
            manager.renderHUD(g2, padding, blockSize);
        } else {
            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 48));
            String msg = "GAME OVER";
            FontMetrics fm = g2.getFontMetrics();
            int x = (app.getWidth() - fm.stringWidth(msg)) / 2;
            int y = app.getHeight() / 2 - 50;
            g2.drawString(msg, x, y);

            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2.setColor(Color.YELLOW);
            String scoreMsg = "Your Score: " + manager.getScore();
            int sx = (app.getWidth() - g2.getFontMetrics().stringWidth(scoreMsg)) / 2;
            int sy = y + 50;
            g2.drawString(scoreMsg, sx, sy);

            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            String hint = "Press any key";
            int hintWidth = g2.getFontMetrics().stringWidth(hint);
            int hintX = (app.getWidth() - hintWidth) / 2;
            int hintY = sy + 40;
            g2.drawString(hint, hintX, hintY);
        }
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        Block cur = manager.getCurrentBlock();
        int[][] shape = (cur != null ? cur.getShape() : null);

        if (manager.isGameOver()) {
            if(new se.tetris.team3.ui.score.ScoreManager().isHighScore(manager.getScore())) {
                app.showScreen(new NameInputScreen(app, manager.getScore()));
            }
            return;
        }

        if (code == KeyEvent.VK_SPACE) {
            isPaused = !isPaused;
            if (!isPaused) {
                if (manager.isGameOver()) {
                    if (timer != null) timer.stop();
                } else {
                    manager.stepDownOrFix();  // 재개 시 즉시 블록 처리
                    updateTimerDelay();
                    if (timer != null && !timer.isRunning()) timer.start();
                }
            } else {
                if (timer != null) timer.stop();
            }
            app.repaint();
            return;
        }

        if (isPaused) {
            return;
        }

        final var km = settings.getKeymap();

        if (code == km.get(se.tetris.team3.core.Settings.Action.MOVE_LEFT)) {
            if (shape != null) {
                int nx = manager.getBlockX() - 1;
                int ny = manager.getBlockY();
                if (fitsRegion(nx, ny, shape)) manager.tryMove(nx, ny);
            }
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.MOVE_RIGHT)) {
            if (shape != null) {
                int nx = manager.getBlockX() + 1;
                int ny = manager.getBlockY();
                if (fitsRegion(nx, ny, shape)) manager.tryMove(nx, ny);
            }
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.ROTATE)) {
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
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.SOFT_DROP)) {
            if (shape != null) {
                int nx = manager.getBlockX();
                int ny = manager.getBlockY() + 1;
                if (fitsRegion(nx, ny, shape)) manager.tryMove(nx, ny);
                else manager.tryMove(nx, ny);
            }
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.PAUSE)) {
            // 기존 기능 비워두기
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.EXIT)) {
            app.showScreen(new MenuScreen(app));
        }

        app.repaint();
    }
}
