package de.derpeterson.browsertests;

import de.derpeterson.browsertests.suites.AuthenticationBrowserTest;
import de.derpeterson.browsertests.suites.InfrastructureBrowserTest;
import de.derpeterson.browsertests.suites.RegistrationBrowserTest;
import de.derpeterson.browsertests.suites.VerificationBrowserTest;
import de.derpeterson.browsertests.suites.PasswordResetBrowserTest;
import de.derpeterson.browsertests.suites.AccountLifecycleBrowserTest;
import de.derpeterson.browsertests.suites.PresentationBrowserTest;
import de.derpeterson.browsertests.suites.TokenConfigurationBrowserTest;
import de.derpeterson.browsertests.suites.UserManagementBrowserTest;
import de.derpeterson.browsertests.suites.UserFiltersBrowserTest;
import java.util.List;

/** The deliberately small list of suites supported by run.ps1 -Suite. */
final class BrowserSuites {
    static List<Class<?>> select(String suite) {
        return switch (suite) {
            case "all" -> List.of(InfrastructureBrowserTest.class, AuthenticationBrowserTest.class, RegistrationBrowserTest.class, VerificationBrowserTest.class, PasswordResetBrowserTest.class, AccountLifecycleBrowserTest.class, PresentationBrowserTest.class, TokenConfigurationBrowserTest.class, UserManagementBrowserTest.class, UserFiltersBrowserTest.class);
            case "infrastructure" -> List.of(InfrastructureBrowserTest.class);
            case "authentication" -> List.of(AuthenticationBrowserTest.class);
            case "registration" -> List.of(RegistrationBrowserTest.class);
            case "verification" -> List.of(VerificationBrowserTest.class);
            case "password-reset" -> List.of(PasswordResetBrowserTest.class);
            case "account-lifecycle" -> List.of(AccountLifecycleBrowserTest.class);
            case "presentation" -> List.of(PresentationBrowserTest.class);
            case "user-management" -> List.of(UserManagementBrowserTest.class, UserFiltersBrowserTest.class);
            case "user-filters" -> List.of(UserFiltersBrowserTest.class);
            case "token-configuration" -> List.of(TokenConfigurationBrowserTest.class);
            default -> throw new IllegalArgumentException("Unknown suite: " + suite
                    + ". Choose all, infrastructure, authentication, registration, verification, password-reset, account-lifecycle, presentation, token-configuration, user-management or user-filters.");
        };
    }
}
