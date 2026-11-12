package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

class EmailActionLinkTemplateTest {
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void everyActionButtonHasTheSameCompleteCopyableLink(String language) throws IOException {
    for (String templateName :
        new String[] {
          "welcome",
          "reset_password",
          "email_change_request",
          "email_changed",
          "password_changed",
          "two_factor"
        }) {
      try (InputStream templateInputStream =
          new ClassPathResource("email/" + templateName + "_" + language + ".html")
              .getInputStream()) {
        String templateHtml =
            new String(templateInputStream.readAllBytes(), StandardCharsets.UTF_8);
        Document mailDocument = Jsoup.parse(templateHtml);
        Element actionAnchor = mailDocument.selectFirst(".cosmos-email-button a");
        Element copyAnchor = mailDocument.selectFirst(".cosmos-email-copy-link a");

        assertNotNull(actionAnchor, templateName);
        assertNotNull(copyAnchor, templateName);
        assertEquals(actionAnchor.attr("href"), copyAnchor.attr("href"), templateName);
        assertEquals(actionAnchor.attr("href"), copyAnchor.text(), templateName);
        assertTrue(copyAnchor.attr("style").contains("word-break: break-all"));
        assertFalse(templateHtml.contains("fonts.googleapis.com"));
        assertFalse(templateHtml.contains("<mj-include"));
      }
    }
  }
}
