package se.tetris.team3.blocks;

import java.awt.Color;

public class SBlock extends Block {

	public SBlock() {
		shape = new int[][] { 
			{0, 1, 1},
			{1, 1, 0}
		};
		color = Color.GREEN;
	}
}
