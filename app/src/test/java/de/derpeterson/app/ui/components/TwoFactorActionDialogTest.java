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
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
    MessageProperties texts = useLanguage(language);
    TwoFactorAuthorization authorization = mock(TwoFactorAuthorization.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    RuntimeException failure =
        switch (reason) {
          case "invalid" -> new BadCredentialsException("fixture");
          case "locked" -> new LockedException("fixture");
          default -> new IllegalStateException("fixture");
        };
    doThrow(failure)
        .when(authorization)
        .confirmAction(request, TwoFactorAction.MANAGE_FACTOR, "123456");
    AtomicInteger actions = new AtomicInteger();
    TwoFactorActionDialog dialog = new TwoFactorActionDialog(texts, authorization, request);
    dialog.open(TwoFactorAction.MANAGE_FACTOR, actions::incrementAndGet);
    TwoFactorCodeInput input = (TwoFactorCodeInput) ReflectionTestUtils.getField(dialog, "code");
    OtpField field = (OtpField) ReflectionTestUtils.getField(input, "appCode");
    field.setValue("123456");
    ((Button) ReflectionTestUtils.getField(dialog, "confirm")).click();
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
          case "invalid" -> texts.getTwoFactorInvalidCode();
          case "locked" -> texts.getTwoFactorThrottled();
          default -> texts.getTwoFactorFailed();
        },
        message.get());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void accountChangeRequiresClickForEitherCode(boolean useRecovery) {
    MessageProperties texts = useLanguage("de");
    TwoFactorAuthorization authorization = mock(TwoFactorAuthorization.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    TwoFactorActionDialog dialog = new TwoFactorActionDialog(texts, authorization, request);
    ui.add(dialog);
    AtomicInteger actions = new AtomicInteger();
    dialog.open(TwoFactorAction.MANAGE_FACTOR, actions::incrementAndGet);
    TwoFactorCodeInput input = (TwoFactorCodeInput) ReflectionTestUtils.getField(dialog, "code");
    OtpField appCode = (OtpField) ReflectionTestUtils.getField(input, "appCode");
    TextField recovery = (TextField) ReflectionTestUtils.getField(input, "recoveryCode");
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
    verifyNoInteractions(authorization);
    assertEquals(0, actions.get());
    ((Button) ReflectionTestUtils.getField(dialog, "confirm")).click();
    verify(authorization).confirmAction(request, TwoFactorAction.MANAGE_FACTOR, code);
    assertEquals(1, actions.get());
    assertFalse(dialog.isOpened());
    assertTrue(appCode.isEmpty());
    assertTrue(recovery.isEmpty());
  }

  private MessageProperties useLanguage(String language) {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.forLanguageTag(language));
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    return new MessageProperties(new CustomI18NProvider());
  }
}
