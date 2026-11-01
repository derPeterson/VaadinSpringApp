package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Externe Anmeldung")
public class ExternalLoginBrowserTest extends BrowserFixture {
  @TempDir Path profile;

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
      assertThat(page.locator("#provider-" + provider + "-redirect input"))
          .hasValue(System.getProperty("browser.tests.baseUrl") + "/login/oauth2/code/" + provider);
      enabled.setChecked(false);
      save.click();
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

  private void chooseAction(String label) {
    page.locator("#external-security-action input").click();
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(label).setExact(true))
        .click();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-04: Verknüpfen und Trennen brauchen frischen Nachweis und erhalten die letzte Anmeldemethode")
  void connectAndDisconnectWithFreshProviderProof(int width) {
    page.setViewportSize(width, 900);
    String google = "link-" + UUID.randomUUID() + "@gmail.com";
    String microsoft = "link-" + UUID.randomUUID() + "@example.test";
    begin("google", google, "valid");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    chooseAction(texts.getExternalConnect("Microsoft"));
    page.locator("#external-confirm-google").click();
    providerIdentity(google, "valid");
    assertThat(page.locator("#external-security-result")).hasText(texts.getExternalProofReady());
    page.locator("#external-connect-microsoft").click();
    providerIdentity(microsoft, "valid");
    assertThat(page.locator("#external-connect-microsoft"))
        .hasText(texts.getExternalDisconnect("Microsoft"));
    chooseAction(texts.getExternalDisconnect("Google"));
    page.locator("#external-confirm-google").click();
    providerIdentity(google, "valid");
    page.locator("#external-connect-google").click();
    assertThat(page).hasURL(Pattern.compile(".*/login$"));
    begin("microsoft", microsoft, "valid");
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    assertThat(page.locator("#external-connect-microsoft")).isDisabled();
    assertThat(page.locator("#external-confirm-microsoft")).isVisible();
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
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "OIDC-05: Externes Konto kann nach frischem Nachweis freiwillig ein Portalpasswort setzen")
  void optionalPortalPassword(int width) {
    page.setViewportSize(width, 900);
    String email = "password-" + UUID.randomUUID() + "@gmail.com";
    begin("google", email, "valid");
    page.locator("#external-register-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    page.locator("#external-confirm-google").click();
    providerIdentity(email, "valid");
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#account-password-current")).isHidden();
    page.locator("#account-password-new input").fill("OptionalPortal1!");
    page.locator("#account-password-confirm input").fill("OptionalPortal1!");
    page.locator("#account-password-save").click();
    assertThat(page.locator("vaadin-notification-card"))
        .containsText(texts.getAccountSecurityPasswordSuccess());
    assertThat(page.locator("#external-connect-google")).isEnabled();
    assertThat(page.locator("#external-confirm-local")).isVisible();
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#account-password-current")).isVisible();
    page.locator("#account-password-cancel").click();
    chooseAction(texts.getAccountSettingsChangeEmail());
    page.locator("#external-confirm-google").click();
    providerIdentity(email, "valid");
    page.locator("#account-email-edit-security").click();
    assertThat(page.locator("#account-email-password")).isHidden();
    page.locator("#account-email-cancel").click();
    new UserMenu(page).requestSignOut();
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(email, "OptionalPortal1!");
    assertThat(page.locator("vaadin-button.tracked-user-button")).isVisible();
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
      assertTrue(
          actor.context.cookies().stream()
                  .anyMatch(cookie -> cookie.name.equals("remember-me") && cookie.expires > 0)
              == remember);
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
    chooseAction(texts.getAccountSettingsChangeEmail());
    page.locator("#external-confirm-google").click();
    providerIdentity(providerEmail, "valid");
    page.locator("#account-email-edit-security").click();
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
    assertThat(page).hasURL(Pattern.compile(".*/login(?:\\?.*)?$"));
  }
}
