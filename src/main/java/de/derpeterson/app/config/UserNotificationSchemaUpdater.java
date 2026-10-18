package de.derpeterson.app.config;

import de.derpeterson.app.model.enums.UserNotificationType;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** H2-Benachrichtigungstypen erweitern, bevor neue Ereignistypen geschrieben werden. */
@Component
@RequiredArgsConstructor
public class UserNotificationSchemaUpdater {
  private final JdbcTemplate jdbc;

  public void updateTypeColumn() {
    var type =
        jdbc.query(
            "select type from user_notifications where 1 = 0",
            r -> {
              return r.getMetaData().getColumnTypeName(1);
            });
    if (type == null || !type.startsWith("ENUM")) return;
    var names = Arrays.stream(UserNotificationType.values()).map(Enum::name).toList();
    if (names.stream().allMatch(n -> type.contains("'" + n + "'"))) return;
    // Feste SQL-Bezeichner und ausschließlich eigene Enumwerte; keine Benutzereingaben.
    var values = names.stream().map(n -> "'" + n + "'").collect(Collectors.joining(", "));
    jdbc.execute("alter table user_notifications alter column type enum (" + values + ") not null");
  }
}
