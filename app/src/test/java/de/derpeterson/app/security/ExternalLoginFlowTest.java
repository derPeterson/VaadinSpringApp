package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.AccountDeactivation;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.EmailVerificationStatus;
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
  private final ExternalIdentityService externalIdentityService =
      mock(ExternalIdentityService.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final SecurityService securityService = mock(SecurityService.class);
  private final TwoFactorAuthorization twoFactor = mock(TwoFactorAuthorization.class);
  private final TwoFactorEnrollment twoFactorEnrollment = mock(TwoFactorEnrollment.class);
  private final TwoFactorPolicies twoFactorPolicies = mock(TwoFactorPolicies.class);
  private final EntityManager entityManager = mock(EntityManager.class);
  private final PlatformTransactionManager transactionManager =
      mock(PlatformTransactionManager.class);
  private final AuthenticationManager passwords = mock(AuthenticationManager.class);
  private final MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
  private final MockHttpServletResponse mockHttpResponse = new MockHttpServletResponse();
  private ExternalLoginFlow externalLoginFlow;
  private ExternalLoginConfiguration.Registrations clients;
  private UserEntity ownerUserEntity;

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
    externalLoginFlow =
        new ExternalLoginFlow(
            clients,
            externalIdentityService,
            userRepository,
            mock(CustomUserDetailsService.class),
            securityService,
            mock(SessionManagementService.class),
            passwords,
            twoFactor,
            twoFactorEnrollment,
            twoFactorPolicies,
            entityManager,
            transactionManager);
    ownerUserEntity = UserEntity.builder().id(1L).email("owner@example.test").enabled(true).build();
    when(twoFactor.sessionConfirmed(any())).thenReturn(true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(ownerUserEntity));
    when(userRepository.findByEmail(ownerUserEntity.getEmail()))
        .thenReturn(Optional.of(ownerUserEntity));
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ownerUserEntity));
    when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    when(twoFactorPolicies.current())
        .thenReturn(new TwoFactorPolicies.Snapshot(-1, TwoFactorPolicy.defaults()));
    when(twoFactor.owner(mockHttpRequest))
        .thenReturn(
            new TwoFactorAuthorization.Owner(
                1L,
                ownerUserEntity.getEmail(),
                ownerUserEntity.getAccessVersion(),
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
  void pendingEmailStopsBeforeSessionAdmissionAndSecondFactor() {
    ownerUserEntity.setEmailVerificationStatus(EmailVerificationStatus.PENDING);

    assertEquals(
        "/verify-email",
        externalLoginFlow.finishLogin(
            mockHttpRequest, mockHttpResponse, ownerUserEntity, "google", true));
    verify(securityService, never()).storeVerifiedAuthentication(any(), any(), any(), anyBoolean());
    verify(twoFactor, never()).sessionConfirmed(any());
  }

  @Test
  void selfDeactivationStopsProviderLoginBeforeSessionAdmissionAndSecondFactor() {
    ownerUserEntity.setEnabled(false);
    ownerUserEntity.setAccountDeactivation(
        new AccountDeactivation(
            Instant.now().plusSeconds(3600), null, "test-digest", null, null, null));
    assertEquals(
        "/account-deactivated",
        externalLoginFlow.finishLogin(
            mockHttpRequest, mockHttpResponse, ownerUserEntity, "google", true));
    verify(securityService, never()).storeVerifiedAuthentication(any(), any(), any(), anyBoolean());
    verify(twoFactor, never()).sessionConfirmed(any());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void missingIssuerIsRejectedWithoutGrantingAccess() {
    OidcUser identity = mock(OidcUser.class);

    assertThrows(AccessDeniedException.class, () -> ExternalLoginFlow.evidence("google", identity));
  }

  private void pendingLink() {
    signedIn();
    externalLoginFlow.confirmPassword(mockHttpRequest, "own-password", Purpose.LINK_MICROSOFT);

    assertEquals(
        "/oauth2/authorization/microsoft",
        externalLoginFlow.beginLink(mockHttpRequest, "microsoft", Locale.GERMAN));
    externalLoginFlow.bindAuthorization(mockHttpRequest, "microsoft", "link-state");
    mockHttpRequest.setParameter("state", "link-state");
  }

  @Test
  void linkConsumesItsProofOnlyInsideTheLockedCallbackTransaction() throws Exception {
    pendingLink();

    assertEquals(
        Optional.of(Purpose.LINK_MICROSOFT), externalLoginFlow.confirmedPurpose(mockHttpRequest));
    verify(twoFactor, never())
        .authorizeCurrent(mockHttpRequest, TwoFactorAction.LINK_PROVIDER, true);
    externalLoginFlow.complete(
        mockHttpRequest, mockHttpResponse, oidc("microsoft", "new-subject", Instant.now()));
    InOrder ordered =
        inOrder(
            userRepository, entityManager, twoFactor, externalIdentityService, transactionManager);
    ordered.verify(userRepository).findByIdForUpdate(1L);
    ordered.verify(entityManager).refresh(ownerUserEntity);
    ordered
        .verify(twoFactor)
        .authorizeCurrent(mockHttpRequest, TwoFactorAction.LINK_PROVIDER, true);
    ordered
        .verify(externalIdentityService)
        .link(eq(1L), eq(ownerUserEntity.getAccessVersion()), any());
    ordered.verify(transactionManager).commit(any());

    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(
                mockHttpRequest, ownerUserEntity, Purpose.LINK_MICROSOFT, true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"policy", "factor", "required"})
  void changeDuringProviderRedirectRejectsTheFinalLink(String change) {
    pendingLink();
    switch (change) {
      case "policy" ->
          when(twoFactorPolicies.current())
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
        AccessDeniedException.class,
        () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, microsoftLogin));
    verify(externalIdentityService, never()).link(any(), any(), any());
    verify(transactionManager).rollback(any());
  }

  @Test
  void redirectCannotExtendTheOriginalProofDeadline() {
    Instant start = Instant.now();
    signedIn();
    ReflectionTestUtils.setField(
        externalLoginFlow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    externalLoginFlow.confirmPassword(mockHttpRequest, "own-password", Purpose.LINK_MICROSOFT);
    ReflectionTestUtils.setField(
        externalLoginFlow,
        "externalLoginClock",
        Clock.fixed(start.plusSeconds(299), ZoneOffset.UTC));
    externalLoginFlow.beginLink(mockHttpRequest, "microsoft", Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "microsoft", "link-state");
    mockHttpRequest.setParameter("state", "link-state");
    ReflectionTestUtils.setField(
        externalLoginFlow,
        "externalLoginClock",
        Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
    OAuth2AuthenticationToken microsoftLogin = oidc("microsoft", "new-subject", start);

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, microsoftLogin));
    verify(externalIdentityService, never()).link(any(), any(), any());
  }

  @Test
  void factorProofStillRequiresItsStoredGrantWhenTheActionRuleIsOff() {
    signedIn();
    externalLoginFlow.confirmFactor(mockHttpRequest, "own-code", Purpose.LINK_MICROSOFT);
    externalLoginFlow.beginLink(mockHttpRequest, "microsoft", Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "microsoft", "link-state");
    mockHttpRequest.setParameter("state", "link-state");
    doThrow(new AccessDeniedException("fixture changed grant"))
        .when(twoFactor)
        .authorizeFactorGrant(mockHttpRequest, TwoFactorAction.LINK_PROVIDER, true);
    OAuth2AuthenticationToken microsoftLogin = oidc("microsoft", "new-subject", Instant.now());

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, microsoftLogin));
    verify(externalIdentityService, never()).link(any(), any(), any());
  }

  @Test
  void pendingAttemptRegistrationAndGrantSurviveSessionSerialization() throws Exception {
    when(twoFactorPolicies.weakGoogleAllowed()).thenReturn(true);
    externalLoginFlow.beginLogin(mockHttpRequest, "google", true, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "state");
    roundTripSessionValues();
    mockHttpRequest.setParameter("state", "state");
    externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, oidc("subject", Instant.now()));
    roundTripSessionValues();

    assertTrue(externalLoginFlow.registration(mockHttpRequest).remember());
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, "google", Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "proof-state");
    mockHttpRequest.setParameter("state", "proof-state");
    externalLoginFlow.complete(
        mockHttpRequest, new MockHttpServletResponse(), oidc("subject", Instant.now()));
    roundTripSessionValues();
    externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, true);

    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, true));
  }

  private void roundTripSessionValues() throws Exception {
    java.util.Enumeration<String> names = mockHttpRequest.getSession().getAttributeNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
        output.writeObject(mockHttpRequest.getSession().getAttribute(name));
      }
      try (ObjectInputStream input =
          new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
        mockHttpRequest.getSession().setAttribute(name, input.readObject());
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
    mockHttpRequest.getSession();
    when(securityService.getAuthenticatedUser(mockHttpRequest))
        .thenReturn(
            Optional.of(
                User.withUsername(ownerUserEntity.getEmail())
                    .password("hash")
                    .roles("USER")
                    .build()));
    when(externalIdentityService.connected(1L)).thenReturn(List.of("google", "microsoft"));
    when(externalIdentityService.belongsTo(eq(1L), any())).thenReturn(true);
  }

  @Test
  void disablingAProviderRejectsBothItsCallbackAndAlreadyPendingRegistration() throws Exception {
    externalLoginFlow.beginLogin(mockHttpRequest, "google", true, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "own-state");
    mockHttpRequest.setParameter("state", "own-state");
    externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, oidc("subject", Instant.now()));
    when(clients.providers()).thenReturn(List.of());

    assertThrows(
        AccessDeniedException.class, () -> externalLoginFlow.registration(mockHttpRequest));
    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.finishLogin(
                mockHttpRequest, mockHttpResponse, ownerUserEntity, "google", false));
    verify(securityService, never())
        .storeVerifiedAuthentication(any(), any(), any(), any(Boolean.class));
  }

  @Test
  void unknownIdentityProducesOnlyExpiringRegistrationAndPreservesRememberChoice()
      throws Exception {
    externalLoginFlow.beginLogin(mockHttpRequest, "google", true, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "own-state");
    mockHttpRequest.setParameter("state", "own-state");
    externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, oidc("subject", Instant.now()));

    assertEquals("/external-registration", mockHttpResponse.getRedirectedUrl());
    assertTrue(externalLoginFlow.registration(mockHttpRequest).remember());
    assertEquals(Locale.GERMAN, externalLoginFlow.registration(mockHttpRequest).locale());
    verify(securityService, never())
        .storeVerifiedAuthentication(
            any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
    OAuth2AuthenticationToken replayedConfirmation = oidc("subject", Instant.now());

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, replayedConfirmation));
  }

  @Test
  void mismatchedStateAndDirectAuthorizationNeverProduceRegistration() {
    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "state"));
    externalLoginFlow.beginLogin(mockHttpRequest, "google", false, Locale.ENGLISH);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "state");
    mockHttpRequest.setParameter("state", "different");
    OAuth2AuthenticationToken mismatchedStateConfirmation = oidc("subject", Instant.now());

    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.complete(
                mockHttpRequest, mockHttpResponse, mismatchedStateConfirmation));
    assertThrows(
        AccessDeniedException.class, () -> externalLoginFlow.registration(mockHttpRequest));
  }

  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void sensitiveProofIsBoundToPurposeVersionSessionAndConsumedOnce(String provider)
      throws Exception {
    if (provider.equals("google")) {
      when(twoFactorPolicies.weakGoogleAllowed()).thenReturn(true);
    }
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, provider, Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, provider, "state");
    mockHttpRequest.setParameter("state", "state");
    externalLoginFlow.complete(
        mockHttpRequest, mockHttpResponse, oidc(provider, "subject", Instant.now()));
    externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, false);

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.EMAIL, false));
    String previousVersion = ownerUserEntity.getAccessVersion();
    ownerUserEntity.rotateAccessVersion();

    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, false));
    ownerUserEntity.setAccessVersion(previousVersion);
    externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, true);

    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, true));
    externalLoginFlow.beginProof(mockHttpRequest, provider, Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, provider, "next-state");
    mockHttpRequest.setParameter("state", "next-state");
    externalLoginFlow.complete(
        mockHttpRequest, new MockHttpServletResponse(), oidc(provider, "subject", Instant.now()));
    mockHttpRequest.changeSessionId();

    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, false));
  }

  @Test
  void oldMicrosoftSessionCannotAuthorizeSensitiveChange() {
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, "microsoft", Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "microsoft", "state");
    mockHttpRequest.setParameter("state", "state");
    OAuth2AuthenticationToken staleConfirmation =
        oidc("microsoft", "subject", Instant.now().minusSeconds(600));
    ProofRejected stale =
        assertThrows(
            ProofRejected.class,
            () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, staleConfirmation));

    assertEquals(ProofProblem.FRESH_AUTHENTICATION_REQUIRED, stale.problem());
    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"google", "microsoft"})
  void differentLinkedIdentityCannotAuthorizeSensitiveChange(String provider) {
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, provider, Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, provider, "state");
    mockHttpRequest.setParameter("state", "state");
    when(externalIdentityService.belongsTo(eq(1L), any())).thenReturn(false);
    OAuth2AuthenticationToken differentIdentityConfirmation =
        oidc(provider, "different-subject", Instant.now());
    ProofRejected mismatch =
        assertThrows(
            ProofRejected.class,
            () ->
                externalLoginFlow.complete(
                    mockHttpRequest, mockHttpResponse, differentIdentityConfirmation));

    assertEquals(ProofProblem.IDENTITY_MISMATCH, mismatch.problem());
    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void missingOrFutureMicrosoftAuthenticationTimeNeverGrantsAProof(boolean missing) {
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, "microsoft", Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "microsoft", "state");
    mockHttpRequest.setParameter("state", "state");
    OAuth2AuthenticationToken invalidTimeConfirmation =
        oidc("microsoft", "subject", missing ? null : Instant.now().plusSeconds(60));
    ProofRejected failure =
        assertThrows(
            ProofRejected.class,
            () ->
                externalLoginFlow.complete(
                    mockHttpRequest, mockHttpResponse, invalidTimeConfirmation));

    assertEquals(ProofProblem.FRESH_AUTHENTICATION_REQUIRED, failure.problem());
    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, false));
    assertThrows(
        AccessDeniedException.class, () -> externalLoginFlow.registration(mockHttpRequest));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void explicitlyEnabledDevelopmentConfirmationAllowsMissingOrOldGoogleTime(boolean missing)
      throws Exception {
    when(twoFactorPolicies.weakGoogleAllowed()).thenReturn(true);
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, "google", Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "state");
    mockHttpRequest.setParameter("state", "state");
    externalLoginFlow.complete(
        mockHttpRequest,
        mockHttpResponse,
        oidc("subject", missing ? null : Instant.now().minusSeconds(86400)));

    assertEquals("/account?section=security&tab=portal", mockHttpResponse.getRedirectedUrl());
    externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, true);

    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, true));
    OAuth2AuthenticationToken replayedConfirmation = oidc("subject", null);

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, replayedConfirmation));
  }

  @ParameterizedTest
  @ValueSource(strings = {"blocked", "version", "signed-out", "different-user"})
  void googleInterimConfirmationStillRequiresTheUnchangedLocalAccount(String change) {
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, "google", Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "state");
    mockHttpRequest.setParameter("state", "state");
    switch (change) {
      case "blocked" -> ownerUserEntity.setEnabled(false);
      case "version" -> ownerUserEntity.rotateAccessVersion();
      case "signed-out" ->
          when(securityService.getAuthenticatedUser(mockHttpRequest)).thenReturn(Optional.empty());
      case "different-user" ->
          when(securityService.getAuthenticatedUser(mockHttpRequest))
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
        () ->
            externalLoginFlow.complete(
                mockHttpRequest, mockHttpResponse, changedAccountConfirmation));
    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, false));
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
    ReflectionTestUtils.setField(
        externalLoginFlow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    externalLoginFlow.beginLogin(mockHttpRequest, "google", false, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "expired-state");
    mockHttpRequest.setParameter("state", "expired-state");
    ReflectionTestUtils.setField(
        externalLoginFlow,
        "externalLoginClock",
        Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
    OAuth2AuthenticationToken expiredConfirmation = oidc("subject", start);

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, expiredConfirmation));
    assertThrows(
        AccessDeniedException.class, () -> externalLoginFlow.registration(mockHttpRequest));
    ReflectionTestUtils.setField(
        externalLoginFlow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    externalLoginFlow.beginLogin(mockHttpRequest, "google", false, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "registration-state");
    mockHttpRequest.setParameter("state", "registration-state");
    externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, oidc("subject", start));
    externalLoginFlow.registration(mockHttpRequest);
    ReflectionTestUtils.setField(
        externalLoginFlow,
        "externalLoginClock",
        Clock.fixed(start.plusSeconds(601), ZoneOffset.UTC));

    assertThrows(
        AccessDeniedException.class, () -> externalLoginFlow.registration(mockHttpRequest));
  }

  @Test
  void expiredFreshProofCannotAuthorizeSecurityActions() throws Exception {
    when(twoFactorPolicies.weakGoogleAllowed()).thenReturn(true);
    Instant start = Instant.now();
    ReflectionTestUtils.setField(
        externalLoginFlow, "externalLoginClock", Clock.fixed(start, ZoneOffset.UTC));
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, "google", Purpose.EMAIL, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "proof-state");
    mockHttpRequest.setParameter("state", "proof-state");
    externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, oidc("subject", start));
    externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.EMAIL, false);
    ReflectionTestUtils.setField(
        externalLoginFlow,
        "externalLoginClock",
        Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.EMAIL, true));
  }

  @Test
  void googleRoundTripAloneDoesNotGrantSensitiveProofByDefault() {
    signedIn();
    externalLoginFlow.beginProof(mockHttpRequest, "google", Purpose.PASSWORD, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "state");
    mockHttpRequest.setParameter("state", "state");
    OAuth2AuthenticationToken confirmation = oidc("subject", Instant.now());
    ProofRejected rejected =
        assertThrows(
            ProofRejected.class,
            () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, confirmation));

    assertEquals(ProofProblem.FRESH_AUTHENTICATION_REQUIRED, rejected.problem());
    assertThrows(
        AccessDeniedException.class,
        () ->
            externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.PASSWORD, true));
  }

  @Test
  void neitherProviderCanOverrideAnEnrolledRequiredSecondFactor() {
    signedIn();
    when(twoFactorPolicies.weakGoogleAllowed()).thenReturn(true);
    when(twoFactor.requires(any(), any())).thenReturn(true);
    for (String provider : List.of("google", "microsoft")) {
      externalLoginFlow.beginProof(mockHttpRequest, provider, Purpose.EMAIL, Locale.GERMAN);
      externalLoginFlow.bindAuthorization(mockHttpRequest, provider, provider + "-state");
      mockHttpRequest.setParameter("state", provider + "-state");
      OAuth2AuthenticationToken confirmation = oidc(provider, "subject", Instant.now());

      assertThrows(
          ProofRejected.class,
          () -> externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, confirmation));
      assertThrows(
          AccessDeniedException.class,
          () -> externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.EMAIL, true));
    }
  }

  @Test
  void turningOffDevelopmentExceptionRevokesAnExistingWeakGrant() throws Exception {
    signedIn();
    when(twoFactorPolicies.weakGoogleAllowed()).thenReturn(true);
    externalLoginFlow.beginProof(mockHttpRequest, "google", Purpose.EMAIL, Locale.GERMAN);
    externalLoginFlow.bindAuthorization(mockHttpRequest, "google", "state");
    mockHttpRequest.setParameter("state", "state");
    externalLoginFlow.complete(mockHttpRequest, mockHttpResponse, oidc("subject", null));
    when(twoFactorPolicies.weakGoogleAllowed()).thenReturn(false);

    assertThrows(
        AccessDeniedException.class,
        () -> externalLoginFlow.authorize(mockHttpRequest, ownerUserEntity, Purpose.EMAIL, true));
  }
}
