package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorPolicies;
import de.derpeterson.app.security.mfa.TwoFactorPolicy;
import de.derpeterson.app.ui.components.CosmosDurationCustomField;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Echte Konfigurationskomponenten mit isolierter UI und gemockten Diensten, ohne Browser. */
class AdminTwoFactorSectionTest {
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private final TwoFactorPolicies twoFactorPolicies = mock(TwoFactorPolicies.class);
  private final TwoFactorAuthorization twoFactorAuthorization = mock(TwoFactorAuthorization.class);
  private final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
  private MessageProperties messageProperties;

  private AdminTwoFactorSection openSection(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(_ -> localeReference.get());
    UI testUi = new UI();
    testUi.getInternals().setSession(vaadinSession);
    UI.setCurrent(testUi);
    VaadinSession.setCurrent(vaadinSession);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    when(twoFactorPolicies.load())
        .thenReturn(new TwoFactorPolicies.Snapshot(1, TwoFactorPolicy.defaults()));
    AdminTwoFactorSection twoFactorSection =
        new AdminTwoFactorSection(
            messageProperties, twoFactorPolicies, twoFactorAuthorization, httpServletRequest);
    twoFactorSection.activate();
    return twoFactorSection;
  }

  @AfterEach
  void closeUi() {
    if (UI.getCurrent() != null) {
      NotificationHelper.getInstance().closeAndClearAllNotifications();
    }
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @ParameterizedTest
  @CsvSource({"de,0", "en,0", "de,9000", "en,9000"})
  void invalidInactivityIsMarkedOnlyOnSaveAndShowsTheRangeInTheNotification(
      String language, int hours) {
    AdminTwoFactorSection twoFactorSection = openSection(language);
    Switch inactivityEnabledSwitch =
        (Switch) ReflectionTestUtils.getField(twoFactorSection, "reconfirmAfterInactivitySwitch");
    inactivityEnabledSwitch.setValue(true);
    CosmosDurationCustomField durationField =
        (CosmosDurationCustomField)
            ReflectionTestUtils.getField(twoFactorSection, "inactivityDurationField");
    Object innerDurationField = ReflectionTestUtils.getField(durationField, "durationField");
    IntegerField durationAmountIntegerField =
        (IntegerField)
            ReflectionTestUtils.getField(innerDurationField, "durationAmountIntegerField");
    durationAmountIntegerField.setValue(hours);
    assertFalse(durationAmountIntegerField.isInvalid());
    assertFalse(durationField.isInvalid());

    ReflectionTestUtils.invokeMethod(twoFactorSection, "requestTwoFactorPolicySave");
    assertTrue(durationField.isInvalid());
    assertTrue(durationAmountIntegerField.isInvalid());
    assertNull(durationField.getErrorMessage());
    verify(twoFactorPolicies, never()).save(any(), anyLong(), any());
    verify(twoFactorAuthorization, never()).requires(any(), any());
    assertRangeNotification();
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    twoFactorSection.refreshTexts();
    NotificationHelper.getInstance().updateText();
    assertRangeNotification();

    durationAmountIntegerField.setValue(1);
    assertFalse(durationField.isInvalid());
    when(twoFactorPolicies.save(eq(httpServletRequest), eq(1L), any()))
        .thenReturn(new TwoFactorPolicies.Snapshot(2, TwoFactorPolicy.defaults()));
    ReflectionTestUtils.invokeMethod(twoFactorSection, "requestTwoFactorPolicySave");
    verify(twoFactorPolicies)
        .save(
            eq(httpServletRequest),
            eq(1L),
            argThat(policy ->
                policy.inactivityEnabled() && policy.inactivity().equals(Duration.ofHours(1))));
  }

  private void assertRangeNotification() {
    assertEquals(
        messageProperties.getTwoFactorInactivityRange(),
        ((Paragraph) ReflectionTestUtils.getField(NotificationHelper.getInstance(), "messageText"))
            .getText());
  }
}
