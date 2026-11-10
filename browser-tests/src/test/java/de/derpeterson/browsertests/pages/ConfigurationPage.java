package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.util.regex.Pattern;

/** Configuration through visible groups and inputs; no application API access. */
public final class ConfigurationPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public ConfigurationPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Gewählte Konfigurationsgruppe über das Adminmenü öffnen")
  public void open(AdminConfigGroup group) {
    new AdminNavigation(browserPage).openGroup(group);
    assertThat(button(uiTexts.getAdminConfigSave())).isVisible();
  }

  public Locator input(String label) {
    return browserPage
        .getByLabel(label, new Page.GetByLabelOptions().setExact(true))
        .and(browserPage.locator("input"));
  }

  public Locator info(String label) {
    return browserPage
        .getByLabel(label, new Page.GetByLabelOptions().setExact(true))
        .and(browserPage.locator("vaadin-button.cosmos-config-info"));
  }

  public Locator helpPanel() {
    return browserPage.locator("#admin-config-help[opened]");
  }

  public static void assertEffectiveUse(Locator help, UiTexts uiTexts, String... notes) {
    Locator section = help.locator(".admin-help-effective-use");
    assertThat(section.getByRole(AriaRole.HEADING)).hasText(uiTexts.getAdminConfigEffectiveTitle());
    Locator items = section.locator("li");
    if (notes.length == 0) {
      assertThat(items.first()).hasText(Pattern.compile("\\S"));
      assertThat(section).not().containsText("adminView.config.effective_");
    } else {
      assertThat(items).hasText(notes);
    }
  }

  @Step("Feldinformation per Klick öffnen und die tatsächlichen Regeln prüfen")
  public void explain(String label, String description, String... rules) {
    info(label).click();
    assertThat(helpPanel().locator("vaadin-dialog-overlay > [part~='overlay']")).isVisible();
    assertThat(helpPanel()).containsText(description);
    for (String rule : rules) {
      assertThat(helpPanel()).containsText(rule);
    }
    assertThat(helpPanel()).containsText(uiTexts.getAdminConfigInputRules());
    closeHelp();
    assertThat(helpPanel()).hasCount(0);
  }

  public void closeHelp() {
    closeHelp(uiTexts.getNotificationsClose());
  }

  public void closeHelp(String label) {
    helpPanel()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(label).setExact(true))
        .last()
        .click();
    assertThat(helpPanel()).hasCount(0);
  }

  public Locator duration(String label) {
    return browserPage
        .locator(".admin-duration-field")
        .filter(new Locator.FilterOptions().setHas(input(label)));
  }

  public java.math.BigDecimal seconds(String label) {
    Locator field = duration(label);
    String selected = field.locator("vaadin-select-value-button").innerText().trim();
    int factor;
    if (selected.equals(uiTexts.getAdminConfigDurationUnitDays())) {
      factor = 86400;
    } else if (selected.equals(uiTexts.getAdminConfigDurationUnitHours())) {
      factor = 3600;
    } else if (selected.equals(uiTexts.getAdminConfigDurationUnitMinutes())) {
      factor = 60;
    } else {
      assertThat(field.locator("vaadin-select-value-button"))
          .hasText(uiTexts.getAdminConfigDurationUnitSeconds());
      factor = 1;
    }
    return new java.math.BigDecimal(input(label).inputValue().replace(",", "."))
        .multiply(java.math.BigDecimal.valueOf(factor));
  }

  public void enterSeconds(String label, java.math.BigDecimal value) {
    duration(label).locator("vaadin-select").click();
    browserPage
        .getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminConfigDurationUnitSeconds())
                .setExact(true))
        .click();
    String number = value.toPlainString();
    input(label).fill(uiTexts.language().equals("de") ? number.replace('.', ',') : number);
    input(label).press("Tab");
  }

  public String value(String label) {
    return input(label).inputValue();
  }

  @Step("Konfigurationswert eingeben")
  public void enter(String label, String value) {
    input(label).fill(value);
    input(label).press("Tab");
  }

  @Step("Konfiguration speichern und Erfolgshinweis schließen")
  public void save() {
    submit();
    notice(uiTexts.getAdminConfigSaveSuccess());
  }

  public void submit() {
    button(uiTexts.getAdminConfigSave()).click();
  }

  @Step("Gespeicherte Konfiguration erneut laden")
  public void reload() {
    button(uiTexts.getAdminConfigReload()).click();
    notice(uiTexts.getAdminConfigReloadSuccess());
  }

  @Step("Ungültiges Feld und verständlichen Hinweis prüfen")
  public void reject(String component, String message) {
    button(uiTexts.getAdminConfigSave()).click();
    assertThat(browserPage.locator(component + "[invalid]")).hasCount(1);
    assertThat(browserPage.locator(component + "[invalid]")).hasJSProperty("errorMessage", message);
    notice(uiTexts.getAdminConfigValidationInvalidFields());
  }

  private void notice(String expected) {
    Locator notice =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(expected));
    assertThat(notice).isVisible();
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }

  private Locator button(String name) {
    return browserPage.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }
}
