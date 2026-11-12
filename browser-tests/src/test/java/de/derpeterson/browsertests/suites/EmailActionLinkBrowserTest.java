package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import de.derpeterson.browsertests.BrowserFixture;
import io.qameta.allure.playwright.AllurePlaywright;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Darstellungsprüfung der kanonischen Mailvorlagen, getrennt von den SMTP-Journeys in Mailpit. */
@Tag("browser")
@Tag("component-follow-up")
public class EmailActionLinkBrowserTest extends BrowserFixture {
  private static final String LONG_ACTION_URL =
      "https://example.test/account-action/" + "copyable-long-test-token-".repeat(12);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "welcome",
        "reset_password",
        "email_change_request",
        "email_changed",
        "password_changed",
        "two_factor",
        "account_deactivated",
        "account_deletion_reminder"
      })
  void everyActionTemplateKeepsItsFullCopyableUrlInsideTheCosmosFrame(String templateName)
      throws IOException {
    browserPage.setContent(renderPreviewTemplate(templateName));
    Locator linkFrameLocator = browserPage.locator(".cosmos-email-copy-link > div");
    Locator copyLinkLocator = linkFrameLocator.locator("a");
    assertThat(linkFrameLocator).hasCount(1);
    assertThat(linkFrameLocator).hasCSS("border-top-color", "rgb(44, 55, 81)");
    assertThat(linkFrameLocator).hasCSS("border-top-width", "1px");
    assertThat(linkFrameLocator).hasCSS("background-color", "rgb(9, 17, 32)");
    assertThat(linkFrameLocator).hasCSS("border-radius", "8px");
    assertThat(copyLinkLocator).hasText(LONG_ACTION_URL);
    assertThat(copyLinkLocator).hasAttribute("href", LONG_ACTION_URL);
    assertThat(browserPage.locator(".cosmos-email-button a")).hasAttribute("href", LONG_ACTION_URL);

    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 1000);
      assertTrue(
          (Boolean)
              linkFrameLocator.evaluate(
                  "element => element.scrollWidth <= element.clientWidth + 1"));
      assertTrue(
          (Boolean)
              browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth"));
      String selectedUrlText =
          copyLinkLocator
              .evaluate(
                  """
                  element => {
                    const range = document.createRange();
                    range.selectNodeContents(element);
                    const selection = window.getSelection();
                    selection.removeAllRanges();
                    selection.addRange(range);
                    return selection.toString();
                  }
                  """)
              .toString();
      assertEquals(LONG_ACTION_URL, selectedUrlText);
      AllurePlaywright.attachScreenshot(
          "Mailvorlage " + templateName + " " + viewportWidth, browserPage);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"account_deleted", "account_reactivated"})
  void informationalTemplatesDoNotContainAnEmptyActionFrame(String templateName)
      throws IOException {
    browserPage.setContent(renderPreviewTemplate(templateName));
    assertThat(browserPage.locator(".cosmos-email-copy-link")).hasCount(0);
    assertThat(browserPage.locator(".cosmos-email-button")).hasCount(0);
  }

  private String renderPreviewTemplate(String templateName) throws IOException {
    String resourcePath = "/email/" + templateName + "_" + uiTexts.language() + ".html";
    try (InputStream templateStream =
        Objects.requireNonNull(getClass().getResourceAsStream(resourcePath), resourcePath)) {
      String templateHtml = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
      // Doppelte geschweifte Klammern begrenzen einen Vorlagenplatzhalter ohne weitere Klammern.
      Matcher placeholderMatcher = Pattern.compile("\\{\\{([^{}]+)}}").matcher(templateHtml);
      StringBuilder renderedHtml = new StringBuilder();
      while (placeholderMatcher.find()) {
        String placeholderName = placeholderMatcher.group(1);
        String replacementText =
            placeholderName.endsWith("_LINK")
                ? LONG_ACTION_URL
                : switch (placeholderName) {
                  case "SERVICE_LOGO" ->
                      System.getProperty("browser.tests.baseUrl") + "/icons/service-logo.png";
                  case "INTER_FONT_STYLESHEET" -> "";
                  case "SERVICE_NAME" -> "Cosmos Mail Preview";
                  case "FIRST_NAME" -> "Alex";
                  case "LAST_NAME" -> "Example";
                  case "NEW_EMAIL" -> "new@example.test";
                  case "OLD_EMAIL" -> "old@example.test";
                  case "CODE" -> "12345678";
                  case "COPYRIGHT_TEXT" -> "Cosmos Mail Preview";
                  default -> "Preview";
                };
        placeholderMatcher.appendReplacement(
            renderedHtml, Matcher.quoteReplacement(replacementText));
      }
      return placeholderMatcher.appendTail(renderedHtml).toString();
    }
  }
}
