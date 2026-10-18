package de.derpeterson.app.scheduler;

import de.derpeterson.app.repository.UserMessageRepository;
import de.derpeterson.app.service.UserMessagePolicy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserMessageCleanup {
  private final UserMessageRepository messages;
  private final UserMessagePolicy policy;

  @Scheduled(cron = "0 20 3 * * *")
  @Transactional
  public int cleanup() {
    return messages.deleteRetired(Instant.now().minus(policy.retentionDays(), ChronoUnit.DAYS));
  }
}
