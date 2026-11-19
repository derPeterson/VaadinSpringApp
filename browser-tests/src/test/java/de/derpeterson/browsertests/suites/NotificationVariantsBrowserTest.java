package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.NOTIFICATIONS_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.mail.AmbiguousSmtpServer;
import de.derpeterson.browsertests.pages.AccountPage;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.NotificationAppearanceChecks;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Tag("notification-variants")
@Feature("Repräsentative Notification-Varianten")
public class NotificationVariantsBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  void requiredErrorAndTitleOnlyRefreshKeepTheirAppearance(int width) {
    signIn(width);
    AccountPage account = new AccountPage(browserPage);
    account.open();
    String firstName = account.input("account-first-name").inputValue();
    account.input("account-first-name").clear();
    account.save();
    account.assertMessage(uiTexts.getBaseValidationRequiredMessage());
    NotificationAppearanceChecks.capture(browserPage, "Fehler-dauerhaft", "error", -1);
    NotificationAppearanceChecks.close(browserPage);
    account.input("account-first-name").fill(firstName);
    account.save();
    account.assertMessage(uiTexts.getAccountSettingsSaveSuccess());
    account.closeNotification();
    browserPage.navigate(path(NOTIFICATIONS_ROUTE));
    browserPage
        .getByRole(
            AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(uiTexts.getNotificationsRefresh()).setExact(true))
        .click();
    assertThat(browserPage.locator("vaadin-notification-card"))
        .containsText(uiTexts.getNotificationsRefreshed());
    assertThat(browserPage.locator("vaadin-notification-card .cosmos-notification-message"))
        .hasCount(0);
    NotificationAppearanceChecks.capture(browserPage, "Nur-Titel", "success", 3000);
    NotificationAppearanceChecks.close(browserPage);
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  void restartWarningKeepsItsNativeListAndPermanentLifetime(int width) {
    signIn(width);
    AdminConfigurationPage configuration = new AdminConfigurationPage(browserPage);
    configuration.open(AdminConfigGroup.SECURITY);
    String keyLabel = uiTexts.getAdminConfigEntryRememberMeSecretKey();
    String originalKey = configuration.value(keyLabel);
    try {
      configuration.enter(keyLabel, "notification-restart-probe-" + width);
      configuration.submit();
      Locator notice = browserPage.locator("vaadin-notification-card:not([closing])");
      assertThat(notice).containsText(uiTexts.getAdminConfigRestartSaved());
      assertThat(notice.locator("ul.cosmos-notification-details > li")).hasText(keyLabel);
      assertThat(notice.locator("ul.cosmos-notification-details > li"))
          .hasCSS("font-weight", "700");
      NotificationAppearanceChecks.capture(browserPage, "Warnung-Aufzaehlung", "warning", 0);
    } finally {
      if (browserPage.locator("vaadin-notification-card:not([closing])").isVisible()) {
        NotificationAppearanceChecks.close(browserPage);
      }
      configuration.enter(keyLabel, originalKey);
      configuration.save();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1280, 390})
  void queuedCleanupKeepsItsInformationPresentation(int width) throws Exception {
    signIn(width);
    AdminConfigurationPage configuration = new AdminConfigurationPage(browserPage);
    configuration.open(AdminConfigGroup.SMTP);
    String previousHost = configuration.value(uiTexts.getAdminConfigEntryMailHost());
    String previousPort = configuration.value(uiTexts.getAdminConfigEntryMailPort());
    try (AmbiguousSmtpServer smtp = new AmbiguousSmtpServer()) {
      try {
        configuration.enter(uiTexts.getAdminConfigEntryMailHost(), "host.docker.internal");
        configuration.enter(uiTexts.getAdminConfigEntryMailPort(), Integer.toString(smtp.port()));
        configuration.save();
        configuration.open(AdminConfigGroup.QUEUE);
        browserPage
            .locator("#admin-queue-tabs")
            .getByRole(
                AriaRole.TAB,
                new Locator.GetByRoleOptions()
                    .setName(uiTexts.getAdminQueueCleanupAssistantsTab())
                    .setExact(true))
            .click();
        smtp.holdFollowingResponses();
        try (BrowserActor registeringActor = new BrowserActor(testBrowser)) {
          RegistrationPage registration = new RegistrationPage(registeringActor.browserPage);
          registration.open();
          String email = "notification-waiting-" + width + "@example.test";
          registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
          registration.submit();
          registration.assertSubmitted(email);
        }
        browserPage.waitForCondition(
            smtp::isHoldingResponse, new Page.WaitForConditionOptions().setTimeout(30_000));
        browserPage.locator("#admin-mail-queue-cleanup").click();
        browserPage.locator("#admin-mail-queue-confirm-submit").click();
        assertThat(browserPage.locator("#admin-mail-queue-status"))
            .hasAttribute("data-status", "WAITING");
        assertThat(browserPage.locator("vaadin-notification-card"))
            .containsText(uiTexts.getAdminQueueCleanupWaitingExplanation());
        NotificationAppearanceChecks.capture(browserPage, "Information", "normal", 3000);
        NotificationAppearanceChecks.close(browserPage);
      } finally {
        smtp.releaseHeldResponses();
        configuration.open(AdminConfigGroup.SMTP);
        configuration.enter(uiTexts.getAdminConfigEntryMailHost(), previousHost);
        configuration.enter(uiTexts.getAdminConfigEntryMailPort(), previousPort);
        configuration.save();
      }
    }
  }

  private void signIn(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(width, width == 390 ? 844 : 900);
  }
}
