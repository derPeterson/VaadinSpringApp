package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Real JPA queries and Spring transactions on an isolated in-memory H2 database. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
class UserServicePersistenceTest {
    @Autowired
    private UserService service;
    @Autowired
    private UserRepository repository;
    @Autowired
    private TransactionTemplate transaction;
    @PersistenceContext
    private EntityManager entityManager;

    private Long adminId;

    @BeforeEach
    void seedDatabase() {
        transaction.executeWithoutResult(status -> {
            repository.deleteAll();
            repository.flush();
            entityManager.createQuery("delete from RoleEntity").executeUpdate();
            RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
            entityManager.persist(role);
            UserEntity admin = newUser("admin@example.com", role, true);
            entityManager.persist(admin);
            adminId = admin.getId();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void dirtyManagedAdminCannotBeDisabledAndTheTransactionRollsBack(boolean useSaveUser) {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            UserEntity admin = repository.findById(adminId).orElseThrow();
            admin.setEnabled(false);
            if (useSaveUser) {
                service.saveUser(admin);
            } else {
                service.save(admin);
            }
        }));

        assertEquals(List.of(adminId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
        assertTrue(repository.findById(adminId).orElseThrow().isEnabled());
    }

    @Test
    void dirtyManagedAdminCannotLoseItsRoleBeforeTheGuardQuery() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            UserEntity admin = repository.findById(adminId).orElseThrow();
            admin.getRoleEntities().clear();
            service.saveUser(admin);
        }));

        assertEquals(List.of(adminId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    @Test
    void detachedLastAdminCannotBeDeletedEvenWithStalePrivileges() {
        UserEntity stale = repository.findById(adminId).orElseThrow();
        stale.setEnabled(false);
        stale.setRoleEntities(List.of());

        assertThrows(IllegalStateException.class, () -> service.deleteUser(stale));

        assertTrue(repository.existsById(adminId));
        assertEquals(List.of(adminId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    @Test
    void queryExcludesDisabledAdminsAndUsersWithoutTheAdminRole() {
        transaction.executeWithoutResult(status -> {
            RoleEntity adminRole = entityManager.createQuery("select r from RoleEntity r", RoleEntity.class).getSingleResult();
            entityManager.persist(newUser("disabled@example.com", adminRole, false));
            RoleEntity ordinaryRole = RoleEntity.builder().name(RoleType.ROLE_USER).build();
            entityManager.persist(ordinaryRole);
            entityManager.persist(newUser("ordinary@example.com", ordinaryRole, true));
        });

        assertEquals(List.of(adminId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    @Test
    void demotionCommitsWhenAnotherEnabledAdminRemains() {
        Long otherId = transaction.execute(status -> {
            RoleEntity role = entityManager.createQuery("select r from RoleEntity r", RoleEntity.class).getSingleResult();
            UserEntity other = newUser("other@example.com", role, true);
            entityManager.persist(other);
            return other.getId();
        });

        transaction.executeWithoutResult(status -> {
            UserEntity admin = repository.findById(adminId).orElseThrow();
            admin.setEnabled(false);
            service.save(admin);
        });

        assertFalse(repository.findById(adminId).orElseThrow().isEnabled());
        assertEquals(List.of(otherId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    private static UserEntity newUser(String email, RoleEntity role, boolean enabled) {
        return UserEntity.builder().firstName("Test").lastName("User").email(email).password("hash")
                .gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH)
                .enabled(enabled).roleEntities(new ArrayList<>(List.of(role))).build();
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:admin-guard-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
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

        @Bean
        TransactionTemplate transactionTemplate(JpaTransactionManager manager) {
            return new TransactionTemplate(manager);
        }

        @Bean
        UserService userService(UserRepository repository) {
            return new UserService(repository, mock(UserStatusBroadcaster.class), mock(PasswordEncoder.class));
        }
    }
}
