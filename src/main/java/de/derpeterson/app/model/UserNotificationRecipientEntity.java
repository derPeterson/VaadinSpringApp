package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
    name = "user_notification_recipients",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_notification_recipient",
            columnNames = {"notification_id", "user_id"}),
    indexes = @Index(name = "idx_user_notification_inbox", columnList = "user_id, read_at, id"))
@Getter
@Setter
public class UserNotificationRecipientEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notification_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserNotificationEntity notification;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserEntity user;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "dismissed_at")
  private Instant dismissedAt;
}
