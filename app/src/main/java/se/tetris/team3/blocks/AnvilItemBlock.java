package se.tetris.team3.blocks;

import java.awt.Color;

// 무게추(Anvil) 아이템 전용 조각: 윗줄 2칸 + 아랫줄 4칸 (폭 4 고정)
public class AnvilItemBlock extends Block {
    public AnvilItemBlock() {
        this.shape = new int[][]{
            {0, 1, 1, 0},
            {1, 1, 1, 1}
        };
        // 아이템임을 분명하게 보이는 색
        this.color = new Color(250, 220, 0);
    }

    // 무게추는 회전  금지
    @Override
    public void rotate() {
        // no-op
    }
}