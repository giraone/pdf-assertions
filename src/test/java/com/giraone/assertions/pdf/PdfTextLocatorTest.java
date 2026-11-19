package com.giraone.assertions.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextLocatorTest {

    @Test
    void assertThat_retrieveLocations_worksBasically() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithOnePage();

        // act
        PointAdaption pointAdaption = new PointAdaption();
        ScaleAdaption scaleAdaption = new ScaleAdaption();
        List<TextLocation> result;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PdfTextLocator pdfTextLocator = new PdfTextLocator(document, pointAdaption, scaleAdaption);
            result = pdfTextLocator.retrieveLocations();
        }

        // assert
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(3);

        TextLocation header = result.get(0);
        Assertions.assertAll(
            () -> assertThat(header.text()).contains("PDF Overlay"),
            () -> assertThat(header.page()).isEqualTo(1),
            () -> assertThat(header.x()).isCloseTo(25.0f, Percentage.withPercentage(10)),
            () -> assertThat(header.y()).isCloseTo(30.0f, Percentage.withPercentage(10)), // first line
            () -> assertThat(header.width()).isCloseTo(67.0f, Percentage.withPercentage(10)),
            () -> assertThat(header.height()).isCloseTo(3.3f, Percentage.withPercentage(10))  // large font
        );

        TextLocation fieldA = result.get(1);
        Assertions.assertAll(
            () -> assertThat(fieldA.text()).contains("Feld A"),
            () -> assertThat(fieldA.page()).isEqualTo(1),
            () -> assertThat(fieldA.x()).isCloseTo(25.0f, Percentage.withPercentage(10)),
            () -> assertThat(fieldA.y()).isCloseTo(38.0f, Percentage.withPercentage(10)), // second line
            () -> assertThat(fieldA.width()).isCloseTo(13.0f, Percentage.withPercentage(10)),
            () -> assertThat(fieldA.height()).isCloseTo(2.0f, Percentage.withPercentage(10))  // normal font
        );

        TextLocation fieldB = result.get(2);
        Assertions.assertAll(
            () -> assertThat(fieldB.text()).contains("Feld B"),
            () -> assertThat(fieldB.page()).isEqualTo(1),
            () -> assertThat(fieldA.x()).isCloseTo(25.0f, Percentage.withPercentage(10)),
            () -> assertThat(fieldA.y()).isCloseTo(38.0f, Percentage.withPercentage(10)), // third line
            () -> assertThat(fieldA.width()).isCloseTo(13.0f, Percentage.withPercentage(10)),
            () -> assertThat(fieldA.height()).isCloseTo(2.0f, Percentage.withPercentage(10))  // normal font
        );
    }

    //------------------------------------------------------------------------------------------------------------------

    private byte[] readTestPdfWithOnePage() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdfs/pdf-with-some-text.pdf"));
    }
}
