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
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.UnsupportedTemporalTypeException;
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
    private Long userId;

    @BeforeEach
    void seed() {
        reset(probes.emails(), probes.users(), config, mail);
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
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
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
        doThrow(new MailSendException("smtp fixture failure")).when(mail).sendEmail(any(), anyString(), anyString());
        service.processQueue();
        var retry = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.PENDING, retry.getStatus());
        assertEquals(1, retry.getRetryCount());
        assertNotNull(retry.getLastRetryAt());
        verify(mail, never()).sendAdminEmail(any(), any(), any());
        service.processQueue();
        var failed = emails.findById(id).orElseThrow();
        assertEquals(EmailStatus.FAILED, failed.getStatus());
        assertEquals(1, failed.getRetryCount());
        assertFalse(failed.getLastRetryAt().isBefore(retry.getLastRetryAt()));
        verify(mail, times(2)).sendEmail(any(), anyString(), anyString());
        verify(mail).sendAdminEmail(eq("admin@example.com"), eq("Email dispatch failed"), contains("after 3 attempts"));
        service.processQueue();
        verify(mail, times(2)).sendEmail(any(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void adminMailFailureCommitsButUnexpectedRuntimeFailureRollsBack(boolean mailException) throws Exception {
        when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
        Long id = insert("subject", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        doThrow(new MailSendException("recipient failed")).when(mail).sendEmail(any(), anyString(), anyString());
        RuntimeException failure = mailException ? new MailSendException("admin failed") : new IllegalStateException("admin runtime");
        doThrow(failure).when(mail).sendAdminEmail(any(), any(), any());
        if (mailException) assertDoesNotThrow(service::processQueue);
        else assertSame(failure, assertThrows(IllegalStateException.class, service::processQueue));
        var actual = emails.findById(id).orElseThrow();
        assertEquals(mailException ? EmailStatus.FAILED : EmailStatus.PENDING, actual.getStatus());
        assertEquals(0, actual.getRetryCount());
        if (mailException) assertNotNull(actual.getLastRetryAt());
        else assertNull(actual.getLastRetryAt());
    }

    @Test
    void laterBatchRuntimeFailureRollsBackEarlierSuccessThoughDispatchAlreadyOccurred() throws Exception {
        Long one = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        Long two = insert("two", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        // Deterministic order, using real managed entities inside the actual transaction.
        doAnswer(call -> List.of(emails.findById(one).orElseThrow(), emails.findById(two).orElseThrow()))
                .when(probes.emails()).findPendingEmails(any());
        var failure = new IllegalStateException("second dispatch runtime");
        doThrow(failure).when(mail).sendEmail(any(), eq("two"), eq("body"));
        assertSame(failure, assertThrows(IllegalStateException.class, service::processQueue));
        assertEquals(2, emails.findByStatus(EmailStatus.PENDING).size());
        verify(mail).sendEmail(any(), eq("one"), eq("body"));
        // Next execution dispatches the already successful first message again.
        reset(mail);
        service.processQueue();
        verify(mail).sendEmail(any(), eq("one"), eq("body"));
        assertEquals(2, emails.findByStatus(EmailStatus.SENT).size());
    }

    @Test
    void twoWorkersCanDispatchTheSamePersistedPendingMailTwice() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var selected = new CyclicBarrier(2);
        var dispatching = new CyclicBarrier(2);
        var calls = new AtomicInteger();
        doAnswer(call -> {
            var result = emails.findPendingEmails(call.getArgument(0));
            assertEquals(1, result.size());
            assertEquals(EmailStatus.PENDING, result.getFirst().getStatus());
            selected.await(10, TimeUnit.SECONDS);
            return result;
        }).when(probes.emails()).findPendingEmails(any());
        doAnswer(call -> {
            calls.incrementAndGet();
            dispatching.await(10, TimeUnit.SECONDS);
            return null;
        }).when(mail).sendEmail(any(), anyString(), anyString());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(service::processQueue);
            var second = executor.submit(service::processQueue);
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertEquals(2, calls.get());
        assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
        assertEquals(1, emails.count());
    }

    @Test
    void laterFailingWorkerCanOverwriteCommittedSuccessWithPendingRetry() throws Exception {
        Long id = insert("one", EmailStatus.PENDING, EmailType.NOTIFICATION, LocalDateTime.now(), 0);
        var selected = new CyclicBarrier(2);
        var successCommitted = new CountDownLatch(1);
        var failingWorker = new ThreadLocal<Boolean>();
        doAnswer(call -> {
            var result = emails.findPendingEmails(call.getArgument(0));
            assertEquals(1, result.size());
            selected.await(10, TimeUnit.SECONDS);
            return result;
        }).when(probes.emails()).findPendingEmails(any());
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
        assertEquals(EmailStatus.PENDING, actual.getStatus());
        assertEquals(1, actual.getRetryCount());
        verify(mail, times(2)).sendEmail(any(), anyString(), anyString());
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
    void f1FormatterFailureRollsBackActualSentDeletionEvenAfterFlush() {
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
        assertThrows(UnsupportedTemporalTypeException.class, service::deleteSentEmails);
        assertTrue(emails.existsById(old));
        assertEquals(3, emails.count());
        verifyNoInteractions(mail);
    }

    @Test
    void f1AlsoThrowsWhenThereIsNothingToDelete() {
        assertThrows(UnsupportedTemporalTypeException.class, service::deleteSentEmails);
        assertEquals(0, emails.count());
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
        @Bean EmailQueueService emailQueueService(ConfigService config, EmailService mail, Probes probes) {
            return new EmailQueueService(config, probes.emails(), mail, probes.users());
        }
    }
}
