package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.DeviceTokenRepository;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

class DeviceTokenRepositorySchemaTest {
  private DriverManagerDataSource source;
  private JdbcTemplate jdbc;

  @BeforeEach
  void setup() {
    source =
        new DriverManagerDataSource(
            "jdbc:h2:mem:device-schema-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    jdbc = new JdbcTemplate(source);
  }

  private DeviceTokenRepository repository() throws Exception {
    DeviceTokenRepository repository =
        new DeviceTokenRepository(source, new DataSourceTransactionManager(source));
    repository.afterPropertiesSet();
    return repository;
  }

  private int usernameLength() {
    return jdbc.query(
        "select username from persistent_logins where 1=0",
        result -> {
          return result.getMetaData().getPrecision(1);
        });
  }

  @Test
  void longValidIdentityWorksWithTheOriginalSpringSchemaWithoutLosingOldTokens() throws Exception {
    jdbc.execute(JdbcTokenRepositoryImpl.CREATE_TABLE_SQL);
    JdbcTokenRepositoryImpl original = new JdbcTokenRepositoryImpl();
    original.setDataSource(source);
    original.createNewToken(
        new PersistentRememberMeToken("old@example.test", "old-series", "old-token", new Date()));
    DeviceTokenRepository repository = repository();
    String identity = "a".repeat(64) + "@example.test";
    assertTrue(identity.length() > 64);
    repository.createNewToken(
        new PersistentRememberMeToken(identity, "long-series", "long-token", new Date()));
    assertEquals(identity, repository.getTokenForSeries("long-series").getUsername());
    assertEquals("old-token", repository.getTokenForSeries("old-series").getTokenValue());
    assertTrue(usernameLength() >= 255);
  }

  @Test
  void freshSchemaSupportsTheSameMaximumIdentityLengthAsTheUserColumn() throws Exception {
    DeviceTokenRepository repository = repository();
    assertEquals(255, usernameLength());
    String identity =
        "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62);
    assertEquals(255, identity.length());
    repository.createNewToken(
        new PersistentRememberMeToken(identity, "max-series", "max-token", new Date()));
    assertEquals(identity, repository.getTokenForSeries("max-series").getUsername());
  }

  @Test
  void anAlreadyWiderColumnIsNotShrunkAndRepeatedStartupPreservesMetadata() throws Exception {
    jdbc.execute(
        "create table persistent_logins (username varchar(500) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)");
    DeviceTokenRepository first = repository();
    first.createNewToken(
        new PersistentRememberMeToken("old@example.test", "old-series", "old-token", new Date()));
    first.describe(
        "old-series",
        "old@example.test",
        DeviceDescription.from("Windows Chrome/130"),
        java.time.Instant.now());
    String handle = first.saved("old@example.test", 3600).getFirst().handle();
    DeviceTokenRepository second = repository();
    assertEquals(500, usernameLength());
    assertEquals(handle, second.saved("old@example.test", 3600).getFirst().handle());
    assertEquals("old-token", second.getTokenForSeries("old-series").getTokenValue());
  }
}
