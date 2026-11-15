package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.EmailQueueCleanupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Ein logischer Gesamtlauf; keine Empfänger oder Mailinhalte, Auftraggeber nur während aktiver
 * Arbeit.
 */
@Entity
@Table(name = "email_queue_cleanup_run")
@Getter
@Setter
public class EmailQueueCleanupRunEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDateTime attemptedAt;

  @Column(nullable = false)
  private boolean automatic;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private EmailQueueCleanupStatus status;

  @Column(nullable = false)
  private long resolvedCount;

  private Long requestedById;

  @Column(nullable = false, length = 64)
  private String minimumClaimAge;

  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
  private LocalDateTime claimCutoffAt;

  @Column(nullable = false)
  private long highWaterId;

  @Column(nullable = false)
  private long cursorId;
}
