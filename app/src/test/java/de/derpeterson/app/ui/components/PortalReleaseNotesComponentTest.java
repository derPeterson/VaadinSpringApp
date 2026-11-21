package de.derpeterson.app.ui.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.PortalReleaseNotes;
import de.derpeterson.app.service.PortalReleaseNotes.FeaturedRelease;
import de.derpeterson.app.service.PortalReleaseNotes.MarkdownDownload;
import de.derpeterson.app.service.PortalReleaseNotes.Release;
import de.derpeterson.app.service.PortalReleaseNotes.ReleaseText;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class PortalReleaseNotesComponentTest {
  private final Locale language = Locale.GERMAN;
  private final UI testUi = new UI();
  private final SecurityService securityService = mock(SecurityService.class);
  private final PortalReleaseNotesComponent notes =
      new PortalReleaseNotesComponent(
          MessageProperties.forLocale(new CustomI18NProvider(), language), securityService);

  @BeforeEach
  void setUpDownloadUiContext() {
    UI.setCurrent(testUi);
  }

  @AfterEach
  void clearDownloadUiContext() {
    UI.setCurrent(null);
  }

  @Test
  void archiveIsHiddenWhenOnlyTheFirstReleaseIsVisible() {
    notes.showReleases(new PortalReleaseNotes("0.2.0").getReleases(), language);

    assertThat(notes.getChildren().filter(Details.class::isInstance)).isEmpty();
  }

  @Test
  void releasesShowSeparatedMetadataAndArchiveStartsCollapsed() {
    notes.showReleases(new PortalReleaseNotes("0.2.1-SNAPSHOT").getReleases(), language);

    CosmosCardComponent latestCard =
        (CosmosCardComponent) notes.getChildren().findFirst().orElseThrow();
    assertThat(latestCard.getChildren().filter(H2.class::isInstance).map(H2.class::cast))
        .extracting(H2::getText)
        .containsExactly("Ein frischer Look fürs Portal");
    HorizontalLayout latestHeaderLayout =
        (HorizontalLayout) latestCard.getChildren().findFirst().orElseThrow();
    HorizontalLayout latestMetadataLayout =
        (HorizontalLayout) latestHeaderLayout.getChildren().findFirst().orElseThrow();
    assertThat(latestMetadataLayout.getChildren().toList())
        .satisfiesExactly(
            version ->
                assertThat(version)
                    .isInstanceOfSatisfying(
                        Badge.class,
                        versionBadge ->
                            assertThat(versionBadge.getText()).isEqualTo("Version 0.2.0")),
            date ->
                assertThat(metadataAfterSeparator(date))
                    .isInstanceOfSatisfying(
                        Span.class,
                        dateSpan -> assertThat(dateSpan.getText()).isEqualTo("20. November 2026")));

    List<Details> archiveEntries =
        notes.getChildren().filter(Details.class::isInstance).map(Details.class::cast).toList();
    assertThat(archiveEntries).hasSize(1);
    Details firstReleaseDetails = archiveEntries.getFirst();
    assertThat(firstReleaseDetails.isOpened()).isFalse();
    assertThat(firstReleaseDetails.getSummary().getChildren().toList())
        .satisfiesExactly(
            title ->
                assertThat(title)
                    .isInstanceOfSatisfying(
                        Span.class,
                        titleSpan ->
                            assertThat(titleSpan.getText()).isEqualTo("Der erste Portal-Release")),
            version ->
                assertThat(metadataAfterSeparator(version))
                    .isInstanceOfSatisfying(
                        Badge.class,
                        versionBadge -> {
                          assertThat(versionBadge.getText()).isEqualTo("Version 0.1.0");
                          assertThat(versionBadge.hasClassName("cosmos-panel-meta")).isTrue();
                        }),
            date ->
                assertThat(metadataAfterSeparator(date))
                    .isInstanceOfSatisfying(
                        Span.class,
                        dateSpan -> assertThat(dateSpan.getText()).isEqualTo("18. November 2026")));
  }

  @Test
  void downloadsAreInTheCurrentHeaderAndArchiveFooterInTheSelectedLanguage() {
    PortalReleaseNotes releaseNotes = new PortalReleaseNotes("0.2.1-SNAPSHOT");
    CustomI18NProvider i18nProvider = new CustomI18NProvider();
    for (Locale locale : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      MessageProperties localizedMessages = MessageProperties.forLocale(i18nProvider, locale);
      PortalReleaseNotesComponent localizedNotes =
          new PortalReleaseNotesComponent(localizedMessages, securityService);
      localizedNotes.showReleases(releaseNotes.getReleases(), locale);

      Component latestCard = localizedNotes.getChildren().findFirst().orElseThrow();
      Component latestHeader = latestCard.getChildren().findFirst().orElseThrow();
      assertDownloadButton(
          latestHeader
              .getChildren()
              .filter(Button.class::isInstance)
              .map(Button.class::cast)
              .toList(),
          localizedMessages.getPortalNewsDownloadMarkdown(),
          "release-notes-0.2.0-" + locale.getLanguage() + ".md");

      Details archiveEntry =
          localizedNotes
              .getChildren()
              .filter(Details.class::isInstance)
              .map(Details.class::cast)
              .findFirst()
              .orElseThrow();
      assertThat(archiveEntry.isOpened()).isFalse();
      assertThat(archiveEntry.getSummary().getChildren().filter(Button.class::isInstance))
          .isEmpty();
      List<Component> archiveContent = archiveEntry.getContent().toList();
      assertThat(archiveContent).hasSize(2);
      assertThat(archiveContent.getFirst()).isInstanceOf(VerticalLayout.class);
      HorizontalLayout archiveFooter = (HorizontalLayout) archiveContent.getLast();
      assertThat(archiveFooter.hasClassName("cosmos-news-archive-footer")).isTrue();
      assertThat(archiveFooter.getJustifyContentMode()).isEqualTo(JustifyContentMode.END);
      assertDownloadButton(
          archiveFooter
              .getChildren()
              .filter(Button.class::isInstance)
              .map(Button.class::cast)
              .toList(),
          localizedMessages.getPortalNewsDownloadMarkdown(),
          "release-notes-0.1.0-" + locale.getLanguage() + ".md");
    }
  }

  private void assertDownloadButton(List<Button> buttons, String label, String fileName) {
    assertThat(buttons)
        .singleElement()
        .satisfies(button -> {
          assertThat(button.getText()).isEqualTo(label);
          assertThat(button.getElement().getAttribute("data-download-url"))
              .endsWith("/" + fileName);
        });
  }

  @Test
  void markdownDownloadsRequireAuthenticationAndTransferUtf8() throws IOException {
    DownloadEvent event = mock(DownloadEvent.class);
    VaadinServletRequest request = mock(VaadinServletRequest.class);
    VaadinResponse response = mock(VaadinResponse.class);
    when(event.getRequest()).thenReturn(request);
    when(event.getResponse()).thenReturn(response);

    MarkdownDownload download = new MarkdownDownload("release-notes-0.2.0-de.md", "# Früher\n");
    assertThat(notes.markdownDownloadResponse(event, download).getError())
        .isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);

    when(securityService.getAuthenticatedUser(request))
        .thenReturn(Optional.of(mock(UserDetails.class)));
    DownloadResponse allowedDownload = notes.markdownDownloadResponse(event, download);
    assertThat(allowedDownload.getFileName()).isEqualTo("release-notes-0.2.0-de.md");
    assertThat(allowedDownload.getContentType()).isEqualTo("text/markdown; charset=UTF-8");
    try (InputStream inputStream = allowedDownload.getInputStream()) {
      byte[] downloadedBytes = inputStream.readAllBytes();
      assertThat(allowedDownload.getContentLength()).isEqualTo(downloadedBytes.length);
      assertThat(new String(downloadedBytes, StandardCharsets.UTF_8)).isEqualTo("# Früher\n");
    }
    verify(response, times(2)).setHeader("Cache-Control", "no-store");
  }

  private Component metadataAfterSeparator(Component groupedMetadata) {
    List<Component> groupChildren = groupedMetadata.getChildren().toList();
    assertThat(groupChildren).hasSize(2);
    assertThat(groupChildren.getFirst())
        .isInstanceOfSatisfying(
            Span.class,
            separatorSpan -> {
              assertThat(separatorSpan.getText()).isEqualTo("·");
              assertThat(separatorSpan.getElement().getAttribute("aria-hidden")).isEqualTo("true");
            });
    return groupChildren.getLast();
  }

  @Test
  void archiveSectionsUseHeadingIconsAndRestartTheNumbering() {
    notes.showReleases(new PortalReleaseNotes("0.2.1-SNAPSHOT").getReleases(), language);

    List<Component> archiveSectionChildren =
        notes
            .getChildren()
            .filter(Details.class::isInstance)
            .map(Details.class::cast)
            .flatMap(Details::getContent)
            .flatMap(Component::getChildren)
            .filter(VerticalLayout.class::isInstance)
            .flatMap(Component::getChildren)
            .toList();
    List<H3> sectionHeadings =
        archiveSectionChildren.stream().filter(H3.class::isInstance).map(H3.class::cast).toList();
    assertThat(sectionHeadings)
        .allSatisfy(
            heading -> assertThat(heading.hasClassName("cosmos-news-section-heading")).isTrue());
    assertThat(
            sectionHeadings.stream()
                .flatMap(Component::getChildren)
                .filter(SvgIcon.class::isInstance))
        .extracting(icon -> icon.getElement().getAttribute("src"))
        .containsExactly(AppIcon.SPARKLES.getSource(), AppIcon.CLIPBOARD_LIST.getSource());

    List<OrderedList> numberedLists =
        archiveSectionChildren.stream()
            .filter(OrderedList.class::isInstance)
            .map(OrderedList.class::cast)
            .toList();
    assertThat(numberedLists)
        .extracting(list -> list.getChildren().count())
        .containsExactly(2L, 3L);
    assertThat(
            numberedLists.stream()
                .flatMap(Component::getChildren)
                .flatMap(Component::getChildren)
                .filter(Span.class::isInstance)
                .flatMap(Component::getChildren))
        .extracting(icon -> icon.getElement().getAttribute("src"))
        .containsExactly(
            AppIcon.NUMBER_ONE.getSource(),
            AppIcon.NUMBER_TWO.getSource(),
            AppIcon.NUMBER_ONE.getSource(),
            AppIcon.NUMBER_TWO.getSource(),
            AppIcon.NUMBER_THREE.getSource());
    assertThat(
            numberedLists
                .getFirst()
                .getChildren()
                .flatMap(Component::getChildren)
                .filter(Markdown.class::isInstance)
                .map(Markdown.class::cast))
        .extracting(Markdown::getContent)
        .satisfiesExactly(
            markdown ->
                assertThat(markdown)
                    .startsWith("**Zugang und Sicherheit:**\nBenutzerkonten registrieren"),
            markdown ->
                assertThat(markdown)
                    .startsWith("**Profil und Zwei-Faktor-Schutz:**\nPersönliche Daten"));
  }

  @Test
  void publishedHighlightsUseLucideSparklesAndTheAreaIconsInBothLanguages() {
    PortalReleaseNotes releaseNotes = new PortalReleaseNotes("0.2.1-SNAPSHOT");
    for (Locale releaseLanguage : List.of(Locale.GERMAN, Locale.ENGLISH)) {
      notes.showReleases(releaseNotes.getReleases(), releaseLanguage);

      assertThat(notes.getElement().getAttribute("lang"))
          .isEqualTo(releaseLanguage.toLanguageTag());
      assertThat(
              notes
                  .getChildren()
                  .filter(H3.class::isInstance)
                  .map(H3.class::cast)
                  .filter(heading -> heading.hasClassName("cosmos-news-latest-section-heading"))
                  .flatMap(Component::getChildren)
                  .filter(SvgIcon.class::isInstance))
          .extracting(icon -> icon.getElement().getAttribute("src"))
          .containsExactly(AppIcon.LUCIDE_SPARKLES.getSource());

      List<String> highlightIconSources =
          notes
              .getChildren()
              .filter(Div.class::isInstance)
              .map(Div.class::cast)
              .filter(grid -> grid.hasClassName("cosmos-news-highlight-grid"))
              .flatMap(Component::getChildren)
              .flatMap(Component::getChildren)
              .filter(HorizontalLayout.class::isInstance)
              .flatMap(Component::getChildren)
              .filter(Div.class::isInstance)
              .flatMap(Component::getChildren)
              .filter(SvgIcon.class::isInstance)
              .map(icon -> icon.getElement().getAttribute("src"))
              .toList();

      assertThat(highlightIconSources)
          .containsExactly(
              AppIcon.LOCK_QUESTION.getSource(),
              AppIcon.BELL.getSource(),
              AppIcon.MESSAGES.getSource());
    }
  }

  @Test
  void improvementListCombinesDigitIconsForTwoDigitNumbers() {
    FeaturedRelease featuredRelease =
        new FeaturedRelease(
            "",
            "Highlights",
            List.of(),
            "Weitere Verbesserungen",
            """
            - Erster Punkt
            - Zweiter Punkt
            - Dritter Punkt
            - Vierter Punkt
            - Fünfter Punkt
            - Sechster Punkt
            - Siebter Punkt
            - Achter Punkt
            - Neunter Punkt
            - Zehnter Punkt
            """);
    ReleaseText releaseText = new ReleaseText("Testausgabe", "", Optional.of(featuredRelease));
    Release release = new Release("0.2.0", LocalDate.of(2026, 11, 20), releaseText, releaseText);
    notes.showReleases(Optional.of(List.of(release)), language);

    OrderedList improvementsList =
        notes
            .getChildren()
            .flatMap(Component::getChildren)
            .filter(OrderedList.class::isInstance)
            .map(OrderedList.class::cast)
            .findFirst()
            .orElseThrow();
    List<Span> numberSpans =
        improvementsList
            .getChildren()
            .map(listItem -> listItem.getChildren().findFirst().orElseThrow())
            .map(Span.class::cast)
            .toList();

    assertThat(numberSpans)
        .extracting(numberSpan -> numberSpan.getChildren().count())
        .containsExactly(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L);
    assertThat(
            numberSpans.stream().flatMap(Component::getChildren).filter(SvgIcon.class::isInstance))
        .extracting(icon -> icon.getElement().getAttribute("src"))
        .containsExactly(
            AppIcon.NUMBER_ONE.getSource(),
            AppIcon.NUMBER_TWO.getSource(),
            AppIcon.NUMBER_THREE.getSource(),
            AppIcon.NUMBER_FOUR.getSource(),
            AppIcon.NUMBER_FIVE.getSource(),
            AppIcon.NUMBER_SIX.getSource(),
            AppIcon.NUMBER_SEVEN.getSource(),
            AppIcon.NUMBER_EIGHT.getSource(),
            AppIcon.NUMBER_NINE.getSource(),
            AppIcon.NUMBER_ONE.getSource(),
            AppIcon.NUMBER_ZERO.getSource());
  }
}
