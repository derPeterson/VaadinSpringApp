package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.*;
import de.derpeterson.browsertests.pages.*;
import io.qameta.allure.*;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Benachrichtigungen: Verfassen, eigene Inbox und Wartung")
public class UserNotificationsBrowserTest extends BrowserFixture {
  private AdminUsersPage admin() {
    var login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    return new AdminUsersPage(page);
  }

  @ParameterizedTest(name = "NOTIFY-01: Verfassen, Validierung und Vorschau bei {0}px")
  @ValueSource(ints = {1280, 390})
  @Description(
      """
      1. Als Superadmin das Verfassenpanel bei Desktop- bzw. schmaler Breite öffnen.
      2. Leere Eingaben verlassen: keine Fehler; Prüfen markiert alle fehlenden Felder und zeigt den allgemeinen Hinweis.
      3. Alle aktiven Konten wählen: Empfängerfeld gesperrt und lokalisierte Anzahl sichtbar.
      4. Vorschau prüfen, Empfängerdetails öffnen/schließen, Entwurf ändern: Versand wieder gesperrt.
      5. Erneut prüfen: aktualisierter Text und Freigabe; Rahmen, Textgröße, Textabstand, gleiche Buttonbreite und kein Überlauf.
      Erwartet: Native Bedienelemente und sichere Prüf-Freigabe; keine Historien-/Header-Overlay-Prüfung.
      """)
  void composeValidationAndReviewAtBothWidths(int width) {
    admin();
    page.setViewportSize(width, 1000);
    var compose = new NotificationComposePage(page);
    compose.open();
    compose.assertEmptyReview();
    compose.select(BrowserTestAccounts.ADMIN_EMAIL);
    compose.everyone().setChecked(true);
    Locator all = page.locator("#admin-notifications-everyone-recipients");
    assertThat(all).hasAttribute("disabled", "");
    assertThat(all.locator("input")).isDisabled();
    String summary = (String) all.evaluate("el => el.value");
    int left = summary.lastIndexOf('('), right = summary.lastIndexOf(')');
    assertTrue(left >= 0 && right > left);
    int count = Integer.parseInt(summary.substring(left + 1, right));
    assertTrue(count >= 2);
    assertEquals(texts.getNotificationsEveryonePlaceholder(count), summary);
    compose.draft("Preview " + UUID.randomUUID(), "Original draft");
    page.locator("#admin-notifications-preview-details").click();
    assertThat(page.locator("#admin-notifications-preview-recipients")).hasAttribute("opened", "");
    assertThat(page.locator("#admin-notifications-preview-recipients"))
        .containsText(BrowserTestAccounts.SUPERADMIN_EMAIL);
    page.locator("#admin-notifications-preview-details").click();
    assertThat(page.locator("#admin-notifications-preview-details"))
        .hasAttribute("aria-expanded", "false");
    compose.body().fill("Changed draft");
    assertThat(compose.send()).isDisabled();
    assertThat(compose.status()).hasText(texts.getNotificationsPreviewNeedsReview());
    compose.review().click();
    assertThat(compose.send()).isEnabled();
    Locator content = compose.root().locator(".cosmos-notification-compose-preview-content");
    assertThat(content).containsText("Changed draft");
    assertThat(content).hasCSS("gap", "8px");
    assertThat(content.locator(".cosmos-notification-body")).hasCSS("font-size", "12px");
    assertEquals(compose.review().boundingBox().width, compose.send().boundingBox().width, 1.0);
    assertTrue(compose.root().boundingBox().x >= 0);
    assertTrue(compose.root().boundingBox().x + compose.root().boundingBox().width <= width + 1);
    compose.review().focus();
    compose.review().press("Tab");
    assertThat(compose.send()).isFocused();
    assertThat(compose.send()).hasCSS("outline-style", "solid");
    AllurePlaywright.attachScreenshot("Geprüftes Verfassenpanel", page);
  }

  @ParameterizedTest(
      name = "NOTIFY-02: Persistenter Versand an alle={0}, private Inbox und Lesestatus")
  @ValueSource(booleans = {false, true})
  @Description(
      """
      1. Zwei eigene aktive Benutzer ausschließlich über die Benutzerverwaltung anlegen.
      2. Als Superadmin wahlweise einen Empfänger oder alle aktiven Konten wählen, prüfen und senden.
      3. In getrennter Benutzersitzung die direkte persönliche Inbox öffnen, Detail zunächst ungelesen sehen, ausdrücklich als gelesen markieren und nach Reload als gelesen wiederfinden.
      4. Der zweite Benutzer erhält bei Einzelversand keinen Eintrag, bei Sammelversand seinen eigenen Eintrag.
      Erwartet: Persistenter Versand, Empfängertrennung und Lesestatus ohne Benachrichtigungs-Header-Overlay oder gesendete Historie.
      """)
  void targetedAndEveryoneDeliveryRemainPrivateAndPersistent(boolean everyone) {
    var users = admin();
    users.open();
    String first = "notify-first-" + UUID.randomUUID() + "@example.test";
    String second = "notify-second-" + UUID.randomUUID() + "@example.test";
    users.createActiveUser(first, BrowserTestAccounts.USER_PASSWORD);
    users.open();
    users.createActiveUser(second, BrowserTestAccounts.USER_PASSWORD);
    String subject = "Notification " + UUID.randomUUID(),
        message = "Own persistent notification ÄÖÜ";
    var compose = new NotificationComposePage(page);
    compose.open();
    if (everyone) compose.everyone().setChecked(true);
    else compose.select(first);
    compose.draft(subject, message);
    compose.send().click();
    compose.notice(texts.getNotificationsSent());
    assertThat(compose.send()).isDisabled();
    try (var actor = new BrowserActor(browser);
        var other = new BrowserActor(browser)) {
      actor.login(first, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.page).selectTestLanguage();
      NotificationComposePage.readOwn(actor.page, subject, message);
      other.login(second, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(other.page).selectTestLanguage();
      if (everyone) NotificationComposePage.readOwn(other.page, subject, message);
      else {
        other.page.navigate(path(NOTIFICATIONS_ROUTE));
        assertThat(
                other
                    .page
                    .locator("#notifications-inbox .cosmos-notification-title")
                    .filter(new Locator.FilterOptions().setHasText(subject)))
            .hasCount(0);
      }
    }
  }

  @Test
  @DisplayName(
      "MAINT-06: Beide Versandpanels folgen Wartungswechsel im anderen Fenster und erhalten Entwürfe")
  @Description(
      """
      1. Als Superadmin Verfassen, Mail-Test und Konfiguration in eigenen Tabs derselben isolierten Sitzung öffnen.
      2. Entwürfe eingeben und Benachrichtigung prüfen; SMTP/INTERN grün, Versand freigegeben.
      3. Wartung in Konfiguration einschalten: beide geöffneten Panels rot und vollständig deaktiviert.
      4. Wartung ausschalten: ursprüngliche Eingaben erhalten, Mail-Test wieder bedienbar, Benachrichtigung erst nach neuer Prüfung freigegeben.
      Erwartet: Tatsächliche native Poll-Aktualisierung ohne Reload/erneuten Login; keine echte Mail versandt.
      """)
  void maintenanceUpdatesBothOpenPanelsAndPreservesDrafts() {
    admin();
    var compose = new NotificationComposePage(page);
    compose.open();
    compose.everyone().setChecked(true);
    compose.draft("Maintenance draft", "Preserved draft");
    Page mailPage = context.newPage(), settingsPage = context.newPage();
    var mail = new AdminMailPage(mailPage);
    var settings = new ConfigurationPage(settingsPage);
    try {
      mailPage.navigate(path(ADMIN_ROUTE) + "?section=mail_test");
      settingsPage.navigate(path(ADMIN_ROUTE));
      settings.open(AdminConfigGroup.SECURITY);
      mail.input(texts.getAdminMailSubject()).fill("Mail draft");
      mail.input(texts.getAdminMailBody()).fill("Preserved mail draft");
      Locator internal = page.locator(".cosmos-notification-delivery"),
          smtp = mailPage.locator(".cosmos-mail-delivery");
      assertThat(internal).hasAttribute("data-state", "open");
      assertThat(smtp).hasAttribute("data-state", "open");
      String green =
          internal
              .locator(".cosmos-notification-delivery-dot")
              .evaluate("el => getComputedStyle(el).backgroundColor")
              .toString();
      settings.input(texts.getAdminConfigEntryMaintenanceMode()).setChecked(true);
      settings.save();
      assertThat(internal).hasAttribute("data-state", "active");
      assertThat(smtp).hasAttribute("data-state", "active");
      assertNotEquals(
          green,
          internal
              .locator(".cosmos-notification-delivery-dot")
              .evaluate("el => getComputedStyle(el).backgroundColor"));
      assertThat(compose.title()).isDisabled();
      assertThat(compose.body()).isDisabled();
      assertThat(compose.everyone()).isDisabled();
      assertThat(compose.review()).isDisabled();
      assertThat(compose.send()).isDisabled();
      assertThat(mail.input(texts.getAdminMailRecipient())).isDisabled();
      assertThat(mail.input(texts.getAdminMailSubject())).isDisabled();
      assertThat(mail.input(texts.getAdminMailBody())).isDisabled();
      assertThat(mailPage.locator(".cosmos-mail-card vaadin-button.cosmos-config-info").first())
          .isDisabled();
      settings.input(texts.getAdminConfigEntryMaintenanceMode()).setChecked(false);
      settings.save();
      assertThat(internal).hasAttribute("data-state", "open");
      assertThat(smtp).hasAttribute("data-state", "open");
      assertThat(compose.title()).hasValue("Maintenance draft");
      assertThat(compose.body()).hasValue("Preserved draft");
      assertThat(compose.review()).isEnabled();
      assertThat(compose.send()).isDisabled();
      assertThat(mail.input(texts.getAdminMailSubject())).hasValue("Mail draft");
      assertThat(mail.input(texts.getAdminMailBody())).hasValue("Preserved mail draft");
      compose.review().click();
      assertThat(compose.send()).isEnabled();
    } finally {
      settings.open(AdminConfigGroup.SECURITY);
      settings.input(texts.getAdminConfigEntryMaintenanceMode()).setChecked(false);
      settings.save();
      mailPage.close();
      settingsPage.close();
    }
  }
}
