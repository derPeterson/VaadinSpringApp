package de.derpeterson.app.views.admin;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.i18n.UserNotificationTexts;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.MaintenanceService.State;
import de.derpeterson.app.service.UserNotificationService;
import de.derpeterson.app.service.UserNotificationService.Preview;
import de.derpeterson.app.service.UserNotificationService.Rejected;
import de.derpeterson.app.service.UserNotificationService.Selection;
import de.derpeterson.app.service.UserNotificationService.Sent;
import de.derpeterson.app.service.UserNotificationService.Target;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPagination;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.ui.components.NotificationEmptyState;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

/** Superadmin-Entwurf mit serverseitiger Empfängervorschau und dauerhaftem Request-Schlüssel. */
public class AdminUserNotificationsSection extends VerticalLayout {
  private static final String COSMOS_PANEL_META_CLASS = "cosmos-panel-meta";
  private static final String ARIA_EXPANDED = "aria-expanded";
  private static final String ARIA_HIDDEN = "aria-hidden";
  private static final String DATA_STATE = "data-state";

  private final transient MessageProperties messageProperties;
  private final transient UserNotificationService userNotificationService;
  private final transient HttpServletRequest httpServletRequest;
  private final Span notificationAdminBreadcrumbSpan = new Span();
  private final Span notificationAdminTitleSpan = new Span();
  private final Span notificationAdminDescriptionSpan = new Span();
  private final H2 composeHeadingH2 = new H2();
  private final Span composeDescriptionSpan = new Span();
  private final Span deliveryLabelSpan = new Span();
  private final Div deliveryStatusDiv = new Div();
  private final Tooltip deliveryTooltip = Tooltip.forComponent(deliveryStatusDiv);
  private final VerticalLayout notificationComposeLayout;
  private MaintenanceService.State maintenanceState;
  private transient Registration maintenancePollRegistration;
  private int previousPollInterval;
  private int installedPollInterval;
  private final Span deliveryInfoSpan = new Span();
  private final Span everyoneCaptionSpan = new Span();
  private final Span everyoneHintSpan = new Span();
  private final Span titleCountSpan = new Span();
  private final Span bodyCountSpan = new Span();
  private final Span previewStatusSpan = new Span();
  private List<Target> eligibleTargets = List.of();
  private final Checkbox sendToEveryoneCheckbox = new Checkbox();
  private final MultiSelectComboBox<Target> recipientComboBox = new MultiSelectComboBox<>();
  private final TextField everyoneRecipientsTextField = new TextField();
  private final Span recipientSummarySpan = new Span();
  private final TextField notificationTitleField = new TextField();
  private final TextArea notificationBodyField = new TextArea();
  private final Button previewButton = new Button();
  private final Button sendNotificationButton = new Button();
  private final Div notificationPreviewDiv = new Div();
  private final Div previewCardDiv = new Div();
  private final Div emptyPreviewDiv = new Div();
  private final Span emptyPreviewSpan = new Span();
  private final H2 previewTitleH2 = new H2();
  private final Span previewDescriptionSpan = new Span();
  private final Span previewBeforeSendHintSpan = new Span();
  private final Span recipientRecheckHintSpan = new Span();
  private final H2 historyTitleH2 = new H2();
  private final Span historyDescriptionSpan = new Span();
  private final Span historyCountSpan = new Span();
  private final Div sentNotificationHistoryDiv = new Div();
  private final CosmosPagination historyPagination;
  private int historyPageSize = 5;
  private Preview reviewedRecipientPreview;
  private UUID sendRequestUuid = UUID.randomUUID();
  private int historyPageIndex;

  public AdminUserNotificationsSection(
      MessageProperties messageProperties,
      UserNotificationService userNotificationService,
      HttpServletRequest httpServletRequest) {
    this.messageProperties = messageProperties;
    this.userNotificationService = userNotificationService;
    this.httpServletRequest = httpServletRequest;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    addClassNames("cosmos-admin-section", "cosmos-admin-section--notifications", "cosmos-theme");
    composeHeadingH2.addClassName(PANEL_TITLE);
    historyTitleH2.addClassName(PANEL_TITLE);
    composeDescriptionSpan.addClassName(PANEL_DESCRIPTION);

    configureComposeInputs();
    notificationComposeLayout = buildNotificationComposeLayout();

    historyPagination = buildHistoryPagination();
    Div sentHistoryPanelDiv = buildHistoryPanelDiv();
    CosmosCardComponent composeCardComponent = new CosmosCardComponent(notificationComposeLayout);
    composeCardComponent.addClassNames("cosmos-admin-form-card", PANEL_CARD);
    CosmosCardComponent sentHistoryCardComponent = new CosmosCardComponent(sentHistoryPanelDiv);
    sentHistoryCardComponent.addClassNames("cosmos-admin-form-card", PANEL_CARD);

    Div notificationColumnsDiv = new Div(composeCardComponent, previewCardDiv);
    notificationColumnsDiv.addClassName("admin-notification-columns");
    add(
        new CosmosSectionHeaderComponent(
            notificationAdminBreadcrumbSpan,
            notificationAdminTitleSpan,
            notificationAdminDescriptionSpan),
        notificationColumnsDiv,
        sentHistoryCardComponent);

    refreshTexts();

    addAttachListener(notificationSectionAttachEvent ->
        startMaintenancePolling(notificationSectionAttachEvent.getUI()));
    addDetachListener(notificationSectionDetachEvent ->
        stopMaintenancePolling(notificationSectionDetachEvent.getUI()));
  }

  private void configureComposeInputs() {
    recipientComboBox.setItemLabelGenerator(
        recipientTarget -> recipientTarget.name() + " · " + recipientTarget.email());
    recipientComboBox.setClassNameGenerator(
        recipientTarget -> "admin-notification-recipient-option");
    recipientComboBox.setRenderer(
        new ComponentRenderer<>(recipientTarget -> {
          Span recipientNameSpan = new Span(recipientTarget.name());
          Span recipientEmailSpan = new Span(recipientTarget.email());
          Div recipientLabelDiv = new Div(recipientNameSpan, recipientEmailSpan);
          recipientLabelDiv.addClassName("admin-notification-recipient-label");
          return recipientLabelDiv;
        }));
    recipientComboBox.setWidthFull();
    recipientComboBox.setManualValidation(true);
    recipientComboBox.setRequiredIndicatorVisible(true);
    recipientComboBox.setId("admin-notifications-recipients");

    sendToEveryoneCheckbox.setId("admin-notifications-everyone");
    everyoneRecipientsTextField.setId("admin-notifications-everyone-recipients");
    everyoneRecipientsTextField.setManualValidation(true);
    everyoneRecipientsTextField.setRequiredIndicatorVisible(true);
    everyoneRecipientsTextField.setReadOnly(true);
    everyoneRecipientsTextField.setEnabled(false);
    everyoneRecipientsTextField.setWidthFull();

    notificationTitleField.setId("admin-notifications-title");
    notificationBodyField.setId("admin-notifications-body");
    notificationTitleField.setMaxLength(UserNotificationService.TITLE_LIMIT);
    notificationBodyField.setMaxLength(UserNotificationService.BODY_LIMIT);
    notificationBodyField.setMinRows(4);
    notificationBodyField.addClassName("admin-notification-body-field");
    notificationTitleField.setManualValidation(true);
    notificationBodyField.setManualValidation(true);

    for (var notificationInputField : List.of(notificationTitleField, notificationBodyField)) {
      notificationInputField.setWidthFull();
      notificationInputField.setRequiredIndicatorVisible(true);
      notificationInputField.setValueChangeMode(ValueChangeMode.EAGER);
      notificationInputField.addValueChangeListener(_ -> {
        refreshNotificationCharacterCounts();
        invalidatePreview();
      });
    }

    sendToEveryoneCheckbox.addValueChangeListener(_ -> {
      refreshRecipientSelectionControls();
      invalidatePreview();
    });
    recipientComboBox.addValueChangeListener(_ -> {
      refreshRecipientSummaryText();
      invalidatePreview();
    });

    previewButton.setId("admin-notifications-preview");
    previewButton.setIcon(AppIcon.EYE.create());
    sendNotificationButton.setIcon(AppIcon.PAPERPLANE.create());
    sendNotificationButton.setId("admin-notifications-send");
    previewButton.addClickListener(_ -> reviewNotificationDraft());
    sendNotificationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendNotificationButton.setEnabled(false);
    sendNotificationButton.addClickListener(_ -> sendReviewedNotification());
  }

  private VerticalLayout buildNotificationComposeLayout() {
    Div composeIconDiv = new Div(createDecorativeSvgIcon(AppIcon.BELL));
    composeIconDiv.addClassName(PANEL_ICON);
    Div composeHeadingDiv = new Div(composeHeadingH2, composeDescriptionSpan);
    composeHeadingDiv.addClassName(PANEL_HEADING);

    Span deliveryDotSpan = new Span();
    deliveryDotSpan.addClassName("cosmos-notification-delivery-dot");
    deliveryDotSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
    deliveryStatusDiv.add(deliveryDotSpan, deliveryLabelSpan);
    deliveryStatusDiv.addClassNames(COSMOS_PANEL_META_CLASS, "cosmos-notification-delivery");
    deliveryStatusDiv.getElement().setAttribute("role", "status");
    Div composeHeaderDiv = new Div(composeIconDiv, composeHeadingDiv, deliveryStatusDiv);
    composeHeaderDiv.addClassName(PANEL_HEADER);

    Div deliveryInfoDiv = new Div(createDecorativeSvgIcon(AppIcon.INFO_CIRCLE), deliveryInfoSpan);
    deliveryInfoDiv.addClassName("cosmos-notification-compose-info");
    sendToEveryoneCheckbox.addClassName("cosmos-notification-compose-everyone");
    everyoneHintSpan.addClassName("cosmos-notification-compose-everyone-hint");
    everyoneHintSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
    VerticalLayout everyoneLabelLayout = new VerticalLayout(everyoneCaptionSpan, everyoneHintSpan);
    everyoneLabelLayout.setPadding(false);
    everyoneLabelLayout.setSpacing(false);
    everyoneLabelLayout.setWidth(null);
    everyoneLabelLayout.setAlignItems(FlexComponent.Alignment.START);
    everyoneLabelLayout.addClassName("cosmos-notification-compose-everyone-label");
    sendToEveryoneCheckbox.setLabelComponent(everyoneLabelLayout);
    sendToEveryoneCheckbox.setAriaDescribedBy(everyoneHintSpan);

    recipientSummarySpan.addClassName("cosmos-notification-compose-recipient-summary");
    recipientSummarySpan.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div recipientFieldDiv = new Div(recipientComboBox, recipientSummarySpan);
    recipientFieldDiv.addClassName("cosmos-notification-compose-recipient-field");
    Div recipientSelectionDiv = new Div(sendToEveryoneCheckbox);
    recipientSelectionDiv.addClassName("cosmos-notification-compose-selection");

    notificationPreviewDiv.addClassName("cosmos-notification-compose-preview");
    notificationPreviewDiv.setVisible(false);
    Div recipientRecheckHintDiv =
        new Div(createDecorativeSvgIcon(AppIcon.INFO_CIRCLE), recipientRecheckHintSpan);
    recipientRecheckHintDiv.addClassName("cosmos-panel-hint");
    configureNotificationPreviewCard();

    Div previewActionsDiv = new Div(previewButton);
    previewActionsDiv.addClassName("cosmos-panel-actions");
    Div composeFooterDiv = new Div(deliveryInfoDiv, recipientRecheckHintDiv, previewActionsDiv);
    composeFooterDiv.addClassName("cosmos-panel-footer");

    previewStatusSpan.addClassName("cosmos-notification-compose-status");
    previewStatusSpan.getElement().setAttribute("role", "status");
    previewStatusSpan.setVisible(false);

    VerticalLayout composeLayout =
        new VerticalLayout(
            composeHeaderDiv,
            recipientSelectionDiv,
            recipientFieldDiv,
            everyoneRecipientsTextField,
            buildFieldWithCounterDiv(notificationTitleField, titleCountSpan),
            buildFieldWithCounterDiv(notificationBodyField, bodyCountSpan),
            composeFooterDiv);
    composeLayout.setPadding(false);
    composeLayout.setSpacing(false);
    composeLayout.setWidthFull();
    composeLayout.addClassNames("cosmos-admin-form", "cosmos-notification-compose");
    return composeLayout;
  }

  private void configureNotificationPreviewCard() {
    previewTitleH2.addClassName(PANEL_TITLE);
    previewDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    Div previewHeadingDiv = new Div(previewTitleH2, previewDescriptionSpan);
    previewHeadingDiv.addClassName(PANEL_HEADING);
    Div previewIconDiv = new Div(createDecorativeSvgIcon(AppIcon.EYE));
    previewIconDiv.addClassName(PANEL_ICON);
    Div previewHeaderDiv = new Div(previewIconDiv, previewHeadingDiv);
    previewHeaderDiv.addClassName(PANEL_HEADER);

    Div sendHintDiv =
        new Div(createDecorativeSvgIcon(AppIcon.INFO_CIRCLE), previewBeforeSendHintSpan);
    sendHintDiv.addClassName("cosmos-panel-hint");
    Div sendActionsDiv = new Div(sendNotificationButton);
    sendActionsDiv.addClassName("cosmos-panel-actions");
    Div previewFooterDiv = new Div(sendHintDiv, sendActionsDiv);
    previewFooterDiv.addClassName("cosmos-panel-footer");

    emptyPreviewDiv.add(createDecorativeSvgIcon(AppIcon.EYE), emptyPreviewSpan);
    emptyPreviewDiv.addClassName("admin-preview-empty");
    previewCardDiv.add(previewHeaderDiv, emptyPreviewDiv, notificationPreviewDiv, previewFooterDiv);
    previewCardDiv.addClassNames(PANEL_CARD, "admin-notification-preview-card");
  }

  private CosmosPagination buildHistoryPagination() {
    CosmosPagination sentHistoryPagination =
        new CosmosPagination(
            messageProperties,
            selectedPageIndex -> {
              historyPageIndex = selectedPageIndex;
              loadSentNotificationHistory();
            },
            selectedPageSize -> {
              historyPageSize = selectedPageSize;
              historyPageIndex = 0;
              loadSentNotificationHistory();
            },
            List.of(5, 10, 15));
    sentHistoryPagination.addClassName("cosmos-notification-sent-pagination");
    return sentHistoryPagination;
  }

  private Div buildHistoryPanelDiv() {
    Div archiveIconDiv = new Div(createDecorativeSvgIcon(AppIcon.BELL));
    archiveIconDiv.addClassName(PANEL_ICON);
    historyDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    Div archiveHeadingDiv = new Div(historyTitleH2, historyDescriptionSpan);
    archiveHeadingDiv.addClassName(PANEL_HEADING);
    historyCountSpan.addClassName(COSMOS_PANEL_META_CLASS);
    Div archiveHeaderDiv = new Div(archiveIconDiv, archiveHeadingDiv, historyCountSpan);
    archiveHeaderDiv.addClassName(PANEL_HEADER);

    sentNotificationHistoryDiv.addClassName("cosmos-notification-sent-list");
    Div sentHistoryPanelDiv =
        new Div(archiveHeaderDiv, sentNotificationHistoryDiv, historyPagination);
    sentHistoryPanelDiv.setId("admin-notifications-sent");
    sentHistoryPanelDiv.addClassName("cosmos-notification-sent");
    return sentHistoryPanelDiv;
  }

  private void startMaintenancePolling(UI notificationAdminUi) {
    refreshDeliveryState();

    previousPollInterval = notificationAdminUi.getPollInterval();
    installedPollInterval = previousPollInterval > 0 ? Math.min(previousPollInterval, 5000) : 5000;
    notificationAdminUi.setPollInterval(installedPollInterval);
    maintenancePollRegistration = notificationAdminUi.addPollListener(_ -> refreshDeliveryState());
  }

  private void stopMaintenancePolling(UI notificationAdminUi) {
    if (maintenancePollRegistration != null) {
      maintenancePollRegistration.remove();
    }

    maintenancePollRegistration = null;
    if (installedPollInterval != previousPollInterval
        && notificationAdminUi.getPollInterval() == installedPollInterval) {
      notificationAdminUi.setPollInterval(previousPollInterval);
    }
  }

  public void refreshDeliveryState() {
    State deliveryState = userNotificationService.deliveryState();
    if (deliveryState == null) {
      deliveryState = MaintenanceService.State.UNAVAILABLE;
    }
    if (deliveryState != maintenanceState) {
      invalidatePreview();
    }
    maintenanceState = deliveryState;

    boolean isDeliveryAvailable = deliveryState == MaintenanceService.State.OPEN;
    notificationComposeLayout.setEnabled(isDeliveryAvailable);
    notificationComposeLayout
        .getElement()
        .setAttribute("aria-disabled", Boolean.toString(!isDeliveryAvailable));
    deliveryStatusDiv
        .getElement()
        .setAttribute(DATA_STATE, deliveryState.name().toLowerCase(Locale.ROOT));

    String maintenanceStatusText =
        switch (deliveryState) {
          case OPEN -> messageProperties.getMaintenanceStatusAvailable();
          case ACTIVE -> messageProperties.getMaintenanceStatusActive();
          case UNAVAILABLE -> messageProperties.getMaintenanceStatusUnavailable();
        };

    String deliveryDescriptionText =
        messageProperties.getNotificationsInternal() + ": " + maintenanceStatusText;
    deliveryStatusDiv.getElement().setAttribute("aria-label", deliveryDescriptionText);
    deliveryTooltip.setText(deliveryDescriptionText);
  }

  private boolean checkDeliveryAvailableForNotificationAction() {
    refreshDeliveryState();
    if (maintenanceState == MaintenanceService.State.OPEN) {
      return true;
    }

    showActionErrorNotification(
        maintenanceState == MaintenanceService.State.ACTIVE
            ? messageProperties::getNotificationsDeliveryPaused
            : messageProperties::getNotificationsUnavailable);
    return false;
  }

  public void activate() {
    refreshDeliveryState();

    // Auch erneute Navigation und jede Serviceaktion prüfen die aktuellen Berechtigungen.
    eligibleTargets =
        List.copyOf(userNotificationService.targets(httpServletRequest.getSession(false)));
    recipientComboBox.setItems(eligibleTargets);
    refreshRecipientSelectionControls();

    clearPreview();

    historyPageIndex = 0;
    loadSentNotificationHistory();
  }

  private void invalidatePreview() {
    clearComposeValidationErrors();
    reviewedRecipientPreview = null;
    if (notificationPreviewDiv.isVisible()) {
      previewStatusSpan.setText(messageProperties.getNotificationsPreviewNeedsReview());
      previewStatusSpan.addComponentAtIndex(0, createDecorativeSvgIcon(AppIcon.BELL));
      previewStatusSpan.getElement().setAttribute(DATA_STATE, "warning");
      previewStatusSpan.setVisible(true);
    }

    sendNotificationButton.setEnabled(false);
    sendRequestUuid = UUID.randomUUID();
  }

  private void clearPreview() {
    invalidatePreview();
    notificationPreviewDiv.removeAll();
    notificationPreviewDiv.setVisible(false);
    emptyPreviewDiv.setVisible(true);
    previewStatusSpan.setText("");
    previewStatusSpan.setVisible(false);
  }

  private void clearComposeValidationErrors() {
    notificationTitleField.setInvalid(false);
    notificationBodyField.setInvalid(false);
    recipientComboBox.setInvalid(false);
    everyoneRecipientsTextField.setInvalid(false);
  }

  private boolean validateNotificationDraft() {
    notificationTitleField.setInvalid(
        notificationTitleField.getValue().isBlank()
            || notificationTitleField.getValue().length() > UserNotificationService.TITLE_LIMIT);
    notificationBodyField.setInvalid(
        notificationBodyField.getValue().isBlank()
            || notificationBodyField.getValue().length() > UserNotificationService.BODY_LIMIT);

    boolean hasNoRecipients =
        Boolean.TRUE.equals(sendToEveryoneCheckbox.getValue())
            ? eligibleTargets.isEmpty()
            : recipientComboBox.getSelectedItems().isEmpty();
    if (hasNoRecipients) {
      markRecipientInputInvalid(messageProperties.getNotificationsNoRecipients());
    }

    if (notificationTitleField.isInvalid()
        || notificationBodyField.isInvalid()
        || hasNoRecipients) {
      showPreviewValidationFailureNotification();
      return false;
    }
    return true;
  }

  private void showPreviewValidationFailureNotification() {
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getNotificationsPreviewFailedTitle,
            messageProperties::getNotificationsPreviewFailedMessage,
            NotificationHelper.NotificationType.ERROR);
  }

  private void markRecipientInputInvalid(String recipientErrorText) {
    if (Boolean.TRUE.equals(sendToEveryoneCheckbox.getValue())) {
      everyoneRecipientsTextField.setErrorMessage(recipientErrorText);
      everyoneRecipientsTextField.setInvalid(true);
    } else {
      recipientComboBox.setErrorMessage(recipientErrorText);
      recipientComboBox.setInvalid(true);
    }
  }

  private void reviewNotificationDraft() {
    if (!checkDeliveryAvailableForNotificationAction()) {
      return;
    }

    invalidatePreview();
    if (!validateNotificationDraft()) {
      return;
    }

    try {
      reviewedRecipientPreview =
          userNotificationService.preview(
              httpServletRequest.getSession(false),
              new Selection(
                  Boolean.TRUE.equals(sendToEveryoneCheckbox.getValue()),
                  recipientComboBox.getSelectedItems().stream()
                      .map(Target::id)
                      .collect(Collectors.toSet())));

      renderNotificationPreview();
      sendNotificationButton.setEnabled(true);
    } catch (RuntimeException failure) {
      invalidatePreview();
      if (failure instanceof Rejected rejected
          && (rejected.problem() == UserNotificationService.Problem.NO_RECIPIENTS
              || rejected.problem() == UserNotificationService.Problem.RECIPIENTS_CHANGED)) {
        markRecipientInputInvalid(
            rejected.problem() == UserNotificationService.Problem.NO_RECIPIENTS
                ? messageProperties.getNotificationsNoRecipients()
                : messageProperties.getNotificationsRecipientsChanged());
        showPreviewValidationFailureNotification();
      } else {
        showNotificationActionFailure(failure);
      }
    }
  }

  private static SvgIcon createDecorativeSvgIcon(AppIcon appIcon) {
    SvgIcon decorativeSvgIcon = appIcon.create();
    decorativeSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    return decorativeSvgIcon;
  }

  private static Div buildFieldWithCounterDiv(
      Component notificationInputComponent, Span characterCountSpan) {
    characterCountSpan.addClassName("cosmos-notification-compose-counter");
    characterCountSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div fieldWithCounterDiv = new Div(notificationInputComponent, characterCountSpan);
    fieldWithCounterDiv.addClassName("cosmos-notification-compose-field");
    return fieldWithCounterDiv;
  }

  private void refreshNotificationCharacterCounts() {
    titleCountSpan.setText(
        notificationTitleField.getValue().length() + " / " + UserNotificationService.TITLE_LIMIT);
    bodyCountSpan.setText(
        notificationBodyField.getValue().length() + " / " + UserNotificationService.BODY_LIMIT);
  }

  private void refreshRecipientSelectionControls() {
    boolean isSendingToEveryone = Boolean.TRUE.equals(sendToEveryoneCheckbox.getValue());
    recipientComboBox.setEnabled(!isSendingToEveryone);
    recipientComboBox.setVisible(!isSendingToEveryone);
    recipientComboBox
        .getParent()
        .ifPresent(
            recipientFieldContainer -> recipientFieldContainer.setVisible(!isSendingToEveryone));
    refreshRecipientSummaryText();

    everyoneRecipientsTextField.setVisible(isSendingToEveryone);
    everyoneRecipientsTextField.setValue(
        messageProperties.getNotificationsEveryonePlaceholder(eligibleTargets.size()));
    recipientComboBox.setPlaceholder(
        isSendingToEveryone
            ? messageProperties.getNotificationsEveryonePlaceholder(eligibleTargets.size())
            : messageProperties.getNotificationsSelectRecipients());
  }

  private void refreshRecipientSummaryText() {
    recipientSummarySpan.setText(
        messageProperties.getNotificationsSelectedUsers(
            recipientComboBox.getSelectedItems().size()));
  }

  private void renderNotificationPreview() {
    notificationPreviewDiv.removeAll();
    notificationPreviewDiv
        .getElement()
        .setAttribute("aria-label", messageProperties.getNotificationsReviewedPreview());
    Div previewHeadingDiv =
        new Div(
            createDecorativeSvgIcon(AppIcon.MESSAGE),
            new Span(messageProperties.getNotificationsContentPreview()));
    previewHeadingDiv.addClassName("admin-notification-preview-heading");
    Div previewHeaderDiv = new Div(previewHeadingDiv, previewStatusSpan);
    previewHeaderDiv.addClassName("cosmos-notification-compose-preview-header");

    HashSet<Long> selectedRecipientIds = new HashSet<>(reviewedRecipientPreview.ids());
    List<Target> selectedRecipientTargets =
        userNotificationService.targets(httpServletRequest.getSession(false)).stream()
            .filter(recipientTarget -> selectedRecipientIds.contains(recipientTarget.id()))
            .toList();

    Span recipientLabelSpan =
        new Span(
            createDecorativeSvgIcon(AppIcon.USERS),
            new Span(messageProperties.getNotificationsRecipients()));
    recipientLabelSpan.addClassName("admin-notification-preview-recipient-label");
    Div recipientAvatarsDiv = new Div(recipientLabelSpan);
    recipientAvatarsDiv.addClassName("cosmos-notification-compose-recipients");
    selectedRecipientTargets.stream()
        .limit(3)
        .forEach(recipientTarget -> {
          Avatar recipientAvatar = new Avatar(recipientTarget.name());
          recipientAvatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
          recipientAvatar
              .getElement()
              .setAttribute("title", recipientTarget.name() + " · " + recipientTarget.email());
          recipientAvatarsDiv.add(recipientAvatar);
        });
    if (selectedRecipientTargets.size() > 3) {
      Span additionalRecipientCountSpan = new Span("+" + (selectedRecipientTargets.size() - 3));
      additionalRecipientCountSpan.addClassName("cosmos-notification-compose-recipient-more");
      recipientAvatarsDiv.add(additionalRecipientCountSpan);
    }

    Div recipientListDiv = new Div();
    recipientListDiv.addClassName("cosmos-notification-compose-recipient-list");
    selectedRecipientTargets.forEach(recipientTarget ->
        recipientListDiv.add(
            new Div(new Span(recipientTarget.name()), new Span(recipientTarget.email()))));
    Details recipientDetails = buildPreviewRecipientDetails(recipientListDiv, recipientAvatarsDiv);

    Span previewSourceSpan = new Span(messageProperties.getNotificationsSourceAdministration());
    previewSourceSpan.addClassNames(
        "cosmos-notification-preview-source", "portal-notification-source-admin");
    Span previewTimestampSpan =
        new Span(
            UserNotificationTexts.formatNotificationTimestamp(
                Instant.now(), UI.getCurrent().getLocale()));
    Div previewMetadataDiv = new Div(previewSourceSpan, previewTimestampSpan);
    previewMetadataDiv.addClassNames(
        "cosmos-notification-message-meta", "admin-notification-preview-meta");

    Span previewSubjectSpan = new Span(notificationTitleField.getValue());
    previewSubjectSpan.addClassName("cosmos-notification-compose-preview-subject");
    Div previewBodyDiv = new Div();
    previewBodyDiv.setText(notificationBodyField.getValue());
    previewBodyDiv.addClassName("cosmos-notification-body");
    Div messagePreviewDiv = new Div(previewMetadataDiv, previewSubjectSpan, previewBodyDiv);
    messagePreviewDiv.addClassName("cosmos-notification-compose-preview-content");

    notificationPreviewDiv.add(
        previewHeaderDiv, messagePreviewDiv, recipientAvatarsDiv, recipientDetails);
    emptyPreviewDiv.setVisible(false);
    notificationPreviewDiv.setVisible(true);
    previewStatusSpan.setText(messageProperties.getNotificationsPreviewUpdated());
    previewStatusSpan.addComponentAtIndex(0, createDecorativeSvgIcon(AppIcon.CHECK));
    previewStatusSpan.getElement().setAttribute(DATA_STATE, "success");
    previewStatusSpan.setVisible(true);
  }

  private Details buildPreviewRecipientDetails(Div recipientListDiv, Div recipientAvatarsDiv) {
    Details recipientDetails = new Details("", recipientListDiv);
    recipientDetails.setId("admin-notifications-preview-recipients");
    recipientDetails.addClassName("cosmos-notification-compose-details");
    recipientDetails.setOpened(false);

    Button recipientDetailsButton =
        new Button(
            messageProperties.getNotificationsRecipientDetails(), AppIcon.ANGLE_DOWN.create());
    recipientDetailsButton.setId("admin-notifications-preview-details");
    recipientDetailsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    recipientDetailsButton.addClassName("cosmos-notification-compose-details-toggle");
    recipientDetailsButton.setIconAfterText(true);
    recipientDetailsButton
        .getElement()
        .setAttribute("aria-controls", recipientDetails.getId().orElseThrow());
    recipientDetailsButton.getElement().setAttribute(ARIA_EXPANDED, "false");
    recipientDetailsButton.addClickListener(
        _ -> recipientDetails.setOpened(!recipientDetails.isOpened()));
    recipientDetails.addOpenedChangeListener(detailsOpenedChangeEvent -> {
      recipientDetailsButton.setIcon(
          detailsOpenedChangeEvent.isOpened()
              ? AppIcon.ANGLE_UP.create()
              : AppIcon.ANGLE_DOWN.create());
      recipientDetailsButton
          .getElement()
          .setAttribute(ARIA_EXPANDED, Boolean.toString(detailsOpenedChangeEvent.isOpened()));
    });

    recipientAvatarsDiv.add(recipientDetailsButton);
    return recipientDetails;
  }

  private void sendReviewedNotification() {
    if (!checkDeliveryAvailableForNotificationAction() || reviewedRecipientPreview == null) {
      return;
    }

    sendNotificationButton.setEnabled(false);
    try {
      userNotificationService.send(
          httpServletRequest.getSession(false),
          sendRequestUuid,
          notificationTitleField.getValue(),
          notificationBodyField.getValue(),
          reviewedRecipientPreview);

      notificationTitleField.clear();
      notificationBodyField.clear();
      notificationTitleField.setInvalid(false);
      notificationBodyField.setInvalid(false);
      clearPreview();
      historyPageIndex = 0;

      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getNotificationsSent,
              NotificationHelper.NotificationType.SUCCESS);
      loadSentNotificationHistory();
    } catch (RuntimeException failure) {
      showNotificationActionFailure(failure);
      if (reviewedRecipientPreview != null) {
        sendNotificationButton.setEnabled(true);
      }
    }
  }

  private void showNotificationActionFailure(RuntimeException failure) {
    if (failure instanceof Rejected rejected) {
      switch (rejected.problem()) {
        case INVALID_DRAFT -> {
          invalidatePreview();
          showActionErrorNotification(messageProperties::getNotificationsInvalidDraft);
        }
        case NO_RECIPIENTS -> {
          invalidatePreview();
          showActionErrorNotification(messageProperties::getNotificationsNoRecipients);
        }
        case RECIPIENTS_CHANGED -> {
          invalidatePreview();
          showActionErrorNotification(messageProperties::getNotificationsRecipientsChanged);
        }
        case DELIVERY_PAUSED -> {
          invalidatePreview();
          refreshDeliveryState();
          showActionErrorNotification(messageProperties::getNotificationsDeliveryPaused);
        }
        case REQUEST_CONFLICT -> {
          invalidatePreview();
          showActionErrorNotification(messageProperties::getNotificationsRequestConflict);
        }
      }
    } else {
      LoggerFactory.getLogger(getClass())
          .warn("Admin notification action failed", safeFailure(failure));
      showActionErrorNotification(messageProperties::getNotificationsUnavailable);
    }
  }

  private void showActionErrorNotification(Supplier<String> errorTextSupplier) {
    NotificationHelper.getInstance()
        .showNotification(errorTextSupplier, NotificationHelper.NotificationType.ERROR);
  }

  private void loadSentNotificationHistory() {
    Page<Sent> sentNotificationsPage =
        userNotificationService.sent(
            httpServletRequest.getSession(false), historyPageIndex, historyPageSize);
    if (historyPageIndex > 0 && historyPageIndex >= sentNotificationsPage.getTotalPages()) {
      historyPageIndex = Math.max(0, sentNotificationsPage.getTotalPages() - 1);
      loadSentNotificationHistory();
      return;
    }

    sentNotificationHistoryDiv.removeAll();
    if (sentNotificationsPage.isEmpty()) {
      sentNotificationHistoryDiv.add(
          new NotificationEmptyState(messageProperties.getNotificationsSentEmpty()));
    }
    for (Sent sentNotification : sentNotificationsPage) {
      sentNotificationHistoryDiv.add(buildSentNotificationRowDiv(sentNotification));
    }

    historyCountSpan.setText(
        messageProperties.getNotificationsHistoryCount(sentNotificationsPage.getTotalElements()));
    historyPagination.showPage(
        historyPageIndex,
        Math.toIntExact(sentNotificationsPage.getTotalElements()),
        historyPageSize);
  }

  private Div buildSentNotificationRowDiv(Sent sentNotification) {
    Div notificationIconDiv = new Div(createDecorativeSvgIcon(AppIcon.BELL));
    notificationIconDiv.addClassNames(PANEL_ICON, "cosmos-notification-sent-icon");

    H3 notificationTitleH3 = new H3(sentNotification.title());
    notificationTitleH3.addClassName("cosmos-notification-sent-title");
    Span notificationMetadataSpan =
        new Span(
            UserNotificationTexts.formatNotificationTimestamp(
                    sentNotification.createdAt(), UI.getCurrent().getLocale())
                + " · "
                + messageProperties.getNotificationsHistoryRecipients(
                    sentNotification.recipientCount()));
    notificationMetadataSpan.addClassName("cosmos-notification-sent-meta");
    Div sentHeadingDiv = new Div(notificationTitleH3, notificationMetadataSpan);
    sentHeadingDiv.addClassName("cosmos-notification-sent-heading");

    Span sentStatusSpan =
        new Span(
            createDecorativeSvgIcon(AppIcon.CHECK),
            new Span(messageProperties.getNotificationsSentStatus()));
    sentStatusSpan.getElement().setAttribute("theme", "badge success");
    sentStatusSpan.addClassName("cosmos-notification-sent-status");

    Div sentBodyDiv = new Div();
    sentBodyDiv.setText(sentNotification.body());
    sentBodyDiv.addClassNames("cosmos-notification-body", "cosmos-notification-sent-body");
    Div sentContentHeadingDiv =
        new Div(
            createDecorativeSvgIcon(AppIcon.MESSAGE),
            new Span(messageProperties.getNotificationsMessageContent()));
    sentContentHeadingDiv.addClassName("admin-history-content-heading");
    Div deliveryNoteDiv =
        new Div(
            new Span(messageProperties.getNotificationsHistoryDelivery()),
            new Span(messageProperties.getNotificationsHistoryImmutableLog()));
    deliveryNoteDiv.addClassName("admin-history-note");
    Div sentDetailContentDiv = new Div(sentContentHeadingDiv, sentBodyDiv, deliveryNoteDiv);
    sentDetailContentDiv.addClassName("cosmos-notification-sent-content");

    Details sentNotificationDetails = new Details("", sentDetailContentDiv);
    sentNotificationDetails.setId("admin-notifications-sent-detail-" + sentNotification.id());
    sentNotificationDetails.addClassName("cosmos-notification-sent-details");

    Button toggleDetailsButton =
        new Button(messageProperties.getNotificationsHistoryDetails(), AppIcon.ANGLE_DOWN.create());
    toggleDetailsButton.setIconAfterText(true);
    toggleDetailsButton.addClassName("cosmos-notification-sent-toggle");
    toggleDetailsButton
        .getElement()
        .setAttribute("aria-controls", sentNotificationDetails.getId().orElseThrow());
    toggleDetailsButton.getElement().setAttribute(ARIA_EXPANDED, "false");
    toggleDetailsButton.setAriaLabel(
        messageProperties.getNotificationsRecipientDetails() + ": " + sentNotification.title());
    toggleDetailsButton.addClickListener(
        _ -> sentNotificationDetails.setOpened(!sentNotificationDetails.isOpened()));
    sentNotificationDetails.addOpenedChangeListener(detailsOpenedChangeEvent -> {
      String detailsLabelText = messageProperties.getNotificationsHistoryDetails();
      toggleDetailsButton.setText(detailsLabelText);
      toggleDetailsButton.setAriaLabel(detailsLabelText + ": " + sentNotification.title());
      toggleDetailsButton.setIcon(
          detailsOpenedChangeEvent.isOpened()
              ? AppIcon.ANGLE_UP.create()
              : AppIcon.ANGLE_DOWN.create());
      toggleDetailsButton
          .getElement()
          .setAttribute(ARIA_EXPANDED, Boolean.toString(detailsOpenedChangeEvent.isOpened()));
    });

    Div sentActionsDiv = new Div(sentStatusSpan, toggleDetailsButton);
    sentActionsDiv.addClassName("cosmos-notification-sent-actions");
    Div sentRowDiv =
        new Div(notificationIconDiv, sentHeadingDiv, sentActionsDiv, sentNotificationDetails);
    sentRowDiv.addClassName("cosmos-notification-sent-row");
    return sentRowDiv;
  }

  public void refreshTexts() {
    notificationAdminBreadcrumbSpan.setText(
        messageProperties.getAdminHeaderTitle()
            + " / "
            + messageProperties.getNotificationsTitle());
    notificationAdminTitleSpan.setText(messageProperties.getNotificationsTitle());
    notificationAdminDescriptionSpan.setText(
        messageProperties.getNotificationsAdminPageDescription());

    emptyPreviewSpan.setText(messageProperties.getNotificationsPreview());
    previewTitleH2.setText(messageProperties.getNotificationsPreviewTitle());
    previewDescriptionSpan.setText(messageProperties.getNotificationsReviewAgain());
    previewBeforeSendHintSpan.setText(messageProperties.getNotificationsBeforeSendHint());

    composeHeadingH2.setText(messageProperties.getNotificationsComposeTitle());
    deliveryLabelSpan.setText(messageProperties.getNotificationsInternal());
    refreshDeliveryState();
    deliveryInfoSpan.setText(messageProperties.getNotificationsDeliveryInfo());
    everyoneHintSpan.setText(messageProperties.getNotificationsEveryoneHint());
    refreshNotificationCharacterCounts();
    refreshRecipientSelectionControls();
    composeDescriptionSpan.setText(messageProperties.getNotificationsAdminDescription());
    everyoneCaptionSpan.setText(messageProperties.getNotificationsEveryone());
    recipientComboBox.setLabel(messageProperties.getNotificationsRecipients());
    everyoneRecipientsTextField.setLabel(messageProperties.getNotificationsRecipients());
    notificationTitleField.setLabel(messageProperties.getNotificationsSubject());
    notificationBodyField.setLabel(messageProperties.getNotificationsBody());
    recipientComboBox.setErrorMessage(messageProperties.getNotificationsNoRecipients());
    everyoneRecipientsTextField.setErrorMessage(messageProperties.getNotificationsNoRecipients());
    notificationTitleField.setErrorMessage(messageProperties.getNotificationsInvalidSubject());
    notificationBodyField.setErrorMessage(messageProperties.getNotificationsInvalidBody());
    previewButton.setText(messageProperties.getNotificationsPreview());
    sendNotificationButton.setText(messageProperties.getNotificationsSend());
    recipientRecheckHintSpan.setText(messageProperties.getNotificationsRecheckHint());

    historyTitleH2.setText(messageProperties.getNotificationsSentHistory());
    historyDescriptionSpan.setText(messageProperties.getNotificationsHistoryDescription());
    historyPagination.refreshTexts();

    if (isAttached()) {
      clearPreview();
      loadSentNotificationHistory();
    }
  }
}
