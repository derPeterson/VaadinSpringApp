package de.derpeterson.browsertests.suites;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.ADMIN_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitUntilState;
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
import de.derpeterson.browsertests.pages.ConfirmationAppearanceChecks;
import de.derpeterson.browsertests.pages.LoginPage;
import de.derpeterson.browsertests.pages.RegistrationPage;
import de.derpeterson.browsertests.pages.UiActions;
import de.derpeterson.browsertests.pages.UserMenu;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.playwright.AllurePlaywright;
import java.math.BigDecimal;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
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
  @ValueSource(ints = {1280, 390})
  @Tag("native-confirmations")
  @DisplayName("QUEUE-05: Manuelle Bestätigung bleibt mit Warnung und Abbruch bedienbar")
  void manualCleanupConfirmationKeepsItsLayoutAndCanBeCancelled(int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, viewportWidth == 390 ? 844 : 900);
    signInAdministrator(true);
    new AdminConfigurationPage(browserPage).open(AdminConfigGroup.QUEUE);
    selectCleanupTabAndGetAutomaticSwitch();
    String originalResult = browserPage.locator("#admin-mail-queue-result").textContent();
    for (String dismissal : new String[] {"cancel", "header", "escape", "outside"}) {
      browserPage.locator("#admin-mail-queue-cleanup").click();
      Locator confirmationDialogLocator = browserPage.locator("#admin-mail-queue-confirm[opened]");
      assertThat(confirmationDialogLocator.locator(".cosmos-security-warning")).isVisible();
      ConfirmationAppearanceChecks.capture(
          browserPage, confirmationDialogLocator, "manual-mail-cleanup");
      assertThat(confirmationDialogLocator.locator(".cosmos-overlay-close")).isFocused();
      UiActions.complete(
          browserPage,
          () -> {
            switch (dismissal) {
              case "cancel" -> browserPage.locator("#admin-mail-queue-confirm-cancel").click();
              case "header" -> confirmationDialogLocator.locator(".cosmos-overlay-close").click();
              case "escape" -> browserPage.keyboard().press("Escape");
              case "outside" -> browserPage.mouse().click(2, 2);
              default -> throw new IllegalArgumentException("Unknown dismissal fixture");
            }
          });
      assertThat(confirmationDialogLocator).hasCount(0);
      assertThat(browserPage.locator("#admin-mail-queue-cleanup")).isEnabled();
      assertThat(browserPage.locator("#admin-mail-queue-result")).hasText(originalResult);
    }
  }

  @Test
  @Tag("queue-full-run")
  @DisplayName(
      "QUEUE-06: Mehr als 100 Aufträge warten auf SMTP und werden ohne zweiten Klick vollständig bereinigt")
  void fullCleanupWaitsForTransportAndAggregatesMoreThanOneBatch() throws Exception {
    signInAdministrator(true);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.QUEUE);
    BigDecimal previousMinimumAge =
        configurationPage.seconds(uiTexts.getAdminQueueCleanupMinimumAge());
    BigDecimal previousInterval = configurationPage.seconds(uiTexts.getAdminQueueCleanupInterval());
    boolean previousAutomatic = selectCleanupTabAndGetAutomaticSwitch().isChecked();
    selectCleanupTabAndGetAutomaticSwitch().setChecked(true);
    configurationPage.enterSeconds(
        uiTexts.getAdminQueueCleanupMinimumAge(), BigDecimal.valueOf(60));
    configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupInterval(), BigDecimal.valueOf(60));
    selectCleanupTabAndGetAutomaticSwitch().setChecked(false);
    configurationPage.save();
    captureCleanupStateAtBothWidths("Bereinigung vor dem Gesamtlauf");

    configurationPage.open(AdminConfigGroup.SMTP);
    String previousHost = configurationPage.value(uiTexts.getAdminConfigEntryMailHost());
    String previousPort = configurationPage.value(uiTexts.getAdminConfigEntryMailPort());
    String accountPrefix = "full-cleanup-" + UUID.randomUUID();
    String lastRecipient = accountPrefix + "-100@example.test";
    String heldRecipient = "held-cleanup-" + UUID.randomUUID() + "@example.test";
    try (AmbiguousSmtpServer smtpServer = new AmbiguousSmtpServer()) {
      try {
        configurationPage.enter(uiTexts.getAdminConfigEntryMailHost(), "host.docker.internal");
        configurationPage.enter(
            uiTexts.getAdminConfigEntryMailPort(), Integer.toString(smtpServer.port()));
        configurationPage.save();
        AdminUsersPage usersPage = new AdminUsersPage(browserPage);
        usersPage.open();
        for (int accountIndex = 0; accountIndex < 101; accountIndex++) {
          AdminUserDialog createDialog = usersPage.openCreate();
          createDialog.fillNewUser(
              accountPrefix + "-" + accountIndex + "@example.test",
              BrowserTestAccounts.USER_PASSWORD,
              true,
              false);
          createDialog.save();
        }
        browserPage.waitForCondition(
            () -> smtpServer.receivedMessageCount() >= 101,
            new Page.WaitForConditionOptions().setTimeout(180_000));
        assertEquals(101, smtpServer.receivedMessageCount());
        Instant lastClaimObservedAt = Instant.now();
        configurationPage.open(AdminConfigGroup.QUEUE);
        selectCleanupTabAndGetAutomaticSwitch();
        browserPage.waitForCondition(
            () -> Instant.now().isAfter(lastClaimObservedAt.plusSeconds(61)),
            new Page.WaitForConditionOptions().setTimeout(65_000));

        smtpServer.holdFollowingResponses();
        registerOwnAccount(heldRecipient);
        browserPage.waitForCondition(
            smtpServer::isHoldingResponse, new Page.WaitForConditionOptions().setTimeout(30_000));
        browserPage.setViewportSize(390, 1000);
        browserPage.locator("#admin-mail-queue-cleanup").click();
        Locator confirmation = browserPage.locator("#admin-mail-queue-confirm[opened]");
        assertThat(confirmation).not().containsText("100");
        browserPage.locator("#admin-mail-queue-confirm-submit").click();
        assertThat(browserPage.locator("#admin-mail-queue-status"))
            .hasAttribute("data-status", "WAITING");
        assertThat(browserPage.locator("#admin-mail-queue-cleanup")).isDisabled();
        assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
            .containsText(uiTexts.getAdminQueueCleanupWaitingExplanation());
        captureCleanupStateAtBothWidths("Wartet auf Ausführung bei echtem SMTP");
        smtpServer.releaseHeldResponses();

        assertThat(browserPage.locator("#admin-mail-queue-status"))
            .hasAttribute(
                "data-status",
                "COMPLETED",
                new LocatorAssertions.HasAttributeOptions().setTimeout(30_000));
        String resultFormat = uiTexts.getAdminQueueCleanupResult();
        String expectedSuffix =
            MessageFormat.format(resultFormat.substring(resultFormat.indexOf("{0}") + 3), "", 101);
        assertThat(browserPage.locator("#admin-mail-queue-result")).containsText(expectedSuffix);
        assertThat(browserPage.locator("#admin-mail-queue-status-explanation"))
            .hasText(uiTexts.getAdminQueueCleanupSuccessExplanation());
        assertThat(browserPage.locator("vaadin-notification-card:not([closing])"))
            .containsText(MessageFormat.format(uiTexts.getAdminQueueCleanupCompleted(), 101));
        assertEquals(
            102,
            smtpServer.receivedMessageCount(),
            "Ein Gesamtlauf darf keinen Versand wiederholen");
        captureCleanupStateAtBothWidths("Gesamtlauf mit 101 bereinigten Aufträgen");
        assertCleanupCountStartsOnNewLine();
      } finally {
        smtpServer.releaseHeldResponses();
        configurationPage.open(AdminConfigGroup.SMTP);
        configurationPage.enter(uiTexts.getAdminConfigEntryMailHost(), previousHost);
        configurationPage.enter(uiTexts.getAdminConfigEntryMailPort(), previousPort);
        configurationPage.save();
      }

      // Der absichtlich junge Halteauftrag gehört nicht zum eingefrorenen Gesamtlauf.
      // Sein wegwerfbares Konto über die echte Verwaltung löschen, damit er spätere Fälle nicht
      // beeinflusst.
      AdminUsersPage usersPage = new AdminUsersPage(browserPage);
      usersPage.open();
      usersPage.openEdit(heldRecipient).requestDelete().confirm();
      new MailpitInbox(browserContext).assertNoMailFor(lastRecipient);
      AdminUserDialog lastAccount = usersPage.openEdit(lastRecipient);
      lastAccount.openSignInDetails();
      lastAccount.root().locator("#admin-resend-verification-mail").click();
      new MailpitInbox(browserContext).awaitVerificationMail(lastRecipient);
      lastAccount.cancel();
    } finally {
      configurationPage.open(AdminConfigGroup.QUEUE);
      selectCleanupTabAndGetAutomaticSwitch().setChecked(true);
      configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupMinimumAge(), previousMinimumAge);
      configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupInterval(), previousInterval);
      selectCleanupTabAndGetAutomaticSwitch().setChecked(previousAutomatic);
      configurationPage.save();
    }
  }

  private void captureCleanupStateAtBothWidths(String evidenceName) {
    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 1000);
      browserPage.locator("#admin-mail-queue-resolution").scrollIntoViewIfNeeded();
      assertThat(browserPage.locator("#admin-mail-queue-status")).isVisible();
      assertTrue(
          (boolean) browserPage.evaluate("document.documentElement.scrollWidth <= innerWidth"));
      AllurePlaywright.attachScreenshot(evidenceName + " " + viewportWidth, browserPage);
    }
  }

  @Test
  @DisplayName(
      "QUEUE-07: Ein wartender Auftrag wird nach Rechteentzug ehrlich als fehlgeschlagen angezeigt")
  void revokedWaitingCleanupShowsFailureWithoutClaimingSuccess() throws Exception {
    signInAdministrator(true);
    String requestingAdministrator = "cleanup-actor-" + UUID.randomUUID() + "@example.test";
    String heldRecipient = "cleanup-held-" + UUID.randomUUID() + "@example.test";
    AdminUsersPage usersPage = new AdminUsersPage(browserPage);
    usersPage.createUser(requestingAdministrator, BrowserTestAccounts.USER_PASSWORD, true, true);
    AdminUserDialog actorDialog = usersPage.openEdit(requestingAdministrator);
    actorDialog.setRole(uiTexts.getRoleSuperadmin(), true);
    actorDialog.save();

    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.SMTP);
    String previousHost = configurationPage.value(uiTexts.getAdminConfigEntryMailHost());
    String previousPort = configurationPage.value(uiTexts.getAdminConfigEntryMailPort());
    try (BrowserActor requestingActor = new BrowserActor(testBrowser);
        AmbiguousSmtpServer smtpServer = new AmbiguousSmtpServer()) {
      LoginPage actorLogin = new LoginPage(requestingActor.browserPage);
      actorLogin.open();
      actorLogin.signIn(requestingAdministrator, BrowserTestAccounts.USER_PASSWORD);
      new UserMenu(requestingActor.browserPage).selectTestLanguage();
      AdminConfigurationPage actorConfiguration =
          new AdminConfigurationPage(requestingActor.browserPage);
      actorConfiguration.open(AdminConfigGroup.QUEUE);
      selectCleanupTabAndGetAutomaticSwitch(requestingActor.browserPage);
      try {
        configurationPage.enter(uiTexts.getAdminConfigEntryMailHost(), "host.docker.internal");
        configurationPage.enter(
            uiTexts.getAdminConfigEntryMailPort(), Integer.toString(smtpServer.port()));
        configurationPage.save();
        smtpServer.holdFollowingResponses();
        registerOwnAccount(heldRecipient);
        browserPage.waitForCondition(
            smtpServer::isHoldingResponse, new Page.WaitForConditionOptions().setTimeout(30_000));
        requestingActor.browserPage.locator("#admin-mail-queue-cleanup").click();
        requestingActor.browserPage.locator("#admin-mail-queue-confirm-submit").click();
        assertThat(requestingActor.browserPage.locator("#admin-mail-queue-status"))
            .hasAttribute("data-status", "WAITING");

        usersPage.open();
        actorDialog = usersPage.openEdit(requestingAdministrator);
        actorDialog.setRole(uiTexts.getRoleSuperadmin(), false);
        actorDialog.save();
        smtpServer.releaseHeldResponses();
        configurationPage.open(AdminConfigGroup.QUEUE);
        selectCleanupTabAndGetAutomaticSwitch();
        assertThat(browserPage.locator("#admin-mail-queue-status"))
            .hasAttribute(
                "data-status",
                "FAILED",
                new LocatorAssertions.HasAttributeOptions().setTimeout(30_000));
        assertThat(browserPage.locator("#admin-mail-queue-status-explanation"))
            .hasText(uiTexts.getAdminQueueCleanupFailureExplanation());
        captureCleanupStateAtBothWidths("Fehlgeschlagener Lauf nach Rechteentzug");
      } finally {
        smtpServer.releaseHeldResponses();
        configurationPage.open(AdminConfigGroup.SMTP);
        configurationPage.enter(uiTexts.getAdminConfigEntryMailHost(), previousHost);
        configurationPage.enter(uiTexts.getAdminConfigEntryMailPort(), previousPort);
        configurationPage.save();
      }
      usersPage.open();
      usersPage.openEdit(heldRecipient).requestDelete().confirm();
      usersPage.openEdit(requestingAdministrator).requestDelete().confirm();
    }
  }

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
    Locator automaticSwitch = selectCleanupTabAndGetAutomaticSwitch();
    boolean previousAutomatic = automaticSwitch.isChecked();
    automaticSwitch.setChecked(true);
    configurationPage.enterSeconds(
        uiTexts.getAdminQueueCleanupMinimumAge(), BigDecimal.valueOf(60));
    configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupInterval(), BigDecimal.valueOf(60));
    automaticSwitch.setChecked(false);
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
          browserPage.waitForCondition(
              () -> smtpServer.receivedMessageCount() == 1,
              new Page.WaitForConditionOptions().setTimeout(30_000));
          assertEquals(1, smtpServer.receivedMessageCount());
          claimObservedAt = Instant.now();
        } finally {
          configurationPage.open(AdminConfigGroup.SMTP);
          configurationPage.enter(uiTexts.getAdminConfigEntryMailHost(), previousHost);
          configurationPage.enter(uiTexts.getAdminConfigEntryMailPort(), previousPort);
          configurationPage.save();
        }
        configurationPage.open(AdminConfigGroup.QUEUE);
        selectCleanupTabAndGetAutomaticSwitch();
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
        assertCleanupCountStartsOnNewLine();

        if (automatic) {
          String automaticCompletionLog =
              "Mail queue cleanup completed without resending: count=1, automatic=true";
          long previousAutomaticCount = logCount(automaticCompletionLog);
          selectCleanupTabAndGetAutomaticSwitch().setChecked(true);
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
      selectCleanupTabAndGetAutomaticSwitch().setChecked(true);
      configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupMinimumAge(), previousMinimumAge);
      configurationPage.enterSeconds(uiTexts.getAdminQueueCleanupInterval(), previousInterval);
      selectCleanupTabAndGetAutomaticSwitch().setChecked(previousAutomatic);
      configurationPage.save();
    }
  }

  private void assertCleanupCountStartsOnNewLine() {
    Locator cleanupResultLocator = browserPage.locator("#admin-mail-queue-result");
    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 1000);
      cleanupResultLocator.scrollIntoViewIfNeeded();
      assertThat(cleanupResultLocator).hasCSS("white-space", "pre-line");

      // Textbereiche messen, ohne Auswahl, Inhalt oder Layout im Browser zu verändern.
      boolean countStartsBelowTimestamp =
          (boolean)
              cleanupResultLocator.evaluate(
                  """
                  element => {
                    const resultTextNode = element.firstChild;
                    if (!resultTextNode || resultTextNode.nodeType !== Node.TEXT_NODE) {
                      return false;
                    }
                    const lineBreakIndex = resultTextNode.textContent.indexOf('\\n');
                    if (lineBreakIndex < 0) {
                      return false;
                    }
                    const timestampRange = document.createRange();
                    timestampRange.setStart(resultTextNode, 0);
                    timestampRange.setEnd(resultTextNode, lineBreakIndex);
                    const countRange = document.createRange();
                    countRange.setStart(resultTextNode, lineBreakIndex + 1);
                    countRange.setEnd(resultTextNode, resultTextNode.textContent.length);
                    const timestampBounds = timestampRange.getBoundingClientRect();
                    const countBounds = countRange.getBoundingClientRect();
                    return countBounds.top >= timestampBounds.bottom - 0.5
                        && Math.abs(countBounds.left - timestampBounds.left) <= 0.5;
                  }
                  """);
      assertTrue(
          countStartsBelowTimestamp,
          "Die Anzahl bereinigter Aufträge muss unter dem Laufzeitpunkt am gleichen Rand beginnen");
      AllurePlaywright.attachScreenshot(
          "Zweizeiliges Bereinigungsergebnis " + viewportWidth, browserPage);
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

  @Test
  @DisplayName("QUEUE-03: Queue-Reiter, Entwürfe und Bereinigungskarte auf Desktop und Smartphone")
  @Description(
      "Allgemeine Felder bleiben für Admins erreichbar; nur Superadmins sehen Assistenten. Entwürfe, Switchabhängigkeiten, Historienfrist, Laden und gemeinsame Speicherung in beiden Breiten prüfen.")
  void queueTabsKeepDraftsAndSeparateGeneralSettingsFromSuperadminAssistants() {
    signInAdministrator(false);
    AdminConfigurationPage configurationPage = new AdminConfigurationPage(browserPage);
    configurationPage.open(AdminConfigGroup.QUEUE);
    Locator queueTabsLocator = browserPage.locator("#admin-queue-tabs");
    assertThat(queueTabsLocator.getByRole(AriaRole.TAB)).hasCount(1);
    assertThat(browserPage.locator(".cosmos-config-field:visible")).hasCount(5);
    assertThat(browserPage.locator(".cosmos-config-setting-count"))
        .hasText(uiTexts.getAdminConfigSettingsCount(5));
    new UserMenu(browserPage).requestSignOut();

    signInAdministrator(true);
    configurationPage.open(AdminConfigGroup.QUEUE);
    String originalHistoryDaysText =
        configurationPage.value(uiTexts.getAdminQueueCleanupHistoryDays());
    String originalRetryCountText =
        configurationPage.value(uiTexts.getAdminConfigEntryEmailQueueMaxRetry());
    boolean wasAutomaticCleanupEnabled = selectCleanupTabAndGetAutomaticSwitch().isChecked();
    try {
      for (int viewportWidth : new int[] {1280, 390}) {
        verifyQueueDraftsAndAssistantLayout(configurationPage, viewportWidth);
      }
    } finally {
      configurationPage.enter(uiTexts.getAdminQueueCleanupHistoryDays(), originalHistoryDaysText);
      configurationPage.enter(
          uiTexts.getAdminConfigEntryEmailQueueMaxRetry(), originalRetryCountText);
      selectCleanupTabAndGetAutomaticSwitch().setChecked(wasAutomaticCleanupEnabled);
      configurationPage.save();
    }
  }

  @Test
  @DisplayName("QUEUE-08: Reiter bleiben über Direktlink und F5 erhalten und beachten die Rolle")
  void queueTabUrlsSurviveReloadAndRejectUnauthorizedAssistantAccess() {
    String queuePath = path(ADMIN_ROUTE) + "?section=configuration&group=queue";
    signInAdministrator(false);
    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 1000);
      browserPage.navigate(
          queuePath + "&tab=assistants",
          new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
      assertSelectedQueueTab(uiTexts.getAdminQueueCleanupGeneralTab(), 5);
      assertThat(browserPage.locator("#admin-queue-tabs").getByRole(AriaRole.TAB)).hasCount(1);
      assertThat(browserPage.locator("#admin-mail-queue-resolution")).hasCount(0);
    }
    new UserMenu(browserPage).requestSignOut();
    signInAdministrator(true);
    for (int viewportWidth : new int[] {1280, 390}) {
      browserPage.setViewportSize(viewportWidth, 1000);
      new AdminConfigurationPage(browserPage).open(AdminConfigGroup.QUEUE);
      selectCleanupTabAndGetAutomaticSwitch();
      // Den eigenen Queryparameter unabhängig von seiner Reihenfolge nach dem Serverereignis
      // abwarten.
      browserPage.waitForURL(url -> {
        String query = URI.create(url).getQuery();
        return query != null && List.of(query.split("&")).contains("tab=assistants");
      });
      String assistantUrl = browserPage.url();
      assertTrue(assistantUrl.contains("tab=assistants"));
      browserPage.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
      assertSelectedQueueTab(uiTexts.getAdminQueueCleanupAssistantsTab(), 4);
      browserPage.navigate(
          queuePath + "&tab=general",
          new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
      assertSelectedQueueTab(uiTexts.getAdminQueueCleanupGeneralTab(), 5);
      browserPage.navigate(
          assistantUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
      assertSelectedQueueTab(uiTexts.getAdminQueueCleanupAssistantsTab(), 4);
      for (String invalidSelection :
          new String[] {"", "&tab=unknown", "&tab=assistants&tab=general"}) {
        browserPage.navigate(
            queuePath + invalidSelection,
            new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        assertSelectedQueueTab(uiTexts.getAdminQueueCleanupGeneralTab(), 5);
      }
      AllurePlaywright.attachScreenshot(
          "URL-Auswahl und Zähler der Queue " + viewportWidth, browserPage);
    }
  }

  private void assertSelectedQueueTab(String tabLabel, int expectedSettings) {
    assertThat(
            browserPage
                .locator("#admin-queue-tabs")
                .getByRole(
                    AriaRole.TAB, new Locator.GetByRoleOptions().setName(tabLabel).setExact(true)))
        .hasAttribute("aria-selected", "true");
    assertThat(browserPage.locator(".cosmos-config-setting-count"))
        .hasText(uiTexts.getAdminConfigSettingsCount(expectedSettings));
  }

  /** Prüft die gespeicherten und ungespeicherten Werte durch die echten Reiter und Aktionen. */
  private void verifyQueueDraftsAndAssistantLayout(
      AdminConfigurationPage configurationPage, int viewportWidth) {
    browserPage.setViewportSize(viewportWidth, 1000);
    assertThat(browserPage.locator(".cosmos-config-card-header"))
        .hasCSS("border-bottom-width", "0px");
    BoundingBox queueHeaderBoundingBox =
        browserPage.locator(".cosmos-config-card-header").boundingBox();
    BoundingBox queueTabSheetBoundingBox = browserPage.locator("#admin-queue-tabs").boundingBox();
    assertEquals(
        queueHeaderBoundingBox.y + queueHeaderBoundingBox.height, queueTabSheetBoundingBox.y, 1);

    configurationPage.enter(uiTexts.getAdminConfigEntryEmailQueueMaxRetry(), "4");
    assertThat(browserPage.locator(".cosmos-config-setting-count"))
        .hasText(uiTexts.getAdminConfigSettingsCount(5));
    AllurePlaywright.attachScreenshot(
        "Allgemeine Queue-Einstellungen " + viewportWidth, browserPage);

    selectCleanupTabAndGetAutomaticSwitch().setChecked(false);
    assertThat(browserPage.locator(".cosmos-config-setting-count"))
        .hasText(uiTexts.getAdminConfigSettingsCount(4));
    assertThat(configurationPage.input(uiTexts.getAdminQueueCleanupInterval())).isDisabled();
    assertThat(configurationPage.input(uiTexts.getAdminQueueCleanupMinimumAge())).isDisabled();
    assertThat(configurationPage.input(uiTexts.getAdminQueueCleanupHistoryDays())).isEnabled();
    configurationPage.enter(uiTexts.getAdminQueueCleanupHistoryDays(), "31");
    AllurePlaywright.attachScreenshot("Assistent ausgeschaltet " + viewportWidth, browserPage);

    selectCleanupTabAndGetAutomaticSwitch().setChecked(true);
    assertThat(configurationPage.input(uiTexts.getAdminQueueCleanupInterval())).isEnabled();
    assertThat(configurationPage.input(uiTexts.getAdminQueueCleanupMinimumAge())).isEnabled();
    assertThat(configurationPage.input(uiTexts.getAdminQueueCleanupHistoryDays())).hasValue("31");
    assertThat(configurationPage.input(uiTexts.getAdminConfigEntryEmailQueueMaxRetry()))
        .hasValue("4");
    configurationPage.input(uiTexts.getAdminQueueCleanupHistoryDays());
    BoundingBox queueTabsBoundingBox = browserPage.locator("#admin-queue-tabs").boundingBox();
    BoundingBox assistantCardBoundingBox =
        browserPage.locator("#admin-mail-queue-resolution").boundingBox();
    assertTrue(assistantCardBoundingBox.x > queueTabsBoundingBox.x);
    assertTrue(
        assistantCardBoundingBox.x + assistantCardBoundingBox.width
            < queueTabsBoundingBox.x + queueTabsBoundingBox.width);
    assertEquals(
        assistantCardBoundingBox.x - queueTabsBoundingBox.x,
        queueTabsBoundingBox.x
            + queueTabsBoundingBox.width
            - assistantCardBoundingBox.x
            - assistantCardBoundingBox.width,
        2);
    configurationPage.save();
    configurationPage.reload();
    assertThat(configurationPage.input(uiTexts.getAdminQueueCleanupHistoryDays())).hasValue("31");
    assertThat(configurationPage.input(uiTexts.getAdminConfigEntryEmailQueueMaxRetry()))
        .hasValue("4");
    configurationPage.explain(
        uiTexts.getAdminQueueCleanupHistoryDays(), uiTexts.getAdminQueueCleanupHistoryHelp());

    selectCleanupTabAndGetAutomaticSwitch();
    browserPage.locator("#admin-mail-queue-refresh").click();
    Locator reloadNoticeLocator =
        browserPage
            .locator("vaadin-notification-card:not([closing])")
            .filter(
                new Locator.FilterOptions()
                    .setHasText(uiTexts.getAdminQueueCleanupRefreshSuccess()));
    assertThat(reloadNoticeLocator).isVisible();
    reloadNoticeLocator.getByRole(AriaRole.BUTTON).click();
    assertThat(browserPage.locator(".admin-queue-cleanup-output"))
        .hasCSS("font-family", "Consolas, \"Courier New\", monospace");
    AllurePlaywright.attachScreenshot("Assistent eingeschaltet " + viewportWidth, browserPage);
    assertTrue(
        (boolean)
            browserPage.evaluate("document.documentElement.scrollWidth <= window.innerWidth"));
  }

  private Locator selectCleanupTabAndGetAutomaticSwitch() {
    return selectCleanupTabAndGetAutomaticSwitch(browserPage);
  }

  private Locator selectCleanupTabAndGetAutomaticSwitch(Page configurationBrowserPage) {
    configurationBrowserPage
        .getByRole(
            AriaRole.TAB,
            new Page.GetByRoleOptions()
                .setName(uiTexts.getAdminQueueCleanupAssistantsTab())
                .setExact(true))
        .click();
    return configurationBrowserPage.getByRole(
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
