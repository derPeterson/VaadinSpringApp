package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.model.enums.IconSize;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserAvatarService.State;
import de.derpeterson.app.websocket.UserProfileBroadcaster;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

class OverlayUserIconTest {
  private UI ui;
  private MessageProperties messages;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getResourceRegistry()).thenReturn(new StreamResourceRegistry(session));
    when(session.access(any()))
        .thenAnswer(
            call -> {
              ((Command) call.getArgument(0)).execute();
              return CompletableFuture.completedFuture(null);
            });
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    messages = new MessageProperties(new CustomI18NProvider());
  }

  @AfterEach
  void cleanup() {
    // Tooltips hängen als eigene UI-Kinder: nur die selbst angehängten Avatare entfernen.
    ui.getChildren().filter(OverlayUserIcon.class::isInstance).toList().forEach(ui::remove);
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  static Stream<Arguments> names() {
    return Stream.of(
        Arguments.of(" Christian ", " Doebl ", "CD"),
        Arguments.of("Änne Maria", "Öster", "ÄÖ"),
        Arguments.of(null, "doebl", "D"),
        Arguments.of("chris", null, "C"),
        Arguments.of("ßeta", "Öster", "SÖ"),
        Arguments.of("e\u0301mile", "Smith", "E\u0301S"),
        Arguments.of("😀", "Test", "😀T"));
  }

  @ParameterizedTest
  @MethodSource("names")
  void initialsUseOneUnicodeCharacterPerName(String first, String last, String expected) {
    assertEquals(expected, OverlayUserIcon.initialsFor(first, last));
  }

  @ParameterizedTest
  @EnumSource(UserStatus.class)
  void allExistingStatusesAreIndependentOfInitials(UserStatus value) {
    UserEntity user =
        UserEntity.builder().firstName("Christian").lastName("Doebl").version(7L).build();
    OverlayUserIcon avatar = new OverlayUserIcon(messages, user, value, IconSize.PIXEL_48, true);
    ui.add(avatar);
    Span initials = (Span) ReflectionTestUtils.getField(avatar, "initials");
    Div status = (Div) ReflectionTestUtils.getField(avatar, "status");
    assertEquals("CD", initials.getText());
    assertEquals(1, status.getComponentCount());
    assertTrue(
        status.getComponentAt(0).getElement().getProperty("innerHTML").contains("fill=\"none\""));
    assertTrue(status.getComponentAt(0).getElement().getProperty("innerHTML").contains("<circle"));
    assertEquals(value.getColor(), avatar.getStyle().get("--cosmos-avatar-status-color"));
    assertEquals(
        "var(--cosmos-avatar-" + value.name().toLowerCase(Locale.ROOT) + "-shadow)",
        avatar.getStyle().get("--cosmos-avatar-status-shadow"));
    assertEquals(value.getColor(), status.getComponentAt(0).getElement().getStyle().get("color"));
    assertEquals(
        "Christian Doebl – " + messages.getUserStatusLabel(value),
        avatar.getElement().getAttribute("aria-label"));
    avatar.setUserStatus(UserStatus.ABSENT);
    avatar.setName("New", "Name");
    assertEquals("NN", initials.getText());
    assertEquals(1, status.getComponentCount());
    assertEquals(
        UserStatus.ABSENT.getColor(),
        status.getComponentAt(0).getElement().getStyle().get("color"));
    assertEquals(
        UserStatus.ABSENT.getColor(), avatar.getStyle().get("--cosmos-avatar-status-color"));
    assertEquals(7L, user.getVersion());
  }

  @Test
  void anonymousAvatarUsesNeutralSymbolAndCanBecomeAnInitialsAvatarWithoutStaleChildren() {
    OverlayUserIcon avatar = new OverlayUserIcon(messages, null, null, IconSize.PIXEL_48);
    Span initials = (Span) ReflectionTestUtils.getField(avatar, "initials");
    assertEquals(1, initials.getComponentCount());
    assertEquals(AppConstants.USER_ICON_ALT, avatar.getElement().getAttribute("aria-label"));
    avatar.setName("Christian", "Doebl");
    assertEquals("CD", initials.getText());
    assertEquals(0, initials.getComponentCount());
    avatar.setIdentity(null);
    assertEquals(1, initials.getComponentCount());
  }

  @Test
  void attachedAvatarRefreshesItsCurrentStatusAndRemovesLanguageListenerOnDetach() {
    UserEntity user = UserEntity.builder().firstName("Christian").lastName("Doebl").build();
    OverlayUserIcon avatar =
        new OverlayUserIcon(messages, user, UserStatus.AVAILABLE, IconSize.PIXEL_48, true);
    ui.add(avatar);
    avatar.setUserStatus(UserStatus.EMPLOYED);
    locale.set(Locale.ENGLISH);
    LanguageChangeEvent.fire(ui, Locale.ENGLISH);
    String english = "Christian Doebl – " + messages.getUserStatusLabel(UserStatus.EMPLOYED);
    assertEquals(english, avatar.getElement().getAttribute("aria-label"));
    ui.remove(avatar);
    locale.set(Locale.GERMAN);
    LanguageChangeEvent.fire(ui, Locale.GERMAN);
    assertEquals(english, avatar.getElement().getAttribute("aria-label"));
    ui.add(avatar);
    assertEquals(
        "Christian Doebl – " + messages.getUserStatusLabel(UserStatus.EMPLOYED),
        avatar.getElement().getAttribute("aria-label"));
  }

  @Test
  void profileEventsRefreshTheImageWithoutRebuildingItForAStatusChangeAndDetachUnsubscribes() {
    UserAvatarService service = mock(UserAvatarService.class);
    UserProfileBroadcaster events = new UserProfileBroadcaster();
    when(service.events()).thenReturn(events);
    UUID revision = UUID.randomUUID();
    State custom = new UserAvatarService.State(1L, AvatarMode.CUSTOM, true, revision);
    when(service.state(12L))
        .thenReturn(new UserAvatarService.State(2L, AvatarMode.INITIALS, true, revision), custom);
    UserEntity user = UserEntity.builder().id(12L).firstName("Christian").lastName("Doebl").build();
    OverlayUserIcon avatar =
        new OverlayUserIcon(messages, user, UserStatus.AVAILABLE, IconSize.PIXEL_48);
    avatar.configureAvatar(service, custom);
    ui.add(avatar);
    Image photo = (Image) ReflectionTestUtils.getField(avatar, "photo");
    String first = photo.getSrc();
    assertTrue(photo.isVisible());
    avatar.setUserStatus(UserStatus.ABSENT);
    assertEquals(first, photo.getSrc());
    Div status = (Div) ReflectionTestUtils.getField(avatar, "status");
    assertFalse(status.isVisible());
    assertFalse(avatar.hasClassName("cosmos-avatar-with-status"));
    assertEquals(0, status.getComponentCount());
    assertEquals("Christian Doebl", avatar.getElement().getAttribute("aria-label"));
    events.broadcast(new UserProfileBroadcaster.Message(12L, "New", "Name"));
    assertFalse(photo.isVisible());
    events.broadcast(new UserProfileBroadcaster.Message(12L, "New", "Name"));
    assertTrue(photo.isVisible());
    assertNotEquals(first, photo.getSrc());
    assertEquals("New Name", avatar.getElement().getAttribute("aria-label"));
    assertFalse(status.isVisible());
    ui.remove(avatar);
    events.broadcast(new UserProfileBroadcaster.Message(12L, "Detached", "Name"));
    verify(service, times(2)).state(12L);
  }
}
