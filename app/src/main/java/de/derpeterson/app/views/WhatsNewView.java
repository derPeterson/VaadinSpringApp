package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.security.IsAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.PortalReleaseNotes;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.CosmosBreadcrumbsComponent.ParentPath;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.PortalReleaseNotesComponent;
import de.derpeterson.app.ui.components.PortalSidebarPageLayout;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;

@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-portal-news.css")
@Route(AppRouteConstants.NEWS_ROUTE)
@UIScope
@PermitAll
public class WhatsNewView extends IsAuthenticatedBaseView implements HasDynamicTitle {
  private final Span newsTitleSpan = new Span();
  private final Span newsTitleAccentSpan = new Span();
  private final Span newsDescriptionSpan = new Span();
  private final PortalReleaseNotesComponent releaseNotesComponent;
  private final PortalReleaseNotes portalReleaseNotes;
  private final PortalSidebarPageLayout newsPageLayout;

  public WhatsNewView(
      MessageProperties messageProperties,
      SecurityService securityService,
      UserService userService,
      UserStatusBroadcaster userStatusBroadcaster,
      HttpServletRequest httpServletRequest,
      UserAvatarService userAvatarService,
      ConfigService configService,
      PortalReleaseNotes portalReleaseNotes) {
    super(
        messageProperties,
        securityService,
        userService,
        userStatusBroadcaster,
        httpServletRequest,
        userAvatarService);

    this.portalReleaseNotes = portalReleaseNotes;
    addClassNames(
        "cosmos-page", "cosmos-theme", "cosmos-app-shell", REFERENCE_PORTAL, "cosmos-news-view");
    HorizontalLayout applicationHeaderLayout = createApplicationHeader(configService);
    CosmosSectionHeaderComponent newsSectionHeader =
        new CosmosSectionHeaderComponent(
            messageProperties,
            ParentPath.PORTAL,
            newsTitleSpan,
            newsTitleAccentSpan,
            newsDescriptionSpan);

    releaseNotesComponent = new PortalReleaseNotesComponent(messageProperties);
    newsPageLayout =
        new PortalSidebarPageLayout(
            messageProperties,
            currentUser,
            AppRoute.NEWS,
            newsSectionHeader,
            releaseNotesComponent,
            userAvatarService,
            httpServletRequest);
    newsPageLayout.configureNavigationToggle(accountNavigationToggle);
    newsPageLayout.getSidebar().configureStatusSelection(userPopoverMenu.createStatusSelect());
    accountHeaderActions.setUnreadCountListener(newsPageLayout.getSidebar()::updateUnreadCount);

    setPortalPage(applicationHeaderLayout, newsPageLayout, configService);
    onTrackedUserLanguageChanged();
  }

  @Override
  public String getPageTitle() {
    return messageProperties.getPortalNewsTitle();
  }

  @Override
  protected void updateHeaderStatusIcon(UserStatus userStatus) {
    super.updateHeaderStatusIcon(userStatus);
    if (newsPageLayout != null) {
      newsPageLayout.updateStatus(userStatus);
    }
  }

  @Override
  protected void onTrackedUserProfileChanged(String firstName, String lastName) {
    if (newsPageLayout != null) {
      newsPageLayout.getSidebar().refreshName(firstName, lastName);
    }
  }

  @Override
  protected void onTrackedUserLanguageChanged() {
    String newsTitle = getPageTitle();
    int accentStart = newsTitle.endsWith("?") ? newsTitle.length() - 1 : newsTitle.length();
    newsTitleSpan.setText(newsTitle.substring(0, accentStart));
    newsTitleAccentSpan.setText(newsTitle.substring(accentStart));
    newsDescriptionSpan.setText(messageProperties.getPortalNewsDescription());
    getUI().ifPresent(ui -> ui.getPage().setTitle(newsTitle));
    newsPageLayout.refreshTexts();
    releaseNotesComponent.showReleases(
        portalReleaseNotes.getReleases(), CustomI18NProvider.getCurrentLocale());
  }
}
