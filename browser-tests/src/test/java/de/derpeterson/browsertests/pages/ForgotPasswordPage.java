package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.FORGOT_PASSWORD_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.FORGOT_PASSWORD_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Anonymous reset request, reached through the visible login link. */
public final class ForgotPasswordPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public ForgotPasswordPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Passwort vergessen über den Link im Loginformular öffnen")
  public void open() {
    new LoginPage(browserPage).open();
    browserPage
        .getByRole(
            AriaRole.LINK,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getLoginForgotPasswordLink())
                .setExact(true))
        .click();

    assertThat(browserPage).hasURL(urlPattern(FORGOT_PASSWORD_ROUTE));
    assertThat(browserPage).hasTitle(FORGOT_PASSWORD_PAGE_TITLE);
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getForgotPasswordTitle())
                    .setExact(true)))
        .isVisible();
    assertThat(emailField()).isVisible();
  }

  @Step("Eigene E-Mail-Adresse in das Anfrageformular eingeben")
  public void enterEmail(String email) {
    emailField().fill(email);
  }

  @Step("Reset-Anfrage über den sichtbaren Senden-Button absenden")
  public void submit() {
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(uiTexts.getPublicRequestLink()).setExact(true))
        .click();
  }

  @Step("Neutralen Anfragehinweis mit Erfolgstitel prüfen")
  public String requestedResponse() {
    Locator passwordResetNoticeLocator = requestedNotice();

    assertThat(passwordResetNoticeLocator).isVisible();
    assertThat(passwordResetNoticeLocator).containsText(uiTexts.getBaseSuccessTitle());
    assertThat(passwordResetNoticeLocator)
        .containsText(uiTexts.visibleText(browserPage, uiTexts.getForgotPasswordSuccessMessage()));
    assertThat(browserPage).hasURL(urlPattern(FORGOT_PASSWORD_ROUTE));
    return passwordResetNoticeLocator.innerText();
  }

  @Step(
      "Vor einer weiteren Anfrage die vorhandene Bestätigung über ihren Schließen-Button schließen")
  public void dismissRequestedNotice() {
    Locator passwordResetNoticeLocator = requestedNotice();
    passwordResetNoticeLocator.getByRole(AriaRole.BUTTON).click();

    assertThat(passwordResetNoticeLocator).not().isVisible();
  }

  @Step("Pflichtfeldhinweis und markierte E-Mail prüfen")
  public void assertRequiredError() {
    assertError(uiTexts.getBaseValidationRequiredMessage());
  }

  @Step("Ungültige E-Mail mit verständlichem Hinweis und Feldmarkierung prüfen")
  public void assertEmailError() {
    assertError(uiTexts.getBaseValidationEmailInvalidMessage());
  }

  private Locator emailField() {
    return browserPage.getByLabel(
        uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true));
  }

  private Locator requestedNotice() {
    return browserPage
        .locator("vaadin-notification-card:not([closing])")
        .filter(
            new Locator.FilterOptions()
                .setHasText(
                    uiTexts.visibleText(browserPage, uiTexts.getForgotPasswordSuccessMessage())));
  }

  private void assertError(String message) {
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.visibleText(browserPage, message));
    assertThat(browserPage.locator("vaadin-email-field[invalid]")).hasCount(1);
    assertThat(requestedNotice()).hasCount(0);
  }
}
