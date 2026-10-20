package de.derpeterson.app.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.AccountNavigationTabs;
import de.derpeterson.app.ui.components.CosmosBackgroundComponent;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosCopyrightComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.UserNotificationsPanel;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;

@Route(AppRouteConstants.NOTIFICATIONS_ROUTE)
@PageTitle(AppRouteConstants.NOTIFICATIONS_PAGE_TITLE)
@UIScope
@PermitAll
public class UserNotificationsView extends IsAuthenticatedBaseView {
  private final UserNotificationsPanel inbox;
  private final AccountNavigationTabs accountNavigation;
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
    addClassNames("cosmos-page", "cosmos-theme", "cosmos-app-shell");
    addToNavbar(createApplicationHeader(config));
    HttpSession session = request.getSession(false);
    inbox = new UserNotificationsPanel(texts, notifications, session, false);
    accountNavigation = new AccountNavigationTabs(texts, true);
    CosmosSectionHeaderComponent sectionHeader =
        new CosmosSectionHeaderComponent(overview, title, description);
    CosmosCardComponent card = new CosmosCardComponent(inbox);
    card.addClassNames("cosmos-panel-card", "cosmos-notification-inbox-card");
    Div content = new Div(sectionHeader, accountNavigation, card);
    content.addClassName("cosmos-notification-inbox-page");
    Div main =
        new Div(
            content,
            new CosmosCopyrightComponent(
                config, Clock.systemDefaultZone(), texts::getAccountCopyrightTemplate));
    main.addClassName("cosmos-app-main");
    main.getElement().setAttribute("role", "main");
    setContent(new Div(new CosmosBackgroundComponent(), main));
    addAttachListener(
        e -> {
          try {
            Long owner = notifications.ownerId(session);
            registration =
                notifications
                    .events()
                    .register(
                        ids -> {
                          if (ids.contains(owner)) {
                            e.getUI()
                                .access(
                                    () -> {
                                      if (isAttached()) {
                                        inbox.refresh();
                                      }
                                    });
                          }
                        });
            inbox.refresh();
          } catch (org.springframework.security.access.AccessDeniedException _) {
            inbox.unavailable();
          }
        });
    addDetachListener(
        e -> {
          if (registration != null) {
            registration.remove();
          }
          registration = null;
        });
    onTrackedUserLanguageChanged();
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    overview.setText(messageProperties.getNotificationsOverview());
    title.setText(messageProperties.getNotificationsTitle());
    description.setText(messageProperties.getNotificationsPageDescription());
    if (inbox != null) {
      inbox.refreshTexts();
    }
    if (accountNavigation != null) {
      accountNavigation.refreshTexts();
    }
  }
}
