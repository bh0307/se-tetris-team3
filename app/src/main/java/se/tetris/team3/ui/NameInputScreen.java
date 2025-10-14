package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import se.tetris.team3.ui.score.ScoreManager;
import se.tetris.team3.ui.score.ScoreboardScreen;

public class NameInputScreen implements Screen {
    private final AppFrame app;
    private final int playerScore;
    private StringBuilder playerName;
    private static final int MAX_NAME_LENGTH = 10;
    private static final ScoreManager scoreManager = new ScoreManager();

    public NameInputScreen(AppFrame app) {
        this(app, 0); // 기본 점수 0
    }
    
    public NameInputScreen(AppFrame app, int playerScore) {
        this.app = app;
        this.playerScore = playerScore;
        this.playerName = new StringBuilder();
    }

    @Override
    public void render(Graphics2D g2) {
        // 화면 크기 정보 가져오기
        int width = app.getWidth();
        int height = app.getHeight();
        
        // 배경색
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
        
        // 이름 입력 안내
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        String prompt = "Enter your name:";
        int promptWidth = g2.getFontMetrics().stringWidth(prompt);
        g2.drawString(prompt, (width - promptWidth) / 2, 280);
        
        // 입력 박스 그리기
        int boxWidth = 300;
        int boxHeight = 40;
        int boxX = (width - boxWidth) / 2;
        int boxY = 300;
        
        // 입력 박스 테두리
        g2.setColor(Color.GRAY);
        g2.drawRect(boxX, boxY, boxWidth, boxHeight);
        
        // 입력 박스 배경
        g2.setColor(Color.WHITE);
        g2.fillRect(boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2);
        
        // 입력된 텍스트 표시
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        String currentName = playerName.toString();
        
        g2.drawString(currentName, boxX + 10, boxY + 25);
        
        // 안내 메시지
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
            case KeyEvent.VK_ENTER,KeyEvent.VK_ESCAPE -> {
                // 공백 제거 후 체크
                String trimmedName = playerName.toString().trim();
                // 이름이 비어있거나 공백만 있으면 "Anonymous"로 설정
                String finalName = trimmedName.isEmpty() ? "Anonymous" : trimmedName;
                scoreManager.addScore(finalName, playerScore);
                app.showScreen(new ScoreboardScreen(app, finalName, playerScore, scoreManager));
            }
            case KeyEvent.VK_BACK_SPACE -> {
                // 백스페이스로 문자 삭제
                if (playerName.length() > 0) {
                    playerName.deleteCharAt(playerName.length() - 1);
                }
            }
            
            default -> {
                // 일반 문자 입력 처리
                char keyChar = e.getKeyChar();
                if (isValidNameChar(keyChar) && playerName.length() < MAX_NAME_LENGTH) {
                    playerName.append(keyChar);
                }
            }
        }
        
        app.repaint();
    }
    
    // 이름에 사용가능한 문자인지 확인
    private boolean isValidNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_';
    }
}