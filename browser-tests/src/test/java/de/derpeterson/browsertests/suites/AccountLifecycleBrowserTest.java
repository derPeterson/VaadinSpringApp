package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.playwright.AllurePlaywright;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Weitere Kontoabläufe")
public class AccountLifecycleBrowserTest extends BrowserFixture {
  @Test
  @DisplayName("REG-02C: Blanke Namen und heutiges/zukünftiges Geburtsdatum bleiben korrigierbar")
  @Story("Einzelne Registrierungsfehler")
  @Description(
      """
      1. Gültige Registrierung vorbereiten, Vorname und Nachname einzeln blank setzen und absenden.
      2. Jeweils Pflichtfeldhinweis prüfen und den Namen korrigieren.
      3. Heutigen und morgigen Geburtstag einzeln absenden: Vergangenheitsregel prüfen.
      4. Vergangenen Geburtstag wiederherstellen und gültig registrieren; echte Mail abwarten.
      **Erwartet:** Fehler erlauben keinen sichtbaren Erfolg; nach Korrektur normaler Mailablauf.
      """)
  void namesAndBirthDateCanBeCorrected() {
    String email = unique();
    RegistrationPage form = new RegistrationPage(browserPage);
    form.open();
    form.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    form.change(uiTexts.getRegistrationFirstNameField(), " ");
    form.submit();
    form.assertValidationError(uiTexts.getBaseValidationRequiredMessage());
    form.change(uiTexts.getRegistrationFirstNameField(), "Browser");
    form.change(uiTexts.getRegistrationLastNameField(), " ");
    form.submit();
    form.assertValidationError(uiTexts.getBaseValidationRequiredMessage());
    form.change(uiTexts.getRegistrationLastNameField(), "Testuser");
    for (LocalDate date :
        new LocalDate[] {
          LocalDate.now(java.time.ZoneId.of("Europe/Berlin")),
          LocalDate.now(java.time.ZoneId.of("Europe/Berlin")).plusDays(1)
        }) {
      form.change(uiTexts.getRegistrationBirthDateField(), uiTexts.dateInput(date));
      browserPage
          .getByLabel(
              uiTexts.getRegistrationBirthDateField(), new Page.GetByLabelOptions().setExact(true))
          .press("Enter");
      form.submit();
      form.assertValidationError(uiTexts.getBaseValidationBirthDatePastMessage());
    }
    form.change(
        uiTexts.getRegistrationBirthDateField(), uiTexts.dateInput(LocalDate.of(1990, 1, 1)));
    browserPage
        .getByLabel(
            uiTexts.getRegistrationBirthDateField(), new Page.GetByLabelOptions().setExact(true))
        .press("Enter");
    form.submit();
    form.assertSubmitted(email);
    new MailpitInbox(browserContext).awaitVerificationMail(email);
  }

  @Test
  @DisplayName("REG-02D: Einzelne Passwortregeln und E-Mail-Bestätigung werden geprüft")
  @Story("Passwort und Bestätigung")
  @Description(
      """
      1. Gültiges Formular vorbereiten; zu kurzes Passwort, fehlenden Großbuchstaben und fehlendes Sonderzeichen einzeln versuchen.
      2. Jeweils Passwortregel prüfen und korrigieren; abweichende E-Mail-Bestätigung versuchen.
      3. Nach vollständig gültiger Korrektur Registrierungsbestätigung und echte Mail prüfen.
      **Erwartet:** Kein Erfolg aus einer ungültigen Variante; alle Varianten bleiben korrigierbar.
      """)
  void passwordRulesAndEmailConfirmation() {
    String email = unique();
    RegistrationPage form = new RegistrationPage(browserPage);
    form.open();
    form.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    for (String password : new String[] {"Abc!", "browseruser1!", "BrowserUser1"}) {
      form.setPasswords(password, password);
      form.submit();
      form.assertValidationError(uiTexts.getBaseValidationPasswordInvalidMessage());
    }
    form.setPasswords(BrowserTestAccounts.USER_PASSWORD, BrowserTestAccounts.USER_PASSWORD);
    form.setEmails(email, unique());
    form.submit();
    form.assertValidationError(uiTexts.getBaseValidationEmailConfirmMessage());
    form.setEmails(email, email);
    form.submit();
    form.assertSubmitted(email);
    new MailpitInbox(browserContext).awaitVerificationMail(email);
  }

  @Test
  @DisplayName(
      "REG-03A: Vorhandene Adresse und kanonisierte Variante werden als Dublette abgelehnt")
  @Story("E-Mail-Dubletten")
  @Description(
      """
      1. Eigenes Konto über Registrierung und echten Mailbutton aktivieren.
      2. Dieselbe Adresse sowie Großschreibung mit äußeren Leerzeichen erneut registrieren; anderes Passwort verwenden.
      3. Jeweils Dublettenhinweis statt Erfolg prüfen.
      4. Anmeldung mit ursprünglichem Passwort prüfen; ersetzendes Passwort wird abgelehnt.
      **Erwartet:** Kein Überschreiben der Identität oder des Passworts. SQL-Eindeutigkeit bleibt Persistenzprüfung.
      """)
  void duplicateRegistrationPreservesOriginalAccount() {
    String email = activate();
    RegistrationPage form = new RegistrationPage(browserPage);
    for (String address :
        new String[] {email, " " + email.toUpperCase(java.util.Locale.ROOT) + " "}) {
      form.open();
      form.fillValid(address, BrowserTestAccounts.RESET_PASSWORD);
      form.submit();
      form.assertValidationError(uiTexts.getBaseValidationEmailExistsMessage());
    }
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(email, BrowserTestAccounts.RESET_PASSWORD);
    login.assertRejected();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(browserPage).selectTestLanguage();
    new UserMenu(browserPage).assertUserSignedInWithoutAdminAction(email);
  }

  @Test
  @DisplayName(
      "RESET-03B: Unverifizierte Adresse erhält dieselbe neutrale Antwort wie eine unbekannte")
  @Story("Keine sichtbare Kontoauskunft")
  @Description(
      """
      1. Eigenes Konto registrieren, dessen echte Verifikationsmail nicht öffnen.
      2. Reset für die unverifizierte Adresse anfordern und sichtbare Antwort erfassen.
      3. Hinweis schließen; unbekannte Adresse anfordern und identische Antwort prüfen.
      **Erwartet:** Keine Auskunft zum Verifikationszustand; keine Timing- oder Mailabwesenheitsbehauptung.
      """)
  void unverifiedRequestIsNeutral() {
    String email = register();
    new MailpitInbox(browserContext).awaitVerificationMail(email);
    ForgotPasswordPage request = new ForgotPasswordPage(browserPage);
    request.open();
    request.enterEmail(email);
    request.submit();
    String response = request.requestedResponse();
    request.dismissRequestedNotice();
    request.open();
    request.enterEmail(unique());
    request.submit();
    assertEquals(response, request.requestedResponse());
  }

  @Test
  @DisplayName(
      "RESET-05A: Bereits geöffnetes zweites Resetformular kann den verbrauchten Token nicht nutzen")
  @Story("Zwei getrennte Reset-Sitzungen")
  @Description(
      """
      1. Eigenes Konto aktivieren und echte Reset-Mail anfordern; Formular in Sitzung A öffnen.
      2. Denselben tatsächlich erhaltenen Link in einer getrennten anonymen Sitzung B öffnen.
      3. In B ein neues Passwort setzen; im alten offenen Formular A ein anderes Passwort versuchen.
      4. Neutrale Ablehnung in A prüfen; Anmeldung nur mit Passwort aus B, nicht aus A.
      **Erwartet:** Zweiter veralteter Formularsubmit überschreibt nicht das erfolgreich gesetzte Passwort.
      """)
  void staleResetFormCannotOverwritePassword() {
    String email = activate();
    ForgotPasswordPage request = new ForgotPasswordPage(browserPage);
    request.open();
    request.enterEmail(email);
    request.submit();
    request.requestedResponse();
    MailpitInbox inbox = new MailpitInbox(browserContext);
    browserPage = inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email));
    ResetPasswordPage first = new ResetPasswordPage(browserPage);
    String link = browserPage.url();
    try (BrowserContext other =
        testBrowser.newContext(
            new Browser.NewContextOptions()
                .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                .setLocale(uiTexts.browserLocale())
                .setTimezoneId("Europe/Berlin"))) {
      AllurePlaywright.register(other);
      try (var _ = AllurePlaywright.startTracing(other)) {
        Page second = other.newPage();
        second.navigate(path(HOME_ROUTE));
        new UserMenu(second).selectTestLanguage();
        second.navigate(link);
        ResetPasswordPage winner = new ResetPasswordPage(second);
        winner.assertFormVisible();
        winner.fillPasswords(
            BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
        winner.submit();
        winner.assertChanged();
        AllurePlaywright.attachScreenshot("Erfolgreicher erster Verbrauch", second);
        first.fillPasswords("OtherReset3!", "OtherReset3!");
        first.submit();
        assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
            .containsText(uiTexts.visibleText(browserPage, uiTexts.getResetPasswordInvalidText()));
        AllurePlaywright.attachScreenshot("Abgelehntes veraltetes Formular", browserPage);
        LoginPage login = new LoginPage(browserPage);
        login.open();
        login.signIn(email, "OtherReset3!");
        login.assertRejected();
        login.signIn(email, BrowserTestAccounts.RESET_PASSWORD);
        assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
      }
    }
  }

  private String unique() {
    return "journey-" + UUID.randomUUID() + "@example.test";
  }

  private String register() {
    String email = unique();
    RegistrationPage form = new RegistrationPage(browserPage);
    form.open();
    form.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
    form.submit();
    form.assertSubmitted(email);
    return email;
  }

  private String activate() {
    String email = register();
    MailpitInbox inbox = new MailpitInbox(browserContext);
    browserPage = inbox.openVerificationLink(inbox.awaitVerificationMail(email));
    return email;
  }
}
