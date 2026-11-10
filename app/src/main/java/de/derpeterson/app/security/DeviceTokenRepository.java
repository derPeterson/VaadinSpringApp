package de.derpeterson.app.security;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Persistente Metadaten ergänzen Springs bestehende Tokenserien; keine zweite Authentifizierung.
 */
public class DeviceTokenRepository extends JdbcTokenRepositoryImpl {
  public record Saved(
      String handle,
      String series,
      String username,
      DeviceDescription device,
      Instant created,
      Instant lastUsed) {}

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;

  public DeviceTokenRepository(DataSource source, PlatformTransactionManager manager) {
    setDataSource(source);
    jdbcTemplate = new JdbcTemplate(source);
    transactionTemplate = new TransactionTemplate(manager);
    // Aktuelles Remember-me-Schema mit derselben maximalen Identitätslänge wie Benutzerkonten.
    jdbcTemplate.execute(
        "create table if not exists persistent_logins (username varchar(255) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)");
    jdbcTemplate.execute(
        "create table if not exists remembered_device (series varchar(64) primary key, handle varchar(36) not null unique, username varchar(255) not null, device_system varchar(32) not null, browser varchar(32) not null, mobile boolean not null, created_at timestamp not null)");
  }

  public <T> T atomic(Supplier<T> action) {
    return transactionTemplate.execute(status -> action.get());
  }

  public void describe(String series, String identity, DeviceDescription device, Instant created) {
    jdbcTemplate.update(
        "delete from remembered_device where series=? and username=?", series, identity);
    jdbcTemplate.update(
        "insert into remembered_device(series,handle,username,device_system,browser,mobile,created_at) values(?,?,?,?,?,?,?)",
        series,
        UUID.randomUUID().toString(),
        identity,
        device.system(),
        device.browser(),
        device.mobile(),
        Timestamp.from(created));
  }

  public boolean belongsTo(String series, String identity) {
    if (series == null) {
      return false;
    }
    PersistentRememberMeToken token = getTokenForSeries(series);
    return token != null && identity.equals(token.getUsername());
  }

  public void removeSeries(String identity, String series) {
    atomic(() -> {
      jdbcTemplate.update(
          "delete from persistent_logins where username=? and series=?", identity, series);
      jdbcTemplate.update(
          "delete from remembered_device where username=? and series=?", identity, series);
      return null;
    });
  }

  @Override
  public void removeUserTokens(String identity) {
    atomic(() -> {
      super.removeUserTokens(identity);
      jdbcTemplate.update("delete from remembered_device where username=?", identity);
      return null;
    });
  }

  public void deleteExpired(int duration) {
    if (duration < 1) {
      throw new IllegalArgumentException("Invalid saved-login duration");
    }
    atomic(() -> {
      jdbcTemplate.update(
          "delete from persistent_logins where last_used<?",
          Timestamp.from(Instant.now().minusSeconds(duration)));
      jdbcTemplate.update(
          "delete from remembered_device where not exists (select 1 from persistent_logins p where p.series=remembered_device.series and p.username=remembered_device.username)");
      return null;
    });
  }

  public List<Saved> saved(String identity, int duration) {
    Instant cutoff = Instant.now().minusSeconds(duration);
    return atomic(() -> {
      jdbcTemplate.update(
          "delete from persistent_logins where username=? and last_used<?",
          identity,
          Timestamp.from(cutoff));
      jdbcTemplate.update(
          "delete from remembered_device where username=? and not exists (select 1 from persistent_logins p where p.series=remembered_device.series and p.username=remembered_device.username)",
          identity);
      return jdbcTemplate.query(
          "select d.handle,d.series,d.username,d.device_system,d.browser,d.mobile,d.created_at,p.last_used from remembered_device d join persistent_logins p on p.series=d.series and p.username=d.username where d.username=? order by p.last_used desc",
          (rs, row) ->
              new Saved(
                  rs.getString(1),
                  rs.getString(2),
                  rs.getString(3),
                  new DeviceDescription(rs.getString(4), rs.getString(5), rs.getBoolean(6)),
                  rs.getTimestamp(7).toInstant(),
                  rs.getTimestamp(8).toInstant()),
          identity);
    });
  }
}
