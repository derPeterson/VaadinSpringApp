package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Only the currently open create/edit form, including its native footer. */
public final class AdminUserDialog {
    private final Page page;
    private final UiTexts texts;

    public AdminUserDialog(Page page, UiTexts texts) { this.page = page; this.texts = texts; }

    public Locator root() { return page.locator("vaadin-dialog.admin-user-dialog[opened]"); }
    public Locator firstName() { return field(texts.getAdminUsersFieldFirstName()); }
    public Locator lastName() { return field(texts.getAdminUsersFieldLastName()); }
    public Locator email() { return field(texts.getAdminUsersFieldEmail()); }
    public Locator password() { return field(texts.getAdminUsersFieldPassword()); }
    public Locator saveButton() { return button(texts.getAdminUsersSave()); }
    public Locator deleteButton() { return button(texts.getAdminUsersDelete()); }

    @Step("Gültige Stammdaten und normale Benutzerrolle im echten Formular eingeben")
    public void fillNewUser(String email, String password, boolean enabled, boolean administrator) {
        firstName().fill("Browser");
        lastName().fill("Testuser");
        this.email().fill(email);
        this.password().fill(password);
        field(texts.getRegistrationGenderCombobox()).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(texts.getGenderOther()).setExact(true)).click();
        field(texts.getRegistrationBirthDateField()).fill(texts.dateInput(LocalDate.of(1990, 1, 1)));
        field(texts.getRegistrationBirthDateField()).press("Enter");
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).not().isVisible();
        check(texts.getAdminUsersFieldEnabled(), enabled);
        check(texts.getRoleUser(), true);
        check(texts.getRoleAdmin(), administrator);
    }

    @Step("Benutzerformular speichern und dessen tatsächliches Schließen abwarten")
    public void save() {
        assertThat(saveButton()).isEnabled();
        saveButton().click();
        assertThat(root()).hasCount(0);
    }

    @Step("Benutzerformular ohne Speicherung abbrechen")
    public void cancel() {
        button(texts.getAdminUsersCancel()).click();
        assertThat(root()).hasCount(0);
    }

    @Step("Löschbestätigung aus dem Bearbeitungsformular öffnen")
    public AdminUserDeleteDialog requestDelete() {
        assertThat(deleteButton()).isEnabled();
        deleteButton().click();
        AdminUserDeleteDialog confirmation = new AdminUserDeleteDialog(page, texts);
        assertThat(confirmation.root()).hasCount(1);
        assertThat(confirmation.root().getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions()
                .setName(texts.getAdminUsersDeleteDialogTitle()).setExact(true))).isVisible();
        return confirmation;
    }

    private Locator field(String label) { return root().getByLabel(label, new Locator.GetByLabelOptions().setExact(true)); }
    private Locator button(String name) { return root().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true)); }
    private void check(String label, boolean checked) {
        root().getByRole(AriaRole.CHECKBOX, new Locator.GetByRoleOptions().setName(label).setExact(true)).setChecked(checked);
    }
}
