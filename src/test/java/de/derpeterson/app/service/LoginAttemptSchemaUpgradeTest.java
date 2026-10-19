package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.LoginAttemptEntity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Old schema and restarted persistence contexts only in an isolated in-memory database. */
class LoginAttemptSchemaUpgradeTest {
  @Test
  void createsOnlyNewTablesPreservesExistingAccountAndRetainsBlockAcrossFactoryRestart()
      throws Exception {
    DriverManagerDataSource source =
        new DriverManagerDataSource("jdbc:h2:mem:login-upgrade-" + UUID.randomUUID(), "sa", "");
    try (Connection anchor = source.getConnection()) {
      LocalContainerEntityManagerFactoryBean old = factory(source);
      Long userId;
      try (EntityManager em = old.getObject().createEntityManager()) {
        em.getTransaction().begin();
        RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
        em.persist(role);
        UserEntity user =
            UserEntity.builder()
                .email("existing-upgrade@example.test")
                .password("unchanged-hash")
                .firstName("Existing")
                .lastName("Account")
                .enabled(true)
                .gender(Gender.OTHER)
                .birthDate(LocalDate.of(1990, 1, 1))
                .roleEntities(List.of(role))
                .build();
        em.persist(user);
        em.getTransaction().commit();
        userId = user.getId();
      } finally {
        old.destroy();
      }
      JdbcTemplate jdbc = new JdbcTemplate(source);
      // Only the freshly constructed fixture loses these tables, simulating the previous app
      // schema.
      jdbc.execute("drop table login_attempts");
      jdbc.execute("drop table login_attempt_guard");
      LocalContainerEntityManagerFactoryBean upgraded = factory(source);
      Instant until = Instant.parse("2026-10-16T12:15:00Z");
      String key = LoginAttemptService.identityKey("existing-upgrade@example.test");
      try (EntityManager em = upgraded.getObject().createEntityManager()) {
        em.getTransaction().begin();
        LoginAttemptEntity row = new LoginAttemptEntity();
        row.setIdentityHash(key);
        row.setFailures(10);
        row.setWindowStartedAt(until.minusSeconds(1000));
        row.setLastActivity(until.minusSeconds(900));
        row.setBlockedUntil(until);
        em.persist(row);
        em.getTransaction().commit();
      } finally {
        upgraded.destroy();
      }
      LocalContainerEntityManagerFactoryBean restarted = factory(source);
      try (EntityManager em = restarted.getObject().createEntityManager()) {
        LoginAttemptEntity row = em.find(LoginAttemptEntity.class, key);
        assertEquals(10, row.getFailures());
        assertEquals(until, row.getBlockedUntil());
        UserEntity existing = em.find(UserEntity.class, userId);
        assertEquals("unchanged-hash", existing.getPassword());
        assertTrue(existing.isEnabled());
        assertTrue(existing.hasRole(RoleType.ROLE_USER));
        assertEquals(
            1L, em.createQuery("select count(u) from UserEntity u", Long.class).getSingleResult());
      } finally {
        restarted.destroy();
      }
    }
  }

  private LocalContainerEntityManagerFactoryBean factory(javax.sql.DataSource source) {
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setPackagesToScan("de.derpeterson.app.model");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "update"));
    factory.afterPropertiesSet();
    return factory;
  }
}
