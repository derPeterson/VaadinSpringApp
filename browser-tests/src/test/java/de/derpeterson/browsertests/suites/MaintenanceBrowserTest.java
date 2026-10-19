package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.FORGOT_PASSWORD_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.LOGIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.MAINTENANCE_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.REGISTRATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.PersistentBrowserActor;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfigurationPage;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.ResetPasswordPage;
import de.derpeterson.browsertests.pages.UserMenu;
import de.derpeterson.browsertests.pages.VerificationPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("browser")
@Feature("Wartungsmodus")
public class MaintenanceBrowserTest extends BrowserFixture {
  private ConfigurationPage settings;
  private boolean originalMode;
  private String originalLimit;
  @TempDir Path profile;

  @BeforeEach
  void prepare() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    settings = new ConfigurationPage(page);
    settings.open(AdminConfigGroup.SECURITY);
    originalMode = settings.input(texts.getAdminConfigEntryMaintenanceMode()).isChecked();
    originalLimit = settings.value(texts.getAdminConfigEntryMaxSessionsPerUser());
  }

  @AfterEach
  void restore() {
    if (settings != null) {
      settings.open(AdminConfigGroup.SECURITY);
      settings.input(texts.getAdminConfigEntryMaintenanceMode()).setChecked(originalMode);
      if (originalLimit != null) {
        settings.enter(texts.getAdminConfigEntryMaxSessionsPerUser(), originalLimit);
      }
      settings.save();
    }
  }

  private void mode(boolean active) {
    settings.open(AdminConfigGroup.SECURITY);
    settings.input(texts.getAdminConfigEntryMaintenanceMode()).setChecked(active);
    settings.save();
  }

  private String email() {
    return "maintenance-" + UUID.randomUUID() + "@example.test";
  }

  private String user(String role) {
    String email = email();
    AdminUsersPage users = new AdminUsersPage(page);
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    if (!role.equals("user")) {
      AdminUserDialog form = users.openEdit(email);
      form.setRole(texts.getRoleUser(), false);
      form.setRole(
          switch (role) {
            case "admin" -> texts.getRoleAdmin();
            case "superadmin" -> texts.getRoleSuperadmin();
            default -> texts.getRoleGuest();
          },
          true);
      form.save();
    }
    return email;
  }

  private void maintenance(Page target) {
    assertThat(
            target.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(texts.getMaintenanceTitle()).setExact(true)))
        .isVisible();
    assertThat(
            target.getByText(
                texts.getMaintenanceDescription(), new Page.GetByTextOptions().setExact(true)))
        .isVisible();
  }

  @Test
  @DisplayName("MAINT-01: Öffentliche Dokumente liefern 503; bestehende Sitzung bleibt erhalten")
  @Description(
      """
      1. Eigenen Benutzer anmelden; Admin schaltet Wartung über die Konfiguration ein.
      2. Neuer anonymer Kontext ruft Home, Registrierung, Passwortanfrage und Admin direkt auf.
      3. HTTP 503, lokalisierte Wartungscard und weiterhin geladene Cosmos-Ressourcen prüfen.
      4. Bestehenden Benutzer neu laden; nur Abmelden und erneuten Versuch als Aktionen anbieten.
      5. Wartung ausschalten und dieselbe unveränderte Sitzung wieder verwenden.
      **Erwartet:** Keine normalen Inhalte während Wartung, keine dauerhafte Abmeldung durch den Schalter.
      """)
  void documentsAndExistingSession() {
    String account = user("user");
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.login(account, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.page).selectTestLanguage();
      Cookie sessionCookie =
          actor.context.cookies().stream()
              .filter(c -> c.name.equals("JSESSIONID"))
              .findFirst()
              .orElseThrow();
      mode(true);
      try (BrowserActor anonymous = new BrowserActor(browser, MAINTENANCE_PAGE_TITLE)) {
        for (String route :
            new String[] {HOME_ROUTE, REGISTRATION_ROUTE, FORGOT_PASSWORD_ROUTE, ADMIN_ROUTE}) {
          Response response = anonymous.page.navigate(path(route));
          assertNotNull(response);
          assertEquals(503, response.status());
          maintenance(anonymous.page);
          assertThat(
                  anonymous.page.getByRole(
                      AriaRole.BUTTON,
                      new Page.GetByRoleOptions()
                          .setName(texts.getMaintenanceLoginButton())
                          .setExact(true)))
              .isVisible();
          assertThat(
                  anonymous.page.getByRole(
                      AriaRole.BUTTON,
                      new Page.GetByRoleOptions()
                          .setName(texts.getBaseLogoutButton())
                          .setExact(true)))
              .isHidden();
          if (route.equals(HOME_ROUTE)) {
            AllurePlaywright.attachScreenshot("Wartungscard mit HTTP 503", anonymous.page);
          }
          assertThat(anonymous.page.locator(".cosmos-card")).isVisible();
        }
      }
      actor.page.reload();
      maintenance(actor.page);
      assertThat(
              actor.page.getByRole(
                  AriaRole.BUTTON,
                  new Page.GetByRoleOptions()
                      .setName(texts.getMaintenanceLoginButton())
                      .setExact(true)))
          .isHidden();
      assertThat(
              actor.page.getByRole(
                  AriaRole.BUTTON,
                  new Page.GetByRoleOptions().setName(texts.getBaseLogoutButton()).setExact(true)))
          .isVisible();
      assertTrue(
          actor.context.cookies().stream()
              .anyMatch(c -> c.name.equals("JSESSIONID") && c.value.equals(sessionCookie.value)));
      page.navigate(path(ADMIN_ROUTE));
      assertThat(page.locator(".cosmos-sidebar #admin-service-status"))
          .containsText(texts.getMaintenanceStatusActive());
      assertThat(page.locator("#admin-service-status"))
          .containsText(texts.getMaintenanceServicesRestricted());
      assertThat(page.locator("#admin-service-status")).hasAttribute("data-state", "active");
      mode(false);
      assertThat(page.locator("#admin-service-status"))
          .containsText(texts.getMaintenanceStatusOnline());
      assertThat(page.locator("#admin-service-status"))
          .containsText(texts.getMaintenanceServicesReady());
      assertThat(page.locator("#admin-service-status")).hasAttribute("data-state", "open");
      actor.page.navigate(path(HOME_ROUTE));
      new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(account);
    }
  }

  @ParameterizedTest(name = "{0}: Rollenentscheidung während Wartung")
  @CsvSource({
    "MAINT-02A,user,false",
    "MAINT-02B,guest,false",
    "MAINT-02C,admin,true",
    "MAINT-02D,superadmin,true"
  })
  @Description(
      """
      1. Eigenes aktives Konto ausschließlich mit der jeweiligen Rolle über das Benutzerformular anlegen.
      2. Wartung einschalten und mit gültigem Passwort im unabhängigen Browser anmelden.
      3. Benutzer/Gast erhalten Wartungshinweis ohne Remember-me-Cookie; Admin/Superadmin erreichen die Verwaltung.
      4. Wartung ausschalten; abgewiesene Konten können sich wieder anmelden.
      **Erwartet:** Beide administrativen Rollen können den Modus selbst wieder ausschalten.
      """)
  void allRoles(String caseId, String role, boolean administrative) {
    String account = user(role);
    try (BrowserActor actor = new BrowserActor(browser)) {
      mode(true);
      actor.login(account, BrowserTestAccounts.USER_PASSWORD, true);
      if (administrative) {
        assertThat(actor.page).hasURL(urlPattern(ADMIN_ROUTE));
        new UserMenu(actor.page).selectTestLanguage();
        ConfigurationPage ownSettings = new ConfigurationPage(actor.page);
        ownSettings.open(AdminConfigGroup.SECURITY);
        ownSettings.input(texts.getAdminConfigEntryMaintenanceMode()).uncheck();
        ownSettings.save();
      } else {
        assertThat(actor.page.locator("vaadin-notification-card:not([closing])"))
            .containsText(texts.getMaintenanceLoginMessage());
        assertFalse(actor.context.cookies().stream().anyMatch(c -> c.name.equals("remember-me")));
        mode(false);
        LoginPage login = new LoginPage(actor.page);
        login.dismissFailure();
        login.signIn(account, BrowserTestAccounts.USER_PASSWORD);
        assertThat(actor.page).hasURL(urlPattern(HOME_ROUTE));
      }
    }
  }

  @Test
  @DisplayName("MAINT-03: Ein vor Wartung geöffnetes Registrierungsformular legt kein Konto an")
  @Description(
      """
      1. Eigenes anonymes Registrierungsformular vor der Wartung vollständig ausfüllen.
      2. Wartung als Admin einschalten; den alten Registrieren-Button tatsächlich anklicken.
      3. Wartungscard prüfen und als Admin nach der eigenen Adresse suchen.
      **Erwartet:** Kein Konto angelegt und kein Fachcallback aus dem alten Formular ausgeführt.
      """)
  void staleRegistrationCannotCreateAccount() {
    String account = email();
    try (BrowserActor actor = new BrowserActor(browser)) {
      RegistrationPage registration = new RegistrationPage(actor.page);
      registration.open();
      registration.fillValid(account, BrowserTestAccounts.USER_PASSWORD);
      actor.page.locator("vaadin-date-picker input").press("Tab");
      mode(true);
      registration.submit();
      maintenance(actor.page);
      AdminUsersPage users = new AdminUsersPage(page);
      users.open();
      users.search(account);
      assertThat(
              page.locator("vaadin-grid")
                  .getByText(account, new Locator.GetByTextOptions().setExact(true)))
          .hasCount(0);
    }
  }

  @ParameterizedTest(name = "{0}: Unverbrauchter {1}-Link bleibt nach Wartung gültig")
  @CsvSource({"MAINT-04A,verification", "MAINT-04B,reset"})
  @Description(
      """
      1. Eigenen gültigen Aktivierungs- oder Resetlink über UI und tatsächlich empfangene SMTP-Mail erzeugen.
      2. Beim Reset zusätzlich das Passwortformular bereits vor Wartung öffnen und ausfüllen.
      3. Wartung einschalten; alten Resetbutton und direkten Mailbutton prüfen: Wartung statt Fachaktion.
      4. Wartung ausschalten und denselben unverbrauchten Link erfolgreich verwenden.
      **Erwartet:** Weder Tokens verbraucht noch Passwort/Kontofreigabe während Wartung verändert.
      """)
  void realMailLinksAndOldResetForm(String caseId, String kind) {
    boolean reset = kind.equals("reset");
    String account = reset ? user("user") : email();
    try (BrowserActor actor = new BrowserActor(browser)) {
      MailpitInbox inbox = new MailpitInbox(actor.context);
      MailpitInbox.CapturedMail mail;
      Page oldForm = null;
      if (reset) {
        ForgotPasswordPage forgot = new ForgotPasswordPage(actor.page);
        forgot.open();
        forgot.enterEmail(account);
        forgot.submit();
        forgot.requestedResponse();
        mail = inbox.awaitPasswordResetMail(account);
        oldForm = inbox.openPasswordResetAction(mail);
        ResetPasswordPage password = new ResetPasswordPage(oldForm);
        password.assertFormVisible();
        password.fillPasswords("MaintenanceNewPassword!", "MaintenanceNewPassword!");
        oldForm.locator("vaadin-password-field input").last().press("Tab");
      } else {
        RegistrationPage registration = new RegistrationPage(actor.page);
        registration.open();
        registration.fillValid(account, BrowserTestAccounts.USER_PASSWORD);
        registration.submit();
        registration.assertSubmitted(account);
        mail = inbox.awaitVerificationMail(account);
      }
      try {
        mode(true);
        if (oldForm != null) {
          new ResetPasswordPage(oldForm).submit();
          maintenance(oldForm);
        }
        try (Page blocked =
            reset ? inbox.openPasswordResetAction(mail) : inbox.openVerificationAction(mail)) {
          maintenance(blocked);
        }
        mode(false);
        try (Page resumed =
            reset ? inbox.openPasswordResetAction(mail) : inbox.openVerificationAction(mail)) {
          if (reset) {
            ResetPasswordPage password = new ResetPasswordPage(resumed);
            password.assertFormVisible();
            password.fillPasswords("MaintenanceNewPassword!", "MaintenanceNewPassword!");
            password.submit();
            password.assertChanged();
          } else {
            new VerificationPage(resumed).assertActivated();
          }
        }
      } finally {
        if (oldForm != null) {
          oldForm.close();
        }
      }
    }
  }

  @Test
  @DisplayName("MAINT-05: Echter Remember-me-Browserneustart umgeht Wartung nicht")
  @Description(
      """
      1. Eigenes Konto anlegen; Sitzungsgrenze 2 speichern und mit Remember-me anmelden.
      2. Chromium-Prozess tatsächlich beenden; Wartung einschalten und dasselbe Profil neu starten.
      3. Lokalisierte Wartungsablehnung am Login prüfen; kein zusätzlicher Sitzungsplatz wird zugelassen.
      4. Wartung ausschalten und manuell anmelden: zweiter Platz trotz der erhaltenen ersten Sitzung verfügbar.
      **Erwartet:** Remember-me umgeht die Rollenentscheidung nicht und Ablehnung verbraucht keinen Platz.
      """)
  void rememberMeRestartCannotBypassMaintenance() {
    String account = user("user");
    settings.open(AdminConfigGroup.SECURITY);
    settings.enter(texts.getAdminConfigEntryMaxSessionsPerUser(), "2");
    settings.save();
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
      login.signIn(account, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(persistent.page).selectTestLanguage();
      assertTrue(persistent.context.cookies().stream().anyMatch(c -> c.name.equals("remember-me")));
    }
    mode(true);
    try (PersistentBrowserActor resumed =
        new PersistentBrowserActor(playwright, profile, LOGIN_ROUTE)) {
      assertThat(resumed.page.locator("vaadin-notification-card:not([closing])"))
          .containsText(texts.getMaintenanceLoginMessage());
      mode(false);
      LoginPage login = new LoginPage(resumed.page);
      login.dismissFailure();
      login.signIn(account, BrowserTestAccounts.USER_PASSWORD);
      assertThat(resumed.page).hasURL(urlPattern(HOME_ROUTE));
    }
  }
}
