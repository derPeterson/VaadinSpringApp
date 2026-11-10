package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.RESET_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.RESET_PASSWORD_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Actual token-bound form and visible reset results; never reads token state from the database. */
public final class ResetPasswordPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public ResetPasswordPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Echter Reset-Link zeigt das Passwortformular in der ausgewählten Sprache")
  public void assertFormVisible() {
    assertThat(browserPage).hasURL(urlPattern(RESET_PASSWORD_ROUTE, "/[0-9a-f-]+"));
    assertThat(browserPage).hasTitle(RESET_PAGE_TITLE);
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getPublicResetFormTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByLabel(
                uiTexts.getResetPasswordNewPasswordField(),
                new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByLabel(
                uiTexts.getResetPasswordConfirmField(),
                new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
  }

  @Step("Neues Passwort und Bestätigung in die sichtbaren Felder eingeben")
  public void fillPasswords(String password, String confirmation) {
    browserPage
        .getByLabel(
            uiTexts.getResetPasswordNewPasswordField(), new Page.GetByLabelOptions().setExact(true))
        .fill(password);
    browserPage
        .getByLabel(
            uiTexts.getResetPasswordConfirmField(), new Page.GetByLabelOptions().setExact(true))
        .fill(confirmation);
  }

  @Step("Passwortänderung über den Reset-Button absenden")
  public void submit() {
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getPublicSavePasswordAction())
                .setExact(true))
        .click();
  }

  @Step("Erfolgreiche Passwortänderung wird sichtbar bestätigt")
  public void assertChanged() {
    Locator notice = browserPage.locator("vaadin-notification-card:not([closing])");
    assertThat(notice).containsText(uiTexts.getBaseSuccessTitle());
    assertThat(notice)
        .containsText(uiTexts.visibleText(browserPage, uiTexts.getResetPasswordSuccessMessage()));
    assertThat(browserPage.locator("vaadin-password-field")).hasCount(0);
    assertThat(
            browserPage
                .locator(".cosmos-card")
                .getByRole(
                    AriaRole.HEADING,
                    new com.microsoft.playwright.Locator.GetByRoleOptions()
                        .setName(uiTexts.getBaseSuccessTitle())
                        .setExact(true)))
        .isVisible();
    assertThat(
            browserPage.locator(
                ".cosmos-card vaadin-icon[src='icons/lucide/circle-check.svg'].cosmos-status-icon"))
        .isVisible();
    assertThat(browserPage.locator(".cosmos-card"))
        .containsText(uiTexts.visibleText(browserPage, uiTexts.getResetPasswordSuccessMessage()));
  }

  @Step("Leere Passwortfelder sind markiert und haben einen Pflichtfeldhinweis")
  public void assertRequiredError() {
    assertError(uiTexts.getBaseValidationRequiredMessage());
  }

  @Step("Ungültiges Passwort wird an beiden Feldern verständlich abgewiesen")
  public void assertPasswordError() {
    assertError(uiTexts.getBaseValidationPasswordInvalidMessage());
  }

  @Step("Abweichende Bestätigung wird an beiden Feldern verständlich abgewiesen")
  public void assertConfirmationError() {
    assertError(uiTexts.getBaseValidationPasswordConfirmMessage());
  }

  @Step("Ungültiger oder verbrauchter Link zeigt einen roten Hinweis und kein Resetformular")
  public void assertInvalid() {
    assertRejectedLink(
        uiTexts.getResetPasswordInvalidTitle(),
        uiTexts.getResetPasswordInvalidText(),
        "text-error");
  }

  @Step("Abgelaufener Link zeigt einen gelben Hinweis und kein Resetformular")
  public void assertExpired() {
    assertRejectedLink(
        uiTexts.getResetPasswordExpiredTitle(),
        uiTexts.getResetPasswordExpiredText(),
        "cosmos-status-icon--warning");
  }

  private void assertRejectedLink(String title, String text, String iconClass) {
    assertThat(browserPage).hasURL(urlPattern(RESET_PASSWORD_ROUTE, "/[0-9a-f-]+"));
    assertThat(browserPage).hasTitle(RESET_PAGE_TITLE);
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setExact(true)))
        .isVisible();
    assertThat(browserPage.getByText(text, new Page.GetByTextOptions().setExact(true))).isVisible();
    assertThat(
            browserPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getPublicSavePasswordAction())
                    .setExact(true)))
        .hasCount(0);
    assertThat(browserPage.locator("vaadin-password-field")).hasCount(0);
    assertThat(
            browserPage.locator(
                ".cosmos-card vaadin-icon[src='icons/lucide/circle-alert.svg']." + iconClass))
        .isVisible();
  }

  private void assertError(String message) {
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.visibleText(browserPage, message));
    assertThat(browserPage.locator("vaadin-password-field[invalid]")).hasCount(2);
    assertThat(
            browserPage
                .locator("vaadin-notification-card:not([closing])")
                .filter(
                    new com.microsoft.playwright.Locator.FilterOptions()
                        .setHasText(uiTexts.getBaseSuccessTitle())))
        .hasCount(0);
  }
}
