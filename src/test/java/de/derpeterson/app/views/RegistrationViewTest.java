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
import com.vaadin.flow.component.notification.Notification;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.HtmlHelper;
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
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
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
    private RoleService roles;
    private Logger logger;
    private ListAppender<ILoggingEvent> logs;
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
        roles = mock(RoleService.class);
        when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()));
        when(encoder.encode("Password!")).thenReturn("hash");
        view = new RegistrationView(new MessageProperties(provider), users, roles, encoder, verification,
                mock(SecurityService.class), mock(HttpServletRequest.class), accountConfig());
        field("firstNameField", TextField.class).setValue("Test");
        field("lastNameField", TextField.class).setValue("User");
        field("emailField", EmailField.class).setValue("test@example.com");
        field("confirmEmailField", EmailField.class).setValue("test@example.com");
        field("passwordField", PasswordField.class).setValue("Password!");
        field("confirmPasswordField", PasswordField.class).setValue("Password!");
        field("genderComboBox", ComboBox.class).setValue(Gender.OTHER);
        field("birthDatePicker", DatePicker.class).setValue(LocalDate.of(1990, 1, 1));
        clearInvocations(encoder);
        logger = (Logger) LoggerFactory.getLogger(RegistrationView.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void cleanup() {
        logger.detachAppender(logs);
        logs.stop();
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void birthDateExampleFollowsLanguageChangesAndPreservesTheSelectedDate(boolean german) {
        Locale initial = german ? Locale.GERMAN : Locale.ENGLISH;
        Locale other = german ? Locale.ENGLISH : Locale.GERMAN;
        DatePicker picker = field("birthDatePicker", DatePicker.class);
        LanguageChangeEvent.fire(ui, initial);
        String initialExample = MessageProperties.forLocale(provider, initial).getRegistrationBirthDatePlaceholder();
        assertEquals(initialExample, picker.getPlaceholder());
        LanguageChangeEvent.fire(ui, other);
        String translatedExample = MessageProperties.forLocale(provider, other).getRegistrationBirthDatePlaceholder();
        assertNotEquals(initialExample, translatedExample);
        assertEquals(translatedExample, picker.getPlaceholder());
        assertEquals(LocalDate.of(1990, 1, 1), picker.getValue());
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cosmosPlaceholdersFollowLanguageChangesWithoutLosingFormValues(boolean german) {
        Locale initial = german ? Locale.GERMAN : Locale.ENGLISH;
        Locale other = german ? Locale.ENGLISH : Locale.GERMAN;
        for (Locale locale : List.of(initial, other, initial)) {
            LanguageChangeEvent.fire(ui, locale);
            MessageProperties expected = MessageProperties.forLocale(provider, locale);
            assertEquals(expected.getAccountFirstNamePlaceholder(), field("firstNameField", TextField.class).getPlaceholder());
            assertEquals(expected.getAccountLastNamePlaceholder(), field("lastNameField", TextField.class).getPlaceholder());
            assertEquals(expected.getAccountEmailPlaceholder(), field("emailField", EmailField.class).getPlaceholder());
            assertEquals(expected.getAccountConfirmEmailPlaceholder(), field("confirmEmailField", EmailField.class).getPlaceholder());
            assertEquals(expected.getAccountPasswordPlaceholder(), field("passwordField", PasswordField.class).getPlaceholder());
            assertEquals(expected.getAccountConfirmPasswordPlaceholder(), field("confirmPasswordField", PasswordField.class).getPlaceholder());
            assertEquals(expected.getAccountGenderPlaceholder(), field("genderComboBox", ComboBox.class).getPlaceholder());
            assertEquals("Test", field("firstNameField", TextField.class).getValue());
            assertEquals("test@example.com", field("emailField", EmailField.class).getValue());
            assertEquals("Password!", field("passwordField", PasswordField.class).getValue());
            assertEquals(Gender.OTHER, field("genderComboBox", ComboBox.class).getValue());
        }
        verifyNoInteractions(users, verification);
    }

    public static class TestService extends com.vaadin.flow.server.VaadinServletService {
        @Override
        public com.vaadin.flow.server.RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }
    }

    private void assertNotification(Function<MessageProperties, String> messageGetter, Locale locale) {
        var helper = NotificationHelper.getInstance();
        Span title = (Span) ReflectionTestUtils.getField(helper, "titleText");
        Html body = (Html) ReflectionTestUtils.getField(helper, "messageText");
        MessageProperties expectedTexts = MessageProperties.forLocale(provider, locale);
        assertEquals(expectedTexts.getBaseFailedTitle(), title.getText());
        String expected = messageGetter.apply(expectedTexts);
        Locale other = locale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN;
        assertNotEquals(expected, messageGetter.apply(MessageProperties.forLocale(provider, other)));
        assertEquals(expected, body.getElement().getProperty("innerHTML"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingUserRoleAbortsBeforeWritesAndVerificationAndTranslatesTheVisibleHint(boolean german) {
        Locale initial = german ? Locale.GERMAN : Locale.ENGLISH;
        Locale other = german ? Locale.ENGLISH : Locale.GERMAN;
        LanguageChangeEvent.fire(ui, initial);
        when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.empty());

        click();

        verify(users).findByEmail("test@example.com");
        verifyNoMoreInteractions(users);
        verify(roles).findByName(RoleType.ROLE_USER);
        verifyNoMoreInteractions(roles);
        verifyNoInteractions(encoder, verification);
        Function<MessageProperties, String> messageGetter = MessageProperties::getRegistrationUnavailableMessage;
        assertNotification(messageGetter, initial);
        var helper = NotificationHelper.getInstance();
        var notification = (Notification) ReflectionTestUtils.getField(helper, "currentNotification");
        assertTrue(notification.isOpened());
        assertEquals(-1, notification.getDuration());
        String initialText = messageGetter.apply(MessageProperties.forLocale(provider, initial));
        assertFalse(initialText.contains("ROLE_USER"));
        assertFalse(initialText.contains("repository"));
        assertNotEquals(initialText, messageGetter.apply(MessageProperties.forLocale(provider, other)));

        LanguageChangeEvent.fire(ui, other);
        assertSame(notification, ReflectionTestUtils.getField(helper, "currentNotification"));
        assertTrue(notification.isOpened());
        assertNotification(messageGetter, other);
        LanguageChangeEvent.fire(ui, initial);
        assertNotification(messageGetter, initial);
        verifyNoMoreInteractions(users, roles);
        verifyNoInteractions(encoder, verification);

        var failureLogs = logs.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
        assertEquals(1, failureLogs.size());
        String technicalMessage = failureLogs.getFirst().getFormattedMessage();
        assertTrue(technicalMessage.contains("ROLE_USER"));
        assertTrue(technicalMessage.contains("missing"));
        assertFalse(technicalMessage.contains("test@example.com"));
        assertFalse(technicalMessage.contains("Password!"));
    }

    @Test
    void availableUserRoleStillSavesBeforeVerificationAndShowsDynamicSuccess() throws Exception {
        var role = RoleEntity.builder().id(7L).name(RoleType.ROLE_USER).build();
        when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(role));
        doAnswer(call -> {
            UserEntity user = call.getArgument(0);
            user.setId(42L);
            return null;
        }).when(users).saveUser(any());
        when(verification.sendVerificationEmailByUser(any())).thenReturn(true);

        click();

        var captured = ArgumentCaptor.forClass(UserEntity.class);
        var order = inOrder(roles, users, encoder, verification);
        order.verify(users).findByEmail("test@example.com");
        order.verify(roles).findByName(RoleType.ROLE_USER);
        order.verify(encoder).encode("Password!");
        order.verify(users).saveUser(captured.capture());
        var user = captured.getValue();
        order.verify(verification).sendVerificationEmailByUser(same(user));
        order.verifyNoMoreInteractions();
        assertEquals(42L, user.getId());
        assertEquals("hash", user.getPassword());
        assertFalse(user.isEnabled());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(1, user.getRoleEntities().size());
        assertSame(role, user.getRoleEntities().iterator().next());
        assertTrue(logs.list.stream().noneMatch(event -> event.getLevel() == Level.ERROR));

        var texts = new MessageProperties(provider);
        var helper = NotificationHelper.getInstance();
        var notification = ReflectionTestUtils.getField(helper, "currentNotification");
        assertEquals(texts.getBaseSuccessTitle(), ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
        assertEquals(new Html(HtmlHelper.ensureParagraphTags(texts.getRegistrationSuccessMessage(user.getEmail())))
                        .getElement().getProperty("innerHTML"),
                ((Html) ReflectionTestUtils.getField(helper, "messageText")).getElement().getProperty("innerHTML"));
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertSame(notification, ReflectionTestUtils.getField(helper, "currentNotification"));
        assertEquals(texts.getBaseSuccessTitle(), ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
        assertEquals(new Html(HtmlHelper.ensureParagraphTags(texts.getRegistrationSuccessMessage(user.getEmail())))
                        .getElement().getProperty("innerHTML"),
                ((Html) ReflectionTestUtils.getField(helper, "messageText")).getElement().getProperty("innerHTML"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"firstNameField", "lastNameField"})
    void whitespaceNamesAreMarkedBeforeServiceOrEncodingAndHintChangesLanguage(String name) {
        field(name, TextField.class).setValue(" \t ");
        click();
        assertTrue(field(name, TextField.class).isInvalid());
        assertFalse(field(name.equals("firstNameField") ? "lastNameField" : "firstNameField", TextField.class).isInvalid());
        verifyNoInteractions(users, encoder, verification);
        assertNotification(MessageProperties::getBaseValidationRequiredMessage, Locale.ENGLISH);
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertNotification(MessageProperties::getBaseValidationRequiredMessage, Locale.GERMAN);
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
        assertNotification(MessageProperties::getBaseValidationEmailExistsMessage, Locale.ENGLISH);
        LanguageChangeEvent.fire(ui, Locale.GERMAN);
        assertNotification(MessageProperties::getBaseValidationEmailExistsMessage, Locale.GERMAN);
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
        assertNotification(MessageProperties::getBaseValidationRequiredMessage, Locale.ENGLISH);
        verifyNoInteractions(verification);
    }
    private static ConfigService accountConfig() {
        ConfigService config = mock(ConfigService.class);
        when(config.getString(any(ConfigEntry.class))).thenAnswer(call ->
                ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
        return config;
    }

}
