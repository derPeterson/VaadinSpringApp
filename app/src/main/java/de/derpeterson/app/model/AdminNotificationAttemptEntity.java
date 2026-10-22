package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.AdminNotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Minimal audit record; intentionally survives deletion of the original queue entry. */
@Entity
@Table(
    name = "admin_notification_attempt",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_admin_attempt_queue", columnNames = "queue_id"))
@Data
@NoArgsConstructor
public class AdminNotificationAttemptEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "queue_id", nullable = false)
  private Long queueId;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AdminNotificationStatus status = AdminNotificationStatus.IN_PROGRESS;
}
