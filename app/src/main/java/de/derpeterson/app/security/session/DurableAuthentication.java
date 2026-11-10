package de.derpeterson.app.security.session;

import static de.derpeterson.app.security.mfa.SecondFactorAuthorities.RECOVERY_CODE;
import static de.derpeterson.app.security.mfa.SecondFactorAuthorities.TOTP;

import de.derpeterson.app.security.ExternalAuthenticationToken;
import de.derpeterson.app.security.PortalUserDetails;
import de.derpeterson.app.security.SignInMethod;
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
    List<Factor> factors,
    String externalProvider) {
  public DurableAuthentication(
      String username,
      String credentialVersion,
      boolean remembered,
      int rememberKeyHash,
      List<Factor> factors) {
    this(username, credentialVersion, remembered, rememberKeyHash, factors, null);
  }

  public record Factor(String authority, Instant issuedAt) {}

  public SignInMethod signInMethod() {
    if (externalProvider != null) {
      return SignInMethod.provider(externalProvider);
    }

    return remembered ? SignInMethod.SAVED_LOGIN : SignInMethod.PORTAL;
  }

  boolean hasPasswordProof() {
    return factors.stream()
        .anyMatch(factor -> FactorGrantedAuthority.PASSWORD_AUTHORITY.equals(factor.authority()));
  }

  public DurableAuthentication {
    if (username == null
        || username.isBlank()
        || username.length() > 255
        || credentialVersion == null
        || credentialVersion.length() != 64
        || factors == null
        || factors.size() > 16
        || externalProvider != null
            && (remembered
                || factors.stream()
                    .anyMatch(factor ->
                        !(TOTP.equals(factor.authority())
                            || RECOVERY_CODE.equals(factor.authority())))
                || !(externalProvider.equals("google") || externalProvider.equals("microsoft")))) {
      throw new IllegalArgumentException("Invalid durable authentication");
    }
    factors = List.copyOf(factors);
  }

  public static DurableAuthentication capture(
      Authentication authentication, UserDetailsService userDetailsService) {
    if (!authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof UserDetails authenticatedUserDetails)
        || !(authentication instanceof UsernamePasswordAuthenticationToken
            || authentication instanceof RememberMeAuthenticationToken
            || authentication instanceof ExternalAuthenticationToken)) {
      throw new AuthenticationServiceException("Unsupported durable authentication");
    }

    UserDetails currentUserDetails =
        userDetailsService.loadUserByUsername(authenticatedUserDetails.getUsername());
    String authenticatedPassword = authenticatedUserDetails.getPassword();
    String currentCredentialVersion = fingerprint(currentUserDetails);
    if (!currentUserDetails.isEnabled()
        || !currentUserDetails.isAccountNonLocked()
        || !currentUserDetails.isAccountNonExpired()
        || !currentUserDetails.isCredentialsNonExpired()
        // Spring/Flow entfernen Credentials nach erfolgreicher Passwortprüfung.
        || authenticatedUserDetails instanceof PortalUserDetails
            && !currentCredentialVersion.equals(fingerprint(authenticatedUserDetails))
        || !(authenticatedUserDetails instanceof PortalUserDetails)
            && authenticatedPassword != null
            && !authenticatedPassword.isEmpty()
            && !currentCredentialVersion.equals(fingerprint(authenticatedPassword))) {
      throw new AuthenticationServiceException("Account unavailable");
    }

    List<Factor> capturedFactors =
        authentication.getAuthorities().stream()
            .filter(FactorGrantedAuthority.class::isInstance)
            .map(FactorGrantedAuthority.class::cast)
            .map(factor -> new Factor(factor.getAuthority(), factor.getIssuedAt()))
            .toList();

    boolean rememberMeAuthentication = authentication instanceof RememberMeAuthenticationToken;
    int rememberMeKeyHash =
        rememberMeAuthentication
            ? ((RememberMeAuthenticationToken) authentication).getKeyHash()
            : 0;

    return new DurableAuthentication(
        currentUserDetails.getUsername(),
        currentCredentialVersion,
        rememberMeAuthentication,
        rememberMeKeyHash,
        capturedFactors,
        authentication instanceof ExternalAuthenticationToken externalAuthenticationToken
            ? externalAuthenticationToken.provider()
            : null);
  }

  public SecurityContext restore(
      UserDetailsService userDetailsService, String configuredRememberMeKey) {
    UserDetails currentUserDetails = userDetailsService.loadUserByUsername(username);
    if (!currentUserDetails.isEnabled()
        || !currentUserDetails.isAccountNonLocked()
        || !currentUserDetails.isAccountNonExpired()
        || !currentUserDetails.isCredentialsNonExpired()
        || !MessageDigest.isEqual(
            credentialVersion.getBytes(StandardCharsets.US_ASCII),
            fingerprint(currentUserDetails).getBytes(StandardCharsets.US_ASCII))) {
      throw new AuthenticationServiceException("Persisted account access is no longer valid");
    }

    List<GrantedAuthority> restoredAuthorities =
        new ArrayList<>(currentUserDetails.getAuthorities());
    for (Factor factor : factors) {
      if (factor.authority() == null || factor.issuedAt() == null) {
        throw new AuthenticationServiceException("Invalid persisted authentication factor");
      }
      restoredAuthorities.add(
          FactorGrantedAuthority.withAuthority(factor.authority())
              .issuedAt(factor.issuedAt())
              .build());
    }

    Authentication restoredAuthentication;
    if (remembered) {
      RememberMeAuthenticationToken rememberMeToken =
          new RememberMeAuthenticationToken(
              configuredRememberMeKey, currentUserDetails, restoredAuthorities);
      if (rememberMeToken.getKeyHash() != rememberKeyHash) {
        throw new AuthenticationServiceException("Saved login configuration changed");
      }
      restoredAuthentication = rememberMeToken;
    } else if (externalProvider != null) {
      restoredAuthentication =
          new ExternalAuthenticationToken(
              currentUserDetails, externalProvider, restoredAuthorities);
    } else {
      restoredAuthentication =
          UsernamePasswordAuthenticationToken.authenticated(
              currentUserDetails, null, restoredAuthorities);
    }

    return new SecurityContextImpl(restoredAuthentication);
  }

  DurableAuthentication withCurrentCredentials(UserDetailsService userDetailsService) {
    UserDetails currentUserDetails = userDetailsService.loadUserByUsername(username);
    if (!currentUserDetails.isEnabled()) {
      throw new AuthenticationServiceException("Account unavailable");
    }
    return new DurableAuthentication(
        username,
        fingerprint(currentUserDetails),
        remembered,
        rememberKeyHash,
        factors,
        externalProvider);
  }

  private static String fingerprint(UserDetails userDetails) {
    return PortalUserDetails.version(userDetails);
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
