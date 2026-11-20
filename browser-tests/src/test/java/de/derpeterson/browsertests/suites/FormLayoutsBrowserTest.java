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
import de.derpeterson.browsertests.pages.ActionGridChecks;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.CosmosFollowUpChecks;
import de.derpeterson.browsertests.pages.ForgotPasswordPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationComposePage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import java.util.List;
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
      if (configurationGroup == AdminConfigGroup.GENERAL) {
        assertConfigurationRequiredIndicators();
        Locator serviceName = configurationPage.input(uiTexts.getAdminConfigEntryServiceName());
        assertThat(serviceName).hasCSS("font-size", "13px");
        assertThat(serviceName.locator("xpath=..").locator("[part=input-field]"))
            .hasCSS("height", "40px");
        assertThat(configurationPage.info(uiTexts.getAdminConfigEntryServiceName()))
            .hasCSS("height", "40px");
      }
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

  private void assertConfigurationRequiredIndicators() {
    Locator requiredLabel = browserPage.locator("#config-label-service_name");
    assertThat(requiredLabel).isVisible();
    assertEquals(
        "\" *\"", requiredLabel.evaluate("label => getComputedStyle(label, '::after').content"));
    assertEquals(
        "rgb(248, 113, 113)",
        requiredLabel.evaluate("label => getComputedStyle(label, '::after').color"));
    Locator optionalLabel = browserPage.locator("#config-label-copyright_text");
    assertThat(optionalLabel).isVisible();
    assertEquals(
        "none", optionalLabel.evaluate("label => getComputedStyle(label, '::after').content"));
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Anbieter und 2FA-Rollen behalten Felder, Tabs und Feldhilfen")
  @Tag("configuration-footers")
  @Tag("switch-style")
  void providerAndRoleFormsKeepTheirArrangement(int viewportWidth) {
    signInForFormInspection(viewportWidth, true);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.EXTERNAL_LOGIN);
    for (String providerId : new String[] {"google", "microsoft"}) {
      browserPage.locator("#provider-tab-" + providerId).click();
      CosmosFollowUpChecks.assertSwitchAppearance(
          browserPage.locator("#provider-" + providerId + "-enabled"));
      captureFormEvidence(
          "provider-" + providerId, browserPage.locator("#admin-provider-settings"));
      captureConfigurationFooter(
          uiTexts.getAdminConfigGroupLabel(AdminConfigGroup.EXTERNAL_LOGIN)
              + " · "
              + browserPage.locator("#provider-tab-" + providerId).innerText(),
          browserPage.locator("#admin-provider-settings"));
    }

    configurationPage.open(AdminConfigGroup.TWO_FACTOR);
    Locator twoFactorSettings = browserPage.locator("#admin-two-factor-settings");
    assertThat(twoFactorSettings.locator("#two-factor-inactivity-enabled")).isVisible();
    for (Locator policySwitch : twoFactorSettings.locator("vaadin-switch:visible").all()) {
      CosmosFollowUpChecks.assertSwitchAppearance(policySwitch);
    }
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
  @Tag("switch-style")
  void mailAndSuperadminFormsKeepTheirArrangement(int viewportWidth) {
    signInForFormInspection(viewportWidth, true);
    new AdminMailPage(browserPage).open();
    captureFormEvidence("mail-test", browserPage.locator(".cosmos-mail-card"));
    NotificationComposePage notificationComposePage = new NotificationComposePage(browserPage);
    notificationComposePage.open();
    Locator everyoneSwitch =
        notificationComposePage.root().locator("#admin-notifications-everyone");
    CosmosFollowUpChecks.assertSwitchAppearance(everyoneSwitch);
    Locator everyoneHint = everyoneSwitch.locator(".cosmos-notification-compose-everyone-hint");
    assertThat(everyoneHint).isVisible();
    assertThat(everyoneHint).hasCSS("font-size", "11px");
    assertThat(everyoneHint).hasCSS("white-space", "normal");
    everyoneSwitch.locator("[slot=label]").click();
    assertThat(notificationComposePage.everyone()).isChecked();
    CosmosFollowUpChecks.assertSwitchAppearance(everyoneSwitch);
    everyoneSwitch.locator("[slot=label]").click();
    assertThat(notificationComposePage.everyone()).not().isChecked();
    captureFormEvidence("notification-compose", notificationComposePage.root());
    if (viewportWidth <= 600) {
      ActionGridChecks.assertAtMobileWidths(
          browserPage,
          () -> {
            for (Locator buttonLocator :
                List.of(notificationComposePage.review(), notificationComposePage.send())) {
              ActionGridChecks.assertButtonsFillColumn(
                  buttonLocator.locator("xpath=.."), buttonLocator);
            }
          });
    }

    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    adminUsersPage.open();
    AdminUserDialog createUserDialog = adminUsersPage.openCreate();
    CosmosFollowUpChecks.assertCloseCircle(
        createUserDialog.root().locator(".cosmos-overlay-close"),
        uiTexts.getAdminUsersCloseDialog());
    assertThat(createUserDialog.firstName()).hasCSS("font-size", "13px");
    assertThat(createUserDialog.firstName().locator("xpath=..").locator("[part=input-field]"))
        .hasCSS("height", "40px");
    assertThat(createUserDialog.saveButton()).hasCSS("font-size", "12px");
    ActionGridChecks.assertActions(
        createUserDialog.root().locator(".cosmos-dialog-actions"), 2, viewportWidth <= 479);
    for (Locator requiredInput :
        List.of(
            createUserDialog.firstName(),
            createUserDialog.lastName(),
            createUserDialog.email(),
            createUserDialog.password())) {
      CosmosFollowUpChecks.assertNativeRequiredIndicator(requiredInput.locator("xpath=.."));
    }
    CosmosFollowUpChecks.assertNativeRequiredIndicator(
        createUserDialog.root().locator(".admin-user-roles"));
    assertThat(createUserDialog.root().locator("vaadin-date-picker [part=required-indicator]"))
        .not()
        .isVisible();
    assertThat(createUserDialog.confirmPassword()).isDisabled();
    Locator editor = createUserDialog.root().locator("vaadin-form-layout.cosmos-dialog-form");
    Locator switches = editor.locator("vaadin-switch.admin-user-option-switch");
    assertThat(switches).hasCount(3);
    double rowWidth =
        ((Number)
                editor.evaluate(
                    """
                    form => {
                      const style = getComputedStyle(form);
                      return form.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
                    }
                    """))
            .doubleValue();
    for (Locator editorSwitch : switches.all()) {
      CosmosFollowUpChecks.assertSwitchAppearance(editorSwitch);
      assertThat(editorSwitch).hasCSS("margin-top", "8px");
      assertThat(editorSwitch).hasCSS("margin-bottom", "8px");
      assertEquals(rowWidth, editorSwitch.boundingBox().width, 1);
      assertThat(editorSwitch.locator("[part=label]")).hasCSS("white-space", "normal");
      Locator hint = editorSwitch.locator(".admin-user-option-hint");
      assertThat(hint).isVisible();
      assertThat(hint).hasCSS("font-size", "11px");
      assertThat(hint).hasCSS("font-weight", "400");
    }
    AccountFormAppearanceChecks.captureUserDialog(browserPage, "Superadmin Benutzer anlegen");
    Locator developmentSwitchLocator = createUserDialog.developmentAccountSwitch();
    Locator verificationSwitchLocator = createUserDialog.emailVerificationSwitch();
    assertThat(verificationSwitchLocator).isChecked();
    Locator developmentSwitch = createUserDialog.root().locator("#admin-development-account");
    verificationSwitchLocator.focus();
    browserPage.keyboard().press("Tab");
    CosmosFollowUpChecks.assertSwitchFocus(developmentSwitch);
    developmentSwitchLocator.press("Space");
    assertThat(developmentSwitchLocator).isChecked();
    CosmosFollowUpChecks.assertSwitchAppearance(developmentSwitch);
    assertThat(verificationSwitchLocator).not().isChecked();
    assertThat(verificationSwitchLocator).isDisabled();
    assertThat(createUserDialog.root().locator("#admin-email-verification-required [part=marker]"))
        .hasCSS("background-color", "rgba(255, 255, 255, 0.1)");
    AccountFormAppearanceChecks.captureUserDialog(
        browserPage, "Superadmin Entwicklerkonto anlegen");
    developmentSwitchLocator.uncheck();
    assertThat(verificationSwitchLocator).isEnabled();
    assertThat(verificationSwitchLocator).isChecked();
    createUserDialog.cancel();

    AdminUserDialog editUserDialog = adminUsersPage.openEdit(BrowserTestAccounts.SUPERADMIN_EMAIL);
    assertThat(editUserDialog.password().locator("xpath=..").locator("[part=required-indicator]"))
        .not()
        .isVisible();
    Locator editActionsLocator = editUserDialog.root().locator(".cosmos-dialog-actions");
    ActionGridChecks.assertActions(editActionsLocator, 3, viewportWidth <= 479);
    if (viewportWidth <= 479) {
      Locator deleteButtonLocator = editActionsLocator.locator(".cosmos-delete-button");
      assertTrue(
          deleteButtonLocator.boundingBox().y
              > editActionsLocator.locator("vaadin-button[theme~='primary']").boundingBox().y);
    }
    editUserDialog.openSignInDetails();
    AccountFormAppearanceChecks.captureUserDialog(
        browserPage, "Superadmin Benutzer bearbeiten Details");
    Locator editClose = editUserDialog.root().locator(".cosmos-overlay-close");
    CosmosFollowUpChecks.assertCloseCircle(editClose, uiTexts.getAdminUsersCloseDialog());
    editClose.click();
    assertThat(editUserDialog.root()).hasCount(0);
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
    Locator readonlyEmail = browserPage.locator("#account-email");
    assertThat(readonlyEmail.locator("input")).hasAttribute("readonly", "");
    assertThat(readonlyEmail.locator("[part=input-field]")).hasCSS("border-top-style", "dashed");
    assertThat(readonlyEmail.locator("[part=required-indicator]")).not().isVisible();
    AccountSecurityPage accountSecurityPage = new AccountSecurityPage(browserPage);
    accountSecurityPage.openPassword();
    AccountFormAppearanceChecks.assertEqualDialogActions(
        browserPage,
        browserPage.locator("#account-password-change-dialog"),
        uiTexts.getAdminUsersCancel(),
        uiTexts.getAccountSettingsChangePassword());
    captureFormEvidence(
        "account-password",
        browserPage.locator("#account-password-change-dialog .cosmos-dialog-form"));
    browserPage.locator("#account-password-cancel").click();

    accountSecurityPage.openEmail();
    AccountFormAppearanceChecks.assertEqualDialogActions(
        browserPage,
        browserPage.locator("#account-email-change-dialog"),
        uiTexts.getAdminUsersCancel(),
        uiTexts.getAccountEmailDialogRequest());
    captureFormEvidence(
        "account-email", browserPage.locator("#account-email-change-dialog .cosmos-dialog-form"));
    browserPage.locator("#account-email-cancel").click();

    browserPage.locator("#account-security-tab-providers").click();
    browserPage.locator("#external-connect-google").click();
    AccountFormAppearanceChecks.assertEqualDialogActions(
        browserPage,
        browserPage.locator("#external-identity-dialog"),
        uiTexts.getAdminUsersCancel(),
        uiTexts.getExternalConnect("Google"));
    captureFormEvidence(
        "account-provider-proof",
        browserPage.locator("#external-identity-dialog .cosmos-dialog-form"));
    browserPage.locator("#external-proof-cancel").click();
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
