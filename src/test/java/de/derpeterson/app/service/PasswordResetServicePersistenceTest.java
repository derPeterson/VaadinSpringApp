package de.derpeterson.app.service;

import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.PasswordResetTokenEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.PasswordResetTokenRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** Real service transaction proxy and isolated H2; no Boot, scheduler or SMTP. */
@SpringJUnitConfig(PasswordResetServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordResetServicePersistenceTest {
    @Autowired
    private PasswordResetService service;
    @Autowired
    private PasswordResetTokenRepository tokens;
    @Autowired
    private UserRepository users;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private Probes probes;
    private Long userId;

    @BeforeEach
    void setup() {
        reset(probes.tokens(), probes.users(), probes.config(), probes.encoder(), probes.queue());
        when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION)).thenReturn("PT1H");
        when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)).thenReturn("P7D");
        when(probes.config().getString(ConfigEntry.SERVICE_NAME)).thenReturn("Reset Test Service");
        when(probes.config().getString(ConfigEntry.BASE_URL)).thenReturn("https://example.invalid/");
        when(probes.encoder().encode(any())).thenAnswer(call -> "hash-" + call.getArgument(0));
        transaction.executeWithoutResult(tx -> {
            tokens.deleteAll();
            users.deleteAll();
            users.flush();
            userId = users.saveAndFlush(UserEntity.builder().firstName("Test").lastName("User")
                    .email("test@example.com").password("old-hash").gender(Gender.OTHER)
                    .birthDate(LocalDate.of(1990, 1, 1)).preferredLocale(Locale.ENGLISH).enabled(true).build()).getId();
        });
    }

    private void seed(String value, TokenStatus status, LocalDateTime expiry) {
        transaction.executeWithoutResult(tx -> tokens.saveAndFlush(new PasswordResetTokenEntity(null, value,
                users.findById(userId).orElseThrow(), expiry, status)));
    }

    private TokenStatus status(String value) {
        return transaction.execute(tx -> tokens.findByToken(value).orElseThrow().getStatus());
    }

    private String password() {
        return users.findById(userId).orElseThrow().getPassword();
    }

    @Test
    void successfulResetCommitsPasswordAndConsumptionTogetherAndSequentialReuseFails() {
        seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        assertTrue(service.resetPassword("valid", "Password!"));
        assertEquals("hash-Password!", password());
        assertEquals(TokenStatus.USED, status("valid"));
        assertTrue(service.existsToken("valid"));
        assertFalse(service.validateToken("valid"));
        assertFalse(service.resetPassword("valid", "OtherPassword!"));
        assertEquals("hash-Password!", password());
        verify(probes.encoder(), times(1)).encode(any());
    }

    @ParameterizedTest
    @EnumSource(value = TokenStatus.class, names = {"USED", "INACTIVE", "EXPIRED"})
    void nonActiveTokensExistButCannotValidateOrChangePasswords(TokenStatus tokenStatus) {
        seed("closed", tokenStatus, LocalDateTime.now().plusDays(1));
        assertTrue(service.existsToken("closed"));
        assertFalse(service.validateToken("closed"));
        assertFalse(service.resetPassword("closed", "Password!"));
        assertEquals(tokenStatus, status("closed"));
        assertEquals("old-hash", password());
        verifyNoInteractions(probes.encoder());
    }

    @Test
    void expiredTokensAreMarkedExpiredByBothEntrypointsWithoutChangingPassword() {
        seed("validate-expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        seed("reset-expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        assertFalse(service.validateToken("validate-expired"));
        assertFalse(service.resetPassword("reset-expired", "Password!"));
        assertEquals(TokenStatus.EXPIRED, status("validate-expired"));
        assertEquals(TokenStatus.EXPIRED, status("reset-expired"));
        assertEquals("old-hash", password());
    }

    @Test
    void disabledAccountRejectsResetAndValidationInsideAnAmbientTransaction() {
        seed("disabled", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        transaction.executeWithoutResult(tx -> users.findById(userId).orElseThrow().setEnabled(false));
        assertFalse(service.resetPassword("disabled", "Password!"));
        boolean valid = transaction.execute(tx -> service.validateToken("disabled"));
        assertFalse(valid);
        assertEquals(TokenStatus.ACTIVE, status("disabled"));
        assertEquals("old-hash", password());
    }

    @Test
    void validTokenValidationCurrentlyNeedsAnAmbientPersistenceContext() {
        seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        assertThrows(LazyInitializationException.class, () -> service.validateToken("valid"));
        boolean valid = transaction.execute(tx -> service.validateToken("valid"));
        assertTrue(valid);
        assertEquals(TokenStatus.ACTIVE, status("valid"));
    }

    @Test
    void newTokenInvalidatesOnlyOldActiveTokensAndCanBeUsed() {
        seed("active", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        seed("used", TokenStatus.USED, LocalDateTime.now().plusDays(1));
        seed("expired", TokenStatus.EXPIRED, LocalDateTime.now().minusDays(1));
        String first = service.createToken(users.findById(userId).orElseThrow());
        String second = service.createToken(users.findById(userId).orElseThrow());
        assertNotEquals(first, second);
        assertEquals(TokenStatus.INACTIVE, status("active"));
        assertEquals(TokenStatus.INACTIVE, status(first));
        assertEquals(TokenStatus.ACTIVE, status(second));
        assertEquals(TokenStatus.USED, status("used"));
        assertEquals(TokenStatus.EXPIRED, status("expired"));
        assertFalse(service.resetPassword(first, "Password!"));
        assertTrue(service.resetPassword(second, "Password!"));
    }

    @Test
    void statusSetterCurrentlyReactivatesUsedTokensAndAllowsASecondPasswordChange() {
        seed("reusable", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        assertTrue(service.resetPassword("reusable", "Password!"));
        service.setTokenStatus("reusable", TokenStatus.ACTIVE);
        assertTrue(service.resetPassword("reusable", "OtherPassword!"));
        assertEquals("hash-OtherPassword!", password());
        assertEquals(TokenStatus.USED, status("reusable"));
    }

    @Test
    void nullStatusIsRejectedByPersistenceWithoutChangingStoredStatus() {
        seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        assertThrows(DataIntegrityViolationException.class, () -> service.setTokenStatus("valid", null));
        assertEquals(TokenStatus.ACTIVE, status("valid"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"encoder", "user-save", "token-save"})
    void runtimeFailuresRollBackBothPasswordAndTokenAndPermitASubsequentRetry(String stage) {
        seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var failure = new DataAccessResourceFailureException("isolated " + stage);
        if (stage.equals("encoder")) {
            doThrow(failure).when(probes.encoder()).encode("Password!");
        } else if (stage.equals("user-save")) {
            doThrow(failure).when(probes.users()).save(any());
        } else {
            doThrow(failure).when(probes.tokens()).save(any());
        }
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.resetPassword("valid", "Password!")));
        assertEquals("old-hash", password());
        assertEquals(TokenStatus.ACTIVE, status("valid"));
        reset(probes.encoder(), probes.users(), probes.tokens());
        when(probes.encoder().encode("Password!")).thenReturn("retry-hash");
        assertTrue(service.resetPassword("valid", "Password!"));
        assertEquals("retry-hash", password());
        assertEquals(TokenStatus.USED, status("valid"));
    }

    @Test
    void invalidPasswordDoesNotConsumeEvenAnExpiredToken() {
        seed("expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("expired", "weak"));
        assertEquals(TokenStatus.ACTIVE, status("expired"));
        assertEquals("old-hash", password());
        verifyNoInteractions(probes.encoder());
    }

    @Test
    void invalidDurationRollsBackInvalidationDuringCreation() {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        when(probes.config().getString(ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION)).thenReturn("invalid");
        assertThrows(java.time.format.DateTimeParseException.class,
                () -> service.createToken(users.findById(userId).orElseThrow()));
        assertEquals(TokenStatus.ACTIVE, status("old"));
        assertEquals(1, tokens.count());
    }

    @Test
    void cleanupFormattingFailureRollsBackTheActualDeletion() {
        seed("old", TokenStatus.USED, LocalDateTime.now().minusDays(10));
        seed("recent", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        assertThrows(UnsupportedTemporalTypeException.class, service::deleteExpiredTokens);
        assertTrue(service.existsToken("old"));
        assertTrue(service.existsToken("recent"));
        assertEquals(2, tokens.count());
    }

    @Test
    void checkedTemplateIoFailureCurrentlyCommitsTokenRotationWithoutQueuingMail() {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var session = mock(VaadinSession.class);
        when(session.getLocale()).thenReturn(Locale.FRENCH);
        VaadinSession.setCurrent(session);
        try {
            assertThrows(IOException.class, () -> service.sendPasswordResetEmail("test@example.com"));
        } finally {
            VaadinSession.setCurrent(null);
        }
        assertEquals(TokenStatus.INACTIVE, status("old"));
        assertEquals(2, tokens.count());
        int active = transaction.execute(tx -> tokens.findAllByUserEntityAndStatus(
                users.findById(userId).orElseThrow(), TokenStatus.ACTIVE).size());
        assertEquals(1, active);
        verify(probes.queue(), never()).addEmailToQueue(any(), any(), any(), any());
    }

    @Test
    void runtimeQueueFailureRollsBackTokenRotation() {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var failure = new DataAccessResourceFailureException("isolated queue failure");
        doThrow(failure).when(probes.queue()).addEmailToQueue(any(), any(), any(), any());
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service.sendPasswordResetEmail("test@example.com")));
        assertEquals(TokenStatus.ACTIVE, status("old"));
        assertEquals(1, tokens.count());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void successfulMailRequestCommitsOneNewTokenAndQueuesItsLinkWithoutSmtp(boolean german) throws IOException {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var session = mock(VaadinSession.class);
        when(session.getLocale()).thenReturn(german ? Locale.GERMAN : Locale.ENGLISH);
        VaadinSession.setCurrent(session);
        try {
            assertTrue(service.sendPasswordResetEmail("test@example.com"));
        } finally {
            VaadinSession.setCurrent(null);
        }
        assertEquals(TokenStatus.INACTIVE, status("old"));
        var value = transaction.execute(tx -> tokens.findAllByUserEntityAndStatus(
                users.findById(userId).orElseThrow(), TokenStatus.ACTIVE).getFirst().getToken());
        var html = ArgumentCaptor.forClass(String.class);
        verify(probes.queue()).addEmailToQueue(argThat(user -> userId.equals(user.getId())), eq("Reset password"),
                html.capture(), eq(de.derpeterson.app.model.enums.EmailType.PASSWORD_RESET));
        assertTrue(html.getValue().contains("https://example.invalid/reset-password/" + value));
        assertEquals(2, tokens.count());
    }

    @RepeatedTest(3)
    void secondTransactionThatReadActiveBeforeFirstCommitCanCurrentlyReuseTheToken() throws Exception {
        seed("race", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var activeRead = new CountDownLatch(1);
        var firstCommitted = new CountDownLatch(1);
        var staleReader = new AtomicReference<Thread>();
        // Only pause after the real token query. Keep the lazy user uninitialized:
        // the second transaction reads the current user version after the first commit.
        doAnswer(call -> {
            var result = tokens.findByTokenAndStatus("race", TokenStatus.ACTIVE);
            if (Thread.currentThread() == staleReader.get()) {
                assertTrue(result.isPresent());
                activeRead.countDown();
                assertTrue(firstCommitted.await(10, TimeUnit.SECONDS));
            }
            return result;
        }).when(probes.tokens()).findByTokenAndStatus("race", TokenStatus.ACTIVE);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var second = executor.submit(() -> {
                staleReader.set(Thread.currentThread());
                return service.resetPassword("race", "SecondPassword!");
            });
            assertTrue(activeRead.await(10, TimeUnit.SECONDS));
            assertTrue(service.resetPassword("race", "FirstPassword!"));
            firstCommitted.countDown();
            assertTrue(second.get(10, TimeUnit.SECONDS), "Characterizes the currently missing atomic single-use guard");
        } finally {
            firstCommitted.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals("hash-SecondPassword!", password());
        assertEquals(TokenStatus.USED, status("race"));
    }

    record Probes(PasswordResetTokenRepository tokens, UserRepository users, ConfigService config,
                  PasswordEncoder encoder, EmailQueueService queue) {
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:password-reset-" + UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
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
        Probes probes(PasswordResetTokenRepository tokens, UserRepository users) {
            return new Probes(mock(PasswordResetTokenRepository.class, delegatesTo(tokens)),
                    mock(UserRepository.class, delegatesTo(users)), mock(ConfigService.class),
                    mock(PasswordEncoder.class), mock(EmailQueueService.class));
        }

        @Bean
        PasswordResetService passwordResetService(Probes probes) {
            var messages = mock(MessageProperties.class);
            when(messages.getEmailResetPasswordSubject()).thenReturn("Reset password");
            return new PasswordResetService(messages, probes.tokens(), probes.users(), probes.config(), probes.encoder(), probes.queue());
        }
    }
}
