package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Fristen und Linknachweise einer vom Kontoinhaber veranlassten Deaktivierung. */
@Embeddable
public record AccountDeactivation(
    @Column(name = "self_deactivation_delete_at") @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
        Instant deletionAt,
    @Column(name = "self_deactivation_remind_at") @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
        Instant reminderAt,
    @Column(name = "self_deactivation_initial_digest", length = 64) String initialTokenDigest,
    @Column(name = "self_deactivation_reminder_digest", length = 64) String reminderTokenDigest,
    @Column(name = "self_deactivation_requested_digest", length = 64) String requestedTokenDigest,
    @Column(name = "self_deactivation_requested_at") @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
        Instant reactivationRequestedAt) {

  public AccountDeactivation withQueuedReminder(String tokenDigest) {
    return new AccountDeactivation(
        deletionAt,
        null,
        initialTokenDigest,
        tokenDigest,
        requestedTokenDigest,
        reactivationRequestedAt);
  }

  public AccountDeactivation withRequestedLink(String tokenDigest, Instant requestedAt) {
    return new AccountDeactivation(
        deletionAt, reminderAt, initialTokenDigest, reminderTokenDigest, tokenDigest, requestedAt);
  }
}
