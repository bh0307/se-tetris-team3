package se.tetris.team3.itemTest;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.ui.GameManager;
import se.tetris.team3.blocks.IBlock;

/**
 * I-only 아이템 테스트
 * - I-only 모드가 활성화된 동안에는 makeRandomBlock()가 항상 IBlock을 반환하는지 검증
 */
public class IOnlyItemTest {

    private GameManager manager;

    @BeforeEach
    public void setUp() {
        manager = new GameManager(GameMode.ITEM);
    }

    @Test
    public void testIOnlyModeProducesOnlyIBlocks() throws Exception {
        // I-only 모드 2초 활성화 (public/private 구분 없이 리플렉션으로 호출)
        callActivateIOnlyMode(manager, 2000);

        // protected 메서드(makeRandomBlock)를 리플렉션으로 호출
        java.lang.reflect.Method m = GameManager.class.getDeclaredMethod("makeRandomBlock");
        m.setAccessible(true);

        // 여러 번 호출해도 모두 IBlock 인스턴스여야 함
        for (int i = 0; i < 30; i++) {
            Object b = m.invoke(manager);
            assertNotNull(b, "반환된 블록은 null이면 안 됩니다 (iter=" + i + ")");
            assertTrue(b instanceof IBlock, "I-only 모드 동안에는 항상 IBlock이 생성되어야 합니다 (iter=" + i + ", got=" + b.getClass().getSimpleName() + ")");
        }
    }

    // 리플렉션 유틸: activateIOnlyMode가 public이면 직접 호출, 아니면 private 리플렉션으로 호출
    private void callActivateIOnlyMode(GameManager gm, int ms) throws Exception {
        try {
            // public 메서드 시도
            GameManager.class.getMethod("activateIOnlyMode", int.class).invoke(gm, ms);
        } catch (NoSuchMethodException e) {
            Method m = GameManager.class.getDeclaredMethod("activateIOnlyMode", int.class);
            m.setAccessible(true);
            m.invoke(gm, ms);
        }
    }
}
