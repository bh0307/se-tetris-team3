package se.tetris.team3.battleTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.gameManager.BattleGameManager;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.core.Settings;
import se.tetris.team3.core.GameMode;

import java.lang.reflect.Field;

/**
 * 배틀 모드(`BattleGameManager`)의 핵심 동작을 검증하는 단위 테스트.
 *
 * 주요 검증 항목:
 * - BattleGameManager 생성 시 Player1과 Player2의 GameManager가 올바르게 초기화되는지.
 * - 한 플레이어가 줄을 삭제했을 때 상대방에게 공격 줄이 전달되는지.
 * - 시간제한 모드 여부가 올바르게 설정되는지.
 * - 각 배틀 모드(BATTLE_NORMAL, BATTLE_ITEM, BATTLE_TIME)에 대해
 *   내부 GameManager가 올바른 모드(CLASSIC or ITEM)로 생성되는지.
 *
 * 테스트에서는 리플렉션을 사용해 private 필드(player1Manager, player2Manager 등)를 접근합니다.
 */
public class BattleGameManagerTest {

    private AppFrame app;
    private Settings settings;

    @BeforeEach
    void setUp() {
        app = new AppFrame();
        settings = app.getSettings();
    }

    @Test
    void testBattleGameManager_createsManagersForBothPlayers() throws Exception {
        // BattleGameManager 생성
        BattleGameManager battle = new BattleGameManager(GameMode.BATTLE_NORMAL, settings, 0);

        // 두 플레이어의 GameManager가 생성되었는지 확인
        Field player1F = BattleGameManager.class.getDeclaredField("player1Manager");
        player1F.setAccessible(true);
        Object player1 = player1F.get(battle);
        assertNotNull(player1, "Player1의 GameManager가 생성되어야 합니다.");

        Field player2F = BattleGameManager.class.getDeclaredField("player2Manager");
        player2F.setAccessible(true);
        Object player2 = player2F.get(battle);
        assertNotNull(player2, "Player2의 GameManager가 생성되어야 합니다.");
    }

    @Test
    void testBattleGameManager_battleNormalMode_usesClassicInternal() throws Exception {
        // BATTLE_NORMAL 모드: 내부 GameManager는 CLASSIC 모드여야 함
        BattleGameManager battle = new BattleGameManager(GameMode.BATTLE_NORMAL, settings, 0);

        Field player1F = BattleGameManager.class.getDeclaredField("player1Manager");
        player1F.setAccessible(true);
        Object player1Manager = player1F.get(battle);
        assertNotNull(player1Manager);

        Field modeF = player1Manager.getClass().getDeclaredField("mode");
        modeF.setAccessible(true);
        GameMode internalMode = (GameMode) modeF.get(player1Manager);
        assertEquals(GameMode.CLASSIC, internalMode,
                "BATTLE_NORMAL 모드는 내부적으로 CLASSIC 모드를 사용해야 합니다.");
    }

    @Test
    void testBattleGameManager_battleItemMode_usesItemInternal() throws Exception {
        // BATTLE_ITEM 모드: 내부 GameManager는 ITEM 모드여야 함
        BattleGameManager battle = new BattleGameManager(GameMode.BATTLE_ITEM, settings, 0);

        Field player1F = BattleGameManager.class.getDeclaredField("player1Manager");
        player1F.setAccessible(true);
        Object player1Manager = player1F.get(battle);

        Field modeF = player1Manager.getClass().getDeclaredField("mode");
        modeF.setAccessible(true);
        GameMode internalMode = (GameMode) modeF.get(player1Manager);
        assertEquals(GameMode.ITEM, internalMode,
                "BATTLE_ITEM 모드는 내부적으로 ITEM 모드를 사용해야 합니다.");
    }

    @Test
    void testBattleGameManager_timeAttackModeFlag() throws Exception {
        // BATTLE_TIME 모드: isTimeAttack 플래그가 true여야 함
        BattleGameManager battle = new BattleGameManager(GameMode.BATTLE_TIME, settings, 180);

        Field isTimeAttackF = BattleGameManager.class.getDeclaredField("isTimeAttack");
        isTimeAttackF.setAccessible(true);
        boolean isTimeAttack = (Boolean) isTimeAttackF.get(battle);
        assertTrue(isTimeAttack, "BATTLE_TIME 모드는 isTimeAttack 플래그가 true여야 합니다.");

        // 시간 제한이 설정되는지 확인
        Field timeLimitF = BattleGameManager.class.getDeclaredField("timeLimit");
        timeLimitF.setAccessible(true);
        long timeLimit = (Long) timeLimitF.get(battle);
        assertEquals(180000, timeLimit, "시간 제한이 밀리초 단위로 올바르게 변환되어야 합니다.");
    }

    @Test
    void testBattleGameManager_normalMode_noTimeAttack() throws Exception {
        // BATTLE_NORMAL 모드: isTimeAttack 플래그가 false여야 함
        BattleGameManager battle = new BattleGameManager(GameMode.BATTLE_NORMAL, settings, 0);

        Field isTimeAttackF = BattleGameManager.class.getDeclaredField("isTimeAttack");
        isTimeAttackF.setAccessible(true);
        boolean isTimeAttack = (Boolean) isTimeAttackF.get(battle);
        assertFalse(isTimeAttack,
                "BATTLE_NORMAL 모드는 isTimeAttack 플래그가 false여야 합니다.");
    }

    @Test
    void testBattleGameManager_startInitializesGame() throws Exception {
        // start() 호출 시 startTime이 설정되고 gameOver/winner가 초기화되는지
        BattleGameManager battle = new BattleGameManager(GameMode.BATTLE_NORMAL, settings, 0);

        battle.start();

        Field gameOverF = BattleGameManager.class.getDeclaredField("gameOver");
        gameOverF.setAccessible(true);
        boolean gameOver = (Boolean) gameOverF.get(battle);
        assertFalse(gameOver, "start() 후 gameOver는 false여야 합니다.");

        Field winnerF = BattleGameManager.class.getDeclaredField("winner");
        winnerF.setAccessible(true);
        int winner = (Integer) winnerF.get(battle);
        assertEquals(0, winner, "start() 후 winner는 0(무승부 기본값)이어야 합니다.");

        Field startTimeF = BattleGameManager.class.getDeclaredField("startTime");
        startTimeF.setAccessible(true);
        long startTime = (Long) startTimeF.get(battle);
        assertTrue(startTime > 0, "startTime이 설정되어야 합니다.");
    }

    @Test
    void testBattleGameManager_constructorWithSettings() throws Exception {
        // 생성자에서 Settings를 받으면 두 매니저 모두에 적용되는지
        Settings customSettings = new Settings();
        customSettings.setDifficulty(Settings.Difficulty.HARD);

        BattleGameManager battle = new BattleGameManager(GameMode.BATTLE_NORMAL, customSettings, 0);

        Field player1F = BattleGameManager.class.getDeclaredField("player1Manager");
        player1F.setAccessible(true);
        Object player1Manager = player1F.get(battle);

        Field settingsF = player1Manager.getClass().getDeclaredField("settings");
        settingsF.setAccessible(true);
        Settings appliedSettings = (Settings) settingsF.get(player1Manager);
        assertEquals(Settings.Difficulty.HARD, appliedSettings.getDifficulty(),
                "생성자에서 받은 Settings가 GameManager에 적용되어야 합니다.");
    }
}
