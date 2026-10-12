package de.derpeterson.browsertests.suites;

import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("browser")
@Feature("Passwort-Zurücksetzen")
public class PasswordResetBrowserTest extends BrowserFixture {
    @Test
    @DisplayName("RESET-01A: Echte Reset-Mail ermöglicht ein neues Passwort und die Anmeldung")
    @Story("Vom vergessenen Passwort zur erneuten Anmeldung")
    @Description("""
            1. Ein eigenes Konto über Registrierung und echten Verifikationsmailbutton aktivieren.
            2. Im Loginformular Passwort vergessen öffnen und seine E-Mail absenden.
            3. Neutralen Anfragehinweis und die eigene tatsächlich versandte Reset-Mail prüfen.
            4. Den sichtbaren Mailbutton anklicken, neues Passwort samt Bestätigung absenden.
            5. Mit dem alten Passwort anmelden: verständliche Ablehnung prüfen.
            6. Mit dem neuen Passwort anmelden: Startseite und eigenes Konto ohne Adminrechte prüfen.

            **Erwartet:** Der Reset-Mailbutton bleibt auf dem exakten Testserver. Nur das neue
            Passwort ermöglicht die Anmeldung. Keine externe SMTP-/Mailclient-Prüfung.
            """)
    void realResetMailChangesTheLoginPassword() {
        String email = activateAccount();
        ResetPasswordPage reset = openResetForm(email);
        reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
        reset.submit();
        reset.assertChanged();
        AllurePlaywright.attachScreenshot("Bestätigte Passwortänderung", page);
        LoginPage login = new LoginPage(page);
        login.open();
        login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
        login.assertRejected();
        assertNewPasswordLogin(email);
    }

    @Test
    @DisplayName("RESET-02A: Leere und ungültige E-Mail sind im Anfrageformular korrigierbar")
    @Story("Anfrageformular validieren")
    @Description("""
            1. Ein eigenes Konto registrieren und über seine Verifikationsmail aktivieren.
            2. Passwort vergessen öffnen und die leere Anfrage absenden.
            3. Pflichtfeldhinweis prüfen; ungültige E-Mail eingeben und erneut absenden.
            4. E-Mail-Hinweis und Markierung prüfen; eigene gültige Adresse eingeben und absenden.
            5. Neutralen Anfragehinweis und Eingang der eigenen Reset-Mail prüfen.

            **Erwartet:** Ungültige Eingaben zeigen keinen Anfrageerfolg. Nach Korrektur wird
            die gültige Anfrage über die Oberfläche angenommen und ihre Mail tatsächlich erfasst.
            """)
    void requestEmailCanBeCorrected() {
        String email = activateAccount();
        ForgotPasswordPage request = new ForgotPasswordPage(page);
        request.open();
        request.submit();
        request.assertRequiredError();
        request.enterEmail("not-an-email");
        request.submit();
        request.assertEmailError();
        AllurePlaywright.attachScreenshot("Ungültige Reset-Anfrage", page);
        request.enterEmail(email);
        request.submit();
        request.requestedResponse();
        new MailpitInbox(context).awaitPasswordResetMail(email);
    }

    @Test
    @DisplayName("RESET-02B: Passwortfehler sind korrigierbar und verbrauchen den Link nicht vorzeitig")
    @Story("Tokengebundenes Formular validieren")
    @Description("""
            1. Eigenes Konto über UI und echte Mail aktivieren; Reset-Mail über Passwort vergessen anfordern.
            2. Ihren Mailbutton öffnen und die leeren Passwortfelder absenden; Pflichtfeldhinweis prüfen.
            3. Zwei ungültige Passwörter absenden; Passwortregel und Feldmarkierungen prüfen.
            4. Zwei gültige, aber verschiedene Passwörter absenden; Bestätigungshinweis prüfen.
            5. Beide Felder gültig und identisch füllen; Änderung und neue Anmeldung prüfen.

            **Erwartet:** Alle drei Fehler bleiben korrigierbar. Derselbe Mail-Link ermöglicht
            danach die Passwortänderung; technische Tokenzustände werden nicht per SQL geprüft.
            """)
    void passwordErrorsCanBeCorrectedUsingTheSameLink() {
        String email = activateAccount();
        ResetPasswordPage reset = openResetForm(email);
        reset.submit();
        reset.assertRequiredError();
        reset.fillPasswords("weak", "weak");
        reset.submit();
        reset.assertPasswordError();
        reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, "DifferentPassword3!");
        reset.submit();
        reset.assertConfirmationError();
        AllurePlaywright.attachScreenshot("Abweichende Passwortbestätigung", page);
        reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
        reset.submit();
        reset.assertChanged();
        assertNewPasswordLogin(email);
    }

    @Test
    @DisplayName("RESET-03A: Vorhandene und unbekannte E-Mail bekommen denselben neutralen Hinweis")
    @Story("Keine Kontoauskunft im sichtbaren Anfrageergebnis")
    @Description("""
            1. Ein eigenes Konto über Registrierung und Verifikationsmail aktivieren.
            2. Reset-Anfrage für dieses Konto absenden, sichtbare Antwort erfassen und eigene Mail abwarten.
            3. Die Bestätigung über ihren Schließen-Button schließen.
            4. Formular erneut öffnen, eine andere neue UUID-Adresse absenden und Antwort erfassen.
            5. Sichtbaren Titel und Nachricht mit der ersten Antwort vergleichen.

            **Erwartet:** Beide Antworten sind identisch und neutral. Die unbekannte Adresse
            verrät kein Konto. Timing-Unterschiede, technische Fehler und dauerhafte Mailabwesenheit
            werden durch diesen UI-Fall nicht nachgewiesen.
            """)
    void unknownEmailHasTheSameVisibleResponse() {
        String email = activateAccount();
        ForgotPasswordPage request = new ForgotPasswordPage(page);
        request.open();
        request.enterEmail(email);
        request.submit();
        String knownResponse = request.requestedResponse();
        new MailpitInbox(context).awaitPasswordResetMail(email);
        request.dismissRequestedNotice();
        request.open();
        request.enterEmail("unknown-reset-" + UUID.randomUUID() + "@example.test");
        request.submit();
        assertEquals(knownResponse, request.requestedResponse());
    }

    @Test
    @DisplayName("RESET-04A: Unbekannter Token wird abgelehnt und ohne Token erscheint die Startseite")
    @Story("Ungültiger oder fehlender Reset-Link")
    @Description("""
            1. /reset-password mit einem neuen, nicht vergebenen UUID-Token öffnen.
            2. Neutralen Fehlerhinweis und fehlendes Passwortformular prüfen.
            3. /reset-password ohne Token öffnen.
            4. Tatsächlichen Startseiteninhalt, URL und Titel prüfen.

            **Erwartet:** Unbekannte Links erlauben keine Passwortänderung. Ohne Token
            erscheint die vollständige Startseite. Exakte Ablaufgrenzen bleiben JUnit-Aufgabe.
            """)
    void unknownAndMissingTokensHaveDefinedUiBehavior() {
        page.navigate(path(RESET_PASSWORD_ROUTE) + "/" + UUID.randomUUID());
        new ResetPasswordPage(page).assertInvalid();
        AllurePlaywright.attachScreenshot("Ungültiger Reset-Link", page);
        page.navigate(path(RESET_PASSWORD_ROUTE));
        assertThat(page).hasURL(urlPattern(HOME_ROUTE));
        assertThat(page).hasTitle(HOME_PAGE_TITLE);
        assertThat(page.getByText("VaadinSpringApp (Browser-Test)", new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    @Test
    @DisplayName("RESET-04B: Bereits benutzter Reset-Link bietet kein weiteres Passwortformular")
    @Story("Einmalige sichtbare Verwendung")
    @Description("""
            1. Eigenes Konto über UI aktivieren; seine echte Reset-Mail anfordern und ihren Button öffnen.
            2. Ein neues Passwort samt Bestätigung setzen und den Erfolg prüfen.
            3. Dieselbe tokengebundene Seite neu laden.
            4. Neutralen Ablehnungshinweis und fehlendes Passwortformular prüfen.

            **Erwartet:** Nach einem erfolgreichen Reset lässt sich der Link nicht erneut
            zur Passwortänderung verwenden. Paralleler Verbrauch bleibt in Persistenztests.
            """)
    void usedResetLinkCannotBeUsedAgain() {
        String email = activateAccount();
        ResetPasswordPage reset = openResetForm(email);
        reset.fillPasswords(BrowserTestAccounts.RESET_PASSWORD, BrowserTestAccounts.RESET_PASSWORD);
        reset.submit();
        reset.assertChanged();
        page.reload();
        reset.assertInvalid();
    }

    private String activateAccount() {
        String email = "reset-" + UUID.randomUUID() + "@example.test";
        RegistrationPage registration = new RegistrationPage(page);
        registration.open();
        registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
        registration.submit();
        registration.assertSubmitted(email);
        MailpitInbox inbox = new MailpitInbox(context);
        page = inbox.openVerificationLink(inbox.awaitVerificationMail(email));
        return email;
    }

    private ResetPasswordPage openResetForm(String email) {
        ForgotPasswordPage request = new ForgotPasswordPage(page);
        request.open();
        request.enterEmail(email);
        request.submit();
        request.requestedResponse();
        MailpitInbox inbox = new MailpitInbox(context);
        page = inbox.openPasswordResetLink(inbox.awaitPasswordResetMail(email));
        return new ResetPasswordPage(page);
    }

    private void assertNewPasswordLogin(String email) {
        LoginPage login = new LoginPage(page);
        login.open();
        login.signIn(email, BrowserTestAccounts.RESET_PASSWORD);
        assertThat(page).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(page).selectTestLanguage();
        new UserMenu(page).assertUserSignedInWithoutAdminAction(email);
    }
}
