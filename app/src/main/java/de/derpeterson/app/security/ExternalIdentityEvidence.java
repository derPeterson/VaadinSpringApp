package de.derpeterson.app.security;

import java.time.Instant;

/** Nur aus einem durch Spring geprüften OIDC-ID-Token erstellen. Keine Roh-Tokens aufbewahren. */
public record ExternalIdentityEvidence(
    String provider,
    String issuer,
    String subject,
    String email,
    boolean trustedEmail,
    String firstName,
    String lastName,
    Instant authenticatedAt) {
  public ExternalIdentityEvidence {
    if ((!"google".equals(provider) && !"microsoft".equals(provider))
        || issuer == null
        || issuer.isBlank()
        || issuer.length() > 255
        || subject == null
        || subject.isBlank()
        || subject.length() > 255) {
      throw new IllegalArgumentException("Invalid external identity");
    }
  }

  @Override
  public String toString() {
    return "Verified external identity";
  }
}
