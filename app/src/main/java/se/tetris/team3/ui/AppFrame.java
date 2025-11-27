// 메인 윈도우 + 화면 전환기
package se.tetris.team3.ui;

import se.tetris.team3.core.Settings;
import se.tetris.team3.store.SettingsStore;
import se.tetris.team3.ui.screen.MenuScreen;
import se.tetris.team3.ui.screen.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class AppFrame extends JFrame {
    private final Settings settings = new Settings();
    private Screen current;
    private BackBufferPanel canvas; // 서보성 추가

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
        
        canvas = new BackBufferPanel(this);
        setContentPane(canvas);
        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ev -> {
            if (ev.getID() == KeyEvent.KEY_PRESSED) {
                if (current != null) current.onKeyPressed((KeyEvent) ev);
                repaint();
            }
            return false; // 다른 컴포넌트도 키를 받을 수 있도록 소비하지 않음
        });

        //--------------------------

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


    /** 내부 백버퍼 패널: 깜빡임 방지 */
    private static final class BackBufferPanel extends JPanel {
        private final AppFrame app;
        private Image back;

        BackBufferPanel(AppFrame app) {
            this.app = app;
            setOpaque(true);
            setBackground(Color.BLACK);
            setDoubleBuffered(true);
        }

        private void ensureBack() {
            int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
            if (back == null || back.getWidth(null) != w || back.getHeight(null) != h) {
                back = createImage(w, h);
            }
        }

        @Override public void update(Graphics g) { paint(g); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ensureBack();
            Graphics2D g2 = (Graphics2D) back.getGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // clear
                g2.setColor(getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                // delegate render
                Screen cur = app.current;
                if (cur != null) cur.render(g2);
            } finally {
                g2.dispose();
            }
            g.drawImage(back, 0, 0, null);
            Toolkit.getDefaultToolkit().sync(); // 일부 환경에서 티어링 감소
        }
    }
}
