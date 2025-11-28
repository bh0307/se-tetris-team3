package se.tetris.team3.items;

import se.tetris.team3.gameManager.GameManager;

// 'L' : 고정 시, 아이템이 포함되었던 "그 줄"을 꽉 차지 않아도 삭제. (강제 라인 클리어)
public class LineClearItem implements Item {

    @Override
    public void onLock(GameManager gm) {
        gm.applyLineClearItem();
    }

    @Override
    public char symbol() {
        return 'L';
    }
}
