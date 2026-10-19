package de.derpeterson.app.config;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class CopyrightConfigurationTest {
  @Test
  void missingCopyrightIsSeededWithItsCentralDefault() {
    ConfigService config = mock(ConfigService.class);
    when(config.exists(any())).thenReturn(true);
    when(config.exists(ConfigEntry.COPYRIGHT_TEXT)).thenReturn(false);
    Environment environment = mock(Environment.class);
    when(environment.getProperty(
            "app.config.defaults.copyright.text",
            ConfigEntry.COPYRIGHT_TEXT.getDefaultValueString()))
        .thenReturn(ConfigEntry.COPYRIGHT_TEXT.getDefaultValueString());
    ReflectionTestUtils.invokeMethod(
        initializer(config, environment), "createDefaultConfigSettings");
    verify(config).set(ConfigEntry.COPYRIGHT_TEXT, "© {year} {serviceName}");
  }

  @Test
  void anExistingCopyrightIsNeverOverwrittenByStartupDefaults() {
    ConfigService config = mock(ConfigService.class);
    when(config.exists(any())).thenReturn(true);
    Environment environment = mock(Environment.class);
    ReflectionTestUtils.invokeMethod(
        initializer(config, environment), "createDefaultConfigSettings");
    verify(config, never()).set(any(), any());
    verifyNoInteractions(environment);
  }

  private DatabaseInitializer initializer(ConfigService config, Environment environment) {
    return new DatabaseInitializer(
        mock(UserRepository.class),
        mock(RoleRepository.class),
        config,
        mock(PasswordEncoder.class),
        environment,
        mock(RoleSchemaUpdater.class),
        mock(de.derpeterson.app.config.UserNotificationSchemaUpdater.class),
        new BootstrapAccountsProperties(),
        mock(org.springframework.transaction.PlatformTransactionManager.class));
  }
}
