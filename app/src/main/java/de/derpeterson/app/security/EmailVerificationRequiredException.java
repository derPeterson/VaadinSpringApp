package de.derpeterson.app.security;

import org.springframework.security.core.AuthenticationException;

/** Erst nach gültigem Passwort; enthält weder Passwort noch Portalberechtigungen. */
public final class EmailVerificationRequiredException extends AuthenticationException {
  private final Long userId;
  private final String credentialVersion;
  private final String email;

  public EmailVerificationRequiredException(PortalUserDetails portalUserDetails) {
    super("Email verification required");
    userId = portalUserDetails.getPendingVerificationUserId();
    credentialVersion = portalUserDetails.credentialVersion();
    email = portalUserDetails.getUsername();
  }

  public Long getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  public String getCredentialVersion() {
    return credentialVersion;
  }
}
