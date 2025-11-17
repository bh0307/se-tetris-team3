package se.tetris.team3.core;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;

// 화면 크기 프리셋, 색맹모드, 키맵, 게임 모드, 난이도 저장
// 전역 공유되는 객체 (AppFrame에서 생성/보관)
public class Settings {

    // SizePreset: 창 크기/블록 크기 스케일링에 즉시 사용 -> 화면 3단계 프리셋
    public enum SizePreset { SMALL, MEDIUM, LARGE }
    private SizePreset sizePreset = SizePreset.MEDIUM;

    // Difficulty: 게임 난이도
    public enum Difficulty { EASY, NORMAL, HARD }
    private Difficulty difficulty = Difficulty.NORMAL;

    // Action: 키 리매핑용 식별자
    public enum Action { MOVE_LEFT, MOVE_RIGHT, ROTATE, SOFT_DROP, HARD_DROP, PAUSE, EXIT }
    private final Map<Action, Integer> keymap = new EnumMap<>(Action.class);

    // 색맹 모드 (기본값 false)
    private boolean colorBlindMode = false;

    // 게임 모드 (기본값 CLASSIC)
    private GameMode gameMode = GameMode.CLASSIC;

    public Settings() { resetDefaults(); }

    // ===== Getters / Setters =====
    public SizePreset getSizePreset() { return sizePreset; }
    public void setSizePreset(SizePreset p) { sizePreset = p; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { difficulty = d; }

    public boolean isColorBlindMode() { return colorBlindMode; }
    public void setColorBlindMode(boolean on) { colorBlindMode = on; }

    public Map<Action, Integer> getKeymap() { return keymap; }

    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode mode) { gameMode = mode; }

    // ===== 화면/블록 스케일링 =====
    // 창/보드 전체 스케일 — AppFrame/게임 스케일링에 사용
    public Dimension resolveWindowSize() {
        return switch (sizePreset) {
            case SMALL -> new Dimension(520, 680);  
            case MEDIUM -> new Dimension(640, 900); 
            case LARGE -> new Dimension(760, 1080); 
        };
    }

    // sizePreset에 따라서 블록 사이즈가 바뀜
    public int resolveBlockSize() {
        return switch (sizePreset) {
            case SMALL -> 24;
            case MEDIUM -> 30;
            case LARGE -> 36;
        };
    }

    // ===== 기본값 초기화 =====
    public void resetDefaults() {
        sizePreset = SizePreset.MEDIUM;
        difficulty = Difficulty.NORMAL;
        colorBlindMode = false;
        gameMode = GameMode.CLASSIC;

        keymap.clear();
        keymap.put(Action.MOVE_LEFT, KeyEvent.VK_LEFT);
        keymap.put(Action.MOVE_RIGHT, KeyEvent.VK_RIGHT);
        keymap.put(Action.ROTATE, KeyEvent.VK_UP);
        keymap.put(Action.SOFT_DROP, KeyEvent.VK_DOWN);
            keymap.put(Action.HARD_DROP, KeyEvent.VK_SPACE); // 기본값: 스페이스바
        keymap.put(Action.PAUSE, KeyEvent.VK_P);
        keymap.put(Action.EXIT, KeyEvent.VK_ESCAPE);
    }
}
