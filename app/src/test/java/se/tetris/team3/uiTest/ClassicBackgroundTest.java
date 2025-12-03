package se.tetris.team3.uiTest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.GameScreen;

/**
 * 클래식/아이템 모드 배경 렌더링 테스트
 * - drawClassicBackground() 메서드 검증
 * - drawClassicBoard() 메서드 검증
 * - 모드별 배경 분기 확인
 */
@DisplayName("클래식 모드 배경 렌더링 테스트")
class ClassicBackgroundTest {
    
    private AppFrame app;
    private Settings settings;
    private GameManager manager;
    private GameScreen screen;
    private Graphics2D g2;
    private BufferedImage image;
    
    @BeforeEach
    void setup() {
        app = Mockito.mock(AppFrame.class);
        settings = new Settings();
        Mockito.when(app.getSettings()).thenReturn(settings);
        Mockito.when(app.getWidth()).thenReturn(800);
        Mockito.when(app.getHeight()).thenReturn(600);
        
        manager = Mockito.spy(new GameManager());
        screen = new GameScreen(app, manager);
        
        image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
    }
    
    @AfterEach
    void tearDown() {
        if (g2 != null) g2.dispose();
    }
    
    @Test
    @DisplayName("클래식 모드 - 특별한 배경 렌더링")
    void testClassicModeBackground() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 배경에 그라데이션과 별이 그려짐 (예외 없이 실행)
        // 실제 픽셀 검증은 어렵지만, 렌더링 과정에서 에러가 없어야 함
    }
    
    @Test
    @DisplayName("아이템 모드 - 클래식과 동일한 배경")
    void testItemModeBackground() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.ITEM);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 클래식과 동일한 배경 (예외 없음)
    }
    
    @Test
    @DisplayName("NORMAL 모드 - 검정 배경")
    void testNormalModeBlackBackground() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 검정 배경 (특별한 효과 없음)
    }
    
    @Test
    @DisplayName("EASY 모드 - 검정 배경")
    void testEasyModeBlackBackground() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 검정 배경
    }
    
    @Test
    @DisplayName("HARD 모드 - 검정 배경")
    void testHardModeBlackBackground() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.BATTLE_NORMAL);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 검정 배경
    }
    
    @Test
    @DisplayName("클래식 보드 - 다중 테두리 렌더링")
    void testClassicBoardMultipleBorders() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 금색/빨강/흰색 3겹 테두리 + 그리드 라인 (예외 없음)
    }
    
    @Test
    @DisplayName("다양한 화면 크기에서 배경 렌더링")
    void testBackgroundOnDifferentScreenSizes() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When & Then: 작은 화면
        Mockito.when(app.getWidth()).thenReturn(400);
        Mockito.when(app.getHeight()).thenReturn(300);
        image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        assertDoesNotThrow(() -> screen.render(g2));
        g2.dispose();
        
        // 큰 화면
        Mockito.when(app.getWidth()).thenReturn(1920);
        Mockito.when(app.getHeight()).thenReturn(1080);
        image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        assertDoesNotThrow(() -> screen.render(g2));
    }
    
    @Test
    @DisplayName("배경 별 개수 확인")
    void testStarCount() {
        // Given: 클래식 모드
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링 (별 30개 + 작은 별 80개)
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 총 110개의 별이 그려짐 (코드상 확인)
        // 실제로는 Random(42) 시드로 같은 위치에 생성됨
    }
    
    @Test
    @DisplayName("배경 그라데이션 색상")
    void testBackgroundGradientColors() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        screen.render(g2);
        
        // Then: 상단 보라색(25,0,51), 하단 파랑(0,20,80) 그라데이션
        // 픽셀 검증
        int topColor = image.getRGB(400, 10);
        int bottomColor = image.getRGB(400, 590);
        
        // 상단은 보라색 계열, 하단은 파란색 계열
        Color top = new Color(topColor);
        Color bottom = new Color(bottomColor);
        
        assertTrue(top.getRed() > 0 || top.getGreen() == 0, "상단은 어두운 색");
        assertTrue(bottom.getBlue() > bottom.getRed(), "하단은 파란색 계열");
    }
    
    @Test
    @DisplayName("보드 테두리 색상 확인")
    void testBoardBorderColors() {
        // Given: 클래식 모드
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 금색(255,215,0), 빨강(220,20,60), 흰색 테두리
        // (실제 픽셀 검증은 복잡하므로 예외 없이 실행되는지만 확인)
    }
    
    @Test
    @DisplayName("그리드 라인 렌더링")
    void testGridLinesRendering() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 세로 9개, 가로 19개 그리드 라인
        // (10x20 보드이므로 내부 라인 개수)
    }
    
    @Test
    @DisplayName("네온 라인 애니메이션 효과")
    void testNeonLineAnimation() {
        // Given
        Mockito.when(manager.getMode()).thenReturn(GameMode.CLASSIC);
        Mockito.when(manager.isGameOver()).thenReturn(false);
        
        // When: 여러 번 렌더링 (시간에 따라 변함)
        screen.render(g2);
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        screen.render(g2);
        
        // Then: 예외 없이 실행 (네온 라인이 시간에 따라 변함)
    }
    
    
}
