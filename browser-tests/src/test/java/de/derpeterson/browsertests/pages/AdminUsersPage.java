package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** User list and optional filters; form/confirmation actions have their own small helpers. */
public final class AdminUsersPage {
    private final Page page;
    private final UiTexts texts;

    public AdminUsersPage(Page page) { this(page, UiTexts.selected()); }
    public AdminUsersPage(Page page, UiTexts texts) { this.page = page; this.texts = texts; }
    public Locator grid() { return page.locator(".admin-users-grid"); }

    @Step("Aktives Testkonto ausschließlich mit normaler Benutzerrolle anlegen")
    public void createActiveUser(String email, String password) { createUser(email, password, true); }

    public void createUser(String email, String password, boolean enabled) { createUser(email, password, enabled, false); }

    @Step("Testkonto mit gewählter Freigabe und Rollen über das echte Formular anlegen")
    public void createUser(String email, String password, boolean enabled, boolean administrator) {
        open();
        AdminUserDialog form = openCreate();
        form.fillNewUser(email, password, enabled, administrator);
        form.save();
        search(email);
        assertThat(emailCells(email)).hasCount(1);
        assertThat(emailCells(email)).hasText(email);
    }

    @Step("Benutzerverwaltung öffnen")
    public void open() {
        page.locator("[slot='drawer']").getByText(texts.getAdminUsersTitle(), new Locator.GetByTextOptions().setExact(true)).click();
        assertThat(grid()).isVisible();
    }

    @Step("Neues Benutzerformular öffnen")
    public AdminUserDialog openCreate() {
        button(texts.getAdminUsersAdd()).click();
        AdminUserDialog form = new AdminUserDialog(page, texts);
        // Vaadin's host has no visible box; assert real rendered fields instead.
        assertThat(form.root()).hasCount(1);
        assertThat(form.firstName()).isVisible();
        return form;
    }

    @Step("Genau das eigene Konto per E-Mail suchen und dessen sichtbaren Editierbutton öffnen")
    public AdminUserDialog openEdit(String email) {
        search(email);
        assertThat(emailCells(email)).hasCount(1);
        assertThat(emailCells(email)).hasText(email);
        Locator edit = grid().locator("vaadin-button:visible").filter(new Locator.FilterOptions()
                .setHas(page.locator("vaadin-icon[icon='vaadin:edit']")));
        // No first-row assumption: filtering must yield one visible account and action.
        assertThat(edit).hasCount(1);
        // Finish the grid's native focus/scroll movement before the mouse click.
        // Otherwise focus on mousedown can move the action before mouseup arrives.
        edit.scrollIntoViewIfNeeded();
        edit.focus();
        edit.hover();
        edit.click();
        AdminUserDialog form = new AdminUserDialog(page, texts);
        // Vaadin's host has no visible box; assert real rendered fields instead.
        assertThat(form.root()).hasCount(1);
        assertThat(form.firstName()).isVisible();
        assertThat(form.email()).hasValue(email);
        return form;
    }

    @Step("Benutzer über Name oder E-Mail suchen")
    public void search(String query) {
        searchField().fill(query);
        searchField().press("Tab");
    }

    public Locator searchField() { return page.getByLabel(texts.getAdminUsersSearchLabel(), new Page.GetByLabelOptions().setExact(true)); }

    @Step("Benötigten Filter über Add Filter einblenden")
    public void addFilter(String label) {
        if (!filterField(label).isVisible()) {
            page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(texts.getAdminUsersAddFilter()).setExact(true)).click();
            page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(label).setExact(true)).click();
        }
        assertThat(filterField(label)).isVisible();
    }

    @Step("Optionalen Dropdownfilter auswählen")
    public void selectFilter(String label, String option) {
        addFilter(label);
        filterField(label).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(option).setExact(true)).click();
        assertThat(filterField(label)).hasValue(option);
    }

    @Step("Filter mit seinem beschrifteten Entfernen-Button zurücksetzen und ausblenden")
    public void removeFilter(String label) {
        button(texts.getAdminUsersRemoveFilter(label)).click();
        assertThat(filterField(label)).not().isVisible();
    }

    @Step("Aktivitätsfilter nur mit Datum setzen; Uhrzeit bleibt optional leer")
    public void activeSince(LocalDate date) {
        addActivityFilter();
        activityTime().fill("");
        activityDate().fill(texts.dateInput(date));
        assertThat(activityDate()).hasValue(texts.dateInput(date));
        // Vaadin may remove leading zeroes on blur. Verify the exact parsed date below.
        // Focus another real field to commit; do not open or cancel the calendar.
        searchField().click();
        assertThat(activityPicker()).hasJSProperty("value", date.toString());
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
        assertThat(activityTime()).hasValue("");
    }

    @Step("Aktivitätsdatum ohne Texteingabe über Kalender-Icon wählen und wiederfinden")
    public LocalDate activityDateUsingCalendar(int daysFromToday) {
        addActivityFilter();
        activityTime().fill("");
        activityDate().click();
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
        // The public value property is read only; selection itself uses ordinary UI actions.
        Object value = activityPicker().evaluate("element => element.value");
        LocalDate base = value instanceof String current && !current.isEmpty()
                ? LocalDate.parse(current) : LocalDate.now(ZoneId.of("Europe/Berlin"));
        LocalDate selected = LocalDate.now(ZoneId.of("Europe/Berlin")).plusDays(daysFromToday);
        activityPicker().locator("[part~='toggle-button']").click();
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).isVisible();
        // Vaadin projects the calendar content into its overlay; its DOM remains with the picker.
        // Match the accessible calendar cell by its public date part, excluding user-grid cells.
        // Desktop opening marks the current date with tabindex=0, before keyboard focus.
        Locator currentDay = page.getByRole(AriaRole.GRIDCELL)
                .and(page.locator("[part~='date'][tabindex='0']"));
        assertThat(currentDay).hasCount(1);
        long delta = java.time.temporal.ChronoUnit.DAYS.between(base, selected);
        for (long step = 0; step < Math.abs(delta); step++) currentDay.press(delta > 0 ? "ArrowRight" : "ArrowLeft");
        currentDay.click();
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
        assertThat(activityPicker()).hasJSProperty("value", selected.toString());
        assertThat(activityTime()).hasValue("");
        activityPicker().locator("[part~='toggle-button']").click();
        Locator selectedDay = page.getByRole(AriaRole.GRIDCELL).and(page.locator("[part~='date'][aria-selected='true']"));
        assertThat(selectedDay).hasCount(1);
        selectedDay.click();
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
        assertThat(activityPicker()).hasJSProperty("value", selected.toString());
        return selected;
    }

    @Step("Optionale Uhrzeit über den angebotenen Dropdown auswählen")
    public void activityTimeUsingDropdown(LocalTime time) {
        activityTime().click();
        assertThat(page.locator("vaadin-time-picker-overlay[opened]")).hasCount(0);
        page.locator(".admin-user-activity-filter vaadin-time-picker")
                .locator("[part~='toggle-button']").click();
        Locator overlay = page.locator("vaadin-time-picker-overlay[opened]");
        assertThat(overlay).isVisible();
        Locator options = page.getByRole(AriaRole.OPTION).and(page.locator("vaadin-time-picker-item"));
        Locator target = page.getByRole(AriaRole.OPTION,
                new Page.GetByRoleOptions().setName(texts.timeInput(time)).setExact(true));
        // The time list is virtualized: later options appear only after real scrolling.
        // Twelve wheel movements cover a full day; each waits for the rendered items to advance.
        // Hover a real list item: the transparent overlay host is not a pointer target.
        options.first().hover();
        for (int step = 0; step < 12 && target.count() == 0; step++) {
            String previousFirst = options.first().textContent();
            page.mouse().wheel(0, 400);
            assertThat(options.first()).not().hasText(previousFirst);
        }
        assertThat(target).hasCount(1);
        target.click();
        assertThat(activityTime()).hasValue(texts.timeInput(time));
    }

    @Step("Optionale Uhrzeit für den bestehenden Aktivitätsfilter ergänzen")
    public void activityTime(LocalTime time) {
        activityTime().fill(texts.timeInput(time));
        activityTime().press("Tab");
        assertThat(activityTime()).hasValue(texts.timeInput(time));
    }

    @Step("Nur die Uhrzeit leeren und den gewählten Tag beibehalten")
    public void clearActivityTime() {
        activityTime().fill("");
        activityTime().press("Tab");
        assertThat(activityTime()).hasValue("");
    }

    @Step("Suche und alle Zusatzfilter über Clear Filter zurücksetzen")
    public void clearAllFilters() {
        button(texts.getAdminUsersClearFilters()).click();
        assertThat(searchField()).hasValue("");
        assertThat(page.locator(".admin-user-filter-control:visible")).hasCount(0);
    }

    public Locator filterField(String label) {
        return page.locator(".admin-user-filters").getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
    }

    /** Grid texts live in light-DOM slot contents, not as descendants of their ARIA rows. */
    public Locator emailCells(String emailPart) {
        return grid().locator("vaadin-grid-cell-content:visible").filter(new Locator.FilterOptions().setHasText(emailPart));
    }

    private void addActivityFilter() {
        if (!activityDate().isVisible()) {
            page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(texts.getAdminUsersAddFilter()).setExact(true)).click();
            page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(texts.getAdminUsersFilterActiveSince()).setExact(true)).click();
        }
        assertThat(activityDate()).isVisible();
        assertThat(activityDate()).hasAttribute("placeholder", texts.getDateInputPlaceholder());
        assertThat(activityTime()).hasAttribute("placeholder", texts.getTimeInputPlaceholder());
        assertThat(page.locator(".admin-user-activity-optional-note"))
                .hasText(texts.getAdminUsersActivityTimeOptional());
    }

    private Locator activityPicker() { return page.locator(".admin-user-activity-filter vaadin-date-picker"); }

    private Locator activityDate() {
        return page.locator(".admin-user-activity-filter").getByRole(AriaRole.COMBOBOX,
                new Locator.GetByRoleOptions().setName(texts.getAdminUsersActivityDate()).setExact(true));
    }
    public Locator activityTime() {
        return page.locator(".admin-user-activity-filter").getByRole(AriaRole.COMBOBOX,
                new Locator.GetByRoleOptions().setName(texts.getAdminUsersActivityTimeOptional()).setExact(true));
    }
    private Locator button(String name) { return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true)); }
}
