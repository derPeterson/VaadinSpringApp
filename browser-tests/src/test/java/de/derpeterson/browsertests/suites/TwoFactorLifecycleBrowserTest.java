package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
public class TwoFactorLifecycleBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("MFA-14: Sprachwechsel erhält Mailcode, QR, einmalige Codeausgabe und Hilfsanfrage")
  void languageChangesPreserveTheCurrentSecurityPhase(int width) {
    browserPage.setViewportSize(width, 900);
    new LoginPage(browserPage).open();
    String email = "locale-mfa-" + UUID.randomUUID() + "@gmail.com";
    browserPage.locator("#external-login-google").click();
    providerIdentity(email);
    browserPage.locator("#external-register-first input").fill("Locale");
    browserPage.locator("#external-register-last input").fill("Fixture");
    browserPage.locator("#external-register-submit").click();

    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-confirm-google").click();
    providerIdentity(email);
    String mailCode = new MailpitInbox(browserContext).awaitAuthenticatorSetupCode(email);
    UiTexts other = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    new UserMenu(browserPage).selectLanguage(other.language());

    assertThat(browserPage.locator("#two-factor-code"))
        .hasJSProperty("label", other.getTwoFactorEmailCodeLabel());
    browserPage.locator("#two-factor-code input").fill(mailCode);
    browserPage.locator("#two-factor-submit").click();

    assertThat(browserPage.locator("#two-factor-qr")).isVisible();
    Locator qr = browserPage.locator("#two-factor-qr");
    String originalQr = qr.getAttribute("src");
    new UserMenu(browserPage).selectTestLanguage();

    assertThat(qr).hasAttribute("src", originalQr);
    assertThat(browserPage.locator("#two-factor-qr"))
        .hasAttribute("alt", uiTexts.getTwoFactorQrAlt());
    TwoFactorPage.confirmEnrollment(browserPage, email);
    java.util.List<String> codes = TwoFactorPage.recoveryCodeValues(browserPage);
    verifyLanguageChangeDuringRecoveryOutput(other, codes);
    new UserMenu(browserPage).signOut();
    browserPage.locator("#external-login-google").click();
    providerIdentity(email);
    TwoFactorPage.openLoginHelp(browserPage);
    browserPage.locator("#two-factor-help").click();

    assertThat(browserPage.locator("#account-support")).isVisible();
    String draft = "Own recovery request " + UUID.randomUUID();
    browserPage.locator("#support-reply textarea").fill(draft);
    new UserMenu(browserPage).selectLanguage(other.language());

    assertThat(browserPage.locator("#support-reply"))
        .hasJSProperty("label", other.getSupportDescription());
    assertThat(browserPage.locator("#support-reply textarea")).hasValue(draft);
    browserPage.locator("#support-create").click();
    Locator ticket =
        browserPage.locator("#support-ticket-grid").getByText(Pattern.compile("^#[0-9]+$"));

    assertThat(ticket).hasCount(1);
    String number = ticket.innerText();
    new UserMenu(browserPage).selectTestLanguage();

    assertThat(ticket).hasText(number);
    assertThat(browserPage.locator("#account-support")).containsText(draft);
    AllurePlaywright.attachScreenshot("Sprachwechsel im eingeschränkten Hilfszugang", browserPage);
    browserPage.navigate("/home");

    assertThat(browserPage).hasURL(Pattern.compile(".*/recovery-help$"));
    browserPage.locator("#support-back").click();

    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor$"));
    browserPage.locator("#two-factor-cancel").click();
    new LoginPage(browserPage).assertFormVisible();
  }

  private void verifyLanguageChangeDuringRecoveryOutput(
      UiTexts other, java.util.List<String> codes) {
    assertThat(browserPage.locator("#two-factor-recovery-details")).hasAttribute("opened", "");
    Locator firstCode = browserPage.locator("#two-factor-recovery-code-1");
    firstCode.locator("input").focus();

    assertThat(firstCode).hasJSProperty("value", "");
    assertThat(firstCode.locator("vaadin-button")).hasCSS("color", "rgb(56, 189, 248)");
    browserContext.grantPermissions(java.util.List.of("clipboard-read", "clipboard-write"));
    firstCode.locator("vaadin-button").click();

    assertThat(
            browserPage
                .locator("vaadin-notification-card:not([closing])")
                .filter(new Locator.FilterOptions().setHasText(uiTexts.getTwoFactorCodeCopied())))
        .isVisible();
    org.junit.jupiter.api.Assertions.assertEquals(
        codes.getFirst(), browserPage.evaluate("navigator.clipboard.readText()"));
    TwoFactorPage.assertCopyFailure(
        browserPage, firstCode.locator("vaadin-button"), uiTexts.getTwoFactorCopyFailed());
    org.junit.jupiter.api.Assertions.assertEquals(
        codes, TwoFactorPage.recoveryCodeValues(browserPage));
    browserPage.locator("#two-factor-recovery-toggle").click();

    assertThat(browserPage.locator("#two-factor-recovery-codes")).isHidden();
    assertThat(browserPage.locator("#two-factor-copy-codes")).isEnabled();
    assertThat(browserPage.locator("#two-factor-download-pdf")).isEnabled();
    browserPage.locator("#two-factor-codes-saved input").setChecked(true);
    new UserMenu(browserPage).selectLanguage(other.language());

    assertThat(browserPage.locator("#two-factor-recovery-codes")).isHidden();
    assertThat(browserPage.locator("#two-factor-recovery-details"))
        .not()
        .hasAttribute("opened", "");
    browserPage.locator("#two-factor-recovery-toggle").click();
    org.junit.jupiter.api.Assertions.assertEquals(
        codes, TwoFactorPage.recoveryCodeValues(browserPage));
    for (int index = 1; index <= 10; index++) {
      assertThat(browserPage.locator("#two-factor-recovery-code-" + index))
          .hasJSProperty("value", "");
    }
    assertThat(browserPage.locator("#two-factor-download-pdf"))
        .hasText(other.getTwoFactorDownloadPdf());
    assertThat(browserPage.locator("#two-factor-codes-saved input")).isChecked();
    new UserMenu(browserPage).selectTestLanguage();
    org.junit.jupiter.api.Assertions.assertEquals(
        codes, TwoFactorPage.recoveryCodeValues(browserPage));
    browserPage.locator("#two-factor-submit").click();

    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("MFA-15: Superadmin-Entzug entfernt bereits geöffnete 2FA- und Anbieterformulare")
  void withdrawingSuperadminProtectsBothOpenConfigurations(int width) {
    signInAdministrator();
    AdminUsersPage users = new AdminUsersPage(browserPage);
    String email = "role-mfa-" + UUID.randomUUID() + "@example.test";
    users.createUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    AdminUserDialog form = users.openEdit(email);
    form.setRole(uiTexts.getRoleSuperadmin(), true);
    form.save();
    try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
      browserActor.browserPage.setViewportSize(width, 900);
      browserActor.loginAdmin(email, BrowserTestAccounts.USER_PASSWORD);
      browserActor.browserPage.navigate("/admin?section=configuration&group=two_factor");

      assertThat(browserActor.browserPage.locator("#two-factor-save")).isVisible();
      browserActor
          .browserPage
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getTwoFactorInactivityEnabled())
                  .setExact(true))
          .setChecked(true);
      try (Page provider = browserActor.browserContext.newPage()) {
        provider.setViewportSize(width, 900);
        provider.navigate("/admin?section=configuration&group=external_login");

        assertThat(provider.locator("#provider-google-save")).isVisible();
        String originalId = provider.locator("#provider-google-client-id input").inputValue();
        provider.locator("#provider-google-client-id input").fill("unsaved-own-test-draft");
        form = users.openEdit(email);
        form.setRole(uiTexts.getRoleSuperadmin(), false);
        form.save();
        // Der bestehende Login und die normale Adminrolle bleiben erhalten. Die live
        // zurückgezogenen Sonderrechte müssen beide alten Formulare verwerfen.
        assertThat(browserActor.browserPage.locator("#two-factor-save")).hasCount(0);
        assertThat(provider.locator("#provider-google-save")).hasCount(0);
        browserActor.browserPage.navigate("/admin?section=configuration&group=two_factor");

        assertThat(browserActor.browserPage.locator("#admin-two-factor-settings")).hasCount(0);
        provider.navigate("/admin?section=configuration&group=external_login");

        assertThat(provider.locator("#admin-provider-settings")).hasCount(0);
        AllurePlaywright.attachScreenshot(
            "Zurückgezogene Superadminrechte in offener Ansicht", browserActor.browserPage);
        openConfiguration();

        assertThat(browserPage.locator("#two-factor-inactivity-enabled"))
            .hasJSProperty("checked", false);
        browserPage.navigate("/admin?section=configuration&group=external_login");

        assertThat(browserPage.locator("#provider-google-client-id input")).hasValue(originalId);
      }
      new UserMenu(browserActor.browserPage).signOut();
    }
  }

  @Test
  @DisplayName(
      "MFA-16: Startfreigabe allein schwächt nichts; gespeicherte Entwicklungsoptionen sind ohne sie unwirksam")
  void developmentOptionsAreExplicitAndIgnoredAfterRestartWithoutTheGate() throws Exception {
    signInAdministrator();
    try {
      BrowserTestRunner.restartWithDevelopmentOptions(true);
      openConfiguration();
      for (int width : new int[] {1280, 390}) {
        browserPage.setViewportSize(width, 900);

        assertThat(
                browserPage.getByText(
                    uiTexts.getTwoFactorDevelopmentWarning(),
                    new Page.GetByTextOptions().setExact(true)))
            .isVisible();
        assertThat(browserPage.locator("#two-factor-development-pause"))
            .hasJSProperty("checked", false);
        assertThat(browserPage.locator("#two-factor-development-google"))
            .hasJSProperty("checked", false);
      }
      assertFreshLoginNeedsFactor();
      browserPage
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getTwoFactorDevelopmentGoogle())
                  .setExact(true))
          .setChecked(true);
      saveConfiguration();
      assertFreshLoginNeedsFactor();
      browserPage
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getTwoFactorDevelopmentPause())
                  .setExact(true))
          .setChecked(true);
      saveConfiguration();
      try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
        LoginPage login = new LoginPage(browserActor.browserPage);
        login.open();
        login.signInFirstFactor(
            BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);

        assertThat(browserActor.browserPage).hasURL(Pattern.compile(".*/admin$"));
        assertThat(browserActor.browserPage.locator("#two-factor-code")).hasCount(0);
        new UserMenu(browserActor.browserPage).selectTestLanguage();
        new UserMenu(browserActor.browserPage).signOut();
      }
    } finally {
      BrowserTestRunner.restartWithDevelopmentOptions(false);
    }
    openConfiguration();

    assertThat(
            browserPage.getByText(
                uiTexts.getTwoFactorDevelopmentTitle(), new Page.GetByTextOptions().setExact(true)))
        .isHidden();
    assertFreshLoginNeedsFactor();
  }

  private void signInAdministrator() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
  }

  private void openConfiguration() {
    browserPage.navigate("/admin?section=configuration&group=two_factor");
    TwoFactorPage.completeIfRequired(
        browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    browserPage.navigate("/admin?section=configuration&group=two_factor");
    new UserMenu(browserPage).selectTestLanguage();

    assertThat(browserPage.locator("#two-factor-save")).isVisible();
  }

  private void saveConfiguration() {
    browserPage.locator("#two-factor-save").click();
    TwoFactorPage.enterActionCode(
        browserPage, TwoFactorPage.nextCode(browserPage, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(
        browserPage, () -> browserPage.locator("#two-factor-action-confirm").click());

    assertThat(browserPage.locator("#two-factor-action-confirm")).isHidden();
    browserPage.waitForCondition(() ->
        java.net.URI.create(browserPage.url()).getPath().equals("/two-factor")
            || browserPage
                .locator("vaadin-notification-card:not([closing])")
                .filter(new Locator.FilterOptions().setHasText(uiTexts.getTwoFactorSaved()))
                .isVisible());
    // Die alte Ansicht kann noch zur erneuten 2FA-Bestätigung weiterleiten.
    // Den gespeicherten Stand ohne konkurrierende Navigation in derselben Sitzung öffnen.
    Page previous = browserPage;
    browserPage = browserContext.newPage();
    previous.close();
    openConfiguration();
  }

  private void assertFreshLoginNeedsFactor() {
    try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
      LoginPage login = new LoginPage(browserActor.browserPage);
      login.open();
      login.signInFirstFactor(
          BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);

      assertThat(browserActor.browserPage).hasURL(Pattern.compile(".*/two-factor.*"));
      assertThat(browserActor.browserPage.locator("#two-factor-code")).isVisible();
      browserActor.browserPage.locator("#two-factor-cancel").click();
      new LoginPage(browserActor.browserPage).assertFormVisible();
    }
  }

  private void providerIdentity(String email) {
    assertThat(browserPage.locator("#fixture-email")).isVisible();
    browserPage.locator("#fixture-email").fill(email);
    browserPage.locator("#fixture-mode").selectOption("valid");
    browserPage.locator("#fixture-signin").click();
  }
}
