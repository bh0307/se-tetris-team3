package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Random;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.blocks.IBlock;
import se.tetris.team3.blocks.JBlock;
import se.tetris.team3.blocks.LBlock;
import se.tetris.team3.blocks.OBlock;
import se.tetris.team3.blocks.SBlock;
import se.tetris.team3.blocks.TBlock;
import se.tetris.team3.blocks.ZBlock;
import se.tetris.team3.blocks.AnvilItemBlock;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;


// 게임 진행(보드/블록 상태/스폰/점수/아이템 효과) 총괄 매니저
public class GameManager {

    public void destroyBlocksBelow() {
        activateWeightEffect();
    }

    public void forceClearRowAtItem() {
        applyLineClearItem();
    }

    // 보드/레벨 기본
    private static final int FIELD_WIDTH = 10;
    private static final int FIELD_HEIGHT = 20;
    private static final int ANVIL_WIDTH = 4; // 무게추 효과 폭(4열)

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
    private boolean pendingItem = false; // 다음 블록을 아이템으로 예약?
    private boolean weightLocked = false; // 무게추: 고정 후 좌우 이동 금지

    // 생성/초기화
    public GameManager() { this(GameMode.CLASSIC); }

    public GameManager(GameMode mode) {
        this.mode = (mode != null ? mode : GameMode.CLASSIC);
        nextBlock = makeRandomBlock(); // 첫 프리뷰
        spawnNewBlock();               // 첫 현재블록
    }

    public void attachSettings(Settings settings) {
        if (settings != null && settings.getGameMode() != null) this.mode = settings.getGameMode();
    }

    // 외부 조회
    public GameMode getMode()           { return mode; }
    public int getFieldValue(int r,int c){ return field[r][c]; }
    public Block getCurrentBlock()      { return currentBlock; }
    public Block getNextBlock()         { return nextBlock; }
    public int getBlockX()              { return blockX; }
    public int getBlockY()              { return blockY; }
    public boolean isGameOver()         { return isGameOver; }
    public int getScore()               { return score; }
    public int getLevel()               { return level; }
    public boolean isSpeedUp()          { return speedUp; }

    // 블록 생성
    private Block makeRandomBlock() {
        return switch (random.nextInt(7)) {
            case 0 -> new IBlock();
            case 1 -> new JBlock();
            case 2 -> new LBlock();
            case 3 -> new OBlock();
            case 4 -> new SBlock();
            case 5 -> new TBlock();
            default -> new ZBlock();
        };
    }

    public void spawnNewBlock() {
        currentBlock = nextBlock;
        weightLocked = false;

        Block candidate = makeRandomBlock(); // 기본 다음 블록

        if (pendingItem) {
            // 아이템 종류 랜덤 선택: true=무게추, false=줄삭제
            boolean pickAnvil = random.nextBoolean();
            if (pickAnvil) {
                // 무게추는 "전용 조각(폭4)" 자체로 표현 (문자 불필요)
                candidate = new AnvilItemBlock();
                candidate.setItemType((char)0); // 혹시 남은 표식 제거
            } else {
                // 줄삭제: 일반 블록에 'L'을 블록 내부 임의 한 칸에 태깅
                candidate = makeRandomBlock();
                candidate.setItemType('L');
                int[][] shp = candidate.getShape();
                java.util.List<int[]> ones = new java.util.ArrayList<>();
                for (int r = 0; r < shp.length; r++)
                    for (int c = 0; c < shp[r].length; c++)
                        if (shp[r][c] == 1) ones.add(new int[]{r, c});
                if (!ones.isEmpty()) {
                    int[] pick = ones.get(random.nextInt(ones.size()));
                    // Block에 setItemCell가 있으면 직접 호출, 없으면 리플렉션 시도
                    try {
                        candidate.getClass().getMethod("setItemCell", int.class, int.class)
                                .invoke(candidate, pick[0], pick[1]);
                    } catch (Exception ignore) { /* Block에 API 없다면 표시만 없이 동작 */ }
                }
            }
            pendingItem = false;
        } else {
            // 아이템 예약이 아니면 아이템 표식 초기화
            candidate.setItemType((char)0);
            try { candidate.getClass().getMethod("setItemCell", int.class, int.class).invoke(candidate, -1, -1); }
            catch (Exception ignore) {}
        }

        nextBlock = candidate;

        blockX = FIELD_WIDTH / 2 - currentBlock.width() / 2;
        blockY = 0;
        blocksGenerated++;

        if (isCollision(blockX, blockY, currentBlock.getShape())) isGameOver = true;

        if (blocksGenerated / 20 > level - 1) level = blocksGenerated / 20 + 1;
        speedUp = (level > 1);
    }

    // 충돌/이동/회전 
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

    public boolean tryMove(int newX, int newY) {
        // 무게추: 고정 후 좌우 이동 금지
        if ((currentBlock instanceof AnvilItemBlock) && weightLocked && newX != blockX) return false;
        if (isCollision(newX, newY, currentBlock.getShape())) return false;
        blockX = newX; blockY = newY; return true;
    }

    public void rotateBlock() {
        currentBlock.rotate();
        if (isCollision(blockX, blockY, currentBlock.getShape())) {
            // 3회 역회전
            currentBlock.rotate(); currentBlock.rotate(); currentBlock.rotate();
        }
    }

    // 고정/아이템효과
    public void fixBlock() {
        int[][] s = currentBlock.getShape();
        for (int i = 0; i < s.length; i++)
            for (int j = 0; j < s[i].length; j++)
                if (s[i][j] != 0) field[blockY + i][blockX + j] = 1;

        score += 10;

        if (currentBlock instanceof AnvilItemBlock) {
            weightLocked = true; // 고정되면 좌우락
            // (무게추는 낙하 중에 이미 지우고 있어서 고정 시 추가 효과 없음)
        }

        if (mode == GameMode.ITEM && currentBlock.getItemType() == 'L') {
            applyLineClearItem(); // ‘그 줄’ 하나만 삭제(+100)
        }
    }

    private void clearRow(int row) {
        if (row < 0 || row >= FIELD_HEIGHT) return;
        for (int y = row; y > 0; y--) System.arraycopy(field[y - 1], 0, field[y], 0, FIELD_WIDTH);
        for (int x = 0; x < FIELD_WIDTH; x++) field[0][x] = 0;
    }

    // 줄삭제(L) : 블록 내부의 L이 붙은 그 줄 하나만 삭제 
    private void applyLineClearItem() {
        int rLocal = -1;
        // Block에 getItemRow()가 있으면 사용, 없으면 리플렉션
        try {
            rLocal = (int) currentBlock.getClass().getMethod("getItemRow").invoke(currentBlock);
        } catch (Exception ignore) {}
        if (rLocal < 0) return;

        int row = blockY + rLocal;
        if (row < 0 || row >= FIELD_HEIGHT) return;

        clearRow(row);
        score += 100;
        linesClearedTotal++;

        // 10줄마다 다음 아이템 예약 (종류는 스폰 때 랜덤 선택)
        if (mode == GameMode.ITEM && linesClearedTotal % 10 == 0) {
            pendingItem = true;
        }
    }

    // 무게추: 낙하 중 4열을 바닥까지 지속적으로 삭제 
    private void activateWeightEffect() {
        int startX = anvilStartX();
        int endX = startX + ANVIL_WIDTH - 1;
        for (int x = startX; x <= endX; x++)
            for (int y = FIELD_HEIGHT - 1; y >= 0; y--)
                if (field[y][x] == 1) field[y][x] = 0;
    }

    private int anvilStartX() {
        int center = blockX + currentBlock.width() / 2;
        int start = center - ANVIL_WIDTH / 2;
        return Math.max(0, Math.min(start, FIELD_WIDTH - ANVIL_WIDTH));
    }

    // 라인 클리어/레벨/아이템 예약
    public void clearLines() {
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
            score += lines * 100;
            linesClearedTotal += lines;
            if (linesClearedTotal / 10 > level - 1) level = linesClearedTotal / 10 + 1;
            speedUp = (level > 1);

            // 10줄마다 다음 아이템 예약 (종류는 스폰 시 랜덤)
            if (mode == GameMode.ITEM && linesClearedTotal % 10 == 0) {
                pendingItem = true;
            }
        }
    }

    public void stepDownOrFix() {
        // 무게추는 낙하 중 계속 뚫고 내려감
        if (currentBlock instanceof AnvilItemBlock) activateWeightEffect();

        if (!tryMove(blockX, blockY + 1)) {
            fixBlock();
            clearLines();
            spawnNewBlock();
        } else {
            score += 1 + (level - 1) * 5;
        }
    }

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
    }

    // HUD: 점수/레벨/다음블록(줄삭제는 L 문자 표기, 무게추는 전용 모양으로 구분)
    public void renderHUD(Graphics2D g2, int padding, int blockSize) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        int fieldW = blockSize * 10;
        int hudX = padding + fieldW + 16;
        int scoreY = padding + 24;

        g2.drawString("SCORE: " + score, hudX, scoreY);
        g2.drawString("LEVEL: " + level, hudX, scoreY + 22);

        if (nextBlock != null) {
            int[][] shape = nextBlock.getShape();
            Color color = nextBlock.getColor();
            g2.drawString("NEXT:", hudX, scoreY + 44);

            int cell = Math.max(8, blockSize / 2);
            Integer ir = null, ic = null;
            if (nextBlock.getItemType() == 'L') {
                try {
                    ir = (Integer) nextBlock.getClass().getMethod("getItemRow").invoke(nextBlock);
                    ic = (Integer) nextBlock.getClass().getMethod("getItemCol").invoke(nextBlock);
                } catch (Exception ignore) {}
            }

            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int x = hudX + c * cell;
                        int y = scoreY + 60 + r * cell;
                        g2.setColor(color);
                        g2.fillRect(x, y, cell - 1, cell - 1);

                        // L은 붙은 칸에만 문자 표시 (무게추는 글자 없음)
                        if (nextBlock.getItemType() == 'L' && ir != null && ic != null && r == ir && c == ic) {
                            GameScreen.drawCenteredChar(g2, x, y, cell, 'L');
                        }
                    }
                }
            }
        }
    }
}
