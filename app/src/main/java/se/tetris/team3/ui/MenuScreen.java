package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import se.tetris.team3.ui.score.ScoreManager;
import se.tetris.team3.ui.score.ScoreboardScreen;

public class MenuScreen implements Screen {
    private final AppFrame app;
    private final List<MenuItem> items = new ArrayList<>();
    private int idx = 0; // 현재 선택 인덱스

    public MenuScreen(AppFrame app) {
        this.app = app;

        items.add(new MenuItem("게임 시작", () -> {
            app.showScreen(new GameScreen(app));
        }));
        items.add(new MenuItem("설정", () -> {
            app.showScreen(new SettingsScreen(app)); //SettingsScreen으로 실제 전환
        }));
        items.add(new MenuItem("스코어보드", () -> {
            app.showScreen(new ScoreboardScreen(app, -1, new ScoreManager()));
        }));
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

        // 하단 힌트
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String hint = "↑/↓ 이동   Enter 선택   Esc 종료";
        g2.drawString(hint, (w - g2.getFontMetrics().stringWidth(hint)) / 2, app.getHeight() - 60);
    }

    @Override public void onKeyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> idx = (idx - 1 + items.size()) % items.size();
            case KeyEvent.VK_DOWN -> idx = (idx + 1) % items.size();
            case KeyEvent.VK_ENTER -> items.get(idx).getAction().run();
            case KeyEvent.VK_ESCAPE -> System.exit(0);
        }
        app.repaint();
    }

    @Override public void onShow() { /* 필요시 포커스 관련 처리 추가 가능 */ }
    @Override public void onHide() { /* 필요시 리소스 정리 */ }
    
}
