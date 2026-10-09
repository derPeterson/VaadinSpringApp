package de.derpeterson.app.service;

import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real repositories and Spring transaction proxy; no Boot app, scheduling or SMTP. */
@SpringJUnitConfig(EmailQueueServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailQueueServicePersistenceTest {
    @Autowired private EmailQueueService service;
    @Autowired private EmailQueueRepository emails;
    @Autowired private UserRepository users;
    @Autowired private ConfigService config;
    @Autowired private EmailService mail;
    @Autowired private TransactionTemplate tx;
    @Autowired private Probes probes;
    @Autowired private MutableQueueClock clock;
    private Long userId;

    @BeforeEach
    void seed() {
        reset(probes.emails(), probes.users(), config, mail);
        clock.set(java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(50);
        lenient().when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(3);
        lenient().when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.com");
        lenient().when(config.getString(ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION)).thenReturn("PT168H");
        tx.executeWithoutResult(status -> {
            emails.deleteAll();
            users.deleteAll();
            users.flush();
            userId = users.saveAndFlush(UserEntity.builder().firstName("Fixture").lastName("User")
                    .email("recipient@example.com").password("fixture-hash-not-a-real-password")
                    .gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH)
                    .roleEntities(new ArrayList<>()).build()).getId();
        });
    }

    @ParameterizedTest
    @EnumSource(EmailType.class)
    void twoDirectProducersCommitOnlyOneOpenMailPerUserAndType(EmailType type) throws Exception {
        var barrier = new CyclicBarrier(2);
        doAnswer(call -> {
            barrier.await(10, TimeUnit.SECONDS);
            return users.lockVerificationUser(call.getArgument(0));
        }).when(probes.users()).lockVerificationUser(userId);
        var detached = snapshot();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.addEmailToQueue(detached, "first", "body", type));
            var second = executor.submit(() -> service.addEmailToQueue(detached, "second", "body", type));
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertEquals(1, emails.count());
        assertEquals(type, emails.findAll().getFirst().getEmailType());
        assertTrue(service.hasOpenEmailForUserAndType(detached, type));
        verifyNoInteractions(mail);
    }

    @ParameterizedTest
    @EnumSource(EmailStatus.class)
    void onlyOpenStatusesSuppressANewMailAndOtherTypesRemainIndependent(EmailStatus status) {
        insert("existing", status, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var user = snapshot();
        service.addEmailToQueue(user, "new", "body", EmailType.NOTIFICATION);
        service.addEmailToQueue(user, "different-type", "body", EmailType.PROMOTION);
        boolean open = status == EmailStatus.PENDING || status == EmailStatus.IN_PROGRESS;
        assertEquals(open ? 2 : 3, emails.count());
        assertEquals(1, emails.findAll().stream().filter(e -> e.getEmailType() == EmailType.PROMOTION).count());
    }

    @Test
    void staleEligibilityIsReloadedBeforeVerificationQueueDecision() {
        var stale = snapshot();
        tx.executeWithoutResult(status -> users.findById(userId).orElseThrow().setEnabled(false));
        assertTrue(stale.isVerificationPending());
        service.addEmailToQueue(stale, "verify", "body", EmailType.VERIFICATION);
        assertEquals(0, emails.count());
        // No undocumented enabled-account requirement is added by this queue method.
        service.addEmailToQueue(stale, "reset", "body", EmailType.PASSWORD_RESET);
        assertEquals(1, emails.count());
    }

    @Test
    void failureAfterFlushedInsertRollsBackTheActualQueueRecord() {
        var failure = new IllegalStateException("after flushed insert");
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            emails.saveAndFlush(call.getArgument(0));
            throw failure;
        }).when(probes.emails()).save(any());
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.addEmailToQueue(snapshot(), "subject", "body", EmailType.NOTIFICATION)));
        assertEquals(0, emails.count());
    }

    @Test
    void processingCommitsSuccessAndOnlySelectsPendingUpToCapacity() throws Exception {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(1);
        for (EmailStatus status : EmailStatus.values()) insert(status.name(), status, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        insert("another-pending", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        service.processQueue();
        assertEquals(1, emails.findByStatus(EmailStatus.PENDING).size());
        assertEquals(2, emails.findByStatus(EmailStatus.SENT).size());
        assertEquals(1, emails.findByStatus(EmailStatus.IN_PROGRESS).size());
        assertEquals(1, emails.findByStatus(EmailStatus.FAILED).size());
        verify(mail, times(1)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    void retryHistoryAndTerminalFailureCommitAcrossSeparateTransactions() throws Exception {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(1);
        Long id = insert("subject", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        doThrow(new MailAuthenticationException("smtp fixture failure")).when(mail).sendEmail(any(), anyString(), anyString());
        service.processQueue();
        var retry = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.PENDING, retry.getStatus());
        assertEquals(1, retry.getRetryCount());
        assertNotNull(retry.getLastRetryAt());
        verify(mail, never()).sendAdminEmail(any(), any(), any());
        clock.advance(java.time.Duration.ofMinutes(1));
        service.processQueue();
        var failed = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.FAILED, failed.getStatus());
        assertEquals(1, failed.getRetryCount());
        assertFalse(failed.getLastRetryAt().isBefore(retry.getLastRetryAt()));
        verify(mail, times(2)).sendEmail(any(), anyString(), anyString());
        verify(mail).sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"), contains("after 2 attempts"));
        service.processQueue();
        verify(mail, times(2)).sendEmail(any(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void adminFailuresCannotRollBackCommittedTerminalFailure(boolean mailException) throws Exception {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
        Long id = insert("subject", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        doThrow(new MailAuthenticationException("recipient failed")).when(mail).sendEmail(any(), anyString(), anyString());
        RuntimeException failure = mailException ? new MailSendException("admin failed") : new IllegalStateException("admin runtime");
        doThrow(failure).when(mail).sendAdminEmail(any(), any(), any());
        assertDoesNotThrow(service::processQueue);
        var actual = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.FAILED, actual.getStatus());
        assertEquals(0, actual.getRetryCount());
        assertNotNull(actual.getLastRetryAt());
    }

    @Test
    void laterRuntimeFailurePreservesEarlierSuccessAndUnknownClaimWithoutRedispatch() throws Exception {
        Long one = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        Long two = insert("two", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        // Deterministic order, using real managed entities inside the actual transaction.
        doAnswer(call -> List.of(emails.findById(one).orElseThrow(), emails.findById(two).orElseThrow()))
                .when(probes.emails()).findPendingEmails(any(), any());
        var failure = new IllegalStateException("second dispatch runtime");
        doThrow(failure).when(mail).sendEmail(any(), eq("two"), eq("body"));
        assertDoesNotThrow(service::processQueue);
        assertEquals(EmailStatus.SENT, emails.findById(one).orElseThrow().getStatus());
        assertEquals(EmailStatus.IN_PROGRESS, emails.findById(two).orElseThrow().getStatus());
        verify(mail).sendEmail(any(), eq("one"), eq("body"));
        // Stale candidate lists must not redispatch SENT or unknown IN_PROGRESS.
        reset(mail);
        service.processQueue();
        verifyNoInteractions(mail);
        assertEquals(1, emails.findByStatus(EmailStatus.SENT).size());
    }

    @Test
    void twoWorkersDispatchTheSamePersistedPendingMailOnlyOnce() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var selected = new CyclicBarrier(2);
        var calls = new AtomicInteger();
        doAnswer(call -> {
            var result = emails.findPendingEmails(call.getArgument(0), call.getArgument(1));
            assertEquals(1, result.size());
            assertEquals(EmailStatus.PENDING, result.getFirst().getStatus());
            selected.await(10, TimeUnit.SECONDS);
            return result;
        }).when(probes.emails()).findPendingEmails(any(), any());
        doAnswer(call -> {
            calls.incrementAndGet();
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(service::processQueue);
            var second = executor.submit(service::processQueue);
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertEquals(1, calls.get());
        assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
        assertEquals(1, emails.count());
    }

    @Test
    void staleFailingWorkerCannotDispatchOrOverwriteCommittedSuccess() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var selected = new CyclicBarrier(2);
        var successCommitted = new CountDownLatch(1);
        var failingWorker = new ThreadLocal<Boolean>();
        doAnswer(call -> {
            var result = emails.findPendingEmails(call.getArgument(0), call.getArgument(1));
            assertEquals(1, result.size());
            selected.await(10, TimeUnit.SECONDS);
            return result;
        }).when(probes.emails()).findPendingEmails(any(), any());
        doAnswer(call -> {
            if (Boolean.TRUE.equals(failingWorker.get())) await(successCommitted);
            return emails.lockById(call.getArgument(0));
        }).when(probes.emails()).lockById(any());
        doAnswer(call -> {
            if (Boolean.TRUE.equals(failingWorker.get())) {
                await(successCommitted);
                throw new MailSendException("second worker failed after first committed");
            }
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var success = executor.submit(() -> {
                try {
                    service.processQueue();
                } finally {
                    successCommitted.countDown();
                }
            });
            var failure = executor.submit(() -> {
                failingWorker.set(true);
                try {
                    service.processQueue();
                } finally {
                    failingWorker.remove();
                }
            });
            success.get(20, TimeUnit.SECONDS);
            failure.get(20, TimeUnit.SECONDS);
        }
        var actual = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.SENT, actual.getStatus());
        assertEquals(0, actual.getRetryCount());
        verify(mail, times(1)).sendEmail(any(), anyString(), anyString());
    }

    @Test
    void producerDuringUncommittedDispatchStillSeesOpenMailAndDoesNotDuplicateIt() throws Exception {
        insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var sending = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(call -> { sending.countDown(); await(release); return null; })
                .when(mail).sendEmail(any(), anyString(), anyString());
        var user = snapshot();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var processing = executor.submit(service::processQueue);
            try {
                await(sending);
                service.addEmailToQueue(user, "duplicate", "body", EmailType.NOTIFICATION);
                assertEquals(1, emails.count());
            } finally {
                release.countDown();
            }
            processing.get(20, TimeUnit.SECONDS);
        }
        assertEquals(1, emails.findByStatus(EmailStatus.SENT).size());
    }

    @Test
    void durableClaimBlocksContenderWhileSenderIsStillRunning() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var selected = new CyclicBarrier(2);
        var sending = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var contenderCompleted = new CountDownLatch(1);
        doAnswer(call -> {
            var result = emails.findPendingEmails(call.getArgument(0), call.getArgument(1));
            selected.await(10, TimeUnit.SECONDS);
            return result;
        }).when(probes.emails()).findPendingEmails(any(), any());
        doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            sending.countDown();
            await(release);
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            Runnable worker = () -> { try { service.processQueue(); } finally { contenderCompleted.countDown(); } };
            var first = executor.submit(worker);
            var second = executor.submit(worker);
            try {
                await(sending);
                await(contenderCompleted);
                assertEquals(EmailStatus.IN_PROGRESS, emails.findById(id).orElseThrow().getStatus());
                verify(mail, times(1)).sendEmail(any(), anyString(), anyString());
            } finally {
                release.countDown();
            }
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
    }

    @Test
    void parallelWorkersKeepMixedSuccessTerminalFailureAndUnknownOutcomesSeparate() throws Exception {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
        Long sent = insert("success", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        Long failed = insert("known-failure", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        Long unknown = insert("unknown", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var selected = new CyclicBarrier(2);
        doAnswer(call -> {
            var result = emails.findPendingEmails(call.getArgument(0), call.getArgument(1));
            assertEquals(3, result.size());
            selected.await(10, TimeUnit.SECONDS);
            return result;
        }).when(probes.emails()).findPendingEmails(any(), any());
        doThrow(new MailAuthenticationException("known failure")).when(mail).sendEmail(any(), eq("known-failure"), anyString());
        doThrow(new IllegalStateException("unknown outcome")).when(mail).sendEmail(any(), eq("unknown"), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(service::processQueue);
            var second = executor.submit(service::processQueue);
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertEquals(EmailStatus.SENT, emails.findById(sent).orElseThrow().getStatus());
        assertEquals(EmailStatus.FAILED, emails.findById(failed).orElseThrow().getStatus());
        assertEquals(EmailStatus.IN_PROGRESS, emails.findById(unknown).orElseThrow().getStatus());
        for (String subject : List.of("success", "known-failure", "unknown")) {
            verify(mail, times(1)).sendEmail(any(), eq(subject), eq("body"));
        }
        verify(mail, times(1)).sendAdminEmail(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"claimSave", "claimCommit", "sentSave", "sentCommit", "retrySave", "retryCommit"})
    void storageAndCommitFailuresAreIsolatedAndUnknownOutcomesAreNeverReleased(String stage) throws Exception {
        Long affected = insert("affected", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        Long healthy = insert("healthy", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var failure = new IllegalStateException(stage);
        boolean claimFailure = stage.startsWith("claim");
        boolean retryFailure = stage.startsWith("retry");
        if (retryFailure) doThrow(new MailAuthenticationException("known failure")).when(mail).sendEmail(any(), eq("affected"), anyString());
        doAnswer(call -> {
            EmailQueueEntity item = call.getArgument(0);
            EmailStatus target = claimFailure ? EmailStatus.IN_PROGRESS : retryFailure ? EmailStatus.PENDING : EmailStatus.SENT;
            if (item.getId().equals(affected) && item.getStatus() == target) {
                emails.saveAndFlush(item);
                if (stage.endsWith("Save")) throw failure;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) { throw failure; }
                });
            } else emails.save(item);
            return item;
        }).when(probes.emails()).save(any());
        assertDoesNotThrow(service::processQueue);
        var actual = emails.findById(affected).orElseThrow();
        assertEquals(claimFailure ? EmailStatus.PENDING : EmailStatus.IN_PROGRESS, actual.getStatus());
        assertEquals(0, actual.getRetryCount());
        assertNull(actual.getLastRetryAt());
        assertEquals(EmailStatus.SENT, emails.findById(healthy).orElseThrow().getStatus());
        verify(mail, times(claimFailure ? 0 : 1)).sendEmail(any(), eq("affected"), anyString());
        verify(mail, times(1)).sendEmail(any(), eq("healthy"), anyString());
        if (!claimFailure) {
            reset(mail);
            service.processQueue();
            verifyNoInteractions(mail);
            assertTrue(service.hasOpenEmailForUserAndType(snapshot(), EmailType.NOTIFICATION));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"adminConfig", "adminMail", "adminRuntime"})
    void laterAdminFailureCannotUndoEarlierSuccessOrStopFollowingMessages(String stage) throws Exception {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
        Long first = insert("first", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        Long terminal = insert("terminal", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        Long last = insert("last", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        doAnswer(call -> List.of(emails.findById(first).orElseThrow(), emails.findById(terminal).orElseThrow(), emails.findById(last).orElseThrow()))
                .when(probes.emails()).findPendingEmails(any(), any());
        doThrow(new MailAuthenticationException("recipient failure")).when(mail).sendEmail(any(), eq("terminal"), anyString());
        if (stage.equals("adminConfig")) when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenThrow(new IllegalStateException(stage));
        else doAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(EmailStatus.FAILED, emails.findById(terminal).orElseThrow().getStatus());
            if (stage.equals("adminMail")) throw new MailSendException(stage);
            throw new IllegalStateException(stage);
        }).when(mail).sendAdminEmail(any(), any(), any());
        assertDoesNotThrow(service::processQueue);
        assertEquals(EmailStatus.SENT, emails.findById(first).orElseThrow().getStatus());
        assertEquals(EmailStatus.FAILED, emails.findById(terminal).orElseThrow().getStatus());
        assertEquals(EmailStatus.SENT, emails.findById(last).orElseThrow().getStatus());
        reset(mail);
        service.processQueue();
        verifyNoInteractions(mail);
    }

    @Test
    void ambientCallerRollbackCannotUndoClaimOrSuccessfulOutcome() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        tx.executeWithoutResult(status -> {
            service.processQueue();
            status.setRollbackOnly();
        });
        assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
        verify(mail, times(1)).sendEmail(any(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void unknownTransportOrRuntimeOutcomeStaysOpenWithoutRetryOrDuplicateProduction(boolean transport) throws Exception {
        Long id = insert("unknown", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now().minusDays(30), 2);
        RuntimeException failure = transport ? new MailSendException("SMTP reply lost") : new IllegalStateException("sender outcome unknown");
        doThrow(failure).when(mail).sendEmail(any(), anyString(), anyString());
        service.processQueue();
        var actual = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.IN_PROGRESS, actual.getStatus());
        assertEquals(2, actual.getRetryCount());
        assertNull(actual.getLastRetryAt());
        verify(mail, never()).sendAdminEmail(any(), any(), any());
        reset(mail);
        service.processQueue();
        service.deleteSentEmails();
        service.addEmailToQueue(snapshot(), "duplicate", "body", EmailType.NOTIFICATION);
        assertEquals(1, emails.count());
        assertEquals(EmailStatus.IN_PROGRESS, emails.findById(id).orElseThrow().getStatus());
        verifyNoInteractions(mail);
    }

    @ParameterizedTest
    @EnumSource(value = EmailStatus.class, names = {"SENT", "FAILED"})
    void outcomeGuardNeverOverwritesAnAlreadyTerminalState(EmailStatus terminal) throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        doAnswer(call -> {
            tx.executeWithoutResult(status -> emails.findById(id).orElseThrow().setStatus(terminal));
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        assertDoesNotThrow(service::processQueue);
        assertEquals(terminal, emails.findById(id).orElseThrow().getStatus());
        assertEquals(0, emails.findById(id).orElseThrow().getRetryCount());
        verify(mail, never()).sendAdminEmail(any(), any(), any());
    }

    @Test
    void outcomeGuardRejectsChangedRetryHistoryWithoutOverwritingIt() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        doAnswer(call -> {
            tx.executeWithoutResult(status -> emails.findById(id).orElseThrow().setRetryCount(9));
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        service.processQueue();
        var actual = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.IN_PROGRESS, actual.getStatus());
        assertEquals(9, actual.getRetryCount());
        verify(mail, never()).sendAdminEmail(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void realSpringConnectionFailureRetriesToLimitThenUnblocksProducer(int retries) throws Exception {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(retries);
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn("sender@example.com");
        var sender = new EmailConnectionFailureTest.Sender();
        var connectionFailure = new jakarta.mail.MessagingException("arbitrary", new java.net.ConnectException("fixture"));
        doThrow(connectionFailure).when(sender.transport).connect(nullable(String.class), anyInt(), nullable(String.class), nullable(String.class));
        var realMail = new EmailService(config, sender);
        doAnswer(call -> {
            realMail.sendEmail(call.getArgument(0), call.getArgument(1), call.getArgument(2));
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        for (int attempt = 1; attempt <= retries + 1; attempt++) {
            service.processQueue();
            var actual = emails.findById(id).orElseThrow();
            assertEquals(attempt <= retries ? EmailStatus.PENDING : EmailStatus.FAILED, actual.getStatus());
            assertEquals(Math.min(attempt, retries), actual.getRetryCount());
            assertNotNull(actual.getLastRetryAt());
            if (attempt <= retries) {
                service.addEmailToQueue(snapshot(), "duplicate", "body", EmailType.NOTIFICATION);
                assertEquals(1, emails.count());
                clock.advance(java.time.Duration.ofMinutes(1));
            }
        }
        verify(sender.transport, times(retries + 1)).connect(nullable(String.class), anyInt(), nullable(String.class), nullable(String.class));
        verify(sender.transport, never()).sendMessage(any(), any());
        verify(mail).sendAdminEmail(eq("admin@example.com"), anyString(), contains("after " + (retries + 1) + " attempts"));
        service.addEmailToQueue(snapshot(), "new", "body", EmailType.NOTIFICATION);
        assertEquals(2, emails.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"send", "close", "sendAndClose"})
    void realSpringUnknownSendOrCloseFailureRemainsInProgress(String stage) throws Exception {
        when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn("sender@example.com");
        var sender = new EmailConnectionFailureTest.Sender();
        var failure = new jakarta.mail.MessagingException("Mail server connection failed", new java.net.ConnectException("fixture"));
        if (!stage.equals("close")) doThrow(failure).when(sender.transport).sendMessage(any(), any());
        if (!stage.equals("send")) doThrow(failure).when(sender.transport).close();
        var realMail = new EmailService(config, sender);
        doAnswer(call -> { realMail.sendEmail(call.getArgument(0), call.getArgument(1), call.getArgument(2)); return null; })
                .when(mail).sendEmail(any(), anyString(), anyString());
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        service.processQueue();
        service.processQueue();
        service.addEmailToQueue(snapshot(), "duplicate", "body", EmailType.NOTIFICATION);
        assertEquals(1, emails.count());
        var actual = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.IN_PROGRESS, actual.getStatus());
        assertEquals(0, actual.getRetryCount());
        assertNull(actual.getLastRetryAt());
        verify(sender.transport, times(1)).sendMessage(any(), any());
        verify(mail, never()).sendAdminEmail(any(), any(), any());
    }

    @Test
    void cleanupCommitsActualSentDeletionEvenAfterFlush() {
        Long old = insert("old-sent", EmailStatus.SENT, EmailType.NOTIFICATION, LocalDateTime.now().minusDays(8), 0);
        insert("new-sent", EmailStatus.SENT, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        insert("old-pending", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now().minusDays(8), 0);
        doAnswer(call -> {
            int deleted = emails.deleteByStatusAndCreatedAtBefore(call.getArgument(0), call.getArgument(1));
            emails.flush();
            assertEquals(1, deleted);
            assertFalse(emails.existsById(old));
            return deleted;
        }).when(probes.emails()).deleteByStatusAndCreatedAtBefore(any(), any());
        assertDoesNotThrow(service::deleteSentEmails);
        assertFalse(emails.existsById(old));
        assertEquals(2, emails.count());
        verifyNoInteractions(mail);
    }

    @Test
    void cleanupCompletesWhenThereIsNothingToDelete() {
        assertDoesNotThrow(service::deleteSentEmails);
        assertEquals(0, emails.count());
    }

    @Test
    void dueSelectionIsOldestCreatedFirstWithIdTieBreakAndFiltersBeforeCapacity() throws Exception {
        var now = LocalDateTime.now(clock).withNano(0);
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(2);
        Long recent = insert("recent", EmailStatus.PENDING, EmailType.NOTIFICATION, now, 0);
        Long equalFirst = insert("tie-first", EmailStatus.PENDING, EmailType.NOTIFICATION, now.minusDays(1), 0);
        Long equalSecond = insert("tie-second", EmailStatus.PENDING, EmailType.NOTIFICATION, now.minusDays(1), 0);
        Long oldest = insert("oldest", EmailStatus.PENDING, EmailType.NOTIFICATION, now.minusDays(2), 0);
        Long waiting = insert("not-due", EmailStatus.PENDING, EmailType.NOTIFICATION, now.minusDays(3), 1);
        tx.executeWithoutResult(status -> emails.findById(waiting).orElseThrow().setLastRetryAt(now));
        insert("unknown", EmailStatus.IN_PROGRESS, EmailType.NOTIFICATION, now.minusDays(4), 0);
        service.processQueue();
        var order = inOrder(mail);
        order.verify(mail).sendEmail(any(), eq("oldest"), eq("body"));
        order.verify(mail).sendEmail(any(), eq("tie-first"), eq("body"));
        assertEquals(EmailStatus.PENDING, emails.findById(equalSecond).orElseThrow().getStatus());
        assertEquals(EmailStatus.PENDING, emails.findById(recent).orElseThrow().getStatus());
        assertEquals(EmailStatus.PENDING, emails.findById(waiting).orElseThrow().getStatus());
        service.processQueue();
        order.verify(mail).sendEmail(any(), eq("tie-second"), eq("body"));
        order.verify(mail).sendEmail(any(), eq("recent"), eq("body"));
        assertEquals(2, emails.findByStatus(EmailStatus.PENDING).size() + emails.findByStatus(EmailStatus.IN_PROGRESS).size());
        assertEquals(EmailStatus.SENT, emails.findById(oldest).orElseThrow().getStatus());
        assertEquals(EmailStatus.SENT, emails.findById(equalFirst).orElseThrow().getStatus());
    }

    @Test
    void retryIsNotSelectedBeforeMinuteAndIsSelectedExactlyAtBoundary() throws Exception {
        clock.set(java.time.Instant.parse("2030-01-01T12:00:00Z"));
        var now = LocalDateTime.now(clock);
        Long id = insert("retry", EmailStatus.PENDING, EmailType.NOTIFICATION, now, 0);
        doThrow(new MailAuthenticationException("fixture")).when(mail).sendEmail(any(), anyString(), anyString());
        service.processQueue();
        assertEquals(now, emails.findById(id).orElseThrow().getLastRetryAt());
        clock.advance(java.time.Duration.ofSeconds(59).plusNanos(999_999_999));
        service.processQueue();
        verify(mail, times(1)).sendEmail(any(), anyString(), anyString());
        clock.advance(java.time.Duration.ofNanos(1));
        service.processQueue();
        verify(mail, times(2)).sendEmail(any(), anyString(), anyString());
        assertEquals(2, emails.findById(id).orElseThrow().getRetryCount());
        assertEquals(now.plusMinutes(1), emails.findById(id).orElseThrow().getLastRetryAt());
    }

    @Test
    void staleWorkerCannotRetryAfterAnotherWorkerCommittedFreshFailure() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(clock), 0);
        var selected = new CyclicBarrier(2);
        var firstCommitted = new CountDownLatch(1);
        var stale = new ThreadLocal<Boolean>();
        doAnswer(call -> {
            var candidates = emails.findPendingEmails(call.getArgument(0), call.getArgument(1));
            assertEquals(1, candidates.size());
            selected.await(10, TimeUnit.SECONDS);
            return candidates;
        }).when(probes.emails()).findPendingEmails(any(), any());
        doAnswer(call -> { if (Boolean.TRUE.equals(stale.get())) await(firstCommitted); return emails.lockById(call.getArgument(0)); })
                .when(probes.emails()).lockById(any());
        doThrow(new MailAuthenticationException("fixture")).when(mail).sendEmail(any(), anyString(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { try { service.processQueue(); } finally { firstCommitted.countDown(); } });
            var second = executor.submit(() -> { stale.set(true); try { service.processQueue(); } finally { stale.remove(); } });
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        verify(mail, times(1)).sendEmail(any(), anyString(), anyString());
        assertEquals(EmailStatus.PENDING, emails.findById(id).orElseThrow().getStatus());
        assertEquals(1, emails.findById(id).orElseThrow().getRetryCount());
    }

    @Test
    void cleanupUsesClockZoneAndStrictCreationAgeNotLastRetryOrStatusAge() {
        clock.set(java.time.Instant.parse("2030-01-01T12:00:00Z"));
        var cutoff = LocalDateTime.now(clock).minusHours(168);
        Long before = insert("before", EmailStatus.SENT, EmailType.NOTIFICATION, cutoff.minusNanos(1000), 0);
        Long equal = insert("equal", EmailStatus.SENT, EmailType.NOTIFICATION, cutoff, 0);
        Long after = insert("after", EmailStatus.SENT, EmailType.NOTIFICATION, cutoff.plusNanos(1000), 0);
        for (EmailStatus status : List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS, EmailStatus.FAILED)) {
            insert(status.name(), status, EmailType.NOTIFICATION, cutoff.minusDays(1), 0);
        }
        tx.executeWithoutResult(status -> emails.findById(before).orElseThrow().setLastRetryAt(LocalDateTime.now(clock)));
        service.deleteSentEmails();
        assertFalse(emails.existsById(before));
        assertTrue(emails.existsById(equal));
        assertTrue(emails.existsById(after));
        assertEquals(5, emails.count());
    }

    @Test
    void queueAcceptsEmptyContentAndUsesInjectedLocalTime() {
        clock.set(java.time.Instant.parse("2030-01-01T12:00:00Z"));
        service.addEmailToQueue(snapshot(), "", "", EmailType.NOTIFICATION);
        var actual = emails.findAll().getFirst();
        assertEquals("", actual.getSubject());
        assertEquals("", actual.getBody());
        assertEquals(LocalDateTime.now(clock), actual.getCreatedAt());
    }

    @Test
    void repositoryCleanupUsesStrictCreatedAtCutoffAndSentStatusNotLastRetry() {
        LocalDateTime cutoff = LocalDateTime.of(2020, 1, 2, 0, 0);
        Long older = insert("older", EmailStatus.SENT, EmailType.NOTIFICATION, cutoff.minusSeconds(1), 2);
        Long equal = insert("equal", EmailStatus.SENT, EmailType.NOTIFICATION, cutoff, 0);
        Long newer = insert("newer", EmailStatus.SENT, EmailType.NOTIFICATION, cutoff.plusSeconds(1), 0);
        for (EmailStatus status : List.of(EmailStatus.PENDING, EmailStatus.IN_PROGRESS, EmailStatus.FAILED)) {
            insert(status.name(), status, EmailType.NOTIFICATION, cutoff.minusDays(1), 0);
        }
        tx.executeWithoutResult(status -> {
            emails.findById(older).orElseThrow().setLastRetryAt(cutoff.plusDays(3));
            assertEquals(1, emails.deleteByStatusAndCreatedAtBefore(EmailStatus.SENT, cutoff));
            emails.flush();
        });
        assertFalse(emails.existsById(older));
        assertTrue(emails.existsById(equal));
        assertTrue(emails.existsById(newer));
        assertEquals(5, emails.count());
    }

    private UserEntity snapshot() {
        return tx.execute(status -> {
            var user = users.findById(userId).orElseThrow();
            user.getRoleEntities().size();
            return user;
        });
    }

    private Long insert(String subject, EmailStatus status, EmailType type, LocalDateTime created, int retries) {
        return tx.execute(transaction -> {
            var item = new EmailQueueEntity();
            item.setUserEntity(users.findById(userId).orElseThrow());
            item.setSubject(subject);
            item.setBody("body");
            item.setStatus(status);
            item.setEmailType(type);
            item.setCreatedAt(created);
            item.setRetryCount(retries);
            return emails.saveAndFlush(item).getId();
        });
    }

    private void await(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(10, TimeUnit.SECONDS), "controlled operation did not arrive");
    }

    record Probes(UserRepository users, EmailQueueRepository emails) { }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:email-queue-tests-" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("de.derpeterson.app.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean TransactionTemplate transactionTemplate(JpaTransactionManager manager) {
            return new TransactionTemplate(manager);
        }
        @Bean Probes probes(UserRepository users, EmailQueueRepository emails) {
            return new Probes(mock(UserRepository.class, delegatesTo(users)), mock(EmailQueueRepository.class, delegatesTo(emails)));
        }
        @Bean ConfigService configService() { return mock(ConfigService.class); }
        @Bean EmailService emailService() { return mock(EmailService.class); }
        @Bean MutableQueueClock queueClock() { return new MutableQueueClock(java.time.ZoneId.of("Europe/Berlin")); }
        @Bean EmailQueueService emailQueueService(ConfigService config, EmailService mail, Probes probes, JpaTransactionManager manager, MutableQueueClock clock) {
            return new EmailQueueService(config, probes.emails(), mail, probes.users(), manager, clock);
        }
    }
}
