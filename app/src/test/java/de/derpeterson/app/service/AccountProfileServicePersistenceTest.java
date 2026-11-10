package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
  @Autowired private UserRepository userRepository;
  @Autowired private ConfigService config;
  @Autowired private TransactionTemplate transactionTemplate;
  @PersistenceContext private EntityManager entityManager;
  private final MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
  private UserEntity testUserEntity;
  private Long tokenId;
  private Long otherId;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
    reset(config);
    transactionTemplate.executeWithoutResult(status -> {
      entityManager.createQuery("delete from VerificationTokenEntity").executeUpdate();
      userRepository.deleteAll();
      userRepository.flush();
      entityManager.createQuery("delete from RoleEntity").executeUpdate();
      RoleEntity role = RoleEntity.builder().name(RoleType.ROLE_USER).build();
      entityManager.persist(role);
      testUserEntity = newUser("profile@example.test", role);
      entityManager.persist(testUserEntity);
      UserEntity other = newUser("other@example.test", role);
      entityManager.persist(other);
      otherId = other.getId();
      VerificationTokenEntity token = new VerificationTokenEntity();
      token.setToken("own-unconsumed-token");
      token.setUserEntity(testUserEntity);
      token.setExpiryDate(LocalDateTime.now().plusDays(1));
      entityManager.persist(token);
      tokenId = token.getId();
    });
    testUserEntity = userRepository.findById(testUserEntity.getId()).orElseThrow();
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
        new CustomUserDetailsService(userRepository).loadUserByUsername(testUserEntity.getEmail());
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
    return new Changes(
        version, " Updated ", " Name ", LocalDate.of(1995, 2, 3), Locale.ENGLISH, Gender.OTHER);
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void allRolesCanSaveOnlyTheirProfileAndRepeatWithoutAStaleVersion(RoleType role) {
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity assigned = userRepository.findById(testUserEntity.getId()).orElseThrow();
      RoleEntity entity = RoleEntity.builder().name(role).build();
      if (role == RoleType.ROLE_USER) {
        entity = assigned.getRoleEntities().iterator().next();
      } else {
        entityManager.persist(entity);
      }
      assigned.setRoleEntities(new ArrayList<>(List.of(entity)));
    });
    testUserEntity = userRepository.findById(testUserEntity.getId()).orElseThrow();
    authenticate();
    Profile saved = profiles.save(mockHttpRequest, changes(testUserEntity.getVersion()));
    UserEntity stored =
        transactionTemplate.execute(status -> {
          UserEntity user = userRepository.findById(testUserEntity.getId()).orElseThrow();
          assertEquals(
              List.of(role), user.getRoleEntities().stream().map(RoleEntity::getName).toList());
          return user;
        });
    assertEquals("Updated", stored.getFirstName());
    assertEquals("Name", stored.getLastName());
    assertEquals(LocalDate.of(1995, 2, 3), stored.getBirthDate());
    assertEquals(Locale.ENGLISH, stored.getPreferredLocale());
    assertEquals(testUserEntity.getEmail(), stored.getEmail());
    assertEquals(testUserEntity.getPassword(), stored.getPassword());
    assertEquals(testUserEntity.getGender(), stored.getGender());
    assertTrue(stored.isEnabled());
    assertEquals(testUserEntity.isVerificationPending(), stored.isVerificationPending());
    assertEquals(testUserEntity.getStatus(), stored.getStatus());
    assertEquals(testUserEntity.getLastActivity(), stored.getLastActivity());
    assertEquals("Before", userRepository.findById(otherId).orElseThrow().getFirstName());
    transactionTemplate.executeWithoutResult(status ->
        assertEquals(
            "own-unconsumed-token",
            entityManager.find(VerificationTokenEntity.class, tokenId).getToken()));
    assertEquals(stored.getVersion(), saved.version());
    assertNotEquals(testUserEntity.getVersion(), saved.version());
    assertDoesNotThrow(() -> profiles.save(mockHttpRequest, changes(saved.version())));
  }

  @Test
  void staleAndMissingVersionsCannotOverwriteAnotherEdit() {
    transactionTemplate.executeWithoutResult(status ->
        userRepository.findById(testUserEntity.getId()).orElseThrow().setFirstName("Concurrent"));
    Changes changesaccountVersion = changes(testUserEntity.getVersion());
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> profiles.save(mockHttpRequest, changesaccountVersion));
    Changes profileChanges = changes(null);
    assertThrows(
        OptimisticLockingFailureException.class,
        () -> profiles.save(mockHttpRequest, profileChanges));
    assertEquals(
        "Concurrent", userRepository.findById(testUserEntity.getId()).orElseThrow().getFirstName());
  }

  @ParameterizedTest
  @EnumSource(Gender.class)
  void genderIsPersistedForTheAuthenticatedAccountOnly(Gender gender) {
    Changes change =
        new Changes(
            testUserEntity.getVersion(),
            "Updated",
            "Name",
            LocalDate.of(1995, 2, 3),
            Locale.ENGLISH,
            gender);
    Profile saved = profiles.save(mockHttpRequest, change);
    assertEquals(gender, saved.gender());
    assertEquals(gender, userRepository.findById(testUserEntity.getId()).orElseThrow().getGender());
    assertEquals(Gender.OTHER, userRepository.findById(otherId).orElseThrow().getGender());
  }

  @Test
  void optionalProfileDataCanBeClearedAndReloaded() {
    Changes change =
        new Changes(testUserEntity.getVersion(), "Updated", "Name", null, Locale.ENGLISH, null);
    AccountProfileService.Profile saved = profiles.save(mockHttpRequest, change);
    UserEntity stored = userRepository.findById(testUserEntity.getId()).orElseThrow();
    assertNull(saved.gender());
    assertNull(saved.birthDate());
    assertNull(stored.getGender());
    assertNull(stored.getBirthDate());
    assertEquals("Updated", stored.getFirstName());
    assertEquals(testUserEntity.getPassword(), stored.getPassword());
    assertEquals(saved.version(), stored.getVersion());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3, 4, 5, 6})
  void invalidValuesAreRejectedServerSideWithoutPartialWrites(int input) {
    Changes valid = changes(testUserEntity.getVersion());
    Changes invalid =
        switch (input) {
          case 0 ->
              new Changes(
                  valid.version(),
                  " ",
                  valid.lastName(),
                  valid.birthDate(),
                  valid.language(),
                  Gender.OTHER);
          case 1 ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  null,
                  valid.birthDate(),
                  valid.language(),
                  Gender.OTHER);
          case 3 ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  valid.lastName(),
                  LocalDate.now(),
                  valid.language(),
                  Gender.OTHER);
          case 4 ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  valid.lastName(),
                  LocalDate.now().plusDays(1),
                  valid.language(),
                  Gender.OTHER);
          case 5 ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  valid.lastName(),
                  valid.birthDate(),
                  null,
                  Gender.OTHER);
          default ->
              new Changes(
                  valid.version(),
                  valid.firstName(),
                  valid.lastName(),
                  valid.birthDate(),
                  Locale.FRENCH,
                  Gender.OTHER);
        };
    assertThrows(IllegalArgumentException.class, () -> profiles.save(mockHttpRequest, invalid));
    UserEntity stored = userRepository.findById(testUserEntity.getId()).orElseThrow();
    assertEquals("Before", stored.getFirstName());
    assertEquals(testUserEntity.getVersion(), stored.getVersion());
    assertEquals(Locale.GERMAN, stored.getPreferredLocale());
  }

  @Test
  void anonymousAndRevokedAccountsCannotSave() {
    SecurityContextHolder.clearContext();
    Changes changesaccountVersion2 = changes(testUserEntity.getVersion());
    assertThrows(
        AccessDeniedException.class, () -> profiles.save(mockHttpRequest, changesaccountVersion2));
    authenticate();
    transactionTemplate.executeWithoutResult(
        status -> userRepository.findById(testUserEntity.getId()).orElseThrow().setEnabled(false));
    Changes changesaccountVersion3 = changes(testUserEntity.getVersion());
    assertThrows(
        AccessDeniedException.class, () -> profiles.save(mockHttpRequest, changesaccountVersion3));
    assertEquals(
        "Before", userRepository.findById(testUserEntity.getId()).orElseThrow().getFirstName());
  }

  @Test
  void unavailableMaintenanceConfigurationDeniesWrites() {
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE))
        .thenThrow(new IllegalStateException("Unavailable"));
    Changes changesaccountVersion4 = changes(testUserEntity.getVersion());
    assertThrows(
        MaintenanceAccessException.class,
        () -> profiles.save(mockHttpRequest, changesaccountVersion4));
    assertEquals(
        "Before", userRepository.findById(testUserEntity.getId()).orElseThrow().getFirstName());
  }

  @Test
  void deletedAccountCannotSaveAnOldForm() {
    transactionTemplate.executeWithoutResult(
        status -> userRepository.deleteById(testUserEntity.getId()));
    Changes changesaccountVersion5 = changes(testUserEntity.getVersion());
    assertThrows(
        AccessDeniedException.class, () -> profiles.save(mockHttpRequest, changesaccountVersion5));
    assertFalse(userRepository.existsById(testUserEntity.getId()));
    assertEquals("Before", userRepository.findById(otherId).orElseThrow().getFirstName());
  }

  @ParameterizedTest
  @EnumSource(RoleType.class)
  void maintenanceAllowsOnlyAdministrativeRoles(RoleType role) {
    transactionTemplate.executeWithoutResult(status -> {
      UserEntity user = userRepository.findById(testUserEntity.getId()).orElseThrow();
      RoleEntity entity =
          role == RoleType.ROLE_USER
              ? user.getRoleEntities().iterator().next()
              : RoleEntity.builder().name(role).build();
      if (role != RoleType.ROLE_USER) {
        entityManager.persist(entity);
      }
      user.setRoleEntities(new ArrayList<>(List.of(entity)));
    });
    testUserEntity = userRepository.findById(testUserEntity.getId()).orElseThrow();
    authenticate();
    when(config.getBoolean(ConfigEntry.MAINTENANCE_MODE)).thenReturn(true);
    if (role.isAdministrative()) {
      assertDoesNotThrow(
          () -> profiles.save(mockHttpRequest, changes(testUserEntity.getVersion())));
    } else {
      Changes changesaccountVersion6 = changes(testUserEntity.getVersion());
      assertThrows(
          MaintenanceAccessException.class,
          () -> profiles.save(mockHttpRequest, changesaccountVersion6));
      assertEquals(
          "Before", userRepository.findById(testUserEntity.getId()).orElseThrow().getFirstName());
    }
  }
}
