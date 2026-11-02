package de.derpeterson.app.ui.components;

import com.vaadin.flow.server.streams.DownloadEvent;
import de.derpeterson.app.helper.logging.LogSafety;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.mfa.RecoveryCodeDocument;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorEnrollment;
import de.derpeterson.app.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Jeder Download prüft die aktuelle Berechtigung und lässt die einmalige Ausgabe unangetastet. */
@Component
@RequiredArgsConstructor
@Slf4j
public final class RecoveryCodeDownload {
  private final TwoFactorEnrollment enrollment;
  private final TwoFactorAuthorization authorization;
  private final ConfigService config;
  private final CustomI18NProvider i18n;
  private final RecoveryCodeDocument pdf;

  public void write(DownloadEvent event, UUID outputId, Locale locale, Runnable failed) {
    event.getResponse().setHeader("Cache-Control", "no-store");
    event.getResponse().setHeader("Pragma", "no-cache");
    event.getResponse().setHeader("X-Content-Type-Options", "nosniff");
    byte[] bytes = null;
    try {
      HttpServletRequest request = (HttpServletRequest) event.getRequest();
      List<String> codes = enrollment.recoveryOutput(request, outputId);
      bytes =
          pdf.render(
              config.getString(ConfigEntry.SERVICE_NAME),
              authorization.owner(request).email(),
              codes,
              Instant.now(),
              locale,
              MessageProperties.forLocale(i18n, locale));
      event.setFileName("recovery-codes.pdf");
      event.setContentType("application/pdf");
      event.setContentLength(bytes.length);
      event.getOutputStream().write(bytes);
    } catch (IOException | RuntimeException failure) {
      log.warn("Recovery code PDF download failed", LogSafety.safeFailure(failure));
      event.getResponse().setStatus(failure instanceof AccessDeniedException ? 403 : 500);
      event.getUI().access(failed::run);
    } finally {
      if (bytes != null) {
        Arrays.fill(bytes, (byte) 0);
      }
    }
  }
}
