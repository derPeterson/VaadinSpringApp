package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

public final class AdminMailPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public AdminMailPage(Page page) {
    this.page = page;
  }

  @Step("Mailtest über das Adminmenü öffnen")
  public void open() {
    new AdminNavigation(page, texts).openSection(texts.getAdminMailTitle());
    assertThat(input(texts.getAdminMailRecipient())).isVisible();
  }

  public Locator input(String label) {
    return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true))
        .and(page.locator("input, textarea"));
  }

  @Step("Testmail über den regulären Versandbutton absenden")
  public void send() {
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getAdminMailSend()).setExact(true))
        .click();
  }

  @Step("Testmail bewusst per Enter im Betreff absenden")
  public void sendFromSubject() {
    input(texts.getAdminMailSubject()).press("Enter");
  }

  public void notice(String message) {
    Locator notice =
        page.locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(message));
    assertThat(notice).isVisible();
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }
}
