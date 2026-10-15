package de.derpeterson.app.config;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final ConfigService configService;
  private final PasswordEncoder passwordEncoder;
  private final Environment environment;
  private final RoleSchemaUpdater roleSchemaUpdater;

  @Value("${app.bootstrap.default-admin.enabled:false}")
  private boolean defaultAdminEnabled;

  @Value("${app.bootstrap.default-admin.email:}")
  private String defaultAdminEmail;

  @Value("${app.bootstrap.default-admin.password:}")
  private String defaultAdminPassword;

  @Bean
  public CommandLineRunner initDatabase() {
    return args -> {
      createDefaultConfigSettings();
      roleSchemaUpdater.updateRoleColumn();
      createDefaultRolesAndAdminUser();
    };
  }

  private void createDefaultRolesAndAdminUser() {
    var roles = new EnumMap<RoleType, RoleEntity>(RoleType.class);
    for (RoleType role : RoleType.values()) {
      roles.put(
          role,
          roleRepository
              .findByName(role)
              .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(role).build())));
    }
    logger.info("✅ Roles checked or created: {}", roles.keySet());

    if (!defaultAdminEnabled) {
      logger.info("✅ Default admin bootstrap is disabled.");
      return;
    }

    if (defaultAdminEmail == null
        || defaultAdminEmail.isBlank()
        || defaultAdminPassword == null
        || defaultAdminPassword.isBlank()) {
      logger.warn("⚠️ Default admin bootstrap is enabled but email/password are missing.");
      return;
    }

    if (userRepository.findByEmail(defaultAdminEmail).isPresent()) {
      logger.info("✅ Default admin user already exists: {}", defaultAdminEmail);
      return;
    }

    UserEntity adminUserEntity =
        UserEntity.builder()
            .firstName("John")
            .lastName("Doe")
            .password(passwordEncoder.encode(defaultAdminPassword))
            .email(defaultAdminEmail)
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .roleEntities(Set.of(roles.get(RoleType.ROLE_ADMIN), roles.get(RoleType.ROLE_USER)))
            .enabled(true)
            .build();

    userRepository.save(adminUserEntity);
    logger.info("✅ Default admin user created: {}", adminUserEntity.getEmail());
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
