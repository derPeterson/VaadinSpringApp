package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
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
          "two_factor",
          "account_deactivated",
          "account_deletion_reminder"
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

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void lifecycleTemplatesUseCosmosAndOnlyOfferActionsWhereApplicable(String language) {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Cosmos <Test>");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT)).thenReturn("Cosmos Test");
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://portal.example.test");
    AccountSecurityMail accountSecurityMail =
        new AccountSecurityMail(configService, new CustomI18NProvider());
    UserEntity recipientUser = new UserEntity();
    recipientUser.setPreferredLocale(Locale.forLanguageTag(language));
    String reactivationUrl = "https://portal.example.test/reactivate-account?token=test&next=login";

    for (String templateName :
        new String[] {
          "account_deactivated",
          "account_deletion_reminder",
          "account_deleted",
          "account_reactivated"
        }) {
      String renderedMail =
          accountSecurityMail.render(
              templateName,
              recipientUser,
              Map.of(
                  "TITLE",
                  "Status <Test>",
                  "MESSAGE",
                  "Deadline: 12.12.2026 <Test>",
                  "REACTIVATION_LINK",
                  reactivationUrl));
      Document mailDocument = Jsoup.parse(renderedMail);
      assertFalse(renderedMail.contains("{{"), templateName);
      assertEquals(language, mailDocument.selectFirst("html").attr("lang"));
      assertFalse(mailDocument.select(".cosmos-email-card").isEmpty(), templateName);
      assertTrue(mailDocument.text().contains("Status <Test>"), templateName);
      assertTrue(mailDocument.text().contains("Deadline: 12.12.2026 <Test>"), templateName);
      boolean actionExpected =
          templateName.equals("account_deactivated")
              || templateName.equals("account_deletion_reminder");
      assertEquals(actionExpected ? 1 : 0, mailDocument.select(".cosmos-email-button a").size());
      assertEquals(actionExpected ? 1 : 0, mailDocument.select(".cosmos-email-copy-link a").size());
      if (actionExpected) {
        assertEquals(reactivationUrl, mailDocument.selectFirst(".cosmos-email-copy-link a").text());
      }
    }
  }
}
