package de.derpeterson.app.ui.components;

import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosCssClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosCssClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableRunnable;
import de.derpeterson.app.helper.ui.ConfirmDialogInitialFocus;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.i18n.MessageProperties;
import java.util.List;
import java.util.Objects;

/**
 * Native Sammelbestätigung; die aufrufende Inbox hält den Snapshot und führt die Leseaktion aus.
 */
public final class NotificationReadAllConfirmationDialog extends ConfirmDialog {
  private final MessageProperties messageProperties;

  private final H2 dialogTitleHeading = new H2();
  private final Span dialogDescriptionSpan = new Span();
  private final Button closeDialogButton = new Button(AppIcon.CLOSE_CIRCLE.create());

  private final Span confirmationQuestionSpan = new Span();
  private final Span confirmationExplanationSpan = new Span();
  private final Div confirmationContextDiv = new Div();
  private final Span confirmationHintSpan = new Span();

  private final Button cancelConfirmationButton = new Button(AppIcon.CLOSE.create());
  private final Button confirmReadAllButton = new Button(AppIcon.CHECK_CHECK.create());
  private SerializableRunnable pendingReadAllAction;
  private int affectedNotificationCount;

  public NotificationReadAllConfirmationDialog(
      MessageProperties messageProperties, String dialogId) {
    this.messageProperties = Objects.requireNonNull(messageProperties);
    setId(dialogId);
    addClassNames(
        "cosmos-theme",
        REFERENCE_OVERLAY,
        "portal-confirmation-dialog",
        "portal-notification-confirmation",
        "notification-read-all-confirmation");
    getElement().getThemeList().addAll(List.of("cosmos", "cosmos-reference"));
    setCancelable(true);
    setCloseOnEsc(true);

    configureDialogHeader();
    configureConfirmationBody();
    configureDialogActions(dialogId);
    configureDialogDismissal();
    refreshTexts();
  }

  private void configureDialogHeader() {
    dialogTitleHeading.addClassName(PANEL_TITLE);
    dialogDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    Div dialogTitlesDiv = new Div(dialogTitleHeading, dialogDescriptionSpan);
    dialogTitlesDiv.addClassName(PANEL_HEADING);

    Div dialogIconDiv = new Div(AppIcon.CHECK_CHECK.create());
    dialogIconDiv.addClassName(PANEL_ICON);
    closeDialogButton.addClassName("cosmos-overlay-close");
    closeDialogButton.addClickListener(clickEvent -> close());

    Div dialogHeaderDiv = new Div(dialogIconDiv, dialogTitlesDiv, closeDialogButton);
    dialogHeaderDiv.addClassName("account-change-dialog-header");
    // Der native Header-Slot ist display:contents; die innere Zeile behält ihre Anordnung.
    setHeader(new Div(dialogHeaderDiv));
  }

  private void configureConfirmationBody() {
    confirmationQuestionSpan.addClassName("portal-confirm-question");
    confirmationExplanationSpan.addClassName("portal-confirm-explanation");
    Div confirmationLeadDiv = new Div(confirmationQuestionSpan, confirmationExplanationSpan);
    confirmationLeadDiv.addClassName("portal-confirm-lead");

    confirmationContextDiv.addClassName("portal-confirm-context");
    Div confirmationHintDiv = new Div(AppIcon.INFO_CIRCLE.create(), confirmationHintSpan);
    confirmationHintDiv.addClassName("cosmos-panel-hint");

    Div confirmationBodyDiv =
        new Div(confirmationLeadDiv, confirmationContextDiv, confirmationHintDiv);
    confirmationBodyDiv.addClassName("portal-confirm-body");
    setText(confirmationBodyDiv);
  }

  private void configureDialogActions(String dialogId) {
    cancelConfirmationButton.setId(dialogId + "-cancel");
    confirmReadAllButton.setId(dialogId + "-submit");
    cancelConfirmationButton.addClassName("notification-read-all-action");
    confirmReadAllButton.addClassName("notification-read-all-action");
    confirmReadAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    setCancelButton(cancelConfirmationButton);
    setConfirmButton(confirmReadAllButton);
    addConfirmListener(confirmEvent -> runPendingReadAllAction());
    addCancelListener(cancelEvent -> close());
    EqualButtonWidths.configureEqualButtonWidths(this);
  }

  private void configureDialogDismissal() {
    // ConfirmDialog schließt standardmäßig nicht bei Außenklick. Der öffentliche Backdrop-Part
    // erlaubt denselben Abbruch wie bisher, ohne einen Zugriff auf interne Overlayelemente.
    getElement()
        .addEventListener("click", clickEvent -> close())
        .setFilter(
            "event.composedPath().some(node => node instanceof HTMLElement && node.part.contains('backdrop'))");

    // ConfirmDialog setzt seinen Fokus erst beim abgeschlossenen Overlay-Öffnen.
    ConfirmDialogInitialFocus.configure(this, closeDialogButton);

    // opened=false wird vor dem nativen ConfirmEvent synchronisiert. Erst der abgeschlossene
    // Schließvorgang darf eine noch nicht ausgeführte Aktion verwerfen; explizite Abbrüche
    // verwerfen sie bereits in close().
    addClosedListener(closedEvent -> {
      // Ein verspätetes ClosedEvent darf keine inzwischen erneut geöffnete Bestätigung verwerfen.
      if (!isOpened()) {
        pendingReadAllAction = null;
      }
    });
  }

  public void open(int affectedNotificationCount, SerializableRunnable readAllAction) {
    this.affectedNotificationCount = affectedNotificationCount;
    pendingReadAllAction = Objects.requireNonNull(readAllAction);
    confirmReadAllButton.setEnabled(true);
    refreshTexts();

    super.open();
  }

  private void runPendingReadAllAction() {
    SerializableRunnable readAllAction = pendingReadAllAction;
    if (readAllAction == null) {
      return;
    }
    pendingReadAllAction = null;
    confirmReadAllButton.setEnabled(false);

    try {
      readAllAction.run();
    } finally {
      close();
      confirmReadAllButton.setEnabled(true);
    }
  }

  @Override
  public void close() {
    pendingReadAllAction = null;
    super.close();
  }

  public void refreshTexts() {
    dialogTitleHeading.setText(messageProperties.getNotificationsMarkAll());
    setHeader(dialogTitleHeading.getText());
    dialogDescriptionSpan.setText(messageProperties.getPortalNotificationSelectionHint());
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());

    confirmationQuestionSpan.setText(messageProperties.getPortalMarkAllQuestion());
    confirmationExplanationSpan.setText(messageProperties.getPortalMarkAllExplanation());
    confirmationContextDiv.removeAll();
    confirmationContextDiv.add(
        new PortalContextCard(
            AppIcon.BELL.create(),
            messageProperties.getPortalNotificationAffected(affectedNotificationCount)));
    confirmationHintSpan.setText(messageProperties.getPortalNotificationSyncHint());

    cancelConfirmationButton.setText(messageProperties.getAdminUsersCancel());
    confirmReadAllButton.setText(messageProperties.getPortalConfirmAction());
  }
}
