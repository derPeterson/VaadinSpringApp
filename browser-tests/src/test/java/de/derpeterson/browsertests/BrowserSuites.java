package de.derpeterson.browsertests;

import de.derpeterson.browsertests.suites.AccountLifecycleBrowserTest;
import de.derpeterson.browsertests.suites.AccountSecurityBrowserTest;
import de.derpeterson.browsertests.suites.AccountSettingsBrowserTest;
import de.derpeterson.browsertests.suites.AdminMailBrowserTest;
import de.derpeterson.browsertests.suites.AuthenticationBrowserTest;
import de.derpeterson.browsertests.suites.BlockedLinksBrowserTest;
import de.derpeterson.browsertests.suites.ConfigurationBrowserTest;
import de.derpeterson.browsertests.suites.CosmosComponentsBrowserTest;
import de.derpeterson.browsertests.suites.CosmosDesignBrowserTest;
import de.derpeterson.browsertests.suites.ExternalLoginBrowserTest;
import de.derpeterson.browsertests.suites.ExternalLoginLayoutBrowserTest;
import de.derpeterson.browsertests.suites.InfrastructureBrowserTest;
import de.derpeterson.browsertests.suites.LoginAttemptBrowserTest;
import de.derpeterson.browsertests.suites.LoginLayoutBrowserTest;
import de.derpeterson.browsertests.suites.MaintenanceBrowserTest;
import de.derpeterson.browsertests.suites.NotificationInboxBrowserTest;
import de.derpeterson.browsertests.suites.PasswordResetBrowserTest;
import de.derpeterson.browsertests.suites.PresentationBrowserTest;
import de.derpeterson.browsertests.suites.RegistrationBrowserTest;
import de.derpeterson.browsertests.suites.RememberMeBrowserTest;
import de.derpeterson.browsertests.suites.RestartAndSmtpBrowserTest;
import de.derpeterson.browsertests.suites.SessionLimitBrowserTest;
import de.derpeterson.browsertests.suites.SessionManagementBrowserTest;
import de.derpeterson.browsertests.suites.SessionSecurityBrowserTest;
import de.derpeterson.browsertests.suites.TokenConfigurationBrowserTest;
import de.derpeterson.browsertests.suites.TokenExpiryBrowserTest;
import de.derpeterson.browsertests.suites.TwoFactorBrowserTest;
import de.derpeterson.browsertests.suites.TwoFactorLifecycleBrowserTest;
import de.derpeterson.browsertests.suites.TwoFactorPushBrowserTest;
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
              TwoFactorBrowserTest.class,
              TwoFactorPushBrowserTest.class,
              TwoFactorLifecycleBrowserTest.class,
              UserNotificationsBrowserTest.class,
              NotificationInboxBrowserTest.class,
              InfrastructureBrowserTest.class,
              RestartAndSmtpBrowserTest.class,
              AuthenticationBrowserTest.class,
              RegistrationBrowserTest.class,
              ExternalLoginBrowserTest.class,
              ExternalLoginLayoutBrowserTest.class,
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
              CosmosComponentsBrowserTest.class,
              LoginAttemptBrowserTest.class,
              SessionLimitBrowserTest.class,
              MaintenanceBrowserTest.class);
      case "external-login-flow" -> List.of(ExternalLoginBrowserTest.class);
      case "external-login-settings" -> List.of(ExternalLoginBrowserTest.class);
      case "follow-up-regressions" ->
          List.of(
              ExternalLoginBrowserTest.class,
              TwoFactorBrowserTest.class,
              TwoFactorPushBrowserTest.class,
              SessionManagementBrowserTest.class,
              UserStatusBrowserTest.class,
              AccountSecurityBrowserTest.class);
      case "two-factor" -> List.of(TwoFactorBrowserTest.class, TwoFactorPushBrowserTest.class);
      case "two-factor-push" -> List.of(TwoFactorPushBrowserTest.class);
      case "ticket-browser-details" ->
          List.of(TwoFactorBrowserTest.class, TwoFactorPushBrowserTest.class);
      case "two-factor-recovery-output" -> List.of(TwoFactorBrowserTest.class);
      case "two-factor-setup" -> List.of(TwoFactorBrowserTest.class);
      case "two-factor-confirmation",
          "two-factor-otp",
          "two-factor-ticket-filters",
          "two-factor-configuration",
          "two-factor-instructions",
          "two-factor-ticket-panel",
          "two-factor-ticket-layout" ->
          List.of(TwoFactorBrowserTest.class);
      case "two-factor-lifecycle" -> List.of(TwoFactorLifecycleBrowserTest.class);
      case "external-login-layout" -> List.of(ExternalLoginLayoutBrowserTest.class);
      case "external-login" ->
          List.of(
              ExternalLoginBrowserTest.class,
              ExternalLoginLayoutBrowserTest.class,
              RegistrationBrowserTest.class,
              AccountSettingsBrowserTest.class,
              AccountSecurityBrowserTest.class);
      case "notifications" ->
          List.of(UserNotificationsBrowserTest.class, NotificationInboxBrowserTest.class);
      case "account-security" -> List.of(AccountSecurityBrowserTest.class);
      case "session-management" -> List.of(SessionManagementBrowserTest.class);
      case "sign-in-session-layout" ->
          List.of(ExternalLoginBrowserTest.class, SessionManagementBrowserTest.class);
      case "account-settings" -> List.of(AccountSettingsBrowserTest.class);
      case "maintenance" -> List.of(MaintenanceBrowserTest.class);
      case "cosmos-design" -> List.of(CosmosDesignBrowserTest.class);
      case "cosmos-components", "cosmos-scrollbars" -> List.of(CosmosComponentsBrowserTest.class);
      case "login-layout" -> List.of(LoginLayoutBrowserTest.class);
      case "login-checks" ->
          List.of(
              LoginLayoutBrowserTest.class,
              AuthenticationBrowserTest.class,
              PresentationBrowserTest.class,
              RememberMeBrowserTest.class);
      case "infrastructure" -> List.of(InfrastructureBrowserTest.class);
      case "https-night" -> List.of(InfrastructureBrowserTest.class, RememberMeBrowserTest.class);
      case "operations" -> List.of(RestartAndSmtpBrowserTest.class);
      case "configuration-follow-up" ->
          List.of(
              ConfigurationBrowserTest.class,
              SessionManagementBrowserTest.class,
              RestartAndSmtpBrowserTest.class);
      case "configuration-field-help" ->
          List.of(
              ConfigurationBrowserTest.class,
              ExternalLoginLayoutBrowserTest.class,
              TwoFactorBrowserTest.class);
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
                  + ". Choose all, fast, external-login-settings, two-factor, two-factor-push, two-factor-recovery-output, two-factor-setup, two-factor-lifecycle, notifications, account-security, session-management, account-settings, cosmos-design, cosmos-components, login-layout, login-checks, infrastructure, https-night, operations, authentication, registration, verification, password-reset, account-lifecycle, presentation, token-configuration, login-attempts, session-limits, session-persistence, maintenance, user-management, user-filters, user-access, session-security, blocked-links, security-journeys, remember-me, user-status, configuration, admin-mail, token-expiry, remaining-configuration or remaining-coverage.");
    };
  }
}
