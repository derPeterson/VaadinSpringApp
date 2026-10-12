package de.derpeterson.browsertests.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The four visible token settings; no database or service access. */
public final class TokenConfigurationPage {
    private final Page page;
    private final UiTexts texts = UiTexts.selected();
    public TokenConfigurationPage(Page page) {this.page = page;}

    @Step("Token-Laufzeiten über die Adminnavigation öffnen")
    public void open() {
        page.locator("[slot='drawer']").getByText(texts.getAdminConfigGroupTokens(), new Locator.GetByTextOptions().setExact(true)).click();
        assertThat(button(texts.getAdminConfigSave())).isVisible();
    }

    private Locator wrapper(String label) {return field(label).locator("..");}
    public Locator field(String label) {
        return page.locator("vaadin-text-field").filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(label).setExact(true))));
    }
    public String value(String label) {return field(label).locator("input").inputValue();}

    @Step("Dauer im sichtbaren Feld eingeben")
    public void enter(String label, String value) {field(label).locator("input").fill(value);field(label).locator("input").press("Tab");}

    @Step("Dauer ablehnen und lokalisierten Feldfehler prüfen")
    public void reject(String label, String error) {
        button(texts.getAdminConfigSave()).click();
        assertThat(field(label)).hasAttribute("invalid", "");
        assertThat(field(label)).hasJSProperty("errorMessage", error);
        assertThat(notice()).containsText(texts.getAdminConfigValidationInvalidFields());
        dismiss();
    }
    @Step("Konfiguration speichern")
    public void save() {button(texts.getAdminConfigSave()).click();assertThat(notice()).containsText(texts.getAdminConfigSaveSuccess());dismiss();}
    @Step("Gespeicherte Konfiguration erneut laden")
    public void reload() {button(texts.getAdminConfigReload()).click();assertThat(notice()).containsText(texts.getAdminConfigReloadSuccess());dismiss();}

    @Step("Sichtbare Mindestdauer und zugehörigen Erklärungsdialog prüfen")
    public void explain(String label, long minutes, String explanation) {
        assertThat(wrapper(label)).containsText(texts.getAdminConfigHelperTokenDurationFormats(minutes));
        wrapper(label).locator("vaadin-icon[icon='vaadin:info-circle']").click();
        assertThat(page.locator("vaadin-dialog-overlay[opened]")).isVisible();
        // Vaadin's slotted dialog content is not a DOM descendant of the overlay.
        assertThat(page.getByText(explanation, new Page.GetByTextOptions().setExact(true))).isVisible();
        button(texts.getOkayHomeButton()).click();
        assertThat(page.locator("vaadin-dialog-overlay[opened]")).not().isVisible();
    }
    private Locator notice() {return page.locator("vaadin-notification-card:not([closing])");}
    private void dismiss() {notice().getByRole(AriaRole.BUTTON).click();assertThat(notice()).not().isVisible();}
    private Locator button(String name) {return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));}
}
