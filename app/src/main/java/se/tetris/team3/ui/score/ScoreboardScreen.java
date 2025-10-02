package se.tetris.team3.ui.score;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.MenuItem;
import se.tetris.team3.ui.MenuScreen;
import se.tetris.team3.ui.Screen;
import se.tetris.team3.ui.score.ScoreManager.ScoreEntry;

public class ScoreboardScreen implements Screen {
    private static final String SCORE_FORMAT_MESSAGE = "YOUR SCORE: %d points";
    
    private int idx = 0;
    private final List<MenuItem> items = new ArrayList<>();
    private final AppFrame app;
    private final ScoreManager scoreManager;
    private final String playerName;
    private final int playerScore;
    
    public ScoreboardScreen(AppFrame app, int playerScore, ScoreManager scoreManager) {
        this(app, null, playerScore, scoreManager);
    }
    
    public ScoreboardScreen(AppFrame app, String playerName, int playerScore, ScoreManager scoreManager) {
        this.app = app;
        this.playerName = playerName;
        this.playerScore = playerScore;
        this.scoreManager = scoreManager;
        
        items.add(new MenuItem("메인 메뉴", () -> {
            app.showScreen(new MenuScreen(app));
        }));
        items.add(new MenuItem("종료", () -> System.exit(0)));
    }

    @Override
    public void render(Graphics2D g2) {
        // 화면 크기 정보 가져오기
        int width = app.getWidth();
        int height = app.getHeight();
        
        // 배경색
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        
        // 제목 렌더링
        renderTitle(g2, width);
        
        // 플레이어 점수 메시지 렌더링
        if (playerScore != -1) {
            renderPlayerScore(g2, width);
        }
        
        // 스코어 테이블 렌더링
        renderScoreTable(g2, width);
        
        // 메뉴 렌더링
        renderMenu(g2, width, height);
    }
    
    private void renderTitle(Graphics2D g2, int width) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.BOLD, 24));
        
        String title = "TOP 10";
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        int titleX = (width - titleWidth) / 2;
        
        g2.drawString(title, titleX, 80);
    }
    
    private void renderPlayerScore(Graphics2D g2, int width) {
        g2.setColor(Color.BLUE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        
        String message = String.format(SCORE_FORMAT_MESSAGE, playerScore);
        int messageWidth = g2.getFontMetrics().stringWidth(message);
        int messageX = (width - messageWidth) / 2;
        
        g2.drawString(message, messageX, 120);
    }
    
    private void renderScoreTable(Graphics2D g2, int width) {
        // 테이블 헤더
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        
        int headerY = 180;
        int leftMargin = width / 8;  // 화면 너비의 1/8을 왼쪽 여백으로
        int nameX = leftMargin + 80;
        int scoreX = leftMargin + 160;
        int dateX = leftMargin + 240;
        
        g2.drawString("Rank", leftMargin, headerY);
        g2.drawString("Name", nameX, headerY);
        g2.drawString("Score", scoreX, headerY);
        g2.drawString("Date", dateX, headerY);
        
        // 헤더 아래 구분선
        int lineStart = leftMargin - 10;
        int lineEnd = width - leftMargin + 10;
        g2.drawLine(lineStart, headerY + 10, lineEnd, headerY + 10);
        
        // 스코어 데이터
        List<ScoreEntry> scores = scoreManager.getHighScores();
        
        for (int i = 0; i < Math.min(scores.size(), 10); i++) {
            ScoreEntry entry = scores.get(i);
            int rowY = headerY + 30 + (i * 30);
            
            // 새로 추가된 점수 하이라이트
            if (isNewlyAddedScore(entry)) {
                g2.setColor(Color.BLUE);
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            } else {
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            }
            
            g2.drawString(String.valueOf(i + 1), leftMargin, rowY);
            g2.drawString(entry.getPlayerName(), nameX, rowY);
            g2.drawString(String.valueOf(entry.getScore()), scoreX, rowY);
            g2.drawString(entry.getFormattedDate(), dateX, rowY);
        }
    }
    
    private void renderMenu(Graphics2D g2, int width, int height) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        
        String[] menuLabels = {"   Return to Main Menu", "   Exit Program"};
        int menuStartY = height - 170;  // 화면 하단에서 170px 위
        
        for (int i = 0; i < items.size(); i++) {
            int menuY = menuStartY + (i * 40);
            String text = menuLabels[i];
            
            if (i == idx) {
                // 선택된 항목 - 배경 하이라이트
                g2.setColor(Color.YELLOW);
                int textWidth = g2.getFontMetrics().stringWidth("▶" + text);
                int textX = (width - textWidth) / 2;
                g2.fillRect(textX - 10, menuY - 20, textWidth + 20, 25);
                
                // 선택된 항목 텍스트
                g2.setColor(Color.BLUE);
                g2.drawString("▶" + text, textX, menuY);
            } else {
                // 선택되지 않은 항목
                g2.setColor(Color.BLACK);
                int textWidth = g2.getFontMetrics().stringWidth(text);
                int textX = (width - textWidth) / 2;
                g2.drawString(text, textX, menuY);
            }
        }
    }
    
    private boolean isNewlyAddedScore(ScoreEntry entry) {
        if (playerName == null || playerScore == -1) {
            return false;
        }
        return entry.getPlayerName().equals(playerName) && 
               entry.getScore() == playerScore;
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            // UP, DOWN => idx 변경(원형 이동)
            case KeyEvent.VK_UP -> idx = (idx - 1 + items.size()) % items.size();
            case KeyEvent.VK_DOWN -> idx = (idx + 1) % items.size();
            
            // 현재 항목의 action.run() 실행
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> items.get(idx).getAction().run();
            
            // System.exit(0) 종료
            case KeyEvent.VK_ESCAPE -> System.exit(0);
        }
        app.repaint();  // app을 통한 화면 갱신
    }

    @Override
    public void onShow() {
        // 화면이 표시될 때 호출
    }

    @Override
    public void onHide() {
        // 화면이 숨겨질 때 호출
    }
}