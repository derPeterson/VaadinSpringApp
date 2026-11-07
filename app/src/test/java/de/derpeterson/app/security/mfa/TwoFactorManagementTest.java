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
  @Autowired TwoFactorEnrollment enrollment;
  @Autowired TwoFactorAuthorization authorization;
  @Autowired TwoFactorManagement management;
  @Autowired TwoFactorStore factors;
  @Autowired UserRepository users;
  @Autowired TransactionTemplate transaction;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @Autowired ExternalIdentityService identities;
  @Autowired ExternalLoginConfiguration.Registrations providers;
  @Autowired SecurityService security;
  @PersistenceContext EntityManager entityManager;
  private MockHttpServletRequest request;
  private UserEntity user;
  private List<String> codes;

  @BeforeEach
  void setup() {
    when(identities.connected(any())).thenReturn(List.of("google"));
    when(providers.providers()).thenReturn(List.of("google"));
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    user =
        transaction.execute(
            status -> {
              RoleEntity role =
                  entityManager
                      .createQuery(
                          "select r from RoleEntity r where r.name = :name", RoleEntity.class)
                      .setParameter("name", RoleType.ROLE_USER)
                      .getResultStream()
                      .findFirst()
                      .orElse(null);
              if (role == null) {
                role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
                entityManager.persist(role);
              }
              return users.saveAndFlush(
                  UserEntity.builder()
                      .firstName("Manage")
                      .lastName("Fixture")
                      .email(UUID.randomUUID() + "@example.test")
                      .password("hash")
                      .enabled(true)
                      .roleEntities(List.of(role))
                      .build());
            });
    request = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    UUID output =
        enrollment.complete(
            request, pending.id(), Totp.code(secret, clock.instant().getEpochSecond() / 30));
    codes = enrollment.recoveryOutput(request, output);
    enrollment.dismissOutput(request, output);
  }

  @Test
  void regeneratingCodesConsumesOwnGrantAndRevokesOldCodes() {
    assertThrows(AccessDeniedException.class, () -> management.regenerate(request));
    authorization.confirmSession(request, codes.get(1));
    String previousProof =
        (String) request.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    clearInvocations(security);
    authorization.confirmAction(request, TwoFactorAction.MANAGE_RECOVERY_CODES, codes.getFirst());
    clock.now.updateAndGet(now -> now.plusSeconds(60));
    UUID output = management.regenerate(request);
    assertTrue(authorization.sessionConfirmed(request));
    JsonMapper json = JsonMapper.builder().build();
    String currentProof =
        (String) request.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    assertEquals(
        json.readTree(previousProof).get("verifiedAt"),
        json.readTree(currentProof).get("verifiedAt"));
    assertEquals(
        json.readTree(previousProof).get("activityAt"),
        json.readTree(currentProof).get("activityAt"));
    verify(security, never())
        .recordSecondFactor(any(), org.mockito.ArgumentMatchers.anyBoolean(), any());
    assertEquals(10, enrollment.recoveryOutput(request, output).size());
    Long userId = user.getId();
    String accessVersion = user.getAccessVersion();
    String recoveryCode = codes.get(2);
    assertThrows(
        BadCredentialsException.class, () -> factors.verify(userId, accessVersion, recoveryCode));
    assertThrows(AccessDeniedException.class, () -> management.regenerate(request));
  }

  @Test
  void removingAuthenticatorPreservesHistoryAndRequiresProtectedReenrollment() {
    authorization.confirmAction(request, TwoFactorAction.MANAGE_FACTOR, codes.getFirst());
    management.disable(request);
    TwoFactorAuthorization.State state = authorization.state(authorization.owner(request));
    assertFalse(state.enrolled());
    assertTrue(state.previouslyEnrolled());
    assertEquals(0, management.remainingCodes(request));
  }

  @Test
  void googleOnlyAccountCannotRemoveItsOnlySafeFutureEnrollmentPath() {
    transaction.executeWithoutResult(
        status -> {
          UserEntity account = users.findById(user.getId()).orElseThrow();
          // Das Setzen ist hier nur Fixture: echte Passwortänderung prüft die Faktorfreigabe.
          account.setPassword(null);
        });
    assertEquals(
        TwoFactorManagement.DisableProblem.NO_INDEPENDENT_ACCESS,
        management.disableProblem(request));
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
        TwoFactorAuthorization authorization,
        TwoFactorPolicies policies,
        TwoFactorStore store,
        TwoFactorEnrollment enrollment,
        TwoFactorRecoveryCodeRepository codes,
        TwoFactorChallengeRepository challenges,
        TwoFactorMail mail,
        ObjectProvider<SessionManagementService> sessions,
        ExternalIdentityService identities,
        ExternalLoginConfiguration.Registrations providers) {
      return new TwoFactorManagement(
          authorization,
          policies,
          store,
          enrollment,
          codes,
          challenges,
          mail,
          sessions,
          identities,
          providers);
    }
  }
}
