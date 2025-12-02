package se.tetris.team3.gameManagerTest;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import se.tetris.team3.core.Settings;
import se.tetris.team3.gameManager.GameManager;

/**
 * 게임 속도 조정 테스트
 */
@DisplayName("게임 속도 테스트")
class GameSpeedTest {
    
    private GameManager manager;
    private Settings settings;
    
    @BeforeEach
    void setup() {
        settings = new Settings();
        manager = new GameManager();
        manager.attachSettings(settings);
    }
    
    private void setLevel(int level) throws Exception {
        Field levelField = GameManager.class.getDeclaredField("level");
        levelField.setAccessible(true);
        levelField.setInt(manager, level);
    }
    
    @Test
    @DisplayName("EASY 난이도 - 레벨당 100ms 감소")
    void testEasyDifficultySpeed() throws Exception {
        settings.setDifficulty(Settings.Difficulty.EASY);
        manager.attachSettings(settings); // 설정 다시 적용
        
        setLevel(1);
        assertEquals(1000, manager.getGameTimerDelay());
        
        setLevel(5);
        assertEquals(600, manager.getGameTimerDelay());
        
        setLevel(10);
        assertEquals(100, manager.getGameTimerDelay());
    }
    
    @Test
    @DisplayName("NORMAL 난이도 - 레벨당 120ms 감소")
    void testNormalDifficultySpeed() throws Exception {
        settings.setDifficulty(Settings.Difficulty.NORMAL);
        manager.attachSettings(settings); // 설정 다시 적용
        
        setLevel(1);
        assertEquals(1000, manager.getGameTimerDelay());
        
        setLevel(2);
        assertEquals(880, manager.getGameTimerDelay());
        
        setLevel(5);
        assertEquals(520, manager.getGameTimerDelay());
    }
    
    @Test
    @DisplayName("HARD 난이도 - 레벨당 150ms 감소")
    void testHardDifficultySpeed() throws Exception {
        settings.setDifficulty(Settings.Difficulty.HARD);
        manager.attachSettings(settings); // 설정 다시 적용
        
        setLevel(1);
        assertEquals(1000, manager.getGameTimerDelay());
        
        setLevel(2);
        assertEquals(850, manager.getGameTimerDelay());
        
        setLevel(4);
        assertEquals(550, manager.getGameTimerDelay());
    }
    
    @Test
    @DisplayName("최소 딜레이는 50ms")
    void testMinimumDelay() throws Exception {
        settings.setDifficulty(Settings.Difficulty.HARD);
        manager.attachSettings(settings); // 설정 다시 적용
        setLevel(100);
        
        int delay = manager.getGameTimerDelay();
        assertTrue(delay >= 50);
    }
    
    @Test
    @DisplayName("레벨이 올라갈수록 속도 증가")
    void testSpeedIncreasesWithLevel() throws Exception {
        settings.setDifficulty(Settings.Difficulty.NORMAL);
        manager.attachSettings(settings); // 설정 다시 적용
        
        setLevel(1);
        int delay1 = manager.getGameTimerDelay();
        
        setLevel(3);
        int delay3 = manager.getGameTimerDelay();
        
        assertTrue(delay1 > delay3);
    }
}
