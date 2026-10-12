package de.derpeterson.browsertests.suites;

import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;
import com.microsoft.playwright.Page;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Tag("browser")
@Feature("Verifikation")
public class VerificationBrowserTest extends BrowserFixture {
    @Test
    @DisplayName("VER-01A: Echte Verifikationsmail aktiviert das Konto und ermöglicht normale Anmeldung")
    @Story("Registrierung bis zur ersten Anmeldung")
    @Description("""
            1. Ein eigenes Konto über das Registrierungsformular anlegen.
            2. Seine tatsächlich versandte Mail in Mailpit lesen; Empfänger, Betreff,
               Namen, Serviceangabe und ausgefüllte Platzhalter prüfen.
            3. Den sichtbaren Mailbutton anklicken und Aktivierungsbestätigung prüfen.
            4. Mit dem neuen Konto anmelden und die fehlende Adminaktion im Menü prüfen.

            **Erwartet:** Der Mailbutton führt ausschließlich in die isolierte Testanwendung.
            Die Verifikation gelingt; normale Anmeldung funktioniert ohne Adminrechte.
            Keine Bewertung der Darstellung in externen Mailprogrammen.
            """)
    void realMailAllowsActivationAndLogin() {
        String email = newEmail();
        MailpitInbox inbox = new MailpitInbox(context);
        Page activated = inbox.openVerificationLink(registerAndReceive(email, inbox));
        assertOrdinaryLogin(activated, email);
    }

    @Test
    @DisplayName("VER-01B: Verbrauchter Link zeigt beim weiterhin aktiven Konto erneut Erfolg")
    @Story("Erneutes Öffnen des Aktivierungslinks")
    @Description("""
            1. Ein eigenes Konto registrieren und über seinen echten Mailbutton aktivieren.
            2. Den bereits verwendeten Link vor einer Anmeldung erneut öffnen.
            3. Erneute Erfolgsanzeige und weiterhin mögliche normale Anmeldung prüfen.

            **Erwartet:** Nach dem bestehenden Vertrag bleibt der Erfolg beim aktiven Konto
            idempotent sichtbar. Eine nachträgliche Adminsperre wird in diesem Fall nicht geprüft.
            """)
    void usedLinkRemainsSuccessfulForActiveAccount() {
        String email = newEmail();
        MailpitInbox inbox = new MailpitInbox(context);
        Page activated = inbox.openVerificationLink(registerAndReceive(email, inbox));
        activated.reload();
        new VerificationPage(activated).assertActivated();
        assertOrdinaryLogin(activated, email);
    }

    @Test
    @DisplayName("VER-02A: Unbekannter Token zeigt einen Hinweis, fehlender Token führt zur Startseite")
    @Story("Ungültiger oder fehlender Aktivierungslink")
    @Description("""
            1. /verification mit einem neuen, nicht vergebenen Token öffnen.
            2. Verständlichen Fehler, fehlende Erfolgsanzeige und sichtbares Mailformular prüfen.
            3. /verification ohne Token öffnen.

            **Erwartet:** Unbekannter Link zeigt keinen Aktivierungserfolg. Ohne Token erscheint
            die Startseite. Erneuter Versand, Ablauf und Adminsperren sind noch nicht abgedeckt.
            """)
    void invalidAndMissingTokensHaveDefinedUiBehavior() {
        page.navigate(path(VERIFICATION_ROUTE) + "/" + UUID.randomUUID());
        new VerificationPage(page).assertUnknownToken();
        page.navigate(path(VERIFICATION_ROUTE));
        assertThat(page).hasURL(urlPattern(HOME_ROUTE));
        assertThat(page.getByText("VaadinSpringApp (Browser-Test)", new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page).hasTitle(HOME_PAGE_TITLE);
    }

    private MailpitInbox.CapturedMail registerAndReceive(String email, MailpitInbox inbox) {
        RegistrationPage registration = new RegistrationPage(page);
        registration.open();
        registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
        registration.submit();
        registration.assertSubmitted(email);
        return inbox.awaitVerificationMail(email);
    }

    private void assertOrdinaryLogin(Page target, String email) {
        LoginPage login = new LoginPage(target);
        login.open();
        login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
        assertThat(target).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(target).selectTestLanguage();
        new UserMenu(target).assertUserSignedInWithoutAdminAction(email);
    }

    private static String newEmail() { return "verification-" + UUID.randomUUID() + "@example.test"; }
}
