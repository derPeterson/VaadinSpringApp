package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.PortalReleaseNotes.FeaturedRelease;
import de.derpeterson.app.service.PortalReleaseNotes.Highlight;
import de.derpeterson.app.service.PortalReleaseNotes.MarkdownDownload;
import de.derpeterson.app.service.PortalReleaseNotes.Release;
import de.derpeterson.app.service.PortalReleaseNotes.ReleaseText;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Stellt die neueste Ausgabe ausführlich dar und bietet ältere Ausgaben zum Aufklappen an. */
public class PortalReleaseNotesComponent extends VerticalLayout {
  private static final String DOWNLOAD_URL_ATTRIBUTE = "data-download-url";
  private static final List<AppIcon> DIGIT_ICONS =
      List.of(
          AppIcon.NUMBER_ZERO,
          AppIcon.NUMBER_ONE,
          AppIcon.NUMBER_TWO,
          AppIcon.NUMBER_THREE,
          AppIcon.NUMBER_FOUR,
          AppIcon.NUMBER_FIVE,
          AppIcon.NUMBER_SIX,
          AppIcon.NUMBER_SEVEN,
          AppIcon.NUMBER_EIGHT,
          AppIcon.NUMBER_NINE);

  private final transient MessageProperties messageProperties;
  private final transient SecurityService securityService;

  public PortalReleaseNotesComponent(
      MessageProperties messageProperties, SecurityService securityService) {
    this.messageProperties = messageProperties;
    this.securityService = securityService;
    setId("portal-news-releases");
    addClassName("cosmos-news-releases");
    setPadding(false);
  }

  public void showReleases(Optional<List<Release>> releases, Locale language) {
    removeAll();
    getElement().setAttribute("lang", language.toLanguageTag());

    if (releases.isEmpty()) {
      add(new Paragraph(messageProperties.getPortalNewsUnavailable()));
      return;
    }
    List<Release> availableReleases = releases.orElseThrow();
    if (availableReleases.isEmpty()) {
      add(new Paragraph(messageProperties.getPortalNewsEmpty()));
      return;
    }

    DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(language);
    Release latestRelease = availableReleases.getFirst();
    showLatestRelease(latestRelease, language, dateFormatter);
    if (availableReleases.size() > 1) {
      showArchive(availableReleases.subList(1, availableReleases.size()), language, dateFormatter);
    }
  }

  private void showLatestRelease(
      Release release, Locale language, DateTimeFormatter dateFormatter) {
    ReleaseText releaseText = release.textFor(language);
    String versionLabel = messageProperties.getPortalNewsReleaseVersion(release.version());
    Span dateSpan = new Span(release.releasedOn().format(dateFormatter));
    dateSpan.addClassName("cosmos-news-release-date");
    HorizontalLayout metadataLayout = new HorizontalLayout();
    metadataLayout.addClassName("cosmos-news-release-metadata");
    metadataLayout.setAlignItems(Alignment.CENTER);
    if (releaseText.title().isBlank()) {
      metadataLayout.add(dateSpan);
    } else {
      Badge versionBadge = new Badge(versionLabel);
      versionBadge.addClassName("cosmos-panel-meta");
      metadataLayout.add(versionBadge, buildSeparatedMetadataSpan(dateSpan));
    }

    H2 releaseTitleHeading =
        new H2(releaseText.title().isBlank() ? versionLabel : releaseText.title());
    releaseTitleHeading.addClassName("cosmos-news-release-title");
    Button downloadButton = buildDownloadButton(release, language);
    HorizontalLayout releaseHeaderLayout = new HorizontalLayout(metadataLayout, downloadButton);
    releaseHeaderLayout.addClassName("cosmos-news-release-header");
    releaseHeaderLayout.setWidthFull();
    releaseHeaderLayout.setAlignItems(Alignment.CENTER);
    CosmosCardComponent releaseCard =
        new CosmosCardComponent(releaseHeaderLayout, releaseTitleHeading);
    releaseCard.addClassNames(PANEL_CARD, "cosmos-news-release");
    releaseCard.setAriaLabelledBy(releaseTitleHeading);
    add(releaseCard);

    if (releaseText.featured().isEmpty()) {
      releaseCard.add(new Markdown(releaseText.markdown()));
      return;
    }

    FeaturedRelease featuredRelease = releaseText.featured().orElseThrow();
    if (!featuredRelease.introduction().isBlank()) {
      releaseCard.add(new Markdown(featuredRelease.introduction()));
    }
    showHighlights(featuredRelease.highlightsHeading(), featuredRelease.highlights());
    showImprovements(featuredRelease);
  }

  private Button buildDownloadButton(Release release, Locale language) {
    MarkdownDownload markdownDownload = release.markdownDownload(language);
    Button downloadButton =
        new Button(messageProperties.getPortalNewsDownloadMarkdown(), AppIcon.DOWNLOAD.create());
    downloadButton.addThemeVariants(ButtonVariant.SMALL);
    downloadButton
        .getElement()
        .setAttribute(
            DOWNLOAD_URL_ATTRIBUTE,
            DownloadHandler.fromInputStream(
                event -> markdownDownloadResponse(event, markdownDownload),
                markdownDownload.fileName()));
    downloadButton.addClickListener(event ->
        downloadButton
            .getUI()
            .ifPresent(ui ->
                ui.getPage()
                    .setLocation(
                        downloadButton.getElement().getAttribute(DOWNLOAD_URL_ATTRIBUTE))));
    return downloadButton;
  }

  DownloadResponse markdownDownloadResponse(
      DownloadEvent event, MarkdownDownload markdownDownload) {
    event.getResponse().setHeader("Cache-Control", "no-store");
    HttpServletRequest httpRequest = (HttpServletRequest) event.getRequest();
    if (securityService.getAuthenticatedUser(httpRequest).isEmpty()) {
      return DownloadResponse.error(HttpServletResponse.SC_UNAUTHORIZED);
    }

    byte[] markdownBytes = markdownDownload.markdown().getBytes(StandardCharsets.UTF_8);
    return new DownloadResponse(
        new ByteArrayInputStream(markdownBytes),
        markdownDownload.fileName(),
        "text/markdown; charset=UTF-8",
        markdownBytes.length);
  }

  private void showHighlights(String heading, List<Highlight> highlights) {
    H3 highlightsHeading = buildSectionHeading(AppIcon.LUCIDE_SPARKLES, heading);
    highlightsHeading.addClassName("cosmos-news-latest-section-heading");
    Div highlightsGrid = new Div();
    highlightsGrid.addClassName("cosmos-news-highlight-grid");
    for (Highlight highlight : highlights) {
      Markdown descriptionMarkdown = new Markdown(highlight.markdown());
      CosmosCardComponent highlightCard = new CosmosCardComponent();
      highlightCard.addClassNames(PANEL_CARD, "cosmos-news-highlight");

      if (!highlight.title().isBlank()) {
        H4 highlightHeading = new H4(highlight.title());
        HorizontalLayout highlightHeaderLayout = new HorizontalLayout(highlightHeading);
        highlightHeaderLayout.addClassName("cosmos-news-highlight-header");
        highlightHeaderLayout.setAlignItems(Alignment.CENTER);
        highlight
            .navigationIconName()
            .flatMap(PortalReleaseNotesComponent::findNavigationIcon)
            .ifPresent(navigationIcon -> {
              Div highlightIconDiv = new Div(navigationIcon.create());
              highlightIconDiv.addClassName(PANEL_ICON);
              highlightHeaderLayout.addComponentAsFirst(highlightIconDiv);
            });
        highlightCard.add(highlightHeaderLayout);
        highlightCard.setAriaLabelledBy(highlightHeading);
      }

      highlightCard.add(descriptionMarkdown);
      highlightsGrid.add(highlightCard);
    }
    add(highlightsHeading, highlightsGrid);
  }

  private static Optional<AppIcon> findNavigationIcon(String iconName) {
    return Arrays.stream(AppIcon.values()).filter(icon -> icon.name().equals(iconName)).findFirst();
  }

  private void showImprovements(FeaturedRelease featuredRelease) {
    H3 improvementsHeading =
        buildSectionHeading(AppIcon.CLIPBOARD_LIST, featuredRelease.improvementsHeading());
    improvementsHeading.addClassName("cosmos-news-latest-section-heading");
    Component improvementsContent = buildImprovementsContent(featuredRelease);
    CosmosCardComponent improvementsCard =
        new CosmosCardComponent(improvementsHeading, improvementsContent);
    improvementsCard.addClassNames(PANEL_CARD, "cosmos-news-improvements");
    improvementsCard.setAriaLabelledBy(improvementsHeading);
    add(improvementsCard);
  }

  private H3 buildSectionHeading(AppIcon headingIcon, String headingText) {
    H3 sectionHeading = new H3(headingIcon.create(), new Span(headingText));
    sectionHeading.addClassName("cosmos-news-section-heading");
    return sectionHeading;
  }

  private Component buildImprovementsContent(FeaturedRelease featuredRelease) {
    List<String> improvementItems = featuredRelease.improvementItems();
    if (improvementItems.isEmpty()) {
      return new Markdown(featuredRelease.improvementsMarkdown());
    }
    return buildNumberedList(improvementItems);
  }

  private OrderedList buildNumberedList(List<String> itemMarkdownTexts) {
    OrderedList numberedList = new OrderedList();
    numberedList.addClassName("cosmos-news-numbered-list");
    numberedList.getElement().setAttribute("role", "list");
    for (int itemIndex = 0; itemIndex < itemMarkdownTexts.size(); itemIndex++) {
      Span numberSpan = buildListNumberSpan(itemIndex + 1);
      numberedList.add(new ListItem(numberSpan, new Markdown(itemMarkdownTexts.get(itemIndex))));
    }
    return numberedList;
  }

  private Span buildListNumberSpan(int itemNumber) {
    Span numberSpan = new Span();
    numberSpan.addClassName("cosmos-news-list-number");
    numberSpan.getElement().setAttribute("aria-hidden", "true");
    for (char digit : Integer.toString(itemNumber).toCharArray()) {
      numberSpan.add(DIGIT_ICONS.get(digit - '0').create());
    }
    return numberSpan;
  }

  private void showArchive(
      List<Release> releases, Locale language, DateTimeFormatter dateFormatter) {
    H2 archiveHeading =
        new H2(
            AppIcon.VERSIONS.create(), new Span(messageProperties.getPortalNewsEarlierVersions()));
    archiveHeading.addClassNames("cosmos-news-section-heading", "cosmos-news-archive-title");
    add(archiveHeading);

    for (Release release : releases) {
      ReleaseText releaseText = release.textFor(language);
      String versionLabel = messageProperties.getPortalNewsReleaseVersion(release.version());
      String dateLabel = release.releasedOn().format(dateFormatter);
      Badge versionBadge = new Badge(versionLabel);
      versionBadge.addClassName("cosmos-panel-meta");
      Span archiveDateSpan = new Span(dateLabel);
      archiveDateSpan.addClassName("cosmos-news-archive-meta");
      Span summarySpan = new Span();
      summarySpan.addClassName("cosmos-news-archive-summary");
      if (releaseText.title().isBlank()) {
        summarySpan.add(versionBadge);
      } else {
        Span titleSpan = new Span(releaseText.title());
        titleSpan.addClassName("cosmos-news-archive-release-title");
        summarySpan.add(titleSpan, buildSeparatedMetadataSpan(versionBadge));
      }
      summarySpan.add(buildSeparatedMetadataSpan(archiveDateSpan));
      Details releaseDetails =
          new Details(
              summarySpan, buildArchiveContent(releaseText), buildArchiveFooter(release, language));
      releaseDetails.addClassName("cosmos-news-archive-entry");
      releaseDetails.setWidthFull();
      add(releaseDetails);
    }
  }

  private static Span buildSeparatedMetadataSpan(Component metadataComponent) {
    Span separatorSpan = new Span("·");
    separatorSpan.addClassName("cosmos-news-metadata-separator");
    separatorSpan.getElement().setAttribute("aria-hidden", "true");
    Span metadataSpan = new Span(separatorSpan, metadataComponent);
    metadataSpan.addClassName("cosmos-news-separated-metadata");
    return metadataSpan;
  }

  private VerticalLayout buildArchiveContent(ReleaseText releaseText) {
    VerticalLayout archiveContentLayout = new VerticalLayout();
    archiveContentLayout.setPadding(false);
    archiveContentLayout.addClassName("cosmos-news-archive-content");
    if (releaseText.featured().isEmpty()) {
      archiveContentLayout.add(new Markdown(releaseText.markdown()));
      return archiveContentLayout;
    }

    FeaturedRelease featuredRelease = releaseText.featured().orElseThrow();
    if (!featuredRelease.introduction().isBlank()) {
      archiveContentLayout.add(new Markdown(featuredRelease.introduction()));
    }

    List<String> highlightMarkdownTexts =
        featuredRelease.highlights().stream()
            .map(PortalReleaseNotesComponent::buildArchiveHighlightMarkdown)
            .toList();
    archiveContentLayout.add(
        buildArchiveSection(
            buildSectionHeading(AppIcon.SPARKLES, featuredRelease.highlightsHeading()),
            buildNumberedList(highlightMarkdownTexts)),
        buildArchiveSection(
            buildSectionHeading(AppIcon.CLIPBOARD_LIST, featuredRelease.improvementsHeading()),
            buildImprovementsContent(featuredRelease)));
    return archiveContentLayout;
  }

  private HorizontalLayout buildArchiveFooter(Release release, Locale language) {
    HorizontalLayout footerLayout = new HorizontalLayout(buildDownloadButton(release, language));
    footerLayout.addClassName("cosmos-news-archive-footer");
    footerLayout.setWidthFull();
    footerLayout.setJustifyContentMode(JustifyContentMode.END);
    return footerLayout;
  }

  private static String buildArchiveHighlightMarkdown(Highlight highlight) {
    String title = highlight.title();
    if (title.isBlank()) {
      return highlight.markdown();
    }
    String separator = title.endsWith(":") ? "" : ":";
    return "**" + title + separator + "**\n" + highlight.markdown();
  }

  private VerticalLayout buildArchiveSection(H3 sectionHeading, Component sectionContent) {
    VerticalLayout archiveSectionLayout = new VerticalLayout(sectionHeading, sectionContent);
    archiveSectionLayout.setPadding(false);
    archiveSectionLayout.addClassName("cosmos-news-archive-section");
    return archiveSectionLayout;
  }
}
