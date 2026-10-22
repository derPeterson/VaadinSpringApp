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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.ConfigInfoButton;
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
        (com.vaadin.flow.component.textfield.IntegerField)
            inputs.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS);
    IntegerField history =
        (com.vaadin.flow.component.textfield.IntegerField)
            inputs.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS);
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
        (com.vaadin.flow.component.textfield.IntegerField)
            inputs.get(ConfigEntry.USER_NOTIFICATIONS_INBOX_RETENTION_DAYS);
    IntegerField history =
        (com.vaadin.flow.component.textfield.IntegerField)
            inputs.get(ConfigEntry.USER_NOTIFICATIONS_HISTORY_RETENTION_DAYS);
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

  private List<?> visibleEntries() {
    return ReflectionTestUtils.invokeMethod(section, "getVisibleConfigEntries");
  }
}
