package se.tetris.team3.ui.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.gameManager.MenuItem;
import se.tetris.team3.gameManager.ScoreManager;
import se.tetris.team3.gameManager.ScoreManager.ScoreEntry;
import se.tetris.team3.ui.AppFrame;

// 스코어보드 화면
// 현재 Settings의 GameMode를 읽어 모드별 상위 랭킹을 보여줌
public class ScoreboardScreen implements Screen {
    
    private static final String SCORE_FORMAT_MESSAGE = "YOUR SCORE: %d points";
    
    private int idx = 0;
    private final List<MenuItem> items = new ArrayList<>();
    private final AppFrame app;
    private final ScoreManager scoreManager;
    private final String playerName;
    private final int playerScore;
    private GameMode currentMode; // 현재 보여줄 모드 추가
    
    public ScoreboardScreen(AppFrame app, int playerScore, ScoreManager scoreManager) {
        this(app, null, playerScore, scoreManager);
    }
    
    public ScoreboardScreen(AppFrame app, String playerName, int playerScore, ScoreManager scoreManager) {
        this.app = app;
        this.playerName = playerName;
        this.playerScore = playerScore;
        this.scoreManager = scoreManager;
        this.currentMode = app.getSettings().getGameMode(); // 현재 모드 초기화
        
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
        
        // 어두운 그라디언트 배경
        for (int i = 0; i < height; i++) {
            float ratio = (float)i / height;
            int r = (int)(25 + ratio * 15);
            int b = (int)(35 + ratio * 20);
            g2.setColor(new Color(r, r, b));
            g2.drawLine(0, i, width, i);
        }
        
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
        // 타이틀은 기본 폰트보다 조금 크게
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(24, baseFont + 10)));
        
        String modeLabel = (currentMode == GameMode.ITEM) ? "ITEM MODE" : "CLASSIC MODE";
        String title = "TOP 10 - " + modeLabel;
        String hint = "(Press TAB to switch mode)";

        // 타이틀 - 골드 색상
        g2.setColor(new Color(255, 215, 0));
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 60);
        
        // 힌트 메시지 표시 - 밝은 회색
        g2.setFont(new Font("Monospaced", Font.PLAIN, Math.max(12, baseFont)));
        g2.setColor(new Color(180, 180, 180));
        int hintWidth = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (width - hintWidth) / 2, 85);
    }
    
    private void renderPlayerScore(Graphics2D g2, int width) {
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(14, baseFont)));
        
        GameMode mode = app.getSettings().getGameMode();
        String modeLabel = (mode == GameMode.ITEM) ? "ITEM" : "CLASSIC";
        String diffLabel = app.getSettings().getDifficulty().name();
        String message = String.format("[%s,%s] YOUR SCORE: %d points", modeLabel, diffLabel, playerScore);
        
        // 하늘색으로 표시
        g2.setColor(new Color(100, 200, 255));
        int messageWidth = g2.getFontMetrics().stringWidth(message);
        g2.drawString(message, (width - messageWidth) / 2, 110);
    }
    
    private void renderScoreTable(Graphics2D g2, int width) {
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        
        int startY = 140;
        int leftMargin = Math.max(40, width / 10);
        int rowHeight = Math.max(45, baseFont * 3);
        int boxWidth = width - leftMargin * 2;
        
        // 현재 선택된 모드의 스코어 데이터
        List<ScoreEntry> scores = scoreManager.getHighScores(currentMode);
        
        for (int i = 0; i < Math.min(scores.size(), 10); i++) {
            ScoreEntry entry = scores.get(i);
            int rowY = startY + (i * rowHeight);
            
            // 순위별 색상 결정
            Color rankColor;
            Color textColor;
            if (i == 0) {
                rankColor = new Color(255, 215, 0); // 금색
                textColor = new Color(255, 215, 0);
            } else if (i == 1) {
                rankColor = new Color(192, 192, 192); // 은색
                textColor = new Color(220, 220, 220);
            } else if (i == 2) {
                rankColor = new Color(205, 127, 50); // 동색
                textColor = new Color(205, 150, 100);
            } else {
                rankColor = new Color(100, 100, 120);
                textColor = new Color(200, 200, 200);
            }
            
            // 새로 추가된 점수는 하이라이트
            boolean isNew = isNewlyAddedScore(entry);
            
            // 배경 박스
            g2.setColor(new Color(45, 45, 55));
            g2.fillRoundRect(leftMargin, rowY, boxWidth, rowHeight - 5, 10, 10);
            
            // 테두리
            g2.setColor(isNew ? new Color(100, 200, 255) : new Color(70, 70, 80));
            g2.drawRoundRect(leftMargin, rowY, boxWidth, rowHeight - 5, 10, 10);
            
            // 순위 배지 (왼쪽 원형)
            int badgeX = leftMargin + 20;
            int badgeY = rowY + (rowHeight - 5) / 2;
            g2.setColor(rankColor);
            g2.fillOval(badgeX - 15, badgeY - 15, 30, 30);
            
            // 순위 번호
            g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(14, baseFont)));
            g2.setColor(new Color(20, 20, 30));
            String rankStr = String.valueOf(i + 1);
            int rankW = g2.getFontMetrics().stringWidth(rankStr);
            g2.drawString(rankStr, badgeX - rankW / 2, badgeY + 5);
            
            // 이름
            g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(14, baseFont + 2)));
            g2.setColor(textColor);
            String displayName = entry.getPlayerName();
            if (displayName.length() > 10) {
                displayName = displayName.substring(0, 10) + "...";
            }
            g2.drawString(displayName, leftMargin + 60, rowY + 20);
            
            // 점수 (오른쪽 정렬)
            g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(16, baseFont + 4)));
            g2.setColor(new Color(255, 215, 0));
            String scoreStr = String.format("✕ %,d", entry.getScore());
            int scoreW = g2.getFontMetrics().stringWidth(scoreStr);
            g2.drawString(scoreStr, leftMargin + boxWidth - scoreW - 20, rowY + 20);
            
            // 난이도와 날짜 (아래쪽)
            g2.setFont(new Font("Monospaced", Font.PLAIN, Math.max(11, baseFont - 1)));
            g2.setColor(new Color(150, 150, 160));
            String diffLabel = entry.getDifficulty().name();
            g2.drawString("Difficulty: " + diffLabel, leftMargin + 60, rowY + 35);
            
            // 날짜 (오른쪽 하단)
            g2.setColor(new Color(120, 120, 130));
            String dateStr = entry.getFormattedDate();
            int dateW = g2.getFontMetrics().stringWidth(dateStr);
            g2.drawString(dateStr, leftMargin + boxWidth - dateW - 20, rowY + 35);
        }
    }
    
    private void renderMenu(Graphics2D g2, int width, int height) {
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(14, baseFont)));
        
        String[] menuLabels = {"Return to Main Menu", "Exit Program"};
        int menuStartY = height - Math.max(100, baseFont * 6);
        
        for (int i = 0; i < items.size(); i++) {
            int menuY = menuStartY + (i * 35);
            String text = menuLabels[i];
            
            if (i == idx) {
                // 선택된 항목 - 배경 하이라이트
                g2.setColor(new Color(255, 215, 0, 100));
                int textWidth = g2.getFontMetrics().stringWidth("▶ " + text);
                int textX = (width - textWidth) / 2;
                g2.fillRoundRect(textX - 15, menuY - (baseFont + 4), textWidth + 30, baseFont + 8, 8, 8);
                
                // 선택된 항목 텍스트
                g2.setColor(new Color(255, 215, 0));
                g2.drawString("▶ " + text, textX, menuY);
            } else {
                // 선택되지 않은 항목
                g2.setColor(new Color(180, 180, 180));
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
            case KeyEvent.VK_UP:
                idx = (idx - 1 + items.size()) % items.size();
                break;
            case KeyEvent.VK_DOWN:
                idx = (idx + 1) % items.size();
                break;
            // TAB 키로 게임 모드 전환
            case KeyEvent.VK_TAB:
                currentMode = (currentMode == GameMode.ITEM) ? GameMode.CLASSIC : GameMode.ITEM;
                app.repaint();
                break;
            // 현재 항목의 action.run() 실행
            case KeyEvent.VK_ENTER:
                items.get(idx).getAction().run();
                break;
            case KeyEvent.VK_SPACE:
                items.get(idx).getAction().run();
                break;
            // System.exit(0) 종료
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
        }
        app.repaint();  // app을 통한 화면 갱신
    }
     // 문자열을 주어진 최대 너비에 맞춰 그리고, 넘치면 말줄임표(...)로 대체
    private void drawStringEllipsis(Graphics2D g2, String text, int x, int y, int maxWidth) {
        if (text == null) return;
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            g2.drawString(text, x, y);
            return;
        }

        String ell = "...";
        int ellWidth = fm.stringWidth(ell);
        int avail = Math.max(0, maxWidth - ellWidth);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (fm.stringWidth(sb.toString()) > avail) {
                sb.setLength(Math.max(0, sb.length() - 1));
                break;
            }
        }
        g2.drawString(sb.toString() + ell, x, y);
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