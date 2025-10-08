// 메인 윈도우 + 화면 전환기
package se.tetris.team3.ui;

import se.tetris.team3.core.Settings;
import se.tetris.team3.store.SettingsStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class AppFrame extends JFrame {
    private final Settings settings = new Settings();
    private Screen current;

    public AppFrame() {
        // 설정 로드
        SettingsStore.load(settings);

        setTitle("TETRIS");
        Dimension win = settings.resolveWindowSize();
        setSize(win.width, win.height);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 키 입력을 현재 화면으로 전달
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (current != null) current.onKeyPressed(e);
                repaint();
            }
        });

        // 첫 화면: 메뉴
        showScreen(new MenuScreen(this));
    }

    public Settings getSettings() {
        return settings;
    }

    // 이전 화면 onHide() -> 새 화면 설정 -> 새 화면 onShow() -> 다시 그리기
    public void showScreen(Screen next) {
        if (current != null) current.onHide();
        current = next;
        current.onShow();
        repaint(); // 새 화면을 그리도록 요청
    }

    // 현재 Screen의 render()로 위임
    @Override public void paint(Graphics g) {
        super.paint(g);
        if (current != null) current.render((Graphics2D) g);
    }
}
