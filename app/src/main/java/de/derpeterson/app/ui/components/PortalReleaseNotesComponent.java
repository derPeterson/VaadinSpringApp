package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.PortalReleaseNotes.FeaturedRelease;
import de.derpeterson.app.service.PortalReleaseNotes.Highlight;
import de.derpeterson.app.service.PortalReleaseNotes.Release;
import de.derpeterson.app.service.PortalReleaseNotes.ReleaseText;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Stellt die neueste Ausgabe ausführlich dar und bietet ältere Ausgaben zum Aufklappen an. */
public class PortalReleaseNotesComponent extends VerticalLayout {
  private final transient MessageProperties messageProperties;

  public PortalReleaseNotesComponent(MessageProperties messageProperties) {
    this.messageProperties = messageProperties;
    setId("portal-news-releases");
    addClassName("cosmos-news-releases");
    setPadding(false);
  }

  public void showReleases(Optional<List<Release>> releases, Locale language) {
    removeAll();

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
    HorizontalLayout metadataLayout = new HorizontalLayout(dateSpan);
    metadataLayout.addClassName("cosmos-news-release-metadata");
    metadataLayout.setAlignItems(Alignment.CENTER);
    if (!releaseText.title().isBlank()) {
      Badge versionBadge = new Badge(versionLabel);
      versionBadge.addClassName("cosmos-news-version-badge");
      metadataLayout.addComponentAsFirst(versionBadge);
    }

    H2 releaseTitleHeading =
        new H2(releaseText.title().isBlank() ? versionLabel : releaseText.title());
    releaseTitleHeading.addClassName("cosmos-news-release-title");
    CosmosCardComponent releaseCard = new CosmosCardComponent(metadataLayout, releaseTitleHeading);
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

  private void showHighlights(String heading, List<Highlight> highlights) {
    H3 highlightsHeading = new H3(AppIcon.CHECK_CIRCLE.create(), new Span(heading));
    highlightsHeading.addClassName("cosmos-news-section-heading");
    Div highlightsGrid = new Div();
    highlightsGrid.addClassName("cosmos-news-highlight-grid");
    for (Highlight highlight : highlights) {
      Markdown descriptionMarkdown = new Markdown(highlight.markdown());
      CosmosCardComponent highlightCard = new CosmosCardComponent();
      highlightCard.addClassNames(PANEL_CARD, "cosmos-news-highlight");

      if (!highlight.title().isBlank()) {
        Div highlightIconDiv = new Div(AppIcon.CHECK.create());
        highlightIconDiv.addClassName(PANEL_ICON);
        H4 highlightHeading = new H4(highlight.title());
        HorizontalLayout highlightHeaderLayout =
            new HorizontalLayout(highlightIconDiv, highlightHeading);
        highlightHeaderLayout.addClassName("cosmos-news-highlight-header");
        highlightHeaderLayout.setAlignItems(Alignment.CENTER);
        highlightCard.add(highlightHeaderLayout);
        highlightCard.setAriaLabelledBy(highlightHeading);
      }

      highlightCard.add(descriptionMarkdown);
      highlightsGrid.add(highlightCard);
    }
    add(highlightsHeading, highlightsGrid);
  }

  private void showImprovements(FeaturedRelease featuredRelease) {
    H3 improvementsHeading =
        new H3(AppIcon.CLIPBOARD_LIST.create(), new Span(featuredRelease.improvementsHeading()));
    improvementsHeading.addClassName("cosmos-news-section-heading");
    Markdown improvementsMarkdown = new Markdown(featuredRelease.improvementsMarkdown());
    CosmosCardComponent improvementsCard =
        new CosmosCardComponent(improvementsHeading, improvementsMarkdown);
    improvementsCard.addClassNames(PANEL_CARD, "cosmos-news-improvements");
    improvementsCard.setAriaLabelledBy(improvementsHeading);
    add(improvementsCard);
  }

  private void showArchive(
      List<Release> releases, Locale language, DateTimeFormatter dateFormatter) {
    H2 archiveHeading = new H2(messageProperties.getPortalNewsEarlierVersions());
    archiveHeading.addClassName("cosmos-news-archive-title");
    add(archiveHeading);

    for (Release release : releases) {
      ReleaseText releaseText = release.textFor(language);
      String versionLabel = messageProperties.getPortalNewsReleaseVersion(release.version());
      String dateLabel = release.releasedOn().format(dateFormatter);
      Span archiveTitleSpan =
          new Span(releaseText.title().isBlank() ? versionLabel : releaseText.title());
      Span archiveMetadataSpan =
          new Span(releaseText.title().isBlank() ? dateLabel : versionLabel + " · " + dateLabel);
      archiveMetadataSpan.addClassName("cosmos-news-archive-meta");
      Span summarySpan = new Span(archiveTitleSpan, archiveMetadataSpan);
      summarySpan.addClassName("cosmos-news-archive-summary");
      Details releaseDetails = new Details(summarySpan, new Markdown(releaseText.markdown()));
      releaseDetails.addClassName("cosmos-news-archive-entry");
      releaseDetails.setWidthFull();
      add(releaseDetails);
    }
  }
}
