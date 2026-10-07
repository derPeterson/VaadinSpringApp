package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Committed isolated H2 transactions; no application/session/database startup. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
class AdminSecurityPersistenceTest {
    @Autowired UserService service;
    @Autowired UserRepository repository;
    @Autowired TransactionTemplate transaction;
    @PersistenceContext EntityManager entityManager;
    private Long firstId;
    private Long secondId;
    private Long ordinaryId;

    @BeforeEach
    void seed() {
        transaction.executeWithoutResult(tx -> {
            repository.deleteAll();
            repository.flush();
            entityManager.createQuery("delete from RoleEntity").executeUpdate();
            var admin = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
            var ordinary = RoleEntity.builder().name(RoleType.ROLE_USER).build();
            entityManager.persist(admin);
            entityManager.persist(ordinary);
            firstId = create("first-admin@example.com", admin);
            secondId = create("second-admin@example.com", admin);
            ordinaryId = create("ordinary@example.com", ordinary);
        });
    }

    private Long create(String email, RoleEntity role) {
        var user = UserEntity.builder().email(email).firstName("Test").lastName("User").password("hash")
                .birthDate(LocalDate.of(1990, 1, 1)).gender(Gender.OTHER).enabled(true).preferredLocale(Locale.ENGLISH)
                .roleEntities(new ArrayList<>(List.of(role))).build();
        entityManager.persist(user);
        return user.getId();
    }

    private UserEntity form(Long id) {
        return repository.findAll().stream().filter(user -> user.getId().equals(id)).findFirst().orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"disable", "role", "form", "delete", "mixed"})
    void parallelMutationsCanNeverRemoveBothAdmins(String mutation) throws Exception {
        var readBarrier = new CyclicBarrier(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> compete(firstId, mutation.equals("mixed") ? "delete" : mutation, readBarrier));
            var second = workers.submit(() -> compete(secondId, mutation.equals("mixed") ? "form" : mutation, readBarrier));
            boolean firstCommitted = first.get(10, TimeUnit.SECONDS);
            boolean secondCommitted = second.get(10, TimeUnit.SECONDS);
            assertNotEquals(firstCommitted, secondCommitted, "Exactly one removal must commit; the other must reject");
        }
        assertEquals(1, repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN).size());
        assertTrue(repository.existsById(ordinaryId));
    }

    private boolean compete(Long id, String mutation, CyclicBarrier barrier) {
        try {
            transaction.executeWithoutResult(tx -> {
                UserEntity candidate = form(id);
                // Both preflights see two admins; only the locked mutation is
                // authoritative once the competing request has committed.
                assertTrue(service.canDeleteUser(candidate));
                assertFalse(service.wouldRemoveLastEnabledAdmin(id, false, List.of()));
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
                change(candidate, mutation);
            });
            return true;
        } catch (IllegalStateException rejection) {
            assertTrue(rejection.getMessage().contains("letzte aktive Administrator"));
            return false;
        }
    }

    private void change(UserEntity user, String mutation) {
        if (mutation.equals("delete")) {
            service.deleteUser(user);
        } else {
            if (mutation.equals("role")) {
                user.getRoleEntities().clear();
            } else {
                user.setEnabled(false);
            }
            if (mutation.equals("form")) {
                service.updateAdminUser(user, user.getVersion(), "");
            } else if (mutation.equals("role")) {
                service.saveUser(user);
            } else {
                service.save(user);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"disable", "role", "form", "delete"})
    void twoSequentialRemovalsInOneTransactionRollBackTheWholeBatch(String mutation) {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(tx -> {
            change(form(firstId), mutation);
            assertFalse(service.canDeleteUser(form(secondId)));
            assertTrue(service.wouldRemoveLastEnabledAdmin(secondId, false, List.of()));
            change(form(secondId), mutation);
        }));
        assertEquals(List.of(firstId, secondId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void flushOfMultipleAlreadyDirtyUsersCannotBypassTheGuard(boolean submitOrdinaryUser) {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(tx -> {
            var users = repository.findAll();
            users.stream().filter(user -> user.getId().equals(firstId) || user.getId().equals(secondId))
                    .forEach(user -> user.setEnabled(false));
            service.save(users.stream().filter(user -> user.getId().equals(submitOrdinaryUser ? ordinaryId : firstId)).findFirst().orElseThrow());
        }));
        assertEquals(2, repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN).size());
    }

    @Test
    void promotionBeforeBatchRemovalPreservesTheInvariant() {
        transaction.executeWithoutResult(tx -> {
            UserEntity replacement = form(ordinaryId);
            replacement.setRoleEntities(new ArrayList<>(form(firstId).getRoleEntities()));
            service.save(replacement);
            change(form(firstId), "disable");
            change(form(secondId), "delete");
        });
        assertEquals(List.of(ordinaryId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    @Test
    void preflightAndMutationsUseStoredMembershipNotStaleSnapshotPrivileges() {
        service.deleteUser(form(secondId));
        UserEntity staleAdmin = form(firstId);
        staleAdmin.setEnabled(false);
        staleAdmin.setRoleEntities(List.of());
        assertFalse(service.canDeleteUser(staleAdmin));
        assertTrue(service.wouldRemoveLastEnabledAdmin(firstId, false, List.of()));
        assertThrows(IllegalStateException.class, () -> service.deleteUser(staleAdmin));
        assertThrows(IllegalStateException.class, () -> service.save(staleAdmin));
        UserEntity ordinary = form(ordinaryId);
        ordinary.setRoleEntities(form(firstId).getRoleEntities());
        assertTrue(service.canDeleteUser(ordinary));
        assertFalse(service.wouldRemoveLastEnabledAdmin(ordinaryId, false, List.of()));
        service.deleteUser(ordinary);
        assertEquals(List.of(firstId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    @Test
    void initiallyZeroAdminsDoNotBlockOrdinaryCreationOrUpdate() {
        // Legacy/initial state is constructed only in this isolated database.
        transaction.executeWithoutResult(tx -> repository.findAll().forEach(user -> user.setEnabled(false)));
        UserEntity ordinary = form(ordinaryId);
        assertFalse(service.wouldRemoveLastEnabledAdmin(ordinaryId, false, ordinary.getRoleEntities()));
        ordinary.setFirstName("Updated");
        service.updateAdminUser(ordinary, ordinary.getVersion(), "");
        UserEntity created = UserEntity.builder().email("new@example.com").firstName("New").lastName("User").password("hash")
                .gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH)
                .roleEntities(ordinary.getRoleEntities()).build();
        assertFalse(service.wouldRemoveLastEnabledAdmin(null, false, created.getRoleEntities()));
        service.saveUser(created);
        assertTrue(repository.findByEmail("new@example.com").isPresent());
        assertEquals("Updated", form(ordinaryId).getFirstName());
    }
}
