package de.derpeterson.app.security.mfa;

public enum TwoFactorMode {
  OFF,
  IF_ENROLLED,
  REQUIRED;

  public boolean requiresFactor(boolean enrolled) {
    return this == REQUIRED || this == IF_ENROLLED && enrolled;
  }
}
