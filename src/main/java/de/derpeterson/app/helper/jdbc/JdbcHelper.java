package de.derpeterson.app.helper.jdbc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcHelper {
    public static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                    Integer.class, tableName.toUpperCase()
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}