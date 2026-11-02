package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.streams.DownloadEvent;
import de.derpeterson.app.i18n.CustomI18NProvider;
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
  private final TwoFactorEnrollment enrollment = mock(TwoFactorEnrollment.class);
  private final TwoFactorAuthorization authorization = mock(TwoFactorAuthorization.class);
  private final RecoveryCodeDocument pdf = mock(RecoveryCodeDocument.class);
  private final RecoveryCodeDownload download =
      new RecoveryCodeDownload(
          enrollment, authorization, mock(ConfigService.class), new CustomI18NProvider(), pdf);
  private final DownloadEvent event = mock(DownloadEvent.class);
  private final VaadinServletRequest request = mock(VaadinServletRequest.class);
  private final VaadinResponse response = mock(VaadinResponse.class);
  private final UI ui = mock(UI.class);
  private final Runnable failed = mock(Runnable.class);
  private final UUID id = UUID.randomUUID();
  private final List<String> codes = List.of("SYNTHETIC-RECOVERY-CODE");

  @BeforeEach
  void prepare() {
    when(event.getRequest()).thenReturn(request);
    when(event.getResponse()).thenReturn(response);
    when(event.getUI()).thenReturn(ui);
    when(enrollment.recoveryOutput(request, id)).thenReturn(codes);
    TwoFactorAuthorization.Owner owner = mock(TwoFactorAuthorization.Owner.class);
    when(authorization.owner(request)).thenReturn(owner);
    when(owner.email()).thenReturn("fixture@example.test");
    doAnswer(
            invocation -> {
              invocation.getArgument(0, Command.class).execute();
              return null;
            })
        .when(ui)
        .access(any(Command.class));
  }

  @Test
  void successDownloadsPdfWithoutConsumingCodesAndClearsTemporaryBytes() {
    byte[] bytes = "%PDF-synthetic".getBytes(StandardCharsets.US_ASCII);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    when(event.getOutputStream()).thenReturn(output);
    when(pdf.render(any(), any(), eq(codes), any(), eq(Locale.GERMAN), any())).thenReturn(bytes);

    download.write(event, id, Locale.GERMAN, failed);

    assertArrayEquals("%PDF-synthetic".getBytes(StandardCharsets.US_ASCII), output.toByteArray());
    assertArrayEquals(new byte[bytes.length], bytes);
    verify(event).setFileName("recovery-codes.pdf");
    verify(event).setContentType("application/pdf");
    verify(response).setHeader("Cache-Control", "no-store");
    verify(enrollment, never()).dismissOutput(any(), any());
    verify(failed, never()).run();
  }

  @Test
  void generationFailureKeepsOutputAvailableForRetry() {
    when(pdf.render(any(), any(), eq(codes), any(), eq(Locale.GERMAN), any()))
        .thenThrow(new IllegalStateException("Synthetic renderer failure"))
        .thenReturn("%PDF-retry".getBytes(StandardCharsets.US_ASCII));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    when(event.getOutputStream()).thenReturn(output);

    download.write(event, id, Locale.GERMAN, failed);
    verify(response).setStatus(500);
    verify(failed).run();
    download.write(event, id, Locale.GERMAN, failed);

    assertArrayEquals("%PDF-retry".getBytes(StandardCharsets.US_ASCII), output.toByteArray());
    verify(enrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void expiredOrRevokedOutputRemainsDenied() {
    when(enrollment.recoveryOutput(request, id)).thenThrow(new AccessDeniedException("Expired"));

    download.write(event, id, Locale.GERMAN, failed);

    verify(response).setStatus(403);
    verify(failed).run();
    verify(event, never()).setContentLength(anyLong());
    verify(enrollment, never()).dismissOutput(any(), any());
  }

  @Test
  void transportFailureReportsErrorWithoutDismissingCodes() throws IOException {
    byte[] bytes = "%PDF-synthetic".getBytes(StandardCharsets.US_ASCII);
    OutputStream output = mock(OutputStream.class);
    org.mockito.Mockito.doThrow(new IOException("Synthetic disconnected client"))
        .when(output)
        .write(bytes);
    when(event.getOutputStream()).thenReturn(output);
    when(pdf.render(any(), any(), eq(codes), any(), eq(Locale.GERMAN), any())).thenReturn(bytes);

    download.write(event, id, Locale.GERMAN, failed);

    verify(failed).run();
    verify(response).setStatus(500);
    assertArrayEquals(new byte[bytes.length], bytes);
    verify(enrollment, never()).dismissOutput(any(), any());
  }
}
