package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

public final class AdminMailPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public AdminMailPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Mailtest über das Adminmenü öffnen")
  public void open() {
    new AdminNavigation(browserPage, uiTexts).openSection(uiTexts.getAdminMailTitle());
    assertThat(input(uiTexts.getAdminMailRecipient())).isVisible();
  }

  public Locator input(String label) {
    return browserPage
        .getByLabel(label, new Page.GetByLabelOptions().setExact(true))
        .and(browserPage.locator("input, textarea"));
  }

  @Step("Testmail über den regulären Versandbutton absenden")
  public void send() {
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(uiTexts.getAdminMailSend()).setExact(true))
        .click();
  }

  @Step("Testmail bewusst per Enter im Betreff absenden")
  public void sendFromSubject() {
    input(uiTexts.getAdminMailSubject()).press("Enter");
  }

  public void notice(String message) {
    Locator notice =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(message));
    assertThat(notice).isVisible();
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }
}
