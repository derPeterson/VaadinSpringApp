package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("browser")
@Feature("Benutzerverwaltung: Finden, Filtern und Bearbeiten")
public class UserManagementBrowserTest extends BrowserFixture {
    @Test
    @DisplayName("USER-05A: Neue Konten bleiben über Suche und nummerierte Seiten erreichbar")
    @Description("""
        1. Als Admin anmelden und zwölf eigene normale Benutzer über das Formular anlegen.
        2. Jedes neue Konto anhand seiner eigenen E-Mail finden, ohne erste-Seite-Annahme.
        3. Eigene Treffer auf zwei Seiten prüfen: zehn und zwei unterschiedliche Konten.
        4. Seitengröße 20 wählen, alle zwölf sehen; erfolglose Suche zeigt Leerzustand.
        **Erwartet:** Kein Konto wird ausgelassen oder doppelt gezählt; Suche setzt die Seite zurück.
        """)
    void searchAndPaginationFindAccountsBeyondTheFirstPage() {
        AdminUsersPage users = login();
        String prefix = unique("pages");
        Set<String> expected = new HashSet<>();
        for (int i = 1; i <= 12; i++) {
            String email = prefix + "-" + i + "@example.test";
            users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
            expected.add(email);
        }
        users.search(prefix);
        assertThat(users.emailCells(prefix)).hasCount(10);
        Set<String> firstPage = new HashSet<>(users.emailCells(prefix).allTextContents());
        assertThat(page.getByText(texts.getAdminUsersPaginationTotal(12), new Page.GetByTextOptions().setExact(true))).isVisible();
        button(texts.getAdminUsersPaginationPage(2)).click();
        assertThat(users.emailCells(prefix)).hasCount(2);
        Set<String> secondPage = new HashSet<>(users.emailCells(prefix).allTextContents());
        assertTrue(Collections.disjoint(firstPage, secondPage));
        Set<String> all = new HashSet<>(firstPage); all.addAll(secondPage);
        assertEquals(expected, all);
        assertThat(button(texts.getAdminUsersPaginationPage(2))).hasAttribute("aria-current", "page");
        assertThat(button(texts.getAdminUsersPaginationNextPage())).isDisabled();
        page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName(texts.getAdminUsersPaginationPerPage()).setExact(true)).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("20").setExact(true)).click();
        assertThat(users.emailCells(prefix)).hasCount(12);
        assertEquals(expected, new HashSet<>(users.emailCells(prefix).allTextContents()));
        AllurePlaywright.attachScreenshot("Eigene Konten und nummerierte Pagination", page);
        users.search(unique("nothing"));
        assertThat(users.emailCells(prefix)).hasCount(0);
        assertThat(page.getByText(texts.getAdminUsersGridEmptyState(), new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(button(texts.getAdminUsersPaginationNextPage())).isDisabled();
        assertThat(button(texts.getAdminUsersPaginationPreviousPage())).isDisabled();
    }

    @Test
    @DisplayName("USER-05B: Optionale Filter kombinieren sich; Datum funktioniert ohne Uhrzeit")
    @Description("""
        1. Eigenes aktives Benutzerkonto und deaktiviertes Konto mit beiden Rollen anlegen.
        2. Freigabe über Add Filter ergänzen; Ja/Nein liefern genau das passende Konto.
        3. Offline und Rollenmitgliedschaft kombinieren; Administrator filtert das Mehrrollen-Konto.
        4. Letzte Aktivität nur mit Datum setzen: Zukunft liefert keinen Treffer, Vergangenheit beide.
        5. Uhrzeit ergänzen und wieder leeren; Datum bleibt wirksam. Clear Filter leert Suche und Zusatzfilter.
        **Erwartet:** Alle Bedingungen wirken gemeinsam; keine Uhrzeitpflicht, keine Datenbankvorbereitung.
        """)
    void combineAndClearTypedFilters() {
        AdminUsersPage users = login();
        String prefix = unique("filters");
        String active = prefix + "-active@example.test", disabled = prefix + "-disabled@example.test";
        users.createUser(active, BrowserTestAccounts.USER_PASSWORD, true);
        users.createUser(disabled, BrowserTestAccounts.USER_PASSWORD, false, true);
        users.search(prefix);
        assertThat(users.emailCells(prefix)).hasCount(2);
        users.selectFilter(texts.getAdminUsersGridEnabled(), texts.getAdminUsersYes());
        assertThat(users.emailCells(prefix)).hasCount(1);
        assertThat(users.emailCells(prefix)).hasText(active);
        users.selectFilter(texts.getAdminUsersGridEnabled(), texts.getAdminUsersNo());
        assertThat(users.emailCells(prefix)).hasCount(1);
        assertThat(users.emailCells(prefix)).hasText(disabled);
        users.removeFilter(texts.getAdminUsersGridEnabled());
        users.selectFilter(texts.getAdminUsersGridStatus(), texts.getUserStatusOffline());
        users.selectFilter(texts.getAdminUsersFilterRole(), texts.getRoleUser());
        assertThat(users.emailCells(prefix)).hasCount(2);
        users.selectFilter(texts.getAdminUsersFilterRole(), texts.getRoleAdmin());
        assertThat(users.emailCells(prefix)).hasCount(1);
        assertThat(users.emailCells(prefix)).hasText(disabled);
        users.selectFilter(texts.getAdminUsersFilterRole(), texts.getRoleUser());
        users.activeSince(LocalDate.of(2099, 1, 1));
        assertThat(users.emailCells(prefix)).hasCount(0);
        users.activeSince(LocalDate.of(2000, 1, 1));
        assertThat(users.emailCells(prefix)).hasCount(2);
        users.activityTime(LocalTime.of(14, 30));
        assertThat(users.emailCells(prefix)).hasCount(2);
        users.clearActivityTime();
        assertThat(users.emailCells(prefix)).hasCount(2);
        users.removeFilter(texts.getAdminUsersFilterActiveSince());
        users.clearAllFilters();
        users.search(prefix);
        assertThat(users.emailCells(prefix)).hasCount(2);
        AllurePlaywright.attachScreenshot("Zurückgesetzte Filter und weiterhin erreichbare Konten", page);
    }

    @Test
    @DisplayName("USER-06A: Anlage blockiert ungültige Eingaben und verwendet die gemeinsame Passwortregel")
    @Description("""
        1. Eigenes bestehendes Konto vorbereiten, dann neues Formular öffnen.
        2. Leeres Formular und schwaches Passwort blockieren Speichern; lokalisierte Regelbeschreibung prüfen.
        3. Blanken Vornamen, ungültige E-Mail und Dublette korrigieren; jeweils Speichern gesperrt.
        4. Mit ABCDEFG! speichern: acht Zeichen, Großbuchstaben/Sonderzeichen, ohne Zahl/Kleinbuchstaben.
        5. Mit dem neu angelegten normalen Konto tatsächlich anmelden.
        **Erwartet:** Gemeinsame UserInputRules greifen; erfolgreicher UI-Ablauf nach Korrektur.
        """)
    void createValidationUsesTheSharedRuleAndAllowsCorrection() {
        AdminUsersPage users = login();
        String existing = unique("existing") + "@example.test";
        users.createActiveUser(existing, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openCreate();
        assertThat(form.saveButton()).isDisabled();
        String email = unique("validated") + "@example.test";
        form.fillNewUser(email, "weak", true, false);
        assertThat(form.saveButton()).isDisabled();
        assertThat(form.root()).containsText(texts.getAdminUsersPasswordHelperCreate());
        form.password().fill("ABCDEFG!");
        form.password().press("Tab");
        assertThat(form.saveButton()).isEnabled();
        form.firstName().fill("   ");
        form.firstName().press("Tab");
        assertThat(form.saveButton()).isDisabled();
        form.firstName().fill("Browser");
        form.email().fill("not-an-email");
        form.email().press("Tab");
        assertThat(form.saveButton()).isDisabled();
        form.email().fill(existing);
        form.email().press("Tab");
        assertThat(form.saveButton()).isDisabled();
        assertThat(form.root()).containsText(texts.getBaseValidationEmailExistsMessage());
        form.email().fill(email);
        form.email().press("Tab");
        AllurePlaywright.attachScreenshot("Korrigiertes Formular mit zentraler Passwortregel", page);
        form.save();
        users.search(email);
        assertThat(users.emailCells(email)).hasCount(1);
        signInAsCreatedUser(email, "ABCDEFG!");
    }

    @Test
    @DisplayName("USER-06B: Bearbeitung bleibt nach erneutem Öffnen gespeichert; leeres Passwort erhält die Anmeldung")
    @Description("""
        1. Eigenes Konto anlegen, per E-Mail suchen und genau dessen Editierdialog öffnen.
        2. Namen und E-Mail ändern, Passwort leer lassen, speichern.
        3. Alte Adresse liefert keinen Treffer; neue Adresse findet das Konto und gespeicherte Angaben.
        4. Dialog abbrechen und mit neuer Adresse plus ursprünglichem Passwort anmelden.
        **Erwartet:** Stammdaten dauerhaft gespeichert; leeres Passwort setzt es nicht zurück.
        """)
    void editAndReopenKeepsNamesEmailAndUnchangedPassword() {
        AdminUsersPage users = login();
        String original = unique("edit") + "@example.test", changed = unique("renamed") + "@example.test";
        users.createActiveUser(original, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openEdit(original);
        assertThat(form.firstName()).hasValue("Browser");
        assertThat(form.root()).containsText(texts.getAdminUsersPasswordHelperEdit());
        form.firstName().fill("Edited"); form.lastName().fill("Account"); form.email().fill(changed);
        assertThat(form.password()).hasValue("");
        form.save();
        users.search(original); assertThat(users.emailCells(original)).hasCount(0);
        form = users.openEdit(changed);
        assertThat(form.firstName()).hasValue("Edited"); assertThat(form.lastName()).hasValue("Account");
        assertThat(form.password()).hasValue("");
        AllurePlaywright.attachScreenshot("Gespeicherte Angaben im erneut geöffneten Formular", page);
        form.cancel();
        signInAsCreatedUser(changed, BrowserTestAccounts.USER_PASSWORD);
    }

    @Test
    @DisplayName("USER-06C: Löschabbruch erhält das Konto; Bestätigung entfernt genau das ausgewählte Konto")
    @Description("""
        1. Eigenes wegwerfbares Konto anlegen und über exakte E-Mail zur Bearbeitung öffnen.
        2. Löschbestätigung mit Name/E-Mail öffnen und abbrechen; Editierdialog bleibt offen.
        3. Nochmals öffnen und die Löschung dieses Kontos bestätigen.
        4. Beide Dialoge sind geschlossen; Suche findet keinen Treffer, Neuladen ebenfalls nicht.
        **Erwartet:** Öffnen/Abbrechen löscht nicht; nur bestätigte Löschung wird dauerhaft wirksam.
        """)
    void cancelAndConfirmDeletionHaveDifferentVisibleOutcomes() {
        AdminUsersPage users = login();
        String email = unique("delete") + "@example.test";
        users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openEdit(email);
        AdminUserDeleteDialog confirmation = form.requestDelete();
        assertThat(confirmation.root()).containsText(email);
        assertThat(confirmation.root()).containsText(texts.getAdminUsersDeleteDialogWarning());
        confirmation.cancel();
        assertThat(form.root()).hasCount(1); assertThat(form.email()).isVisible(); assertThat(form.email()).hasValue(email);
        confirmation = form.requestDelete();
        AllurePlaywright.attachScreenshot("Eindeutige Bestätigung für das eigene Testkonto", page);
        confirmation.confirm();
        assertThat(form.root()).hasCount(0);
        users.search(email); assertThat(users.emailCells(email)).hasCount(0);
        assertThat(page.getByText(texts.getAdminUsersGridEmptyState(), new Page.GetByTextOptions().setExact(true))).isVisible();
        button(texts.getAdminUsersRefreshTooltip()).click();
        assertThat(users.emailCells(email)).hasCount(0);
    }

    @Test
    @DisplayName("USER-06D: Das eigene Administratorkonto bietet keine ausführbare Selbstlöschung")
    @Description("""
        1. Als Bootstrap-Admin anmelden und das eigene Konto über seine genaue Adresse suchen.
        2. Editierdialog öffnen; Löschbutton ist sichtbar, aber deaktiviert.
        3. Abbrechen und Konto weiterhin in der Liste finden.
        **Erwartet:** Keine Löschbestätigung und kein Verlust des laufenden Administratorkontos.
        """)
    void selfDeletionIsDisabledInTheEditDialog() {
        AdminUsersPage users = login();
        users.open();
        AdminUserDialog form = users.openEdit(BrowserTestAccounts.ADMIN_EMAIL);
        assertThat(form.deleteButton()).isVisible(); assertThat(form.deleteButton()).isDisabled();
        assertThat(page.locator("vaadin-dialog.admin-user-delete-dialog[opened]")).hasCount(0);
        AllurePlaywright.attachScreenshot("Selbstlöschschutz im Bearbeitungsformular", page);
        form.cancel();
        assertThat(users.emailCells(BrowserTestAccounts.ADMIN_EMAIL)).hasCount(1);
    }

    @Test
    @DisplayName("USER-06E: Sprachwechsel erhält Suche, Freigabefilter und dessen ausgewählten Wert")
    @Description("""
        1. Zwei eigene Konten anlegen, eines freigegeben und eines deaktiviert.
        2. Eigene Suche und Filter Nein setzen; genau deaktiviertes Konto sichtbar.
        3. Sprache über den echten Avatar wechseln; neue Labels/übersetzter Nein-Wert, gleicher Treffer.
        4. Ausgangssprache wieder wählen; Suche und Filter bleiben erhalten.
        **Erwartet:** Auswahl ist typisiert, unabhängig von übersetzten Strings und ohne Neuladen erhalten.
        """)
    void languageChangesRetainSelectedFilterAndSearch() {
        AdminUsersPage users = login();
        String prefix = unique("locale"), disabled = prefix + "-disabled@example.test";
        users.createUser(prefix + "-active@example.test", BrowserTestAccounts.USER_PASSWORD, true);
        users.createUser(disabled, BrowserTestAccounts.USER_PASSWORD, false);
        users.search(prefix); users.selectFilter(texts.getAdminUsersGridEnabled(), texts.getAdminUsersNo());
        assertThat(users.emailCells(prefix)).hasText(disabled);
        UiTexts other = new UiTexts(texts.language().equals("de") ? "en" : "de");
        new UserMenu(page).selectLanguage(other.language());
        AdminUsersPage translated = new AdminUsersPage(page, other);
        assertThat(translated.searchField()).hasValue(prefix);
        assertThat(translated.filterField(other.getAdminUsersGridEnabled())).hasValue(other.getAdminUsersNo());
        assertThat(translated.emailCells(prefix)).hasText(disabled);
        new UserMenu(page).selectTestLanguage();
        assertThat(users.filterField(texts.getAdminUsersGridEnabled())).hasValue(texts.getAdminUsersNo());
        assertThat(users.emailCells(prefix)).hasText(disabled);
        AllurePlaywright.attachScreenshot("Erhaltene Filter nach Sprachwechsel in beide Richtungen", page);
    }

    @Test
    @DisplayName("USER-06F: Passwortänderung im Adminformular lehnt das alte Passwort danach ab")
    @Description("""
        1. Eigenes Konto anlegen und bearbeiten; schwaches neues Passwort sperrt Speichern.
        2. Gültiges neues Passwort speichern, Dialog schließen und als Admin abmelden.
        3. Alte Zugangsdaten werden abgelehnt; neue Zugangsdaten melden dasselbe Konto an.
        **Erwartet:** Dieselbe Passwortregel gilt beim Editieren; Änderung ist tatsächlich wirksam.
        """)
    void changedPasswordWorksAndThePreviousPasswordIsRejected() {
        AdminUsersPage users = login();
        String email = unique("password") + "@example.test", changed = "ChangedBrowser2!";
        users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        AdminUserDialog form = users.openEdit(email);
        form.password().fill("weak"); form.password().press("Tab");
        assertThat(form.saveButton()).isDisabled();
        form.password().fill(changed); form.password().press("Tab");
        form.save();
        new UserMenu(page).signOut();
        LoginPage login = new LoginPage(page); login.open(); login.signIn(email, BrowserTestAccounts.USER_PASSWORD); login.assertRejected();
        login.signIn(email, changed);
        assertThat(page).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(page).selectTestLanguage(); new UserMenu(page).assertUserSignedInWithoutAdminAction(email);
        AllurePlaywright.attachScreenshot("Anmeldung mit geändertem Passwort", page);
    }

    private AdminUsersPage login() {
        LoginPage login = new LoginPage(page); login.open();
        login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage();
        return new AdminUsersPage(page);
    }

    private void signInAsCreatedUser(String email, String password) {
        new UserMenu(page).signOut();
        LoginPage login = new LoginPage(page); login.open(); login.signIn(email, password);
        assertThat(page).hasURL(urlPattern(HOME_ROUTE));
        new UserMenu(page).selectTestLanguage(); new UserMenu(page).assertUserSignedInWithoutAdminAction(email);
    }

    private String unique(String purpose) { return purpose + "-" + UUID.randomUUID(); }
    private Locator button(String name) { return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true)); }
}
