package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.logging.LogSafety;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.NotificationHelper.NotificationType;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.EmailQueueCleanupStatus;
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
  private final Badge cleanupStatusBadge = new Badge();
  private final Span cleanupExplanationSpan = new Span();
  private CleanupResult latestCleanupResult;
  private Long observedManualRunId;
  private transient Registration cleanupPollRegistration;
  private int previousPollInterval;
  private int installedPollInterval;

  private final Button refreshResultButton = new Button(AppIcon.REFRESH.create());
  private final Button runCleanupButton = new Button(AppIcon.BRUSH_CLEANING.create());
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
        new HorizontalLayout(
            AppIcon.WARNING_TRIANGLE.create(), new Span(warningLabelSpan, warningTextSpan));
    securityWarningHorizontalLayout.setPadding(false);
    securityWarningHorizontalLayout.setSpacing(false);
    securityWarningHorizontalLayout.setAlignItems(Alignment.START);
    securityWarningHorizontalLayout.addClassName("cosmos-security-warning");

    cleanupResultSpan.setId("admin-mail-queue-result");
    cleanupResultSpan.addClassName("admin-queue-cleanup-output");
    statusTitleSpan.addClassName("admin-queue-cleanup-status-title");
    cleanupStatusBadge.setId("admin-mail-queue-status");
    cleanupExplanationSpan.setId("admin-mail-queue-status-explanation");
    cleanupExplanationSpan.addClassName("admin-queue-cleanup-explanation");
    HorizontalLayout statusHeadingLayout =
        new HorizontalLayout(statusTitleSpan, cleanupStatusBadge);
    statusHeadingLayout.setPadding(false);
    statusHeadingLayout.setAlignItems(Alignment.CENTER);
    statusHeadingLayout.addClassName("admin-queue-cleanup-status-heading");
    VerticalLayout statusCopyVerticalLayout =
        new VerticalLayout(statusHeadingLayout, cleanupResultSpan, cleanupExplanationSpan);
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
    cleanupConfirmationDialog.addClassName("admin-queue-cleanup-confirmation");
    add(assistantHeaderHorizontalLayout, cleanupContentVerticalLayout, cleanupConfirmationDialog);
    addAttachListener(event -> loadLatestCleanupResult(false));
    addDetachListener(event -> {
      stopCleanupPolling(event.getUI());
      cleanupConfirmationDialog.close();
    });
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
    runCleanupButton.setText(messageProperties.getAdminQueueCleanupManualTitle());
    cleanupConfirmationDialog.refreshTexts();
    renderCleanupResult();
  }

  private void loadLatestCleanupResult(boolean announceSuccess) {
    try {
      CleanupResult observedResult =
          observedManualRunId == null
              ? null
              : emailQueueCleanupService.resultFor(observedManualRunId);
      if (observedManualRunId != null && observedResult == null) {
        observedManualRunId = null;
      }
      latestCleanupResult = emailQueueCleanupService.latestResult();
      renderCleanupResult();
      boolean announcedCompletion = observedResult != null && !observedResult.active();
      if (announcedCompletion) {
        observedManualRunId = null;
        announceCleanupOutcome(observedResult);
      }
      updateCleanupPolling();
      if (announceSuccess && !announcedCompletion) {
        NotificationHelper.getInstance()
            .showTextNotification(
                messageProperties::getBaseSuccessTitle,
                messageProperties::getAdminQueueCleanupRefreshSuccess,
                NotificationType.SUCCESS);
      }
    } catch (RuntimeException cleanupFailure) {
      observedManualRunId = null;
      getUI().ifPresent(this::stopCleanupPolling);
      showCleanupFailure(cleanupFailure);
    }
  }

  private void renderCleanupResult() {
    cleanupStatusBadge.removeThemeVariants(
        BadgeVariant.SUCCESS, BadgeVariant.ERROR, BadgeVariant.WARNING);
    cleanupExplanationSpan.setVisible(latestCleanupResult != null);
    cleanupExplanationSpan.removeClassNames(
        "admin-queue-cleanup-success", "admin-queue-cleanup-failed", "admin-queue-cleanup-warning");
    if (latestCleanupResult == null) {
      runCleanupButton.setEnabled(true);
      cleanupStatusBadge.getElement().setAttribute("data-status", "NOT_RUN");
      cleanupStatusBadge.setText(messageProperties.getAdminQueueCleanupNeverRun());
      cleanupResultSpan.setText(
          MessageFormat.format(messageProperties.getAdminQueueCleanupResult(), "—", "—"));
      return;
    }
    EmailQueueCleanupStatus status = latestCleanupResult.status();
    cleanupStatusBadge.getElement().setAttribute("data-status", status.name());
    switch (status) {
      case WAITING ->
          setCleanupStatus(
              messageProperties.getAdminQueueCleanupStatusWaiting(),
              messageProperties.getAdminQueueCleanupWaitingExplanation(),
              BadgeVariant.WARNING);
      case RUNNING ->
          setCleanupStatus(
              messageProperties.getAdminQueueCleanupStatusRunning(),
              messageProperties.getAdminQueueCleanupRunningExplanation(),
              BadgeVariant.WARNING);
      case COMPLETED ->
          setCleanupStatus(
              messageProperties.getAdminQueueCleanupStatusCompleted(),
              messageProperties.getAdminQueueCleanupSuccessExplanation(),
              BadgeVariant.SUCCESS);
      case FAILED ->
          setCleanupStatus(
              messageProperties.getAdminQueueCleanupStatusFailed(),
              messageProperties.getAdminQueueCleanupFailureExplanation(),
              BadgeVariant.ERROR);
      case PARTIAL ->
          setCleanupStatus(
              messageProperties.getAdminQueueCleanupStatusPartial(),
              messageProperties.getAdminQueueCleanupPartialExplanation(),
              BadgeVariant.WARNING);
    }
    runCleanupButton.setEnabled(!latestCleanupResult.active());
    String timeText =
        (latestCleanupResult.startedAt() == null
                ? latestCleanupResult.requestedAt()
                : latestCleanupResult.startedAt())
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    cleanupResultSpan.setText(
        MessageFormat.format(
            status == EmailQueueCleanupStatus.WAITING
                ? messageProperties.getAdminQueueCleanupRequestedResult()
                : messageProperties.getAdminQueueCleanupResult(),
            timeText,
            latestCleanupResult.resolvedCount()));
  }

  private void setCleanupStatus(String label, String explanation, BadgeVariant variant) {
    cleanupStatusBadge.setText(label);
    cleanupStatusBadge.addThemeVariants(variant);
    cleanupExplanationSpan.setText(explanation);
    cleanupExplanationSpan.addClassName(
        switch (variant) {
          case SUCCESS -> "admin-queue-cleanup-success";
          case ERROR -> "admin-queue-cleanup-failed";
          default -> "admin-queue-cleanup-warning";
        });
  }

  private void updateCleanupPolling() {
    getUI()
        .ifPresent(cleanupUi -> {
          boolean needsPolling =
              observedManualRunId != null
                  || latestCleanupResult != null && latestCleanupResult.active();
          if (!needsPolling) {
            stopCleanupPolling(cleanupUi);
          } else if (cleanupPollRegistration == null) {
            previousPollInterval = cleanupUi.getPollInterval();
            installedPollInterval =
                previousPollInterval > 0 ? Math.min(previousPollInterval, 1000) : 1000;
            cleanupUi.setPollInterval(installedPollInterval);
            cleanupPollRegistration =
                cleanupUi.addPollListener(event -> loadLatestCleanupResult(false));
          }
        });
  }

  private void stopCleanupPolling(UI cleanupUi) {
    if (cleanupPollRegistration != null) {
      cleanupPollRegistration.remove();
      cleanupPollRegistration = null;
      if (cleanupUi.getPollInterval() == installedPollInterval) {
        cleanupUi.setPollInterval(previousPollInterval);
      }
    }
  }

  private void openCleanupConfirmationDialog() {
    cleanupConfirmationDialog.open(
        new PortalConfirmationDialog.Prompt(
            messageProperties::getAdminQueueCleanupManualTitle,
            messageProperties::getAdminQueueResolutionTitle,
            messageProperties::getAdminQueueResolutionConfirm,
            messageProperties::getAdminQueueResolutionConsequence,
            this::createCleanupConfirmationDetails,
            () -> "",
            messageProperties::getAdminQueueCleanupRun,
            true,
            AppIcon.BRUSH_CLEANING::create),
        this::runManualCleanup);
  }

  private VerticalLayout createCleanupConfirmationDetails() {
    Span deliveryWarningTitleSpan =
        new Span(messageProperties.getAdminQueueCleanupWarningTitle() + " ");
    deliveryWarningTitleSpan.addClassName("cosmos-security-warning-label");
    Span deliveryWarningTextSpan =
        new Span(messageProperties.getAdminQueueCleanupWarningDescription());
    HorizontalLayout deliveryWarningHorizontalLayout =
        new HorizontalLayout(
            AppIcon.WARNING_TRIANGLE.create(),
            new Span(deliveryWarningTitleSpan, deliveryWarningTextSpan));
    deliveryWarningHorizontalLayout.setPadding(false);
    deliveryWarningHorizontalLayout.setSpacing(false);
    deliveryWarningHorizontalLayout.setAlignItems(Alignment.START);
    deliveryWarningHorizontalLayout.addClassName("cosmos-security-warning");

    VerticalLayout cleanupParametersCopyLayout =
        new VerticalLayout(
            new Span(messageProperties.getAdminQueueCleanupParametersHint()),
            new Span(messageProperties.getAdminQueueCleanupParametersScopeHint()));
    cleanupParametersCopyLayout.setPadding(false);
    cleanupParametersCopyLayout.setSpacing(false);
    cleanupParametersCopyLayout.addClassName("admin-queue-cleanup-parameters-copy");
    HorizontalLayout cleanupParametersHorizontalLayout =
        new HorizontalLayout(AppIcon.INFO_CIRCLE.create(), cleanupParametersCopyLayout);
    cleanupParametersHorizontalLayout.setPadding(false);
    cleanupParametersHorizontalLayout.setSpacing(false);
    cleanupParametersHorizontalLayout.setAlignItems(Alignment.START);
    cleanupParametersHorizontalLayout.addClassName("admin-queue-cleanup-info");

    VerticalLayout confirmationDetailsVerticalLayout =
        new VerticalLayout(deliveryWarningHorizontalLayout, cleanupParametersHorizontalLayout);
    confirmationDetailsVerticalLayout.setPadding(false);
    confirmationDetailsVerticalLayout.setSpacing(false);
    confirmationDetailsVerticalLayout.addClassName("admin-queue-cleanup-confirmation-details");
    return confirmationDetailsVerticalLayout;
  }

  private void runManualCleanup() {
    try {
      CleanupResult requestedResult = emailQueueCleanupService.runManually();
      latestCleanupResult = requestedResult;
      renderCleanupResult();
      if (requestedResult.active()) {
        observedManualRunId = requestedResult.runId();
        NotificationHelper.getInstance()
            .showTextNotification(
                messageProperties::getAdminQueueResolutionTitle,
                () ->
                    requestedResult.status() == EmailQueueCleanupStatus.RUNNING
                        ? messageProperties.getAdminQueueCleanupRunningExplanation()
                        : messageProperties.getAdminQueueCleanupWaitingExplanation(),
                NotificationType.NORMAL);
      } else {
        announceCleanupOutcome(requestedResult);
      }
      updateCleanupPolling();
    } catch (RuntimeException cleanupFailure) {
      showCleanupFailure(cleanupFailure);
    }
  }

  private void announceCleanupOutcome(CleanupResult result) {
    NotificationType notificationType =
        switch (result.status()) {
          case COMPLETED -> NotificationType.SUCCESS;
          case PARTIAL -> NotificationType.WARNING;
          default -> NotificationType.ERROR;
        };
    NotificationHelper.getInstance()
        .showTextNotification(
            messageProperties::getAdminQueueResolutionTitle,
            () ->
                switch (result.status()) {
                  case COMPLETED ->
                      MessageFormat.format(
                          messageProperties.getAdminQueueCleanupCompleted(),
                          result.resolvedCount());
                  case PARTIAL ->
                      MessageFormat.format(
                          messageProperties.getAdminQueueCleanupPartialCompleted(),
                          result.resolvedCount());
                  default -> messageProperties.getAdminQueueCleanupFailureExplanation();
                },
            notificationType);
  }

  private void showCleanupFailure(RuntimeException cleanupFailure) {
    logger.error("Mail queue administration failed", LogSafety.safeFailure(cleanupFailure));
    NotificationHelper.getInstance()
        .showTextNotification(
            messageProperties::getBaseFailedTitle,
            messageProperties::getAdminQueueResolutionFailure,
            NotificationType.ERROR);
  }
}
