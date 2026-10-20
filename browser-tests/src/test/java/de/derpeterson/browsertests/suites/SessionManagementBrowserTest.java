package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitUntilState;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AccountSessionsPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UiActions;
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
@Feature("Eigene Sitzungen und gespeicherte Anmeldungen verwalten")
public class SessionManagementBrowserTest extends BrowserFixture {
  private ConfigurationPage configuration;
  private String original;
  @TempDir Path profile;

  @BeforeEach
  void prepare() {
    LoginPage login = new LoginPage(page);
    login.open();
    page.getByRole(
            AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName(texts.getLoginRememberMeCheckbox()).setExact(true))
        .check();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    configuration = new ConfigurationPage(page);
    configuration.open(AdminConfigGroup.SECURITY);
    original = configuration.value(texts.getAdminConfigEntryMaxSessionsPerUser());
    limit(5);
  }

  @AfterEach
  void restore() {
    if (original != null) {
      // Neustartfälle haben auch das Admin-Fixture verloren: frisches Dokument vor Navigation.
      page.navigate(path(ADMIN_ROUTE));
      new UserMenu(page).selectTestLanguage();
      configuration.open(AdminConfigGroup.SECURITY);
      configuration.enter(texts.getAdminConfigEntryMaxSessionsPerUser(), original);
      configuration.save();
    }
  }

  private void limit(int value) {
    configuration.open(AdminConfigGroup.SECURITY);
    configuration.enter(texts.getAdminConfigEntryMaxSessionsPerUser(), String.valueOf(value));
    configuration.save();
  }

  private String user() {
    String email = "own-sessions-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }

  private void login(BrowserActor actor, String email, boolean remember) {
    actor.login(email, BrowserTestAccounts.USER_PASSWORD, remember);
    assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(actor.page).selectTestLanguage();
  }

  private void revoked(Page target) {
    target.navigate(
        path(LOGIN_ROUTE),
        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    new LoginPage(target).assertFormVisible();
  }

  @Test
  @DisplayName("SESSIONS-01: Echte Anzahl, aktuelle Sitzung, Tabs und geändertes Limit anzeigen")
  @Description(
      """
      1. Eigenes Konto anlegen und in zwei unabhängigen Browserkontexten anmelden.
      2. Feldfehler im ersten Fenster und Speichererfolg im zweiten prüfen: Hinweise beeinflussen sich nicht.
      3. Kontositzungen öffnen: genau zwei aktive Zeilen, aktuelle Sitzung deaktiviert.
      4. Weiteren Tab im selben Kontext öffnen: kein zusätzlicher Platz.
      5. Limit im Adminformular ändern und Sitzungsliste aktualisieren; Sitzungs-Tab per Reload erhalten.
      **Erwartet:** Echte Daten, keine Beispielgeräte, sofort aktuelles Limit und eine gemeinsame Tabsitzung.
      """)
  void realListCountsTabsOnceAndReadsChangedLimit() {
    String email = user();
    try (BrowserActor owner = new BrowserActor(browser);
        BrowserActor other = new BrowserActor(browser)) {
      login(owner, email, false);
      login(other, email, true);
      // Ein Fehler im ersten Fenster bleibt bestehen, wenn ein anderes Fenster eine Meldung zeigt.
      AccountPage ownerForm = new AccountPage(owner.page);
      ownerForm.open();
      ownerForm.input("account-first-name").fill("");
      ownerForm.save();
      ownerForm.assertMessage(texts.getBaseValidationRequiredMessage());
      AccountPage otherForm = new AccountPage(other.page);
      otherForm.open();
      otherForm.language(texts.language());
      otherForm.save();
      otherForm.assertMessage(texts.getAccountSettingsSaveSuccess());
      ownerForm.assertMessage(texts.getBaseValidationRequiredMessage());
      otherForm.closeNotification();
      ownerForm.assertMessage(texts.getBaseValidationRequiredMessage());
      ownerForm.closeNotification();
      AccountSessionsPage sessions = new AccountSessionsPage(owner.page);
      sessions.open();
      sessions.assertCounts(2, 1, 0);
      try (Page tab = owner.context.newPage()) {
        tab.navigate(path(HOME_ROUTE));
        new UserMenu(tab).selectTestLanguage();
        sessions.refresh();
        sessions.assertCounts(2, 1, 0);
      }
      limit(7);
      sessions.refresh();
      assertThat(owner.page.locator("#account-sessions-limit"))
          .hasText(texts.getAccountSettingsSessionsLimit(7));
      owner.page.reload();
      assertThat(owner.page).hasURL(urlPattern(ACCOUNT_ROUTE, "\\?tab=sessions"));
      assertThat(owner.page.locator("#account-tab-sessions")).hasAttribute("selected", "");
      sessions.assertCounts(2, 1, 0);
    }
  }

  @Test
  @DisplayName(
      "SESSIONS-02: Einzelwiderruf sperrt alte Speichern-Aktion und Remember-me, erhält andere Sitzung")
  @Description(
      """
      1. Eigenes Konto zweimal anmelden; zweite Sitzung mit Remember-me und offenem Profilentwurf.
      2. Nur diese andere Sitzung im Kontopanel abmelden.
      3. Alten Speichern-Button anklicken: echte HTTP-401 vor der Fachaktion.
      4. Aktuelle Sitzung bleibt nutzbar, gespeicherte Anmeldung kehrt nicht zurück; Entwurf bleibt ungespeichert.
      **Erwartet:** Gezielter Widerruf mit Erfolgsmeldung und sofort freiem Platz.
      """)
  void selectedRevokeBlocksOldFormAndSavedReturn() {
    String email = user();
    try (BrowserActor owner = new BrowserActor(browser);
        BrowserActor other = new BrowserActor(browser)) {
      login(owner, email, false);
      login(other, email, true);
      AccountPage stale = new AccountPage(other.page);
      stale.open();
      String originalName = stale.input("account-first-name").inputValue();
      stale.input("account-first-name").fill("MustNotBeSaved");
      AccountSessionsPage sessions = new AccountSessionsPage(owner.page);
      sessions.open();
      sessions.assertCounts(2, 1, 0);
      sessions.revokeOther(0);
      new AccountPage(owner.page).assertMessage(texts.getSessionRevoked());
      sessions.assertCounts(1, 0, 0);
      Response denied =
          other.page.waitForResponse(
              response ->
                  response.status() == 401
                      && response.url().startsWith(System.getProperty("browser.tests.baseUrl")),
              () -> stale.host("account-save").click());
      assertEquals(401, denied.status());
      revoked(other.page);
      UiActions.complete(owner.page, () -> owner.page.locator("#account-tab-profile").click());
      assertThat(new AccountPage(owner.page).input("account-first-name")).hasValue(originalName);
      owner.page.reload();
      assertThat(new AccountPage(owner.page).input("account-email")).hasValue(email);
      assertThat(new AccountPage(owner.page).input("account-first-name")).hasValue(originalName);
    }
  }

  @Test
  @DisplayName(
      "SESSIONS-03: Alle anderen abmelden und aktuelle Sitzung samt gespeichertem Zugang behalten")
  @Description(
      """
      1. Eigenes Konto in drei unabhängigen Sitzungen anmelden, aktuelle und eine andere mit Remember-me.
      2. Alle anderen abmelden: nur die aktuelle Zeile bleibt, Sammelaktion ist deaktiviert.
      3. Beide anderen Sitzungen verlieren Zugriff und gespeicherte Rückkehr.
      4. Aktuelle Sitzung bleibt nach Reload angemeldet und ihr eigener gespeicherter Zugang erhalten.
      **Erwartet:** Keine pauschale Löschung des aktuellen gespeicherten Zugangs.
      """)
  void logoutOthersKeepsCurrentSessionAndItsSavedSignIn() {
    String email = user();
    try (BrowserActor owner = new BrowserActor(browser);
        BrowserActor second = new BrowserActor(browser);
        BrowserActor third = new BrowserActor(browser)) {
      login(owner, email, true);
      login(second, email, true);
      login(third, email, false);
      AccountSessionsPage sessions = new AccountSessionsPage(owner.page);
      sessions.open();
      sessions.assertCounts(3, 2, 0);
      sessions.revokeOthers();
      sessions.assertCounts(1, 0, 0);
      assertThat(owner.page.locator("#account-logout-others")).isDisabled();
      revoked(second.page);
      revoked(third.page);
      owner.page.reload();
      sessions.assertCounts(1, 0, 0);
      assertTrue(
          owner.context.cookies().stream().anyMatch(cookie -> cookie.name.equals("remember-me")));
    }
  }

  @Test
  @DisplayName(
      "SESSIONS-04: Aktive Sitzung samt gespeichertem Zugang nach echtem Neustart widerrufen")
  @Description(
      """
      1. Eigenes Konto im temporären Chromium-Profil mit Remember-me anmelden und Prozess schließen.
      2. In zweitem Kontext anmelden; nur die eigene Testanwendung neu starten.
      3. Zweite Anmeldung wiederherstellen: zwei erhaltene aktive Sitzungen, keine zusätzliche gespeicherte Zeile.
      4. Andere aktive Sitzung widerrufen; alten Chromium-Prozess mit seinem Profil neu öffnen.
      5. Erneut den Server starten: Widerruf bleibt erhalten und es gibt genau einen aktiven Platz.
      **Erwartet:** Neustart erhält aktive Geräte; Widerruf entfernt auch ihren gespeicherten Zugang.
      """)
  void activeSignInSurvivesRestartAndItsSavedReturnCanBeRevoked() throws Exception {
    String email = user();
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
      assertThat(persistent.page).hasURL(urlPattern(HOME_ROUTE));
    }
    try (BrowserActor owner = new BrowserActor(browser)) {
      login(owner, email, true);
      BrowserTestRunner.restartTestApplication();
      owner.page.reload();
      assertThat(owner.page).hasURL(urlPattern(HOME_ROUTE));
      new UserMenu(owner.page).selectTestLanguage();
      AccountSessionsPage sessions = new AccountSessionsPage(owner.page);
      sessions.open();
      sessions.assertCounts(2, 1, 0);
      sessions.revokeOther(0);
      sessions.assertCounts(1, 0, 0);
      BrowserTestRunner.restartTestApplication();
      owner.page.reload();
      sessions.open();
      sessions.assertCounts(1, 0, 0);
      try (PersistentBrowserActor persistent = new PersistentBrowserActor(playwright, profile)) {
        LoginPage login = new LoginPage(persistent.page);
        login.open();
        login.assertFormVisible();
      }
    }
    // Auch der Admin hatte einen eigenen gespeicherten Zugang; nur über UI wiederherstellen.
    page.navigate(path(ADMIN_ROUTE));
    new UserMenu(page).selectTestLanguage();
    new LoginPage(page).assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
  }

  @Test
  @DisplayName("SESSIONS-06: Neustart verwirft alte UI-Aktionen und erhält Sitzungsgrenzen")
  @Description(
      """
      1. Eigenes Konto ohne Remember-me anmelden; Limit eins und ungespeicherten Profilentwurf setzen.
      2. Nur den eigenen Testserver neu starten und den alten Speichern-Button verwenden.
      3. Ein frisches Dokument wird angezeigt; Konto öffnen und unveränderten Namen prüfen.
      4. Zweites Gerät muss weiterhin in die eingeschränkte Verwaltung; bestehender Platz zählt.
      5. Alten Platz widerrufen und fortsetzen; nach weiterem Neustart bleibt nur die neue Sitzung.
      **Erwartet:** Kein Replay alter RPCs, kein gespeicherter Entwurf und kein Limit-Bypass durch Neustart.
      """)
  void restartRebuildsUiWithoutReplayingDraftsOrLosingTheSlot() throws Exception {
    String email = user();
    limit(1);
    try (BrowserActor owner = new BrowserActor(browser);
        BrowserActor next = new BrowserActor(browser)) {
      login(owner, email, false);
      AccountPage form = new AccountPage(owner.page);
      form.open();
      String originalName = form.input("account-first-name").inputValue();
      form.input("account-first-name").fill("MustNotSurviveRestart");
      BrowserTestRunner.restartTestApplication();
      form.host("account-save").click();
      assertThat(owner.page).hasURL(urlPattern(HOME_ROUTE));
      form.open();
      assertThat(form.input("account-first-name")).hasValue(originalName);
      next.login(email, BrowserTestAccounts.USER_PASSWORD);
      LoginPage recovery = new LoginPage(next.page);
      recovery.assertRestrictedManagement();
      recovery.revokeRecoverySession(0);
      UiActions.complete(next.page, () -> next.page.locator("#session-recovery-continue").click());
      assertThat(next.page).hasURL(urlPattern(HOME_ROUTE));
      BrowserTestRunner.restartTestApplication();
      next.page.reload();
      AccountSessionsPage sessions = new AccountSessionsPage(next.page);
      sessions.open();
      sessions.assertCounts(1, 0, 0);
      revoked(owner.page);
    }
  }

  @Test
  @DisplayName(
      "SESSIONS-05: Bei erreichtem Limit nur eigene Sitzungen verwalten und Anmeldung fortsetzen")
  @Description(
      """
      1. Eigenes Konto anlegen, Limit 1 speichern und auf einem Gerät mit Remember-me anmelden.
      2. Neues Gerät mit korrektem Passwort anmelden: eingeschränkte Liste ohne normale Anwendungsrechte.
      3. Geschützte Account-URL führt weiter zur Loginverwaltung; andere Sitzung widerrufen.
      4. Anmeldung fortsetzen: normale Zulassung, eigene aktuelle Sitzung, neuer Remember-me-Zugang.
      **Erwartet:** Vor Fortsetzen kein normaler Zugriff und kein belegter zusätzlicher Platz.
      """)
  void reachedLimitCanBeResolvedWithoutGrantingNormalAccessEarly() {
    String email = user();
    limit(1);
    try (BrowserActor owner = new BrowserActor(browser);
        BrowserActor next = new BrowserActor(browser)) {
      login(owner, email, true);
      next.login(email, BrowserTestAccounts.USER_PASSWORD, true);
      LoginPage login = new LoginPage(next.page);
      login.assertRestrictedManagement();
      assertFalse(
          next.context.cookies().stream().anyMatch(cookie -> cookie.name.equals("remember-me")));
      next.page.navigate(path(ACCOUNT_ROUTE));
      login.assertRestrictedManagement();
      assertThat(next.page.locator("#account-session-entries [data-session-state='active']"))
          .hasCount(1);
      assertThat(next.page.locator("#account-session-entries [data-session-state='current']"))
          .hasCount(0);
      UiActions.complete(
          next.page, () -> next.page.locator("#account-session-entries vaadin-button").click());
      assertThat(next.page.locator("#account-session-entries [data-session-state='active']"))
          .hasCount(0);
      UiActions.complete(next.page, () -> next.page.locator("#session-recovery-continue").click());
      assertThat(next.page).hasURL(urlPattern(HOME_ROUTE));
      new UserMenu(next.page).selectTestLanguage();
      assertTrue(
          next.context.cookies().stream().anyMatch(cookie -> cookie.name.equals("remember-me")));
      revoked(owner.page);
      AccountSessionsPage sessions = new AccountSessionsPage(next.page);
      sessions.open();
      sessions.assertCounts(1, 0, 0);
    }
  }
}
