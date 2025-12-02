package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.gameManager.ScoreManager;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.GameScreen;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 순위 진행 상황 표시 기능 테스트
 */
@DisplayName("순위 진행 상황 표시 테스트")
class RankProgressDisplayTest {
    
    private AppFrame app;
    private Settings settings;
    private GameManager manager;
    private GameScreen screen;
    private Graphics2D g2;
    private ScoreManager scoreManager;
    
    @BeforeEach
    void setup() {
        scoreManager = new ScoreManager();
        
        app = Mockito.mock(AppFrame.class);
        settings = new Settings();
        Mockito.when(app.getSettings()).thenReturn(settings);
        Mockito.when(app.getWidth()).thenReturn(800);
        Mockito.when(app.getHeight()).thenReturn(600);
        
        manager = Mockito.spy(new GameManager());
        screen = new GameScreen(app, manager);
        
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = img.createGraphics();
    }
    
    @AfterEach
    void tearDown() {
        if (g2 != null) g2.dispose();
    }
    
    @Test
    @DisplayName("순위가 없을 때 렌더링")
    void testNoRankings() {
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.getScore()).thenReturn(100);
        assertDoesNotThrow(() -> screen.render(g2));
    }
    
    @Test
    @DisplayName("현재 1등일 때 렌더링")
    void testCurrentlyFirstPlace() {
        scoreManager.addScore(GameMode.CLASSIC, "Player1", 1000);
        scoreManager.addScore(GameMode.CLASSIC, "Player2", 800);
        
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.getScore()).thenReturn(1500);
        assertDoesNotThrow(() -> screen.render(g2));
    }
    
    @Test
    @DisplayName("2-9등일 때 렌더링")
    void testSecondToNinthPlace() {
        scoreManager.addScore(GameMode.CLASSIC, "Top1", 2000);
        scoreManager.addScore(GameMode.CLASSIC, "Top2", 1500);
        scoreManager.addScore(GameMode.CLASSIC, "Top3", 1000);
        
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.getScore()).thenReturn(1200);
        assertDoesNotThrow(() -> screen.render(g2));
    }
    
    @Test
    @DisplayName("10등 밖일 때 렌더링")
    void testBelowTenth() {
        for (int i = 1; i <= 10; i++) {
            scoreManager.addScore(GameMode.CLASSIC, "Player" + i, 2000 - i * 100);
        }
        
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.getScore()).thenReturn(500);
        assertDoesNotThrow(() -> screen.render(g2));
    }
    
    @Test
    @DisplayName("점수가 증가하면서 순위 변화")
    void testRankChangesWithScore() {
        scoreManager.addScore(GameMode.CLASSIC, "Top1", 1000);
        scoreManager.addScore(GameMode.CLASSIC, "Top2", 800);
        scoreManager.addScore(GameMode.CLASSIC, "Top3", 600);
        
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        
        Mockito.when(manager.getScore()).thenReturn(500);
        assertDoesNotThrow(() -> screen.render(g2));
        
        Mockito.when(manager.getScore()).thenReturn(700);
        assertDoesNotThrow(() -> screen.render(g2));
        
        Mockito.when(manager.getScore()).thenReturn(1100);
        assertDoesNotThrow(() -> screen.render(g2));
    }
    
    @Test
    @DisplayName("다양한 화면 크기 대응")
    void testDifferentScreenSizes() {
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.getScore()).thenReturn(1000);
        
        Mockito.when(app.getWidth()).thenReturn(600);
        Mockito.when(app.getHeight()).thenReturn(400);
        assertDoesNotThrow(() -> screen.render(g2));
        
        Mockito.when(app.getWidth()).thenReturn(1920);
        Mockito.when(app.getHeight()).thenReturn(1080);
        assertDoesNotThrow(() -> screen.render(g2));
    }
}
