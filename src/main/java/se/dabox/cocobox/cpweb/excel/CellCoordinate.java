/*
 * (c) Dabox AB 2012 All Rights Reserved
 */
package se.dabox.cocobox.cpweb.excel;

/**
 *
 * @author Jerker Klang <jerker.klang@dabox.se>
 */
public class CellCoordinate {
    private final int row;
    private final int col;

    public CellCoordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /**
     * Returns the cell coordinate in an Excel-like fashion.
     * For example the coordinate for 0,0 would be A1 and 2,2 C3.
     *
     * @return A string with the coordinate
     */
    @Override
    public String toString() {
        return toColumnString(col)+(row+1);
    }

    public static String toColumnString(int col) {
        if (col >= 26) {
            throw new IllegalStateException("Can't encode column index: "+col);
        }

        return Character.toString((char)('A'+col));
    }

}
