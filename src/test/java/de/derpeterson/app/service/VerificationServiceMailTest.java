package de.derpeterson.app.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.derpeterson.app.helper.image.ImageHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated logger/resource fixtures; never starts SMTP, scheduling or the application. */
@ExtendWith(MockitoExtension.class)
@Isolated
class VerificationServiceMailTest {
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
        service = new VerificationService(messages, tokens, users, config, queue,
                Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC));
        user = UserEntity.builder().id(1L).firstName("Test").lastName("User").email("test@example.com").build();
        when(users.lockVerificationUser(1L)).thenReturn(Optional.of(user));
        lenient().when(config.getString(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION)).thenReturn("PT1H");
        lenient().when(config.getString(ConfigEntry.SERVICE_NAME)).thenReturn("Test Service");
        lenient().when(config.getString(ConfigEntry.BASE_URL)).thenReturn("https://example.com/");
        lenient().when(messages.getEmailVerificationSubject()).thenReturn("Verify account");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void templateStreamIsClosedOnSuccessAndOnReadFailure(boolean readFailure, boolean closeFailure) throws IOException {
        var stream = new TemplateStream(readFailure, closeFailure);
        try (var image = mockStatic(ImageHelper.class);
             var locale = mockStatic(CustomI18NProvider.class);
             var resources = mockConstruction(ClassPathResource.class, (resource, context) -> {
                 assertEquals("email/welcome_en.html", context.arguments().getFirst());
                 when(resource.getInputStream()).thenReturn(stream);
             })) {
            image.when(() -> ImageHelper.convertImageToBase64("META-INF/resources/custom-theme/service_logo.png"))
                    .thenReturn("logo");
            locale.when(CustomI18NProvider::getCurrentLocale).thenReturn(Locale.ENGLISH);
            if (readFailure || closeFailure) {
                IOException failure = assertThrows(IOException.class, () -> service.sendVerificationEmailByUser(user));
                assertSame(readFailure ? stream.readError : stream.closeError, failure);
                assertArrayEquals(readFailure && closeFailure ? new Throwable[]{stream.closeError} : new Throwable[0],
                        failure.getSuppressed());
                verify(queue, never()).addEmailToQueue(any(), any(), any(), any());
            } else {
                assertTrue(service.sendVerificationEmailByUser(user));
                verify(queue).addEmailToQueue(user, "Verify account", "Hello Test", EmailType.VERIFICATION);
            }
            assertEquals(1, resources.constructed().size());
            assertTrue(stream.closed);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"queued", "duplicate", "queueFailure", "templateFailure"})
    void logReportsQueueAdditionOnlyAfterSuccessfulEnqueue(String outcome) throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(VerificationService.class);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);
        try (var locale = mockStatic(CustomI18NProvider.class)) {
            locale.when(CustomI18NProvider::getCurrentLocale).thenReturn(
                    outcome.equals("templateFailure") ? Locale.FRENCH : Locale.ENGLISH);
            if (outcome.equals("duplicate")) {
                when(queue.hasOpenEmailForUserAndType(user, EmailType.VERIFICATION)).thenReturn(true);
            } else if (outcome.equals("queueFailure")) {
                doAnswer(call -> {
                    assertTrue(logs.list.stream().noneMatch(event -> event.getFormattedMessage().contains("added to the mail queue")));
                    throw new IllegalStateException("queue failed");
                }).when(queue).addEmailToQueue(any(), any(), any(), any());
            }
            if (outcome.equals("queueFailure")) {
                assertThrows(IllegalStateException.class, () -> service.sendVerificationEmailByUser(user));
            } else if (outcome.equals("templateFailure")) {
                assertThrows(IOException.class, () -> service.sendVerificationEmailByUser(user));
            } else {
                assertTrue(service.sendVerificationEmailByUser(user));
            }
            assertEquals(outcome.equals("queued") ? 1 : 0, logs.list.stream()
                    .filter(event -> event.getFormattedMessage().contains("added to the mail queue")).count());
            assertTrue(logs.list.stream().noneMatch(event -> event.getFormattedMessage().contains("sent")));
            if (outcome.equals("queued") || outcome.equals("queueFailure")) {
                verify(queue).addEmailToQueue(eq(user), eq("Verify account"), anyString(), eq(EmailType.VERIFICATION));
            } else {
                verify(queue, never()).addEmailToQueue(any(), any(), any(), any());
            }
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }

    private static class TemplateStream extends InputStream {
        private final InputStream content = new ByteArrayInputStream("Hello {{FIRST_NAME}}".getBytes(StandardCharsets.UTF_8));
        private final IOException readError = new IOException("template read failed");
        private final IOException closeError = new IOException("template close failed");
        private final boolean readFailure;
        private final boolean closeFailure;
        private boolean closed;

        TemplateStream(boolean readFailure, boolean closeFailure) {
            this.readFailure = readFailure;
            this.closeFailure = closeFailure;
        }

        @Override
        public int read() throws IOException {
            if (readFailure) {
                throw readError;
            }
            return content.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (readFailure) {
                throw readError;
            }
            return content.read(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            content.close();
            if (closeFailure) {
                throw closeError;
            }
        }
    }
}
