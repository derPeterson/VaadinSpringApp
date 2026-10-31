package de.derpeterson.app.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Geprüfte externe Identität mit ausschließlich lokalen Rechten; kein Passwort-/MFA-Nachweis. */
public final class ExternalAuthenticationToken extends AbstractAuthenticationToken {
  private final UserDetails principal;
  private final String provider;

  public ExternalAuthenticationToken(UserDetails principal, String provider) {
    this(principal, provider, principal.getAuthorities());
  }

  public ExternalAuthenticationToken(
      UserDetails principal, String provider, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    if (!"google".equals(provider) && !"microsoft".equals(provider)) {
      throw new IllegalArgumentException("Unknown external provider");
    }
    this.principal = principal;
    this.provider = provider;
    super.setAuthenticated(true);
  }

  public String provider() {
    return provider;
  }

  @Override
  public UserDetails getPrincipal() {
    return principal;
  }

  @Override
  public Object getCredentials() {
    return null;
  }
}
