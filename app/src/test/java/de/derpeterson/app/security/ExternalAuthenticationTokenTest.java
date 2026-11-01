package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class ExternalAuthenticationTokenTest {
  @Test
  void equalityIncludesTheVerifiedProviderAndLocalAuthorities() {
    UserDetails user = User.withUsername("own@example.test").password("").roles("USER").build();
    ExternalAuthenticationToken google = new ExternalAuthenticationToken(user, "google");
    ExternalAuthenticationToken same = new ExternalAuthenticationToken(user, "google");
    ExternalAuthenticationToken microsoft = new ExternalAuthenticationToken(user, "microsoft");
    ExternalAuthenticationToken noRoles =
        new ExternalAuthenticationToken(user, "google", List.of());

    assertEquals(google, same);
    assertEquals(google.hashCode(), same.hashCode());
    assertNotEquals(google, microsoft);
    assertNotEquals(google, noRoles);
    assertNotEquals(google, null);
    assertThrows(
        IllegalArgumentException.class, () -> new ExternalAuthenticationToken(user, "unknown"));
  }
}
