package de.derpeterson.app.config;

import de.derpeterson.app.model.Role;
import de.derpeterson.app.model.User;
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
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = Role.builder().name("ROLE_ADMIN").build();
                    return roleRepository.save(role);
                });

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = Role.builder().name("ROLE_USER").build();
                    return roleRepository.save(role);
                });

        logger.info("✅ Rollen geprüft oder erstellt: ROLE_ADMIN, ROLE_USER");

        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User admin = User.builder()
                    .password(passwordEncoder.encode("Admin@123"))
                    .email("admin@example.com")
                    .gender(User.Gender.MALE)
                    .birthDate(java.time.LocalDate.of(1990, 1, 1))
                    .roles(Set.of(adminRole, userRole))
                    .enabled(true)
                    .build();

            userRepository.save(admin);
            logger.info("✅ Admin user created: {} / {}", admin.getEmail(), admin.getPassword());
        } else {
            logger.info("⚠️ Admin user already exists!");
        }
    }

    private void createDefaultConfigSettings() {
        for (ConfigSetting setting : ConfigSetting.values()) {
            if (!configService.exists(setting)) {
                configService.set(setting, setting.getDefaultValueString());
                logger.info("✅ Config entry created: {} / {}", setting.getKey(), setting.getDefaultValueString());
            } else {
                logger.info("⚠️ Config entry {} exists!", setting.getKey());
            }
        }
    }
}
