package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

class RecoveryCodeDocumentTest {
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void rendersOneA4PageWithLogoAllCodesAndEscapedIdentity(String language) throws Exception {
    Locale locale = Locale.forLanguageTag(language);
    MessageProperties texts = MessageProperties.forLocale(new CustomI18NProvider(), locale);
    List<String> codes =
        IntStream.range(0, 10)
            .mapToObj(index -> String.format(Locale.ROOT, "%08X-12345678-ABCDEF01-23456789", index))
            .toList();
    byte[] bytes =
        new RecoveryCodeDocument()
            .render(
                "Cosmos <Test>",
                "fixture@example.test",
                codes,
                Instant.parse("2026-11-02T12:00:00Z"),
                locale,
                texts);
    PdfReader reader = new PdfReader(bytes);
    try {
      assertEquals(1, reader.getNumberOfPages());
      assertEquals(595, Math.round(reader.getPageSize(1).getWidth()));
      assertEquals(842, Math.round(reader.getPageSize(1).getHeight()));
      String content = new PdfTextExtractor(reader).getTextFromPage(1);
      // Zeilenumbrüche und andere Leerraumfolgen des PDF-Textauszugs zusammenfassen.
      String normalized = content.replaceAll("\\s+", " ");
      assertTrue(normalized.contains(texts.getTwoFactorRecoveryExplanation()));
      assertTrue(content.contains("Cosmos <Test>"));
      assertTrue(content.contains("fixture@example.test"));
      assertTrue(content.contains(texts.getTwoFactorRecoveryTitle()));
      for (String code : codes) {
        assertTrue(content.contains(code), "Every complete recovery code must be readable");
      }
      assertTrue(
          reader
                  .getPageN(1)
                  .getAsDict(org.openpdf.text.pdf.PdfName.RESOURCES)
                  .getAsDict(org.openpdf.text.pdf.PdfName.XOBJECT)
                  .size()
              > 0,
          "Embedded service logo required");
    } finally {
      reader.close();
    }
    // Ausschließlich synthetische Sichtprüfbelege im ignorierten Buildverzeichnis.
    Path directory = Path.of("target", "mfa-preview");
    Files.createDirectories(directory);
    Files.write(directory.resolve("recovery-" + language + ".pdf"), bytes);
  }
}
