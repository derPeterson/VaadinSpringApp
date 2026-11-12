package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.scheduler.UserStatusScheduler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

/** Real Hibernate mapping, database predicates and locked status updates in disposable H2 only. */
@SpringJUnitConfig(UserServicePersistenceTest.Config.class)
class UserActivityTimePersistenceTest {
  @Autowired private UserRepository userRepository;
  @Autowired private UserService userService;
  @Autowired private TransactionTemplate transactionTemplate;
  @PersistenceContext private EntityManager entityManager;

  @BeforeEach
  void clearOwnFixture() {
    transactionTemplate.executeWithoutResult(tx -> {
      userRepository.deleteAll();
      userRepository.flush();
    });
  }

  private UserEntity user(String email, Instant activity) {
    return UserEntity.builder()
        .firstName("Activity")
        .lastName("Test")
        .email(email)
        .password("test-hash")
        .gender(Gender.OTHER)
        .birthDate(LocalDate.of(1990, 1, 1))
        .status(UserStatus.AVAILABLE)
        .lastActivity(activity)
        .roleEntities(List.of())
        .build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTC", "Europe/Berlin", "America/New_York"})
  void repeatedLocalHoursRemainDifferentAcrossDatabaseSessionZones(String zone) {
    Instant first = Instant.parse("2026-10-25T00:30:00Z");
    Instant second = Instant.parse("2026-10-25T01:30:00Z");
    Instant cutoff = Instant.parse("2026-10-25T01:00:00Z");
    transactionTemplate.executeWithoutResult(tx -> {
      entityManager
          .unwrap(Session.class)
          .doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
              // The zone is a fixed test parameter, never application input.
              statement.execute("SET TIME ZONE '" + zone + "'");
            }
          });
      UserEntity earlier = userRepository.saveAndFlush(user("earlier@example.test", first));
      UserEntity later = userRepository.saveAndFlush(user("later@example.test", second));
      Long firstId = earlier.getId();
      Long secondId = later.getId();
      entityManager.clear();

      assertEquals(first, userRepository.findById(firstId).orElseThrow().getLastActivity());
      assertEquals(second, userRepository.findById(secondId).orElseThrow().getLastActivity());
      assertEquals(
          List.of(firstId),
          userRepository.findByLastActivityBeforeAndStatus(cutoff, UserStatus.AVAILABLE).stream()
              .map(UserEntity::getId)
              .toList());
      assertEquals(
          List.of(secondId),
          userRepository
              .findByLastActivityAfterAndStatusAndStatusManuallySetFalse(
                  cutoff, UserStatus.AVAILABLE)
              .stream()
              .map(UserEntity::getId)
              .toList());
      entityManager
          .unwrap(Session.class)
          .doWork(connection -> {
            try (ResultSet columns =
                connection.getMetaData().getColumns(null, null, "USERS", "LAST_ACTIVITY")) {
              assertTrue(columns.next());
              assertEquals(java.sql.Types.TIMESTAMP_WITH_TIMEZONE, columns.getInt("DATA_TYPE"));
            }
            try (PreparedStatement statement =
                connection.prepareStatement("select last_activity from users where id = ?")) {
              statement.setLong(1, secondId);
              try (ResultSet values = statement.executeQuery()) {
                assertTrue(values.next());
                assertEquals(second, values.getObject(1, OffsetDateTime.class).toInstant());
              }
            }
          });
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"2026-03-29T01:01:00Z", "2026-10-25T01:01:00Z"})
  void schedulerUsesElapsedTimeoutAndRecoveryAcrossBothClockChanges(String value) {
    Instant now = Instant.parse(value);
    Long id =
        transactionTemplate.execute(tx ->
            userRepository
                .saveAndFlush(user("status-time@example.test", now.minusSeconds(120)))
                .getId());
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(ConfigEntry.USER_AUTO_ABSENT_TIMEOUT)).thenReturn("PT5M");
    Clock fixed = Clock.fixed(now, ZoneId.of("America/New_York"));
    UserStatusScheduler scheduler =
        new UserStatusScheduler(userRepository, userService, configService, fixed);
    scheduler.checkInactiveAvailableUsers();

    assertEquals(UserStatus.AVAILABLE, userRepository.findById(id).orElseThrow().getStatus());
    // Equality retains the original strict timeout contract.
    new UserStatusScheduler(
            userRepository,
            userService,
            configService,
            Clock.offset(fixed, java.time.Duration.ofMinutes(3)))
        .checkInactiveAvailableUsers();

    assertEquals(UserStatus.AVAILABLE, userRepository.findById(id).orElseThrow().getStatus());
    new UserStatusScheduler(
            userRepository,
            userService,
            configService,
            Clock.offset(fixed, java.time.Duration.ofSeconds(181)))
        .checkInactiveAvailableUsers();

    assertEquals(UserStatus.ABSENT, userRepository.findById(id).orElseThrow().getStatus());
    transactionTemplate.executeWithoutResult(
        tx -> userRepository.updateLastActivity(id, now.minusSeconds(1)));
    scheduler.checkRecentlyActiveAbsentUsers();

    assertEquals(UserStatus.AVAILABLE, userRepository.findById(id).orElseThrow().getStatus());
  }
}
