package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void saveUser(UserEntity userEntity) {
        userRepository.save(userEntity);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
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
        if (manualChange) {
            userEntity.setManualStatus(newStatus);
        } else {
            userEntity.setAutomaticStatus(newStatus);
        }

        userRepository.save(userEntity);
    }
}
