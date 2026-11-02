package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TwoFactorPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.playwright.AllurePlaywright;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
public class TwoFactorBrowserTest extends BrowserFixture {
  @TempDir Path profile;

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-17: Verpflichtender Einstieg A nutzt die frische Passwortanmeldung ohne zweite Abfrage")
  void requiredIntroductionReusesFreshPasswordWithoutGrantingPortalAccess(int width) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    String email = "intro-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.page.setViewportSize(width, 900);
      LoginPage fresh = new LoginPage(actor.page);
      fresh.open();
      fresh.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.page).selectTestLanguage();
      assertThat(actor.page.locator("#two-factor-introduction-title"))
          .hasText(texts.getTwoFactorRequiredTitle());
      assertThat(actor.page.locator("#two-factor-password")).hasCount(0);
      assertThat(actor.page.locator("#two-factor-submit")).hasText(texts.getTwoFactorBeginSetup());
      assertThat(actor.page.locator("#two-factor-cancel")).hasText(texts.getBaseLogoutButton());
      assertTrue(
          (Boolean)
              actor
                  .page
                  .locator(".two-factor-introduction-art")
                  .evaluate("image => image.complete && image.naturalWidth > 0"));
      assertTrue(
          (Boolean)
              actor.page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
      AllurePlaywright.attachScreenshot(
          "Vaadin: verpflichtender Einstieg A bei " + width + "px", actor.page);
      actor.page.locator("#two-factor-cancel").click();
      assertThat(actor.page).hasURL(Pattern.compile(".*/login$"));
      actor.page.navigate("/home");
      assertThat(actor.page).hasURL(Pattern.compile(".*/login.*"));
      fresh.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.reload();
      assertThat(actor.page.locator("#two-factor-introduction-title")).isVisible();
      actor.page.navigate("/admin");
      assertThat(actor.page).hasURL(Pattern.compile(".*/two-factor.*"));
      actor.page.locator("#two-factor-submit").click();
      assertThat(actor.page.locator("#two-factor-qr")).isVisible();
      assertThat(actor.page.locator("#two-factor-password")).hasCount(0);
      TwoFactorPage.enrollCurrent(actor.page, email);
      assertThat(actor.page).hasURL(Pattern.compile(".*/admin$"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "password,1280",
    "password,390",
    "google,1280",
    "google,390",
    "microsoft,1280",
    "microsoft,390"
  })
  @DisplayName(
      "MFA-12: Letzter Ersatzcode erlaubt nur den gezielten Wechsel vor neuem Kontozugriff")
  void lastRecoveryCodeReplacesTheLostAuthenticatorWithoutGeneralAccess(
      String provider, int width) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "recover-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).signOut();
    page.setViewportSize(width, 900);
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/account?tab=security");
    page.locator("#account-two-factor-setup").click();
    page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    page.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(page, email);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
    new UserMenu(page).selectTestLanguage();
    if (!provider.equals("password")) {
      page.locator("#account-security-tab-providers").click();
      page.locator("#external-connect-" + provider).click();
      page.locator("#external-confirm-factor input").fill(TwoFactorPage.nextCode(page, email));
      page.locator("#external-confirm-local").click();
      providerIdentity(email);
      assertThat(page.locator("#external-connect-" + provider))
          .hasText(texts.getExternalDisconnect(provider.equals("google") ? "Google" : "Microsoft"));
      page.navigate("/account?tab=security");
    }
    // Echte Bestätigungen ohne Kontoänderung verbrauchen alle bis auf den letzten ausgegebenen
    // Code.
    int confirmations = TwoFactorPage.remainingRecoveryCodes(email) - 1;
    for (int index = 0; index < confirmations; index++) {
      // Unterschiedliche Zwecke erfordern jeweils einen neuen Nachweis statt einer offenen
      // Freigabe.
      String action = index % 2 == 0 ? "password" : "email";
      page.locator("#account-" + action + "-edit-security").click();
      page.locator("#external-confirm-factor input").fill(TwoFactorPage.nextCode(page, email));
      page.locator("#external-confirm-local").click();
      assertThat(page.locator("#account-" + action + "-new")).isVisible();
      page.locator("#account-" + action + "-cancel").click();
      assertThat(page.locator("#account-" + action + "-new")).isHidden();
    }
    String last = TwoFactorPage.nextCode(page, email);
    new UserMenu(page).requestSignOut();
    assertThat(page.locator(".public-header")).isVisible();
    signInUntilSecondFactor(email, provider);
    page.locator("#two-factor-recover").click();
    assertThat(page.locator("#two-factor-panel")).containsText(texts.getTwoFactorRecoverHint());
    page.locator("#two-factor-code input").fill(last);
    page.locator("#two-factor-submit").click();
    assertThat(page.locator("#two-factor-qr")).isVisible();
    try (Page blocked = context.newPage()) {
      blocked.navigate("/account?tab=profile");
      assertThat(blocked).hasURL(Pattern.compile(".*/two-factor.*"));
      assertThat(blocked.locator("#two-factor-qr")).isVisible();
      assertThat(blocked.locator("#account-panel-profile")).hasCount(0);
    }
    page.reload();
    TwoFactorPage.enrollCurrent(page, email);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
    assertThat(page.locator("#account-two-factor")).containsText(texts.getTwoFactorEnrolled());
    AllurePlaywright.attachScreenshot("Wiederhergestellter Authenticator mit neuem Codesatz", page);
    new UserMenu(page).requestSignOut();
    assertThat(page.locator(".public-header")).isVisible();
    signInUntilSecondFactor(email, provider);
    page.locator("#two-factor-code input").fill(last);
    page.locator("#two-factor-submit").click();
    assertThat(page.locator("#two-factor-code")).hasAttribute("invalid", "");
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.getTwoFactorInvalidCode());
    page.locator("#two-factor-code input").fill(TwoFactorPage.nextCode(page, email));
    page.locator("#two-factor-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
  }

  private void signInUntilSecondFactor(String email, String provider) {
    LoginPage login = new LoginPage(page);
    login.open();
    if (provider.equals("password")) {
      login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    } else {
      page.locator("#external-login-" + provider).click();
      providerIdentity(email);
    }
    assertThat(page).hasURL(Pattern.compile(".*/two-factor.*"));
    new UserMenu(page).selectTestLanguage();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-11: Freiwilliger Authenticator wird nur mit eigenem Faktor gewechselt und entfernt")
  void ownAuthenticatorCanBeReplacedAndRemovedWithIndependentProof(int width) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "manage-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(browser)) {
      user.page.setViewportSize(width, 900);
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      user.page.navigate("/account?tab=security");
      user.page.locator("#account-two-factor-setup").click();
      user.page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
      user.page.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(user.page, email);
      assertThat(user.page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
      new UserMenu(user.page).selectTestLanguage();
      user.page.locator("#account-two-factor-replace").click();
      user.page
          .locator("#two-factor-action-code input")
          .fill(TwoFactorPage.nextCode(user.page, email));
      user.page.locator("#two-factor-action-confirm").click();
      TwoFactorPage.enrollCurrent(user.page, email);
      assertThat(user.page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
      user.page.locator("#account-two-factor-remove").click();
      user.page
          .locator("#two-factor-action-code input")
          .fill(TwoFactorPage.nextCode(user.page, email));
      user.page.locator("#two-factor-action-confirm").click();
      assertThat(user.page.locator("#account-two-factor-setup")).isVisible();
      assertThat(user.page.locator("#account-two-factor vaadin-badge"))
          .hasText(texts.getTwoFactorNotEnrolled());
      assertThat(user.page.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getTwoFactorDisabled());
      AllurePlaywright.attachScreenshot(
          "Freiwilliger Faktor nach geschütztem Wechsel und Entfernen", user.page);
      new UserMenu(user.page).signOut();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-09: Neue Pflichtregel schützt ein bereits angemeldetes Konto und bleibt konfigurierbar")
  void policyChangesProtectAnExistingSession(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "policy-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (BrowserActor user = new BrowserActor(browser)) {
      user.login(email, BrowserTestAccounts.USER_PASSWORD);
      try {
        changeUserLoginRule(texts.getTwoFactorModeRequired());
        user.page.navigate("/account?tab=profile");
        assertThat(user.page).hasURL(Pattern.compile(".*/two-factor.*"));
        assertThat(user.page.locator("#account-first-name")).hasCount(0);
        TwoFactorPage.completeIfRequired(user.page, email, BrowserTestAccounts.USER_PASSWORD);
        user.page.navigate("/account?tab=security");
        assertThat(user.page.locator("#account-two-factor")).isVisible();
        AllurePlaywright.attachScreenshot(
            "Bestehende Sitzung nach neu vorgeschriebener Einrichtung", user.page);
      } finally {
        changeUserLoginRule(texts.getTwoFactorModeEnrolled());
        user.page.navigate("/home");
        TwoFactorPage.completeIfRequired(user.page, email, BrowserTestAccounts.USER_PASSWORD);
        new UserMenu(user.page).requestSignOut();
        assertThat(user.page.locator(".public-header")).isVisible();
      }
    }
  }

  private void changeUserLoginRule(String label) {
    page.navigate("/admin?section=configuration&group=two_factor");
    TwoFactorPage.completeIfRequired(
        page, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    page.navigate("/admin?section=configuration&group=two_factor");
    new UserMenu(page).selectTestLanguage();
    page.locator("#two-factor-role-tabs vaadin-tab")
        .getByText(texts.getRoleUser(), new Locator.GetByTextOptions().setExact(true))
        .click();
    page.locator("#two-factor-rule-role_user-login").click();
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(label).setExact(true))
        .click();
    page.locator("#two-factor-save").click();
    page.locator("#two-factor-action-code input")
        .fill(TwoFactorPage.nextCode(page, BrowserTestAccounts.SUPERADMIN_EMAIL));
    UiActions.complete(page, () -> page.locator("#two-factor-action-confirm").click());
    page.waitForCondition(
        () ->
            java.net.URI.create(page.url()).getPath().equals("/two-factor")
                || page.locator("vaadin-notification-card:not([closing])")
                    .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorSaved()))
                    .isVisible());
    // Die alte Ansicht darf parallel bereits den neuen Regelstand erkennen und weiterleiten.
    // Ein neuer Tab prüft die neue Anforderung ohne konkurrierende Navigation dieser Ansicht.
    try (Page fresh = context.newPage()) {
      fresh.navigate("/admin?section=configuration&group=two_factor");
      TwoFactorPage.completeIfRequired(
          fresh, BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
      fresh.navigate("/admin?section=configuration&group=two_factor");
      assertThat(fresh.locator("#two-factor-save")).isVisible();
    }
  }

  @Test
  @DisplayName(
      "MFA-10: Echter Anwendungsneustart erhält den vorhandenen Faktorstatus derselben Sitzung")
  void serverRestartDoesNotInventOrDiscardSecondFactorFreshness() throws Exception {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    page.navigate("/account?tab=security");
    try (Page second = context.newPage()) {
      second.navigate("/account?tab=security");
      assertThat(second.locator("#account-two-factor")).isVisible();
      BrowserTestRunner.restartTestApplication();
      page.reload();
      second.reload();
      assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
      assertThat(second).hasURL(Pattern.compile(".*/account\\?tab=security$"));
      assertThat(page.locator("#account-two-factor")).isVisible();
      assertThat(second.locator("#account-two-factor")).isVisible();
      assertThat(page.locator("#two-factor-code")).hasCount(0);
    }
  }

  @Test
  @DisplayName("MFA-08: Echter Browserneustart mit Remember-me verlangt einen neuen zweiten Faktor")
  void rememberedFirstFactorDoesNotRestoreThePreviousSecondFactor() {
    LoginPage administrator = new LoginPage(page);
    administrator.open();
    administrator.signIn(
        BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "remember-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      LoginPage login = new LoginPage(actor.page);
      login.open();
      actor
          .page
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(texts.getLoginRememberMeCheckbox())
                  .setExact(true))
          .setChecked(true);
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.navigate("/account?tab=security");
      actor.page.locator("#account-two-factor-setup").click();
      actor.page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
      actor.page.locator("#two-factor-submit").click();
      TwoFactorPage.enrollCurrent(actor.page, email);
      assertThat(actor.page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
      new UserMenu(actor.page).selectTestLanguage();
    }
    try (PersistentBrowserActor actor =
        new PersistentBrowserActor(playwright, profile, "two-factor")) {
      assertThat(actor.page.locator("#two-factor-code")).isVisible();
      assertThat(actor.page.locator("#admin-sidebar")).hasCount(0);
      TwoFactorPage.completeIfRequired(actor.page, email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.navigate("/account?tab=sessions");
      actor.page.locator("#account-logout-others").click();
      actor.page.locator("#sessions-confirmation-submit").click();
      new UserMenu(actor.page).signOut();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-06: HTML-Momentaufnahmen zeigen Einrichtung, Codeausgabe, Rollenregeln und Kontakt")
  void referenceStatesAndKeyboardNavigation(int width) {
    page.setViewportSize(width, 900);
    page.navigate(
        Path.of("docs/design/frontend-referenzen/public.html").toAbsolutePath().toUri()
            + "#public-two-factor");
    assertThat(page.locator("#public-two-factor")).isVisible();
    assertThat(page.locator(".mfa-introduction")).isVisible();
    assertThat(page.locator(".two-factor-introduction-title"))
        .hasText(new UiTexts("de").getTwoFactorRequiredTitle());
    AllurePlaywright.attachScreenshot("HTML: verpflichtende Ersteinrichtung Variante A", page);
    page.locator("[data-mfa-continue]").click();
    assertThat(page.locator(".mfa-qr-placeholder")).isVisible();
    page.locator("[data-mfa-phase=confirm]").click();
    page.locator("[data-mfa-go=recover]").click();
    assertThat(page.locator("[data-mfa-phase-content]")).hasAttribute("data-phase", "recover");
    assertThat(page.locator("[data-mfa-phase-content]"))
        .containsText(new UiTexts("de").getTwoFactorRecoverHint());
    page.locator("[data-mfa-phase=qr]").click();
    assertThat(page.locator(".mfa-qr-placeholder")).isVisible();
    page.locator("[data-mfa-phase=codes]").click();
    assertThat(page.locator(".mfa-code-values")).containsText("ABCDEF01");
    AllurePlaywright.attachScreenshot("HTML: synthetische Zwei-Faktor-Ausgabe", page);
    page.navigate(
        Path.of("docs/design/frontend-referenzen/portal.html").toAbsolutePath().toUri()
            + "#security");
    page.locator("[data-mfa-manage=codes]").click();
    assertThat(page.locator("dialog.mfa-reference")).isVisible();
    page.locator("[data-mfa-close]").click();
    page.navigate(
        Path.of("docs/design/frontend-referenzen/admin.html").toAbsolutePath().toUri().toString());
    if (width == 390) {
      page.locator("#mobileMenuBtn").click();
    }
    page.locator("[data-admin-route=settings]:visible").first().click();
    if (!page.locator("[data-ad-group=TWO_FACTOR]:visible").first().isVisible()) {
      page.locator("#mobileMenuBtn").click();
    }
    page.locator("[data-ad-group=TWO_FACTOR]:visible").first().click();
    assertThat(page.locator("[data-mfa-role]")).hasCount(4);
    page.locator("[data-mfa-role='0']").focus();
    page.keyboard().press("End");
    assertThat(page.locator("[data-mfa-role='3']")).hasAttribute("aria-selected", "true");
    assertThat(page.locator("[data-mfa-rules='3'] select")).hasCount(9);
    AllurePlaywright.attachScreenshot("HTML: rollenbezogene Zwei-Faktor-Regeln", page);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-07: Admin übernimmt eine eingeschränkte Hilfsanfrage und sendet die geprüfte Anleitung")
  void recoverySupportRemainsContactInsteadOfFactorReset(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "support-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    page.navigate("/account?tab=security");
    page.locator("#account-two-factor-setup").click();
    page.locator("#two-factor-password input").fill(BrowserTestAccounts.USER_PASSWORD);
    page.locator("#two-factor-submit").click();
    TwoFactorPage.enrollCurrent(page, email);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
    new UserMenu(page).selectTestLanguage();
    new UserMenu(page).signOut();
    login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    page.navigate("/recovery-help");
    assertThat(page.locator("#account-support")).isVisible();
    page.locator("#support-reply textarea")
        .fill("Synthetische Bitte um Hilfe mit meinen Ersatzcodes.");
    UiActions.complete(page, () -> page.locator("#support-create").click());
    String ticketNumber =
        page.locator("#support-ticket-grid")
            .getByText(Pattern.compile("^#[0-9]+$"))
            .first()
            .innerText();
    try (BrowserActor administrator = new BrowserActor(browser)) {
      administrator.login(
          BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
      Page admin = administrator.page;
      new UserMenu(admin).selectTestLanguage();
      admin.setViewportSize(width, 900);
      admin.navigate("/admin?section=support");
      admin
          .locator("#support-ticket-grid")
          .getByText(ticketNumber, new Locator.GetByTextOptions().setExact(true))
          .click();
      UiActions.complete(admin, () -> admin.locator("#support-claim").click());
      admin.locator("#support-instructions").click();
      assertThat(admin.locator("#support-instruction-preview")).hasJSProperty("opened", true);
      assertThat(admin.locator("#support-instruction-preview"))
          .containsText(texts.getSupportRecoveryInstructions());
      admin.evaluate(
          "async () => { await Promise.all(document.getAnimations().filter(animation => animation.effect?.getTiming().iterations !== Infinity).map(animation => animation.finished.catch(() => {}))); }");
      AllurePlaywright.attachScreenshot("Geprüfte Hilfeantwort ohne neue Zugangscodes", admin);
      admin.locator("#support-instruction-preview-submit").click();
      assertThat(admin.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getSupportQueued());
      new MailpitInbox(administrator.context)
          .awaitSecurityMail(email, texts.getSupportRecoverySubject());
      UiActions.complete(admin, () -> admin.locator("#support-close").click());
      new UserMenu(admin).signOut();
    }
    page.navigate("/admin");
    assertThat(page).hasURL(Pattern.compile(".*/two-factor.*"));
    page.locator("#two-factor-cancel").click();
    new LoginPage(page).assertFormVisible();
  }

  @ParameterizedTest
  @CsvSource({"google,1280", "google,390", "microsoft,1280", "microsoft,390"})
  @DisplayName(
      "MFA-05: Externes Konto richtet 2FA sicher ein, bestätigt neu und verbindet einen zweiten Anbieter")
  void externalAccountEnrollmentAndIndependentSensitiveProof(String provider, int width) {
    page.setViewportSize(width, 900);
    String identity = "mfa-" + UUID.randomUUID() + "@gmail.com";
    page.navigate("/login");
    page.locator("#external-login-" + provider).click();
    providerIdentity(identity);
    page.locator("#external-register-first input").fill("Browser");
    page.locator("#external-register-last input").fill("Testuser");
    page.locator("#external-register-submit").click();
    if (provider.equals("microsoft")) {
      MailpitInbox inbox = new MailpitInbox(context);
      try (Page activation = inbox.openVerificationLink(inbox.awaitVerificationMail(identity))) {
        assertThat(activation.locator(".public-panel")).isVisible();
      }
      page.navigate("/login");
      page.locator("#external-login-" + provider).click();
      providerIdentity(identity);
    }
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    page.locator("#account-two-factor-setup").click();
    page.locator("#two-factor-confirm-" + provider).click();
    providerIdentity(identity);
    if (provider.equals("google")) {
      assertThat(page.locator("#two-factor-code")).isVisible();
      page.locator("#two-factor-code input")
          .fill(new MailpitInbox(context).awaitAuthenticatorSetupCode(identity));
      page.locator("#two-factor-submit").click();
    }
    TwoFactorPage.enrollCurrent(page, identity);
    assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
    new UserMenu(page).signOut();
    page.locator("#external-login-" + provider).click();
    providerIdentity(identity);
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    TwoFactorPage.completeIfRequired(page, identity, null);
    assertThat(page).hasURL(Pattern.compile(".*/home$"));
    page.navigate("/account?tab=security");
    new UserMenu(page).selectTestLanguage();
    page.locator("#account-password-edit-security").click();
    assertThat(page.locator("#external-confirm-password")).isHidden();
    assertThat(page.locator("#external-confirm-" + provider)).hasCount(0);
    page.locator("#external-confirm-factor input").fill("invalid");
    page.locator("#external-confirm-local").click();
    assertThat(page.locator("#external-confirm-factor")).hasJSProperty("invalid", true);
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.getTwoFactorInvalidCode());
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .hasAttribute("theme", Pattern.compile(".*cosmos-reference-error.*"));
    page.locator("vaadin-notification-card:not([closing])").getByRole(AriaRole.BUTTON).click();
    page.locator("#external-confirm-factor input").fill(TwoFactorPage.nextCode(page, identity));
    page.locator("#external-confirm-local").click();
    page.locator("#account-password-new input").fill("OwnOptionalPortal1!");
    page.locator("#account-password-confirm input").fill("OwnOptionalPortal1!");
    page.locator("#account-password-save").click();
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.getAccountSecurityPasswordSuccess());
    page.locator("#account-security-tab-providers").click();
    String second = provider.equals("google") ? "microsoft" : "google";
    page.locator("#external-connect-" + second).click();
    page.locator("#external-confirm-factor input").fill(TwoFactorPage.nextCode(page, identity));
    page.locator("#external-confirm-local").click();
    providerIdentity("linked-" + UUID.randomUUID() + "@gmail.com");
    assertThat(page.locator("#external-connect-" + second))
        .hasText(texts.getExternalDisconnect(second.equals("google") ? "Google" : "Microsoft"));
    AllurePlaywright.attachScreenshot("Eigenständige 2FA bei verbundenen Anmeldeanbietern", page);
  }

  private void providerIdentity(String identity) {
    assertThat(page.locator("#fixture-email")).isVisible();
    page.locator("#fixture-email").fill(identity);
    page.locator("#fixture-mode").selectOption("valid");
    page.locator("#fixture-signin").click();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-01: Authenticator einrichten und dieselbe Sitzung bei Refresh und weiterem Tab erhalten")
  void requiredAuthenticatorSetupAndSessionContinuity(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signInFirstFactor(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    assertThat(page.locator("#two-factor-panel")).isVisible();
    if (page.locator("#two-factor-password").isVisible()) {
      page.locator("#two-factor-password input").fill("WrongSetupPassword!");
      page.locator("#two-factor-submit").click();
      assertThat(page.locator("#two-factor-password")).hasAttribute("invalid", "");
      assertThat(page.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getAccountSecurityWrongPassword());
    }
    AllurePlaywright.attachScreenshot("Eingeschränkter Zwei-Faktor-Zugang", page);
    TwoFactorPage.completeIfRequired(
        page, BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(Pattern.compile(".*/admin$"));
    page.reload();
    assertThat(page.locator("#admin-sidebar")).isAttached();
    try (Page tab = context.newPage()) {
      tab.navigate("/account?tab=security");
      new UserMenu(tab).selectTestLanguage();
      assertThat(tab.locator("#account-two-factor")).containsText(texts.getTwoFactorEnrolled());
      assertThat(tab).hasURL(Pattern.compile(".*/account\\?tab=security$"));
    }
    page.navigate("/account?tab=security");
    new UserMenu(page).selectTestLanguage();
    assertThat(page.locator("#account-two-factor")).containsText(texts.getTwoFactorEnrolled());
    AllurePlaywright.attachScreenshot("Eigene Zwei-Faktor-Verwaltung", page);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("MFA-02: Neue Ersatzcodes nach eigener Bestätigung und geschütztes PDF")
  @Tag("recovery-output")
  void protectedRecoveryCodeIssuance(int width) throws Exception {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    page.navigate("/account?tab=security");
    page.locator("#account-two-factor-renew").click();
    page.locator("#two-factor-action-code input")
        .fill(TwoFactorPage.nextCode(page, BrowserTestAccounts.ADMIN_EMAIL));
    page.locator("#two-factor-action-confirm").click();
    assertThat(page.locator("#two-factor-recovery-codes")).isVisible();
    assertThat(page.locator("#two-factor-submit")).isDisabled();
    assertThat(page.locator("#two-factor-cancel")).isDisabled();
    String outputPage = page.url();
    String url = page.locator("#two-factor-download-pdf").getAttribute("href");
    @SuppressWarnings("unchecked")
    Map<String, Object> pdf =
        (Map<String, Object>)
            page.evaluate(
                "async url => { const response = await fetch(url, {cache:'no-store'}); return {status:response.status, cache:response.headers.get('cache-control'), size:(await response.arrayBuffer()).byteLength}; }",
                url);
    org.junit.jupiter.api.Assertions.assertEquals(200, ((Number) pdf.get("status")).intValue());
    org.junit.jupiter.api.Assertions.assertTrue(((String) pdf.get("cache")).contains("no-store"));
    org.junit.jupiter.api.Assertions.assertTrue(((Number) pdf.get("size")).intValue() > 1000);
    TwoFactorPage.captureCodes(page, BrowserTestAccounts.ADMIN_EMAIL);
    String originalCodes =
        page.frameLocator("#two-factor-recovery-codes")
            .locator("#recovery-code-values")
            .innerText();
    // Ein tatsächlich nicht verfügbarer Codesatz wird vom geschützten Serverendpunkt
    // abgelehnt. Die native Downloadaktion muss auch dabei die sichtbare Ausgabe erhalten.
    String unavailable = "/internal/two-factor/recovery/" + UUID.randomUUID() + "/codes.pdf";
    page.locator("#two-factor-download-pdf")
        .evaluate("(anchor, href) => anchor.href = href", unavailable);
    try {
      com.microsoft.playwright.Download failed =
          page.waitForDownload(() -> page.locator("#two-factor-download-pdf").click());
      org.junit.jupiter.api.Assertions.assertNotNull(failed.failure());
    } finally {
      page.locator("#two-factor-download-pdf")
          .evaluate("(anchor, href) => anchor.href = href", url);
    }
    assertThat(page).hasURL(outputPage);
    assertThat(page.frameLocator("#two-factor-recovery-codes").locator("#recovery-code-values"))
        .hasText(originalCodes);
    assertThat(page.locator("#two-factor-submit")).isDisabled();
    com.microsoft.playwright.Download download =
        page.waitForDownload(() -> page.locator("#two-factor-download-pdf").click());
    org.junit.jupiter.api.Assertions.assertNull(download.failure());
    byte[] bytes = java.nio.file.Files.readAllBytes(download.path());
    assertTrue(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"));
    assertThat(page).hasURL(outputPage);
    assertThat(page.frameLocator("#two-factor-recovery-codes").locator("#recovery-code-values"))
        .hasText(originalCodes);
    AllurePlaywright.attachScreenshot("Einmalige Ausgabe synthetischer Ersatzcodes", page);
    page.locator("#two-factor-codes-saved input").setChecked(true);
    page.locator("#two-factor-submit").click();
    assertThat(page).hasURL(Pattern.compile(".*/account\\?tab=security$"));
    org.junit.jupiter.api.Assertions.assertTrue(
        ((Number) page.evaluate("async url => (await fetch(url, {cache:'no-store'})).status", url))
                .intValue()
            >= 400);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-03: Sicherheitskonfiguration nur für Superadmins mit nativen Rollenregeln und Feldhilfe")
  void superadminOnlyConfiguration(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    page.navigate("/admin?section=configuration&group=two_factor");
    assertThat(page.locator("#two-factor-save")).isVisible();
    assertThat(page.locator("#two-factor-role-tabs vaadin-tab")).hasCount(4);
    assertThat(
            page.getByText(
                texts.getTwoFactorDevelopmentTitle(), new Page.GetByTextOptions().setExact(true)))
        .isHidden();
    AllurePlaywright.attachScreenshot("Superadmin-Regeln für Zwei-Faktor-Authentifizierung", page);
    try (BrowserActor administrator = new BrowserActor(browser)) {
      administrator.login(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      administrator.page.navigate("/admin?section=configuration&group=two_factor");
      assertThat(administrator.page.locator("#two-factor-save")).hasCount(0);
      assertThat(administrator.page.locator("#admin-nav-configuration-two_factor")).isHidden();
      administrator.page.navigate("/admin?section=configuration&group=external_login");
      assertThat(administrator.page.locator("#admin-provider-settings")).hasCount(0);
      new UserMenu(administrator.page).signOut();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "MFA-04: Hilfszugang ohne zweiten Faktor zeigt nur eigene Wiederherstellungsanfragen")
  void restrictedRecoveryHelp(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signInFirstFactor(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    Locator help = page.locator("#two-factor-help");
    assertThat(help).isVisible();
    assertTrue(
        (Boolean)
            help.evaluate(
                "button => { const box = button.getBoundingClientRect(); const area = button.parentElement.getBoundingClientRect(); const field = document.getElementById('two-factor-code').getBoundingClientRect(); return box.top >= field.bottom + 12 && box.left >= area.left - 1 && box.right <= area.right + 1 && button.scrollWidth <= button.clientWidth + 1; }"),
        "Der übersetzte Hilfebutton muss innerhalb des Inhalts bleiben.");
    AllurePlaywright.attachScreenshot("Zwei-Faktor-Hilfe ohne überbreiten Button", page);
    // Der erste Fall hat dieses feste Testkonto eingerichtet. Im getrennten Hilfszugang
    // bleiben Authenticator und Backupcodes absichtlich unbenutzt.
    page.navigate("/recovery-help");
    assertThat(page.locator("#account-support")).isVisible();
    page.locator("#support-reply textarea")
        .fill("Synthetische Anfrage: Ich finde meine gesicherten Ersatzcodes nicht.");
    if (page.locator("#support-create").isVisible()) {
      page.locator("#support-create").click();
      assertThat(page.locator("#support-ticket-grid"))
          .containsText(texts.getSupportRecoverySubject());
    }
    AllurePlaywright.attachScreenshot("Eingeschränkte eigene Wiederherstellungsanfrage", page);
    page.navigate("/admin");
    assertThat(page).hasURL(Pattern.compile(".*/two-factor.*"));
    page.locator("#two-factor-cancel").click();
    assertThat(page).hasURL(Pattern.compile(".*/login$"));
  }
}
