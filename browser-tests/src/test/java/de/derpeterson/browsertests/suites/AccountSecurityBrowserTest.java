package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.VERIFICATION_PAGE_TITLE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.mail.MailpitInbox.CapturedMail;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AccountSecurityPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationInboxPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Eigene Passwort- und verifizierte E-Mail-Änderung")
public class AccountSecurityBrowserTest extends BrowserFixture {
  private String account(boolean remember) {
    String email = "account-security-" + UUID.randomUUID() + "@example.test";
    RegistrationPage registration = new RegistrationPage(browserPage);
    registration.open();
    registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    registration.submit();
    registration.assertSubmitted(email);
    MailpitInbox inbox = new MailpitInbox(browserContext);
    try (Page activated = inbox.openVerificationLink(inbox.awaitVerificationMail(email))) {
      assertThat(activated).hasTitle(VERIFICATION_PAGE_TITLE);
    }
    LoginPage login = new LoginPage(browserPage);
    login.open();
    browserPage
        .getByRole(
            AriaRole.SWITCH,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getLoginRememberMeCheckbox())
                .setExact(true))
        .setChecked(remember);
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);

    assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    AccountPage form = new AccountPage(browserPage);
    form.open();
    // Sicherheitsmails folgen der gespeicherten Kontosprache, nicht nur dem aktuellen UI-Menü.
    form.language(uiTexts.language());
    form.save();
    form.assertMessage(uiTexts.getAccountSettingsSaveSuccess());
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
    AccountPage form = new AccountPage(browserPage);
    AccountSecurityPage security = new AccountSecurityPage(browserPage);
    security.openPassword();
    security.input("account-password-new").fill("x");

    assertThat(security.host("account-password-new")).hasJSProperty("invalid", false);
    assertThat(browserPage.locator("vaadin-notification-card")).hasCount(0);
    security.savePassword();
    form.assertMessage(uiTexts.getBaseValidationRequiredMessage());
    form.closeNotification();
    security.password("x", "x", BrowserTestAccounts.USER_PASSWORD);
    security.savePassword();
    form.assertMessage(uiTexts.getBaseValidationPasswordInvalidMessage());

    assertThat(security.host("account-password-new")).hasJSProperty("invalid", true);
    form.closeNotification();
    security.password(
        "Changed!Password9", "Different!Password9", BrowserTestAccounts.USER_PASSWORD);
    security.savePassword();
    form.assertMessage(uiTexts.getBaseValidationPasswordConfirmMessage());

    assertThat(security.host("account-password-confirm")).hasJSProperty("invalid", true);
    form.closeNotification();
    security.password("Changed!Password9", "Changed!Password9", "Wrong!Password9");
    security.savePassword();
    form.assertMessage(uiTexts.getAccountSecurityWrongPassword());

    assertThat(security.host("account-password-current")).hasJSProperty("invalid", true);
    assertThat(browserPage.locator("vaadin-dialog.account-password-dialog[opened]")).hasCount(1);
    assertThat(security.input("account-password-current")).isVisible();
    security.host("account-password-cancel").click();
  }

  @Test
  @Tag("follow-up-regression")
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
    try (BrowserActor other = new BrowserActor(testBrowser)) {
      other.login(email, BrowserTestAccounts.USER_PASSWORD, true);

      assertThat(other.browserPage).hasURL(urlPattern(HOME_ROUTE));
      AccountSecurityPage security = new AccountSecurityPage(browserPage);
      security.openPassword();

      assertThat(browserPage.locator("vaadin-dialog.account-password-dialog[opened]"))
          .containsText(uiTexts.getAccountPasswordDialogHint());
      security.password(changed, changed, BrowserTestAccounts.USER_PASSWORD);
      security.savePassword();
      new AccountPage(browserPage).assertMessage(uiTexts.getAccountSecurityPasswordSuccess());

      assertThat(browserPage.locator("vaadin-dialog.account-password-dialog[opened]")).hasCount(0);
      browserPage.reload();
      // Nach Reload bleibt bewusst der Sicherheits-Tab ausgewählt; das Profil erst öffnen.
      UiActions.complete(browserPage, () -> browserPage.locator("#account-tab-profile").click());

      assertThat(browserPage.locator("#account-email input")).hasValue(email);
      MailpitInbox inbox = new MailpitInbox(browserContext);
      inbox.assertSecurityNotice(
          inbox.awaitSecurityMail(email, uiTexts.getAccountPasswordChangedSubject()),
          changed,
          BrowserTestAccounts.USER_PASSWORD);
      other.browserPage.navigate(path(LOGIN_ROUTE));
      LoginPage login = new LoginPage(other.browserPage);
      login.assertFormVisible();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      login.assertRejected();
      login.open();
      login.signIn(email, changed);

      assertThat(other.browserPage).hasURL(urlPattern(HOME_ROUTE));
      new UserMenu(other.browserPage).selectTestLanguage();
    }
    verifyAccountNotificationDestination();
  }

  private void verifyAccountNotificationDestination() {
    NotificationInboxPage inbox = new NotificationInboxPage(browserPage);
    for (int width : new int[] {1280, 390}) {
      browserPage.setViewportSize(width, 900);
      inbox.open();
      inbox
          .row(inbox.inbox(), uiTexts.getNotificationsPasswordChangedTitle())
          .locator(".cosmos-notification-row-open")
          .click();
      Locator destination = inbox.dialog().locator(".cosmos-notification-detail-account");

      assertThat(destination).hasAccessibleName(uiTexts.getNotificationsOpenAccount());
      assertThat(inbox.dialog().locator(".cosmos-notification-detail-ticket")).hasCount(0);
      if (width == 390) {
        assertThat(destination).hasCSS("grid-column-start", "1");
        assertThat(destination).hasCSS("grid-column-end", "-1");
      }
      destination.click();

      assertThat(browserPage.locator("#account-security-tab-portal")).isVisible();
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
    AccountPage form = new AccountPage(browserPage);
    AccountSecurityPage security = new AccountSecurityPage(browserPage);
    security.openEmail();
    security.email(
        "new-" + UUID.randomUUID() + "@example.test",
        "different@example.test",
        BrowserTestAccounts.USER_PASSWORD);
    security.requestEmail();
    form.assertMessage(uiTexts.getBaseValidationEmailConfirmMessage());

    assertThat(security.host("account-email-confirm")).hasJSProperty("invalid", true);
    form.closeNotification();
    String target = "new-" + UUID.randomUUID() + "@example.test";
    security.email(target, target, "Wrong!Password9");
    security.requestEmail();
    form.assertMessage(uiTexts.getAccountSecurityWrongPassword());

    assertThat(security.host("account-email-password")).hasJSProperty("invalid", true);
    form.closeNotification();
    security.email(email, email, BrowserTestAccounts.USER_PASSWORD);
    security.requestEmail();
    form.assertMessage(uiTexts.getAccountSecuritySameEmail());
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
    try (BrowserActor other = new BrowserActor(testBrowser)) {
      other.login(oldEmail, BrowserTestAccounts.USER_PASSWORD, true);

      assertThat(other.browserPage).hasURL(urlPattern(HOME_ROUTE));
      AccountPage form = new AccountPage(browserPage);
      AccountSecurityPage security = new AccountSecurityPage(browserPage);
      security.openEmail();
      security.email(newEmail, newEmail, BrowserTestAccounts.USER_PASSWORD);
      security.requestEmail();
      form.assertMessage(uiTexts.getAccountSecurityEmailRequested());
      form.closeNotification();
      form.host("account-tab-profile").click();

      assertThat(form.input("account-email")).hasValue(oldEmail);
      MailpitInbox inbox = new MailpitInbox(browserContext);
      CapturedMail requested =
          inbox.awaitSecurityMail(newEmail, uiTexts.getAccountEmailChangeSubject());
      try (Page confirm = inbox.openEmailChangeAction(requested)) {
        assertThat(confirm.locator("#account-email-confirmation"))
            .containsText(uiTexts.getAccountEmailConfirmationReady());
        String capturedLink = confirm.url();
        confirm.reload();

        assertThat(confirm.locator("#account-email-confirm-action")).isVisible();
        browserPage.reload();

        assertThat(form.input("account-email")).hasValue(oldEmail);
        UiActions.complete(confirm, () -> confirm.locator("#account-email-confirm-action").click());

        assertThat(confirm.locator("#account-email-confirmation"))
            .containsText(uiTexts.getAccountEmailConfirmationSuccess());
        inbox.assertSecurityNotice(
            inbox.awaitSecurityMail(oldEmail, uiTexts.getAccountEmailChangedSubject()),
            BrowserTestAccounts.USER_PASSWORD);
        inbox.assertSecurityNotice(
            inbox.awaitSecurityMail(newEmail, uiTexts.getAccountEmailChangedSubject()),
            BrowserTestAccounts.USER_PASSWORD);
        // Der native Loginbutton muss trotz widerrufener bisheriger Sitzung funktionieren.
        confirm.locator("#account-email-confirm-login").click();
        new LoginPage(confirm).assertFormVisible();
        other.browserPage.navigate(path(LOGIN_ROUTE));
        new LoginPage(other.browserPage).assertFormVisible();
        LoginPage login = new LoginPage(confirm);
        login.signIn(oldEmail, BrowserTestAccounts.USER_PASSWORD);
        login.assertRejected();
        login.open();
        login.signIn(newEmail, BrowserTestAccounts.USER_PASSWORD);

        assertThat(confirm).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(confirm).selectTestLanguage();
        try (BrowserActor replay = new BrowserActor(testBrowser)) {
          replay.browserPage.navigate(capturedLink);

          assertThat(replay.browserPage.locator("#account-email-confirmation"))
              .containsText(uiTexts.getAccountEmailConfirmationInvalid());
          assertThat(replay.browserPage.locator("#account-email-confirm-action")).hasCount(0);
        }
      }
    }
  }
}
