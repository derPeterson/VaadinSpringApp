package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.security.authentication.LockedException;
import org.springframework.test.util.ReflectionTestUtils;

class ExternalConnectionsComponentTest {
  private final ExternalLoginFlow externalLoginFlow = mock(ExternalLoginFlow.class);
  private final ExternalIdentityService externalIdentityService =
      mock(ExternalIdentityService.class);
  private final HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);

  // Testkonto, bestätigter Zweck und beobachtete Kontoaktionen
  private final AtomicReference<Purpose> confirmedPurposeReference = new AtomicReference<>();
  private final UserEntity testUserEntity =
      UserEntity.builder().id(1L).email("own@example.test").build();
  private final AtomicInteger settingsActionCounter = new AtomicInteger();

  // UI und Sprachkontext
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);
  private MessageProperties messageProperties;
  private UI testUi;

  @BeforeEach
  void createExternalConnectionsEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    messageProperties = new MessageProperties(new CustomI18NProvider());

    when(externalLoginFlow.owner(mockHttpRequest)).thenReturn(testUserEntity);
    when(externalLoginFlow.factorEnrolled(mockHttpRequest)).thenReturn(true);
    when(externalLoginFlow.canUseProviderProof(any(), any(), any())).thenReturn(true);
    when(externalLoginFlow.providers()).thenReturn(List.of("google", "microsoft"));
    when(externalIdentityService.connected(1L)).thenReturn(List.of("google"));
    when(externalLoginFlow.confirmedPurpose(mockHttpRequest))
        .thenAnswer(mockInvocation -> Optional.ofNullable(confirmedPurposeReference.get()));
  }

  @AfterEach
  void clearExternalConnectionsEnvironment() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  // Anbieterübersicht und verfügbare Nachweise
  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void overviewKeepsLastMethodProtectedAndRequestsOnlyTheChosenAction(String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    ExternalConnectionsComponent externalConnectionsComponent =
        new ExternalConnectionsComponent(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);

    assertEquals(
        0,
        streamComponentTree(externalConnectionsComponent)
            .filter(ComboBox.class::isInstance)
            .count());
    assertFalse(
        ((Button)
                findProviderUiComponentById(
                    externalConnectionsComponent, "external-connect-google"))
            .isEnabled());
    assertFalse(externalConnectionsComponent.getConfirmationDialog().isOpened());
    ((Button)
            findProviderUiComponentById(externalConnectionsComponent, "external-connect-microsoft"))
        .click();
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        externalConnectionsComponent.getConfirmationDialog();

    assertTrue(externalIdentityConfirmationDialog.isOpened());
    assertFalse(
        findProviderUiComponentById(externalIdentityConfirmationDialog, "external-confirm-password")
            .isVisible());
    assertFalse(findLocalConfirmationButton(externalIdentityConfirmationDialog).isEnabled());
    assertTrue(
        findProviderUiComponentById(externalIdentityConfirmationDialog, "external-confirm-google")
            .isVisible());
    assertEquals(
        0,
        streamComponentTree(externalIdentityConfirmationDialog)
            .filter(candidateComponent ->
                candidateComponent.getId().orElse("").equals("external-confirm-microsoft"))
            .count());
    verify(externalLoginFlow, never())
        .beginLink(mockHttpRequest, "microsoft", activeLocaleReference.get());
  }

  @ParameterizedTest
  @CsvSource({"false, de", "true, de", "false, en", "true, en"})
  void disabledProvidersKeepConnectionsAndRequireAnExistingConfirmationMethod(
      boolean hasPortalPassword, String languageTag) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    testUserEntity.setPassword(hasPortalPassword ? "encoded" : null);
    when(externalLoginFlow.providers()).thenReturn(List.of());
    ExternalConnectionsComponent externalConnectionsComponent =
        new ExternalConnectionsComponent(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    externalConnectionsComponent.configureAccountActions(
        settingsActionCounter::incrementAndGet, settingsActionCounter::incrementAndGet, () -> {});

    assertEquals(
        1,
        streamComponentTree(externalConnectionsComponent)
            .filter(
                candidateComponent -> candidateComponent.hasClassName("external-connection-row"))
            .count());
    assertEquals(
        hasPortalPassword,
        ((Button)
                findProviderUiComponentById(
                    externalConnectionsComponent, "external-connect-google"))
            .isEnabled());
    assertTrue(
        streamComponentTree(externalConnectionsComponent)
            .filter(Span.class::isInstance)
            .map(Span.class::cast)
            .anyMatch(candidateSpan ->
                candidateSpan
                    .getText()
                    .equals(messageProperties.getExternalProviderUnavailable())));

    externalConnectionsComponent.requestPasswordChange();
    externalConnectionsComponent.requestEmailChange();

    assertEquals(hasPortalPassword ? 2 : 0, settingsActionCounter.get());
    assertFalse(externalConnectionsComponent.getConfirmationDialog().isOpened());
    if (!hasPortalPassword) {
      Supplier<?> notificationMessageSupplier =
          (Supplier<?>)
              ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

      assertEquals(
          messageProperties.getExternalProofUnavailable(), notificationMessageSupplier.get());
      activeLocaleReference.set(languageTag.equals("de") ? Locale.ENGLISH : Locale.GERMAN);

      assertEquals(
          messageProperties.getExternalProofUnavailable(), notificationMessageSupplier.get());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void missingProofStartsTheSessionLockBeforeTheSetupPageLoads(boolean isPasswordChange) {
    HttpSession httpSession = mock(HttpSession.class);
    when(mockHttpRequest.getSession()).thenReturn(httpSession);
    when(mockHttpRequest.getContextPath()).thenReturn("");
    when(externalLoginFlow.factorEnrolled(mockHttpRequest)).thenReturn(false);
    when(externalLoginFlow.providers()).thenReturn(List.of());
    ExternalConnectionsComponent externalConnectionsComponent =
        new ExternalConnectionsComponent(
            externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
    externalConnectionsComponent.configureAccountActions(
        settingsActionCounter::incrementAndGet, settingsActionCounter::incrementAndGet, () -> {});
    Page browserPage = mock(Page.class);
    UI redirectingUi = mock(UI.class);
    when(redirectingUi.getPage()).thenReturn(browserPage);
    UI.setCurrent(redirectingUi);

    if (isPasswordChange) {
      externalConnectionsComponent.requestPasswordChange();
    } else {
      externalConnectionsComponent.requestEmailChange();
    }

    InOrder sessionLockInOrder = inOrder(httpSession, browserPage);
    sessionLockInOrder
        .verify(httpSession)
        .setAttribute(TwoFactorFlow.SESSION_ATTRIBUTE, TwoFactorFlow.SETUP.name());
    sessionLockInOrder.verify(browserPage).setLocation(TwoFactorFlow.SETUP.location());

    assertEquals(0, settingsActionCounter.get());
    assertFalse(externalConnectionsComponent.getConfirmationDialog().isOpened());
  }

  @Test
  void refreshingConfirmationAfterDeactivationShowsNoPasswordPromptOrProviderAction() {
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.PASSWORD, settingsActionCounter::incrementAndGet);
    when(externalLoginFlow.providers()).thenReturn(List.of());
    externalIdentityConfirmationDialog.refreshTexts();

    assertFalse(
        findProviderUiComponentById(externalIdentityConfirmationDialog, "external-confirm-password")
            .isVisible());
    assertFalse(findLocalConfirmationButton(externalIdentityConfirmationDialog).isEnabled());
    assertEquals(
        0,
        streamComponentTree(externalIdentityConfirmationDialog)
            .filter(
                candidateComponent -> candidateComponent.hasClassName("external-proof-provider"))
            .count());
    assertEquals(
        messageProperties.getExternalProofUnavailable(),
        ((Span)
                ReflectionTestUtils.getField(
                    externalIdentityConfirmationDialog, "identityProofExplanation"))
            .getText());
    assertEquals(0, settingsActionCounter.get());
  }

  @Test
  void providerDisabledAfterOpeningClosesTheStaleConfirmationWithoutStartingAnAction() {
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.PASSWORD, settingsActionCounter::incrementAndGet);
    Button providerProofButton =
        (Button)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-google");
    when(externalLoginFlow.providers()).thenReturn(List.of());
    doThrow(new AccessDeniedException("Provider unavailable"))
        .when(externalLoginFlow)
        .beginProof(mockHttpRequest, "google", Purpose.PASSWORD, Locale.GERMAN);
    providerProofButton.click();

    assertFalse(externalIdentityConfirmationDialog.isOpened());
    assertEquals(0, settingsActionCounter.get());
    Supplier<?> notificationMessageSupplier =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(
        messageProperties.getExternalProviderUnavailable(), notificationMessageSupplier.get());
  }

  // Bestätigung mit dem Portalpasswort
  @Test
  void localProofIsBoundToTheChosenActionAndClosingDiscardsSecrets() {
    testUserEntity.setPassword("encoded");
    confirmedPurposeReference.set(Purpose.EMAIL);
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.LINK_MICROSOFT, settingsActionCounter::incrementAndGet);
    PasswordField portalPasswordField =
        (PasswordField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-password");
    portalPasswordField.setValue("OwnPortal1!");
    doAnswer(
            mockInvocation -> {
              confirmedPurposeReference.set(Purpose.LINK_MICROSOFT);
              return null;
            })
        .when(externalLoginFlow)
        .confirmPassword(mockHttpRequest, "OwnPortal1!", Purpose.LINK_MICROSOFT);
    findLocalConfirmationButton(externalIdentityConfirmationDialog).click();

    verify(externalLoginFlow)
        .confirmPassword(mockHttpRequest, "OwnPortal1!", Purpose.LINK_MICROSOFT);
    assertEquals(1, settingsActionCounter.get());
    assertFalse(externalIdentityConfirmationDialog.isOpened());
    assertTrue(portalPasswordField.isEmpty());
    externalIdentityConfirmationDialog.open(Purpose.EMAIL, settingsActionCounter::incrementAndGet);
    portalPasswordField.setValue("DiscardMe");
    externalIdentityConfirmationDialog.close();

    assertTrue(portalPasswordField.isEmpty());
    assertEquals(1, settingsActionCounter.get());
  }

  @Test
  void emptyProofUsesTheRequiredNotificationWithoutInlineErrorText() {
    testUserEntity.setPassword("encoded");
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.LINK_GOOGLE, settingsActionCounter::incrementAndGet);
    findLocalConfirmationButton(externalIdentityConfirmationDialog).click();
    PasswordField portalPasswordField =
        (PasswordField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-password");

    assertTrue(portalPasswordField.isInvalid());
    assertTrue(
        portalPasswordField.getErrorMessage() == null
            || portalPasswordField.getErrorMessage().isBlank());
    Supplier<?> notificationMessageSupplier =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(
        messageProperties.getBaseValidationRequiredMessage(), notificationMessageSupplier.get());
    assertEquals(0, settingsActionCounter.get());
  }

  @Test
  void failedProofKeepsDialogOpenAndDoesNotRunTheAction() {
    testUserEntity.setPassword("encoded");
    doThrow(new BadCredentialsException("Rejected"))
        .when(externalLoginFlow)
        .confirmPassword(mockHttpRequest, "wrong", Purpose.UNLINK_GOOGLE);
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.UNLINK_GOOGLE, settingsActionCounter::incrementAndGet);
    PasswordField portalPasswordField =
        (PasswordField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-password");
    portalPasswordField.setValue("wrong");
    findLocalConfirmationButton(externalIdentityConfirmationDialog).click();

    assertEquals(0, settingsActionCounter.get());
    assertTrue(externalIdentityConfirmationDialog.isOpened());
    assertTrue(portalPasswordField.isEmpty());
    assertTrue(portalPasswordField.isInvalid());
    assertTrue(
        portalPasswordField.getErrorMessage() == null
            || portalPasswordField.getErrorMessage().isBlank());
    Supplier<?> notificationMessageSupplier =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(
        messageProperties.getAccountSecurityWrongPassword(), notificationMessageSupplier.get());
  }

  @Test
  void languageRefreshPreservesTheOpenPasswordDraft() {
    testUserEntity.setPassword("encoded");
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.PASSWORD, settingsActionCounter::incrementAndGet);
    PasswordField portalPasswordField =
        (PasswordField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-password");
    portalPasswordField.setValue("Draft1!");
    activeLocaleReference.set(Locale.ENGLISH);
    externalIdentityConfirmationDialog.refreshTexts();

    assertEquals("Draft1!", portalPasswordField.getValue());
    assertEquals(
        messageProperties.getExternalContinueAction(),
        findLocalConfirmationButton(externalIdentityConfirmationDialog).getText());
  }

  // Bestätigung mit App- und Ersatzcode
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
      String languageTag, String rejectionReason) {
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    when(externalLoginFlow.requiresFactor(mockHttpRequest, Purpose.PASSWORD)).thenReturn(true);
    when(externalLoginFlow.canUseProviderProof(any(), any(), any())).thenReturn(false);
    RuntimeException authorizationFailure =
        switch (rejectionReason) {
          case "locked" -> new LockedException("fixture");
          case "invalid" -> new BadCredentialsException("fixture");
          default -> new IllegalStateException("fixture service unavailable");
        };
    doThrow(authorizationFailure)
        .when(externalLoginFlow)
        .confirmFactor(mockHttpRequest, "123", Purpose.PASSWORD);
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.PASSWORD, settingsActionCounter::incrementAndGet);
    OtpField appCodeOtpField =
        (OtpField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-factor");
    appCodeOtpField.setValue("123");
    findLocalConfirmationButton(externalIdentityConfirmationDialog).click();

    assertEquals(0, settingsActionCounter.get());
    assertTrue(externalIdentityConfirmationDialog.isOpened());
    assertTrue(appCodeOtpField.isEmpty());
    assertTrue(appCodeOtpField.isInvalid());
    assertTrue(
        appCodeOtpField.getErrorMessage() == null || appCodeOtpField.getErrorMessage().isBlank());
    Supplier<?> notificationMessageSupplier =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(
        switch (rejectionReason) {
          case "locked" -> messageProperties.getTwoFactorThrottled();
          case "invalid" -> messageProperties.getTwoFactorInvalidCode();
          default -> messageProperties.getTwoFactorFailed();
        },
        notificationMessageSupplier.get());
  }

  @ParameterizedTest
  @CsvSource({"LINK_GOOGLE,false", "LINK_GOOGLE,true", "LINK_MICROSOFT,false"})
  void linkingConfirmsACompleteAppCodeOrAnExplicitRecoveryCode(
      Purpose confirmationPurpose, boolean useRecoveryCode) {
    when(externalLoginFlow.requiresFactor(mockHttpRequest, confirmationPurpose)).thenReturn(true);
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    testUi.add(externalIdentityConfirmationDialog);
    externalIdentityConfirmationDialog.open(
        confirmationPurpose, settingsActionCounter::incrementAndGet);

    OtpField appCodeOtpField =
        (OtpField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-factor");
    TextField recoveryCodeTextField =
        (TextField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-recovery-code");

    assertTrue(appCodeOtpField.isAutofocus());
    assertEquals(6, appCodeOtpField.getLength());
    assertTrue(appCodeOtpField.isVisible());
    assertFalse(recoveryCodeTextField.isVisible());

    String verificationCode = "123456";
    appCodeOtpField.setValue(verificationCode);
    if (useRecoveryCode) {
      Tab recoveryTab =
          (Tab)
              findProviderUiComponentById(
                  externalIdentityConfirmationDialog, "external-proof-recovery-code-tab");
      Tabs codeMethodTabs = (Tabs) recoveryTab.getParent().orElseThrow();
      codeMethodTabs.setSelectedTab(recoveryTab);

      assertTrue(appCodeOtpField.isEmpty());
      assertFalse(appCodeOtpField.isVisible());
      assertTrue(recoveryCodeTextField.isVisible());
      verificationCode = "ABCD1234-ABCD1234-ABCD1234-ABCD1234";
      recoveryCodeTextField.setValue(verificationCode);
      ComponentUtil.fireEvent(
          appCodeOtpField, new OtpCompleteEvent(appCodeOtpField, true, "123456"));
    }

    verify(externalLoginFlow, never())
        .confirmFactor(mockHttpRequest, verificationCode, confirmationPurpose);
    assertEquals(0, settingsActionCounter.get());

    if (useRecoveryCode) {
      findLocalConfirmationButton(externalIdentityConfirmationDialog).click();
    } else {
      ComponentUtil.fireEvent(
          appCodeOtpField, new OtpCompleteEvent(appCodeOtpField, true, "123456"));
      ComponentUtil.fireEvent(
          appCodeOtpField, new OtpCompleteEvent(appCodeOtpField, true, "123456"));
    }

    verify(externalLoginFlow).confirmFactor(mockHttpRequest, verificationCode, confirmationPurpose);
    assertEquals(1, settingsActionCounter.get());
    assertFalse(externalIdentityConfirmationDialog.isOpened());
  }

  @ParameterizedTest
  @CsvSource({"UNLINK_GOOGLE", "UNLINK_MICROSOFT"})
  void unlinkingWaitsForExplicitConfirmation(Purpose confirmationPurpose) {
    when(externalLoginFlow.requiresFactor(mockHttpRequest, confirmationPurpose)).thenReturn(true);
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    testUi.add(externalIdentityConfirmationDialog);
    externalIdentityConfirmationDialog.open(
        confirmationPurpose, settingsActionCounter::incrementAndGet);
    OtpField appCodeOtpField =
        (OtpField)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-factor");
    appCodeOtpField.setValue("123456");
    ComponentUtil.fireEvent(appCodeOtpField, new OtpCompleteEvent(appCodeOtpField, true, "123456"));

    verify(externalLoginFlow, never())
        .confirmFactor(mockHttpRequest, "123456", confirmationPurpose);
    assertEquals(0, settingsActionCounter.get());
    findLocalConfirmationButton(externalIdentityConfirmationDialog).click();

    verify(externalLoginFlow).confirmFactor(mockHttpRequest, "123456", confirmationPurpose);
    assertEquals(1, settingsActionCounter.get());
  }

  // Gültigkeit und Rückkehr des Anbieternachweises
  @Test
  void expiredProviderProofCannotExecuteAPasswordlessAction() {
    confirmedPurposeReference.set(Purpose.UNLINK_GOOGLE);
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.UNLINK_GOOGLE, settingsActionCounter::incrementAndGet);
    Button confirmIdentityButton =
        (Button)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-local");

    assertTrue(confirmIdentityButton.isEnabled());
    confirmedPurposeReference.set(null);
    confirmIdentityButton.click();

    assertEquals(0, settingsActionCounter.get());
    assertTrue(externalIdentityConfirmationDialog.isOpened());
    assertFalse(confirmIdentityButton.isEnabled());
  }

  @Test
  void providerReturnResumesOnlyOnceAndRequiresTheMatchingProof() {
    stubProviderReturnSessionAttributes();

    when(externalLoginFlow.beginProof(
            mockHttpRequest, "google", Purpose.LINK_MICROSOFT, Locale.GERMAN))
        .thenReturn("/own-provider");
    ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog =
        createExternalIdentityConfirmationDialog();
    externalIdentityConfirmationDialog.open(
        Purpose.LINK_MICROSOFT, settingsActionCounter::incrementAndGet);
    ((Button)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-google"))
        .click();
    confirmedPurposeReference.set(Purpose.LINK_MICROSOFT);

    assertEquals(
        Optional.of(Purpose.LINK_MICROSOFT),
        externalIdentityConfirmationDialog.consumeProviderReturn());
    assertTrue(externalIdentityConfirmationDialog.consumeProviderReturn().isEmpty());
    ((Button)
            findProviderUiComponentById(
                externalIdentityConfirmationDialog, "external-confirm-google"))
        .click();
    confirmedPurposeReference.set(Purpose.EMAIL);

    assertTrue(externalIdentityConfirmationDialog.consumeProviderReturn().isEmpty());
    assertEquals(0, settingsActionCounter.get());
  }

  private ExternalIdentityConfirmationDialog createExternalIdentityConfirmationDialog() {
    return new ExternalIdentityConfirmationDialog(
        externalLoginFlow, externalIdentityService, mockHttpRequest, messageProperties);
  }

  private void stubProviderReturnSessionAttributes() {
    HttpSession httpSession = mock(HttpSession.class);
    Map<String, Object> sessionAttributes = new HashMap<>();
    when(mockHttpRequest.getSession(false)).thenReturn(httpSession);
    when(httpSession.getAttribute(anyString()))
        .thenAnswer(mockInvocation -> sessionAttributes.get(mockInvocation.getArgument(0)));
    doAnswer(
            mockInvocation -> {
              sessionAttributes.put(mockInvocation.getArgument(0), mockInvocation.getArgument(1));
              return null;
            })
        .when(httpSession)
        .setAttribute(anyString(), any());
    doAnswer(
            mockInvocation -> {
              sessionAttributes.remove(mockInvocation.getArgument(0));
              return null;
            })
        .when(httpSession)
        .removeAttribute(anyString());
  }

  private Button findLocalConfirmationButton(
      ExternalIdentityConfirmationDialog externalIdentityConfirmationDialog) {
    return (Button)
        findProviderUiComponentById(externalIdentityConfirmationDialog, "external-confirm-local");
  }

  private Component findProviderUiComponentById(Component rootComponent, String componentId) {
    return streamComponentTree(rootComponent)
        .filter(candidateComponent -> candidateComponent.getId().orElse("").equals(componentId))
        .findFirst()
        .orElseThrow();
  }

  private Stream<Component> streamComponentTree(Component rootComponent) {
    Stream<Component> childComponents =
        rootComponent.getChildren().flatMap(this::streamComponentTree);
    if (rootComponent instanceof Dialog confirmationDialog) {
      childComponents =
          Stream.concat(
              childComponents,
              Stream.concat(
                      confirmationDialog.getHeader().getElement().getChildren(),
                      confirmationDialog.getFooter().getElement().getChildren())
                  .flatMap(childElement -> childElement.getComponent().stream())
                  .flatMap(this::streamComponentTree));
    }

    return Stream.concat(Stream.of(rootComponent), childComponents);
  }
}
