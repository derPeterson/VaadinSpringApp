package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ScreenshotAnimations;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AccountFormAppearanceChecks;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AccountSecurityPage;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.RegistrationPage;
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
  @Tag("configuration-footers")
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
      String userRoleLabel = superadmin ? uiTexts.getRoleSuperadmin() : uiTexts.getRoleAdmin();
      captureConfigurationFooter(
          uiTexts.getAdminConfigGroupLabel(configurationGroup) + " · " + userRoleLabel,
          browserPage.locator(".cosmos-config-card"));
      if (configurationGroup == AdminConfigGroup.QUEUE && superadmin) {
        browserPage
            .locator("#admin-queue-tabs")
            .getByRole(
                AriaRole.TAB,
                new Locator.GetByRoleOptions()
                    .setName(uiTexts.getAdminQueueCleanupAssistantsTab())
                    .setExact(true))
            .click();
        captureConfigurationFooter(
            uiTexts.getAdminConfigGroupLabel(configurationGroup)
                + " · "
                + uiTexts.getAdminQueueCleanupAssistantsTab()
                + " · "
                + userRoleLabel,
            browserPage.locator(".cosmos-config-card"));
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Anbieter und 2FA-Rollen behalten Felder, Tabs und Feldhilfen")
  @Tag("configuration-footers")
  void providerAndRoleFormsKeepTheirArrangement(int viewportWidth) {
    signInForFormInspection(viewportWidth, true);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.EXTERNAL_LOGIN);
    for (String providerId : new String[] {"google", "microsoft"}) {
      browserPage.locator("#provider-tab-" + providerId).click();
      captureFormEvidence(
          "provider-" + providerId, browserPage.locator("#admin-provider-settings"));
      captureConfigurationFooter(
          uiTexts.getAdminConfigGroupLabel(AdminConfigGroup.EXTERNAL_LOGIN)
              + " · "
              + browserPage.locator("#provider-tab-" + providerId).innerText(),
          browserPage.locator("#admin-provider-settings"));
    }

    configurationPage.open(AdminConfigGroup.TWO_FACTOR);
    Locator roleTabsLocator = browserPage.locator("#two-factor-role-tabs").getByRole(AriaRole.TAB);
    int roleCount = roleTabsLocator.count();
    assertTrue(roleCount > 0);
    for (int roleIndex = 0; roleIndex < roleCount; roleIndex++) {
      roleTabsLocator.nth(roleIndex).click();
      captureFormEvidence(
          "two-factor-role-" + roleIndex, browserPage.locator(".cosmos-mfa-settings"));
      captureConfigurationFooter(
          uiTexts.getAdminConfigGroupLabel(AdminConfigGroup.TWO_FACTOR)
              + " · "
              + roleTabsLocator.nth(roleIndex).innerText(),
          browserPage.locator(".cosmos-mfa-settings"));
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

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Kontodialoge behalten Eingaben, Hinweise und Aktionsbereiche")
  void accountDialogsKeepTheirArrangement(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    String accountEmailAddress = "form-layout-" + viewportWidth + "@example.test";
    RegistrationPage registrationPage = new RegistrationPage(browserPage);
    registrationPage.open();
    registrationPage.fillValid(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    registrationPage.submit();
    registrationPage.assertSubmitted(accountEmailAddress);
    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    try (Page verificationPage =
        mailpitInbox.openVerificationLink(
            mailpitInbox.awaitVerificationMail(accountEmailAddress))) {
      assertThat(verificationPage)
          .hasTitle(de.derpeterson.app.config.AppRouteConstants.VERIFICATION_PAGE_TITLE);
    }
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(accountEmailAddress, BrowserTestAccounts.USER_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    new AccountPage(browserPage).open();
    AccountSecurityPage accountSecurityPage = new AccountSecurityPage(browserPage);
    accountSecurityPage.openPassword();
    captureFormEvidence(
        "account-password",
        browserPage.locator("#account-password-change-dialog .cosmos-dialog-form"));
    browserPage.locator("#account-password-cancel").click();

    accountSecurityPage.openEmail();
    assertEmailDialogActionLayout(viewportWidth <= 479);
    captureFormEvidence(
        "account-email", browserPage.locator("#account-email-change-dialog .cosmos-dialog-form"));
    if (viewportWidth == 390) {
      browserPage.setViewportSize(320, 844);
      assertEmailDialogActionLayout(true);
      browserPage.setViewportSize(viewportWidth, 844);
    }
    browserPage.locator("#account-email-cancel").click();

    browserPage.locator("#account-security-tab-providers").click();
    browserPage.locator("#external-connect-google").click();
    captureFormEvidence(
        "account-provider-proof",
        browserPage.locator("#external-identity-dialog .cosmos-dialog-form"));
    browserPage.locator("#external-proof-cancel").click();
  }

  private void assertEmailDialogActionLayout(boolean stacked) {
    Locator emailDialogLocator = browserPage.locator("#account-email-change-dialog");
    Locator actionsLocator = emailDialogLocator.locator(".cosmos-dialog-actions");
    Locator cancelButtonLocator = actionsLocator.locator("#account-email-cancel");
    Locator requestButtonLocator = actionsLocator.locator("#account-email-request");

    assertThat(actionsLocator).hasCSS("display", "grid");
    assertThat(cancelButtonLocator).hasText(uiTexts.getAdminUsersCancel());
    assertThat(requestButtonLocator).hasText(uiTexts.getAccountEmailDialogRequest());

    BoundingBox actionsBounds = actionsLocator.boundingBox();
    BoundingBox cancelBounds = cancelButtonLocator.boundingBox();
    BoundingBox requestBounds = requestButtonLocator.boundingBox();

    assertEquals(cancelBounds.width, requestBounds.width, 1);
    if (stacked) {
      assertEquals(actionsBounds.width, cancelBounds.width, 1);
      assertEquals(cancelBounds.x, requestBounds.x, 1);
      assertTrue(cancelBounds.y + cancelBounds.height <= requestBounds.y);
    } else {
      assertEquals(cancelBounds.y, requestBounds.y, 1);
      assertTrue(cancelBounds.x + cancelBounds.width <= requestBounds.x);
    }

    for (Locator buttonLocator : new Locator[] {cancelButtonLocator, requestButtonLocator}) {
      Locator labelLocator = buttonLocator.locator("[part~='label']");
      assertThat(labelLocator).isVisible();

      assertTrue(
          (Boolean)
              labelLocator.evaluate(
                  """
                  label => label.scrollWidth <= label.clientWidth + 1
                    && label.scrollHeight <= label.clientHeight + 1
                  """));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Öffentliche Formularpanels behalten Feldabstände und Hinweise")
  void publicFormsKeepTheirArrangement(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    new LoginPage(browserPage).open();
    captureFormEvidence("public-login", browserPage.locator(".public-panel-body").first());

    new RegistrationPage(browserPage).open();
    captureFormEvidence("public-registration", browserPage.locator(".public-panel-body"));

    new ForgotPasswordPage(browserPage).open();
    captureFormEvidence("public-forgot-password", browserPage.locator(".public-panel-body"));
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
    assertFieldHelpColumnsUseAvailableWidth(formPanelLocator);

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

  private void captureConfigurationFooter(String scenarioLabel, Locator formPanelLocator) {
    Locator footerActionsLocator = formPanelLocator.locator(".cosmos-config-card-actions:visible");
    assertThat(footerActionsLocator).hasCount(1);
    assertThat(footerActionsLocator.locator("vaadin-button")).hasCount(2);
    footerActionsLocator.scrollIntoViewIfNeeded();
    browserPage.mouse().move(0, 0);
    Allure.attachment(
        "Konfigurationsfooter " + scenarioLabel + " " + browserPage.viewportSize().width,
        "image/png",
        new ByteArrayInputStream(
            footerActionsLocator.screenshot(
                new Locator.ScreenshotOptions().setAnimations(ScreenshotAnimations.DISABLED))),
        AttachmentOptions.empty());
    assertMobileFooterActions(footerActionsLocator);
  }

  private void assertMobileFooterActions(Locator footerActionsLocator) {
    if (browserPage.viewportSize().width > 600) {
      return;
    }
    Locator actionButtonsLocator = footerActionsLocator.locator("vaadin-button");
    double availableWidth =
        ((Number)
                footerActionsLocator.evaluate(
                    "element => element.clientWidth - parseFloat(getComputedStyle(element).paddingLeft) - parseFloat(getComputedStyle(element).paddingRight)"))
            .doubleValue();
    double leftEdge =
        footerActionsLocator.boundingBox().x
            + ((Number)
                    footerActionsLocator.evaluate(
                        "element => parseFloat(getComputedStyle(element).paddingLeft) + parseFloat(getComputedStyle(element).borderLeftWidth)"))
                .doubleValue();
    double rowGap =
        ((Number)
                footerActionsLocator.evaluate(
                    "element => parseFloat(getComputedStyle(element).rowGap)"))
            .doubleValue();
    BoundingBox reloadButtonBounds = actionButtonsLocator.nth(0).boundingBox();
    BoundingBox saveButtonBounds = actionButtonsLocator.nth(1).boundingBox();
    assertEquals(availableWidth, reloadButtonBounds.width, 1);
    assertEquals(availableWidth, saveButtonBounds.width, 1);
    assertEquals(leftEdge, reloadButtonBounds.x, 1);
    assertEquals(leftEdge, saveButtonBounds.x, 1);
    assertEquals(reloadButtonBounds.height, saveButtonBounds.height, 1);
    assertEquals(rowGap, saveButtonBounds.y - reloadButtonBounds.y - reloadButtonBounds.height, 1);
  }

  private static void assertFieldHelpColumnsUseAvailableWidth(Locator formPanelLocator) {
    boolean alignedHelpColumns =
        (Boolean)
            formPanelLocator.evaluate(
                """
                panel => Array.from(panel.querySelectorAll('.cosmos-config-input-row'))
                  .filter(row => row.getBoundingClientRect().width > 0).every(row => {
                    const slot = row.querySelector(':scope > .cosmos-config-help-slot');
                    if (!slot) return true;
                    const input = row.firstElementChild.getBoundingClientRect();
                    const help = slot.getBoundingClientRect();
                    const bounds = row.getBoundingClientRect();
                    const gap = parseFloat(getComputedStyle(row).columnGap);
                    return Math.abs(help.right - bounds.right) < .2
                      && Math.abs(help.left - input.right - gap) < .2;
                  })
                """);
    assertTrue(
        alignedHelpColumns, "Feldhilfespalten müssen ohne zusätzliche Leerspalte anschließen");
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
