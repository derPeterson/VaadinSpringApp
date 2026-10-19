package de.derpeterson.app.model.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RetiredConfigEntryTest {
  @Test
  void retiredKeysAreUniqueNonBlankAndNeverActive() {
    Set<String> activeKeys =
        Arrays.stream(ConfigEntry.values()).map(ConfigEntry::getKey).collect(Collectors.toSet());
    HashSet<String> retiredKeys = new HashSet<String>();
    for (RetiredConfigEntry entry : RetiredConfigEntry.values()) {
      assertFalse(entry.getKey().isBlank());
      assertTrue(
          retiredKeys.add(entry.getKey()), "Doppelter ausgemusterter Schlüssel: " + entry.getKey());
      assertFalse(
          activeKeys.contains(entry.getKey()),
          "Aktiver Schlüssel darf nicht gelöscht werden: " + entry.getKey());
    }
  }
}
