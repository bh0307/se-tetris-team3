package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import se.tetris.team3.core.Settings;
import se.tetris.team3.core.Settings.Action;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.KeymapScreen;
import se.tetris.team3.ui.screen.SettingsScreen;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

@DisplayName("KeymapScreen UI 및 입력 동작 테스트")
class KeymapScreenTest {
    AppFrame app;
    Settings settings;
    KeymapScreen screen;
    Graphics2D g2;

    @BeforeEach
    void setup() {
        app = Mockito.mock(AppFrame.class);
        settings = new Settings();
        Mockito.when(app.getSettings()).thenReturn(settings);
        Mockito.when(app.getWidth()).thenReturn(800);
        Mockito.when(app.getHeight()).thenReturn(600);
        screen = new KeymapScreen(app);
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = img.createGraphics();
    }

    @AfterEach
    void tearDown() {
        if (g2 != null) g2.dispose();
    }

    @Test
    @DisplayName("render 메서드 예외 없이 동작")
    void testRender() {
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("UP/DOWN 키로 커서 이동")
    void testCursorNavigation() {
        int before = getCursor(screen);
        KeyEvent down = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_DOWN, ' ');
        screen.onKeyPressed(down);
        int after = getCursor(screen);
        assertEquals((before + 1) % 7, after);

        KeyEvent up = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_UP, ' ');
        screen.onKeyPressed(up);
        int afterUp = getCursor(screen);
        assertEquals(before, afterUp);
    }

    @Test
    @DisplayName("ENTER 키로 입력 대기 진입 및 ESC로 취소")
    void testEnterAndEscCancelInput() {
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        assertTrue(getWaitingInput(screen));
        KeyEvent esc = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ESCAPE, (char)27);
        screen.onKeyPressed(esc);
        assertFalse(getWaitingInput(screen));
    }

    @Test
    @DisplayName("이미 사용 중인 키 입력 시 상태 메시지 변경")
    void testDuplicateKeyInputShowsWarning() {
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        // 현재 커서가 MOVE_LEFT, 기본값 LEFT
        KeyEvent duplicate = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_RIGHT, ' ');
        screen.onKeyPressed(duplicate);
        String status = getStatus(screen);
        assertTrue(status.contains("이미 사용 중"));
        assertTrue(getWaitingInput(screen)); // 입력 대기 유지
    }

    @Test
    @DisplayName("새 키 입력 시 키맵 변경 및 저장 메시지")
    void testChangeKeyAndSave() {
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        KeyEvent newKey = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_A, 'A');
        screen.onKeyPressed(newKey);
        String status = getStatus(screen);
        assertTrue(status.contains("저장됨"));
        assertFalse(getWaitingInput(screen));
        // 실제로 키맵이 변경됐는지 확인
        assertEquals(KeyEvent.VK_A, settings.getKeymap().get(Action.MOVE_LEFT));
    }

    @Test
    @DisplayName("ESC 키로 SettingsScreen으로 복귀")
    void testEscReturnsToSettingsScreen() {
        KeyEvent esc = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ESCAPE, (char)27);
        screen.onKeyPressed(esc);
        Mockito.verify(app).showScreen(Mockito.any(SettingsScreen.class));
    }

    @Test
    @DisplayName("isKeyInUse: 중복 키 사용 여부 판정")
    void testIsKeyInUse() throws Exception {
        java.lang.reflect.Method m = KeymapScreen.class.getDeclaredMethod("isKeyInUse", int.class, Action.class);
        m.setAccessible(true);
        // 기본값: LEFT는 MOVE_LEFT에만 사용, except가 MOVE_RIGHT면 true
        boolean used = (boolean) m.invoke(screen, KeyEvent.VK_LEFT, Action.MOVE_RIGHT);
        assertTrue(used);
        // RIGHT는 MOVE_RIGHT에 사용, MOVE_LEFT 제외하면 true
        used = (boolean) m.invoke(screen, KeyEvent.VK_RIGHT, Action.MOVE_LEFT);
        assertTrue(used);
    }

    // --- Reflection helpers ---
    private int getCursor(KeymapScreen s) {
        try {
            java.lang.reflect.Field f = KeymapScreen.class.getDeclaredField("cursor");
            f.setAccessible(true);
            return f.getInt(s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private boolean getWaitingInput(KeymapScreen s) {
        try {
            java.lang.reflect.Field f = KeymapScreen.class.getDeclaredField("waitingInput");
            f.setAccessible(true);
            return f.getBoolean(s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private String getStatus(KeymapScreen s) {
        try {
            java.lang.reflect.Field f = KeymapScreen.class.getDeclaredField("status");
            f.setAccessible(true);
            return (String) f.get(s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
