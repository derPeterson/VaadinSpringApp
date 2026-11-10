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
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class CopyrightConfigurationTest {
  @Test
  void missingCopyrightIsSeededWithItsCentralDefault() {
    ConfigService configService = mock(ConfigService.class);
    when(configService.exists(any())).thenReturn(true);
    when(configService.exists(ConfigEntry.COPYRIGHT_TEXT)).thenReturn(false);
    Environment environment = mock(Environment.class);
    when(environment.getProperty(
            "app.config.defaults.copyright.text",
            ConfigEntry.COPYRIGHT_TEXT.getDefaultValueString()))
        .thenReturn(ConfigEntry.COPYRIGHT_TEXT.getDefaultValueString());
    ReflectionTestUtils.invokeMethod(
        initializer(configService, environment), "createDefaultConfigSettings");

    verify(configService).set(ConfigEntry.COPYRIGHT_TEXT, "© {year} {serviceName}");
  }

  @Test
  void anExistingCopyrightIsNeverOverwrittenByStartupDefaults() {
    ConfigService configService = mock(ConfigService.class);
    when(configService.exists(any())).thenReturn(true);
    Environment environment = mock(Environment.class);
    ReflectionTestUtils.invokeMethod(
        initializer(configService, environment), "createDefaultConfigSettings");

    verify(configService, never()).set(any(), any());
    verifyNoInteractions(environment);
  }

  private DatabaseInitializer initializer(ConfigService configService, Environment environment) {
    return new DatabaseInitializer(
        mock(UserRepository.class),
        mock(RoleRepository.class),
        configService,
        mock(PasswordEncoder.class),
        environment,
        new BootstrapAccountsProperties(),
        mock(org.springframework.transaction.PlatformTransactionManager.class),
        mock(ExternalProviderSettings.class));
  }
}
