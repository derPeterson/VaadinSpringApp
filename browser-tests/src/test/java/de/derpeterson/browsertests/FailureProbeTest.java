package de.derpeterson.browsertests;

import org.junit.jupiter.api.*;
import com.microsoft.playwright.assertions.PageAssertions;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Deliberate failure, selected only by run.ps1 -FailureProbe; excluded from mvn test. */
@Tag("browser")
class FailureProbeTest extends BrowserFixture {
    @Test
    @DisplayName("REPORTING-PROBE: Absichtlicher Fehler zum Vergleich von Screenshot und Trace")
    void deliberateFailure() {
        page.navigate("/home");
        assertThat(page).hasTitle("ABSICHTLICHER REPORTER-PROBEFEHLER", new PageAssertions.HasTitleOptions().setTimeout(1000));
    }
}
