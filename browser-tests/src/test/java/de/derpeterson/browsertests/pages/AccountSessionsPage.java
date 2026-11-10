package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

public final class AccountSessionsPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public AccountSessionsPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Eigene Sitzungsliste im Konto öffnen")
  public void open() {
    new AccountPage(browserPage).open();
    new AccountPage(browserPage).section("account-tab-sessions");

    assertThat(browserPage.locator("#account-panel-sessions")).isVisible();
  }

  public Locator rows(String state) {
    return browserPage.locator("#account-session-entries [data-session-state='" + state + "']");
  }

  @Step("Aktive Anzahl, andere Sitzungen und gespeicherte Zugänge prüfen")
  public void assertCounts(int activeSessionCount, int otherSessionCount, int savedSignInCount) {
    assertThat(browserPage.locator("#account-panel-sessions vaadin-badge.account-session-count"))
        .hasText(uiTexts.getPortalSessionActiveCount(activeSessionCount));
    assertThat(rows("current")).hasCount(1);
    assertThat(browserPage.locator("#account-current-session")).isDisabled();
    assertThat(rows("active")).hasCount(otherSessionCount);
    assertThat(rows("saved")).hasCount(savedSignInCount);
    assertGroups(activeSessionCount, savedSignInCount);
  }

  public void assertGroups(int activeSessionCount, int savedSignInCount) {
    Locator activeGroupLocator = browserPage.locator("#account-session-group-active");
    Locator savedGroupLocator = browserPage.locator("#account-session-group-saved");

    assertThat(activeGroupLocator).hasText(uiTexts.getPortalSessionActiveGroup(activeSessionCount));
    assertThat(activeGroupLocator).hasCSS("text-transform", "uppercase");
    assertThat(activeGroupLocator).hasCSS("color", "rgb(56, 189, 248)");
    if (savedSignInCount > 0) {
      assertThat(savedGroupLocator).hasText(uiTexts.getPortalSessionSavedGroup(savedSignInCount));
      assertThat(savedGroupLocator).hasCSS("text-transform", "uppercase");
      assertThat(savedGroupLocator).hasCSS("color", "rgb(148, 163, 184)");
    } else {
      assertThat(savedGroupLocator).hasCount(0);
    }
  }

  public void assertSummaryAlignment() {
    Locator sessionHeadingLocator = browserPage.locator(".account-session-heading");

    assertThat(sessionHeadingLocator.locator(".account-session-summary")).hasCSS("row-gap", "6px");
    assertTrue(
        (Boolean)
            sessionHeadingLocator.evaluate(
                """
                heading => {
                  const summary = heading.querySelector('.account-session-summary').getBoundingClientRect();
                  const limit = heading.querySelector('#account-sessions-limit').getBoundingClientRect();
                  return Math.abs(summary.top + summary.height / 2 - limit.top - limit.height / 2) < 1
                      && limit.left >= summary.right;
                }
                """));
  }

  public void assertConfirmationDetails() {
    Locator confirmationCardLocator =
        browserPage.locator("#sessions-confirmation .portal-context-card");
    Locator signInMethodLocator =
        confirmationCardLocator.locator(
            ".account-session-sign-in-method .account-session-detail-value");

    assertThat(signInMethodLocator).hasText(uiTexts.getSignInPortalPassword());
    assertThat(signInMethodLocator).hasCSS("font-weight", "700");
    assertThat(signInMethodLocator).hasCSS("color", "rgb(255, 255, 255)");
    assertThat(
            confirmationCardLocator.locator(
                ".portal-context-detail > .account-session-detail-value"))
        .hasCount(3);
    assertThat(confirmationCardLocator.locator("vaadin-badge.account-session-state"))
        .hasText(uiTexts.getPortalSessionActive());
    assertThat(confirmationCardLocator.locator("vaadin-badge.account-session-state"))
        .hasCSS("border-top-width", "1px");
  }

  @Step("Andere Sitzung gezielt abmelden")
  public void revokeOther(int index) {
    UiActions.complete(
        browserPage, () -> rows("active").nth(index).locator("vaadin-button").click());
    confirmDeviceRevocation();
  }

  @Step("Gespeicherten Zugang widerrufen")
  public void revokeSaved(int index) {
    UiActions.complete(
        browserPage, () -> rows("saved").nth(index).locator("vaadin-button").click());
    confirmDeviceRevocation();
  }

  @Step("Alle anderen Sitzungen abmelden")
  public void revokeOthers() {
    UiActions.complete(browserPage, () -> browserPage.locator("#account-logout-others").click());
    confirmDeviceRevocation();
  }

  public void confirmDeviceRevocation() {
    assertThat(
            browserPage.locator(
                "#sessions-confirmation[opened] vaadin-dialog-overlay > [part~='overlay']"))
        .isVisible();
    UiActions.complete(
        browserPage, () -> browserPage.locator("#sessions-confirmation-submit").click());

    assertThat(browserPage.locator("#sessions-confirmation[opened]")).hasCount(0);
  }

  @Step("Sitzungsliste über Aktualisieren neu laden")
  public void refreshSessionList() {
    UiActions.complete(browserPage, () -> browserPage.locator("#account-sessions-refresh").click());
  }
}
