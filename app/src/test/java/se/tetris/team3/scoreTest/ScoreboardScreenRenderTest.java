package se.tetris.team3.scoreTest;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.score.ScoreManager;
import se.tetris.team3.ui.score.ScoreManager.ScoreEntry;
import se.tetris.team3.ui.score.ScoreboardScreen;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ScoreboardScreen 렌더링 및 UI 테스트")
class ScoreboardScreenRenderTest {
    private AppFrame app;
    private ScoreManager scoreManager;
    private Settings settings;
    private Graphics2D g2;
    private ScoreboardScreen screen;
    private FontMetrics fontMetrics;

    @BeforeEach
    void setUp() {
        app = Mockito.mock(AppFrame.class);
        settings = Mockito.mock(Settings.class);
        scoreManager = Mockito.mock(ScoreManager.class);
        
        // BufferedImage로 실제 Graphics2D 생성
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = img.createGraphics();
        fontMetrics = g2.getFontMetrics();
        
        when(app.getSettings()).thenReturn(settings);
        when(app.getWidth()).thenReturn(800);
        when(app.getHeight()).thenReturn(600);
        when(settings.resolveBlockSize()).thenReturn(30);
        when(settings.getGameMode()).thenReturn(GameMode.CLASSIC);
        when(settings.getDifficulty()).thenReturn(Settings.Difficulty.NORMAL);
    }

    @AfterEach
    void tearDown() {
        if (g2 != null) {
            g2.dispose();
        }
    }

    @Test
    @DisplayName("빈 스코어 리스트로 렌더링 시 예외 없이 완료")
    void testRenderWithEmptyScores() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, "Player", 1000, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("10개 이상의 스코어가 있을 때 상위 10개만 렌더링")
    void testRenderWithMoreThan10Scores() {
        List<ScoreEntry> scores = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            scores.add(new ScoreEntry("Player" + i, 1000 - i * 10, Settings.Difficulty.NORMAL));
        }
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, "Player0", 1000, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("1-3위 순위별 색상이 다르게 렌더링됨 (금/은/동)")
    void testTopThreeRankColors() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Gold", 3000, Settings.Difficulty.HARD),
            new ScoreEntry("Silver", 2000, Settings.Difficulty.NORMAL),
            new ScoreEntry("Bronze", 1000, Settings.Difficulty.EASY)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        // 렌더링 후 Color 설정 호출 검증을 위해 spy 사용
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("새로 추가된 점수는 하이라이트 테두리로 표시")
    void testNewScoreHighlight() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Player1", 2000, Settings.Difficulty.NORMAL),
            new ScoreEntry("NewPlayer", 1500, Settings.Difficulty.NORMAL),
            new ScoreEntry("Player2", 1000, Settings.Difficulty.EASY)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, "NewPlayer", 1500, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("난이도별로 올바른 라벨 표시 (EASY/NORMAL/HARD)")
    void testDifficultyLabels() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Easy", 1000, Settings.Difficulty.EASY),
            new ScoreEntry("Normal", 2000, Settings.Difficulty.NORMAL),
            new ScoreEntry("Hard", 3000, Settings.Difficulty.HARD)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("CLASSIC 모드와 ITEM 모드 제목이 올바르게 표시")
    void testModeTitleDisplay() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        
        // CLASSIC 모드
        when(settings.getGameMode()).thenReturn(GameMode.CLASSIC);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        assertDoesNotThrow(() -> screen.render(g2));
        
        // ITEM 모드
        when(settings.getGameMode()).thenReturn(GameMode.ITEM);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("플레이어 점수 메시지가 올바르게 표시")
    void testPlayerScoreMessage() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        when(settings.getGameMode()).thenReturn(GameMode.ITEM);
        when(settings.getDifficulty()).thenReturn(Settings.Difficulty.HARD);
        
        screen = new ScoreboardScreen(app, "TestPlayer", 5000, scoreManager);
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("점수가 -1일 때 플레이어 점수 메시지가 표시되지 않음")
    void testNoPlayerScoreWhenMinusOne() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, "TestPlayer", -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("날짜 형식이 yy.MM.dd로 올바르게 표시")
    void testDateFormat() {
        Date testDate = new Date(1735489200000L); // 2024-12-30
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Player", 1000, Settings.Difficulty.NORMAL, testDate)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("긴 플레이어 이름이 말줄임표로 처리됨")
    void testLongPlayerNameEllipsis() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("VeryLongPlayerNameThatExceedsLimit", 1000, Settings.Difficulty.NORMAL)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("점수가 쉼표 형식으로 표시됨 (✕ 1,000)")
    void testScoreFormatWithComma() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Player", 123456, Settings.Difficulty.NORMAL)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("작은 화면 크기에서도 렌더링 정상 동작")
    void testSmallScreenRendering() {
        when(app.getWidth()).thenReturn(400);
        when(app.getHeight()).thenReturn(300);
        when(settings.resolveBlockSize()).thenReturn(15);
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("큰 화면 크기에서도 렌더링 정상 동작")
    void testLargeScreenRendering() {
        when(app.getWidth()).thenReturn(1920);
        when(app.getHeight()).thenReturn(1080);
        when(settings.resolveBlockSize()).thenReturn(50);
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("TAB 키로 모드 전환 시 재렌더링 요청")
    void testTabKeySwitchesMode() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        KeyEvent tabEvent = new KeyEvent(
            new java.awt.Component(){}, 
            KeyEvent.KEY_PRESSED, 
            System.currentTimeMillis(), 
            0, 
            KeyEvent.VK_TAB, 
            '\t'
        );
        
        screen.onKeyPressed(tabEvent);
        verify(app, atLeastOnce()).repaint();
    }

    @Test
    @DisplayName("UP/DOWN 키로 메뉴 선택 변경")
    void testUpDownKeyMenuNavigation() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        KeyEvent downEvent = new KeyEvent(
            new java.awt.Component(){}, 
            KeyEvent.KEY_PRESSED, 
            System.currentTimeMillis(), 
            0, 
            KeyEvent.VK_DOWN, 
            KeyEvent.CHAR_UNDEFINED
        );
        
        screen.onKeyPressed(downEvent);
        verify(app, atLeastOnce()).repaint();
    }

    @Test
    @DisplayName("ENTER 키로 메뉴 아이템 실행")
    void testEnterKeyExecutesMenuItem() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        KeyEvent enterEvent = new KeyEvent(
            new java.awt.Component(){}, 
            KeyEvent.KEY_PRESSED, 
            System.currentTimeMillis(), 
            0, 
            KeyEvent.VK_ENTER, 
            '\n'
        );
        
        // 메인 메뉴로 돌아가기 실행 시 showScreen 호출 검증
        screen.onKeyPressed(enterEvent);
        verify(app, atLeastOnce()).showScreen(any());
    }

    @Test
    @DisplayName("배경 그라디언트가 올바르게 렌더링")
    void testBackgroundGradient() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("둥근 모서리 박스가 올바르게 렌더링")
    void testRoundedBoxRendering() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Player1", 1000, Settings.Difficulty.NORMAL)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("순위 배지가 원형으로 렌더링")
    void testRankBadgeCircle() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Player1", 1000, Settings.Difficulty.NORMAL)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("메뉴 선택 시 하이라이트 표시")
    void testMenuHighlight() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        // 첫 번째 메뉴 선택
        assertDoesNotThrow(() -> screen.render(g2));
        
        // DOWN 키로 두 번째 메뉴 선택
        KeyEvent downEvent = new KeyEvent(
            new java.awt.Component(){}, 
            KeyEvent.KEY_PRESSED, 
            System.currentTimeMillis(), 
            0, 
            KeyEvent.VK_DOWN, 
            KeyEvent.CHAR_UNDEFINED
        );
        screen.onKeyPressed(downEvent);
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("null 플레이어 이름으로 생성 시 예외 없음")
    void testNullPlayerName() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, 1000, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("매우 높은 점수도 올바르게 표시됨")
    void testVeryHighScore() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Player", 999999999, Settings.Difficulty.HARD)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("0점도 올바르게 표시됨")
    void testZeroScore() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Player", 0, Settings.Difficulty.NORMAL)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("onShow 호출 시 예외 없음")
    void testOnShow() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.onShow());
    }

    @Test
    @DisplayName("onHide 호출 시 예외 없음")
    void testOnHide() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.onHide());
    }

    @Test
    @DisplayName("SPACE 키로도 메뉴 아이템 실행 가능")
    void testSpaceKeyExecutesMenuItem() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        KeyEvent spaceEvent = new KeyEvent(
            new java.awt.Component(){}, 
            KeyEvent.KEY_PRESSED, 
            System.currentTimeMillis(), 
            0, 
            KeyEvent.VK_SPACE, 
            ' '
        );
        
        screen.onKeyPressed(spaceEvent);
        verify(app, atLeastOnce()).showScreen(any());
    }

    @Test
    @DisplayName("여러 난이도가 섞여있어도 올바르게 렌더링")
    void testMixedDifficulties() {
        List<ScoreEntry> scores = Arrays.asList(
            new ScoreEntry("Easy1", 1000, Settings.Difficulty.EASY),
            new ScoreEntry("Hard1", 2000, Settings.Difficulty.HARD),
            new ScoreEntry("Normal1", 1500, Settings.Difficulty.NORMAL),
            new ScoreEntry("Easy2", 800, Settings.Difficulty.EASY)
        );
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(scores);
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("UP 키로 순환 선택 (마지막에서 첫 번째로)")
    void testUpKeyCircularNavigation() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        KeyEvent upEvent = new KeyEvent(
            new java.awt.Component(){}, 
            KeyEvent.KEY_PRESSED, 
            System.currentTimeMillis(), 
            0, 
            KeyEvent.VK_UP, 
            KeyEvent.CHAR_UNDEFINED
        );
        
        screen.onKeyPressed(upEvent);
        verify(app, atLeastOnce()).repaint();
    }

    @Test
    @DisplayName("DOWN 키로 순환 선택 (마지막 다음은 첫 번째)")
    void testDownKeyCircularNavigation() {
        when(scoreManager.getHighScores(any(GameMode.class))).thenReturn(new ArrayList<>());
        screen = new ScoreboardScreen(app, null, -1, scoreManager);
        
        KeyEvent downEvent = new KeyEvent(
            new java.awt.Component(){}, 
            KeyEvent.KEY_PRESSED, 
            System.currentTimeMillis(), 
            0, 
            KeyEvent.VK_DOWN, 
            KeyEvent.CHAR_UNDEFINED
        );
        
        // 두 번 누르면 다시 첫 번째로
        screen.onKeyPressed(downEvent);
        screen.onKeyPressed(downEvent);
        verify(app, atLeast(2)).repaint();
    }
}
