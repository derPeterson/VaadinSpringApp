package de.derpeterson.app.config;

public final class AppRouteConstants {
  private AppRouteConstants() {}

  public static final String BASE_ROUTE = "";
  public static final String BASE_SECURITY_ROUTE = "/";
  public static final String BASE_PAGE_TITLE = "Base";

  public static final String HOME_ROUTE = "home";
  public static final String HOME_SECURITY_ROUTE = "/home";
  public static final String HOME_PAGE_TITLE = "Home";

  public static final String MESSAGES_ROUTE = "messages";
  public static final String MESSAGES_SECURITY_ROUTE = "/messages";
  public static final String MESSAGES_PAGE_TITLE = "Messages";

  public static final String ACCOUNT_ROUTE = "account";
  public static final String ACCOUNT_SECURITY_ROUTE = "/account";
  public static final String ACCOUNT_PAGE_TITLE = "My Account";

  public static final String EMAIL_CHANGE_ROUTE = "confirm-email-change";
  public static final String EMAIL_CHANGE_SECURITY_ROUTE = "/confirm-email-change/**";
  public static final String EMAIL_CHANGE_PAGE_TITLE = "Confirm Email Change";

  public static final String ADMIN_ROUTE = "admin";
  public static final String ADMIN_SECURITY_ROUTE = "/admin";
  public static final String ADMIN_PAGE_TITLE = "Admin";
  public static final String ADMIN_ROUTE_ROLES = "ADMIN";

  public static final String LOGIN_ROUTE = "login";
  public static final String LOGIN_SECURITY_ROUTE = "/login";
  public static final String LOGIN_PAGE_TITLE = "Login";

  public static final String LOGOUT_ROUTE = "logout";
  public static final String LOGOUT_SECURITY_ROUTE = "/logout";
  public static final String LOGOUT_PAGE_TITLE = "Logout";

  public static final String REGISTRATION_ROUTE = "registration";
  public static final String REGISTRATION_SECURITY_ROUTE = "/registration";
  public static final String REGISTRATION_PAGE_TITLE = "Registration";

  public static final String VERIFICATION_ROUTE = "verification";
  public static final String VERIFICATION_SECURITY_ROUTE = "/verification/**";
  public static final String VERIFICATION_PAGE_TITEL = "Verification";

  public static final String FORGOT_PASSWORD_ROUTE = "forgot-password";
  public static final String FORGOT_PASSWORD_SECURITY_ROUTE = "/forgot-password";
  public static final String FORGOT_PASSWORD_PAGE_TITLE = "Forgot Password";

  public static final String RESET_PASSWORD_ROUTE = "reset-password";
  public static final String RESET_PASSWORD_SECURITY_ROUTE = "/reset-password/**";
  public static final String RESET_PAGE_TITLE = "Reset Password";

  public static final String MAINTENANCE_ROUTE = "maintenance";
  public static final String MAINTENANCE_SECURITY_ROUTE = "/maintenance";
  public static final String MAINTENANCE_PAGE_TITLE = "Maintenance";

  public static final String H2_ROUTE = "h2";
  public static final String H2_SECURITY_ROUTE = "/h2";
  public static final String H2_PAGE_TITLE = "H2 Database";
}
