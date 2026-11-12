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
  private final AtomicReference<Locale> activeLocaleReference =
      new AtomicReference<>(Locale.GERMAN);

  @BeforeEach
  void createAvatarDialogEnvironment() {
    testUi = new UI();
    UI.setCurrent(testUi);

    VaadinSession vaadinSession = mock(VaadinSession.class);
    when(vaadinSession.hasLock()).thenReturn(true);
    when(vaadinSession.getLocale()).thenAnswer(mockInvocation -> activeLocaleReference.get());
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
        .thenAnswer(mockInvocation ->
            new UserAvatarService.Open(UserAvatarService.State.initial(), UUID.randomUUID()));
    accountAvatarDialog.configure(userAvatarService, new MockHttpServletRequest());
    testUi.add(accountAvatarDialog);
  }

  @AfterEach
  void clearAvatarDialogEnvironment() {
    accountAvatarDialog.close();
    NotificationHelper.getInstance().closeAndClearAllNotifications();

    UI.setCurrent(null);
    VaadinSession.setCurrent(null);
  }

  @SuppressWarnings("unchecked")
  private CosmosSegmentedTabs<AvatarMode> readAvatarModeTabs() {
    return (CosmosSegmentedTabs<AvatarMode>)
        ReflectionTestUtils.getField(accountAvatarDialog, "avatarModeTabs");
  }

  private AccountAvatarEditor readAccountAvatarEditor() {
    return (AccountAvatarEditor) ReflectionTestUtils.getField(accountAvatarDialog, "avatarEditor");
  }

  private Button readAvatarActionButton(String buttonFieldName) {
    return (Button) ReflectionTestUtils.getField(accountAvatarDialog, buttonFieldName);
  }

  private PortalConfirmationDialog readAvatarDeletionConfirmationDialog() {
    return (PortalConfirmationDialog)
        ReflectionTestUtils.getField(accountAvatarDialog, "deleteConfirmation");
  }

  private Button readDeletionConfirmationButton(String buttonFieldName) {
    return (Button)
        ReflectionTestUtils.getField(readAvatarDeletionConfirmationDialog(), buttonFieldName);
  }

  private UUID openDialogWithUploadedImage() {
    accountAvatarDialog.open();
    UUID uploadedImageUuid = UUID.randomUUID();
    ReflectionTestUtils.invokeMethod(
        accountAvatarDialog, "showUploadedImage", readAvatarDraftUuid(), uploadedImageUuid);

    return uploadedImageUuid;
  }

  @Test
  void droppingPicturesUsesTheNativeUploadAndIsDisabledWhileSaving() {
    Upload avatarUpload =
        (Upload) ReflectionTestUtils.getField(readAccountAvatarEditor(), "avatarUpload");

    assertTrue(avatarUpload.isDropAllowed());

    readAccountAvatarEditor().setUploadEnabled(false);

    assertFalse(avatarUpload.isEnabled());

    readAccountAvatarEditor().setUploadEnabled(true);

    assertTrue(avatarUpload.isEnabled());
  }

  // Löschbestätigung und Entwurfswechsel
  @Test
  void cancellingDeletionLeavesPictureAndDraftUntouched() {
    UUID uploadedImageUuid = openDialogWithUploadedImage();
    readAvatarActionButton("deleteAvatarButton").click();

    assertTrue(readAvatarDeletionConfirmationDialog().isOpened());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    assertEquals(uploadedImageUuid, readUploadedImageUuid());

    readDeletionConfirmationButton("cancelButton").click();

    assertTrue(accountAvatarDialog.isOpened());
    assertFalse(readAvatarDeletionConfirmationDialog().isOpened());
    assertEquals(AvatarMode.CUSTOM, readAvatarModeTabs().getValue());
    assertEquals(uploadedImageUuid, readUploadedImageUuid());
    verify(userAvatarService, never()).save(any(), any());
  }

  @Test
  void confirmedDeletionIsOnlyPersistedWhenTheEditorIsSaved() {
    openDialogWithUploadedImage();
    readAvatarActionButton("deleteAvatarButton").click();
    readDeletionConfirmationButton("confirmButton").click();

    assertTrue(accountAvatarDialog.isOpened());
    assertFalse(readAvatarDeletionConfirmationDialog().isOpened());
    assertEquals(true, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    assertEquals(AvatarMode.INITIALS, readAvatarModeTabs().getValue());
    assertNull(readUploadedImageUuid());
    verify(userAvatarService, never()).save(any(), any());

    ReflectionTestUtils.invokeMethod(
        accountAvatarDialog,
        "saveAvatarCrop",
        new ObjectMapper().readTree("{\"local\":false,\"zoom\":1,\"x\":0,\"y\":0}"));

    ArgumentCaptor<Change> avatarChangeCaptor = ArgumentCaptor.forClass(Change.class);

    verify(userAvatarService).save(any(), avatarChangeCaptor.capture());
    assertTrue(avatarChangeCaptor.getValue().deleteImage());
    assertFalse(accountAvatarDialog.isOpened());
  }

  @Test
  void closingEditorDiscardsPendingDeletionAndCannotAffectTheNextDraft() {
    openDialogWithUploadedImage();
    readAvatarActionButton("deleteAvatarButton").click();
    accountAvatarDialog.close();

    assertFalse(readAvatarDeletionConfirmationDialog().isOpened());
    UUID nextUploadedImageUuid = openDialogWithUploadedImage();

    readDeletionConfirmationButton("confirmButton").click();

    assertEquals(nextUploadedImageUuid, readUploadedImageUuid());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    verify(userAvatarService, never()).save(any(), any());
  }

  @Test
  void uploadCompletedAfterOpeningConfirmationIsNotDeletedByTheOldAction() {
    openDialogWithUploadedImage();
    readAvatarActionButton("deleteAvatarButton").click();
    UUID newerUploadedImageUuid = UUID.randomUUID();
    ReflectionTestUtils.invokeMethod(
        accountAvatarDialog, "showUploadedImage", readAvatarDraftUuid(), newerUploadedImageUuid);

    readDeletionConfirmationButton("confirmButton").click();

    assertEquals(newerUploadedImageUuid, readUploadedImageUuid());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void deletionConfirmationUsesCurrentLanguage(String languageTag) {
    openDialogWithUploadedImage();
    readAvatarActionButton("deleteAvatarButton").click();
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));

    accountAvatarDialog.refreshTexts();

    assertTrue(
        readAvatarDeletionConfirmationDialog()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarDeleteQuestion()));
    assertTrue(
        readAvatarDeletionConfirmationDialog()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarDeleteHint()));
    assertEquals(
        messageProperties.getPortalDeleteAction(),
        readDeletionConfirmationButton("confirmButton").getText());
  }

  // Vorschau, Auswahl und Speichern
  @Test
  void previewUsesSavedInitialsAndReadsNameChangesOnReopenWithoutChangingTheAccount() {
    accountAvatarDialog.open();

    assertEquals("CD", readAvatarInitialsSpan().getText());
    assertEquals(AvatarMode.INITIALS, readAvatarModeTabs().getValue());
    assertTrue(
        readAccountAvatarEditor()
            .getElement()
            .getChildren()
            .flatMap(Element::getChildren)
            .noneMatch(
                childElement -> childElement.getClassList().contains("cosmos-avatar-status")));

    accountAvatarDialog.close();
    testUserEntity.setFirstName("  Änne  ");
    testUserEntity.setLastName("Öster");
    accountAvatarDialog.open();

    assertEquals("ÄÖ", readAvatarInitialsSpan().getText());
    assertEquals(7L, testUserEntity.getVersion());
    assertEquals(UserStatus.AVAILABLE, testUserEntity.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"cancelAvatarChangesButton", "closeDialogButton"})
  void closingDiscardsTheSelectedMode(String closeButtonFieldName) {
    accountAvatarDialog.open();
    readAvatarModeTabs().setValue(AvatarMode.CUSTOM);

    assertEquals("CUSTOM", readAccountAvatarEditor().getElement().getAttribute("data-mode"));

    readAvatarActionButton(closeButtonFieldName).click();

    assertFalse(accountAvatarDialog.isOpened());
    assertEquals(AvatarMode.INITIALS, readAvatarModeTabs().getValue());
    accountAvatarDialog.open();

    assertEquals("INITIALS", readAccountAvatarEditor().getElement().getAttribute("data-mode"));
    assertFalse(readAvatarActionButton("deleteAvatarButton").isVisible());
  }

  @ParameterizedTest
  @ValueSource(strings = {"de", "en"})
  void labelsFollowLanguageChangesWithoutLosingTheSelectedMode(String languageTag) {
    accountAvatarDialog.open();
    readAvatarModeTabs().setValue(AvatarMode.CUSTOM);
    activeLocaleReference.set(Locale.forLanguageTag(languageTag));
    accountAvatarDialog.refreshTexts();

    assertEquals(AvatarMode.CUSTOM, readAvatarModeTabs().getValue());
    assertEquals(
        messageProperties.getAccountAvatarMode(),
        readAvatarModeTabs().getAriaLabel().orElseThrow());
    assertEquals(
        messageProperties.getAccountAvatarCustom(),
        readAvatarModeTabs().getItemLabelGenerator().apply(AvatarMode.CUSTOM));
    assertTrue(
        readAvatarModeTabs()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarInitials()));
    assertTrue(
        readAvatarModeTabs()
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarCustom()));
    Button chooseAvatarFileButton =
        (Button) ReflectionTestUtils.getField(readAccountAvatarEditor(), "chooseAvatarFileButton");

    assertEquals(
        messageProperties.getAccountAvatarChoose(),
        chooseAvatarFileButton.getAriaLabel().orElseThrow());
    assertTrue(
        chooseAvatarFileButton
            .getElement()
            .getTextRecursively()
            .contains(messageProperties.getAccountAvatarUploadHint()));
    assertEquals(
        messageProperties.getAdminUsersSave(),
        readAvatarActionButton("saveAvatarButton").getText());
    assertTrue(accountAvatarDialog.isCloseOnEsc());
    assertTrue(accountAvatarDialog.isCloseOnOutsideClick());
  }

  @Test
  void savePersistsTheSelectedModeClosesTheDialogAndShowsTheRealSuccessMessage() {
    accountAvatarDialog.open();
    JsonNode cropJsonNode =
        new ObjectMapper()
            .readTree("{\"local\":false,\"loading\":false,\"zoom\":1,\"x\":0,\"y\":0}");
    ReflectionTestUtils.invokeMethod(accountAvatarDialog, "saveAvatarCrop", cropJsonNode);
    ArgumentCaptor<Change> avatarChangeCaptor =
        ArgumentCaptor.forClass(UserAvatarService.Change.class);

    verify(userAvatarService).save(any(), avatarChangeCaptor.capture());
    assertEquals(AvatarMode.INITIALS, avatarChangeCaptor.getValue().mode());
    assertNull(avatarChangeCaptor.getValue().imageId());
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
  void cancellingTheNewSelectionPreservesTheSavedPictureAndKeepsTheDialogOpen(
      boolean hasSavedImage) {
    AccountAvatarEditor avatarEditor = spy(readAccountAvatarEditor());
    ReflectionTestUtils.setField(accountAvatarDialog, "avatarEditor", avatarEditor);
    doNothing().when(avatarEditor).loadSaved(any(), any(), any());
    UUID avatarRevisionUuid = hasSavedImage ? UUID.randomUUID() : null;
    when(userAvatarService.open(any()))
        .thenReturn(
            new UserAvatarService.Open(
                new UserAvatarService.State(
                    7L, AvatarMode.INITIALS, hasSavedImage, avatarRevisionUuid),
                UUID.randomUUID()));
    accountAvatarDialog.open();
    readAvatarModeTabs().setValue(AvatarMode.CUSTOM);
    ReflectionTestUtils.setField(accountAvatarDialog, "uploadedImageId", UUID.randomUUID());
    readAvatarActionButton("deleteAvatarButton").setVisible(true);
    ReflectionTestUtils.invokeMethod(accountAvatarDialog, "cancelSelection");

    assertNull(readUploadedImageUuid());
    assertEquals(AvatarMode.CUSTOM, readAvatarModeTabs().getValue());
    assertTrue(accountAvatarDialog.isOpened());
    assertEquals(hasSavedImage, readAvatarActionButton("deleteAvatarButton").isVisible());
    assertEquals(false, ReflectionTestUtils.getField(accountAvatarDialog, "deleteImageOnSave"));
    verify(userAvatarService, never()).save(any(), any());
    if (hasSavedImage) {
      verify(avatarEditor, times(2))
          .loadSaved(userAvatarService, testUserEntity.getId(), avatarRevisionUuid);
    }
  }

  // Upload und Bindung an den aktiven Entwurf
  @Test
  void completedUploadDoesNotChangeAReopenedDialogAndClosesItsInput() throws Exception {
    UploadHandler avatarUploadHandler = captureConfiguredAvatarUploadHandler();

    accountAvatarDialog.open();
    UUID avatarDraftUuid = readAvatarDraftUuid();
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    mockHttpRequest.addHeader("X-Avatar-Draft", avatarDraftUuid.toString());
    UploadEvent uploadEvent = mock(UploadEvent.class);
    VaadinServletService vaadinServletService = mock(VaadinServletService.class);
    when(uploadEvent.getRequest())
        .thenReturn(new VaadinServletRequest(mockHttpRequest, vaadinServletService));
    VaadinResponse vaadinResponse = mock(VaadinResponse.class);
    when(uploadEvent.getResponse()).thenReturn(vaadinResponse);
    ByteArrayInputStream uploadInputStream = spy(new ByteArrayInputStream(new byte[] {1}));
    when(uploadEvent.getInputStream()).thenReturn(uploadInputStream);
    UI uploadUi = mock(UI.class);
    when(uploadEvent.getUI()).thenReturn(uploadUi);
    doAnswer(
            mockInvocation -> {
              accountAvatarDialog.close();
              accountAvatarDialog.open();
              ((Command) mockInvocation.getArgument(0)).execute();
              return null;
            })
        .when(uploadUi)
        .accessSynchronously(any());
    when(userAvatarService.upload(any(), eq(avatarDraftUuid), any())).thenReturn(UUID.randomUUID());

    avatarUploadHandler.handleUploadRequest(uploadEvent);

    verify(uploadEvent).reject();
    verify(uploadInputStream).close();
    verify(userAvatarService).release(avatarDraftUuid);
    assertTrue(accountAvatarDialog.isOpened());
    assertNotEquals(avatarDraftUuid, readAvatarDraftUuid());
    assertNull(readUploadedImageUuid());
    assertEquals(AvatarMode.INITIALS, readAvatarModeTabs().getValue());
    assertFalse(readAvatarActionButton("deleteAvatarButton").isVisible());
  }

  @Test
  void uploadResponseBindsTheAcceptedOriginalToItsServerRevisionAndRejectsOldDrafts()
      throws Exception {
    UploadHandler avatarUploadHandler = captureConfiguredAvatarUploadHandler();

    accountAvatarDialog.open();
    UUID avatarDraftUuid = readAvatarDraftUuid();
    MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
    mockHttpRequest.addHeader("X-Avatar-Draft", avatarDraftUuid.toString());
    UploadEvent uploadEvent = mock(UploadEvent.class);
    VaadinResponse vaadinResponse = mock(VaadinResponse.class);
    UI uploadUi = mock(UI.class);
    VaadinServletService vaadinServletService = mock(VaadinServletService.class);
    when(uploadEvent.getRequest())
        .thenReturn(new VaadinServletRequest(mockHttpRequest, vaadinServletService));
    when(uploadEvent.getResponse()).thenReturn(vaadinResponse);
    when(uploadEvent.getUI()).thenReturn(uploadUi);
    when(uploadEvent.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1}));
    doAnswer(
            mockInvocation -> {
              ((Command) mockInvocation.getArgument(0)).execute();
              return null;
            })
        .when(uploadUi)
        .accessSynchronously(any());
    UUID avatarRevisionUuid = UUID.randomUUID();
    when(userAvatarService.upload(any(), eq(avatarDraftUuid), any()))
        .thenReturn(avatarRevisionUuid);
    avatarUploadHandler.handleUploadRequest(uploadEvent);

    verify(vaadinResponse).setHeader("X-Avatar-Image", avatarRevisionUuid.toString());
    assertEquals(AvatarMode.CUSTOM, readAvatarModeTabs().getValue());
    assertTrue(readAvatarActionButton("deleteAvatarButton").isVisible());

    accountAvatarDialog.close();
    accountAvatarDialog.open();
    avatarUploadHandler.handleUploadRequest(uploadEvent);

    verify(uploadEvent).reject();
    verify(userAvatarService, times(1)).upload(any(), any(), any());
  }

  private UploadHandler captureConfiguredAvatarUploadHandler() {
    AccountAvatarEditor avatarEditor = spy(readAccountAvatarEditor());
    ReflectionTestUtils.setField(accountAvatarDialog, "avatarEditor", avatarEditor);
    accountAvatarDialog.configure(userAvatarService, new MockHttpServletRequest());
    ArgumentCaptor<UploadHandler> uploadHandlerCaptor =
        ArgumentCaptor.forClass(UploadHandler.class);

    verify(avatarEditor).setUploadHandler(uploadHandlerCaptor.capture());

    return uploadHandlerCaptor.getValue();
  }

  private UUID readAvatarDraftUuid() {
    return (UUID) ReflectionTestUtils.getField(accountAvatarDialog, "avatarDraftId");
  }

  private UUID readUploadedImageUuid() {
    return (UUID) ReflectionTestUtils.getField(accountAvatarDialog, "uploadedImageId");
  }

  private Span readAvatarInitialsSpan() {
    return (Span) ReflectionTestUtils.getField(readAccountAvatarEditor(), "avatarInitialsLabel");
  }
}
