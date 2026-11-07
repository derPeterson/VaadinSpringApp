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
  @Autowired TwoFactorEnrollment enrollment;
  @Autowired TwoFactorAuthorization authorization;
  @Autowired TwoFactorStore store;
  @Autowired TwoFactorMail mail;
  @Autowired UserRepository users;
  @Autowired TwoFactorAccountRepository accounts;
  @Autowired TwoFactorRecoveryCodeRepository recoveryCodes;
  @Autowired TwoFactorChallengeRepository challenges;
  @Autowired TransactionTemplate transaction;
  @Autowired PlatformTransactionManager transactions;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @Autowired SecurityService security;
  @PersistenceContext EntityManager entityManager;
  private UserEntity user;
  private MockHttpServletRequest request;

  @BeforeEach
  void setup() {
    reset(mail);
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    user =
        transaction.execute(
            status -> {
              RoleEntity role = new RoleEntity();
              role.setName(RoleType.ROLE_USER);
              role =
                  entityManager
                      .createQuery(
                          "select r from RoleEntity r where r.name = :name", RoleEntity.class)
                      .setParameter("name", RoleType.ROLE_USER)
                      .getResultStream()
                      .findFirst()
                      .orElse(role);
              if (role.getId() == null) {
                entityManager.persist(role);
              }
              return users.saveAndFlush(
                  UserEntity.builder()
                      .firstName("Factor")
                      .lastName("Fixture")
                      .email(UUID.randomUUID() + "@example.test")
                      .password("hash")
                      .enabled(true)
                      .roleEntities(List.of(role))
                      .build());
            });
    request = TwoFactorAuthorizationTest.requestFor(user.getEmail());
  }

  @Test
  void passwordSetupProtectsDocumentsAndProducesOneIssuancePhase() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    assertEquals(Kind.ENROLLMENT, pending.kind());
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    MockHttpServletRequest otherRequest = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    UUID pendingId = pending.id();
    assertThrows(
        AccessDeniedException.class, () -> enrollment.pendingSecret(otherRequest, pendingId));
    UUID output = enrollment.complete(request, pending.id(), currentCode(secret));
    assertTrue(authorization.sessionConfirmed(request));
    List<String> codes = enrollment.recoveryOutput(request, output);
    assertEquals(10, codes.size());
    String code = currentCode(secret);
    assertThrows(AccessDeniedException.class, () -> enrollment.complete(request, pendingId, code));
    assertFalse(
        java.util.Collections.list(request.getSession().getAttributeNames()).stream()
            .map(name -> String.valueOf(request.getSession().getAttribute(name)))
            .anyMatch(
                value -> value.contains(Totp.base32(secret)) || value.contains(codes.getFirst())));
    enrollment.dismissOutput(request, output);
    assertThrows(AccessDeniedException.class, () -> enrollment.recoveryOutput(request, output));
    verify(mail).securityChange(any(), eq(TwoFactorMail.Event.ENABLED));
  }

  @Test
  void recoveryOutputRemainsAvailableUntilAcknowledgementInTheSameSession() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    UUID output =
        enrollment.complete(
            request, pending.id(), currentCode(enrollment.pendingSecret(request, pending.id())));
    List<String> originalCodes = enrollment.recoveryOutput(request, output);
    clock.now.updateAndGet(now -> now.plusSeconds(3600));
    transaction.executeWithoutResult(status -> challenges.clearExpiredSecrets(clock.instant()));

    assertEquals(originalCodes, enrollment.recoveryOutput(request, output));
    assertEquals(output, enrollment.pending(request).id());
    assertTrue(authorization.sessionConfirmed(request));
    assertDoesNotThrow(() -> enrollment.dismissOutput(request, output));
    assertNull(challenges.findById(output).orElseThrow().getEncryptedPayload());
    assertTrue(challenges.findById(output).orElseThrow().isConsumed());
    assertThrows(AccessDeniedException.class, () -> enrollment.recoveryOutput(request, output));
  }

  @Test
  void acknowledgementAfterRepeatedConfirmationOrMetadataDeletionIsIdempotent() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    UUID output =
        enrollment.complete(
            request, pending.id(), currentCode(enrollment.pendingSecret(request, pending.id())));
    clock.now.updateAndGet(now -> now.plusSeconds(301));
    transaction.executeWithoutResult(status -> challenges.clearExpiredSecrets(clock.instant()));

    assertDoesNotThrow(() -> enrollment.dismissOutput(request, output));
    assertDoesNotThrow(() -> enrollment.dismissOutput(request, output));
    transaction.executeWithoutResult(status -> challenges.deleteById(output));
    assertDoesNotThrow(() -> enrollment.dismissOutput(request, output));
    assertThrows(AccessDeniedException.class, () -> enrollment.recoveryOutput(request, output));
  }

  @Test
  void recoveryOutputCannotBeReopenedAfterSessionEnd() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    UUID output =
        enrollment.complete(
            request, pending.id(), currentCode(enrollment.pendingSecret(request, pending.id())));
    request.getSession().invalidate();
    assertThrows(AccessDeniedException.class, () -> enrollment.recoveryOutput(request, output));
  }

  @Test
  void enrollmentConfirmationStillExpiresAfterFiveMinutes() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(AccessDeniedException.class, () -> enrollment.complete(request, pendingId, code));
    assertFalse(authorization.state(authorization.owner(request)).enrolled());
  }

  @Test
  void acknowledgementRejectsAnotherSessionMissingProofAndAnEnrollmentChallenge() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    UUID pendingId = pending.id();
    assertThrows(AccessDeniedException.class, () -> enrollment.dismissOutput(request, pendingId));
    UUID output =
        enrollment.complete(
            request, pending.id(), currentCode(enrollment.pendingSecret(request, pending.id())));
    MockHttpServletRequest other = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    assertThrows(AccessDeniedException.class, () -> enrollment.dismissOutput(other, output));
    authorization.confirmSession(other, enrollment.recoveryOutput(request, output).getFirst());
    assertThrows(AccessDeniedException.class, () -> enrollment.dismissOutput(other, output));
    Object proof = request.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    request.getSession().removeAttribute(TwoFactorAuthorization.SESSION_PROOF);
    assertThrows(AccessDeniedException.class, () -> enrollment.dismissOutput(request, output));
    request.getSession().setAttribute(TwoFactorAuthorization.SESSION_PROOF, proof);
    assertEquals(10, enrollment.recoveryOutput(request, output).size());
  }

  @Test
  void freshPasswordSignInStartsOnlyRequiredFirstEnrollmentAndStillRequiresTotp() {
    Authentication authentication = freshPasswordSignIn();
    assertTrue(enrollment.canBeginFromPasswordLogin(request));
    TwoFactorEnrollment.Pending pending = enrollment.beginFromPasswordLogin(request);
    assertEquals(SetupProof.PASSWORD, pending.proof());
    assertFalse(authorization.state(authorization.owner(request)).enrolled());
    assertFalse(authorization.sessionConfirmed(request));
    assertEquals(authentication.getAuthorities(), freshAuthentication().getAuthorities());
    enrollment.complete(
        request, pending.id(), currentCode(enrollment.pendingSecret(request, pending.id())));
    assertTrue(authorization.sessionConfirmed(request));
    assertFalse(enrollment.canBeginFromPasswordLogin(request));
  }

  @Test
  void firstEnrollmentRejectsExpiredOrFuturePasswordEvidenceAndAnotherSession() {
    freshPasswordSignIn();
    assertFalse(
        enrollment.canBeginFromPasswordLogin(
            TwoFactorAuthorizationTest.requestFor(user.getEmail())));
    clock.now.updateAndGet(now -> now.minusSeconds(1));
    assertFalse(enrollment.canBeginFromPasswordLogin(request));
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    assertTrue(enrollment.canBeginFromPasswordLogin(request));
    clock.now.updateAndGet(now -> now.plusSeconds(1));
    assertFalse(enrollment.canBeginFromPasswordLogin(request));
    assertThrows(AccessDeniedException.class, () -> enrollment.beginFromPasswordLogin(request));
    assertNull(enrollment.pending(request));
    assertEquals(Kind.ENROLLMENT, enrollment.beginPassword(request, "correct").kind());
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
      when(security.refreshSessionAuthentication(request.getSession(false)))
          .thenReturn(Optional.of(other));
      assertFalse(enrollment.canBeginFromPasswordLogin(request));
      assertThrows(AccessDeniedException.class, () -> enrollment.beginFromPasswordLogin(request));
    }
  }

  @Test
  void optionalEnrollmentAndRemovedFactorsStillNeedExplicitProof() {
    freshPasswordSignIn();
    transaction.executeWithoutResult(
        status -> {
          UserEntity current = users.findById(user.getId()).orElseThrow();
          current.setRoleEntities(
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :role", RoleEntity.class)
                  .setParameter("role", RoleType.ROLE_USER)
                  .getResultList());
          users.saveAndFlush(current);
        });
    assertFalse(enrollment.canBeginFromPasswordLogin(request));
    freshPasswordSignIn();
    UUID revision = store.initialize(user.getId(), user.getAccessVersion());
    transaction.executeWithoutResult(
        status -> {
          TwoFactorAccountEntity account = accounts.findById(user.getId()).orElseThrow();
          account.setPreviouslyEnrolled(true);
          accounts.saveAndFlush(account);
        });
    assertFalse(enrollment.canBeginFromPasswordLogin(request));
    assertThrows(AccessDeniedException.class, () -> enrollment.beginFromPasswordLogin(request));
    assertEquals(revision, accounts.findById(user.getId()).orElseThrow().getRevision());
  }

  private Authentication freshPasswordSignIn() {
    transaction.executeWithoutResult(
        status -> {
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
          UserEntity current = users.findById(user.getId()).orElseThrow();
          current.setRoleEntities(new ArrayList<>(List.of(role)));
          users.saveAndFlush(current);
        });
    UserDetails principal =
        User.withUsername(user.getEmail()).password("").roles("SUPERADMIN").build();
    List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
    authorities.add(
        FactorGrantedAuthority.withAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)
            .issuedAt(clock.instant())
            .build());
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    when(security.refreshSessionAuthentication(request.getSession(false)))
        .thenReturn(Optional.of(authentication));
    return authentication;
  }

  private Authentication freshAuthentication() {
    return security.refreshSessionAuthentication(request.getSession(false)).orElseThrow();
  }

  @Test
  void aConcurrentFactorChangeAfterEnrollmentCannotConfirmTheSessionForAnotherRevision() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    TransactionTemplate concurrentChange = new TransactionTemplate(transactions);
    concurrentChange.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    doAnswer(
            invocation -> {
              // Reproduziert den Faktorwechsel zwischen Commit und Sitzungsfreigabe gezielt,
              // ohne zeitabhängige Thread-Rennen.
              TransactionSynchronizationManager.registerSynchronization(
                  new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                      concurrentChange.executeWithoutResult(
                          status -> {
                            TwoFactorAccountEntity account =
                                accounts.findById(user.getId()).orElseThrow();
                            account.setRevision(UUID.randomUUID());
                            accounts.saveAndFlush(account);
                          });
                    }
                  });
              return null;
            })
        .when(mail)
        .securityChange(any(), eq(TwoFactorMail.Event.ENABLED));

    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(AccessDeniedException.class, () -> enrollment.complete(request, pendingId, code));
    assertFalse(authorization.sessionConfirmed(request));
  }

  @Test
  void invalidPasswordAndExpiredSetupNeverCreateAFactor() {
    assertThrows(BadCredentialsException.class, () -> enrollment.beginPassword(request, "wrong"));
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(AccessDeniedException.class, () -> enrollment.complete(request, pendingId, code));
    assertFalse(authorization.state(authorization.owner(request)).enrolled());
  }

  @Test
  void securityMailFailureRollsBackTheFactorAndPreservesTheSetupAttempt() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    doThrow(new IllegalStateException("test queue unavailable"))
        .when(mail)
        .securityChange(any(), any());
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(IllegalStateException.class, () -> enrollment.complete(request, pendingId, code));
    assertFalse(authorization.state(authorization.owner(request)).enrolled());
    assertArrayEquals(secret, enrollment.pendingSecret(request, pending.id()));
  }

  @Test
  void googleFirstSetupNeedsMailCodeAndCannotReplaceAPreviousFactor() {
    // Der tatsächliche OIDC-/State-Check wird im ExternalLoginFlowTest geprüft. Hier startet
    // die Dienstgrenze mit einer vom Flow gelieferten vertrauenswürdigen Bestätigung.
    EnrollmentConfirmation confirmation = mock(EnrollmentConfirmation.class);
    when(confirmation.matches(any(), any(), any(), any())).thenReturn(true);
    when(confirmation.provider()).thenReturn("google");
    TwoFactorEnrollment.Pending pending =
        enrollment.afterProviderConfirmation(request, confirmation);
    assertEquals(Kind.GOOGLE_EMAIL, pending.kind());
    UUID pendingId = pending.id();
    assertThrows(AccessDeniedException.class, () -> enrollment.pendingSecret(request, pendingId));
    ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
    verify(mail).setupCode(any(), code.capture());
    assertThrows(
        BadCredentialsException.class,
        () -> enrollment.confirmEmail(request, pendingId, "invalid"));
    enrollment.confirmEmail(request, pending.id(), code.getValue());
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    enrollment.complete(request, pending.id(), currentCode(secret));
    assertThrows(
        AccessDeniedException.class,
        () -> enrollment.afterProviderConfirmation(request, confirmation));
    assertThrows(AccessDeniedException.class, () -> enrollment.beginPassword(request, "correct"));
  }

  @Test
  void emailResendInvalidatesPreviousCodeAndRemainsBoundToContactAndSession() {
    EnrollmentConfirmation confirmation = mock(EnrollmentConfirmation.class);
    when(confirmation.matches(any(), any(), any(), any())).thenReturn(true);
    when(confirmation.provider()).thenReturn("google");
    TwoFactorEnrollment.Pending first = enrollment.afterProviderConfirmation(request, confirmation);
    assertThrows(
        AccessDeniedException.class,
        () -> enrollment.afterProviderConfirmation(request, confirmation));
    clock.now.updateAndGet(now -> now.plusSeconds(61));
    TwoFactorEnrollment.Pending second =
        enrollment.afterProviderConfirmation(request, confirmation);
    ArgumentCaptor<String> codes = ArgumentCaptor.forClass(String.class);
    verify(mail, times(2)).setupCode(any(), codes.capture());
    UUID firstId = first.id();
    String firstEmailCode = codes.getAllValues().getFirst();
    assertThrows(
        AccessDeniedException.class,
        () -> enrollment.confirmEmail(request, firstId, firstEmailCode));
    MockHttpServletRequest otherRequest = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    UUID secondId = second.id();
    String secondEmailCode = codes.getAllValues().getLast();
    assertThrows(
        AccessDeniedException.class,
        () -> enrollment.confirmEmail(otherRequest, secondId, secondEmailCode));
    transaction.executeWithoutResult(
        status -> {
          UserEntity changed = users.findById(user.getId()).orElseThrow();
          changed.setEmail("changed-" + UUID.randomUUID() + "@example.test");
          users.saveAndFlush(changed);
        });
    MockHttpServletRequest changedRequest =
        TwoFactorAuthorizationTest.requestFor(
            users.findById(user.getId()).orElseThrow().getEmail());
    assertThrows(
        AccessDeniedException.class,
        () -> enrollment.confirmEmail(changedRequest, secondId, secondEmailCode));
  }

  @Test
  void invalidEmailAndFirstTotpCodesPersistTheirAccountWideLock() {
    EnrollmentConfirmation confirmation = mock(EnrollmentConfirmation.class);
    when(confirmation.matches(any(), any(), any(), any())).thenReturn(true);
    when(confirmation.provider()).thenReturn("google");
    TwoFactorEnrollment.Pending pending =
        enrollment.afterProviderConfirmation(request, confirmation);
    for (int attempt = 0; attempt < 4; attempt++) {
      UUID pendingId = pending.id();
      assertThrows(
          BadCredentialsException.class,
          () -> enrollment.confirmEmail(request, pendingId, "invalid"));
    }
    assertThrows(
        org.springframework.security.authentication.LockedException.class,
        () -> enrollment.confirmEmail(request, pending.id(), "invalid"));
    TwoFactorEnrollment.Pending password = enrollment.beginPassword(request, "correct");
    byte[] seed = enrollment.pendingSecret(request, password.id());
    assertThrows(
        org.springframework.security.authentication.LockedException.class,
        () -> enrollment.complete(request, password.id(), currentCode(seed)));
    assertFalse(authorization.state(authorization.owner(request)).enrolled());
  }

  @Test
  void replacementRequiresOwnFactorAndInvalidatesTheOldSeedAndCodesOnlyOnCompletion() {
    TwoFactorEnrollment.Pending first = enrollment.beginPassword(request, "correct");
    byte[] old = enrollment.pendingSecret(request, first.id());
    UUID output = enrollment.complete(request, first.id(), currentCode(old));
    List<String> oldCodes = enrollment.recoveryOutput(request, output);
    assertThrows(AccessDeniedException.class, () -> enrollment.beginReplacement(request));
    authorization.confirmAction(request, TwoFactorAction.MANAGE_FACTOR, oldCodes.getFirst());
    TwoFactorEnrollment.Pending replacement = enrollment.beginReplacement(request);
    assertTrue(authorization.state(authorization.owner(request)).enrolled());
    byte[] secret = enrollment.pendingSecret(request, replacement.id());
    enrollment.complete(request, replacement.id(), currentCode(secret));
    Long userId = user.getId();
    String accessVersion = user.getAccessVersion();
    String recoveryCode = oldCodes.get(1);
    assertThrows(
        BadCredentialsException.class, () -> store.verify(userId, accessVersion, recoveryCode));
    String previousFactorCode = currentCode(old);
    assertThrows(
        BadCredentialsException.class,
        () -> store.verify(userId, accessVersion, previousFactorCode));
  }

  private String currentCode(byte[] secret) {
    return Totp.code(secret, clock.instant().getEpochSecond() / 30);
  }

  @Test
  void lastRecoveryCodeAuthorizesOnlyReplacementUntilTheNewFirstTotp() {
    String last = lastRecoveryCode();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    assertFalse(authorization.sessionConfirmed(recovery));
    TwoFactorEnrollment.Pending pending = enrollment.beginRecovery(recovery, last);
    assertEquals(SetupProof.RECOVERY_CODE, pending.proof());
    assertEquals(1, recoveryCodes.remaining(user.getId()));
    assertFalse(authorization.sessionConfirmed(recovery));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(recovery, TwoFactorAction.PASSWORD, false));
    UUID pendingId = pending.id();
    assertThrows(AccessDeniedException.class, () -> enrollment.pendingSecret(request, pendingId));
    byte[] secret = enrollment.pendingSecret(recovery, pending.id());
    UUID output = enrollment.complete(recovery, pending.id(), currentCode(secret));
    assertTrue(authorization.sessionConfirmed(recovery));
    assertFalse(authorization.sessionConfirmed(request));
    assertEquals(10, enrollment.recoveryOutput(recovery, output).size());
    assertEquals(SetupProof.RECOVERY_CODE, enrollment.pending(recovery).proof());
    Long userId = user.getId();
    String accessVersion = user.getAccessVersion();
    assertThrows(BadCredentialsException.class, () -> store.verify(userId, accessVersion, last));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(recovery, TwoFactorAction.PASSWORD, false));
  }

  @Test
  void expiredRecoveryPreparationDoesNotSpendTheLastCode() {
    String last = lastRecoveryCode();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    TwoFactorEnrollment.Pending pending = enrollment.beginRecovery(recovery, last);
    byte[] secret = enrollment.pendingSecret(recovery, pending.id());
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(AccessDeniedException.class, () -> enrollment.complete(recovery, pendingId, code));
    assertFalse(authorization.sessionConfirmed(recovery));
    assertEquals(1, recoveryCodes.remaining(user.getId()));
    TwoFactorEnrollment.Pending retry = enrollment.beginRecovery(recovery, last);
    assertNotEquals(pending.id(), retry.id());
    enrollment.complete(
        recovery, retry.id(), currentCode(enrollment.pendingSecret(recovery, retry.id())));
    assertTrue(authorization.sessionConfirmed(recovery));
    assertEquals(10, recoveryCodes.remaining(user.getId()));
  }

  @Test
  void invalidNewTotpAndMailFailureKeepTheOldFactorAndRecoveryCode() {
    String last = lastRecoveryCode();
    String oldSecret = accounts.findById(user.getId()).orElseThrow().getEncryptedSecret();
    TwoFactorEnrollment.Pending pending = enrollment.beginRecovery(request, last);
    byte[] secret = enrollment.pendingSecret(request, pending.id());
    UUID pendingId = pending.id();
    assertThrows(
        BadCredentialsException.class, () -> enrollment.complete(request, pendingId, "invalid"));
    assertEquals(1, recoveryCodes.remaining(user.getId()));
    doThrow(new IllegalStateException("test queue unavailable"))
        .when(mail)
        .securityChange(any(), any());
    String code = currentCode(secret);
    assertThrows(IllegalStateException.class, () -> enrollment.complete(request, pendingId, code));
    assertEquals(oldSecret, accounts.findById(user.getId()).orElseThrow().getEncryptedSecret());
    assertEquals(1, recoveryCodes.remaining(user.getId()));
    assertFalse(authorization.sessionConfirmed(request));
    reset(mail);
    enrollment.complete(request, pending.id(), currentCode(secret));
    assertTrue(authorization.sessionConfirmed(request));
  }

  @Test
  void codeConsumedByAnotherLoginCannotCompleteRecovery() {
    List<String> codes = initialRecoveryCodes();
    UUID before = authorization.state(authorization.owner(request)).revision();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    TwoFactorEnrollment.Pending pending = enrollment.beginRecovery(recovery, codes.getFirst());
    byte[] secret = enrollment.pendingSecret(recovery, pending.id());
    MockHttpServletRequest login = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    authorization.confirmSession(login, codes.getFirst());
    UUID pendingId = pending.id();
    String code = currentCode(secret);
    assertThrows(AccessDeniedException.class, () -> enrollment.complete(recovery, pendingId, code));
    assertEquals(before, authorization.state(authorization.owner(request)).revision());
    assertEquals(9, recoveryCodes.remaining(user.getId()));
    assertFalse(authorization.sessionConfirmed(recovery));
    assertTrue(authorization.sessionConfirmed(login));
  }

  @Test
  void parallelRecoveryWithOneCodeCompletesExactlyOnce() throws Exception {
    String last = lastRecoveryCode();
    MockHttpServletRequest first = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    MockHttpServletRequest second = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    TwoFactorEnrollment.Pending firstPending = enrollment.beginRecovery(first, last);
    TwoFactorEnrollment.Pending secondPending = enrollment.beginRecovery(second, last);
    String firstCode = currentCode(enrollment.pendingSecret(first, firstPending.id()));
    String secondCode = currentCode(enrollment.pendingSecret(second, secondPending.id()));
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
      assertEquals(10, enrollment.recoveryOutput(winner, output).size());
      assertNotEquals(
          authorization.sessionConfirmed(first), authorization.sessionConfirmed(second));
      assertEquals(10, recoveryCodes.remaining(user.getId()));
    }
    verify(mail, times(1)).securityChange(any(), eq(TwoFactorMail.Event.ENABLED));
  }

  @Test
  void recoveryChecksHaveTheSameAccountWideAttemptLimit() {
    String last = lastRecoveryCode();
    MockHttpServletRequest recovery = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    for (int attempt = 0; attempt < 4; attempt++) {
      assertThrows(
          BadCredentialsException.class, () -> enrollment.beginRecovery(recovery, "invalid"));
    }
    assertThrows(LockedException.class, () -> enrollment.beginRecovery(recovery, "invalid"));
    assertThrows(LockedException.class, () -> enrollment.beginRecovery(recovery, last));
    assertEquals(1, recoveryCodes.remaining(user.getId()));
    clock.now.updateAndGet(now -> now.plusSeconds(901));
    assertEquals(SetupProof.RECOVERY_CODE, enrollment.beginRecovery(recovery, last).proof());
    assertFalse(authorization.sessionConfirmed(recovery));
  }

  private List<String> initialRecoveryCodes() {
    TwoFactorEnrollment.Pending pending = enrollment.beginPassword(request, "correct");
    UUID output =
        enrollment.complete(
            request, pending.id(), currentCode(enrollment.pendingSecret(request, pending.id())));
    List<String> codes = enrollment.recoveryOutput(request, output);
    enrollment.dismissOutput(request, output);
    return codes;
  }

  private String lastRecoveryCode() {
    List<String> codes = initialRecoveryCodes();
    for (int index = 1; index < codes.size(); index++) {
      store.verify(user.getId(), user.getAccessVersion(), codes.get(index));
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
      return enrollment.complete(recovery, pending, code);
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
        TwoFactorAuthorization authorization,
        TwoFactorStore store,
        TwoFactorSecrets secrets,
        de.derpeterson.app.repository.TwoFactorChallengeRepository challenges,
        TwoFactorPolicies policies,
        ObjectProvider<AuthenticationManager> passwords,
        ObjectProvider<SessionManagementService> sessions,
        TwoFactorMail mail,
        PlatformTransactionManager transactions,
        TwoFactorStoreTest.TestClock clock) {
      return new TwoFactorEnrollment(
          authorization,
          store,
          secrets,
          challenges,
          policies,
          passwords,
          sessions,
          mail,
          transactions,
          clock);
    }
  }
}
