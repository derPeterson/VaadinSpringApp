package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;

class TwoFactorHttpFilterTest {
  private final SecurityService securityService = mock(SecurityService.class);
  private final TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
  private final TwoFactorHttpFilter twoFactorHttpFilter =
      new TwoFactorHttpFilter(securityService, twoFactorAuthorization);

  @ParameterizedTest
  @CsvSource({
    "SETUP,/home,/two-factor?setup",
    "SETUP,/admin,/two-factor?setup",
    "SETUP,/account,/two-factor?setup",
    "SETUP,/recovery-help,/two-factor?setup",
    "RECOVERY,/home,/two-factor?recover",
    "ACCOUNT_HELP,/account,/recovery-help",
    "LOGIN_HELP,/two-factor,/recovery-help"
  })
  void activeFlowBlocksDirectNavigationEvenAfterSuccessfulAuthentication(
      TwoFactorFlow flow, String path, String target) throws IOException, ServletException {
    MockHttpServletRequest request = authenticatedRequest(path);
    flow.enter(request);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    twoFactorHttpFilter.doFilter(request, response, chain);

    assertEquals(target, response.getRedirectedUrl());
    assertNull(chain.getRequest());
  }

  @Test
  void directNavigationIsAvailableAgainAfterCompletingTheFlow()
      throws IOException, ServletException {
    MockHttpServletRequest request = authenticatedRequest("/account");
    TwoFactorFlow.SETUP.enter(request);
    TwoFactorFlow.finish(request);
    MockFilterChain chain = new MockFilterChain();

    twoFactorHttpFilter.doFilter(request, new MockHttpServletResponse(), chain);

    assertEquals(request, chain.getRequest());
  }

  @ParameterizedTest
  @CsvSource({
    "SETUP,two-factor-recovery-codes.pdf,true",
    "SETUP,avatar.png,false",
    "ACCOUNT_HELP,two-factor-recovery-codes.pdf,false"
  })
  void activeFlowOnlyPassesTheNamedRecoveryDownloadToItsAuthorizationHandler(
      TwoFactorFlow flow, String name, boolean permitted) throws IOException, ServletException {
    MockHttpServletRequest request =
        authenticatedRequest(
            "/VAADIN/dynamic/resource/0/c72f14a9-5489-45fe-b4cc-100312a972b1/" + name);
    flow.enter(request);
    MockFilterChain chain = new MockFilterChain();

    twoFactorHttpFilter.doFilter(request, new MockHttpServletResponse(), chain);

    assertEquals(permitted ? request : null, chain.getRequest());
  }

  private MockHttpServletRequest authenticatedRequest(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setServletPath(path);
    when(securityService.getAuthenticatedUser(request))
        .thenReturn(
            Optional.of(User.withUsername("fixture").password("unused").roles("USER").build()));
    when(twoFactorAuthorization.sessionConfirmed(request)).thenReturn(true);
    when(twoFactorAuthorization.administrationConfirmed(request)).thenReturn(true);
    return request;
  }
}
