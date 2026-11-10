package de.derpeterson.app.security;

import com.nimbusds.jwt.JWTParser;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Microsofts gemeinsamer Einstieg wird auf einen konkret geprüften Mandanten aufgelöst. */
public final class ExternalIdTokenDecoders implements JwtDecoderFactory<ClientRegistration> {
  private OidcIdTokenDecoderFactory defaultIdTokenDecoderFactory = new OidcIdTokenDecoderFactory();
  private String googleClientId;
  private final ExternalLoginProperties externalLoginProperties;
  private final Map<String, JwtDecoder> tenantTokenDecoders = new LinkedHashMap<>();

  public ExternalIdTokenDecoders(ExternalLoginProperties externalLoginProperties) {
    this.externalLoginProperties = externalLoginProperties;
  }

  @Override
  public synchronized JwtDecoder createDecoder(ClientRegistration registration) {
    if (!"microsoft".equals(registration.getRegistrationId())) {
      if (!registration.getClientId().equals(googleClientId)) {
        // Der Frameworkcache ist nach Registrierungs-ID indiziert, nicht nach Client-ID.
        defaultIdTokenDecoderFactory = new OidcIdTokenDecoderFactory();
        googleClientId = registration.getClientId();
      }
      return defaultIdTokenDecoderFactory.createDecoder(registration);
    }
    return token -> decodeMicrosoft(registration, token);
  }

  private Jwt decodeMicrosoft(ClientRegistration clientRegistration, String serializedIdToken) {
    String tenantId;
    try {
      // Ungeprüfte Daten wählen ausschließlich einen UUID-Pfad am festen Microsoft-Host.
      // Vertrauen entsteht erst anschließend durch Signatur- und vollständige OIDC-Prüfung.
      tenantId = JWTParser.parse(serializedIdToken).getJWTClaimsSet().getStringClaim("tid");
      if (tenantId == null || !UUID.fromString(tenantId).toString().equals(tenantId)) {
        throw new IllegalArgumentException("Invalid tenant");
      }
    } catch (java.text.ParseException | IllegalArgumentException tenantParseFailure) {
      throw new JwtException("Invalid Microsoft tenant", tenantParseFailure);
    }

    JwtDecoder tenantJwtDecoder;
    synchronized (tenantTokenDecoders) {
      String tenantDecoderCacheKey = clientRegistration.getClientId() + ":" + tenantId;
      tenantJwtDecoder = tenantTokenDecoders.get(tenantDecoderCacheKey);
      if (tenantJwtDecoder == null) {
        String tenantIssuerBaseUri = "https://login.microsoftonline.com/" + tenantId;
        String configuredJwkSetUri = externalLoginProperties.getMicrosoft().getJwkSetUri();
        ClientRegistration tenantClientRegistration =
            ClientRegistration.withClientRegistration(clientRegistration)
                .issuerUri(tenantIssuerBaseUri + "/v2.0")
                .jwkSetUri(
                    configuredJwkSetUri == null || configuredJwkSetUri.isBlank()
                        ? tenantIssuerBaseUri + "/discovery/v2.0/keys"
                        : configuredJwkSetUri)
                .registrationId("microsoft-" + tenantId)
                .build();

        NimbusJwtDecoder microsoftJwtDecoder =
            NimbusJwtDecoder.withJwkSetUri(
                    Objects.requireNonNull(
                        tenantClientRegistration.getProviderDetails().getJwkSetUri()))
                .restOperations(
                    MicrosoftSigningKeys.client(
                        tenantClientRegistration.getProviderDetails().getIssuerUri()))
                .build();
        microsoftJwtDecoder.setClaimSetConverter(
            OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter());
        microsoftJwtDecoder.setJwtValidator(new OidcIdTokenValidator(tenantClientRegistration));
        tenantJwtDecoder = microsoftJwtDecoder;
        // Auch der Frameworkcache bleibt begrenzt: je Eintrag eine eigene Decoderfactory.
        if (tenantTokenDecoders.size() >= 32) {
          tenantTokenDecoders.remove(tenantTokenDecoders.keySet().iterator().next());
        }
        tenantTokenDecoders.put(tenantDecoderCacheKey, tenantJwtDecoder);
      }
    }

    Jwt validatedJwt = tenantJwtDecoder.decode(serializedIdToken);
    if (!tenantId.equals(validatedJwt.getClaimAsString("tid"))) {
      throw new JwtException("Microsoft tenant changed");
    }

    return validatedJwt;
  }
}
