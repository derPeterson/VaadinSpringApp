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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Aktuelle Rollen, Regeln, Faktorversion und Sitzungsbindung werden an jeder Grenze geprüft. */
@Service
public class TwoFactorAuthorization {
  public static final String SESSION_PROOF = TwoFactorAuthorization.class.getName() + ".proof";
  private final ObjectProvider<SecurityService> security;
  private final UserRepository users;
  private final TwoFactorAccountRepository accounts;
  private final TwoFactorChallengeRepository challenges;
  private final TwoFactorPolicies policies;
  private final TwoFactorStore factors;
  private final TransactionTemplate transaction;
  private final Clock clock;
  private final JsonMapper mapper = JsonMapper.builder().build();

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
      ObjectProvider<SecurityService> security,
      UserRepository users,
      TwoFactorAccountRepository accounts,
      TwoFactorChallengeRepository challenges,
      TwoFactorPolicies policies,
      TwoFactorStore factors,
      PlatformTransactionManager transactions) {
    this(security, users, accounts, challenges, policies, factors, transactions, Clock.systemUTC());
  }

  TwoFactorAuthorization(
      ObjectProvider<SecurityService> security,
      UserRepository users,
      TwoFactorAccountRepository accounts,
      TwoFactorChallengeRepository challenges,
      TwoFactorPolicies policies,
      TwoFactorStore factors,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.security = security;
    this.users = users;
    this.accounts = accounts;
    this.challenges = challenges;
    this.policies = policies;
    this.factors = factors;
    this.transaction = new TransactionTemplate(transactions);
    this.clock = clock;
  }

  public Owner owner(HttpServletRequest request) {
    String identity =
        security
            .getObject()
            .getAuthenticatedUser(request)
            .orElseThrow(() -> new AccessDeniedException("Sign in required"))
            .getUsername();
    return owner(identity);
  }

  private Owner owner(String identity) {
    List<TwoFactorAccessRow> rows = users.findTwoFactorAccess(identity);
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
    return accounts
        .state(owner.id())
        .map(
            row ->
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
    return required(owner, state(owner), policies.current(), action);
  }

  private boolean required(
      Owner owner, State factor, TwoFactorPolicies.Snapshot configuration, TwoFactorAction action) {
    if (factor.operatorEnrollment()) {
      return true;
    }
    return !policies.pauseEnabled(configuration)
        && configuration.policy().mode(owner.roles(), action).requiresFactor(factor.enrolled());
  }

  public boolean sessionConfirmed(HttpServletRequest request) {
    return sessionConfirmed(request, TwoFactorAction.LOGIN);
  }

  public boolean administrationConfirmed(HttpServletRequest request) {
    return sessionConfirmed(request) && sessionConfirmed(request, TwoFactorAction.ADMINISTRATION);
  }

  private boolean sessionConfirmed(HttpServletRequest request, TwoFactorAction action) {
    Owner owner = owner(request);
    return sessionConfirmed(owner, request.getSession(false), action);
  }

  private boolean sessionConfirmed(Owner owner, HttpSession session, TwoFactorAction action) {
    State factor = state(owner);
    TwoFactorPolicies.Snapshot configuration = policies.current();
    if (session != null
        && challenges.setupInProgress(
            owner.id(),
            sessionDigest(session),
            Kind.ENROLLMENT,
            SetupProof.RECOVERY_CODE,
            clock.instant())) {
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
        && !proof.verifiedAt().isAfter(clock.instant())
        && (!configuration.policy().inactivityEnabled()
            || proof
                .activityAt()
                .plus(configuration.policy().inactivity())
                .isAfter(clock.instant()));
  }

  public void requireSession(HttpServletRequest request) {
    if (!sessionConfirmed(request)) {
      throw new AccessDeniedException("Two-factor sign-in required");
    }
  }

  /** Auch Push-Callbacks prüfen den aktuellen Nachweis ohne einen Servlet-Request-Proxy. */
  public void requireSession(HttpSession session) {
    String identity =
        security
            .getObject()
            .getSessionUser(session)
            .orElseThrow(() -> new AccessDeniedException("Sign in required"))
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
    security.getObject().clearSecondFactor(request);
  }

  /** Nur nach dem bestätigten eigenen Passwortwechsel, ohne den Nachweiszeitpunkt zu erneuern. */
  public void retainAfterPasswordChange(HttpServletRequest request, String previousAccessVersion) {
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
    if (validSessionProof(previous, state(current), policies.current(), session, proof)) {
      session.setAttribute(
          SESSION_PROOF,
          mapper.writeValueAsString(
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
    if (validSessionProof(owner, state(owner), policies.current(), session, proof)) {
      session.setAttribute(
          SESSION_PROOF,
          mapper.writeValueAsString(
              new SessionProof(
                  proof.userId(),
                  proof.accessVersion(),
                  proof.factorRevision(),
                  proof.policyRevision(),
                  proof.sessionDigest(),
                  proof.verifiedAt(),
                  clock.instant())));
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
    if (validSessionProof(owner, previous, policies.current(), session, proof)) {
      session.setAttribute(
          SESSION_PROOF,
          mapper.writeValueAsString(
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
    TwoFactorStore.Verified verified = factors.verify(owner.id(), owner.accessVersion(), code);
    markSession(request, owner, verified.revision(), verified.recoveryCode());
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
        mapper.writeValueAsString(
            new SessionProof(
                owner.id(),
                owner.accessVersion(),
                revision,
                policies.current().revision(),
                sessionDigest(session),
                clock.instant(),
                clock.instant())));
    security.getObject().recordSecondFactor(request, recoveryCode, clock.instant());
  }

  public void confirmAction(HttpServletRequest request, TwoFactorAction action, String code) {
    requireSession(request);
    Owner owner = owner(request);
    TwoFactorStore.Verified verified = factors.verify(owner.id(), owner.accessVersion(), code);
    transaction.executeWithoutResult(
        status -> {
          State current = state(owner);
          if (!current.enrolled() || !Objects.equals(current.revision(), verified.revision())) {
            throw new AccessDeniedException("Two-factor account changed");
          }
          String session = sessionDigest(request.getSession(false));
          challenges.invalidate(owner.id(), session, Kind.ACTION, action);
          TwoFactorChallengeEntity grant = new TwoFactorChallengeEntity();
          grant.setUser(users.getReferenceById(owner.id()));
          grant.setAccessVersion(owner.accessVersion());
          grant.setSessionDigest(session);
          grant.setFactorRevision(verified.revision());
          grant.setPolicyRevision(policies.current().revision());
          grant.setAction(action);
          grant.setKind(Kind.ACTION);
          grant.setCreatedAt(clock.instant());
          grant.setExpiresAt(clock.instant().plusSeconds(300));
          challenges.saveAndFlush(grant);
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
    TwoFactorPolicies.Snapshot configuration = policies.current();
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
    TwoFactorPolicies.Snapshot configuration = policies.current();
    if (!factor.enrolled()) {
      throw new AccessDeniedException("Set up two-factor authentication first");
    }
    String session = sessionDigest(request.getSession(false));
    Boolean accepted =
        transaction.execute(
            status -> {
              List<UUID> grants =
                  challenges.active(current.id(), session, Kind.ACTION, action, clock.instant());
              if (grants.isEmpty()) {
                return false;
              }
              TwoFactorChallengeEntity grant = challenges.lock(grants.getFirst()).orElseThrow();
              if (grant.isConsumed()
                  || !grant.getExpiresAt().isAfter(clock.instant())
                  || !grant.getAccessVersion().equals(current.accessVersion())
                  || !grant.getFactorRevision().equals(factor.revision())
                  || grant.getPolicyRevision() != configuration.revision()) {
                return false;
              }
              if (consume) {
                grant.setConsumed(true);
                challenges.saveAndFlush(grant);
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
      return mapper.readValue(json, SessionProof.class);
    } catch (RuntimeException invalid) {
      session.removeAttribute(SESSION_PROOF);
      return null;
    }
  }

  static String sessionDigest(HttpSession session) {
    if (session == null) {
      throw new AccessDeniedException("Session required");
    }
    return TwoFactorSecrets.digest(session.getId());
  }
}
