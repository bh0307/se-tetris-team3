package se.tetris.team3.gameManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class GarbageManagerTest {
    @Test
    @DisplayName("가비지 패턴 생성: 올바른 형태 반환")
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

    @Test
    @DisplayName("공격 줄 제한: 이미 10줄이 차 있으면 추가 공격 무시")
    void testEnqueueGarbageIgnoresWhenFull() throws Exception {
        GameManager gm = new GameManager();
        
        // 10줄을 먼저 추가
        for (int i = 0; i < 10; i++) {
            boolean[][] oneRow = new boolean[1][10];
            for (int j = 0; j < 10; j++) {
                oneRow[0][j] = (j != i); // 각 줄마다 다른 위치에 빈칸
            }
            gm.enqueueGarbage(oneRow);
        }
        
        // 현재 큐 크기 확인
        int sizeBefore = gm.getPendingGarbagePreview().size();
        assertEquals(10, sizeBefore, "10줄이 추가되어야 함");
        
        // 추가로 줄을 넣어봄 (무시되어야 함)
        boolean[][] extraRows = new boolean[3][10];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 10; j++) {
                extraRows[i][j] = (j != 0);
            }
        }
        gm.enqueueGarbage(extraRows);
        
        // 여전히 10줄이어야 함
        int sizeAfter = gm.getPendingGarbagePreview().size();
        assertEquals(10, sizeAfter, "10줄 이상은 추가되지 않아야 함");
    }

    @Test
    @DisplayName("공격 줄 제한: 여러 번의 공격으로 줄이 아래쪽에 추가됨")
    void testEnqueueGarbageAddsToBottom() throws Exception {
        GameManager gm = new GameManager();
        
        // 첫 번째 공격: 2줄
        boolean[][] firstAttack = new boolean[2][10];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 10; j++) {
                firstAttack[i][j] = (j != i); // 0번째 줄: [F,T,T...], 1번째 줄: [T,F,T...]
            }
        }
        gm.enqueueGarbage(firstAttack);
        
        // 두 번째 공격: 3줄
        boolean[][] secondAttack = new boolean[3][10];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 10; j++) {
                secondAttack[i][j] = (j != (i + 2)); // 2,3,4번 위치에 빈칸
            }
        }
        gm.enqueueGarbage(secondAttack);
        
        // 총 5줄이 되어야 함
        java.util.List<boolean[]> preview = gm.getPendingGarbagePreview();
        assertEquals(5, preview.size(), "총 5줄이 추가되어야 함");
        
        // 첫 번째 공격이 먼저 (인덱스 0, 1)
        assertFalse(preview.get(0)[0], "첫 번째 줄의 0번 위치는 빈칸");
        assertFalse(preview.get(1)[1], "두 번째 줄의 1번 위치는 빈칸");
        
        // 두 번째 공격이 나중에 (인덱스 2, 3, 4)
        assertFalse(preview.get(2)[2], "세 번째 줄의 2번 위치는 빈칸");
        assertFalse(preview.get(3)[3], "네 번째 줄의 3번 위치는 빈칸");
        assertFalse(preview.get(4)[4], "다섯 번째 줄의 4번 위치는 빈칸");
    }

    @Test
    @DisplayName("공격 줄 제한: 현재 줄 + 새 줄이 10을 넘으면 잘라냄")
    void testEnqueueGarbageTruncatesWhenExceeding10() throws Exception {
        GameManager gm = new GameManager();
        
        // 먼저 7줄 추가
        for (int i = 0; i < 7; i++) {
            boolean[][] oneRow = new boolean[1][10];
            for (int j = 0; j < 10; j++) {
                oneRow[0][j] = (j != i);
            }
            gm.enqueueGarbage(oneRow);
        }
        
        assertEquals(7, gm.getPendingGarbagePreview().size(), "7줄이 추가되어야 함");
        
        // 5줄을 더 추가 시도 (총 12줄이 되려고 함)
        boolean[][] fiveRows = new boolean[5][10];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                fiveRows[i][j] = (j != (i + 7)); // 7,8,9,0,1번 위치에 빈칸
            }
        }
        gm.enqueueGarbage(fiveRows);
        
        // 최대 10줄까지만 추가되어야 함 (7 + 3 = 10)
        java.util.List<boolean[]> preview = gm.getPendingGarbagePreview();
        assertEquals(10, preview.size(), "최대 10줄까지만 추가되어야 함");
        
        // 처음 7줄은 그대로
        for (int i = 0; i < 7; i++) {
            assertFalse(preview.get(i)[i], (i + 1) + "번째 줄의 " + i + "번 위치는 빈칸");
        }
        
        // 나머지 3줄만 추가됨 (5줄 중 앞의 3줄만)
        assertFalse(preview.get(7)[7], "8번째 줄의 7번 위치는 빈칸");
        assertFalse(preview.get(8)[8], "9번째 줄의 8번 위치는 빈칸");
        assertFalse(preview.get(9)[9], "10번째 줄의 9번 위치는 빈칸");
    }

    @Test
    @DisplayName("공격 줄 제한: 0줄 공격은 무시됨")
    void testEnqueueGarbageIgnoresEmptyAttack() throws Exception {
        GameManager gm = new GameManager();
        
        // null 추가
        gm.enqueueGarbage(null);
        assertEquals(0, gm.getPendingGarbagePreview().size(), "null은 무시되어야 함");
        
        // 빈 배열 추가
        gm.enqueueGarbage(new boolean[0][10]);
        assertEquals(0, gm.getPendingGarbagePreview().size(), "빈 배열은 무시되어야 함");
    }

    @Test
    @DisplayName("공격 줄 제한: 정확히 10줄 추가 가능")
    void testEnqueueGarbageExactly10Lines() throws Exception {
        GameManager gm = new GameManager();
        
        // 한 번에 10줄 추가
        boolean[][] tenRows = new boolean[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                tenRows[i][j] = (j != i);
            }
        }
        gm.enqueueGarbage(tenRows);
        
        assertEquals(10, gm.getPendingGarbagePreview().size(), "정확히 10줄이 추가되어야 함");
        
        // 각 줄의 빈칸 위치 확인
        for (int i = 0; i < 10; i++) {
            assertFalse(gm.getPendingGarbagePreview().get(i)[i], 
                (i + 1) + "번째 줄의 " + i + "번 위치는 빈칸");
        }
    }
}
