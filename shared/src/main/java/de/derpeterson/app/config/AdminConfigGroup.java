package de.derpeterson.app.config;

/** Stable configuration groups; independent of translated labels and properties keys. */
public enum AdminConfigGroup {
  GENERAL,
  SECURITY,
  ACCOUNT_MANAGEMENT,
  EXTERNAL_LOGIN,
  TWO_FACTOR,
  TOKENS,
  USER_STATUS,
  NOTIFICATIONS,
  EMAIL_SENDER,
  SMTP,
  QUEUE;

  public boolean isSuperadminOnly() {
    return this == EXTERNAL_LOGIN || this == TWO_FACTOR || this == ACCOUNT_MANAGEMENT;
  }
}
