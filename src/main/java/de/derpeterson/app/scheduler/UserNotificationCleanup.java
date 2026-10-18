package de.derpeterson.app.scheduler;

import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.service.UserNotificationPolicy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserNotificationCleanup {
  private final UserNotificationRepository notifications;
  private final UserNotificationPolicy policy;

  @Scheduled(cron = "0 20 3 * * *")
  @Transactional
  public int cleanup() {
    return notifications.deleteRetired(
        Instant.now().minus(policy.retentionDays(), ChronoUnit.DAYS));
  }
}
