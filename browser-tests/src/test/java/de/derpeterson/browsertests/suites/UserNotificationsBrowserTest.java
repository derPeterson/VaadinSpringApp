package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.NOTIFICATIONS_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.CosmosFollowUpChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Benachrichtigungen: Verfassen, eigene Inbox und Wartung")
public class UserNotificationsBrowserTest extends BrowserFixture {
  private AdminUsersPage admin() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    return new AdminUsersPage(browserPage);
  }

  @ParameterizedTest(name = "NOTIFY-01: Verfassen, Validierung und Vorschau bei {0}px")
  @ValueSource(ints = {1280, 390})
  @Description(
      """
      1. Als Superadmin das Verfassenpanel bei Desktop- bzw. schmaler Breite öffnen.
      2. Leere Eingaben verlassen: keine Fehler; Prüfen markiert alle fehlenden Felder und meldet zuerst die fehlenden Empfänger.
      3. Alle aktiven Konten wählen: Empfängerfeld gesperrt und lokalisierte Anzahl sichtbar; zurück zur erhaltenen manuellen Auswahl.
      4. Vorschau prüfen, Empfänger-Popover öffnen und schließen, Entwurf ändern: Versand wieder gesperrt.
      5. Erneut prüfen: aktualisierter Text und Freigabe; Rahmen, Textgröße, Textabstand, gleiche Buttonhöhe, bündige Panelköpfe und kein Überlauf.
      Erwartet: Native Bedienelemente und sichere Prüf-Freigabe; keine Historien-/Header-Overlay-Prüfung.
      """)
  @Tag("component-follow-up")
  void composeValidationAndReviewAtBothWidths(int width) {
    admin();
    browserPage.setViewportSize(width, 1000);
    NotificationComposePage compose = new NotificationComposePage(browserPage);
    compose.open();
    UiActions.resizeTextArea(browserPage, browserPage.locator("#admin-notifications-body"), 122);
    verifyPreviewReportsFirstInvalidField(compose);
    verifyEveryoneModeKeepsManualSelection(compose);
    compose.draft("Preview " + UUID.randomUUID(), "Original draft");
    verifyPreviewRecipientList(compose);
    compose.body().fill("Changed draft");

    assertThat(compose.send()).isDisabled();
    assertThat(compose.status()).hasText(uiTexts.getNotificationsPreviewNeedsReview());
    compose.review().click();

    assertThat(compose.send()).isEnabled();
    assertReviewedPreviewLayout(compose, width);
    compose.review().focus();
    compose.review().press("Tab");
    Locator recipientAvatars = browserPage.locator("#admin-notifications-recipient-avatars");
    assertThat(recipientAvatars).isFocused();
    recipientAvatars.press("Tab");

    assertThat(compose.send()).isFocused();
    assertThat(compose.send()).hasCSS("outline-style", "solid");
    AllurePlaywright.attachScreenshot("Geprüftes Verfassenpanel", browserPage);
  }

  private void verifyPreviewReportsFirstInvalidField(NotificationComposePage compose) {
    CosmosFollowUpChecks.assertNativeRequiredIndicator(compose.recipients());
    CosmosFollowUpChecks.assertNativeRequiredIndicator(
        compose.root().locator("#admin-notifications-title"));
    CosmosFollowUpChecks.assertNativeRequiredIndicator(
        compose.root().locator("#admin-notifications-body"));
    compose.assertEmptyReview();
    compose.select(BrowserTestAccounts.ADMIN_EMAIL);
    assertThat(compose.recipients()).not().hasAttribute("invalid", "");
    assertThat(compose.root().locator("#admin-notifications-title[invalid]")).hasCount(1);
    assertThat(compose.root().locator("#admin-notifications-body[invalid]")).hasCount(1);

    compose.review().click();
    compose.notice(uiTexts.getBaseFailedTitle(), uiTexts.getNotificationsInvalidSubject());
    compose.title().fill("Subject");
    assertThat(compose.root().locator("#admin-notifications-title[invalid]")).hasCount(0);
    assertThat(compose.root().locator("#admin-notifications-body[invalid]")).hasCount(1);
    compose.review().click();
    compose.notice(uiTexts.getBaseFailedTitle(), uiTexts.getNotificationsInvalidBody());
    assertThat(compose.send()).isDisabled();
  }

  private void verifyEveryoneModeKeepsManualSelection(NotificationComposePage compose) {
    assertThat(compose.root().locator(".cosmos-notification-compose-everyone-hint"))
        .hasText(uiTexts.getNotificationsEveryoneHint());
    assertThat(
            compose
                .root()
                .getByRole(
                    AriaRole.SWITCH,
                    new Locator.GetByRoleOptions().setName(uiTexts.getNotificationsEveryone())))
        .hasCount(1);
    compose.everyone().setChecked(true);
    Locator everyoneRecipientsField =
        browserPage.locator("#admin-notifications-everyone-recipients");

    assertThat(compose.recipients()).isHidden();
    assertThat(everyoneRecipientsField).hasAttribute("disabled", "");
    assertThat(everyoneRecipientsField.locator("input")).isDisabled();
    String recipientSummary = (String) everyoneRecipientsField.evaluate("element => element.value");
    int countStart = recipientSummary.lastIndexOf('(');
    int countEnd = recipientSummary.lastIndexOf(')');
    assertTrue(countStart >= 0 && countEnd > countStart);
    int recipientCount = Integer.parseInt(recipientSummary.substring(countStart + 1, countEnd));
    assertTrue(recipientCount >= 2);
    assertEquals(uiTexts.getNotificationsEveryonePlaceholder(recipientCount), recipientSummary);

    compose.everyone().setChecked(false);
    assertThat(compose.recipients()).isVisible();
    assertThat(compose.root().locator(".cosmos-notification-compose-recipient-summary"))
        .hasText(uiTexts.getNotificationsSelectedUsers(1));
    compose.everyone().setChecked(true);
  }

  private void verifyPreviewRecipientList(NotificationComposePage compose) {
    Locator recipientAvatars = browserPage.locator("#admin-notifications-recipient-avatars");
    Locator recipientPopover = browserPage.locator("#admin-notifications-preview-recipients");
    compose.review().focus();
    compose.review().press("Tab");
    assertThat(recipientAvatars).isFocused();
    recipientAvatars.press("Space");
    assertThat(recipientPopover).hasAttribute("opened", "");
    recipientPopover
        .locator("#admin-notifications-recipient-search input")
        .fill(BrowserTestAccounts.SUPERADMIN_EMAIL);
    assertThat(recipientPopover.locator(".cosmos-recipient-button:visible")).hasCount(1);
    assertThat(recipientPopover.locator(".cosmos-recipient-button:visible"))
        .containsText(BrowserTestAccounts.SUPERADMIN_EMAIL);
    recipientPopover
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(uiTexts.getNotificationsClose()).setExact(true))
        .click();
    assertThat(recipientPopover).not().hasAttribute("opened", "");
    assertThat(recipientAvatars).isFocused();
    assertThat(compose.send()).isEnabled();
  }

  private void assertReviewedPreviewLayout(NotificationComposePage compose, int width) {
    Locator content =
        browserPage.locator(
            ".admin-notification-preview-card .cosmos-notification-compose-preview-content");

    assertThat(content).containsText("Changed draft");
    assertThat(content).hasCSS("gap", "8px");
    assertThat(content.locator(".cosmos-notification-body")).hasCSS("font-size", "12px");
    assertEquals(compose.review().boundingBox().height, compose.send().boundingBox().height, 1.0);
    if (width == 1280) {
      assertEquals(
          compose.root().boundingBox().y,
          browserPage.locator(".admin-notification-preview-card").boundingBox().y,
          1.0);
    }
    assertTrue(compose.root().boundingBox().x >= 0);
    assertTrue(compose.root().boundingBox().x + compose.root().boundingBox().width <= width + 1);
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
    AdminUsersPage users = admin();
    users.open();
    String first = "notify-first-" + UUID.randomUUID() + "@example.test";
    String second = "notify-second-" + UUID.randomUUID() + "@example.test";
    users.createActiveUser(first, BrowserTestAccounts.USER_PASSWORD);
    users.open();
    users.createActiveUser(second, BrowserTestAccounts.USER_PASSWORD);
    String subject = "Notification " + UUID.randomUUID(),
        message = "Own persistent notification ÄÖÜ";
    NotificationComposePage compose = new NotificationComposePage(browserPage);
    compose.open();
    if (everyone) {
      compose.everyone().setChecked(true);
    } else {
      compose.select(first);
    }
    compose.draft(subject, message);
    compose.send().click();
    compose.notice(uiTexts.getNotificationsSent());

    assertThat(compose.send()).isDisabled();
    try (BrowserActor browserActor = new BrowserActor(testBrowser);
        BrowserActor other = new BrowserActor(testBrowser)) {
      browserActor.login(first, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(browserActor.browserPage).selectTestLanguage();
      NotificationComposePage.readOwn(browserActor.browserPage, subject, message);
      other.login(second, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(other.browserPage).selectTestLanguage();
      if (everyone) {
        NotificationComposePage.readOwn(other.browserPage, subject, message);
      } else {
        other.browserPage.navigate(path(NOTIFICATIONS_ROUTE));

        assertThat(
                other
                    .browserPage
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
    NotificationComposePage compose = new NotificationComposePage(browserPage);
    compose.open();
    compose.everyone().setChecked(true);
    compose.draft("Maintenance draft", "Preserved draft");
    Page mailPage = browserContext.newPage(), settingsPage = browserContext.newPage();
    AdminMailPage mail = new AdminMailPage(mailPage);
    AdminConfigurationPage settings = new AdminConfigurationPage(settingsPage);
    try {
      mailPage.navigate(path(ADMIN_ROUTE) + "?section=mail_test");
      settingsPage.navigate(path(ADMIN_ROUTE));
      settings.open(AdminConfigGroup.SECURITY);
      mail.input(uiTexts.getAdminMailSubject()).fill("Mail draft");
      mail.input(uiTexts.getAdminMailBody()).fill("Preserved mail draft");
      Locator internal = browserPage.locator("vaadin-badge.cosmos-notification-delivery"),
          smtp = mailPage.locator("vaadin-badge.cosmos-mail-delivery");

      assertThat(internal).hasAttribute("data-state", "open");
      assertThat(smtp).hasAttribute("data-state", "open");
      String availableColor = internal.evaluate("el => getComputedStyle(el).color").toString();
      assertEquals(availableColor, smtp.evaluate("el => getComputedStyle(el).color"));
      settings.input(uiTexts.getAdminConfigEntryMaintenanceMode()).setChecked(true);
      settings.save();

      assertThat(internal).hasAttribute("data-state", "active");
      assertThat(smtp).hasAttribute("data-state", "active");
      String unavailableColor = internal.evaluate("el => getComputedStyle(el).color").toString();
      assertNotEquals(availableColor, unavailableColor);
      assertEquals(unavailableColor, smtp.evaluate("el => getComputedStyle(el).color"));
      assertMailAndNotificationEditorsDisabled(compose, mail, mailPage);
      settings.input(uiTexts.getAdminConfigEntryMaintenanceMode()).setChecked(false);
      settings.save();

      assertThat(internal).hasAttribute("data-state", "open");
      assertEquals(availableColor, internal.evaluate("el => getComputedStyle(el).color"));
      assertThat(smtp).hasAttribute("data-state", "open");
      assertThat(compose.title()).hasValue("Maintenance draft");
      assertThat(compose.body()).hasValue("Preserved draft");
      assertThat(compose.review()).isEnabled();
      assertThat(compose.send()).isDisabled();
      assertThat(mail.input(uiTexts.getAdminMailSubject())).hasValue("Mail draft");
      assertThat(mail.input(uiTexts.getAdminMailBody())).hasValue("Preserved mail draft");
      compose.review().click();

      assertThat(compose.send()).isEnabled();
    } finally {
      settings.open(AdminConfigGroup.SECURITY);
      settings.input(uiTexts.getAdminConfigEntryMaintenanceMode()).setChecked(false);
      settings.save();
      mailPage.close();
      settingsPage.close();
    }
  }

  private void assertMailAndNotificationEditorsDisabled(
      NotificationComposePage composePage, AdminMailPage mailTestPage, Page mailBrowserPage) {
    assertThat(composePage.title()).isDisabled();
    assertThat(composePage.body()).isDisabled();
    assertThat(composePage.everyone()).isDisabled();
    assertThat(composePage.review()).isDisabled();
    assertThat(composePage.send()).isDisabled();

    assertThat(mailTestPage.input(uiTexts.getAdminMailRecipient())).isDisabled();
    assertThat(mailTestPage.input(uiTexts.getAdminMailSubject())).isDisabled();
    assertThat(mailTestPage.input(uiTexts.getAdminMailBody())).isDisabled();
    assertThat(
            mailBrowserPage.locator(".cosmos-mail-card vaadin-button.cosmos-config-info").first())
        .isDisabled();
  }
}
