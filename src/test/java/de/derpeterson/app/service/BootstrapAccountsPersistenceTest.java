package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.config.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.boot.context.properties.source.*;
import org.springframework.core.env.*;
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
    var a = new BootstrapAccountsProperties.Account();
    a.setEmail(email);
    a.setPassword("OnlyForTests!");
    return a;
  }

  DatabaseInitializer initializer(PasswordEncoder encoder) {
    var config = mock(ConfigService.class);
    when(config.exists(any())).thenReturn(true);
    return new DatabaseInitializer(
        users,
        roles,
        config,
        encoder,
        mock(Environment.class),
        mock(RoleSchemaUpdater.class),
        mock(de.derpeterson.app.config.UserNotificationSchemaUpdater.class),
        settings,
        manager);
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
    var expected =
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
    for (var user : users.findAll()) {
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
    var existing = users.findAll().getFirst();
    var hash = existing.getPassword();
    settings.setUsers(List.of());
    var replacement = account(" EXISTING@example.test ");
    replacement.setPassword("Changed-secret!");
    replacement.setFirstName("Changed");
    settings.setSuperadmins(List.of(replacement));
    run();
    var stored = users.findAll().getFirst();
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
    var error = assertThrows(IllegalArgumentException.class, this::run);
    assertFalse(error.getMessage().contains("OnlyForTests"));
    assertEquals(0, users.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"email", "password", "firstName", "birthDate", "language"})
  void invalidLaterEntryDoesNotLeavePartialAccounts(String invalid) {
    var broken = account("broken@example.test");
    switch (invalid) {
      case "email" -> broken.setEmail("invalid");
      case "password" -> broken.setPassword("weak");
      case "firstName" -> broken.setFirstName(" ");
      case "birthDate" -> broken.setBirthDate(java.time.LocalDate.now().plusDays(1));
      case "language" -> broken.setPreferredLocale(Locale.FRENCH);
    }
    settings.setAdmins(List.of(account("valid@example.test"), broken));
    assertThrows(IllegalArgumentException.class, this::run);
    assertEquals(0, users.count());
  }

  @Test
  void databaseFailureRollsBackTheWholeSeedTransaction() {
    settings.setAdmins(List.of(account("first@example.test"), account("second@example.test")));
    var encoder = mock(PasswordEncoder.class);
    when(encoder.encode(any()))
        .thenReturn("first-hash")
        .thenThrow(new IllegalStateException("test failure"));
    assertThrows(IllegalStateException.class, () -> initializer(encoder).initDatabase().run());
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
    var values = new HashMap<String, Object>();
    values.put("app.bootstrap.enabled", "true");
    for (var group : List.of("superadmins", "admins", "users", "guests")) {
      values.put("app.bootstrap." + group + "[0].email", group + "@example.test");
      values.put("app.bootstrap." + group + "[0].password", "OnlyForTests!");
    }
    values.put("app.bootstrap.superadmins[0].first-name", "Test");
    values.put("app.bootstrap.superadmins[0].birth-date", "1986-04-26");
    values.put("app.bootstrap.superadmins[0].preferred-locale", "de");
    var bound =
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
    var env =
        new SystemEnvironmentPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            Map.of(
                "APP_BOOTSTRAP_ENABLED",
                "true",
                "APP_BOOTSTRAP_ADMINS_0_EMAIL",
                "admin@example.test",
                "APP_BOOTSTRAP_ADMINS_0_PASSWORD",
                "OnlyForTests!"));
    var bound =
        new Binder(ConfigurationPropertySources.from(env))
            .bind("app.bootstrap", Bindable.of(BootstrapAccountsProperties.class))
            .get();
    assertEquals(1, bound.validatedEntries().size());
    assertEquals(RoleType.ROLE_ADMIN, bound.validatedEntries().getFirst().role());
  }
}
