package de.derpeterson.app.security.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

class DurableAuthenticationTest {
  private static final String IDENTITY = "fixture@example.test";
  private final UserDetails current =
      User.withUsername(IDENTITY).password("fixture-encoded-version").roles("USER").build();

  private UserDetails account(String password) {
    UserDetails user = mock(UserDetails.class);
    when(user.getUsername()).thenReturn(IDENTITY);
    when(user.getPassword()).thenReturn(password);
    when(user.isEnabled()).thenReturn(true);
    when(user.isAccountNonLocked()).thenReturn(true);
    when(user.isAccountNonExpired()).thenReturn(true);
    when(user.isCredentialsNonExpired()).thenReturn(true);
    return user;
  }

  private Authentication authentication(UserDetails principal) {
    return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void erasedLoginCredentialsUseTheCurrentStoredVersion(String erased) {
    Authentication authenticated = authentication(account(erased));
    DurableAuthentication captured = DurableAuthentication.capture(authenticated, name -> current);
    assertEquals(IDENTITY, captured.username());
    assertNotNull(captured.restore(name -> current, "fixture-key").getAuthentication());
  }

  @Test
  void identityBeyondTheAccountColumnLimitIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DurableAuthentication("a".repeat(256), "0".repeat(64), false, 0, List.of()));
  }

  @Test
  void principalAndBothPasswordVersionsAreReadOnlyOnceDuringCapture() {
    UserDetails principal = account("fixture-encoded-version");
    when(principal.getPassword()).thenReturn("fixture-encoded-version", null);
    UserDetails stored = account("fixture-encoded-version");
    when(stored.getPassword()).thenReturn("fixture-encoded-version", null);
    UsernamePasswordAuthenticationToken authentication =
        mock(UsernamePasswordAuthenticationToken.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(principal, null);
    when(authentication.getAuthorities()).thenReturn(List.of());
    DurableAuthentication captured = DurableAuthentication.capture(authentication, name -> stored);
    assertEquals(IDENTITY, captured.username());
    verify(authentication).getPrincipal();
    verify(principal).getPassword();
    verify(stored).getPassword();
  }

  @Test
  void missingPrincipalIsRejectedAsAnAuthenticationFailure() {
    UsernamePasswordAuthenticationToken authentication =
        mock(UsernamePasswordAuthenticationToken.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    UserDetailsService users = name -> current;
    assertThrows(
        AuthenticationServiceException.class,
        () -> DurableAuthentication.capture(authentication, users));
  }

  @Test
  void aDifferentNonErasedLoginPasswordCannotAdoptTheCurrentVersion() {
    Authentication authenticated = authentication(account("different-version"));
    UserDetailsService users = name -> current;
    assertThrows(
        AuthenticationServiceException.class,
        () -> DurableAuthentication.capture(authenticated, users));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void captureRejectsMissingCurrentStoredCredentialsWithoutANullPointer(String missing) {
    Authentication authenticated = authentication(account(null));
    UserDetails unavailable = account(missing);
    UserDetailsService users = name -> unavailable;
    assertThrows(
        AuthenticationServiceException.class,
        () -> DurableAuthentication.capture(authenticated, users));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void restorationRejectsMissingCurrentStoredCredentialsWithoutANullPointer(String missing) {
    DurableAuthentication captured =
        DurableAuthentication.capture(authentication(current), name -> current);
    UserDetails unavailable = account(missing);
    UserDetailsService users = name -> unavailable;
    assertThrows(
        AuthenticationServiceException.class, () -> captured.restore(users, "fixture-key"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void passwordChangeCannotRetainAMissingCurrentVersion(String missing) {
    DurableAuthentication captured =
        DurableAuthentication.capture(authentication(current), name -> current);
    UserDetails unavailable = account(missing);
    UserDetailsService users = name -> unavailable;
    assertThrows(
        AuthenticationServiceException.class, () -> captured.withCurrentCredentials(users));
  }
}
