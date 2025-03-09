package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_queue",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "to_email", "subject", "email_type"}))
// Einzigartigkeit erweitern
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userEntity;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType emailType = EmailType.NOTIFICATION;  // Standard: NOTIFICATION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status = EmailStatus.PENDING;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column
    private LocalDateTime lastRetryAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}