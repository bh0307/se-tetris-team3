package se.tetris.team3.ui.render;

import se.tetris.team3.blocks.Block;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** 
 * 블록 셀 패턴 렌더러
 * - 다양한 호출 형태(오버로드) 지원
 * - 클래스패스/파일시스템 양쪽에서 텍스처 탐색
 * - TexturePaint 캐시 + (0,0) 앵커로 패턴 흔들림 방지
 */
public final class PatternPainter {

    private static final Map<String, TexturePaint> CACHE = new HashMap<>();

    private PatternPainter() {}

    /* ========== 공개 API (여러 오버로드) ========== */

    /** 
     * 가장 흔한 형태: 색맹모드면 패턴, 아니면 기본색으로 채움.
     * block이 null이어도 안전.
     */
    public static void drawCell(Graphics2D g2, int x, int y, int size,
                                Color baseColor, Block block, boolean colorBlindMode) {
        drawCell(g2, x, y, size, baseColor, block, colorBlindMode, 160);
    }

    // alpha 파라미터 추가 버전
    public static void drawCell(Graphics2D g2, int x, int y, int size,
                                Color baseColor, Block block, boolean colorBlindMode, int patternAlpha) {
        if (!colorBlindMode) {
            fillRounded(g2, x, y, size, baseColor);
            return;
        }
        TexturePaint tp = hatchForKeyWithAlpha(block != null ? block.getClass().getSimpleName() : "default", patternAlpha);
        fillRoundedWithTexture(g2, x, y, size, tp, baseColor.darker());
    }

    /** 가로/세로 크기가 다른 직사각형 셀 그리기 */
    public static void drawCellRect(Graphics2D g2, int x, int y, int width, int height,
                                    Color baseColor, Block block, boolean colorBlindMode) {
        if (!colorBlindMode) {
            fillRoundedRect(g2, x, y, width, height, baseColor);
            return;
        }
        TexturePaint tp = hatchForKeyWithAlpha(block != null ? block.getClass().getSimpleName() : "default", 255);
        fillRoundedRectWithTexture(g2, x, y, width, height, tp, baseColor.darker());
    }

    /** block이 없이 부르는 코드와도 호환 (임의 공용 패턴 지정) */
    public static void drawCell(Graphics2D g2, int x, int y, int size,
                                Color baseColor, boolean colorBlindMode) {
        drawCell(g2, x, y, size, baseColor, (Block) null, colorBlindMode);
    }

    /** 예전 코드 호환: 패턴만 그리고 싶은 경우 */
    public static void drawPattern(Graphics2D g2, int x, int y, int size, Block block) {
        TexturePaint tp = textureFor(block);
        if (tp != null) {
            fillRoundedWithTexture(g2, x, y, size, tp, Color.DARK_GRAY);
        }
    }

    /* ========== 내부 도우미 ========== */

    // 키(블록 타입)에 따라 다른 해치 패턴 생성 (외부 이미지 불필요)
    private static TexturePaint hatchForKeyWithAlpha(String key, int alpha) {
        int style = Math.abs(key.hashCode()) % 4; // 0~3 스타일
        int sz = 12;
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // 배경을 약간 밝은 톤(투명 포함)으로
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(255,255,255,0));
        g.fillRect(0,0,sz,sz);
        g.setStroke(new BasicStroke(2f));
        Color line = new Color(255,255,255,alpha);
        g.setColor(line);
        switch (style) {
            case 0:
                // /// 대각선
                for (int i=-sz; i<sz*2; i+=4) {
                    g.drawLine(i, 0, i+sz, sz);
                }
                break;
            case 1:
                // \\ 대각선 반대
                for (int i=-sz; i<sz*2; i+=4) {
                    g.drawLine(i, sz, i+sz, 0);
                }
                break;
            case 2:
                // 십자격자
                for (int i=0; i<sz; i+=4) {
                    g.drawLine(i, 0, i, sz);
                    g.drawLine(0, i, sz, i);
                }
                break;
            default:
                // 점 패턴
                for (int y=1; y<sz; y+=4) {
                    for (int x=1; x<sz; x+=4) {
                        g.fillRect(x, y, 2, 2);
                    }
                }
                break;
        }
        g.dispose();
        return new TexturePaint(img, new Rectangle(0,0,sz,sz)); // (0,0) 앵커
    }


    private static void fillRounded(Graphics2D g2, int x, int y, int size, Color c) {
        int arc = Math.max(4, Math.round(size * 0.2f));
        Shape cell = new RoundRectangle2D.Float(x, y, size, size, arc, arc);
        g2.setColor(c);
        g2.fill(cell);
        g2.setColor(c.darker());
        g2.draw(cell);
    }

    private static void fillRoundedRect(Graphics2D g2, int x, int y, int width, int height, Color c) {
        int arc = Math.max(4, Math.round(width * 0.2f));
        Shape cell = new RoundRectangle2D.Float(x, y, width, height, arc, arc);
        g2.setColor(c);
        g2.fill(cell);
        g2.setColor(c.darker());
        g2.draw(cell);
    }

    private static void fillRoundedWithTexture(Graphics2D g2, int x, int y, int size,
                                               TexturePaint tp, Color border) {
        int arc = Math.max(4, Math.round(size * 0.2f));
        Shape cell = new RoundRectangle2D.Float(x, y, size, size, arc, arc);
        Paint old = g2.getPaint();
        g2.setPaint(tp);          // (0,0) 앵커 고정된 TexturePaint
        g2.fill(cell);
        g2.setPaint(old);
        g2.setColor(border);
        g2.draw(cell);
    }

    private static void fillRoundedRectWithTexture(Graphics2D g2, int x, int y, int width, int height,
                                                   TexturePaint tp, Color border) {
        int arc = Math.max(4, Math.round(width * 0.2f));
        Shape cell = new RoundRectangle2D.Float(x, y, width, height, arc, arc);
        Paint old = g2.getPaint();
        g2.setPaint(tp);
        g2.fill(cell);
        g2.setPaint(old);
        g2.setColor(border);
        g2.draw(cell);
    }

    /** 블록 클래스명과 동일한 파일명 텍스처를 찾는다 (예: OBlock.png). */
    private static TexturePaint textureFor(Block block) {
        String key = (block != null) ? block.getClass().getSimpleName() : "default";
        TexturePaint cached = CACHE.get(key);
        if (cached != null) return cached;

        // 1) 클래스패스: /textures/<name>.png
        String classpath = "/textures/" + key + ".png";
        BufferedImage img = loadFromClasspath(classpath);

        // 2) 실행 폴더 상대 파일: ./textures/<name>.png
        if (img == null) {
            img = loadFromFile(new File("textures/" + key + ".png"));
        }
        // 3) Gradle 표준 리소스 경로(개발시): app/src/main/resources/textures/<name>.png
        if (img == null) {
            img = loadFromFile(new File("app/src/main/resources/textures/" + key + ".png"));
        }

        if (img != null) {
            TexturePaint tp = new TexturePaint(img, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
            CACHE.put(key, tp);
            return tp;
        }
        return null;
    }

    private static BufferedImage loadFromClasspath(String path) {
        try (InputStream in = PatternPainter.class.getResourceAsStream(path)) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {}
        return null;
    }

    private static BufferedImage loadFromFile(File f) {
        try {
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception ignored) {}
        return null;
    }
}
