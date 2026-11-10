package de.derpeterson.app.security.mfa;

import de.derpeterson.app.model.enums.RoleType;
import java.io.Serializable;
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
    boolean developmentGoogleConfirmation)
    implements Serializable {
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

    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> validatedRulesByRole =
        new EnumMap<>(RoleType.class);
    for (RoleType roleType : RoleType.values()) {
      Map<TwoFactorAction, TwoFactorMode> modesByAction = rules.get(roleType);
      if (modesByAction == null || modesByAction.size() != TwoFactorAction.values().length) {
        throw new IllegalArgumentException("Incomplete two-factor policy");
      }
      for (TwoFactorAction twoFactorAction : TwoFactorAction.values()) {
        Objects.requireNonNull(modesByAction.get(twoFactorAction), "Missing two-factor action");
      }
      validatedRulesByRole.put(roleType, Map.copyOf(modesByAction));
    }

    rules = Map.copyOf(validatedRulesByRole);
  }

  public TwoFactorMode mode(Collection<RoleType> accountRoles, TwoFactorAction twoFactorAction) {
    Objects.requireNonNull(twoFactorAction);
    // Ein Konto ohne aktuelle Rolle erhält durch eine leere Rollenmenge keinen Freibrief.
    if (accountRoles.isEmpty()) {
      return TwoFactorMode.REQUIRED;
    }

    TwoFactorMode strictestMode = TwoFactorMode.OFF;
    for (RoleType roleType : accountRoles) {
      TwoFactorMode candidateMode = rules.get(roleType).get(twoFactorAction);
      if (candidateMode.ordinal() > strictestMode.ordinal()) {
        strictestMode = candidateMode;
      }
    }

    return strictestMode;
  }

  public static TwoFactorPolicy defaults() {
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> defaultRulesByRole =
        new EnumMap<>(RoleType.class);
    for (RoleType roleType : RoleType.values()) {
      Map<TwoFactorAction, TwoFactorMode> defaultModesByAction =
          new EnumMap<>(TwoFactorAction.class);
      for (TwoFactorAction twoFactorAction : TwoFactorAction.values()) {
        TwoFactorMode defaultMode =
            roleType.isAdministrative() ? TwoFactorMode.REQUIRED : TwoFactorMode.IF_ENROLLED;
        defaultModesByAction.put(
            twoFactorAction,
            twoFactorAction == TwoFactorAction.ADMINISTRATION ? TwoFactorMode.OFF : defaultMode);
      }
      defaultRulesByRole.put(roleType, defaultModesByAction);
    }

    return new TwoFactorPolicy(defaultRulesByRole, false, Duration.ofHours(12), false, false);
  }
}
