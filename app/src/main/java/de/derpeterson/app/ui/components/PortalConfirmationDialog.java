package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;
import static de.derpeterson.app.ui.components.CosmosCssClasses.WARNING_BUTTON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.Objects;
import java.util.function.Supplier;

/** Gemeinsame Bestätigung; nur die aufrufende Fachkomponente führt die Aktion aus. */
public class PortalConfirmationDialog extends Dialog {
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
    addClassNames("cosmos-theme", REFERENCE_OVERLAY, "portal-confirmation-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(dialogVariant == Variant.ACTION);

    configureDialogHeader();
    configureConfirmationBody();
    configureDialogActions(dialogId);

    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        clearPendingActionsAndKeepDraft();
      } else if (dialogVariant == Variant.UNSAVED_CHANGES) {
        confirmButton.focus();
      }
    });
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
    getHeader().add(dialogHeaderDiv);
  }

  private void configureConfirmationBody() {
    confirmationQuestionSpan.addClassName("portal-confirm-question");
    confirmationExplanationSpan.addClassName("portal-confirm-explanation");

    Div confirmationLeadDiv = new Div(confirmationQuestionSpan, confirmationExplanationSpan);
    confirmationLeadDiv.addClassName("portal-confirm-lead");
    confirmationContextDiv.addClassName("portal-confirm-context");
    confirmationNoticeDiv.add(AppIcon.INFO_CIRCLE.create(), confirmationHintSpan);
    confirmationNoticeDiv.addClassName("cosmos-panel-hint");

    Div confirmationBodyDiv =
        new Div(confirmationLeadDiv, confirmationContextDiv, confirmationNoticeDiv);
    confirmationBodyDiv.addClassName("portal-confirm-body");
    add(confirmationBodyDiv);
  }

  private void configureDialogActions(String dialogId) {
    cancelButton.setId(dialogId + "-cancel");
    cancelButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelButton.addClickListener(event -> {
      if (dialogVariant == Variant.UNSAVED_CHANGES) {
        runPendingConfirmationAction();
      } else {
        close();
      }
    });

    confirmButton.setId(dialogId + "-submit");
    confirmButton.addClickListener(event -> {
      if (dialogVariant == Variant.UNSAVED_CHANGES) {
        close();
      } else {
        runPendingConfirmationAction();
      }
    });

    Div confirmationActionsDiv = new Div(cancelButton, confirmButton);
    confirmationActionsDiv.addClassName("cosmos-dialog-actions");
    if (dialogVariant == Variant.UNSAVED_CHANGES) {
      addClassName("cosmos-unsaved-changes-dialog");
      cancelButton.addClassName(WARNING_BUTTON);
      cancelButton.setPrefixComponent(AppIcon.FILE_X.create());
      confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      confirmButton.setPrefixComponent(AppIcon.ANGLE_RIGHT.create());
      confirmButton.getElement().setAttribute("autofocus", true);
    }

    EqualButtonWidths.configureEqualButtonWidths(confirmationActionsDiv);
    getFooter().add(confirmationActionsDiv);
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
    confirmButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    if (!confirmationPrompt.destructive()) {
      confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
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
    confirmButton.setEnabled(false);

    try {
      confirmationAction.run();
    } finally {
      close();
      confirmButton.setEnabled(true);
    }
  }

  public void refreshTexts() {
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    if (dialogVariant == Variant.UNSAVED_CHANGES) {
      dialogTitleHeading.setText(messageProperties.getUnsavedChangesTitle());
      getElement().setProperty("ariaLabel", dialogTitleHeading.getText());
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
    getElement().setProperty("ariaLabel", dialogTitleHeading.getText());
    dialogDescriptionSpan.setText(confirmationPrompt.dialogDescription().get());
    confirmationQuestionSpan.setText(confirmationPrompt.confirmationQuestion().get());
    confirmationExplanationSpan.setText(confirmationPrompt.confirmationExplanation().get());
    confirmationHintSpan.setText(confirmationPrompt.confirmationHint().get());
    confirmationNoticeDiv.setVisible(!confirmationHintSpan.getText().isBlank());
    confirmButton.setText(confirmationPrompt.actionLabel().get());
    confirmationContextDiv.removeAll();
    confirmationContextDiv.add(confirmationPrompt.confirmationContext().get());
  }
}
