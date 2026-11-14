package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.pages.AccountFormAppearanceChecks;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Misst die tatsächlichen Formularflächen; bestehende Fachfälle prüfen deren Speicherverträge. */
@Tag("browser")
@Feature("Native Formularlayouts")
public class FormLayoutsBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @CsvSource({"1280,false", "390,false", "1280,true", "390,true"})
  @DisplayName("Formularraster aller allgemeinen Konfigurationsgruppen behalten ihre Anordnung")
  void generalConfigurationFormsKeepTheirArrangement(int viewportWidth, boolean superadmin) {
    signInForFormInspection(viewportWidth, superadmin);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);

    for (AdminConfigGroup configurationGroup : AdminConfigGroup.values()) {
      if (configurationGroup == AdminConfigGroup.EXTERNAL_LOGIN
          || configurationGroup == AdminConfigGroup.TWO_FACTOR
          || configurationGroup.isSuperadminOnly() && !superadmin) {
        continue;
      }
      configurationPage.open(configurationGroup);
      captureFormEvidence(
          "config-" + configurationGroup.name() + "-" + superadmin,
          browserPage.locator(".cosmos-config-card"));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Anbieter und 2FA-Rollen behalten Felder, Tabs und Feldhilfen")
  void providerAndRoleFormsKeepTheirArrangement(int viewportWidth) {
    signInForFormInspection(viewportWidth, true);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.EXTERNAL_LOGIN);
    for (String providerId : new String[] {"google", "microsoft"}) {
      browserPage.locator("#provider-tab-" + providerId).click();
      captureFormEvidence(
          "provider-" + providerId, browserPage.locator("#admin-provider-settings"));
    }

    configurationPage.open(AdminConfigGroup.TWO_FACTOR);
    Locator roleTabsLocator = browserPage.locator("#two-factor-role-tabs").getByRole(AriaRole.TAB);
    int roleCount = roleTabsLocator.count();
    assertTrue(roleCount > 0);
    for (int roleIndex = 0; roleIndex < roleCount; roleIndex++) {
      roleTabsLocator.nth(roleIndex).click();
      captureFormEvidence(
          "two-factor-role-" + roleIndex, browserPage.locator(".cosmos-mfa-settings"));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Mailtest und Superadmin-Zusatzfelder bleiben vollständig bedienbar")
  void mailAndSuperadminFormsKeepTheirArrangement(int viewportWidth) {
    signInForFormInspection(viewportWidth, true);
    new AdminMailPage(browserPage).open();
    captureFormEvidence("mail-test", browserPage.locator(".cosmos-mail-card"));
    NotificationComposePage notificationComposePage = new NotificationComposePage(browserPage);
    notificationComposePage.open();
    captureFormEvidence("notification-compose", notificationComposePage.root());

    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    adminUsersPage.open();
    AdminUserDialog createUserDialog = adminUsersPage.openCreate();
    AccountFormAppearanceChecks.captureUserDialog(browserPage, "Superadmin Benutzer anlegen");
    Locator developmentCheckboxLocator =
        createUserDialog.root().locator("#admin-development-account").getByRole(AriaRole.CHECKBOX);
    Locator verificationCheckboxLocator =
        createUserDialog
            .root()
            .locator("#admin-email-verification-required")
            .getByRole(AriaRole.CHECKBOX);
    assertThat(verificationCheckboxLocator).isChecked();
    developmentCheckboxLocator.check();
    assertThat(verificationCheckboxLocator).not().isChecked();
    assertThat(verificationCheckboxLocator).isDisabled();
    AccountFormAppearanceChecks.captureUserDialog(
        browserPage, "Superadmin Entwicklerkonto anlegen");
    developmentCheckboxLocator.uncheck();
    assertThat(verificationCheckboxLocator).isEnabled();
    assertThat(verificationCheckboxLocator).isChecked();
    createUserDialog.cancel();

    AdminUserDialog editUserDialog = adminUsersPage.openEdit(BrowserTestAccounts.SUPERADMIN_EMAIL);
    editUserDialog.openSignInDetails();
    AccountFormAppearanceChecks.captureUserDialog(
        browserPage, "Superadmin Benutzer bearbeiten Details");
    editUserDialog.cancel();
  }

  private void signInForFormInspection(int viewportWidth, boolean superadmin) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(
        superadmin ? BrowserTestAccounts.SUPERADMIN_EMAIL : BrowserTestAccounts.ADMIN_EMAIL,
        superadmin ? BrowserTestAccounts.SUPERADMIN_PASSWORD : BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/admin");
  }

  private void captureFormEvidence(String formKey, Locator formPanelLocator) {
    assertThat(formPanelLocator).isVisible();
    browserPage.evaluate("() => document.fonts.ready");
    Locator visibleFieldLocators =
        formPanelLocator.locator(
            "vaadin-text-field:visible, vaadin-email-field:visible, vaadin-password-field:visible, "
                + "vaadin-big-decimal-field:visible, vaadin-integer-field:visible, vaadin-select:visible, "
                + "vaadin-combo-box:visible, vaadin-multi-select-combo-box:visible, vaadin-switch:visible, vaadin-text-area:visible");
    assertTrue(visibleFieldLocators.count() > 0);
    visibleFieldLocators.first().scrollIntoViewIfNeeded();
    browserPage.mouse().move(0, 0);

    String evidenceName = formKey + " " + browserPage.viewportSize().width;
    String measurementsJson =
        (String)
            formPanelLocator.evaluate(
                """
                panel => {
                  const bounds = panel.getBoundingClientRect();
                  const fields = Array.from(panel.querySelectorAll(`vaadin-text-field, vaadin-email-field,
                    vaadin-password-field, vaadin-big-decimal-field, vaadin-integer-field, vaadin-select,
                    vaadin-combo-box, vaadin-multi-select-combo-box, vaadin-switch, vaadin-text-area`))
                    .filter(field => field.getBoundingClientRect().height > 0).map(field => {
                      const rect = field.getBoundingClientRect();
                      const input = field.shadowRoot?.querySelector('[part="input-field"]');
                      const style = getComputedStyle(input ?? field);
                      return {id: field.id, tag: field.localName, label: field.label,
                        x: rect.x - bounds.x, y: rect.y - bounds.y, width: rect.width, height: rect.height,
                        font: style.font, color: style.color, background: style.backgroundColor, border: style.border};
                    });
                  return JSON.stringify({width: bounds.width, height: bounds.height, fields});
                }
                """);
    Allure.attachment(evidenceName + " Messwerte", "application/json", measurementsJson);
    attachViewportScreenshot(evidenceName + " oben");

    visibleFieldLocators.last().scrollIntoViewIfNeeded();
    attachViewportScreenshot(evidenceName + " unten");
  }

  private void attachViewportScreenshot(String evidenceName) {
    Allure.attachment(
        evidenceName,
        "image/png",
        new ByteArrayInputStream(
            browserPage.screenshot(
                new Page.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
  }
}
