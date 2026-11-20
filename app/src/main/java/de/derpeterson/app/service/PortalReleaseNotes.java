package de.derpeterson.app.service;

import de.derpeterson.app.helper.logging.LogSafety;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Liest den beim Build übernommenen Changelog einmal und hält die Portaltexte im Speicher. */
@Service
@Slf4j
public class PortalReleaseNotes {
  private static final Pattern RELEASE_START = Pattern.compile("(?m)(?=^## \\[)");
  // Nur die datierten Versionsüberschriften des Release-Helfers beginnen eine Ausgabe.
  private static final Pattern RELEASE_HEADING =
      Pattern.compile("^## \\[([0-9]+\\.[0-9]+\\.[0-9]+)] - ([0-9]{4}-[0-9]{2}-[0-9]{2})\\h*\\R");
  // Innerhalb eines Releases trennen nur Deutsch und Englisch die Sprachabschnitte.
  private static final Pattern LANGUAGE_HEADING = Pattern.compile("(?m)^### (DE|EN)\\h*$");
  private static final String TITLE_PREFIX = "#### ";
  // Die beiden Überschriften bilden die redaktionelle Struktur einer hervorgehobenen Ausgabe.
  private static final Pattern HIGHLIGHTS_HEADING = Pattern.compile("(?m)^##### (Highlights)\\h*$");
  private static final Pattern IMPROVEMENTS_HEADING =
      Pattern.compile("(?m)^##### (Weitere Verbesserungen|Further improvements)\\h*$");
  // Nur ein Bindestrich mit folgendem Leerraum am Zeilenanfang beginnt eine Karte.
  private static final Pattern HIGHLIGHT_ITEM = Pattern.compile("(?m)^-\\h+");
  // Ein fett gesetzter Titel auf einer eigenen Zeile wird zur Kartenüberschrift.
  private static final Pattern HIGHLIGHT_TITLE =
      Pattern.compile("^\\*\\*([^\\r\\n]+?)\\*\\*\\h*\\R");

  private final Optional<List<Release>> releases;

  public PortalReleaseNotes(@Value("${app.portal-news.build-version:}") String buildVersion) {
    releases = readBundledReleases(buildVersion);
  }

  public Optional<List<Release>> getReleases() {
    return releases;
  }

  private static Optional<List<Release>> readBundledReleases(String buildVersion) {
    if (buildVersion.isBlank()) {
      log.warn("Portal release notes unavailable: missing build version");
      return Optional.empty();
    }

    ClassPathResource changelogResource = new ClassPathResource("portal-news/CHANGELOG.md");
    try (InputStream changelogStream = changelogResource.getInputStream()) {
      String changelog = new String(changelogStream.readAllBytes(), StandardCharsets.UTF_8);
      return Optional.of(parse(changelog, buildVersion));
    } catch (IOException | IllegalArgumentException | DateTimeParseException failure) {
      log.warn("Portal release notes unavailable", LogSafety.safeFailure(failure));
      return Optional.empty();
    }
  }

  static List<Release> parse(String changelog, String buildVersion) {
    Version applicationVersion = Version.parse(buildVersion.replaceFirst("-SNAPSHOT$", ""));
    List<Release> publishedReleases = new ArrayList<>();
    Set<Version> includedVersions = new HashSet<>();

    for (String releaseSection : RELEASE_START.split(changelog)) {
      if (!releaseSection.startsWith("## [") || releaseSection.startsWith("## [Unreleased]")) {
        continue;
      }

      Matcher releaseHeadingMatcher = RELEASE_HEADING.matcher(releaseSection);
      if (!releaseHeadingMatcher.lookingAt()) {
        throw new IllegalArgumentException("Invalid release heading");
      }

      Version releaseVersion = Version.parse(releaseHeadingMatcher.group(1));
      // prepare legt den datierten Eintrag vor dem Tag an. next erhöht die Build-Version erst
      // danach.
      if (releaseVersion.compareTo(applicationVersion) >= 0) {
        continue;
      }
      if (!includedVersions.add(releaseVersion)) {
        throw new IllegalArgumentException("Duplicate release version");
      }

      LocalDate releasedOn = LocalDate.parse(releaseHeadingMatcher.group(2));
      String releaseBody = releaseSection.substring(releaseHeadingMatcher.end());
      publishedReleases.add(parseRelease(releaseVersion, releasedOn, releaseBody));
    }

    publishedReleases.sort(
        Comparator.comparing(Release::releasedOn)
            .thenComparing(release -> Version.parse(release.version()))
            .reversed());
    return List.copyOf(publishedReleases);
  }

  private static Release parseRelease(Version version, LocalDate releasedOn, String releaseBody) {
    Matcher languageHeadingMatcher = LANGUAGE_HEADING.matcher(releaseBody);
    if (!languageHeadingMatcher.find()
        || !"DE".equals(languageHeadingMatcher.group(1))
        || !releaseBody.substring(0, languageHeadingMatcher.start()).isBlank()) {
      throw new IllegalArgumentException("Missing German release notes");
    }
    int germanBodyStart = languageHeadingMatcher.end();
    if (!languageHeadingMatcher.find() || !"EN".equals(languageHeadingMatcher.group(1))) {
      throw new IllegalArgumentException("Missing English release notes");
    }
    String germanBody = releaseBody.substring(germanBodyStart, languageHeadingMatcher.start());
    String englishBody = releaseBody.substring(languageHeadingMatcher.end());
    if (languageHeadingMatcher.find()) {
      throw new IllegalArgumentException("Duplicate release language");
    }
    return new Release(
        version.toString(), releasedOn, parseText(germanBody), parseText(englishBody));
  }

  private static ReleaseText parseText(String releaseBody) {
    String releaseText = releaseBody.strip();
    if (releaseText.isEmpty()) {
      throw new IllegalArgumentException("Empty release notes");
    }
    if (!releaseText.startsWith(TITLE_PREFIX)) {
      return new ReleaseText("", releaseText, parseFeaturedRelease(releaseText));
    }

    int titleEnd = releaseText.indexOf('\n');
    if (titleEnd < 0 || releaseText.substring(titleEnd).isBlank()) {
      throw new IllegalArgumentException("Missing text below release title");
    }
    String editorialTitle = releaseText.substring(TITLE_PREFIX.length(), titleEnd).strip();
    String markdown = releaseText.substring(titleEnd).strip();
    return new ReleaseText(editorialTitle, markdown, parseFeaturedRelease(markdown));
  }

  private static Optional<FeaturedRelease> parseFeaturedRelease(String markdown) {
    Matcher highlightsHeadingMatcher = HIGHLIGHTS_HEADING.matcher(markdown);
    Matcher improvementsHeadingMatcher = IMPROVEMENTS_HEADING.matcher(markdown);
    if (!highlightsHeadingMatcher.find()
        || !improvementsHeadingMatcher.find(highlightsHeadingMatcher.end())) {
      return Optional.empty();
    }

    String introduction = markdown.substring(0, highlightsHeadingMatcher.start()).strip();
    String highlightsMarkdown =
        markdown
            .substring(highlightsHeadingMatcher.end(), improvementsHeadingMatcher.start())
            .strip();
    String improvementsMarkdown = markdown.substring(improvementsHeadingMatcher.end()).strip();
    String[] highlightItems = HIGHLIGHT_ITEM.split(highlightsMarkdown, -1);
    if (highlightItems.length < 2
        || !highlightItems[0].isEmpty()
        || improvementsMarkdown.isEmpty()) {
      return Optional.empty();
    }

    List<Highlight> highlights = new ArrayList<>();
    for (int itemIndex = 1; itemIndex < highlightItems.length; itemIndex++) {
      // Ohne Listenmarker entfällt auch der dazugehörige Einzug der Fortsetzungszeilen.
      String itemMarkdown = highlightItems[itemIndex].indent(-2).strip();
      if (itemMarkdown.isEmpty()) {
        return Optional.empty();
      }

      Matcher titleMatcher = HIGHLIGHT_TITLE.matcher(itemMarkdown);
      if (titleMatcher.lookingAt()) {
        highlights.add(
            new Highlight(
                titleMatcher.group(1).strip(), itemMarkdown.substring(titleMatcher.end()).strip()));
      } else {
        highlights.add(new Highlight("", itemMarkdown));
      }
    }

    return Optional.of(
        new FeaturedRelease(
            introduction,
            highlightsHeadingMatcher.group(1),
            List.copyOf(highlights),
            improvementsHeadingMatcher.group(1),
            improvementsMarkdown));
  }

  public record Release(
      String version, LocalDate releasedOn, ReleaseText german, ReleaseText english) {
    public ReleaseText textFor(Locale locale) {
      return Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? german : english;
    }
  }

  public record ReleaseText(String title, String markdown, Optional<FeaturedRelease> featured) {}

  public record FeaturedRelease(
      String introduction,
      String highlightsHeading,
      List<Highlight> highlights,
      String improvementsHeading,
      String improvementsMarkdown) {}

  public record Highlight(String title, String markdown) {}

  private record Version(int major, int minor, int patch) implements Comparable<Version> {
    private static final Comparator<Version> ORDER =
        Comparator.comparingInt(Version::major)
            .thenComparingInt(Version::minor)
            .thenComparingInt(Version::patch);

    private static Version parse(String version) {
      // Der Punkt trennt exakt die drei numerischen Teile der Projektversion.
      String[] parts = version.split("\\.", -1);
      if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid version");
      }
      return new Version(
          Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    @Override
    public int compareTo(Version other) {
      return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
      return major + "." + minor + "." + patch;
    }
  }
}
