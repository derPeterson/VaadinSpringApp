package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AdminDurationField;
import de.derpeterson.app.ui.components.AdminSidePanelDialog;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Group identity and language-refresh lifecycle; no application, database or browser. */
class AdminConfigurationGroupTest {
  private final CustomI18NProvider i18nProvider = new CustomI18NProvider();
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);
  private UI testUi;
  private VaadinSession vaadinSession;
  private AdminConfigurationSection configurationSection;
  private ConfigService configService;

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
    configurationSection =
        new AdminConfigurationSection(new MessageProperties(i18nProvider), configService, () -> {});
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    configurationSection = null;
    testUi = null;
    vaadinSession = null;
  }

  @Test
  void notificationGroupContainsTwoPositiveDaySettingsAndSavesThemIndependently() {
    configurationSection.showGroup(AdminConfigGroup.NOTIFICATIONS);

    assertEquals(
        List.of(
            ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS,
            ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS),
        visibleEntries());
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    IntegerField inbox =
        (IntegerField)
            ReflectionTestUtils.getField(
                configInputsByEntry.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS),
                "durationAmountField");
    IntegerField history =
        (IntegerField)
            ReflectionTestUtils.getField(
                configInputsByEntry.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS),
                "durationAmountField");

    assertEquals(180, inbox.getValue());
    assertEquals(365, history.getValue());
    inbox.setValue(0);
    ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");

    assertTrue(inbox.isInvalid());
    verify(configService, never()).set(any(), anyString());
    inbox.setValue(30);
    history.setValue(14);
    ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");

    verify(configService).set(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS, "30");
    verify(configService).set(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS, "14");
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void emptyNotificationRetentionStaysEmptyAfterSaveReloadAndLanguageChange(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    EnumMap<ConfigEntry, String> stored =
        new java.util.EnumMap<ConfigEntry, String>(ConfigEntry.class);
    when(configService.getString(any()))
        .thenAnswer(call -> {
          ConfigEntry entry = call.getArgument(0);
          return stored.getOrDefault(entry, entry.getDefaultValueString());
        });
    doAnswer(
            call -> {
              stored.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(configService)
        .set(any(), anyString());
    configurationSection.showGroup(AdminConfigGroup.NOTIFICATIONS);
    configurationSection.refreshTexts();
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    IntegerField inbox =
        (IntegerField)
            ReflectionTestUtils.getField(
                configInputsByEntry.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS),
                "durationAmountField");
    IntegerField history =
        (IntegerField)
            ReflectionTestUtils.getField(
                configInputsByEntry.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS),
                "durationAmountField");
    inbox.clear();
    history.clear();
    MessageProperties messages = MessageProperties.forLocale(i18nProvider, localeReference.get());

    assertEquals(messages.getAdminConfigUnlimited(), inbox.getPlaceholder());
    assertEquals(messages.getAdminConfigUnlimited(), history.getPlaceholder());
    ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");

    assertFalse(inbox.isInvalid());
    assertFalse(history.isInvalid());
    assertEquals("", stored.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS));
    assertEquals("", stored.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS));
    assertNull(inbox.getValue());
    assertNull(history.getValue());
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    configurationSection.refreshTexts();
    ReflectionTestUtils.invokeMethod(configurationSection, "loadConfigValues");

    assertNull(inbox.getValue());
    assertNull(history.getValue());
    assertEquals(
        MessageProperties.forLocale(i18nProvider, localeReference.get()).getAdminConfigUnlimited(),
        inbox.getPlaceholder());
    Map<?, ?> infos =
        (Map<?, ?>) ReflectionTestUtils.getField(configurationSection, "configInfoButtons");
    ConfigInfoButton info =
        (ConfigInfoButton) infos.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS);
    MessageProperties translated = MessageProperties.forLocale(i18nProvider, localeReference.get());

    assertTrue(info.getTooltip().getText().contains(translated.getAdminConfigRuleEmptyUnlimited()));
    assertFalse(
        info.getTooltip().getText().contains(translated.getAdminConfigRuleEmptyUsesDefault()));
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @Test
  void smtpGroupContainsOnlyTheSupportedTransportSettings() {
    configurationSection.showGroup(AdminConfigGroup.SMTP);

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
    configurationSection.showGroup(AdminConfigGroup.TOKENS);
    List<ConfigEntry> tokenEntries =
        List.of(
            ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
            ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
            ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION);

    assertEquals(tokenEntries, visibleEntries());
    configurationSection.showGroup(null);

    assertEquals(AdminConfigGroup.TOKENS, selectedGroup());
    assertEquals(tokenEntries, visibleEntries());
    configurationSection.showGroup(AdminConfigGroup.GENERAL);

    assertEquals(
        List.of(ConfigEntry.SERVICE_NAME, ConfigEntry.COPYRIGHT_TEXT, ConfigEntry.BASE_URL),
        visibleEntries());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void allTimeSpanFieldsPreserveValuesMinimumsAndUnsavedUnitChanges(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    configurationSection.refreshTexts();
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    for (ConfigEntry entry :
        new ConfigEntry[] {
          ConfigEntry.USER_AUTO_ABSENT_TIMEOUT, ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
          ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION,
              ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
          ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION, ConfigEntry.EMAIL_QUEUE_SENT_LIVE_DURATION
        }) {
      AdminDurationField adminDurationField = (AdminDurationField) configInputsByEntry.get(entry);
      adminDurationField.setStoredValue("PT90.123456789S");

      assertEquals("PT1M30.123456789S", adminDurationField.getStoredValue());
      assertTrue(adminDurationField.getEquivalentValue().contains("123456789"));
      adminDurationField.setStoredValue("PT9223372036854775807.999999999S");

      assertEquals("PT2562047788015215H30M7.999999999S", adminDurationField.getStoredValue());
      adminDurationField.setStoredValue("PT2H");
      @SuppressWarnings("unchecked")
      ComboBox<AdminDurationField.Unit> durationUnitComboBox =
          (ComboBox<AdminDurationField.Unit>)
              ReflectionTestUtils.getField(adminDurationField, "durationUnitComboBox");
      durationUnitComboBox.setValue(AdminDurationField.Unit.MINUTES);

      assertEquals("PT2M", adminDurationField.getStoredValue());
      BigDecimalField amount =
          (BigDecimalField)
              ReflectionTestUtils.getField(adminDurationField, "preciseDurationField");
      amount.setValue(new BigDecimal("0.5"));

      assertEquals("PT30S", adminDurationField.getStoredValue());
      String error =
          ReflectionTestUtils.invokeMethod(
              configurationSection, "validateValue", entry, adminDurationField.getStoredValue());
      if (entry.getMinimumTokenDuration() != null) {
        assertNotNull(error);
      } else {
        assertNull(error);
      }
      amount.clear();

      assertEquals(
          "", ReflectionTestUtils.invokeMethod(configurationSection, "extractInputValue", entry));
      assertNotNull(
          ReflectionTestUtils.invokeMethod(configurationSection, "validateValue", entry, ""));
    }
    verify(configService, never()).set(any(), anyString());
  }

  @Test
  void timeSpanSavingUsesExistingIsoFormatAndRejectsInvalidPrecision() {
    configurationSection.showGroup(AdminConfigGroup.TOKENS);
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    AdminDurationField adminDurationField =
        (AdminDurationField) configInputsByEntry.get(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION);
    BigDecimalField amount =
        (BigDecimalField) ReflectionTestUtils.getField(adminDurationField, "preciseDurationField");
    adminDurationField.setStoredValue("PT1S");
    amount.setValue(new BigDecimal("0.0000000001"));

    assertEquals("invalid", adminDurationField.getStoredValue());
    ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");

    assertTrue(adminDurationField.isInvalid());
    verify(configService, never()).set(any(), anyString());
    adminDurationField.setStoredValue("PT90S");
    ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");

    verify(configService).set(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION, "PT1M30S");
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @Test
  void sessionTimeoutSavesInSeconds() {
    configurationSection.showGroup(AdminConfigGroup.SECURITY);
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    AdminDurationField adminDurationField =
        (AdminDurationField) configInputsByEntry.get(ConfigEntry.SESSION_TIMEOUT_SECONDS);

    assertEquals("1800", adminDurationField.getStoredValue());
    adminDurationField.setSecondsValue(10);
    ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");

    verify(configService).set(ConfigEntry.SESSION_TIMEOUT_SECONDS, "10");
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @Test
  void restartWarningNamesOnlyTheChangedStartupSetting() {
    configurationSection.showGroup(AdminConfigGroup.SECURITY);
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    AdminDurationField timeout =
        (AdminDurationField) configInputsByEntry.get(ConfigEntry.SESSION_TIMEOUT_SECONDS);
    PasswordField key = (PasswordField) configInputsByEntry.get(ConfigEntry.REMEMBER_ME_SECRET_KEY);
    MessageProperties messages = new MessageProperties(i18nProvider);
    NotificationHelper notification = NotificationHelper.getInstance();
    try {
      timeout.setSecondsValue(120);
      ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");
      Supplier<?> ordinary =
          (Supplier<?>) ReflectionTestUtils.getField(notification, "messageSupplier");

      assertEquals(messages.getAdminConfigSaveSuccess(), ordinary.get());

      timeout.setSecondsValue(120);
      key.setValue("fixture-new-key");
      ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");
      Supplier<?> warning =
          (Supplier<?>) ReflectionTestUtils.getField(notification, "messageSupplier");

      assertEquals(messages.getAdminConfigRestartSaved(), warning.get());
      Component layout = (Component) ReflectionTestUtils.getField(notification, "textLayout");

      assertEquals(
          List.of(messages.getAdminConfigEntryRememberMeSecretKey()),
          layout
              .getElement()
              .getChildren()
              .filter(element -> "ul".equals(element.getTag()))
              .flatMap(Node::getChildren)
              .map(Element::getTextRecursively)
              .toList());
    } finally {
      notification.closeAndClearAllNotifications();
    }
  }

  @Test
  void fieldHelpSeparatesLiveTimingFromTheRestartWarning() {
    configurationSection.showGroup(AdminConfigGroup.SECURITY);
    MessageProperties messages = new MessageProperties(i18nProvider);
    AdminSidePanelDialog help =
        (AdminSidePanelDialog) ReflectionTestUtils.getField(configurationSection, "helpPanel");

    ReflectionTestUtils.invokeMethod(
        configurationSection, "openHelp", ConfigEntry.SESSION_TIMEOUT_SECONDS);
    String timeoutCopy = help.getElement().getTextRecursively();

    assertTrue(timeoutCopy.contains(messages.getAdminConfigEffectiveTitle()));
    assertTrue(timeoutCopy.contains(messages.getAdminConfigEffectiveSessionNew()));
    assertTrue(timeoutCopy.contains(messages.getAdminConfigEffectiveSessionExisting()));
    assertFalse(timeoutCopy.contains(messages.getAdminConfigRestartRequired()));

    ReflectionTestUtils.invokeMethod(
        configurationSection, "openHelp", ConfigEntry.REMEMBER_ME_SECRET_KEY);

    assertTrue(
        help.getElement().getTextRecursively().contains(messages.getAdminConfigRestartRequired()));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void allConfigurationFieldsRenderLocalizedTimingNotes(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    AdminSidePanelDialog help =
        (AdminSidePanelDialog) ReflectionTestUtils.getField(configurationSection, "helpPanel");
    for (ConfigEntry entry : ConfigEntry.values()) {
      ReflectionTestUtils.invokeMethod(configurationSection, "openHelp", entry);
      Component timing =
          help.getChildren()
              .filter(
                  child -> child.getElement().getClassList().contains("admin-help-effective-use"))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Feldhilfe ohne Wirksamkeit: " + entry));
      List<String> notes =
          timing
              .getElement()
              .getChildren()
              .filter(element -> "ul".equals(element.getTag()))
              .flatMap(Node::getChildren)
              .map(Element::getTextRecursively)
              .toList();

      assertFalse(notes.isEmpty(), entry.name());
      assertTrue(
          notes.stream().noneMatch(note -> note.isBlank() || note.contains("effective_")),
          entry.name());
    }
  }

  @Test
  void durationConvertsExactlyAndRejectsOverflowWithoutSaving() {
    configurationSection.showGroup(AdminConfigGroup.SECURITY);
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    AdminDurationField adminDurationField =
        (AdminDurationField) configInputsByEntry.get(ConfigEntry.REMEMBER_ME_DURATION);
    IntegerField amount =
        (IntegerField) ReflectionTestUtils.getField(adminDurationField, "durationAmountField");
    @SuppressWarnings("unchecked")
    ComboBox<AdminDurationField.Unit> durationUnitComboBox =
        (ComboBox<AdminDurationField.Unit>)
            ReflectionTestUtils.getField(adminDurationField, "durationUnitComboBox");

    assertEquals(14, amount.getValue());
    assertEquals(AdminDurationField.Unit.DAYS, durationUnitComboBox.getValue());
    for (int seconds : new int[] {61, 3600, 1209600, Integer.MAX_VALUE}) {
      adminDurationField.setSecondsValue(seconds);

      assertEquals(String.valueOf(seconds), adminDurationField.getSecondsValue());
    }
    adminDurationField.setSecondsValue(1209600);
    durationUnitComboBox.setValue(AdminDurationField.Unit.HOURS);

    assertEquals("50400", adminDurationField.getSecondsValue());
    amount.setValue(999999);
    ReflectionTestUtils.invokeMethod(configurationSection, "saveConfigValues");

    assertTrue(adminDurationField.isInvalid());
    verify(configService, never()).set(any(), anyString());
    amount.clear();

    assertEquals(
        "1209600",
        ReflectionTestUtils.invokeMethod(
            configurationSection, "extractInputValue", ConfigEntry.REMEMBER_ME_DURATION));
    de.derpeterson.app.helper.ui.NotificationHelper.getInstance().closeAndClearAllNotifications();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void durationHelpShowsUnsavedValueAndConversionOnlyInsideHelp(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    configurationSection.showGroup(AdminConfigGroup.SECURITY);
    configurationSection.refreshTexts();
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    AdminDurationField adminDurationField =
        (AdminDurationField) configInputsByEntry.get(ConfigEntry.REMEMBER_ME_DURATION);
    IntegerField amount =
        (IntegerField) ReflectionTestUtils.getField(adminDurationField, "durationAmountField");
    amount.setValue(7);
    ReflectionTestUtils.invokeMethod(
        configurationSection, "openHelp", ConfigEntry.REMEMBER_ME_DURATION);
    AdminSidePanelDialog help =
        (AdminSidePanelDialog) ReflectionTestUtils.getField(configurationSection, "helpPanel");
    String copy = help.getElement().getTextRecursively();

    assertTrue(copy.contains(adminDurationField.getDisplayValue()));
    assertTrue(copy.contains(adminDurationField.getEquivalentValue()));
    assertFalse(
        adminDurationField
            .getElement()
            .getTextRecursively()
            .contains(adminDurationField.getEquivalentValue()));
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    configurationSection.refreshTexts();

    assertEquals(7, amount.getValue());
    assertTrue(
        help.getElement().getTextRecursively().contains(adminDurationField.getDisplayValue()));
    verify(configService, never()).set(any(), anyString());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "de,SECONDS",
    "en,SECONDS",
    "de,DAYS",
    "en,DAYS",
    "de,DURATION",
    "en,DURATION"
  })
  void clearedDurationFieldsKeepDefaultUnlimitedAndRequiredMeanings(
      String language, AdminDurationField.Storage storage) {
    localeReference.set(Locale.forLanguageTag(language));
    MessageProperties messageProperties = new MessageProperties(i18nProvider);
    AdminDurationField adminDurationField = new AdminDurationField(messageProperties, storage);
    if (storage == AdminDurationField.Storage.DURATION) {
      adminDurationField.setDefaultValue("PT15M");
      adminDurationField.setStoredValue("PT30S");
      BigDecimalField amount =
          (BigDecimalField)
              ReflectionTestUtils.getField(adminDurationField, "preciseDurationField");
      amount.clear();

      assertEquals(
          messageProperties.getAdminConfigDurationInvalid(), adminDurationField.getDisplayValue());
      assertEquals(
          messageProperties.getAdminConfigDurationInvalid(),
          adminDurationField.getEquivalentValue());
    } else {
      adminDurationField.setStoredValue(
          storage == AdminDurationField.Storage.DAYS ? "7" : "1209600");
      IntegerField amount =
          (IntegerField) ReflectionTestUtils.getField(adminDurationField, "durationAmountField");
      amount.clear();
      if (storage == AdminDurationField.Storage.DAYS) {
        assertEquals(
            messageProperties.getAdminConfigUnlimited(), adminDurationField.getDisplayValue());
        assertEquals(
            messageProperties.getAdminConfigUnlimited(), adminDurationField.getEquivalentValue());
      } else {
        assertEquals(
            messageProperties.getAdminConfigDurationDays(BigDecimal.valueOf(14)),
            adminDurationField.getDisplayValue());
        assertEquals(
            messageProperties.getAdminConfigDurationSeconds(BigDecimal.valueOf(1209600)),
            adminDurationField.getEquivalentValue());
      }
    }
    assertEquals("", adminDurationField.getStoredValue());
    assertEquals("", adminDurationField.getSecondsValue());
  }

  @Test
  void languageRefreshKeepsTheSelectedGroupAndUnsavedInput() {
    configurationSection.showGroup(AdminConfigGroup.SECURITY);
    List<ConfigEntry> securityEntries =
        List.of(
            ConfigEntry.REMEMBER_ME_DURATION,
            ConfigEntry.REMEMBER_ME_SECRET_KEY,
            ConfigEntry.LOGIN_ATTEMPTS_LIMIT,
            ConfigEntry.LOGIN_ATTEMPT_WINDOW_SECONDS,
            ConfigEntry.MAX_SESSIONS_PER_USER,
            ConfigEntry.SESSION_TIMEOUT_SECONDS,
            ConfigEntry.MAINTENANCE_MODE);

    assertEquals(securityEntries, visibleEntries());
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    PasswordField secretField =
        (PasswordField) configInputsByEntry.get(ConfigEntry.REMEMBER_ME_SECRET_KEY);
    secretField.setValue("edited-test-value");
    H2 title = (H2) ReflectionTestUtils.getField(configurationSection, "currentGroupTitle");
    String germanTitle =
        MessageProperties.forLocale(i18nProvider, Locale.GERMAN).getAdminConfigGroupSecurity();

    assertEquals(germanTitle, title.getText());

    localeReference.set(Locale.ENGLISH);
    configurationSection.refreshTexts();

    assertEquals(
        MessageProperties.forLocale(i18nProvider, Locale.ENGLISH).getAdminConfigGroupSecurity(),
        title.getText());
    assertNotEquals(germanTitle, title.getText());
    assertEquals(AdminConfigGroup.SECURITY, selectedGroup());
    assertEquals(securityEntries, visibleEntries());
    assertEquals("edited-test-value", secretField.getValue());

    localeReference.set(Locale.GERMAN);
    configurationSection.refreshTexts();

    assertEquals(germanTitle, title.getText());
    assertEquals(AdminConfigGroup.SECURITY, selectedGroup());
    assertEquals("edited-test-value", secretField.getValue());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  void tooltipUsesConfiguredDefaultAndRefreshesLanguageWithoutSavingTheDraft(String language) {
    when(configService.getDefaultValue(ConfigEntry.SERVICE_NAME)).thenReturn("Configured default");
    when(configService.getDefaultValue(ConfigEntry.REMEMBER_ME_SECRET_KEY))
        .thenReturn("SecretDefault1!");
    localeReference.set(Locale.forLanguageTag(language));
    configurationSection.refreshTexts();
    MessageProperties messages = MessageProperties.forLocale(i18nProvider, localeReference.get());
    Map<?, ?> configInputsByEntry = getConfigInputsByEntry();
    Map<?, ?> infos =
        (Map<?, ?>) ReflectionTestUtils.getField(configurationSection, "configInfoButtons");
    TextField serviceName = (TextField) configInputsByEntry.get(ConfigEntry.SERVICE_NAME);
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
    localeReference.set(language.equals("de") ? Locale.ENGLISH : Locale.GERMAN);
    configurationSection.refreshTexts();

    assertEquals("Unsaved draft", serviceName.getValue());
    assertTrue(
        info.getTooltip()
            .getText()
            .contains(
                MessageProperties.forLocale(i18nProvider, localeReference.get())
                    .getAdminConfigDefault()));
    verify(configService, never()).set(any(ConfigEntry.class), anyString());
  }

  private AdminConfigGroup selectedGroup() {
    return (AdminConfigGroup) ReflectionTestUtils.getField(configurationSection, "selectedGroup");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"de", "en"})
  @SuppressWarnings("unchecked")
  void everyFieldHelpKeepsRulesAndSafeDefaultsWithoutChangingConfiguration(String language) {
    localeReference.set(Locale.forLanguageTag(language));
    configurationSection.refreshTexts();
    MessageProperties messages = MessageProperties.forLocale(i18nProvider, localeReference.get());
    Map<ConfigEntry, ConfigInfoButton> infos =
        (Map<ConfigEntry, ConfigInfoButton>)
            ReflectionTestUtils.getField(configurationSection, "configInfoButtons");
    AdminSidePanelDialog panel =
        (AdminSidePanelDialog) ReflectionTestUtils.getField(configurationSection, "helpPanel");
    int checkedFields = 0;
    for (AdminConfigGroup group : AdminConfigGroup.values()) {
      if (group.isSuperadminOnly()) {
        // Anbieter haben ein eigenes Panel ohne die generischen ConfigEntry-Feldhilfen.
        continue;
      }
      configurationSection.showGroup(group);
      for (Object value : visibleEntries()) {
        ConfigEntry entry = (ConfigEntry) value;
        infos.get(entry).click();

        assertTrue(panel.isOpened());
        String content = panel.getElement().getTextRecursively();

        assertTrue(content.contains(messages.getAdminConfigEntryLabel(entry)), entry.name());
        assertTrue(content.contains(messages.getAdminConfigEntryTooltip(entry)), entry.name());
        assertTrue(content.contains(messages.getAdminConfigGroupLabel(group)), entry.name());
        assertTrue(content.contains(messages.getAdminConfigInputRules()), entry.name());
        List<String> rules =
            ReflectionTestUtils.invokeMethod(configurationSection, "buildInputRules", entry);
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
    assertEquals(ConfigEntry.values().length, checkedFields);
    verify(configService, never()).set(any(ConfigEntry.class), anyString());
  }

  private List<?> visibleEntries() {
    return ReflectionTestUtils.invokeMethod(configurationSection, "getVisibleConfigEntries");
  }

  private Map<?, ?> getConfigInputsByEntry() {
    return (Map<?, ?>) ReflectionTestUtils.getField(configurationSection, "configInputs");
  }
}
