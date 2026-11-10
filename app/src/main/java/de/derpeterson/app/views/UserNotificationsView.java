package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.PortalContentComponent;
import de.derpeterson.app.ui.components.UserNotificationsPanel;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@StyleSheet("custom-theme/cosmos-account.css")
@Route(AppRouteConstants.NOTIFICATIONS_ROUTE)
@PageTitle(AppRouteConstants.NOTIFICATIONS_PAGE_TITLE)
@UIScope
@PermitAll
public class UserNotificationsView extends IsAuthenticatedBaseView {
  private final UserNotificationsPanel notificationInbox;
  private final PortalContentComponent portalContent;
  private final Span overview = new Span();
  private final Span title = new Span();
  private final Span description = new Span();
  private transient Registration registration;

  public UserNotificationsView(
      MessageProperties texts,
      SecurityService security,
      UserService users,
      UserStatusBroadcaster status,
      HttpServletRequest request,
      UserAvatarService avatars,
      UserNotificationService notifications,
      ConfigService config) {
    super(texts, security, users, status, request, avatars);
    addClassNames("cosmos-page", "cosmos-theme", "cosmos-app-shell", REFERENCE_PORTAL);
    HorizontalLayout header = createApplicationHeader(config);
    HttpSession session = request.getSession(false);
    notificationInbox = new UserNotificationsPanel(texts, notifications, session, false);
    CosmosSectionHeaderComponent sectionHeader =
        new CosmosSectionHeaderComponent(overview, title, description);
    CosmosCardComponent card = new CosmosCardComponent(notificationInbox);
    card.addClassNames(PANEL_CARD, "cosmos-notification-inbox-card");
    portalContent =
        new PortalContentComponent(
            texts, currentUser, AppRoute.NOTIFICATIONS, sectionHeader, card, avatars, request);
    portalContent.configureNavigationToggle(accountNavigationToggle);
    portalContent.getSidebar().configureStatusSelection(userPopoverMenu.createStatusSelect());
    accountHeaderActions.setUnreadCountListener(portalContent.getSidebar()::updateUnreadCount);
    setPortalPage(header, portalContent, config);
    addAttachListener(e -> {
      try {
        Long owner = notifications.ownerId(session);
        registration =
            notifications
                .events()
                .register(ids -> {
                  if (ids.contains(owner)) {
                    e.getUI()
                        .access(() -> {
                          if (isAttached()) {
                            notificationInbox.refreshInbox();
                          }
                        });
                  }
                });
        notificationInbox.refreshInbox();
      } catch (org.springframework.security.access.AccessDeniedException _) {
        notificationInbox.unavailable();
      }
    });
    addDetachListener(e -> {
      if (registration != null) {
        registration.remove();
      }
      registration = null;
    });
    onTrackedUserLanguageChanged();
  }

  @Override
  protected void updateHeaderStatusIcon(UserStatus status) {
    super.updateHeaderStatusIcon(status);
    if (portalContent != null) {
      portalContent.updateStatus(status);
    }
  }

  @Override
  protected void onTrackedUserProfileChanged(String first, String last) {
    if (portalContent != null) {
      portalContent.getSidebar().refreshName(first, last);
    }
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    overview.setText(messageProperties.getAccountNavigationPortal());
    title.setText(messageProperties.getNotificationsTitle());
    description.setText(messageProperties.getNotificationsPageDescription());
    if (notificationInbox != null) {
      notificationInbox.refreshTexts();
    }
    if (portalContent != null) {
      portalContent.refreshTexts();
    }
  }
}
