package de.derpeterson.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import de.derpeterson.app.service.PortalReleaseNotes.FeaturedRelease;
import de.derpeterson.app.service.PortalReleaseNotes.Highlight;
import de.derpeterson.app.service.PortalReleaseNotes.MarkdownDownload;
import de.derpeterson.app.service.PortalReleaseNotes.Release;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class PortalReleaseNotesTest {
  private static final String CHANGELOG =
      """
      # Änderungen

      ## [Unreleased]

      ### DE
      - Entwurf
      ### EN
      - Draft

      ## [0.1.0] - 2026-11-18

      ### DE
      - Erstes Portal
      ### EN
      - First portal release

      ## [0.2.0] - 2026-11-20

      ### DE
      #### Neues im Portal

      Der kurze Einstieg.

      ##### Highlights

      - **Empfänger finden** <!-- nav-icon: BELL -->
        Übersichtliche [Empfängerliste](/notifications)
        ![Empfängersuche](/images/release-notes/0.2.0/recipients.png)

          - Suche nach Name
          - Suche nach E-Mail
      - Überblick behalten

      ##### Weitere Verbesserungen

      - Neue Sortierung
      - Übersichtliche [Liste](/notifications)

        - Mit Details

      ### EN
      #### Portal updates

      A short introduction.

      ##### Highlights

      - An easier way to browse recipients

      ##### Further improvements

      - New sorting order
      """;

  @Test
  void releaseBuildShowsItsOwnNotesAndHidesLaterVersions() {
    assertThat(PortalReleaseNotes.parse(CHANGELOG, "0.2.0"))
        .extracting(Release::version)
        .containsExactly("0.2.0", "0.1.0");
    assertThat(PortalReleaseNotes.parse(CHANGELOG, "0.1.0"))
        .extracting(Release::version)
        .containsExactly("0.1.0");
  }

  @Test
  void snapshotBuildHidesItsOwnDatedCandidate() {
    assertThat(PortalReleaseNotes.parse(CHANGELOG, "0.2.0-SNAPSHOT"))
        .extracting(Release::version)
        .containsExactly("0.1.0");
  }

  @Test
  void followUpBuildShowsPublishedHistoryAndPreservesTheLocalizedMarkdown() {
    List<Release> releases = PortalReleaseNotes.parse(CHANGELOG, "0.2.1-SNAPSHOT");

    assertThat(releases).extracting(Release::version).containsExactly("0.2.0", "0.1.0");
    Release latestRelease = releases.getFirst();
    assertThat(latestRelease.textFor(Locale.GERMANY).title()).isEqualTo("Neues im Portal");
    assertThat(latestRelease.textFor(Locale.GERMAN).markdown())
        .contains(
            "Der kurze Einstieg.",
            "##### Highlights",
            "##### Weitere Verbesserungen",
            "![Empfängersuche](/images/release-notes/0.2.0/recipients.png)");
    assertThat(latestRelease.textFor(Locale.ENGLISH).title()).isEqualTo("Portal updates");
    assertThat(latestRelease.textFor(Locale.ENGLISH).markdown())
        .contains("A short introduction.", "##### Further improvements")
        .doesNotContain("Der kurze Einstieg.");

    FeaturedRelease germanRelease = latestRelease.textFor(Locale.GERMAN).featured().orElseThrow();
    assertThat(germanRelease.introduction()).isEqualTo("Der kurze Einstieg.");
    assertThat(germanRelease.highlights())
        .extracting(Highlight::title)
        .containsExactly("Empfänger finden", "");
    assertThat(germanRelease.highlights().getFirst().navigationIconName()).contains("BELL");
    assertThat(germanRelease.highlights().getLast().navigationIconName()).isEmpty();
    assertThat(germanRelease.highlights().getFirst().markdown())
        .isEqualTo(
            """
            Übersichtliche [Empfängerliste](/notifications)
            ![Empfängersuche](/images/release-notes/0.2.0/recipients.png)

              - Suche nach Name
              - Suche nach E-Mail
            """
                .stripTrailing());
    assertThat(germanRelease.improvementItems())
        .containsExactly(
            "Neue Sortierung", "Übersichtliche [Liste](/notifications)\n\n- Mit Details");
    FeaturedRelease englishRelease = latestRelease.textFor(Locale.ENGLISH).featured().orElseThrow();
    assertThat(englishRelease.improvementsHeading()).isEqualTo("Further improvements");
    assertThat(englishRelease.improvementsMarkdown()).isEqualTo("- New sorting order");

    assertThat(releases.getLast().textFor(Locale.GERMAN).title()).isEmpty();
    assertThat(releases.getLast().textFor(Locale.GERMAN).markdown()).isEqualTo("- Erstes Portal");
    assertThat(releases.getLast().textFor(Locale.GERMAN).featured()).isEmpty();
  }

  @Test
  void downloadsContainOnlyTheSelectedPublishedReleaseAndLanguage() {
    List<Release> releases = PortalReleaseNotes.parse(CHANGELOG, "0.2.1-SNAPSHOT");

    MarkdownDownload germanDownload = releases.getFirst().markdownDownload(Locale.GERMANY);
    assertThat(germanDownload.fileName()).isEqualTo("release-notes-0.2.0-de.md");
    assertThat(germanDownload.markdown())
        .startsWith("# Neues im Portal\n\nVersion 0.2.0 · 2026-11-20\n\nDer kurze Einstieg.")
        .contains("- **Empfänger finden**\n  Übersichtliche [Empfängerliste](/notifications)")
        .doesNotContain("nav-icon", "Portal updates", "Erstes Portal", "Entwurf");

    MarkdownDownload englishDownload = releases.getFirst().markdownDownload(Locale.US);
    assertThat(englishDownload.fileName()).isEqualTo("release-notes-0.2.0-en.md");
    assertThat(englishDownload.markdown())
        .startsWith("# Portal updates\n\nVersion 0.2.0 · 2026-11-20\n\nA short introduction.")
        .contains("- New sorting order")
        .doesNotContain("Erstes Portal", "First portal release", "Draft", "Neues im Portal");

    MarkdownDownload earlierDownload = releases.getLast().markdownDownload(Locale.ENGLISH);
    assertThat(earlierDownload.fileName()).isEqualTo("release-notes-0.1.0-en.md");
    assertThat(earlierDownload.markdown())
        .isEqualTo("# Version 0.1.0\n\n2026-11-18\n\n- First portal release\n");
  }

  @Test
  void missingTranslationIsRejectedInsteadOfPublishingAnIncompleteRelease() {
    int englishHeading = CHANGELOG.lastIndexOf("### EN");
    String incompleteChangelog = CHANGELOG.substring(0, englishHeading);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> PortalReleaseNotes.parse(incompleteChangelog, "0.2.1-SNAPSHOT"))
        .withMessage("Missing English release notes");
  }

  @Test
  void standardBuildResourcesProvideReleaseNotesAndTheProjectVersion() throws IOException {
    String buildVersion =
        PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))
            .getProperty("app.portal-news.build-version");

    assertThat(buildVersion).matches("[0-9]+\\.[0-9]+\\.[0-9]+(-SNAPSHOT)?");
    assertThat(new PortalReleaseNotes(buildVersion).getReleases())
        .hasValueSatisfying(
            releases -> assertThat(releases).extracting(Release::version).contains("0.1.0"));
  }
}
