package de.derpeterson.app;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

/** Bildet die öffentlichen nativen Clientereignisse ab; Button.click() löst sie nicht aus. */
public final class ConfirmDialogTestActions {
  private ConfirmDialogTestActions() {}

  public static void click(Button actionButton) {
    Component parentComponent = actionButton.getParent().orElse(null);
    String nativeSlot = actionButton.getElement().getAttribute("slot");
    if (parentComponent instanceof ConfirmDialog confirmationDialog) {
      if ("confirm-button".equals(nativeSlot)) {
        confirmationDialog.setOpened(false);
        ComponentUtil.fireEvent(
            confirmationDialog, new ConfirmDialog.ConfirmEvent(confirmationDialog, true));
        return;
      }
      if ("cancel-button".equals(nativeSlot)) {
        confirmationDialog.setOpened(false);
        ComponentUtil.fireEvent(
            confirmationDialog, new ConfirmDialog.CancelEvent(confirmationDialog, true));
        return;
      }
    }
    actionButton.click();
  }
}
