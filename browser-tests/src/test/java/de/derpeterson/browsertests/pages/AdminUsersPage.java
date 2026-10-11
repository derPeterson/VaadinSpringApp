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
        page.locator("[slot='drawer']").getByText(texts.text("admin.users.title"), new Locator.GetByTextOptions().setExact(true)).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(texts.text("admin.users.add")).setExact(true)).click();
        Locator dialog = page.locator("vaadin-dialog-overlay[opened]");
        assertThat(dialog).isVisible();
        field("admin.users.field.first_name").fill("Browser");
        field("admin.users.field.last_name").fill("Testuser");
        field("admin.users.field.email").fill(email);
        field("admin.users.field.password").fill(password);
        field("registrationView.gender_combobox").click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(texts.text("gender.other")).setExact(true)).click();
        field("registrationView.birth_date_field").fill(texts.dateInput(LocalDate.of(1990, 1, 1)));
        field("registrationView.birth_date_field").press("Enter");
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).not().isVisible();
        assertThat(dialog).isVisible();
        check("admin.users.field.enabled");
        check("role.user");
        assertThat(page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(texts.text("role.admin")).setExact(true))).not().isChecked();
        Locator save = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(texts.text("admin.users.save")).setExact(true));
        assertThat(save).isEnabled();
        save.click();
        assertThat(dialog).not().isVisible();
        assertThat(page.getByText(email, new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    private Locator field(String key) {
        return page.getByLabel(texts.text(key), new Page.GetByLabelOptions().setExact(true));
    }

    private void check(String key) {
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(texts.text(key)).setExact(true)).check();
    }
}
