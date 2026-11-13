package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Ein abgeschlossener Bereinigungsversuch ohne Empfänger, Mailinhalte oder Kontobezug. */
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

  @Column(nullable = false)
  private int resolvedCount;

  @Column(nullable = false)
  private boolean busy;
}
