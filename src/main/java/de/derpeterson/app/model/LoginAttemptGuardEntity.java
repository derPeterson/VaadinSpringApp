package de.derpeterson.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A short database lock bounds identity creation across application instances. */
@Entity
@Table(name = "login_attempt_guard")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttemptGuardEntity {
  @Id private Integer id;
}
