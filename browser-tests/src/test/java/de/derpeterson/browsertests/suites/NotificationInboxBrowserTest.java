package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.NotificationInboxPage;
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
@Feature("Fertiges Benachrichtigungsdesign: Vorschau, Inbox und Archiv")
public class NotificationInboxBrowserTest extends BrowserFixture {
  private String prepareRecipient() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    AdminUsersPage users = new AdminUsersPage(page);
    users.open();
    String email = "inbox-" + UUID.randomUUID() + "@example.test";
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }

  private void send(String recipient, String subject, String body) {
    NotificationComposePage compose = new NotificationComposePage(page);
    compose.open();
    compose.select(recipient);
    compose.draft(subject, body);
    compose.send().click();
    compose.notice(texts.getNotificationsSent());
    assertThat(compose.send()).isDisabled();
  }

  @ParameterizedTest(name = "NOTIFY-03: Fertige Vorschau/Inbox und sichere Löschaktionen bei {0}px")
  @ValueSource(ints = {1280, 390})
  @Description(
      """
      1. Drei eigene Nachrichten über das echte Versandformular senden, eine mit 120 Zeichen Betreff und 4000 Zeichen Inhalt.
      2. Empfänger bei Desktop-/Mobilbreite anmelden: gemeinsame Marke, Einstellungen-/Benachrichtigungstabs und Header-Vorschau prüfen.
      3. In Vorschau und Inbox vollständige Details öffnen: weiterhin ungelesen; Inbox-Link nur aus der Vorschau.
      4. Iconaktionen, gekürzte Vorschauen ohne horizontalen Überlauf, gleiche Metadaten-/Aktionszeile und lokalisierte Filter prüfen.
      5. Einen Eintrag ausdrücklich lesen; Sammellöschen entfernt nur diesen und erhält zwei ungelesene Einträge.
      6. Einzelnes X entfernt einen weiteren Eintrag ohne Detailöffnung; alle als gelesen markieren, Sammellöschen und persistente leere Inbox prüfen.
      Erwartet: Kein Sammellöschen in der Vorschau, kein Statuswechsel allein beim Öffnen, keine verlorenen ungelesenen Nachrichten.
      """)
  void previewInboxFiltersAndDismissalAtBothWidths(int width) {
    String recipient = prepareRecipient();
    String prefix = "Layout " + UUID.randomUUID();
    String subject = (prefix + " Long subject ".repeat(12)).substring(0, 120);
    String body = "Lange Nachricht ÄÖÜ ".repeat(250).substring(0, 4000);
    send(recipient, prefix + " first", "First body");
    send(recipient, prefix + " second", "Second body");
    send(recipient, subject, body);
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.login(recipient, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.page).selectTestLanguage();
      actor.page.setViewportSize(width, 1000);
      NotificationInboxPage inbox = new NotificationInboxPage(actor.page);
      inbox.open();
      assertThat(actor.page.locator(".cosmos-topbar-logo")).isVisible();
      assertTrue(
          (Boolean)
              actor
                  .page
                  .locator(".cosmos-topbar-logo")
                  .evaluate("el => el.complete && el.naturalWidth > 0"));
      assertThat(actor.page.locator("#account-navigation-notifications"))
          .hasAttribute("aria-selected", "true");
      actor.page.locator("#account-navigation-settings").click();
      assertThat(actor.page.locator("#account-navigation-settings"))
          .hasAttribute("aria-selected", "true");
      actor.page.locator("#account-navigation-notifications").click();
      assertThat(inbox.rows(inbox.inbox())).hasCount(3);
      inbox.openPreview();
      assertThat(inbox.rows(inbox.preview())).hasCount(3);
      inbox.assertRowLayout(inbox.preview(), true);
      inbox.openDetail(inbox.preview(), subject, body, true);
      inbox.closeDetail();
      inbox.filter(inbox.preview(), texts.getNotificationsRead());
      assertThat(inbox.rows(inbox.preview())).hasCount(0);
      inbox.filter(inbox.preview(), texts.getNotificationsUnread());
      assertThat(inbox.rows(inbox.preview())).hasCount(3);
      inbox.button(inbox.preview(), texts.getNotificationsHistory()).click();
      assertThat(inbox.inbox()).isVisible();
      assertThat(inbox.preview()).not().isVisible();
      inbox.assertRowLayout(inbox.inbox(), false);
      inbox.openDetail(inbox.inbox(), subject, body, false);
      inbox.closeDetail();
      assertThat(actor.page.locator("#notifications-inbox-unread-total")).hasText("3");
      inbox.row(inbox.inbox(), subject).locator(".cosmos-notification-inbox-mark-read").click();
      assertThat(actor.page.locator("#notifications-inbox-unread-total")).hasText("2");
      inbox.filter(inbox.inbox(), texts.getNotificationsRead());
      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.filter(inbox.inbox(), texts.getNotificationsAll());
      inbox.button(inbox.inbox(), texts.getNotificationsDismissRead()).click();
      assertThat(inbox.rows(inbox.inbox())).hasCount(2);
      assertThat(inbox.row(inbox.inbox(), subject)).hasCount(0);
      assertThat(actor.page.locator("#notifications-inbox-unread-total")).hasText("2");
      assertThat(inbox.button(inbox.inbox(), texts.getNotificationsDismissRead())).isDisabled();
      inbox
          .row(inbox.inbox(), prefix + " first")
          .locator(".cosmos-notification-preview-dismiss")
          .click();
      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      assertThat(inbox.dialog()).hasCount(0);
      inbox.button(inbox.inbox(), texts.getNotificationsMarkAll()).click();
      assertThat(actor.page.locator("#notifications-inbox-unread-total")).hasText("0");
      assertThat(inbox.button(inbox.inbox(), texts.getNotificationsMarkAll())).isDisabled();
      inbox.button(inbox.inbox(), texts.getNotificationsDismissRead()).click();
      assertThat(inbox.rows(inbox.inbox())).hasCount(0);
      assertThat(inbox.inbox().locator(".cosmos-notification-inbox-empty"))
          .containsText(texts.getNotificationsEmpty());
      actor.page.reload();
      assertThat(actor.page.locator("#notifications-inbox-total")).hasText("0");
      AllurePlaywright.attachScreenshot(
          "Leere Inbox nach ausdrücklichen Löschaktionen", actor.page);
    }
    // Der privilegierte Header muss auch von der Inbox aus zum Adminbereich führen.
    NotificationInboxPage own = new NotificationInboxPage(page);
    own.open();
    UserMenu menu = new UserMenu(page);
    menu.open();
    page.getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions()
                .setName(texts.getBaseManageApplicationButton())
                .setExact(true))
        .click();
    assertThat(page).hasURL(urlPattern(ADMIN_ROUTE));
  }

  @Test
  @DisplayName(
      "NOTIFY-04: Inbox-Pagination, Vorschaugrenze und Versandarchiv mit echten UI-Sendungen")
  @Description(
      """
      1. Elf Nachrichten ausschließlich über das Versandformular an ein eigenes frisches Konto senden.
      2. Versandarchiv: neueste fünf, Metadaten und vollständige aufklappbare Details; Seitengröße 56px und nächste Seite prüfen.
      3. Empfänger-Inbox: zehn und eine Nachricht auf zwei Seiten ohne Dopplung; Seitengröße 20 zeigt alle elf.
      4. Suche findet einen Betreff, erfolglose Suche zeigt den hervorgehobenen Leerzustand; Rücksetzen erhält alle Einträge.
      5. Header-Vorschau begrenzt auf fünf Einträge; Sammellesen markiert alle elf und bleibt nach Reload erhalten.
      Erwartet: Echte persistente Pagination und Suche, Vorschaugrenze fünf, Sammellesen auch jenseits der sichtbaren Seite.
      """)
  void paginationPreviewLimitAndSentArchive() {
    String recipient = prepareRecipient();
    String prefix = "Pages " + UUID.randomUUID();
    for (int index = 0; index < 11; index++) {
      send(recipient, prefix + " item " + index, "Archive body " + index);
    }
    Locator archive = page.locator("#admin-notifications-sent");
    assertThat(archive.locator(".cosmos-notification-sent-row")).hasCount(5);
    Locator latest = archive.locator(".cosmos-notification-sent-row").first();
    assertThat(latest).containsText(prefix + " item 10");
    latest.locator(".cosmos-notification-sent-toggle").click();
    assertThat(latest.locator(".cosmos-notification-sent-details")).hasAttribute("opened", "");
    assertThat(latest.locator(".cosmos-notification-sent-body")).hasText("Archive body 10");
    assertEquals(56, archive.locator("vaadin-combo-box").boundingBox().width, 1);
    archive
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions()
                .setName(texts.getAdminUsersPaginationNextPage())
                .setExact(true))
        .click();
    assertThat(archive.locator(".cosmos-notification-sent-row").first())
        .containsText(prefix + " item 5");
    try (BrowserActor actor = new BrowserActor(browser)) {
      actor.login(recipient, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(actor.page).selectTestLanguage();
      NotificationInboxPage inbox = new NotificationInboxPage(actor.page);
      inbox.open();
      assertThat(inbox.rows(inbox.inbox())).hasCount(10);
      assertThat(actor.page.locator("#notifications-inbox-total")).hasText("11");
      assertEquals(56, actor.page.locator("#notifications-inbox-page-size").boundingBox().width, 1);
      inbox.button(inbox.inbox(), texts.getNotificationsNext()).click();
      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      assertThat(inbox.row(inbox.inbox(), prefix + " item 0")).hasCount(1);
      assertThat(inbox.button(inbox.inbox(), texts.getNotificationsNext())).isDisabled();
      for (String size : new String[] {"20", "50"}) {
        inbox.pageSize(inbox.inbox(), size);
        assertThat(inbox.rows(inbox.inbox())).hasCount(11);
      }
      inbox.search(prefix + " item 10");
      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.filter(inbox.inbox(), texts.getNotificationsRead());
      assertThat(inbox.rows(inbox.inbox())).hasCount(0);
      inbox.filter(inbox.inbox(), texts.getNotificationsUnread());
      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.filter(inbox.inbox(), texts.getNotificationsAll());
      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.search("no-result-" + UUID.randomUUID());
      assertThat(inbox.rows(inbox.inbox())).hasCount(0);
      assertThat(inbox.inbox().locator(".cosmos-notification-inbox-empty"))
          .containsText(texts.getNotificationsSearchEmpty());
      inbox.search("");
      assertThat(inbox.rows(inbox.inbox())).hasCount(11);
      inbox.openPreview();
      assertThat(inbox.rows(inbox.preview())).hasCount(5);
      inbox.button(inbox.preview(), texts.getNotificationsMarkAll()).click();
      assertThat(inbox.button(inbox.preview(), texts.getNotificationsMarkAll())).isDisabled();
      inbox.button(inbox.preview(), texts.getNotificationsHistory()).click();
      assertThat(actor.page.locator("#notifications-inbox-unread-total")).hasText("0");
      actor.page.reload();
      assertThat(actor.page.locator("#notifications-inbox-unread-total")).hasText("0");
      assertThat(actor.page.locator("#notifications-inbox-total")).hasText("11");
      AllurePlaywright.attachScreenshot("Persistente Inbox und Pagination", actor.page);
    }
  }
}
