package de.derpeterson.app.security.mfa;

import de.derpeterson.app.model.enums.RoleType;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Vollständige, unveränderliche Regelmatrix. Mehrere Rollen ergeben die strengste aktuelle Regel.
 */
public record TwoFactorPolicy(
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> rules,
    boolean inactivityEnabled,
    Duration inactivity,
    boolean developmentPause,
    boolean developmentGoogleConfirmation) {
  public TwoFactorPolicy {
    Objects.requireNonNull(rules, "Rules required");
    Objects.requireNonNull(inactivity, "Inactivity interval required");
    if (rules.size() != RoleType.values().length
        || inactivity.isNegative()
        || inactivity.isZero()
        || inactivity.toSeconds() % 60 != 0
        || inactivity.compareTo(Duration.ofDays(365)) > 0
        || inactivity.getNano() != 0) {
      throw new IllegalArgumentException("Invalid two-factor policy");
    }
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> checked = new EnumMap<>(RoleType.class);
    for (RoleType role : RoleType.values()) {
      Map<TwoFactorAction, TwoFactorMode> actions = rules.get(role);
      if (actions == null || actions.size() != TwoFactorAction.values().length) {
        throw new IllegalArgumentException("Incomplete two-factor policy");
      }
      for (TwoFactorAction action : TwoFactorAction.values()) {
        Objects.requireNonNull(actions.get(action), "Missing two-factor action");
      }
      checked.put(role, Map.copyOf(actions));
    }
    rules = Map.copyOf(checked);
  }

  public TwoFactorMode mode(Collection<RoleType> roles, TwoFactorAction action) {
    Objects.requireNonNull(action);
    // Ein Konto ohne aktuelle Rolle erhält durch eine leere Rollenmenge keinen Freibrief.
    if (roles.isEmpty()) {
      return TwoFactorMode.REQUIRED;
    }
    TwoFactorMode strictest = TwoFactorMode.OFF;
    for (RoleType role : roles) {
      TwoFactorMode candidate = rules.get(role).get(action);
      if (candidate.ordinal() > strictest.ordinal()) {
        strictest = candidate;
      }
    }
    return strictest;
  }

  public static TwoFactorPolicy defaults() {
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> rules = new EnumMap<>(RoleType.class);
    for (RoleType role : RoleType.values()) {
      Map<TwoFactorAction, TwoFactorMode> actions = new EnumMap<>(TwoFactorAction.class);
      for (TwoFactorAction action : TwoFactorAction.values()) {
        TwoFactorMode mode =
            role.isAdministrative() ? TwoFactorMode.REQUIRED : TwoFactorMode.IF_ENROLLED;
        actions.put(action, action == TwoFactorAction.ADMINISTRATION ? TwoFactorMode.OFF : mode);
      }
      rules.put(role, actions);
    }
    return new TwoFactorPolicy(rules, false, Duration.ofHours(12), false, false);
  }
}
