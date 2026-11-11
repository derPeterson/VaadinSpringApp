package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import de.derpeterson.app.i18n.MessageProperties;

/** Bestätigt das Verwerfen eines Formularentwurfs vor einer Aktion oder Navigation. */
public final class UnsavedChangesDialog {
  private UnsavedChangesDialog() {}

  public static void confirmDiscard(
      MessageProperties messageProperties, SerializableRunnable discardChangesAction) {
    openDiscardDialog(messageProperties, discardChangesAction, () -> {});
  }

  public static void confirmNavigation(
      BeforeLeaveEvent beforeLeaveEvent,
      MessageProperties messageProperties,
      SerializableRunnable discardChangesAction) {
    ContinueNavigationAction continueNavigationAction = beforeLeaveEvent.postpone();
    openDiscardDialog(
        messageProperties,
        () -> {
          discardChangesAction.run();
          continueNavigationAction.proceed();
        },
        continueNavigationAction::cancel);
  }

  private static void openDiscardDialog(
      MessageProperties messageProperties,
      SerializableRunnable discardChangesAction,
      SerializableRunnable keepEditingAction) {
    ConfirmDialog discardChangesDialog = new ConfirmDialog();
    discardChangesDialog.setId("unsaved-changes-confirmation");
    discardChangesDialog.addClassNames(
        "cosmos-theme", REFERENCE_OVERLAY, "cosmos-unsaved-changes-dialog");
    discardChangesDialog.setHeader(messageProperties.getUnsavedChangesTitle());
    discardChangesDialog.setText(messageProperties.getUnsavedChangesDescription());
    Button keepEditingButton = new Button(messageProperties.getUnsavedChangesKeepEditing());
    keepEditingButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    discardChangesDialog.setConfirmButton(keepEditingButton);
    discardChangesDialog.addConfirmListener(confirmEvent -> keepEditingAction.run());

    discardChangesDialog.setRejectable(true);
    Button discardChangesButton = new Button(messageProperties.getUnsavedChangesDiscard());
    discardChangesButton.addClassName(DELETE_BUTTON);
    discardChangesDialog.setRejectButton(discardChangesButton);
    discardChangesDialog.addRejectListener(rejectEvent -> discardChangesAction.run());
    discardChangesDialog.addCancelListener(cancelEvent -> keepEditingAction.run());

    discardChangesDialog.open();
  }
}
