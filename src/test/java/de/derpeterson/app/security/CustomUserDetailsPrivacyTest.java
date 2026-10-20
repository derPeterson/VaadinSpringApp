package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.UserRepository.SecurityUserRow;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class CustomUserDetailsPrivacyTest {
  @Test
  void missingAccountExceptionDoesNotCarryTheSubmittedAddress() {
    UserRepository users = mock(UserRepository.class);
    String submitted = "unknown-account@example.test";
    when(users.findPasswordSecurityUser(submitted)).thenReturn(List.of());
    CustomUserDetailsService details = new CustomUserDetailsService(users);

    UsernameNotFoundException failure =
        assertThrows(
            UsernameNotFoundException.class, () -> details.loadPasswordUserByUsername(submitted));

    assertFalse(failure.getMessage().contains(submitted));
  }

  @Test
  void realPasswordProviderReportsTheSameFailureForMissingAccountAndWrongPassword() {
    UserRepository users = mock(UserRepository.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    SecurityUserRow existing = mock(SecurityUserRow.class);
    when(existing.getEmail()).thenReturn("existing@example.test");
    when(existing.getPassword()).thenReturn(encoder.encode("Correct-fixture!"));
    when(existing.isEnabled()).thenReturn(true);
    when(existing.getRole()).thenReturn(RoleType.ROLE_USER);
    when(users.findPasswordSecurityUser("existing@example.test")).thenReturn(List.of(existing));
    when(users.findPasswordSecurityUser("missing@example.test")).thenReturn(List.of());
    CustomUserDetailsService details = new CustomUserDetailsService(users);
    DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(details::loadPasswordUserByUsername);
    provider.setPasswordEncoder(encoder);
    UsernamePasswordAuthenticationToken wrongPassword =
        UsernamePasswordAuthenticationToken.unauthenticated(
            "existing@example.test", "Wrong-fixture!");
    UsernamePasswordAuthenticationToken missingAccount =
        UsernamePasswordAuthenticationToken.unauthenticated(
            "missing@example.test", "Wrong-fixture!");

    BadCredentialsException wrong =
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(wrongPassword));
    BadCredentialsException missing =
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(missingAccount));

    assertEquals(wrong.getMessage(), missing.getMessage());
  }
}
