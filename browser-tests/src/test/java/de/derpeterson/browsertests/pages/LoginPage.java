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
  private final UiTexts uiTexts = UiTexts.selected();

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
    signInFirstFactor(email, password);
    TwoFactorPage.completeIfRequired(page, email, password);
  }

  public void signInFirstFactor(String email, String password) {
    for (Locator notice : page.locator("vaadin-notification-card:not([closing])").all()) {
      notice.getByRole(AriaRole.BUTTON).click();
    }
    page.getByLabel(uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true))
        .fill(email);
    page.getByLabel(uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true))
        .fill(password);
    Locator submit =
        page.locator(".public-panel")
            .getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                    .setName(uiTexts.getBaseLoginButton())
                    .setExact(true));
    UiActions.complete(page, submit::click);
    page.waitForCondition(() ->
        !java.net.URI.create(page.url()).getPath().equals(path(LOGIN_ROUTE))
            || page.locator("vaadin-notification-card:not([closing])").isVisible()
            || page.locator("#session-recovery").isVisible());
  }

  @Step("Loginformular ist in der ausgewählten Sprache sichtbar")
  public void assertFormVisible() {
    assertThat(page).hasURL(urlPattern(LOGIN_ROUTE));
    assertThat(page).hasTitle(LOGIN_PAGE_TITLE);
    assertThat(
            page.getByLabel(
                uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByLabel(
                uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(uiTexts.getLoginTitle()).setExact(true)))
        .isVisible();
  }

  @Step("Verständlicher Anmeldefehler ohne Zugriff auf den Adminbereich")
  public void assertRejected() {
    assertFormVisible();
    assertThat(
            page.locator("vaadin-notification-card:not([closing])")
                .getByText(
                    uiTexts.getLoginFailedMessage(), new Locator.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getAdminDashboardTitle())
                    .setExact(true)
                    .setLevel(1)))
        .hasCount(0);
  }

  @Step("Sitzungsgrenze bei gültigen Zugangsdaten prüfen")
  public void assertSessionLimit() {
    assertFormVisible();
    assertThat(
            page.getByText(
                uiTexts.getLoginSessionLimitMessage(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Step("Nach Passwortprüfung nur die eingeschränkte Sitzungsverwaltung anzeigen")
  public void assertRestrictedManagement() {
    assertThat(page).hasURL(urlPattern(LOGIN_ROUTE));
    assertThat(page.locator("#session-recovery")).isVisible();
    assertThat(page.locator("#session-recovery")).containsText(uiTexts.getSessionRecoveryHint());
    assertThat(page.locator("#session-recovery-continue")).isVisible();
    assertThat(
            page.getByLabel(
                uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)))
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
    new AccountSessionsPage(page).confirmDeviceRevocation();
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
                uiTexts.getLoginThrottledMessage(), new Page.GetByTextOptions().setExact(true)))
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
                    .setName(uiTexts.getAdminDashboardTitle())
                    .setExact(true)
                    .setLevel(1)))
        .isVisible();
    UserMenu menu = new UserMenu(page);
    menu.open();
    assertThat(page.locator(".user-popover-body")).containsText(email);
    menu.close();
  }
}
