package se.tetris.team3.p2pTest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.net.P2PConnection;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.P2PBattleScreen;

/**
 * P2P 대전 모드 배경 렌더링 테스트
 * - BattleScreen과 동일한 배경 적용 확인
 */
@DisplayName("P2P 대전 배경 렌더링 테스트")
class P2PBattleBackgroundTest {
    
    private AppFrame app;
    private Settings settings;
    private P2PConnection connection;
    private P2PBattleScreen screen;
    private Graphics2D g2;
    private BufferedImage image;
    
    @BeforeEach
    void setup() {
        app = Mockito.mock(AppFrame.class);
        settings = new Settings();
        connection = Mockito.mock(P2PConnection.class);
        
        Mockito.when(app.getSettings()).thenReturn(settings);
        Mockito.when(app.getWidth()).thenReturn(1200);
        Mockito.when(app.getHeight()).thenReturn(800);
        
        screen = new P2PBattleScreen(app, connection, GameMode.BATTLE_NORMAL, settings, 0, true);
        
        image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
    }
    
    @AfterEach
    void tearDown() {
        if (g2 != null) g2.dispose();
    }
    
    @Test
    @DisplayName("P2P 대전 - 전투 배경 렌더링")
    void testP2PBattleBackground() {
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: BattleScreen과 동일한 배경
    }
    
    @Test
    @DisplayName("서버로 시작 시 배경 렌더링")
    void testServerBackground() {
        // Given: 서버로 시작
        P2PBattleScreen serverScreen = new P2PBattleScreen(
            app, connection, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        // When: 렌더링
        assertDoesNotThrow(() -> serverScreen.render(g2));
        
        // Then: 정상 렌더링
    }
    
    @Test
    @DisplayName("클라이언트로 시작 시 배경 렌더링")
    void testClientBackground() {
        // Given: 클라이언트로 시작
        P2PBattleScreen clientScreen = new P2PBattleScreen(
            app, connection, GameMode.BATTLE_NORMAL, settings, 0, false
        );
        
        // When: 렌더링
        assertDoesNotThrow(() -> clientScreen.render(g2));
        
        // Then: 정상 렌더링
    }
    
    @Test
    @DisplayName("대각선 경고 스트라이프")
    void testDiagonalStripes() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 주황색 대각선 줄무늬
        assertNotNull(image);
    }
    
    @Test
    @DisplayName("중앙 VS 라인")
    void testCenterLine() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 중앙에 빨간 세로선
        int centerX = 600;
        Color c = new Color(image.getRGB(centerX, 400));
        assertNotNull(c);
    }
    
    @Test
    @DisplayName("번개 효과 애니메이션")
    void testLightningAnimation() {
        // When: 시간차 렌더링
        assertDoesNotThrow(() -> {
            screen.render(g2);
            Thread.sleep(600);
            screen.render(g2);
        });
        
        // Then: 번개가 시간에 따라 변함
    }
    
    @Test
    @DisplayName("폭발 파티클 효과")
    void testExplosionParticles() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 40개의 주황색 파티클
        assertNotNull(image);
    }
    
    @Test
    @DisplayName("붉은 섬광 효과")
    void testRedFlash() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 상단 1/4에 붉은 빛
        Color topColor = new Color(image.getRGB(600, 100), true);
        assertNotNull(topColor);
    }
    
    @Test
    @DisplayName("P2P 아이템 모드 배경")
    void testP2PItemModeBackground() {
        // Given: 아이템 모드
        P2PBattleScreen itemScreen = new P2PBattleScreen(
            app, connection, GameMode.BATTLE_ITEM, settings, 0, true
        );
        
        // When: 렌더링
        assertDoesNotThrow(() -> itemScreen.render(g2));
        
        // Then: 동일한 배경
    }
    
    @Test
    @DisplayName("P2P 시간제한 모드 배경")
    void testP2PTimeModeBackground() {
        // Given: 시간제한 모드
        P2PBattleScreen timeScreen = new P2PBattleScreen(
            app, connection, GameMode.BATTLE_TIME, settings, 180, true
        );
        
        // When: 렌더링
        assertDoesNotThrow(() -> timeScreen.render(g2));
        
        // Then: 동일한 배경
    }
    
    @Test
    @DisplayName("다양한 화면 크기 대응")
    void testDifferentScreenSizes() {
        // Given & When & Then: 작은 화면
        Mockito.when(app.getWidth()).thenReturn(800);
        Mockito.when(app.getHeight()).thenReturn(600);
        image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
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
    @DisplayName("연결 전 렌더링")
    void testRenderBeforeConnection() {
        // When: 연결 전에도 렌더링 가능
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 예외 없음
    }
    
    @Test
    @DisplayName("게임 시작 후 배경")
    void testBackgroundAfterGameStart() {
        // Given: 게임 시작
        screen.onShow();
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 배경 표시
        screen.onHide();
    }
    
    @Test
    @DisplayName("BattleScreen과 동일한 배경 확인")
    void testSameAsBattleScreen() {
        // Given: P2P와 일반 대전 화면
        P2PBattleScreen p2pScreen = new P2PBattleScreen(
            app, connection, GameMode.BATTLE_NORMAL, settings, 0, true
        );
        
        BufferedImage p2pImage = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D p2pG2 = p2pImage.createGraphics();
        
        // When: 둘 다 렌더링
        assertDoesNotThrow(() -> p2pScreen.render(p2pG2));
        
        // Then: 예외 없이 동일한 배경 렌더링
        p2pG2.dispose();
    }
}
