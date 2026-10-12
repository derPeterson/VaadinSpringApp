package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.UUID;

/** Visible user filters, independently runnable without the edit/delete cases. */
@Tag("browser")
@Feature("Benutzerverwaltung: Filter")
public class UserFiltersBrowserTest extends BrowserFixture {
    @Test
    @DisplayName("USER-05B: Optionale Filter kombinieren sich; direkte Datumeingabe funktioniert ohne Uhrzeit")
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
    @DisplayName("USER-05C: Aktivitätsdatum über Kalender-Icon und optionale Uhrzeit über Dropdown")
    @Description("""
        1. Eigenes aktives Konto über das Adminformular anlegen und exakt suchen.
        2. Kurzes lokalisiertes Datumsbeispiel prüfen; Feldklick öffnet keinen Kalender.
        3. Über das Icon morgen wählen: Konto verschwindet; Wiederöffnen markiert die Auswahl.
        4. Über den Kalender gestern wählen: Konto erscheint, Uhrzeit bleibt leer.
        5. Kurzbeispiel und Optional-Hinweis prüfen; Feldklick öffnet keine Zeitliste. Uhrzeit über Icon/Dropdown auswählen und wieder leeren; Datum bleibt wirksam.
        6. Clear Filter leert Suche und Zusatzfilter; Konto bleibt über Suche erreichbar.
        **Erwartet:** Echte Kalenderauswahl ohne direkte Datumeingabe, optionale Zeit und wirksame Trefferfilterung.
        """)
    void calendarAndOptionalTimeDropdownFilterTheActualAccount() {
        AdminUsersPage users = login();
        String email = unique("calendar-filter") + "@example.test";
        users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
        users.search(email);
        assertThat(users.emailCells(email)).hasCount(1);
        users.activityDateUsingCalendar(1);
        assertThat(users.emailCells(email)).hasCount(0);
        users.activityDateUsingCalendar(-1);
        assertThat(users.emailCells(email)).hasCount(1);
        users.activityTimeUsingDropdown(LocalTime.of(14, 30));
        assertThat(users.emailCells(email)).hasCount(1);
        users.clearActivityTime();
        assertThat(users.emailCells(email)).hasCount(1);
        AllurePlaywright.attachScreenshot("Kalenderauswahl und optionale Uhrzeit", page);
        users.clearAllFilters();
        users.search(email);
        assertThat(users.emailCells(email)).hasCount(1);
    }

    private AdminUsersPage login() {
        LoginPage login = new LoginPage(page); login.open();
        login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
        new UserMenu(page).selectTestLanguage();
        return new AdminUsersPage(page);
    }

    private String unique(String purpose) { return purpose + "-" + UUID.randomUUID(); }
}
