package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/** Matches the visible selected value separately from options and their descriptions. */
public final class UserStatusMenu {
  private final Page browserPage;

  public UserStatusMenu(Page browserPage) {
    this.browserPage = browserPage;
  }

  public Locator current() {
    return browserPage.locator(".user-popover-body .portal-status-option[aria-pressed=true]");
  }

  @Step("Ausgewählten Benutzerstatus im geöffneten Menü prüfen")
  public void expect(String label) {
    new UserMenu(browserPage).open();
    assertThat(current()).containsText(label);
  }

  @Step("Manuellen Status über die tatsächlich angebotene Option auswählen")
  public void select(String label) {
    new UserMenu(browserPage).open();
    Locator option =
        browserPage
            .locator(".user-popover-body .portal-status-option")
            .filter(
                new Locator.FilterOptions()
                    .setHas(
                        browserPage.getByText(label, new Page.GetByTextOptions().setExact(true))));
    assertThat(option).hasCount(1);
    UiActions.complete(browserPage, option::click);
    assertThat(current()).containsText(label);
  }
}
