package de.derpeterson.browsertests.suites;

import com.microsoft.playwright.*;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import de.derpeterson.browsertests.mail.MailpitInbox;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.*;

@Tag("browser") @Feature("Benutzerverwaltung: Freigabe, Rollen und Versionskonflikte")
public class UserAccessBrowserTest extends BrowserFixture {
    @Test @DisplayName("USER-07A: Kontosperre verweigert Anmeldung; gezielte Freigabe erhält das Passwort")
    @Description("""
        1. Eigenes aktives Konto im Adminformular anlegen; Freigabe entfernen und speichern.
        2. In einem unabhängigen Browserkontext anmelden: verständliche Ablehnung.
        3. Konto als Admin erneut freigeben und Formular neu öffnen: Aktiv markiert.
        4. Mit unverändertem Passwort anmelden: eigenes normales Konto ohne Adminrechte.
        **Erwartet:** Gespeicherte Sperre und Freigabe wirken tatsächlich auf neue Anmeldungen.
        """)
    void disableAndEnableAUserControlsTheActualLogin() {
        AdminUsersPage users = admin(); String email = email("enable");
        users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openEdit(email); form.setEnabled(false); form.save();
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.login(email, BrowserTestAccounts.USER_PASSWORD); new LoginPage(actor.page).assertRejected();
        }
        form = users.openEdit(email); assertThat(form.enabled()).not().isChecked();
        form.setEnabled(true); form.save();
        form = users.openEdit(email); assertThat(form.enabled()).isChecked(); form.cancel();
        ordinaryLogin(email);
    }

    @Test @DisplayName("USER-07B: Administratorrolle wird gespeichert; Entzug verhindert neuen Adminzugriff")
    @Description("""
        1. Eigenes normales Konto anlegen, Administratorrolle zusätzlich vergeben und neu öffnen.
        2. In unabhängiger Sitzung anmelden: Dashboard und Adminzugriff vorhanden.
        3. Abmelden; als ursprünglicher Admin Rolle entziehen, normale Rolle behalten.
        4. Neu anmelden: Startseite/eigenes Konto; direkter Adminaufruf bleibt ausgeschlossen.
        **Erwartet:** Die gespeicherten Rollen bestimmen neue Anmeldungen; aktuelle Sitzungen sind ein eigener Fall.
        """)
    void roleGrantAndRemovalDetermineNewLogins() {
        AdminUsersPage users = admin(); String email = email("role");
        users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openEdit(email); form.setAdministrator(true); form.save();
        form = users.openEdit(email); assertThat(form.administrator()).isChecked(); form.cancel();
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.loginAdmin(email, BrowserTestAccounts.USER_PASSWORD); new UserMenu(actor.page).signOut();
        }
        form = users.openEdit(email); form.setAdministrator(false); form.save();
        form = users.openEdit(email); assertThat(form.administrator()).not().isChecked(); form.cancel();
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.login(email, BrowserTestAccounts.USER_PASSWORD);
            assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
            new UserMenu(actor.page).selectTestLanguage();
            new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
            actor.page.navigate(path(ADMIN_ROUTE));
            assertThat(actor.page.getByText(texts.getBaseErrorAdminAccessDenied(), new Page.GetByTextOptions().setExact(true))).isVisible();
        }
    }

    @Test @DisplayName("USER-07C: Ein unverifiziertes Konto wird nur durch ausdrückliche Adminfreigabe aktiviert")
    @Description("""
        1. Eigenes Konto registrieren und echte Verifikationsmail erfassen, deren Link nicht verwenden.
        2. Neue Anmeldung wird abgewiesen. Als Admin genau dieses Konto öffnen: Aktiv nicht markiert.
        3. Aktiv ausdrücklich markieren und speichern; in unabhängiger Sitzung anmelden.
        **Erwartet:** Sichtbare Adminfreigabe ermöglicht Anmeldung ohne versehentliches Verwenden des Mailtokens.
        """)
    void administratorCanExplicitlyEnableAnUnverifiedAccount() {
        String email = email("pending");
        RegistrationPage register = new RegistrationPage(page); register.open();
        register.fillValid(email, BrowserTestAccounts.USER_PASSWORD); register.submit(); register.assertSubmitted(email);
        new MailpitInbox(context).awaitVerificationMail(email);
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.login(email, BrowserTestAccounts.USER_PASSWORD); new LoginPage(actor.page).assertRejected();
        }
        AdminUsersPage users = admin(); AdminUserDialog form = users.openEdit(email);
        assertThat(form.enabled()).not().isChecked(); form.setEnabled(true); form.save(); ordinaryLogin(email);
    }

    @Test @DisplayName("USER-07D: Zwei geöffnete Adminformulare überschreiben keine neuere Änderung")
    @Description("""
        1. Eigenes Zielkonto und zweites Administratorkonto über UI anlegen.
        2. Beide Administratoren öffnen gleichzeitig das Zielkonto in getrennten Kontexten.
        3. Erstes Formular speichert einen neuen Vornamen; zweites versucht einen anderen Nachnamen.
        4. Erwarteter Speicherhinweis hält das alte Formular offen; abbrechen und neu öffnen.
        **Erwartet:** Der erste Vorname bleibt gespeichert, der veraltete Nachname wird nicht übernommen.
        """)
    void twoOpenEditorsRejectTheStaleMutation() {
        AdminUsersPage users = admin(); String target = email("version"), second = email("second-admin");
        users.createActiveUser(target, BrowserTestAccounts.USER_PASSWORD);
        users.createUser(second, BrowserTestAccounts.USER_PASSWORD, true, true);
        try (BrowserActor actor = new BrowserActor(browser)) {
            AdminUsersPage other = actor.loginAdmin(second, BrowserTestAccounts.USER_PASSWORD);
            AdminUserDialog stale = other.openEdit(target);
            AdminUserDialog current = users.openEdit(target); current.firstName().fill("Winner"); current.save();
            stale.lastName().fill("Stale"); stale.firstName().click();
            stale.expectSaveError(texts.getBaseUserUpdateConflict()); stale.cancel();
            AdminUserDialog stored = users.openEdit(target);
            assertThat(stored.firstName()).hasValue("Winner"); assertThat(stored.lastName()).hasValue("Testuser");
            AllurePlaywright.attachScreenshot("Neuere Daten nach abgewiesenem alten Formular", page); stored.cancel();
        }
    }

    @Test @DisplayName("USER-07E: Eigene Sperre und eigener Adminrollenentzug werden verständlich abgewiesen")
    @Description("""
        1. Eigenes Administratorformular öffnen; Aktiv entfernen und Speichern versuchen.
        2. Lokalisierter Selbstsperrhinweis; Aktiv korrigieren und eigene Adminrolle entfernen.
        3. Lokalisierter Rollenhinweis; abbrechen, neu öffnen und erhaltene Freigabe/Rolle prüfen.
        **Erwartet:** Administrativer Selbstschutz ist über die echte Oberfläche wirksam; globale DB-Rennen bleiben technische Tests.
        """)
    void ownDisableAndAdminRoleRemovalAreRejected() {
        AdminUsersPage users = admin(); AdminUserDialog form = users.openEdit(BrowserTestAccounts.ADMIN_EMAIL);
        form.setEnabled(false); form.expectSaveError(texts.getAdminUsersErrorDisableOwnUser());
        form.setEnabled(true); form.setAdministrator(false); form.expectSaveError(texts.getAdminUsersErrorRemoveOwnAdminRole());
        form.cancel(); form = users.openEdit(BrowserTestAccounts.ADMIN_EMAIL);
        assertThat(form.enabled()).isChecked(); assertThat(form.administrator()).isChecked(); form.cancel();
    }

    @Test @DisplayName("USER-07F: Superadmin allein erhält Adminzugriff und die Adminaktion auf Home")
    @Description("""
        1. Eigenes aktives Konto über die UI anlegen; ausschließlich Superadmin zuweisen.
        2. Formular neu öffnen: Superadmin gespeichert, Administrator/Benutzer/Gast nicht zugewiesen.
        3. In einer unabhängigen Sitzung anmelden: Admin-Dashboard erreichbar.
        4. Home öffnen und über die sichtbare Verwaltungsaktion zurück in den Adminbereich wechseln.
        5. Eigenes Formular ohne zweite Adminrolle bearbeiten und speichern.
        **Erwartet:** Superadmin erbt Adminrechte ohne zusätzliche gespeicherte Administratorrolle.
        """)
    void superadminAloneHasAdminNavigationAndCanSaveItsOwnAccount() {
        AdminUsersPage users = admin(); String email = email("superadmin");
        users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openEdit(email);
        form.setRole(texts.getRoleUser(), false); form.setRole(texts.getRoleSuperadmin(), true); form.save();
        form = users.openEdit(email);
        assertThat(form.role(texts.getRoleSuperadmin())).isChecked();
        assertThat(form.role(texts.getRoleAdmin())).not().isChecked();
        assertThat(form.role(texts.getRoleUser())).not().isChecked();
        assertThat(form.role(texts.getRoleGuest())).not().isChecked(); form.cancel();
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.loginAdmin(email, BrowserTestAccounts.USER_PASSWORD);
            actor.page.navigate(path(HOME_ROUTE));
            assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
            new UserMenu(actor.page).open();
            actor.page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions()
                    .setName(texts.getBaseManageApplicationButton()).setExact(true)).click();
            assertThat(actor.page).hasURL(urlPattern(ADMIN_ROUTE));
            AdminUserDialog own = new AdminUsersPage(actor.page).openEdit(email);
            own.firstName().fill("Superadmin"); own.save();
            own = new AdminUsersPage(actor.page).openEdit(email);
            assertThat(own.firstName()).hasValue("Superadmin"); own.cancel();
        }
    }

    @Test @DisplayName("USER-07G: Gastrolle allein erlaubt Home, aber keinen Adminbereich")
    @Description("""
        1. Eigenes aktives Konto über die UI anlegen; ausschließlich Gast zuweisen.
        2. Formular neu öffnen: Gast gespeichert, Benutzer/Administrator/Superadmin nicht zugewiesen.
        3. In unabhängiger Sitzung anmelden: Home und eigenes Benutzermenü erreichbar.
        4. Admin direkt aufrufen: lokalisierte Zugriffsverweigerung auf Home.
        **Erwartet:** Gast kann sich wie Benutzer anmelden und Home nutzen; keine Adminrechte.
        """)
    void guestAloneCanSignInAndUseHomeWithoutAdminAccess() {
        AdminUsersPage users = admin(); String email = email("guest-role");
        users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openEdit(email);
        form.setRole(texts.getRoleUser(), false); form.setRole(texts.getRoleGuest(), true); form.save();
        form = users.openEdit(email);
        assertThat(form.role(texts.getRoleGuest())).isChecked();
        assertThat(form.role(texts.getRoleUser())).not().isChecked();
        assertThat(form.role(texts.getRoleAdmin())).not().isChecked();
        assertThat(form.role(texts.getRoleSuperadmin())).not().isChecked(); form.cancel();
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.login(email, BrowserTestAccounts.USER_PASSWORD);
            assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
            new UserMenu(actor.page).selectTestLanguage();
            new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
            new UserMenu(actor.page).close();
            actor.page.navigate(path(ADMIN_ROUTE));
            assertThat(actor.page.getByText(texts.getBaseErrorAdminAccessDenied(), new Page.GetByTextOptions().setExact(true))).isVisible();
        }
    }

    private AdminUsersPage admin() {
        LoginPage login = new LoginPage(page); login.open(); login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage(); login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
        return new AdminUsersPage(page);
    }
    private void ordinaryLogin(String email) {
        try (BrowserActor actor = new BrowserActor(browser)) {
            actor.login(email, BrowserTestAccounts.USER_PASSWORD); assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
            new UserMenu(actor.page).selectTestLanguage(); new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
        }
    }
    private String email(String purpose) { return purpose + "-" + UUID.randomUUID() + "@example.test"; }
}
