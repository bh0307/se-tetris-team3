package se.tetris.team3.ui;

import se.tetris.team3.core.Settings;
import se.tetris.team3.store.SettingsStore;

import java.awt.*;
import java.awt.event.KeyEvent;

public class SettingsScreen implements Screen {

    private final AppFrame app;
    private final Settings settings;

    private final String[] items = new String[] {
            "화면 크기 프리셋: %s",
            "색맹 모드: %s",
            "스코어 초기화",
            "기본값 복원",
            "뒤로가기"
    };
    private int index = 0;

    public SettingsScreen(AppFrame app) {
        this.app = app;
        this.settings = app.getSettings();
    }

    @Override public void onShow() { /* 필요 시 포커스 관련 작업 */ }
    @Override public void onHide() { /* 필요 시 리소스 정리 */ }

    /* 입력값 */
    @Override
    public void onKeyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> index = (index + items.length - 1) % items.length;
            case KeyEvent.VK_DOWN -> index = (index + 1) % items.length;
            case KeyEvent.VK_ENTER -> onEnter();
            case KeyEvent.VK_ESCAPE -> app.showScreen(new MenuScreen(app));
        }
        app.repaint();
    }

    private void onEnter() {
        switch (index) {
            case 0 -> { // 프리셋 순환
                cyclePresetInPlace();
                SettingsStore.save(settings);
                app.setSize(settings.resolveWindowSize());
            }
            case 1 -> { // 색맹 모드 토글
                settings.setColorBlindMode(!settings.isColorBlindMode());
                SettingsStore.save(settings);
            }
            case 2 -> { // 스코어 초기화
                SettingsStore.resetScores();
                javax.swing.JOptionPane.showMessageDialog(null, "스코어가 초기화되었습니다.");
            }
            case 3 -> { // 기본값 복원
                settings.resetDefaults();
                SettingsStore.save(settings);
                app.setSize(settings.resolveWindowSize());
            }
            case 4 -> app.showScreen(new MenuScreen(app)); // 뒤로가기
        }
    }

    private void cyclePresetInPlace() {
        Settings.SizePreset cur = settings.getSizePreset();
        Settings.SizePreset next = switch (cur) {
            case SMALL -> Settings.SizePreset.MEDIUM;
            case MEDIUM -> Settings.SizePreset.LARGE;
            case LARGE -> Settings.SizePreset.SMALL;
        };
        settings.setSizePreset(next);
    }

    /* 렌더링 */
    @Override
    public void render(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = app.getWidth();
        int h = app.getHeight();

        // 배경
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        // 타이틀
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        String title = "Settings";
        g2.drawString(title, (w - g2.getFontMetrics().stringWidth(title)) / 2, 120);

        // 항목
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        int y = 200;
        for (int i = 0; i < items.length; i++) {
            String line = formatItem(i);
            String draw = (i == index ? "> " : "  ") + line;
            g2.setColor(i == index ? new Color(120, 200, 255) : Color.LIGHT_GRAY);
            g2.drawString(draw, (w - g2.getFontMetrics().stringWidth(draw)) / 2, y);
            y += 36;
        }

        // 하단 가이드
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String hint = "↑/↓ 이동   Enter 선택   ESC 뒤로";
        g2.drawString(hint, (w - g2.getFontMetrics().stringWidth(hint)) / 2, h - 60);
    }

    private String formatItem(int i) {
        return switch (i) {
            case 0 -> String.format(items[i], settings.getSizePreset().name());
            case 1 -> String.format(items[i], settings.isColorBlindMode() ? "ON" : "OFF");
            default -> items[i];
        };
    }
}
