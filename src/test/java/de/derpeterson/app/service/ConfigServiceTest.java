package de.derpeterson.app.service;

import de.derpeterson.app.model.ConfigEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.repository.ConfigRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tests the service contract without application startup or database access. */
@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {
    @Mock
    private ConfigRepository repository;
    @Mock
    private PlatformTransactionManager transactionManager;
    private ConfigService service;

    @BeforeEach
    void setUp() {
        service = new ConfigService(repository, transactionManager);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void existenceUsesTheConfigKeyAndReturnsTheRepositoryResult(boolean exists) {
        when(repository.existsByKey(ConfigEntry.SERVICE_NAME.getKey())).thenReturn(exists);

        assertEquals(exists, service.exists(ConfigEntry.SERVICE_NAME));

        verify(repository).existsByKey(ConfigEntry.SERVICE_NAME.getKey());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void existencePropagatesRepositoryFailure() {
        var failure = failure();
        when(repository.existsByKey(ConfigEntry.SERVICE_NAME.getKey())).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.exists(ConfigEntry.SERVICE_NAME)));
    }

    @Nested
    class Strings {
        @ParameterizedTest
        @ValueSource(strings = {"configured", "", "  untrimmed  ", "Grüße 🌍"})
        void storedStringsOverrideBothKindsOfDefaultWithoutNormalization(String value) {
            stored(ConfigEntry.SERVICE_NAME, value);

            assertEquals(value, service.getString(ConfigEntry.SERVICE_NAME));
            assertEquals(value, service.getString(ConfigEntry.SERVICE_NAME, "fallback"));
            verify(repository, times(2)).findByKey(ConfigEntry.SERVICE_NAME.getKey());
            verify(repository, never()).save(any());
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void missingValueUsesTheDeclaredStringDefault() {
            missing(ConfigEntry.SERVICE_NAME);

            assertEquals("{ServiceName}", service.getString(ConfigEntry.SERVICE_NAME));
        }

        @Test
        void nonStringDeclaredDefaultsAreConvertedToStrings() {
            missing(ConfigEntry.MAIL_PORT);
            missing(ConfigEntry.MAIL_SMTP_AUTH);

            assertEquals("1025", service.getString(ConfigEntry.MAIL_PORT));
            assertEquals("true", service.getString(ConfigEntry.MAIL_SMTP_AUTH));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"fallback", "  default  "})
        void missingValueUsesTheExplicitDefaultIncludingNull(String fallback) {
            missing(ConfigEntry.SERVICE_NAME);

            assertEquals(fallback, service.getString(ConfigEntry.SERVICE_NAME, fallback));
        }

        @Test
        void nullEntityValueFallsBackLikeAMissingEntry() {
            stored(ConfigEntry.SERVICE_NAME, null);

            assertEquals("{ServiceName}", service.getString(ConfigEntry.SERVICE_NAME));
            assertEquals("fallback", service.getString(ConfigEntry.SERVICE_NAME, "fallback"));
        }
    }

    @Nested
    class Integers {
        @ParameterizedTest
        @CsvSource({"42,42", "0,0", "-1,-1", "+7,7", "2147483647,2147483647", "-2147483648,-2147483648"})
        void parsesStoredIntegersWithBothOverloads(String raw, int expected) {
            stored(ConfigEntry.MAIL_PORT, raw);

            assertEquals(expected, service.getInteger(ConfigEntry.MAIL_PORT));
            assertEquals(expected, service.getInteger(ConfigEntry.MAIL_PORT, 99));
            verify(repository, never()).save(any());
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void missingIntegerUsesDeclaredDefault() {
            missing(ConfigEntry.MAIL_PORT);

            assertEquals(1025, service.getInteger(ConfigEntry.MAIL_PORT));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
        void missingIntegerUsesExplicitDefault(int fallback) {
            missing(ConfigEntry.MAIL_PORT);

            assertEquals(fallback, service.getInteger(ConfigEntry.MAIL_PORT, fallback));
        }

        @Test
        void nullIntegerValueUsesDeclaredOrExplicitDefault() {
            stored(ConfigEntry.MAIL_PORT, null);

            assertEquals(1025, service.getInteger(ConfigEntry.MAIL_PORT));
            assertEquals(-5, service.getInteger(ConfigEntry.MAIL_PORT, -5));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", " 42", "42 ", "1.5", "abc", "2147483648", "-2147483649"})
        void malformedOrOverflowingIntegersThrowInsteadOfUsingFallback(String raw) {
            stored(ConfigEntry.MAIL_PORT, raw);

            assertThrows(NumberFormatException.class, () -> service.getInteger(ConfigEntry.MAIL_PORT));
            assertThrows(NumberFormatException.class, () -> service.getInteger(ConfigEntry.MAIL_PORT, 99));
            verify(repository, never()).save(any());
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        void incompatibleDeclaredDefaultIsNotEvaluatedWhenAnIntegerIsStored() {
            stored(ConfigEntry.SERVICE_NAME, "17");

            assertEquals(17, service.getInteger(ConfigEntry.SERVICE_NAME));
        }

        @Test
        void incompatibleDeclaredDefaultFailsOnlyWhenNeeded() {
            missing(ConfigEntry.SERVICE_NAME);

            assertThrows(NumberFormatException.class, () -> service.getInteger(ConfigEntry.SERVICE_NAME));
            assertEquals(9, service.getInteger(ConfigEntry.SERVICE_NAME, 9));
        }

        @Test
        void legacyNullWithIncompatibleDeclaredDefaultAlsoFails() {
            stored(ConfigEntry.SERVICE_NAME, null);

            assertThrows(NumberFormatException.class, () -> service.getInteger(ConfigEntry.SERVICE_NAME));
            assertEquals(9, service.getInteger(ConfigEntry.SERVICE_NAME, 9));
        }

        @Test
        void explicitIntegerDefaultAvoidsParsingAnIncompatibleEnumDefault() {
            stored(ConfigEntry.SERVICE_NAME, "17");

            assertEquals(17, service.getInteger(ConfigEntry.SERVICE_NAME, 9));
        }
    }

    @Nested
    class Booleans {
        @ParameterizedTest
        @CsvSource({"true,true", "TRUE,true", "TrUe,true", "false,false", "FALSE,false"})
        void parsesStoredBooleansCaseInsensitively(String raw, boolean expected) {
            stored(ConfigEntry.MAIL_SMTP_AUTH, raw);

            assertEquals(expected, service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
            assertEquals(expected, service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, !expected));
            verify(repository, never()).save(any());
            verify(repository, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"yes", "no", "1", "0", " true ", "false ", "", " "})
        void malformedBooleansThrowWithEitherFallback(String raw) {
            stored(ConfigEntry.MAIL_SMTP_AUTH, raw);

            var failure = assertThrows(IllegalArgumentException.class,
                    () -> service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
            assertTrue(failure.getMessage().contains(ConfigEntry.MAIL_SMTP_AUTH.getKey()));
            assertThrows(IllegalArgumentException.class, () -> service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, true));
            assertThrows(IllegalArgumentException.class, () -> service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, false));
        }

        @Test
        void incompatibleBooleanDefaultIsValidatedOnlyWhenNeeded() {
            stored(ConfigEntry.SERVICE_NAME, "true");
            assertTrue(service.getBoolean(ConfigEntry.SERVICE_NAME));
            missing(ConfigEntry.SERVICE_NAME);
            assertThrows(IllegalArgumentException.class, () -> service.getBoolean(ConfigEntry.SERVICE_NAME));
            assertFalse(service.getBoolean(ConfigEntry.SERVICE_NAME, false));
        }

        @Test
        void missingBooleansUseTrueAndFalseDeclaredDefaults() {
            missing(ConfigEntry.MAIL_SMTP_AUTH);
            missing(ConfigEntry.MAINTENANCE_MODE);
            missing(ConfigEntry.MAIL_DEBUG);

            assertTrue(service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
            assertFalse(service.getBoolean(ConfigEntry.MAINTENANCE_MODE));
            assertFalse(service.getBoolean(ConfigEntry.MAIL_DEBUG));
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void missingBooleanUsesExplicitDefault(boolean fallback) {
            missing(ConfigEntry.MAIL_SMTP_AUTH);

            assertEquals(fallback, service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, fallback));
        }

        @Test
        void nullBooleanValueUsesDeclaredOrExplicitDefault() {
            stored(ConfigEntry.MAIL_SMTP_AUTH, null);

            assertTrue(service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
            assertFalse(service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, false));
        }
    }

    @Nested
    class Writes {
        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {"new value", "  preserved  "})
        void updatesTheExistingEntityPreservingItsIdentityAndKey(String value) {
            ConfigEntity existing = new ConfigEntity(7L, ConfigEntry.SERVICE_NAME.getKey(), "old");
            when(repository.findByKey(existing.getKey())).thenReturn(Optional.of(existing));

            service.set(ConfigEntry.SERVICE_NAME, value);

            assertEquals(7L, existing.getId());
            assertEquals(ConfigEntry.SERVICE_NAME.getKey(), existing.getKey());
            assertEquals(value, existing.getValue());
            verify(repository).saveAndFlush(same(existing));
            verify(transactionManager).getTransaction(argThat(definition ->
                    definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        }

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {"new value", "  preserved  "})
        void createsAMissingEntryWithTheRequestedKeyAndValue(String value) {
            missing(ConfigEntry.SERVICE_NAME);

            service.set(ConfigEntry.SERVICE_NAME, value);

            var captor = ArgumentCaptor.forClass(ConfigEntity.class);
            var order = inOrder(repository);
            order.verify(repository).findByKey(ConfigEntry.SERVICE_NAME.getKey());
            order.verify(repository).saveAndFlush(captor.capture());
            assertNull(captor.getValue().getId());
            assertEquals(ConfigEntry.SERVICE_NAME.getKey(), captor.getValue().getKey());
            assertEquals(value, captor.getValue().getValue());
        }

        @Test
        void failedLookupPreventsSaving() {
            var failure = failure();
            when(repository.findByKey(ConfigEntry.SERVICE_NAME.getKey())).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> service.set(ConfigEntry.SERVICE_NAME, "new")));
            verify(repository, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void saveFailureIsPropagatedForCreationAndUpdate(boolean existingEntry) {
            if (existingEntry) {
                stored(ConfigEntry.SERVICE_NAME, "old");
            } else {
                missing(ConfigEntry.SERVICE_NAME);
            }
            var failure = failure();
            when(repository.saveAndFlush(any(ConfigEntity.class))).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> service.set(ConfigEntry.SERVICE_NAME, "new")));
        }

        @Test
        void nullValueIsRejectedBeforeAnyRepositoryOrTransactionAccess() {
            assertThrows(NullPointerException.class, () -> service.set(ConfigEntry.SERVICE_NAME, null));
            verifyNoInteractions(repository, transactionManager);
        }

        @Test
        void failedCreationRetriesExistingRowAfterRollback() {
            var existing = new ConfigEntity(7L, ConfigEntry.SERVICE_NAME.getKey(), "winner");
            when(repository.findByKey(existing.getKey())).thenReturn(Optional.empty(), Optional.of(existing));
            var failure = new DataIntegrityViolationException("duplicate key");
            when(repository.saveAndFlush(any())).thenThrow(failure).thenAnswer(invocation -> invocation.getArgument(0));

            service.set(ConfigEntry.SERVICE_NAME, "loser");

            assertEquals("loser", existing.getValue());
            var order = inOrder(repository, transactionManager);
            order.verify(transactionManager).getTransaction(any());
            order.verify(repository).findByKey(existing.getKey());
            order.verify(repository).saveAndFlush(any());
            order.verify(transactionManager).rollback(any());
            order.verify(transactionManager).getTransaction(any());
            order.verify(repository).findByKey(existing.getKey());
            order.verify(repository).saveAndFlush(same(existing));
            order.verify(transactionManager).commit(any());
        }

        @Test
        void failedCreationWithoutCompetingRowPropagatesOriginalFailure() {
            missing(ConfigEntry.SERVICE_NAME);
            var failure = new DataIntegrityViolationException("invalid value");
            when(repository.saveAndFlush(any())).thenThrow(failure);

            assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                    () -> service.set(ConfigEntry.SERVICE_NAME, "new")));
            verify(repository, times(1)).saveAndFlush(any());
        }

        @Test
        void updateIntegrityFailureIsNotRetried() {
            stored(ConfigEntry.SERVICE_NAME, "old");
            var failure = new DataIntegrityViolationException("invalid value");
            when(repository.saveAndFlush(any())).thenThrow(failure);

            assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                    () -> service.set(ConfigEntry.SERVICE_NAME, "new")));
            verify(repository, times(1)).findByKey(ConfigEntry.SERVICE_NAME.getKey());
        }

        @Test
        void retryFailureRemainsVisibleAndIsNotRetriedAgain() {
            var existing = new ConfigEntity(7L, ConfigEntry.SERVICE_NAME.getKey(), "winner");
            when(repository.findByKey(existing.getKey())).thenReturn(Optional.empty(), Optional.of(existing));
            var retryFailure = failure();
            when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"))
                    .thenThrow(retryFailure);

            assertSame(retryFailure, assertThrows(DataAccessResourceFailureException.class,
                    () -> service.set(ConfigEntry.SERVICE_NAME, "new")));
            verify(repository, times(2)).findByKey(existing.getKey());
            verify(repository, times(2)).saveAndFlush(any());
            verify(transactionManager, times(2)).rollback(any());
        }
    }

    @Test
    void everyReadOverloadPropagatesLookupFailure() {
        var failure = failure();
        when(repository.findByKey(ConfigEntry.MAIL_PORT.getKey())).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> service.getString(ConfigEntry.MAIL_PORT)));
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> service.getString(ConfigEntry.MAIL_PORT, "fallback")));
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> service.getInteger(ConfigEntry.MAIL_PORT)));
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> service.getInteger(ConfigEntry.MAIL_PORT, 1)));
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> service.getBoolean(ConfigEntry.MAIL_PORT)));
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> service.getBoolean(ConfigEntry.MAIL_PORT, true)));
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void nullConfigEntryIsRejectedBeforeRepositoryAccessByEveryPublicMethod() {
        assertThrows(NullPointerException.class, () -> service.exists(null));
        assertThrows(NullPointerException.class, () -> service.getString(null));
        assertThrows(NullPointerException.class, () -> service.getString(null, "fallback"));
        assertThrows(NullPointerException.class, () -> service.getInteger(null));
        assertThrows(NullPointerException.class, () -> service.getInteger(null, 1));
        assertThrows(NullPointerException.class, () -> service.getBoolean(null));
        assertThrows(NullPointerException.class, () -> service.getBoolean(null, true));
        assertThrows(NullPointerException.class, () -> service.set(null, "value"));
        verifyNoInteractions(repository, transactionManager);
    }

    private void stored(ConfigEntry entry, String value) {
        when(repository.findByKey(entry.getKey())).thenReturn(Optional.of(new ConfigEntity(7L, entry.getKey(), value)));
    }

    private void missing(ConfigEntry entry) {
        when(repository.findByKey(entry.getKey())).thenReturn(Optional.empty());
    }

    private static DataAccessResourceFailureException failure() {
        return new DataAccessResourceFailureException("Configuration repository unavailable");
    }
}
