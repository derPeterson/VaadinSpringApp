package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

        userStatusBroadcaster.broadcast(new UserStatusBroadcaster.UserStatusMessage(userEntity.getId(), oldStatus.name(), newStatus.name()));
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
}
