package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** Configuration through visible groups and inputs; no application API access. */
public final class ConfigurationPage {
  private final Page page;
  private final UiTexts texts = UiTexts.selected();

  public ConfigurationPage(Page page) {
    this.page = page;
  }

  @Step("Gewählte Konfigurationsgruppe über das Adminmenü öffnen")
  public void open(AdminConfigGroup group) {
    new AdminNavigation(page).openGroup(group);
    assertThat(button(texts.getAdminConfigSave())).isVisible();
  }

  public Locator input(String label) {
    return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true))
        .and(page.locator("input"));
  }

  public Locator info(String label) {
    return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true))
        .and(page.locator("vaadin-button.cosmos-config-info"));
  }

  public Locator helpPanel() {
    return page.locator("#admin-config-help[opened]");
  }

  @Step("Feldinformation per Klick öffnen und die tatsächlichen Regeln prüfen")
  public void explain(String label, String description, String... rules) {
    info(label).click();
    assertThat(helpPanel().locator("vaadin-dialog-overlay > [part~='overlay']")).isVisible();
    assertThat(helpPanel()).containsText(description);
    for (String rule : rules) {
      assertThat(helpPanel()).containsText(rule);
    }
    assertThat(helpPanel()).containsText(texts.getAdminConfigInputRules());
    closeHelp();
    assertThat(helpPanel()).hasCount(0);
  }

  public void closeHelp() {
    closeHelp(texts.getNotificationsClose());
  }

  public void closeHelp(String label) {
    helpPanel()
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(label).setExact(true))
        .last()
        .click();
    assertThat(helpPanel()).hasCount(0);
  }

  public Locator duration(String label) {
    return page.locator(".admin-duration-field")
        .filter(new Locator.FilterOptions().setHas(input(label)));
  }

  public java.math.BigDecimal seconds(String label) {
    Locator field = duration(label);
    String selected = field.locator("vaadin-select-value-button").innerText().trim();
    int factor;
    if (selected.equals(texts.getAdminConfigDurationUnitDays())) {
      factor = 86400;
    } else if (selected.equals(texts.getAdminConfigDurationUnitHours())) {
      factor = 3600;
    } else if (selected.equals(texts.getAdminConfigDurationUnitMinutes())) {
      factor = 60;
    } else {
      assertThat(field.locator("vaadin-select-value-button"))
          .hasText(texts.getAdminConfigDurationUnitSeconds());
      factor = 1;
    }
    return new java.math.BigDecimal(input(label).inputValue().replace(",", "."))
        .multiply(java.math.BigDecimal.valueOf(factor));
  }

  public void enterSeconds(String label, java.math.BigDecimal value) {
    duration(label).locator("vaadin-select").click();
    page.getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(texts.getAdminConfigDurationUnitSeconds())
                .setExact(true))
        .click();
    String number = value.toPlainString();
    input(label).fill(texts.language().equals("de") ? number.replace('.', ',') : number);
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
    notice(texts.getAdminConfigSaveSuccess());
  }

  public void submit() {
    button(texts.getAdminConfigSave()).click();
  }

  @Step("Gespeicherte Konfiguration erneut laden")
  public void reload() {
    button(texts.getAdminConfigReload()).click();
    notice(texts.getAdminConfigReloadSuccess());
  }

  @Step("Ungültiges Feld und verständlichen Hinweis prüfen")
  public void reject(String component, String message) {
    button(texts.getAdminConfigSave()).click();
    assertThat(page.locator(component + "[invalid]")).hasCount(1);
    assertThat(page.locator(component + "[invalid]")).hasJSProperty("errorMessage", message);
    notice(texts.getAdminConfigValidationInvalidFields());
  }

  private void notice(String expected) {
    Locator notice =
        page.locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(expected));
    assertThat(notice).isVisible();
    notice.getByRole(AriaRole.BUTTON).click();
    assertThat(notice).not().isVisible();
  }

  private Locator button(String name) {
    return page.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }
}
