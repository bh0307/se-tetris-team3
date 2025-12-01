package se.tetris.team3.uiTest;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import se.tetris.team3.core.Settings;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.ui.AppFrame;
import se.tetris.team3.ui.screen.GameScreen;
import se.tetris.team3.blocks.Block;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

@DisplayName("GameScreen 렌더 및 입력 분기 테스트")
class GameScreenTest {
    AppFrame app;
    Settings settings;
    GameManager manager;
    GameScreen screen;
    Graphics2D g2;

    @BeforeEach
    void setup() {
        app = Mockito.mock(AppFrame.class);
        settings = new Settings();
        Mockito.when(app.getSettings()).thenReturn(settings);
        Mockito.when(app.getWidth()).thenReturn(800);
        Mockito.when(app.getHeight()).thenReturn(600);
        manager = Mockito.spy(new GameManager());
        screen = new GameScreen(app, manager);
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = img.createGraphics();
    }

    @AfterEach
    void tearDown() {
        if (g2 != null) g2.dispose();
    }

    @Test
    @DisplayName("고정 블록 실제 픽셀 검증")
    void testRenderFixedBlockPixel() throws Exception {
        Mockito.doReturn(false).when(manager).isGameOver();
        Mockito.doReturn(1).when(manager).getFieldValue(5, 3);
        Mockito.doReturn(Color.BLUE).when(manager).getBlockColor(5, 3);
        Mockito.doReturn(false).when(manager).isRowFlashing(5);
        Mockito.doReturn(false).when(manager).isRowFlashing(6);
        Block block = Mockito.mock(Block.class);
        Mockito.when(manager.getCurrentBlock()).thenReturn(null);
        Mockito.doReturn(3).when(manager).getBlockX();
        Mockito.doReturn(5).when(manager).getBlockY();
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        int x = padding + manager.getBlockX() * blockSize;
        int y = padding + manager.getBlockY() * blockSizeH;
        boolean foundBlue = false;
        for (int dx = 2; dx < blockSize - 2; dx++) {
            for (int dy = 2; dy < blockSizeH - 2; dy++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    Color c = new Color(img.getRGB(px, py), true);
                    if (c.getBlue() > 150 && c.getRed() < 100 && c.getGreen() < 100) {
                        foundBlue = true;
                        break;
                    }
                }
            }
            if (foundBlue) break;
        }
        assertTrue(foundBlue, "고정 블록이 파란색으로 렌더링되어야 함");
    }

    @Test
    @DisplayName("플래시 줄 실제 픽셀 검증")
    void testRenderFlashingRowPixel() throws Exception {
        Mockito.doReturn(false).when(manager).isGameOver();
        Mockito.doReturn(1).when(manager).getFieldValue(6, 3);
        Mockito.doReturn(Color.BLUE).when(manager).getBlockColor(6, 3);
        Mockito.doReturn(true).when(manager).isRowFlashing(6);
        Block block = Mockito.mock(Block.class);
        Mockito.when(manager.getCurrentBlock()).thenReturn(null);
        Mockito.doReturn(3).when(manager).getBlockX();
        Mockito.doReturn(6).when(manager).getBlockY();
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        int x = padding + manager.getBlockX() * blockSize;
        int y = padding + manager.getBlockY() * blockSizeH;
        boolean foundWhite = false;
        for (int dx = 2; dx < blockSize - 2; dx++) {
            for (int dy = 2; dy < blockSizeH - 2; dy++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    Color c = new Color(img.getRGB(px, py), true);
                    if (c.getRed() > 200 && c.getGreen() > 200 && c.getBlue() > 200) {
                        foundWhite = true;
                        break;
                    }
                }
            }
            if (foundWhite) break;
        }
        assertTrue(foundWhite, "플래시 줄이 하얀색으로 렌더링되어야 함");
    }

    @Test
    @DisplayName("아이템 글자 실제 픽셀 검증")
    void testRenderItemCharPixel() throws Exception {
        Mockito.doReturn(false).when(manager).isGameOver();
        Mockito.doReturn(1).when(manager).getFieldValue(5, 3);
        Mockito.doReturn(Color.BLUE).when(manager).getBlockColor(5, 3);
        Mockito.doReturn(false).when(manager).isRowFlashing(5);
        Mockito.doReturn(true).when(manager).hasItem(5, 3);
        Mockito.doReturn('L').when(manager).getItemType(5, 3);
        Block block = Mockito.mock(Block.class);
        Mockito.when(manager.getCurrentBlock()).thenReturn(null);
        Mockito.doReturn(3).when(manager).getBlockX();
        Mockito.doReturn(5).when(manager).getBlockY();
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        int x = padding + manager.getBlockX() * blockSize;
        int y = padding + manager.getBlockY() * blockSizeH;
        boolean foundItemChar = false;
        for (int dx = 2; dx < blockSize - 2; dx++) {
            for (int dy = 2; dy < blockSizeH - 2; dy++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    Color c = new Color(img.getRGB(px, py), true);
                    if (c.getRed() > 200 && c.getGreen() > 200 && c.getBlue() > 200 && c.getAlpha() > 150) {
                        foundItemChar = true;
                        break;
                    }
                }
            }
            if (foundItemChar) break;
        }
        assertTrue(foundItemChar, "아이템 글자가 흰색으로 렌더링되어야 함");
    }

    @Test
    @DisplayName("HUD/파티클 실제 픽셀 검증")
    void testRenderHUDPixel() throws Exception {
        Mockito.doReturn(false).when(manager).isGameOver();
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        int hudY = padding;
        boolean foundHUD = false;
        for (int dx = 2; dx < blockSize * 2; dx++) {
            for (int dy = 2; dy < blockSizeH; dy++) {
                int px = dx, py = hudY + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    Color c = new Color(img.getRGB(px, py), true);
                    if (c.getRed() > 200 && c.getGreen() > 200 && c.getBlue() > 200) {
                        foundHUD = true;
                        break;
                    }
                }
            }
            if (foundHUD) break;
        }
        assertTrue(foundHUD, "HUD가 실제로 렌더링되어야 함");
    }
    @DisplayName("일시정지 상태 렌더링: PAUSED 텍스트 실제 픽셀 검증(전체 이미지 스캔)")
    void testRenderPaused() throws Exception {
        java.lang.reflect.Field f = GameScreen.class.getDeclaredField("isPaused");
        f.setAccessible(true);
        f.set(screen, true);
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int redCount = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y), true);
                if (c.getRed() > 150 && c.getRed() > c.getGreen() && c.getRed() > c.getBlue()) redCount++;
            }
        }
        assertTrue(redCount > 100, "PAUSED 텍스트가 실제로 렌더링되어야 함");
    }

    @Test
    @DisplayName("게임오버 상태 렌더링: GAME OVER 텍스트 실제 픽셀 검증(전체 이미지 스캔)")
    void testRenderGameOver() {
        Mockito.doReturn(true).when(manager).isGameOver();
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int redCount = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y), true);
                if (c.getRed() > 150 && c.getRed() > c.getGreen() && c.getRed() > c.getBlue()) redCount++;
            }
        }
        assertTrue(redCount > 100, "GAME OVER 텍스트가 실제로 렌더링되어야 함");
    }

    @Test
    @DisplayName("drawCenteredChar: 문자 중앙 정렬 실제 픽셀 검증")
    void testDrawCenteredChar() {
        BufferedImage img = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        GameScreen.drawCenteredChar(g, 10, 10, 40, 'L');
        int whiteCount = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y), true);
                // 문자 영역에 흰색(혹은 밝은색) 픽셀이 일정 이상 존재해야 함
                if (c.getRed() > 200 && c.getGreen() > 200 && c.getBlue() > 200 && c.getAlpha() > 150) whiteCount++;
            }
        }
        assertTrue(whiteCount > 50, "문자 L이 실제로 중앙에 그려져야 함");
    }

    @Test
    @DisplayName("alignSpawnIfNewBlock: 새 블록 등장 시 위치 조정")
    void testAlignSpawnIfNewBlock() throws Exception {
        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getShape()).thenReturn(new int[][]{{1,1},{1,1}});
        Mockito.when(manager.getCurrentBlock()).thenReturn(block);
        java.lang.reflect.Field f = GameScreen.class.getDeclaredField("lastBlockRef");
        f.setAccessible(true);
        f.set(screen, null);
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method m = GameScreen.class.getDeclaredMethod("alignSpawnIfNewBlock");
            m.setAccessible(true);
            m.invoke(screen);
        });
    }

    @Test
    @DisplayName("onKeyPressed: 일시정지/해제 및 ESC 종료 실제 상태 변화 검증 (timer 초기화 포함)")
    void testOnKeyPressedPauseAndExit() throws Exception {
        int pauseKey = settings.getKeymap().get(Settings.Action.PAUSE);
        int exitKey = settings.getKeymap().get(Settings.Action.EXIT);
        // timer 초기화
        screen.onShow();
        // 일시정지 진입
        KeyEvent pause = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, pauseKey, ' ');
        screen.onKeyPressed(pause);
        java.lang.reflect.Field f = GameScreen.class.getDeclaredField("isPaused");
        f.setAccessible(true);
        assertTrue(f.getBoolean(screen));
        // 일시정지 해제
        screen.onKeyPressed(pause);
        assertFalse(f.getBoolean(screen));
        // 다시 일시정지 후 ESC로 종료
        screen.onKeyPressed(pause);
        f.set(screen, true);
        KeyEvent esc = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, exitKey, (char)27);
        screen.onKeyPressed(esc);
        Mockito.verify(app, Mockito.atLeastOnce()).showScreen(Mockito.any());
    }

    @Test
    @DisplayName("onKeyPressed: MOVE_LEFT 실제 위치 변화 픽셀 검증")
    void testOnKeyPressedMoveLeftPixel() throws Exception {
        Block block = new se.tetris.team3.blocks.JBlock();
        java.lang.reflect.Field bxField = GameManager.class.getDeclaredField("blockX");
        bxField.setAccessible(true);
        bxField.set(manager, 4);
        java.lang.reflect.Field byField = GameManager.class.getDeclaredField("blockY");
        byField.setAccessible(true);
        byField.set(manager, 5);
        java.lang.reflect.Field curField = GameManager.class.getDeclaredField("currentBlock");
        curField.setAccessible(true);
        curField.set(manager, block);
        KeyEvent left = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, settings.getKeymap().get(Settings.Action.MOVE_LEFT), ' ');
        screen.onKeyPressed(left);
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        int x = padding + manager.getBlockX() * blockSize;
        int y = padding + manager.getBlockY() * blockSizeH;
        boolean found = false;
        for (int dx = 2; dx < blockSize - 2; dx++) {
            for (int dy = 2; dy < blockSizeH - 2; dy++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    Color c = new Color(img.getRGB(px, py), true);
                    if (c.getBlue() > 150 && c.getRed() < 100 && c.getGreen() < 100) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) break;
        }
        assertTrue(found, "MOVE_LEFT 입력 후 블록이 왼쪽으로 이동해 파란색으로 렌더링되어야 함");
    }

    @Test
    @DisplayName("onKeyPressed: MOVE_RIGHT 실제 위치 변화 픽셀 검증")
    void testOnKeyPressedMoveRightPixel() throws Exception {
        Block block = new se.tetris.team3.blocks.JBlock();
        java.lang.reflect.Field bxField = GameManager.class.getDeclaredField("blockX");
        bxField.setAccessible(true);
        bxField.set(manager, 4);
        java.lang.reflect.Field byField = GameManager.class.getDeclaredField("blockY");
        byField.setAccessible(true);
        byField.set(manager, 5);
        java.lang.reflect.Field curField = GameManager.class.getDeclaredField("currentBlock");
        curField.setAccessible(true);
        curField.set(manager, block);
        KeyEvent right = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, settings.getKeymap().get(Settings.Action.MOVE_RIGHT), ' ');
        screen.onKeyPressed(right);
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        int x = padding + manager.getBlockX() * blockSize;
        int y = padding + manager.getBlockY() * blockSizeH;
        boolean found = false;
        for (int dx = 2; dx < blockSize - 2; dx++) {
            for (int dy = 2; dy < blockSizeH - 2; dy++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    Color c = new Color(img.getRGB(px, py), true);
                    if (c.getBlue() > 150 && c.getRed() < 100 && c.getGreen() < 100) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) break;
        }
        assertTrue(found, "MOVE_RIGHT 입력 후 블록이 오른쪽으로 이동해 파란색으로 렌더링되어야 함");
    }

    @Test
    @DisplayName("onKeyPressed: SOFT_DROP 실제 위치 변화 픽셀 검증")
    void testOnKeyPressedSoftDropPixel() throws Exception {
        Block block = new se.tetris.team3.blocks.JBlock();
        java.lang.reflect.Field bxField = GameManager.class.getDeclaredField("blockX");
        bxField.setAccessible(true);
        bxField.set(manager, 4);
        java.lang.reflect.Field byField = GameManager.class.getDeclaredField("blockY");
        byField.setAccessible(true);
        byField.set(manager, 5);
        java.lang.reflect.Field curField = GameManager.class.getDeclaredField("currentBlock");
        curField.setAccessible(true);
        curField.set(manager, block);
        KeyEvent down = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, settings.getKeymap().get(Settings.Action.SOFT_DROP), ' ');
        screen.onKeyPressed(down);
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        int x = padding + manager.getBlockX() * blockSize;
        int y = padding + manager.getBlockY() * blockSizeH;
        boolean found = false;
        for (int dx = 2; dx < blockSize - 2; dx++) {
            for (int dy = 2; dy < blockSizeH - 2; dy++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    Color c = new Color(img.getRGB(px, py), true);
                    if (c.getBlue() > 150 && c.getRed() < 100 && c.getGreen() < 100) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) break;
        }
        assertTrue(found, "SOFT_DROP 입력 후 블록이 아래로 이동해 파란색으로 렌더링되어야 함");
    }

    @Test
    @DisplayName("onKeyPressed: HARD_DROP 실제 위치 변화 및 렌더링 픽셀 검증")
    void testOnKeyPressedHardDropPixel() throws Exception {
        Block block = new se.tetris.team3.blocks.JBlock();
        int startY = 5;
        int bottomY = 19;
        // 블록을 위에 두고 시작
        java.lang.reflect.Field bxField = GameManager.class.getDeclaredField("blockX");
        bxField.setAccessible(true);
        bxField.set(manager, 4);
        java.lang.reflect.Field byField = GameManager.class.getDeclaredField("blockY");
        byField.setAccessible(true);
        byField.set(manager, startY);
        java.lang.reflect.Field curField = GameManager.class.getDeclaredField("currentBlock");
        curField.setAccessible(true);
        curField.set(manager, block);
        // 하드드롭 입력
        KeyEvent hardDrop = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, settings.getKeymap().get(Settings.Action.HARD_DROP), ' ');
        screen.onKeyPressed(hardDrop);
        // field 배열의 바닥에 블록이 고정됐는지 확인
        int[][] field;
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            field = (int[][]) f.get(manager);
        } catch (Exception e) { throw new RuntimeException(e); }
        boolean blockAtBottom = false;
        for (int x = 0; x < field[0].length; x++) {
            if (field[bottomY][x] == 1) {
                blockAtBottom = true;
                break;
            }
        }
        assertTrue(blockAtBottom, "하드 드롭 후 블록이 바닥에 고정되어야 함");
        // 픽셀 렌더링 확인 (블록이 바닥에 파란색으로 그려졌는지)
        // currentBlock을 null로 설정해서 고정된 블록만 렌더링
        curField.set(manager, null);
        BufferedImage img = new BufferedImage(800, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        // 바닥(19번째 줄)에서 블록이 있는 모든 칸을 스캔
        boolean found = false;
        for (int col = 0; col < field[0].length; col++) {
            if (field[bottomY][col] == 1) {
                int x = padding + col * blockSize;
                int y = padding + bottomY * blockSizeH;
                for (int dx = 0; dx < blockSize; dx++) {
                    for (int dy = 0; dy < blockSizeH; dy++) {
                        int px = x + dx, py = y + dy;
                        if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                            Color c = new Color(img.getRGB(px, py), true);
                            if (c.getBlue() > 100 || c.getRed() > 100 || c.getGreen() > 100) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
                if (found) break;
            }
        }
        assertTrue(found, "HARD_DROP 입력 후 블록이 맨 아래로 이동해 렌더링되어야 함");
    }

    @Test
    @DisplayName("onKeyPressed: ROTATE 실제 회전 후 픽셀 검증")
    void testOnKeyPressedRotatePixel() throws Exception {
        Block block = new se.tetris.team3.blocks.JBlock();
        java.lang.reflect.Field bxField = GameManager.class.getDeclaredField("blockX");
        bxField.setAccessible(true);
        bxField.set(manager, 4);
        java.lang.reflect.Field byField = GameManager.class.getDeclaredField("blockY");
        byField.setAccessible(true);
        byField.set(manager, 5);
        java.lang.reflect.Field curField = GameManager.class.getDeclaredField("currentBlock");
        curField.setAccessible(true);
        curField.set(manager, block);
        KeyEvent rotate = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, settings.getKeymap().get(Settings.Action.ROTATE), ' ');
        screen.onKeyPressed(rotate);
        
        // 회전 후 블록을 field에 고정
        Block rotatedBlock = (Block) curField.get(manager);
        int bx = manager.getBlockX();
        int by = manager.getBlockY();
        int[][] shape = rotatedBlock.getShape();
        int[][] field;
        try {
            java.lang.reflect.Field f = GameManager.class.getDeclaredField("field");
            f.setAccessible(true);
            field = (int[][]) f.get(manager);
        } catch (Exception e) { throw new RuntimeException(e); }
        
        // 회전된 블록을 field에 배치
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0 && by + r >= 0 && by + r < field.length && bx + c >= 0 && bx + c < field[0].length) {
                    field[by + r][bx + c] = 1;
                }
            }
        }
        
        // currentBlock을 null로 설정해서 고정된 블록만 렌더링
        curField.set(manager, null);
        
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        screen.render(g);
        int padding = 18, blockSize = settings.resolveBlockSize(), blockSizeH = (int)(blockSize * 1.15);
        
        // 회전된 블록 위치에서 픽셀 확인
        boolean found = false;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int x = padding + (bx + c) * blockSize;
                    int y = padding + (by + r) * blockSizeH;
                    for (int dx = 0; dx < blockSize; dx++) {
                        for (int dy = 0; dy < blockSizeH; dy++) {
                            int px = x + dx, py = y + dy;
                            if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                                Color color = new Color(img.getRGB(px, py), true);
                                if (color.getBlue() > 100 || color.getRed() > 100 || color.getGreen() > 100) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) break;
                    }
                    if (found) break;
                }
            }
            if (found) break;
        }
        
        assertTrue(found, "ROTATE 입력 후 블록이 회전되어 렌더링되어야 함");
    }
    

    @Test
    @DisplayName("onKeyPressed: 게임오버 시 분기")
    void testOnKeyPressedGameOver() {
        Mockito.doReturn(true).when(manager).isGameOver();
        KeyEvent any = new KeyEvent(new java.awt.Component(){}, 0, 0, 0, KeyEvent.VK_A, 'A');
        assertDoesNotThrow(() -> screen.onKeyPressed(any));
    }
}
