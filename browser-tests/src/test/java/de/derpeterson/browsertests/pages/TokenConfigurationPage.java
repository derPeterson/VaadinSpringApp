package de.derpeterson.browsertests.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.i18n.UiTexts;
import io.qameta.allure.Step;

/** The four visible token settings; no database or service access. */
public final class TokenConfigurationPage {
  private final Page browserPage;
  private final UiTexts uiTexts = UiTexts.selected();

  public TokenConfigurationPage(Page browserPage) {
    this.browserPage = browserPage;
  }

  @Step("Token-Laufzeiten über die Adminnavigation öffnen")
  public void open() {
    new ConfigurationPage(browserPage).open(de.derpeterson.app.config.AdminConfigGroup.TOKENS);
    assertThat(button(uiTexts.getAdminConfigSave())).isVisible();
  }

  public Locator field(String label) {
    return new ConfigurationPage(browserPage).duration(label).locator("vaadin-big-decimal-field");
  }

  public String value(String label) {
    java.math.BigDecimal seconds = new ConfigurationPage(browserPage).seconds(label);
    java.math.BigInteger[] parts =
        seconds
            .movePointRight(9)
            .toBigIntegerExact()
            .divideAndRemainder(java.math.BigInteger.valueOf(1_000_000_000));
    return java.time.Duration.ofSeconds(parts[0].longValueExact(), parts[1].longValueExact())
        .toString();
  }

  @Step("Dauer als Zahl und Einheit eingeben")
  public void enter(String label, String value) {
    java.time.Duration duration =
        value.startsWith("P")
            ? java.time.Duration.parse(value)
            : java.time.Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
    java.math.BigDecimal seconds =
        java.math.BigDecimal.valueOf(duration.getSeconds())
            .add(java.math.BigDecimal.valueOf(duration.getNano(), 9));
    new ConfigurationPage(browserPage).enterSeconds(label, seconds);
  }

  @Step("Dauer ablehnen und lokalisierten Feldfehler prüfen")
  public void reject(String label, String error) {
    button(uiTexts.getAdminConfigSave()).click();
    assertThat(field(label)).hasAttribute("invalid", "");
    assertThat(field(label)).hasJSProperty("errorMessage", error);
    assertThat(notice()).containsText(uiTexts.getAdminConfigValidationInvalidFields());
    dismiss();
  }

  @Step("Konfiguration speichern")
  public void save() {
    button(uiTexts.getAdminConfigSave()).click();
    assertThat(notice()).containsText(uiTexts.getAdminConfigSaveSuccess());
    dismiss();
  }

  @Step("Gespeicherte Konfiguration erneut laden")
  public void reload() {
    button(uiTexts.getAdminConfigReload()).click();
    assertThat(notice()).containsText(uiTexts.getAdminConfigReloadSuccess());
    dismiss();
  }

  @Step("Mindestdauer und Erklärung im neuen Klick-Tooltip prüfen")
  public void explain(String label, long minutes, String explanation) {
    new ConfigurationPage(browserPage)
        .explain(label, explanation, uiTexts.getAdminConfigValidationDurationMinimum(minutes));
  }

  private Locator notice() {
    return browserPage.locator("vaadin-notification-card:not([closing])");
  }

  private void dismiss() {
    notice().getByRole(AriaRole.BUTTON).click();
    assertThat(notice()).not().isVisible();
  }

  private Locator button(String name) {
    return browserPage.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
  }
}
