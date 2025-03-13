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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            createDefaultConfigSettings();
            createDefaultRolesAndAdminUser();
        };
    }

    private void createDefaultRolesAndAdminUser() {
        RoleEntity adminRoleEntity = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> {
                    RoleEntity roleEntity = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
                    return roleRepository.save(roleEntity);
                });

        RoleEntity userRoleEntity = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseGet(() -> {
                    RoleEntity roleEntity = RoleEntity.builder().name(RoleType.ROLE_USER).build();
                    return roleRepository.save(roleEntity);
                });

        logger.info("✅ Rollen geprüft oder erstellt: ROLE_ADMIN, ROLE_USER");

        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            UserEntity adminUserEntity = UserEntity.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .password(passwordEncoder.encode("Admin@123"))
                    .email("admin@example.com")
                    .gender(Gender.OTHER)
                    .birthDate(java.time.LocalDate.of(1990, 1, 1))
                    .roleEntities(Set.of(adminRoleEntity, userRoleEntity))
                    .enabled(true)
                    .build();

            userRepository.save(adminUserEntity);

            logger.info("✅ Admin user created: {} / {}", adminUserEntity.getEmail(), adminUserEntity.getPassword());
        } else {
            logger.info("⚠️ Admin user already exists!");
        }
    }

    private void createDefaultConfigSettings() {
        for (ConfigEntry configEntry : ConfigEntry.values()) {
            if (!configService.exists(configEntry)) {
                configService.set(configEntry, configEntry.getDefaultValueString());
                logger.info("✅ ConfigEntity entry created: {} / {}", configEntry.getKey(), configEntry.getDefaultValueString());
            } else {
                logger.info("⚠️ ConfigEntity entry {} exists!", configEntry.getKey());
            }
        }
    }
}
