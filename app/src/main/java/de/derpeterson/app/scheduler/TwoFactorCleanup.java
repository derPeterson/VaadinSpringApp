package de.derpeterson.app.scheduler;

import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.repository.SupportTicketRepository;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.session.DurableSessionRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TwoFactorCleanup {
  private final TwoFactorChallengeRepository twoFactorChallengeRepository;
  private final SupportTicketRepository supportTicketRepository;
  private final JdbcOperations sessionJdbcOperations;

  @Scheduled(fixedDelay = 60_000)
  @Transactional
  public void purgeExpiredRecoveryData() {
    Instant cleanupTime = Instant.now();
    // Derselbe Sessionbestand wie bei der Anmeldung; Klartext oder rohe IDs werden nicht
    // gespeichert.
    List<String> activeSessionDigests =
        sessionJdbcOperations
            .queryForList(
                "select s.SESSION_ID from SPRING_SESSION s where s.EXPIRY_TIME > ? and not exists (select 1 from SPRING_SESSION_ATTRIBUTES a where a.SESSION_PRIMARY_ID = s.PRIMARY_ID and a.ATTRIBUTE_NAME = ?)",
                String.class,
                cleanupTime.toEpochMilli(),
                DurableSessionRepository.EXPIRED)
            .stream()
            .map(TwoFactorAuthorization::sessionDigest)
            .toList();

    twoFactorChallengeRepository.deleteClosedRecoveryOutputs(activeSessionDigests, cleanupTime);
    twoFactorChallengeRepository.clearExpiredSecrets(cleanupTime);
    // Metadaten kurz für das kontoweite Mail-Versuchslimit erhalten, niemals Ausgabewerte.
    twoFactorChallengeRepository.deleteExpired(cleanupTime.minusSeconds(3600));

    supportTicketRepository.deleteClosedBefore(
        cleanupTime.minusSeconds(180L * 86400), List.of(Status.CLOSED, Status.CLOSED_BY_REQUESTER));
  }
}
