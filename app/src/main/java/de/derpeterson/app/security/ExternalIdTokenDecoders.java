package de.derpeterson.app.security;

import com.nimbusds.jwt.JWTParser;
import java.util.LinkedHashMap;
import java.util.Map;
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
  private OidcIdTokenDecoderFactory standard = new OidcIdTokenDecoderFactory();
  private String googleClientId;
  private final ExternalLoginProperties properties;
  private final Map<String, JwtDecoder> tenants = new LinkedHashMap<>();

  public ExternalIdTokenDecoders(ExternalLoginProperties properties) {
    this.properties = properties;
  }

  @Override
  public synchronized JwtDecoder createDecoder(ClientRegistration registration) {
    if (!"microsoft".equals(registration.getRegistrationId())) {
      if (!registration.getClientId().equals(googleClientId)) {
        // Der Frameworkcache ist nach Registrierungs-ID indiziert, nicht nach Client-ID.
        standard = new OidcIdTokenDecoderFactory();
        googleClientId = registration.getClientId();
      }
      return standard.createDecoder(registration);
    }
    return token -> decodeMicrosoft(registration, token);
  }

  private Jwt decodeMicrosoft(ClientRegistration registration, String token) {
    String tenant;
    try {
      // Ungeprüfte Daten wählen ausschließlich einen UUID-Pfad am festen Microsoft-Host.
      // Vertrauen entsteht erst anschließend durch Signatur- und vollständige OIDC-Prüfung.
      tenant = JWTParser.parse(token).getJWTClaimsSet().getStringClaim("tid");
      if (tenant == null || !UUID.fromString(tenant).toString().equals(tenant)) {
        throw new IllegalArgumentException("Invalid tenant");
      }
    } catch (java.text.ParseException | IllegalArgumentException failure) {
      throw new JwtException("Invalid Microsoft tenant", failure);
    }
    JwtDecoder decoder;
    synchronized (tenants) {
      String cacheKey = registration.getClientId() + ":" + tenant;
      decoder = tenants.get(cacheKey);
      if (decoder == null) {
        String base = "https://login.microsoftonline.com/" + tenant;
        String keys = properties.getMicrosoft().getJwkSetUri();
        ClientRegistration scoped =
            ClientRegistration.withClientRegistration(registration)
                .issuerUri(base + "/v2.0")
                .jwkSetUri(keys == null || keys.isBlank() ? base + "/discovery/v2.0/keys" : keys)
                .registrationId("microsoft-" + tenant)
                .build();
        NimbusJwtDecoder microsoft =
            NimbusJwtDecoder.withJwkSetUri(scoped.getProviderDetails().getJwkSetUri())
                .restOperations(
                    MicrosoftSigningKeys.client(scoped.getProviderDetails().getIssuerUri()))
                .build();
        microsoft.setClaimSetConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter());
        microsoft.setJwtValidator(new OidcIdTokenValidator(scoped));
        decoder = microsoft;
        // Auch der Frameworkcache bleibt begrenzt: je Eintrag eine eigene Decoderfactory.
        if (tenants.size() >= 32) {
          tenants.remove(tenants.keySet().iterator().next());
        }
        tenants.put(cacheKey, decoder);
      }
    }
    Jwt jwt = decoder.decode(token);
    if (!tenant.equals(jwt.getClaimAsString("tid"))) {
      throw new JwtException("Microsoft tenant changed");
    }
    return jwt;
  }
}
