package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.TokenStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Einmaliger Nachweis für eine konkrete neue Adresse; gespeichert wird nur der Tokenhash. */
@Entity
@Table(
    name = "email_change_token",
    indexes = @Index(name = "idx_email_change_expiry", columnList = "expiryDate"))
@Data
public class EmailChangeTokenEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String digest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserEntity userEntity;

  @Column(nullable = false)
  private String oldEmail;

  @Column(nullable = false)
  private String newEmail;

  @Column(nullable = false, length = 64)
  private String credentialDigest;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime expiryDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TokenStatus status = TokenStatus.ACTIVE;
}
