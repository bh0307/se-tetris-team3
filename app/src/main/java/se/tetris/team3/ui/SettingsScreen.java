package se.tetris.team3.ui;

import se.tetris.team3.core.Settings;
import se.tetris.team3.store.SettingsStore;

import java.awt.*;
import java.awt.event.KeyEvent;

public class SettingsScreen implements Screen {

    private final AppFrame app;
    private final Settings settings;

    // 항목 배열에 난이도 추가
    private final String[] items = new String[] {
            "화면 크기 프리셋: %s",
            "난이도: %s",          // 난이도 메뉴 추가
            "키 설정",
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
            case KeyEvent.VK_UP:
                index = (index + items.length - 1) % items.length;
                break;
            case KeyEvent.VK_DOWN:
                index = (index + 1) % items.length;
                break;
            case KeyEvent.VK_ENTER:
                onEnter();
                break;
            case KeyEvent.VK_ESCAPE:
                app.showScreen(new MenuScreen(app));
                break;
        }
        app.repaint();
    }

    private void onEnter() {
        switch (index) {
            case 0:
                // 프리셋 순환
                cyclePresetInPlace();
                SettingsStore.save(settings);
                app.setSize(settings.resolveWindowSize());
                break;
            case 1:
                // 난이도 순환
                cycleDifficulty();
                SettingsStore.save(settings);
                break;
            case 2:
                // 키 설정 화면으로 이동
                app.showScreen(new KeymapScreen(app));
                return;
            case 3:
                // 색맹 모드 토글
                settings.setColorBlindMode(!settings.isColorBlindMode());
                SettingsStore.save(settings);
                break;
            case 4:
                // 스코어 초기화
                SettingsStore.resetScores();
                break;
            case 5:
                // 기본값 복원
                settings.resetDefaults();
                SettingsStore.save(settings);
                app.setSize(settings.resolveWindowSize());
                break;
            case 6:
                app.showScreen(new MenuScreen(app)); // 뒤로가기
                break;
        }
    }

    private void cyclePresetInPlace() {
        Settings.SizePreset cur = settings.getSizePreset();
        Settings.SizePreset next;
        switch (cur) {
            case SMALL:
                next = Settings.SizePreset.MEDIUM;
                break;
            case MEDIUM:
                next = Settings.SizePreset.LARGE;
                break;
            case LARGE:
                next = Settings.SizePreset.SMALL;
                break;
            default:
                next = Settings.SizePreset.MEDIUM;
                break;
        }
        settings.setSizePreset(next);
    }

    private void cycleDifficulty() {
        Settings.Difficulty cur = settings.getDifficulty();
        Settings.Difficulty next;
        switch (cur) {
            case EASY:
                next = Settings.Difficulty.NORMAL;
                break;
            case NORMAL:
                next = Settings.Difficulty.HARD;
                break;
            case HARD:
                next = Settings.Difficulty.EASY;
                break;
            default:
                next = Settings.Difficulty.NORMAL;
                break;
        }
        settings.setDifficulty(next);
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
        switch (i) {
            case 0:
                return String.format(items[i], settings.getSizePreset().name());
            case 1:
                return String.format(items[i], settings.getDifficulty().name()); // 난이도 표시
            case 3:
                return String.format(items[i], settings.isColorBlindMode() ? "ON" : "OFF");
            default:
                return items[i];
        }
    }
}
