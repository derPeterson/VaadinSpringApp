package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
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
  public static final String RESOURCE_NAME = "two-factor-recovery-codes.pdf";

  private final TwoFactorEnrollment twoFactorEnrollment;
  private final TwoFactorAuthorization twoFactorAuthorization;
  private final ConfigService configService;
  private final CustomI18NProvider i18nProvider;
  private final RecoveryCodeDocument recoveryCodeDocument;

  /** Ein Fehlerstatus darf im Browser weder eine TXT-Datei noch einen Seitenwechsel auslösen. */
  public void configure(Button downloadCodesButton, Runnable downloadFailedAction) {
    downloadCodesButton.addAttachListener(event ->
        event.getUI().getPage().addJavaScript("context://custom-theme/recovery-code-download.js"));
    downloadCodesButton.addClickListener(event ->
        downloadCodesButton
            .getElement()
            .executeJs("return window.cosmosRecoveryCodeDownload(this)")
            .then(
                Boolean.class,
                success -> {
                  if (Boolean.FALSE.equals(success)) {
                    downloadFailedAction.run();
                  }
                }));
  }

  public DownloadHandler handler(UUID recoveryOutputId, Locale locale) {
    return new DownloadHandler() {
      @Override
      public String getUrlPostfix() {
        return RESOURCE_NAME;
      }

      @Override
      public void handleDownloadRequest(DownloadEvent event) {
        writeRecoveryCodesPdf(event, recoveryOutputId, locale);
      }
    };
  }

  void writeRecoveryCodesPdf(DownloadEvent downloadEvent, UUID recoveryOutputId, Locale locale) {
    downloadEvent.getResponse().setHeader("Cache-Control", "no-store");
    downloadEvent.getResponse().setHeader("Pragma", "no-cache");
    downloadEvent.getResponse().setHeader("X-Content-Type-Options", "nosniff");

    byte[] recoveryPdfBytes = null;
    try {
      HttpServletRequest httpServletRequest = (HttpServletRequest) downloadEvent.getRequest();
      List<String> recoveryCodes =
          twoFactorEnrollment.recoveryOutput(httpServletRequest, recoveryOutputId);
      String serviceNameText = configService.getString(ConfigEntry.SERVICE_NAME);
      recoveryPdfBytes =
          recoveryCodeDocument.render(
              serviceNameText,
              twoFactorAuthorization.owner(httpServletRequest).email(),
              recoveryCodes,
              Instant.now(),
              locale,
              MessageProperties.forLocale(i18nProvider, locale));

      downloadEvent.setFileName(RecoveryCodeDocument.fileName(serviceNameText));
      downloadEvent.setContentType("application/pdf");
      downloadEvent.setContentLength(recoveryPdfBytes.length);
      downloadEvent.getOutputStream().write(recoveryPdfBytes);
    } catch (IOException | RuntimeException downloadFailure) {
      log.warn("Recovery code PDF download failed", LogSafety.safeFailure(downloadFailure));
      downloadEvent
          .getResponse()
          .setStatus(downloadFailure instanceof AccessDeniedException ? 403 : 500);
    } finally {
      if (recoveryPdfBytes != null) {
        Arrays.fill(recoveryPdfBytes, (byte) 0);
      }
    }
  }
}
