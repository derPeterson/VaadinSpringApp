package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Getrennte Lesegrenzen für den Hilfszugang und die Administration desselben Kontos. */
@Entity
@Table(name = "support_ticket_viewers")
@Getter
@Setter
public class SupportTicketViewerEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long ticketId;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private long requesterReadThrough;

  @Column(nullable = false)
  private long adminReadThrough;

  @Column(nullable = false)
  private boolean subscribed;
}
