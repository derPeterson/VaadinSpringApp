package de.derpeterson.app.security;

import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ExternalIdentityService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

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
    UNLINK_MICROSOFT
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

  private record Grant(
      Long userId, String version, Purpose purpose, Instant expires, String sessionId)
      implements Serializable {}

  private static final String ATTEMPT = ExternalLoginFlow.class.getName() + ".attempt";
  private static final String REGISTRATION = ExternalLoginFlow.class.getName() + ".registration";
  private static final String GRANT = ExternalLoginFlow.class.getName() + ".grant";
  private final ExternalLoginConfiguration.Registrations clients;
  private final ExternalIdentityService identities;
  private final UserRepository users;
  private final CustomUserDetailsService details;
  private final SecurityService security;
  private final SessionManagementService sessions;
  private final AuthenticationManager authenticationManager;
  private Clock clock = Clock.systemUTC();

  public void confirmPassword(HttpServletRequest request, String password, Purpose purpose) {
    Objects.requireNonNull(purpose, "Security action required");
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
    HttpSession session = request.getSession(false);
    session.setAttribute(
        GRANT,
        new Grant(
            current.getId(),
            current.getAccessVersion(),
            purpose,
            clock.instant().plusSeconds(300),
            session.getId()));
  }

  public List<String> providers() {
    return clients.providers();
  }

  public String beginLogin(
      HttpServletRequest request, String provider, boolean remember, Locale locale) {
    if (security.getAuthenticatedUser(request).isPresent()) {
      throw new AccessDeniedException("Already signed in");
    }
    return begin(request, provider, Mode.LOGIN, null, null, remember, locale);
  }

  public String beginProof(
      HttpServletRequest request, String provider, Purpose purpose, Locale locale) {
    Objects.requireNonNull(purpose, "Security action required");
    UserEntity user = owner(request);
    if (!identities.connected(user.getId()).contains(provider)) {
      throw new AccessDeniedException("Provider not connected");
    }
    return begin(request, provider, Mode.PROOF, user, purpose, false, locale);
  }

  public String beginLink(HttpServletRequest request, String provider, Locale locale) {
    UserEntity user = owner(request);
    Purpose purpose = linkPurpose(provider);
    authorize(request, user, purpose, true);
    return begin(request, provider, Mode.LINK, user, purpose, false, locale);
  }

  private String begin(
      HttpServletRequest request,
      String provider,
      Mode mode,
      UserEntity user,
      Purpose purpose,
      boolean remember,
      Locale locale) {
    if (clients.findByRegistrationId(provider) == null) {
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
            clock.instant(),
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
        || started.plusSeconds(300).isBefore(clock.instant())) {
      throw new AccessDeniedException("Start sign-in from the portal");
    }
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
        || attempt.started().plusSeconds(300).isBefore(clock.instant())
        || !Objects.equals(attempt.state(), request.getParameter("state"))
        || !(authentication instanceof OAuth2AuthenticationToken oauth)
        || !attempt.provider().equals(oauth.getAuthorizedClientRegistrationId())
        || !(oauth.getPrincipal() instanceof OidcUser oidc)) {
      throw new AccessDeniedException("External sign-in no longer valid");
    }
    requireProvider(attempt.provider());
    ExternalIdentityEvidence evidence = evidence(attempt.provider(), oidc);
    // Das Framework-OIDC-Token ist kein lokaler Portalzugang und wird nicht gespeichert.
    SecurityContextHolder.clearContext();
    if (attempt.mode() != Mode.LOGIN) {
      completeAccountAction(request, session, attempt, evidence);
      response.sendRedirect("/account?tab=security");
      return;
    }
    Optional<UserEntity> user = identities.resolve(evidence);
    if (user.isPresent()) {
      login(request, response, user.get(), evidence.provider(), attempt.remember());
      return;
    }
    session.setAttribute(
        REGISTRATION,
        new Registration(
            evidence, attempt.locale(), attempt.remember(), clock.instant().plusSeconds(600)));
    response.sendRedirect("/external-registration");
  }

  private void completeAccountAction(
      HttpServletRequest request,
      HttpSession session,
      Attempt attempt,
      ExternalIdentityEvidence evidence) {
    UserEntity user =
        users
            .findById(attempt.userId())
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    if (!user.isEnabled()
        || !Objects.equals(attempt.accessVersion(), user.getAccessVersion())
        || security
            .getAuthenticatedUser(request)
            .filter(principal -> principal.getUsername().equals(user.getEmail()))
            .isEmpty()) {
      throw new AccessDeniedException("Account access changed");
    }
    if (attempt.mode() == Mode.LINK) {
      identities.link(user.getId(), attempt.accessVersion(), evidence);
    } else {
      if (!identities.belongsTo(user.getId(), evidence)) {
        throw new ProofRejected(ProofProblem.IDENTITY_MISMATCH);
      }
      // Freigegebene Google-Zwischenlösung: Ein neuer OIDC-Durchlauf bestätigt das Konto,
      // aber keine erneute Passworteingabe. Diese bekannte Schwäche muss später 2FA ablösen.
      if (!attempt.provider().equals(GOOGLE)) {
        Instant authenticated = evidence.authenticatedAt();
        if (authenticated == null
            || authenticated.isBefore(attempt.started().minusSeconds(5))
            || authenticated.isAfter(clock.instant().plusSeconds(30))) {
          throw new ProofRejected(ProofProblem.FRESH_AUTHENTICATION_REQUIRED);
        }
      }
      session.setAttribute(
          GRANT,
          new Grant(
              user.getId(),
              user.getAccessVersion(),
              attempt.purpose(),
              clock.instant().plusSeconds(300),
              session.getId()));
    }
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
    ExternalAuthenticationToken authentication =
        new ExternalAuthenticationToken(details.loadUserByUsername(user.getEmail()), provider);
    try {
      security.storeVerifiedAuthentication(request, response, authentication, remember);
      return authentication.getAuthorities().stream()
              .anyMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()))
          ? "/admin"
          : "/home";
    } catch (SessionLimitException _) {
      sessions.startExternalRecovery(request, authentication, remember);
      return "/login?error=sessions";
    }
  }

  public Registration registration(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null
        && session.getAttribute(REGISTRATION) instanceof Registration registration
        && registration.expires().isAfter(clock.instant())) {
      requireProvider(registration.identity().provider());
      return registration;
    }
    throw new AccessDeniedException("External registration expired");
  }

  private void requireProvider(String provider) {
    if (!clients.providers().contains(provider)) {
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
    HttpSession session = request.getSession(false);
    if (session == null
        || !(session.getAttribute(GRANT)
            instanceof
            Grant(
                Long userId,
                String version,
                Purpose grantedPurpose,
                Instant expires,
                String sessionId))
        || !userId.equals(user.getId())
        || !version.equals(user.getAccessVersion())
        || grantedPurpose != purpose
        || !expires.isAfter(clock.instant())
        || !sessionId.equals(session.getId())) {
      throw new AccessDeniedException("Confirm this security action again");
    }
    if (consume) {
      session.removeAttribute(GRANT);
    }
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
        security
            .getAuthenticatedUser(request)
            .orElseThrow(() -> new AccessDeniedException("Sign in required"))
            .getUsername();
    return users
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
