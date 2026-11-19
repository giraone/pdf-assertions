package com.giraone.assertions.pdf;

import com.giraone.assertions.pdf.model.RectangleInMm;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PdfContentAssertionTest {

    @Test
    void assertThat_isValidPdf_basicallyWorks() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithOnePage();
        // assert
        PdfContentAssertion.assertThat(pdfBytes).isValidPdf();
    }

    @Test
    void assertThat_isValidPdf_detectsHtmlPageAsInvalidPdf() {

        // assert
        PdfContentAssertion.assertThat((byte[]) null).isNotValidPdf();
        PdfContentAssertion.assertThat("".getBytes()).isNotValidPdf();
        PdfContentAssertion.assertThat("%PDF-1.5".getBytes()).isNotValidPdf();
        PdfContentAssertion.assertThat(
            "<html><body>Some large text 0123456789 0123456789 0123456789 0123456789 0123456789</body></html>"
                .getBytes()).isNotValidPdf();
    }

    @Disabled
    @Test
    void assertThat_assertDocumentTitle_basicallyWorks() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithThreePagesAndTitle();
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes)
            .assertDocumentTitle((value) -> assertThat(value)
                .isEqualTo("Sample PDF"));
    }

    @Disabled
    @Test
    void assertThat_assertXmpDocumentTitle_basicallyWorks() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithThreePagesAndTitle();
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes)
            .assertXmpDocumentTitle((value) -> assertThat(value)
                .isEqualTo("Sample PDF"));
    }

    @ParameterizedTest
    @CsvSource({
        "pdf-with-some-text.pdf, 1",
        "pdf-with-two-pages-and-some-text.pdf, 2",
        "pdf-with-three-pages-and-title.pdf, 3"
    })
    void assertThat_hasNumberOfPages_basicallyWorks(String fileName, int expectedPages) throws IOException {

        // arrange
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/test-pdfs", fileName));
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes).isValidPdf();
        PdfContentAssertion.assertThat(pdfBytes).hasNumberOfPages(expectedPages);
    }

    @Test
    void assertThat_containsTextStringOnAnyPage_basicallyWorks() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithOnePage();
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes).isValidPdf();
        PdfContentAssertion.assertThat(pdfBytes).containsTextStringOnAnyPage("Feld");
    }

    @Test
    void assertThat_containsTextStringAtPosition_basicallyWorks() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithOnePage();
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes)
            .isValidPdf()
            .containsTextStringAtPosition(1, new RectangleInMm(20, 30, 100, 40), "PDF Overlay")
            .containsTextStringAtPosition(1, new RectangleInMm(20, 35, 50, 45), "Feld A")
            .containsTextStringAtPosition(1, new RectangleInMm(20, 40, 50, 50), "Feld B");
    }

    @Test
    void assertThat_containsTextStringAtPosition_worksForMultipagePdf() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithTwoPages();
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes)
            .isValidPdf()
            .containsTextStringAtPosition(1, new RectangleInMm(20, 30, 100, 40), "PDF Overlay")
            .containsTextStringAtPosition(1, new RectangleInMm(20, 35, 50, 45), "Feld A")
            .containsTextStringAtPosition(1, new RectangleInMm(20, 40, 50, 50), "Feld B")
            .containsTextStringAtPosition(2, new RectangleInMm(20, 30, 100, 40), "PDF Overlay")
            .containsTextStringAtPosition(2, new RectangleInMm(20, 35, 50, 45), "Feld C")
            .containsTextStringAtPosition(2, new RectangleInMm(20, 40, 50, 50), "Feld D");
    }

    @Test
    void  assertThat_extractXPath_worksBasically() {

        // Some XPath expressions ...
        assertThat(PdfContentAssertion.extractXPath("<first>FIRST</first>",
            "/first"))
            .isEqualTo("FIRST");
        assertThat(PdfContentAssertion.extractXPath("<first><second>SECOND</second></first>",
            "//second"))
            .isEqualTo("SECOND");
        assertThat(PdfContentAssertion.extractXPath("<rdf:first><rdf:second>SECOND</rdf:second></rdf:first>",
            "//*[name()='rdf:second']"))
            .isEqualTo("SECOND");
        assertThat(PdfContentAssertion.extractXPath("<rdf:first><rdf:second><rdf:third>THIRD</rdf:third></rdf:second></rdf:first>",
            "//*[name()='rdf:first']//*[name()='rdf:third']"))
            .isEqualTo("THIRD");
    }

    //------------------------------------------------------------------------------------------------------------------

    private byte[] readTestPdfWithOnePage() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdfs/pdf-with-some-text.pdf"));
    }

    private byte[] readTestPdfWithTwoPages() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdfs/pdf-with-two-pages-and-some-text.pdf"));
    }

    private byte[] readTestPdfWithThreePagesAndTitle() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdfs/pdf-with-three-pages-and-title.pdf"));
    }
}