package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_PAGE_TITLE;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.PENDING_EMAIL_VERIFICATION_ROUTE;
import static de.derpeterson.app.config.AppRouteConstants.VERIFICATION_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static de.derpeterson.browsertests.BrowserRoutes.urlPattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.i18n.UiTexts;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.CosmosFollowUpChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UnsavedChangesPrompt;
import de.derpeterson.browsertests.pages.UserMenu;
import de.derpeterson.browsertests.pages.VerificationPage;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.playwright.AllurePlaywright;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Tag("email-verification")
@Feature("Verifikation")
public class VerificationBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @Tag("breadcrumbs")
  @Tag("component-follow-up")
  @ValueSource(ints = {1280, 390})
  @DisplayName("Verifikations- und Reaktivierungspfade bleiben ohne Navigationslinks")
  void verificationAndReactivationBreadcrumbsStayUnlinked(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    String emailAddress = newVerificationEmailAddress();
    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail verificationMail =
        registerAccountAndAwaitVerificationMail(emailAddress, mailpitInbox);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertPendingVerificationPage(emailAddress);
    assertUnlinkedPortalBreadcrumb(browserPage);
    CosmosFollowUpChecks.assertPendingAccountStyle(browserPage, uiTexts);
    AllurePlaywright.attachScreenshot("Verifikationspfad " + viewportWidth, browserPage);

    try (Page activatedPage = mailpitInbox.openVerificationLink(verificationMail)) {
      activatedPage.setViewportSize(viewportWidth, 900);
      assertUnlinkedPortalBreadcrumb(activatedPage);
      assertOrdinaryLogin(activatedPage, emailAddress);
      activatedPage.navigate("/account?section=security&tab=lifecycle");
      activatedPage.locator("#account-deactivate").click();
      activatedPage
          .locator("#account-lifecycle-password input")
          .fill(BrowserTestAccounts.USER_PASSWORD);
      activatedPage.locator("#account-lifecycle-submit").click();
      assertThat(activatedPage.getByText(uiTexts.getAccountDeactivatedSuccess())).isVisible();
    }

    loginPage.open();
    loginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertThat(browserPage.locator("#pending-reactivation-email")).hasText(emailAddress);
    assertUnlinkedPortalBreadcrumb(browserPage);
    CosmosFollowUpChecks.assertPendingAccountStyle(browserPage, uiTexts);
    AllurePlaywright.attachScreenshot("Reaktivierungspfad " + viewportWidth, browserPage);
    browserPage.navigate("/account?section=profile");
    assertThat(browserPage.locator("#pending-reactivation-email")).hasText(emailAddress);
    assertUnlinkedPortalBreadcrumb(browserPage);

    MailpitInbox.CapturedMail deactivationMail =
        mailpitInbox.awaitSecurityMail(emailAddress, uiTexts.getAccountDeactivatedMailSubject());
    assertAdminBlockPreservesSelfDeactivation(emailAddress, deactivationMail, viewportWidth);
    loginPage.open();
    loginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertThat(browserPage.locator("#pending-reactivation-email")).hasText(emailAddress);

    try (Page reactivationPage = mailpitInbox.openAccountReactivationAction(deactivationMail)) {
      reactivationPage.setViewportSize(viewportWidth, 900);
      assertUnlinkedPortalBreadcrumb(reactivationPage);
      AllurePlaywright.attachScreenshot(
          "Reaktivierungslinkpfad " + viewportWidth, reactivationPage);
    }
  }

  private void assertAdminBlockPreservesSelfDeactivation(
      String emailAddress, MailpitInbox.CapturedMail deactivationMail, int viewportWidth) {
    try (BrowserActor administratorActor = new BrowserActor(testBrowser)) {
      administratorActor.browserPage.setViewportSize(viewportWidth, 1000);
      AdminUsersPage adminUsersPage =
          administratorActor.loginAdmin(
              BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
      AdminUserDialog editUserDialog = adminUsersPage.openEdit(emailAddress);
      Locator signInDetailsLocator = editUserDialog.root().locator("#admin-user-sign-in-details");
      assertThat(signInDetailsLocator).not().hasAttribute("opened", "");
      assertThat(editUserDialog.administrativeBlockCheckbox()).not().isChecked();
      CosmosFollowUpChecks.assertDetailsWithoutHover(
          editUserDialog.root().locator("#admin-user-sign-in-toggle"));
      editUserDialog.openSignInDetails();
      Locator deactivationWarningLocator =
          editUserDialog.root().locator("#admin-user-self-deactivation-warning");
      assertThat(deactivationWarningLocator).isVisible();
      assertThat(editUserDialog.root().locator(".admin-user-lifecycle-status"))
          .hasCSS("border-top-width", "1px");
      assertThat(deactivationWarningLocator).hasCSS("border-inline-start-width", "4px");
      assertThat(deactivationWarningLocator.locator("#admin-user-self-deactivation"))
          .hasText(uiTexts.getAdminUserSelfDeactivated() + ": ");
      assertThat(deactivationWarningLocator.locator("#admin-user-self-deactivation"))
          .hasCSS("font-weight", "700");
      String deletionDeadlineText =
          editUserDialog.root().locator("#admin-user-deletion-deadline").innerText();
      org.junit.jupiter.api.Assertions.assertTrue(deletionDeadlineText.endsWith(" UTC"));
      org.junit.jupiter.api.Assertions.assertFalse(deletionDeadlineText.contains("\uFFFD"));
      AllurePlaywright.attachScreenshot(
          "Selbstdeaktivierung im Benutzereditor", administratorActor.browserPage);

      editUserDialog.firstName().fill("Updated account");
      editUserDialog.save();
      editUserDialog = adminUsersPage.openEdit(emailAddress);
      editUserDialog.openSignInDetails();
      assertThat(editUserDialog.root().locator("#admin-user-deletion-deadline"))
          .hasText(deletionDeadlineText);
      editUserDialog.setEnabled(false);
      editUserDialog.save();

      LoginPage blockedLoginPage = new LoginPage(browserPage);
      blockedLoginPage.open();
      blockedLoginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
      blockedLoginPage.assertRejected();
      try (Page blockedReactivationPage =
          new MailpitInbox(browserContext).openAccountReactivationAction(deactivationMail)) {
        blockedReactivationPage.locator("#account-reactivation-submit").click();
        assertThat(blockedReactivationPage.getByText(uiTexts.getAccountReactivateInvalid()))
            .isVisible();
      }

      editUserDialog = adminUsersPage.openEdit(emailAddress);
      editUserDialog.setEnabled(true);
      editUserDialog.save();
      editUserDialog = adminUsersPage.openEdit(emailAddress);
      editUserDialog.openSignInDetails();
      assertThat(editUserDialog.root().locator("#admin-user-deletion-deadline"))
          .hasText(deletionDeadlineText);
      editUserDialog.cancel();
    }
  }

  private void assertUnlinkedPortalBreadcrumb(Page currentBrowserPage) {
    assertThat(currentBrowserPage.locator(".cosmos-breadcrumbs > vaadin-breadcrumbs-item"))
        .hasText(new String[] {uiTexts.getAccountNavigationPortal()});
    assertThat(currentBrowserPage.locator(".cosmos-breadcrumbs a")).hasCount(0);
    assertThat(currentBrowserPage.locator(".cosmos-breadcrumbs"))
        .hasCSS("text-transform", "uppercase");
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "VER-03: Nur ein korrekter erster Nachweis öffnet die eingeschränkte Bestätigungsseite")
  void pendingVerificationKeepsPortalAccessBlocked(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    String emailAddress = newVerificationEmailAddress();
    MailpitInbox mailpitInbox = new MailpitInbox(browserContext);
    MailpitInbox.CapturedMail verificationMail =
        registerAccountAndAwaitVerificationMail(emailAddress, mailpitInbox);
    Instant verificationResendAllowedInstant = Instant.now().plusSeconds(61);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signInFirstFactor(emailAddress, "WrongPassword1!");
    loginPage.assertRejected();

    loginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertPendingVerificationPage(emailAddress);
    assertPendingProofSurvivesRefreshAndLanguageChange(emailAddress);
    assertImmediateResendIsThrottled();
    AllurePlaywright.attachScreenshot(
        "Ausstehende E-Mail-Bestätigung bei " + viewportWidth + " px", browserPage);

    assertPendingPortalAccessAndCancellation(emailAddress, loginPage);
    if (viewportWidth == 1280) {
      verificationMail =
          resendVerificationMailAfterCooldownOnMobile(
              emailAddress, mailpitInbox, verificationMail, verificationResendAllowedInstant);
      browserPage.setViewportSize(viewportWidth, 900);
    }

    try (Page activatedPage = mailpitInbox.openVerificationLink(verificationMail)) {
      assertOrdinaryLogin(activatedPage, emailAddress);
      activatedPage.navigate("/account?section=security");
      assertThat(activatedPage.locator("#email-verification-status"))
          .hasText(uiTexts.getEmailVerified());
      assertThat(activatedPage.locator("#account-password-status"))
          .hasText(uiTexts.getAccountPasswordConfigured());
      AllurePlaywright.attachScreenshot(
          "Verifizierte Adresse und eingerichtetes Passwort", activatedPage);
    }
  }

  private void assertPendingProofSurvivesRefreshAndLanguageChange(String emailAddress) {
    browserPage.reload();
    assertPendingVerificationPage(emailAddress);
    UiTexts alternateUiTexts = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    new UserMenu(browserPage).selectLanguage(alternateUiTexts.language());
    assertThat(browserPage).hasTitle(alternateUiTexts.getEmailVerificationTitle());
    assertThat(browserPage.locator("#pending-verification-email")).hasText(emailAddress);
    assertThat(browserPage.locator("#resend-verification-email"))
        .hasText(alternateUiTexts.getEmailVerificationResend());
    new UserMenu(browserPage).selectTestLanguage();
  }

  private void assertImmediateResendIsThrottled() {
    browserPage.locator("#resend-verification-email").click();
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getEmailVerificationWait());
    browserPage
        .locator("vaadin-notification-card:not([closing])")
        .getByRole(AriaRole.BUTTON)
        .click();
  }

  private void assertPendingPortalAccessAndCancellation(String emailAddress, LoginPage loginPage) {
    browserPage.navigate("/account?section=security");
    assertThat(browserPage.locator("#account-panel-security")).hasCount(0);
    browserPage.navigate("/admin");
    assertThat(browserPage.locator(".admin-users-grid")).hasCount(0);
    loginPage.open();
    loginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(uiTexts.getBaseLogoutButton()).setExact(true))
        .click();
    loginPage.assertFormVisible();
    browserPage.navigate("/verify-email");
    loginPage.assertFormVisible();
  }

  private MailpitInbox.CapturedMail resendVerificationMailAfterCooldownOnMobile(
      String emailAddress,
      MailpitInbox mailpitInbox,
      MailpitInbox.CapturedMail previousVerificationMail,
      Instant verificationResendAllowedInstant) {
    new LoginPage(browserPage).signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    browserPage.setViewportSize(390, 900);
    browserPage.waitForCondition(
        () -> Instant.now().isAfter(verificationResendAllowedInstant),
        new Page.WaitForConditionOptions().setTimeout(65_000));
    UiActions.complete(
        browserPage, () -> browserPage.locator("#resend-verification-email").click());
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getEmailVerificationQueued());
    AllurePlaywright.attachScreenshot(
        "Erneuter Versand nach echter Wartezeit auf Smartphonebreite", browserPage);
    return mailpitInbox.awaitVerificationMailAfter(emailAddress, previousVerificationMail);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "VER-04: Normale administrative Anlage erfordert Bestätigung und bewahrt Mailaktionsentwürfe")
  @Tag("form-details")
  @Tag("component-follow-up")
  void administrativeCreationRequiresMailProof(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    adminUsersPage.open();
    AdminUserDialog createUserDialog = adminUsersPage.openCreate();
    assertThat(createUserDialog.root().locator("#admin-email-verification-required")).hasCount(0);
    assertThat(createUserDialog.root().locator("#admin-development-account")).hasCount(0);
    String emailAddress = newVerificationEmailAddress();
    createUserDialog.fillNewUser(emailAddress, BrowserTestAccounts.USER_PASSWORD, true, false);
    createUserDialog.save();
    new MailpitInbox(browserContext).awaitVerificationMail(emailAddress);

    AdminUserDialog editUserDialog = adminUsersPage.openEdit(emailAddress);
    CosmosFollowUpChecks.assertDetailsWithoutHover(
        editUserDialog.root().locator("#admin-user-sign-in-toggle"));
    editUserDialog.openSignInDetails();
    editUserDialog =
        inspectAccountTypeInBothLanguagesAndReopenEditor(
            adminUsersPage, editUserDialog, emailAddress, false);
    assertThat(editUserDialog.root().locator("#admin-user-registered-via"))
        .hasText(uiTexts.getRegistrationAdministration());
    assertThat(editUserDialog.root().locator("#email-verification-status"))
        .hasText(uiTexts.getEmailVerificationPending());
    assertThat(editUserDialog.root().locator("#admin-resend-verification-mail")).isEnabled();
    assertThat(editUserDialog.root().locator("#admin-send-password-reset")).isDisabled();
    editUserDialog.root().locator(".admin-user-sign-in").scrollIntoViewIfNeeded();
    AllurePlaywright.attachScreenshot("Anmeldebereich mit ausstehendem Nachweis", browserPage);
    Locator firstNameInputLocator = editUserDialog.firstName();
    firstNameInputLocator.fill("Unsaved draft");
    new UnsavedChangesPrompt(browserPage, uiTexts).assertBrowserReloadCanBeCancelled();
    UiActions.complete(browserPage, () -> firstNameInputLocator.press("Tab"));
    Locator resendMailButtonLocator =
        editUserDialog.root().locator("#admin-resend-verification-mail");
    resendMailButtonLocator.scrollIntoViewIfNeeded();
    resendMailButtonLocator.focus();
    UiActions.complete(browserPage, resendMailButtonLocator::click);
    assertThat(editUserDialog.firstName()).hasValue("Unsaved draft");
    assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
        .containsText(uiTexts.getEmailVerificationWait());
    browserPage
        .locator("vaadin-notification-card:not([closing])")
        .getByRole(AriaRole.BUTTON)
        .click();
    editUserDialog.cancel();

    try (BrowserActor pendingUserActor = new BrowserActor(testBrowser)) {
      pendingUserActor.browserPage.setViewportSize(viewportWidth, 900);
      pendingUserActor.login(emailAddress, BrowserTestAccounts.USER_PASSWORD);
      assertThat(pendingUserActor.browserPage.locator("#pending-verification-email"))
          .hasText(emailAddress);
      MailpitInbox mailpitInbox = new MailpitInbox(pendingUserActor.browserContext);
      try (Page activatedPage =
          mailpitInbox.openVerificationLink(mailpitInbox.awaitVerificationMail(emailAddress))) {
        assertOrdinaryLogin(activatedPage, emailAddress);
      }
    }

    editUserDialog = adminUsersPage.openEdit(emailAddress);
    editUserDialog.openSignInDetails();
    assertThat(editUserDialog.firstName()).hasValue("Browser");
    assertThat(editUserDialog.root().locator("#email-verification-status"))
        .hasText(uiTexts.getEmailVerified());
    assertThat(editUserDialog.root().locator("#admin-resend-verification-mail")).isDisabled();
    editUserDialog.root().locator("#admin-send-password-reset").click();
    new MailpitInbox(browserContext).awaitPasswordResetMail(emailAddress);
    assertThat(editUserDialog.root()).hasCount(1);
    editUserDialog.cancel();
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "VER-05: Nur der Superadmin kann die standardmäßig ausgewählte Verifikation abwählen")
  void superadminCanExplicitlyCreateUncheckedAccount(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.navigate("/account?section=security");
    assertThat(browserPage.locator("#email-verification-status"))
        .hasText(uiTexts.getEmailNotChecked());

    browserPage.navigate("/admin?section=users");
    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    adminUsersPage.open();
    AdminUserDialog createUserDialog = adminUsersPage.openCreate();
    Locator verificationCheckboxLocator =
        createUserDialog
            .root()
            .locator("#admin-email-verification-required")
            .getByRole(AriaRole.CHECKBOX);
    assertThat(verificationCheckboxLocator).isChecked();
    assertThat(
            createUserDialog
                .root()
                .getByText(
                    uiTexts.getAdminEmailVerificationHint(),
                    new Locator.GetByTextOptions().setExact(true)))
        .isVisible();
    String emailAddress = newVerificationEmailAddress();
    createUserDialog.fillNewUser(emailAddress, BrowserTestAccounts.USER_PASSWORD, true, false);
    verificationCheckboxLocator.uncheck();
    AllurePlaywright.attachScreenshot("Verifikation bei der Kontoanlage", browserPage);
    createUserDialog.save();
    AdminUserDialog editUserDialog = adminUsersPage.openEdit(emailAddress);
    editUserDialog.openSignInDetails();
    assertThat(editUserDialog.root().locator("#email-verification-status"))
        .hasText(uiTexts.getEmailNotChecked());
    assertThat(editUserDialog.root().locator("#admin-user-registered-via"))
        .hasText(uiTexts.getRegistrationAdministration());
    editUserDialog.cancel();
    new UserMenu(browserPage).signOut();

    loginPage.open();
    loginPage.signIn(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    browserPage.navigate("/account?section=security");
    assertThat(browserPage.locator("#email-verification-status"))
        .hasText(uiTexts.getEmailNotChecked());
    assertThat(browserPage.locator("#account-password-status"))
        .hasText(uiTexts.getAccountPasswordConfigured());
    AllurePlaywright.attachScreenshot("Ungeprüfte Adresse neben der Überschrift", browserPage);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName("Entwicklungskonten sind ausdrücklich gekennzeichnet und senden keine Kontomails")
  @Tag("component-follow-up")
  void developmentAccountIsExplicitAndDoesNotSendAccountMail(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
    adminUsersPage.open();
    AdminUserDialog createUserDialog = adminUsersPage.openCreate();
    Locator developmentCheckbox =
        createUserDialog.root().locator("#admin-development-account").getByRole(AriaRole.CHECKBOX);
    Locator verificationCheckbox =
        createUserDialog
            .root()
            .locator("#admin-email-verification-required")
            .getByRole(AriaRole.CHECKBOX);
    assertThat(developmentCheckbox).not().isChecked();
    assertThat(verificationCheckbox).isChecked();
    String developmentEmail = newVerificationEmailAddress();
    createUserDialog.fillNewUser(developmentEmail, BrowserTestAccounts.USER_PASSWORD, true, false);
    developmentCheckbox.check();
    assertThat(verificationCheckbox).not().isChecked();
    assertThat(verificationCheckbox).isDisabled();
    AllurePlaywright.attachScreenshot(
        "Entwicklungskonto anlegen bei " + viewportWidth + " px", browserPage);
    createUserDialog.save();

    AdminUserDialog editUserDialog = adminUsersPage.openEdit(developmentEmail);
    editUserDialog.openSignInDetails();
    editUserDialog =
        inspectAccountTypeInBothLanguagesAndReopenEditor(
            adminUsersPage, editUserDialog, developmentEmail, true);
    assertThat(editUserDialog.root().locator("#admin-user-account-type"))
        .hasText(uiTexts.getDevelopmentAccountLabel());
    assertThat(editUserDialog.root().locator("#admin-resend-verification-mail")).isDisabled();
    assertThat(editUserDialog.root().locator("#admin-send-password-reset")).isDisabled();
    assertThat(editUserDialog.root()).containsText(uiTexts.getDevelopmentAccountMailUnavailable());
    editUserDialog.root().locator("#admin-user-account-type").scrollIntoViewIfNeeded();
    AllurePlaywright.attachScreenshot(
        "Entwicklungskonto bearbeiten bei " + viewportWidth + " px", browserPage);
    editUserDialog.cancel();
    new UserMenu(browserPage).signOut();

    loginPage.signIn(developmentEmail, BrowserTestAccounts.USER_PASSWORD);
    browserPage.navigate("/account?section=security&tab=lifecycle");
    browserPage.locator("#account-delete").click();
    browserPage
        .locator("#account-lifecycle-password input")
        .fill(BrowserTestAccounts.USER_PASSWORD);
    browserPage.locator("#account-lifecycle-submit").click();
    assertThat(browserPage.getByText(uiTexts.getAccountDeletedSuccess())).isVisible();
    assertThat(browserPage.getByText(uiTexts.getAccountLifecycleMailQueued())).hasCount(0);
    new MailpitInbox(browserContext).assertNoMailFor(developmentEmail);
  }

  private AdminUserDialog inspectAccountTypeInBothLanguagesAndReopenEditor(
      AdminUsersPage adminUsersPage,
      AdminUserDialog editUserDialog,
      String emailAddress,
      boolean developmentAccountExpected) {
    CosmosFollowUpChecks.assertAccountType(
        editUserDialog.root(), uiTexts, developmentAccountExpected);
    assertThat(editUserDialog.root().locator(".admin-user-lifecycle-status:visible")).hasCount(0);
    editUserDialog.cancel();

    UiTexts alternateUiTexts = new UiTexts(uiTexts.language().equals("de") ? "en" : "de");
    new UserMenu(browserPage).selectLanguage(alternateUiTexts.language());
    AdminUserDialog translatedUserDialog =
        new AdminUsersPage(browserPage, alternateUiTexts).openEdit(emailAddress);
    translatedUserDialog.openSignInDetails();
    CosmosFollowUpChecks.assertAccountType(
        translatedUserDialog.root(), alternateUiTexts, developmentAccountExpected);
    AllurePlaywright.attachScreenshot("Kontotyp nach Sprachwechsel", browserPage);
    translatedUserDialog.cancel();
    new UserMenu(browserPage).selectTestLanguage();

    AdminUserDialog reopenedUserDialog = adminUsersPage.openEdit(emailAddress);
    reopenedUserDialog.openSignInDetails();
    return reopenedUserDialog;
  }

  private void assertPendingVerificationPage(String emailAddress) {
    assertThat(browserPage).hasURL(urlPattern(PENDING_EMAIL_VERIFICATION_ROUTE));
    assertThat(browserPage.locator("#pending-verification-email")).hasText(emailAddress);
    assertThat(browserPage.locator(".cosmos-security-warning"))
        .containsText(uiTexts.getEmailVerificationSpamHint());
    assertThat(browserPage.locator("vaadin-button.tracked-user-button")).hasCount(0);
    assertThat(browserPage.locator("#email-verification-status"))
        .hasText(uiTexts.getEmailVerificationPending());
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  @DisplayName(
      "VER-06: Ein Startkonto mit ausdrücklicher Verifikationspflicht erhält eine echte Bestätigungsmail")
  void bootstrapAccountCanRequireVerification(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 900);
    String emailAddress = "bootstrap-verification-" + viewportWidth + "@example.test";
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signInFirstFactor(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    assertPendingVerificationPage(emailAddress);

    // Die Startkonfiguration dieses Fixtures verwendet ausdrücklich die Standardsprache Englisch.
    MailpitInbox mailpitInbox = new MailpitInbox(browserContext, null, new UiTexts("en"));
    try (Page activatedPage =
        mailpitInbox.openVerificationLink(mailpitInbox.awaitVerificationMail(emailAddress))) {
      new UserMenu(activatedPage).selectTestLanguage();
      assertOrdinaryLogin(activatedPage, emailAddress);
    }
  }

  @Tag("fast")
  @Test
  @DisplayName(
      "VER-01A: Echte Verifikationsmail aktiviert das Konto und ermöglicht normale Anmeldung")
  @Story("Registrierung bis zur ersten Anmeldung")
  @Description(
      """
      1. Ein eigenes Konto über das Registrierungsformular anlegen.
      2. Seine tatsächlich versandte Mail in Mailpit lesen; Empfänger, Betreff,
         Namen, Serviceangabe und ausgefüllte Platzhalter prüfen.
      3. Den sichtbaren Mailbutton anklicken und Aktivierungsbestätigung prüfen.
      4. Mit dem neuen Konto anmelden und die fehlende Adminaktion im Menü prüfen.

      **Erwartet:** Der Mailbutton führt ausschließlich in die isolierte Testanwendung.
      Die Verifikation gelingt; normale Anmeldung funktioniert ohne Adminrechte.
      Keine Bewertung der Darstellung in externen Mailprogrammen.
      """)
  void realMailAllowsActivationAndLogin() {
    String email = newVerificationEmailAddress();
    MailpitInbox inbox = new MailpitInbox(browserContext);
    Page activated =
        inbox.openVerificationLink(registerAccountAndAwaitVerificationMail(email, inbox));
    assertOrdinaryLogin(activated, email);
  }

  @Test
  @DisplayName("VER-01B: Verbrauchter Link zeigt beim weiterhin aktiven Konto erneut Erfolg")
  @Story("Erneutes Öffnen des Aktivierungslinks")
  @Description(
      """
      1. Ein eigenes Konto registrieren und über seinen echten Mailbutton aktivieren.
      2. Den bereits verwendeten Link vor einer Anmeldung erneut öffnen.
      3. Erneute Erfolgsanzeige und weiterhin mögliche normale Anmeldung prüfen.

      **Erwartet:** Nach dem bestehenden Vertrag bleibt der Erfolg beim aktiven Konto
      idempotent sichtbar. Eine nachträgliche Adminsperre wird in diesem Fall nicht geprüft.
      """)
  void usedLinkRemainsSuccessfulForActiveAccount() {
    String email = newVerificationEmailAddress();
    MailpitInbox inbox = new MailpitInbox(browserContext);
    Page activated =
        inbox.openVerificationLink(registerAccountAndAwaitVerificationMail(email, inbox));
    activated.reload();
    new VerificationPage(activated).assertActivated();
    assertOrdinaryLogin(activated, email);
  }

  @Test
  @DisplayName(
      "VER-02A: Unbekannter Token zeigt einen Hinweis, fehlender Token führt zur Startseite")
  @Story("Ungültiger oder fehlender Aktivierungslink")
  @Description(
      """
      1. /verification mit einem neuen, nicht vergebenen Token öffnen.
      2. Verständlichen Fehler, fehlende Erfolgsanzeige und sichtbares Mailformular prüfen.
      3. /verification ohne Token öffnen.

      **Erwartet:** Unbekannter Link zeigt keinen Aktivierungserfolg. Ohne Token erscheint
      die Startseite. Erneuter Versand, Ablauf und Adminsperren sind noch nicht abgedeckt.
      """)
  void invalidAndMissingTokensHaveDefinedUiBehavior() {
    browserPage.navigate(path(VERIFICATION_ROUTE) + "/" + UUID.randomUUID());
    new VerificationPage(browserPage).assertUnknownToken();
    browserPage.navigate(path(VERIFICATION_ROUTE));

    assertThat(browserPage).hasURL(urlPattern(HOME_ROUTE));
    assertThat(
            browserPage.getByText(
                "VaadinSpringApp (Browser-Test)", new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(browserPage).hasTitle(HOME_PAGE_TITLE);
  }

  private MailpitInbox.CapturedMail registerAccountAndAwaitVerificationMail(
      String emailAddress, MailpitInbox mailpitInbox) {
    RegistrationPage registrationPage = new RegistrationPage(browserPage);
    registrationPage.open();
    registrationPage.fillValid(emailAddress, BrowserTestAccounts.USER_PASSWORD);
    registrationPage.submit();
    registrationPage.assertSubmitted(emailAddress);

    return mailpitInbox.awaitVerificationMail(emailAddress);
  }

  private void assertOrdinaryLogin(Page targetPage, String emailAddress) {
    LoginPage loginPage = new LoginPage(targetPage);
    loginPage.open();
    loginPage.signIn(emailAddress, BrowserTestAccounts.USER_PASSWORD);

    assertThat(targetPage).hasURL(urlPattern(HOME_ROUTE));
    new UserMenu(targetPage).selectTestLanguage();
    new UserMenu(targetPage).assertUserSignedInWithoutAdminAction(emailAddress);
  }

  private static String newVerificationEmailAddress() {
    return "verification-" + UUID.randomUUID() + "@example.test";
  }
}
