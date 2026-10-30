package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminUserFiltersTest {
  private final Instant cutoff = Instant.parse("2026-10-12T08:30:00Z");

  private UserEntity user() {
    return UserEntity.builder()
        .firstName("Lena")
        .lastName("Bauer")
        .email("lena@example.test")
        .enabled(true)
        .status(UserStatus.OFFLINE)
        .lastActivity(cutoff)
        .roleEntities(
            List.of(
                RoleEntity.builder().name(RoleType.ROLE_USER).build(),
                RoleEntity.builder().name(RoleType.ROLE_ADMIN).build()))
        .build();
  }

  @Test
  void multipleStatusAndRoleSelectionsEachCountAsOneGroup() {
    AdminUserFilters filters =
        new AdminUserFilters(
            "",
            true,
            Set.of(UserStatus.AVAILABLE, UserStatus.EMPLOYED, UserStatus.ABSENT),
            EnumSet.allOf(RoleType.class),
            null);
    assertEquals(3, filters.activeGroupCount());
    assertEquals(
        3,
        AdminUserFilters.countActiveGroups(
            filters.search(), filters.enabled(), filters.statuses(), filters.roles(), false));
  }

  @Test
  void dateAndTimeCountTogetherAndEveryGroupCountsAtMostOnce() {
    AdminUserFilters filters =
        new AdminUserFilters(
            "Lena", false, EnumSet.allOf(UserStatus.class), EnumSet.allOf(RoleType.class), cutoff);
    assertEquals(5, filters.activeGroupCount());
    assertEquals(1, new AdminUserFilters("", null, Set.of(), Set.of(), cutoff).activeGroupCount());
  }

  @Test
  void blankSearchAndEmptySelectionsDoNotCountButDisabledAccountsDo() {
    assertEquals(0, new AdminUserFilters("  ", null, Set.of(), Set.of(), null).activeGroupCount());
    assertEquals(0, new AdminUserFilters(null, null, Set.of(), Set.of(), null).activeGroupCount());
    assertEquals(1, new AdminUserFilters("", false, Set.of(), Set.of(), null).activeGroupCount());
  }

  @Test
  void searchMatchesOnlyNameAndEmailIgnoringCaseAndSurroundingSpaces() {
    for (String query : List.of("", " LENA BAUER ", "bauer", "LENA@EXAMPLE")) {
      assertTrue(
          new AdminUserFilters(query, null, Set.of(), Set.of(), null).matches(user()), query);
    }
    for (String query : List.of("offline", "administrator", "2026", "absent", "no match")) {
      assertFalse(
          new AdminUserFilters(query, null, Set.of(), Set.of(), null).matches(user()), query);
    }
  }

  @Test
  void roleFilterMatchesMembershipIncludingAccountsWithMultipleRoles() {
    assertTrue(
        new AdminUserFilters("", null, Set.of(), Set.of(RoleType.ROLE_USER), null).matches(user()));
    assertTrue(
        new AdminUserFilters("", null, Set.of(), Set.of(RoleType.ROLE_ADMIN), null)
            .matches(user()));
    UserEntity plain = user();
    plain.setRoleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()));
    assertFalse(
        new AdminUserFilters("", null, Set.of(), Set.of(RoleType.ROLE_ADMIN), null).matches(plain));
    plain.setRoleEntities(null);
    assertFalse(
        new AdminUserFilters("", null, Set.of(), Set.of(RoleType.ROLE_USER), null).matches(plain));
  }

  @Test
  void activeSinceIncludesEqualityAndLaterButExcludesEarlierOrMissingActivity() {
    AdminUserFilters filter = new AdminUserFilters("", null, Set.of(), Set.of(), cutoff);
    UserEntity candidate = user();
    assertTrue(filter.matches(candidate));
    candidate.setLastActivity(cutoff.plusNanos(1));
    assertTrue(filter.matches(candidate));
    candidate.setLastActivity(cutoff.minusNanos(1));
    assertFalse(filter.matches(candidate));
    candidate.setLastActivity(null);
    assertFalse(filter.matches(candidate));
    assertTrue(new AdminUserFilters("", null, Set.of(), Set.of(), null).matches(candidate));
  }

  @Test
  void allSelectedFiltersMustMatchAndNullSelectionsRemoveRestrictions() {
    assertTrue(
        new AdminUserFilters(
                "lena", true, Set.of(UserStatus.OFFLINE), Set.of(RoleType.ROLE_USER), cutoff)
            .matches(user()));
    assertFalse(
        new AdminUserFilters(
                "lena", false, Set.of(UserStatus.OFFLINE), Set.of(RoleType.ROLE_USER), cutoff)
            .matches(user()));
    assertFalse(
        new AdminUserFilters(
                "lena", true, Set.of(UserStatus.AVAILABLE), Set.of(RoleType.ROLE_USER), cutoff)
            .matches(user()));
    assertFalse(
        new AdminUserFilters(
                "other", true, Set.of(UserStatus.OFFLINE), Set.of(RoleType.ROLE_USER), cutoff)
            .matches(user()));
    assertTrue(new AdminUserFilters(null, null, Set.of(), Set.of(), null).matches(user()));
  }

  @Test
  void multipleSelectionsUseOrWithinEachGroupAndAndBetweenGroups() {
    assertTrue(
        new AdminUserFilters(
                "",
                null,
                Set.of(UserStatus.AVAILABLE, UserStatus.OFFLINE),
                Set.of(RoleType.ROLE_GUEST, RoleType.ROLE_ADMIN),
                null)
            .matches(user()));
    assertFalse(
        new AdminUserFilters(
                "",
                null,
                Set.of(UserStatus.AVAILABLE, UserStatus.ABSENT),
                Set.of(RoleType.ROLE_USER),
                null)
            .matches(user()));
  }
}
