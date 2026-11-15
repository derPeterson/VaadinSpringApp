package de.derpeterson.app.helper.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

/** Erhält den vorgesehenen Anfangsfokus nach der nativen Fokusvergabe des ConfirmDialog. */
public final class ConfirmDialogInitialFocus {
  private ConfirmDialogInitialFocus() {}

  public static void configure(ConfirmDialog confirmationDialog, Button initialFocusButton) {
    confirmationDialog.addAttachListener(attachEvent ->
        confirmationDialog
            .getElement()
            .executeJs(
                """
                this.updateComplete.then(() => {
                  this.__cosmosInitialFocusController?.abort();
                  const controller = new AbortController();
                  this.__cosmosInitialFocusController = controller;
                  this.shadowRoot.addEventListener('vaadin-overlay-open', () => {
                    if (this.opened) $0.focus();
                  }, {signal: controller.signal});
                });
                """,
                initialFocusButton.getElement()));
  }
}
