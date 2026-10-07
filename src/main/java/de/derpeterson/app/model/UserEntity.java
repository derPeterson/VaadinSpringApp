package de.derpeterson.app.model;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;

@Entity
@DynamicUpdate
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"), check = @CheckConstraint(name = "ck_users_canonical_email",
        constraint = "email = lower(trim(email)) and length(email) > 0 and ascii(left(email, 1)) > 32 and ascii(right(email, 1)) > 32"), indexes = {
        @Index(name = "idx_user_lastactivity_status", columnList = "lastActivity, status")
})
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

    @Column(nullable = false)
    @Builder.Default
    private Locale preferredLocale = CustomI18NProvider.getCurrentLocale();

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
    private LocalDateTime lastActivity = LocalDateTime.now();

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Collection<RoleEntity> roleEntities;

    public void setEmail(String email) {
        this.email = EmailIdentity.canonicalize(email);
    }

    @PrePersist
    @PreUpdate
    private void canonicalizeEmail() {
        // Lombok builders and JPA field access do not invoke the setter.
        email = EmailIdentity.canonicalize(email);
    }

    public void setAutomaticStatus(UserStatus newStatus) {
        if (!this.statusManuallySet || this.status == UserStatus.AVAILABLE || this.status == UserStatus.ABSENT) {
            this.status = newStatus;
            this.statusManuallySet = false;
        }
    }

    public void setManualStatus(UserStatus newStatus) {
        this.status = newStatus;
        this.statusManuallySet = true;
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public boolean hasRole(RoleType roleType) {
        return roleEntities != null &&
                roleEntities.stream().anyMatch(role -> role.getName() == roleType);
    }
}
