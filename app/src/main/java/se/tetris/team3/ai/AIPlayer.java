package se.tetris.team3.ai;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.ui.GameManager;

/**
 * 컴퓨터 플레이어 AI
 * - 현재 보드 상태를 분석하여 다음 키 입력을 결정
 * - 0.5~1초 간격으로 호출되어 사람처럼 플레이
 */
public class AIPlayer {
    
    public enum AIAction {
        MOVE_LEFT,
        MOVE_RIGHT,
        ROTATE,
        SOFT_DROP,
        HARD_DROP,
        NONE
    }
    
    private final GameManager gameManager;
    private int targetX = -1;
    private int targetRotation = 0;
    private int currentRotationCount = 0;  // 현재 회전 횟수 추적
    private boolean hasCalculatedMove = false;
    private Block lastBlock = null;  // 이전 블록 참조
    
    /**
     * AI 플레이어 생성자
     * @param gameManager 제어할 GameManager
     */
    public AIPlayer(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    /**
     * 현재 보드 상태를 분석하여 다음 액션 결정
     * @return 다음에 수행할 액션
     */
    public AIAction getNextAction() {
        if (gameManager.isGameOver()) {
            return AIAction.NONE;
        }
        
        Block currentBlock = gameManager.getCurrentBlock();
        if (currentBlock == null) {
            return AIAction.NONE;
        }
        
        // 새 블록이 나왔으면 리셋
        if (currentBlock != lastBlock) {
            lastBlock = currentBlock;
            currentRotationCount = 0;
            hasCalculatedMove = false;
        }
        
        // 새 블록이 나왔으면 최적 위치 계산
        if (!hasCalculatedMove) {
            calculateBestMove();
            hasCalculatedMove = true;
        }
        
        int currentX = gameManager.getBlockX();
        
        // 1. 먼저 회전을 목표 상태로 맞춤
        if (currentRotationCount < targetRotation) {
            currentRotationCount++;
            return AIAction.ROTATE;
        }
        
        // 2. 회전이 완료되면 x 위치를 목표로 이동
        if (currentX < targetX) {
            return AIAction.MOVE_RIGHT;
        } else if (currentX > targetX) {
            return AIAction.MOVE_LEFT;
        }
        
        // 3. 위치와 회전이 모두 맞으면 하드 드롭
        hasCalculatedMove = false; // 다음 블록을 위해 리셋
        currentRotationCount = 0;
        return AIAction.HARD_DROP;
    }
    
    /**
     * 현재 블록의 최적 위치와 회전 상태 계산
     */
    private void calculateBestMove() {
        Block block = gameManager.getCurrentBlock();
        if (block == null) {
            targetX = 4;
            targetRotation = 0;
            return;
        }
        
        int bestX = 4;
        int bestRotation = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        // 모든 회전 상태 시도 (0, 1, 2, 3)
        for (int rotation = 0; rotation < 4; rotation++) {
            // 임시로 회전
            for (int r = 0; r < rotation; r++) {
                block.rotate();
            }
            
            int[][] shape = block.getShape();
            int width = shape[0].length;
            
            // 모든 x 위치 시도
            for (int x = 0; x <= 10 - width; x++) {
                // 해당 위치에서 블록을 떨어뜨렸을 때의 점수 계산
                int dropY = calculateDropPosition(x, shape);
                
                if (dropY >= 0) {
                    double score = evaluatePosition(x, dropY, shape);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestX = x;
                        bestRotation = rotation;
                    }
                }
            }
            
            // 원래 상태로 복구
            for (int r = 0; r < (4 - rotation) % 4; r++) {
                block.rotate();
            }
        }
        
        targetX = bestX;
        targetRotation = bestRotation;
    }
    
    /**
     * 주어진 x 위치에서 블록이 떨어질 최종 y 위치 계산
     */
    private int calculateDropPosition(int x, int[][] shape) {
        int y = 0;
        
        while (y < 20) {
            if (wouldCollide(x, y + 1, shape)) {
                return y;
            }
            y++;
        }
        
        return y;
    }
    
    /**
     * 블록이 해당 위치에 놓일 때 충돌 여부 확인
     */
    private boolean wouldCollide(int x, int y, int[][] shape) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldX = x + c;
                    int fieldY = y + r;
                    
                    if (fieldX < 0 || fieldX >= 10 || fieldY >= 20) {
                        return true;
                    }
                    
                    if (fieldY >= 0 && gameManager.getFieldValue(fieldY, fieldX) != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 블록 위치의 품질 평가 (높을수록 좋음)
     * - 높이가 낮을수록 좋음
     * - 구멍을 만들지 않을수록 좋음
     * - 줄을 완성할 수 있으면 좋음
     */
    private double evaluatePosition(int x, int y, int[][] shape) {
        double score = 0;
        
        // 1. 완성된 줄 보너스 - 최우선 전략 (압도적으로 높은 보너스)
        int completedLines = countCompletedLines(x, y, shape);
        score += completedLines * 1000;  // 줄 완성이 가장 중요!
        
        // 2. 구멍 페널티 - 매우 강한 페널티 (구멍은 치명적)
        int holes = countHoles(x, y, shape);
        score -= holes * 500;
        
        // 3. 높이 차이 페널티 (평평할수록 좋음)
        double heightVariance = calculateHeightVariance(x, y, shape);
        score -= heightVariance * 20;
        
        // 4. 최대 높이 페널티 (높이 관리 중요)
        int maxHeight = getMaxHeight(x, y, shape);
        score -= maxHeight * 15;  // 높을수록 페널티
        if (maxHeight > 15) {
            score -= (maxHeight - 15) * 100;  // 15줄 넘으면 강한 페널티
        }
        
        // 5. 벽이나 다른 블록에 붙을수록 좋음
        int touchingCells = countTouchingCells(x, y, shape);
        score += touchingCells * 8;
        
        // 6. 거의 완성된 줄 보너스 (8칸 이상 채워진 줄)
        int almostCompletedLines = countAlmostCompletedLines(x, y, shape);
        score += almostCompletedLines * 150;
        
        // 7. 열의 전환 최소화 (블록이 놓일 때 높이 변화가 적을수록 좋음)
        int columnTransitions = countColumnTransitions(x, y, shape);
        score -= columnTransitions * 10;
        
        return score;
    }
    
    /**
     * 블록을 놓았을 때 완성되는 줄 수 계산
     */
    private int countCompletedLines(int x, int y, int[][] shape) {
        boolean[][] tempField = copyField();
        
        // 블록을 임시 필드에 배치
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldY = y + r;
                    int fieldX = x + c;
                    if (fieldY >= 0 && fieldY < 20 && fieldX >= 0 && fieldX < 10) {
                        tempField[fieldY][fieldX] = true;
                    }
                }
            }
        }
        
        // 완성된 줄 카운트
        int count = 0;
        for (int row = 0; row < 20; row++) {
            boolean full = true;
            for (int col = 0; col < 10; col++) {
                if (!tempField[row][col]) {
                    full = false;
                    break;
                }
            }
            if (full) count++;
        }
        
        return count;
    }
    
    /**
     * 블록을 놓았을 때 생기는 구멍 개수 계산
     */
    private int countHoles(int x, int y, int[][] shape) {
        boolean[][] tempField = copyField();
        
        // 블록을 임시 필드에 배치
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldY = y + r;
                    int fieldX = x + c;
                    if (fieldY >= 0 && fieldY < 20 && fieldX >= 0 && fieldX < 10) {
                        tempField[fieldY][fieldX] = true;
                    }
                }
            }
        }
        
        // 구멍 찾기 (위에 블록이 있는데 빈 칸)
        int holes = 0;
        for (int col = 0; col < 10; col++) {
            boolean foundBlock = false;
            for (int row = 0; row < 20; row++) {
                if (tempField[row][col]) {
                    foundBlock = true;
                } else if (foundBlock) {
                    holes++;
                }
            }
        }
        
        return holes;
    }
    
    /**
     * 높이 분산 계산 (평평할수록 0에 가까움)
     */
    private double calculateHeightVariance(int x, int y, int[][] shape) {
        boolean[][] tempField = copyField();
        
        // 블록을 임시 필드에 배치
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldY = y + r;
                    int fieldX = x + c;
                    if (fieldY >= 0 && fieldY < 20 && fieldX >= 0 && fieldX < 10) {
                        tempField[fieldY][fieldX] = true;
                    }
                }
            }
        }
        
        // 각 열의 높이 계산
        int[] heights = new int[10];
        for (int col = 0; col < 10; col++) {
            heights[col] = 0;
            for (int row = 0; row < 20; row++) {
                if (tempField[row][col]) {
                    heights[col] = 20 - row;
                    break;
                }
            }
        }
        
        // 평균 높이
        double avg = 0;
        for (int h : heights) avg += h;
        avg /= 10;
        
        // 분산 계산
        double variance = 0;
        for (int h : heights) {
            variance += Math.pow(h - avg, 2);
        }
        
        return variance / 10;
    }
    
    /**
     * 블록이 벽이나 다른 블록과 접촉하는 면의 개수
     */
    private int countTouchingCells(int x, int y, int[][] shape) {
        int count = 0;
        
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldX = x + c;
                    int fieldY = y + r;
                    
                    // 왼쪽
                    if (fieldX == 0 || (fieldX > 0 && gameManager.getFieldValue(fieldY, fieldX - 1) != 0)) {
                        count++;
                    }
                    // 오른쪽
                    if (fieldX == 9 || (fieldX < 9 && gameManager.getFieldValue(fieldY, fieldX + 1) != 0)) {
                        count++;
                    }
                    // 아래
                    if (fieldY == 19 || (fieldY < 19 && gameManager.getFieldValue(fieldY + 1, fieldX) != 0)) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * 거의 완성된 줄 개수 계산 (8칸 이상 채워진 줄)
     */
    private int countAlmostCompletedLines(int x, int y, int[][] shape) {
        boolean[][] tempField = copyField();
        
        // 블록을 임시 필드에 배치
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldY = y + r;
                    int fieldX = x + c;
                    if (fieldY >= 0 && fieldY < 20 && fieldX >= 0 && fieldX < 10) {
                        tempField[fieldY][fieldX] = true;
                    }
                }
            }
        }
        
        // 8칸 이상 채워진 줄 카운트
        int count = 0;
        for (int row = 0; row < 20; row++) {
            int filled = 0;
            for (int col = 0; col < 10; col++) {
                if (tempField[row][col]) filled++;
            }
            if (filled >= 8 && filled < 10) count++;
        }
        
        return count;
    }
    
    /**
     * 열의 높이 전환 개수 (인접한 열 간 높이 차이)
     */
    private int countColumnTransitions(int x, int y, int[][] shape) {
        boolean[][] tempField = copyField();
        
        // 블록을 임시 필드에 배치
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldY = y + r;
                    int fieldX = x + c;
                    if (fieldY >= 0 && fieldY < 20 && fieldX >= 0 && fieldX < 10) {
                        tempField[fieldY][fieldX] = true;
                    }
                }
            }
        }
        
        // 각 열의 높이 계산
        int[] heights = new int[10];
        for (int col = 0; col < 10; col++) {
            for (int row = 0; row < 20; row++) {
                if (tempField[row][col]) {
                    heights[col] = 20 - row;
                    break;
                }
            }
        }
        
        // 인접 열 간 높이 차이의 합
        int transitions = 0;
        for (int col = 0; col < 9; col++) {
            transitions += Math.abs(heights[col] - heights[col + 1]);
        }
        
        return transitions;
    }
    
    /**
     * 블록 배치 후 최대 높이 계산
     */
    private int getMaxHeight(int x, int y, int[][] shape) {
        boolean[][] tempField = copyField();
        
        // 블록을 임시 필드에 배치
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int fieldY = y + r;
                    int fieldX = x + c;
                    if (fieldY >= 0 && fieldY < 20 && fieldX >= 0 && fieldX < 10) {
                        tempField[fieldY][fieldX] = true;
                    }
                }
            }
        }
        
        // 가장 높은 블록의 위치 찾기
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                if (tempField[row][col]) {
                    return row; // 높이는 행 번호 (0이 가장 높음)
                }
            }
        }
        
        return 20; // 블록이 없으면 최대값 반환
    }
    
    /**
     * 현재 필드 상태 복사
     */
    private boolean[][] copyField() {
        boolean[][] copy = new boolean[20][10];
        for (int r = 0; r < 20; r++) {
            for (int c = 0; c < 10; c++) {
                copy[r][c] = (gameManager.getFieldValue(r, c) != 0);
            }
        }
        return copy;
    }
    
    /**
     * 다음 블록을 위해 리셋
     */
    public void reset() {
        hasCalculatedMove = false;
        targetX = -1;
        targetRotation = 0;
        currentRotationCount = 0;
        lastBlock = null;
    }
}
