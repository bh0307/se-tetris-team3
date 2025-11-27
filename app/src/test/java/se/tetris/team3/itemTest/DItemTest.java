package se.tetris.team3.itemTest;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.ui.GameManager;

/**
 * D 아이템(점수 2배) 기능 테스트
 * - D 아이템 파괴 시 10초간 점수 2배가 적용되는지 검증
 * - TItemTest와 동일한 패턴(리플렉션으로 활성화 메서드 호출, 타이머/상태 확인)
 */
public class DItemTest {

    private GameManager manager;

    @BeforeEach
    public void setup() {
        // 아이템 모드(또는 실제 사용하는 모드)로 초기화
        manager = new GameManager(GameMode.ITEM);
    }

    @Test
    public void testInitialState() throws Exception {
        // isDoubleScoreActive()가 공개되어 있으면 그대로 호출
        // 없으면 리플렉션으로 호출
        boolean active = callIsDoubleScoreActive(manager);
        assertFalse(active, "초기에는 점수 2배 모드가 꺼져 있어야 함");

        Integer remain = tryCallGetDoubleScoreRemainingTime(manager);
        if (remain != null) {
            assertEquals(0, remain.intValue(), "초기 남은 시간은 0이어야 함");
        }
    }

    @Test
    public void testActivation() throws Exception {
        // private일 수 있으므로 리플렉션으로 활성화
        callActivateDoubleScoreItem(manager);

        // 바로 활성화되었는지 확인
        assertTrue(callIsDoubleScoreActive(manager), "D 아이템 활성화 후에는 점수 2배 모드가 켜져야 함");

        // 남은 시간(초)이 9~10 근처인지 (메서드가 있다면) 확인
        Integer remain = tryCallGetDoubleScoreRemainingTime(manager);
        if (remain != null) {
            assertTrue(remain >= 9 && remain <= 10,
                "활성 직후 남은 시간은 9~10초 사이여야 함 (실제: " + remain + "초)");
        }
    }

    @Test
    public void testExpire() throws Exception {
        callActivateDoubleScoreItem(manager);

        // 만료 전에 상태 유지되는지 중간 점검 (예: 5초 대기)
        Thread.sleep(5000);
        tryCallUpdateDoubleScore(manager); // 만료 갱신 메서드가 있으면 호출

        assertTrue(callIsDoubleScoreActive(manager), "만료 전(5초)에는 여전히 2배 모드여야 함");

        // 만료 시점 넘기기 (총 11초 경과)
        Thread.sleep(6000);
        tryCallUpdateDoubleScore(manager);

        assertFalse(callIsDoubleScoreActive(manager), "만료 후에는 2배 모드가 꺼져야 함");
    }

    // ----------------------------------------------------------------------
    // 아래는 리플렉션 유틸: GameManager에 메서드가 public이면 그냥 호출되고,
    // private/protected면 setAccessible(true)로 접근합니다.
    // 팀의 실제 메서드명과 정확히 맞추세요:
    //  - activateDoubleScoreItem()
    //  - isDoubleScoreActive()
    //  - getDoubleScoreRemainingTime()  (있으면)
    //  - updateDoubleScore()            (있으면)
    // ----------------------------------------------------------------------

    private void callActivateDoubleScoreItem(GameManager gm) throws Exception {
        try {
            // public인 경우
            GameManager.class.getMethod("activateDoubleScoreItem").invoke(gm);
        } catch (NoSuchMethodException e) {
            // private인 경우
            Method m = GameManager.class.getDeclaredMethod("activateDoubleScoreItem");
            m.setAccessible(true);
            m.invoke(gm);
        }
    }

    private boolean callIsDoubleScoreActive(GameManager gm) throws Exception {
        try {
            // public
            return (boolean) GameManager.class.getMethod("isDoubleScoreActive").invoke(gm);
        } catch (NoSuchMethodException e) {
            // private
            Method m = GameManager.class.getDeclaredMethod("isDoubleScoreActive");
            m.setAccessible(true);
            return (boolean) m.invoke(gm);
        }
    }

    /** 남은 시간을 초 단위 int로 반환하는 메서드가 없을 수도 있어 optional */
    private Integer tryCallGetDoubleScoreRemainingTime(GameManager gm) {
        try {
            Method m = GameManager.class.getMethod("getDoubleScoreRemainingTime");
            Object r = m.invoke(gm);
            if (r instanceof Integer) return (Integer) r;
            if (r instanceof Number) return ((Number) r).intValue();
        } catch (Exception ignore) {}
        return null;
        // 없으면 테스트를 스킵(다른 단언으로 커버)
    }

    /** 만료 갱신 전용 메서드가 있으면 호출 (없으면 넘어감) */
    private void tryCallUpdateDoubleScore(GameManager gm) {
        try {
            Method m = GameManager.class.getMethod("updateDoubleScore");
            m.invoke(gm);
        } catch (Exception ignore) {
            // 없으면 자연 만료(시간 비교 로직이 isDoubleScoreActive 내부에 있을 수도 있음)
        }
    }
}
