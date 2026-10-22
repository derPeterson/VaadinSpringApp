package de.derpeterson.browsertests;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

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
          Map.entry("OperationsBrowserTest", 320),
          Map.entry("TokenExpiryBrowserTest", 209),
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
          Map.entry("AccountSettingsBrowserTest", 150),
          // Neue Sicherheits-/Sitzungsjourneys zunächst geschätzt, keine Laufzeitgarantie.
          Map.entry("AccountSecurityBrowserTest", 120),
          Map.entry("SessionManagementBrowserTest", 130),
          Map.entry("RememberMeBrowserTest", 142),
          Map.entry("PresentationBrowserTest", 29),
          Map.entry("AuthenticationBrowserTest", 26),
          Map.entry("LoginLayoutBrowserTest", 25),
          Map.entry("VerificationBrowserTest", 20),
          Map.entry("AdminMailBrowserTest", 19),
          Map.entry("RegistrationBrowserTest", 16),
          Map.entry("InfrastructureBrowserTest", 7));

  static boolean fast(Method method) {
    return Arrays.stream(method.getAnnotationsByType(Tag.class))
        .anyMatch(tag -> tag.value().equals("fast"));
  }

  static List<Class<?>> classes(String suite) {
    return BrowserSuites.select(suite).stream()
        .filter(
            type ->
                !suite.equals("fast")
                    || Arrays.stream(type.getDeclaredMethods())
                        .anyMatch(BrowserExecutionPlan::fast))
        .toList();
  }

  static LauncherDiscoveryRequest request(List<Class<?>> classes, String suite) {
    LauncherDiscoveryRequestBuilder builder =
        LauncherDiscoveryRequestBuilder.request()
            .selectors(classes.stream().map(type -> selectClass(type)).toList())
            // Playwright und globale App-Konfiguration bleiben je Worker strikt sequenziell.
            .configurationParameter("junit.jupiter.execution.parallel.enabled", "false");
    if (suite.equals("fast")) {
      builder.filters(TagFilter.includeTags("fast"));
    }
    return builder.build();
  }

  static List<Class<?>> workerClasses(String suite, String names) {
    List<Class<?>> allowed = classes(suite);
    if (names == null) {
      return allowed;
    }
    // Das Komma ist ein literales Trennzeichen; keine Anker, Gruppen oder Ersetzungen.
    List<String> requested = List.of(names.split(","));
    List<Class<?>> selected =
        allowed.stream().filter(type -> requested.contains(type.getSimpleName())).toList();
    if (selected.isEmpty() || selected.size() != requested.size()) {
      throw new IllegalArgumentException(
          "Worker classes must be a unique subset of the selected suite.");
    }
    return selected;
  }

  static List<List<Class<?>>> partition(List<Class<?>> classes, int workers, String suite) {
    if (workers < 1
        || workers > 4
        || classes.isEmpty()
        || new HashSet<>(classes).size() != classes.size()) {
      throw new IllegalArgumentException("Use 1 to 4 workers and distinct, nonempty test classes.");
    }
    int count = Math.min(workers, classes.size());
    List<List<Class<?>>> groups = new ArrayList<>();
    double[] costs = new double[count];
    for (int i = 0; i < count; i++) {
      groups.add(new ArrayList<>());
    }
    ArrayList<Class<?>> sorted = new ArrayList<>(classes);
    sorted.sort(
        Comparator.<Class<?>>comparingDouble(type -> weight(type, suite))
            .reversed()
            .thenComparing(Class::getSimpleName));
    for (Class<?> type : sorted) {
      int target = 0;
      for (int i = 1; i < count; i++) {
        if (costs[i] < costs[target]) {
          target = i;
        }
      }
      groups.get(target).add(type);
      costs[target] += weight(type, suite);
    }
    return groups.stream().map(List::copyOf).toList();
  }

  private static double weight(Class<?> type, String suite) {
    double total = 0, selected = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Test.class)
          && !method.isAnnotationPresent(ParameterizedTest.class)) {
        continue;
      }
      CsvSource csv = method.getAnnotation(CsvSource.class);
      int cases = csv == null ? 1 : Math.max(1, csv.value().length);
      total += cases;
      if (fast(method)) {
        selected += cases;
      }
    }
    double seconds = SECONDS.getOrDefault(type.getSimpleName(), 10);
    return suite.equals("fast") && total > 0 ? seconds * selected / total : seconds;
  }
}
