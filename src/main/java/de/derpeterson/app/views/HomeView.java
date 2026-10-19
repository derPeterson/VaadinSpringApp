package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.NavigationErrorCode;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.ui.components.CosmosBackgroundComponent;
import de.derpeterson.app.ui.components.CosmosCopyrightComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

@Route(AppRouteConstants.HOME_ROUTE)
@UIScope
@PageTitle(AppRouteConstants.HOME_PAGE_TITLE)
@AnonymousAllowed
public class HomeView extends TrackedUserAppLayout implements AfterNavigationObserver {

  private final transient ConfigService configService;

  private final Span pageTitle = new Span();
  private final Span pageDescription = new Span();
  private final Span overviewLabel = new Span();

  @Autowired
  public HomeView(
      MessageProperties messageProperties,
      SecurityService securityService,
      HttpServletRequest request,
      ConfigService configService,
      UserService userService,
      UserStatusBroadcaster userStatusBroadcaster,
      UserAvatarService avatars) {
    super(messageProperties, securityService, userService, userStatusBroadcaster, request, avatars);

    this.configService = configService;

    addClassNames("cosmos-page", "cosmos-theme", "cosmos-app-shell", "cosmos-home");
    addToNavbar(createApplicationHeader(configService, createPopoverActions()));
    setContent(createHomeContent());
    refreshTexts();
  }

  private Div createHomeContent() {
    CosmosSectionHeaderComponent introduction =
        new CosmosSectionHeaderComponent(overviewLabel, pageTitle, pageDescription);

    CosmosCopyrightComponent copyright =
        new CosmosCopyrightComponent(
            configService,
            Clock.systemDefaultZone(),
            messageProperties::getAccountCopyrightTemplate);
    Div main = new Div(introduction, copyright);
    main.addClassName("cosmos-app-main");
    main.getElement().setAttribute("role", "main");
    return new Div(new CosmosBackgroundComponent(), main);
  }

  private void refreshTexts() {
    pageTitle.setText(messageProperties.getHomeTitle());
    pageDescription.setText(messageProperties.getHomeDescription());
    overviewLabel.setText(messageProperties.getHomeOverview());
  }

  private UserPopoverMenu.Actions createPopoverActions() {
    Runnable manageApplicationAction = null;

    if (currentUser != null && currentUser.hasAdministrativeRole()) {
      manageApplicationAction = () -> UI.getCurrent().navigate(AdminView.class);
    }

    return new UserPopoverMenu.Actions(
        manageApplicationAction,
        null,
        () -> UI.getCurrent().navigate(RegistrationView.class),
        () -> UI.getCurrent().navigate(LoginView.class));
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    refreshTexts();
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    Map<String, List<String>> queryParameters =
        event.getLocation().getQueryParameters().getParameters();
    List<String> errorParameters = queryParameters.get("error");

    if (errorParameters == null || errorParameters.isEmpty()) {
      return;
    }

    String errorCode = errorParameters.getFirst();

    NavigationErrorCode.fromCode(errorCode)
        .ifPresent(
            error ->
                NotificationHelper.getInstance()
                    .showNotification(
                        messageProperties::getBaseFailedTitle,
                        () -> error.getMessage(messageProperties),
                        NotificationHelper.NotificationType.ERROR));

    UI.getCurrent().getPage().getHistory().replaceState(null, AppRoute.HOME.getSecurityRoute());
  }
}
