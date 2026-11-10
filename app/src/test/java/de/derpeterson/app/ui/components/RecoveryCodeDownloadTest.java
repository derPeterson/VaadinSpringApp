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
  private final DownloadEvent event = mock(DownloadEvent.class);
  private final VaadinServletRequest request = mock(VaadinServletRequest.class);
  private final VaadinResponse vaadinResponse = mock(VaadinResponse.class);
  private final UUID id = UUID.randomUUID();
  private final List<String> codes = List.of("SYNTHETIC-RECOVERY-CODE");

  @BeforeEach
  void prepare() {
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("My / Service");
    when(event.getRequest()).thenReturn(request);
    when(event.getResponse()).thenReturn(vaadinResponse);
    when(twoFactorEnrollment.recoveryOutput(request, id)).thenReturn(codes);
    TwoFactorAuthorization.Owner owner = mock(TwoFactorAuthorization.Owner.class);
    when(twoFactorAuthorization.owner(request)).thenReturn(owner);
    when(owner.email()).thenReturn("fixture@example.test");
  }

  @Test
  void successDownloadsPdfWithoutConsumingCodesAndClearsTemporaryBytes() {
    byte[] bytes = "%PDF-synthetic".getBytes(StandardCharsets.US_ASCII);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    when(event.getOutputStream()).thenReturn(output);
    when(recoveryCodeDocument.render(any(), any(), eq(codes), any(), eq(Locale.GERMAN), any()))
        .thenReturn(bytes);

    recoveryCodeDownload.write(event, id, Locale.GERMAN);

    assertArrayEquals("%PDF-synthetic".getBytes(StandardCharsets.US_ASCII), output.toByteArray());
    assertArrayEquals(new byte[bytes.length], bytes);
    verify(event).setFileName("My-Service-recovery-codes.pdf");
    verify(event).setContentType("application/pdf");
    verify(vaadinResponse).setHeader("Cache-Control", "no-store");
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void generationFailureKeepsOutputAvailableForRetry() {
    when(recoveryCodeDocument.render(any(), any(), eq(codes), any(), eq(Locale.GERMAN), any()))
        .thenThrow(new IllegalStateException("Synthetic renderer failure"))
        .thenReturn("%PDF-retry".getBytes(StandardCharsets.US_ASCII));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    when(event.getOutputStream()).thenReturn(output);

    recoveryCodeDownload.write(event, id, Locale.GERMAN);
    verify(vaadinResponse).setStatus(500);
    recoveryCodeDownload.write(event, id, Locale.GERMAN);

    assertArrayEquals("%PDF-retry".getBytes(StandardCharsets.US_ASCII), output.toByteArray());
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void expiredOrRevokedOutputRemainsDenied() {
    when(twoFactorEnrollment.recoveryOutput(request, id))
        .thenThrow(new AccessDeniedException("Expired"));

    recoveryCodeDownload.write(event, id, Locale.GERMAN);

    verify(vaadinResponse).setStatus(403);
    verify(event, never()).setContentLength(anyLong());
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void transportFailureReportsErrorWithoutDismissingCodes() throws IOException {
    byte[] bytes = "%PDF-synthetic".getBytes(StandardCharsets.US_ASCII);
    OutputStream output = mock(OutputStream.class);
    doThrow(new IOException("Synthetic disconnected client")).when(output).write(bytes);
    when(event.getOutputStream()).thenReturn(output);
    when(recoveryCodeDocument.render(any(), any(), eq(codes), any(), eq(Locale.GERMAN), any()))
        .thenReturn(bytes);

    recoveryCodeDownload.write(event, id, Locale.GERMAN);

    verify(vaadinResponse).setStatus(500);
    assertArrayEquals(new byte[bytes.length], bytes);
    verify(twoFactorEnrollment, never()).dismissOutput(any(), any());
  }
}
