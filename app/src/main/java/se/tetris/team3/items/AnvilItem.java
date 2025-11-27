package se.tetris.team3.items;

import se.tetris.team3.gameManager.GameManager;

// 'W' : 낙하 중 아래 블록들을 파괴, 고정 후 좌우 이동 불가, 점수 0.
public class AnvilItem implements Item {

    private boolean horizontalLocked = false;

    @Override
    public void onFalling(GameManager gm) {
        // 하강하며 아래 열의 블럭들을 삭제
        gm.activateWeightEffectAt(gm.getBlockX(), gm.getBlockY());
    }

    @Override
    public void onLock(GameManager gm) {
        // 고정되는 순간 좌우 이동 잠금
        horizontalLocked = true;
    }

    public boolean isHorizontalLocked() {
        return horizontalLocked;
    }

    @Override
    public char symbol() {
        return 'W';
    }
}
