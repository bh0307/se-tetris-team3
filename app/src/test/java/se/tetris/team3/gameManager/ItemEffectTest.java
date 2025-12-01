package se.tetris.team3.gameManager;

import org.junit.jupiter.api.Test;
import se.tetris.team3.core.GameMode;

import static org.junit.jupiter.api.Assertions.*;

class ItemEffectTest {
        // 헬퍼: currentBlock을 무게추로 세팅
        private void setCurrentBlockToAnvil(GameManager gm) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("currentBlock");
                f.setAccessible(true);
                f.set(gm, new se.tetris.team3.blocks.AnvilItemBlock());
            } catch (Exception e) { throw new RuntimeException(e); }
        }

        // 헬퍼: blockY 위치 설정
        private void setBlockY(GameManager gm, int y) {
            try {
                java.lang.reflect.Field f = GameManager.class.getDeclaredField("blockY");
                f.setAccessible(true);
                f.setInt(gm, y);
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    @Test
    void testAnvilBlockDestroysBlocksBelow() {
        GameManager gm = new GameManager(GameMode.ITEM);
        setCurrentBlockToAnvil(gm);
        setBlockY(gm, 17); // 무게추가 17에 위치, 18~19에 블록 배치
        setBlockX(gm, 0); // 무게추가 x좌표를 0으로 설정
        for (int col = 0; col < 4; col++) {
            fillColumn(gm, col, 18, 19);
        }
        int[][] before = getField(gm);
        for (int col = 0; col < 4; col++) {
            assertEquals(1, before[18][col], "아래 블록이 존재해야 함");
            assertEquals(1, before[19][col], "아래 블록이 존재해야 함");
        }

        gm.stepDownOrFix(); // 무게추 고정 및 효과 발동

        // 최대 2초 대기하며 블록이 깨지는지 확인
        boolean destroyed = false;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 2000) {
            int[][] after = getField(gm);
            boolean allZero = true;
            for (int col = 0; col < 4; col++) {
                if (after[18][col] != 0 || after[19][col] != 0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                destroyed = true;
                break;
            }
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
        assertTrue(destroyed, "무게추가 아래 블록을 깨트려야 함");
    }

    // 헬퍼: blockX 위치 설정
    private void setBlockX(GameManager gm, int x) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("blockX");
            f.setAccessible(true);
            f.setInt(gm, x);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // 헬퍼: 특정 열에 블록 배치
    private void fillColumn(GameManager gm, int col, int... rows) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            int[][] field = (int[][]) f.get(gm);
            for (int r : rows) {
                field[r][col] = 1;
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // 헬퍼: 필드 반환
    private int[][] getField(GameManager gm) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            return (int[][]) f.get(gm);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void testActivateIOnlyModeActivatesIOnly() {
        GameManager gm = new GameManager(GameMode.ITEM);
        gm.activateIOnlyMode(2); // 2초
        assertTrue(isIOnlyModeActive(gm), "I-only 모드 활성화");
    }

    @Test
    void testActivateDoubleScoreItemAndDuration() throws InterruptedException {
        GameManager gm = new GameManager(GameMode.ITEM);
        gm.activateDoubleScoreItem();
        assertTrue(gm.isDoubleScoreActive(), "점수 2배 버프 활성화");
        long remain = gm.getDoubleScoreRemainingSeconds();
        assertTrue(remain > 0, "버프 남은 시간 양수");
        Thread.sleep(1500);
        assertTrue(gm.getDoubleScoreRemainingSeconds() < remain, "시간 경과 후 남은 시간 감소");
    }

    @Test
    void testActivateSlowModeAndUpdate() throws Exception {
        GameManager gm = new GameManager(GameMode.ITEM);
        java.lang.reflect.Method m = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        m.setAccessible(true);
        m.invoke(gm);
        assertTrue(gm.isSlowModeActive(), "슬로우 모드 활성화");
        long remain = gm.getSlowModeRemainingTime();
        assertTrue(remain > 0, "슬로우 남은 시간 양수");
        Thread.sleep(1500);
        gm.updateSlowMode();
        assertTrue(gm.getSlowModeRemainingTime() < remain, "업데이트 후 남은 시간 감소");
    }

    // private 필드 접근 헬퍼
    private boolean getWeightLocked(GameManager gm) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("weightLocked");
            f.setAccessible(true);
            return f.getBoolean(gm);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private boolean isIOnlyModeActive(GameManager gm) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("iOnlyModeActive");
            f.setAccessible(true);
            return f.getBoolean(gm);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
