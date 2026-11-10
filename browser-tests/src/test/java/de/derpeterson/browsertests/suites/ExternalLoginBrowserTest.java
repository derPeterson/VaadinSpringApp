package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AccountSessionsPage;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Externe Anmeldung")
public class ExternalLoginBrowserTest extends BrowserFixture {
  @TempDir Path profile;

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("follow-up-regression")
  @Tag("sign-in-session-layout")
  @DisplayName("OIDC-14: Benutzerdetails und aktive Sitzungen unterscheiden Anmeldewege")
  @Description(
      "Ein administrativ angelegtes Konto verbindet Google. Zwei aktive Sitzungen zeigen jeweils ihren tatsächlichen Anmeldeweg. Der Bearbeitungsdialog zeigt Herkunft und verfügbare Wege schreibgeschützt.")
  void signInDetailsDistinguishAccountAndSessionMethods(int width) {
    browserPage.setViewportSize(width, 1000);
    String email = "session-example-" + UUID.randomUUID() + "@example.test";
    String google = "session-provider-" + UUID.randomUUID() + "@gmail.com";
    try (BrowserActor administrator = new BrowserActor(testBrowser);
        BrowserActor providerSession = new BrowserActor(testBrowser)) {
      administrator.browserPage.setViewportSize(width, 1000);
      AdminUsersPage users =
          administrator.loginAdmin(
              BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
      LoginPage login = new LoginPage(browserPage);
      login.open();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(browserPage).selectTestLanguage();
      browserPage.navigate("/account?section=security&tab=providers");
      browserPage.locator("#external-connect-google").click();
      browserPage
          .locator("#external-confirm-password input")
          .fill(BrowserTestAccounts.USER_PASSWORD);
      browserPage.locator("#external-confirm-local").click();
      providerIdentity(google, "valid");
      assertThat(browserPage.locator("#external-connect-google"))
          .hasText(uiTexts.getExternalDisconnect("Google"));

      begin(providerSession.browserPage, "google", google, "valid", true);
      assertThat(providerSession.browserPage).hasURL(Pattern.compile(".*/home$"));
      AccountSessionsPage sessions = new AccountSessionsPage(browserPage);
      sessions.open();
      sessions.assertCounts(2, 1, 0);
      assertThat(sessions.rows("current").locator(".account-session-sign-in-method"))
          .hasText(uiTexts.getSessionSignedInVia(uiTexts.getSignInPortalPassword()));
      assertThat(sessions.rows("active").locator(".account-session-sign-in-method"))
          .hasText(uiTexts.getSessionSignedInVia(uiTexts.getExternalGoogle()));
      AllurePlaywright.attachScreenshot("Sitzungen über Portalpasswort und Google", browserPage);

      AdminUserDialog user = users.openEdit(email);
      Locator title = user.root().locator(".admin-user-sign-in-title");
      Locator registration = user.root().locator("vaadin-badge#admin-user-registered-via");
      Locator methods = user.root().locator("#admin-user-sign-in-methods");
      assertThat(title).hasText(uiTexts.getSignInDetailsTitle());
      assertThat(title).hasCSS("text-transform", "uppercase");
      assertThat(title).hasCSS("color", "rgb(56, 189, 248)");
      assertThat(registration).hasText(uiTexts.getSignInAccountCreated());
      Locator registrationLabel = user.root().locator(".admin-user-registration-label");
      assertThat(registrationLabel).hasText(uiTexts.getSignInRegisteredVia());
      assertThat(registrationLabel).hasCSS("text-transform", "none");
      assertThat(registrationLabel).hasCSS("color", "rgb(203, 213, 225)");
      assertThat(registration).hasCSS("color", "rgb(203, 213, 225)");
      assertThat(user.root().locator(".admin-user-methods-label"))
          .hasText(uiTexts.getSignInAvailableMethods());
      assertThat(methods.locator("vaadin-badge[theme~=success]"))
          .hasText(new String[] {uiTexts.getSignInPortalPassword(), uiTexts.getExternalGoogle()});
      assertThat(methods).hasCSS("border-top-width", "0px");
      assertThat(methods.locator("vaadin-badge").first()).hasCSS("border-top-width", "1px");
      assertThat(methods.locator("vaadin-badge").first()).hasCSS("border-top-left-radius", "4px");
      methods.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot(
          "Schreibgeschützte Anmeldeangaben", administrator.browserPage);
      user.cancel();
      assertSelfRegisteredSignInDetails(users);
      new UserMenu(administrator.browserPage).signOut();
    }
  }

  private void assertSelfRegisteredSignInDetails(AdminUsersPage users) {
    new UserMenu(browserPage).signOut();
    String email = "self-registered-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    browserPage.locator("#external-register-first input").fill("Browser");
    browserPage.locator("#external-register-last input").fill("Testuser");
    browserPage.locator("#external-register-submit").click();
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));

    AdminUserDialog user = users.openEdit(email);
    Locator panel = user.root().locator(".admin-user-sign-in");
    Locator title = panel.locator(".admin-user-sign-in-title");
    Locator badge = panel.locator("vaadin-badge#admin-user-registered-via");
    assertThat(title).hasCSS("font-size", "13px");
    assertThat(badge).hasText(uiTexts.getExternalGoogle());
    assertThat(badge).hasCSS("color", "rgb(56, 189, 248)");
    assertTrue(
        (Boolean)
            panel.evaluate(
                """
                panel => {
                  const [icon, content] = panel.children;
                  const title = content.firstElementChild.getBoundingClientRect();
                  const marker = icon.getBoundingClientRect();
                  const innerBottom = panel.getBoundingClientRect().bottom
                    - parseFloat(getComputedStyle(panel).paddingBottom)
                    - parseFloat(getComputedStyle(panel).borderBottomWidth);
                  return Math.abs((marker.top + marker.bottom - title.top - title.bottom) / 2) <= 2
                    && Math.abs(innerBottom - content.getBoundingClientRect().bottom) <= 2;
                }
                """),
        "Die Infobox muss ohne zusätzlichen Leerraum mit Icon auf Höhe des Titels erscheinen");
    user.cancel();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("follow-up-regression")
  @DisplayName("OIDC-15: Ersatzcode verbindet einen Anbieter erst nach der Bestätigung")
  @Description(
      "Der eigene Authenticator stellt einen gültigen Ersatzcode aus. Das Eingeben des Codes lässt den Dialog offen. Erst der Aktionsbutton führt zum Testanbieter und verbindet das Konto.")
  void recoveryCodeConnectsProviderOnlyAfterExplicitConfirmation(int width) {
    browserPage.setViewportSize(width, 900);
    String email = "recovery-link-" + UUID.randomUUID() + "@example.test";
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(browserPage, email);
    browserPage.locator("#account-security-tab-providers").click();

    browserPage.locator("#external-connect-google").click();
    String accountLocation = browserPage.url();
    browserPage.locator("#external-proof-recovery-code-tab").click();
    String recoveryCode = TwoFactorPage.nextCode(browserPage, email);
    Locator input = browserPage.locator("#external-confirm-recovery-code input");
    input.fill(recoveryCode);
    input.press("Tab");
    Locator confirm = browserPage.locator("#external-confirm-local");
    assertThat(confirm).isVisible();
    assertThat(input).hasValue(recoveryCode);
    assertThat(browserPage).hasURL(accountLocation);
    confirm.click();
    providerIdentity("recovery-link-" + UUID.randomUUID() + "@gmail.com", "valid");
    assertThat(browserPage.locator("#external-connect-google"))
        .hasText(uiTexts.getExternalDisconnect("Google"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("OIDC-13: Anmeldefehler nach direktem Rücksprung verwenden den Standardtoast")
  void loginFailureAfterRedirectUsesThePublicTheme(int width) {
    browserPage.setViewportSize(width, 900);
    browserPage.navigate("/login?error=unavailable");
    assertStandardNotification(browserPage, uiTexts.getLoginUnavailableMessage(), "error");
    new LoginPage(browserPage).assertFormVisible();
    AllurePlaywright.attachScreenshot("Anmeldefehler nach direktem Rücksprung", browserPage);
  }

  @ParameterizedTest
  @CsvSource({"1280, false", "390, false", "1280, true", "390, true"})
  @Tag("provider-settings")
  @DisplayName(
      "OIDC-12: Deaktivierte Anbieter erhalten Verbindungen und bieten nur nutzbare Bestätigungen")
  void disabledProvidersPreserveExistingAccountAccess(int width, boolean hasPassword) {
    browserPage.setViewportSize(width, 900);
    String email = "disabled-providers-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    browserPage.locator("#external-register-submit").click();
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    if (hasPassword) {
      enrollGoogle(email, "valid");
      browserPage.navigate("/account?section=security&tab=portal");
      browserPage.locator("#account-password-edit-security").click();
      confirmFactor(email);
      browserPage.locator("#account-password-new input").fill("OptionalPortal1!");
      browserPage.locator("#account-password-confirm input").fill("OptionalPortal1!");
      browserPage.locator("#account-password-save").click();
      assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
          .containsText(uiTexts.getAccountSecurityPasswordSuccess());
    }
    try (BrowserActor administrator = new BrowserActor(testBrowser)) {
      Page admin = administrator.browserPage;
      admin.navigate("/login");
      new UserMenu(admin).selectTestLanguage();
      LoginPage login = new LoginPage(admin);
      login.assertFormVisible();
      login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
      new UserMenu(admin).selectTestLanguage();
      admin.navigate("/admin?section=configuration&group=external_login");
      try {
        setProviderAvailability(admin, false);
        verifyDisabledConnection(hasPassword);
        browserPage.locator("#account-security-tab-portal").click();
        browserPage.locator("#account-password-edit-security").click();
        if (hasPassword) {
          confirmFactor(email);
          assertThat(browserPage.locator("#account-password-new")).isVisible();
          browserPage.locator("#account-password-cancel").click();
        } else {
          assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor\\?setup$"));
          assertThat(browserPage.locator("#two-factor-confirm-google")).hasCount(0);
          assertThat(browserPage.locator("#two-factor-password")).isHidden();
          assertThat(browserPage.locator("#two-factor-submit")).isHidden();
          browserPage.navigate("/account?section=security&tab=portal");
          assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor\\?setup$"));
          browserPage.locator("#two-factor-cancel").click();
          assertThat(browserPage.locator("#account-password-edit-security")).isVisible();
        }
        browserPage.locator("#account-email-edit-security").click();
        if (hasPassword) {
          confirmFactor(email);
          assertThat(browserPage.locator("#account-email-new")).isVisible();
          browserPage.locator("#account-email-cancel").click();
        } else {
          assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor\\?setup$"));
          assertThat(browserPage.locator("#two-factor-confirm-google")).hasCount(0);
          browserPage.locator("#two-factor-cancel").click();
          assertThat(browserPage.locator("#account-email-edit-security")).isVisible();
        }
        AllurePlaywright.attachScreenshot(
            "Kontoführung ohne verfügbare Anmeldeanbieter", browserPage);
      } finally {
        setProviderAvailability(admin, true);
        // Das Fenster erst nach der Logout-Weiterleitung schließen, sonst bleibt sein Platz belegt.
        new UserMenu(admin).signOut();
      }
    }
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-password-edit-security").click();
    if (hasPassword) {
      assertThat(browserPage.locator("#external-confirm-factor")).isVisible();
      browserPage.locator("#external-proof-cancel").click();
    } else {
      assertThat(browserPage.locator("#two-factor-confirm-google")).isVisible();
      browserPage.locator("#two-factor-cancel").click();
      assertThat(browserPage.locator("#account-password-edit-security")).isVisible();
    }
  }

  private void setProviderAvailability(Page admin, boolean enabled) {
    for (String provider : new String[] {"google", "microsoft"}) {
      admin.locator("#provider-tab-" + provider).click();
      admin
          .locator("#provider-" + provider + "-enabled")
          .getByRole(AriaRole.SWITCH)
          .setChecked(enabled);
      saveProviderSettings(admin, provider);
      assertThat(admin.locator("#provider-" + provider + "-status"))
          .hasText(
              enabled
                  ? uiTexts.getProviderSettingsActive()
                  : uiTexts.getProviderSettingsInactive());
    }
  }

  private void saveProviderSettings(Page admin, String provider) {
    UiActions.complete(admin, () -> admin.locator("#provider-" + provider + "-save").click());
    assertThat(admin.locator("#two-factor-action-factor")).isVisible();
    TwoFactorPage.enterActionCode(
        admin, TwoFactorPage.nextCode(admin, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(admin, () -> admin.locator("#two-factor-action-confirm").click());
    assertThat(admin.locator("#two-factor-action-confirm")).isHidden();
  }

  private void confirmFactor(String email) {
    String code = TwoFactorPage.nextCode(browserPage, email);
    if (code.length() > 6) {
      browserPage.locator("#external-proof-recovery-code-tab").click();
      browserPage.locator("#external-confirm-recovery-code input").fill(code);
    } else {
      browserPage.locator("#external-confirm-factor input").fill(code);
    }
    browserPage.locator("#external-confirm-local").click();
  }

  private void enrollGoogle(String email, String mode) {
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-confirm-google").click();
    assertThat(browserPage.locator("#fixture-authorize-parameters"))
        .hasAttribute("data-prompt", "select_account consent");
    assertThat(browserPage.locator("#fixture-authorize-parameters"))
        .hasAttribute("data-max-age", "");
    assertThat(browserPage.locator("#fixture-authorize-parameters"))
        .hasAttribute("data-claims", "");
    providerIdentity(email, mode);
    browserPage
        .locator("#two-factor-code input")
        .fill(new MailpitInbox(browserContext).awaitAuthenticatorSetupCode(email));
    browserPage.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(browserPage, email);
    assertThat(browserPage).hasURL(Pattern.compile(".*/account\\?section=security&tab=portal$"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-11: Lokales Konto verbindet Anbieter erst nach kontextbezogener Passwortbestätigung")
  void localAccountConnectsFromContextualDialog(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String localEmail = "context-local-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(localEmail, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).signOut();
    login.signIn(localEmail, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-security-tab-providers").click();
    assertThat(browserPage.locator("#external-security-action")).hasCount(0);
    assertThat(browserPage.locator(".external-connection-row")).hasCount(2);
    assertThat(browserPage.locator("#external-identity-dialog")).hasJSProperty("opened", false);
    AllurePlaywright.attachScreenshot(
        "Verbundene Anmeldungen: kompakte Anbieterzeilen", browserPage);
    browserPage.locator("#external-connect-google").click();
    assertThat(browserPage.locator("#external-confirm-password")).isVisible();
    assertThat(browserPage.locator("#external-confirm-google")).hasCount(0);
    browserPage.locator("#external-confirm-local").click();
    assertThat(browserPage.locator("#external-confirm-password")).hasAttribute("invalid", "");
    assertStandardNotification(browserPage, uiTexts.getBaseValidationRequiredMessage(), "error");
    assertThat(browserPage.locator("#external-confirm-password [part=error-message]")).isHidden();
    browserPage.locator("#external-confirm-password input").fill("wrong-password");
    browserPage.locator("#external-confirm-local").click();
    assertStandardNotification(browserPage, uiTexts.getAccountSecurityWrongPassword(), "error");
    assertThat(browserPage.locator("#external-confirm-password [part=error-message]")).isHidden();
    browserPage.locator("#external-confirm-password input").fill("unconfirmed-draft");
    browserPage.locator("#external-proof-cancel").click();
    browserPage.locator("#external-connect-google").click();
    assertThat(browserPage.locator("#external-confirm-password input")).hasValue("");
    browserPage.locator("#external-confirm-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    AllurePlaywright.attachScreenshot(
        "Kontextbezogene Bestätigung für Google verbinden", browserPage);
    browserPage.locator("#external-confirm-local").click();
    String google = "context-" + UUID.randomUUID() + "@gmail.com";
    providerIdentity(google, "valid");
    assertThat(browserPage.locator("#external-connect-google"))
        .hasText(uiTexts.getExternalDisconnect("Google"));
    browserPage.locator("#external-connect-google").click();
    // Ein neues Google-OIDC allein ersetzt keinen unabhängigen Aktionsnachweis.
    assertThat(browserPage.locator("#external-confirm-google")).hasCount(0);
    browserPage.locator("#external-confirm-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#external-confirm-local").click();
    assertThat(browserPage.locator("#external-connect-google"))
        .hasText(uiTexts.getExternalConnect("Google"));
    assertStandardNotification(browserPage, uiTexts.getExternalDisconnectedHint(), "success");
    browserPage.locator("#external-connect-google").click();
    browserPage.locator("#external-confirm-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#external-confirm-local").click();
    providerIdentity(google, "valid");
    assertThat(browserPage.locator("#external-connect-google"))
        .hasText(uiTexts.getExternalDisconnect("Google"));
    new UserMenu(browserPage).signOut();
    begin("google", google, "valid");
    browserPage.navigate("/account?section=security&tab=providers");
    browserPage.locator("#external-connect-google").click();
    browserPage.locator("#external-confirm-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#external-confirm-local").click();
    assertThat(browserPage).hasURL(Pattern.compile(".*/login\\?provider_disconnected$"));
    assertStandardNotification(browserPage, uiTexts.getExternalDisconnectedSignIn(), "success");
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-08: Superadmins bestätigen Anbieteränderungen mit 2FA und Geheimnisse bleiben serverseitig")
  @Tag("provider-settings")
  void administrativeProviderSettings(int width) throws Exception {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/admin?section=configuration&group=external_login");
    assertThat(browserPage.locator("#admin-provider-settings")).isVisible();
    AllurePlaywright.attachScreenshot("Anbietereinrichtung mit Google und Microsoft", browserPage);
    for (String provider : new String[] {"google", "microsoft"}) {
      browserPage.locator("#provider-tab-" + provider).click();
      com.microsoft.playwright.Locator enabled =
          browserPage.locator("#provider-" + provider + "-enabled").getByRole(AriaRole.SWITCH);
      com.microsoft.playwright.Locator secret =
          browserPage.locator("#provider-" + provider + "-secret input");
      assertThat(secret).hasValue("");
      String originalId =
          browserPage.locator("#provider-" + provider + "-client-id input").inputValue();
      browserPage.locator("#provider-" + provider + "-client-id input").fill("");
      saveProviderSettings(browserPage, provider);
      assertStandardNotification(browserPage, uiTexts.getProviderSettingsIncomplete(), "error");
      browserPage.locator("#provider-" + provider + "-client-id input").fill(originalId);
      assertThat(browserPage.locator("#provider-" + provider + "-redirect input"))
          .hasValue(System.getProperty("browser.tests.baseUrl") + "/login/oauth2/code/" + provider);
      enabled.setChecked(false);
      saveProviderSettings(browserPage, provider);
      assertStandardNotification(browserPage, uiTexts.getProviderSettingsSaved(), "success");
      assertThat(browserPage.locator("#provider-" + provider + "-status"))
          .hasText(uiTexts.getProviderSettingsInactive());
      try (com.microsoft.playwright.BrowserContext visitor =
          testBrowser.newContext(
              new com.microsoft.playwright.Browser.NewContextOptions()
                  .setBaseURL(System.getProperty("browser.tests.baseUrl")))) {
        Page anonymous = visitor.newPage();
        anonymous.navigate("/login");
        assertThat(anonymous.locator("#external-login-" + provider)).hasCount(0);
        assertThat(anonymous.locator("vaadin-password-field")).isVisible();
      } finally {
        // Neu eingegebene Geheimnisse werden tatsächlich verwendet, aber nie zurückgeladen.
        secret.fill(BrowserTestAccounts.OIDC_CLIENT_SECRET);
        browserPage.locator("#provider-" + provider + "-client-id input").fill(originalId);
        enabled.setChecked(true);
        saveProviderSettings(browserPage, provider);
        assertThat(browserPage.locator("#provider-" + provider + "-status"))
            .hasText(uiTexts.getProviderSettingsActive());
      }
      browserPage.reload();
      browserPage.locator("#provider-tab-" + provider).click();
      assertThat(secret).hasValue("");
      assertThat(browserPage.locator("#provider-" + provider + "-status"))
          .hasText(uiTexts.getProviderSettingsActive());
    }
    if (width == 1280) {
      verifyRestartWithBothProvidersDisabled();
    }
    AllurePlaywright.attachScreenshot("Anmeldeanbieter im bestehenden Referenzstyle", browserPage);
    // Der anschließende echte Code-/Tokenaustausch mit dem eigenen Anbieter prüft die Übernahme.
    new UserMenu(browserPage).signOut();
    for (String provider : new String[] {"google", "microsoft"}) {
      String providerEmail = "admin-settings-" + UUID.randomUUID() + "@gmail.com";
      begin(provider, providerEmail, "valid");
      browserPage.locator("#external-register-first input").fill("Browser");
      browserPage.locator("#external-register-last input").fill("Testuser");
      browserPage.locator("#external-register-submit").click();
      if (provider.equals("microsoft")) {
        // Der Testanbieter bestätigt diese Kontaktadresse nicht verbindlich.
        // Microsoft-Konten durchlaufen deshalb weiterhin die lokale Mailverifikation.
        MailpitInbox inbox = new MailpitInbox(browserContext);
        try (Page activation =
            inbox.openVerificationLink(inbox.awaitVerificationMail(providerEmail))) {
          new de.derpeterson.browsertests.pages.VerificationPage(activation).assertActivated();
        }
        begin(provider, providerEmail, "valid");
      }
      assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
      new UserMenu(browserPage).selectTestLanguage();
      new UserMenu(browserPage).requestSignOut();
      // Die Startseite ist auch anonym erreichbar. Der geschützte Kontoaufruf
      // muss nach der echten Abmeldung wieder zur Anmeldung führen.
      browserPage.navigate("/account");
      new LoginPage(browserPage).assertFormVisible();
    }
  }

  private void verifyRestartWithBothProvidersDisabled() throws Exception {
    for (String provider : new String[] {"google", "microsoft"}) {
      browserPage.locator("#provider-tab-" + provider).click();
      browserPage
          .locator("#provider-" + provider + "-enabled")
          .getByRole(AriaRole.SWITCH)
          .uncheck();
      saveProviderSettings(browserPage, provider);
      assertThat(browserPage.locator("#provider-" + provider + "-status"))
          .hasText(uiTexts.getProviderSettingsInactive());
    }
    // Auch mit zwei deaktivierten Anbietern muss der Server ohne OAuth-Neukonfiguration starten.
    browserPage.navigate("about:blank");
    de.derpeterson.browsertests.BrowserTestRunner.restartTestApplication();
    browserPage.navigate("/admin?section=configuration&group=external_login");
    for (String provider : new String[] {"google", "microsoft"}) {
      browserPage.locator("#provider-tab-" + provider).click();
      assertThat(browserPage.locator("#provider-" + provider + "-status"))
          .hasText(uiTexts.getProviderSettingsInactive());
      assertThat(browserPage.locator("#provider-" + provider + "-secret input")).hasValue("");
      browserPage.locator("#provider-" + provider + "-enabled").getByRole(AriaRole.SWITCH).check();
      saveProviderSettings(browserPage, provider);
      assertThat(browserPage.locator("#provider-" + provider + "-status"))
          .hasText(uiTexts.getProviderSettingsActive());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("PROFILE-01: Adminanlage und Bearbeitung erhalten leere freiwillige Profilangaben")
  void administrativeOptionalProfile(int width) {
    browserPage.setViewportSize(width, 900);
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.open();
    AdminUserDialog form = users.openCreate();
    String email = "optional-admin-" + UUID.randomUUID() + "@example.test";
    form.fillNewUser(email, BrowserTestAccounts.USER_PASSWORD, true, false);
    form.root().locator("vaadin-combo-box [part~='clear-button']").click();
    form.root().locator("vaadin-date-picker input").fill("");
    form.firstName().click();
    form.save();
    form = users.openEdit(email);
    assertThat(form.root().locator("vaadin-combo-box input")).hasValue("");
    assertThat(form.root().locator("vaadin-date-picker input")).hasValue("");
    form.firstName().fill("Optional");
    form.save();
    form = users.openEdit(email);
    assertThat(form.firstName()).hasValue("Optional");
    assertThat(form.root().locator("vaadin-combo-box input")).hasValue("");
    assertThat(form.root().locator("vaadin-date-picker input")).hasValue("");
    form.cancel();
  }

  private void verifyDisabledConnection(boolean hasPassword) {
    // Ein direkter Seitenaufruf muss die bestehenden Zuordnungen weiter anzeigen.
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-security-tab-providers").click();
    assertThat(browserPage.locator(".external-connection-row")).hasCount(1);
    assertThat(browserPage.locator(".external-connection-heading vaadin-badge"))
        .hasText(uiTexts.getExternalConnected());
    assertThat(browserPage.locator(".external-connection-row"))
        .containsText(uiTexts.getExternalProviderUnavailable());
    Locator disconnect = browserPage.locator("#external-connect-google");
    if (hasPassword) {
      assertThat(disconnect).isEnabled();
      disconnect.click();
      assertThat(browserPage.locator("#external-confirm-factor")).isVisible();
      assertThat(browserPage.locator("#external-confirm-password")).isHidden();
      assertThat(browserPage.locator("#external-confirm-google")).hasCount(0);
      browserPage.locator("#external-proof-cancel").click();
    } else {
      assertThat(disconnect).isDisabled();
    }
    AllurePlaywright.attachScreenshot(
        "Deaktivierter Anbieter bleibt mit seinem Konto verbunden", browserPage);
  }

  private void begin(String provider, String email, String mode) {
    begin(browserPage, provider, email, mode, false);
  }

  private void begin(Page target, String provider, String email, String mode, boolean remember) {
    begin(target, provider, email, mode, remember, uiTexts);
  }

  private void begin(
      Page target,
      String provider,
      String email,
      String mode,
      boolean remember,
      UiTexts loginTexts) {
    target.navigate("/login");
    target
        .getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(loginTexts.getLoginRememberMeCheckbox())
                .setExact(true))
        .setChecked(remember);
    assertThat(target.locator("#external-login-" + provider + " img")).isVisible();
    target.waitForCondition(() ->
        (Boolean)
            target
                .locator("#external-login-" + provider + " img")
                .evaluate("image => image.complete && image.naturalWidth > 0"));
    target.locator("#external-login-" + provider).click();
    providerIdentity(target, email, mode);
  }

  private void providerIdentity(String email, String mode) {
    providerIdentity(browserPage, email, mode);
  }

  private void providerIdentity(Page target, String email, String mode) {
    assertThat(
            target.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Own OIDC test provider").setExact(true)))
        .isVisible();
    target.locator("#fixture-email").fill(email);
    target.locator("#fixture-mode").selectOption(mode);
    target.locator("#fixture-signin").click();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-04: Verknüpfen und Trennen brauchen einen Aktionsnachweis und erhalten die letzte Anmeldung")
  void connectAndDisconnectWithProviderConfirmation(int width) {
    browserPage.setViewportSize(width, 900);
    String google = "link-" + UUID.randomUUID() + "@gmail.com";
    String microsoft = "link-" + UUID.randomUUID() + "@example.test";
    begin("microsoft", microsoft, "valid");
    browserPage.locator("#external-register-first input").fill("Browser");
    browserPage.locator("#external-register-last input").fill("Testuser");
    browserPage.locator("#external-register-submit").click();
    MailpitInbox inbox = new MailpitInbox(browserContext);
    try (Page activation = inbox.openVerificationLink(inbox.awaitVerificationMail(microsoft))) {
      new de.derpeterson.browsertests.pages.VerificationPage(activation).assertActivated();
    }
    begin("microsoft", microsoft, "valid");
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-security-tab-providers").click();
    browserPage.locator("#external-connect-google").click();
    browserPage.locator("#external-confirm-microsoft").click();
    providerIdentity(microsoft, "valid");
    assertThat(browserPage.locator("#external-security-result"))
        .hasText(uiTexts.getExternalProofReady());
    browserPage.locator("#external-confirm-local").click();
    providerIdentity(google, "valid");
    assertThat(browserPage.locator("#external-connect-google"))
        .hasText(uiTexts.getExternalDisconnect("Google"));
    browserPage.locator("#external-connect-google").click();
    browserPage.locator("#external-confirm-microsoft").click();
    providerIdentity(microsoft, "valid");
    browserPage.locator("#external-confirm-local").click();
    assertThat(browserPage.locator("#external-connect-google"))
        .hasText(uiTexts.getExternalConnect("Google"));
    assertStandardNotification(browserPage, uiTexts.getExternalDisconnectedHint(), "success");
    assertThat(browserPage.locator("#external-connect-microsoft")).isDisabled();
    browserPage.locator("#account-security-tab-portal").click();
    browserPage.locator("#account-password-edit-security").click();
    assertThat(browserPage.locator("#external-confirm-microsoft")).isVisible();
    assertProviderProofIcon("microsoft");
    rejectedPasswordProof(
        "microsoft", microsoft, "missing-auth-time", uiTexts.getExternalProofFreshRequired());
    rejectedPasswordProof("microsoft", microsoft, "stale", uiTexts.getExternalProofFreshRequired());
    AllurePlaywright.attachScreenshot("Letzte externe Anmeldung bleibt geschützt", browserPage);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-01: Google registriert ohne Portalpasswort, erhält die gewählte Sprache und meldet erneut an")
  void googleRegistrationAndReturn(int width) {
    browserPage.setViewportSize(width, 900);
    String email = "google-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    assertThat(browserPage).hasURL(Pattern.compile(".*/external-registration$"));
    assertThat(browserPage.locator("vaadin-password-field")).hasCount(0);
    assertThat(browserPage.locator("#external-register-first input")).hasValue("External");
    String registrationLanguage = uiTexts.language().equals("de") ? "en" : "de";
    new UserMenu(browserPage).selectLanguage(registrationLanguage);
    AllurePlaywright.attachScreenshot("Ergänzende Registrierung ohne Portalpasswort", browserPage);
    browserPage.locator("#external-register-submit").click();
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    browserPage.navigate("/account?tab=profile");
    Locale selectedLanguage = Locale.forLanguageTag(registrationLanguage);
    assertThat(browserPage.locator("#account-language input"))
        .hasValue(selectedLanguage.getDisplayLanguage(selectedLanguage));
    browserPage.reload();
    assertThat(browserPage.locator("#account-language input"))
        .hasValue(selectedLanguage.getDisplayLanguage(selectedLanguage));
    new UserMenu(browserPage).selectTestLanguage();
    assertThat(browserPage.locator("#account-birth-date input")).hasValue("");
    assertThat(browserPage.locator("#account-gender input")).hasValue("");
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-security-tab-providers").click();
    assertThat(browserPage.locator("#external-connect-google")).isDisabled();
    assertThat(browserPage.locator("#account-external-connections")).isVisible();
    AllurePlaywright.attachScreenshot("Externes Konto ohne erfundene Profilwerte", browserPage);
    new UserMenu(browserPage).requestSignOut();
    begin(browserPage, "google", email, "valid", false, new UiTexts(registrationLanguage));
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    assertTrue(
        (Boolean) browserPage.evaluate("document.documentElement.scrollWidth <= innerWidth"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-02: Microsoft erfordert eigenen E-Mail-Nachweis und keine zusätzliche Passwortregistrierung")
  void microsoftRequiresContactVerification(int width) {
    browserPage.setViewportSize(width, 900);
    String email = "microsoft-" + UUID.randomUUID() + "@example.test";
    begin("microsoft", email, "valid");
    assertThat(browserPage).hasURL(Pattern.compile(".*/external-registration$"));
    browserPage.locator("#external-register-first input").fill("Browser");
    browserPage.locator("#external-register-last input").fill("Testuser");
    browserPage.locator("#external-register-submit").click();
    assertThat(browserPage.locator(".public-panel")).containsText(uiTexts.getExternalVerifyEmail());
    MailpitInbox inbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail mail = inbox.awaitVerificationMail(email);
    assertThat(browserPage.locator(".public-header")).isVisible();
    AllurePlaywright.attachScreenshot(
        "Microsoft: Kontaktbestätigung vor Portalzugang", browserPage);
    try (Page activation = inbox.openVerificationLink(mail)) {
      new de.derpeterson.browsertests.pages.VerificationPage(activation).assertActivated();
    }
    begin("microsoft", email, "valid");
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
  }

  @ParameterizedTest
  @CsvSource({"1280, missing-auth-time", "390, missing-auth-time", "1280, stale", "390, stale"})
  @DisplayName(
      "OIDC-05: Google-Ersteinrichtung braucht Kontaktcode und TOTP vor optionalem Portalpasswort")
  void optionalPortalPassword(int width, String confirmationMode) {
    browserPage.setViewportSize(width, 900);
    String email = "password-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    browserPage.locator("#external-register-submit").click();
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    if (width == 1280 && confirmationMode.equals("missing-auth-time")) {
      rejectedEnrollment(email, "cancel", uiTexts.getExternalError());
      rejectedEnrollment(
          "different-" + UUID.randomUUID() + "@gmail.com",
          "valid",
          uiTexts.getExternalProofIdentityMismatch());
      rejectedEnrollment(email, "nonce", uiTexts.getExternalError());
      rejectedEnrollment(email, "audience", uiTexts.getExternalError());
    }
    enrollGoogle(email, confirmationMode);
    browserPage.locator("#account-password-edit-security").click();
    assertThat(browserPage.locator("#external-confirm-google")).hasCount(0);
    confirmFactor(email);
    assertThat(browserPage.locator("#account-password-new")).isVisible();
    assertThat(browserPage.locator("#account-password-current")).isHidden();
    browserPage.locator("#account-password-new input").fill("OptionalPortal1!");
    browserPage.locator("#account-password-confirm input").fill("OptionalPortal1!");
    browserPage.locator("#account-password-save").click();
    assertStandardNotification(browserPage, uiTexts.getAccountSecurityPasswordSuccess(), "success");
    browserPage.locator("#account-security-tab-providers").click();
    assertThat(browserPage.locator("#external-connect-google")).isEnabled();
    browserPage.locator("#account-security-tab-portal").click();
    browserPage.locator("#account-password-edit-security").click();
    assertThat(browserPage.locator("#external-confirm-factor")).isVisible();
    browserPage.locator("#external-proof-cancel").click();
    browserPage.locator("#account-email-edit-security").click();
    confirmFactor(email);
    assertThat(browserPage.locator("#account-email-password")).isHidden();
    browserPage.locator("#account-email-cancel").click();
    new UserMenu(browserPage).requestSignOut();
    assertThat(browserPage.locator(".public-header")).isVisible();
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(email, "OptionalPortal1!");
    assertThat(browserPage.locator("vaadin-button.tracked-user-button")).isVisible();
  }

  private void rejectedEnrollment(String email, String mode, String message) {
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-two-factor-setup").click();
    browserPage.locator("#two-factor-confirm-google").click();
    providerIdentity(email, mode);
    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor\\?setup&external_error=.*"));
    assertStandardNotification(browserPage, message, "error");
    assertThat(browserPage.locator("#two-factor-qr")).hasCount(0);
    assertThat(browserPage.locator("#two-factor-recovery-codes")).hasCount(0);
    assertThat(browserPage.locator("#two-factor-confirm-google")).isVisible();
    browserPage.locator("#two-factor-cancel").click();
    assertThat(browserPage.locator("#account-two-factor-setup")).isVisible();
  }

  private void rejectedPasswordProof(String provider, String email, String mode, String message) {
    browserPage.locator("#external-confirm-" + provider).click();
    if (provider.equals("microsoft")) {
      assertThat(browserPage.locator("#fixture-authorize-parameters"))
          .hasAttribute("data-prompt", "login");
      assertThat(browserPage.locator("#fixture-authorize-parameters"))
          .hasAttribute("data-max-age", "0");
      assertThat(browserPage.locator("#fixture-authorize-parameters"))
          .hasAttribute("data-claims", "{\"id_token\":{\"auth_time\":{\"essential\":true}}}");
    }
    providerIdentity(email, mode);
    assertThat(browserPage)
        .hasURL(Pattern.compile(".*/account\\?section=security&tab=portal&external_error=.*"));
    assertStandardNotification(browserPage, message, "error");
    AllurePlaywright.attachScreenshot(
        "Abgelehnter Anbieternachweis im Standarddesign: " + mode, browserPage);
    assertThat(browserPage.locator("#account-password-new")).isHidden();
    assertThat(browserPage.locator("#external-register-submit")).hasCount(0);
    browserPage.locator("#account-password-edit-security").click();
    assertThat(browserPage.locator("#external-confirm-local")).isDisabled();
    assertProviderProofIcon(provider);
  }

  private void assertStandardNotification(Page target, String message, String severity) {
    Locator notice =
        target
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(message));
    assertThat(notice).isVisible();
    assertThat(notice)
        .hasAttribute("theme", Pattern.compile(".*cosmos-reference-" + severity + ".*"));
    Locator surface = notice.locator("[part=overlay]");
    assertThat(surface).hasCSS("background-color", "rgb(14, 23, 38)");
    assertThat(surface).hasCSS("border-left-width", "3px");
    String accent =
        switch (severity) {
          case "success" -> "rgb(52, 211, 153)";
          case "warning" -> "rgb(251, 191, 36)";
          default -> "rgb(248, 113, 113)";
        };
    assertThat(surface).hasCSS("border-left-color", accent);
    Locator heading = notice.locator(".cosmos-toast-title");
    if (heading.count() > 0) {
      assertThat(heading).hasCSS("font-size", "12px");
      assertThat(heading).hasCSS("font-weight", "600");
      assertThat(heading).hasCSS("color", "rgb(226, 232, 240)");
    }
    Locator body = notice.locator(".cosmos-notification-message");
    if (body.count() > 0) {
      assertThat(body).hasCSS("font-size", "12px");
      assertThat(body).hasCSS("color", "rgb(148, 163, 184)");
    }
    assertThat(notice.locator("vaadin-button")).hasCSS("width", "36px");
    assertTrue((Boolean) target.evaluate("document.documentElement.scrollWidth <= innerWidth"));
  }

  private void assertProviderProofIcon(String provider) {
    Locator button = browserPage.locator("#external-confirm-" + provider);
    Locator image = button.locator("img");
    assertThat(image).hasCSS("width", "16px");
    assertThat(image).hasCSS("height", "16px");
    assertThat(button).hasCSS("height", "36px");
    AllurePlaywright.attachScreenshot("Bestätigungsaktion für " + provider, browserPage);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @DisplayName(
      "OIDC-06: Externe Anmeldung bleibt nur mit bewusster Auswahl nach Browserneustart erhalten")
  void externalRememberChoiceSurvivesOnlyWhenRequested(boolean remember) {
    String email = "remember-" + UUID.randomUUID() + "@gmail.com";
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwrightRuntime, profile)) {
      begin(actor.browserPage, "google", email, "valid", remember);
      actor.browserPage.locator("#external-register-submit").click();
      assertThat(actor.browserPage).hasURL(Pattern.compile(".*/home$"));
      assertEquals(
          remember,
          actor.browserContext.cookies().stream()
              .anyMatch(cookie -> cookie.name.equals("remember-me") && cookie.expires > 0));
    }
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwrightRuntime, profile)) {
      if (remember) {
        new UserMenu(actor.browserPage).assertUserSignedInWithoutAdminAction(email);
        new UserMenu(actor.browserPage).requestSignOut();
      } else {
        actor.browserPage.navigate("/account");
        new LoginPage(actor.browserPage).assertFormVisible();
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-07: Passwortloses Konto ändert Kontaktadresse mit eigenem Faktor und eigenem Einmallink")
  void passwordlessContactChangeKeepsTheStableProviderIdentity(int width) {
    browserPage.setViewportSize(width, 900);
    String providerEmail = "contact-" + UUID.randomUUID() + "@gmail.com";
    String contact = "changed-" + UUID.randomUUID() + "@example.test";
    begin("google", providerEmail, "valid");
    browserPage.locator("#external-register-submit").click();
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    enrollGoogle(providerEmail, "valid");
    browserPage.navigate("/account?section=security&tab=portal");
    browserPage.locator("#account-email-edit-security").click();
    confirmFactor(providerEmail);
    assertThat(browserPage.locator("#account-email-password")).isHidden();
    browserPage.locator("#account-email-new input").fill(contact);
    browserPage.locator("#account-email-confirm input").fill(contact);
    UiActions.complete(browserPage, () -> browserPage.locator("#account-email-request").click());
    assertThat(browserPage.locator("vaadin-notification-card"))
        .containsText(uiTexts.getAccountSecurityEmailRequested());
    MailpitInbox inbox = new MailpitInbox(browserContext);
    try (Page confirmation =
        inbox.openEmailChangeAction(
            inbox.awaitSecurityMail(contact, uiTexts.getAccountEmailChangeSubject()))) {
      UiActions.complete(
          confirmation, () -> confirmation.locator("#account-email-confirm-action").click());
      assertThat(confirmation.locator("#account-email-confirmation"))
          .containsText(uiTexts.getAccountEmailConfirmationSuccess());
      inbox.awaitSecurityMail(providerEmail, uiTexts.getAccountEmailChangedSubject());
      inbox.awaitSecurityMail(contact, uiTexts.getAccountEmailChangedSubject());
    }
    begin("google", providerEmail, "valid");
    assertThat(browserPage).hasURL(Pattern.compile(".*/two-factor$"));
    TwoFactorPage.completeIfRequired(browserPage, providerEmail, null);
    assertThat(browserPage).hasURL(Pattern.compile(".*/home$"));
    browserPage.navigate("/account?tab=profile");
    assertThat(browserPage.locator("#account-email input")).hasValue(contact);
  }

  @ParameterizedTest
  @ValueSource(strings = {"nonce", "audience", "cancel"})
  @DisplayName("OIDC-03: Ungültige Antwort oder Abbruch vergibt keine Portalsitzung")
  void rejectedProviderResponse(String mode) {
    begin("google", "invalid-" + UUID.randomUUID() + "@gmail.com", mode);
    assertThat(browserPage).hasURL(Pattern.compile(".*/external-registration\\?error=external$"));
    assertThat(browserPage.locator("#external-register-submit")).isHidden();
    browserPage.navigate("/account");
    browserPage.waitForURL(url -> URI.create(url).getPath().endsWith("/login"));
    assertTrue(URI.create(browserPage.url()).getPath().endsWith("/login"));
  }
}
