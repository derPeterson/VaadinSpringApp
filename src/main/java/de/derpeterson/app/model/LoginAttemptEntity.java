package de.derpeterson.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "login_attempts",
    indexes = @Index(name = "idx_login_attempt_activity", columnList = "last_activity"))
@Getter
@Setter
@NoArgsConstructor
public class LoginAttemptEntity {
  @Id
  @Column(name = "identity_hash", length = 64)
  private String identityHash;

  @Column(name = "window_started_at", nullable = false)
  private Instant windowStartedAt;

  @Column(name = "last_activity", nullable = false)
  private Instant lastActivity;

  @Column(nullable = false)
  private int failures;

  @Column(name = "blocked_until")
  private Instant blockedUntil;
}
