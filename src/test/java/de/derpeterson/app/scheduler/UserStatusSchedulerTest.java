package de.derpeterson.app.scheduler;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserStatusSchedulerTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void conflictAtOneCandidateDoesNotPreventTheNextCandidate(boolean toAbsent) {
        var repository = mock(UserRepository.class);
        var service = mock(UserService.class);
        var config = mock(ConfigService.class);
        var first = UserEntity.builder().id(1L).build();
        var second = UserEntity.builder().id(2L).build();
        if (toAbsent) {
            when(config.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
            when(repository.findByLastActivityBeforeAndStatus(any(), eq(UserStatus.AVAILABLE))).thenReturn(List.of(first, second));
        } else {
            when(repository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(any(), eq(UserStatus.ABSENT))).thenReturn(List.of(first, second));
        }
        UserStatus target = toAbsent ? UserStatus.ABSENT : UserStatus.AVAILABLE;
        doThrow(new OptimisticLockingFailureException("candidate conflict")).when(service).updateScheduledStatus(eq(1L), eq(target), any());
        var scheduler = new UserStatusScheduler(repository, service, config);
        assertDoesNotThrow(() -> scan(scheduler, toAbsent));
        verify(service).updateScheduledStatus(eq(2L), eq(target), any());

        reset(service);
        var databaseFailure = new DataAccessResourceFailureException("database unavailable");
        doThrow(databaseFailure).when(service).updateScheduledStatus(eq(1L), eq(target), any());
        assertSame(databaseFailure, assertThrows(DataAccessResourceFailureException.class, () -> scan(scheduler, toAbsent)));
        verify(service, never()).updateScheduledStatus(eq(2L), eq(target), any());
    }

    private void scan(UserStatusScheduler scheduler, boolean toAbsent) {
        if (toAbsent) {
            scheduler.checkInactiveAvailableUsers();
        } else {
            scheduler.checkRecentlyActiveAbsentUsers();
        }
    }
}
