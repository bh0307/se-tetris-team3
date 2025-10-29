package se.tetris.team3.blocksTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.ui.GameManager;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.core.Settings;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.blocks.*;

import java.util.HashMap;
import java.util.Map;

public class BlockGenerationTest {
    // 테스트용 GameManager 확장 클래스를 클래스 내부에 둠
    private static class TestGameManager extends GameManager {
        public TestGameManager() {
            super(GameMode.CLASSIC);
        }

        public Block getRandomBlock() {
            return makeRandomBlock();
        }
    }

    private TestGameManager gameManager;
    private AppFrame app;
    private static final int ITERATIONS = 50000;
    private static final double ALLOWED_ERROR = 0.05;

    @BeforeEach
    void setUp() {
        app = new AppFrame();
        gameManager = new TestGameManager();
        Settings settings = app.getSettings();
        settings.setGameMode(GameMode.CLASSIC);
        gameManager.attachSettings(settings);
    }

    @Test
    @DisplayName("EASY 난이도에서의 블록 생성 확률 테스트")
    void testBlockProbabilityInEasyMode() {
        app.getSettings().setDifficulty(Settings.Difficulty.EASY);
        gameManager.attachSettings(app.getSettings());
        Map<Class<? extends Block>, Integer> blockCounts = countBlockGeneration();
        verifyBlockDistribution(blockCounts, 1.2);
    }

    @Test
    @DisplayName("NORMAL 난이도에서의 블록 생성 확률 테스트")
    void testBlockProbabilityInNormalMode() {
        app.getSettings().setDifficulty(Settings.Difficulty.NORMAL);
        gameManager.attachSettings(app.getSettings());
        Map<Class<? extends Block>, Integer> blockCounts = countBlockGeneration();
        verifyBlockDistribution(blockCounts, 1.0);
    }

    @Test
    @DisplayName("HARD 난이도에서의 블록 생성 확률 테스트")
    void testBlockProbabilityInHardMode() {
        app.getSettings().setDifficulty(Settings.Difficulty.HARD);
        gameManager.attachSettings(app.getSettings());
        Map<Class<? extends Block>, Integer> blockCounts = countBlockGeneration();
        verifyBlockDistribution(blockCounts, 0.8);
    }

    private Map<Class<? extends Block>, Integer> countBlockGeneration() {
        Map<Class<? extends Block>, Integer> blockCounts = new HashMap<>();

        blockCounts.put(IBlock.class, 0);
        blockCounts.put(JBlock.class, 0);
        blockCounts.put(LBlock.class, 0);
        blockCounts.put(OBlock.class, 0);
        blockCounts.put(SBlock.class, 0);
        blockCounts.put(TBlock.class, 0);
        blockCounts.put(ZBlock.class, 0);

        for (int i = 0; i < ITERATIONS; i++) {
            Block block = gameManager.getRandomBlock();
            blockCounts.merge(block.getClass(), 1, Integer::sum);
        }

        return blockCounts;
    }

    private void verifyBlockDistribution(Map<Class<? extends Block>, Integer> blockCounts, double iBlockWeight) {
        double baseWeight = 10.0;
        double totalWeight = (baseWeight * 6) + (baseWeight * iBlockWeight);

        double expectedIBlockCount = (baseWeight * iBlockWeight / totalWeight) * ITERATIONS;
        double expectedOtherBlockCount = (baseWeight / totalWeight) * ITERATIONS;

        double allowedIBlockError = expectedIBlockCount * ALLOWED_ERROR;
        double allowedOtherBlockError = expectedOtherBlockCount * ALLOWED_ERROR;

        int actualIBlockCount = blockCounts.get(IBlock.class);
        assertTrue(Math.abs(actualIBlockCount - expectedIBlockCount) <= allowedIBlockError,
                String.format("I블록 생성 횟수가 예상 범위를 벗어났습니다. 예상: %.2f±%.2f, 실제: %d",
                        expectedIBlockCount, allowedIBlockError, actualIBlockCount));

        for (Map.Entry<Class<? extends Block>, Integer> entry : blockCounts.entrySet()) {
            if (entry.getKey() != IBlock.class) {
                int count = entry.getValue();
                assertTrue(Math.abs(count - expectedOtherBlockCount) <= allowedOtherBlockError,
                        String.format("%s 생성 횟수가 예상 범위를 벗어났습니다. 예상: %.2f±%.2f, 실제: %d",
                                entry.getKey().getSimpleName(), expectedOtherBlockCount, allowedOtherBlockError, count));
            }
        }
    }
}