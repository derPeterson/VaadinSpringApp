package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Group identity and language-refresh lifecycle; no application, database or browser. */
class AdminConfigurationGroupTest {
    private final CustomI18NProvider provider = new CustomI18NProvider();
    private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);
    private UI ui;
    private VaadinSession session;
    private AdminConfigurationSection section;

    @BeforeEach
    void setup() {
        ui = new UI();
        UI.setCurrent(ui);
        session = mock(VaadinSession.class);
        when(session.getLocale()).thenAnswer(ignored -> locale.get());
        when(session.hasLock()).thenReturn(true);
        VaadinSession.setCurrent(session);
        ConfigService config = mock(ConfigService.class);
        when(config.getString(any(ConfigEntry.class))).thenAnswer(call ->
                ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
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
        assertEquals(List.of(ConfigEntry.SERVICE_NAME, ConfigEntry.COPYRIGHT_TEXT, ConfigEntry.BASE_URL), visibleEntries());
        section.showGroup(AdminConfigGroup.TOKENS);
        List<ConfigEntry> tokenEntries = List.of(ConfigEntry.VERIFICATION_TOKEN_VALID_DURATION,
                ConfigEntry.VERIFICATION_TOKEN_LIVE_DURATION, ConfigEntry.PASSWORD_RESET_TOKEN_VALID_DURATION,
                ConfigEntry.PASSWORD_RESET_TOKEN_LIVE_DURATION);
        assertEquals(tokenEntries, visibleEntries());
        section.showGroup(null);
        assertEquals(AdminConfigGroup.TOKENS, selectedGroup());
        assertEquals(tokenEntries, visibleEntries());
        section.showGroup(AdminConfigGroup.GENERAL);
        assertEquals(List.of(ConfigEntry.SERVICE_NAME, ConfigEntry.COPYRIGHT_TEXT, ConfigEntry.BASE_URL), visibleEntries());
    }

    @Test
    void languageRefreshKeepsTheSelectedGroupAndUnsavedInput() {
        section.showGroup(AdminConfigGroup.SECURITY);
        List<ConfigEntry> securityEntries = List.of(ConfigEntry.REMEMBER_ME_DURATION,
                ConfigEntry.REMEMBER_ME_SECRET_KEY, ConfigEntry.LOGIN_ATTEMPTS_LIMIT,
                ConfigEntry.MAX_SESSIONS_PER_USER, ConfigEntry.MAINTENANCE_MODE);
        assertEquals(securityEntries, visibleEntries());
        var inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
        PasswordField secretField = (PasswordField) inputs.get(ConfigEntry.REMEMBER_ME_SECRET_KEY);
        secretField.setValue("edited-test-value");
        H2 title = (H2) ReflectionTestUtils.getField(section, "currentGroupTitle");
        String germanTitle = MessageProperties.forLocale(provider, Locale.GERMAN).getAdminConfigGroupSecurity();
        assertEquals(germanTitle, title.getText());

        locale.set(Locale.ENGLISH);
        section.refreshTexts();
        assertEquals(MessageProperties.forLocale(provider, Locale.ENGLISH).getAdminConfigGroupSecurity(), title.getText());
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

    private AdminConfigGroup selectedGroup() {
        return (AdminConfigGroup) ReflectionTestUtils.getField(section, "selectedGroup");
    }

    private List<?> visibleEntries() {
        return ReflectionTestUtils.invokeMethod(section, "getVisibleConfigEntries");
    }
}
