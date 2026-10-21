package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.LoginAttemptEntity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserNotificationType;
import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Aktuelles Schema und Wiederanlauf auf eigenen H2-Daten, ohne Anwendungsserver. */
class CurrentSchemaPersistenceTest {
  private DriverManagerDataSource source;

  @BeforeEach
  void setup() {
    source =
        new DriverManagerDataSource(
            "jdbc:h2:mem:current-schema-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    initialize();
  }

  private void initialize() {
    new ResourceDatabasePopulator(new ClassPathResource("db/application-schema-h2.sql"))
        .execute(source);
  }

  private LocalContainerEntityManagerFactoryBean factory() {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setPackagesToScan("de.derpeterson.app.model");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of(
            "hibernate.hbm2ddl.auto", "validate",
            "hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl",
            "hibernate.implicit_naming_strategy",
                "org.springframework.boot.hibernate.SpringImplicitNamingStrategy"));
    factory.afterPropertiesSet();
    return factory;
  }

  @Test
  void currentSchemaValidatesAgainstEveryJpaMappingAndAcceptsAllCurrentEnumValues() {
    LocalContainerEntityManagerFactoryBean factory = factory();
    try {
      JdbcTemplate jdbc = new JdbcTemplate(source);
      for (RoleType role : RoleType.values()) {
        jdbc.update("insert into roles(name) values(?)", role.name());
      }
      for (UserNotificationType type : UserNotificationType.values()) {
        jdbc.update(
            "insert into user_notifications(request_key,type,created_at,broadcast,recipient_count) values(?,?,?,?,?)",
            "fixture-" + type.name(),
            type.name(),
            java.sql.Timestamp.from(Instant.now()),
            false,
            0);
      }
      assertEquals(
          RoleType.values().length,
          jdbc.queryForObject("select count(*) from roles", Integer.class));
      assertEquals(
          UserNotificationType.values().length,
          jdbc.queryForObject("select count(*) from user_notifications", Integer.class));
    } finally {
      factory.destroy();
    }
  }

  @Test
  void repeatedInitializationAndJpaRestartKeepTheSameAccountVersionAndLoginBlock() {
    LocalContainerEntityManagerFactoryBean first = factory();
    Long userId;
    Instant until = Instant.parse("2026-10-21T10:15:00Z");
    try (EntityManager manager = first.getObject().createEntityManager()) {
      manager.getTransaction().begin();
      RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
      manager.persist(role);
      UserEntity user =
          UserEntity.builder()
              .email("fixture@example.test")
              .password("fixture-encoded-password")
              .firstName("Fixture")
              .lastName("Account")
              .enabled(true)
              .gender(Gender.OTHER)
              .birthDate(LocalDate.of(1990, 1, 1))
              .roleEntities(List.of(role))
              .build();
      manager.persist(user);
      LoginAttemptEntity block = new LoginAttemptEntity();
      block.setIdentityHash("fixture-identity-hash");
      block.setFailures(10);
      block.setWindowStartedAt(until.minusSeconds(1000));
      block.setLastActivity(until.minusSeconds(900));
      block.setBlockedUntil(until);
      manager.persist(block);
      manager.getTransaction().commit();
      userId = user.getId();
      assertEquals(0L, user.getVersion());
    } finally {
      first.destroy();
    }
    initialize();
    LocalContainerEntityManagerFactoryBean restarted = factory();
    try (EntityManager manager = restarted.getObject().createEntityManager()) {
      UserEntity user = manager.find(UserEntity.class, userId);
      assertEquals(0L, user.getVersion());
      assertEquals("fixture-encoded-password", user.getPassword());
      assertTrue(user.hasRole(RoleType.ROLE_USER));
      LoginAttemptEntity block = manager.find(LoginAttemptEntity.class, "fixture-identity-hash");
      assertEquals(10, block.getFailures());
      assertEquals(until, block.getBlockedUntil());
    } finally {
      restarted.destroy();
    }
  }

  @Test
  void maximumValidAccountIdentityFitsTheCurrentUserSchema() {
    String identity =
        "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62);
    assertEquals(255, identity.length());
    LocalContainerEntityManagerFactoryBean factory = factory();
    try (EntityManager manager = factory.getObject().createEntityManager()) {
      manager.getTransaction().begin();
      UserEntity user =
          UserEntity.builder()
              .email(identity)
              .password("fixture-encoded-password")
              .firstName("Fixture")
              .lastName("Account")
              .enabled(true)
              .gender(Gender.OTHER)
              .birthDate(LocalDate.of(1990, 1, 1))
              .roleEntities(List.of())
              .build();
      manager.persist(user);
      manager.getTransaction().commit();
      manager.clear();
      assertEquals(identity, manager.find(UserEntity.class, user.getId()).getEmail());
    } finally {
      factory.destroy();
    }
  }

  @Test
  void applicationConfigurationCreatesTheCurrentSchemaAndOnlyValidatesJpa() throws Exception {
    Properties settings = new Properties();
    try (InputStream input = new ClassPathResource("application.properties").getInputStream()) {
      settings.load(input);
    }
    assertEquals("validate", settings.getProperty("spring.jpa.hibernate.ddl-auto"));
    assertEquals("always", settings.getProperty("spring.sql.init.mode"));
    assertEquals(
        "classpath:db/application-schema-h2.sql",
        settings.getProperty("spring.sql.init.schema-locations"));
  }
}
