package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.router.Router;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual server-side Button callback, components and translations; no application/SMTP. */
public class RegistrationViewTest {
    private UI ui;
    private VaadinSession session;
    private RegistrationView view;
    private UserService users;
    private PasswordEncoder encoder;
    private VerificationService verification;
    private final CustomI18NProvider provider = new CustomI18NProvider();

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
        var registry = new ApplicationRouteRegistry(mock(VaadinContext.class)) {};
        registry.setRoute("login", LoginView.class, List.of());
        var vaadin = mock(TestService.class);
        when(vaadin.getRouter()).thenReturn(new Router(registry));
        when(vaadin.getRouteRegistry()).thenReturn(registry);
        when(session.getService()).thenReturn(vaadin);
        VaadinService.setCurrent(vaadin);
        users = mock(UserService.class);
        encoder = mock(PasswordEncoder.class);
        verification = mock(VerificationService.class);
        var roles = mock(RoleService.class);
        when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()));
        when(encoder.encode("Password!")).thenReturn("hash");
        view = new RegistrationView(new MessageProperties(provider), users, roles, encoder, verification,
                mock(SecurityService.class), mock(HttpServletRequest.class));
        field("firstNameField", TextField.class).setValue("Test");
        field("lastNameField", TextField.class).setValue("User");
        field("emailField", EmailField.class).setValue("test@example.com");
        field("confirmEmailField", EmailField.class).setValue("test@example.com");
        field("passwordField", PasswordField.class).setValue("Password!");
        field("confirmPasswordField", PasswordField.class).setValue("Password!");
        field("genderComboBox", ComboBox.class).setValue(Gender.OTHER);
        field("birthDatePicker", DatePicker.class).setValue(LocalDate.of(1990, 1, 1));
        clearInvocations(encoder);
    }

    @AfterEach
    void cleanup() {
        NotificationHelper.getInstance().closeAndClearAllNotifications();
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        VaadinService.setCurrent(null);
        view = null;
        ui = null;
        session = null;
    }

    private <T> T field(String name, Class<T> type) {
        return type.cast(ReflectionTestUtils.getField(view, name));
    }

    private void click() {
        field("registrationButton", Button.class).click();
    }

    public static class TestService extends com.vaadin.flow.server.VaadinServletService {
        @Override
        public com.vaadin.flow.server.RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }
    }

    private void assertNotification(String key, Locale locale) {
        var helper = NotificationHelper.getInstance();
        Span title = (Span) ReflectionTestUtils.getField(helper, "titleText");
        Html body = (Html) ReflectionTestUtils.getField(helper, "messageText");
        assertEquals(provider.getTranslation("base.failed.title", locale), title.getText());
        String expected = provider.getTranslation(key, locale);
        assertNotEquals(key, expected);
        assertEquals(expected, body.getElement().getProperty("innerHTML"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"firstNameField", "lastNameField"})
    void whitespaceNamesAreMarkedBeforeServiceOrEncodingAndHintChangesLanguage(String name) {
        field(name, TextField.class).setValue(" \t ");
        click();
        assertTrue(field(name, TextField.class).isInvalid());
        assertFalse(field(name.equals("firstNameField") ? "lastNameField" : "firstNameField", TextField.class).isInvalid());
        verifyNoInteractions(users, encoder, verification);
        assertNotification("base.validation.required_message", Locale.ENGLISH);
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertNotification("base.validation.required_message", Locale.GERMAN);
    }

    @Test
    void correctingNamesClearsMarkersAndValidSubmissionStillSavesAndSends() throws Exception {
        field("firstNameField", TextField.class).setValue(" ");
        click();
        field("firstNameField", TextField.class).setValue(" Test ");
        when(verification.sendVerificationEmailByUser(any())).thenReturn(true);
        click();
        assertFalse(field("firstNameField", TextField.class).isInvalid());
        verify(users).saveUser(argThat(user -> user.getFirstName().equals(" Test ") && !user.isEnabled() && user.getPassword().equals("hash")));
        verify(verification).sendVerificationEmailByUser(any());
    }

    static DataIntegrityViolationException integrity(String constraint, String state) {
        return new DataIntegrityViolationException("db failure", new ConstraintViolationException("violation",
                new SQLException("violation", state), "insert", constraint));
    }

    @ParameterizedTest
    @ValueSource(strings = {"uk_users_email", "PUBLIC.UK_USERS_EMAIL INDEX PUBLIC.UK_USERS_EMAIL_INDEX_4"})
    void duplicateAfterSuccessfulPreflightMarksBothEmailsAndUsesDynamicTranslation(String constraint) {
        doThrow(integrity(constraint, "23505")).when(users).saveUser(any());
        click();
        assertTrue(field("emailField", EmailField.class).isInvalid());
        assertTrue(field("confirmEmailField", EmailField.class).isInvalid());
        verify(users).saveUser(any());
        verifyNoInteractions(verification);
        assertNotification("base.validation.email_exists_message", Locale.ENGLISH);
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertNotification("base.validation.email_exists_message", Locale.GERMAN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"uk_other", "ck_users_canonical_email", ""})
    void unrelatedIntegrityFailureIsNeverPresentedAsDuplicate(String constraint) {
        var failure = integrity(constraint, "23505");
        doThrow(failure).when(users).saveUser(any());
        assertSame(failure, assertThrows(DataIntegrityViolationException.class, this::click));
        assertFalse(field("emailField", EmailField.class).isInvalid());
        verifyNoInteractions(verification);
    }

    @Test
    void namedConstraintWithOtherSqlStateAndUnexplainedArgumentFailurePropagate() {
        var failure = integrity("uk_users_email", "23503");
        doThrow(failure).when(users).saveUser(any());
        assertSame(failure, assertThrows(DataIntegrityViolationException.class, this::click));
        var argument = new IllegalArgumentException("not a form problem");
        doThrow(argument).when(users).saveUser(any());
        assertSame(argument, assertThrows(IllegalArgumentException.class, this::click));
        verifyNoInteractions(verification);
    }

    @Test
    void serviceArgumentFailureRevalidatesAndMarksTheInvalidFormWithoutSending() {
        doAnswer(call -> {
            field("lastNameField", TextField.class).setValue(" ");
            throw new IllegalArgumentException("Last name must not be blank");
        }).when(users).saveUser(any());
        click();
        assertTrue(field("lastNameField", TextField.class).isInvalid());
        assertNotification("base.validation.required_message", Locale.ENGLISH);
        verifyNoInteractions(verification);
    }
}
