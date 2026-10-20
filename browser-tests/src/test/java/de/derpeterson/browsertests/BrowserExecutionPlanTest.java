package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.browsertests.suites.AccountSecurityBrowserTest;
import de.derpeterson.browsertests.suites.AccountSettingsBrowserTest;
import de.derpeterson.browsertests.suites.AuthenticationBrowserTest;
import de.derpeterson.browsertests.suites.MaintenanceBrowserTest;
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
  @Test
  void notificationCasesAreInAllAndKeepTheFastSelectionUnchanged() {
    Class<UserNotificationsBrowserTest> type =
        de.derpeterson.browsertests.suites.UserNotificationsBrowserTest.class;
    assertTrue(BrowserSuites.select("all").contains(type));
    assertFalse(BrowserExecutionPlan.classes("fast").contains(type));
    int cases = 0;
    for (Method method : type.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Test.class)
          && !method.isAnnotationPresent(ParameterizedTest.class)) {
        continue;
      }
      assertFalse(method.isAnnotationPresent(Disabled.class));
      ValueSource values =
          method.getAnnotation(org.junit.jupiter.params.provider.ValueSource.class);
      cases += values == null ? 1 : values.ints().length + values.booleans().length;
    }
    assertEquals(5, cases);
    assertTrue(
        BrowserSuites.select("all")
            .contains(de.derpeterson.browsertests.suites.NotificationInboxBrowserTest.class));
    assertEquals(2, BrowserSuites.select("notifications").size());
    assertFalse(
        BrowserExecutionPlan.classes("fast")
            .contains(de.derpeterson.browsertests.suites.NotificationInboxBrowserTest.class));
  }

  @Test
  void fastUsesOnlyExplicitlyMarkedExistingCases() {
    List<Class<?>> selected = BrowserExecutionPlan.classes("fast");
    assertFalse(selected.isEmpty());
    assertTrue(BrowserSuites.select("all").containsAll(selected));
    TestPlan plan =
        LauncherFactory.create().discover(BrowserExecutionPlan.request(selected, "fast"));
    Set<String> discovered = new HashSet<>();
    for (TestIdentifier root : plan.getRoots()) {
      for (TestIdentifier test : plan.getDescendants(root)) {
        test.getSource()
            .filter(MethodSource.class::isInstance)
            .map(MethodSource.class::cast)
            .ifPresent(
                method -> discovered.add(method.getClassName() + "#" + method.getMethodName()));
      }
    }
    Set<String> marked = new HashSet<>();
    for (Class<?> type : BrowserSuites.select("all")) {
      for (Method method : type.getDeclaredMethods()) {
        if (BrowserExecutionPlan.fast(method)) {
          marked.add(type.getName() + "#" + method.getName());
        }
      }
    }
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
    for (Class<?> type : BrowserExecutionPlan.classes("fast")) {
      for (Method method : type.getDeclaredMethods()) {
        if (!BrowserExecutionPlan.fast(method)) {
          continue;
        }
        CsvSource csv = method.getAnnotation(CsvSource.class);
        cases += csv == null ? 1 : csv.value().length;
        assertFalse(method.isAnnotationPresent(Disabled.class));
      }
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
            "session-persistence",
            "maintenance",
            "user-management")) {
      List<Class<?>> selected = BrowserExecutionPlan.classes(suite);
      for (int workers = 1; workers <= 4; workers++) {
        List<List<Class<?>>> groups = BrowserExecutionPlan.partition(selected, workers, suite);
        List<Class<?>> flattened = groups.stream().flatMap(List::stream).toList();
        assertEquals(selected.size(), flattened.size());
        assertEquals(new HashSet<>(selected), new HashSet<>(flattened));
        assertEquals(Math.min(workers, selected.size()), groups.size());
        assertTrue(groups.stream().noneMatch(List::isEmpty));
        assertEquals(groups, BrowserExecutionPlan.partition(selected, workers, suite));
      }
    }
  }

  @Test
  void persistenceAcceptanceCombinesSessionMaintenanceAndAccountSecurityWithoutDuplicates() {
    List<Class<?>> selected = BrowserSuites.select("session-persistence");
    assertEquals(9, selected.size());
    assertEquals(selected.size(), new HashSet<>(selected).size());
    assertTrue(selected.containsAll(BrowserSuites.select("session-limits")));
    assertTrue(selected.containsAll(BrowserSuites.select("maintenance")));
    assertTrue(selected.containsAll(BrowserSuites.select("account-security")));
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
    List<Class<?>> classes = BrowserSuites.select("maintenance");
    assertEquals(List.of(MaintenanceBrowserTest.class), classes);
    assertTrue(BrowserSuites.select("all").containsAll(classes));
    int cases = 0;
    for (Method method : classes.getFirst().getDeclaredMethods()) {
      if (!method.isAnnotationPresent(Test.class)
          && !method.isAnnotationPresent(ParameterizedTest.class)) {
        continue;
      }
      assertFalse(BrowserExecutionPlan.fast(method));
      CsvSource csv = method.getAnnotation(CsvSource.class);
      cases += csv == null ? 1 : csv.value().length;
    }
    assertEquals(9, cases);
  }

  @Test
  void accountSettingsAreIncludedInAllAndRemainOutsideFastUntilDeliberatelySelected() {
    assertEquals(
        List.of(AccountSettingsBrowserTest.class), BrowserSuites.select("account-settings"));
    assertTrue(BrowserSuites.select("all").contains(AccountSettingsBrowserTest.class));
    for (Method method : AccountSettingsBrowserTest.class.getDeclaredMethods()) {
      assertFalse(BrowserExecutionPlan.fast(method));
    }
    assertThrows(IllegalStateException.class, BrowserTestRunner::restartTestApplication);
  }

  @Test
  void newAccountJourneysAreIncludedOnceInAllAndOutsideFast() {
    assertEquals(
        List.of(AccountSecurityBrowserTest.class), BrowserSuites.select("account-security"));
    assertEquals(
        List.of(SessionManagementBrowserTest.class), BrowserSuites.select("session-management"));
    for (Class<? extends BrowserFixture> type :
        List.of(AccountSecurityBrowserTest.class, SessionManagementBrowserTest.class)) {
      assertEquals(1, BrowserSuites.select("all").stream().filter(item -> item == type).count());
      assertFalse(BrowserExecutionPlan.classes("fast").contains(type));
    }
    assertTrue(BrowserSuites.select("session-limits").contains(SessionManagementBrowserTest.class));
    assertEquals(
        4,
        Arrays.stream(AccountSecurityBrowserTest.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Test.class))
            .count());
    assertEquals(
        6,
        Arrays.stream(SessionManagementBrowserTest.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Test.class))
            .count());
  }
}
