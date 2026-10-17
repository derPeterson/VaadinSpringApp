package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.*;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.service.UserAvatarService;
import java.util.*;
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
    var avatars = mock(UserAvatarService.class);
    var revision = UUID.randomUUID();
    when(avatars.image(any(), eq(12L), eq(revision), eq(false)))
        .thenReturn(Optional.of(new byte[] {1, 2, 3}));
    var service = mock(VaadinServletService.class);
    var request = new VaadinServletRequest(new MockHttpServletRequest(), service);
    var response = new MockHttpServletResponse();
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
    var avatars = mock(UserAvatarService.class);
    var revision = UUID.randomUUID();
    if (code == 404)
      when(avatars.image(any(), any(), any(), anyBoolean())).thenReturn(Optional.empty());
    else
      when(avatars.image(any(), any(), any(), anyBoolean()))
          .thenThrow(
              switch (code) {
                case 401 -> new AuthenticationCredentialsNotFoundException("revoked");
                case 503 -> new MaintenanceAccessException("maintenance");
                default -> new AccessDeniedException("forbidden");
              });
    var service = mock(VaadinServletService.class);
    var response = new MockHttpServletResponse();
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
