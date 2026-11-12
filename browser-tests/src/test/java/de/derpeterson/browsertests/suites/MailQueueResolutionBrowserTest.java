package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.RequestOptions;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.mail.AmbiguousSmtpServer;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminConfigurationPage;
import de.derpeterson.browsertests.pages.AdminUserDialog;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Ungeklärte Mailaufträge")
public class MailQueueResolutionBrowserTest extends BrowserFixture {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @DisplayName(
      "QUEUE-01: Ungeklärten Versand manuell oder automatisch ohne Wiederholung abschließen")
  @Description(
      """
      1. Nur Superadmins sehen Bereinigungsfelder und Aktion; Feldhilfen in beiden Breiten prüfen.
      2. Eigene Registrierung an einen eigenen SMTP-Testdienst: nach DATA bricht die Antwort ab.
      3. Bestätigung abbrechen: kein Abschluss. Die echte Mindestfrist bleibt wirksam.
      4. Nach Mindestalter manuell bzw. automatisch bereinigen; Ergebnis und genau einen Versuch prüfen.
      5. Einen neuen Link ausdrücklich anfordern, in Mailpit empfangen und als Empfänger benutzen.
      Erwartet: Gespeicherte Zeitwerte und aktive Versandgrenzen bleiben erhalten. Beide Darstellungen
      verwenden native Komponenten. Kein Datenbankeingriff und kein automatischer erneuter Versand.
      """)
  void unresolvedRequestNeedsExplicitResolutionBeforeAnotherVerificationMail(boolean automatic)
      throws Exception {
    signInAdministrator(false);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.QUEUE);
    assertThat(browserPage.locator("#admin-mail-queue-resolution")).hasCount(0);
    assertThat(
            browserPage.getByLabel(
                uiTexts.getAdminQueueCleanupEnabled(), new Page.GetByLabelOptions().setExact(true)))
        .hasCount(0);
    new UserMenu(browserPage).requestSignOut();

    signInAdministrator(true);
    configurationPage.open(AdminConfigGroup.QUEUE);
    BigDecimal previousMinimumAge =
        configurationPage.seconds(uiTexts.getAdminQueueCleanupMinimumAge());
    BigDecimal previousInterval = configurationPage.seconds(uiTexts.getAdminQueueCleanupInterval());
    Locator automaticSwitch = cleanupSwitch();
    boolean previousAutomatic = automaticSwitch.isChecked();
    automaticSwitch.setChecked(false);
    configurationPage.enterSeconds(
        uiTexts.getAdminQueueCleanupMinimumAge(), BigDecimal.valueOf(60));
    configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupInterval(), BigDecimal.valueOf(60));
    configurationPage.save();

    String recipientEmailAddress = "queue-resolution-" + UUID.randomUUID() + "@example.test";
    try {
      for (int viewportWidth : new int[] {1280, 390}) {
        browserPage.setViewportSize(viewportWidth, 1000);
        configurationPage.explain(
            uiTexts.getAdminQueueCleanupMinimumAge(),
            uiTexts.getAdminQueueCleanupMinimumAgeHelp(),
            uiTexts.getAdminQueueCleanupMinimumRule());
        configurationPage.explain(
            uiTexts.getAdminQueueCleanupInterval(),
            uiTexts.getAdminQueueCleanupIntervalHelp(),
            uiTexts.getAdminQueueCleanupMinimumRule());
        assertThat(browserPage.locator("vaadin-dialog-overlay:visible")).hasCount(0);
        assertThat(browserPage.locator("#admin-mail-queue-cleanup")).isVisible();
        AllurePlaywright.attachScreenshot("Bereinigung " + viewportWidth, browserPage);
      }
      Instant claimObservedAt;
      long previousUnknownCount = logCount("is unknown; IN_PROGRESS retained.");
      try (AmbiguousSmtpServer smtpServer = new AmbiguousSmtpServer()) {
        configurationPage.open(AdminConfigGroup.SMTP);
        String previousHost = configurationPage.value(uiTexts.getAdminConfigEntryMailHost());
        String previousPort = configurationPage.value(uiTexts.getAdminConfigEntryMailPort());
        try {
          configurationPage.enter(uiTexts.getAdminConfigEntryMailHost(), "host.docker.internal");
          configurationPage.enter(
              uiTexts.getAdminConfigEntryMailPort(), Integer.toString(smtpServer.port()));
          configurationPage.save();
          registerOwnAccount(recipientEmailAddress);
          awaitNewLog("is unknown; IN_PROGRESS retained.", previousUnknownCount);
          assertEquals(1, smtpServer.receivedMessageCount());
          claimObservedAt = Instant.now();
        } finally {
          configurationPage.open(AdminConfigGroup.SMTP);
          configurationPage.enter(uiTexts.getAdminConfigEntryMailHost(), previousHost);
          configurationPage.enter(uiTexts.getAdminConfigEntryMailPort(), previousPort);
          configurationPage.save();
        }
        configurationPage.open(AdminConfigGroup.QUEUE);
        browserPage.locator("#admin-mail-queue-cleanup").click();
        assertThat(browserPage.locator("#admin-mail-queue-confirm[opened]"))
            .containsText(uiTexts.getAdminQueueResolutionConsequence());
        AllurePlaywright.attachScreenshot("Bereinigung bestätigen", browserPage);
        browserPage.locator("#admin-mail-queue-confirm-cancel").click();

        browserPage.locator("#admin-mail-queue-cleanup").click();
        browserPage.locator("#admin-mail-queue-confirm-submit").click();
        String resultFormat = uiTexts.getAdminQueueCleanupResult();
        String emptyResultSuffix =
            MessageFormat.format(resultFormat.substring(resultFormat.indexOf("{0}") + 3), "", 0);
        assertThat(browserPage.locator("#admin-mail-queue-result")).containsText(emptyResultSuffix);

        if (automatic) {
          String automaticCompletionLog =
              "Unresolved mail queue entries closed without resending: count=1, automatic=true";
          long previousAutomaticCount = logCount(automaticCompletionLog);
          cleanupSwitch().setChecked(true);
          configurationPage.save();
          awaitNewLog(automaticCompletionLog, previousAutomaticCount);
        } else {
          // Der konfigurierte Mindestabstand wird tatsächlich abgewartet, nicht künstlich verkürzt.
          browserPage.waitForCondition(
              () -> Instant.now().isAfter(claimObservedAt.plusSeconds(61)),
              new Page.WaitForConditionOptions().setTimeout(65_000));
          browserPage.locator("#admin-mail-queue-cleanup").click();
          browserPage.locator("#admin-mail-queue-confirm-submit").click();
        }
        browserPage.locator("#admin-mail-queue-refresh").click();
        String expectedSuffix =
            MessageFormat.format(resultFormat.substring(resultFormat.indexOf("{0}") + 3), "", 1);
        assertThat(browserPage.locator("#admin-mail-queue-result")).containsText(expectedSuffix);
        assertEquals(1, smtpServer.receivedMessageCount(), "Bereinigung darf nicht erneut senden");
        new MailpitInbox(browserContext).assertNoMailFor(recipientEmailAddress);
      }

      AdminUsersPage adminUsersPage = new AdminUsersPage(browserPage);
      adminUsersPage.open();
      AdminUserDialog adminUserDialog = adminUsersPage.openEdit(recipientEmailAddress);
      adminUserDialog.openSignInDetails();
      adminUserDialog.root().locator("#admin-resend-verification-mail").click();
      MailpitInbox.CapturedMail verificationMail =
          new MailpitInbox(browserContext).awaitVerificationMail(recipientEmailAddress);
      adminUserDialog.cancel();
      try (BrowserActor verifyingActor = new BrowserActor(testBrowser);
          Page verificationPage =
              new MailpitInbox(verifyingActor.browserContext)
                  .openVerificationLink(verificationMail)) {
        assertThat(verificationPage.locator(".public-panel")).isVisible();
      }
    } finally {
      configurationPage.open(AdminConfigGroup.QUEUE);
      cleanupSwitch().setChecked(previousAutomatic);
      configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupMinimumAge(), previousMinimumAge);
      configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupInterval(), previousInterval);
      configurationPage.save();
    }
  }

  @Test
  @DisplayName("QUEUE-02: Explizite SMTP-Ablehnung bleibt nicht als ungeklärter Auftrag hängen")
  void explicitSmtpRejectionReachesItsRetryLimitInsteadOfQuarantine() {
    signInAdministrator(true);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.QUEUE);
    String previousRetries =
        configurationPage.value(uiTexts.getAdminConfigEntryEmailQueueMaxRetry());
    configurationPage.enter(uiTexts.getAdminConfigEntryEmailQueueMaxRetry(), "0");
    configurationPage.save();
    long previousTerminalCount = logCount("Email permanently failed:");
    long previousUnknownCount = logCount("is unknown; IN_PROGRESS retained.");
    try {
      setOwnMailpitSenderFailure(true);
      registerOwnAccount("smtp-rejection-" + UUID.randomUUID() + "@example.test");
      awaitNewLog("Email permanently failed:", previousTerminalCount);
      assertEquals(previousUnknownCount, logCount("is unknown; IN_PROGRESS retained."));
    } finally {
      setOwnMailpitSenderFailure(false);
      configurationPage.enter(uiTexts.getAdminConfigEntryEmailQueueMaxRetry(), previousRetries);
      configurationPage.save();
    }
  }

  private Locator cleanupSwitch() {
    return browserPage.getByRole(
        AriaRole.SWITCH,
        new Page.GetByRoleOptions().setName(uiTexts.getAdminQueueCleanupEnabled()).setExact(true));
  }

  private void registerOwnAccount(String emailAddress) {
    try (BrowserActor registeringActor = new BrowserActor(testBrowser)) {
      RegistrationPage registrationPage = new RegistrationPage(registeringActor.browserPage);
      registrationPage.open();
      registrationPage.fillValid(emailAddress, BrowserTestAccounts.USER_PASSWORD);
      registrationPage.submit();
      registrationPage.assertSubmitted(emailAddress);
    }
  }

  private void signInAdministrator(boolean superadmin) {
    LoginPage loginPage = new LoginPage(browserPage);
    loginPage.open();
    loginPage.signIn(
        superadmin ? BrowserTestAccounts.SUPERADMIN_EMAIL : BrowserTestAccounts.ADMIN_EMAIL,
        superadmin ? BrowserTestAccounts.SUPERADMIN_PASSWORD : BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(browserPage).selectTestLanguage();
  }

  private void setOwnMailpitSenderFailure(boolean enabled) {
    Map<String, ?> chaosTriggers =
        enabled ? Map.of("Sender", Map.of("ErrorCode", 451, "Probability", 100)) : Map.of();
    APIResponse chaosResponse =
        browserContext
            .request()
            .put(
                System.getProperty("browser.tests.mailpitUrl") + "/api/v1/chaos",
                RequestOptions.create().setData(chaosTriggers));
    try {
      assertEquals(200, chaosResponse.status());
    } finally {
      chaosResponse.dispose();
    }
  }

  private long logCount(String logFragment) {
    try {
      return BrowserTestRunner.testApplicationLogs()
          .lines()
          .filter(line -> line.contains(logFragment))
          .count();
    } catch (Exception logReadFailure) {
      throw new IllegalStateException(
          "Own test application's dispatch result could not be read", logReadFailure);
    }
  }

  private void awaitNewLog(String logFragment, long previousCount) {
    Instant[] nextLogRead = {Instant.MIN};
    browserPage.waitForCondition(
        () -> {
          if (Instant.now().isBefore(nextLogRead[0])) {
            return false;
          }
          nextLogRead[0] = Instant.now().plusSeconds(1);
          return logCount(logFragment) > previousCount;
        },
        new Page.WaitForConditionOptions().setTimeout(140_000));
    assertTrue(logCount(logFragment) > previousCount);
  }
}
