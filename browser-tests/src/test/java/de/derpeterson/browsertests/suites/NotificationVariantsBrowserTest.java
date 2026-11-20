package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.NOTIFICATIONS_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ViewportSize;
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
import de.derpeterson.browsertests.pages.UiActions;
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
    assertErrorNotificationSurvivesViewportChanges();
    Locator closeButton =
        browserPage.locator("vaadin-notification-card:not([closing])").getByRole(AriaRole.BUTTON);
    closeButton.focus();
    assertThat(closeButton).isFocused();
    closeButton.press("Enter");
    assertThat(browserPage.locator("vaadin-notification-card")).hasCount(0);
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

  private void assertErrorNotificationSurvivesViewportChanges() {
    ViewportSize initialViewport = browserPage.viewportSize();
    Locator notice = browserPage.locator("vaadin-notification-card:not([closing])");
    Locator noticeTitle = notice.locator(".cosmos-toast-title");
    Locator noticeMessage = notice.locator(".cosmos-notification-message");
    String originalTitle = noticeTitle.innerText();
    String originalMessage = noticeMessage.innerText();
    try {
      for (int viewportWidth : new int[] {780, 750, 780}) {
        browserPage.setViewportSize(viewportWidth, 600);
        NotificationAppearanceChecks.assertPositionAndDuration(browserPage, -1);
        assertThat(notice).hasCount(1);
        assertThat(noticeTitle).hasText(originalTitle);
        assertThat(noticeMessage).hasText(originalMessage);
        assertThat(notice.getByRole(AriaRole.BUTTON)).isVisible();
        browserPage.waitForCondition(() -> notificationFitsViewport(notice, viewportWidth));
      }
    } finally {
      browserPage.setViewportSize(initialViewport.width, initialViewport.height);
    }
  }

  private boolean notificationFitsViewport(Locator notice, int viewportWidth) {
    BoundingBox overlayBounds = notice.locator("[part=overlay]").boundingBox();
    if (overlayBounds == null) {
      return false;
    }
    if (viewportWidth >= 768) {
      return Math.abs(viewportWidth - overlayBounds.x - overlayBounds.width - 24) < 2;
    }
    return Math.abs(overlayBounds.x + overlayBounds.width / 2 - viewportWidth / 2.0) < 2
        && overlayBounds.y + overlayBounds.height > 550
        && overlayBounds.y + overlayBounds.height <= 600;
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
        saveAndVerifySmtpEndpoint(
            configuration, "host.docker.internal", Integer.toString(smtp.port()));
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
        saveAndVerifySmtpEndpoint(configuration, previousHost, previousPort);
      }
    }
  }

  private void saveAndVerifySmtpEndpoint(
      AdminConfigurationPage configuration, String host, String port) {
    String hostLabel = uiTexts.getAdminConfigEntryMailHost();
    String portLabel = uiTexts.getAdminConfigEntryMailPort();
    configuration.enter(hostLabel, host);
    configuration.enter(portLabel, port);
    UiActions.complete(browserPage, configuration::submit);
    browserPage.reload();
    configuration.open(AdminConfigGroup.SMTP);

    assertThat(configuration.input(hostLabel)).hasValue(host);
    assertThat(configuration.input(portLabel)).hasValue(port);
  }

  private void signIn(int width) {
    LoginPage login = new LoginPage(browserPage);
    login.open();
    login.signIn(BrowserTestAccounts.SUPERADMIN_EMAIL, BrowserTestAccounts.SUPERADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
    browserPage.setViewportSize(width, width == 390 ? 844 : 900);
  }
}
