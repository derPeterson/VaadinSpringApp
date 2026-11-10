package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AdminDurationField;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Real component save action and language refresh, no application or database. */
class AdminTokenDurationValidationTest {
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private AdminConfigurationSection configurationSection;
  private ConfigService configService;
  private MessageProperties messageProperties;
  private UI testUi;
  private VaadinSession vaadinSession;

  @BeforeEach
  void setup() {
    testUi = new UI();
    UI.setCurrent(testUi);
    vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.getLocale()).thenAnswer(ignored -> localeReference.get());
    when(vaadinSession.hasLock()).thenReturn(true);
    VaadinSession.setCurrent(vaadinSession);
    configService = mock(ConfigService.class);
    when(configService.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    messageProperties = new MessageProperties(new CustomI18NProvider());
    configurationSection =
        new AdminConfigurationSection(messageProperties, configService, () -> {});
    configurationSection.showGroup(AdminConfigGroup.TOKENS);
  }

  @AfterEach
  void cleanup() {
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    configurationSection = null;
    testUi = null;
    vaadinSession = null;
  }

  static Stream<Arguments> tokenSettings() {
    return Stream.of(
            ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
            ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION)
        .flatMap(entry ->
            Stream.of(Locale.GERMAN, Locale.ENGLISH)
                .map(language -> Arguments.of(entry, language)));
  }

  @ParameterizedTest
  @MethodSource("tokenSettings")
  void invalidAndTooShortInputsDoNotSaveAnyVisibleSetting(ConfigEntry entry, Locale language) {
    localeReference.set(language);
    configurationSection.refreshTexts();
    Duration minimum = entry.getMinimumTokenDuration();
    AdminDurationField input = field(entry);
    assertEquals(
        "config-label-" + entry.name().toLowerCase(Locale.ROOT),
        input.getElement().getAttribute("aria-labelledby"));
    for (String value :
        new String[] {"", "PT0S", "PT-1S", "PT1S", minimum.minusNanos(1).toString()}) {
      if (value.isBlank()) {
        amount(entry).clear();
      } else {
        input.setStoredValue(value);
      }
      clearInvocations(configService);
      saveButton().click();
      assertTrue(input.isInvalid(), entry + " / " + value);
      assertNotNull(amount(entry).getErrorMessage());
      if (!value.isBlank()) {
        assertEquals(
            messageProperties.getAdminConfigValidationDurationMinimum(minimum.toMinutes()),
            amount(entry).getErrorMessage());
      } else {
        assertEquals(
            messageProperties.getAdminConfigValidationTokenDuration(minimum.toMinutes()),
            amount(entry).getErrorMessage());
      }
      verify(configService, never()).set(any(ConfigEntry.class), anyString());
    }
    // Existing invalid field errors must follow a later language switch.
    localeReference.set(language.equals(Locale.GERMAN) ? Locale.ENGLISH : Locale.GERMAN);
    configurationSection.refreshTexts();
    assertEquals(
        messageProperties.getAdminConfigValidationDurationMinimum(minimum.toMinutes()),
        amount(entry).getErrorMessage());
  }

  @ParameterizedTest
  @MethodSource("tokenSettings")
  void exactMinimumInHumanFormatIsSavedAsIsoDuration(ConfigEntry entry, Locale language) {
    localeReference.set(language);
    configurationSection.refreshTexts();
    Duration minimum = entry.getMinimumTokenDuration();
    field(entry).setStoredValue(minimum.toMinutes() + "m");
    clearInvocations(configService);
    saveButton().click();
    verify(configService).set(entry, minimum.toString());
    assertFalse(field(entry).isInvalid());
    ConfigInfoButton info =
        (ConfigInfoButton)
            ((Map<?, ?>) ReflectionTestUtils.getField(configurationSection, "configInfoButtons"))
                .get(entry);
    assertTrue(
        info.getTooltip().getText().contains(messageProperties.getAdminConfigDurationNumberRule()));
    assertTrue(
        info.getTooltip()
            .getText()
            .contains(
                messageProperties.getAdminConfigValidationDurationMinimum(minimum.toMinutes())));
    assertTrue(
        info.getTooltip().getText().contains(messageProperties.getAdminConfigEntryTooltip(entry)));
    assertFalse(messageProperties.getAdminConfigEntryTooltip(entry).contains("{0}"));
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void queueRetentionRejectsZeroAndNegativeButAcceptsPositiveSeconds(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    configurationSection.refreshTexts();
    configurationSection.showGroup(AdminConfigGroup.QUEUE);
    ConfigEntry entry = ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION;
    for (String invalid : new String[] {"0s", "PT0S", "-1s"}) {
      field(entry).setStoredValue(invalid);
      clearInvocations(configService);
      saveButton().click();
      assertTrue(field(entry).isInvalid());
      assertEquals(
          messageProperties.getAdminConfigValidationPositiveDuration(),
          amount(entry).getErrorMessage());
      verify(configService, never()).set(any(ConfigEntry.class), anyString());
    }
    field(entry).setStoredValue("1s");
    clearInvocations(configService);
    saveButton().click();
    assertFalse(field(entry).isInvalid());
    verify(configService).set(entry, "PT1S");
  }

  private AdminDurationField field(ConfigEntry entry) {
    return (AdminDurationField)
        ((Map<?, ?>) ReflectionTestUtils.getField(configurationSection, "configInputs")).get(entry);
  }

  private BigDecimalField amount(ConfigEntry entry) {
    return (BigDecimalField) ReflectionTestUtils.getField(field(entry), "preciseDurationField");
  }

  private Button saveButton() {
    return (Button) ReflectionTestUtils.getField(configurationSection, "saveConfigButton");
  }
}
