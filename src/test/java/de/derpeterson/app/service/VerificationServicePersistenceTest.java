package de.derpeterson.app.service;

import de.derpeterson.app.helper.image.ImageHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.VerificationTokenEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.TokenStatus;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** Real repositories and service transaction proxy, never the application or mail scheduler. */
@SpringJUnitConfig(VerificationServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VerificationServicePersistenceTest {
    @Autowired
    private VerificationService service;
    @Autowired
    private VerificationTokenRepository tokens;
    @Autowired
    private UserRepository users;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private Probes probes;
    private Long userId;

    @BeforeEach
    void setUp() {
        reset(probes.tokens(), probes.config(), probes.queue());
        when(probes.config().getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1H");
        when(probes.config().getString(ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION)).thenReturn("P7D");
        when(probes.config().getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test Service");
        when(probes.config().getString(ConfigEntry.BASE_URL)).thenReturn("https://example.com/");
        transaction.executeWithoutResult(status -> {
            tokens.deleteAll();
            users.deleteAll();
            users.flush();
            var user = UserEntity.builder().firstName("Test").lastName("User").email("test@example.com")
                    .password("hash").gender(Gender.OTHER).birthDate(LocalDate.of(1990, 1, 1))
                    .preferredLocale(Locale.ENGLISH).enabled(false).build();
            userId = users.saveAndFlush(user).getId();
        });
    }

    @Test
    void activationAndConsumptionCommitTogetherAndUsedTokenCannotReactivateDisabledAccount() {
        seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        assertTrue(service.validateToken("valid"));
        assertTrue(users.findById(userId).orElseThrow().isEnabled());
        assertEquals(TokenStatus.USED, status("valid"));
        assertFalse(service.validateToken("valid"));
        transaction.executeWithoutResult(tx -> users.findById(userId).orElseThrow().setEnabled(false));
        assertFalse(service.validateToken("valid"));
        assertFalse(users.findById(userId).orElseThrow().isEnabled());
    }

    @ParameterizedTest
    @EnumSource(value = TokenStatus.class, names = {"INACTIVE", "USED", "EXPIRED"})
    void nonActiveTokensExistButCannotActivateAccounts(TokenStatus tokenStatus) {
        seed("rejected", tokenStatus, LocalDateTime.now().plusDays(1));
        assertTrue(service.existsToken("rejected"));
        assertFalse(service.validateToken("rejected"));
        assertFalse(users.findById(userId).orElseThrow().isEnabled());
        assertEquals(tokenStatus, status("rejected"));
    }

    @Test
    void expiredActiveTokenIsPersistentlyMarkedExpiredWithoutEnablingAccount() {
        seed("expired", TokenStatus.ACTIVE, LocalDateTime.now().minusDays(1));
        assertFalse(service.validateToken("expired"));
        assertEquals(TokenStatus.EXPIRED, status("expired"));
        assertFalse(users.findById(userId).orElseThrow().isEnabled());
    }

    @Test
    void sequentialCreationInvalidatesOnlyActiveTokens() {
        seed("inactive", TokenStatus.INACTIVE, LocalDateTime.now().plusDays(1));
        seed("used", TokenStatus.USED, LocalDateTime.now().plusDays(1));
        seed("expired", TokenStatus.EXPIRED, LocalDateTime.now().minusDays(1));
        String first = service.createToken(detachedUser());
        String second = service.createToken(detachedUser());
        assertNotEquals(first, second);
        assertEquals(TokenStatus.INACTIVE, status(first));
        assertEquals(TokenStatus.ACTIVE, status(second));
        assertEquals(TokenStatus.INACTIVE, status("inactive"));
        assertEquals(TokenStatus.USED, status("used"));
        assertEquals(TokenStatus.EXPIRED, status("expired"));
        assertFalse(service.validateToken(first));
        assertTrue(service.validateToken(second));
    }

    @Test
    void invalidDurationRollsBackInvalidationOfExistingToken() {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        when(probes.config().getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("invalid");
        assertThrows(DateTimeParseException.class, () -> service.createToken(detachedUser()));
        assertEquals(TokenStatus.ACTIVE, status("old"));
        assertEquals(1, tokens.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT0S", "-PT1H"})
    void nonPositiveConfiguredLifetimeCreatesAnImmediatelyUnusableToken(String lifetime) {
        when(probes.config().getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn(lifetime);
        LocalDateTime now = LocalDateTime.of(2026, 10, 7, 12, 0);
        LocalDateTime later = now.plusSeconds(1);
        try (var clock = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            clock.when(LocalDateTime::now).thenReturn(now);
            String value = service.createToken(detachedUser());
            assertEquals(TokenStatus.ACTIVE, status(value));
            clock.when(LocalDateTime::now).thenReturn(later);
            assertFalse(service.validateToken(value));
            assertEquals(TokenStatus.EXPIRED, status(value));
        }
        assertFalse(users.findById(userId).orElseThrow().isEnabled());
    }

    @Test
    void emailResendUsesCanonicalIdentityAndCommitsRotationWithoutActivatingAccount() throws IOException {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        try (var locale = mockStatic(CustomI18NProvider.class)) {
            locale.when(CustomI18NProvider::getCurrentLocale).thenReturn(Locale.GERMAN);
            assertTrue(service.sendVerificationEmailByEmail("  TEST@EXAMPLE.COM  "));
        }
        assertEquals(TokenStatus.INACTIVE, status("old"));
        assertEquals(1, activeCount());
        assertEquals(2, tokens.count());
        assertFalse(users.findById(userId).orElseThrow().isEnabled());
        verify(probes.queue()).addEmailToQueue(any(UserEntity.class), eq("Verify account"),
                contains("Willkommen Test User bei Test Service!"), eq(EmailType.VERIFICATION));
    }

    @Test
    void enabledAccountRejectsValidationButLeavesTheUnusedTokenActive() {
        seed("unused", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        transaction.executeWithoutResult(tx -> users.findById(userId).orElseThrow().setEnabled(true));
        assertFalse(service.validateToken("unused"));
        assertEquals(TokenStatus.ACTIVE, status("unused"));
        assertTrue(users.findById(userId).orElseThrow().isEnabled());
    }

    @Test
    void failureSavingUsedStatusRollsBackAccountActivationEvenAfterFlush() {
        seed("valid", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        doAnswer(call -> {
            var entity = (VerificationTokenEntity) call.getArgument(0);
            if (entity.getStatus() == TokenStatus.USED) {
                users.flush();
                throw new IllegalStateException("simulated token persistence failure");
            }
            return tokens.save(entity);
        }).when(probes.tokens()).save(any(VerificationTokenEntity.class));
        assertThrows(IllegalStateException.class, () -> service.validateToken("valid"));
        assertFalse(users.findById(userId).orElseThrow().isEnabled());
        assertEquals(TokenStatus.ACTIVE, status("valid"));
    }

    @Test
    void queueRuntimeFailureRollsBackTokenRotation() {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        doThrow(new IllegalStateException("simulated queue failure")).when(probes.queue())
                .addEmailToQueue(any(), any(), any(), eq(EmailType.VERIFICATION));
        try (var locale = mockStatic(CustomI18NProvider.class)) {
            locale.when(CustomI18NProvider::getCurrentLocale).thenReturn(Locale.ENGLISH);
            assertThrows(IllegalStateException.class, () -> service.sendVerificationEmailByToken("old"));
        }
        assertEquals(TokenStatus.ACTIVE, status("old"));
        assertEquals(1, tokens.count());
    }

    @Test
    void findingCheckedTemplateFailureCommitsRotationWithoutQueuingEmail() {
        seed("old", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var failure = new IOException("simulated classpath read failure");
        try (var image = mockStatic(ImageHelper.class)) {
            image.when(() -> ImageHelper.convertImageToBase64("META-INF/resources/custom-theme/service_logo.png"))
                    .thenThrow(failure);
            assertSame(failure, assertThrows(IOException.class, () -> service.sendVerificationEmailByToken("old")));
        }
        assertEquals(TokenStatus.INACTIVE, status("old"));
        assertEquals(2, tokens.count());
        assertEquals(1, activeCount());
        verify(probes.queue(), never()).addEmailToQueue(any(), any(), any(), any());
    }

    @Test
    void findingCleanupFormattingFailureRollsBackDeletionOfOldTokens() {
        seed("old", TokenStatus.EXPIRED, LocalDateTime.now().minusDays(8));
        seed("recent", TokenStatus.EXPIRED, LocalDateTime.now().minusDays(1));
        assertThrows(UnsupportedTemporalTypeException.class, service::deleteExpiredTokens);
        assertTrue(service.existsToken("old"));
        assertTrue(service.existsToken("recent"));
        assertEquals(2, tokens.count());
    }

    @Test
    void findingDeleteTokenNeedsAnExternalTransaction() {
        seed("delete", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        assertThrows(InvalidDataAccessApiUsageException.class, () -> service.deleteToken("delete"));
        assertTrue(service.existsToken("delete"));
        int deleted = transaction.execute(tx -> service.deleteToken("delete"));
        assertEquals(1, deleted);
        assertFalse(service.existsToken("delete"));
        int missing = transaction.execute(tx -> service.deleteToken("delete"));
        assertEquals(0, missing);
    }

    @Test
    void findingConcurrentCreationCanCommitTwoActiveTokensForTheSameAccount() throws Exception {
        var bothRead = new CyclicBarrier(2);
        doAnswer(call -> {
            var found = tokens.findAllByUserEntityAndStatus(call.getArgument(0), call.getArgument(1));
            assertTrue(found.isEmpty());
            bothRead.await(10, TimeUnit.SECONDS);
            return found;
        }).when(probes.tokens()).findAllByUserEntityAndStatus(any(), eq(TokenStatus.ACTIVE));
        UserEntity user = detachedUser();
        var values = race(() -> service.createToken(user));
        assertInstanceOf(String.class, values.get(0));
        assertInstanceOf(String.class, values.get(1));
        assertNotEquals(values.get(0), values.get(1));
        assertEquals(2, activeCount());
        assertEquals(2, tokens.count());
    }

    @Test
    void concurrentValidationHasOneCommittedWinnerAndOneOptimisticLockFailure() throws Exception {
        seed("shared", TokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        var bothRead = new CyclicBarrier(2);
        doAnswer(call -> {
            var found = tokens.findByTokenAndStatus(call.getArgument(0), call.getArgument(1));
            assertFalse(found.orElseThrow().getUserEntity().isEnabled());
            bothRead.await(10, TimeUnit.SECONDS);
            return found;
        }).when(probes.tokens()).findByTokenAndStatus("shared", TokenStatus.ACTIVE);
        var results = race(() -> service.validateToken("shared"));
        assertEquals(1, results.stream().filter(Boolean.TRUE::equals).count());
        assertEquals(1, results.stream().filter(OptimisticLockingFailureException.class::isInstance).count());
        assertTrue(users.findById(userId).orElseThrow().isEnabled());
        assertEquals(TokenStatus.USED, status("shared"));
    }

    private List<Object> race(Callable<Object> work) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Object> capture = () -> {
                try {
                    return work.call();
                } catch (Exception failure) {
                    return failure;
                }
            };
            var first = executor.submit(capture);
            var second = executor.submit(capture);
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
    }

    private UserEntity detachedUser() {
        return users.findById(userId).orElseThrow();
    }

    private void seed(String value, TokenStatus tokenStatus, LocalDateTime expiry) {
        transaction.executeWithoutResult(tx -> tokens.saveAndFlush(new VerificationTokenEntity(null, value,
                users.findById(userId).orElseThrow(), expiry, tokenStatus)));
    }

    private TokenStatus status(String value) {
        return transaction.execute(tx -> tokens.findByToken(value).orElseThrow().getStatus());
    }

    private int activeCount() {
        return transaction.execute(tx -> tokens.findAllByUserEntityAndStatus(
                users.findById(userId).orElseThrow(), TokenStatus.ACTIVE).size());
    }

    record Probes(VerificationTokenRepository tokens, ConfigService config, EmailQueueService queue) {
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:verification-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
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
        Probes probes(VerificationTokenRepository tokens) {
            return new Probes(mock(VerificationTokenRepository.class, delegatesTo(tokens)),
                    mock(ConfigService.class), mock(EmailQueueService.class));
        }

        @Bean
        VerificationService verificationService(Probes probes, UserRepository users) {
            var messages = mock(MessageProperties.class);
            when(messages.getEmailVerificationSubject()).thenReturn("Verify account");
            return new VerificationService(messages, probes.tokens(), users, probes.config(), probes.queue());
        }
    }
}
