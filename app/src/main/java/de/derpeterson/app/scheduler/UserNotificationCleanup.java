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
  private final UserNotificationRepository notifications;
  private final UserNotificationPolicy policy;

  @Scheduled(cron = "0 20 3 * * *")
  @Transactional
  public int cleanup() {
    Instant now = Instant.now();
    // Inhalte erst löschen, wenn sie weder in einer Inbox noch in der Historie gebraucht werden.
    int deleted = notifications.deleteRetired(policy.inboxCutoff(now), policy.historyCutoff(now));
    if (deleted > 0) {
      AfterCommitLog.info(log, "Retired notifications deleted: count={}", deleted);
    }
    return deleted;
  }
}
