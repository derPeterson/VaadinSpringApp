package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Deterministic interleavings at the session/context boundary, without timing or a server. */
class SecurityServiceSessionStateTest {
  private CustomUserDetailsService users;
  private SecurityService security;
  private UserDetails principal;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    users = mock(CustomUserDetailsService.class);
    security =
        new SecurityService(
            mock(PersistentTokenBasedRememberMeServices.class),
            mock(UserService.class),
            mock(UserRepository.class),
            users,
            new HttpSessionSecurityContextRepository(),
            mock(SessionAdmissionService.class));
    principal =
        User.withUsername("session-state@example.test").password("test-hash").roles("USER").build();
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void contextRemovedBetweenReadsRejectsTheRequestWithoutNullPointerException() {
    MockHttpSession session = spy(new MockHttpSession());
    SecurityContext context = context(passwordAuthentication());
    // A logout removes the attribute after the first snapshot; the refresh must fail closed.
    when(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
        .thenReturn(context)
        .thenReturn(null);
    MockHttpServletRequest request = request(session);

    assertFalse(security.hasCurrentSessionPrivileges(request));

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(users);
  }

  @Test
  void authenticationClearedInsideAnExistingContextRejectsTheRequestWithoutNullPointerException() {
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(passwordAuthentication()).thenReturn(null);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    MockHttpServletRequest request = request(session);

    assertFalse(security.hasCurrentSessionPrivileges(request));

    assertTrue(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(users);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void stablePasswordAndRememberMeSessionsStillRefreshTheirUser(boolean remembered) {
    Authentication authentication =
        remembered
            ? new RememberMeAuthenticationToken("test-key", principal, principal.getAuthorities())
            : passwordAuthentication();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context(authentication));
    when(users.loadUserByUsername(principal.getUsername())).thenReturn(principal);

    assertTrue(security.hasCurrentSessionPrivileges(request(session)));

    assertFalse(session.isInvalid());
    assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    assertSame(principal, session.getAttribute("authenticatedUser"));
  }

  @Test
  void anonymousSessionRemainsForTheFollowingAuthorizationFilterToDecide() {
    MockHttpSession session = new MockHttpSession();

    assertTrue(security.hasCurrentSessionPrivileges(request(session)));

    assertFalse(session.isInvalid());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(users);
  }

  private Authentication passwordAuthentication() {
    return UsernamePasswordAuthenticationToken.authenticated(
        principal, null, principal.getAuthorities());
  }

  private SecurityContext context(Authentication authentication) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }

  private MockHttpServletRequest request(MockHttpSession session) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    return request;
  }
}
