package de.derpeterson.app.security;

import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.helper.logging.LogSafety;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.stereotype.Component;

/** Framework-Codefluss; erst der lokale Abschluss vergibt eine zugelassene Portalsitzung. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalLoginHttp {
  private final ExternalLoginConfiguration.Registrations clients;
  private final ExternalLoginProperties properties;
  private final ExternalLoginFlow flow;
  private final ExternalProviderSettings settings;
  private final SecurityService security;

  public void configure(HttpSecurity http) {
    OidcUserService userService = new OidcUserService();
    userService.setRetrieveUserInfo(request -> false);
    OidcAuthorizationCodeAuthenticationProvider provider =
        new OidcAuthorizationCodeAuthenticationProvider(
            new RestClientAuthorizationCodeTokenResponseClient(), userService);
    provider.setJwtDecoderFactory(new ExternalIdTokenDecoders(properties));
    DefaultOAuth2AuthorizationRequestResolver base =
        new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
    base.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    http.oauth2Login(
        oauth -> {
          oauth.addObjectPostProcessor(
              new ObjectPostProcessor<OAuth2LoginAuthenticationFilter>() {
                @Override
                public <O extends OAuth2LoginAuthenticationFilter> O postProcess(O filter) {
                  filter.setAuthenticationManager(new ProviderManager(provider));
                  filter.setSessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy());
                  // Der gemeinsame HTTP-Kontext darf das vorläufige OIDC-Token nicht speichern.
                  // Erst flow.complete vergibt nach den lokalen Kontoprüfungen eine Portalsitzung.
                  filter.setSecurityContextRepository(new NullSecurityContextRepository());
                  return filter;
                }
              });
          oauth
              .clientRegistrationRepository(clients)
              .loginPage("/login")
              .authorizedClientRepository(new DiscardedClients())
              .securityContextRepository(new NullSecurityContextRepository())
              .authorizationEndpoint(
                  endpoint ->
                      endpoint
                          .authorizationRequestResolver(new Resolver(base))
                          .authorizationRequestRepository(
                              new ProviderAuthorizationRequests(settings)))
              .successHandler(
                  (request, response, authentication) -> {
                    try {
                      flow.complete(request, response, authentication);
                    } catch (RuntimeException failure) {
                      log.warn(
                          "External sign-in completion failed: reason={}",
                          proofProblem(failure),
                          LogSafety.safeFailure(failure));
                      fail(request, response, failure);
                    }
                  })
              .failureHandler(
                  (request, response, failure) -> {
                    log.warn("External sign-in was rejected", LogSafety.safeFailure(failure));
                    fail(request, response, failure);
                  });
        });
  }

  private void fail(
      HttpServletRequest request, HttpServletResponse response, RuntimeException failure)
      throws IOException {
    boolean localAccount = security.getAuthenticatedUser(request).isPresent();
    flow.cancel(request);
    response.sendRedirect(
        localAccount
            ? AppRouteConstants.ACCOUNT_PORTAL_LOCATION
                + "&"
                + ExternalLoginFlow.ERROR_PARAMETER
                + "="
                + proofProblem(failure).name()
            : "/external-registration?error=external");
  }

  private ExternalLoginFlow.ProofProblem proofProblem(RuntimeException failure) {
    return failure instanceof ExternalLoginFlow.ProofRejected rejected
        ? rejected.problem()
        : ExternalLoginFlow.ProofProblem.UNAVAILABLE;
  }

  private final class Resolver implements OAuth2AuthorizationRequestResolver {
    private final DefaultOAuth2AuthorizationRequestResolver base;

    private Resolver(DefaultOAuth2AuthorizationRequestResolver base) {
      this.base = base;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
      return bind(request, base.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String id) {
      return bind(request, base.resolve(request, id));
    }

    private OAuth2AuthorizationRequest bind(
        HttpServletRequest request, OAuth2AuthorizationRequest authorization) {
      if (authorization == null) {
        return null;
      }
      String id = authorization.getAttribute("registration_id");
      ExternalLoginFlow.Attempt attempt =
          flow.bindAuthorization(request, id, authorization.getState());
      if (attempt.mode() != ExternalLoginFlow.Mode.PROOF) {
        return authorization;
      }
      return OAuth2AuthorizationRequest.from(authorization)
          .additionalParameters(
              parameters -> {
                if (id.equals("google")) {
                  parameters.put("prompt", "select_account consent");
                } else {
                  parameters.put("max_age", "0");
                  parameters.put("claims", "{\"id_token\":{\"auth_time\":{\"essential\":true}}}");
                  parameters.put("prompt", "login");
                }
              })
          .build();
    }
  }

  /** Die Anwendung benötigt nach der Identitätsprüfung keinen Anbieter-API-Zugang. */
  private static final class DiscardedClients implements OAuth2AuthorizedClientRepository {
    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
        String id, Authentication principal, HttpServletRequest request) {
      return null;
    }

    @Override
    public void saveAuthorizedClient(
        OAuth2AuthorizedClient client,
        Authentication principal,
        HttpServletRequest request,
        HttpServletResponse response) {
      // Tokens werden nach dem Identitätsnachweis absichtlich nicht gespeichert.
    }

    @Override
    public void removeAuthorizedClient(
        String id,
        Authentication principal,
        HttpServletRequest request,
        HttpServletResponse response) {
      // Ohne gespeicherte Anbieterzugänge gibt es hier nichts zu entfernen.
    }
  }
}
