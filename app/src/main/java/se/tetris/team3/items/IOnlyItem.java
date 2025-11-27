
package se.tetris.team3.items;

import se.tetris.team3.gameManager.GameManager;

// I 아이템: 고정 시 일정 시간 동안 I형 블록만 생성되게 함
public class IOnlyItem implements Item {

    private final int durationMs;

    public IOnlyItem() { this(5000); }
    public IOnlyItem(int durationMs) { this.durationMs = durationMs; }

    @Override
    public void onLock(GameManager gm) {
        if (gm != null) gm.activateIOnlyMode(durationMs);
    }

    @Override
    public char symbol() { return 'I'; }
}
