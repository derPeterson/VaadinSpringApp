package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Separate confirmation; opening it never substitutes for confirming a deletion. */
public final class AdminUserDeleteDialog {
    private final Page page;
    private final UiTexts texts;
    public AdminUserDeleteDialog(Page page, UiTexts texts) { this.page = page; this.texts = texts; }
    public Locator root() { return page.locator("vaadin-dialog.admin-user-delete-dialog[opened]"); }

    @Step("Löschbestätigung abbrechen; Konto und Bearbeitungsformular erhalten")
    public void cancel() {
        button(texts.getAdminUsersCancel()).click();
        assertThat(root()).hasCount(0);
    }

    @Step("Löschung ausschließlich des eigenen wegwerfbaren Testkontos bestätigen")
    public void confirm() {
        button(texts.getAdminUsersDelete()).click();
        assertThat(root()).hasCount(0);
    }

    private Locator button(String name) { return root().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true)); }
}
