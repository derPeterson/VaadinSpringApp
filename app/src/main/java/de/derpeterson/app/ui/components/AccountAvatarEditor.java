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
  private final Span chooseLabel = new Span();
  private final Upload avatarUpload = new Upload();
  private final Image savedSource = new Image();
  private final Span uploadHint = new Span();
  private final Span cropHint = new Span();
  private final Span zoomLabel = new Span();
  private final Button resetAvatarCropButton = new Button();
  private final Button cancelSelection = new Button();
  private final Input zoomSliderInput = new Input();
  private final Div cropFrame = new Div();
  private String avatarDisplayName = "";

  public AccountAvatarEditor(MessageProperties messageProperties, UserEntity accountUserEntity) {
    this.messageProperties = messageProperties;
    addClassName("account-avatar-editor");
    getElement().setAttribute("data-mode", "INITIALS");
    getElement().setAttribute("data-has-image", "false");
    avatarInitialsLabel.addClassNames("account-avatar-initials", "cosmos-avatar-initials");
    Div circle = new Div(avatarInitialsLabel);
    circle.addClassNames("account-avatar-circle", "cosmos-avatar-circle");
    Element imagePreview = canvas("preview");
    imagePreview.setAttribute(ARIA_HIDDEN, "true");
    circle.getElement().appendChild(imagePreview);
    avatarPreviewContainer.addClassNames("account-avatar-preview", "cosmos-user-avatar");
    avatarPreviewContainer.getStyle().set("--cosmos-avatar-size", "96px");
    avatarPreviewContainer.getElement().setAttribute("role", "img");
    avatarPreviewContainer.add(circle);

    chooseAvatarFileButton.setId("account-avatar-choose");
    SvgIcon uploadSymbol = AppIcon.UPLOAD.create();
    uploadSymbol.getElement().setAttribute(ARIA_HIDDEN, "true");
    Span uploadBadge = new Span(uploadSymbol);
    uploadBadge.addClassName("account-avatar-upload-icon");
    chooseLabel.addClassName("account-avatar-upload-title");
    Span uploadContent = new Span(uploadBadge, chooseLabel, uploadHint);
    uploadContent.addClassName("account-avatar-upload-content");
    chooseAvatarFileButton.getElement().appendChild(uploadContent.getElement());
    avatarUpload.setId("account-avatar-upload");
    avatarUpload.addClassName("account-avatar-upload-control");
    // RAW vermeidet eine vorgelagerte Multipart-Pufferung; tatsächliche Bytes begrenzt der Handler.
    avatarUpload.setUploadFormat(UploadFormat.RAW);
    avatarUpload.setDropAllowed(true);
    avatarUpload.setAcceptedMimeTypes("image/jpeg", "image/png");
    avatarUpload.setMaxFiles(1);
    avatarUpload.setMaxFileSize(AvatarImageProcessor.MAX_UPLOAD);
    avatarUpload.addFileRejectedListener(event -> {
      Supplier<String> error = messageProperties::getAccountAvatarBusy;
      if (event.getErrorMessage().equals(messageProperties.getAccountAvatarSizeError())) {
        error = messageProperties::getAccountAvatarSizeError;
      } else if (event.getErrorMessage().equals(messageProperties.getAccountAvatarTypeError())) {
        error = messageProperties::getAccountAvatarTypeError;
      }
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              error,
              -1,
              NotificationHelper.NotificationType.ERROR);
    });
    avatarUpload.setUploadButton(chooseAvatarFileButton);
    role(avatarUpload.getElement(), "upload");
    // Eigene, ruhige Anzeige statt zusätzlicher nativer Dateiliste.
    Div fileList = new Div();
    fileList.getElement().setAttribute("slot", "file-list");
    avatarUpload.getElement().appendChild(fileList.getElement());
    savedSource.getStyle().set("display", "none");
    Span filename = new Span();
    filename.addClassName("account-avatar-filename");
    role(filename.getElement(), "filename");
    uploadHint.addClassName("account-avatar-helper");
    Div upload = new Div(avatarUpload, filename, savedSource);
    upload.addClassName("account-avatar-upload");

    Span error = new Span();
    error.addClassName("account-avatar-error");
    error.getElement().setAttribute("role", "status");
    error.getElement().setAttribute("aria-live", "polite");
    role(error.getElement(), "error");
    cropFrame.addClassName("account-avatar-crop-frame");
    Element crop = canvas("crop");
    crop.setAttribute("tabindex", "0");
    crop.setAttribute("role", "img");
    role(cropFrame.getElement(), "frame");
    cropFrame.getElement().appendChild(crop);
    Div mask = new Div();
    mask.addClassName("account-avatar-crop-mask");
    mask.getElement().setAttribute(ARIA_HIDDEN, "true");
    cropFrame.add(mask);
    cropHint.addClassName("account-avatar-helper");
    zoomSliderInput.setType("range");
    zoomSliderInput.getElement().setAttribute("min", "1");
    zoomSliderInput.getElement().setAttribute("max", "3");
    zoomSliderInput.getElement().setAttribute("step", "0.01");
    zoomSliderInput.setValue("1");
    role(zoomSliderInput.getElement(), "zoom");
    Span percent = new Span("100 %");
    percent.addClassName("account-avatar-zoom-value");
    role(percent.getElement(), "percent");
    Div zoomRow = new Div(zoomLabel, zoomSliderInput, percent);
    zoomRow.addClassName("account-avatar-zoom");
    resetAvatarCropButton.setPrefixComponent(AppIcon.ROTATE_LEFT.create());
    role(resetAvatarCropButton.getElement(), "reset");
    cancelSelection.setId("account-avatar-cancel-selection");
    cancelSelection.setPrefixComponent(AppIcon.CLOSE.create());
    cancelSelection.addClassName("account-avatar-cancel-selection");
    role(cancelSelection.getElement(), "cancel");
    Div controls = new Div(cropFrame, cropHint, zoomRow, resetAvatarCropButton, cancelSelection);
    controls.addClassName("account-avatar-crop-controls");
    customAvatarContainer.addClassName("account-avatar-custom");
    customAvatarContainer.add(upload, error, controls);
    add(avatarPreviewContainer, customAvatarContainer);
    setIdentity(accountUserEntity);
    refreshTexts();
  }

  private static void role(Element element, String value) {
    element.setAttribute("data-avatar", value);
  }

  private static Element canvas(String value) {
    Element element = new Element("canvas");
    role(element, value);
    return element;
  }

  public void setIdentity(UserEntity accountUserEntity) {
    String first =
        accountUserEntity == null || accountUserEntity.getFirstName() == null
            ? ""
            : accountUserEntity.getFirstName().strip();
    String last =
        accountUserEntity == null || accountUserEntity.getLastName() == null
            ? ""
            : accountUserEntity.getLastName().strip();
    avatarDisplayName = (first + " " + last).strip();
    avatarInitialsLabel.removeAll();
    avatarInitialsLabel.setText(OverlayUserIcon.initialsFor(first, last));
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
    savedSource.getElement().removeAttribute("src");
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

  public void loadSaved(UserAvatarService avatars, Long userId, UUID revision) {
    savedSource.setSrc(
        AvatarImageResource.createAvatarImageDownloadHandler(avatars, userId, revision, true));
    getElement().callJsFunction("loadSaved", savedSource.getElement().getAttribute("src"));
  }

  private void updatePreviewLabel() {
    avatarPreviewContainer
        .getElement()
        .setAttribute(
            ARIA_LABEL,
            avatarDisplayName.isEmpty() ? AppConstants.USER_ICON_ALT : avatarDisplayName);
  }

  public void refreshTexts() {
    chooseLabel.setText(messageProperties.getAccountAvatarChoose());
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
    uploadHint.setText(messageProperties.getAccountAvatarUploadHint());
    cropHint.setText(messageProperties.getAccountAvatarCropHint());
    zoomLabel.setText(messageProperties.getAccountAvatarZoom());
    zoomSliderInput.getElement().setAttribute(ARIA_LABEL, messageProperties.getAccountAvatarZoom());
    cropFrame
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
