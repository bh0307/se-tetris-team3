package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Random;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.blocks.IBlock; // PatternPainter를 GameManager에서 쓰려면 이것도
import se.tetris.team3.blocks.JBlock;
import se.tetris.team3.blocks.LBlock;
import se.tetris.team3.blocks.OBlock;
import se.tetris.team3.blocks.SBlock;
import se.tetris.team3.blocks.TBlock;
import se.tetris.team3.blocks.ZBlock;
import se.tetris.team3.core.Settings;

public class GameManager {

    private final int FIELD_WIDTH = 10;
    private final int FIELD_HEIGHT = 20;

    private int[][] field;
    private Block currentBlock;
    private Block nextBlock;   // 다음 블록
    private int blockX, blockY;
    private boolean isGameOver;
    private int score;
    private Random random;

    private Settings settings;

    public GameManager() {
        field = new int[FIELD_HEIGHT][FIELD_WIDTH];
        random = new Random();
        nextBlock = makeRandomBlock();   // NEXT 미리 준비
        spawnNewBlock();                 // 현재 블록으로 옮기기
        isGameOver = false;
        score = 0;
    }

    public void attachSettings(Settings settings) {
        this.settings = settings;
    }

    public int getFieldValue(int row, int col) {
        return field[row][col];
    }

    public Block getCurrentBlock() {
        return currentBlock;
    }

    public Block getNextBlock() {
        return nextBlock;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public int getScore() {
        return score;
    }

    private Block makeRandomBlock() {
        int blockType = random.nextInt(7);
        return switch (blockType) {
            case 0 -> new IBlock();
            case 1 -> new JBlock();
            case 2 -> new LBlock();
            case 3 -> new OBlock();
            case 4 -> new SBlock();
            case 5 -> new TBlock();
            case 6 -> new ZBlock();
            default -> new IBlock();
        };
    }

    public void spawnNewBlock() {
        currentBlock = nextBlock;
        nextBlock = makeRandomBlock();   // NEXT 갱신
        blockX = FIELD_WIDTH / 2 - currentBlock.width() / 2;
        blockY = 0;
        if (isCollision(blockX, blockY, currentBlock.getShape())) {
            isGameOver = true;
        }
    }

    // 이게 True 반환하면 위 메소드에서 게임오버 실행
    private boolean isCollision(int x, int y, int[][] shape) {
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] == 1) {
                    int fx = x + j;
                    int fy = y + i;
                    if (fx < 0 || fx >= FIELD_WIDTH || fy < 0 || fy >= FIELD_HEIGHT) {
                        return true;
                    }
                    if (field[fy][fx] == 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean tryMove(int newX, int newY) {
        if (isCollision(newX, newY, currentBlock.getShape())) {
            return false;
        } else {
            blockX = newX;
            blockY = newY;
            return true;
        }
    }

    public void rotateBlock() {
        currentBlock.rotate();
        if (isCollision(blockX, blockY, currentBlock.getShape())) {
            currentBlock.rotate();
            currentBlock.rotate();
            currentBlock.rotate();
        }
    }

    public void fixBlock() {
        int[][] shape = currentBlock.getShape();
        for (int i = 0; i < shape.length; i++) {
            for (int j = 0; j < shape[i].length; j++) {
                if (shape[i][j] == 1) {
                    field[blockY + i][blockX + j] = 1;
                }
            }
        }
    }

    // 라인을 다 채우면 점수 추가
    public void clearLines() {
        int linesCleared = 0;
        for (int i = FIELD_HEIGHT - 1; i >= 0; i--) {
            boolean fullLine = true;
            for (int j = 0; j < FIELD_WIDTH; j++) {
                if (field[i][j] == 0) {
                    fullLine = false;
                    break;
                }
            }
            if (fullLine) {
                linesCleared++;
                for (int k = i; k > 0; k--) {
                    System.arraycopy(field[k - 1], 0, field[k], 0, FIELD_WIDTH);
                }
                for (int j = 0; j < FIELD_WIDTH; j++) {
                    field[0][j] = 0;
                }
                i++;
            }
        }
        if (linesCleared > 0) score += linesCleared * 100;
    }

    public void stepDownOrFix() {
        if (!tryMove(blockX, blockY + 1)) {
            fixBlock();
            clearLines();
            spawnNewBlock();
        }
    }

    public void resetGame() {
        field = new int[FIELD_HEIGHT][FIELD_WIDTH];
        isGameOver = false;
        score = 0;
        nextBlock = makeRandomBlock();
        spawnNewBlock();
    }

    // HUD(점수, 다음 블록 등)를 그리는 메서드 추가
    // **** 화면 크기에 관계없이 계속 글씨 크기가 똑같은데 이걸 어떻게 해결해야할까? (메인에도)

    public void renderHUD(Graphics2D g2, int padding, int blockSize) {
        // 점수
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        // 필드 폭과 HUD 시작점 계산 (우측 고정 배치)
        int fieldW = blockSize * 10;
        Settings.SizePreset preset = (settings != null ? settings.getSizePreset() : Settings.SizePreset.MEDIUM);

        // 프리셋별 간격/프리뷰 박스 크기
        final int gutter = switch (preset) {
            case SMALL -> Math.max(8, blockSize / 2);
            case MEDIUM -> Math.max(12, blockSize * 2 / 3);
            case LARGE -> Math.max(16, blockSize * 3 / 4);
        };
        final int previewBox = switch (preset) {
            case SMALL -> blockSize * 3;
            case MEDIUM -> blockSize * 4;
            case LARGE -> blockSize * 5;
        };

        int hudX = padding + fieldW + gutter;
        int scoreY = padding + 24;

        g2.drawString("SCORE: " + score, hudX, scoreY);

        if (nextBlock != null) {
            int[][] shape = nextBlock.getShape();
            Color color = nextBlock.getColor();

            g2.drawString("NEXT:", hudX, scoreY + 28);

            int boxX = hudX;
            int boxY = scoreY + 36;

            int rows = shape.length;
            int cols = (rows > 0 ? shape[0].length : 0);

            // 프리뷰 박스에 맞춰 셀 크기 산정
            int cell = 0;
            if (rows > 0 && cols > 0) {
                int cellForWidth  = Math.max(6, (previewBox - 2) / cols);
                int cellForHeight = Math.max(6, (previewBox - 2) / rows);
                cell = Math.min(cellForWidth, cellForHeight);
            }
            // 지나치게 작거나 크게 보이지 않도록 클램프
            int minCell = Math.max(8, blockSize / 2);
            int maxCell = Math.max(minCell + 2, blockSize - 2);
            cell = Math.max(minCell, Math.min(maxCell, cell));

            // 중앙 정렬 오프셋
            int totalW = cols * cell;
            int totalH = rows * cell;
            int px = boxX + Math.max(0, (previewBox - totalW) / 2);
            int py = boxY + Math.max(0, (previewBox - totalH) / 2);

            // 가이드 박스(선택)
            g2.setColor(new Color(255,255,255,30));
            g2.drawRect(boxX, boxY, previewBox, previewBox);

            final boolean cb = (settings != null && settings.isColorBlindMode());

            // 블록 그리기
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (shape[r][c] != 0) {
                        int x = px + c * cell;
                        int y = py + r * cell;
                        PatternPainter.drawCell(g2, x, y, cell, color, null, cb);
                    }
                }
            }
        }
    }
}
