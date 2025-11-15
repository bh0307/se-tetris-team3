package se.tetris.team3.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.ui.score.ScoreManager;
import se.tetris.team3.ui.score.ScoreboardScreen;

// 메뉴 항목: 클래식 시작, 아이템 모드 시작, 설정, 스코어보드, 종료
public class MenuScreen implements Screen {

    private final AppFrame app;
    private final List<MenuItem> items = new ArrayList<>();
    private int idx = 0; // 현재 선택 인덱스
    private boolean showHintHighlight = false; // 힌트 강조 표시 여부
    private long hintHighlightTime = 0; // 힌트 강조 시작 시간
    private Timer repaintTimer; // 화면 갱신 타이머

    public MenuScreen(AppFrame app) {
        this.app = app;

        items.add(new MenuItem("클래식 시작", () -> {
            app.getSettings().setGameMode(GameMode.CLASSIC);
            app.showScreen(new GameScreen(app, new GameManager(GameMode.CLASSIC)));
        }));
        items.add(new MenuItem("아이템 모드 시작", () -> {
            app.getSettings().setGameMode(GameMode.ITEM);
            app.showScreen(new GameScreen(app, new GameManager(GameMode.ITEM)));
        }));
        items.add(new MenuItem("대전 모드", () -> app.showScreen(new BattleModeSelectScreen(app, app.getSettings()))));
        items.add(new MenuItem("설정", () -> app.showScreen(new SettingsScreen(app))));
        items.add(new MenuItem("스코어보드", () -> app.showScreen(new ScoreboardScreen(app, -1, new ScoreManager()))));
        items.add(new MenuItem("종료", () -> System.exit(0)));
    }

    @Override public void render(Graphics2D g2) {
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        int w = app.getWidth();

        // 배경
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, app.getWidth(), app.getHeight());

        // 타이틀
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 36));
        String title = "TETRIS";
        g2.drawString(title, (w - g2.getFontMetrics().stringWidth(title)) / 2, 140);

        // 메뉴 항목
        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        int y = 220;
        for (int i = 0; i < items.size(); i++) {
            String t = (i == idx ? "> " : "  ") + items.get(i).getLabel();
            g2.setColor(i == idx ? new Color(120, 200, 255) : Color.LIGHT_GRAY);
            g2.drawString(t, (w - g2.getFontMetrics().stringWidth(t)) / 2, y);
            y += 40;
        }

        // 하단 힌트 (잘못된 키 입력 시 1초간만 표시)
        boolean showHint = showHintHighlight && System.currentTimeMillis() - hintHighlightTime < 1000;
        if (showHintHighlight && !showHint) {
            showHintHighlight = false;
        }
        
        if (showHint) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            String hint = "↑/↓ 이동   Enter 선택   Esc 종료";
            g2.drawString(hint, (w - g2.getFontMetrics().stringWidth(hint)) / 2, app.getHeight() - 60);
        }
    }

    @Override public void onKeyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> idx = (idx - 1 + items.size()) % items.size();
            case KeyEvent.VK_DOWN -> idx = (idx + 1) % items.size();
            case KeyEvent.VK_ENTER -> items.get(idx).getAction().run();
            case KeyEvent.VK_ESCAPE -> System.exit(0);
            default -> {
                // 유효하지 않은 키 입력 시 힌트 강조
                showHintHighlight = true;
                hintHighlightTime = System.currentTimeMillis();
                
                // 1초 후 자동으로 화면 갱신하기 위한 타이머
                if (repaintTimer != null) {
                    repaintTimer.stop();
                }
                repaintTimer = new Timer(1000, e1 -> {
                    app.repaint();
                    repaintTimer.stop();
                });
                repaintTimer.setRepeats(false);
                repaintTimer.start();
            }
        }
        app.repaint();
    }
    //필요시 포커스 관련 처리 추가 가능
    @Override public void onShow() {}
    //필요시 리소스 정리
    @Override public void onHide() {
        if (repaintTimer != null) {
            repaintTimer.stop();
        }
    }
    
}
