package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_PORTAL;

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
import de.derpeterson.app.ui.components.CosmosBreadcrumbsComponent.ParentPath;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.PortalHomeComponent;
import de.derpeterson.app.ui.components.PortalSidebarPageLayout;
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
  private PortalSidebarPageLayout homePortalContentComponent;
  private PortalHomeComponent portalHomeComponent;

  private final Span homeTitleSpan = new Span();
  private final Span homeDescriptionSpan = new Span();

  @Autowired
  public HomeView(
      MessageProperties messageProperties,
      SecurityService securityService,
      HttpServletRequest httpServletRequest,
      ConfigService configService,
      UserService userService,
      UserStatusBroadcaster userStatusBroadcaster,
      UserAvatarService userAvatarService) {
    super(
        messageProperties,
        securityService,
        userService,
        userStatusBroadcaster,
        httpServletRequest,
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
      showAuthenticatedHomePage();
    }

    refreshHomePageTexts();
  }

  private void showAuthenticatedHomePage() {
    HorizontalLayout applicationHeaderHorizontalLayout =
        createApplicationHeader(configService, createHomePopoverActions());
    CosmosSectionHeaderComponent homeSectionHeaderComponent =
        new CosmosSectionHeaderComponent(
            messageProperties, ParentPath.PORTAL, homeTitleSpan, homeDescriptionSpan);

    portalHomeComponent = new PortalHomeComponent(messageProperties);
    homePortalContentComponent =
        new PortalSidebarPageLayout(
            messageProperties,
            currentUser,
            AppRoute.HOME,
            homeSectionHeaderComponent,
            portalHomeComponent,
            userAvatarService,
            request);

    homePortalContentComponent.configureNavigationToggle(accountNavigationToggle);
    homePortalContentComponent
        .getSidebar()
        .configureStatusSelection(userPopoverMenu.createStatusSelect());

    accountHeaderActions.setUnreadCountListener(unreadNotificationCount -> {
      homePortalContentComponent.getSidebar().updateUnreadCount(unreadNotificationCount);
      portalHomeComponent.updateUnreadCount(unreadNotificationCount);
    });

    setPortalPage(applicationHeaderHorizontalLayout, homePortalContentComponent, configService);
  }

  private void refreshHomePageTexts() {
    homeTitleSpan.setText(messageProperties.getHomeTitle());
    homeDescriptionSpan.setText(
        currentUser == null
            ? messageProperties.getHomeDescription()
            : messageProperties.getPortalHomeIntroduction());

    if (homePortalContentComponent != null) {
      homePortalContentComponent.refreshTexts();
      portalHomeComponent.refreshTexts();
    }
  }

  @Override
  protected void updateHeaderStatusIcon(UserStatus userStatus) {
    super.updateHeaderStatusIcon(userStatus);
    if (homePortalContentComponent != null) {
      homePortalContentComponent.updateStatus(userStatus);
    }
  }

  @Override
  protected void onTrackedUserProfileChanged(String firstName, String lastName) {
    if (homePortalContentComponent != null) {
      homePortalContentComponent.getSidebar().refreshName(firstName, lastName);
    }
  }

  private UserPopoverMenu.UserPopoverActions createHomePopoverActions() {
    Runnable openAdministrationAction = null;

    if (currentUser != null && currentUser.hasAdministrativeRole()) {
      openAdministrationAction = () -> UI.getCurrent().navigate(AdminView.class);
    }

    return new UserPopoverMenu.UserPopoverActions(openAdministrationAction, null);
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    refreshHomePageTexts();
  }

  @Override
  public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
    Map<String, List<String>> queryParameters =
        afterNavigationEvent.getLocation().getQueryParameters().getParameters();
    List<String> navigationErrorParameterValues = queryParameters.get("error");

    if (navigationErrorParameterValues == null || navigationErrorParameterValues.isEmpty()) {
      return;
    }

    String navigationErrorCodeText = navigationErrorParameterValues.getFirst();

    NavigationErrorCode.fromCode(navigationErrorCodeText)
        .ifPresent(error ->
            NotificationHelper.getInstance()
                .showNotification(
                    messageProperties::getBaseFailedTitle,
                    () -> error.getMessage(messageProperties),
                    NotificationHelper.NotificationType.ERROR));

    UI.getCurrent().getPage().getHistory().replaceState(null, AppRoute.HOME.getSecurityRoute());
  }
}
