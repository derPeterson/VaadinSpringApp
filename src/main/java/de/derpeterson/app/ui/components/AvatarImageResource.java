package de.derpeterson.app.ui.components;

import com.vaadin.flow.server.streams.DownloadHandler;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.service.UserAvatarService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/** Jeder Abruf prüft die aktuelle Anmeldung; URLs allein gewähren keine Bildberechtigung. */
public final class AvatarImageResource {
  private AvatarImageResource() {}

  public static DownloadHandler handler(
      UserAvatarService avatars, Long userId, UUID revision, boolean editor) {
    return event -> {
      event.getResponse().setHeader("Cache-Control", "private, no-store");
      event.getResponse().setHeader("X-Content-Type-Options", "nosniff");
      event.getResponse().setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
      try {
        Optional<byte[]> image =
            avatars.image((HttpServletRequest) event.getRequest(), userId, revision, editor);
        if (image.isEmpty()) {
          event.getResponse().setStatus(404);
          return;
        }
        event.inline("avatar.png");
        event.setContentType("image/png");
        event.setContentLength(image.get().length);
        event.getOutputStream().write(image.get());
      } catch (MaintenanceAccessException blocked) {
        event.getResponse().setStatus(503);
      } catch (AuthenticationCredentialsNotFoundException denied) {
        event.getResponse().setStatus(401);
      } catch (AccessDeniedException denied) {
        event.getResponse().setStatus(403);
      }
    };
  }
}
