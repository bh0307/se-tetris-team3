package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;

import javax.swing.Timer;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;

/**
 * 2인 대전 모드 화면
 * - 좌우에 각각 플레이어 보드 표시
 * - Player1(왼쪽): WASD + 스페이스(회전), Shift(하드드랍)
 * - Player2(오른쪽): 화살표 + Enter(회전), RCtrl(하드드랍)
 * - 게임 오버 시 승자 표시
 */
public class BattleScreen implements Screen {
    
    private final BattleGameManager battleManager;
    private final AppFrame frame;
    private final Settings settings;
    
    // 화면 구성 비율 (화면 크기에 따라 동적으로 계산됨)
    private int blockSize;
    private int boardWidth;
    private int boardHeight;
    private int centerGap;
    private int topMargin;
    
    // 타이머
    private Timer gameTimer;
    private Timer dropTimer;
    
    // 플레이어별 낙하 타이머
    private long player1LastDrop;
    private long player2LastDrop;
    
    // 일시정지 상태
    private boolean paused = false;
    
    /**
     * BattleScreen 생성자
     * @param frame 부모 프레임
     * @param mode 대전 모드 (BATTLE_NORMAL, BATTLE_ITEM, BATTLE_TIME)
     * @param settings 게임 설정
     * @param timeLimitSeconds 시간제한(초), 시간제한 모드가 아니면 0
     */
    public BattleScreen(AppFrame frame, GameMode mode, Settings settings, int timeLimitSeconds) {
        this.frame = frame;
        this.settings = settings;
        this.battleManager = new BattleGameManager(mode, settings, timeLimitSeconds);
        
        // 블록 낙하 타이머 시작
        player1LastDrop = System.currentTimeMillis();
        player2LastDrop = System.currentTimeMillis();
    }
    
    @Override
    public void onShow() {
        // 게임 루프 타이머 시작 (60 FPS)
        gameTimer = new Timer(16, evt -> {
            if (!paused) {
                battleManager.update();
                frame.repaint();
            }
        });
        gameTimer.start();
        
        // 낙하 타이머 (각 플레이어 독립적으로)
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
                
                // Player2 낙하
                if (now - player2LastDrop >= p2.getGameTimerDelay()) {
                    p2.stepDownOrFix();
                    player2LastDrop = now;
                }
            }
        });
        dropTimer.start();
    }
    
    @Override
    public void onHide() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (dropTimer != null) {
            dropTimer.stop();
        }
    }
    
    /**
     * 키 입력 처리
     * Player1: W/A/S/D (이동+회전), P(일시정지)
     * Player2: 화살표 (이동+회전), P(일시정지)
     */
    @Override
    public void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        // 게임 오버 상태면 Enter/Esc로만 처리
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
        
        // Player1 조작 (WASD + Shift)
        switch (key) {
            case KeyEvent.VK_A:
                p1.tryMove(p1.getBlockX() - 1, p1.getBlockY());
                break;
            case KeyEvent.VK_D:
                p1.tryMove(p1.getBlockX() + 1, p1.getBlockY());
                break;
            case KeyEvent.VK_S:
                p1.stepDownOrFix();
                player1LastDrop = System.currentTimeMillis();
                break;
            case KeyEvent.VK_W:
                p1.rotateBlock();
                break;
                
            // Player2 조작 (화살표)
            case KeyEvent.VK_LEFT:
                p2.tryMove(p2.getBlockX() - 1, p2.getBlockY());
                break;
            case KeyEvent.VK_RIGHT:
                p2.tryMove(p2.getBlockX() + 1, p2.getBlockY());
                break;
            case KeyEvent.VK_DOWN:
                p2.stepDownOrFix();
                player2LastDrop = System.currentTimeMillis();
                break;
            case KeyEvent.VK_UP:
                p2.rotateBlock();
                break;
                
            // ESC: 메뉴로 돌아가기
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
        
        // 화면 크기에 맞게 레이아웃 계산
        calculateLayout(width, height);
        
        // 배경
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        
        // 각 보드 + NEXT 박스 포함 너비
        int nextBoxWidth = (int)(blockSize * 3.5);
        int playerAreaWidth = boardWidth + nextBoxWidth;
        
        // 전체 너비 = 왼쪽영역 + 간격 + 오른쪽영역
        int totalWidth = playerAreaWidth + centerGap + playerAreaWidth;
        int startX = (width - totalWidth) / 2;
        
        // 왼쪽 플레이어 보드 위치
        int leftBoardX = startX;
        int boardY = topMargin;
        
        // 오른쪽 플레이어 보드 위치
        int rightBoardX = startX + playerAreaWidth + centerGap;
        
        // Player1 보드 그리기
        drawPlayerBoard(g2, leftBoardX, boardY, battleManager.getPlayer1Manager(), "Player 1", 1);
        
        // Player2 보드 그리기
        drawPlayerBoard(g2, rightBoardX, boardY, battleManager.getPlayer2Manager(), "Player 2", 2);
        
        // 중앙 정보 표시 (시간제한, 일시정지, 게임 오버)
        drawCenterInfo(g2, width, height);
    }
    
    /**
     * 화면 크기에 맞게 레이아웃 계산
     */
    private void calculateLayout(int screenWidth, int screenHeight) {
        // NEXT 박스와 여백을 고려한 사용 가능한 공간 계산
        int usableWidth = screenWidth - 80;  // 좌우 여백
        int usableHeight = screenHeight - 220; // 상하단 여백 + 컨트롤 안내
        
        // 높이 기준으로 블록 크기 계산
        int blockSizeByHeight = usableHeight / 20; // 20줄
        
        // 너비 기준으로 블록 크기 계산 (2개 보드 + 2개 NEXT 박스 + 간격)
        // 전체 구성: NEXT(3.5) + 보드(10) + 간격(3) + 보드(10) + NEXT(3.5) = 약 30 블록폭
        int blockSizeByWidth = usableWidth / 30;
        
        // 둘 중 작은 값 선택하고 범위 제한
        int calculatedBlockSize = Math.min(blockSizeByHeight, blockSizeByWidth);
        blockSize = Math.max(14, Math.min(28, calculatedBlockSize));
        
        // 보드 크기 계산
        boardWidth = 10 * blockSize;
        boardHeight = 20 * blockSize;
        
        // 간격 계산
        centerGap = Math.max(35, blockSize * 2);
        
        // 상단 여백 (플레이어 정보 표시 공간 확보)
        topMargin = Math.max(75, (screenHeight - boardHeight - 100) / 2);
    }
    
    /**
     * 플레이어 보드 그리기
     */
    private void drawPlayerBoard(Graphics2D g2, int x, int y, GameManager manager, String playerName, int playerNum) {
        // 플레이어 이름 표시
        g2.setColor(Color.CYAN);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(14, blockSize)));
        g2.drawString(playerName, x + (boardWidth - g2.getFontMetrics().stringWidth(playerName)) / 2, y - 45);
        
        // 점수 표시
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(12, blockSize * 3 / 4)));
        String scoreText = "Score: " + manager.getScore();
        g2.drawString(scoreText, x + (boardWidth - g2.getFontMetrics().stringWidth(scoreText)) / 2, y - 25);
        
        // 레벨 표시
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, Math.max(10, blockSize * 2 / 3)));
        String levelText = "Level: " + manager.getLevel();
        g2.drawString(levelText, x + (boardWidth - g2.getFontMetrics().stringWidth(levelText)) / 2, y - 10);
        
        // 보드 테두리
        g2.setColor(Color.GRAY);
        g2.drawRect(x, y, boardWidth, boardHeight);
        
        // 필드 그리기
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                int cellX = x + col * blockSize;
                int cellY = y + row * blockSize;
                
                // 플래시 효과
                if (manager.isRowFlashing(row)) {
                    g2.setColor(Color.WHITE);
                    g2.fillRect(cellX, cellY, blockSize - 1, blockSize - 1);
                } else if (manager.getFieldValue(row, col) == 1) {
                    // 고정된 블록
                    g2.setColor(Color.DARK_GRAY);
                    g2.fillRect(cellX, cellY, blockSize - 1, blockSize - 1);
                    
                    // 아이템 표시
                    char itemType = manager.getItemType(row, col);
                    if (itemType != 0) {
                        g2.setColor(Color.YELLOW);
                        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, blockSize - 6)));
                        g2.drawString(String.valueOf(itemType), cellX + blockSize/3, cellY + blockSize*2/3);
                    }
                }
            }
        }
        
        // 현재 블록 그리기
        if (manager.getCurrentBlock() != null) {
            int[][] shape = manager.getCurrentBlock().getShape();
            Color color = manager.getCurrentBlock().getColor();
            
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int cellX = x + (manager.getBlockX() + c) * blockSize;
                        int cellY = y + (manager.getBlockY() + r) * blockSize;
                        g2.setColor(color);
                        g2.fillRect(cellX, cellY, blockSize - 1, blockSize - 1);
                    }
                }
            }
        }
        
        // 다음 블록 미리보기 (보드 오른쪽)
        drawNextBlock(g2, x + boardWidth + 10, y, manager);
        
        // 컨트롤 안내 (보드 아래)
        g2.setColor(Color.LIGHT_GRAY);
        int fontSize = Math.max(8, blockSize / 2);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, fontSize));
        if (playerNum == 1) {
            String control1 = "A/D: Move";
            String control2 = "W: Rotate";
            String control3 = "S: Drop";
            int lineHeight = fontSize + 3;
            g2.drawString(control1, x + (boardWidth - g2.getFontMetrics().stringWidth(control1)) / 2, y + boardHeight + 15);
            g2.drawString(control2, x + (boardWidth - g2.getFontMetrics().stringWidth(control2)) / 2, y + boardHeight + 15 + lineHeight);
            g2.drawString(control3, x + (boardWidth - g2.getFontMetrics().stringWidth(control3)) / 2, y + boardHeight + 15 + lineHeight * 2);
        } else {
            String control1 = "Left/Right: Move";
            String control2 = "Up: Rotate";
            String control3 = "Down: Drop";
            int lineHeight = fontSize + 3;
            g2.drawString(control1, x + (boardWidth - g2.getFontMetrics().stringWidth(control1)) / 2, y + boardHeight + 15);
            g2.drawString(control2, x + (boardWidth - g2.getFontMetrics().stringWidth(control2)) / 2, y + boardHeight + 15 + lineHeight);
            g2.drawString(control3, x + (boardWidth - g2.getFontMetrics().stringWidth(control3)) / 2, y + boardHeight + 15 + lineHeight * 2);
        }
    }
    
    /**
     * 다음 블록 미리보기
     */
    private void drawNextBlock(Graphics2D g2, int nextX, int nextY, GameManager manager) {
        if (manager.getNextBlock() == null) return;
        
        nextY += (int)(blockSize * 3);  // 보드 위쪽에서 조금 아래로
        int previewSize = (int)(blockSize * 3.5);
        
        // "NEXT" 라벨
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, Math.max(10, blockSize / 2)));
        String nextLabel = "NEXT";
        g2.drawString(nextLabel, nextX + (previewSize - g2.getFontMetrics().stringWidth(nextLabel)) / 2, nextY - 5);
        
        // 미리보기 박스 테두리
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(nextX, nextY, previewSize, previewSize);
        
        // 다음 블록 그리기
        int[][] shape = manager.getNextBlock().getShape();
        Color color = manager.getNextBlock().getColor();
        
        // 블록을 중앙에 배치하기 위한 오프셋 계산
        int nextBlockSize = (int)(blockSize * 0.75);
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
     * 중앙 정보 표시 (시간, 일시정지, 승자)
     */
    private void drawCenterInfo(Graphics2D g2, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        // 시간제한 모드일 경우 남은 시간 표시
        if (battleManager.getBattleMode() == GameMode.BATTLE_TIME) {
            int remaining = battleManager.getRemainingTimeSeconds();
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 28));
            String timeStr = String.format("Time: %d:%02d", remaining / 60, remaining % 60);
            int strWidth = g2.getFontMetrics().stringWidth(timeStr);
            g2.drawString(timeStr, centerX - strWidth / 2, 40);
        }
        
        // 일시정지 표시
        if (paused) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, width, height);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 48));
            String pauseText = "PAUSED";
            int strWidth = g2.getFontMetrics().stringWidth(pauseText);
            g2.drawString(pauseText, centerX - strWidth / 2, centerY);
            
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
            String resumeText = "Press P to Resume";
            strWidth = g2.getFontMetrics().stringWidth(resumeText);
            g2.drawString(resumeText, centerX - strWidth / 2, centerY + 50);
        }
        
        // 게임 오버 및 승자 표시
        if (battleManager.isGameOver()) {
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(0, 0, width, height);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 56));
            
            String resultText;
            int winner = battleManager.getWinner();
            if (winner == 0) {
                resultText = "DRAW!";
                g2.setColor(Color.YELLOW);
            } else if (winner == 1) {
                resultText = "Player 1 WINS!";
                g2.setColor(Color.CYAN);
            } else {
                resultText = "Player 2 WINS!";
                g2.setColor(Color.GREEN);
            }
            
            int strWidth = g2.getFontMetrics().stringWidth(resultText);
            g2.drawString(resultText, centerX - strWidth / 2, centerY - 50);
            
            // 재시작 안내
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 24));
            String restartText = "Press ENTER to Restart";
            strWidth = g2.getFontMetrics().stringWidth(restartText);
            g2.drawString(restartText, centerX - strWidth / 2, centerY + 20);
            
            String menuText = "Press ESC to Main Menu";
            strWidth = g2.getFontMetrics().stringWidth(menuText);
            g2.drawString(menuText, centerX - strWidth / 2, centerY + 60);
        }
    }
}
