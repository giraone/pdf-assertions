package com.giraone.assertions.pdf.model;

import java.util.List;

/**
 * Definition of text assertions for a PDF.
 *
 * @param textAssertions the text assertions
 */
public record AssertionDefinition(List<TextInRectangle> textAssertions) {
}
