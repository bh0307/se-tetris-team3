package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Random;

import se.tetris.team3.blocks.AnvilItemBlock;
import se.tetris.team3.blocks.Block;
import se.tetris.team3.blocks.IBlock;
import se.tetris.team3.blocks.JBlock;
import se.tetris.team3.blocks.LBlock;
import se.tetris.team3.blocks.OBlock;
import se.tetris.team3.blocks.SBlock;
import se.tetris.team3.blocks.TBlock;
import se.tetris.team3.blocks.ZBlock;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;


public class GameManager {

    private static final int FIELD_WIDTH = 10;
    private static final int FIELD_HEIGHT = 20;
    private static final int ANVIL_WIDTH = 4;

    private final Random random = new Random();

    private int[][] field = new int[FIELD_HEIGHT][FIELD_WIDTH];
    private char[][] itemField = new char[FIELD_HEIGHT][FIELD_WIDTH]; // 아이템 타입 저장
    private Block currentBlock;
    private Block nextBlock;
    private int blockX, blockY;

    private boolean isGameOver;
    private int score;
    private int level = 1;
    private int linesClearedTotal = 0;
    private int blocksGenerated = 0;
    private boolean speedUp = false;

    private GameMode mode = GameMode.CLASSIC;

    // 아이템 상태
    private boolean pendingItem = false;
    private boolean weightLocked = false;

    // 난이도 적용 변수
    private Settings.Difficulty difficulty = Settings.Difficulty.NORMAL;
    private int baseFallDelay = 500; // 기본 낙하 딜레이(ms)
    private double scoreMultiplier = 1.0;

    private Settings settings;

    // I-only 모드: 일정 시간 동안 I형 블록만 생성
    private boolean iOnlyModeActive = false;
    private long iOnlyModeEndMillis = 0L;

    // 블록 제거 시 발생하는 파티클
    private java.util.List<Particle> particles = new java.util.ArrayList<>();

    // T 아이템 느린 모드 상태
    private boolean slowModeActive = false;
    private long slowModeEndTime = 0;
    private static final long SLOW_MODE_DURATION = 10000; // 10초

    // D 아이템 점수 2배 상태         
    private boolean doubleScoreActive = false;
    private long doubleScoreTime = 0L;  // 점수 2배 아이템 지속시간 타이머
    private static final int DOUBLE_SCORE_DURATION = 10_000; // 10초

    // 생성자
    public GameManager() { 
        this(GameMode.CLASSIC); 
        // 아이템 필드 초기화
        for (int i = 0; i < FIELD_HEIGHT; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                itemField[i][j] = 0; // 아이템 없음
            }
        }
    }

    public GameManager(GameMode mode) {
        this.mode = (mode != null ? mode : GameMode.CLASSIC);
        nextBlock = makeRandomBlock();
        spawnNewBlock();
    }

    // 설정 객체 적용
    public void attachSettings(Settings settings) {
        if (settings != null) {
            this.settings = settings;
            difficulty = settings.getDifficulty();
            applyDifficultySettings();
            if (settings.getGameMode() != null) this.mode = settings.getGameMode();
        }
    }

    // 난이도별 설정 적용
    private void applyDifficultySettings() {
        switch (difficulty) {
            case EASY -> { baseFallDelay = 700; scoreMultiplier = 0.8; } // 느린 낙하, 점수 감소
            case NORMAL -> { baseFallDelay = 500; scoreMultiplier = 1.0; } // 기본
            case HARD -> { baseFallDelay = 300; scoreMultiplier = 1.2; } // 빠른 낙하, 점수 보너스
        }
    }

    // Getter
    public GameMode getMode() { return mode; }
    public int getFieldValue(int r,int c){ return field[r][c]; }
    public Block getCurrentBlock() { return currentBlock; }
    public Block getNextBlock() { return nextBlock; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public boolean isGameOver() { return isGameOver; }
    public int getScore() { return score; }
    public int getLevel() { return level; }
    // 난이도 기반 기본 낙하 딜레이 접근자
    public int getBaseFallDelay() { return baseFallDelay; }
    public boolean isSpeedUp() { return speedUp; }

    // 블록 생성
    private Block makeRandomBlock() {
        // I-only 모드가 활성화되어 있으면 남은 시간 동안 I블록만 반환
        if (iOnlyModeActive) {
            if (System.currentTimeMillis() > iOnlyModeEndMillis) {
                iOnlyModeActive = false;
            } else {
                return new IBlock();
            }
        }
        // Roulette Wheel Selection을 사용한 가중치 기반 블록 선택
        // 기본 가중치: 다른 블럭들은 10, I형 블럭은 난이도에 따라 +/-20%
        // EASY: I=12 (+20%), NORMAL: I=10, HARD: I=8 (-20%)
        int baseWeight = 10;
        int iWeight;
        switch (difficulty) {
            case EASY -> iWeight = (int) Math.round(baseWeight * 1.2);
            case HARD -> iWeight = (int) Math.round(baseWeight * 0.8);
            default -> iWeight = baseWeight;
        }

        // 블록 클래스 배열과 대응되는 가중치 배열
        java.util.List<Block> types = new java.util.ArrayList<>();
        java.util.List<Integer> weights = new java.util.ArrayList<>();

        types.add(new IBlock()); weights.add(iWeight);
        types.add(new JBlock()); weights.add(baseWeight);
        types.add(new LBlock()); weights.add(baseWeight);
        types.add(new OBlock()); weights.add(baseWeight);
        types.add(new SBlock()); weights.add(baseWeight);
        types.add(new TBlock()); weights.add(baseWeight);
        types.add(new ZBlock()); weights.add(baseWeight);

        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        if (total <= 0) return new IBlock(); // 안전 장치

        int r = random.nextInt(total);
        int acc = 0;
        for (int idx = 0; idx < weights.size(); idx++) {
            acc += Math.max(0, weights.get(idx));
            if (r < acc) return types.get(idx);
        }

        // 만약 루프가 끝나면 마지막 타입 반환
        return types.get(types.size() - 1);
    }

    // 새로운 블록 등장
    public void spawnNewBlock() {
        currentBlock = nextBlock;
        weightLocked = false;

        Block candidate = makeRandomBlock();

        // 아이템 블록 처리
        // 5개의 아이템 중에서 랜덤으로 선택되도록 설정
        if (pendingItem) {
            int randomItem = random.nextInt(5);
            if(randomItem==0){  // 무게추 아이템
                candidate = new AnvilItemBlock();
                candidate.setItemType((char)0);
                pendingItem = false;
            } else {
                candidate = makeRandomBlock();
                java.util.List<int[]> ones = new java.util.ArrayList<>();
                switch (randomItem) {
                case 1: // row 하나 지우기 아이템
                    candidate.setItemType('L');    
                    break;
                case 2: // 블럭 느리게 떨어지기 아이템
                    candidate.setItemType('T');
                    break;
                case 3: // 일정 시간 I형만 등장 아이템
                    candidate.setItemType('I');
                    break;
                default: // 일정 시간 점수 2배
                    candidate.setItemType('D');
                    break;
            }
            // 블럭 중 한 칸 랜덤으로 선택해서 아이템 삽입
            int[][] shp = candidate.getShape();
                    for (int r = 0; r < shp.length; r++)
                        for (int c = 0; c < shp[r].length; c++)
                        if (shp[r][c] == 1) ones.add(new int[]{r, c});
                    if (!ones.isEmpty()) {
                        int[] pick = ones.get(random.nextInt(ones.size()));
                        try {
                            candidate.getClass().getMethod("setItemCell", int.class, int.class)
                                    .invoke(candidate, pick[0], pick[1]);
                        } catch (Exception ignore) {}
                    }
                pendingItem = false;
            }
            
        } else {
            candidate.setItemType((char)0);
            try { candidate.getClass().getMethod("setItemCell", int.class, int.class).invoke(candidate, -1, -1); }
            catch (Exception ignore) {}
        }

        nextBlock = candidate;

        blockX = FIELD_WIDTH / 2 - currentBlock.width() / 2;
        blockY = 0;
        blocksGenerated++;

        // 생성 직후 충돌 시 게임 오버
        if (isCollision(blockX, blockY, currentBlock.getShape())) isGameOver = true;

        // 레벨 증가
        if (blocksGenerated / 20 > level - 1) level = blocksGenerated / 20 + 1;
        speedUp = (level > 1);
    }

    // 충돌 체크
    private boolean isCollision(int x, int y, int[][] shape) {
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] != 0) {
                    int fx = x + j, fy = y + i;
                    if (fx < 0 || fx >= FIELD_WIDTH || fy < 0 || fy >= FIELD_HEIGHT) return true;
                    if (field[fy][fx] == 1) return true;
                }
            }
        }
        return false;
    }

    // 블록 이동 시도
    public boolean tryMove(int newX, int newY) {
        if ((currentBlock instanceof AnvilItemBlock) && weightLocked && newX != blockX) return false;
        if (isCollision(newX, newY, currentBlock.getShape())) return false;
        blockX = newX; blockY = newY; return true;
    }

    // 블록 회전
    public void rotateBlock() {
        currentBlock.rotate();
        if (isCollision(blockX, blockY, currentBlock.getShape())) {
            currentBlock.rotate(); currentBlock.rotate(); currentBlock.rotate(); // 원위치
        }
    }

    // 블록 고정
    public void fixBlock() {
        int[][] s = currentBlock.getShape();
        
        // 아이템 위치 정보 가져오기
        Integer ir = null, ic = null;
        char itemType = currentBlock.getItemType();
        if (itemType != 0) {
            try {
                ir = (Integer) currentBlock.getClass().getMethod("getItemRow").invoke(currentBlock);
                ic = (Integer) currentBlock.getClass().getMethod("getItemCol").invoke(currentBlock);
            } catch (Exception ignore) {}
        }
        
        for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < s[i].length; j++) {
                if (s[i][j] != 0) {
                    int fieldY = blockY + i;
                    int fieldX = blockX + j;
                    
                    field[fieldY][fieldX] = 1;
                    
                    // 아이템 정보 저장
                    if (itemType != 0 && ir != null && ic != null && i == ir && j == ic) {
                        itemField[fieldY][fieldX] = itemType;
                    }
                }
            }
        }

        // 무게추는 고정 시 점수 부여하지 않음
        if (!(currentBlock instanceof AnvilItemBlock)) {
            //score += 10;
            score += getScoreWithMultiplier(10);
            // System.out.println(getScoreWithMultiplier(10)); 검증용

        }

        if (currentBlock instanceof AnvilItemBlock) weightLocked = true;

        // 아이템 모드 전용 라인클리어 아이템 발동 처리
        if (mode == GameMode.ITEM) {
            char it = currentBlock.getItemType();
            if (it == 'L') {
                applyLineClearItem();
            }
        }
    }

    // 한 줄 제거
    private void clearRow(int row) {
    if (row < 0 || row >= FIELD_HEIGHT) return;
    
    // 파티클 효과 생성 및 T 아이템 체크
    for (int x = 0; x < FIELD_WIDTH; x++) {
        if (field[row][x] == 1) {
            // T 아이템이나 I 아이템이 있는 블록이 깨지면 아이템 효과 실행
            char itemType = itemField[row][x];
            if (itemType == 'T') {
                activateTimeSlowItem();
            } else if (itemType == 'I') {
                // I-only 모드 10초 발동
                activateIOnlyMode(10000);
            } else if (itemType == 'D') {
                activateDoubleScoreItem();
            }
            
            addBreakEffect(x, row);
        }
    }
    
    // 실제 줄 삭제 (아이템 정보도 함께 이동)
    for (int y = row; y > 0; y--) {
        System.arraycopy(field[y - 1], 0, field[y], 0, FIELD_WIDTH);
        System.arraycopy(itemField[y - 1], 0, itemField[y], 0, FIELD_WIDTH);
    }
    for (int x = 0; x < FIELD_WIDTH; x++) {
        field[0][x] = 0;
        itemField[0][x] = 0;
    }
}

    // 아이템 '라인 제거' 적용
    public void applyLineClearItem() {
        int rLocal = -1;
        try { rLocal = (int) currentBlock.getClass().getMethod("getItemRow").invoke(currentBlock); } 
        catch (Exception ignore) {}
        if (rLocal < 0) return;

        int row = blockY + rLocal;
        if (row < 0 || row >= FIELD_HEIGHT) return;

        clearRow(row);
        //score += 100;
        score += getScoreWithMultiplier(100);
        // System.out.println(getScoreWithMultiplier(100)); 검증용

        linesClearedTotal++;

        if (mode == GameMode.ITEM && linesClearedTotal >= 2) {
                pendingItem = true;
                linesClearedTotal -= 2;
            }
    }

    // 무게 블록 효과: 충돌 순간 아래 블록 제거
    public void activateWeightEffectAt(int startX, int startY) {
        int sX = Math.max(0, Math.min(startX, FIELD_WIDTH - ANVIL_WIDTH));
        int endX = sX + ANVIL_WIDTH - 1;
        int sY = Math.max(0, startY);

        for (int x = sX; x <= endX; x++) {
            for (int y = sY; y < FIELD_HEIGHT; y++) {
                if (field[y][x] == 1) {
                    // T 아이템이나 I 아이템이 있는 블록이 무게추로 깨지면 아이템 효과 실행
                    char itemType = itemField[y][x];
                    if (itemType == 'T') {
                        activateTimeSlowItem();
                    } else if (itemType == 'I') {
                        // I-only 모드 10초 발동
                        activateIOnlyMode(10000);
                    } else if (itemType == 'D') {
                        activateDoubleScoreItem();
                    }
                    
                    addBreakEffect(x,y);
                    field[y][x] = 0;
                    itemField[y][x] = 0; // 아이템 정보도 제거
                }
            }
        }
    }

    // 충돌한 지점을 추정(블록을 y+1로 내리다 실패했을 때의 첫 접촉 셀)
    private int[] findFirstContactCell(int x, int y, int[][] shape) {
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] != 0) {
                    int fx = x + j;
                    int fy = y + i;
                    if (fy >= FIELD_HEIGHT) return new int[]{Math.max(0, Math.min(fx, FIELD_WIDTH - 1)), FIELD_HEIGHT - 1};
                    if (fy >= 0 && fx >= 0 && fx < FIELD_WIDTH && field[fy][fx] == 1) {
                        return new int[]{fx, fy};
                    }
                }
            }
        }
        return null;
    }
    
     // 라인 제거 함수(무게추일 경우 점수 미집계)
    public void clearLines(boolean awardScore) {
        int lines = 0;
        for (int i = FIELD_HEIGHT - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < FIELD_WIDTH; j++) if (field[i][j] == 0) { full = false; break; }
            if (full) {
                lines++;
                clearRow(i);
                i++;
            }
        }
        if (lines > 0) {
            if (awardScore) {
                // score += Math.round(lines * 100 * scoreMultiplier);
                score += getScoreWithMultiplier(Math.round(lines * 100 * scoreMultiplier));

                // System.out.println(getScoreWithMultiplier(Math.round(lines * 100 * scoreMultiplier))); 검증용

            }
            linesClearedTotal += lines;
            if (linesClearedTotal / 10 > level - 1) level = linesClearedTotal / 10 + 1;
            speedUp = (level > 1);
            if (mode == GameMode.ITEM && linesClearedTotal >= 2) {
                pendingItem = true;
                linesClearedTotal -= 2;
            }
        }
    }
    public void clearLines() { clearLines(true); }

    // 블록 한 칸 아래로 이동 또는 고정
    public void stepDownOrFix() {
        // 이동 중에는 무게추 효과를 받지 않음
        int nextY = blockY + 1;

        if (!tryMove(blockX, nextY)) {
            boolean isAnvil = (currentBlock instanceof AnvilItemBlock);

            if (isAnvil) {
                int[] contact = findFirstContactCell(blockX, nextY, currentBlock.getShape());
                int contactY = (contact != null) ? contact[1] : blockY;
                int startX = Math.max(0, Math.min(blockX, FIELD_WIDTH - ANVIL_WIDTH));
                activateWeightEffectAt(startX, contactY);

                // 천천히 아래로 떨어지게
                new Thread(() -> {
                    try {
                        while (!isCollision(blockX, blockY + 1, currentBlock.getShape())) {
                            blockY++;
                            Thread.sleep(40); // 낙하 속도 (조절 가능)
                        }
                        fixBlock();
                        clearLines(false); // 무게추는 점수 없음
                        spawnNewBlock();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

                return; // 아래 일반 고정 로직은 건너뜀
            }


            fixBlock();
            clearLines(!isAnvil);  // 무게추는 점수 반영 안 함
            spawnNewBlock();
        } else {
            //score += Math.round((1 + (level - 1) * 5) * scoreMultiplier);
            score += getScoreWithMultiplier((1 + (level - 1) * 5) * scoreMultiplier);

            // System.err.println(getScoreWithMultiplier((1 + (level - 1) * 5) * scoreMultiplier)); 검증용
        }
    }

    // 게임 초기화
    public void resetGame() {
        field = new int[FIELD_HEIGHT][FIELD_WIDTH];
        // 아이템 필드도 초기화
        for (int i = 0; i < FIELD_HEIGHT; i++) {
            for (int j = 0; j < FIELD_WIDTH; j++) {
                itemField[i][j] = 0;
            }
        }
        
        isGameOver = false;
        score = 0;
        nextBlock = makeRandomBlock();
        spawnNewBlock();
        level = 1;
        linesClearedTotal = 0;
        blocksGenerated = 0;
        speedUp = false;
        pendingItem = false;
        weightLocked = false;
        
        // 느린 모드 초기화
        slowModeActive = false;
        slowModeEndTime = 0;

        // 점수 2배 모드 초기화
        doubleScoreActive = false;
        doubleScoreTime = 0L;

        applyDifficultySettings();
    }

    // T 아이템 효과: 시간 느리게 하기
    private void activateTimeSlowItem() {
        slowModeActive = true;
        slowModeEndTime = System.currentTimeMillis() + SLOW_MODE_DURATION;
    }
    
    // 느린 모드 상태 체크 및 업데이트
    public void updateSlowMode() {
        if (slowModeActive && System.currentTimeMillis() >= slowModeEndTime) {
            slowModeActive = false;
        }
    }
    
    // 게임 타이머 딜레이 계산 (느린 모드 적용)
    public int getGameTimerDelay() {
        // 기본 딜레이 계산
        int base = baseFallDelay;
        int lvl = Math.max(1, level);
        int delay = Math.max(50, base - (lvl - 1) * 100);
        
        // 느린 모드가 활성화되면 속도를 절반(딜레이 2배)으로
        if (slowModeActive) {
            delay *= 2;
        }
        
        return delay;
    }
    
    // 느린 모드 상태 확인
    public boolean isSlowModeActive() {
        return slowModeActive;
    }
    
    // 느린 모드 남은 시간 (초 단위)
    public int getSlowModeRemainingTime() {
        if (!slowModeActive) return 0;
        long remaining = slowModeEndTime - System.currentTimeMillis();
        return Math.max(0, (int)(remaining / 1000));
    }

    public void activateDoubleScoreItem() {
        doubleScoreTime = System.currentTimeMillis() + DOUBLE_SCORE_DURATION;
        doubleScoreActive = true;
        System.out.println("Double Score Item activated!");
    }

    /** 기본 점수를 입력하면, 현재 버프에 따라 조정된 점수를 반환
     * int, long, double 삽입 가능 */
    private int getScoreWithMultiplier(int base)    { return isDoubleScoreActive() ? base * 2 : base; }
    private int getScoreWithMultiplier(long base)   { int v = (int) base; return isDoubleScoreActive() ? v * 2 : v; }
    private int getScoreWithMultiplier(double base) { int v = (int) Math.round(base); return isDoubleScoreActive() ? v * 2 : v; }

    public boolean isDoubleScoreActive() {
        if (doubleScoreActive && System.currentTimeMillis() > doubleScoreTime) {
            doubleScoreActive = false;  // 만료
            // System.out.println("Double Score Item expired!"); 검증용
        }
        return doubleScoreActive;
    }

    public int getDoubleScoreRemainingSeconds() {
        if (!doubleScoreActive) return 0;
        long rem = doubleScoreTime - System.currentTimeMillis();
        if (rem <= 0) { doubleScoreActive = false; return 0; }
        return (int) Math.ceil(rem / 1000.0);
    }
    
    // 아이템 정보 접근 메서드들
    public char getItemType(int row, int col) {
        if (row < 0 || row >= FIELD_HEIGHT || col < 0 || col >= FIELD_WIDTH) return 0;
        return itemField[row][col];
    }
    
    public boolean hasItem(int row, int col) {
        return getItemType(row, col) != 0;
    }

// HUD: 점수/레벨/난이도/다음블록(줄삭제는 L 문자 표기, 무게추는 전용 모양으로 구분)
public void renderHUD(Graphics2D g2, int padding, int blockSize, int totalWidth) {
    g2.setColor(Color.WHITE);
    
    int fieldW = blockSize * 10;           // 보드 폭
    int hudX = padding + fieldW + 16;      // HUD 시작 X 좌표
    int hudWidth = Math.max(120, totalWidth - hudX - padding); // HUD 영역 너비
    
    // 폰트 크기를 화면 크기에 맞춰 동적 조절
    int baseFontSize = Math.max(12, Math.min(24, blockSize / 2)); // 최소 12, 최대 24
    g2.setFont(new Font("맑은 고딕", Font.BOLD, baseFontSize));
    
    // 행간도 폰트 크기에 맞춰 동적 조절
    int lineSpacing = (int)(baseFontSize * 1.5);
    int scoreY = padding + lineSpacing;

    // 점수 표시 (말줄임 처리)
    drawStringEllipsis(g2, "SCORE: " + score, hudX, scoreY, hudWidth - 8);

    // 레벨 표시
    drawStringEllipsis(g2, "LEVEL: " + level, hudX, scoreY + lineSpacing, hudWidth - 8);

    // 난이도 표시 (전체 이름으로)
    String diffLabel = switch (difficulty) {
        case EASY -> "EASY";
        case HARD -> "HARD";
        default -> "NORMAL";
    };
    drawStringEllipsis(g2, "DIFFICULTY: " + diffLabel, hudX, scoreY + lineSpacing * 2, hudWidth - 8);

    // 다음 블록 표시
    if (nextBlock != null) {
        int[][] shape = nextBlock.getShape();
        Color color = nextBlock.getColor();

        // NEXT 라벨은 iOnlyMode 상태와 관계없이 항상 같은 위치에 표시
        drawStringEllipsis(g2, "NEXT:", hudX, scoreY + lineSpacing * 4, hudWidth - 8);

        final boolean cb = (settings != null && settings.isColorBlindMode());

        // 다음 블록을 그릴 가로 영역을 hudWidth로 제한
        int cell = Math.max(8, Math.min(blockSize / 2, (hudWidth - 8) / 4)); // 최대 4열 보이도록 조정
        Integer ir = null, ic = null;

        // 다음 블록이 줄삭제 아이템(L)인 경우 위치 조회
        if (nextBlock.getItemType() != 0) {
            try {
                ir = (Integer) nextBlock.getClass().getMethod("getItemRow").invoke(nextBlock);
                ic = (Integer) nextBlock.getClass().getMethod("getItemCol").invoke(nextBlock);
            } catch (Exception ignore) {}
        }

        // 다음 블록 그리기
        int nextBlockStartY = scoreY + lineSpacing * 4 + 20;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int x = hudX + c * cell;
                    int y = nextBlockStartY + r * cell;
                    /*
                    g2.setColor(color);
                    g2.fillRect(x, y, cell - 1, cell - 1);
                    */
                    PatternPainter.drawCell(g2, x, y, cell - 1, color, nextBlock, cb);
                    // L 아이템은 문자 표시
                    if (nextBlock.getItemType() != 0 && ir != null && ic != null && r == ir && c == ic) {
                        GameScreen.drawCenteredChar(g2, x, y, cell, nextBlock.getItemType());
                    }
                }
            }
        }

    }
    
    // 느린 모드 표시 남은 시간 표시
    if (slowModeActive) {
        g2.setColor(Color.CYAN);
        int remaining = getSlowModeRemainingTime();
        drawStringEllipsis(g2, "SLOW: " + remaining + "s", hudX, scoreY + 180, hudWidth - 8);
    }
    
    // I-only 모드 남은 시간 표시
    if (iOnlyModeActive) {
        long rem = Math.max(0, iOnlyModeEndMillis - System.currentTimeMillis());
        String remS = String.format("I-MODE: %ds", (rem + 999) / 1000);
        int yPos = slowModeActive ? scoreY + 204 : scoreY + 180; // SLOW MODE 있으면 그 아래, 없으면 같은 위치
        g2.setColor(Color.YELLOW);
        drawStringEllipsis(g2, remS, hudX, yPos, hudWidth - 8);
    }

    // 점수 2배 모드 표시 남은 시간 표시
    // 점수 2배 모드 표시 남은 시간 표시 (위치 스택: SLOW → I-MODE → 2x)
    if (doubleScoreActive) {
        int remain = (int) Math.ceil((doubleScoreTime - System.currentTimeMillis()) / 1000.0);
        // 기본 기준 위치는 SLOW와 동일
        int yPos = scoreY + 180;
        // SLOW가 보이면 그 아래
        if (slowModeActive) yPos += 24;
        // I-MODE가 보이면 그 아래 (SLOW가 없더라도, I-MODE가 있으면 한 칸 아래)
        if (iOnlyModeActive) yPos += 24;

        g2.setColor(Color.YELLOW); // 기존 색 유지
        String text = "2x SCORE: " + Math.max(0, remain) + "s";
        drawStringEllipsis(g2, text, hudX, yPos, hudWidth - 8);
    }
    
}

// 문자열을 주어진 최대 너비에 맞춰 그리고, 넘치면 말줄임표(...)로 대체
private void drawStringEllipsis(Graphics2D g2, String text, int x, int y, int maxWidth) {
    if (text == null) return;
    FontMetrics fm = g2.getFontMetrics();
    if (fm.stringWidth(text) <= maxWidth) {
        g2.drawString(text, x, y);
        return;
    }

    String ell = "...";
    int ellWidth = fm.stringWidth(ell);
    int avail = Math.max(0, maxWidth - ellWidth);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
        sb.append(text.charAt(i));
        if (fm.stringWidth(sb.toString()) > avail) {
            sb.setLength(Math.max(0, sb.length() - 1));
            break;
        }
    }
    g2.drawString(sb.toString() + ell, x, y);
}
private static class Particle {
    float x, y;           // 위치
    float vx, vy;         // 속도
    Color color;          // 색상
    int life;             // 생명력 (프레임)
    int maxLife;          // 최대 생명력
    
    public Particle(float x, float y, Color color, float vx, float vy) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.vx = vx;
        this.vy = vy;
        this.maxLife = 30 + (int)(Math.random() * 20); // 30~50 프레임
        this.life = maxLife;
    }
    
    public void update() {
        x += vx;
        y += vy;
        vy += 0.2f; // 중력
        vx *= 0.98f; // 공기 저항
        life--;
    }
    
    public boolean isDead() {
        return life <= 0;
    }
    
    public void render(Graphics2D g2, int blockSize) {
        if (isDead()) return;
        
        // 생명력에 따라 투명도 조절
        float alpha = (float)life / maxLife;
        Color fadeColor = new Color(
            color.getRed(), 
            color.getGreen(), 
            color.getBlue(), 
            (int)(255 * alpha)
        );
        
        g2.setColor(fadeColor);
        int size = Math.max(1, (int)(4 * alpha));
        g2.fillOval((int)x, (int)y, size, size);
    }
}

public void updateParticles() {
    // 파티클 업데이트 및 죽은 파티클 제거
    particles.removeIf(particle -> {
        particle.update();
        return particle.isDead();
    });
}

    // I-only 모드 활성화: 지정된 밀리초 동안 I형 블록만 생성
    public void activateIOnlyMode(int milliseconds) {
        if (mode != GameMode.ITEM) return; // 아이템 모드에서만 동작
        iOnlyModeActive = true;
        iOnlyModeEndMillis = System.currentTimeMillis() + Math.max(0, milliseconds);
        System.out.println("[GameManager] I-only mode activated for " + milliseconds + " ms");
    }

public void renderParticles(Graphics2D g2, int blockSize) {
    for (Particle particle : particles) {
        particle.render(g2, blockSize);
    }
}

// 블록 파괴 효과 생성
private void addBreakEffect(int gridX, int gridY) {
    float centerX = gridX * 30 + 15;
    float centerY = gridY * 30 + 15;
    Color blockColor = Color.LIGHT_GRAY;
    
    // 8~12개의 파티클 생성
    int particleCount = 8 + (int)(Math.random() * 5);
    
    for (int i = 0; i < particleCount; i++) {
        // 랜덤한 방향과 속도
        float angle = (float)(Math.random() * 2 * Math.PI);
        float speed = 2 + (float)(Math.random() * 4); // 2~6 픽셀/프레임
        
        float vx = (float)(Math.cos(angle) * speed);
        float vy = (float)(Math.sin(angle) * speed) - 1; // 약간 위로 튀어오르게
        
        // 블록 중심에서 약간씩 다른 위치에서 시작
        float offsetX = -8 + (float)(Math.random() * 16);
        float offsetY = -8 + (float)(Math.random() * 16);
        
        particles.add(new Particle(
            centerX + offsetX, 
            centerY + offsetY, 
            blockColor, 
            vx, 
            vy
        ));
    }
}
}