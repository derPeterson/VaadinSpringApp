package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.AttachEvent;
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
import org.springframework.security.access.AccessDeniedException;

@StyleSheet("custom-theme/cosmos-account.css")
@Route(AppRouteConstants.NOTIFICATIONS_ROUTE)
@PageTitle(AppRouteConstants.NOTIFICATIONS_PAGE_TITLE)
@UIScope
@PermitAll
public class UserNotificationsView extends IsAuthenticatedBaseView {
  private final UserNotificationsPanel notificationInboxPanel;
  private final PortalContentComponent notificationsPortalContentComponent;
  private final Span portalBreadcrumbSpan = new Span();
  private final Span notificationsTitleSpan = new Span();
  private final Span notificationsDescriptionSpan = new Span();

  private transient Registration inboxUpdateRegistration;

  public UserNotificationsView(
      MessageProperties messageProperties,
      SecurityService securityService,
      UserService userService,
      UserStatusBroadcaster userStatusBroadcaster,
      HttpServletRequest httpServletRequest,
      UserAvatarService userAvatarService,
      UserNotificationService userNotificationService,
      ConfigService configService) {
    super(
        messageProperties,
        securityService,
        userService,
        userStatusBroadcaster,
        httpServletRequest,
        userAvatarService);

    addClassNames("cosmos-page", "cosmos-theme", "cosmos-app-shell", REFERENCE_PORTAL);
    HorizontalLayout applicationHeaderHorizontalLayout = createApplicationHeader(configService);
    HttpSession inboxHttpSession = httpServletRequest.getSession(false);
    notificationInboxPanel =
        new UserNotificationsPanel(
            messageProperties, userNotificationService, inboxHttpSession, false);

    CosmosSectionHeaderComponent notificationsSectionHeaderComponent =
        new CosmosSectionHeaderComponent(
            portalBreadcrumbSpan, notificationsTitleSpan, notificationsDescriptionSpan);
    CosmosCardComponent inboxCardComponent = new CosmosCardComponent(notificationInboxPanel);
    inboxCardComponent.addClassNames(PANEL_CARD, "cosmos-notification-inbox-card");
    notificationsPortalContentComponent =
        new PortalContentComponent(
            messageProperties,
            currentUser,
            AppRoute.NOTIFICATIONS,
            notificationsSectionHeaderComponent,
            inboxCardComponent,
            userAvatarService,
            httpServletRequest);
    notificationsPortalContentComponent.configureNavigationToggle(accountNavigationToggle);
    notificationsPortalContentComponent
        .getSidebar()
        .configureStatusSelection(userPopoverMenu.createStatusSelect());
    accountHeaderActions.setUnreadCountListener(
        notificationsPortalContentComponent.getSidebar()::updateUnreadCount);
    setPortalPage(
        applicationHeaderHorizontalLayout, notificationsPortalContentComponent, configService);

    addAttachListener(inboxAttachEvent ->
        subscribeAndRefreshNotificationInbox(
            inboxAttachEvent, userNotificationService, inboxHttpSession));
    addDetachListener(_ -> unsubscribeFromNotificationInboxUpdates());
    onTrackedUserLanguageChanged();
  }

  private void subscribeAndRefreshNotificationInbox(
      AttachEvent inboxAttachEvent,
      UserNotificationService userNotificationService,
      HttpSession inboxHttpSession) {
    try {
      Long inboxOwnerUserId = userNotificationService.ownerId(inboxHttpSession);
      inboxUpdateRegistration =
          userNotificationService
              .events()
              .register(affectedUserIds -> {
                if (affectedUserIds.contains(inboxOwnerUserId)) {
                  inboxAttachEvent.getUI().access(this::refreshNotificationInboxIfAttached);
                }
              });
      notificationInboxPanel.refreshInbox();
    } catch (AccessDeniedException _) {
      notificationInboxPanel.unavailable();
    }
  }

  private void refreshNotificationInboxIfAttached() {
    if (isAttached()) {
      notificationInboxPanel.refreshInbox();
    }
  }

  private void unsubscribeFromNotificationInboxUpdates() {
    if (inboxUpdateRegistration != null) {
      inboxUpdateRegistration.remove();
      inboxUpdateRegistration = null;
    }
  }

  @Override
  protected void updateHeaderStatusIcon(UserStatus userStatus) {
    super.updateHeaderStatusIcon(userStatus);
    if (notificationsPortalContentComponent != null) {
      notificationsPortalContentComponent.updateStatus(userStatus);
    }
  }

  @Override
  protected void onTrackedUserProfileChanged(String firstName, String lastName) {
    if (notificationsPortalContentComponent != null) {
      notificationsPortalContentComponent.getSidebar().refreshName(firstName, lastName);
    }
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    portalBreadcrumbSpan.setText(messageProperties.getAccountNavigationPortal());
    notificationsTitleSpan.setText(messageProperties.getNotificationsTitle());
    notificationsDescriptionSpan.setText(messageProperties.getNotificationsPageDescription());
    if (notificationInboxPanel != null) {
      notificationInboxPanel.refreshTexts();
    }
    if (notificationsPortalContentComponent != null) {
      notificationsPortalContentComponent.refreshTexts();
    }
  }
}
