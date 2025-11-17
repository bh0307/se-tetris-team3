package se.tetris.team3.uiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.GameScreen;
import se.tetris.team3.ui.MenuScreen;
import se.tetris.team3.ui.Screen;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import javax.swing.Timer;

/**
 * GameScreen 일시정지 기능 테스트
 * - 설정된 PAUSE 키로 일시정지가 작동하는지 검증
 * - 설정된 EXIT 키로 일시정지 중 종료가 작동하는지 검증 (반영된 키 설정 사용)
 */
public class GameScreenPauseTest {

    private GameScreen gameScreen;
    private MockAppFrame mockApp;
    private Settings settings;

    @BeforeEach
    public void setUp() throws Exception {
        settings = new Settings();
        settings.resetDefaults();
        mockApp = new MockAppFrame(settings);
        gameScreen = new GameScreen(mockApp);
        initializeTimer(gameScreen);
    }

    @Test
    public void testInitialPauseState() throws Exception {
        // 초기 상태: 일시정지가 아니어야 함
        assertFalse(isPaused(), "게임 시작 시 일시정지 상태가 아니어야 함");
    }

    @Test
    public void testPauseWithDefaultKey() throws Exception {
        // 기본 P 키로 일시정지 (VK_P)
        KeyEvent pauseEvent = createKeyEvent(KeyEvent.VK_P);
        gameScreen.onKeyPressed(pauseEvent);

        assertTrue(isPaused(), "P 키 입력 후 일시정지 상태여야 함");

        // 다시 P 키로 재개
        gameScreen.onKeyPressed(pauseEvent);
        assertFalse(isPaused(), "P 키 재입력 후 일시정지 해제되어야 함");
    }

    @Test
    public void testPauseWithCustomKey() throws Exception {
        // 커스텀 키로 PAUSE 설정 (Space)
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_SPACE);

        // Space 키로 일시정지
        KeyEvent spaceEvent = createKeyEvent(KeyEvent.VK_SPACE);
        gameScreen.onKeyPressed(spaceEvent);

        assertTrue(isPaused(), "Space 키(커스텀 설정) 입력 후 일시정지 상태여야 함");

        // 기존 P 키로는 일시정지 해제 안 됨
        KeyEvent pEvent = createKeyEvent(KeyEvent.VK_P);
        gameScreen.onKeyPressed(pEvent);
        assertTrue(isPaused(), "P 키는 더 이상 일시정지 키가 아니므로 일시정지 유지되어야 함");

        // Space 키로 재개
        gameScreen.onKeyPressed(spaceEvent);
        assertFalse(isPaused(), "Space 키 재입력 후 일시정지 해제되어야 함");
    }

    @Test
    public void testExitDuringPauseWithDefaultKey() throws Exception {
        // 일시정지 상태로 전환
        KeyEvent pauseEvent = createKeyEvent(KeyEvent.VK_P);
        gameScreen.onKeyPressed(pauseEvent);
        assertTrue(isPaused(), "일시정지 상태여야 함");

        // ESC 키로 종료 (기본 EXIT 키)
        KeyEvent escEvent = createKeyEvent(KeyEvent.VK_ESCAPE);
        gameScreen.onKeyPressed(escEvent);

        // MockAppFrame에서 MenuScreen으로 전환되었는지 확인
        assertTrue(mockApp.hasShownMenuScreen(), "일시정지 중 ESC 키로 메뉴로 돌아가야 함");
    }

    @Test
    public void testExitDuringPauseWithCustomKey() throws Exception {
        // 커스텀 EXIT 키 설정 (Q)
        settings.getKeymap().put(Action.EXIT, KeyEvent.VK_Q);

        // 일시정지 상태로 전환
        KeyEvent pauseEvent = createKeyEvent(KeyEvent.VK_P);
        gameScreen.onKeyPressed(pauseEvent);
        assertTrue(isPaused(), "일시정지 상태여야 함");

        // Q 키로 종료 (커스텀 EXIT 키)
        KeyEvent qEvent = createKeyEvent(KeyEvent.VK_Q);
        gameScreen.onKeyPressed(qEvent);

        assertTrue(mockApp.hasShownMenuScreen(), "일시정지 중 Q 키(커스텀 설정)로 메뉴로 돌아가야 함");
    }

    @Test
    public void testExitNotWorkingWhenNotPaused() throws Exception {
        // 일시정지 상태가 아닐 때
        assertFalse(isPaused(), "일시정지 상태가 아니어야 함");

        // EXIT 키를 눌러도 게임은 계속됨 (일반 플레이 중 EXIT는 메뉴로 감)
        KeyEvent escEvent = createKeyEvent(KeyEvent.VK_ESCAPE);
        gameScreen.onKeyPressed(escEvent);

        // 일반 플레이 중에도 EXIT는 메뉴로 이동
        assertTrue(mockApp.hasShownMenuScreen(), "게임 플레이 중 EXIT 키로 메뉴로 돌아가야 함");
    }

    @Test
    public void testMultipleKeyChanges() throws Exception {
        // PAUSE 키를 여러 번 변경
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_SPACE);
        
        KeyEvent spaceEvent = createKeyEvent(KeyEvent.VK_SPACE);
        gameScreen.onKeyPressed(spaceEvent);
        assertTrue(isPaused(), "Space 키로 일시정지되어야 함");

        // PAUSE 키를 Enter로 변경
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_ENTER);
        gameScreen.onKeyPressed(spaceEvent); // Space는 더 이상 작동 안 함
        assertTrue(isPaused(), "Space 키는 이제 일시정지 키가 아니므로 일시정지 유지");

        // Enter 키로 재개
        KeyEvent enterEvent = createKeyEvent(KeyEvent.VK_ENTER);
        gameScreen.onKeyPressed(enterEvent);
        assertFalse(isPaused(), "Enter 키로 일시정지 해제되어야 함");
    }

    // ===== Helper Methods =====

    /**
     * GameScreen의 timer 필드를 더미 타이머로 초기화
     * onShow()를 호출하지 않고도 updateTimerDelay() NPE를 방지
     */
    private void initializeTimer(GameScreen screen) throws Exception {
        Field timerField = GameScreen.class.getDeclaredField("timer");
        timerField.setAccessible(true);
        // 더미 타이머 (실제로 실행되지 않음, delay 설정만 가능)
        Timer dummyTimer = new Timer(1000, e -> {});
        timerField.set(screen, dummyTimer);
    }

    private boolean isPaused() throws Exception {
        Field pauseField = GameScreen.class.getDeclaredField("isPaused");
        pauseField.setAccessible(true);
        return (boolean) pauseField.get(gameScreen);
    }

    private KeyEvent createKeyEvent(int keyCode) {
        return new KeyEvent(
            mockApp,
            KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            0,
            keyCode,
            KeyEvent.CHAR_UNDEFINED
        );
    }

    // ===== Mock AppFrame =====

    private static class MockAppFrame extends AppFrame {
        private final Settings customSettings;
        private boolean menuScreenShown = false;

        public MockAppFrame(Settings settings) {
            super();
            this.customSettings = settings;
        }

        @Override
        public Settings getSettings() {
            return customSettings;
        }

        @Override
        public void showScreen(Screen screen) {
            if (screen instanceof MenuScreen) {
                menuScreenShown = true;
            }
        }

        public boolean hasShownMenuScreen() {
            return menuScreenShown;
        }

        @Override
        public int getWidth() {
            return 480;
        }

        @Override
        public int getHeight() {
            return 720;
        }
    }
}
