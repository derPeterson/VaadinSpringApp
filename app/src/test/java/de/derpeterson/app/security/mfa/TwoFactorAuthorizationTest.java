package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.TwoFactorChallengeEntity.SetupProof;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.TwoFactorAccountRepository;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.repository.TwoFactorPolicyRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.security.SecurityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig({TwoFactorStoreTest.Config.class, TwoFactorAuthorizationTest.Config.class})
class TwoFactorAuthorizationTest {
  @Autowired TwoFactorAuthorization twoFactorAuthorization;
  @Autowired TwoFactorPolicies policies;
  @Autowired TwoFactorStore twoFactorStore;
  @Autowired TwoFactorPolicyRepository settings;
  @Autowired TwoFactorAccountRepository accounts;
  @Autowired UserRepository users;
  @Autowired TransactionTemplate transaction;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @Autowired EntityManagerFactory factory;
  @Autowired AdminRolePolicy administrators;
  @Autowired ObjectProvider<TwoFactorAuthorization> authorizations;
  @Autowired ObjectProvider<SecurityService> security;
  @Autowired TwoFactorChallengeRepository challenges;
  @Autowired JpaTransactionManager manager;
  @PersistenceContext EntityManager entityManager;
  private UserEntity testAccount;
  private MockHttpServletRequest mockHttpRequest;
  private List<String> recoveryCodes;

  @BeforeEach
  void setup() {
    clock.now.set(java.time.Instant.parse("2026-11-02T12:00:00Z"));
    settings.deleteAll();
    testAccount =
        transaction.execute(status -> {
          RoleEntity role =
              entityManager
                  .createQuery("select r from RoleEntity r where r.name = :name", RoleEntity.class)
                  .setParameter("name", RoleType.ROLE_SUPERADMIN)
                  .getResultStream()
                  .findFirst()
                  .orElse(null);
          if (role == null) {
            role = RoleEntity.builder().name(RoleType.ROLE_SUPERADMIN).build();
            entityManager.persist(role);
          }
          return users.saveAndFlush(
              UserEntity.builder()
                  .firstName("MFA")
                  .lastName("Fixture")
                  .email(UUID.randomUUID() + "@example.test")
                  .enabled(true)
                  .roleEntities(List.of(role))
                  .build());
        });
    mockHttpRequest = requestFor(testAccount.getEmail());
    UserDetails principal =
        User.withUsername(testAccount.getEmail())
            .password("unused")
            .roles("SUPERADMIN", "ADMIN")
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    UUID revision = twoFactorStore.initialize(testAccount.getId(), testAccount.getAccessVersion());
    byte[] secret = Totp.newSecret();
    recoveryCodes =
        twoFactorStore
            .enroll(
                testAccount.getId(),
                testAccount.getAccessVersion(),
                revision,
                secret,
                Totp.code(secret, clock.instant().getEpochSecond() / 30),
                false)
            .recoveryCodes();
  }

  static MockHttpServletRequest requestFor(String identity) {
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    mockHttpRequest.setSession(new MockHttpSession());
    mockHttpRequest.setAttribute("fixture.identity", identity);
    return mockHttpRequest;
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void firstFactorOrRememberedSessionDoesNotSatisfyTheCurrentLoginRule() {
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    String recoveryCode = recoveryCodes.get(0);
    assertThrows(
        AccessDeniedException.class,
        () ->
            twoFactorAuthorization.confirmAction(
                mockHttpRequest, TwoFactorAction.PASSWORD, recoveryCode));
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertFalse(twoFactorAuthorization.sessionConfirmed(requestFor(testAccount.getEmail())));
  }

  @Test
  void actionProofIsBoundToPurposeSessionAndSingleUse() {
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    assertThrows(
        AccessDeniedException.class,
        () ->
            twoFactorAuthorization.authorizeCurrent(
                mockHttpRequest, TwoFactorAction.PASSWORD, true));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.PASSWORD, recoveryCodes.get(1));
    assertThrows(
        AccessDeniedException.class,
        () ->
            twoFactorAuthorization.authorizeCurrent(mockHttpRequest, TwoFactorAction.EMAIL, true));
    MockHttpServletRequest another = requestFor(testAccount.getEmail());
    twoFactorAuthorization.confirmSession(another, recoveryCodes.get(2));
    assertThrows(
        AccessDeniedException.class,
        () -> twoFactorAuthorization.authorizeCurrent(another, TwoFactorAction.PASSWORD, true));
    twoFactorAuthorization.authorizeCurrent(mockHttpRequest, TwoFactorAction.PASSWORD, true);
    assertThrows(
        AccessDeniedException.class,
        () ->
            twoFactorAuthorization.authorizeCurrent(
                mockHttpRequest, TwoFactorAction.PASSWORD, true));
  }

  @Test
  void accountFactorAndSessionRotationInvalidateExistingProofs() {
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.EMAIL, recoveryCodes.get(1));
    UUID revision = accounts.state(testAccount.getId()).orElseThrow().getRevision();
    twoFactorStore.renewRecoveryCodes(
        testAccount.getId(), testAccount.getAccessVersion(), revision);
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertThrows(
        AccessDeniedException.class,
        () ->
            twoFactorAuthorization.authorizeCurrent(mockHttpRequest, TwoFactorAction.EMAIL, true));
    MockHttpSession original = (MockHttpSession) mockHttpRequest.getSession();
    original.changeSessionId();
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
  }

  @Test
  void ownConfirmedPasswordChangePreservesOnlyExistingSessionProofWithoutRenewingItsTime() {
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.EMAIL, recoveryCodes.get(1));
    MockHttpServletRequest other = requestFor(testAccount.getEmail());
    twoFactorAuthorization.confirmSession(other, recoveryCodes.get(2));
    String before =
        (String) mockHttpRequest.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    String previous = testAccount.getAccessVersion();
    clock.now.updateAndGet(now -> now.plusSeconds(60));
    transaction.executeWithoutResult(status -> {
      UserEntity account = users.findById(testAccount.getId()).orElseThrow();
      account.setPassword("new-fixture-hash");
      users.saveAndFlush(account);
    });
    String current = users.findById(testAccount.getId()).orElseThrow().getAccessVersion();
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    twoFactorAuthorization.retainAfterAccessVersionChange(mockHttpRequest, previous);
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    org.junit.jupiter.api.Assertions.assertEquals(
        before.replace(previous, current),
        mockHttpRequest.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF));
    assertFalse(twoFactorAuthorization.sessionConfirmed(other));
    assertThrows(
        AccessDeniedException.class,
        () ->
            twoFactorAuthorization.authorizeCurrent(mockHttpRequest, TwoFactorAction.EMAIL, true));
    MockHttpServletRequest unconfirmed = requestFor(testAccount.getEmail());
    twoFactorAuthorization.retainAfterAccessVersionChange(unconfirmed, previous);
    assertFalse(twoFactorAuthorization.sessionConfirmed(unconfirmed));
  }

  @Test
  void disablingPolicyRequiresProofUnderOldRuleAndDevelopmentGateCannotBeBypassed() {
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> relaxed = new EnumMap<>(RoleType.class);
    policies
        .current()
        .policy()
        .rules()
        .forEach((role, actions) -> {
          Map<TwoFactorAction, TwoFactorMode> changed = new EnumMap<>(actions);
          changed.replaceAll((action, value) -> TwoFactorMode.OFF);
          relaxed.put(role, changed);
        });
    TwoFactorPolicy desired =
        new TwoFactorPolicy(relaxed, false, Duration.ofHours(1), false, false);
    assertThrows(AccessDeniedException.class, () -> policies.save(mockHttpRequest, -1, desired));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.SECURITY_CONFIGURATION, recoveryCodes.get(1));
    TwoFactorPolicy development =
        new TwoFactorPolicy(relaxed, false, Duration.ofHours(1), true, true);
    assertThrows(
        AccessDeniedException.class, () -> policies.save(mockHttpRequest, -1, development));
    policies.save(mockHttpRequest, -1, desired);
    assertTrue(twoFactorAuthorization.sessionConfirmed(requestFor(testAccount.getEmail())));
    assertFalse(policies.weakGoogleAllowed());
  }

  @Test
  void sessionBoundPushReadsRequireFreshProofWithoutARequestOrSecurityContext() {
    HttpSession session = mockHttpRequest.getSession();
    when(security.getObject().getSessionUser(session)).thenReturn(Optional.of(testAccount));
    SecurityContextHolder.clearContext();
    assertThrows(AccessDeniedException.class, () -> twoFactorAuthorization.requireSession(session));
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.SECURITY_CONFIGURATION, recoveryCodes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    UserDetails principal =
        User.withUsername(testAccount.getEmail()).password("unused").roles("SUPERADMIN").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    policies.save(
        mockHttpRequest,
        -1,
        new TwoFactorPolicy(original.rules(), true, Duration.ofMinutes(1), false, false));
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(2));
    SecurityContextHolder.clearContext();
    assertDoesNotThrow(() -> twoFactorAuthorization.requireSession(session));
    String proof = (String) session.getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    clock.now.updateAndGet(now -> now.plusSeconds(60));
    assertThrows(AccessDeniedException.class, () -> twoFactorAuthorization.requireSession(session));
    assertEquals(proof, session.getAttribute(TwoFactorAuthorization.SESSION_PROOF));
    MockHttpSession unboundSession = new MockHttpSession();
    assertThrows(
        AccessDeniedException.class, () -> twoFactorAuthorization.requireSession(unboundSession));
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(3));
    assertDoesNotThrow(() -> twoFactorAuthorization.requireSession(session));
  }

  @Test
  void inactivityIsServerTimedAndExpiredProofCannotBeExtendedByActivity() {
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.SECURITY_CONFIGURATION, recoveryCodes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    policies.save(
        mockHttpRequest,
        -1,
        new TwoFactorPolicy(original.rules(), true, Duration.ofHours(1), false, false));
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(2));
    clock.now.updateAndGet(now -> now.plusSeconds(3599));
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    clock.now.updateAndGet(now -> now.plusSeconds(1));
    twoFactorAuthorization.recordActivity(mockHttpRequest);
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
  }

  @Test
  void activeRecoveryRestrictsItsSessionEvenWhenTheLoginRuleIsOff() {
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.SECURITY_CONFIGURATION, recoveryCodes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> rules = new EnumMap<>(original.rules());
    Map<TwoFactorAction, TwoFactorMode> superadmin =
        new EnumMap<>(rules.get(RoleType.ROLE_SUPERADMIN));
    superadmin.put(TwoFactorAction.LOGIN, TwoFactorMode.OFF);
    rules.put(RoleType.ROLE_SUPERADMIN, superadmin);
    TwoFactorPolicies.Snapshot saved =
        policies.save(
            mockHttpRequest,
            -1,
            new TwoFactorPolicy(rules, false, original.inactivity(), false, false));
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    String previousProof =
        mockHttpRequest.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF).toString();
    TwoFactorChallengeEntity recovery = new TwoFactorChallengeEntity();
    recovery.setUser(users.getReferenceById(testAccount.getId()));
    recovery.setSessionDigest(TwoFactorAuthorization.sessionDigest(mockHttpRequest.getSession()));
    recovery.setAccessVersion(testAccount.getAccessVersion());
    recovery.setFactorRevision(accounts.state(testAccount.getId()).orElseThrow().getRevision());
    recovery.setPolicyRevision(saved.revision());
    recovery.setAction(TwoFactorAction.MANAGE_FACTOR);
    recovery.setKind(Kind.ENROLLMENT);
    recovery.setSetupProof(SetupProof.RECOVERY_CODE);
    recovery.setCreatedAt(clock.instant());
    recovery.setExpiresAt(clock.instant().plusSeconds(300));
    challenges.saveAndFlush(recovery);
    assertFalse(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertTrue(twoFactorAuthorization.sessionConfirmed(requestFor(testAccount.getEmail())));
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    // Nach Ablauf gilt wieder die ausdrückliche Aus-Regel. Es wurde kein Nachweis erneuert.
    assertTrue(twoFactorAuthorization.sessionConfirmed(mockHttpRequest));
    assertEquals(
        previousProof,
        mockHttpRequest.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF));
  }

  @Test
  void startupGateAloneChangesNothingAndStoredDevelopmentFlagsAreIgnoredWithoutIt() {
    TwoFactorPolicies development =
        new TwoFactorPolicies(
            settings,
            users,
            SharedEntityManagerCreator.createSharedEntityManager(factory),
            administrators,
            authorizations,
            new MockEnvironment().withProperty("app.security.development-options-enabled", "true"));
    assertTrue(development.developmentOptionsAvailable());
    assertFalse(development.pauseEnabled(development.current()));
    assertFalse(development.weakGoogleAllowed());
    twoFactorAuthorization.confirmSession(mockHttpRequest, recoveryCodes.get(0));
    twoFactorAuthorization.confirmAction(
        mockHttpRequest, TwoFactorAction.SECURITY_CONFIGURATION, recoveryCodes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    transaction.executeWithoutResult(status ->
        development.save(
            mockHttpRequest,
            -1,
            new TwoFactorPolicy(original.rules(), false, original.inactivity(), true, true)));
    assertTrue(development.pauseEnabled(development.current()));
    assertTrue(development.weakGoogleAllowed());
    assertFalse(policies.pauseEnabled(policies.current()));
    assertFalse(policies.weakGoogleAllowed());
    MockHttpServletRequest newSession = requestFor(testAccount.getEmail());
    assertFalse(twoFactorAuthorization.sessionConfirmed(newSession));
    TwoFactorAuthorization relaxed =
        new TwoFactorAuthorization(
            security, users, accounts, challenges, development, twoFactorStore, manager, clock);
    assertTrue(relaxed.sessionConfirmed(newSession));
    assertNull(newSession.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF));
    assertThrows(
        AccessDeniedException.class,
        () -> relaxed.authorizeFactorGrant(newSession, TwoFactorAction.MANAGE_FACTOR, true));
  }

  @Configuration
  @EnableTransactionManagement
  static class Config {
    @Bean
    SecurityService securityService() {
      SecurityService security = mock(SecurityService.class);
      when(security.getAuthenticatedUser(any()))
          .thenAnswer(invocation -> {
            jakarta.servlet.http.HttpServletRequest invocationRequest = invocation.getArgument(0);
            Object identity = invocationRequest.getAttribute("fixture.identity");
            if (!(identity instanceof String email)) {
              return Optional.empty();
            }
            return Optional.of(User.withUsername(email).password("unused").roles("USER").build());
          });
      return security;
    }

    @Bean
    AdminRolePolicy adminRolePolicy(UserRepository users) {
      return new AdminRolePolicy(users);
    }

    @Bean
    TwoFactorPolicies policies(
        TwoFactorPolicyRepository repository,
        UserRepository users,
        EntityManagerFactory factory,
        AdminRolePolicy administrators,
        ObjectProvider<TwoFactorAuthorization> twoFactorAuthorization) {
      return new TwoFactorPolicies(
          repository,
          users,
          SharedEntityManagerCreator.createSharedEntityManager(factory),
          administrators,
          twoFactorAuthorization,
          new MockEnvironment());
    }

    @Bean
    TwoFactorAuthorization authorization(
        ObjectProvider<SecurityService> security,
        UserRepository users,
        TwoFactorAccountRepository accounts,
        TwoFactorChallengeRepository challenges,
        TwoFactorPolicies policies,
        TwoFactorStore store,
        JpaTransactionManager transactions,
        TwoFactorStoreTest.TestClock clock) {
      return new TwoFactorAuthorization(
          security, users, accounts, challenges, policies, store, transactions, clock);
    }
  }
}
