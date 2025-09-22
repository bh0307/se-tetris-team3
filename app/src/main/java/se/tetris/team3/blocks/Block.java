package se.tetris.team3.blocks;
import java.awt.Color;

public abstract class Block {
		
	protected int[][] shape;
	protected Color color;
	
	public Block() {
		shape = new int[][]{ 
				{1, 1}, 
				{1, 1}
		};
		color = Color.YELLOW;
	}
	
	public int[][] getShape() {
		return shape;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void rotate() {
		// 시계방향 90도 회전
		int rows = shape.length;
		int cols = shape[0].length;
		int[][] rotated = new int[cols][rows];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				rotated[j][rows - 1 - i] = shape[i][j];
			}
		}
		shape = rotated;
	}
	
	public int height() {
		return shape.length;
	}
	
	public int width() {
		if(shape.length > 0)
			return shape[0].length;
		return 0;
	}
}
