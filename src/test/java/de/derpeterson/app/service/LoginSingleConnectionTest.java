package de.derpeterson.app.service;

import com.zaxxer.hikari.HikariDataSource;
import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(LoginSingleConnectionTest.Config.class)
class LoginSingleConnectionTest {
    @Autowired HikariDataSource source;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired LoginAttemptService attempts;
    @Autowired LoginAttemptRepository records;

    @Test
    void protectedPasswordLookupSharesTheOnlyConnectionAndKeepsItsCounter() {
        var encoder = new BCryptPasswordEncoder(4);
        var role = roles.saveAndFlush(RoleEntity.builder().name(RoleType.ROLE_USER).build());
        String email = "one-connection@example.test";
        users.saveAndFlush(UserEntity.builder().email(email).firstName("Single").lastName("Connection")
                .password(encoder.encode("OriginalPassword!")).enabled(true).gender(Gender.OTHER)
                .birthDate(LocalDate.of(1990, 1, 1)).roleEntities(List.of(role)).build());
        var provider = new DaoAuthenticationProvider(new CustomUserDetailsService(users)::loadPasswordUserByUsername);
        provider.setPasswordEncoder(encoder);
        var login = new LoginAuthenticationService(provider, attempts);
        assertTrue(login.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, "OriginalPassword!")).isAuthenticated());
        assertThrows(BadCredentialsException.class, () -> login.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, "WrongPassword!")));
        assertEquals(1, records.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
        assertEquals(0, source.getHikariPoolMXBean().getActiveConnections());
        assertEquals(1, source.getMaximumPoolSize());
    }

    @Configuration @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Config {
        @Bean HikariDataSource dataSource() {
            var source = new HikariDataSource(); source.setJdbcUrl("jdbc:h2:mem:single-login-" + UUID.randomUUID());
            source.setUsername("sa"); source.setPassword(""); source.setMaximumPoolSize(1);
            source.setMinimumIdle(1); source.setConnectionTimeout(1000); return source;
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(HikariDataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(source);
            factory.setPackagesToScan("de.derpeterson.app.model"); factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop")); return factory;
        }
        @Bean JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean ConfigService config() {
            var config = mock(ConfigService.class); when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(2); return config;
        }
        @Bean LoginAttemptService attempts(LoginAttemptRepository attempts, LoginAttemptGuardRepository guard,
                ConfigService config, JpaTransactionManager manager) {
            return new LoginAttemptService(attempts, guard, config, manager);
        }
    }
}
