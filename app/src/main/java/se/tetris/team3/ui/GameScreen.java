package se.tetris.team3.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.core.Settings;
import se.tetris.team3.ui.score.ScoreManager;

// 키 입력/타이머/렌더링, 일시정지, 게임오버 처리
public class GameScreen implements Screen {

    private final AppFrame app;
    private final Settings settings;
    private final GameManager manager;
    private Timer timer;

    private static final int REGION_COLS = 10;
    private static final int REGION_ROWS = 20;

    private Block lastBlockRef = null;
    private boolean isPaused = false;

    public GameScreen(AppFrame app) { this(app, new GameManager()); }

    public GameScreen(AppFrame app, GameManager manager) {
        this.app = app;
        this.manager = manager;
        this.settings = app.getSettings();
        this.manager.attachSettings(app.getSettings());
    }

    @Override public void onShow() {
    // 게임 로직 타이머
    timer = new Timer(1000, new ActionListener() {
        @Override public void actionPerformed(ActionEvent e) {
            if (!manager.isGameOver() && !isPaused) {
                manager.stepDownOrFix();
                updateTimerDelay();
            } else {
                if (timer != null) timer.stop();
            }
        }
    });
    
    // 렌더링 전용 타이머
    Timer renderTimer = new Timer(16, new ActionListener() { // 16ms = 60FPS
        @Override public void actionPerformed(ActionEvent e) {
            if (!isPaused) {
                manager.updateParticles();
                app.repaint();
            }
        }
    });
    
    timer.start();
    renderTimer.start();
}


    @Override public void onHide() { if (timer != null) timer.stop(); }

    private void updateTimerDelay() {
        // GameManager에서 설정된 난이도 기반 기본 낙하 딜레이를 사용
        int base = manager.getBaseFallDelay();
        int lvl = Math.max(1, manager.getLevel());
        // 레벨이 올라갈수록 delay를 줄임 (레벨당 100ms 감소). 최소값은 50ms로 제한.
        int delay = Math.max(50, base - (lvl - 1) * 100);
        timer.setDelay(delay);
    }

    private boolean fitsRegion(int gx, int gy, int[][] shape) {
        if (shape == null) return false;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int x = gx + c, y = gy + r;
                    if (x < 0 || x >= REGION_COLS) return false;
                    if (y < 0 || y >= REGION_ROWS) return false;
                }
            }
        }
        return true;
    }

    private static int bottomRowIndex(int[][] shape) {
        for (int r = shape.length - 1; r >= 0; r--)
            for (int c = 0; c < shape[r].length; c++)
                if (shape[r][c] != 0) return r;
        return shape.length - 1;
    }

    private void alignSpawnIfNewBlock() {
        Block cur = manager.getCurrentBlock();
        if (cur == null || cur == lastBlockRef) return;

        int[][] s = cur.getShape();
        int w = s[0].length;
        int br = bottomRowIndex(s);

        int x = manager.getBlockX();
        int minX = 0, maxX = Math.max(0, REGION_COLS - w);
        if (x < minX || x > maxX) x = (REGION_COLS - w) / 2;

        int y = -br;
        manager.tryMove(x, y);
        lastBlockRef = cur;
    }

    // 셀 중앙에 문자 그리기 (L 표시에 사용)
    public static void drawCenteredChar(Graphics2D g2, int x, int y, int size, char ch) {
        Font old = g2.getFont();
        Object aa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Monospaced", Font.BOLD, Math.max(10, (int)(size * 0.6))));
        String s = String.valueOf(ch);
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (size - fm.stringWidth(s)) / 2;
        int ty = y + (size + fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(new Color(0,0,0,180)); // 외곽
        for (int dx=-1; dx<=1; dx++) for (int dy=-1; dy<=1; dy++) {
            if (dx==0 && dy==0) continue; g2.drawString(s, tx+dx, ty+dy);
        }
        g2.setColor(new Color(255,255,255,230));
        g2.drawString(s, tx, ty);

        g2.setFont(old);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
    }

    @Override
    public void render(Graphics2D g2) {
        int blockSize = settings.resolveBlockSize();
        int padding = 18;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, app.getWidth(), app.getHeight());

        if (isPaused) {
            int width = app.getWidth(), height = app.getHeight();
            g2.setColor(new Color(255,255,255,160));
            g2.fillRect(0,0,width,height);
            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 72));
            String msg = "PAUSED";
            int msgWidth = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (width - msgWidth)/2, height/2);
            return;
        }

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(padding, padding, blockSize * 10, blockSize * 20);
        g2.setStroke(new BasicStroke(1));

        alignSpawnIfNewBlock();

        // 고정 블록
        for (int r = 0; r < REGION_ROWS; r++) {
            for (int c = 0; c < REGION_COLS; c++) {
                if (manager.getFieldValue(r, c) != 0) {
                    int x = padding + c * blockSize;
                    int y = padding + r * blockSize;
                    PatternPainter.drawCell(g2, x, y, blockSize, Color.GRAY, null, settings.isColorBlindMode());
                    
                    // 고정된 블록에 아이템이 있으면 글자 표시
                    if (manager.hasItem(r, c)) {
                        char itemType = manager.getItemType(r, c);
                        drawCenteredChar(g2, x, y, blockSize, itemType);
                    }
                }
            }
        }

        // 현재 블록
        if (!manager.isGameOver()) {
            Block cur = manager.getCurrentBlock();
            if (cur != null) {
                int[][] shape = cur.getShape();
                Color base = cur.getColor();
                int bx = manager.getBlockX(), by = manager.getBlockY();

                // L 위치 (있을 때만 사용)
                Integer ir = null, ic = null;
                if (cur.getItemType() != 0) {
                    try {
                        ir = (Integer) cur.getClass().getMethod("getItemRow").invoke(cur);
                        ic = (Integer) cur.getClass().getMethod("getItemCol").invoke(cur);
                    } catch (Exception ignore) {}
                }

                for (int r = 0; r < shape.length; r++) {
                    for (int c = 0; c < shape[r].length; c++) {
                        if (shape[r][c] != 0) {
                            int gx = bx + c, gy = by + r;
                            if (gx>=0 && gx<REGION_COLS && gy>=0 && gy<REGION_ROWS) {
                                int x = padding + gx * blockSize;
                                int y = padding + gy * blockSize;
                                PatternPainter.drawCell(g2, x, y, blockSize, base, cur, settings.isColorBlindMode());

                                // ★ 줄삭제 L은 붙은 칸에만 문자 오버레이(무게추는 글자 없음)
                                if (cur.getItemType() != 0 && ir != null && ic != null && r == ir && c == ic) {
                                    drawCenteredChar(g2, x, y, blockSize, cur.getItemType());
                                }
                            }
                        }
                    }
                }
            }
            int width = app.getWidth();
            manager.renderHUD(g2, padding, blockSize, width);
            // 파티클 효과 렌더링
            manager.renderParticles(g2, blockSize);
        } else {
            // GAME OVER
            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 48));
            String msg = "GAME OVER";
            FontMetrics fm = g2.getFontMetrics();
            int x = (app.getWidth() - fm.stringWidth(msg)) / 2;
            int y = app.getHeight() / 2 - 50;
            g2.drawString(msg, x, y);

            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2.setColor(Color.YELLOW);
            String scoreMsg = "Your Score: " + manager.getScore();
            int sx = (app.getWidth() - g2.getFontMetrics().stringWidth(scoreMsg)) / 2;
            int sy = y + 50;
            g2.drawString(scoreMsg, sx, sy);

            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            String hint = "Press any key";
            int hintWidth = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, (app.getWidth() - hintWidth) / 2, sy + 40);
        }
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        Block cur = manager.getCurrentBlock();
        int[][] shape = (cur != null ? cur.getShape() : null);

        if (manager.isGameOver()) {
            ScoreManager sm = new ScoreManager();
            var mode = manager.getMode();
            var score = manager.getScore();

            // 최고 점수이면 이름 입력 화면으로
            if (sm.isHighScore(mode, score)) {
                // 기존: app.showScreen(new NameInputScreen(app, manager.getScore()));
                // 수정: 모드 포함 버전
                app.showScreen(new NameInputScreen(app, mode, score));
            }
            return;
        }

        if (code == KeyEvent.VK_SPACE) {
            isPaused = !isPaused;
            if (!isPaused) {
                if (manager.isGameOver()) { if (timer != null) timer.stop(); }
                else {
                    manager.stepDownOrFix();
                    updateTimerDelay();
                    if (timer != null && !timer.isRunning()) timer.start();
                }
            } else { if (timer != null) timer.stop(); }
            app.repaint(); return;
        }
        if (isPaused) return;

        final var km = settings.getKeymap();

        if (code == km.get(se.tetris.team3.core.Settings.Action.MOVE_LEFT)) {
            if (shape != null) manager.tryMove(manager.getBlockX() - 1, manager.getBlockY());
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.MOVE_RIGHT)) {
            if (shape != null) manager.tryMove(manager.getBlockX() + 1, manager.getBlockY());
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.ROTATE)) {
            if (shape != null) {
                int sh = shape.length, sw = shape[0].length;
                int[][] rotated = new int[sw][sh];
                for (int r = 0; r < sh; r++)
                    for (int c = 0; c < sw; c++)
                        rotated[c][sh - 1 - r] = shape[r][c];
                int bx = manager.getBlockX(), by = manager.getBlockY();
                if (fitsRegion(bx, by, rotated)) manager.rotateBlock();
                else {
                    int[] kicks = {-1, 1, -2, 2};
                    for (int dx : kicks) {
                        if (fitsRegion(bx + dx, by, rotated) && manager.tryMove(bx + dx, by)) { manager.rotateBlock(); break; }
                    }
                }
            }
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.SOFT_DROP)) {
            if (shape != null) manager.tryMove(manager.getBlockX(), manager.getBlockY() + 1);
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.EXIT)) {
            app.showScreen(new MenuScreen(app));
        }

        app.repaint();
    }
}
