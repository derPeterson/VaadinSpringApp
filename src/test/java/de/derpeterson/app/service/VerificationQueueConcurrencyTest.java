package de.derpeterson.app.service;

import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.helper.image.ImageHelper;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.*;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** Real token AND queue persistence; no scheduler or SMTP sender is started. */
@SpringJUnitConfig(VerificationQueueConcurrencyTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VerificationQueueConcurrencyTest {
    @Autowired
    private VerificationService verification;
    @Autowired
    private EmailQueueService mailQueue;
    @Autowired
    private UserService admin;
    @Autowired
    private UserRepository users;
    @Autowired
    private VerificationTokenRepository tokens;
    @Autowired
    private EmailQueueRepository emails;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private Probes probes;
    private Long id;

    @BeforeEach
    void seed() {
        reset(probes.users(), probes.tokens(), probes.emails());
        tx.executeWithoutResult(status -> {
            emails.deleteAll();
            tokens.deleteAll();
            users.deleteAll();
            users.flush();
            var user = UserEntity.builder().firstName("Test").lastName("User").email("test@example.com")
                    .password("hash").gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1))
                    .preferredLocale(Locale.ENGLISH).roleEntities(new ArrayList<>()).build();
            id = users.saveAndFlush(user).getId();
            tokens.saveAndFlush(new VerificationTokenEntity(null, "old", user,
                    LocalDateTime.now().plusDays(1), TokenStatus.ACTIVE));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"token", "email", "user"})
    void concurrentResendsCommitOneMailWhoseLinkIsTheOnlyActiveToken(String entry) throws Exception {
        rendezvous();
        assertEquals(List.of(true, true), compete(() -> resend(entry), () -> resend(entry)));
        assertCoherentQueue();
        assertEquals(TokenStatus.INACTIVE, tokenStatus("old"));
        assertEquals(2, tokens.count());
    }

    @Test
    void mixedResendEntrypointsAlsoShareTheSameAccountLock() throws Exception {
        rendezvous();
        assertEquals(List.of(true, true), compete(() -> resend("token"), () -> resend("email")));
        assertCoherentQueue();
    }

    @Test
    void activationAgainstResendHasACoherentStoredOutcomeInEitherOrder() throws Exception {
        rendezvous();
        var results = compete(() -> verification.validateToken("old"), () -> resend("email"));
        if (Boolean.TRUE.equals(results.get(0))) {
            assertEquals(List.of(true, false), results);
            assertTrue(snapshot().isEnabled());
            assertFalse(snapshot().isVerificationPending());
            assertEquals(TokenStatus.USED, tokenStatus("old"));
            assertEquals(0, emails.count());
        } else {
            assertEquals(List.of(false, true), results);
            assertCoherentQueue();
            assertEquals(TokenStatus.INACTIVE, tokenStatus("old"));
        }
    }

    @ParameterizedTest
    @CsvSource({"token,logo", "email,logo", "user,logo", "token,template", "email,template", "user,template"})
    void checkedResourceFailureAlsoRollsBackWithTheRealQueueService(String entry, String resource) {
        var failure = new IOException("simulated image failure");
        try (var image = mockStatic(ImageHelper.class, CALLS_REAL_METHODS);
             var locale = mockStatic(CustomI18NProvider.class)) {
            locale.when(CustomI18NProvider::getCurrentLocale).thenReturn(
                    resource.equals("template") ? Locale.FRENCH : Locale.ENGLISH);
            if (resource.equals("logo")) {
                image.when(() -> ImageHelper.convertImageToBase64("META-INF/resources/custom-theme/service_logo.png"))
                        .thenThrow(failure);
            }
            IOException thrown = assertThrows(IOException.class, () -> resend(entry));
            if (resource.equals("logo")) {
                assertSame(failure, thrown);
            } else {
                assertInstanceOf(java.io.FileNotFoundException.class, thrown);
            }
        }
        assertEquals(0, emails.count());
        assertEquals(1, tokens.count());
        assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
        assertFalse(snapshot().isEnabled());
        assertTrue(snapshot().isVerificationPending());
    }

    @Test
    void standaloneCreationCannotInvalidateAnAlreadyQueuedLink() throws IOException {
        assertTrue(resend("user"));
        String active = activeTokens().getFirst().getToken();
        assertThrows(IllegalStateException.class, () -> verification.createToken(snapshot()));
        assertEquals(active, activeTokens().getFirst().getToken());
        assertCoherentQueue();
    }

    @Test
    void standaloneCreationAgainstResendKeepsQueueAndTokenCoherentInEitherOrder() throws Exception {
        rendezvous();
        var results = compete(() -> verification.createToken(snapshot()), () -> resend("email"));
        assertEquals(true, results.get(1));
        assertTrue(results.get(0) instanceof String || results.get(0) instanceof IllegalStateException);
        assertCoherentQueue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"token", "email", "user"})
    void queueInsertFailureRollsBackBothTheRealQueueAndTokenRotation(String entry) {
        var failure = new IllegalStateException("simulated failure after queue insert");
        doAnswer(call -> {
            var saved = emails.save(call.getArgument(0));
            emails.flush();
            throw failure;
        }).when(probes.emails()).save(any());
        assertSame(failure, assertThrows(IllegalStateException.class, () -> resend(entry)));
        assertEquals(0, emails.count());
        assertEquals(1, tokens.count());
        assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
        assertTrue(snapshot().isVerificationPending());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void technicalValidationFailureIsNotConvertedToSuccessOrInvalidLink(boolean optimistic) {
        RuntimeException failure = optimistic
                ? new OptimisticLockingFailureException("unrelated optimistic conflict")
                : new IllegalStateException("simulated database failure");
        doThrow(failure).when(probes.tokens()).findByTokenAndStatus("old", TokenStatus.ACTIVE);
        assertSame(failure, assertThrows(failure.getClass(), () -> verification.validateToken("old")));
        assertFalse(snapshot().isEnabled());
        assertTrue(snapshot().isVerificationPending());
        assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
    }

    @Test
    void adminGrantThenDisableCannotBeUndoneByAnOldUnusedLinkOrStaleResendUser() throws IOException {
        UserEntity stale = snapshot();
        editEnabled(true);
        assertFalse(snapshot().isVerificationPending());
        editEnabled(false);
        assertFalse(verification.validateToken("old"));
        assertFalse(verification.sendVerificationEmailByToken("old"));
        assertFalse(verification.sendVerificationEmailByEmail("test@example.com"));
        assertFalse(verification.sendVerificationEmailByUser(stale));
        assertThrows(IllegalStateException.class, () -> verification.createToken(stale));
        assertFalse(snapshot().isEnabled());
        assertFalse(snapshot().isVerificationPending());
        assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
        assertEquals(1, tokens.count());
        assertEquals(0, emails.count());
    }

    @Test
    void adminDisableAlsoBlocksAnAccountThatWasNeverEnabled() throws IOException {
        editEnabled(false);
        assertFalse(snapshot().isVerificationPending());
        assertFalse(verification.validateToken("old"));
        assertFalse(resend("user"));
        assertEquals(0, emails.count());
    }

    @Test
    void staleManagedAccountIsRefreshedBeforeTheEligibilityDecision() throws Exception {
        tx.executeWithoutResult(status -> {
            UserEntity cached = users.findById(id).orElseThrow();
            assertTrue(cached.isVerificationPending());
            try (var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(() -> { editEnabled(false); return null; }).get(10, TimeUnit.SECONDS);
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
            assertTrue(cached.isVerificationPending());
            assertFalse(verification.validateToken("old"));
            assertFalse(cached.isVerificationPending());
        });
        assertFalse(snapshot().isEnabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {"validate", "token", "email", "user"})
    void committedAdminDisableWinsAgainstAWaitingVerificationOperation(String entry) throws Exception {
        var adminHasWritten = new CountDownLatch(1);
        var verificationAttempted = new CountDownLatch(1);
        doAnswer(call -> {
            verificationAttempted.countDown();
            return users.lockVerificationUser(call.getArgument(0));
        }).when(probes.users()).lockVerificationUser(id);
        var results = compete(() -> {
            tx.executeWithoutResult(status -> {
                editEnabled(false);
                adminHasWritten.countDown();
                await(verificationAttempted);
            });
            return true;
        }, () -> {
            await(adminHasWritten);
            return entry.equals("validate") ? verification.validateToken("old") : resend(entry);
        });
        assertEquals(List.of(true, false), results);
        assertFalse(snapshot().isEnabled());
        assertFalse(snapshot().isVerificationPending());
        assertEquals(0, emails.count());
        assertEquals(TokenStatus.ACTIVE, tokenStatus("old"));
    }

    @Test
    void activationFirstCannotMakeAFollowingAdminDisableReactivatable() throws Exception {
        var verificationHasLock = new CountDownLatch(1);
        var adminAttempted = new CountDownLatch(1);
        var first = new AtomicBoolean(true);
        doAnswer(call -> {
            var user = users.lockVerificationUser(call.getArgument(0));
            if (first.getAndSet(false)) {
                verificationHasLock.countDown();
                await(adminAttempted);
            }
            return user;
        }).when(probes.users()).lockVerificationUser(id);
        doAnswer(call -> {
            adminAttempted.countDown();
            return users.findByIdForUpdate(call.getArgument(0));
        }).when(probes.users()).findByIdForUpdate(id);
        var results = compete(() -> verification.validateToken("old"), () -> {
            await(verificationHasLock);
            tx.executeWithoutResult(status -> {
                UserEntity fresh = probes.users().findByIdForUpdate(id).orElseThrow();
                Long version = fresh.getVersion();
                fresh.setEnabled(false);
                admin.updateAdminUser(fresh, version, "");
            });
            return true;
        });
        assertEquals(List.of(true, true), results);
        assertFalse(snapshot().isEnabled());
        assertFalse(snapshot().isVerificationPending());
        assertEquals(TokenStatus.USED, tokenStatus("old"));
        // Remove synchronization hooks before the sequential regression check.
        reset(probes.users());
        assertFalse(verification.validateToken("old"));
        assertFalse(resend("email"));
    }

    @Test
    void staleAdminFormStillFailsInsteadOfOverwritingCommittedVerification() {
        UserEntity stale = snapshot();
        Long version = stale.getVersion();
        stale.setEnabled(false);
        assertTrue(verification.validateToken("old"));
        assertThrows(OptimisticLockingFailureException.class, () -> admin.updateAdminUser(stale, version, ""));
        assertTrue(snapshot().isEnabled());
        editEnabled(false);
        assertFalse(verification.validateToken("old"));
    }

    @Test
    void directQueueProducersAreSerializedAndCannotQueueForBlockedAccounts() throws Exception {
        rendezvous();
        Callable<Object> producer = () -> {
            mailQueue.addEmailToQueue(snapshot(), "Subject", "Body", EmailType.VERIFICATION);
            return true;
        };
        assertEquals(List.of(true, true), compete(producer, producer));
        assertEquals(1, emails.count());
        reset(probes.users());
        editEnabled(false);
        tx.executeWithoutResult(status -> emails.deleteAll());
        mailQueue.addEmailToQueue(snapshot(), "Subject", "Body", EmailType.VERIFICATION);
        assertEquals(0, emails.count());
    }

    private void rendezvous() {
        var barrier = new CyclicBarrier(2);
        Set<Long> threads = ConcurrentHashMap.newKeySet();
        doAnswer(call -> {
            if (threads.add(Thread.currentThread().threadId())) {
                barrier.await(10, TimeUnit.SECONDS);
            }
            return users.lockVerificationUser(call.getArgument(0));
        }).when(probes.users()).lockVerificationUser(id);
    }

    private List<Object> compete(Callable<Object> one, Callable<Object> two) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> capture(one));
            var second = executor.submit(() -> capture(two));
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
    }

    private Object capture(Callable<Object> work) {
        try {
            return work.call();
        } catch (Exception failure) {
            return failure;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "controlled operation did not arrive");
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failure);
        }
    }

    private boolean resend(String entry) throws IOException {
        return switch (entry) {
            case "token" -> verification.sendVerificationEmailByToken("old");
            case "email" -> verification.sendVerificationEmailByEmail("test@example.com");
            case "user" -> verification.sendVerificationEmailByUser(snapshot());
            default -> throw new IllegalArgumentException(entry);
        };
    }

    private UserEntity snapshot() {
        return tx.execute(status -> {
            UserEntity user = users.findById(id).orElseThrow();
            user.getRoleEntities().size();
            return user;
        });
    }

    private void editEnabled(boolean enabled) {
        UserEntity edited = snapshot();
        Long version = edited.getVersion();
        edited.setEnabled(enabled);
        admin.updateAdminUser(edited, version, "");
    }

    private TokenStatus tokenStatus(String value) {
        return tokens.findByToken(value).orElseThrow().getStatus();
    }

    private List<VerificationTokenEntity> activeTokens() {
        return tx.execute(status -> tokens.findAllByUserEntityAndStatus(snapshot(), TokenStatus.ACTIVE));
    }

    private void assertCoherentQueue() {
        var active = activeTokens();
        assertEquals(1, active.size());
        var queued = emails.findAll();
        assertEquals(1, queued.size());
        assertEquals(EmailStatus.PENDING, queued.getFirst().getStatus());
        assertTrue(queued.getFirst().getBody().contains("/verification/" + active.getFirst().getToken()));
        assertFalse(snapshot().isEnabled());
        assertTrue(snapshot().isVerificationPending());
    }

    record Probes(UserRepository users, VerificationTokenRepository tokens, EmailQueueRepository emails) {
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:verification-queue-" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        }
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("de.derpeterson.app.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        TransactionTemplate transactionTemplate(JpaTransactionManager manager) {
            return new TransactionTemplate(manager);
        }

        @Bean
        Probes probes(UserRepository users, VerificationTokenRepository tokens, EmailQueueRepository emails) {
            return new Probes(mock(UserRepository.class, delegatesTo(users)),
                    mock(VerificationTokenRepository.class, delegatesTo(tokens)), mock(EmailQueueRepository.class, delegatesTo(emails)));
        }
        @Bean
        ConfigService configService() {
            var config = mock(ConfigService.class);
            when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1H");
            when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test Service");
            when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.com/");
            return config;
        }
        @Bean
        EmailQueueService emailQueueService(ConfigService config, Probes probes) {
            return new EmailQueueService(config, probes.emails(), mock(EmailService.class), probes.users());
        }
        @Bean
        VerificationService verificationService(ConfigService config, Probes probes, EmailQueueService queue) {
            var messages = mock(MessageProperties.class);
            when(messages.getEmailVerificationSubject()).thenReturn("Verify account");
            return new VerificationService(messages, probes.tokens(), probes.users(), config, queue);
        }
        @Bean
        UserService userService(Probes probes) {
            return new UserService(probes.users(), mock(UserStatusBroadcaster.class), mock(PasswordEncoder.class));
        }
    }
}
