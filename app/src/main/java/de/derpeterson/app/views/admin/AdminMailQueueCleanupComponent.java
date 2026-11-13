package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosCssClasses;
import de.derpeterson.app.ui.components.PortalConfirmationDialog;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sofortige Verwaltungsaktion, unabhängig vom Speichern des Konfigurationsentwurfs. */
public final class AdminMailQueueCleanupComponent extends CosmosCardComponent {
  private static final Logger logger =
      LoggerFactory.getLogger(AdminMailQueueCleanupComponent.class);
  private final MessageProperties messageProperties;
  private final transient EmailQueueCleanupService emailQueueCleanupService;

  private final H3 queueTitleHeading = new H3();
  private final Span warningTextSpan = new Span();
  private final Span warningLabelSpan = new Span();
  private final Span assistantDescriptionSpan = new Span();
  private final Span statusTitleSpan = new Span();
  private final Span manualHintSpan = new Span();
  private final FormLayout cleanupSettingsFormLayout = new FormLayout();
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
    addClassNames("admin-mail-queue-resolution", CosmosCssClasses.PANEL_CARD);
    // Der lokale responsive Kartenrand bestimmt die Breite statt der Basis-Card mit 100 %.
    setWidth(null);
    queueTitleHeading.addClassName(CosmosCssClasses.PANEL_TITLE);
    assistantDescriptionSpan.addClassName(CosmosCssClasses.PANEL_DESCRIPTION);
    VerticalLayout assistantHeadingVerticalLayout =
        new VerticalLayout(queueTitleHeading, assistantDescriptionSpan);
    assistantHeadingVerticalLayout.setPadding(false);
    assistantHeadingVerticalLayout.setSpacing(false);
    assistantHeadingVerticalLayout.addClassName(CosmosCssClasses.PANEL_HEADING);
    Span assistantIconSpan = new Span(AppIcon.BRUSH_CLEANING.create());
    assistantIconSpan.addClassName(CosmosCssClasses.PANEL_ICON);
    HorizontalLayout assistantHeaderHorizontalLayout =
        new HorizontalLayout(assistantIconSpan, assistantHeadingVerticalLayout);
    assistantHeaderHorizontalLayout.addClassName(CosmosCssClasses.PANEL_HEADER);
    assistantHeaderHorizontalLayout.setAlignItems(Alignment.CENTER);

    cleanupSettingsFormLayout.setWidthFull();
    cleanupSettingsFormLayout.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("700px", 2));
    cleanupSettingsFormLayout.addClassName("admin-queue-cleanup-fields");
    warningLabelSpan.addClassName("cosmos-security-warning-label");
    HorizontalLayout securityWarningHorizontalLayout =
        new HorizontalLayout(AppIcon.WARNING.create(), new Span(warningLabelSpan, warningTextSpan));
    securityWarningHorizontalLayout.setPadding(false);
    securityWarningHorizontalLayout.setSpacing(false);
    securityWarningHorizontalLayout.setAlignItems(Alignment.START);
    securityWarningHorizontalLayout.addClassName("cosmos-security-warning");

    cleanupResultSpan.setId("admin-mail-queue-result");
    cleanupResultSpan.addClassName("admin-queue-cleanup-output");
    statusTitleSpan.addClassName("admin-queue-cleanup-status-title");
    VerticalLayout statusCopyVerticalLayout =
        new VerticalLayout(statusTitleSpan, cleanupResultSpan);
    statusCopyVerticalLayout.setPadding(false);
    statusCopyVerticalLayout.setSpacing(false);
    statusCopyVerticalLayout.setWidthFull();
    refreshResultButton.setId("admin-mail-queue-refresh");
    refreshResultButton.addClickListener(event -> loadLatestCleanupResult(true));
    runCleanupButton.setId("admin-mail-queue-cleanup");
    runCleanupButton.addClassName(CosmosCssClasses.DELETE_BUTTON);
    runCleanupButton.addClickListener(event -> openCleanupConfirmationDialog());
    HorizontalLayout cleanupActionsHorizontalLayout =
        new HorizontalLayout(refreshResultButton, runCleanupButton);
    cleanupActionsHorizontalLayout.setPadding(false);
    cleanupActionsHorizontalLayout.addClassName("admin-queue-cleanup-actions");
    HorizontalLayout cleanupStatusHorizontalLayout =
        new HorizontalLayout(
            AppIcon.TERMINAL.create(), statusCopyVerticalLayout, cleanupActionsHorizontalLayout);
    cleanupStatusHorizontalLayout.setAlignItems(Alignment.CENTER);
    cleanupStatusHorizontalLayout.addClassName("admin-queue-cleanup-status");
    HorizontalLayout manualHintHorizontalLayout =
        new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), manualHintSpan);
    manualHintHorizontalLayout.setAlignItems(Alignment.CENTER);
    manualHintHorizontalLayout.addClassName("admin-queue-cleanup-info");

    VerticalLayout cleanupContentVerticalLayout =
        new VerticalLayout(
            cleanupSettingsFormLayout,
            securityWarningHorizontalLayout,
            cleanupStatusHorizontalLayout,
            manualHintHorizontalLayout);
    cleanupContentVerticalLayout.addClassName("admin-queue-cleanup-content");
    cleanupContentVerticalLayout.setPadding(false);
    cleanupContentVerticalLayout.setSpacing(false);
    cleanupConfirmationDialog =
        new PortalConfirmationDialog(messageProperties, "admin-mail-queue-confirm");
    add(assistantHeaderHorizontalLayout, cleanupContentVerticalLayout, cleanupConfirmationDialog);
    addAttachListener(event -> loadLatestCleanupResult(false));
    addDetachListener(event -> cleanupConfirmationDialog.close());
    refreshTexts();
  }

  public void setSettingsFields(
      Component automaticCleanupField,
      Component historyRetentionField,
      Component cleanupIntervalField,
      Component minimumAgeField) {
    cleanupSettingsFormLayout.removeAll();
    cleanupSettingsFormLayout.add(
        automaticCleanupField, historyRetentionField, cleanupIntervalField, minimumAgeField);
  }

  public boolean canManageCleanup() {
    return emailQueueCleanupService.canManageCleanup();
  }

  public void refreshTexts() {
    queueTitleHeading.setText(messageProperties.getAdminQueueCleanupAssistantTitle());
    assistantDescriptionSpan.setText(messageProperties.getAdminQueueCleanupAssistantDescription());
    statusTitleSpan.setText(messageProperties.getAdminQueueCleanupStatusTitle());
    manualHintSpan.setText(messageProperties.getAdminQueueCleanupManualHint());
    warningLabelSpan.setText(messageProperties.getSupportSecurityNoticeLabel() + " ");
    warningTextSpan.setText(messageProperties.getAdminQueueResolutionDescription());
    refreshResultButton.setAriaLabel(messageProperties.getNotificationsRefresh());
    refreshResultButton.setTooltipText(messageProperties.getNotificationsRefresh());
    runCleanupButton.setText(messageProperties.getAdminQueueCleanupRun());
    cleanupConfirmationDialog.refreshTexts();
    renderCleanupResult();
  }

  private void loadLatestCleanupResult(boolean announceSuccess) {
    try {
      latestCleanupResult = emailQueueCleanupService.latestResult();
      renderCleanupResult();
      if (announceSuccess) {
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getBaseSuccessTitle,
                messageProperties::getAdminQueueCleanupRefreshSuccess,
                NotificationType.SUCCESS);
      }
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
