package de.derpeterson.app.service;

import de.derpeterson.app.model.enums.ConfigEntry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Ein leer gespeicherter Wert bedeutet unbegrenzt; fehlende Einträge verwenden die Defaults. */
@Component
@RequiredArgsConstructor
public class UserNotificationPolicy {
  private final ConfigService config;

  public OptionalInt retentionDays() {
    return days(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS);
  }

  public OptionalInt historyRetentionDays() {
    return days(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS);
  }

  /** Null ist eine ausdrücklich unbegrenzte Frist, niemals eine Ersatzfrist bei Fehlern. */
  public @Nullable Instant inboxCutoff(Instant now) {
    return cutoff(now, retentionDays());
  }

  public @Nullable Instant historyCutoff(Instant now) {
    return cutoff(now, historyRetentionDays());
  }

  private @Nullable Instant cutoff(Instant now, OptionalInt days) {
    return days.isPresent() ? now.minus(days.getAsInt(), ChronoUnit.DAYS) : null;
  }

  private OptionalInt days(ConfigEntry entry) {
    String value = config.getString(entry);
    if (value.isBlank()) return OptionalInt.empty();
    int days = Integer.parseInt(value.trim());
    if (days < 1)
      throw new IllegalArgumentException(
          "Positive notification retention required: " + entry.getKey());
    return OptionalInt.of(days);
  }
}
