package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.*;

import de.derpeterson.browsertests.suites.AccountSettingsBrowserTest;
import de.derpeterson.browsertests.suites.AuthenticationBrowserTest;
import de.derpeterson.browsertests.suites.MaintenanceBrowserTest;
import java.util.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.core.LauncherFactory;

class BrowserExecutionPlanTest {
  @Test
  void fastUsesOnlyExplicitlyMarkedExistingCases() {
    var selected = BrowserExecutionPlan.classes("fast");
    assertFalse(selected.isEmpty());
    assertTrue(BrowserSuites.select("all").containsAll(selected));
    var plan = LauncherFactory.create().discover(BrowserExecutionPlan.request(selected, "fast"));
    Set<String> discovered = new HashSet<>();
    for (var root : plan.getRoots())
      for (var test : plan.getDescendants(root)) {
        test.getSource()
            .filter(MethodSource.class::isInstance)
            .map(MethodSource.class::cast)
            .ifPresent(
                method -> discovered.add(method.getClassName() + "#" + method.getMethodName()));
      }
    Set<String> marked = new HashSet<>();
    for (var type : BrowserSuites.select("all"))
      for (var method : type.getDeclaredMethods())
        if (BrowserExecutionPlan.fast(method)) marked.add(type.getName() + "#" + method.getName());
    assertEquals(marked, discovered);
    assertTrue(discovered.stream().anyMatch(name -> name.contains("AuthenticationBrowserTest")));
    assertTrue(discovered.stream().anyMatch(name -> name.contains("UserFiltersBrowserTest")));
    assertTrue(discovered.stream().anyMatch(name -> name.contains("CosmosDesignBrowserTest")));
    assertTrue(
        discovered.stream()
            .noneMatch(
                name ->
                    name.contains("TokenExpiryBrowserTest")
                        || name.contains("UserStatusBrowserTest")));
  }

  @Test
  void fastCaseCountMatchesDocumentedSelectionIncludingAllCsvVariants() {
    int cases = 0;
    for (var type : BrowserExecutionPlan.classes("fast"))
      for (var method : type.getDeclaredMethods()) {
        if (!BrowserExecutionPlan.fast(method)) continue;
        var csv = method.getAnnotation(CsvSource.class);
        cases += csv == null ? 1 : csv.value().length;
        assertFalse(method.isAnnotationPresent(Disabled.class));
      }
    assertEquals(
        34, cases, "Update the documented selection deliberately when changing fast tags.");
  }

  @Test
  void partitionsCoverEveryClassOnceForAllSupportedWorkerCounts() {
    for (String suite :
        List.of(
            "all",
            "fast",
            "authentication",
            "login-attempts",
            "session-limits",
            "maintenance",
            "user-management")) {
      var selected = BrowserExecutionPlan.classes(suite);
      for (int workers = 1; workers <= 4; workers++) {
        var groups = BrowserExecutionPlan.partition(selected, workers, suite);
        var flattened = groups.stream().flatMap(List::stream).toList();
        assertEquals(selected.size(), flattened.size());
        assertEquals(new HashSet<>(selected), new HashSet<>(flattened));
        assertEquals(Math.min(workers, selected.size()), groups.size());
        assertTrue(groups.stream().noneMatch(List::isEmpty));
        assertEquals(groups, BrowserExecutionPlan.partition(selected, workers, suite));
      }
    }
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
    assertThrows(
        IllegalArgumentException.class, () -> BrowserExecutionPlan.partition(List.of(), 2, "all"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserExecutionPlan.partition(BrowserExecutionPlan.classes("all"), 0, "all"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BrowserExecutionPlan.partition(BrowserExecutionPlan.classes("all"), 5, "all"));
    assertEquals(
        List.of(AuthenticationBrowserTest.class),
        BrowserExecutionPlan.workerClasses("all", "AuthenticationBrowserTest"));
  }

  @Test
  void maintenanceCasesAreIncludedInAllButNotImplicitlyInFast() {
    var classes = BrowserSuites.select("maintenance");
    assertEquals(List.of(MaintenanceBrowserTest.class), classes);
    assertTrue(BrowserSuites.select("all").containsAll(classes));
    int cases = 0;
    for (var method : classes.getFirst().getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Test.class)
          && !method.isAnnotationPresent(ParameterizedTest.class)) continue;
      assertFalse(BrowserExecutionPlan.fast(method));
      var csv = method.getAnnotation(CsvSource.class);
      cases += csv == null ? 1 : csv.value().length;
    }
    assertEquals(9, cases);
  }

  @Test
  void accountSettingsAreIncludedInAllAndRemainOutsideFastUntilDeliberatelySelected() {
    assertEquals(
        List.of(AccountSettingsBrowserTest.class), BrowserSuites.select("account-settings"));
    assertTrue(BrowserSuites.select("all").contains(AccountSettingsBrowserTest.class));
    for (var method : AccountSettingsBrowserTest.class.getDeclaredMethods())
      assertFalse(BrowserExecutionPlan.fast(method));
    assertThrows(IllegalStateException.class, BrowserTestRunner::restartTestApplication);
  }
}
