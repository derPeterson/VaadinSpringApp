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
  private final String query;
  private final boolean account;

  private static final class Query {
    private static final String RECOVER = "?recover";
  }

  TwoFactorFlow(String query, boolean account) {
    this.query = query;
    this.account = account;
  }

  public static TwoFactorFlow current(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session != null && session.getAttribute(SESSION_ATTRIBUTE) instanceof String name
        ? valueOf(name)
        : null;
  }

  public void enter(HttpServletRequest request) {
    request.getSession().setAttribute(SESSION_ATTRIBUTE, name());
  }

  public static void finish(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(SESSION_ATTRIBUTE);
      session.removeAttribute(INTRO_ACCEPTED);
    }
  }

  public String path() {
    return help() ? HELP_PATH : AppRouteConstants.TWO_FACTOR_SECURITY_ROUTE;
  }

  public String location() {
    return help() ? path() : path() + query;
  }

  public boolean help() {
    return query == null;
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
