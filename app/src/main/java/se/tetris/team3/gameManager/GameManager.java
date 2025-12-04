package se.tetris.team3.gameManager;

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
import se.tetris.team3.ui.render.PatternPainter;
import se.tetris.team3.ui.screen.GameScreen;

public class GameManager {
    private static final int FIELD_WIDTH = 10;
    private static final int FIELD_HEIGHT = 20;
    private static final int ANVIL_WIDTH = 4;

    // 공격(garbage) 블록 여부 표시
    private boolean[][] garbageMark = new boolean[FIELD_HEIGHT][FIELD_WIDTH];

    //10줄 규칙
    private static final int MAX_GARBAGE_QUEUE = 10;

    private final Random random = new Random();

    private int[][] field = new int[FIELD_HEIGHT][FIELD_WIDTH];
    private char[][] itemField = new char[FIELD_HEIGHT][FIELD_WIDTH]; // 아이템 타입 저장
    private Color[][] colorField = new Color[FIELD_HEIGHT][FIELD_WIDTH]; // 블록 색상 저장
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

    // 대전 모드용: 줄 삭제 콜백 + 상대에게서 넘어온 쓰레기 줄 큐
    private LineClearListener lineClearListener;
    private final java.util.List<boolean[]> pendingGarbage = new java.util.LinkedList<>();

    private Settings settings;

    // I-only 모드: 일정 시간 동안 I형 블록만 생성
    private boolean iOnlyModeActive = false;
    private long iOnlyModeEndMillis = 0L;

    // 블록 제거 시 발생하는 파티클 (스레드 안전하게 동기화)
    private java.util.List<Particle> particles = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    // 라인 삭제 시 플래시 효과
    private java.util.Set<Integer> flashingRows = new java.util.HashSet<>();

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
        }
    }

    // 난이도별 설정 적용
    private void applyDifficultySettings() {
        // 기본 딜레이는 1000ms (1초)
        baseFallDelay = 1000;
        switch (difficulty) {
            case EASY:
                scoreMultiplier = 0.8;
                break; // 점수 감소
            case NORMAL:
                scoreMultiplier = 1.0;
                break; // 기본
            case HARD:
                scoreMultiplier = 1.2;
                break; // 점수 보너스
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

    // 대전 모드: 줄 삭제 리스너 등록
    public void setLineClearListener(LineClearListener listener) {
        this.lineClearListener = listener;
    }

    // 상대에게서 넘어온 쓰레기 줄 추가 (BattleGameManager가 호출)
    public void enqueueGarbage(boolean[][] rows) {
        if (rows == null || rows.length == 0) return;

        int current = pendingGarbage.size();

        // 이미 10줄이면, 새로 온 공격은 전부 무시
        if (current >= MAX_GARBAGE_QUEUE) {
            return;
        }

        // 아직 여유가 있으면, 남은 칸까지만 앞에서부터 채운다
        int remaining = MAX_GARBAGE_QUEUE - current;

        for (int i = 0; i < rows.length && i < remaining; i++) {
            boolean[] r = rows[i];
            if (r != null && r.length == FIELD_WIDTH) {
                pendingGarbage.add(r.clone());
            }
        }
    }

    // 쓰레기 줄 미리보기용 (BattleScreen에서 UI 그릴 때 사용)
    public java.util.List<boolean[]> getPendingGarbagePreview() {
        return java.util.Collections.unmodifiableList(pendingGarbage);
    }

    // 테스트용 메서드
    protected Block makeRandomBlock() {
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
            case EASY:
                iWeight = (int) Math.round(baseWeight * 1.2);
                break;
            case HARD:
                iWeight = (int) Math.round(baseWeight * 0.8);
                break;
            default:
                iWeight = baseWeight;
                break;
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
        // 대전 모드: 새 블록이 나오기 전에 넘어온 쓰레기 줄을 먼저 보드에 추가
        applyPendingGarbage();

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
        // 아래로 한 칸 이동 시, 낙하 속도가 빨라졌으면 추가 점수 부여
        if (newX == blockX && newY == blockY + 1) {
            int curDelay = getGameTimerDelay();
            int baseDelay = getBaseFallDelay();
            int bonus = 1;
            if (curDelay < baseDelay) {
                // 속도가 빨라질수록 더 많은 점수 (예: 1 + (baseDelay - curDelay) / 100)
                bonus += Math.max(1, (baseDelay - curDelay) / 100);
            }
            score += getScoreWithMultiplier(bonus);
        }
        blockX = newX;
        blockY = newY;
        return true;
    }

    // 블록 회전
    public void rotateBlock() {
        currentBlock.rotate();
        if (isCollision(blockX, blockY, currentBlock.getShape())) {
            currentBlock.rotate(); currentBlock.rotate(); currentBlock.rotate(); // 원위치
        }
    }

    // 하드드롭: 블록을 바닥까지 즉시 내림
    public void hardDrop() {
        while (tryMove(blockX, blockY + 1));
        stepDownOrFix();
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
                    colorField[fieldY][fieldX] = currentBlock.getColor(); // 색상 저장
                    garbageMark[fieldY][fieldX] = false;                 // 직접 쌓은 블록

                    // 아이템 정보 저장
                    if (itemType != 0 && ir != null && ic != null && i == ir && j == ic) {
                        itemField[fieldY][fieldX] = itemType;
                    }
                }
            }
        }

        // 무게추는 고정 시 점수 부여하지 않음
        if (!(currentBlock instanceof AnvilItemBlock)) {
            score += getScoreWithMultiplier(10);
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

    // 대전 모드: 대기 중인 쓰레기 줄을 모두 보드 아래에 추가
    private void applyPendingGarbage() {
        while (!pendingGarbage.isEmpty()) {
            boolean[] pattern = pendingGarbage.remove(0);
            addGarbageRowToField(pattern);
        }
    }

    // 한 줄을 위로 밀고, 맨 아래에 pattern 모양으로 회색 블럭 추가
    private void addGarbageRowToField(boolean[] pattern) {
        if (pattern == null || pattern.length != FIELD_WIDTH) return;

        // 윗줄부터 한 칸씩 위로 밀어 올리기
        for (int y = 0; y < FIELD_HEIGHT - 1; y++) {
            System.arraycopy(field[y + 1], 0, field[y], 0, FIELD_WIDTH);
            System.arraycopy(itemField[y + 1], 0, itemField[y], 0, FIELD_WIDTH);
            System.arraycopy(colorField[y + 1], 0, colorField[y], 0, FIELD_WIDTH);
            System.arraycopy(garbageMark[y + 1], 0, garbageMark[y], 0, FIELD_WIDTH);
        }

        // 맨 아래 줄 채우기 (아이템은 없는 쓰레기 줄이므로 itemField는 0으로)
        for (int x = 0; x < FIELD_WIDTH; x++) {
            field[FIELD_HEIGHT - 1][x] = pattern[x] ? 1 : 0;
            itemField[FIELD_HEIGHT - 1][x] = 0;
            colorField[FIELD_HEIGHT - 1][x] = Color.GRAY;    // 회색 저장(더블체크용)
            garbageMark[FIELD_HEIGHT - 1][x] = pattern[x];   // 공격 블록 표시
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

        // 실제 줄 삭제 (아이템/색/garbage 정보도 함께 이동)
        for (int y = row; y > 0; y--) {
            System.arraycopy(field[y - 1], 0, field[y], 0, FIELD_WIDTH);
            System.arraycopy(itemField[y - 1], 0, itemField[y], 0, FIELD_WIDTH);
            System.arraycopy(colorField[y - 1], 0, colorField[y], 0, FIELD_WIDTH);
            System.arraycopy(garbageMark[y - 1], 0, garbageMark[y], 0, FIELD_WIDTH);
        }
        for (int x = 0; x < FIELD_WIDTH; x++) {
            field[0][x] = 0;
            itemField[0][x] = 0;
            colorField[0][x] = null;
            garbageMark[0][x] = false;
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
        // 아이템으로 인해 삭제되는 줄에 대해서도 기존 방식대로 점수 계산 (1줄 * 100 * scoreMultiplier)
        score += getScoreWithMultiplier(Math.round(1 * 100 * scoreMultiplier));

        linesClearedTotal++;

        if (mode == GameMode.ITEM && linesClearedTotal >= 10) {
            pendingItem = true;
            linesClearedTotal -= 10;
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
                    itemField[y][x] = 0;   // 아이템 정보도 제거
                    colorField[y][x] = null;
                    garbageMark[y][x] = false;
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
                    if (fy >= FIELD_HEIGHT)
                        return new int[]{Math.max(0, Math.min(fx, FIELD_WIDTH - 1)), FIELD_HEIGHT - 1};
                    if (fy >= 0 && fx >= 0 && fx < FIELD_WIDTH && field[fy][fx] == 1) {
                        return new int[]{fx, fy};
                    }
                }
            }
        }
        return null;
    }

    // 현재 필드에서 가득 찬 줄 인덱스 모으기 (실제 삭제는 하지 않음)
    private int[] findFullRows() {
        java.util.List<Integer> list = new java.util.ArrayList<>();
        for (int i = 0; i < FIELD_HEIGHT; i++) {
            boolean full = true;
            for (int j = 0; j < FIELD_WIDTH; j++) {
                if (field[i][j] == 0) { full = false; break; }
            }
            if (full) list.add(i);
        }
        int[] result = new int[list.size()];
        for (int k = 0; k < list.size(); k++) result[k] = list.get(k);
        return result;
    }

    // 줄을 삭제한 블럭 모양처럼 빈 칸이 존재
    private boolean[][] buildGarbagePattern(int[] fullRows) {
        if (currentBlock == null || fullRows == null || fullRows.length == 0) {
            return new boolean[0][];
        }

        boolean[][] garbage = new boolean[fullRows.length][FIELD_WIDTH];

        // 기본은 전부 채워진 상태(true)
        for (int i = 0; i < fullRows.length; i++) {
            java.util.Arrays.fill(garbage[i], true);
        }

        int[][] shape = currentBlock.getShape();

        // 현재 블럭이 차지하고 있던 칸은 빈 칸(false)으로 만든다.
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] == 0) continue;

                int gx = blockX + c;
                int gy = blockY + r;

                if (gx < 0 || gx >= FIELD_WIDTH || gy < 0 || gy >= FIELD_HEIGHT) continue;

                for (int i = 0; i < fullRows.length; i++) {
                    if (fullRows[i] == gy) {
                        garbage[i][gx] = false;
                    }
                }
            }
        }

        return garbage;
    }

    // 라인 제거 함수(무게추일 경우 점수 미집계)
    public void clearLines(boolean awardScore) {
        java.util.List<Integer> fullRows = new java.util.ArrayList<>();
        for (int i = FIELD_HEIGHT - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < FIELD_WIDTH; j++) if (field[i][j] == 0) { full = false; break; }
            if (full) fullRows.add(i);
        }

        if (!fullRows.isEmpty()) {
            // 플래시 효과: 해당 줄을 잠깐 하얗게 표시
            flashingRows.clear();
            flashingRows.addAll(fullRows);

            // 짧은 딜레이 후 실제 삭제
            new Thread(() -> {
                try {
                    Thread.sleep(100); // 100ms 플래시
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 실제 줄 삭제 (위에서 아래로 - 역순으로 삭제해야 인덱스 안 꼬임)
                for (int i = fullRows.size() - 1; i >= 0; i--) {
                    clearRow(fullRows.get(i));
                }
                flashingRows.clear();
            }).start();

            int lines = fullRows.size();
            if (awardScore) {
                score += getScoreWithMultiplier(Math.round(lines * 100 * scoreMultiplier));
            }
            linesClearedTotal += lines;
            if (linesClearedTotal / 10 > level - 1) level = linesClearedTotal / 10 + 1;
            speedUp = (level > 1);
            if (mode == GameMode.ITEM && linesClearedTotal >= 10) {
                pendingItem = true;
                linesClearedTotal -= 10;
            }
        }
    }
    public void clearLines() { clearLines(true); }

    // 자동 라인 체크 (렌더링 타이머에서 호출)
    public void autoCheckLines() {
        // 플래시 중이 아니고, 게임 오버가 아닐 때만 체크
        if (flashingRows.isEmpty() && !isGameOver) {
            clearLines(true);
        }
    }

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

            // 대전 모드: 여기서 몇 줄이 꽉 찼는지 보고, 2줄 이상이면 공격 이벤트 발생
            if (lineClearListener != null) {
                int[] fullRows = findFullRows(); // 아직 clearLines() 호출 전 상태
                if (fullRows.length >= 2) {
                    boolean[][] garbage = buildGarbagePattern(fullRows);
                    lineClearListener.onAttack(this, fullRows, garbage);
                }
            }

            clearLines(!isAnvil);  // 무게추는 점수 반영 안 함
            spawnNewBlock();
        }
    }

    // 게임 초기화
    public void resetGame() {
        field = new int[FIELD_HEIGHT][FIELD_WIDTH];
        itemField = new char[FIELD_HEIGHT][FIELD_WIDTH];
        colorField = new Color[FIELD_HEIGHT][FIELD_WIDTH];
        garbageMark = new boolean[FIELD_HEIGHT][FIELD_WIDTH];

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

        pendingGarbage.clear();

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
        int perLevelDecrease;
        switch (difficulty) {
            case EASY:
                perLevelDecrease = 100; // 레벨당 100ms 감소
                break;
            case HARD:
                perLevelDecrease = 150; // 레벨당 150ms 감소
                break;
            default:
                perLevelDecrease = 120; // NORMAL - 레벨당 120ms 감소
                break;
        }
        int delay = Math.max(50, base - (lvl - 1) * perLevelDecrease);
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

    public boolean isDoubleScoreActive() {
        if (doubleScoreActive && System.currentTimeMillis() > doubleScoreTime) {
            doubleScoreActive = false;  // 만료
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

    // 색상 정보 접근 메서드
    public Color getBlockColor(int row, int col) {
        if (row < 0 || row >= FIELD_HEIGHT || col < 0 || col >= FIELD_WIDTH) return null;
        return colorField[row][col];
    }

    // 라인 플래시 효과 확인
    public boolean isRowFlashing(int row) {
        return flashingRows.contains(row);
    }

    // HUD: 점수/레벨/난이도/다음블록(줄삭제는 L 문자 표기, 무게추는 전용 모양으로 구분)
    public void renderHUD(Graphics2D g2, int padding, int blockSize, int totalWidth) {
        g2.setColor(Color.WHITE);

        int fieldW = blockSize * 10;           // 보드 폭
        int hudX = padding + fieldW + 16;      // HUD 시작 X 좌표
        int hudWidth = Math.max(120, totalWidth - hudX - padding); // HUD 영역 너비

        // 폰트 크기를 화면 크기에 맞춰 동적 조절 (더 크게)
        // 기본: blockSize / 2, 최소 16, 최대 28
        int baseFontSize = Math.max(16, Math.min(28, Math.max(14, blockSize / 2)));
        g2.setFont(new Font("맑은 고딕", Font.BOLD, baseFontSize));

        // 행간도 폰트 크기에 맞춰 조절 (기본의 1.5배)
        int lineSpacing = (int)(baseFontSize * 1.5);
        int scoreY = padding + lineSpacing;

        // 점수 표시 (말줄임 처리)
        drawStringEllipsis(g2, "SCORE: " + score, hudX, scoreY, hudWidth - 8);

        // 레벨 표시
        drawStringEllipsis(g2, "LEVEL: " + level, hudX, scoreY + lineSpacing, hudWidth - 8);

        // 난이도 표시 (단문: E/N/H)
        String diffLabel;
        switch (difficulty) {
            case EASY:
                diffLabel = "E";
                break;
            case HARD:
                diffLabel = "H";
                break;
            default:
                diffLabel = "N";
                break;
        }
        drawStringEllipsis(g2, "DIFFICULTY: " + diffLabel, hudX, scoreY + lineSpacing * 2, hudWidth - 8);

        // 다음 블록 표시
        if (nextBlock != null) {
            int[][] shape = nextBlock.getShape();
            Color color = nextBlock.getColor();

            // NEXT 라벨은 iOnlyMode 상태와 관계없이 항상 같은 위치에 표시
            drawStringEllipsis(g2, "NEXT:", hudX, scoreY + lineSpacing * 4, hudWidth - 8);

            final boolean cb = (settings != null && settings.isColorBlindMode());

            // 다음 블록을 그릴 가로 영역을 hudWidth로 제한
            int cell = Math.max(14, Math.min(blockSize, (hudWidth - 8) / 4)); // 최대 4열 보이도록 조정, 크기 증가
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
            g2.setColor(Color.RED);
            int remaining = getSlowModeRemainingTime();
            drawStringEllipsis(g2, "SLOW: " + remaining + "s", hudX, scoreY + 200, hudWidth - 8);
        }

        // I-only 모드 남은 시간 표시
        if (iOnlyModeActive) {
            long rem = Math.max(0, iOnlyModeEndMillis - System.currentTimeMillis());
            String remS = String.format("I-MODE: %ds", (rem + 999) / 1000);
            int yPos = slowModeActive ? scoreY + 230 : scoreY + 200; // SLOW MODE 있으면 그 아래, 없으면 같은 위치
            g2.setColor(Color.GREEN);
            drawStringEllipsis(g2, remS, hudX, yPos, hudWidth - 8);
        }

        // 점수 2배 모드 표시 남은 시간 표시 (위치 스택: SLOW → I-MODE → 2x)
        if (doubleScoreActive) {
            int remain = (int) Math.ceil((doubleScoreTime - System.currentTimeMillis()) / 1000.0);
            // 기본 기준 위치는 SLOW와 동일
            int yPos = scoreY + 200;
            // SLOW가 보이면 그 아래
            if (slowModeActive) yPos += 30;
            // I-MODE가 보이면 그 아래
            if (iOnlyModeActive) yPos += 30;

            g2.setColor(Color.BLUE);
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
        // 어떤 칸(gridX, gridY)에서 튀어나오는지 (보드 그리드 좌표)
        float gridX, gridY;

        // 그 칸 중심 기준의 픽셀 오프셋
        float offsetX, offsetY;

        float vx, vy;         // 속도(오프셋에 적용)
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
            // 오프셋에만 속도 적용 (그리드 위치는 고정)
            offsetX += vx;
            offsetY += vy;
            vy += 0.2f;
            vx *= 0.98f;
            life--;
        }

        public boolean isDead() { return life <= 0; }

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

            // 실제 화면 좌표: 보드 기준 + 그리드 * blockSize + center + offset
            float px = originX + gridX * blockSize + (blockSize / 2.0f) + offsetX;
            float py = originY + gridY * blockSize + (blockSize / 2.0f) + offsetY;

            g2.fillOval((int) px, (int) py, size, size);
        }
    }

    public void updateParticles() {
        // 파티클 업데이트 및 죽은 파티클 제거 (동기화 블록 사용)
        synchronized(particles) {
            particles.removeIf(particle -> {
                particle.update();
                return particle.isDead();
            });
        }
    }

    // I-only 모드 활성화: 지정된 밀리초 동안 I형 블록만 생성
    public void activateIOnlyMode(int milliseconds) {
        if (mode != GameMode.ITEM) return; // 아이템 모드에서만 동작
        iOnlyModeActive = true;
        iOnlyModeEndMillis = System.currentTimeMillis() + Math.max(0, milliseconds);
        System.out.println("[GameManager] I-only mode activated for " + milliseconds + " ms");
    }

    public void renderParticles(Graphics2D g2, int originX, int originY, int blockSize) {
        synchronized (particles) {
            for (Particle particle : particles) {
                particle.render(g2, originX, originY, blockSize);
            }
        }
    }

    // 블록 파괴 효과 생성
    private void addBreakEffect(int gridX, int gridY) {
        Color blockColor = Color.LIGHT_GRAY;

        int particleCount = 8 + (int)(Math.random() * 5);

        for (int i = 0; i < particleCount; i++) {
            float angle = (float) (Math.random() * 2 * Math.PI);
            float speed = 2 + (float) (Math.random() * 4);

            float vx = (float) (Math.cos(angle) * speed);
            float vy = (float) (Math.sin(angle) * speed) - 1;

            float offsetX = -8 + (float) (Math.random() * 16);
            float offsetY = -8 + (float) (Math.random() * 16);

            particles.add(new Particle(
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

    // 공격 줄 여부
    public boolean isGarbage(int r, int c) {
        return garbageMark[r][c];
    }
}
