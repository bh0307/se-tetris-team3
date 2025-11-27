package se.tetris.team3.scoreTest;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.score.ScoreManager;
import se.tetris.team3.ui.score.ScoreManager.ScoreEntry;
import se.tetris.team3.ui.score.ScoreboardScreen;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardScreenTest {
    private AppFrame app;
    private ScoreManager scoreManager;
    private Settings settings;
    private Graphics2D g2;
    private ScoreboardScreen screen;

    @BeforeEach
    void setUp() {
        app = Mockito.mock(AppFrame.class);
        settings = Mockito.mock(Settings.class);
        Mockito.when(app.getSettings()).thenReturn(settings);
        Mockito.when(app.getWidth()).thenReturn(800);
        Mockito.when(app.getHeight()).thenReturn(600);
        Mockito.when(settings.resolveBlockSize()).thenReturn(30);
        Mockito.when(settings.getGameMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(settings.getDifficulty()).thenReturn(Settings.Difficulty.NORMAL);
        scoreManager = Mockito.mock(ScoreManager.class);
        g2 = Mockito.mock(Graphics2D.class);
        Mockito.when(g2.getFontMetrics()).thenReturn(Mockito.mock(FontMetrics.class));
        Mockito.when(g2.getFontMetrics().stringWidth(Mockito.anyString())).thenReturn(50);
        screen = new ScoreboardScreen(app, "Tester", 1000, scoreManager);
    }

    @Test
    @DisplayName("playerName이 null이고 playerScore가 -1일 때 객체가 정상적으로 생성되는지 검증")
    void testConstructorPlayerNameNullScoreMinusOne() {
        ScoreboardScreen s = new ScoreboardScreen(app, -1, scoreManager);
        assertNotNull(s);
    }

    @Test
    @DisplayName("playerName과 playerScore가 정상 값일 때 객체가 정상적으로 생성되는지 검증")
    void testConstructorPlayerNameScore() {
        ScoreboardScreen s = new ScoreboardScreen(app, "Tester", 1000, scoreManager);
        assertNotNull(s);
    }

    @Test
    @DisplayName("playerScore가 -1이면 점수 메시지가 표시되지 않고 예외 없이 렌더링되는지 검증")
    void testRenderPlayerScoreMinusOne() {
        ScoreboardScreen s = new ScoreboardScreen(app, -1, scoreManager);
        Mockito.when(scoreManager.getHighScores(Mockito.any())).thenReturn(Collections.emptyList());
        s.render(g2);
    }

    @Test
    @DisplayName("playerScore가 0 또는 양수일 때 점수 메시지가 정상적으로 표시되고 예외 없이 렌더링되는지 검증")
    void testRenderPlayerScoreZeroAndPositive() {
        ScoreboardScreen s1 = new ScoreboardScreen(app, "Tester", 0, scoreManager);
        Mockito.when(scoreManager.getHighScores(Mockito.any())).thenReturn(Collections.emptyList());
        s1.render(g2);

        ScoreboardScreen s2 = new ScoreboardScreen(app, "Tester", 1000, scoreManager);
        Mockito.when(scoreManager.getHighScores(Mockito.any())).thenReturn(Collections.emptyList());
        s2.render(g2);
    }

    @Test
    @DisplayName("ScoreEntry가 10개일 때 모든 랭킹 정보가 정상적으로 렌더링되는지 검증")
    void testRenderScoreTableVariousEntries() {
        List<ScoreEntry> entries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ScoreEntry entry = Mockito.mock(ScoreEntry.class);
            Mockito.when(entry.getPlayerName()).thenReturn(i == 0 ? "Tester" : "Player"+i);
            Mockito.when(entry.getScore()).thenReturn(i == 0 ? 1000 : i*100);
            Mockito.when(entry.getDifficulty()).thenReturn(i%3==0 ? Settings.Difficulty.EASY : (i%3==1 ? Settings.Difficulty.HARD : Settings.Difficulty.NORMAL));
            Mockito.when(entry.getFormattedDate()).thenReturn("2025-11-17");
            entries.add(entry);
        }
        Mockito.when(scoreManager.getHighScores(Mockito.any())).thenReturn(entries);
        screen.render(g2);
    }

    @Test
    @DisplayName("drawStringEllipsis가 긴 문자열, 짧은 문자열, null 입력에 대해 예외 없이 처리되는지 검증")
    void testDrawStringEllipsis() throws Exception {
        Method method = ScoreboardScreen.class.getDeclaredMethod("drawStringEllipsis", Graphics2D.class, String.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(screen, g2, "Short", 0, 0, 100);
        method.invoke(screen, g2, "VeryLongNameThatExceeds", 0, 0, 10);
        method.invoke(screen, g2, null, 0, 0, 10);
    }

    @Test
    @DisplayName("현재 플레이어의 점수가 새로 추가된 점수로 인식되는지/아닌지 검증")
    void testIsNewlyAddedScore() throws Exception {
        ScoreEntry entry = Mockito.mock(ScoreEntry.class);
        Mockito.when(entry.getPlayerName()).thenReturn("Tester");
        Mockito.when(entry.getScore()).thenReturn(1000);
        Method method = ScoreboardScreen.class.getDeclaredMethod("isNewlyAddedScore", ScoreEntry.class);
        method.setAccessible(true);
        assertTrue((Boolean)method.invoke(screen, entry));

        Mockito.when(entry.getPlayerName()).thenReturn("Other");
        assertFalse((Boolean)method.invoke(screen, entry));
    }

    @Test
    @DisplayName("DOWN 키 입력 시 메뉴 인덱스가 증가해야 한다 / UP 키 입력 시 감소해야 한다 / TAB 키 입력 시 게임 모드가 전환되어야 한다 / ENTER, SPACE 입력 시 해당 메뉴의 액션이 실행되어야 한다")
    void testOnKeyPressedStateChange() {
        // 메뉴 인덱스 변화 (UP/DOWN)
        int oldIdx = getIdx(screen);
        KeyEvent down = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, ' ');
        screen.onKeyPressed(down);
        assertEquals((oldIdx + 1) % 2, getIdx(screen), "DOWN 키 입력 시 메뉴 인덱스 증가");

        KeyEvent up = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, ' ');
        screen.onKeyPressed(up);
        assertEquals(oldIdx, getIdx(screen), "UP 키 입력 시 메뉴 인덱스 감소");

        // TAB 입력 시 모드 전환
        GameMode beforeMode = getCurrentMode(screen);
        KeyEvent tab = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, ' ');
        screen.onKeyPressed(tab);
        assertNotEquals(beforeMode, getCurrentMode(screen), "TAB 키 입력 시 모드 전환");

        // ENTER/SPACE 입력 시 액션 실행 여부 (메뉴 액션이 호출되는지)
        // MenuItem의 action을 mock으로 대체하여 호출 여부 검증
        // idx=0: 메인 메뉴, idx=1: 종료
        Runnable mainMenuAction = Mockito.mock(Runnable.class);
        Runnable exitAction = Mockito.mock(Runnable.class);
        // MenuItem 교체
        setMenuItemAction(screen, 0, mainMenuAction);
        setMenuItemAction(screen, 1, exitAction);

        // idx=0에서 ENTER
        setIdx(screen, 0);
        KeyEvent enter = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, ' ');
        screen.onKeyPressed(enter);
        Mockito.verify(mainMenuAction, Mockito.times(1)).run();

        // idx=1에서 SPACE
        setIdx(screen, 1);
        KeyEvent space = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' ');
        screen.onKeyPressed(space);
        Mockito.verify(exitAction, Mockito.times(1)).run();
    }

    @Test
    @DisplayName("onShow, onHide 호출 시 예외 없이 정상적으로 동작해야 한다")
    void testOnShowOnHide() {
        // 화면 상태 변화가 있다면 검증, 현재는 예외 없이 호출되는지만 확인
        assertDoesNotThrow(() -> screen.onShow());
        assertDoesNotThrow(() -> screen.onHide());
    }
    // MenuItem action 교체용 유틸리티
    private void setMenuItemAction(ScoreboardScreen s, int idx, Runnable action) {
        try {
            Field f = ScoreboardScreen.class.getDeclaredField("items");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) f.get(s);
            Object menuItem = items.get(idx);
            Field af = menuItem.getClass().getDeclaredField("action");
            af.setAccessible(true);
            af.set(menuItem, action);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private void setIdx(ScoreboardScreen s, int idx) {
        try {
            Field f = ScoreboardScreen.class.getDeclaredField("idx");
            f.setAccessible(true);
            f.setInt(s, idx);
        } catch (Exception e) { }
    }

    @Test
    @DisplayName("renderMenu가 선택 인덱스에 따라 메뉴 하이라이트를 정상적으로 처리하는지 검증")
    void testRenderMenuIndex() throws Exception {
        Method method = ScoreboardScreen.class.getDeclaredMethod("renderMenu", Graphics2D.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(screen, g2, 800, 600);
    }

    @Test
    @DisplayName("DOWN 키 입력 시 메뉴 인덱스가 실제로 변경되는지 검증")
    void testMenuNavigation() {
        int oldIdx = getIdx(screen);
        KeyEvent down = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, ' ');
        screen.onKeyPressed(down);
        assertNotEquals(oldIdx, getIdx(screen));
    }

    @Test
    @DisplayName("TAB 키 입력 시 게임 모드가 실제로 전환되는지 검증")
    void testModeSwitch() {
        GameMode before = getCurrentMode(screen);
        KeyEvent tab = new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_TAB, '\t');
        screen.onKeyPressed(tab);
        assertNotEquals(before, getCurrentMode(screen));
    }

    // private 필드 접근용 메서드
    private int getIdx(ScoreboardScreen s) {
        try {
            Field f = ScoreboardScreen.class.getDeclaredField("idx");
            f.setAccessible(true);
            return f.getInt(s);
        } catch (Exception e) { return -1; }
    }
    private GameMode getCurrentMode(ScoreboardScreen s) {
        try {
            Field f = ScoreboardScreen.class.getDeclaredField("currentMode");
            f.setAccessible(true);
            return (GameMode) f.get(s);
        } catch (Exception e) { return null; }
    }
}
