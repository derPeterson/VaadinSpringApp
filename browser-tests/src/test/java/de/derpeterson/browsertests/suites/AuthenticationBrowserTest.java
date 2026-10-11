package de.derpeterson.browsertests.suites;

import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.app.config.AppConstants.USER_ICON_ALT;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Independent browser contexts per test; each case can run without preceding cases. */
@Tag("browser")
@Feature("Anmeldung und Zugriffsrechte")
public class AuthenticationBrowserTest extends BrowserFixture {
    @Test
    @DisplayName("AUTH-01A: Admin meldet sich an und erreicht das Dashboard")
    @Story("Admin-Anmeldung")
    @Description("""
            1. In einer frischen Browsersitzung das Loginformular öffnen.
            2. Mit dem aktiven Bootstrap-Testadmin anmelden.
            3. Dashboard und angezeigte Konto-E-Mail prüfen.

            **Erwartet:** Erfolgreiche Anmeldung; Admin-Dashboard und korrektes Konto sichtbar.
            Prüft keine nachträgliche Vergabe einer Adminrolle.
            """)
    void adminCanSignIn() {
        signInAsAdmin();
    }

    @Test
    @DisplayName("AUTH-01B: Falsches Passwort wird verständlich abgelehnt")
    @Story("Abgelehnte Anmeldung")
    @Description("""
            1. In einer frischen Browsersitzung das Loginformular öffnen.
            2. Die bekannte Admin-E-Mail mit einem falschen Passwort absenden.
            3. Fehlermeldung prüfen und anschließend /admin direkt aufrufen.

            **Erwartet:** Allgemeiner Anmeldefehler; keine Anmeldung und kein Adminzugriff.
            """)
    void wrongPasswordIsRejected() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.signIn(BrowserTestAccounts.ADMIN_EMAIL, "DefinitelyWrong1!");
        login.assertRejected();
        page.navigate(path(ADMIN_ROUTE));
        login.assertFormVisible();
    }

    @Test
    @DisplayName("AUTH-01C: Unbekanntes Konto erhält denselben Anmeldefehler")
    @Story("Abgelehnte Anmeldung")
    @Description("""
            1. In einer frischen Browsersitzung das Loginformular öffnen.
            2. Eine syntaktisch gültige, nicht angelegte Testadresse absenden.
            3. Allgemeine Fehlermeldung und fehlenden Adminzugriff prüfen.

            **Erwartet:** Derselbe verständliche Fehler wie bei falschem Passwort; keine Anmeldung.
            """)
    void unknownUserIsRejected() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.signIn("unknown-" + UUID.randomUUID() + "@example.test", "UnknownUser1!");
        login.assertRejected();
        page.navigate(path(ADMIN_ROUTE));
        login.assertFormVisible();
    }

    @Test
    @DisplayName("AUTH-01D: Anonyme Sitzung kann den Adminbereich nicht öffnen")
    @Story("Anonymer Zugriff")
    @Description("""
            1. Ohne Anmeldung /admin direkt öffnen.
            2. Loginformular und Abwesenheit des Admin-Dashboards prüfen.

            **Erwartet:** Weiterleitung zur Anmeldung; keine Admininhalte.
            """)
    void anonymousUserCannotOpenAdmin() {
        page.navigate(path(ADMIN_ROUTE));
        new LoginPage(page).assertFormVisible();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(texts.getAdminDashboardTitle()).setExact(true))).hasCount(0);
    }

    @Test
    @DisplayName("AUTH-02A: Logout entzieht den Zugriff auch einem bestehenden Tab")
    @Story("Abmeldung und bestehende Tabs")
    @Description("""
            1. Als Testadmin anmelden und /admin in einem zweiten Tab derselben Sitzung öffnen.
            2. Im ersten Tab über das Benutzermenü abmelden.
            3. Im zweiten Tab eine Navigation auslösen; anschließend /admin neu laden.
            4. Im ersten Tab Browser-Zurück und anschließend /admin erneut öffnen.

            **Erwartet:** Nach Logout kein neuer Adminzugriff; beide Tabs benötigen Anmeldung.
            Eine gecachte Ansicht zählt nicht als Zugriff. Mutationen aus einem alten offenen
            Formular und Rollenentzug über Push werden damit noch nicht vollständig geprüft.
            """)
    void logoutEndsAccessInBothTabs() {
        signInAsAdmin();
        Page otherTab = context.newPage();
        otherTab.navigate(path(ADMIN_ROUTE));
        new LoginPage(otherTab).assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
        new UserMenu(page).signOut();
        otherTab.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(USER_ICON_ALT).setExact(true)).click();
        otherTab.navigate(path(ADMIN_ROUTE));
        new LoginPage(otherTab).assertFormVisible();
        page.goBack();
        new LoginPage(page).assertFormVisible();
        page.navigate(path(ADMIN_ROUTE));
        new LoginPage(page).assertFormVisible();
        otherTab.close();
    }

    @Test
    @DisplayName("SEC-01A: Normaler Benutzer kann sich anmelden, aber keine Adminfunktionen nutzen")
    @Story("Zugriffsrechte normaler Benutzer")
    @Description("""
            1. Als Admin ein aktives Testkonto nur mit Benutzerrolle über die Oberfläche anlegen.
            2. Abmelden und das neue Konto über das Loginformular anmelden.
            3. Angemeldetes Konto und fehlende Adminaktion im Menü prüfen.
            4. /admin direkt aufrufen und Zugriffsverweigerung prüfen.

            **Erwartet:** Normale Anmeldung funktioniert; Adminbereich bleibt gesperrt und
            zeigt einen verständlichen Hinweis. Benutzeranlage dient der Testvorbereitung;
            die vollständige Benutzerverwaltung wird noch nicht geprüft.
            """)
    void ordinaryUserHasNoAdminAccess() {
        String email = "browser-user-" + UUID.randomUUID() + "@example.test";
        signInAsAdmin();
        new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        new UserMenu(page).signOut();
        LoginPage login = new LoginPage(page);
        login.open();
        login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
        assertThat(page).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(page).selectTestLanguage();
        new UserMenu(page).assertUserSignedInWithoutAdminAction(email);
        page.navigate(path(ADMIN_ROUTE));
        assertThat(page).hasURL(urlPattern(HOME_ROUTE));
        assertThat(page.getByText(texts.getBaseErrorAdminAccessDenied(), new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(texts.getAdminDashboardTitle()).setExact(true))).hasCount(0);
    }

    private void signInAsAdmin() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
        new UserMenu(page).selectTestLanguage();
        login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    }
}
