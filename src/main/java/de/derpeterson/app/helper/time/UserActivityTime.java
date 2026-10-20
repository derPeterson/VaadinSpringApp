package de.derpeterson.app.helper.time;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/** Storage and durations use instants; only calendar input and display use this zone. */
public final class UserActivityTime {
  public static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Berlin");

  private UserActivityTime() {}

  public static Instant inclusiveCutoff(LocalDateTime local) {
    if (local == null) {
      return null;
    }
    List<ZoneOffset> offsets = DISPLAY_ZONE.getRules().getValidOffsets(local);
    if (offsets.isEmpty()) {
      throw new DateTimeException("Activity cutoff does not exist in the display zone");
    }
    // Bei der doppelten Herbststunde beginnt „aktiv seit“ am ersten Vorkommen.
    // So bleiben beide Vorkommen eingeschlossen; Frühlingslücken werden nicht verschoben.
    return local.toInstant(offsets.getFirst());
  }
}
