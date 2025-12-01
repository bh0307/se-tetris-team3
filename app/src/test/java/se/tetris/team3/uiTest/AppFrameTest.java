package se.tetris.team3.uiTest;

import org.junit.jupiter.api.Test;
import se.tetris.team3.ui.AppFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class AppFrameTest {
    @Test
    void testAppFrameInitialization() {
        AppFrame frame = new AppFrame();
        frame.setVisible(true); // Ensure displayable
        assertNotNull(frame.getSettings());
        assertTrue(frame.isDisplayable());
    }

    @Test
    void testBackBufferPanelPaint() {
        AppFrame frame = new AppFrame();
        JPanel panel = (JPanel) frame.getContentPane();
        assertNotNull(panel);
        // Try to invoke paintComponent (simulate rendering)
        try {
            Graphics2D g2 = (Graphics2D) new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).getGraphics();
            panel.paint(g2); // Should not throw
        } catch (Exception e) {
            fail("paintComponent threw exception: " + e.getMessage());
        }
    }
}
