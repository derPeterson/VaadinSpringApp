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
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
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
  private static final String ENROLLMENT_PREFIX = "enrollment:";
  // Die Codeausgabe endet mit der Sitzung oder Bestätigung, nicht mit einer Bestätigungsfrist.
  private static final Instant SESSION_BOUND_OUTPUT = Instant.parse("9999-12-31T23:59:59Z");
  private final TwoFactorAuthorization twoFactorAuthorization;
  private final TwoFactorStore twoFactorStore;
  private final TwoFactorSecrets twoFactorSecrets;
  private final TwoFactorChallengeRepository twoFactorChallengeRepository;
  private final TwoFactorPolicies twoFactorPolicies;
  private final ObjectProvider<AuthenticationManager> passwordAuthenticationManagerProvider;
  private final ObjectProvider<SessionManagementService> sessionManagementServiceProvider;
  private final TwoFactorMail twoFactorMail;
  private final TransactionTemplate transactionTemplate;
  private final Clock twoFactorClock;
  private final SecureRandom secureRandom = new SecureRandom();

  public record Pending(UUID id, Kind kind, SetupProof proof) implements Serializable {}

  private record CompletedEnrollment(UUID outputId, UUID factorRevision) {}

  @Autowired
  public TwoFactorEnrollment(
      TwoFactorAuthorization twoFactorAuthorization,
      TwoFactorStore twoFactorStore,
      TwoFactorSecrets twoFactorSecrets,
      TwoFactorChallengeRepository twoFactorChallengeRepository,
      TwoFactorPolicies twoFactorPolicies,
      ObjectProvider<AuthenticationManager> passwordAuthenticationManagerProvider,
      ObjectProvider<SessionManagementService> sessionManagementServiceProvider,
      TwoFactorMail twoFactorMail,
      PlatformTransactionManager transactionManager) {
    this(
        twoFactorAuthorization,
        twoFactorStore,
        twoFactorSecrets,
        twoFactorChallengeRepository,
        twoFactorPolicies,
        passwordAuthenticationManagerProvider,
        sessionManagementServiceProvider,
        twoFactorMail,
        transactionManager,
        Clock.systemUTC());
  }

  TwoFactorEnrollment(
      TwoFactorAuthorization twoFactorAuthorization,
      TwoFactorStore twoFactorStore,
      TwoFactorSecrets twoFactorSecrets,
      TwoFactorChallengeRepository twoFactorChallengeRepository,
      TwoFactorPolicies twoFactorPolicies,
      ObjectProvider<AuthenticationManager> passwordAuthenticationManagerProvider,
      ObjectProvider<SessionManagementService> sessionManagementServiceProvider,
      TwoFactorMail twoFactorMail,
      PlatformTransactionManager transactionManager,
      Clock twoFactorClock) {
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.twoFactorStore = twoFactorStore;
    this.twoFactorSecrets = twoFactorSecrets;
    this.twoFactorChallengeRepository = twoFactorChallengeRepository;
    this.twoFactorPolicies = twoFactorPolicies;
    this.passwordAuthenticationManagerProvider = passwordAuthenticationManagerProvider;
    this.sessionManagementServiceProvider = sessionManagementServiceProvider;
    this.twoFactorMail = twoFactorMail;
    this.twoFactorClock = twoFactorClock;
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public boolean available() {
    return twoFactorSecrets.available();
  }

  /** Ein Ersatzcode bereitet nur den Wechsel vor. Verbraucht wird er atomar beim Abschluss. */
  public Pending beginRecovery(HttpServletRequest httpRequest, String code) {
    if (!available()) {
      throw new IllegalStateException("TOTP encryption key is not configured");
    }
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    String digest = TwoFactorSecrets.recoveryDigest(owner.id(), code);
    Pending pending =
        twoFactorStore.locked(
            owner.id(),
            owner.accessVersion(),
            twoFactorAccountEntity -> {
              if (!twoFactorStore.checkAdditionalProof(
                  twoFactorAccountEntity,
                  twoFactorStore.hasUnusedRecoveryCode(twoFactorAccountEntity, digest))) {
                return null;
              }
              TwoFactorChallengeEntity twoFactorChallengeEntity =
                  createTwoFactorChallengeEntity(
                      httpRequest, twoFactorAccountEntity, Kind.ENROLLMENT);
              twoFactorChallengeEntity.setSetupProof(SetupProof.RECOVERY_CODE);
              twoFactorChallengeEntity.setVerificationDigest(digest);
              prepareSecret(twoFactorChallengeEntity);
              twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
              return new Pending(
                  twoFactorChallengeEntity.getId(),
                  twoFactorChallengeEntity.getKind(),
                  twoFactorChallengeEntity.getSetupProof());
            });
    if (pending == null) {
      throw twoFactorStore.invalidConfirmation(owner.id());
    }
    twoFactorAuthorization.clearSessionProof(httpRequest);
    return pending;
  }

  Pending beginReplacement(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    TwoFactorAuthorization.State expected = twoFactorAuthorization.state(owner);
    if (!expected.enrolled()) {
      throw new AccessDeniedException("Existing factor required");
    }
    // Ein echter TOTP-/Ersatzcode ist auch bei abgeschalteter Rollenregel notwendig.
    twoFactorAuthorization.authorizeFactorGrant(httpRequest, TwoFactorAction.MANAGE_FACTOR, true);
    return twoFactorStore.locked(
        owner.id(),
        owner.accessVersion(),
        twoFactorAccountEntity -> {
          if (!twoFactorAccountEntity.enrolled()
              || !Objects.equals(twoFactorAccountEntity.getRevision(), expected.revision())) {
            throw new AccessDeniedException("Existing factor required");
          }
          TwoFactorChallengeEntity twoFactorChallengeEntity =
              createTwoFactorChallengeEntity(httpRequest, twoFactorAccountEntity, Kind.ENROLLMENT);
          twoFactorChallengeEntity.setSetupProof(SetupProof.FACTOR);
          prepareSecret(twoFactorChallengeEntity);
          twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
          return new Pending(
              twoFactorChallengeEntity.getId(),
              twoFactorChallengeEntity.getKind(),
              twoFactorChallengeEntity.getSetupProof());
        });
  }

  public Pending beginPassword(HttpServletRequest httpRequest, String password) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    passwordAuthenticationManagerProvider
        .getObject()
        .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(owner.email(), password));
    TwoFactorAuthorization.Owner current = twoFactorAuthorization.owner(httpRequest);
    if (!current.id().equals(owner.id())
        || !current.accessVersion().equals(owner.accessVersion())) {
      throw new AccessDeniedException("Account access changed");
    }
    return begin(httpRequest, current, SetupProof.PASSWORD);
  }

  public boolean canBeginFromPasswordLogin(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    TwoFactorAuthorization.State state = twoFactorAuthorization.state(owner);
    return !state.enrolled()
        && !state.previouslyEnrolled()
        && !state.operatorEnrollment()
        && (twoFactorAuthorization.requires(httpRequest, TwoFactorAction.LOGIN)
            || twoFactorAuthorization.requires(httpRequest, TwoFactorAction.ADMINISTRATION))
        && twoFactorAuthorization.hasFreshPasswordLogin(httpRequest);
  }

  /** Die frische Passwortanmeldung gilt ausschließlich für die verpflichtende Ersteinrichtung. */
  public Pending beginFromPasswordLogin(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    if (!canBeginFromPasswordLogin(httpRequest)) {
      throw new AccessDeniedException("Fresh password sign-in for initial enrollment required");
    }
    return begin(httpRequest, owner, SetupProof.PASSWORD, true);
  }

  /**
   * Nur nach der vollständigen OIDC-Konto-, State- und Frischeprüfung des ExternalLoginFlow
   * aufrufen.
   */
  public Pending afterProviderConfirmation(
      HttpServletRequest httpRequest, EnrollmentConfirmation confirmation) {
    if (!available()) {
      throw new IllegalStateException("TOTP encryption key is not configured");
    }
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    if (confirmation == null
        || !confirmation.matches(
            owner.id(),
            owner.accessVersion(),
            httpRequest.getSession(false),
            twoFactorClock.instant())) {
      throw new AccessDeniedException("Verified provider enrollment required");
    }
    String provider = confirmation.provider();
    TwoFactorAuthorization.State state = twoFactorAuthorization.state(owner);
    if (state.operatorEnrollment()) {
      return begin(httpRequest, owner, SetupProof.OPERATOR);
    }
    if ("microsoft".equals(provider)) {
      return begin(httpRequest, owner, SetupProof.MICROSOFT);
    }
    if (!"google".equals(provider)
        || state.enrolled()
        || state.previouslyEnrolled()
        || !owner.verifiedContact()) {
      throw new AccessDeniedException("Independent enrollment confirmation required");
    }
    return twoFactorStore.locked(
        owner.id(),
        owner.accessVersion(),
        twoFactorAccountEntity -> {
          requireNoFactor(twoFactorAccountEntity);
          if (twoFactorAccountEntity.isPreviouslyEnrolled()
              || twoFactorAccountEntity.isOperatorEnrollment()
              || !twoFactorAccountEntity.getUser().getEmail().equals(owner.email())
              || twoFactorAccountEntity.getUser().isVerificationPending()) {
            throw new AccessDeniedException("Google email enrollment is not available");
          }
          if (twoFactorChallengeRepository.emailRequests(
                      owner.id(), twoFactorClock.instant().minusSeconds(60))
                  > 0
              || twoFactorChallengeRepository.emailRequests(
                      owner.id(), twoFactorClock.instant().minusSeconds(900))
                  >= 5) {
            throw new AccessDeniedException("Enrollment email requested too often");
          }
          TwoFactorChallengeEntity twoFactorChallengeEntity =
              createTwoFactorChallengeEntity(
                  httpRequest, twoFactorAccountEntity, Kind.GOOGLE_EMAIL);
          String code = String.format(Locale.ROOT, "%08d", secureRandom.nextInt(100_000_000));
          twoFactorChallengeEntity.setContactAddress(owner.email());
          twoFactorChallengeEntity.setVerificationDigest(
              emailDigest(twoFactorChallengeEntity, code));
          twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
          twoFactorMail.setupCode(twoFactorAccountEntity.getUser(), code);
          return new Pending(
              twoFactorChallengeEntity.getId(),
              twoFactorChallengeEntity.getKind(),
              twoFactorChallengeEntity.getSetupProof());
        });
  }

  private Pending begin(
      HttpServletRequest httpRequest, TwoFactorAuthorization.Owner owner, SetupProof proof) {
    return begin(httpRequest, owner, proof, false);
  }

  private Pending begin(
      HttpServletRequest httpRequest,
      TwoFactorAuthorization.Owner owner,
      SetupProof proof,
      boolean passwordLogin) {
    if (!twoFactorSecrets.available()) {
      throw new IllegalStateException("TOTP encryption key is not configured");
    }
    return twoFactorStore.locked(
        owner.id(),
        owner.accessVersion(),
        twoFactorAccountEntity -> {
          requireNoFactor(twoFactorAccountEntity);
          if (passwordLogin
              && (!canBeginFromPasswordLogin(httpRequest)
                  || twoFactorAccountEntity.isPreviouslyEnrolled()
                  || twoFactorAccountEntity.isOperatorEnrollment())) {
            throw new AccessDeniedException("Initial enrollment changed");
          }
          if (proof == SetupProof.OPERATOR && !twoFactorAccountEntity.isOperatorEnrollment()) {
            throw new AccessDeniedException("Operator enrollment is not permitted");
          }
          TwoFactorChallengeEntity twoFactorChallengeEntity =
              createTwoFactorChallengeEntity(httpRequest, twoFactorAccountEntity, Kind.ENROLLMENT);
          twoFactorChallengeEntity.setSetupProof(proof);
          prepareSecret(twoFactorChallengeEntity);
          twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
          return new Pending(
              twoFactorChallengeEntity.getId(),
              twoFactorChallengeEntity.getKind(),
              twoFactorChallengeEntity.getSetupProof());
        });
  }

  public Pending confirmEmail(HttpServletRequest httpRequest, UUID id, String code) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    Pending result =
        twoFactorStore.locked(
            owner.id(),
            owner.accessVersion(),
            twoFactorAccountEntity -> {
              TwoFactorChallengeEntity twoFactorChallengeEntity =
                  locked(httpRequest, owner, id, Kind.GOOGLE_EMAIL);
              requireNoFactor(twoFactorAccountEntity);
              // Acht ASCII-Ziffern. Kein E-Mail-Nachweis für ein früher schon geschütztes Konto.
              boolean valid =
                  code != null
                      && code.matches("\\d{8}")
                      && !twoFactorAccountEntity.isPreviouslyEnrolled()
                      && !twoFactorAccountEntity.isOperatorEnrollment()
                      && !twoFactorAccountEntity.getUser().isVerificationPending()
                      && twoFactorAccountEntity
                          .getUser()
                          .getEmail()
                          .equals(twoFactorChallengeEntity.getContactAddress())
                      && Objects.equals(
                          twoFactorAccountEntity.getRevision(),
                          twoFactorChallengeEntity.getFactorRevision())
                      && MessageDigest.isEqual(
                          twoFactorChallengeEntity
                              .getVerificationDigest()
                              .getBytes(StandardCharsets.US_ASCII),
                          emailDigest(twoFactorChallengeEntity, code)
                              .getBytes(StandardCharsets.US_ASCII));
              if (!twoFactorStore.checkAdditionalProof(twoFactorAccountEntity, valid)) {
                twoFactorChallengeEntity.setAttempts(twoFactorChallengeEntity.getAttempts() + 1);
                if (twoFactorChallengeEntity.getAttempts() >= 5) {
                  twoFactorChallengeEntity.setConsumed(true);
                }
                twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
                return null;
              }
              twoFactorChallengeEntity.setKind(Kind.ENROLLMENT);
              twoFactorChallengeEntity.setSetupProof(SetupProof.GOOGLE_EMAIL);
              twoFactorChallengeEntity.setVerificationDigest(null);
              prepareSecret(twoFactorChallengeEntity);
              twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
              return new Pending(
                  twoFactorChallengeEntity.getId(),
                  twoFactorChallengeEntity.getKind(),
                  twoFactorChallengeEntity.getSetupProof());
            });
    if (result == null) {
      throw twoFactorStore.invalidConfirmation(owner.id());
    }
    return result;
  }

  public UUID complete(HttpServletRequest httpRequest, UUID id, String firstCode) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    CompletedEnrollment completed =
        twoFactorStore.locked(
            owner.id(),
            owner.accessVersion(),
            twoFactorAccountEntity -> {
              TwoFactorChallengeEntity twoFactorChallengeEntity =
                  locked(httpRequest, owner, id, Kind.ENROLLMENT);
              if (!Objects.equals(
                      twoFactorAccountEntity.getRevision(),
                      twoFactorChallengeEntity.getFactorRevision())
                  || twoFactorChallengeEntity.getSetupProof() == null) {
                throw new AccessDeniedException("Enrollment changed");
              }
              if (twoFactorChallengeEntity.getSetupProof() == SetupProof.RECOVERY_CODE
                  && !twoFactorStore.hasUnusedRecoveryCode(
                      twoFactorAccountEntity, twoFactorChallengeEntity.getVerificationDigest())) {
                throw new AccessDeniedException("Recovery code was already used or changed");
              }
              byte[] secret =
                  twoFactorSecrets.decrypt(
                      owner.id(),
                      ENROLLMENT_PREFIX + id,
                      twoFactorChallengeEntity.getEncryptedPayload());
              TwoFactorStore.Enrollment result;
              try {
                if (twoFactorAccountEntity.enrolled()
                    && twoFactorChallengeEntity.getSetupProof() != SetupProof.FACTOR
                    && twoFactorChallengeEntity.getSetupProof() != SetupProof.RECOVERY_CODE) {
                  throw new AccessDeniedException("Existing factor requires its own proof");
                }
                result =
                    twoFactorStore.enrollLocked(
                        twoFactorAccountEntity,
                        twoFactorChallengeEntity.getFactorRevision(),
                        secret,
                        firstCode,
                        twoFactorChallengeEntity.getSetupProof() != SetupProof.GOOGLE_EMAIL);
              } finally {
                Arrays.fill(secret, (byte) 0);
              }
              if (result == null) {
                return null;
              }
              twoFactorChallengeEntity.setConsumed(true);
              twoFactorChallengeEntity.setEncryptedPayload(null);
              twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
              TwoFactorChallengeEntity recoveryOutputChallengeEntity =
                  recoveryOutput(httpRequest, twoFactorAccountEntity, result.recoveryCodes());
              recoveryOutputChallengeEntity.setSetupProof(twoFactorChallengeEntity.getSetupProof());
              twoFactorMail.securityChange(
                  twoFactorAccountEntity.getUser(), TwoFactorMail.Event.ENABLED);
              AfterCommitLog.info(log, "Authenticator enrolled: accountId={}", owner.id());
              return new CompletedEnrollment(
                  recoveryOutputChallengeEntity.getId(), result.revision());
            });
    if (completed == null) {
      throw twoFactorStore.invalidConfirmation(owner.id());
    }
    // Ein konkurrierender Faktorwechsel nach dem Commit darf nicht nachträglich als von
    // dieser Einrichtung bestätigt gelten.
    twoFactorAuthorization.markSession(httpRequest, owner, completed.factorRevision());
    sessionManagementServiceProvider.getObject().revokeOthers(httpRequest);
    return completed.outputId();
  }

  /** Ausschließlich für einen sitzungsgebundenen, nicht gecachten internen Dokument-Endpunkt. */
  public byte[] pendingSecret(HttpServletRequest httpRequest, UUID id) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    return transactionTemplate.execute(status -> {
      TwoFactorChallengeEntity twoFactorChallengeEntity =
          locked(httpRequest, owner, id, Kind.ENROLLMENT);
      return twoFactorSecrets.decrypt(
          owner.id(), ENROLLMENT_PREFIX + id, twoFactorChallengeEntity.getEncryptedPayload());
    });
  }

  public List<String> recoveryOutput(HttpServletRequest httpRequest, UUID id) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    twoFactorAuthorization.requireSession(httpRequest);
    return transactionTemplate.execute(status -> {
      TwoFactorChallengeEntity twoFactorChallengeEntity =
          locked(httpRequest, owner, id, Kind.RECOVERY_OUTPUT);
      byte[] plaintext =
          twoFactorSecrets.decrypt(
              owner.id(), "recovery:" + id, twoFactorChallengeEntity.getEncryptedPayload());
      try {
        return List.of(new String(plaintext, StandardCharsets.UTF_8).split("\n"));
      } finally {
        Arrays.fill(plaintext, (byte) 0);
      }
    });
  }

  TwoFactorChallengeEntity recoveryOutput(
      HttpServletRequest httpRequest,
      TwoFactorAccountEntity twoFactorAccountEntity,
      List<String> values) {
    TwoFactorChallengeEntity recoveryOutputChallengeEntity =
        createTwoFactorChallengeEntity(httpRequest, twoFactorAccountEntity, Kind.RECOVERY_OUTPUT);
    recoveryOutputChallengeEntity.setEncryptedPayload(
        twoFactorSecrets.encrypt(
            twoFactorAccountEntity.getUserId(),
            "recovery:" + recoveryOutputChallengeEntity.getId(),
            String.join("\n", values).getBytes(StandardCharsets.UTF_8)));
    return twoFactorChallengeRepository.saveAndFlush(recoveryOutputChallengeEntity);
  }

  /**
   * Bestätigt nur die Sicherung bereits angezeigter Codes und verwirft die Ausgabe. Wiederholte
   * Bestätigung darf auch nach einer Bereinigung gelingen, solange die Sitzung berechtigt ist.
   */
  public void dismissOutput(HttpServletRequest httpRequest, UUID id) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    twoFactorAuthorization.requireSession(httpRequest);
    transactionTemplate.executeWithoutResult(status -> {
      TwoFactorChallengeEntity twoFactorChallengeEntity =
          twoFactorChallengeRepository.lock(id).orElse(null);
      if (twoFactorChallengeEntity == null) {
        return;
      }
      if (twoFactorChallengeEntity.getKind() != Kind.RECOVERY_OUTPUT
          || !twoFactorChallengeEntity.getUser().getId().equals(owner.id())
          || !twoFactorChallengeEntity.getAccessVersion().equals(owner.accessVersion())
          || !twoFactorChallengeEntity
              .getSessionDigest()
              .equals(TwoFactorAuthorization.sessionDigest(httpRequest.getSession(false)))) {
        throw new AccessDeniedException("Recovery output belongs to another operation");
      }
      twoFactorChallengeEntity.setEncryptedPayload(null);
      twoFactorChallengeEntity.setConsumed(true);
      twoFactorChallengeRepository.saveAndFlush(twoFactorChallengeEntity);
    });
  }

  public Pending pending(HttpServletRequest httpRequest) {
    TwoFactorAuthorization.Owner owner = twoFactorAuthorization.owner(httpRequest);
    return transactionTemplate.execute(status -> {
      for (Kind kind : List.of(Kind.RECOVERY_OUTPUT, Kind.GOOGLE_EMAIL, Kind.ENROLLMENT)) {
        List<UUID> ids =
            twoFactorChallengeRepository.active(
                owner.id(),
                TwoFactorAuthorization.sessionDigest(httpRequest.getSession(false)),
                kind,
                TwoFactorAction.MANAGE_FACTOR,
                twoFactorClock.instant());
        if (!ids.isEmpty()) {
          try {
            TwoFactorChallengeEntity twoFactorChallengeEntity =
                locked(httpRequest, owner, ids.getFirst(), kind);
            return new Pending(
                twoFactorChallengeEntity.getId(), kind, twoFactorChallengeEntity.getSetupProof());
          } catch (AccessDeniedException _) {
            /* Ein veralteter Ablauf wird nicht wiederaufgenommen. */
          }
        }
      }
      return null;
    });
  }

  /** Ein Abbruch verwirft nur vorbereitete Schritte, niemals bereits ausgegebene Codes. */
  public boolean cancelPreparation(HttpServletRequest httpRequest) {
    return Boolean.TRUE.equals(
        transactionTemplate.execute(status -> {
          Pending current = pending(httpRequest);
          if (current == null) {
            return true;
          }
          if (current.kind() == Kind.RECOVERY_OUTPUT) {
            return false;
          }
          TwoFactorChallengeEntity draft =
              locked(
                  httpRequest,
                  twoFactorAuthorization.owner(httpRequest),
                  current.id(),
                  current.kind());
          draft.setConsumed(true);
          draft.setEncryptedPayload(null);
          draft.setVerificationDigest(null);
          twoFactorChallengeRepository.saveAndFlush(draft);
          return true;
        }));
  }

  private TwoFactorChallengeEntity createTwoFactorChallengeEntity(
      HttpServletRequest httpRequest, TwoFactorAccountEntity twoFactorAccountEntity, Kind kind) {
    String session = TwoFactorAuthorization.sessionDigest(httpRequest.getSession(false));
    twoFactorChallengeRepository.invalidate(
        twoFactorAccountEntity.getUserId(), session, kind, TwoFactorAction.MANAGE_FACTOR);
    TwoFactorChallengeEntity twoFactorChallengeEntity = new TwoFactorChallengeEntity();
    twoFactorChallengeEntity.setUser(twoFactorAccountEntity.getUser());
    twoFactorChallengeEntity.setAccessVersion(twoFactorAccountEntity.getUser().getAccessVersion());
    twoFactorChallengeEntity.setSessionDigest(session);
    twoFactorChallengeEntity.setFactorRevision(twoFactorAccountEntity.getRevision());
    twoFactorChallengeEntity.setPolicyRevision(twoFactorPolicies.current().revision());
    twoFactorChallengeEntity.setAction(TwoFactorAction.MANAGE_FACTOR);
    twoFactorChallengeEntity.setKind(kind);
    twoFactorChallengeEntity.setCreatedAt(twoFactorClock.instant());
    twoFactorChallengeEntity.setExpiresAt(
        kind == Kind.RECOVERY_OUTPUT
            ? SESSION_BOUND_OUTPUT
            : twoFactorClock.instant().plusSeconds(300));
    twoFactorChallengeEntity.setOperatorEnrollment(twoFactorAccountEntity.isOperatorEnrollment());
    return twoFactorChallengeEntity;
  }

  private TwoFactorChallengeEntity locked(
      HttpServletRequest httpRequest, TwoFactorAuthorization.Owner owner, UUID id, Kind kind) {
    TwoFactorChallengeEntity twoFactorChallengeEntity =
        twoFactorChallengeRepository
            .lock(id)
            .orElseThrow(() -> new AccessDeniedException("Two-factor challenge unavailable"));
    if (twoFactorChallengeEntity.isConsumed()
        || twoFactorChallengeEntity.getKind() != kind
        || !twoFactorChallengeEntity.getUser().getId().equals(owner.id())
        || !twoFactorChallengeEntity.getAccessVersion().equals(owner.accessVersion())
        || !twoFactorChallengeEntity
            .getSessionDigest()
            .equals(TwoFactorAuthorization.sessionDigest(httpRequest.getSession(false)))
        || kind != Kind.RECOVERY_OUTPUT
            && !twoFactorChallengeEntity.getExpiresAt().isAfter(twoFactorClock.instant())
        || twoFactorChallengeEntity.getPolicyRevision() != twoFactorPolicies.current().revision()
        || !Objects.equals(
            twoFactorChallengeEntity.getFactorRevision(),
            twoFactorAuthorization.state(owner).revision())) {
      throw new AccessDeniedException("Two-factor challenge expired or changed");
    }
    return twoFactorChallengeEntity;
  }

  private void requireNoFactor(TwoFactorAccountEntity twoFactorAccountEntity) {
    if (twoFactorAccountEntity.enrolled()) {
      throw new AccessDeniedException("Existing factor requires its own recovery proof");
    }
  }

  private void prepareSecret(TwoFactorChallengeEntity twoFactorChallengeEntity) {
    byte[] secret = Totp.newSecret();
    try {
      twoFactorChallengeEntity.setEncryptedPayload(
          twoFactorSecrets.encrypt(
              twoFactorChallengeEntity.getUser().getId(),
              ENROLLMENT_PREFIX + twoFactorChallengeEntity.getId(),
              secret));
    } finally {
      Arrays.fill(secret, (byte) 0);
    }
  }

  private String emailDigest(TwoFactorChallengeEntity twoFactorChallengeEntity, String code) {
    return twoFactorSecrets.emailVerifier(
        twoFactorChallengeEntity.getUser().getId(),
        twoFactorChallengeEntity.getId(),
        twoFactorChallengeEntity.getContactAddress(),
        code);
  }
}
