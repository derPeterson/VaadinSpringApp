package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.SupportReplyEntity;
import de.derpeterson.app.model.SupportTicketEntity;
import de.derpeterson.app.model.SupportTicketEntity.Status;
import de.derpeterson.app.model.TwoFactorChallengeEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.SupportReplyRepository;
import de.derpeterson.app.repository.SupportTicketRepository;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.scheduler.TwoFactorCleanup;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(TwoFactorStoreTest.Config.class)
class TwoFactorCleanupTest {
  @Autowired UserRepository users;
  @Autowired TwoFactorChallengeRepository challenges;
  @Autowired SupportTicketRepository tickets;
  @Autowired SupportReplyRepository replies;
  @Autowired TransactionTemplate transaction;

  @Test
  void clearsExpiredSecretsBeforeMetadataAndDeletesOnlyOldClosedConversations() {
    Instant now = Instant.now();
    UserEntity user =
        users.saveAndFlush(
            UserEntity.builder()
                .firstName("Cleanup")
                .lastName("Fixture")
                .email(UUID.randomUUID() + "@example.test")
                .enabled(true)
                .roleEntities(List.of())
                .build());
    TwoFactorChallengeEntity expired = challenge(user, now.minusSeconds(60));
    TwoFactorChallengeEntity old = challenge(user, now.minusSeconds(7200));
    TwoFactorChallengeEntity active = challenge(user, now.plusSeconds(300));
    SupportTicketEntity oldClosed = ticket(user, now.minus(181, ChronoUnit.DAYS), Status.CLOSED);
    SupportTicketEntity oldOpen = ticket(user, now.minus(181, ChronoUnit.DAYS), Status.OPEN);
    SupportTicketEntity recentClosed = ticket(user, now.minus(1, ChronoUnit.DAYS), Status.CLOSED);
    SupportReplyEntity reply = new SupportReplyEntity();
    reply.setTicket(oldClosed);
    reply.setAuthor(user);
    reply.setBody("Synthetic closed conversation");
    reply.setCreatedAt(oldClosed.getCreatedAt());
    replies.saveAndFlush(reply);

    transaction.executeWithoutResult(status -> new TwoFactorCleanup(challenges, tickets).cleanup());

    TwoFactorChallengeEntity cleared = challenges.findById(expired.getId()).orElseThrow();
    assertTrue(cleared.isConsumed());
    assertNull(cleared.getEncryptedPayload());
    assertNull(cleared.getVerificationDigest());
    assertFalse(challenges.existsById(old.getId()));
    assertEquals(
        "synthetic encrypted payload",
        challenges.findById(active.getId()).orElseThrow().getEncryptedPayload());
    assertFalse(tickets.existsById(oldClosed.getId()));
    assertFalse(replies.existsById(reply.getId()));
    assertTrue(tickets.existsById(oldOpen.getId()));
    assertTrue(tickets.existsById(recentClosed.getId()));
  }

  private TwoFactorChallengeEntity challenge(UserEntity user, Instant expiry) {
    TwoFactorChallengeEntity row = new TwoFactorChallengeEntity();
    row.setUser(user);
    row.setSessionDigest("synthetic-session");
    row.setAccessVersion(user.getAccessVersion());
    row.setFactorRevision(UUID.randomUUID());
    row.setAction(TwoFactorAction.MANAGE_FACTOR);
    row.setKind(Kind.ENROLLMENT);
    row.setCreatedAt(expiry.minusSeconds(300));
    row.setExpiresAt(expiry);
    row.setEncryptedPayload("synthetic encrypted payload");
    row.setVerificationDigest("synthetic digest");
    return challenges.saveAndFlush(row);
  }

  private SupportTicketEntity ticket(UserEntity user, Instant updated, Status status) {
    SupportTicketEntity row = new SupportTicketEntity();
    row.setOwner(user);
    row.setSubject("Synthetic recovery request");
    row.setStatus(status);
    row.setCreatedAt(updated);
    row.setUpdatedAt(updated);
    return tickets.saveAndFlush(row);
  }
}
