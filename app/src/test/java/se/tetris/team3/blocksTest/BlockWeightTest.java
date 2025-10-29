package se.tetris.team3.blocksTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.ui.GameManager;
import se.tetris.team3.blocks.*;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;

import java.util.HashMap;
import java.util.Map;

public class BlockWeightTest {
    
    private static class TestGameManager extends GameManager {
        public TestGameManager(Settings.Difficulty difficulty) {
            super(GameMode.CLASSIC);
            attachSettings(new Settings() {{
                setDifficulty(difficulty);
            }});
        }

        @Override
        public Block makeRandomBlock() {
            return super.makeRandomBlock();
        }
    }

    private static final int ITERATIONS = 100000;  // 100000번 반복
    private static final double ALLOWED_ERROR = 0.05;  // 허용 오차 5%

    @Test
    @DisplayName("EASY 난이도에서의 블록 생성 확률 테스트")
    void testBlockProbabilityInEasyMode() {
        TestGameManager gameManager = new TestGameManager(Settings.Difficulty.EASY);
        Map<Class<? extends Block>, Integer> counts = generateBlocks(gameManager);
        verifyDistribution(counts, 1.2); // I블록 가중치 1.2배
    }

    @Test
    @DisplayName("NORMAL 난이도에서의 블록 생성 확률 테스트")
    void testBlockProbabilityInNormalMode() {
        TestGameManager gameManager = new TestGameManager(Settings.Difficulty.NORMAL);
        Map<Class<? extends Block>, Integer> counts = generateBlocks(gameManager);
        verifyDistribution(counts, 1.0); // I블록 기본 가중치
    }

    @Test
    @DisplayName("HARD 난이도에서의 블록 생성 확률 테스트")
    void testBlockProbabilityInHardMode() {
        TestGameManager gameManager = new TestGameManager(Settings.Difficulty.HARD);
        Map<Class<? extends Block>, Integer> counts = generateBlocks(gameManager);
        verifyDistribution(counts, 0.8); // I블록 가중치 0.8배
    }

    private Map<Class<? extends Block>, Integer> generateBlocks(TestGameManager gameManager) {
        Map<Class<? extends Block>, Integer> counts = new HashMap<>();
        
        // 모든 블록 타입 초기화
        counts.put(IBlock.class, 0);
        counts.put(JBlock.class, 0);
        counts.put(LBlock.class, 0);
        counts.put(OBlock.class, 0);
        counts.put(SBlock.class, 0);
        counts.put(TBlock.class, 0);
        counts.put(ZBlock.class, 0);
        
        // 블록 생성 및 카운트
        for (int i = 0; i < ITERATIONS; i++) {
            Block block = gameManager.makeRandomBlock();
            counts.merge(block.getClass(), 1, Integer::sum);
        }
        
        return counts;
    }

    private void verifyDistribution(Map<Class<? extends Block>, Integer> counts, double iBlockWeight) {
        int totalBlocks = ITERATIONS;
        double baseWeight = 10.0;
        double totalWeight = (baseWeight * 6) + (baseWeight * iBlockWeight); // 6개 일반 블록 + I블록
        
        // 예상 확률 계산
        double expectedIBlockProb = (baseWeight * iBlockWeight) / totalWeight;
        double expectedOtherBlockProb = baseWeight / totalWeight;
        
        // 허용 오차 범위 계산
        double allowedIBlockError = ITERATIONS * expectedIBlockProb * ALLOWED_ERROR;
        double allowedOtherBlockError = ITERATIONS * expectedOtherBlockProb * ALLOWED_ERROR;
        
        // I블록 검증
        int iBlockCount = counts.get(IBlock.class);
        double expectedIBlockCount = ITERATIONS * expectedIBlockProb;
        assertTrue(Math.abs(iBlockCount - expectedIBlockCount) <= allowedIBlockError,
                String.format("I블록 생성 횟수가 예상 범위를 벗어났습니다. 예상: %.2f±%.2f, 실제: %d", 
                        expectedIBlockCount, allowedIBlockError, iBlockCount));
        
        // 나머지 블록들 검증
        for (Map.Entry<Class<? extends Block>, Integer> entry : counts.entrySet()) {
            if (entry.getKey() != IBlock.class) {
                int count = entry.getValue();
                double expectedCount = ITERATIONS * expectedOtherBlockProb;
                assertTrue(Math.abs(count - expectedCount) <= allowedOtherBlockError,
                        String.format("%s 생성 횟수가 예상 범위를 벗어났습니다. 예상: %.2f±%.2f, 실제: %d",
                                entry.getKey().getSimpleName(), expectedCount, allowedOtherBlockError, count));
            }
        }
        
        // 전체 블록 수 검증
        assertEquals(ITERATIONS, counts.values().stream().mapToInt(Integer::intValue).sum(),
                "전체 블록 생성 횟수가 일치하지 않습니다.");
    }
}