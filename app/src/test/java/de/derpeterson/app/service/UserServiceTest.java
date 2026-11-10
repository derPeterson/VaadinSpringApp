package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster.UserStatusMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Unit tests use real entities and mocked boundaries; no Spring context or database. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserStatusBroadcaster userStatusBroadcaster;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService =
        new UserService(
            userRepository,
            userStatusBroadcaster,
            passwordEncoder,
            mock(UserProfileBroadcaster.class),
            mock(de.derpeterson.app.security.AdminRolePolicy.class),
            mock(org.springframework.context.ApplicationEventPublisher.class));
  }

  @Test
  void activityPropagatesOtherDatabaseFailuresWithoutLoadingOrRetryingAnEntity() {
    DataAccessResourceFailureException failure = repositoryFailure();
    when(userRepository.updateLastActivity(eq(1L), any())).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class, () -> userService.updateLastActivity(1L)));
    verify(userRepository, times(1)).updateLastActivity(eq(1L), any());
    verify(userRepository, never()).findById(any());
    verifyNoInteractions(userStatusBroadcaster, passwordEncoder);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTC", "Europe/Berlin", "America/New_York"})
  void activityWritesTheInjectedInstantIndependentlyOfTheClocksZone(String zone) {
    Instant now = Instant.parse("2026-10-25T01:30:00Z");
    UserService timed =
        new UserService(
            userRepository,
            userStatusBroadcaster,
            passwordEncoder,
            mock(UserProfileBroadcaster.class),
            mock(de.derpeterson.app.security.AdminRolePolicy.class),
            mock(org.springframework.context.ApplicationEventPublisher.class),
            Clock.fixed(now, ZoneId.of(zone)));
    when(userRepository.updateLastActivity(1L, now)).thenReturn(1);
    timed.updateLastActivity(1L);

    verify(userRepository).updateLastActivity(1L, now);
    verify(userRepository, never()).save(any());
  }

  @Nested
  class Persistence {
    @ParameterizedTest(name = "saveUser alias: {0}")
    @ValueSource(booleans = {true, false})
    void bothSaveMethodsPersistTheSuppliedUser(boolean useSaveUser) {
      UserEntity testUserEntity = user(1L, true, RoleType.ROLE_USER);

      save(testUserEntity, useSaveUser);

      verify(userRepository).save(same(testUserEntity));
      verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
    }

    @ParameterizedTest(name = "saveUser alias: {0}")
    @ValueSource(booleans = {true, false})
    void bothSaveMethodsPropagatePersistenceFailures(boolean useSaveUser) {
      UserEntity testUserEntity = user(1L, true);
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.save(testUserEntity)).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class, () -> save(testUserEntity, useSaveUser)));
      verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
    }

    private void save(UserEntity testUserEntity, boolean useSaveUser) {
      if (useSaveUser) {
        userService.saveUser(testUserEntity);
      } else {
        userService.save(testUserEntity);
      }
    }

    @Test
    void findsAnExistingUserUsingTheSuppliedEmail() {
      UserEntity testUserEntity = user(1L, true);
      String email = " MixedCase@example.com ";
      when(userRepository.findByEmail("mixedcase@example.com"))
          .thenReturn(Optional.of(testUserEntity));

      assertSame(testUserEntity, userService.findByEmail(email).orElseThrow());
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
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findByEmail("test@example.com")).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
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
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findAll()).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(DataAccessResourceFailureException.class, userService::findAllUsers));
    }

    @Test
    void deletesTheSuppliedUser() {
      UserEntity testUserEntity = user(1L, true, RoleType.ROLE_USER);

      userService.deleteUser(testUserEntity);

      verify(userRepository).delete(same(testUserEntity));
      verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
    }

    @Test
    void propagatesDeletionFailure() {
      UserEntity testUserEntity = user(1L, true);
      DataAccessResourceFailureException failure = repositoryFailure();
      doThrow(failure).when(userRepository).delete(testUserEntity);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> userService.deleteUser(testUserEntity)));
      verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
    }
  }

  @Nested
  class MutationSuperadminProtection {
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
    void deletionRejectsTheLastStoredSuperadminEvenIfTheSuppliedUserLooksOrdinary() {
      UserEntity staleUser = user(1000L, false, RoleType.ROLE_USER);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1000L));

      assertThrows(IllegalStateException.class, () -> userService.deleteUser(staleUser));

      verify(userRepository, never()).delete(any());
      verifyNoInteractions(passwordEncoder, userStatusBroadcaster);
    }

    @Test
    void deletionRejectsTheLastEnabledSuperadminWithoutAPreliminaryUiCheck() {
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L));

      UserEntity targetUser = user(1L, true, RoleType.ROLE_SUPERADMIN);

      assertThrows(IllegalStateException.class, () -> userService.deleteUser(targetUser));

      verify(userRepository, never()).delete(any());
    }

    @Test
    void deletionAllowsAnAdminIfAnotherEnabledAdminRemains() {
      UserEntity adminUserEntity = user(1L, true, RoleType.ROLE_SUPERADMIN);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L, 2L));

      userService.deleteUser(adminUserEntity);

      InOrder order = inOrder(userRepository);
      order.verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
      order.verify(userRepository).delete(same(adminUserEntity));
    }

    @Test
    void deletionAllowsADisabledAdminWhenTheStoredEnabledAdminIsSomeoneElse() {
      UserEntity disabledAdmin = user(1L, false, RoleType.ROLE_SUPERADMIN);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(2L));

      userService.deleteUser(disabledAdmin);

      verify(userRepository).delete(same(disabledAdmin));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothSaveMethodsRejectDisablingTheLastStoredSuperadmin(boolean useSaveUser) {
      UserEntity edited = user(1L, false, RoleType.ROLE_SUPERADMIN);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L));

      assertThrows(IllegalStateException.class, () -> save(edited, useSaveUser));

      verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothSaveMethodsRejectRemovingTheLastSuperadminsRole(boolean useSaveUser) {
      UserEntity edited = user(1L, true, RoleType.ROLE_USER);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L));

      assertThrows(IllegalStateException.class, () -> save(edited, useSaveUser));

      verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothSaveMethodsAllowDisablingWhenAnotherAdminRemains(boolean useSaveUser) {
      UserEntity edited = user(1L, false, RoleType.ROLE_SUPERADMIN);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L, 2L));

      save(edited, useSaveUser);

      verify(userRepository).save(same(edited));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothSaveMethodsAllowRoleRemovalWhenAnotherAdminRemains(boolean useSaveUser) {
      UserEntity edited = user(1L, true, RoleType.ROLE_USER);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L, 2L));

      save(edited, useSaveUser);

      verify(userRepository).save(same(edited));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothSaveMethodsAllowUnchangedAdminPrivileges(boolean useSaveUser) {
      UserEntity adminUserEntity = user(1L, true, RoleType.ROLE_SUPERADMIN);

      save(adminUserEntity, useSaveUser);

      verify(userRepository).save(same(adminUserEntity));
      verify(userRepository, atLeastOnce()).findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void registrationStillWorksBeforeTheFirstAdminIsCreated(boolean useSaveUser) {
      UserEntity newUser = user(null, false, RoleType.ROLE_USER);

      save(newUser, useSaveUser);

      verify(userRepository).save(same(newUser));
      verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ordinaryAccountUpdatesDoNotDependOnBeingAnAdmin(boolean useSaveUser) {
      UserEntity ordinary = user(1L, true, RoleType.ROLE_USER);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(2L));

      save(ordinary, useSaveUser);

      verify(userRepository).save(same(ordinary));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void failedAdminLookupPreventsSavingAndPropagatesTheOriginalException(boolean useSaveUser) {
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN)).thenThrow(failure);

      UserEntity targetUser = user(1L, false, RoleType.ROLE_SUPERADMIN);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class, () -> save(targetUser, useSaveUser)));

      verify(userRepository, never()).save(any());
    }

    @Test
    void failedAdminLookupPreventsDeletingAndPropagatesTheOriginalException() {
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN)).thenThrow(failure);

      UserEntity targetUser = user(1L, true, RoleType.ROLE_SUPERADMIN);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class, () -> userService.deleteUser(targetUser)));

      verify(userRepository, never()).delete(any());
    }

    @Test
    void statusUpdateIgnoresDemotionInTheSuppliedSnapshot() {
      UserEntity storedUserEntity = user(1L, true, RoleType.ROLE_SUPERADMIN);
      when(userRepository.findById(1L)).thenReturn(Optional.of(storedUserEntity));
      TransactionSynchronizationManager.initSynchronization();
      try {
        userService.updateUserStatus(
            user(1L, false, RoleType.ROLE_SUPERADMIN), UserStatus.AVAILABLE, true);

        assertTrue(storedUserEntity.isEnabled());
        verify(userRepository).save(same(storedUserEntity));
        verify(userRepository, never()).findEnabledUserIdsByRole(any());
        verifyNoInteractions(userStatusBroadcaster);
      } finally {
        TransactionSynchronizationManager.clearSynchronization();
      }
    }

    private void save(UserEntity testUserEntity, boolean useSaveUser) {
      if (useSaveUser) {
        userService.saveUser(testUserEntity);
      } else {
        userService.save(testUserEntity);
      }
    }
  }

  @Nested
  class LocaleUpdates {
    @Test
    void savesAnExistingUsersNewLocale() {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setPreferredLocale(Locale.ENGLISH);
      when(userRepository.findByEmail(testUserEntity.getEmail()))
          .thenReturn(Optional.of(testUserEntity));

      userService.updateUserLocale(testUserEntity.getEmail(), Locale.FRENCH);

      assertEquals(Locale.FRENCH, testUserEntity.getPreferredLocale());
      verify(userRepository).save(same(testUserEntity));
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
      UserEntity testUserEntity = user(1L, true);
      String accountEmail = testUserEntity.getEmail();

      assertThrows(
          IllegalArgumentException.class, () -> userService.updateUserLocale(accountEmail, null));
      assertEquals(Locale.ENGLISH, testUserEntity.getPreferredLocale());
      verifyNoInteractions(userRepository);
    }

    @Test
    void acceptsTheRootLocale() {
      UserEntity testUserEntity = user(1L, true);
      when(userRepository.findByEmail(testUserEntity.getEmail()))
          .thenReturn(Optional.of(testUserEntity));

      userService.updateUserLocale(testUserEntity.getEmail(), Locale.ROOT);

      assertEquals(Locale.ROOT, testUserEntity.getPreferredLocale());
      verify(userRepository).save(same(testUserEntity));
    }

    @Test
    void doesNotSaveWhenLocaleLookupFails() {
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findByEmail("test@example.com")).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> userService.updateUserLocale("test@example.com", Locale.GERMAN)));
      verify(userRepository, never()).save(any());
    }

    @Test
    void propagatesLocalePersistenceFailure() {
      UserEntity testUserEntity = user(1L, true);
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findByEmail(testUserEntity.getEmail()))
          .thenReturn(Optional.of(testUserEntity));
      when(userRepository.save(testUserEntity)).thenThrow(failure);

      String accountEmail = testUserEntity.getEmail();

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> userService.updateUserLocale(accountEmail, Locale.GERMAN)));
    }
  }

  @Nested
  class PasswordUpdates {
    @ParameterizedTest
    @ValueSource(strings = {"NewPassword!", "  Password with spaces!  ", "A!234567"})
    void encodesTheUnmodifiedInputWithoutSavingTheUser(String rawPassword) {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setPassword("old-hash");
      when(passwordEncoder.encode(rawPassword)).thenReturn("new-hash");

      userService.updatePassword(testUserEntity, rawPassword);

      assertEquals("new-hash", testUserEntity.getPassword());
      verify(passwordEncoder).encode(rawPassword);
      verifyNoInteractions(userRepository, userStatusBroadcaster);
    }

    @Test
    void rejectsANullUser() {
      assertThrows(
          IllegalArgumentException.class, () -> userService.updatePassword(null, "newRawPassword"));
      verifyNoInteractions(userRepository, userStatusBroadcaster);
    }

    @Test
    void leavesTheExistingPasswordUnchangedWhenEncodingFails() {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setPassword("old-hash");
      IllegalStateException failure = new IllegalStateException("Encoding failed");
      when(passwordEncoder.encode("NewPassword!")).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              IllegalStateException.class,
              () -> userService.updatePassword(testUserEntity, "NewPassword!")));
      assertEquals("old-hash", testUserEntity.getPassword());
      verifyNoInteractions(userRepository, userStatusBroadcaster);
    }

    @Test
    void rejectsNullPasswordWithoutEncodingOrChangingTheUser() {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setPassword("old-hash");

      assertThrows(
          IllegalArgumentException.class, () -> userService.updatePassword(testUserEntity, null));
      assertEquals("old-hash", testUserEntity.getPassword());
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
      UserEntity storedUserEntity = UserServiceTest.user(id, enabled);
      when(userRepository.findById(id)).thenReturn(Optional.of(storedUserEntity));
      return storedUserEntity;
    }

    private void commit() {
      verifyNoInteractions(userStatusBroadcaster);
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void manualChangeSavesStatusAndFlagBeforeBroadcasting() {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setStatus(UserStatus.AVAILABLE);
      doAnswer(
              invocation -> {
                assertEquals(UserStatus.EMPLOYED, testUserEntity.getStatus());
                assertTrue(testUserEntity.isStatusManuallySet());
                return testUserEntity;
              })
          .when(userRepository)
          .save(testUserEntity);

      userService.updateUserStatus(testUserEntity, UserStatus.EMPLOYED, true);
      commit();

      InOrder order = inOrder(userRepository, userStatusBroadcaster);
      order.verify(userRepository).save(same(testUserEntity));
      order
          .verify(userStatusBroadcaster)
          .broadcast(message(testUserEntity, UserStatus.AVAILABLE, UserStatus.EMPLOYED));

      verifyNoInteractions(passwordEncoder);
    }

    @Test
    void automaticChangeSavesNewStatusWithoutManualFlag() {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setStatus(UserStatus.AVAILABLE);

      userService.updateUserStatus(testUserEntity, UserStatus.ABSENT, false);
      commit();

      assertEquals(UserStatus.ABSENT, testUserEntity.getStatus());
      assertFalse(testUserEntity.isStatusManuallySet());
      verify(userRepository).save(same(testUserEntity));
      verify(userStatusBroadcaster)
          .broadcast(message(testUserEntity, UserStatus.AVAILABLE, UserStatus.ABSENT));
    }

    @ParameterizedTest
    @EnumSource(
        value = UserStatus.class,
        names = {"AVAILABLE", "ABSENT"})
    void automaticChangeResetsManualAvailableOrAbsentStatus(UserStatus previousStatus) {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setManualStatus(previousStatus);

      userService.updateUserStatus(testUserEntity, UserStatus.OFFLINE, false);
      commit();

      assertEquals(UserStatus.OFFLINE, testUserEntity.getStatus());
      assertFalse(testUserEntity.isStatusManuallySet());
      verify(userRepository).save(same(testUserEntity));
      verify(userStatusBroadcaster)
          .broadcast(message(testUserEntity, previousStatus, UserStatus.OFFLINE));
    }

    @ParameterizedTest
    @EnumSource(
        value = UserStatus.class,
        names = {"EMPLOYED", "OFFLINE"})
    void protectedManualStatusIsRetainedWithoutBroadcastingARejectedChange(
        UserStatus previousStatus) {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setManualStatus(previousStatus);

      userService.updateUserStatus(testUserEntity, UserStatus.ABSENT, false);

      assertEquals(previousStatus, testUserEntity.getStatus());
      assertTrue(testUserEntity.isStatusManuallySet());
      verify(userRepository).save(same(testUserEntity));
      verifyNoInteractions(userStatusBroadcaster);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sameStatusRequestPersistsTheManualFlagWithoutBroadcasting(boolean manualChange) {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setStatus(UserStatus.AVAILABLE);
      testUserEntity.setStatusManuallySet(!manualChange);
      doAnswer(
              invocation -> {
                assertEquals(UserStatus.AVAILABLE, testUserEntity.getStatus());
                assertEquals(manualChange, testUserEntity.isStatusManuallySet());
                return testUserEntity;
              })
          .when(userRepository)
          .save(testUserEntity);

      userService.updateUserStatus(testUserEntity, UserStatus.AVAILABLE, manualChange);

      assertEquals(UserStatus.AVAILABLE, testUserEntity.getStatus());
      assertEquals(manualChange, testUserEntity.isStatusManuallySet());
      verify(userRepository).save(same(testUserEntity));
      verifyNoInteractions(userStatusBroadcaster);
    }

    @ParameterizedTest
    @EnumSource(
        value = UserStatus.class,
        names = {"EMPLOYED", "OFFLINE"})
    void rejectedAutomaticChangeStillPropagatesPersistenceFailure(UserStatus previousStatus) {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setManualStatus(previousStatus);
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.save(testUserEntity)).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> userService.updateUserStatus(testUserEntity, UserStatus.ABSENT, false)));
      assertEquals(previousStatus, testUserEntity.getStatus());
      assertTrue(testUserEntity.isStatusManuallySet());
      verify(userRepository).save(same(testUserEntity));
      verifyNoInteractions(userStatusBroadcaster);
    }

    @Test
    void manualChangeCanReplaceAProtectedManualStatus() {
      UserEntity testUserEntity = user(1L, true);
      testUserEntity.setManualStatus(UserStatus.EMPLOYED);

      userService.updateUserStatus(testUserEntity, UserStatus.AVAILABLE, true);
      commit();

      assertEquals(UserStatus.AVAILABLE, testUserEntity.getStatus());
      assertTrue(testUserEntity.isStatusManuallySet());
      verify(userRepository).save(same(testUserEntity));
      verify(userStatusBroadcaster)
          .broadcast(message(testUserEntity, UserStatus.EMPLOYED, UserStatus.AVAILABLE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void doesNotBroadcastWhenSavingFails(boolean manualChange) {
      UserEntity testUserEntity = user(1L, true);
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.save(testUserEntity)).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () ->
                  userService.updateUserStatus(
                      testUserEntity, UserStatus.AVAILABLE, manualChange)));
      verifyNoInteractions(userStatusBroadcaster);
    }

    @Test
    void isolatesBroadcastFailureAfterCommit() {
      UserEntity testUserEntity = user(1L, true);
      UserStatusMessage event = message(testUserEntity, UserStatus.OFFLINE, UserStatus.AVAILABLE);
      IllegalStateException failure = new IllegalStateException("Listener failed");
      doThrow(failure).when(userStatusBroadcaster).broadcast(event);

      userService.updateUserStatus(testUserEntity, UserStatus.AVAILABLE, true);

      assertDoesNotThrow(this::commit);
      assertEquals(UserStatus.AVAILABLE, testUserEntity.getStatus());
      verify(userRepository).save(same(testUserEntity));
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
      UserEntity storedUserEntity = user(1000L, true);
      when(userRepository.emailExistsForOtherUser(storedUserEntity.getEmail(), 1000L))
          .thenReturn(false);

      assertFalse(
          userService.emailExistsForOtherUser(storedUserEntity.getEmail(), Long.valueOf("1000")));
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
      when(userRepository.emailExistsForOtherUser(current.getEmail(), current.getId()))
          .thenReturn(true);

      assertTrue(userService.emailExistsForOtherUser(current.getEmail(), current.getId()));
    }

    @Test
    void propagatesUniquenessLookupFailure() {
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.emailExistsForOtherUser("test@example.com", 1L)).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> userService.emailExistsForOtherUser("test@example.com", 1L)));
    }
  }

  @Nested
  class SuperadminProtection {
    @Test
    void cannotDeleteANullUser() {
      assertFalse(userService.canDeleteUser(null));
      verifyNoInteractions(userRepository);
    }

    @Test
    void canDeleteAnOrdinaryUserBasedOnStoredPrivileges() {
      assertTrue(userService.canDeleteUser(user(1L, true, RoleType.ROLE_USER)));
      verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
    }

    @Test
    void canDeleteUsersWithEmptyOrMissingRoles() {
      UserEntity withoutRoles = user(1L, true);

      assertTrue(userService.canDeleteUser(withoutRoles));
      withoutRoles.setRoleEntities(null);

      assertTrue(userService.canDeleteUser(withoutRoles));
      verify(userRepository, times(2)).findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
    }

    @Test
    void canDeleteADisabledAdminBasedOnStoredPrivileges() {
      assertTrue(userService.canDeleteUser(user(1L, false, RoleType.ROLE_SUPERADMIN)));
      verify(userRepository).findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
    }

    @Test
    void cannotDeleteAnEnabledAdminWhenNoOtherAccountsExist() {
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L));

      assertFalse(userService.canDeleteUser(user(1L, true, RoleType.ROLE_SUPERADMIN)));
    }

    @Test
    void cannotDeleteLastEnabledSuperadminAndDoesNotCountSameIdOrIneligibleAccounts() {
      UserEntity targetUserEntity = user(1000L, true, RoleType.ROLE_SUPERADMIN);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(Long.valueOf("1000")));

      assertFalse(userService.canDeleteUser(targetUserEntity));
      verify(userRepository, never()).delete(any());
    }

    @Test
    void canDeleteAdminWhenAnotherEnabledAdminRemains() {
      UserEntity targetUserEntity = user(1L, true, RoleType.ROLE_SUPERADMIN);
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L, 2L));

      assertTrue(userService.canDeleteUser(targetUserEntity));
      verify(userRepository, never()).delete(any());
    }

    @Test
    void retainingEnabledAdminRoleNeedsNoOtherAdmin() {
      assertFalse(
          userService.wouldRemoveLastEnabledSuperadmin(
              1L, true, List.of(role(RoleType.ROLE_USER), role(RoleType.ROLE_SUPERADMIN))));
      verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void removalOfRoleOrDisablingTheLastSuperadminIsBlocked(boolean remainsEnabled) {
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L));
      List<RoleEntity> proposedRoles =
          remainsEnabled ? List.of() : List.of(role(RoleType.ROLE_SUPERADMIN));

      assertTrue(userService.wouldRemoveLastEnabledSuperadmin(1L, remainsEnabled, proposedRoles));
    }

    @Test
    void missingProposedRolesAreRejectedBeforeTheAdminQuery() {
      assertThrows(
          IllegalArgumentException.class,
          () -> userService.wouldRemoveLastEnabledSuperadmin(1L, true, null));
      verifyNoInteractions(userRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void roleRemovalOrDisablingIsAllowedWhenAnotherAdminRemains(boolean remainsEnabled) {
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L, 2L));
      List<RoleEntity> proposedRoles =
          remainsEnabled ? List.of() : List.of(role(RoleType.ROLE_SUPERADMIN));

      assertFalse(userService.wouldRemoveLastEnabledSuperadmin(1L, remainsEnabled, proposedRoles));
    }

    @Test
    void disabledAdminsAndOrdinaryUsersDoNotPreventLastSuperadminRemoval() {
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN))
          .thenReturn(List.of(1L));

      assertTrue(
          userService.wouldRemoveLastEnabledSuperadmin(
              1L, true, List.of(role(RoleType.ROLE_USER))));
    }

    @Test
    void newNonAdminAccountIsAllowedWhenAnEnabledAdminExists() {
      assertFalse(
          userService.wouldRemoveLastEnabledSuperadmin(
              null, true, List.of(role(RoleType.ROLE_USER))));
    }

    @Test
    void nonAdminCreationIsAllowedEvenBeforeTheFirstEnabledAdmin() {
      assertFalse(
          userService.wouldRemoveLastEnabledSuperadmin(
              null, true, List.of(role(RoleType.ROLE_USER))));
      verifyNoInteractions(userRepository);
    }

    @Test
    void propagatesAdminLookupFailureRatherThanGrantingDeletion() {
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN)).thenThrow(failure);

      UserEntity targetUser = user(1L, true, RoleType.ROLE_SUPERADMIN);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> userService.canDeleteUser(targetUser)));
      verify(userRepository, never()).delete(any());
    }

    @Test
    void propagatesAdminLookupFailureRatherThanAllowingRemoval() {
      DataAccessResourceFailureException failure = repositoryFailure();
      when(userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN)).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> userService.wouldRemoveLastEnabledSuperadmin(1L, false, List.of())));
      verify(userRepository, never()).save(any());
    }
  }

  private static UserEntity user(Long id, boolean enabled, RoleType... roles) {
    return UserEntity.builder()
        .firstName("Test")
        .lastName("User")
        .password("hash")
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

  private static UserStatusBroadcaster.UserStatusMessage message(
      UserEntity testUserEntity, UserStatus oldStatus, UserStatus requestedStatus) {
    return new UserStatusBroadcaster.UserStatusMessage(
        testUserEntity.getId(), oldStatus.name(), requestedStatus.name());
  }

  private static DataAccessResourceFailureException repositoryFailure() {
    return new DataAccessResourceFailureException("Repository unavailable");
  }
}
