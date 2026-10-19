package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.UserNotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Unveränderlicher Inhalt; der Lesestatus gehört ausschließlich zum Empfänger. */
@Entity
@Table(
    name = "user_notifications",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_user_notification_request", columnNames = "request_key"),
    indexes = @Index(name = "idx_user_notification_created", columnList = "created_at"))
@Getter
@Setter
public class UserNotificationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_key", nullable = false, length = 80, updatable = false)
  private String requestKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private UserNotificationType type;

  @Column(length = 120, updatable = false)
  private String title;

  @Column(length = 4000, updatable = false)
  private String body;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false, updatable = false)
  private boolean broadcast;

  @Column(name = "recipient_count", nullable = false, updatable = false)
  private int recipientCount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  private UserEntity sender;
}
