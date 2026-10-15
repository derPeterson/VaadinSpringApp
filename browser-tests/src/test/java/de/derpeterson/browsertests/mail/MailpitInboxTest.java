package de.derpeterson.browsertests.mail;

import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Checks isolation before any browser can follow a link from a captured test mail. */
class MailpitInboxTest {
  private static final String TOKEN = "a28b857d-9841-4ae8-9e21-50db0264a6b1";

  @Test
  void linkOnCustomRunnerPortIsAccepted() {
    withRunnerOrigin(
        () ->
            assertDoesNotThrow(
                () ->
                    MailpitInbox.assertLocalVerificationLink(
                        "http://127.0.0.1:18082" + path(VERIFICATION_ROUTE) + "/" + TOKEN)));
  }

  @Test
  void foreignOrManualTargetsAndWrongRoutesAreRejectedBeforeNavigation() {
    withRunnerOrigin(
        () -> {
          for (String wrongLink :
              new String[] {
                "http://127.0.0.1:18080" + path(VERIFICATION_ROUTE) + "/" + TOKEN,
                "http://example.test:18082" + path(VERIFICATION_ROUTE) + "/" + TOKEN,
                "https://127.0.0.1:18082" + path(VERIFICATION_ROUTE) + "/" + TOKEN,
                "http://user@127.0.0.1:18082" + path(VERIFICATION_ROUTE) + "/" + TOKEN,
                "http://127.0.0.1:18082" + path(ADMIN_ROUTE),
                "http://127.0.0.1:18082"
                    + path(VERIFICATION_ROUTE)
                    + "/"
                    + TOKEN
                    + "?redirect=elsewhere"
              }) {
            assertThrows(
                AssertionError.class, () -> MailpitInbox.assertLocalVerificationLink(wrongLink));
          }
        });
  }

  @Test
  void resetLinkOnTheExactRunnerOriginIsAccepted() {
    withRunnerOrigin(
        () ->
            assertDoesNotThrow(
                () ->
                    MailpitInbox.assertLocalPasswordResetLink(
                        "http://127.0.0.1:18082" + path(RESET_PASSWORD_ROUTE) + "/" + TOKEN)));
  }

  @Test
  void resetMailCannotNavigateToOtherOriginsOrActionRoutes() {
    withRunnerOrigin(
        () -> {
          for (String wrongLink :
              new String[] {
                "http://127.0.0.1:18080" + path(RESET_PASSWORD_ROUTE) + "/" + TOKEN,
                "http://example.test:18082" + path(RESET_PASSWORD_ROUTE) + "/" + TOKEN,
                "https://127.0.0.1:18082" + path(RESET_PASSWORD_ROUTE) + "/" + TOKEN,
                "http://user@127.0.0.1:18082" + path(RESET_PASSWORD_ROUTE) + "/" + TOKEN,
                "http://127.0.0.1:18082" + path(VERIFICATION_ROUTE) + "/" + TOKEN,
                "http://127.0.0.1:18082" + path(ADMIN_ROUTE),
                "http://127.0.0.1:18082"
                    + path(RESET_PASSWORD_ROUTE)
                    + "/"
                    + TOKEN
                    + "?redirect=elsewhere",
                "http://127.0.0.1:18082" + path(RESET_PASSWORD_ROUTE) + "/" + TOKEN + "#elsewhere"
              }) {
            assertThrows(
                AssertionError.class, () -> MailpitInbox.assertLocalPasswordResetLink(wrongLink));
          }
        });
  }

  @Test
  void regularSmtpLineEndingsPreserveMultilineUnicodeContent() {
    String expected = "First line\nSecond line ÄÖÜ ß 中文";
    assertDoesNotThrow(
        () ->
            MailpitInbox.assertPlainTextContent(
                "First line\r\nSecond line ÄÖÜ ß 中文\r\n", expected));
    assertDoesNotThrow(() -> MailpitInbox.assertPlainTextContent(expected + "\n", expected));
  }

  @Test
  void contentComparisonStillRejectsChangedUnicodeWhitespaceAndAdditionalLines() {
    String expected = "First line\nSecond line ÄÖÜ ß 中文";
    for (String changed :
        new String[] {
          "First line\r\nSecond line ÄÖU ß 中文\r\n",
          "First line \r\nSecond line ÄÖÜ ß 中文\r\n",
          "First line\r\n\r\nSecond line ÄÖÜ ß 中文\r\n",
          "First line\r\nSecond line ÄÖÜ ß 中文\r\n\r\n",
          "First line\rSecond line ÄÖÜ ß 中文\r\n"
        })
      assertThrows(
          AssertionError.class, () -> MailpitInbox.assertPlainTextContent(changed, expected));
  }

  private static void withRunnerOrigin(Runnable test) {
    String previous = System.getProperty("browser.tests.baseUrl");
    try {
      System.setProperty("browser.tests.baseUrl", "http://127.0.0.1:18082");
      test.run();
    } finally {
      if (previous == null) System.clearProperty("browser.tests.baseUrl");
      else System.setProperty("browser.tests.baseUrl", previous);
    }
  }
}
