package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.BrowserContext;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.browsertests.BrowserActor;
import de.derpeterson.browsertests.BrowserFixture;
import de.derpeterson.browsertests.BrowserTestAccounts;
import de.derpeterson.browsertests.BrowserTestRunner;
import de.derpeterson.browsertests.mail.MailpitInbox;
import de.derpeterson.browsertests.pages.AdminMailPage;
import de.derpeterson.browsertests.pages.AdminUsersPage;
import de.derpeterson.browsertests.pages.ConfigurationPage;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("browser")
@Feature("Isolierter Wiederanlauf und SMTP-Betrieb")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperationsBrowserTest extends BrowserFixture {
  @Order(1)
  @Test
  @DisplayName("OPS-01: Gespeicherte Daten ohne Bootstrap und startgebundenes SMTP-Ziel")
  @Description(
      """
      1. Über UI eigenes Konto anlegen; Testmail erreicht das eigene Default-SMTP-Ziel A.
      2. SMTP-Ziel B und Dienstname speichern: ohne Neustart geht eine weitere Mail noch an A.
      3. Nur eigene Anwendung mit gleicher Datenbank und ohne Bootstrap neu starten.
      4. Adminrolle, Kontoanmeldung und gespeicherte Werte bleiben erhalten; neue Mail erreicht B.
      5. Originalwerte speichern und Anwendung nochmals ohne Bootstrap starten.
      **Erwartet:** Persistenz ohne erneute Anlage und SMTP-Bindung erst beim Start.
      """)
  void persistedConfigurationAndAccountsSurviveRestartWithoutBootstrap() throws Exception {
    ConfigurationPage settings = admin();
    AdminUsersPage users = new AdminUsersPage(page);
    users.open();
    String email = "restart-" + UUID.randomUUID() + "@example.test";
    users.createActiveUser(email, BrowserTestAccounts.USER_PASSWORD);
    settings.open(AdminConfigGroup.GENERAL);
    String nameLabel = texts.getAdminConfigEntryServiceName();
    String originalName = settings.value(nameLabel);
    settings.open(AdminConfigGroup.SMTP);
    String hostLabel = texts.getAdminConfigEntryMailHost();
    String originalHost = settings.value(hostLabel);
    try {
      sendAndAssertTarget(false);
      settings.open(AdminConfigGroup.SMTP);
      settings.enter(hostLabel, "mailpit-secondary");
      settings.save();
      sendAndAssertTarget(false);
      settings.open(AdminConfigGroup.GENERAL);
      settings.enter(nameLabel, "Restart proof");
      settings.save();
      BrowserTestRunner.restartTestApplication();
      page.navigate(
          de.derpeterson.browsertests.BrowserRoutes.path(
              de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE));
      new LoginPage(page).assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
      settings.open(AdminConfigGroup.GENERAL);
      assertThat(settings.input(nameLabel)).hasValue("Restart proof");
      settings.open(AdminConfigGroup.SMTP);
      assertThat(settings.input(hostLabel)).hasValue("mailpit-secondary");
      sendAndAssertTarget(true);
      try (BrowserActor actor = new BrowserActor(browser)) {
        actor.login(email, BrowserTestAccounts.USER_PASSWORD, false);
        new UserMenu(actor.page).assertUserSignedInWithoutAdminAction(email);
      }
    } finally {
      settings.open(AdminConfigGroup.GENERAL);
      settings.enter(nameLabel, originalName);
      settings.save();
      settings.open(AdminConfigGroup.SMTP);
      settings.enter(hostLabel, originalHost);
      settings.save();
      BrowserTestRunner.restartTestApplication();
      page.reload();
    }
  }

  @ParameterizedTest
  @Order(2)
  @ValueSource(booleans = {true, false})
  @DisplayName("OPS-02/03: Echter SMTP-Verbindungsfehler mit Wiederanlauf oder terminalem Ende")
  @Description(
      """
      1. Im eigenen Worker Retrylimit eins setzen und ausschließlich dessen Mailpit stoppen.
      2. Konto über Registrierung anlegen; Verbindungsfehler und Queue-ID im eigenen Log nachweisen.
      3. Wiederanlaufvariante: Mailpit starten, Versand abwarten und Aktivierungslink benutzen.
      4. Ausfallvariante: zweiten Versuch und terminalen Status nachweisen, dann App/Mailpit starten.
      5. Einen weiteren vollen Retryzeitraum beobachten: eine Zustellung bzw. keine terminale Mail.
      **Erwartet:** Begrenzte echte Transportversuche, keine doppelte oder terminal erneuerte Zustellung.
      """)
  void realConnectionFailureRecoversOnceOrRemainsTerminal(boolean recover) throws Exception {
    ConfigurationPage settings = admin();
    settings.open(AdminConfigGroup.QUEUE);
    String label = texts.getAdminConfigEntryEmailQueueMaxRetry();
    String original = settings.value(label);
    settings.enter(label, "1");
    settings.save();
    String email = "smtp-outage-" + UUID.randomUUID() + "@example.test";
    // Gruppe 1 enthält ausschließlich die numerische Queue-ID der festen Retry-Logzeile.
    Pattern retry = Pattern.compile("Email retry scheduled: queueId=(\\d+), attempt=1 of 2");
    java.util.Set<String> earlierQueues = new java.util.HashSet<>();
    Matcher earlier = retry.matcher(BrowserTestRunner.testApplicationLogs());
    while (earlier.find()) {
      earlierQueues.add(earlier.group(1));
    }
    try {
      new UserMenu(page).requestSignOut();
      BrowserTestRunner.setTestMailpitAvailable(false);
      RegistrationPage registration = new RegistrationPage(page);
      registration.open();
      registration.fillValid(email, BrowserTestAccounts.USER_PASSWORD);
      registration.submit();
      registration.assertSubmitted(email);
      AtomicReference<String> queue = new AtomicReference<>();
      awaitCondition(
          () -> {
            Matcher match = retry.matcher(BrowserTestRunner.testApplicationLogs());
            while (match.find()) {
              if (!earlierQueues.contains(match.group(1))) {
                queue.set(match.group(1));
              }
            }
            return queue.get() != null;
          },
          30_000);
      String queueId = queue.get();
      if (!recover) {
        awaitCondition(
            () ->
                BrowserTestRunner.testApplicationLogs()
                    .contains("Email permanently failed: queueId=" + queueId + ", attempts=2"),
            90_000);
      }
      BrowserTestRunner.setTestMailpitAvailable(true);
      if (recover) {
        awaitCondition(() -> countMessages(email, false) == 1, 90_000);
        MailpitInbox inbox = new MailpitInbox(context);
        try (com.microsoft.playwright.Page activation =
            inbox.openVerificationLink(inbox.awaitVerificationMail(email))) {
          assertTrue(activation.url().startsWith(System.getProperty("browser.tests.baseUrl")));
        }
      } else {
        BrowserTestRunner.restartTestApplication();
      }
      Instant retryPeriodEnds = Instant.now().plusSeconds(66);
      context.waitForCondition(
          () -> Instant.now().isAfter(retryPeriodEnds),
          new BrowserContext.WaitForConditionOptions().setTimeout(75_000));
      assertEquals(recover ? 1 : 0, countMessages(email, false));
    } finally {
      BrowserTestRunner.setTestMailpitAvailable(true);
      page.navigate(de.derpeterson.browsertests.BrowserRoutes.path(""));
      settings = admin();
      settings.open(AdminConfigGroup.QUEUE);
      settings.enter(label, original);
      settings.save();
    }
  }

  private void sendAndAssertTarget(boolean secondary) throws Exception {
    String email = "smtp-target-" + UUID.randomUUID() + "@example.test";
    String subject = "SMTP target " + UUID.randomUUID();
    AdminMailPage mail = new AdminMailPage(page);
    mail.open();
    mail.input(texts.getAdminMailRecipient()).fill(email);
    mail.input(texts.getAdminMailSubject()).fill(subject);
    mail.input(texts.getAdminMailBody()).fill("Own SMTP target proof");
    mail.send();
    mail.notice(texts.getAdminMailSuccess());
    awaitCondition(() -> countMessages(email, secondary) == 1, 15_000);
    assertEquals(0, countMessages(email, !secondary));
  }

  private int countMessages(String email, boolean secondary) throws Exception {
    int count = 0;
    for (JsonNode message : BrowserTestRunner.testMailpitMessages(secondary).path("messages")) {
      for (JsonNode recipient : message.path("To")) {
        if (email.equals(recipient.path("Address").asText())) {
          count++;
        }
      }
    }
    return count;
  }

  @FunctionalInterface
  private interface CheckedCondition {
    boolean ready() throws Exception;
  }

  private void awaitCondition(CheckedCondition condition, double timeout) {
    Instant[] nextCheck = {Instant.MIN};
    context.waitForCondition(
        () -> {
          if (Instant.now().isBefore(nextCheck[0])) {
            return false;
          }
          nextCheck[0] = Instant.now().plusSeconds(1);
          try {
            return condition.ready();
          } catch (Exception error) {
            throw new IllegalStateException("Owned fixture check failed", error);
          }
        },
        new BrowserContext.WaitForConditionOptions().setTimeout(timeout));
  }

  private ConfigurationPage admin() {
    LoginPage login = new LoginPage(page);
    login.open();
    login.signIn(BrowserTestAccounts.ADMIN_EMAIL, BrowserTestAccounts.ADMIN_PASSWORD);
    new UserMenu(page).selectTestLanguage();
    login.assertAdminSignedIn(BrowserTestAccounts.ADMIN_EMAIL);
    return new ConfigurationPage(page);
  }
}
