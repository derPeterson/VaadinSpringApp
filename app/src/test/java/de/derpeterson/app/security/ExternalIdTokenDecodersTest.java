package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Signierte eigene Tokens und eigener JWKS-Dienst. Keine echten Anbieterzugänge. */
class ExternalIdTokenDecodersTest {
  private static final String TENANT = "9188040d-6c67-4c5b-b112-36a304b66dad";
  private HttpServer server;
  private RSAKey key;
  private ExternalLoginProperties externalLoginProperties;

  @BeforeEach
  void setup() throws Exception {
    key = new RSAKeyGenerator(2048).keyID("own-test-key").generate();
    Map<String, Object> published = new LinkedHashMap<>(key.toPublicJWK().toJSONObject());
    published.put("issuer", "https://login.microsoftonline.com/{tenantid}/v2.0");
    byte[] keys =
        JSONObjectUtils.toJSONString(Map.of("keys", List.of(published)))
            .getBytes(StandardCharsets.UTF_8);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/keys",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, keys.length);
          try (var output = exchange.getResponseBody()) {
            output.write(keys);
          }
        });
    server.start();
    externalLoginProperties = new ExternalLoginProperties();
    for (ExternalLoginProperties.Provider provider :
        new ExternalLoginProperties.Provider[] {
          externalLoginProperties.getGoogle(), externalLoginProperties.getMicrosoft()
        }) {
      provider.setClientId("own-client");
      provider.setEnabled(true);
      provider.setClientSecret("own-fixture-secret");
      provider.setJwkSetUri("http://127.0.0.1:" + server.getAddress().getPort() + "/keys");
    }
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private ClientRegistration registration(String provider) {
    return new ExternalLoginConfiguration()
        .externalRegistrations(externalLoginProperties)
        .findByRegistrationId(provider);
  }

  private String token(
      String issuer, String audience, String tenant, Instant expires, RSAKey signer)
      throws JOSEException {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("stable-subject")
            .audience(audience)
            .issueTime(Date.from(now.minusSeconds(5)))
            .expirationTime(Date.from(expires))
            .claim("tid", tenant)
            .claim("nonce", "nonce-bound-in-framework")
            .build();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("own-test-key").build(), claims);
    jwt.sign(new RSASSASigner(signer));
    return jwt.serialize();
  }

  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void signatureAudienceExpiryAndExactIssuerAreRequired(String provider) throws Exception {
    String issuer =
        provider.equals("google")
            ? "https://accounts.google.com"
            : "https://login.microsoftonline.com/" + TENANT + "/v2.0";
    JwtDecoder decoder =
        new ExternalIdTokenDecoders(externalLoginProperties).createDecoder(registration(provider));
    Instant valid = Instant.now().plusSeconds(300);

    assertEquals(
        "stable-subject",
        decoder.decode(token(issuer, "own-client", TENANT, valid, key)).getSubject());
    String wrongAudience = token(issuer, "another-client", TENANT, valid, key);

    assertThrows(JwtException.class, () -> decoder.decode(wrongAudience));
    String wrongIssuer = token("https://attacker.example.test", "own-client", TENANT, valid, key);

    assertThrows(JwtException.class, () -> decoder.decode(wrongIssuer));
    String expiredToken = token(issuer, "own-client", TENANT, Instant.now().minusSeconds(300), key);

    assertThrows(JwtException.class, () -> decoder.decode(expiredToken));
    RSAKey other = new RSAKeyGenerator(2048).keyID("own-test-key").generate();
    String wrongSignature = token(issuer, "own-client", TENANT, valid, other);

    assertThrows(JwtException.class, () -> decoder.decode(wrongSignature));
  }

  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void changedClientIdNeverReusesThePreviousAudienceValidator(String provider) throws Exception {
    String issuer =
        provider.equals("google")
            ? "https://accounts.google.com"
            : "https://login.microsoftonline.com/" + TENANT + "/v2.0";
    ExternalIdTokenDecoders factory = new ExternalIdTokenDecoders(externalLoginProperties);
    Instant expires = Instant.now().plusSeconds(300);
    factory
        .createDecoder(registration(provider))
        .decode(token(issuer, "own-client", TENANT, expires, key));
    (provider.equals("google")
            ? externalLoginProperties.getGoogle()
            : externalLoginProperties.getMicrosoft())
        .setClientId("replacement-client");
    JwtDecoder replacement = factory.createDecoder(registration(provider));
    String previousAudience = token(issuer, "own-client", TENANT, expires, key);

    assertThrows(JwtException.class, () -> replacement.decode(previousAudience));
    assertEquals(
        "stable-subject",
        replacement.decode(token(issuer, "replacement-client", TENANT, expires, key)).getSubject());
  }

  @Test
  void microsoftSupportsDifferentConcreteTenantsButNeverArbitraryIssuerPaths() throws Exception {
    JwtDecoder decoder =
        new ExternalIdTokenDecoders(externalLoginProperties)
            .createDecoder(registration("microsoft"));
    String company = "12345678-1234-1234-1234-123456789abc";

    assertEquals(
        company,
        decoder
            .decode(
                token(
                    "https://login.microsoftonline.com/" + company + "/v2.0",
                    "own-client",
                    company,
                    Instant.now().plusSeconds(300),
                    key))
            .getClaimAsString("tid"));
    String invalidTenantPath =
        token(
            "https://attacker.example.test",
            "own-client",
            "../../attacker",
            Instant.now().plusSeconds(300),
            key);

    assertThrows(JwtException.class, () -> decoder.decode(invalidTenantPath));
    String mismatchedTenant =
        token(
            "https://login.microsoftonline.com/" + TENANT + "/v2.0",
            "own-client",
            company,
            Instant.now().plusSeconds(300),
            key);

    assertThrows(JwtException.class, () -> decoder.decode(mismatchedTenant));
  }
}
