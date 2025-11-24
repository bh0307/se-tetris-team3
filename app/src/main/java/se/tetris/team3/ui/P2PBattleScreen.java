package se.tetris.team3.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Timer;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.net.P2PConnectionListener;
import se.tetris.team3.net.P2PMessage;

/**
 * P2P 대전 게임 화면
 * - 왼쪽: 내 보드 + NEXT + GARBAGE + 파티클
 * - 오른쪽: 상대 보드 + NEXT + GARBAGE + (remoteField diff 기반 파티클)
 */
public class P2PBattleScreen implements Screen, P2PConnectionListener {

    private final AppFrame frame;
    private final Settings settings;
    private final GameMode battleMode;
    private final int timeLimitSeconds;
    private final boolean asServer;
    private final P2PConnection connection;

    private final GameManager myManager;

    // 레이아웃
    private int blockSize;
    private int boardWidth;
    private int boardHeight;
    private int centerGap;
    private int topMargin;

    // 타이머
    private Timer gameTimer;
    private Timer dropTimer;
    private Timer stateSendTimer;
    private long lastDrop;

    // 시간제한 모드
    private final boolean isTimeAttack;
    private final long timeLimitMillis;
    private long startTime;

    private boolean gameOver = false;
    private int winner = 0; // 0: 진행/무승부, 1: 내 승, 2: 상대 승
    private boolean paused = false;
    private String lagMessage = "";

    // ────────── 상대 상태 스냅샷 ──────────
    private volatile int remoteScore = 0;
    private volatile int remoteLevel = 1;
    private volatile boolean remoteGameOver = false;

    private volatile int[][] remoteField;        // [20][10]
    private volatile char[][] remoteItemField;   // [20][10]

    private volatile int[][] remoteCurShape;
    private volatile Color remoteCurColor;
    private volatile int remoteCurX, remoteCurY;
    private volatile char remoteCurItemType;
    private volatile int remoteCurItemRow, remoteCurItemCol;

    private volatile int[][] remoteNextShape;
    private volatile Color remoteNextColor;
    private volatile char remoteNextItemType;
    private volatile int remoteNextItemRow, remoteNextItemCol;

    private volatile boolean[][] remoteGarbagePreview;

    // diff 비교용 이전 필드
    private int[][] prevRemoteField;

    // 상대 보드용 파티클
    private final List<Particle> remoteParticles =
            Collections.synchronizedList(new ArrayList<>());

    public P2PBattleScreen(AppFrame frame,
                           P2PConnection connection,
                           GameMode mode,
                           Settings settings,
                           int timeLimitSeconds,
                           boolean asServer) {
        this.frame = frame;
        this.connection = connection;
        this.settings = settings;
        this.battleMode = mode;
        this.timeLimitSeconds = timeLimitSeconds;
        this.asServer = asServer;

        this.isTimeAttack = (mode == GameMode.BATTLE_TIME);
        this.timeLimitMillis = timeLimitSeconds * 1000L;

        GameMode internalMode =
                (mode == GameMode.BATTLE_ITEM) ? GameMode.ITEM : GameMode.CLASSIC;
        myManager = new GameManager(internalMode);
        if (settings != null) myManager.attachSettings(settings);

        // 대전 공격: 공격 줄 네트워크로 전송
        myManager.setLineClearListener((gm, clearedRows, garbageRows) -> {
            if (garbageRows != null && garbageRows.length > 0) {
                connection.send(P2PMessage.attack(garbageRows));
            }
        });

        // 이 화면이 P2P 콜백 받도록 등록
        connection.setListener(this);
    }

    @Override
    public void onShow() {
        startTime = System.currentTimeMillis();
        lastDrop = System.currentTimeMillis();

        // 게임 로직 및 애니메이션
        gameTimer = new Timer(16, e -> {
            if (paused || gameOver) return;

            myManager.updateSlowMode();
            myManager.updateParticles();   // 내쪽 파티클
            updateRemoteParticles();       // 상대쪽 파티클

            myManager.autoCheckLines();

            // 시간제한 모드
            if (isTimeAttack) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= timeLimitMillis && !gameOver) {
                    gameOver = true;
                    int myScore = myManager.getScore();
                    if (myScore > remoteScore)      winner = 1;
                    else if (myScore < remoteScore) winner = 2;
                    else                            winner = 0;
                }
            }

            // 내 게임오버
            if (myManager.isGameOver() && !gameOver) {
                gameOver = true;
                winner = remoteGameOver ? 0 : 2;
            }

            frame.repaint();
        });
        gameTimer.start();

        // 자동 낙하 타이머
        dropTimer = new Timer(50, e -> {
            if (paused || gameOver || myManager.isGameOver()) return;
            long now = System.currentTimeMillis();
            if (now - lastDrop >= myManager.getGameTimerDelay()) {
                myManager.stepDownOrFix();
                lastDrop = now;
            }
        });
        dropTimer.start();

        // 내 상태 STATE 메시지 주기적으로 전송
        stateSendTimer = new Timer(100, e -> sendStateSnapshot());
        stateSendTimer.start();
    }

    @Override
    public void onHide() {
        if (gameTimer != null) gameTimer.stop();
        if (dropTimer != null) dropTimer.stop();
        if (stateSendTimer != null) stateSendTimer.stop();
    }

    // ────────── 내 상태를 STATE 메시지로 전송 ──────────
    private void sendStateSnapshot() {
        if (connection == null) return;

        P2PMessage msg = P2PMessage.emptyState();
        msg.myScore = myManager.getScore();
        msg.myLevel = myManager.getLevel();
        msg.gameOver = myManager.isGameOver();

        // 필드 + 아이템
        int h = 20, w = 10;
        msg.field = new int[h][w];
        msg.itemField = new char[h][w];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                msg.field[r][c] = myManager.getFieldValue(r, c);
                msg.itemField[r][c] = myManager.getItemType(r, c);
            }
        }

        // 현재 블록
        if (!myManager.isGameOver() && myManager.getCurrentBlock() != null) {
            var cur = myManager.getCurrentBlock();
            msg.curShape = deepCopy(cur.getShape());
            msg.curColor = cur.getColor();
            msg.curX = myManager.getBlockX();
            msg.curY = myManager.getBlockY();
            msg.curItemType = cur.getItemType();
            msg.curItemRow = -1;
            msg.curItemCol = -1;
            if (msg.curItemType != 0) {
                try {
                    msg.curItemRow = (Integer) cur.getClass()
                            .getMethod("getItemRow").invoke(cur);
                    msg.curItemCol = (Integer) cur.getClass()
                            .getMethod("getItemCol").invoke(cur);
                } catch (Exception ignore) {}
            }
        }

        // NEXT 블록
        if (myManager.getNextBlock() != null) {
            var nb = myManager.getNextBlock();
            msg.nextShape = deepCopy(nb.getShape());
            msg.nextColor = nb.getColor();
            msg.nextItemType = nb.getItemType();
            msg.nextItemRow = -1;
            msg.nextItemCol = -1;
            if (msg.nextItemType != 0) {
                try {
                    msg.nextItemRow = (Integer) nb.getClass()
                            .getMethod("getItemRow").invoke(nb);
                    msg.nextItemCol = (Integer) nb.getClass()
                            .getMethod("getItemCol").invoke(nb);
                } catch (Exception ignore) {}
            }
        }

        // 쓰레기 줄 큐
        List<boolean[]> queue = myManager.getPendingGarbagePreview();
        msg.garbagePreview = new boolean[queue.size()][];
        for (int i = 0; i < queue.size(); i++) {
            boolean[] row = queue.get(i);
            msg.garbagePreview[i] = (row != null ? row.clone() : null);
        }

        connection.send(msg);
    }

    private static int[][] deepCopy(int[][] src) {
        if (src == null) return null;
        int[][] out = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            out[i] = src[i].clone();
        }
        return out;
    }

    // ────────── P2PConnectionListener 구현 ──────────

    @Override
    public void onConnected(boolean asServer) {
        // 이미 로비에서 연결된 상태라 여기서는 할 것 없음
    }

    @Override
    public void onDisconnected(String reason) {
        gameOver = true;
        winner = 1; // 상대가 나가면 내가 이긴 걸로 처리
        lagMessage = "연결 끊김: " + reason;
    }

    @Override
    public void onMessageReceived(P2PMessage msg) {
        switch (msg.type) {
            case ATTACK:
                if (msg.garbageRows != null && !gameOver) {
                    myManager.enqueueGarbage(msg.garbageRows);
                }
                break;

            case STATE:
                // 1. 이전 필드를 백업
                int[][] old = remoteField == null ? null : deepCopy(remoteField);

                // 2. 새 상태 반영
                remoteScore = msg.myScore;
                remoteLevel = msg.myLevel;
                remoteGameOver = msg.gameOver;
                remoteField = msg.field;
                remoteItemField = msg.itemField;
                remoteCurShape = msg.curShape;
                remoteCurColor = msg.curColor;
                remoteCurX = msg.curX;
                remoteCurY = msg.curY;
                remoteCurItemType = msg.curItemType;
                remoteCurItemRow = msg.curItemRow;
                remoteCurItemCol = msg.curItemCol;
                remoteNextShape = msg.nextShape;
                remoteNextColor = msg.nextColor;
                remoteNextItemType = msg.nextItemType;
                remoteNextItemRow = msg.nextItemRow;
                remoteNextItemCol = msg.nextItemCol;
                remoteGarbagePreview = msg.garbagePreview;

                // 3. diff 기반 파티클 생성
                if (old != null && remoteField != null) {
                    spawnRemoteBreakParticlesFromDiff(old, remoteField);
                }

                // 상대가 먼저 죽은 경우
                if (remoteGameOver && !myManager.isGameOver() && !gameOver) {
                    gameOver = true;
                    winner = 1;
                }
                break;

            case LAG_WARNING:
                lagMessage = msg.text;
                break;

            case ERROR:
            case DISCONNECT:
                lagMessage = msg.text;
                break;

            default:
                // HELLO 등은 여기선 무시 (채팅 화면에서 처리)
                break;
        }
    }

    @Override
    public void onNetworkError(Exception e) {
        lagMessage = "네트워크 오류: " + e.getMessage();
    }

    // ────────── remoteField diff → 파티클 생성 ──────────

    private void spawnRemoteBreakParticlesFromDiff(int[][] oldField, int[][] newField) {
        int h = Math.min(oldField.length, newField.length);
        if (h == 0) return;
        int w = Math.min(oldField[0].length, newField[0].length);

        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (oldField[r][c] == 1 && newField[r][c] == 0) {
                    // (r,c) 블럭이 사라짐 → 파티클 생성
                    addRemoteBreakEffect(c, r);
                }
            }
        }
    }

    // GameManager 의 Particle 로직과 동일한 구조
    private static class Particle {
        float gridX, gridY;
        float offsetX, offsetY;
        float vx, vy;
        Color color;
        int life;
        int maxLife;

        public Particle(float gridX, float gridY, Color color,
                        float vx, float vy,
                        float offsetX, float offsetY) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.color = color;
            this.vx = vx;
            this.vy = vy;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.maxLife = 30 + (int)(Math.random() * 20);
            this.life = maxLife;
        }

        public void update() {
            offsetX += vx;
            offsetY += vy;
            vy += 0.2f;
            vx *= 0.98f;
            life--;
        }

        public boolean isDead() {
            return life <= 0;
        }

        public void render(Graphics2D g2, int originX, int originY, int blockSize) {
            if (isDead()) return;
            float alpha = (float) life / maxLife;
            Color fadeColor = new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    (int) (255 * alpha)
            );
            g2.setColor(fadeColor);
            int size = Math.max(1, (int) (4 * alpha));

            float px = originX + gridX * blockSize + (blockSize / 2.0f) + offsetX;
            float py = originY + gridY * blockSize + (blockSize / 2.0f) + offsetY;

            g2.fillOval((int) px, (int) py, size, size);
        }
    }

    private void addRemoteBreakEffect(int gridX, int gridY) {
        Color blockColor = Color.LIGHT_GRAY; // 상대는 그냥 회색 블럭 기준

        int particleCount = 8 + (int)(Math.random() * 5);
        for (int i = 0; i < particleCount; i++) {
            float angle = (float)(Math.random() * 2 * Math.PI);
            float speed = 2 + (float)(Math.random() * 4);

            float vx = (float)(Math.cos(angle) * speed);
            float vy = (float)(Math.sin(angle) * speed) - 1;

            float offsetX = -8 + (float)(Math.random() * 16);
            float offsetY = -8 + (float)(Math.random() * 16);

            remoteParticles.add(new Particle(
                    gridX,
                    gridY,
                    blockColor,
                    vx,
                    vy,
                    offsetX,
                    offsetY
            ));
        }
    }

    private void updateRemoteParticles() {
        synchronized (remoteParticles) {
            remoteParticles.removeIf(p -> {
                p.update();
                return p.isDead();
            });
        }
    }

    private void renderRemoteParticles(Graphics2D g2, int originX, int originY) {
        synchronized (remoteParticles) {
            for (Particle p : remoteParticles) {
                p.render(g2, originX, originY, blockSize);
            }
        }
    }

    // ────────── 입력 처리 ──────────

    @Override
    public void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameOver) {
            if (key == KeyEvent.VK_ESCAPE) {
                frame.showScreen(new MenuScreen(frame));
            } else if (key == KeyEvent.VK_ENTER) {
                // 다시 P2P 로비로
                frame.showScreen(new P2PLobbyScreen(frame, settings));
            }
            return;
        }

        if (key == KeyEvent.VK_P) {
            paused = !paused;
            return;
        }
        if (paused) return;

        switch (key) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                myManager.tryMove(myManager.getBlockX() - 1, myManager.getBlockY());
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                myManager.tryMove(myManager.getBlockX() + 1, myManager.getBlockY());
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                myManager.stepDownOrFix();
                lastDrop = System.currentTimeMillis();
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                myManager.rotateBlock();
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                myManager.hardDrop();
                lastDrop = System.currentTimeMillis();
                break;
            case KeyEvent.VK_ESCAPE:
                frame.showScreen(new MenuScreen(frame));
                break;
        }
    }

    // ────────── 렌더링 ──────────

    @Override
    public void render(Graphics2D g2) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);

        calculateLayout(width, height);

        int nextBoxWidth = (int) (blockSize * 3.5);
        int playerAreaWidth = boardWidth + nextBoxWidth;
        int totalWidth = playerAreaWidth * 2 + centerGap;
        int startX = (width - totalWidth) / 2;
        int boardY = topMargin;

        int myX = startX;
        int remoteX = startX + playerAreaWidth + centerGap;

        drawLocalSide(g2, myX, boardY);
        drawRemoteSide(g2, remoteX, boardY);

        drawCenterInfo(g2, width, height);
    }

    private void calculateLayout(int screenWidth, int screenHeight) {
        int usableWidth = screenWidth - 80;
        int usableHeight = screenHeight - 220;

        int blockSizeByHeight = usableHeight / 20;
        int blockSizeByWidth  = usableWidth / 30; // (보드+NEXT)*2 + gap 대략 30칸

        int maxFitSize = Math.min(blockSizeByHeight, blockSizeByWidth);
        int preferred = (settings != null ? settings.resolveBlockSize() : maxFitSize);
        blockSize = Math.max(12, Math.min(preferred, maxFitSize));

        boardWidth  = 10 * blockSize;
        boardHeight = 20 * blockSize;

        centerGap = Math.max(35, blockSize * 2);
        topMargin = Math.max(75, (screenHeight - boardHeight - 100) / 2);
    }

    // ────────── 왼쪽(내 보드) ──────────

    private void drawLocalSide(Graphics2D g2, int x, int y) {
        int nextX = x + boardWidth + 10;
        int nextTopY = y + blockSize;
        int garbageTopY = nextTopY + (int) (blockSize * 7);

        drawBoardBaseLocal(g2, x, y);
        drawNextBlockLocal(g2, nextX, nextTopY, myManager);
        drawGarbagePreviewLocal(g2, nextX, garbageTopY, myManager);

        // 조작 안내
        g2.setColor(Color.LIGHT_GRAY);
        int fontSize = Math.max(8, blockSize / 2);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, fontSize));
        int lineH = fontSize + 3;
        int yBase = y + boardHeight + 15;

        String c1 = "←/→ 또는 A/D : 이동";
        String c2 = "↑/W : 회전, ↓/S : 소프트 드롭";
        String c3 = "Space/Enter : 하드 드롭";

        g2.drawString(c1, x, yBase);
        g2.drawString(c2, x, yBase + lineH);
        g2.drawString(c3, x, yBase + lineH * 2);
    }

    private void drawBoardBaseLocal(Graphics2D g2, int x, int y) {
        String name = asServer ? "You (Server)" : "You (Client)";

        // 이름/점수/레벨
        g2.setColor(Color.CYAN);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(14, blockSize)));
        int nw = g2.getFontMetrics().stringWidth(name);
        g2.drawString(name, x + (boardWidth - nw) / 2, y - 45);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(12, blockSize * 3 / 4)));
        String scoreText = "Score: " + myManager.getScore();
        int sw = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, x + (boardWidth - sw) / 2, y - 25);

        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(10, blockSize * 2 / 3)));
        String levelText = "Level: " + myManager.getLevel();
        int lw = g2.getFontMetrics().stringWidth(levelText);
        g2.drawString(levelText, x + (boardWidth - lw) / 2, y - 10);

        // 보드 테두리
        g2.setColor(Color.GRAY);
        g2.drawRect(x, y, boardWidth, boardHeight);

        // 필드 + 아이템
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                int cellX = x + col * blockSize;
                int cellY = y + row * blockSize;

                if (myManager.isRowFlashing(row)) {
                    g2.setColor(Color.WHITE);
                    g2.fillRect(cellX, cellY, blockSize - 1, blockSize - 1);
                } else if (myManager.getFieldValue(row, col) == 1) {
                    g2.setColor(Color.DARK_GRAY);
                    g2.fillRect(cellX, cellY, blockSize - 1, blockSize - 1);
                    char itemType = myManager.getItemType(row, col);
                    if (itemType != 0) {
                        GameScreen.drawCenteredChar(g2, cellX, cellY, blockSize, itemType);
                    }
                }
            }
        }
        // 파티클
        myManager.renderParticles(g2, x, y, blockSize);
    }

    // ────────── 오른쪽(상대 보드) ──────────

    private void drawRemoteSide(Graphics2D g2, int x, int y) {
        int nextX = x + boardWidth + 10;
        int nextTopY = y + blockSize;
        int garbageTopY = nextTopY + (int) (blockSize * 7);

        drawBoardBaseRemote(g2, x, y);
        drawNextBlockRemote(g2, nextX, nextTopY);
        drawGarbagePreviewRemote(g2, nextX, garbageTopY);

        // 상대쪽 파티클
        renderRemoteParticles(g2, x, y);

        if (remoteGameOver) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(x, y, boardWidth, boardHeight);
            g2.setColor(Color.RED);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            String txt = "GAME OVER";
            int tw = g2.getFontMetrics().stringWidth(txt);
            g2.drawString(txt, x + (boardWidth - tw) / 2, y + boardHeight / 2);
        }
    }

    private void drawBoardBaseRemote(Graphics2D g2, int x, int y) {
        String name = asServer ? "Remote (Client)" : "Remote (Server)";

        g2.setColor(Color.PINK);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(14, blockSize)));
        int nw = g2.getFontMetrics().stringWidth(name);
        g2.drawString(name, x + (boardWidth - nw) / 2, y - 45);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(12, blockSize * 3 / 4)));
        String scoreText = "Score: " + remoteScore;
        int sw = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, x + (boardWidth - sw) / 2, y - 25);

        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(10, blockSize * 2 / 3)));
        String levelText = "Level: " + remoteLevel;
        int lw = g2.getFontMetrics().stringWidth(levelText);
        g2.drawString(levelText, x + (boardWidth - lw) / 2, y - 10);

        g2.setColor(Color.GRAY);
        g2.drawRect(x, y, boardWidth, boardHeight);

        if (remoteField != null) {
            for (int row = 0; row < 20; row++) {
                if (row >= remoteField.length) break;
                for (int col = 0; col < 10; col++) {
                    if (col >= remoteField[row].length) break;
                    if (remoteField[row][col] == 1) {
                        int cellX = x + col * blockSize;
                        int cellY = y + row * blockSize;
                        g2.setColor(Color.DARK_GRAY);
                        g2.fillRect(cellX, cellY, blockSize - 1, blockSize - 1);

                        if (remoteItemField != null &&
                            row < remoteItemField.length &&
                            col < remoteItemField[row].length) {
                            char itemType = remoteItemField[row][col];
                            if (itemType != 0) {
                                GameScreen.drawCenteredChar(g2, cellX, cellY, blockSize, itemType);
                            }
                        }
                    }
                }
            }
        }

        // 상대 현재 블록
        if (remoteCurShape != null && remoteCurColor != null && !remoteGameOver) {
            g2.setColor(remoteCurColor);
            for (int r = 0; r < remoteCurShape.length; r++) {
                for (int c = 0; c < remoteCurShape[r].length; c++) {
                    if (remoteCurShape[r][c] != 0) {
                        int gx = remoteCurX + c;
                        int gy = remoteCurY + r;
                        if (gx >= 0 && gx < 10 && gy >= 0 && gy < 20) {
                            int cellX = x + gx * blockSize;
                            int cellY = y + gy * blockSize;
                            g2.fillRect(cellX, cellY, blockSize - 1, blockSize - 1);
                            if (remoteCurItemType != 0 &&
                                r == remoteCurItemRow && c == remoteCurItemCol) {
                                GameScreen.drawCenteredChar(g2, cellX, cellY, blockSize, remoteCurItemType);
                            }
                        }
                    }
                }
            }
        }
    }

    // ────────── NEXT / GARBAGE (내 쪽) ──────────

    private void drawNextBlockLocal(Graphics2D g2, int nextX, int nextY, GameManager gm) {
        if (gm.getNextBlock() == null) return;

        int previewSize = (int) (blockSize * 3.5);
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, blockSize / 2)));
        String label = "NEXT";
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, nextX + (previewSize - lw) / 2, nextY - 5);

        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(nextX, nextY, previewSize, previewSize);

        var nb = gm.getNextBlock();
        int[][] shape = nb.getShape();
        Color color = nb.getColor();

        int nextBlockSize = (int) (blockSize * 0.75);
        int shapeW = shape[0].length;
        int shapeH = shape.length;
        int offsetX = (previewSize - shapeW * nextBlockSize) / 2;
        int offsetY = (previewSize - shapeH * nextBlockSize) / 2;

        Integer ir = null, ic = null;
        if (nb.getItemType() != 0) {
            try {
                ir = (Integer) nb.getClass().getMethod("getItemRow").invoke(nb);
                ic = (Integer) nb.getClass().getMethod("getItemCol").invoke(nb);
            } catch (Exception ignore) {}
        }

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int cellX = nextX + offsetX + c * nextBlockSize;
                    int cellY = nextY + offsetY + r * nextBlockSize;
                    g2.setColor(color);
                    g2.fillRect(cellX, cellY, nextBlockSize - 1, nextBlockSize - 1);
                    if (nb.getItemType() != 0 && ir != null && ic != null &&
                        r == ir && c == ic) {
                        GameScreen.drawCenteredChar(g2, cellX, cellY, nextBlockSize, nb.getItemType());
                    }
                }
            }
        }
    }

    private void drawGarbagePreviewLocal(Graphics2D g2, int x, int y, GameManager gm) {
        List<boolean[]> queue = gm.getPendingGarbagePreview();
        drawGarbagePreviewGeneric(g2, x, y, queue);
    }

    // ────────── NEXT / GARBAGE (상대 쪽) ──────────

    private void drawNextBlockRemote(Graphics2D g2, int nextX, int nextY) {
        if (remoteNextShape == null || remoteNextColor == null) return;

        int previewSize = (int) (blockSize * 3.5);
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, blockSize / 2)));
        String label = "NEXT";
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, nextX + (previewSize - lw) / 2, nextY - 5);

        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(nextX, nextY, previewSize, previewSize);

        int[][] shape = remoteNextShape;
        Color color = remoteNextColor;

        int nextBlockSize = (int) (blockSize * 0.75);
        int shapeW = shape[0].length;
        int shapeH = shape.length;
        int offsetX = (previewSize - shapeW * nextBlockSize) / 2;
        int offsetY = (previewSize - shapeH * nextBlockSize) / 2;

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int cellX = nextX + offsetX + c * nextBlockSize;
                    int cellY = nextY + offsetY + r * nextBlockSize;
                    g2.setColor(color);
                    g2.fillRect(cellX, cellY, nextBlockSize - 1, nextBlockSize - 1);
                    if (remoteNextItemType != 0 &&
                        r == remoteNextItemRow && c == remoteNextItemCol) {
                        GameScreen.drawCenteredChar(g2, cellX, cellY, nextBlockSize, remoteNextItemType);
                    }
                }
            }
        }
    }

    private void drawGarbagePreviewRemote(Graphics2D g2, int x, int y) {
        List<boolean[]> list = new ArrayList<>();
        if (remoteGarbagePreview != null) {
            for (boolean[] row : remoteGarbagePreview) list.add(row);
        }
        drawGarbagePreviewGeneric(g2, x, y, list);
    }

    private void drawGarbagePreviewGeneric(Graphics2D g2, int x, int y, List<boolean[]> queue) {
        final int cols = 10;
        final int rowsPreview = 10;

        int boxSize   = (int) (blockSize * 3.5);
        int boxWidth  = boxSize;
        int boxHeight = boxSize;

        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, blockSize / 2)));
        String label = "GARBAGE";
        int w = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, x + (boxWidth - w) / 2, y - 5);

        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(x, y, boxWidth, boxHeight);
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 1, y + 1, boxWidth - 2, boxHeight - 2);

        int cellH = boxHeight / rowsPreview;
        int gridHeight = cellH * rowsPreview;
        int verticalPadding = (boxHeight - gridHeight) / 2;
        double cellWf = (double) boxWidth / cols;

        int actualRows = (queue == null) ? 0 : Math.min(rowsPreview, queue.size());

        for (int i = 0; i < rowsPreview; i++) {
            int rowY = y + boxHeight - verticalPadding - (i + 1) * cellH;
            boolean[] rowData = (i < actualRows ? queue.get(i) : null);

            for (int col = 0; col < cols; col++) {
                int x0 = x + (int) Math.round(col * cellWf);
                int x1 = x + (int) Math.round((col + 1) * cellWf);
                int cellW = x1 - x0;

                g2.setColor(new Color(40, 40, 40));
                g2.drawRect(x0, rowY, cellW, cellH);

                if (rowData != null && col < rowData.length && rowData[col]) {
                    g2.setColor(Color.GRAY);
                    g2.fillRect(x0 + 1, rowY + 1, cellW - 1, cellH - 1);
                }
            }
        }
    }

    // ────────── 중앙 정보 (시간, 랙, 승패) ──────────

    private void drawCenterInfo(Graphics2D g2, int width, int height) {
        int cx = width / 2;
        int cy = height / 2;

        // 시간제한
        if (isTimeAttack) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remain = Math.max(0, timeLimitMillis - elapsed);
            int sec = (int) (remain / 1000);
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 24));
            String t = String.format("Time: %d:%02d", sec / 60, sec % 60);
            int tw = g2.getFontMetrics().stringWidth(t);
            g2.drawString(t, cx - tw / 2, 40);
        }

        if (!lagMessage.isEmpty()) {
            g2.setColor(Color.ORANGE);
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            int tw = g2.getFontMetrics().stringWidth(lagMessage);
            g2.drawString(lagMessage, cx - tw / 2, 60);
        }

        if (paused && !gameOver) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, width, height);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 48));
            String txt = "PAUSED";
            int tw = g2.getFontMetrics().stringWidth(txt);
            g2.drawString(txt, cx - tw / 2, cy);
            return;
        }

        if (gameOver) {
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(0, 0, width, height);

            g2.setFont(new Font("맑은 고딕", Font.BOLD, 40));
            String res;
            Color col;
            if (winner == 1) { res = "YOU WIN!"; col = Color.CYAN; }
            else if (winner == 2) { res = "YOU LOSE"; col = Color.RED; }
            else { res = "DRAW"; col = Color.YELLOW; }
            g2.setColor(col);
            int tw = g2.getFontMetrics().stringWidth(res);
            g2.drawString(res, cx - tw / 2, cy - 20);

            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
            g2.setColor(Color.WHITE);
            String t1 = "ENTER: 새 P2P 게임";
            String t2 = "ESC: 메인 메뉴";
            int w1 = g2.getFontMetrics().stringWidth(t1);
            int w2 = g2.getFontMetrics().stringWidth(t2);
            g2.drawString(t1, cx - w1 / 2, cy + 30);
            g2.drawString(t2, cx - w2 / 2, cy + 60);
        }
    }
}
