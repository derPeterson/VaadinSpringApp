package de.derpeterson.app.security;

import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/** Aktueller Anmeldeweg, unabhängig von der Herkunft des Kontos. */
public enum SignInMethod {
  PORTAL,
  GOOGLE,
  MICROSOFT,
  SAVED_LOGIN;

  static SignInMethod fromAuthentication(Authentication authentication) {
    return switch (authentication) {
      case ExternalAuthenticationToken external -> provider(external.provider());
      case RememberMeAuthenticationToken ignored -> SAVED_LOGIN;
      case UsernamePasswordAuthenticationToken ignored -> PORTAL;
      case null, default -> null;
    };
  }

  public static SignInMethod provider(String provider) {
    return switch (provider) {
      case "google" -> GOOGLE;
      case "microsoft" -> MICROSOFT;
      default -> throw new IllegalArgumentException("Unknown sign-in provider");
    };
  }
}
