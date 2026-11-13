package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/** Configuration through visible groups and inputs; no application API access. */
public final class AdminConfigurationPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public AdminConfigurationPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Gewählte Konfigurationsgruppe über das Adminmenü öffnen")
  public void open(AdminConfigGroup configGroup) {
    new AdminNavigation(browserPage).openGroup(configGroup);

    assertThat(findActionButtonLocator(uiTexts.getAdminConfigSave())).isVisible();
  }

  private void selectQueueSettingsTab(String fieldLabelText) {
    Locator queueTabsLocator = browserPage.locator("#admin-queue-tabs");
    if (!queueTabsLocator.isVisible()) {
      return;
    }
    boolean cleanupSetting =
        List.of(
                uiTexts.getAdminQueueCleanupEnabled(),
                uiTexts.getAdminQueueCleanupInterval(),
                uiTexts.getAdminQueueCleanupMinimumAge(),
                uiTexts.getAdminQueueCleanupHistoryDays())
            .contains(fieldLabelText);
    String selectedTabLabel =
        cleanupSetting
            ? uiTexts.getAdminQueueCleanupAssistantsTab()
            : uiTexts.getAdminQueueCleanupGeneralTab();
    queueTabsLocator
        .getByRole(
            AriaRole.TAB, new Locator.GetByRoleOptions().setName(selectedTabLabel).setExact(true))
        .click();
  }

  public Locator input(String fieldLabelText) {
    selectQueueSettingsTab(fieldLabelText);
    return browserPage
        .getByLabel(fieldLabelText, new Page.GetByLabelOptions().setExact(true))
        .and(browserPage.locator("input"));
  }

  public Locator info(String fieldLabelText) {
    selectQueueSettingsTab(fieldLabelText);
    return browserPage
        .getByLabel(fieldLabelText, new Page.GetByLabelOptions().setExact(true))
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
      // Mindestens ein sichtbares Zeichen verhindert eine leere Wirksamkeitserklärung.
      assertThat(effectiveUseItemLocators.first()).hasText(Pattern.compile("\\S"));
      assertThat(effectiveUseSectionLocator).not().containsText("adminView.config.effective_");
    } else {
      assertThat(effectiveUseItemLocators).hasText(expectedEffectiveUseNotes);
    }
  }

  @Step("Feldinformation per Klick öffnen und die tatsächlichen Regeln prüfen")
  public void explain(String fieldLabelText, String helpDescriptionText, String... inputRuleTexts) {
    info(fieldLabelText).click();

    assertThat(helpPanel().locator("vaadin-dialog-overlay > [part~='overlay']")).isVisible();
    assertThat(helpPanel()).containsText(helpDescriptionText);
    for (String inputRuleText : inputRuleTexts) {
      assertThat(helpPanel()).containsText(inputRuleText);
    }
    assertThat(helpPanel()).containsText(uiTexts.getAdminConfigInputRules());
    closeHelp();

    assertThat(helpPanel()).hasCount(0);
  }

  public void closeHelp() {
    closeHelp(uiTexts.getNotificationsClose());
  }

  public void closeHelp(String closeButtonLabelText) {
    helpPanel()
        .getByRole(
            AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(closeButtonLabelText).setExact(true))
        .last()
        .click();

    assertThat(helpPanel()).hasCount(0);
  }

  public Locator duration(String fieldLabelText) {
    return browserPage
        .locator(".cosmos-duration-field")
        .filter(new Locator.FilterOptions().setHas(input(fieldLabelText)));
  }

  public BigDecimal seconds(String durationFieldLabel) {
    Locator durationFieldLocator = duration(durationFieldLabel);
    Locator durationUnitLabel =
        durationFieldLocator.locator("vaadin-select-value-button > vaadin-select-item");
    String selectedDurationUnitText =
        durationUnitLabel.count() == 0
            ? durationFieldLocator.locator(".cosmos-duration-unit-label").innerText().trim()
            : durationUnitLabel.innerText().trim();
    int secondsPerUnit;
    if (selectedDurationUnitText.equals(uiTexts.getAdminConfigDurationUnitDays())) {
      secondsPerUnit = 86400;
    } else if (selectedDurationUnitText.equals(uiTexts.getAdminConfigDurationUnitHours())) {
      secondsPerUnit = 3600;
    } else if (selectedDurationUnitText.equals(uiTexts.getAdminConfigDurationUnitMinutes())) {
      secondsPerUnit = 60;
    } else {
      assertThat(durationUnitLabel).hasText(uiTexts.getAdminConfigDurationUnitSeconds());
      secondsPerUnit = 1;
    }

    return new BigDecimal(input(durationFieldLabel).inputValue().replace(",", "."))
        .multiply(BigDecimal.valueOf(secondsPerUnit));
  }

  public void enterSeconds(String fieldLabelText, BigDecimal durationSeconds) {
    duration(fieldLabelText).locator("vaadin-select").click();
    browserPage
        .getByRole(
            AriaRole.OPTION,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminConfigDurationUnitSeconds())
                .setExact(true))
        .click();

    String durationValueText = durationSeconds.toPlainString();
    input(fieldLabelText)
        .fill(
            uiTexts.language().equals("de")
                ? durationValueText.replace('.', ',')
                : durationValueText);
    input(fieldLabelText).press("Tab");
  }

  public String value(String fieldLabelText) {
    return input(fieldLabelText).inputValue();
  }

  @Step("Konfigurationswert eingeben")
  public void enter(String fieldLabelText, String fieldValueText) {
    input(fieldLabelText).fill(fieldValueText);
    input(fieldLabelText).press("Tab");
  }

  @Step("Konfiguration speichern und Erfolgshinweis schließen")
  public void save() {
    submit();
    assertAndDismissNotice(uiTexts.getAdminConfigSaveSuccess());
  }

  public void submit() {
    findActionButtonLocator(uiTexts.getAdminConfigSave()).click();
  }

  @Step("Gespeicherte Konfiguration erneut laden")
  public void reload() {
    Locator reloadSuccessNoticeLocator =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(uiTexts.getAdminConfigReloadSuccess()));

    UiActions.complete(browserPage, findActionButtonLocator(uiTexts.getAdminConfigReload())::click);
    new UnsavedChangesPrompt(browserPage, uiTexts)
        .discardAfterActionIfNeeded(() -> reloadSuccessNoticeLocator.count() > 0);

    assertAndDismissNotice(uiTexts.getAdminConfigReloadSuccess());
  }

  @Step("Ungültiges Feld und verständlichen Hinweis prüfen")
  public void reject(String fieldSelector, String expectedErrorText) {
    findActionButtonLocator(uiTexts.getAdminConfigSave()).click();

    assertThat(browserPage.locator(fieldSelector + "[invalid]")).hasCount(1);
    assertThat(browserPage.locator(fieldSelector + "[invalid]"))
        .hasJSProperty("errorMessage", expectedErrorText);
    assertAndDismissNotice(uiTexts.getAdminConfigValidationInvalidFields());
  }

  private void assertAndDismissNotice(String expectedNotificationText) {
    Locator notificationCardLocator =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(new Locator.FilterOptions().setHasText(expectedNotificationText));

    assertThat(notificationCardLocator).isVisible();
    notificationCardLocator.getByRole(AriaRole.BUTTON).click();

    assertThat(notificationCardLocator).not().isVisible();
  }

  private Locator findActionButtonLocator(String buttonLabelText) {
    return browserPage.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buttonLabelText).setExact(true));
  }
}
