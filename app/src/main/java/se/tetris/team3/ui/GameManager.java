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
import se.tetris.team3.core.Settings;

public class GameManager {
    private final int FIELD_WIDTH = 10;
    private final int FIELD_HEIGHT = 20;

    private int[][] field;
    private Block currentBlock;
    private Block nextBlock;
    private int blockX, blockY;
    private boolean isGameOver;
    private int score;
    private Random random;

    private Settings settings;

    private int level = 1;
    private int linesClearedTotal = 0;
    private int blocksGenerated = 0;

    private boolean speedUp = false;

    public GameManager() {
        field = new int[FIELD_HEIGHT][FIELD_WIDTH];
        random = new Random();
        nextBlock = makeRandomBlock();
        spawnNewBlock();
        isGameOver = false;
        score = 0;
        level = 1;
        linesClearedTotal = 0;
        blocksGenerated = 0;
        speedUp = false;
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

    public int getLevel() {
        return level;
    }

    public boolean isSpeedUp() {
        return speedUp;
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
        nextBlock = makeRandomBlock();
        blockX = FIELD_WIDTH / 2 - currentBlock.width() / 2;
        blockY = 0;
        blocksGenerated++;
        if (blocksGenerated / 20 > level - 1) {
            level = blocksGenerated / 20 + 1;
        }
        speedUp = (level > 1);
        if (isCollision(blockX, blockY, currentBlock.getShape())) {
            isGameOver = true;
        }
    }

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
        score += 10;  // 블록 고정 점수
    }

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
        if (linesCleared > 0) {
            score += linesCleared * 100;
            linesClearedTotal += linesCleared;
            if (linesClearedTotal / 10 > level - 1) {
                level = linesClearedTotal / 10 + 1;
            }
            speedUp = (level > 1);
        }
    }

    // 누적 점수 증가 공식: 1, 6, 11, 16, ...
    private int getFallScoreByLevel(int level) {
        return 1 + (level - 1) * 5;
    }

    public void stepDownOrFix() {
        if (!tryMove(blockX, blockY + 1)) {
            fixBlock();
            clearLines();
            spawnNewBlock();
        } else {
            score += getFallScoreByLevel(level);
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
    }

    public void renderHUD(Graphics2D g2, int padding, int blockSize) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));

        int fieldW = blockSize * 10;
        Settings.SizePreset preset = (settings != null ? settings.getSizePreset() : Settings.SizePreset.MEDIUM);

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
        g2.drawString("LEVEL: " + level, hudX, scoreY + 22);

        if (nextBlock != null) {
            int[][] shape = nextBlock.getShape();
            Color color = nextBlock.getColor();

            g2.drawString("NEXT:", hudX, scoreY + 44);

            int boxX = hudX;
            int boxY = scoreY + 54;

            int rows = shape.length;
            int cols = (rows > 0 ? shape[0].length : 0);

            int cell = 0;
            if (rows > 0 && cols > 0) {
                int cellForWidth  = Math.max(6, (previewBox - 2) / cols);
                int cellForHeight = Math.max(6, (previewBox - 2) / rows);
                cell = Math.min(cellForWidth, cellForHeight);
            }
            int minCell = Math.max(8, blockSize / 2);
            int maxCell = Math.max(minCell + 2, blockSize - 2);
            cell = Math.max(minCell, Math.min(maxCell, cell));

            int totalW = cols * cell;
            int totalH = rows * cell;
            int px = boxX + Math.max(0, (previewBox - totalW) / 2);
            int py = boxY + Math.max(0, (previewBox - totalH) / 2);

            g2.setColor(new Color(255,255,255,30));
            g2.drawRect(boxX, boxY, previewBox, previewBox);

            final boolean cb = (settings != null && settings.isColorBlindMode());

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
