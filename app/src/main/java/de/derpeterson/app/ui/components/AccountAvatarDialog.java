package de.derpeterson.app.ui.components;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.service.AvatarImageProcessor;
import de.derpeterson.app.service.AvatarImageProcessor.Crop;
import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import de.derpeterson.app.service.AvatarImageProcessor.Problem;
import de.derpeterson.app.service.UserAvatarService;
import de.derpeterson.app.service.UserAvatarService.Open;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.JsonNode;

/** Eigener Avatar: Upload und Ausschnitt bleiben bis zum Speichern ein begrenzter Entwurf. */
public class AccountAvatarDialog extends Dialog {
  private static final String ARIA_HIDDEN = "aria-hidden";

  private final transient MessageProperties messageProperties;
  private final UserEntity accountUserEntity;
  private final H2 dialogTitle = new H2();
  private final Span dialogDescription = new Span();
  private final Span keepHint = new Span();
  private transient UserAvatarService userAvatarService;
  private transient HttpServletRequest httpRequest;
  private volatile UUID avatarDraftId;
  private UserAvatarService.State originalAvatarState = UserAvatarService.State.initial();
  private boolean deleteImageOnSave;
  private UUID uploadedImageId;
  private final Button closeDialogButton =
      new Button(AppIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancelAvatarChangesButton = new Button("", event -> close());
  private final Button saveAvatarButton = new Button();
  private final Button deleteAvatarButton = new Button();
  private final RadioButtonGroup<AvatarMode> avatarModeSelector = new RadioButtonGroup<>();
  private final AccountAvatarEditor avatarEditor;
  private final PortalConfirmationDialog deleteConfirmation;

  public AccountAvatarDialog(MessageProperties messageProperties, UserEntity accountUserEntity) {
    this.messageProperties = messageProperties;
    this.accountUserEntity = accountUserEntity;
    deleteConfirmation =
        new PortalConfirmationDialog(messageProperties, "account-avatar-delete-confirmation");
    avatarEditor = new AccountAvatarEditor(messageProperties, accountUserEntity);
    setId("account-avatar-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-avatar-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    addClassName(REFERENCE_OVERLAY);
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    dialogTitle.addClassName(PANEL_TITLE);
    dialogDescription.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(dialogTitle, dialogDescription);
    heading.addClassName(PANEL_HEADING);
    SvgIcon symbol = AppIcon.PICTURE.create();
    symbol.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div badge = new Div(symbol);
    badge.addClassName(PANEL_ICON);
    closeDialogButton.setId("account-avatar-close");
    closeDialogButton.setIcon(AppIcon.CLOSE_CIRCLE.create());
    closeDialogButton.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    closeDialogButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    Div header = new Div(badge, heading, closeDialogButton);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);

    avatarModeSelector.setId("account-avatar-mode");
    avatarModeSelector.setItems(AvatarMode.values());
    avatarModeSelector.setItemLabelGenerator(value ->
        value == AvatarMode.INITIALS
            ? messageProperties.getAccountAvatarInitials()
            : messageProperties.getAccountAvatarCustom());
    avatarModeSelector.setRenderer(new ComponentRenderer<>(this::createModeLabel));
    avatarModeSelector.setValue(AvatarMode.INITIALS);
    avatarModeSelector.addValueChangeListener(event -> {
      if (event.getValue() != null) {
        avatarEditor.setMode(event.getValue());
      }
    });
    avatarEditor
        .getElement()
        .addEventListener(
            "avatar-file-selected",
            event -> {
              if (!isOpened()) {
                return;
              }
              avatarModeSelector.setValue(AvatarMode.CUSTOM);
              deleteImageOnSave = false;
              deleteAvatarButton.setVisible(true);
            });
    avatarEditor
        .getElement()
        .addEventListener(
            "avatar-file-cleared",
            event -> {
              avatarModeSelector.setValue(AvatarMode.INITIALS);
              uploadedImageId = null;
              deleteAvatarButton.setVisible(false);
            });
    avatarEditor.getElement().addEventListener("avatar-file-cancelled", event -> cancelSelection());
    avatarEditor.addComponentAtIndex(0, avatarModeSelector);
    SvgIcon infoIcon = AppIcon.INFO_CIRCLE.create();
    infoIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Span infoBadge = new Span(infoIcon);
    infoBadge.addClassName("account-avatar-info-icon");
    Div info = new Div(infoBadge, keepHint);
    info.addClassNames("cosmos-panel-hint", "account-avatar-info");
    Div body = new Div(avatarEditor, info);
    body.addClassNames("cosmos-dialog-form", "account-change-dialog-form", "account-avatar-form");
    add(body, deleteConfirmation);

    deleteAvatarButton.setId("account-avatar-delete");
    deleteAvatarButton.setPrefixComponent(AppIcon.TRASH.create());
    deleteAvatarButton.addClassName(DELETE_BUTTON);
    deleteAvatarButton.setVisible(false);
    deleteAvatarButton.addClickListener(event -> confirmDeleteImage());
    cancelAvatarChangesButton.setId("account-avatar-cancel");
    cancelAvatarChangesButton.setPrefixComponent(AppIcon.CLOSE.create());
    saveAvatarButton.setId("account-avatar-save");
    saveAvatarButton.setPrefixComponent(AppIcon.CHECK.create());
    saveAvatarButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveAvatarButton.addClickListener(event -> submitAvatarChanges());
    Div actions = new Div(cancelAvatarChangesButton, saveAvatarButton);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(actions);
    Div footer = new Div(deleteAvatarButton, actions);
    footer.addClassName("account-avatar-footer");
    getFooter().add(footer);
    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        clearDraft();
      }
    });
    addDetachListener(event -> clearDraft());
    refreshTexts();
  }

  private void confirmDeleteImage() {
    if (!isOpened() || !deleteAvatarButton.isVisible() || !deleteAvatarButton.isEnabled()) {
      return;
    }
    UUID selectedDraft = avatarDraftId;
    UUID selectedImage = uploadedImageId;
    deleteConfirmation.open(
        new PortalConfirmationDialog.Prompt(
            messageProperties::getAccountAvatarDelete,
            messageProperties::getPortalSessionCheckSelection,
            messageProperties::getAccountAvatarDeleteQuestion,
            messageProperties::getAccountAvatarDeleteExplanation,
            () ->
                new PortalContextCard(
                    AppIcon.PICTURE.create(), messageProperties.getAccountAvatarCustom()),
            messageProperties::getAccountAvatarDeleteHint,
            messageProperties::getPortalDeleteAction,
            true),
        () -> {
          // Ein inzwischen geschlossener oder durch einen Upload geänderter Entwurf bleibt
          // unberührt.
          if (isOpened()
              && Objects.equals(selectedDraft, avatarDraftId)
              && Objects.equals(selectedImage, uploadedImageId)) {
            deleteImageOnSave = true;
            uploadedImageId = null;
            avatarEditor.clearImage();
            avatarModeSelector.setValue(AvatarMode.INITIALS);
            deleteAvatarButton.setVisible(false);
          }
        });
  }

  private void cancelSelection() {
    if (!isOpened()) {
      return;
    }
    uploadedImageId = null;
    boolean restore = originalAvatarState.hasImage() && !deleteImageOnSave;
    deleteAvatarButton.setVisible(restore);
    if (restore) {
      avatarEditor.loadSaved(
          userAvatarService, accountUserEntity.getId(), originalAvatarState.imageRevision());
    }
  }

  private Span createModeLabel(AvatarMode value) {
    Span symbol =
        value == AvatarMode.INITIALS ? new Span("Aa") : new Span(AppIcon.PICTURE.create());
    symbol.addClassName("account-avatar-mode-icon");
    symbol.getElement().setAttribute(ARIA_HIDDEN, "true");
    Span label = new Span(avatarModeSelector.getItemLabelGenerator().apply(value));
    Span content = new Span(symbol, label);
    content.addClassName("account-avatar-mode-label");
    return content;
  }

  public void configure(UserAvatarService userAvatarService, HttpServletRequest httpRequest) {
    this.userAvatarService = userAvatarService;
    this.httpRequest = httpRequest;
    avatarEditor.setUploadHandler(
        new UploadHandler() {
          @Override
          public long getFileSizeMax() {
            return AvatarImageProcessor.MAX_UPLOAD;
          }

          @Override
          public long getRequestSizeMax() {
            return AvatarImageProcessor.MAX_UPLOAD + 65536L;
          }

          @Override
          public long getFileCountMax() {
            return 1;
          }

          @Override
          public void handleUploadRequest(UploadEvent event) throws IOException {
            receiveUpload(event, userAvatarService);
          }
        });
  }

  private void receiveUpload(UploadEvent event, UserAvatarService userAvatarService)
      throws IOException {
    UUID token;
    try {
      token = UUID.fromString(event.getRequest().getHeader("X-Avatar-Draft"));
    } catch (RuntimeException _) {
      event.reject();
      return;
    }
    if (!Objects.equals(token, avatarDraftId)) {
      event.reject();
      return;
    }
    try (InputStream input = event.getInputStream()) {
      UUID id = userAvatarService.upload((HttpServletRequest) event.getRequest(), token, input);
      event.getResponse().setHeader("X-Avatar-Image", id.toString());
      event.getUI().accessSynchronously(() -> showUploadedImage(token, id));
      if (!Objects.equals(token, avatarDraftId)) {
        event.reject();
      }
    } catch (RuntimeException | IOException failure) {
      logUnexpected(failure);
      event.reject();
      event
          .getUI()
          .access(() -> {
            if (Objects.equals(token, avatarDraftId) && isOpened()) {
              failure(failure);
            }
          });
    }
  }

  private void showUploadedImage(UUID token, UUID id) {
    if (!Objects.equals(token, avatarDraftId) || !isOpened()) {
      return;
    }
    uploadedImageId = id;
    avatarEditor.getElement().setAttribute("data-image-id", id.toString());
    deleteImageOnSave = false;
    avatarModeSelector.setValue(AvatarMode.CUSTOM);
    deleteAvatarButton.setVisible(true);
  }

  @Override
  public void open() {
    clearDraft();
    avatarEditor.setIdentity(accountUserEntity);
    try {
      if (userAvatarService != null) {
        Open opened = userAvatarService.open(httpRequest);
        originalAvatarState = opened.state();
        avatarDraftId = opened.draft();
        avatarEditor.getElement().setAttribute("data-draft-id", avatarDraftId.toString());
        if (originalAvatarState.hasImage()) {
          avatarEditor.loadSaved(
              userAvatarService, accountUserEntity.getId(), originalAvatarState.imageRevision());
        }
      }
      avatarModeSelector.setValue(originalAvatarState.mode());
      deleteAvatarButton.setVisible(originalAvatarState.hasImage());
      super.open();
    } catch (RuntimeException failure) {
      logUnexpected(failure);
      clearDraft();
      failure(failure);
    }
  }

  private void submitAvatarChanges() {
    if (userAvatarService == null) {
      failure(new IllegalStateException("Avatar service required"));
      return;
    }
    busy(true);
    UUID token = avatarDraftId;
    avatarEditor
        .getElement()
        .callJsFunction("getCrop")
        .then(
            data -> {
              if (!isOpened() || !Objects.equals(token, avatarDraftId)) {
                busy(false);
                return;
              }
              try {
                save(data);
              } catch (RuntimeException failure) {
                logUnexpected(failure);
                failure(failure);
              } finally {
                busy(false);
              }
            },
            error -> {
              busy(false);
              failure(new InvalidImage(Problem.IMAGE));
            });
  }

  private void save(JsonNode data) {
    if (data == null || !data.isObject()) {
      throw new InvalidImage(Problem.CROP);
    }
    if (data.path("loading").asBoolean()) {
      throw new InvalidImage(Problem.BUSY);
    }
    boolean local = data.path("local").asBoolean();
    UUID imageId = local ? UUID.fromString(data.path("imageId").asString()) : null;
    if (local && !Objects.equals(uploadedImageId, imageId)) {
      throw new InvalidImage(Problem.MISSING);
    }
    Crop crop =
        new AvatarImageProcessor.Crop(
            data.path("zoom").asDouble(Double.NaN),
            data.path("x").asDouble(Double.NaN),
            data.path("y").asDouble(Double.NaN));
    userAvatarService.save(
        httpRequest,
        new UserAvatarService.Change(
            originalAvatarState.version(),
            avatarModeSelector.getValue(),
            deleteImageOnSave,
            avatarDraftId,
            imageId,
            crop));
    close();
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseSuccessTitle,
            messageProperties::getAccountAvatarSaved,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private void busy(boolean value) {
    saveAvatarButton.setEnabled(!value);
    cancelAvatarChangesButton.setEnabled(!value);
    closeDialogButton.setEnabled(!value);
    deleteAvatarButton.setEnabled(!value);
    avatarModeSelector.setEnabled(!value);
    avatarEditor.setUploadEnabled(!value);
    setCloseOnEsc(!value);
    setCloseOnOutsideClick(!value);
  }

  private void failure(Throwable failure) {
    Supplier<String> message = messageProperties::getAccountAvatarSaveFailed;
    if (failure instanceof OptimisticLockingFailureException) {
      message = messageProperties::getAccountAvatarConflict;
    }
    if (failure instanceof InvalidImage invalid) {
      message =
          switch (invalid.problem()) {
            case TYPE -> messageProperties::getAccountAvatarTypeError;
            case SIZE -> messageProperties::getAccountAvatarSizeError;
            case OUTPUT -> messageProperties::getAccountAvatarOutputError;
            case IMAGE -> messageProperties::getAccountAvatarImageError;
            case DIMENSIONS -> messageProperties::getAccountAvatarDimensionsError;
            case CROP -> messageProperties::getAccountAvatarCropError;
            case BUSY -> messageProperties::getAccountAvatarBusy;
            case EXPIRED -> messageProperties::getAccountAvatarExpired;
            case MISSING -> messageProperties::getAccountAvatarMissing;
          };
    }
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            message,
            -1,
            NotificationHelper.NotificationType.ERROR);
  }

  private static void logUnexpected(Throwable failure) {
    if (!(failure instanceof InvalidImage)
        && !(failure instanceof OptimisticLockingFailureException)) {
      LoggerFactory.getLogger(AccountAvatarDialog.class)
          .error("Unexpected failure while processing the account avatar", safeFailure(failure));
    }
  }

  private void clearDraft() {
    deleteConfirmation.close();
    if (userAvatarService != null) {
      userAvatarService.release(avatarDraftId);
    }
    avatarDraftId = null;
    uploadedImageId = null;
    deleteImageOnSave = false;
    originalAvatarState = UserAvatarService.State.initial();
    avatarModeSelector.setValue(AvatarMode.INITIALS);
    avatarEditor.resetDraft();
    avatarEditor.getElement().removeAttribute("data-draft-id");
    avatarEditor.getElement().removeAttribute("data-image-id");
    deleteAvatarButton.setVisible(false);
    busy(false);
  }

  public void refreshTexts() {
    dialogTitle.setText(messageProperties.getAccountSettingsEditPicture());
    getElement().setProperty("ariaLabel", dialogTitle.getText());
    dialogDescription.setText(messageProperties.getAccountAvatarDescription());
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    avatarModeSelector.setAriaLabel(messageProperties.getAccountAvatarMode());
    // Vaadin setzt die Auswahl beim Neuaufbau der Radiolabels zurück.
    AvatarMode selectedMode = avatarModeSelector.getValue();
    avatarModeSelector.getDataProvider().refreshAll();
    avatarModeSelector.setValue(selectedMode == null ? AvatarMode.INITIALS : selectedMode);
    keepHint.setText(messageProperties.getAccountAvatarKeepHint());
    deleteAvatarButton.setText(messageProperties.getAccountAvatarDelete());
    cancelAvatarChangesButton.setText(messageProperties.getAdminUsersCancel());
    saveAvatarButton.setText(messageProperties.getAdminUsersSave());
    avatarEditor.refreshTexts();
    deleteConfirmation.refreshTexts();
  }

  // Die dauerhafte HTTP-Sitzung speichert nur Sicherheitsdaten. Dieses UI enthält
  // JPA-/Formular-/Uploadzustand und wird nach Neustart bewusst frisch aufgebaut.
  @Serial
  private void writeObject(ObjectOutputStream output) throws IOException {
    throw new NotSerializableException("Runtime UI must be rebuilt from the authenticated session");
  }

  @Serial
  private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
    throw new InvalidObjectException("Runtime UI cannot be restored from a Java object graph");
  }
}
