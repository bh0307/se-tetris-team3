package se.tetris.team3.storeTest;

import org.junit.jupiter.api.*;
import se.tetris.team3.core.Settings;
import se.tetris.team3.store.SettingsStore;

import java.io.File;
import java.util.List;

import se.tetris.team3.gameManager.ScoreManager;
import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreTest {
    private Settings settings;
    private static final String SETTINGS_PATH = System.getProperty("user.home") + File.separator + ".tetris" + File.separator + "settings.properties";

    @BeforeEach
    void setUp() {
        settings = new Settings();
        // 테스트 전 기존 파일 삭제 (격리)
        File f = new File(SETTINGS_PATH);
        if (f.exists()) f.delete();
    }

    @Test
    void testSaveAndLoadSettings() {
        settings.setColorBlindMode(true);
        SettingsStore.save(settings);
        Settings loaded = new Settings();
        SettingsStore.load(loaded);
        assertTrue(loaded.isColorBlindMode());
    }

    @Test
    void testRecentP2PIPs() {
        SettingsStore.addRecentP2PIP("127.0.0.1");
        SettingsStore.addRecentP2PIP("192.168.0.1");
        List<String> ips = SettingsStore.getRecentP2PIPs();
        assertEquals("192.168.0.1", ips.get(0));
        assertEquals("127.0.0.1", ips.get(1));
    }

    @Test
    void testResetScores() {
        // 점수 임의로 추가
        ScoreManager manager = new ScoreManager();
        manager.addScore("testUser", 12345);
        assertFalse(manager.getHighScores().isEmpty(), "Score should exist before reset");

        // 점수 초기화
        SettingsStore.resetScores();

        // 실제로 점수가 모두 삭제되었는지 확인
        ScoreManager managerAfter = new ScoreManager();
        assertTrue(managerAfter.getHighScores().isEmpty(), "Scores should be empty after reset");
    }
}
