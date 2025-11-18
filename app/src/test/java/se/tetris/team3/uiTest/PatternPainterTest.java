package se.tetris.team3.uiTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import se.tetris.team3.ui.PatternPainter;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

@DisplayName("PatternPainter 실제 게임 상황 기반 테스트")
class PatternPainterTest {
    @Test
    @DisplayName("drawCell: OBlock, 일반모드 - OBlock이 baseColor 단색으로만 채워져야 한다")
    void testDrawCellOBlockNormalMode() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        se.tetris.team3.blocks.OBlock oBlock = new se.tetris.team3.blocks.OBlock();
        Color baseColor = Color.YELLOW;
        PatternPainter.drawCell(g2, 0, 0, 20, baseColor, oBlock, false);
        int rgb = img.getRGB(10, 10);
        Color result = new Color(rgb, true);
        assertTrue(Math.abs(result.getRed() - baseColor.getRed()) < 30);
        assertTrue(Math.abs(result.getGreen() - baseColor.getGreen()) < 30);
        assertTrue(Math.abs(result.getBlue() - baseColor.getBlue()) < 30);
    }

    @Test
    @DisplayName("drawCell: OBlock, 색약모드 - OBlock 타입에 맞는 패턴이 적용되어 셀이 baseColor 단색과 달라야 한다")
    void testDrawCellOBlockColorBlindMode() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        se.tetris.team3.blocks.OBlock oBlock = new se.tetris.team3.blocks.OBlock();
        Color baseColor = Color.YELLOW;
        PatternPainter.drawCell(g2, 0, 0, 20, baseColor, oBlock, true);
        int rgb = img.getRGB(10, 10);
        Color result = new Color(rgb, true);
        assertFalse(result.equals(baseColor));
    }

    @Test
    @DisplayName("drawCell: Block=null, 일반모드 - 블록 정보가 없을 때도 셀이 baseColor 단색으로만 채워져야 한다")
    void testDrawCellNullBlockNormalMode() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Color baseColor = Color.CYAN;
        PatternPainter.drawCell(g2, 0, 0, 20, baseColor, null, false);
        int rgb = img.getRGB(10, 10);
        Color result = new Color(rgb, true);
        assertTrue(Math.abs(result.getRed() - baseColor.getRed()) < 30);
        assertTrue(Math.abs(result.getGreen() - baseColor.getGreen()) < 30);
        assertTrue(Math.abs(result.getBlue() - baseColor.getBlue()) < 30);
    }

    @Test
    @DisplayName("drawCell: Block=null, 색약모드 - 블록 정보가 없을 때도 default 패턴이 적용되어 셀이 baseColor 단색과 달라야 한다")
    void testDrawCellNullBlockColorBlindMode() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Color baseColor = Color.CYAN;
        PatternPainter.drawCell(g2, 0, 0, 20, baseColor, null, true);
        int rgb = img.getRGB(10, 10);
        Color result = new Color(rgb, true);
        assertFalse(result.equals(baseColor));
    }

    @Test
    @DisplayName("drawPattern: 실제 블록 객체(IBlock)로 패턴이 적용되어 셀이 DARK_GRAY 단색과 달라야 한다")
    void testDrawPatternWithBlock() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        se.tetris.team3.blocks.IBlock iBlock = new se.tetris.team3.blocks.IBlock();
        PatternPainter.drawPattern(g2, 0, 0, 20, iBlock);
        int rgb = img.getRGB(10, 10);
        Color result = new Color(rgb, true);
        assertFalse(result.equals(Color.DARK_GRAY));
    }

    @Test
    @DisplayName("drawPattern: Block=null - 블록 정보가 없으면 아무 패턴도 적용되지 않고 셀이 투명(초기값)이어야 한다")
    void testDrawPatternNullBlock() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        PatternPainter.drawPattern(g2, 0, 0, 20, null);
        int rgb = img.getRGB(10, 10);
        Color result = new Color(rgb, true);
        assertEquals(0, result.getAlpha());
    }
}
