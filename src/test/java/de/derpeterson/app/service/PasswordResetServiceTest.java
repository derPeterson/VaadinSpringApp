package de.derpeterson.app.service;

import de.derpeterson.app.helper.image.ImageHelper;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Service logic and exact time boundaries; no application, database or SMTP. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 12, 0);
    @Mock
    private MessageProperties messages;
    @Mock
    private PasswordResetTokenRepository tokens;
    @Mock
    private UserRepository users;
    @Mock
    private ConfigService config;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private EmailQueueService queue;
    private PasswordResetService service;
    private UserEntity user;

    @BeforeEach
    void setup() {
        service = new PasswordResetService(messages, tokens, users, config, encoder, queue);
        user = UserEntity.builder().id(1L).email("test@example.com").password("old-hash").enabled(true).build();
    }

    private PasswordResetTokenEntity token(String value, TokenStatus status, LocalDateTime expiry) {
        return new PasswordResetTokenEntity(7L, value, user, expiry, status);
    }

    @Test
    void creationInvalidatesActiveTokensBeforeSavingANewUuidForTheSameUser() {
        var old = token("old", TokenStatus.ACTIVE, NOW.plusDays(1));
        when(tokens.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE)).thenReturn(List.of(old));
        when(config.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION)).thenReturn("PT1H");
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(NOW);
            String value = service.createToken(user);
            assertEquals(value, UUID.fromString(value).toString());
            assertNotEquals(old.getToken(), value);
            var saved = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
            var order = inOrder(tokens, config);
            order.verify(tokens).findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE);
            order.verify(tokens).save(old);
            order.verify(config).getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION);
            order.verify(tokens).save(saved.capture());
            assertEquals(TokenStatus.INACTIVE, old.getStatus());
            assertNull(saved.getValue().getId());
            assertSame(user, saved.getValue().getUserEntity());
            assertEquals(value, saved.getValue().getToken());
            assertEquals(NOW.plusHours(1), saved.getValue().getExpiryDate());
            assertEquals(TokenStatus.ACTIVE, saved.getValue().getStatus());
            order.verifyNoMoreInteractions();
        }
        verifyNoInteractions(users, encoder, queue);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1S"})
    void creationCurrentlyAcceptsNonPositiveDurations(String duration) {
        when(config.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION)).thenReturn(duration);
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(NOW);
            service.createToken(user);
            var saved = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
            verify(tokens).save(saved.capture());
            assertEquals(NOW.plus(java.time.Duration.parse(duration)), saved.getValue().getExpiryDate());
            assertEquals(TokenStatus.ACTIVE, saved.getValue().getStatus());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"broken"})
    void invalidDurationPropagatesWithoutSavingANewToken(String duration) {
        when(config.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION)).thenReturn(duration);
        assertThrows(RuntimeException.class, () -> service.createToken(user));
        verify(tokens).findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE);
        verifyNoMoreInteractions(tokens);
        verifyNoInteractions(users, queue, encoder);
    }

    @Test
    void invalidationChangesOnlyTheReturnedActiveTokensAndLeavesPasswordUntouched() {
        var first = token("first", TokenStatus.ACTIVE, NOW);
        var second = token("second", TokenStatus.ACTIVE, NOW);
        when(tokens.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE)).thenReturn(List.of(first, second));
        service.setInactiveTokensForUser(user);
        assertEquals(TokenStatus.INACTIVE, first.getStatus());
        assertEquals(TokenStatus.INACTIVE, second.getStatus());
        verify(tokens).save(first);
        verify(tokens).save(second);
        assertEquals("old-hash", user.getPassword());
        verifyNoInteractions(users, encoder, queue);
    }

    @ParameterizedTest
    @EnumSource(TokenStatus.class)
    void statusSetterCurrentlyAllowsEveryTransitionIncludingReactivation(TokenStatus status) {
        var stored = token("used", TokenStatus.USED, NOW.plusDays(1));
        when(tokens.findByToken("used")).thenReturn(Optional.of(stored));
        service.setTokenStatus("used", status);
        assertEquals(status, stored.getStatus());
        verify(tokens).save(stored);
        verifyNoInteractions(users, encoder, queue);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown"})
    void unknownOrEmptyTokensAreNotValidDoNotResetAndDoNotExist(String value) {
        assertFalse(service.validateToken(value));
        assertFalse(service.resetPassword(value, "Password!"));
        assertFalse(service.existsToken(value));
        service.setTokenStatus(value, TokenStatus.USED);
        verify(tokens, times(2)).findByTokenAndStatus(value, TokenStatus.ACTIVE);
        verify(tokens, times(2)).findByToken(value);
        verifyNoMoreInteractions(tokens);
        verifyNoInteractions(users, encoder, config, queue);
    }

    @ParameterizedTest
    @CsvSource({"false,-1", "false,0", "false,1", "true,-1", "true,0", "true,1"})
    void expiryIsStrictlyBeforeNowAndResetRechecksIt(boolean resetPassword, long nanoseconds) {
        var stored = token("boundary", TokenStatus.ACTIVE, NOW.plusNanos(nanoseconds));
        when(tokens.findByTokenAndStatus("boundary", TokenStatus.ACTIVE)).thenReturn(Optional.of(stored));
        if (nanoseconds < 0 || resetPassword) {
            when(tokens.findByToken("boundary")).thenReturn(Optional.of(stored));
        }
        if (resetPassword && nanoseconds >= 0) {
            when(encoder.encode("Password!")).thenReturn("new-hash");
        }
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(NOW);
            boolean result = resetPassword ? service.resetPassword("boundary", "Password!") : service.validateToken("boundary");
            assertEquals(nanoseconds >= 0, result);
        }
        assertEquals(nanoseconds < 0 ? TokenStatus.EXPIRED : resetPassword ? TokenStatus.USED : TokenStatus.ACTIVE, stored.getStatus());
        if (resetPassword && nanoseconds >= 0) {
            assertEquals("new-hash", user.getPassword());
            verify(users).save(user);
        } else {
            assertEquals("old-hash", user.getPassword());
            verifyNoInteractions(users, encoder);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void disabledAccountRejectsBothValidationAndResetWithoutConsumingToken(boolean resetPassword) {
        user.setEnabled(false);
        var stored = token("disabled", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        when(tokens.findByTokenAndStatus("disabled", TokenStatus.ACTIVE)).thenReturn(Optional.of(stored));
        assertFalse(resetPassword ? service.resetPassword("disabled", "Password!") : service.validateToken("disabled"));
        assertEquals(TokenStatus.ACTIVE, stored.getStatus());
        verify(tokens, never()).save(any());
        verifyNoInteractions(users, encoder);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "short!A", "lowercase!", "NoSpecialCharacter"})
    void invalidPasswordsFailBeforeAnyTokenLookupOrSideEffect(String password) {
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("valid", password));
        verifyNoInteractions(tokens, users, encoder, config, queue);
    }

    @Test
    void securePasswordIsEncodedVerbatimAndTokenConsumedAfterUserSave() {
        var stored = token("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        when(tokens.findByTokenAndStatus("valid", TokenStatus.ACTIVE)).thenReturn(Optional.of(stored));
        when(tokens.findByToken("valid")).thenReturn(Optional.of(stored));
        when(encoder.encode("  Password!  ")).thenReturn("hash");
        assertTrue(service.resetPassword("valid", "  Password!  "));
        var order = inOrder(tokens, users, encoder);
        order.verify(tokens).findByTokenAndStatus("valid", TokenStatus.ACTIVE);
        order.verify(encoder).encode("  Password!  ");
        order.verify(users).save(user);
        order.verify(tokens).findByToken("valid");
        order.verify(tokens).save(stored);
        assertEquals(TokenStatus.USED, stored.getStatus());
        assertEquals("hash", user.getPassword());
    }

    @ParameterizedTest
    @ValueSource(strings = {"encoder", "user-save", "status-lookup", "token-save"})
    void passwordChangeFailuresArePropagatedWithoutRetry(String stage) {
        var stored = token("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var failure = new DataAccessResourceFailureException("test " + stage);
        when(tokens.findByTokenAndStatus("valid", TokenStatus.ACTIVE)).thenReturn(Optional.of(stored));
        if (stage.equals("encoder")) {
            when(encoder.encode("Password!")).thenThrow(failure);
        } else {
            when(encoder.encode("Password!")).thenReturn("hash");
            if (stage.equals("user-save")) {
                doThrow(failure).when(users).save(user);
            } else if (stage.equals("status-lookup")) {
                when(tokens.findByToken("valid")).thenThrow(failure);
            } else {
                when(tokens.findByToken("valid")).thenReturn(Optional.of(stored));
                doThrow(failure).when(tokens).save(stored);
            }
        }
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.resetPassword("valid", "Password!")));
        verify(encoder).encode("Password!");
        if (stage.equals("encoder")) {
            assertEquals("old-hash", user.getPassword());
            verifyNoInteractions(users);
        }
        if (stage.equals("encoder") || stage.equals("user-save")) {
            assertEquals(TokenStatus.ACTIVE, stored.getStatus());
            verify(tokens, never()).findByToken(any());
            verify(tokens, never()).save(any());
        }
        verifyNoInteractions(queue);
    }

    @ParameterizedTest
    @EnumSource(TokenStatus.class)
    void existenceDoesNotMeanActiveUnexpiredOrEnabled(TokenStatus status) {
        user.setEnabled(false);
        when(tokens.findByToken("exists")).thenReturn(Optional.of(token("exists", status, NOW.minusDays(1))));
        assertTrue(service.existsToken("exists"));
        verifyNoInteractions(users, encoder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"validate", "reset", "exists", "status", "invalidate", "create"})
    void repositoryLookupFailuresAreNotConvertedIntoFalseOrSwallowed(String operation) {
        var failure = new DataAccessResourceFailureException("test lookup");
        if (operation.equals("validate") || operation.equals("reset")) {
            when(tokens.findByTokenAndStatus("value", TokenStatus.ACTIVE)).thenThrow(failure);
        } else if (operation.equals("invalidate") || operation.equals("create")) {
            when(tokens.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE)).thenThrow(failure);
        } else {
            when(tokens.findByToken("value")).thenThrow(failure);
        }
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, () -> {
            switch (operation) {
                case "validate" -> service.validateToken("value");
                case "reset" -> service.resetPassword("value", "Password!");
                case "exists" -> service.existsToken("value");
                case "status" -> service.setTokenStatus("value", TokenStatus.USED);
                case "invalidate" -> service.setInactiveTokensForUser(user);
                case "create" -> service.createToken(user);
            }
        }));
        verifyNoInteractions(users, encoder, config, queue);
    }

    @Test
    void cleanupCurrentlyThrowsAfterCallingDeleteBecauseLocalDateTimeHasNoOffset() {
        when(config.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)).thenReturn("P7D");
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(LocalDateTime::now).thenReturn(NOW);
            assertThrows(UnsupportedTemporalTypeException.class, service::deleteExpiredTokens);
        }
        verify(tokens).deleteByExpiryDateBefore(NOW.minusDays(7));
    }

    @Test
    void cleanupDeleteFailurePropagatesWithoutFormattingOrRetry() {
        when(config.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)).thenReturn("P7D");
        var failure = new DataAccessResourceFailureException("test cleanup");
        when(tokens.deleteByExpiryDateBefore(any())).thenThrow(failure);
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class, service::deleteExpiredTokens));
        verify(tokens).deleteByExpiryDateBefore(any());
        verifyNoMoreInteractions(tokens);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user-lookup", "queue-check"})
    void mailPreflightFailuresPropagateWithoutRotatingTokens(String stage) {
        var failure = new DataAccessResourceFailureException("test mail preflight");
        if (stage.equals("user-lookup")) {
            when(users.findByEmail("test@example.com")).thenThrow(failure);
        } else {
            when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(queue.hasOpenEmailForUserAndType(user, EmailType.PASSWORD_RESET)).thenThrow(failure);
        }
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.sendPasswordResetEmail("test@example.com")));
        verifyNoInteractions(tokens, config, encoder);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void absentOrDisabledMailRecipientDoesNotCreateTokensOrQueueMail(boolean present) throws IOException {
        user.setEnabled(false);
        when(users.findByEmail("test@example.com")).thenReturn(present ? Optional.of(user) : Optional.empty());
        assertFalse(service.sendPasswordResetEmail("test@example.com"));
        verifyNoInteractions(tokens, config, encoder, queue);
    }

    @Test
    void alreadyQueuedMailReturnsSuccessWithoutRotatingToken() throws IOException {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(queue.hasOpenEmailForUserAndType(user, EmailType.PASSWORD_RESET)).thenReturn(true);
        assertTrue(service.sendPasswordResetEmail("test@example.com"));
        verifyNoInteractions(tokens, config, encoder);
        verify(queue, never()).addEmailToQueue(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void realClasspathMailTemplateContainsTokenAndLogoButCurrentlyLeavesServiceNamePlaceholder(boolean german) throws IOException {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(config.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION)).thenReturn("PT1H");
        when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Reset Test Service");
        when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/app/");
        when(messages.getEmailResetPasswordSubject()).thenReturn("Reset subject");
        var session = mock(VaadinSession.class);
        when(session.getLocale()).thenReturn(german ? Locale.GERMAN : Locale.ENGLISH);
        VaadinSession.setCurrent(session);
        try {
            assertTrue(service.sendPasswordResetEmail("test@example.com"));
        } finally {
            VaadinSession.setCurrent(null);
        }
        var stored = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokens).save(stored.capture());
        var html = ArgumentCaptor.forClass(String.class);
        verify(queue).addEmailToQueue(same(user), eq("Reset subject"), html.capture(), eq(EmailType.PASSWORD_RESET));
        assertTrue(html.getValue().contains("https://example.invalid/app/reset-password/" + stored.getValue().getToken()));
        // The actual template has no SERVICE_NAME placeholder, although the service reads it.
        verify(config).getString(ConfigEntry.SERVICE_NAME);
        assertTrue(html.getValue().contains(ImageHelper.convertImageToBase64("META-INF/resources/custom-theme/service_logo.png")));
        assertFalse(html.getValue().contains("{{SERVICE_LOGO}}"));
        assertFalse(html.getValue().contains("{{RESET_PASSWORD_LINK}}"));
        // Confirmed template typo: the service replaces SERVICE_NAME, not SERVICES_NAME.
        assertTrue(html.getValue().contains("{{SERVICES_NAME}}"));
        verifyNoInteractions(encoder);
    }

    @Test
    void mailQueueFailurePropagatesInsteadOfClaimingSuccess() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(config.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION)).thenReturn("PT1H");
        when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Reset Test Service");
        when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
        when(messages.getEmailResetPasswordSubject()).thenReturn("subject");
        var failure = new DataAccessResourceFailureException("test queue");
        doThrow(failure).when(queue).addEmailToQueue(any(), any(), any(), any());
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.sendPasswordResetEmail("test@example.com")));
    }
}
