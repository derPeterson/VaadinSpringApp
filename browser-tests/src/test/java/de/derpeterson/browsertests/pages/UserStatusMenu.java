package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** Matches the visible selected value separately from options and their descriptions. */
public final class UserStatusMenu {
  private final Page page;

  public UserStatusMenu(Page page) {
    this.page = page;
  }

  public Locator current() {
    return page.locator(".user-popover-body vaadin-select-value-button");
  }

  @Step("Ausgewählten Benutzerstatus im geöffneten Menü prüfen")
  public void expect(String label) {
    new UserMenu(page).open();
    assertThat(current()).containsText(label);
  }

  @Step("Manuellen Status über die tatsächlich angebotene Option auswählen")
  public void select(String label) {
    new UserMenu(page).open();
    current().click();
    Locator option =
        page.getByRole(AriaRole.OPTION)
            .filter(
                new Locator.FilterOptions()
                    .setHas(page.getByText(label, new Page.GetByTextOptions().setExact(true))));
    assertThat(option).hasCount(1);
    UiActions.completeValueChange(page, () -> option.click());
    assertThat(current()).containsText(label);
  }
}
