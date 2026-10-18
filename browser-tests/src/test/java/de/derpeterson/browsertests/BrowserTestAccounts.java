package de.derpeterson.browsertests;

/** Invented credentials exclusively for the runner's disposable Docker database. */
public final class BrowserTestAccounts {
  public static final String ADMIN_EMAIL = "browser-admin@example.test";
  public static final String ADMIN_PASSWORD = "BrowserAdmin1!";
  public static final String SUPERADMIN_EMAIL = "browser-superadmin@example.test";
  public static final String SUPERADMIN_PASSWORD = "BrowserSuperadmin1!";
  public static final String USER_PASSWORD = "BrowserUser1!";
  public static final String RESET_PASSWORD = "BrowserReset2!";

  private BrowserTestAccounts() {}
}
