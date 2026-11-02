package de.derpeterson.app.security;

import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE;

import de.derpeterson.app.security.ExternalProviderSettings.Credentials;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@EnableConfigurationProperties(ExternalLoginProperties.class)
public class ExternalLoginConfiguration {
  public static final class Registrations
      implements ClientRegistrationRepository, Iterable<ClientRegistration> {
    private final Supplier<List<ClientRegistration>> clients;

    Registrations(List<ClientRegistration> clients) {
      this.clients = () -> List.copyOf(clients);
    }

    Registrations(Supplier<List<ClientRegistration>> clients) {
      this.clients = clients;
    }

    @Override
    public ClientRegistration findByRegistrationId(String id) {
      return clients.get().stream()
          .filter(client -> client.getRegistrationId().equals(id))
          .findFirst()
          .orElse(null);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
      return clients.get().iterator();
    }

    public List<String> providers() {
      return clients.get().stream().map(ClientRegistration::getRegistrationId).toList();
    }
  }

  @Bean
  public Registrations managedExternalRegistrations(
      ExternalLoginProperties properties, ExternalProviderSettings settings) {
    return new Registrations(
        () -> {
          ExternalLoginProperties current = new ExternalLoginProperties();
          for (String provider : ExternalProviderSettings.PROVIDERS) {
            Credentials stored = settings.credentials(provider);
            ExternalLoginProperties.Provider target =
                provider.equals(GOOGLE) ? current.getGoogle() : current.getMicrosoft();
            ExternalLoginProperties.Provider defaults =
                provider.equals(GOOGLE) ? properties.getGoogle() : properties.getMicrosoft();
            if (stored.enabled()) {
              target.setClientId(stored.clientId());
              target.setClientSecret(stored.clientSecret());
              target.setEnabled(true);
            }
            // Endpunkte stammen ausschließlich aus der Serverkonfiguration, nie aus Admin-Eingaben.
            target.setAuthorizationUri(defaults.getAuthorizationUri());
            target.setTokenUri(defaults.getTokenUri());
            target.setJwkSetUri(defaults.getJwkSetUri());
          }
          List<ClientRegistration> result = new ArrayList<>();
          externalRegistrations(current).forEach(result::add);
          return List.copyOf(result);
        });
  }

  /** Unveränderliche Registrierungen für isolierte Protokolltests. */
  public Registrations externalRegistrations(ExternalLoginProperties properties) {
    List<ClientRegistration> clients = new ArrayList<>();
    if (properties.getGoogle().configured()) {
      clients.add(
          registration(
              GOOGLE,
              properties.getGoogle(),
              "https://accounts.google.com",
              "https://accounts.google.com/o/oauth2/v2/auth",
              "https://oauth2.googleapis.com/token",
              "https://www.googleapis.com/oauth2/v3/certs"));
    }
    if (properties.getMicrosoft().configured()) {
      clients.add(
          registration(
              "microsoft",
              properties.getMicrosoft(),
              "https://login.microsoftonline.com/common/v2.0",
              "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
              "https://login.microsoftonline.com/common/oauth2/v2.0/token",
              "https://login.microsoftonline.com/common/discovery/v2.0/keys"));
    }
    return new Registrations(clients);
  }

  private ClientRegistration registration(
      String id,
      ExternalLoginProperties.Provider properties,
      String issuer,
      String authorization,
      String token,
      String keys) {
    return ClientRegistration.withRegistrationId(id)
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "profile", "email")
        .issuerUri(issuer)
        .authorizationUri(value(properties.getAuthorizationUri(), authorization))
        .tokenUri(value(properties.getTokenUri(), token))
        .jwkSetUri(value(properties.getJwkSetUri(), keys))
        .userNameAttributeName("sub")
        .clientName(id.equals(GOOGLE) ? "Google" : "Microsoft")
        .build();
  }

  private String value(String configured, String fallback) {
    return configured == null || configured.isBlank() ? fallback : configured;
  }
}
