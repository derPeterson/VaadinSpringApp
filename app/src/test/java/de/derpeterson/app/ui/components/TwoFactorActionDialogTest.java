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
  private UI ui;
  private VaadinSession session;

  @AfterEach
  void cleanup() {
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
      String language, String reason) {
    MessageProperties messageProperties = useLanguage(language);
    TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    RuntimeException failure =
        switch (reason) {
          case "invalid" -> new BadCredentialsException("fixture");
          case "locked" -> new LockedException("fixture");
          default -> new IllegalStateException("fixture");
        };
    doThrow(failure)
        .when(twoFactorAuthorization)
        .confirmAction(request, TwoFactorAction.MANAGE_FACTOR, "123456");
    AtomicInteger actions = new AtomicInteger();
    TwoFactorActionDialog dialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, request);
    dialog.open(TwoFactorAction.MANAGE_FACTOR, actions::incrementAndGet);
    TwoFactorCodeInput input =
        (TwoFactorCodeInput) ReflectionTestUtils.getField(dialog, "twoFactorCodeInput");
    OtpField field = (OtpField) ReflectionTestUtils.getField(input, "appCodeField");
    field.setValue("123456");
    ((Button) ReflectionTestUtils.getField(dialog, "confirmIdentityButton")).click();
    assertEquals(0, actions.get());
    assertTrue(dialog.isOpened());
    assertTrue(field.isEmpty());
    assertTrue(field.isInvalid());
    assertTrue(field.getErrorMessage() == null || field.getErrorMessage().isBlank());
    Supplier<?> message =
        (Supplier<?>)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageSupplier");
    assertEquals(
        switch (reason) {
          case "invalid" -> messageProperties.getTwoFactorInvalidCode();
          case "locked" -> messageProperties.getTwoFactorThrottled();
          default -> messageProperties.getTwoFactorFailed();
        },
        message.get());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void accountChangeRequiresClickForEitherCode(boolean useRecovery) {
    MessageProperties messageProperties = useLanguage("de");
    TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    TwoFactorActionDialog dialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, request);
    ui.add(dialog);
    AtomicInteger actions = new AtomicInteger();
    dialog.open(TwoFactorAction.MANAGE_FACTOR, actions::incrementAndGet);
    TwoFactorCodeInput input =
        (TwoFactorCodeInput) ReflectionTestUtils.getField(dialog, "twoFactorCodeInput");
    OtpField appCode = (OtpField) ReflectionTestUtils.getField(input, "appCodeField");
    TextField recovery = (TextField) ReflectionTestUtils.getField(input, "recoveryCodeField");
    assertTrue(appCode.isAutofocus());
    assertEquals(6, appCode.getLength());
    String code = "123456";
    appCode.setValue(code);
    if (useRecovery) {
      Tab recoveryTab = (Tab) ReflectionTestUtils.getField(input, "recoveryTab");
      ((Tabs) recoveryTab.getParent().orElseThrow()).setSelectedTab(recoveryTab);
      assertTrue(appCode.isEmpty());
      code = "ABCD1234-ABCD1234-ABCD1234-ABCD1234";
      recovery.setValue(code);
    } else {
      ComponentUtil.fireEvent(appCode, new OtpCompleteEvent(appCode, true, code));
    }
    verifyNoInteractions(twoFactorAuthorization);
    assertEquals(0, actions.get());
    ((Button) ReflectionTestUtils.getField(dialog, "confirmIdentityButton")).click();
    verify(twoFactorAuthorization).confirmAction(request, TwoFactorAction.MANAGE_FACTOR, code);
    assertEquals(1, actions.get());
    assertFalse(dialog.isOpened());
    assertTrue(appCode.isEmpty());
    assertTrue(recovery.isEmpty());
  }

  private MessageProperties useLanguage(String language) {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    VaadinService service = mock(VaadinService.class);
    DeploymentConfiguration configuration = mock(DeploymentConfiguration.class);
    when(configuration.getUrlSafeSchemes()).thenCallRealMethod();
    when(service.getDeploymentConfiguration()).thenReturn(configuration);
    when(session.getService()).thenReturn(service);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.forLanguageTag(language));
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    return new MessageProperties(new CustomI18NProvider());
  }

  @Test
  void recoveryHelpClosesThePendingActionWithoutConfirmingIt() {
    MessageProperties messageProperties = useLanguage("de");
    TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
    MockHttpServletRequest request = new MockHttpServletRequest();
    TwoFactorActionDialog dialog =
        new TwoFactorActionDialog(messageProperties, twoFactorAuthorization, request);
    ui.add(dialog);
    AtomicInteger actions = new AtomicInteger();
    dialog.open(TwoFactorAction.MANAGE_FACTOR, actions::incrementAndGet);
    TwoFactorCodeInput input =
        (TwoFactorCodeInput) ReflectionTestUtils.getField(dialog, "twoFactorCodeInput");
    ((Button) ReflectionTestUtils.getField(input, "openRecoveryHelpButton")).click();

    assertFalse(dialog.isOpened());
    assertEquals(TwoFactorFlow.ACCOUNT_HELP, TwoFactorFlow.current(request));
    assertEquals(0, actions.get());
    verifyNoInteractions(twoFactorAuthorization);
  }
}
