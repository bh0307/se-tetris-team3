package se.tetris.team3.uiTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.SettingsScreen;
import se.tetris.team3.core.Settings;
import java.awt.event.KeyEvent;

@DisplayName("SettingsScreen 주요 동작 테스트")
class SettingsScreenTest {
    AppFrame app;
    Settings settings;
    SettingsScreen screen;

    @BeforeEach
    void setup() {
        app = new AppFrame(); // 실제 객체로 생성
        settings = app.getSettings(); // 반드시 app의 settings를 사용
        screen = new SettingsScreen(app);
    }

    @Test
    @DisplayName("UP 키 입력 시 index가 위로 이동하는지 검증")
    void testUpKeyMovesIndexUp() {
        int before = getIndex(screen);
        KeyEvent up = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_UP, ' ');
        screen.onKeyPressed(up);
        int after = getIndex(screen);
        assertEquals((before + 6) % 7, after); // 7개 메뉴, 위로 이동
    }

    @Test
    @DisplayName("DOWN 키 입력 시 index가 아래로 이동하는지 검증")
    void testDownKeyMovesIndexDown() {
        int before = getIndex(screen);
        KeyEvent down = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_DOWN, ' ');
        screen.onKeyPressed(down);
        int after = getIndex(screen);
        assertEquals((before + 1) % 7, after); // 7개 메뉴, 아래로 이동
    }

    @Test
    @DisplayName("프리셋 메뉴 선택 시 SizePreset 값이 변경되는지 검증")
    void testPresetMenuChangesSizePreset() {
        setIndex(screen, 0);
        settings.setSizePreset(Settings.SizePreset.SMALL);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        assertEquals(Settings.SizePreset.MEDIUM, settings.getSizePreset());
    }

    @Test
    @DisplayName("난이도 메뉴 선택 시 Difficulty 값이 변경되는지 검증")
    void testDifficultyMenuChangesDifficulty() {
        setIndex(screen, 1);
        settings.setDifficulty(Settings.Difficulty.EASY);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        assertEquals(Settings.Difficulty.NORMAL, settings.getDifficulty());
    }

    @Test
    @DisplayName("색맹 모드 메뉴 선택 시 isColorBlindMode 값이 변경되는지 검증")
    void testColorBlindMenuChangesMode() {
        setIndex(screen, 3);
        settings.setColorBlindMode(false);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        assertTrue(settings.isColorBlindMode());
    }

    @Test
    @DisplayName("기본값 복원 메뉴 선택 시 settings.resetDefaults가 실제로 동작하는지 검증")
    void testResetDefaultsMenuWorks() {
        setIndex(screen, 5);
        settings.setSizePreset(Settings.SizePreset.LARGE);
        settings.setDifficulty(Settings.Difficulty.HARD);
        settings.setColorBlindMode(true);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        assertEquals(Settings.SizePreset.MEDIUM, settings.getSizePreset());
        assertEquals(Settings.Difficulty.NORMAL, settings.getDifficulty());
        assertFalse(settings.isColorBlindMode());
    }

    @Test
    @DisplayName("뒤로가기 메뉴 선택 시 예외 없이 동작하는지 검증")
    void testBackMenuWorks() {
        setIndex(screen, 6);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        assertDoesNotThrow(() -> screen.onKeyPressed(enter));
    }

    @Test
    @DisplayName("키 설정 메뉴 선택 시 예외 없이 동작하는지 검증")
    void testKeymapMenuWorks() {
        setIndex(screen, 2);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        assertDoesNotThrow(() -> screen.onKeyPressed(enter));
    }

    @Test
    @DisplayName("스코어 초기화 메뉴 선택 시 예외 없이 동작하는지 검증")
    void testScoreResetMenuWorks() {
        setIndex(screen, 4);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        assertDoesNotThrow(() -> screen.onKeyPressed(enter));
    }

    @Test
    @DisplayName("ESC 키 입력 시 예외 없이 동작하는지 검증")
    void testEscKeyWorks() {
        KeyEvent esc = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ESCAPE, ' ');
        assertDoesNotThrow(() -> screen.onKeyPressed(esc));
    }

    // index 필드 접근용 헬퍼
    private int getIndex(SettingsScreen s) {
        try {
            java.lang.reflect.Field f = SettingsScreen.class.getDeclaredField("index");
            f.setAccessible(true);
            return f.getInt(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // index 필드 설정용 헬퍼
    private void setIndex(SettingsScreen s, int value) {
        try {
            java.lang.reflect.Field f = SettingsScreen.class.getDeclaredField("index");
            f.setAccessible(true);
            f.setInt(s, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
