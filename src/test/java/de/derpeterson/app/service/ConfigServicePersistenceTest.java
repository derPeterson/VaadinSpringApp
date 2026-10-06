package de.derpeterson.app.service;

import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** Real JPA transactions and constraints; no application, SMTP or file database. */
@SpringJUnitConfig(ConfigServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
class ConfigServicePersistenceTest {
    @Autowired
    private ConfigRepository repository;
    @Autowired
    private JpaTransactionManager transactionManager;
    private ConfigService service;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
        service = new ConfigService(repository, transactionManager);
    }

    @Test
    void createAndUpdatePreserveIdentityAndVerbatimValues() {
        service.set(ConfigEntry.SERVICE_NAME, "  Grüße  ");
        var created = repository.findByKey(ConfigEntry.SERVICE_NAME.getKey()).orElseThrow();
        assertNotNull(created.getId());
        assertEquals("  Grüße  ", created.getValue());

        service.set(ConfigEntry.SERVICE_NAME, "");

        var updated = repository.findByKey(created.getKey()).orElseThrow();
        assertEquals(created.getId(), updated.getId());
        assertEquals("", updated.getValue());
        assertEquals(1, repository.count());
    }

    @Test
    void rejectedNullDoesNotCreateOrModifyARow() {
        assertThrows(NullPointerException.class, () -> service.set(ConfigEntry.SERVICE_NAME, null));
        assertEquals(0, repository.count());
        service.set(ConfigEntry.SERVICE_NAME, "old");
        assertThrows(NullPointerException.class, () -> service.set(ConfigEntry.SERVICE_NAME, null));
        assertEquals("old", service.getString(ConfigEntry.SERVICE_NAME));
    }

    @Test
    void databaseStillEnforcesNonNullAndUniqueKeyConstraints() {
        service.set(ConfigEntry.SERVICE_NAME, "old");

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(
                new ConfigEntity(null, ConfigEntry.SERVICE_NAME.getKey(), "duplicate")));
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(
                new ConfigEntity(null, ConfigEntry.BASE_URL.getKey(), null)));
        assertEquals(1, repository.count());
        assertEquals("old", service.getString(ConfigEntry.SERVICE_NAME));
    }

    @Test
    void setCommitsIndependentlyOfAnOuterRollback() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.set(ConfigEntry.SERVICE_NAME, "committed");
            status.setRollbackOnly();
        });

        assertEquals("committed", service.getString(ConfigEntry.SERVICE_NAME));
    }

    @RepeatedTest(3)
    void concurrentMissingKeyInsertsRecoverInAFreshTransaction() throws Exception {
        var bothReadMissing = new CyclicBarrier(2);
        var missingReads = new AtomicInteger();
        var conflicts = new AtomicInteger();
        var insertedId = new AtomicReference<Long>();
        var retryValue = new AtomicReference<String>();
        // Only coordinate the reads; all queries, inserts, commits and rollbacks
        // go to the real repository. Both transactions must see the missing key.
        var coordinated = mock(ConfigRepository.class, delegatesTo(repository));
        doAnswer(invocation -> {
            var result = repository.findByKey(invocation.getArgument(0));
            if (result.isEmpty()) {
                missingReads.incrementAndGet();
                bothReadMissing.await(10, TimeUnit.SECONDS);
            }
            return result;
        }).when(coordinated).findByKey(ConfigEntry.SERVICE_NAME.getKey());
        doAnswer(invocation -> {
            ConfigEntity entity = invocation.getArgument(0);
            boolean inserting = entity.getId() == null;
            try {
                var saved = repository.saveAndFlush(entity);
                if (inserting) {
                    insertedId.set(saved.getId());
                }
                return saved;
            } catch (DataIntegrityViolationException failure) {
                conflicts.incrementAndGet();
                retryValue.set(entity.getValue());
                throw failure;
            }
        }).when(coordinated).saveAndFlush(any());
        // Distinct service instances: no JVM-local lock can hide the DB race.
        var first = new ConfigService(coordinated, transactionManager);
        var second = new ConfigService(coordinated, transactionManager);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstWrite = executor.submit(() -> first.set(ConfigEntry.SERVICE_NAME, "first"));
            var secondWrite = executor.submit(() -> second.set(ConfigEntry.SERVICE_NAME, "second"));
            firstWrite.get(20, TimeUnit.SECONDS);
            secondWrite.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(2, missingReads.get());
        assertEquals(1, conflicts.get(), "The test must exercise a real unique-key violation");
        assertEquals(1, repository.count());
        var stored = repository.findByKey(ConfigEntry.SERVICE_NAME.getKey()).orElseThrow();
        assertEquals(insertedId.get(), stored.getId());
        assertEquals(retryValue.get(), stored.getValue(), "The retry is the last committed write");
        verify(coordinated, times(3)).findByKey(ConfigEntry.SERVICE_NAME.getKey());
        verify(coordinated, times(3)).saveAndFlush(any());
        // Neither a failed persistence context nor rollback-only state leaks.
        service.set(ConfigEntry.SERVICE_NAME, "after race");
        assertEquals("after race", service.getString(ConfigEntry.SERVICE_NAME));
    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = ConfigRepository.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:config-" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
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
