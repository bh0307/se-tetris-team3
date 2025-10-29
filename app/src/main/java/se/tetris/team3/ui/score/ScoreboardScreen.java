package se.tetris.team3.ui.score;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.MenuItem;
import se.tetris.team3.ui.MenuScreen;
import se.tetris.team3.ui.Screen;
import se.tetris.team3.ui.score.ScoreManager.ScoreEntry;

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
        // 타이틀은 기본 폰트보다 조금 크게
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(18, baseFont + 6)));
        
        String modeLabel = (currentMode == GameMode.ITEM) ? "ITEM MODE" : "CLASSIC MODE";
        String title = "TOP 10 - " + modeLabel;
        String hint = "(Press TAB to switch mode)";

        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (width - titleWidth) / 2, 80);
        
        // 힌트 메시지 표시
        g2.setFont(new Font("Monospaced", Font.PLAIN, Math.max(12, baseFont)));
        int hintWidth = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (width - hintWidth) / 2, 110);
    }
    
    private void renderPlayerScore(Graphics2D g2, int width) {
        g2.setColor(Color.BLUE);
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        g2.setFont(new Font("Monospaced", Font.BOLD, baseFont));
        
        GameMode mode = app.getSettings().getGameMode();
        String modeLabel = (mode == GameMode.ITEM) ? "ITEM" : "CLASSIC";
        String diffLabel = app.getSettings().getDifficulty().name();
        String message = String.format("[%s,%s] YOUR SCORE: %d points", modeLabel, diffLabel, playerScore);
        
        int messageWidth = g2.getFontMetrics().stringWidth(message);
        g2.drawString(message, (width - messageWidth) / 2, 135);
    }
    
    private void renderScoreTable(Graphics2D g2, int width) {
        // 테이블 헤더
        g2.setColor(Color.BLACK);
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(12, baseFont)));
        
    int headerY = Math.max(160, baseFont * 14); // 타이틀과 힌트 메시지를 위해 시작 위치를 아래로 조정
    int leftMargin = Math.max(24, width / 12);  // 화면 너비의 1/12을 왼쪽 여백으로

    // 반응형 컬럼 배치: 전체 테이블 너비 안에서 컬럼 비율로 분배
    int tableWidth = width - leftMargin * 2;
    int nameColW = Math.max(50, (int)(tableWidth * 0.30)); // 이름 컬럼 너비 더 줄임
    int scoreColW = Math.max(50, (int)(tableWidth * 0.15)); // 점수 컬럼 너비도 줄임
    int diffColW = Math.max(40, (int)(tableWidth * 0.12));
    int dateColW = tableWidth - (nameColW + scoreColW + diffColW) - 10; // 여유 공간 줄임

    // 날짜는 항상 난이도 옆(같은 행)에 표시하도록 함

    // 랭크와 이름이 겹치지 않도록 rankX와 nameX 분리
    int nameX = leftMargin + Math.max(30, baseFont * 5); // 이름 시작 위치를 좀 더 왼쪽으로
    int scoreX = nameX + nameColW - 10; // 점수를 이름 쪽으로 더 가깝게
    int diffX = scoreX + scoreColW;
    int dateX = diffX + diffColW;

    // 행 높이: 기본/반응형
    int rowHeight = Math.max(24, baseFont * 2);
        
    int rankX = leftMargin;
    g2.drawString("Rank", rankX, headerY);
        g2.drawString("Name", nameX, headerY);
    g2.drawString("Score", scoreX, headerY);
    g2.drawString("Diff", diffX, headerY);
    g2.drawString("Date", dateX, headerY);
        
    // 헤더 아래 구분선
    g2.drawLine(leftMargin - 10, headerY + 10, width - leftMargin + 10, headerY + 10);
        
        // 현재 선택된 모드의 스코어 데이터
        List<ScoreEntry> scores = scoreManager.getHighScores(currentMode);
        
        for (int i = 0; i < Math.min(scores.size(), 10); i++) {
            ScoreEntry entry = scores.get(i);
            int rowY = headerY + Math.max(30, baseFont * 3) + (i * rowHeight);
            
            // 새로 추가된 점수 하이라이트
            if (isNewlyAddedScore(entry)) {
                g2.setColor(Color.BLUE);
                g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(12, baseFont - 0)));
            } else {
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Monospaced", Font.PLAIN, Math.max(11, baseFont - 1)));
            }
            
            g2.drawString(String.valueOf(i + 1), rankX, rowY);
            // 이름은 6자로 제한하고 말줄임표 처리
            String displayName = entry.getPlayerName();
            if (displayName.length() > 6) {
                displayName = displayName.substring(0, 6) + "...";
            }
            drawStringEllipsis(g2, displayName, nameX, rowY, nameColW - 8);
            drawStringEllipsis(g2, String.valueOf(entry.getScore()), scoreX, rowY, scoreColW - 8);
            // 난이도 축약표기: EASY->E, NORMAL->N, HARD->H
            String diffShort = switch (entry.getDifficulty()) {
                case EASY -> "E";
                case HARD -> "H";
                default -> "N";
            };
            drawStringEllipsis(g2, diffShort, diffX, rowY, diffColW - 8);

            // 날짜는 항상 같은 행에 표시 (너비가 부족하면 말줄임 처리)
            drawStringEllipsis(g2, entry.getFormattedDate(), dateX, rowY, dateColW - 8);
        }
    }
    
    private void renderMenu(Graphics2D g2, int width, int height) {
        int blockSize = app.getSettings().resolveBlockSize();
        int baseFont = Math.max(10, Math.min(18, Math.max(8, blockSize / 3)));
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(12, baseFont)));
        
        String[] menuLabels = {"   Return to Main Menu", "   Exit Program"};
    int menuStartY = height - Math.max(140, baseFont * 8);  // 화면 하단에서 비례 위치
        
        for (int i = 0; i < items.size(); i++) {
            int menuY = menuStartY + (i * 40);
            String text = menuLabels[i];
            
            if (i == idx) {
                // 선택된 항목 - 배경 하이라이트
                g2.setColor(Color.YELLOW);
                int textWidth = g2.getFontMetrics().stringWidth("▶"+text);
                int textX = (width - textWidth) / 2;
                g2.fillRect(textX - 10, menuY - (baseFont + 6), textWidth + 20, baseFont + 10);
                
                // 선택된 항목 텍스트
                g2.setColor(Color.BLUE);
                g2.drawString("▶"+text, textX, menuY);
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
            
            // TAB 키로 게임 모드 전환
            case KeyEvent.VK_TAB -> {
                currentMode = (currentMode == GameMode.ITEM) ? GameMode.CLASSIC : GameMode.ITEM;
                app.repaint();
            }
            
            // 현재 항목의 action.run() 실행
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> items.get(idx).getAction().run();
            
            // System.exit(0) 종료
            case KeyEvent.VK_ESCAPE -> System.exit(0);
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