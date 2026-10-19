package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

class UserNotificationPolicyTest {
  final ConfigRepository repository = mock(ConfigRepository.class);
  final MockEnvironment environment = new MockEnvironment();
  final ConfigService config =
      new ConfigService(repository, mock(PlatformTransactionManager.class), environment);
  final UserNotificationPolicy policy = new UserNotificationPolicy(config);

  @Test
  void defaultsAre180DaysForInboxAnd365DaysForHistory() {
    assertEquals(java.util.OptionalInt.of(180), policy.retentionDays());
    assertEquals(java.util.OptionalInt.of(365), policy.historyRetentionDays());
  }

  @Test
  void configuredDefaultsAndStoredValuesAreReadLiveWithDatabasePrecedence() {
    environment
        .withProperty("app.config.defaults.user.notifications.inbox.retention.days", "30")
        .withProperty("app.config.defaults.user.notifications.history.retention.days", "60");
    assertEquals(java.util.OptionalInt.of(30), policy.retentionDays());
    assertEquals(java.util.OptionalInt.of(60), policy.historyRetentionDays());
    var stored =
        new ConfigEntity(null, ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS.getKey(), "45");
    when(repository.findByKey(stored.getKey())).thenReturn(Optional.of(stored));
    assertEquals(java.util.OptionalInt.of(45), policy.retentionDays());
    stored.setValue("90");
    assertEquals(java.util.OptionalInt.of(90), policy.retentionDays());
    assertEquals(java.util.OptionalInt.of(60), policy.historyRetentionDays());
    environment.setProperty("app.config.defaults.user.notifications.history.retention.days", "7");
    assertEquals(java.util.OptionalInt.of(7), policy.historyRetentionDays());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void bothPeriodsRejectNonpositiveValues(int days) {
    environment.setProperty(
        "app.config.defaults.user.notifications.inbox.retention.days", Integer.toString(days));
    assertThrows(IllegalArgumentException.class, policy::retentionDays);
    environment.setProperty(
        "app.config.defaults.user.notifications.history.retention.days", Integer.toString(days));
    assertThrows(IllegalArgumentException.class, policy::historyRetentionDays);
  }

  @Test
  void malformedValueFailsInsteadOfSilentlyShorteningRetention() {
    environment.setProperty(
        "app.config.defaults.user.notifications.inbox.retention.days", "invalid");
    assertThrows(NumberFormatException.class, policy::retentionDays);
  }

  @Test
  void explicitlyStoredEmptyValuesOverrideDefaultsAndHaveNoCutoff() {
    var inbox = ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS;
    var history = ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS;
    when(repository.findByKey(inbox.getKey()))
        .thenReturn(Optional.of(new ConfigEntity(null, inbox.getKey(), "")));
    when(repository.findByKey(history.getKey()))
        .thenReturn(Optional.of(new ConfigEntity(null, history.getKey(), "")));
    assertTrue(policy.retentionDays().isEmpty());
    assertTrue(policy.historyRetentionDays().isEmpty());
    assertNull(policy.inboxCutoff(java.time.Instant.now()));
    assertNull(policy.historyCutoff(java.time.Instant.now()));
    assertEquals("180", config.getDefaultValue(inbox));
    assertEquals("365", config.getDefaultValue(history));
  }

  @Test
  void emptyConfiguredInitialValuesAreAlsoExplicitlyUnlimited() {
    environment.setProperty("app.config.defaults.user.notifications.inbox.retention.days", "");
    environment.setProperty("app.config.defaults.user.notifications.history.retention.days", "");
    assertTrue(policy.retentionDays().isEmpty());
    assertTrue(policy.historyRetentionDays().isEmpty());
  }
}
