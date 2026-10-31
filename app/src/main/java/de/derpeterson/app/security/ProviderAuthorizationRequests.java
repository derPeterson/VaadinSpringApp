package de.derpeterson.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
  private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
      new HttpSessionOAuth2AuthorizationRequestRepository();
  private final ExternalProviderSettings settings;

  ProviderAuthorizationRequests(ExternalProviderSettings settings) {
    this.settings = settings;
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return delegate.loadAuthorizationRequest(request);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorization,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorization != null) {
      String provider = authorization.getAttribute("registration_id");
      authorization =
          OAuth2AuthorizationRequest.from(authorization)
              .attributes(
                  attributes ->
                      attributes.put(
                          REVISION,
                          ExternalProviderSettings.revision(settings.credentials(provider))))
              .build();
    }
    delegate.saveAuthorizationRequest(authorization, request, response);
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorization =
        delegate.removeAuthorizationRequest(request, response);
    if (authorization == null) {
      return null;
    }
    String provider = authorization.getAttribute("registration_id");
    String revision = authorization.getAttribute(REVISION);
    return ExternalProviderSettings.revision(settings.credentials(provider)).equals(revision)
        ? authorization
        : null;
  }
}
