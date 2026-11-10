package de.derpeterson.app.security.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Geschützte Dokumente verhindern, dass Seeds oder Ersatzcodes in Vaadins UI-/Sitzungszustand
 * landen.
 */
@RestController
@RequestMapping(TwoFactorDocumentsController.PATH)
@RequiredArgsConstructor
public class TwoFactorDocumentsController {
  public static final String PATH = "/internal/two-factor";
  private final TwoFactorEnrollment twoFactorEnrollment;
  private final TwoFactorAuthorization twoFactorAuthorization;
  private final ConfigService configService;
  private final CustomI18NProvider i18nProvider;
  private final RecoveryCodeDocument recoveryCodeDocument;

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Void> denied() {
    // Dokumentabrufe dürfen keine Login-HTML-Seite mit scheinbar erfolgreichem Status liefern.
    return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).build();
  }

  @GetMapping("/setup/{id}/qr.png")
  public ResponseEntity<byte[]> qr(HttpServletRequest request, @PathVariable UUID id) {
    byte[] secret = twoFactorEnrollment.pendingSecret(request, id);
    try {
      String service = configService.getString(ConfigEntry.SERVICE_NAME);
      String identity = twoFactorAuthorization.owner(request).email();
      String uri =
          "otpauth://totp/"
              + encode(service + ":" + identity)
              + "?secret="
              + Totp.base32(secret)
              + "&issuer="
              + encode(service)
              + "&algorithm=SHA1&digits=6&period=30";
      BitMatrix matrix =
          new QRCodeWriter()
              .encode(
                  uri,
                  BarcodeFormat.QR_CODE,
                  280,
                  280,
                  Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 4));
      BufferedImage image =
          new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < matrix.getHeight(); y++) {
        for (int x = 0; x < matrix.getWidth(); x++) {
          image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xffffff);
        }
      }
      try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        ImageIO.write(image, "png", output);
        return response(output.toByteArray(), MediaType.IMAGE_PNG);
      }
    } catch (WriterException failure) {
      throw new IllegalStateException("Cannot encode authenticator setup", failure);
    } catch (IOException failure) {
      throw new UncheckedIOException("Cannot render authenticator setup", failure);
    } finally {
      Arrays.fill(secret, (byte) 0);
    }
  }

  @GetMapping("/setup/{id}/key")
  public ResponseEntity<byte[]> manualKey(HttpServletRequest request, @PathVariable UUID id) {
    byte[] secret = twoFactorEnrollment.pendingSecret(request, id);
    try {
      return response(
          Totp.base32(secret).getBytes(StandardCharsets.US_ASCII), MediaType.TEXT_PLAIN);
    } finally {
      Arrays.fill(secret, (byte) 0);
    }
  }

  @GetMapping("/recovery/{id}/codes")
  public ResponseEntity<byte[]> codes(HttpServletRequest request, @PathVariable UUID id) {
    List<String> codes = twoFactorEnrollment.recoveryOutput(request, id);
    return response(
        String.join("\n", codes).getBytes(StandardCharsets.US_ASCII), MediaType.TEXT_PLAIN);
  }

  @GetMapping("/recovery/{id}/codes.pdf")
  public ResponseEntity<byte[]> pdf(
      HttpServletRequest request,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "en") String lang) {
    List<String> codes = twoFactorEnrollment.recoveryOutput(request, id);
    Locale locale = locale(lang);
    String serviceName = configService.getString(ConfigEntry.SERVICE_NAME);
    byte[] bytes =
        recoveryCodeDocument.render(
            serviceName,
            twoFactorAuthorization.owner(request).email(),
            codes,
            Instant.now(),
            locale,
            MessageProperties.forLocale(i18nProvider, locale));
    return response(
        bytes,
        MediaType.APPLICATION_PDF,
        "attachment; filename=\"" + RecoveryCodeDocument.fileName(serviceName) + "\"");
  }

  private static Locale locale(String lang) {
    return "de".equals(lang) ? Locale.GERMAN : Locale.ENGLISH;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static ResponseEntity<byte[]> response(byte[] bytes, MediaType type) {
    return response(bytes, type, null);
  }

  private static ResponseEntity<byte[]> response(byte[] bytes, MediaType type, String disposition) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(type);
    headers.setCacheControl(CacheControl.noStore());
    headers.setPragma("no-cache");
    headers.set("Referrer-Policy", "no-referrer");
    headers.set("X-Robots-Tag", "noindex, nofollow");
    headers.set(
        "Content-Security-Policy", "default-src 'none'; style-src 'self'; frame-ancestors 'self'");
    if (disposition != null) {
      headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
    }
    return ResponseEntity.ok().headers(headers).body(bytes);
  }
}
