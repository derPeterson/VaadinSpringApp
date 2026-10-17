package de.derpeterson.app.security;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.authentication.rememberme.*;
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

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public DeviceTokenRepository(DataSource source, PlatformTransactionManager manager) {
    setDataSource(source);
    jdbc = new JdbcTemplate(source);
    transaction = new TransactionTemplate(manager);
    // Springs Komfortschema begrenzt usernames auf 64 Zeichen, unsere Kontoadresse auf 255.
    // Nur verbreitern: vorhandene Token und eventuell bereits größere Spalten bleiben erhalten.
    jdbc.execute(
        "create table if not exists persistent_logins (username varchar(255) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)");
    int identityLength =
        jdbc.query(
            "select username from persistent_logins where 1=0",
            result -> {
              return result.getMetaData().getPrecision(1);
            });
    if (identityLength < 255)
      jdbc.execute("alter table persistent_logins alter column username varchar(255) not null");
    jdbc.execute(
        "create table if not exists remembered_device (series varchar(64) primary key, handle varchar(36) not null unique, username varchar(255) not null, device_system varchar(32) not null, browser varchar(32) not null, mobile boolean not null, created_at timestamp not null)");
  }

  public <T> T atomic(Supplier<T> action) {
    return transaction.execute(status -> action.get());
  }

  public void describe(String series, String identity, DeviceDescription device, Instant created) {
    jdbc.update("delete from remembered_device where series=? and username=?", series, identity);
    jdbc.update(
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
    if (series == null) return false;
    var token = getTokenForSeries(series);
    return token != null && identity.equals(token.getUsername());
  }

  public void removeSeries(String identity, String series) {
    atomic(
        () -> {
          jdbc.update(
              "delete from persistent_logins where username=? and series=?", identity, series);
          jdbc.update(
              "delete from remembered_device where username=? and series=?", identity, series);
          return null;
        });
  }

  @Override
  public void removeUserTokens(String identity) {
    atomic(
        () -> {
          super.removeUserTokens(identity);
          jdbc.update("delete from remembered_device where username=?", identity);
          return null;
        });
  }

  public void deleteExpired(int duration) {
    if (duration < 1) throw new IllegalArgumentException("Invalid saved-login duration");
    atomic(
        () -> {
          jdbc.update(
              "delete from persistent_logins where last_used<?",
              Timestamp.from(Instant.now().minusSeconds(duration)));
          jdbc.update(
              "delete from remembered_device where not exists (select 1 from persistent_logins p where p.series=remembered_device.series and p.username=remembered_device.username)");
          return null;
        });
  }

  public List<Saved> saved(String identity, int duration) {
    Instant cutoff = Instant.now().minusSeconds(duration);
    return atomic(
        () -> {
          jdbc.update(
              "delete from persistent_logins where username=? and last_used<?",
              identity,
              Timestamp.from(cutoff));
          jdbc.update(
              "delete from remembered_device where username=? and not exists (select 1 from persistent_logins p where p.series=remembered_device.series and p.username=remembered_device.username)",
              identity);
          // Frühere Tokenserien bekommen neutrale Metadaten, ohne Cookies zu erneuern.
          var legacy =
              jdbc.query(
                  "select p.series,p.last_used from persistent_logins p left join remembered_device d on d.series=p.series where p.username=? and d.series is null",
                  (rs, row) -> Map.entry(rs.getString(1), rs.getTimestamp(2).toInstant()),
                  identity);
          legacy.forEach(
              entry ->
                  describe(
                      entry.getKey(), identity, DeviceDescription.from(null), entry.getValue()));
          return jdbc.query(
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
