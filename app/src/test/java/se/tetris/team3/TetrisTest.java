package se.tetris.team3;

import org.junit.jupiter.api.Test;
import se.tetris.team3.ui.AppFrame;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TetrisTest {

    @Test
    void appFrameSmokeTest_headlessSafe() throws Exception {
        // If headless (CI), just check class loads.
        if (GraphicsEnvironment.isHeadless()) {
            assertDoesNotThrow(() -> Class.forName("se.tetris.team3.ui.AppFrame"));
            return;
        }

        // If not headless (local dev), actually construct and dispose the frame.
        assertDoesNotThrow(() ->
            SwingUtilities.invokeAndWait(() -> {
                new AppFrame().dispose();
            })
        );
    }
}
