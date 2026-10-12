package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import java.time.LocalTime;
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
        activityDate().press("Tab");
        assertThat(activityTime()).hasValue("");
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
    }

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
