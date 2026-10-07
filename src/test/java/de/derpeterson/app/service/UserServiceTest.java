package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit tests use real entities and mocked boundaries; no Spring context or database. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStatusBroadcaster userStatusBroadcaster;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userStatusBroadcaster, passwordEncoder);
    }

    @Test
    void activityPropagatesOtherDatabaseFailuresWithoutLoadingOrRetryingAnEntity() {
        var failure = repositoryFailure();
        when(userRepository.updateLastActivity(eq(1L), any())).thenThrow(failure);
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> userService.updateLastActivity(1L)));
        verify(userRepository, times(1)).updateLastActivity(eq(1L), any());
        verify(userRepository, never()).findById(any());
        verifyNoInteractions(userStatusBroadcaster, passwordEncoder);
    }

    @Nested
    class Persistence {
        @ParameterizedTest(name = "saveUser alias: {0}")
        @ValueSource(booleans = {true, false})
        void bothSaveMethodsPersistTheSuppliedUser(boolean useSaveUser) {
            UserEntity user = user(1L, true, RoleType.ROLE_USER);

            save(user, useSaveUser);

            verify(userRepository).save(same(user));
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        @ParameterizedTest(name = "saveUser alias: {0}")
        @ValueSource(booleans = {true, false})
        void bothSaveMethodsPropagatePersistenceFailures(boolean useSaveUser) {
            UserEntity user = user(1L, true);
            var failure = repositoryFailure();
            when(userRepository.save(user)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> save(user, useSaveUser)));
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        private void save(UserEntity user, boolean useSaveUser) {
            if (useSaveUser) {
                userService.saveUser(user);
            } else {
                userService.save(user);
            }
        }

        @Test
        void findsAnExistingUserUsingTheSuppliedEmail() {
            UserEntity user = user(1L, true);
            String email = " MixedCase@example.com ";
            when(userRepository.findByEmail("mixedcase@example.com")).thenReturn(Optional.of(user));

            assertSame(user, userService.findByEmail(email).orElseThrow());
            verify(userRepository).findByEmail("mixedcase@example.com");
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        @Test
        void returnsEmptyWhenEmailIsUnknown() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            assertTrue(userService.findByEmail("missing@example.com").isEmpty());
        }

        @Test
        void propagatesEmailLookupFailure() {
            var failure = repositoryFailure();
            when(userRepository.findByEmail("test@example.com")).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.findByEmail("test@example.com")));
        }

        @Test
        void returnsAllUsersInRepositoryOrder() {
            List<UserEntity> users = List.of(user(2L, false), user(1L, true));
            when(userRepository.findAll()).thenReturn(users);

            assertEquals(users, userService.findAllUsers());
            verify(userRepository).findAll();
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        @Test
        void returnsAnEmptyListWhenThereAreNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertTrue(userService.findAllUsers().isEmpty());
        }

        @Test
        void propagatesListingFailure() {
            var failure = repositoryFailure();
            when(userRepository.findAll()).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    userService::findAllUsers));
        }

        @Test
        void deletesTheSuppliedUser() {
            UserEntity user = user(1L, true, RoleType.ROLE_USER);

            userService.deleteUser(user);

            verify(userRepository).delete(same(user));
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        @Test
        void propagatesDeletionFailure() {
            UserEntity user = user(1L, true);
            var failure = repositoryFailure();
            doThrow(failure).when(userRepository).delete(user);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.deleteUser(user)));
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }
    }

    @Nested
    class MutationAdminProtection {
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void nullSaveStillRaisesAnArgumentError(boolean useSaveUser) {
            assertThrows(IllegalArgumentException.class, () -> save(null, useSaveUser));
            verifyNoInteractions(userRepository);
        }

        @Test
        void nullDeleteStillRaisesAnArgumentError() {
            assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(null));
            verifyNoInteractions(userRepository);
        }

        @Test
        void deletionRejectsTheLastStoredAdminEvenIfTheSuppliedUserLooksOrdinary() {
            UserEntity staleUser = user(1000L, false, RoleType.ROLE_USER);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1000L));

            assertThrows(IllegalStateException.class, () -> userService.deleteUser(staleUser));

            verify(userRepository, never()).delete(any());
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        @Test
        void deletionRejectsTheLastEnabledAdminWithoutAPreliminaryUiCheck() {
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L));

            assertThrows(IllegalStateException.class,
                    () -> userService.deleteUser(user(1L, true, RoleType.ROLE_ADMIN)));

            verify(userRepository, never()).delete(any());
        }

        @Test
        void deletionAllowsAnAdminIfAnotherEnabledAdminRemains() {
            UserEntity admin = user(1L, true, RoleType.ROLE_ADMIN);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L, 2L));

            userService.deleteUser(admin);

            var order = inOrder(userRepository);
            order.verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
            order.verify(userRepository).delete(same(admin));
        }

        @Test
        void deletionAllowsADisabledAdminWhenTheStoredEnabledAdminIsSomeoneElse() {
            UserEntity disabledAdmin = user(1L, false, RoleType.ROLE_ADMIN);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(2L));

            userService.deleteUser(disabledAdmin);

            verify(userRepository).delete(same(disabledAdmin));
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void bothSaveMethodsRejectDisablingTheLastStoredAdmin(boolean useSaveUser) {
            UserEntity edited = user(1L, false, RoleType.ROLE_ADMIN);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L));

            assertThrows(IllegalStateException.class, () -> save(edited, useSaveUser));

            verify(userRepository, never()).save(any());
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void bothSaveMethodsRejectRemovingTheLastAdminsRole(boolean useSaveUser) {
            UserEntity edited = user(1L, true, RoleType.ROLE_USER);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L));

            assertThrows(IllegalStateException.class, () -> save(edited, useSaveUser));

            verify(userRepository, never()).save(any());
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void bothSaveMethodsAllowDisablingWhenAnotherAdminRemains(boolean useSaveUser) {
            UserEntity edited = user(1L, false, RoleType.ROLE_ADMIN);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L, 2L));

            save(edited, useSaveUser);

            verify(userRepository).save(same(edited));
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void bothSaveMethodsAllowRoleRemovalWhenAnotherAdminRemains(boolean useSaveUser) {
            UserEntity edited = user(1L, true, RoleType.ROLE_USER);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L, 2L));

            save(edited, useSaveUser);

            verify(userRepository).save(same(edited));
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void bothSaveMethodsAllowUnchangedAdminPrivileges(boolean useSaveUser) {
            UserEntity admin = user(1L, true, RoleType.ROLE_ADMIN);

            save(admin, useSaveUser);

            verify(userRepository).save(same(admin));
            verify(userRepository, atLeastOnce()).findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void registrationStillWorksBeforeTheFirstAdminIsCreated(boolean useSaveUser) {
            UserEntity newUser = user(null, false, RoleType.ROLE_USER);

            save(newUser, useSaveUser);

            verify(userRepository).save(same(newUser));
            verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void ordinaryAccountUpdatesDoNotDependOnBeingAnAdmin(boolean useSaveUser) {
            UserEntity ordinary = user(1L, true, RoleType.ROLE_USER);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(2L));

            save(ordinary, useSaveUser);

            verify(userRepository).save(same(ordinary));
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void failedAdminLookupPreventsSavingAndPropagatesTheOriginalException(boolean useSaveUser) {
            var failure = repositoryFailure();
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> save(user(1L, false, RoleType.ROLE_ADMIN), useSaveUser)));

            verify(userRepository, never()).save(any());
        }

        @Test
        void failedAdminLookupPreventsDeletingAndPropagatesTheOriginalException() {
            var failure = repositoryFailure();
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.deleteUser(user(1L, true, RoleType.ROLE_ADMIN))));

            verify(userRepository, never()).delete(any());
        }

        @Test
        void statusUpdateIgnoresDemotionInTheSuppliedSnapshot() {
            UserEntity stored = user(1L, true, RoleType.ROLE_ADMIN);
            when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
            TransactionSynchronizationManager.initSynchronization();
            try {
                userService.updateUserStatus(user(1L, false, RoleType.ROLE_ADMIN), UserStatus.AVAILABLE, true);
                assertTrue(stored.isEnabled());
                verify(userRepository).save(same(stored));
                verify(userRepository, never()).findEnabledUserIdsByRole(any());
                verifyNoInteractions(userStatusBroadcaster);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        private void save(UserEntity user, boolean useSaveUser) {
            if (useSaveUser) {
                userService.saveUser(user);
            } else {
                userService.save(user);
            }
        }
    }

    @Nested
    class LocaleUpdates {
        @Test
        void savesAnExistingUsersNewLocale() {
            UserEntity user = user(1L, true);
            user.setPreferredLocale(Locale.ENGLISH);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            userService.updateUserLocale(user.getEmail(), Locale.FRENCH);

            assertEquals(Locale.FRENCH, user.getPreferredLocale());
            verify(userRepository).save(same(user));
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        @Test
        void doesNotSaveWhenTheUserIsMissing() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            userService.updateUserLocale("missing@example.com", Locale.GERMAN);

            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
        }

        @Test
        void rejectsNullLocaleBeforeLookingUpOrChangingTheUser() {
            UserEntity user = user(1L, true);
            assertThrows(IllegalArgumentException.class, () -> userService.updateUserLocale(user.getEmail(), null));
            assertEquals(Locale.ENGLISH, user.getPreferredLocale());
            verifyNoInteractions(userRepository);
        }

        @Test
        void acceptsTheRootLocale() {
            UserEntity user = user(1L, true);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            userService.updateUserLocale(user.getEmail(), Locale.ROOT);

            assertEquals(Locale.ROOT, user.getPreferredLocale());
            verify(userRepository).save(same(user));
        }

        @Test
        void doesNotSaveWhenLocaleLookupFails() {
            var failure = repositoryFailure();
            when(userRepository.findByEmail("test@example.com")).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.updateUserLocale("test@example.com", Locale.GERMAN)));
            verify(userRepository, never()).save(any());
        }

        @Test
        void propagatesLocalePersistenceFailure() {
            UserEntity user = user(1L, true);
            var failure = repositoryFailure();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.updateUserLocale(user.getEmail(), Locale.GERMAN)));
        }
    }

    @Nested
    class PasswordUpdates {
        @ParameterizedTest
        @ValueSource(strings = {"NewPassword!", "  Password with spaces!  ", "A!234567"})
        void encodesTheUnmodifiedInputWithoutSavingTheUser(String rawPassword) {
            UserEntity user = user(1L, true);
            user.setPassword("old-hash");
            when(passwordEncoder.encode(rawPassword)).thenReturn("new-hash");

            userService.updatePassword(user, rawPassword);

            assertEquals("new-hash", user.getPassword());
            verify(passwordEncoder).encode(rawPassword);
            verifyNoInteractions(userRepository, userStatusBroadcaster);
        }

        @Test
        void rejectsANullUser() {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.updatePassword(null, "newRawPassword"));
            verifyNoInteractions(userRepository, userStatusBroadcaster);
        }

        @Test
        void leavesTheExistingPasswordUnchangedWhenEncodingFails() {
            UserEntity user = user(1L, true);
            user.setPassword("old-hash");
            var failure = new IllegalStateException("Encoding failed");
            when(passwordEncoder.encode("NewPassword!")).thenThrow(failure);

            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> userService.updatePassword(user, "NewPassword!")));
            assertEquals("old-hash", user.getPassword());
            verifyNoInteractions(userRepository, userStatusBroadcaster);
        }

        @Test
        void rejectsNullPasswordWithoutEncodingOrChangingTheUser() {
            UserEntity user = user(1L, true);
            user.setPassword("old-hash");
            assertThrows(IllegalArgumentException.class, () -> userService.updatePassword(user, null));
            assertEquals("old-hash", user.getPassword());
            verifyNoInteractions(userRepository, userStatusBroadcaster);
            verifyNoInteractions(passwordEncoder);
        }
    }

    @Nested
    class StatusUpdates {
        @BeforeEach
        void openSynchronization() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void closeSynchronization() {
            TransactionSynchronizationManager.clearSynchronization();
        }

        private UserEntity user(Long id, boolean enabled) {
            UserEntity stored = UserServiceTest.user(id, enabled);
            when(userRepository.findById(id)).thenReturn(Optional.of(stored));
            return stored;
        }

        private void commit() {
            verifyNoInteractions(userStatusBroadcaster);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        }

        @Test
        void manualChangeSavesStatusAndFlagBeforeBroadcasting() {
            UserEntity user = user(1L, true);
            user.setStatus(UserStatus.AVAILABLE);
            doAnswer(invocation -> {
                assertEquals(UserStatus.EMPLOYED, user.getStatus());
                assertTrue(user.isStatusManuallySet());
                return user;
            }).when(userRepository).save(user);

            userService.updateUserStatus(user, UserStatus.EMPLOYED, true);
            commit();

            var order = inOrder(userRepository, userStatusBroadcaster);
            order.verify(userRepository).save(same(user));
            order.verify(userStatusBroadcaster).broadcast(message(user, UserStatus.AVAILABLE, UserStatus.EMPLOYED));
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        void automaticChangeSavesNewStatusWithoutManualFlag() {
            UserEntity user = user(1L, true);
            user.setStatus(UserStatus.AVAILABLE);

            userService.updateUserStatus(user, UserStatus.ABSENT, false);
            commit();

            assertEquals(UserStatus.ABSENT, user.getStatus());
            assertFalse(user.isStatusManuallySet());
            verify(userRepository).save(same(user));
            verify(userStatusBroadcaster).broadcast(message(user, UserStatus.AVAILABLE, UserStatus.ABSENT));
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"AVAILABLE", "ABSENT"})
        void automaticChangeResetsManualAvailableOrAbsentStatus(UserStatus previousStatus) {
            UserEntity user = user(1L, true);
            user.setManualStatus(previousStatus);

            userService.updateUserStatus(user, UserStatus.OFFLINE, false);
            commit();

            assertEquals(UserStatus.OFFLINE, user.getStatus());
            assertFalse(user.isStatusManuallySet());
            verify(userRepository).save(same(user));
            verify(userStatusBroadcaster).broadcast(message(user, previousStatus, UserStatus.OFFLINE));
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"EMPLOYED", "OFFLINE"})
        void protectedManualStatusIsRetainedWithoutBroadcastingARejectedChange(UserStatus previousStatus) {
            UserEntity user = user(1L, true);
            user.setManualStatus(previousStatus);

            userService.updateUserStatus(user, UserStatus.ABSENT, false);

            assertEquals(previousStatus, user.getStatus());
            assertTrue(user.isStatusManuallySet());
            verify(userRepository).save(same(user));
            verifyNoInteractions(userStatusBroadcaster);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void sameStatusRequestPersistsTheManualFlagWithoutBroadcasting(boolean manualChange) {
            UserEntity user = user(1L, true);
            user.setStatus(UserStatus.AVAILABLE);
            user.setStatusManuallySet(!manualChange);
            doAnswer(invocation -> {
                assertEquals(UserStatus.AVAILABLE, user.getStatus());
                assertEquals(manualChange, user.isStatusManuallySet());
                return user;
            }).when(userRepository).save(user);

            userService.updateUserStatus(user, UserStatus.AVAILABLE, manualChange);

            assertEquals(UserStatus.AVAILABLE, user.getStatus());
            assertEquals(manualChange, user.isStatusManuallySet());
            verify(userRepository).save(same(user));
            verifyNoInteractions(userStatusBroadcaster);
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"EMPLOYED", "OFFLINE"})
        void rejectedAutomaticChangeStillPropagatesPersistenceFailure(UserStatus previousStatus) {
            UserEntity user = user(1L, true);
            user.setManualStatus(previousStatus);
            var failure = repositoryFailure();
            when(userRepository.save(user)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.updateUserStatus(user, UserStatus.ABSENT, false)));
            assertEquals(previousStatus, user.getStatus());
            assertTrue(user.isStatusManuallySet());
            verify(userRepository).save(same(user));
            verifyNoInteractions(userStatusBroadcaster);
        }

        @Test
        void manualChangeCanReplaceAProtectedManualStatus() {
            UserEntity user = user(1L, true);
            user.setManualStatus(UserStatus.EMPLOYED);

            userService.updateUserStatus(user, UserStatus.AVAILABLE, true);
            commit();

            assertEquals(UserStatus.AVAILABLE, user.getStatus());
            assertTrue(user.isStatusManuallySet());
            verify(userRepository).save(same(user));
            verify(userStatusBroadcaster).broadcast(message(user, UserStatus.EMPLOYED, UserStatus.AVAILABLE));
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void doesNotBroadcastWhenSavingFails(boolean manualChange) {
            UserEntity user = user(1L, true);
            var failure = repositoryFailure();
            when(userRepository.save(user)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.updateUserStatus(user, UserStatus.AVAILABLE, manualChange)));
            verifyNoInteractions(userStatusBroadcaster);
        }

        @Test
        void isolatesBroadcastFailureAfterCommit() {
            UserEntity user = user(1L, true);
            var event = message(user, UserStatus.OFFLINE, UserStatus.AVAILABLE);
            var failure = new IllegalStateException("Listener failed");
            doThrow(failure).when(userStatusBroadcaster).broadcast(event);

            userService.updateUserStatus(user, UserStatus.AVAILABLE, true);
            assertDoesNotThrow(this::commit);
            assertEquals(UserStatus.AVAILABLE, user.getStatus());
            verify(userRepository).save(same(user));
        }
    }

    @Nested
    class EmailUniqueness {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        void blankEmailDoesNotQueryTheRepository(String email) {
            assertFalse(userService.emailExistsForOtherUser(email, 1L));
            verifyNoInteractions(userRepository);
        }

        @Test
        void returnsFalseWhenNoUsersExist() {
            when(userRepository.emailExistsForOtherUser("test@example.com", 1L)).thenReturn(false);

            assertFalse(userService.emailExistsForOtherUser("test@example.com", 1L));
        }

        @Test
        void returnsFalseWhenOnlyUnrelatedEmailsExist() {
            when(userRepository.emailExistsForOtherUser("missing@example.com", 1L)).thenReturn(false);

            assertFalse(userService.emailExistsForOtherUser("missing@example.com", 1L));
        }

        @ParameterizedTest
        @ValueSource(strings = {"test@example.com", "TEST@EXAMPLE.COM", "  TEST@EXAMPLE.COM  "})
        void detectsAnotherUsersEmailWithCanonicalQueryParameter(String email) {
            when(userRepository.emailExistsForOtherUser("test@example.com", 1L)).thenReturn(true);

            assertTrue(userService.emailExistsForOtherUser(email, 1L));
        }

        @Test
        void ignoresTheCurrentAccountByIdRatherThanEntityIdentity() {
            UserEntity stored = user(1000L, true);
            when(userRepository.emailExistsForOtherUser(stored.getEmail(), 1000L)).thenReturn(false);

            assertFalse(userService.emailExistsForOtherUser(stored.getEmail(), Long.valueOf("1000")));
        }

        @Test
        void creationWithNoCurrentIdDetectsAnyMatchingAccount() {
            UserEntity existing = user(1L, true);
            when(userRepository.emailExistsForOtherUser(existing.getEmail(), null)).thenReturn(true);

            assertTrue(userService.emailExistsForOtherUser(existing.getEmail(), null));
        }

        @Test
        void delegatesCurrentUserExclusionToTheTargetedQuery() {
            UserEntity current = user(1L, true);
            when(userRepository.emailExistsForOtherUser(current.getEmail(), current.getId())).thenReturn(true);

            assertTrue(userService.emailExistsForOtherUser(current.getEmail(), current.getId()));
        }

        @Test
        void propagatesUniquenessLookupFailure() {
            var failure = repositoryFailure();
            when(userRepository.emailExistsForOtherUser("test@example.com", 1L)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.emailExistsForOtherUser("test@example.com", 1L)));
        }
    }

    @Nested
    class AdminProtection {
        @Test
        void cannotDeleteANullUser() {
            assertFalse(userService.canDeleteUser(null));
            verifyNoInteractions(userRepository);
        }

        @Test
        void canDeleteAnOrdinaryUserBasedOnStoredPrivileges() {
            assertTrue(userService.canDeleteUser(user(1L, true, RoleType.ROLE_USER)));
            verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
        }

        @Test
        void canDeleteUsersWithEmptyOrMissingRoles() {
            UserEntity withoutRoles = user(1L, true);
            assertTrue(userService.canDeleteUser(withoutRoles));
            withoutRoles.setRoleEntities(null);
            assertTrue(userService.canDeleteUser(withoutRoles));
            verify(userRepository, times(2)).findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
        }

        @Test
        void canDeleteADisabledAdminBasedOnStoredPrivileges() {
            assertTrue(userService.canDeleteUser(user(1L, false, RoleType.ROLE_ADMIN)));
            verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
        }

        @Test
        void cannotDeleteAnEnabledAdminWhenNoOtherAccountsExist() {
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L));

            assertFalse(userService.canDeleteUser(user(1L, true, RoleType.ROLE_ADMIN)));
        }

        @Test
        void cannotDeleteLastEnabledAdminAndDoesNotCountSameIdOrIneligibleAccounts() {
            UserEntity target = user(1000L, true, RoleType.ROLE_ADMIN);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(Long.valueOf("1000")));

            assertFalse(userService.canDeleteUser(target));
            verify(userRepository, never()).delete(any());
        }

        @Test
        void canDeleteAdminWhenAnotherEnabledAdminRemains() {
            UserEntity target = user(1L, true, RoleType.ROLE_ADMIN);
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L, 2L));

            assertTrue(userService.canDeleteUser(target));
            verify(userRepository, never()).delete(any());
        }

        @Test
        void retainingEnabledAdminRoleNeedsNoOtherAdmin() {
            assertFalse(userService.wouldRemoveLastEnabledAdmin(1L, true,
                    List.of(role(RoleType.ROLE_USER), role(RoleType.ROLE_ADMIN))));
            verifyNoInteractions(userRepository);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void removalOfRoleOrDisablingTheLastAdminIsBlocked(boolean remainsEnabled) {
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L));
            List<RoleEntity> proposedRoles = remainsEnabled ? List.of() : List.of(role(RoleType.ROLE_ADMIN));

            assertTrue(userService.wouldRemoveLastEnabledAdmin(1L, remainsEnabled, proposedRoles));
        }

        @Test
        void missingProposedRolesAreRejectedBeforeTheAdminQuery() {
            assertThrows(IllegalArgumentException.class, () -> userService.wouldRemoveLastEnabledAdmin(1L, true, null));
            verifyNoInteractions(userRepository);
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void roleRemovalOrDisablingIsAllowedWhenAnotherAdminRemains(boolean remainsEnabled) {
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L, 2L));
            List<RoleEntity> proposedRoles = remainsEnabled ? List.of() : List.of(role(RoleType.ROLE_ADMIN));

            assertFalse(userService.wouldRemoveLastEnabledAdmin(1L, remainsEnabled, proposedRoles));
        }

        @Test
        void disabledAdminsAndOrdinaryUsersDoNotPreventLastAdminRemoval() {
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenReturn(List.of(1L));

            assertTrue(userService.wouldRemoveLastEnabledAdmin(1L, true, List.of(role(RoleType.ROLE_USER))));
        }

        @Test
        void newNonAdminAccountIsAllowedWhenAnEnabledAdminExists() {
            assertFalse(userService.wouldRemoveLastEnabledAdmin(null, true, List.of(role(RoleType.ROLE_USER))));
        }

        @Test
        void nonAdminCreationIsAllowedEvenBeforeTheFirstEnabledAdmin() {
            assertFalse(userService.wouldRemoveLastEnabledAdmin(null, true, List.of(role(RoleType.ROLE_USER))));
            verifyNoInteractions(userRepository);
        }

        @Test
        void propagatesAdminLookupFailureRatherThanGrantingDeletion() {
            var failure = repositoryFailure();
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.canDeleteUser(user(1L, true, RoleType.ROLE_ADMIN))));
            verify(userRepository, never()).delete(any());
        }

        @Test
        void propagatesAdminLookupFailureRatherThanAllowingRemoval() {
            var failure = repositoryFailure();
            when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN)).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> userService.wouldRemoveLastEnabledAdmin(1L, false, List.of())));
            verify(userRepository, never()).save(any());
        }
    }

    private static UserEntity user(Long id, boolean enabled, RoleType... roles) {
        return UserEntity.builder()
                .firstName("Test").lastName("User").password("hash")
                .gender(de.derpeterson.app.model.enums.Gender.OTHER)
                .birthDate(java.time.LocalDate.of(1990, 1, 1))
                .id(id)
                .email("user" + id + "@example.com")
                .enabled(enabled)
                .preferredLocale(Locale.ENGLISH)
                .status(UserStatus.OFFLINE)
                .roleEntities(Arrays.stream(roles).map(UserServiceTest::role).toList())
                .build();
    }

    private static RoleEntity role(RoleType type) {
        return RoleEntity.builder().name(type).build();
    }

    private static UserStatusBroadcaster.UserStatusMessage message(UserEntity user, UserStatus oldStatus,
                                                                  UserStatus requestedStatus) {
        return new UserStatusBroadcaster.UserStatusMessage(user.getId(), oldStatus.name(), requestedStatus.name());
    }

    private static DataAccessResourceFailureException repositoryFailure() {
        return new DataAccessResourceFailureException("Repository unavailable");
    }
}
