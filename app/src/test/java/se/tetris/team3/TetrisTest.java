package se.tetris.team3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.tetris.team3.ui.AppFrame;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @DisplayName("Windows 10 이상이면 통과")
    void testWindows10OrHigherPass() {
        String origName = System.getProperty("os.name");
        String origVer = System.getProperty("os.version");
        try {
            System.setProperty("os.name", "Windows 10");
            System.setProperty("os.version", "10.0");
            assertDoesNotThrow(() -> Tetris.check());

            System.setProperty("os.name", "Windows 11");
            System.setProperty("os.version", "11.0");
            assertDoesNotThrow(() -> Tetris.check());
        } finally {
            System.setProperty("os.name", origName);
            System.setProperty("os.version", origVer);
        }
    }

    @Test
    @DisplayName("Windows 7이면 실패")
    void testWindows7Fail() {
        String origName = System.getProperty("os.name");
        String origVer = System.getProperty("os.version");
        try {
            System.setProperty("os.name", "Windows 7");
            System.setProperty("os.version", "6.1");
            assertThrows(RuntimeException.class, () -> Tetris.check());
        } finally {
            System.setProperty("os.name", origName);
            System.setProperty("os.version", origVer);
        }
    }

    @Test
    @DisplayName("Java 1.8 이상이면 통과")
    void testJava18OrHigherPass() {
        String origVer = System.getProperty("java.version");
        try {
            System.setProperty("java.version", "1.8.0_301");
            assertDoesNotThrow(() -> Tetris.check());
            System.setProperty("java.version", "11.0.2");
            assertDoesNotThrow(() -> Tetris.check());
        } finally {
            System.setProperty("java.version", origVer);
        }
    }

    @Test
    @DisplayName("Java 1.7이면 실패")
    void testJava17Fail() {
        String origVer = System.getProperty("java.version");
        try {
            System.setProperty("java.version", "1.7.0_80");
            assertThrows(RuntimeException.class, () -> Tetris.check());
        } finally {
            System.setProperty("java.version", origVer);
        }
    }
}
