package de.derpeterson.app.config;

import de.derpeterson.app.config.BootstrapAccountsProperties.Account;
import de.derpeterson.app.config.BootstrapAccountsProperties.Entry;
import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailVerificationStatus;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.VerificationService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final Environment springEnvironment;
  private final BootstrapAccountsProperties bootstrapAccountsProperties;
  private final PlatformTransactionManager platformTransactionManager;
  private final ExternalProviderSettings externalProviderSettings;
  private VerificationService verificationService;

  @Autowired
  public void configureVerificationMail(VerificationService verificationService) {
    this.verificationService = verificationService;
  }

  @Bean
  public CommandLineRunner initDatabase() {
    return applicationArguments -> {
      createDefaultConfigSettings();
      externalProviderSettings.initializeDefaults();
      createDefaultRolesAndAccounts();
    };
  }

  private void createDefaultRolesAndAccounts() {
    // Vor irgendeiner Benutzeranlage alle Gruppen prüfen; Fehler nennen keine Zugangsdaten.
    List<Entry> validatedBootstrapEntries = bootstrapAccountsProperties.validatedEntries();

    new TransactionTemplate(platformTransactionManager)
        .executeWithoutResult(_ -> {
          EnumMap<RoleType, RoleEntity> roleEntitiesByType = createMissingRoleEntities();

          if (!bootstrapAccountsProperties.isEnabled()) {
            logger.info("Bootstrap account creation is disabled.");
            return;
          }
          createMissingBootstrapAccounts(validatedBootstrapEntries, roleEntitiesByType);
        });
  }

  private EnumMap<RoleType, RoleEntity> createMissingRoleEntities() {
    EnumMap<RoleType, RoleEntity> roleEntitiesByType =
        new EnumMap<RoleType, RoleEntity>(RoleType.class);

    for (RoleType roleType : RoleType.values()) {
      roleEntitiesByType.put(
          roleType,
          roleRepository
              .findByName(roleType)
              .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(roleType).build())));
    }

    return roleEntitiesByType;
  }

  private void createMissingBootstrapAccounts(
      List<Entry> validatedBootstrapEntries, EnumMap<RoleType, RoleEntity> roleEntitiesByType) {
    for (Entry bootstrapEntry : validatedBootstrapEntries) {
      Account bootstrapAccount = bootstrapEntry.account();
      if (userRepository.findByEmail(bootstrapAccount.getEmail()).isPresent()) {
        continue;
      }

      UserEntity newUserEntity =
          UserEntity.builder()
              .firstName(bootstrapAccount.getFirstName().strip())
              .lastName(bootstrapAccount.getLastName().strip())
              .email(bootstrapAccount.getEmail())
              .password(passwordEncoder.encode(bootstrapAccount.getPassword()))
              .birthDate(bootstrapAccount.getBirthDate())
              .gender(bootstrapAccount.getGender())
              .preferredLocale(bootstrapAccount.getPreferredLocale())
              .roleEntities(Set.of(roleEntitiesByType.get(bootstrapEntry.role())))
              .enabled(true)
              .emailVerificationStatus(
                  bootstrapAccount.isEmailVerificationRequired()
                      ? EmailVerificationStatus.PENDING
                      : EmailVerificationStatus.NOT_CHECKED)
              .registrationChannel(RegistrationChannel.APPLICATION_START)
              .build();

      userRepository.save(newUserEntity);
      if (bootstrapAccount.isEmailVerificationRequired()) {
        try {
          if (!verificationService.sendVerificationEmailByUser(newUserEntity)) {
            throw new IllegalStateException("Bootstrap verification mail could not be queued");
          }
        } catch (IOException failure) {
          throw new UncheckedIOException(
              "Bootstrap verification mail could not be prepared", failure);
        }
      }
      AfterCommitLog.info(logger, "Bootstrap account created with role {}", bootstrapEntry.role());
    }

    userRepository.flush();
  }

  private void createDefaultConfigSettings() {
    int createdSettingCount = 0;
    for (ConfigEntry configEntry : ConfigEntry.values()) {
      if (!configService.exists(configEntry)) {
        configService.set(configEntry, resolveDefaultValue(configEntry));
        createdSettingCount++;
      }
    }

    logger.info(
        "Configuration initialized: entries={}, created={}",
        ConfigEntry.values().length,
        createdSettingCount);
  }

  private String resolveDefaultValue(ConfigEntry configEntry) {
    String propertyKey = ConfigurationCatalog.property(configEntry);
    return springEnvironment.getProperty(propertyKey, configEntry.getDefaultValueString());
  }
}
