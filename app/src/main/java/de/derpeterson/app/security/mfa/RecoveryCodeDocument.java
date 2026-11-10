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
      String service,
      String account,
      List<String> codes,
      Instant createdAt,
      Locale locale,
      MessageProperties messageProperties) {
    if (codes.size() != 10
        || codes.stream().anyMatch(code -> TwoFactorSecrets.recoveryDigest(1L, code) == null)) {
      throw new IllegalArgumentException("Complete recovery code set required");
    }
    try (InputStream input = new ClassPathResource("pdf/recovery-codes.html").getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      String htmlTemplate = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      String documentHtml =
          fillRecoveryCodeTemplate(
              htmlTemplate, service, account, codes, createdAt, locale, messageProperties);
      ITextRenderer renderer = createLocalPdfRenderer();
      renderer.setDocumentFromString(documentHtml);
      renderer.layout();
      renderer.createPDF(output);
      return output.toByteArray();
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot read recovery document resources", failure);
    } catch (DocumentException failure) {
      throw new IllegalStateException("Cannot embed recovery document fonts", failure);
    }
  }

  private String fillRecoveryCodeTemplate(
      String htmlTemplate,
      String serviceName,
      String account,
      List<String> recoveryCodes,
      Instant createdAt,
      Locale locale,
      MessageProperties messageProperties)
      throws IOException {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("LANG", locale.getLanguage());
    values.put("TITLE", messageProperties.getTwoFactorRecoveryTitle());
    values.put("SERVICE", serviceName);
    values.put("ACCOUNT", account);
    values.put("ACCOUNT_LABEL", messageProperties.getTwoFactorDocumentAccount());
    values.put("DATE_LABEL", messageProperties.getTwoFactorDocumentDate());
    values.put(
        "DATE",
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC)
            .format(createdAt));
    values.put("EXPLANATION", messageProperties.getTwoFactorRecoveryExplanation());
    values.put("STORAGE_NOTICE", messageProperties.getTwoFactorRecoveryStorage());
    values.put("REPLACEMENT_NOTICE", messageProperties.getTwoFactorRecoveryReplacement());
    values.put("SUPPORT_NOTICE", messageProperties.getTwoFactorRecoverySupport());
    values.put(
        "LOGO",
        "data:image/png;base64," + Base64.getEncoder().encodeToString(EmailLogo.readBytes()));
    for (Map.Entry<String, String> value : values.entrySet()) {
      htmlTemplate =
          htmlTemplate.replace(
              "{{" + value.getKey() + "}}", HtmlUtils.htmlEscape(value.getValue()));
    }
    return htmlTemplate.replace("{{CODE_ROWS}}", formatRecoveryCodeRows(recoveryCodes));
  }

  private String formatRecoveryCodeRows(List<String> recoveryCodes) {
    StringBuilder rows = new StringBuilder();
    for (int index = 0; index < recoveryCodes.size(); index += 2) {
      rows.append("<tr><td>")
          .append(HtmlUtils.htmlEscape(recoveryCodes.get(index)))
          .append("</td><td>")
          .append(HtmlUtils.htmlEscape(recoveryCodes.get(index + 1)))
          .append("</td></tr>");
    }
    return rows.toString();
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
