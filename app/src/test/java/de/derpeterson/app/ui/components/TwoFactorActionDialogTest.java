package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.flowingcode.vaadin.addons.otpfield.OtpCompleteEvent;
import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.TwoFactorFlow;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.util.ReflectionTestUtils;

class TwoFactorActionDialogTest {
  private UI testUi;
  private VaadinSession vaadinSession;

  @AfterEach
  void clearTwoFactorDialogEnvironment() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @CsvSource({
    "de,invalid",
    "en,invalid",
    "de,locked",
    "en,locked",
    "de,unavailable",
    "en,unavailable"
  })
  void rejectedCodeShowsTheRightLocalizedToastAndNeverRunsTheAction(
      String languageTag, String rejectionReason) {
    MessageProperties messageProperties = createLocalizedDialogEnvironment(languageTag);
    TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    RuntimeException authorizationFailure =
        switch (rejectionReason) {
          case "invalid" -> new BadCredentialsException("fixture");
          case "locked" -> new LockedException("fixture");
          default -> new IllegalStateException("fixture");
        };
    doThrow(authorizationFailure)
        .when(twoFactorAuthorization)
        .confirmAction(httpRequest, TwoFactorAction.MANAGE_FACTOR, "123456");

    AtomicInteger confirmedActionCounter = new AtomicInteger();
    TwoFactorActionDialog twoFactorActionDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpRequest);
    twoFactorActionDialog.open(
        TwoFactorAction.MANAGE_FACTOR, confirmedActionCounter::incrementAndGet);
    TwoFactorCodeInput twoFactorCodeInput =
        (TwoFactorCodeInput)
            ReflectionTestUtils.getField(twoFactorActionDialog, "twoFactorCodeInput");
    OtpField appCodeOtpField =
        (OtpField) ReflectionTestUtils.getField(twoFactorCodeInput, "appCodeField");

    appCodeOtpField.setValue("123456");
    ((Button) ReflectionTestUtils.getField(twoFactorActionDialog, "confirmIdentityButton")).click();

    assertEquals(0, confirmedActionCounter.get());
    assertTrue(twoFactorActionDialog.isOpened());
    assertTrue(appCodeOtpField.isEmpty());
    assertTrue(appCodeOtpField.isInvalid());
    assertTrue(
        appCodeOtpField.getErrorMessage() == null || appCodeOtpField.getErrorMessage().isBlank());
    Supplier<?> errorMessageSupplier =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");

    assertEquals(
        switch (rejectionReason) {
          case "invalid" -> messageProperties.getTwoFactorInvalidCode();
          case "locked" -> messageProperties.getTwoFactorThrottled();
          default -> messageProperties.getTwoFactorFailed();
        },
        errorMessageSupplier.get());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void accountChangeRequiresClickForEitherCode(boolean useRecoveryCode) {
    MessageProperties messageProperties = createLocalizedDialogEnvironment("de");
    TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    TwoFactorActionDialog twoFactorActionDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, httpRequest);
    testUi.add(twoFactorActionDialog);
    AtomicInteger confirmedActionCounter = new AtomicInteger();
    twoFactorActionDialog.open(
        TwoFactorAction.MANAGE_FACTOR, confirmedActionCounter::incrementAndGet);
    TwoFactorCodeInput twoFactorCodeInput =
        (TwoFactorCodeInput)
            ReflectionTestUtils.getField(twoFactorActionDialog, "twoFactorCodeInput");
    OtpField appCodeOtpField =
        (OtpField) ReflectionTestUtils.getField(twoFactorCodeInput, "appCodeField");
    TextField recoveryCodeTextField =
        (TextField) ReflectionTestUtils.getField(twoFactorCodeInput, "recoveryCodeField");

    assertTrue(appCodeOtpField.isAutofocus());
    assertEquals(6, appCodeOtpField.getLength());

    String verificationCode = "123456";
    appCodeOtpField.setValue(verificationCode);
    if (useRecoveryCode) {
      Tab recoveryTab = (Tab) ReflectionTestUtils.getField(twoFactorCodeInput, "recoveryTab");
      ((Tabs) recoveryTab.getParent().orElseThrow()).setSelectedTab(recoveryTab);

      assertTrue(appCodeOtpField.isEmpty());
      verificationCode = "ABCD1234-ABCD1234-ABCD1234-ABCD1234";
      recoveryCodeTextField.setValue(verificationCode);
    } else {
      ComponentUtil.fireEvent(
          appCodeOtpField, new OtpCompleteEvent(appCodeOtpField, true, verificationCode));
    }

    verifyNoInteractions(twoFactorAuthorization);
    assertEquals(0, confirmedActionCounter.get());

    ((Button) ReflectionTestUtils.getField(twoFactorActionDialog, "confirmIdentityButton")).click();

    verify(twoFactorAuthorization)
        .confirmAction(httpRequest, TwoFactorAction.MANAGE_FACTOR, verificationCode);
    assertEquals(1, confirmedActionCounter.get());
    assertFalse(twoFactorActionDialog.isOpened());
    assertTrue(appCodeOtpField.isEmpty());
    assertTrue(recoveryCodeTextField.isEmpty());
  }

  private MessageProperties createLocalizedDialogEnvironment(String languageTag) {
    testUi = new UI();
    UI.setCurrent(testUi);

    vaadinSession = mock(VaadinSession.class);
    VaadinService vaadinService = mock(VaadinService.class);
    DeploymentConfiguration deploymentConfiguration = mock(DeploymentConfiguration.class);
    when(deploymentConfiguration.getUrlSafeSchemes()).thenCallRealMethod();
    when(vaadinService.getDeploymentConfiguration()).thenReturn(deploymentConfiguration);
    when(vaadinSession.getService()).thenReturn(vaadinService);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenReturn(Locale.forLanguageTag(languageTag));

    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    return new MessageProperties(new CustomI18NProvider());
  }

  @Test
  void recoveryHelpClosesThePendingActionWithoutConfirmingIt() {
    MessageProperties messageProperties = createLocalizedDialogEnvironment("de");
    TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    TwoFactorActionDialog twoFactorActionDialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, mockHttpRequest);
    testUi.add(twoFactorActionDialog);
    AtomicInteger confirmedActionCounter = new AtomicInteger();
    twoFactorActionDialog.open(
        TwoFactorAction.MANAGE_FACTOR, confirmedActionCounter::incrementAndGet);
    TwoFactorCodeInput twoFactorCodeInput =
        (TwoFactorCodeInput)
            ReflectionTestUtils.getField(twoFactorActionDialog, "twoFactorCodeInput");
    ((Button) ReflectionTestUtils.getField(twoFactorCodeInput, "openRecoveryHelpButton")).click();

    assertFalse(twoFactorActionDialog.isOpened());
    assertEquals(TwoFactorFlow.ACCOUNT_HELP, TwoFactorFlow.current(mockHttpRequest));
    assertEquals(0, confirmedActionCounter.get());
    verifyNoInteractions(twoFactorAuthorization);
  }
}
