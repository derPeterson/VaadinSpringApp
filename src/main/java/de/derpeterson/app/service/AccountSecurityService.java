package de.derpeterson.app.service;

import de.derpeterson.app.config.TokenDurationPolicy;
import de.derpeterson.app.model.EmailChangeTokenEntity;
import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.EmailChangeTokenRepository;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.validation.EmailConflict;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

/** Frische Anmeldung, Kontolock, Änderung und Mail-Outbox bilden getrennte klare Grenzen. */
@Service
@RequiredArgsConstructor
public class AccountSecurityService {
  public record PasswordChange(String currentPassword, String newPassword, String confirmation)
      implements Serializable {
    @Override
    public String toString() {
      return "PasswordChange[redacted]";
    }
  }

  public record EmailChange(String newEmail, String confirmation, String currentPassword)
      implements Serializable {
    @Override
    public String toString() {
      return "EmailChange[redacted]";
    }
  }

  public record PasswordChanged(Long previousVersion, Long version) implements Serializable {}

  public enum Confirmation {
    READY,
    SUCCESS,
    INVALID,
    EXPIRED,
    CONFLICT
  }

  public enum Problem {
    EMAIL_UNAVAILABLE,
    SAME_EMAIL,
    SAME_PASSWORD,
    REQUEST_PENDING
  }

  public static class Rejected extends RuntimeException {
    private final Problem problem;

    public Rejected(Problem problem) {
      super(problem.name());
      this.problem = problem;
    }

    public Problem problem() {
      return problem;
    }
  }

  private final SecurityService security;
  private final CustomUserDetailsService details;
  private final LoginAttemptService attempts;
  private final UserRepository users;
  private final EmailChangeTokenRepository tokens;
  private final PasswordResetTokenRepository resets;
  private final PasswordEncoder encoder;
  private final ConfigService config;
  private final EmailQueueService queue;
  private final AccountSecurityMail mail;
  private final PersistentTokenRepository remembered;
  private final SessionRegistry sessions;
  private final PlatformTransactionManager manager;
  private final Clock clock = Clock.systemDefaultZone();
  private final SecureRandom random = new SecureRandom();

  public PasswordChanged changePassword(HttpServletRequest request, PasswordChange change) {
    Assert.notNull(change, "Password change required");
    Assert.isTrue(UserInputRules.isPasswordSecure(change.newPassword()), "Invalid password");
    Assert.isTrue(Objects.equals(change.newPassword(), change.confirmation()), "Password mismatch");
    var identity = reauthenticate(request, change.currentPassword());
    return transaction(
        () -> {
          var user = lockedAccount(identity, change.currentPassword());
          if (encoder.matches(change.newPassword(), user.getPassword()))
            throw new Rejected(Problem.SAME_PASSWORD);
          var body = mail.render("password_changed", user, Map.of());
          var previous = user.getVersion();
          user.setPassword(encoder.encode(change.newPassword()));
          users.saveAndFlush(user);
          invalidateEmailChanges(user);
          invalidateResets(user);
          queue.addAddressedNotification(
              user, user.getEmail(), mail.texts(user).getAccountPasswordChangedSubject(), body);
          remembered.removeUserTokens(identity);
          var session = request.getSession(false);
          revokeAfterCommit(identity, session == null ? null : session.getId());
          return new PasswordChanged(previous, user.getVersion());
        });
  }

  public void requestEmailChange(HttpServletRequest request, EmailChange change) {
    Assert.notNull(change, "Email change required");
    var target = EmailIdentity.canonicalize(change.newEmail());
    Assert.isTrue(UserInputRules.isEmailValid(target), "Invalid email");
    Assert.isTrue(
        Objects.equals(target, EmailIdentity.canonicalize(change.confirmation())),
        "Email mismatch");
    var identity = reauthenticate(request, change.currentPassword());
    transaction(
        () -> {
          var user = lockedAccount(identity, change.currentPassword());
          if (user.getEmail().equals(target)) throw new Rejected(Problem.SAME_EMAIL);
          if (users.emailExistsForOtherUser(target, user.getId()))
            throw new Rejected(Problem.EMAIL_UNAVAILABLE);
          var now = LocalDateTime.now(clock);
          var active = tokens.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE);
          if (active.stream().anyMatch(t -> t.getCreatedAt().isAfter(now.minusMinutes(1)))) {
            throw new Rejected(Problem.REQUEST_PENDING);
          }
          var duration = duration(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION);
          var raw = new byte[32];
          random.nextBytes(raw);
          var token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
          var link =
              UriComponentsBuilder.fromUriString(config.getString(ConfigEntry.BASE_URL))
                  .pathSegment(AppRoute.EMAIL_CHANGE.getRoute(), token)
                  .toUriString();
          var body =
              mail.render(
                  "email_change_request",
                  user,
                  Map.of("CONFIRM_EMAIL_LINK", link, "NEW_EMAIL", target));
          active.forEach(t -> t.setStatus(TokenStatus.INACTIVE));
          tokens.saveAll(active);
          var pending = new EmailChangeTokenEntity();
          pending.setDigest(digest(token));
          pending.setUserEntity(user);
          pending.setOldEmail(user.getEmail());
          pending.setNewEmail(target);
          pending.setCredentialDigest(digest(user.getPassword()));
          pending.setCreatedAt(now);
          pending.setExpiryDate(now.plus(duration));
          tokens.save(pending);
          queue.addAddressedNotification(
              user, target, mail.texts(user).getAccountEmailChangeSubject(), body);
          return null;
        });
  }

  public Confirmation inspect(String token) {
    MaintenanceService.requirePublicAccess(config);
    var hash = tokenDigest(token);
    if (hash == null) return Confirmation.INVALID;
    return transaction(
        () -> tokens.findByDigest(hash).map(this::state).orElse(Confirmation.INVALID));
  }

  /** Erst der ausdrückliche Buttonklick verbraucht den Link, kein Mailscanner-GET. */
  public Confirmation confirm(String token) {
    MaintenanceService.requirePublicAccess(config);
    var hash = tokenDigest(token);
    if (hash == null) return Confirmation.INVALID;
    try {
      return transaction(
          () -> {
            var user =
                tokens.findUserIdByDigest(hash).flatMap(users::lockVerificationUser).orElse(null);
            if (user == null) return Confirmation.INVALID;
            var pending = tokens.findByDigest(hash).orElseThrow();
            var state = state(pending);
            if (state != Confirmation.READY) {
              if (state == Confirmation.EXPIRED) pending.setStatus(TokenStatus.EXPIRED);
              return state;
            }
            if (users.emailExistsForOtherUser(pending.getNewEmail(), user.getId())) {
              pending.setStatus(TokenStatus.INACTIVE);
              return Confirmation.CONFLICT;
            }
            var old = user.getEmail();
            var target = pending.getNewEmail();
            var body =
                mail.render("email_changed", user, Map.of("OLD_EMAIL", old, "NEW_EMAIL", target));
            user.setEmail(target);
            users.saveAndFlush(user);
            pending.setStatus(TokenStatus.USED);
            tokens.save(pending);
            invalidateEmailChanges(user);
            invalidateResets(user);
            var subject = mail.texts(user).getAccountEmailChangedSubject();
            queue.addAddressedNotification(user, old, subject, body);
            queue.addAddressedNotification(user, target, subject, body);
            remembered.removeUserTokens(old);
            revokeAfterCommit(old, null);
            return Confirmation.SUCCESS;
          });
    } catch (RuntimeException failure) {
      if (EmailConflict.isDuplicate(failure)) return Confirmation.CONFLICT;
      throw failure;
    }
  }

  private Confirmation state(EmailChangeTokenEntity token) {
    if (token.getStatus() != TokenStatus.ACTIVE) return Confirmation.INVALID;
    if (!token.getExpiryDate().isAfter(LocalDateTime.now(clock))) return Confirmation.EXPIRED;
    var user = token.getUserEntity();
    if (!user.isEnabled()
        || !Objects.equals(user.getEmail(), token.getOldEmail())
        || !Objects.equals(digest(user.getPassword()), token.getCredentialDigest()))
      return Confirmation.INVALID;
    return Confirmation.READY;
  }

  private String reauthenticate(HttpServletRequest request, String password) {
    var principal =
        security
            .getAuthenticatedUser(request)
            .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    MaintenanceService.requirePublicAccess(config);
    Assert.hasText(password, "Current password required");
    var auth =
        attempts.authenticate(
            principal.getUsername(),
            () -> {
              var current = details.loadPasswordUserByUsername(principal.getUsername());
              if (!current.isEnabled()) throw new DisabledException("Account disabled");
              if (!encoder.matches(password, current.getPassword()))
                throw new BadCredentialsException("Current password invalid");
              return UsernamePasswordAuthenticationToken.authenticated(
                  current, null, current.getAuthorities());
            });
    return EmailIdentity.canonicalize(auth.getName());
  }

  private UserEntity lockedAccount(String identity, String password) {
    MaintenanceService.requirePublicAccess(config);
    var user =
        users
            .findByEmailForUpdate(identity)
            .orElseThrow(() -> new AccessDeniedException("Account unavailable"));
    if (!user.isEnabled()) throw new AccessDeniedException("Account disabled");
    if (!encoder.matches(password, user.getPassword()))
      throw new BadCredentialsException("Credentials changed");
    return user;
  }

  private void invalidateEmailChanges(UserEntity user) {
    var active = tokens.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE);
    active.forEach(t -> t.setStatus(TokenStatus.INACTIVE));
    tokens.saveAll(active);
  }

  private void invalidateResets(UserEntity user) {
    var active = resets.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE);
    active.forEach(t -> t.setStatus(TokenStatus.INACTIVE));
    resets.saveAll(active);
  }

  private void revokeAfterCommit(String identity, String keep) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            sessions.getAllSessions(identity, true).stream()
                .filter(session -> !Objects.equals(session.getSessionId(), keep))
                .forEach(session -> session.expireNow());
          }
        });
  }

  private <T> T transaction(Supplier<T> action) {
    var tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tx.execute(status -> action.get());
  }

  private Duration duration(ConfigEntry entry) {
    var value = Duration.parse(config.getString(entry));
    Assert.isTrue(
        TokenDurationPolicy.isAtLeast(value, entry.getMinimumTokenDuration()),
        "Invalid token duration");
    return value;
  }

  @Scheduled(cron = "0 0 3 1/3 * ?")
  public void deleteExpiredTokens() {
    var cutoff =
        LocalDateTime.now(clock).minus(duration(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION));
    transaction(() -> tokens.deleteByExpiryDateBefore(cutoff));
  }

  private static String tokenDigest(String raw) {
    if (raw == null || raw.length() != 43) return null;
    try {
      var decoded = Base64.getUrlDecoder().decode(raw);
      if (decoded.length != 32
          || !Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(raw))
        return null;
      return digest(raw);
    } catch (IllegalArgumentException invalid) {
      return null;
    }
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
