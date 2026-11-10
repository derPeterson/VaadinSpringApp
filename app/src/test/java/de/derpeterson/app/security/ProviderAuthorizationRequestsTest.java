package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.security.ExternalProviderSettings.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class ProviderAuthorizationRequestsTest {
  @Test
  void savingNullRemovesTheMatchingAuthorizationRequest() {
    ExternalProviderSettings settings = mock(ExternalProviderSettings.class);
    Credentials entity = new Credentials("google", "own-client", "own-secret", true, 0L);
    when(settings.credentials("google")).thenReturn(entity);
    ProviderAuthorizationRequests repository = new ProviderAuthorizationRequests(settings);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    OAuth2AuthorizationRequest authorization =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("own-client")
            .state("own-state")
            .attributes(attributes -> attributes.put("registration_id", "google"))
            .build();
    repository.saveAuthorizationRequest(authorization, mockHttpRequest, mockHttpResponse);
    mockHttpRequest.setParameter("state", "own-state");

    assertNotNull(repository.loadAuthorizationRequest(mockHttpRequest));
    repository.saveAuthorizationRequest(null, mockHttpRequest, mockHttpResponse);

    assertNull(repository.loadAuthorizationRequest(mockHttpRequest));
  }

  @ParameterizedTest
  @ValueSource(strings = {"unchanged", "disabled", "secret", "client", "version"})
  void callbackRequiresTheSameProviderConfigurationAndConsumesTheAttempt(String change) {
    ExternalProviderSettings settings = mock(ExternalProviderSettings.class);
    Credentials entity = new Credentials("google", "own-client", "own-secret", true, 0L);
    when(settings.credentials("google")).thenReturn(entity);
    ProviderAuthorizationRequests repository = new ProviderAuthorizationRequests(settings);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
    OAuth2AuthorizationRequest authorization =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("own-client")
            .redirectUri("https://localhost/login/oauth2/code/google")
            .state("own-state")
            .attributes(attributes -> attributes.put("registration_id", "google"))
            .build();
    repository.saveAuthorizationRequest(authorization, mockHttpRequest, mockHttpResponse);
    mockHttpRequest.setParameter("state", "own-state");
    when(settings.credentials("google"))
        .thenReturn(
            new Credentials(
                "google",
                change.equals("client") ? "replacement" : "own-client",
                change.equals("secret") ? "replacement" : "own-secret",
                !change.equals("disabled"),
                change.equals("version") ? 2L : 0L));
    if (change.equals("unchanged")) {
      assertNotNull(repository.removeAuthorizationRequest(mockHttpRequest, mockHttpResponse));
    } else {
      assertNull(repository.removeAuthorizationRequest(mockHttpRequest, mockHttpResponse));
    }
    assertNull(repository.removeAuthorizationRequest(mockHttpRequest, mockHttpResponse));
  }
}
