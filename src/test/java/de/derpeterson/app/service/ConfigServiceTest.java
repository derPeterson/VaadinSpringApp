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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tests the service contract without application startup or database access. */
@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {
    @Mock
    private ConfigRepository repository;
    private ConfigService service;

    @BeforeEach
    void setUp() {
        service = new ConfigService(repository);
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
        }

        @Test
        void incompatibleDeclaredDefaultFailsEvenWhenAnIntegerIsStored() {
            // The no-default overload parses the enum default before querying persistence.
            assertThrows(NumberFormatException.class, () -> service.getInteger(ConfigEntry.SERVICE_NAME));
            verifyNoInteractions(repository);
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
        @CsvSource({"true,true", "TRUE,true", "TrUe,true", "false,false", "FALSE,false", "yes,false", "1,false", "' true ',false", "'',false"})
        void parsesStoredBooleansWithoutTrimmingOrRejectingUnknownValues(String raw, boolean expected) {
            stored(ConfigEntry.MAIL_SMTP_AUTH, raw);

            assertEquals(expected, service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH));
            assertEquals(expected, service.getBoolean(ConfigEntry.MAIL_SMTP_AUTH, !expected));
            verify(repository, never()).save(any());
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
        @NullAndEmptySource
        @ValueSource(strings = {"new value", "  preserved  "})
        void updatesTheExistingEntityPreservingItsIdentityAndKey(String value) {
            ConfigEntity existing = new ConfigEntity(7L, ConfigEntry.SERVICE_NAME.getKey(), "old");
            when(repository.findByKey(existing.getKey())).thenReturn(Optional.of(existing));

            service.set(ConfigEntry.SERVICE_NAME, value);

            assertEquals(7L, existing.getId());
            assertEquals(ConfigEntry.SERVICE_NAME.getKey(), existing.getKey());
            assertEquals(value, existing.getValue());
            verify(repository).save(same(existing));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"new value", "  preserved  "})
        void createsAMissingEntryWithTheRequestedKeyAndValue(String value) {
            missing(ConfigEntry.SERVICE_NAME);

            service.set(ConfigEntry.SERVICE_NAME, value);

            var captor = ArgumentCaptor.forClass(ConfigEntity.class);
            var order = inOrder(repository);
            order.verify(repository).findByKey(ConfigEntry.SERVICE_NAME.getKey());
            order.verify(repository).save(captor.capture());
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
            verify(repository, never()).save(any());
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
            when(repository.save(any(ConfigEntity.class))).thenThrow(failure);

            assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                    () -> service.set(ConfigEntry.SERVICE_NAME, "new")));
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
        verifyNoInteractions(repository);
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
