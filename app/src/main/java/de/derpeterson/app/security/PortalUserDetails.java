package de.derpeterson.app.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/** Lokale Kontorechte und Zugangsversion unabhängig von einem vorhandenen Portalpasswort. */
public final class PortalUserDetails extends User {
  private final String credentialVersion;

  public PortalUserDetails(
      String email,
      String password,
      String accessVersion,
      boolean enabled,
      Collection<? extends GrantedAuthority> authorities) {
    if (accessVersion == null || accessVersion.isBlank()) {
      throw new IllegalArgumentException("Account access version required");
    }
    super(email, password == null ? "" : password, enabled, true, true, true, authorities);
    credentialVersion = credentialVersion(accessVersion, password);
  }

  public String credentialVersion() {
    return credentialVersion;
  }

  public static String credentialVersion(String accessVersion, String password) {
    return digest(accessVersion + ":" + (password == null ? "" : password));
  }

  public static String version(UserDetails user) {
    return user instanceof PortalUserDetails portal
        ? portal.credentialVersion()
        : digest(user.getPassword());
  }

  private static String digest(String credentialVersionInput) {
    if (credentialVersionInput == null || credentialVersionInput.isBlank()) {
      throw new AuthenticationServiceException("Account credential version unavailable");
    }

    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(credentialVersionInput.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
