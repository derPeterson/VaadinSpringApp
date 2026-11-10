package de.derpeterson.app.security;

import de.derpeterson.app.config.AppRouteConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Die Sitzung bestimmt den erlaubten Ablauf. Queryparameter allein wechseln keinen Schritt. */
public enum TwoFactorFlow {
  LOGIN("", false),
  ADMINISTRATION("?administration", false),
  SETUP("?setup", true),
  RECOVERY(Query.RECOVER, false),
  ADMIN_RECOVERY(Query.RECOVER, false),
  ACCOUNT_RECOVERY(Query.RECOVER, true),
  LOGIN_HELP(null, false),
  ADMIN_HELP(null, false),
  ACCOUNT_HELP(null, true);

  public static final String SESSION_ATTRIBUTE = TwoFactorFlow.class.getName();
  public static final String INTRO_ACCEPTED = SESSION_ATTRIBUTE + ".introAccepted";
  public static final String HELP_PATH = "/recovery-help";
  private final String flowQueryParameter;
  private final boolean account;

  private static final class Query {
    private static final String RECOVER = "?recover";
  }

  TwoFactorFlow(String flowQueryParameter, boolean account) {
    this.flowQueryParameter = flowQueryParameter;
    this.account = account;
  }

  public static TwoFactorFlow current(HttpServletRequest httpRequest) {
    HttpSession httpSession = httpRequest.getSession(false);

    return httpSession != null
            && httpSession.getAttribute(SESSION_ATTRIBUTE) instanceof String storedFlowName
        ? valueOf(storedFlowName)
        : null;
  }

  public void enter(HttpServletRequest httpRequest) {
    httpRequest.getSession().setAttribute(SESSION_ATTRIBUTE, name());
  }

  public static void finish(HttpServletRequest httpRequest) {
    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession != null) {
      httpSession.removeAttribute(SESSION_ATTRIBUTE);
      httpSession.removeAttribute(INTRO_ACCEPTED);
    }
  }

  public String path() {
    return help() ? HELP_PATH : AppRouteConstants.TWO_FACTOR_SECURITY_ROUTE;
  }

  public String location() {
    return help() ? path() : path() + flowQueryParameter;
  }

  public boolean help() {
    return flowQueryParameter == null;
  }

  public boolean account() {
    return account;
  }

  public boolean administration() {
    return this == ADMINISTRATION || this == ADMIN_RECOVERY || this == ADMIN_HELP;
  }

  public boolean recovery() {
    return this == RECOVERY || this == ADMIN_RECOVERY || this == ACCOUNT_RECOVERY;
  }
}
