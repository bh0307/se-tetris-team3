package se.tetris.team3.gameManager;

import org.junit.jupiter.api.Test;
import se.tetris.team3.core.GameMode;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerRenderTest {
        @Test
        void testRenderHUDAllBranches() throws Exception {
            GameManager gm = new GameManager(GameMode.ITEM);
            BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();

            // 난이도별 분기
            for (se.tetris.team3.core.Settings.Difficulty diff : se.tetris.team3.core.Settings.Difficulty.values()) {
                se.tetris.team3.core.Settings settings = new se.tetris.team3.core.Settings();
                java.lang.reflect.Field diffField = se.tetris.team3.core.Settings.class.getDeclaredField("difficulty");
                diffField.setAccessible(true);
                diffField.set(settings, diff);
                gm.attachSettings(settings);
                gm.renderHUD(g2, 10, 20, 300);
            }

            // nextBlock에 아이템 없는 경우
            java.lang.reflect.Field nextBlockField = GameManager.class.getDeclaredField("nextBlock");
            nextBlockField.setAccessible(true);
            nextBlockField.set(gm, new se.tetris.team3.blocks.OBlock());
            gm.renderHUD(g2, 10, 20, 300);

            // nextBlock에 아이템 있는 경우 (L 아이템)
            se.tetris.team3.blocks.TBlock block = new se.tetris.team3.blocks.TBlock();
            block.setItemType('L');
            block.getClass().getMethod("setItemCell", int.class, int.class).invoke(block, 0, 0);
            nextBlockField.set(gm, block);
            gm.renderHUD(g2, 10, 20, 300);

            // slowModeActive 분기
            java.lang.reflect.Field slowField = GameManager.class.getDeclaredField("slowModeActive");
            slowField.setAccessible(true);
            slowField.set(gm, true);
            java.lang.reflect.Field slowEndField = GameManager.class.getDeclaredField("slowModeEndTime");
            slowEndField.setAccessible(true);
            slowEndField.set(gm, System.currentTimeMillis() + 5000);
            gm.renderHUD(g2, 10, 20, 300);
            slowField.set(gm, false);

            // I-only 모드 분기
            java.lang.reflect.Field iOnlyField = GameManager.class.getDeclaredField("iOnlyModeActive");
            iOnlyField.setAccessible(true);
            iOnlyField.set(gm, true);
            java.lang.reflect.Field iOnlyEndField = GameManager.class.getDeclaredField("iOnlyModeEndMillis");
            iOnlyEndField.setAccessible(true);
            iOnlyEndField.set(gm, System.currentTimeMillis() + 5000);
            gm.renderHUD(g2, 10, 20, 300);
            iOnlyField.set(gm, false);

            // doubleScoreActive 분기
            java.lang.reflect.Field doubleField = GameManager.class.getDeclaredField("doubleScoreActive");
            doubleField.setAccessible(true);
            doubleField.set(gm, true);
            java.lang.reflect.Field doubleEndField = GameManager.class.getDeclaredField("doubleScoreTime");
            doubleEndField.setAccessible(true);
            doubleEndField.set(gm, System.currentTimeMillis() + 5000);
            gm.renderHUD(g2, 10, 20, 300);
            doubleField.set(gm, false);
        }
        
    @Test
    void testRenderHUDDoesNotThrow() {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        // HUD 렌더링이 예외 없이 동작하는지 확인
        assertDoesNotThrow(() -> gm.renderHUD(g2, 10, 20, 200));
    }

                    @Test
                    void testRenderHUDPixelVerification() throws Exception {
                        GameManager gm = new GameManager(GameMode.ITEM);
                        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = img.createGraphics();

                        // 점수/레벨/난이도 텍스트 렌더링 확인 (HUD 영역)
                        java.lang.reflect.Field scoreField = GameManager.class.getDeclaredField("score");
                        scoreField.setAccessible(true);
                        scoreField.setInt(gm, 12345);
                        java.lang.reflect.Field levelField = GameManager.class.getDeclaredField("level");
                        levelField.setAccessible(true);
                        levelField.setInt(gm, 7);
                        java.lang.reflect.Field diffField = GameManager.class.getDeclaredField("difficulty");
                        diffField.setAccessible(true);
                        diffField.set(gm, se.tetris.team3.core.Settings.Difficulty.HARD);
                        gm.renderHUD(g2, 10, 20, 300);
                        int blockSize = 20;
                        int padding = 10;
                        int hudX = padding + blockSize * 10 + 16;
                        int baseFontSize = Math.max(16, Math.min(28, Math.max(14, blockSize / 2)));
                        int lineSpacing = (int)(baseFontSize * 1.5);
                        int scoreY = padding + lineSpacing;
                        int hudWidth = Math.max(120, 300 - hudX - padding);
                        // HUD 영역 내 여러 픽셀을 스캔하여 텍스트가 실제로 렌더링된 부분(흰색 또는 밝은 색상)이 있는지 확인
                        boolean foundScoreText = false;
                        for (int y = scoreY - 2; y <= scoreY + 2; y++) {
                            for (int x = hudX; x < hudX + hudWidth; x += 2) {
                                int color = img.getRGB(x, y);
                                Color c = new Color(color, true);
                                if ((c.getRed() > 30 || c.getGreen() > 30 || c.getBlue() > 30) && c.getAlpha() > 0) {
                                    foundScoreText = true;
                                    break;
                                }
                            }
                            if (foundScoreText) break;
                        }
                        assertTrue(foundScoreText, "HUD score text should render visible pixel");

                        // 슬로우 모드 HUD에 빨간색 텍스트 렌더링 확인
                        java.lang.reflect.Field slowField = GameManager.class.getDeclaredField("slowModeActive");
                        slowField.setAccessible(true);
                        slowField.set(gm, true);
                        java.lang.reflect.Field slowEndField = GameManager.class.getDeclaredField("slowModeEndTime");
                        slowEndField.setAccessible(true);
                        slowEndField.set(gm, System.currentTimeMillis() + 5000);
                        gm.renderHUD(g2, 10, 20, 300);
                        int slowY = scoreY + 200;
                        boolean foundSlowText = false;
                        for (int y = slowY - 2; y <= slowY + 2; y++) {
                            for (int x = hudX; x < hudX + hudWidth; x += 2) {
                                int color = img.getRGB(x, y);
                                Color c = new Color(color, true);
                                if (c.getRed() > 150 && c.getGreen() < 100 && c.getBlue() < 100 && c.getAlpha() > 0) {
                                    foundSlowText = true;
                                    break;
                                }
                            }
                            if (foundSlowText) break;
                        }
                        assertTrue(foundSlowText, "SLOW mode text should be reddish");
                        slowField.set(gm, false);

                        // I-only 모드 HUD에 녹색 텍스트 렌더링 확인
                        java.lang.reflect.Field iOnlyField = GameManager.class.getDeclaredField("iOnlyModeActive");
                        iOnlyField.setAccessible(true);
                        iOnlyField.set(gm, true);
                        java.lang.reflect.Field iOnlyEndField = GameManager.class.getDeclaredField("iOnlyModeEndMillis");
                        iOnlyEndField.setAccessible(true);
                        iOnlyEndField.set(gm, System.currentTimeMillis() + 5000);
                        gm.renderHUD(g2, 10, 20, 300);
                        int iOnlyY = slowField.getBoolean(gm) ? scoreY + 230 : scoreY + 200;
                        boolean foundIOnlyText = false;
                        for (int y = iOnlyY - 2; y <= iOnlyY + 2; y++) {
                            for (int x = hudX; x < hudX + hudWidth; x += 2) {
                                int color = img.getRGB(x, y);
                                Color c = new Color(color, true);
                                if (c.getGreen() > 150 && c.getRed() < 100 && c.getBlue() < 100 && c.getAlpha() > 0) {
                                    foundIOnlyText = true;
                                    break;
                                }
                            }
                            if (foundIOnlyText) break;
                        }
                        assertTrue(foundIOnlyText, "I-only mode text should be greenish");
                        iOnlyField.set(gm, false);

                        // 2배 점수 모드 HUD에 파란색 텍스트 렌더링 확인
                        java.lang.reflect.Field doubleField = GameManager.class.getDeclaredField("doubleScoreActive");
                        doubleField.setAccessible(true);
                        doubleField.set(gm, true);
                        java.lang.reflect.Field doubleEndField = GameManager.class.getDeclaredField("doubleScoreTime");
                        doubleEndField.setAccessible(true);
                        doubleEndField.set(gm, System.currentTimeMillis() + 5000);
                        gm.renderHUD(g2, 10, 20, 300);
                        int doubleY = scoreY + 200;
                        if (slowField.getBoolean(gm)) doubleY += 30;
                        if (iOnlyField.getBoolean(gm)) doubleY += 30;
                        boolean foundDoubleText = false;
                        for (int y = doubleY - 2; y <= doubleY + 2; y++) {
                            for (int x = hudX; x < hudX + hudWidth; x += 2) {
                                int color = img.getRGB(x, y);
                                Color c = new Color(color, true);
                                if (c.getBlue() > 150 && c.getRed() < 100 && c.getGreen() < 100 && c.getAlpha() > 0) {
                                    foundDoubleText = true;
                                    break;
                                }
                            }
                            if (foundDoubleText) break;
                        }
                        assertTrue(foundDoubleText, "Double score mode text should be bluish");
                        doubleField.set(gm, false);
                    }
    @Test
    void testDrawStringEllipsisRendersText() throws Exception {
        GameManager gm = new GameManager(GameMode.CLASSIC);
        BufferedImage img = new BufferedImage(100, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        // drawStringEllipsis가 예외 없이 동작하는지 확인
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method m = GameManager.class.getDeclaredMethod("drawStringEllipsis", Graphics2D.class, String.class, int.class, int.class, int.class);
            m.setAccessible(true);
            m.invoke(gm, g2, "테스트 문자열", 0, 20, 80);
        });
    }
}
