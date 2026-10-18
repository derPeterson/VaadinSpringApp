package de.derpeterson.app.config;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import java.util.EnumMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableConfigurationProperties(BootstrapAccountsProperties.class)
@RequiredArgsConstructor
public class DatabaseInitializer {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final ConfigService configService;
  private final PasswordEncoder passwordEncoder;
  private final Environment environment;
  private final RoleSchemaUpdater roleSchemaUpdater;

  private final UserNotificationSchemaUpdater notificationSchemaUpdater;
  private final BootstrapAccountsProperties bootstrap;
  private final PlatformTransactionManager transactionManager;

  @Bean
  public CommandLineRunner initDatabase() {
    return args -> {
      configService.removeRetiredEntries();
      createDefaultConfigSettings();
      roleSchemaUpdater.updateRoleColumn();
      notificationSchemaUpdater.updateTypeColumn();
      createDefaultRolesAndAccounts();
    };
  }

  private void createDefaultRolesAndAccounts() {
    // Vor irgendeiner Benutzeranlage alle Gruppen prüfen; Fehler nennen keine Zugangsdaten.
    var entries = bootstrap.validatedEntries();
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              var roles = new EnumMap<RoleType, RoleEntity>(RoleType.class);
              for (var role : RoleType.values())
                roles.put(
                    role,
                    roleRepository
                        .findByName(role)
                        .orElseGet(
                            () -> roleRepository.save(RoleEntity.builder().name(role).build())));
              if (!bootstrap.isEnabled()) {
                logger.info("Bootstrap account creation is disabled.");
                return;
              }
              for (var entry : entries) {
                var account = entry.account();
                if (userRepository.findByEmail(account.getEmail()).isPresent()) continue;
                var user =
                    UserEntity.builder()
                        .firstName(account.getFirstName().strip())
                        .lastName(account.getLastName().strip())
                        .email(account.getEmail())
                        .password(passwordEncoder.encode(account.getPassword()))
                        .birthDate(account.getBirthDate())
                        .gender(account.getGender())
                        .preferredLocale(account.getPreferredLocale())
                        .roleEntities(Set.of(roles.get(entry.role())))
                        .enabled(true)
                        .build();
                userRepository.save(user);
                logger.info("Bootstrap account created with role {}", entry.role());
              }
              userRepository.flush();
            });
  }

  private void createDefaultConfigSettings() {
    for (ConfigEntry configEntry : ConfigEntry.values()) {
      if (!configService.exists(configEntry)) {
        configService.set(configEntry, resolveDefaultValue(configEntry));
        logger.info("✅ Config entry created: {}", configEntry.getKey());
      } else {
        logger.info("✅ Config entry already exists: {}", configEntry.getKey());
      }
    }
  }

  private String resolveDefaultValue(ConfigEntry configEntry) {
    String propertyKey = "app.config.defaults." + configEntry.getKey();
    return environment.getProperty(propertyKey, configEntry.getDefaultValueString());
  }
}
