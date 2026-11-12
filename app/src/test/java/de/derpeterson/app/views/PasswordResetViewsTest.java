package de.derpeterson.app.views;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.PasswordResetService;
import de.derpeterson.app.service.PasswordResetService.LinkState;
import de.derpeterson.app.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/** Real direct caller callbacks and language events; no server, database or SMTP. */
class PasswordResetViewsTest {
  private UI testUi;
  private VaadinSession vaadinSession;
  private PasswordResetService passwordResetService;
  private ResetPasswordView resetView;
  private ForgotPasswordView forgotView;
  private final CustomI18NProvider i18nProvider = new CustomI18NProvider();
  private final MessageProperties messageProperties = new MessageProperties(i18nProvider);

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
    passwordResetService = mock(PasswordResetService.class);
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    resetView = null;
    forgotView = null;
    vaadinSession = null;
    testUi = null;
  }

  private <T> T field(Object view, String name, Class<T> type) {
    return type.cast(ReflectionTestUtils.getField(view, name));
  }

  private void createResetForm() {
    when(passwordResetService.inspectToken("link-token")).thenReturn(LinkState.VALID);
    resetView =
        new ResetPasswordView(
            messageProperties,
            passwordResetService,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    resetView.setParameter(null, "link-token");
    field(resetView, "newPasswordField", PasswordField.class).setValue("Password!");
    field(resetView, "confirmNewPasswordField", PasswordField.class).setValue("Password!");
  }

  private void notification(boolean success, String body, Locale locale) {
    NotificationHelper helper = NotificationHelper.getInstance();
    Span titleSpan = (Span) ReflectionTestUtils.getField(helper, "titleText");
    Html messageHtml = (Html) ReflectionTestUtils.getField(helper, "messageText");
    MessageProperties expectedTexts = MessageProperties.forLocale(i18nProvider, locale);

    assertEquals(
        success ? expectedTexts.getBaseSuccessTitle() : expectedTexts.getBaseFailedTitle(),
        titleSpan.getText());
    assertEquals(body, messageHtml.getElement().getProperty("innerHTML"));
  }

  @Test
  void rejectedTokenShowsInvalidCardWithoutOfferingPasswordChangeAndTranslates() {
    resetView =
        new ResetPasswordView(
            messageProperties,
            passwordResetService,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    when(passwordResetService.inspectToken("used-or-expired")).thenReturn(LinkState.INVALID);
    resetView.setParameter(null, "used-or-expired");

    assertNull(ReflectionTestUtils.getField(resetView, "saveNewPasswordButton"));
    assertEquals(
        messageProperties.getResetPasswordInvalidTitle(),
        field(resetView, "resetLinkProblemTitleH1", H1.class).getText());
    Button request = field(resetView, "requestNewResetLinkButton", Button.class);

    assertEquals(messageProperties.getPublicRequestNewLink(), request.getText());
    assertEquals("mail", request.getPrefixComponent().getElement().getAttribute("data-lucide"));
    assertNull(ReflectionTestUtils.getField(resetView, "returnHomeButton"));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(
        messageProperties.getResetPasswordInvalidTitle(),
        field(resetView, "resetLinkProblemTitleH1", H1.class).getText());
    assertEquals(messageProperties.getPublicRequestNewLink(), request.getText());
    UI navigation = mock(UI.class);
    UI.setCurrent(navigation);
    request.click();

    verify(navigation).navigate(ForgotPasswordView.class);
    UI.setCurrent(testUi);

    verify(passwordResetService).inspectToken("used-or-expired");
    verifyNoMoreInteractions(passwordResetService);
  }

  @Test
  void expiredLinkHasItsOwnTranslatedPageAndNeverOffersAReset() {
    when(passwordResetService.inspectToken("expired")).thenReturn(LinkState.EXPIRED);
    resetView =
        new ResetPasswordView(
            messageProperties,
            passwordResetService,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    resetView.setParameter(null, "expired");

    assertEquals(
        messageProperties.getResetPasswordExpiredTitle(),
        field(resetView, "resetLinkProblemTitleH1", H1.class).getText());
    assertEquals(
        messageProperties.getResetPasswordExpiredText(),
        field(resetView, "resetLinkProblemMessageSpan", Span.class).getText());
    assertNull(ReflectionTestUtils.getField(resetView, "saveNewPasswordButton"));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(
        messageProperties.getResetPasswordExpiredTitle(),
        field(resetView, "resetLinkProblemTitleH1", H1.class).getText());
    assertEquals(
        messageProperties.getResetPasswordExpiredText(),
        field(resetView, "resetLinkProblemMessageSpan", Span.class).getText());
    verify(passwordResetService).inspectToken("expired");
    verifyNoMoreInteractions(passwordResetService);
  }

  @Test
  void resetFormHasClearActionsAndCanReturnToLoginWithoutChangingPassword() {
    createResetForm();
    Button save = field(resetView, "saveNewPasswordButton", Button.class);
    Button login = field(resetView, "goToLoginButton", Button.class);

    assertEquals(
        messageProperties.getPublicResetFormTitle(),
        field(resetView, "resetPasswordTitleH1", H1.class).getText());
    assertEquals(messageProperties.getPublicSavePasswordAction(), save.getText());
    assertEquals("check", save.getPrefixComponent().getElement().getAttribute("data-lucide"));
    assertEquals("log-in", login.getPrefixComponent().getElement().getAttribute("data-lucide"));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(messageProperties.getPublicSavePasswordAction(), save.getText());
    assertEquals(messageProperties.getBaseLoginButton(), login.getText());
    UI navigation = mock(UI.class);
    UI.setCurrent(navigation);
    login.click();

    verify(navigation).navigate(LoginView.class);
    UI.setCurrent(testUi);

    verify(passwordResetService).inspectToken("link-token");
    verifyNoMoreInteractions(passwordResetService);
  }

  @Test
  void expiredActivationPreservesTokenBoundResendAndTranslatedHomeAction() throws IOException {
    VerificationService verification = mock(VerificationService.class);
    when(verification.existsToken("expired-link")).thenReturn(true);
    when(verification.sendVerificationEmailByToken("expired-link")).thenReturn(true);
    VerificationView view =
        new VerificationView(
            messageProperties,
            verification,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    view.setParameter(null, "expired-link");
    Button resend = field(view, "resendVerificationLinkButton", Button.class);
    Button home = field(view, "verificationHomeButton", Button.class);

    assertEquals("mail", resend.getPrefixComponent().getElement().getAttribute("data-lucide"));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(
        messageProperties.getVerificationExpiredTitle(),
        field(view, "expiredLinkH1", H1.class).getText());
    assertEquals(
        messageProperties.getVerificationExpiredText1(),
        field(view, "expiredLinkHintSpan", Span.class).getText());
    assertEquals(messageProperties.getPublicHomeAction(), home.getText());
    assertEquals(messageProperties.getBaseResendButton(), resend.getText());
    resend.click();

    verify(verification).validateToken("expired-link");
    verify(verification).existsToken("expired-link");
    verify(verification).sendVerificationEmailByToken("expired-link");
    verifyNoMoreInteractions(verification);
  }

  @Test
  void invalidActivationOffersTranslatedEmailRequestOnlyAfterValidInput() throws IOException {
    VerificationService verification = mock(VerificationService.class);
    when(verification.sendVerificationEmailByEmail("pending@example.test")).thenReturn(true);
    VerificationView view =
        new VerificationView(
            messageProperties,
            verification,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    view.setParameter(null, "unknown-link");
    Button request = field(view, "requestVerificationLinkButton", Button.class);

    assertEquals(messageProperties.getVerificationRequestActivationLink(), request.getText());
    assertEquals("mail", request.getPrefixComponent().getElement().getAttribute("data-lucide"));
    request.click();

    verify(verification).validateToken("unknown-link");
    verify(verification).existsToken("unknown-link");
    verifyNoMoreInteractions(verification);
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(messageProperties.getVerificationRequestActivationLink(), request.getText());
    assertEquals(
        messageProperties.getVerificationNotFoundTitle(),
        field(view, "unknownLinkH1", H1.class).getText());
    assertEquals(
        messageProperties.getVerificationNotFoundText1(),
        field(view, "invalidLinkExplanationSpan", Span.class).getText());
    assertEquals(
        messageProperties.getVerificationNotFoundText2(),
        field(view, "requestNewLinkHintSpan", Span.class).getText());
    field(view, "verificationEmailField", EmailField.class).setValue("pending@example.test");
    request.click();

    verify(verification).sendVerificationEmailByEmail("pending@example.test");
    verifyNoMoreInteractions(verification);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void declinedVerificationRequestShowsTranslatedWaitWarning(boolean expiredToken) {
    VerificationService verificationService = mock(VerificationService.class);
    when(verificationService.existsToken("pending-link")).thenReturn(expiredToken);
    VerificationView verificationView =
        new VerificationView(
            messageProperties,
            verificationService,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    verificationView.setParameter(null, "pending-link");

    if (expiredToken) {
      field(verificationView, "resendVerificationLinkButton", Button.class).click();
    } else {
      field(verificationView, "verificationEmailField", EmailField.class)
          .setValue("pending@example.test");
      field(verificationView, "requestVerificationLinkButton", Button.class).click();
    }

    NotificationHelper notificationHelper = NotificationHelper.getInstance();
    for (Locale notificationLocale : new Locale[] {Locale.ENGLISH, Locale.GERMAN}) {
      LanguageChangeEvent.fire(testUi, notificationLocale);
      Span titleSpan = (Span) ReflectionTestUtils.getField(notificationHelper, "titleText");
      Html messageHtml = (Html) ReflectionTestUtils.getField(notificationHelper, "messageText");
      assertEquals(messageProperties.getEmailVerificationTitle(), titleSpan.getText());
      assertTrue(titleSpan.hasClassName("text-warning"));
      assertEquals(
          messageProperties.getEmailVerificationWait(),
          messageHtml.getElement().getProperty("innerHTML"));
    }
  }

  @Test
  void activationConfirmationUsesTranslatedReferenceTextsAndLoginNavigation() {
    VerificationService verification = mock(VerificationService.class);
    when(verification.validateToken("verified-link")).thenReturn(true);
    VerificationView view =
        new VerificationView(
            messageProperties,
            verification,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    view.setParameter(null, "verified-link");
    Button login = field(view, "verificationLoginButton", Button.class);

    assertEquals(messageProperties.getBaseLoginButton(), login.getText());
    assertEquals("log-in", login.getPrefixComponent().getElement().getAttribute("data-lucide"));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(
        messageProperties.getVerificationSuccessTitle(),
        field(view, "verificationSuccessH1", H1.class).getText());
    assertEquals(
        messageProperties.getVerificationSuccessText(),
        field(view, "verificationSuccessMessageSpan", Span.class).getText());
    assertEquals(messageProperties.getBaseLoginButton(), login.getText());
    UI navigation = mock(UI.class);
    UI.setCurrent(navigation);
    login.click();

    verify(navigation).navigate(LoginView.class);
    UI.setCurrent(testUi);

    verify(verification).validateToken("verified-link");
    verifyNoMoreInteractions(verification);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void clickUsesTheOriginalTokenAndShowsDynamicSuccessOrFailureIncludingExpiryAfterRendering(
      boolean success) {
    createResetForm();
    when(passwordResetService.resetPassword("link-token", "Password!")).thenReturn(success);
    if (!success) {
      when(passwordResetService.inspectToken("link-token")).thenReturn(LinkState.EXPIRED);
    }
    field(resetView, "saveNewPasswordButton", Button.class).click();

    verify(passwordResetService, times(success ? 1 : 2)).inspectToken("link-token");
    verify(passwordResetService).resetPassword("link-token", "Password!");
    verifyNoMoreInteractions(passwordResetService);
    notification(
        success,
        success
            ? messageProperties.getResetPasswordSuccessMessage()
            : messageProperties.getResetPasswordExpiredText(),
        Locale.ENGLISH);
    NotificationHelper helper = NotificationHelper.getInstance();
    Object visible = ReflectionTestUtils.getField(helper, "currentNotification");
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertSame(visible, ReflectionTestUtils.getField(helper, "currentNotification"));
    notification(
        success,
        success
            ? messageProperties.getResetPasswordSuccessMessage()
            : messageProperties.getResetPasswordExpiredText(),
        Locale.GERMAN);
  }

  @ParameterizedTest
  @ValueSource(strings = {"required", "weak", "mismatch"})
  void invalidFormNeverCallsThePasswordChangeService(String kind) {
    createResetForm();
    field(resetView, "confirmNewPasswordField", PasswordField.class)
        .setValue(
            switch (kind) {
              case "required" -> "";
              case "weak" -> "weak";
              default -> "DifferentPassword!";
            });
    field(resetView, "saveNewPasswordButton", Button.class).click();

    verify(passwordResetService).inspectToken("link-token");
    verifyNoMoreInteractions(passwordResetService);
  }

  @Test
  void resetRepositoryFailureIsLoggedAndShowsOnlyDynamicNeutralText() {
    createResetForm();
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("secret SQL and password detail");
    when(passwordResetService.resetPassword("link-token", "Password!")).thenThrow(failure);
    assertLogged(
        ResetPasswordView.class,
        failure,
        () -> field(resetView, "saveNewPasswordButton", Button.class).click());
    notification(false, messageProperties.getResetPasswordTechnicalErrorMessage(), Locale.ENGLISH);
    Object visible =
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertSame(
        visible,
        ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification"));
    notification(false, messageProperties.getResetPasswordTechnicalErrorMessage(), Locale.GERMAN);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"missing", "disabled", "queued", "pending", "in-progress", "io", "runtime"})
  void anonymousMailRequestAlwaysShowsTheSameDynamicConfirmation(String outcome)
      throws IOException {
    forgotView =
        new ForgotPasswordView(
            messageProperties,
            passwordResetService,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    field(forgotView, "passwordResetEmailField", EmailField.class).setValue("test@example.com");
    if (outcome.equals("io") || outcome.equals("runtime")) {
      Exception failure =
          outcome.equals("io")
              ? new IOException("secret template path")
              : new DataAccessResourceFailureException("secret SQL");
      when(passwordResetService.sendPasswordResetEmail("test@example.com")).thenThrow(failure);
      assertLogged(
          ForgotPasswordView.class,
          failure,
          () -> field(forgotView, "requestPasswordResetButton", Button.class).click());
    } else {
      when(passwordResetService.sendPasswordResetEmail("test@example.com"))
          .thenReturn(!outcome.equals("missing") && !outcome.equals("disabled"));
      field(forgotView, "requestPasswordResetButton", Button.class).click();
    }
    notification(true, messageProperties.getForgotPasswordSuccessMessage(), Locale.ENGLISH);
    NotificationHelper helper = NotificationHelper.getInstance();
    Notification visible =
        (com.vaadin.flow.component.notification.Notification)
            ReflectionTestUtils.getField(helper, "currentNotification");

    assertEquals(-1, visible.getDuration());
    assertTrue(
        ((Span) ReflectionTestUtils.getField(helper, "titleText")).hasClassName("text-success"));
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertSame(visible, ReflectionTestUtils.getField(helper, "currentNotification"));
    notification(true, messageProperties.getForgotPasswordSuccessMessage(), Locale.GERMAN);

    verify(passwordResetService).sendPasswordResetEmail("test@example.com");
    verifyNoMoreInteractions(passwordResetService);
  }

  @Test
  void validationFailureShowsTechnicalCardWithoutPasswordFormAndTranslates() {
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("secret SQL");
    when(passwordResetService.inspectToken("link-token")).thenThrow(failure);
    resetView =
        new ResetPasswordView(
            messageProperties,
            passwordResetService,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    assertLogged(
        ResetPasswordView.class, failure, () -> resetView.setParameter(null, "link-token"));

    assertNull(ReflectionTestUtils.getField(resetView, "saveNewPasswordButton"));
    assertEquals(
        messageProperties.getBaseFailedTitle(),
        field(resetView, "resetLinkProblemTitleH1", H1.class).getText());
    assertEquals(
        messageProperties.getResetPasswordTechnicalErrorMessage(),
        field(resetView, "resetLinkProblemMessageSpan", Span.class).getText());
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(
        messageProperties.getBaseFailedTitle(),
        field(resetView, "resetLinkProblemTitleH1", H1.class).getText());
    assertEquals(
        messageProperties.getResetPasswordTechnicalErrorMessage(),
        field(resetView, "resetLinkProblemMessageSpan", Span.class).getText());
    verify(passwordResetService).inspectToken("link-token");
    verifyNoMoreInteractions(passwordResetService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not-an-email"})
  void anonymousInputValidationDoesNotQueryAccounts(String value) {
    forgotView =
        new ForgotPasswordView(
            messageProperties,
            passwordResetService,
            mock(SecurityService.class),
            mock(HttpServletRequest.class),
            accountConfig());
    field(forgotView, "passwordResetEmailField", EmailField.class).setValue(value);
    field(forgotView, "requestPasswordResetButton", Button.class).click();

    verifyNoInteractions(passwordResetService);
  }

  @Test
  void neutralTranslationsAreRealGermanAndEnglishAndContainRetryAdvice() {
    assertTrue(
        messageProperties
            .getForgotPasswordSuccessMessage()
            .contains("If an eligible account exists"));
    assertTrue(
        messageProperties.getResetPasswordTechnicalErrorMessage().contains("try again later"));
    vaadinSession.setLocale(Locale.GERMAN);

    assertTrue(
        messageProperties.getForgotPasswordSuccessMessage().contains("Wenn ein geeignetes Konto"));
    assertTrue(messageProperties.getForgotPasswordSuccessMessage().contains("prüfen"));
    assertTrue(messageProperties.getResetPasswordTechnicalErrorMessage().contains("später erneut"));
    assertFalse(messageProperties.getResetPasswordTechnicalErrorMessage().contains("\\u"));
  }

  private void assertLogged(Class<?> source, Exception failure, Runnable action) {
    Logger logger = (Logger) LoggerFactory.getLogger(source);
    ListAppender<ILoggingEvent> logs = new ListAppender<ILoggingEvent>();
    logs.start();
    logger.addAppender(logs);
    try {
      assertDoesNotThrow(action::run);
      assertTrue(
          logs.list.stream()
              .anyMatch(event ->
                  event.getThrowableProxy() != null
                      && event
                          .getThrowableProxy()
                          .getMessage()
                          .equals(failure.getClass().getName())));
    } finally {
      logger.detachAppender(logs);
      logs.stop();
    }
  }

  private static ConfigService accountConfig() {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    return configService;
  }
}
