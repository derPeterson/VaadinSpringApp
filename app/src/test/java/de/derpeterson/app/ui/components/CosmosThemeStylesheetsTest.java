package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vaadin.flow.component.dependency.StyleSheet;
import de.derpeterson.app.Application;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/** Sichert die sichtbare Ladeordnung des Cosmos-Themes über {@code @StyleSheet}. */
class CosmosThemeStylesheetsTest {
  private static final String THEME_DIRECTORY = "custom-theme/";
  private static final String RESOURCE_ROOT = "/META-INF/resources/";

  /**
   * Dateien mit eigenem Ladeweg: Mailvorlagen, OTP-Schattenwurzel und die zweite
   * Hintergrundvariante.
   */
  private static final Set<String> SEPARATELY_LOADED_STYLESHEETS =
      Set.of(
          "custom-theme/email-fonts.css",
          "custom-theme/cosmos-mfa-otp.css",
          "custom-theme/cosmos-background-base.css",
          "custom-theme/cosmos-background-advanced.css");

  @Test
  void applicationLoadsGlobalLayersInDocumentedOrder() {
    List<String> globalStylesheets =
        Arrays.stream(Application.class.getAnnotationsByType(StyleSheet.class))
            .map(StyleSheet::value)
            .filter(stylesheetPath -> stylesheetPath.startsWith(THEME_DIRECTORY))
            .toList();

    assertEquals(
        List.of(
            "custom-theme/cosmos-palette.css",
            "custom-theme/typography.css",
            "custom-theme/cosmos-tokens.css",
            "custom-theme/cosmos-aura.css",
            "custom-theme/cosmos-scrollbars.css",
            "custom-theme/cosmos-duration-field.css",
            "custom-theme/cosmos-breadcrumbs.css",
            "custom-theme/cosmos-segmented-tabs.css",
            "custom-theme/cosmos-theme.css",
            "custom-theme/cosmos-utilities.css",
            "custom-theme/cosmos-cards.css",
            "custom-theme/cosmos-avatar.css",
            "custom-theme/cosmos-shell.css",
            CosmosBackgroundComponent.STYLESHEET,
            "custom-theme/cosmos-admin.css",
            "custom-theme/cosmos-notifications.css"),
        globalStylesheets);
  }

  @Test
  void everyThemeFileIsLoadedVisiblyAndExists() throws Exception {
    Set<String> annotatedStylesheets = findAnnotatedThemeStylesheets();
    for (String stylesheetPath : annotatedStylesheets) {
      try (InputStream stylesheetStream =
          getClass().getResourceAsStream(RESOURCE_ROOT + stylesheetPath)) {
        assertNotNull(stylesheetStream, stylesheetPath);
      }
    }

    Set<String> unreferencedStylesheets = new TreeSet<>(listThemeStylesheets());
    unreferencedStylesheets.removeAll(annotatedStylesheets);
    unreferencedStylesheets.removeAll(SEPARATELY_LOADED_STYLESHEETS);

    assertEquals(Set.of(), unreferencedStylesheets);
  }

  @Test
  void themeFilesDoNotHideFurtherStylesheetsBehindImports() throws Exception {
    for (String stylesheetPath : listThemeStylesheets()) {
      String stylesheetContent =
          Files.readString(
              themeDirectory().resolve(stylesheetPath.substring(THEME_DIRECTORY.length())));

      assertFalse(stylesheetContent.contains("@import"), stylesheetPath);
    }
  }

  private Set<String> findAnnotatedThemeStylesheets() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider stylesheetScanner =
        new ClassPathScanningCandidateComponentProvider(false);
    stylesheetScanner.addIncludeFilter(new AnnotationTypeFilter(StyleSheet.class));
    stylesheetScanner.addIncludeFilter(new AnnotationTypeFilter(StyleSheet.Container.class));

    Set<String> stylesheetPaths = new TreeSet<>();
    for (BeanDefinition candidate :
        stylesheetScanner.findCandidateComponents("de.derpeterson.app")) {
      Class<?> annotatedClass = Class.forName(candidate.getBeanClassName());
      for (StyleSheet styleSheet : annotatedClass.getAnnotationsByType(StyleSheet.class)) {
        if (styleSheet.value().startsWith(THEME_DIRECTORY)) {
          stylesheetPaths.add(styleSheet.value());
        }
      }
    }
    return stylesheetPaths;
  }

  private List<String> listThemeStylesheets() throws IOException, URISyntaxException {
    List<String> stylesheetPaths = new ArrayList<>();
    try (Stream<Path> themeFiles = Files.list(themeDirectory())) {
      themeFiles
          .map(themeFile -> themeFile.getFileName().toString())
          .filter(fileName -> fileName.endsWith(".css"))
          .sorted()
          .forEach(fileName -> stylesheetPaths.add(THEME_DIRECTORY + fileName));
    }
    return stylesheetPaths;
  }

  private Path themeDirectory() throws URISyntaxException {
    URL themeDirectoryUrl = getClass().getResource(RESOURCE_ROOT + THEME_DIRECTORY);
    assertNotNull(themeDirectoryUrl);
    return Path.of(themeDirectoryUrl.toURI());
  }
}
