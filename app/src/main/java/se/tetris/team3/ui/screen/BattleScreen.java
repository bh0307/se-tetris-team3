package se.tetris.team3.ui.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

import javax.swing.Timer;

import se.tetris.team3.ai.AIPlayer;
import se.tetris.team3.blocks.Block;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.gameManager.BattleGameManager;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.ui.AppFrame;

/**
 * 2인 대전 모드 화면
 * - 좌우에 각각 플레이어 보드 표시
 * - Player1(왼쪽): WASD (이동/회전/낙하)
 * - Player2(오른쪽): 화살표 (이동/회전/낙하)
 * - P: 일시정지, ESC: 메뉴로, ENTER: 재시작
 */
public class BattleScreen implements Screen {

    private final BattleGameManager battleManager;
    private final AppFrame frame;
    private final Settings settings;

    // 화면 레이아웃 관련
    private int blockSize;
    private int blockSizeW; // 블록 가로 길이
    private int blockSizeH; // 블록 세로 길이
    private int boardWidth;
    private int boardHeight;
    private int centerGap;
    private int topMargin;

    // 타이머 (게임 루프 + 낙하)
    private Timer gameTimer;
    private Timer dropTimer;

    private long player1LastDrop;
    private long player2LastDrop;

    private boolean paused = false;
    
    // AI 모드 관련
    private final boolean isAIMode;
    private AIPlayer aiPlayer;

    /**
     * BattleScreen 생성자
     * @param frame        부모 프레임
     * @param mode         대전 모드 (BATTLE_NORMAL, BATTLE_ITEM, BATTLE_TIME)
     * @param settings     게임 설정
     * @param timeLimitSeconds 시간제한 모드일 경우 제한 시간(초), 아니면 0
     * @param isAIMode     Player2를 AI로 설정할지 여부
     */
    public BattleScreen(AppFrame frame, GameMode mode, Settings settings, int timeLimitSeconds, boolean isAIMode) {
        this.frame = frame;
        this.settings = settings;
        this.isAIMode = isAIMode;
        this.battleManager = new BattleGameManager(mode, settings, timeLimitSeconds);

        if (isAIMode) {
            this.aiPlayer = new AIPlayer(battleManager.getPlayer2Manager());
        }

        player1LastDrop = System.currentTimeMillis();
        player2LastDrop = System.currentTimeMillis();
    }

    @Override
    public void onShow() {
        // 게임 시작
        battleManager.start();
        
        // 배경 음악 재생
        frame.getAudioManager().playBattleMusic();
        
        // 게임 로직 업데이트(60fps 정도)
        gameTimer = new Timer(16, evt -> {
            if (!paused) {
                battleManager.update();
                frame.repaint();
            }
        });
        gameTimer.start();

        // 낙하 타이머 (각 플레이어 별도로 속도 계산)
        dropTimer = new Timer(50, evt -> {
            if (!paused && !battleManager.isGameOver()) {
                long now = System.currentTimeMillis();

                GameManager p1 = battleManager.getPlayer1Manager();
                GameManager p2 = battleManager.getPlayer2Manager();

                // Player1 낙하
                if (now - player1LastDrop >= p1.getGameTimerDelay()) {
                    p1.stepDownOrFix();
                    player1LastDrop = now;
                }
                // Player2 낙하 (AI 모드면 AI가 조작)
                if (now - player2LastDrop >= p2.getGameTimerDelay()) {
                    if (isAIMode && aiPlayer != null) {
                        aiPlayer.makeMove();
                    } else {
                        p2.stepDownOrFix();
                    }
                    player2LastDrop = now;
                }
            }
        });
        dropTimer.start();
    }

    @Override
    public void onHide() {
        if (gameTimer != null) gameTimer.stop();
        if (dropTimer != null) dropTimer.stop();
    }

    @Override
    public void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // 게임 오버 상태일 때: ENTER 재시작 / ESC 메뉴
        if (battleManager.isGameOver()) {
            if (key == KeyEvent.VK_ENTER) {
                battleManager.restart();
                player1LastDrop = System.currentTimeMillis();
                player2LastDrop = System.currentTimeMillis();
            } else if (key == KeyEvent.VK_ESCAPE) {
                frame.showScreen(new MenuScreen(frame));
            }
            return;
        }

        // 일시정지 토글
        if (key == KeyEvent.VK_P) {
            paused = !paused;
            return;
        }
        if (paused) return;

        GameManager p1 = battleManager.getPlayer1Manager();
        GameManager p2 = battleManager.getPlayer2Manager();

        switch (key) {
            // Player1: 방향키
            case KeyEvent.VK_LEFT:
                p1.tryMove(p1.getBlockX() - 1, p1.getBlockY());
                break;
            case KeyEvent.VK_RIGHT:
                p1.tryMove(p1.getBlockX() + 1, p1.getBlockY());
                break;
            case KeyEvent.VK_DOWN:
                p1.stepDownOrFix();
                player1LastDrop = System.currentTimeMillis();
                break;
            case KeyEvent.VK_UP:
                p1.rotateBlock();
                break;
            case KeyEvent.VK_SPACE:
                p1.hardDrop();
                player1LastDrop = System.currentTimeMillis();
                break;

            // Player2: WASD (AI 모드면 무시)
            case KeyEvent.VK_A:
                if (!isAIMode) p2.tryMove(p2.getBlockX() - 1, p2.getBlockY());
                break;
            case KeyEvent.VK_D:
                if (!isAIMode) p2.tryMove(p2.getBlockX() + 1, p2.getBlockY());
                break;
            case KeyEvent.VK_S:
                if (!isAIMode) {
                    p2.stepDownOrFix();
                    player2LastDrop = System.currentTimeMillis();
                }
                break;
            case KeyEvent.VK_W:
                if (!isAIMode) p2.rotateBlock();
                break;
            case KeyEvent.VK_ENTER:
                if (!isAIMode) {
                    p2.hardDrop();
                    player2LastDrop = System.currentTimeMillis();
                }
                break;

            // ESC: 메뉴로
            case KeyEvent.VK_ESCAPE:
                frame.showScreen(new MenuScreen(frame));
                break;
        }
    }

    @Override
    public void render(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = frame.getWidth();
        int height = frame.getHeight();

        calculateLayout(width, height);

        // 배틀 전용 배경
        drawBattleBackground(g2, width, height);

        int nextBoxWidth = (int) (blockSize * 3.5);
        int playerAreaWidth = boardWidth + nextBoxWidth;
        int totalWidth = playerAreaWidth + centerGap + playerAreaWidth;
        int startX = (width - totalWidth) / 2 - 20; // 왼쪽으로 20픽셀 이동

        int leftBoardX = startX;
        int rightBoardX = startX + playerAreaWidth + centerGap;
        int boardY = topMargin;

        // 왼쪽 플레이어
        drawPlayerBoard(g2, leftBoardX, boardY, battleManager.getPlayer1Manager(), "Player 1", 1);

        // 오른쪽 플레이어
        String player2Label = isAIMode ? "Computer (AI)" : "Player 2";
        drawPlayerBoard(g2, rightBoardX, boardY, battleManager.getPlayer2Manager(), player2Label, 2);

        // 중앙 시간/승자/일시정지 표시
        drawCenterInfo(g2, width, height);
    }

    /**
     * 화면 크기에 맞게 블록 크기, 간격 계산
     */
    private void calculateLayout(int screenWidth, int screenHeight) {
        int usableWidth = screenWidth - 80;    // 좌우 여백
        int usableHeight = screenHeight - 220; // 상단/하단 여백

        int blockSizeByHeight = usableHeight / 20; // 20줄 기준
        int blockSizeByWidth  = usableWidth  / 30; // 보드2 + NEXT2 + 간격 대략 30칸

        int maxFitSize = Math.min(blockSizeByHeight, blockSizeByWidth);

        // ⚙️ 설정에 따른 선호 블록 크기 (small/medium/large)
        int preferred = (settings != null ? settings.resolveBlockSize() : maxFitSize);

        // 화면에 안 튀어나가도록 상한은 maxFitSize, 너무 작진 않게 하한은 12
        blockSize = Math.max(12, Math.min(preferred, maxFitSize));
        blockSizeW = (int)(blockSize * 1.2); // 가로 길이 20% 증가
        blockSizeH = (int)(blockSize * 1.7); // 세로 길이 70% 증가

        boardWidth  = 10 * blockSizeW;
        boardHeight = 20 * blockSizeH;

        centerGap = Math.max(35, blockSize * 2);
        topMargin = Math.max(75, (screenHeight - boardHeight - 100) / 2);
    }

    /**
     * 한 플레이어 보드 + NEXT + GARBAGE + 조작 안내 그리기
     */
    private void drawPlayerBoard(Graphics2D g2, int x, int y,
                                GameManager manager, String playerName, int playerNum) {

        // 플레이어 이름
        g2.setColor(Color.CYAN);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(14, blockSize)));
        int nameW = g2.getFontMetrics().stringWidth(playerName);
        g2.drawString(playerName, x + (boardWidth - nameW) / 2, y - 45);

        // 점수
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(12, blockSize * 3 / 4)));
        String scoreText = "Score: " + manager.getScore();
        int scoreW = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, x + (boardWidth - scoreW) / 2, y - 25);

        // 레벨
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(10, blockSize * 2 / 3)));
        String levelText = "Level: " + manager.getLevel();
        int levelW = g2.getFontMetrics().stringWidth(levelText);
        g2.drawString(levelText, x + (boardWidth - levelW) / 2, y - 10);

        // 보드 테두리
        g2.setColor(Color.GRAY);
        g2.drawRect(x, y, boardWidth, boardHeight);

        // 고정 블럭
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                int cellX = x + col * blockSizeW;
                int cellY = y + row * blockSizeH;

                if (manager.isRowFlashing(row)) {
                    // 플래시 효과
                    g2.setColor(Color.WHITE);
                    g2.fillRect(cellX, cellY, blockSizeW - 1, blockSizeH - 1);
                } else if (manager.getFieldValue(row, col) == 1) {
                    g2.setColor(Color.DARK_GRAY);
                    g2.fillRect(cellX, cellY, blockSizeW - 1, blockSizeH - 1);

                    // 고정 블럭에 아이템이 있으면 글자 표시
                    char itemType = manager.getItemType(row, col);
                    if (itemType != 0) {
                        GameScreen.drawCenteredChar(g2, cellX, cellY, blockSize, itemType);
                    }
                }
            }
        }

        // 현재 블럭 + 고스트 블록(하드 드롭 위치 미리보기)
        if (!manager.isGameOver() && manager.getCurrentBlock() != null) {
            Block cur = manager.getCurrentBlock();
            int[][] shape = cur.getShape();
            Color base = cur.getColor();
            int bx = manager.getBlockX();
            int by = manager.getBlockY();

            // 1. 하드 드롭 위치 계산
            int ghostY = by;
            while (true) {
                boolean canMove = true;
                for (int r = 0; r < shape.length; r++) {
                    for (int c = 0; c < shape[r].length; c++) {
                        if (shape[r][c] != 0) {
                            int testY = ghostY + r + 1;
                            int testX = bx + c;
                            if (testY >= 20 || manager.getFieldValue(testY, testX) != 0) {
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
                        int gx = bx + c;
                        int gy = ghostY + r;
                        if (gx >= 0 && gx < 10 && gy >= 0 && gy < 20) {
                            int cellX = x + gx * blockSizeW;
                            int cellY = y + gy * blockSizeH;
                            g2.setColor(ghostColor);
                            g2.fillRect(cellX, cellY, blockSizeW - 1, blockSizeH - 1);
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
                        int gx = bx + c;
                        int gy = by + r;
                        if (gx >= 0 && gx < 10 && gy >= 0 && gy < 20) {
                            int cellX = x + gx * blockSizeW;
                            int cellY = y + gy * blockSizeH;
                            g2.setColor(base);
                            g2.fillRect(cellX, cellY, blockSizeW - 1, blockSizeH - 1);
                            if (cur.getItemType() != 0 && ir != null && ic != null
                                    && r == ir && c == ic) {
                                GameScreen.drawCenteredChar(g2, cellX, cellY, blockSize, cur.getItemType());
                            }
                        }
                    }
                }
            }
        }

        manager.renderParticles(g2, x, y, blockSize);

        // NEXT + GARBAGE 박스 배치
        int nextX = x + boardWidth + 10;
        int nextTopY = y + blockSize;                        // 보드 위에서 한 칸 내려온 위치
        drawNextBlock(g2, nextX, nextTopY, manager);

        int garbageTopY = nextTopY + (int) (blockSize * 7); // NEXT 아래쪽 여유공간 이후
        drawGarbagePreview(g2, nextX, garbageTopY, manager);

        // 조작 안내
        g2.setColor(Color.LIGHT_GRAY);
        int fontSize = Math.max(8, blockSize / 2);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, fontSize));
        int lineH = fontSize + 3;

        if (playerNum == 1) {
            String c1 = "←/→: Move";
            String c2 = "↑: Rotate";
            String c3 = "↓: Drop";
            int yBase = y + boardHeight + 15;
            g2.drawString(c1, x + (boardWidth - g2.getFontMetrics().stringWidth(c1)) / 2, yBase);
            g2.drawString(c2, x + (boardWidth - g2.getFontMetrics().stringWidth(c2)) / 2, yBase + lineH);
            g2.drawString(c3, x + (boardWidth - g2.getFontMetrics().stringWidth(c3)) / 2, yBase + lineH * 2);
        } else {
            String c1 = "A/D: Move";
            String c2 = "W: Rotate";
            String c3 = "S: Drop";
            int yBase = y + boardHeight + 15;
            g2.drawString(c1, x + (boardWidth - g2.getFontMetrics().stringWidth(c1)) / 2, yBase);
            g2.drawString(c2, x + (boardWidth - g2.getFontMetrics().stringWidth(c2)) / 2, yBase + lineH);
            g2.drawString(c3, x + (boardWidth - g2.getFontMetrics().stringWidth(c3)) / 2, yBase + lineH * 2);
        }
    }

    /**
     * 다음 블럭 미리보기
     */
    private void drawNextBlock(Graphics2D g2, int nextX, int nextY, GameManager manager) {
        if (manager.getNextBlock() == null) return;

        int previewSize = (int) (blockSize * 3.5);

        // 라벨
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, blockSize / 2)));
        String label = "NEXT";
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, nextX + (previewSize - lw) / 2, nextY - 5);

        // 박스
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(nextX, nextY, previewSize, previewSize);

        int[][] shape = manager.getNextBlock().getShape();
        Color color = manager.getNextBlock().getColor();

        int nextBlockSize = (int) (blockSize * 0.75);
        int shapeWidth = shape[0].length;
        int shapeHeight = shape.length;
        int offsetX = (previewSize - shapeWidth * nextBlockSize) / 2;
        int offsetY = (previewSize - shapeHeight * nextBlockSize) / 2;

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int cellX = nextX + offsetX + c * nextBlockSize;
                    int cellY = nextY + offsetY + r * nextBlockSize;
                    g2.setColor(color);
                    g2.fillRect(cellX, cellY, nextBlockSize - 1, nextBlockSize - 1);
                }
            }
        }
    }

    /**
     * 넘어간 쓰레기 줄 미리보기 박스
     * - 항상 라벨/박스는 보이고
     * - 대기 중인 공격 줄이 있을 때만 안쪽에 회색 칸을 채운다
     */
    private void drawGarbagePreview(Graphics2D g2, int x, int y, GameManager manager) {
        java.util.List<boolean[]> queue = manager.getPendingGarbagePreview();

        final int cols = 10;
        final int rowsPreview = 10;

        // NEXT 박스와 동일한 정사각형 크기
        int boxSize   = (int) (blockSize * 3.5);
        int boxWidth  = boxSize;
        int boxHeight = boxSize;

        // 라벨
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, blockSize / 2)));
        String label = "GARBAGE";
        int w = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, x + (boxWidth - w) / 2, y - 5);

        // 테두리 + 배경
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(x, y, boxWidth, boxHeight);
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 1, y + 1, boxWidth - 2, boxHeight - 2);

        // 세로 방향
        int cellH = boxHeight / rowsPreview;          // 한 줄 높이 (정수)
        int gridHeight = cellH * rowsPreview;         // 실제 그리드 높이
        int verticalPadding = (boxHeight - gridHeight) / 2; // 위/아래 동일하게

        // 가로 방향
        double cellWf = (double) boxWidth / cols;

        int actualRows = (queue == null) ? 0 : Math.min(rowsPreview, queue.size());

        for (int i = 0; i < rowsPreview; i++) {
            // 아래에서 위로 쌓이게 + 아래쪽 여백도 verticalPadding 만큼
            int rowY = y + boxHeight - verticalPadding - (i + 1) * cellH;
            boolean[] rowData = (i < actualRows ? queue.get(i) : null);

            for (int col = 0; col < cols; col++) {
                int x0 = x + (int) Math.round(col * cellWf);
                int x1 = x + (int) Math.round((col + 1) * cellWf);
                int cellW = x1 - x0;

                // 그리드 선
                g2.setColor(new Color(40, 40, 40));
                g2.drawRect(x0, rowY, cellW, cellH);

                // 공격 줄 채우기
                if (rowData != null && col < rowData.length && rowData[col]) {
                    g2.setColor(Color.GRAY);
                    g2.fillRect(x0 + 1, rowY + 1, cellW - 1, cellH - 1);
                }
            }
        }
    }







    /**
     * 중앙 정보: 시간제한, 일시정지, 승자 표시
     */
    private void drawCenterInfo(Graphics2D g2, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;

        // 시간제한 모드: 남은 시간
        if (battleManager.getBattleMode() == GameMode.BATTLE_TIME) {
            int remaining = battleManager.getRemainingTimeSeconds();
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 28));
            String timeStr = String.format("Time: %d:%02d", remaining / 60, remaining % 60);
            int tw = g2.getFontMetrics().stringWidth(timeStr);
            g2.drawString(timeStr, centerX - tw / 2, 40);
        }

        // 일시정지
        if (paused) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, width, height);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 48));
            String pauseText = "PAUSED";
            int pw = g2.getFontMetrics().stringWidth(pauseText);
            g2.drawString(pauseText, centerX - pw / 2, centerY);

            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
            String resumeText = "Press P to Resume";
            int rw = g2.getFontMetrics().stringWidth(resumeText);
            g2.drawString(resumeText, centerX - rw / 2, centerY + 50);
        }

        // 게임 오버 + 승자
        if (battleManager.isGameOver()) {
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(0, 0, width, height);

            int winner = battleManager.getWinner();
            String resultText;
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 56));

            if (winner == 0) {
                resultText = "DRAW!";
                g2.setColor(Color.YELLOW);
            } else if (winner == 1) {
                resultText = "Player 1 WINS!";
                g2.setColor(Color.CYAN);
            } else {
                resultText = isAIMode ? "AI WINS!" : "Player 2 WINS!";
                g2.setColor(Color.RED);
            }

            int rwid = g2.getFontMetrics().stringWidth(resultText);
            g2.drawString(resultText, centerX - rwid / 2, centerY - 50);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 24));
            String restartText = "Press ENTER to Restart";
            int stw = g2.getFontMetrics().stringWidth(restartText);
            g2.drawString(restartText, centerX - stw / 2, centerY + 20);

            String menuText = "Press ESC to Main Menu";
            int mtw = g2.getFontMetrics().stringWidth(menuText);
            g2.drawString(menuText, centerX - mtw / 2, centerY + 60);
        }
    }
    
    /**
     * 대전 모드 전용 배경 - 격렬한 전투 느낌
     */
    private void drawBattleBackground(Graphics2D g2, int width, int height) {
        // 어두운 빨강-검정 그라데이션 (전장 느낌)
        java.awt.GradientPaint gradient = new java.awt.GradientPaint(
            0, 0, new Color(40, 0, 0),
            0, height, new Color(0, 0, 0)
        );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, width, height);
        
        // 대각선 경고 스트라이프 (공사장/위험 느낌)
        g2.setColor(new Color(255, 100, 0, 40)); // 주황색 반투명
        for (int i = -height; i < width + height; i += 80) {
            int[] xPoints = {i, i + 40, i + 40 + height, i + height};
            int[] yPoints = {0, 0, height, height};
            g2.fillPolygon(xPoints, yPoints, 4);
        }
        
        // 중앙 VS 라인 (양쪽 대결 강조)
        int centerX = width / 2;
        g2.setColor(new Color(255, 0, 0, 100));
        g2.setStroke(new java.awt.BasicStroke(4));
        g2.drawLine(centerX, 0, centerX, height);
        
        // 번개 효과 라인 (좌우 대각선)
        g2.setColor(new Color(255, 255, 0, 60)); // 노란색 번개
        g2.setStroke(new java.awt.BasicStroke(3));
        java.util.Random rand = new java.util.Random(System.currentTimeMillis() / 500); // 느린 애니메이션
        for (int i = 0; i < 3; i++) {
            int startX = rand.nextInt(width / 4);
            int endX = width / 4 + rand.nextInt(width / 4);
            int y = rand.nextInt(height);
            g2.drawLine(startX, y, endX, y + 50);
            
            startX = width - rand.nextInt(width / 4);
            endX = width - (width / 4 + rand.nextInt(width / 4));
            y = rand.nextInt(height);
            g2.drawLine(startX, y, endX, y + 50);
        }
        
        // 폭발 파티클 효과 (배경에 흩어진 점들)
        g2.setColor(new Color(255, 150, 0, 150)); // 주황색 불꽃
        rand = new java.util.Random(42); // 고정 패턴
        for (int i = 0; i < 40; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            int size = rand.nextInt(4) + 2;
            g2.fillOval(x, y, size, size);
        }
        
        // 붉은 섬광 (상단)
        g2.setColor(new Color(255, 0, 0, 30));
        g2.fillRect(0, 0, width, height / 4);
        
        g2.setStroke(new java.awt.BasicStroke(1));
    }
}
