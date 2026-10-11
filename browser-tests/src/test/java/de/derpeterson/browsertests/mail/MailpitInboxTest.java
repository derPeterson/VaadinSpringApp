package de.derpeterson.browsertests.mail;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks isolation before any browser can follow a link from a captured test mail. */
class MailpitInboxTest {
    private static final String TOKEN = "a28b857d-9841-4ae8-9e21-50db0264a6b1";

    @Test
    void linkOnCustomRunnerPortIsAccepted() {
        withRunnerOrigin(() -> assertDoesNotThrow(() -> MailpitInbox.assertLocalVerificationLink(
                "http://127.0.0.1:18082/verification/" + TOKEN)));
    }

    @Test
    void foreignOrManualTargetsAndWrongRoutesAreRejectedBeforeNavigation() {
        withRunnerOrigin(() -> {
            for (String wrongLink : new String[] {
                    "http://127.0.0.1:18080/verification/" + TOKEN,
                    "http://example.test:18082/verification/" + TOKEN,
                    "https://127.0.0.1:18082/verification/" + TOKEN,
                    "http://user@127.0.0.1:18082/verification/" + TOKEN,
                    "http://127.0.0.1:18082/admin",
                    "http://127.0.0.1:18082/verification/" + TOKEN + "?redirect=elsewhere"
            }) {
                assertThrows(AssertionError.class, () -> MailpitInbox.assertLocalVerificationLink(wrongLink));
            }
        });
    }

    private static void withRunnerOrigin(Runnable test) {
        String previous = System.getProperty("browser.tests.baseUrl");
        try {
            System.setProperty("browser.tests.baseUrl", "http://127.0.0.1:18082");
            test.run();
        } finally {
            if (previous == null) System.clearProperty("browser.tests.baseUrl");
            else System.setProperty("browser.tests.baseUrl", previous);
        }
    }
}
