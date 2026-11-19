package com.giraone.assertions.pdf;

/**
 * Location of a text on a page of a PDF; all positions and sizes in mm.
 *
 * @param text   the text
 * @param page   page number (1-based)
 * @param x      left upper horizontal position
 * @param y      left upper vertical position
 * @param width  width of the text
 * @param height height of the text
 */
public record TextLocation(String text, int page, float x, float y, float width, float height) {

    /**
     * @return horizontal position of the right lower corner
     */
    public float getRightLowerX() {
        return x + width;
    }

    /**
     * @return vertical position of the right lower corner
     */
    public float getRightLowerY() {
        return y + height;
    }

    /**
     * @return printable representation of the covered area
     */
    public String printPos() {
        return String.format("[%5.1f, %5.1f] - [%5.1f, %5.1f]", x, y, getRightLowerX(), getRightLowerY());
    }
}
