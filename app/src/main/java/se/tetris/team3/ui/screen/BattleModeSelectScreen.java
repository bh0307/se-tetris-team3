package se.tetris.team3.ui.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import se.tetris.team3.core.GameMode;
import se.tetris.team3.core.Settings;
import se.tetris.team3.ui.AppFrame;

/**
 * 대전 모드 선택 화면
 * - 일반 대전: 상대방 게임 오버시까지
 * - 아이템 대전: 아이템 사용 가능
 * - 시간제한 대전: 제한 시간 내 점수 경쟁
 */
public class BattleModeSelectScreen implements Screen {
    
    private final AppFrame frame;
    private final Settings settings;
    
    private int selectedMode = 0;  // 0: 일반, 1: 아이템, 2: 시간제한
    private int timeLimit = 3;     // 시간제한 모드의 제한 시간 (분)
    
    private final String[] modeNames = {
        "Normal Battle",
        "Item Battle",
        "Time Attack"
    };
    
    private final String[] modeDescriptions = {
        "상대방이 게임 오버될 때까지 대결합니다",
        "아이템을 사용하여 상대방을 방해할 수 있습니다",
        "제한 시간 내에 더 높은 점수를 얻는 사람이 승리합니다"
    };
    
    public BattleModeSelectScreen(AppFrame frame, Settings settings) {
        this.frame = frame;
        this.settings = settings;
    }
    
    @Override
    public void onKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        // 모드 선택 (위/아래 화살표)
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            selectedMode = (selectedMode - 1 + modeNames.length) % modeNames.length;
        } else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            selectedMode = (selectedMode + 1) % modeNames.length;
        }
        
        // 시간제한 모드일 경우 시간 조정 (좌/우 화살표)
        else if (selectedMode == 2 && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A)) {
            if (timeLimit > 1) {
                timeLimit--;
            }
        } else if (selectedMode == 2 && (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D)) {
            if (timeLimit < 10) {
                timeLimit++;
            }
        }
        
        // 게임 시작 (Enter)
        else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            startBattle();
        }
        
        // 뒤로 가기 (ESC)
        else if (key == KeyEvent.VK_ESCAPE) {
            frame.showScreen(new MenuScreen(frame));
        }
    }
    
    /**
     * 선택한 모드로 대전 시작
     */
    private void startBattle() {
        GameMode mode;
        int timeLimitSeconds = 0;
        
        switch (selectedMode) {
            case 0:
                mode = GameMode.BATTLE_NORMAL;
                break;
            case 1:
                mode = GameMode.BATTLE_ITEM;
                break;
            case 2:
                mode = GameMode.BATTLE_TIME;
                timeLimitSeconds = timeLimit * 60;  // 분을 초로 변환
                break;
            default:
                mode = GameMode.BATTLE_NORMAL;
        }
        
        BattleScreen battleScreen = new BattleScreen(frame, mode, settings, timeLimitSeconds);
        frame.showScreen(battleScreen);
    }
    
    @Override
    public void render(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = frame.getWidth();
        int height = frame.getHeight();
        int centerX = width / 2;
        
        // 배경
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        
        // 제목
        g2.setColor(Color.CYAN);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 48));
        String title = "BATTLE MODE";
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, centerX - titleWidth / 2, 100);
        
        // 부제
        g2.setColor(Color.LIGHT_GRAY);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        String subtitle = "Select Battle Mode";
        int subtitleWidth = g2.getFontMetrics().stringWidth(subtitle);
        g2.drawString(subtitle, centerX - subtitleWidth / 2, 140);
        
        // 모드 목록
        int startY = 220;
        int spacing = 120;
        
        for (int i = 0; i < modeNames.length; i++) {
            int y = startY + i * spacing;
            boolean isSelected = (i == selectedMode);
            
            // 선택 표시
            if (isSelected) {
                g2.setColor(new Color(255, 255, 0, 50));
                g2.fillRoundRect(centerX - 250, y - 40, 500, 100, 20, 20);
                
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 32));
            } else {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("맑은 고딕", Font.PLAIN, 28));
            }
            
            // 모드 이름
            String modeName = modeNames[i];
            int nameWidth = g2.getFontMetrics().stringWidth(modeName);
            g2.drawString(modeName, centerX - nameWidth / 2, y);
            
            // 설명
            g2.setColor(isSelected ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
            String desc = modeDescriptions[i];
            int descWidth = g2.getFontMetrics().stringWidth(desc);
            g2.drawString(desc, centerX - descWidth / 2, y + 30);
            
            // 시간제한 모드일 경우 시간 설정 표시
            if (i == 2 && isSelected) {
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("맑은 고딕", Font.BOLD, 20));
                String timeText = "< " + timeLimit + " min" + (timeLimit > 1 ? "s" : "") + " >";
                int timeWidth = g2.getFontMetrics().stringWidth(timeText);
                g2.drawString(timeText, centerX - timeWidth / 2, y + 60);
            }
        }
        
        // 하단 조작 안내
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        
        String[] instructions = {
            "↑/↓ or W/S: Select Mode",
            "←/→ or A/D: Adjust Time (Time Attack Only)",
            "ENTER or SPACE: Start Battle",
            "ESC: Back to Menu"
        };
        
        int instructY = height - 150;
        for (String inst : instructions) {
            int instWidth = g2.getFontMetrics().stringWidth(inst);
            g2.drawString(inst, centerX - instWidth / 2, instructY);
            instructY += 25;
        }
    }
}

