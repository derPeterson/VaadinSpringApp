package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.validation.EmailConflict;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.RegistrationView;
import de.derpeterson.app.views.RegistrationViewTest;
import de.derpeterson.app.views.RegistrationViewTest.TestService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

/** Real constraints/commits in the isolated H2 fixture, never application data. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
class UserIdentityPersistenceTest {
  @Autowired UserService userService;
  @Autowired UserRepository userRepository;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired DataSource dataSource;
  @PersistenceContext EntityManager entityManager;
  private Long adminId;
  private Long targetId;
  private Long otherId;
  private RoleEntity ordinary;

  @BeforeEach
  void seed() {
    transactionTemplate.executeWithoutResult(tx -> {
      entityManager.createQuery("delete from VerificationTokenEntity").executeUpdate();
      entityManager.createQuery("delete from PasswordResetTokenEntity").executeUpdate();
      entityManager.createQuery("delete from EmailQueueEntity").executeUpdate();
      userRepository.deleteAll();
      userRepository.flush();
      entityManager.createQuery("delete from RoleEntity").executeUpdate();
      RoleEntity admin = RoleEntity.builder().name(RoleType.ROLE_SUPERADMIN).build();
      ordinary = RoleEntity.builder().name(RoleType.ROLE_USER).build();
      entityManager.persist(admin);
      entityManager.persist(ordinary);
      adminId = persist("admin@example.com", admin);
      targetId = persist("target@example.com", ordinary);
      otherId = persist("other@example.com", ordinary);
      for (Long id : List.of(targetId, otherId, adminId)) {
        UserEntity testUserEntity = entityManager.find(UserEntity.class, id);
        for (TokenStatus status : TokenStatus.values()) {
          VerificationTokenEntity verification =
              new VerificationTokenEntity(
                  null,
                  "verification-" + id + status,
                  testUserEntity,
                  LocalDateTime.now().plusHours(1),
                  status);
          PasswordResetTokenEntity reset =
              new PasswordResetTokenEntity(
                  null,
                  "reset-" + id + status,
                  testUserEntity,
                  LocalDateTime.now().plusHours(1),
                  status);
          entityManager.persist(verification);
          entityManager.persist(reset);
        }
        EmailQueueEntity mail = new EmailQueueEntity();
        mail.setUserEntity(testUserEntity);
        mail.setSubject("Test");
        mail.setBody("Test");
        entityManager.persist(mail);
      }
    });
  }

  private UserEntity user(String email, RoleEntity role) {
    return UserEntity.builder()
        .email(email)
        .firstName("Test")
        .lastName("User")
        .password("hash")
        .birthDate(LocalDate.of(1990, 1, 1))
        .gender(Gender.OTHER)
        .enabled(true)
        .preferredLocale(Locale.ENGLISH)
        .roleEntities(new ArrayList<>(List.of(role)))
        .build();
  }

  private Long persist(String email, RoleEntity role) {
    UserEntity testUserEntity = user(email, role);
    entityManager.persist(testUserEntity);
    return testUserEntity.getId();
  }

  private UserEntity form(Long id) {
    return userRepository.findAll().stream()
        .filter(user -> user.getId().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private long children(String table, Long id) {
    return new JdbcTemplate(dataSource)
        .queryForObject("select count(*) from " + table + " where user_id = ?", Long.class, id);
  }

  @Test
  void deletionCascadesAllTokenStatesAndQueuedMailWithoutRemovingOtherUsersOrRoles() {
    userService.deleteUser(form(targetId));

    assertFalse(userRepository.existsById(targetId));
    for (String table :
        List.of("verification_token", "password_reset_token", "email_queue", "users_roles")) {
      assertEquals(0, children(table, targetId));
      assertTrue(children(table, otherId) > 0);
    }
    assertTrue(userRepository.existsById(otherId));
    assertEquals(1, userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN).size());
  }

  @Test
  void outerRollbackRestoresUserAndEveryDependentRowAfterSuccessfulDeleteFlush() {
    transactionTemplate.executeWithoutResult(tx -> {
      userService.deleteUser(form(targetId));

      assertFalse(userRepository.existsById(targetId));
      tx.setRollbackOnly();
    });

    assertTrue(userRepository.existsById(targetId));
    assertEquals(TokenStatus.values().length, children("verification_token", targetId));
    assertEquals(TokenStatus.values().length, children("password_reset_token", targetId));
    assertEquals(1, children("email_queue", targetId));
  }

  @Test
  void lastAdminRejectionLeavesAllTokensAndMailUntouched() {
    UserEntity targetUser = form(adminId);

    assertThrows(IllegalStateException.class, () -> userService.deleteUser(targetUser));
    assertTrue(userRepository.existsById(adminId));
    assertEquals(TokenStatus.values().length, children("verification_token", adminId));
    assertEquals(TokenStatus.values().length, children("password_reset_token", adminId));
    assertEquals(1, children("email_queue", adminId));
  }

  @Test
  void emailPreflightCannotFlushAnUnguardedPendingDemotionOfTheLastSuperadmin() {
    assertThrows(
        IllegalStateException.class,
        () ->
            transactionTemplate.executeWithoutResult(tx -> {
              List<UserEntity> users = userRepository.findAll();
              users.stream()
                  .filter(user -> user.getId().equals(adminId))
                  .findFirst()
                  .orElseThrow()
                  .setEnabled(false);

              assertFalse(userService.emailExistsForOtherUser("unused@example.com", null));
              userService.save(
                  users.stream()
                      .filter(user -> user.getId().equals(targetId))
                      .findFirst()
                      .orElseThrow());
            }));
    assertEquals(
        List.of(adminId), userRepository.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void staleVersionDeleteRejectsWithoutLosingChildren() {
    UserEntity stale = form(targetId);
    UserEntity fresh = form(targetId);
    fresh.setFirstName("Changed");
    userService.updateAdminUser(fresh, fresh.getVersion(), "");

    assertThrows(OptimisticLockingFailureException.class, () -> userService.deleteUser(stale));
    assertEquals("Changed", form(targetId).getFirstName());
    assertEquals(TokenStatus.values().length, children("verification_token", targetId));
    assertEquals(TokenStatus.values().length, children("password_reset_token", targetId));
    assertEquals(1, children("email_queue", targetId));
  }

  @Test
  void canonicalIdentityIsSharedBySaveLookupSecurityLocaleAndUniqueness() {
    UserEntity edited = form(targetId);
    edited.setEmail("  MiXeD@Example.COM  ");
    userService.updateAdminUser(edited, edited.getVersion(), "");

    assertEquals("mixed@example.com", form(targetId).getEmail());
    assertEquals(targetId, userService.findByEmail(" MIXED@EXAMPLE.COM ").orElseThrow().getId());
    assertEquals(targetId, userRepository.findByEmail(" MIXED@EXAMPLE.COM ").orElseThrow().getId());
    assertEquals(
        "mixed@example.com",
        new CustomUserDetailsService(userRepository)
            .loadUserByUsername(" MIXED@EXAMPLE.COM ")
            .getUsername());
    transactionTemplate.executeWithoutResult(tx ->
        assertEquals(
            targetId,
            userRepository.findByEmailForUpdate(" MIXED@EXAMPLE.COM ").orElseThrow().getId()));
    userService.updateUserLocale(" MIXED@EXAMPLE.COM ", Locale.GERMAN);

    assertEquals(Locale.GERMAN, form(targetId).getPreferredLocale());
    assertTrue(userService.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", otherId));
    assertFalse(userService.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", targetId));
    assertTrue(userService.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", null));
    assertTrue(userRepository.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", otherId));
    assertFalse(userRepository.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", targetId));
  }

  @Test
  void directJpaBuilderAndUpdatesAlsoCanonicalizeAndDatabaseRejectsRawVariants() {
    Long id = transactionTemplate.execute(tx -> persist(" BUILDER@Example.COM ", ordinary));

    assertEquals("builder@example.com", form(id).getEmail());
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "update users set email = ? where id = ?", " BUILDER@Example.COM ", otherId));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "update users set email = ? where id = ?", "\tbuilder@example.com\t", otherId));
    assertThrows(
        DataIntegrityViolationException.class,
        () -> jdbcTemplate.update("update users set email = ? where id = ?", "", otherId));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "update users set email = ? where id = ?", "builder@example.com", otherId));
    assertEquals("other@example.com", form(otherId).getEmail());
    transactionTemplate.executeWithoutResult(tx -> {
      UserEntity managed = entityManager.find(UserEntity.class, id);
      ReflectionTestUtils.setField(managed, "email", " UPDATED@Example.COM ");
      managed.setFirstName("Updated through field access");
    });

    assertEquals("updated@example.com", form(id).getEmail());
  }

  @Test
  void actualEmailConstraintCanBeDistinguishedFromOtherDatabaseFailures() {
    UserEntity duplicate = user("TARGET@EXAMPLE.COM", ordinary);
    DataIntegrityViolationException failure =
        assertThrows(DataIntegrityViolationException.class, () -> userService.saveUser(duplicate));

    assertTrue(
        EmailConflict.isDuplicate(failure),
        () -> {
          Throwable cause = failure;
          while (cause.getCause() != null && !(cause instanceof ConstraintViolationException)) {
            cause = cause.getCause();
          }
          return cause instanceof ConstraintViolationException v
              ? "constraint=" + v.getConstraintName() + ", state=" + v.getSQLState()
              : cause.toString();
        });
    assertEquals(3, userRepository.count());
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    DataIntegrityViolationException invalid =
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                jdbcTemplate.update(
                    "update users set email = ? where id = ?", " INVALID@EXAMPLE.COM ", otherId));

    assertFalse(EmailConflict.isDuplicate(invalid));
    assertEquals("other@example.com", form(otherId).getEmail());
  }

  @Test
  void registrationCallbackHandlesARealCommittedDuplicateBetweenPreflightAndSave() {
    UI ui = new UI();
    VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
    Mockito.when(vaadinSession.hasLock()).thenReturn(true);
    Mockito.when(vaadinSession.getLocale()).thenReturn(Locale.ENGLISH);
    var registry = new ApplicationRouteRegistry(Mockito.mock(VaadinContext.class)) {};
    registry.setRoute("login", LoginView.class, List.of());
    TestService vaadin = Mockito.mock(RegistrationViewTest.TestService.class);
    Mockito.when(vaadin.getRouter()).thenReturn(new Router(registry));
    Mockito.when(vaadin.getRouteRegistry()).thenReturn(registry);
    Mockito.when(vaadinSession.getService()).thenReturn(vaadin);
    UI.setCurrent(ui);
    VaadinSession.setCurrent(vaadinSession);
    VaadinService.setCurrent(vaadin);
    try {
      UserService users = Mockito.mock(UserService.class);
      Mockito.when(users.findByEmail("registration@example.com"))
          .thenAnswer(call -> {
            assertTrue(userService.findByEmail("registration@example.com").isEmpty());
            // Deterministic competing commit after a successful preflight;
            // the following callback must encounter the actual DB constraint.
            userService.saveUser(user("registration@example.com", ordinary));
            return Optional.empty();
          });
      Mockito.doAnswer(
              call -> {
                userService.saveUser(call.getArgument(0));
                return null;
              })
          .when(users)
          .saveUser(ArgumentMatchers.any());
      RoleService roles = Mockito.mock(RoleService.class);
      Mockito.when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(ordinary));
      PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);
      Mockito.when(encoder.encode("Password!")).thenReturn("hash");
      VerificationService verification = Mockito.mock(VerificationService.class);
      RegistrationView registrationView =
          new RegistrationView(
              new MessageProperties(new CustomI18NProvider()),
              users,
              roles,
              encoder,
              new RegistrationService(users, verification, accountConfig()),
              Mockito.mock(SecurityService.class),
              Mockito.mock(HttpServletRequest.class),
              accountConfig(),
              Mockito.mock(de.derpeterson.app.security.ExternalLoginFlow.class));
      for (String name : List.of("firstNameField", "lastNameField")) {
        ((TextField) ReflectionTestUtils.getField(registrationView, name)).setValue("Test");
      }
      for (String name : List.of("emailField", "confirmEmailField")) {
        ((EmailField) ReflectionTestUtils.getField(registrationView, name))
            .setValue("registration@example.com");
      }
      for (String name : List.of("passwordField", "confirmPasswordField")) {
        ((PasswordField) ReflectionTestUtils.getField(registrationView, name))
            .setValue("Password!");
      }
      ((ComboBox) ReflectionTestUtils.getField(registrationView, "genderComboBox"))
          .setValue(Gender.OTHER);
      ((DatePicker) ReflectionTestUtils.getField(registrationView, "birthDatePicker"))
          .setValue(LocalDate.of(1990, 1, 1));
      ((Button) ReflectionTestUtils.getField(registrationView, "registrationButton")).click();

      assertTrue(
          ((EmailField) ReflectionTestUtils.getField(registrationView, "emailField")).isInvalid());
      assertTrue(
          ((EmailField) ReflectionTestUtils.getField(registrationView, "confirmEmailField"))
              .isInvalid());
      assertEquals(4, userRepository.count());
      Mockito.verifyNoInteractions(verification);
    } finally {
      NotificationHelper.getInstance().closeAndClearAllNotifications();
      UI.setCurrent(null);
      VaadinSession.setCurrent(null);
      VaadinService.setCurrent(null);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void concurrentCanonicalDuplicatesAreRejectedByDatabaseEvenWithoutServiceGuard(boolean update)
      throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first =
          executorService.submit(() -> race(targetId, " RACE@Example.COM ", update, barrier));
      Future<Boolean> second =
          executorService.submit(() -> race(otherId, "race@example.com", update, barrier));

      assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
    }
    assertEquals(
        1L,
        new JdbcTemplate(dataSource)
            .queryForObject(
                "select count(*) from users where email = 'race@example.com'", Long.class));
  }

  private boolean race(Long id, String email, boolean update, CyclicBarrier barrier) {
    try {
      transactionTemplate.executeWithoutResult(tx -> {
        UserEntity candidate =
            update ? entityManager.find(UserEntity.class, id) : user(email, ordinary);

        assertFalse(userService.emailExistsForOtherUser(email, update ? id : null));
        try {
          barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
          throw new AssertionError(exception);
        }
        // Deliberately bypass the role-row service lock: the UNIQUE
        // constraint must be authoritative for any writer/instance.
        candidate.setEmail(email);
        userRepository.saveAndFlush(candidate);
      });
      return true;
    } catch (DataIntegrityViolationException conflict) {
      assertTrue(EmailConflict.isDuplicate(conflict));
      return false;
    }
  }

  private static ConfigService accountConfig() {
    ConfigService configService = Mockito.mock(ConfigService.class);
    Mockito.when(configService.getString(ArgumentMatchers.any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    return configService;
  }
}
