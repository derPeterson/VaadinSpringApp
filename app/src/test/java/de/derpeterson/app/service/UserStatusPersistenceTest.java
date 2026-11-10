package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.scheduler.UserStatusScheduler;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserService.StatusUpdate;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** No application, OSIV, scheduler startup or mail: real service proxy and isolated JPA commits. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
class UserStatusPersistenceTest {
  @Autowired private UserService userService;
  @Autowired private SecurityService securityService;
  @Autowired private UserRepository userRepository;
  @Autowired private UserStatusBroadcaster userStatusBroadcaster;
  @Autowired private TransactionTemplate transactionTemplate;
  @PersistenceContext private EntityManager entityManager;

  private Long userId;
  private UI testUi;
  private VaadinSession vaadinSession;
  private final List<UserStatusBroadcaster.UserStatusMessage> messages = new ArrayList<>();
  private final List<Registration> registrations = new ArrayList<>();

  @BeforeEach
  void seed() {
    messages.clear();
    transactionTemplate.executeWithoutResult(status -> {
      userRepository.deleteAll();
      userRepository.flush();
      entityManager.createQuery("delete from RoleEntity").executeUpdate();
      RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
      entityManager.persist(role);
      UserEntity user =
          UserEntity.builder()
              .firstName("Test")
              .lastName("User")
              .email("status@example.com")
              .password("old-hash")
              .enabled(true)
              .gender(Gender.OTHER)
              .birthDate(LocalDate.of(1990, 1, 1))
              .preferredLocale(Locale.ENGLISH)
              .roleEntities(new ArrayList<>(List.of(role)))
              .status(UserStatus.OFFLINE)
              .lastActivity(Instant.now().minusSeconds(3600))
              .build();
      entityManager.persist(user);
      userId = user.getId();
    });
  }

  @AfterEach
  void cleanup() {
    registrations.forEach(Registration::remove);
    registrations.clear();
    if (UI.getCurrent() != null) {
      NotificationHelper.getInstance().closeAndClearAllNotifications();
    }
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    testUi = null;
    vaadinSession = null;
  }

  private void listen() {
    registrations.add(userStatusBroadcaster.register(messages::add));
  }

  private UserEntity stored() {
    return userRepository.findById(userId).orElseThrow();
  }

  private UserEntity form() {
    return userRepository.findAll().getFirst();
  }

  private TransactionTemplate independent() {
    TransactionTemplate separate =
        new TransactionTemplate(transactionTemplate.getTransactionManager());
    separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return separate;
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void overlappingActivityAndLoginWaitForProfileCommitWithoutLosingFields(boolean login)
      throws Exception {
    try (ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
      AtomicReference<Future<?>> future =
          new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>>();
      transactionTemplate.executeWithoutResult(tx -> {
        UserEntity profile = userRepository.findByIdForUpdate(userId).orElseThrow();
        profile.setFirstName("Concurrent profile");
        profile.setPassword("concurrent-security-hash");
        profile.setEnabled(false);
        profile.setManualStatus(UserStatus.EMPLOYED);
        userRepository.flush();
        future.set(
            worker.submit(() -> {
              entered.countDown();
              if (login) {
                securityService.handleLogin(
                    org.springframework.security.core.userdetails.User.withUsername(
                            "status@example.com")
                        .password("hash")
                        .roles("USER")
                        .build());
              } else {
                userService.updateLastActivity(userId);
              }
            }));
        assertTrue(await(entered));
        assertThrows(
            java.util.concurrent.TimeoutException.class,
            () -> future.get().get(150, java.util.concurrent.TimeUnit.MILLISECONDS));
      });
      future.get().get(5, java.util.concurrent.TimeUnit.SECONDS);
    }
    assertEquals("Concurrent profile", stored().getFirstName());
    assertEquals("concurrent-security-hash", stored().getPassword());
    assertFalse(stored().isEnabled());
    assertEquals(UserStatus.EMPLOYED, stored().getStatus());
    assertTrue(stored().isStatusManuallySet());
    assertEquals(1L, stored().getVersion());
    assertTrue(stored().getLastActivity().isAfter(Instant.now().minusSeconds(60)));
    assertTrue(form().hasRole(RoleType.ROLE_USER));
  }

  private boolean await(java.util.concurrent.CountDownLatch latch) {
    try {
      return latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void realCandidateLockFailureDoesNotRollBackOrPreventTheNextCandidate(boolean toAbsent)
      throws Exception {
    UserStatus initial = toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT;
    UserStatus target = toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE;
    userService.updateUserStatus(stored(), initial, false);
    Instant activity = toAbsent ? Instant.now().minusSeconds(3600) : Instant.now();
    Long otherId =
        transactionTemplate.execute(tx -> {
          UserEntity first = stored();
          first.setLastActivity(activity);
          UserEntity other =
              UserEntity.builder()
                  .firstName("Other")
                  .lastName("User")
                  .email("other-status@example.com")
                  .password("other-hash")
                  .enabled(true)
                  .gender(Gender.OTHER)
                  .birthDate(LocalDate.of(1990, 1, 1))
                  .preferredLocale(Locale.ENGLISH)
                  .roleEntities(new ArrayList<>(first.getRoleEntities()))
                  .status(initial)
                  .lastActivity(activity)
                  .build();
          entityManager.persist(other);
          return other.getId();
        });
    List<UserEntity> candidates = List.of(stored(), userRepository.findById(otherId).orElseThrow());
    UserRepository selection = mock(UserRepository.class);
    ConfigService config = mock(ConfigService.class);
    if (toAbsent) {
      when(config.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
      when(selection.findByLastActivityBeforeAndStatus(any(), eq(initial))).thenReturn(candidates);
    } else {
      when(selection.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(any(), eq(initial)))
          .thenReturn(candidates);
    }
    UserStatusScheduler scheduler = new UserStatusScheduler(selection, userService, config);
    listen();
    try (ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      transactionTemplate.executeWithoutResult(tx -> {
        userRepository.findByIdForUpdate(userId).orElseThrow();
        Future<?> future =
            worker.submit(() -> {
              if (toAbsent) {
                scheduler.checkInactiveAvailableUsers();
              } else {
                scheduler.checkRecentlyActiveAbsentUsers();
              }
            });
        // Keep the first row locked until H2's real lock timeout has been
        // translated and the scheduler has committed its second candidate.
        assertDoesNotThrow(() -> future.get(10, java.util.concurrent.TimeUnit.SECONDS));
      });
    }
    assertEquals(initial, stored().getStatus());
    assertEquals(target, userRepository.findById(otherId).orElseThrow().getStatus());
    assertEquals(
        List.of(
            new UserStatusBroadcaster.UserStatusMessage(otherId, initial.name(), target.name())),
        messages);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scheduledMutationWaitsForActivityCommitAndThenRechecks(boolean toAbsent) throws Exception {
    userService.updateUserStatus(
        stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, false);
    Instant cutoff = Instant.now().minusSeconds(300);
    transactionTemplate.executeWithoutResult(tx ->
        stored().setLastActivity(toAbsent ? cutoff.minusSeconds(60) : cutoff.plusSeconds(60)));
    List<UserEntity> candidates =
        toAbsent
            ? userRepository.findByLastActivityBeforeAndStatus(cutoff, UserStatus.AVAILABLE)
            : userRepository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
                cutoff, UserStatus.ABSENT);
    assertEquals(1, candidates.size());
    listen();
    try (ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
      AtomicReference<Future<?>> future =
          new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>>();
      transactionTemplate.executeWithoutResult(tx -> {
        UserEntity active = userRepository.findByIdForUpdate(userId).orElseThrow();
        active.setLastActivity(toAbsent ? cutoff.plusSeconds(60) : cutoff.minusSeconds(60));
        userRepository.flush();
        future.set(
            worker.submit(() -> {
              entered.countDown();
              userService.updateScheduledStatus(
                  candidates.getFirst().getId(),
                  toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE,
                  cutoff);
            }));
        assertTrue(await(entered));
        assertThrows(
            java.util.concurrent.TimeoutException.class,
            () -> future.get().get(150, java.util.concurrent.TimeUnit.MILLISECONDS));
      });
      future.get().get(5, java.util.concurrent.TimeUnit.SECONDS);
    }
    assertEquals(toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, stored().getStatus());
    assertTrue(messages.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void activityFromAnAlreadyLoadedContextSurvivesConcurrentVersionedChanges(boolean statusChange) {
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity stale = userRepository.findById(userId).orElseThrow();
      Long version = stale.getVersion();
      independent()
          .executeWithoutResult(other -> {
            UserEntity fresh = userRepository.findById(userId).orElseThrow();
            if (statusChange) {
              userService.updateUserStatus(fresh, UserStatus.EMPLOYED, true);
            } else {
              fresh.setFirstName("Latest profile");
              fresh.setPassword("latest-security-hash");
              fresh.setEnabled(false);
              fresh.setPreferredLocale(Locale.GERMAN);
            }
          });
      assertEquals(version, stale.getVersion());
      assertDoesNotThrow(() -> userService.updateLastActivity(userId));
    });
    UserEntity fresh = stored();
    assertTrue(fresh.getLastActivity().isAfter(Instant.now().minusSeconds(60)));
    assertEquals(1L, fresh.getVersion());
    assertEquals(statusChange ? UserStatus.EMPLOYED : UserStatus.OFFLINE, fresh.getStatus());
    assertEquals(statusChange ? "Test" : "Latest profile", fresh.getFirstName());
    assertEquals(statusChange ? "old-hash" : "latest-security-hash", fresh.getPassword());
    assertEquals(statusChange, fresh.isEnabled());
    assertEquals(statusChange ? Locale.ENGLISH : Locale.GERMAN, fresh.getPreferredLocale());
    assertTrue(form().hasRole(RoleType.ROLE_USER));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void concurrentActivityIsNotOverwrittenByAnAlreadyLoadedStatusOrProfileEditor(
      boolean statusChange) {
    AtomicReference<Instant> activity = new java.util.concurrent.atomic.AtomicReference<Instant>();
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity editor = userRepository.findById(userId).orElseThrow();
      independent().executeWithoutResult(other -> userService.updateLastActivity(userId));
      activity.set(independent().execute(other -> stored().getLastActivity()));
      if (statusChange) {
        userService.updateUserStatus(editor, UserStatus.ABSENT, false);
      } else {
        editor.setLastName("Newest");
        editor.setPassword("new-password");
        editor.setEnabled(false);
      }
    });
    assertEquals(activity.get(), stored().getLastActivity());
    assertEquals(statusChange ? UserStatus.ABSENT : UserStatus.OFFLINE, stored().getStatus());
    assertEquals(statusChange ? "User" : "Newest", stored().getLastName());
    assertEquals(statusChange ? "old-hash" : "new-password", stored().getPassword());
    assertEquals(statusChange, stored().isEnabled());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void schedulerRechecksActivityChangedAfterSelectionInBothDirections(boolean toAbsent) {
    userService.updateUserStatus(
        stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, false);
    if (!toAbsent) {
      userService.updateLastActivity(userId);
    }
    UserRepository selection = mock(UserRepository.class);
    if (toAbsent) {
      when(selection.findByLastActivityBeforeAndStatus(any(), eq(UserStatus.AVAILABLE)))
          .thenAnswer(call -> {
            List<UserEntity> candidates =
                userRepository.findByLastActivityBeforeAndStatus(
                    call.getArgument(0), UserStatus.AVAILABLE);
            assertEquals(1, candidates.size());
            userService.updateLastActivity(userId);
            return candidates;
          });
    } else {
      when(selection.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
              any(), eq(UserStatus.ABSENT)))
          .thenAnswer(call -> {
            List<UserEntity> candidates =
                userRepository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
                    call.getArgument(0), UserStatus.ABSENT);
            assertEquals(1, candidates.size());
            transactionTemplate.executeWithoutResult(
                tx -> stored().setLastActivity(Instant.now().minusSeconds(600)));
            return candidates;
          });
    }
    ConfigService config = mock(ConfigService.class);
    if (toAbsent) {
      when(config.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
    }
    UserStatusScheduler scheduler = new UserStatusScheduler(selection, userService, config);
    listen();
    if (toAbsent) {
      scheduler.checkInactiveAvailableUsers();
    } else {
      scheduler.checkRecentlyActiveAbsentUsers();
    }
    assertEquals(toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, stored().getStatus());
    assertTrue(messages.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(UserStatus.class)
  void schedulerRechecksInterveningManualStatusInBothDirections(UserStatus manual) {
    for (boolean toAbsent : new boolean[] {true, false}) {
      userService.updateUserStatus(
          stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, true);
      userService.updateUserStatus(
          stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, false);
      Instant cutoff = Instant.now().minusSeconds(300);
      transactionTemplate.executeWithoutResult(tx ->
          stored().setLastActivity(toAbsent ? cutoff.minusSeconds(60) : cutoff.plusSeconds(60)));
      List<UserEntity> candidates =
          toAbsent
              ? userRepository.findByLastActivityBeforeAndStatus(cutoff, UserStatus.AVAILABLE)
              : userRepository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
                  cutoff, UserStatus.ABSENT);
      assertEquals(1, candidates.size());
      userService.updateUserStatus(stored(), manual, true);
      messages.clear();
      listen();
      userService.updateScheduledStatus(
          candidates.getFirst().getId(),
          toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE,
          cutoff);
      boolean eligible = toAbsent && manual == UserStatus.AVAILABLE;
      assertEquals(eligible ? UserStatus.ABSENT : manual, stored().getStatus());
      assertEquals(!eligible, stored().isStatusManuallySet());
      assertEquals(eligible ? 1 : 0, messages.size());
      registrations.forEach(Registration::remove);
      registrations.clear();
    }
  }

  @Test
  void deletedUserHasExplicitStatusActivityAndFormContracts() {
    UserEntity snapshot = form();
    userRepository.deleteById(userId);
    listen();
    assertThrows(
        IllegalStateException.class,
        () -> userService.updateUserStatus(snapshot, UserStatus.AVAILABLE, true));
    assertThrows(IllegalStateException.class, () -> userService.updateLastActivity(userId));
    Long snapshotVersion = snapshot.getVersion();
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> userService.updateAdminUser(snapshot, snapshotVersion, "new"));
    assertDoesNotThrow(
        () -> userService.updateScheduledStatus(userId, UserStatus.ABSENT, Instant.now()));
    assertFalse(userRepository.existsById(userId));
    assertTrue(messages.isEmpty());
  }

  @Test
  void missingFormVersionIsRejectedWithoutChangingAnyField() {
    UserEntity edited = form();
    edited.setFirstName("Rejected");
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> userService.updateAdminUser(edited, null, "new"));
    assertEquals("Test", stored().getFirstName());
    assertEquals("old-hash", stored().getPassword());
    assertEquals(edited.getVersion(), stored().getVersion());
  }

  @Test
  void explicitAdminPasswordPreservesUnrelatedStatusLocaleAndActivity() {
    userService.updateUserStatus(stored(), UserStatus.EMPLOYED, true);
    transactionTemplate.executeWithoutResult(tx -> stored().setPreferredLocale(Locale.GERMAN));
    UserEntity edited = form();
    userService.updateLastActivity(userId);
    Instant activity = stored().getLastActivity();
    // These snapshot fields are not admin-editable and must not be merged.
    edited.setStatus(UserStatus.OFFLINE);
    edited.setStatusManuallySet(false);
    edited.setPreferredLocale(Locale.ENGLISH);
    edited.setLastActivity(Instant.EPOCH);
    edited.setFirstName("Admin edited");
    userService.updateAdminUser(edited, edited.getVersion(), "Explicit-new-password!");
    UserEntity fresh = stored();
    assertTrue(
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            .matches("Explicit-new-password!", fresh.getPassword()));
    assertEquals("Admin edited", fresh.getFirstName());
    assertEquals(UserStatus.EMPLOYED, fresh.getStatus());
    assertTrue(fresh.isStatusManuallySet());
    assertEquals(Locale.GERMAN, fresh.getPreferredLocale());
    assertEquals(activity, fresh.getLastActivity());
    assertTrue(fresh.isEnabled());
    assertTrue(form().hasRole(RoleType.ROLE_USER));
  }

  @Test
  void staleStatusSnapshotCannotUndoLockPasswordRolesOrProfileChanges() {
    UserEntity stale = stored();
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    assertFalse(Hibernate.isInitialized(stale.getRoleEntities()));
    Instant activity =
        Instant.now().minusSeconds(120).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity user = userRepository.findById(userId).orElseThrow();
      user.setEnabled(false);
      user.setPassword("new-hash");
      user.setPreferredLocale(Locale.GERMAN);
      user.setFirstName("Changed");
      user.setLastActivity(activity);
      user.getRoleEntities().clear();
    });
    listen();

    StatusUpdate result = userService.updateUserStatus(stale, UserStatus.AVAILABLE, true);

    UserEntity current = stored();
    assertFalse(current.isEnabled());
    assertEquals("new-hash", current.getPassword());
    assertEquals(Locale.GERMAN, current.getPreferredLocale());
    assertEquals("Changed", current.getFirstName());
    assertEquals(activity, current.getLastActivity());
    assertTrue(form().getRoleEntities().isEmpty());
    assertEquals(new UserService.StatusUpdate(UserStatus.AVAILABLE, true), result);
    assertEquals(UserStatus.OFFLINE, stale.getStatus());
    assertEquals(
        List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "AVAILABLE")),
        messages);
  }

  @Test
  void staleAdminFormCannotUndoAnotherFormOrPasswordChange() {
    UserEntity first = form();
    UserEntity stale = form();
    first.setFirstName("First editor");
    userService.updateAdminUser(first, first.getVersion(), "");
    stale.setLastName("Second editor");

    Long staleVersion = stale.getVersion();
    OptimisticLockingFailureException conflict =
        assertThrows(
            OptimisticLockingFailureException.class,
            () -> userService.updateAdminUser(stale, staleVersion, ""));
    assertTrue(conflict.getMessage().contains("Formular neu öffnen"));
    assertEquals("First editor", stored().getFirstName());
    assertEquals("User", stored().getLastName());

    UserEntity beforePasswordChange = form();
    transactionTemplate.executeWithoutResult(
        status -> userRepository.findById(userId).orElseThrow().setPassword("reset-hash"));
    Long beforePasswordChangeVersion = beforePasswordChange.getVersion();
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> userService.updateAdminUser(beforePasswordChange, beforePasswordChangeVersion, ""));
    assertEquals("reset-hash", stored().getPassword());
  }

  @Test
  void activityDoesNotInvalidateAdminFormAndAdminSavePreservesActivity() {
    UserEntity edited = form();
    Long version = edited.getVersion();
    for (int activityUpdateIndex = 0; activityUpdateIndex < 3; activityUpdateIndex++) {
      userService.updateLastActivity(userId);
    }
    Instant activity = stored().getLastActivity();
    assertEquals(version, stored().getVersion());
    assertTrue(activity.isAfter(edited.getLastActivity()));
    edited.setFirstName("Edited");
    userService.updateAdminUser(edited, version, "");
    assertEquals(activity, stored().getLastActivity());
    assertEquals("Edited", stored().getFirstName());
  }

  @Test
  void statusChangeInvalidatesAdminFormButRepeatedNoOpDoesNotAdvanceVersion() {
    UserEntity edited = form();
    userService.updateUserStatus(edited, UserStatus.AVAILABLE, true);
    Long version = stored().getVersion();
    userService.updateUserStatus(edited, UserStatus.AVAILABLE, true);
    assertEquals(version, stored().getVersion());
    Long editedVersion = edited.getVersion();
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> userService.updateAdminUser(edited, editedVersion, ""));
    assertEquals(UserStatus.AVAILABLE, stored().getStatus());
  }

  @Test
  void genericSaveAlsoRejectsStaleDetachedEntities() {
    UserEntity stale = form();
    transactionTemplate.executeWithoutResult(
        status -> userRepository.findById(userId).orElseThrow().setPassword("reset-hash"));
    stale.setFirstName("Stale");
    assertThrows(OptimisticLockingFailureException.class, () -> userService.saveUser(stale));
    assertEquals("reset-hash", stored().getPassword());
    assertEquals("Test", stored().getFirstName());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void adminFormUpdateRetainsLastSuperadminProtection(boolean disable) {
    transactionTemplate.executeWithoutResult(status -> {
      RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_SUPERADMIN).build();
      entityManager.persist(role);
      userRepository.findById(userId).orElseThrow().setRoleEntities(new ArrayList<>(List.of(role)));
    });
    UserEntity edited = form();
    if (disable) {
      edited.setEnabled(false);
    } else {
      edited.setRoleEntities(List.of());
    }
    Long editedVersion = edited.getVersion();
    assertThrows(
        IllegalStateException.class, () -> userService.updateAdminUser(edited, editedVersion, ""));
    assertTrue(stored().isEnabled());
    assertTrue(form().hasRole(RoleType.ROLE_SUPERADMIN));
    userService.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
    assertTrue(form().hasRole(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void detachedListingAndAdminHelpersHaveAnExplicitRolesLoadContract() {
    List<UserEntity> users = userService.findAllUsers();
    assertTrue(Hibernate.isInitialized(users.getFirst().getRoleEntities()));
    assertTrue(users.getFirst().hasRole(RoleType.ROLE_USER));
    assertTrue(userService.canDeleteUser(users.getFirst()));
    assertFalse(
        userService.wouldRemoveLastEnabledSuperadmin(
            userId, false, users.getFirst().getRoleEntities()));
  }

  @ParameterizedTest
  @EnumSource(UserStatus.class)
  void automaticPriorityUsesStoredStateNotTheDetachedSnapshot(UserStatus manualStatus) {
    UserEntity stale = stored();
    userService.updateUserStatus(stale, manualStatus, true);
    listen();
    StatusUpdate result = userService.updateUserStatus(stale, UserStatus.ABSENT, false);
    boolean protectedStatus =
        manualStatus == UserStatus.EMPLOYED || manualStatus == UserStatus.OFFLINE;
    assertEquals(protectedStatus ? manualStatus : UserStatus.ABSENT, result.status());
    assertEquals(protectedStatus, result.manuallySet());
    assertEquals(result.status(), stored().getStatus());
    assertEquals(protectedStatus || manualStatus == UserStatus.ABSENT ? 0 : 1, messages.size());
  }

  @Test
  void schedulerRunsBothDirectionsWithoutOsivOrInitializedRoles() {
    ConfigService config = mock(ConfigService.class);
    when(config.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
    UserStatusScheduler scheduler = new UserStatusScheduler(userRepository, userService, config);
    userService.updateUserStatus(stored(), UserStatus.AVAILABLE, false);
    UserEntity candidate =
        userRepository
            .findByLastActivityBeforeAndStatus(Instant.now(), UserStatus.AVAILABLE)
            .getFirst();
    assertFalse(Hibernate.isInitialized(candidate.getRoleEntities()));
    listen();
    scheduler.checkInactiveAvailableUsers();
    assertEquals(UserStatus.ABSENT, stored().getStatus());
    userService.updateLastActivity(userId);
    scheduler.checkRecentlyActiveAbsentUsers();
    assertEquals(UserStatus.AVAILABLE, stored().getStatus());
    assertEquals(2, messages.size());
    assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
  }

  @Test
  void eventWaitsForOuterCommitAndContainsStoredOldStatus() {
    UserEntity stale = stored();
    stale.setStatus(
        UserStatus.EMPLOYED); // Even the former popover ordering cannot corrupt the old status.
    listen();
    transactionTemplate.executeWithoutResult(status -> {
      userService.updateUserStatus(stale, UserStatus.AVAILABLE, true);
      userRepository.flush();
      assertTrue(messages.isEmpty());
    });
    assertEquals(
        List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "AVAILABLE")),
        messages);
    assertEquals(UserStatus.AVAILABLE, stored().getStatus());
  }

  @Test
  void rollbackSendsNothingEvenAfterFlush() {
    listen();
    transactionTemplate.executeWithoutResult(status -> {
      userService.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
      userRepository.flush();
      status.setRollbackOnly();
    });
    assertTrue(messages.isEmpty());
    assertEquals(UserStatus.OFFLINE, stored().getStatus());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void repeatedChangesInOneTransactionOnlySendTheCommittedNetChange(boolean returnToOriginal) {
    listen();
    transactionTemplate.executeWithoutResult(status -> {
      userService.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
      userRepository.flush();
      userService.updateUserStatus(
          stored(), returnToOriginal ? UserStatus.OFFLINE : UserStatus.EMPLOYED, true);
      assertTrue(messages.isEmpty());
    });
    assertEquals(returnToOriginal ? UserStatus.OFFLINE : UserStatus.EMPLOYED, stored().getStatus());
    assertEquals(
        returnToOriginal
            ? List.of()
            : List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "EMPLOYED")),
        messages);
  }

  @Test
  void realOptimisticCommitFailureSendsNothingAndPreservesConcurrentPasswordChange() {
    listen();
    TransactionTemplate independent =
        new TransactionTemplate(transactionTemplate.getTransactionManager());
    independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThrows(
        OptimisticLockingFailureException.class,
        () ->
            transactionTemplate.executeWithoutResult(status -> {
              userService.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
              independent.executeWithoutResult(other ->
                  userRepository.findById(userId).orElseThrow().setPassword("concurrent-hash"));
              assertTrue(messages.isEmpty());
              // No explicit flush: the real JpaTransactionManager commit must fail.
            }));
    assertTrue(messages.isEmpty());
    assertEquals("concurrent-hash", stored().getPassword());
    assertEquals(UserStatus.OFFLINE, stored().getStatus());
  }

  @Test
  void unchangedStatusCanChangeManualFlagWithoutAnEvent() {
    listen();
    userService.updateUserStatus(stored(), UserStatus.OFFLINE, true);
    assertTrue(stored().isStatusManuallySet());
    assertTrue(messages.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void loginPreservesManualPriorityAndUsesCommitBoundStatusUpdates(boolean manual) {
    if (manual) {
      userService.updateUserStatus(stored(), UserStatus.EMPLOYED, true);
    }
    Instant previousActivity = stored().getLastActivity();
    listen();
    securityService.handleLogin(
        org.springframework.security.core.userdetails.User.withUsername("status@example.com")
            .password("hash")
            .roles("USER")
            .build());
    assertEquals(manual ? UserStatus.EMPLOYED : UserStatus.AVAILABLE, stored().getStatus());
    assertTrue(stored().getLastActivity().isAfter(previousActivity));
    assertEquals(manual ? 0 : 1, messages.size());
  }

  @Test
  void faultyFirstListenerDoesNotBlockHealthyListenerOrSuccessfulPersistence() {
    registrations.add(
        userStatusBroadcaster.register(message -> {
          throw new IllegalStateException("broken listener");
        }));
    listen();
    assertDoesNotThrow(() -> userService.updateUserStatus(stored(), UserStatus.AVAILABLE, true));
    assertEquals(UserStatus.AVAILABLE, stored().getStatus());
    assertEquals(1, messages.size());
  }

  private UserPopoverMenu popover(
      UserService statusService, UserEntity snapshot, List<UserStatus> icons) {
    testUi = new UI();
    UI.setCurrent(testUi);
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenReturn(Locale.ENGLISH);
    when(vaadinSession.hasLock()).thenReturn(true);
    VaadinSession.setCurrent(vaadinSession);
    MessageProperties messageProperties =
        mock(
            MessageProperties.class,
            invocation ->
                invocation.getMethod().getReturnType() == String.class
                    ? "Text"
                    : RETURNS_DEFAULTS.answer(invocation));
    UserPopoverMenu menu =
        new UserPopoverMenu(
            messageProperties,
            mock(SecurityService.class),
            statusService,
            snapshot,
            new Button(),
            new UserPopoverMenu.UserPopoverActions(null, null),
            icons::add);
    icons.clear();
    return menu;
  }

  @Test
  void actualPopoverCommitsBeforeUpdatingSnapshotAndIcons() {
    UserEntity snapshot = stored();
    List<UserStatus> icons = new ArrayList<>();
    UserPopoverMenu menu = popover(userService, snapshot, icons);
    registrations.add(
        userStatusBroadcaster.register(message -> {
          assertEquals(UserStatus.OFFLINE, snapshot.getStatus());
          assertTrue(icons.isEmpty());
          messages.add(message);
        }));

    ReflectionTestUtils.invokeMethod(menu, "handleUserStatusChange", UserStatus.EMPLOYED);

    assertEquals(UserStatus.EMPLOYED, stored().getStatus());
    assertEquals(UserStatus.EMPLOYED, snapshot.getStatus());
    assertTrue(snapshot.isStatusManuallySet());
    assertFalse(icons.isEmpty());
    assertTrue(icons.stream().allMatch(value -> value == UserStatus.EMPLOYED));
    assertEquals(
        List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "EMPLOYED")),
        messages);
  }

  @Test
  void failedPopoverUpdateKeepsSnapshotAndIconsAtThePreviousStatus() {
    UserEntity snapshot = stored();
    UserService failing = mock(UserService.class);
    List<UserStatus> icons = new ArrayList<>();
    UserPopoverMenu menu = popover(failing, snapshot, icons);
    when(failing.updateUserStatus(snapshot, UserStatus.EMPLOYED, true))
        .thenAnswer(invocation -> {
          assertEquals(UserStatus.OFFLINE, snapshot.getStatus());
          assertTrue(icons.isEmpty());
          throw new OptimisticLockingFailureException("conflict");
        });

    assertDoesNotThrow(() ->
        ReflectionTestUtils.invokeMethod(menu, "handleUserStatusChange", UserStatus.EMPLOYED));
    NotificationHelper helper = NotificationHelper.getInstance();
    Notification notification =
        (com.vaadin.flow.component.notification.Notification)
            ReflectionTestUtils.getField(helper, "currentNotification");
    assertTrue(notification.isOpened());
    Span title =
        (com.vaadin.flow.component.html.Span) ReflectionTestUtils.getField(helper, "titleText");
    Html message =
        (com.vaadin.flow.component.Html) ReflectionTestUtils.getField(helper, "messageText");
    assertEquals("Text", title.getText());
    assertEquals("p", message.getElement().getTag());
    assertEquals("Text", message.getElement().getProperty("innerHTML"));
    assertEquals(UserStatus.OFFLINE, snapshot.getStatus());
    assertTrue(icons.stream().allMatch(value -> value == UserStatus.OFFLINE));
    verify(failing, times(1)).updateUserStatus(snapshot, UserStatus.EMPLOYED, true);
  }
}
