package se.tetris.team3.ui.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.Timer;

import se.tetris.team3.blocks.Block;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.gameManager.GameManager;
import se.tetris.team3.gameManager.MenuItem;
import se.tetris.team3.gameManager.ScoreManager;
import se.tetris.team3.ui.AppFrame;

// 메뉴 항목: 클래식 시작, 아이템 모드 시작, 설정, 스코어보드, 종료
public class MenuScreen implements Screen {

    private final AppFrame app;
    private final List<MenuItem> items = new ArrayList<>();
    private int idx = 0; // 현재 선택 인덱스
    private boolean showHintHighlight = false; // 힌트 강조 표시 여부
    private long hintHighlightTime = 0; // 힌트 강조 시작 시간
    private Timer repaintTimer; // 화면 갱신 타이머
    private Timer animationTimer; // 배경 애니메이션 타이머
    private List<FallingBlock> fallingBlocks = new ArrayList<>();
    private Random random = new Random();
    private int menuStartY = 280; // 메뉴 시작 Y 좌표
    private int menuItemHeight = 40; // 메뉴 항목 높이

    // 배경 떨어지는 블록 클래스
    private static class FallingBlock {
        Block block;
        int x;
        float y;
        float speed;
        int rotation;
        
        FallingBlock(Block block, int x, float y, float speed, int rotation) {
            this.block = block;
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.rotation = rotation;
        }
    }

    public MenuScreen(AppFrame app) {
        this.app = app;

        items.add(new MenuItem("클래식 시작", () -> {
            app.getSettings().setGameMode(GameMode.CLASSIC);
            app.showScreen(new GameScreen(app, new GameManager(GameMode.CLASSIC)));
        }));
        items.add(new MenuItem("아이템 모드 시작", () -> {
            app.getSettings().setGameMode(GameMode.ITEM);
            app.showScreen(new GameScreen(app, new GameManager(GameMode.ITEM)));
        }));

        items.add(new MenuItem("대전 모드", () -> app.showScreen(new BattleModeSelectScreen(app, app.getSettings()))));
        items.add(new MenuItem("P2P 대전 모드", () ->app.showScreen(new P2PLobbyScreen(app, app.getSettings()))));

        items.add(new MenuItem("설정", () -> app.showScreen(new SettingsScreen(app))));

        items.add(new MenuItem("스코어보드", () -> app.showScreen(new ScoreboardScreen(app, -1, new ScoreManager()))));

        items.add(new MenuItem("종료", () -> System.exit(0)));
    }

    @Override public void render(Graphics2D g2) {
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        int w = app.getWidth();

        // 배경 그라디언트
        for (int i = 0; i < app.getHeight(); i++) {
            float ratio = (float)i / app.getHeight();
            int r = (int)(20 + ratio * 10);
            int g = (int)(20 + ratio * 10);
            int b = (int)(40 + ratio * 20);
            g2.setColor(new Color(r, g, b));
            g2.drawLine(0, i, app.getWidth(), i);
        }

        // 떨어지는 배경 블록
        int blockSize = 25;
        for (FallingBlock fb : fallingBlocks) {
            int[][] shape = fb.block.getShape();
            Color color = new Color(
                fb.block.getColor().getRed(),
                fb.block.getColor().getGreen(),
                fb.block.getColor().getBlue(),
                50
            );
            g2.setColor(color);
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[r].length; c++) {
                    if (shape[r][c] != 0) {
                        int px = fb.x + c * blockSize;
                        int py = (int)fb.y + r * blockSize;
                        g2.fillRect(px, py, blockSize - 1, blockSize - 1);
                    }
                }
            }
        }

        // 타이틀 - 테트리스 블록 형태로 그리기
        drawTetrisLogo(g2, w / 2 - 200, 70);

        // 메뉴 항목
        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        int y = menuStartY;
        for (int i = 0; i < items.size(); i++) {
            String t = (i == idx ? "> " : "  ") + items.get(i).getLabel();
            g2.setColor(i == idx ? new Color(120, 200, 255) : Color.LIGHT_GRAY);
            g2.drawString(t, (w - g2.getFontMetrics().stringWidth(t)) / 2, y);
            y += menuItemHeight;
        }

        // 하단 힌트 (잘못된 키 입력 시 1초간만 표시)
        boolean showHint = showHintHighlight && System.currentTimeMillis() - hintHighlightTime < 1000;
        if (showHintHighlight && !showHint) {
            showHintHighlight = false;
        }
        
        if (showHint) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            String hint = "↑/↓ 이동   Enter 선택   Esc 종료";
            g2.drawString(hint, (w - g2.getFontMetrics().stringWidth(hint)) / 2, app.getHeight() - 60);
        }
    }

    // 테트리스 로고 그리기 (블록 모양으로)
    private void drawTetrisLogo(Graphics2D g2, int startX, int startY) {
        int size = 22;
        int spacing = 4; // 문자 간 간격
        Color[] colors = {
            new Color(0, 240, 240),   // I - 청록
            new Color(240, 160, 0),   // L - 주황
            new Color(0, 0, 240),     // J - 파랑
            new Color(240, 240, 0),   // O - 노랑
            new Color(0, 240, 0),     // S - 초록
            new Color(160, 0, 240),   // T - 보라
            new Color(240, 0, 0)      // Z - 빨강
        };
        
        // "TETRIS" 문자를 블록으로 표현
        int x = startX;
        
        // T
        drawBlockLetter(g2, x, startY, size, colors[5], new int[][]{
            {1,1,1},
            {0,1,0},
            {0,1,0}
        });
        x += size * 3 + spacing;
        
        // E
        drawBlockLetter(g2, x, startY, size, colors[0], new int[][]{
            {1,1,1},
            {1,1,0},
            {1,1,1}
        });
        x += size * 3 + spacing;
        
        // T
        drawBlockLetter(g2, x, startY, size, colors[6], new int[][]{
            {1,1,1},
            {0,1,0},
            {0,1,0}
        });
        x += size * 3 + spacing;
        
        // R
        drawBlockLetter(g2, x, startY, size, colors[2], new int[][]{
            {1,1,0},
            {1,1,0},
            {1,0,1}
        });
        x += size * 3 + spacing;
        
        // I
        drawBlockLetter(g2, x, startY, size, colors[1], new int[][]{
            {1,1,1},
            {0,1,0},
            {1,1,1}
        });
        x += size * 3 + spacing;
        
        // S
        drawBlockLetter(g2, x, startY, size, colors[4], new int[][]{
            {0,1,1},
            {1,1,0},
            {1,1,0}
        });
    }
    
    private void drawBlockLetter(Graphics2D g2, int x, int y, int size, Color color, int[][] pattern) {
        for (int r = 0; r < pattern.length; r++) {
            for (int c = 0; c < pattern[r].length; c++) {
                if (pattern[r][c] == 1) {
                    // 블록 그리기 (그라디언트 효과)
                    int px = x + c * size;
                    int py = y + r * size;
                    
                    // 메인 블록
                    g2.setColor(color);
                    g2.fillRect(px, py, size - 2, size - 2);
                    
                    // 하이라이트
                    g2.setColor(color.brighter());
                    g2.fillRect(px + 2, py + 2, size - 6, size / 4);
                    
                    // 테두리
                    g2.setColor(color.darker());
                    g2.drawRect(px, py, size - 2, size - 2);
                }
            }
        }
    }
    
    // 배경 블록 업데이트
    private void updateFallingBlocks() {
        // 블록 이동
        for (int i = fallingBlocks.size() - 1; i >= 0; i--) {
            FallingBlock fb = fallingBlocks.get(i);
            fb.y += fb.speed;
            
            // 화면 밖으로 나가면 제거
            if (fb.y > app.getHeight()) {
                fallingBlocks.remove(i);
            }
        }
        
        // 새 블록 생성 (확률적으로 계속 생성)
        if (random.nextInt(100) < 5) {
            Block[] blocks = {
                new se.tetris.team3.blocks.IBlock(),
                new se.tetris.team3.blocks.JBlock(),
                new se.tetris.team3.blocks.LBlock(),
                new se.tetris.team3.blocks.OBlock(),
                new se.tetris.team3.blocks.SBlock(),
                new se.tetris.team3.blocks.TBlock(),
                new se.tetris.team3.blocks.ZBlock()
            };
            Block block = blocks[random.nextInt(blocks.length)];
            int x = random.nextInt(app.getWidth() - 100);
            float speed = 0.5f + random.nextFloat() * 1.5f;
            int rotation = random.nextInt(4);
            
            // 회전 적용
            for (int i = 0; i < rotation; i++) {
                block.rotate();
            }
            
            fallingBlocks.add(new FallingBlock(block, x, -100, speed, rotation));
        }
    }

    @Override public void onKeyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                idx = (idx - 1 + items.size()) % items.size();
                break;
            case KeyEvent.VK_DOWN:
                idx = (idx + 1) % items.size();
                break;
            case KeyEvent.VK_ENTER:
                items.get(idx).getAction().run();
                break;
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
            default:
                // 유효하지 않은 키 입력 시 힌트 강조
                showHintHighlight = true;
                hintHighlightTime = System.currentTimeMillis();
                // 1초 후 자동으로 화면 갱신하기 위한 타이머
                if (repaintTimer != null) {
                    repaintTimer.stop();
                }
                repaintTimer = new Timer(1000, e1 -> {
                    app.repaint();
                    repaintTimer.stop();
                });
                repaintTimer.setRepeats(false);
                repaintTimer.start();
                break;
        }
        app.repaint();
    }
    
    @Override public void onMouseClicked(MouseEvent e) {
        int mouseY = e.getY();
        
        // 클릭된 메뉴 항목 찾기
        for (int i = 0; i < items.size(); i++) {
            int itemY = menuStartY + i * menuItemHeight;
            int itemTop = itemY - 20; // 텍스트 위쪽 여유
            int itemBottom = itemY + 10; // 텍스트 아래쪽 여유
            
            if (mouseY >= itemTop && mouseY <= itemBottom) {
                // 메뉴 항목 실행
                items.get(i).getAction().run();
                break;
            }
        }
    }
    
    @Override public void onMouseMoved(MouseEvent e) {
        int mouseY = e.getY();
        int oldIdx = idx;
        
        // 마우스 위치에 따라 선택 항목 변경
        for (int i = 0; i < items.size(); i++) {
            int itemY = menuStartY + i * menuItemHeight;
            int itemTop = itemY - 20;
            int itemBottom = itemY + 10;
            
            if (mouseY >= itemTop && mouseY <= itemBottom) {
                idx = i;
                break;
            }
        }
        
        // 선택이 바뀌었으면 다시 그리기
        if (oldIdx != idx) {
            app.repaint();
        }
    }
    
    //필요시 포커스 관련 처리 추가 가능
    @Override public void onShow() {
        // 배경 애니메이션 타이머 시작
        animationTimer = new Timer(30, e -> {
            updateFallingBlocks();
            app.repaint();
        });
        animationTimer.start();
    }
    //필요시 리소스 정리
    @Override public void onHide() {
        if (repaintTimer != null) {
            repaintTimer.stop();
        }
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
    
}
