package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.ExternalLoginFlow.Purpose;
import de.derpeterson.app.service.ExternalIdentityService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.util.ReflectionTestUtils;

class ExternalLoginFlowTest {
  private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
  private final UserRepository users = mock(UserRepository.class);
  private final SecurityService security = mock(SecurityService.class);
  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private ExternalLoginFlow flow;
  private ExternalLoginConfiguration.Registrations clients;
  private UserEntity owner;

  @BeforeEach
  void setup() {
    ExternalLoginProperties properties = new ExternalLoginProperties();
    properties.getGoogle().setClientId("own-client");
    properties.getGoogle().setClientSecret("own-fixture-secret");
    clients = spy(new ExternalLoginConfiguration().externalRegistrations(properties));
    flow =
        new ExternalLoginFlow(
            clients,
            identities,
            users,
            mock(CustomUserDetailsService.class),
            security,
            mock(SessionManagementService.class),
            mock(AuthenticationManager.class));
    owner = UserEntity.builder().id(1L).email("owner@example.test").enabled(true).build();
    when(users.findById(1L)).thenReturn(Optional.of(owner));
    when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private OAuth2AuthenticationToken oidc(String subject, Instant authenticated) {
    Instant now = Instant.now();
    OidcIdToken token =
        OidcIdToken.withTokenValue("own-already-validated-fixture")
            .issuer("https://accounts.google.com")
            .subject(subject)
            .audience(List.of("own-client"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .claim("email", "person@gmail.com")
            .claim("email_verified", true)
            .claim("auth_time", authenticated)
            .build();
    DefaultOidcUser user = new DefaultOidcUser(List.of(), token);
    return new OAuth2AuthenticationToken(user, List.of(), "google");
  }

  private void signedIn() {
    when(security.getAuthenticatedUser(request))
        .thenReturn(
            Optional.of(
                User.withUsername(owner.getEmail()).password("hash").roles("USER").build()));
    when(identities.connected(1L)).thenReturn(List.of("google"));
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
    assertThrows(
        AccessDeniedException.class,
        () -> flow.complete(request, response, oidc("subject", Instant.now())));
  }

  @Test
  void mismatchedStateAndDirectAuthorizationNeverProduceRegistration() {
    assertThrows(
        AccessDeniedException.class, () -> flow.bindAuthorization(request, "google", "state"));
    flow.beginLogin(request, "google", false, Locale.ENGLISH);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "different");
    assertThrows(
        AccessDeniedException.class,
        () -> flow.complete(request, response, oidc("subject", Instant.now())));
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
  }

  @Test
  void sensitiveProofIsBoundToPurposeVersionSessionAndConsumedOnce() throws Exception {
    signedIn();
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "state");
    flow.complete(request, response, oidc("subject", Instant.now()));
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
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "next-state");
    request.setParameter("state", "next-state");
    flow.complete(request, new MockHttpServletResponse(), oidc("subject", Instant.now()));
    request.changeSessionId();
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
  }

  @Test
  void oldProviderSessionAndDifferentLinkedIdentityCannotAuthorizeSensitiveChange() {
    signedIn();
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "state");
    request.setParameter("state", "state");
    assertThrows(
        AccessDeniedException.class,
        () -> flow.complete(request, response, oidc("subject", Instant.now().minusSeconds(600))));
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.PASSWORD, false));
    flow.beginProof(request, "google", Purpose.PASSWORD, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "next");
    request.setParameter("state", "next");
    when(identities.belongsTo(eq(1L), any())).thenReturn(false);
    assertThrows(
        AccessDeniedException.class,
        () -> flow.complete(request, response, oidc("different-subject", Instant.now())));
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
    ReflectionTestUtils.setField(flow, "clock", Clock.fixed(start, ZoneOffset.UTC));
    flow.beginLogin(request, "google", false, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "expired-state");
    request.setParameter("state", "expired-state");
    ReflectionTestUtils.setField(
        flow, "clock", Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
    assertThrows(
        AccessDeniedException.class,
        () -> flow.complete(request, response, oidc("subject", start)));
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
    ReflectionTestUtils.setField(flow, "clock", Clock.fixed(start, ZoneOffset.UTC));
    flow.beginLogin(request, "google", false, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "registration-state");
    request.setParameter("state", "registration-state");
    flow.complete(request, response, oidc("subject", start));
    flow.registration(request);
    ReflectionTestUtils.setField(
        flow, "clock", Clock.fixed(start.plusSeconds(601), ZoneOffset.UTC));
    assertThrows(AccessDeniedException.class, () -> flow.registration(request));
  }

  @Test
  void expiredFreshProofCannotAuthorizeSecurityActions() throws Exception {
    Instant start = Instant.now();
    ReflectionTestUtils.setField(flow, "clock", Clock.fixed(start, ZoneOffset.UTC));
    signedIn();
    flow.beginProof(request, "google", Purpose.EMAIL, Locale.GERMAN);
    flow.bindAuthorization(request, "google", "proof-state");
    request.setParameter("state", "proof-state");
    flow.complete(request, response, oidc("subject", start));
    flow.authorize(request, owner, Purpose.EMAIL, false);
    ReflectionTestUtils.setField(
        flow, "clock", Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
    assertThrows(
        AccessDeniedException.class, () -> flow.authorize(request, owner, Purpose.EMAIL, true));
  }
}
