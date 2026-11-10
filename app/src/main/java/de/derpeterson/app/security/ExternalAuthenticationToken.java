package de.derpeterson.app.security;

import java.util.Collection;
import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Geprüfte externe Identität mit ausschließlich lokalen Rechten; kein Passwort-/MFA-Nachweis. */
public final class ExternalAuthenticationToken extends AbstractAuthenticationToken {
  private final UserDetails authenticatedUserDetails;
  private final String identityProviderName;

  public ExternalAuthenticationToken(
      UserDetails authenticatedUserDetails, String identityProviderName) {
    this(authenticatedUserDetails, identityProviderName, authenticatedUserDetails.getAuthorities());
  }

  public ExternalAuthenticationToken(
      UserDetails authenticatedUserDetails,
      String identityProviderName,
      Collection<? extends GrantedAuthority> authorities) {
    if (!"google".equals(identityProviderName) && !"microsoft".equals(identityProviderName)) {
      throw new IllegalArgumentException("Unknown external provider");
    }
    super(authorities);
    this.authenticatedUserDetails = authenticatedUserDetails;
    this.identityProviderName = identityProviderName;
    super.setAuthenticated(true);
  }

  public String provider() {
    return identityProviderName;
  }

  @Override
  public UserDetails getPrincipal() {
    return authenticatedUserDetails;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExternalAuthenticationToken external
        && super.equals(other)
        && identityProviderName.equals(external.identityProviderName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), identityProviderName);
  }
}
