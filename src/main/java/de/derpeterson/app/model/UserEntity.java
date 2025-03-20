package de.derpeterson.app.model;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    // Static Regex für das Passwort
    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotNull(message = "Der Vorname darf nicht leer sein.")
    private String firstName;

    @Column(nullable = false)
    @NotNull(message = "Der Nachname darf nicht leer sein.")
    private String lastName;

    @Column(nullable = false)
    @NotBlank(message = "Das Passwort darf nicht leer sein.")
    @Size(min = 8, message = "Das Passwort muss mindestens 8 Zeichen lang sein.")
    @Pattern(
            regexp = PASSWORD_REGEX,
            message = "Das Passwort muss mindestens einen Großbuchstaben und ein Sonderzeichen enthalten."
    )
    private String password;

    @Column(unique = true, nullable = false)
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
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private LocalDateTime lastActivity = LocalDateTime.now();

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Collection<RoleEntity> roleEntities;

    public void setAutomaticStatus(UserStatus newStatus) {
        if (!statusManuallySet) {
            this.status = newStatus;
        }
    }

    public void setManualStatus(UserStatus newStatus) {
        this.status = newStatus;
        this.statusManuallySet = true;
    }

    public void enableAutomaticStatus() {
        this.statusManuallySet = false;
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }
}
