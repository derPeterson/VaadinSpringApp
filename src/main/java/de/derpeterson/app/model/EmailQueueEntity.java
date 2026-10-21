package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "email_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity userEntity;

  // Sicherheitsmails fixieren ihre Zieladresse; andere Mailaufträge verwenden die Kontoadresse.
  @Column private String recipient;

  @Column(nullable = false)
  private String subject;

  @Lob
  @Column(nullable = false, columnDefinition = "CLOB")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmailType emailType = EmailType.NOTIFICATION; // Standard: NOTIFICATION

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmailStatus status = EmailStatus.PENDING;

  @Column(nullable = false)
  private int retryCount = 0;

  @Column private LocalDateTime lastRetryAt;

  @Column(nullable = false)
  // Die Mailqueue verwendet lokale Serverzeit; Aktivitätszeiten sind absolute Instant-Werte.
  private LocalDateTime createdAt = LocalDateTime.now(ZoneId.systemDefault());
}
