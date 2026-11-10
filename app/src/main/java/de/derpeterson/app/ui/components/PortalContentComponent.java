package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.service.UserAvatarService;
import jakarta.servlet.http.HttpServletRequest;

/** Portalrahmen für Bereiche außerhalb des eigenen Kontobinders. */
public class PortalContentComponent extends PortalPageComponent {
  private final PortalSidebarComponent portalSidebar;
  private final AccountAvatarDialog avatarDialog;

  public PortalContentComponent(
      MessageProperties messageProperties,
      UserEntity user,
      AppRoute route,
      Component heading,
      Component content,
      UserAvatarService avatars,
      HttpServletRequest request) {
    avatarDialog = new AccountAvatarDialog(messageProperties, user);
    avatarDialog.configure(avatars, request);
    portalSidebar =
        new PortalSidebarComponent(
            messageProperties,
            user,
            route,
            PortalSidebarComponent.accountLinks(messageProperties),
            avatarDialog::open);
    portalSidebar.configureAvatars(avatars);
    setPortalContent(heading, portalSidebar, content);
    add(avatarDialog);
  }

  public PortalSidebarComponent getSidebar() {
    return portalSidebar;
  }

  public void updateStatus(UserStatus status) {
    portalSidebar.updateStatus(status);
  }

  public void refreshTexts() {
    portalSidebar.refreshTexts();
    avatarDialog.refreshTexts();
  }
}
