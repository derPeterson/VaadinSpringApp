package de.derpeterson.app.security;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.AccountLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Kurzlebiger erster Anmeldenachweis, ausdrücklich ohne SecurityContext und Remember-me. */
@Service
@RequiredArgsConstructor
public class PendingAccountReactivationFlow {
  private static final String SESSION_KEY = PendingAccountReactivationFlow.class.getName();
  private final UserRepository userRepository;
  private final AccountLifecycleService accountLifecycleService;

  private record PendingProof(Long userId, String credentialVersion, Instant expiresAt)
      implements Serializable {}

  public static void start(
      HttpServletRequest httpRequest, AccountReactivationRequiredException requiredException) {
    storeProof(
        httpRequest, requiredException.getUserId(), requiredException.getCredentialVersion());
  }

  public static void startForProviderAccount(
      HttpServletRequest httpRequest, UserEntity accountUser) {
    storeProof(
        httpRequest,
        accountUser.getId(),
        PortalUserDetails.credentialVersion(
            accountUser.getAccessVersion(), accountUser.getPassword()));
  }

  private static void storeProof(
      HttpServletRequest httpRequest, Long userId, String credentialVersion) {
    httpRequest
        .getSession()
        .setAttribute(
            SESSION_KEY,
            new PendingProof(userId, credentialVersion, Instant.now().plusSeconds(600)));
  }

  public static void cancel(HttpServletRequest httpRequest) {
    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession != null) {
      httpSession.removeAttribute(SESSION_KEY);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<UserEntity> currentAccount(HttpServletRequest httpRequest) {
    PendingProof pendingProof = currentProof(httpRequest);
    if (pendingProof == null) {
      return Optional.empty();
    }
    Optional<UserEntity> accountUser =
        userRepository
            .findByIdForUpdate(pendingProof.userId())
            .filter(PendingAccountReactivationFlow::canReactivate)
            .filter(user ->
                Objects.equals(
                    pendingProof.credentialVersion(),
                    PortalUserDetails.credentialVersion(
                        user.getAccessVersion(), user.getPassword())));
    if (accountUser.isEmpty()) {
      cancel(httpRequest);
    }
    return accountUser;
  }

  public boolean resend(HttpServletRequest httpRequest) {
    PendingProof pendingProof = currentProof(httpRequest);
    if (pendingProof == null) {
      throw new AccessDeniedException("Reactivation proof expired");
    }
    // Die erneute Kontoprüfung und Queueaufnahme erfolgen atomar im zuständigen Dienst.
    return accountLifecycleService.requestReactivationLink(
        pendingProof.userId(), pendingProof.credentialVersion());
  }

  private PendingProof currentProof(HttpServletRequest httpRequest) {
    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession != null
        && httpSession.getAttribute(SESSION_KEY) instanceof PendingProof pendingProof
        && Instant.now().isBefore(pendingProof.expiresAt())) {
      return pendingProof;
    }
    cancel(httpRequest);
    return null;
  }

  public static boolean canReactivate(UserEntity accountUser) {
    return !accountUser.isEnabled()
        && accountUser.getAccountDeactivation() != null
        && accountUser.getAccountDeactivation().deletionAt().isAfter(Instant.now());
  }
}
