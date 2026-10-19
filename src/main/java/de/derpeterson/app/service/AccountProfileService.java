package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.validation.UserInputRules;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/** Speichert ausschließlich die freigegebenen Profilfelder des aktuell angemeldeten Kontos. */
@Service
@RequiredArgsConstructor
public class AccountProfileService {
  private final SecurityService security;
  private final UserRepository users;
  private final ConfigService config;
  private final UserProfileBroadcaster profileEvents;

  // Kein Benutzerbezeichner aus dem Formular: Das Zielkonto kommt immer aus der Anmeldung.
  public record Changes(
      Long version, String firstName, String lastName, LocalDate birthDate, Locale language)
      implements Serializable {}

  public record Profile(
      Long version, String firstName, String lastName, LocalDate birthDate, Locale language)
      implements Serializable {
    public static Profile from(UserEntity user) {
      return new Profile(
          user.getVersion(),
          user.getFirstName(),
          user.getLastName(),
          user.getBirthDate(),
          user.getPreferredLocale());
    }
  }

  // Eigene Transaktion verhindert, dass ein alter OSIV-/UI-Zustand als aktuelle DB-Version gilt.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Profile save(HttpServletRequest request, Changes changes) {
    UserDetails principal =
        security
            .getAuthenticatedUser(request)
            .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    MaintenanceService.requirePublicAccess(config);
    Assert.notNull(changes, "Profile must not be null");
    Assert.isTrue(UserInputRules.isRequiredTextValid(changes.firstName()), "Invalid first name");
    Assert.isTrue(UserInputRules.isRequiredTextValid(changes.lastName()), "Invalid last name");
    Assert.isTrue(UserInputRules.isBirthDateValid(changes.birthDate()), "Invalid birth date");
    Assert.isTrue(
        Locale.GERMAN.equals(changes.language()) || Locale.ENGLISH.equals(changes.language()),
        "Unsupported language");
    UserEntity user =
        users
            .findByEmailForUpdate(principal.getUsername())
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    if (!user.isEnabled()) {
      throw new AccessDeniedException("Account disabled");
    }
    if (changes.version() == null || !Objects.equals(changes.version(), user.getVersion())) {
      throw new OptimisticLockingFailureException("Profile changed; reload before saving");
    }
    user.setFirstName(changes.firstName().trim());
    user.setLastName(changes.lastName().trim());
    user.setBirthDate(changes.birthDate());
    user.setPreferredLocale(changes.language());
    // Keine Übernahme einer UI-Entity: E-Mail, Passwort, Freigabe, Rollen und Tokens bleiben
    // erhalten.
    users.saveAndFlush(user);
    profileEvents.afterCommit(user);
    return Profile.from(user);
  }
}
