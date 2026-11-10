package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

/** Committed isolated H2 transactions; no application/session/database startup. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
class AdminSecurityPersistenceTest {
  @Autowired UserService userService;
  @Autowired UserRepository userRepository;
  @Autowired TransactionTemplate transactionTemplate;
  @PersistenceContext EntityManager entityManager;
  private Long firstId;
  private Long secondId;
  private Long ordinaryId;
  private Long adminId;

  @BeforeEach
  void seed() {
    transactionTemplate.executeWithoutResult(tx -> {
      userRepository.deleteAll();
      userRepository.flush();
      entityManager.createQuery("delete from RoleEntity").executeUpdate();
      RoleEntity admin = RoleEntity.builder().name(RoleType.ROLE_SUPERADMIN).build();
      RoleEntity ordinary = RoleEntity.builder().name(RoleType.ROLE_USER).build();
      entityManager.persist(admin);
      entityManager.persist(ordinary);
      RoleEntity adminRole = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
      entityManager.persist(adminRole);
      adminId = create("ordinary-admin@example.com", adminRole);
      firstId = create("first-admin@example.com", admin);
      secondId = create("second-admin@example.com", admin);
      ordinaryId = create("ordinary@example.com", ordinary);
    });
  }

  private void makeFirstAdmin() {
    transactionTemplate.executeWithoutResult(
        tx -> form(firstId).setRoleEntities(new ArrayList<>(form(adminId).getRoleEntities())));
  }

  private List<Long> administratorIds() {
    return userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
  }

  @ParameterizedTest
  @ValueSource(strings = {"disable", "role", "form", "delete", "mixed"})
  void parallelMixedAdminAndSuperadminMutationsProtectOnlyTheSuperadmin(String mutation)
      throws Exception {
    makeFirstAdmin();
    CyclicBarrier barrier = new CyclicBarrier(2);
    try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first =
          workers.submit(
              () -> compete(firstId, mutation.equals("mixed") ? "delete" : mutation, barrier));
      Future<Boolean> second =
          workers.submit(
              () -> compete(secondId, mutation.equals("mixed") ? "form" : mutation, barrier));

      assertTrue(first.get(10, TimeUnit.SECONDS), "Ordinary admin may be removed");
      assertFalse(second.get(10, TimeUnit.SECONDS), "Last superadmin must remain");
    }
    assertEquals(1, administratorIds().size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void lastSuperadminCannotBeRemovedIncludingWhenItAlsoHasAdmin(boolean keepAdminAssignment) {
    if (keepAdminAssignment) {
      transactionTemplate.executeWithoutResult(
          tx -> form(firstId).getRoleEntities().addAll(form(adminId).getRoleEntities()));
    }
    userService.deleteUser(form(secondId));

    assertEquals(List.of(firstId), administratorIds());
    assertFalse(userService.canDeleteUser(form(firstId)));
    assertTrue(
        userService.wouldRemoveLastEnabledSuperadmin(
            firstId, false, form(firstId).getRoleEntities()));
    assertTrue(
        userService.wouldRemoveLastEnabledSuperadmin(
            firstId, true, form(ordinaryId).getRoleEntities()));
    assertFalse(
        userService.wouldRemoveLastEnabledSuperadmin(
            firstId, true, form(firstId).getRoleEntities()));
    UserEntity userToDelete = form(firstId);

    assertThrows(IllegalStateException.class, () -> userService.deleteUser(userToDelete));
    UserEntity userToDisable = form(firstId);

    assertThrows(IllegalStateException.class, () -> change(userToDisable, "disable"));
    UserEntity userToDemote = form(firstId);

    assertThrows(IllegalStateException.class, () -> change(userToDemote, "role"));
    assertEquals(List.of(firstId), administratorIds());
  }

  @Test
  void soleSuperadminCannotBeDowngradedEvenIfItRetainsAdminAndAnotherAdminExists() {
    userService.deleteUser(form(secondId));
    UserEntity edited = form(firstId);
    edited.setRoleEntities(new ArrayList<>(form(adminId).getRoleEntities()));

    assertTrue(
        userService.wouldRemoveLastEnabledSuperadmin(firstId, true, edited.getRoleEntities()));
    Long editedVersion = edited.getVersion();

    assertThrows(
        IllegalStateException.class, () -> userService.updateAdminUser(edited, editedVersion, ""));
    assertTrue(form(firstId).hasRole(RoleType.ROLE_SUPERADMIN));
    assertTrue(form(adminId).hasRole(RoleType.ROLE_ADMIN));
  }

  @Test
  void disabledSuperadminDoesNotAllowRemovingTheLastActiveSuperadmin() {
    transactionTemplate.executeWithoutResult(tx -> form(secondId).setEnabled(false));

    assertFalse(userService.canDeleteUser(form(firstId)));
    assertTrue(userService.canDeleteUser(form(secondId)));
    UserEntity targetUser = form(firstId);

    assertThrows(IllegalStateException.class, () -> userService.deleteUser(targetUser));
    userService.deleteUser(form(secondId));

    assertEquals(List.of(firstId), administratorIds());
  }

  private Long create(String email, RoleEntity role) {
    UserEntity testUserEntity =
        UserEntity.builder()
            .email(email)
            .firstName("Test")
            .lastName("User")
            .password("hash")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender(Gender.OTHER)
            .enabled(true)
            .preferredLocale(Locale.ENGLISH)
            .roleEntities(new ArrayList<>(List.of(role)))
            .build();
    entityManager.persist(testUserEntity);
    return testUserEntity.getId();
  }

  private UserEntity form(Long id) {
    return userRepository.findAll().stream()
        .filter(user -> user.getId().equals(id))
        .findFirst()
        .orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"disable", "role", "form", "delete", "mixed"})
  void parallelMutationsCanNeverRemoveBothSuperadmins(String mutation) throws Exception {
    CyclicBarrier readBarrier = new CyclicBarrier(2);
    try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first =
          workers.submit(
              () -> compete(firstId, mutation.equals("mixed") ? "delete" : mutation, readBarrier));
      Future<Boolean> second =
          workers.submit(
              () -> compete(secondId, mutation.equals("mixed") ? "form" : mutation, readBarrier));
      boolean firstCommitted = first.get(10, TimeUnit.SECONDS);
      boolean secondCommitted = second.get(10, TimeUnit.SECONDS);

      assertNotEquals(
          firstCommitted,
          secondCommitted,
          "Exactly one removal must commit; the other must reject");
    }
    assertEquals(1, userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN).size());
    assertTrue(userRepository.existsById(ordinaryId));
  }

  private boolean compete(Long id, String mutation, CyclicBarrier barrier) {
    try {
      transactionTemplate.executeWithoutResult(tx -> {
        UserEntity candidate = form(id);
        // Preflight bleibt unverbindlich; die gesperrte Mutation prüft den aktuellen Bestand.
        assertEquals(
            !userService.canDeleteUser(candidate),
            userService.wouldRemoveLastEnabledSuperadmin(id, false, List.of()));
        try {
          barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
          throw new AssertionError(exception);
        }
        change(candidate, mutation);
      });
      return true;
    } catch (IllegalStateException rejection) {
      assertTrue(rejection.getMessage().contains("letzte aktive Superadmin"));
      return false;
    }
  }

  private void change(UserEntity testUserEntity, String mutation) {
    if (mutation.equals("delete")) {
      userService.deleteUser(testUserEntity);
    } else {
      if (mutation.equals("role")) {
        testUserEntity.getRoleEntities().clear();
      } else {
        testUserEntity.setEnabled(false);
      }
      if (mutation.equals("form")) {
        userService.updateAdminUser(testUserEntity, testUserEntity.getVersion(), "");
      } else if (mutation.equals("role")) {
        userService.saveUser(testUserEntity);
      } else {
        userService.save(testUserEntity);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"disable", "role", "form", "delete"})
  void twoSequentialRemovalsInOneTransactionRollBackTheWholeBatch(String mutation) {
    assertThrows(
        IllegalStateException.class,
        () ->
            transactionTemplate.executeWithoutResult(tx -> {
              change(form(firstId), mutation);

              assertFalse(userService.canDeleteUser(form(secondId)));
              assertTrue(userService.wouldRemoveLastEnabledSuperadmin(secondId, false, List.of()));
              change(form(secondId), mutation);
            }));
    assertEquals(
        List.of(firstId, secondId),
        userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void flushOfMultipleAlreadyDirtyUsersCannotBypassTheGuard(boolean submitOrdinaryUser) {
    assertThrows(
        IllegalStateException.class,
        () ->
            transactionTemplate.executeWithoutResult(tx -> {
              List<UserEntity> users = userRepository.findAll();
              users.stream()
                  .filter(user -> user.getId().equals(firstId) || user.getId().equals(secondId))
                  .forEach(user -> user.setEnabled(false));
              userService.save(
                  users.stream()
                      .filter(
                          user -> user.getId().equals(submitOrdinaryUser ? ordinaryId : firstId))
                      .findFirst()
                      .orElseThrow());
            }));
    assertEquals(2, userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN).size());
  }

  @Test
  void promotionBeforeBatchRemovalPreservesTheInvariant() {
    transactionTemplate.executeWithoutResult(tx -> {
      UserEntity replacement = form(ordinaryId);
      replacement.setRoleEntities(new ArrayList<>(form(firstId).getRoleEntities()));
      userService.save(replacement);
      change(form(firstId), "disable");
      change(form(secondId), "delete");
    });

    assertEquals(
        List.of(ordinaryId), userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void preflightAndMutationsUseStoredMembershipNotStaleSnapshotPrivileges() {
    userService.deleteUser(form(secondId));
    UserEntity staleAdmin = form(firstId);
    staleAdmin.setEnabled(false);
    staleAdmin.setRoleEntities(List.of());

    assertFalse(userService.canDeleteUser(staleAdmin));
    assertTrue(userService.wouldRemoveLastEnabledSuperadmin(firstId, false, List.of()));
    assertThrows(IllegalStateException.class, () -> userService.deleteUser(staleAdmin));
    assertThrows(IllegalStateException.class, () -> userService.save(staleAdmin));
    UserEntity ordinary = form(ordinaryId);
    ordinary.setRoleEntities(form(firstId).getRoleEntities());

    assertTrue(userService.canDeleteUser(ordinary));
    assertFalse(userService.wouldRemoveLastEnabledSuperadmin(ordinaryId, false, List.of()));
    userService.deleteUser(ordinary);

    assertEquals(
        List.of(firstId), userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void initiallyZeroAdminsDoNotBlockOrdinaryCreationOrUpdate() {
    // Der bewusst ungeschützte Anfangszustand gehört nur zu dieser isolierten Testdatenbank.
    transactionTemplate.executeWithoutResult(
        tx -> userRepository.findAll().forEach(user -> user.setEnabled(false)));
    UserEntity ordinary = form(ordinaryId);

    assertFalse(
        userService.wouldRemoveLastEnabledSuperadmin(
            ordinaryId, false, ordinary.getRoleEntities()));
    ordinary.setFirstName("Updated");
    userService.updateAdminUser(ordinary, ordinary.getVersion(), "");
    UserEntity created =
        UserEntity.builder()
            .email("new@example.com")
            .firstName("New")
            .lastName("User")
            .password("hash")
            .gender(Gender.OTHER)
            .birthDate(LocalDate.of(1990, 1, 1))
            .preferredLocale(Locale.ENGLISH)
            .roleEntities(ordinary.getRoleEntities())
            .build();

    assertFalse(
        userService.wouldRemoveLastEnabledSuperadmin(null, false, created.getRoleEntities()));
    userService.saveUser(created);

    assertTrue(userRepository.findByEmail("new@example.com").isPresent());
    assertEquals("Updated", form(ordinaryId).getFirstName());
  }
}
