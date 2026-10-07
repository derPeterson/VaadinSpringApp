package de.derpeterson.app.service;

import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
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

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusBroadcaster userStatusBroadcaster;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void saveUser(UserEntity userEntity) {
        save(userEntity);
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
        List<Long> admins = lockAndReadAdmins();
        ensureAdminChangeAllowed(user.getId(), isEnabledAdmin(user), admins);
        userRepository.save(user);
        userRepository.flush();
        ensureAdminsRemain(admins);
    }

    @Transactional
    public void updateUserLocale(String email, Locale locale) {
        Assert.isTrue(ValidationHelper.isEmailValid(email), "Invalid email");
        validateLocale(locale);
        Optional<UserEntity> userOptional = findByEmail(email);
        userOptional.ifPresent(user -> {
            user.setPreferredLocale(locale);
            save(user);
        });
    }

    public record StatusUpdate(UserStatus status, boolean manuallySet) {
    }

    @Transactional
    public StatusUpdate updateUserStatus(UserEntity userEntity, UserStatus newStatus, boolean manualChange) {
        Assert.notNull(userEntity, "Entity must not be null");
        Assert.notNull(userEntity.getId(), "User ID must not be null");
        Assert.notNull(newStatus, "Status must not be null");
        UserEntity user = userRepository.findById(userEntity.getId()).orElseThrow(
                () -> new IllegalStateException("Der Benutzer ist nicht mehr vorhanden."));
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
            StatusChangeSynchronization pending = TransactionSynchronizationManager.getSynchronizations().stream()
                    .filter(StatusChangeSynchronization.class::isInstance)
                    .map(StatusChangeSynchronization.class::cast)
                    .filter(change -> Objects.equals(change.userId, user.getId()))
                    .findFirst().orElse(null);
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
                userStatusBroadcaster.broadcast(new UserStatusBroadcaster.UserStatusMessage(userId, oldStatus.name(), newStatus.name()));
            } catch (RuntimeException exception) {
                log.error("Statuszustellung nach Commit fehlgeschlagen für Benutzer {}", userId, exception);
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

    /** Rechecks a scheduler candidate under a per-user lock, including non-versioned activity. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateScheduledStatus(Long userId, UserStatus target, LocalDateTime cutoff) {
        Assert.notNull(userId, "User ID must not be null");
        Assert.notNull(cutoff, "Cutoff must not be null");
        Assert.isTrue(target == UserStatus.ABSENT || target == UserStatus.AVAILABLE, "Invalid scheduler status");
        UserEntity user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null) {
            return;
        }
        boolean eligible = target == UserStatus.ABSENT
                ? user.getStatus() == UserStatus.AVAILABLE && user.getLastActivity().isBefore(cutoff)
                : user.getStatus() == UserStatus.ABSENT && !user.isStatusManuallySet() && user.getLastActivity().isAfter(cutoff);
        if (eligible) {
            updateUserStatus(user, target, false);
        }
    }

    /** Applies only admin-editable fields; the snapshot version is fixed when the form opens. */
    @Transactional
    public void updateAdminUser(UserEntity edited, Long expectedVersion, String rawPassword) {
        Assert.notNull(edited, "Entity must not be null");
        Assert.notNull(edited.getId(), "User ID must not be null");
        List<Long> admins = lockAndReadAdmins();
        UserEntity stored = userRepository.findById(edited.getId()).orElseThrow(
                () -> new OptimisticLockingFailureException("Der Benutzer wurde inzwischen gelöscht."));
        if (expectedVersion == null || !Objects.equals(expectedVersion, stored.getVersion())) {
            throw new OptimisticLockingFailureException("Der Benutzer wurde inzwischen geändert. Bitte das Formular neu öffnen und die Änderungen prüfen.");
        }
        validateUserFields(edited);
        if (rawPassword != null && !rawPassword.isBlank()) {
            Assert.isTrue(ValidationHelper.isPasswordSecure(rawPassword), "Invalid password");
        }
        ensureAdminChangeAllowed(stored.getId(), isEnabledAdmin(edited), admins);
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
        ensureAdminsRemain(admins);
    }

    public List<UserEntity> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(UserEntity user) {
        Assert.notNull(user, "Entity must not be null");
        Assert.notNull(user.getId(), "User ID must not be null");
        List<Long> admins = lockAndReadAdmins();
        ensureAdminChangeAllowed(user.getId(), false, admins);
        userRepository.delete(user);
        userRepository.flush();
        ensureAdminsRemain(admins);
    }

    private List<Long> lockAndReadAdmins() {
        userRepository.lockRole(RoleType.ROLE_ADMIN);
        return userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
    }

    private boolean removesLastAdmin(Long userId, boolean remainsAdmin, List<Long> admins) {
        return !remainsAdmin && userId != null && admins.size() == 1 && Objects.equals(admins.getFirst(), userId);
    }

    private void ensureAdminChangeAllowed(Long userId, boolean remainsAdmin, List<Long> admins) {
        if (removesLastAdmin(userId, remainsAdmin, admins)) {
            throw new IllegalStateException("Der letzte aktive Administrator muss erhalten bleiben.");
        }
    }

    private void ensureAdminsRemain(List<Long> before) {
        // flush can include other users already dirtied by a caller in the same
        // transaction. Reject that entire batch, not just the submitted entity.
        if (!before.isEmpty() && userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN).isEmpty()) {
            throw new IllegalStateException("Der letzte aktive Administrator muss erhalten bleiben.");
        }
    }

    public void updatePassword(UserEntity user, String rawPassword) {
        Assert.notNull(user, "Entity must not be null");
        Assert.isTrue(ValidationHelper.isPasswordSecure(rawPassword), "Invalid password");
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
        return user != null && user.getId() != null && !removesLastAdmin(user.getId(), false,
                userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    @Transactional(readOnly = true)
    public boolean wouldRemoveLastEnabledAdmin(Long editedUserId, boolean enabled, Collection<RoleEntity> roleEntities) {
        validateRoles(roleEntities);
        if (editedUserId == null || enabled && hasAdminRole(roleEntities)) {
            return false;
        }
        return removesLastAdmin(editedUserId, enabled && hasAdminRole(roleEntities),
                userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
    }

    private boolean isEnabledAdmin(UserEntity user) {
        return user != null && user.isEnabled() && user.hasRole(RoleType.ROLE_ADMIN);
    }

    private boolean hasAdminRole(Collection<RoleEntity> roleEntities) {
        return roleEntities != null && roleEntities.stream().anyMatch(roleEntity -> roleEntity.getName() == RoleType.ROLE_ADMIN);
    }

    private void validateUserFields(UserEntity user) {
        Assert.isTrue(ValidationHelper.isRequiredTextValid(user.getFirstName()), "First name must not be blank");
        Assert.isTrue(ValidationHelper.isRequiredTextValid(user.getLastName()), "Last name must not be blank");
        Assert.isTrue(ValidationHelper.isEmailValid(user.getEmail()), "Invalid email");
        Assert.notNull(user.getGender(), "Gender must not be null");
        Assert.isTrue(ValidationHelper.isBirthDateValid(user.getBirthDate()), "Birth date must be in the past");
        validateRoles(user.getRoleEntities());
    }

    private void validateRoles(Collection<RoleEntity> roles) {
        Assert.notNull(roles, "Roles must not be null");
        Assert.isTrue(roles.stream().allMatch(role -> role != null && role.getName() != null), "Invalid role");
    }

    private void validateLocale(Locale locale) {
        Assert.notNull(locale, "Locale must not be null");
    }
}
