package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.*;
import static de.derpeterson.browsertests.BrowserRoutes.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Nur das freigegebene Verfassenpanel und die persönliche Inbox; kein Header-Overlay. */
public final class NotificationComposePage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public NotificationComposePage(Page page) {
    this.page = page;
  }

  public Locator root() {
    return page.locator(".cosmos-notification-compose");
  }

  public Locator title() {
    return page.locator("#admin-notifications-title input");
  }

  public Locator body() {
    return page.locator("#admin-notifications-body textarea");
  }

  public Locator send() {
    return page.locator("#admin-notifications-send");
  }

  public Locator review() {
    return page.locator("#admin-notifications-preview");
  }

  public Locator recipients() {
    return page.locator("#admin-notifications-recipients");
  }

  public Locator everyone() {
    return page.locator("#admin-notifications-everyone input");
  }

  public Locator status() {
    return root().locator(".cosmos-notification-compose-status");
  }

  @Step("Benachrichtigungen verfassen direkt über die vorhandene Adminroute öffnen")
  public void open() {
    page.navigate(path(ADMIN_ROUTE) + "?section=notifications");
    assertThat(root()).isVisible();
    assertThat(title()).isVisible();
  }

  @Step("Bestimmten aktiven Empfänger im nativen Mehrfachauswahlmenü wählen")
  public void select(String email) {
    recipients().locator("input").fill(email);
    Locator option =
        page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText(email));
    assertThat(option).hasCount(1);
    option.click();
    recipients().locator("input").press("Escape");
    assertThat(root().locator(".cosmos-notification-compose-recipient-summary"))
        .hasText(texts.getNotificationsSelectedUsers(1));
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
    assertThat(page.locator("#admin-notifications-title"))
        .hasJSProperty("errorMessage", texts.getNotificationsInvalidSubject());
    assertThat(page.locator("#admin-notifications-title[invalid]")).hasCount(1);
    assertThat(page.locator("#admin-notifications-body[invalid]")).hasCount(1);
    assertThat(page.locator("#admin-notifications-body"))
        .hasJSProperty("errorMessage", texts.getNotificationsInvalidBody());
    assertThat(page.locator("#admin-notifications-recipients[invalid]")).hasCount(1);
    assertThat(recipients()).hasJSProperty("errorMessage", texts.getNotificationsNoRecipients());
    notice(
        texts.getNotificationsPreviewFailedTitle(), texts.getNotificationsPreviewFailedMessage());
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
    assertThat(status()).hasText(texts.getNotificationsPreviewUpdated());
    assertThat(root().locator(".cosmos-notification-compose-preview-content"))
        .containsText(subject);
    assertThat(root().locator(".cosmos-notification-compose-preview-content"))
        .containsText(message);
  }

  public void notice(String... expected) {
    Locator notice = page.locator("vaadin-notification-card:not([closing])");
    assertThat(notice).isVisible();
    for (String text : expected) assertThat(notice).containsText(text);
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }

  @Step(
      "Persönliche Benachrichtigung über direkte Inbox-Navigation lesen und nach Reload wiederfinden")
  public static void readOwn(Page target, String subject, String message) {
    UiTexts texts = UiTexts.selected();
    target.navigate(path(NOTIFICATIONS_ROUTE));
    Locator row =
        target
            .locator("#notifications-inbox .cosmos-notification-row")
            .filter(new Locator.FilterOptions().setHasText(subject));
    assertThat(row).hasCount(1);
    // .* erlaubt weitere CSS-Klassen vor/nach dem wörtlichen Status; keine Gruppen/Ersetzungen.
    assertThat(row)
        .hasClass(java.util.regex.Pattern.compile(".*cosmos-notification-row--unread.*"));
    row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(subject).setExact(true))
        .click();
    Locator dialog = target.locator("vaadin-dialog.cosmos-notification-dialog[opened]");
    assertThat(dialog).containsText(message);
    assertThat(dialog.locator(".cosmos-notification-detail-state"))
        .hasText(texts.getNotificationsUnread());
    dialog
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(texts.getNotificationsMarkRead()).setExact(true))
        .click();
    assertThat(dialog.locator(".cosmos-notification-detail-state"))
        .hasText(texts.getNotificationsRead());
    dialog
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(texts.getNotificationsClose()).setExact(true))
        .last()
        .click();
    assertThat(dialog).hasCount(0);
    target.reload();
    assertThat(row).hasCount(1);
    assertThat(row)
        .not()
        .hasClass(java.util.regex.Pattern.compile(".*cosmos-notification-row--unread.*"));
  }
}
