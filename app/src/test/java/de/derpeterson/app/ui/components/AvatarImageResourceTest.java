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
  void imageResponseHasFixedTypePrivateCacheAndNoOriginalFilename() throws Exception {
    UserAvatarService avatars = mock(UserAvatarService.class);
    UUID revision = UUID.randomUUID();
    when(avatars.image(any(), eq(12L), eq(revision), eq(false)))
        .thenReturn(Optional.of(new byte[] {1, 2, 3}));
    VaadinServletService service = mock(VaadinServletService.class);
    VaadinServletRequest request = new VaadinServletRequest(new MockHttpServletRequest(), service);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AvatarImageResource.handler(avatars, 12L, revision, false)
        .handleRequest(
            request,
            new VaadinServletResponse(response, service),
            mock(VaadinSession.class),
            new Element("img"));
    assertEquals(200, response.getStatus());
    assertEquals("image/png", response.getContentType());
    assertEquals("private, no-store", response.getHeader("Cache-Control"));
    assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    assertTrue(response.getHeader("Content-Disposition").startsWith("inline"));
    assertArrayEquals(new byte[] {1, 2, 3}, response.getContentAsByteArray());
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403, 404, 503})
  void deniedOrOldResourcesReturnNoImageAndRemainUncacheable(int code) throws Exception {
    UserAvatarService avatars = mock(UserAvatarService.class);
    UUID revision = UUID.randomUUID();
    if (code == 404) {
      when(avatars.image(any(), any(), any(), anyBoolean())).thenReturn(Optional.empty());
    } else {
      when(avatars.image(any(), any(), any(), anyBoolean()))
          .thenThrow(
              switch (code) {
                case 401 -> new AuthenticationCredentialsNotFoundException("revoked");
                case 503 -> new MaintenanceAccessException("maintenance");
                default -> new AccessDeniedException("forbidden");
              });
    }
    VaadinServletService service = mock(VaadinServletService.class);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AvatarImageResource.handler(avatars, 12L, revision, false)
        .handleRequest(
            new VaadinServletRequest(new MockHttpServletRequest(), service),
            new VaadinServletResponse(response, service),
            mock(VaadinSession.class),
            new Element("img"));
    assertEquals(code, response.getStatus());
    assertEquals(0, response.getContentAsByteArray().length);
    assertEquals("private, no-store", response.getHeader("Cache-Control"));
  }
}
