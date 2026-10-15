package de.derpeterson.app.config;

import de.derpeterson.app.model.enums.RoleType;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Hibernate does not extend existing H2 native enums when Java enum constants are added. */
@Component
@RequiredArgsConstructor
public class RoleSchemaUpdater {
  private final JdbcTemplate jdbc;

  public void updateRoleColumn() {
    String type =
        jdbc.query(
            "select name from roles where 1 = 0",
            result -> {
              return result.getMetaData().getColumnTypeName(1);
            });
    if (type == null || !type.startsWith("ENUM")) return;
    var names = Arrays.stream(RoleType.values()).map(Enum::name).toList();
    if (names.stream().allMatch(name -> type.contains("'" + name + "'"))) return;
    // SQL identifiers are fixed; all values come exclusively from our Java enum, never user input.
    String values = names.stream().map(name -> "'" + name + "'").collect(Collectors.joining(", "));
    jdbc.execute("alter table roles alter column name enum (" + values + ") not null");
  }
}
