package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AdminConfigGroup;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.service.ConfigService;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

/** SMTP policy through the actual configuration validator; no application, database or mail. */
class AdminEmailConfigurationValidationTest {
  private UI ui;
  private VaadinSession session;
  private AdminConfigurationSection section;
  private MessageProperties messages;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    when(session.getLocale()).thenReturn(Locale.GERMAN);
    when(session.hasLock()).thenReturn(true);
    VaadinSession.setCurrent(session);
    messages = MessageProperties.forLocale(new CustomI18NProvider(), Locale.GERMAN);
    ConfigService config = mock(ConfigService.class);
    when(config.getString(any(ConfigEntry.class)))
        .thenAnswer(call -> ((ConfigEntry) call.getArgument(0)).getDefaultValueString());
    section = new AdminConfigurationSection(messages, config, () -> {});
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    section = null;
    session = null;
    ui = null;
  }

  @ParameterizedTest
  @EnumSource(
      value = ConfigEntry.class,
      names = {"EMAIL_FROM", "EMAIL_ADMIN"})
  void smtpPolicyAndTrimmingRemainUnchanged(ConfigEntry entry) {
    String[] accepted = {
      "a@b.c",
      "user+tag@example.test",
      "ü@domain.test",
      "a!b@b.c",
      "a@b..",
      "a@..b",
      "a@b.c.",
      "a@.b.c",
      "a@b.c.d",
      "a@b.\u2003",
      "a\u00a0b@c.d",
      "用户@域名.测试",
      " \ta@b.c\r\n"
    };
    String[] rejected = {
      null,
      "",
      " ",
      "invalid",
      "@b.c",
      "a@",
      "a@b",
      "a@b.",
      "a@.b",
      "a@.",
      "a@@b.c",
      "a@b.c@d.e",
      "a b@c.d",
      "a@b c.d",
      "a@b. c",
      "a@b.\tc",
      "a@b.\nc",
      "a@b.\rc",
      "a@b.\fc",
      "a@b.\u000bc"
    };
    for (String value : accepted) {
      assertNull(validate(entry, value), () -> "Expected accepted SMTP address: " + value);
    }
    for (String value : rejected) {
      assertEquals(messages.getAdminConfigValidationEmail(), validate(entry, value));
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = ConfigEntry.class,
      names = {"EMAIL_FROM", "EMAIL_ADMIN"})
  void longValidAndInvalidValuesDoNotCauseExcessiveBacktracking(ConfigEntry entry) {
    String domain = "b.".repeat(50_000);
    String local = "a".repeat(100_000);
    assertTimeout(
        Duration.ofSeconds(3),
        () -> {
          assertNull(validate(entry, "a@" + domain + "test"));
          assertNull(validate(entry, local + "@b.c"));
          assertNotNull(validate(entry, "a@" + domain + " x"));
          assertNotNull(validate(entry, "a@" + domain + "test@other.test"));
          assertNotNull(validate(entry, "a@" + local));
          assertNotNull(validate(entry, local));
          assertNull(validate(entry, "a@" + domain));
        });
  }

  @ParameterizedTest
  @EnumSource(
      value = ConfigEntry.class,
      names = {"EMAIL_FROM", "EMAIL_ADMIN"})
  void automaticFieldValidationUsesTheSameSafeSmtpPolicy(ConfigEntry entry) {
    section.showGroup(AdminConfigGroup.EMAIL_SENDER);
    Map<?, ?> inputs = (Map<?, ?>) ReflectionTestUtils.getField(section, "configInputs");
    EmailField field = (EmailField) inputs.get(entry);
    String domain = "b.".repeat(50_000);
    assertTimeout(
        Duration.ofSeconds(3),
        () -> {
          field.setValue("a@" + domain + "test");
          assertFalse(
              field
                  .getDefaultValidator()
                  .apply(field.getValue(), new ValueContext(field))
                  .isError());
          field.setValue("a@" + domain + " x");
          assertTrue(
              field
                  .getDefaultValidator()
                  .apply(field.getValue(), new ValueContext(field))
                  .isError());
        });
  }

  private String validate(ConfigEntry entry, String value) {
    return ReflectionTestUtils.invokeMethod(section, "validateValue", entry, value);
  }
}
