package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;

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
import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.validation.EmailConflict;
import de.derpeterson.app.views.LoginView;
import de.derpeterson.app.views.RegistrationView;
import de.derpeterson.app.views.RegistrationViewTest;
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
import java.util.concurrent.Executors;
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
  @Autowired UserService service;
  @Autowired UserRepository repository;
  @Autowired TransactionTemplate transaction;
  @Autowired DataSource dataSource;
  @PersistenceContext EntityManager entityManager;
  private Long adminId;
  private Long targetId;
  private Long otherId;
  private RoleEntity ordinary;

  @BeforeEach
  void seed() {
    transaction.executeWithoutResult(
        tx -> {
          entityManager.createQuery("delete from VerificationTokenEntity").executeUpdate();
          entityManager.createQuery("delete from PasswordResetTokenEntity").executeUpdate();
          entityManager.createQuery("delete from EmailQueueEntity").executeUpdate();
          repository.deleteAll();
          repository.flush();
          entityManager.createQuery("delete from RoleEntity").executeUpdate();
          var admin = RoleEntity.builder().name(RoleType.ROLE_ADMIN).build();
          ordinary = RoleEntity.builder().name(RoleType.ROLE_USER).build();
          entityManager.persist(admin);
          entityManager.persist(ordinary);
          adminId = persist("admin@example.com", admin);
          targetId = persist("target@example.com", ordinary);
          otherId = persist("other@example.com", ordinary);
          for (Long id : List.of(targetId, otherId, adminId)) {
            UserEntity user = entityManager.find(UserEntity.class, id);
            for (TokenStatus status : TokenStatus.values()) {
              var verification =
                  new VerificationTokenEntity(
                      null,
                      "verification-" + id + status,
                      user,
                      LocalDateTime.now().plusHours(1),
                      status);
              var reset =
                  new PasswordResetTokenEntity(
                      null, "reset-" + id + status, user, LocalDateTime.now().plusHours(1), status);
              entityManager.persist(verification);
              entityManager.persist(reset);
            }
            var mail = new EmailQueueEntity();
            mail.setUserEntity(user);
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
    UserEntity user = user(email, role);
    entityManager.persist(user);
    return user.getId();
  }

  private UserEntity form(Long id) {
    return repository.findAll().stream()
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
    service.deleteUser(form(targetId));
    assertFalse(repository.existsById(targetId));
    for (String table :
        List.of("verification_token", "password_reset_token", "email_queue", "users_roles")) {
      assertEquals(0, children(table, targetId));
      assertTrue(children(table, otherId) > 0);
    }
    assertTrue(repository.existsById(otherId));
    assertEquals(1, repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN).size());
  }

  @Test
  void outerRollbackRestoresUserAndEveryDependentRowAfterSuccessfulDeleteFlush() {
    transaction.executeWithoutResult(
        tx -> {
          service.deleteUser(form(targetId));
          assertFalse(repository.existsById(targetId));
          tx.setRollbackOnly();
        });
    assertTrue(repository.existsById(targetId));
    assertEquals(TokenStatus.values().length, children("verification_token", targetId));
    assertEquals(TokenStatus.values().length, children("password_reset_token", targetId));
    assertEquals(1, children("email_queue", targetId));
  }

  @Test
  void lastAdminRejectionLeavesAllTokensAndMailUntouched() {
    assertThrows(IllegalStateException.class, () -> service.deleteUser(form(adminId)));
    assertTrue(repository.existsById(adminId));
    assertEquals(TokenStatus.values().length, children("verification_token", adminId));
    assertEquals(TokenStatus.values().length, children("password_reset_token", adminId));
    assertEquals(1, children("email_queue", adminId));
  }

  @Test
  void emailPreflightCannotFlushAnUnguardedPendingDemotionOfTheLastAdmin() {
    assertThrows(
        IllegalStateException.class,
        () ->
            transaction.executeWithoutResult(
                tx -> {
                  var users = repository.findAll();
                  users.stream()
                      .filter(user -> user.getId().equals(adminId))
                      .findFirst()
                      .orElseThrow()
                      .setEnabled(false);
                  assertFalse(service.emailExistsForOtherUser("unused@example.com", null));
                  service.save(
                      users.stream()
                          .filter(user -> user.getId().equals(targetId))
                          .findFirst()
                          .orElseThrow());
                }));
    assertEquals(List.of(adminId), repository.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN));
  }

  @Test
  void staleVersionDeleteRejectsWithoutLosingChildren() {
    UserEntity stale = form(targetId);
    UserEntity fresh = form(targetId);
    fresh.setFirstName("Changed");
    service.updateAdminUser(fresh, fresh.getVersion(), "");
    assertThrows(OptimisticLockingFailureException.class, () -> service.deleteUser(stale));
    assertEquals("Changed", form(targetId).getFirstName());
    assertEquals(TokenStatus.values().length, children("verification_token", targetId));
    assertEquals(TokenStatus.values().length, children("password_reset_token", targetId));
    assertEquals(1, children("email_queue", targetId));
  }

  @Test
  void canonicalIdentityIsSharedBySaveLookupSecurityLocaleAndUniqueness() {
    UserEntity edited = form(targetId);
    edited.setEmail("  MiXeD@Example.COM  ");
    service.updateAdminUser(edited, edited.getVersion(), "");
    assertEquals("mixed@example.com", form(targetId).getEmail());
    assertEquals(targetId, service.findByEmail(" MIXED@EXAMPLE.COM ").orElseThrow().getId());
    assertEquals(targetId, repository.findByEmail(" MIXED@EXAMPLE.COM ").orElseThrow().getId());
    assertEquals(
        "mixed@example.com",
        new CustomUserDetailsService(repository)
            .loadUserByUsername(" MIXED@EXAMPLE.COM ")
            .getUsername());
    transaction.executeWithoutResult(
        tx ->
            assertEquals(
                targetId,
                repository.findByEmailForUpdate(" MIXED@EXAMPLE.COM ").orElseThrow().getId()));
    service.updateUserLocale(" MIXED@EXAMPLE.COM ", Locale.GERMAN);
    assertEquals(Locale.GERMAN, form(targetId).getPreferredLocale());
    assertTrue(service.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", otherId));
    assertFalse(service.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", targetId));
    assertTrue(service.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", null));
    assertTrue(repository.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", otherId));
    assertFalse(repository.emailExistsForOtherUser(" MIXED@EXAMPLE.COM ", targetId));
  }

  @Test
  void directJpaBuilderAndUpdatesAlsoCanonicalizeAndDatabaseRejectsRawVariants() {
    Long id = transaction.execute(tx -> persist(" BUILDER@Example.COM ", ordinary));
    assertEquals("builder@example.com", form(id).getEmail());
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            new JdbcTemplate(dataSource)
                .update(
                    "update users set email = ? where id = ?", " BUILDER@Example.COM ", otherId));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            new JdbcTemplate(dataSource)
                .update(
                    "update users set email = ? where id = ?", "\tbuilder@example.com\t", otherId));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            new JdbcTemplate(dataSource)
                .update("update users set email = ? where id = ?", "", otherId));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            new JdbcTemplate(dataSource)
                .update("update users set email = ? where id = ?", "builder@example.com", otherId));
    assertEquals("other@example.com", form(otherId).getEmail());
    transaction.executeWithoutResult(
        tx -> {
          UserEntity managed = entityManager.find(UserEntity.class, id);
          ReflectionTestUtils.setField(managed, "email", " UPDATED@Example.COM ");
          managed.setFirstName("Updated through field access");
        });
    assertEquals("updated@example.com", form(id).getEmail());
  }

  @Test
  void actualEmailConstraintCanBeDistinguishedFromOtherDatabaseFailures() {
    var duplicate = user("TARGET@EXAMPLE.COM", ordinary);
    var failure =
        assertThrows(DataIntegrityViolationException.class, () -> service.saveUser(duplicate));
    assertTrue(
        EmailConflict.isDuplicate(failure),
        () -> {
          Throwable cause = failure;
          while (cause.getCause() != null && !(cause instanceof ConstraintViolationException))
            cause = cause.getCause();
          return cause instanceof ConstraintViolationException v
              ? "constraint=" + v.getConstraintName() + ", state=" + v.getSQLState()
              : cause.toString();
        });
    assertEquals(3, repository.count());
    var invalid =
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                new JdbcTemplate(dataSource)
                    .update(
                        "update users set email = ? where id = ?",
                        " INVALID@EXAMPLE.COM ",
                        otherId));
    assertFalse(EmailConflict.isDuplicate(invalid));
    assertEquals("other@example.com", form(otherId).getEmail());
  }

  @Test
  void registrationCallbackHandlesARealCommittedDuplicateBetweenPreflightAndSave() {
    var ui = new UI();
    var session = Mockito.mock(VaadinSession.class);
    Mockito.when(session.hasLock()).thenReturn(true);
    Mockito.when(session.getLocale()).thenReturn(Locale.ENGLISH);
    var registry = new ApplicationRouteRegistry(Mockito.mock(VaadinContext.class)) {};
    registry.setRoute("login", LoginView.class, List.of());
    var vaadin = Mockito.mock(RegistrationViewTest.TestService.class);
    Mockito.when(vaadin.getRouter()).thenReturn(new Router(registry));
    Mockito.when(vaadin.getRouteRegistry()).thenReturn(registry);
    Mockito.when(session.getService()).thenReturn(vaadin);
    UI.setCurrent(ui);
    VaadinSession.setCurrent(session);
    VaadinService.setCurrent(vaadin);
    try {
      var users = Mockito.mock(UserService.class);
      Mockito.when(users.findByEmail("registration@example.com"))
          .thenAnswer(
              call -> {
                assertTrue(service.findByEmail("registration@example.com").isEmpty());
                // Deterministic competing commit after a successful preflight;
                // the following callback must encounter the actual DB constraint.
                service.saveUser(user("registration@example.com", ordinary));
                return Optional.empty();
              });
      Mockito.doAnswer(
              call -> {
                service.saveUser(call.getArgument(0));
                return null;
              })
          .when(users)
          .saveUser(ArgumentMatchers.any());
      var roles = Mockito.mock(RoleService.class);
      Mockito.when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(ordinary));
      var encoder = Mockito.mock(PasswordEncoder.class);
      Mockito.when(encoder.encode("Password!")).thenReturn("hash");
      var verification = Mockito.mock(VerificationService.class);
      var view =
          new RegistrationView(
              new MessageProperties(new CustomI18NProvider()),
              users,
              roles,
              encoder,
              new RegistrationService(users, verification, accountConfig()),
              Mockito.mock(SecurityService.class),
              Mockito.mock(HttpServletRequest.class),
              accountConfig());
      for (String name : List.of("firstNameField", "lastNameField")) {
        ((TextField) ReflectionTestUtils.getField(view, name)).setValue("Test");
      }
      for (String name : List.of("emailField", "confirmEmailField")) {
        ((EmailField) ReflectionTestUtils.getField(view, name))
            .setValue("registration@example.com");
      }
      for (String name : List.of("passwordField", "confirmPasswordField")) {
        ((PasswordField) ReflectionTestUtils.getField(view, name)).setValue("Password!");
      }
      ((ComboBox) ReflectionTestUtils.getField(view, "genderComboBox")).setValue(Gender.OTHER);
      ((DatePicker) ReflectionTestUtils.getField(view, "birthDatePicker"))
          .setValue(LocalDate.of(1990, 1, 1));
      ((Button) ReflectionTestUtils.getField(view, "registrationButton")).click();
      assertTrue(((EmailField) ReflectionTestUtils.getField(view, "emailField")).isInvalid());
      assertTrue(
          ((EmailField) ReflectionTestUtils.getField(view, "confirmEmailField")).isInvalid());
      assertEquals(4, repository.count());
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
    var barrier = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> race(targetId, " RACE@Example.COM ", update, barrier));
      var second = executor.submit(() -> race(otherId, "race@example.com", update, barrier));
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
      transaction.executeWithoutResult(
          tx -> {
            UserEntity candidate =
                update ? entityManager.find(UserEntity.class, id) : user(email, ordinary);
            assertFalse(service.emailExistsForOtherUser(email, update ? id : null));
            try {
              barrier.await(5, TimeUnit.SECONDS);
            } catch (Exception exception) {
              throw new AssertionError(exception);
            }
            // Deliberately bypass the role-row service lock: the UNIQUE
            // constraint must be authoritative for any writer/instance.
            candidate.setEmail(email);
            repository.saveAndFlush(candidate);
          });
      return true;
    } catch (DataIntegrityViolationException conflict) {
      assertTrue(EmailConflict.isDuplicate(conflict));
      return false;
    }
  }

  private static ConfigService accountConfig() {
    ConfigService config = Mockito.mock(ConfigService.class);
    Mockito.when(config.getString(ArgumentMatchers.any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    return config;
  }
}
