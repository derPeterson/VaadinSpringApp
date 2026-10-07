package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Real derived queries and constraints; no application, SMTP or file database. */
@SpringJUnitConfig(RoleServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
class RoleServicePersistenceTest {
    @Autowired
    private RoleRepository repository;
    private RoleService service;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
        service = new RoleService(repository);
    }

    @ParameterizedTest
    @EnumSource(RoleType.class)
    void findsExactlyTheRequestedPersistedRole(RoleType name) {
        for (var role : RoleType.values()) {
            repository.saveAndFlush(RoleEntity.builder().name(role).build());
        }

        var found = service.findByName(name).orElseThrow();

        assertEquals(name, found.getName());
        assertNotNull(found.getId());
        assertEquals(repository.findByName(name).orElseThrow().getId(), found.getId());
        assertEquals(RoleType.values().length, repository.count());
    }

    @ParameterizedTest
    @EnumSource(RoleType.class)
    void absentRoleIsNotConfusedWithAnotherRoleOrCreated(RoleType name) {
        var other = name == RoleType.ROLE_ADMIN ? RoleType.ROLE_USER : RoleType.ROLE_ADMIN;
        repository.saveAndFlush(RoleEntity.builder().name(other).build());

        assertTrue(service.findByName(name).isEmpty());
        assertEquals(1, repository.count());
        assertTrue(service.findByName(other).isPresent());
    }

    @Test
    void emptyDatabaseAndNullLookupReturnEmptyWithoutWrites() {
        for (var name : RoleType.values()) {
            assertTrue(service.findByName(name).isEmpty());
        }
        assertTrue(service.findByName(null).isEmpty());
        repository.saveAndFlush(RoleEntity.builder().name(RoleType.ROLE_USER).build());
        assertTrue(service.findByName(null).isEmpty());
        assertEquals(1, repository.count());
    }

    @Test
    void uniqueNameConstraintRejectsDuplicatesAndPreservesOriginalLookup() {
        var original = repository.saveAndFlush(RoleEntity.builder().name(RoleType.ROLE_USER).build());

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(
                RoleEntity.builder().name(RoleType.ROLE_USER).build()));

        assertEquals(1, repository.count());
        assertEquals(original.getId(), service.findByName(RoleType.ROLE_USER).orElseThrow().getId());
    }

    @Test
    void nullNameCannotBePersistedAndDoesNotLeaveARow() {
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(
                RoleEntity.builder().build()));

        assertEquals(0, repository.count());
        assertTrue(service.findByName(null).isEmpty());
    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = RoleRepository.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:roles-" + UUID.randomUUID()
                    + ";DB_CLOSE_DELAY=-1", "sa", "");
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
