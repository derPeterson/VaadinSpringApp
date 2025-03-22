package de.derpeterson.app.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppRouteConstants {
    public static final String BASE_ROUTE = "";
    public static final String BASE_SECURITY_ROUTE = "/";
    public static final String BASE_PAGE_TITLE = "Base";

    public static final String HOME_ROUTE = "home";
    public static final String HOME_SECURITY_ROUTE = "/home";
    public static final String HOME_PAGE_TITLE = "Home";

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

    public static final String H2_ROUTE = "h2";
    public static final String H2_SECURITY_ROUTE = "/h2";
    public static final String H2_PAGE_TITLE = "H2 Database";

}
