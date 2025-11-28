package se.tetris.team3.itemTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.gameManager.GameManager;

/**
 * T 아이템 슬로우 모드 기능 테스트
 * - T 아이템 파괴 시 10초간 블록 낙하 속도 절반 감소
 */
public class TItemTest {

    private GameManager manager;

    @BeforeEach
    public void setUp() {
        manager = new GameManager(GameMode.ITEM);
    }

    @Test
    public void testSlowModeNotActiveInitially() {
        // 초기 상태: 슬로우 모드가 비활성화되어 있어야 함
        assertFalse(manager.isSlowModeActive(), "초기 상태에서 슬로우 모드는 비활성화되어야 함");
        assertEquals(0, manager.getSlowModeRemainingTime(), "초기 남은 시간은 0이어야 함");
    }

    @Test
    public void testSlowModeActivation() throws Exception {
        // T 아이템 활성화 메서드 호출 (리플렉션 사용)
        java.lang.reflect.Method activateMethod = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        activateMethod.setAccessible(true);
        activateMethod.invoke(manager);

        // 슬로우 모드가 활성화되어야 함
        assertTrue(manager.isSlowModeActive(), "T 아이템 활성화 후 슬로우 모드가 켜져야 함");
        
        // 남은 시간이 10초 근처여야 함 (9~10초)
        int remaining = manager.getSlowModeRemainingTime();
        assertTrue(remaining >= 9 && remaining <= 10, 
            "슬로우 모드 남은 시간은 9~10초 사이여야 함 (실제: " + remaining + "초)");
    }

    @Test
    public void testSlowModeTimerDelay() throws Exception {
        // 기본 딜레이 저장
        int normalDelay = manager.getGameTimerDelay();

        // T 아이템 활성화
        java.lang.reflect.Method activateMethod = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        activateMethod.setAccessible(true);
        activateMethod.invoke(manager);

        // 슬로우 모드 활성화 후 딜레이는 2배가 되어야 함
        int slowDelay = manager.getGameTimerDelay();
        assertEquals(normalDelay * 2, slowDelay, 
            "슬로우 모드에서 타이머 딜레이는 2배가 되어야 함");
    }

    @Test
    public void testSlowModeExpiration() throws Exception {
        // T 아이템 활성화
        java.lang.reflect.Method activateMethod = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        activateMethod.setAccessible(true);
        activateMethod.invoke(manager);

        assertTrue(manager.isSlowModeActive(), "활성화 직후 슬로우 모드가 켜져있어야 함");

        // 11초 대기 (10초 + 여유 1초)
        Thread.sleep(11000);

        // 슬로우 모드 상태 업데이트
        manager.updateSlowMode();

        // 슬로우 모드가 비활성화되어야 함
        assertFalse(manager.isSlowModeActive(), "10초 후 슬로우 모드가 꺼져야 함");
        assertEquals(0, manager.getSlowModeRemainingTime(), "만료 후 남은 시간은 0이어야 함");
    }

    @Test
    public void testSlowModeRemainingTimeDecrease() throws Exception {
        // T 아이템 활성화
        java.lang.reflect.Method activateMethod = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        activateMethod.setAccessible(true);
        activateMethod.invoke(manager);

        int initialRemaining = manager.getSlowModeRemainingTime();

        // 2초 대기
        Thread.sleep(2000);

        int afterRemaining = manager.getSlowModeRemainingTime();

        // 남은 시간이 감소해야 함
        assertTrue(afterRemaining < initialRemaining, 
            "시간이 지나면 남은 시간이 감소해야 함");
        
        // 약 2초 정도 감소했어야 함 (1~3초 범위 허용)
        int difference = initialRemaining - afterRemaining;
        assertTrue(difference >= 1 && difference <= 3, 
            "2초 대기 후 1~3초 정도 감소해야 함 (실제: " + difference + "초)");
    }

    @Test
    public void testSlowModeResetOnGameReset() throws Exception {
        // T 아이템 활성화
        java.lang.reflect.Method activateMethod = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        activateMethod.setAccessible(true);
        activateMethod.invoke(manager);

        assertTrue(manager.isSlowModeActive(), "활성화 후 슬로우 모드가 켜져있어야 함");

        // 게임 리셋
        manager.resetGame();

        // 슬로우 모드가 비활성화되어야 함
        assertFalse(manager.isSlowModeActive(), "게임 리셋 후 슬로우 모드가 꺼져야 함");
        assertEquals(0, manager.getSlowModeRemainingTime(), "리셋 후 남은 시간은 0이어야 함");
    }

    @Test
    public void testMultipleSlowModeActivation() throws Exception {
        // 첫 번째 활성화
        java.lang.reflect.Method activateMethod = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        activateMethod.setAccessible(true);
        activateMethod.invoke(manager);

        // 2초 대기
        Thread.sleep(2000);

        int remaining1 = manager.getSlowModeRemainingTime();

        // 두 번째 활성화 (시간 갱신)
        activateMethod.invoke(manager);

        int remaining2 = manager.getSlowModeRemainingTime();

        // 두 번째 활성화 후 시간이 다시 10초로 리셋되어야 함
        assertTrue(remaining2 > remaining1, 
            "재활성화 시 남은 시간이 갱신되어야 함");
        assertTrue(remaining2 >= 9 && remaining2 <= 10, 
            "재활성화 후 남은 시간은 9~10초 사이여야 함");
    }

    @Test
    public void testUpdateSlowModeBeforeExpiration() throws Exception {
        // T 아이템 활성화
        java.lang.reflect.Method activateMethod = GameManager.class.getDeclaredMethod("activateTimeSlowItem");
        activateMethod.setAccessible(true);
        activateMethod.invoke(manager);

        // 5초 대기 (만료 전)
        Thread.sleep(5000);

        manager.updateSlowMode();

        // 여전히 슬로우 모드가 활성화되어 있어야 함
        assertTrue(manager.isSlowModeActive(), "만료 전에는 슬로우 모드가 유지되어야 함");
        
        int remaining = manager.getSlowModeRemainingTime();
        assertTrue(remaining >= 4 && remaining <= 6, 
            "5초 후 남은 시간은 4~6초 사이여야 함 (실제: " + remaining + "초)");
    }
}
