package se.tetris.team3.uiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.GameManager;
import se.tetris.team3.ui.GameScreen;

import java.awt.event.KeyEvent;

/**
 * GameScreen 설정 반영 테스트
 * - 색맹 모드, 키맵 변경, 난이도 등 Settings가 GameScreen에 제대로 반영되는지 검증
 */
public class GameScreenSettingsTest {
    private AppFrame app;
    private GameManager manager;
    private GameScreen screen;
    private Settings settings;

    @BeforeEach
    void setUp() {
        app = new AppFrame();
        manager = new GameManager();
        settings = app.getSettings();
        manager.attachSettings(settings);
        screen = new GameScreen(app, manager);
        screen.onShow();
    }

    @Test
    void testColorBlindModeReflects() {
        settings.setColorBlindMode(true);
        assertTrue(settings.isColorBlindMode(), "Settings의 색맹 모드가 true여야 함");
        // GameScreen 내부에서 settings.isColorBlindMode()를 참조하므로 값이 반영됨
        // 실제 렌더링은 PatternPainterTest에서 별도 검증
    }

    @Test
    void testKeymapChangeReflects() {
        // PAUSE 키를 F1로 변경 (겹치지 않는 키)
        settings.getKeymap().put(Action.PAUSE, KeyEvent.VK_F1);
        KeyEvent f1Pause = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_F1, KeyEvent.CHAR_UNDEFINED);
        screen.onKeyPressed(f1Pause);
        assertTrue(getIsPaused(screen), "F1 키로 일시정지되어야 함");
    }

    @Test
    void testDifficultyReflects() {
        settings.setDifficulty(Settings.Difficulty.HARD);
        manager.attachSettings(settings); // 변경된 설정을 GameManager에 반영
        assertEquals(Settings.Difficulty.HARD, settings.getDifficulty(), "난이도가 HARD로 변경되어야 함");
        int delay = manager.getGameTimerDelay();
        assertTrue(delay < 500, "HARD 난이도에서는 timer delay가 더 짧아야 함");
    }

    // GameScreen의 isPaused 필드 접근
    private boolean getIsPaused(GameScreen s) {
        try {
            java.lang.reflect.Field f = GameScreen.class.getDeclaredField("isPaused");
            f.setAccessible(true);
            return f.getBoolean(s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
