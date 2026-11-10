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
  @Autowired UserRepository userRepository;
  @Autowired TwoFactorChallengeRepository twoFactorChallengeRepository;
  @Autowired SupportTicketRepository tickets;
  @Autowired SupportReplyRepository replies;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired DataSource testDataSource;

  @Test
  void clearsExpiredSecretsBeforeMetadataAndDeletesOnlyOldClosedConversations() {
    Instant now = Instant.now();
    UserEntity testUserEntity =
        userRepository.saveAndFlush(
            UserEntity.builder()
                .firstName("Cleanup")
                .lastName("Fixture")
                .email(UUID.randomUUID() + "@example.test")
                .enabled(true)
                .roleEntities(List.of())
                .build());
    TwoFactorChallengeEntity expired = challenge(testUserEntity, now.minusSeconds(60));
    TwoFactorChallengeEntity old = challenge(testUserEntity, now.minusSeconds(7200));
    TwoFactorChallengeEntity active = challenge(testUserEntity, now.plusSeconds(300));
    SupportTicketEntity oldClosed =
        ticket(testUserEntity, now.minus(181, ChronoUnit.DAYS), Status.CLOSED);
    SupportTicketEntity oldRequesterClosed =
        ticket(testUserEntity, now.minus(181, ChronoUnit.DAYS), Status.CLOSED_BY_REQUESTER);
    SupportTicketEntity oldOpen =
        ticket(testUserEntity, now.minus(181, ChronoUnit.DAYS), Status.OPEN);
    SupportTicketEntity recentClosed =
        ticket(testUserEntity, now.minus(1, ChronoUnit.DAYS), Status.CLOSED);
    SupportTicketEntity recentRequesterClosed =
        ticket(testUserEntity, now.minus(1, ChronoUnit.DAYS), Status.CLOSED_BY_REQUESTER);
    SupportReplyEntity reply = new SupportReplyEntity();
    reply.setTicket(oldClosed);
    reply.setAuthor(testUserEntity);
    reply.setBody("Synthetic closed conversation");
    reply.setCreatedAt(oldClosed.getCreatedAt());
    replies.saveAndFlush(reply);

    new ResourceDatabasePopulator(new ClassPathResource("db/durable-sessions-h2.sql"))
        .execute(testDataSource);
    transactionTemplate.executeWithoutResult(status ->
        new TwoFactorCleanup(
                twoFactorChallengeRepository, tickets, new JdbcTemplate(testDataSource))
            .purgeExpiredRecoveryData());

    TwoFactorChallengeEntity cleared =
        twoFactorChallengeRepository.findById(expired.getId()).orElseThrow();

    assertTrue(cleared.isConsumed());
    assertNull(cleared.getEncryptedPayload());
    assertNull(cleared.getVerificationDigest());
    assertFalse(twoFactorChallengeRepository.existsById(old.getId()));
    assertEquals(
        "synthetic encrypted payload",
        twoFactorChallengeRepository.findById(active.getId()).orElseThrow().getEncryptedPayload());
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
        .execute(testDataSource);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
    Instant now = Instant.now();
    String sessionId = UUID.randomUUID().toString();
    String expiredSessionId = UUID.randomUUID().toString();
    String revokedSessionId = UUID.randomUUID().toString();
    insertSession(jdbcTemplate, sessionId, now.plusSeconds(1800));
    insertSession(jdbcTemplate, expiredSessionId, now.minusSeconds(1));
    insertSession(jdbcTemplate, revokedSessionId, now.plusSeconds(1800));
    jdbcTemplate.update(
        "insert into SPRING_SESSION_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) select PRIMARY_ID, ?, ? from SPRING_SESSION where SESSION_ID = ?",
        DurableSessionRepository.EXPIRED,
        new byte[] {1},
        revokedSessionId);
    UserEntity testUserEntity =
        userRepository.saveAndFlush(
            UserEntity.builder()
                .firstName("Recovery")
                .lastName("Fixture")
                .email(UUID.randomUUID() + "@example.test")
                .enabled(true)
                .roleEntities(List.of())
                .build());
    TwoFactorChallengeEntity active =
        output(testUserEntity, sessionId, now.minusSeconds(7200), false);
    TwoFactorChallengeEntity expired =
        output(testUserEntity, expiredSessionId, now.minusSeconds(7200), false);
    TwoFactorChallengeEntity revoked =
        output(testUserEntity, revokedSessionId, now.minusSeconds(7200), false);
    TwoFactorChallengeEntity missing =
        output(testUserEntity, UUID.randomUUID().toString(), now.minusSeconds(7200), false);
    TwoFactorChallengeEntity saved =
        output(testUserEntity, sessionId, now.minusSeconds(7200), true);
    transactionTemplate.executeWithoutResult(status ->
        new TwoFactorCleanup(twoFactorChallengeRepository, tickets, jdbcTemplate)
            .purgeExpiredRecoveryData());

    assertEquals(
        "synthetic encrypted payload",
        twoFactorChallengeRepository.findById(active.getId()).orElseThrow().getEncryptedPayload());
    assertFalse(twoFactorChallengeRepository.existsById(expired.getId()));
    assertFalse(twoFactorChallengeRepository.existsById(revoked.getId()));
    assertFalse(twoFactorChallengeRepository.existsById(missing.getId()));
    assertFalse(twoFactorChallengeRepository.existsById(saved.getId()));
    jdbcTemplate.update("delete from SPRING_SESSION where SESSION_ID = ?", sessionId);
    transactionTemplate.executeWithoutResult(status ->
        new TwoFactorCleanup(twoFactorChallengeRepository, tickets, jdbcTemplate)
            .purgeExpiredRecoveryData());

    assertFalse(twoFactorChallengeRepository.existsById(active.getId()));
  }

  private void insertSession(JdbcTemplate jdbcTemplate, String id, Instant expires) {
    long created = expires.minusSeconds(1800).toEpochMilli();
    jdbcTemplate.update(
        "insert into SPRING_SESSION (PRIMARY_ID, SESSION_ID, CREATION_TIME, LAST_ACCESS_TIME, MAX_INACTIVE_INTERVAL, EXPIRY_TIME) values (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID().toString(),
        id,
        created,
        created,
        1800,
        expires.toEpochMilli());
  }

  private TwoFactorChallengeEntity output(
      UserEntity testUserEntity, String sessionId, Instant expiry, boolean saved) {
    TwoFactorChallengeEntity output = challenge(testUserEntity, expiry);
    output.setKind(Kind.RECOVERY_OUTPUT);
    output.setSessionDigest(TwoFactorAuthorization.sessionDigest(sessionId));
    output.setConsumed(saved);
    return twoFactorChallengeRepository.saveAndFlush(output);
  }

  private TwoFactorChallengeEntity challenge(UserEntity testUserEntity, Instant expiry) {
    TwoFactorChallengeEntity row = new TwoFactorChallengeEntity();
    row.setUser(testUserEntity);
    row.setSessionDigest("synthetic-session");
    row.setAccessVersion(testUserEntity.getAccessVersion());
    row.setFactorRevision(UUID.randomUUID());
    row.setAction(TwoFactorAction.MANAGE_FACTOR);
    row.setKind(Kind.ENROLLMENT);
    row.setCreatedAt(expiry.minusSeconds(300));
    row.setExpiresAt(expiry);
    row.setEncryptedPayload("synthetic encrypted payload");
    row.setVerificationDigest("synthetic digest");
    return twoFactorChallengeRepository.saveAndFlush(row);
  }

  private SupportTicketEntity ticket(UserEntity testUserEntity, Instant updated, Status status) {
    SupportTicketEntity row = new SupportTicketEntity();
    row.setOwner(testUserEntity);
    row.setSubject("Synthetic recovery request");
    row.setStatus(status);
    row.setCreatedAt(updated);
    row.setUpdatedAt(updated);
    row.setPublicUpdatedAt(updated);
    return tickets.saveAndFlush(row);
  }
}
