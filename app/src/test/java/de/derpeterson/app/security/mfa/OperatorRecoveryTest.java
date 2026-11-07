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
  @Autowired DataSource source;
  @Autowired PlatformTransactionManager transactions;
  @Autowired TransactionTemplate transaction;
  @Autowired UserRepository users;
  @Autowired TwoFactorAuthorization authorization;
  @Autowired TwoFactorStore factors;
  @Autowired TwoFactorEnrollment enrollment;
  @Autowired TwoFactorStoreTest.TestClock clock;
  @PersistenceContext EntityManager entityManager;

  @Test
  void documentedRepairRevokesOnlyTheChosenAccountAndForcesProtectedReenrollment()
      throws Exception {
    clock.now.set(Instant.parse("2026-11-02T12:00:00Z"));
    UserEntity user =
        transaction.execute(
            status -> {
              RoleEntity role = new RoleEntity();
              role.setName(RoleType.ROLE_SUPERADMIN);
              entityManager.persist(role);
              return users.saveAndFlush(
                  UserEntity.builder()
                      .firstName("Operator")
                      .lastName("Fixture")
                      .email(UUID.randomUUID() + "@example.test")
                      .password("hash")
                      .enabled(true)
                      .roleEntities(List.of(role))
                      .build());
            });
    var request = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    TwoFactorEnrollment.Pending initial = enrollment.beginPassword(request, "correct");
    byte[] oldSeed = enrollment.pendingSecret(request, initial.id());
    UUID output =
        enrollment.complete(
            request, initial.id(), Totp.code(oldSeed, clock.instant().getEpochSecond() / 30));
    List<String> codes = enrollment.recoveryOutput(request, output);
    authorization.confirmAction(request, TwoFactorAction.SECURITY_CONFIGURATION, codes.getFirst());
    assertTrue(authorization.sessionConfirmed(request));
    new ResourceDatabasePopulator(new ClassPathResource("db/durable-sessions-h2.sql"))
        .execute(source);
    DeviceTokenRepository remembered = new DeviceTokenRepository(source, transactions);
    remembered.createNewToken(
        new PersistentRememberMeToken(
            user.getEmail(), "own-series", "own-token", Date.from(clock.instant())));
    remembered.describe(
        "own-series",
        user.getEmail(),
        DeviceDescription.from("Windows Chrome/130"),
        clock.instant());
    JdbcTemplate jdbc = new JdbcTemplate(source);
    String control = "control@example.test";
    remembered.createNewToken(
        new PersistentRememberMeToken(
            control, "control-series", "control-token", Date.from(clock.instant())));
    for (String identity : List.of(user.getEmail(), control)) {
      jdbc.update(
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
    NamedParameterJdbcTemplate parameters = new NamedParameterJdbcTemplate(source);
    transaction.executeWithoutResult(
        status -> {
          int updates = 0;
          for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
              int changed =
                  parameters.update(
                      statement.strip(),
                      Map.of(
                          "gepruefteBenutzerId",
                          user.getId(),
                          "bestaetigteKontoadresse",
                          user.getEmail()));
              if (updates++ < 2) {
                assertEquals(1, changed);
              }
            }
          }
          assertEquals(7, updates);
        });
    UserEntity repaired = users.findById(user.getId()).orElseThrow();
    assertNotEquals(user.getAccessVersion(), repaired.getAccessVersion());
    assertFalse(authorization.sessionConfirmed(request));
    Long userId = user.getId();
    String accessVersion = user.getAccessVersion();
    String recoveryCode = codes.get(1);
    assertThrows(
        BadCredentialsException.class, () -> factors.verify(userId, accessVersion, recoveryCode));
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> enrollment.recoveryOutput(request, output));
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from remembered_device where username=?",
            Integer.class,
            user.getEmail()));
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from persistent_logins where username=?",
            Integer.class,
            user.getEmail()));
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from spring_session where principal_name=?",
            Integer.class,
            user.getEmail()));
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from persistent_logins where username=?", Integer.class, control));
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from spring_session where principal_name=?", Integer.class, control));
    var next = TwoFactorAuthorizationTest.requestFor(user.getEmail());
    assertTrue(authorization.state(authorization.owner(next)).operatorEnrollment());
    assertFalse(authorization.sessionConfirmed(next));
    assertThrows(BadCredentialsException.class, () -> enrollment.beginPassword(next, "wrong"));
    TwoFactorEnrollment.Pending setup = enrollment.beginPassword(next, "correct");
    byte[] seed = enrollment.pendingSecret(next, setup.id());
    enrollment.complete(next, setup.id(), Totp.code(seed, clock.instant().getEpochSecond() / 30));
    assertTrue(authorization.sessionConfirmed(next));
    assertFalse(authorization.state(authorization.owner(next)).operatorEnrollment());
    String repairedAccessVersion = repaired.getAccessVersion();
    assertThrows(
        BadCredentialsException.class,
        () -> factors.verify(userId, repairedAccessVersion, recoveryCode));
  }
}
