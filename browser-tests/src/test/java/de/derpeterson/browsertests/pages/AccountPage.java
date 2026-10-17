package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import java.util.Locale;

/** Kontoseite nur über sichtbare UI-Aktionen; keine internen Service-/DB-Zugriffe. */
public final class AccountPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public AccountPage(Page page) {
    this.page = page;
  }

  @Step("Kontoverwaltung über das Benutzermenü öffnen")
  public void open() {
    new UserMenu(page).open();
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getBaseManageAccountButton()).setExact(true))
        .click();
    assertThat(page.locator("#account-panel-profile")).isVisible();
  }

  public Locator input(String id) {
    return page.locator("#" + id).locator("input");
  }

  public Locator host(String id) {
    return page.locator("#" + id);
  }

  @Step("Basisdaten im Profilformular eingeben")
  public void fill(String first, String last, LocalDate date) {
    input("account-first-name").fill(first);
    input("account-last-name").fill(last);
    input("account-birth-date").fill(texts.dateInput(date));
    input("account-first-name").click();
    assertThat(host("account-birth-date")).hasJSProperty("value", date.toString());
  }

  @Step("Profiländerungen speichern")
  public void save() {
    UiActions.complete(page, () -> host("account-save").click());
  }

  public void assertMessage(String message) {
    assertThat(
            page.locator("vaadin-notification-card")
                .getByText(message, new Locator.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  public void closeNotification() {
    page.locator("vaadin-notification-card").getByRole(AriaRole.BUTTON).click();
    assertThat(page.locator("vaadin-notification-card")).hasCount(0);
  }

  @Step("Bevorzugte Sprache im Profil wählen")
  public void language(String language) {
    host("account-language").click();
    page.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(
                    Locale.forLanguageTag(language)
                        .getDisplayLanguage(Locale.forLanguageTag(language)))
                .setExact(true))
        .click();
    input("account-first-name").click();
  }

  public Locator dialog() {
    return page.locator("vaadin-dialog.account-email-dialog[opened]");
  }

  @Step("E-Mail-Dialog öffnen")
  public void openEmail() {
    host("account-tab-security").click();
    host("account-email-edit-security").click();
    assertThat(input("account-email-new")).isVisible();
  }

  @Step("E-Mail-Dialog auf dem gewählten UI-Weg schließen")
  public void closeEmail(String action) {
    switch (action) {
      case "icon" -> host("account-email-close").click();
      case "cancel" -> host("account-email-cancel").click();
      case "escape" -> page.keyboard().press("Escape");
      case "outside" -> page.mouse().click(4, 4);
      default -> throw new IllegalArgumentException(action);
    }
    assertThat(dialog()).hasCount(0);
    assertThat(page.locator("vaadin-dialog.account-email-dialog[closing]")).hasCount(0);
  }
}
