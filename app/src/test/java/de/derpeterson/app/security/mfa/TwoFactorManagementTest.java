package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.repository.TwoFactorRecoveryCodeRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.ExternalLoginConfiguration;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig({
  TwoFactorStoreTest.Config.class,
  TwoFactorAuthorizationTest.Config.class,
  TwoFactorEnrollmentTest.Config.class,
  TwoFactorManagementTest.Config.class
})
class TwoFactorManagementTest {
  @Autowired TwoFactorEnrollment twoFactorEnrollment;
  @Autowired TwoFactorAuthorization twoFactorAuthorization;
  @Autowired TwoFactorManagement twoFactorManagement;
  @Autowired TwoFactorStore twoFactorStore;
  @Autowired UserRepository userRepository;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @Autowired ExternalIdentityService externalIdentityService;
  @Autowired ExternalLoginConfiguration.Registrations providerRegistrations;
  @Autowired SecurityService securityService;
  @PersistenceContext EntityManager entityManager;
  private MockHttpServletRequest mockHttpRequest;
  private UserEntity testAccount;
  private List<String> recoveryCodes;

  @BeforeEach
  void setup() {
    when(externalIdentityService.connected(any())).thenReturn(List.of("google"));
    when(providerRegistrations.providers()).thenReturn(List.of("google"));
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    testAccount =
        transactionTemplate.execute(status -> {
          RoleEntity role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :name", RoleEntity.class)
                  .setParameter("name", RoleType.ROLE_USER)
                  .getResultStream()
                  .findFirst()
                  .orElse(null);
          if (role == null) {
            role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
            entityManager.persist(role);
          }
          return userRepository.saveAndFlush(
              UserEntity.builder()
                  .firstName("Manage")
                  .lastName("Fixture")
                  .email(UUID.randomUUID() + "@example.test")
                  .password("hash")
                  .enabled(true)
                  .roleEntities(List.of(role))
                  .build());
        });
    mockHttpRequest = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    UUID output =
        twoFactorEnrollment.complete(
            mockHttpRequest,
            pending.id(),
            Totp.code(secret, clock.instant().getEpochSecond() / 30));
    recoveryCodes = twoFactorEnrollment.recoveryOutput(mockHttpRequest, output);
    twoFactorEnrollment.dismissOutput(mockHttpRequest, output);
  }

  @Test
  void regeneratingCodesConsumesOwnGrantAndRevokesOldCodes() {
    assertThrows(
        AccessDeniedException.class, () -> twoFactorManagement.regenerate(mockHttpRequest));
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(1));
    String previousProof =
        (String) mockHttpRequest.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    clearInvocations(securityService);
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.MANAGE_RECOVERY_CODES, recoveryCodes.getFirst());
    clock.now.updateAndGet(now -> now.plusSeconds(60));
    UUID output = twoFactorManagement.regenerate(mockHttpRequest);
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    JsonMapper json = JsonMapper.builder().build();
    String currentProof =
        (String) mockHttpRequest.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    assertEquals(
        json.readTree(previousProof).get("verifiedAt"),
        json.readTree(currentProof).get("verifiedAt"));
    assertEquals(
        json.readTree(previousProof).get("activityAt"),
        json.readTree(currentProof).get("activityAt"));
    verify(securityService, never())
        .recordSecondFactor(any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
    assertEquals(10, twoFactorEnrollment.recoveryOutput(mockHttpRequest, output).size());
    Long userId = testAccount.getId();
    String accessVersion = testAccount.getAccessVersion();
    String recoveryCode = recoveryCodes.get(2);
    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, accessVersion, recoveryCode));
    assertThrows(
        AccessDeniedException.class, () -> twoFactorManagement.regenerate(mockHttpRequest));
  }

  @Test
  void removingAuthenticatorPreservesHistoryAndRequiresProtectedReenrollment() {
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.MANAGE_FACTOR, recoveryCodes.getFirst());
    twoFactorManagement.disable(mockHttpRequest);
    TwoFactorAuthorization.State state =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest));
    assertFalse(state.enrolled());
    assertTrue(state.previouslyEnrolled());
    assertEquals(0, twoFactorManagement.remainingCodes(mockHttpRequest));
  }

  @Test
  void googleOnlyAccountCannotRemoveItsOnlySafeFutureEnrollmentPath() {
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity account = userRepository.findById(testAccount.getId()).orElseThrow();
      // Das Setzen ist hier nur Fixture: echte Passwortänderung prüft die Faktorfreigabe.
      account.setPassword(null);
    });
    assertEquals(
        TwoFactorManagement.DisableProblem.NO_INDEPENDENT_ACCESS,
        twoFactorManagement.disableProblem(mockHttpRequest));
  }

  @Configuration
  static class Config {
    @Bean
    ExternalIdentityService identities() {
      return mock(ExternalIdentityService.class);
    }

    @Bean
    ExternalLoginConfiguration.Registrations providers() {
      return mock(ExternalLoginConfiguration.Registrations.class);
    }

    @Bean
    TwoFactorManagement management(
        TwoFactorAuthorization twoFactorAuthorization,
        TwoFactorPolicies policies,
        TwoFactorStore store,
        TwoFactorEnrollment twoFactorEnrollment,
        TwoFactorRecoveryCodeRepository recoveryCodes,
        TwoFactorChallengeRepository challenges,
        TwoFactorMail mail,
        ObjectProvider<SessionManagementService> sessions,
        ExternalIdentityService externalIdentityService,
        ExternalLoginConfiguration.Registrations providerRegistrations) {
      return new TwoFactorManagement(
          twoFactorAuthorization,
          policies,
          store,
          twoFactorEnrollment,
          recoveryCodes,
          challenges,
          mail,
          sessions,
          externalIdentityService,
          providerRegistrations);
    }
  }
}
