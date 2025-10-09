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
    private final GameManager manager;
    private Timer timer;
    private int blockSize;
    private final Settings settings;

    private int gameOverSelection = 0;
    private final String[] gameOverOptions = {"다시하기", "나가기"};
    private Font gameOverOptionFont = new Font("SansSerif", Font.PLAIN, 24);

    public GameScreen(AppFrame app) {
        this.app = app;
        this.manager = new GameManager();
        this.settings = app.getSettings();
        this.blockSize = settings.resolveBlockSize();
    }

    @Override
    public void onShow() {
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

    @Override
    public void onHide() {
        if (timer != null) timer.stop();
    }

    @Override
    public void render(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, app.getWidth(), app.getHeight());

        int blockSize = this.blockSize;
        int padding = 20;

        // 고정된 블록 그리기
        g2.setColor(Color.GRAY);
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 10; j++) {
                if (manager.getFieldValue(i, j) == 1) {
                    g2.fillRect(padding + j * blockSize, padding + i * blockSize, blockSize, blockSize);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(padding + j * blockSize, padding + i * blockSize, blockSize, blockSize);
                    g2.setColor(Color.GRAY);
                }
            }
        }

        // 현재 블록 그리기
        if (!manager.isGameOver()) {
            Block currentBlock = manager.getCurrentBlock();
            int[][] shape = currentBlock.getShape();
            g2.setColor(currentBlock.getColor());
            for (int i = 0; i < shape.length; i++) {
                for (int j = 0; j < shape[i].length; j++) {
                    if (shape[i][j] == 1) {
                        int x = manager.getBlockX() + j;
                        int y = manager.getBlockY() + i;
                        g2.fillRect(padding + x * blockSize, padding + y * blockSize, blockSize, blockSize);
                        g2.setColor(Color.BLACK);
                        g2.drawRect(padding + x * blockSize, padding + y * blockSize, blockSize, blockSize);
                        g2.setColor(currentBlock.getColor());
                    }
                }
            }
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

        // 필드 테두리(틀) 그리기
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(padding, padding, blockSize * 10, blockSize * 20);
        g2.setStroke(new BasicStroke(1));

        // 점수 표시 (필드 안쪽 우상단)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        String scoreText = "Score: " + manager.getScore();
        int scoreX = padding + blockSize * 10 - 10 - g2.getFontMetrics().stringWidth(scoreText);
        int scoreY = padding + 30;
        g2.drawString(scoreText, scoreX, scoreY);

        // ===== 네트 블록 미리보기 영역 =====
        int previewX = padding + blockSize * 10 + 40;
        int previewY = padding + 80;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Next", previewX, previewY - 25);
        Block nextBlock = manager.getNextBlock();
        int[][] nextShape = nextBlock.getShape();
        Color nextColor = nextBlock.getColor();
        int previewBlockSize = blockSize * 2 / 3;
        g2.setColor(nextColor);
        for (int i = 0; i < nextShape.length; i++) {
            for (int j = 0; j < nextShape[i].length; j++) {
                if (nextShape[i][j] == 1) {
                    int drawX = previewX + j * previewBlockSize;
                    int drawY = previewY + i * previewBlockSize;
                    g2.fillRect(drawX, drawY, previewBlockSize, previewBlockSize);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(drawX, drawY, previewBlockSize, previewBlockSize);
                    g2.setColor(nextColor);
                }
            }
        }
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        var km = settings.getKeymap();

        if (manager.isGameOver()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP, KeyEvent.VK_DOWN -> gameOverSelection = 1 - gameOverSelection;
                case KeyEvent.VK_ENTER -> {
                    if (gameOverSelection == 0) {
                        manager.resetGame();
                        if (timer != null) timer.start();
                    } else {
                        app.showScreen(new MenuScreen(app));
                    }
                }
            }
            app.repaint();
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> manager.tryMove(manager.getBlockX() - 1, manager.getBlockY());
            case KeyEvent.VK_RIGHT -> manager.tryMove(manager.getBlockX() + 1, manager.getBlockY());
            case KeyEvent.VK_UP -> manager.rotateBlock();
            case KeyEvent.VK_DOWN -> manager.tryMove(manager.getBlockX(), manager.getBlockY() + 1);
            case KeyEvent.VK_ESCAPE -> app.showScreen(new MenuScreen(app));
        }
        app.repaint();
    }
}
