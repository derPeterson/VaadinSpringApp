package de.derpeterson.app.security.mfa;

import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.TwoFactorAccountEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.TwoFactorChallengeEntity.SetupProof;
import de.derpeterson.app.repository.TwoFactorChallengeRepository;
import de.derpeterson.app.security.ExternalLoginFlow.EnrollmentConfirmation;
import de.derpeterson.app.security.SessionManagementService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Geschützte Einrichtung; Mailbestätigung ist ausschließlich für die echte Google-Ersteinrichtung.
 */
@Service
@Slf4j
public class TwoFactorEnrollment {
  private final TwoFactorAuthorization authorization;
  private final TwoFactorStore factors;
  private final TwoFactorSecrets secrets;
  private final TwoFactorChallengeRepository challenges;
  private final TwoFactorPolicies policies;
  private final ObjectProvider<AuthenticationManager> passwords;
  private final ObjectProvider<SessionManagementService> sessions;
  private final TwoFactorMail mail;
  private final TransactionTemplate transaction;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public record Pending(UUID id, Kind kind, SetupProof proof) {}

  private record CompletedEnrollment(UUID outputId, UUID factorRevision) {}

  @Autowired
  public TwoFactorEnrollment(
      TwoFactorAuthorization authorization,
      TwoFactorStore factors,
      TwoFactorSecrets secrets,
      TwoFactorChallengeRepository challenges,
      TwoFactorPolicies policies,
      ObjectProvider<AuthenticationManager> passwords,
      ObjectProvider<SessionManagementService> sessions,
      TwoFactorMail mail,
      PlatformTransactionManager transactions) {
    this(
        authorization,
        factors,
        secrets,
        challenges,
        policies,
        passwords,
        sessions,
        mail,
        transactions,
        Clock.systemUTC());
  }

  TwoFactorEnrollment(
      TwoFactorAuthorization authorization,
      TwoFactorStore factors,
      TwoFactorSecrets secrets,
      TwoFactorChallengeRepository challenges,
      TwoFactorPolicies policies,
      ObjectProvider<AuthenticationManager> passwords,
      ObjectProvider<SessionManagementService> sessions,
      TwoFactorMail mail,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.authorization = authorization;
    this.factors = factors;
    this.secrets = secrets;
    this.challenges = challenges;
    this.policies = policies;
    this.passwords = passwords;
    this.sessions = sessions;
    this.mail = mail;
    this.clock = clock;
    transaction = new TransactionTemplate(transactions);
  }

  public boolean available() {
    return secrets.available();
  }

  /** Ein Ersatzcode bereitet nur den Wechsel vor. Verbraucht wird er atomar beim Abschluss. */
  public Pending beginRecovery(HttpServletRequest request, String code) {
    if (!available()) {
      throw new IllegalStateException("TOTP encryption key is not configured");
    }
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    String digest = TwoFactorSecrets.recoveryDigest(owner.id(), code);
    Pending pending =
        factors.locked(
            owner.id(),
            owner.accessVersion(),
            account -> {
              if (!factors.checkAdditionalProof(
                  account, factors.hasUnusedRecoveryCode(account, digest))) {
                return null;
              }
              TwoFactorChallengeEntity challenge = create(request, account, Kind.ENROLLMENT);
              challenge.setSetupProof(SetupProof.RECOVERY_CODE);
              challenge.setVerificationDigest(digest);
              prepareSecret(challenge);
              challenges.saveAndFlush(challenge);
              return new Pending(challenge.getId(), challenge.getKind(), challenge.getSetupProof());
            });
    if (pending == null) {
      throw factors.invalidConfirmation(owner.id());
    }
    authorization.clearSessionProof(request);
    return pending;
  }

  Pending beginReplacement(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    TwoFactorAuthorization.State expected = authorization.state(owner);
    if (!expected.enrolled()) {
      throw new AccessDeniedException("Existing factor required");
    }
    // Ein echter TOTP-/Ersatzcode ist auch bei abgeschalteter Rollenregel notwendig.
    authorization.authorizeFactorGrant(request, TwoFactorAction.MANAGE_FACTOR, true);
    return factors.locked(
        owner.id(),
        owner.accessVersion(),
        account -> {
          if (!account.enrolled() || !Objects.equals(account.getRevision(), expected.revision())) {
            throw new AccessDeniedException("Existing factor required");
          }
          TwoFactorChallengeEntity challenge = create(request, account, Kind.ENROLLMENT);
          challenge.setSetupProof(SetupProof.FACTOR);
          prepareSecret(challenge);
          challenges.saveAndFlush(challenge);
          return new Pending(challenge.getId(), challenge.getKind(), challenge.getSetupProof());
        });
  }

  public Pending beginPassword(HttpServletRequest request, String password) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    passwords
        .getObject()
        .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(owner.email(), password));
    TwoFactorAuthorization.Owner current = authorization.owner(request);
    if (!current.id().equals(owner.id())
        || !current.accessVersion().equals(owner.accessVersion())) {
      throw new AccessDeniedException("Account access changed");
    }
    return begin(request, current, SetupProof.PASSWORD);
  }

  public boolean canBeginFromPasswordLogin(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    TwoFactorAuthorization.State state = authorization.state(owner);
    return !state.enrolled()
        && !state.previouslyEnrolled()
        && !state.operatorEnrollment()
        && (authorization.requires(request, TwoFactorAction.LOGIN)
            || authorization.requires(request, TwoFactorAction.ADMINISTRATION))
        && authorization.hasFreshPasswordLogin(request);
  }

  /** Die frische Passwortanmeldung gilt ausschließlich für die verpflichtende Ersteinrichtung. */
  public Pending beginFromPasswordLogin(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    if (!canBeginFromPasswordLogin(request)) {
      throw new AccessDeniedException("Fresh password sign-in for initial enrollment required");
    }
    return begin(request, owner, SetupProof.PASSWORD, true);
  }

  /**
   * Nur nach der vollständigen OIDC-Konto-, State- und Frischeprüfung des ExternalLoginFlow
   * aufrufen.
   */
  public Pending afterProviderConfirmation(
      HttpServletRequest request, EnrollmentConfirmation confirmation) {
    if (!available()) {
      throw new IllegalStateException("TOTP encryption key is not configured");
    }
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    if (confirmation == null
        || !confirmation.matches(
            owner.id(), owner.accessVersion(), request.getSession(false), clock.instant())) {
      throw new AccessDeniedException("Verified provider enrollment required");
    }
    String provider = confirmation.provider();
    TwoFactorAuthorization.State state = authorization.state(owner);
    if (state.operatorEnrollment()) {
      return begin(request, owner, SetupProof.OPERATOR);
    }
    if ("microsoft".equals(provider)) {
      return begin(request, owner, SetupProof.MICROSOFT);
    }
    if (!"google".equals(provider)
        || state.enrolled()
        || state.previouslyEnrolled()
        || !owner.verifiedContact()) {
      throw new AccessDeniedException("Independent enrollment confirmation required");
    }
    return factors.locked(
        owner.id(),
        owner.accessVersion(),
        account -> {
          requireNoFactor(account);
          if (account.isPreviouslyEnrolled()
              || account.isOperatorEnrollment()
              || !account.getUser().getEmail().equals(owner.email())
              || account.getUser().isVerificationPending()) {
            throw new AccessDeniedException("Google email enrollment is not available");
          }
          if (challenges.emailRequests(owner.id(), clock.instant().minusSeconds(60)) > 0
              || challenges.emailRequests(owner.id(), clock.instant().minusSeconds(900)) >= 5) {
            throw new AccessDeniedException("Enrollment email requested too often");
          }
          TwoFactorChallengeEntity challenge = create(request, account, Kind.GOOGLE_EMAIL);
          String code = String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
          challenge.setContactAddress(owner.email());
          challenge.setVerificationDigest(emailDigest(challenge, code));
          challenges.saveAndFlush(challenge);
          mail.setupCode(account.getUser(), code);
          return new Pending(challenge.getId(), challenge.getKind(), challenge.getSetupProof());
        });
  }

  private Pending begin(
      HttpServletRequest request, TwoFactorAuthorization.Owner owner, SetupProof proof) {
    return begin(request, owner, proof, false);
  }

  private Pending begin(
      HttpServletRequest request,
      TwoFactorAuthorization.Owner owner,
      SetupProof proof,
      boolean passwordLogin) {
    if (!secrets.available()) {
      throw new IllegalStateException("TOTP encryption key is not configured");
    }
    return factors.locked(
        owner.id(),
        owner.accessVersion(),
        account -> {
          requireNoFactor(account);
          if (passwordLogin
              && (!canBeginFromPasswordLogin(request)
                  || account.isPreviouslyEnrolled()
                  || account.isOperatorEnrollment())) {
            throw new AccessDeniedException("Initial enrollment changed");
          }
          if (proof == SetupProof.OPERATOR && !account.isOperatorEnrollment()) {
            throw new AccessDeniedException("Operator enrollment is not permitted");
          }
          TwoFactorChallengeEntity challenge = create(request, account, Kind.ENROLLMENT);
          challenge.setSetupProof(proof);
          prepareSecret(challenge);
          challenges.saveAndFlush(challenge);
          return new Pending(challenge.getId(), challenge.getKind(), challenge.getSetupProof());
        });
  }

  public Pending confirmEmail(HttpServletRequest request, UUID id, String code) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    Pending result =
        factors.locked(
            owner.id(),
            owner.accessVersion(),
            account -> {
              TwoFactorChallengeEntity challenge = locked(request, owner, id, Kind.GOOGLE_EMAIL);
              requireNoFactor(account);
              // Acht ASCII-Ziffern. Kein E-Mail-Nachweis für ein früher schon geschütztes Konto.
              boolean valid =
                  code != null
                      && code.matches("[0-9]{8}")
                      && !account.isPreviouslyEnrolled()
                      && !account.isOperatorEnrollment()
                      && !account.getUser().isVerificationPending()
                      && account.getUser().getEmail().equals(challenge.getContactAddress())
                      && Objects.equals(account.getRevision(), challenge.getFactorRevision())
                      && MessageDigest.isEqual(
                          challenge.getVerificationDigest().getBytes(StandardCharsets.US_ASCII),
                          emailDigest(challenge, code).getBytes(StandardCharsets.US_ASCII));
              if (!factors.checkAdditionalProof(account, valid)) {
                challenge.setAttempts(challenge.getAttempts() + 1);
                if (challenge.getAttempts() >= 5) {
                  challenge.setConsumed(true);
                }
                challenges.saveAndFlush(challenge);
                return null;
              }
              challenge.setKind(Kind.ENROLLMENT);
              challenge.setSetupProof(SetupProof.GOOGLE_EMAIL);
              challenge.setVerificationDigest(null);
              prepareSecret(challenge);
              challenges.saveAndFlush(challenge);
              return new Pending(challenge.getId(), challenge.getKind(), challenge.getSetupProof());
            });
    if (result == null) {
      throw factors.invalidConfirmation(owner.id());
    }
    return result;
  }

  public UUID complete(HttpServletRequest request, UUID id, String firstCode) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    CompletedEnrollment completed =
        factors.locked(
            owner.id(),
            owner.accessVersion(),
            account -> {
              TwoFactorChallengeEntity challenge = locked(request, owner, id, Kind.ENROLLMENT);
              if (!Objects.equals(account.getRevision(), challenge.getFactorRevision())
                  || challenge.getSetupProof() == null) {
                throw new AccessDeniedException("Enrollment changed");
              }
              if (challenge.getSetupProof() == SetupProof.RECOVERY_CODE
                  && !factors.hasUnusedRecoveryCode(account, challenge.getVerificationDigest())) {
                throw new AccessDeniedException("Recovery code was already used or changed");
              }
              byte[] secret =
                  secrets.decrypt(owner.id(), "enrollment:" + id, challenge.getEncryptedPayload());
              TwoFactorStore.Enrollment result;
              try {
                if (account.enrolled()
                    && challenge.getSetupProof() != SetupProof.FACTOR
                    && challenge.getSetupProof() != SetupProof.RECOVERY_CODE) {
                  throw new AccessDeniedException("Existing factor requires its own proof");
                }
                result =
                    factors.enrollLocked(
                        account,
                        challenge.getFactorRevision(),
                        secret,
                        firstCode,
                        challenge.getSetupProof() != SetupProof.GOOGLE_EMAIL);
              } finally {
                Arrays.fill(secret, (byte) 0);
              }
              if (result == null) {
                return null;
              }
              challenge.setConsumed(true);
              challenge.setEncryptedPayload(null);
              challenges.saveAndFlush(challenge);
              TwoFactorChallengeEntity codes =
                  recoveryOutput(request, account, result.recoveryCodes());
              codes.setSetupProof(challenge.getSetupProof());
              mail.securityChange(account.getUser(), TwoFactorMail.Event.ENABLED);
              AfterCommitLog.info(log, "Authenticator enrolled: accountId={}", owner.id());
              return new CompletedEnrollment(codes.getId(), result.revision());
            });
    if (completed == null) {
      throw factors.invalidConfirmation(owner.id());
    }
    // Ein konkurrierender Faktorwechsel nach dem Commit darf nicht nachträglich als von
    // dieser Einrichtung bestätigt gelten.
    authorization.markSession(request, owner, completed.factorRevision());
    sessions.getObject().revokeOthers(request);
    return completed.outputId();
  }

  /** Ausschließlich für einen sitzungsgebundenen, nicht gecachten internen Dokument-Endpunkt. */
  public byte[] pendingSecret(HttpServletRequest request, UUID id) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    return transaction.execute(
        status -> {
          TwoFactorChallengeEntity challenge = locked(request, owner, id, Kind.ENROLLMENT);
          return secrets.decrypt(owner.id(), "enrollment:" + id, challenge.getEncryptedPayload());
        });
  }

  public List<String> recoveryOutput(HttpServletRequest request, UUID id) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    authorization.requireSession(request);
    return transaction.execute(
        status -> {
          TwoFactorChallengeEntity challenge = locked(request, owner, id, Kind.RECOVERY_OUTPUT);
          byte[] plaintext =
              secrets.decrypt(owner.id(), "recovery:" + id, challenge.getEncryptedPayload());
          try {
            return List.of(new String(plaintext, StandardCharsets.UTF_8).split("\n"));
          } finally {
            Arrays.fill(plaintext, (byte) 0);
          }
        });
  }

  TwoFactorChallengeEntity recoveryOutput(
      HttpServletRequest request, TwoFactorAccountEntity account, List<String> values) {
    TwoFactorChallengeEntity codes = create(request, account, Kind.RECOVERY_OUTPUT);
    codes.setEncryptedPayload(
        secrets.encrypt(
            account.getUserId(),
            "recovery:" + codes.getId(),
            String.join("\n", values).getBytes(StandardCharsets.UTF_8)));
    return challenges.saveAndFlush(codes);
  }

  public void dismissOutput(HttpServletRequest request, UUID id) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    transaction.executeWithoutResult(
        status -> {
          TwoFactorChallengeEntity challenge = locked(request, owner, id, Kind.RECOVERY_OUTPUT);
          challenge.setEncryptedPayload(null);
          challenge.setConsumed(true);
          challenges.saveAndFlush(challenge);
        });
  }

  public Pending pending(HttpServletRequest request) {
    TwoFactorAuthorization.Owner owner = authorization.owner(request);
    return transaction.execute(
        status -> {
          for (Kind kind : List.of(Kind.GOOGLE_EMAIL, Kind.ENROLLMENT, Kind.RECOVERY_OUTPUT)) {
            List<UUID> ids =
                challenges.active(
                    owner.id(),
                    TwoFactorAuthorization.sessionDigest(request.getSession(false)),
                    kind,
                    TwoFactorAction.MANAGE_FACTOR,
                    clock.instant());
            if (!ids.isEmpty()) {
              try {
                TwoFactorChallengeEntity challenge = locked(request, owner, ids.getFirst(), kind);
                return new Pending(challenge.getId(), kind, challenge.getSetupProof());
              } catch (AccessDeniedException expired) {
                /* Ein veralteter Ablauf wird nicht wiederaufgenommen. */
              }
            }
          }
          return null;
        });
  }

  private TwoFactorChallengeEntity create(
      HttpServletRequest request, TwoFactorAccountEntity account, Kind kind) {
    String session = TwoFactorAuthorization.sessionDigest(request.getSession(false));
    challenges.invalidate(account.getUserId(), session, kind, TwoFactorAction.MANAGE_FACTOR);
    TwoFactorChallengeEntity challenge = new TwoFactorChallengeEntity();
    challenge.setUser(account.getUser());
    challenge.setAccessVersion(account.getUser().getAccessVersion());
    challenge.setSessionDigest(session);
    challenge.setFactorRevision(account.getRevision());
    challenge.setPolicyRevision(policies.current().revision());
    challenge.setAction(TwoFactorAction.MANAGE_FACTOR);
    challenge.setKind(kind);
    challenge.setCreatedAt(clock.instant());
    challenge.setExpiresAt(clock.instant().plusSeconds(300));
    challenge.setOperatorEnrollment(account.isOperatorEnrollment());
    return challenge;
  }

  private TwoFactorChallengeEntity locked(
      HttpServletRequest request, TwoFactorAuthorization.Owner owner, UUID id, Kind kind) {
    TwoFactorChallengeEntity challenge =
        challenges
            .lock(id)
            .orElseThrow(() -> new AccessDeniedException("Two-factor challenge unavailable"));
    if (challenge.isConsumed()
        || challenge.getKind() != kind
        || !challenge.getUser().getId().equals(owner.id())
        || !challenge.getAccessVersion().equals(owner.accessVersion())
        || !challenge
            .getSessionDigest()
            .equals(TwoFactorAuthorization.sessionDigest(request.getSession(false)))
        || !challenge.getExpiresAt().isAfter(clock.instant())
        || challenge.getPolicyRevision() != policies.current().revision()
        || !Objects.equals(challenge.getFactorRevision(), authorization.state(owner).revision())) {
      throw new AccessDeniedException("Two-factor challenge expired or changed");
    }
    return challenge;
  }

  private void requireNoFactor(TwoFactorAccountEntity account) {
    if (account.enrolled()) {
      throw new AccessDeniedException("Existing factor requires its own recovery proof");
    }
  }

  private void prepareSecret(TwoFactorChallengeEntity challenge) {
    byte[] secret = Totp.newSecret();
    try {
      challenge.setEncryptedPayload(
          secrets.encrypt(challenge.getUser().getId(), "enrollment:" + challenge.getId(), secret));
    } finally {
      Arrays.fill(secret, (byte) 0);
    }
  }

  private String emailDigest(TwoFactorChallengeEntity challenge, String code) {
    return secrets.emailVerifier(
        challenge.getUser().getId(), challenge.getId(), challenge.getContactAddress(), code);
  }
}
