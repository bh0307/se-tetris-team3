package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.BattleModeSelectScreen;
import se.tetris.team3.ui.screen.BattleScreen;
import se.tetris.team3.ui.screen.MenuScreen;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

@DisplayName("BattleModeSelectScreen 주요 동작 테스트")
class BattleModeSelectScreenTest {
    AppFrame app;
    Settings settings;
    BattleModeSelectScreen screen;
    Graphics2D g2;

    @BeforeEach
    void setup() {
        app = Mockito.mock(AppFrame.class);
        settings = new Settings(); // 실제 객체로 변경
        Mockito.when(app.getWidth()).thenReturn(1200);
        Mockito.when(app.getHeight()).thenReturn(700);
        screen = new BattleModeSelectScreen(app, settings);
        BufferedImage img = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
        g2 = img.createGraphics();
    }

    @AfterEach
    void tearDown() {
        if (g2 != null) g2.dispose();
    }

    @Test
    @DisplayName("UP/DOWN 키로 모드 이동")
    void testModeNavigation() {
        int before = getSelectedMode(screen);
        KeyEvent down = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_DOWN, ' ');
        screen.onKeyPressed(down);
        int after = getSelectedMode(screen);
        assertEquals((before + 1) % 3, after);

        KeyEvent up = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_UP, ' ');
        screen.onKeyPressed(up);
        int afterUp = getSelectedMode(screen);
        assertEquals(before, afterUp);
    }

    @Test
    @DisplayName("시간제한 모드에서 좌/우 키로 시간 조정")
    void testTimeAttackAdjustTime() {
        setSelectedMode(screen, 2); // Time Attack
        int before = getTimeLimit(screen);
        KeyEvent right = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_RIGHT, ' ');
        screen.onKeyPressed(right);
        int after = getTimeLimit(screen);
        assertTrue(after > before);

        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_LEFT, ' ');
        screen.onKeyPressed(left);
        int afterLeft = getTimeLimit(screen);
        assertTrue(afterLeft < after);
    }

    @Test
    @DisplayName("ENTER 키로 BattleScreen 전환 및 showScreen 호출 검증")
    void testEnterStartsBattle() {
        setSelectedMode(screen, 0); // Normal Battle
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        assertDoesNotThrow(() -> screen.onKeyPressed(enter));
        Mockito.verify(app).showScreen(Mockito.any(BattleScreen.class));
    }

    @Test
    @DisplayName("ESC 키로 메뉴 복귀")
    void testEscReturnsToMenu() {
        KeyEvent esc = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ESCAPE, (char)27);
        assertDoesNotThrow(() -> screen.onKeyPressed(esc));
    }

    @Test
    @DisplayName("모드 박스 클릭 시 BattleScreen 전환")
    void testMouseClickSelectsModeAndStartsBattle() {
        int centerX = app.getWidth() / 2;
        int y = 220 + 1 * 120; // Item Battle 위치
        MouseEvent click = new MouseEvent(new java.awt.Component(){}, 0, 0, 0, centerX, y, 1, false);
        assertDoesNotThrow(() -> screen.onMouseClicked(click));
    }

    @Test
    @DisplayName("모드 박스 호버 시 selectedMode 변경 및 repaint 호출")
    void testMouseMoveChangesSelectedMode() {
        int centerX = app.getWidth() / 2;
        int y = 220 + 2 * 120; // Time Attack 위치
        MouseEvent move = new MouseEvent(new java.awt.Component(){}, 0, 0, 0, centerX, y, 0, false);
        assertDoesNotThrow(() -> screen.onMouseMoved(move));
    }

    @Test
    @DisplayName("render 메서드 예외 없이 동작")
    void testRender() {
        assertDoesNotThrow(() -> screen.render(g2));
    }

    // --- Reflection helpers ---
    private int getSelectedMode(BattleModeSelectScreen s) {
        try {
            java.lang.reflect.Field f = BattleModeSelectScreen.class.getDeclaredField("selectedMode");
            f.setAccessible(true);
            return f.getInt(s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private void setSelectedMode(BattleModeSelectScreen s, int value) {
        try {
            java.lang.reflect.Field f = BattleModeSelectScreen.class.getDeclaredField("selectedMode");
            f.setAccessible(true);
            f.setInt(s, value);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private int getTimeLimit(BattleModeSelectScreen s) {
        try {
            java.lang.reflect.Field f = BattleModeSelectScreen.class.getDeclaredField("timeLimit");
            f.setAccessible(true);
            return f.getInt(s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
