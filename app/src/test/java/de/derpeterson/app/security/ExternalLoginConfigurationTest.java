package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

/** Echte Anwendungsvorgaben mit eigenen Properties; keine lokale .env oder Anbieteranfrage. */
class ExternalLoginConfigurationTest {
  @ParameterizedTest
  @CsvSource({"google,GOOGLE", "microsoft,MICROSOFT"})
  void documentedDotEnvCredentialsEnableTheCorrespondingProvider(String provider, String key)
      throws Exception {
    ExternalLoginConfiguration.Registrations registrations =
        registrations(key, "own-fixture-secret");

    assertEquals(List.of(provider), registrations.providers());
    assertEquals("own-client", registrations.findByRegistrationId(provider).getClientId());
  }

  @ParameterizedTest
  @CsvSource({"google,GOOGLE", "microsoft,MICROSOFT"})
  void missingDotEnvSecretKeepsTheProviderUnavailable(String provider, String key)
      throws Exception {
    ExternalLoginConfiguration.Registrations registrations = registrations(key, "");

    assertTrue(registrations.providers().isEmpty());
    assertNull(registrations.findByRegistrationId(provider));
  }

  private ExternalLoginConfiguration.Registrations registrations(String key, String secret)
      throws Exception {
    // Eine importierte .env ist wie MockEnvironment eine normale Properties-Quelle,
    // keine Betriebssystem-Umgebung mit deren besonderer Namensauflösung.
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("APP_EXTERNAL_LOGIN_" + key + "_CLIENT_ID", "own-client")
            .withProperty("APP_EXTERNAL_LOGIN_" + key + "_CLIENT_SECRET", secret);
    environment
        .getPropertySources()
        .addLast(new ResourcePropertySource("classpath:application.properties"));
    ExternalLoginProperties properties =
        Binder.get(environment)
            .bind("app.external-login", Bindable.of(ExternalLoginProperties.class))
            .orElseGet(ExternalLoginProperties::new);
    return new ExternalLoginConfiguration().externalRegistrations(properties);
  }
}
