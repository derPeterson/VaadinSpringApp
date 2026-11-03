package de.derpeterson.app.helper.email;

import org.springframework.web.util.UriComponentsBuilder;

/** Gemeinsame Schriftressource vom eigenen Anwendungsursprung, ohne externen Schriftdienst. */
public final class EmailFonts {
  private EmailFonts() {}

  public static String styleSheetUrl(String baseUrl) {
    return UriComponentsBuilder.fromUriString(baseUrl)
        .pathSegment("custom-theme", "email-fonts.css")
        .toUriString();
  }
}
