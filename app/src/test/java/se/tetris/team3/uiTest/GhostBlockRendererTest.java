package se.tetris.team3.uiTest;

import org.junit.jupiter.api.Test;
import se.tetris.team3.blocks.IBlock;
import se.tetris.team3.blocks.Block;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import se.tetris.team3.core.Settings;
import se.tetris.team3.ui.render.GhostBlockRenderer;
import se.tetris.team3.ui.render.GhostBlockRenderer.FieldValueProvider;

class GhostBlockRendererTest {
    @Test
    void testCalculateGhostY_simpleDrop() {
        Block block = new IBlock();
        int blockX = 3;
        int blockY = 0;
        int regionRows = 20;
        int regionCols = 10;
        // 빈 필드
        GhostBlockRenderer.FieldValueProvider field = (row, col) -> 0;
        int ghostY = GhostBlockRenderer.calculateGhostY(block, blockX, blockY, regionRows, regionCols, field);
        // IBlock의 최상단 좌표는 19 (필드 끝)
        assertEquals(19, ghostY);
    }

    @Test
    void testCalculateGhostY_withObstacle() {
        Block block = new IBlock();
        int blockX = 3;
        int blockY = 0;
        int regionRows = 20;
        int regionCols = 10;
        // 10번째 줄에 장애물
        GhostBlockRenderer.FieldValueProvider field = (row, col) -> (row == 10 && col >= 3 && col <= 6) ? 1 : 0;
        int ghostY = GhostBlockRenderer.calculateGhostY(block, blockX, blockY, regionRows, regionCols, field);
        // 장애물 위에 멈춰야 하므로 ghostY는 9 (장애물 바로 위)
        assertEquals(9, ghostY);
    }

    @Test
    void testRenderGhostBlock_drawsGhost() {
        Block block = new IBlock();
        int blockX = 3;
        int ghostY = 10;
        int regionRows = 20;
        int regionCols = 10;
        int blockSize = 20;
        int paddingX = 0;
        int paddingY = 0;
        Color ghostColor = new Color(128, 128, 128, 128);
        Settings settings = new Settings();
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        assertDoesNotThrow(() -> GhostBlockRenderer.renderGhostBlock(
                g2, block, blockX, ghostY, regionRows, regionCols, blockSize, paddingX, paddingY, ghostColor, settings));
        // IBlock의 마지막(아래쪽) 셀만 검사
        int[][] shape = block.getShape();
        int r = shape.length - 1;
        for (int c = 0; c < shape[r].length; c++) {
            if (shape[r][c] != 0) {
                int gx = blockX + c;
                int gy = ghostY + r;
                int x = paddingX + gx * blockSize + blockSize / 2;
                int y = paddingY + gy * blockSize + blockSize / 2;
                Color result = new Color(img.getRGB(x, y), true);
                assertTrue(result.getAlpha() > 0, "Ghost block bottom line should be drawn");
            }
        }
        g2.dispose();
    }
}
