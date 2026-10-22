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
  private final Clock clock;

  @Autowired
  public UserStatusScheduler(
      UserRepository userRepository, UserService userService, ConfigService configService) {
    this(userRepository, userService, configService, Clock.systemUTC());
  }

  public UserStatusScheduler(
      UserRepository userRepository,
      UserService userService,
      ConfigService configService,
      Clock clock) {
    this.userRepository = userRepository;
    this.userService = userService;
    this.configService = configService;
    this.clock = Objects.requireNonNull(clock);
  }

  @Scheduled(fixedRate = 10000)
  public void checkInactiveAvailableUsers() {
    Instant timeout =
        clock
            .instant()
            .minus(Duration.parse(configService.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)));

    List<UserEntity> inactiveAvailableUsers =
        userRepository.findByLastActivityBeforeAndStatus(timeout, UserStatus.AVAILABLE);

    if (!inactiveAvailableUsers.isEmpty()) {
      for (UserEntity user : inactiveAvailableUsers) {
        updateCandidate(user, UserStatus.ABSENT, timeout);
      }
    }
  }

  @Scheduled(fixedRate = 1000)
  public void checkRecentlyActiveAbsentUsers() {
    Instant recentActivity = clock.instant().minusSeconds(5);

    List<UserEntity> activeAbsentUsers =
        userRepository.findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
            recentActivity, UserStatus.ABSENT);

    if (!activeAbsentUsers.isEmpty()) {
      for (UserEntity user : activeAbsentUsers) {
        updateCandidate(user, UserStatus.AVAILABLE, recentActivity);
      }
    }
  }

  private void updateCandidate(UserEntity user, UserStatus target, Instant cutoff) {
    try {
      userService.updateScheduledStatus(user.getId(), target, cutoff);
    } catch (ConcurrencyFailureException exception) {
      // The next scan reselects this candidate; other users still get processed.
      log.warn(
          "Automatischer Statuskonflikt für Benutzer {}", user.getId(), safeFailure(exception));
    }
  }
}
