[![GitHub license](https://img.shields.io/github/license/giraone/pdf-assertions)](https://github.com/giraone/pdf-assertions/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.0.0-blue)](https://mvnrepository.com/artifact/com.giraone.imaging/pdf-assertions)
[![GitHub issues](https://img.shields.io/github/issues/giraone/pdf-assertions)](https://github.com/giraone/pdf-assertions/issues)
[![GitHub stars](https://img.shields.io/github/stars/giraone/pdf-assertions)](https://github.com/giraone/pdf-assertions/stargazers)
[![Platform](https://img.shields.io/badge/platform-jre21%2B-blue)](https://github.com/giraone/pdf-assertions/pom.xml)

# PDF Assertion Lib

[AssertJ](https://assertj.github.io/doc/) style custom assertions for PDF documents. The goal is to verify the content
of generated PDFs in unit tests: validity, number of pages, document metadata (info dictionary and XMP),
PDF/A compliance and - most notably - whether a text appears on a page, optionally inside a given rectangle
(in millimeters).

The PDF is parsed only once per assertion chain.

## Technologies

- Java 25
- [Apache PDFBox](https://pdfbox.apache.org/): for text extraction with positions basic PDF/A validation (*PDFBox Preflight*)
- [veraPDF](https://github.com/verapdf/): for PDF/A validation against different PDF/A profiles
- [AssertJ](https://assertj.github.io/doc/): base of the custom assertions
- JUnit 5: test framework
- Maven: build, release to Maven Central

## Maven coordinates

```xml
<dependency>
  <groupId>com.giraone.assertions</groupId>
  <artifactId>pdf-assertions</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

## Usage examples

### 1. Basic validity and page count

```java
import static com.giraone.assertions.pdf.PdfContentAssertion.assertThat;

byte[] pdfBytes = Files.readAllBytes(Path.of("invoice.pdf"));
assertThat(pdfBytes)
    .isValidPdf()
    .hasNumberOfPages(2)
    .containsTextStringOnAnyPage("Invoice");
```

### 2. Text at a position (in millimeters, per page)

```java
PdfContentAssertion.assertThat(new File("letter.pdf"))
    .isValidPdf()
    .containsTextStringAtPosition(1, new RectangleInMm(60, 80, 160, 120), "PDF Overlay")
    .containsTextStringAtPosition(2, new RectangleInMm(60, 100, 160, 140), "Field C");
```

### 3. Document metadata

```java
PdfContentAssertion.assertThat(pdfBytes)
    .assertDocumentTitle(title -> org.assertj.core.api.Assertions.assertThat(title).isEqualTo("My Title"))
    .assertDocumentAuthor(author -> org.assertj.core.api.Assertions.assertThat(author).contains("John Doe"));
```

### 4. PDF/A validation

```java
PdfContentAssertion.assertThat(pdfBytes).isValidPdfAProfile("1b");
```

### 5. Negative check

```java
PdfContentAssertion.assertThat("<html></html>".getBytes()).isNotValidPdf();
```

## Build

```
mvn clean verify
```

## Release to Maven Central

Releases are done with `mvn release:prepare release:perform`. The `release` profile signs the artifacts with GPG
(passphrase from environment variable `MAVEN_GPG_PASSPHRASE`); sources and Javadoc JARs are attached by default.
The server id `ossrh-staging` must be configured in `~/.m2/settings.xml`.

## License

Apache License 2.0
