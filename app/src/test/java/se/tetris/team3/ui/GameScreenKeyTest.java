
package se.tetris.team3.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

public class GameScreenKeyTest {
    private GameScreen gameScreen;
    private MockAppFrame mockApp;
    private Settings settings;
    private Object manager;

    @BeforeEach
    public void setUp() throws Exception {
        settings = new Settings();
        settings.resetDefaults();
        mockApp = new MockAppFrame(settings);
        gameScreen = new GameScreen(mockApp);
        Field managerField = GameScreen.class.getDeclaredField("manager");
        managerField.setAccessible(true);
        manager = managerField.get(gameScreen);
    }

    private int getBlockX() throws Exception {
        Field blockXField = manager.getClass().getDeclaredField("blockX");
        blockXField.setAccessible(true);
        return (int) blockXField.get(manager);
    }
    private int getBlockY() throws Exception {
        Field blockYField = manager.getClass().getDeclaredField("blockY");
        blockYField.setAccessible(true);
        return (int) blockYField.get(manager);
    }
    private Object getCurrentBlock() throws Exception {
        Field currentBlockField = manager.getClass().getDeclaredField("currentBlock");
        currentBlockField.setAccessible(true);
        return currentBlockField.get(manager);
    }

    private KeyEvent makeKeyEvent(int keyCode) {
        return new KeyEvent(mockApp, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    @Test
    public void testMoveLeftKey() throws Exception {
        int beforeX = getBlockX();
        KeyEvent leftEvent = makeKeyEvent(settings.getKeymap().get(Action.MOVE_LEFT));
        gameScreen.onKeyPressed(leftEvent);
        int afterX = getBlockX();
        assertEquals(beforeX - 1, afterX, "왼쪽 이동 키 입력 시 블록 X 좌표가 감소해야 함");
    }

    @Test
    public void testMoveRightKey() throws Exception {
        int beforeX = getBlockX();
        KeyEvent rightEvent = makeKeyEvent(settings.getKeymap().get(Action.MOVE_RIGHT));
        gameScreen.onKeyPressed(rightEvent);
        int afterX = getBlockX();
        assertEquals(beforeX + 1, afterX, "오른쪽 이동 키 입력 시 블록 X 좌표가 증가해야 함");
    }

    @Test
    public void testRotateKey() throws Exception {
        Object block = getCurrentBlock();
        int[][] beforeShape = (int[][]) block.getClass().getMethod("getShape").invoke(block);
        KeyEvent rotateEvent = makeKeyEvent(settings.getKeymap().get(Action.ROTATE));
        gameScreen.onKeyPressed(rotateEvent);
        Object blockAfter = getCurrentBlock();
        int[][] afterShape = (int[][]) blockAfter.getClass().getMethod("getShape").invoke(blockAfter);
        assertFalse(java.util.Arrays.deepEquals(beforeShape, afterShape), "회전 키 입력 시 블록 모양이 변경되어야 함");
    }

    @Test
    public void testSoftDropKey() throws Exception {
        int beforeY = getBlockY();
        KeyEvent downEvent = makeKeyEvent(settings.getKeymap().get(Action.SOFT_DROP));
        gameScreen.onKeyPressed(downEvent);
        int afterY = getBlockY();
        assertEquals(beforeY + 1, afterY, "소프트 드롭 키 입력 시 블록 Y 좌표가 증가해야 함");
    }

    @Test
    public void testHardDropKey() throws Exception {
        // 하드 드롭 실행
        KeyEvent hardDropEvent = makeKeyEvent(settings.getKeymap().get(Action.HARD_DROP));
        gameScreen.onKeyPressed(hardDropEvent);

        // 필드에서 가장 아래에 블록이 고정됐는지 확인
        Field fieldField = manager.getClass().getDeclaredField("field");
        fieldField.setAccessible(true);
        int[][] field = (int[][]) fieldField.get(manager);
        boolean blockAtBottom = false;
        for (int x = 0; x < field[0].length; x++) {
            if (field[field.length - 1][x] == 1) {
                blockAtBottom = true;
                break;
            }
        }
        assertTrue(blockAtBottom, "하드 드롭 후 블록이 바닥에 고정되어야 함");
    }

    // ===== Mock AppFrame =====
    private static class MockAppFrame extends AppFrame {
        private final Settings customSettings;
        public MockAppFrame(Settings settings) {
            super();
            this.customSettings = settings;
        }
        @Override
        public Settings getSettings() { return customSettings; }
        @Override
        public int getWidth() { return 480; }
        @Override
        public int getHeight() { return 640; }
    }
}
