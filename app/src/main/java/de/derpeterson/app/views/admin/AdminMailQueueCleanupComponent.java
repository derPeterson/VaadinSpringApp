package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.derpeterson.app.helper.logging.LogSafety;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.NotificationHelper.NotificationType;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.EmailQueueCleanupService;
import de.derpeterson.app.service.EmailQueueCleanupService.CleanupResult;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCssClasses;
import de.derpeterson.app.ui.components.PortalConfirmationDialog;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sofortige Verwaltungsaktion, unabhängig vom Speichern des Konfigurationsentwurfs. */
public final class AdminMailQueueCleanupComponent extends VerticalLayout {
  private static final Logger logger =
      LoggerFactory.getLogger(AdminMailQueueCleanupComponent.class);
  private final MessageProperties messageProperties;
  private final transient EmailQueueCleanupService emailQueueCleanupService;

  private final H3 queueTitleHeading = new H3();
  private final Span warningTextSpan = new Span();
  private final Span cleanupResultSpan = new Span();
  private CleanupResult latestCleanupResult;

  private final Button refreshResultButton = new Button(AppIcon.REFRESH.create());
  private final Button runCleanupButton = new Button(AppIcon.CLOSE.create());
  private final PortalConfirmationDialog cleanupConfirmationDialog;

  public AdminMailQueueCleanupComponent(
      MessageProperties messageProperties, EmailQueueCleanupService emailQueueCleanupService) {
    this.messageProperties = messageProperties;
    this.emailQueueCleanupService = emailQueueCleanupService;
    setId("admin-mail-queue-resolution");
    addClassName("admin-mail-queue-resolution");
    setWidthFull();
    setPadding(false);
    queueTitleHeading.addClassName(CosmosCssClasses.PANEL_TITLE);

    HorizontalLayout warningLayout =
        new HorizontalLayout(AppIcon.WARNING.create(), warningTextSpan);
    warningLayout.setPadding(false);
    warningLayout.setSpacing(false);
    warningLayout.setAlignItems(Alignment.CENTER);
    warningLayout.addClassName("cosmos-security-warning");

    cleanupResultSpan.setId("admin-mail-queue-result");
    cleanupResultSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    refreshResultButton.setId("admin-mail-queue-refresh");
    refreshResultButton.addClickListener(event -> loadLatestCleanupResult());
    runCleanupButton.setId("admin-mail-queue-cleanup");
    runCleanupButton.addClassName(CosmosCssClasses.DELETE_BUTTON);
    runCleanupButton.addClickListener(event -> openCleanupConfirmationDialog());
    HorizontalLayout cleanupActionsLayout =
        new HorizontalLayout(refreshResultButton, runCleanupButton);
    cleanupActionsLayout.setPadding(false);
    cleanupActionsLayout.setWidthFull();
    cleanupActionsLayout.setJustifyContentMode(JustifyContentMode.END);

    cleanupConfirmationDialog =
        new PortalConfirmationDialog(messageProperties, "admin-mail-queue-confirm");
    add(
        queueTitleHeading,
        warningLayout,
        cleanupActionsLayout,
        cleanupResultSpan,
        cleanupConfirmationDialog);
    addAttachListener(event -> loadLatestCleanupResult());
    addDetachListener(event -> cleanupConfirmationDialog.close());
    refreshTexts();
  }

  public boolean canManageCleanup() {
    return emailQueueCleanupService.canManageCleanup();
  }

  public void refreshTexts() {
    queueTitleHeading.setText(messageProperties.getAdminQueueResolutionTitle());
    warningTextSpan.setText(messageProperties.getAdminQueueResolutionDescription());
    refreshResultButton.setAriaLabel(messageProperties.getNotificationsRefresh());
    refreshResultButton.setTooltipText(messageProperties.getNotificationsRefresh());
    runCleanupButton.setText(messageProperties.getAdminQueueCleanupRun());
    cleanupConfirmationDialog.refreshTexts();
    renderCleanupResult();
  }

  private void loadLatestCleanupResult() {
    try {
      latestCleanupResult = emailQueueCleanupService.latestResult();
      renderCleanupResult();
    } catch (RuntimeException cleanupFailure) {
      showCleanupFailure(cleanupFailure);
    }
  }

  private void renderCleanupResult() {
    if (latestCleanupResult == null) {
      cleanupResultSpan.setText(messageProperties.getAdminQueueCleanupNeverRun());
      return;
    }
    String finishTimeText =
        latestCleanupResult.finishedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    cleanupResultSpan.setText(
        MessageFormat.format(
            latestCleanupResult.busy()
                ? messageProperties.getAdminQueueCleanupBusyResult()
                : messageProperties.getAdminQueueCleanupResult(),
            finishTimeText,
            latestCleanupResult.resolvedCount()));
  }

  private void openCleanupConfirmationDialog() {
    cleanupConfirmationDialog.open(
        new PortalConfirmationDialog.Prompt(
            messageProperties::getAdminQueueCleanupRun,
            messageProperties::getAdminQueueResolutionTitle,
            messageProperties::getAdminQueueResolutionConfirm,
            messageProperties::getAdminQueueResolutionConsequence,
            () -> new Span(messageProperties.getAdminQueueCleanupLimit()),
            messageProperties::getAdminQueueResolutionDescription,
            messageProperties::getAdminQueueCleanupRun,
            true,
            AppIcon.CLOSE::create),
        this::runManualCleanup);
  }

  private void runManualCleanup() {
    try {
      CleanupResult completedResult = emailQueueCleanupService.runManually();
      latestCleanupResult = completedResult;
      renderCleanupResult();
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getAdminQueueResolutionTitle,
              () ->
                  completedResult.busy()
                      ? messageProperties.getAdminQueueResolutionBusy()
                      : MessageFormat.format(
                          messageProperties.getAdminQueueCleanupCompleted(),
                          completedResult.resolvedCount()),
              completedResult.busy() ? NotificationType.WARNING : NotificationType.SUCCESS);
    } catch (RuntimeException cleanupFailure) {
      showCleanupFailure(cleanupFailure);
    }
  }

  private void showCleanupFailure(RuntimeException cleanupFailure) {
    logger.error("Mail queue administration failed", LogSafety.safeFailure(cleanupFailure));
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            messageProperties::getAdminQueueResolutionFailure,
            NotificationType.ERROR);
  }
}
