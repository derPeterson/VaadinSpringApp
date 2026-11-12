package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.AccountDeactivation;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.AccountLifecycleService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

class PendingAccountReactivationFlowTest {
  private final UserRepository userRepository = mock(UserRepository.class);
  private final AccountLifecycleService accountLifecycleService =
      mock(AccountLifecycleService.class);
  private final PendingAccountReactivationFlow reactivationFlow =
      new PendingAccountReactivationFlow(userRepository, accountLifecycleService);
  private final MockHttpServletRequest httpRequest = new MockHttpServletRequest();

  @Test
  void providerProofAllowsOnlyTheSavedAccountAndResendsWithItsCredentialVersion() {
    UserEntity accountUser = selfDeactivatedAccount();
    assertTrue(reactivationFlow.currentAccount(httpRequest).isEmpty());
    PendingAccountReactivationFlow.startForProviderAccount(httpRequest, accountUser);
    assertTrue(reactivationFlow.currentAccount(httpRequest).isPresent());

    reactivationFlow.resend(httpRequest);
    verify(accountLifecycleService)
        .requestReactivationLink(
            8L,
            PortalUserDetails.credentialVersion(
                accountUser.getAccessVersion(), accountUser.getPassword()));
    assertNull(SecurityContextHolder.getContext().getAuthentication());

    accountUser.rotateAccessVersion();
    assertTrue(reactivationFlow.currentAccount(httpRequest).isEmpty());
    assertThrows(AccessDeniedException.class, () -> reactivationFlow.resend(httpRequest));
  }

  @Test
  void administrativeBlockingDeadlineAndCancellationInvalidateTheProof() {
    UserEntity accountUser = selfDeactivatedAccount();
    PendingAccountReactivationFlow.startForProviderAccount(httpRequest, accountUser);
    accountUser.setAccountDeactivation(null);
    assertTrue(reactivationFlow.currentAccount(httpRequest).isEmpty());

    accountUser.setAccountDeactivation(
        new AccountDeactivation(Instant.now().minusSeconds(1), null, "expired", null, null, null));
    PendingAccountReactivationFlow.startForProviderAccount(httpRequest, accountUser);
    assertTrue(reactivationFlow.currentAccount(httpRequest).isEmpty());

    accountUser = selfDeactivatedAccount();
    PendingAccountReactivationFlow.startForProviderAccount(httpRequest, accountUser);
    PendingAccountReactivationFlow.cancel(httpRequest);
    assertTrue(reactivationFlow.currentAccount(httpRequest).isEmpty());
  }

  private UserEntity selfDeactivatedAccount() {
    UserEntity accountUser =
        UserEntity.builder()
            .id(8L)
            .enabled(false)
            .email("reactivate@example.test")
            .password("encoded-password")
            .build();
    accountUser.setAccountDeactivation(
        new AccountDeactivation(
            Instant.now().plusSeconds(3600), null, "test-digest", null, null, null));
    when(userRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(accountUser));
    return accountUser;
  }
}
