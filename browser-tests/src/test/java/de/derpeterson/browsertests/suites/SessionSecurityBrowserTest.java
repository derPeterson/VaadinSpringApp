package de.derpeterson.browsertests.suites;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import de.derpeterson.browsertests.mail.MailpitInbox;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;

@Tag("browser") @Feature("Bestehende Sitzung nach administrativer Änderung")
public class SessionSecurityBrowserTest extends BrowserFixture {
    @ParameterizedTest(name = "{0}: Altes Adminformular nach {1}, Remember-me={2}")
    @CsvSource({"SESSION-01A,role,false", "SESSION-01B,disable,false", "SESSION-01C,delete,false", "SESSION-01D,disable,true", "SESSION-01E,delete,true"})
    @Description("""
        1. Eigenes zweites Administratorkonto über die Oberfläche anlegen und separat anmelden.
        2. Dort ein neues Benutzerformular vollständig ausfüllen, noch nicht speichern; Sperre/Löschung auch mit Remember-me.
        3. Als ursprünglicher Admin Rolle entziehen, Konto sperren bzw. löschen.
        4. Alten Speichern-Button wirklich anklicken: Sicherheitsantwort 401 am Browserrequest prüfen.
        5. Geschützte Seite neu aufrufen: Login. Als ursprünglicher Admin nach dem Probe-Konto suchen.
        **Erwartet:** Der alte privilegierte Callback wird verweigert, Probe-Konto fehlt tatsächlich.
        Keine synthetischen RPCs; bereits laufende Aktionen sind kein rückwirkender Widerruf.
        """)
    void oldPrivilegedCallbackIsRejected(String caseId, String change, boolean remember) {
        AdminUsersPage users = admin(); String account = email("session"), probe = email("blocked-create");
        users.createUser(account, BrowserTestAccounts.USER_PASSWORD, true, true);
        try (BrowserActor actor = new BrowserActor(browser)) {
            AdminUsersPage other = actor.loginAdmin(account, BrowserTestAccounts.USER_PASSWORD, remember);
            other.open(); AdminUserDialog stale = other.openCreate();
            stale.fillNewUser(probe, BrowserTestAccounts.USER_PASSWORD, true, false);
            AdminUserDialog current = users.openEdit(account);
            switch (change) {
                case "role" -> {current.setAdministrator(false); current.save();}
                case "disable" -> {current.setEnabled(false); current.save();}
                case "delete" -> current.requestDelete().confirm();
                default -> throw new IllegalArgumentException(change);
            }
            Response denied = actor.page.waitForResponse(response -> response.status() == 401
                    && response.url().startsWith(System.getProperty("browser.tests.baseUrl")),
                    stale.saveButton()::click);
            assertEquals(401, denied.status());
            actor.page.navigate(path(ADMIN_ROUTE));
            assertThat(actor.page).hasURL(urlPattern(LOGIN_ROUTE));
            assertThat(actor.page).hasTitle(LOGIN_PAGE_TITLE);
            users.open(); users.search(probe); assertThat(users.emailCells(probe)).hasCount(0);
        }
    }

    private AdminUsersPage admin() {
        LoginPage login = new LoginPage(page); login.open(); login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage(); login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
        return new AdminUsersPage(page);
    }
    private String email(String purpose) {return purpose + "-" + UUID.randomUUID() + "@example.test";}
}
