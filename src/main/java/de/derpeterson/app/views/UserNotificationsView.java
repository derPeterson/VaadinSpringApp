package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.*;
import de.derpeterson.app.service.*;
import de.derpeterson.app.ui.components.*;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;

@Route(AppRouteConstants.NOTIFICATIONS_ROUTE)
@PageTitle(AppRouteConstants.NOTIFICATIONS_PAGE_TITLE)
@UIScope
@PermitAll
public class UserNotificationsView extends IsAuthenticatedBaseView {
  private final UserNotificationsPanel inbox;
  private final Span description = new Span();
  private final Button home = new Button();
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
    var brand = new Span(config.getString(ConfigEntry.SERVICE_NAME));
    brand.addClassName("cosmos-topbar-service-name");
    home.addClickListener(e -> UI.getCurrent().navigate(HomeView.class));
    createTrackedUserButton();
    createTrackedUserPopover(
        new UserPopoverMenu.Actions(
            null, () -> UI.getCurrent().navigate(HomeView.class), null, null));
    userPopoverMenu.addThemeName("cosmos");
    var right = new HorizontalLayout(home, accountHeaderActions, userIconButton);
    right.setSpacing(false);
    right.addClassName("cosmos-topbar-account-menu");
    var top = new HorizontalLayout(brand, right);
    top.setSpacing(false);
    top.addClassName("cosmos-topbar");
    addToNavbar(top);
    var session = request.getSession(false);
    inbox = new UserNotificationsPanel(texts, notifications, session, false);
    description.setText(texts.getNotificationsDescription());
    description.addClassName("cosmos-panel-description");
    var card = new CosmosCardComponent(description, inbox);
    var main =
        new Div(
            card,
            new CosmosCopyrightComponent(
                config, Clock.systemDefaultZone(), texts::getAccountCopyrightTemplate));
    main.addClassName("cosmos-app-main");
    main.getElement().setAttribute("role", "main");
    setContent(new Div(new CosmosBackgroundComponent(), main));
    addAttachListener(
        e -> {
          try {
            var owner = notifications.ownerId(session);
            registration =
                notifications
                    .events()
                    .register(
                        ids -> {
                          if (ids.contains(owner))
                            e.getUI()
                                .access(
                                    () -> {
                                      if (isAttached()) inbox.refresh();
                                    });
                        });
            inbox.refresh();
          } catch (org.springframework.security.access.AccessDeniedException denied) {
            inbox.unavailable();
          }
        });
    addDetachListener(
        e -> {
          if (registration != null) registration.remove();
          registration = null;
        });
    onTrackedUserLanguageChanged();
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    home.setText(messageProperties.getHomeTitle());
    description.setText(messageProperties.getNotificationsDescription());
    if (inbox != null) inbox.refreshTexts();
  }
}
