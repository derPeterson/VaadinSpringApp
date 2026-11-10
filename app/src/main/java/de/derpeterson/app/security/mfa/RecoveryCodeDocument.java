package de.derpeterson.app.security.mfa;

import de.derpeterson.app.helper.email.EmailLogo;
import de.derpeterson.app.i18n.MessageProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.openpdf.pdf.ITextRenderer;
import org.openpdf.pdf.ITextUserAgent;
import org.openpdf.text.DocumentException;
import org.openpdf.text.pdf.BaseFont;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Lokale HTML/CSS-Vorlage, ausschließlich eingebettete Ressourcen und In-Memory-Ausgabe. */
@Component
public final class RecoveryCodeDocument {
  public static String fileName(String serviceName) {
    // Zerlegt Umlaute und entfernt kombinierende Akzente. Nur ASCII-Buchstaben, Ziffern,
    // Bindestriche und Unterstriche bleiben; Pfad-, Steuer- und Windows-Sonderzeichen entfallen.
    String prefix =
        Normalizer.normalize(serviceName == null ? "" : serviceName, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}+", "")
            .replaceAll("[^A-Za-z0-9_-]+", "-");
    if (prefix.length() > 64) {
      prefix = prefix.substring(0, 64);
    }
    prefix = StringUtils.strip(prefix, "-");
    return prefix.isEmpty() ? "recovery-codes.pdf" : prefix + "-recovery-codes.pdf";
  }

  public byte[] render(
      String serviceName,
      String accountEmailAddress,
      List<String> recoveryCodes,
      Instant createdAt,
      Locale locale,
      MessageProperties messageProperties) {
    if (recoveryCodes.size() != 10
        || recoveryCodes.stream()
            .anyMatch(recoveryCode -> TwoFactorSecrets.recoveryDigest(1L, recoveryCode) == null)) {
      throw new IllegalArgumentException("Complete recovery code set required");
    }

    try (InputStream templateInputStream =
            new ClassPathResource("pdf/recovery-codes.html").getInputStream();
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {
      String htmlTemplate = new String(templateInputStream.readAllBytes(), StandardCharsets.UTF_8);
      String documentHtml =
          fillRecoveryCodeTemplate(
              htmlTemplate,
              serviceName,
              accountEmailAddress,
              recoveryCodes,
              createdAt,
              locale,
              messageProperties);

      ITextRenderer pdfRenderer = createLocalPdfRenderer();
      pdfRenderer.setDocumentFromString(documentHtml);
      pdfRenderer.layout();
      pdfRenderer.createPDF(pdfOutputStream);

      return pdfOutputStream.toByteArray();
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read recovery document resources", failure);
    } catch (DocumentException failure) {
      throw new IllegalStateException("Cannot embed recovery document fonts", failure);
    }
  }

  private String fillRecoveryCodeTemplate(
      String htmlTemplate,
      String serviceName,
      String accountEmailAddress,
      List<String> recoveryCodes,
      Instant createdAt,
      Locale locale,
      MessageProperties messageProperties)
      throws IOException {
    Map<String, String> templateValues = new LinkedHashMap<>();
    templateValues.put("LANG", locale.getLanguage());
    templateValues.put("TITLE", messageProperties.getTwoFactorRecoveryTitle());
    templateValues.put("SERVICE", serviceName);
    templateValues.put("ACCOUNT", accountEmailAddress);
    templateValues.put("ACCOUNT_LABEL", messageProperties.getTwoFactorDocumentAccount());
    templateValues.put("DATE_LABEL", messageProperties.getTwoFactorDocumentDate());
    templateValues.put(
        "DATE",
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC)
            .format(createdAt));
    templateValues.put("EXPLANATION", messageProperties.getTwoFactorRecoveryExplanation());
    templateValues.put("STORAGE_NOTICE", messageProperties.getTwoFactorRecoveryStorage());
    templateValues.put("REPLACEMENT_NOTICE", messageProperties.getTwoFactorRecoveryReplacement());
    templateValues.put("SUPPORT_NOTICE", messageProperties.getTwoFactorRecoverySupport());
    templateValues.put(
        "LOGO",
        "data:image/png;base64," + Base64.getEncoder().encodeToString(EmailLogo.readBytes()));

    for (Map.Entry<String, String> templateEntry : templateValues.entrySet()) {
      htmlTemplate =
          htmlTemplate.replace(
              "{{" + templateEntry.getKey() + "}}", HtmlUtils.htmlEscape(templateEntry.getValue()));
    }

    return htmlTemplate.replace("{{CODE_ROWS}}", formatRecoveryCodeRows(recoveryCodes));
  }

  private String formatRecoveryCodeRows(List<String> recoveryCodes) {
    StringBuilder htmlRowsBuilder = new StringBuilder();
    for (int recoveryCodeIndex = 0;
        recoveryCodeIndex < recoveryCodes.size();
        recoveryCodeIndex += 2) {
      htmlRowsBuilder
          .append("<tr><td>")
          .append(HtmlUtils.htmlEscape(recoveryCodes.get(recoveryCodeIndex)))
          .append("</td><td>")
          .append(HtmlUtils.htmlEscape(recoveryCodes.get(recoveryCodeIndex + 1)))
          .append("</td></tr>");
    }

    return htmlRowsBuilder.toString();
  }

  private ITextRenderer createLocalPdfRenderer() throws IOException, DocumentException {
    ITextRenderer renderer = new ITextRenderer();
    addEmbeddedFont(renderer, "InterVariable.ttf", "Inter");
    addEmbeddedFont(renderer, "SpaceMono-Regular.ttf", "Space Mono");
    String defaultCss =
        Objects.requireNonNull(
                ITextRenderer.class.getResource("/resources/css/XhtmlNamespaceHandler.css"))
            .toExternalForm();
    renderer
        .getSharedContext()
        .setUserAgentCallback(
            new ITextUserAgent(renderer.getOutputDevice(), ITextRenderer.DEFAULT_DOTS_PER_PIXEL) {
              @Override
              public String resolveURI(String uri) {
                // Ausschließlich die mitgelieferte Renderer-Basis-CSS und unser eingebettetes
                // Logo.
                if (defaultCss.equals(uri)
                    || uri != null && uri.startsWith("data:image/png;base64,")) {
                  return uri;
                }
                throw new SecurityException("External PDF resources are not permitted");
              }
            });
    return renderer;
  }

  private void addEmbeddedFont(ITextRenderer renderer, String fileName, String family)
      throws IOException, DocumentException {
    String resource = "META-INF/resources/custom-theme/fonts/" + fileName;
    try (InputStream input = new ClassPathResource(resource).getInputStream()) {
      BaseFont font =
          BaseFont.createFont(
              fileName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, input.readAllBytes(), null);
      renderer.getFontResolver().addFont(font, resource, family);
    }
  }
}
