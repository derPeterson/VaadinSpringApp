package de.derpeterson.app.views;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real direct caller callbacks and language events; no server, database or SMTP. */
class PasswordResetViewsTest {
    private UI ui;
    private VaadinSession session;
    private PasswordResetService service;
    private ResetPasswordView resetView;
    private ForgotPasswordView forgotView;
    private final CustomI18NProvider provider = new CustomI18NProvider();
    private final MessageProperties texts = new MessageProperties(provider);

    @BeforeEach
    void setup() {
        ui = new UI();
        session = mock(VaadinSession.class);
        var locale = new AtomicReference<>(Locale.ENGLISH);
        when(session.getLocale()).thenAnswer(call -> locale.get());
        when(session.hasLock()).thenReturn(true);
        doAnswer(call -> { locale.set(call.getArgument(0)); return null; }).when(session).setLocale(any());
        UI.setCurrent(ui);
        VaadinSession.setCurrent(session);
        service = mock(PasswordResetService.class);
    }

    @AfterEach
    void cleanup() {
        NotificationHelper.getInstance().closeAndClearAllNotifications();
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        resetView = null;
        forgotView = null;
        session = null;
        ui = null;
    }

    private <T> T field(Object view, String name, Class<T> type) {
        return type.cast(ReflectionTestUtils.getField(view, name));
    }

    private void createResetForm() {
        when(service.validateToken("link-token")).thenReturn(true);
        resetView = new ResetPasswordView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
        resetView.setParameter(null, "link-token");
        field(resetView, "passwordField", PasswordField.class).setValue("Password!");
        field(resetView, "confirmPasswordField", PasswordField.class).setValue("Password!");
    }

    private void notification(boolean success, String body, Locale locale) {
        var helper = NotificationHelper.getInstance();
        var title = (Span) ReflectionTestUtils.getField(helper, "titleText");
        var message = (Html) ReflectionTestUtils.getField(helper, "messageText");
        assertEquals(provider.getTranslation(success ? "base.success.title" : "base.failed.title", locale), title.getText());
        assertEquals(body, message.getElement().getProperty("innerHTML"));
    }

    @Test
    void rejectedTokenShowsInvalidCardWithoutOfferingPasswordChangeAndTranslates() {
        resetView = new ResetPasswordView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
        resetView.setParameter(null, "used-or-expired");
        assertNull(ReflectionTestUtils.getField(resetView, "resetButton"));
        assertEquals(texts.getResetPasswordInvalidTitle(), field(resetView, "invalidTitle", H1.class).getText());
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertEquals(texts.getResetPasswordInvalidTitle(), field(resetView, "invalidTitle", H1.class).getText());
        verify(service).validateToken("used-or-expired");
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clickUsesTheOriginalTokenAndShowsDynamicSuccessOrFailureIncludingExpiryAfterRendering(boolean success) {
        createResetForm();
        when(service.resetPassword("link-token", "Password!")).thenReturn(success);
        field(resetView, "resetButton", Button.class).click();
        verify(service).validateToken("link-token");
        verify(service).resetPassword("link-token", "Password!");
        verifyNoMoreInteractions(service);
        notification(success, success ? texts.getResetPasswordSuccessMessage() : texts.getResetPasswordInvalidText(), Locale.ENGLISH);
        var helper = NotificationHelper.getInstance();
        var visible = ReflectionTestUtils.getField(helper, "currentNotification");
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertSame(visible, ReflectionTestUtils.getField(helper, "currentNotification"));
        notification(success, success ? texts.getResetPasswordSuccessMessage() : texts.getResetPasswordInvalidText(), Locale.GERMAN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"required", "weak", "mismatch"})
    void invalidFormNeverCallsThePasswordChangeService(String kind) {
        createResetForm();
        field(resetView, "confirmPasswordField", PasswordField.class).setValue(switch (kind) {
            case "required" -> "";
            case "weak" -> "weak";
            default -> "DifferentPassword!";
        });
        field(resetView, "resetButton", Button.class).click();
        verify(service).validateToken("link-token");
        verifyNoMoreInteractions(service);
    }

    @Test
    void resetRepositoryFailureIsLoggedAndShowsOnlyDynamicNeutralText() {
        createResetForm();
        var failure = new DataAccessResourceFailureException("secret SQL and password detail");
        when(service.resetPassword("link-token", "Password!")).thenThrow(failure);
        assertLogged(ResetPasswordView.class, failure, () -> field(resetView, "resetButton", Button.class).click());
        notification(false, texts.getResetPasswordTechnicalErrorMessage(), Locale.ENGLISH);
        var visible = ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertSame(visible, ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification"));
        notification(false, texts.getResetPasswordTechnicalErrorMessage(), Locale.GERMAN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "disabled", "queued", "pending", "in-progress", "io", "runtime"})
    void anonymousMailRequestAlwaysShowsTheSameDynamicConfirmation(String outcome) throws IOException {
        forgotView = new ForgotPasswordView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
        field(forgotView, "emailField", EmailField.class).setValue("test@example.com");
        if (outcome.equals("io") || outcome.equals("runtime")) {
            Exception failure = outcome.equals("io") ? new IOException("secret template path") : new DataAccessResourceFailureException("secret SQL");
            when(service.sendPasswordResetEmail("test@example.com")).thenThrow(failure);
            assertLogged(ForgotPasswordView.class, failure, () -> field(forgotView, "sendButton", Button.class).click());
        } else {
            when(service.sendPasswordResetEmail("test@example.com")).thenReturn(!outcome.equals("missing") && !outcome.equals("disabled"));
            field(forgotView, "sendButton", Button.class).click();
        }
        notification(true, texts.getForgotPasswordSuccessMessage(), Locale.ENGLISH);
        var helper = NotificationHelper.getInstance();
        var visible = (com.vaadin.flow.component.notification.Notification) ReflectionTestUtils.getField(helper, "currentNotification");
        assertEquals(-1, visible.getDuration());
        assertTrue(((Span) ReflectionTestUtils.getField(helper, "titleText")).hasClassName("text-success"));
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertSame(visible, ReflectionTestUtils.getField(helper, "currentNotification"));
        notification(true, texts.getForgotPasswordSuccessMessage(), Locale.GERMAN);
        verify(service).sendPasswordResetEmail("test@example.com");
        verifyNoMoreInteractions(service);
    }

    @Test
    void validationFailureShowsTechnicalCardWithoutPasswordFormAndTranslates() {
        var failure = new DataAccessResourceFailureException("secret SQL");
        when(service.validateToken("link-token")).thenThrow(failure);
        resetView = new ResetPasswordView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
        assertLogged(ResetPasswordView.class, failure, () -> resetView.setParameter(null, "link-token"));
        assertNull(ReflectionTestUtils.getField(resetView, "resetButton"));
        assertEquals(texts.getBaseFailedTitle(), field(resetView, "invalidTitle", H1.class).getText());
        assertEquals(texts.getResetPasswordTechnicalErrorMessage(), field(resetView, "invalidText", Span.class).getText());
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertEquals(texts.getBaseFailedTitle(), field(resetView, "invalidTitle", H1.class).getText());
        assertEquals(texts.getResetPasswordTechnicalErrorMessage(), field(resetView, "invalidText", Span.class).getText());
        verify(service).validateToken("link-token");
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-an-email"})
    void anonymousInputValidationDoesNotQueryAccounts(String value) {
        forgotView = new ForgotPasswordView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
        field(forgotView, "emailField", EmailField.class).setValue(value);
        field(forgotView, "sendButton", Button.class).click();
        verifyNoInteractions(service);
    }

    @Test
    void neutralTranslationsAreRealGermanAndEnglishAndContainRetryAdvice() {
        assertTrue(texts.getForgotPasswordSuccessMessage().contains("If an eligible account exists"));
        assertTrue(texts.getResetPasswordTechnicalErrorMessage().contains("try again later"));
        session.setLocale(Locale.GERMAN);
        assertTrue(texts.getForgotPasswordSuccessMessage().contains("Wenn ein geeignetes Konto"));
        assertTrue(texts.getForgotPasswordSuccessMessage().contains("prüfen"));
        assertTrue(texts.getResetPasswordTechnicalErrorMessage().contains("später erneut"));
        assertFalse(texts.getResetPasswordTechnicalErrorMessage().contains("\\u"));
    }

    private void assertLogged(Class<?> source, Exception failure, Runnable action) {
        var logger = (Logger) LoggerFactory.getLogger(source);
        var logs = new ListAppender<ILoggingEvent>();
        logs.start();
        logger.addAppender(logs);
        try {
            assertDoesNotThrow(action::run);
            assertTrue(logs.list.stream().anyMatch(event -> event.getThrowableProxy() != null
                    && event.getThrowableProxy().getMessage().equals(failure.getMessage())));
        } finally {
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
