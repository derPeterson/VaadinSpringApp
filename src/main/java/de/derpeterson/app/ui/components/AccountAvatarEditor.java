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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
  private final transient MessageProperties messages;
  private final Span initials = new Span();
  private final Div preview = new Div();
  private final Div custom = new Div();
  private final Button choose = new Button();
  private final Span chooseLabel = new Span();
  private final Upload uploader = new Upload();
  private final Image savedSource = new Image();
  private final Span uploadHint = new Span();
  private final Span cropHint = new Span();
  private final Span zoomLabel = new Span();
  private final Button reset = new Button();
  private final Button cancelSelection = new Button();
  private final Input zoom = new Input();
  private final Div cropFrame = new Div();
  private String name = "";

  public AccountAvatarEditor(MessageProperties messages, UserEntity user) {
    this.messages = messages;
    addClassName("account-avatar-editor");
    getElement().setAttribute("data-mode", "INITIALS");
    getElement().setAttribute("data-has-image", "false");
    initials.addClassNames("account-avatar-initials", "cosmos-avatar-initials");
    Div circle = new Div(initials);
    circle.addClassNames("account-avatar-circle", "cosmos-avatar-circle");
    Element imagePreview = canvas("preview");
    imagePreview.setAttribute("aria-hidden", "true");
    circle.getElement().appendChild(imagePreview);
    preview.addClassNames("account-avatar-preview", "cosmos-user-avatar");
    preview.getStyle().set("--cosmos-avatar-size", "96px");
    preview.getElement().setAttribute("role", "img");
    preview.add(circle);

    choose.setId("account-avatar-choose");
    Icon uploadSymbol = VaadinIcon.UPLOAD.create();
    uploadSymbol.getElement().setAttribute("aria-hidden", "true");
    Span uploadBadge = new Span(uploadSymbol);
    uploadBadge.addClassName("account-avatar-upload-icon");
    chooseLabel.addClassName("account-avatar-upload-title");
    Span uploadContent = new Span(uploadBadge, chooseLabel, uploadHint);
    uploadContent.addClassName("account-avatar-upload-content");
    choose.getElement().appendChild(uploadContent.getElement());
    uploader.setId("account-avatar-upload");
    uploader.addClassName("account-avatar-upload-control");
    // RAW vermeidet eine vorgelagerte Multipart-Pufferung; tatsächliche Bytes begrenzt der Handler.
    uploader.setUploadFormat(UploadFormat.RAW);
    uploader.setDropAllowed(false);
    uploader.setAcceptedFileTypes("image/jpeg", "image/png");
    uploader.setMaxFiles(1);
    uploader.setMaxFileSize(AvatarImageProcessor.MAX_UPLOAD);
    uploader.addFileRejectedListener(
        event -> {
          Supplier<String> error = messages::getAccountAvatarBusy;
          if (event.getErrorMessage().equals(messages.getAccountAvatarSizeError())) {
            error = messages::getAccountAvatarSizeError;
          } else if (event.getErrorMessage().equals(messages.getAccountAvatarTypeError())) {
            error = messages::getAccountAvatarTypeError;
          }
          NotificationHelper.getInstance()
              .showNotification(
                  messages::getBaseFailedTitle,
                  error,
                  -1,
                  NotificationHelper.NotificationType.ERROR);
        });
    uploader.setUploadButton(choose);
    role(uploader.getElement(), "upload");
    // Eigene, ruhige Anzeige statt zusätzlicher nativer Dateiliste.
    Div fileList = new Div();
    fileList.getElement().setAttribute("slot", "file-list");
    uploader.getElement().appendChild(fileList.getElement());
    savedSource.getStyle().set("display", "none");
    Span filename = new Span();
    filename.addClassName("account-avatar-filename");
    role(filename.getElement(), "filename");
    uploadHint.addClassName("account-avatar-helper");
    Div upload = new Div(uploader, filename, savedSource);
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
    mask.getElement().setAttribute("aria-hidden", "true");
    cropFrame.add(mask);
    cropHint.addClassName("account-avatar-helper");
    zoom.setType("range");
    zoom.getElement().setAttribute("min", "1");
    zoom.getElement().setAttribute("max", "3");
    zoom.getElement().setAttribute("step", "0.01");
    zoom.setValue("1");
    role(zoom.getElement(), "zoom");
    Span percent = new Span("100 %");
    percent.addClassName("account-avatar-zoom-value");
    role(percent.getElement(), "percent");
    Div zoomRow = new Div(zoomLabel, zoom, percent);
    zoomRow.addClassName("account-avatar-zoom");
    reset.setPrefixComponent(VaadinIcon.ROTATE_LEFT.create());
    role(reset.getElement(), "reset");
    cancelSelection.setId("account-avatar-cancel-selection");
    cancelSelection.setPrefixComponent(VaadinIcon.CLOSE_SMALL.create());
    cancelSelection.addClassName("account-avatar-cancel-selection");
    role(cancelSelection.getElement(), "cancel");
    Div controls = new Div(cropFrame, cropHint, zoomRow, reset, cancelSelection);
    controls.addClassName("account-avatar-crop-controls");
    custom.addClassName("account-avatar-custom");
    custom.add(upload, error, controls);
    add(preview, custom);
    setIdentity(user);
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

  public void setIdentity(UserEntity user) {
    String first = user == null || user.getFirstName() == null ? "" : user.getFirstName().strip();
    String last = user == null || user.getLastName() == null ? "" : user.getLastName().strip();
    name = (first + " " + last).strip();
    initials.removeAll();
    initials.setText(OverlayUserIcon.initialsFor(first, last));
    if (initials.getText().isEmpty()) {
      initials.add(VaadinIcon.USER.create());
    }
    updatePreviewLabel();
  }

  public void setMode(AvatarMode mode) {
    getElement().setAttribute("data-mode", mode.name());
  }

  public void resetDraft() {
    setMode(AvatarMode.INITIALS);
    uploader.clearFileList();
    savedSource.getElement().removeAttribute("src");
    getElement().callJsFunction("resetDraft");
  }

  public void clearImage() {
    setMode(AvatarMode.INITIALS);
    getElement().callJsFunction("clearImage");
  }

  public void setUploadHandler(UploadHandler handler) {
    uploader.setUploadHandler(handler);
  }

  public void setUploadEnabled(boolean enabled) {
    choose.setEnabled(enabled);
    cancelSelection.setEnabled(enabled);
  }

  public void loadSaved(UserAvatarService avatars, Long userId, UUID revision) {
    savedSource.setSrc(AvatarImageResource.handler(avatars, userId, revision, true));
    getElement().callJsFunction("loadSaved", savedSource.getElement().getAttribute("src"));
  }

  private void updatePreviewLabel() {
    preview
        .getElement()
        .setAttribute("aria-label", name.isEmpty() ? AppConstants.USER_ICON_ALT : name);
  }

  public void refreshTexts() {
    chooseLabel.setText(messages.getAccountAvatarChoose());
    choose.setAriaLabel(messages.getAccountAvatarChoose());
    cancelSelection.setText(messages.getAdminUsersCancel());
    cancelSelection.setAriaLabel(
        messages.getAccountAvatarChoose() + ": " + messages.getAdminUsersCancel());
    uploader.setI18n(
        new UploadI18N()
            .setError(
                new UploadI18N.Error()
                    .setFileIsTooBig(messages.getAccountAvatarSizeError())
                    .setIncorrectFileType(messages.getAccountAvatarTypeError())
                    .setTooManyFiles(messages.getAccountAvatarBusy())));
    uploadHint.setText(messages.getAccountAvatarUploadHint());
    cropHint.setText(messages.getAccountAvatarCropHint());
    zoomLabel.setText(messages.getAccountAvatarZoom());
    zoom.getElement().setAttribute("aria-label", messages.getAccountAvatarZoom());
    cropFrame
        .getElement()
        .getChild(0)
        .setAttribute("aria-label", messages.getAccountAvatarCropHint());
    reset.setText(messages.getAccountAvatarResetCrop());
    getElement().setAttribute("data-error-type", messages.getAccountAvatarTypeError());
    getElement().setAttribute("data-error-size", messages.getAccountAvatarSizeError());
    getElement().setAttribute("data-error-image", messages.getAccountAvatarImageError());
    getElement().setAttribute("data-error-dimensions", messages.getAccountAvatarDimensionsError());
    updatePreviewLabel();
  }
}
