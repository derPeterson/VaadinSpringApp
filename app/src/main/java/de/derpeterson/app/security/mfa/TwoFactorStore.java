package de.derpeterson.app.security.mfa;

import de.derpeterson.app.model.TwoFactorAccountEntity;
import de.derpeterson.app.model.TwoFactorRecoveryCodeEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.repository.TwoFactorAccountRepository;
import de.derpeterson.app.repository.TwoFactorRecoveryCodeRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Persistenzkern. Aufruf nur nach Prüfung von Sitzung, Zweck und Berechtigung im MFA-Dienst. */
@Component
public class TwoFactorStore {
  private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);
  private static final int ATTEMPT_LIMIT = 5;
  private final UserRepository users;
  private final TwoFactorAccountRepository accounts;
  private final TwoFactorRecoveryCodeRepository recoveryCodes;
  private final EntityManager entityManager;
  private final TwoFactorSecrets secrets;
  private final TransactionTemplate independent;
  private final Clock clock;

  record Verified(UUID revision, boolean recoveryCode) {}

  record Enrollment(UUID revision, List<String> recoveryCodes) {
    @Override
    public String toString() {
      return "Two-factor enrollment";
    }
  }

  @Autowired
  public TwoFactorStore(
      UserRepository users,
      TwoFactorAccountRepository accounts,
      TwoFactorRecoveryCodeRepository recoveryCodes,
      EntityManager entityManager,
      TwoFactorSecrets secrets,
      PlatformTransactionManager transactions) {
    this(users, accounts, recoveryCodes, entityManager, secrets, transactions, Clock.systemUTC());
  }

  TwoFactorStore(
      UserRepository users,
      TwoFactorAccountRepository accounts,
      TwoFactorRecoveryCodeRepository recoveryCodes,
      EntityManager entityManager,
      TwoFactorSecrets secrets,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.users = users;
    this.accounts = accounts;
    this.recoveryCodes = recoveryCodes;
    this.entityManager = entityManager;
    this.secrets = secrets;
    this.clock = Objects.requireNonNull(clock);
    independent = new TransactionTemplate(transactions);
    independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  Verified verify(Long userId, String accessVersion, String input) {
    Verification result = locked(userId, accessVersion, account -> verifyLocked(account, input));
    // Fehlversuche und Sperren müssen vor der Exception committed sein, sonst wären sie umgehbar.
    if (result.blocked()) {
      throw new LockedException("Two-factor verification temporarily blocked");
    }
    if (result.verified() == null) {
      throw new BadCredentialsException("Invalid two-factor verification");
    }
    return result.verified();
  }

  private record Verification(Verified verified, boolean blocked) {}

  private Verification verifyLocked(TwoFactorAccountEntity account, String input) {
    Instant now = clock.instant();
    if (blocked(account, now)) {
      return new Verification(null, true);
    }
    if (!account.enrolled() || account.isOperatorEnrollment()) {
      return failed(account, now);
    }
    byte[] secret = secrets.decrypt(account.getUserId(), "factor", account.getEncryptedSecret());
    long step;
    try {
      step = Totp.verify(secret, input, now, account.getAcceptedStep());
    } finally {
      Arrays.fill(secret, (byte) 0);
    }
    if (step >= 0) {
      account.setAcceptedStep(step);
      resetAttempts(account);
      return new Verification(new Verified(account.getRevision(), false), false);
    }
    String digest = TwoFactorSecrets.recoveryDigest(account.getUserId(), input);
    TwoFactorRecoveryCodeEntity recovery =
        digest == null ? null : recoveryCodes.unused(account.getUserId(), digest).orElse(null);
    if (recovery != null) {
      recovery.setUsedAt(now);
      resetAttempts(account);
      return new Verification(new Verified(account.getRevision(), true), false);
    }
    return failed(account, now);
  }

  boolean blocked(TwoFactorAccountEntity account, Instant now) {
    if (account.getBlockedUntil() != null && now.isBefore(account.getBlockedUntil())) {
      return true;
    }
    if (account.getAttemptWindow() == null
        || !now.isBefore(account.getAttemptWindow().plus(ATTEMPT_WINDOW))) {
      resetAttempts(account);
      account.setAttemptWindow(now);
    }
    return false;
  }

  private Verification failed(TwoFactorAccountEntity account, Instant now) {
    account.setFailedAttempts(account.getFailedAttempts() + 1);
    if (account.getFailedAttempts() >= ATTEMPT_LIMIT) {
      account.setBlockedUntil(now.plus(ATTEMPT_WINDOW));
    }
    return new Verification(null, account.getFailedAttempts() >= ATTEMPT_LIMIT);
  }

  boolean checkAdditionalProof(TwoFactorAccountEntity account, boolean valid) {
    Instant now = clock.instant();
    if (blocked(account, now)) {
      return false;
    }
    if (!valid) {
      failed(account, now);
    }
    return valid;
  }

  /** Ausschließlich unter demselben Kontolock wie Einrichtung bzw. Abschluss aufrufen. */
  boolean hasUnusedRecoveryCode(TwoFactorAccountEntity account, String digest) {
    return account.enrolled()
        && !account.isOperatorEnrollment()
        && digest != null
        && recoveryCodes.unused(account.getUserId(), digest).isPresent();
  }

  private void resetAttempts(TwoFactorAccountEntity account) {
    account.setFailedAttempts(0);
    account.setAttemptWindow(null);
    account.setBlockedUntil(null);
  }

  AuthenticationException invalidConfirmation(Long userId) {
    // Fehlversuch und Sperre wurden bereits unabhängig committed. Frischen Skalarstand lesen.
    return accounts.blockedUntil(userId).filter(until -> until.isAfter(clock.instant())).isPresent()
        ? new LockedException("Two-factor confirmation is temporarily locked")
        : new BadCredentialsException("Invalid two-factor confirmation");
  }

  Enrollment enroll(
      Long userId,
      String accessVersion,
      UUID expectedRevision,
      byte[] secret,
      String firstCode,
      boolean operatorEnrollment) {
    Enrollment result =
        locked(
            userId,
            accessVersion,
            account ->
                enrollLocked(
                    account,
                    expectedRevision,
                    secret,
                    firstCode,
                    operatorEnrollment && account.isOperatorEnrollment()));
    if (result == null) {
      throw new BadCredentialsException("Invalid two-factor enrollment code");
    }
    return result;
  }

  Enrollment enrollLocked(
      TwoFactorAccountEntity account,
      UUID expectedRevision,
      byte[] secret,
      String firstCode,
      boolean protectedReenrollment) {
    Instant now = clock.instant();
    if (!Objects.equals(account.getRevision(), expectedRevision)
        || account.enrolled() && !protectedReenrollment
        || account.isPreviouslyEnrolled() && !protectedReenrollment) {
      throw new BadCredentialsException("Two-factor enrollment no longer permitted");
    }
    if (blocked(account, now)) {
      return null;
    }
    long step = Totp.verify(secret, firstCode, now, -1);
    if (step < 0) {
      failed(account, now);
      return null;
    }
    account.setEncryptedSecret(secrets.encrypt(account.getUserId(), "factor", secret));
    account.setPreviouslyEnrolled(true);
    account.setOperatorEnrollment(false);
    account.setEnrolledAt(now);
    account.setAcceptedStep(step);
    account.setRevision(UUID.randomUUID());
    resetAttempts(account);
    return new Enrollment(account.getRevision(), replaceRecoveryCodes(account));
  }

  List<String> renewRecoveryCodes(Long userId, String accessVersion, UUID expectedRevision) {
    return locked(
        userId,
        accessVersion,
        account -> {
          requireRevision(account, expectedRevision);
          account.setRevision(UUID.randomUUID());
          return replaceRecoveryCodes(account);
        });
  }

  void disable(Long userId, String accessVersion, UUID expectedRevision) {
    locked(
        userId,
        accessVersion,
        account -> {
          requireRevision(account, expectedRevision);
          account.setEncryptedSecret(null);
          account.setRevision(UUID.randomUUID());
          account.setAcceptedStep(-1);
          recoveryCodes.deleteForUser(userId);
          // Die Historie bleibt erhalten: Löschen eröffnet niemals den schwachen
          // Ersteinrichtungsweg.
          return null;
        });
  }

  UUID initialize(Long userId, String accessVersion) {
    return locked(userId, accessVersion, TwoFactorAccountEntity::getRevision);
  }

  private void requireRevision(TwoFactorAccountEntity account, UUID revision) {
    if (!account.enrolled() || !Objects.equals(account.getRevision(), revision)) {
      throw new BadCredentialsException("Two-factor account changed");
    }
  }

  List<String> replaceRecoveryCodes(TwoFactorAccountEntity account) {
    recoveryCodes.deleteForUser(account.getUserId());
    List<String> codes = secrets.newRecoveryCodes();
    for (String code : codes) {
      TwoFactorRecoveryCodeEntity row = new TwoFactorRecoveryCodeEntity();
      row.setUser(account.getUser());
      row.setDigest(TwoFactorSecrets.recoveryDigest(account.getUserId(), code));
      recoveryCodes.save(row);
    }
    return codes;
  }

  <T> T locked(Long userId, String accessVersion, Function<TwoFactorAccountEntity, T> action) {
    return independent.execute(
        status -> {
          UserEntity user =
              users
                  .findByIdForUpdate(userId)
                  .orElseThrow(() -> new BadCredentialsException("Account unavailable"));
          entityManager.refresh(user);
          if (!user.isEnabled() || !Objects.equals(user.getAccessVersion(), accessVersion)) {
            throw new BadCredentialsException("Account access changed");
          }
          TwoFactorAccountEntity account = accounts.findById(userId).orElse(null);
          if (account == null) {
            account = new TwoFactorAccountEntity();
            account.setUser(user);
            account = accounts.saveAndFlush(account);
          } else {
            entityManager.refresh(account);
          }
          T result = action.apply(account);
          accounts.saveAndFlush(account);
          return result;
        });
  }
}
