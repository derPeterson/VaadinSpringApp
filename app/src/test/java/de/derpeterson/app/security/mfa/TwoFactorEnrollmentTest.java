package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.TwoFactorAccountEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.TwoFactorChallengeEntity.SetupProof;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.TwoFactorAccountRepository;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.repository.TwoFactorRecoveryCodeRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.ExternalAuthenticationToken;
import de.derpeterson.app.security.ExternalLoginFlow.EnrollmentConfirmation;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionManagementService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig({
  TwoFactorStoreTest.Config.class,
  TwoFactorAuthorizationTest.Config.class,
  TwoFactorEnrollmentTest.Config.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TwoFactorEnrollmentTest {
  @Autowired TwoFactorEnrollment twoFactorEnrollment;
  @Autowired TwoFactorAuthorization twoFactorAuthorization;
  @Autowired TwoFactorStore twoFactorStore;
  @Autowired TwoFactorMail mail;
  @Autowired UserRepository userRepository;
  @Autowired TwoFactorAccountRepository twoFactorAccountRepository;
  @Autowired TwoFactorRecoveryCodeRepository recoveryCodes;
  @Autowired TwoFactorChallengeRepository challenges;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @Autowired SecurityService securityService;
  @PersistenceContext EntityManager entityManager;
  private UserEntity testAccount;
  private MockHttpServletRequest mockHttpRequest;

  @BeforeEach
  void setup() {
    reset(mail);
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    testAccount =
        transactionTemplate.execute(status -> {
          RoleEntity role = new RoleEntity();
          role.setName(RoleType.ROLE_USER);
          role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :name", RoleEntity.class)
                  .setParameter("name", RoleType.ROLE_USER)
                  .getResultStream()
                  .findFirst()
                  .orElse(role);
          if (role.getId() == null) {
            entityManager.persist(role);
          }
          return userRepository.saveAndFlush(
              UserEntity.builder()
                  .firstName("Factor")
                  .lastName("Fixture")
                  .email(UUID.randomUUID() + "@example.test")
                  .password("hash")
                  .enabled(true)
                  .roleEntities(List.of(role))
                  .build());
        });
    mockHttpRequest = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
  }

  @Test
  void passwordSetupProtectsDocumentsAndProducesOneIssuancePhase() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    assertEquals(Kind.ENROLLMENT, pending.kind());
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    MockHttpServletRequest otherRequest =
        TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    UUID pendingId = pending.id();
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.pendingSecret(otherRequest, pendingId));
    UUID output = twoFactorEnrollment.complete(mockHttpRequest, pending.id(), currentCode(secret));
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    List<String> codes = twoFactorEnrollment.recoveryOutput(mockHttpRequest, output);
    assertEquals(10, codes.size());
    String code = currentCode(secret);
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, pendingId, code));
    assertFalse(
        java.util.Collections.list(mockHttpRequest.getSession().getAttributeNames()).stream()
            .map(name -> String.valueOf(mockHttpRequest.getSession().getAttribute(name)))
            .anyMatch(
                value -> value.contains(Totp.base32(secret)) || value.contains(codes.getFirst())));
    twoFactorEnrollment.dismissOutput(mockHttpRequest, output);
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.recoveryOutput(mockHttpRequest, output));
    verify(mail).securityChange(any(), eq(TwoFactorMail.Event.ENABLED));
  }

  @Test
  void recoveryOutputRemainsAvailableUntilAcknowledgementInTheSameSession() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    UUID output =
        twoFactorEnrollment.complete(
            mockHttpRequest,
            pending.id(),
            currentCode(twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id())));
    List<String> originalCodes = twoFactorEnrollment.recoveryOutput(mockHttpRequest, output);
    clock.now.updateAndGet(now -> now.plusSeconds(3600));
    transactionTemplate.executeWithoutResult(
        status -> challenges.clearExpiredSecrets(clock.instant()));

    assertEquals(originalCodes, twoFactorEnrollment.recoveryOutput(mockHttpRequest, output));
    assertEquals(output, twoFactorEnrollment.pending(mockHttpRequest).id());
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertDoesNotThrow(() -> twoFactorEnrollment.dismissOutput(mockHttpRequest, output));
    assertNull(challenges.findById(output).orElseThrow().getEncryptedPayload());
    assertTrue(challenges.findById(output).orElseThrow().isConsumed());
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.recoveryOutput(mockHttpRequest, output));
  }

  @Test
  void acknowledgementAfterRepeatedConfirmationOrMetadataDeletionIsIdempotent() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    UUID output =
        twoFactorEnrollment.complete(
            mockHttpRequest,
            pending.id(),
            currentCode(twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id())));
    clock.now.updateAndGet(now -> now.plusSeconds(301));
    transactionTemplate.executeWithoutResult(
        status -> challenges.clearExpiredSecrets(clock.instant()));

    assertDoesNotThrow(() -> twoFactorEnrollment.dismissOutput(mockHttpRequest, output));
    assertDoesNotThrow(() -> twoFactorEnrollment.dismissOutput(mockHttpRequest, output));
    transactionTemplate.executeWithoutResult(status -> challenges.deleteById(output));
    assertDoesNotThrow(() -> twoFactorEnrollment.dismissOutput(mockHttpRequest, output));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.recoveryOutput(mockHttpRequest, output));
  }

  @Test
  void recoveryOutputCannotBeReopenedAfterSessionEnd() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    UUID output =
        twoFactorEnrollment.complete(
            mockHttpRequest,
            pending.id(),
            currentCode(twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id())));
    mockHttpRequest.getSession().invalidate();
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.recoveryOutput(mockHttpRequest, output));
  }

  @Test
  void enrollmentConfirmationStillExpiresAfterFiveMinutes() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, pendingId, code));
    assertFalse(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).enrolled());
  }

  @Test
  void acknowledgementRejectsAnotherSessionMissingProofAndAnEnrollmentChallenge() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    UUID pendingId = pending.id();
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.dismissOutput(mockHttpRequest, pendingId));
    UUID output =
        twoFactorEnrollment.complete(
            mockHttpRequest,
            pending.id(),
            currentCode(twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id())));
    MockHttpServletRequest other = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    assertThrows(
        AccessDeniedException.class, () -> twoFactorEnrollment.dismissOutput(other, output));
    twoFactorAuthorization.confirmSession(
        other, twoFactorEnrollment.recoveryOutput(mockHttpRequest, output).getFirst());
    assertThrows(
        AccessDeniedException.class, () -> twoFactorEnrollment.dismissOutput(other, output));
    Object proof = mockHttpRequest.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    mockHttpRequest.getSession().removeAttribute(TwoFactorAuthorization.SESSION_PROOF);
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.dismissOutput(mockHttpRequest, output));
    mockHttpRequest.getSession().setAttribute(TwoFactorAuthorization.SESSION_PROOF, proof);
    assertEquals(10, twoFactorEnrollment.recoveryOutput(mockHttpRequest, output).size());
  }

  @Test
  void freshPasswordSignInStartsOnlyRequiredFirstEnrollmentAndStillRequiresTotp() {
    Authentication authentication = freshPasswordSignIn();
    assertTrue(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginFromPasswordLogin(mockHttpRequest);
    assertEquals(SetupProof.PASSWORD, pending.proof());
    assertFalse(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).enrolled());
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertEquals(authentication.getAuthorities(), freshAuthentication().getAuthorities());
    twoFactorEnrollment.complete(
        mockHttpRequest,
        pending.id(),
        currentCode(twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id())));
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertFalse(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
  }

  @Test
  void firstEnrollmentRejectsExpiredOrFuturePasswordEvidenceAndAnotherSession() {
    freshPasswordSignIn();
    assertFalse(
        twoFactorEnrollment.canBeginFromPasswordLogin(
            TwoFactorAuthorizationTest.requestFor(testAccount.getEmail())));
    clock.now.updateAndGet(now -> now.minusSeconds(1));
    assertFalse(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    assertTrue(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
    clock.now.updateAndGet(now -> now.plusSeconds(1));
    assertFalse(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.beginFromPasswordLogin(mockHttpRequest));
    assertNull(twoFactorEnrollment.pending(mockHttpRequest));
    assertEquals(
        Kind.ENROLLMENT, twoFactorEnrollment.beginPassword(mockHttpRequest, "correct").kind());
  }

  @Test
  void rememberMeExternalLoginAndPlainAuthorityNeverSupplyFreshPasswordProof() {
    Authentication password = freshPasswordSignIn();
    UserDetails principal = (UserDetails) password.getPrincipal();
    for (Authentication other :
        List.of(
            new RememberMeAuthenticationToken("fixture", principal, password.getAuthorities()),
            new ExternalAuthenticationToken(principal, "google", password.getAuthorities()),
            new ExternalAuthenticationToken(principal, "microsoft", password.getAuthorities()),
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY))))) {
      when(securityService.refreshSessionAuthentication(mockHttpRequest.getSession(false)))
          .thenReturn(Optional.of(other));
      assertFalse(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
      assertThrows(
          AccessDeniedException.class,
          () -> twoFactorEnrollment.beginFromPasswordLogin(mockHttpRequest));
    }
  }

  @Test
  void optionalEnrollmentAndRemovedFactorsStillNeedExplicitProof() {
    freshPasswordSignIn();
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity current = userRepository.findById(testAccount.getId()).orElseThrow();
      current.setRoleEntities(
          entityManager
              .createQuery("select r from RoleEntity r where r.name = :role", RoleEntity.class)
              .setParameter("role", RoleType.ROLE_USER)
              .getResultList());
      userRepository.saveAndFlush(current);
    });
    assertFalse(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
    freshPasswordSignIn();
    UUID revision = twoFactorStore.initialize(testAccount.getId(), testAccount.getAccessVersion());
    transactionTemplate.executeWithoutResult(status -> {
      TwoFactorAccountEntity account =
          twoFactorAccountRepository.findById(testAccount.getId()).orElseThrow();
      account.setPreviouslyEnrolled(true);
      twoFactorAccountRepository.saveAndFlush(account);
    });
    assertFalse(twoFactorEnrollment.canBeginFromPasswordLogin(mockHttpRequest));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.beginFromPasswordLogin(mockHttpRequest));
    assertEquals(
        revision,
        twoFactorAccountRepository.findById(testAccount.getId()).orElseThrow().getRevision());
  }

  private Authentication freshPasswordSignIn() {
    transactionTemplate.executeWithoutResult(status -> {
      RoleEntity role =
          entityManager
              .createQuery("select r from RoleEntity r where r.name = :role", RoleEntity.class)
              .setParameter("role", RoleType.ROLE_SUPERADMIN)
              .getResultStream()
              .findFirst()
              .orElse(null);
      if (role == null) {
        role = RoleEntity.builder().name(RoleType.ROLE_SUPERADMIN).build();
        entityManager.persist(role);
      }
      UserEntity current = userRepository.findById(testAccount.getId()).orElseThrow();
      current.setRoleEntities(new ArrayList<>(List.of(role)));
      userRepository.saveAndFlush(current);
    });
    UserDetails principal =
        User.withUsername(testAccount.getEmail()).password("").roles("SUPERADMIN").build();
    List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
    authorities.add(
        FactorGrantedAuthority.withAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)
            .issuedAt(clock.instant())
            .build());
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    when(securityService.refreshSessionAuthentication(mockHttpRequest.getSession(false)))
        .thenReturn(Optional.of(authentication));
    return authentication;
  }

  private Authentication freshAuthentication() {
    return securityService
        .refreshSessionAuthentication(mockHttpRequest.getSession(false))
        .orElseThrow();
  }

  @Test
  void aConcurrentFactorChangeAfterEnrollmentCannotConfirmTheSessionForAnotherRevision() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    TransactionTemplate concurrentChange = new TransactionTemplate(transactionManager);
    concurrentChange.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    doAnswer(
            invocation -> {
              // Reproduziert den Faktorwechsel zwischen Commit und Sitzungsfreigabe gezielt,
              // ohne zeitabhängige Thread-Rennen.
              TransactionSynchronizationManager.registerSynchronization(
                  new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                      concurrentChange.executeWithoutResult(status -> {
                        TwoFactorAccountEntity account =
                            twoFactorAccountRepository.findById(testAccount.getId()).orElseThrow();
                        account.setRevision(UUID.randomUUID());
                        twoFactorAccountRepository.saveAndFlush(account);
                      });
                    }
                  });
              return null;
            })
        .when(mail)
        .securityChange(any(), eq(TwoFactorMail.Event.ENABLED));

    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, pendingId, code));
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
  }

  @Test
  void invalidPasswordAndExpiredSetupNeverCreateAFactor() {
    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorEnrollment.beginPassword(mockHttpRequest, "wrong"));
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, pendingId, code));
    assertFalse(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).enrolled());
  }

  @Test
  void securityMailFailureRollsBackTheFactorAndPreservesTheSetupAttempt() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    doThrow(new IllegalStateException("test queue unavailable"))
        .when(mail)
        .securityChange(any(), any());
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(
        IllegalStateException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, pendingId, code));
    assertFalse(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).enrolled());
    assertArrayEquals(secret, twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id()));
  }

  @Test
  void googleFirstSetupNeedsMailCodeAndCannotReplaceAPreviousFactor() {
    // Der tatsächliche OIDC-/State-Check wird im ExternalLoginFlowTest geprüft. Hier startet
    // die Dienstgrenze mit einer vom Flow gelieferten vertrauenswürdigen Bestätigung.
    EnrollmentConfirmation confirmation = mock(EnrollmentConfirmation.class);
    when(confirmation.matches(any(), any(), any(), any())).thenReturn(true);
    when(confirmation.provider()).thenReturn("google");
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.afterProviderConfirmation(mockHttpRequest, confirmation);
    assertEquals(Kind.GOOGLE_EMAIL, pending.kind());
    UUID pendingId = pending.id();
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.pendingSecret(mockHttpRequest, pendingId));
    ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
    verify(mail).setupCode(any(), code.capture());
    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorEnrollment.confirmEmail(mockHttpRequest, pendingId, "invalid"));
    twoFactorEnrollment.confirmEmail(mockHttpRequest, pending.id(), code.getValue());
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    twoFactorEnrollment.complete(mockHttpRequest, pending.id(), currentCode(secret));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.afterProviderConfirmation(mockHttpRequest, confirmation));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.beginPassword(mockHttpRequest, "correct"));
  }

  @Test
  void emailResendInvalidatesPreviousCodeAndRemainsBoundToContactAndSession() {
    EnrollmentConfirmation confirmation = mock(EnrollmentConfirmation.class);
    when(confirmation.matches(any(), any(), any(), any())).thenReturn(true);
    when(confirmation.provider()).thenReturn("google");
    TwoFactorEnrollment.Pending first =
        twoFactorEnrollment.afterProviderConfirmation(mockHttpRequest, confirmation);
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.afterProviderConfirmation(mockHttpRequest, confirmation));
    clock.now.updateAndGet(now -> now.plusSeconds(61));
    TwoFactorEnrollment.Pending second =
        twoFactorEnrollment.afterProviderConfirmation(mockHttpRequest, confirmation);
    ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
    verify(mail, times(2)).setupCode(any(), codes.capture());
    UUID firstId = first.id();
    String firstEmailCode = codes.getAllValues().getFirst();
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.confirmEmail(mockHttpRequest, firstId, firstEmailCode));
    MockHttpServletRequest otherRequest =
        TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    UUID secondId = second.id();
    String secondEmailCode = codes.getAllValues().getLast();
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.confirmEmail(otherRequest, secondId, secondEmailCode));
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity changed = userRepository.findById(testAccount.getId()).orElseThrow();
      changed.setEmail("changed-" + UUID.randomUUID() + "@example.test");
      userRepository.saveAndFlush(changed);
    });
    MockHttpServletRequest changedRequest =
        TwoFactorAuthorizationTest.requestFor(
            userRepository.findById(testAccount.getId()).orElseThrow().getEmail());
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.confirmEmail(changedRequest, secondId, secondEmailCode));
  }

  @Test
  void invalidEmailAndFirstTotpCodesPersistTheirAccountWideLock() {
    EnrollmentConfirmation confirmation = mock(EnrollmentConfirmation.class);
    when(confirmation.matches(any(), any(), any(), any())).thenReturn(true);
    when(confirmation.provider()).thenReturn("google");
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.afterProviderConfirmation(mockHttpRequest, confirmation);
    for (int attempt = 0; attempt < 4; attempt++) {
      UUID pendingId = pending.id();
      assertThrows(
          BadCredentialsException.class,
          () -> twoFactorEnrollment.confirmEmail(mockHttpRequest, pendingId, "invalid"));
    }
    assertThrows(
        org.springframework.security.authentication.LockedException.class,
        () -> twoFactorEnrollment.confirmEmail(mockHttpRequest, pending.id(), "invalid"));
    TwoFactorEnrollment.Pending password =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    byte[] seed = twoFactorEnrollment.pendingSecret(mockHttpRequest, password.id());
    assertThrows(
        org.springframework.security.authentication.LockedException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, password.id(), currentCode(seed)));
    assertFalse(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).enrolled());
  }

  @Test
  void replacementRequiresOwnFactorAndInvalidatesTheOldSeedAndCodesOnlyOnCompletion() {
    TwoFactorEnrollment.Pending first =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    byte[] old = twoFactorEnrollment.pendingSecret(mockHttpRequest, first.id());
    UUID output = twoFactorEnrollment.complete(mockHttpRequest, first.id(), currentCode(old));
    List<String> oldCodes = twoFactorEnrollment.recoveryOutput(mockHttpRequest, output);
    assertThrows(
        AccessDeniedException.class, () -> twoFactorEnrollment.beginReplacement(mockHttpRequest));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.MANAGE_FACTOR, oldCodes.getFirst());
    TwoFactorEnrollment.Pending replacement = twoFactorEnrollment.beginReplacement(mockHttpRequest);
    assertTrue(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).enrolled());
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, replacement.id());
    twoFactorEnrollment.complete(mockHttpRequest, replacement.id(), currentCode(secret));
    Long userId = testAccount.getId();
    String accessVersion = testAccount.getAccessVersion();
    String recoveryCode = oldCodes.get(1);
    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, accessVersion, recoveryCode));
    String previousFactorCode = currentCode(old);
    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, accessVersion, previousFactorCode));
  }

  private String currentCode(byte[] secret) {
    return Totp.code(secret, clock.instant().getEpochSecond() / 30);
  }

  @Test
  void lastRecoveryCodeAuthorizesOnlyReplacementUntilTheNewFirstTotp() {
    String last = lastRecoveryCode();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    assertFalse(twoFactorAuthorization.sessionConfirmed(recovery));
    TwoFactorEnrollment.Pending pending = twoFactorEnrollment.beginRecovery(recovery, last);
    assertEquals(SetupProof.RECOVERY_CODE, pending.proof());
    assertEquals(1, recoveryCodes.remaining(testAccount.getId()));
    assertFalse(twoFactorAuthorization.sessionConfirmed(recovery));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorAuthorization.authorizeCurrent(recovery, TwoFactorAction.PASSWORD, false));
    UUID pendingId = pending.id();
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorEnrollment.pendingSecret(mockHttpRequest, pendingId));
    byte[] secret = twoFactorEnrollment.pendingSecret(recovery, pending.id());
    UUID output = twoFactorEnrollment.complete(recovery, pending.id(), currentCode(secret));
    assertTrue(twoFactorAuthorization.sessionConfirmed(recovery));
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertEquals(10, twoFactorEnrollment.recoveryOutput(recovery, output).size());
    assertEquals(SetupProof.RECOVERY_CODE, twoFactorEnrollment.pending(recovery).proof());
    Long userId = testAccount.getId();
    String accessVersion = testAccount.getAccessVersion();
    assertThrows(
        BadCredentialsException.class, () -> twoFactorStore.verify(userId, accessVersion, last));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorAuthorization.authorizeCurrent(recovery, TwoFactorAction.PASSWORD, false));
  }

  @Test
  void expiredRecoveryPreparationDoesNotSpendTheLastCode() {
    String last = lastRecoveryCode();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    TwoFactorEnrollment.Pending pending = twoFactorEnrollment.beginRecovery(recovery, last);
    byte[] secret = twoFactorEnrollment.pendingSecret(recovery, pending.id());
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(
        AccessDeniedException.class, () -> twoFactorEnrollment.complete(recovery, pendingId, code));
    assertFalse(twoFactorAuthorization.sessionConfirmed(recovery));
    assertEquals(1, recoveryCodes.remaining(testAccount.getId()));
    TwoFactorEnrollment.Pending retry = twoFactorEnrollment.beginRecovery(recovery, last);
    assertNotEquals(pending.id(), retry.id());
    twoFactorEnrollment.complete(
        recovery, retry.id(), currentCode(twoFactorEnrollment.pendingSecret(recovery, retry.id())));
    assertTrue(twoFactorAuthorization.sessionConfirmed(recovery));
    assertEquals(10, recoveryCodes.remaining(testAccount.getId()));
  }

  @Test
  void invalidNewTotpAndMailFailureKeepTheOldFactorAndRecoveryCode() {
    String last = lastRecoveryCode();
    String oldSecret =
        twoFactorAccountRepository.findById(testAccount.getId()).orElseThrow().getEncryptedSecret();
    TwoFactorEnrollment.Pending pending = twoFactorEnrollment.beginRecovery(mockHttpRequest, last);
    byte[] secret = twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id());
    UUID pendingId = pending.id();
    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, pendingId, "invalid"));
    assertEquals(1, recoveryCodes.remaining(testAccount.getId()));
    doThrow(new IllegalStateException("test queue unavailable"))
        .when(mail)
        .securityChange(any(), any());
    String code = currentCode(secret);
    assertThrows(
        IllegalStateException.class,
        () -> twoFactorEnrollment.complete(mockHttpRequest, pendingId, code));
    assertEquals(
        oldSecret,
        twoFactorAccountRepository
            .findById(testAccount.getId())
            .orElseThrow()
            .getEncryptedSecret());
    assertEquals(1, recoveryCodes.remaining(testAccount.getId()));
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    reset(mail);
    twoFactorEnrollment.complete(mockHttpRequest, pending.id(), currentCode(secret));
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
  }

  @Test
  void codeConsumedByAnotherLoginCannotCompleteRecovery() {
    List<String> codes = initialRecoveryCodes();
    UUID before =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).revision();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginRecovery(recovery, codes.getFirst());
    byte[] secret = twoFactorEnrollment.pendingSecret(recovery, pending.id());
    MockHttpServletRequest login = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    twoFactorAuthorization.confirmSession(login, codes.getFirst());
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(
        AccessDeniedException.class, () -> twoFactorEnrollment.complete(recovery, pendingId, code));
    assertEquals(
        before,
        twoFactorAuthorization.state(twoFactorAuthorization.owner(mockHttpRequest)).revision());
    assertEquals(9, recoveryCodes.remaining(testAccount.getId()));
    assertFalse(twoFactorAuthorization.sessionConfirmed(recovery));
    assertTrue(twoFactorAuthorization.sessionConfirmed(login));
  }

  @Test
  void parallelRecoveryWithOneCodeCompletesExactlyOnce() throws Exception {
    String last = lastRecoveryCode();
    MockHttpServletRequest first = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    MockHttpServletRequest second = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    TwoFactorEnrollment.Pending firstPending = twoFactorEnrollment.beginRecovery(first, last);
    TwoFactorEnrollment.Pending secondPending = twoFactorEnrollment.beginRecovery(second, last);
    String firstCode = currentCode(twoFactorEnrollment.pendingSecret(first, firstPending.id()));
    String secondCode = currentCode(twoFactorEnrollment.pendingSecret(second, secondPending.id()));
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    reset(mail);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<UUID> firstResult =
          executor.submit(
              () -> completeAfterStart(ready, start, first, firstPending.id(), firstCode));
      Future<UUID> secondResult =
          executor.submit(
              () -> completeAfterStart(ready, start, second, secondPending.id(), secondCode));
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      UUID firstOutput = firstResult.get(10, TimeUnit.SECONDS);
      UUID secondOutput = secondResult.get(10, TimeUnit.SECONDS);
      assertNotEquals(firstOutput == null, secondOutput == null);
      MockHttpServletRequest winner = firstOutput != null ? first : second;
      UUID output = firstOutput != null ? firstOutput : secondOutput;
      assertEquals(10, twoFactorEnrollment.recoveryOutput(winner, output).size());
      assertNotEquals(
          twoFactorAuthorization.sessionConfirmed(first),
          twoFactorAuthorization.sessionConfirmed(second));
      assertEquals(10, recoveryCodes.remaining(testAccount.getId()));
    }
    verify(mail, times(1)).securityChange(any(), eq(TwoFactorMail.Event.ENABLED));
  }

  @Test
  void recoveryChecksHaveTheSameAccountWideAttemptLimit() {
    String last = lastRecoveryCode();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(testAccount.getEmail());
    for (int attempt = 0; attempt < 4; attempt++) {
      assertThrows(
          BadCredentialsException.class,
          () -> twoFactorEnrollment.beginRecovery(recovery, "invalid"));
    }
    assertThrows(
        LockedException.class, () -> twoFactorEnrollment.beginRecovery(recovery, "invalid"));
    assertThrows(LockedException.class, () -> twoFactorEnrollment.beginRecovery(recovery, last));
    assertEquals(1, recoveryCodes.remaining(testAccount.getId()));
    clock.now.updateAndGet(now -> now.plusSeconds(901));
    assertEquals(
        SetupProof.RECOVERY_CODE, twoFactorEnrollment.beginRecovery(recovery, last).proof());
    assertFalse(twoFactorAuthorization.sessionConfirmed(recovery));
  }

  private List<String> initialRecoveryCodes() {
    TwoFactorEnrollment.Pending pending =
        twoFactorEnrollment.beginPassword(mockHttpRequest, "correct");
    UUID output =
        twoFactorEnrollment.complete(
            mockHttpRequest,
            pending.id(),
            currentCode(twoFactorEnrollment.pendingSecret(mockHttpRequest, pending.id())));
    List<String> codes = twoFactorEnrollment.recoveryOutput(mockHttpRequest, output);
    twoFactorEnrollment.dismissOutput(mockHttpRequest, output);
    return codes;
  }

  private String lastRecoveryCode() {
    List<String> codes = initialRecoveryCodes();
    for (int index = 1; index < codes.size(); index++) {
      twoFactorStore.verify(testAccount.getId(), testAccount.getAccessVersion(), codes.get(index));
    }
    return codes.getFirst();
  }

  private UUID completeAfterStart(
      CountDownLatch ready,
      CountDownLatch start,
      MockHttpServletRequest recovery,
      UUID pending,
      String code)
      throws InterruptedException {
    ready.countDown();
    assertTrue(start.await(10, TimeUnit.SECONDS));
    try {
      return twoFactorEnrollment.complete(recovery, pending, code);
    } catch (AccessDeniedException _) {
      return null;
    }
  }

  @Configuration
  static class Config {
    @Bean
    TwoFactorMail twoFactorMail() {
      return mock(TwoFactorMail.class);
    }

    @Bean
    SessionManagementService sessions() {
      return mock(SessionManagementService.class);
    }

    @Bean
    AuthenticationManager passwordManager() {
      return authentication -> {
        if (!"correct".equals(authentication.getCredentials())) {
          throw new BadCredentialsException("wrong fixture password");
        }
        return UsernamePasswordAuthenticationToken.authenticated(
            authentication.getPrincipal(), null, List.of());
      };
    }

    @Bean
    TwoFactorEnrollment enrollment(
        TwoFactorAuthorization twoFactorAuthorization,
        TwoFactorStore twoFactorStore,
        TwoFactorSecrets secrets,
        de.derpeterson.app.repository.TwoFactorChallengeRepository challenges,
        TwoFactorPolicies policies,
        ObjectProvider<AuthenticationManager> passwords,
        ObjectProvider<SessionManagementService> sessions,
        TwoFactorMail mail,
        PlatformTransactionManager transactionManager,
        TwoFactorStoreTest.TestClock clock) {
      return new TwoFactorEnrollment(
          twoFactorAuthorization,
          twoFactorStore,
          secrets,
          challenges,
          policies,
          passwords,
          sessions,
          mail,
          transactionManager,
          clock);
    }
  }
}
