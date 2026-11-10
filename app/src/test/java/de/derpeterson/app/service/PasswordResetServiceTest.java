package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.email.EmailLogo;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.PasswordResetService.LinkState;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.HtmlUtils;

/** Service logic and exact time boundaries; no application, database or SMTP. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 8, 12, 0);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-08T10:00:00Z"), ZoneId.of("Europe/Berlin"));
  @Mock private MessageProperties messageProperties;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private ConfigService configService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailQueueService emailQueueService;
  @Mock private LoginAttemptService loginAttemptService;
  private PasswordResetService passwordResetService;
  private UserEntity testUserEntity;

  @BeforeEach
  void setup() {
    // Neue Konfiguration gehört zur Mailfixture; andere Fälle lesen diese Werte nicht.
    lenient()
        .when(configService.getString(ConfigEntry.COPYRIGHT_TEXT))
        .thenReturn(ConfigEntry.COPYRIGHT_TEXT.getDefaultValueString());
    lenient()
        .when(messageProperties.getAccountCopyrightTemplate())
        .thenAnswer(call ->
            MessageProperties.forLocale(
                    new CustomI18NProvider(), CustomI18NProvider.getCurrentLocale())
                .getAccountCopyrightTemplate());
    passwordResetService =
        new PasswordResetService(
            messageProperties,
            passwordResetTokenRepository,
            userRepository,
            configService,
            passwordEncoder,
            emailQueueService,
            loginAttemptService,
            mock(UserNotificationService.class),
            CLOCK);
    testUserEntity =
        UserEntity.builder()
            .id(1L)
            .email("test@example.com")
            .password("old-hash")
            .enabled(true)
            .build();
    lenient().when(userRepository.lockVerificationUser(1L)).thenReturn(Optional.of(testUserEntity));
    lenient()
        .when(passwordResetTokenRepository.findUserIdByToken(anyString()))
        .thenReturn(Optional.of(1L));
  }

  private PasswordResetTokenEntity token(String value, TokenStatus status, LocalDateTime expiry) {
    return new PasswordResetTokenEntity(7L, value, testUserEntity, expiry, status);
  }

  @Test
  void explicitClockMustNotBeNull() {
    assertThrows(
        NullPointerException.class,
        () ->
            new PasswordResetService(
                messageProperties,
                passwordResetTokenRepository,
                userRepository,
                configService,
                passwordEncoder,
                emailQueueService,
                loginAttemptService,
                mock(UserNotificationService.class),
                null));
  }

  @Test
  void creationInvalidatesActiveTokensBeforeSavingANewUuidForTheSameUser() {
    PasswordResetTokenEntity old = token("old", TokenStatus.ACTIVE, NOW.plusDays(1));
    when(passwordResetTokenRepository.findAllByUserEntityAndStatus(
            testUserEntity, TokenStatus.ACTIVE))
        .thenReturn(List.of(old));
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("PT1H");
    {
      String value = passwordResetService.createToken(testUserEntity);

      assertEquals(value, UUID.fromString(value).toString());
      assertNotEquals(old.getToken(), value);
      ArgumentCaptor<PasswordResetTokenEntity> saved =
          ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
      InOrder order = inOrder(passwordResetTokenRepository, configService);
      order.verify(configService).getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION);
      order
          .verify(passwordResetTokenRepository)
          .findAllByUserEntityAndStatus(testUserEntity, TokenStatus.ACTIVE);
      order.verify(passwordResetTokenRepository).save(old);
      order.verify(passwordResetTokenRepository).save(saved.capture());

      assertEquals(TokenStatus.INACTIVE, old.getStatus());
      assertNull(saved.getValue().getId());
      assertSame(testUserEntity, saved.getValue().getUserEntity());
      assertEquals(value, saved.getValue().getToken());
      assertEquals(NOW.plusHours(1), saved.getValue().getExpiryDate());
      assertEquals(TokenStatus.ACTIVE, saved.getValue().getStatus());
      order.verifyNoMoreInteractions();
    }
    verify(userRepository).lockVerificationUser(1L);
    verifyNoInteractions(passwordEncoder);
    verify(emailQueueService).hasOpenEmailForUserAndType(testUserEntity, EmailType.PASSWORD_RESET);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT0S", "-PT1S", "PT0.000000001S", "PT1S", "PT59.999999999S"})
  void creationRejectsBelowMinimumDurationsBeforeInvalidation(String duration) {
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn(duration);

    assertThrows(
        IllegalArgumentException.class, () -> passwordResetService.createToken(testUserEntity));
    verifyNoInteractions(passwordResetTokenRepository);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"broken"})
  void invalidDurationPropagatesWithoutSavingANewToken(String duration) {
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn(duration);

    assertThrows(RuntimeException.class, () -> passwordResetService.createToken(testUserEntity));
    verifyNoInteractions(passwordResetTokenRepository);
    verify(userRepository).lockVerificationUser(1L);
    verify(emailQueueService).hasOpenEmailForUserAndType(testUserEntity, EmailType.PASSWORD_RESET);
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  void invalidationChangesOnlyTheReturnedActiveTokensAndLeavesPasswordUntouched() {
    PasswordResetTokenEntity first = token("first", TokenStatus.ACTIVE, NOW);
    PasswordResetTokenEntity second = token("second", TokenStatus.ACTIVE, NOW);
    when(passwordResetTokenRepository.findAllByUserEntityAndStatus(
            testUserEntity, TokenStatus.ACTIVE))
        .thenReturn(List.of(first, second));
    passwordResetService.setInactiveTokensForUser(testUserEntity);

    assertEquals(TokenStatus.INACTIVE, first.getStatus());
    assertEquals(TokenStatus.INACTIVE, second.getStatus());
    verify(passwordResetTokenRepository).save(first);
    verify(passwordResetTokenRepository).save(second);
    assertEquals("old-hash", testUserEntity.getPassword());
    verify(userRepository).lockVerificationUser(1L);
    verifyNoInteractions(passwordEncoder, emailQueueService);
  }

  @ParameterizedTest
  @EnumSource(TokenStatus.class)
  void statusSetterRejectsReactivationAndChangesOfTerminalStatus(TokenStatus status) {
    PasswordResetTokenEntity stored = token("used", TokenStatus.USED, NOW.plusDays(1));
    if (status == TokenStatus.ACTIVE) {
      assertThrows(
          IllegalArgumentException.class,
          () -> passwordResetService.setTokenStatus("used", status));
      verifyNoInteractions(passwordResetTokenRepository, userRepository);
    } else {
      when(passwordResetTokenRepository.findByToken("used")).thenReturn(Optional.of(stored));
      if (status == TokenStatus.USED) {
        passwordResetService.setTokenStatus("used", status);
      } else {
        assertThrows(
            IllegalStateException.class, () -> passwordResetService.setTokenStatus("used", status));
      }
      verify(passwordResetTokenRepository, never()).save(any());
    }
    assertEquals(TokenStatus.USED, stored.getStatus());
    verifyNoInteractions(passwordEncoder, emailQueueService);
  }

  @ParameterizedTest
  @EnumSource(
      value = TokenStatus.class,
      names = {"USED", "INACTIVE", "EXPIRED"})
  void activeTokenMayTransitionToATerminalStatus(TokenStatus status) {
    PasswordResetTokenEntity stored = token("active", TokenStatus.ACTIVE, NOW.plusDays(1));
    when(passwordResetTokenRepository.findByToken("active")).thenReturn(Optional.of(stored));
    passwordResetService.setTokenStatus("active", status);

    assertEquals(status, stored.getStatus());
    InOrder order = inOrder(passwordResetTokenRepository, userRepository);
    order.verify(passwordResetTokenRepository).findUserIdByToken("active");
    order.verify(userRepository).lockVerificationUser(1L);
    order.verify(passwordResetTokenRepository).findByToken("active");
    order.verify(passwordResetTokenRepository).save(stored);
  }

  @ParameterizedTest
  @ValueSource(strings = {"create", "validate", "reset", "status", "invalidate", "mail"})
  void accountLockFailurePropagatesBeforeTokenMutation(String operation) {
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("test account lock");
    when(userRepository.lockVerificationUser(1L)).thenThrow(failure);
    if (operation.equals("mail")) {
      when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserEntity));
    }
    Executable operationUnderTest =
        switch (operation) {
          case "create" -> () -> passwordResetService.createToken(testUserEntity);
          case "validate" -> () -> passwordResetService.inspectToken("valid");
          case "reset" -> () -> passwordResetService.resetPassword("valid", "Password!");
          case "status" -> () -> passwordResetService.setTokenStatus("valid", TokenStatus.USED);
          case "invalidate" -> () -> passwordResetService.setInactiveTokensForUser(testUserEntity);
          case "mail" -> () -> passwordResetService.sendPasswordResetEmail("test@example.com");
          default -> throw new IllegalArgumentException("Unknown fixture operation: " + operation);
        };

    assertSame(failure, assertThrows(DataAccessResourceFailureException.class, operationUnderTest));
    verify(passwordResetTokenRepository, never()).save(any());
    verify(userRepository, never()).save(any());
    verifyNoInteractions(passwordEncoder, emailQueueService);
    verifyMaintenanceOnly(
        Set.of("create", "validate", "reset", "mail").contains(operation) ? 1 : 0);
  }

  @Test
  void openMailPreventsDirectRotationBeforeInvalidation() {
    when(emailQueueService.hasOpenEmailForUserAndType(testUserEntity, EmailType.PASSWORD_RESET))
        .thenReturn(true);

    assertThrows(
        IllegalStateException.class, () -> passwordResetService.createToken(testUserEntity));
    verifyNoInteractions(passwordResetTokenRepository, passwordEncoder);
    verifyMaintenanceOnly(1);
  }

  @Test
  void deletedAccountCannotBeUsedForTokenCreation() {
    when(userRepository.lockVerificationUser(1L)).thenReturn(Optional.empty());

    assertThrows(
        IllegalStateException.class, () -> passwordResetService.createToken(testUserEntity));
    verifyNoInteractions(passwordResetTokenRepository, passwordEncoder, emailQueueService);
    verifyMaintenanceOnly(1);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"unknown"})
  void unknownOrEmptyTokensAreNotValidAndDoNotReset(String value) {
    when(passwordResetTokenRepository.findUserIdByToken(value)).thenReturn(Optional.empty());

    assertEquals(LinkState.INVALID, passwordResetService.inspectToken(value));
    assertFalse(passwordResetService.resetPassword(value, "Password!"));
    passwordResetService.setTokenStatus(value, TokenStatus.USED);

    verify(passwordResetTokenRepository, times(3)).findUserIdByToken(value);
    verifyNoMoreInteractions(passwordResetTokenRepository);
    verifyNoInteractions(userRepository, passwordEncoder, emailQueueService);
    verifyMaintenanceOnly(2);
  }

  @ParameterizedTest
  @EnumSource(
      value = TokenStatus.class,
      names = {"EXPIRED", "USED", "INACTIVE"})
  void closedLinksRetainTheirMeaningWithoutChangingPasswords(TokenStatus status) {
    PasswordResetTokenEntity stored = token("closed", status, NOW.minusDays(1));
    when(passwordResetTokenRepository.findByToken("closed")).thenReturn(Optional.of(stored));

    assertEquals(
        status == TokenStatus.EXPIRED ? LinkState.EXPIRED : LinkState.INVALID,
        passwordResetService.inspectToken("closed"));
    assertEquals(
        status == TokenStatus.EXPIRED ? LinkState.EXPIRED : LinkState.INVALID,
        passwordResetService.inspectToken("closed"));
    assertEquals(status, stored.getStatus());
    verify(passwordResetTokenRepository, never()).save(any());
    verifyNoInteractions(passwordEncoder, emailQueueService);
  }

  @ParameterizedTest
  @CsvSource({"false,-1", "false,0", "false,1", "true,-1", "true,0", "true,1"})
  void expiryIsExclusiveAndResetRechecksItWithTheInjectedLocalClock(
      boolean resetPassword, long nanoseconds) {
    PasswordResetTokenEntity stored =
        token("boundary", TokenStatus.ACTIVE, NOW.plusNanos(nanoseconds));
    if (resetPassword) {
      when(passwordResetTokenRepository.findByTokenAndStatus("boundary", TokenStatus.ACTIVE))
          .thenReturn(Optional.of(stored));
    } else {
      when(passwordResetTokenRepository.findByToken("boundary")).thenReturn(Optional.of(stored));
    }
    if (resetPassword && nanoseconds > 0) {
      when(passwordEncoder.encode("Password!")).thenReturn("new-hash");
    }
    boolean result =
        resetPassword
            ? passwordResetService.resetPassword("boundary", "Password!")
            : passwordResetService.inspectToken("boundary") == LinkState.VALID;

    assertEquals(nanoseconds > 0, result);
    TokenStatus expectedStatus = TokenStatus.ACTIVE;
    if (nanoseconds <= 0) {
      expectedStatus = TokenStatus.EXPIRED;
    } else if (resetPassword) {
      expectedStatus = TokenStatus.USED;
    }
    assertEquals(expectedStatus, stored.getStatus());
    if (resetPassword && nanoseconds > 0) {
      assertEquals("new-hash", testUserEntity.getPassword());
      verify(userRepository).save(testUserEntity);
    } else {
      assertEquals("old-hash", testUserEntity.getPassword());
      verify(userRepository, never()).save(any());
      verifyNoInteractions(passwordEncoder);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void disabledAccountRejectsBothValidationAndResetWithoutConsumingToken(boolean resetPassword) {
    testUserEntity.setEnabled(false);
    PasswordResetTokenEntity stored =
        token("disabled", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    if (resetPassword) {
      when(passwordResetTokenRepository.findByTokenAndStatus("disabled", TokenStatus.ACTIVE))
          .thenReturn(Optional.of(stored));
    }
    assertFalse(
        resetPassword
            ? passwordResetService.resetPassword("disabled", "Password!")
            : passwordResetService.inspectToken("disabled") == LinkState.VALID);
    assertEquals(TokenStatus.ACTIVE, stored.getStatus());
    verify(passwordResetTokenRepository, never()).save(any());
    verify(userRepository, never()).save(any());
    verifyNoInteractions(passwordEncoder);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "short!A", "lowercase!", "NoSpecialCharacter"})
  void invalidPasswordsFailBeforeAnyTokenLookupOrSideEffect(String password) {
    assertThrows(
        IllegalArgumentException.class,
        () -> passwordResetService.resetPassword("valid", password));
    verifyNoInteractions(
        passwordResetTokenRepository,
        userRepository,
        passwordEncoder,
        configService,
        emailQueueService);
  }

  @Test
  void securePasswordIsEncodedVerbatimAndTokenConsumedAfterUserSave() {
    PasswordResetTokenEntity stored =
        token("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    when(passwordResetTokenRepository.findByTokenAndStatus("valid", TokenStatus.ACTIVE))
        .thenReturn(Optional.of(stored));
    when(passwordEncoder.encode("  Password!  ")).thenReturn("hash");

    assertTrue(passwordResetService.resetPassword("valid", "  Password!  "));
    InOrder order = inOrder(passwordResetTokenRepository, userRepository, passwordEncoder);
    order.verify(passwordResetTokenRepository).findByTokenAndStatus("valid", TokenStatus.ACTIVE);
    order.verify(passwordEncoder).encode("  Password!  ");
    order.verify(userRepository).save(testUserEntity);
    order.verify(passwordResetTokenRepository).save(stored);

    assertEquals(TokenStatus.USED, stored.getStatus());
    assertEquals("hash", testUserEntity.getPassword());
  }

  @ParameterizedTest
  @ValueSource(strings = {"encoder", "user-save", "token-save"})
  void passwordChangeFailuresArePropagatedWithoutRetry(String stage) {
    PasswordResetTokenEntity stored =
        token("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("test " + stage);
    when(passwordResetTokenRepository.findByTokenAndStatus("valid", TokenStatus.ACTIVE))
        .thenReturn(Optional.of(stored));
    if (stage.equals("encoder")) {
      when(passwordEncoder.encode("Password!")).thenThrow(failure);
    } else {
      when(passwordEncoder.encode("Password!")).thenReturn("hash");
      if (stage.equals("user-save")) {
        doThrow(failure).when(userRepository).save(testUserEntity);
      } else {
        doThrow(failure).when(passwordResetTokenRepository).save(stored);
      }
    }
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> passwordResetService.resetPassword("valid", "Password!")));
    verify(passwordEncoder).encode("Password!");
    if (stage.equals("encoder")) {
      assertEquals("old-hash", testUserEntity.getPassword());
      verify(userRepository, never()).save(any());
    }
    if (stage.equals("encoder") || stage.equals("user-save")) {
      assertEquals(TokenStatus.ACTIVE, stored.getStatus());
      verify(passwordResetTokenRepository, never()).findByToken(any());
      verify(passwordResetTokenRepository, never()).save(any());
    }
    verifyNoInteractions(emailQueueService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"validate", "reset", "status", "invalidate", "create"})
  void repositoryLookupFailuresAreNotConvertedIntoFalseOrSwallowed(String operation) {
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("test lookup");
    if (operation.equals("create")) {
      when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
          .thenReturn("PT1H");
    }
    if (operation.equals("validate")) {
      when(passwordResetTokenRepository.findByToken("value")).thenThrow(failure);
    } else if (operation.equals("reset")) {
      when(passwordResetTokenRepository.findByTokenAndStatus("value", TokenStatus.ACTIVE))
          .thenThrow(failure);
    } else if (operation.equals("invalidate") || operation.equals("create")) {
      when(passwordResetTokenRepository.findAllByUserEntityAndStatus(
              testUserEntity, TokenStatus.ACTIVE))
          .thenThrow(failure);
    } else {
      when(passwordResetTokenRepository.findByToken("value")).thenThrow(failure);
    }
    Executable operationUnderTest =
        switch (operation) {
          case "validate" -> () -> passwordResetService.inspectToken("value");
          case "reset" -> () -> passwordResetService.resetPassword("value", "Password!");
          case "status" -> () -> passwordResetService.setTokenStatus("value", TokenStatus.USED);
          case "invalidate" -> () -> passwordResetService.setInactiveTokensForUser(testUserEntity);
          case "create" -> () -> passwordResetService.createToken(testUserEntity);
          default -> throw new IllegalArgumentException("Unknown fixture operation: " + operation);
        };

    assertSame(failure, assertThrows(DataAccessResourceFailureException.class, operationUnderTest));
    verify(userRepository, never()).save(any());
    verifyNoInteractions(passwordEncoder);
    if (!operation.equals("create")) {
      verifyMaintenanceOnly(Set.of("validate", "reset").contains(operation) ? 1 : 0);
    }
    if (operation.equals("create")) {
      verify(emailQueueService)
          .hasOpenEmailForUserAndType(testUserEntity, EmailType.PASSWORD_RESET);
    } else {
      verifyNoInteractions(emailQueueService);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 3})
  void cleanupCompletesWithAnExactLocalCutoffEvenWhenNothingIsDeleted(int deleted) {
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)).thenReturn("P7D");
    when(passwordResetTokenRepository.deleteByExpiryDateBefore(NOW.minusDays(7)))
        .thenReturn(deleted);

    assertDoesNotThrow(passwordResetService::deleteExpiredTokens);
    verify(passwordResetTokenRepository).deleteByExpiryDateBefore(NOW.minusDays(7));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"broken", "PT0S", "-PT1S", "PT1M", "PT119.999999999S"})
  void invalidRetentionNeverDeletesTokens(String duration) {
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION))
        .thenReturn(duration);

    assertThrows(RuntimeException.class, passwordResetService::deleteExpiredTokens);
    verifyNoInteractions(passwordResetTokenRepository);
  }

  @Test
  void minimumDurationAndNonUtcClockRetainLocalTime() {
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("PT1M");
    passwordResetService.createToken(testUserEntity);
    ArgumentCaptor<PasswordResetTokenEntity> saved =
        ArgumentCaptor.forClass(PasswordResetTokenEntity.class);

    verify(passwordResetTokenRepository).save(saved.capture());
    assertEquals(NOW.plusMinutes(1), saved.getValue().getExpiryDate());
  }

  @Test
  void minimumRetentionUsesExpiryBasedCutoff() {
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION))
        .thenReturn("PT2M");
    passwordResetService.deleteExpiredTokens();

    verify(passwordResetTokenRepository).deleteByExpiryDateBefore(NOW.minusMinutes(2));
  }

  @Test
  void cleanupDeleteFailurePropagatesWithoutFormattingOrRetry() {
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)).thenReturn("P7D");
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("test cleanup");
    when(passwordResetTokenRepository.deleteByExpiryDateBefore(any())).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class, passwordResetService::deleteExpiredTokens));
    verify(passwordResetTokenRepository).deleteByExpiryDateBefore(any());
    verifyNoMoreInteractions(passwordResetTokenRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {"user-lookup", "queue-check"})
  void mailPreflightFailuresPropagateWithoutRotatingTokens(String stage) {
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("test mail preflight");
    if (stage.equals("user-lookup")) {
      when(userRepository.findByEmail("test@example.com")).thenThrow(failure);
    } else {
      when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserEntity));
      when(emailQueueService.hasOpenEmailForUserAndType(testUserEntity, EmailType.PASSWORD_RESET))
          .thenThrow(failure);
    }
    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> passwordResetService.sendPasswordResetEmail("test@example.com")));
    verifyNoInteractions(passwordResetTokenRepository, passwordEncoder);
    verifyMaintenanceOnly(1);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void absentOrDisabledMailRecipientDoesNotCreateTokensOrQueueMail(boolean present)
      throws IOException {
    testUserEntity.setEnabled(false);
    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(present ? Optional.of(testUserEntity) : Optional.empty());

    assertFalse(passwordResetService.sendPasswordResetEmail("test@example.com"));
    verifyNoInteractions(passwordResetTokenRepository, passwordEncoder, emailQueueService);
    verifyMaintenanceOnly(1);
  }

  @Test
  void alreadyQueuedMailReturnsSuccessWithoutRotatingToken() throws IOException {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserEntity));
    when(emailQueueService.hasOpenEmailForUserAndType(testUserEntity, EmailType.PASSWORD_RESET))
        .thenReturn(true);

    assertTrue(passwordResetService.sendPasswordResetEmail("test@example.com"));
    verifyNoInteractions(passwordResetTokenRepository, passwordEncoder);
    verifyMaintenanceOnly(1);

    verify(emailQueueService, never()).addEmailToQueue(any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void realClasspathMailTemplateReplacesAllPlaceholdersInBothLanguages(boolean german)
      throws IOException {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserEntity));
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("PT1H");
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Reset Test Service");
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/app/");
    when(messageProperties.getEmailResetPasswordSubject()).thenReturn("Reset subject");
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenReturn(german ? Locale.GERMAN : Locale.ENGLISH);
    VaadinSession.setCurrent(vaadinSession);
    try {
      assertTrue(passwordResetService.sendPasswordResetEmail("test@example.com"));
    } finally {
      VaadinSession.setCurrent(null);
    }
    ArgumentCaptor<PasswordResetTokenEntity> stored =
        ArgumentCaptor.forClass(PasswordResetTokenEntity.class);

    verify(passwordResetTokenRepository).save(stored.capture());
    ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);

    verify(emailQueueService)
        .addEmailToQueue(
            same(testUserEntity),
            eq("Reset subject"),
            html.capture(),
            eq(EmailType.PASSWORD_RESET));
    assertTrue(
        html.getValue()
            .contains(
                "https://example.invalid/app/reset-password/" + stored.getValue().getToken()));
    verify(configService).getString(ConfigEntry.SERVICE_NAME);
    assertTrue(html.getValue().contains("cid:" + EmailLogo.CONTENT_ID));
    String expectedFooter =
        MessageProperties.forLocale(
                new CustomI18NProvider(), german ? Locale.GERMAN : Locale.ENGLISH)
            .getAccountCopyrightTemplate()
            .replace("{year}", String.valueOf(Year.now(CLOCK).getValue()))
            .replace("{serviceName}", "Reset Test Service");

    assertTrue(HtmlUtils.htmlUnescape(html.getValue()).contains(expectedFooter));
    assertFalse(html.getValue().contains("{{SERVICE_LOGO}}"));
    assertFalse(html.getValue().contains("{{RESET_PASSWORD_LINK}}"));
    assertTrue(html.getValue().contains("Reset Test Service"));
    assertFalse(html.getValue().contains("{{"));
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  void customCopyrightIsResolvedFromConfigurationAndEscapedInResetMail() throws IOException {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserEntity));
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("PT1H");
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Company & Partners");
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT))
        .thenReturn("© {year} {serviceName} <b>Custom footer</b>");
    when(messageProperties.getEmailResetPasswordSubject()).thenReturn("subject");

    assertTrue(passwordResetService.sendPasswordResetEmail("test@example.com"));
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

    verify(emailQueueService)
        .addEmailToQueue(
            eq(testUserEntity), eq("subject"), body.capture(), eq(EmailType.PASSWORD_RESET));
    assertTrue(
        body.getValue()
            .contains("&copy; 2026 Company &amp; Partners &lt;b&gt;Custom footer&lt;/b&gt;"));
    assertFalse(body.getValue().contains("<b>Custom footer</b>"));
    assertFalse(body.getValue().contains("{year}"));
    verify(messageProperties, never()).getAccountCopyrightTemplate();
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void htmlAndMjmlSourcesUseTheSameSupportedPlaceholders(String language) throws IOException {
    for (String extension : List.of("html", "mjml")) {
      try (InputStream input =
          new ClassPathResource("email/reset_password_" + language + "." + extension)
              .getInputStream()) {
        String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        // Findet {{...}}-Platzhalter; Gruppe 1 erfasst mindestens ein Zeichen bis zur ersten
        // schließenden Klammer.
        Set<String> placeholders =
            Pattern.compile("\\{\\{([^}]+)}}")
                .matcher(source)
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());

        assertEquals(
            Set.of(
                "SERVICE_NAME",
                "SERVICE_LOGO",
                "RESET_PASSWORD_LINK",
                "COPYRIGHT_TEXT",
                "INTER_FONT_STYLESHEET"),
            placeholders);
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"success", "read", "close", "read-and-close"})
  void templateStreamIsClosedOnSuccessAndIoFailures(String stage) throws IOException {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserEntity));
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("PT1H");
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Reset Test Service");
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
    when(messageProperties.getEmailResetPasswordSubject()).thenReturn("subject");
    IOException readFailure = new IOException("isolated template read");
    IOException closeFailure = new IOException("isolated template close");
    AtomicBoolean closed = new AtomicBoolean();
    InputStream template =
        new InputStream() {
          private final InputStream content =
              new ByteArrayInputStream("{{SERVICE_NAME}}".getBytes(StandardCharsets.UTF_8));

          @Override
          public int read() throws IOException {
            if (stage.contains("read")) {
              throw readFailure;
            }
            return content.read();
          }

          @Override
          public void close() throws IOException {
            closed.set(true);
            if (stage.contains("close")) {
              throw closeFailure;
            }
          }
        };
    // Keep real classpath coverage above; intercept only resource construction here.
    try (var _ =
        mockConstruction(
            ClassPathResource.class,
            (resource, context) ->
                when(resource.getInputStream())
                    .thenReturn(
                        context.arguments().getFirst().toString().startsWith("email/")
                            ? template
                            : new ByteArrayInputStream(new byte[] {1})))) {
      if (stage.equals("success")) {
        assertTrue(passwordResetService.sendPasswordResetEmail("test@example.com"));
        verify(emailQueueService)
            .addEmailToQueue(
                same(testUserEntity),
                eq("subject"),
                eq("Reset Test Service"),
                eq(EmailType.PASSWORD_RESET));
      } else {
        IOException thrown =
            assertThrows(
                IOException.class,
                () -> passwordResetService.sendPasswordResetEmail("test@example.com"));

        assertSame(stage.contains("read") ? readFailure : closeFailure, thrown);
        if (stage.equals("read-and-close")) {
          assertArrayEquals(new Throwable[] {closeFailure}, thrown.getSuppressed());
        }
        verify(emailQueueService, never()).addEmailToQueue(any(), any(), any(), any());
      }
    }
    assertTrue(closed.get());
  }

  @Test
  void mailQueueFailurePropagatesInsteadOfClaimingSuccess() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUserEntity));
    when(configService.getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION))
        .thenReturn("PT1H");
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Reset Test Service");
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
    when(messageProperties.getEmailResetPasswordSubject()).thenReturn("subject");
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("test queue");
    doThrow(failure).when(emailQueueService).addEmailToQueue(any(), any(), any(), any());

    assertSame(
        failure,
        assertThrows(
            DataAccessResourceFailureException.class,
            () -> passwordResetService.sendPasswordResetEmail("test@example.com")));
  }

  private void verifyMaintenanceOnly(int reads) {
    verify(configService, times(reads)).getBoolean(ConfigEntry.MAINTENANCE_MODE);
    verifyNoMoreInteractions(configService);
  }
}
