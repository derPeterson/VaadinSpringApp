package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import de.derpeterson.app.repository.ExternalProviderRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.security.ExternalLoginConfiguration;
import de.derpeterson.app.security.ExternalLoginProperties;
import de.derpeterson.app.security.ExternalProviderSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ExternalProviderSettingsPersistenceTest.Config.class)
class ExternalProviderSettingsPersistenceTest {
  @Configuration
  @Import({UserServicePersistenceTest.Config.class, ExternalProviderSettings.class})
  static class Config {
    @Bean
    de.derpeterson.app.security.mfa.TwoFactorAuthorization twoFactor() {
      return org.mockito.Mockito.mock(de.derpeterson.app.security.mfa.TwoFactorAuthorization.class);
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

  @Autowired ExternalProviderSettings settings;
  @Autowired ExternalProviderRepository repository;
  @Autowired ExternalLoginProperties properties;
  @Autowired AdminRolePolicy policy;

  @BeforeEach
  void resetSettings() {
    repository.deleteAll();
    properties.getGoogle().setClientId(null);
    properties.getGoogle().setClientSecret(null);
    properties.getGoogle().setEnabled(false);
    properties.getMicrosoft().setClientId(null);
    properties.getMicrosoft().setClientSecret(null);
    properties.getMicrosoft().setEnabled(false);
    reset(policy);
  }

  @Test
  void startupPersistsBothProvidersAndPreservesSavedSettingsAcrossRestarts() {
    properties.getGoogle().setClientId("own-google-client");
    properties.getGoogle().setClientSecret("own-google-secret");
    properties.getGoogle().setEnabled(true);
    properties.getMicrosoft().setClientId("own-microsoft-client");
    properties.getMicrosoft().setClientSecret("own-microsoft-secret");

    settings.initializeDefaults();

    assertEquals(2, repository.count());
    assertEquals("own-google-client", repository.findById("google").orElseThrow().getClientId());
    assertEquals(
        "own-google-secret", repository.findById("google").orElseThrow().getClientSecret());
    assertTrue(repository.findById("google").orElseThrow().isEnabled());
    assertFalse(repository.findById("microsoft").orElseThrow().isEnabled());
    assertEquals(
        java.util.List.of("google"),
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(new ExternalLoginProperties(), settings)
            .providers());
    ExternalProviderSettings.Settings saved = settings.load("google");
    settings.save("google", "saved-client", "", false, saved.revision());
    properties.getGoogle().setClientId("changed-environment-client");
    properties.getGoogle().setClientSecret("changed-environment-secret");
    properties.getMicrosoft().setEnabled(true);
    settings.initializeDefaults();

    assertEquals("saved-client", settings.load("google").clientId());
    assertFalse(settings.load("google").enabled());
    assertFalse(settings.load("microsoft").enabled());
    assertEquals(
        "own-google-secret", repository.findById("google").orElseThrow().getClientSecret());
    assertTrue(
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(new ExternalLoginProperties(), settings)
            .providers()
            .isEmpty());
  }

  @Test
  void startupCreatesDisabledRowsWithoutCredentialsAndNeverStoresInvalidActivationPartially() {
    properties.getMicrosoft().setEnabled(true);
    assertThrows(IllegalArgumentException.class, settings::initializeDefaults);
    assertEquals(0, repository.count());

    properties.getMicrosoft().setEnabled(false);
    settings.initializeDefaults();
    assertEquals(2, repository.count());
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      assertEquals("", settings.load(provider).clientId());
      assertFalse(settings.load(provider).secretStored());
      assertFalse(settings.load(provider).enabled());
    }
  }

  @Test
  void configurationBecomesAvailableWithoutRestartAndSurvivesANewAdapter() {
    ExternalLoginConfiguration.Registrations live =
        new ExternalLoginConfiguration().managedExternalRegistrations(properties, settings);
    assertTrue(live.providers().isEmpty());
    ExternalProviderSettings.Settings empty = settings.load("google");
    ExternalProviderSettings.Settings saved =
        settings.save("google", "own-client", "own-secret", true, empty.revision());
    assertEquals("own-client", live.findByRegistrationId("google").getClientId());
    assertTrue(saved.secretStored());
    assertFalse(saved.toString().contains("own-secret"));
    assertNotNull(
        new ExternalLoginConfiguration()
            .managedExternalRegistrations(properties, settings)
            .findByRegistrationId("google"));
    settings.save("google", "own-client", "", false, saved.revision());
    assertNull(live.findByRegistrationId("google"));
    assertEquals("own-secret", repository.findById("google").orElseThrow().getClientSecret());
  }

  @Test
  void emptySecretPreservesImportedCredentialAndDatabaseDisableWinsOverEnvironment() {
    properties.getMicrosoft().setClientId("own-environment-client");
    properties.getMicrosoft().setEnabled(true);
    properties.getMicrosoft().setClientSecret("own-environment-secret");
    ExternalProviderSettings.Settings imported = settings.load("microsoft");
    assertTrue(imported.enabled());
    ExternalProviderSettings.Settings saved =
        settings.save("microsoft", imported.clientId(), "", false, imported.revision());
    assertFalse(settings.load("microsoft").enabled());
    assertTrue(saved.secretStored());
    assertEquals(
        "own-environment-secret", repository.findById("microsoft").orElseThrow().getClientSecret());
  }

  @Test
  void activationRequiresBothCredentialsAndRollsBackInvalidUpdates() {
    String revision = settings.load("google").revision();
    assertThrows(
        IllegalArgumentException.class, () -> settings.save("google", "own", "", true, revision));
    assertTrue(repository.findAll().isEmpty());
    ExternalProviderSettings.Settings saved =
        settings.save("google", "own", "own-secret", true, revision);
    String savedRevision = saved.revision();
    assertThrows(
        IllegalArgumentException.class,
        () -> settings.save("google", "bad\nclient", "", true, savedRevision));
    assertEquals("own", settings.load("google").clientId());
  }

  @Test
  void staleFormCannotOverwriteNewConfigurationOrCreateASecondInitialVersion() {
    String initial = settings.load("google").revision();
    ExternalProviderSettings.Settings first =
        settings.save("google", "own", "first-secret", true, initial);
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> settings.save("google", "stale", "", false, initial));
    settings.save("google", "own", "second-secret", true, first.revision());
    String firstRevision = first.revision();
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> settings.save("google", "stale", "", false, firstRevision));
    assertEquals("second-secret", repository.findById("google").orElseThrow().getClientSecret());
  }

  @Test
  void freshAdministratorAuthorizationProtectsReadAndWriteAndUnsupportedProvidersAreRejected() {
    String revision = settings.load("google").revision();
    doThrow(new AccessDeniedException("Revoked")).when(policy).requireSuperadmin();
    assertThrows(AccessDeniedException.class, () -> settings.load("google"));
    assertThrows(
        AccessDeniedException.class,
        () -> settings.save("google", "own", "own-secret", true, revision));
    assertTrue(repository.findAll().isEmpty());
    reset(policy);
    assertThrows(IllegalArgumentException.class, () -> settings.load("unknown"));
  }
}
