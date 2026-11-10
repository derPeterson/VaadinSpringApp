package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadFormat;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.streams.UploadHandler;
import de.derpeterson.app.config.AppConstants;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.service.AvatarImageProcessor;
import de.derpeterson.app.service.UserAvatarService;
import java.util.UUID;
import java.util.function.Supplier;

/** Dateiupload und lokale Ausschnittvorschau; der Server prüft das Original vor dem Speichern. */
@Tag("account-avatar-editor")
@JsModule("./account-avatar-editor.js")
public class AccountAvatarEditor extends Component implements HasComponents, HasStyle {
  private static final String ARIA_HIDDEN = "aria-hidden";
  private static final String ARIA_LABEL = "aria-label";

  private final transient MessageProperties messageProperties;
  private final Span avatarInitialsLabel = new Span();
  private final Div avatarPreviewContainer = new Div();
  private final Div customAvatarContainer = new Div();
  private final Button chooseAvatarFileButton = new Button();
  private final Span chooseAvatarFileLabelSpan = new Span();
  private final Upload avatarUpload = new Upload();
  private final Image savedAvatarImage = new Image();
  private final Span avatarUploadHintSpan = new Span();
  private final Span avatarCropHintSpan = new Span();
  private final Span avatarZoomLabelSpan = new Span();
  private final Button resetAvatarCropButton = new Button();
  private final Button cancelSelection = new Button();
  private final Input zoomSliderInput = new Input();
  private final Div avatarCropFrameDiv = new Div();
  private String avatarDisplayName = "";

  public AccountAvatarEditor(MessageProperties messageProperties, UserEntity accountUserEntity) {
    this.messageProperties = messageProperties;
    addClassName("account-avatar-editor");
    getElement().setAttribute("data-mode", "INITIALS");
    getElement().setAttribute("data-has-image", "false");

    avatarInitialsLabel.addClassNames("account-avatar-initials", "cosmos-avatar-initials");

    Div previewCircleDiv = new Div(avatarInitialsLabel);
    previewCircleDiv.addClassNames("account-avatar-circle", "cosmos-avatar-circle");

    Element imagePreviewCanvasElement = createAvatarCanvasElement("preview");
    imagePreviewCanvasElement.setAttribute(ARIA_HIDDEN, "true");
    previewCircleDiv.getElement().appendChild(imagePreviewCanvasElement);

    avatarPreviewContainer.addClassNames("account-avatar-preview", "cosmos-user-avatar");
    avatarPreviewContainer.getStyle().set("--cosmos-avatar-size", "96px");
    avatarPreviewContainer.getElement().setAttribute("role", "img");
    avatarPreviewContainer.add(previewCircleDiv);

    chooseAvatarFileButton.setId("account-avatar-choose");

    SvgIcon uploadSvgIcon = AppIcon.UPLOAD.create();
    uploadSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Span uploadBadgeSpan = new Span(uploadSvgIcon);
    uploadBadgeSpan.addClassName("account-avatar-upload-icon");
    chooseAvatarFileLabelSpan.addClassName("account-avatar-upload-title");
    Span uploadContentSpan =
        new Span(uploadBadgeSpan, chooseAvatarFileLabelSpan, avatarUploadHintSpan);
    uploadContentSpan.addClassName("account-avatar-upload-content");
    chooseAvatarFileButton.getElement().appendChild(uploadContentSpan.getElement());

    avatarUpload.setId("account-avatar-upload");
    avatarUpload.addClassName("account-avatar-upload-control");
    // RAW vermeidet eine vorgelagerte Multipart-Pufferung; tatsächliche Bytes begrenzt der Handler.
    avatarUpload.setUploadFormat(UploadFormat.RAW);
    avatarUpload.setDropAllowed(true);
    avatarUpload.setAcceptedMimeTypes("image/jpeg", "image/png");
    avatarUpload.setMaxFiles(1);
    avatarUpload.setMaxFileSize(AvatarImageProcessor.MAX_UPLOAD);
    avatarUpload.addFileRejectedListener(event -> {
      Supplier<String> uploadErrorTextSupplier = messageProperties::getAccountAvatarBusy;
      if (event.getErrorMessage().equals(messageProperties.getAccountAvatarSizeError())) {
        uploadErrorTextSupplier = messageProperties::getAccountAvatarSizeError;
      } else if (event.getErrorMessage().equals(messageProperties.getAccountAvatarTypeError())) {
        uploadErrorTextSupplier = messageProperties::getAccountAvatarTypeError;
      }
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              uploadErrorTextSupplier,
              -1,
              NotificationHelper.NotificationType.ERROR);
    });

    avatarUpload.setUploadButton(chooseAvatarFileButton);
    setAvatarElementRole(avatarUpload.getElement(), "upload");
    // Eigene, ruhige Anzeige statt zusätzlicher nativer Dateiliste.
    Div uploadedFilesDiv = new Div();
    uploadedFilesDiv.getElement().setAttribute("slot", "file-list");
    avatarUpload.getElement().appendChild(uploadedFilesDiv.getElement());
    savedAvatarImage.getStyle().set("display", "none");
    Span uploadedFilenameSpan = new Span();
    uploadedFilenameSpan.addClassName("account-avatar-filename");
    setAvatarElementRole(uploadedFilenameSpan.getElement(), "filename");
    avatarUploadHintSpan.addClassName("account-avatar-helper");
    Div avatarUploadDiv = new Div(avatarUpload, uploadedFilenameSpan, savedAvatarImage);
    avatarUploadDiv.addClassName("account-avatar-upload");

    Span uploadErrorSpan = new Span();
    uploadErrorSpan.addClassName("account-avatar-error");
    uploadErrorSpan.getElement().setAttribute("role", "status");
    uploadErrorSpan.getElement().setAttribute("aria-live", "polite");
    setAvatarElementRole(uploadErrorSpan.getElement(), "error");

    avatarCropFrameDiv.addClassName("account-avatar-crop-frame");

    Element cropCanvasElement = createAvatarCanvasElement("crop");
    cropCanvasElement.setAttribute("tabindex", "0");
    cropCanvasElement.setAttribute("role", "img");
    setAvatarElementRole(avatarCropFrameDiv.getElement(), "frame");
    avatarCropFrameDiv.getElement().appendChild(cropCanvasElement);
    Div cropMaskDiv = new Div();
    cropMaskDiv.addClassName("account-avatar-crop-mask");
    cropMaskDiv.getElement().setAttribute(ARIA_HIDDEN, "true");
    avatarCropFrameDiv.add(cropMaskDiv);
    avatarCropHintSpan.addClassName("account-avatar-helper");

    zoomSliderInput.setType("range");
    zoomSliderInput.getElement().setAttribute("min", "1");
    zoomSliderInput.getElement().setAttribute("max", "3");
    zoomSliderInput.getElement().setAttribute("step", "0.01");
    zoomSliderInput.setValue("1");
    setAvatarElementRole(zoomSliderInput.getElement(), "zoom");

    Span zoomPercentSpan = new Span("100 %");
    zoomPercentSpan.addClassName("account-avatar-zoom-value");
    setAvatarElementRole(zoomPercentSpan.getElement(), "percent");
    Div zoomRowDiv = new Div(avatarZoomLabelSpan, zoomSliderInput, zoomPercentSpan);
    zoomRowDiv.addClassName("account-avatar-zoom");

    resetAvatarCropButton.setPrefixComponent(AppIcon.ROTATE_LEFT.create());
    setAvatarElementRole(resetAvatarCropButton.getElement(), "reset");
    cancelSelection.setId("account-avatar-cancel-selection");
    cancelSelection.setPrefixComponent(AppIcon.CLOSE.create());
    cancelSelection.addClassName("account-avatar-cancel-selection");
    setAvatarElementRole(cancelSelection.getElement(), "cancel");

    Div cropControlsDiv =
        new Div(
            avatarCropFrameDiv,
            avatarCropHintSpan,
            zoomRowDiv,
            resetAvatarCropButton,
            cancelSelection);
    cropControlsDiv.addClassName("account-avatar-crop-controls");

    customAvatarContainer.addClassName("account-avatar-custom");
    customAvatarContainer.add(avatarUploadDiv, uploadErrorSpan, cropControlsDiv);
    add(avatarPreviewContainer, customAvatarContainer);

    setIdentity(accountUserEntity);
    refreshTexts();
  }

  private static void setAvatarElementRole(Element targetElement, String roleName) {
    targetElement.setAttribute("data-avatar", roleName);
  }

  private static Element createAvatarCanvasElement(String canvasRole) {
    Element avatarCanvasElement = new Element("canvas");
    setAvatarElementRole(avatarCanvasElement, canvasRole);
    return avatarCanvasElement;
  }

  public void setIdentity(UserEntity accountUserEntity) {
    String firstName =
        accountUserEntity == null || accountUserEntity.getFirstName() == null
            ? ""
            : accountUserEntity.getFirstName().strip();
    String lastName =
        accountUserEntity == null || accountUserEntity.getLastName() == null
            ? ""
            : accountUserEntity.getLastName().strip();
    avatarDisplayName = (firstName + " " + lastName).strip();
    avatarInitialsLabel.removeAll();
    avatarInitialsLabel.setText(OverlayUserIcon.initialsFor(firstName, lastName));
    if (avatarInitialsLabel.getText().isEmpty()) {
      avatarInitialsLabel.add(AppIcon.USER.create());
    }
    updatePreviewLabel();
  }

  public void setMode(AvatarMode mode) {
    getElement().setAttribute("data-mode", mode.name());
  }

  public void resetDraft() {
    setMode(AvatarMode.INITIALS);
    avatarUpload.clearFileList();
    savedAvatarImage.getElement().removeAttribute("src");
    getElement().callJsFunction("resetDraft");
  }

  public void clearImage() {
    setMode(AvatarMode.INITIALS);
    getElement().callJsFunction("clearImage");
  }

  public void setUploadHandler(UploadHandler handler) {
    avatarUpload.setUploadHandler(handler);
  }

  public void setUploadEnabled(boolean enabled) {
    avatarUpload.setEnabled(enabled);
    chooseAvatarFileButton.setEnabled(enabled);
    cancelSelection.setEnabled(enabled);
  }

  public void loadSaved(
      UserAvatarService userAvatarService, Long avatarOwnerId, UUID avatarRevision) {
    savedAvatarImage.setSrc(
        AvatarImageResource.createAvatarImageDownloadHandler(
            userAvatarService, avatarOwnerId, avatarRevision, true));
    getElement().callJsFunction("loadSaved", savedAvatarImage.getElement().getAttribute("src"));
  }

  private void updatePreviewLabel() {
    avatarPreviewContainer
        .getElement()
        .setAttribute(
            ARIA_LABEL,
            avatarDisplayName.isEmpty() ? AppConstants.USER_ICON_ALT : avatarDisplayName);
  }

  public void refreshTexts() {
    chooseAvatarFileLabelSpan.setText(messageProperties.getAccountAvatarChoose());
    chooseAvatarFileButton.setAriaLabel(messageProperties.getAccountAvatarChoose());
    cancelSelection.setText(messageProperties.getAdminUsersCancel());
    cancelSelection.setAriaLabel(
        messageProperties.getAccountAvatarChoose()
            + ": "
            + messageProperties.getAdminUsersCancel());
    avatarUpload.setI18n(
        new UploadI18N()
            .setError(
                new UploadI18N.Error()
                    .setFileIsTooBig(messageProperties.getAccountAvatarSizeError())
                    .setIncorrectFileType(messageProperties.getAccountAvatarTypeError())
                    .setTooManyFiles(messageProperties.getAccountAvatarBusy())));
    avatarUploadHintSpan.setText(messageProperties.getAccountAvatarUploadHint());
    avatarCropHintSpan.setText(messageProperties.getAccountAvatarCropHint());
    avatarZoomLabelSpan.setText(messageProperties.getAccountAvatarZoom());
    zoomSliderInput.getElement().setAttribute(ARIA_LABEL, messageProperties.getAccountAvatarZoom());
    avatarCropFrameDiv
        .getElement()
        .getChild(0)
        .setAttribute(ARIA_LABEL, messageProperties.getAccountAvatarCropHint());
    resetAvatarCropButton.setText(messageProperties.getAccountAvatarResetCrop());
    getElement().setAttribute("data-error-type", messageProperties.getAccountAvatarTypeError());
    getElement().setAttribute("data-error-size", messageProperties.getAccountAvatarSizeError());
    getElement().setAttribute("data-error-image", messageProperties.getAccountAvatarImageError());
    getElement()
        .setAttribute("data-error-dimensions", messageProperties.getAccountAvatarDimensionsError());
    updatePreviewLabel();
  }
}
