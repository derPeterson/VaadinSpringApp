package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AdminDurationField;
import de.derpeterson.app.ui.components.AdminSidePanel;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Group identity and language-refresh lifecycle; no application, database or browser. */
class AdminConfigurationGroupTest {
  private final CustomI18NProvider provider = new CustomI18NProvider();
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
  private UI ui;
  private VaadinSession session;
  private AdminConfigurationSection section;
  private ConfigService config;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    when(session.getLocale()).thenAnswer(ignored -> locale.get());
    when(session.hasLock()).thenReturn(true);
    VaadinSession.setCurrent(session);
    config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    section = new AdminConfigurationSection(new MessageProperties(provider), config, () -> {});
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    section = null;
    ui = null;
    session = null;
  }

  @Test
  void notificationGroupContainsTwoPositiveDaySettingsAndSavesThemIndependently() {
    section.showGroup(AdminConfigGroup.NOTIFICATIONS);
    assertEquals(
        List.of(
            ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS,
            ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS),
        visibleEntries());
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    IntegerField inbox =
        (IntegerField)
            ReflectionTestUtils.getField(
                inputs.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS), "amount");
    IntegerField history =
        (IntegerField)
            ReflectionTestUtils.getField(
                inputs.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS), "amount");
    assertEquals(180, inbox.getValue());
    assertEquals(365, history.getValue());
    inbox.setValue(0);
    ReflectionTestUtils.invokeMethod(section, "saveConfigValues");
    assertTrue(inbox.isInvalid());
    verify(config, never()).set(any(), anyString());
    inbox.setValue(30);
    history.setValue(14);
    ReflectionTestUtils.invokeMethod(section, "saveConfigValues");
    verify(config).set(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS, "30");
    verify(config).set(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS, "14");
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void emptyNotificationRetentionStaysEmptyAfterSaveReloadAndLanguageChange(String language) {
    locale.set(Locale.forLanguageTag(language));
    EnumMap<ConfigEntry, String> stored =
        new java.util.EnumMap<ConfigEntry, String>(ConfigEntry.class);
    when(config.getString(any()))
        .thenAnswer(
            call -> {
              ConfigEntry entry = call.getArgument(0);
              return stored.getOrDefault(entry, entry.getDefaultValueString());
            });
    doAnswer(
            call -> {
              stored.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(config)
        .set(any(), anyString());
    section.showGroup(AdminConfigGroup.NOTIFICATIONS);
    section.refreshTexts();
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    IntegerField inbox =
        (IntegerField)
            ReflectionTestUtils.getField(
                inputs.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS), "amount");
    IntegerField history =
        (IntegerField)
            ReflectionTestUtils.getField(
                inputs.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS), "amount");
    inbox.clear();
    history.clear();
    MessageProperties messages = MessageProperties.forLocale(provider, locale.get());
    assertEquals(messages.getAdminConfigUnlimited(), inbox.getPlaceholder());
    assertEquals(messages.getAdminConfigUnlimited(), history.getPlaceholder());
    ReflectionTestUtils.invokeMethod(section, "saveConfigValues");
    assertFalse(inbox.isInvalid());
    assertFalse(history.isInvalid());
    assertEquals("", stored.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS));
    assertEquals("", stored.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS));
    assertNull(inbox.getValue());
    assertNull(history.getValue());
    locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    ReflectionTestUtils.invokeMethod(section, "loadConfigValues");
    assertNull(inbox.getValue());
    assertNull(history.getValue());
    assertEquals(
        MessageProperties.forLocale(provider, locale.get()).getAdminConfigUnlimited(),
        inbox.getPlaceholder());
    Map<?, ?> infos = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInfoButtons");
    ConfigInfoButton info =
        (ConfigInfoButton) infos.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS);
    MessageProperties translated = MessageProperties.forLocale(provider, locale.get());
    assertTrue(info.getTooltip().getText().contains(translated.getAdminConfigRuleEmptyUnlimited()));
    assertFalse(
        info.getTooltip().getText().contains(translated.getAdminConfigRuleEmptyUsesDefault()));
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @Test
  void smtpGroupContainsOnlyTheSupportedTransportSettings() {
    section.showGroup(AdminConfigGroup.SMTP);
    assertEquals(
        List.of(
            ConfigEntry.MAIL_HOST,
            ConfigEntry.MAIL_PORT,
            ConfigEntry.MAIL_USERNAME,
            ConfigEntry.MAIL_PASSWORD,
            ConfigEntry.MAIL_SMTP_AUTH,
            ConfigEntry.MAIL_SMTP_STARTTLS_ENABLE,
            ConfigEntry.MAIL_SMTP_SSL_ENABLE,
            ConfigEntry.MAIL_SMTP_SSL_TRUST,
            ConfigEntry.MAIL_DEBUG),
        visibleEntries());
  }

  @Test
  void typedSelectionKeepsDefaultAndTokenFieldsAndIgnoresNull() {
    assertEquals(AdminConfigGroup.GENERAL, selectedGroup());
    assertEquals(
        List.of(ConfigEntry.SERVICE_NAME, ConfigEntry.COPYRIGHT_TEXT, ConfigEntry.BASE_URL),
        visibleEntries());
    section.showGroup(AdminConfigGroup.TOKENS);
    List<ConfigEntry> tokenEntries =
        List.of(
            ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
            ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION);
    assertEquals(tokenEntries, visibleEntries());
    section.showGroup(null);
    assertEquals(AdminConfigGroup.TOKENS, selectedGroup());
    assertEquals(tokenEntries, visibleEntries());
    section.showGroup(AdminConfigGroup.GENERAL);
    assertEquals(
        List.of(ConfigEntry.SERVICE_NAME, ConfigEntry.COPYRIGHT_TEXT, ConfigEntry.BASE_URL),
        visibleEntries());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void allTimeSpanFieldsPreserveValuesMinimumsAndUnsavedUnitChanges(String language) {
    locale.set(Locale.forLanguageTag(language));
    section.refreshTexts();
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    for (ConfigEntry entry :
        new ConfigEntry[] {
          ConfigEntry.USER_AUTO_ABSENT_TIMEOUT, ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
          ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
              ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
          ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION, ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION
        }) {
      AdminDurationField field = (AdminDurationField) inputs.get(entry);
      field.setStoredValue("PT90.123456789S");
      assertEquals("PT1M30.123456789S", field.getStoredValue());
      assertTrue(field.getEquivalentValue().contains("123456789"));
      field.setStoredValue("PT9223372036854775807.999999999S");
      assertEquals("PT2562047788015215H30M7.999999999S", field.getStoredValue());
      field.setStoredValue("PT2H");
      @SuppressWarnings("unchecked")
      Select<AdminDurationField.Unit> units =
          (Select<AdminDurationField.Unit>) ReflectionTestUtils.getField(field, "unit");
      units.setValue(AdminDurationField.Unit.MINUTES);
      assertEquals("PT2M", field.getStoredValue());
      BigDecimalField amount =
          (BigDecimalField) ReflectionTestUtils.getField(field, "preciseAmount");
      amount.setValue(new BigDecimal("0.5"));
      assertEquals("PT30S", field.getStoredValue());
      String error =
          ReflectionTestUtils.invokeMethod(section, "validateValue", entry, field.getStoredValue());
      if (entry.getMinimumTokenDuration() != null) {
        assertTrue(error != null);
      } else {
        assertNull(error);
      }
      amount.clear();
      assertEquals("", ReflectionTestUtils.invokeMethod(section, "extractInputValue", entry));
      assertTrue(ReflectionTestUtils.invokeMethod(section, "validateValue", entry, "") != null);
    }
    verify(config, never()).set(any(), anyString());
  }

  @Test
  void timeSpanSavingUsesExistingIsoFormatAndRejectsInvalidPrecision() {
    section.showGroup(AdminConfigGroup.TOKENS);
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    AdminDurationField field =
        (AdminDurationField) inputs.get(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION);
    BigDecimalField amount = (BigDecimalField) ReflectionTestUtils.getField(field, "preciseAmount");
    field.setStoredValue("PT1S");
    amount.setValue(new BigDecimal("0.0000000001"));
    assertEquals("invalid", field.getStoredValue());
    ReflectionTestUtils.invokeMethod(section, "saveConfigValues");
    assertTrue(field.isInvalid());
    verify(config, never()).set(any(), anyString());
    field.setStoredValue("PT90S");
    ReflectionTestUtils.invokeMethod(section, "saveConfigValues");
    verify(config).set(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION, "PT1M30S");
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @Test
  void durationConvertsExactlyAndRejectsOverflowWithoutSaving() {
    section.showGroup(AdminConfigGroup.SECURITY);
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    AdminDurationField field = (AdminDurationField) inputs.get(ConfigEntry.REMEMBER_ME_DURATION);
    IntegerField amount = (IntegerField) ReflectionTestUtils.getField(field, "amount");
    @SuppressWarnings("unchecked")
    Select<AdminDurationField.Unit> unit =
        (Select<AdminDurationField.Unit>) ReflectionTestUtils.getField(field, "unit");
    assertEquals(14, amount.getValue());
    assertEquals(AdminDurationField.Unit.DAYS, unit.getValue());
    for (int seconds : new int[] {61, 3600, 1209600, Integer.MAX_VALUE}) {
      field.setSecondsValue(seconds);
      assertEquals(String.valueOf(seconds), field.getSecondsValue());
    }
    field.setSecondsValue(1209600);
    unit.setValue(AdminDurationField.Unit.HOURS);
    assertEquals("50400", field.getSecondsValue());
    amount.setValue(999999);
    ReflectionTestUtils.invokeMethod(section, "saveConfigValues");
    assertTrue(field.isInvalid());
    verify(config, never()).set(any(), anyString());
    amount.clear();
    assertEquals(
        "1209600",
        ReflectionTestUtils.invokeMethod(
            section, "extractInputValue", ConfigEntry.REMEMBER_ME_DURATION));
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void durationHelpShowsUnsavedValueAndConversionOnlyInsideHelp(String language) {
    locale.set(Locale.forLanguageTag(language));
    section.showGroup(AdminConfigGroup.SECURITY);
    section.refreshTexts();
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    AdminDurationField field = (AdminDurationField) inputs.get(ConfigEntry.REMEMBER_ME_DURATION);
    IntegerField amount = (IntegerField) ReflectionTestUtils.getField(field, "amount");
    amount.setValue(7);
    ReflectionTestUtils.invokeMethod(section, "openHelp", ConfigEntry.REMEMBER_ME_DURATION);
    AdminSidePanel help = (AdminSidePanel) ReflectionTestUtils.getField(section, "helpPanel");
    String copy = help.getElement().getTextRecursively();
    assertTrue(copy.contains(field.getDisplayValue()));
    assertTrue(copy.contains(field.getEquivalentValue()));
    assertFalse(field.getElement().getTextRecursively().contains(field.getEquivalentValue()));
    locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertEquals(7, amount.getValue());
    assertTrue(help.getElement().getTextRecursively().contains(field.getDisplayValue()));
    verify(config, never()).set(any(), anyString());
  }

  @Test
  void languageRefreshKeepsTheSelectedGroupAndUnsavedInput() {
    section.showGroup(AdminConfigGroup.SECURITY);
    List<ConfigEntry> securityEntries =
        List.of(
            ConfigEntry.REMEMBER_ME_DURATION,
            ConfigEntry.REMEMBER_ME_SECRET_KEY,
            ConfigEntry.LOGIN_ATTEMPTS_LIMIT,
            ConfigEntry.MAX_SESSIONS_PER_USER,
            ConfigEntry.MAINTENANCE_MODE);
    assertEquals(securityEntries, visibleEntries());
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    PasswordField secretField = (PasswordField) inputs.get(ConfigEntry.REMEMBER_ME_SECRET_KEY);
    secretField.setValue("edited-test-value");
    H2 title = (H2) ReflectionTestUtils.getField(section, "currentGroupTitle");
    String germanTitle =
        MessageProperties.forLocale(provider, Locale.GERMAN).getAdminConfigGroupSecurity();
    assertEquals(germanTitle, title.getText());

    locale.set(Locale.ENGLISH);
    section.refreshTexts();
    assertEquals(
        MessageProperties.forLocale(provider, Locale.ENGLISH).getAdminConfigGroupSecurity(),
        title.getText());
    assertNotEquals(germanTitle, title.getText());
    assertEquals(AdminConfigGroup.SECURITY, selectedGroup());
    assertEquals(securityEntries, visibleEntries());
    assertEquals("edited-test-value", secretField.getValue());

    locale.set(Locale.GERMAN);
    section.refreshTexts();
    assertEquals(germanTitle, title.getText());
    assertEquals(AdminConfigGroup.SECURITY, selectedGroup());
    assertEquals("edited-test-value", secretField.getValue());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void tooltipUsesConfiguredDefaultAndRefreshesLanguageWithoutSavingTheDraft(String language) {
    when(config.getDefaultValue(ConfigEntry.SERVICE_NAME)).thenReturn("Configured default");
    when(config.getDefaultValue(ConfigEntry.REMEMBER_ME_SECRET_KEY)).thenReturn("SecretDefault1!");
    locale.set(Locale.forLanguageTag(language));
    section.refreshTexts();
    MessageProperties messages = MessageProperties.forLocale(provider, locale.get());
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    Map<?, ?> infos = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInfoButtons");
    TextField serviceName = (TextField) inputs.get(ConfigEntry.SERVICE_NAME);
    serviceName.setValue("Unsaved draft");
    ConfigInfoButton info = (ConfigInfoButton) infos.get(ConfigEntry.SERVICE_NAME);
    assertEquals("Configured default", serviceName.getPlaceholder());
    assertTrue(
        info.getTooltip()
            .getText()
            .contains(messages.getAdminConfigDefault() + ": Configured default"));
    assertTrue(
        info.getTooltip().getText().contains(messages.getAdminConfigHelperServiceNameMax(30)));
    assertFalse(
        ((ConfigInfoButton) infos.get(ConfigEntry.REMEMBER_ME_SECRET_KEY))
            .getTooltip()
            .getText()
            .contains("SecretDefault1!"));
    locale.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    section.refreshTexts();
    assertEquals("Unsaved draft", serviceName.getValue());
    assertTrue(
        info.getTooltip()
            .getText()
            .contains(MessageProperties.forLocale(provider, locale.get()).getAdminConfigDefault()));
    verify(config, never()).set(any(ConfigEntry.class), anyString());
  }

  private AdminConfigGroup selectedGroup() {
    return (AdminConfigGroup) ReflectionTestUtils.getField(section, "selectedGroup");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  @SuppressWarnings("unchecked")
  void everyFieldHelpKeepsRulesAndSafeDefaultsWithoutChangingConfiguration(String language) {
    locale.set(Locale.forLanguageTag(language));
    section.refreshTexts();
    MessageProperties messages = MessageProperties.forLocale(provider, locale.get());
    Map<ConfigEntry, ConfigInfoButton> infos =
        (Map<ConfigEntry, ConfigInfoButton>)
            ReflectionTestUtils.getField(section, "configInfoButtons");
    AdminSidePanel panel = (AdminSidePanel) ReflectionTestUtils.getField(section, "helpPanel");
    int checkedFields = 0;
    for (AdminConfigGroup group : AdminConfigGroup.values()) {
      section.showGroup(group);
      for (Object value : visibleEntries()) {
        ConfigEntry entry = (ConfigEntry) value;
        infos.get(entry).click();
        assertTrue(panel.isOpened());
        String content = panel.getElement().getTextRecursively();
        assertTrue(content.contains(messages.getAdminConfigEntryLabel(entry)), entry.name());
        assertTrue(content.contains(messages.getAdminConfigEntryTooltip(entry)), entry.name());
        assertTrue(content.contains(messages.getAdminConfigGroupLabel(group)), entry.name());
        assertTrue(content.contains(messages.getAdminConfigInputRules()), entry.name());
        List<String> rules = ReflectionTestUtils.invokeMethod(section, "buildInputRules", entry);
        for (String rule : rules) {
          assertTrue(content.contains(rule), entry.name());
        }
        if (entry == ConfigEntry.COPYRIGHT_TEXT) {
          assertTrue(content.contains(messages.getAdminConfigHelpYear()));
          assertTrue(content.contains(messages.getAdminConfigHelpServiceName()));
        }
        if (entry == ConfigEntry.REMEMBER_ME_SECRET_KEY) {
          assertFalse(content.contains(entry.getDefaultValueString()));
          assertTrue(content.contains(messages.getAdminConfigDefaultConfidential()));
        }
        panel.close();
        checkedFields++;
      }
    }
    assertEquals(30, checkedFields);
    verify(config, never()).set(any(ConfigEntry.class), anyString());
  }

  private List<?> visibleEntries() {
    return ReflectionTestUtils.invokeMethod(section, "getVisibleConfigEntries");
  }
}
