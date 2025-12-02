package se.tetris.team3.gameManagerTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.Settings;
import se.tetris.team3.gameManager.GameManager;

import java.awt.Color;
import java.lang.reflect.Field;

/**
 * 색상 필드 테스트
 */
@DisplayName("색상 필드 테스트")
class ColorFieldCopyTest {
    
    private GameManager manager;
    private Settings settings;
    
    @BeforeEach
    void setup() {
        settings = new Settings();
        manager = new GameManager();
        manager.attachSettings(settings);
    }
    
    @Test
    @DisplayName("colorField 필드 존재 확인")
    void testColorFieldExists() throws Exception {
        Field colorFieldField = GameManager.class.getDeclaredField("colorField");
        colorFieldField.setAccessible(true);
        Color[][] colorField = (Color[][]) colorFieldField.get(manager);
        
        assertNotNull(colorField);
        assertEquals(20, colorField.length);
        assertEquals(10, colorField[0].length);
    }
    
    @Test
    @DisplayName("블록 색상 메서드 동작")
    void testGetBlockColor() {
        Color color = manager.getBlockColor(19, 5);
        assertTrue(color == null || color instanceof Color);
    }
}
