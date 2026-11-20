package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.NOTIFICATIONS_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
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
    Locator filterTabLocator =
        root.getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName(label).setExact(true));
    if (!"true".equals(filterTabLocator.getAttribute("aria-selected"))) {
      UiActions.complete(browserPage, filterTabLocator::click);
    }

    assertThat(filterTabLocator).hasAttribute("aria-selected", "true");
  }

  @Step("Nach Betreff suchen und die tatsächliche Wertänderung abwarten")
  public void search(String query) {
    UiActions.completeValueChange(
        browserPage, () -> browserPage.locator("#notifications-inbox-search input").fill(query));
  }

  @Step("Seitengröße im nativen Menü wählen")
  public void pageSize(Locator root, String value) {
    Locator pageSizeInput =
        root.getByRole(
            AriaRole.COMBOBOX,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getAdminUsersPaginationPerPage())
                .setExact(true));
    pageSizeInput.fill(value);
    Locator matchingOption =
        browserPage.getByRole(
            AriaRole.OPTION, new Page.GetByRoleOptions().setName(value).setExact(true));
    assertThat(matchingOption).isVisible();
    pageSizeInput.press("ArrowDown");
    pageSizeInput.press("Enter");
    assertThat(browserPage.locator("vaadin-combo-box-overlay[opened]")).hasCount(0);
    assertThat(pageSizeInput).hasValue(value);
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
    Locator notificationConfirmationDialogLocator =
        browserPage.locator(".portal-confirmation-dialog[opened]");

    assertThat(notificationConfirmationDialogLocator.locator("[part~='overlay']").first())
        .isVisible();
    ConfirmationAppearanceChecks.capture(
        browserPage, notificationConfirmationDialogLocator, "notification-action");
    UiActions.complete(
        browserPage,
        () ->
            notificationConfirmationDialogLocator.locator("vaadin-button[id$='-submit']").click());

    assertThat(browserPage.locator(".portal-confirmation-dialog[opened]")).hasCount(0);
  }

  public void closeDetail() {
    button(dialog(), uiTexts.getNotificationsClose()).last().click();

    assertThat(dialog()).hasCount(0);
  }

  @Step("Titel, Nachricht und Metadaten teilen die linke Spalte")
  public void assertRowLayout(Locator root, boolean compact) {
    for (Locator row : rows(root).all()) {
      assertRowContentSpacing(row, compact);
      Locator notificationActionsLocator = row.locator(rowPartSelector(compact, "actions"));

      for (Locator action : notificationActionsLocator.locator("vaadin-button:visible").all()) {
        // Tooltiptexte gehören zur Zugänglichkeit; nur die sichtbare Buttonbeschriftung muss leer
        // sein.
        assertEquals(
            "",
            action.evaluate(
                "el => Array.from(el.childNodes).filter(n => n.nodeType === Node.TEXT_NODE).map(n => n.textContent).join('').trim()"));
        assertThat(action.locator("vaadin-icon")).hasCount(1);
        assertTrue(action.boundingBox().y >= notificationActionsLocator.boundingBox().y - 1);
        assertTrue(
            action.boundingBox().y + action.boundingBox().height
                <= notificationActionsLocator.boundingBox().y
                    + notificationActionsLocator.boundingBox().height
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

  private void assertRowContentSpacing(Locator row, boolean compact) {
    int padding = compact ? 12 : 16;
    int titleGap = compact ? 4 : 8;
    int metadataGap = compact ? 8 : 12;
    Locator title = row.locator(".cosmos-notification-title");
    Locator titleLabel = title.locator("[part=label]");
    Locator actions = row.locator(rowPartSelector(compact, "actions"));
    assertThat(actions).isVisible();
    assertThat(title.locator("vaadin-icon")).hasCount(0);
    assertThat(titleLabel).hasCSS("text-align", "left");
    assertThat(row).hasCSS("padding", padding + "px");
    assertThat(row).hasCSS("border-left-width", "3px");

    BoundingBox rowBounds = row.boundingBox();
    BoundingBox titleBounds = title.boundingBox();
    BoundingBox messageBounds = row.locator(rowPartSelector(compact, "message")).boundingBox();
    BoundingBox metadataBounds = row.locator(rowPartSelector(compact, "meta")).boundingBox();
    BoundingBox actionsBounds = actions.boundingBox();
    assertEquals(rowBounds.x + padding + 3, titleLabel.boundingBox().x, 1);
    assertEquals(titleBounds.x, messageBounds.x, 1);
    assertEquals(titleBounds.x, metadataBounds.x, 1);
    assertEquals(titleGap, messageBounds.y - titleBounds.y - titleBounds.height, 1);
    assertEquals(metadataGap, metadataBounds.y - messageBounds.y - messageBounds.height, 1);
    assertEquals(titleBounds.y, actionsBounds.y, 1);
    assertTrue(actionsBounds.x >= titleBounds.x + titleBounds.width);
    assertEquals(
        rowBounds.x + rowBounds.width - padding - 1, actionsBounds.x + actionsBounds.width, 1);
    assertEquals(
        rowBounds.y + rowBounds.height - padding - 1, metadataBounds.y + metadataBounds.height, 1);
  }

  public void assertPreviewReadLine(Locator row, boolean unread) {
    assertThat(row).hasCSS("border-left-color", unread ? "rgb(0, 119, 182)" : "rgb(148, 163, 184)");
    assertRowContentSpacing(row, true);
    CosmosFollowUpChecks.assertReadableBadge(
        row.locator("vaadin-badge.cosmos-notification-preview-source"), 22);
    CosmosFollowUpChecks.assertReadableBadge(
        row.locator("vaadin-badge.cosmos-notification-preview-state"), 22);
  }

  private String rowPartSelector(boolean compact, String part) {
    return ".cosmos-notification-" + (compact ? "preview-" : "inbox-") + part;
  }
}
