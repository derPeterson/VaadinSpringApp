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
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public ResetPasswordPage(Page page) {
    this.page = page;
  }

  @Step("Echter Reset-Link zeigt das Passwortformular in der ausgewählten Sprache")
  public void assertFormVisible() {
    assertThat(page).hasURL(urlPattern(RESET_PASSWORD_ROUTE, "/[0-9a-f-]+"));
    assertThat(page).hasTitle(RESET_PAGE_TITLE);
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getPublicResetFormTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            page.getByLabel(
                texts.getResetPasswordNewPasswordField(),
                new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByLabel(
                texts.getResetPasswordConfirmField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
  }

  @Step("Neues Passwort und Bestätigung in die sichtbaren Felder eingeben")
  public void fillPasswords(String password, String confirmation) {
    page.getByLabel(
            texts.getResetPasswordNewPasswordField(), new Page.GetByLabelOptions().setExact(true))
        .fill(password);
    page.getByLabel(
            texts.getResetPasswordConfirmField(), new Page.GetByLabelOptions().setExact(true))
        .fill(confirmation);
  }

  @Step("Passwortänderung über den Reset-Button absenden")
  public void submit() {
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getPublicSavePasswordAction()).setExact(true))
        .click();
  }

  @Step("Erfolgreiche Passwortänderung wird sichtbar bestätigt")
  public void assertChanged() {
    Locator notice = page.locator("vaadin-notification-card:not([closing])");
    assertThat(notice).containsText(texts.getBaseSuccessTitle());
    assertThat(notice)
        .containsText(texts.visibleText(page, texts.getResetPasswordSuccessMessage()));
    assertThat(page.locator("vaadin-password-field")).hasCount(0);
    assertThat(
            page.locator(".cosmos-card")
                .getByRole(
                    AriaRole.HEADING,
                    new com.microsoft.playwright.Locator.GetByRoleOptions()
                        .setName(texts.getBaseSuccessTitle())
                        .setExact(true)))
        .isVisible();
    assertThat(
            page.locator(
                ".cosmos-card vaadin-icon[src='icons/lucide/circle-check.svg'].cosmos-status-icon"))
        .isVisible();
    assertThat(page.locator(".cosmos-card"))
        .containsText(texts.visibleText(page, texts.getResetPasswordSuccessMessage()));
  }

  @Step("Leere Passwortfelder sind markiert und haben einen Pflichtfeldhinweis")
  public void assertRequiredError() {
    assertError(texts.getBaseValidationRequiredMessage());
  }

  @Step("Ungültiges Passwort wird an beiden Feldern verständlich abgewiesen")
  public void assertPasswordError() {
    assertError(texts.getBaseValidationPasswordInvalidMessage());
  }

  @Step("Abweichende Bestätigung wird an beiden Feldern verständlich abgewiesen")
  public void assertConfirmationError() {
    assertError(texts.getBaseValidationPasswordConfirmMessage());
  }

  @Step("Ungültiger oder verbrauchter Link zeigt einen roten Hinweis und kein Resetformular")
  public void assertInvalid() {
    assertRejectedLink(
        texts.getResetPasswordInvalidTitle(), texts.getResetPasswordInvalidText(), "text-error");
  }

  @Step("Abgelaufener Link zeigt einen gelben Hinweis und kein Resetformular")
  public void assertExpired() {
    assertRejectedLink(
        texts.getResetPasswordExpiredTitle(),
        texts.getResetPasswordExpiredText(),
        "cosmos-status-icon--warning");
  }

  private void assertRejectedLink(String title, String text, String iconClass) {
    assertThat(page).hasURL(urlPattern(RESET_PASSWORD_ROUTE, "/[0-9a-f-]+"));
    assertThat(page).hasTitle(RESET_PAGE_TITLE);
    assertThat(
            page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setExact(true)))
        .isVisible();
    assertThat(page.getByText(text, new Page.GetByTextOptions().setExact(true))).isVisible();
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(texts.getPublicSavePasswordAction())
                    .setExact(true)))
        .hasCount(0);
    assertThat(page.locator("vaadin-password-field")).hasCount(0);
    assertThat(
            page.locator(
                ".cosmos-card vaadin-icon[src='icons/lucide/circle-alert.svg']." + iconClass))
        .isVisible();
  }

  private void assertError(String message) {
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.visibleText(page, message));
    assertThat(page.locator("vaadin-password-field[invalid]")).hasCount(2);
    assertThat(
            page.locator("vaadin-notification-card:not([closing])")
                .filter(
                    new com.microsoft.playwright.Locator.FilterOptions()
                        .setHasText(texts.getBaseSuccessTitle())))
        .hasCount(0);
  }
}
