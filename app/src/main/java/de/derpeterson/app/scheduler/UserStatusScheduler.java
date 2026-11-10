package de.derpeterson.app.scheduler;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserStatusScheduler {

  private final UserRepository userRepository;
  private final UserService userService;
  private final ConfigService configService;
  private final Clock statusUpdateClock;

  @Autowired
  public UserStatusScheduler(
      UserRepository userRepository, UserService userService, ConfigService configService) {
    this(userRepository, userService, configService, Clock.systemUTC());
  }

  public UserStatusScheduler(
      UserRepository userRepository,
      UserService userService,
      ConfigService configService,
      Clock statusUpdateClock) {
    this.userRepository = userRepository;
    this.userService = userService;
    this.configService = configService;
    this.statusUpdateClock = Objects.requireNonNull(statusUpdateClock);
  }

  @Scheduled(fixedRate = 10000)
  public void checkInactiveAvailableUsers() {
    Instant inactivityCutoff =
        statusUpdateClock
            .instant()
            .minus(Duration.parse(configService.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)));

    List<UserEntity> inactiveAvailableUsers =
        userRepository.findByLastActivityBeforeAndStatus(inactivityCutoff, UserStatus.AVAILABLE);

    if (!inactiveAvailableUsers.isEmpty()) {
      for (UserEntity candidateUserEntity : inactiveAvailableUsers) {
        updateCandidateStatus(candidateUserEntity, UserStatus.ABSENT, inactivityCutoff);
      }
    }
  }

  @Scheduled(fixedRate = 1000)
  public void checkRecentlyActiveAbsentUsers() {
    Instant recentActivityCutoff = statusUpdateClock.instant().minusSeconds(5);

    List<UserEntity> activeAbsentUsers =
        userRepository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
            recentActivityCutoff, UserStatus.ABSENT);

    if (!activeAbsentUsers.isEmpty()) {
      for (UserEntity candidateUserEntity : activeAbsentUsers) {
        updateCandidateStatus(candidateUserEntity, UserStatus.AVAILABLE, recentActivityCutoff);
      }
    }
  }

  private void updateCandidateStatus(
      UserEntity candidateUserEntity, UserStatus targetUserStatus, Instant activityCutoff) {
    try {
      userService.updateScheduledStatus(
          candidateUserEntity.getId(), targetUserStatus, activityCutoff);
    } catch (ConcurrencyFailureException concurrentStatusUpdateFailure) {
      // The next scan reselects this candidate; other users still get processed.
      log.warn(
          "Automatischer Statuskonflikt für Benutzer {}",
          candidateUserEntity.getId(),
          safeFailure(concurrentStatusUpdateFailure));
    }
  }
}
