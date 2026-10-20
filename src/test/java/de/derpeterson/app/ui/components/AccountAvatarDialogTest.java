package de.derpeterson.app.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.model.enums.UserStatus;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserAvatarService.Change;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AccountAvatarDialogTest {
  private UI ui;
  private UserEntity user;
  private MessageProperties messages;
  private AccountAvatarDialog dialog;
  private UserAvatarService avatars;
  private final AtomicReference<Locale> locale = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    ui = new UI();
    UI.setCurrent(ui);
    VaadinSession session = mock(VaadinSession.class);
    when(session.hasLock()).thenReturn(true);
    when(session.getLocale()).thenAnswer(call -> locale.get());
    VaadinSession.setCurrent(session);
    ui.getInternals().setSession(session);
    messages = new MessageProperties(new CustomI18NProvider());
    user =
        UserEntity.builder()
            .firstName("Christian")
            .lastName("Doebl")
            .version(7L)
            .status(UserStatus.AVAILABLE)
            .build();
    dialog = new AccountAvatarDialog(messages, user);
    avatars = mock(UserAvatarService.class);
    when(avatars.open(any()))
        .thenAnswer(
            call ->
                new UserAvatarService.Open(UserAvatarService.State.initial(), UUID.randomUUID()));
    dialog.configure(avatars, new MockHttpServletRequest());
    ui.add(dialog);
  }

  @AfterEach
  void cleanup() {
    dialog.close();
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @SuppressWarnings("unchecked")
  private RadioButtonGroup<AvatarMode> mode() {
    return (RadioButtonGroup<AvatarMode>) ReflectionTestUtils.getField(dialog, "mode");
  }

  private AccountAvatarEditor editor() {
    return (AccountAvatarEditor) ReflectionTestUtils.getField(dialog, "editor");
  }

  private Button button(String field) {
    return (Button) ReflectionTestUtils.getField(dialog, field);
  }

  @Test
  void previewUsesSavedInitialsAndReadsNameChangesOnReopenWithoutChangingTheAccount() {
    dialog.open();
    assertEquals("CD", ((Span) ReflectionTestUtils.getField(editor(), "initials")).getText());
    assertEquals(AvatarMode.INITIALS, mode().getValue());
    assertTrue(
        editor()
            .getElement()
            .getChildren()
            .flatMap(Element::getChildren)
            .noneMatch(element -> element.getClassList().contains("cosmos-avatar-status")));
    dialog.close();
    user.setFirstName("  Änne  ");
    user.setLastName("Öster");
    dialog.open();
    assertEquals("ÄÖ", ((Span) ReflectionTestUtils.getField(editor(), "initials")).getText());
    assertEquals(7L, user.getVersion());
    assertEquals(UserStatus.AVAILABLE, user.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cancel", "close"})
  void closingDiscardsTheSelectedMode(String closeButton) {
    dialog.open();
    mode().setValue(AvatarMode.CUSTOM);
    assertEquals("CUSTOM", editor().getElement().getAttribute("data-mode"));
    button(closeButton).click();
    assertFalse(dialog.isOpened());
    assertEquals(AvatarMode.INITIALS, mode().getValue());
    dialog.open();
    assertEquals("INITIALS", editor().getElement().getAttribute("data-mode"));
    assertFalse(button("delete").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void labelsFollowLanguageChangesWithoutLosingTheSelectedMode(String language) {
    dialog.open();
    mode().setValue(AvatarMode.CUSTOM);
    locale.set(Locale.forLanguageTag(language));
    dialog.refreshTexts();
    assertEquals(AvatarMode.CUSTOM, mode().getValue());
    assertEquals(messages.getAccountAvatarMode(), mode().getAriaLabel().orElseThrow());
    assertEquals(
        messages.getAccountAvatarCustom(), mode().getItemLabelGenerator().apply(AvatarMode.CUSTOM));
    assertTrue(
        mode().getElement().getTextRecursively().contains(messages.getAccountAvatarInitials()));
    assertTrue(
        mode().getElement().getTextRecursively().contains(messages.getAccountAvatarCustom()));
    Button choose = (Button) ReflectionTestUtils.getField(editor(), "choose");
    assertEquals(messages.getAccountAvatarChoose(), choose.getAriaLabel().orElseThrow());
    assertTrue(
        choose.getElement().getTextRecursively().contains(messages.getAccountAvatarUploadHint()));
    assertEquals(messages.getAdminUsersSave(), button("save").getText());
    assertTrue(dialog.isCloseOnEsc());
    assertTrue(dialog.isCloseOnOutsideClick());
  }

  @Test
  void savePersistsTheSelectedModeClosesTheDialogAndShowsTheRealSuccessMessage() {
    dialog.open();
    JsonNode json =
        new ObjectMapper()
            .readTree("{\"local\":false,\"loading\":false,\"zoom\":1,\"x\":0,\"y\":0}");
    ReflectionTestUtils.invokeMethod(dialog, "save", json);
    ArgumentCaptor<Change> captor = ArgumentCaptor.forClass(UserAvatarService.Change.class);
    verify(avatars).save(any(), captor.capture());
    assertEquals(AvatarMode.INITIALS, captor.getValue().mode());
    assertNull(captor.getValue().imageId());
    assertFalse(dialog.isOpened());
    Notification notification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");
    assertNotNull(notification);
    assertTrue(
        notification.getElement().getTextRecursively().contains(messages.getBaseSuccessTitle()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cancellingTheNewSelectionPreservesTheSavedPictureAndKeepsTheDialogOpen(boolean hasImage) {
    AccountAvatarEditor editor = spy(editor());
    ReflectionTestUtils.setField(dialog, "editor", editor);
    doNothing().when(editor).loadSaved(any(), any(), any());
    UUID revision = hasImage ? UUID.randomUUID() : null;
    when(avatars.open(any()))
        .thenReturn(
            new UserAvatarService.Open(
                new UserAvatarService.State(7L, AvatarMode.INITIALS, hasImage, revision),
                UUID.randomUUID()));
    dialog.open();
    mode().setValue(AvatarMode.CUSTOM);
    ReflectionTestUtils.setField(dialog, "uploadedImage", UUID.randomUUID());
    button("delete").setVisible(true);
    ReflectionTestUtils.invokeMethod(dialog, "cancelSelection");
    assertNull(ReflectionTestUtils.getField(dialog, "uploadedImage"));
    assertEquals(AvatarMode.CUSTOM, mode().getValue());
    assertTrue(dialog.isOpened());
    assertEquals(hasImage, button("delete").isVisible());
    assertEquals(false, ReflectionTestUtils.getField(dialog, "deleteImage"));
    verify(avatars, never()).save(any(), any());
    if (hasImage) {
      verify(editor, times(2)).loadSaved(avatars, user.getId(), revision);
    }
  }

  @Test
  void completedUploadDoesNotChangeAReopenedDialogAndClosesItsInput() throws Exception {
    AccountAvatarEditor editor = spy(editor());
    ReflectionTestUtils.setField(dialog, "editor", editor);
    dialog.configure(avatars, new MockHttpServletRequest());
    ArgumentCaptor<UploadHandler> handler = ArgumentCaptor.forClass(UploadHandler.class);
    verify(editor).setUploadHandler(handler.capture());
    dialog.open();
    UUID token = (UUID) ReflectionTestUtils.getField(dialog, "draft");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Avatar-Draft", token.toString());
    UploadEvent event = mock(UploadEvent.class);
    VaadinServletService servletService = mock(VaadinServletService.class);
    when(event.getRequest()).thenReturn(new VaadinServletRequest(request, servletService));
    when(event.getResponse()).thenReturn(mock(VaadinResponse.class));
    ByteArrayInputStream input = spy(new ByteArrayInputStream(new byte[] {1}));
    when(event.getInputStream()).thenReturn(input);
    UI uploadUi = mock(UI.class);
    when(event.getUI()).thenReturn(uploadUi);
    doAnswer(
            call -> {
              dialog.close();
              dialog.open();
              ((Command) call.getArgument(0)).execute();
              return null;
            })
        .when(uploadUi)
        .accessSynchronously(any());
    when(avatars.upload(any(), eq(token), any())).thenReturn(UUID.randomUUID());

    handler.getValue().handleUploadRequest(event);

    verify(event).reject();
    verify(input).close();
    verify(avatars).release(token);
    assertTrue(dialog.isOpened());
    assertNotEquals(token, ReflectionTestUtils.getField(dialog, "draft"));
    assertNull(ReflectionTestUtils.getField(dialog, "uploadedImage"));
    assertEquals(AvatarMode.INITIALS, mode().getValue());
    assertFalse(button("delete").isVisible());
  }

  @Test
  void uploadResponseBindsTheAcceptedOriginalToItsServerRevisionAndRejectsOldDrafts()
      throws Exception {
    AccountAvatarEditor editor = spy(editor());
    ReflectionTestUtils.setField(dialog, "editor", editor);
    dialog.configure(avatars, new MockHttpServletRequest());
    ArgumentCaptor<UploadHandler> handler = ArgumentCaptor.forClass(UploadHandler.class);
    verify(editor).setUploadHandler(handler.capture());
    dialog.open();
    UUID token = (UUID) ReflectionTestUtils.getField(dialog, "draft");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Avatar-Draft", token.toString());
    UploadEvent event = mock(UploadEvent.class);
    VaadinResponse response = mock(VaadinResponse.class);
    UI uploadUi = mock(UI.class);
    VaadinServletService servletService = mock(VaadinServletService.class);
    when(event.getRequest()).thenReturn(new VaadinServletRequest(request, servletService));
    when(event.getResponse()).thenReturn(response);
    when(event.getUI()).thenReturn(uploadUi);
    when(event.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1}));
    doAnswer(
            call -> {
              ((Command) call.getArgument(0)).execute();
              return null;
            })
        .when(uploadUi)
        .accessSynchronously(any());
    UUID revision = UUID.randomUUID();
    when(avatars.upload(any(), eq(token), any())).thenReturn(revision);
    handler.getValue().handleUploadRequest(event);
    verify(response).setHeader("X-Avatar-Image", revision.toString());
    assertEquals(AvatarMode.CUSTOM, mode().getValue());
    assertTrue(button("delete").isVisible());
    dialog.close();
    dialog.open();
    handler.getValue().handleUploadRequest(event);
    verify(event).reject();
    verify(avatars, times(1)).upload(any(), any(), any());
  }
}
