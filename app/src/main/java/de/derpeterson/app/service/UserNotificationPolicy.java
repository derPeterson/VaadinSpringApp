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
  private final ConfigService configService;

  public OptionalInt retentionDays() {
    return days(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS);
  }

  public OptionalInt historyRetentionDays() {
    return days(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS);
  }

  /** Null ist eine ausdrücklich unbegrenzte Frist, niemals eine Ersatzfrist bei Fehlern. */
  public @Nullable Instant inboxCutoff(Instant referenceTime) {
    return cutoff(referenceTime, retentionDays());
  }

  public @Nullable Instant historyCutoff(Instant referenceTime) {
    return cutoff(referenceTime, historyRetentionDays());
  }

  private @Nullable Instant cutoff(Instant referenceTime, OptionalInt days) {
    return days.isPresent() ? referenceTime.minus(days.getAsInt(), ChronoUnit.DAYS) : null;
  }

  private OptionalInt days(ConfigEntry retentionConfigEntry) {
    String configuredRetentionText = configService.getString(retentionConfigEntry);

    if (configuredRetentionText.isBlank()) {
      return OptionalInt.empty();
    }

    int retentionDaysCount = Integer.parseInt(configuredRetentionText.trim());
    if (retentionDaysCount < 1) {
      throw new IllegalArgumentException(
          "Positive notification retention required: " + retentionConfigEntry.getKey());
    }

    return OptionalInt.of(retentionDaysCount);
  }
}
