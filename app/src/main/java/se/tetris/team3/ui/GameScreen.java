package se.tetris.team3.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Map;
import se.tetris.team3.core.GameMode;
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
                manager.updateSlowMode(); // 느린 모드 상태 업데이트
                manager.autoCheckLines(); // 자동 라인 체크 (연쇄 제거)
                app.repaint();
            }
        }
    });
    
    timer.start();
    renderTimer.start();
}


    @Override public void onHide() { if (timer != null) timer.stop(); }

    private void updateTimerDelay() {
        // GameManager에서 느린 모드를 반영한 딜레이 사용
        int delay = manager.getGameTimerDelay();
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
            
            // 안내 메시지 - 설정된 키 표시
            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g2.setColor(Color.DARK_GRAY);
            String pauseKey = KeyEvent.getKeyText(settings.getKeymap().get(Settings.Action.PAUSE));
            String exitKey = KeyEvent.getKeyText(settings.getKeymap().get(Settings.Action.EXIT));
            String hint = pauseKey + " 계속   " + exitKey + " 종료";
            int hintWidth = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, (width - hintWidth)/2, height/2 + 60);
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
                    
                    // 플래시 효과: 해당 줄이 깨지기 직전이면 하얗게 렌더링
                    if (manager.isRowFlashing(r)) {
                        g2.setColor(Color.WHITE);
                        g2.fillRect(x, y, blockSize, blockSize);
                        g2.setColor(Color.LIGHT_GRAY);
                        g2.drawRect(x, y, blockSize - 1, blockSize - 1);
                    } else {
                        PatternPainter.drawCell(g2, x, y, blockSize, Color.GRAY, null, settings.isColorBlindMode());
                        
                        // 고정된 블록에 아이템이 있으면 글자 표시
                        if (manager.hasItem(r, c)) {
                            char itemType = manager.getItemType(r, c);
                            drawCenteredChar(g2, x, y, blockSize, itemType);
                        }
                    }
                }
            }
        }

        // 현재 블록 및 고스트 블록(하드 드롭 위치 미리보기)
        if (!manager.isGameOver()) {
            Block cur = manager.getCurrentBlock();
            if (cur != null) {
                int[][] shape = cur.getShape();
                Color base = cur.getColor();
                int bx = manager.getBlockX(), by = manager.getBlockY();

                // 1. 하드 드롭 위치 계산
                int ghostY = by;
                while (true) {
                    boolean canMove = true;
                    for (int r = 0; r < shape.length; r++) {
                        for (int c = 0; c < shape[r].length; c++) {
                            if (shape[r][c] != 0) {
                                int testY = ghostY + r + 1;
                                int testX = bx + c;
                                if (testY >= REGION_ROWS || manager.getFieldValue(testY, testX) != 0) {
                                    canMove = false;
                                    break;
                                }
                            }
                        }
                        if (!canMove) break;
                    }
                    if (!canMove) break;
                    ghostY++;
                }

                // 2. 고스트 블록(연한 색) 먼저 그림
                Color ghostColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), 80); // 투명도 적용
                for (int r = 0; r < shape.length; r++) {
                    for (int c = 0; c < shape[r].length; c++) {
                        if (shape[r][c] != 0) {
                            int gx = bx + c, gy = ghostY + r;
                            if (gx>=0 && gx<REGION_COLS && gy>=0 && gy<REGION_ROWS) {
                                int x = padding + gx * blockSize;
                                int y = padding + gy * blockSize;
                                PatternPainter.drawCell(g2, x, y, blockSize, ghostColor, cur, settings.isColorBlindMode());
                            }
                        }
                    }
                }

                // 3. 실제 블록 그림
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
            manager.renderParticles(g2, padding, padding, blockSize);
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
            GameMode mode = manager.getMode();
            int score = manager.getScore();

            // 최고 점수이면 이름 입력 화면으로
            if (sm.isHighScore(mode, score)) {
                app.showScreen(new NameInputScreen(app, mode, score));
            } else {
                // 최고 점수가 아니면 바로 스코어보드로
                app.showScreen(new se.tetris.team3.ui.score.ScoreboardScreen(app, score, sm));
            }
            return;
        }

        final Map<Settings.Action, Integer> km = settings.getKeymap();

        if (code == km.get(se.tetris.team3.core.Settings.Action.PAUSE)) {
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
        
        // 일시정지 중 ESC로 게임 종료
        if (isPaused && code == km.get(se.tetris.team3.core.Settings.Action.EXIT)) {
            app.showScreen(new MenuScreen(app));
            return;
        }
        
        if (isPaused) return;

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
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.HARD_DROP)) {
            if (shape != null) manager.hardDrop();
        } else if (code == km.get(se.tetris.team3.core.Settings.Action.EXIT)) {
            app.showScreen(new MenuScreen(app));
        }

        app.repaint();
    }
}
