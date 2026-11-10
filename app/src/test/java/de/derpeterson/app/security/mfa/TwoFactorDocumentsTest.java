package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.service.ConfigService;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class TwoFactorDocumentsTest {
  private final TwoFactorEnrollment twoFactorEnrollment = mock(TwoFactorEnrollment.class);
  private final CustomI18NProvider i18n = new CustomI18NProvider();
  private final TwoFactorDocuments documents =
      new TwoFactorDocuments(
          twoFactorEnrollment,
          mock(TwoFactorAuthorization.class),
          mock(ConfigService.class),
          i18n,
          mock(RecoveryCodeDocument.class));

  @Test
  void manualKeyIsDeliveredWithoutCachingAndSecretBufferIsCleared() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    UUID id = UUID.randomUUID();
    byte[] secret = new byte[20];
    Arrays.fill(secret, (byte) 1);
    when(twoFactorEnrollment.pendingSecret(request, id)).thenReturn(secret);

    ResponseEntity<byte[]> response = documents.manualKey(request, id);

    assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
    assertEquals("no-store", response.getHeaders().getCacheControl());
    assertEquals("no-cache", response.getHeaders().getPragma());
    assertEquals(
        "default-src 'none'; style-src 'self'; frame-ancestors 'self'",
        response.getHeaders().getFirst("Content-Security-Policy"));
    assertEquals(
        "AEAQCAIBAEAQCAIBAEAQCAIBAEAQCAIB",
        new String(response.getBody(), StandardCharsets.US_ASCII));
    assertArrayEquals(new byte[20], secret);
  }

  @Test
  void manualKeyCannotBeViewedWithoutAnAuthorizedPendingSetup() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    UUID id = UUID.randomUUID();
    when(twoFactorEnrollment.pendingSecret(request, id))
        .thenThrow(new AccessDeniedException("Expired setup"));

    assertThrows(AccessDeniedException.class, () -> documents.manualKey(request, id));
    assertEquals(403, documents.denied().getStatusCode().value());
    assertEquals("no-store", documents.denied().getHeaders().getCacheControl());
  }

  @Test
  void recoveryCodesAreDeliveredAsPlainTextWithoutCaching() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath("/portal");
    UUID id = UUID.randomUUID();
    List<String> codes =
        List.of("12345678-12345678-12345678-12345678", "ABCDEF12-ABCDEF12-ABCDEF12-ABCDEF12");
    when(twoFactorEnrollment.recoveryOutput(request, id)).thenReturn(codes);

    ResponseEntity<byte[]> response = documents.codes(request, id);

    assertEquals("no-store", response.getHeaders().getCacheControl());
    assertEquals("no-cache", response.getHeaders().getPragma());
    assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
    assertEquals(
        String.join("\n", codes), new String(response.getBody(), StandardCharsets.US_ASCII));
  }

  @Test
  void recoveryDocumentCannotBeViewedWithoutAnAuthorizedOutput() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    UUID id = UUID.randomUUID();
    when(twoFactorEnrollment.recoveryOutput(request, id))
        .thenThrow(new AccessDeniedException("Expired output"));

    assertThrows(AccessDeniedException.class, () -> documents.codes(request, id));
  }
}
