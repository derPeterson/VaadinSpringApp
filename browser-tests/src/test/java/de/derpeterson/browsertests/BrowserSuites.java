package de.derpeterson.browsertests;

import de.derpeterson.browsertests.suites.AuthenticationBrowserTest;
import de.derpeterson.browsertests.suites.InfrastructureBrowserTest;
import de.derpeterson.browsertests.suites.RegistrationBrowserTest;
import de.derpeterson.browsertests.suites.VerificationBrowserTest;
import de.derpeterson.browsertests.suites.PasswordResetBrowserTest;
import java.util.List;

/** The deliberately small list of suites supported by run.ps1 -Suite. */
final class BrowserSuites {
    static List<Class<?>> select(String suite) {
        return switch (suite) {
            case "all" -> List.of(InfrastructureBrowserTest.class, AuthenticationBrowserTest.class, RegistrationBrowserTest.class, VerificationBrowserTest.class, PasswordResetBrowserTest.class);
            case "infrastructure" -> List.of(InfrastructureBrowserTest.class);
            case "authentication" -> List.of(AuthenticationBrowserTest.class);
            case "registration" -> List.of(RegistrationBrowserTest.class);
            case "verification" -> List.of(VerificationBrowserTest.class);
            case "password-reset" -> List.of(PasswordResetBrowserTest.class);
            default -> throw new IllegalArgumentException("Unknown suite: " + suite
                    + ". Choose all, infrastructure, authentication, registration, verification or password-reset.");
        };
    }
}
