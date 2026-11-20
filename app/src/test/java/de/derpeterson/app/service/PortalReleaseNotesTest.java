package de.derpeterson.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

      - Übersichtliche Empfängerliste
      ![Empfängersuche](/images/release-notes/0.2.0/recipients.png)

      ##### Weitere Verbesserungen

      - Neue Sortierung

      ### EN
      #### Portal updates

      A short introduction.

      ##### Highlights

      - An easier way to browse recipients

      ##### Further improvements

      - New sorting order
      """;

  @Test
  void draftAndCurrentCandidateAreHiddenInCandidateAndSnapshotBuilds() {
    assertThat(PortalReleaseNotes.parse(CHANGELOG, "0.2.0"))
        .extracting(Release::version)
        .containsExactly("0.1.0");
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

    assertThat(releases.getLast().textFor(Locale.GERMAN).title()).isEmpty();
    assertThat(releases.getLast().textFor(Locale.GERMAN).markdown()).isEqualTo("- Erstes Portal");
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
