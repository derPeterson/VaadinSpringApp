package de.derpeterson.app.scheduler;

import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.repository.SupportTicketRepository;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TwoFactorCleanup {
  private final TwoFactorChallengeRepository challenges;
  private final SupportTicketRepository tickets;

  @Scheduled(fixedDelay = 60_000)
  @Transactional
  public void cleanup() {
    Instant now = Instant.now();
    challenges.clearExpiredSecrets(now);
    // Metadaten kurz für das kontoweite Mail-Versuchslimit erhalten, niemals Ausgabewerte.
    challenges.deleteExpired(now.minusSeconds(3600));
    tickets.deleteClosedBefore(now.minusSeconds(180L * 86400), Status.CLOSED);
  }
}
