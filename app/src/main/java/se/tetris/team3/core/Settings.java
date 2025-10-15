package se.tetris.team3.core;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;

public class Settings {
    // SizePreset:창 크기/블록 크기 스케일링에 즉시 사용 -> 화면 3단계 프리셋
    public enum SizePreset { SMALL, MEDIUM, LARGE }
    // Action: 키 리매핑용 식별자
    public enum Action { MOVE_LEFT, MOVE_RIGHT, ROTATE, SOFT_DROP, PAUSE, EXIT }

    private SizePreset sizePreset = SizePreset.MEDIUM;
    // colorBlindMode: 색맹 모드 (기본값 false)
    private boolean colorBlindMode = false;
    private final Map<Action, Integer> keymap = new EnumMap<>(Action.class);

    public Settings() { resetDefaults(); }

    public SizePreset getSizePreset() { return sizePreset; }
    public void setSizePreset(SizePreset p) { sizePreset = p; }

    public boolean isColorBlindMode() { return colorBlindMode; }
    public void setColorBlindMode(boolean on) { colorBlindMode = on; }

    public Map<Action, Integer> getKeymap() { return keymap; }

    // 창/보드 전체 스케일 — AppFrame/게임 스케일링에 사용
    public Dimension resolveWindowSize() {
        return switch (sizePreset) {
            case SMALL -> new Dimension(360, 600);
            case MEDIUM -> new Dimension(480, 720);
            case LARGE -> new Dimension(600, 900);
        };
    }
    
    //sizePreset에 따라서 블록 사이즈가 바뀜
    public int resolveBlockSize() {
        return switch (sizePreset) {
            case SMALL -> 24;
            case MEDIUM -> 30;
            case LARGE -> 36;
        };
    }

    // 설정이 엉망이 됐을 때, 기본값으로 초기화 할 수 있음
    public void resetDefaults() {
        sizePreset = SizePreset.MEDIUM;
        colorBlindMode = false;
        keymap.clear();
        keymap.put(Action.MOVE_LEFT, KeyEvent.VK_LEFT);
        keymap.put(Action.MOVE_RIGHT, KeyEvent.VK_RIGHT);
        keymap.put(Action.ROTATE, KeyEvent.VK_UP);
        keymap.put(Action.SOFT_DROP, KeyEvent.VK_DOWN);
        keymap.put(Action.PAUSE, KeyEvent.VK_P);
        keymap.put(Action.EXIT, KeyEvent.VK_ESCAPE);
    }

    /*

    게임 껐을때도 색맹모드 상태 저장하고 싶다면 이 부분 추가하면 됨

    private boolean colorBlindMode = false;

    public boolean isColorBlindMode() {
        return colorBlindMode;
    }

    public void setColorBlindMode(boolean colorBlindMode) {
        this.colorBlindMode = colorBlindMode;
     */

}
