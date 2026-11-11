package de.derpeterson.app.service;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.EmailVerificationStatus;
import de.derpeterson.app.model.enums.RegistrationChannel;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.validation.UserInputRules;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * User saves and admin edits require valid names, mail, optional past birth date, optional gender
 * and non-null roles/elements. Empty roles remain allowed. Save accepts an encoded, nonblank
 * password plus non-null locale/status; password updates use the existing raw password rule.
 * Invalid arguments raise IllegalArgumentException; last-superadmin removal or missing
 * status/activity users raise IllegalStateException. Stale admin snapshots/deletes raise
 * OptimisticLockingFailureException. Persistence failures propagate (including unique email
 * conflicts); roll back the whole transaction, never reuse its persistence context after failure.
 */
@Service
@Slf4j
public class UserService {
  private static final String ENTITY_REQUIRED_MESSAGE = "Entity must not be null";
  private static final String USER_ID_REQUIRED_MESSAGE = "User ID must not be null";

  private final UserRepository userRepository;
  private final UserStatusBroadcaster userStatusBroadcaster;
  private final PasswordEncoder passwordEncoder;
  private final UserProfileBroadcaster profileEvents;
  private final AdminRolePolicy rolePolicy;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final Clock userServiceClock;

  @Autowired
  public UserService(
      UserRepository userRepository,
      UserStatusBroadcaster userStatusBroadcaster,
      PasswordEncoder passwordEncoder,
      UserProfileBroadcaster profileEvents,
      AdminRolePolicy rolePolicy,
      ApplicationEventPublisher applicationEventPublisher) {
    this(
        userRepository,
        userStatusBroadcaster,
        passwordEncoder,
        profileEvents,
        rolePolicy,
        applicationEventPublisher,
        Clock.systemUTC());
  }

  UserService(
      UserRepository userRepository,
      UserStatusBroadcaster userStatusBroadcaster,
      PasswordEncoder passwordEncoder,
      UserProfileBroadcaster profileEvents,
      AdminRolePolicy rolePolicy,
      ApplicationEventPublisher applicationEventPublisher,
      Clock userServiceClock) {
    this.userRepository = userRepository;
    this.userStatusBroadcaster = userStatusBroadcaster;
    this.passwordEncoder = passwordEncoder;
    this.profileEvents = profileEvents;
    this.rolePolicy = rolePolicy;
    this.applicationEventPublisher = applicationEventPublisher;
    this.userServiceClock = Objects.requireNonNull(userServiceClock);
  }

  private Set<RoleType> roleTypes(Collection<RoleEntity> roles) {
    return roles.stream().map(RoleEntity::getName).collect(java.util.stream.Collectors.toSet());
  }

  public boolean canManageSuperadmins() {
    return rolePolicy.canManageSuperadmins();
  }

  @Transactional
  public void createAdminUser(UserEntity accountUserEntity, String rawPortalPassword) {
    createAdminUser(accountUserEntity, rawPortalPassword, false);
  }

  @Transactional
  public void createAdminUser(
      UserEntity accountUserEntity, String rawPortalPassword, boolean skipEmailVerification) {
    Assert.notNull(accountUserEntity, ENTITY_REQUIRED_MESSAGE);
    Assert.isNull(accountUserEntity.getId(), "New account must not have an ID");
    validateUserFields(accountUserEntity);
    lockAndReadSuperadmins();
    rolePolicy.authorizeMutation(null, roleTypes(accountUserEntity.getRoleEntities()));

    if (skipEmailVerification) {
      rolePolicy.requireSuperadmin();
    }

    accountUserEntity.setRegistrationChannel(RegistrationChannel.ADMINISTRATION);
    accountUserEntity.setEmailVerificationStatus(
        skipEmailVerification
            ? EmailVerificationStatus.NOT_CHECKED
            : EmailVerificationStatus.PENDING);

    updatePassword(accountUserEntity, rawPortalPassword);
    save(accountUserEntity);
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
        .ifPresent(user -> {
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
    Assert.notNull(user, ENTITY_REQUIRED_MESSAGE);
    validateUserFields(user);
    Assert.isTrue(
        user.getPassword() != null && !user.getPassword().isBlank()
            || user.getPassword() == null
                && user.getId() != null
                && userRepository.hasExternalSignIn(user.getId()),
        "An encoded password or a connected provider is required");
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
      applicationEventPublisher.publishEvent(
          new de.derpeterson.app.events.UserRolesChanged(
              user.getId(), previousRoles, roleTypes(user.getRoleEntities())));
    }
  }

  @Transactional
  public void updateUserLocale(String email, Locale locale) {
    Assert.isTrue(UserInputRules.isEmailValid(email), "Invalid email");
    validateLocale(locale);
    Optional<UserEntity> userOptional = findByEmail(email);
    userOptional.ifPresent(user -> {
      user.setPreferredLocale(locale);
      save(user);
    });
  }

  public record StatusUpdate(UserStatus status, boolean manuallySet) {}

  @Transactional
  public StatusUpdate updateUserStatus(
      UserEntity userEntity, UserStatus newStatus, boolean manualChange) {
    Assert.notNull(userEntity, ENTITY_REQUIRED_MESSAGE);
    Assert.notNull(userEntity.getId(), USER_ID_REQUIRED_MESSAGE);
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
    Assert.notNull(userId, USER_ID_REQUIRED_MESSAGE);
    if (userRepository.updateLastActivity(userId, userServiceClock.instant()) == 0) {
      throw new IllegalStateException("Der Benutzer ist nicht mehr vorhanden.");
    }
  }

  /**
   * Requires ID/cutoff and ABSENT/AVAILABLE; missing/ineligible candidates are ignored. Rechecks
   * under a per-user lock.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateScheduledStatus(Long userId, UserStatus target, Instant cutoff) {
    Assert.notNull(userId, USER_ID_REQUIRED_MESSAGE);
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
   * raise OptimisticLockingFailureException. Returns whether the canonical email address changed.
   */
  @Transactional
  public boolean updateAdminUser(
      UserEntity editedUserEntity, Long expectedUserVersion, String rawPortalPassword) {
    Assert.notNull(editedUserEntity, ENTITY_REQUIRED_MESSAGE);
    Assert.notNull(editedUserEntity.getId(), USER_ID_REQUIRED_MESSAGE);
    List<Long> enabledSuperadminIds = lockAndReadSuperadmins();
    UserEntity storedUserEntity =
        userRepository
            .findByIdForUpdate(editedUserEntity.getId())
            .orElseThrow(() ->
                new OptimisticLockingFailureException("Der Benutzer wurde inzwischen gelöscht."));
    if (expectedUserVersion == null
        || !Objects.equals(expectedUserVersion, storedUserEntity.getVersion())) {
      throw new OptimisticLockingFailureException(
          "Der Benutzer wurde inzwischen geändert. Bitte das Formular neu öffnen und die Änderungen prüfen.");
    }

    validateUserFields(editedUserEntity);
    if (rawPortalPassword != null && !rawPortalPassword.isBlank()) {
      Assert.isTrue(UserInputRules.isPasswordSecure(rawPortalPassword), "Invalid password");
    }

    Set<RoleType> previousRoleTypes =
        Set.copyOf(userRepository.findExplicitRoles(storedUserEntity.getId()));
    Set<RoleType> nextRoleTypes = roleTypes(editedUserEntity.getRoleEntities());
    rolePolicy.authorizeMutation(storedUserEntity.getId(), nextRoleTypes);
    ensureSuperadminChangeAllowed(
        storedUserEntity.getId(), isEnabledSuperadmin(editedUserEntity), enabledSuperadminIds);

    storedUserEntity.setFirstName(editedUserEntity.getFirstName());
    storedUserEntity.setLastName(editedUserEntity.getLastName());
    boolean emailAddressChanged =
        updateAdminEmailAddress(storedUserEntity, editedUserEntity.getEmail());
    storedUserEntity.setGender(editedUserEntity.getGender());
    storedUserEntity.setBirthDate(editedUserEntity.getBirthDate());
    storedUserEntity.setEnabled(editedUserEntity.isEnabled());
    storedUserEntity.setRoleEntities(new ArrayList<>(editedUserEntity.getRoleEntities()));

    if (rawPortalPassword != null && !rawPortalPassword.isBlank()) {
      updatePassword(storedUserEntity, rawPortalPassword);
    }

    userRepository.saveAndFlush(storedUserEntity);
    profileEvents.afterCommit(storedUserEntity);
    ensureSuperadminsRemain(enabledSuperadminIds);

    AfterCommitLog.info(
        log,
        "Admin account update committed: userId={}, enabled={}, roles={}, passwordChanged={}",
        storedUserEntity.getId(),
        storedUserEntity.isEnabled(),
        Set.copyOf(nextRoleTypes),
        rawPortalPassword != null && !rawPortalPassword.isBlank());
    if (!previousRoleTypes.equals(nextRoleTypes)) {
      applicationEventPublisher.publishEvent(
          new de.derpeterson.app.events.UserRolesChanged(
              storedUserEntity.getId(), previousRoleTypes, nextRoleTypes));
    }

    return emailAddressChanged;
  }

  private boolean updateAdminEmailAddress(UserEntity storedUserEntity, String updatedEmailAddress) {
    String canonicalEmailAddress = EmailIdentity.canonicalize(updatedEmailAddress);
    if (Objects.equals(storedUserEntity.getEmail(), canonicalEmailAddress)) {
      return false;
    }

    // Ein bereits beanspruchter Auftrag behält sein altes Ziel und wird nicht erneut freigegeben.
    userRepository.preserveClaimedAddressMailRecipients(
        storedUserEntity.getId(), storedUserEntity.getEmail());
    boolean emailVerificationExempt =
        storedUserEntity.getEmailVerificationStatus() == EmailVerificationStatus.NOT_CHECKED;
    storedUserEntity.setEmail(canonicalEmailAddress);
    if (emailVerificationExempt) {
      storedUserEntity.setEmailVerificationStatus(EmailVerificationStatus.NOT_CHECKED);
    }

    userRepository.invalidateVerificationTokensForAddressChange(storedUserEntity.getId());
    userRepository.invalidatePasswordResetTokensForAddressChange(storedUserEntity.getId());
    userRepository.deletePendingAddressMails(storedUserEntity.getId());
    storedUserEntity.setVerificationMailRequestedAt(null);
    storedUserEntity.setPasswordResetMailRequestedAt(null);
    storedUserEntity.rotateAccessVersion();
    return true;
  }

  public List<UserEntity> findAllUsers() {
    return userRepository.findAll();
  }

  @Transactional
  public void deleteUser(UserEntity user) {
    Assert.notNull(user, ENTITY_REQUIRED_MESSAGE);
    Assert.notNull(user.getId(), USER_ID_REQUIRED_MESSAGE);
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
    Assert.notNull(user, ENTITY_REQUIRED_MESSAGE);
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
    return user != null
        && user.isEnabled()
        && !user.requiresEmailVerification()
        && user.hasRole(RoleType.ROLE_SUPERADMIN);
  }

  private boolean hasSuperadminRole(Collection<RoleEntity> roleEntities) {
    return roleEntities != null
        && roleEntities.stream()
            .anyMatch(roleEntity -> roleEntity.getName() == RoleType.ROLE_SUPERADMIN);
  }

  private void validateUserFields(UserEntity accountUserEntity) {
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(accountUserEntity.getFirstName()),
        "First name must not be blank");
    Assert.isTrue(
        UserInputRules.isRequiredTextValid(accountUserEntity.getLastName()),
        "Last name must not be blank");
    Assert.isTrue(UserInputRules.isEmailValid(accountUserEntity.getEmail()), "Invalid email");
    Assert.isTrue(
        UserInputRules.isBirthDateValid(accountUserEntity.getBirthDate()),
        "Birth date must be in the past");

    validateRoles(accountUserEntity.getRoleEntities());
  }

  private void validateRoles(Collection<RoleEntity> assignedRoleEntities) {
    Assert.notNull(assignedRoleEntities, "Roles must not be null");
    Assert.isTrue(
        assignedRoleEntities.stream()
            .allMatch(assignedRoleEntity ->
                assignedRoleEntity != null && assignedRoleEntity.getName() != null),
        "Invalid role");
  }

  private void validateLocale(Locale locale) {
    Assert.notNull(locale, "Locale must not be null");
  }
}
