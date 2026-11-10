package de.derpeterson.app.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;

class UserStatusSchedulerTest {
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void conflictAtOneCandidateDoesNotPreventTheNextCandidate(boolean toAbsent) {
    UserRepository userRepository = mock(UserRepository.class);
    UserService userService = mock(UserService.class);
    ConfigService configService = mock(ConfigService.class);
    UserEntity firstUserEntity = UserEntity.builder().id(1L).build();
    UserEntity secondUserEntity = UserEntity.builder().id(2L).build();
    if (toAbsent) {
      when(configService.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
      when(userRepository.findByLastActivityBeforeAndStatus(any(), eq(UserStatus.AVAILABLE)))
          .thenReturn(List.of(firstUserEntity, secondUserEntity));
    } else {
      when(userRepository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
              any(), eq(UserStatus.ABSENT)))
          .thenReturn(List.of(firstUserEntity, secondUserEntity));
    }
    UserStatus target = toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE;
    doThrow(new OptimisticLockingFailureException("candidate conflict"))
        .when(userService)
        .updateScheduledStatus(eq(1L), eq(target), any());
    UserStatusScheduler scheduler =
        new UserStatusScheduler(userRepository, userService, configService);

    assertDoesNotThrow(() -> scan(scheduler, toAbsent));
    verify(userService).updateScheduledStatus(eq(2L), eq(target), any());

    reset(userService);
    DataAccessResourceFailureException databaseFailure =
        new DataAccessResourceFailureException("database unavailable");
    doThrow(databaseFailure).when(userService).updateScheduledStatus(eq(1L), eq(target), any());

    assertSame(
        databaseFailure,
        assertThrows(DataAccessResourceFailureException.class, () -> scan(scheduler, toAbsent)));
    verify(userService, never()).updateScheduledStatus(eq(2L), eq(target), any());
  }

  private void scan(UserStatusScheduler scheduler, boolean toAbsent) {
    if (toAbsent) {
      scheduler.checkInactiveAvailableUsers();
    } else {
      scheduler.checkRecentlyActiveAbsentUsers();
    }
  }
}
