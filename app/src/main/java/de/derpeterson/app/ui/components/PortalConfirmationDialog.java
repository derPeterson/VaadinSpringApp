package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

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
  private final H2 dialogTitle = new H2();
  private final Span dialogDescription = new Span();
  private final Span confirmationQuestion = new Span();
  private final Span confirmationExplanation = new Span();
  private final Span confirmationHint = new Span();
  private final Div confirmationContext = new Div();
  private final Div dialogIconContainer = new Div();
  private final Div confirmationNotice = new Div();
  private final Button cancelButton = new Button();
  private final Button confirmButton = new Button();
  private final Button closeDialogButton = new Button(AppIcon.CLOSE_CIRCLE.create());
  private transient Prompt prompt;
  private SerializableRunnable pendingConfirmationAction;

  public PortalConfirmationDialog(MessageProperties messageProperties, String id) {
    this.messageProperties = messageProperties;
    setId(id);
    addClassNames("cosmos-theme", REFERENCE_OVERLAY, "portal-confirmation-dialog");
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    dialogTitle.addClassName(PANEL_TITLE);
    dialogDescription.addClassName(PANEL_DESCRIPTION);
    dialogIconContainer.addClassName(PANEL_ICON);
    Div titles = new Div(dialogTitle, dialogDescription);
    titles.addClassName(PANEL_HEADING);
    closeDialogButton.addClassName("cosmos-overlay-close");
    closeDialogButton.addClickListener(event -> close());
    Div header = new Div(dialogIconContainer, titles, closeDialogButton);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);
    confirmationQuestion.addClassName("portal-confirm-question");
    confirmationExplanation.addClassName("portal-confirm-explanation");
    Div lead = new Div(confirmationQuestion, confirmationExplanation);
    lead.addClassName("portal-confirm-lead");
    confirmationContext.addClassName("portal-confirm-context");
    confirmationNotice.add(AppIcon.INFO_CIRCLE.create(), confirmationHint);
    confirmationNotice.addClassName("cosmos-panel-hint");
    Div body = new Div(lead, confirmationContext, confirmationNotice);
    body.addClassName("portal-confirm-body");
    add(body);
    cancelButton.setId(id + "-cancel");
    cancelButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelButton.addClickListener(event -> close());
    confirmButton.setId(id + "-submit");
    confirmButton.addClickListener(event -> confirmPending());
    Div actions = new Div(cancelButton, confirmButton);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configure(actions);
    getFooter().add(actions);
    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        pendingConfirmationAction = null;
        prompt = null;
        confirmationContext.removeAll();
      }
    });
  }

  public void open(Prompt prompt, SerializableRunnable action) {
    this.prompt = Objects.requireNonNull(prompt);
    pendingConfirmationAction = Objects.requireNonNull(action);
    setClassName("portal-confirm-destructive", prompt.destructive());
    confirmButton.setClassName(DELETE_BUTTON, prompt.destructive());
    confirmButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    if (!prompt.destructive()) {
      confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    dialogIconContainer.removeAll();
    dialogIconContainer.add(prompt.actionIcon().get());
    confirmButton.setPrefixComponent(prompt.actionIcon().get());
    confirmButton.setEnabled(true);
    refreshTexts();
    super.open();
  }

  private void confirmPending() {
    SerializableRunnable action = pendingConfirmationAction;
    if (action == null) {
      return;
    }
    pendingConfirmationAction = null;
    confirmButton.setEnabled(false);
    try {
      action.run();
    } finally {
      close();
      confirmButton.setEnabled(true);
    }
  }

  public void refreshTexts() {
    cancelButton.setText(messageProperties.getAdminUsersCancel());
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
    if (prompt == null) {
      return;
    }
    dialogTitle.setText(prompt.dialogTitle().get());
    getElement().setProperty("ariaLabel", dialogTitle.getText());
    dialogDescription.setText(prompt.dialogDescription().get());
    confirmationQuestion.setText(prompt.confirmationQuestion().get());
    confirmationExplanation.setText(prompt.confirmationExplanation().get());
    confirmationHint.setText(prompt.confirmationHint().get());
    confirmationNotice.setVisible(!confirmationHint.getText().isBlank());
    confirmButton.setText(prompt.actionLabel().get());
    confirmationContext.removeAll();
    confirmationContext.add(prompt.confirmationContext().get());
  }
}
