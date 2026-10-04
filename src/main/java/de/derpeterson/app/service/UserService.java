package de.derpeterson.app.service;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusBroadcaster userStatusBroadcaster;
    private final PasswordEncoder passwordEncoder;

    public void saveUser(UserEntity userEntity) {
        userRepository.save(userEntity);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void save(UserEntity user) {
        userRepository.save(user);
    }

    @Transactional
    public void updateUserLocale(String email, Locale locale) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        userOptional.ifPresent(user -> {
            user.setPreferredLocale(locale);
            userRepository.save(user);
        });
    }

    public void updateUserStatus(UserEntity userEntity, UserStatus newStatus, boolean manualChange) {
        UserStatus oldStatus = userEntity.getStatus();

        if (manualChange) {
            userEntity.setManualStatus(newStatus);
        } else {
            userEntity.setAutomaticStatus(newStatus);
        }

        userRepository.save(userEntity);

        UserStatus resultingStatus = userEntity.getStatus();
        if (oldStatus != resultingStatus) {
            userStatusBroadcaster.broadcast(new UserStatusBroadcaster.UserStatusMessage(userEntity.getId(), oldStatus.name(), resultingStatus.name()));
        }
    }

    public List<UserEntity> findAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(UserEntity user) {
        userRepository.delete(user);
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
