package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

public final class AccountSessionsPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

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
    assertThat(
            page.locator("#account-panel-sessions .account-session-heading .account-entry-title"))
        .hasText(texts.getPortalSessionActiveCount(active));
    assertThat(rows("current")).hasCount(1);
    assertThat(page.locator("#account-current-session")).isDisabled();
    assertThat(rows("active")).hasCount(other);
    assertThat(rows("saved")).hasCount(saved);
  }

  @Step("Andere Sitzung gezielt abmelden")
  public void revokeOther(int index) {
    UiActions.complete(page, () -> rows("active").nth(index).locator("vaadin-button").click());
    confirm();
  }

  @Step("Gespeicherten Zugang widerrufen")
  public void revokeSaved(int index) {
    UiActions.complete(page, () -> rows("saved").nth(index).locator("vaadin-button").click());
    confirm();
  }

  @Step("Alle anderen Sitzungen abmelden")
  public void revokeOthers() {
    UiActions.complete(page, () -> page.locator("#account-logout-others").click());
    confirm();
  }

  public void confirm() {
    assertThat(
            page.locator(
                "#sessions-confirmation[opened] vaadin-dialog-overlay > [part~='overlay']"))
        .isVisible();
    UiActions.complete(page, () -> page.locator("#sessions-confirmation-submit").click());
    assertThat(page.locator("#sessions-confirmation[opened]")).hasCount(0);
  }

  @Step("Sitzungsliste über Aktualisieren neu laden")
  public void refresh() {
    UiActions.complete(page, () -> page.locator("#account-sessions-refresh").click());
  }
}
