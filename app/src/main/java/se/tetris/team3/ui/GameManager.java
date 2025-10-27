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

    // 생성자
    public GameManager() { this(GameMode.CLASSIC); }

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
    public boolean isSpeedUp() { return speedUp; }

    // 블록 생성
    private Block makeRandomBlock() {
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
        if (pendingItem) {
            boolean pickAnvil = random.nextBoolean();
            if (pickAnvil) {
                candidate = new AnvilItemBlock();
                candidate.setItemType((char)0);
            } else {
                candidate = makeRandomBlock();
                candidate.setItemType('L');
                int[][] shp = candidate.getShape();
                java.util.List<int[]> ones = new java.util.ArrayList<>();
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
            }
            pendingItem = false;
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
        for (int i = 0; i < s.length; i++)
            for (int j = 0; j < s[i].length; j++)
                if (s[i][j] != 0) field[blockY + i][blockX + j] = 1;

        // 무게추는 고정 시 점수 부여하지 않음
        if (!(currentBlock instanceof AnvilItemBlock)) {
            score += 10;
        }

        if (currentBlock instanceof AnvilItemBlock) weightLocked = true;

        if (mode == GameMode.ITEM && currentBlock.getItemType() == 'L') applyLineClearItem();
    }

    // 한 줄 제거
    private void clearRow(int row) {
        if (row < 0 || row >= FIELD_HEIGHT) return;
        for (int y = row; y > 0; y--) System.arraycopy(field[y - 1], 0, field[y], 0, FIELD_WIDTH);
        for (int x = 0; x < FIELD_WIDTH; x++) field[0][x] = 0;
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
        score += 100;
        linesClearedTotal++;

        if (mode == GameMode.ITEM && linesClearedTotal % 10 == 0) pendingItem = true;
    }

    // 무게 블록 효과: 충돌 순간 아래 블록 제거
    public void activateWeightEffectAt(int startX, int startY) {
        int sX = Math.max(0, Math.min(startX, FIELD_WIDTH - ANVIL_WIDTH));
        int endX = sX + ANVIL_WIDTH - 1;
        int sY = Math.max(0, startY);

        for (int x = sX; x <= endX; x++) {
            for (int y = sY; y < FIELD_HEIGHT; y++) {
                if (field[y][x] == 1) field[y][x] = 0;
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
                for (int k = i; k > 0; k--) System.arraycopy(field[k - 1], 0, field[k], 0, FIELD_WIDTH);
                for (int j = 0; j < FIELD_WIDTH; j++) field[0][j] = 0;
                i++;
            }
        }
        if (lines > 0) {
            if (awardScore) score += Math.round(lines * 100 * scoreMultiplier);
            linesClearedTotal += lines;
            if (linesClearedTotal / 10 > level - 1) level = linesClearedTotal / 10 + 1;
            speedUp = (level > 1);
            if (mode == GameMode.ITEM && linesClearedTotal % 10 == 0) pendingItem = true;
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
            score += Math.round((1 + (level - 1) * 5) * scoreMultiplier);
        }
    }

    // 게임 초기화
    public void resetGame() {
        field = new int[FIELD_HEIGHT][FIELD_WIDTH];
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

        applyDifficultySettings();
    }

// HUD: 점수/레벨/난이도/다음블록(줄삭제는 L 문자 표기, 무게추는 전용 모양으로 구분)
public void renderHUD(Graphics2D g2, int padding, int blockSize, int totalWidth) {
    g2.setColor(Color.WHITE);
    g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));

    int fieldW = blockSize * 10;           // 보드 폭
    int hudX = padding + fieldW + 16;      // HUD 시작 X 좌표
    int scoreY = padding + 24;             // HUD 첫 Y 좌표
    int hudWidth = Math.max(120, totalWidth - hudX - padding); // HUD 영역 너비

    // 점수 표시 (말줄임 처리)
    drawStringEllipsis(g2, "SCORE: " + score, hudX, scoreY, hudWidth - 8);

    // 레벨 표시
    drawStringEllipsis(g2, "LEVEL: " + level, hudX, scoreY + 24, hudWidth - 8);

    // 난이도 표시 (한 글자로)
    String diffLabel = switch (difficulty) {
        case EASY -> "E";
        case HARD -> "H";
        default -> "N";
    };
    drawStringEllipsis(g2, "DIFFICULTY: " + diffLabel, hudX, scoreY + 48, hudWidth - 8);

    // 다음 블록 표시
    if (nextBlock != null) {
        int[][] shape = nextBlock.getShape();
        Color color = nextBlock.getColor();

        drawStringEllipsis(g2, "NEXT:", hudX, scoreY + 72, hudWidth - 8);

        final boolean cb = (settings != null && settings.isColorBlindMode());

        // 다음 블록을 그릴 가로 영역을 hudWidth로 제한
        int cell = Math.max(8, Math.min(blockSize / 2, (hudWidth - 8) / 4)); // 최대 4열 보이도록 조정
        Integer ir = null, ic = null;

        // 다음 블록이 줄삭제 아이템(L)인 경우 위치 조회
        if (nextBlock.getItemType() == 'L') {
            try {
                ir = (Integer) nextBlock.getClass().getMethod("getItemRow").invoke(nextBlock);
                ic = (Integer) nextBlock.getClass().getMethod("getItemCol").invoke(nextBlock);
            } catch (Exception ignore) {}
        }

        // 다음 블록 그리기
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int x = hudX + c * cell;
                    int y = scoreY + 88 + r * cell;
                    /*
                    g2.setColor(color);
                    g2.fillRect(x, y, cell - 1, cell - 1);
                    */
                    PatternPainter.drawCell(g2, x, y, cell - 1, color, nextBlock, cb);
                    // L 아이템은 문자 표시
                    if (nextBlock.getItemType() == 'L' && ir != null && ic != null && r == ir && c == ic) {
                        GameScreen.drawCenteredChar(g2, x, y, cell, 'L');
                    }
                }
            }
        }

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
}