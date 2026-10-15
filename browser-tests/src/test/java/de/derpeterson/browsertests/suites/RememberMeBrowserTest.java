package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

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
