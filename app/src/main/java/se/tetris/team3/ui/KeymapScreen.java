package se.tetris.team3.ui;

import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.store.SettingsStore;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeymapScreen implements Screen {

    private final AppFrame app;
    private final Settings settings;

    private final List<Action> items = new ArrayList<>();
    private int cursor = 0;
    private boolean waitingInput = false;
    private String status = "↑/↓: 이동  Enter: 입력대기  ESC/Backspace: 뒤로가기";

    public KeymapScreen(AppFrame app) {
        this.app = app;
        this.settings = app.getSettings();
        items.add(Action.MOVE_LEFT);
        items.add(Action.MOVE_RIGHT);
        items.add(Action.ROTATE);
        items.add(Action.SOFT_DROP);
        items.add(Action.PAUSE);
        items.add(Action.EXIT);
    }

    @Override public void render(Graphics2D g2) {
        int w = app.getWidth(), h = app.getHeight();
        g2.setColor(Color.BLACK);
        g2.fillRect(0,0,w,h);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2.drawString("키 설정", 36, 60);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        int y = 110;
        for (int i = 0; i < items.size(); i++) {
            Action a = items.get(i);
            String left = a.name();
            String right = KeyEvent.getKeyText(settings.getKeymap().get(a));
            if (i == cursor) {
                g2.setColor(new Color(30,144,255));
                g2.fillRoundRect(26, y-18, w-52, 28, 8, 8);
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(Color.LIGHT_GRAY);
            }
            g2.drawString(left, 46, y);

            String disp = (waitingInput && i==cursor) ? "입력 대기..." : right;
            int strW = g2.getFontMetrics().stringWidth(disp);
            g2.drawString(disp, w - 46 - strW, y);
            y += 36;
        }

        g2.setColor(waitingInput ? Color.ORANGE : Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2.drawString(status, 36, h - 36);
    }

    private boolean isKeyInUse(int keyCode, Action except) {
        for (Map.Entry<Action, Integer> e : settings.getKeymap().entrySet()) {
            if (e.getKey() != except && e.getValue() == keyCode) return true;
        }
        return false;
    }

    @Override public void onKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (waitingInput) {
            // 취소
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                waitingInput = false;
                status = "입력 취소됨";
                app.repaint();
                return;
            }

            Action target = items.get(cursor);
            if (isKeyInUse(code, target)) {
                status = "이미 사용 중: " + KeyEvent.getKeyText(code);
            } else {
                settings.getKeymap().put(target, code);
                SettingsStore.save(settings); // 저장
                status = target.name() + " → " + KeyEvent.getKeyText(code) + " 저장됨";
                waitingInput = false;
            }
            app.repaint();
            return;
        }

        // 일반 네비
        switch (code) {
            case KeyEvent.VK_UP -> cursor = (cursor - 1 + items.size()) % items.size();
            case KeyEvent.VK_DOWN -> cursor = (cursor + 1) % items.size();
            case KeyEvent.VK_ENTER -> {
                waitingInput = true;
                status = items.get(cursor).name() + " : 새 키를 눌러주세요 (ESC/Backspace 취소)";
            }
            case KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE -> app.showScreen(new SettingsScreen(app));
        }
        app.repaint();
    }
}
