package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "two_factor_accounts")
@Getter
@Setter
@NoArgsConstructor
public class TwoFactorAccountEntity {
  @Id private Long userId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserEntity user;

  @Version private Long version;

  @Column(nullable = false)
  private UUID revision = UUID.randomUUID();

  @Column(length = 512)
  private String encryptedSecret;

  @Column(nullable = false)
  private boolean previouslyEnrolled;

  // Nur ein dokumentierter, bewusster Betreibereingriff darf diesen Zustand setzen.
  @Column(nullable = false)
  private boolean operatorEnrollment;

  private Instant enrolledAt;

  @Column(nullable = false)
  private long acceptedStep = -1;

  @Column(nullable = false)
  private int failedAttempts;

  private Instant attemptWindow;
  private Instant blockedUntil;

  public boolean enrolled() {
    return encryptedSecret != null;
  }

  @Override
  public String toString() {
    return "Two-factor account";
  }
}
