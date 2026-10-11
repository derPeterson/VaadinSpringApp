package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.time.LocalDate;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Creates the ordinary test account through the real admin UI, without database access. */
public final class AdminUsersPage {
    private final Page page;
    private final UiTexts texts = UiTexts.selected();

    public AdminUsersPage(Page page) { this.page = page; }

    @Step("Aktives Testkonto ausschließlich mit normaler Benutzerrolle anlegen")
    public void createActiveUser(String email, String password) {
        page.locator("[slot='drawer']").getByText(texts.getAdminUsersTitle(), new Locator.GetByTextOptions().setExact(true)).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(texts.getAdminUsersAdd()).setExact(true)).click();
        Locator dialog = page.locator("vaadin-dialog-overlay[opened]");
        assertThat(dialog).isVisible();
        field(texts.getAdminUsersFieldFirstName()).fill("Browser");
        field(texts.getAdminUsersFieldLastName()).fill("Testuser");
        field(texts.getAdminUsersFieldEmail()).fill(email);
        field(texts.getAdminUsersFieldPassword()).fill(password);
        field(texts.getRegistrationGenderCombobox()).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(texts.getGenderOther()).setExact(true)).click();
        field(texts.getRegistrationBirthDateField()).fill(texts.dateInput(LocalDate.of(1990, 1, 1)));
        field(texts.getRegistrationBirthDateField()).press("Enter");
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).not().isVisible();
        assertThat(dialog).isVisible();
        check(texts.getAdminUsersFieldEnabled());
        check(texts.getRoleUser());
        assertThat(page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(texts.getRoleAdmin()).setExact(true))).not().isChecked();
        Locator save = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(texts.getAdminUsersSave()).setExact(true));
        assertThat(save).isEnabled();
        save.click();
        assertThat(dialog).not().isVisible();
        assertThat(page.getByText(email, new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    private Locator field(String label) {
        return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
    }

    private void check(String label) {
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(label).setExact(true)).check();
    }
}
