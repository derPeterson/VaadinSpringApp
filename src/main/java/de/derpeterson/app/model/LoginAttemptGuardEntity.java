package de.derpeterson.app.model;

import jakarta.persistence.*;
import lombok.*;

/** A short database lock bounds identity creation across application instances. */
@Entity
@Table(name = "login_attempt_guard")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginAttemptGuardEntity {
    @Id
    private Integer id;
}
