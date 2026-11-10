package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/** Tests the service contract without application startup or database access. */
@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {
  @Mock private ConfigRepository configRepository;
  @Mock private PlatformTransactionManager transactionManager;
  private ConfigService configService;

  @BeforeEach
  void setUp() {
    configService = new ConfigService(configRepository, transactionManager, new MockEnvironment());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void existenceUsesTheConfigKeyAndReturnsTheRepositoryResult(boolean exists) {
    when(configRepository.existsByKey(ConfigEntry.SERVICE_NAME.getKey())).thenReturn(exists);

    assertEquals(exists, configService.exists(ConfigEntry.SERVICE_NAME));

    verify(configRepository).existsByKey(ConfigEntry.SERVICE_NAME.getKey());
    verifyNoMoreInteractions(configRepository);
  }

  @Test
  void existencePropagatesRepositoryFailure() {
    DataAccessResourceFailureException failure = failure();
    when(configRepository.existsByKey(ConfigEntry.SERVICE_NAME.getKey())).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> configService.exists(ConfigEntry.SERVICE_NAME)));
  }

  @Nested
  class Strings {
    @ParameterizedTest
    @ValueSource(strings = {"configured", "", "  untrimmed  ", "Grüße 🌍"})
    void storedStringsOverrideBothKindsOfDefaultWithoutNormalization(String value) {
      stored(ConfigEntry.SERVICE_NAME, value);

      assertEquals(value, configService.getString(ConfigEntry.SERVICE_NAME));
      assertEquals(value, configService.getString(ConfigEntry.SERVICE_NAME, "fallback"));
      verify(configRepository, times(2)).findByKey(ConfigEntry.SERVICE_NAME.getKey());
      verify(configRepository, never()).save(any());
      verify(configRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingValueUsesTheDeclaredStringDefault() {
      missing(ConfigEntry.SERVICE_NAME);

      assertEquals("{ServiceName}", configService.getString(ConfigEntry.SERVICE_NAME));
    }

    @Test
    void nonStringDeclaredDefaultsAreConvertedToStrings() {
      missing(ConfigEntry.MAIL_PORT);
      missing(ConfigEntry.MAIL_SMTP_AUTH);

      assertEquals("1025", configService.getString(ConfigEntry.MAIL_PORT));
      assertEquals("true", configService.getString(ConfigEntry.MAIL_SMTP_AUTH));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"fallback", "  default  "})
    void missingValueUsesTheExplicitDefaultIncludingNull(String fallback) {
      missing(ConfigEntry.SERVICE_NAME);

      assertEquals(fallback, configService.getString(ConfigEntry.SERVICE_NAME, fallback));
    }

    @Test
    void nullEntityValueFallsBackLikeAMissingEntry() {
      stored(ConfigEntry.SERVICE_NAME, null);

      assertEquals("{ServiceName}", configService.getString(ConfigEntry.SERVICE_NAME));
      assertEquals("fallback", configService.getString(ConfigEntry.SERVICE_NAME, "fallback"));
    }
  }

  @Nested
  class Integers {
    @ParameterizedTest
    @CsvSource({
      "42,42",
      "0,0",
      "-1,-1",
      "+7,7",
      "2147483647,2147483647",
      "-2147483648,-2147483648"
    })
    void parsesStoredIntegersWithBothOverloads(String raw, int expected) {
      stored(ConfigEntry.MAIL_PORT, raw);

      assertEquals(expected, configService.getInteger(ConfigEntry.MAIL_PORT));
      assertEquals(expected, configService.getInteger(ConfigEntry.MAIL_PORT, 99));
      verify(configRepository, never()).save(any());
      verify(configRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingIntegerUsesDeclaredDefault() {
      missing(ConfigEntry.MAIL_PORT);

      assertEquals(1025, configService.getInteger(ConfigEntry.MAIL_PORT));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void missingIntegerUsesExplicitDefault(int fallback) {
      missing(ConfigEntry.MAIL_PORT);

      assertEquals(fallback, configService.getInteger(ConfigEntry.MAIL_PORT, fallback));
    }

    @Test
    void nullIntegerValueUsesDeclaredOrExplicitDefault() {
      stored(ConfigEntry.MAIL_PORT, null);

      assertEquals(1025, configService.getInteger(ConfigEntry.MAIL_PORT));
      assertEquals(-5, configService.getInteger(ConfigEntry.MAIL_PORT, -5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", " 42", "42 ", "1.5", "abc", "2147483648", "-2147483649"})
    void malformedOrOverflowingIntegersThrowInsteadOfUsingFallback(String raw) {
      stored(ConfigEntry.MAIL_PORT, raw);

      assertThrows(
          NumberFormatException.class, () -> configService.getInteger(ConfigEntry.MAIL_PORT));
      assertThrows(
          NumberFormatException.class, () -> configService.getInteger(ConfigEntry.MAIL_PORT, 99));
      verify(configRepository, never()).save(any());
      verify(configRepository, never()).saveAndFlush(any());
    }

    @Test
    void incompatibleDeclaredDefaultIsNotEvaluatedWhenAnIntegerIsStored() {
      stored(ConfigEntry.SERVICE_NAME, "17");

      assertEquals(17, configService.getInteger(ConfigEntry.SERVICE_NAME));
    }

    @Test
    void incompatibleDeclaredDefaultFailsOnlyWhenNeeded() {
      missing(ConfigEntry.SERVICE_NAME);

      assertThrows(
          NumberFormatException.class, () -> configService.getInteger(ConfigEntry.SERVICE_NAME));
      assertEquals(9, configService.getInteger(ConfigEntry.SERVICE_NAME, 9));
    }

    @Test
    void missingValueWithIncompatibleDeclaredDefaultAlsoFails() {
      stored(ConfigEntry.SERVICE_NAME, null);

      assertThrows(
          NumberFormatException.class, () -> configService.getInteger(ConfigEntry.SERVICE_NAME));
      assertEquals(9, configService.getInteger(ConfigEntry.SERVICE_NAME, 9));
    }

    @Test
    void explicitIntegerDefaultAvoidsParsingAnIncompatibleEnumDefault() {
      stored(ConfigEntry.SERVICE_NAME, "17");

      assertEquals(17, configService.getInteger(ConfigEntry.SERVICE_NAME, 9));
    }
  }

  @Nested
  class Booleans {
    @ParameterizedTest
    @CsvSource({"true,true", "TRUE,true", "TrUe,true", "false,false", "FALSE,false"})
    void parsesStoredBooleansCaseInsensitively(String raw, boolean expected) {
      stored(ConfigEntry.MAIL_SMTP_AUTH, raw);

      assertEquals(expected, configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
      assertEquals(expected, configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, !expected));
      verify(configRepository, never()).save(any());
      verify(configRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "no", "1", "0", " true ", "false ", "", " "})
    void malformedBooleansThrowWithEitherFallback(String raw) {
      stored(ConfigEntry.MAIL_SMTP_AUTH, raw);

      IllegalArgumentException failure =
          assertThrows(
              IllegalArgumentException.class,
              () -> configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));

      assertTrue(failure.getMessage().contains(ConfigEntry.MAIL_SMTP_AUTH.getKey()));
      assertThrows(
          IllegalArgumentException.class,
          () -> configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, true));
      assertThrows(
          IllegalArgumentException.class,
          () -> configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, false));
    }

    @Test
    void incompatibleBooleanDefaultIsValidatedOnlyWhenNeeded() {
      stored(ConfigEntry.SERVICE_NAME, "true");

      assertTrue(configService.getBoolean(ConfigEntry.SERVICE_NAME));
      missing(ConfigEntry.SERVICE_NAME);

      assertThrows(
          IllegalArgumentException.class, () -> configService.getBoolean(ConfigEntry.SERVICE_NAME));
      assertFalse(configService.getBoolean(ConfigEntry.SERVICE_NAME, false));
    }

    @Test
    void missingBooleansUseTrueAndFalseDeclaredDefaults() {
      missing(ConfigEntry.MAIL_SMTP_AUTH);
      missing(ConfigEntry.MAINTENANCE_MODE);
      missing(ConfigEntry.MAIL_DEBUG);

      assertTrue(configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
      assertFalse(configService.getBoolean(ConfigEntry.MAINTENANCE_MODE));
      assertFalse(configService.getBoolean(ConfigEntry.MAIL_DEBUG));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingBooleanUsesExplicitDefault(boolean fallback) {
      missing(ConfigEntry.MAIL_SMTP_AUTH);

      assertEquals(fallback, configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, fallback));
    }

    @Test
    void nullBooleanValueUsesDeclaredOrExplicitDefault() {
      stored(ConfigEntry.MAIL_SMTP_AUTH, null);

      assertTrue(configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
      assertFalse(configService.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, false));
    }
  }

  @Nested
  class Writes {
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"new value", "  preserved  "})
    void updatesTheExistingEntityPreservingItsIdentityAndKey(String value) {
      ConfigEntity existing = new ConfigEntity(7L, ConfigEntry.SERVICE_NAME.getKey(), "old");
      when(configRepository.findByKey(existing.getKey())).thenReturn(Optional.of(existing));

      configService.set(ConfigEntry.SERVICE_NAME, value);

      assertEquals(7L, existing.getId());
      assertEquals(ConfigEntry.SERVICE_NAME.getKey(), existing.getKey());
      assertEquals(value, existing.getValue());
      verify(configRepository).saveAndFlush(same(existing));
      verify(transactionManager)
          .getTransaction(
              argThat(definition ->
                  definition.getPropagationBehavior()
                      == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"new value", "  preserved  "})
    void createsAMissingEntryWithTheRequestedKeyAndValue(String value) {
      missing(ConfigEntry.SERVICE_NAME);

      configService.set(ConfigEntry.SERVICE_NAME, value);

      ArgumentCaptor<ConfigEntity> captor = ArgumentCaptor.forClass(ConfigEntity.class);
      InOrder order = inOrder(configRepository);
      order.verify(configRepository).findByKey(ConfigEntry.SERVICE_NAME.getKey());
      order.verify(configRepository).saveAndFlush(captor.capture());

      assertNull(captor.getValue().getId());
      assertEquals(ConfigEntry.SERVICE_NAME.getKey(), captor.getValue().getKey());
      assertEquals(value, captor.getValue().getValue());
    }

    @Test
    void failedLookupPreventsSaving() {
      DataAccessResourceFailureException failure = failure();
      when(configRepository.findByKey(ConfigEntry.SERVICE_NAME.getKey())).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> configService.set(ConfigEntry.SERVICE_NAME, "new")));
      verify(configRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void saveFailureIsPropagatedForCreationAndUpdate(boolean existingEntry) {
      if (existingEntry) {
        stored(ConfigEntry.SERVICE_NAME, "old");
      } else {
        missing(ConfigEntry.SERVICE_NAME);
      }
      DataAccessResourceFailureException failure = failure();
      when(configRepository.saveAndFlush(any(ConfigEntity.class))).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> configService.set(ConfigEntry.SERVICE_NAME, "new")));
    }

    @Test
    void nullValueIsRejectedBeforeAnyRepositoryOrTransactionAccess() {
      assertThrows(
          NullPointerException.class, () -> configService.set(ConfigEntry.SERVICE_NAME, null));
      verifyNoInteractions(configRepository, transactionManager);
    }

    @Test
    void failedCreationRetriesExistingRowAfterRollback() {
      ConfigEntity existing = new ConfigEntity(7L, ConfigEntry.SERVICE_NAME.getKey(), "winner");
      when(configRepository.findByKey(existing.getKey()))
          .thenReturn(Optional.empty(), Optional.of(existing));
      DataIntegrityViolationException failure =
          new DataIntegrityViolationException("duplicate key");
      when(configRepository.saveAndFlush(any()))
          .thenThrow(failure)
          .thenAnswer(invocation -> invocation.getArgument(0));

      configService.set(ConfigEntry.SERVICE_NAME, "loser");

      assertEquals("loser", existing.getValue());
      InOrder order = inOrder(configRepository, transactionManager);
      order.verify(transactionManager).getTransaction(any());
      order.verify(configRepository).findByKey(existing.getKey());
      order.verify(configRepository).saveAndFlush(any());
      order.verify(transactionManager).rollback(any());
      order.verify(transactionManager).getTransaction(any());
      order.verify(configRepository).findByKey(existing.getKey());
      order.verify(configRepository).saveAndFlush(same(existing));
      order.verify(transactionManager).commit(any());
    }

    @Test
    void failedCreationWithoutCompetingRowPropagatesOriginalFailure() {
      missing(ConfigEntry.SERVICE_NAME);
      DataIntegrityViolationException failure =
          new DataIntegrityViolationException("invalid value");
      when(configRepository.saveAndFlush(any())).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataIntegrityViolationException.class,
              () -> configService.set(ConfigEntry.SERVICE_NAME, "new")));
      verify(configRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void updateIntegrityFailureIsNotRetried() {
      stored(ConfigEntry.SERVICE_NAME, "old");
      DataIntegrityViolationException failure =
          new DataIntegrityViolationException("invalid value");
      when(configRepository.saveAndFlush(any())).thenThrow(failure);

      assertSame(
          failure,
          assertThrows(
              DataIntegrityViolationException.class,
              () -> configService.set(ConfigEntry.SERVICE_NAME, "new")));
      verify(configRepository, times(1)).findByKey(ConfigEntry.SERVICE_NAME.getKey());
    }

    @Test
    void retryFailureRemainsVisibleAndIsNotRetriedAgain() {
      ConfigEntity existing = new ConfigEntity(7L, ConfigEntry.SERVICE_NAME.getKey(), "winner");
      when(configRepository.findByKey(existing.getKey()))
          .thenReturn(Optional.empty(), Optional.of(existing));
      DataAccessResourceFailureException retryFailure = failure();
      when(configRepository.saveAndFlush(any()))
          .thenThrow(new DataIntegrityViolationException("duplicate key"))
          .thenThrow(retryFailure);

      assertSame(
          retryFailure,
          assertThrows(
              DataAccessResourceFailureException.class,
              () -> configService.set(ConfigEntry.SERVICE_NAME, "new")));
      verify(configRepository, times(2)).findByKey(existing.getKey());
      verify(configRepository, times(2)).saveAndFlush(any());
      verify(transactionManager, times(2)).rollback(any());
    }
  }

  @Test
  void everyReadOverloadPropagatesLookupFailure() {
    DataAccessResourceFailureException failure = failure();
    when(configRepository.findByKey(ConfigEntry.MAIL_PORT.getKey())).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> configService.getString(ConfigEntry.MAIL_PORT)));
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> configService.getString(ConfigEntry.MAIL_PORT, "fallback")));
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> configService.getInteger(ConfigEntry.MAIL_PORT)));
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> configService.getInteger(ConfigEntry.MAIL_PORT, 1)));
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> configService.getBoolean(ConfigEntry.MAIL_PORT)));
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> configService.getBoolean(ConfigEntry.MAIL_PORT, true)));
    verify(configRepository, never()).save(any());
    verify(configRepository, never()).saveAndFlush(any());
  }

  @Test
  void nullConfigEntryIsRejectedBeforeRepositoryAccessByEveryPublicMethod() {
    assertThrows(NullPointerException.class, () -> configService.exists(null));
    assertThrows(NullPointerException.class, () -> configService.getString(null));
    assertThrows(NullPointerException.class, () -> configService.getString(null, "fallback"));
    assertThrows(NullPointerException.class, () -> configService.getInteger(null));
    assertThrows(NullPointerException.class, () -> configService.getInteger(null, 1));
    assertThrows(NullPointerException.class, () -> configService.getBoolean(null));
    assertThrows(NullPointerException.class, () -> configService.getBoolean(null, true));
    assertThrows(NullPointerException.class, () -> configService.set(null, "value"));
    verifyNoInteractions(configRepository, transactionManager);
  }

  private void stored(ConfigEntry entry, String value) {
    when(configRepository.findByKey(entry.getKey()))
        .thenReturn(Optional.of(new ConfigEntity(7L, entry.getKey(), value)));
  }

  private void missing(ConfigEntry entry) {
    when(configRepository.findByKey(entry.getKey())).thenReturn(Optional.empty());
  }

  private static DataAccessResourceFailureException failure() {
    return new DataAccessResourceFailureException("Configuration repository unavailable");
  }
}
