package de.derpeterson.app.service;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrative Mailaktionen lesen ausschließlich den gesperrten, gespeicherten Kontostand. */
@Service
@RequiredArgsConstructor
public class AdminAccountMailService {
  private final UserService userService;
  private final UserRepository userRepository;
  private final AdminRolePolicy adminRolePolicy;
  private final VerificationService verificationService;
  private final PasswordResetService passwordResetService;

  @Transactional(rollbackFor = IOException.class)
  public void createUser(UserEntity userEntity, String rawPassword, boolean skipEmailVerification)
      throws IOException {
    userService.createAdminUser(userEntity, rawPassword, skipEmailVerification);

    if (!skipEmailVerification
        && userEntity.isEnabled()
        && !verificationService.sendVerificationEmailByUser(userEntity)) {
      throw new IllegalStateException("Initial verification mail could not be queued");
    }
  }

  @Transactional(rollbackFor = IOException.class)
  public boolean resendVerification(Long userId, String expectedEmail) throws IOException {
    UserEntity storedUserEntity = lockAuthorizedUser(userId, expectedEmail);
    return storedUserEntity.requiresEmailVerification()
        && verificationService.sendVerificationEmailByUser(storedUserEntity);
  }

  @Transactional(rollbackFor = IOException.class)
  public boolean sendPasswordReset(Long userId, String expectedEmail) throws IOException {
    UserEntity storedUserEntity = lockAuthorizedUser(userId, expectedEmail);
    if (!storedUserEntity.isEnabled()
        || storedUserEntity.requiresEmailVerification()
        || storedUserEntity.getPassword() == null
        || storedUserEntity.getPassword().isBlank()) {
      return false;
    }

    Instant lastResetRequestInstant = storedUserEntity.getPasswordResetMailRequestedAt();
    if (lastResetRequestInstant != null
        && Instant.now().isBefore(lastResetRequestInstant.plusSeconds(60))) {
      return false;
    }

    boolean requestAccepted =
        passwordResetService.sendPasswordResetEmail(storedUserEntity.getEmail());
    if (requestAccepted) {
      storedUserEntity.setPasswordResetMailRequestedAt(Instant.now());
    }
    return requestAccepted;
  }

  private UserEntity lockAuthorizedUser(Long userId, String expectedEmail) {
    UserEntity storedUserEntity =
        userRepository
            .lockVerificationUser(userId)
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    adminRolePolicy.authorizeMutation(userId, Set.of());

    if (!Objects.equals(expectedEmail, storedUserEntity.getEmail())) {
      throw new AccessDeniedException("Stored account address changed");
    }

    return storedUserEntity;
  }
}
