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
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfigurationPage;
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
  private ConfigurationPage settings;
  private String original;

  @BeforeEach
  void configureLimit() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    settings = new ConfigurationPage(page);
    settings.open(AdminConfigGroup.SECURITY);
    original = settings.value(texts.getAdminConfigEntryLoginAttemptsLimit());
  }

  @AfterEach
  void restoreLimit() {
    if (original != null) {
      settings.open(AdminConfigGroup.SECURITY);
      settings.enter(texts.getAdminConfigEntryLoginAttemptsLimit(), original);
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
    try (BrowserActor existing = new BrowserActor(browser);
        BrowserActor attacker = new BrowserActor(browser)) {
      existing.login(email, BrowserTestAccounts.USER_PASSWORD, true);
      assertThat(existing.page).hasURL(urlPattern(HOME_ROUTE));
      attacker.login(email, "WrongPassword!", true);
      LoginPage login = new LoginPage(attacker.page);
      login.assertRejected();
      login.dismissFailure();
      login.signIn(email, "WrongPassword!");
      login.assertRejected();
      login.dismissFailure();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      login.assertThrottled();
      assertFalse(
          attacker.context.cookies().stream()
              .anyMatch(cookie -> cookie.name.equals("remember-me")));
      attacker.page.navigate(path(LOGIN_ROUTE) + "?error=throttled");
      login.assertThrottled();
      existing.page.reload();
      new UserMenu(existing.page).selectTestLanguage();
      new UserMenu(existing.page).assertUserSignedInWithoutAdminAction(email);
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
    try (BrowserActor known = new BrowserActor(browser);
        BrowserActor missing = new BrowserActor(browser)) {
      known.login(email, "WrongPassword!");
      LoginPage first = new LoginPage(known.page);
      first.assertRejected();
      first.dismissFailure();
      missing.login(unknown, "WrongPassword!");
      LoginPage second = new LoginPage(missing.page);
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
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.login(email, "WrongPassword!");
      LoginPage login = new LoginPage(actor.page);
      login.assertRejected();
      login.dismissFailure();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      login.assertThrottled();
      login.dismissFailure();
      ForgotPasswordPage request = new ForgotPasswordPage(actor.page);
      request.open();
      request.enterEmail(email);
      request.submit();
      request.requestedResponse();
      request.dismissRequestedNotice();
      MailpitInbox inbox = new MailpitInbox(actor.context);
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
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }

  private void limit(int value) {
    settings.open(AdminConfigGroup.SECURITY);
    settings.enter(texts.getAdminConfigEntryLoginAttemptsLimit(), String.valueOf(value));
    settings.save();
  }
}
