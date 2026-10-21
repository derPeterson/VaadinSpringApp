package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppConstants.USER_ICON_ALT;
import static de.derpeterson.app.config.AppRouteConstants.ACCOUNT_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Independent browser contexts per test; each case can run without preceding cases. */
@Tag("browser")
@Feature("Anmeldung und Zugriffsrechte")
public class AuthenticationBrowserTest extends BrowserFixture {
  @Tag("fast")
  @Test
  @DisplayName("AUTH-01A: Admin meldet sich an und erreicht das Dashboard")
  @Story("Admin-Anmeldung")
  @Description(
      """
            1. In einer frischen Browsersitzung das Loginformular öffnen.
            2. Mit dem aktiven Bootstrap-Testadmin anmelden.
            3. Dashboard und angezeigte Konto-E-Mail prüfen.

            **Erwartet:** Erfolgreiche Anmeldung; Admin-Dashboard und korrektes Konto sichtbar.
            Prüft keine nachträgliche Vergabe einer Adminrolle.
            """)
  void adminCanSignIn() {
    signInAsAdmin();
  }

  @Tag("fast")
  @Test
  @DisplayName("AUTH-01B: Falsches Passwort wird verständlich abgelehnt")
  @Story("Abgelehnte Anmeldung")
  @Description(
      """
            1. In einer frischen Browsersitzung das Loginformular öffnen.
            2. Die bekannte Admin-E-Mail mit einem falschen Passwort absenden.
            3. Fehlermeldung prüfen und anschließend /admin direkt aufrufen.

            **Erwartet:** Allgemeiner Anmeldefehler; keine Anmeldung und kein Adminzugriff.
            """)
  void wrongPasswordIsRejected() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, "DefinitelyWrong1!");
    login.assertRejected();
    page.navigate(path(ADMIN_ROUTE));
    login.assertFormVisible();
  }

  @Tag("fast")
  @Test
  @DisplayName("AUTH-01C: Unbekanntes Konto erhält denselben Anmeldefehler")
  @Story("Abgelehnte Anmeldung")
  @Description(
      """
            1. In einer frischen Browsersitzung das Loginformular öffnen.
            2. Eine syntaktisch gültige, nicht angelegte Testadresse absenden.
            3. Allgemeine Fehlermeldung und fehlenden Adminzugriff prüfen.

            **Erwartet:** Derselbe verständliche Fehler wie bei falschem Passwort; keine Anmeldung.
            """)
  void unknownUserIsRejected() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn("unknown-" + UUID.randomUUID() + "@example.test", "UnknownUser1!");
    login.assertRejected();
    page.navigate(path(ADMIN_ROUTE));
    login.assertFormVisible();
  }

  @Tag("fast")
  @Test
  @DisplayName("AUTH-01D: Anonyme Sitzung kann den Adminbereich nicht öffnen")
  @Story("Anonymer Zugriff")
  @Description(
      """
            1. Ohne Anmeldung /admin direkt öffnen.
            2. Loginformular und Abwesenheit des Admin-Dashboards prüfen.

            **Erwartet:** Weiterleitung zur Anmeldung; keine Admininhalte.
            """)
  void anonymousUserCannotOpenAdmin() {
    page.navigate(path(ADMIN_ROUTE));
    new LoginPage(page).assertFormVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(texts.getAdminDashboardTitle()).setExact(true)))
        .hasCount(0);
  }

  @Test
  @DisplayName("AUTH-01E: Konto mit maximal langer E-Mail bleibt nach Neuladen angemeldet")
  @Story("Anmeldung an der Identitätsgrenze")
  @Description(
      """
      1. Als Administrator ein aktives Testkonto mit einer gültigen 255-Zeichen-Adresse anlegen.
      2. Abmelden und über das normale Formular mit diesem Konto anmelden.
      3. Home und die genaue Identität im Kontomenü prüfen; die Seite neu laden.
      4. Weiterhin angemeldetes Konto prüfen, abmelden und den geschützten Kontobereich aufrufen.
      **Erwartet:** Die gesamte erlaubte Kontoadresse passt auch in die dauerhafte
      Sitzung; Neuladen erhält die Anmeldung, nach Logout verlangt der Kontobereich erneut Login.
      """)
  void maximumAccountIdentityCanSignInAndSurviveReload() {
    signInAsAdmin();
    String email =
        UUID.randomUUID().toString()
            + "a".repeat(28)
            + "@"
            + "b".repeat(63)
            + "."
            + "c".repeat(63)
            + "."
            + "d".repeat(62);
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).signOut();
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    assertThat(page).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(page).assertUserSignedInWithoutAdminAction(email);
    page.reload();
    assertThat(page).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(page).assertUserSignedInWithoutAdminAction(email);
    new UserMenu(page).requestSignOut();
    page.navigate(path(ACCOUNT_ROUTE));
    login.assertFormVisible();
  }

  @Tag("fast")
  @Test
  @DisplayName("AUTH-02A: Logout entzieht den Zugriff auch einem bestehenden Tab")
  @Story("Abmeldung und bestehende Tabs")
  @Description(
      """
            1. Als Testadmin anmelden und /admin in einem zweiten Tab derselben Sitzung öffnen.
            2. Im ersten Tab über das Benutzermenü abmelden.
            3. Im zweiten Tab eine Navigation auslösen; anschließend /admin neu laden.
            4. Im ersten Tab Browser-Zurück und anschließend /admin erneut öffnen.

            **Erwartet:** Nach Logout kein neuer Adminzugriff; beide Tabs benötigen Anmeldung.
            Eine gecachte Ansicht zählt nicht als Zugriff. Mutationen aus einem alten offenen
            Formular und Rollenentzug über Push werden damit noch nicht vollständig geprüft.
            """)
  void logoutEndsAccessInBothTabs() {
    signInAsAdmin();
    Page otherTab = context.newPage();
    otherTab.navigate(path(ADMIN_ROUTE));
    new LoginPage(otherTab).assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    new UserMenu(page).signOut();
    com.microsoft.playwright.Locator staleMenu =
        otherTab.getByRole(
            AriaRole.BUTTON, new Page.GetByRoleOptions().setName(USER_ICON_ALT).setExact(true));
    if (staleMenu.isVisible()) {
      staleMenu.click();
    } else {
      // Sitzungsende darf die zweite UI bereits vor ihrer nächsten Aktion abmelden.
      new LoginPage(otherTab).assertFormVisible();
    }
    otherTab.navigate(path(ADMIN_ROUTE));
    new LoginPage(otherTab).assertFormVisible();
    page.goBack();
    new LoginPage(page).assertFormVisible();
    page.navigate(path(ADMIN_ROUTE));
    new LoginPage(page).assertFormVisible();
    otherTab.close();
  }

  @Tag("fast")
  @Test
  @DisplayName("SEC-01A: Normaler Benutzer kann sich anmelden, aber keine Adminfunktionen nutzen")
  @Story("Zugriffsrechte normaler Benutzer")
  @Description(
      """
            1. Als Admin ein aktives Testkonto nur mit Benutzerrolle über die Oberfläche anlegen.
            2. Abmelden und das neue Konto über das Loginformular anmelden.
            3. Angemeldetes Konto und fehlende Adminaktion im Menü prüfen.
            4. /admin direkt aufrufen und Zugriffsverweigerung prüfen.

            **Erwartet:** Normale Anmeldung funktioniert; Adminbereich bleibt gesperrt und
            zeigt einen verständlichen Hinweis. Benutzeranlage dient der Testvorbereitung;
            die vollständige Benutzerverwaltung wird noch nicht geprüft.
            """)
  void ordinaryUserHasNoAdminAccess() {
    String email = "browser-user-" + UUID.randomUUID() + "@example.test";
    signInAsAdmin();
    new AdminUsersPage(page).createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(page).signOut();
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(email, BrowserTestAccounts.USER_PASSWORD);
    assertThat(page).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(page).selectTestLanguage();
    new UserMenu(page).assertUserSignedInWithoutAdminAction(email);
    page.navigate(path(ADMIN_ROUTE));
    assertThat(page).hasURL(urlPattern(HOME_ROUTE));
    assertThat(
            page.getByText(
                texts.getBaseErrorAdminAccessDenied(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(
            page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(texts.getAdminDashboardTitle()).setExact(true)))
        .hasCount(0);
  }

  private void signInAsAdmin() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
    new UserMenu(page).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
  }
}
