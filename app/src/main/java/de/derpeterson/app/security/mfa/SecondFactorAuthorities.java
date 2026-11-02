package de.derpeterson.app.security.mfa;

/** Eigene, ausschließlich als echte Spring-Faktorautoritäten verwendete Nachweisarten. */
public final class SecondFactorAuthorities {
  public static final String TOTP = "FACTOR_TOTP";
  public static final String RECOVERY_CODE = "FACTOR_RECOVERY_CODE";

  private SecondFactorAuthorities() {}
}
