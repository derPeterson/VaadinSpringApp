package de.derpeterson.browsertests;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static de.derpeterson.app.config.AppRouteConstants.HOME_ROUTE;
import static de.derpeterson.browsertests.BrowserRoutes.path;

import com.microsoft.playwright.assertions.PageAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Deliberate failure, selected only by run.ps1 -FailureProbe; excluded from mvn test. */
@Tag("browser")
class FailureProbeTest extends BrowserFixture {
  @Test
  @DisplayName("REPORTING-PROBE: Absichtlicher Fehler zum Vergleich von Screenshot und Trace")
  void deliberateFailure() {
    page.navigate(path(HOME_ROUTE));
    assertThat(page)
        .hasTitle(
            "ABSICHTLICHER REPORTER-PROBEFEHLER",
            new PageAssertions.HasTitleOptions().setTimeout(1000));
  }
}
