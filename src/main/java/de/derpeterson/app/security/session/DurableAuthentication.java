package de.derpeterson.app.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/** Explizites Authentifizierungsmemento; niemals Credentials oder einen Servicegraph speichern. */
public record DurableAuthentication(
    String username,
    String credentialVersion,
    boolean remembered,
    int rememberKeyHash,
    List<Factor> factors) {
  public record Factor(String authority, Instant issuedAt) {}

  public DurableAuthentication {
    if (username == null
        || username.isBlank()
        || username.length() > 254
        || credentialVersion == null
        || credentialVersion.length() != 64
        || factors == null
        || factors.size() > 16) {
      throw new IllegalArgumentException("Invalid durable authentication");
    }
    factors = List.copyOf(factors);
  }

  public static DurableAuthentication capture(
      Authentication authentication, UserDetailsService users) {
    if (!authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof UserDetails authenticated)
        || !(authentication instanceof UsernamePasswordAuthenticationToken
            || authentication instanceof RememberMeAuthenticationToken)) {
      throw new AuthenticationServiceException("Unsupported durable authentication");
    }
    UserDetails current = users.loadUserByUsername(authenticated.getUsername());
    String authenticatedPassword = authenticated.getPassword();
    String currentVersion = fingerprint(current);
    if (!current.isEnabled()
        || !current.isAccountNonLocked()
        || !current.isAccountNonExpired()
        || !current.isCredentialsNonExpired()
        // Spring/Flow entfernen Credentials nach erfolgreicher Passwortprüfung.
        || authenticatedPassword != null
            && !authenticatedPassword.isEmpty()
            && !currentVersion.equals(fingerprint(authenticatedPassword))) {
      throw new AuthenticationServiceException("Account unavailable");
    }
    List<Factor> factors =
        authentication.getAuthorities().stream()
            .filter(FactorGrantedAuthority.class::isInstance)
            .map(FactorGrantedAuthority.class::cast)
            .map(factor -> new Factor(factor.getAuthority(), factor.getIssuedAt()))
            .toList();
    boolean remembered = authentication instanceof RememberMeAuthenticationToken;
    int keyHash = remembered ? ((RememberMeAuthenticationToken) authentication).getKeyHash() : 0;
    return new DurableAuthentication(
        current.getUsername(), currentVersion, remembered, keyHash, factors);
  }

  public SecurityContext restore(UserDetailsService users, String rememberKey) {
    UserDetails current = users.loadUserByUsername(username);
    if (!current.isEnabled()
        || !current.isAccountNonLocked()
        || !current.isAccountNonExpired()
        || !current.isCredentialsNonExpired()
        || !MessageDigest.isEqual(
            credentialVersion.getBytes(StandardCharsets.US_ASCII),
            fingerprint(current).getBytes(StandardCharsets.US_ASCII))) {
      throw new AuthenticationServiceException("Persisted account access is no longer valid");
    }
    List<GrantedAuthority> authorities = new ArrayList<>(current.getAuthorities());
    for (Factor factor : factors) {
      if (factor.authority() == null || factor.issuedAt() == null) {
        throw new AuthenticationServiceException("Invalid persisted authentication factor");
      }
      authorities.add(
          FactorGrantedAuthority.withAuthority(factor.authority())
              .issuedAt(factor.issuedAt())
              .build());
    }
    Authentication authentication;
    if (remembered) {
      RememberMeAuthenticationToken token =
          new RememberMeAuthenticationToken(rememberKey, current, authorities);
      if (token.getKeyHash() != rememberKeyHash) {
        throw new AuthenticationServiceException("Saved login configuration changed");
      }
      authentication = token;
    } else {
      authentication =
          UsernamePasswordAuthenticationToken.authenticated(current, null, authorities);
    }
    return new SecurityContextImpl(authentication);
  }

  DurableAuthentication withCurrentCredentials(UserDetailsService users) {
    UserDetails current = users.loadUserByUsername(username);
    if (!current.isEnabled()) {
      throw new AuthenticationServiceException("Account unavailable");
    }
    return new DurableAuthentication(
        username, fingerprint(current), remembered, rememberKeyHash, factors);
  }

  private static String fingerprint(UserDetails user) {
    return fingerprint(user.getPassword());
  }

  private static String fingerprint(String password) {
    // Entfernte Login-Credentials sind zulässig, eine fehlende aktuelle DB-Version nicht.
    if (password == null || password.isEmpty()) {
      throw new AuthenticationServiceException("Account credential version unavailable");
    }
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(password.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException failure) {
      throw new IllegalStateException("SHA-256 unavailable", failure);
    }
  }

  @Override
  public String toString() {
    return "Durable authentication";
  }
}
