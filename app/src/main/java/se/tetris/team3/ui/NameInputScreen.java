package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import se.tetris.team3.core.GameMode;        
import se.tetris.team3.ui.score.ScoreManager;
import se.tetris.team3.ui.score.ScoreboardScreen;

public class NameInputScreen implements Screen {
    private final AppFrame app;
    private final int playerScore;
    private StringBuilder playerName;
    private static final int MAX_NAME_LENGTH = 10;

    private static final ScoreManager scoreManager = new ScoreManager();

    public NameInputScreen(AppFrame app) {
        this(app, 0);
    }

    public NameInputScreen(AppFrame app, int playerScore) {
        this.app = app;
        this.playerScore = playerScore;
        this.playerName = new StringBuilder();
    }

    @Override
    public void render(Graphics2D g2) {
        int width = app.getWidth();
        int height = app.getHeight();

        // 배경
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        // 제목
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.BOLD, 28));
        String title = "NEW HIGH SCORE!";
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 150);

        // 점수 표시
        g2.setColor(Color.BLUE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        String scoreText = "Score: " + playerScore;
        int scoreWidth = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, (width - scoreWidth) / 2, 200);

        // 입력 안내
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        String prompt = "Enter your name:";
        int promptWidth = g2.getFontMetrics().stringWidth(prompt);
        g2.drawString(prompt, (width - promptWidth) / 2, 280);

        // 입력 박스
        int boxWidth = 300, boxHeight = 40;
        int boxX = (width - boxWidth) / 2, boxY = 300;

        g2.setColor(Color.GRAY);
        g2.drawRect(boxX, boxY, boxWidth, boxHeight);
        g2.setColor(Color.WHITE);
        g2.fillRect(boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2);

        // 현재 입력된 이름
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g2.drawString(playerName.toString(), boxX + 10, boxY + 25);

        // 하단 안내
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        String instructions = "Type name (max " + MAX_NAME_LENGTH + " chars) and press ENTER";
        int instructWidth = g2.getFontMetrics().stringWidth(instructions);
        g2.drawString(instructions, (width - instructWidth) / 2, 380);

        String escapeInfo = "Press ESC to skip name entry";
        int escapeWidth = g2.getFontMetrics().stringWidth(escapeInfo);
        g2.drawString(escapeInfo, (width - escapeWidth) / 2, 400);
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE -> {
                // 이름 확정 (비었으면 Anonymous)
                String trimmedName = playerName.toString().trim();
                String finalName = trimmedName.isEmpty() ? "Anonymous" : trimmedName;

                // 현재 모드로 저장 (클래식/아이템 분리)
                GameMode mode = app.getSettings().getGameMode();
                scoreManager.addScore(mode, finalName, playerScore);

                // 스코어보드는 내부에서 현재 모드로 읽도록 이미 수정했음
                app.showScreen(new ScoreboardScreen(app, finalName, playerScore, scoreManager));
            }
            case KeyEvent.VK_BACK_SPACE -> {
                if (playerName.length() > 0) playerName.deleteCharAt(playerName.length() - 1);
            }
            default -> {
                char keyChar = e.getKeyChar();
                if (isValidNameChar(keyChar) && playerName.length() < MAX_NAME_LENGTH) {
                    playerName.append(keyChar);
                }
            }
        }
        app.repaint();
    }

    // 이름 허용 문자
    private boolean isValidNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_';
    }
}
