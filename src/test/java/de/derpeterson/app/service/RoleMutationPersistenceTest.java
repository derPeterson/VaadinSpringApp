package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.model.enums.UserNotificationType;
import de.derpeterson.app.repository.UserNotificationRecipientRepository;
import de.derpeterson.app.repository.UserNotificationRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.AdminRolePolicy;
import de.derpeterson.app.security.CustomUserDetailsService;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.UserNotificationService.ReadFilter;
import de.derpeterson.app.websocket.UserNotificationBroadcaster;
import de.derpeterson.app.websocket.UserRoleBroadcaster;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
      ConfigService config = mock(ConfigService.class);
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
          for (RoleType type : RoleType.values()) {
            em.persist(RoleEntity.builder().name(type).build());
          }
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
    UserEntity u =
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
    UserDetails p = new CustomUserDetailsService(users).loadUserByUsername(actor.getEmail());
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
    UserEntity edit = form(target);
    List<RoleEntity> roles = tx.execute(s -> Arrays.stream(types).map(this::role).toList());
    edit.setRoleEntities(roles);
    service.updateAdminUser(edit, edit.getVersion(), "");
  }

  MockHttpSession session(UserEntity u) {
    UserDetails p = new CustomUserDetailsService(users).loadUserByUsername(u.getEmail());
    SecurityContext c = SecurityContextHolder.createEmptyContext();
    c.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(p, null, p.getAuthorities()));
    MockHttpSession http = new MockHttpSession();
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
    if (type != RoleType.ROLE_USER) {
      assertEquals(1, notifications.count());
    } else {
      assertEquals(0, notifications.count());
    }
    assign(user, RoleType.ROLE_GUEST);
    assertTrue(form(user).hasRole(RoleType.ROLE_GUEST));
  }

  @Test
  void ordinaryAdminCannotAssignSuperadminOrMutateExistingSuperadminAccount() {
    login(admin);
    assertThrows(AccessDeniedException.class, () -> assign(user, RoleType.ROLE_SUPERADMIN));
    assertThrows(AccessDeniedException.class, () -> assign(superadmin, RoleType.ROLE_ADMIN));
    UserEntity edited = form(superadmin);
    edited.setFirstName("Changed");
    Long editedVersion = edited.getVersion();
    assertThrows(
        AccessDeniedException.class,
        () -> service.updateAdminUser(edited, editedVersion, "Changed-secret!"));
    UserEntity targetUser = form(superadmin);
    assertThrows(AccessDeniedException.class, () -> service.deleteUser(targetUser));
    edited.setEnabled(false);
    assertThrows(AccessDeniedException.class, () -> service.save(edited));
    assertEquals(0, notifications.count());
    assertTrue(form(superadmin).isEnabled());
    assertEquals("hash", form(superadmin).getPassword());
  }

  @Test
  void genericSaveCannotBypassSuperadminAssignmentGuard() {
    login(admin);
    UserEntity edit = form(user);
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
      case "anonymous" -> {
        // Die Setup-Fixture besitzt noch keine Authentifizierung.
      }
      case "user" -> login(user);
      case "guest" -> login(guest);
      case "disabled-admin", "stale-admin" -> {
        login(admin);
        tx.executeWithoutResult(
            s -> {
              UserEntity a = users.findById(admin.getId()).orElseThrow();
              if (source.equals("disabled-admin")) {
                a.setEnabled(false);
              } else {
                a.setRoleEntities(new ArrayList<>());
              }
            });
      }
      default -> throw new IllegalArgumentException("Unknown authorization fixture: " + source);
    }
    assertThrows(AccessDeniedException.class, () -> assign(user, RoleType.ROLE_ADMIN));
    assertEquals(0, notifications.count());
  }

  @Test
  void creatingAccountsUsesTheSameRoleGuardAndDoesNotSendRoleChangeForInitialAssignment() {
    login(admin);
    UserEntity a =
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
    AtomicInteger pushes = new AtomicInteger();
    Registration listener = events.register(id -> pushes.incrementAndGet());
    try {
      UserEntity edit = form(user);
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
    Object target = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(changes);
    UserNotificationService failing = mock(UserNotificationService.class);
    doThrow(new IllegalStateException("message persistence unavailable"))
        .when(failing)
        .recordSecurityEvent(any(), any());
    AtomicInteger count = new AtomicInteger();
    Registration listener = events.register(id -> count.incrementAndGet());
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
    MockHttpSession http = session(user);
    String id = http.getId();
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
    AtomicInteger count = new AtomicInteger();
    Registration registration = events.register(id -> count.incrementAndGet());
    try {
      UserEntity edited = form(superadmin);
      assertFalse(service.canDeleteUser(edited));
      if (operation.equals("delete")) {
        assertThrows(IllegalStateException.class, () -> service.deleteUser(edited));
      } else if (operation.equals("disable")) {
        edited.setEnabled(false);
        Long editedVersion = edited.getVersion();
        assertThrows(
            IllegalStateException.class, () -> service.updateAdminUser(edited, editedVersion, ""));
      } else if (operation.equals("demote")) {
        assertThrows(IllegalStateException.class, () -> assign(superadmin, RoleType.ROLE_ADMIN));
      } else {
        assertThrows(IllegalStateException.class, () -> assign(superadmin));
      }
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
    if (operation.equals("delete")) {
      service.deleteUser(form(superadmin));
    } else if (operation.equals("disable")) {
      UserEntity edited = form(superadmin);
      edited.setEnabled(false);
      service.updateAdminUser(edited, edited.getVersion(), "");
    } else {
      assign(superadmin, RoleType.ROLE_ADMIN);
    }
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
    if (hasSuperadmin) {
      login(superadmin);
    } else {
      // Anfangsbestand ohne Superadmin: normales Admin-Konto bekommt keinen Mindestschutz.
      tx.executeWithoutResult(
          s -> users.findById(superadmin.getId()).orElseThrow().setEnabled(false));
      login(admin);
    }
    assertTrue(service.canDeleteUser(form(admin)));
    assertFalse(service.wouldRemoveLastEnabledSuperadmin(admin.getId(), false, List.of()));
    if (operation.equals("delete")) {
      service.deleteUser(form(admin));
    } else if (operation.equals("disable")) {
      UserEntity edited = form(admin);
      edited.setEnabled(false);
      service.updateAdminUser(edited, edited.getVersion(), "");
    } else {
      assign(admin, RoleType.ROLE_USER);
    }
    assertTrue(users.findEnabledUserIdsByRole(RoleType.ROLE_ADMIN).isEmpty());
    assertEquals(
        hasSuperadmin ? List.of(superadmin.getId()) : List.of(),
        users.findEnabledUserIdsByRole(RoleType.ROLE_SUPERADMIN));
  }
}
