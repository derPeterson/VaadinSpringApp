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
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Kleine Vorgangsgrundlage. Bearbeitungsstatus ist ausdrücklich keine Sicherheitsfreigabe. */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
public class SupportTicketEntity {
  public enum Type {
    RECOVERY
  }

  public enum Status {
    OPEN,
    IN_PROGRESS,
    CLOSED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(nullable = false)
  private Long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserEntity owner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  private UserEntity assignee;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Type type = Type.RECOVERY;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.OPEN;

  @Column(nullable = false, length = 120)
  private String subject;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant instructionSentAt;
}
