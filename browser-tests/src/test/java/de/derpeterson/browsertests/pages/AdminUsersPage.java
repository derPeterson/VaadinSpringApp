package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import java.util.regex.Pattern;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Creates the ordinary test account through the real admin UI, without database access. */
public final class AdminUsersPage {
    private final Page page;

    public AdminUsersPage(Page page) { this.page = page; }

    @Step("Aktives Testkonto ausschließlich mit normaler Benutzerrolle anlegen")
    public void createActiveUser(String email, String password) {
        page.locator("[slot='drawer']").getByText(Pattern.compile("^(Benutzerverwaltung|User Management)$")).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Neuen Benutzer anlegen|Create new user)$"))).click();
        Locator dialog = page.locator("vaadin-dialog-overlay[opened]");
        assertThat(dialog).isVisible();
        page.getByLabel(Pattern.compile("^(Vorname|First name)$")).fill("Browser");
        page.getByLabel(Pattern.compile("^(Nachname|Last name)$")).fill("Testuser");
        page.getByLabel(Pattern.compile("^(E-Mail|Email)$")).fill(email);
        page.getByLabel(Pattern.compile("^(Passwort|Password)$")).fill(password);
        page.getByLabel(Pattern.compile("^(Geschlecht|Gender)$")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(Pattern.compile("^(Divers|Other)$"))).click();
        page.getByLabel(Pattern.compile("^(Geburtsdatum|Birth Date)$")).fill("1/1/1990");
        page.getByLabel(Pattern.compile("^(Geburtsdatum|Birth Date)$")).press("Enter");
        assertThat(page.locator("vaadin-date-picker-overlay[opened]")).not().isVisible();
        assertThat(dialog).isVisible();
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Aktiv|Enabled)$"))).check();
        page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions()
                .setName(Pattern.compile("^(Benutzer|User)$"))).check();
        assertThat(page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Administrator").setExact(true))).not().isChecked();
        Locator save = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("^(Speichern|Save)$")));
        assertThat(save).isEnabled();
        save.click();
        assertThat(dialog).not().isVisible();
        assertThat(page.getByText(email, new Page.GetByTextOptions().setExact(true))).isVisible();
    }
}
