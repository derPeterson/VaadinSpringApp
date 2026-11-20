package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_PORTAL;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import de.derpeterson.app.service.PortalReleaseNotes.Release;
import de.derpeterson.app.service.PortalReleaseNotes.ReleaseText;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.ui.components.CosmosBreadcrumbsComponent.ParentPath;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.PortalSidebarPageLayout;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-portal-news.css")
@Route(AppRouteConstants.NEWS_ROUTE)
@UIScope
@PermitAll
public class WhatsNewView extends IsAuthenticatedBaseView implements HasDynamicTitle {
  private final Span newsTitleSpan = new Span();
  private final Span newsTitleAccentSpan = new Span();
  private final Span newsDescriptionSpan = new Span();
  private final VerticalLayout newsContentLayout = new VerticalLayout();
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

    newsContentLayout.setId("portal-news-releases");
    newsContentLayout.setPadding(false);
    newsContentLayout.addClassName("cosmos-news-releases");
    newsPageLayout =
        new PortalSidebarPageLayout(
            messageProperties,
            currentUser,
            AppRoute.NEWS,
            newsSectionHeader,
            newsContentLayout,
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
    newsContentLayout.removeAll();
    portalReleaseNotes
        .getReleases()
        .ifPresentOrElse(
            this::showReleases,
            () ->
                newsContentLayout.add(new Paragraph(messageProperties.getPortalNewsUnavailable())));
  }

  private void showReleases(List<Release> releases) {
    if (releases.isEmpty()) {
      newsContentLayout.add(new Paragraph(messageProperties.getPortalNewsEmpty()));
      return;
    }

    Locale language = CustomI18NProvider.getCurrentLocale();
    DateTimeFormatter releaseDateFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(language);
    Release latestRelease = releases.getFirst();
    ReleaseText latestReleaseText = latestRelease.textFor(language);
    String latestVersionLabel =
        messageProperties.getPortalNewsReleaseVersion(latestRelease.version());
    String latestReleaseDate = latestRelease.releasedOn().format(releaseDateFormatter);
    H2 latestReleaseHeading =
        new H2(
            latestReleaseText.title().isBlank() ? latestVersionLabel : latestReleaseText.title());
    latestReleaseHeading.addClassName("cosmos-news-release-title");
    Span latestReleaseMetadata =
        new Span(
            latestReleaseText.title().isBlank()
                ? latestReleaseDate
                : latestVersionLabel + " · " + latestReleaseDate);
    latestReleaseMetadata.addClassName("cosmos-news-release-metadata");
    CosmosCardComponent latestReleaseCard =
        new CosmosCardComponent(
            latestReleaseHeading,
            latestReleaseMetadata,
            new Markdown(latestReleaseText.markdown()));
    latestReleaseCard.addClassNames(PANEL_CARD, "cosmos-news-release");
    newsContentLayout.add(latestReleaseCard);

    if (releases.size() > 1) {
      H2 earlierReleasesHeading = new H2(messageProperties.getPortalNewsEarlierVersions());
      earlierReleasesHeading.addClassName("cosmos-news-archive-title");
      newsContentLayout.add(earlierReleasesHeading);
      for (Release earlierRelease : releases.subList(1, releases.size())) {
        ReleaseText earlierReleaseText = earlierRelease.textFor(language);
        String summary =
            messageProperties.getPortalNewsReleaseVersion(earlierRelease.version())
                + " · "
                + earlierRelease.releasedOn().format(releaseDateFormatter);
        if (!earlierReleaseText.title().isBlank()) {
          summary = earlierReleaseText.title() + " · " + summary;
        }
        Details earlierReleaseDetails =
            new Details(summary, new Markdown(earlierReleaseText.markdown()));
        earlierReleaseDetails.setWidthFull();
        newsContentLayout.add(earlierReleaseDetails);
      }
    }
  }
}
