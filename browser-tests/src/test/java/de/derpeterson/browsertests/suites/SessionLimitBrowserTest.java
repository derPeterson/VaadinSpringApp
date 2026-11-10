package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("browser")
@Feature("Gleichzeitige Sitzungen begrenzen")
public class SessionLimitBrowserTest extends BrowserFixture {
  private ConfigurationPage settings;
  private String original;
  @TempDir Path profile;

  @BeforeEach
  void prepare() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    settings = new ConfigurationPage(browserPage);
    settings.open(AdminConfigGroup.SECURITY);
    original = settings.value(uiTexts.getAdminConfigEntryMaxSessionsPerUser());
  }

  @AfterEach
  void restore() {
    if (original != null) {
      settings.open(AdminConfigGroup.SECURITY);
      settings.enter(uiTexts.getAdminConfigEntryMaxSessionsPerUser(), original);
      settings.save();
    }
  }

  private String user() {
    String email = "session-limit-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(browserPage).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }

  private void limit(int value) {
    settings.open(AdminConfigGroup.SECURITY);
    settings.enter(uiTexts.getAdminConfigEntryMaxSessionsPerUser(), String.valueOf(value));
    settings.save();
  }

  private void signedIn(Page target, String email) {
    assertThat(target).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(target).selectTestLanguage();
    new UserMenu(target).assertUserSignedInWithoutAdminAction(email);
  }

  @Test
  @DisplayName("SESSION-LIMIT-01: Zusätzliche Tabs zählen einmal; Logout gibt den Platz frei")
  @Description(
      """
      1. Eigenes aktives Konto anlegen; Sitzungslimit 1 im Adminformular speichern.
      2. In unabhängigem Kontext anmelden, dann einen weiteren Tab desselben Kontextes öffnen.
      3. Zweiten Kontext mit richtigem Passwort und Remember-me anmelden: ausschließlich Sitzungsverwaltung anzeigen.
      4. Erste Sitzung über das Menü abmelden; die eingeschränkte Verwaltung abbrechen und im bisher abgewiesenen Kontext erneut anmelden.
      **Erwartet:** Tabs teilen einen Platz, bestehende Sitzung bleibt erhalten, abgewiesener Login erhält kein Remember-me-Cookie.
      """)
  void tabsShareOneSlotAndLogoutMakesRoom() {
    String email = user();
    limit(1);
    try (BrowserActor first = new BrowserActor(testBrowser);
        BrowserActor second = new BrowserActor(testBrowser)) {
      first.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(first.browserPage, email);
      Page tab = first.browserContext.newPage();
      tab.navigate(path(HOME_ROUTE));
      signedIn(tab, email);
      second.login(email, BrowserTestAccounts.USER_PASSWORD, true);
      LoginPage login = new LoginPage(second.browserPage);
      login.assertRestrictedManagement();

      assertFalse(
          second.browserContext.cookies().stream()
              .anyMatch(cookie -> cookie.name.equals("remember-me")));
      first.browserPage.reload();
      signedIn(first.browserPage, email);
      new UserMenu(first.browserPage).requestSignOut();
      login.cancelRecovery();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(second.browserPage, email);
      tab.close();
    }
  }

  @Test
  @DisplayName("SESSION-LIMIT-02: Reduzierung erhält bestehende Sitzungen; Erhöhung wirkt sofort")
  @Description(
      """
      1. Eigenes Konto anlegen; Limit 3 speichern und zwei unabhängige Sitzungen anmelden.
      2. Limit auf 1 reduzieren: beide offenen Sitzungen bleiben nach Neuladen angemeldet.
      3. Dritte Anmeldung öffnet nur die eingeschränkte Verwaltung. Limit auf 3 erhöhen, Verwaltung abbrechen und Login erneut absenden.
      **Erwartet:** Konfiguration gilt ohne Neustart für neue Zulassungen, bestehende Geräte werden nicht verdrängt.
      """)
  void changesApplyWithoutEvictingExistingDevices() {
    String email = user();
    limit(3);
    try (BrowserActor first = new BrowserActor(testBrowser);
        BrowserActor second = new BrowserActor(testBrowser);
        BrowserActor third = new BrowserActor(testBrowser)) {
      first.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(first.browserPage, email);
      second.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(second.browserPage, email);
      limit(1);
      first.browserPage.reload();
      signedIn(first.browserPage, email);
      second.browserPage.reload();
      signedIn(second.browserPage, email);
      third.login(email, BrowserTestAccounts.USER_PASSWORD);
      LoginPage login = new LoginPage(third.browserPage);
      login.assertRestrictedManagement();
      limit(3);
      login.cancelRecovery();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(third.browserPage, email);
    }
  }

  @Test
  @DisplayName("SESSION-LIMIT-03: Browserschließen ohne Logout gibt den Platz nicht sofort frei")
  @Description(
      """
      1. Eigenes Konto anlegen; Limit 1 speichern.
      2. Mit eigenem temporären Chromium-Profil ohne Remember-me anmelden und den Prozess schließen.
      3. In einem unabhängigen Browserprofil mit richtigem Passwort anmelden.
      **Erwartet:** Der Server hat noch keinen Logout oder Timeout erkannt und weist die neue Sitzung ab.
      """)
  void closingBrowserDoesNotPretendToBeLogout() {
    String email = user();
    limit(1);
    try (PersistentBrowserActor persistent =
        new PersistentBrowserActor(playwrightRuntime, profile)) {
      LoginPage login = new LoginPage(persistent.browserPage);
      login.open();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(persistent.browserPage, email);
    }
    try (BrowserActor next = new BrowserActor(testBrowser)) {
      next.login(email, BrowserTestAccounts.USER_PASSWORD);
      new LoginPage(next.browserPage).assertRestrictedManagement();
    }
  }

  @Test
  @DisplayName(
      "SESSION-LIMIT-04: Automatisches Remember-me respektiert die Grenze ohne Login-Schleife")
  @Description(
      """
      1. Eigenes Konto anlegen; Limit 2 speichern und in temporärem Chromium-Profil mit Remember-me anmelden.
      2. Browserprozess schließen; zweite unabhängige Sitzung anmelden, sodass beide Plätze belegt sind.
      3. Chromium mit demselben Profil neu starten: automatische Anmeldung wird mit Sitzungsgrenze abgewiesen.
      4. Bestehende zweite Sitzung bleibt nutzbar; Limit erhöhen und im abgewiesenen Profil normal anmelden.
      **Erwartet:** Automatische Anmeldung umgeht das Limit nicht, Cookie wird im betroffenen Browser entfernt; kein Redirect-Loop.
      """)
  void rememberMeCannotBypassTheLimit() {
    String email = user();
    limit(2);
    try (PersistentBrowserActor persistent =
        new PersistentBrowserActor(playwrightRuntime, profile)) {
      LoginPage login = new LoginPage(persistent.browserPage);
      login.open();
      persistent
          .browserPage
          .getByRole(
              AriaRole.SWITCH,
              new Page.GetByRoleOptions()
                  .setName(uiTexts.getLoginRememberMeCheckbox())
                  .setExact(true))
          .check();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(persistent.browserPage, email);

      assertTrue(
          persistent.browserContext.cookies().stream()
              .anyMatch(cookie -> cookie.name.equals("remember-me")));
    }
    try (BrowserActor existing = new BrowserActor(testBrowser)) {
      existing.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(existing.browserPage, email);
      try (PersistentBrowserActor persistent =
          new PersistentBrowserActor(playwrightRuntime, profile, LOGIN_ROUTE)) {
        LoginPage login = new LoginPage(persistent.browserPage);
        login.assertSessionLimit();

        assertFalse(
            persistent.browserContext.cookies().stream()
                .anyMatch(cookie -> cookie.name.equals("remember-me")));
        existing.browserPage.reload();
        signedIn(existing.browserPage, email);
        limit(3);
        login.dismissFailure();
        login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
        signedIn(persistent.browserPage, email);
      }
    }
  }
}
