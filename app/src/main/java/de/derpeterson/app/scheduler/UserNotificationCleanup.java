package de.derpeterson.app.scheduler;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.service.UserNotificationPolicy;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserNotificationCleanup {
  private final UserNotificationRepository userNotificationRepository;
  private final UserNotificationPolicy userNotificationPolicy;

  @Scheduled(cron = "0 20 3 * * *")
  @Transactional
  public int purgeRetiredNotifications() {
    Instant cleanupTime = Instant.now();
    // Inhalte erst löschen, wenn sie weder in einer Inbox noch in der Historie gebraucht werden.
    int deletedNotificationCount =
        userNotificationRepository.deleteRetired(
            userNotificationPolicy.inboxCutoff(cleanupTime),
            userNotificationPolicy.historyCutoff(cleanupTime));

    if (deletedNotificationCount > 0) {
      AfterCommitLog.info(log, "Retired notifications deleted: count={}", deletedNotificationCount);
    }

    return deletedNotificationCount;
  }
}
