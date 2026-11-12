package de.derpeterson.app.security;

import org.springframework.security.core.AuthenticationException;

/** Wird erst nach gültigem Portalpasswort ausgelöst, ohne Portalrechte zu erteilen. */
public final class AccountReactivationRequiredException extends AuthenticationException {
  private final Long userId;
  private final String credentialVersion;

  public AccountReactivationRequiredException(PortalUserDetails portalUserDetails) {
    super("Account reactivation required");
    userId = portalUserDetails.getPendingReactivationUserId();
    credentialVersion = portalUserDetails.credentialVersion();
  }

  public Long getUserId() {
    return userId;
  }

  public String getCredentialVersion() {
    return credentialVersion;
  }
}
