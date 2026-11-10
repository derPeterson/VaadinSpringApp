package de.derpeterson.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Ein während der Anbieteränderung laufender Login muss mit den neuen Einstellungen neu beginnen.
 */
final class ProviderAuthorizationRequests
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
  private static final String REVISION =
      ProviderAuthorizationRequests.class.getName() + ".revision";
  private final HttpSessionOAuth2AuthorizationRequestRepository authorizationRequestRepository =
      new HttpSessionOAuth2AuthorizationRequestRepository();
  private final ExternalProviderSettings externalProviderSettings;

  ProviderAuthorizationRequests(ExternalProviderSettings externalProviderSettings) {
    this.externalProviderSettings = externalProviderSettings;
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return authorizationRequestRepository.loadAuthorizationRequest(request);
  }

  @Override
  public void saveAuthorizationRequest(
      @Nullable OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    if (authorizationRequest == null) {
      authorizationRequestRepository.removeAuthorizationRequest(httpRequest, httpResponse);
      return;
    }

    String providerRegistrationId = authorizationRequest.getAttribute("registration_id");
    authorizationRequest =
        OAuth2AuthorizationRequest.from(authorizationRequest)
            .attributes(attributes ->
                attributes.put(
                    REVISION,
                    ExternalProviderSettings.revision(
                        externalProviderSettings.credentials(providerRegistrationId))))
            .build();

    authorizationRequestRepository.saveAuthorizationRequest(
        authorizationRequest, httpRequest, httpResponse);
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    OAuth2AuthorizationRequest authorizationRequest =
        authorizationRequestRepository.removeAuthorizationRequest(httpRequest, httpResponse);
    if (authorizationRequest == null) {
      return null;
    }
    String providerRegistrationId = authorizationRequest.getAttribute("registration_id");
    String storedProviderRevision = authorizationRequest.getAttribute(REVISION);

    return ExternalProviderSettings.revision(
                externalProviderSettings.credentials(providerRegistrationId))
            .equals(storedProviderRevision)
        ? authorizationRequest
        : null;
  }
}
