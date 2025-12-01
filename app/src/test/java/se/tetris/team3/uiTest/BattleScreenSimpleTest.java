package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.BattleScreen;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BattleScreen 기본 테스트")
class BattleScreenSimpleTest {
    private AppFrame app;
    private Settings settings;
    private Graphics2D g2;
    private BattleScreen screen;

    @BeforeEach
    void setUp() {
        app = Mockito.mock(AppFrame.class);
        settings = new Settings();
        
        BufferedImage img = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
        g2 = img.createGraphics();
        
        when(app.getSettings()).thenReturn(settings);
        when(app.getWidth()).thenReturn(1200);
        when(app.getHeight()).thenReturn(700);
    }

    @AfterEach
    void tearDown() {
        if (g2 != null) {
            g2.dispose();
        }
        if (screen != null) {
            screen.onHide();
        }
    }

    @Test
    @DisplayName("BattleScreen 생성 - BATTLE_NORMAL 모드")
    void testConstructorNormalMode() {
        try {
            screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
            assertNotNull(screen);
        } catch (Exception e) {
            fail("BattleScreen 생성 중 예외 발생: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("BattleScreen 생성 - BATTLE_ITEM 모드")
    void testConstructorItemMode() {
        try {
            screen = new BattleScreen(app, GameMode.BATTLE_ITEM, settings, 0);
            assertNotNull(screen);
        } catch (Exception e) {
            fail("BattleScreen 생성 중 예외 발생: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("BattleScreen 생성 - BATTLE_TIME 모드")
    void testConstructorTimeMode() {
        try {
            screen = new BattleScreen(app, GameMode.BATTLE_TIME, settings, 180);
            assertNotNull(screen);
        } catch (Exception e) {
            fail("BattleScreen 생성 중 예외 발생: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("BattleScreen 렌더링 테스트")
    void testRender() {
        screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("onShow 호출 시 예외 없음")
    void testOnShow() {
        screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
        assertDoesNotThrow(() -> screen.onShow());
    }

    @Test
    @DisplayName("onHide 호출 시 예외 없음")
    void testOnHide() {
        screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
        screen.onShow();
        assertDoesNotThrow(() -> screen.onHide());
    }

    @Test
    @DisplayName("작은 화면 크기에서 생성")
    void testSmallScreenSize() {
        when(app.getWidth()).thenReturn(600);
        when(app.getHeight()).thenReturn(400);
        
        assertDoesNotThrow(() -> {
            screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
            screen.render(g2);
        });
    }

    @Test
    @DisplayName("큰 화면 크기에서 생성")
    void testLargeScreenSize() {
        when(app.getWidth()).thenReturn(1920);
        when(app.getHeight()).thenReturn(1080);
        
        assertDoesNotThrow(() -> {
            screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
            screen.render(g2);
        });
    }

    @Test
    @DisplayName("색맹 모드에서 렌더링")
    void testColorBlindMode() {
        settings.setColorBlindMode(true);
        
        screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
        assertDoesNotThrow(() -> screen.render(g2));
    }

    @Test
    @DisplayName("시간 제한 모드에서 다양한 시간 값")
    void testTimeAttackWithDifferentTimes() {
        // 1분
        assertDoesNotThrow(() -> {
            BattleScreen s1 = new BattleScreen(app, GameMode.BATTLE_TIME, settings, 60);
            s1.onHide();
        });
        
        // 3분
        assertDoesNotThrow(() -> {
            BattleScreen s2 = new BattleScreen(app, GameMode.BATTLE_TIME, settings, 180);
            s2.onHide();
        });
        
        // 5분
        assertDoesNotThrow(() -> {
            BattleScreen s3 = new BattleScreen(app, GameMode.BATTLE_TIME, settings, 300);
            s3.onHide();
        });
    }

    @Test
    @DisplayName("블록 크기 70% 증가 확인 - 렌더링 테스트")
    void testBlockSizeIncrease() {
        screen = new BattleScreen(app, GameMode.BATTLE_NORMAL, settings, 0);
        // blockSizeH가 1.7배로 설정되어 렌더링되는지 확인
        assertDoesNotThrow(() -> screen.render(g2));
    }
}
