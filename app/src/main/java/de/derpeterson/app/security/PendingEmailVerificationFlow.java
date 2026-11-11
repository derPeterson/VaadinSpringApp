package de.derpeterson.app.security;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Kurzlebiger Anmeldenachweis zum erneuten Mailversand, ohne SecurityContext oder Rollen. */
@Service
@RequiredArgsConstructor
public class PendingEmailVerificationFlow {
  private static final String SESSION_KEY = PendingEmailVerificationFlow.class.getName();

  private final UserRepository userRepository;
  private final VerificationService verificationService;

  private record PendingProof(
      Long userId, String email, String credentialVersion, Instant expiresAt)
      implements Serializable {}

  public static void start(
      HttpServletRequest httpRequest,
      EmailVerificationRequiredException verificationRequiredException) {
    httpRequest
        .getSession()
        .setAttribute(
            SESSION_KEY,
            new PendingProof(
                verificationRequiredException.getUserId(),
                verificationRequiredException.getEmail(),
                verificationRequiredException.getCredentialVersion(),
                Instant.now().plusSeconds(600)));
  }

  public static void startForProviderAccount(
      HttpServletRequest httpRequest, UserEntity accountUserEntity) {
    httpRequest
        .getSession()
        .setAttribute(
            SESSION_KEY,
            new PendingProof(
                accountUserEntity.getId(),
                accountUserEntity.getEmail(),
                PortalUserDetails.credentialVersion(
                    accountUserEntity.getAccessVersion(), accountUserEntity.getPassword()),
                Instant.now().plusSeconds(600)));
  }

  public static void cancel(HttpServletRequest httpRequest) {
    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession != null) {
      httpSession.removeAttribute(SESSION_KEY);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<UserEntity> currentAccount(HttpServletRequest httpRequest) {
    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession == null
        || !(httpSession.getAttribute(SESSION_KEY) instanceof PendingProof pendingProof)
        || !Instant.now().isBefore(pendingProof.expiresAt())) {
      cancel(httpRequest);
      return Optional.empty();
    }

    Optional<UserEntity> pendingUserEntity =
        userRepository
            .lockVerificationUser(pendingProof.userId())
            .filter(UserEntity::requiresEmailVerification)
            .filter(userEntity -> Objects.equals(pendingProof.email(), userEntity.getEmail()))
            .filter(userEntity ->
                Objects.equals(
                    pendingProof.credentialVersion(),
                    PortalUserDetails.credentialVersion(
                        userEntity.getAccessVersion(), userEntity.getPassword())));
    if (pendingUserEntity.isEmpty()) {
      cancel(httpRequest);
    }
    return pendingUserEntity;
  }

  @Transactional(rollbackFor = IOException.class)
  public boolean resendVerification(HttpServletRequest httpRequest) throws IOException {
    UserEntity pendingUserEntity =
        currentAccount(httpRequest)
            .orElseThrow(() -> new AccessDeniedException("Verification proof expired"));
    return verificationService.sendVerificationEmailByUser(pendingUserEntity);
  }
}
