package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.service.UserAvatarService;
import jakarta.servlet.http.HttpServletRequest;

/** Portalrahmen mit der Konto-Seitennavigation für eigenständige Portalseiten. */
public class PortalSidebarPageLayout extends PageContentLayout {
  private final PortalSidebarComponent portalSidebar;
  private final AccountAvatarDialog avatarDialog;

  public PortalSidebarPageLayout(
      MessageProperties messageProperties,
      UserEntity user,
      AppRoute route,
      Component pageHeadingComponent,
      Component pageContentComponent,
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
    setPageContent(pageHeadingComponent, portalSidebar, pageContentComponent);
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
