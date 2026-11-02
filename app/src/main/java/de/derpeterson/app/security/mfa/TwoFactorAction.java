package de.derpeterson.app.security.mfa;

/**
 * Fachliche Zwecke werden getrennt freigegeben; ein Login bestätigt keine spätere Kontoänderung.
 */
public enum TwoFactorAction {
  LOGIN,
  ADMINISTRATION,
  PASSWORD,
  EMAIL,
  LINK_PROVIDER,
  UNLINK_PROVIDER,
  MANAGE_FACTOR,
  MANAGE_RECOVERY_CODES,
  SECURITY_CONFIGURATION
}
