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
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    settings = new ConfigurationPage(page);
    settings.open(AdminConfigGroup.SECURITY);
    original = settings.value(texts.getAdminConfigEntryMaxSessionsPerUser());
  }

  @AfterEach
  void restore() {
    if (original != null) {
      settings.open(AdminConfigGroup.SECURITY);
      settings.enter(texts.getAdminConfigEntryMaxSessionsPerUser(), original);
      settings.save();
    }
  }

  private String user() {
    String email = "session-limit-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }

  private void limit(int value) {
    settings.open(AdminConfigGroup.SECURITY);
    settings.enter(texts.getAdminConfigEntryMaxSessionsPerUser(), String.valueOf(value));
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
    try (BrowserActor first = new BrowserActor(browser);
        BrowserActor second = new BrowserActor(browser)) {
      first.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(first.page, email);
      Page tab = first.context.newPage();
      tab.navigate(path(HOME_ROUTE));
      signedIn(tab, email);
      second.login(email, BrowserTestAccounts.USER_PASSWORD, true);
      LoginPage login = new LoginPage(second.page);
      login.assertRestrictedManagement();
      assertFalse(
          second.context.cookies().stream().anyMatch(cookie -> cookie.name.equals("remember-me")));
      first.page.reload();
      signedIn(first.page, email);
      new UserMenu(first.page).requestSignOut();
      login.cancelRecovery();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(second.page, email);
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
    try (BrowserActor first = new BrowserActor(browser);
        BrowserActor second = new BrowserActor(browser);
        BrowserActor third = new BrowserActor(browser)) {
      first.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(first.page, email);
      second.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(second.page, email);
      limit(1);
      first.page.reload();
      signedIn(first.page, email);
      second.page.reload();
      signedIn(second.page, email);
      third.login(email, BrowserTestAccounts.USER_PASSWORD);
      LoginPage login = new LoginPage(third.page);
      login.assertRestrictedManagement();
      limit(3);
      login.cancelRecovery();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(third.page, email);
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
    try (PersistentBrowserActor persistent = new PersistentBrowserActor(playwright, profile)) {
      LoginPage login = new LoginPage(persistent.page);
      login.open();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(persistent.page, email);
    }
    try (BrowserActor next = new BrowserActor(browser)) {
      next.login(email, BrowserTestAccounts.USER_PASSWORD);
      new LoginPage(next.page).assertRestrictedManagement();
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
    try (PersistentBrowserActor persistent = new PersistentBrowserActor(playwright, profile)) {
      LoginPage login = new LoginPage(persistent.page);
      login.open();
      persistent
          .page
          .getByRole(
              AriaRole.CHECKBOX,
              new Page.GetByRoleOptions()
                  .setName(texts.getLoginRememberMeCheckbox())
                  .setExact(true))
          .check();
      login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(persistent.page, email);
      assertTrue(
          persistent.context.cookies().stream()
              .anyMatch(cookie -> cookie.name.equals("remember-me")));
    }
    try (BrowserActor existing = new BrowserActor(browser)) {
      existing.login(email, BrowserTestAccounts.USER_PASSWORD);
      signedIn(existing.page, email);
      try (PersistentBrowserActor persistent =
          new PersistentBrowserActor(playwright, profile, LOGIN_ROUTE)) {
        LoginPage login = new LoginPage(persistent.page);
        login.assertSessionLimit();
        assertFalse(
            persistent.context.cookies().stream()
                .anyMatch(cookie -> cookie.name.equals("remember-me")));
        existing.page.reload();
        signedIn(existing.page, email);
        limit(3);
        login.dismissFailure();
        login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
        signedIn(persistent.page, email);
      }
    }
  }
}
