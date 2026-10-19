package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/** Saves through the real Spring repository and reads from an independent subsequent request. */
class LoginContextPersistenceTest {
  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void customLoginPersistsAuthenticationBeforeRememberMeAndFollowingRequests(boolean remember) {
    HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();
    PersistentTokenBasedRememberMeServices rememberService =
        mock(PersistentTokenBasedRememberMeServices.class);
    SecurityService service =
        new SecurityService(
            rememberService,
            mock(UserService.class),
            mock(UserRepository.class),
            mock(CustomUserDetailsService.class),
            repository,
            new de.derpeterson.app.security.SessionAdmissionService(
                configuredLimit(),
                new org.springframework.security.core.session.SessionRegistryImpl()));
    UserDetails principal =
        User.withUsername("context@example.test").password("test-hash").roles("USER").build();
    MockHttpServletRequest login = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    if (remember) {
      doAnswer(
              call -> {
                // The login hook already sees the durable session context, before its cookie is
                // issued.
                assertTrue(repository.containsContext(login));
                assertEquals(
                    "true",
                    call.getArgument(0, jakarta.servlet.http.HttpServletRequest.class)
                        .getParameter("remember-me"));
                return null;
              })
          .when(rememberService)
          .loginSuccess(any(), any(), any());
    }
    service.storeAuthenticatedUser(login, response, principal, remember);
    SecurityContextHolder.clearContext();
    MockHttpServletRequest following = new MockHttpServletRequest();
    following.setSession(login.getSession(false));
    Authentication loaded = repository.loadDeferredContext(following).get().getAuthentication();
    assertNotNull(loaded);
    assertNull(loaded.getCredentials());
    verify(rememberService, never()).setAlwaysRemember(anyBoolean());
    assertTrue(loaded.isAuthenticated());
    assertSame(principal, loaded.getPrincipal());
    assertEquals("ROLE_USER", loaded.getAuthorities().iterator().next().getAuthority());
    if (remember) {
      verify(rememberService).loginSuccess(any(), eq(response), any());
    } else {
      verify(rememberService, never()).loginSuccess(any(), any(), any());
    }
  }

  private ConfigService configuredLimit() {
    ConfigService config = mock(ConfigService.class);
    when(config.getInteger(de.derpeterson.app.model.enums.ConfigEntry.MAX_SESSIONS_PER_USER))
        .thenReturn(3);
    return config;
  }
}
