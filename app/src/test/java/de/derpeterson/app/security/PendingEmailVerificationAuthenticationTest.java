package de.derpeterson.app.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailVerificationStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.LoginAttemptService;
import de.derpeterson.app.service.VerificationService;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Echte Passwortprüfung: Ein Verifizierungsnachweis darf erst nach gültigem Passwort entstehen. */
class PendingEmailVerificationAuthenticationTest {
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
  private final String accountEmail = "pending@example.test";
  private CustomUserDetailsService userDetailsService;
  private AuthenticationManager authenticationManager;

  @BeforeEach
  void configurePasswordAuthentication() {
    userDetailsService = mock(CustomUserDetailsService.class);
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.REMEMBER_ME_SECRET_KEY))
        .thenReturn("isolated-test-key");
    LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
    when(loginAttemptService.authenticate(any(), any()))
        .thenAnswer(invocation -> {
          Supplier<Authentication> passwordCheck = invocation.getArgument(1);
          return passwordCheck.get();
        });
    SecurityConfig securityConfig =
        new SecurityConfig(mock(VaadinSecurityFilter.class), userDetailsService, configService);
    authenticationManager =
        securityConfig.authenticationManager(loginAttemptService, passwordEncoder);
  }

  @AfterEach
  void clearAuthenticationContext() {
    SecurityContextHolder.clearContext();
  }

  private PortalUserDetails passwordUser(boolean enabled, boolean pending) {
    PortalUserDetails portalUserDetails =
        new PortalUserDetails(
            accountEmail,
            passwordEncoder.encode("KnownPassword!"),
            "account-access-version",
            enabled,
            List.of());
    if (pending) {
      portalUserDetails.requireEmailVerification(19L);
    }
    when(userDetailsService.loadPasswordUserByUsername(accountEmail)).thenReturn(portalUserDetails);
    return portalUserDetails;
  }

  @Test
  void incorrectPasswordDoesNotDisclosePendingVerification() {
    passwordUser(true, true);

    assertThrows(
        BadCredentialsException.class,
        () ->
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    accountEmail, "WrongPassword!")));
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void correctPasswordCreatesOnlyTheRestrictedProof() {
    PortalUserDetails portalUserDetails = passwordUser(true, true);

    EmailVerificationRequiredException pendingException =
        assertThrows(
            EmailVerificationRequiredException.class,
            () ->
                authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                        accountEmail, "KnownPassword!")));

    assertEquals(19L, pendingException.getUserId());
    assertEquals(portalUserDetails.credentialVersion(), pendingException.getCredentialVersion());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void disabledAccountDoesNotReceiveVerificationProof() {
    passwordUser(false, false);

    assertThrows(
        DisabledException.class,
        () ->
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    accountEmail, "KnownPassword!")));
  }

  @Test
  void verifiedOrExplicitlyExemptAccountCanAuthenticateNormally() {
    passwordUser(true, false);

    Authentication authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(accountEmail, "KnownPassword!"));

    assertTrue(authentication.isAuthenticated());
    assertNull(authentication.getCredentials());
  }

  @Test
  void restrictedProofCannotBeReusedAfterAddressChangeOrBlocking() {
    PortalUserDetails portalUserDetails = passwordUser(true, true);
    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    PendingEmailVerificationFlow.start(
        httpRequest, new EmailVerificationRequiredException(portalUserDetails));
    UserEntity pendingUser =
        UserEntity.builder()
            .id(19L)
            .email(accountEmail)
            .password(portalUserDetails.getPassword())
            .accessVersion("account-access-version")
            .enabled(false)
            .emailVerificationStatus(EmailVerificationStatus.PENDING)
            .build();
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.lockVerificationUser(19L)).thenReturn(Optional.of(pendingUser));
    PendingEmailVerificationFlow pendingFlow =
        new PendingEmailVerificationFlow(userRepository, mock(VerificationService.class));
    assertTrue(pendingFlow.currentAccount(httpRequest).isPresent());

    pendingUser.setEmail("other@example.test");
    assertTrue(pendingFlow.currentAccount(httpRequest).isEmpty());

    pendingUser.setEmail(accountEmail);
    PendingEmailVerificationFlow.start(
        httpRequest, new EmailVerificationRequiredException(portalUserDetails));
    pendingUser.setEnabled(false);
    assertTrue(pendingFlow.currentAccount(httpRequest).isEmpty());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
