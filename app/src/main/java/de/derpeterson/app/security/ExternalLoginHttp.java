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
  private final ExternalLoginConfiguration.Registrations providerRegistrations;
  private final ExternalLoginProperties externalLoginProperties;
  private final ExternalLoginFlow externalLoginFlow;
  private final ExternalProviderSettings externalProviderSettings;
  private final SecurityService securityService;

  public void configure(HttpSecurity http) {
    OidcUserService userService = new OidcUserService();
    userService.setRetrieveUserInfo(request -> false);
    OidcAuthorizationCodeAuthenticationProvider provider =
        new OidcAuthorizationCodeAuthenticationProvider(
            new RestClientAuthorizationCodeTokenResponseClient(), userService);
    provider.setJwtDecoderFactory(new ExternalIdTokenDecoders(externalLoginProperties));
    DefaultOAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            providerRegistrations, "/oauth2/authorization");
    defaultAuthorizationRequestResolver.setAuthorizationRequestCustomizer(
        OAuth2AuthorizationRequestCustomizers.withPkce());
    http.oauth2Login(oauth -> {
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
          .clientRegistrationRepository(providerRegistrations)
          .loginPage("/login")
          .authorizedClientRepository(new DiscardedClients())
          .securityContextRepository(new NullSecurityContextRepository())
          .authorizationEndpoint(endpoint ->
              endpoint
                  .authorizationRequestResolver(new Resolver(defaultAuthorizationRequestResolver))
                  .authorizationRequestRepository(
                      new ProviderAuthorizationRequests(externalProviderSettings)))
          .successHandler((request, response, authentication) -> {
            try {
              externalLoginFlow.complete(request, response, authentication);
            } catch (RuntimeException failure) {
              log.warn(
                  "External sign-in completion failed: reason={}",
                  proofProblem(failure),
                  LogSafety.safeFailure(failure));
              fail(request, response, failure);
            }
          })
          .failureHandler((request, response, failure) -> {
            log.warn("External sign-in was rejected", LogSafety.safeFailure(failure));
            fail(request, response, failure);
          });
    });
  }

  private void fail(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      RuntimeException externalLoginFailure)
      throws IOException {
    boolean hasAuthenticatedAccount = securityService.getAuthenticatedUser(httpRequest).isPresent();
    TwoFactorFlow activeTwoFactorFlow =
        hasAuthenticatedAccount ? TwoFactorFlow.current(httpRequest) : null;

    externalLoginFlow.cancel(httpRequest);
    if (!hasAuthenticatedAccount) {
      httpResponse.sendRedirect("/external-registration?error=external");
      return;
    }

    String failureRedirectLocation =
        activeTwoFactorFlow == null
            ? AppRouteConstants.ACCOUNT_PORTAL_LOCATION
            : activeTwoFactorFlow.location();
    httpResponse.sendRedirect(
        failureRedirectLocation
            + (failureRedirectLocation.contains("?") ? "&" : "?")
            + ExternalLoginFlow.ERROR_PARAMETER
            + "="
            + proofProblem(externalLoginFailure).name());
  }

  private ExternalLoginFlow.ProofProblem proofProblem(RuntimeException failure) {
    return failure instanceof ExternalLoginFlow.ProofRejected rejected
        ? rejected.problem()
        : ExternalLoginFlow.ProofProblem.UNAVAILABLE;
  }

  private final class Resolver implements OAuth2AuthorizationRequestResolver {
    private final DefaultOAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

    private Resolver(
        DefaultOAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver) {
      this.defaultAuthorizationRequestResolver = defaultAuthorizationRequestResolver;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
      return bind(request, defaultAuthorizationRequestResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String id) {
      return bind(request, defaultAuthorizationRequestResolver.resolve(request, id));
    }

    private OAuth2AuthorizationRequest bind(
        HttpServletRequest request, OAuth2AuthorizationRequest authorization) {
      if (authorization == null) {
        return null;
      }
      String id = authorization.getAttribute("registration_id");
      ExternalLoginFlow.Attempt attempt =
          externalLoginFlow.bindAuthorization(request, id, authorization.getState());
      if (attempt.mode() != ExternalLoginFlow.Mode.PROOF) {
        return authorization;
      }
      return OAuth2AuthorizationRequest.from(authorization)
          .additionalParameters(parameters -> {
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
