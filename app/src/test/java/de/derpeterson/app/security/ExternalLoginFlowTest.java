package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.ExternalLoginFlow.ProofProblem;
import de.derpeterson.app.security.ExternalLoginFlow.ProofRejected;
import de.derpeterson.app.security.ExternalLoginFlow.Purpose;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorEnrollment;
import de.derpeterson.app.security.mfa.TwoFactorPolicies;
import de.derpeterson.app.security.mfa.TwoFactorPolicy;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class ExternalLoginFlowTest {
  private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
  private final UserRepository users = mock(UserRepository.class);
  private final SecurityService security = mock(SecurityService.class);
  private final TwoFactorAuthorization twoFactor = mock(TwoFactorAuthorization.class);
  private final TwoFactorEnrollment enrollment = mock(TwoFactorEnrollment.class);
  private final TwoFactorPolicies policies = mock(TwoFactorPolicies.class);
  private final EntityManager entityManager = mock(EntityManager.class);
  private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
  private final AuthenticationManager passwords = mock(AuthenticationManager.class);
  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private ExternalLoginFlow flow;
  private ExternalLoginConfiguration.Registrations clients;
  private UserEntity owner;

  @BeforeEach
  void setup() {
    ExternalLoginProperties properties = new ExternalLoginProperties();
    properties.getGoogle().setClientId("own-client");
    properties.getGoogle().setEnabled(true);
    properties.getGoogle().setClientSecret("own-fixture-secret");
    properties.getMicrosoft().setClientId("own-client");
    properties.getMicrosoft().setEnabled(true);
    properties.getMicrosoft().setClientSecret("own-fixture-secret");
    clients = spy(new ExternalLoginConfiguration().externalRegistrations(properties));
    flow =
        new ExternalLoginFlow(
            clients,
            identities,
            users,
            mock(CustomUserDetailsService.class),
            security,
            mock(SessionManagementService.class),
            passwords,
            twoFactor,
            enrollment,
            policies,
            entityManager,
            transactions);
    owner = UserEntity.builder().id(1L).email("owner@example.test").enabled(true).build();
    when(twoFactor.sessionConfirmed(any())).thenReturn(true);
    when(users.findById(1L)).thenReturn(Optional.of(owner));
    when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
    when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    when(policies.current())
        .thenReturn(new TwoFactorPolicies.Snapshot(-1, TwoFactorPolicy.defaults()));
    when(twoFactor.owner(request))
        .thenReturn(
            new TwoFactorAuthorization.Owner(
                1L,
                owner.getEmail(),
                owner.getAccessVersion(),
                true,
                true,
                List.of(RoleType.ROLE_USER)));
    when(twoFactor.state(any()))
        .thenReturn(new TwoFactorAuthorization.State(false, false, false, null, null));
    when(passwords.authenticate(any()))
        .thenReturn(UsernamePasswordAuthenticationToken.authenticated("fixture", null, List.of()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void missingIssuerIsRejectedWithoutGrantingAccess() {
    OidcUser identity = mock(OidcUser.class);
    assertThrows(AccessDeniedException.class, () -> ExternalLoginFlow.evidence("google", identity));
  }

  private void pendingLink() {
    signedIn();
    flow.confirmPassword(request, "own-password", Purpose.LINK_MICROSOFT);
    assertEquals(
        "/oauth2/authorization/microsoft", flow.beginLink(request, "microsoft", Locale.GERMAN));
    flow.bindAuthorization(request, "microsoft", "link-state");
    request.setParameter("state", "link-state");
  }

  @Test
  void linkConsumesItsProofOnlyInsideTheLockedCallbackTransaction() throws Exception {
    pendingLink();
    assertEquals(Optional.of(Purpose.LINK_MICROSOFT), flow.confirmedPurpose(request));
    verify(twoFactor, never()).authorizeCurrent(request, TwoFactorAction.LINK_PROVIDER, true);
    flow.complete(request, response, oidc("microsoft", "new-subject", Instant.now()));
    InOrder ordered = inOrder(users, entityManager, twoFactor, identities, transactions);
    ordered.verify(users).findByIdForUpdate(1L);
    ordered.verify(entityManager).refresh(owner);
    ordered.verify(twoFactor).authorizeCurrent(request, TwoFactorAction.LINK_PROVIDER, true);
    ordered.verify(identities).link(eq(1L), eq(owner.getAccessVersion()), any());
    ordered.verify(transactions).commit(any());
    assertThrows(
        AccessDeniedException.class,
        () -> flow.authorize(request, owner, Purpose.LINK_MICROSOFT, true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"policy", "factor", "required"})
  void changeDuringProviderRedirectRejectsTheFinalLink(String change) {
    pendingLink();
    switch (change) {
      case "policy" ->
          when(policies.current())
              .thenReturn(new TwoFactorPolicies.Snapshot(0, TwoFactorPolicy.defaults()));
      case "factor" ->
          when(twoFactor.state(any()))
              .thenReturn(
                  new TwoFactorAuthorization.State(
                      true, true, false, UUID.randomUUID(), Instant.now()));
      default -> when(twoFactor.requires(any(), any())).thenReturn(true);
    }
    OAuth2AuthenticationToken microsoftLogin = oidc("microsoft", "new-subject", Instant.now());
    assertThrows(
        AccessDeniedException.class, () -> flow.complete(request, response, microsoftLogin));
    verify(identities, never()).link(any(), any(), any());
    verify(transactions).rollback(any());
  }

  @Test
  void redirectCannotExtendTheOriginalProofDeadline() {
    Instant start = Instant.now();
    signedIn();
    ReflectionTestUtils.setField(flow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    flow.confirmPassword(request, "own-password", Purpose.LINK_MICROSOFT);
    ReflectionTestUtils.setField(
        flow, "externalLoginClock", Clock.fixed(start.plusSeconds(299), ZoneOffset.UTC));
    flow.beginLink(request, "microsoft", Locale.GERMAN);
    flow.bindAuthorization(request, "microsoft", "link-state");
    request.setParameter("state", "link-state");
    ReflectionTestUtils.setField(
        flow, "externalLoginClock", Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
    OAuth2AuthenticationToken microsoftLogin = oidc("microsoft", "new-subject", start);
    assertThrows(
        AccessDeniedException.class, () -> flow.complete(request, response, microsoftLogin));
    verify(identities, never()).link(any(), any(), any());
  }

  @Test
  void factorProofStillRequiresItsStoredGrantWhenTheActionRuleIsOff() {
    signedIn();
    flow.confirmFactor(request, "own-code", Purpose.LINK_MICROSOFT);
    flow.beginLink(request, "microsoft", Locale.GERMAN);
    flow.bindAuthorization(request, "microsoft", "link-state");
    request.setParameter("state", "link-state");
    doThrow(new AccessDeniedException("fixture changed grant"))
        .when(twoFactor)
        .authorizeFactorGrant(request, TwoFactorAction.LINK_PROVIDER, true);
    OAuth2AuthenticationToken microsoftLogin = oidc("microsoft", "new-subject", Instant.now());
    assertThrows(
        AccessDeniedException.class, () -> flow.complete(request, response, microsoftLogin));
    verify(identities, never()).link(any(), any(), any());
  }

  @Test
  void pendingAttemptRegistrationAndGrantSurviveSessionSerialization() throws Exception {
    when(policies.weakGoogleAllowed()).thenReturn(true);
    flow.beginLogin(request, "google", true, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "state");
    roundTripSessionValues();
    request.setParameter("state", "state");
    flow.complete(request, response, oidc("subject", Instant.now()));
    roundTripSessionValues();
    assertTrue(flow.registration(request).remember());
    signedIn();
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "proof-state");
    request.setParameter("state", "proof-state");
    flow.complete(request, new MockHttpServletResponse(), oidc("subject", Instant.now()));
    roundTripSessionValues();
    flow.authorize(request, owner, Purpose.PASSWORD, true);
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, true));
  }

  private void roundTripSessionValues() throws Exception {
    java.util.Enumeration<String> names = request.getSession().getAttributeNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
        output.writeObject(request.getSession().getAttribute(name));
      }
      try (ObjectInputStream input =
          new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
        request.getSession().setAttribute(name, input.readObject());
      }
    }
  }

  private OAuth2AuthenticationToken oidc(String subject, Instant authenticated) {
    return oidc("google", subject, authenticated);
  }

  private OAuth2AuthenticationToken oidc(String provider, String subject, Instant authenticated) {
    Instant now = Instant.now();
    OidcIdToken token =
        OidcIdToken.withTokenValue("own-already-validated-fixture")
            .issuer(
                provider.equals("google")
                    ? "https://accounts.google.com"
                    : "https://login.microsoftonline.com/11111111-2222-3333-4444-555555555555/v2.0")
            .subject(subject)
            .audience(List.of("own-client"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .claim("email", "person@gmail.com")
            .claim("email_verified", true)
            .claims(claims -> {
              if (authenticated != null) {
                claims.put("auth_time", authenticated);
              }
            })
            .build();
    DefaultOidcUser user = new DefaultOidcUser(List.of(), token);
    return new OAuth2AuthenticationToken(user, List.of(), provider);
  }

  private void signedIn() {
    request.getSession();
    when(security.getAuthenticatedUser(request))
        .thenReturn(
            Optional.of(
                User.withUsername(owner.getEmail()).password("hash").roles("USER").build()));
    when(identities.connected(1L)).thenReturn(List.of("google", "microsoft"));
    when(identities.belongsTo(eq(1L), any())).thenReturn(true);
  }

  @Test
  void disablingAProviderRejectsBothItsCallbackAndAlreadyPendingRegistration() throws Exception {
    flow.beginLogin(request, "google", true, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "own-state");
    request.setParameter("state", "own-state");
    flow.complete(request, response, oidc("subject", Instant.now()));
    when(clients.providers()).thenReturn(List.of());
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
    assertThrows(
        AccessDeniedException.class,
        () -> flow.finishLogin(request, response, owner, "google", false));
    verify(security, never()).storeVerifiedAuthentication(any(), any(), any(), any(Boolean.class));
  }

  @Test
  void unknownIdentityProducesOnlyExpiringRegistrationAndPreservesRememberChoice()
      throws Exception {
    flow.beginLogin(request, "google", true, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "own-state");
    request.setParameter("state", "own-state");
    flow.complete(request, response, oidc("subject", Instant.now()));
    assertEquals("/external-registration", response.getRedirectedUrl());
    assertTrue(flow.registration(request).remember());
    assertEquals(Locale.GERMAN, flow.registration(request).locale());
    verify(security, never())
        .storeVerifiedAuthentication(
            any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    OAuth2AuthenticationToken replayedConfirmation = oidc("subject", Instant.now());
    assertThrows(
        AccessDeniedException.class, () -> flow.complete(request, response, replayedConfirmation));
  }

  @Test
  void mismatchedStateAndDirectAuthorizationNeverProduceRegistration() {
    assertThrows(
        AccessDeniedException.class, () -> flow.bindAuthorization(request, "google", "state"));
    flow.beginLogin(request, "google", false, Locale.ENGLISH);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "different");
    OAuth2AuthenticationToken mismatchedStateConfirmation = oidc("subject", Instant.now());
    assertThrows(
        AccessDeniedException.class,
        () -> flow.complete(request, response, mismatchedStateConfirmation));
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void sensitiveProofIsBoundToPurposeVersionSessionAndConsumedOnce(String provider)
      throws Exception {
    if (provider.equals("google")) {
      when(policies.weakGoogleAllowed()).thenReturn(true);
    }
    signedIn();
    flow.beginProof(request, provider, Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, provider, "state");
    request.setParameter("state", "state");
    flow.complete(request, response, oidc(provider, "subject", Instant.now()));
    flow.authorize(request, owner, Purpose.PASSWORD, false);
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.EMAIL, false));
    String previousVersion = owner.getAccessVersion();
    owner.rotateAccessVersion();
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
    owner.setAccessVersion(previousVersion);
    flow.authorize(request, owner, Purpose.PASSWORD, true);
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, true));
    flow.beginProof(request, provider, Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, provider, "next-state");
    request.setParameter("state", "next-state");
    flow.complete(request, new MockHttpServletResponse(), oidc(provider, "subject", Instant.now()));
    request.changeSessionId();
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
  }

  @Test
  void oldMicrosoftSessionCannotAuthorizeSensitiveChange() {
    signedIn();
    flow.beginProof(request, "microsoft", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "microsoft", "state");
    request.setParameter("state", "state");
    OAuth2AuthenticationToken staleConfirmation =
        oidc("microsoft", "subject", Instant.now().minusSeconds(600));
    ProofRejected stale =
        assertThrows(
            ProofRejected.class, () -> flow.complete(request, response, staleConfirmation));
    assertEquals(ProofProblem.FRESH_AUTHENTICATION_REQUIRED, stale.problem());
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void differentLinkedIdentityCannotAuthorizeSensitiveChange(String provider) {
    signedIn();
    flow.beginProof(request, provider, Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, provider, "state");
    request.setParameter("state", "state");
    when(identities.belongsTo(eq(1L), any())).thenReturn(false);
    OAuth2AuthenticationToken differentIdentityConfirmation =
        oidc(provider, "different-subject", Instant.now());
    ProofRejected mismatch =
        assertThrows(
            ProofRejected.class,
            () -> flow.complete(request, response, differentIdentityConfirmation));
    assertEquals(ProofProblem.IDENTITY_MISMATCH, mismatch.problem());
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void missingOrFutureMicrosoftAuthenticationTimeNeverGrantsAProof(boolean missing) {
    signedIn();
    flow.beginProof(request, "microsoft", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "microsoft", "state");
    request.setParameter("state", "state");
    OAuth2AuthenticationToken invalidTimeConfirmation =
        oidc("microsoft", "subject", missing ? null : Instant.now().plusSeconds(60));
    ProofRejected failure =
        assertThrows(
            ProofRejected.class, () -> flow.complete(request, response, invalidTimeConfirmation));
    assertEquals(ProofProblem.FRESH_AUTHENTICATION_REQUIRED, failure.problem());
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void explicitlyEnabledDevelopmentConfirmationAllowsMissingOrOldGoogleTime(boolean missing)
      throws Exception {
    when(policies.weakGoogleAllowed()).thenReturn(true);
    signedIn();
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "state");
    flow.complete(
        request, response, oidc("subject", missing ? null : Instant.now().minusSeconds(86400)));
    assertEquals("/account?section=security&tab=portal", response.getRedirectedUrl());
    flow.authorize(request, owner, Purpose.PASSWORD, true);
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, true));
    OAuth2AuthenticationToken replayedConfirmation = oidc("subject", null);
    assertThrows(
        AccessDeniedException.class, () -> flow.complete(request, response, replayedConfirmation));
  }

  @ParameterizedTest
  @ValueSource(strings = {"blocked", "version", "signed-out", "different-user"})
  void googleInterimConfirmationStillRequiresTheUnchangedLocalAccount(String change) {
    signedIn();
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "state");
    switch (change) {
      case "blocked" -> owner.setEnabled(false);
      case "version" -> owner.rotateAccessVersion();
      case "signed-out" ->
          when(security.getAuthenticatedUser(request)).thenReturn(Optional.empty());
      case "different-user" ->
          when(security.getAuthenticatedUser(request))
              .thenReturn(
                  Optional.of(
                      User.withUsername("different@example.test")
                          .password("hash")
                          .roles("USER")
                          .build()));
      default -> throw new IllegalArgumentException(change);
    }
    OAuth2AuthenticationToken changedAccountConfirmation = oidc("subject", null);
    assertThrows(
        AccessDeniedException.class,
        () -> flow.complete(request, response, changedAccountConfirmation));
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
  }

  @Test
  void microsoftEmailClaimIsNeverContactProof() {
    assertFalse(
        ExternalLoginFlow.evidence(
                "microsoft", (DefaultOidcUser) oidc("subject", Instant.now()).getPrincipal())
            .trustedEmail());
    assertTrue(
        ExternalLoginFlow.evidence(
                "google", (DefaultOidcUser) oidc("subject", Instant.now()).getPrincipal())
            .trustedEmail());
  }

  @Test
  void expiredAttemptAndRegistrationCannotBeReused() throws Exception {
    Instant start = Instant.now();
    ReflectionTestUtils.setField(flow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    flow.beginLogin(request, "google", false, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "expired-state");
    request.setParameter("state", "expired-state");
    ReflectionTestUtils.setField(
        flow, "externalLoginClock", Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
    OAuth2AuthenticationToken expiredConfirmation = oidc("subject", start);
    assertThrows(
        AccessDeniedException.class, () -> flow.complete(request, response, expiredConfirmation));
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
    ReflectionTestUtils.setField(flow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    flow.beginLogin(request, "google", false, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "registration-state");
    request.setParameter("state", "registration-state");
    flow.complete(request, response, oidc("subject", start));
    flow.registration(request);
    ReflectionTestUtils.setField(
        flow, "externalLoginClock", Clock.fixed(start.plusSeconds(601), ZoneOffset.UTC));
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
  }

  @Test
  void expiredFreshProofCannotAuthorizeSecurityActions() throws Exception {
    when(policies.weakGoogleAllowed()).thenReturn(true);
    Instant start = Instant.now();
    ReflectionTestUtils.setField(flow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    signedIn();
    flow.beginProof(request, "google", Purpose.EMAIL, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "proof-state");
    request.setParameter("state", "proof-state");
    flow.complete(request, response, oidc("subject", start));
    flow.authorize(request, owner, Purpose.EMAIL, false);
    ReflectionTestUtils.setField(
        flow, "externalLoginClock", Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.EMAIL, true));
  }

  @Test
  void googleRoundTripAloneDoesNotGrantSensitiveProofByDefault() {
    signedIn();
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "state");
    OAuth2AuthenticationToken confirmation = oidc("subject", Instant.now());
    ProofRejected rejected =
        assertThrows(ProofRejected.class, () -> flow.complete(request, response, confirmation));
    assertEquals(ProofProblem.FRESH_AUTHENTICATION_REQUIRED, rejected.problem());
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, true));
  }

  @Test
  void neitherProviderCanOverrideAnEnrolledRequiredSecondFactor() {
    signedIn();
    when(policies.weakGoogleAllowed()).thenReturn(true);
    when(twoFactor.requires(any(), any())).thenReturn(true);
    for (String provider : List.of("google", "microsoft")) {
      flow.beginProof(request, provider, Purpose.EMAIL, Locale.GERMAN);
      flow.bindAuthorization(request, provider, provider + "-state");
      request.setParameter("state", provider + "-state");
      OAuth2AuthenticationToken confirmation = oidc(provider, "subject", Instant.now());
      assertThrows(ProofRejected.class, () -> flow.complete(request, response, confirmation));
      assertThrows(
          AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.EMAIL, true));
    }
  }

  @Test
  void turningOffDevelopmentExceptionRevokesAnExistingWeakGrant() throws Exception {
    signedIn();
    when(policies.weakGoogleAllowed()).thenReturn(true);
    flow.beginProof(request, "google", Purpose.EMAIL, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "state");
    flow.complete(request, response, oidc("subject", null));
    when(policies.weakGoogleAllowed()).thenReturn(false);
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.EMAIL, true));
  }
}
