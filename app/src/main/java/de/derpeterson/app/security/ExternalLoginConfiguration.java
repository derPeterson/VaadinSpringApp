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
    private final Supplier<List<ClientRegistration>> clientRegistrationsSupplier;

    Registrations(List<ClientRegistration> clientRegistrationsSupplier) {
      this.clientRegistrationsSupplier = () -> List.copyOf(clientRegistrationsSupplier);
    }

    Registrations(Supplier<List<ClientRegistration>> clientRegistrationsSupplier) {
      this.clientRegistrationsSupplier = clientRegistrationsSupplier;
    }

    @Override
    public ClientRegistration findByRegistrationId(String id) {
      return clientRegistrationsSupplier.get().stream()
          .filter(client -> client.getRegistrationId().equals(id))
          .findFirst()
          .orElse(null);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
      return clientRegistrationsSupplier.get().iterator();
    }

    public List<String> providers() {
      return clientRegistrationsSupplier.get().stream()
          .map(ClientRegistration::getRegistrationId)
          .toList();
    }
  }

  @Bean
  public Registrations managedExternalRegistrations(
      ExternalLoginProperties externalLoginProperties,
      ExternalProviderSettings externalProviderSettings) {
    return new Registrations(() -> {
      ExternalLoginProperties currentProviderProperties = new ExternalLoginProperties();
      for (String providerId : ExternalProviderSettings.PROVIDERS) {
        Credentials storedCredentials = externalProviderSettings.credentials(providerId);
        ExternalLoginProperties.Provider targetProviderProperties =
            providerId.equals(GOOGLE)
                ? currentProviderProperties.getGoogle()
                : currentProviderProperties.getMicrosoft();
        ExternalLoginProperties.Provider defaultProviderProperties =
            providerId.equals(GOOGLE)
                ? externalLoginProperties.getGoogle()
                : externalLoginProperties.getMicrosoft();
        if (storedCredentials.enabled()) {
          targetProviderProperties.setClientId(storedCredentials.clientId());
          targetProviderProperties.setClientSecret(storedCredentials.clientSecret());
          targetProviderProperties.setEnabled(true);
        }
        // Endpunkte stammen ausschließlich aus der Serverkonfiguration, nie aus Admin-Eingaben.
        targetProviderProperties.setAuthorizationUri(
            defaultProviderProperties.getAuthorizationUri());
        targetProviderProperties.setTokenUri(defaultProviderProperties.getTokenUri());
        targetProviderProperties.setJwkSetUri(defaultProviderProperties.getJwkSetUri());
      }

      List<ClientRegistration> configuredClientRegistrations = new ArrayList<>();
      externalRegistrations(currentProviderProperties).forEach(configuredClientRegistrations::add);
      return List.copyOf(configuredClientRegistrations);
    });
  }

  /** Unveränderliche Registrierungen für isolierte Protokolltests. */
  public Registrations externalRegistrations(ExternalLoginProperties externalLoginProperties) {
    List<ClientRegistration> configuredClientRegistrations = new ArrayList<>();
    if (externalLoginProperties.getGoogle().configured()) {
      configuredClientRegistrations.add(
          createClientRegistration(
              GOOGLE,
              externalLoginProperties.getGoogle(),
              "https://accounts.google.com",
              "https://accounts.google.com/o/oauth2/v2/auth",
              "https://oauth2.googleapis.com/token",
              "https://www.googleapis.com/oauth2/v3/certs"));
    }

    if (externalLoginProperties.getMicrosoft().configured()) {
      configuredClientRegistrations.add(
          createClientRegistration(
              "microsoft",
              externalLoginProperties.getMicrosoft(),
              "https://login.microsoftonline.com/common/v2.0",
              "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
              "https://login.microsoftonline.com/common/oauth2/v2.0/token",
              "https://login.microsoftonline.com/common/discovery/v2.0/keys"));
    }

    return new Registrations(configuredClientRegistrations);
  }

  private ClientRegistration createClientRegistration(
      String providerId,
      ExternalLoginProperties.Provider providerProperties,
      String defaultIssuerUri,
      String defaultAuthorizationUri,
      String defaultTokenUri,
      String defaultJwkSetUri) {
    return ClientRegistration.withRegistrationId(providerId)
        .clientId(providerProperties.getClientId())
        .clientSecret(providerProperties.getClientSecret())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "profile", "email")
        .issuerUri(defaultIssuerUri)
        .authorizationUri(
            configuredUriOrDefault(
                providerProperties.getAuthorizationUri(), defaultAuthorizationUri))
        .tokenUri(configuredUriOrDefault(providerProperties.getTokenUri(), defaultTokenUri))
        .jwkSetUri(configuredUriOrDefault(providerProperties.getJwkSetUri(), defaultJwkSetUri))
        .userNameAttributeName("sub")
        .clientName(providerId.equals(GOOGLE) ? "Google" : "Microsoft")
        .build();
  }

  private String configuredUriOrDefault(String configuredUri, String defaultUri) {
    return configuredUri == null || configuredUri.isBlank() ? defaultUri : configuredUri;
  }
}
