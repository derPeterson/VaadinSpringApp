package de.derpeterson.browsertests;

import de.derpeterson.browsertests.suites.AccountLifecycleBrowserTest;
import de.derpeterson.browsertests.suites.AccountSecurityBrowserTest;
import de.derpeterson.browsertests.suites.AccountSettingsBrowserTest;
import de.derpeterson.browsertests.suites.AdminMailBrowserTest;
import de.derpeterson.browsertests.suites.AuthenticationBrowserTest;
import de.derpeterson.browsertests.suites.BlockedLinksBrowserTest;
import de.derpeterson.browsertests.suites.ConfigurationBrowserTest;
import de.derpeterson.browsertests.suites.CosmosDesignBrowserTest;
import de.derpeterson.browsertests.suites.InfrastructureBrowserTest;
import de.derpeterson.browsertests.suites.LoginAttemptBrowserTest;
import de.derpeterson.browsertests.suites.LoginLayoutBrowserTest;
import de.derpeterson.browsertests.suites.MaintenanceBrowserTest;
import de.derpeterson.browsertests.suites.NotificationInboxBrowserTest;
import de.derpeterson.browsertests.suites.PasswordResetBrowserTest;
import de.derpeterson.browsertests.suites.PresentationBrowserTest;
import de.derpeterson.browsertests.suites.RegistrationBrowserTest;
import de.derpeterson.browsertests.suites.RememberMeBrowserTest;
import de.derpeterson.browsertests.suites.SessionLimitBrowserTest;
import de.derpeterson.browsertests.suites.SessionManagementBrowserTest;
import de.derpeterson.browsertests.suites.SessionSecurityBrowserTest;
import de.derpeterson.browsertests.suites.TokenConfigurationBrowserTest;
import de.derpeterson.browsertests.suites.TokenExpiryBrowserTest;
import de.derpeterson.browsertests.suites.UserAccessBrowserTest;
import de.derpeterson.browsertests.suites.UserFiltersBrowserTest;
import de.derpeterson.browsertests.suites.UserManagementBrowserTest;
import de.derpeterson.browsertests.suites.UserNotificationsBrowserTest;
import de.derpeterson.browsertests.suites.UserStatusBrowserTest;
import de.derpeterson.browsertests.suites.VerificationBrowserTest;
import java.util.List;

/** The deliberately small list of suites supported by run.ps1 -Suite. */
final class BrowserSuites {
  static List<Class<?>> select(String suite) {
    return switch (suite) {
      case "all", "fast" ->
          List.of(
              UserNotificationsBrowserTest.class,
              NotificationInboxBrowserTest.class,
              InfrastructureBrowserTest.class,
              AuthenticationBrowserTest.class,
              RegistrationBrowserTest.class,
              VerificationBrowserTest.class,
              PasswordResetBrowserTest.class,
              AccountLifecycleBrowserTest.class,
              AccountSettingsBrowserTest.class,
              AccountSecurityBrowserTest.class,
              SessionManagementBrowserTest.class,
              PresentationBrowserTest.class,
              TokenConfigurationBrowserTest.class,
              UserManagementBrowserTest.class,
              UserFiltersBrowserTest.class,
              UserAccessBrowserTest.class,
              SessionSecurityBrowserTest.class,
              BlockedLinksBrowserTest.class,
              RememberMeBrowserTest.class,
              UserStatusBrowserTest.class,
              ConfigurationBrowserTest.class,
              AdminMailBrowserTest.class,
              TokenExpiryBrowserTest.class,
              LoginLayoutBrowserTest.class,
              CosmosDesignBrowserTest.class,
              LoginAttemptBrowserTest.class,
              SessionLimitBrowserTest.class,
              MaintenanceBrowserTest.class);
      case "notifications" ->
          List.of(UserNotificationsBrowserTest.class, NotificationInboxBrowserTest.class);
      case "account-security" -> List.of(AccountSecurityBrowserTest.class);
      case "session-management" -> List.of(SessionManagementBrowserTest.class);
      case "account-settings" -> List.of(AccountSettingsBrowserTest.class);
      case "maintenance" -> List.of(MaintenanceBrowserTest.class);
      case "cosmos-design" -> List.of(CosmosDesignBrowserTest.class);
      case "login-layout" -> List.of(LoginLayoutBrowserTest.class);
      case "login-checks" ->
          List.of(
              LoginLayoutBrowserTest.class,
              AuthenticationBrowserTest.class,
              PresentationBrowserTest.class,
              RememberMeBrowserTest.class);
      case "infrastructure" -> List.of(InfrastructureBrowserTest.class);
      case "session-persistence" ->
          List.of(
              SessionLimitBrowserTest.class,
              SessionManagementBrowserTest.class,
              AuthenticationBrowserTest.class,
              LoginAttemptBrowserTest.class,
              RememberMeBrowserTest.class,
              SessionSecurityBrowserTest.class,
              UserAccessBrowserTest.class,
              MaintenanceBrowserTest.class,
              AccountSecurityBrowserTest.class);
      case "session-limits" ->
          List.of(
              SessionLimitBrowserTest.class,
              SessionManagementBrowserTest.class,
              AuthenticationBrowserTest.class,
              LoginAttemptBrowserTest.class,
              RememberMeBrowserTest.class,
              SessionSecurityBrowserTest.class,
              UserAccessBrowserTest.class);
      case "login-attempts" ->
          List.of(
              LoginAttemptBrowserTest.class,
              AuthenticationBrowserTest.class,
              RememberMeBrowserTest.class);
      case "authentication" -> List.of(AuthenticationBrowserTest.class);
      case "registration" -> List.of(RegistrationBrowserTest.class);
      case "verification" -> List.of(VerificationBrowserTest.class);
      case "password-reset" -> List.of(PasswordResetBrowserTest.class);
      case "account-lifecycle" -> List.of(AccountLifecycleBrowserTest.class);
      case "presentation" -> List.of(PresentationBrowserTest.class);
      case "user-management" ->
          List.of(UserManagementBrowserTest.class, UserFiltersBrowserTest.class);
      case "remaining-coverage" ->
          List.of(
              RememberMeBrowserTest.class,
              UserStatusBrowserTest.class,
              ConfigurationBrowserTest.class,
              AdminMailBrowserTest.class,
              TokenExpiryBrowserTest.class);
      case "configuration" -> List.of(ConfigurationBrowserTest.class);
      case "admin-mail" -> List.of(AdminMailBrowserTest.class);
      case "token-expiry" -> List.of(TokenExpiryBrowserTest.class);
      case "remaining-configuration" ->
          List.of(
              ConfigurationBrowserTest.class,
              AdminMailBrowserTest.class,
              TokenExpiryBrowserTest.class);
      case "remember-me" -> List.of(RememberMeBrowserTest.class);
      case "user-status" -> List.of(UserStatusBrowserTest.class);
      case "session-security" -> List.of(SessionSecurityBrowserTest.class);
      case "blocked-links" -> List.of(BlockedLinksBrowserTest.class);
      case "security-journeys" ->
          List.of(SessionSecurityBrowserTest.class, BlockedLinksBrowserTest.class);
      case "user-access" -> List.of(UserAccessBrowserTest.class);
      case "user-filters" -> List.of(UserFiltersBrowserTest.class);
      case "token-configuration" -> List.of(TokenConfigurationBrowserTest.class);
      default ->
          throw new IllegalArgumentException(
              "Unknown suite: "
                  + suite
                  + ". Choose all, fast, notifications, account-security, session-management, account-settings, cosmos-design, login-layout, login-checks, infrastructure, authentication, registration, verification, password-reset, account-lifecycle, presentation, token-configuration, login-attempts, session-limits, session-persistence, maintenance, user-management, user-filters, user-access, session-security, blocked-links, security-journeys, remember-me, user-status, configuration, admin-mail, token-expiry, remaining-configuration or remaining-coverage.");
    };
  }
}
