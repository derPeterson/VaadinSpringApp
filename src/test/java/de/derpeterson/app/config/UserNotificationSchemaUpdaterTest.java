package de.derpeterson.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class UserNotificationSchemaUpdaterTest {
  JdbcTemplate jdbc() {
    return new JdbcTemplate(
        new DriverManagerDataSource(
            "jdbc:h2:mem:message-enum-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
  }

  @Test
  void extendsOldNativeEnumAndPreservesExistingNotificationsIdempotently() {
    JdbcTemplate jdbc = jdbc();
    jdbc.execute(
        "create table user_notifications(id bigint primary key,type enum('ADMIN_ANNOUNCEMENT','PASSWORD_CHANGED','PASSWORD_RESET','EMAIL_CHANGED') not null)");
    jdbc.execute("insert into user_notifications values (1,'PASSWORD_CHANGED')");
    UserNotificationSchemaUpdater migration = new UserNotificationSchemaUpdater(jdbc);
    migration.updateTypeColumn();
    migration.updateTypeColumn();
    jdbc.execute("insert into user_notifications values (2,'ROLES_CHANGED')");
    assertEquals(
        "PASSWORD_CHANGED",
        jdbc.queryForObject("select type from user_notifications where id=1", String.class));
    assertEquals(2, jdbc.queryForObject("select count(*) from user_notifications", Integer.class));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> jdbc.execute("insert into user_notifications values(3,null)"));
  }

  @Test
  void varcharSchemaIsKeptAndAlreadyAcceptsRoleEvents() {
    JdbcTemplate jdbc = jdbc();
    jdbc.execute(
        "create table user_notifications(id bigint primary key,type varchar(255) not null)");
    new UserNotificationSchemaUpdater(jdbc).updateTypeColumn();
    jdbc.execute("insert into user_notifications values(1,'ROLES_CHANGED')");
    assertEquals(1, jdbc.queryForObject("select count(*) from user_notifications", Integer.class));
  }
}
