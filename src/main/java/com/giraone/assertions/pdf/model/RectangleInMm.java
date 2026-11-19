package com.giraone.assertions.pdf.model;

/**
 * Rectangle on a page, measured in mm.
 *
 * @param leftUpperX  upper left position horizontally from left in mm
 * @param leftUpperY  upper left position vertically from top in mm
 * @param rightLowerX lower right position horizontally from left in mm
 * @param rightLowerY lower right position vertically from top in mm
 */
public record RectangleInMm(float leftUpperX, float leftUpperY, float rightLowerX, float rightLowerY) {

    @Override
    public String toString() {
        return String.format("[%5.1f, %5.1f] - [%5.1f, %5.1f]", leftUpperX, leftUpperY, rightLowerX, rightLowerY);
    }
}
