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
  private final PortalSidebarComponent sidebar;
  private final AccountAvatarDialog avatarDialog;

  public PortalContentComponent(
      MessageProperties texts,
      UserEntity user,
      AppRoute route,
      Component heading,
      Component content,
      UserAvatarService avatars,
      HttpServletRequest request) {
    avatarDialog = new AccountAvatarDialog(texts, user);
    avatarDialog.configure(avatars, request);
    sidebar =
        new PortalSidebarComponent(
            texts, user, route, PortalSidebarComponent.accountLinks(texts), avatarDialog::open);
    sidebar.configureAvatars(avatars);
    setPortalContent(heading, sidebar, content);
    add(avatarDialog);
  }

  public PortalSidebarComponent getSidebar() {
    return sidebar;
  }

  public void updateStatus(UserStatus status) {
    sidebar.updateStatus(status);
  }

  public void refreshTexts() {
    sidebar.refreshTexts();
    avatarDialog.refreshTexts();
  }
}
