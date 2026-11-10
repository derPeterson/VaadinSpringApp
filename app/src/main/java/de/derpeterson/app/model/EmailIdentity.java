package de.derpeterson.app.model;

import java.util.Locale;

/** Shared identity contract for persistence, authentication and lookup. */
public final class EmailIdentity {
  private EmailIdentity() {}

  public static String canonicalize(String emailAddress) {
    return emailAddress == null ? null : emailAddress.trim().toLowerCase(Locale.ROOT);
  }
}
