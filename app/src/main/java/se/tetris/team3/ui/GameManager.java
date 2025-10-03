package se.tetris.team3.ui;

import java.util.Random;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.blocks.IBlock;
import se.tetris.team3.blocks.JBlock;
import se.tetris.team3.blocks.LBlock;
import se.tetris.team3.blocks.OBlock;
import se.tetris.team3.blocks.SBlock;
import se.tetris.team3.blocks.TBlock;
import se.tetris.team3.blocks.ZBlock;

public class GameManager {

    private final int FIELD_WIDTH = 10;
    private final int FIELD_HEIGHT = 20;

    private int[][] field;
    private Block currentBlock;
    private int blockX, blockY;
    private boolean isGameOver;

    private Random random;

    public GameManager() {
        field = new int[FIELD_HEIGHT][FIELD_WIDTH];
        random = new Random();
        spawnNewBlock();
        isGameOver = false;
    }

    public int getFieldValue(int row, int col) {
        return field[row][col];
    }

    public Block getCurrentBlock() {
        return currentBlock;
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

    public void spawnNewBlock() {
        int blockType = random.nextInt(7);
        switch (blockType) {
            case 0 -> currentBlock = new IBlock();
            case 1 -> currentBlock = new JBlock();
            case 2 -> currentBlock = new LBlock();
            case 3 -> currentBlock = new OBlock();
            case 4 -> currentBlock = new SBlock();
            case 5 -> currentBlock = new TBlock();
            case 6 -> currentBlock = new ZBlock();
            default -> currentBlock = new IBlock();
        }
        blockX = FIELD_WIDTH / 2 - currentBlock.width() / 2;
        blockY = 0;

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
    }

    public void clearLines() {
        for (int i = FIELD_HEIGHT - 1; i >= 0; i--) {
            boolean fullLine = true;
            for (int j = 0; j < FIELD_WIDTH; j++) {
                if (field[i][j] == 0) {
                    fullLine = false;
                    break;
                }
            }
            if (fullLine) {
                for (int k = i; k > 0; k--) {
                    System.arraycopy(field[k - 1], 0, field[k], 0, FIELD_WIDTH);
                }
                for (int j = 0; j < FIELD_WIDTH; j++) {
                    field[0][j] = 0;
                }
                i++;
            }
        }
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
        spawnNewBlock();
    }
}
