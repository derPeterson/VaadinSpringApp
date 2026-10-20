package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Only the visible login form; does not bypass the application's authentication. */
public final class LoginPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public LoginPage(Page page) {
    this.page = page;
  }

  @Step("Loginformular öffnen")
  public void open() {
    page.navigate(path(LOGIN_ROUTE));
    assertFormVisible();
  }

  @Step("E-Mail und Passwort eingeben und Anmeldung absenden")
  public void signIn(String email, String password) {
    page.getByLabel(texts.getLoginEmailField(), new Page.GetByLabelOptions().setExact(true))
        .fill(email);
    page.getByLabel(texts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true))
        .fill(password);
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getBaseLoginButton()).setExact(true))
        .click();
  }

  @Step("Loginformular ist in der ausgewählten Sprache sichtbar")
  public void assertFormVisible() {
    assertThat(page).hasURL(urlPattern(LOGIN_ROUTE));
    assertThat(page).hasTitle(LOGIN_PAGE_TITLE);
    assertThat(
            page.getByLabel(
                texts.getLoginEmailField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByLabel(
                texts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(texts.getLoginTitle()).setExact(true)))
        .isVisible();
  }

  @Step("Verständlicher Anmeldefehler ohne Zugriff auf den Adminbereich")
  public void assertRejected() {
    assertFormVisible();
    assertThat(
            page.getByText(
                texts.getLoginFailedMessage(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getAdminDashboardTitle())
                    .setExact(true)
                    .setLevel(1)))
        .hasCount(0);
  }

  @Step("Sitzungsgrenze bei gültigen Zugangsdaten prüfen")
  public void assertSessionLimit() {
    assertFormVisible();
    assertThat(
            page.getByText(
                texts.getLoginSessionLimitMessage(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Step("Nach Passwortprüfung nur die eingeschränkte Sitzungsverwaltung anzeigen")
  public void assertRestrictedManagement() {
    assertThat(page).hasURL(urlPattern(LOGIN_ROUTE));
    assertThat(page.locator("#session-recovery")).isVisible();
    assertThat(page.locator("#session-recovery")).containsText(texts.getSessionRecoveryHint());
    assertThat(page.locator("#session-recovery-continue")).isVisible();
    assertThat(
            page.getByLabel(
                texts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)))
        .not()
        .isVisible();
  }

  @Step("Eine bestehende Sitzung in der eingeschränkten Anmeldung widerrufen")
  public void revokeRecoverySession(int index) {
    UiActions.complete(
        page,
        () ->
            page.locator("#account-session-entries [data-session-state='active'] vaadin-button")
                .nth(index)
                .click());
  }

  @Step("Eingeschränkte Verwaltung abbrechen und zur normalen Anmeldung zurückkehren")
  public void cancelRecovery() {
    UiActions.complete(page, () -> page.locator("#session-recovery-cancel").click());
    assertFormVisible();
  }

  @Step("Vorübergehende Anmeldesperre ohne Kontoauskunft prüfen")
  public void assertThrottled() {
    assertFormVisible();
    assertThat(
            page.getByText(
                texts.getLoginThrottledMessage(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Step("Vorhandenen Fehlerhinweis vor dem nächsten Anmeldeversuch schließen")
  public void dismissFailure() {
    Locator notice = page.locator("vaadin-notification-card:not([closing])");
    assertThat(notice).hasCount(1);
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }

  @Step("Admin-Dashboard und angemeldetes Konto sind sichtbar")
  public void assertAdminSignedIn(String email) {
    assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(texts.getAdminDashboardTitle())
                    .setExact(true)
                    .setLevel(1)))
        .isVisible();
    assertThat(
            page.getByText(
                texts.getAdminDashboardWelcome() + ": " + email,
                new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }
}
