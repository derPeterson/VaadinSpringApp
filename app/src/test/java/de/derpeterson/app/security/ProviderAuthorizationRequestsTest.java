package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.ExternalProviderEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class ProviderAuthorizationRequestsTest {
  @ParameterizedTest
  @ValueSource(strings = {"unchanged", "disabled", "secret", "client", "version"})
  void callbackRequiresTheSameProviderConfigurationAndConsumesTheAttempt(String change) {
    ExternalProviderSettings settings = mock(ExternalProviderSettings.class);
    ExternalProviderEntity entity = new ExternalProviderEntity();
    entity.setProvider("google");
    entity.setClientId("own-client");
    entity.setClientSecret("own-secret");
    entity.setEnabled(true);
    entity.setVersion(0L);
    when(settings.credentials("google")).thenReturn(entity);
    ProviderAuthorizationRequests repository = new ProviderAuthorizationRequests(settings);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    OAuth2AuthorizationRequest authorization =
        OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("own-client")
            .redirectUri("https://localhost/login/oauth2/code/google")
            .state("own-state")
            .attributes(attributes -> attributes.put("registration_id", "google"))
            .build();
    repository.saveAuthorizationRequest(authorization, request, response);
    request.setParameter("state", "own-state");
    switch (change) {
      case "disabled" -> entity.setEnabled(false);
      case "secret" -> entity.setClientSecret("replacement");
      case "client" -> entity.setClientId("replacement");
      case "version" -> entity.setVersion(2L);
      default -> {}
    }
    if (change.equals("unchanged")) {
      assertNotNull(repository.removeAuthorizationRequest(request, response));
    } else {
      assertNull(repository.removeAuthorizationRequest(request, response));
    }
    assertNull(repository.removeAuthorizationRequest(request, response));
  }
}
