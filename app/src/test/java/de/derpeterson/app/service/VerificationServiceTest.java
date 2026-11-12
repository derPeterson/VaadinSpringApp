package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.helper.email.EmailLogo;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.HtmlUtils;

/** Isolated service tests without application startup, SMTP or a database. */
@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
  @Mock private MessageProperties messageProperties;
  @Mock private VerificationTokenRepository verificationTokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private ConfigService configService;
  @Mock private EmailQueueService emailQueueService;
  private VerificationService verificationService;
  private UserEntity testUserEntity;

  @BeforeEach
  void setUp() {
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
    verificationService =
        new VerificationService(
            messageProperties,
            verificationTokenRepository,
            userRepository,
            configService,
            emailQueueService,
            CLOCK);
    testUserEntity =
        UserEntity.builder()
            .id(1L)
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .preferredLocale(Locale.ENGLISH)
            .build();
    lenient().when(userRepository.lockVerificationUser(1L)).thenReturn(Optional.of(testUserEntity));
    lenient()
        .when(verificationTokenRepository.findUserIdByToken("token"))
        .thenReturn(Optional.of(1L));
  }

  @Test
  void creationInvalidatesAllActiveTokensAndPersistsANewUuidWithConfiguredLifetime() {
    VerificationTokenEntity first = token(TokenStatus.ACTIVE, NOW.plusDays(1));
    VerificationTokenEntity second = token(TokenStatus.ACTIVE, NOW.minusDays(1));
    when(verificationTokenRepository.findAllByUserEntityAndStatus(
            testUserEntity, TokenStatus.ACTIVE))
        .thenReturn(List.of(first, second));
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT2H");

    String value = verificationService.createToken(testUserEntity);

    ArgumentCaptor<VerificationTokenEntity> captor =
        ArgumentCaptor.forClass(VerificationTokenEntity.class);

    verify(verificationTokenRepository, times(3)).save(captor.capture());
    VerificationTokenEntity created = captor.getAllValues().getLast();

    assertEquals(TokenStatus.INACTIVE, first.getStatus());
    assertEquals(TokenStatus.INACTIVE, second.getStatus());
    assertEquals(UUID.fromString(value).toString(), value);
    assertEquals(value, created.getToken());
    assertSame(testUserEntity, created.getUserEntity());
    assertEquals(TokenStatus.ACTIVE, created.getStatus());
    assertEquals(NOW.plusHours(2), created.getExpiryDate());
    verify(userRepository, never()).save(any());
    verify(emailQueueService).hasOpenEmailForUserAndType(testUserEntity, EmailType.VERIFICATION);
    verify(emailQueueService, never()).addEmailToQueue(any(), any(), any(), any());
  }

  @Test
  void invalidLifetimePropagatesRatherThanSavingANewToken() {
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION))
        .thenReturn("invalid");

    assertThrows(
        DateTimeParseException.class, () -> verificationService.createToken(testUserEntity));
    verify(verificationTokenRepository, never()).save(any());
    verify(verificationTokenRepository, never()).findAllByUserEntityAndStatus(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT0S", "-PT1H", "PT0.000000001S", "PT1S", "PT59.999999999S"})
  void belowMinimumLifetimeIsRejectedBeforeInvalidatingTokens(String duration) {
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION))
        .thenReturn(duration);

    assertThrows(
        IllegalArgumentException.class, () -> verificationService.createToken(testUserEntity));
    verify(verificationTokenRepository, never()).findAllByUserEntityAndStatus(any(), any());
    verify(verificationTokenRepository, never()).save(any());
    verify(emailQueueService, never()).addEmailToQueue(any(), any(), any(), any());
  }

  @Test
  void minimumLifetimeIsAccepted() {
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1M");
    verificationService.createToken(testUserEntity);
    ArgumentCaptor<VerificationTokenEntity> captor =
        ArgumentCaptor.forClass(VerificationTokenEntity.class);

    verify(verificationTokenRepository).save(captor.capture());
    assertEquals(NOW.plusMinutes(1), captor.getValue().getExpiryDate());
  }

  @Test
  void unrepresentableExpiryDoesNotInvalidateExistingTokens() {
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION))
        .thenReturn("P366000000000D");

    assertThrows(DateTimeException.class, () -> verificationService.createToken(testUserEntity));
    verify(verificationTokenRepository, never()).findAllByUserEntityAndStatus(any(), any());
    verify(verificationTokenRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void consumedExpiredLinkOnlyConfirmsAnEnabledVerifiedAccountWithoutWrites(boolean enabled) {
    testUserEntity.setEnabled(enabled);
    testUserEntity.markEmailVerified();
    when(verificationTokenRepository.findByTokenAndStatus("token", TokenStatus.ACTIVE))
        .thenReturn(Optional.empty());
    if (enabled) {
      when(verificationTokenRepository.findByTokenAndStatus("token", TokenStatus.USED))
          .thenReturn(Optional.of(token(TokenStatus.USED, NOW.minusDays(1))));
    }
    assertEquals(enabled, verificationService.validateToken("token"));
    verify(userRepository, never()).save(any());
    verify(verificationTokenRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 1})
  void expirationBoundaryRejectsEqualityAndConsumesOnlyFutureTokens(long offsetNanos) {
    VerificationTokenEntity entity = token(TokenStatus.ACTIVE, NOW.plusNanos(offsetNanos));
    when(verificationTokenRepository.findByTokenAndStatus("token", TokenStatus.ACTIVE))
        .thenReturn(Optional.of(entity));

    assertEquals(offsetNanos > 0, verificationService.validateToken("token"));
    assertEquals(offsetNanos > 0, testUserEntity.isEnabled());
    assertEquals(offsetNanos > 0 ? TokenStatus.USED : TokenStatus.EXPIRED, entity.getStatus());
    verify(userRepository, times(offsetNanos > 0 ? 1 : 0)).save(testUserEntity);
    verify(verificationTokenRepository).save(entity);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"unknown", "   "})
  void unknownOrEmptyTokenIsRejectedWithoutWrites(String value) {
    assertFalse(verificationService.validateToken(value));
    verify(verificationTokenRepository).findUserIdByToken(value);
    verify(verificationTokenRepository, never()).save(any());
    verifyNoInteractions(userRepository, emailQueueService);
  }

  @Test
  void enabledAccountIsNotSavedAndItsActiveTokenRemainsActive() {
    testUserEntity.setEnabled(true);
    VerificationTokenEntity entity = token(TokenStatus.ACTIVE, NOW.plusDays(1));
    when(verificationTokenRepository.findByTokenAndStatus("token", TokenStatus.ACTIVE))
        .thenReturn(Optional.of(entity));

    assertFalse(verificationService.validateToken("token"));
    assertEquals(TokenStatus.ACTIVE, entity.getStatus());
    verify(userRepository, never()).save(any());
    verify(verificationTokenRepository, never()).save(any());
  }

  @ParameterizedTest
  @EnumSource(
      value = TokenStatus.class,
      names = {"INACTIVE", "USED", "EXPIRED"})
  void statusSetterOnlyCompletesActiveTokens(TokenStatus status) {
    VerificationTokenEntity entity = token(TokenStatus.ACTIVE, NOW.plusDays(1));
    when(verificationTokenRepository.findByTokenAndStatus("token", TokenStatus.ACTIVE))
        .thenReturn(Optional.of(entity));
    verificationService.setTokenStatus("token", status);

    assertEquals(status, entity.getStatus());
    verify(verificationTokenRepository).save(entity);
  }

  @Test
  void statusSetterCannotReactivateOrRewriteCompletedTokens() {
    assertThrows(
        IllegalArgumentException.class,
        () -> verificationService.setTokenStatus("token", TokenStatus.ACTIVE));
    VerificationTokenEntity entity = token(TokenStatus.USED, NOW.plusDays(1));
    when(verificationTokenRepository.findByToken("token")).thenReturn(Optional.of(entity));

    assertThrows(
        IllegalStateException.class,
        () -> verificationService.setTokenStatus("token", TokenStatus.INACTIVE));
    verificationService.setTokenStatus("token", TokenStatus.USED);

    verify(verificationTokenRepository, never()).save(any());
  }

  @Test
  void missingStatusTargetIsANoOp() {
    verificationService.setTokenStatus("missing", TokenStatus.USED);

    verify(verificationTokenRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void existenceReflectsRepositoryPresenceRegardlessOfStatus(boolean exists) {
    when(verificationTokenRepository.findByToken("token"))
        .thenReturn(
            exists ? Optional.of(token(TokenStatus.EXPIRED, NOW.minusDays(1))) : Optional.empty());

    assertEquals(exists, verificationService.existsToken("token"));
  }

  @ParameterizedTest
  @CsvSource({"token,en", "email,en", "user,en", "token,de", "email,de", "user,de"})
  void resendForDisabledUserRotatesTokenAndQueuesRealClasspathTemplate(
      String entry, String language) throws IOException {
    prepareResend(entry);
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1H");
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Example Service");
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.com/app/");
    when(messageProperties.getEmailVerificationSubject()).thenReturn("Verify account");
    try (MockedStatic<CustomI18NProvider> locale = mockStatic(CustomI18NProvider.class)) {
      locale.when(CustomI18NProvider::getCurrentLocale).thenReturn(Locale.forLanguageTag(language));

      assertTrue(resend(entry));
    }

    ArgumentCaptor<VerificationTokenEntity> tokenCaptor =
        ArgumentCaptor.forClass(VerificationTokenEntity.class);

    verify(verificationTokenRepository).save(tokenCaptor.capture());
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

    verify(emailQueueService)
        .addEmailToQueue(
            eq(testUserEntity), eq("Verify account"), body.capture(), eq(EmailType.VERIFICATION));
    assertTrue(
        body.getValue()
            .contains("https://example.com/app/verification/" + tokenCaptor.getValue().getToken()));
    assertTrue(body.getValue().contains("Example Service"));
    assertTrue(body.getValue().contains("Test"));
    assertTrue(body.getValue().contains("User"));
    assertTrue(body.getValue().contains("cid:" + EmailLogo.CONTENT_ID));
    for (String placeholder :
        List.of(
            "SERVICE_LOGO",
            "SERVICE_NAME",
            "FIRST_NAME",
            "LAST_NAME",
            "VERIFICATION_LINK",
            "COPYRIGHT_TEXT")) {
      assertFalse(body.getValue().contains("{{" + placeholder + "}}"));
    }
    String expectedFooter =
        MessageProperties.forLocale(new CustomI18NProvider(), Locale.forLanguageTag(language))
            .getAccountCopyrightTemplate()
            .replace("{year}", String.valueOf(Year.now(CLOCK).getValue()))
            .replace("{serviceName}", "Example Service");

    assertTrue(HtmlUtils.htmlUnescape(body.getValue()).contains(expectedFooter));
    assertFalse(body.getValue().contains("{{"));
    assertFalse(testUserEntity.isEnabled());
  }

  @Test
  void customCopyrightIsResolvedFromConfigurationAndEscapedInTheMail() throws IOException {
    prepareResend("email");
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1H");
    when(configService.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Company & Partners");
    when(configService.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.com/");
    when(configService.getString(ConfigEntry.COPYRIGHT_TEXT))
        .thenReturn("© {year} {serviceName} <b>Eigener Text</b>");
    when(messageProperties.getEmailVerificationSubject()).thenReturn("subject");

    assertTrue(resend("email"));
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

    verify(emailQueueService)
        .addEmailToQueue(
            eq(testUserEntity), eq("subject"), body.capture(), eq(EmailType.VERIFICATION));
    assertTrue(
        body.getValue()
            .contains("&copy; 2026 Company &amp; Partners &lt;b&gt;Eigener Text&lt;/b&gt;"));
    assertFalse(body.getValue().contains("<b>Eigener Text</b>"));
    assertFalse(body.getValue().contains("{year}"));
    verify(messageProperties, never()).getAccountCopyrightTemplate();
  }

  @ParameterizedTest
  @ValueSource(strings = {"token", "email", "user"})
  void openQueueEntryRejectsAnotherRequestWithoutRotatingOrQueuingAgain(String entry)
      throws IOException {
    prepareResend(entry);
    when(emailQueueService.hasOpenEmailForUserAndType(testUserEntity, EmailType.VERIFICATION))
        .thenReturn(true);

    assertFalse(resend(entry));
    verify(verificationTokenRepository, never()).save(any());
    verify(emailQueueService, never()).addEmailToQueue(any(), any(), any(), any());
    verifyMaintenanceOnly(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"token", "email", "user"})
  void resendForEnabledAccountIsRejected(String entry) throws IOException {
    prepareResend(entry);
    testUserEntity.setEnabled(true);

    assertFalse(resend(entry));
    verifyNoInteractions(emailQueueService);
    verifyMaintenanceOnly(1);

    verify(verificationTokenRepository, never()).save(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"token", "email"})
  void resendForUnknownTokenOrEmailIsRejected(String entry) throws IOException {
    if (entry.equals("token")) {
      when(verificationTokenRepository.findUserIdByToken("token")).thenReturn(Optional.empty());
    }
    assertFalse(resend(entry));
    verifyNoInteractions(emailQueueService);
    verifyMaintenanceOnly(1);
  }

  @Test
  void cleanupDeletesUsingConfiguredCutoffWithoutFormattingFailure() {
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION)).thenReturn("P7D");

    assertDoesNotThrow(verificationService::deleteExpiredTokens);
    ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(verificationTokenRepository).deleteByExpiryDateBefore(cutoff.capture());
    assertEquals(NOW.minusDays(7), cutoff.getValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT0S", "-PT1H", "invalid", "PT1M", "PT119.999999999S"})
  void invalidRetentionDurationDoesNotDeleteTokens(String duration) {
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION))
        .thenReturn(duration);
    Class<? extends RuntimeException> expected =
        duration.equals("invalid") ? DateTimeParseException.class : IllegalArgumentException.class;

    assertThrows(expected, verificationService::deleteExpiredTokens);
    verifyNoInteractions(verificationTokenRepository);
  }

  @Test
  void minimumRetentionUsesExpiryBasedCutoff() {
    when(configService.getString(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION)).thenReturn("PT2M");
    verificationService.deleteExpiredTokens();

    verify(verificationTokenRepository).deleteByExpiryDateBefore(NOW.minusMinutes(2));
  }

  @Test
  void disappearedAccountRejectsValidationStatusChangesAndResend() throws IOException {
    when(userRepository.lockVerificationUser(1L)).thenReturn(Optional.empty());

    assertFalse(verificationService.validateToken("token"));
    verificationService.setTokenStatus("token", TokenStatus.INACTIVE);

    assertFalse(verificationService.sendVerificationEmailByUser(testUserEntity));
    assertThrows(
        IllegalStateException.class, () -> verificationService.createToken(testUserEntity));
    verify(verificationTokenRepository, never()).save(any());
    verifyNoInteractions(emailQueueService);
    verifyMaintenanceOnly(3);
  }

  @Test
  void missingUserAndNullStatusAreRejectedWithoutRepositoryAccess() throws IOException {
    assertFalse(verificationService.sendVerificationEmailByUser(null));
    assertFalse(verificationService.sendVerificationEmailByUser(UserEntity.builder().build()));
    assertThrows(
        IllegalArgumentException.class, () -> verificationService.setTokenStatus("token", null));
    verifyNoInteractions(
        verificationTokenRepository, userRepository, emailQueueService, configService);
  }

  private VerificationTokenEntity token(TokenStatus status, LocalDateTime expiry) {
    return new VerificationTokenEntity(null, "token", testUserEntity, expiry, status);
  }

  private void prepareResend(String entry) {
    if (entry.equals("token")) {
      // Scalar reference from the fixture; account state is checked under its lock.
    } else if (entry.equals("email")) {
      when(userRepository.findByEmail(testUserEntity.getEmail()))
          .thenReturn(Optional.of(testUserEntity));
    }
  }

  private boolean resend(String entry) throws IOException {
    return switch (entry) {
      case "token" -> verificationService.sendVerificationEmailByToken("token");
      case "email" -> verificationService.sendVerificationEmailByEmail(testUserEntity.getEmail());
      case "user" -> verificationService.sendVerificationEmailByUser(testUserEntity);
      default -> throw new IllegalArgumentException(entry);
    };
  }

  private void verifyMaintenanceOnly(int reads) {
    verify(configService, times(reads)).getBoolean(ConfigEntry.MAINTENANCE_MODE);
    verifyNoMoreInteractions(configService);
  }
}
