package de.derpeterson.app.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.scheduler.UserStatusScheduler;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.ui.components.UserPopoverMenu;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** No application, OSIV, scheduler startup or mail: real service proxy and isolated JPA commits. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
class UserStatusPersistenceTest {
    @Autowired
    private UserService service;
    @Autowired
    private SecurityService security;
    @Autowired
    private UserRepository repository;
    @Autowired
    private UserStatusBroadcaster broadcaster;
    @Autowired
    private TransactionTemplate transaction;
    @PersistenceContext
    private EntityManager entityManager;

    private Long userId;
    private UI ui;
    private VaadinSession session;
    private final List<UserStatusBroadcaster.UserStatusMessage> messages = new ArrayList<>();
    private final List<Registration> registrations = new ArrayList<>();

    @BeforeEach
    void seed() {
        messages.clear();
        transaction.executeWithoutResult(status -> {
            repository.deleteAll();
            repository.flush();
            entityManager.createQuery("delete from RoleEntity").executeUpdate();
            RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
            entityManager.persist(role);
            UserEntity user = UserEntity.builder().firstName("Test").lastName("User")
                    .email("status@example.com").password("old-hash").enabled(true)
                    .gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1))
                    .preferredLocale(Locale.ENGLISH).roleEntities(new ArrayList<>(List.of(role)))
                    .status(UserStatus.OFFLINE).lastActivity(LocalDateTime.now().minusHours(1)).build();
            entityManager.persist(user);
            userId = user.getId();
        });
    }

    @AfterEach
    void cleanup() {
        registrations.forEach(Registration::remove);
        registrations.clear();
        NotificationHelper.getInstance().closeAndClearAllNotifications();
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        ui = null;
        session = null;
    }

    private void listen() {
        registrations.add(broadcaster.register(messages::add));
    }

    private UserEntity stored() {
        return repository.findById(userId).orElseThrow();
    }

    private UserEntity form() {
        return repository.findAll().getFirst();
    }

    private TransactionTemplate independent() {
        var separate = new TransactionTemplate(transaction.getTransactionManager());
        separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return separate;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void overlappingActivityAndLoginWaitForProfileCommitWithoutLosingFields(boolean login) throws Exception {
        try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var entered = new java.util.concurrent.CountDownLatch(1);
            var future = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>>();
            transaction.executeWithoutResult(tx -> {
                UserEntity profile = repository.findByIdForUpdate(userId).orElseThrow();
                profile.setFirstName("Concurrent profile");
                profile.setPassword("concurrent-security-hash");
                profile.setEnabled(false);
                profile.setManualStatus(UserStatus.EMPLOYED);
                repository.flush();
                future.set(worker.submit(() -> {
                    entered.countDown();
                    if (login) {
                        security.handleLogin(org.springframework.security.core.userdetails.User.withUsername("status@example.com")
                                .password("hash").roles("USER").build());
                    } else {
                        service.updateLastActivity(userId);
                    }
                }));
                assertTrue(await(entered));
                assertThrows(java.util.concurrent.TimeoutException.class, () -> future.get().get(150, java.util.concurrent.TimeUnit.MILLISECONDS));
            });
            future.get().get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals("Concurrent profile", stored().getFirstName());
        assertEquals("concurrent-security-hash", stored().getPassword());
        assertFalse(stored().isEnabled());
        assertEquals(UserStatus.EMPLOYED, stored().getStatus());
        assertTrue(stored().isStatusManuallySet());
        assertEquals(1L, stored().getVersion());
        assertTrue(stored().getLastActivity().isAfter(LocalDateTime.now().minusMinutes(1)));
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
    void realCandidateLockFailureDoesNotRollBackOrPreventTheNextCandidate(boolean toAbsent) throws Exception {
        UserStatus initial = toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT;
        UserStatus target = toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE;
        service.updateUserStatus(stored(), initial, false);
        LocalDateTime activity = toAbsent ? LocalDateTime.now().minusHours(1) : LocalDateTime.now();
        Long otherId = transaction.execute(tx -> {
            UserEntity first = stored();
            first.setLastActivity(activity);
            UserEntity other = UserEntity.builder().firstName("Other").lastName("User").email("other-status@example.com")
                    .password("other-hash").enabled(true).gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1))
                    .preferredLocale(Locale.ENGLISH).roleEntities(new ArrayList<>(first.getRoleEntities()))
                    .status(initial).lastActivity(activity).build();
            entityManager.persist(other);
            return other.getId();
        });
        List<UserEntity> candidates = List.of(stored(), repository.findById(otherId).orElseThrow());
        var selection = mock(UserRepository.class);
        var config = mock(ConfigService.class);
        if (toAbsent) {
            when(config.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
            when(selection.findByLastActivityBeforeAndStatus(any(), eq(initial))).thenReturn(candidates);
        } else {
            when(selection.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(any(), eq(initial))).thenReturn(candidates);
        }
        var scheduler = new UserStatusScheduler(selection, service, config);
        listen();
        try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            transaction.executeWithoutResult(tx -> {
                repository.findByIdForUpdate(userId).orElseThrow();
                var future = worker.submit(() -> {
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
        assertEquals(target, repository.findById(otherId).orElseThrow().getStatus());
        assertEquals(List.of(new UserStatusBroadcaster.UserStatusMessage(otherId, initial.name(), target.name())), messages);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void scheduledMutationWaitsForActivityCommitAndThenRechecks(boolean toAbsent) throws Exception {
        service.updateUserStatus(stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, false);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        transaction.executeWithoutResult(tx -> stored().setLastActivity(toAbsent ? cutoff.minusMinutes(1) : cutoff.plusMinutes(1)));
        var candidates = toAbsent
                ? repository.findByLastActivityBeforeAndStatus(cutoff, UserStatus.AVAILABLE)
                : repository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(cutoff, UserStatus.ABSENT);
        assertEquals(1, candidates.size());
        listen();
        try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var entered = new java.util.concurrent.CountDownLatch(1);
            var future = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>>();
            transaction.executeWithoutResult(tx -> {
                UserEntity active = repository.findByIdForUpdate(userId).orElseThrow();
                active.setLastActivity(toAbsent ? cutoff.plusMinutes(1) : cutoff.minusMinutes(1));
                repository.flush();
                future.set(worker.submit(() -> {
                    entered.countDown();
                    service.updateScheduledStatus(candidates.getFirst().getId(), toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE, cutoff);
                }));
                assertTrue(await(entered));
                assertThrows(java.util.concurrent.TimeoutException.class, () -> future.get().get(150, java.util.concurrent.TimeUnit.MILLISECONDS));
            });
            future.get().get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, stored().getStatus());
        assertTrue(messages.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void activityFromAnAlreadyLoadedContextSurvivesConcurrentVersionedChanges(boolean statusChange) {
        transaction.executeWithoutResult(status -> {
            UserEntity stale = repository.findById(userId).orElseThrow();
            Long version = stale.getVersion();
            independent().executeWithoutResult(other -> {
                UserEntity fresh = repository.findById(userId).orElseThrow();
                if (statusChange) {
                    service.updateUserStatus(fresh, UserStatus.EMPLOYED, true);
                } else {
                    fresh.setFirstName("Latest profile");
                    fresh.setPassword("latest-security-hash");
                    fresh.setEnabled(false);
                    fresh.setPreferredLocale(Locale.GERMAN);
                }
            });
            assertEquals(version, stale.getVersion());
            assertDoesNotThrow(() -> service.updateLastActivity(userId));
        });
        UserEntity fresh = stored();
        assertTrue(fresh.getLastActivity().isAfter(LocalDateTime.now().minusMinutes(1)));
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
    void concurrentActivityIsNotOverwrittenByAnAlreadyLoadedStatusOrProfileEditor(boolean statusChange) {
        var activity = new java.util.concurrent.atomic.AtomicReference<LocalDateTime>();
        transaction.executeWithoutResult(status -> {
            UserEntity editor = repository.findById(userId).orElseThrow();
            independent().executeWithoutResult(other -> service.updateLastActivity(userId));
            activity.set(independent().execute(other -> stored().getLastActivity()));
            if (statusChange) {
                service.updateUserStatus(editor, UserStatus.ABSENT, false);
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
        service.updateUserStatus(stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, false);
        if (!toAbsent) {
            service.updateLastActivity(userId);
        }
        UserRepository selection = mock(UserRepository.class);
        if (toAbsent) {
            when(selection.findByLastActivityBeforeAndStatus(any(), eq(UserStatus.AVAILABLE))).thenAnswer(call -> {
                var candidates = repository.findByLastActivityBeforeAndStatus(call.getArgument(0), UserStatus.AVAILABLE);
                assertEquals(1, candidates.size());
                service.updateLastActivity(userId);
                return candidates;
            });
        } else {
            when(selection.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(any(), eq(UserStatus.ABSENT))).thenAnswer(call -> {
                var candidates = repository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(call.getArgument(0), UserStatus.ABSENT);
                assertEquals(1, candidates.size());
                transaction.executeWithoutResult(tx -> stored().setLastActivity(LocalDateTime.now().minusMinutes(10)));
                return candidates;
            });
        }
        ConfigService config = mock(ConfigService.class);
        if (toAbsent) {
            when(config.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
        }
        var scheduler = new UserStatusScheduler(selection, service, config);
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
        for (boolean toAbsent : new boolean[]{true, false}) {
            service.updateUserStatus(stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, true);
            service.updateUserStatus(stored(), toAbsent ? UserStatus.AVAILABLE : UserStatus.ABSENT, false);
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
            transaction.executeWithoutResult(tx -> stored().setLastActivity(toAbsent ? cutoff.minusMinutes(1) : cutoff.plusMinutes(1)));
            var candidates = toAbsent
                    ? repository.findByLastActivityBeforeAndStatus(cutoff, UserStatus.AVAILABLE)
                    : repository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(cutoff, UserStatus.ABSENT);
            assertEquals(1, candidates.size());
            service.updateUserStatus(stored(), manual, true);
            messages.clear();
            listen();
            service.updateScheduledStatus(candidates.getFirst().getId(), toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE, cutoff);
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
        repository.deleteById(userId);
        listen();
        assertThrows(IllegalStateException.class, () -> service.updateUserStatus(snapshot, UserStatus.AVAILABLE, true));
        assertThrows(IllegalStateException.class, () -> service.updateLastActivity(userId));
        assertThrows(OptimisticLockingFailureException.class, () -> service.updateAdminUser(snapshot, snapshot.getVersion(), "new"));
        assertDoesNotThrow(() -> service.updateScheduledStatus(userId, UserStatus.ABSENT, LocalDateTime.now()));
        assertFalse(repository.existsById(userId));
        assertTrue(messages.isEmpty());
    }

    @Test
    void missingFormVersionIsRejectedWithoutChangingAnyField() {
        UserEntity edited = form();
        edited.setFirstName("Rejected");
        assertThrows(OptimisticLockingFailureException.class, () -> service.updateAdminUser(edited, null, "new"));
        assertEquals("Test", stored().getFirstName());
        assertEquals("old-hash", stored().getPassword());
        assertEquals(edited.getVersion(), stored().getVersion());
    }

    @Test
    void explicitAdminPasswordPreservesUnrelatedStatusLocaleAndActivity() {
        service.updateUserStatus(stored(), UserStatus.EMPLOYED, true);
        transaction.executeWithoutResult(tx -> stored().setPreferredLocale(Locale.GERMAN));
        UserEntity edited = form();
        service.updateLastActivity(userId);
        LocalDateTime activity = stored().getLastActivity();
        // These snapshot fields are not admin-editable and must not be merged.
        edited.setStatus(UserStatus.OFFLINE);
        edited.setStatusManuallySet(false);
        edited.setPreferredLocale(Locale.ENGLISH);
        edited.setLastActivity(LocalDateTime.MIN);
        edited.setFirstName("Admin edited");
        service.updateAdminUser(edited, edited.getVersion(), "explicit-new-password");
        UserEntity fresh = stored();
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("explicit-new-password", fresh.getPassword()));
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
        LocalDateTime activity = LocalDateTime.now().minusMinutes(2).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        transaction.executeWithoutResult(status -> {
            UserEntity user = repository.findById(userId).orElseThrow();
            user.setEnabled(false);
            user.setPassword("new-hash");
            user.setPreferredLocale(Locale.GERMAN);
            user.setFirstName("Changed");
            user.setLastActivity(activity);
            user.getRoleEntities().clear();
        });
        listen();

        var result = service.updateUserStatus(stale, UserStatus.AVAILABLE, true);

        UserEntity current = stored();
        assertFalse(current.isEnabled());
        assertEquals("new-hash", current.getPassword());
        assertEquals(Locale.GERMAN, current.getPreferredLocale());
        assertEquals("Changed", current.getFirstName());
        assertEquals(activity, current.getLastActivity());
        assertTrue(form().getRoleEntities().isEmpty());
        assertEquals(new UserService.StatusUpdate(UserStatus.AVAILABLE, true), result);
        assertEquals(UserStatus.OFFLINE, stale.getStatus());
        assertEquals(List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "AVAILABLE")), messages);
    }

    @Test
    void staleAdminFormCannotUndoAnotherFormOrPasswordChange() {
        UserEntity first = form();
        UserEntity stale = form();
        first.setFirstName("First editor");
        service.updateAdminUser(first, first.getVersion(), "");
        stale.setLastName("Second editor");

        var conflict = assertThrows(OptimisticLockingFailureException.class,
                () -> service.updateAdminUser(stale, stale.getVersion(), ""));
        assertTrue(conflict.getMessage().contains("Formular neu öffnen"));
        assertEquals("First editor", stored().getFirstName());
        assertEquals("User", stored().getLastName());

        UserEntity beforePasswordChange = form();
        transaction.executeWithoutResult(status -> repository.findById(userId).orElseThrow().setPassword("reset-hash"));
        assertThrows(OptimisticLockingFailureException.class,
                () -> service.updateAdminUser(beforePasswordChange, beforePasswordChange.getVersion(), ""));
        assertEquals("reset-hash", stored().getPassword());
    }

    @Test
    void activityDoesNotInvalidateAdminFormAndAdminSavePreservesActivity() {
        UserEntity edited = form();
        Long version = edited.getVersion();
        for (int i = 0; i < 3; i++) {
            service.updateLastActivity(userId);
        }
        LocalDateTime activity = stored().getLastActivity();
        assertEquals(version, stored().getVersion());
        assertTrue(activity.isAfter(edited.getLastActivity()));
        edited.setFirstName("Edited");
        service.updateAdminUser(edited, version, "");
        assertEquals(activity, stored().getLastActivity());
        assertEquals("Edited", stored().getFirstName());
    }

    @Test
    void statusChangeInvalidatesAdminFormButRepeatedNoOpDoesNotAdvanceVersion() {
        UserEntity edited = form();
        service.updateUserStatus(edited, UserStatus.AVAILABLE, true);
        Long version = stored().getVersion();
        service.updateUserStatus(edited, UserStatus.AVAILABLE, true);
        assertEquals(version, stored().getVersion());
        assertThrows(OptimisticLockingFailureException.class,
                () -> service.updateAdminUser(edited, edited.getVersion(), ""));
        assertEquals(UserStatus.AVAILABLE, stored().getStatus());
    }

    @Test
    void genericSaveAlsoRejectsStaleDetachedEntities() {
        UserEntity stale = form();
        transaction.executeWithoutResult(status -> repository.findById(userId).orElseThrow().setPassword("reset-hash"));
        stale.setFirstName("Stale");
        assertThrows(OptimisticLockingFailureException.class, () -> service.saveUser(stale));
        assertEquals("reset-hash", stored().getPassword());
        assertEquals("Test", stored().getFirstName());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void adminFormUpdateRetainsLastAdminProtection(boolean disable) {
        transaction.executeWithoutResult(status -> {
            RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
            entityManager.persist(role);
            repository.findById(userId).orElseThrow().setRoleEntities(new ArrayList<>(List.of(role)));
        });
        UserEntity edited = form();
        if (disable) {
            edited.setEnabled(false);
        } else {
            edited.setRoleEntities(List.of());
        }
        assertThrows(IllegalStateException.class,
                () -> service.updateAdminUser(edited, edited.getVersion(), ""));
        assertTrue(stored().isEnabled());
        assertTrue(form().hasRole(RoleType.ROLE_ADMIN));
        service.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
        assertTrue(form().hasRole(RoleType.ROLE_ADMIN));
    }

    @Test
    void detachedListingAndAdminHelpersHaveAnExplicitRolesLoadContract() {
        List<UserEntity> users = service.findAllUsers();
        assertTrue(Hibernate.isInitialized(users.getFirst().getRoleEntities()));
        assertTrue(users.getFirst().hasRole(RoleType.ROLE_USER));
        assertTrue(service.canDeleteUser(users.getFirst()));
        assertTrue(service.wouldRemoveLastEnabledAdmin(userId, false, users.getFirst().getRoleEntities()));
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    void automaticPriorityUsesStoredStateNotTheDetachedSnapshot(UserStatus manualStatus) {
        UserEntity stale = stored();
        service.updateUserStatus(stale, manualStatus, true);
        listen();
        var result = service.updateUserStatus(stale, UserStatus.ABSENT, false);
        boolean protectedStatus = manualStatus == UserStatus.EMPLOYED || manualStatus == UserStatus.OFFLINE;
        assertEquals(protectedStatus ? manualStatus : UserStatus.ABSENT, result.status());
        assertEquals(protectedStatus, result.manuallySet());
        assertEquals(result.status(), stored().getStatus());
        assertEquals(protectedStatus || manualStatus == UserStatus.ABSENT ? 0 : 1, messages.size());
    }

    @Test
    void schedulerRunsBothDirectionsWithoutOsivOrInitializedRoles() {
        ConfigService config = mock(ConfigService.class);
        when(config.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
        var scheduler = new UserStatusScheduler(repository, service, config);
        service.updateUserStatus(stored(), UserStatus.AVAILABLE, false);
        UserEntity candidate = repository.findByLastActivityBeforeAndStatus(LocalDateTime.now(), UserStatus.AVAILABLE).getFirst();
        assertFalse(Hibernate.isInitialized(candidate.getRoleEntities()));
        listen();
        scheduler.checkInactiveAvailableUsers();
        assertEquals(UserStatus.ABSENT, stored().getStatus());
        service.updateLastActivity(userId);
        scheduler.checkRecentlyActiveAbsentUsers();
        assertEquals(UserStatus.AVAILABLE, stored().getStatus());
        assertEquals(2, messages.size());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    @Test
    void eventWaitsForOuterCommitAndContainsStoredOldStatus() {
        UserEntity stale = stored();
        stale.setStatus(UserStatus.EMPLOYED); // Even the former popover ordering cannot corrupt the old status.
        listen();
        transaction.executeWithoutResult(status -> {
            service.updateUserStatus(stale, UserStatus.AVAILABLE, true);
            repository.flush();
            assertTrue(messages.isEmpty());
        });
        assertEquals(List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "AVAILABLE")), messages);
        assertEquals(UserStatus.AVAILABLE, stored().getStatus());
    }

    @Test
    void rollbackSendsNothingEvenAfterFlush() {
        listen();
        transaction.executeWithoutResult(status -> {
            service.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
            repository.flush();
            status.setRollbackOnly();
        });
        assertTrue(messages.isEmpty());
        assertEquals(UserStatus.OFFLINE, stored().getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void repeatedChangesInOneTransactionOnlySendTheCommittedNetChange(boolean returnToOriginal) {
        listen();
        transaction.executeWithoutResult(status -> {
            service.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
            repository.flush();
            service.updateUserStatus(stored(), returnToOriginal ? UserStatus.OFFLINE : UserStatus.EMPLOYED, true);
            assertTrue(messages.isEmpty());
        });
        assertEquals(returnToOriginal ? UserStatus.OFFLINE : UserStatus.EMPLOYED, stored().getStatus());
        assertEquals(returnToOriginal ? List.of()
                : List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "EMPLOYED")), messages);
    }

    @Test
    void realOptimisticCommitFailureSendsNothingAndPreservesConcurrentPasswordChange() {
        listen();
        var independent = new TransactionTemplate(transaction.getTransactionManager());
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThrows(OptimisticLockingFailureException.class, () -> transaction.executeWithoutResult(status -> {
            service.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
            independent.executeWithoutResult(other -> repository.findById(userId).orElseThrow().setPassword("concurrent-hash"));
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
        service.updateUserStatus(stored(), UserStatus.OFFLINE, true);
        assertTrue(stored().isStatusManuallySet());
        assertTrue(messages.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void loginPreservesManualPriorityAndUsesCommitBoundStatusUpdates(boolean manual) {
        if (manual) {
            service.updateUserStatus(stored(), UserStatus.EMPLOYED, true);
        }
        LocalDateTime previousActivity = stored().getLastActivity();
        listen();
        security.handleLogin(org.springframework.security.core.userdetails.User.withUsername("status@example.com")
                .password("hash").roles("USER").build());
        assertEquals(manual ? UserStatus.EMPLOYED : UserStatus.AVAILABLE, stored().getStatus());
        assertTrue(stored().getLastActivity().isAfter(previousActivity));
        assertEquals(manual ? 0 : 1, messages.size());
    }

    @Test
    void documentedMigrationBackfillsLegacyRowsBeforeVersionedJpaUpdates() {
        // Isolated in-memory test DB only: emulate a legacy table without version.
        transaction.executeWithoutResult(status -> {
            entityManager.createNativeQuery("alter table users drop column version").executeUpdate();
            entityManager.createNativeQuery("alter table users add column version bigint default 0").executeUpdate();
            entityManager.createNativeQuery("update users set version = null").executeUpdate();
            entityManager.createNativeQuery("update users set version = 0 where version is null").executeUpdate();
            entityManager.createNativeQuery("alter table users alter column version set not null").executeUpdate();
        });
        assertEquals(0L, stored().getVersion());
        service.updateUserStatus(stored(), UserStatus.AVAILABLE, true);
        assertEquals(1L, stored().getVersion());
    }

    @Test
    void faultyFirstListenerDoesNotBlockHealthyListenerOrSuccessfulPersistence() {
        registrations.add(broadcaster.register(message -> { throw new IllegalStateException("broken listener"); }));
        listen();
        assertDoesNotThrow(() -> service.updateUserStatus(stored(), UserStatus.AVAILABLE, true));
        assertEquals(UserStatus.AVAILABLE, stored().getStatus());
        assertEquals(1, messages.size());
    }

    private UserPopoverMenu popover(UserService statusService, UserEntity snapshot, List<UserStatus> icons) {
        ui = new UI();
        UI.setCurrent(ui);
        session = mock(VaadinSession.class);
        when(session.getLocale()).thenReturn(Locale.ENGLISH);
        when(session.hasLock()).thenReturn(true);
        VaadinSession.setCurrent(session);
        MessageProperties texts = mock(MessageProperties.class, invocation ->
                invocation.getMethod().getReturnType() == String.class ? "Text" : RETURNS_DEFAULTS.answer(invocation));
        var menu = new UserPopoverMenu(texts, mock(SecurityService.class), statusService, snapshot, new Button(),
                new UserPopoverMenu.Actions(null, null, null, null), icons::add);
        icons.clear();
        return menu;
    }

    @Test
    void actualPopoverCommitsBeforeUpdatingSnapshotAndIcons() {
        UserEntity snapshot = stored();
        List<UserStatus> icons = new ArrayList<>();
        UserPopoverMenu menu = popover(service, snapshot, icons);
        registrations.add(broadcaster.register(message -> {
            assertEquals(UserStatus.OFFLINE, snapshot.getStatus());
            assertTrue(icons.isEmpty());
            messages.add(message);
        }));

        ReflectionTestUtils.invokeMethod(menu, "handleUserStatusChange", UserStatus.EMPLOYED, true);

        assertEquals(UserStatus.EMPLOYED, stored().getStatus());
        assertEquals(UserStatus.EMPLOYED, snapshot.getStatus());
        assertTrue(snapshot.isStatusManuallySet());
        assertFalse(icons.isEmpty());
        assertTrue(icons.stream().allMatch(value -> value == UserStatus.EMPLOYED));
        assertEquals(List.of(new UserStatusBroadcaster.UserStatusMessage(userId, "OFFLINE", "EMPLOYED")), messages);
    }

    @Test
    void failedPopoverUpdateKeepsSnapshotAndIconsAtThePreviousStatus() {
        UserEntity snapshot = stored();
        UserService failing = mock(UserService.class);
        List<UserStatus> icons = new ArrayList<>();
        UserPopoverMenu menu = popover(failing, snapshot, icons);
        when(failing.updateUserStatus(snapshot, UserStatus.EMPLOYED, true)).thenAnswer(invocation -> {
            assertEquals(UserStatus.OFFLINE, snapshot.getStatus());
            assertTrue(icons.isEmpty());
            throw new OptimisticLockingFailureException("conflict");
        });

        assertDoesNotThrow(
                () -> ReflectionTestUtils.invokeMethod(menu, "handleUserStatusChange", UserStatus.EMPLOYED, true));
        var helper = NotificationHelper.getInstance();
        var notification = (com.vaadin.flow.component.notification.Notification) ReflectionTestUtils.getField(helper, "currentNotification");
        assertTrue(notification.isOpened());
        var title = (com.vaadin.flow.component.html.Span) ReflectionTestUtils.getField(helper, "titleText");
        var message = (com.vaadin.flow.component.Html) ReflectionTestUtils.getField(helper, "messageText");
        assertEquals("Text", title.getText());
        assertEquals("p", message.getElement().getTag());
        assertEquals("Text", message.getElement().getProperty("innerHTML"));
        assertEquals(UserStatus.OFFLINE, snapshot.getStatus());
        assertTrue(icons.stream().allMatch(value -> value == UserStatus.OFFLINE));
        verify(failing, times(1)).updateUserStatus(snapshot, UserStatus.EMPLOYED, true);
    }
}
