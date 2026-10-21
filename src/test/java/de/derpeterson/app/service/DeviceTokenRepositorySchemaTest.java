package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.derpeterson.app.security.DeviceDescription;
import de.derpeterson.app.security.DeviceTokenRepository;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
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

  @Test
  void freshSchemaSupportsTheSameMaximumIdentityLengthAsTheUserColumn() throws Exception {
    DeviceTokenRepository repository = repository();
    assertEquals(
        255,
        jdbc.<Integer>query(
            "select username from persistent_logins where 1=0",
            result -> {
              return result.getMetaData().getPrecision(1);
            }));
    String identity =
        "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62);
    assertEquals(255, identity.length());
    repository.createNewToken(
        new PersistentRememberMeToken(identity, "max-series", "max-token", new Date()));
    assertEquals(identity, repository.getTokenForSeries("max-series").getUsername());
  }

  @Test
  void repeatedStartupOfTheCurrentSchemaKeepsTokensAndDeviceHandles() throws Exception {
    DeviceTokenRepository first = repository();
    String identity = "fixture@example.test";
    first.createNewToken(
        new PersistentRememberMeToken(identity, "fixture-series", "fixture-token", new Date()));
    first.describe(
        "fixture-series", identity, DeviceDescription.from("Windows Chrome/130"), Instant.now());
    String handle = first.saved(identity, 3600).getFirst().handle();
    DeviceTokenRepository second = repository();
    assertEquals(handle, second.saved(identity, 3600).getFirst().handle());
    assertEquals("fixture-token", second.getTokenForSeries("fixture-series").getTokenValue());
  }

  @Test
  void listingDoesNotInventMetadataForAnIncompleteToken() throws Exception {
    DeviceTokenRepository repository = repository();
    repository.createNewToken(
        new PersistentRememberMeToken(
            "fixture@example.test", "incomplete-series", "fixture-token", new Date()));
    assertTrue(repository.saved("fixture@example.test", 3600).isEmpty());
    assertEquals(0, jdbc.queryForObject("select count(*) from remembered_device", Integer.class));
  }
}
