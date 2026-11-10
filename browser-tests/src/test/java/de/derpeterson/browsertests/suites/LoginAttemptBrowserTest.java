package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.microsoft.playwright.Page;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.mail.MailpitInbox.CapturedMail;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Anmeldeversuche begrenzen")
public class LoginAttemptBrowserTest extends BrowserFixture {
  private AdminConfigurationPage settings;
  private String original;

  @BeforeEach
  void configureLimit() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    settings = new AdminConfigurationPage(browserPage);
    settings.open(AdminConfigGroup.SECURITY);
    original = settings.value(uiTexts.getAdminConfigEntryLoginAttemptsLimit());
  }

  @AfterEach
  void restoreLimit() {
    if (original != null) {
      settings.open(AdminConfigGroup.SECURITY);
      settings.enter(uiTexts.getAdminConfigEntryLoginAttemptsLimit(), original);
      settings.save();
    }
  }

  @Test
  @DisplayName("AUTH-LIMIT-01: Passwortsperre erhält eine bereits angemeldete Sitzung")
  @Description(
      """
      1. Eigenes aktives Benutzerkonto über das Adminformular anlegen; Versuchslimit 2 speichern.
      2. In einem unabhängigen Kontext mit dem richtigen Passwort anmelden.
      3. In einem weiteren Kontext zweimal falsch anmelden; dritter Versuch mit richtigem Passwort wird gedrosselt.
      4. Ohne Anmeldung und Remember-me-Cookie bleiben; bestehende Sitzung nach Neuladen weiterhin angemeldet.
      **Erwartet:** Serverseitige Sperre nach dem gespeicherten Limit; keine nachträgliche Sperre des aktiven Geräts.
      """)
  void blockedPasswordDoesNotInvalidateAnExistingSession() {
    String email = createUser();
    limit(2);
    try (BrowserActor existing = new BrowserActor(testBrowser);
        BrowserActor attacker = new BrowserActor(testBrowser)) {
      existing.login(email, BrowserTestAccounts.USER_PASSWORD, true);

      assertThat(existing.browserPage).hasURL(urlPattern(HOME_ROUTE));
      attacker.login(email, "WrongPassword!", true);
      LoginPage login = new LoginPage(attacker.browserPage);
      login.assertRejected();
      login.dismissFailure();
      login.signIn(email, "WrongPassword!");
      login.assertRejected();
      login.dismissFailure();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      login.assertThrottled();

      assertFalse(
          attacker.browserContext.cookies().stream()
              .anyMatch(cookie -> cookie.name.equals("remember-me")));
      attacker.browserPage.navigate(path(LOGIN_ROUTE) + "?error=throttled");
      login.assertThrottled();
      existing.browserPage.reload();
      new UserMenu(existing.browserPage).selectTestLanguage();
      new UserMenu(existing.browserPage).assertUserSignedInWithoutAdminAction(email);
    }
  }

  @Test
  @DisplayName(
      "AUTH-LIMIT-02: Gespeichertes kleineres Limit gilt sofort für bekannte und unbekannte Kennungen")
  @Description(
      """
      1. Eigenes Konto anlegen und Versuchslimit 3 über die Oberfläche speichern.
      2. Für dieses Konto und eine neue unbekannte Adresse je einen falschen Versuch absenden.
      3. Im weiterhin angemeldeten Adminkontext das Limit auf 1 reduzieren.
      4. Erneute Versuche erhalten dieselbe neutrale Sperrmeldung, ohne Kontoauskunft.
      **Erwartet:** Änderung wirkt ohne Neustart, auch ein richtiges Passwort umgeht die bestehende Grenze nicht.
      """)
  void lowerLimitAppliesImmediatelyAndDoesNotExposeAccountExistence() {
    String email = createUser();
    String unknown = "unknown-limit-" + UUID.randomUUID() + "@example.test";
    limit(3);
    try (BrowserActor known = new BrowserActor(testBrowser);
        BrowserActor missing = new BrowserActor(testBrowser)) {
      known.login(email, "WrongPassword!");
      LoginPage first = new LoginPage(known.browserPage);
      first.assertRejected();
      first.dismissFailure();
      missing.login(unknown, "WrongPassword!");
      LoginPage second = new LoginPage(missing.browserPage);
      second.assertRejected();
      second.dismissFailure();
      limit(1);
      first.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      first.assertThrottled();
      second.signIn(unknown, "WrongPassword!");
      second.assertThrottled();
    }
  }

  @Test
  @DisplayName(
      "AUTH-LIMIT-03: Erst der abgeschlossene echte Passwortreset hebt die Anmeldesperre auf")
  @Description(
      """
      1. Eigenes aktives Konto anlegen; Versuchslimit 1 speichern und Passwortanmeldung sperren.
      2. Über Passwort vergessen eine echte Resetmail anfordern. Richtiger Login bleibt gesperrt.
      3. Sichtbaren Link der eigenen Mailpit-Mail öffnen und neues Passwort samt Bestätigung speichern.
      4. Mit dem neuen Passwort über das sichtbare Formular anmelden.
      **Erwartet:** Anfrage allein entsperrt nicht; erfolgreicher Token-/Passwortwechsel setzt den Zähler zurück.
      """)
  void completedMailPasswordResetClearsTheBlock() {
    String email = createUser();
    limit(1);
    try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
      browserActor.login(email, "WrongPassword!");
      LoginPage login = new LoginPage(browserActor.browserPage);
      login.assertRejected();
      login.dismissFailure();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      login.assertThrottled();
      login.dismissFailure();
      ForgotPasswordPage forgotPasswordPage = new ForgotPasswordPage(browserActor.browserPage);
      forgotPasswordPage.open();
      forgotPasswordPage.enterEmail(email);
      forgotPasswordPage.submit();
      forgotPasswordPage.requestedResponse();
      forgotPasswordPage.dismissRequestedNotice();
      MailpitInbox inbox = new MailpitInbox(browserActor.browserContext);
      CapturedMail mail = inbox.awaitPasswordResetMail(email);
      login.open();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      login.assertThrottled();
      login.dismissFailure();
      Page resetPage = inbox.openPasswordResetLink(mail);
      ResetPasswordPage reset = new ResetPasswordPage(resetPage);
      reset.assertFormVisible();
      reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
      reset.submit();
      reset.assertChanged();
      LoginPage renewed = new LoginPage(resetPage);
      renewed.open();
      renewed.signIn(email, BrowserTestAccounts.RESET_PASSWORD);

      assertThat(resetPage).hasURL(urlPattern(HOME_ROUTE));
      new UserMenu(resetPage).assertUserSignedInWithoutAdminAction(email);
    }
  }

  private String createUser() {
    String email = "attempt-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }

  private void limit(int value) {
    settings.open(AdminConfigGroup.SECURITY);
    settings.enter(uiTexts.getAdminConfigEntryLoginAttemptsLimit(), String.valueOf(value));
    settings.save();
  }
}
