package de.derpeterson.app.service;

import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Saves through the real Spring repository and reads from an independent subsequent request. */
class LoginContextPersistenceTest {
    @AfterEach void cleanup() {SecurityContextHolder.clearContext();}

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void customLoginPersistsAuthenticationBeforeRememberMeAndFollowingRequests(boolean remember) {
        var repository = new HttpSessionSecurityContextRepository();
        var rememberService = mock(PersistentTokenBasedRememberMeServices.class);
        var service = new SecurityService(rememberService, mock(UserService.class), mock(UserRepository.class),
                mock(CustomUserDetailsService.class), repository);
        var principal = User.withUsername("context@example.test").password("test-hash").roles("USER").build();
        var login = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        if (remember) doAnswer(call -> {
            // The login hook already sees the durable session context, before its cookie is issued.
            assertTrue(repository.containsContext(login));
            return null;
        }).when(rememberService).loginSuccess(any(), any(), any());
        service.storeAuthenticatedUser(login, response, principal, remember);
        SecurityContextHolder.clearContext();
        var following = new MockHttpServletRequest(); following.setSession(login.getSession(false));
        var loaded = repository.loadDeferredContext(following).get().getAuthentication();
        assertNotNull(loaded); assertTrue(loaded.isAuthenticated()); assertSame(principal, loaded.getPrincipal());
        assertEquals("ROLE_USER", loaded.getAuthorities().iterator().next().getAuthority());
        if (remember) verify(rememberService).loginSuccess(eq(login), eq(response), any());
        else verify(rememberService, never()).loginSuccess(any(), any(), any());
    }
}
