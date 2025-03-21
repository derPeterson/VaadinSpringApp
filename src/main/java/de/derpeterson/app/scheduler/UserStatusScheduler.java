package de.derpeterson.app.scheduler;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class UserStatusScheduler {

    private final UserRepository userRepository;
    private final UserService userService;
    private final ConfigService configService;

    @Scheduled(fixedRate = 10000)
    public void checkInactiveAvailableUsers() {
        LocalDateTime timeout = LocalDateTime.now().minus(Duration.parse(configService.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)));

        List<UserEntity> inactiveAvailableUsers = userRepository
                .findByLastActivityBeforeAndStatus(timeout, UserStatus.AVAILABLE);

        if (!inactiveAvailableUsers.isEmpty()) {
            for (UserEntity user : inactiveAvailableUsers) {
                userService.updateUserStatus(user, UserStatus.ABSENT, false);
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    public void checkRecentlyActiveAbsentUsers() {
        LocalDateTime recentActivity = LocalDateTime.now().minusSeconds(5);

        List<UserEntity> activeAbsentUsers = userRepository
                .findByLastActivityAfterAndStatusAndStatusManuallySetFalse(recentActivity, UserStatus.ABSENT);

        if (!activeAbsentUsers.isEmpty()) {
            for (UserEntity user : activeAbsentUsers) {
                userService.updateUserStatus(user, UserStatus.AVAILABLE, false);
            }
        }
    }
}
