package de.derpeterson.app.scheduler;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserStatusScheduler {

    private final UserRepository userRepository;

    public UserStatusScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void checkInactiveUsers() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10);

        List<UserEntity> inactiveUsers = userRepository.findByLastActivityBeforeAndStatusNot(
                timeoutThreshold, UserStatus.ABSENT);

        for (UserEntity user : inactiveUsers) {
            user.setAutomaticStatus(UserStatus.ABSENT);
            userRepository.save(user);
        }
    }
}
