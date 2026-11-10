package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.NOTIFICATIONS_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Versandformular und bestehende Zustellungsjourney; weitere Inbox-/Overlayaktionen separat. */
public final class NotificationComposePage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public NotificationComposePage(Page browserPage) {
    this.browserPage = browserPage;
  }

  public Locator root() {
    return browserPage.locator(".cosmos-notification-compose");
  }

  public Locator title() {
    return browserPage.locator("#admin-notifications-title input");
  }

  public Locator body() {
    return browserPage.locator("#admin-notifications-body textarea");
  }

  public Locator send() {
    return browserPage.locator("#admin-notifications-send");
  }

  public Locator review() {
    return browserPage.locator("#admin-notifications-preview");
  }

  public Locator recipients() {
    return browserPage.locator("#admin-notifications-recipients");
  }

  public Locator everyone() {
    return browserPage.locator("#admin-notifications-everyone input");
  }

  public Locator status() {
    return browserPage.locator(
        ".admin-notification-preview-card .cosmos-notification-compose-status");
  }

  @Step("Benachrichtigungen verfassen direkt über die vorhandene Adminroute öffnen")
  public void open() {
    browserPage.navigate(path(ADMIN_ROUTE) + "?section=notifications");

    assertThat(root()).isVisible();
    assertThat(title()).isVisible();
  }

  @Step("Bestimmten aktiven Empfänger im nativen Mehrfachauswahlmenü wählen")
  public void select(String email) {
    recipients().locator("input").fill(email);
    Locator recipientOptionLocator =
        browserPage
            .getByRole(AriaRole.OPTION)
            .filter(new Locator.FilterOptions().setHasText(email));

    assertThat(recipientOptionLocator).hasCount(1);
    recipientOptionLocator.click();
    recipients().locator("input").press("Escape");

    assertThat(root().locator(".cosmos-notification-compose-recipient-summary"))
        .hasText(uiTexts.getNotificationsSelectedUsers(1));
  }

  @Step("Allgemeinen Vorschaufehler mit konkreten Hinweisen an allen leeren Feldern prüfen")
  public void assertEmptyReview() {
    assertThat(send()).isDisabled();
    title().fill("Temporary");
    title().clear();
    body().fill("Temporary");
    body().clear();
    title().press("Tab");

    assertThat(root().locator("[invalid]")).hasCount(0);
    review().click();

    assertThat(browserPage.locator("#admin-notifications-title"))
        .hasJSProperty("errorMessage", uiTexts.getNotificationsInvalidSubject());
    assertThat(browserPage.locator("#admin-notifications-title[invalid]")).hasCount(1);
    assertThat(browserPage.locator("#admin-notifications-body[invalid]")).hasCount(1);
    assertThat(browserPage.locator("#admin-notifications-body"))
        .hasJSProperty("errorMessage", uiTexts.getNotificationsInvalidBody());
    assertThat(browserPage.locator("#admin-notifications-recipients[invalid]")).hasCount(1);
    assertThat(recipients()).hasJSProperty("errorMessage", uiTexts.getNotificationsNoRecipients());
    notice(
        uiTexts.getNotificationsPreviewFailedTitle(),
        uiTexts.getNotificationsPreviewFailedMessage());
  }

  @Step("Eigenen Entwurf in der tatsächlich gerenderten Vorschau freigeben")
  public void draft(String subject, String message) {
    title().fill(subject);
    body().fill(message);

    assertThat(root().locator(".cosmos-notification-compose-counter").nth(0))
        .hasText(subject.length() + " / 120");
    assertThat(root().locator(".cosmos-notification-compose-counter").nth(1))
        .hasText(message.length() + " / 4000");
    assertThat(send()).isDisabled();
    review().click();

    assertThat(send()).isEnabled();
    assertThat(status()).hasText(uiTexts.getNotificationsPreviewUpdated());
    assertThat(
            browserPage.locator(
                ".admin-notification-preview-card .cosmos-notification-compose-preview-content"))
        .containsText(subject);
    assertThat(
            browserPage.locator(
                ".admin-notification-preview-card .cosmos-notification-compose-preview-content"))
        .containsText(message);
  }

  public void notice(String... expected) {
    Locator notificationCardLocator =
        browserPage.locator("vaadin-notification-card:not([closing])");

    assertThat(notificationCardLocator).isVisible();
    for (String text : expected) {
      assertThat(notificationCardLocator).containsText(text);
    }
    notificationCardLocator.getByRole(AriaRole.BUTTON).click();

    assertThat(notificationCardLocator).not().isVisible();
  }

  @Step(
      "Persönliche Benachrichtigung über direkte Inbox-Navigation lesen und nach Reload wiederfinden")
  public static void readOwn(Page target, String subject, String message) {
    UiTexts uiTexts = UiTexts.selected();
    target.navigate(path(NOTIFICATIONS_ROUTE));
    Locator notificationRowLocator =
        target
            .locator("#notifications-inbox .cosmos-notification-row")
            .filter(new Locator.FilterOptions().setHasText(subject));

    assertThat(notificationRowLocator).hasCount(1);
    // .* erlaubt weitere CSS-Klassen vor/nach dem wörtlichen Status; keine Gruppen/Ersetzungen.
    assertThat(notificationRowLocator)
        .hasClass(java.util.regex.Pattern.compile(".*cosmos-notification-row--unread.*"));
    notificationRowLocator
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(subject).setExact(true))
        .click();
    Locator notificationDetailDialogLocator =
        target.locator("vaadin-dialog.cosmos-notification-dialog[opened]");

    assertThat(notificationDetailDialogLocator).containsText(message);
    assertThat(notificationDetailDialogLocator.locator(".cosmos-notification-detail-state"))
        .hasText(uiTexts.getNotificationsUnread());
    notificationDetailDialogLocator
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getNotificationsMarkRead())
                .setExact(true))
        .click();
    new NotificationInboxPage(target).confirm();

    assertThat(notificationDetailDialogLocator.locator(".cosmos-notification-detail-state"))
        .hasText(uiTexts.getNotificationsRead());
    notificationDetailDialogLocator
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(uiTexts.getNotificationsClose()).setExact(true))
        .last()
        .click();

    assertThat(notificationDetailDialogLocator).hasCount(0);
    target.reload();

    assertThat(notificationRowLocator).hasCount(1);
    assertThat(notificationRowLocator)
        .not()
        .hasClass(java.util.regex.Pattern.compile(".*cosmos-notification-row--unread.*"));
  }
}
