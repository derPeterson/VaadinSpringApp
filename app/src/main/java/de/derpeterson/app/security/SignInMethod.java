package de.derpeterson.app.security;

/** Aktueller Anmeldeweg, unabhängig von der Herkunft des Kontos. */
public enum SignInMethod {
  PORTAL,
  GOOGLE,
  MICROSOFT,
  SAVED_LOGIN;

  public static SignInMethod provider(String provider) {
    return switch (provider) {
      case "google" -> GOOGLE;
      case "microsoft" -> MICROSOFT;
      default -> throw new IllegalArgumentException("Unknown sign-in provider");
    };
  }
}
