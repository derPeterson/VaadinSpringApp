package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import java.util.UUID;
import org.junit.jupiter.api.*;

@Tag("browser")
@Feature("Eigene Passwort- und verifizierte E-Mail-Änderung")
public class AccountSecurityBrowserTest extends BrowserFixture {
  private String account(boolean remember) {
    String email = "account-security-" + UUID.randomUUID() + "@example.test";
    var registration = new RegistrationPage(page);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
    var inbox = new MailpitInbox(context);
    try (Page activated = inbox.openVerificationLink(inbox.awaitVerificationMail(email))) {
      assertThat(activated).hasTitle(VERIFICATION_PAGE_TITEL);
    }
    var login = new LoginPage(page);
    login.open();
    page.getByRole(
            AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName(texts.getLoginRememberMeCheckbox()).setExact(true))
        .setChecked(remember);
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    assertThat(page).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(page).selectTestLanguage();
    var form = new AccountPage(page);
    form.open();
    // Sicherheitsmails folgen der gespeicherten Kontosprache, nicht nur dem aktuellen UI-Menü.
    form.language(texts.language());
    form.save();
    form.assertMessage(texts.getAccountSettingsSaveSuccess());
    form.closeNotification();
    return email;
  }

  @Test
  @DisplayName("ACCOUNT-SEC-01: Passwort erst beim Absenden validieren und Fehler korrigieren")
  @Description(
      """
      1. Eigenes Konto registrieren, über echte Mail aktivieren und anmelden.
      2. Passwortdialog öffnen: erster Buchstabe zeigt noch keine rote Servervalidierung.
      3. Leere Pflichtfelder, unsicheres Passwort, abweichende Bestätigung und falsches aktuelles Passwort absenden.
      4. Jeweilige Notification und passende rote Felder prüfen; Dialog bleibt offen.
      **Erwartet:** Keine vorzeitige Validierung, keine behauptete Speicherung bei Fehlern.
      """)
  void passwordValidationIsExplicitAndCorrectable() {
    account(false);
    var form = new AccountPage(page);
    var security = new AccountSecurityPage(page);
    security.openPassword();
    security.input("account-password-new").fill("x");
    assertThat(security.host("account-password-new")).hasJSProperty("invalid", false);
    assertThat(page.locator("vaadin-notification-card")).hasCount(0);
    security.savePassword();
    form.assertMessage(texts.getBaseValidationRequiredMessage());
    form.closeNotification();
    security.password("x", "x", BrowserTestAccounts.USER_PASSWORD);
    security.savePassword();
    form.assertMessage(texts.getBaseValidationPasswordInvalidMessage());
    assertThat(security.host("account-password-new")).hasJSProperty("invalid", true);
    form.closeNotification();
    security.password(
        "Changed!Password9", "Different!Password9", BrowserTestAccounts.USER_PASSWORD);
    security.savePassword();
    form.assertMessage(texts.getBaseValidationPasswordConfirmMessage());
    assertThat(security.host("account-password-confirm")).hasJSProperty("invalid", true);
    form.closeNotification();
    security.password("Changed!Password9", "Changed!Password9", "Wrong!Password9");
    security.savePassword();
    form.assertMessage(texts.getAccountSecurityWrongPassword());
    assertThat(security.host("account-password-current")).hasJSProperty("invalid", true);
    assertThat(page.locator("vaadin-dialog.account-password-dialog[opened]")).hasCount(1);
    assertThat(security.input("account-password-current")).isVisible();
    security.host("account-password-cancel").click();
  }

  @Test
  @DisplayName(
      "ACCOUNT-SEC-02: Passwort speichern, Sicherheitsmail empfangen und andere Sitzung widerrufen")
  @Description(
      """
      1. Eigenes verifiziertes Konto mit Remember-me und eine zweite unabhängige Remember-me-Sitzung anmelden.
      2. Passwort im Kontodialog ändern; Erfolg und erhaltene aktuelle Sitzung nach Reload prüfen.
      3. Echte lokalisierte Sicherheitsmail samt geladenem Inline-Logo/Footer und ohne Passwortinhalt prüfen.
      4. Zweite Sitzung darf nicht weiterarbeiten; altes Passwort schlägt fehl, neues Passwort funktioniert.
      **Erwartet:** Aktuelle Sitzung bleibt erhalten, andere Sitzungen/gespeicherte Anmeldungen sind widerrufen.
      """)
  void passwordChangeRetainsCurrentSessionAndSendsSecurityMail() {
    String email = account(true);
    String changed = "Changed!Password9";
    try (var other = new BrowserActor(browser)) {
      other.login(email, BrowserTestAccounts.USER_PASSWORD, true);
      assertThat(other.page).hasURL(urlPattern(HOME_ROUTE));
      var security = new AccountSecurityPage(page);
      security.openPassword();
      assertThat(page.locator("vaadin-dialog.account-password-dialog[opened]"))
          .containsText(texts.getAccountPasswordDialogHint());
      security.password(changed, changed, BrowserTestAccounts.USER_PASSWORD);
      security.savePassword();
      new AccountPage(page).assertMessage(texts.getAccountSecurityPasswordSuccess());
      assertThat(page.locator("vaadin-dialog.account-password-dialog[opened]")).hasCount(0);
      page.reload();
      // Nach Reload bleibt bewusst der Sicherheits-Tab ausgewählt; das Profil erst öffnen.
      UiActions.complete(page, () -> page.locator("#account-tab-profile").click());
      assertThat(page.locator("#account-email input")).hasValue(email);
      var inbox = new MailpitInbox(context);
      inbox.assertSecurityNotice(
          inbox.awaitSecurityMail(email, texts.getAccountPasswordChangedSubject()),
          changed,
          BrowserTestAccounts.USER_PASSWORD);
      other.page.navigate(path(LOGIN_ROUTE));
      var login = new LoginPage(other.page);
      login.assertFormVisible();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      login.assertRejected();
      login.open();
      login.signIn(email, changed);
      assertThat(other.page).hasURL(urlPattern(HOME_ROUTE));
      new UserMenu(other.page).selectTestLanguage();
    }
  }

  @Test
  @DisplayName(
      "ACCOUNT-SEC-03: E-Mail-Bestätigung, aktuelles Passwort und unveränderte Adresse prüfen")
  @Description(
      """
      1. Eigenes verifiziertes Konto öffnen und E-Mail-Dialog unter Sicherheit aufrufen.
      2. Abweichende neue Adressen absenden; Bestätigungsfehler und Markierung prüfen.
      3. Gleiche neue Adressen mit falschem aktuellem Passwort absenden; Passwortfehler prüfen.
      4. Bisherige Adresse als neue Adresse absenden; verständliche Ablehnung und unverändertes Profil prüfen.
      **Erwartet:** Fehler bleiben im Dialog korrigierbar; die Kontoadresse ändert sich nicht.
      """)
  void emailValidationDoesNotChangeTheAccount() {
    String email = account(false);
    var form = new AccountPage(page);
    var security = new AccountSecurityPage(page);
    security.openEmail();
    security.email(
        "new-" + UUID.randomUUID() + "@example.test",
        "different@example.test",
        BrowserTestAccounts.USER_PASSWORD);
    security.requestEmail();
    form.assertMessage(texts.getBaseValidationEmailConfirmMessage());
    assertThat(security.host("account-email-confirm")).hasJSProperty("invalid", true);
    form.closeNotification();
    String target = "new-" + UUID.randomUUID() + "@example.test";
    security.email(target, target, "Wrong!Password9");
    security.requestEmail();
    form.assertMessage(texts.getAccountSecurityWrongPassword());
    assertThat(security.host("account-email-password")).hasJSProperty("invalid", true);
    form.closeNotification();
    security.email(email, email, BrowserTestAccounts.USER_PASSWORD);
    security.requestEmail();
    form.assertMessage(texts.getAccountSecuritySameEmail());
    security.host("account-email-cancel").click();
    form.host("account-tab-profile").click();
    assertThat(form.input("account-email")).hasValue(email);
  }

  @Test
  @DisplayName(
      "ACCOUNT-SEC-04: E-Mail erst per Einmallink bestätigen, beide Sicherheitsmails und neue Anmeldung")
  @Description(
      """
      1. Eigenes Konto über Registrierung/eigene Mail aktivieren; zweite Remember-me-Sitzung anmelden.
      2. Neue Adresse im Dialog anfordern: bisherige Profiladresse bleibt bestehen.
      3. Echten Mailbutton öffnen und Vorschau neu laden: GET verbraucht den Link nicht und beendet keine Sitzung.
      4. Bewusst bestätigen: Erfolg, Sicherheitsmails an alte/neue Adresse, neue Anmeldung und Einmaligkeit prüfen.
      **Erwartet:** Adresse ändert sich erst mit Bestätigung; alle bisherigen Zugänge werden widerrufen.
      """)
  void emailChangeRequiresExplicitConfirmationAndNotifiesBothAddresses() {
    String oldEmail = account(true);
    String newEmail = "confirmed-" + UUID.randomUUID() + "@example.test";
    try (var other = new BrowserActor(browser)) {
      other.login(oldEmail, BrowserTestAccounts.USER_PASSWORD, true);
      assertThat(other.page).hasURL(urlPattern(HOME_ROUTE));
      var form = new AccountPage(page);
      var security = new AccountSecurityPage(page);
      security.openEmail();
      security.email(newEmail, newEmail, BrowserTestAccounts.USER_PASSWORD);
      security.requestEmail();
      form.assertMessage(texts.getAccountSecurityEmailRequested());
      form.closeNotification();
      form.host("account-tab-profile").click();
      assertThat(form.input("account-email")).hasValue(oldEmail);
      var inbox = new MailpitInbox(context);
      var requested = inbox.awaitSecurityMail(newEmail, texts.getAccountEmailChangeSubject());
      try (Page confirm = inbox.openEmailChangeAction(requested)) {
        assertThat(confirm.locator("#account-email-confirmation"))
            .containsText(texts.getAccountEmailConfirmationReady());
        String capturedLink = confirm.url();
        confirm.reload();
        assertThat(confirm.locator("#account-email-confirm-action")).isVisible();
        page.reload();
        assertThat(form.input("account-email")).hasValue(oldEmail);
        UiActions.complete(confirm, () -> confirm.locator("#account-email-confirm-action").click());
        assertThat(confirm.locator("#account-email-confirmation"))
            .containsText(texts.getAccountEmailConfirmationSuccess());
        inbox.assertSecurityNotice(
            inbox.awaitSecurityMail(oldEmail, texts.getAccountEmailChangedSubject()),
            BrowserTestAccounts.USER_PASSWORD);
        inbox.assertSecurityNotice(
            inbox.awaitSecurityMail(newEmail, texts.getAccountEmailChangedSubject()),
            BrowserTestAccounts.USER_PASSWORD);
        // Der native Loginbutton muss trotz widerrufener bisheriger Sitzung funktionieren.
        confirm.locator("#account-email-confirm-login").click();
        new LoginPage(confirm).assertFormVisible();
        other.page.navigate(path(LOGIN_ROUTE));
        new LoginPage(other.page).assertFormVisible();
        var login = new LoginPage(confirm);
        login.signIn(oldEmail, BrowserTestAccounts.USER_PASSWORD);
        login.assertRejected();
        login.open();
        login.signIn(newEmail, BrowserTestAccounts.USER_PASSWORD);
        assertThat(confirm).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(confirm).selectTestLanguage();
        try (var replay = new BrowserActor(browser)) {
          replay.page.navigate(capturedLink);
          assertThat(replay.page.locator("#account-email-confirmation"))
              .containsText(texts.getAccountEmailConfirmationInvalid());
          assertThat(replay.page.locator("#account-email-confirm-action")).hasCount(0);
        }
      }
    }
  }
}
