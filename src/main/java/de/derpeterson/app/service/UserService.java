package de.derpeterson.app.service;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.validation.UserInputRules;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * User saves and admin edits require valid names, mail, past birth date, gender and non-null
 * roles/elements. Empty roles remain allowed. Save accepts an encoded, nonblank password plus
 * non-null locale/status; password updates use the existing raw password rule. Invalid arguments
 * raise IllegalArgumentException; last-superadmin removal or missing status/activity users raise
 * IllegalStateException. Stale admin snapshots/deletes raise OptimisticLockingFailureException.
 * Persistence failures propagate (including unique email conflicts); roll back the whole
 * transaction, never reuse its persistence context after failure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UserStatusBroadcaster userStatusBroadcaster;
  private final PasswordEncoder passwordEncoder;
  private final UserProfileBroadcaster profileEvents;
  private final de.derpeterson.app.security.AdminRolePolicy rolePolicy;
  private final org.springframework.context.ApplicationEventPublisher events;

  private Set<RoleType> roleTypes(Collection<RoleEntity> roles) {
    return roles.stream().map(RoleEntity::getName).collect(java.util.stream.Collectors.toSet());
  }

  public boolean canManageSuperadmins() {
    return rolePolicy.canManageSuperadmins();
  }

  @Transactional
  public void createAdminUser(UserEntity user, String password) {
    Assert.notNull(user, "Entity must not be null");
    Assert.isNull(user.getId(), "New account must not have an ID");
    validateUserFields(user);
    lockAndReadSuperadmins();
    rolePolicy.authorizeMutation(null, roleTypes(user.getRoleEntities()));
    updatePassword(user, password);
    save(user);
  }

  @Transactional
  public void saveUser(UserEntity userEntity) {
    save(userEntity);
  }

  /**
   * Called through the service proxy so a login's status/activity update has its own transaction
   * boundary.
   */
  @Transactional
  public void recordLogin(String email) {
    userRepository
        .findByEmailForUpdate(EmailIdentity.canonicalize(email))
        .ifPresent(
            user -> {
              if (!user.isStatusManuallySet()) {
                updateUserStatus(user, UserStatus.AVAILABLE, false);
              }
              updateLastActivity(user.getId());
            });
  }

  public Optional<UserEntity> findByEmail(String email) {
    return userRepository.findByEmail(EmailIdentity.canonicalize(email));
  }

  @Transactional
  public void save(UserEntity user) {
    Assert.notNull(user, "Entity must not be null");
    validateUserFields(user);
    Assert.hasText(user.getPassword(), "Encoded password must not be blank");
    validateLocale(user.getPreferredLocale());
    Assert.notNull(user.getStatus(), "Status must not be null");
    user.setEmail(user.getEmail());
    List<Long> superadmins = lockAndReadSuperadmins();
    boolean creating = user.getId() == null;
    Set<RoleType> previousRoles =
        user.getId() == null ? null : Set.copyOf(userRepository.findExplicitRoles(user.getId()));
    Set<RoleType> desired = roleTypes(user.getRoleEntities());
    if (previousRoles != null && !previousRoles.equals(desired)
        || previousRoles == null && !desired.equals(Set.of(RoleType.ROLE_USER))) {
      rolePolicy.authorizeMutation(user.getId(), desired);
    } else {
      rolePolicy.authorizeProtectedMutation(user.getId(), desired);
    }
    ensureSuperadminChangeAllowed(user.getId(), isEnabledSuperadmin(user), superadmins);
    userRepository.save(user);
    userRepository.flush();
    ensureSuperadminsRemain(superadmins);
    if (creating) {
      AfterCommitLog.info(
          log,
          "Account created: userId={}, enabled={}, roles={}",
          user.getId(),
          user.isEnabled(),
          Set.copyOf(desired));
    } else if (!previousRoles.equals(desired)) {
      AfterCommitLog.info(
          log, "Account roles changed: userId={}, roles={}", user.getId(), Set.copyOf(desired));
    }
    if (previousRoles != null && !previousRoles.equals(roleTypes(user.getRoleEntities()))) {
      events.publishEvent(
          new de.derpeterson.app.events.UserRolesChanged(
              user.getId(), previousRoles, roleTypes(user.getRoleEntities())));
    }
  }

  @Transactional
  public void updateUserLocale(String email, Locale locale) {
    Assert.isTrue(UserInputRules.isEmailValid(email), "Invalid email");
    validateLocale(locale);
    Optional<UserEntity> userOptional = findByEmail(email);
    userOptional.ifPresent(
        user -> {
          user.setPreferredLocale(locale);
          save(user);
        });
  }

  public record StatusUpdate(UserStatus status, boolean manuallySet) {}

  @Transactional
  public StatusUpdate updateUserStatus(
      UserEntity userEntity, UserStatus newStatus, boolean manualChange) {
    Assert.notNull(userEntity, "Entity must not be null");
    Assert.notNull(userEntity.getId(), "User ID must not be null");
    Assert.notNull(newStatus, "Status must not be null");
    UserEntity user =
        userRepository
            .findById(userEntity.getId())
            .orElseThrow(() -> new IllegalStateException("Der Benutzer ist nicht mehr vorhanden."));
    UserStatus oldStatus = user.getStatus();

    if (manualChange) {
      user.setManualStatus(newStatus);
    } else {
      user.setAutomaticStatus(newStatus);
    }

    // No merge of the caller's snapshot and no admin guard: privileges are not changed.
    userRepository.save(user);

    UserStatus resultingStatus = user.getStatus();
    if (oldStatus != resultingStatus) {
      // Multiple changes in one transaction describe its committed net result,
      // never intermediate states which were not committed independently.
      StatusChangeSynchronization pending =
          TransactionSynchronizationManager.getSynchronizations().stream()
              .filter(StatusChangeSynchronization.class::isInstance)
              .map(StatusChangeSynchronization.class::cast)
              .filter(change -> Objects.equals(change.userId, user.getId()))
              .findFirst()
              .orElse(null);
      if (pending == null) {
        TransactionSynchronizationManager.registerSynchronization(
            new StatusChangeSynchronization(user.getId(), oldStatus, resultingStatus));
      } else {
        pending.newStatus = resultingStatus;
      }
    }
    return new StatusUpdate(resultingStatus, user.isStatusManuallySet());
  }

  private class StatusChangeSynchronization implements TransactionSynchronization {
    private final Long userId;
    private final UserStatus oldStatus;
    private UserStatus newStatus;

    private StatusChangeSynchronization(Long userId, UserStatus oldStatus, UserStatus newStatus) {
      this.userId = userId;
      this.oldStatus = oldStatus;
      this.newStatus = newStatus;
    }

    @Override
    public void afterCommit() {
      if (oldStatus == newStatus) {
        return;
      }
      try {
        userStatusBroadcaster.broadcast(
            new UserStatusBroadcaster.UserStatusMessage(
                userId, oldStatus.name(), newStatus.name()));
      } catch (RuntimeException exception) {
        log.error(
            "Statuszustellung nach Commit fehlgeschlagen für Benutzer {}",
            userId,
            safeFailure(exception));
      }
    }
  }

  @Transactional
  public void updateLastActivity(Long userId) {
    Assert.notNull(userId, "User ID must not be null");
    if (userRepository.updateLastActivity(userId, LocalDateTime.now()) == 0) {
      throw new IllegalStateException("Der Benutzer ist nicht mehr vorhanden.");
    }
  }

  /**
   * Requires ID/cutoff and ABSENT/AVAILABLE; missing/ineligible candidates are ignored. Rechecks
   * under a per-user lock.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateScheduledStatus(Long userId, UserStatus target, LocalDateTime cutoff) {
    Assert.notNull(userId, "User ID must not be null");
    Assert.notNull(cutoff, "Cutoff must not be null");
    Assert.isTrue(
        target == UserStatus.ABSENT || target == UserStatus.AVAILABLE, "Invalid scheduler status");
    UserEntity user = userRepository.findByIdForUpdate(userId).orElse(null);
    if (user == null) {
      return;
    }
    boolean eligible =
        target == UserStatus.ABSENT
            ? user.getStatus() == UserStatus.AVAILABLE && user.getLastActivity().isBefore(cutoff)
            : user.getStatus() == UserStatus.ABSENT
                && !user.isStatusManuallySet()
                && user.getLastActivity().isAfter(cutoff);
    if (eligible) {
      updateUserStatus(user, target, false);
    }
  }

  /**
   * Requires ID/form-open version; null/blank password means unchanged. Stale/deleted snapshots
   * raise OptimisticLockingFailureException.
   */
  @Transactional
  public void updateAdminUser(UserEntity edited, Long expectedVersion, String rawPassword) {
    Assert.notNull(edited, "Entity must not be null");
    Assert.notNull(edited.getId(), "User ID must not be null");
    List<Long> superadmins = lockAndReadSuperadmins();
    UserEntity stored =
        userRepository
            .findByIdForUpdate(edited.getId())
            .orElseThrow(
                () ->
                    new OptimisticLockingFailureException(
                        "Der Benutzer wurde inzwischen gelöscht."));
    if (expectedVersion == null || !Objects.equals(expectedVersion, stored.getVersion())) {
      throw new OptimisticLockingFailureException(
          "Der Benutzer wurde inzwischen geändert. Bitte das Formular neu öffnen und die Änderungen prüfen.");
    }
    validateUserFields(edited);
    if (rawPassword != null && !rawPassword.isBlank()) {
      Assert.isTrue(UserInputRules.isPasswordSecure(rawPassword), "Invalid password");
    }
    Set<RoleType> previousRoles = Set.copyOf(userRepository.findExplicitRoles(stored.getId()));
    Set<RoleType> nextRoles = roleTypes(edited.getRoleEntities());
    rolePolicy.authorizeMutation(stored.getId(), nextRoles);
    ensureSuperadminChangeAllowed(stored.getId(), isEnabledSuperadmin(edited), superadmins);
    stored.setFirstName(edited.getFirstName());
    stored.setLastName(edited.getLastName());
    stored.setEmail(edited.getEmail());
    stored.setGender(edited.getGender());
    stored.setBirthDate(edited.getBirthDate());
    stored.setEnabled(edited.isEnabled());
    stored.setRoleEntities(new ArrayList<>(edited.getRoleEntities()));
    if (rawPassword != null && !rawPassword.isBlank()) {
      updatePassword(stored, rawPassword);
    }
    userRepository.saveAndFlush(stored);
    profileEvents.afterCommit(stored);
    ensureSuperadminsRemain(superadmins);
    AfterCommitLog.info(
        log,
        "Admin account update committed: userId={}, enabled={}, roles={}, passwordChanged={}",
        stored.getId(),
        stored.isEnabled(),
        Set.copyOf(nextRoles),
        rawPassword != null && !rawPassword.isBlank());
    if (!previousRoles.equals(nextRoles)) {
      events.publishEvent(
          new de.derpeterson.app.events.UserRolesChanged(stored.getId(), previousRoles, nextRoles));
    }
  }

  public List<UserEntity> findAllUsers() {
    return userRepository.findAll();
  }

  @Transactional
  public void deleteUser(UserEntity user) {
    Assert.notNull(user, "Entity must not be null");
    Assert.notNull(user.getId(), "User ID must not be null");
    List<Long> superadmins = lockAndReadSuperadmins();
    rolePolicy.authorizeMutation(user.getId(), Set.of());
    ensureSuperadminChangeAllowed(user.getId(), false, superadmins);
    userRepository.delete(user);
    userRepository.flush();
    ensureSuperadminsRemain(superadmins);
    AfterCommitLog.info(log, "Account deleted: userId={}", user.getId());
  }

  private List<Long> lockAndReadSuperadmins() {
    // Alle Rollenmutationen teilen die Sperre, auch parallele Entzüge/Deaktivierungen.
    userRepository.lockRole(RoleType.ROLE_SUPERADMIN);
    return readSuperadminIds();
  }

  private List<Long> readSuperadminIds() {
    return userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN);
  }

  private boolean removesLastSuperadmin(
      Long userId, boolean remainsSuperadmin, List<Long> superadmins) {
    return !remainsSuperadmin
        && userId != null
        && superadmins.size() == 1
        && Objects.equals(superadmins.getFirst(), userId);
  }

  private void ensureSuperadminChangeAllowed(
      Long userId, boolean remainsSuperadmin, List<Long> superadmins) {
    if (removesLastSuperadmin(userId, remainsSuperadmin, superadmins)) {
      log.warn("Account mutation rejected: last enabled superadmin must remain, userId={}", userId);
      throw new IllegalStateException("Der letzte aktive Superadmin muss erhalten bleiben.");
    }
  }

  private void ensureSuperadminsRemain(List<Long> before) {
    // flush can include other users already dirtied by a caller in the same
    // transaction. Reject that entire batch, not just the submitted entity.
    if (!before.isEmpty() && readSuperadminIds().isEmpty()) {
      throw new IllegalStateException("Der letzte aktive Superadmin muss erhalten bleiben.");
    }
  }

  /**
   * Requires an entity and valid raw password; changes its hash but does not persist it. Encoder
   * failures propagate.
   */
  public void updatePassword(UserEntity user, String rawPassword) {
    Assert.notNull(user, "Entity must not be null");
    Assert.isTrue(UserInputRules.isPasswordSecure(rawPassword), "Invalid password");
    if (user.getId() != null) {
      rolePolicy.authorizeMutation(user.getId(), roleTypes(user.getRoleEntities()));
    }
    user.setPassword(passwordEncoder.encode(rawPassword));
  }

  public boolean emailExistsForOtherUser(String email, Long currentUserId) {
    if (email == null || email.isBlank()) {
      return false;
    }

    return userRepository.emailExistsForOtherUser(EmailIdentity.canonicalize(email), currentUserId);
  }

  @Transactional(readOnly = true)
  public boolean canDeleteUser(UserEntity user) {
    return user != null
        && user.getId() != null
        && !removesLastSuperadmin(user.getId(), false, readSuperadminIds());
  }

  @Transactional(readOnly = true)
  public boolean wouldRemoveLastEnabledSuperadmin(
      Long editedUserId, boolean enabled, Collection<RoleEntity> roleEntities) {
    validateRoles(roleEntities);
    if (editedUserId == null || enabled && hasSuperadminRole(roleEntities)) {
      return false;
    }
    return removesLastSuperadmin(
        editedUserId, enabled && hasSuperadminRole(roleEntities), readSuperadminIds());
  }

  private boolean isEnabledSuperadmin(UserEntity user) {
    return user != null && user.isEnabled() && user.hasRole(RoleType.ROLE_SUPERADMIN);
  }

  private boolean hasSuperadminRole(Collection<RoleEntity> roleEntities) {
    return roleEntities != null
        && roleEntities.stream()
            .anyMatch(roleEntity -> roleEntity.getName() == RoleType.ROLE_SUPERADMIN);
  }

  private void validateUserFields(UserEntity user) {
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(user.getFirstName()), "First name must not be blank");
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(user.getLastName()), "Last name must not be blank");
    Assert.isTrue(UserInputRules.isEmailValid(user.getEmail()), "Invalid email");
    Assert.notNull(user.getGender(), "Gender must not be null");
    Assert.isTrue(
        UserInputRules.isBirthDateValid(user.getBirthDate()), "Birth date must be in the past");
    validateRoles(user.getRoleEntities());
  }

  private void validateRoles(Collection<RoleEntity> roles) {
    Assert.notNull(roles, "Roles must not be null");
    Assert.isTrue(
        roles.stream().allMatch(role -> role != null && role.getName() != null), "Invalid role");
  }

  private void validateLocale(Locale locale) {
    Assert.notNull(locale, "Locale must not be null");
  }
}
