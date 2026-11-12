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
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public LoginPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Loginformular öffnen")
  public void open() {
    browserPage.navigate(path(LOGIN_ROUTE));

    // Vaadin rendert das Formular erst nach dem HTML-Laden. Vor den Inhaltsprüfungen
    // auf das sichtbare Eingabefeld warten, auch im frisch gestarteten Browserprozess.
    browserPage
        .getByLabel(uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true))
        .waitFor();

    assertFormVisible();
  }

  @Step("E-Mail und Passwort eingeben und Anmeldung absenden")
  public void signIn(String email, String password) {
    signInFirstFactor(email, password);
    TwoFactorPage.completeIfRequired(browserPage, email, password);
  }

  public void signInFirstFactor(String email, String password) {
    for (Locator notificationCardLocator :
        browserPage.locator("vaadin-notification-card:not([closing])").all()) {
      notificationCardLocator.getByRole(AriaRole.BUTTON).click();
    }
    browserPage
        .getByLabel(uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true))
        .fill(email);
    browserPage
        .getByLabel(uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true))
        .fill(password);
    Locator loginSubmitButtonLocator =
        browserPage
            .locator(".public-panel")
            .getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                    .setName(uiTexts.getBaseLoginButton())
                    .setExact(true));
    UiActions.complete(browserPage, loginSubmitButtonLocator::click);
    browserPage.waitForCondition(() ->
        !java.net.URI.create(browserPage.url()).getPath().equals(path(LOGIN_ROUTE))
            || browserPage.locator("vaadin-notification-card:not([closing])").isVisible()
            || browserPage.locator("#session-recovery").isVisible());
  }

  @Step("Loginformular ist in der ausgewählten Sprache sichtbar")
  public void assertFormVisible() {
    assertThat(browserPage).hasURL(urlPattern(LOGIN_ROUTE));
    assertThat(browserPage).hasTitle(LOGIN_PAGE_TITLE);
    assertThat(
            browserPage.getByLabel(
                uiTexts.getPublicEmailField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByLabel(
                uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(uiTexts.getLoginTitle()).setExact(true)))
        .isVisible();
  }

  @Step("Verständlicher Anmeldefehler ohne Zugriff auf den Adminbereich")
  public void assertRejected() {
    assertFormVisible();

    assertThat(
            browserPage
                .locator("vaadin-notification-card:not([closing])")
                .getByText(
                    uiTexts.getLoginFailedMessage(), new Locator.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            browserPage.getByRole(
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
            browserPage.getByText(
                uiTexts.getLoginSessionLimitMessage(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Step("Nach Passwortprüfung nur die eingeschränkte Sitzungsverwaltung anzeigen")
  public void assertRestrictedManagement() {
    assertThat(browserPage).hasURL(urlPattern(LOGIN_ROUTE));
    assertThat(browserPage.locator("#session-recovery")).isVisible();
    assertThat(browserPage.locator("#session-recovery"))
        .containsText(uiTexts.getSessionRecoveryHint());
    assertThat(browserPage.locator("#session-recovery-continue")).isVisible();
    assertThat(
            browserPage.getByLabel(
                uiTexts.getLoginPasswordField(), new Page.GetByLabelOptions().setExact(true)))
        .not()
        .isVisible();
  }

  @Step("Eine bestehende Sitzung in der eingeschränkten Anmeldung widerrufen")
  public void revokeRecoverySession(int index) {
    UiActions.complete(
        browserPage,
        () ->
            browserPage
                .locator("#account-session-entries [data-session-state='active'] vaadin-button")
                .nth(index)
                .click());
    new AccountSessionsPage(browserPage).confirmDeviceRevocation();
  }

  @Step("Eingeschränkte Verwaltung abbrechen und zur normalen Anmeldung zurückkehren")
  public void cancelRecovery() {
    UiActions.complete(browserPage, () -> browserPage.locator("#session-recovery-cancel").click());
    assertFormVisible();
  }

  @Step("Vorübergehende Anmeldesperre ohne Kontoauskunft prüfen")
  public void assertThrottled() {
    assertFormVisible();

    assertThat(
            browserPage.getByText(
                uiTexts.getLoginThrottledMessage(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Step("Vorhandenen Fehlerhinweis vor dem nächsten Anmeldeversuch schließen")
  public void dismissFailure() {
    Locator notificationCardLocator =
        browserPage.locator("vaadin-notification-card:not([closing])");

    assertThat(notificationCardLocator).hasCount(1);
    notificationCardLocator.getByRole(AriaRole.BUTTON).click();

    assertThat(notificationCardLocator).not().isVisible();
  }

  @Step("Admin-Dashboard und angemeldetes Konto sind sichtbar")
  public void assertAdminSignedIn(String email) {
    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE));
    assertThat(
            browserPage.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                    .setName(uiTexts.getAdminDashboardTitle())
                    .setExact(true)
                    .setLevel(1)))
        .isVisible();
    UserMenu userMenu = new UserMenu(browserPage);
    userMenu.open();

    assertThat(browserPage.locator(".user-popover-body")).containsText(email);
    userMenu.close();
  }
}
