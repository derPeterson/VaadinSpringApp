package de.derpeterson.app.service;

import de.derpeterson.app.model.LoginAttemptEntity;
import de.derpeterson.app.model.LoginAttemptGuardEntity;
import de.derpeterson.app.model.EmailIdentity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.LoginAttemptRepository;
import de.derpeterson.app.repository.LoginAttemptGuardRepository;
import de.derpeterson.app.security.LoginThrottledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class LoginAttemptService {
    public static final Duration WINDOW = Duration.ofMinutes(15);
    public static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    public static final Duration RETENTION = Duration.ofHours(24);
    public static final int MAX_IDENTITIES = 10_000;
    private final LoginAttemptRepository repository;
    private final LoginAttemptGuardRepository guard;
    private final ConfigService config;
    private final TransactionTemplate independent;
    private final Clock clock;
    private final int capacity;

    @Autowired
    public LoginAttemptService(LoginAttemptRepository repository, LoginAttemptGuardRepository guard,
            ConfigService config, PlatformTransactionManager manager) {
        this(repository, guard, config, manager, Clock.systemUTC(), MAX_IDENTITIES);
    }

    public LoginAttemptService(LoginAttemptRepository repository, LoginAttemptGuardRepository guard,
            ConfigService config, PlatformTransactionManager manager, Clock clock, int capacity) {
        this.repository = repository;
        this.guard = guard;
        this.config = config;
        this.clock = Objects.requireNonNull(clock);
        if (capacity < 1) throw new IllegalArgumentException("Login identity capacity must be positive");
        this.capacity = capacity;
        independent = new TransactionTemplate(manager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Authentication authenticate(String identity, Supplier<Authentication> checkPassword) {
        Objects.requireNonNull(checkPassword);
        String key = identityKey(identity);
        Outcome outcome;
        try {
            positiveLimit();
            outcome = null;
            for (int retry = 0; retry < 2 && outcome == null; retry++) {
                ensureIdentity(key);
                outcome = independent.execute(status -> repository.lockIdentity(key)
                        .map(row -> checkLocked(row, checkPassword)).orElse(null));
                // Cleanup can delete a previously idle row between lookup and lock; retry with a fresh row.
            }
            if (outcome == null) throw new AuthenticationServiceException("Sign-in state unavailable");
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthenticationServiceException("Sign-in temporarily unavailable", exception);
        }
        // Credential failures must be thrown only AFTER the independent counter commit.
        if (outcome.failure() != null) throw outcome.failure();
        return outcome.authentication();
    }

    private Outcome checkLocked(LoginAttemptEntity row, Supplier<Authentication> checkPassword) {
        Instant now = clock.instant();
        int limit = positiveLimit();
        if (row.getBlockedUntil() != null && now.isBefore(row.getBlockedUntil())) {
            return new Outcome(null, new LoginThrottledException());
        }
        if (row.getBlockedUntil() != null || !now.isBefore(row.getWindowStartedAt().plus(WINDOW))) {
            reset(row, now);
        }
        if (row.getFailures() >= limit) {
            row.setBlockedUntil(now.plus(BLOCK_DURATION));
            row.setLastActivity(now);
            repository.save(row);
            return new Outcome(null, new LoginThrottledException());
        }
        try {
            Authentication authenticated = checkPassword.get();
            if (authenticated == null || !authenticated.isAuthenticated()) {
                throw new AuthenticationServiceException("Password provider returned no authenticated principal");
            }
            reset(row, clock.instant());
            repository.save(row);
            return new Outcome(authenticated, null);
        } catch (BadCredentialsException | UsernameNotFoundException | AccountStatusException failure) {
            Instant completed = clock.instant();
            row.setFailures(row.getFailures() + 1);
            row.setLastActivity(completed);
            if (row.getFailures() >= limit) row.setBlockedUntil(completed.plus(BLOCK_DURATION));
            repository.save(row);
            return new Outcome(null, failure);
        }
    }

    private int positiveLimit() {
        int value = config.getInteger(ConfigEntry.LOGIN_ATTEMPTS_LIMIT);
        if (value < 1) throw new AuthenticationServiceException("Invalid login attempt configuration");
        return value;
    }

    private void ensureIdentity(String key) {
        if (repository.existsById(key)) return;
        ensureGuard();
        independent.executeWithoutResult(status -> {
            guard.lockCreation().orElseThrow(() -> new IllegalStateException("Missing login creation guard"));
            if (repository.existsById(key)) return;
            if (repository.count() >= capacity) {
                throw new AuthenticationServiceException("Sign-in temporarily unavailable");
            }
            var row = new LoginAttemptEntity();
            row.setIdentityHash(key);
            reset(row, clock.instant());
            repository.saveAndFlush(row);
        });
    }

    private void ensureGuard() {
        if (guard.existsById(1)) return;
        try {
            independent.executeWithoutResult(status -> guard.saveAndFlush(new LoginAttemptGuardEntity(1)));
        } catch (DataIntegrityViolationException competingCreation) {
            if (!guard.existsById(1)) throw competingCreation;
        }
    }

    /** Joins token/password changes so rollback also restores the previous throttle state. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void clearAfterPasswordReset(String identity) {
        repository.lockIdentity(identityKey(identity)).ifPresent(row -> {
            reset(row, clock.instant());
            repository.save(row);
        });
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void deleteExpiredAttempts() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(RETENTION);
        var keys = independent.execute(status -> repository.staleIdentities(cutoff, now, PageRequest.of(0, 1000)));
        for (String key : Objects.requireNonNull(keys)) {
            independent.executeWithoutResult(status -> repository.lockIdentity(key).ifPresent(row -> {
                if (row.getLastActivity().isBefore(cutoff)
                        && (row.getBlockedUntil() == null || !row.getBlockedUntil().isAfter(now))) {
                    repository.delete(row);
                }
            }));
        }
    }

    private void reset(LoginAttemptEntity row, Instant now) {
        row.setFailures(0);
        row.setBlockedUntil(null);
        row.setWindowStartedAt(now);
        row.setLastActivity(now);
    }

    public static String identityKey(String identity) {
        String normalized = EmailIdentity.canonicalize(identity);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((normalized == null ? "" : normalized).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Required SHA-256 algorithm unavailable", impossible);
        }
    }

    private record Outcome(Authentication authentication, AuthenticationException failure) { }
}
