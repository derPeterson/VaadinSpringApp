package de.derpeterson.browsertests;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.core.LauncherFactory;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BrowserExecutionPlanTest {
    @Test void fastUsesOnlyExplicitlyMarkedExistingCases() {
        var selected = BrowserExecutionPlan.classes("fast");
        assertFalse(selected.isEmpty());
        assertTrue(BrowserSuites.select("all").containsAll(selected));
        var plan = LauncherFactory.create().discover(BrowserExecutionPlan.request(selected, "fast"));
        Set<String> discovered = new HashSet<>();
        for (var root : plan.getRoots()) for (var test : plan.getDescendants(root)) {
            test.getSource().filter(MethodSource.class::isInstance).map(MethodSource.class::cast)
                    .ifPresent(method -> discovered.add(method.getClassName() + "#" + method.getMethodName()));
        }
        Set<String> marked = new HashSet<>();
        for (var type : BrowserSuites.select("all")) for (var method : type.getDeclaredMethods())
            if (BrowserExecutionPlan.fast(method)) marked.add(type.getName() + "#" + method.getName());
        assertEquals(marked, discovered);
        assertTrue(discovered.stream().anyMatch(name -> name.contains("AuthenticationBrowserTest")));
        assertTrue(discovered.stream().anyMatch(name -> name.contains("UserFiltersBrowserTest")));
        assertTrue(discovered.stream().anyMatch(name -> name.contains("CosmosDesignBrowserTest")));
        assertTrue(discovered.stream().noneMatch(name -> name.contains("TokenExpiryBrowserTest") || name.contains("UserStatusBrowserTest")));
    }

    @Test void fastCaseCountMatchesDocumentedSelectionIncludingAllCsvVariants() {
        int cases = 0;
        for (var type : BrowserExecutionPlan.classes("fast")) for (var method : type.getDeclaredMethods()) {
            if (!BrowserExecutionPlan.fast(method)) continue;
            var csv = method.getAnnotation(org.junit.jupiter.params.provider.CsvSource.class);
            cases += csv == null ? 1 : csv.value().length;
            assertFalse(method.isAnnotationPresent(org.junit.jupiter.api.Disabled.class));
        }
        assertEquals(34, cases, "Update the documented selection deliberately when changing fast tags.");
    }

    @Test void partitionsCoverEveryClassOnceForAllSupportedWorkerCounts() {
        for (String suite : List.of("all", "fast", "authentication", "user-management")) {
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

    @Test void invalidOrRepeatedWorkerSelectionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> BrowserExecutionPlan.workerClasses("fast", "TokenExpiryBrowserTest"));
        assertThrows(IllegalArgumentException.class, () -> BrowserExecutionPlan.workerClasses("all", "AuthenticationBrowserTest,AuthenticationBrowserTest"));
        assertThrows(IllegalArgumentException.class, () -> BrowserExecutionPlan.partition(List.of(), 2, "all"));
        assertThrows(IllegalArgumentException.class, () -> BrowserExecutionPlan.partition(BrowserExecutionPlan.classes("all"), 0, "all"));
        assertThrows(IllegalArgumentException.class, () -> BrowserExecutionPlan.partition(BrowserExecutionPlan.classes("all"), 5, "all"));
        assertEquals(List.of(de.derpeterson.browsertests.suites.AuthenticationBrowserTest.class),
                BrowserExecutionPlan.workerClasses("all", "AuthenticationBrowserTest"));
    }
}
