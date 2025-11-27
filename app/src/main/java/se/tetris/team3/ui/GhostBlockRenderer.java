package se.tetris.team3.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import se.tetris.team3.blocks.Block;
import se.tetris.team3.core.Settings;

/**
 * 고스트 블록(하드 드롭 위치 미리보기) 렌더링 공통 클래스
 */
public class GhostBlockRenderer {
    /**
     * 고스트 블록의 하드 드롭 위치를 계산합니다.
     * @param block 현재 블록
     * @param blockX 블록 X 위치
     * @param blockY 블록 Y 위치
     * @param regionRows 필드 행 개수
     * @param regionCols 필드 열 개수
     * @param getFieldValue 필드 값 조회 함수 (row, col)
     * @return 고스트 블록의 Y 위치
     */
    public static int calculateGhostY(Block block, int blockX, int blockY, int regionRows, int regionCols, FieldValueProvider getFieldValue) {
        int[][] shape = block.getShape();
        int ghostY = blockY;
        while (true) {
            boolean canMove = true;
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int testY = ghostY + r + 1;
                        int testX = blockX + c;
                        if (testY >= regionRows || getFieldValue.get(testY, testX) != 0) {
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
        return ghostY;
    }

    /**
     * 고스트 블록을 렌더링합니다.
     */
    public static void renderGhostBlock(Graphics2D g2, Block block, int blockX, int ghostY, int regionRows, int regionCols, int blockSize, int paddingX, int paddingY, Color ghostColor, Settings settings) {
        int[][] shape = block.getShape();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int gx = blockX + c;
                    int gy = ghostY + r;
                    if (gx >= 0 && gx < regionCols && gy >= 0 && gy < regionRows) {
                        int x = paddingX + gx * blockSize;
                        int y = paddingY + gy * blockSize;
                        PatternPainter.drawCell(g2, x, y, blockSize, ghostColor, block, settings != null && settings.isColorBlindMode(), ghostColor.getAlpha());
                    }
                }
            }
        }
    }

    /**
     * 필드 값 조회용 함수형 인터페이스
     */
    public interface FieldValueProvider {
        int get(int row, int col);
    }
}
