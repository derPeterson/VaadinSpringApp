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

  public static DownloadHandler createAvatarImageDownloadHandler(
      UserAvatarService userAvatarService,
      Long avatarOwnerId,
      UUID imageRevision,
      boolean allowEditorDownload) {
    return downloadEvent -> {
      downloadEvent.getResponse().setHeader("Cache-Control", "private, no-store");
      downloadEvent.getResponse().setHeader("X-Content-Type-Options", "nosniff");
      downloadEvent
          .getResponse()
          .setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
      try {
        Optional<byte[]> avatarImageBytes =
            userAvatarService.image(
                (HttpServletRequest) downloadEvent.getRequest(),
                avatarOwnerId,
                imageRevision,
                allowEditorDownload);
        if (avatarImageBytes.isEmpty()) {
          downloadEvent.getResponse().setStatus(404);
          return;
        }
        downloadEvent.inline("avatar.png");
        downloadEvent.setContentType("image/png");
        downloadEvent.setContentLength(avatarImageBytes.get().length);
        downloadEvent.getOutputStream().write(avatarImageBytes.get());
      } catch (MaintenanceAccessException _) {
        downloadEvent.getResponse().setStatus(503);
      } catch (AuthenticationCredentialsNotFoundException _) {
        downloadEvent.getResponse().setStatus(401);
      } catch (AccessDeniedException _) {
        downloadEvent.getResponse().setStatus(403);
      }
    };
  }
}
