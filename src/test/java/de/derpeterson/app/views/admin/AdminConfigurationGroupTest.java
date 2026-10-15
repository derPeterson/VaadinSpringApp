package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.ConfigInfoButton;
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
    var inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
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
    var inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    var infos = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInfoButtons");
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
