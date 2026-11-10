package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import de.derpeterson.app.model.ExternalProviderEntity;
import de.derpeterson.app.repository.ExternalProviderRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.security.ExternalLoginConfiguration;
import de.derpeterson.app.security.ExternalLoginProperties;
import de.derpeterson.app.security.ExternalProviderSecrets;
import de.derpeterson.app.security.ExternalProviderSettings;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ExternalProviderSettingsPersistenceTest.Config.class)
class ExternalProviderSettingsPersistenceTest {
  @Configuration
  @Import({UserServicePersistenceTest.Config.class, ExternalProviderSettings.class})
  static class Config {
    @Bean
    ExternalProviderSecrets providerSecrets() {
      return new ExternalProviderSecrets(
          new MockEnvironment()
              .withProperty(
                  ExternalProviderSecrets.KEY_PROPERTY,
                  java.util.Base64.getEncoder().encodeToString(new byte[32])));
    }

    @Bean
    de.derpeterson.app.security.mfa.TwoFactorAuthorization twoFactor() {
      return mock(de.derpeterson.app.security.mfa.TwoFactorAuthorization.class);
    }

    @Bean
    jakarta.servlet.http.HttpServletRequest request() {
      return new org.springframework.mock.web.MockHttpServletRequest();
    }

    @Bean
    ExternalLoginProperties externalProperties() {
      return new ExternalLoginProperties();
    }
  }

  @Autowired ExternalProviderSettings externalProviderSettings;
  @Autowired ExternalProviderRepository repository;
  @Autowired ExternalLoginProperties externalLoginProperties;
  @Autowired AdminRolePolicy adminRolePolicy;
  @Autowired ExternalProviderSecrets secrets;
  @Autowired DataSource testDataSource;

  @BeforeEach
  void resetSettings() {
    repository.deleteAll();
    externalLoginProperties.getGoogle().setClientId(null);
    externalLoginProperties.getGoogle().setClientSecret(null);
    externalLoginProperties.getGoogle().setEnabled(false);
    externalLoginProperties.getMicrosoft().setClientId(null);
    externalLoginProperties.getMicrosoft().setClientSecret(null);
    externalLoginProperties.getMicrosoft().setEnabled(false);
    reset(adminRolePolicy);
  }

  @Test
  void startupPersistsBothProvidersAndPreservesSavedSettingsAcrossRestarts() {
    externalLoginProperties.getGoogle().setClientId("own-google-client");
    externalLoginProperties.getGoogle().setClientSecret("own-google-secret");
    externalLoginProperties.getGoogle().setEnabled(true);
    externalLoginProperties.getMicrosoft().setClientId("own-microsoft-client");
    externalLoginProperties.getMicrosoft().setClientSecret("own-microsoft-secret");

    externalProviderSettings.initializeDefaults();

    assertEquals(2, repository.count());
    String rawGoogle =
        new JdbcTemplate(testDataSource)
            .queryForObject(
                "select encrypted_client_secret from external_providers where provider = 'google'",
                String.class);

    assertFalse(rawGoogle.contains("own-google-secret"));
    assertEquals("own-google-secret", secrets.decrypt("google", rawGoogle));
    assertEquals("own-google-client", repository.findById("google").orElseThrow().getClientId());
    assertEquals(
        "own-google-secret",
        secrets.decrypt(
            "google", repository.findById("google").orElseThrow().getEncryptedClientSecret()));
    assertTrue(repository.findById("google").orElseThrow().isEnabled());
    assertFalse(repository.findById("microsoft").orElseThrow().isEnabled());
    assertEquals(
        java.util.List.of("google"),
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(new ExternalLoginProperties(), externalProviderSettings)
            .providers());
    ExternalProviderSettings.Settings saved = externalProviderSettings.load("google");
    externalProviderSettings.save("google", "saved-client", "", false, saved.revision());
    externalLoginProperties.getGoogle().setClientId("changed-environment-client");
    externalLoginProperties.getGoogle().setClientSecret("changed-environment-secret");
    externalLoginProperties.getMicrosoft().setEnabled(true);
    externalProviderSettings.initializeDefaults();

    assertEquals("saved-client", externalProviderSettings.load("google").clientId());
    assertFalse(externalProviderSettings.load("google").enabled());
    assertFalse(externalProviderSettings.load("microsoft").enabled());
    assertEquals(
        "own-google-secret",
        secrets.decrypt(
            "google", repository.findById("google").orElseThrow().getEncryptedClientSecret()));
    assertTrue(
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(new ExternalLoginProperties(), externalProviderSettings)
            .providers()
            .isEmpty());
  }

  @Test
  void startupCreatesDisabledRowsWithoutCredentialsAndNeverStoresInvalidActivationPartially() {
    externalLoginProperties.getMicrosoft().setEnabled(true);

    assertThrows(IllegalArgumentException.class, externalProviderSettings::initializeDefaults);
    assertEquals(0, repository.count());

    externalLoginProperties.getMicrosoft().setEnabled(false);
    externalProviderSettings.initializeDefaults();

    assertEquals(2, repository.count());
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      assertEquals("", externalProviderSettings.load(provider).clientId());
      assertFalse(externalProviderSettings.load(provider).secretStored());
      assertFalse(externalProviderSettings.load(provider).enabled());
    }
  }

  @Test
  void configurationBecomesAvailableWithoutRestartAndSurvivesANewAdapter() {
    ExternalLoginConfiguration.Registrations live =
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(externalLoginProperties, externalProviderSettings);

    assertTrue(live.providers().isEmpty());
    ExternalProviderSettings.Settings empty = externalProviderSettings.load("google");
    ExternalProviderSettings.Settings saved =
        externalProviderSettings.save("google", "own-client", "own-secret", true, empty.revision());

    assertEquals("own-client", live.findByRegistrationId("google").getClientId());
    assertEquals("own-secret", live.findByRegistrationId("google").getClientSecret());
    String encrypted = repository.findById("google").orElseThrow().getEncryptedClientSecret();

    assertTrue(saved.secretStored());
    assertFalse(saved.toString().contains("own-secret"));
    assertNotNull(
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(externalLoginProperties, externalProviderSettings)
            .findByRegistrationId("google"));
    externalProviderSettings.save("google", "own-client", "", false, saved.revision());

    assertEquals(encrypted, repository.findById("google").orElseThrow().getEncryptedClientSecret());
    assertNull(live.findByRegistrationId("google"));
    assertEquals(
        "own-secret",
        secrets.decrypt(
            "google", repository.findById("google").orElseThrow().getEncryptedClientSecret()));
  }

  @Test
  void unreadableStoredSecretsFailStartupInsteadOfBeingOverwrittenByEnvironment() {
    externalProviderSettings.save(
        "google",
        "own-client",
        "own-secret",
        true,
        externalProviderSettings.load("google").revision());
    String expected = repository.findById("google").orElseThrow().getEncryptedClientSecret();
    ExternalProviderEntity corrupted = repository.findById("google").orElseThrow();
    corrupted.setEncryptedClientSecret(expected.substring(0, expected.length() - 5));
    repository.saveAndFlush(corrupted);
    externalLoginProperties.getGoogle().setClientSecret("environment-cannot-repair-this");

    assertThrows(IllegalStateException.class, externalProviderSettings::initializeDefaults);
    assertEquals(
        expected.substring(0, expected.length() - 5),
        repository.findById("google").orElseThrow().getEncryptedClientSecret());
    ExternalLoginConfiguration.Registrations registrations =
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(externalLoginProperties, externalProviderSettings);

    assertThrows(IllegalStateException.class, registrations::providers);
  }

  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void bothProvidersUseDecryptedCredentialsWhileDatabaseAndEmptyUpdatesKeepCiphertext(
      String provider) {
    ExternalProviderSettings.Settings saved =
        externalProviderSettings.save(
            provider,
            "own-client",
            "own-secret",
            true,
            externalProviderSettings.load(provider).revision());
    String encrypted = repository.findById(provider).orElseThrow().getEncryptedClientSecret();

    assertFalse(encrypted.contains("own-secret"));
    ExternalLoginConfiguration.Registrations registrations =
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(externalLoginProperties, externalProviderSettings);

    assertEquals("own-secret", registrations.findByRegistrationId(provider).getClientSecret());
    externalProviderSettings.save(provider, "own-client", "", true, saved.revision());

    assertEquals(encrypted, repository.findById(provider).orElseThrow().getEncryptedClientSecret());
    assertEquals("own-secret", registrations.findByRegistrationId(provider).getClientSecret());
  }

  @Test
  void missingEncryptionKeyPreventsStartupSeedingAndSavingAnyNonemptySecret() {
    ExternalProviderSettings unavailable =
        new ExternalProviderSettings(
            repository,
            externalLoginProperties,
            new ExternalProviderSecrets(new MockEnvironment()),
            adminRolePolicy,
            mock(de.derpeterson.app.security.mfa.TwoFactorAuthorization.class),
            new org.springframework.mock.web.MockHttpServletRequest());
    externalLoginProperties.getGoogle().setClientId("own-client");
    externalLoginProperties.getGoogle().setClientSecret("own-secret");
    externalLoginProperties.getGoogle().setEnabled(true);

    assertThrows(IllegalStateException.class, unavailable::initializeDefaults);
    assertEquals(0, repository.count());
    String revision = externalProviderSettings.load("google").revision();

    assertThrows(
        IllegalStateException.class,
        () -> unavailable.save("google", "own-client", "own-secret", true, revision));
    assertEquals(0, repository.count());
  }

  @Test
  void emptySecretPreservesImportedCredentialAndDatabaseDisableWinsOverEnvironment() {
    externalLoginProperties.getMicrosoft().setClientId("own-environment-client");
    externalLoginProperties.getMicrosoft().setEnabled(true);
    externalLoginProperties.getMicrosoft().setClientSecret("own-environment-secret");
    ExternalProviderSettings.Settings imported = externalProviderSettings.load("microsoft");

    assertTrue(imported.enabled());
    ExternalProviderSettings.Settings saved =
        externalProviderSettings.save(
            "microsoft", imported.clientId(), "", false, imported.revision());

    assertFalse(externalProviderSettings.load("microsoft").enabled());
    assertTrue(saved.secretStored());
    assertEquals(
        "own-environment-secret",
        secrets.decrypt(
            "microsoft",
            repository.findById("microsoft").orElseThrow().getEncryptedClientSecret()));
  }

  @Test
  void activationRequiresBothCredentialsAndRollsBackInvalidUpdates() {
    String revision = externalProviderSettings.load("google").revision();

    assertThrows(
        IllegalArgumentException.class,
        () -> externalProviderSettings.save("google", "own", "", true, revision));
    assertTrue(repository.findAll().isEmpty());
    ExternalProviderSettings.Settings saved =
        externalProviderSettings.save("google", "own", "own-secret", true, revision);
    String savedRevision = saved.revision();

    assertThrows(
        IllegalArgumentException.class,
        () -> externalProviderSettings.save("google", "bad\nclient", "", true, savedRevision));
    assertEquals("own", externalProviderSettings.load("google").clientId());
  }

  @Test
  void staleFormCannotOverwriteNewConfigurationOrCreateASecondInitialVersion() {
    String initial = externalProviderSettings.load("google").revision();
    ExternalProviderSettings.Settings first =
        externalProviderSettings.save("google", "own", "first-secret", true, initial);

    assertThrows(
        OptimisticLockingFailureException.class,
        () -> externalProviderSettings.save("google", "stale", "", false, initial));
    externalProviderSettings.save("google", "own", "second-secret", true, first.revision());
    String firstRevision = first.revision();

    assertThrows(
        OptimisticLockingFailureException.class,
        () -> externalProviderSettings.save("google", "stale", "", false, firstRevision));
    assertEquals(
        "second-secret",
        secrets.decrypt(
            "google", repository.findById("google").orElseThrow().getEncryptedClientSecret()));
  }

  @Test
  void freshAdministratorAuthorizationProtectsReadAndWriteAndUnsupportedProvidersAreRejected() {
    String revision = externalProviderSettings.load("google").revision();
    doThrow(new AccessDeniedException("Revoked")).when(adminRolePolicy).requireSuperadmin();

    assertThrows(AccessDeniedException.class, () -> externalProviderSettings.load("google"));
    assertThrows(
        AccessDeniedException.class,
        () -> externalProviderSettings.save("google", "own", "own-secret", true, revision));
    assertTrue(repository.findAll().isEmpty());
    reset(adminRolePolicy);

    assertThrows(IllegalArgumentException.class, () -> externalProviderSettings.load("unknown"));
  }
}
