package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
  @Autowired TwoFactorAuthorization authorization;
  @Autowired TwoFactorPolicies policies;
  @Autowired TwoFactorStore factors;
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
  private UserEntity user;
  private MockHttpServletRequest request;
  private List<String> codes;

  @BeforeEach
  void setup() {
    clock.now.set(java.time.Instant.parse("2026-11-02T12:00:00Z"));
    settings.deleteAll();
    user =
        transaction.execute(
            status -> {
              RoleEntity role =
                  entityManager
                      .createQuery(
                          "select r from RoleEntity r where r.name = :name", RoleEntity.class)
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
    request = requestFor(user.getEmail());
    UserDetails principal =
        User.withUsername(user.getEmail()).password("unused").roles("SUPERADMIN", "ADMIN").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    UUID revision = factors.initialize(user.getId(), user.getAccessVersion());
    byte[] secret = Totp.newSecret();
    codes =
        factors
            .enroll(
                user.getId(),
                user.getAccessVersion(),
                revision,
                secret,
                Totp.code(secret, clock.instant().getEpochSecond() / 30),
                false)
            .recoveryCodes();
  }

  static MockHttpServletRequest requestFor(String identity) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(new MockHttpSession());
    request.setAttribute("fixture.identity", identity);
    return request;
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void firstFactorOrRememberedSessionDoesNotSatisfyTheCurrentLoginRule() {
    assertFalse(authorization.sessionConfirmed(request));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.confirmAction(request, TwoFactorAction.PASSWORD, codes.get(0)));
    authorization.confirmSession(request, codes.get(0));
    assertTrue(authorization.sessionConfirmed(request));
    assertFalse(authorization.sessionConfirmed(requestFor(user.getEmail())));
  }

  @Test
  void actionProofIsBoundToPurposeSessionAndSingleUse() {
    authorization.confirmSession(request, codes.get(0));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(request, TwoFactorAction.PASSWORD, true));
    authorization.confirmAction(request, TwoFactorAction.PASSWORD, codes.get(1));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(request, TwoFactorAction.EMAIL, true));
    MockHttpServletRequest another = requestFor(user.getEmail());
    authorization.confirmSession(another, codes.get(2));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(another, TwoFactorAction.PASSWORD, true));
    authorization.authorizeCurrent(request, TwoFactorAction.PASSWORD, true);
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(request, TwoFactorAction.PASSWORD, true));
  }

  @Test
  void accountFactorAndSessionRotationInvalidateExistingProofs() {
    authorization.confirmSession(request, codes.get(0));
    authorization.confirmAction(request, TwoFactorAction.EMAIL, codes.get(1));
    UUID revision = accounts.state(user.getId()).orElseThrow().getRevision();
    factors.renewRecoveryCodes(user.getId(), user.getAccessVersion(), revision);
    assertFalse(authorization.sessionConfirmed(request));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(request, TwoFactorAction.EMAIL, true));
    MockHttpSession original = (MockHttpSession) request.getSession();
    original.changeSessionId();
    assertFalse(authorization.sessionConfirmed(request));
  }

  @Test
  void ownConfirmedPasswordChangePreservesOnlyExistingSessionProofWithoutRenewingItsTime() {
    authorization.confirmSession(request, codes.get(0));
    authorization.confirmAction(request, TwoFactorAction.EMAIL, codes.get(1));
    MockHttpServletRequest other = requestFor(user.getEmail());
    authorization.confirmSession(other, codes.get(2));
    String before =
        (String) request.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    String previous = user.getAccessVersion();
    clock.now.updateAndGet(now -> now.plusSeconds(60));
    transaction.executeWithoutResult(
        status -> {
          UserEntity account = users.findById(user.getId()).orElseThrow();
          account.setPassword("new-fixture-hash");
          users.saveAndFlush(account);
        });
    String current = users.findById(user.getId()).orElseThrow().getAccessVersion();
    assertFalse(authorization.sessionConfirmed(request));
    authorization.retainAfterPasswordChange(request, previous);
    assertTrue(authorization.sessionConfirmed(request));
    org.junit.jupiter.api.Assertions.assertEquals(
        before.replace(previous, current),
        request.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF));
    assertFalse(authorization.sessionConfirmed(other));
    assertThrows(
        AccessDeniedException.class,
        () -> authorization.authorizeCurrent(request, TwoFactorAction.EMAIL, true));
    MockHttpServletRequest unconfirmed = requestFor(user.getEmail());
    authorization.retainAfterPasswordChange(unconfirmed, previous);
    assertFalse(authorization.sessionConfirmed(unconfirmed));
  }

  @Test
  void disablingPolicyRequiresProofUnderOldRuleAndDevelopmentGateCannotBeBypassed() {
    authorization.confirmSession(request, codes.get(0));
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> relaxed = new EnumMap<>(RoleType.class);
    policies
        .current()
        .policy()
        .rules()
        .forEach(
            (role, actions) -> {
              Map<TwoFactorAction, TwoFactorMode> changed = new EnumMap<>(actions);
              changed.replaceAll((action, value) -> TwoFactorMode.OFF);
              relaxed.put(role, changed);
            });
    TwoFactorPolicy desired =
        new TwoFactorPolicy(relaxed, false, Duration.ofHours(1), false, false);
    assertThrows(AccessDeniedException.class, () -> policies.save(request, -1, desired));
    authorization.confirmAction(request, TwoFactorAction.SECURITY_CONFIGURATION, codes.get(1));
    TwoFactorPolicy development =
        new TwoFactorPolicy(relaxed, false, Duration.ofHours(1), true, true);
    assertThrows(AccessDeniedException.class, () -> policies.save(request, -1, development));
    policies.save(request, -1, desired);
    assertTrue(authorization.sessionConfirmed(requestFor(user.getEmail())));
    assertFalse(policies.weakGoogleAllowed());
  }

  @Test
  void sessionBoundPushReadsRequireFreshProofWithoutARequestOrSecurityContext() {
    HttpSession session = request.getSession();
    when(security.getObject().getSessionUser(session)).thenReturn(Optional.of(user));
    SecurityContextHolder.clearContext();
    assertThrows(AccessDeniedException.class, () -> authorization.requireSession(session));
    authorization.confirmSession(request, codes.get(0));
    authorization.confirmAction(request, TwoFactorAction.SECURITY_CONFIGURATION, codes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    UserDetails principal =
        User.withUsername(user.getEmail()).password("unused").roles("SUPERADMIN").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
    policies.save(
        request,
        -1,
        new TwoFactorPolicy(original.rules(), true, Duration.ofMinutes(1), false, false));
    authorization.confirmSession(request, codes.get(2));
    SecurityContextHolder.clearContext();
    assertDoesNotThrow(() -> authorization.requireSession(session));
    String proof = (String) session.getAttribute(TwoFactorAuthorization.SESSION_PROOF);
    clock.now.updateAndGet(now -> now.plusSeconds(60));
    assertThrows(AccessDeniedException.class, () -> authorization.requireSession(session));
    assertEquals(proof, session.getAttribute(TwoFactorAuthorization.SESSION_PROOF));
    assertThrows(
        AccessDeniedException.class, () -> authorization.requireSession(new MockHttpSession()));
    authorization.confirmSession(request, codes.get(3));
    assertDoesNotThrow(() -> authorization.requireSession(session));
  }

  @Test
  void inactivityIsServerTimedAndExpiredProofCannotBeExtendedByActivity() {
    authorization.confirmSession(request, codes.get(0));
    authorization.confirmAction(request, TwoFactorAction.SECURITY_CONFIGURATION, codes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    policies.save(
        request,
        -1,
        new TwoFactorPolicy(original.rules(), true, Duration.ofHours(1), false, false));
    assertFalse(authorization.sessionConfirmed(request));
    authorization.confirmSession(request, codes.get(2));
    clock.now.updateAndGet(now -> now.plusSeconds(3599));
    assertTrue(authorization.sessionConfirmed(request));
    clock.now.updateAndGet(now -> now.plusSeconds(1));
    authorization.recordActivity(request);
    assertFalse(authorization.sessionConfirmed(request));
  }

  @Test
  void activeRecoveryRestrictsItsSessionEvenWhenTheLoginRuleIsOff() {
    authorization.confirmSession(request, codes.get(0));
    authorization.confirmAction(request, TwoFactorAction.SECURITY_CONFIGURATION, codes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> rules = new EnumMap<>(original.rules());
    Map<TwoFactorAction, TwoFactorMode> superadmin =
        new EnumMap<>(rules.get(RoleType.ROLE_SUPERADMIN));
    superadmin.put(TwoFactorAction.LOGIN, TwoFactorMode.OFF);
    rules.put(RoleType.ROLE_SUPERADMIN, superadmin);
    TwoFactorPolicies.Snapshot saved =
        policies.save(
            request, -1, new TwoFactorPolicy(rules, false, original.inactivity(), false, false));
    assertTrue(authorization.sessionConfirmed(request));
    String previousProof =
        request.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF).toString();
    TwoFactorChallengeEntity recovery = new TwoFactorChallengeEntity();
    recovery.setUser(users.getReferenceById(user.getId()));
    recovery.setSessionDigest(TwoFactorAuthorization.sessionDigest(request.getSession()));
    recovery.setAccessVersion(user.getAccessVersion());
    recovery.setFactorRevision(accounts.state(user.getId()).orElseThrow().getRevision());
    recovery.setPolicyRevision(saved.revision());
    recovery.setAction(TwoFactorAction.MANAGE_FACTOR);
    recovery.setKind(Kind.ENROLLMENT);
    recovery.setSetupProof(SetupProof.RECOVERY_CODE);
    recovery.setCreatedAt(clock.instant());
    recovery.setExpiresAt(clock.instant().plusSeconds(300));
    challenges.saveAndFlush(recovery);
    assertFalse(authorization.sessionConfirmed(request));
    assertTrue(authorization.sessionConfirmed(requestFor(user.getEmail())));
    clock.now.updateAndGet(now -> now.plusSeconds(300));
    // Nach Ablauf gilt wieder die ausdrückliche Aus-Regel. Es wurde kein Nachweis erneuert.
    assertTrue(authorization.sessionConfirmed(request));
    assertEquals(
        previousProof, request.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF));
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
    authorization.confirmSession(request, codes.get(0));
    authorization.confirmAction(request, TwoFactorAction.SECURITY_CONFIGURATION, codes.get(1));
    TwoFactorPolicy original = policies.current().policy();
    transaction.executeWithoutResult(
        status ->
            development.save(
                request,
                -1,
                new TwoFactorPolicy(original.rules(), false, original.inactivity(), true, true)));
    assertTrue(development.pauseEnabled(development.current()));
    assertTrue(development.weakGoogleAllowed());
    assertFalse(policies.pauseEnabled(policies.current()));
    assertFalse(policies.weakGoogleAllowed());
    MockHttpServletRequest newSession = requestFor(user.getEmail());
    assertFalse(authorization.sessionConfirmed(newSession));
    TwoFactorAuthorization relaxed =
        new TwoFactorAuthorization(
            security, users, accounts, challenges, development, factors, manager, clock);
    assertTrue(relaxed.sessionConfirmed(newSession));
    assertTrue(newSession.getSession().getAttribute(TwoFactorAuthorization.SESSION_PROOF) == null);
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
          .thenAnswer(
              invocation -> {
                jakarta.servlet.http.HttpServletRequest request = invocation.getArgument(0);
                Object identity = request.getAttribute("fixture.identity");
                if (!(identity instanceof String email)) {
                  return Optional.empty();
                }
                return Optional.of(
                    User.withUsername(email).password("unused").roles("USER").build());
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
        ObjectProvider<TwoFactorAuthorization> authorization) {
      return new TwoFactorPolicies(
          repository,
          users,
          SharedEntityManagerCreator.createSharedEntityManager(factory),
          administrators,
          authorization,
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
