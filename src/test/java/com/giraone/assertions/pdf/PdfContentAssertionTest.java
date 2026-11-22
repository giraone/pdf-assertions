package com.giraone.assertions.pdf;

import com.giraone.assertions.pdf.model.RectangleInMm;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PdfContentAssertionTest {

    @ParameterizedTest(name = "Basic Test {0}")
    @CsvSource(value = {
    // NAME                   | PG | TEXT                | TITLE                              | SUBJECT    | CREATOR                       | AUTHOR           | PRODUCER                           |
    "pdf-1p-v1.4-by-dist.pdf  |  1 | ItalicLine 8        | Microsoft Word - sampleForSpec.rtf |            | Acrobat PDFMaker 6.0 for Word | Michel Felix     | Acrobat Distiller 6.0 (Windows)    |",
    "pdf-1p-v1.4-by-oo.pdf    |  1 | Security Threat     |                                    |            | Writer                        |                  | OpenOffice 4.1.2                   |",
    "pdf-1p-v1.5-text.pdf     |  1 | PDF Overlay Seite 1 |                                    |            |                               |                  |                                    |",
    "pdf-1p-v1.7-by-pdf24.pdf |  1 | VIELEN DANK         | Test                               | My Subject | PDF24 Creator                 | giraone          | GPL Ghostscript 10.08.0            |",
    "pdf-2p-v1.3-text.pdf     |  2 | Carogne             | Erlkönig.pages                     |            | Pages                         |                  | Mac OS X 10.11.1 Quartz PDFContext |",
    "pdf-3p-v1.3-by-FPDF.pdf  |  3 | NVIDIA GeForce      |                                    |            |                               |                  | FPDF 1.53                          |",
    "pdf-3p-v1.3-props.pdf    |  3 | Three long pages    | Sample PDF                         | ''         | Word                          | Philip Hutchison | Mac OS X 10.11.3 Quartz PDFContext |",
    "pdf-28p-v1.3-text.pdf    | 28 | Chinese foo         |                                    |            | TeX                           |                  | pdfTeX-0.14f                       |",
    "pdf-1p-v2.0.pdf          |  1 | Hello World         |                                    |            |                               |                  |                                    |",
    }, delimiter = '|')
    void basicAssertions_basicallyWork(String fileName, int expectedPages,
                                       String expectedText, String expectedTitle, String expectedSubject,
                                       String expectedCreator, String expectedAuthor, String expectedProducer) throws IOException {

        // arrange
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/test-pdfs", fileName));
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes)
                .isValidPdf()
                .hasNumberOfPages(expectedPages)
                .containsTextStringOnAnyPage(expectedText)
                .assertDocumentTitle(value -> Assertions.assertThat(value).isEqualTo(expectedTitle))
                .assertDocumentSubject(value -> Assertions.assertThat(value).isEqualTo(expectedSubject))
                .assertDocumentCreator(value -> Assertions.assertThat(value).isEqualTo(expectedCreator))
                .assertDocumentAuthor(value -> Assertions.assertThat(value).isEqualTo(expectedAuthor))
                .assertDocumentProducer(value -> Assertions.assertThat(value).isEqualTo(expectedProducer))
        ;
    }

    @Test
    void containsTextStringAtPosition_basicallyWorks() throws IOException {

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
    void containsTextStringAtPosition_worksForMultipagePdf() throws IOException {

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
    void isValidPdf_detectsHtmlPageAsInvalidPdf() {

        // assert
        PdfContentAssertion.assertThat((byte[]) null).isNotValidPdf();
        PdfContentAssertion.assertThat("".getBytes()).isNotValidPdf();
        PdfContentAssertion.assertThat("%PDF-1.5".getBytes()).isNotValidPdf();
        PdfContentAssertion.assertThat(
                "<html><body>Some large text 0123456789 0123456789 0123456789 0123456789 0123456789</body></html>"
                        .getBytes()).isNotValidPdf();
    }

    @Test
    void assertXmpDocumentTitle_basicallyWorks() throws IOException {

        // arrange
        byte[] pdfBytes = readTestPdfWithProperties();
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes)
                .assertXmpDocumentTitle((value) -> assertThat(value)
                        .isEqualTo("Microsoft Word - sampleForSpec.rtf"));
    }

    @Test
    void extractXPath_worksBasically() {

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


    @Disabled
    @Test
    void isValidPdfA() throws IOException {
        // arrange
        byte[] pdfBytes = readTestPdfA("xps-3a");
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes).isValidPdfA();
    }

    @ParameterizedTest(name = "isValidPdfAProfile {0}")
    @CsvSource(value = {
        // NAME        | PROFILE |
        "xps-3a        |      3a |",
        //"pdf24-1b      |      1b |",
        "pdf24-2b      |      2b |",
        "pdf24-3b      |      3b |",
        "pdf24-4       |       4 |",
        //"pdf24-4f      |      4f |",
    }, delimiter = '|')
    void isValidPdfAProfile(String name, String profile) throws IOException {
        // arrange
        byte[] pdfBytes = readTestPdfA(name);
        // act/assert
        PdfContentAssertion.assertThat(pdfBytes).isValidPdfAProfile(profile);
    }

    @ParameterizedTest(name = "isValidPdfAProfile {0}")
    @CsvSource(value = {
        // NAME        | PROFILES |
        "xps-3a        | PDFA_3_A |",
        "pdf24-1b      | PDFA_1_B |",
        "pdf24-2b      | PDFA_2_B |",
        "pdf24-3b      | PDFA_3_B |",
        "pdf24-4       | PDFA_4   |",
        "pdf24-4f      | PDFA_4_F |",
    }, delimiter = '|')
    void fetchPdfAProfiles(String name, String profile) throws IOException {
        // arrange
        byte[] pdfBytes = readTestPdfA(name);
        // act/assert
        assertThat(PdfContentAssertion.assertThat(pdfBytes).fetchPdfAProfiles())
            .containsExactlyInAnyOrder(profile);
    }

    //------------------------------------------------------------------------------------------------------------------

    private byte[] readTestPdfWithOnePage() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdfs/pdf-1p-v1.5-text.pdf"));
    }

    private byte[] readTestPdfWithTwoPages() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdfs/pdf-2p-v1.5-text.pdf"));
    }

    private byte[] readTestPdfWithProperties() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdfs/pdf-1p-v1.4-by-dist.pdf"));
    }

    private byte[] readTestPdfA(String name) throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-pdf-a/" + name + ".pdf"));
    }
}
