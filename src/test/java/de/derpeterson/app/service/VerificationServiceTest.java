package de.derpeterson.app.service;

import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
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

import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated service tests without application startup, SMTP or a database. */
@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    @Mock
    private MessageProperties messages;
    @Mock
    private VerificationTokenRepository tokens;
    @Mock
    private UserRepository users;
    @Mock
    private ConfigService config;
    @Mock
    private EmailQueueService queue;
    private VerificationService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new VerificationService(messages, tokens, users, config, queue, CLOCK);
        user = UserEntity.builder().id(1L).firstName("Test").lastName("User")
                .email("test@example.com").preferredLocale(Locale.ENGLISH).build();
        lenient().when(users.lockVerificationUser(1L)).thenReturn(Optional.of(user));
        lenient().when(tokens.findUserIdByToken("token")).thenReturn(Optional.of(1L));
    }

    @Test
    void creationInvalidatesAllActiveTokensAndPersistsANewUuidWithConfiguredLifetime() {
        var first = token(TokenStatus.ACTIVE, NOW.plusDays(1));
        var second = token(TokenStatus.ACTIVE, NOW.minusDays(1));
        when(tokens.findAllByUserEntityAndStatus(user, TokenStatus.ACTIVE)).thenReturn(List.of(first, second));
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT2H");

        String value = service.createToken(user);

        var captor = ArgumentCaptor.forClass(VerificationTokenEntity.class);
        verify(tokens, times(3)).save(captor.capture());
        var created = captor.getAllValues().getLast();
        assertEquals(TokenStatus.INACTIVE, first.getStatus());
        assertEquals(TokenStatus.INACTIVE, second.getStatus());
        assertEquals(UUID.fromString(value).toString(), value);
        assertEquals(value, created.getToken());
        assertSame(user, created.getUserEntity());
        assertEquals(TokenStatus.ACTIVE, created.getStatus());
        assertEquals(NOW.plusHours(2), created.getExpiryDate());
        verify(users, never()).save(any());
        verify(queue).hasOpenEmailForUserAndType(user, EmailType.VERIFICATION);
        verify(queue, never()).addEmailToQueue(any(), any(), any(), any());
    }

    @Test
    void invalidLifetimePropagatesRatherThanSavingANewToken() {
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("invalid");
        assertThrows(DateTimeParseException.class, () -> service.createToken(user));
        verify(tokens, never()).save(any());
        verify(tokens, never()).findAllByUserEntityAndStatus(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1H"})
    void nonPositiveLifetimeIsRejectedBeforeInvalidatingTokens(String duration) {
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn(duration);
        assertThrows(IllegalArgumentException.class, () -> service.createToken(user));
        verify(tokens, never()).findAllByUserEntityAndStatus(any(), any());
        verify(tokens, never()).save(any());
        verify(queue, never()).addEmailToQueue(any(), any(), any(), any());
    }

    @Test
    void positiveSubsecondLifetimeIsAccepted() {
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT0.000000001S");
        service.createToken(user);
        var captor = ArgumentCaptor.forClass(VerificationTokenEntity.class);
        verify(tokens).save(captor.capture());
        assertEquals(NOW.plusNanos(1), captor.getValue().getExpiryDate());
    }

    @Test
    void unrepresentableExpiryDoesNotInvalidateExistingTokens() {
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("P366000000000D");
        assertThrows(DateTimeException.class, () -> service.createToken(user));
        verify(tokens, never()).findAllByUserEntityAndStatus(any(), any());
        verify(tokens, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void consumedExpiredLinkOnlyConfirmsAnEnabledAccountWithoutWrites(boolean enabled) {
        user.setEnabled(enabled);
        when(tokens.findByTokenAndStatus("token", TokenStatus.ACTIVE)).thenReturn(Optional.empty());
        if (enabled) {
            when(tokens.findByTokenAndStatus("token", TokenStatus.USED))
                    .thenReturn(Optional.of(token(TokenStatus.USED, NOW.minusDays(1))));
        }
        assertEquals(enabled, service.validateToken("token"));
        verify(users, never()).save(any());
        verify(tokens, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void expirationBoundaryRejectsEqualityAndConsumesOnlyFutureTokens(long offsetNanos) {
        var entity = token(TokenStatus.ACTIVE, NOW.plusNanos(offsetNanos));
        when(tokens.findByTokenAndStatus("token", TokenStatus.ACTIVE)).thenReturn(Optional.of(entity));
        assertEquals(offsetNanos > 0, service.validateToken("token"));
        assertEquals(offsetNanos > 0, user.isEnabled());
        assertEquals(offsetNanos > 0 ? TokenStatus.USED : TokenStatus.EXPIRED, entity.getStatus());
        verify(users, times(offsetNanos > 0 ? 1 : 0)).save(user);
        verify(tokens).save(entity);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", "   "})
    void unknownOrEmptyTokenIsRejectedWithoutWrites(String value) {
        assertFalse(service.validateToken(value));
        verify(tokens).findUserIdByToken(value);
        verify(tokens, never()).save(any());
        verifyNoInteractions(users, queue);
    }

    @Test
    void enabledAccountIsNotSavedAndItsActiveTokenRemainsActive() {
        user.setEnabled(true);
        var entity = token(TokenStatus.ACTIVE, NOW.plusDays(1));
        when(tokens.findByTokenAndStatus("token", TokenStatus.ACTIVE)).thenReturn(Optional.of(entity));

        assertFalse(service.validateToken("token"));
        assertEquals(TokenStatus.ACTIVE, entity.getStatus());
        verify(users, never()).save(any());
        verify(tokens, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TokenStatus.class, names = {"INACTIVE", "USED", "EXPIRED"})
    void statusSetterOnlyCompletesActiveTokens(TokenStatus status) {
        var entity = token(TokenStatus.ACTIVE, NOW.plusDays(1));
        when(tokens.findByTokenAndStatus("token", TokenStatus.ACTIVE)).thenReturn(Optional.of(entity));
        service.setTokenStatus("token", status);
        assertEquals(status, entity.getStatus());
        verify(tokens).save(entity);
    }

    @Test
    void statusSetterCannotReactivateOrRewriteCompletedTokens() {
        assertThrows(IllegalArgumentException.class, () -> service.setTokenStatus("token", TokenStatus.ACTIVE));
        var entity = token(TokenStatus.USED, NOW.plusDays(1));
        when(tokens.findByToken("token")).thenReturn(Optional.of(entity));
        assertThrows(IllegalStateException.class, () -> service.setTokenStatus("token", TokenStatus.INACTIVE));
        service.setTokenStatus("token", TokenStatus.USED);
        verify(tokens, never()).save(any());
    }

    @Test
    void missingStatusTargetIsANoOp() {
        service.setTokenStatus("missing", TokenStatus.USED);
        verify(tokens, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void existenceReflectsRepositoryPresenceRegardlessOfStatus(boolean exists) {
        when(tokens.findByToken("token")).thenReturn(exists
                ? Optional.of(token(TokenStatus.EXPIRED, NOW.minusDays(1))) : Optional.empty());
        assertEquals(exists, service.existsToken("token"));
    }

    @Test
    void deletionReturnsRepositoryCount() {
        when(tokens.deleteByToken("token")).thenReturn(1);
        assertEquals(1, service.deleteToken("token"));
    }

    @ParameterizedTest
    @CsvSource({"token,en", "email,en", "user,en", "token,de", "email,de", "user,de"})
    void resendForDisabledUserRotatesTokenAndQueuesRealClasspathTemplate(String entry, String language) throws IOException {
        prepareResend(entry);
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1H");
        when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Example Service");
        when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.com/app/");
        when(messages.getEmailVerificationSubject()).thenReturn("Verify account");
        try (var locale = mockStatic(CustomI18NProvider.class)) {
            locale.when(CustomI18NProvider::getCurrentLocale).thenReturn(Locale.forLanguageTag(language));
            assertTrue(resend(entry));
        }

        var tokenCaptor = ArgumentCaptor.forClass(VerificationTokenEntity.class);
        verify(tokens).save(tokenCaptor.capture());
        var body = ArgumentCaptor.forClass(String.class);
        verify(queue).addEmailToQueue(eq(user), eq("Verify account"), body.capture(), eq(EmailType.VERIFICATION));
        assertTrue(body.getValue().contains("https://example.com/app/verification/" + tokenCaptor.getValue().getToken()));
        assertTrue(body.getValue().contains("Example Service"));
        assertTrue(body.getValue().contains("Test"));
        assertTrue(body.getValue().contains("User"));
        assertTrue(body.getValue().contains("data:image/png;base64,"));
        for (String placeholder : List.of("SERVICE_LOGO", "SERVICE_NAME", "FIRST_NAME", "LAST_NAME", "VERIFICATION_LINK")) {
            assertFalse(body.getValue().contains("{{" + placeholder + "}}"));
        }
        assertTrue(body.getValue().contains("© 2025 Example Service. "
                + (language.equals("de") ? "Alle Rechte vorbehalten." : "All rights reserved.")));
        assertFalse(body.getValue().contains("{{"));
        assertFalse(user.isEnabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {"token", "email", "user"})
    void openQueueEntryReturnsSuccessWithoutRotatingOrQueuingAgain(String entry) throws IOException {
        prepareResend(entry);
        when(queue.hasOpenEmailForUserAndType(user, EmailType.VERIFICATION)).thenReturn(true);
        assertTrue(resend(entry));
        verify(tokens, never()).save(any());
        verify(queue, never()).addEmailToQueue(any(), any(), any(), any());
        verifyNoInteractions(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"token", "email", "user"})
    void resendForEnabledAccountIsRejected(String entry) throws IOException {
        prepareResend(entry);
        user.setEnabled(true);
        assertFalse(resend(entry));
        verifyNoInteractions(queue, config);
        verify(tokens, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"token", "email"})
    void resendForUnknownTokenOrEmailIsRejected(String entry) throws IOException {
        if (entry.equals("token")) {
            when(tokens.findUserIdByToken("token")).thenReturn(Optional.empty());
        }
        assertFalse(resend(entry));
        verifyNoInteractions(queue, config);
    }

    @Test
    void cleanupDeletesUsingConfiguredCutoffWithoutFormattingFailure() {
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION)).thenReturn("P7D");
        assertDoesNotThrow(service::deleteExpiredTokens);
        var cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokens).deleteByExpiryDateBefore(cutoff.capture());
        assertEquals(NOW.minusDays(7), cutoff.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1H", "invalid"})
    void invalidRetentionDurationDoesNotDeleteTokens(String duration) {
        when(config.getString(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION)).thenReturn(duration);
        Class<? extends RuntimeException> expected = duration.equals("invalid")
                ? DateTimeParseException.class : IllegalArgumentException.class;
        assertThrows(expected, service::deleteExpiredTokens);
        verifyNoInteractions(tokens);
    }

    @Test
    void disappearedAccountRejectsValidationDeletionStatusChangesAndResend() throws IOException {
        when(users.lockVerificationUser(1L)).thenReturn(Optional.empty());
        assertFalse(service.validateToken("token"));
        assertEquals(0, service.deleteToken("token"));
        service.setTokenStatus("token", TokenStatus.INACTIVE);
        assertFalse(service.sendVerificationEmailByUser(user));
        assertThrows(IllegalStateException.class, () -> service.createToken(user));
        verify(tokens, never()).save(any());
        verify(tokens, never()).deleteByToken(any());
        verifyNoInteractions(config, queue);
    }

    @Test
    void missingUserAndNullStatusAreRejectedWithoutRepositoryAccess() throws IOException {
        assertFalse(service.sendVerificationEmailByUser(null));
        assertFalse(service.sendVerificationEmailByUser(UserEntity.builder().build()));
        assertThrows(IllegalArgumentException.class, () -> service.setTokenStatus("token", null));
        verifyNoInteractions(tokens, users, queue, config);
    }

    private VerificationTokenEntity token(TokenStatus status, LocalDateTime expiry) {
        return new VerificationTokenEntity(null, "token", user, expiry, status);
    }

    private void prepareResend(String entry) {
        if (entry.equals("token")) {
            // Scalar reference from the fixture; account state is checked under its lock.
        } else if (entry.equals("email")) {
            when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        }
    }

    private boolean resend(String entry) throws IOException {
        return switch (entry) {
            case "token" -> service.sendVerificationEmailByToken("token");
            case "email" -> service.sendVerificationEmailByEmail(user.getEmail());
            case "user" -> service.sendVerificationEmailByUser(user);
            default -> throw new IllegalArgumentException(entry);
        };
    }
}
