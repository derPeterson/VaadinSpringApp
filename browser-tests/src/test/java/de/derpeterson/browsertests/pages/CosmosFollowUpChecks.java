package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.i18n.UiTexts;

/** Gemeinsame sichtbare Verträge der gezielt nachgeprüften Konto- und Detailsdarstellung. */
public final class CosmosFollowUpChecks {
  private CosmosFollowUpChecks() {}

  /** Prüft einen anfangs eingeklappten Schalter und hinterlässt ihn wieder eingeklappt. */
  public static void assertDetailsWithoutHover(Locator detailsToggleLocator) {
    String backgroundColor =
        detailsToggleLocator
            .evaluate("element => getComputedStyle(element).backgroundColor")
            .toString();

    detailsToggleLocator.hover();
    assertThat(detailsToggleLocator).hasCSS("background-color", backgroundColor);
    assertThat(detailsToggleLocator).hasCSS("cursor", "pointer");

    detailsToggleLocator.focus();
    detailsToggleLocator.press("Enter");
    assertThat(detailsToggleLocator).hasAttribute("aria-expanded", "true");
    detailsToggleLocator.press("Enter");
    assertThat(detailsToggleLocator).hasAttribute("aria-expanded", "false");
  }

  public static void assertPendingAccountStyle(Page browserPage, UiTexts uiTexts) {
    assertThat(browserPage.locator(".cosmos-status-icon--warning"))
        .hasCSS("color", "rgb(251, 191, 36)");

    Locator emailAddressLocator = browserPage.locator(".pending-verification-address");
    assertThat(emailAddressLocator.locator(".pending-verification-address-label"))
        .hasText(uiTexts.getPublicEmailField());
    Locator statusBadgeLocator = emailAddressLocator.locator("vaadin-badge");
    assertThat(statusBadgeLocator).hasCount(1);
    assertThat(statusBadgeLocator).hasCSS("border-top-width", "1px");

    assertThat(browserPage.locator(".pending-verification-folder-emphasis"))
        .hasText(uiTexts.getEmailVerificationSpamFolderLabel());
    assertThat(browserPage.locator(".pending-verification-folder-emphasis"))
        .hasCSS("font-weight", "700");

    Locator resendHintLocator = browserPage.locator(".pending-verification-resend-hint");
    assertTrue(
        (Boolean)
            resendHintLocator.evaluate(
                "element => element.scrollWidth <= element.clientWidth + 1"));
    if (browserPage.viewportSize().width >= 1000) {
      assertTrue(
          resendHintLocator.boundingBox().height <= 24,
          "Der kurze Versandhinweis muss auf Desktop einzeilig bleiben.");
    }
  }

  public static void assertAccountType(
      Locator dialogLocator, UiTexts uiTexts, boolean developmentAccountExpected) {
    Locator accountTypeBadgeLocator = dialogLocator.locator("#admin-user-account-type");
    assertThat(accountTypeBadgeLocator)
        .hasText(
            developmentAccountExpected
                ? uiTexts.getDevelopmentAccountLabel()
                : uiTexts.getStandardAccountLabel());
    assertThat(accountTypeBadgeLocator)
        .hasCSS("color", developmentAccountExpected ? "rgb(216, 180, 254)" : "rgb(56, 189, 248)");
    assertThat(accountTypeBadgeLocator).hasCSS("border-top-width", "1px");
  }
}
