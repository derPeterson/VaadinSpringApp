package de.derpeterson.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Collection;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String firstName;

    @Column(nullable = true)
    private String lastName;

    @Column(nullable = false)
    @NotBlank(message = "Das Passwort darf nicht leer sein.")
    @Size(min = 8, message = "Das Passwort muss mindestens 8 Zeichen lang sein.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$",
            message = "Das Passwort muss mindestens einen Großbuchstaben und ein Sonderzeichen enthalten."
    )
    private String password;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "Die E-Mail-Adresse darf nicht leer sein.")
    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein.")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Das Geschlecht muss angegeben werden.")
    private Gender gender;

    @Column(nullable = false)
    @Past(message = "Das Geburtsdatum muss in der Vergangenheit liegen.")
    @NotNull(message = "Das Geburtsdatum darf nicht leer sein.")
    private LocalDate birthDate;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private Collection<Role> roles;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
