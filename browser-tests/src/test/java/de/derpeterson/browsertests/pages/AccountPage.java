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
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public AccountPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Kontoverwaltung über das Benutzermenü öffnen")
  public void open() {
    new UserMenu(browserPage).open();
    browserPage
        .locator(".user-popover-body")
        .getByRole(
            AriaRole.BUTTON,
            new com.microsoft.playwright.Locator.GetByRoleOptions()
                .setName(uiTexts.getBaseManageAccountButton())
                .setExact(true))
        .click();

    assertThat(browserPage.locator("#account-panel-profile")).isVisible();
  }

  public Locator input(String id) {
    return browserPage.locator("#" + id).locator("input");
  }

  public Locator host(String id) {
    return browserPage.locator("#" + id);
  }

  @Step("Basisdaten im Profilformular eingeben")
  public void fill(String firstName, String lastName, LocalDate birthDate) {
    input("account-first-name").fill(firstName);
    input("account-last-name").fill(lastName);
    input("account-birth-date").fill(uiTexts.dateInput(birthDate));
    input("account-first-name").click();

    assertThat(host("account-birth-date")).hasJSProperty("value", birthDate.toString());
  }

  @Step("Profiländerungen speichern")
  public void save() {
    UiActions.complete(browserPage, () -> host("account-save").click());
  }

  public void assertMessage(String message) {
    assertThat(
            browserPage
                .locator("vaadin-notification-card")
                .getByText(message, new Locator.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  public void closeNotification() {
    browserPage.locator("vaadin-notification-card").getByRole(AriaRole.BUTTON).click();

    assertThat(browserPage.locator("vaadin-notification-card")).hasCount(0);
  }

  @Step("Bevorzugte Sprache im Profil wählen")
  public void language(String language) {
    host("account-language").click();
    browserPage
        .getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(
                    Locale.forLanguageTag(language)
                        .getDisplayLanguage(Locale.forLanguageTag(uiTexts.language())))
                .setExact(true))
        .click();
    input("account-first-name").click();
  }

  public Locator dialog() {
    return browserPage.locator("vaadin-dialog.account-email-dialog[opened]");
  }

  @Step("Kontobereich über die Seitennavigation auswählen")
  public void section(String sectionNavigationId) {
    assertThat(host(sectionNavigationId)).hasCount(1);

    if (!host(sectionNavigationId).isVisible() && host("account-navigation-toggle").isVisible()) {
      UiActions.complete(browserPage, () -> host("account-navigation-toggle").click());

      assertThat(host("account-navigation-toggle")).hasAttribute("aria-expanded", "true");
    }

    host(sectionNavigationId).click();
  }

  @Step("E-Mail-Dialog öffnen")
  public void openEmail() {
    section("account-tab-security");
    host("account-email-edit-security").click();

    assertThat(input("account-email-new")).isVisible();
  }

  @Step("E-Mail-Dialog auf dem gewählten UI-Weg schließen")
  public void closeEmail(String closeAction) {
    switch (closeAction) {
      case "icon" -> host("account-email-close").click();
      case "cancel" -> host("account-email-cancel").click();
      case "escape" -> browserPage.keyboard().press("Escape");
      case "outside" -> browserPage.mouse().click(4, 4);
      default -> throw new IllegalArgumentException(closeAction);
    }

    assertThat(dialog()).hasCount(0);
    assertThat(browserPage.locator("vaadin-dialog.account-email-dialog[closing]")).hasCount(0);
  }
}
