package de.derpeterson.app.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.AdminNotificationAttemptEntity;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailParseException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Characterizes current behavior, including defects, without SMTP or persistence. */
@ExtendWith(MockitoExtension.class)
class EmailQueueServiceTest {
    @Mock private ConfigService config;
    @Mock private EmailQueueRepository emails;
    @Mock private EmailService mail;
    @Mock private UserRepository users;
    @Mock private PlatformTransactionManager transactions;
    @Mock private AdminNotificationAttemptRepository adminAttempts;
    private EmailQueueService service;
    private UserEntity user;
    private MutableQueueClock clock;

    @BeforeEach
    void setup() {
        lenient().when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        clock = new MutableQueueClock();
        service = new EmailQueueService(config, emails, mail, users, transactions, adminAttempts, clock);
        lenient().when(adminAttempts.saveAndFlush(any())).thenAnswer(call -> {
            AdminNotificationAttemptEntity audit = call.getArgument(0);
            audit.setId(1L);
            lenient().when(adminAttempts.lockById(1L)).thenReturn(Optional.of(audit));
            return audit;
        });
        lenient().when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(3);
        user = UserEntity.builder().id(12L).email("recipient@example.com").firstName("Fixture")
                .lastName("User").password("fixture-hash-not-a-real-password").preferredLocale(Locale.ENGLISH)
                .roleEntities(new ArrayList<>()).build();
    }

    @ParameterizedTest
    @EnumSource(EmailType.class)
    void openQueryUsesExactlyPendingAndInProgressForTheRequestedType(EmailType type) {
        var statuses = List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS);
        when(emails.existsByUserEntityAndEmailTypeAndStatusIn(user, type, statuses)).thenReturn(true, false);
        assertTrue(service.hasOpenEmailForUserAndType(user, type));
        assertFalse(service.hasOpenEmailForUserAndType(user, type));
        verifyNoInteractions(users, mail, config);
    }

    @ParameterizedTest
    @EnumSource(EmailType.class)
    void enqueueLocksAndUsesFreshAccountAndPreservesContent(EmailType type) {
        var stale = UserEntity.builder().id(12L).email("stale@example.com").preferredLocale(Locale.ENGLISH).build();
        when(users.lockVerificationUser(12L)).thenReturn(Optional.of(user));
        LocalDateTime before = LocalDateTime.now();
        service.addEmailToQueue(stale, "Grüße 🔐", "<b>日本語</b>\r\n", type);
        var order = inOrder(users, emails);
        order.verify(users).lockVerificationUser(12L);
        order.verify(emails).existsByUserEntityAndEmailTypeAndStatusIn(user, type,
                List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS));
        var captor = ArgumentCaptor.forClass(EmailQueueEntity.class);
        order.verify(emails).save(captor.capture());
        var queued = captor.getValue();
        assertSame(user, queued.getUserEntity());
        assertEquals(type, queued.getEmailType());
        assertEquals("Grüße 🔐", queued.getSubject());
        assertEquals("<b>日本語</b>\r\n", queued.getBody());
        assertEquals(EmailStatus.PENDING, queued.getStatus());
        assertEquals(0, queued.getRetryCount());
        assertNull(queued.getLastRetryAt());
        assertNull(queued.getId());
        assertBetween(queued.getCreatedAt(), before);
        verifyNoInteractions(mail, config);
    }

    @ParameterizedTest
    @EnumSource(EmailType.class)
    void openDuplicateDoesNotOverwriteOrInsert(EmailType type) {
        when(users.lockVerificationUser(12L)).thenReturn(Optional.of(user));
        when(emails.existsByUserEntityAndEmailTypeAndStatusIn(eq(user), eq(type), anyCollection())).thenReturn(true);
        service.addEmailToQueue(user, "new", "new", type);
        verify(emails, never()).save(any());
        verifyNoInteractions(mail);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void verificationCannotQueueForEnabledOrExplicitlyBlockedAccounts(boolean enabled) {
        user.setEnabled(enabled);
        when(users.lockVerificationUser(12L)).thenReturn(Optional.of(user));
        service.addEmailToQueue(user, "subject", "body", EmailType.VERIFICATION);
        verifyNoInteractions(emails, mail);
    }

    @ParameterizedTest
    @EnumSource(value = EmailType.class, names = "VERIFICATION", mode = EnumSource.Mode.EXCLUDE)
    void nonVerificationQueueDoesNotAddItsOwnEligibilityRule(EmailType type) {
        user.setEnabled(false);
        when(users.lockVerificationUser(12L)).thenReturn(Optional.of(user));
        service.addEmailToQueue(user, "", "", type);
        verify(emails).save(argThat(e -> e.getSubject().isEmpty() && e.getBody().isEmpty()));
    }

    @Test
    void missingAccountFailsBeforeQueueAccess() {
        when(users.lockVerificationUser(12L)).thenReturn(Optional.empty());
        assertEquals("Der Benutzer ist nicht mehr vorhanden.", assertThrows(IllegalStateException.class,
                () -> service.addEmailToQueue(user, "subject", "body", EmailType.NOTIFICATION)).getMessage());
        verifyNoInteractions(emails, mail);
    }

    @Test
    void queueInsertFailurePropagatesUnchanged() {
        when(users.lockVerificationUser(12L)).thenReturn(Optional.of(user));
        var failure = new IllegalStateException("queue storage failed");
        when(emails.save(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.addEmailToQueue(user, "subject", "body", EmailType.NOTIFICATION)));
        verifyNoInteractions(mail);
    }

    @Test
    void emptyBatchOnlyQueriesConfiguredCapacity() {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(7);
        when(emails.findPendingEmails(any(), eq(PageRequest.of(0, 7)))).thenReturn(List.of());
        service.processQueue();
        verifyNoInteractions(mail, users);
        verify(emails, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nonPositiveCapacityFailsBeforeQuery(int capacity) {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(capacity);
        assertThrows(IllegalArgumentException.class, service::processQueue);
        verifyNoInteractions(emails, mail);
    }

    @ParameterizedTest
    @EnumSource(value = EmailStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void nonPendingEntriesAreNotSentEvenIfReturnedByRepository(EmailStatus status) {
        var item = item(status, 2);
        batch(item);
        service.processQueue();
        verifyNoInteractions(mail);
        verify(emails, never()).save(any());
        assertEquals(status, item.getStatus());
        assertEquals(2, item.getRetryCount());
    }

    @Test
    void successSavesInProgressBeforeSendingAndSentAfterwardsWithoutResettingHistory() throws Exception {
        var item = item(EmailStatus.PENDING, 2);
        var retryAt = item.getLastRetryAt();
        batch(item);
        var transitions = new ArrayList<EmailStatus>();
        when(emails.save(item)).thenAnswer(call -> { transitions.add(item.getStatus()); return item; });
        doAnswer(call -> { assertEquals(EmailStatus.IN_PROGRESS, item.getStatus()); return null; })
                .when(mail).sendEmail(user, "subject", "body");
        service.processQueue();
        assertEquals(List.of(EmailStatus.IN_PROGRESS, EmailStatus.SENT), transitions);
        assertEquals(2, item.getRetryCount());
        assertEquals(retryAt, item.getLastRetryAt());
        var order = inOrder(emails, mail);
        order.verify(emails).findPendingEmails(any(), eq(PageRequest.of(0, 3)));
        order.verify(emails).save(item);
        order.verify(mail).sendEmail(user, "subject", "body");
        order.verify(emails).save(item);
        verify(mail, never()).sendAdminEmail(any(), any(), any());
        verify(config).getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY);
    }

    @ParameterizedTest
    @CsvSource({"0,3,false", "2,3,false", "3,3,true", "4,3,true", "0,0,true"})
    void retryBoundaryUsesStoredCounterAndConfiguredLimit(int retries, int limit, boolean terminal) throws Exception {
        var item = item(EmailStatus.PENDING, retries);
        batch(item);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(limit);
        if (terminal) when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
        doThrow(new MailAuthenticationException("smtp failed")).when(mail).sendEmail(user, "subject", "body");
        LocalDateTime before = LocalDateTime.now();
        service.processQueue();
        assertEquals(terminal ? EmailStatus.FAILED : EmailStatus.PENDING, item.getStatus());
        assertEquals(terminal ? retries : retries + 1, item.getRetryCount());
        assertBetween(item.getLastRetryAt(), before);
        verify(emails, times(2)).save(item);
        if (terminal) verify(mail).sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"), anyString());
        else verify(mail, never()).sendAdminEmail(any(), any(), any());
    }

    @Test
    void checkedMessagingFailureAlsoRequeuesAndContinuesBatch() throws Exception {
        var first = item(EmailStatus.PENDING, 0);
        var second = item(EmailStatus.PENDING, 0);
        second.setSubject("second");
        batch(first, second);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(2);
        doThrow(new MessagingException("mime failed")).when(mail).sendEmail(user, "subject", "body");
        service.processQueue();
        assertEquals(EmailStatus.PENDING, first.getStatus());
        assertEquals(1, first.getRetryCount());
        assertEquals(EmailStatus.SENT, second.getStatus());
        verify(mail).sendEmail(user, "second", "body");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void knownPreparationAndParseFailuresRetainRetrySemantics(boolean preparation) throws Exception {
        var item = item(EmailStatus.PENDING, 0);
        batch(item);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(3);
        RuntimeException failure = preparation ? new MailPreparationException("prepare") : new MailParseException("parse");
        doThrow(failure).when(mail).sendEmail(user, "subject", "body");
        service.processQueue();
        assertEquals(EmailStatus.PENDING, item.getStatus());
        assertEquals(1, item.getRetryCount());
        verify(mail, never()).sendAdminEmail(any(), any(), any());
    }

    @Test
    void deletedCandidateIsSkippedBeforeDispatch() {
        var item = item(EmailStatus.PENDING, 0);
        batch(item);
        when(emails.lockById(item.getId())).thenReturn(Optional.empty());
        service.processQueue();
        verifyNoInteractions(mail);
        verify(emails, never()).save(any());
    }

    @Test
    void configuredThreeRetriesActuallyMeansFourFailedDispatches() throws Exception {
        var item = item(EmailStatus.PENDING, 0);
        batch(item);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(3);
        when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
        doThrow(new MailAuthenticationException("failure")).when(mail).sendEmail(user, "subject", "body");
        for (int attempt = 1; attempt <= 3; attempt++) {
            service.processQueue();
            assertEquals(attempt, item.getRetryCount());
            assertEquals(EmailStatus.PENDING, item.getStatus());
            clock.advance(java.time.Duration.ofMinutes(1));
        }
        service.processQueue();
        assertEquals(EmailStatus.FAILED, item.getStatus());
        assertEquals(3, item.getRetryCount());
        verify(mail, times(4)).sendEmail(user, "subject", "body");
        var body = ArgumentCaptor.forClass(String.class);
        verify(mail).sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"), body.capture());
        assertEquals("The email to recipient@example.com with the subject 'subject' could not be delivered after 4 attempts.\n\nError message: failure\nEmail type: NOTIFICATION\nSending time: "
                + item.getLastRetryAt(), body.getValue());
        assertFalse(body.getValue().contains(user.toString()));
        assertFalse(body.getValue().contains(user.getPassword()));
        assertFalse(body.getValue().contains(user.getFirstName()));
        assertFalse(body.getValue().contains(user.getLastName()));
        assertTrue(body.getValue().contains("Error message: failure"));
        assertTrue(body.getValue().contains("Email type: NOTIFICATION"));
        assertTrue(body.getValue().contains("Sending time: " + item.getLastRetryAt()));
    }

    @Test
    void adminMailExceptionIsSwallowedAndRemainingBatchContinues() throws Exception {
        var first = item(EmailStatus.PENDING, 0);
        var second = item(EmailStatus.PENDING, 0);
        second.setSubject("second");
        batch(first, second);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
        when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
        doThrow(new MailAuthenticationException("failed")).when(mail).sendEmail(user, "subject", "body");
        doThrow(new MailSendException("admin failed")).when(mail).sendAdminEmail(any(), any(), any());
        assertDoesNotThrow(service::processQueue);
        assertEquals(EmailStatus.FAILED, first.getStatus());
        assertEquals(EmailStatus.SENT, second.getStatus());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void logsAndAdminBodyCountInitialAttemptPlusConfiguredRetries(int limit) throws Exception {
        var item = item(EmailStatus.PENDING, 0);
        batch(item);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(limit);
        when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
        doThrow(new MailAuthenticationException("fixture failure")).when(mail).sendEmail(user, "subject", "body");
        var logger = (Logger) LoggerFactory.getLogger(EmailQueueService.class);
        var logs = threadLogs();
        String thread = Thread.currentThread().getName();
        logs.start();
        logger.addAppender(logs);
        try {
            for (int attempt = 1; attempt <= limit + 1; attempt++) {
                service.processQueue();
                String expected = attempt <= limit
                        ? "⚠️ Error sending the email to recipient@example.com, attempt " + attempt + " of " + (limit + 1) + ": fixture failure"
                        : "❌ Email to recipient@example.com failed after " + attempt + " attempts: fixture failure";
                assertTrue(logs.list.stream().anyMatch(event -> event.getThreadName().equals(thread)
                        && event.getFormattedMessage().equals(expected)), expected);
                assertEquals(attempt <= limit ? EmailStatus.PENDING : EmailStatus.FAILED, item.getStatus());
                clock.advance(java.time.Duration.ofMinutes(1));
            }
            assertEquals(limit, item.getRetryCount());
            verify(mail, times(limit + 1)).sendEmail(user, "subject", "body");
            verify(mail).sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"),
                    contains("after " + (limit + 1) + " attempts"));
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    @Test
    void terminalAttemptCountUsesStoredHistoryRatherThanChangedConfiguration() throws Exception {
        var item = item(EmailStatus.PENDING, 4);
        batch(item);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(1);
        when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
        doThrow(new MailAuthenticationException("fixture failure")).when(mail).sendEmail(user, "subject", "body");
        service.processQueue();
        assertEquals(EmailStatus.FAILED, item.getStatus());
        assertEquals(4, item.getRetryCount());
        verify(mail).sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"), contains("after 5 attempts"));
    }

    @Test
    void successfulRetryLogsActualAttemptWithoutChangingHistory() throws Exception {
        var item = item(EmailStatus.PENDING, 2);
        batch(item);
        var logger = (Logger) LoggerFactory.getLogger(EmailQueueService.class);
        var logs = threadLogs();
        String thread = Thread.currentThread().getName();
        logs.start();
        logger.addAppender(logs);
        try {
            service.processQueue();
            assertTrue(logs.list.stream().anyMatch(event -> event.getThreadName().equals(thread)
                    && event.getFormattedMessage().equals("✅ Email successfully sent to recipient@example.com on attempt 3.")));
            assertEquals(EmailStatus.SENT, item.getStatus());
            assertEquals(2, item.getRetryCount());
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    @Test
    void unexpectedSenderRuntimeFailureKeepsClaimAndContinuesOtherDispatches() throws Exception {
        var first = item(EmailStatus.PENDING, 0);
        var second = item(EmailStatus.PENDING, 0);
        second.setSubject("second");
        batch(first, second);
        var failure = new IllegalStateException("unexpected");
        doThrow(failure).when(mail).sendEmail(user, "subject", "body");
        assertDoesNotThrow(service::processQueue);
        assertEquals(EmailStatus.IN_PROGRESS, first.getStatus());
        assertEquals(EmailStatus.SENT, second.getStatus());
        verify(mail).sendEmail(user, "second", "body");
        verify(config).getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"save", "adminConfig", "adminSender"})
    void infrastructureRuntimeFailuresAreIsolatedWithoutAutomaticRelease(String stage) throws Exception {
        var item = item(EmailStatus.PENDING, 0);
        batch(item);
        var failure = new IllegalStateException(stage);
        if (stage.equals("save")) {
            when(emails.save(item)).thenThrow(failure);
        } else {
            doThrow(new MailAuthenticationException("smtp")).when(mail).sendEmail(user, "subject", "body");
            {
                when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
                if (stage.equals("adminConfig")) when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenThrow(failure);
                else {
                    when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
                    doThrow(failure).when(mail).sendAdminEmail(any(), any(), any());
                }
            }
        }
        assertDoesNotThrow(service::processQueue);
        if (stage.equals("save")) verifyNoInteractions(mail);
        else assertEquals(EmailStatus.FAILED, item.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT168H", "PT0.000000001S"})
    void cleanupCompletesAfterRequestingSentOnlyDeletionWithUnchangedCutoff(String duration) {
        when(config.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn(duration);
        LocalDateTime expected = LocalDateTime.now(clock).minus(java.time.Duration.parse(duration));
        when(emails.deleteByStatusAndCreatedAtBefore(eq(EmailStatus.SENT), any())).thenReturn(5);
        assertDoesNotThrow(service::deleteSentEmails);
        var cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(emails).deleteByStatusAndCreatedAtBefore(eq(EmailStatus.SENT), cutoff.capture());
        assertEquals(expected, cutoff.getValue());
        verifyNoInteractions(mail);
    }

    @Test
    void invalidCleanupDurationDoesNotDelete() {
        when(config.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn("not-a-duration");
        assertThrows(DateTimeParseException.class, service::deleteSentEmails);
        verifyNoInteractions(emails, mail);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void negativeRetryLimitFailsBeforeSelectionOrClaim(int limit) {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(1);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(limit);
        assertThrows(IllegalArgumentException.class, service::processQueue);
        verifyNoInteractions(emails, mail, transactions);
    }

    @Test
    void retryConfigFailureStopsBeforeSelectionAndSending() {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(1);
        var failure = new IllegalStateException("config");
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, service::processQueue));
        verifyNoInteractions(emails, mail, transactions);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1H"})
    void nonPositiveCleanupDurationFailsBeforeDeletion(String duration) {
        when(config.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn(duration);
        assertThrows(IllegalArgumentException.class, service::deleteSentEmails);
        verifyNoInteractions(emails, mail);
    }

    @Test
    void nullRetentionAndClockAreRejected() {
        assertThrows(NullPointerException.class, service::deleteSentEmails);
        assertThrows(NullPointerException.class, () -> new EmailQueueService(config, emails, mail, users, transactions, adminAttempts, null));
        verifyNoInteractions(emails, mail);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "id", "subject", "body", "type"})
    void nullQueueRequiredFieldsFailBeforeRepositoryAccess(String field) {
        if (field.equals("id")) user.setId(null);
        assertThrows(NullPointerException.class, () -> service.addEmailToQueue(field.equals("user") ? null : user,
                field.equals("subject") ? null : "", field.equals("body") ? null : "", field.equals("type") ? null : EmailType.NOTIFICATION));
        verifyNoInteractions(users, emails, mail);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "id", "type"})
    void nullOpenQueryRequiredFieldsFailBeforeRepositoryAccess(String field) {
        if (field.equals("id")) user.setId(null);
        assertThrows(NullPointerException.class, () -> service.hasOpenEmailForUserAndType(field.equals("user") ? null : user,
                field.equals("type") ? null : EmailType.NOTIFICATION));
        verifyNoInteractions(emails);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void claimRechecksRetryBoundaryUsingClock(long offsetNanos) throws Exception {
        var item = item(EmailStatus.PENDING, 1);
        item.setLastRetryAt(LocalDateTime.now(clock).minusMinutes(1).plusNanos(offsetNanos));
        batch(item);
        service.processQueue();
        if (offsetNanos > 0) {
            assertEquals(EmailStatus.PENDING, item.getStatus());
            verifyNoInteractions(mail);
            verify(emails, never()).save(any());
        } else {
            assertEquals(EmailStatus.SENT, item.getStatus());
            verify(mail).sendEmail(user, "subject", "body");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Europe/Berlin", "America/New_York"})
    void injectedClockZoneDeterminesLocalCreationAndCleanup(String zone) {
        var fixed = java.time.Clock.fixed(java.time.Instant.parse("2030-01-01T12:00:00Z"), java.time.ZoneId.of(zone));
        var localService = new EmailQueueService(config, emails, mail, users, transactions, adminAttempts, fixed);
        when(users.lockVerificationUser(user.getId())).thenReturn(Optional.of(user));
        localService.addEmailToQueue(user, "", "", EmailType.NOTIFICATION);
        verify(emails).save(argThat(item -> item.getCreatedAt().equals(LocalDateTime.now(fixed))));
        when(config.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn("PT1H");
        localService.deleteSentEmails();
        verify(emails).deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, LocalDateTime.now(fixed).minusHours(1));
        localService.deleteFailedEmails();
        verify(emails).deleteByStatusAndLastRetryAtBefore(EmailStatus.FAILED, LocalDateTime.now(fixed).minusDays(30));
    }

    @Test
    void positiveMaximumCapacityAndRetryLimitAreAccepted() {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(Integer.MAX_VALUE);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(Integer.MAX_VALUE);
        when(emails.findPendingEmails(any(), eq(PageRequest.of(0, Integer.MAX_VALUE)))).thenReturn(List.of());
        assertDoesNotThrow(service::processQueue);
        verifyNoInteractions(mail);
    }

    @Test
    void failedCleanupUsesInjectedClockAndFixedThirtyDayLastAttemptCutoff() {
        service.deleteFailedEmails();
        verify(emails).deleteByStatusAndLastRetryAtBefore(EmailStatus.FAILED, LocalDateTime.now(clock).minusDays(30));
        verifyNoInteractions(config, mail, adminAttempts);
    }

    @Test
    void failedCleanupUsesLocalDaysAcrossDaylightSavingNotSevenHundredTwentyHours() {
        var fixed = java.time.Clock.fixed(java.time.Instant.parse("2030-10-28T12:00:00Z"), java.time.ZoneId.of("Europe/Berlin"));
        var localService = new EmailQueueService(config, emails, mail, users, transactions, adminAttempts, fixed);
        localService.deleteFailedEmails();
        var expected = LocalDateTime.of(2030, 9, 28, 13, 0);
        verify(emails).deleteByStatusAndLastRetryAtBefore(EmailStatus.FAILED, expected);
        assertNotEquals(expected, LocalDateTime.ofInstant(fixed.instant().minus(java.time.Duration.ofDays(30)), fixed.getZone()));
    }

    private ListAppender<ILoggingEvent> threadLogs() {
        Thread owner = Thread.currentThread();
        return new ListAppender<>() {
            @Override
            protected void append(ILoggingEvent event) {
                if (Thread.currentThread() == owner) super.append(event);
            }
        };
    }

    private void batch(EmailQueueEntity... items) {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(3);
        when(emails.findPendingEmails(any(), eq(PageRequest.of(0, 3)))).thenReturn(List.of(items));
        for (int i = 0; i < items.length; i++) {
            items[i].setId((long) i + 1);
            when(emails.lockById(items[i].getId())).thenReturn(Optional.of(items[i]));
        }
    }

    private EmailQueueEntity item(EmailStatus status, int retries) {
        var item = new EmailQueueEntity();
        item.setUserEntity(user);
        item.setSubject("subject");
        item.setBody("body");
        item.setStatus(status);
        item.setRetryCount(retries);
        item.setLastRetryAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        return item;
    }

    private void assertBetween(LocalDateTime actual, LocalDateTime before) {
        assertNotNull(actual);
        assertEquals(LocalDateTime.now(clock), actual);
    }
}
