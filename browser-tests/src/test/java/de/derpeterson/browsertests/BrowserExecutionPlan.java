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
  // Bestandsgewichte aus der DE-/EN-Messung vom 15.10.2026; Wartung zunächst geschätzt.
  // Dies sind Verteilungsgewichte, keine gemessene Dauer oder Laufzeitgarantie neuer Fälle.
  // Lange Klassen zuerst verteilen; kein Zugriff auf aufbewahrte Run-Ordner nötig.
  private static final Map<String, Integer> SECONDS =
      Map.ofEntries(
          Map.entry("RestartAndSmtpBrowserTest", 320),
          // Eine zusätzliche echte Minute prüft die erneute Verifikationsanforderung.
          Map.entry("TokenExpiryBrowserTest", 269),
          Map.entry("UserManagementBrowserTest", 173),
          Map.entry("SessionSecurityBrowserTest", 116),
          Map.entry("UserAccessBrowserTest", 114),
          Map.entry("BlockedLinksBrowserTest", 112),
          Map.entry("CosmosDesignBrowserTest", 100),
          Map.entry("MaintenanceBrowserTest", 150),
          Map.entry("PasswordResetBrowserTest", 93),
          Map.entry("ConfigurationBrowserTest", 92),
          Map.entry("TokenConfigurationBrowserTest", 77),
          Map.entry("AccountLifecycleBrowserTest", 72),
          Map.entry("UserStatusBrowserTest", 72),
          Map.entry("UserFiltersBrowserTest", 66),
          // Neue Klassen/Rebootfälle: gezielte DE-Messung vom 16.10.2026, grob gerundet.
          // Zusätzliche echte Reaktivierungswartezeit auf beiden Breiten: 2 x 5 Minuten.
          Map.entry("AccountSettingsBrowserTest", 750),
          // DE-Gesamtlauf vom 03.11.2026: gerundete Verteilungsgewichte einschließlich
          // echter Frist- und Neustartprüfungen, keine Laufzeitgarantie.
          Map.entry("ExternalLoginBrowserTest", 1260),
          Map.entry("ExternalLoginLayoutBrowserTest", 90),
          Map.entry("TwoFactorBrowserTest", 1380),
          Map.entry("TwoFactorLifecycleBrowserTest", 240),
          Map.entry("TwoFactorPushBrowserTest", 540),
          // Neue Sicherheits-/Sitzungsjourneys zunächst geschätzt, keine Laufzeitgarantie.
          Map.entry("AccountSecurityBrowserTest", 120),
          Map.entry("SessionManagementBrowserTest", 130),
          Map.entry("RememberMeBrowserTest", 142),
          Map.entry("PresentationBrowserTest", 29),
          Map.entry("AuthenticationBrowserTest", 26),
          Map.entry("LoginLayoutBrowserTest", 25),
          // Zusätzliche Nachweisfälle mit echter Versandwartezeit, zunächst geschätzt.
          Map.entry("VerificationBrowserTest", 300),
          Map.entry("AdminMailBrowserTest", 19),
          Map.entry("RegistrationBrowserTest", 16),
          Map.entry("InfrastructureBrowserTest", 7));

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

    double estimatedClassSeconds = SECONDS.getOrDefault(testClass.getSimpleName(), 10);
    return suiteName.equals("fast") && totalCaseCount > 0
        ? estimatedClassSeconds * fastCaseCount / totalCaseCount
        : estimatedClassSeconds;
  }
}
