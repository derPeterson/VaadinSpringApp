package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Bestehende Sitzung nach administrativer Änderung")
public class SessionSecurityBrowserTest extends BrowserFixture {
  @ParameterizedTest(name = "{0}: Altes Adminformular nach {1}, Remember-me={2}")
  @CsvSource({
    "SESSION-01A,role,false",
    "SESSION-01B,disable,false",
    "SESSION-01C,delete,false",
    "SESSION-01D,disable,true",
    "SESSION-01E,delete,true"
  })
  @Description(
      """
        1. Eigenes zweites Administratorkonto über die Oberfläche anlegen und separat anmelden.
        2. Dort ein neues Benutzerformular vollständig ausfüllen, noch nicht speichern; Sperre/Löschung auch mit Remember-me.
        3. Als ursprünglicher Admin Rolle entziehen, Konto sperren bzw. löschen.
        4. Rollenentzug: automatische Weiterleitung auf Home bei erhaltenem Login, altes Formular verschwindet. Bei Sperre/Löschung alten Speichern-Button anklicken: 401 prüfen.
        5. Bei Sperre/Löschung geschützte Seite neu aufrufen: Login. Als ursprünglicher Admin nach dem Probe-Konto suchen.
        **Erwartet:** Der alte privilegierte Callback wird verweigert, Probe-Konto fehlt tatsächlich.
        Keine synthetischen RPCs; bereits laufende Aktionen sind kein rückwirkender Widerruf.
        """)
  void oldPrivilegedCallbackIsRejected(String caseId, String change, boolean remember) {
    AdminUsersPage users = admin();
    String account = email("session"), probe = email("blocked-create");
    users.createUser(account, BrowserTestAccounts.USER_PASSWORD, true, true);
    try (BrowserActor actor = new BrowserActor(browser)) {
      AdminUsersPage other = actor.loginAdmin(account, BrowserTestAccounts.USER_PASSWORD, remember);
      other.open();
      AdminUserDialog stale = other.openCreate();
      stale.fillNewUser(probe, BrowserTestAccounts.USER_PASSWORD, true, false);
      AdminUserDialog current = users.openEdit(account);
      switch (change) {
        case "role" -> {
          current.setAdministrator(false);
          current.save();
        }
        case "disable" -> {
          current.setEnabled(false);
          current.save();
        }
        case "delete" -> current.requestDelete().confirm();
        default -> throw new IllegalArgumentException(change);
      }
      if (change.equals("role")) {
        // Rollenwechsel erhalten den Login; Push verwirft die alte privilegierte Ansicht.
        assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
        assertThat(actor.page.locator("vaadin-dialog-overlay[opened]")).hasCount(0);
        var menu = new UserMenu(actor.page);
        menu.assertUserSignedInWithoutAdminAction(account);
        menu.close();
        users.open();
        users.search(probe);
        assertThat(users.emailCells(probe)).hasCount(0);
        return;
      }
      Response denied =
          actor.page.waitForResponse(
              response ->
                  response.status() == 401
                      && response.url().startsWith(System.getProperty("browser.tests.baseUrl")),
              stale.saveButton()::click);
      assertEquals(401, denied.status());
      // Nach der 401 können alte Push-Anfragen noch auslaufen. Das neue Dokument
      // und die sichtbare Loginmaske belegen die Abwehr, nicht das globale load-Ereignis.
      actor.page.navigate(
          path(ADMIN_ROUTE),
          new Page.NavigateOptions()
              .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
      new LoginPage(actor.page).assertFormVisible();
      users.open();
      users.search(probe);
      assertThat(users.emailCells(probe)).hasCount(0);
    }
  }

  private AdminUsersPage admin() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    return new AdminUsersPage(page);
  }

  private String email(String purpose) {
    return purpose + "-" + UUID.randomUUID() + "@example.test";
  }
}
