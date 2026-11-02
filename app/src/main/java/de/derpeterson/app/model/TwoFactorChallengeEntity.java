package de.derpeterson.app.model;

import de.derpeterson.app.security.mfa.TwoFactorAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "two_factor_challenges")
@Getter
@Setter
@NoArgsConstructor
public class TwoFactorChallengeEntity {
  public enum Kind {
    ACTION,
    ENROLLMENT,
    GOOGLE_EMAIL,
    RECOVERY_OUTPUT
  }

  public enum SetupProof {
    FACTOR,
    RECOVERY_CODE,
    PASSWORD,
    MICROSOFT,
    GOOGLE_EMAIL,
    OPERATOR
  }

  @Id private UUID id = UUID.randomUUID();
  @Version private Long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private UserEntity user;

  @Column(nullable = false, length = 64)
  private String sessionDigest;

  @Column(nullable = false, length = 36)
  private String accessVersion;

  @Column(nullable = false)
  private UUID factorRevision;

  @Column(nullable = false)
  private long policyRevision;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TwoFactorAction action;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private Kind kind;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean consumed;

  @Column(nullable = false)
  private int attempts;

  @Column(length = 8192)
  private String encryptedPayload;

  @Column(length = 64)
  private String verificationDigest;

  @Column(length = 255)
  private String contactAddress;

  @Column(nullable = false)
  private boolean operatorEnrollment;

  @Enumerated(EnumType.STRING)
  @Column(length = 16)
  private SetupProof setupProof;

  @Override
  public String toString() {
    return "Two-factor challenge";
  }
}
