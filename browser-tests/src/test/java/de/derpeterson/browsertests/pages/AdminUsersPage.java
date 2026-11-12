package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import io.qameta.allure.Step;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

/** User list and optional filters; form/confirmation actions have their own small helpers. */
public final class AdminUsersPage {
  private final Page browserPage;
  private final UiTexts uiTexts;

  public AdminUsersPage(Page browserPage) {
    this(browserPage, UiTexts.selected());
  }

  public AdminUsersPage(Page browserPage, UiTexts uiTexts) {
    this.browserPage = browserPage;
    this.uiTexts = uiTexts;
  }

  public Locator grid() {
    return browserPage.locator(".admin-users-grid");
  }

  @Step("Aktives Testkonto ausschließlich mit normaler Benutzerrolle anlegen")
  public void createActiveUser(String email, String password) {
    createUser(email, password, true);
  }

  public void createUser(String email, String password, boolean enabled) {
    createUser(email, password, enabled, false);
  }

  @Step("Testkonto mit gewählten Rollen anlegen und bei aktiver Freigabe per E-Mail bestätigen")
  public void createUser(String email, String password, boolean enabled, boolean administrator) {
    open();
    AdminUserDialog form = openCreate();
    form.fillNewUser(email, password, enabled, administrator);
    form.save();
    search(email);

    assertThat(emailCells(email)).hasCount(1);
    assertThat(emailCells(email)).hasText(email);

    if (enabled) {
      verifyCreatedAccount(email);
    }
  }

  /** Der Mailnachweis läuft getrennt von der angemeldeten Adminsitzung. */
  public void verifyCreatedAccount(String emailAddress) {
    verifyCreatedAccount(emailAddress, "Browser Testuser");
  }

  public void verifyCreatedAccount(String emailAddress, String recipientDisplayName) {
    try (BrowserContext verificationBrowserContext =
        browserPage
            .context()
            .browser()
            .newContext(
                new Browser.NewContextOptions()
                    .setBaseURL(System.getProperty("browser.tests.baseUrl"))
                    .setLocale(uiTexts.browserLocale()))) {
      MailpitInbox mailpitInbox = new MailpitInbox(verificationBrowserContext);
      try (Page verificationPage =
          mailpitInbox.openVerificationLink(
              mailpitInbox.awaitVerificationMail(emailAddress), recipientDisplayName)) {
        assertThat(verificationPage.locator(".public-panel")).isVisible();
      }
    }
  }

  @Step("Benutzerverwaltung öffnen")
  public void open() {
    new AdminNavigation(browserPage, uiTexts).openSection(uiTexts.getAdminUsersTitle());

    assertThat(grid()).isVisible();
  }

  @Step("Neues Benutzerformular öffnen")
  public AdminUserDialog openCreate() {
    button(uiTexts.getAdminUsersAdd()).click();
    AdminUserDialog form = new AdminUserDialog(browserPage, uiTexts);
    // Vaadin's host has no visible box; assert real rendered fields instead.
    assertThat(form.root()).hasCount(1);
    assertThat(form.firstName()).isVisible();
    return form;
  }

  @Step("Genau das eigene Konto per E-Mail suchen und dessen sichtbaren Editierbutton öffnen")
  public AdminUserDialog openEdit(String email) {
    open();
    // Login/logout and other users may have changed the stored row version.
    // Use the offered refresh action before opening a new snapshot; open forms remain untouched.
    button(uiTexts.getAdminUsersRefreshTooltip()).click();
    search(email);

    assertThat(emailCells(email)).hasCount(1);
    assertThat(emailCells(email)).hasText(email);
    Locator edit =
        grid()
            .locator("vaadin-button:visible")
            .filter(
                new Locator.FilterOptions()
                    .setHas(browserPage.locator("vaadin-icon[src='icons/lucide/square-pen.svg']")));
    // No first-row assumption: filtering must yield one visible account and action.
    assertThat(edit).hasCount(1);
    // Finish the grid's native focus/scroll movement before the mouse click.
    // Otherwise focus on mousedown can move the action before mouseup arrives.
    edit.scrollIntoViewIfNeeded();
    edit.focus();
    edit.hover();
    edit.click();
    AdminUserDialog form = new AdminUserDialog(browserPage, uiTexts);
    // Vaadin's host has no visible box; assert real rendered fields instead.
    assertThat(form.root()).hasCount(1);
    assertThat(form.firstName()).isVisible();
    assertThat(form.email()).hasValue(email);
    return form;
  }

  @Step("Benutzer über Name oder E-Mail suchen")
  public void search(String query) {
    openFilters();
    searchField().fill(query);
    searchField().press("Tab");
    applyFilters();
  }

  public Locator searchField() {
    return browserPage.getByLabel(
        uiTexts.getAdminUsersSearchLabel(), new Page.GetByLabelOptions().setExact(true));
  }

  @Step("Filterpanel bei Bedarf öffnen; alle Filter bleiben dauerhaft vorhanden")
  public void openFilters() {
    Locator filterToggleButtonLocator = button(uiTexts.getAdminUsersAddFilter());
    if (!"true".equals(filterToggleButtonLocator.getAttribute("aria-expanded"))) {
      UiActions.complete(browserPage, filterToggleButtonLocator::click);
    }
    assertThat(filterToggleButtonLocator).hasAttribute("aria-expanded", "true");
    assertThat(button(uiTexts.getAdminUsersApplyFilters())).isVisible();
  }

  @Step("Filterentwurf auswählen, ohne ihn bereits anzuwenden")
  public void selectDraftFilter(String label, String option) {
    openFilters();
    Locator filterFieldLocator = filterField(label);
    if (label.equals(uiTexts.getAdminUsersGridEnabled())) {
      filterFieldLocator.getByText(option, new Locator.GetByTextOptions().setExact(true)).click();

      assertThat(
              filterFieldLocator.getByRole(
                  AriaRole.TAB, new Locator.GetByRoleOptions().setName(option).setExact(true)))
          .hasAttribute("aria-selected", "true");
    } else {
      for (Locator choice : filterFieldLocator.locator("vaadin-checkbox").all()) {
        if (choice.getByRole(AriaRole.CHECKBOX).isChecked()) {
          choice.locator("label[slot=label]").click();

          assertThat(choice.getByRole(AriaRole.CHECKBOX)).not().isChecked();
        }
      }
      filterFieldLocator.getByText(option, new Locator.GetByTextOptions().setExact(true)).click();

      assertThat(
              filterFieldLocator.getByRole(
                  AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName(option).setExact(true)))
          .isChecked();
    }
  }

  @Step("Filter auswählen und ausdrücklich anwenden")
  public void selectFilter(String label, String option) {
    selectDraftFilter(label, option);
    applyFilters();
  }

  @Step("Filterentwürfe über Filter anwenden übernehmen")
  public void applyFilters() {
    UiActions.complete(browserPage, () -> button(uiTexts.getAdminUsersApplyFilters()).click());

    assertThat(browserPage.locator("#admin-user-filter-panel[opened]")).hasCount(0);
  }

  @Step("Eine Filtergruppe im Overlay zurücksetzen und anwenden")
  public void removeFilter(String label) {
    openFilters();
    if (label.equals(uiTexts.getAdminUsersGridEnabled())) {
      filterField(label)
          .getByText(
              uiTexts.getAdminUsersFilterAll(), new Locator.GetByTextOptions().setExact(true))
          .click();
    } else if (label.equals(uiTexts.getAdminUsersFilterActiveSince())) {
      activityDate().clear();
      activityDate().press("Tab");
    } else {
      for (Locator choice : filterField(label).locator("vaadin-checkbox").all()) {
        if (choice.getByRole(AriaRole.CHECKBOX).isChecked()) {
          choice.locator("label[slot=label]").click();

          assertThat(choice.getByRole(AriaRole.CHECKBOX)).not().isChecked();
        }
      }
    }
    applyFilters();
  }

  @Step("Aktivitätsfilter nur mit Datum setzen; Uhrzeit bleibt optional leer")
  public void activeSince(LocalDate date) {
    addActivityFilter();
    if (activityTime().isEnabled()) {
      activityTime().fill("");
    }
    activityDate().fill(uiTexts.dateInput(date));

    assertThat(activityDate()).hasValue(uiTexts.dateInput(date));
    // Vaadin may remove leading zeroes on blur. Verify the exact parsed date below.
    // Focus another real field to commit; do not open or cancel the calendar.
    searchField().click();

    assertThat(activityPicker()).hasJSProperty("value", date.toString());
    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    assertThat(activityTime()).hasValue("");
    applyFilters();
  }

  @Step("Aktivitätsdatum ohne Texteingabe über Kalender-Icon wählen und wiederfinden")
  public LocalDate activityDateUsingCalendar(int daysFromToday) {
    addActivityFilter();
    if (activityTime().isEnabled()) {
      activityTime().fill("");
    }
    activityDate().click();

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    // The public value property is read only; selection itself uses ordinary UI actions.
    Object value = activityPicker().evaluate("element => element.value");
    LocalDate base =
        value instanceof String current && !current.isEmpty()
            ? LocalDate.parse(current)
            : LocalDate.now(ZoneId.of("Europe/Berlin"));
    LocalDate selected = LocalDate.now(ZoneId.of("Europe/Berlin")).plusDays(daysFromToday);
    activityPicker().locator("[part~='toggle-button']").click();

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).isVisible();
    // Vaadin projects the calendar content into its overlay; its DOM remains with the picker.
    // Match the accessible calendar cell by its public date part, excluding user-grid cells.
    // Desktop opening marks the current date with tabindex=0, before keyboard focus.
    Locator currentDay =
        browserPage
            .getByRole(AriaRole.GRIDCELL)
            .and(browserPage.locator("[part~='date'][tabindex='0']"));

    assertThat(currentDay).hasCount(1);
    long delta = ChronoUnit.DAYS.between(base, selected);
    for (long step = 0; step < Math.abs(delta); step++) {
      currentDay.press(delta > 0 ? "ArrowRight" : "ArrowLeft");
    }
    // Am Monatswechsel bewegt sich das Gitter noch. Die bereits per Tastatur fokussierte
    // Zelle mit Enter bestätigen, statt während dieser Bewegung eine Mauskoordinate zu wählen.
    currentDay.press("Enter");

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    assertThat(activityPicker()).hasJSProperty("value", selected.toString());
    assertThat(activityTime()).hasValue("");
    activityPicker().locator("[part~='toggle-button']").click();
    Locator selectedDay =
        browserPage
            .getByRole(AriaRole.GRIDCELL)
            .and(browserPage.locator("[part~='date'][aria-selected='true']"));

    assertThat(selectedDay).hasCount(1);
    selectedDay.click();

    assertThat(browserPage.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
    assertThat(activityPicker()).hasJSProperty("value", selected.toString());
    applyFilters();
    return selected;
  }

  @Step("Optionale Uhrzeit über den angebotenen Dropdown auswählen")
  public void activityTimeUsingDropdown(LocalTime time) {
    activityTime().click();

    assertThat(browserPage.locator("vaadin-time-picker-overlay[opened]")).hasCount(0);
    browserPage
        .locator(".admin-user-activity-filter vaadin-time-picker")
        .locator("[part~='toggle-button']")
        .click();
    Locator timePickerOverlayLocator = browserPage.locator("vaadin-time-picker-overlay[opened]");

    assertThat(timePickerOverlayLocator).isVisible();
    Locator options =
        browserPage.getByRole(AriaRole.OPTION).and(browserPage.locator("vaadin-time-picker-item"));
    Locator timeOptionLocator =
        browserPage.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions().setName(uiTexts.timeInput(time)).setExact(true));
    // Die Liste hält unsichtbare Pufferzeilen vor. Das Mausrad muss über dem
    // sichtbaren Overlay liegen, nicht über dessen erstem DOM-Puffereintrag.
    BoundingBox bounds = timePickerOverlayLocator.locator("[part~='overlay']").boundingBox();
    browserPage.mouse().move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    Locator timePickerScrollerLocator = browserPage.locator("vaadin-time-picker-scroller:visible");
    for (int step = 0; step < 24 && timeOptionLocator.count() == 0; step++) {
      // Vaadin hält auch eine unsichtbare Mitternachtszeile im DOM. Sie ist kein
      // Maß für die Scrollrichtung: maßgeblich ist die erste tatsächlich sichtbare Zeile.
      BoundingBox visibleBounds = timePickerScrollerLocator.boundingBox();
      String firstVisible =
          (String)
              options.evaluateAll(
                  """
                  (items, viewport) => items.find(item => {
                    const rect = item.getBoundingClientRect();
                    return rect.height > 0 && rect.top >= viewport.top
                        && rect.bottom <= viewport.bottom;
                  })?.textContent.trim()
                  """,
                  Map.of("top", visibleBounds.y, "bottom", visibleBounds.y + visibleBounds.height));
      if (firstVisible == null) {
        throw new IllegalStateException("No visible time option in the open dropdown");
      }
      LocalTime firstTime =
          LocalTime.parse(
              firstVisible,
              DateTimeFormatter.ofPattern(
                  uiTexts.language().equals("de") ? "HH:mm" : "h:mm a",
                  Locale.forLanguageTag(uiTexts.language())));
      Object previousScroll = timePickerScrollerLocator.evaluate("element => element.scrollTop");
      browserPage.mouse().wheel(0, time.isBefore(firstTime) ? -240 : 240);

      assertThat(timePickerScrollerLocator).not().hasJSProperty("scrollTop", previousScroll);
    }
    assertThat(timeOptionLocator).hasCount(1);
    timeOptionLocator.click();

    assertThat(activityTime()).hasValue(uiTexts.timeInput(time));
    applyFilters();
  }

  @Step("Optionale Uhrzeit für den bestehenden Aktivitätsfilter ergänzen")
  public void activityTime(LocalTime time) {
    activityTime().fill(uiTexts.timeInput(time));
    activityTime().press("Tab");

    assertThat(activityTime()).hasValue(uiTexts.timeInput(time));
    applyFilters();
  }

  @Step("Nur die Uhrzeit leeren und den gewählten Tag beibehalten")
  public void clearActivityTime() {
    activityTime().fill("");
    activityTime().press("Tab");

    assertThat(activityTime()).hasValue("");
    applyFilters();
  }

  @Step("Suche, Entwürfe und aktive Badges über Alle zurücksetzen leeren")
  public void clearAllFilters() {
    openFilters();
    button(uiTexts.getAdminUsersClearFilters()).click();

    assertThat(searchField()).hasValue("");
    assertThat(browserPage.locator(".admin-user-active-filter-chip:visible")).hasCount(0);
    assertThat(activityTime()).isDisabled();
  }

  public Locator filterField(String label) {
    return browserPage
        .locator(".admin-user-filters")
        .getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
  }

  /** Grid texts live in light-DOM slot contents, not as descendants of their ARIA rows. */
  public Locator emailCells(String emailPart) {
    return grid()
        .locator("vaadin-grid-cell-content:visible")
        .filter(new Locator.FilterOptions().setHasText(emailPart));
  }

  private void addActivityFilter() {
    openFilters();

    assertThat(activityDate()).isVisible();
    assertThat(activityDate())
        .hasAttribute("placeholder", uiTexts.getRegistrationBirthDatePlaceholder());
    assertThat(browserPage.locator("#admin-user-filter-panel .admin-filter-hint"))
        .containsText(uiTexts.getAdminUsersFilterTimeHint());
  }

  private Locator activityPicker() {
    return browserPage.locator(".admin-user-activity-filter vaadin-date-picker");
  }

  public Locator activityDate() {
    openFilters();
    return browserPage
        .locator(".admin-user-activity-filter")
        .getByRole(
            AriaRole.COMBOBOX,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getAdminUsersActivityDate())
                .setExact(true));
  }

  public Locator activityTime() {
    openFilters();
    return browserPage
        .locator(".admin-user-activity-filter")
        .getByRole(
            AriaRole.COMBOBOX,
            new Locator.GetByRoleOptions()
                .setName(uiTexts.getAdminUsersActivityTimeOptional())
                .setExact(true));
  }

  private Locator button(String name) {
    return browserPage.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }
}
