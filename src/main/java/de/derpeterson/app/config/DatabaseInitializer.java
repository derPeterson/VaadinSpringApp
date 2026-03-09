package de.derpeterson.app.config;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

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
            createDefaultRolesAndAdminUser();
        };
    }

    private void createDefaultRolesAndAdminUser() {
        RoleEntity adminRoleEntity = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(RoleType.ROLE_ADMIN).build()));

        RoleEntity userRoleEntity = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(RoleType.ROLE_USER).build()));

        logger.info("✅ Roles checked or created: ROLE_ADMIN, ROLE_USER");

        if (!defaultAdminEnabled) {
            logger.info("✅ Default admin bootstrap is disabled.");
            return;
        }

        if (defaultAdminEmail == null || defaultAdminEmail.isBlank()
                || defaultAdminPassword == null || defaultAdminPassword.isBlank()) {
            logger.warn("⚠️ Default admin bootstrap is enabled but email/password are missing.");
            return;
        }

        if (userRepository.findByEmail(defaultAdminEmail).isPresent()) {
            logger.info("✅ Default admin user already exists: {}", defaultAdminEmail);
            return;
        }

        UserEntity adminUserEntity = UserEntity.builder()
                .firstName("John")
                .lastName("Doe")
                .password(passwordEncoder.encode(defaultAdminPassword))
                .email(defaultAdminEmail)
                .gender(Gender.OTHER)
                .birthDate(LocalDate.of(1990, 1, 1))
                .roleEntities(Set.of(adminRoleEntity, userRoleEntity))
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
