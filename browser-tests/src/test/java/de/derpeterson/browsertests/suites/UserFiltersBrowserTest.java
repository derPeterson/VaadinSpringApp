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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Visible user filters, independently runnable without the edit/delete cases. */
@Tag("browser")
@Feature("Benutzerverwaltung: Filter")
public class UserFiltersBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @Tag("component-follow-up")
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
  void combineAndClearTypedFilters(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    AdminUsersPage users = login();
    String prefix = unique("filters");
    String active = prefix + "-active@example.test", disabled = prefix + "-disabled@example.test";
    users.createUser(active, BrowserTestAccounts.USER_PASSWORD, true);
    users.createUser(disabled, BrowserTestAccounts.USER_PASSWORD, false, true);
    users.search(prefix);

    assertThat(users.emailCells(prefix)).hasCount(2);
    users.selectFilter(uiTexts.getAdminUsersGridEnabled(), uiTexts.getAdminUsersYes());

    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(users.emailCells(prefix)).hasText(active);
    users.selectFilter(uiTexts.getAdminUsersGridEnabled(), uiTexts.getAdminUsersNo());

    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(users.emailCells(prefix)).hasText(disabled);
    users.removeFilter(uiTexts.getAdminUsersGridEnabled());
    users.selectFilter(uiTexts.getAdminUsersGridStatus(), uiTexts.getUserStatusOffline());
    users.selectFilter(uiTexts.getAdminUsersFilterRole(), uiTexts.getRoleUser());

    assertThat(users.emailCells(prefix)).hasCount(2);
    users.selectDraftFilter(uiTexts.getAdminUsersFilterRole(), uiTexts.getRoleAdmin());
    Locator adminRole =
        users
            .filterField(uiTexts.getAdminUsersFilterRole())
            .locator("vaadin-checkbox")
            .filter(new Locator.FilterOptions().setHasText(uiTexts.getRoleAdmin()));
    // Auch der freie rechte Rand gehört zur sichtbaren Badge-Klickfläche.
    adminRole.click(
        new Locator.ClickOptions()
            .setPosition(adminRole.boundingBox().width - 6, adminRole.boundingBox().height / 2));

    assertThat(adminRole.getByRole(AriaRole.CHECKBOX)).not().isChecked();
    adminRole.click();

    assertThat(adminRole.getByRole(AriaRole.CHECKBOX)).isChecked();
    AllurePlaywright.attachScreenshot("Vollständig anklickbare Filter-Badges", browserPage);
    users.applyFilters();

    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(users.emailCells(prefix)).hasText(disabled);
    users.selectFilter(uiTexts.getAdminUsersFilterRole(), uiTexts.getRoleUser());
    users.activeSince(LocalDate.of(2099, 1, 1));

    assertThat(users.emailCells(prefix)).hasCount(0);
    users.activeSince(LocalDate.of(2000, 1, 1));

    assertThat(users.emailCells(prefix)).hasCount(2);
    users.activityTime(LocalTime.of(14, 30));

    assertThat(users.emailCells(prefix)).hasCount(2);
    users.clearActivityTime();

    assertThat(users.emailCells(prefix)).hasCount(2);
    users.removeFilter(uiTexts.getAdminUsersFilterActiveSince());
    users.clearAllFilters();
    users.search(prefix);

    assertThat(users.emailCells(prefix)).hasCount(2);
    AllurePlaywright.attachScreenshot(
        "Zurückgesetzte Filter und weiterhin erreichbare Konten", browserPage);
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
    AllurePlaywright.attachScreenshot("Kalenderauswahl und optionale Uhrzeit", browserPage);
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
          uiTexts,
          new de.derpeterson.browsertests.i18n.UiTexts(
              uiTexts.language().equals("de") ? "en" : "de")
        }) {
      if (!expected.language().equals(uiTexts.language())) {
        browserPage.keyboard().press("Escape");
        if (browserPage.locator("#admin-user-filter-panel[opened]").count() > 0) {
          browserPage
              .getByRole(
                  AriaRole.BUTTON,
                  new Page.GetByRoleOptions().setName(uiTexts.getAdminUsersCancel()).setExact(true))
              .click();

          assertThat(browserPage.locator("#admin-user-filter-panel[opened]")).hasCount(0);
        }
        new UserMenu(browserPage).selectLanguage(expected.language());
        users = new AdminUsersPage(browserPage, expected);
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

    assertThat(browserPage.locator("vaadin-time-picker")).hasAttribute("invalid", "");
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
    users.selectDraftFilter(uiTexts.getAdminUsersGridEnabled(), uiTexts.getAdminUsersNo());
    users.searchField().fill("no-match-" + prefix);

    assertThat(users.emailCells(prefix)).hasCount(2);
    // Im nativen TextField leert Escape zuerst den Suchtext. Den Dialogabschluss
    // deshalb außerhalb dieses Felds auslösen und die Schließantwort abwarten.
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(uiTexts.getAdminUsersApplyFilters()).setExact(true))
        .focus();
    browserPage.keyboard().press("Escape");

    assertThat(browserPage.locator("#admin-user-filter-panel[opened]")).hasCount(0);
    users.openFilters();

    assertThat(users.searchField()).hasValue(prefix);
    assertThat(
            users
                .filterField(uiTexts.getAdminUsersGridEnabled())
                .getByRole(
                    AriaRole.TAB,
                    new Locator.GetByRoleOptions()
                        .setName(uiTexts.getAdminUsersFilterAll())
                        .setExact(true)))
        .hasAttribute("aria-selected", "true");
    users.selectDraftFilter(uiTexts.getAdminUsersGridEnabled(), uiTexts.getAdminUsersNo());
    users.applyFilters();

    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(users.emailCells(prefix)).hasText(disabled);
    users.clearAllFilters();

    assertThat(users.emailCells(prefix)).hasCount(1);
    browserPage.keyboard().press("Escape");

    assertThat(browserPage.locator("#admin-user-filter-panel[opened]")).hasCount(0);
    assertThat(users.emailCells(prefix)).hasCount(1);
    users.clearAllFilters();
    users.searchField().fill(prefix);
    users.applyFilters();

    assertThat(users.emailCells(prefix)).hasCount(2);
    users.selectDraftFilter(uiTexts.getAdminUsersGridStatus(), uiTexts.getUserStatusOffline());
    users
        .filterField(uiTexts.getAdminUsersGridStatus())
        .getByText(uiTexts.getUserStatusEmployed(), new Locator.GetByTextOptions().setExact(true))
        .click();

    assertThat(browserPage.locator(".admin-user-active-filter-count"))
        .hasText(uiTexts.getAdminUsersActiveFilterCount(2));
    users.applyFilters();

    assertThat(users.emailCells(prefix)).hasCount(2);
    users.selectFilter(uiTexts.getAdminUsersFilterRole(), uiTexts.getRoleAdmin());
    users.openFilters();
    browserPage.setViewportSize(390, 844);

    assertThat(browserPage.locator(".admin-user-active-filter-count"))
        .hasText(uiTexts.getAdminUsersActiveFilterCount(3));
    browserPage.setViewportSize(1280, 900);
    users.applyFilters();

    assertThat(users.emailCells(prefix)).hasCount(1);
    assertThat(users.emailCells(prefix)).hasText(disabled);
    AllurePlaywright.attachScreenshot("Filterentwürfe, Reset und Mehrfachauswahl", browserPage);
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
    Locator picker = browserPage.locator(".admin-user-activity-filter vaadin-time-picker");
    picker.locator("[part~='toggle-button']").click();
    Locator overlay = browserPage.locator("vaadin-time-picker-overlay[opened]");

    assertThat(overlay).isVisible();
    String placeholder = users.activityTime().getAttribute("placeholder");

    assertThat(
            browserPage.getByRole(
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
    AllurePlaywright.attachScreenshot("Datumabhängigkeit ohne implizite Zeitauswahl", browserPage);
  }

  private AdminUsersPage login() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    return new AdminUsersPage(browserPage);
  }

  private String unique(String purpose) {
    return purpose + "-" + UUID.randomUUID();
  }
}
