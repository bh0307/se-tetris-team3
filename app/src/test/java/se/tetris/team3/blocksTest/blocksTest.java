package se.tetris.team3.blocksTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import se.tetris.team3.blocks.LBlock;

public class blocksTest {
    
    private LBlock lBlock;
    
    @BeforeEach
    void setUp() {
        lBlock = new LBlock();
    }
    
    @Test 
    @DisplayName("LBlock 회전 테스트")
    void rotateTest(){
        int[][][] expectedShapes = new int[][][] { 
			{
                {1, 1, 1},
			    {1, 0, 0}
            },
            {
                {1,1},
                {0,1},
                {0,1}
            },
            {
                {0, 0, 1},
			    {1, 1, 1}
            },
            {
                {1,0},
                {1,0},
                {1,1}
            }
		};
        for (int[][] expectedShape : expectedShapes) {
            assertArrayEquals(lBlock.getShape(),expectedShape);
            lBlock.rotate();
        }
    }
}
