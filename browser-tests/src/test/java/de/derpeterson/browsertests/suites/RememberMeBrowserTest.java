package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Remember-me mit echtem Browserneustart")
public class RememberMeBrowserTest extends BrowserFixture {
  @TempDir Path profile;

  @Test
  @DisplayName("AUTH-07A: Ohne Remember-me bleibt nach Prozessneustart keine Anmeldung erhalten")
  @Description(
      """
        1. Eigenes aktives Konto über das Adminformular anlegen.
        2. In eigenem temporären Chromium-Profil anmelden, Remember-me nicht markieren.
        3. Identität prüfen und den gesamten separaten Browserprozess schließen.
        4. Denselben Profilordner mit neuem Browserprozess öffnen: Gastmenü ohne eigene Identität.
        **Erwartet:** Kein dauerhaftes Remember-me-Cookie und keine wiederhergestellte Anmeldung.
        """)
  void ordinarySessionDoesNotSurviveBrowserRestart() {
    String email = createUser();
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      signIn(actor, email, false);
      assertTrue(
          actor.context.cookies().stream().noneMatch(cookie -> cookie.name.equals("remember-me")));
    }
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      assertAnonymous(actor.page, email);
    }
  }

  @Test
  @DisplayName("AUTH-07B: Remember-me überlebt Browserneustart; Logout beendet es dauerhaft")
  @Description(
      """
        1. Eigenes Konto im separaten temporären Chromium-Profil mit Checkbox Remember-me anmelden.
        2. Dauerhaftes Cookie nur auf Namen/Laufzeit prüfen; Prozess vollständig schließen.
        3. Echten neuen Prozess mit demselben Profil öffnen: eigene normale Identität wiederhergestellt.
        4. Über die Oberfläche abmelden, Prozess schließen und nochmals starten: Gast.
        **Erwartet:** Anmelden und Abmelden wirken über echte Neustarts; keine Cookie-/Storage-Injektion.
        """)
  void rememberMeSurvivesRestartUntilLogout() {
    String email = createUser();
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      signIn(actor, email, true);
      assertTrue(
          actor.context.cookies().stream()
              .anyMatch(
                  cookie ->
                      cookie.name.equals("remember-me")
                          && cookie.secure
                          && cookie.httpOnly
                          && cookie.sameSite
                              == com.microsoft.playwright.options.SameSiteAttribute.LAX
                          && cookie.expires > System.currentTimeMillis() / 1000.0),
          "Dauerhaftes Remember-me-Cookie fehlt.");
    }
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
      new UserMenu(actor.page).requestSignOut();
      actor.context.waitForCondition(
          () ->
              actor.context.cookies().stream()
                  .noneMatch(cookie -> cookie.name.equals("remember-me")));
      // A normal user's public Home may remain the logout destination; prove anonymous access
      // instead.
      actor.page.navigate(path(HOME_ROUTE));
      new UserMenu(actor.page).selectTestLanguage();
      assertAnonymous(actor.page, email);
    }
    try (PersistentBrowserActor actor = new PersistentBrowserActor(playwright, profile)) {
      assertAnonymous(actor.page, email);
    }
  }

  @ParameterizedTest
  @CsvSource({"false, AUTH-07C", "true, AUTH-07D"})
  @DisplayName("AUTH-07C/D: Anmeldung nach echtem Anwendungsneustart")
  @Description(
      """
      1. Eigenes Konto über die Benutzerverwaltung erstellen und mit/ohne Remember-me anmelden.
      2. Zweiten angemeldeten Tab im selben Kontext öffnen und nur die eigene Testanwendung neu starten.
      3. Beide Tabs neu laden: dieselbe Identität bleibt mit und ohne Remember-me erhalten.
      4. Abmelden, Anwendung erneut starten und beide Tabs laden: keine Anmeldung mehr.
      **Erwartet:** Serverneustart erhält den bestehenden Sitzungsplatz, Logout beendet ihn dauerhaft.
      Browserprozess-Neustarts ohne Remember-me bleiben gesondert anonym.
      """)
  void serverRestartRestoresBothLoginKinds(boolean remember, String caseId) throws Exception {
    String email = createUser();
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.login(email, BrowserTestAccounts.USER_PASSWORD, remember);
      new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
      Page second = actor.context.newPage();
      second.navigate(path(HOME_ROUTE));
      new UserMenu(second).assertUserSignedInWithoutAdminAction(email);
      BrowserTestRunner.restartTestApplication();
      actor.page.reload();
      second.reload();
      new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
      new UserMenu(second).assertUserSignedInWithoutAdminAction(email);
      if (remember) {
        assertTrue(
            actor.context.cookies().stream().anyMatch(cookie -> cookie.name.equals("remember-me")));
      }
      new UserMenu(actor.page).requestSignOut();
      BrowserTestRunner.restartTestApplication();
      actor.page.navigate(path(HOME_ROUTE));
      second.navigate(path(HOME_ROUTE));
      new UserMenu(actor.page).selectTestLanguage();
      assertAnonymous(actor.page, email);
      assertAnonymous(second, email);
    }
  }

  private void signIn(PersistentBrowserActor actor, String email, boolean remember) {
    LoginPage login = new LoginPage(actor.page);
    login.open();
    actor
        .page
        .getByRole(
            AriaRole.CHECKBOX,
            new Page.GetByRoleOptions().setName(texts.getLoginRememberMeCheckbox()).setExact(true))
        .setChecked(remember);
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(actor.page).selectTestLanguage();
    new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
  }

  private void assertAnonymous(Page page, String email) {
    new UserMenu(page).open();
    assertThat(page.getByText(email, new Page.GetByTextOptions().setExact(true))).hasCount(0);
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(texts.getBaseLoginButton()).setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(texts.getBaseLogoutButton()).setExact(true)))
        .hasCount(0);
  }

  private String createUser() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    String email = "remember-" + UUID.randomUUID() + "@example.test";
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }
}
