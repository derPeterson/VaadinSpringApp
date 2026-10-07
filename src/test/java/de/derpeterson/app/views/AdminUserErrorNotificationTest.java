package de.derpeterson.app.views;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.*;
import de.derpeterson.app.model.*;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.*;
import de.derpeterson.app.ui.base.TrackedUserAppLayout;
import de.derpeterson.app.views.admin.AdminUserManagementSection;
import de.derpeterson.app.websocket.UserStatusBroadcaster;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real dialog/button callbacks and language events; no application, DB or SMTP. */
class AdminUserErrorNotificationTest {
    private UI ui;
    private VaadinSession session;
    private TrackedUserAppLayout layout;
    private AdminUserManagementSection section;
    private UserService users;
    private SecurityService security;
    private RoleService roles;
    private final CustomI18NProvider provider = new CustomI18NProvider();
    private final MessageProperties texts = new MessageProperties(provider);
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

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
        ui.getInternals().setSession(session);
        users = mock(UserService.class);
        security = mock(SecurityService.class);
        roles = mock(RoleService.class);
        when(roles.findByName(any())).thenAnswer(call -> Optional.of(RoleEntity.builder().name(call.getArgument(0)).build()));
        when(users.canDeleteUser(any())).thenReturn(true);
        layout = new TrackedUserAppLayout(texts, security, users, new UserStatusBroadcaster(), mock(HttpServletRequest.class)) {};
        section = new AdminUserManagementSection(texts, users, roles, security);
        // Same section refresh path used by AdminView.onTrackedUserLanguageChanged.
        ComponentUtil.addListener(ui, LanguageChangeEvent.class, event -> section.refreshTexts());
        logger = (Logger) LoggerFactory.getLogger(AdminUserManagementSection.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void cleanup() {
        logger.detachAppender(logs);
        logs.stop();
        descendants(ui).filter(Dialog.class::isInstance).map(Dialog.class::cast).toList().forEach(Dialog::close);
        NotificationHelper.getInstance().closeAndClearAllNotifications();
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        section = null;
        layout = null;
        ui = null;
        session = null;
    }

    private static Stream<Component> descendants(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(AdminUserErrorNotificationTest::descendants));
    }

    private UserEntity user(boolean create) {
        return UserEntity.builder().id(create ? null : 2L).version(create ? null : 3L)
                .firstName("Test").lastName("User").email("test@example.com").password("hash")
                .birthDate(LocalDate.of(1990, 1, 1)).gender(Gender.OTHER).enabled(true)
                .roleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_ADMIN).build())).build();
    }

    private Dialog dialog() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return descendants(ui).filter(Dialog.class::isInstance).map(Dialog.class::cast).filter(Dialog::isOpened).findFirst().orElseThrow();
    }

    private void submit(String operation, UserEntity user) {
        ReflectionTestUtils.invokeMethod(section, operation.equals("delete") ? "deleteUser" : "openDialog", user);
        Dialog dialog = dialog();
        if (operation.equals("create")) {
            descendants(dialog).filter(PasswordField.class::isInstance).map(PasswordField.class::cast).forEach(field -> field.setValue("Password!"));
        }
        String label = operation.equals("delete") ? texts.getAdminUsersDelete() : texts.getAdminUsersSave();
        descendants(dialog).filter(Button.class::isInstance).map(Button.class::cast).filter(button -> button.getText().equals(label)).findFirst().orElseThrow().click();
    }

    static Stream<Arguments> failures() {
        return Stream.of(false, true).flatMap(german -> Stream.of("create", "edit", "delete").flatMap(operation ->
                Stream.of("version", "email", "admin", "argument", "unknown-argument", "unknown", "integrity", "null-message").map(kind -> Arguments.of(german, operation, kind))));
    }

    private RuntimeException failure(String kind) {
        return switch (kind) {
            case "version" -> new OptimisticLockingFailureException("secret SELECT password FROM users");
            case "email" -> new DataIntegrityViolationException("secret SQL", new ConstraintViolationException("secret SQL",
                    new SQLException("secret SQL", "23505"), "INSERT secret SQL", "uk_users_email"));
            case "admin" -> new IllegalStateException("Der letzte aktive Administrator muss erhalten bleiben.");
            case "argument" -> new IllegalArgumentException("Invalid password");
            case "unknown-argument" -> new IllegalArgumentException("secret configuration argument");
            case "integrity" -> new DataIntegrityViolationException("secret FK SQL", new ConstraintViolationException("secret FK SQL",
                    new SQLException("secret FK SQL", "23503"), "DELETE secret SQL", "unknown_fk"));
            case "null-message" -> new RuntimeException();
            default -> new IllegalStateException("secret SQL configuration failure");
        };
    }

    @ParameterizedTest
    @MethodSource("failures")
    void mutationCallbacksMapErrorsLogDetailsAndTranslateTheSameVisibleNotification(boolean german, String operation, String kind) {
        LanguageChangeEvent.fire(ui, german ? Locale.GERMAN : Locale.ENGLISH);
        RuntimeException failure = failure(kind);
        if (operation.equals("delete")) doThrow(failure).when(users).deleteUser(any());
        else if (operation.equals("create")) doThrow(failure).when(users).saveUser(any());
        else doThrow(failure).when(users).updateAdminUser(any(), any(), any());
        submit(operation, user(operation.equals("create")));
        String expected = switch (kind) {
            case "version" -> texts.getBaseUserUpdateConflict();
            case "email" -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteFailed() : texts.getBaseValidationEmailExistsMessage();
            case "admin" -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteLastAdmin() : texts.getAdminUsersErrorLastActiveAdmin();
            case "argument" -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteFailed() : texts.getAdminUsersValidationCheckFields();
            default -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteFailed() : texts.getAdminUsersErrorSaveFailed();
        };
        Object notification = ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
        assertNotification(expected);
        assertTrue(logs.list.stream().anyMatch(event -> event.getThrowableProxy() != null && Objects.equals(event.getThrowableProxy().getMessage(), failure.getMessage())));
        LanguageChangeEvent.fire(ui, german ? Locale.ENGLISH : Locale.GERMAN);
        assertSame(notification, ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification"));
        String translated = switch (kind) {
            case "version" -> texts.getBaseUserUpdateConflict();
            case "email" -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteFailed() : texts.getBaseValidationEmailExistsMessage();
            case "admin" -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteLastAdmin() : texts.getAdminUsersErrorLastActiveAdmin();
            case "argument" -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteFailed() : texts.getAdminUsersValidationCheckFields();
            default -> operation.equals("delete") ? texts.getAdminUsersErrorDeleteFailed() : texts.getAdminUsersErrorSaveFailed();
        };
        assertNotEquals(expected, translated);
        assertNotification(translated);
        if (operation.equals("delete")) verify(users).deleteUser(any());
        else if (operation.equals("create")) verify(users).saveUser(any());
        else verify(users).updateAdminUser(any(), eq(3L), any());
    }

    private void assertNotification(String expected) {
        var helper = NotificationHelper.getInstance();
        assertEquals(texts.getBaseFailedTitle(), ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
        String actual = ((Html) ReflectionTestUtils.getField(helper, "messageText")).getElement().getProperty("innerHTML");
        assertEquals(expected, actual);
        assertFalse(actual.contains("secret"));
        assertFalse(actual.contains("SELECT"));
        assertFalse(actual.contains("SQLException"));
    }

    static Stream<Arguments> guards() {
        return Stream.of(false, true).flatMap(german -> Stream.of("own-delete", "last-delete", "role", "disable", "remove-admin", "last-save").map(guard -> Arguments.of(german, guard)));
    }

    @ParameterizedTest
    @MethodSource("guards")
    void preflightHintsAreDynamicInBothDirections(boolean german, String guard) {
        LanguageChangeEvent.fire(ui, german ? Locale.GERMAN : Locale.ENGLISH);
        var user = user(false);
        if (guard.equals("own-delete") || guard.equals("disable") || guard.equals("remove-admin")) when(security.getCurrentUser()).thenReturn(Optional.of(user));
        if (guard.equals("last-delete")) when(users.canDeleteUser(any())).thenReturn(false);
        if (guard.equals("role")) when(roles.findByName(any())).thenReturn(Optional.empty());
        if (guard.equals("disable")) user.setEnabled(false);
        if (guard.equals("remove-admin")) user.setRoleEntities(List.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()));
        if (guard.equals("last-save")) when(users.wouldRemoveLastEnabledAdmin(any(), anyBoolean(), any())).thenReturn(true);
        if (guard.endsWith("delete")) ReflectionTestUtils.invokeMethod(section, "deleteUser", user);
        else submit("edit", user);
        String expected = guardText(guard);
        assertNotification(expected);
        LanguageChangeEvent.fire(ui, german ? Locale.ENGLISH : Locale.GERMAN);
        assertNotEquals(expected, guardText(guard));
        assertNotification(guardText(guard));
        verify(users, never()).updateAdminUser(any(), any(), any());
        verify(users, never()).deleteUser(any());
    }

    private String guardText(String guard) {
        return switch (guard) {
            case "own-delete" -> texts.getAdminUsersErrorDeleteOwnUser();
            case "last-delete" -> texts.getAdminUsersErrorDeleteLastAdmin();
            case "role" -> texts.getAdminUsersErrorRoleMissing();
            case "disable" -> texts.getAdminUsersErrorDisableOwnUser();
            case "remove-admin" -> texts.getAdminUsersErrorRemoveOwnAdminRole();
            default -> texts.getAdminUsersErrorLastActiveAdmin();
        };
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void alreadyVisibleFieldErrorsRefreshWithoutWritingAndClosedDialogsReleaseRefreshers(boolean german) {
        LanguageChangeEvent.fire(ui, german ? Locale.GERMAN : Locale.ENGLISH);
        ReflectionTestUtils.invokeMethod(section, "openDialog", user(false));
        Dialog dialog = dialog();
        TextField firstName = descendants(dialog).filter(TextField.class::isInstance).map(TextField.class::cast)
                .filter(field -> field.getLabel().equals(texts.getAdminUsersFieldFirstName())).findFirst().orElseThrow();
        firstName.setValue(" ");
        assertTrue(firstName.isInvalid());
        assertEquals(texts.getBaseValidationRequiredMessage(), firstName.getErrorMessage());
        LanguageChangeEvent.fire(ui, german ? Locale.ENGLISH : Locale.GERMAN);
        assertTrue(firstName.isInvalid());
        assertEquals(texts.getBaseValidationRequiredMessage(), firstName.getErrorMessage());
        verify(users, never()).updateAdminUser(any(), any(), any());
        dialog.close();
        assertTrue(((List<?>) ReflectionTestUtils.getField(section, "dialogErrorRefreshers")).isEmpty());
    }

    static Stream<Arguments> preflightFailures() {
        return Stream.of(false, true).flatMap(german -> Stream.of(false, true).map(deleting -> Arguments.of(german, deleting)));
    }

    @ParameterizedTest
    @MethodSource("preflightFailures")
    void unexpectedPreflightDatabaseFailuresAlsoUseGenericDynamicHints(boolean german, boolean deleting) {
        LanguageChangeEvent.fire(ui, german ? Locale.GERMAN : Locale.ENGLISH);
        var failure = new DataAccessResourceFailureException("secret SQL connection failure");
        if (deleting) {
            when(users.canDeleteUser(any())).thenThrow(failure);
            ReflectionTestUtils.invokeMethod(section, "deleteUser", user(false));
        } else {
            when(users.wouldRemoveLastEnabledAdmin(any(), anyBoolean(), any())).thenThrow(failure);
            submit("edit", user(false));
        }
        assertNotification(deleting ? texts.getAdminUsersErrorDeleteFailed() : texts.getAdminUsersErrorSaveFailed());
        LanguageChangeEvent.fire(ui, german ? Locale.ENGLISH : Locale.GERMAN);
        assertNotification(deleting ? texts.getAdminUsersErrorDeleteFailed() : texts.getAdminUsersErrorSaveFailed());
        assertTrue(logs.list.stream().anyMatch(event -> event.getThrowableProxy() != null && event.getThrowableProxy().getMessage().equals(failure.getMessage())));
    }
}
