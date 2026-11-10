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
import com.vaadin.flow.component.upload.Upload;
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
  private UI testUi;
  private UserEntity testUserEntity;
  private MessageProperties messageProperties;
  private AccountAvatarDialog accountAvatarDialog;
  private UserAvatarService userAvatarService;
  private final AtomicReference<Locale> localeReference = new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void setup() {
    testUi = new UI();
    UI.setCurrent(testUi);
    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(call -> localeReference.get());
    VaadinSession.setCurrent(vaadinSession);
    testUi.getInternals().setSession(vaadinSession);
    messageProperties = new MessageProperties(new CustomI18NProvider());
    testUserEntity =
        UserEntity.builder()
            .firstName("Christian")
            .lastName("Doebl")
            .version(7L)
            .status(UserStatus.AVAILABLE)
            .build();
    accountAvatarDialog = new AccountAvatarDialog(messageProperties, testUserEntity);
    userAvatarService = mock(UserAvatarService.class);
    when(userAvatarService.open(any()))
        .thenAnswer(call ->
            new UserAvatarService.Open(UserAvatarService.State.initial(), UUID.randomUUID()));
    accountAvatarDialog.configure(userAvatarService, new MockHttpServletRequest());
    testUi.add(accountAvatarDialog);
  }

  @AfterEach
  void cleanup() {
    accountAvatarDialog.close();
    NotificationHelper.getInstance().closeAndClearAllNotifications();
    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @SuppressWarnings("unchecked")
  private RadioButtonGroup<AvatarMode> getAvatarModeSelector() {
    return (RadioButtonGroup<AvatarMode>)
        ReflectionTestUtils.getField(accountAvatarDialog, "avatarModeSelector");
  }

  private AccountAvatarEditor getAccountAvatarEditor() {
    return (AccountAvatarEditor) ReflectionTestUtils.getField(accountAvatarDialog, "avatarEditor");
  }

  private Button button(String field) {
    return (Button) ReflectionTestUtils.getField(accountAvatarDialog, field);
  }

  private PortalConfirmationDialog deleteConfirmation() {
    return (PortalConfirmationDialog)
        ReflectionTestUtils.getField(accountAvatarDialog, "deleteConfirmation");
  }

  private Button confirmationButton(String field) {
    return (Button) ReflectionTestUtils.getField(deleteConfirmation(), field);
  }

  private UUID openUploadedPicture() {
    accountAvatarDialog.open();
    UUID image = UUID.randomUUID();
    ReflectionTestUtils.invokeMethod(
        accountAvatarDialog, "showUploadedImage", getAvatarDraftId(), image);
    return image;
  }

  @Test
  void droppingPicturesUsesTheNativeUploadAndIsDisabledWhileSaving() {
    Upload upload = (Upload) ReflectionTestUtils.getField(getAccountAvatarEditor(), "avatarUpload");

    assertTrue(upload.isDropAllowed());
    getAccountAvatarEditor().setUploadEnabled(false);

    assertFalse(upload.isEnabled());
    getAccountAvatarEditor().setUploadEnabled(true);

    assertTrue(upload.isEnabled());
  }

  @Test
  void cancellingDeletionLeavesPictureAndDraftUntouched() {
    UUID image = openUploadedPicture();
    button("deleteAvatarButton").click();

    assertTrue(deleteConfirmation().isOpened());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    assertEquals(image, getUploadedImageId());

    confirmationButton("cancelButton").click();

    assertTrue(accountAvatarDialog.isOpened());
    assertFalse(deleteConfirmation().isOpened());
    assertEquals(AvatarMode.CUSTOM, getAvatarModeSelector().getValue());
    assertEquals(image, getUploadedImageId());
    verify(userAvatarService, never()).save(any(), any());
  }

  @Test
  void confirmedDeletionIsOnlyPersistedWhenTheEditorIsSaved() {
    openUploadedPicture();
    button("deleteAvatarButton").click();
    confirmationButton("confirmButton").click();

    assertTrue(accountAvatarDialog.isOpened());
    assertFalse(deleteConfirmation().isOpened());
    assertEquals(true, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    assertEquals(AvatarMode.INITIALS, getAvatarModeSelector().getValue());
    assertNull(getUploadedImageId());
    verify(userAvatarService, never()).save(any(), any());

    ReflectionTestUtils.invokeMethod(
        accountAvatarDialog,
        "saveAvatarCrop",
        new ObjectMapper().readTree("{\"local\":false,\"zoom\":1,\"x\":0,\"y\":0}"));

    ArgumentCaptor<Change> change = ArgumentCaptor.forClass(Change.class);

    verify(userAvatarService).save(any(), change.capture());
    assertTrue(change.getValue().deleteImage());
    assertFalse(accountAvatarDialog.isOpened());
  }

  @Test
  void closingEditorDiscardsPendingDeletionAndCannotAffectTheNextDraft() {
    openUploadedPicture();
    button("deleteAvatarButton").click();
    accountAvatarDialog.close();

    assertFalse(deleteConfirmation().isOpened());
    UUID nextImage = openUploadedPicture();

    confirmationButton("confirmButton").click();

    assertEquals(nextImage, getUploadedImageId());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    verify(userAvatarService, never()).save(any(), any());
  }

  @Test
  void uploadCompletedAfterOpeningConfirmationIsNotDeletedByTheOldAction() {
    openUploadedPicture();
    button("deleteAvatarButton").click();
    UUID newerImage = UUID.randomUUID();
    ReflectionTestUtils.invokeMethod(
        accountAvatarDialog, "showUploadedImage", getAvatarDraftId(), newerImage);

    confirmationButton("confirmButton").click();

    assertEquals(newerImage, getUploadedImageId());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void deletionConfirmationUsesCurrentLanguage(String language) {
    openUploadedPicture();
    button("deleteAvatarButton").click();
    localeReference.set(Locale.forLanguageTag(language));

    accountAvatarDialog.refreshTexts();

    assertTrue(
        deleteConfirmation()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarDeleteQuestion()));
    assertTrue(
        deleteConfirmation()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarDeleteHint()));
    assertEquals(
        messageProperties.getPortalDeleteAction(), confirmationButton("confirmButton").getText());
  }

  @Test
  void previewUsesSavedInitialsAndReadsNameChangesOnReopenWithoutChangingTheAccount() {
    accountAvatarDialog.open();

    assertEquals("CD", getAvatarInitialsLabelSpan().getText());
    assertEquals(AvatarMode.INITIALS, getAvatarModeSelector().getValue());
    assertTrue(
        getAccountAvatarEditor()
            .getElement()
            .getChildren()
            .flatMap(Element::getChildren)
            .noneMatch(element -> element.getClassList().contains("cosmos-avatar-status")));
    accountAvatarDialog.close();
    testUserEntity.setFirstName("  Änne  ");
    testUserEntity.setLastName("Öster");
    accountAvatarDialog.open();

    assertEquals("ÄÖ", getAvatarInitialsLabelSpan().getText());
    assertEquals(7L, testUserEntity.getVersion());
    assertEquals(UserStatus.AVAILABLE, testUserEntity.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cancelAvatarChangesButton", "closeDialogButton"})
  void closingDiscardsTheSelectedMode(String closeButton) {
    accountAvatarDialog.open();
    getAvatarModeSelector().setValue(AvatarMode.CUSTOM);

    assertEquals("CUSTOM", getAccountAvatarEditor().getElement().getAttribute("data-mode"));
    button(closeButton).click();

    assertFalse(accountAvatarDialog.isOpened());
    assertEquals(AvatarMode.INITIALS, getAvatarModeSelector().getValue());
    accountAvatarDialog.open();

    assertEquals("INITIALS", getAccountAvatarEditor().getElement().getAttribute("data-mode"));
    assertFalse(button("deleteAvatarButton").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void labelsFollowLanguageChangesWithoutLosingTheSelectedMode(String language) {
    accountAvatarDialog.open();
    getAvatarModeSelector().setValue(AvatarMode.CUSTOM);
    localeReference.set(Locale.forLanguageTag(language));
    accountAvatarDialog.refreshTexts();

    assertEquals(AvatarMode.CUSTOM, getAvatarModeSelector().getValue());
    assertEquals(
        messageProperties.getAccountAvatarMode(),
        getAvatarModeSelector().getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getAccountAvatarCustom(),
        getAvatarModeSelector().getItemLabelGenerator().apply(AvatarMode.CUSTOM));
    assertTrue(
        getAvatarModeSelector()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarInitials()));
    assertTrue(
        getAvatarModeSelector()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarCustom()));
    Button choose =
        (Button) ReflectionTestUtils.getField(getAccountAvatarEditor(), "chooseAvatarFileButton");

    assertEquals(messageProperties.getAccountAvatarChoose(), choose.getAriaLabel().orElseThrow());
    assertTrue(
        choose
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarUploadHint()));
    assertEquals(messageProperties.getAdminUsersSave(), button("saveAvatarButton").getText());
    assertTrue(accountAvatarDialog.isCloseOnEsc());
    assertTrue(accountAvatarDialog.isCloseOnOutsideClick());
  }

  @Test
  void savePersistsTheSelectedModeClosesTheDialogAndShowsTheRealSuccessMessage() {
    accountAvatarDialog.open();
    JsonNode json =
        new ObjectMapper()
            .readTree("{\"local\":false,\"loading\":false,\"zoom\":1,\"x\":0,\"y\":0}");
    ReflectionTestUtils.invokeMethod(accountAvatarDialog, "saveAvatarCrop", json);
    ArgumentCaptor<Change> captor = ArgumentCaptor.forClass(UserAvatarService.Change.class);

    verify(userAvatarService).save(any(), captor.capture());
    assertEquals(AvatarMode.INITIALS, captor.getValue().mode());
    assertNull(captor.getValue().imageId());
    assertFalse(accountAvatarDialog.isOpened());
    Notification notification =
        (Notification)
            ReflectionTestUtils.getField(NotificationHelper.getInstance(), "currentNotification");

    assertNotNull(notification);
    assertTrue(
        notification
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getBaseSuccessTitle()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void cancellingTheNewSelectionPreservesTheSavedPictureAndKeepsTheDialogOpen(boolean hasImage) {
    AccountAvatarEditor editor = spy(getAccountAvatarEditor());
    ReflectionTestUtils.setField(accountAvatarDialog, "avatarEditor", editor);
    doNothing().when(editor).loadSaved(any(), any(), any());
    UUID revision = hasImage ? UUID.randomUUID() : null;
    when(userAvatarService.open(any()))
        .thenReturn(
            new UserAvatarService.Open(
                new UserAvatarService.State(7L, AvatarMode.INITIALS, hasImage, revision),
                UUID.randomUUID()));
    accountAvatarDialog.open();
    getAvatarModeSelector().setValue(AvatarMode.CUSTOM);
    ReflectionTestUtils.setField(accountAvatarDialog, "uploadedImageId", UUID.randomUUID());
    button("deleteAvatarButton").setVisible(true);
    ReflectionTestUtils.invokeMethod(accountAvatarDialog, "cancelSelection");

    assertNull(getUploadedImageId());
    assertEquals(AvatarMode.CUSTOM, getAvatarModeSelector().getValue());
    assertTrue(accountAvatarDialog.isOpened());
    assertEquals(hasImage, button("deleteAvatarButton").isVisible());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    verify(userAvatarService, never()).save(any(), any());
    if (hasImage) {
      verify(editor, times(2)).loadSaved(userAvatarService, testUserEntity.getId(), revision);
    }
  }

  @Test
  void completedUploadDoesNotChangeAReopenedDialogAndClosesItsInput() throws Exception {
    AccountAvatarEditor editor = spy(getAccountAvatarEditor());
    ReflectionTestUtils.setField(accountAvatarDialog, "avatarEditor", editor);
    accountAvatarDialog.configure(userAvatarService, new MockHttpServletRequest());
    ArgumentCaptor<UploadHandler> handler = ArgumentCaptor.forClass(UploadHandler.class);

    verify(editor).setUploadHandler(handler.capture());
    accountAvatarDialog.open();
    UUID token = getAvatarDraftId();
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    mockHttpRequest.addHeader("X-Avatar-Draft", token.toString());
    UploadEvent event = mock(UploadEvent.class);
    VaadinServletService servletService = mock(VaadinServletService.class);
    when(event.getRequest()).thenReturn(new VaadinServletRequest(mockHttpRequest, servletService));
    VaadinResponse vaadinResponse = mock(VaadinResponse.class);
    when(event.getResponse()).thenReturn(vaadinResponse);
    ByteArrayInputStream input = spy(new ByteArrayInputStream(new byte[] {1}));
    when(event.getInputStream()).thenReturn(input);
    UI uploadUi = mock(UI.class);
    when(event.getUI()).thenReturn(uploadUi);
    doAnswer(
            call -> {
              accountAvatarDialog.close();
              accountAvatarDialog.open();
              ((Command) call.getArgument(0)).execute();
              return null;
            })
        .when(uploadUi)
        .accessSynchronously(any());
    when(userAvatarService.upload(any(), eq(token), any())).thenReturn(UUID.randomUUID());

    handler.getValue().handleUploadRequest(event);

    verify(event).reject();
    verify(input).close();
    verify(userAvatarService).release(token);
    assertTrue(accountAvatarDialog.isOpened());
    assertNotEquals(token, getAvatarDraftId());
    assertNull(getUploadedImageId());
    assertEquals(AvatarMode.INITIALS, getAvatarModeSelector().getValue());
    assertFalse(button("deleteAvatarButton").isVisible());
  }

  @Test
  void uploadResponseBindsTheAcceptedOriginalToItsServerRevisionAndRejectsOldDrafts()
      throws Exception {
    AccountAvatarEditor editor = spy(getAccountAvatarEditor());
    ReflectionTestUtils.setField(accountAvatarDialog, "avatarEditor", editor);
    accountAvatarDialog.configure(userAvatarService, new MockHttpServletRequest());
    ArgumentCaptor<UploadHandler> handler = ArgumentCaptor.forClass(UploadHandler.class);

    verify(editor).setUploadHandler(handler.capture());
    accountAvatarDialog.open();
    UUID token = getAvatarDraftId();
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    mockHttpRequest.addHeader("X-Avatar-Draft", token.toString());
    UploadEvent event = mock(UploadEvent.class);
    VaadinResponse vaadinResponse = mock(VaadinResponse.class);
    UI uploadUi = mock(UI.class);
    VaadinServletService servletService = mock(VaadinServletService.class);
    when(event.getRequest()).thenReturn(new VaadinServletRequest(mockHttpRequest, servletService));
    when(event.getResponse()).thenReturn(vaadinResponse);
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
    when(userAvatarService.upload(any(), eq(token), any())).thenReturn(revision);
    handler.getValue().handleUploadRequest(event);

    verify(vaadinResponse).setHeader("X-Avatar-Image", revision.toString());
    assertEquals(AvatarMode.CUSTOM, getAvatarModeSelector().getValue());
    assertTrue(button("deleteAvatarButton").isVisible());
    accountAvatarDialog.close();
    accountAvatarDialog.open();
    handler.getValue().handleUploadRequest(event);

    verify(event).reject();
    verify(userAvatarService, times(1)).upload(any(), any(), any());
  }

  private UUID getAvatarDraftId() {
    return (UUID) ReflectionTestUtils.getField(accountAvatarDialog, "avatarDraftId");
  }

  private UUID getUploadedImageId() {
    return (UUID) ReflectionTestUtils.getField(accountAvatarDialog, "uploadedImageId");
  }

  private Span getAvatarInitialsLabelSpan() {
    return (Span) ReflectionTestUtils.getField(getAccountAvatarEditor(), "avatarInitialsLabel");
  }
}
