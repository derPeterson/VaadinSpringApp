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
import de.derpeterson.browsertests.pages.CosmosFollowUpChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.NotificationInboxPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Fertiges Benachrichtigungsdesign: Vorschau, Inbox und Archiv")
public class NotificationInboxBrowserTest extends BrowserFixture {
  private String prepareRecipient() {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminUsersPage users = new AdminUsersPage(browserPage);
    users.open();
    String email = "inbox-" + UUID.randomUUID() + "@example.test";
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    return email;
  }

  private void send(String recipient, String subject, String body) {
    NotificationComposePage compose = new NotificationComposePage(browserPage);
    compose.open();
    compose.select(recipient);
    compose.draft(subject, body);
    compose.send().click();
    compose.notice(uiTexts.getNotificationsSent());

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
  @Tag("component-follow-up")
  @Tag("metric-cards")
  @Tag("native-confirmations")
  void previewInboxFiltersAndDismissalAtBothWidths(int width) {
    String recipient = prepareRecipient();
    String prefix = "Layout " + UUID.randomUUID();
    String subject = (prefix + " Long subject ".repeat(12)).substring(0, 120);
    String body = "Lange Nachricht ÄÖÜ ".repeat(250).substring(0, 4000);
    send(recipient, prefix + " first", "First body");
    send(recipient, prefix + " second", "Second body");
    send(recipient, subject, body);
    try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
      browserActor.login(recipient, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(browserActor.browserPage).selectTestLanguage();
      browserActor.browserPage.setViewportSize(width, 1000);
      NotificationInboxPage inbox = new NotificationInboxPage(browserActor.browserPage);
      inbox.open();

      attachMetricCardEvidence(browserActor.browserPage, width);

      assertThat(browserActor.browserPage.locator(".cosmos-topbar-logo")).isVisible();
      assertTrue(
          (Boolean)
              browserActor
                  .browserPage
                  .locator(".cosmos-topbar-logo")
                  .evaluate("el => el.complete && el.naturalWidth > 0"));
      assertThat(browserActor.browserPage.locator("#account-nav-notifications"))
          .hasClass(java.util.regex.Pattern.compile(".*cosmos-nav-item--active.*"));
      new de.derpeterson.browsertests.pages.AccountPage(browserActor.browserPage)
          .section("account-nav-profile");

      assertThat(browserActor.browserPage.locator("#account-tab-profile"))
          .hasAttribute("aria-current", "page");
      new de.derpeterson.browsertests.pages.AccountPage(browserActor.browserPage)
          .section("account-nav-notifications");

      assertThat(inbox.rows(inbox.inbox())).hasCount(3);
      inbox.openPreview();

      assertThat(inbox.rows(inbox.preview())).hasCount(3);
      inbox.assertRowLayout(inbox.preview(), true);
      inbox.openDetail(inbox.preview(), subject, body, true);
      inbox.closeDetail();
      inbox.filter(inbox.preview(), uiTexts.getNotificationsRead());

      assertThat(inbox.rows(inbox.preview())).hasCount(0);
      inbox.filter(inbox.preview(), uiTexts.getNotificationsUnread());

      assertThat(inbox.rows(inbox.preview())).hasCount(3);
      inbox.button(inbox.preview(), uiTexts.getNotificationsHistory()).click();

      assertThat(inbox.inbox()).isVisible();
      assertThat(inbox.preview()).not().isVisible();
      inbox.assertRowLayout(inbox.inbox(), false);
      inbox.openDetail(inbox.inbox(), subject, body, false);
      inbox.closeDetail();

      assertThat(browserActor.browserPage.locator("#notifications-inbox-unread-total"))
          .hasText("3");
      inbox.row(inbox.inbox(), subject).locator(".cosmos-notification-inbox-mark-read").click();
      inbox.confirm();

      assertThat(browserActor.browserPage.locator("#notifications-inbox-unread-total"))
          .hasText("2");
      inbox.filter(inbox.inbox(), uiTexts.getNotificationsRead());

      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.filter(inbox.inbox(), uiTexts.getNotificationsAll());
      inbox.button(inbox.inbox(), uiTexts.getNotificationsDismissRead()).click();
      inbox.confirm();

      assertThat(inbox.rows(inbox.inbox())).hasCount(2);
      assertThat(inbox.row(inbox.inbox(), subject)).hasCount(0);
      assertThat(browserActor.browserPage.locator("#notifications-inbox-unread-total"))
          .hasText("2");
      assertThat(inbox.button(inbox.inbox(), uiTexts.getNotificationsDismissRead())).isDisabled();
      inbox.openDetail(inbox.inbox(), prefix + " first", "First body", false);
      inbox.button(inbox.dialog(), uiTexts.getNotificationsMoreActions()).click();
      browserActor
          .browserPage
          .locator(".cosmos-notification-detail-tools:visible .cosmos-notification-detail-delete")
          .click();
      inbox.confirm();

      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      assertThat(inbox.dialog()).hasCount(0);
      inbox.button(inbox.inbox(), uiTexts.getNotificationsMarkAll()).click();
      inbox.confirm();

      assertThat(browserActor.browserPage.locator("#notifications-inbox-unread-total"))
          .hasText("0");
      assertThat(inbox.button(inbox.inbox(), uiTexts.getNotificationsMarkAll())).isDisabled();
      inbox.button(inbox.inbox(), uiTexts.getNotificationsDismissRead()).click();
      inbox.confirm();

      assertThat(inbox.rows(inbox.inbox())).hasCount(0);
      assertThat(inbox.inbox().locator(".cosmos-notification-empty"))
          .containsText(uiTexts.getNotificationsEmpty());
      browserActor.browserPage.reload();

      assertThat(browserActor.browserPage.locator("#notifications-inbox-total")).hasText("0");
      AllurePlaywright.attachScreenshot(
          "Leere Inbox nach ausdrücklichen Löschaktionen", browserActor.browserPage);
    }
    // Der privilegierte Header muss auch von der Inbox aus zum Adminbereich führen.
    NotificationInboxPage own = new NotificationInboxPage(browserPage);
    own.open();
    UserMenu userMenu = new UserMenu(browserPage);
    userMenu.open();
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getBaseManageApplicationButton())
                .setExact(true))
        .click();

    assertThat(browserPage).hasURL(urlPattern(ADMIN_ROUTE));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "NOTIFY-04: Inbox-Pagination, Vorschaugrenze und Versandarchiv mit echten UI-Sendungen")
  @Description(
      """
      1. Elf Nachrichten ausschließlich über das Versandformular an ein eigenes frisches Konto senden.
      2. Versandarchiv: neueste fünf, Metadaten und vollständige aufklappbare Details; 64 px breite Seitengrößenauswahl und nächste Seite prüfen.
      3. Empfänger-Inbox: zehn und eine Nachricht auf zwei Seiten ohne Dopplung; Seitengröße 20 zeigt alle elf.
      4. Suche findet einen Betreff, erfolglose Suche zeigt den hervorgehobenen Leerzustand; Rücksetzen erhält alle Einträge.
      5. Header-Vorschau begrenzt auf fünf Einträge; Sammellesen markiert alle elf und bleibt nach Reload erhalten.
      Erwartet: Echte persistente Pagination und Suche, Vorschaugrenze fünf, Sammellesen auch jenseits der sichtbaren Seite.
      """)
  @Tag("component-follow-up")
  @Tag("inbox-pagination")
  void paginationPreviewLimitAndSentArchive(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    String recipient = prepareRecipient();
    capturePagination(
        browserPage, browserPage.locator(".admin-user-pagination:visible"), "Users", viewportWidth);
    String prefix = "Pages " + UUID.randomUUID();
    for (int index = 0; index < 11; index++) {
      send(recipient, prefix + " item " + index, "Archive body " + index);
    }
    Locator archive = browserPage.locator("#admin-notifications-sent");

    assertThat(archive.locator(".cosmos-notification-sent-row")).hasCount(5);
    Locator latest = archive.locator(".cosmos-notification-sent-row").first();

    assertThat(latest).containsText(prefix + " item 10");
    CosmosFollowUpChecks.assertDetailsWithoutHover(
        latest.locator(".cosmos-notification-sent-toggle"));
    latest.locator(".cosmos-notification-sent-toggle").click();

    assertThat(latest.locator(".cosmos-notification-sent-details")).hasAttribute("opened", "");
    assertThat(latest.locator(".cosmos-notification-sent-body")).hasText("Archive body 10");
    assertEquals(64, archive.locator("vaadin-combo-box").boundingBox().width, 1);
    capturePagination(
        browserPage, archive.locator(".admin-user-pagination"), "Sent", viewportWidth);
    archive
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getAdminUsersPaginationNextPage())
                .setExact(true))
        .click();

    assertThat(archive.locator(".cosmos-notification-sent-row").first())
        .containsText(prefix + " item 5");
    try (BrowserActor browserActor = new BrowserActor(testBrowser)) {
      browserActor.login(recipient, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(browserActor.browserPage).selectTestLanguage();
      browserActor.browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
      NotificationInboxPage inbox = new NotificationInboxPage(browserActor.browserPage);
      inbox.open();

      assertThat(inbox.rows(inbox.inbox())).hasCount(10);
      assertThat(browserActor.browserPage.locator("#notifications-inbox-total")).hasText("11");
      assertEquals(
          64,
          browserActor.browserPage.locator("#notifications-inbox-page-size").boundingBox().width,
          1);
      capturePagination(
          browserActor.browserPage,
          inbox.inbox().locator(".cosmos-notification-inbox-pagination-row"),
          "Inbox",
          viewportWidth);
      inbox.button(inbox.inbox(), uiTexts.getNotificationsNext()).click();

      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      assertThat(inbox.row(inbox.inbox(), prefix + " item 0")).hasCount(1);
      assertThat(inbox.button(inbox.inbox(), uiTexts.getNotificationsNext())).isDisabled();
      for (String size : new String[] {"20", "50"}) {
        inbox.pageSize(inbox.inbox(), size);

        assertThat(inbox.rows(inbox.inbox())).hasCount(11);
      }
      inbox.search(prefix + " item 10");

      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.filter(inbox.inbox(), uiTexts.getNotificationsRead());

      assertThat(inbox.rows(inbox.inbox())).hasCount(0);
      inbox.filter(inbox.inbox(), uiTexts.getNotificationsUnread());

      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.filter(inbox.inbox(), uiTexts.getNotificationsAll());

      assertThat(inbox.rows(inbox.inbox())).hasCount(1);
      inbox.search("no-result-" + UUID.randomUUID());

      assertThat(inbox.rows(inbox.inbox())).hasCount(0);
      assertThat(inbox.inbox().locator(".cosmos-notification-empty"))
          .containsText(uiTexts.getNotificationsSearchEmpty());
      inbox.search("");

      assertThat(inbox.rows(inbox.inbox())).hasCount(11);
      inbox.openPreview();

      assertThat(inbox.rows(inbox.preview())).hasCount(5);
      inbox.button(inbox.preview(), uiTexts.getNotificationsMarkAll()).click();
      inbox.confirm();

      assertThat(inbox.button(inbox.preview(), uiTexts.getNotificationsMarkAll())).isDisabled();
      inbox.button(inbox.preview(), uiTexts.getNotificationsHistory()).click();

      assertThat(browserActor.browserPage.locator("#notifications-inbox-unread-total"))
          .hasText("0");
      browserActor.browserPage.reload();

      assertThat(browserActor.browserPage.locator("#notifications-inbox-unread-total"))
          .hasText("0");
      assertThat(browserActor.browserPage.locator("#notifications-inbox-total")).hasText("11");
      AllurePlaywright.attachScreenshot(
          "Persistente Inbox und Pagination", browserActor.browserPage);
    }
  }

  private void capturePagination(
      Page page, Locator paginationLocator, String context, int viewportWidth) {
    paginationLocator.scrollIntoViewIfNeeded();
    String evidenceName = "Pagination-" + context + "-" + viewportWidth;
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(paginationLocator.screenshot()),
        AttachmentOptions.empty());
    String geometry =
        (String)
            paginationLocator.evaluate(
                """
                element => JSON.stringify({
                  width: element.getBoundingClientRect().width,
                  height: element.getBoundingClientRect().height,
                  numbers: [...element.querySelectorAll('vaadin-button')]
                    // Rein numerische Beschriftungen erkennen die Seitenzahlen, nicht die Pfeile.
                    .filter(button => /^[0-9]+$/.test(button.textContent.trim()))
                    .map(button => {
                      const box = button.getBoundingClientRect();
                      const label = button.shadowRoot.querySelector('[part=label]').getBoundingClientRect();
                      return {number: button.textContent.trim(), width: box.width, height: box.height,
                        offsetX: label.x + label.width / 2 - box.x - box.width / 2};
                    })
                })
                """);
    Allure.attachment(evidenceName + "-geometry", "application/json", geometry);
    assertTrue(
        (Boolean)
            paginationLocator.evaluate(
                """
                element => {
                  // Rein numerische Beschriftungen erkennen die Seitenzahlen, nicht die Pfeile.
                  const buttons = [...element.querySelectorAll('vaadin-button')]
                    .filter(button => /^[0-9]+$/.test(button.textContent.trim()));
                  return buttons.length > 0 && buttons.every(button => {
                    const box = button.getBoundingClientRect();
                    const label = button.shadowRoot.querySelector('[part=label]').getBoundingClientRect();
                    return Math.abs(label.x + label.width / 2 - box.x - box.width / 2) < 0.1;
                  });
                }
                """),
        evidenceName + ": Seitenzahlen müssen mittig stehen: " + geometry);
    AllurePlaywright.attachScreenshot(evidenceName + "-page", page);
  }

  private void attachMetricCardEvidence(Page browserPage, int viewportWidth) {
    Locator metricCardsLocator = browserPage.locator(".cosmos-notification-inbox-counts");
    assertThat(metricCardsLocator.locator(":scope > vaadin-card")).hasCount(2);
    assertThat(browserPage.locator("#notifications-inbox-total")).hasText("3");
    assertThat(browserPage.locator("#notifications-inbox-unread-total")).hasText("3");

    metricCardsLocator.scrollIntoViewIfNeeded();
    Allure.attachment(
        "Kennzahlkarten " + viewportWidth,
        "image/png",
        new ByteArrayInputStream(metricCardsLocator.screenshot()),
        AttachmentOptions.empty());

    String metricStylesJson =
        (String)
            metricCardsLocator.evaluate(
                """
                group => JSON.stringify([...group.children].map(card => {
                  const bounds = card.getBoundingClientRect();
                  const cardStyle = getComputedStyle(card);
                  const label = card.querySelector('span');
                  const value = card.querySelector('[id^="notifications-inbox-"]');
                  const typography = element => {
                    const style = getComputedStyle(element);
                    return {text: element.textContent, color: style.color, font: style.font,
                      letterSpacing: style.letterSpacing, marginTop: style.marginTop};
                  };
                  return {width: bounds.width, height: bounds.height,
                    background: cardStyle.backgroundColor, border: cardStyle.border,
                    radius: cardStyle.borderRadius, label: typography(label), value: typography(value)};
                }))
                """);
    Allure.attachment("Kennzahl-Messwerte " + viewportWidth, "application/json", metricStylesJson);
  }

  @ParameterizedTest(name = "NOTIFY-05: Sammellesen mit unveränderter Bestätigung bei {0}px")
  @ValueSource(ints = {1280, 390})
  @Tag("confirmation-dialog")
  @Description(
      """
      Zwei Nachrichten über das Versandformular senden. In Inbox und Glockenvorschau die
      Sammelbestätigung aufnehmen und vermessen. Abbrechen, Schließen, Escape und Außenklick
      erhalten beide ungelesenen Nachrichten. Die Tastatur erreicht Schließen und beide Aktionen
      vorwärts und rückwärts. Erst ausdrückliches Bestätigen markiert beide Nachrichten dauerhaft als gelesen.
      """)
  void markAllConfirmationPreservesLayoutAndExplicitActions(int viewportWidth) {
    String recipientEmail = prepareRecipient();
    send(recipientEmail, "Confirmation first", "First unread notification");
    send(recipientEmail, "Confirmation second", "Second unread notification");

    try (BrowserActor recipientBrowserActor = new BrowserActor(testBrowser)) {
      Page recipientBrowserPage = recipientBrowserActor.browserPage;
      recipientBrowserActor.login(recipientEmail, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(recipientBrowserPage).selectTestLanguage();
      recipientBrowserPage.setViewportSize(viewportWidth, 1000);
      NotificationInboxPage notificationInboxPage = new NotificationInboxPage(recipientBrowserPage);
      notificationInboxPage.open();

      for (boolean fromPreview : new boolean[] {false, true}) {
        if (fromPreview) {
          notificationInboxPage.openPreview();
        }
        Locator notificationPanelLocator =
            fromPreview ? notificationInboxPage.preview() : notificationInboxPage.inbox();
        Locator markAllButtonLocator =
            notificationInboxPage.button(
                notificationPanelLocator, uiTexts.getNotificationsMarkAll());
        Locator confirmationDialogLocator =
            recipientBrowserPage.locator(".portal-confirmation-dialog[opened]");

        openMarkAllConfirmation(
            recipientBrowserPage, markAllButtonLocator, confirmationDialogLocator);
        assertThat(confirmationDialogLocator).hasJSProperty("localName", "vaadin-confirm-dialog");
        assertThat(confirmationDialogLocator.locator(".cosmos-panel-title"))
            .hasText(uiTexts.getNotificationsMarkAll());
        assertThat(confirmationDialogLocator.locator(".cosmos-overlay-close")).isFocused();
        attachConfirmationEvidence(
            recipientBrowserPage,
            confirmationDialogLocator,
            viewportWidth,
            fromPreview ? "preview" : "inbox");

        Locator closeDialogButtonLocator =
            confirmationDialogLocator.locator(".cosmos-overlay-close");
        Locator cancelButtonLocator =
            confirmationDialogLocator.locator("vaadin-button[id$='-cancel']");
        Locator confirmButtonLocator =
            confirmationDialogLocator.locator("vaadin-button[id$='-submit']");
        closeDialogButtonLocator.focus();
        closeDialogButtonLocator.press("Tab");
        assertThat(cancelButtonLocator).isFocused();
        cancelButtonLocator.press("Tab");
        assertThat(confirmButtonLocator).isFocused();
        confirmButtonLocator.press("Shift+Tab");
        assertThat(cancelButtonLocator).isFocused();
        cancelButtonLocator.press("Shift+Tab");
        assertThat(closeDialogButtonLocator).isFocused();
        cancelButtonLocator.click();
        assertConfirmationCancelled(recipientBrowserPage, markAllButtonLocator);

        openMarkAllConfirmation(
            recipientBrowserPage, markAllButtonLocator, confirmationDialogLocator);
        closeDialogButtonLocator.click();
        assertConfirmationCancelled(recipientBrowserPage, markAllButtonLocator);

        openMarkAllConfirmation(
            recipientBrowserPage, markAllButtonLocator, confirmationDialogLocator);
        recipientBrowserPage.keyboard().press("Escape");
        assertConfirmationCancelled(recipientBrowserPage, markAllButtonLocator);

        openMarkAllConfirmation(
            recipientBrowserPage, markAllButtonLocator, confirmationDialogLocator);
        assertThat(confirmationDialogLocator.locator("[part~='overlay']").first()).isVisible();
        recipientBrowserPage.mouse().click(2, 2);
        assertConfirmationCancelled(recipientBrowserPage, markAllButtonLocator);
      }

      notificationInboxPage
          .button(notificationInboxPage.preview(), uiTexts.getNotificationsMarkAll())
          .click();
      notificationInboxPage.confirm();
      assertThat(
              notificationInboxPage.button(
                  notificationInboxPage.preview(), uiTexts.getNotificationsMarkAll()))
          .isDisabled();
      recipientBrowserPage.reload();
      assertThat(recipientBrowserPage.locator("#notifications-inbox-unread-total")).hasText("0");
      assertThat(recipientBrowserPage.locator("#notifications-inbox-total")).hasText("2");
    }
  }

  private void assertConfirmationCancelled(
      Page recipientBrowserPage, Locator markAllButtonLocator) {
    assertThat(recipientBrowserPage.locator(".portal-confirmation-dialog[opened]")).hasCount(0);
    assertThat(recipientBrowserPage.locator("#notifications-inbox-unread-total")).hasText("2");
    assertThat(markAllButtonLocator).isEnabled();
  }

  private void openMarkAllConfirmation(
      Page recipientBrowserPage, Locator markAllButtonLocator, Locator confirmationDialogLocator) {
    UiActions.complete(recipientBrowserPage, markAllButtonLocator::click);
    assertThat(confirmationDialogLocator.locator("[part~='overlay']").first()).isVisible();
    recipientBrowserPage.waitForCondition(() ->
        (Boolean)
            confirmationDialogLocator.evaluate(
                "dialog => !dialog.hasAttribute('opening') && !dialog.hasAttribute('closing')"));
  }

  private void attachConfirmationEvidence(
      Page recipientBrowserPage,
      Locator confirmationDialogLocator,
      int viewportWidth,
      String panelName) {
    Locator dialogSurfaceLocator = confirmationDialogLocator.locator("[part~='overlay']").first();
    assertThat(dialogSurfaceLocator).isVisible();
    recipientBrowserPage.mouse().move(0, 0);
    String evidenceName = "confirmation-" + panelName + "-" + viewportWidth;
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(dialogSurfaceLocator.screenshot()),
        AttachmentOptions.empty());
    AllurePlaywright.attachScreenshot(evidenceName + "-page", recipientBrowserPage);
    String dialogMetricsJson =
        (String)
            confirmationDialogLocator.evaluate(
                """
                dialog => {
                  const describe = element => {
                    const bounds = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return {x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height,
                      font: style.font, color: style.color, background: style.backgroundColor,
                      border: style.border, radius: style.borderRadius, padding: style.padding};
                  };
                  return JSON.stringify([...dialog.querySelectorAll(
                    '.account-change-dialog-header,.cosmos-panel-title,.cosmos-panel-description,' +
                    '.portal-confirm-body,.portal-confirm-question,.portal-confirm-explanation,' +
                    '.portal-context-card,.cosmos-panel-hint,vaadin-button')].map(describe));
                }
                """);
    Allure.attachment(evidenceName + "-metrics", "application/json", dialogMetricsJson);
  }
}
