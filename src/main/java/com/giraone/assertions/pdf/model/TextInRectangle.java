package com.giraone.assertions.pdf.model;

/**
 * Expected text within a rectangle on a page.
 *
 * @param _comment  optional comment
 * @param page      page number (1-based); a value of 0 defaults to 1
 * @param rectangle the area
 * @param exactText the expected text
 */
public record TextInRectangle(String _comment, int page, RectangleInMm rectangle, String exactText) {

    public TextInRectangle {
        if (page == 0) {
            page = 1;
        }
    }
}
