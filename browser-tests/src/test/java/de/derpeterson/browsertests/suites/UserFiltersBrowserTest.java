package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
        3. Offline und Rollenmitgliedschaft kombinieren; Rollenbadge am freien Rand ab-/anwählen. Administrator filtert das Mehrrollen-Konto.
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
    users.selectDraftFilter(texts.getAdminUsersFilterRole(), texts.getRoleAdmin());
    Locator adminRole =
        users
            .filterField(texts.getAdminUsersFilterRole())
            .locator("vaadin-checkbox")
            .filter(new Locator.FilterOptions().setHasText(texts.getRoleAdmin()));
    // Auch der freie rechte Rand gehört zur sichtbaren Badge-Klickfläche.
    adminRole.click(
        new Locator.ClickOptions()
            .setPosition(adminRole.boundingBox().width - 6, adminRole.boundingBox().height / 2));
    assertThat(adminRole.getByRole(AriaRole.CHECKBOX)).not().isChecked();
    adminRole.click();
    assertThat(adminRole.getByRole(AriaRole.CHECKBOX)).isChecked();
    AllurePlaywright.attachScreenshot("Vollständig anklickbare Filter-Badges", page);
    users.applyFilters();
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

  @Test
  @DisplayName("INPUT-02: Kompakte Zeit 930/1430 bleibt nach Sprachwechsel verwendbar")
  @Description(
      "Aktivitätsfilter öffnen, Datum setzen, 930 per Tab und 1430 per Enter übernehmen. Sprache im UI wechseln, dieselben Eingaben mit neu lokalisiertem Zeitformat prüfen. Ungültige 2460 zurückweisen.")
  void compactTimeSurvivesLocaleChanges() {
    AdminUsersPage users = login();
    users.open();
    users.activeSince(LocalDate.of(2000, 1, 1));
    for (UiTexts expected :
        new de.derpeterson.browsertests.i18n.UiTexts[] {
          texts,
          new de.derpeterson.browsertests.i18n.UiTexts(texts.language().equals("de") ? "en" : "de")
        }) {
      if (!expected.language().equals(texts.language())) {
        page.keyboard().press("Escape");
        if (page.locator("#admin-user-filter-panel[opened]").count() > 0) {
          page.getByRole(
                  AriaRole.BUTTON,
                  new Page.GetByRoleOptions().setName(texts.getAdminUsersCancel()).setExact(true))
              .click();
          assertThat(page.locator("#admin-user-filter-panel[opened]")).hasCount(0);
        }
        new UserMenu(page).selectLanguage(expected.language());
        users = new AdminUsersPage(page, expected);
      }
      users.activityTime().fill("930");
      users.activityTime().press("Tab");
      assertThat(users.activityTime()).hasValue(expected.timeInput(LocalTime.of(9, 30)));
      users.activityTime().fill("1430");
      users.activityTime().press("Enter");
      assertThat(users.activityTime()).hasValue(expected.timeInput(LocalTime.of(14, 30)));
    }
    users.activityTime().fill("2460");
    users.activityTime().press("Tab");
    assertThat(page.locator("vaadin-time-picker")).hasAttribute("invalid", "");
  }

  @Tag("fast")
  @Test
  @DisplayName("USER-11A: Suche und Filter sind Entwürfe; Abbrechen verwirft, Anwenden übernimmt")
  @Description(
      "Zwei eigene Konten suchen. Nein und eine andere Suche nur als Entwurf setzen. Escape verwirft beide. Nein anwenden reduziert auf ein Konto. Zurücksetzen wirkt erst nach Anwenden; Abbrechen erhält den aktiven Filter. Mehrere Status innerhalb einer Gruppe wirken als Oder, Status und Rolle zusammen als Und.")
  void draftApplyAndBadgeRemovalKeepTheirDistinctEffects() {
    AdminUsersPage users = login();
    String prefix = unique("drafts");
    String enabled = prefix + "-yes@example.test", disabled = prefix + "-no@example.test";
    users.createUser(enabled, BrowserTestAccounts.USER_PASSWORD, true);
    users.createUser(disabled, BrowserTestAccounts.USER_PASSWORD, false, true);
    users.search(prefix);
    users.selectDraftFilter(texts.getAdminUsersGridEnabled(), texts.getAdminUsersNo());
    users.searchField().fill("no-match-" + prefix);
    assertThat(users.emailCells(prefix)).hasCount(2);
    // Im nativen TextField leert Escape zuerst den Suchtext. Den Dialogabschluss
    // deshalb außerhalb dieses Felds auslösen und die Schließantwort abwarten.
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(texts.getAdminUsersApplyFilters()).setExact(true))
        .focus();
    page.keyboard().press("Escape");
    assertThat(page.locator("#admin-user-filter-panel[opened]")).hasCount(0);
    users.openFilters();
    assertThat(users.searchField()).hasValue(prefix);
    assertThat(
            users
                .filterField(texts.getAdminUsersGridEnabled())
                .getByRole(
                    AriaRole.RADIO,
                    new Locator.GetByRoleOptions()
                        .setName(texts.getAdminUsersFilterAll())
                        .setExact(true)))
        .isChecked();
    users.selectDraftFilter(texts.getAdminUsersGridEnabled(), texts.getAdminUsersNo());
    users.applyFilters();
    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(users.emailCells(prefix)).hasText(disabled);
    users.clearAllFilters();
    assertThat(users.emailCells(prefix)).hasCount(1);
    page.keyboard().press("Escape");
    assertThat(page.locator("#admin-user-filter-panel[opened]")).hasCount(0);
    assertThat(users.emailCells(prefix)).hasCount(1);
    users.clearAllFilters();
    users.searchField().fill(prefix);
    users.applyFilters();
    assertThat(users.emailCells(prefix)).hasCount(2);
    users.selectDraftFilter(texts.getAdminUsersGridStatus(), texts.getUserStatusOffline());
    users
        .filterField(texts.getAdminUsersGridStatus())
        .getByText(texts.getUserStatusEmployed(), new Locator.GetByTextOptions().setExact(true))
        .click();
    assertThat(page.locator(".admin-user-active-filter-count"))
        .hasText(texts.getAdminUsersActiveFilterCount(2));
    users.applyFilters();
    assertThat(users.emailCells(prefix)).hasCount(2);
    users.selectFilter(texts.getAdminUsersFilterRole(), texts.getRoleAdmin());
    users.openFilters();
    page.setViewportSize(390, 844);
    assertThat(page.locator(".admin-user-active-filter-count"))
        .hasText(texts.getAdminUsersActiveFilterCount(3));
    page.setViewportSize(1280, 900);
    users.applyFilters();
    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(users.emailCells(prefix)).hasText(disabled);
    AllurePlaywright.attachScreenshot("Filterentwürfe, Reset und Mehrfachauswahl", page);
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
