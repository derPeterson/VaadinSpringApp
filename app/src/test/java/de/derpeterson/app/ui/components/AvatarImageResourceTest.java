package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.service.UserAvatarService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

class AvatarImageResourceTest {
  @Test
  void imageResponseUsesPngPrivateCacheAndInlineDisposition() throws Exception {
    UserAvatarService userAvatarService = mock(UserAvatarService.class);
    UUID avatarRevisionUuid = UUID.randomUUID();
    when(userAvatarService.image(any(), eq(12L), eq(avatarRevisionUuid), eq(false)))
        .thenReturn(Optional.of(new byte[] {1, 2, 3}));

    VaadinServletService vaadinServletService = mock(VaadinServletService.class);
    VaadinServletRequest vaadinServletRequest =
        new VaadinServletRequest(new MockHttpServletRequest(), vaadinServletService);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();

    AvatarImageResource.createAvatarImageDownloadHandler(
            userAvatarService, 12L, avatarRevisionUuid, false)
        .handleRequest(
            vaadinServletRequest,
            new VaadinServletResponse(mockHttpResponse, vaadinServletService),
            mock(VaadinSession.class),
            new Element("img"));

    assertEquals(200, mockHttpResponse.getStatus());
    assertEquals("image/png", mockHttpResponse.getContentType());
    assertEquals("private, no-store", mockHttpResponse.getHeader("Cache-Control"));
    assertEquals("nosniff", mockHttpResponse.getHeader("X-Content-Type-Options"));
    assertTrue(mockHttpResponse.getHeader("Content-Disposition").startsWith("inline"));
    assertArrayEquals(new byte[] {1, 2, 3}, mockHttpResponse.getContentAsByteArray());
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403, 404, 503})
  void deniedOrOldResourcesReturnNoImageAndRemainUncacheable(int httpStatusCode) throws Exception {
    UserAvatarService userAvatarService = mock(UserAvatarService.class);
    UUID avatarRevisionUuid = UUID.randomUUID();
    if (httpStatusCode == 404) {
      when(userAvatarService.image(any(), any(), any(), anyBoolean())).thenReturn(Optional.empty());
    } else {
      when(userAvatarService.image(any(), any(), any(), anyBoolean()))
          .thenThrow(
              switch (httpStatusCode) {
                case 401 -> new AuthenticationCredentialsNotFoundException("revoked");
                case 503 -> new MaintenanceAccessException("maintenance");
                default -> new AccessDeniedException("forbidden");
              });
    }

    VaadinServletService vaadinServletService = mock(VaadinServletService.class);
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();

    AvatarImageResource.createAvatarImageDownloadHandler(
            userAvatarService, 12L, avatarRevisionUuid, false)
        .handleRequest(
            new VaadinServletRequest(new MockHttpServletRequest(), vaadinServletService),
            new VaadinServletResponse(mockHttpResponse, vaadinServletService),
            mock(VaadinSession.class),
            new Element("img"));

    assertEquals(httpStatusCode, mockHttpResponse.getStatus());
    assertEquals(0, mockHttpResponse.getContentAsByteArray().length);
    assertEquals("private, no-store", mockHttpResponse.getHeader("Cache-Control"));
  }
}
