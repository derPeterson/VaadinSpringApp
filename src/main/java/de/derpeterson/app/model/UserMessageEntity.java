package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.UserMessageType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Unveränderlicher Inhalt; der Lesestatus gehört ausschließlich zum Empfänger. */
@Entity
@Table(
    name = "user_messages",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_user_message_request", columnNames = "request_key"),
    indexes = @Index(name = "idx_user_message_created", columnList = "created_at"))
@Getter
@Setter
public class UserMessageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_key", nullable = false, length = 80, updatable = false)
  private String requestKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private UserMessageType type;

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
