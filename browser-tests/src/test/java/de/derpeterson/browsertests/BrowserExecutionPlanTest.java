package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.browsertests.suites.AccountSecurityBrowserTest;
import de.derpeterson.browsertests.suites.AccountSettingsBrowserTest;
import de.derpeterson.browsertests.suites.AuthenticationBrowserTest;
import de.derpeterson.browsertests.suites.ExternalLoginBrowserTest;
import de.derpeterson.browsertests.suites.MaintenanceBrowserTest;
import de.derpeterson.browsertests.suites.NotificationInboxBrowserTest;
import de.derpeterson.browsertests.suites.SessionManagementBrowserTest;
import de.derpeterson.browsertests.suites.UserNotificationsBrowserTest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

class BrowserExecutionPlanTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "two-factor-setup",
        "two-factor-confirmation",
        "two-factor-otp",
        "two-factor-ticket-filters",
        "two-factor-configuration",
        "two-factor-instructions",
        "two-factor-ticket-layout",
        "two-factor-ticket-panel",
        "cosmos-scrollbars",
        "duration-field",
        "segmented-selection",
        "mail-queue-resolution",
        "component-follow-up",
        "breadcrumbs",
        "form-details",
        "email-verification",
        "configuration-follow-up",
        "configuration-field-help",
        "external-login-settings"
      })
  void focusedSuitesSelectOnlyTheirExistingJourneys(String suiteName) {
    TestPlan browserTestPlan =
        LauncherFactory.create()
            .discover(BrowserExecutionPlan.request(BrowserSuites.select(suiteName), suiteName));
    Set<String> selectedMethodNames = new HashSet<>();
    for (TestIdentifier rootTestIdentifier : browserTestPlan.getRoots()) {
      for (TestIdentifier testIdentifier : browserTestPlan.getDescendants(rootTestIdentifier)) {
        testIdentifier
            .getSource()
            .filter(MethodSource.class::isInstance)
            .map(MethodSource.class::cast)
            .ifPresent(
                testMethodSource -> selectedMethodNames.add(testMethodSource.getMethodName()));
      }
    }
    Set<String> expectedMethodNames =
        switch (suiteName) {
          case "two-factor-setup" ->
              Set.of("requiredIntroductionReusesFreshPasswordWithoutGrantingPortalAccess");
          case "two-factor-confirmation" -> Set.of("confirmationRecoveryAndHelpUseNativeControls");
          case "two-factor-otp" ->
              Set.of("otpFocusAndAutomaticConfirmationCoverSetupLoginReplacementAndRecovery");
          case "two-factor-ticket-filters" ->
              Set.of(
                  "ticketFiltersAndDirectJumpUseNativeControls",
                  "ticketPagesPreserveDetailsAndDrafts");
          case "two-factor-configuration" -> Set.of("superadminOnlyConfiguration");
          case "two-factor-instructions" ->
              Set.of("recoverySupportRemainsContactInsteadOfFactorReset");
          case "two-factor-ticket-panel" ->
              Set.of("administrativeTicketLayoutKeepsTabsAndCardsUnclipped");
          case "breadcrumbs" ->
              Set.of(
                  "pageBreadcrumbsFollowHierarchyAndKeepSecurityFlowsLocked",
                  "verificationAndReactivationBreadcrumbsStayUnlinked");
          case "segmented-selection" -> Set.of("segmentSelectorsPreserveTheirSurroundingViews");
          case "mail-queue-resolution" ->
              Set.of(
                  "unresolvedRequestNeedsExplicitResolutionBeforeAnotherVerificationMail",
                  "explicitSmtpRejectionReachesItsRetryLimitInsteadOfQuarantine",
                  "queueTabsKeepDraftsAndSeparateGeneralSettingsFromSuperadminAssistants");
          case "component-follow-up" ->
              Set.of(
                  "durationUnitIsCenteredInsideNumericField",
                  "durationVariantsKeepKeyboardAndValidationContracts",
                  "headerActionsAndReadFiltersUsePointerWithoutProfileHover",
                  "verificationAndReactivationBreadcrumbsStayUnlinked",
                  "administrativeCreationRequiresMailProof",
                  "developmentAccountIsExplicitAndDoesNotSendAccountMail",
                  "accountRetentionUsesFixedDaysAndIsRestrictedToSuperadmins",
                  "remainingDurationFieldsPersistWithoutExposingTheCalculationBelowTheInput",
                  "accountDeletionRejectsWrongPasswordAndSendsConfirmation",
                  "enrolledAccountCanBeDeletedWithARecoveryCode",
                  "securityCardsAndLifecycleProofRemainConsistent",
                  "composeValidationAndReviewAtBothWidths",
                  "confirmationRecoveryAndHelpUseNativeControls",
                  "recoverySupportRemainsContactInsteadOfFactorReset",
                  "previewInboxFiltersAndDismissalAtBothWidths",
                  "paginationPreviewLimitAndSentArchive",
                  "everyActionTemplateKeepsItsFullCopyableUrlInsideTheCosmosFrame",
                  "informationalTemplatesDoNotContainAnEmptyActionFrame",
                  "combineAndClearTypedFilters",
                  "languageChangesRetainSelectedFilterAndSearch");
          case "duration-field" -> Set.of("durationUnitIsCenteredInsideNumericField");
          case "cosmos-scrollbars" ->
              Set.of("globalScrollbarsPreserveNativeScrollingAndLocalChatSpacing");
          case "form-details" ->
              Set.of(
                  "globalScrollbarsPreserveNativeScrollingAndLocalChatSpacing",
                  "navigationStatesAreConsistentInPortalAndAdministration",
                  "roleSearchPreservesSelectedRolesInBothDialogs",
                  "choiceHoverPaginationAndDaySuffixUseTheSharedFieldStyle",
                  "profileDraftSurvivesCancelledNavigationAndIsClearedOnDiscard",
                  "providerAndFactorDraftsRequireExplicitDiscard",
                  "changingConfigurationGroupRequiresDiscardingTheDraft",
                  "administrativeCreationRequiresMailProof");
          case "email-verification" ->
              Set.of(
                  "pendingVerificationKeepsPortalAccessBlocked",
                  "verificationAndReactivationBreadcrumbsStayUnlinked",
                  "administrativeCreationRequiresMailProof",
                  "superadminCanExplicitlyCreateUncheckedAccount",
                  "developmentAccountIsExplicitAndDoesNotSendAccountMail",
                  "bootstrapAccountCanRequireVerification",
                  "realMailAllowsActivationAndLogin",
                  "usedLinkRemainsSuccessfulForActiveAccount",
                  "invalidAndMissingTokensHaveDefinedUiBehavior",
                  "signInDetailsDistinguishAccountAndSessionMethods",
                  "microsoftRequiresContactVerification",
                  "optionalPortalPassword");
          case "configuration-follow-up" ->
              Set.of(
                  "accountRetentionUsesFixedDaysAndIsRestrictedToSuperadmins",
                  "changingConfigurationGroupRequiresDiscardingTheDraft",
                  "providerAndFactorDraftsRequireExplicitDiscard",
                  "everyConfigurationFieldHasAnAccessibleLocalizedInfo",
                  "remainingDurationFieldsPersistWithoutExposingTheCalculationBelowTheInput",
                  "expiredSessionLeavesOnlyItsSavedLogin",
                  "persistedConfigurationAndAccountsSurviveRestartWithoutBootstrap");
          case "configuration-field-help" ->
              Set.of(
                  "accountRetentionUsesFixedDaysAndIsRestrictedToSuperadmins",
                  "everyConfigurationFieldHasAnAccessibleLocalizedInfo",
                  "providerTabsKeepIndependentDrafts",
                  "superadminOnlyConfiguration");
          case "two-factor-ticket-layout" ->
              Set.of(
                  "administrativeTicketLayoutKeepsTabsAndCardsUnclipped",
                  "ticketUnreadAndNotificationsFollowAssignmentAndClosure");
          default ->
              Set.of(
                  "administrativeProviderSettings",
                  "disabledProvidersPreserveExistingAccountAccess");
        };

    assertEquals(expectedMethodNames, selectedMethodNames);
  }

  @Test
  void externalSuiteKeepsTheLongProviderClassOnItsOwnWorker() {
    List<List<Class<?>>> workerClassGroups =
        BrowserExecutionPlan.partition(BrowserSuites.select("external-login"), 3, "external-login");

    assertEquals(
        List.of(ExternalLoginBrowserTest.class),
        workerClassGroups.stream()
            .filter(workerClasses -> workerClasses.contains(ExternalLoginBrowserTest.class))
            .findFirst()
            .orElseThrow());
  }

  @Test
  void notificationCasesAreInAllAndKeepTheFastSelectionUnchanged() {
    Class<UserNotificationsBrowserTest> testClass = UserNotificationsBrowserTest.class;

    assertTrue(BrowserSuites.select("all").contains(testClass));
    assertFalse(BrowserExecutionPlan.classes("fast").contains(testClass));
    int testCaseCount = 0;
    for (Method testMethod : testClass.getDeclaredMethods()) {
      if (!testMethod.isAnnotationPresent(Test.class)
          && !testMethod.isAnnotationPresent(ParameterizedTest.class)) {
        continue;
      }
      assertFalse(testMethod.isAnnotationPresent(Disabled.class));
      ValueSource valueSource = testMethod.getAnnotation(ValueSource.class);
      testCaseCount +=
          valueSource == null ? 1 : valueSource.ints().length + valueSource.booleans().length;
    }
    assertEquals(5, testCaseCount);
    assertTrue(BrowserSuites.select("all").contains(NotificationInboxBrowserTest.class));
    assertEquals(2, BrowserSuites.select("notifications").size());
    assertFalse(BrowserExecutionPlan.classes("fast").contains(NotificationInboxBrowserTest.class));
  }

  @Test
  void fastUsesOnlyExplicitlyMarkedExistingCases() {
    List<Class<?>> selectedTestClasses = BrowserExecutionPlan.classes("fast");

    assertFalse(selectedTestClasses.isEmpty());
    assertTrue(BrowserSuites.select("all").containsAll(selectedTestClasses));
    TestPlan browserTestPlan =
        LauncherFactory.create()
            .discover(BrowserExecutionPlan.request(selectedTestClasses, "fast"));
    Set<String> discoveredMethodNames = new HashSet<>();
    for (TestIdentifier rootTestIdentifier : browserTestPlan.getRoots()) {
      for (TestIdentifier testIdentifier : browserTestPlan.getDescendants(rootTestIdentifier)) {
        testIdentifier
            .getSource()
            .filter(MethodSource.class::isInstance)
            .map(MethodSource.class::cast)
            .ifPresent(testMethodSource ->
                discoveredMethodNames.add(
                    testMethodSource.getClassName() + "#" + testMethodSource.getMethodName()));
      }
    }
    Set<String> fastTaggedMethodNames = new HashSet<>();
    for (Class<?> testClass : BrowserSuites.select("all")) {
      for (Method testMethod : testClass.getDeclaredMethods()) {
        if (BrowserExecutionPlan.fast(testMethod)) {
          fastTaggedMethodNames.add(testClass.getName() + "#" + testMethod.getName());
        }
      }
    }
    assertEquals(fastTaggedMethodNames, discoveredMethodNames);
    assertTrue(
        discoveredMethodNames.stream()
            .anyMatch(methodName -> methodName.contains("AuthenticationBrowserTest")));
    assertTrue(
        discoveredMethodNames.stream()
            .anyMatch(methodName -> methodName.contains("UserFiltersBrowserTest")));
    assertTrue(
        discoveredMethodNames.stream()
            .anyMatch(methodName -> methodName.contains("CosmosDesignBrowserTest")));
    assertTrue(
        discoveredMethodNames.stream()
            .noneMatch(methodName ->
                methodName.contains("TokenExpiryBrowserTest")
                    || methodName.contains("UserStatusBrowserTest")));
  }

  @Test
  void fastCaseCountMatchesDocumentedSelectionIncludingAllCsvVariants() {
    int testCaseCount = 0;
    for (Class<?> testClass : BrowserExecutionPlan.classes("fast")) {
      for (Method testMethod : testClass.getDeclaredMethods()) {
        if (!BrowserExecutionPlan.fast(testMethod)) {
          continue;
        }
        CsvSource csvSource = testMethod.getAnnotation(CsvSource.class);
        testCaseCount += csvSource == null ? 1 : csvSource.value().length;

        assertFalse(testMethod.isAnnotationPresent(Disabled.class));
      }
    }
    assertEquals(
        34, testCaseCount, "Update the documented selection deliberately when changing fast tags.");
  }

  @Test
  void partitionsCoverEveryClassOnceForAllSupportedWorkerCounts() {
    for (String suiteName :
        List.of(
            "all",
            "fast",
            "authentication",
            "login-attempts",
            "session-limits",
            "session-persistence",
            "maintenance",
            "user-management")) {
      List<Class<?>> selectedTestClasses = BrowserExecutionPlan.classes(suiteName);
      for (int workerCount = 1; workerCount <= 4; workerCount++) {
        List<List<Class<?>>> workerClassGroups =
            BrowserExecutionPlan.partition(selectedTestClasses, workerCount, suiteName);
        List<Class<?>> flattenedTestClasses =
            workerClassGroups.stream().flatMap(List::stream).toList();

        assertEquals(selectedTestClasses.size(), flattenedTestClasses.size());
        assertEquals(new HashSet<>(selectedTestClasses), new HashSet<>(flattenedTestClasses));
        assertEquals(Math.min(workerCount, selectedTestClasses.size()), workerClassGroups.size());
        assertTrue(workerClassGroups.stream().noneMatch(List::isEmpty));
        assertEquals(
            workerClassGroups,
            BrowserExecutionPlan.partition(selectedTestClasses, workerCount, suiteName));
      }
    }
  }

  @Test
  void persistenceAcceptanceCombinesSessionMaintenanceAndAccountSecurityWithoutDuplicates() {
    List<Class<?>> selectedTestClasses = BrowserSuites.select("session-persistence");

    assertEquals(9, selectedTestClasses.size());
    assertEquals(selectedTestClasses.size(), new HashSet<>(selectedTestClasses).size());
    assertTrue(selectedTestClasses.containsAll(BrowserSuites.select("session-limits")));
    assertTrue(selectedTestClasses.containsAll(BrowserSuites.select("maintenance")));
    assertTrue(selectedTestClasses.containsAll(BrowserSuites.select("account-security")));
  }

  @Test
  void invalidOrRepeatedWorkerSelectionsAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserExecutionPlan.workerClasses("fast", "TokenExpiryBrowserTest"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BrowserExecutionPlan.workerClasses(
                "all", "AuthenticationBrowserTest,AuthenticationBrowserTest"));
    List<Class<?>> emptyTestClasses = List.of();
    List<Class<?>> allTestClasses = BrowserExecutionPlan.classes("all");

    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserExecutionPlan.partition(emptyTestClasses, 2, "all"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserExecutionPlan.partition(allTestClasses, 0, "all"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserExecutionPlan.partition(allTestClasses, 5, "all"));
    assertEquals(
        List.of(AuthenticationBrowserTest.class),
        BrowserExecutionPlan.workerClasses("all", "AuthenticationBrowserTest"));
  }

  @Test
  void maintenanceCasesAreIncludedInAllButNotImplicitlyInFast() {
    List<Class<?>> selectedTestClasses = BrowserSuites.select("maintenance");

    assertEquals(List.of(MaintenanceBrowserTest.class), selectedTestClasses);
    assertTrue(BrowserSuites.select("all").containsAll(selectedTestClasses));
    int testCaseCount = 0;
    for (Method testMethod : selectedTestClasses.getFirst().getDeclaredMethods()) {
      if (!testMethod.isAnnotationPresent(Test.class)
          && !testMethod.isAnnotationPresent(ParameterizedTest.class)) {
        continue;
      }
      assertFalse(BrowserExecutionPlan.fast(testMethod));
      CsvSource csvSource = testMethod.getAnnotation(CsvSource.class);
      testCaseCount += csvSource == null ? 1 : csvSource.value().length;
    }
    assertEquals(9, testCaseCount);
  }

  @Test
  void accountSettingsAreIncludedInAllAndRemainOutsideFastUntilDeliberatelySelected() {
    assertEquals(
        List.of(AccountSettingsBrowserTest.class), BrowserSuites.select("account-settings"));
    assertTrue(BrowserSuites.select("all").contains(AccountSettingsBrowserTest.class));
    for (Method testMethod : AccountSettingsBrowserTest.class.getDeclaredMethods()) {
      assertFalse(BrowserExecutionPlan.fast(testMethod));
    }
    assertThrows(IllegalStateException.class, BrowserTestRunner::restartTestApplication);
  }

  @Test
  void newAccountJourneysAreIncludedOnceInAllAndOutsideFast() {
    assertEquals(
        List.of(AccountSecurityBrowserTest.class), BrowserSuites.select("account-security"));
    assertEquals(
        List.of(SessionManagementBrowserTest.class), BrowserSuites.select("session-management"));
    for (Class<? extends BrowserFixture> testClass :
        List.of(AccountSecurityBrowserTest.class, SessionManagementBrowserTest.class)) {
      assertEquals(
          1, BrowserSuites.select("all").stream().filter(item -> item == testClass).count());
      assertFalse(BrowserExecutionPlan.classes("fast").contains(testClass));
    }
    assertTrue(BrowserSuites.select("session-limits").contains(SessionManagementBrowserTest.class));
    assertEquals(
        4,
        Arrays.stream(AccountSecurityBrowserTest.class.getDeclaredMethods())
            .filter(testMethod -> testMethod.isAnnotationPresent(Test.class))
            .count());
    assertEquals(
        7,
        Arrays.stream(SessionManagementBrowserTest.class.getDeclaredMethods())
            .filter(testMethod -> testMethod.isAnnotationPresent(Test.class))
            .count());
  }
}
