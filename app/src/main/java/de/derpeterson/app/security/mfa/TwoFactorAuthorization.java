package de.derpeterson.app.security.mfa;

import de.derpeterson.app.model.TwoFactorChallengeEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.TwoFactorChallengeEntity.SetupProof;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.TwoFactorAccountRepository;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.UserRepository.TwoFactorAccessRow;
import de.derpeterson.app.security.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Aktuelle Rollen, Regeln, Faktorversion und Sitzungsbindung werden an jeder Grenze geprüft. */
@Service
public class TwoFactorAuthorization {
  private static final String SIGN_IN_REQUIRED = "Sign in required";
  public static final String SESSION_PROOF = TwoFactorAuthorization.class.getName() + ".proof";
  private final ObjectProvider<SecurityService> securityServiceProvider;
  private final UserRepository userRepository;
  private final TwoFactorAccountRepository twoFactorAccountRepository;
  private final TwoFactorChallengeRepository twoFactorChallengeRepository;
  private final TwoFactorPolicies twoFactorPolicies;
  private final TwoFactorStore twoFactorStore;
  private final TransactionTemplate transactionTemplate;
  private final Clock authorizationClock;
  private final JsonMapper proofJsonMapper = JsonMapper.builder().build();

  public record Owner(
      Long id,
      String email,
      String accessVersion,
      boolean verifiedContact,
      boolean hasPassword,
      List<RoleType> roles) {}

  public record State(
      boolean enrolled,
      boolean previouslyEnrolled,
      boolean operatorEnrollment,
      UUID revision,
      Instant enrolledAt) {}

  private record SessionProof(
      Long userId,
      String accessVersion,
      UUID factorRevision,
      long policyRevision,
      String sessionDigest,
      Instant verifiedAt,
      Instant activityAt) {}

  @Autowired
  public TwoFactorAuthorization(
      ObjectProvider<SecurityService> securityServiceProvider,
      UserRepository userRepository,
      TwoFactorAccountRepository twoFactorAccountRepository,
      TwoFactorChallengeRepository twoFactorChallengeRepository,
      TwoFactorPolicies twoFactorPolicies,
      TwoFactorStore twoFactorStore,
      PlatformTransactionManager transactionManager) {
    this(
        securityServiceProvider,
        userRepository,
        twoFactorAccountRepository,
        twoFactorChallengeRepository,
        twoFactorPolicies,
        twoFactorStore,
        transactionManager,
        Clock.systemUTC());
  }

  TwoFactorAuthorization(
      ObjectProvider<SecurityService> securityServiceProvider,
      UserRepository userRepository,
      TwoFactorAccountRepository twoFactorAccountRepository,
      TwoFactorChallengeRepository twoFactorChallengeRepository,
      TwoFactorPolicies twoFactorPolicies,
      TwoFactorStore twoFactorStore,
      PlatformTransactionManager transactionManager,
      Clock authorizationClock) {
    this.securityServiceProvider = securityServiceProvider;
    this.userRepository = userRepository;
    this.twoFactorAccountRepository = twoFactorAccountRepository;
    this.twoFactorChallengeRepository = twoFactorChallengeRepository;
    this.twoFactorPolicies = twoFactorPolicies;
    this.twoFactorStore = twoFactorStore;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.authorizationClock = authorizationClock;
  }

  public Owner owner(HttpServletRequest request) {
    String identity =
        securityServiceProvider
            .getObject()
            .getAuthenticatedUser(request)
            .orElseThrow(() -> new AccessDeniedException(SIGN_IN_REQUIRED))
            .getUsername();
    return owner(identity);
  }

  /** Authentifiziert Push-Aktualisierungen aus der gebundenen Sitzung ohne Aktivität zu buchen. */
  public Owner owner(HttpSession session) {
    String identity =
        securityServiceProvider
            .getObject()
            .refreshSessionAuthentication(session)
            .orElseThrow(() -> new AccessDeniedException(SIGN_IN_REQUIRED))
            .getName();
    return owner(identity);
  }

  private Owner owner(String identity) {
    List<TwoFactorAccessRow> rows = userRepository.findTwoFactorAccess(identity);
    if (rows.isEmpty() || !rows.getFirst().getEnabled()) {
      throw new AccessDeniedException("Account unavailable");
    }
    TwoFactorAccessRow account = rows.getFirst();
    return new Owner(
        account.getId(),
        account.getEmail(),
        account.getAccessVersion(),
        !account.getVerificationPending(),
        account.getHasPassword(),
        rows.stream()
            .map(TwoFactorAccessRow::getRole)
            .filter(Objects::nonNull)
            .distinct()
            .toList());
  }

  public State state(Owner owner) {
    return twoFactorAccountRepository
        .state(owner.id())
        .map(row ->
            new State(
                row.getEnrolled(),
                row.getPreviouslyEnrolled(),
                row.getOperatorEnrollment(),
                row.getRevision(),
                row.getEnrolledAt()))
        .orElse(new State(false, false, false, null, null));
  }

  public boolean requires(HttpServletRequest request, TwoFactorAction action) {
    Owner owner = owner(request);
    return required(owner, state(owner), twoFactorPolicies.current(), action);
  }

  /** Nur der echte Passwortnachweis der gebundenen Sitzung, höchstens fünf Minuten alt. */
  public boolean hasFreshPasswordLogin(HttpServletRequest request) {
    Owner owner = owner(request);
    Instant proofCheckTime = authorizationClock.instant();

    return owner.hasPassword()
        && securityServiceProvider
            .getObject()
            .refreshSessionAuthentication(request.getSession(false))
            .filter(UsernamePasswordAuthenticationToken.class::isInstance)
            .filter(authentication ->
                authentication.isAuthenticated() && authentication.getName().equals(owner.email()))
            .stream()
            .flatMap(authentication -> authentication.getAuthorities().stream())
            .filter(FactorGrantedAuthority.class::isInstance)
            .map(FactorGrantedAuthority.class::cast)
            .anyMatch(factor ->
                FactorGrantedAuthority.PASSWORD_AUTHORITY.equals(factor.getAuthority())
                    && !factor.getIssuedAt().isAfter(proofCheckTime)
                    && factor.getIssuedAt().plusSeconds(300).isAfter(proofCheckTime));
  }

  private boolean required(
      Owner owner, State factor, TwoFactorPolicies.Snapshot configuration, TwoFactorAction action) {
    if (factor.operatorEnrollment()) {
      return true;
    }

    return !twoFactorPolicies.pauseEnabled(configuration)
        && configuration.policy().mode(owner.roles(), action).requiresFactor(factor.enrolled());
  }

  public boolean sessionConfirmed(HttpServletRequest request) {
    return sessionConfirmed(request, TwoFactorAction.LOGIN);
  }

  public boolean administrationConfirmed(HttpServletRequest request) {
    return sessionConfirmed(request) && sessionConfirmed(request, TwoFactorAction.ADMINISTRATION);
  }

  public boolean administrationConfirmed(Owner owner, HttpSession session) {
    return sessionConfirmed(owner, session, TwoFactorAction.LOGIN)
        && sessionConfirmed(owner, session, TwoFactorAction.ADMINISTRATION);
  }

  private boolean sessionConfirmed(HttpServletRequest request, TwoFactorAction action) {
    Owner owner = owner(request);
    return sessionConfirmed(owner, request.getSession(false), action);
  }

  private boolean sessionConfirmed(Owner owner, HttpSession session, TwoFactorAction action) {
    State factor = state(owner);
    TwoFactorPolicies.Snapshot configuration = twoFactorPolicies.current();
    if (session != null
        && twoFactorChallengeRepository.setupInProgress(
            owner.id(),
            sessionDigest(session),
            Kind.ENROLLMENT,
            SetupProof.RECOVERY_CODE,
            authorizationClock.instant())) {
      return false;
    }
    if (!required(owner, factor, configuration, action)) {
      return true;
    }
    SessionProof proof = sessionProof(session);
    return validSessionProof(owner, factor, configuration, session, proof);
  }

  private boolean validSessionProof(
      Owner owner,
      State factor,
      TwoFactorPolicies.Snapshot configuration,
      HttpSession session,
      SessionProof proof) {
    return factor.enrolled()
        && proof != null
        && proof.userId().equals(owner.id())
        && proof.accessVersion().equals(owner.accessVersion())
        && proof.factorRevision().equals(factor.revision())
        && proof.policyRevision() == configuration.revision()
        && proof.sessionDigest().equals(sessionDigest(session))
        && !proof.verifiedAt().isAfter(authorizationClock.instant())
        && (!configuration.policy().inactivityEnabled()
            || proof
                .activityAt()
                .plus(configuration.policy().inactivity())
                .isAfter(authorizationClock.instant()));
  }

  public void requireSession(HttpServletRequest request) {
    if (!sessionConfirmed(request)) {
      throw new AccessDeniedException("Two-factor sign-in required");
    }
  }

  /** Auch Push-Callbacks prüfen den aktuellen Nachweis ohne einen Servlet-Request-Proxy. */
  public void requireSession(HttpSession session) {
    String identity =
        securityServiceProvider
            .getObject()
            .getSessionUser(session)
            .orElseThrow(() -> new AccessDeniedException(SIGN_IN_REQUIRED))
            .getEmail();
    if (!sessionConfirmed(owner(identity), session, TwoFactorAction.LOGIN)) {
      throw new AccessDeniedException("Two-factor sign-in required");
    }
  }

  public void clearSessionProof(HttpServletRequest request) {
    owner(request);
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(SESSION_PROOF);
    }
    securityServiceProvider.getObject().clearSecondFactor(request);
  }

  /**
   * Nur für eine nach bestätigter Zugangsänderung beibehaltene Sitzung. Der Nachweiszeitpunkt
   * bleibt erhalten.
   */
  public void retainAfterAccessVersionChange(
      HttpServletRequest request, String previousAccessVersion) {
    Owner current = owner(request);
    HttpSession session = request.getSession(false);
    SessionProof proof = sessionProof(session);
    Owner previous =
        new Owner(
            current.id(),
            current.email(),
            previousAccessVersion,
            current.verifiedContact(),
            current.hasPassword(),
            current.roles());
    if (validSessionProof(previous, state(current), twoFactorPolicies.current(), session, proof)) {
      session.setAttribute(
          SESSION_PROOF,
          proofJsonMapper.writeValueAsString(
              new SessionProof(
                  current.id(),
                  current.accessVersion(),
                  proof.factorRevision(),
                  proof.policyRevision(),
                  proof.sessionDigest(),
                  proof.verifiedAt(),
                  proof.activityAt())));
    }
  }

  public void recordActivity(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    SessionProof proof = sessionProof(session);
    Owner owner = owner(request);
    if (validSessionProof(owner, state(owner), twoFactorPolicies.current(), session, proof)) {
      session.setAttribute(
          SESSION_PROOF,
          proofJsonMapper.writeValueAsString(
              new SessionProof(
                  proof.userId(),
                  proof.accessVersion(),
                  proof.factorRevision(),
                  proof.policyRevision(),
                  proof.sessionDigest(),
                  proof.verifiedAt(),
                  authorizationClock.instant())));
    }
  }

  /** Eigene Ersatzcodes erneuern ändert die Faktorversion, aber weder Nachweisart noch Frische. */
  void retainAfterFactorChange(
      HttpServletRequest request, UUID previousRevision, UUID expectedRevision) {
    Owner owner = owner(request);
    State current = state(owner);
    if (!current.enrolled() || !Objects.equals(current.revision(), expectedRevision)) {
      return;
    }
    HttpSession session = request.getSession(false);
    SessionProof proof = sessionProof(session);
    State previous =
        new State(
            true,
            current.previouslyEnrolled(),
            current.operatorEnrollment(),
            previousRevision,
            current.enrolledAt());
    if (validSessionProof(owner, previous, twoFactorPolicies.current(), session, proof)) {
      session.setAttribute(
          SESSION_PROOF,
          proofJsonMapper.writeValueAsString(
              new SessionProof(
                  owner.id(),
                  owner.accessVersion(),
                  expectedRevision,
                  proof.policyRevision(),
                  proof.sessionDigest(),
                  proof.verifiedAt(),
                  proof.activityAt())));
    }
  }

  public void confirmSession(HttpServletRequest request, String code) {
    Owner owner = owner(request);
    TwoFactorStore.Verified verified =
        twoFactorStore.verify(owner.id(), owner.accessVersion(), code);
    markSession(request, owner, verified.revision(), verified.recoveryCode());
  }

  /** Der Nachweis gilt nur für die unmittelbar folgende Kontoaktion. */
  public UUID verifyAccountLifecycleCode(Owner accountOwner, String authenticatorCode) {
    return twoFactorStore
        .verify(accountOwner.id(), accountOwner.accessVersion(), authenticatorCode)
        .revision();
  }

  void markSession(HttpServletRequest request, Owner owner, UUID revision) {
    markSession(request, owner, revision, false);
  }

  private void markSession(
      HttpServletRequest request, Owner owner, UUID revision, boolean recoveryCode) {
    Owner current = owner(request);
    State state = state(current);
    if (!current.id().equals(owner.id())
        || !current.accessVersion().equals(owner.accessVersion())
        || !state.enrolled()
        || !Objects.equals(state.revision(), revision)) {
      throw new AccessDeniedException("Two-factor account changed");
    }
    HttpSession session = request.getSession(false);
    session.setAttribute(
        SESSION_PROOF,
        proofJsonMapper.writeValueAsString(
            new SessionProof(
                owner.id(),
                owner.accessVersion(),
                revision,
                twoFactorPolicies.current().revision(),
                sessionDigest(session),
                authorizationClock.instant(),
                authorizationClock.instant())));
    securityServiceProvider
        .getObject()
        .recordSecondFactor(request, recoveryCode, authorizationClock.instant());
  }

  public void confirmAction(HttpServletRequest request, TwoFactorAction action, String code) {
    requireSession(request);
    Owner owner = owner(request);
    TwoFactorStore.Verified verified =
        twoFactorStore.verify(owner.id(), owner.accessVersion(), code);
    transactionTemplate.executeWithoutResult(status -> {
      State current = state(owner);
      if (!current.enrolled() || !Objects.equals(current.revision(), verified.revision())) {
        throw new AccessDeniedException("Two-factor account changed");
      }
      String session = sessionDigest(request.getSession(false));
      twoFactorChallengeRepository.invalidate(owner.id(), session, Kind.ACTION, action);
      TwoFactorChallengeEntity grant = new TwoFactorChallengeEntity();
      grant.setUser(userRepository.getReferenceById(owner.id()));
      grant.setAccessVersion(owner.accessVersion());
      grant.setSessionDigest(session);
      grant.setFactorRevision(verified.revision());
      grant.setPolicyRevision(twoFactorPolicies.current().revision());
      grant.setAction(action);
      grant.setKind(Kind.ACTION);
      grant.setCreatedAt(authorizationClock.instant());
      grant.setExpiresAt(authorizationClock.instant().plusSeconds(300));
      twoFactorChallengeRepository.saveAndFlush(grant);
    });
  }

  public void authorizeCurrent(
      HttpServletRequest request, TwoFactorAction action, boolean consume) {
    Owner owner = owner(request);
    authorize(request, owner, action, consume);
  }

  public void authorize(
      HttpServletRequest request, Owner owner, TwoFactorAction action, boolean consume) {
    requireSession(request);
    Owner current = owner(request);
    if (!owner.id().equals(current.id())
        || !owner.accessVersion().equals(current.accessVersion())) {
      throw new AccessDeniedException("Account access changed");
    }
    State factor = state(current);
    TwoFactorPolicies.Snapshot configuration = twoFactorPolicies.current();
    if (!required(current, factor, configuration, action)) {
      return;
    }
    authorizeFactorGrant(request, action, consume);
  }

  /**
   * Faktorverwaltung darf auch bei ausgeschalteter Richtlinie keinen vorhandenen Faktor umgehen.
   */
  public void authorizeFactorGrant(
      HttpServletRequest request, TwoFactorAction action, boolean consume) {
    requireSession(request);
    Owner current = owner(request);
    State factor = state(current);
    TwoFactorPolicies.Snapshot configuration = twoFactorPolicies.current();
    if (!factor.enrolled()) {
      throw new AccessDeniedException("Set up two-factor authentication first");
    }
    String session = sessionDigest(request.getSession(false));
    Boolean accepted =
        transactionTemplate.execute(status -> {
          List<UUID> grants =
              twoFactorChallengeRepository.active(
                  current.id(), session, Kind.ACTION, action, authorizationClock.instant());
          if (grants.isEmpty()) {
            return false;
          }
          TwoFactorChallengeEntity grant =
              twoFactorChallengeRepository.lock(grants.getFirst()).orElseThrow();
          if (grant.isConsumed()
              || !grant.getExpiresAt().isAfter(authorizationClock.instant())
              || !grant.getAccessVersion().equals(current.accessVersion())
              || !grant.getFactorRevision().equals(factor.revision())
              || grant.getPolicyRevision() != configuration.revision()) {
            return false;
          }
          if (consume) {
            grant.setConsumed(true);
            twoFactorChallengeRepository.saveAndFlush(grant);
          }
          return true;
        });
    if (!Boolean.TRUE.equals(accepted)) {
      throw new AccessDeniedException("Confirm this action with your second factor");
    }
  }

  private SessionProof sessionProof(HttpSession session) {
    if (session == null || !(session.getAttribute(SESSION_PROOF) instanceof String json)) {
      return null;
    }
    try {
      return proofJsonMapper.readValue(json, SessionProof.class);
    } catch (RuntimeException _) {
      session.removeAttribute(SESSION_PROOF);
      return null;
    }
  }

  static String sessionDigest(HttpSession session) {
    if (session == null) {
      throw new AccessDeniedException("Session required");
    }
    return sessionDigest(session.getId());
  }

  /**
   * Sitzungsbindung für Bereinigung und Autorisierung ohne Speicherung zusätzlicher Session-IDs.
   */
  public static String sessionDigest(String sessionId) {
    return TwoFactorSecrets.digest(sessionId);
  }
}
