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

  @Test
  @Tag("ticket-layout")
  @DisplayName("MFA-19: Ticketliste, Panel-Tabs und Auswahlhinweis passen zur verfügbaren Breite")
  void administrativeTicketLayoutKeepsTabsAndCardsUnclipped() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/recovery-help");
    page.locator("#support-reply textarea")
        .fill("Synthetische erste Anfrage für die Layoutprüfung.");
    UiActions.complete(page, () -> page.locator("#support-create").click());
    UiActions.complete(page, () -> page.locator("#support-close").click());
    page.locator("#support-reply textarea")
        .fill("Synthetische offene Anfrage für die Layoutprüfung.");
    UiActions.complete(page, () -> page.locator("#support-create").click());
    page.navigate("/admin?section=support");
    Locator categories = page.locator("#support-ticket-categories");
    Locator hint = page.locator("#support-select-ticket");
    assertThat(hint).containsText(texts.getSupportSelectTicket());
    assertThat(hint.locator("[data-lucide=list]")).isVisible();
    UiActions.complete(page, () -> categories.locator("vaadin-tab").nth(1).click());
    assertThat(hint.locator("[data-lucide=list]")).isVisible();
    assertThat(page.locator(".support-admin-ticket-card")).hasCount(2);
    for (int width : new int[] {1600, 1280, 1024, 390}) {
      page.setViewportSize(width, 1000);
      page.waitForCondition(
          () ->
              (Boolean)
                  page.evaluate(
                      """
                      () => {
                        const body = document.querySelector('#support-panel > .account-panel-body').getBoundingClientRect();
                        const tabs = document.querySelector('#support-ticket-categories').getBoundingClientRect();
                        const queue = document.querySelector('.support-admin-queue').getBoundingClientRect();
                        const detail = document.querySelector('.support-admin-detail-column').getBoundingClientRect();
                        const hint = document.querySelector('#support-select-ticket').getBoundingClientRect();
                        const tabItems = [...document.querySelectorAll('#support-ticket-categories vaadin-tab')];
                        const tabsFit = tabs.left >= body.left - 1 && tabs.right <= body.right + 1
                            && tabs.height >= 48 && tabItems.every(item => {
                              const rect = item.getBoundingClientRect();
                              return rect.top >= tabs.top - 1 && rect.bottom <= tabs.bottom + 1;
                            });
                        const columnsFit = body.width >= 820
                            ? queue.width >= 370 && detail.left >= queue.right - 1
                            : detail.top >= queue.bottom - 1 && Math.abs(detail.width - queue.width) < 2;
                        const centered = Math.abs(hint.left + hint.width / 2 - detail.left - detail.width / 2) < 2
                            && Math.abs(hint.top + hint.height / 2 - detail.top - detail.height / 2) < 2;
                        return tabsFit && columnsFit && centered
                            && document.documentElement.scrollWidth <= window.innerWidth;
                      }
                      """));
      AllurePlaywright.attachScreenshot(
          "Ticketübersicht und Auswahlhinweis bei " + width + "px", page);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("setup-screen")
  @DisplayName(
      "MFA-17: Verpflichtender Einstieg A nutzt die frische Passwortanmeldung ohne zweite Abfrage")
  void requiredIntroductionReusesFreshPasswordWithoutGrantingPortalAccess(int width) {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
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
      assertThat(actor.page.locator(".public-header")).isVisible();
      actor.page.navigate("/account");
      assertThat(actor.page).hasURL(Pattern.compile(".*/login.*"));
      new UserMenu(actor.page).selectTestLanguage();
      fresh.assertFormVisible();
      fresh.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
      actor.page.reload();
      assertThat(actor.page.locator("#two-factor-introduction-title")).isVisible();
      actor.page.navigate("/admin");
      assertThat(actor.page).hasURL(Pattern.compile(".*/two-factor.*"));
      actor.page.locator("#two-factor-submit").click();
      assertThat(actor.page.locator("#two-factor-qr")).isVisible();
      assertThat(actor.page.locator("#two-factor-password")).hasCount(0);
      assertEnrollmentControls(actor.page);
      TwoFactorPage.enrollCurrent(actor.page, email);
      assertThat(actor.page).hasURL(Pattern.compile(".*/admin$"));
    }
  }

  private void assertEnrollmentControls(Page enrollment) {
    assertThat(enrollment.locator(".two-factor-method-tabs")).hasCSS("height", "42px");
    assertThat(enrollment.locator(".two-factor-app-links a")).hasCount(3);
    assertThat(enrollment.locator(".two-factor-app-compatibility"))
        .hasText(texts.getTwoFactorAppCompatibility());
    assertThat(enrollment.locator("#two-factor-qr")).hasJSProperty("complete", true);
    assertTrue(
        (Boolean)
            enrollment
                .locator("#two-factor-qr-pane")
                .evaluate(
                    """
                pane => {
                  const image = pane.querySelector('img');
                  const bounds = pane.getBoundingClientRect();
                  const qr = image.getBoundingClientRect();
                  return image.complete && image.naturalWidth > 0
                    && Math.abs(qr.x + qr.width / 2 - bounds.x - bounds.width / 2) <= 1;
                }
                """));
    enrollment.locator("#two-factor-manual-tab").click();
    Locator manual = enrollment.locator("#two-factor-manual-key");
    Locator manualInput = manual.locator("textarea");
    // Genau zwei Zeilen aus Base32-Zeichen und Gruppierungsleerzeichen zulassen.
    assertThat(manualInput).hasValue(Pattern.compile("[A-Z2-7 ]+\\n[A-Z2-7 ]+"));
    assertThat(manual).hasJSProperty("value", "");
    assertThat(manualInput).hasAttribute("readonly", "");
    String formattedKey = manualInput.inputValue();
    manualInput.focus();
    assertThat(manual).hasJSProperty("value", "");
    assertThat(manual.locator("#two-factor-copy-key")).hasCSS("color", "rgb(56, 189, 248)");
    enrollment.context().grantPermissions(java.util.List.of("clipboard-read", "clipboard-write"));
    manual.locator("#two-factor-copy-key").click();
    assertThat(
            enrollment
                .locator("vaadin-notification-card:not([closing])")
                .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorManualCopied())))
        .isVisible();
    org.junit.jupiter.api.Assertions.assertEquals(
        formattedKey.replace(" ", "").replace("\n", ""),
        enrollment.evaluate("navigator.clipboard.readText()"));
    TwoFactorPage.assertCopyFailure(
        enrollment, manual.locator("#two-factor-copy-key"), texts.getTwoFactorManualCopyFailed());
    assertThat(manualInput).hasValue(formattedKey);
    assertThat(manual).hasJSProperty("value", "");
    UiTexts other = new UiTexts(texts.language().equals("de") ? "en" : "de");
    new UserMenu(enrollment).selectLanguage(other.language());
    assertThat(enrollment.locator("#two-factor-manual-tab")).hasAttribute("selected", "");
    assertThat(manual).hasJSProperty("label", other.getTwoFactorManualLabel());
    assertThat(manualInput).hasValue(formattedKey);
    assertThat(manual).hasJSProperty("value", "");
    new UserMenu(enrollment).selectTestLanguage();
    assertThat(manualInput).hasValue(formattedKey);
    AllurePlaywright.attachScreenshot(
        "Manueller Einrichtungsschlüssel nach Sprachwechsel", enrollment);
    enrollment.locator("#two-factor-qr-tab").click();
    assertThat(enrollment.locator("#two-factor-qr")).isVisible();
    assertThat(enrollment.locator("#two-factor-qr")).hasJSProperty("complete", true);
    AllurePlaywright.attachScreenshot("QR-Einrichtung mit nativer OTP-Eingabe", enrollment);
    assertOtpInput(enrollment);
    AllurePlaywright.attachScreenshot("Fehlermarkierung bei leerem Authenticator-Code", enrollment);
  }

  private void assertOtpInput(Page enrollment) {
    Locator otp = enrollment.locator("#two-factor-enrollment-code");
    Locator slots = otp.locator("[part=slot]");
    Locator input = otp.locator("input");
    assertThat(slots).hasCount(6);
    assertThat(slots).hasText(new String[] {"•", "•", "•", "•", "•", "•"});
    assertThat(otp.locator("[part=slot][active]:not([filled])")).hasCount(1);
    assertThat(input).not().isFocused();
    input.pressSequentially("12");
    assertThat(otp.locator("[part=slot][filled]")).hasCount(2);
    assertThat(slots.nth(2)).hasAttribute("active", "");
    input.press("Backspace");
    assertThat(input).hasValue("1");
    assertThat(slots.nth(1)).hasAttribute("active", "");
    input.press("ArrowLeft");
    assertThat(slots.first()).hasAttribute("active", "");
    input.press("ArrowRight");
    assertThat(slots.nth(1)).hasAttribute("active", "");
    // Auch eingefügte, vom Add-on auf sechs Ziffern begrenzte Codes werden sofort geprüft.
    for (String copiedCode : new String[] {"1234567", "123 456 7"}) {
      enrollment.evaluate("value => navigator.clipboard.writeText(value)", copiedCode);
      input.press("ControlOrMeta+A");
      input.press("ControlOrMeta+V");
      assertThat(otp).hasAttribute("invalid", "");
      assertThat(input).hasValue("");
      assertThat(
              enrollment
                  .locator("vaadin-notification-card:not([closing])")
                  .filter(new Locator.FilterOptions().setHasText(texts.getTwoFactorInvalidCode())))
          .isVisible();
      assertThat(input).hasCSS("color", "rgba(0, 0, 0, 0)");
      assertTrue(
          (Boolean)
              enrollment.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    }
    input.fill("");
    enrollment.locator("#two-factor-submit").click();
    assertThat(otp).hasAttribute("invalid", "");
    assertThat(
            enrollment
                .locator("vaadin-notification-card:not([closing])")
                .filter(
                    new Locator.FilterOptions()
                        .setHasText(texts.getBaseValidationRequiredMessage())))
        .isVisible();
    assertThat(enrollment.locator("#two-factor-qr")).isVisible();
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
    TwoFactorPage.openLoginHelp(page);
    page.locator("#two-factor-recover").click();
    assertThat(page.locator("#two-factor-panel .cosmos-panel-header"))
        .containsText(texts.getTwoFactorRecoverSubtitle());
    TwoFactorPage.enterCode(page, last);
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
    TwoFactorPage.enterCode(page, last);
    page.locator("#two-factor-submit").click();
    assertThat(page.locator("#two-factor-code")).hasAttribute("invalid", "");
    assertThat(page.locator("vaadin-notification-card:not([closing])"))
        .containsText(texts.getTwoFactorInvalidCode());
    String nextCode = TwoFactorPage.nextCode(page, email);
    TwoFactorPage.enterCode(page, nextCode);
    if (nextCode.length() > 6) {
      page.locator("#two-factor-submit").click();
    }
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
    TwoFactorPage.selectTestLanguageIfNeeded(page);
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
      "MFA-07: Admin übernimmt eine eingeschränkte Hilfsanfrage und sendet die geprüfte Anleitung")
  @Tag("recovery-instructions")
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
      Locator ticketCategories = admin.locator("#support-ticket-categories");
      assertThat(ticketCategories).containsText(texts.getSupportAllTickets());
      UiActions.complete(
          admin, () -> ticketCategories.getByText(texts.getSupportRecoveryCategory()).click());
      Locator ticket =
          admin
              .locator("#support-ticket-grid")
              .getByText(ticketNumber, new Locator.GetByTextOptions().setExact(true));
      UiActions.complete(admin, ticket::click);
      assertThat(admin.locator("#admin-support .support-message[data-side=other]"))
          .containsText("Synthetische Bitte um Hilfe mit meinen Ersatzcodes.");
      UiActions.complete(admin, () -> admin.locator("#support-claim").click());
      assertThat(page.locator("#support-ticket-grid")).containsText(texts.getSupportInProgress());
      assertThat(admin.locator("#admin-support .cosmos-security-warning")).isVisible();
      assertThat(admin.locator("#support-add-reply")).isVisible();
      String draft = "Eine Antwort bleibt beim Aktualisieren erhalten.";
      String followUp = "Eine synthetische Ergänzung zur Anfrage.";
      String privateNote = "Synthetischer interner Vermerk.";
      String publicReply = "Eine sichtbare Rückmeldung aus der Administration.";
      Locator adminComposer = admin.locator("#support-reply textarea");
      adminComposer.fill(draft);
      page.locator("#support-reply textarea").fill(followUp);
      UiActions.complete(page, () -> page.locator("#support-add-reply").click());
      assertThat(admin.locator(".support-message-thread")).containsText(followUp);
      assertThat(adminComposer).hasValue(draft);
      adminComposer.fill(privateNote);
      admin.locator("#support-internal-note").click();
      UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());
      assertThat(admin.locator(".support-internal-message")).containsText(privateNote);
      adminComposer.fill(publicReply);
      UiActions.complete(admin, () -> admin.locator("#support-add-reply").click());
      assertThat(page.locator(".support-message-thread")).containsText(publicReply);
      assertThat(page.locator(".support-message-thread")).not().containsText(privateNote);
      AllurePlaywright.attachScreenshot("Admin-Tickets mit ausgewählter Anfrage", admin);
      admin.locator("#support-instructions").click();
      assertThat(admin.locator("#support-instruction-preview")).hasJSProperty("opened", true);
      assertThat(admin.locator("#support-instruction-preview"))
          .containsText(texts.getSupportRecoveryInstructions().lines().findFirst().orElseThrow());
      assertThat(admin.locator("#support-instruction-preview .support-instruction-step"))
          .hasCount(4);
      assertThat(admin.locator("#support-instruction-preview .cosmos-security-warning"))
          .isVisible();
      admin.evaluate(
          "async () => { await Promise.all(document.getAnimations().filter(animation => animation.effect?.getTiming().iterations !== Infinity).map(animation => animation.finished.catch(() => {}))); }");
      if (width == 390) {
        admin.setViewportSize(width, 1200);
      }
      AllurePlaywright.attachScreenshot("Geprüfte Hilfeantwort ohne neue Zugangscodes", admin);
      admin.setViewportSize(width, 900);
      admin.locator("#support-instruction-preview-submit").click();
      assertThat(admin.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getSupportQueued());
      new MailpitInbox(administrator.context)
          .awaitSecurityMail(email, texts.getSupportRecoverySubject());
      Locator guide = page.locator(".support-instruction-message");
      assertThat(guide).containsText(texts.getSupportInstructionMessageTitle());
      assertThat(guide.locator(".support-instruction-details")).not().hasAttribute("opened", "");
      guide.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot("Anleitung kompakt im Nachrichtenverlauf", page);
      UiActions.complete(page, () -> guide.locator(".support-instruction-toggle").click());
      assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
      assertThat(guide.locator(".support-instruction-step")).hasCount(4);
      assertThat(guide.locator(".cosmos-security-warning")).isVisible();
      UiActions.complete(page, () -> page.locator("#support-reload").click());
      assertThat(guide.locator(".support-instruction-details")).hasAttribute("opened", "");
      guide.scrollIntoViewIfNeeded();
      AllurePlaywright.attachScreenshot("Anleitung aufgeklappt im Nachrichtenverlauf", page);
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
    assertThat(page.locator("#two-factor-recovery-toggle")).isVisible();
    java.time.Instant shortProofExpired = java.time.Instant.now().plusSeconds(301);
    assertThat(page.locator("#two-factor-recovery-codes")).isVisible();
    page.locator("#two-factor-recovery-toggle").click();
    assertThat(page.locator("#two-factor-recovery-codes")).isHidden();
    assertThat(page.locator("#two-factor-submit")).isDisabled();
    assertThat(page.locator("#two-factor-cancel")).isHidden();
    String outputPage = page.url();
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                    .setName(texts.getTwoFactorDownloadPdf())
                    .setExact(true)))
        .isVisible();
    String url = page.locator("#two-factor-download-pdf").getAttribute("data-download-url");
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
    java.util.List<String> originalCodes = TwoFactorPage.recoveryCodeValues(page);
    for (int index = 1; index <= 10; index++) {
      assertThat(page.locator("#two-factor-recovery-code-" + index)).hasJSProperty("value", "");
      assertThat(page.locator("#two-factor-recovery-code-" + index + " input"))
          .hasAttribute("readonly", "");
    }
    // Ein tatsächlich nicht verfügbarer Codesatz wird vom geschützten Serverendpunkt
    // abgelehnt. Die native Downloadaktion muss auch dabei die sichtbare Ausgabe erhalten.
    String unavailable = "/internal/two-factor/recovery/" + UUID.randomUUID() + "/codes.pdf";
    page.locator("#two-factor-download-pdf")
        .evaluate("(button, url) => button.setAttribute('data-download-url', url)", unavailable);
    java.util.List<com.microsoft.playwright.Download> failedDownloads = new java.util.ArrayList<>();
    java.util.function.Consumer<com.microsoft.playwright.Download> captureDownload =
        failedDownloads::add;
    page.onDownload(captureDownload);
    try {
      page.locator("#two-factor-download-pdf").click();
      assertThat(
              page.getByText(
                  texts.getTwoFactorDownloadFailed(), new Page.GetByTextOptions().setExact(true)))
          .isVisible();
      assertTrue(failedDownloads.isEmpty(), "Error responses must not offer a file download");
    } finally {
      page.offDownload(captureDownload);
      page.locator("#two-factor-download-pdf")
          .evaluate("(button, url) => button.setAttribute('data-download-url', url)", url);
    }
    assertThat(page).hasURL(outputPage);
    org.junit.jupiter.api.Assertions.assertEquals(
        originalCodes, TwoFactorPage.recoveryCodeValues(page));
    assertThat(page.locator("#two-factor-submit")).isDisabled();
    com.microsoft.playwright.Download download =
        page.waitForDownload(() -> page.locator("#two-factor-download-pdf").click());
    org.junit.jupiter.api.Assertions.assertNull(download.failure());
    byte[] bytes = java.nio.file.Files.readAllBytes(download.path());
    assertTrue(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"));
    assertThat(page).hasURL(outputPage);
    org.junit.jupiter.api.Assertions.assertEquals(
        originalCodes, TwoFactorPage.recoveryCodeValues(page));
    if (width == 1280) {
      // Die echte Fünf-Minuten-Frist darf nur den kurzen Sicherheitsnachweis beenden,
      // nicht die noch unbestätigte sitzungsgebundene Codeausgabe oder deren PDF.
      page.waitForCondition(
          () -> java.time.Instant.now().isAfter(shortProofExpired),
          new Page.WaitForConditionOptions().setTimeout(310_000));
      page.reload();
      org.junit.jupiter.api.Assertions.assertEquals(
          originalCodes, TwoFactorPage.recoveryCodeValues(page));
      com.microsoft.playwright.Download laterDownload =
          page.waitForDownload(() -> page.locator("#two-factor-download-pdf").click());
      org.junit.jupiter.api.Assertions.assertNull(laterDownload.failure());
      byte[] laterBytes = java.nio.file.Files.readAllBytes(laterDownload.path());
      assertTrue(
          new String(laterBytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"));
      assertThat(page).hasURL(outputPage);
    }
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
  @Tag("two-factor-configuration")
  void superadminOnlyConfiguration(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    page.navigate("/admin?section=configuration&group=two_factor");
    assertThat(page.locator("#two-factor-save")).isVisible();
    Locator tabs = page.locator("#two-factor-role-tabs vaadin-tab");
    assertThat(tabs).hasCount(4);
    Locator roleContent = page.locator(".mfa-role-content:visible");
    assertThat(roleContent.locator("vaadin-select")).hasCount(9);
    assertThat(roleContent.locator(".mfa-config-heading").first())
        .hasText(texts.getTwoFactorAccessGroup());
    Locator guide = roleContent.locator(".mfa-role-guide");
    assertThat(guide).containsText(texts.getTwoFactorRoleScope(texts.getRoleSuperadmin()));
    assertTrue(
        (Boolean)
            guide.evaluate(
                """
                (guide, desktop) => {
                  const fields = guide.parentElement.firstElementChild.getBoundingClientRect();
                  const help = guide.getBoundingClientRect();
                  return desktop ? help.left >= fields.right : help.top >= fields.bottom;
                }
                """,
                width == 1280));
    page.locator("#two-factor-rule-role_superadmin-administration + .cosmos-config-info").click();
    Locator help = page.locator("#two-factor-field-help");
    Locator helpOverlay = help.locator("vaadin-dialog-overlay > [part~=overlay]");
    assertThat(helpOverlay).isVisible();
    assertThat(help).containsText(texts.getTwoFactorAdminAccessHelp());
    page.keyboard().press("Escape");
    assertThat(helpOverlay).isHidden();
    tabs.nth(2).click();
    assertThat(guide).containsText(texts.getTwoFactorRoleScope(texts.getRoleUser()));
    assertThat(page.locator("#two-factor-inactivity-enabled")).isVisible();
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    assertThat(
            page.getByText(
                texts.getTwoFactorDevelopmentTitle(), new Page.GetByTextOptions().setExact(true)))
        .isHidden();
    AllurePlaywright.attachScreenshot("Superadmin-Regeln für Zwei-Faktor-Authentifizierung", page);
    try (BrowserActor administrator = new BrowserActor(browser)) {
      administrator.login(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      new UserMenu(administrator.page).selectTestLanguage();
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
  @Tag("confirmation-screens")
  @DisplayName("MFA-18: Codebestätigung, geschützter Faktorwechsel und Hilfe folgen Variante A")
  void confirmationRecoveryAndHelpUseNativeControls(int width) {
    page.setViewportSize(width, 900);
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "confirmation-mfa-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createUser(email, BrowserTestAccounts.USER_PASSWORD, true, true);
    new UserMenu(page).signOut();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    new UserMenu(page).signOut();
    login.signInFirstFactor(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    assertThat(page.locator("#two-factor-code")).hasJSProperty("length", 6);
    assertThat(page.locator("#two-factor-code [part=slot]")).hasCount(6);
    page.locator("#two-factor-code input").fill("12345");
    assertThat(page.locator("#two-factor-code [part=slot]"))
        .hasText(new String[] {"1", "2", "3", "4", "5", "•"});
    page.locator("#two-factor-recovery-tab").click();
    String recoveryCode = "12345678-12345678-12345678-12345678";
    TwoFactorPage.enterCode(page, recoveryCode);
    assertThat(page.locator("#two-factor-code input")).hasValue(recoveryCode);
    UiTexts other = new UiTexts(texts.language().equals("de") ? "en" : "de");
    new UserMenu(page).selectLanguage(other.language());
    assertThat(page.locator("#two-factor-recovery-tab")).hasAttribute("selected", "");
    assertThat(page.locator("#two-factor-code"))
        .hasJSProperty("label", other.getTwoFactorRecoveryCodeLabel());
    new UserMenu(page).selectTestLanguage();
    page.locator("#two-factor-authenticator-tab").click();
    assertThat(page.locator("#two-factor-code [part=slot]")).hasCount(6);
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    AllurePlaywright.attachScreenshot("Anmeldung bestätigen mit nativer Methodenauswahl", page);
    assertThat(page.locator("#two-factor-recover, #two-factor-help").first()).isHidden();
    TwoFactorPage.openLoginHelp(page);
    page.locator("#two-factor-recover").click();
    assertThat(page.locator("vaadin-text-field#two-factor-code input"))
        .hasAttribute("maxlength", "35");
    assertThat(page.locator(".cosmos-panel-footer #two-factor-recovery-back"))
        .hasText(texts.getTwoFactorReturnToCode());
    assertThat(page.locator("#two-factor-help")).isHidden();
    assertThat(page.locator("#two-factor-panel"))
        .containsText(texts.getTwoFactorRecoveryConsumptionHint());
    AllurePlaywright.attachScreenshot(
        "Authenticator mit Ersatzcode geschützt wiederherstellen", page);
    page.locator("#two-factor-recovery-back").click();
    assertThat(page).hasURL(Pattern.compile(".*/two-factor$"));
    assertThat(page.locator("#two-factor-authenticator-tab")).isVisible();
    TwoFactorPage.openLoginHelp(page);
    page.locator("#two-factor-help").click();
    assertThat(page.locator("#account-support")).containsText(texts.getSupportEmpty());
    assertThat(page.locator(".cosmos-panel-footer #support-back")).isVisible();
    assertThat(page.locator("#support-ticket-grid")).isHidden();
    String draft = "Synthetische Bitte um Hilfe zu meinen gesicherten Ersatzcodes.";
    page.locator("#support-reply textarea").fill(draft);
    new UserMenu(page).selectLanguage(other.language());
    assertThat(page.locator("#support-reply textarea")).hasValue(draft);
    new UserMenu(page).selectTestLanguage();
    AllurePlaywright.attachScreenshot(
        "Hilfeanfrage mit Leerzustand und nativen Footeraktionen", page);
    page.locator("#support-create").click();
    assertThat(page.locator("#support-ticket-grid"))
        .containsText(texts.getSupportRecoverySubject());
    assertThat(page.locator("#support-add-reply")).isVisible();
    assertThat(page.locator("#support-back")).isVisible();
    assertTrue(
        (Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
    page.locator("#support-back").click();
    assertThat(page.locator("#two-factor-code")).isVisible();
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
    TwoFactorPage.openLoginHelp(page);
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
