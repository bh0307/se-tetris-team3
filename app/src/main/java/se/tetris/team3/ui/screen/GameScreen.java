package se.tetris.team3.ui.screen;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.gameManager.ScoreManager;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.render.PatternPainter;

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
    // 배경 음악 재생
    app.getAudioManager().playGameMusic();
    
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

    // 클래식 아케이드 스타일 배경
    private void drawClassicBackground(Graphics2D g2) {
        int width = app.getWidth();
        int height = app.getHeight();
        
        // 진한 보라-파랑 그라데이션 배경
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(25, 0, 51),
            0, height, new Color(0, 20, 80)
        );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);
        
        // 큰 별들 (반짝임 효과)
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 30; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            int size = rand.nextInt(3) + 3; // 3-5픽셀
            
            // 별 중심 (밝은 노란색)
            g2.setColor(new Color(255, 255, 200, 220));
            g2.fillOval(x - size/2, y - size/2, size, size);
            
            // 반짝임 효과 (주변에 하얀 빛)
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillOval(x - size, y - size, size * 2, size * 2);
        }
        
        // 작은 별들
        for (int i = 0; i < 80; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRect(x, y, 2, 2);
        }
        
        // 네온 스트라이프 패턴 (좌우 양쪽)
        g2.setColor(new Color(255, 0, 150, 30));
        for (int i = 0; i < height; i += 40) {
            g2.fillRect(0, i, 30, 20);
            g2.fillRect(width - 30, i + 20, 30, 20);
        }
        
        // 분홍-파랑 네온 라인
        g2.setColor(new Color(0, 255, 255, 50));
        g2.setStroke(new BasicStroke(3));
        for (int i = 0; i < 5; i++) {
            int y = rand.nextInt(height);
            g2.drawLine(0, y, width, y);
        }
        g2.setStroke(new BasicStroke(1));
    }
    
    // 클래식 아케이드 스타일 게임판
    private void drawClassicBoard(Graphics2D g2, int padding, int blockSize, int blockSizeH) {
        int boardWidth = blockSize * 10;
        int boardHeight = blockSizeH * 20;
        
        // 게임판 내부 어두운 배경
        g2.setColor(new Color(0, 0, 30));
        g2.fillRect(padding, padding, boardWidth, boardHeight);
        
        // 장식적인 테두리 (여러겹)
        // 바깥쪽 금색 테두리
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(padding - 8, padding - 8, boardWidth + 16, boardHeight + 16);
        
        // 중간 빨간색 테두리
        g2.setColor(new Color(220, 20, 60));
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(padding - 4, padding - 4, boardWidth + 8, boardHeight + 8);
        
        // 안쪽 흰색 테두리
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(padding, padding, boardWidth, boardHeight);
        
        // 그리드 라인 (연한 파란색)
        g2.setColor(new Color(70, 130, 180, 30));
        g2.setStroke(new BasicStroke(1));
        for (int i = 1; i < 10; i++) {
            int x = padding + i * blockSize;
            g2.drawLine(x, padding, x, padding + boardHeight);
        }
        for (int i = 1; i < 20; i++) {
            int y = padding + i * blockSizeH;
            g2.drawLine(padding, y, padding + boardWidth, y);
        }
        
        g2.setStroke(new BasicStroke(1));
    }
    
    // 다음 순위까지 남은 점수 표시
    private void drawNextRankInfo(Graphics2D g2) {
        ScoreManager sm = new ScoreManager();
        GameMode mode = manager.getMode();
        int currentScore = manager.getScore();
        
        java.util.List<ScoreManager.ScoreEntry> highScores = sm.getHighScores(mode);
        
        String msg;
        Color bgColor;
        Color textColor;
        String icon;
        
        if (highScores.isEmpty()) {
            // 랭킹이 없으면 1등 되라고 표시
            msg = "첫 기록을 세워보세요!";
            bgColor = new Color(255, 215, 0, 200); // 금색 배경
            textColor = new Color(139, 69, 19); // 갈색 텍스트
            icon = "★";
        } else {
            // 현재 점수가 랭킹에 들어갈 위치 찾기
            int myRank = -1;
            for (int i = 0; i < highScores.size(); i++) {
                if (currentScore > highScores.get(i).getScore()) {
                    myRank = i + 1; // 1등, 2등, 3등... (1-based)
                    break;
                }
            }
            
            if (myRank == -1) {
                // 현재 최하위보다 낮음
                if (highScores.size() < 10) {
                    // 10등 안에 들 수 있음
                    int lastScore = highScores.get(highScores.size() - 1).getScore();
                    int needed = lastScore - currentScore + 1;
                    msg = String.format("%,d점 더 얻으면 %d등!", needed, highScores.size() + 1);
                } else {
                    // 10등까지 다 찼고, 10등보다 낮음
                    int tenthScore = highScores.get(9).getScore();
                    int needed = tenthScore - currentScore + 1;
                    msg = String.format("%,d점 더 얻으면 10등!", needed);
                }
                bgColor = new Color(100, 149, 237, 200); // 하늘색 배경
                textColor = Color.WHITE;
                icon = "↑";
            } else if (myRank == 1) {
                // 1등 중
                msg = "현재 1등! 계속 유지하세요!";
                bgColor = new Color(255, 215, 0, 200); // 금색 배경
                textColor = new Color(139, 69, 19); // 갈색 텍스트
                icon = "👑";
            } else {
                // 2등 이상
                int prevScore = highScores.get(myRank - 2).getScore();
                int needed = prevScore - currentScore + 1;
                msg = String.format("%,d점 더 얻으면 %d등!", needed, myRank - 1);
                bgColor = new Color(50, 205, 50, 200); // 초록색 배경
                textColor = Color.WHITE;
                icon = "▲";
            }
        }
        
        // 게임판 바로 아래에 표시
        int blockSize = settings.resolveBlockSize();
        int blockSizeH = (int)(blockSize * 1.15);
        int padding = 18;
        int yPos = padding + blockSizeH * 20 + 10; // padding + 게임판 높이 + 10px
        
        // 배경 박스 그리기
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        int msgWidth = g2.getFontMetrics().stringWidth(msg);
        int iconWidth = g2.getFontMetrics().stringWidth(icon + " ");
        int totalWidth = iconWidth + msgWidth + 18; // 여백 포함
        int boxHeight = 28;
        int xPos = padding + blockSize * 10 + 10; // 게임판 오른쪽 끝 + 10px
        
        // 그림자 효과
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(xPos + 3, yPos - 20 + 3, totalWidth, boxHeight, 15, 15);
        
        // 배경 박스
        g2.setColor(bgColor);
        g2.fillRoundRect(xPos, yPos - 20, totalWidth, boxHeight, 15, 15);
        
        // 테두리
        g2.setColor(new Color(255, 255, 255, 150));
        g2.setStroke(new java.awt.BasicStroke(2));
        g2.drawRoundRect(xPos, yPos - 20, totalWidth, boxHeight, 15, 15);
        g2.setStroke(new java.awt.BasicStroke(1));
        
        // 아이콘과 텍스트
        g2.setColor(textColor);
        g2.drawString(icon, xPos + 10, yPos);
        g2.drawString(msg, xPos + 10 + iconWidth, yPos);
    }

    @Override
    public void render(Graphics2D g2) {
        int blockSize = settings.resolveBlockSize();
        int blockSizeH = (int)(blockSize * 1.15); // 세로 길이 15% 증가
        int padding = 18;

        // 클래식 모드면 아케이드 스타일 배경, 아니면 검정색
        if (manager.getMode() == GameMode.CLASSIC || manager.getMode() == GameMode.ITEM) {
            drawClassicBackground(g2);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, app.getWidth(), app.getHeight());
        }

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

        // 클래식 모드면 아케이드 스타일 보드, 아니면 기본 테두리
        if (manager.getMode() == GameMode.CLASSIC || manager.getMode() == GameMode.ITEM) {
            drawClassicBoard(g2, padding, blockSize, blockSizeH);
        } else {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(padding, padding, blockSize * 10, blockSizeH * 20);
            g2.setStroke(new BasicStroke(1));
        }

        alignSpawnIfNewBlock();

        // 고정 블록
        for (int r = 0; r < REGION_ROWS; r++) {
            for (int c = 0; c < REGION_COLS; c++) {
                if (manager.getFieldValue(r, c) != 0) {
                    int x = padding + c * blockSize;
                    int y = padding + r * blockSizeH;
                    
                    // 플래시 효과: 해당 줄이 깨지기 직전이면 하얗게 렌더링
                    if (manager.isRowFlashing(r)) {
                        g2.setColor(Color.WHITE);
                        g2.fillRect(x, y, blockSize, blockSizeH);
                        g2.setColor(Color.LIGHT_GRAY);
                        g2.drawRect(x, y, blockSize - 1, blockSizeH - 1);
                    } else {
                        Color blockColor = manager.getBlockColor(r, c);
                        if (blockColor == null) blockColor = Color.GRAY;
                        PatternPainter.drawCellRect(g2, x, y, blockSize, blockSizeH, blockColor, null, settings.isColorBlindMode());
                        
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
                                int y = padding + gy * blockSizeH;
                                PatternPainter.drawCellRect(g2, x, y, blockSize, blockSizeH, ghostColor, cur, settings.isColorBlindMode());
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
                                int y = padding + gy * blockSizeH;
                                PatternPainter.drawCellRect(g2, x, y, blockSize, blockSizeH, base, cur, settings.isColorBlindMode());
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
            
            // 하단에 다음 순위까지 남은 점수 표시
            drawNextRankInfo(g2);
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
                app.showScreen(new ScoreboardScreen(app, score, sm));
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
