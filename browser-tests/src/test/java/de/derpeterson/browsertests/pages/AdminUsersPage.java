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
        new AdminNavigation(page, texts).openSection(texts.getAdminUsersTitle());
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
        open();
        // Login/logout and other users may have changed the stored row version.
        // Use the offered refresh action before opening a new snapshot; open forms remain untouched.
        button(texts.getAdminUsersRefreshTooltip()).click();
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

    @Step("Filterpanel bei Bedarf öffnen; alle Filter bleiben dauerhaft vorhanden")
    public void openFilters() {
        Locator toggle = button(texts.getAdminUsersAddFilter());
        if (!"true".equals(toggle.getAttribute("aria-expanded"))) toggle.click();
        assertThat(toggle).hasAttribute("aria-expanded", "true");
        assertThat(button(texts.getAdminUsersApplyFilters())).isVisible();
    }

    @Step("Filterentwurf auswählen, ohne ihn bereits anzuwenden")
    public void selectDraftFilter(String label, String option) {
        openFilters();
        filterField(label).click();
        UiActions.completeValueChange(page, () -> page.getByRole(AriaRole.OPTION,
                new Page.GetByRoleOptions().setName(option).setExact(true)).click());
        assertThat(filterField(label)).hasValue(option);
    }

    @Step("Dropdownfilter auswählen und ausdrücklich anwenden")
    public void selectFilter(String label, String option) {
        selectDraftFilter(label, option);
        applyFilters();
    }

    @Step("Filterentwürfe über Filter anwenden übernehmen")
    public void applyFilters() {
        button(texts.getAdminUsersApplyFilters()).click();
    }

    @Step("Nur den ausgewählten angewandten Filter über seinen Badge entfernen")
    public void removeFilter(String label) {
        button(texts.getAdminUsersRemoveFilter(label)).click();
        assertThat(page.locator(".admin-user-active-filter-chip:visible").filter(new Locator.FilterOptions()
                .setHasText(label + ":"))).hasCount(0);
    }

    @Step("Aktivitätsfilter nur mit Datum setzen; Uhrzeit bleibt optional leer")
    public void activeSince(LocalDate date) {
        addActivityFilter();
        if (activityTime().isEnabled()) activityTime().fill("");
        activityDate().fill(texts.dateInput(date));
        assertThat(activityDate()).hasValue(texts.dateInput(date));
        // Vaadin may remove leading zeroes on blur. Verify the exact parsed date below.
        // Focus another real field to commit; do not open or cancel the calendar.
        searchField().click();
        assertThat(activityPicker()).hasJSProperty("value", date.toString());
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).hasCount(0);
        assertThat(activityTime()).hasValue("");
        applyFilters();
    }

    @Step("Aktivitätsdatum ohne Texteingabe über Kalender-Icon wählen und wiederfinden")
    public LocalDate activityDateUsingCalendar(int daysFromToday) {
        addActivityFilter();
        if (activityTime().isEnabled()) activityTime().fill("");
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
        applyFilters();
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
        // Die Liste hält unsichtbare Pufferzeilen vor. Das Mausrad muss über dem
        // sichtbaren Overlay liegen, nicht über dessen erstem DOM-Puffereintrag.
        var bounds = overlay.locator("[part~='overlay']").boundingBox();
        page.mouse().move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
        Locator scroller = page.locator("vaadin-time-picker-scroller:visible");
        for (int step = 0; step < 24 && target.count() == 0; step++) {
            LocalTime firstTime = LocalTime.parse(options.first().innerText().trim(),
                    java.time.format.DateTimeFormatter.ofPattern(texts.language().equals("de") ? "HH:mm" : "h:mm a",
                            java.util.Locale.forLanguageTag(texts.language())));
            Object previousScroll = scroller.evaluate("element => element.scrollTop");
            page.mouse().wheel(0, time.isBefore(firstTime) ? -240 : 240);
            assertThat(scroller).not().hasJSProperty("scrollTop", previousScroll);
        }
        assertThat(target).hasCount(1);
        target.click();
        assertThat(activityTime()).hasValue(texts.timeInput(time));
        applyFilters();
    }

    @Step("Optionale Uhrzeit für den bestehenden Aktivitätsfilter ergänzen")
    public void activityTime(LocalTime time) {
        activityTime().fill(texts.timeInput(time));
        activityTime().press("Tab");
        assertThat(activityTime()).hasValue(texts.timeInput(time));
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
        button(texts.getAdminUsersClearFilters()).click();
        assertThat(searchField()).hasValue("");
        assertThat(page.locator(".admin-user-active-filter-chip:visible")).hasCount(0);
        assertThat(activityTime()).isDisabled();
    }

    public Locator filterField(String label) {
        return page.locator(".admin-user-filters").getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
    }

    /** Grid texts live in light-DOM slot contents, not as descendants of their ARIA rows. */
    public Locator emailCells(String emailPart) {
        return grid().locator("vaadin-grid-cell-content:visible").filter(new Locator.FilterOptions().setHasText(emailPart));
    }

    private void addActivityFilter() {
        openFilters();
        assertThat(activityDate()).isVisible();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
        String example = today.format(java.time.format.DateTimeFormatter.ofPattern(
                texts.language().equals("de") ? "dd.MM.uuuu" : "MM/dd/uuuu"));
        assertThat(activityDate()).hasAttribute("placeholder", example);
        assertThat(page.locator(".admin-user-filter-footer")).containsText(texts.getAdminUsersFilterTimeHint());
    }

    private Locator activityPicker() { return page.locator(".admin-user-activity-filter vaadin-date-picker"); }

    public Locator activityDate() {
        return page.locator(".admin-user-activity-filter").getByRole(AriaRole.COMBOBOX,
                new Locator.GetByRoleOptions().setName(texts.getAdminUsersActivityDate()).setExact(true));
    }
    public Locator activityTime() {
        return page.locator(".admin-user-activity-filter").getByRole(AriaRole.COMBOBOX,
                new Locator.GetByRoleOptions().setName(texts.getAdminUsersActivityTimeOptional()).setExact(true));
    }
    private Locator button(String name) { return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true)); }
}
