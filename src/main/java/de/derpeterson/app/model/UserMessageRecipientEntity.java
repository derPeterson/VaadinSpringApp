package de.derpeterson.app.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "user_message_recipients",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_message_recipient",
            columnNames = {"message_id", "user_id"}),
    indexes = @Index(name = "idx_user_message_inbox", columnList = "user_id, read_at, id"))
@Getter
@Setter
public class UserMessageRecipientEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "message_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserMessageEntity message;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserEntity user;

  @Column(name = "read_at")
  private Instant readAt;
}
