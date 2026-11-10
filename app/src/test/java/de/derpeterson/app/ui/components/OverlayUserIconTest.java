package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
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
  private UI testUi;
  private MessageProperties messageProperties;
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void createAvatarTestEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getResourceRegistry()).thenReturn(new StreamResourceRegistry(vaadinSession));
    when(vaadinSession.access(any()))
        .thenAnswer(mockInvocation -> {
          ((Command) mockInvocation.getArgument(0)).execute();
          return CompletableFuture.completedFuture(null);
        });
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);

    messageProperties = new MessageProperties(new CustomI18NProvider());
  }

  @AfterEach
  void clearAvatarTestEnvironment() {
    // Tooltips hängen als eigene UI-Kinder: nur die selbst angehängten Avatare entfernen.
    testUi.getChildren().filter(OverlayUserIcon.class::isInstance).toList().forEach(testUi::remove);

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  private static Stream<Arguments> unicodeNameCases() {
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
  @MethodSource("unicodeNameCases")
  void initialsUseOneUnicodeCharacterPerName(
      String firstName, String lastName, String expectedInitials) {
    assertEquals(expectedInitials, OverlayUserIcon.initialsFor(firstName, lastName));
  }

  @Test
  void identityTooltipKeepsNameAndEmailOnSeparateLines() {
    OverlayUserIcon userAvatarIcon =
        new OverlayUserIcon(messageProperties, null, null, IconSize.PIXEL_24);
    userAvatarIcon.setName("Alex", "Beispiel");
    userAvatarIcon.showIdentityTooltip("alex@example.test");
    Tooltip identityTooltip =
        (Tooltip) ReflectionTestUtils.getField(userAvatarIcon, "avatarIdentityTooltip");

    assertNotNull(identityTooltip);
    assertEquals("Alex Beispiel\nalex@example.test", identityTooltip.getText());

    userAvatarIcon.setName(null, null);

    assertEquals("alex@example.test", identityTooltip.getText());
  }

  @ParameterizedTest
  @EnumSource(UserStatus.class)
  void allExistingStatusesAreIndependentOfInitials(UserStatus userStatus) {
    UserEntity testUserEntity =
        UserEntity.builder().firstName("Christian").lastName("Doebl").version(7L).build();
    OverlayUserIcon userAvatarIcon =
        new OverlayUserIcon(messageProperties, testUserEntity, userStatus, IconSize.PIXEL_48, true);
    testUi.add(userAvatarIcon);
    Span avatarInitialsSpan =
        (Span) ReflectionTestUtils.getField(userAvatarIcon, "avatarInitialsLabel");
    Div statusIndicatorDiv =
        (Div) ReflectionTestUtils.getField(userAvatarIcon, "statusIndicatorContainer");

    assertEquals("CD", avatarInitialsSpan.getText());
    assertEquals(1, statusIndicatorDiv.getComponentCount());
    Component statusIconComponent =
        statusIndicatorDiv.getComponentAt(0).getChildren().findFirst().orElseThrow();

    assertEquals("vaadin-icon", statusIconComponent.getElement().getTag());
    assertNotNull(statusIconComponent.getElement().getAttribute("data-lucide"));
    assertEquals("true", statusIconComponent.getElement().getAttribute("aria-hidden"));
    assertEquals(
        userStatus.getColor(), userAvatarIcon.getStyle().get("--cosmos-avatar-status-color"));
    assertEquals(
        "var(--cosmos-avatar-" + userStatus.name().toLowerCase(Locale.ROOT) + "-shadow)",
        userAvatarIcon.getStyle().get("--cosmos-avatar-status-shadow"));
    assertEquals(
        userStatus.getColor(),
        statusIndicatorDiv.getComponentAt(0).getElement().getStyle().get("color"));
    assertEquals(
        "Christian Doebl – " + messageProperties.getUserStatusLabel(userStatus),
        userAvatarIcon.getElement().getAttribute("aria-label"));

    userAvatarIcon.setUserStatus(UserStatus.ABSENT);
    userAvatarIcon.setName("New", "Name");

    assertEquals("NN", avatarInitialsSpan.getText());
    assertEquals(1, statusIndicatorDiv.getComponentCount());
    assertEquals(
        UserStatus.ABSENT.getColor(),
        statusIndicatorDiv.getComponentAt(0).getElement().getStyle().get("color"));
    assertEquals(
        UserStatus.ABSENT.getColor(),
        userAvatarIcon.getStyle().get("--cosmos-avatar-status-color"));
    assertEquals(7L, testUserEntity.getVersion());
  }

  @Test
  void anonymousAvatarUsesNeutralSymbolAndCanBecomeAnInitialsAvatarWithoutStaleChildren() {
    OverlayUserIcon userAvatarIcon =
        new OverlayUserIcon(messageProperties, null, null, IconSize.PIXEL_48);
    Span avatarInitialsSpan =
        (Span) ReflectionTestUtils.getField(userAvatarIcon, "avatarInitialsLabel");

    assertEquals(1, avatarInitialsSpan.getComponentCount());
    assertEquals(
        AppConstants.USER_ICON_ALT, userAvatarIcon.getElement().getAttribute("aria-label"));
    userAvatarIcon.setName("Christian", "Doebl");

    assertEquals("CD", avatarInitialsSpan.getText());
    assertEquals(0, avatarInitialsSpan.getComponentCount());

    userAvatarIcon.setIdentity(null);

    assertEquals(1, avatarInitialsSpan.getComponentCount());
  }

  @Test
  void attachedAvatarRefreshesItsCurrentStatusAndRemovesLanguageListenerOnDetach() {
    UserEntity testUserEntity =
        UserEntity.builder().firstName("Christian").lastName("Doebl").build();
    OverlayUserIcon userAvatarIcon =
        new OverlayUserIcon(
            messageProperties, testUserEntity, UserStatus.AVAILABLE, IconSize.PIXEL_48, true);
    testUi.add(userAvatarIcon);
    userAvatarIcon.setUserStatus(UserStatus.EMPLOYED);
    activeLocaleReference.set(Locale.ENGLISH);
    LanguageChangeEvent.fire(testUi, Locale.ENGLISH);
    String englishAriaLabel =
        "Christian Doebl – " + messageProperties.getUserStatusLabel(UserStatus.EMPLOYED);

    assertEquals(englishAriaLabel, userAvatarIcon.getElement().getAttribute("aria-label"));

    testUi.remove(userAvatarIcon);
    activeLocaleReference.set(Locale.GERMAN);
    LanguageChangeEvent.fire(testUi, Locale.GERMAN);

    assertEquals(englishAriaLabel, userAvatarIcon.getElement().getAttribute("aria-label"));
    testUi.add(userAvatarIcon);

    assertEquals(
        "Christian Doebl – " + messageProperties.getUserStatusLabel(UserStatus.EMPLOYED),
        userAvatarIcon.getElement().getAttribute("aria-label"));
  }

  @Test
  void profileEventsRefreshTheImageWithoutRebuildingItForAStatusChangeAndDetachUnsubscribes() {
    UserAvatarService userAvatarService = mock(UserAvatarService.class);
    UserProfileBroadcaster userProfileBroadcaster = new UserProfileBroadcaster();
    when(userAvatarService.events()).thenReturn(userProfileBroadcaster);
    UUID avatarRevisionUuid = UUID.randomUUID();
    State customAvatarState =
        new UserAvatarService.State(1L, AvatarMode.CUSTOM, true, avatarRevisionUuid);
    when(userAvatarService.state(12L))
        .thenReturn(
            new UserAvatarService.State(2L, AvatarMode.INITIALS, true, avatarRevisionUuid),
            customAvatarState);
    UserEntity testUserEntity =
        UserEntity.builder().id(12L).firstName("Christian").lastName("Doebl").build();
    OverlayUserIcon userAvatarIcon =
        new OverlayUserIcon(
            messageProperties, testUserEntity, UserStatus.AVAILABLE, IconSize.PIXEL_48);
    userAvatarIcon.configureAvatar(userAvatarService, customAvatarState);
    testUi.add(userAvatarIcon);
    Image avatarPhotoImage =
        (Image) ReflectionTestUtils.getField(userAvatarIcon, "avatarPhotoImage");
    String initialImageSource = avatarPhotoImage.getSrc();

    assertTrue(avatarPhotoImage.isVisible());
    userAvatarIcon.setUserStatus(UserStatus.ABSENT);

    assertEquals(initialImageSource, avatarPhotoImage.getSrc());
    Div statusIndicatorDiv =
        (Div) ReflectionTestUtils.getField(userAvatarIcon, "statusIndicatorContainer");

    assertFalse(statusIndicatorDiv.isVisible());
    assertFalse(userAvatarIcon.hasClassName("cosmos-avatar-with-status"));
    assertEquals(0, statusIndicatorDiv.getComponentCount());
    assertEquals("Christian Doebl", userAvatarIcon.getElement().getAttribute("aria-label"));

    userProfileBroadcaster.broadcast(
        new UserProfileBroadcaster.UserProfileChangeMessage(12L, "New", "Name"));

    assertFalse(avatarPhotoImage.isVisible());

    userProfileBroadcaster.broadcast(
        new UserProfileBroadcaster.UserProfileChangeMessage(12L, "New", "Name"));

    assertTrue(avatarPhotoImage.isVisible());
    assertNotEquals(initialImageSource, avatarPhotoImage.getSrc());
    assertEquals("New Name", userAvatarIcon.getElement().getAttribute("aria-label"));
    assertFalse(statusIndicatorDiv.isVisible());

    testUi.remove(userAvatarIcon);

    userProfileBroadcaster.broadcast(
        new UserProfileBroadcaster.UserProfileChangeMessage(12L, "Detached", "Name"));

    verify(userAvatarService, times(2)).state(12L);
  }
}
