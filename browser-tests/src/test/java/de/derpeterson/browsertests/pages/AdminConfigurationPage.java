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
public final class AdminConfigurationPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public AdminConfigurationPage(Page browserPage) {
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

  public static void assertEffectiveUse(
      Locator helpPanelLocator, UiTexts uiTexts, String... expectedEffectiveUseNotes) {
    Locator effectiveUseSectionLocator = helpPanelLocator.locator(".admin-help-effective-use");

    assertThat(effectiveUseSectionLocator.getByRole(AriaRole.HEADING))
        .hasText(uiTexts.getAdminConfigEffectiveTitle());
    Locator effectiveUseItemLocators = effectiveUseSectionLocator.locator("li");
    if (expectedEffectiveUseNotes.length == 0) {
      assertThat(effectiveUseItemLocators.first()).hasText(Pattern.compile("\\S"));
      assertThat(effectiveUseSectionLocator).not().containsText("adminView.config.effective_");
    } else {
      assertThat(effectiveUseItemLocators).hasText(expectedEffectiveUseNotes);
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

  public java.math.BigDecimal seconds(String durationFieldLabel) {
    Locator durationFieldLocator = duration(durationFieldLabel);
    String selectedDurationUnitText =
        durationFieldLocator.locator("vaadin-select-value-button").innerText().trim();
    int secondsPerUnit;
    if (selectedDurationUnitText.equals(uiTexts.getAdminConfigDurationUnitDays())) {
      secondsPerUnit = 86400;
    } else if (selectedDurationUnitText.equals(uiTexts.getAdminConfigDurationUnitHours())) {
      secondsPerUnit = 3600;
    } else if (selectedDurationUnitText.equals(uiTexts.getAdminConfigDurationUnitMinutes())) {
      secondsPerUnit = 60;
    } else {
      assertThat(durationFieldLocator.locator("vaadin-select-value-button"))
          .hasText(uiTexts.getAdminConfigDurationUnitSeconds());
      secondsPerUnit = 1;
    }
    return new java.math.BigDecimal(input(durationFieldLabel).inputValue().replace(",", "."))
        .multiply(java.math.BigDecimal.valueOf(secondsPerUnit));
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

  private void notice(String expectedNotificationText) {
    Locator notificationCardLocator =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(expectedNotificationText));

    assertThat(notificationCardLocator).isVisible();
    notificationCardLocator.getByRole(AriaRole.BUTTON).click();

    assertThat(notificationCardLocator).not().isVisible();
  }

  private Locator button(String name) {
    return browserPage.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }
}
