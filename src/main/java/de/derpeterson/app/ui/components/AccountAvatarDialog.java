package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AvatarMode;
import de.derpeterson.app.service.AvatarImageProcessor;
import de.derpeterson.app.service.AvatarImageProcessor.InvalidImage;
import de.derpeterson.app.service.AvatarImageProcessor.Problem;
import de.derpeterson.app.service.UserAvatarService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import tools.jackson.databind.JsonNode;

/** Eigener Avatar: Upload und Ausschnitt bleiben bis zum Speichern ein begrenzter Entwurf. */
public class AccountAvatarDialog extends Dialog {
  private final transient MessageProperties messages;
  private final UserEntity user;
  private final H2 title = new H2();
  private final Span description = new Span();
  private final Span keepHint = new Span();
  private transient UserAvatarService avatars;
  private transient HttpServletRequest request;
  private volatile UUID draft;
  private UserAvatarService.State original = UserAvatarService.State.initial();
  private boolean deleteImage;
  private UUID uploadedImage;
  private final Button close = new Button(VaadinIcon.CLOSE_CIRCLE.create(), event -> close());
  private final Button cancel = new Button("", event -> close());
  private final Button save = new Button();
  private final Button delete = new Button();
  private final RadioButtonGroup<AvatarMode> mode = new RadioButtonGroup<>();
  private final AccountAvatarEditor editor;

  public AccountAvatarDialog(MessageProperties messages, UserEntity user) {
    this.messages = messages;
    this.user = user;
    editor = new AccountAvatarEditor(messages, user);
    setId("account-avatar-dialog");
    addClassNames("cosmos-theme", "account-change-dialog", "account-avatar-dialog");
    getThemeNames().add("cosmos");
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    title.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");
    var heading = new Div(title, description);
    heading.addClassName("cosmos-panel-heading");
    var symbol = VaadinIcon.PICTURE.create();
    symbol.getElement().setAttribute("aria-hidden", "true");
    var badge = new Div(symbol);
    badge.addClassName("cosmos-panel-icon");
    close.setId("account-avatar-close");
    close.addClassNames("cosmos-overlay-close", LumoUtility.TextColor.PRIMARY);
    close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    var header = new Div(badge, heading, close);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);

    mode.setId("account-avatar-mode");
    mode.setItems(AvatarMode.values());
    mode.setItemLabelGenerator(
        value ->
            value == AvatarMode.INITIALS
                ? messages.getAccountAvatarInitials()
                : messages.getAccountAvatarCustom());
    mode.setRenderer(new ComponentRenderer<>(this::createModeLabel));
    mode.setValue(AvatarMode.INITIALS);
    mode.addValueChangeListener(
        event -> {
          if (event.getValue() != null) editor.setMode(event.getValue());
        });
    editor
        .getElement()
        .addEventListener(
            "avatar-file-selected",
            event -> {
              if (!isOpened()) return;
              mode.setValue(AvatarMode.CUSTOM);
              deleteImage = false;
              delete.setVisible(true);
            });
    editor
        .getElement()
        .addEventListener(
            "avatar-file-cleared",
            event -> {
              mode.setValue(AvatarMode.INITIALS);
              uploadedImage = null;
              delete.setVisible(false);
            });
    editor.getElement().addEventListener("avatar-file-cancelled", event -> cancelSelection());
    editor.addComponentAtIndex(1, mode);
    var infoIcon = VaadinIcon.INFO_CIRCLE_O.create();
    infoIcon.getElement().setAttribute("aria-hidden", "true");
    var infoBadge = new Span(infoIcon);
    infoBadge.addClassName("account-avatar-info-icon");
    var info = new Div(infoBadge, keepHint);
    info.addClassNames("cosmos-panel-hint", "account-avatar-info");
    var body = new Div(editor, info);
    body.addClassNames("cosmos-dialog-form", "account-change-dialog-form", "account-avatar-form");
    add(body);

    delete.setId("account-avatar-delete");
    delete.setPrefixComponent(VaadinIcon.TRASH.create());
    delete.addClassName("cosmos-delete-button");
    delete.setVisible(false);
    delete.addClickListener(
        event -> {
          deleteImage = true;
          uploadedImage = null;
          editor.clearImage();
          mode.setValue(AvatarMode.INITIALS);
          delete.setVisible(false);
        });
    cancel.setId("account-avatar-cancel");
    cancel.setPrefixComponent(VaadinIcon.CLOSE_SMALL.create());
    save.setId("account-avatar-save");
    save.setPrefixComponent(VaadinIcon.CHECK.create());
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    save.addClickListener(event -> submit());
    var actions = new Div(cancel, save);
    actions.addClassName("cosmos-dialog-actions");
    var footer = new Div(delete, actions);
    footer.addClassName("account-avatar-footer");
    getFooter().add(footer);
    addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) clearDraft();
        });
    addDetachListener(event -> clearDraft());
    refreshTexts();
  }

  private void cancelSelection() {
    if (!isOpened()) return;
    uploadedImage = null;
    boolean restore = original.hasImage() && !deleteImage;
    delete.setVisible(restore);
    if (restore) editor.loadSaved(avatars, user.getId(), original.imageRevision());
  }

  private Span createModeLabel(AvatarMode value) {
    var symbol =
        value == AvatarMode.INITIALS ? new Span("Aa") : new Span(VaadinIcon.PICTURE.create());
    symbol.addClassName("account-avatar-mode-icon");
    symbol.getElement().setAttribute("aria-hidden", "true");
    var label = new Span(mode.getItemLabelGenerator().apply(value));
    var content = new Span(symbol, label);
    content.addClassName("account-avatar-mode-label");
    return content;
  }

  public void configure(UserAvatarService avatars, HttpServletRequest request) {
    this.avatars = avatars;
    this.request = request;
    editor.setUploadHandler(
        new UploadHandler() {
          @Override
          public long getFileSizeMax() {
            return AvatarImageProcessor.MAX_UPLOAD;
          }

          @Override
          public long getRequestSizeMax() {
            return AvatarImageProcessor.MAX_UPLOAD + 65536;
          }

          @Override
          public long getFileCountMax() {
            return 1;
          }

          @Override
          public void handleUploadRequest(UploadEvent event) throws IOException {
            UUID token;
            try {
              token = UUID.fromString(event.getRequest().getHeader("X-Avatar-Draft"));
            } catch (RuntimeException invalid) {
              event.reject();
              return;
            }
            if (!Objects.equals(token, draft)) {
              event.reject();
              return;
            }
            try (var input = event.getInputStream()) {
              UUID id = avatars.upload((HttpServletRequest) event.getRequest(), token, input);
              event.getResponse().setHeader("X-Avatar-Image", id.toString());
              event
                  .getUI()
                  .accessSynchronously(
                      () -> {
                        if (!Objects.equals(token, draft) || !isOpened()) return;
                        uploadedImage = id;
                        editor.getElement().setAttribute("data-image-id", id.toString());
                        deleteImage = false;
                        mode.setValue(AvatarMode.CUSTOM);
                        delete.setVisible(true);
                      });
              if (!Objects.equals(token, draft)) event.reject();
            } catch (RuntimeException | IOException failure) {
              logUnexpected(failure);
              event.reject();
              event
                  .getUI()
                  .access(
                      () -> {
                        if (Objects.equals(token, draft) && isOpened()) failure(failure);
                      });
            }
          }
        });
  }

  @Override
  public void open() {
    clearDraft();
    editor.setIdentity(user);
    try {
      if (avatars != null) {
        var opened = avatars.open(request);
        original = opened.state();
        draft = opened.draft();
        editor.getElement().setAttribute("data-draft-id", draft.toString());
        if (original.hasImage()) editor.loadSaved(avatars, user.getId(), original.imageRevision());
      }
      mode.setValue(original.mode());
      delete.setVisible(original.hasImage());
      super.open();
    } catch (RuntimeException failure) {
      logUnexpected(failure);
      clearDraft();
      failure(failure);
    }
  }

  private void submit() {
    if (avatars == null) {
      failure(new IllegalStateException("Avatar service required"));
      return;
    }
    busy(true);
    UUID token = draft;
    editor
        .getElement()
        .callJsFunction("getCrop")
        .then(
            data -> {
              if (!isOpened() || !Objects.equals(token, draft)) {
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
    if (data == null || !data.isObject()) throw new InvalidImage(Problem.CROP);
    if (data.path("loading").asBoolean()) throw new InvalidImage(Problem.BUSY);
    boolean local = data.path("local").asBoolean();
    UUID imageId = local ? UUID.fromString(data.path("imageId").asText()) : null;
    if (local && !Objects.equals(uploadedImage, imageId)) throw new InvalidImage(Problem.MISSING);
    var crop =
        new AvatarImageProcessor.Crop(
            data.path("zoom").asDouble(Double.NaN),
            data.path("x").asDouble(Double.NaN),
            data.path("y").asDouble(Double.NaN));
    avatars.save(
        request,
        new UserAvatarService.Change(
            original.version(), mode.getValue(), deleteImage, draft, imageId, crop));
    close();
    NotificationHelper.getInstance()
        .showNotification(
            messages::getBaseSuccessTitle,
            messages::getAccountAvatarSaved,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private void busy(boolean value) {
    save.setEnabled(!value);
    cancel.setEnabled(!value);
    close.setEnabled(!value);
    delete.setEnabled(!value);
    mode.setEnabled(!value);
    editor.setUploadEnabled(!value);
    setCloseOnEsc(!value);
    setCloseOnOutsideClick(!value);
  }

  private void failure(Throwable failure) {
    Supplier<String> message = messages::getAccountAvatarSaveFailed;
    if (failure instanceof OptimisticLockingFailureException)
      message = messages::getAccountAvatarConflict;
    if (failure instanceof InvalidImage invalid)
      message =
          switch (invalid.problem()) {
            case TYPE -> messages::getAccountAvatarTypeError;
            case SIZE -> messages::getAccountAvatarSizeError;
            case OUTPUT -> messages::getAccountAvatarOutputError;
            case IMAGE -> messages::getAccountAvatarImageError;
            case DIMENSIONS -> messages::getAccountAvatarDimensionsError;
            case CROP -> messages::getAccountAvatarCropError;
            case BUSY -> messages::getAccountAvatarBusy;
            case EXPIRED -> messages::getAccountAvatarExpired;
            case MISSING -> messages::getAccountAvatarMissing;
          };
    NotificationHelper.getInstance()
        .showNotification(
            messages::getBaseFailedTitle, message, -1, NotificationHelper.NotificationType.ERROR);
  }

  private static void logUnexpected(Throwable failure) {
    if (!(failure instanceof InvalidImage)
        && !(failure instanceof OptimisticLockingFailureException))
      LoggerFactory.getLogger(AccountAvatarDialog.class)
          .error("Unexpected failure while processing the account avatar", failure);
  }

  private void clearDraft() {
    if (avatars != null) avatars.release(draft);
    draft = null;
    uploadedImage = null;
    deleteImage = false;
    original = UserAvatarService.State.initial();
    mode.setValue(AvatarMode.INITIALS);
    editor.resetDraft();
    editor.getElement().removeAttribute("data-draft-id");
    editor.getElement().removeAttribute("data-image-id");
    delete.setVisible(false);
    busy(false);
  }

  public void refreshTexts() {
    title.setText(messages.getAccountSettingsEditPicture());
    getElement().setProperty("ariaLabel", title.getText());
    description.setText(messages.getAccountAvatarDescription());
    close.setAriaLabel(messages.getAdminUsersCloseDialog());
    mode.setAriaLabel(messages.getAccountAvatarMode());
    // Vaadin setzt die Auswahl beim Neuaufbau der Radiolabels zurück.
    var selectedMode = mode.getValue();
    mode.getDataProvider().refreshAll();
    mode.setValue(selectedMode == null ? AvatarMode.INITIALS : selectedMode);
    keepHint.setText(messages.getAccountAvatarKeepHint());
    delete.setText(messages.getAccountAvatarDelete());
    cancel.setText(messages.getAdminUsersCancel());
    save.setText(messages.getAdminUsersSave());
    editor.refreshTexts();
  }
}
