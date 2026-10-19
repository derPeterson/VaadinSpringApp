package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.security.*;
import de.derpeterson.app.service.UserNotificationService.ReadFilter;
import de.derpeterson.app.websocket.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

/** Echte Policy, synchroner Pflichtmitteilungslistener und H2; keine Bestandsdaten. */
@SpringJUnitConfig(RoleMutationPersistenceTest.Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RoleMutationPersistenceTest {
  @Configuration
  @Import({
    UserServicePersistenceTest.Config.class,
    UserNotificationService.class,
    UserRoleChanges.class,
    UserNotificationBroadcaster.class,
    UserRoleBroadcaster.class
  })
  static class Config {
    @Bean
    @Primary
    AdminRolePolicy actualPolicy(UserRepository users) {
      return new AdminRolePolicy(users);
    }

    @Bean
    ConfigService config() {
      var config = mock(ConfigService.class);
      when(config.getString(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS)).thenReturn("180");
      when(config.getString(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS))
          .thenReturn("365");
      return config;
    }

    @Bean
    UserNotificationPolicy messagePolicy(ConfigService config) {
      return new UserNotificationPolicy(config);
    }
  }

  @Autowired UserService service;
  @Autowired UserRepository users;
  @Autowired UserNotificationRepository notifications;
  @Autowired UserNotificationRecipientRepository recipients;
  @Autowired UserNotificationService inbox;
  @Autowired UserRoleChanges changes;
  @Autowired UserRoleBroadcaster events;
  @Autowired SecurityService security;
  @Autowired TransactionTemplate tx;
  @PersistenceContext EntityManager em;
  UserEntity superadmin, admin, user, guest;

  @BeforeEach
  void seed() {
    SecurityContextHolder.clearContext();
    tx.executeWithoutResult(
        s -> {
          recipients.deleteAll();
          notifications.deleteAll();
          users.deleteAll();
          users.flush();
          em.createQuery("delete from RoleEntity").executeUpdate();
          for (var type : RoleType.values()) em.persist(RoleEntity.builder().name(type).build());
          superadmin = create("super", RoleType.ROLE_SUPERADMIN);
          admin = create("admin", RoleType.ROLE_ADMIN);
          user = create("user", RoleType.ROLE_USER);
          guest = create("guest", RoleType.ROLE_GUEST);
        });
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  UserEntity create(String name, RoleType type) {
    var u =
        UserEntity.builder()
            .email(name + "@example.test")
            .password("hash")
            .firstName(name)
            .lastName("Test")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender(Gender.OTHER)
            .enabled(true)
            .roleEntities(new ArrayList<>(List.of(role(type))))
            .build();
    return users.saveAndFlush(u);
  }

  RoleEntity role(RoleType type) {
    return em.createQuery("select r from RoleEntity r where r.name = :type", RoleEntity.class)
        .setParameter("type", type)
        .getSingleResult();
  }

  void login(UserEntity actor) {
    var p = new CustomUserDetailsService(users).loadUserByUsername(actor.getEmail());
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
  }

  UserEntity form(UserEntity u) {
    return users.findAll().stream()
        .filter(e -> e.getId().equals(u.getId()))
        .findFirst()
        .orElseThrow();
  }

  void assign(UserEntity target, RoleType... types) {
    var edit = form(target);
    var roles = tx.execute(s -> Arrays.stream(types).map(this::role).toList());
    edit.setRoleEntities(roles);
    service.updateAdminUser(edit, edit.getVersion(), "");
  }

  MockHttpSession session(UserEntity u) {
    var p = new CustomUserDetailsService(users).loadUserByUsername(u.getEmail());
    var c = SecurityContextHolder.createEmptyContext();
    c.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
    var http = new MockHttpSession();
    http.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, c);
    return http;
  }

  @ParameterizedTest
  @EnumSource(
      value = RoleType.class,
      names = {"ROLE_ADMIN", "ROLE_USER", "ROLE_GUEST"})
  void adminCanAssignAndRemoveEveryNonSuperadminRole(RoleType type) {
    login(admin);
    assign(user, type);
    assertTrue(form(user).hasRole(type));
    if (type != RoleType.ROLE_USER) assertEquals(1, notifications.count());
    else assertEquals(0, notifications.count());
    assign(user, RoleType.ROLE_GUEST);
    assertTrue(form(user).hasRole(RoleType.ROLE_GUEST));
  }

  @Test
  void ordinaryAdminCannotAssignSuperadminOrMutateExistingSuperadminAccount() {
    login(admin);
    assertThrows(AccessDeniedException.class, () -> assign(user, RoleType.ROLE_SUPERADMIN));
    assertThrows(AccessDeniedException.class, () -> assign(superadmin, RoleType.ROLE_ADMIN));
    var edited = form(superadmin);
    edited.setFirstName("Changed");
    assertThrows(
        AccessDeniedException.class,
        () -> service.updateAdminUser(edited, edited.getVersion(), "Changed-secret!"));
    assertThrows(AccessDeniedException.class, () -> service.deleteUser(form(superadmin)));
    edited.setEnabled(false);
    assertThrows(AccessDeniedException.class, () -> service.save(edited));
    assertEquals(0, notifications.count());
    assertTrue(form(superadmin).isEnabled());
    assertEquals("hash", form(superadmin).getPassword());
  }

  @Test
  void genericSaveCannotBypassSuperadminAssignmentGuard() {
    login(admin);
    var edit = form(user);
    edit.setRoleEntities(form(superadmin).getRoleEntities());
    assertThrows(AccessDeniedException.class, () -> service.save(edit));
    assertFalse(form(user).hasRole(RoleType.ROLE_SUPERADMIN));
  }

  @Test
  void superadminCanGrantAndRemoveSuperadminAndOnlyRecipientSeesEvent() {
    login(superadmin);
    assign(user, RoleType.ROLE_SUPERADMIN);
    assertTrue(form(user).hasRole(RoleType.ROLE_SUPERADMIN));
    assertEquals(UserNotificationType.ROLES_CHANGED, notifications.findAll().getFirst().getType());
    assertEquals(1, recipients.count());
    assertEquals(user.getId(), tx.execute(s -> recipients.findAll().getFirst().getUser().getId()));
    assign(user, RoleType.ROLE_USER);
    assertFalse(form(user).hasRole(RoleType.ROLE_SUPERADMIN));
    assertEquals(2, notifications.count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"anonymous", "user", "guest", "disabled-admin", "stale-admin"})
  void freshServiceAuthorizationRejectsNonAdminsAndLostPrivileges(String source) {
    switch (source) {
      case "user" -> login(user);
      case "guest" -> login(guest);
      case "disabled-admin", "stale-admin" -> {
        login(admin);
        tx.executeWithoutResult(
            s -> {
              var a = users.findById(admin.getId()).orElseThrow();
              if (source.equals("disabled-admin")) a.setEnabled(false);
              else a.setRoleEntities(new ArrayList<>());
            });
      }
    }
    assertThrows(AccessDeniedException.class, () -> assign(user, RoleType.ROLE_ADMIN));
    assertEquals(0, notifications.count());
  }

  @Test
  void creatingAccountsUsesTheSameRoleGuardAndDoesNotSendRoleChangeForInitialAssignment() {
    login(admin);
    var a =
        UserEntity.builder()
            .email("new@example.test")
            .firstName("New")
            .lastName("User")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender(Gender.OTHER)
            .enabled(true)
            .roleEntities(form(superadmin).getRoleEntities())
            .build();
    assertThrows(AccessDeniedException.class, () -> service.createAdminUser(a, "OnlyForTests!"));
    assertEquals(4, users.count());
    a.setRoleEntities(form(guest).getRoleEntities());
    service.createAdminUser(a, "OnlyForTests!");
    assertEquals(5, users.count());
    assertEquals(0, notifications.count());
  }

  @Test
  void unchangedSaveAndFailedTransactionCreateNoNoticeOrRolePush() {
    login(admin);
    var pushes = new AtomicInteger();
    var listener = events.register(id -> pushes.incrementAndGet());
    try {
      var edit = form(user);
      edit.setFirstName("Changed");
      service.updateAdminUser(edit, edit.getVersion(), "");
      assertEquals(0, notifications.count());
      assertEquals(0, pushes.get());
      tx.executeWithoutResult(
          s -> {
            assign(user, RoleType.ROLE_ADMIN);
            assertEquals(0, pushes.get());
            s.setRollbackOnly();
          });
      assertEquals(0, notifications.count());
      assertEquals(0, pushes.get());
      assertTrue(form(user).hasRole(RoleType.ROLE_USER));
      assign(user, RoleType.ROLE_ADMIN);
      assertEquals(1, pushes.get());
      assertEquals(1, notifications.count());
    } finally {
      listener.remove();
    }
  }

  @Test
  void failedMandatoryNoticeRollsBackRoleAssignmentAndPublishesNoRoleRefresh() {
    login(superadmin);
    var target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(changes);
    var failing = mock(UserNotificationService.class);
    doThrow(new IllegalStateException("message persistence unavailable"))
        .when(failing)
        .recordSecurityEvent(any(), any());
    var count = new AtomicInteger();
    var listener = events.register(id -> count.incrementAndGet());
    org.springframework.test.util.ReflectionTestUtils.setField(target, "notifications", failing);
    try {
      assertThrows(RuntimeException.class, () -> assign(user, RoleType.ROLE_SUPERADMIN));
      assertTrue(form(user).hasRole(RoleType.ROLE_USER));
      assertEquals(0, notifications.count());
      assertEquals(0, count.get());
    } finally {
      org.springframework.test.util.ReflectionTestUtils.setField(target, "notifications", inbox);
      listener.remove();
    }
  }

  @Test
  void roleChangeKeepsSessionIdentityAndMakesInboxUsableWithFreshRoles() {
    var http = session(user);
    var id = http.getId();
    login(admin);
    assign(user, RoleType.ROLE_ADMIN);
    assertEquals(id, http.getId());
    assertFalse(http.isInvalid());
    assertTrue(
        security.refreshSessionAuthentication(http).orElseThrow().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    assertEquals(1, inbox.inbox(http, ReadFilter.ALL, 0, 20).getTotalElements());
    assign(user, RoleType.ROLE_USER);
    assertFalse(http.isInvalid());
    assertFalse(
        security.refreshSessionAuthentication(http).orElseThrow().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"delete", "disable", "demote", "remove"})
  void lastSuperadminIsProtectedEvenThoughAnOrdinaryAdminRemains(String operation) {
    login(superadmin);
    var count = new AtomicInteger();
    var registration = events.register(id -> count.incrementAndGet());
    try {
      var edited = form(superadmin);
      assertFalse(service.canDeleteUser(edited));
      assertThrows(
          IllegalStateException.class,
          () -> {
            if (operation.equals("delete")) service.deleteUser(edited);
            else if (operation.equals("disable")) {
              edited.setEnabled(false);
              service.updateAdminUser(edited, edited.getVersion(), "");
            } else if (operation.equals("demote")) assign(superadmin, RoleType.ROLE_ADMIN);
            else assign(superadmin);
          });
      assertTrue(form(superadmin).isEnabled());
      assertTrue(form(superadmin).hasRole(RoleType.ROLE_SUPERADMIN));
      assertTrue(form(admin).hasRole(RoleType.ROLE_ADMIN));
      assertEquals(0, notifications.count());
      assertEquals(0, recipients.count());
      assertEquals(0, count.get());
    } finally {
      registration.remove();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"delete", "disable", "demote"})
  void superadminRemovalSucceedsWhenAnotherActiveSuperadminExists(String operation) {
    login(superadmin);
    assign(user, RoleType.ROLE_SUPERADMIN);
    login(form(user));
    if (operation.equals("delete")) service.deleteUser(form(superadmin));
    else if (operation.equals("disable")) {
      var edited = form(superadmin);
      edited.setEnabled(false);
      service.updateAdminUser(edited, edited.getVersion(), "");
    } else assign(superadmin, RoleType.ROLE_ADMIN);
    assertEquals(List.of(user.getId()), users.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN));
  }

  @ParameterizedTest
  @CsvSource({
    "delete,true",
    "disable,true",
    "demote,true",
    "delete,false",
    "disable,false",
    "demote,false"
  })
  void lastOrdinaryAdminHasNoMinimumRequirement(String operation, boolean hasSuperadmin) {
    if (hasSuperadmin) login(superadmin);
    else {
      // Anfangsbestand ohne Superadmin: normales Admin-Konto bekommt keinen Mindestschutz.
      tx.executeWithoutResult(
          s -> users.findById(superadmin.getId()).orElseThrow().setEnabled(false));
      login(admin);
    }
    assertTrue(service.canDeleteUser(form(admin)));
    assertFalse(service.wouldRemoveLastEnabledSuperadmin(admin.getId(), false, List.of()));
    if (operation.equals("delete")) service.deleteUser(form(admin));
    else if (operation.equals("disable")) {
      var edited = form(admin);
      edited.setEnabled(false);
      service.updateAdminUser(edited, edited.getVersion(), "");
    } else assign(admin, RoleType.ROLE_USER);
    assertTrue(users.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN).isEmpty());
    assertEquals(
        hasSuperadmin ? List.of(superadmin.getId()) : List.of(),
        users.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN));
  }
}
