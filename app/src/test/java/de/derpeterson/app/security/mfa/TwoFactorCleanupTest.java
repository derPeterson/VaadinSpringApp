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
import de.derpeterson.app.security.session.DurableSessionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(TwoFactorStoreTest.Config.class)
class TwoFactorCleanupTest {
  @Autowired UserRepository users;
  @Autowired TwoFactorChallengeRepository challenges;
  @Autowired SupportTicketRepository tickets;
  @Autowired SupportReplyRepository replies;
  @Autowired TransactionTemplate transaction;
  @Autowired DataSource source;

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
    SupportTicketEntity oldRequesterClosed =
        ticket(user, now.minus(181, ChronoUnit.DAYS), Status.CLOSED_BY_REQUESTER);
    SupportTicketEntity oldOpen = ticket(user, now.minus(181, ChronoUnit.DAYS), Status.OPEN);
    SupportTicketEntity recentClosed = ticket(user, now.minus(1, ChronoUnit.DAYS), Status.CLOSED);
    SupportTicketEntity recentRequesterClosed =
        ticket(user, now.minus(1, ChronoUnit.DAYS), Status.CLOSED_BY_REQUESTER);
    SupportReplyEntity reply = new SupportReplyEntity();
    reply.setTicket(oldClosed);
    reply.setAuthor(user);
    reply.setBody("Synthetic closed conversation");
    reply.setCreatedAt(oldClosed.getCreatedAt());
    replies.saveAndFlush(reply);

    new ResourceDatabasePopulator(new ClassPathResource("db/durable-sessions-h2.sql"))
        .execute(source);
    transaction.executeWithoutResult(
        status -> new TwoFactorCleanup(challenges, tickets, new JdbcTemplate(source)).cleanup());

    TwoFactorChallengeEntity cleared = challenges.findById(expired.getId()).orElseThrow();
    assertTrue(cleared.isConsumed());
    assertNull(cleared.getEncryptedPayload());
    assertNull(cleared.getVerificationDigest());
    assertFalse(challenges.existsById(old.getId()));
    assertEquals(
        "synthetic encrypted payload",
        challenges.findById(active.getId()).orElseThrow().getEncryptedPayload());
    assertFalse(tickets.existsById(oldClosed.getId()));
    assertFalse(tickets.existsById(oldRequesterClosed.getId()));
    assertFalse(replies.existsById(reply.getId()));
    assertTrue(tickets.existsById(oldOpen.getId()));
    assertTrue(tickets.existsById(recentClosed.getId()));
    assertTrue(tickets.existsById(recentRequesterClosed.getId()));
  }

  @Test
  void recoveryOutputsFollowTheSessionAndAcknowledgementInsteadOfConfirmationExpiry() {
    new ResourceDatabasePopulator(new ClassPathResource("db/durable-sessions-h2.sql"))
        .execute(source);
    JdbcTemplate jdbc = new JdbcTemplate(source);
    Instant now = Instant.now();
    String sessionId = UUID.randomUUID().toString();
    String expiredSessionId = UUID.randomUUID().toString();
    String revokedSessionId = UUID.randomUUID().toString();
    insertSession(jdbc, sessionId, now.plusSeconds(1800));
    insertSession(jdbc, expiredSessionId, now.minusSeconds(1));
    insertSession(jdbc, revokedSessionId, now.plusSeconds(1800));
    jdbc.update(
        "insert into SPRING_SESSION_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) select PRIMARY_ID, ?, ? from SPRING_SESSION where SESSION_ID = ?",
        DurableSessionRepository.EXPIRED,
        new byte[] {1},
        revokedSessionId);
    UserEntity user =
        users.saveAndFlush(
            UserEntity.builder()
                .firstName("Recovery")
                .lastName("Fixture")
                .email(UUID.randomUUID() + "@example.test")
                .enabled(true)
                .roleEntities(List.of())
                .build());
    TwoFactorChallengeEntity active = output(user, sessionId, now.minusSeconds(7200), false);
    TwoFactorChallengeEntity expired =
        output(user, expiredSessionId, now.minusSeconds(7200), false);
    TwoFactorChallengeEntity revoked =
        output(user, revokedSessionId, now.minusSeconds(7200), false);
    TwoFactorChallengeEntity missing =
        output(user, UUID.randomUUID().toString(), now.minusSeconds(7200), false);
    TwoFactorChallengeEntity saved = output(user, sessionId, now.minusSeconds(7200), true);
    transaction.executeWithoutResult(
        status -> new TwoFactorCleanup(challenges, tickets, jdbc).cleanup());
    assertEquals(
        "synthetic encrypted payload",
        challenges.findById(active.getId()).orElseThrow().getEncryptedPayload());
    assertFalse(challenges.existsById(expired.getId()));
    assertFalse(challenges.existsById(revoked.getId()));
    assertFalse(challenges.existsById(missing.getId()));
    assertFalse(challenges.existsById(saved.getId()));
    jdbc.update("delete from SPRING_SESSION where SESSION_ID = ?", sessionId);
    transaction.executeWithoutResult(
        status -> new TwoFactorCleanup(challenges, tickets, jdbc).cleanup());
    assertFalse(challenges.existsById(active.getId()));
  }

  private void insertSession(JdbcTemplate jdbc, String id, Instant expires) {
    long created = expires.minusSeconds(1800).toEpochMilli();
    jdbc.update(
        "insert into SPRING_SESSION (PRIMARY_ID, SESSION_ID, CREATION_TIME, LAST_ACCESS_TIME, MAX_INACTIVE_INTERVAL, EXPIRY_TIME) values (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID().toString(),
        id,
        created,
        created,
        1800,
        expires.toEpochMilli());
  }

  private TwoFactorChallengeEntity output(
      UserEntity user, String sessionId, Instant expiry, boolean saved) {
    TwoFactorChallengeEntity output = challenge(user, expiry);
    output.setKind(Kind.RECOVERY_OUTPUT);
    output.setSessionDigest(TwoFactorAuthorization.sessionDigest(sessionId));
    output.setConsumed(saved);
    return challenges.saveAndFlush(output);
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
