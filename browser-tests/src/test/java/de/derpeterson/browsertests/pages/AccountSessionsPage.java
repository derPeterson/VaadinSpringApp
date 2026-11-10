package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

public final class AccountSessionsPage {
  private final Page page;
  private final UiTexts uiTexts = UiTexts.selected();

  public AccountSessionsPage(Page page) {
    this.page = page;
  }

  @Step("Eigene Sitzungsliste im Konto öffnen")
  public void open() {
    new AccountPage(page).open();
    new AccountPage(page).section("account-tab-sessions");
    assertThat(page.locator("#account-panel-sessions")).isVisible();
  }

  public Locator rows(String state) {
    return page.locator("#account-session-entries [data-session-state='" + state + "']");
  }

  @Step("Aktive Anzahl, andere Sitzungen und gespeicherte Zugänge prüfen")
  public void assertCounts(int active, int other, int saved) {
    assertThat(page.locator("#account-panel-sessions vaadin-badge.account-session-count"))
        .hasText(uiTexts.getPortalSessionActiveCount(active));
    assertThat(rows("current")).hasCount(1);
    assertThat(page.locator("#account-current-session")).isDisabled();
    assertThat(rows("active")).hasCount(other);
    assertThat(rows("saved")).hasCount(saved);
    assertGroups(active, saved);
  }

  public void assertGroups(int active, int saved) {
    Locator activeGroup = page.locator("#account-session-group-active");
    Locator savedGroup = page.locator("#account-session-group-saved");
    assertThat(activeGroup).hasText(uiTexts.getPortalSessionActiveGroup(active));
    assertThat(activeGroup).hasCSS("text-transform", "uppercase");
    assertThat(activeGroup).hasCSS("color", "rgb(56, 189, 248)");
    if (saved > 0) {
      assertThat(savedGroup).hasText(uiTexts.getPortalSessionSavedGroup(saved));
      assertThat(savedGroup).hasCSS("text-transform", "uppercase");
      assertThat(savedGroup).hasCSS("color", "rgb(148, 163, 184)");
    } else {
      assertThat(savedGroup).hasCount(0);
    }
  }

  public void assertSummaryAlignment() {
    Locator heading = page.locator(".account-session-heading");
    assertThat(heading.locator(".account-session-summary")).hasCSS("row-gap", "6px");
    assertTrue(
        (Boolean)
            heading.evaluate(
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
    Locator card = page.locator("#sessions-confirmation .portal-context-card");
    Locator method = card.locator(".account-session-sign-in-method .account-session-detail-value");
    assertThat(method).hasText(uiTexts.getSignInPortalPassword());
    assertThat(method).hasCSS("font-weight", "700");
    assertThat(method).hasCSS("color", "rgb(255, 255, 255)");
    assertThat(card.locator(".portal-context-detail > .account-session-detail-value")).hasCount(3);
    assertThat(card.locator("vaadin-badge.account-session-state"))
        .hasText(uiTexts.getPortalSessionActive());
    assertThat(card.locator("vaadin-badge.account-session-state"))
        .hasCSS("border-top-width", "1px");
  }

  @Step("Andere Sitzung gezielt abmelden")
  public void revokeOther(int index) {
    UiActions.complete(page, () -> rows("active").nth(index).locator("vaadin-button").click());
    confirmDeviceRevocation();
  }

  @Step("Gespeicherten Zugang widerrufen")
  public void revokeSaved(int index) {
    UiActions.complete(page, () -> rows("saved").nth(index).locator("vaadin-button").click());
    confirmDeviceRevocation();
  }

  @Step("Alle anderen Sitzungen abmelden")
  public void revokeOthers() {
    UiActions.complete(page, () -> page.locator("#account-logout-others").click());
    confirmDeviceRevocation();
  }

  public void confirmDeviceRevocation() {
    assertThat(
            page.locator(
                "#sessions-confirmation[opened] vaadin-dialog-overlay > [part~='overlay']"))
        .isVisible();
    UiActions.complete(page, () -> page.locator("#sessions-confirmation-submit").click());
    assertThat(page.locator("#sessions-confirmation[opened]")).hasCount(0);
  }

  @Step("Sitzungsliste über Aktualisieren neu laden")
  public void refreshSessionList() {
    UiActions.complete(page, () -> page.locator("#account-sessions-refresh").click());
  }
}
