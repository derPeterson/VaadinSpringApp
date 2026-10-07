package de.derpeterson.app.service;

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
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void save(UserEntity user) {
        Assert.notNull(user, "Entity must not be null");
        if (!isEnabledAdmin(user)) {
            ensureNotLastEnabledAdmin(user.getId());
        }
        userRepository.save(user);
    }

    @Transactional
    public void updateUserLocale(String email, Locale locale) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
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
        if (userRepository.updateLastActivity(userId, LocalDateTime.now()) == 0) {
            throw new IllegalStateException("Der Benutzer ist nicht mehr vorhanden.");
        }
    }

    /** Rechecks a scheduler candidate under a per-user lock, including non-versioned activity. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateScheduledStatus(Long userId, UserStatus target, LocalDateTime cutoff) {
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
        UserEntity stored = userRepository.findById(edited.getId()).orElseThrow(
                () -> new OptimisticLockingFailureException("Der Benutzer wurde inzwischen gelöscht."));
        if (expectedVersion == null || !Objects.equals(expectedVersion, stored.getVersion())) {
            throw new OptimisticLockingFailureException("Der Benutzer wurde inzwischen geändert. Bitte das Formular neu öffnen und die Änderungen prüfen.");
        }
        // Check before mutating the managed entity; preserve the existing last-admin guard.
        if (!isEnabledAdmin(edited)) {
            ensureNotLastEnabledAdmin(stored.getId());
        }
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
    }

    public List<UserEntity> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(UserEntity user) {
        Assert.notNull(user, "Entity must not be null");
        ensureNotLastEnabledAdmin(user.getId());
        userRepository.delete(user);
    }

    private void ensureNotLastEnabledAdmin(Long userId) {
        if (userId == null) {
            return;
        }
        List<Long> enabledAdminIds = userRepository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN);
        if (enabledAdminIds.size() == 1 && Objects.equals(enabledAdminIds.getFirst(), userId)) {
            throw new IllegalStateException("Der letzte aktive Administrator muss erhalten bleiben.");
        }
    }

    public void updatePassword(UserEntity user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
    }

    public boolean emailExistsForOtherUser(String email, Long currentUserId) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(existingUser -> existingUser.getEmail() != null)
                .anyMatch(existingUser -> existingUser.getEmail().trim().toLowerCase(Locale.ROOT).equals(normalizedEmail)
                        && (currentUserId == null || !Objects.equals(existingUser.getId(), currentUserId)));
    }

    public boolean canDeleteUser(UserEntity user) {
        if (user == null) {
            return false;
        }

        if (!isEnabledAdmin(user)) {
            return true;
        }

        long otherEnabledAdmins = userRepository.findAll().stream()
                .filter(existingUser -> !Objects.equals(existingUser.getId(), user.getId()))
                .filter(this::isEnabledAdmin)
                .count();

        return otherEnabledAdmins > 0;
    }

    public boolean wouldRemoveLastEnabledAdmin(Long editedUserId, boolean enabled, Collection<RoleEntity> roleEntities) {
        boolean userWillRemainEnabledAdmin = enabled && hasAdminRole(roleEntities);
        if (userWillRemainEnabledAdmin) {
            return false;
        }

        long otherEnabledAdmins = userRepository.findAll().stream()
                .filter(existingUser -> editedUserId == null || !Objects.equals(existingUser.getId(), editedUserId))
                .filter(this::isEnabledAdmin)
                .count();

        return otherEnabledAdmins == 0;
    }

    private boolean isEnabledAdmin(UserEntity user) {
        return user != null && user.isEnabled() && user.hasRole(RoleType.ROLE_ADMIN);
    }

    private boolean hasAdminRole(Collection<RoleEntity> roleEntities) {
        return roleEntities != null && roleEntities.stream().anyMatch(roleEntity -> roleEntity.getName() == RoleType.ROLE_ADMIN);
    }
}
