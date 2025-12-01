package se.tetris.team3.gameManager;

import org.junit.jupiter.api.Test;
import se.tetris.team3.core.GameMode;

import static org.junit.jupiter.api.Assertions.*;

class FieldAndLineTest {
        @Test
        void testClearLinesAwardScoreFalseDoesNotChangeScore() throws InterruptedException {
            GameManager gm = new GameManager(GameMode.CLASSIC);
            int[][] field = getField(gm);
            for (int i = 0; i < field[19].length; i++) field[19][i] = 1;
            setField(gm, field);
            int scoreBefore = getScore(gm);
            gm.clearLines(false);
            Thread.sleep(200);
            int scoreAfter = getScore(gm);
            assertEquals(scoreBefore, scoreAfter, "awardScore=false일 때 점수 변화 없음");
        }

        @Test
        void testClearLinesLevelUpAndSpeedUp() throws InterruptedException {
            GameManager gm = new GameManager(GameMode.CLASSIC);
            int[][] field = getField(gm);
            // 10줄을 모두 채움
            for (int r = 10; r < 20; r++) for (int c = 0; c < field[r].length; c++) field[r][c] = 1;
            setField(gm, field);
            gm.clearLines();
            Thread.sleep(300);
            assertTrue(getLevel(gm) > 1, "10줄 삭제 후 레벨업");
            assertTrue(isSpeedUp(gm), "레벨업 후 speedUp true");
        }

        @Test
        void testClearLinesItemModeGivesPendingItem() throws InterruptedException {
            GameManager gm = new GameManager(GameMode.ITEM);
            int[][] field = getField(gm);
            // 10줄을 모두 채움
            for (int r = 10; r < 20; r++) for (int c = 0; c < field[r].length; c++) field[r][c] = 1;
            setField(gm, field);
            gm.clearLines();
            Thread.sleep(300);
            assertTrue(isPendingItem(gm), "아이템 모드에서 10줄 삭제 시 pendingItem true");
        }

        @Test
        void testFindFullRowsReturnsCorrectIndices() {
            GameManager gm = new GameManager(GameMode.CLASSIC);
            int[][] field = getField(gm);
            // 5, 7, 19번째 줄만 채움
            for (int c = 0; c < field[5].length; c++) field[5][c] = 1;
            for (int c = 0; c < field[7].length; c++) field[7][c] = 1;
            for (int c = 0; c < field[19].length; c++) field[19][c] = 1;
            setField(gm, field);
            int[] fullRows = invokeFindFullRows(gm);
            assertArrayEquals(new int[]{5, 7, 19}, fullRows, "가득 찬 줄 인덱스 반환");
        }

        // GameManager private 필드/메서드 접근 헬퍼
        private int getScore(GameManager gm) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("score");
                f.setAccessible(true);
                return f.getInt(gm);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        private int getLevel(GameManager gm) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("level");
                f.setAccessible(true);
                return f.getInt(gm);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        private boolean isSpeedUp(GameManager gm) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("speedUp");
                f.setAccessible(true);
                return f.getBoolean(gm);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        private boolean isPendingItem(GameManager gm) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("pendingItem");
                f.setAccessible(true);
                return f.getBoolean(gm);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        private int[] invokeFindFullRows(GameManager gm) {
            try {
                java.lang.reflect.Method m = GameManager.class.getDeclaredMethod("findFullRows");
                m.setAccessible(true);
                return (int[]) m.invoke(gm);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    @Test
    void testFieldInitializationIsEmpty() {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        int[][] field = getField(gm);
        for (int[] row : field) {
            for (int cell : row) {
                assertEquals(0, cell, "필드 초기화 시 모든 셀이 0이어야 함");
            }
        }
    }

    @Test
    void testLineClearRemovesFilledLine() throws InterruptedException {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        int[][] field = getField(gm);
        // 19번째 줄을 모두 채움
        for (int i = 0; i < field[19].length; i++) {
            field[19][i] = 1;
        }
        setField(gm, field);
        gm.clearLines();
        Thread.sleep(200); // 삭제 완료 대기
        field = getField(gm);
        for (int cell : field[19]) {
            assertEquals(0, cell, "클리어 후 마지막 줄이 0이어야 함");
        }
    }

    @Test
    void testMultipleLineClearRemovesMultipleLines() throws InterruptedException {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        int[][] field = getField(gm);
        // 18, 19번째 줄을 모두 채움
        for (int i = 0; i < field[19].length; i++) {
            field[19][i] = 1;
            field[18][i] = 1;
        }
        setField(gm, field);
        gm.clearLines();
        Thread.sleep(200); // 삭제 완료 대기
        field = getField(gm);
        for (int cell : field[19]) {
            assertEquals(0, cell, "클리어 후 마지막 줄이 0이어야 함");
        }
        for (int cell : field[18]) {
            assertEquals(0, cell, "클리어 후 그 위 줄도 0이어야 함");
        }
    }

    // 필드 getter
    private int[][] getField(GameManager gm) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            return (int[][]) f.get(gm);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    // 필드 setter
    private void setField(GameManager gm, int[][] field) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            f.set(gm, field);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
