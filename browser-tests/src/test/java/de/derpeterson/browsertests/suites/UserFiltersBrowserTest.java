package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;

/** Visible user filters, independently runnable without the edit/delete cases. */
@Tag("browser")
@Feature("Benutzerverwaltung: Filter")
public class UserFiltersBrowserTest extends BrowserFixture {
  @Test
  @DisplayName(
      "USER-05B: Optionale Filter kombinieren sich; direkte Datumeingabe funktioniert ohne Uhrzeit")
  @Description(
      """
        1. Eigenes aktives Benutzerkonto und deaktiviertes Konto mit beiden Rollen anlegen.
        2. Filterpanel öffnen, Freigabe wählen und anwenden; Ja/Nein liefern genau das passende Konto.
        3. Offline und Rollenmitgliedschaft kombinieren; Administrator filtert das Mehrrollen-Konto.
        4. Letzte Aktivität nur mit Datum setzen: Zukunft liefert keinen Treffer, Vergangenheit beide.
        5. Uhrzeit ergänzen und wieder leeren; Datum bleibt wirksam. Alle zurücksetzen leert Suche, Zusatzfilter und aktive Badges.
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
    AllurePlaywright.attachScreenshot(
        "Zurückgesetzte Filter und weiterhin erreichbare Konten", page);
  }

  @Test
  @DisplayName("USER-05C: Aktivitätsdatum über Kalender-Icon und optionale Uhrzeit über Dropdown")
  @Description(
      """
        1. Eigenes aktives Konto über das Adminformular anlegen und exakt suchen.
        2. Heutiges Datum als lokalisierten Platzhalter prüfen; Feldklick öffnet keinen Kalender.
        3. Über das Icon morgen wählen: Konto verschwindet; Wiederöffnen markiert die Auswahl.
        4. Über den Kalender gestern wählen: Konto erscheint, Uhrzeit bleibt leer.
        5. Aktuellen Uhrzeitplatzhalter und optionalen Zeithinweis prüfen; Feldklick öffnet keine Zeitliste. Uhrzeit über Icon/Dropdown auswählen, zum Tagesende und zurück scrollen, dann leeren; Datum bleibt wirksam.
        6. Alle zurücksetzen leert Suche, Zusatzfilter und aktive Badges; Konto bleibt über Suche erreichbar.
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
    // Beide Scrollrichtungen unabhängig von der tatsächlichen Uhrzeit nachweisen.
    users.activityTimeUsingDropdown(LocalTime.of(23, 45));
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

  @Tag("fast")
  @Test
  @DisplayName(
      "USER-11A: Filterentwürfe wirken erst nach Anwenden; Badge-X übernimmt keine anderen Entwürfe")
  @Description(
      """
        1. Zwei eigene Konten anlegen, eines aktiv und eines deaktiviert, und gemeinsam suchen.
        2. Nein als Entwurf wählen: beide Treffer bleiben. Panel schließen/öffnen erhält den Entwurf.
        3. Anwenden: ein Treffer und ein aktiver Badge. Einen noch nicht angewandten Rollenentwurf wählen.
        4. Aktiv-Badge über X entfernen: beide Treffer wieder sichtbar, Rollenentwurf bleibt ohne Wirkung.
        5. Rollenentwurf anwenden, danach Alle zurücksetzen: Treffer und Badges folgen den Aktionen.
        **Erwartet:** Suche bleibt live; Entwürfe, angewandte Filter und einzelne Badge-Aktionen bleiben getrennt.
        """)
  void draftApplyAndBadgeRemovalKeepTheirDistinctEffects() {
    AdminUsersPage users = login();
    String prefix = unique("drafts");
    users.createUser(prefix + "-yes@example.test", BrowserTestAccounts.USER_PASSWORD, true);
    users.createUser(prefix + "-no@example.test", BrowserTestAccounts.USER_PASSWORD, false);
    users.search(prefix);
    users.selectDraftFilter(texts.getAdminUsersGridEnabled(), texts.getAdminUsersNo());
    assertThat(users.emailCells(prefix)).hasCount(2);
    assertThat(page.locator(".admin-user-active-filter-chip:visible")).hasCount(0);
    Locator toggle =
        page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getAdminUsersAddFilter()).setExact(true));
    toggle.click();
    assertThat(toggle).hasAttribute("aria-expanded", "false");
    users.openFilters();
    assertThat(users.filterField(texts.getAdminUsersGridEnabled()))
        .hasValue(texts.getAdminUsersNo());
    users.applyFilters();
    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(page.locator(".admin-user-active-filter-count"))
        .hasText(texts.getAdminUsersActiveFilterCount(1));
    users.selectDraftFilter(texts.getAdminUsersFilterRole(), texts.getRoleAdmin());
    users.removeFilter(texts.getAdminUsersGridEnabled());
    assertThat(users.emailCells(prefix)).hasCount(2);
    assertThat(users.filterField(texts.getAdminUsersFilterRole())).hasValue(texts.getRoleAdmin());
    users.applyFilters();
    assertThat(users.emailCells(prefix)).hasCount(0);
    users.clearAllFilters();
    users.search(prefix);
    assertThat(users.emailCells(prefix)).hasCount(2);
    AllurePlaywright.attachScreenshot("Entwürfe und aktive Filter korrekt getrennt", page);
  }

  @Tag("fast")
  @Test
  @DisplayName(
      "USER-11B: Uhrzeit braucht ein Datum; Overlay scrollt zum Platzhalter ohne einen Wert zu übernehmen")
  @Description(
      """
        1. Filterpanel öffnen: Uhrzeit ist ohne Datum deaktiviert.
        2. Gestern als Datum setzen: Uhrzeit wird aktiv und bleibt leer.
        3. Zeitliste über das Icon öffnen: der aktuelle Viertelstunden-Platzhalter ist sichtbar, Höhe höchstens 300 px plus Border.
        4. Ohne Auswahl schließen: Uhrzeit bleibt leer. Datum leeren: Uhrzeit wird wieder deaktiviert.
        **Erwartet:** Platzhalter ist keine Auswahl; Scrollposition, Größenlimit und Abhängigkeit sind tatsächlich wirksam.
        """)
  void timeRequiresDateAndPlaceholderScrollDoesNotCommitAValue() {
    AdminUsersPage users = login();
    users.open();
    users.openFilters();
    assertThat(users.activityTime()).isDisabled();
    users.activeSince(LocalDate.now(ZoneId.of("Europe/Berlin")).minusDays(1));
    assertThat(users.activityTime()).isEnabled();
    Locator picker = page.locator(".admin-user-activity-filter vaadin-time-picker");
    picker.locator("[part~='toggle-button']").click();
    Locator overlay = page.locator("vaadin-time-picker-overlay[opened]");
    assertThat(overlay).isVisible();
    String placeholder = users.activityTime().getAttribute("placeholder");
    assertThat(
            page.getByRole(
                AriaRole.OPTION, new Page.GetByRoleOptions().setName(placeholder).setExact(true)))
        .isVisible();
    org.junit.jupiter.api.Assertions.assertTrue(
        overlay.locator("[part~='overlay']").boundingBox().height <= 302);
    users.activityTime().press("Escape");
    assertThat(overlay).hasCount(0);
    assertThat(users.activityTime()).hasValue("");
    users.activityDate().fill("");
    users.activityDate().press("Tab");
    assertThat(users.activityTime()).isDisabled();
    assertThat(users.activityTime()).hasValue("");
    AllurePlaywright.attachScreenshot("Datumabhängigkeit ohne implizite Zeitauswahl", page);
  }

  private AdminUsersPage login() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    return new AdminUsersPage(page);
  }

  private String unique(String purpose) {
    return purpose + "-" + UUID.randomUUID();
  }
}
