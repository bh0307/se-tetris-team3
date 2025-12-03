package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import se.tetris.team3.ui.*;
import se.tetris.team3.ui.screen.GameScreen;
import se.tetris.team3.ui.screen.MenuScreen;
import se.tetris.team3.ui.screen.NameInputScreen;
import se.tetris.team3.core.*;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.blocks.Block;

import java.awt.event.KeyEvent;

@DisplayName("GameScreen 키 입력 동작 테스트")
class GameScreenKeyTest {
    AppFrame app;
    GameManager manager;
    GameScreen screen;

    @BeforeEach
    void setup() {
        app = new AppFrame();
        manager = new GameManager();
        manager.attachSettings(app.getSettings());
        screen = new GameScreen(app, manager);
        screen.onShow(); // timer 초기화
    }

    @Test
    @DisplayName("PAUSE: 일시정지 중 키 입력은 무시됨")
    void testKeyIgnoredWhenPaused() {
        KeyEvent pause = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.PAUSE), ' ');
        screen.onKeyPressed(pause); // 일시정지
        int beforeX = manager.getBlockX();
        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_LEFT), ' ');
        screen.onKeyPressed(left); // 이동 시도
        assertEquals(beforeX, manager.getBlockX(), "일시정지 중에는 이동 불가");
    }

    @Test
    @DisplayName("EXIT: ESC 키 입력 시 메뉴 복귀")
    void testExitKeyShowsMenuFromGame() {
        KeyEvent exit = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.EXIT), ' ');
        screen.onKeyPressed(exit);
        assertTrue(appScreenIsMenu(), "ESC 입력 시 메뉴 화면으로 복귀");
    }

    @Test
    @DisplayName("PAUSE: 일시정지 중 ESC 키 입력 시 메뉴 복귀")
    void testExitKeyShowsMenuWhenPaused() {
        KeyEvent pause = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.PAUSE), ' ');
        screen.onKeyPressed(pause); // 일시정지
        KeyEvent exit = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.EXIT), ' ');
        screen.onKeyPressed(exit);
        assertTrue(appScreenIsMenu(), "일시정지 중 ESC 입력 시 메뉴 화면으로 복귀");
    }

    @Test
    @DisplayName("GAME_OVER: 게임오버 상태에서 키 입력 시 화면 전환")
    void testGameOverKeyShowsScoreOrNameInputFromGameOver() {
        setGameOver(true);
        KeyEvent any = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_SPACE, ' ');
        screen.onKeyPressed(any);
        assertTrue(appScreenIsScoreOrNameInput(), "게임오버 상태에서 키 입력 시 화면 전환");
    }

    @Test
    @DisplayName("HARD_DROP: 이미 고정된 상태에서는 변화 없음")
    void testHardDropWhenFixed() {
        manager.resetGame();
        clearField();
        Block block = new se.tetris.team3.blocks.IBlock();
        setCurrentBlock(block);
        setNextBlock(block);
        manager.tryMove(5, 0);
        // 이미 바닥에 고정된 상태로 만듦
        setFieldRow(19, 1); // 바닥 전체 고정
        KeyEvent hardDrop = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.HARD_DROP), ' ');
        screen.onKeyPressed(hardDrop);
        // blockY가 변하지 않음
        assertEquals(0, manager.getBlockY(), "이미 고정된 상태에서는 변화 없음");
    }

    @Test
    @DisplayName("ROTATE: 벽 근처에서 킥 동작 정상")
    void testRotateNearWallKick() {
        clearField();
        Block block = new se.tetris.team3.blocks.IBlock();
        setCurrentBlock(block);
        setNextBlock(block);
        setBlockPosition(0, 0); // 좌측 벽 근처
        int[][] shapeBefore = deepCopy(block.getShape());
        KeyEvent rotate = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.ROTATE), ' ');
        screen.onKeyPressed(rotate);
        int[][] shapeAfter = manager.getCurrentBlock().getShape();
        assertFalse(arraysEqual(shapeBefore, shapeAfter), "벽 근처에서도 킥 동작으로 회전");
    }

    @Test
    @DisplayName("ROTATE: 장애물 근처에서는 회전 불가")
    void testRotateBlockedByObstacle() {
        clearField();
        Block block = new se.tetris.team3.blocks.IBlock();
        setCurrentBlock(block);
        setNextBlock(block);
        setBlockPosition(5, 0);
        setFieldRow(1, 1); // 1번째 줄에 장애물
        int[][] shapeBefore = deepCopy(block.getShape());
        KeyEvent rotate = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.ROTATE), ' ');
        screen.onKeyPressed(rotate);
        int[][] shapeAfter = manager.getCurrentBlock().getShape();
        assertTrue(arraysEqual(shapeBefore, shapeAfter), "장애물 근처에서는 회전 불가");
    }

    @Test
    @DisplayName("ROTATE: 중간에서는 정상 회전")
    void testRotateInMiddle() {
        clearField();
        Block block = new se.tetris.team3.blocks.IBlock();
        setCurrentBlock(block);
        setNextBlock(block);
        setBlockPosition(5, 5);
        int[][] shapeBefore = deepCopy(block.getShape());
        KeyEvent rotate = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.ROTATE), ' ');
        screen.onKeyPressed(rotate);
        int[][] shapeAfter = manager.getCurrentBlock().getShape();
        assertFalse(arraysEqual(shapeBefore, shapeAfter), "중간에서는 정상적으로 회전");
    }

    @Test
    @DisplayName("SOFT_DROP: 바닥에서는 이동 불가")
    void testSoftDropAtBottom() {
        Block block = manager.getCurrentBlock();
        int maxY = 20 - block.getShape().length;
        setBlockPosition(5, maxY); // 바닥
        KeyEvent drop = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.SOFT_DROP), ' ');
        screen.onKeyPressed(drop);
        assertEquals(maxY, manager.getBlockY(), "바닥에서는 더 하강 불가");
    }

    @Test
    @DisplayName("SOFT_DROP: 장애물 위에서는 이동 불가")
    void testSoftDropBlockedByObstacle() {
        setBlockPosition(5, 8);
        setFieldRow(10, 1); // 10번째 줄에 장애물
        KeyEvent drop = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.SOFT_DROP), ' ');
        screen.onKeyPressed(drop);
        // 장애물 위에 멈춤
        assertTrue(manager.getBlockY() < 10, "장애물 위에서는 더 하강 불가");
    }

    @Test
    @DisplayName("SOFT_DROP: 중간에서는 정상 하강")
    void testSoftDropInMiddle() {
        setBlockPosition(5, 5);
        KeyEvent drop = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.SOFT_DROP), ' ');
        screen.onKeyPressed(drop);
        assertEquals(6, manager.getBlockY(), "중간에서는 정상적으로 하강");
    }

    @Test
    @DisplayName("MOVE_LEFT: 좌측 끝에서는 이동 불가")
    void testMoveLeftAtLeftEdge() {
        setBlockPosition(0, 0); // 좌측 끝
        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_LEFT), ' ');
        screen.onKeyPressed(left);
        assertEquals(0, manager.getBlockX(), "좌측 끝에서는 더 이동 불가");
    }

    @Test
    @DisplayName("MOVE_RIGHT: 우측 끝에서는 이동 불가")
    void testMoveRightAtRightEdge() {
        Block block = manager.getCurrentBlock();
        int maxX = 10 - block.getShape()[0].length;
        setBlockPosition(maxX, 0); // 우측 끝
        KeyEvent right = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_RIGHT), ' ');
        screen.onKeyPressed(right);
        assertEquals(maxX, manager.getBlockX(), "우측 끝에서는 더 이동 불가");
    }

    @Test
    @DisplayName("MOVE_LEFT: 장애물 옆에서는 이동 불가")
    void testMoveLeftBlockedByObstacle() {
        setBlockPosition(2, 0);
        setFieldRow(0, 1); // 0번째 줄 전체 장애물
        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_LEFT), ' ');
        screen.onKeyPressed(left);
        // 장애물 때문에 이동 불가
        assertEquals(2, manager.getBlockX(), "장애물 옆에서는 이동 불가");
    }

    @Test
    @DisplayName("MOVE_RIGHT: 장애물 옆에서는 이동 불가")
    void testMoveRightBlockedByObstacle() {
        setBlockPosition(2, 0);
        setFieldRow(0, 1); // 0번째 줄 전체 장애물
        KeyEvent right = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_RIGHT), ' ');
        screen.onKeyPressed(right);
        // 장애물 때문에 이동 불가
        assertEquals(2, manager.getBlockX(), "장애물 옆에서는 이동 불가");
    }

    @Test
    @DisplayName("MOVE_LEFT: 중간에서는 정상 이동")
    void testMoveLeftInMiddle() {
        setBlockPosition(5, 0);
        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_LEFT), ' ');
        screen.onKeyPressed(left);
        assertEquals(4, manager.getBlockX(), "중간에서는 정상적으로 좌측 이동");
    }

    @Test
    @DisplayName("MOVE_RIGHT: 중간에서는 정상 이동")
    void testMoveRightInMiddle() {
        setBlockPosition(5, 0);
        KeyEvent right = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_RIGHT), ' ');
        screen.onKeyPressed(right);
        assertEquals(6, manager.getBlockX(), "중간에서는 정상적으로 우측 이동");
    }

    // nextBlock을 직접 세팅
    private void setNextBlock(Block b) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("nextBlock");
            f.setAccessible(true);
            f.set(manager, b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ...existing code...

    @Test
    @DisplayName("MOVE_LEFT 키 입력 시 블록 좌측 이동")
    void testMoveLeftKey() {
        setBlockPosition(5, 0);
        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_LEFT), ' ');
        screen.onKeyPressed(left);
        assertEquals(4, manager.getBlockX());
    }

    @Test
    @DisplayName("MOVE_RIGHT 키 입력 시 블록 우측 이동")
    void testMoveRightKey() {
        setBlockPosition(5, 0);
        KeyEvent right = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.MOVE_RIGHT), ' ');
        screen.onKeyPressed(right);
        assertEquals(6, manager.getBlockX());
    }

    @Test
    @DisplayName("SOFT_DROP 키 입력 시 블록 하강")
    void testSoftDropKey() {
        setBlockPosition(5, 0);
        KeyEvent drop = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.SOFT_DROP), ' ');
        screen.onKeyPressed(drop);
        assertEquals(1, manager.getBlockY());
    }

    // currentBlock을 직접 세팅
    private void setCurrentBlock(Block block) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("currentBlock");
            f.setAccessible(true);
            f.set(manager, block);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // 필드 전체를 0으로 초기화
    private void clearField() {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            int[][] field = (int[][]) f.get(manager);
            for (int r = 0; r < field.length; r++)
                for (int c = 0; c < field[r].length; c++)
                    field[r][c] = 0;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    // 특정 행에 장애물(블록) 추가
    private void setFieldRow(int row, int value) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            int[][] field = (int[][]) f.get(manager);
            for (int c = 0; c < field[row].length; c++)
                field[row][c] = value;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @DisplayName("ROTATE 키 입력 시 블록 회전")
    void testRotateKey() {
        clearField();
        Block block = new se.tetris.team3.blocks.IBlock();
        setCurrentBlock(block);
        setNextBlock(block);
        setBlockPosition(5, 0);
        int[][] shapeBefore = deepCopy(block.getShape());
        KeyEvent rotate = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.ROTATE), ' ');
        screen.onKeyPressed(rotate);
        int[][] shapeAfter = manager.getCurrentBlock().getShape();
        assertFalse(arraysEqual(shapeBefore, shapeAfter)); // 회전 결과가 다름
    }

    // shape 배열 deep copy
    private int[][] deepCopy(int[][] arr) {
        int[][] copy = new int[arr.length][];
        for (int i = 0; i < arr.length; i++) copy[i] = arr[i].clone();
        return copy;
    }
    // shape 배열 전체 비교
    private boolean arraysEqual(int[][] a, int[][] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i].length != b[i].length) return false;
            for (int j = 0; j < a[i].length; j++) {
                if (a[i][j] != b[i][j]) return false;
            }
        }
        return true;
    }

    @Test
    @DisplayName("PAUSE 키 입력 시 일시정지/재개")
    void testPauseKey() {
        KeyEvent pause = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.PAUSE), ' ');
        screen.onKeyPressed(pause);
        assertTrue(getIsPaused(screen));
        screen.onKeyPressed(pause);
        assertFalse(getIsPaused(screen));
    }

    @Test
    @DisplayName("EXIT 키 입력 시 메뉴 화면으로 복귀")
    void testExitKeyShowsMenu() {
        KeyEvent exit = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, app.getSettings().getKeymap().get(Settings.Action.EXIT), ' ');
        screen.onKeyPressed(exit);
        assertTrue(appScreenIsMenu());
    }

    @Test
    @DisplayName("게임 오버 상태에서 키 입력 시 화면 전환")
    void testGameOverKeyShowsScoreOrNameInput() {
        setGameOver(true);
        KeyEvent any = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_SPACE, ' ');
        screen.onKeyPressed(any);
        assertTrue(appScreenIsScoreOrNameInput());
    }

    // 블록 위치 설정 헬퍼
    private void setBlockPosition(int x, int y) {
        try {
            java.lang.reflect.Field fx = GameManager.class.getDeclaredField("blockX");
            java.lang.reflect.Field fy = GameManager.class.getDeclaredField("blockY");
            fx.setAccessible(true); fy.setAccessible(true);
            fx.setInt(manager, x); fy.setInt(manager, y);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    // 게임 오버 상태 설정
    private void setGameOver(boolean value) {
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("isGameOver");
            f.setAccessible(true);
            f.setBoolean(manager, value);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    // 일시정지 상태 확인
    private boolean getIsPaused(GameScreen s) {
        try {
            java.lang.reflect.Field f = GameScreen.class.getDeclaredField("isPaused");
            f.setAccessible(true);
            return f.getBoolean(s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    // 메뉴 화면 전환 확인
    private boolean appScreenIsMenu() {
        try {
            java.lang.reflect.Field f = AppFrame.class.getDeclaredField("current");
            f.setAccessible(true);
            Object current = f.get(app);
            return current instanceof MenuScreen;
        } catch (Exception e) { return false; }
    }
    // 게임 오버 시 화면 전환 확인
    private boolean appScreenIsScoreOrNameInput() {
        try {
            java.lang.reflect.Field f = AppFrame.class.getDeclaredField("current");
            f.setAccessible(true);
            Object current = f.get(app);
            return current instanceof se.tetris.team3.ui.screen.ScoreboardScreen || current instanceof NameInputScreen;
        } catch (Exception e) { return false; }
    }
}
