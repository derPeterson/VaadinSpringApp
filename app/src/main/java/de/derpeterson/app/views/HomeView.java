package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.PortalContentComponent;
import de.derpeterson.app.ui.components.PortalHomeComponent;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

@StyleSheet("custom-theme/cosmos-account.css")
@Route(AppRouteConstants.HOME_ROUTE)
@UIScope
@PageTitle(AppRouteConstants.HOME_PAGE_TITLE)
@AnonymousAllowed
public class HomeView extends TrackedUserAppLayout implements AfterNavigationObserver {

  private final transient ConfigService configService;
  private PortalContentComponent portalContent;
  private PortalHomeComponent homeContent;

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
      UserAvatarService userAvatarService) {
    super(
        messageProperties,
        securityService,
        userService,
        userStatusBroadcaster,
        request,
        userAvatarService);

    this.configService = configService;

    addClassNames("cosmos-page", "cosmos-theme", "cosmos-app-shell", "cosmos-home");
    if (currentUser != null) {
      addClassName(REFERENCE_PORTAL);
    }
    if (currentUser == null) {
      setContent(
          new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.HOME));
    } else {
      createHomeContent();
    }
    refreshTexts();
  }

  private void createHomeContent() {
    HorizontalLayout header = createApplicationHeader(configService, createPopoverActions());
    CosmosSectionHeaderComponent introduction =
        new CosmosSectionHeaderComponent(overviewLabel, pageTitle, pageDescription);

    homeContent = new PortalHomeComponent(messageProperties);
    portalContent =
        new PortalContentComponent(
            messageProperties,
            currentUser,
            AppRoute.HOME,
            introduction,
            homeContent,
            userAvatarService,
            request);
    portalContent.configureNavigationToggle(accountNavigationToggle);
    portalContent.getSidebar().configureStatusSelection(userPopoverMenu.createStatusSelect());
    accountHeaderActions.setUnreadCountListener(count -> {
      portalContent.getSidebar().updateUnreadCount(count);
      homeContent.updateUnreadCount(count);
    });
    setPortalPage(header, portalContent, configService);
  }

  private void refreshTexts() {
    pageTitle.setText(messageProperties.getHomeTitle());
    pageDescription.setText(
        currentUser == null
            ? messageProperties.getHomeDescription()
            : messageProperties.getPortalHomeIntroduction());
    overviewLabel.setText(
        currentUser == null
            ? messageProperties.getHomeOverview()
            : messageProperties.getAccountNavigationPortal());
    if (portalContent != null) {
      portalContent.refreshTexts();
      homeContent.refreshTexts();
    }
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

  private UserPopoverMenu.UserPopoverActions createPopoverActions() {
    Runnable manageApplicationAction = null;

    if (currentUser != null && currentUser.hasAdministrativeRole()) {
      manageApplicationAction = () -> UI.getCurrent().navigate(AdminView.class);
    }

    return new UserPopoverMenu.UserPopoverActions(manageApplicationAction, null);
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
        .ifPresent(error ->
            NotificationHelper.getInstance()
                .showNotification(
                    messageProperties::getBaseFailedTitle,
                    () -> error.getMessage(messageProperties),
                    NotificationHelper.NotificationType.ERROR));

    UI.getCurrent().getPage().getHistory().replaceState(null, AppRoute.HOME.getSecurityRoute());
  }
}
