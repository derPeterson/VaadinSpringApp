package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
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
  @DisplayName("OIDC-13: Anmeldefehler nach direktem Rücksprung verwenden den Standardtoast")
  void loginFailureAfterRedirectUsesThePublicTheme(int width) {
    page.setViewportSize(width, 900);
    page.navigate("/login?error=unavailable");
    assertStandardNotification(page, texts.getLoginUnavailableMessage(), "error");
    new LoginPage(page).assertFormVisible();
    AllurePlaywright.attachScreenshot("Anmeldefehler nach direktem Rücksprung", page);
  }

  @ParameterizedTest
  @CsvSource({"1280, false", "390, false", "1280, true", "390, true"})
  @DisplayName(
      "OIDC-12: Deaktivierte Anbieter erhalten Verbindungen und bieten nur nutzbare Bestätigungen")
  void disabledProvidersPreserveExistingAccountAccess(int width, boolean hasPassword) {
    page.setViewportSize(width, 900);
    String email = "disabled-providers-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    if (hasPassword) {
      page.navigate("/account?tab=security");
      page.locator("#account-password-edit-security").click();
      page.locator("#external-confirm-google").click();
      providerIdentity(email, "valid");
      page.locator("#account-password-new input").fill("OptionalPortal1!");
      page.locator("#account-password-confirm input").fill("OptionalPortal1!");
      page.locator("#account-password-save").click();
      assertThat(page.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getAccountSecurityPasswordSuccess());
    } else {
      page.navigate("/account?tab=security");
      page.locator("#account-password-edit-security").click();
      assertThat(page.locator("#external-confirm-google")).isVisible();
    }
    try (BrowserContext adminContext =
        browser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setLocale(texts.browserLocale())
                .setTimezoneId("Europe/Berlin"))) {
      Page admin = adminContext.newPage();
      admin.navigate("/login");
      new UserMenu(admin).selectTestLanguage();
      LoginPage login = new LoginPage(admin);
      login.assertFormVisible();
      login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      new UserMenu(admin).selectTestLanguage();
      admin.navigate("/admin?section=configuration&group=external_login");
      try {
        setProviderAvailability(admin, false);
        if (!hasPassword) {
          // Eine bereits offene Bestätigung darf keinen deaktivierten Anbieter mehr starten.
          page.locator("#external-confirm-google").click();
          assertThat(page.locator("#external-identity-dialog")).hasJSProperty("opened", false);
          assertStandardNotification(page, texts.getExternalProviderUnavailable(), "warning");
        }
        // Ein direkter Seitenaufruf muss die bestehenden Zuordnungen weiter anzeigen.
        page.navigate("/account?tab=security");
        page.locator("#account-security-tab-providers").click();
        assertThat(page.locator(".external-connection-row")).hasCount(1);
        assertThat(page.locator(".external-connection-heading vaadin-badge"))
            .hasText(texts.getExternalConnected());
        assertThat(page.locator(".external-connection-row"))
            .containsText(texts.getExternalProviderUnavailable());
        Locator disconnect = page.locator("#external-connect-google");
        if (hasPassword) {
          assertThat(disconnect).isEnabled();
          disconnect.click();
          assertThat(page.locator("#external-confirm-password")).isVisible();
          assertThat(page.locator("#external-confirm-google")).hasCount(0);
          page.locator("#external-proof-cancel").click();
        } else {
          assertThat(disconnect).isDisabled();
        }
        AllurePlaywright.attachScreenshot(
            "Deaktivierter Anbieter bleibt mit seinem Konto verbunden", page);
        page.locator("#account-security-tab-portal").click();
        page.locator("#account-password-edit-security").click();
        assertThat(page.locator("#external-identity-dialog")).hasJSProperty("opened", false);
        if (hasPassword) {
          assertThat(page.locator("#account-password-current")).isVisible();
          page.locator("#account-password-cancel").click();
        } else {
          assertUnavailableConfirmation();
          assertThat(page.locator("#account-password-change-dialog"))
              .hasJSProperty("opened", false);
        }
        page.locator("#account-email-edit-security").click();
        if (hasPassword) {
          assertThat(page.locator("#account-email-password")).isVisible();
          page.locator("#account-email-cancel").click();
        } else {
          assertUnavailableConfirmation();
          assertThat(page.locator("#account-email-change-dialog")).hasJSProperty("opened", false);
        }
        AllurePlaywright.attachScreenshot("Kontoführung ohne verfügbare Anmeldeanbieter", page);
      } finally {
        setProviderAvailability(admin, true);
        // Das Fenster erst nach der Logout-Weiterleitung schließen, sonst bleibt sein Platz belegt.
        new UserMenu(admin).signOut();
      }
    }
    page.reload();
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#external-confirm-google")).isVisible();
    page.locator("#external-proof-cancel").click();
  }

  private void assertUnavailableConfirmation() {
    assertStandardNotification(page, texts.getExternalProofUnavailable(), "warning");
    assertThat(page.locator("#external-confirm-password")).isHidden();
  }

  private void setProviderAvailability(Page admin, boolean enabled) {
    for (String provider : new String[] {"google", "microsoft"}) {
      admin.locator("#provider-tab-" + provider).click();
      admin
          .locator("#provider-" + provider + "-enabled")
          .getByRole(AriaRole.CHECKBOX)
          .setChecked(enabled);
      admin.locator("#provider-" + provider + "-save").click();
      assertThat(admin.locator("#provider-" + provider + "-status"))
          .hasText(
              enabled ? texts.getProviderSettingsActive() : texts.getProviderSettingsInactive());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-11: Lokales Konto verbindet Anbieter erst nach kontextbezogener Passwortbestätigung")
  void localAccountConnectsFromContextualDialog(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/account?tab=security");
    page.locator("#account-security-tab-providers").click();
    assertThat(page.locator("#external-security-action")).hasCount(0);
    assertThat(page.locator(".external-connection-row")).hasCount(2);
    assertThat(page.locator("#external-identity-dialog")).hasJSProperty("opened", false);
    AllurePlaywright.attachScreenshot("Verbundene Anmeldungen: kompakte Anbieterzeilen", page);
    page.locator("#external-connect-google").click();
    assertThat(page.locator("#external-confirm-password")).isVisible();
    assertThat(page.locator("#external-confirm-google")).hasCount(0);
    page.locator("#external-confirm-local").click();
    assertThat(page.locator("#external-confirm-password")).hasAttribute("invalid", "");
    assertStandardNotification(page, texts.getBaseValidationRequiredMessage(), "error");
    assertThat(page.locator("#external-confirm-password [part=error-message]")).isHidden();
    page.locator("#external-confirm-password input").fill("wrong-password");
    page.locator("#external-confirm-local").click();
    assertStandardNotification(page, texts.getAccountSecurityWrongPassword(), "error");
    assertThat(page.locator("#external-confirm-password [part=error-message]")).isHidden();
    page.locator("#external-confirm-password input").fill("unconfirmed-draft");
    page.locator("#external-proof-cancel").click();
    page.locator("#external-connect-google").click();
    assertThat(page.locator("#external-confirm-password input")).hasValue("");
    page.locator("#external-confirm-password input").fill(BrowserTestAccounts.ADMIN_PASSWORD);
    AllurePlaywright.attachScreenshot("Kontextbezogene Bestätigung für Google verbinden", page);
    page.locator("#external-confirm-local").click();
    providerIdentity("context-" + UUID.randomUUID() + "@gmail.com", "valid");
    assertThat(page.locator("#external-connect-google"))
        .hasText(texts.getExternalDisconnect("Google"));
    page.locator("#external-connect-google").click();
    assertThat(page.locator("#external-confirm-google")).isVisible();
    page.locator("#external-confirm-password input").fill(BrowserTestAccounts.ADMIN_PASSWORD);
    page.locator("#external-confirm-local").click();
    assertThat(page).hasURL(Pattern.compile(".*/login$"));
    page.navigate(
        Path.of("docs/design/frontend-referenzen/portal.html").toAbsolutePath().toUri()
            + "#security");
    assertThat(page.locator(".external-connection-row")).hasCount(2);
    page.locator("#security-tab-providers").click();
    AllurePlaywright.attachScreenshot("HTML-Referenz: verbundene Anmeldungen als Zeilen", page);
    page.locator("[data-external-provider=google]").click();
    assertThat(page.locator("#externalIdentityDialog")).isVisible();
    assertThat(page.locator("#externalIdentityTitle")).hasText("Google trennen");
    page.locator("#externalProofPassword").fill("Discarded-example");
    page.locator("#externalIdentityDialog [data-close-dialog]").last().click();
    page.locator("[data-external-provider=google]").click();
    assertThat(page.locator("#externalProofPassword")).hasValue("");
    AllurePlaywright.attachScreenshot("HTML-Referenz: kontextbezogener Bestätigungsdialog", page);
    assertTrue((Boolean) page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
    page.locator("#externalIdentityDialog [data-close-dialog]").last().click();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-08: Administratoren schalten Anbieter ohne Neustart frei und Geheimnisse bleiben serverseitig")
  void administrativeProviderSettings(int width) throws Exception {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/admin?section=configuration&group=external_login");
    assertThat(page.locator("#admin-provider-settings")).isVisible();
    AllurePlaywright.attachScreenshot("Anbietereinrichtung mit Google und Microsoft", page);
    for (String provider : new String[] {"google", "microsoft"}) {
      page.locator("#provider-tab-" + provider).click();
      com.microsoft.playwright.Locator enabled =
          page.locator("#provider-" + provider + "-enabled").getByRole(AriaRole.CHECKBOX);
      com.microsoft.playwright.Locator save = page.locator("#provider-" + provider + "-save");
      com.microsoft.playwright.Locator secret =
          page.locator("#provider-" + provider + "-secret input");
      assertThat(secret).hasValue("");
      String originalId = page.locator("#provider-" + provider + "-client-id input").inputValue();
      page.locator("#provider-" + provider + "-client-id input").fill("");
      save.click();
      assertStandardNotification(page, texts.getProviderSettingsIncomplete(), "error");
      page.locator("#provider-" + provider + "-client-id input").fill(originalId);
      assertThat(page.locator("#provider-" + provider + "-redirect input"))
          .hasValue(System.getProperty("browser.tests.baseUrl") + "/login/oauth2/code/" + provider);
      enabled.setChecked(false);
      save.click();
      assertStandardNotification(page, texts.getProviderSettingsSaved(), "success");
      assertThat(page.locator("#provider-" + provider + "-status"))
          .hasText(texts.getProviderSettingsInactive());
      try (com.microsoft.playwright.BrowserContext visitor =
          browser.newContext(
              new com.microsoft.playwright.Browser.NewContextOptions()
                  .setBaseURL(System.getProperty("browser.tests.baseUrl")))) {
        Page anonymous = visitor.newPage();
        anonymous.navigate("/login");
        assertThat(anonymous.locator("#external-login-" + provider)).hasCount(0);
        assertThat(anonymous.locator("vaadin-password-field")).isVisible();
      } finally {
        // Neu eingegebene Geheimnisse werden tatsächlich verwendet, aber nie zurückgeladen.
        secret.fill(BrowserTestAccounts.OIDC_CLIENT_SECRET);
        page.locator("#provider-" + provider + "-client-id input").fill(originalId);
        enabled.setChecked(true);
        save.click();
        assertThat(page.locator("#provider-" + provider + "-status"))
            .hasText(texts.getProviderSettingsActive());
      }
      page.reload();
      page.locator("#provider-tab-" + provider).click();
      assertThat(secret).hasValue("");
      assertThat(page.locator("#provider-" + provider + "-status"))
          .hasText(texts.getProviderSettingsActive());
    }
    if (width == 1280) {
      for (String provider : new String[] {"google", "microsoft"}) {
        page.locator("#provider-tab-" + provider).click();
        page.locator("#provider-" + provider + "-enabled").getByRole(AriaRole.CHECKBOX).uncheck();
        page.locator("#provider-" + provider + "-save").click();
        assertThat(page.locator("#provider-" + provider + "-status"))
            .hasText(texts.getProviderSettingsInactive());
      }
      // Auch mit zwei deaktivierten Anbietern muss der Server ohne OAuth-Neukonfiguration starten.
      page.navigate("about:blank");
      de.derpeterson.browsertests.BrowserTestRunner.restartTestApplication();
      page.navigate("/admin?section=configuration&group=external_login");
      for (String provider : new String[] {"google", "microsoft"}) {
        page.locator("#provider-tab-" + provider).click();
        assertThat(page.locator("#provider-" + provider + "-status"))
            .hasText(texts.getProviderSettingsInactive());
        assertThat(page.locator("#provider-" + provider + "-secret input")).hasValue("");
        page.locator("#provider-" + provider + "-enabled").getByRole(AriaRole.CHECKBOX).check();
        page.locator("#provider-" + provider + "-save").click();
        assertThat(page.locator("#provider-" + provider + "-status"))
            .hasText(texts.getProviderSettingsActive());
      }
    }
    AllurePlaywright.attachScreenshot("Anmeldeanbieter im bestehenden Referenzstyle", page);
    // Der anschließende echte Code-/Tokenaustausch mit dem eigenen Anbieter prüft die Übernahme.
    new UserMenu(page).signOut();
    begin("google", "admin-settings-" + UUID.randomUUID() + "@gmail.com", "valid");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("PROFILE-01: Adminanlage und Bearbeitung erhalten leere freiwillige Profilangaben")
  void administrativeOptionalProfile(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    AdminUsersPage users = new AdminUsersPage(page);
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

  private void begin(String provider, String email, String mode) {
    begin(page, provider, email, mode, false);
  }

  private void begin(Page target, String provider, String email, String mode, boolean remember) {
    target.navigate("/login");
    target
        .getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions().setName(texts.getLoginRememberMeCheckbox()).setExact(true))
        .setChecked(remember);
    assertThat(target.locator("#external-login-" + provider + " img")).isVisible();
    target.waitForCondition(
        () ->
            (Boolean)
                target
                    .locator("#external-login-" + provider + " img")
                    .evaluate("image => image.complete && image.naturalWidth > 0"));
    target.locator("#external-login-" + provider).click();
    providerIdentity(target, email, mode);
  }

  private void providerIdentity(String email, String mode) {
    providerIdentity(page, email, mode);
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
    page.setViewportSize(width, 900);
    String google = "link-" + UUID.randomUUID() + "@gmail.com";
    String microsoft = "link-" + UUID.randomUUID() + "@example.test";
    begin("google", google, "valid");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    page.locator("#account-security-tab-providers").click();
    page.locator("#external-connect-microsoft").click();
    page.locator("#external-confirm-google").click();
    providerIdentity(google, "valid");
    assertThat(page.locator("#external-security-result")).hasText(texts.getExternalProofReady());
    page.locator("#external-confirm-local").click();
    providerIdentity(microsoft, "valid");
    assertThat(page.locator("#external-connect-microsoft"))
        .hasText(texts.getExternalDisconnect("Microsoft"));
    page.locator("#external-connect-google").click();
    page.locator("#external-confirm-google").click();
    providerIdentity(google, "valid");
    page.locator("#external-confirm-local").click();
    assertThat(page).hasURL(Pattern.compile(".*/login$"));
    begin("microsoft", microsoft, "valid");
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    page.locator("#account-security-tab-providers").click();
    assertThat(page.locator("#external-connect-microsoft")).isDisabled();
    page.locator("#account-security-tab-portal").click();
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#external-confirm-microsoft")).isVisible();
    assertProviderProofIcon("microsoft");
    rejectedPasswordProof(
        "microsoft", microsoft, "missing-auth-time", texts.getExternalProofFreshRequired());
    rejectedPasswordProof("microsoft", microsoft, "stale", texts.getExternalProofFreshRequired());
    AllurePlaywright.attachScreenshot("Letzte externe Anmeldung bleibt geschützt", page);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-01: Google registriert ohne Portalpasswort, erhält die gewählte Sprache und meldet erneut an")
  void googleRegistrationAndReturn(int width) {
    page.setViewportSize(width, 900);
    String email = "google-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    assertThat(page).hasURL(Pattern.compile(".*/external-registration$"));
    assertThat(page.locator("vaadin-password-field")).hasCount(0);
    assertThat(page.locator("#external-register-first input")).hasValue("External");
    String registrationLanguage = texts.language().equals("de") ? "en" : "de";
    new UserMenu(page).selectLanguage(registrationLanguage);
    AllurePlaywright.attachScreenshot("Ergänzende Registrierung ohne Portalpasswort", page);
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=profile");
    Locale selectedLanguage = Locale.forLanguageTag(registrationLanguage);
    assertThat(page.locator("#account-language input"))
        .hasValue(selectedLanguage.getDisplayLanguage(selectedLanguage));
    page.reload();
    assertThat(page.locator("#account-language input"))
        .hasValue(selectedLanguage.getDisplayLanguage(selectedLanguage));
    new UserMenu(page).selectTestLanguage();
    assertThat(page.locator("#account-birth-date input")).hasValue("");
    assertThat(page.locator("#account-gender input")).hasValue("");
    page.navigate("/account?tab=security");
    page.locator("#account-security-tab-providers").click();
    assertThat(page.locator("#external-connect-google")).isDisabled();
    assertThat(page.locator("#account-external-connections")).isVisible();
    AllurePlaywright.attachScreenshot("Externes Konto ohne erfundene Profilwerte", page);
    new UserMenu(page).requestSignOut();
    begin("google", email, "valid");
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    assertTrue((Boolean) page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-02: Microsoft erfordert eigenen E-Mail-Nachweis und keine zusätzliche Passwortregistrierung")
  void microsoftRequiresContactVerification(int width) {
    page.setViewportSize(width, 900);
    String email = "microsoft-" + UUID.randomUUID() + "@example.test";
    begin("microsoft", email, "valid");
    assertThat(page).hasURL(Pattern.compile(".*/external-registration$"));
    page.locator("#external-register-first input").fill("Browser");
    page.locator("#external-register-last input").fill("Testuser");
    page.locator("#external-register-submit").click();
    assertThat(page.locator(".public-panel")).containsText(texts.getExternalVerifyEmail());
    MailpitInbox inbox = new MailpitInbox(context);
    MailpitInbox.CapturedMail mail = inbox.awaitVerificationMail(email);
    assertThat(page.locator(".public-header")).isVisible();
    AllurePlaywright.attachScreenshot("Microsoft: Kontaktbestätigung vor Portalzugang", page);
    try (Page activation = inbox.openVerificationLink(mail)) {
      new de.derpeterson.browsertests.pages.VerificationPage(activation).assertActivated();
    }
    begin("microsoft", email, "valid");
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
  }

  @ParameterizedTest
  @CsvSource({"1280, missing-auth-time", "390, missing-auth-time", "1280, stale", "390, stale"})
  @DisplayName(
      "OIDC-05: Google-Zwischenlösung bestätigt nur das verbundene Konto über einen neuen Durchlauf")
  void optionalPortalPassword(int width, String confirmationMode) {
    page.setViewportSize(width, 900);
    String email = "password-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    page.locator("#account-password-edit-security").click();
    assertProviderProofIcon("google");
    rejectedPasswordProof("google", email, "cancel", texts.getExternalError());
    rejectedPasswordProof(
        "google",
        "different-" + UUID.randomUUID() + "@gmail.com",
        "valid",
        texts.getExternalProofIdentityMismatch());
    rejectedPasswordProof("google", email, "nonce", texts.getExternalError());
    rejectedPasswordProof("google", email, "audience", texts.getExternalError());
    page.locator("#external-confirm-google").click();
    assertThat(page.locator("#fixture-authorize-parameters"))
        .hasAttribute("data-prompt", "select_account consent");
    assertThat(page.locator("#fixture-authorize-parameters")).hasAttribute("data-max-age", "");
    assertThat(page.locator("#fixture-authorize-parameters")).hasAttribute("data-claims", "");
    providerIdentity(email, confirmationMode);
    assertThat(page.locator("#account-password-new")).isVisible();
    assertThat(page.locator("#account-password-current")).isHidden();
    page.locator("#account-password-new input").fill("OptionalPortal1!");
    page.locator("#account-password-confirm input").fill("OptionalPortal1!");
    page.locator("#account-password-save").click();
    assertStandardNotification(page, texts.getAccountSecurityPasswordSuccess(), "success");
    page.locator("#account-security-tab-providers").click();
    assertThat(page.locator("#external-connect-google")).isEnabled();
    page.locator("#account-security-tab-portal").click();
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#external-confirm-password")).isVisible();
    page.locator("#external-proof-cancel").click();
    page.locator("#account-email-edit-security").click();
    page.locator("#external-confirm-google").click();
    providerIdentity(email, "valid");
    assertThat(page.locator("#account-email-password")).isHidden();
    page.locator("#account-email-cancel").click();
    new UserMenu(page).requestSignOut();
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(email, "OptionalPortal1!");
    assertThat(page.locator("vaadin-button.tracked-user-button")).isVisible();
  }

  private void rejectedPasswordProof(String provider, String email, String mode, String message) {
    page.locator("#external-confirm-" + provider).click();
    if (provider.equals("microsoft")) {
      assertThat(page.locator("#fixture-authorize-parameters"))
          .hasAttribute("data-prompt", "login");
      assertThat(page.locator("#fixture-authorize-parameters")).hasAttribute("data-max-age", "0");
      assertThat(page.locator("#fixture-authorize-parameters"))
          .hasAttribute("data-claims", "{\"id_token\":{\"auth_time\":{\"essential\":true}}}");
    }
    providerIdentity(email, mode);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security&external_error=.*"));
    assertStandardNotification(page, message, "error");
    AllurePlaywright.attachScreenshot(
        "Abgelehnter Anbieternachweis im Standarddesign: " + mode, page);
    assertThat(page.locator("#account-password-new")).isHidden();
    assertThat(page.locator("#external-register-submit")).hasCount(0);
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#external-confirm-local")).isDisabled();
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
    Locator button = page.locator("#external-confirm-" + provider);
    Locator image = button.locator("img");
    assertThat(image).hasCSS("width", "16px");
    assertThat(image).hasCSS("height", "16px");
    assertThat(button).hasCSS("height", "36px");
    AllurePlaywright.attachScreenshot("Bestätigungsaktion für " + provider, page);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @DisplayName(
      "OIDC-06: Externe Anmeldung bleibt nur mit bewusster Auswahl nach Browserneustart erhalten")
  void externalRememberChoiceSurvivesOnlyWhenRequested(boolean remember) {
    String email = "remember-" + UUID.randomUUID() + "@gmail.com";
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      begin(actor.page, "google", email, "valid", remember);
      actor.page.locator("#external-register-submit").click();
      assertThat(actor.page).hasURL(Pattern.compile(".*/home$"));
      assertEquals(
          remember,
          actor.context.cookies().stream()
              .anyMatch(cookie -> cookie.name.equals("remember-me") && cookie.expires > 0));
    }
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      if (remember) {
        new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
        new UserMenu(actor.page).requestSignOut();
      } else {
        actor.page.navigate("/account");
        new LoginPage(actor.page).assertFormVisible();
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-07: Passwortloses Konto ändert Kontaktadresse nach Anbieterbeweis und eigenem Einmallink")
  void passwordlessContactChangeKeepsTheStableProviderIdentity(int width) {
    page.setViewportSize(width, 900);
    String providerEmail = "contact-" + UUID.randomUUID() + "@gmail.com";
    String contact = "changed-" + UUID.randomUUID() + "@example.test";
    begin("google", providerEmail, "valid");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    page.locator("#account-email-edit-security").click();
    page.locator("#external-confirm-google").click();
    providerIdentity(providerEmail, "valid");
    assertThat(page.locator("#account-email-password")).isHidden();
    page.locator("#account-email-new input").fill(contact);
    page.locator("#account-email-confirm input").fill(contact);
    UiActions.complete(page, () -> page.locator("#account-email-request").click());
    assertThat(page.locator("vaadin-notification-card"))
        .containsText(texts.getAccountSecurityEmailRequested());
    MailpitInbox inbox = new MailpitInbox(context);
    try (Page confirmation =
        inbox.openEmailChangeAction(
            inbox.awaitSecurityMail(contact, texts.getAccountEmailChangeSubject()))) {
      UiActions.complete(
          confirmation, () -> confirmation.locator("#account-email-confirm-action").click());
      assertThat(confirmation.locator("#account-email-confirmation"))
          .containsText(texts.getAccountEmailConfirmationSuccess());
      inbox.awaitSecurityMail(providerEmail, texts.getAccountEmailChangedSubject());
      inbox.awaitSecurityMail(contact, texts.getAccountEmailChangedSubject());
    }
    begin("google", providerEmail, "valid");
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=profile");
    assertThat(page.locator("#account-email input")).hasValue(contact);
  }

  @ParameterizedTest
  @ValueSource(strings = {"nonce", "audience", "cancel"})
  @DisplayName("OIDC-03: Ungültige Antwort oder Abbruch vergibt keine Portalsitzung")
  void rejectedProviderResponse(String mode) {
    begin("google", "invalid-" + UUID.randomUUID() + "@gmail.com", mode);
    assertThat(page).hasURL(Pattern.compile(".*/external-registration\\?error=external$"));
    assertThat(page.locator("#external-register-submit")).isHidden();
    page.navigate("/account");
    assertThat(page).hasURL(Pattern.compile("^.*/login(?:\\?.*)?$"));
  }
}
