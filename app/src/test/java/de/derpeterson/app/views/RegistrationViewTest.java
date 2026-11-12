package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.HtmlHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.RoleEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.RegistrationService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import de.derpeterson.app.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Actual server-side Button callback, components and translations; no application/SMTP. */
public class RegistrationViewTest {
  private UI testUi;
  private VaadinSession vaadinSession;
  private RegistrationView view;
  private UserService userService;
  private PasswordEncoder passwordEncoder;
  private VerificationService verificationService;
  private RoleService roles;
  private Logger logger;
  private ListAppender<ILoggingEvent> logs;
  private final CustomI18NProvider i18nProvider = new CustomI18NProvider();

  @BeforeEach
  void setup() {
    testUi = new UI();
    vaadinSession = mock(VaadinSession.class);
    AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.ENGLISH);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    when(vaadinSession.hasLock()).thenReturn(true);
    doAnswer(
            call -> {
              localeReference.set(call.getArgument(0));
              return null;
            })
        .when(vaadinSession)
        .setLocale(any());
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    VaadinService.setCurrent(null);
    var registry = new ApplicationRouteRegistry(mock(VaadinContext.class)) {};
    registry.setRoute("login", LoginView.class, List.of());
    TestService vaadin = mock(TestService.class);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(deploymentConfiguration.getUrlSafeSchemes()).thenCallRealMethod();
    when(vaadin.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(vaadin.getRouter()).thenReturn(new Router(registry));
    when(vaadin.getRouteRegistry()).thenReturn(registry);
    when(vaadinSession.getService()).thenReturn(vaadin);
    VaadinService.setCurrent(vaadin);
    userService = mock(UserService.class);
    passwordEncoder = mock(PasswordEncoder.class);
    verificationService = mock(VerificationService.class);
    roles = mock(RoleService.class);
    when(roles.findByName(RoleType.ROLE_USER))
        .thenReturn(Optional.of(RoleEntity.builder().name(RoleType.ROLE_USER).build()));
    when(passwordEncoder.encode("Password!")).thenReturn("hash");
    view =
        new RegistrationView(
            new MessageProperties(i18nProvider),
            userService,
            roles,
            passwordEncoder,
            new RegistrationService(userService, verificationService, accountConfig()),
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig(),
            mock(de.derpeterson.app.security.ExternalLoginFlow.class));
    field("firstNameField", TextField.class).setValue("Test");
    field("lastNameField", TextField.class).setValue("User");
    field("emailField", EmailField.class).setValue("test@example.com");
    field("confirmEmailField", EmailField.class).setValue("test@example.com");
    field("passwordField", PasswordField.class).setValue("Password!");
    field("confirmPasswordField", PasswordField.class).setValue("Password!");
    field("genderComboBox", ComboBox.class).setValue(Gender.OTHER);
    field("birthDatePicker", DatePicker.class).setValue(LocalDate.of(1990, 1, 1));
    clearInvocations(passwordEncoder);
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
    testUi = null;
    vaadinSession = null;
  }

  private <T> T field(String name, Class<T> type) {
    return type.cast(ReflectionTestUtils.getField(view, name));
  }

  private void click() {
    field("registrationButton", Button.class).click();
  }

  @Test
  void registrationAllowsContactAutofillAndDeclaresBothPasswordsAsNew() {
    assertEquals(Autocomplete.EMAIL, field("emailField", EmailField.class).getAutocomplete());
    assertEquals(
        Autocomplete.EMAIL, field("confirmEmailField", EmailField.class).getAutocomplete());
    assertEquals(
        Autocomplete.NEW_PASSWORD, field("passwordField", PasswordField.class).getAutocomplete());
    assertEquals(
        Autocomplete.NEW_PASSWORD,
        field("confirmPasswordField", PasswordField.class).getAutocomplete());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void birthDateExampleFollowsLanguageChangesAndPreservesTheSelectedDate(boolean german) {
    Locale initial = german ? Locale.GERMAN : Locale.ENGLISH;
    Locale other = german ? Locale.ENGLISH : Locale.GERMAN;
    DatePicker picker = field("birthDatePicker", DatePicker.class);
    LanguageChangeEvent.fire(testUi, initial);
    String initialExample =
        MessageProperties.forLocale(i18nProvider, initial).getRegistrationBirthDatePlaceholder();

    assertEquals(initialExample, picker.getPlaceholder());
    LanguageChangeEvent.fire(testUi, other);
    String translatedExample =
        MessageProperties.forLocale(i18nProvider, other).getRegistrationBirthDatePlaceholder();

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
      LanguageChangeEvent.fire(testUi, locale);
      MessageProperties expected = MessageProperties.forLocale(i18nProvider, locale);

      assertEquals(
          expected.getAccountFirstNamePlaceholder(),
          field("firstNameField", TextField.class).getPlaceholder());
      assertEquals(
          expected.getAccountLastNamePlaceholder(),
          field("lastNameField", TextField.class).getPlaceholder());
      assertEquals(
          expected.getAccountEmailPlaceholder(),
          field("emailField", EmailField.class).getPlaceholder());
      assertEquals(
          expected.getAccountConfirmEmailPlaceholder(),
          field("confirmEmailField", EmailField.class).getPlaceholder());
      assertEquals(
          expected.getAccountPasswordPlaceholder(),
          field("passwordField", PasswordField.class).getPlaceholder());
      assertEquals(
          expected.getAccountConfirmPasswordPlaceholder(),
          field("confirmPasswordField", PasswordField.class).getPlaceholder());
      assertEquals(
          expected.getAccountGenderPlaceholder(),
          field("genderComboBox", ComboBox.class).getPlaceholder());
      assertEquals("Test", field("firstNameField", TextField.class).getValue());
      assertEquals("test@example.com", field("emailField", EmailField.class).getValue());
      assertEquals("Password!", field("passwordField", PasswordField.class).getValue());
      assertEquals(Gender.OTHER, field("genderComboBox", ComboBox.class).getValue());
    }
    verifyNoInteractions(userService, verificationService);
  }

  public static class TestService extends VaadinServletService {
    @Override
    public RouteRegistry getRouteRegistry() {
      return super.getRouteRegistry();
    }
  }

  private void assertNotification(
      Function<MessageProperties, String> messageGetter, Locale locale) {
    NotificationHelper helper = NotificationHelper.getInstance();
    Span titleSpan = (Span) ReflectionTestUtils.getField(helper, "titleText");
    Html body = (Html) ReflectionTestUtils.getField(helper, "messageText");
    MessageProperties expectedTexts = MessageProperties.forLocale(i18nProvider, locale);

    assertEquals(expectedTexts.getBaseFailedTitle(), titleSpan.getText());
    String expected = messageGetter.apply(expectedTexts);
    Locale other = locale.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN;

    assertNotEquals(
        expected, messageGetter.apply(MessageProperties.forLocale(i18nProvider, other)));
    assertEquals(expected, body.getElement().getProperty("innerHTML"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingUserRoleAbortsBeforeWritesAndVerificationAndTranslatesTheVisibleHint(boolean german) {
    Locale initial = german ? Locale.GERMAN : Locale.ENGLISH;
    Locale other = german ? Locale.ENGLISH : Locale.GERMAN;
    LanguageChangeEvent.fire(testUi, initial);
    when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.empty());

    click();

    verify(userService).findByEmail("test@example.com");
    verifyNoMoreInteractions(userService);
    verify(roles).findByName(RoleType.ROLE_USER);
    verifyNoMoreInteractions(roles);
    verifyNoInteractions(passwordEncoder, verificationService);
    Function<MessageProperties, String> messageGetter =
        MessageProperties::getRegistrationUnavailableMessage;
    assertNotification(messageGetter, initial);
    NotificationHelper helper = NotificationHelper.getInstance();
    Notification notification =
        (Notification) ReflectionTestUtils.getField(helper, "currentNotification");

    assertTrue(notification.isOpened());
    assertEquals(-1, notification.getDuration());
    String initialText = messageGetter.apply(MessageProperties.forLocale(i18nProvider, initial));

    assertFalse(initialText.contains("ROLE_USER"));
    assertFalse(initialText.contains("repository"));
    assertNotEquals(
        initialText, messageGetter.apply(MessageProperties.forLocale(i18nProvider, other)));

    LanguageChangeEvent.fire(testUi, other);

    assertSame(notification, ReflectionTestUtils.getField(helper, "currentNotification"));
    assertTrue(notification.isOpened());
    assertNotification(messageGetter, other);
    LanguageChangeEvent.fire(testUi, initial);
    assertNotification(messageGetter, initial);

    verifyNoMoreInteractions(userService, roles);
    verifyNoInteractions(passwordEncoder, verificationService);

    List<ILoggingEvent> failureLogs =
        logs.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();

    assertEquals(1, failureLogs.size());
    String technicalMessage = failureLogs.getFirst().getFormattedMessage();

    assertTrue(technicalMessage.contains("ROLE_USER"));
    assertTrue(technicalMessage.contains("missing"));
    assertFalse(technicalMessage.contains("test@example.com"));
    assertFalse(technicalMessage.contains("Password!"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void newRegistrationExplicitlyKeepsTheSelectedUiLanguage(String language) throws Exception {
    Locale selected = Locale.forLanguageTag(language);
    vaadinSession.setLocale(selected);
    when(verificationService.sendVerificationEmailByUser(any())).thenReturn(true);
    click();

    verify(userService).saveUser(argThat(user -> selected.equals(user.getPreferredLocale())));
    verify(verificationService)
        .sendVerificationEmailByUser(argThat(user -> selected.equals(user.getPreferredLocale())));
  }

  @Test
  void availableUserRoleStillSavesBeforeVerificationAndShowsDynamicSuccess() throws Exception {
    RoleEntity role = RoleEntity.builder().id(7L).name(RoleType.ROLE_USER).build();
    when(roles.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(role));
    doAnswer(
            call -> {
              UserEntity user = call.getArgument(0);
              user.setId(42L);
              return null;
            })
        .when(userService)
        .saveUser(any());
    when(verificationService.sendVerificationEmailByUser(any())).thenReturn(true);

    click();

    ArgumentCaptor<UserEntity> captured = ArgumentCaptor.forClass(UserEntity.class);
    InOrder order = inOrder(roles, userService, passwordEncoder, verificationService);
    order.verify(userService).findByEmail("test@example.com");
    order.verify(roles).findByName(RoleType.ROLE_USER);
    order.verify(passwordEncoder).encode("Password!");
    order.verify(userService).saveUser(captured.capture());
    UserEntity user = captured.getValue();
    order.verify(verificationService).sendVerificationEmailByUser(same(user));
    order.verifyNoMoreInteractions();

    assertEquals(42L, user.getId());
    assertEquals("hash", user.getPassword());
    assertEquals(
        de.derpeterson.app.model.enums.RegistrationChannel.PORTAL, user.getRegistrationChannel());
    assertFalse(user.isEnabled());
    assertEquals("test@example.com", user.getEmail());
    assertEquals(1, user.getRoleEntities().size());
    assertSame(role, user.getRoleEntities().iterator().next());
    assertTrue(logs.list.stream().noneMatch(event -> event.getLevel() == Level.ERROR));

    MessageProperties messageProperties = new MessageProperties(i18nProvider);
    NotificationHelper helper = NotificationHelper.getInstance();
    Object notification = ReflectionTestUtils.getField(helper, "currentNotification");

    assertEquals(
        messageProperties.getBaseSuccessTitle(),
        ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
    assertEquals(
        new Html(
                HtmlHelper.ensureParagraphTags(
                    messageProperties.getRegistrationSuccessMessage(user.getEmail())))
            .getElement()
            .getProperty("innerHTML"),
        ((Html) ReflectionTestUtils.getField(helper, "messageText"))
            .getElement()
            .getProperty("innerHTML"));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertSame(notification, ReflectionTestUtils.getField(helper, "currentNotification"));
    assertEquals(
        messageProperties.getBaseSuccessTitle(),
        ((Span) ReflectionTestUtils.getField(helper, "titleText")).getText());
    assertEquals(
        new Html(
                HtmlHelper.ensureParagraphTags(
                    messageProperties.getRegistrationSuccessMessage(user.getEmail())))
            .getElement()
            .getProperty("innerHTML"),
        ((Html) ReflectionTestUtils.getField(helper, "messageText"))
            .getElement()
            .getProperty("innerHTML"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"firstNameField", "lastNameField"})
  void whitespaceNamesAreMarkedBeforeServiceOrEncodingAndHintChangesLanguage(String name) {
    field(name, TextField.class).setValue(" \t ");
    click();

    assertTrue(field(name, TextField.class).isInvalid());
    assertFalse(
        field(name.equals("firstNameField") ? "lastNameField" : "firstNameField", TextField.class)
            .isInvalid());
    verifyNoInteractions(userService, passwordEncoder, verificationService);
    assertNotification(MessageProperties::getBaseValidationRequiredMessage, Locale.ENGLISH);
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);
    assertNotification(MessageProperties::getBaseValidationRequiredMessage, Locale.GERMAN);
  }

  @Test
  void correctingNamesClearsMarkersAndValidSubmissionStillSavesAndSends() throws Exception {
    field("firstNameField", TextField.class).setValue(" ");
    click();
    field("firstNameField", TextField.class).setValue(" Test ");
    when(verificationService.sendVerificationEmailByUser(any())).thenReturn(true);
    click();

    assertFalse(field("firstNameField", TextField.class).isInvalid());
    verify(userService)
        .saveUser(
            argThat(user ->
                user.getFirstName().equals(" Test ")
                    && !user.isEnabled()
                    && user.getPassword().equals("hash")));
    verify(verificationService).sendVerificationEmailByUser(any());
  }

  static DataIntegrityViolationException integrity(String constraint, String state) {
    return new DataIntegrityViolationException(
        "db failure",
        new ConstraintViolationException(
            "violation", new SQLException("violation", state), "insert", constraint));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"uk_users_email", "PUBLIC.UK_USERS_EMAIL INDEX PUBLIC.UK_USERS_EMAIL_INDEX_4"})
  void duplicateAfterSuccessfulPreflightMarksBothEmailsAndUsesDynamicTranslation(
      String constraint) {
    doThrow(integrity(constraint, "23505")).when(userService).saveUser(any());
    click();

    assertTrue(field("emailField", EmailField.class).isInvalid());
    assertTrue(field("confirmEmailField", EmailField.class).isInvalid());
    verify(userService).saveUser(any());
    verifyNoInteractions(verificationService);
    assertNotification(MessageProperties::getBaseValidationEmailExistsMessage, Locale.ENGLISH);
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);
    assertNotification(MessageProperties::getBaseValidationEmailExistsMessage, Locale.GERMAN);
  }

  @ParameterizedTest
  @ValueSource(strings = {"uk_other", "ck_users_canonical_email", ""})
  void unrelatedIntegrityFailureIsNeverPresentedAsDuplicate(String constraint) {
    DataIntegrityViolationException failure = integrity(constraint, "23505");
    doThrow(failure).when(userService).saveUser(any());

    assertSame(failure, assertThrows(DataIntegrityViolationException.class, this::click));
    assertFalse(field("emailField", EmailField.class).isInvalid());
    verifyNoInteractions(verificationService);
  }

  @Test
  void namedConstraintWithOtherSqlStateAndUnexplainedArgumentFailurePropagate() {
    DataIntegrityViolationException failure = integrity("uk_users_email", "23503");
    doThrow(failure).when(userService).saveUser(any());

    assertSame(failure, assertThrows(DataIntegrityViolationException.class, this::click));
    IllegalArgumentException argument = new IllegalArgumentException("not a form problem");
    doThrow(argument).when(userService).saveUser(any());

    assertSame(argument, assertThrows(IllegalArgumentException.class, this::click));
    verifyNoInteractions(verificationService);
  }

  @Test
  void serviceArgumentFailureRevalidatesAndMarksTheInvalidFormWithoutSending() {
    doAnswer(
            call -> {
              field("lastNameField", TextField.class).setValue(" ");
              throw new IllegalArgumentException("Last name must not be blank");
            })
        .when(userService)
        .saveUser(any());
    click();

    assertTrue(field("lastNameField", TextField.class).isInvalid());
    assertNotification(MessageProperties::getBaseValidationRequiredMessage, Locale.ENGLISH);

    verifyNoInteractions(verificationService);
  }

  private static ConfigService accountConfig() {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    return configService;
  }

  @Test
  void longEmailInputIsSafeBeforeTheRegistrationCallbackRuns() {
    EmailField email = field("emailField", EmailField.class);
    EmailField confirmation = field("confirmEmailField", EmailField.class);
    String address = "a@" + "b.".repeat(8192) + "test";

    assertTimeout(
        Duration.ofSeconds(3),
        () -> {
          email.setValue(address);
          confirmation.setValue(address);

          assertFalse(email.isInvalid());
          assertFalse(confirmation.isInvalid());
          email.setValue(address + " x");
          confirmation.setValue(address + " x");

          assertTrue(email.isInvalid());
          assertTrue(confirmation.isInvalid());
        });
    verifyNoInteractions(userService, verificationService);
  }
}
