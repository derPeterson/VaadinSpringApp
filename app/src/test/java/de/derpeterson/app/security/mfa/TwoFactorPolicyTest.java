package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.enums.RoleType;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TwoFactorPolicyTest {
  @Test
  void defaultsRequireAdminsButAllowVoluntaryEnrollmentForOrdinaryUsers() {
    TwoFactorPolicy policy = TwoFactorPolicy.defaults();

    assertFalse(policy.inactivityEnabled());
    assertFalse(policy.developmentPause());
    assertFalse(policy.developmentGoogleConfirmation());
    assertEquals(
        TwoFactorMode.REQUIRED, policy.mode(List.of(RoleType.ROLE_ADMIN), TwoFactorAction.LOGIN));
    assertEquals(
        TwoFactorMode.REQUIRED,
        policy.mode(List.of(RoleType.ROLE_SUPERADMIN), TwoFactorAction.PASSWORD));
    assertEquals(
        TwoFactorMode.IF_ENROLLED, policy.mode(List.of(RoleType.ROLE_USER), TwoFactorAction.LOGIN));
    assertTrue(
        policy.mode(List.of(RoleType.ROLE_USER), TwoFactorAction.PASSWORD).requiresFactor(true));
    assertFalse(
        policy.mode(List.of(RoleType.ROLE_USER), TwoFactorAction.PASSWORD).requiresFactor(false));
    assertEquals(
        TwoFactorMode.OFF,
        policy.mode(List.of(RoleType.ROLE_ADMIN), TwoFactorAction.ADMINISTRATION));
  }

  @Test
  void everyRoleIncludingSuperadminsCanBeConfiguredAndStrictestRuleWins() {
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> rules = new EnumMap<>(RoleType.class);
    TwoFactorPolicy.defaults()
        .rules()
        .forEach((role, actions) -> rules.put(role, new EnumMap<>(actions)));
    rules.get(RoleType.ROLE_SUPERADMIN).put(TwoFactorAction.LOGIN, TwoFactorMode.OFF);
    rules.get(RoleType.ROLE_ADMIN).put(TwoFactorAction.LOGIN, TwoFactorMode.OFF);
    rules.get(RoleType.ROLE_USER).put(TwoFactorAction.LOGIN, TwoFactorMode.REQUIRED);
    TwoFactorPolicy policy = new TwoFactorPolicy(rules, false, Duration.ofHours(2), false, false);

    assertEquals(
        TwoFactorMode.OFF,
        policy.mode(List.of(RoleType.ROLE_SUPERADMIN, RoleType.ROLE_ADMIN), TwoFactorAction.LOGIN));
    assertEquals(
        TwoFactorMode.REQUIRED,
        policy.mode(List.of(RoleType.ROLE_ADMIN, RoleType.ROLE_USER), TwoFactorAction.LOGIN));
    rules.get(RoleType.ROLE_USER).put(TwoFactorAction.LOGIN, TwoFactorMode.OFF);

    assertEquals(
        TwoFactorMode.REQUIRED, policy.mode(List.of(RoleType.ROLE_USER), TwoFactorAction.LOGIN));
    assertEquals(TwoFactorMode.REQUIRED, policy.mode(List.of(), TwoFactorAction.LOGIN));
  }

  @Test
  void rejectsIncompleteOrAmbiguousRules() {
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> missingRules = Map.of();
    Duration validTimeout = Duration.ofHours(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> new TwoFactorPolicy(missingRules, false, validTimeout, false, false));
    Map<RoleType, Map<TwoFactorAction, TwoFactorMode>> defaultRules =
        TwoFactorPolicy.defaults().rules();

    assertThrows(
        IllegalArgumentException.class,
        () -> new TwoFactorPolicy(defaultRules, true, Duration.ZERO, false, false));
    Duration subsecondTimeout = Duration.ofNanos(1);

    assertThrows(
        IllegalArgumentException.class,
        () -> new TwoFactorPolicy(defaultRules, true, subsecondTimeout, false, false));
  }
}
