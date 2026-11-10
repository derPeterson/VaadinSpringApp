package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowingcode.vaadin.addons.otpfield.OtpCompleteEvent;
import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.ExternalLoginFlow.Purpose;
import de.derpeterson.app.security.TwoFactorFlow;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

class ExternalConnectionsComponentTest {
  private final ExternalLoginFlow externalLoginFlow = mock(ExternalLoginFlow.class);
  private final ExternalIdentityService externalIdentityService =
      mock(ExternalIdentityService.class);
  private final HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
  private final AtomicReference<Purpose> confirmedPurpose = new AtomicReference<>();
  private final UserEntity testAccount =
      UserEntity.builder().id(1L).email("own@example.test").build();
  private final AtomicInteger settingsActionCount = new AtomicInteger();
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private MessageProperties messageProperties;
  private UI ui;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    when(externalLoginFlow.owner(mockHttpRequest)).thenReturn(testAccount);
    when(externalLoginFlow.factorEnrolled(mockHttpRequest)).thenReturn(true);
    when(externalLoginFlow.canUseProviderProof(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    when(externalLoginFlow.providers()).thenReturn(List.of("google", "microsoft"));
    when(externalIdentityService.connected(1L)).thenReturn(List.of("google"));
    when(externalLoginFlow.confirmedPurpose(mockHttpRequest))
        .thenAnswer(call -> Optional.ofNullable(confirmedPurpose.get()));
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void overviewKeepsLastMethodProtectedAndRequestsOnlyTheChosenAction(String language) {
    locale.set(Locale.forLanguageTag(language));
    ExternalConnectionsComponent component =
        new ExternalConnectionsComponent(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    assertEquals(0, tree(component).filter(ComboBox.class::isInstance).count());
    assertFalse(((Button) find(component, "external-connect-google")).isEnabled());
    assertFalse(component.getConfirmationDialog().isOpened());
    ((Button) find(component, "external-connect-microsoft")).click();
    ExternalIdentityConfirmationDialog dialog = component.getConfirmationDialog();
    assertTrue(dialog.isOpened());
    assertFalse(find(dialog, "external-confirm-password").isVisible());
    assertFalse(((Button) find(dialog, "external-confirm-local")).isEnabled());
    assertTrue(find(dialog, "external-confirm-google").isVisible());
    assertEquals(
        0,
        tree(dialog)
            .filter(c -> c.getId().orElse("").equals("external-confirm-microsoft"))
            .count());
    verify(externalLoginFlow, never()).beginLink(mockHttpRequest, "microsoft", locale.get());
  }

  @ParameterizedTest
  @CsvSource({"false, de", "true, de", "false, en", "true, en"})
  void disabledProvidersKeepConnectionsAndRequireAnExistingConfirmationMethod(
      boolean hasPassword, String language) {
    locale.set(Locale.forLanguageTag(language));
    testAccount.setPassword(hasPassword ? "encoded" : null);
    when(externalLoginFlow.providers()).thenReturn(List.of());
    ExternalConnectionsComponent component =
        new ExternalConnectionsComponent(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    component.configureAccountActions(
        settingsActionCount::incrementAndGet, settingsActionCount::incrementAndGet, () -> {});
    assertEquals(1, tree(component).filter(c -> c.hasClassName("external-connection-row")).count());
    assertEquals(hasPassword, ((Button) find(component, "external-connect-google")).isEnabled());
    assertTrue(
        tree(component)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(
                span -> span.getText().equals(messageProperties.getExternalProviderUnavailable())));
    component.requestPasswordChange();
    component.requestEmailChange();
    assertEquals(hasPassword ? 2 : 0, settingsActionCount.get());
    assertFalse(component.getConfirmationDialog().isOpened());
    if (!hasPassword) {
      Supplier<?> message =
          (Supplier<?>)
              ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
      assertEquals(messageProperties.getExternalProofUnavailable(), message.get());
      locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
      assertEquals(messageProperties.getExternalProofUnavailable(), message.get());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void missingProofStartsTheSessionLockBeforeTheSetupPageLoads(boolean passwordChange) {
    HttpSession session = mock(HttpSession.class);
    when(mockHttpRequest.getSession()).thenReturn(session);
    when(mockHttpRequest.getContextPath()).thenReturn("");
    when(externalLoginFlow.factorEnrolled(mockHttpRequest)).thenReturn(false);
    when(externalLoginFlow.providers()).thenReturn(List.of());
    ExternalConnectionsComponent component =
        new ExternalConnectionsComponent(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    component.configureAccountActions(
        settingsActionCount::incrementAndGet, settingsActionCount::incrementAndGet, () -> {});
    Page page = mock(Page.class);
    UI redirectingUi = mock(UI.class);
    when(redirectingUi.getPage()).thenReturn(page);
    UI.setCurrent(redirectingUi);

    if (passwordChange) {
      component.requestPasswordChange();
    } else {
      component.requestEmailChange();
    }

    InOrder effects = inOrder(session, page);
    effects
        .verify(session)
        .setAttribute(TwoFactorFlow.SESSION_ATTRIBUTE, TwoFactorFlow.SETUP.name());
    effects.verify(page).setLocation(TwoFactorFlow.SETUP.location());
    assertEquals(0, settingsActionCount.get());
    assertFalse(component.getConfirmationDialog().isOpened());
  }

  @Test
  void refreshingConfirmationAfterDeactivationShowsNoPasswordPromptOrProviderAction() {
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.PASSWORD, settingsActionCount::incrementAndGet);
    when(externalLoginFlow.providers()).thenReturn(List.of());
    dialog.refreshTexts();
    assertFalse(find(dialog, "external-confirm-password").isVisible());
    assertFalse(((Button) find(dialog, "external-confirm-local")).isEnabled());
    assertEquals(0, tree(dialog).filter(c -> c.hasClassName("external-proof-provider")).count());
    assertEquals(
        messageProperties.getExternalProofUnavailable(),
        ((Span) ReflectionTestUtils.getField(dialog, "identityProofExplanation")).getText());
    assertEquals(0, settingsActionCount.get());
  }

  @Test
  void providerDisabledAfterOpeningClosesTheStaleConfirmationWithoutStartingAnAction() {
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.PASSWORD, settingsActionCount::incrementAndGet);
    Button provider = (Button) find(dialog, "external-confirm-google");
    when(externalLoginFlow.providers()).thenReturn(List.of());
    doThrow(new AccessDeniedException("Provider unavailable"))
        .when(externalLoginFlow)
        .beginProof(mockHttpRequest, "google", Purpose.PASSWORD, Locale.GERMAN);
    provider.click();
    assertFalse(dialog.isOpened());
    assertEquals(0, settingsActionCount.get());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(messageProperties.getExternalProviderUnavailable(), message.get());
  }

  @Test
  void localProofIsBoundToTheChosenActionAndClosingDiscardsSecrets() {
    testAccount.setPassword("encoded");
    confirmedPurpose.set(Purpose.EMAIL);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.LINK_MICROSOFT, settingsActionCount::incrementAndGet);
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    password.setValue("OwnPortal1!");
    doAnswer(
            call -> {
              confirmedPurpose.set(Purpose.LINK_MICROSOFT);
              return null;
            })
        .when(externalLoginFlow)
        .confirmPassword(mockHttpRequest, "OwnPortal1!", Purpose.LINK_MICROSOFT);
    ((Button) find(dialog, "external-confirm-local")).click();
    verify(externalLoginFlow)
        .confirmPassword(mockHttpRequest, "OwnPortal1!", Purpose.LINK_MICROSOFT);
    assertEquals(1, settingsActionCount.get());
    assertFalse(dialog.isOpened());
    assertTrue(password.isEmpty());
    dialog.open(Purpose.EMAIL, settingsActionCount::incrementAndGet);
    password.setValue("DiscardMe");
    dialog.close();
    assertTrue(password.isEmpty());
    assertEquals(1, settingsActionCount.get());
  }

  @Test
  void emptyProofUsesTheRequiredNotificationWithoutInlineErrorText() {
    testAccount.setPassword("encoded");
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.LINK_GOOGLE, settingsActionCount::incrementAndGet);
    ((Button) find(dialog, "external-confirm-local")).click();
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    assertTrue(password.isInvalid());
    assertTrue(password.getErrorMessage() == null || password.getErrorMessage().isBlank());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(messageProperties.getBaseValidationRequiredMessage(), message.get());
    assertEquals(0, settingsActionCount.get());
  }

  @Test
  void failedProofKeepsDialogOpenAndDoesNotRunTheAction() {
    testAccount.setPassword("encoded");
    doThrow(new BadCredentialsException("Rejected"))
        .when(externalLoginFlow)
        .confirmPassword(mockHttpRequest, "wrong", Purpose.UNLINK_GOOGLE);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.UNLINK_GOOGLE, settingsActionCount::incrementAndGet);
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    password.setValue("wrong");
    ((Button) find(dialog, "external-confirm-local")).click();
    assertEquals(0, settingsActionCount.get());
    assertTrue(dialog.isOpened());
    assertTrue(password.isEmpty());
    assertTrue(password.isInvalid());
    assertTrue(password.getErrorMessage() == null || password.getErrorMessage().isBlank());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(messageProperties.getAccountSecurityWrongPassword(), message.get());
  }

  @ParameterizedTest
  @CsvSource({
    "de, invalid",
    "en, invalid",
    "de, locked",
    "en, locked",
    "de, unavailable",
    "en, unavailable"
  })
  void factorFailureUsesStandardToastAndDistinguishesLockFromInvalidCode(
      String language, String reason) {
    locale.set(Locale.forLanguageTag(language));
    when(externalLoginFlow.requiresFactor(mockHttpRequest, Purpose.PASSWORD)).thenReturn(true);
    when(externalLoginFlow.canUseProviderProof(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);
    RuntimeException failure =
        switch (reason) {
          case "locked" ->
              new org.springframework.security.authentication.LockedException("fixture");
          case "invalid" -> new BadCredentialsException("fixture");
          default -> new IllegalStateException("fixture service unavailable");
        };
    doThrow(failure)
        .when(externalLoginFlow)
        .confirmFactor(mockHttpRequest, "123", Purpose.PASSWORD);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.PASSWORD, settingsActionCount::incrementAndGet);
    OtpField code = (OtpField) find(dialog, "external-confirm-factor");
    code.setValue("123");
    ((Button) find(dialog, "external-confirm-local")).click();
    assertEquals(0, settingsActionCount.get());
    assertTrue(dialog.isOpened());
    assertTrue(code.isEmpty());
    assertTrue(code.isInvalid());
    assertTrue(code.getErrorMessage() == null || code.getErrorMessage().isBlank());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(
        switch (reason) {
          case "locked" -> messageProperties.getTwoFactorThrottled();
          case "invalid" -> messageProperties.getTwoFactorInvalidCode();
          default -> messageProperties.getTwoFactorFailed();
        },
        message.get());
  }

  @ParameterizedTest
  @CsvSource({"LINK_GOOGLE,false", "LINK_GOOGLE,true", "LINK_MICROSOFT,false"})
  void linkingConfirmsACompleteAppCodeOrAnExplicitRecoveryCode(
      Purpose purpose, boolean useRecoveryCode) {
    when(externalLoginFlow.requiresFactor(mockHttpRequest, purpose)).thenReturn(true);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    ui.add(dialog);
    dialog.open(purpose, settingsActionCount::incrementAndGet);

    OtpField appCode = (OtpField) find(dialog, "external-confirm-factor");
    TextField recoveryCode = (TextField) find(dialog, "external-confirm-recovery-code");
    assertTrue(appCode.isAutofocus());
    assertEquals(6, appCode.getLength());
    assertTrue(appCode.isVisible());
    assertFalse(recoveryCode.isVisible());
    String value = "123456";
    appCode.setValue(value);
    if (useRecoveryCode) {
      Tab recoveryTab = (Tab) find(dialog, "external-proof-recovery-code-tab");
      Tabs methods = (Tabs) recoveryTab.getParent().orElseThrow();
      methods.setSelectedTab(recoveryTab);
      assertTrue(appCode.isEmpty());
      assertFalse(appCode.isVisible());
      assertTrue(recoveryCode.isVisible());
      value = "ABCD1234-ABCD1234-ABCD1234-ABCD1234";
      recoveryCode.setValue(value);
      ComponentUtil.fireEvent(appCode, new OtpCompleteEvent(appCode, true, "123456"));
    }
    verify(externalLoginFlow, never()).confirmFactor(mockHttpRequest, value, purpose);
    assertEquals(0, settingsActionCount.get());
    if (useRecoveryCode) {
      ((Button) find(dialog, "external-confirm-local")).click();
    } else {
      ComponentUtil.fireEvent(appCode, new OtpCompleteEvent(appCode, true, "123456"));
      ComponentUtil.fireEvent(appCode, new OtpCompleteEvent(appCode, true, "123456"));
    }
    verify(externalLoginFlow).confirmFactor(mockHttpRequest, value, purpose);
    assertEquals(1, settingsActionCount.get());
    assertFalse(dialog.isOpened());
  }

  @ParameterizedTest
  @CsvSource({"UNLINK_GOOGLE", "UNLINK_MICROSOFT"})
  void unlinkingWaitsForExplicitConfirmation(Purpose purpose) {
    when(externalLoginFlow.requiresFactor(mockHttpRequest, purpose)).thenReturn(true);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    ui.add(dialog);
    dialog.open(purpose, settingsActionCount::incrementAndGet);
    OtpField appCode = (OtpField) find(dialog, "external-confirm-factor");
    appCode.setValue("123456");
    ComponentUtil.fireEvent(appCode, new OtpCompleteEvent(appCode, true, "123456"));
    verify(externalLoginFlow, never()).confirmFactor(mockHttpRequest, "123456", purpose);
    assertEquals(0, settingsActionCount.get());
    ((Button) find(dialog, "external-confirm-local")).click();
    verify(externalLoginFlow).confirmFactor(mockHttpRequest, "123456", purpose);
    assertEquals(1, settingsActionCount.get());
  }

  @Test
  void expiredProviderProofCannotExecuteAPasswordlessAction() {
    confirmedPurpose.set(Purpose.UNLINK_GOOGLE);
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.UNLINK_GOOGLE, settingsActionCount::incrementAndGet);
    Button confirm = (Button) find(dialog, "external-confirm-local");
    assertTrue(confirm.isEnabled());
    confirmedPurpose.set(null);
    confirm.click();
    assertEquals(0, settingsActionCount.get());
    assertTrue(dialog.isOpened());
    assertFalse(confirm.isEnabled());
  }

  @Test
  void languageRefreshPreservesTheOpenPasswordDraft() {
    testAccount.setPassword("encoded");
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.PASSWORD, settingsActionCount::incrementAndGet);
    PasswordField password = (PasswordField) find(dialog, "external-confirm-password");
    password.setValue("Draft1!");
    locale.set(Locale.ENGLISH);
    dialog.refreshTexts();
    assertEquals("Draft1!", password.getValue());
    assertEquals(
        messageProperties.getExternalContinueAction(),
        ((Button) find(dialog, "external-confirm-local")).getText());
  }

  @Test
  void providerReturnResumesOnlyOnceAndRequiresTheMatchingProof() {
    HttpSession session = mock(HttpSession.class);
    Map<String, Object> attributes = new HashMap<>();
    when(mockHttpRequest.getSession(false)).thenReturn(session);
    when(session.getAttribute(org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(call -> attributes.get(call.getArgument(0)));
    doAnswer(
            call -> {
              attributes.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(session)
        .setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    doAnswer(
            call -> {
              attributes.remove(call.getArgument(0));
              return null;
            })
        .when(session)
        .removeAttribute(org.mockito.ArgumentMatchers.anyString());
    when(externalLoginFlow.beginProof(
            mockHttpRequest, "google", Purpose.LINK_MICROSOFT, Locale.GERMAN))
        .thenReturn("/own-provider");
    ExternalIdentityConfirmationDialog dialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    dialog.open(Purpose.LINK_MICROSOFT, settingsActionCount::incrementAndGet);
    ((Button) find(dialog, "external-confirm-google")).click();
    confirmedPurpose.set(Purpose.LINK_MICROSOFT);
    assertEquals(Optional.of(Purpose.LINK_MICROSOFT), dialog.consumeProviderReturn());
    assertTrue(dialog.consumeProviderReturn().isEmpty());
    ((Button) find(dialog, "external-confirm-google")).click();
    confirmedPurpose.set(Purpose.EMAIL);
    assertTrue(dialog.consumeProviderReturn().isEmpty());
    assertEquals(0, settingsActionCount.get());
  }

  private Component find(Component root, String id) {
    return tree(root).filter(c -> c.getId().orElse("").equals(id)).findFirst().orElseThrow();
  }

  private Stream<Component> tree(Component component) {
    Stream<Component> children = component.getChildren().flatMap(this::tree);
    if (component instanceof Dialog dialog) {
      children =
          Stream.concat(
              children,
              Stream.concat(
                      dialog.getHeader().getElement().getChildren(),
                      dialog.getFooter().getElement().getChildren())
                  .flatMap(element -> element.getComponent().stream())
                  .flatMap(this::tree));
    }
    return Stream.concat(Stream.of(component), children);
  }
}
