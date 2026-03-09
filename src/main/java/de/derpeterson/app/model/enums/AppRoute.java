package de.derpeterson.app.model.enums;

import de.derpeterson.app.config.AppRouteConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AppRoute {

    BASE(AppRouteConstants.BASE_ROUTE, AppRouteConstants.BASE_SECURITY_ROUTE, AppRouteConstants.BASE_PAGE_TITLE, null),
    HOME(AppRouteConstants.HOME_ROUTE, AppRouteConstants.HOME_SECURITY_ROUTE, AppRouteConstants.HOME_PAGE_TITLE, null),
    ADMIN(AppRouteConstants.ADMIN_ROUTE, AppRouteConstants.ADMIN_SECURITY_ROUTE, AppRouteConstants.ADMIN_PAGE_TITLE, AppRouteConstants.ADMIN_ROUTE_ROLES),
    LOGIN(AppRouteConstants.LOGIN_ROUTE, AppRouteConstants.LOGIN_SECURITY_ROUTE, AppRouteConstants.LOGIN_PAGE_TITLE, null),
    LOGOUT(AppRouteConstants.LOGOUT_ROUTE, AppRouteConstants.LOGOUT_SECURITY_ROUTE, AppRouteConstants.LOGOUT_PAGE_TITLE, null),
    REGISTRATION(AppRouteConstants.REGISTRATION_ROUTE, AppRouteConstants.REGISTRATION_SECURITY_ROUTE, AppRouteConstants.REGISTRATION_PAGE_TITLE, null),
    VERIFICATION(AppRouteConstants.VERIFICATION_ROUTE, AppRouteConstants.VERIFICATION_SECURITY_ROUTE, AppRouteConstants.VERIFICATION_PAGE_TITEL, null),
    FORGOT_PASSWORD(AppRouteConstants.FORGOT_PASSWORD_ROUTE, AppRouteConstants.FORGOT_PASSWORD_SECURITY_ROUTE, AppRouteConstants.FORGOT_PASSWORD_PAGE_TITLE, null),
    RESET_PASSWORD(AppRouteConstants.RESET_PASSWORD_ROUTE, AppRouteConstants.RESET_PASSWORD_SECURITY_ROUTE, AppRouteConstants.RESET_PAGE_TITLE, null),

    H2(AppRouteConstants.H2_ROUTE, AppRouteConstants.H2_SECURITY_ROUTE, AppRouteConstants.H2_PAGE_TITLE, null);

    private final String route;
    private final String securityRoute;
    private final String pageTitle;
    private final String roles;
}
