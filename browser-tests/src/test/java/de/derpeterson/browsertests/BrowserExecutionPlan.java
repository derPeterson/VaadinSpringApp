package de.derpeterson.browsertests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

/** Selects existing cases and keeps whole classes together in independent JVMs. */
final class BrowserExecutionPlan {
  // Gerundete Verteilungsgewichte aus dem DE-Gesamtlauf vom 13.11.2026.
  // Die langen 2FA-/Feldhilfefälle sind vollständig berücksichtigt. Keine Zeitgarantie.
  // Bei unterbrochenen Fällen ist die verbleibende Arbeit konservativ geschätzt.
  // Einzelne Testtimeouts und das Gesamtlimit eines Workers bleiben unverändert.
  private static final Map<String, Integer> ESTIMATED_TEST_CLASS_SECONDS_MAP =
      Map.ofEntries(
          Map.entry("RestartAndSmtpBrowserTest", 410),
          Map.entry("MailQueueResolutionBrowserTest", 320),
          Map.entry("SegmentedSelectionBrowserTest", 70),
          Map.entry("EmailActionLinkBrowserTest", 20),
          Map.entry("TokenExpiryBrowserTest", 260),
          Map.entry("UserManagementBrowserTest", 410),
          Map.entry("SessionSecurityBrowserTest", 200),
          Map.entry("UserAccessBrowserTest", 330),
          Map.entry("BlockedLinksBrowserTest", 150),
          Map.entry("CosmosDesignBrowserTest", 330),
          Map.entry("MaintenanceBrowserTest", 280),
          Map.entry("PasswordResetBrowserTest", 100),
          // Unterbrochene Teilfälle beziehungsweise noch fehlende Folgeschritte mit Reserve.
          Map.entry("ConfigurationBrowserTest", 620),
          Map.entry("TokenConfigurationBrowserTest", 120),
          Map.entry("AccountLifecycleBrowserTest", 70),
          Map.entry("UserStatusBrowserTest", 100),
          Map.entry("UserFiltersBrowserTest", 260),
          Map.entry("AccountSettingsBrowserTest", 1290),
          // Unterbrochene Teilfälle beziehungsweise noch fehlende Folgeschritte mit Reserve.
          Map.entry("ExternalLoginBrowserTest", 1440),
          Map.entry("ExternalLoginLayoutBrowserTest", 150),
          Map.entry("TwoFactorBrowserTest", 2400),
          Map.entry("TwoFactorLifecycleBrowserTest", 260),
          Map.entry("TwoFactorPushBrowserTest", 510),
          Map.entry("AccountSecurityBrowserTest", 100),
          Map.entry("SessionManagementBrowserTest", 430),
          Map.entry("RememberMeBrowserTest", 190),
          Map.entry("PresentationBrowserTest", 60),
          Map.entry("AuthenticationBrowserTest", 70),
          Map.entry("LoginLayoutBrowserTest", 120),
          Map.entry("VerificationBrowserTest", 460),
          // Unterbrochene Teilfälle beziehungsweise noch fehlende Folgeschritte mit Reserve.
          Map.entry("AdminMailBrowserTest", 120),
          Map.entry("RegistrationBrowserTest", 20),
          Map.entry("InfrastructureBrowserTest", 20),
          // Unterbrochene Teilfälle beziehungsweise noch fehlende Folgeschritte mit Reserve.
          Map.entry("CosmosComponentsBrowserTest", 430),
          Map.entry("NotificationInboxBrowserTest", 210),
          Map.entry("SessionLimitBrowserTest", 160),
          Map.entry("UserNotificationsBrowserTest", 180),
          // Im abgebrochenen Worker noch nicht gestartete Klasse, konservativ geschätzt.
          Map.entry("LoginAttemptBrowserTest", 120));

  static boolean fast(Method method) {
    return Arrays.stream(method.getAnnotationsByType(Tag.class))
        .anyMatch(tag -> tag.value().equals("fast"));
  }

  static List<Class<?>> classes(String suite) {
    return BrowserSuites.select(suite).stream()
        .filter(type ->
            !suite.equals("fast")
                || Arrays.stream(type.getDeclaredMethods()).anyMatch(BrowserExecutionPlan::fast))
        .toList();
  }

  static LauncherDiscoveryRequest request(List<Class<?>> testClasses, String suiteName) {
    LauncherDiscoveryRequestBuilder discoveryRequestBuilder =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(testClasses.stream().map(DiscoverySelectors::selectClass).toList())
            // Playwright und globale App-Konfiguration bleiben je Worker strikt sequenziell.
            .configurationParameter("junit.jupiter.execution.parallel.enabled", "false");

    if (suiteName.equals("fast")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("fast"));
    } else if (suiteName.equals("two-factor-recovery-output")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("recovery-output"));
    } else if (suiteName.equals("two-factor-setup")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("setup-screen"));
    } else if (suiteName.equals("two-factor-confirmation")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("confirmation-screens"));
    } else if (suiteName.equals("two-factor-otp")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("otp-behavior"));
    } else if (suiteName.equals("two-factor-ticket-filters")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("ticket-filters"));
    } else if (suiteName.equals("two-factor-configuration")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("two-factor-configuration"));
    } else if (suiteName.equals("two-factor-instructions")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("recovery-instructions"));
    } else if (suiteName.equals("two-factor-ticket-layout")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("ticket-layout"));
    } else if (suiteName.equals("two-factor-ticket-panel")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("ticket-panel-style"));
    } else if (suiteName.equals("cosmos-scrollbars")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("scrollbar-style"));
    } else if (suiteName.equals("breadcrumbs")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("breadcrumbs"));
    } else if (suiteName.equals("component-follow-up")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("component-follow-up"));
    } else if (suiteName.equals("duration-field")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("duration-field"));
    } else if (suiteName.equals("external-login-settings")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("provider-settings"));
    } else if (suiteName.equals("follow-up-regressions")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("follow-up-regression"));
    } else if (suiteName.equals("ticket-browser-details")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("ticket-browser-detail"));
    } else if (suiteName.equals("sign-in-session-layout")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("sign-in-session-layout"));
    } else if (suiteName.equals("configuration-follow-up")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("configuration-follow-up"));
    } else if (suiteName.equals("form-details")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("form-details"));
    } else if (suiteName.equals("email-verification")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("email-verification"));
    } else if (suiteName.equals("configuration-field-help")) {
      discoveryRequestBuilder.filters(TagFilter.includeTags("configuration-field-help"));
    }

    return discoveryRequestBuilder.build();
  }

  static List<Class<?>> workerClasses(String suiteName, String requestedClassNamesCsv) {
    List<Class<?>> allowedClasses = classes(suiteName);
    if (requestedClassNamesCsv == null) {
      return allowedClasses;
    }

    // Das Komma ist ein literales Trennzeichen; keine Anker, Gruppen oder Ersetzungen.
    List<String> requestedClassNames = List.of(requestedClassNamesCsv.split(","));
    List<Class<?>> selectedWorkerClasses =
        allowedClasses.stream()
            .filter(testClass -> requestedClassNames.contains(testClass.getSimpleName()))
            .toList();

    if (selectedWorkerClasses.isEmpty()
        || selectedWorkerClasses.size() != requestedClassNames.size()) {
      throw new IllegalArgumentException(
          "Worker classes must be a unique subset of the selected suite.");
    }

    return selectedWorkerClasses;
  }

  static List<List<Class<?>>> partition(
      List<Class<?>> testClasses, int requestedWorkerCount, String suiteName) {
    if (requestedWorkerCount < 1
        || requestedWorkerCount > 4
        || testClasses.isEmpty()
        || new HashSet<>(testClasses).size() != testClasses.size()) {
      throw new IllegalArgumentException("Use 1 to 4 workers and distinct, nonempty test classes.");
    }

    int workerCount = Math.min(requestedWorkerCount, testClasses.size());
    List<List<Class<?>>> classesByWorker = new ArrayList<>();
    double[] estimatedSecondsByWorker = new double[workerCount];
    for (int workerGroupIndex = 0; workerGroupIndex < workerCount; workerGroupIndex++) {
      classesByWorker.add(new ArrayList<>());
    }

    ArrayList<Class<?>> classesByDescendingWeight = new ArrayList<>(testClasses);
    classesByDescendingWeight.sort(
        Comparator.<Class<?>>comparingDouble(testClass -> weight(testClass, suiteName))
            .reversed()
            .thenComparing(Class::getSimpleName));

    for (Class<?> testClass : classesByDescendingWeight) {
      int leastLoadedWorkerIndex = 0;
      for (int workerGroupIndex = 1; workerGroupIndex < workerCount; workerGroupIndex++) {
        if (estimatedSecondsByWorker[workerGroupIndex]
            < estimatedSecondsByWorker[leastLoadedWorkerIndex]) {
          leastLoadedWorkerIndex = workerGroupIndex;
        }
      }
      classesByWorker.get(leastLoadedWorkerIndex).add(testClass);
      estimatedSecondsByWorker[leastLoadedWorkerIndex] += weight(testClass, suiteName);
    }

    return classesByWorker.stream().map(List::copyOf).toList();
  }

  private static double weight(Class<?> testClass, String suiteName) {
    double totalCaseCount = 0, fastCaseCount = 0;
    for (Method testMethod : testClass.getDeclaredMethods()) {
      if (!testMethod.isAnnotationPresent(Test.class)
          && !testMethod.isAnnotationPresent(ParameterizedTest.class)) {
        continue;
      }
      CsvSource csvSource = testMethod.getAnnotation(CsvSource.class);
      int methodCaseCount = csvSource == null ? 1 : Math.max(1, csvSource.value().length);
      totalCaseCount += methodCaseCount;
      if (fast(testMethod)) {
        fastCaseCount += methodCaseCount;
      }
    }

    double estimatedClassSeconds =
        ESTIMATED_TEST_CLASS_SECONDS_MAP.getOrDefault(testClass.getSimpleName(), 10);
    return suiteName.equals("fast") && totalCaseCount > 0
        ? estimatedClassSeconds * fastCaseCount / totalCaseCount
        : estimatedClassSeconds;
  }
}
