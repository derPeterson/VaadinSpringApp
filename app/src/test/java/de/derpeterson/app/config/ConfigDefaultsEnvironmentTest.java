package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.derpeterson.app.model.enums.ConfigEntry;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

/** Prüft die echten Startvorgaben ohne lokale .env, Anwendungsstart oder Datenbank. */
class ConfigDefaultsEnvironmentTest {
  @ParameterizedTest
  @EnumSource(ConfigEntry.class)
  void missingEnvironmentValuesPreserveTheCentralDefault(ConfigEntry entry) throws IOException {
    MockEnvironment environment = environment(false);

    assertEquals(
        entry.getDefaultValueString(),
        environment.getRequiredProperty("app.config.defaults." + entry.getKey()));
  }

  @ParameterizedTest
  @EnumSource(ConfigEntry.class)
  void eachConfigEntryAcceptsItsDocumentedEnvironmentVariable(ConfigEntry entry)
      throws IOException {
    MockEnvironment environment = environment(false);
    environment.setProperty("APP_CFG_" + entry.name(), "own-fixture-value");

    assertEquals(
        "own-fixture-value",
        environment.getRequiredProperty("app.config.defaults." + entry.getKey()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"service.name", "base.url"})
  void dockerUsesTheSameEnvironmentMapping(String key) throws IOException {
    MockEnvironment environment = environment(true);
    environment.setProperty("APP_CFG_SERVICE_NAME", "Own Docker fixture");
    environment.setProperty("APP_CFG_BASE_URL", "https://example.test/");

    assertEquals(
        "service.name".equals(key) ? "Own Docker fixture" : "https://example.test/",
        environment.getRequiredProperty("app.config.defaults." + key));
  }

  private MockEnvironment environment(boolean docker) throws IOException {
    MockEnvironment environment = new MockEnvironment();
    if (docker) {
      environment
          .getPropertySources()
          .addLast(new ResourcePropertySource("classpath:application-docker.properties"));
    }
    environment
        .getPropertySources()
        .addLast(new ResourcePropertySource("classpath:application.properties"));
    return environment;
  }
}
