package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.NOTIFICATIONS_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import io.qameta.allure.playwright.AllurePlaywright;

/** Echte UI-Aktionen für persönliche Inbox und Header-Vorschau, ohne Backendzugriff. */
public final class NotificationInboxPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public NotificationInboxPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  public Locator inbox() {
    return browserPage.locator("#notifications-inbox");
  }

  public Locator preview() {
    return browserPage.locator("#notifications-preview");
  }

  public Locator rows(Locator root) {
    return root.locator(".cosmos-notification-row");
  }

  public Locator row(Locator root, String subject) {
    return rows(root).filter(new Locator.FilterOptions().setHasText(subject));
  }

  public Locator button(Locator root, String name) {
    return root.getByRole(
        AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
  }

  public Locator dialog() {
    return browserPage.locator("vaadin-dialog.cosmos-notification-dialog[opened]");
  }

  @Step("Persönliche Inbox direkt öffnen")
  public void open() {
    browserPage.navigate(path(NOTIFICATIONS_ROUTE));

    assertThat(inbox()).isVisible();
  }

  @Step("Benachrichtigungsvorschau über den gemeinsamen Header öffnen")
  public void openPreview() {
    browserPage.locator("#account-header-notifications").click();

    assertThat(preview()).isVisible();
    assertThat(
            preview()
                .getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions()
                        .setName(uiTexts.getNotificationsDismissRead())
                        .setExact(true)))
        .hasCount(0);
  }

  @Step("Nativen Alle-/Gelesen-/Ungelesen-Filter bedienen")
  public void filter(Locator root, String label) {
    // Das sichtbare native Label bleibt klickbar, während das Segment den Radiokreis ausblendet.
    root.locator("vaadin-radio-button")
        .getByText(label, new Locator.GetByTextOptions().setExact(true))
        .click();

    assertThat(
            root.getByRole(
                AriaRole.RADIO, new Locator.GetByRoleOptions().setName(label).setExact(true)))
        .isChecked();
  }

  @Step("Nach Betreff suchen und die tatsächliche Wertänderung abwarten")
  public void search(String query) {
    UiActions.completeValueChange(
        browserPage, () -> browserPage.locator("#notifications-inbox-search input").fill(query));
  }

  @Step("Seitengröße im nativen Menü wählen")
  public void pageSize(Locator root, String value) {
    root.getByRole(
            AriaRole.COMBOBOX,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getAdminUsersPaginationPerPage())
                .setExact(true))
        .click();
    browserPage
        .getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(value).setExact(true))
        .click();
  }

  @Step("Detail öffnen: voller Text und keine automatische Lesemarkierung")
  public void openDetail(Locator root, String subject, String body, boolean fromPreview) {
    row(root, subject).locator(".cosmos-notification-row-open").click();

    assertThat(dialog().locator(".cosmos-notification-detail-title")).hasText(subject);
    assertThat(dialog().locator(".cosmos-notification-detail-body")).hasText(body);
    assertThat(dialog().locator(".cosmos-notification-detail-state"))
        .hasText(uiTexts.getNotificationsUnread());
    assertThat(
            dialog()
                .locator(
                    ".cosmos-notification-detail-message .cosmos-notification-detail-mark-read"))
        .isVisible();
    assertTrue(
        (Boolean)
            dialog()
                .locator(".cosmos-notification-detail-actions")
                .evaluate("el => el.scrollWidth <= el.clientWidth + 1"),
        "Die beschrifteten Footeraktionen dürfen nicht horizontal überlaufen");
    AllurePlaywright.attachScreenshot(
        "Benachrichtigungsdetail mit beschrifteten Aktionen", browserPage);
    button(dialog(), uiTexts.getNotificationsMoreActions()).click();
    Locator detailToolsMenuLocator =
        browserPage.locator(".cosmos-notification-detail-tools:visible");

    assertThat(detailToolsMenuLocator).isVisible();
    if (fromPreview) {
      assertThat(detailToolsMenuLocator.locator(".cosmos-notification-detail-history")).isVisible();
    } else {
      assertThat(detailToolsMenuLocator.locator(".cosmos-notification-detail-history")).hasCount(0);
    }
    assertThat(detailToolsMenuLocator.locator(".cosmos-notification-detail-delete")).isVisible();
    browserPage.keyboard().press("Escape");

    assertThat(detailToolsMenuLocator).hasCount(0);
    // Vaadin zeigt den Dialoginhalt im Overlay; der Flow-Host selbst hat keine sichtbare Fläche.
    assertThat(dialog().locator(".cosmos-notification-detail-title")).isVisible();
  }

  @Step("Die ausdrücklich ausgewählte Nachrichtenaktion bestätigen")
  public void confirm() {
    Locator deleteConfirmationDialogLocator =
        browserPage.locator(".portal-confirmation-dialog[opened]");

    assertThat(deleteConfirmationDialogLocator.locator("vaadin-dialog-overlay > [part~='overlay']"))
        .isVisible();
    UiActions.complete(
        browserPage,
        () -> deleteConfirmationDialogLocator.locator("vaadin-button[id$='-submit']").click());

    assertThat(browserPage.locator(".portal-confirmation-dialog[opened]")).hasCount(0);
  }

  public void closeDetail() {
    button(dialog(), uiTexts.getNotificationsClose()).last().click();

    assertThat(dialog()).hasCount(0);
  }

  @Step("Iconaktionen und Metadaten bilden eine gemeinsame Zeile")
  public void assertRowLayout(Locator root, boolean compact) {
    String footerClass =
        compact ? ".cosmos-notification-preview-actions" : ".cosmos-notification-inbox-row-header";
    for (Locator row : rows(root).all()) {
      Locator notificationFooterLocator = row.locator(footerClass);

      assertThat(notificationFooterLocator).isVisible();
      for (Locator action : row.locator(".cosmos-notification-row-action").all()) {
        // Tooltiptexte gehören zur Zugänglichkeit; nur die sichtbare Buttonbeschriftung muss leer
        // sein.
        assertEquals(
            "",
            action.evaluate(
                "el => Array.from(el.childNodes).filter(n => n.nodeType === Node.TEXT_NODE).map(n => n.textContent).join('').trim()"));
        assertThat(action.locator("vaadin-icon")).hasCount(1);
        assertTrue(action.boundingBox().y >= notificationFooterLocator.boundingBox().y - 1);
        assertTrue(
            action.boundingBox().y + action.boundingBox().height
                <= notificationFooterLocator.boundingBox().y
                    + notificationFooterLocator.boundingBox().height
                    + 1);
      }
    }
    Locator firstRow = rows(root).first();
    Locator open = firstRow.locator(".cosmos-notification-row-open");
    Locator mark = firstRow.locator(".cosmos-notification-row-action").nth(1);
    Locator tooltipLocator = browserPage.getByRole(AriaRole.TOOLTIP);
    open.hover();

    assertThat(tooltipLocator).hasText(uiTexts.getNotificationsOpen());
    mark.hover();

    assertThat(tooltipLocator).hasText(uiTexts.getNotificationsMarkRead());
    browserPage.mouse().move(0, 0);
    open.focus();
    open.press("Tab");

    assertThat(mark).isFocused();
    assertTrue(
        (Boolean) root.evaluate("el => el.scrollWidth <= el.clientWidth + 1"),
        "Kein horizontaler Überlauf im Panel");
    assertTrue(
        (Boolean)
            browserPage.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
        "Kein horizontaler Seitenüberlauf");
  }
}
