package se.tetris.team3.uiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.GameManager;
import se.tetris.team3.ui.GameScreen;

import static org.junit.jupiter.api.Assertions.*;
import javax.swing.Timer;

public class TimerTest {
    private GameScreen gameScreen;
    private AppFrame app;

    @BeforeEach
    void setUp() {
        app = new AppFrame();
        GameManager manager = new GameManager();
        gameScreen = new GameScreen(app, manager);
    }

    @Test
    public void testTimerStartsOnShow() throws Exception {
        gameScreen.onShow();
        java.lang.reflect.Field f = GameScreen.class.getDeclaredField("timer");
        f.setAccessible(true);
        Timer timer = (Timer) f.get(gameScreen);
        assertNotNull(timer, "Timer should be initialized on onShow()");
        assertTrue(timer.isRunning(), "Timer should be running after onShow()");
    }

    @Test
    public void testTimerStopsOnHide() throws Exception {
        gameScreen.onShow();
        java.lang.reflect.Field f = GameScreen.class.getDeclaredField("timer");
        f.setAccessible(true);
        Timer timer = (Timer) f.get(gameScreen);
        assertTrue(timer.isRunning(), "Timer should be running after onShow()");
        gameScreen.onHide();
        assertFalse(timer.isRunning(), "Timer should stop after onHide()");
    }

    // 일시정지/재개 테스트는 키 이벤트를 통한 상태 변화가 필요하므로 별도 구현 필요
        @Test
        public void testTimerPauseAndResumeWithKey() throws Exception {
        gameScreen.onShow();
        java.lang.reflect.Field f = GameScreen.class.getDeclaredField("timer");
        f.setAccessible(true);
        Timer timer = (Timer) f.get(gameScreen);
        assertTrue(timer.isRunning(), "Timer should be running after onShow()");

        // PAUSE 키 이벤트 생성
        int pauseKeyCode = app.getSettings().getKeymap().get(se.tetris.team3.core.Settings.Action.PAUSE);
        java.awt.event.KeyEvent pauseEvent = new java.awt.event.KeyEvent(app, java.awt.event.KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, pauseKeyCode, java.awt.event.KeyEvent.CHAR_UNDEFINED);
        gameScreen.onKeyPressed(pauseEvent);
        timer = (Timer) f.get(gameScreen);
        assertFalse(timer.isRunning(), "Timer should stop after PAUSE key pressed");

        // 다시 PAUSE 키 입력 (재개)
        gameScreen.onKeyPressed(pauseEvent);
        timer = (Timer) f.get(gameScreen);
        assertTrue(timer.isRunning(), "Timer should resume after PAUSE key pressed again");
        }
}
