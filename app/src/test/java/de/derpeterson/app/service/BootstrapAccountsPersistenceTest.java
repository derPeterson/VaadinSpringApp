package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.config.BootstrapAccountsProperties;
import de.derpeterson.app.config.BootstrapAccountsProperties.Account;
import de.derpeterson.app.config.DatabaseInitializer;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BootstrapAccountsPersistenceTest {
  @Autowired UserRepository users;
  @Autowired RoleRepository roles;
  @Autowired PlatformTransactionManager manager;
  @Autowired TransactionTemplate tx;
  @PersistenceContext EntityManager em;
  BootstrapAccountsProperties settings;

  @BeforeEach
  void resetDatabase() {
    tx.executeWithoutResult(
        s -> {
          users.deleteAll();
          users.flush();
          em.createQuery("delete from RoleEntity").executeUpdate();
        });
    settings = new BootstrapAccountsProperties();
    settings.setEnabled(true);
  }

  BootstrapAccountsProperties.Account account(String email) {
    Account a = new BootstrapAccountsProperties.Account();
    a.setEmail(email);
    a.setPassword("OnlyForTests!");
    return a;
  }

  DatabaseInitializer initializer(PasswordEncoder encoder) {
    ConfigService config = mock(ConfigService.class);
    when(config.exists(any())).thenReturn(true);
    return new DatabaseInitializer(
        users, roles, config, encoder, mock(Environment.class), settings, manager);
  }

  void run() throws Exception {
    initializer(new BCryptPasswordEncoder(4)).initDatabase().run();
  }

  @Test
  void allFourListsCreateMultipleAccountsWithExactlyOneRoleAndRealHashes() throws Exception {
    settings.setSuperadmins(
        List.of(account("super1@example.test"), account("super2@example.test")));
    settings.setAdmins(List.of(account("admin@example.test")));
    settings.setUsers(List.of(account("user@example.test")));
    settings.setGuests(List.of(account("guest@example.test")));
    run();
    run();
    assertEquals(5, users.count());
    assertEquals(4, roles.count());
    Map<String, RoleType> expected =
        Map.of(
            "super1",
            RoleType.ROLE_SUPERADMIN,
            "super2",
            RoleType.ROLE_SUPERADMIN,
            "admin",
            RoleType.ROLE_ADMIN,
            "user",
            RoleType.ROLE_USER,
            "guest",
            RoleType.ROLE_GUEST);
    for (UserEntity user : users.findAll()) {
      assertEquals(
          Set.of(expected.get(user.getEmail().split("@")[0])),
          user.getRoleEntities().stream()
              .map(r -> r.getName())
              .collect(java.util.stream.Collectors.toSet()));
      assertTrue(user.isEnabled());
      assertFalse(user.isVerificationPending());
      assertTrue(new BCryptPasswordEncoder().matches("OnlyForTests!", user.getPassword()));
    }
  }

  @Test
  void existingAccountIsNotPromotedRenamedOrGivenANewPassword() throws Exception {
    settings.setUsers(List.of(account("existing@example.test")));
    run();
    UserEntity existing = users.findAll().getFirst();
    String hash = existing.getPassword();
    settings.setUsers(List.of());
    Account replacement = account(" EXISTING@example.test ");
    replacement.setPassword("Changed-secret!");
    replacement.setFirstName("Changed");
    settings.setSuperadmins(List.of(replacement));
    run();
    UserEntity stored = users.findAll().getFirst();
    assertEquals(hash, stored.getPassword());
    assertEquals(existing.getFirstName(), stored.getFirstName());
    assertEquals(1, users.count());
    assertTrue(stored.hasRole(RoleType.ROLE_USER));
    assertFalse(stored.hasRole(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void duplicateAcrossListsIsRejectedBeforeAnyCreationAndDoesNotLeakCredentials() {
    settings.setSuperadmins(List.of(account("duplicate@example.test")));
    settings.setGuests(List.of(account(" DUPLICATE@example.test ")));
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, this::run);
    assertFalse(error.getMessage().contains("OnlyForTests"));
    assertEquals(0, users.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"email", "password", "firstName", "birthDate", "language"})
  void invalidLaterEntryDoesNotLeavePartialAccounts(String invalid) {
    Account broken = account("broken@example.test");
    switch (invalid) {
      case "email" -> broken.setEmail("invalid");
      case "password" -> broken.setPassword("weak");
      case "firstName" -> broken.setFirstName(" ");
      case "birthDate" -> broken.setBirthDate(java.time.LocalDate.now().plusDays(1));
      case "language" -> broken.setPreferredLocale(Locale.FRENCH);
      default -> throw new IllegalArgumentException("Unknown invalid bootstrap field: " + invalid);
    }
    settings.setAdmins(List.of(account("valid@example.test"), broken));
    assertThrows(IllegalArgumentException.class, this::run);
    assertEquals(0, users.count());
  }

  @Test
  void databaseFailureRollsBackTheWholeSeedTransaction() {
    settings.setAdmins(List.of(account("first@example.test"), account("second@example.test")));
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    when(encoder.encode(any()))
        .thenReturn("first-hash")
        .thenThrow(new IllegalStateException("test failure"));
    CommandLineRunner initialization = initializer(encoder).initDatabase();
    assertThrows(IllegalStateException.class, initialization::run);
    assertEquals(0, users.count());
    assertEquals(0, roles.count());
  }

  @Test
  void disabledBootstrapCreatesRolesButNoAccountsAndIgnoresInactiveListErrors() throws Exception {
    settings.setEnabled(false);
    settings.setUsers(List.of(account("invalid")));
    run();
    assertEquals(0, users.count());
    assertEquals(4, roles.count());
  }

  @Test
  void springPropertiesBindEveryGroupAndOptionalProfileFields() {
    HashMap<String, Object> values = new HashMap<String, Object>();
    values.put("app.bootstrap.enabled", "true");
    for (String group : List.of("superadmins", "admins", "users", "guests")) {
      values.put("app.bootstrap." + group + "[0].email", group + "@example.test");
      values.put("app.bootstrap." + group + "[0].password", "OnlyForTests!");
    }
    values.put("app.bootstrap.superadmins[0].first-name", "Test");
    values.put("app.bootstrap.superadmins[0].birth-date", "1986-04-26");
    values.put("app.bootstrap.superadmins[0].preferred-locale", "de");
    BootstrapAccountsProperties bound =
        new Binder(new MapConfigurationPropertySource(values))
            .bind("app.bootstrap", Bindable.of(BootstrapAccountsProperties.class))
            .get();
    assertEquals(4, bound.validatedEntries().size());
    assertEquals("Test", bound.getSuperadmins().getFirst().getFirstName());
    assertEquals(Locale.GERMAN, bound.getSuperadmins().getFirst().getPreferredLocale());
    assertFalse(bound.getSuperadmins().getFirst().toString().contains("OnlyForTests"));
  }

  @Test
  void indexedOperatingSystemEnvironmentBindsListsUsedByDocker() {
    SystemEnvironmentPropertySource env =
        new SystemEnvironmentPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            Map.of(
                "APP_BOOTSTRAP_ENABLED",
                "true",
                "APP_BOOTSTRAP_ADMINS_0_EMAIL",
                "admin@example.test",
                "APP_BOOTSTRAP_ADMINS_0_PASSWORD",
                "OnlyForTests!"));
    BootstrapAccountsProperties bound =
        new Binder(ConfigurationPropertySources.from(env))
            .bind("app.bootstrap", Bindable.of(BootstrapAccountsProperties.class))
            .get();
    assertEquals(1, bound.validatedEntries().size());
    assertEquals(RoleType.ROLE_ADMIN, bound.validatedEntries().getFirst().role());
  }
}
