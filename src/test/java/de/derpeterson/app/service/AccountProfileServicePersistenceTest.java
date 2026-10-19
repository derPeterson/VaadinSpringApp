package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.MaintenanceAccessException;
import de.derpeterson.app.service.AccountProfileService.Changes;
import de.derpeterson.app.service.AccountProfileService.Profile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

/** Echte Transaktionen auf eigener H2-Datenbank; keine Anwendungsdaten oder externen Dienste. */
@SpringJUnitConfig(AccountProfileServicePersistenceTest.Config.class)
class AccountProfileServicePersistenceTest {
  @Configuration
  @Import({UserServicePersistenceTest.Config.class, AccountProfileService.class})
  static class Config {
    @Bean
    ConfigService configService() {
      return mock(ConfigService.class);
    }
  }

  @Autowired private AccountProfileService profiles;
  @Autowired private UserRepository users;
  @Autowired private ConfigService config;
  @Autowired private TransactionTemplate transaction;
  @PersistenceContext private EntityManager em;
  private final MockHttpServletRequest request = new MockHttpServletRequest();
  private UserEntity account;
  private Long tokenId;
  private Long otherId;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    reset(config);
    transaction.executeWithoutResult(
        status -> {
          em.createQuery("delete from VerificationTokenEntity").executeUpdate();
          users.deleteAll();
          users.flush();
          em.createQuery("delete from RoleEntity").executeUpdate();
          RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
          em.persist(role);
          account = newUser("profile@example.test", role);
          em.persist(account);
          UserEntity other = newUser("other@example.test", role);
          em.persist(other);
          otherId = other.getId();
          VerificationTokenEntity token = new VerificationTokenEntity();
          token.setToken("own-unconsumed-token");
          token.setUserEntity(account);
          token.setExpiryDate(LocalDateTime.now().plusDays(1));
          em.persist(token);
          tokenId = token.getId();
        });
    account = users.findById(account.getId()).orElseThrow();
    authenticate();
  }

  private UserEntity newUser(String email, RoleEntity role) {
    return UserEntity.builder()
        .firstName("Before")
        .lastName("Profile")
        .email(email)
        .password("unchanged-hash")
        .gender(Gender.OTHER)
        .birthDate(LocalDate.of(1990, 1, 1))
        .preferredLocale(Locale.GERMAN)
        .enabled(true)
        .roleEntities(new ArrayList<>(List.of(role)))
        .build();
  }

  private void authenticate() {
    UserDetails principal =
        new CustomUserDetailsService(users).loadUserByUsername(account.getEmail());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private Changes changes(Long version) {
    return new Changes(version, " Updated ", " Name ", LocalDate.of(1995, 2, 3), Locale.ENGLISH);
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void allRolesCanSaveOnlyTheirProfileAndRepeatWithoutAStaleVersion(RoleType role) {
    transaction.executeWithoutResult(
        status -> {
          UserEntity assigned = users.findById(account.getId()).orElseThrow();
          RoleEntity entity = RoleEntity.builder().name(role).build();
          if (role == RoleType.ROLE_USER) {
            entity = assigned.getRoleEntities().iterator().next();
          } else {
            em.persist(entity);
          }
          assigned.setRoleEntities(new ArrayList<>(List.of(entity)));
        });
    account = users.findById(account.getId()).orElseThrow();
    authenticate();
    Profile saved = profiles.save(request, changes(account.getVersion()));
    UserEntity stored =
        transaction.execute(
            status -> {
              UserEntity user = users.findById(account.getId()).orElseThrow();
              assertEquals(
                  List.of(role), user.getRoleEntities().stream().map(RoleEntity::getName).toList());
              return user;
            });
    assertEquals("Updated", stored.getFirstName());
    assertEquals("Name", stored.getLastName());
    assertEquals(LocalDate.of(1995, 2, 3), stored.getBirthDate());
    assertEquals(Locale.ENGLISH, stored.getPreferredLocale());
    assertEquals(account.getEmail(), stored.getEmail());
    assertEquals(account.getPassword(), stored.getPassword());
    assertEquals(account.getGender(), stored.getGender());
    assertTrue(stored.isEnabled());
    assertEquals(account.isVerificationPending(), stored.isVerificationPending());
    assertEquals(account.getStatus(), stored.getStatus());
    assertEquals(account.getLastActivity(), stored.getLastActivity());
    assertEquals("Before", users.findById(otherId).orElseThrow().getFirstName());
    transaction.executeWithoutResult(
        status ->
            assertEquals(
                "own-unconsumed-token",
                em.find(VerificationTokenEntity.class, tokenId).getToken()));
    assertEquals(stored.getVersion(), saved.version());
    assertNotEquals(account.getVersion(), saved.version());
    assertDoesNotThrow(() -> profiles.save(request, changes(saved.version())));
  }

  @Test
  void staleAndMissingVersionsCannotOverwriteAnotherEdit() {
    transaction.executeWithoutResult(
        status -> users.findById(account.getId()).orElseThrow().setFirstName("Concurrent"));
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> profiles.save(request, changes(account.getVersion())));
    assertThrows(
        OptimisticLockingFailureException.class, () -> profiles.save(request, changes(null)));
    assertEquals("Concurrent", users.findById(account.getId()).orElseThrow().getFirstName());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
  void invalidValuesAreRejectedServerSideWithoutPartialWrites(int input) {
    Changes valid = changes(account.getVersion());
    Changes invalid =
        switch (input) {
          case 0 ->
              new Changes(
                  valid.version(), " ", valid.lastName(), valid.birthDate(), valid.language());
          case 1 ->
              new Changes(
                  valid.version(), valid.firstName(), null, valid.birthDate(), valid.language());
          case 2 ->
              new Changes(
                  valid.version(), valid.firstName(), valid.lastName(), null, valid.language());
          case 3 ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  valid.lastName(),
                  LocalDate.now(),
                  valid.language());
          case 4 ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  valid.lastName(),
                  LocalDate.now().plusDays(1),
                  valid.language());
          case 5 ->
              new Changes(
                  valid.version(), valid.firstName(), valid.lastName(), valid.birthDate(), null);
          default ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  valid.lastName(),
                  valid.birthDate(),
                  Locale.FRENCH);
        };
    assertThrows(IllegalArgumentException.class, () -> profiles.save(request, invalid));
    UserEntity stored = users.findById(account.getId()).orElseThrow();
    assertEquals("Before", stored.getFirstName());
    assertEquals(account.getVersion(), stored.getVersion());
    assertEquals(Locale.GERMAN, stored.getPreferredLocale());
  }

  @Test
  void anonymousAndRevokedAccountsCannotSave() {
    SecurityContextHolder.clearContext();
    assertThrows(
        AccessDeniedException.class, () -> profiles.save(request, changes(account.getVersion())));
    authenticate();
    transaction.executeWithoutResult(
        status -> users.findById(account.getId()).orElseThrow().setEnabled(false));
    assertThrows(
        AccessDeniedException.class, () -> profiles.save(request, changes(account.getVersion())));
    assertEquals("Before", users.findById(account.getId()).orElseThrow().getFirstName());
  }

  @Test
  void unavailableMaintenanceConfigurationDeniesWrites() {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalStateException("Unavailable"));
    assertThrows(
        MaintenanceAccessException.class,
        () -> profiles.save(request, changes(account.getVersion())));
    assertEquals("Before", users.findById(account.getId()).orElseThrow().getFirstName());
  }

  @Test
  void deletedAccountCannotSaveAnOldForm() {
    transaction.executeWithoutResult(status -> users.deleteById(account.getId()));
    assertThrows(
        AccessDeniedException.class, () -> profiles.save(request, changes(account.getVersion())));
    assertFalse(users.existsById(account.getId()));
    assertEquals("Before", users.findById(otherId).orElseThrow().getFirstName());
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void maintenanceAllowsOnlyAdministrativeRoles(RoleType role) {
    transaction.executeWithoutResult(
        status -> {
          UserEntity user = users.findById(account.getId()).orElseThrow();
          RoleEntity entity =
              role == RoleType.ROLE_USER
                  ? user.getRoleEntities().iterator().next()
                  : RoleEntity.builder().name(role).build();
          if (role != RoleType.ROLE_USER) {
            em.persist(entity);
          }
          user.setRoleEntities(new ArrayList<>(List.of(entity)));
        });
    account = users.findById(account.getId()).orElseThrow();
    authenticate();
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    if (role.isAdministrative()) {
      assertDoesNotThrow(() -> profiles.save(request, changes(account.getVersion())));
    } else {
      assertThrows(
          MaintenanceAccessException.class,
          () -> profiles.save(request, changes(account.getVersion())));
      assertEquals("Before", users.findById(account.getId()).orElseThrow().getFirstName());
    }
  }
}
