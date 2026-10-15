package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.*;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminUserFiltersTest {
  private final LocalDateTime cutoff = LocalDateTime.of(2026, 10, 12, 10, 30);

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
  void searchMatchesOnlyNameAndEmailIgnoringCaseAndSurroundingSpaces() {
    for (String query : List.of("", " LENA BAUER ", "bauer", "LENA@EXAMPLE"))
      assertTrue(new AdminUserFilters(query, null, null, null, null).matches(user()), query);
    for (String query : List.of("offline", "administrator", "2026", "absent", "no match"))
      assertFalse(new AdminUserFilters(query, null, null, null, null).matches(user()), query);
  }

  @Test
  void roleFilterMatchesMembershipIncludingAccountsWithMultipleRoles() {
    assertTrue(new AdminUserFilters("", null, null, RoleType.ROLE_USER, null).matches(user()));
    assertTrue(new AdminUserFilters("", null, null, RoleType.ROLE_ADMIN, null).matches(user()));
    UserEntity plain = user();
    plain.setRoleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()));
    assertFalse(new AdminUserFilters("", null, null, RoleType.ROLE_ADMIN, null).matches(plain));
    plain.setRoleEntities(null);
    assertFalse(new AdminUserFilters("", null, null, RoleType.ROLE_USER, null).matches(plain));
  }

  @Test
  void activeSinceIncludesEqualityAndLaterButExcludesEarlierOrMissingActivity() {
    AdminUserFilters filter = new AdminUserFilters("", null, null, null, cutoff);
    UserEntity candidate = user();
    assertTrue(filter.matches(candidate));
    candidate.setLastActivity(cutoff.plusNanos(1));
    assertTrue(filter.matches(candidate));
    candidate.setLastActivity(cutoff.minusNanos(1));
    assertFalse(filter.matches(candidate));
    candidate.setLastActivity(null);
    assertFalse(filter.matches(candidate));
    assertTrue(new AdminUserFilters("", null, null, null, null).matches(candidate));
  }

  @Test
  void allSelectedFiltersMustMatchAndNullSelectionsRemoveRestrictions() {
    assertTrue(
        new AdminUserFilters("lena", true, UserStatus.OFFLINE, RoleType.ROLE_USER, cutoff)
            .matches(user()));
    assertFalse(
        new AdminUserFilters("lena", false, UserStatus.OFFLINE, RoleType.ROLE_USER, cutoff)
            .matches(user()));
    assertFalse(
        new AdminUserFilters("lena", true, UserStatus.AVAILABLE, RoleType.ROLE_USER, cutoff)
            .matches(user()));
    assertFalse(
        new AdminUserFilters("other", true, UserStatus.OFFLINE, RoleType.ROLE_USER, cutoff)
            .matches(user()));
    assertTrue(new AdminUserFilters(null, null, null, null, null).matches(user()));
  }
}
