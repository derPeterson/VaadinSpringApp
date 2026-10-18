package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.RoleRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.ConfigService;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class DatabaseInitializerRolesTest {
  @Test
  void fillsMissingRolesOnceEvenWithBootstrapDisabledAndDoesNotModifyAccounts() throws Exception {
    var roles = mock(RoleRepository.class);
    var users = mock(UserRepository.class);
    var stored = new EnumMap<RoleType, RoleEntity>(RoleType.class);
    var admin = RoleEntity.builder().id(12L).name(RoleType.ROLE_ADMIN).build();
    var user = RoleEntity.builder().id(13L).name(RoleType.ROLE_USER).build();
    stored.put(RoleType.ROLE_ADMIN, admin);
    stored.put(RoleType.ROLE_USER, user);
    when(roles.findByName(any()))
        .thenAnswer(call -> Optional.ofNullable(stored.get(call.getArgument(0))));
    when(roles.save(any()))
        .thenAnswer(
            call -> {
              RoleEntity role = call.getArgument(0);
              stored.put(role.getName(), role);
              return role;
            });
    var initializer =
        new DatabaseInitializer(
            users,
            roles,
            mock(ConfigService.class),
            mock(PasswordEncoder.class),
            mock(Environment.class),
            mock(RoleSchemaUpdater.class));
    initializer.initDatabase().run();
    initializer.initDatabase().run();
    assertEquals(Set.of(RoleType.values()), stored.keySet());
    assertSame(admin, stored.get(RoleType.ROLE_ADMIN));
    assertSame(user, stored.get(RoleType.ROLE_USER));
    verify(roles, times(2)).save(any());
    verifyNoInteractions(users);
  }

  @Test
  void startupCleansRetiredConfigurationBeforeSeedingAndPreservesStoredValues() throws Exception {
    var config = mock(ConfigService.class);
    when(config.exists(any())).thenReturn(true);
    var environment = mock(Environment.class);
    var initializer =
        new DatabaseInitializer(
            mock(UserRepository.class),
            mock(RoleRepository.class),
            config,
            mock(PasswordEncoder.class),
            environment,
            mock(RoleSchemaUpdater.class));

    initializer.initDatabase().run();
    initializer.initDatabase().run();

    var order = inOrder(config);
    for (int run = 0; run < 2; run++) {
      order.verify(config).removeRetiredEntries();
      for (var entry : de.derpeterson.app.model.enums.ConfigEntry.values()) {
        order.verify(config).exists(entry);
      }
    }
    order.verifyNoMoreInteractions();
    verifyNoInteractions(environment);
  }

  @Test
  void optionalBootstrapKeepsAdminAndUserAndNeverAssignsSuperadminOrGuest() throws Exception {
    var roles = mock(RoleRepository.class);
    var users = mock(UserRepository.class);
    when(roles.findByName(any()))
        .thenAnswer(call -> Optional.of(RoleEntity.builder().name(call.getArgument(0)).build()));
    when(users.findByEmail(anyString())).thenReturn(Optional.empty());
    var initializer =
        new DatabaseInitializer(
            users,
            roles,
            mock(ConfigService.class),
            mock(PasswordEncoder.class),
            mock(Environment.class),
            mock(RoleSchemaUpdater.class));
    ReflectionTestUtils.setField(initializer, "defaultAdminEnabled", true);
    ReflectionTestUtils.setField(
        initializer, "defaultAdminEmail", "isolated-bootstrap@example.test");
    ReflectionTestUtils.setField(initializer, "defaultAdminPassword", "OnlyATestPassword!");
    initializer.initDatabase().run();
    var created = ArgumentCaptor.forClass(UserEntity.class);
    verify(users).save(created.capture());
    assertEquals(
        Set.of(RoleType.ROLE_ADMIN, RoleType.ROLE_USER),
        created.getValue().getRoleEntities().stream()
            .map(RoleEntity::getName)
            .collect(Collectors.toSet()));
  }
}
