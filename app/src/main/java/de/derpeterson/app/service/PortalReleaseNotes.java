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
      return new ReleaseText("", releaseText);
    }

    int titleEnd = releaseText.indexOf('\n');
    if (titleEnd < 0 || releaseText.substring(titleEnd).isBlank()) {
      throw new IllegalArgumentException("Missing text below release title");
    }
    String editorialTitle = releaseText.substring(TITLE_PREFIX.length(), titleEnd).strip();
    return new ReleaseText(editorialTitle, releaseText.substring(titleEnd).strip());
  }

  public record Release(
      String version, LocalDate releasedOn, ReleaseText german, ReleaseText english) {
    public ReleaseText textFor(Locale locale) {
      return Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? german : english;
    }
  }

  public record ReleaseText(String title, String markdown) {}

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
