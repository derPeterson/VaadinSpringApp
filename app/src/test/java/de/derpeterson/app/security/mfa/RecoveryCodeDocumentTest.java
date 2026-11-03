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
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openpdf.text.pdf.PdfDictionary;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

class RecoveryCodeDocumentTest {
  @Test
  void fileNamesKeepTheServiceWithoutPathsInvalidCharactersOrUnboundedLength() {
    Map<String, String> examples =
        Map.of(
            "derPeter", "derPeter-recovery-codes.pdf",
            "Müller / Portal: \"Test\"?", "Muller-Portal-Test-recovery-codes.pdf",
            "../../CON\r\n", "CON-recovery-codes.pdf",
            "...<>|?*", "recovery-codes.pdf");
    examples.forEach(
        (service, expected) -> assertEquals(expected, RecoveryCodeDocument.fileName(service)));
    assertEquals("recovery-codes.pdf", RecoveryCodeDocument.fileName(null));
    assertEquals("recovery-codes.pdf", RecoveryCodeDocument.fileName(""));
    assertEquals(
        "A".repeat(64) + "-recovery-codes.pdf", RecoveryCodeDocument.fileName("A".repeat(300)));
  }

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
      PdfDictionary fonts = reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.FONT);
      String fontNames =
          fonts.getKeys().stream()
              .map(name -> fonts.getAsDict(name).getAsName(PdfName.BASEFONT).toString())
              .reduce("", (names, name) -> names + " " + name);
      assertTrue(fontNames.contains("Inter"), "Document text must use Inter");
      assertTrue(fontNames.contains("SpaceMono"), "Recovery codes must use Space Mono");
      for (PdfName name : fonts.getKeys()) {
        PdfDictionary font = fonts.getAsDict(name);
        if (font.getAsArray(PdfName.DESCENDANTFONTS) != null) {
          PdfDictionary descendant = font.getAsArray(PdfName.DESCENDANTFONTS).getAsDict(0);
          assertTrue(
              descendant.getAsDict(PdfName.FONTDESCRIPTOR).contains(PdfName.FONTFILE2),
              "TrueType fonts must be embedded for offline use");
        }
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
