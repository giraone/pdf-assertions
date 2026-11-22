package com.giraone.assertions.pdf;

import com.giraone.assertions.pdf.model.RectangleInMm;
import com.giraone.assertions.pdf.model.TextInRectangle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.text.PDFTextStripper;
import org.assertj.core.api.AbstractAssert;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.flavours.PDFFlavours;
import org.verapdf.pdfa.validation.profiles.RuleId;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A custom assertion class, to check whether a PDF contains a certain data, e.g. a certain text [in a certain region].
 * Should be used like this - here the PDF is read and parsed only once:
 * <code>
 * byte[] pdfBytes = ...
 * PdfContentAssertion.assertThat(pdfBytes)
 * .isValidPdf()
 * .containsTextStringAtPosition(1, new RectangleInMm(60, 80, 160, 120), "PDF Overlay")
 * .containsTextStringAtPosition(1, new RectangleInMm(60, 100, 160, 140), "Feld A")
 * .containsTextStringAtPosition(1, new RectangleInMm(60, 120, 160, 160), "Feld B")
 * .containsTextStringAtPosition(2, new RectangleInMm(60, 80, 160, 120), "PDF Overlay")
 * .containsTextStringAtPosition(2, new RectangleInMm(60, 100, 160, 140), "Feld C")
 * .containsTextStringAtPosition(2, new RectangleInMm(60, 120, 160, 160), "Feld D");
 * </code>
 */
public class PdfContentAssertion extends AbstractAssert<PdfContentAssertion, byte[]> {

    private static final float EPSILON = 0.05f;
    private static final AtomicBoolean VERA_PDF_INITIALIZED = new AtomicBoolean(false);

    String allText;
    Integer actualNumberOfPages;
    PDDocumentInformation documentInformation;
    String xmpMetaData;
    List<TextLocation> locations;
    PointAdaption pointAdaption;
    ScaleAdaption scaleAdaption;

    public PdfContentAssertion(byte[] bytes, PointAdaption pointAdaption, ScaleAdaption scaleAdaption) {
        super(bytes, PdfContentAssertion.class);
        this.pointAdaption = pointAdaption;
        this.scaleAdaption = scaleAdaption;
    }

    public static PdfContentAssertion assertThat(File actual) {
        try {
            byte[] bytes = Files.readAllBytes(actual.toPath());
            return assertThat(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF file: " + actual, e);
        }
    }
    public static PdfContentAssertion assertThat(byte[] actual) {
        return new PdfContentAssertion(actual, new PointAdaption(), new ScaleAdaption());
    }

    public static synchronized void initializeVeraPdf() {
        if (!VERA_PDF_INITIALIZED.get()) {
            VeraGreenfieldFoundryProvider.initialise();
            VERA_PDF_INITIALIZED.set(true);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    public PdfContentAssertion withPointAdaption(PointAdaption pointAdaption) {
        this.pointAdaption = pointAdaption;
        return this;
    }

    @SuppressWarnings("unused")
    public PdfContentAssertion withScaleAdaption(ScaleAdaption scaleAdaption) {
        this.scaleAdaption = scaleAdaption;
        return this;
    }

    /**
     * Test, if content is valid PDF.
     */
    public PdfContentAssertion isValidPdf() {

        if (this.actual == null) {
            failWithMessage("PDF bytes are null");
            return this;
        }
        if (this.actual.length < 50) {
            failWithMessage("PDF bytes are too small! Expected at least 50 bytes, but was " + this.actual.length);
            return this;
        }
        final String magicIntro = new String(this.actual, 0, 5);
        if (!"%PDF-".equals(magicIntro)) {
            failWithMessage("PDF does not start with %PDF-, it was \"" + magicIntro + "\"");
            return this;
        }
        return this;
    }

    /**
     * Test, if content is NOT valid PDF.
     */
    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion isNotValidPdf() {

        if (this.actual == null) {
            return this;
        }
        if (this.actual.length < 50) {
            return this;
        }
        final String magicIntro = new String(this.actual, 0, 5);
        if (!"%PDF-".equals(magicIntro)) {
            return this;
        }
        failWithMessage("PDF seems to be valid");
        return this;
    }

    /**
     * Test for PDF/A compatibility using PDFBox preflight.
     */
    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion isValidPdfA() throws IOException {

        if (this.actual == null) {
            failWithMessage("PDF bytes are null");
            return this;
        }

        if (this.actual.length < 50) {
            failWithMessage("PDF bytes are too small! Expected at least 50 bytes, but was " + this.actual.length);
            return this;
        }

        final Path pdf = Files.createTempFile("rrs-assertion-", ".pdf");
        try {
            Files.write(pdf, this.actual);
            final org.apache.pdfbox.preflight.ValidationResult result = PreflightParser.validate(pdf.toFile());
            if (!result.isValid()) {
                final StringBuilder sb = new StringBuilder("PDF is not PDF/A!");
                // for interpretation of error codes see:
                // https://github.com/apache/pdfbox/blob/trunk/preflight/src/main/java/org/apache/pdfbox/preflight/PreflightConstants.java
                for (org.apache.pdfbox.preflight.ValidationResult.ValidationError error : result.getErrorsList()) {
                    sb.append(System.lineSeparator()).append(error.getErrorCode()).append(" : ").append(error.getDetails());
                }
                failWithMessage(sb.toString().replace("%", "%%"));
            }
        } finally {
            Files.deleteIfExists(pdf);
        }
        return this;
    }

    /**
     * Test for PDF/A compatibility against a PDF/A profile, e.g. "1b", using veraPDF.
     * @param profile Profile ("1a", "1b", "2a", "2b")
     * @return content assertion
     */
    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion isValidPdfAProfile(String profile) throws IOException {

        if (this.actual == null) {
            failWithMessage("PDF bytes are null");
            return this;
        }
        if (this.actual.length < 50) {
            failWithMessage("PDF bytes are too small! Expected at least 50 bytes, but was " + this.actual.length);
            return this;
        }

        initializeVeraPdf();
        PDFAFlavour flavour = PDFAFlavour.fromString(profile);
        try (ByteArrayInputStream input = new ByteArrayInputStream(this.actual)) {
            try (VeraPDFFoundry foundry = Foundries.defaultInstance()) {
                try (PDFAParser parser = foundry.createParser(input, flavour)) {
                    PDFAValidator validator = foundry.createValidator(flavour, false);
                    org.verapdf.pdfa.results.ValidationResult result = validator.validate(parser);
                    if (!result.isCompliant()) {
                        Map<RuleId, Integer> failedChecks = result.getFailedChecks();
                        StringBuilder sb = new StringBuilder();
                        failedChecks.forEach((ruleId, failedCheck) ->
                            sb.append(System.lineSeparator()).append(ruleId).append(" : ").append(failedCheck).append("\n"));
                        failWithMessage("PDF is NOT compliant to PDF/A profile \"%s\"! Failed checks: %s",
                            profile, sb.toString());
                    }
                } catch (Exception e) {
                    failWithMessage("Cannot parse PDF to check for PDF/A profile \"%s\"", profile);
                }
            }
        }
        return this;
    }

    /**
     * Fetch all PDF/A profiles defined in the document using veraPDF.
     * @return list with profile names, e.g. "PDFA_3_A"
     * @throws IOException on errors reading the PDF
     */
    public List<String> fetchPdfAProfiles() throws IOException {

        if (this.actual == null) {
            failWithMessage("PDF bytes are null");
            return List.of();
        }
        if (this.actual.length < 50) {
            failWithMessage("PDF bytes are too small! Expected at least 50 bytes, but was " + this.actual.length);
            return List.of();
        }

        initializeVeraPdf();
        List<String> profiles = new ArrayList<>();
        try (ByteArrayInputStream input = new ByteArrayInputStream(this.actual)) {
            try (VeraPDFFoundry foundry = Foundries.defaultInstance()) {
                try (PDFAParser parser = foundry.createParser(input)) {
                    List<PDFAFlavour> detectedFlavours = parser.getFlavours();
                    for (PDFAFlavour flavour : detectedFlavours) {
                        // iterate through all detected flavors and pick up PDF/A and PDF/UA ones for validation
                        if (PDFFlavours.isFlavourFamily(flavour, PDFAFlavour.SpecificationFamily.PDF_A) ||
                            PDFFlavours.isFlavourFamily(flavour, PDFAFlavour.SpecificationFamily.PDF_UA)) {
                            profiles.add(flavour.name());
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return profiles;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion assertDocumentTitle(Function<String,Object> assertFunction) {

        isNotNull();
        init();
        assertFunction.apply(documentInformation != null ? documentInformation.getTitle() : null);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion assertDocumentSubject(Function<String,Object> assertFunction) {

        isNotNull();
        init();
        assertFunction.apply(documentInformation != null ? documentInformation.getSubject() : null);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion assertDocumentAuthor(Function<String,Object> assertFunction) {

        isNotNull();
        init();
        assertFunction.apply(documentInformation != null ? documentInformation.getAuthor() : null);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion assertDocumentProducer(Function<String,Object> assertFunction) {

        isNotNull();
        init();
        assertFunction.apply(documentInformation != null ? documentInformation.getProducer() : null);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion assertDocumentCreator(Function<String,Object> assertFunction) {

        isNotNull();
        init();
        assertFunction.apply(documentInformation != null ? documentInformation.getCreator() : null);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion assertXmpDocumentTitle(Function<String,Object> assertFunction) {

        isNotNull();
        init();
        assertFunction.apply(xmpMetaData != null ? extractXPath(xmpMetaData, "//*[name()='dc:title']//*[name()='rdf:li']") : null);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion assertXmpDocumentAuthor(Function<String,Object> assertFunction) {

        isNotNull();
        init();
        assertFunction.apply(xmpMetaData != null ? extractXPath(xmpMetaData, "//*[name()='dc:creator']//*[name()='rdf:li']") : null);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion hasNumberOfPages(int expectedNumberOfPages) {

        isNotNull();
        init();
        if (actualNumberOfPages != expectedNumberOfPages) {
            failWithMessage("Expected PDF to contain %d pages, but there are %d pages!",
                expectedNumberOfPages, actualNumberOfPages);
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion containsTextStringOnAnyPage(String text) {

        isNotNull();
        if (allText == null) {
            try {
                allText = readAndParseTextFromPdf();
            } catch (IOException e) {
                failWithMessage("Cannot parse PDF to check, if it contains \"%s\"! %s", text, e.getMessage());
                return this;
            }
        }
        if (!allText.contains(text)) {
            failWithMessage("Expected PDF to contain \"%s\", but there was none in:\r\n%s", text, allText);
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public PdfContentAssertion apply(TextInRectangle textInRectangle) {
        return containsTextStringAtPosition(textInRectangle.page(), textInRectangle.rectangle(), textInRectangle.exactText());
    }

    /**
     * Tests, whether the given text is on a given page with a given area
     * @param page  page number
     * @param position page area - hint: the area of the complete paragraph is checked!
     * @param text the text to be contained
     * @return assertion object
     */
    public PdfContentAssertion containsTextStringAtPosition(int page, RectangleInMm position, String text) {

        isNotNull();
        if (locations == null) {
            try {
                locations = readAndParseTextLocationsFromPdf();
            } catch (IOException e) {
                failWithMessage("Cannot parse PDF to check, if it contains \"%s\" within %s on page %d!",
                    text, position, page);
                return this;
            }
        }

        if (!containsAtPosition(locations, position, text, page)) {
            failWithMessage("Expected PDF to contain \"%s\" within %s on page %d there was none in: %s",
                text, position, page, dumpAllLocations());
        }
        return this;
    }

    //------------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("UnusedReturnValue")
    private boolean init() {
        if (actualNumberOfPages == null) {
            try (PDDocument document = Loader.loadPDF(this.actual)) {
                actualNumberOfPages = document.getNumberOfPages();
                documentInformation = copyOf(document.getDocumentInformation());
                xmpMetaData = readXmlMetaData(document);
            } catch (IOException e) {
                failWithMessage("Cannot parse PDF to check its number of pages! %s", e.getMessage());
                return true;
            }
        }
        return false;
    }

    // Values may be indirect objects, which cannot be resolved once the document is closed
    private static PDDocumentInformation copyOf(PDDocumentInformation source) {
        if (source == null) {
            return null;
        }
        final PDDocumentInformation copy = new PDDocumentInformation();
        copy.setTitle(source.getTitle());
        copy.setSubject(source.getSubject());
        copy.setAuthor(source.getAuthor());
        copy.setProducer(source.getProducer());
        copy.setCreator(source.getCreator());
        return copy;
    }

    private String readXmlMetaData(PDDocument document ) throws IOException {

        final PDDocumentCatalog catalog = document.getDocumentCatalog();
        final PDMetadata metadata = catalog.getMetadata();
        if (metadata == null) {
            return null;
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream x = metadata.exportXMPMetadata()) {
            final byte[] b = new byte[1024];
            int r;
            while ((r = x.read(b)) > 0) {
                out.write(b, 0, r);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    static String extractXPath(String xmlInputString, String xPathExpression) {

        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(new ByteArrayInputStream(xmlInputString.getBytes(StandardCharsets.UTF_8)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node node = (Node) xPath.compile(xPathExpression).evaluate(xmlDocument, XPathConstants.NODE);
            return node != null ? node.getTextContent() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readAndParseTextFromPdf() throws IOException {

        // From: https://www.tutorialspoint.com/pdfbox/pdfbox_reading_text.htm
        try (PDDocument document = Loader.loadPDF(this.actual)) {
            final PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    private List<TextLocation> readAndParseTextLocationsFromPdf() throws IOException {

        List<TextLocation> locations;
        try (PDDocument document = Loader.loadPDF(this.actual)) {
            PdfTextLocator pdfTextLocator = new PdfTextLocator(document, this.pointAdaption, this.scaleAdaption);
            locations = pdfTextLocator.retrieveLocations();
        }
        return locations;
    }

    private boolean containsAtPosition(List<TextLocation> locations, RectangleInMm position, String text, int page) {

        for (TextLocation location : locations) {
            if (location.text().contains(text) && isWithIn(location, position) && isOnSamePage(location, page)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnSamePage(TextLocation location, int page) {
        return location.page() == page;
    }

    private boolean isWithIn(TextLocation location, RectangleInMm position) {

        return (location.x() > position.leftUpperX() || fuzzyEquals(location.x(), position.leftUpperX()))
            && (location.getRightLowerX() < position.rightLowerX() || fuzzyEquals(location.getRightLowerX(), position.rightLowerX()))
            && (location.y() >= position.leftUpperY() || fuzzyEquals(location.y(), position.leftUpperY()))
            && (location.getRightLowerY() <= position.rightLowerY() || fuzzyEquals(location.getRightLowerY(), position.rightLowerY()));
    }

    private static boolean fuzzyEquals (float a, float b) {
        if (a==b) {
            return true;
        }
        return Math.abs(a - b) < EPSILON;
    }

    private String dumpAllLocations() {

        StringBuilder ret = new StringBuilder();
        for (TextLocation location : locations) {
            ret.append("\r\n- \"").append(location.printPos()).append(" = \"").append(location.text()).append("\"")
                .append(" on page ").append(location.page());
        }
        return ret.toString();
    }
}
