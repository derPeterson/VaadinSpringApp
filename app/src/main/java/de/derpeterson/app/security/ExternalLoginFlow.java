package de.derpeterson.app.security;

import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE;

import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorEnrollment;
import de.derpeterson.app.security.mfa.TwoFactorPolicies;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Kurzlebige, sitzungsgebundene Abläufe. Nur fertige lokale Logins erhalten Portalzugriff. */
@Service
@RequiredArgsConstructor
public class ExternalLoginFlow {
  public enum Purpose {
    PASSWORD,
    EMAIL,
    LINK_GOOGLE,
    LINK_MICROSOFT,
    UNLINK_GOOGLE,
    UNLINK_MICROSOFT,
    ENROLL_FACTOR
  }

  public enum Mode {
    LOGIN,
    LINK,
    PROOF
  }

  public enum ProofProblem {
    FRESH_AUTHENTICATION_REQUIRED,
    IDENTITY_MISMATCH,
    UNAVAILABLE
  }

  public static final class ProofRejected extends AccessDeniedException {
    private final ProofProblem problem;

    public ProofRejected(ProofProblem problem) {
      super(problem.name());
      this.problem = problem;
    }

    public ProofProblem problem() {
      return problem;
    }
  }

  public static final String ERROR_PARAMETER = "external_error";
  public static final String DISCONNECTED_PARAMETER = "provider_disconnected";

  public record Attempt(
      String provider,
      Mode mode,
      Long userId,
      String accessVersion,
      Purpose purpose,
      boolean remember,
      Locale locale,
      Instant started,
      String state)
      implements Serializable {
    @Override
    public String toString() {
      return "External sign-in attempt";
    }
  }

  public record Registration(
      ExternalIdentityEvidence identity, Locale locale, boolean remember, Instant expires)
      implements Serializable {
    @Override
    public String toString() {
      return "Pending external registration";
    }
  }

  private enum ProofMethod {
    PASSWORD,
    MICROSOFT,
    GOOGLE_DEVELOPMENT,
    SECOND_FACTOR
  }

  private record Grant(
      Long userId,
      String version,
      Purpose purpose,
      Instant expires,
      String sessionId,
      ProofMethod method,
      UUID factorRevision,
      long policyRevision)
      implements Serializable {
    @Override
    public String toString() {
      return "External action grant";
    }
  }

  /** Kann ausschließlich nach der OIDC-Prüfung dieses Dienstes erzeugt werden. */
  public static final class EnrollmentConfirmation {
    private final Long userId;
    private final String version;
    private final String sessionId;
    private final String provider;
    private final Instant expires;

    private EnrollmentConfirmation(
        UserEntity user, HttpSession session, String provider, Instant expires) {
      this.userId = user.getId();
      this.version = user.getAccessVersion();
      this.sessionId = session.getId();
      this.provider = provider;
      this.expires = expires;
    }

    public boolean matches(Long ownerId, String accessVersion, HttpSession session, Instant now) {
      return session != null
          && userId.equals(ownerId)
          && version.equals(accessVersion)
          && sessionId.equals(session.getId())
          && expires.isAfter(now);
    }

    public String provider() {
      return provider;
    }

    @Override
    public String toString() {
      return "Verified enrollment provider";
    }
  }

  private static final String ATTEMPT = ExternalLoginFlow.class.getName() + ".attempt";
  private static final String REGISTRATION = ExternalLoginFlow.class.getName() + ".registration";
  private static final String GRANT = ExternalLoginFlow.class.getName() + ".grant";
  private final ExternalLoginConfiguration.Registrations providerRegistrations;
  private final ExternalIdentityService externalIdentityService;
  private final UserRepository userRepository;
  private final CustomUserDetailsService customUserDetailsService;
  private final SecurityService securityService;
  private final SessionManagementService sessionManagementService;
  private final AuthenticationManager authenticationManager;
  private final TwoFactorAuthorization twoFactor;
  private final TwoFactorEnrollment twoFactorEnrollment;
  private final TwoFactorPolicies twoFactorPolicies;
  private final EntityManager entityManager;
  private final PlatformTransactionManager transactionManager;
  private Clock externalLoginClock = Clock.systemUTC();

  public void confirmPassword(HttpServletRequest request, String password, Purpose purpose) {
    Objects.requireNonNull(purpose, "Security action required");
    if (purpose == Purpose.ENROLL_FACTOR || requiresFactor(request, purpose)) {
      throw new AccessDeniedException("Confirm with the required second factor");
    }
    UserEntity user = owner(request);
    Authentication verified =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(user.getEmail(), password));
    if (!verified.isAuthenticated()) {
      throw new AccessDeniedException("Confirmation failed");
    }
    UserEntity current = owner(request);
    if (!Objects.equals(user.getAccessVersion(), current.getAccessVersion())) {
      throw new AccessDeniedException("Account changed");
    }
    storeGrant(request, current, purpose, ProofMethod.PASSWORD);
  }

  public boolean requiresFactor(HttpServletRequest request, Purpose purpose) {
    return twoFactor.requires(request, action(purpose));
  }

  public boolean factorEnrolled(HttpServletRequest request) {
    return twoFactor.state(twoFactor.owner(request)).enrolled();
  }

  public boolean loginConfirmed(HttpServletRequest request) {
    return twoFactor.sessionConfirmed(request);
  }

  public boolean developmentExceptionActive() {
    return twoFactorPolicies.pauseEnabled(twoFactorPolicies.current())
        || twoFactorPolicies.weakGoogleAllowed();
  }

  public boolean canUseProviderProof(HttpServletRequest request, String provider, Purpose purpose) {
    return purpose == Purpose.ENROLL_FACTOR
        || !requiresFactor(request, purpose)
            && (!GOOGLE.equals(provider) || twoFactorPolicies.weakGoogleAllowed());
  }

  public void confirmFactor(HttpServletRequest request, String code, Purpose purpose) {
    if (purpose == Purpose.ENROLL_FACTOR) {
      throw new AccessDeniedException("Use the protected enrollment flow");
    }
    twoFactor.confirmAction(request, action(purpose), code);
    UserEntity user = owner(request);
    storeGrant(request, user, purpose, ProofMethod.SECOND_FACTOR);
  }

  public List<String> providers() {
    return providerRegistrations.providers();
  }

  public String beginLogin(
      HttpServletRequest request, String provider, boolean remember, Locale locale) {
    if (securityService.getAuthenticatedUser(request).isPresent()) {
      throw new AccessDeniedException("Already signed in");
    }
    return begin(request, provider, Mode.LOGIN, null, null, remember, locale);
  }

  public String beginProof(
      HttpServletRequest request, String provider, Purpose purpose, Locale locale) {
    Objects.requireNonNull(purpose, "Security action required");
    UserEntity user = owner(request);
    if (!externalIdentityService.connected(user.getId()).contains(provider)) {
      throw new AccessDeniedException("Provider not connected");
    }
    return begin(request, provider, Mode.PROOF, user, purpose, false, locale);
  }

  public String beginLink(
      HttpServletRequest httpRequest, String providerId, Locale preferredLocale) {
    UserEntity accountOwner = owner(httpRequest);
    Purpose linkPurpose = linkPurpose(providerId);

    // Der ausgehende OIDC-Aufruf verbraucht die Freigabe noch nicht. Die eigentliche
    // Verbindung prüft und verbraucht sie nach dem Rücksprung unter dem Kontolock.
    authorize(httpRequest, accountOwner, linkPurpose, false);
    return begin(
        httpRequest, providerId, Mode.LINK, accountOwner, linkPurpose, false, preferredLocale);
  }

  public boolean disconnect(HttpServletRequest httpRequest, String providerId) {
    UserEntity accountOwner = owner(httpRequest);
    authorize(httpRequest, accountOwner, unlinkPurpose(providerId), true);

    String previousAccessVersion = accountOwner.getAccessVersion();
    boolean currentSessionRetained =
        externalIdentityService.unlink(
            httpRequest, accountOwner.getId(), previousAccessVersion, providerId);

    cancel(httpRequest);
    if (currentSessionRetained) {
      twoFactor.retainAfterAccessVersionChange(httpRequest, previousAccessVersion);
    } else {
      securityService.logout();
    }

    return currentSessionRetained;
  }

  private String begin(
      HttpServletRequest request,
      String provider,
      Mode mode,
      UserEntity user,
      Purpose purpose,
      boolean remember,
      Locale locale) {
    requireCompatibleFlow(request, purpose);
    if (providerRegistrations.findByRegistrationId(provider) == null) {
      throw new AccessDeniedException("Provider unavailable");
    }
    HttpSession session = request.getSession(true);
    session.removeAttribute(REGISTRATION);
    session.setAttribute(
        ATTEMPT,
        new Attempt(
            provider,
            mode,
            user == null ? null : user.getId(),
            user == null ? null : user.getAccessVersion(),
            purpose,
            remember,
            Locale.GERMAN.equals(locale) ? Locale.GERMAN : Locale.ENGLISH,
            externalLoginClock.instant(),
            null));
    return "/oauth2/authorization/" + provider;
  }

  public Attempt bindAuthorization(HttpServletRequest request, String provider, String state) {
    HttpSession session = request.getSession(false);
    if (session == null
        || !(session.getAttribute(ATTEMPT)
            instanceof
            Attempt(
                String previousProvider,
                Mode mode,
                Long userId,
                String accessVersion,
                Purpose purpose,
                boolean remember,
                Locale locale,
                Instant started,
                String previousState))
        || !previousProvider.equals(provider)
        || previousState != null
        || started.plusSeconds(300).isBefore(externalLoginClock.instant())) {
      throw new AccessDeniedException("Start sign-in from the portal");
    }
    requireCompatibleFlow(request, purpose);
    Attempt attempt =
        new Attempt(
            previousProvider,
            mode,
            userId,
            accessVersion,
            purpose,
            remember,
            locale,
            started,
            state);
    session.setAttribute(ATTEMPT, attempt);
    return attempt;
  }

  public void complete(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    HttpSession session = request.getSession(false);
    Attempt attempt = session == null ? null : (Attempt) session.getAttribute(ATTEMPT);
    if (session != null) {
      session.removeAttribute(ATTEMPT);
    }
    if (attempt == null
        || attempt.started().plusSeconds(300).isBefore(externalLoginClock.instant())
        || !Objects.equals(attempt.state(), request.getParameter("state"))
        || !(authentication instanceof OAuth2AuthenticationToken oauth)
        || !attempt.provider().equals(oauth.getAuthorizedClientRegistrationId())
        || !(oauth.getPrincipal() instanceof OidcUser oidc)) {
      throw new AccessDeniedException("External sign-in no longer valid");
    }
    requireCompatibleFlow(request, attempt.purpose());
    requireProvider(attempt.provider());
    ExternalIdentityEvidence evidence = evidence(attempt.provider(), oidc);
    // Das Framework-OIDC-Token ist kein lokaler Portalzugang und wird nicht gespeichert.
    SecurityContextHolder.clearContext();
    if (attempt.mode() != Mode.LOGIN) {
      completeAccountAction(request, session, attempt, evidence);
      TwoFactorFlow active = TwoFactorFlow.current(request);
      String setupLocation = active == null ? TwoFactorFlow.SETUP.location() : active.location();
      response.sendRedirect(
          attempt.purpose() == Purpose.ENROLL_FACTOR
              ? setupLocation
              : AppRouteConstants.ACCOUNT_PORTAL_LOCATION);
      return;
    }
    Optional<UserEntity> user = externalIdentityService.resolve(evidence);
    if (user.isPresent()) {
      login(request, response, user.get(), evidence.provider(), attempt.remember());
      return;
    }
    session.setAttribute(
        REGISTRATION,
        new Registration(
            evidence,
            attempt.locale(),
            attempt.remember(),
            externalLoginClock.instant().plusSeconds(600)));
    response.sendRedirect("/external-registration");
  }

  private void requireCompatibleFlow(HttpServletRequest request, Purpose purpose) {
    TwoFactorFlow active = TwoFactorFlow.current(request);
    if (active != null
        && (purpose != Purpose.ENROLL_FACTOR || active.help() || active.recovery())) {
      throw new AccessDeniedException("Finish the active two-factor flow first");
    }
  }

  private void completeAccountAction(
      HttpServletRequest request,
      HttpSession session,
      Attempt attempt,
      ExternalIdentityEvidence evidence) {
    UserEntity user =
        userRepository
            .findById(attempt.userId())
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    if (!user.isEnabled()
        || !Objects.equals(attempt.accessVersion(), user.getAccessVersion())
        || securityService
            .getAuthenticatedUser(request)
            .filter(principal -> principal.getUsername().equals(user.getEmail()))
            .isEmpty()) {
      throw new AccessDeniedException("Account access changed");
    }
    if (attempt.mode() == Mode.LINK) {
      completeLink(request, attempt, evidence);
      return;
    }
    validateAccountProof(attempt, evidence, user);
    if (attempt.purpose() == Purpose.ENROLL_FACTOR) {
      twoFactorEnrollment.afterProviderConfirmation(
          request,
          new EnrollmentConfirmation(
              user, session, attempt.provider(), externalLoginClock.instant().plusSeconds(60)));
      return;
    }
    if (!canUseProviderProof(request, attempt.provider(), attempt.purpose())) {
      throw new ProofRejected(ProofProblem.FRESH_AUTHENTICATION_REQUIRED);
    }
    storeGrant(
        request,
        user,
        attempt.purpose(),
        attempt.provider().equals(GOOGLE) ? ProofMethod.GOOGLE_DEVELOPMENT : ProofMethod.MICROSOFT);
  }

  private void validateAccountProof(
      Attempt attempt, ExternalIdentityEvidence evidence, UserEntity user) {
    if (!externalIdentityService.belongsTo(user.getId(), evidence)) {
      throw new ProofRejected(ProofProblem.IDENTITY_MISMATCH);
    }
    if (attempt.provider().equals(GOOGLE)) {
      return;
    }
    Instant authenticated = evidence.authenticatedAt();
    if (authenticated == null
        || authenticated.isBefore(attempt.started().minusSeconds(5))
        || authenticated.isAfter(externalLoginClock.instant().plusSeconds(30))) {
      throw new ProofRejected(ProofProblem.FRESH_AUTHENTICATION_REQUIRED);
    }
  }

  private void completeLink(
      HttpServletRequest request, Attempt attempt, ExternalIdentityEvidence evidence) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(status -> {
          UserEntity current = userRepository.findByIdForUpdate(attempt.userId()).orElseThrow();
          entityManager.refresh(current);
          if (!current.isEnabled()
              || !Objects.equals(current.getAccessVersion(), attempt.accessVersion())) {
            throw new AccessDeniedException("Account access changed");
          }
          authorize(request, current, attempt.purpose(), true);
          externalIdentityService.link(current.getId(), attempt.accessVersion(), evidence);
        });
  }

  private void storeGrant(
      HttpServletRequest request, UserEntity user, Purpose purpose, ProofMethod method) {
    HttpSession session = request.getSession(false);
    session.setAttribute(
        GRANT,
        new Grant(
            user.getId(),
            user.getAccessVersion(),
            purpose,
            externalLoginClock.instant().plusSeconds(300),
            session.getId(),
            method,
            currentFactorRevision(request),
            twoFactorPolicies.current().revision()));
  }

  private UUID currentFactorRevision(HttpServletRequest request) {
    return twoFactor.state(twoFactor.owner(request)).revision();
  }

  public void login(
      HttpServletRequest request,
      HttpServletResponse response,
      UserEntity user,
      String provider,
      boolean remember)
      throws IOException {
    response.sendRedirect(finishLogin(request, response, user, provider, remember));
  }

  public String finishLogin(
      HttpServletRequest request,
      HttpServletResponse response,
      UserEntity user,
      String provider,
      boolean remember) {
    requireProvider(provider);
    if (user.requiresEmailVerification()) {
      PendingEmailVerificationFlow.startForProviderAccount(request, user);
      return AppRouteConstants.PENDING_EMAIL_VERIFICATION_LOCATION;
    }
    ExternalAuthenticationToken authentication =
        new ExternalAuthenticationToken(
            customUserDetailsService.loadUserByUsername(user.getEmail()), provider);
    try {
      securityService.storeVerifiedAuthentication(request, response, authentication, remember);
      if (!twoFactor.sessionConfirmed(request)) {
        return AppRouteConstants.TWO_FACTOR_SECURITY_ROUTE;
      }
      return authentication.getAuthorities().stream()
              .anyMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()))
          ? "/admin"
          : "/home";
    } catch (SessionLimitException _) {
      sessionManagementService.startExternalRecovery(request, authentication, remember);
      return "/login?error=sessions";
    }
  }

  public Registration registration(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null
        && session.getAttribute(REGISTRATION) instanceof Registration registration
        && registration.expires().isAfter(externalLoginClock.instant())) {
      requireProvider(registration.identity().provider());
      return registration;
    }
    throw new AccessDeniedException("External registration expired");
  }

  private void requireProvider(String provider) {
    if (!providerRegistrations.providers().contains(provider)) {
      throw new AccessDeniedException("Sign-in provider unavailable");
    }
  }

  public void cancel(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(REGISTRATION);
      session.removeAttribute(ATTEMPT);
      session.removeAttribute(GRANT);
    }
  }

  public void authorize(
      HttpServletRequest request, UserEntity user, Purpose purpose, boolean consume) {
    if (purpose == Purpose.ENROLL_FACTOR) {
      throw new AccessDeniedException("Enrollment proof cannot authorize account changes");
    }
    HttpSession session = request.getSession(false);
    if (session == null
        || !(session.getAttribute(GRANT)
            instanceof
            Grant(
                Long userId,
                String version,
                Purpose grantedPurpose,
                Instant expires,
                String sessionId,
                ProofMethod method,
                UUID factorRevision,
                long policyRevision))
        || !userId.equals(user.getId())
        || !version.equals(user.getAccessVersion())
        || grantedPurpose != purpose
        || !expires.isAfter(externalLoginClock.instant())
        || !sessionId.equals(session.getId())
        || policyRevision != twoFactorPolicies.current().revision()
        || !Objects.equals(factorRevision, currentFactorRevision(request))
        || method == ProofMethod.GOOGLE_DEVELOPMENT && !twoFactorPolicies.weakGoogleAllowed()) {
      throw new AccessDeniedException("Confirm this security action again");
    }
    if (requiresFactor(request, purpose) && method != ProofMethod.SECOND_FACTOR) {
      throw new AccessDeniedException("Second factor required");
    }
    if (method == ProofMethod.SECOND_FACTOR) {
      twoFactor.authorizeFactorGrant(request, action(purpose), consume);
    } else {
      twoFactor.authorizeCurrent(request, action(purpose), consume);
    }
    if (consume) {
      session.removeAttribute(GRANT);
    }
  }

  public static TwoFactorAction action(Purpose purpose) {
    return switch (purpose) {
      case PASSWORD -> TwoFactorAction.PASSWORD;
      case EMAIL -> TwoFactorAction.EMAIL;
      case LINK_GOOGLE, LINK_MICROSOFT -> TwoFactorAction.LINK_PROVIDER;
      case UNLINK_GOOGLE, UNLINK_MICROSOFT -> TwoFactorAction.UNLINK_PROVIDER;
      case ENROLL_FACTOR -> TwoFactorAction.MANAGE_FACTOR;
    };
  }

  public Optional<Purpose> confirmedPurpose(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || !(session.getAttribute(GRANT) instanceof Grant grant)) {
      return Optional.empty();
    }
    try {
      authorize(request, owner(request), grant.purpose(), false);
      return Optional.of(grant.purpose());
    } catch (AccessDeniedException _) {
      return Optional.empty();
    }
  }

  public UserEntity owner(HttpServletRequest request) {
    String email =
        securityService
            .getAuthenticatedUser(request)
            .orElseThrow(() -> new AccessDeniedException("Sign in required"))
            .getUsername();
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
  }

  public static Purpose linkPurpose(String provider) {
    return switch (provider) {
      case GOOGLE -> Purpose.LINK_GOOGLE;
      case "microsoft" -> Purpose.LINK_MICROSOFT;
      default -> throw new IllegalArgumentException("Unknown provider");
    };
  }

  public static Purpose unlinkPurpose(String provider) {
    return switch (provider) {
      case GOOGLE -> Purpose.UNLINK_GOOGLE;
      case "microsoft" -> Purpose.UNLINK_MICROSOFT;
      default -> throw new IllegalArgumentException("Unknown provider");
    };
  }

  static ExternalIdentityEvidence evidence(String provider, OidcUser user) {
    String email = user.getEmail();
    String domain =
        email != null && email.contains("@") ? email.substring(email.lastIndexOf('@') + 1) : "";
    String hosted = user.getClaimAsString("hd");
    boolean trusted =
        provider.equals(GOOGLE)
            && Boolean.TRUE.equals(user.getEmailVerified())
            && ("gmail.com".equalsIgnoreCase(domain)
                || hosted != null && hosted.equalsIgnoreCase(domain));
    URL issuer = user.getIssuer();
    if (issuer == null) {
      throw new AccessDeniedException("External issuer required");
    }
    return new ExternalIdentityEvidence(
        provider,
        issuer.toString(),
        user.getSubject(),
        email,
        trusted,
        user.getGivenName(),
        user.getFamilyName(),
        user.getAuthenticatedAt());
  }
}
