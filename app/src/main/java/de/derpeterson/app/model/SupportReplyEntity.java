package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "support_replies")
@Getter
@Setter
public class SupportReplyEntity {
  public enum Kind {
    MESSAGE,
    RECOVERY_INSTRUCTIONS
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ticket_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private SupportTicketEntity ticket;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  private UserEntity author;

  @Column(nullable = false)
  private boolean supportAuthor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Kind kind = Kind.MESSAGE;

  @Column(nullable = false, length = 2000)
  private String body;

  @Column(nullable = false)
  private boolean internal;

  @Column(nullable = false)
  private Instant createdAt;
}
