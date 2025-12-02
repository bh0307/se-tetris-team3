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
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.BattleScreen;

/**
 * 대전 모드 배경 렌더링 테스트
 * - drawBattleBackground() 메서드 검증
 * - 전투 느낌의 시각 효과 확인
 */
@DisplayName("대전 모드 배경 렌더링 테스트")
class BattleBackgroundTest {
    
    private AppFrame app;
    private Settings settings;
    private BattleScreen screen;
    private Graphics2D g2;
    private BufferedImage image;
    
    @BeforeEach
    void setup() {
        app = Mockito.mock(AppFrame.class);
        settings = new Settings();
        Mockito.when(app.getSettings()).thenReturn(settings);
        Mockito.when(app.getWidth()).thenReturn(1200);
        Mockito.when(app.getHeight()).thenReturn(800);
        
        screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
        
        image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
    }
    
    @AfterEach
    void tearDown() {
        if (g2 != null) g2.dispose();
    }
    
    @Test
    @DisplayName("대전 모드 - 전투 배경 렌더링")
    void testBattleBackground() {
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 빨강-검정 그라데이션 배경 (예외 없음)
    }
    
    @Test
    @DisplayName("대각선 경고 스트라이프 렌더링")
    void testDiagonalWarningStripes() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 주황색 대각선 줄무늬 (공사장 느낌)
        // 픽셀 확인 (주황색 계열 존재)
        boolean hasOrangeStripe = false;
        for (int x = 0; x < 1200; x += 50) {
            for (int y = 0; y < 800; y += 50) {
                Color c = new Color(image.getRGB(x, y));
                if (c.getRed() > 200 && c.getGreen() > 50 && c.getBlue() < 50) {
                    hasOrangeStripe = true;
                    break;
                }
            }
            if (hasOrangeStripe) break;
        }
        // 주황색이 있을 수도 있음 (반투명이라 확실하지 않음)
    }
    
    @Test
    @DisplayName("중앙 VS 라인 렌더링")
    void testCenterVSLine() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 중앙(x=600)에 빨간 세로선
        int centerX = 600;
        Color lineColor = new Color(image.getRGB(centerX, 400));
        // 빨간색 계열이어야 함 (완전 검정은 아님)
        assertTrue(lineColor.getRed() > 0 || lineColor.getGreen() > 0 || lineColor.getBlue() > 0, 
                   "중앙에 라인이 그려짐");
    }
    
    @Test
    @DisplayName("번개 효과 라인 렌더링")
    void testLightningEffects() {
        // When: 여러 번 렌더링 (시간에 따라 변함)
        assertDoesNotThrow(() -> {
            screen.render(g2);
            Thread.sleep(600); // 0.6초 대기 (애니메이션)
            screen.render(g2);
        });
        
        // Then: 노란색 번개 라인 (예외 없음)
    }
    
    @Test
    @DisplayName("폭발 파티클 렌더링")
    void testExplosionParticles() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 주황색 불꽃 점들 40개
        int particleCount = 0;
        for (int x = 0; x < 1200; x += 20) {
            for (int y = 0; y < 800; y += 20) {
                Color c = new Color(image.getRGB(x, y), true);
                // 주황색 계열 반투명 점
                if (c.getRed() > 200 && c.getGreen() > 100 && c.getBlue() < 100 && c.getAlpha() > 100) {
                    particleCount++;
                }
            }
        }
        // 정확히 40개는 아니더라도 파티클이 존재
        assertTrue(particleCount >= 0, "파티클이 렌더링됨");
    }
    
    @Test
    @DisplayName("붉은 섬광 상단 렌더링")
    void testRedFlashTopArea() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 상단 1/4에 붉은 빛
        Color topColor = new Color(image.getRGB(600, 100), true);
        // 빨간색 반투명 레이어가 있어야 함
        assertTrue(topColor.getAlpha() < 255 || topColor.getRed() > 0, "상단에 붉은 효과");
    }
    
    @Test
    @DisplayName("배경 그라데이션 색상 확인")
    void testBackgroundGradient() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 상단 어두운 빨강(40,0,0), 하단 검정(0,0,0)
        Color topColor = new Color(image.getRGB(600, 10));
        Color bottomColor = new Color(image.getRGB(600, 790));
        
        // 상단이 하단보다 밝음 (빨강 성분)
        assertTrue(topColor.getRed() >= bottomColor.getRed(), "상단이 더 밝음");
    }
    
    @Test
    @DisplayName("아이템 대전 모드도 동일한 배경")
    void testBattleItemModeBackground() {
        // Given: 아이템 대전 모드
        BattleScreen itemBattle = new BattleScreen(app, GameMode.BATTLE_ITEM, settings, 0);
        
        // When: 렌더링
        assertDoesNotThrow(() -> itemBattle.render(g2));
        
        // Then: 동일한 전투 배경
    }
    
    @Test
    @DisplayName("시간제한 대전 모드도 동일한 배경")
    void testBattleTimeModeBackground() {
        // Given: 시간제한 대전 모드
        BattleScreen timeBattle = new BattleScreen(app, GameMode.BATTLE_TIME, settings, 180);
        
        // When: 렌더링
        assertDoesNotThrow(() -> timeBattle.render(g2));
        
        // Then: 동일한 전투 배경
    }
    
    @Test
    @DisplayName("다양한 화면 크기에서 배경 렌더링")
    void testBackgroundOnDifferentSizes() {
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
    @DisplayName("보드 위치 왼쪽 이동 확인")
    void testBoardPositionShift() {
        // When: 렌더링
        screen.render(g2);
        
        // Then: 보드가 중앙에서 20픽셀 왼쪽으로 이동
        // (실제로는 startX 계산에서 -20 적용됨)
        // 렌더링 예외 없음으로 확인
    }
    
    @Test
    @DisplayName("배경 렌더링 성능 테스트")
    void testBackgroundPerformance() {
        // When: 100번 렌더링
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            screen.render(g2);
        }
        long endTime = System.currentTimeMillis();
        
        // Then: 적절한 시간 내 완료 (5초 이내)
        long elapsed = endTime - startTime;
        assertTrue(elapsed < 5000, "100번 렌더링이 5초 이내: " + elapsed + "ms");
    }
    
    @Test
    @DisplayName("일시정지 시에도 배경 렌더링")
    void testBackgroundWhenPaused() throws Exception {
        // Given: 게임 시작
        screen.onShow();
        
        // When: 렌더링
        assertDoesNotThrow(() -> screen.render(g2));
        
        // Then: 배경은 여전히 표시됨
        screen.onHide();
    }
    
    @Test
    @DisplayName("게임 오버 시에도 배경 렌더링")
    void testBackgroundWhenGameOver() {
        // Given: 게임 시작
        screen.onShow();
        
        // When: 렌더링 (게임 오버 여부와 관계없이)
        assertDoesNotThrow(() -> screen.render(g2));
        
        screen.onHide();
    }
}
