package de.derpeterson.app.model;

import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.type.SqlTypes;

@Entity
@DynamicUpdate
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
    check =
        @CheckConstraint(
            name = "ck_users_canonical_email",
            constraint =
                "email = lower(trim(email)) and length(email) > 0 and ascii(left(email, 1)) > 32 and ascii(right(email, 1)) > 32"),
    indexes = {@Index(name = "idx_user_lastactivity_status", columnList = "lastActivity, status")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(nullable = false)
  private Long version;

  @Column(nullable = false)
  @NotNull(message = "Der Vorname darf nicht leer sein.")
  private String firstName;

  @Column(nullable = false)
  @NotNull(message = "Der Nachname darf nicht leer sein.")
  private String lastName;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  @NotBlank(message = "Die E-Mail-Adresse darf nicht leer sein.")
  @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein.")
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @NotNull(message = "Der Geschlecht darf nicht leer sein.")
  private Gender gender;

  @Column(nullable = false)
  @Past(message = "Das Geburtsdatum muss in der Vergangenheit liegen.")
  @NotNull(message = "Das Geburtsdatum darf nicht leer sein.")
  private LocalDate birthDate;

  @Column(nullable = false)
  @Builder.Default
  private boolean enabled = false;

  /** Only a new, still-pending registration may be activated by an email link. */
  @Column(name = "verification_pending", nullable = false)
  @Builder.Default
  @Setter(AccessLevel.NONE)
  private boolean verificationPending = true;

  @Column(nullable = false)
  @Builder.Default
  // JPA erzeugt Instanzen auch bei Uploads ohne UI-Sperre. Die UI setzt die gewählte Sprache
  // explizit.
  private Locale preferredLocale = Locale.ENGLISH;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private UserStatus status = UserStatus.getDefaultStatus();

  @Column(nullable = false)
  @Builder.Default
  private boolean statusManuallySet = false;

  @Column(nullable = false)
  @Builder.Default
  @OptimisticLock(excluded = true)
  @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
  private Instant lastActivity = Instant.now();

  @ManyToMany
  @JoinTable(
      name = "users_roles",
      joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
  private Collection<RoleEntity> roleEntities;

  public void setEmail(String email) {
    this.email = EmailIdentity.canonicalize(email);
  }

  /** Explicit account decisions (including admin disable) end link-based activation. */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    this.verificationPending = false;
  }

  @PrePersist
  @PreUpdate
  private void canonicalizeEmail() {
    // Lombok builders and JPA field access do not invoke the setter.
    email = EmailIdentity.canonicalize(email);
    if (enabled) {
      verificationPending = false;
    }
  }

  public void setAutomaticStatus(UserStatus newStatus) {
    if (!this.statusManuallySet
        || this.status == UserStatus.AVAILABLE
        || this.status == UserStatus.ABSENT) {
      this.status = newStatus;
      this.statusManuallySet = false;
    }
  }

  public void setManualStatus(UserStatus newStatus) {
    this.status = newStatus;
    this.statusManuallySet = true;
  }

  public boolean hasAdministrativeRole() {
    return roleEntities != null
        && roleEntities.stream()
            .anyMatch(role -> role.getName() != null && role.getName().isAdministrative());
  }

  public boolean hasRole(RoleType roleType) {
    return roleEntities != null
        && roleEntities.stream().anyMatch(role -> role.getName() == roleType);
  }
}
