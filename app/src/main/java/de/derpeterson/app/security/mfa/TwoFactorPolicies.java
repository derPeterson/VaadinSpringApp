package de.derpeterson.app.security.mfa;

import de.derpeterson.app.config.ConfigurationCatalog.StartupSetting;
import de.derpeterson.app.helper.logging.AfterCommitLog;
import de.derpeterson.app.model.TwoFactorPolicyEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.TwoFactorPolicyRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Ein atomarer Regelstand statt unabhängig gespeicherter Einzelschalter. */
@Service
@Slf4j
public class TwoFactorPolicies {
  private final TwoFactorPolicyRepository twoFactorPolicyRepository;
  private final UserRepository userRepository;
  private final EntityManager entityManager;
  private final AdminRolePolicy adminRolePolicy;
  private final ObjectProvider<TwoFactorAuthorization> twoFactorAuthorizationProvider;
  private final boolean developmentOptions;
  private final JsonMapper policyJsonMapper = JsonMapper.builder().build();

  public record Snapshot(long revision, TwoFactorPolicy policy) implements Serializable {}

  // Sekunden bleiben das Speicherformat; die Oberfläche verwendet Zahl und Zeiteinheit.
  private record Stored(
      Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> rules,
      boolean inactivityEnabled,
      long inactivitySeconds,
      boolean developmentPause,
      boolean developmentGoogleConfirmation) {}

  public TwoFactorPolicies(
      TwoFactorPolicyRepository twoFactorPolicyRepository,
      UserRepository userRepository,
      EntityManager entityManager,
      AdminRolePolicy adminRolePolicy,
      ObjectProvider<TwoFactorAuthorization> twoFactorAuthorizationProvider,
      Environment environment) {
    this.twoFactorPolicyRepository = twoFactorPolicyRepository;
    this.userRepository = userRepository;
    this.entityManager = entityManager;
    this.adminRolePolicy = adminRolePolicy;
    this.twoFactorAuthorizationProvider = twoFactorAuthorizationProvider;
    developmentOptions =
        environment.getProperty(
            StartupSetting.DEVELOPMENT_OPTIONS_ENABLED.property(), Boolean.class, false);
  }

  /**
   * Interner Regelzugriff für Anmeldung und Fachprüfungen, unabhängig von UI-Administratorrechten.
   */
  public Snapshot current() {
    return twoFactorPolicyRepository
        .snapshot()
        .map(row -> new Snapshot(row.getVersion(), decode(row.getRules())))
        .orElseGet(() -> new Snapshot(-1, TwoFactorPolicy.defaults()));
  }

  public Snapshot load() {
    adminRolePolicy.requireSuperadmin();
    return current();
  }

  public boolean developmentOptionsAvailable() {
    return developmentOptions;
  }

  public boolean pauseEnabled(Snapshot snapshot) {
    return developmentOptions && snapshot.policy().developmentPause();
  }

  public boolean weakGoogleAllowed() {
    return developmentOptions && current().policy().developmentGoogleConfirmation();
  }

  @Transactional
  public Snapshot save(HttpServletRequest request, long expectedRevision, TwoFactorPolicy desired) {
    Objects.requireNonNull(desired);
    // Derselbe Rollenlock schützt auch konkurrierenden Superadmin-Entzug.
    userRepository
        .lockRole(RoleType.ROLE_SUPERADMIN)
        .orElseThrow(() -> new AccessDeniedException("Superadmin role unavailable"));
    adminRolePolicy.requireSuperadmin();
    Snapshot previous = current();
    if (previous.revision() != expectedRevision) {
      throw new OptimisticLockingFailureException("Two-factor configuration changed");
    }
    if (!developmentOptions
        && (desired.developmentPause() || desired.developmentGoogleConfirmation())) {
      throw new AccessDeniedException("Development security options are disabled at startup");
    }
    // Die alte Regel entscheidet über ihre eigene Abschwächung, niemals die Formularwerte.
    twoFactorAuthorizationProvider
        .getObject()
        .authorizeCurrent(request, TwoFactorAction.SECURITY_CONFIGURATION, true);
    TwoFactorPolicyEntity row =
        twoFactorPolicyRepository.findById(1).orElseGet(TwoFactorPolicyEntity::new);
    if (row.getVersion() != null) {
      entityManager.refresh(row);
      if (row.getVersion() != expectedRevision) {
        throw new OptimisticLockingFailureException("Two-factor configuration changed");
      }
    }
    row.setRules(encode(desired));
    TwoFactorPolicyEntity saved = twoFactorPolicyRepository.saveAndFlush(row);
    AfterCommitLog.info(
        log,
        "Two-factor policy updated: revision={}, developmentPause={}, developmentGoogle={}",
        saved.getVersion(),
        desired.developmentPause(),
        desired.developmentGoogleConfirmation());
    return new Snapshot(saved.getVersion(), desired);
  }

  private String encode(TwoFactorPolicy policy) {
    return policyJsonMapper.writeValueAsString(
        new Stored(
            policy.rules(),
            policy.inactivityEnabled(),
            policy.inactivity().toSeconds(),
            policy.developmentPause(),
            policy.developmentGoogleConfirmation()));
  }

  private TwoFactorPolicy decode(String json) {
    Stored stored = policyJsonMapper.readValue(json, Stored.class);
    return new TwoFactorPolicy(
        stored.rules(),
        stored.inactivityEnabled(),
        Duration.ofSeconds(stored.inactivitySeconds()),
        developmentOptions && stored.developmentPause(),
        developmentOptions && stored.developmentGoogleConfirmation());
  }
}
