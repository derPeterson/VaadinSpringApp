package de.derpeterson.app.security.mfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.DeviceTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Führt ausschließlich auf einer eigenen H2-Datenbank die tatsächlich dokumentierten SQLs aus. */
@SpringJUnitConfig({
  TwoFactorStoreTest.Config.class,
  TwoFactorAuthorizationTest.Config.class,
  TwoFactorEnrollmentTest.Config.class
})
class OperatorRecoveryTest {
  @Autowired DataSource testDataSource;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired UserRepository userRepository;
  @Autowired TwoFactorAuthorization twoFactorAuthorization;
  @Autowired TwoFactorStore twoFactorStore;
  @Autowired TwoFactorEnrollment twoFactorEnrollment;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @PersistenceContext EntityManager entityManager;

  @Test
  void documentedRepairRevokesOnlyTheChosenAccountAndForcesProtectedReenrollment()
      throws Exception {
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    UserEntity testUserEntity =
        transactionTemplate.execute(status -> {
          RoleEntity role = new RoleEntity();
          role.setName(RoleType.ROLE_SUPERADMIN);
          entityManager.persist(role);
          return userRepository.saveAndFlush(
              UserEntity.builder()
                  .firstName("Operator")
                  .lastName("Fixture")
                  .email(UUID.randomUUID() + "@example.test")
                  .password("hash")
                  .enabled(true)
                  .roleEntities(List.of(role))
                  .build());
        });
    var request = TwoFactorAuthorizationTest.requestFor(testUserEntity.getEmail());
    TwoFactorEnrollment.Pending initial = twoFactorEnrollment.beginPassword(request, "correct");
    byte[] oldSeed = twoFactorEnrollment.pendingSecret(request, initial.id());
    UUID output =
        twoFactorEnrollment.complete(
            request, initial.id(), Totp.code(oldSeed, clock.instant().getEpochSecond() / 30));
    List<String> codes = twoFactorEnrollment.recoveryOutput(request, output);
    twoFactorAuthorization.confirmAction(
        request, TwoFactorAction.SECURITY_CONFIGURATION, codes.getFirst());

    assertTrue(twoFactorAuthorization.sessionConfirmed(request));
    new ResourceDatabasePopulator(new ClassPathResource("db/durable-sessions-h2.sql"))
        .execute(testDataSource);
    DeviceTokenRepository remembered =
        new DeviceTokenRepository(testDataSource, transactionManager);
    remembered.createNewToken(
        new PersistentRememberMeToken(
            testUserEntity.getEmail(), "own-series", "own-token", Date.from(clock.instant())));
    remembered.describe(
        "own-series",
        testUserEntity.getEmail(),
        DeviceDescription.from("Windows Chrome/130"),
        clock.instant());
    JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
    String control = "control@example.test";
    remembered.createNewToken(
        new PersistentRememberMeToken(
            control, "control-series", "control-token", Date.from(clock.instant())));
    for (String identity : List.of(testUserEntity.getEmail(), control)) {
      jdbcTemplate.update(
          "insert into spring_session(primary_id,session_id,creation_time,last_access_time,max_inactive_interval,expiry_time,principal_name) values(?,?,?,?,?,?,?)",
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          0,
          0,
          1800,
          Long.MAX_VALUE,
          identity);
    }
    Path instructions = Path.of("..", "docs", "betrieb", "2fa-notfall.md");
    String markdown = Files.readString(instructions);
    String sql = markdown.split("```sql", 2)[1].split("```", 2)[0];
    NamedParameterJdbcTemplate parameters = new NamedParameterJdbcTemplate(testDataSource);
    transactionTemplate.executeWithoutResult(status -> {
      int updates = 0;
      for (String statement : sql.split(";")) {
        if (!statement.isBlank()) {
          int changed =
              parameters.update(
                  statement.strip(),
                  Map.of(
                      "gepruefteBenutzerId",
                      testUserEntity.getId(),
                      "bestaetigteKontoadresse",
                      testUserEntity.getEmail()));
          if (updates++ < 2) {
            assertEquals(1, changed);
          }
        }
      }
      assertEquals(7, updates);
    });
    UserEntity repaired = userRepository.findById(testUserEntity.getId()).orElseThrow();

    assertNotEquals(testUserEntity.getAccessVersion(), repaired.getAccessVersion());
    assertFalse(twoFactorAuthorization.sessionConfirmed(request));
    Long userId = testUserEntity.getId();
    String accessVersion = testUserEntity.getAccessVersion();
    String recoveryCode = codes.get(1);

    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, accessVersion, recoveryCode));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> twoFactorEnrollment.recoveryOutput(request, output));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from remembered_device where username=?",
            Integer.class,
            testUserEntity.getEmail()));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from persistent_logins where username=?",
            Integer.class,
            testUserEntity.getEmail()));
    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from spring_session where principal_name=?",
            Integer.class,
            testUserEntity.getEmail()));
    assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "select count(*) from persistent_logins where username=?", Integer.class, control));
    assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "select count(*) from spring_session where principal_name=?", Integer.class, control));
    var next = TwoFactorAuthorizationTest.requestFor(testUserEntity.getEmail());

    assertTrue(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(next)).operatorEnrollment());
    assertFalse(twoFactorAuthorization.sessionConfirmed(next));
    assertThrows(
        BadCredentialsException.class, () -> twoFactorEnrollment.beginPassword(next, "wrong"));
    TwoFactorEnrollment.Pending setup = twoFactorEnrollment.beginPassword(next, "correct");
    byte[] seed = twoFactorEnrollment.pendingSecret(next, setup.id());
    twoFactorEnrollment.complete(
        next, setup.id(), Totp.code(seed, clock.instant().getEpochSecond() / 30));

    assertTrue(twoFactorAuthorization.sessionConfirmed(next));
    assertFalse(
        twoFactorAuthorization.state(twoFactorAuthorization.owner(next)).operatorEnrollment());
    String repairedAccessVersion = repaired.getAccessVersion();

    assertThrows(
        BadCredentialsException.class,
        () -> twoFactorStore.verify(userId, repairedAccessVersion, recoveryCode));
  }
}
