package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import se.tetris.team3.ui.*;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.ui.score.ScoreboardScreen;
import java.awt.event.KeyEvent;

@DisplayName("MenuScreen 주요 동작 테스트")
class MenuScreenTest {

        // System.exit를 예외로 변환하는 SecurityManager
        static class NoExitSecurityManager extends SecurityManager {
            @Override
            public void checkPermission(java.security.Permission perm) {}
            @Override
            public void checkExit(int status) { throw new SecurityException("System.exit 호출 감지"); }
        }
    AppFrame app;
    MenuScreen screen;

    @BeforeEach
    void setup() {
        app = new AppFrame();
        screen = new MenuScreen(app);
    }

    @Test
    @DisplayName("UP/DOWN 키로 메뉴 이동")
    void testMenuNavigation() {
        int before = getIndex(screen);
        KeyEvent down = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_DOWN, ' ');
        screen.onKeyPressed(down);
        int after = getIndex(screen);
        assertEquals((before + 1) % 6, after);

        KeyEvent up = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_UP, ' ');
        screen.onKeyPressed(up);
        int afterUp = getIndex(screen);
        assertEquals(before, afterUp);
    }

    @Test
    @DisplayName("클래식 시작 메뉴 동작")
    void testClassicStartMenu() {
        setIndex(screen, 0);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        assertTrue(appScreenIsGameScreen(GameMode.CLASSIC));
    }

    @Test
    @DisplayName("아이템 모드 시작 메뉴 동작")
    void testItemStartMenu() {
        setIndex(screen, 1);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        assertTrue(appScreenIsGameScreen(GameMode.ITEM));
    }

    @Test
    @DisplayName("대전 모드 메뉴 동작")
    void testBattleModeMenu() {
        setIndex(screen, 2);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        assertTrue(appScreenIsBattleModeSelect());
    }

    @Test
    @DisplayName("설정 메뉴 동작")
    void testSettingsMenu() {
        setIndex(screen, 3);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        assertTrue(appScreenIsSettingsScreen());
    }

    @Test
    @DisplayName("스코어보드 메뉴 동작")
    void testScoreboardMenu() {
        setIndex(screen, 4);
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        assertTrue(appScreenIsScoreboard());
    }

    @Test
    @DisplayName("종료 메뉴 동작(System.exit 예외로 처리)")
    void testExitMenuThrowsSecurityException() {
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        try {
            setIndex(screen, 5);
            KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
            assertThrows(SecurityException.class, () -> screen.onKeyPressed(enter));
        } finally {
            System.setSecurityManager(original);
        }
    }

    @Test
    @DisplayName("ESC 키 입력 시 종료(System.exit 예외로 처리)")
    void testEscKeyThrowsSecurityException() {
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());
        try {
            KeyEvent esc = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ESCAPE, (char)27);
            assertThrows(SecurityException.class, () -> screen.onKeyPressed(esc));
        } finally {
            System.setSecurityManager(original);
        }
    }

    @Test
    @DisplayName("잘못된 키 입력 시 힌트 표시 플래그 변경")
    void testInvalidKeyShowsHint() {
        KeyEvent invalid = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_F1, ' ');
        screen.onKeyPressed(invalid);
        assertTrue(getShowHintHighlight(screen));
    }

    // idx 필드 접근
    private int getIndex(MenuScreen s) {
        try {
            java.lang.reflect.Field f = MenuScreen.class.getDeclaredField("idx");
            f.setAccessible(true);
            return f.getInt(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void setIndex(MenuScreen s, int value) {
        try {
            java.lang.reflect.Field f = MenuScreen.class.getDeclaredField("idx");
            f.setAccessible(true);
            f.setInt(s, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // 화면 전환 확인
    private boolean appScreenIsGameScreen(GameMode mode) {
        try {
            java.lang.reflect.Field f = AppFrame.class.getDeclaredField("current");
            f.setAccessible(true);
            Object current = f.get(app);
            if (current instanceof GameScreen) {
                java.lang.reflect.Field gm = GameScreen.class.getDeclaredField("manager");
                gm.setAccessible(true);
                Object manager = gm.get(current);
                java.lang.reflect.Field mmode = manager.getClass().getDeclaredField("mode");
                mmode.setAccessible(true);
                return mmode.get(manager) == mode;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    private boolean appScreenIsBattleModeSelect() {
        try {
            java.lang.reflect.Field f = AppFrame.class.getDeclaredField("current");
            f.setAccessible(true);
            Object current = f.get(app);
            return current instanceof BattleModeSelectScreen;
        } catch (Exception e) {
            return false;
        }
    }
    private boolean appScreenIsSettingsScreen() {
        try {
            java.lang.reflect.Field f = AppFrame.class.getDeclaredField("current");
            f.setAccessible(true);
            Object current = f.get(app);
            return current instanceof SettingsScreen;
        } catch (Exception e) {
            return false;
        }
    }
    private boolean appScreenIsScoreboard() {
        try {
            java.lang.reflect.Field f = AppFrame.class.getDeclaredField("current");
            f.setAccessible(true);
            Object current = f.get(app);
            return current instanceof ScoreboardScreen;
        } catch (Exception e) {
            return false;
        }
    }
    // 힌트 플래그 확인
    private boolean getShowHintHighlight(MenuScreen s) {
        try {
            java.lang.reflect.Field f = MenuScreen.class.getDeclaredField("showHintHighlight");
            f.setAccessible(true);
            return f.getBoolean(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
