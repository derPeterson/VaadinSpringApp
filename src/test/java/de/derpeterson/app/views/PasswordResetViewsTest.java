package de.derpeterson.app.views;

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
        notification(success, success ? texts.getResetPasswordSuccessMessage() : texts.getBaseFailedMessage(), Locale.ENGLISH);
        var helper = NotificationHelper.getInstance();
        var visible = ReflectionTestUtils.getField(helper, "currentNotification");
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertSame(visible, ReflectionTestUtils.getField(helper, "currentNotification"));
        notification(success, success ? texts.getResetPasswordSuccessMessage() : texts.getBaseFailedMessage(), Locale.GERMAN);
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
    void resetRepositoryFailureCurrentlyEscapesTheCallback() {
        createResetForm();
        var failure = new DataAccessResourceFailureException("isolated reset failure");
        when(service.resetPassword("link-token", "Password!")).thenThrow(failure);
        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> field(resetView, "resetButton", Button.class).click()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mailRequestMapsServiceResultToDynamicNotification(boolean success) throws IOException {
        forgotView = new ForgotPasswordView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
        field(forgotView, "emailField", EmailField.class).setValue("test@example.com");
        when(service.sendPasswordResetEmail("test@example.com")).thenReturn(success);
        field(forgotView, "sendButton", Button.class).click();
        notification(success, success ? texts.getForgotPasswordSuccessMessage() : texts.getBaseFailedMessage(), Locale.ENGLISH);
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        notification(success, success ? texts.getForgotPasswordSuccessMessage() : texts.getBaseFailedMessage(), Locale.GERMAN);
        verify(service).sendPasswordResetEmail("test@example.com");
        verifyNoMoreInteractions(service);
    }

    @Test
    void mailIoFailureIsHandledAsFailureRatherThanSuccess() throws IOException {
        forgotView = new ForgotPasswordView(texts, service, mock(SecurityService.class), mock(HttpServletRequest.class));
        field(forgotView, "emailField", EmailField.class).setValue("test@example.com");
        when(service.sendPasswordResetEmail("test@example.com")).thenThrow(new IOException("isolated template failure"));
        assertDoesNotThrow(() -> field(forgotView, "sendButton", Button.class).click());
        notification(false, texts.getBaseFailedMessage(), Locale.ENGLISH);
    }
}
