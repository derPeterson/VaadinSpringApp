package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.ConfigRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.UserRepository.SecurityUserRow;
import de.derpeterson.app.security.AdminRolePolicy;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

/** Real JPA transactions and constraints; no application, SMTP or file database. */
@SpringJUnitConfig(ConfigServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
class ConfigServicePersistenceTest {
  @Autowired private ConfigRepository configRepository;
  @Autowired private JpaTransactionManager transactionManager;
  private ConfigService configService;

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_SUPERADMIN", "ROLE_ADMIN", "ROLE_USER", "ANONYMOUS"})
  void accountRetentionReadAndWriteRequireFreshSuperadminRole(String roleName) {
    UserRepository securityUserRepository = mock(UserRepository.class);
    SecurityUserRow securityUserRow = mock(SecurityUserRow.class);
    ConfigService securedConfigService =
        new ConfigService(
            configRepository,
            transactionManager,
            new MockEnvironment(),
            new AdminRolePolicy(securityUserRepository));
    securedConfigService.initializeIfMissing(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS, "30");
    securedConfigService.initializeIfMissing(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS, "999");
    assertEquals(30, securedConfigService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS));

    try {
      if (!roleName.equals("ANONYMOUS")) {
        UserDetails adminUserDetails =
            User.withUsername("admin@example.test").password("test").authorities(roleName).build();
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    adminUserDetails, null, adminUserDetails.getAuthorities()));
        when(securityUserRepository.findSecurityUser(adminUserDetails.getUsername()))
            .thenReturn(List.of(securityUserRow));
        when(securityUserRow.isEnabled()).thenReturn(true);
        when(securityUserRow.getRole()).thenReturn(RoleType.valueOf(roleName));
      }
      if (roleName.equals("ROLE_SUPERADMIN")) {
        assertEquals(
            "30",
            securedConfigService.getForAdministration(
                ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS));
        securedConfigService.set(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS, "60");
        securedConfigService.set(ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS, "0");
        assertEquals(
            60, securedConfigService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS));
        assertThrows(
            IllegalArgumentException.class,
            () -> securedConfigService.set(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS, "0"));
        assertThrows(
            IllegalArgumentException.class,
            () -> securedConfigService.set(ConfigEntry.SELF_DEACTIVATION_REMINDER_DAYS, "-1"));
        // Ein während der Sitzung entzogener Superadminstatus wirkt beim nächsten Dienstaufruf.
        when(securityUserRow.getRole()).thenReturn(RoleType.ROLE_ADMIN);
      }

      assertThrows(
          AccessDeniedException.class,
          () ->
              securedConfigService.getForAdministration(
                  ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS));
      assertThrows(
          AccessDeniedException.class,
          () -> securedConfigService.set(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS, "99"));
      assertThrows(
          IllegalArgumentException.class,
          () ->
              securedConfigService.saveSmtpSettings(
                  Map.of(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS, "99")));
      assertEquals(
          roleName.equals("ROLE_SUPERADMIN") ? 60 : 30,
          securedConfigService.getInteger(ConfigEntry.SELF_DEACTIVATION_RETENTION_DAYS));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @BeforeEach
  void clearDatabase() {
    configRepository.deleteAll();
    configService =
        new ConfigService(
            configRepository,
            transactionManager,
            new MockEnvironment(),
            mock(AdminRolePolicy.class));
  }

  @Test
  void createAndUpdatePreserveIdentityAndVerbatimValues() {
    configService.set(ConfigEntry.SERVICE_NAME, "  Grüße  ");
    ConfigEntity created =
        configRepository.findByKey(ConfigEntry.SERVICE_NAME.getKey()).orElseThrow();

    assertNotNull(created.getId());
    assertEquals("  Grüße  ", created.getValue());

    configService.set(ConfigEntry.SERVICE_NAME, "");

    ConfigEntity updated = configRepository.findByKey(created.getKey()).orElseThrow();

    assertEquals(created.getId(), updated.getId());
    assertEquals("", updated.getValue());
    assertEquals(1, configRepository.count());
  }

  @Test
  void rejectedNullDoesNotCreateOrModifyARow() {
    assertThrows(
        NullPointerException.class, () -> configService.set(ConfigEntry.SERVICE_NAME, null));
    assertEquals(0, configRepository.count());
    configService.set(ConfigEntry.SERVICE_NAME, "old");

    assertThrows(
        NullPointerException.class, () -> configService.set(ConfigEntry.SERVICE_NAME, null));
    assertEquals("old", configService.getString(ConfigEntry.SERVICE_NAME));
  }

  @Test
  void databaseStillEnforcesNonNullAndUniqueKeyConstraints() {
    configService.set(ConfigEntry.SERVICE_NAME, "old");

    ConfigEntity configEntity =
        new ConfigEntity(null, ConfigEntry.SERVICE_NAME.getKey(), "duplicate");

    assertThrows(
        DataIntegrityViolationException.class, () -> configRepository.saveAndFlush(configEntity));
    ConfigEntity configEntity2 = new ConfigEntity(null, ConfigEntry.BASE_URL.getKey(), null);

    assertThrows(
        DataIntegrityViolationException.class, () -> configRepository.saveAndFlush(configEntity2));
    assertEquals(1, configRepository.count());
    assertEquals("old", configService.getString(ConfigEntry.SERVICE_NAME));
  }

  @Test
  void setCommitsIndependentlyOfAnOuterRollback() {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(status -> {
          configService.set(ConfigEntry.SERVICE_NAME, "committed");
          status.setRollbackOnly();
        });

    assertEquals("committed", configService.getString(ConfigEntry.SERVICE_NAME));
  }

  @RepeatedTest(3)
  void concurrentMissingKeyInsertsRecoverInAFreshTransaction() throws Exception {
    CyclicBarrier bothReadMissing = new CyclicBarrier(2);
    AtomicInteger missingReads = new AtomicInteger();
    AtomicInteger conflicts = new AtomicInteger();
    AtomicReference<Long> insertedId = new AtomicReference<Long>();
    AtomicReference<String> retryValue = new AtomicReference<String>();
    // Only coordinate the reads; all queries, inserts, commits and rollbacks
    // go to the real repository. Both transactions must see the missing key.
    ConfigRepository coordinated = mock(ConfigRepository.class, delegatesTo(configRepository));
    doAnswer(
            invocation -> {
              Optional<ConfigEntity> result = configRepository.findByKey(invocation.getArgument(0));
              if (result.isEmpty()) {
                missingReads.incrementAndGet();
                bothReadMissing.await(10, TimeUnit.SECONDS);
              }
              return result;
            })
        .when(coordinated)
        .findByKey(ConfigEntry.SERVICE_NAME.getKey());
    doAnswer(
            invocation -> {
              ConfigEntity entity = invocation.getArgument(0);
              boolean inserting = entity.getId() == null;
              try {
                ConfigEntity saved = configRepository.saveAndFlush(entity);
                if (inserting) {
                  insertedId.set(saved.getId());
                }
                return saved;
              } catch (DataIntegrityViolationException failure) {
                conflicts.incrementAndGet();
                retryValue.set(entity.getValue());
                throw failure;
              }
            })
        .when(coordinated)
        .saveAndFlush(any());
    // Distinct service instances: no JVM-local lock can hide the DB race.
    ConfigService first =
        new ConfigService(
            coordinated, transactionManager, new MockEnvironment(), mock(AdminRolePolicy.class));
    ConfigService second =
        new ConfigService(
            coordinated, transactionManager, new MockEnvironment(), mock(AdminRolePolicy.class));
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try {
      Future<?> firstWrite =
          executorService.submit(() -> first.set(ConfigEntry.SERVICE_NAME, "first"));
      Future<?> secondWrite =
          executorService.submit(() -> second.set(ConfigEntry.SERVICE_NAME, "second"));
      firstWrite.get(20, TimeUnit.SECONDS);
      secondWrite.get(20, TimeUnit.SECONDS);
    } finally {
      executorService.shutdownNow();

      assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }

    assertEquals(2, missingReads.get());
    assertEquals(1, conflicts.get(), "The test must exercise a real unique-key violation");
    assertEquals(1, configRepository.count());
    ConfigEntity stored =
        configRepository.findByKey(ConfigEntry.SERVICE_NAME.getKey()).orElseThrow();

    assertEquals(insertedId.get(), stored.getId());
    assertEquals(retryValue.get(), stored.getValue(), "The retry is the last committed write");
    verify(coordinated, times(3)).findByKey(ConfigEntry.SERVICE_NAME.getKey());
    verify(coordinated, times(3)).saveAndFlush(any());
    // Neither a failed persistence context nor rollback-only state leaks.
    configService.set(ConfigEntry.SERVICE_NAME, "after race");

    assertEquals("after race", configService.getString(ConfigEntry.SERVICE_NAME));
  }

  @Configuration
  @EnableJpaRepositories(basePackageClasses = ConfigRepository.class)
  static class Config {
    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
          "jdbc:h2:mem:config-"
              + UUID.randomUUID()
              + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
          "sa",
          "");
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(dataSource);
      factory.setPackagesToScan("de.derpeterson.app.model");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
      return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }
  }
}
