package se.tetris.team3.gameManager;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GarbageManagerTest {
    @Test
    void testBuildGarbagePatternReturnsCorrectShape() {
        GameManager gm = new GameManager();
        // 블록과 필드 상태를 세팅
        gm.spawnNewBlock();
        // 블록을 중앙에 위치시키고, 필드에 일부 채움
        int[][] shape = gm.getCurrentBlock().getShape();
        int blockX = gm.getBlockX();
        int blockY = gm.getBlockY();
        // 필드에 블록 모양대로 채움 (reflection 사용)
        try {
            java.lang.reflect.Field fieldArr = GameManager.class.getDeclaredField("field");
            fieldArr.setAccessible(true);
            int[][] field = (int[][]) fieldArr.get(gm);
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int fy = blockY + r;
                        int fx = blockX + c;
                        if (fy >= 0 && fy < field.length && fx >= 0 && fx < field[0].length) {
                            field[fy][fx] = 1;
                        }
                    }
                }
            }
        } catch (Exception e) {
            fail("필드 세팅 중 오류: " + e.getMessage());
        }
        // 가득 찬 줄 인덱스 생성
        int[] fullRows = {blockY};
        // buildGarbagePattern 호출
        boolean[][] garbage = null;
        try {
            java.lang.reflect.Method m = GameManager.class.getDeclaredMethod("buildGarbagePattern", int[].class);
            m.setAccessible(true);
            Object result = m.invoke(gm, (Object) fullRows);
            garbage = (boolean[][]) result;
        } catch (Exception e) {
            fail("buildGarbagePattern reflection 오류: " + e.getMessage());
        }
        // 결과 검증: 길이, true/false 패턴 등
        assertNotNull(garbage);
        assertEquals(fullRows.length, garbage.length);
        // 각 row에 최소 하나의 false(빈 칸)가 있는지 확인
        boolean hasEmpty = false;
        for (boolean cell : garbage[0]) {
            if (!cell) hasEmpty = true;
        }
        assertTrue(hasEmpty, "가비지 패턴에 빈 칸이 있어야 함");
    }
}
