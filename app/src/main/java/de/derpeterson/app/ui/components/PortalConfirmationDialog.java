package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;
import static de.derpeterson.app.ui.components.CosmosCssClasses.WARNING_BUTTON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.helper.ui.ConfirmDialogInitialFocus;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.Objects;
import java.util.function.Supplier;

/** Gemeinsame Bestätigung; nur die aufrufende Fachkomponente führt die Aktion aus. */
@StyleSheet("custom-theme/cosmos-confirmation.css")
public class PortalConfirmationDialog extends ConfirmDialog {
  public enum Variant {
    ACTION,
    UNSAVED_CHANGES
  }

  public record Prompt(
      Supplier<String> dialogTitle,
      Supplier<String> dialogDescription,
      Supplier<String> confirmationQuestion,
      Supplier<String> confirmationExplanation,
      Supplier<Component> confirmationContext,
      Supplier<String> confirmationHint,
      Supplier<String> actionLabel,
      boolean destructive,
      Supplier<Component> actionIcon) {
    public Prompt(
        Supplier<String> dialogTitle,
        Supplier<String> dialogDescription,
        Supplier<String> confirmationQuestion,
        Supplier<String> confirmationExplanation,
        Supplier<Component> confirmationContext,
        Supplier<String> confirmationHint,
        Supplier<String> actionLabel,
        boolean destructive) {
      this(
          dialogTitle,
          dialogDescription,
          confirmationQuestion,
          confirmationExplanation,
          confirmationContext,
          confirmationHint,
          actionLabel,
          destructive,
          (destructive ? AppIcon.TRASH : AppIcon.CHECK)::create);
    }
  }

  private final MessageProperties messageProperties;
  private final Variant dialogVariant;

  private final H2 dialogTitleHeading = new H2();
  private final Span dialogDescriptionSpan = new Span();
  private final Div dialogIconDiv = new Div();
  private final Button closeDialogButton = new Button(AppIcon.CLOSE_CIRCLE.create());

  private final Div confirmationLeadDiv = new Div();
  private final Span confirmationQuestionSpan = new Span();
  private final Span confirmationExplanationSpan = new Span();
  private final Span confirmationHintSpan = new Span();
  private final Div confirmationContextDiv = new Div();
  private final Div confirmationNoticeDiv = new Div();

  private final Button cancelButton = new Button();
  private final Button confirmButton = new Button();

  private transient Prompt confirmationPrompt;
  private SerializableRunnable pendingConfirmationAction;
  private SerializableRunnable pendingKeepEditingAction;

  public PortalConfirmationDialog(MessageProperties messageProperties, String dialogId) {
    this(messageProperties, dialogId, Variant.ACTION);
  }

  public PortalConfirmationDialog(
      MessageProperties messageProperties, String dialogId, Variant dialogVariant) {
    this.messageProperties = messageProperties;
    this.dialogVariant = Objects.requireNonNull(dialogVariant);
    setId(dialogId);
    addClassNames(
        "cosmos-theme",
        REFERENCE_OVERLAY,
        "portal-confirmation-dialog",
        "cosmos-native-confirmation");
    getElement().getThemeList().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    setCancelable(true);
    setCloseOnEsc(true);

    configureDialogHeader();
    configureConfirmationBody();
    configureDialogActions(dialogId);

    addConfirmListener(event -> runPendingConfirmationAction());
    addCancelListener(event -> close());
    addClosedListener(event -> {
      // opened=false wird vor ConfirmEvent übertragen. Erst Closed verwirft ohne Bestätigung.
      if (!isOpened()) {
        clearPendingActionsAndKeepDraft();
      }
    });
    if (dialogVariant == Variant.ACTION) {
      getElement()
          .addEventListener("click", event -> close())
          .setFilter(
              "event.composedPath().some(node => node instanceof HTMLElement && node.part.contains('backdrop'))");
    }
    ConfirmDialogInitialFocus.configure(
        this, dialogVariant == Variant.UNSAVED_CHANGES ? confirmButton : closeDialogButton);
  }

  private void configureDialogHeader() {
    dialogTitleHeading.addClassName(PANEL_TITLE);
    dialogDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    dialogIconDiv.addClassName(PANEL_ICON);

    Div dialogTitlesDiv = new Div(dialogTitleHeading, dialogDescriptionSpan);
    dialogTitlesDiv.addClassName(PANEL_HEADING);
    closeDialogButton.addClassName("cosmos-overlay-close");
    closeDialogButton.addClickListener(event -> close());

    Div dialogHeaderDiv = new Div(dialogIconDiv, dialogTitlesDiv, closeDialogButton);
    dialogHeaderDiv.addClassName("account-change-dialog-header");
    setHeader(new Div(dialogHeaderDiv));
  }

  private void configureConfirmationBody() {
    confirmationQuestionSpan.addClassName("portal-confirm-question");
    confirmationExplanationSpan.addClassName("portal-confirm-explanation");

    confirmationLeadDiv.add(confirmationQuestionSpan, confirmationExplanationSpan);
    confirmationLeadDiv.addClassName("portal-confirm-lead");
    confirmationContextDiv.addClassName("portal-confirm-context");
    confirmationNoticeDiv.add(AppIcon.INFO_CIRCLE.create(), confirmationHintSpan);
    confirmationNoticeDiv.addClassName("cosmos-panel-hint");

    Div confirmationBodyDiv =
        new Div(confirmationLeadDiv, confirmationContextDiv, confirmationNoticeDiv);
    confirmationBodyDiv.addClassName("portal-confirm-body");
    setText(confirmationBodyDiv);
  }

  private void configureDialogActions(String dialogId) {
    cancelButton.setId(dialogId + "-cancel");
    cancelButton.setPrefixComponent(AppIcon.CLOSE.create());
    confirmButton.setId(dialogId + "-submit");
    cancelButton.addClassName("cosmos-confirmation-action");
    confirmButton.addClassName("cosmos-confirmation-action");
    // Beim Formularschutz verwirft links; die rechte primäre Aktion behält den Entwurf.
    if (dialogVariant == Variant.UNSAVED_CHANGES) {
      setConfirmButton(cancelButton);
      setCancelButton(confirmButton);
    } else {
      setCancelButton(cancelButton);
      setConfirmButton(confirmButton);
    }
    if (dialogVariant == Variant.UNSAVED_CHANGES) {
      addClassName("cosmos-unsaved-changes-dialog");
      cancelButton.addClassName(WARNING_BUTTON);
      cancelButton.setPrefixComponent(AppIcon.FILE_X.create());
      confirmButton.addThemeVariants(ButtonVariant.PRIMARY);
      confirmButton.setPrefixComponent(AppIcon.ANGLE_RIGHT.create());
      confirmButton.getElement().setAttribute("autofocus", true);
    }

    EqualButtonWidths.configureEqualButtonWidths(this);
  }

  /** Schließen und Escape behalten den Entwurf; ausschließlich die linke Aktion verwirft ihn. */
  public void openDiscardChanges(
      SerializableRunnable discardChangesAction, SerializableRunnable keepEditingAction) {
    if (dialogVariant != Variant.UNSAVED_CHANGES) {
      throw new IllegalStateException("Unsaved changes variant required");
    }

    pendingConfirmationAction = Objects.requireNonNull(discardChangesAction);
    pendingKeepEditingAction = Objects.requireNonNull(keepEditingAction);

    dialogIconDiv.removeAll();
    dialogIconDiv.add(AppIcon.FILE_DIFF.create());
    dialogDescriptionSpan.setVisible(false);
    confirmationQuestionSpan.setVisible(false);
    confirmationContextDiv.setVisible(false);
    confirmationNoticeDiv.setVisible(false);

    refreshTexts();
    super.open();
  }

  @Override
  public void close() {
    clearPendingActionsAndKeepDraft();
    super.close();
  }

  private void clearPendingActionsAndKeepDraft() {
    SerializableRunnable keepEditingAction = pendingKeepEditingAction;
    pendingKeepEditingAction = null;
    pendingConfirmationAction = null;
    confirmationPrompt = null;
    confirmationContextDiv.removeAll();

    if (keepEditingAction != null) {
      keepEditingAction.run();
    }
  }

  public void open(Prompt confirmationPrompt, SerializableRunnable confirmationAction) {
    if (dialogVariant != Variant.ACTION) {
      throw new IllegalStateException("Action variant required");
    }

    this.confirmationPrompt = Objects.requireNonNull(confirmationPrompt);
    pendingConfirmationAction = Objects.requireNonNull(confirmationAction);

    setClassName("portal-confirm-destructive", confirmationPrompt.destructive());
    confirmButton.setClassName(DELETE_BUTTON, confirmationPrompt.destructive());
    confirmButton.removeThemeVariants(ButtonVariant.PRIMARY);
    if (!confirmationPrompt.destructive()) {
      confirmButton.addThemeVariants(ButtonVariant.PRIMARY);
    }

    dialogIconDiv.removeAll();
    dialogIconDiv.add(confirmationPrompt.actionIcon().get());
    confirmButton.setPrefixComponent(confirmationPrompt.actionIcon().get());
    confirmButton.setEnabled(true);

    refreshTexts();
    super.open();
  }

  private void runPendingConfirmationAction() {
    SerializableRunnable confirmationAction = pendingConfirmationAction;
    if (confirmationAction == null) {
      return;
    }

    pendingConfirmationAction = null;
    pendingKeepEditingAction = null;
    Button confirmationActionButton =
        dialogVariant == Variant.UNSAVED_CHANGES ? cancelButton : confirmButton;
    confirmationActionButton.setEnabled(false);

    try {
      confirmationAction.run();
    } finally {
      close();
      confirmationActionButton.setEnabled(true);
    }
  }

  public void refreshTexts() {
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    if (dialogVariant == Variant.UNSAVED_CHANGES) {
      dialogTitleHeading.setText(messageProperties.getUnsavedChangesTitle());
      setHeader(dialogTitleHeading.getText());
      confirmationExplanationSpan.setText(messageProperties.getUnsavedChangesDescription());
      cancelButton.setText(messageProperties.getUnsavedChangesDiscard());
      confirmButton.setText(messageProperties.getUnsavedChangesKeepEditing());
      return;
    }

    cancelButton.setText(messageProperties.getAdminUsersCancel());
    if (confirmationPrompt == null) {
      return;
    }

    dialogTitleHeading.setText(confirmationPrompt.dialogTitle().get());
    setHeader(dialogTitleHeading.getText());
    dialogDescriptionSpan.setText(confirmationPrompt.dialogDescription().get());
    confirmationQuestionSpan.setText(confirmationPrompt.confirmationQuestion().get());
    confirmationExplanationSpan.setText(confirmationPrompt.confirmationExplanation().get());
    confirmationLeadDiv.setVisible(
        !confirmationQuestionSpan.getText().isBlank()
            || !confirmationExplanationSpan.getText().isBlank());
    confirmationHintSpan.setText(confirmationPrompt.confirmationHint().get());
    confirmationNoticeDiv.setVisible(!confirmationHintSpan.getText().isBlank());
    confirmButton.setText(confirmationPrompt.actionLabel().get());
    confirmationContextDiv.removeAll();
    confirmationContextDiv.add(confirmationPrompt.confirmationContext().get());
  }
}
