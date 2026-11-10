package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.streams.DownloadEvent;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.mfa.RecoveryCodeDocument;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorEnrollment;
import de.derpeterson.app.service.ConfigService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class RecoveryCodeDownloadTest {
  private final TwoFactorEnrollment twoFactorEnrollment = mock(TwoFactorEnrollment.class);
  private final TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
  private final RecoveryCodeDocument recoveryCodeDocument = mock(RecoveryCodeDocument.class);
  private final ConfigService configService = mock(ConfigService.class);
  private final RecoveryCodeDownload recoveryCodeDownload =
      new RecoveryCodeDownload(
          twoFactorEnrollment,
          twoFactorAuthorization,
          configService,
          new CustomI18NProvider(),
          recoveryCodeDocument);

  // Anfrage und temporäre Ersatzcode-Ausgabe
  private final DownloadEvent downloadEvent = mock(DownloadEvent.class);
  private final VaadinServletRequest vaadinServletRequest = mock(VaadinServletRequest.class);
  private final VaadinResponse vaadinResponse = mock(VaadinResponse.class);
  private final UUID recoveryOutputUuid = UUID.randomUUID();
  private final List<String> recoveryCodes = List.of("SYNTHETIC-RECOVERY-CODE");

  @BeforeEach
  void prepareRecoveryCodeDownload() {
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("My / Service");
    when(downloadEvent.getRequest()).thenReturn(vaadinServletRequest);
    when(downloadEvent.getResponse()).thenReturn(vaadinResponse);
    when(twoFactorEnrollment.recoveryOutput(vaadinServletRequest, recoveryOutputUuid))
        .thenReturn(recoveryCodes);

    TwoFactorAuthorization.Owner twoFactorOwner = mock(TwoFactorAuthorization.Owner.class);
    when(twoFactorAuthorization.owner(vaadinServletRequest)).thenReturn(twoFactorOwner);
    when(twoFactorOwner.email()).thenReturn("fixture@example.test");
  }

  @Test
  void successDownloadsPdfWithoutConsumingCodesAndClearsTemporaryBytes() {
    byte[] pdfBytes = "%PDF-synthetic".getBytes(StandardCharsets.US_ASCII);
    ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
    when(downloadEvent.getOutputStream()).thenReturn(pdfOutputStream);
    when(recoveryCodeDocument.render(
            any(), any(), eq(recoveryCodes), any(), eq(Locale.GERMAN), any()))
        .thenReturn(pdfBytes);

    recoveryCodeDownload.writeRecoveryCodesPdf(downloadEvent, recoveryOutputUuid, Locale.GERMAN);

    assertArrayEquals(
        "%PDF-synthetic".getBytes(StandardCharsets.US_ASCII), pdfOutputStream.toByteArray());
    assertArrayEquals(new byte[pdfBytes.length], pdfBytes);
    verify(downloadEvent).setFileName("My-Service-recovery-codes.pdf");
    verify(downloadEvent).setContentType("application/pdf");
    verify(vaadinResponse).setHeader("Cache-Control", "no-store");
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void generationFailureKeepsOutputAvailableForRetry() {
    when(recoveryCodeDocument.render(
            any(), any(), eq(recoveryCodes), any(), eq(Locale.GERMAN), any()))
        .thenThrow(new IllegalStateException("Synthetic renderer failure"))
        .thenReturn("%PDF-retry".getBytes(StandardCharsets.US_ASCII));
    ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
    when(downloadEvent.getOutputStream()).thenReturn(pdfOutputStream);

    recoveryCodeDownload.writeRecoveryCodesPdf(downloadEvent, recoveryOutputUuid, Locale.GERMAN);

    verify(vaadinResponse).setStatus(500);

    recoveryCodeDownload.writeRecoveryCodesPdf(downloadEvent, recoveryOutputUuid, Locale.GERMAN);

    assertArrayEquals(
        "%PDF-retry".getBytes(StandardCharsets.US_ASCII), pdfOutputStream.toByteArray());
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void expiredOrRevokedOutputRemainsDenied() {
    when(twoFactorEnrollment.recoveryOutput(vaadinServletRequest, recoveryOutputUuid))
        .thenThrow(new AccessDeniedException("Expired"));

    recoveryCodeDownload.writeRecoveryCodesPdf(downloadEvent, recoveryOutputUuid, Locale.GERMAN);

    verify(vaadinResponse).setStatus(403);
    verify(downloadEvent, never()).setContentLength(anyLong());
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void transportFailureReportsErrorWithoutDismissingCodes() throws IOException {
    byte[] pdfBytes = "%PDF-synthetic".getBytes(StandardCharsets.US_ASCII);
    OutputStream pdfOutputStream = mock(OutputStream.class);
    doThrow(new IOException("Synthetic disconnected client")).when(pdfOutputStream).write(pdfBytes);
    when(downloadEvent.getOutputStream()).thenReturn(pdfOutputStream);
    when(recoveryCodeDocument.render(
            any(), any(), eq(recoveryCodes), any(), eq(Locale.GERMAN), any()))
        .thenReturn(pdfBytes);

    recoveryCodeDownload.writeRecoveryCodesPdf(downloadEvent, recoveryOutputUuid, Locale.GERMAN);

    verify(vaadinResponse).setStatus(500);
    assertArrayEquals(new byte[pdfBytes.length], pdfBytes);
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }
}
