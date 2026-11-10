package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.config.ConfigurationCatalog.Entry;
import de.derpeterson.app.config.ConfigurationCatalog.ProviderField;
import de.derpeterson.app.config.ConfigurationCatalog.Storage;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalProviderSettings;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

class ConfigurationCatalogTest {
  @Test
  void catalogCoversEveryPersistedTwoFactorPolicyField() throws Exception {
    Class<?> stored = Class.forName("de.derpeterson.app.security.mfa.TwoFactorPolicies$Stored");
    Set<String> fields =
        Arrays.stream(stored.getRecordComponents())
            .map(component -> "two-factor." + component.getName())
            .collect(Collectors.toSet());

    assertEquals(
        fields,
        ConfigurationCatalog.entries().stream()
            .filter(entry -> entry.storage() == Storage.TWO_FACTOR_POLICY)
            .map(Entry::key)
            .collect(Collectors.toSet()));
  }

  @Test
  void catalogCoversAllGeneralSettingsAndBothProviderFormsWithoutDuplicateKeys() {
    List<Entry> entries = ConfigurationCatalog.entries();

    assertEquals(entries.size(), entries.stream().map(Entry::key).distinct().count());
    assertEquals(
        Set.of(ConfigEntry.values()).stream().map(ConfigEntry::getKey).collect(Collectors.toSet()),
        entries.stream()
            .filter(entry -> entry.storage() == Storage.GENERAL_SETTINGS)
            .map(Entry::key)
            .collect(Collectors.toSet()));
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      for (ProviderField field : ProviderField.values()) {
        assertTrue(
            entries.stream()
                .anyMatch(entry ->
                    entry.storage() == Storage.SIGN_IN_PROVIDER
                        && entry.key().equals(field.property(provider))
                        && entry
                            .environmentVariable()
                            .equals(field.environmentVariable(provider))));
      }
    }
    assertThrows(UnsupportedOperationException.class, entries::clear);
    assertThrows(IllegalArgumentException.class, () -> ProviderField.SECRET.property("unknown"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "APP_SECURITY_TOTP_ENCRYPTION_KEY",
        "APP_SECURITY_PROVIDER_SECRETS_ENCRYPTION_KEY"
      })
  void startupOnlySecretsNeverBecomeGeneralDatabaseEntriesAndMappingsResolve(String variable)
      throws Exception {
    MockEnvironment environment = new MockEnvironment();
    environment
        .getPropertySources()
        .addLast(new ResourcePropertySource("classpath:application.properties"));
    for (Entry entry : ConfigurationCatalog.entries()) {
      assertNotNull(entry.group());
      if (entry.environmentVariable() != null) {
        environment.setProperty(entry.environmentVariable(), "own-fixture-value");

        assertEquals("own-fixture-value", environment.getRequiredProperty(entry.property()));
      }
    }
    Entry encryption =
        ConfigurationCatalog.entries().stream()
            .filter(entry ->
                entry.environmentVariable() != null && entry.environmentVariable().equals(variable))
            .findFirst()
            .orElseThrow();

    assertTrue(encryption.secret());
    assertEquals(Storage.STARTUP_ONLY, encryption.storage());
    assertFalse(
        ConfigurationCatalog.entries().stream()
            .filter(entry -> entry.storage() == Storage.GENERAL_SETTINGS)
            .anyMatch(entry -> entry.property().equals(encryption.property())));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void labelsAndHelpUseTheExistingLanguageCatalog(String language) {
    MessageProperties messageProperties =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language));
    for (Entry entry : ConfigurationCatalog.entries()) {
      assertFalse(entry.label().apply(messageProperties).isBlank(), entry.key());
      assertFalse(entry.description().apply(messageProperties).isBlank(), entry.key());
    }
  }
}
