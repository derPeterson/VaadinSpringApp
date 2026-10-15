package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;

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
                new Page.GetByRoleOptions().setName(texts.getResetPasswordTitle()).setExact(true)))
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
            new Page.GetByRoleOptions().setName(texts.getBaseResetButton()).setExact(true))
        .click();
  }

  @Step("Erfolgreiche Passwortänderung wird sichtbar bestätigt")
  public void assertChanged() {
    var notice = page.locator("vaadin-notification-card:not([closing])");
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
            page.locator(".cosmos-card vaadin-icon[icon='vaadin:check-circle'].cosmos-status-icon"))
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

  @Step("Ungültiger oder verbrauchter Link zeigt einen neutralen Hinweis und kein Resetformular")
  public void assertInvalid() {
    assertThat(page).hasURL(urlPattern(RESET_PASSWORD_ROUTE, "/[0-9a-f-]+"));
    assertThat(page).hasTitle(RESET_PAGE_TITLE);
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getResetPasswordInvalidTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(
            page.getByText(
                texts.getResetPasswordInvalidText(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(texts.getBaseResetButton()).setExact(true)))
        .hasCount(0);
    assertThat(page.locator("vaadin-password-field")).hasCount(0);
    assertThat(
            page.locator(
                ".cosmos-card vaadin-icon[icon='vaadin:warning'].cosmos-status-icon--warning"))
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
