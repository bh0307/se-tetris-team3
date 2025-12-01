package se.tetris.team3.gameManager;

import org.junit.jupiter.api.Test;
import se.tetris.team3.blocks.Block;
import se.tetris.team3.blocks.IBlock;
import se.tetris.team3.core.GameMode;

import static org.junit.jupiter.api.Assertions.*;

class BlockMovementAndSpawnTest {
            @Test
            void testSpawnNewBlockItemCellIsTagged() {
                GameManager gm = new GameManager(GameMode.ITEM);
                setPendingItem(gm, true);
                gm.spawnNewBlock();
                Block block = gm.getCurrentBlock();
                char type = block.getItemType();
                if (type != 0) { // 무게추는 제외
                    int itemRow = block.getItemRow();
                    int itemCol = block.getItemCol();
                    assertTrue(itemRow >= 0 && itemCol >= 0, "아이템 셀이 정상적으로 할당됨");
                    assertTrue(block.isItemCell(itemRow, itemCol), "아이템 셀 태깅 확인");
                }
            }
        @Test
        void testSpawnNewBlockCreatesItemBlockWhenPendingItem() {
            GameManager gm = new GameManager(GameMode.ITEM);
            setPendingItem(gm, true);
            gm.spawnNewBlock();
            Block block = gm.getCurrentBlock();
            // 아이템 타입이 올바르게 할당됐는지 확인 (무게추, D, T 등)
            char type = block.getItemType();
            assertTrue(type == 0 || type == 'L' || type == 'T' || type == 'I' || type == 'D', "아이템 타입이 올바르게 할당됨");
            assertFalse(isPendingItem(gm), "pendingItem이 false로 변경됨");
        }

        // 헬퍼: GameManager의 pendingItem 필드 설정
        private void setPendingItem(GameManager gm, boolean value) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("pendingItem");
                f.setAccessible(true);
                f.setBoolean(gm, value);
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        // 헬퍼: GameManager의 pendingItem 상태 반환
        private boolean isPendingItem(GameManager gm) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("pendingItem");
                f.setAccessible(true);
                return f.getBoolean(gm);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    @Test
    void testSpawnNewBlockInitializesCurrentBlock() {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        Block before = gm.getCurrentBlock();
        gm.spawnNewBlock();
        Block after = gm.getCurrentBlock();
        assertNotNull(after);
        assertNotEquals(before, after); // 새 블록 등장
    }

    @Test
    void testMakeRandomBlockReturnsBlock() {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        Block b = gm.makeRandomBlock();
        assertNotNull(b);
        assertTrue(b instanceof Block);
    }

    @Test
    void testStepDownOrFixMovesDownWhenEmptyBelow() {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        int yBefore = gm.getBlockY();
        // 밑에 아무것도 없는 상태
        gm.stepDownOrFix();
        int yAfter = gm.getBlockY();
        // 한 칸 내려가야 함
        assertEquals(yBefore + 1, yAfter);
    }

    @Test
    void testStepDownOrFixFixesBlockWhenBlockBelow() {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        // 밑에 블록이 있도록 필드 설정
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            int[][] field = (int[][]) f.get(gm);
            int blockX = gm.getBlockX();
            int blockY = gm.getBlockY();
            // 블록 바로 아래에 장애물 추가
            field[blockY + 1][blockX] = 1;
        } catch (Exception e) { throw new RuntimeException(e); }
        gm.stepDownOrFix();
        int yAfter = gm.getBlockY();
        // 고정 후 초기화되어야 함
        assertEquals(0, yAfter);
    }
}
