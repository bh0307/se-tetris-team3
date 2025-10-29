package se.tetris.team3.itemTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import se.tetris.team3.core.GameMode;
import se.tetris.team3.ui.GameManager;
import se.tetris.team3.blocks.OBlock;

import java.lang.reflect.Field;

/**
 * Line Clear 아이템 테스트
 * - 줄삭제 아이템이 붙은 블록을 고정(또는 applyLineClearItem 직접 호출)하면 해당 행이 삭제되고 점수가 증가하는지 검증
 */
public class LineClearItemTest {

    private GameManager manager;

    @BeforeEach
    public void setUp() {
        manager = new GameManager(GameMode.ITEM);
    }

    @Test
    public void testApplyLineClearRemovesFullRowAndAwardsScore() throws Exception {
        // 내부 필드에 접근하여 특정 행을 모두 채워 '가득 찬 행'으로 만듦
        Field fieldF = GameManager.class.getDeclaredField("field");
        fieldF.setAccessible(true);
        int[][] field = (int[][]) fieldF.get(manager);

        final int targetRow = 15; // 테스트용 행
        for (int c = 0; c < field[targetRow].length; c++) field[targetRow][c] = 1;

        // 현재 점수 저장
        int beforeScore = manager.getScore();

        // O 블록을 만들고, 아이템(L)을 붙여서 currentBlock으로 설정
        OBlock b = new OBlock();
        b.setItemType('L');
        // 블록 내부의 아이템 위치를 맨 위 왼쪽(0,0)로 설정
        b.setItemCell(0, 0);

        // currentBlock과 blockY를 리플렉션으로 설정하여 itemRow가 targetRow를 가리키게 함
        Field currF = GameManager.class.getDeclaredField("currentBlock");
        currF.setAccessible(true);
        currF.set(manager, b);

        Field blockYF = GameManager.class.getDeclaredField("blockY");
        blockYF.setAccessible(true);
        blockYF.setInt(manager, targetRow); // itemRow 0이므로 blockY == targetRow

        // applyLineClearItem 호출 (public)
        manager.applyLineClearItem();

        // 해당 행이 모두 0으로 비워졌는지 확인
        for (int c = 0; c < field[targetRow].length; c++) {
            assertEquals(0, manager.getFieldValue(targetRow, c), "줄삭제 후 해당 칸은 0이어야 함 (col=" + c + ")");
        }

        // 점수는 100 증가했어야 함 (getScoreWithMultiplier 기반)
        int afterScore = manager.getScore();
        assertEquals(beforeScore + 100, afterScore, "줄삭제 아이템 적용 시 점수가 100 증가해야 함");
    }
}
