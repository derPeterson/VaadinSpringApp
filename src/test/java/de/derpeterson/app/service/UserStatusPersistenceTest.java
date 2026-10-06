package de.derpeterson.app.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
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
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
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
        UI.setCurrent(new UI());
        VaadinSession session = mock(VaadinSession.class);
        when(session.getLocale()).thenReturn(Locale.ENGLISH);
        VaadinSession.setCurrent(session);
        MessageProperties texts = mock(MessageProperties.class, invocation ->
                invocation.getMethod().getReturnType() == String.class ? "Text" : RETURNS_DEFAULTS.answer(invocation));
        // The pre-existing ABSENT renderer fetches an SVG over localhost HTTP. Isolate
        // that unrelated resource boundary, not the popover's status handler or service.
        try (var svg = mockStatic(UserStatus.class, invocation -> {
            if (invocation.getMethod().getName().equals("createSvgComponent")) {
                return new com.vaadin.flow.component.html.Div();
            }
            return invocation.callRealMethod();
        })) {
            var menu = new UserPopoverMenu(texts, mock(SecurityService.class), statusService, snapshot, new Button(),
                    new UserPopoverMenu.Actions(null, null, null, null), icons::add);
            icons.clear();
            return menu;
        }
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

        try (var notification = mockStatic(com.vaadin.flow.component.notification.Notification.class)) {
            assertDoesNotThrow(
                    () -> ReflectionTestUtils.invokeMethod(menu, "handleUserStatusChange", UserStatus.EMPLOYED, true));
            notification.verify(() -> com.vaadin.flow.component.notification.Notification.show("Text"));
        }
        assertEquals(UserStatus.OFFLINE, snapshot.getStatus());
        assertTrue(icons.stream().allMatch(value -> value == UserStatus.OFFLINE));
        verify(failing, times(1)).updateUserStatus(snapshot, UserStatus.EMPLOYED, true);
    }
}
