package se.tetris.team3.blocksTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import se.tetris.team3.ui.*;
import se.tetris.team3.ui.screen.GameScreen;
import se.tetris.team3.blocks.*;
import se.tetris.team3.core.*;
import se.tetris.team3.gameManager.GameManager;

@DisplayName("GameScreen 블록 스폰/정렬 테스트")
class BlockSpawnTest {
    AppFrame app;
    GameManager manager;
    GameScreen screen;

    @BeforeEach
    void setup() {
        app = new AppFrame();
        manager = new GameManager();
        manager.attachSettings(app.getSettings());
        screen = new GameScreen(app, manager);
    }

    @Test
    @DisplayName("새 블록 등장 시 중앙 정렬 및 필드 밖 방지")
    void testBlockSpawnCenteredAndInBounds() {
        Block[] blocks = {
            new IBlock(), new OBlock(), new TBlock(), new SBlock(), new ZBlock(), new JBlock(), new LBlock()
        };
        for (Block block : blocks) {
            setCurrentBlock(block);
            screen.onShow();
            int x = manager.getBlockX();
            int y = manager.getBlockY();
            int[][] shape = block.getShape();
            int fieldWidth = 10;
            int fieldHeight = 20;
            assertTrue(x >= 0 && x <= fieldWidth - shape[0].length, "블록 X 좌표가 필드 내에 있어야 함");
            assertTrue(y <= 0, "블록 Y 좌표가 스폰 위치(음수) 또는 0이어야 함");
            int expectedX = (fieldWidth - shape[0].length) / 2;
            assertTrue(Math.abs(x - expectedX) <= 1, "블록이 중앙에 정렬되어야 함");
        }
    }

    private void setCurrentBlock(Block block) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("currentBlock");
            f.setAccessible(true);
            f.set(manager, block);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
