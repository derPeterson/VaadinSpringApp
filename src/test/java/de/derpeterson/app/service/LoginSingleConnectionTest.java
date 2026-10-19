package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.LoginAttemptGuardRepository;
import de.derpeterson.app.repository.LoginAttemptRepository;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.LoginAuthenticationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringJUnitConfig(LoginSingleConnectionTest.Config.class)
class LoginSingleConnectionTest {
  @Autowired HikariDataSource source;
  @Autowired UserRepository users;
  @Autowired RoleRepository roles;
  @Autowired LoginAttemptService attempts;
  @Autowired LoginAttemptRepository records;

  @Test
  void protectedPasswordLookupSharesTheOnlyConnectionAndKeepsItsCounter() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    RoleEntity role = roles.saveAndFlush(RoleEntity.builder().name(RoleType.ROLE_USER).build());
    String email = "one-connection@example.test";
    users.saveAndFlush(
        UserEntity.builder()
            .email(email)
            .firstName("Single")
            .lastName("Connection")
            .password(encoder.encode("OriginalPassword!"))
            .enabled(true)
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .roleEntities(List.of(role))
            .build());
    DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(
            new CustomUserDetailsService(users)::loadPasswordUserByUsername);
    provider.setPasswordEncoder(encoder);
    LoginAuthenticationService login = new LoginAuthenticationService(provider, attempts);
    assertTrue(
        login
            .authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, "OriginalPassword!"))
            .isAuthenticated());
    assertThrows(
        BadCredentialsException.class,
        () ->
            login.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, "WrongPassword!")));
    assertEquals(
        1, records.findById(LoginAttemptService.identityKey(email)).orElseThrow().getFailures());
    assertEquals(0, source.getHikariPoolMXBean().getActiveConnections());
    assertEquals(1, source.getMaximumPoolSize());
  }

  @Configuration
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  static class Config {
    @Bean
    HikariDataSource dataSource() {
      HikariDataSource source = new HikariDataSource();
      source.setJdbcUrl("jdbc:h2:mem:single-login-" + UUID.randomUUID());
      source.setUsername("sa");
      source.setPassword("");
      source.setMaximumPoolSize(1);
      source.setMinimumIdle(1);
      source.setConnectionTimeout(1000);
      return source;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(HikariDataSource source) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(source);
      factory.setPackagesToScan("de.derpeterson.app.model");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
      return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }

    @Bean
    ConfigService config() {
      ConfigService config = mock(ConfigService.class);
      when(config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT)).thenReturn(2);
      return config;
    }

    @Bean
    LoginAttemptService attempts(
        LoginAttemptRepository attempts,
        LoginAttemptGuardRepository guard,
        ConfigService config,
        JpaTransactionManager manager) {
      return new LoginAttemptService(attempts, guard, config, manager);
    }
  }
}
