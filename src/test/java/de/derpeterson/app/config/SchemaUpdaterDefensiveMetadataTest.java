package de.derpeterson.app.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

class SchemaUpdaterDefensiveMetadataTest {
  @ParameterizedTest
  @CsvSource(
      value = {"true,NULL", "false,NULL", "true,VARCHAR", "false,VARCHAR"},
      nullValues = "NULL")
  void absentOrNonEnumMetadataDoesNotTriggerDdl(boolean roles, String type) {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.query(anyString(), ArgumentMatchers.<ResultSetExtractor<String>>any()))
        .thenReturn(type);

    if (roles) {
      new RoleSchemaUpdater(jdbc).updateRoleColumn();
    } else {
      new UserNotificationSchemaUpdater(jdbc).updateTypeColumn();
    }

    verify(jdbc, never()).execute(anyString());
  }
}
