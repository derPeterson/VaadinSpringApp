package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.config.BootstrapAccountsProperties.Account;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
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
import org.mockito.InOrder;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class DatabaseInitializerRolesTest {
  @Test
  void fillsMissingRolesOnceEvenWithBootstrapDisabledAndDoesNotModifyAccounts() throws Exception {
    RoleRepository roles = mock(RoleRepository.class);
    UserRepository users = mock(UserRepository.class);
    EnumMap<RoleType, RoleEntity> stored = new EnumMap<RoleType, RoleEntity>(RoleType.class);
    RoleEntity admin = RoleEntity.builder().id(12L).name(RoleType.ROLE_ADMIN).build();
    RoleEntity user = RoleEntity.builder().id(13L).name(RoleType.ROLE_USER).build();
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
    DatabaseInitializer initializer =
        new DatabaseInitializer(
            users,
            roles,
            mock(ConfigService.class),
            mock(PasswordEncoder.class),
            mock(Environment.class),
            new BootstrapAccountsProperties(),
            mock(org.springframework.transaction.PlatformTransactionManager.class));
    initializer.initDatabase().run();
    initializer.initDatabase().run();
    assertEquals(Set.of(RoleType.values()), stored.keySet());
    assertSame(admin, stored.get(RoleType.ROLE_ADMIN));
    assertSame(user, stored.get(RoleType.ROLE_USER));
    verify(roles, times(2)).save(any());
    verifyNoInteractions(users);
  }

  @Test
  void startupKeepsConfiguredValuesAndSeedsOnlyMissingKeys() throws Exception {
    ConfigService config = mock(ConfigService.class);
    when(config.exists(any())).thenReturn(true);
    Environment environment = mock(Environment.class);
    DatabaseInitializer initializer =
        new DatabaseInitializer(
            mock(UserRepository.class),
            mock(RoleRepository.class),
            config,
            mock(PasswordEncoder.class),
            environment,
            new BootstrapAccountsProperties(),
            mock(org.springframework.transaction.PlatformTransactionManager.class));

    initializer.initDatabase().run();
    initializer.initDatabase().run();

    InOrder order = inOrder(config);
    for (int run = 0; run < 2; run++) {
      for (ConfigEntry entry : de.derpeterson.app.model.enums.ConfigEntry.values()) {
        order.verify(config).exists(entry);
      }
    }
    order.verifyNoMoreInteractions();
    verifyNoInteractions(environment);
  }

  @Test
  void optionalBootstrapCreatesOnlyTheExplicitSuperadminRole() throws Exception {
    RoleRepository roles = mock(RoleRepository.class);
    UserRepository users = mock(UserRepository.class);
    when(roles.findByName(any()))
        .thenAnswer(call -> Optional.of(RoleEntity.builder().name(call.getArgument(0)).build()));
    when(users.findByEmail(anyString())).thenReturn(Optional.empty());
    DatabaseInitializer initializer =
        new DatabaseInitializer(
            users,
            roles,
            mock(ConfigService.class),
            mock(PasswordEncoder.class),
            mock(Environment.class),
            new BootstrapAccountsProperties(),
            mock(org.springframework.transaction.PlatformTransactionManager.class));
    BootstrapAccountsProperties bootstrap = new BootstrapAccountsProperties();
    bootstrap.setEnabled(true);
    Account account = new BootstrapAccountsProperties.Account();
    account.setEmail("isolated-bootstrap@example.test");
    account.setPassword("OnlyATestPassword!");
    bootstrap.setSuperadmins(java.util.List.of(account));
    ReflectionTestUtils.setField(initializer, "bootstrap", bootstrap);
    initializer.initDatabase().run();
    ArgumentCaptor<UserEntity> created = ArgumentCaptor.forClass(UserEntity.class);
    verify(users).save(created.capture());
    assertEquals(
        Set.of(RoleType.ROLE_SUPERADMIN),
        created.getValue().getRoleEntities().stream()
            .map(RoleEntity::getName)
            .collect(Collectors.toSet()));
  }
}
