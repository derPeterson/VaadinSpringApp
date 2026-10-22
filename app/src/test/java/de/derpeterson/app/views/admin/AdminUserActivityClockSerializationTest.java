package de.derpeterson.app.views.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.RoleService;
import de.derpeterson.app.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Serialisiert nur die tatsächlichen Clock-Feldwerte, nicht den vollständigen Vaadin-UI-Graph. */
class AdminUserActivityClockSerializationTest {
  enum Owner {
    FILTER,
    USER_MANAGEMENT
  }

  private UI ui;
  private VaadinSession session;
  private MessageProperties texts;
  private UserService users;

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    session = mock(VaadinSession.class);
    when(session.getLocale()).thenReturn(Locale.ENGLISH);
    VaadinSession.setCurrent(session);
    texts = MessageProperties.forLocale(new CustomI18NProvider(), Locale.ENGLISH);
    users = mock(UserService.class);
    when(users.findAllUsers()).thenReturn(List.of());
  }

  @AfterEach
  void cleanup() {
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
    ui = null;
    session = null;
  }

  @ParameterizedTest
  @EnumSource(Owner.class)
  void actualDefaultClockFieldsCanBeSerializedAndReadBack(Owner owner) throws Exception {
    Component component =
        switch (owner) {
          case FILTER -> new AdminUserActivityFilter();
          case USER_MANAGEMENT ->
              new AdminUserManagementSection(
                  texts, users, mock(RoleService.class), mock(SecurityService.class));
        };
    Clock original = storedClock(component);
    Clock restored = roundTrip(original);
    assertEquals(original, restored);
    assertEquals(ZoneOffset.UTC, restored.getZone());
    assertNotNull(restored.instant());
  }

  @ParameterizedTest
  @EnumSource(Owner.class)
  void fixedFieldClockRetainsItsInstantAndZoneAndRemainsUsableAfterReading(Owner owner)
      throws Exception {
    Instant instant = Instant.parse("2026-10-25T01:45:00Z");
    Clock fixed = Clock.fixed(instant, ZoneId.of("America/New_York"));
    Component original = withClock(owner, fixed);
    Clock restored = roundTrip(storedClock(original));
    assertEquals(instant, restored.instant());
    assertEquals(fixed.getZone(), restored.getZone());
    Component rebuilt = withClock(owner, restored);
    if (rebuilt instanceof AdminUserActivityFilter filter) {
      filter.setLocale(Locale.ENGLISH);
      filter.refreshTexts(texts);
      DatePicker date = (DatePicker) ReflectionTestUtils.getField(filter, "date");
      TimePicker time = (TimePicker) ReflectionTestUtils.getField(filter, "time");
      assertEquals("10/25/2026", date.getPlaceholder());
      assertEquals(
          LocalTime.of(2, 45),
          LocalTime.parse(
              time.getPlaceholder(), DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)));
    } else {
      UserEntity user = UserEntity.builder().lastActivity(instant.minusSeconds(900)).build();
      Span activity = ReflectionTestUtils.invokeMethod(rebuilt, "createLastActivity", user);
      assertEquals(texts.getAdminUsersActivityMinutes(15), activity.getText());
      assertTrue(activity.getElement().getAttribute("title").endsWith("+01:00"));
    }
  }

  private Component withClock(Owner owner, Clock clock) {
    return switch (owner) {
      case FILTER -> new AdminUserActivityFilter(clock);
      case USER_MANAGEMENT ->
          new AdminUserManagementSection(
              texts, users, mock(RoleService.class), mock(SecurityService.class), clock);
    };
  }

  private Clock storedClock(Component component) {
    return (Clock) ReflectionTestUtils.getField(component, "clock");
  }

  private Clock roundTrip(Clock clock) throws Exception {
    byte[] serialized;
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(bytes)) {
      output.writeObject(clock);
      output.flush();
      serialized = bytes.toByteArray();
    }
    // Nur selbst erzeugte flüchtige Testbytes; keine externen serialisierten Daten einlesen.
    try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
      return (Clock) input.readObject();
    }
  }
}
