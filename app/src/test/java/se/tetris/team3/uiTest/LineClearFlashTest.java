package se.tetris.team3.uiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.gameManager.GameManager;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 라인 삭제 시 플래시 효과 테스트
 */
public class LineClearFlashTest {

    private GameManager manager;

    @BeforeEach
    public void setUp() {
        manager = new GameManager(GameMode.CLASSIC);
    }

    /**
     * Reflection을 사용하여 private 필드에 접근
     */
    private int[][] getField() throws Exception {
        Field fieldField = GameManager.class.getDeclaredField("field");
        fieldField.setAccessible(true);
        return (int[][]) fieldField.get(manager);
    }

    /**
     * flashingRows Set에 접근
     */
    @SuppressWarnings("unchecked")
    private Set<Integer> getFlashingRows() throws Exception {
        Field flashingRowsField = GameManager.class.getDeclaredField("flashingRows");
        flashingRowsField.setAccessible(true);
        return (Set<Integer>) flashingRowsField.get(manager);
    }

    @Test
    @DisplayName("라인이 꽉 차면 flashingRows에 해당 줄 번호가 추가된다")
    public void testFlashingRowsAddedWhenLineFull() throws Exception {
        int[][] field = getField();
        
        // 맨 아래 줄(19번)을 가득 채움
        for (int col = 0; col < 10; col++) {
            field[19][col] = 1;
        }
        
        // clearLines 호출
        manager.clearLines(true);
        
        // 잠시 대기 (플래시 효과가 시작되도록)
        Thread.sleep(50);
        
        Set<Integer> flashingRows = getFlashingRows();
        
        // flashingRows에 19번 줄이 포함되어야 함
        assertTrue(flashingRows.contains(19), "꽉 찬 줄이 flashingRows에 추가되어야 함");
    }

    @Test
    @DisplayName("여러 줄이 동시에 꽉 차면 모두 flashingRows에 추가된다")
    public void testMultipleFlashingRows() throws Exception {
        int[][] field = getField();
        
        // 18번, 19번 두 줄을 가득 채움
        for (int col = 0; col < 10; col++) {
            field[18][col] = 1;
            field[19][col] = 1;
        }
        
        manager.clearLines(true);
        
        Thread.sleep(50);
        
        Set<Integer> flashingRows = getFlashingRows();
        
        assertEquals(2, flashingRows.size(), "두 줄이 flashingRows에 추가되어야 함");
        assertTrue(flashingRows.contains(18), "18번 줄이 포함되어야 함");
        assertTrue(flashingRows.contains(19), "19번 줄이 포함되어야 함");
    }

    @Test
    @DisplayName("플래시 효과 후 flashingRows가 비워진다")
    public void testFlashingRowsClearedAfterDelay() throws Exception {
        int[][] field = getField();
        
        // 맨 아래 줄을 가득 채움
        for (int col = 0; col < 10; col++) {
            field[19][col] = 1;
        }
        
        manager.clearLines(true);
        
        // 플래시가 시작되었는지 확인
        Thread.sleep(50);
        Set<Integer> flashingRows = getFlashingRows();
        assertFalse(flashingRows.isEmpty(), "플래시 시작 후 flashingRows가 비어있지 않아야 함");
        
        // 150ms 대기 (100ms 플래시 + 여유)
        Thread.sleep(150);
        
        // flashingRows가 비워졌는지 확인
        assertTrue(flashingRows.isEmpty(), "플래시 종료 후 flashingRows가 비워져야 함");
    }

    @Test
    @DisplayName("isRowFlashing() 메서드가 플래시 중인 줄을 올바르게 반환한다")
    public void testIsRowFlashing() throws Exception {
        int[][] field = getField();
        
        // 19번 줄만 가득 채움
        for (int col = 0; col < 10; col++) {
            field[19][col] = 1;
        }
        
        // clearLines 호출 전에는 플래시 중이지 않음
        assertFalse(manager.isRowFlashing(19), "clearLines 호출 전에는 플래시 중이 아님");
        
        manager.clearLines(true);
        
        // 플래시 시작 대기
        Thread.sleep(50);
        
        // 19번 줄은 플래시 중
        assertTrue(manager.isRowFlashing(19), "19번 줄은 플래시 중이어야 함");
        
        // 다른 줄은 플래시 중이 아님
        assertFalse(manager.isRowFlashing(18), "18번 줄은 플래시 중이 아니어야 함");
        assertFalse(manager.isRowFlashing(0), "0번 줄은 플래시 중이 아니어야 함");
    }

    @Test
    @DisplayName("라인이 꽉 차지 않으면 플래시 효과가 발생하지 않는다")
    public void testNoFlashWhenLineNotFull() throws Exception {
        int[][] field = getField();
        
        // 19번 줄을 부분적으로만 채움 (9칸만)
        for (int col = 0; col < 9; col++) {
            field[19][col] = 1;
        }
        
        manager.clearLines(true);
        
        Thread.sleep(50);
        
        Set<Integer> flashingRows = getFlashingRows();
        
        assertTrue(flashingRows.isEmpty(), "꽉 차지 않은 줄은 플래시 효과가 없어야 함");
        assertFalse(manager.isRowFlashing(19), "부분적으로 찬 줄은 플래시 중이 아님");
    }

    @Test
    @DisplayName("clearLines 호출 시 기존 flashingRows가 초기화된다")
    public void testFlashingRowsResetOnNewClearLines() throws Exception {
        int[][] field = getField();
        
        // 첫 번째: 19번 줄 채우기
        for (int col = 0; col < 10; col++) {
            field[19][col] = 1;
        }
        
        manager.clearLines(true);
        Thread.sleep(50);
        
        Set<Integer> flashingRows = getFlashingRows();
        assertTrue(flashingRows.contains(19), "첫 번째 clearLines 후 19번 줄 포함");
        
        // 플래시 완료 대기
        Thread.sleep(150);
        
        // 두 번째: 18번 줄 채우기
        for (int col = 0; col < 10; col++) {
            field[18][col] = 1;
        }
        
        manager.clearLines(true);
        Thread.sleep(50);
        
        // 이전 flashingRows는 초기화되고 새로운 줄만 포함
        assertTrue(flashingRows.contains(18), "두 번째 clearLines 후 18번 줄 포함");
        assertFalse(flashingRows.contains(19), "이전 줄(19)은 포함되지 않아야 함");
    }

    @Test
    @DisplayName("플래시 타이밍: 100ms 동안만 플래시 상태가 유지된다")
    public void testFlashDuration() throws Exception {
        int[][] field = getField();
        
        for (int col = 0; col < 10; col++) {
            field[19][col] = 1;
        }
        
        long startTime = System.currentTimeMillis();
        manager.clearLines(true);
        
        // 30ms 후: 플래시 중이어야 함
        Thread.sleep(30);
        assertTrue(manager.isRowFlashing(19), "30ms 시점에 플래시 중");
        
        // 60ms 후: 여전히 플래시 중
        Thread.sleep(30);
        assertTrue(manager.isRowFlashing(19), "60ms 시점에 플래시 중");
        
        // 150ms 후: 플래시 종료
        Thread.sleep(90);
        assertFalse(manager.isRowFlashing(19), "150ms 시점에 플래시 종료");
        
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 150, "최소 150ms 경과");
    }
}
