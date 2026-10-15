package de.derpeterson.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "login_attempts", indexes = @Index(name = "idx_login_attempt_activity", columnList = "last_activity"))
@Getter @Setter @NoArgsConstructor
public class LoginAttemptEntity {
    @Id @Column(name = "identity_hash", length = 64)
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
