package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.TokenConfigurationPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import de.derpeterson.browsertests.pages.UserStatusMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("browser")
@Feature("Manueller und automatischer Benutzerstatus")
public class UserStatusBrowserTest extends BrowserFixture {
  @Test
  @Tag("follow-up-regression")
  @DisplayName("STATUS-01: Manuelle Statuswahl wird gespeichert und in zweitem Tab live angezeigt")
  @Description(
      """
      1. Eigenes Konto über Adminformular anlegen und separat anmelden.
      2. Zwei echte Tabs derselben Sitzung öffnen und deren Benutzermenüs anzeigen.
      3. Verfügbar, abwesend, beschäftigt und offline über die Optionen wählen: zweiter Tab folgt live.
      4. Beschäftigt einstellen, Seite neu laden und gespeicherten aktuellen Wert prüfen.
      **Erwartet:** Ausgewählter Wert, nicht versteckte Optionsliste, zeigt den Status in beiden Tabs.
      """)
  void manualStatusIsStoredAndPushedToAnotherTab() {
    String email = createUser();
    try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
      browserActor.login(email, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(browserActor.browserPage).selectTestLanguage();
      Page second = browserActor.browserContext.newPage();
      second.navigate(path(HOME_ROUTE));
      UserStatusMenu firstMenu = new UserStatusMenu(browserActor.browserPage),
          secondMenu = new UserStatusMenu(second);
      secondMenu.expect(uiTexts.getUserStatusAvailable());
      for (String status :
          new String[] {
            uiTexts.getUserStatusAbsent(),
            uiTexts.getUserStatusEmployed(),
            uiTexts.getUserStatusOffline(),
            uiTexts.getUserStatusAvailable()
          }) {
        firstMenu.select(status);
        secondMenu.expect(status);
      }
      firstMenu.select(uiTexts.getUserStatusEmployed());
      secondMenu.expect(uiTexts.getUserStatusEmployed());
      assertPresenceTogglePreservesChoice(browserActor.browserPage, firstMenu);
      browserActor.browserPage.reload();
      firstMenu.expect(uiTexts.getUserStatusEmployed());
      second.close();
    }
  }

  private void assertPresenceTogglePreservesChoice(Page target, UserStatusMenu status) {
    UserMenu userMenu = new UserMenu(target);
    UiTexts other = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    for (int width : new int[] {1280, 390}) {
      target.setViewportSize(width, 900);
      status.expect(uiTexts.getUserStatusEmployed());
      Locator toggle = target.locator("#portal-menu-presence-toggle");

      assertThat(toggle).hasAttribute("aria-expanded", "true");
      UiActions.complete(target, toggle::click);

      assertThat(toggle).hasAttribute("aria-expanded", "false");
      assertThat(status.current()).isHidden();
      userMenu.selectLanguage(other.language());
      userMenu.open();

      assertThat(toggle).hasAttribute("aria-expanded", "false");
      UiActions.complete(target, () -> toggle.press("Enter"));

      assertThat(toggle).hasAttribute("aria-expanded", "true");
      assertThat(status.current()).containsText(other.getUserStatusEmployed());
      userMenu.selectTestLanguage();
    }
  }

  @Test
  @DisplayName(
      "STATUS-02: Automatische Abwesenheit und Rückkehr; manuell beschäftigt bleibt erhalten")
  @Description(
      """
      1. Als Admin Abwesenheitsdauer merken und im UI auf zehn Sekunden setzen.
      2. Eigenes Konto anmelden; ohne Benutzeraktion tatsächliche automatische Abwesenheit abwarten.
      3. Seite über echten Browseraufruf neu laden: verfügbar. Manuell beschäftigt wählen.
      4. Weitere zwanzig Sekunden real abwarten: beschäftigt bleibt. Manuell abwesend wählen und neu laden: auch nach Rückkehrlauf erhalten. Original wiederherstellen.
      **Erwartet:** Scheduler und Live-Anzeige wirken; keine Uhr-/Datenbankmanipulation.
      """)
  void automaticAbsenceAndReturnRespectManualStatus() {
    String email = createUser();
    AdminConfigurationPage adminConfigurationPage = new AdminConfigurationPage(browserPage);
    adminConfigurationPage.open(AdminConfigGroup.USER_STATUS);
    String label = uiTexts.getAdminConfigEntryUserAutoAbsentTimeout();
    TokenConfigurationPage durations = new TokenConfigurationPage(browserPage);
    String original = durations.value(label);
    try {
      durations.enter(label, "PT10S");
      adminConfigurationPage.save();
      try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
        browserActor.login(email, BrowserTestAccounts.USER_PASSWORD);
        new UserMenu(browserActor.browserPage).selectTestLanguage();
        UserStatusMenu menu = new UserStatusMenu(browserActor.browserPage);
        menu.expect(uiTexts.getUserStatusAvailable());

        assertThat(menu.current())
            .containsText(
                uiTexts.getUserStatusAbsent(),
                new com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions()
                    .setTimeout(40_000));
        browserActor.browserPage.reload();
        menu.expect(uiTexts.getUserStatusAvailable());
        menu.select(uiTexts.getUserStatusEmployed());
        long end = System.nanoTime() + java.time.Duration.ofSeconds(20).toNanos();
        browserActor.browserContext.waitForCondition(
            () -> System.nanoTime() >= end,
            new BrowserContext.WaitForConditionOptions().setTimeout(25_000));
        menu.expect(uiTexts.getUserStatusEmployed());
        menu.select(uiTexts.getUserStatusAbsent());
        browserActor.browserPage.reload();
        menu.expect(uiTexts.getUserStatusAbsent());
        long returnScan = System.nanoTime() + java.time.Duration.ofSeconds(3).toNanos();
        browserActor.browserContext.waitForCondition(() -> System.nanoTime() >= returnScan);
        menu.expect(uiTexts.getUserStatusAbsent());
      }
    } finally {
      adminConfigurationPage.open(AdminConfigGroup.USER_STATUS);
      durations.enter(label, original);
      adminConfigurationPage.save();
    }
  }

  private String createUser() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    String email = "status-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }
}
