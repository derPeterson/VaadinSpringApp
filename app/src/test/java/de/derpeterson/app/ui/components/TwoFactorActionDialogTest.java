package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenReturn(Locale.forLanguageTag(language));
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    MessageProperties texts = new MessageProperties(new CustomI18NProvider());
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
        .confirmAction(request, TwoFactorAction.MANAGE_FACTOR, "invalid");
    AtomicInteger actions = new AtomicInteger();
    TwoFactorActionDialog dialog = new TwoFactorActionDialog(texts, authorization, request);
    dialog.open(TwoFactorAction.MANAGE_FACTOR, actions::incrementAndGet);
    TextField field = (TextField) ReflectionTestUtils.getField(dialog, "code");
    field.setValue("invalid");
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
}
