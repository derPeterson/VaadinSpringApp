package de.derpeterson.app.service;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.Gender;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AccountProfileService {
  private final SecurityService securityService;
  private final UserRepository userRepository;
  private final ConfigService configService;
  private final UserProfileBroadcaster userProfileBroadcaster;

  // Kein Benutzerbezeichner aus dem Formular: Das Zielkonto kommt immer aus der Anmeldung.
  public record Changes(
      Long version,
      String firstName,
      String lastName,
      LocalDate birthDate,
      Locale language,
      Gender gender)
      implements Serializable {}

  public record Profile(
      Long version,
      String firstName,
      String lastName,
      LocalDate birthDate,
      Locale language,
      Gender gender)
      implements Serializable {
    public static Profile from(UserEntity accountUserEntity) {
      return new Profile(
          accountUserEntity.getVersion(),
          accountUserEntity.getFirstName(),
          accountUserEntity.getLastName(),
          accountUserEntity.getBirthDate(),
          accountUserEntity.getPreferredLocale(),
          accountUserEntity.getGender());
    }
  }

  // Eigene Transaktion verhindert, dass ein alter OSIV-/UI-Zustand als aktuelle DB-Version gilt.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Profile save(HttpServletRequest httpRequest, Changes profileChanges) {
    UserDetails authenticatedUserDetails =
        securityService
            .getAuthenticatedUser(httpRequest)
            .orElseThrow(() -> new AccessDeniedException("Authentication required"));

    MaintenanceService.requirePublicAccess(configService);
    Assert.notNull(profileChanges, "Profile must not be null");
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(profileChanges.firstName()), "Invalid first name");
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(profileChanges.lastName()), "Invalid last name");
    Assert.isTrue(
        UserInputRules.isBirthDateValid(profileChanges.birthDate()), "Invalid birth date");
    Assert.isTrue(
        Locale.GERMAN.equals(profileChanges.language())
            || Locale.ENGLISH.equals(profileChanges.language()),
        "Unsupported language");

    UserEntity accountUserEntity =
        userRepository
            .findByEmailForUpdate(authenticatedUserDetails.getUsername())
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));

    if (!accountUserEntity.isEnabled()) {
      throw new AccessDeniedException("Account disabled");
    }
    if (profileChanges.version() == null
        || !Objects.equals(profileChanges.version(), accountUserEntity.getVersion())) {
      throw new OptimisticLockingFailureException("Profile changed; reload before saving");
    }

    accountUserEntity.setFirstName(profileChanges.firstName().trim());
    accountUserEntity.setLastName(profileChanges.lastName().trim());
    accountUserEntity.setBirthDate(profileChanges.birthDate());
    accountUserEntity.setGender(profileChanges.gender());
    accountUserEntity.setPreferredLocale(profileChanges.language());

    // Keine Übernahme einer UI-Entity: E-Mail, Passwort, Freigabe, Rollen und Tokens bleiben
    // erhalten.
    userRepository.saveAndFlush(accountUserEntity);

    userProfileBroadcaster.afterCommit(accountUserEntity);
    AfterCommitLog.debug(log, "Account profile updated: userId={}", accountUserEntity.getId());

    return Profile.from(accountUserEntity);
  }
}
