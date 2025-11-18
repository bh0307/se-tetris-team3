package se.tetris.team3.uiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.store.SettingsStore;

import java.awt.event.KeyEvent;

/**
 * 키 매핑 설정 및 저장/로드 테스트
 * - 키 설정 화면에서 지정한 키가 Settings에 올바르게 저장되는지 검증
 * - 저장된 키 설정이 파일로 저장되고 로드되는지 검증
 */
public class KeymapTest {

    private Settings settings;

    @BeforeEach
    public void setUp() {
        settings = new Settings();
        settings.resetDefaults(); // 기본값으로 초기화
    }

    @Test
    public void testDefaultKeymapSettings() {
        // 기본 키 매핑 검증
        assertEquals(KeyEvent.VK_LEFT, settings.getKeymap().get(Action.MOVE_LEFT),
            "기본 좌측 이동 키는 LEFT여야 함");
        assertEquals(KeyEvent.VK_RIGHT, settings.getKeymap().get(Action.MOVE_RIGHT),
            "기본 우측 이동 키는 RIGHT여야 함");
        assertEquals(KeyEvent.VK_UP, settings.getKeymap().get(Action.ROTATE),
            "기본 회전 키는 UP이어야 함");
        assertEquals(KeyEvent.VK_DOWN, settings.getKeymap().get(Action.SOFT_DROP),
            "기본 소프트 드롭 키는 DOWN이어야 함");
        assertEquals(KeyEvent.VK_P, settings.getKeymap().get(Action.PAUSE),
            "기본 일시정지 키는 P여야 함");
        assertEquals(KeyEvent.VK_ESCAPE, settings.getKeymap().get(Action.EXIT),
            "기본 종료 키는 ESCAPE여야 함");
    }

    @Test
    public void testCustomKeymapSettings() {
        // 커스텀 키 설정 - 모든 액션에 대해
        settings.getKeymap().put(Action.MOVE_LEFT, KeyEvent.VK_A);
        settings.getKeymap().put(Action.MOVE_RIGHT, KeyEvent.VK_D);
        settings.getKeymap().put(Action.ROTATE, KeyEvent.VK_W);
        settings.getKeymap().put(Action.SOFT_DROP, KeyEvent.VK_S);
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_SPACE);
        settings.getKeymap().put(Action.EXIT, KeyEvent.VK_Q);

        // 변경된 키 매핑 검증 - 모든 키
        assertEquals(KeyEvent.VK_A, settings.getKeymap().get(Action.MOVE_LEFT),
            "좌측 이동 키를 A로 변경했을 때 올바르게 저장되어야 함");
        assertEquals(KeyEvent.VK_D, settings.getKeymap().get(Action.MOVE_RIGHT),
            "우측 이동 키를 D로 변경했을 때 올바르게 저장되어야 함");
        assertEquals(KeyEvent.VK_W, settings.getKeymap().get(Action.ROTATE),
            "회전 키를 W로 변경했을 때 올바르게 저장되어야 함");
        assertEquals(KeyEvent.VK_S, settings.getKeymap().get(Action.SOFT_DROP),
            "소프트 드롭 키를 S로 변경했을 때 올바르게 저장되어야 함");
        assertEquals(KeyEvent.VK_SPACE, settings.getKeymap().get(Action.PAUSE),
            "일시정지 키를 SPACE로 변경했을 때 올바르게 저장되어야 함");
        assertEquals(KeyEvent.VK_Q, settings.getKeymap().get(Action.EXIT),
            "종료 키를 Q로 변경했을 때 올바르게 저장되어야 함");
    }

    @Test
    public void testKeymapPersistence() {
        // 키 설정 변경
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_SPACE);
        settings.getKeymap().put(Action.EXIT, KeyEvent.VK_Q);

        // 설정 저장
        SettingsStore.save(settings);

        // 새로운 Settings 객체로 로드
        Settings loadedSettings = new Settings();
        SettingsStore.load(loadedSettings);

        // 저장된 키 설정이 올바르게 로드되는지 검증
        assertEquals(KeyEvent.VK_SPACE, loadedSettings.getKeymap().get(Action.PAUSE),
            "저장 후 로드 시 PAUSE 키가 유지되어야 함");
        assertEquals(KeyEvent.VK_Q, loadedSettings.getKeymap().get(Action.EXIT),
            "저장 후 로드 시 EXIT 키가 유지되어야 함");
    }

    @Test
    public void testMultipleKeymapChanges() {
        // 여러 번 키 설정 변경
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_SPACE);
        assertEquals(KeyEvent.VK_SPACE, settings.getKeymap().get(Action.PAUSE));

        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_ENTER);
        assertEquals(KeyEvent.VK_ENTER, settings.getKeymap().get(Action.PAUSE),
            "키를 여러 번 변경해도 마지막 값이 유지되어야 함");
    }

    @Test
    public void testAllActionsHaveKeys() {
        // 모든 액션에 키가 매핑되어 있는지 검증
        for (Action action : Action.values()) {
            assertNotNull(settings.getKeymap().get(action),
                action.name() + " 액션에는 키가 매핑되어 있어야 함");
        }
    }

    @Test
    public void testResetToDefaults() {
        // 커스텀 설정 적용
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_SPACE);
        settings.getKeymap().put(Action.EXIT, KeyEvent.VK_Q);

        // 기본값으로 리셋
        settings.resetDefaults();

        // 기본 키로 복원되었는지 검증
        assertEquals(KeyEvent.VK_P, settings.getKeymap().get(Action.PAUSE),
            "리셋 후 PAUSE 키는 기본값 P로 돌아가야 함");
        assertEquals(KeyEvent.VK_ESCAPE, settings.getKeymap().get(Action.EXIT),
            "리셋 후 EXIT 키는 기본값 ESCAPE로 돌아가야 함");
    }
}
