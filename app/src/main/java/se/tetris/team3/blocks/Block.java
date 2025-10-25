package se.tetris.team3.blocks;

import java.awt.Color;

// itemType: 간단한 문자 기반 아이템 태깅('L','W', 0=없음)
// item: 추후 실제 Item 객체를 붙여 세밀한 동작을 넣고 싶을 때 사용.
// UI Rendering에서 블록/아이템 문자를 표시할 수 있으므로 getItemType()사용
public abstract class Block {

    protected int[][] shape;
    protected Color color;

    // 아이템 태깅(줄삭제용)
    private char itemType = 0;   // 'L'만 사용(무게추는 타입으로 구분)
    private int itemRow = -1;    // 블록 내부 행
    private int itemCol = -1;    // 블록 내부 열

    public Block() {
        shape = new int[][]{{1,1},{1,1}};
        color = Color.YELLOW;
    }

    public int[][] getShape() { return shape; }
    public Color getColor() { return color; }

    // 회전 시 L 좌표도 같이 회전
    public void rotate() {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = shape[r][c];
            }
        }
        shape = rotated;

        // L이 붙어 있으면 좌표를 같이 회전 (r,c) -> (c, rows-1-r)
        if (itemType == 'L' && itemRow >= 0 && itemCol >= 0) {
            int newRow = itemCol;
            int newCol = rows - 1 - itemRow; // 주의: rows는 "회전 전" 높이
            itemRow = newRow;
            itemCol = newCol;
        }
    }

    public int height() { return shape.length; }
    public int width() { return shape.length > 0 ? shape[0].length : 0; }

    // 줄삭제 아이템용 게터/세터
    public void setItemType(char t) { this.itemType = t; }
    public char getItemType() { return this.itemType; }

    // 블록 내부 좌표(r,c)에 L을 태깅. 미지정은 (-1,-1)
    public void setItemCell(int r, int c) { this.itemRow = r; this.itemCol = c; }
    public int getItemRow() { return itemRow; }
    public int getItemCol() { return itemCol; }
    public boolean isItemCell(int r, int c) { return (r == itemRow && c == itemCol); }

    // (무게추는 전용 조각 클래스로 구분하므로 별도 itemType 필요 없음)
}
