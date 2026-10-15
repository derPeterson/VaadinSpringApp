package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Anonymous reset request, reached through the visible login link. */
public final class ForgotPasswordPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public ForgotPasswordPage(Page page) {
    this.page = page;
  }

  @Step("Passwort vergessen über den Link im Loginformular öffnen")
  public void open() {
    new LoginPage(page).open();
    page.getByRole(
            AriaRole.LINK,
            new Page.GetByRoleOptions().setName(texts.getLoginForgotPasswordLink()).setExact(true))
        .click();
    assertThat(page).hasURL(urlPattern(FORGOT_PASSWORD_ROUTE));
    assertThat(page).hasTitle(FORGOT_PASSWORD_PAGE_TITLE);
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(texts.getForgotPasswordTitle()).setExact(true)))
        .isVisible();
    assertThat(emailField()).isVisible();
  }

  @Step("Eigene E-Mail-Adresse in das Anfrageformular eingeben")
  public void enterEmail(String email) {
    emailField().fill(email);
  }

  @Step("Reset-Anfrage über den sichtbaren Senden-Button absenden")
  public void submit() {
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getBaseSendButton()).setExact(true))
        .click();
  }

  @Step("Neutralen Anfragehinweis mit Erfolgstitel prüfen")
  public String requestedResponse() {
    Locator notice = requestedNotice();
    assertThat(notice).isVisible();
    assertThat(notice).containsText(texts.getBaseSuccessTitle());
    assertThat(notice)
        .containsText(texts.visibleText(page, texts.getForgotPasswordSuccessMessage()));
    assertThat(page).hasURL(urlPattern(FORGOT_PASSWORD_ROUTE));
    return notice.innerText();
  }

  @Step(
      "Vor einer weiteren Anfrage die vorhandene Bestätigung über ihren Schließen-Button schließen")
  public void dismissRequestedNotice() {
    Locator notice = requestedNotice();
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }

  @Step("Pflichtfeldhinweis und markierte E-Mail prüfen")
  public void assertRequiredError() {
    assertError(texts.getBaseValidationRequiredMessage());
  }

  @Step("Ungültige E-Mail mit verständlichem Hinweis und Feldmarkierung prüfen")
  public void assertEmailError() {
    assertError(texts.getBaseValidationEmailInvalidMessage());
  }

  private Locator emailField() {
    return page.getByLabel(
        texts.getForgotPasswordEmailField(), new Page.GetByLabelOptions().setExact(true));
  }

  private Locator requestedNotice() {
    return page.locator("vaadin-notification-card:not([closing])")
        .filter(
            new Locator.FilterOptions()
                .setHasText(texts.visibleText(page, texts.getForgotPasswordSuccessMessage())));
  }

  private void assertError(String message) {
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.visibleText(page, message));
    assertThat(page.locator("vaadin-email-field[invalid]")).hasCount(1);
    assertThat(requestedNotice()).hasCount(0);
  }
}
