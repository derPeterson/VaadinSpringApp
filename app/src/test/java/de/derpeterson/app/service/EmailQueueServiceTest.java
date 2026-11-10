package de.derpeterson.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.derpeterson.app.model.AdminNotificationAttemptEntity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.mail.MessagingException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Characterizes current behavior, including defects, without SMTP or persistence. */
@ExtendWith(MockitoExtension.class)
class EmailQueueServiceTest {
  @Mock private ConfigService configService;
  @Mock private EmailQueueRepository emailQueueRepository;
  @Mock private EmailService emailService;
  @Mock private UserRepository userRepository;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private AdminNotificationAttemptRepository adminAttempts;
  private EmailQueueService emailQueueService;
  private UserEntity testUserEntity;
  private MutableQueueClock clock;

  @BeforeEach
  void setup() {
    lenient()
        .when(transactionManager.getTransaction(any()))
        .thenAnswer(call -> new SimpleTransactionStatus());
    clock = new MutableQueueClock();
    emailQueueService =
        new EmailQueueService(
            configService,
            emailQueueRepository,
            emailService,
            userRepository,
            transactionManager,
            adminAttempts,
            clock,
            QueueTestWorkers.inline());
    lenient()
        .when(adminAttempts.saveAndFlush(any()))
        .thenAnswer(call -> {
          AdminNotificationAttemptEntity audit = call.getArgument(0);
          audit.setId(1L);
          lenient().when(adminAttempts.lockById(1L)).thenReturn(Optional.of(audit));
          return audit;
        });
    lenient()
        .when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_RETRY_DELAY_SECONDS))
        .thenReturn(60);
    lenient().when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(3);
    testUserEntity =
        UserEntity.builder()
            .id(12L)
            .email("recipient@example.com")
            .firstName("Fixture")
            .lastName("User")
            .password("fixture-hash-not-a-real-password")
            .preferredLocale(Locale.ENGLISH)
            .roleEntities(new ArrayList<>())
            .build();
  }

  @ParameterizedTest
  @EnumSource(EmailType.class)
  void openQueryUsesExactlyPendingAndInProgressForTheRequestedType(EmailType type) {
    List<EmailStatus> statuses = List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS);
    when(emailQueueRepository.existsByUserEntityAndEmailTypeAndStatusIn(
            testUserEntity, type, statuses))
        .thenReturn(true, false);

    assertTrue(emailQueueService.hasOpenEmailForUserAndType(testUserEntity, type));
    assertFalse(emailQueueService.hasOpenEmailForUserAndType(testUserEntity, type));
    verifyNoInteractions(userRepository, emailService, configService);
  }

  @ParameterizedTest
  @EnumSource(EmailType.class)
  void enqueueLocksAndUsesFreshAccountAndPreservesContent(EmailType type) {
    UserEntity stale =
        UserEntity.builder()
            .id(12L)
            .email("stale@example.com")
            .preferredLocale(Locale.ENGLISH)
            .build();
    when(userRepository.lockVerificationUser(12L)).thenReturn(Optional.of(testUserEntity));
    emailQueueService.addEmailToQueue(stale, "Grüße 🔐", "<b>日本語</b>\r\n", type);
    InOrder order = inOrder(userRepository, emailQueueRepository);
    order.verify(userRepository).lockVerificationUser(12L);
    order
        .verify(emailQueueRepository)
        .existsByUserEntityAndEmailTypeAndStatusIn(
            testUserEntity, type, List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS));
    ArgumentCaptor<EmailQueueEntity> captor = ArgumentCaptor.forClass(EmailQueueEntity.class);
    order.verify(emailQueueRepository).save(captor.capture());
    EmailQueueEntity queued = captor.getValue();

    assertSame(testUserEntity, queued.getUserEntity());
    assertEquals(type, queued.getEmailType());
    assertEquals("Grüße 🔐", queued.getSubject());
    assertEquals("<b>日本語</b>\r\n", queued.getBody());
    assertEquals(EmailStatus.PENDING, queued.getStatus());
    assertEquals(0, queued.getRetryCount());
    assertNull(queued.getLastRetryAt());
    assertNull(queued.getId());
    assertAtFixedClock(queued.getCreatedAt());

    verifyNoInteractions(emailService, configService);
  }

  @ParameterizedTest
  @EnumSource(EmailType.class)
  void openDuplicateDoesNotOverwriteOrInsert(EmailType type) {
    when(userRepository.lockVerificationUser(12L)).thenReturn(Optional.of(testUserEntity));
    when(emailQueueRepository.existsByUserEntityAndEmailTypeAndStatusIn(
            eq(testUserEntity), eq(type), anyCollection()))
        .thenReturn(true);
    emailQueueService.addEmailToQueue(testUserEntity, "new", "new", type);

    verify(emailQueueRepository, never()).save(any());
    verifyNoInteractions(emailService);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void verificationCannotQueueForEnabledOrExplicitlyBlockedAccounts(boolean enabled) {
    testUserEntity.setEnabled(enabled);
    when(userRepository.lockVerificationUser(12L)).thenReturn(Optional.of(testUserEntity));
    emailQueueService.addEmailToQueue(testUserEntity, "subject", "body", EmailType.VERIFICATION);

    verifyNoInteractions(emailQueueRepository, emailService);
  }

  @ParameterizedTest
  @EnumSource(value = EmailType.class, names = "VERIFICATION", mode = EnumSource.Mode.EXCLUDE)
  void nonVerificationQueueDoesNotAddItsOwnEligibilityRule(EmailType type) {
    testUserEntity.setEnabled(false);
    when(userRepository.lockVerificationUser(12L)).thenReturn(Optional.of(testUserEntity));
    emailQueueService.addEmailToQueue(testUserEntity, "", "", type);

    verify(emailQueueRepository)
        .save(argThat(e -> e.getSubject().isEmpty() && e.getBody().isEmpty()));
  }

  @Test
  void missingAccountFailsBeforeQueueAccess() {
    when(userRepository.lockVerificationUser(12L)).thenReturn(Optional.empty());

    assertEquals(
        "Der Benutzer ist nicht mehr vorhanden.",
        assertThrows(
                IllegalStateException.class,
                () ->
                    emailQueueService.addEmailToQueue(
                        testUserEntity, "subject", "body", EmailType.NOTIFICATION))
            .getMessage());
    verifyNoInteractions(emailQueueRepository, emailService);
  }

  @Test
  void queueInsertFailurePropagatesUnchanged() {
    when(userRepository.lockVerificationUser(12L)).thenReturn(Optional.of(testUserEntity));
    IllegalStateException failure = new IllegalStateException("queue storage failed");
    when(emailQueueRepository.save(any())).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () ->
                emailQueueService.addEmailToQueue(
                    testUserEntity, "subject", "body", EmailType.NOTIFICATION)));
    verifyNoInteractions(emailService);
  }

  @Test
  void emptyBatchOnlyQueriesConfiguredCapacity() {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(7);
    when(emailQueueRepository.findPendingEmails(any(), eq(PageRequest.of(0, 7))))
        .thenReturn(List.of());
    emailQueueService.processQueue();

    verifyNoInteractions(emailService, userRepository);
    verify(emailQueueRepository, never()).save(any());
  }

  @Test
  void nextBatchUsesTheChangedRetryDelay() {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(7);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_RETRY_DELAY_SECONDS)).thenReturn(60, 180);
    PageRequest page = PageRequest.of(0, 7);
    when(emailQueueRepository.findPendingEmails(any(), eq(page))).thenReturn(List.of());

    emailQueueService.processQueue();
    emailQueueService.processQueue();

    verify(emailQueueRepository).findPendingEmails(LocalDateTime.now(clock).minusSeconds(60), page);
    verify(emailQueueRepository)
        .findPendingEmails(LocalDateTime.now(clock).minusSeconds(180), page);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void nonPositiveCapacityFailsBeforeQuery(int capacity) {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(capacity);

    assertThrows(IllegalArgumentException.class, emailQueueService::processQueue);
    verifyNoInteractions(emailQueueRepository, emailService);
  }

  @ParameterizedTest
  @EnumSource(value = EmailStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
  void nonPendingEntriesAreNotSentEvenIfReturnedByRepository(EmailStatus status) {
    EmailQueueEntity emailQueueEntry = item(status, 2);
    batch(emailQueueEntry);
    emailQueueService.processQueue();

    verifyNoInteractions(emailService);
    verify(emailQueueRepository, never()).save(any());
    assertEquals(status, emailQueueEntry.getStatus());
    assertEquals(2, emailQueueEntry.getRetryCount());
  }

  @Test
  void successSavesInProgressBeforeSendingAndSentAfterwardsWithoutResettingHistory()
      throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 2);
    LocalDateTime retryAt = emailQueueEntry.getLastRetryAt();
    batch(emailQueueEntry);
    ArrayList<EmailStatus> transitions = new ArrayList<EmailStatus>();
    when(emailQueueRepository.save(emailQueueEntry))
        .thenAnswer(call -> {
          transitions.add(emailQueueEntry.getStatus());
          return emailQueueEntry;
        });
    doAnswer(
            call -> {
              assertEquals(EmailStatus.IN_PROGRESS, emailQueueEntry.getStatus());
              return null;
            })
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    emailQueueService.processQueue();

    assertEquals(List.of(EmailStatus.IN_PROGRESS, EmailStatus.SENT), transitions);
    assertEquals(2, emailQueueEntry.getRetryCount());
    assertEquals(retryAt, emailQueueEntry.getLastRetryAt());
    InOrder order = inOrder(emailQueueRepository, emailService);
    order.verify(emailQueueRepository).findPendingEmails(any(), eq(PageRequest.of(0, 3)));
    order.verify(emailQueueRepository).save(emailQueueEntry);
    order.verify(emailService).sendEmail(testUserEntity, "subject", "body");
    order.verify(emailQueueRepository).save(emailQueueEntry);

    verify(emailService, never()).sendAdminEmail(any(), any(), any());
    verify(configService).getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY);
  }

  @ParameterizedTest
  @CsvSource({"0,3,false", "2,3,false", "3,3,true", "4,3,true", "0,0,true"})
  void retryBoundaryUsesStoredCounterAndConfiguredLimit(int retries, int limit, boolean terminal)
      throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, retries);
    batch(emailQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(limit);
    if (terminal) {
      when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
    }
    doThrow(new MailAuthenticationException("smtp failed"))
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    emailQueueService.processQueue();

    assertEquals(terminal ? EmailStatus.FAILED : EmailStatus.PENDING, emailQueueEntry.getStatus());
    assertEquals(terminal ? retries : retries + 1, emailQueueEntry.getRetryCount());
    assertAtFixedClock(emailQueueEntry.getLastRetryAt());

    verify(emailQueueRepository, times(2)).save(emailQueueEntry);
    if (terminal) {
      verify(emailService)
          .sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"), anyString());
    } else {
      verify(emailService, never()).sendAdminEmail(any(), any(), any());
    }
  }

  @Test
  void checkedMessagingFailureAlsoRequeuesAndContinuesBatch() throws Exception {
    EmailQueueEntity firstQueueEntry = item(EmailStatus.PENDING, 0);
    EmailQueueEntity secondQueueEntry = item(EmailStatus.PENDING, 0);
    secondQueueEntry.setSubject("second");
    batch(firstQueueEntry, secondQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(2);
    doThrow(new MessagingException("mime failed"))
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    emailQueueService.processQueue();

    assertEquals(EmailStatus.PENDING, firstQueueEntry.getStatus());
    assertEquals(1, firstQueueEntry.getRetryCount());
    assertEquals(EmailStatus.SENT, secondQueueEntry.getStatus());
    verify(emailService).sendEmail(testUserEntity, "second", "body");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void knownPreparationAndParseFailuresRetainRetrySemantics(boolean preparation) throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 0);
    batch(emailQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(3);
    RuntimeException failure =
        preparation ? new MailPreparationException("prepare") : new MailParseException("parse");
    doThrow(failure).when(emailService).sendEmail(testUserEntity, "subject", "body");
    emailQueueService.processQueue();

    assertEquals(EmailStatus.PENDING, emailQueueEntry.getStatus());
    assertEquals(1, emailQueueEntry.getRetryCount());
    verify(emailService, never()).sendAdminEmail(any(), any(), any());
  }

  @Test
  void deletedCandidateIsSkippedBeforeDispatch() {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 0);
    batch(emailQueueEntry);
    when(emailQueueRepository.lockById(emailQueueEntry.getId())).thenReturn(Optional.empty());
    emailQueueService.processQueue();

    verifyNoInteractions(emailService);
    verify(emailQueueRepository, never()).save(any());
  }

  @Test
  void configuredThreeRetriesActuallyMeansFourFailedDispatches() throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 0);
    batch(emailQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(3);
    when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
    doThrow(new MailAuthenticationException("failure"))
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    for (int attempt = 1; attempt <= 3; attempt++) {
      emailQueueService.processQueue();

      assertEquals(attempt, emailQueueEntry.getRetryCount());
      assertEquals(EmailStatus.PENDING, emailQueueEntry.getStatus());
      clock.advance(java.time.Duration.ofMinutes(1));
    }
    emailQueueService.processQueue();

    assertEquals(EmailStatus.FAILED, emailQueueEntry.getStatus());
    assertEquals(3, emailQueueEntry.getRetryCount());
    verify(emailService, times(4)).sendEmail(testUserEntity, "subject", "body");
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

    verify(emailService)
        .sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"), body.capture());
    assertEquals(
        "The email to recipient@example.com with the subject 'subject' could not be delivered after 4 attempts.\n\nError message: failure\nEmail type: NOTIFICATION\nSending time: "
            + emailQueueEntry.getLastRetryAt(),
        body.getValue());
    assertFalse(body.getValue().contains(testUserEntity.toString()));
    assertFalse(body.getValue().contains(testUserEntity.getPassword()));
    assertFalse(body.getValue().contains(testUserEntity.getFirstName()));
    assertFalse(body.getValue().contains(testUserEntity.getLastName()));
    assertTrue(body.getValue().contains("Error message: failure"));
    assertTrue(body.getValue().contains("Email type: NOTIFICATION"));
    assertTrue(body.getValue().contains("Sending time: " + emailQueueEntry.getLastRetryAt()));
  }

  @Test
  void adminMailExceptionIsSwallowedAndRemainingBatchContinues() throws Exception {
    EmailQueueEntity firstQueueEntry = item(EmailStatus.PENDING, 0);
    EmailQueueEntity secondQueueEntry = item(EmailStatus.PENDING, 0);
    secondQueueEntry.setSubject("second");
    batch(firstQueueEntry, secondQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
    when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
    doThrow(new MailAuthenticationException("failed"))
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    doThrow(new MailSendException("admin failed"))
        .when(emailService)
        .sendAdminEmail(any(), any(), any());

    assertDoesNotThrow(emailQueueService::processQueue);
    assertEquals(EmailStatus.FAILED, firstQueueEntry.getStatus());
    assertEquals(EmailStatus.SENT, secondQueueEntry.getStatus());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3})
  void logsAndAdminBodyCountInitialAttemptPlusConfiguredRetries(int limit) throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 0);
    batch(emailQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(limit);
    when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
    doThrow(new MailAuthenticationException("fixture failure"))
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    Logger logger = (Logger) LoggerFactory.getLogger(EmailQueueService.class);
    ListAppender<ILoggingEvent> logs = threadLogs();
    String thread = Thread.currentThread().getName();
    logs.start();
    logger.addAppender(logs);
    try {
      for (int attempt = 1; attempt <= limit + 1; attempt++) {
        emailQueueService.processQueue();
        String expected =
            attempt <= limit
                ? "Email retry scheduled: queueId=1, attempt="
                    + attempt
                    + " of "
                    + (limit + 1)
                    + ", failureType=org.springframework.mail.MailAuthenticationException"
                : "Email permanently failed: queueId=1, attempts=" + attempt;

        assertTrue(
            logs.list.stream()
                .anyMatch(event ->
                    event.getThreadName().equals(thread)
                        && event.getFormattedMessage().equals(expected)),
            expected);
        assertEquals(
            attempt <= limit ? EmailStatus.PENDING : EmailStatus.FAILED,
            emailQueueEntry.getStatus());
        clock.advance(java.time.Duration.ofMinutes(1));
      }
      assertEquals(limit, emailQueueEntry.getRetryCount());
      verify(emailService, times(limit + 1)).sendEmail(testUserEntity, "subject", "body");
      verify(emailService)
          .sendAdminEmail(
              eq("admin@example.com"),
              eq("Email dispatch failed"),
              contains("after " + (limit + 1) + " attempts"));
    } finally {
      logger.detachAppender(logs);
      logs.stop();
    }
  }

  @Test
  void transportFailureLogsUseQueueIdsAndSanitizedCausesInsteadOfMessageOrAddresses()
      throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 0);
    batch(emailQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
    when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
    doThrow(new MailAuthenticationException("password=private token=private recipient@example.com"))
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    Logger logger = (Logger) LoggerFactory.getLogger(EmailQueueService.class);
    ListAppender<ILoggingEvent> logs = threadLogs();
    logs.start();
    logger.addAppender(logs);
    try {
      emailQueueService.processQueue();

      assertEquals(EmailStatus.FAILED, emailQueueEntry.getStatus());
      assertTrue(
          logs.list.stream().anyMatch(event -> event.getFormattedMessage().contains("queueId=1")));
      for (ILoggingEvent event : logs.list) {
        String text =
            event.getFormattedMessage()
                + (event.getThrowableProxy() == null
                    ? ""
                    : ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(
                        event.getThrowableProxy()));

        assertFalse(text.contains("private"));
        assertFalse(text.contains("recipient@example.com"));
        assertFalse(text.contains("admin@example.com"));
        assertFalse(text.contains("subject"));
      }
      assertTrue(
          logs.list.stream()
              .anyMatch(event ->
                  event.getThrowableProxy() != null
                      && event
                          .getThrowableProxy()
                          .getMessage()
                          .equals(MailAuthenticationException.class.getName())));
    } finally {
      logger.detachAppender(logs);
      logs.stop();
    }
  }

  @Test
  void terminalAttemptCountUsesStoredHistoryRatherThanChangedConfiguration() throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 4);
    batch(emailQueueEntry);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(1);
    when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
    doThrow(new MailAuthenticationException("fixture failure"))
        .when(emailService)
        .sendEmail(testUserEntity, "subject", "body");
    emailQueueService.processQueue();

    assertEquals(EmailStatus.FAILED, emailQueueEntry.getStatus());
    assertEquals(4, emailQueueEntry.getRetryCount());
    verify(emailService)
        .sendAdminEmail(
            eq("admin@example.com"), eq("Email dispatch failed"), contains("after 5 attempts"));
  }

  @Test
  void successfulRetryLogsActualAttemptWithoutChangingHistory() {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 2);
    batch(emailQueueEntry);
    Logger logger = (Logger) LoggerFactory.getLogger(EmailQueueService.class);
    ListAppender<ILoggingEvent> logs = threadLogs();
    String thread = Thread.currentThread().getName();
    logs.start();
    logger.addAppender(logs);
    try {
      emailQueueService.processQueue();

      assertTrue(
          logs.list.stream()
              .anyMatch(event ->
                  event.getThreadName().equals(thread)
                      && event
                          .getFormattedMessage()
                          .equals(
                              "Email accepted by transport: queueId=1, type=NOTIFICATION, attempt=3")));
      assertEquals(EmailStatus.SENT, emailQueueEntry.getStatus());
      assertEquals(2, emailQueueEntry.getRetryCount());
    } finally {
      logger.detachAppender(logs);
      logs.stop();
    }
  }

  @Test
  void unexpectedSenderRuntimeFailureKeepsClaimAndContinuesOtherDispatches() throws Exception {
    EmailQueueEntity firstQueueEntry = item(EmailStatus.PENDING, 0);
    EmailQueueEntity secondQueueEntry = item(EmailStatus.PENDING, 0);
    secondQueueEntry.setSubject("second");
    batch(firstQueueEntry, secondQueueEntry);
    IllegalStateException failure = new IllegalStateException("unexpected");
    doThrow(failure).when(emailService).sendEmail(testUserEntity, "subject", "body");

    assertDoesNotThrow(emailQueueService::processQueue);
    assertEquals(EmailStatus.IN_PROGRESS, firstQueueEntry.getStatus());
    assertEquals(EmailStatus.SENT, secondQueueEntry.getStatus());
    verify(emailService).sendEmail(testUserEntity, "second", "body");
    verify(configService).getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY);
  }

  @ParameterizedTest
  @ValueSource(strings = {"save", "adminConfig", "adminSender"})
  void infrastructureRuntimeFailuresAreIsolatedWithoutAutomaticRelease(String stage)
      throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 0);
    batch(emailQueueEntry);
    IllegalStateException failure = new IllegalStateException(stage);
    if (stage.equals("save")) {
      when(emailQueueRepository.save(emailQueueEntry)).thenThrow(failure);
    } else {
      doThrow(new MailAuthenticationException("smtp"))
          .when(emailService)
          .sendEmail(testUserEntity, "subject", "body");
      {
        when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
        if (stage.equals("adminConfig")) {
          when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenThrow(failure);
        } else {
          when(configService.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
          doThrow(failure).when(emailService).sendAdminEmail(any(), any(), any());
        }
      }
    }
    assertDoesNotThrow(emailQueueService::processQueue);
    if (stage.equals("save")) {
      verifyNoInteractions(emailService);
    } else {
      assertEquals(EmailStatus.FAILED, emailQueueEntry.getStatus());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT168H", "PT0.000000001S"})
  void cleanupCompletesAfterRequestingSentOnlyDeletionWithUnchangedCutoff(String duration) {
    when(configService.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn(duration);
    LocalDateTime expected = LocalDateTime.now(clock).minus(java.time.Duration.parse(duration));
    when(emailQueueRepository.deleteByStatusAndCreatedAtBefore(eq(EmailStatus.SENT), any()))
        .thenReturn(5);

    assertDoesNotThrow(emailQueueService::deleteSentEmails);
    ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);

    verify(emailQueueRepository)
        .deleteByStatusAndCreatedAtBefore(eq(EmailStatus.SENT), cutoff.capture());
    assertEquals(expected, cutoff.getValue());
    verifyNoInteractions(emailService);
  }

  @Test
  void invalidCleanupDurationDoesNotDelete() {
    when(configService.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION))
        .thenReturn("not-a-duration");

    assertThrows(DateTimeParseException.class, emailQueueService::deleteSentEmails);
    verifyNoInteractions(emailQueueRepository, emailService);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, Integer.MIN_VALUE})
  void negativeRetryLimitFailsBeforeSelectionOrClaim(int limit) {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(1);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(limit);

    assertThrows(IllegalArgumentException.class, emailQueueService::processQueue);
    verifyNoInteractions(emailQueueRepository, emailService, transactionManager);
  }

  @Test
  void retryConfigFailureStopsBeforeSelectionAndSending() {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(1);
    IllegalStateException failure = new IllegalStateException("config");
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenThrow(failure);

    assertSame(failure, assertThrows(IllegalStateException.class, emailQueueService::processQueue));
    verifyNoInteractions(emailQueueRepository, emailService, transactionManager);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PT0S", "-PT1H"})
  void nonPositiveCleanupDurationFailsBeforeDeletion(String duration) {
    when(configService.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn(duration);

    assertThrows(IllegalArgumentException.class, emailQueueService::deleteSentEmails);
    verifyNoInteractions(emailQueueRepository, emailService);
  }

  @Test
  void nullRetentionAndClockAreRejected() {
    assertThrows(NullPointerException.class, emailQueueService::deleteSentEmails);
    EmailQueueWorkerPool workers = QueueTestWorkers.inline();

    assertThrows(
        NullPointerException.class,
        () ->
            new EmailQueueService(
                configService,
                emailQueueRepository,
                emailService,
                userRepository,
                transactionManager,
                adminAttempts,
                null,
                workers));
    verifyNoInteractions(emailQueueRepository, emailService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"user", "id", "subject", "body", "type"})
  void nullQueueRequiredFieldsFailBeforeRepositoryAccess(String field) {
    if (field.equals("id")) {
      testUserEntity.setId(null);
    }
    UserEntity targetUser = field.equals("user") ? null : testUserEntity;
    String subject2 = field.equals("subject") ? null : "";
    String body2 = field.equals("body") ? null : "";
    EmailType emailType = field.equals("type") ? null : EmailType.NOTIFICATION;

    assertThrows(
        NullPointerException.class,
        () -> emailQueueService.addEmailToQueue(targetUser, subject2, body2, emailType));
    verifyNoInteractions(userRepository, emailQueueRepository, emailService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"user", "id", "type"})
  void nullOpenQueryRequiredFieldsFailBeforeRepositoryAccess(String field) {
    if (field.equals("id")) {
      testUserEntity.setId(null);
    }
    UserEntity targetUser = field.equals("user") ? null : testUserEntity;
    EmailType emailType2 = field.equals("type") ? null : EmailType.NOTIFICATION;

    assertThrows(
        NullPointerException.class,
        () -> emailQueueService.hasOpenEmailForUserAndType(targetUser, emailType2));
    verifyNoInteractions(emailQueueRepository);
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 1})
  void claimRechecksRetryBoundaryUsingClock(long offsetNanos) throws Exception {
    EmailQueueEntity emailQueueEntry = item(EmailStatus.PENDING, 1);
    emailQueueEntry.setLastRetryAt(LocalDateTime.now(clock).minusMinutes(1).plusNanos(offsetNanos));
    batch(emailQueueEntry);
    emailQueueService.processQueue();
    if (offsetNanos > 0) {
      assertEquals(EmailStatus.PENDING, emailQueueEntry.getStatus());
      verifyNoInteractions(emailService);
      verify(emailQueueRepository, never()).save(any());
    } else {
      assertEquals(EmailStatus.SENT, emailQueueEntry.getStatus());
      verify(emailService).sendEmail(testUserEntity, "subject", "body");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Europe/Berlin", "America/New_York"})
  void injectedClockZoneDeterminesLocalCreationAndCleanup(String zone) {
    Clock fixed =
        java.time.Clock.fixed(
            java.time.Instant.parse("2030-01-01T12:00:00Z"), java.time.ZoneId.of(zone));
    EmailQueueService localService =
        new EmailQueueService(
            configService,
            emailQueueRepository,
            emailService,
            userRepository,
            transactionManager,
            adminAttempts,
            fixed,
            QueueTestWorkers.inline());
    when(userRepository.lockVerificationUser(testUserEntity.getId()))
        .thenReturn(Optional.of(testUserEntity));
    localService.addEmailToQueue(testUserEntity, "", "", EmailType.NOTIFICATION);

    verify(emailQueueRepository)
        .save(argThat(item -> item.getCreatedAt().equals(LocalDateTime.now(fixed))));
    when(configService.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn("PT1H");
    localService.deleteSentEmails();

    verify(emailQueueRepository)
        .deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, LocalDateTime.now(fixed).minusHours(1));
    localService.deleteFailedEmails();

    verify(emailQueueRepository)
        .deleteByStatusAndLastRetryAtBefore(
            EmailStatus.FAILED, LocalDateTime.now(fixed).minusDays(30));
  }

  @Test
  void positiveMaximumCapacityAndRetryLimitAreAccepted() {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(Integer.MAX_VALUE);
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(Integer.MAX_VALUE);
    when(emailQueueRepository.findPendingEmails(any(), eq(PageRequest.of(0, Integer.MAX_VALUE))))
        .thenReturn(List.of());

    assertDoesNotThrow(emailQueueService::processQueue);
    verifyNoInteractions(emailService);
  }

  @Test
  void failedCleanupUsesInjectedClockAndFixedThirtyDayLastAttemptCutoff() {
    emailQueueService.deleteFailedEmails();

    verify(emailQueueRepository)
        .deleteByStatusAndLastRetryAtBefore(
            EmailStatus.FAILED, LocalDateTime.now(clock).minusDays(30));
    verifyNoInteractions(configService, emailService, adminAttempts);
  }

  @Test
  void failedCleanupUsesLocalDaysAcrossDaylightSavingNotSevenHundredTwentyHours() {
    Clock fixed =
        java.time.Clock.fixed(
            java.time.Instant.parse("2030-10-28T12:00:00Z"), java.time.ZoneId.of("Europe/Berlin"));
    EmailQueueService localService =
        new EmailQueueService(
            configService,
            emailQueueRepository,
            emailService,
            userRepository,
            transactionManager,
            adminAttempts,
            fixed,
            QueueTestWorkers.inline());
    localService.deleteFailedEmails();
    LocalDateTime expected = LocalDateTime.of(2030, 9, 28, 13, 0);

    verify(emailQueueRepository).deleteByStatusAndLastRetryAtBefore(EmailStatus.FAILED, expected);
    assertNotEquals(
        expected,
        LocalDateTime.ofInstant(
            fixed.instant().minus(java.time.Duration.ofDays(30)), fixed.getZone()));
  }

  private ListAppender<ILoggingEvent> threadLogs() {
    Thread owner = Thread.currentThread();
    return new ListAppender<>() {
      @Override
      protected void append(ILoggingEvent event) {
        if (Thread.currentThread() == owner) {
          super.append(event);
        }
      }
    };
  }

  private void batch(EmailQueueEntity... items) {
    when(configService.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(3);
    when(emailQueueRepository.findPendingEmails(any(), eq(PageRequest.of(0, 3))))
        .thenReturn(List.of(items));
    for (int queueItemIndex = 0; queueItemIndex < items.length; queueItemIndex++) {
      items[queueItemIndex].setId((long) queueItemIndex + 1);
      when(emailQueueRepository.lockById(items[queueItemIndex].getId()))
          .thenReturn(Optional.of(items[queueItemIndex]));
    }
  }

  private EmailQueueEntity item(EmailStatus status, int retries) {
    EmailQueueEntity emailQueueEntry = new EmailQueueEntity();
    emailQueueEntry.setUserEntity(testUserEntity);
    emailQueueEntry.setSubject("subject");
    emailQueueEntry.setBody("body");
    emailQueueEntry.setStatus(status);
    emailQueueEntry.setRetryCount(retries);
    emailQueueEntry.setLastRetryAt(LocalDateTime.of(2020, 1, 1, 0, 0));
    return emailQueueEntry;
  }

  private void assertAtFixedClock(LocalDateTime actual) {
    assertNotNull(actual);
    assertEquals(LocalDateTime.now(clock), actual);
  }
}
