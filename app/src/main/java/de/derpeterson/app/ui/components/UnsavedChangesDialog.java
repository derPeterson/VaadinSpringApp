package de.derpeterson.app.ui.components;

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
    PortalConfirmationDialog discardChangesDialog =
        new PortalConfirmationDialog(
            messageProperties,
            "unsaved-changes-confirmation",
            PortalConfirmationDialog.Variant.UNSAVED_CHANGES);
    discardChangesDialog.openDiscardChanges(discardChangesAction, keepEditingAction);
  }
}
