package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import se.tetris.team3.ui.*;
import se.tetris.team3.core.*;
import se.tetris.team3.gameManager.ScoreManager;
import se.tetris.team3.ui.screen.NameInputScreen;
import se.tetris.team3.ui.screen.ScoreboardScreen;

import java.awt.event.KeyEvent;

@DisplayName("NameInputScreen 주요 동작 테스트")
class NameInputScreenTest {
    AppFrame app;
    NameInputScreen screen;
    int score = 1234;

    @BeforeEach
    void setup() {
        app = new AppFrame();
        screen = new NameInputScreen(app, GameMode.CLASSIC, score);
        clearScores();
    }

    @Test
    @DisplayName("이름 입력 후 ENTER로 저장 및 화면 전환")
    void testNameEntryAndEnter() {
        typeName("TestUser");
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        // ScoreManager에 저장됐는지 확인
        assertTrue(scoreExists("TestUser", score));
        // 화면 전환 확인
        assertTrue(appScreenIsScoreboard());
    }

    @Test
    @DisplayName("ESC로 이름 스킵 시 Anonymous로 저장 및 화면 전환")
    void testEscStoresAnonymous() {
        KeyEvent esc = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ESCAPE, (char)27);
        screen.onKeyPressed(esc);
        assertTrue(scoreExists("Anonymous", score));
        assertTrue(appScreenIsScoreboard());
    }

    @Test
    @DisplayName("빈 이름에서 ENTER 시 Anonymous로 저장")
    void testEmptyNameEnterStoresAnonymous() {
        KeyEvent enter = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_ENTER, '\n');
        screen.onKeyPressed(enter);
        assertTrue(scoreExists("Anonymous", score));
    }
    // 점수 저장 여부 확인 (파일 기반)
    private boolean scoreExists(String name, int score) {
        try {
            java.lang.reflect.Field f = NameInputScreen.class.getDeclaredField("scoreManager");
            f.setAccessible(true);
            ScoreManager manager = (ScoreManager) f.get(null);
            for (ScoreManager.ScoreEntry entry : manager.getHighScores(GameMode.CLASSIC)) {
                if (entry.getPlayerName().equals(name) && entry.getScore() == score) return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 점수 파일 초기화
    private void clearScores() {
        try {
            java.lang.reflect.Field f = NameInputScreen.class.getDeclaredField("scoreManager");
            f.setAccessible(true);
            ScoreManager manager = (ScoreManager) f.get(null);
            manager.clearScores();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("백스페이스로 이름 삭제 동작")
    void testBackspaceDeletesChar() {
        typeName("Test");
        KeyEvent back = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_BACK_SPACE, '\b');
        screen.onKeyPressed(back);
        assertEquals("Tes", getPlayerName());
    }

    @Test
    @DisplayName("최대 길이 초과 입력 무시")
    void testMaxLengthLimit() {
        typeName("ABCDEFGHIJKL"); // 12자 입력 시도
        assertEquals(10, getPlayerName().length()); // 10자까지만 허용
    }

    @Test
    @DisplayName("유효하지 않은 문자 입력 무시")
    void testInvalidCharIgnored() {
        typeName("Test$"); // $는 허용되지 않음
        assertEquals("Test", getPlayerName());
    }

    // 이름 입력 헬퍼
    private void typeName(String name) {
        for (char c : name.toCharArray()) {
            KeyEvent e = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_UNDEFINED, c);
            screen.onKeyPressed(e);
        }
    }

    // playerName 필드 접근
    private String getPlayerName() {
        try {
            java.lang.reflect.Field f = NameInputScreen.class.getDeclaredField("playerName");
            f.setAccessible(true);
            return ((StringBuilder)f.get(screen)).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ScoreboardScreen으로 전환됐는지 확인
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
}
