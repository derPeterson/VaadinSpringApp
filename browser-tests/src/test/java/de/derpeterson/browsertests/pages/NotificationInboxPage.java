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

/** Echte UI-Aktionen für persönliche Inbox und Header-Vorschau, ohne Backendzugriff. */
public final class NotificationInboxPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public NotificationInboxPage(Page page) {
    this.page = page;
  }

  public Locator inbox() {
    return page.locator("#notifications-inbox");
  }

  public Locator preview() {
    return page.locator("#notifications-preview");
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
    return page.locator("vaadin-dialog.cosmos-notification-dialog[opened]");
  }

  @Step("Persönliche Inbox direkt öffnen")
  public void open() {
    page.navigate(path(NOTIFICATIONS_ROUTE));
    assertThat(inbox()).isVisible();
  }

  @Step("Benachrichtigungsvorschau über den gemeinsamen Header öffnen")
  public void openPreview() {
    page.locator("#account-header-notifications").click();
    assertThat(preview()).isVisible();
    assertThat(
            preview()
                .getByRole(
                    AriaRole.BUTTON,
                    new Locator.GetByRoleOptions()
                        .setName(texts.getNotificationsDismissRead())
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
        page, () -> page.locator("#notifications-inbox-search input").fill(query));
  }

  @Step("Seitengröße im nativen Menü wählen")
  public void pageSize(Locator root, String value) {
    root.getByRole(
            AriaRole.COMBOBOX,
            new Locator.GetByRoleOptions()
                .setName(texts.getAdminUsersPaginationPerPage())
                .setExact(true))
        .click();
    page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(value).setExact(true))
        .click();
  }

  @Step("Detail öffnen: voller Text und keine automatische Lesemarkierung")
  public void openDetail(Locator root, String subject, String body, boolean fromPreview) {
    row(root, subject).locator(".cosmos-notification-row-open").click();
    assertThat(dialog().locator(".cosmos-notification-detail-title")).hasText(subject);
    assertThat(dialog().locator(".cosmos-notification-detail-body")).hasText(body);
    assertThat(dialog().locator(".cosmos-notification-detail-state"))
        .hasText(texts.getNotificationsUnread());
    if (fromPreview) {
      assertThat(dialog().locator(".cosmos-notification-detail-history")).isVisible();
    } else {
      assertThat(dialog().locator(".cosmos-notification-detail-history")).hasCount(0);
    }
  }

  public void closeDetail() {
    button(dialog(), texts.getNotificationsClose()).last().click();
    assertThat(dialog()).hasCount(0);
  }

  @Step("Iconaktionen und Metadaten bilden eine gemeinsame untere Zeile")
  public void assertRowLayout(Locator root, boolean compact) {
    String footerClass =
        compact
            ? ".cosmos-notification-preview-row-footer"
            : ".cosmos-notification-inbox-row-footer";
    for (Locator row : rows(root).all()) {
      Locator footer = row.locator(footerClass);
      assertThat(footer).isVisible();
      for (Locator action : row.locator(".cosmos-notification-row-action").all()) {
        // Tooltiptexte gehören zur Zugänglichkeit; nur die sichtbare Buttonbeschriftung muss leer
        // sein.
        assertEquals(
            "",
            action.evaluate(
                "el => Array.from(el.childNodes).filter(n => n.nodeType === Node.TEXT_NODE).map(n => n.textContent).join('').trim()"));
        assertThat(action.locator("vaadin-icon")).hasCount(1);
        assertTrue(action.boundingBox().y >= footer.boundingBox().y - 1);
        assertTrue(
            action.boundingBox().y + action.boundingBox().height
                <= footer.boundingBox().y + footer.boundingBox().height + 1);
      }
    }
    Locator firstRow = rows(root).first();
    Locator open = firstRow.locator(".cosmos-notification-row-open");
    Locator mark = firstRow.locator(".cosmos-notification-row-action").nth(1);
    Locator tooltip = page.getByRole(AriaRole.TOOLTIP);
    open.hover();
    assertThat(tooltip).hasText(texts.getNotificationsOpen());
    mark.hover();
    assertThat(tooltip).hasText(texts.getNotificationsMarkRead());
    page.mouse().move(0, 0);
    open.focus();
    open.press("Tab");
    assertThat(mark).isFocused();
    assertTrue(
        (Boolean) root.evaluate("el => el.scrollWidth <= el.clientWidth + 1"),
        "Kein horizontaler Überlauf im Panel");
    assertTrue(
        (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= innerWidth + 1"),
        "Kein horizontaler Seitenüberlauf");
  }
}
