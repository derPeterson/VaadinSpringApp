package de.derpeterson.app.views.admin;

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
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
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
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

/** Superadmin-Entwurf mit serverseitiger Empfängervorschau und dauerhaftem Request-Schlüssel. */
public class AdminUserNotificationsSection extends VerticalLayout {
  private final transient MessageProperties texts;
  private final transient UserNotificationService service;
  private final transient HttpServletRequest request;
  private final Span breadcrumb = new Span();
  private final Span pageTitle = new Span();
  private final Span pageDescription = new Span();
  private final H2 heading = new H2();
  private final Span description = new Span();
  private final Span deliveryLabel = new Span();
  private final Div deliveryMeta = new Div();
  private final VerticalLayout composeForm;
  private MaintenanceService.State maintenanceState;
  private transient Registration maintenancePolling;
  private int previousPollInterval;
  private int installedPollInterval;
  private final Span deliveryInfo = new Span();
  private final Span everyoneCaption = new Span();
  private final Span everyoneHint = new Span();
  private final Span titleCount = new Span();
  private final Span bodyCount = new Span();
  private final Span previewStatus = new Span();
  private List<Target> eligibleTargets = List.of();
  private final Checkbox everyone = new Checkbox();
  private final MultiSelectComboBox<Target> targets = new MultiSelectComboBox<>();
  private final TextField everyoneRecipients = new TextField();
  private final Span recipientSummary = new Span();
  private final TextField title = new TextField();
  private final TextArea body = new TextArea();
  private final Button previewButton = new Button();
  private final Button send = new Button();
  private final Div previewArea = new Div();
  private final Span hint = new Span();
  private final H2 historyTitle = new H2();
  private final Span historyDescription = new Span();
  private final Span historyCount = new Span();
  private final Div history = new Div();
  private final AdminUserPagination historyPagination;
  private int historyPageSize = 5;
  private Preview preview;
  private UUID requestId = UUID.randomUUID();
  private int page;

  public AdminUserNotificationsSection(
      MessageProperties texts, UserNotificationService service, HttpServletRequest request) {
    this.texts = texts;
    this.service = service;
    this.request = request;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    addClassNames("cosmos-admin-section", "cosmos-admin-section--notifications", "cosmos-theme");
    heading.addClassName("cosmos-panel-title");
    historyTitle.addClassName("cosmos-panel-title");
    description.addClassName("cosmos-panel-description");

    configureComposeInputs();
    composeForm = createComposeForm();
    historyPagination = createHistoryPagination();
    Div sent = createHistoryContent();
    CosmosCardComponent formCard = new CosmosCardComponent(composeForm);
    formCard.addClassNames("cosmos-admin-form-card", "cosmos-panel-card");
    CosmosCardComponent sentCard = new CosmosCardComponent(sent);
    sentCard.addClassNames("cosmos-admin-form-card", "cosmos-panel-card");
    add(
        new CosmosSectionHeaderComponent(breadcrumb, pageTitle, pageDescription),
        formCard,
        sentCard);
    refreshTexts();
    addAttachListener(e -> startMaintenancePolling(e.getUI()));
    addDetachListener(e -> stopMaintenancePolling(e.getUI()));
  }

  private void configureComposeInputs() {
    targets.setItemLabelGenerator(target -> target.name() + " · " + target.email());
    targets.setWidthFull();
    targets.setManualValidation(true);
    targets.setId("admin-notifications-recipients");
    everyone.setId("admin-notifications-everyone");
    everyoneRecipients.setId("admin-notifications-everyone-recipients");
    everyoneRecipients.setManualValidation(true);
    everyoneRecipients.setReadOnly(true);
    everyoneRecipients.setEnabled(false);
    everyoneRecipients.setWidthFull();
    title.setId("admin-notifications-title");
    body.setId("admin-notifications-body");
    title.setMaxLength(UserNotificationService.TITLE_LIMIT);
    body.setMaxLength(UserNotificationService.BODY_LIMIT);
    body.setMinRows(4);
    body.setMaxRows(10);
    title.setManualValidation(true);
    body.setManualValidation(true);
    for (var field : List.of(title, body)) {
      field.setWidthFull();
      field.setRequiredIndicatorVisible(true);
      field.setValueChangeMode(ValueChangeMode.EAGER);
      field.addValueChangeListener(
          e -> {
            refreshCounters();
            invalidatePreview();
          });
    }
    everyone.addValueChangeListener(
        e -> {
          refreshRecipientField();
          invalidatePreview();
        });
    targets.addValueChangeListener(
        e -> {
          refreshRecipientSummary();
          invalidatePreview();
        });
    previewButton.setId("admin-notifications-preview");
    previewButton.setIcon(VaadinIcon.EYE.create());
    send.setIcon(VaadinIcon.PAPERPLANE.create());
    send.setId("admin-notifications-send");
    previewButton.addClickListener(e -> preview());
    send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    send.setEnabled(false);
    send.addClickListener(e -> send());
  }

  private VerticalLayout createComposeForm() {
    Div panelIcon = new Div(decorativeIcon(VaadinIcon.PAPERPLANE));
    panelIcon.addClassName("cosmos-panel-icon");
    Div panelHeading = new Div(heading, description);
    panelHeading.addClassName("cosmos-panel-heading");
    Span deliveryDot = new Span();
    deliveryDot.addClassName("cosmos-notification-delivery-dot");
    deliveryDot.getElement().setAttribute("aria-hidden", "true");
    deliveryMeta.add(deliveryDot, deliveryLabel);
    deliveryMeta.addClassNames("cosmos-panel-meta", "cosmos-notification-delivery");
    deliveryMeta.getElement().setAttribute("role", "status");
    Div header = new Div(panelIcon, panelHeading, deliveryMeta);
    header.addClassName("cosmos-panel-header");
    Div info = new Div(decorativeIcon(VaadinIcon.INFO_CIRCLE_O), deliveryInfo);
    info.addClassName("cosmos-notification-compose-info");
    everyone.addClassName("cosmos-notification-compose-everyone");
    everyoneHint.addClassName("cosmos-notification-compose-everyone-hint");
    everyoneHint.getElement().setAttribute("aria-hidden", "true");
    VerticalLayout everyoneLabel = new VerticalLayout(everyoneCaption, everyoneHint);
    everyoneLabel.setPadding(false);
    everyoneLabel.setSpacing(false);
    everyoneLabel.setWidth(null);
    everyoneLabel.setAlignItems(FlexComponent.Alignment.START);
    everyoneLabel.addClassName("cosmos-notification-compose-everyone-label");
    everyone.setLabelComponent(everyoneLabel);
    everyone.setAriaDescribedBy(everyoneHint);
    recipientSummary.addClassName("cosmos-notification-compose-recipient-summary");
    recipientSummary.getElement().setAttribute("aria-hidden", "true");
    Div recipientField = new Div(targets, recipientSummary);
    recipientField.addClassName("cosmos-notification-compose-recipient-field");
    Div selection = new Div(everyone);
    selection.addClassName("cosmos-notification-compose-selection");
    previewArea.addClassName("cosmos-notification-compose-preview");
    previewArea.setVisible(false);
    Div footerHint = new Div(decorativeIcon(VaadinIcon.INFO_CIRCLE_O), hint);
    footerHint.addClassName("cosmos-panel-hint");
    Div actions = new Div(previewButton, send);
    actions.addClassName("cosmos-panel-actions");
    Div footer = new Div(footerHint, actions);
    footer.addClassName("cosmos-panel-footer");
    previewStatus.addClassName("cosmos-notification-compose-status");
    previewStatus.getElement().setAttribute("role", "status");
    previewStatus.setVisible(false);
    VerticalLayout form =
        new VerticalLayout(
            header,
            info,
            selection,
            recipientField,
            everyoneRecipients,
            fieldWithCounter(title, titleCount),
            fieldWithCounter(body, bodyCount),
            previewArea,
            footer);
    form.setPadding(false);
    form.setSpacing(false);
    form.setWidthFull();
    form.addClassNames("cosmos-admin-form", "cosmos-notification-compose");
    return form;
  }

  private AdminUserPagination createHistoryPagination() {
    AdminUserPagination pagination =
        new AdminUserPagination(
            texts,
            selectedPage -> {
              page = selectedPage;
              loadHistory();
            },
            selectedSize -> {
              historyPageSize = selectedSize;
              page = 0;
              loadHistory();
            },
            List.of(5, 10, 15));
    pagination.addClassName("cosmos-notification-sent-pagination");
    return pagination;
  }

  private Div createHistoryContent() {
    Div archiveIcon = new Div(decorativeIcon(VaadinIcon.ARCHIVE));
    archiveIcon.addClassName("cosmos-panel-icon");
    historyDescription.addClassName("cosmos-panel-description");
    Div archiveHeading = new Div(historyTitle, historyDescription);
    archiveHeading.addClassName("cosmos-panel-heading");
    historyCount.addClassName("cosmos-panel-meta");
    Div archiveHeader = new Div(archiveIcon, archiveHeading, historyCount);
    archiveHeader.addClassName("cosmos-panel-header");
    history.addClassName("cosmos-notification-sent-list");
    Div sent = new Div(archiveHeader, history, historyPagination);
    sent.setId("admin-notifications-sent");
    sent.addClassName("cosmos-notification-sent");
    return sent;
  }

  private void startMaintenancePolling(UI ui) {
    refreshDeliveryState();
    previousPollInterval = ui.getPollInterval();
    installedPollInterval = previousPollInterval > 0 ? Math.min(previousPollInterval, 5000) : 5000;
    ui.setPollInterval(installedPollInterval);
    maintenancePolling = ui.addPollListener(e -> refreshDeliveryState());
  }

  private void stopMaintenancePolling(UI ui) {
    if (maintenancePolling != null) {
      maintenancePolling.remove();
    }
    maintenancePolling = null;
    if (installedPollInterval != previousPollInterval
        && ui.getPollInterval() == installedPollInterval) {
      ui.setPollInterval(previousPollInterval);
    }
  }

  public void refreshDeliveryState() {
    State state = service.deliveryState();
    if (state == null) {
      state = MaintenanceService.State.UNAVAILABLE;
    }
    if (state != maintenanceState) {
      invalidatePreview();
    }
    maintenanceState = state;
    boolean open = state == MaintenanceService.State.OPEN;
    composeForm.setEnabled(open);
    composeForm.getElement().setAttribute("aria-disabled", Boolean.toString(!open));
    deliveryMeta.getElement().setAttribute("data-state", state.name().toLowerCase(Locale.ROOT));
    String status =
        switch (state) {
          case OPEN -> texts.getMaintenanceStatusOnline();
          case ACTIVE -> texts.getMaintenanceStatusActive();
          case UNAVAILABLE -> texts.getMaintenanceStatusUnavailable();
        };
    deliveryMeta
        .getElement()
        .setAttribute("aria-label", texts.getNotificationsInternal() + ": " + status);
  }

  private boolean deliveryAvailable() {
    refreshDeliveryState();
    if (maintenanceState == MaintenanceService.State.OPEN) {
      return true;
    }
    feedback(
        maintenanceState == MaintenanceService.State.ACTIVE
            ? texts::getNotificationsDeliveryPaused
            : texts::getNotificationsUnavailable);
    return false;
  }

  public void activate() {
    refreshDeliveryState();
    // Auch erneute Navigation und jede Serviceaktion prüfen die aktuellen Berechtigungen.
    eligibleTargets = List.copyOf(service.targets(request.getSession(false)));
    targets.setItems(eligibleTargets);
    refreshRecipientField();
    clearPreview();
    page = 0;
    loadHistory();
  }

  private void invalidatePreview() {
    clearValidation();
    preview = null;
    if (previewArea.isVisible()) {
      previewStatus.setText(texts.getNotificationsPreviewNeedsReview());
      previewStatus.addComponentAtIndex(0, decorativeIcon(VaadinIcon.WARNING));
      previewStatus.getElement().setAttribute("data-state", "warning");
      previewStatus.setVisible(true);
    }
    send.setEnabled(false);
    requestId = UUID.randomUUID();
  }

  private void clearPreview() {
    invalidatePreview();
    previewArea.removeAll();
    previewArea.setVisible(false);
    previewStatus.setText("");
    previewStatus.setVisible(false);
  }

  private void clearValidation() {
    title.setInvalid(false);
    body.setInvalid(false);
    targets.setInvalid(false);
    everyoneRecipients.setInvalid(false);
  }

  private boolean valid() {
    title.setInvalid(
        title.getValue().isBlank()
            || title.getValue().length() > UserNotificationService.TITLE_LIMIT);
    body.setInvalid(
        body.getValue().isBlank() || body.getValue().length() > UserNotificationService.BODY_LIMIT);
    boolean noRecipients =
        everyone.getValue() ? eligibleTargets.isEmpty() : targets.getSelectedItems().isEmpty();
    if (noRecipients) {
      markRecipientError(texts.getNotificationsNoRecipients());
    }
    if (title.isInvalid() || body.isInvalid() || noRecipients) {
      previewValidationFeedback();
      return false;
    }
    return true;
  }

  private void previewValidationFeedback() {
    NotificationHelper.getInstance()
        .showNotification(
            texts::getNotificationsPreviewFailedTitle,
            texts::getNotificationsPreviewFailedMessage,
            NotificationHelper.NotificationType.ERROR);
  }

  private void markRecipientError(String message) {
    if (everyone.getValue()) {
      everyoneRecipients.setErrorMessage(message);
      everyoneRecipients.setInvalid(true);
    } else {
      targets.setErrorMessage(message);
      targets.setInvalid(true);
    }
  }

  private void preview() {
    if (!deliveryAvailable()) {
      return;
    }
    invalidatePreview();
    if (!valid()) {
      return;
    }
    try {
      preview =
          service.preview(
              request.getSession(false),
              new Selection(
                  everyone.getValue(),
                  targets.getSelectedItems().stream()
                      .map(Target::id)
                      .collect(java.util.stream.Collectors.toSet())));
      renderPreview();
      send.setEnabled(true);
    } catch (RuntimeException failure) {
      invalidatePreview();
      if (failure instanceof Rejected rejected
          && (rejected.problem() == UserNotificationService.Problem.NO_RECIPIENTS
              || rejected.problem() == UserNotificationService.Problem.RECIPIENTS_CHANGED)) {
        markRecipientError(
            rejected.problem() == UserNotificationService.Problem.NO_RECIPIENTS
                ? texts.getNotificationsNoRecipients()
                : texts.getNotificationsRecipientsChanged());
        previewValidationFeedback();
      } else {
        handle(failure);
      }
    }
  }

  private static Icon decorativeIcon(VaadinIcon type) {
    Icon icon = type.create();
    icon.getElement().setAttribute("aria-hidden", "true");
    return icon;
  }

  private static Div fieldWithCounter(Component field, Span counter) {
    counter.addClassName("cosmos-notification-compose-counter");
    counter.getElement().setAttribute("aria-hidden", "true");
    Div wrapper = new Div(field, counter);
    wrapper.addClassName("cosmos-notification-compose-field");
    return wrapper;
  }

  private void refreshCounters() {
    titleCount.setText(title.getValue().length() + " / " + UserNotificationService.TITLE_LIMIT);
    bodyCount.setText(body.getValue().length() + " / " + UserNotificationService.BODY_LIMIT);
  }

  private void refreshRecipientField() {
    boolean all = everyone.getValue();
    targets.setEnabled(!all);
    targets.setVisible(!all);
    targets.getParent().ifPresent(wrapper -> wrapper.setVisible(!all));
    refreshRecipientSummary();
    everyoneRecipients.setVisible(all);
    everyoneRecipients.setValue(texts.getNotificationsEveryonePlaceholder(eligibleTargets.size()));
    targets.setPlaceholder(
        all
            ? texts.getNotificationsEveryonePlaceholder(eligibleTargets.size())
            : texts.getNotificationsSelectRecipients());
  }

  private void refreshRecipientSummary() {
    recipientSummary.setText(
        texts.getNotificationsSelectedUsers(targets.getSelectedItems().size()));
  }

  private void renderPreview() {
    previewArea.removeAll();
    previewArea.getElement().setAttribute("aria-label", texts.getNotificationsReviewedPreview());
    Span caption = new Span(texts.getNotificationsReviewedPreview());
    Div captionRow = new Div(decorativeIcon(VaadinIcon.EYE), caption);
    captionRow.addClassName("cosmos-notification-compose-preview-title");
    Div previewHeader = new Div(captionRow, previewStatus);
    previewHeader.addClassName("cosmos-notification-compose-preview-header");

    HashSet<Long> selected = new HashSet<>(preview.ids());
    List<Target> selectedTargets =
        service.targets(request.getSession(false)).stream()
            .filter(t -> selected.contains(t.id()))
            .toList();
    Span recipientLabel = new Span(texts.getNotificationsRecipients());
    recipientLabel.addClassName("cosmos-panel-meta");
    Div chips = new Div(recipientLabel);
    chips.addClassName("cosmos-notification-compose-recipients");
    selectedTargets.stream()
        .limit(3)
        .forEach(
            t -> {
              Avatar avatar = new Avatar(t.name());
              avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
              Span chip = new Span(avatar, new Span(t.name()));
              chip.getElement().setAttribute("theme", "badge");
              chip.addClassNames("cosmos-admin-role-chip", "cosmos-notification-compose-recipient");
              chips.add(chip);
            });
    if (selectedTargets.size() > 3) {
      Span moreRecipients = new Span("+" + (selectedTargets.size() - 3));
      moreRecipients.addClassName("cosmos-notification-compose-recipient-more");
      chips.add(moreRecipients);
    }
    Div recipientList = new Div();
    recipientList.addClassName("cosmos-notification-compose-recipient-list");
    selectedTargets.forEach(
        t -> recipientList.add(new Div(new Span(t.name()), new Span(t.email()))));
    Details details = new Details("", recipientList);
    details.setId("admin-notifications-preview-recipients");
    details.addClassName("cosmos-notification-compose-details");
    details.setOpened(false);
    Button detailsButton =
        new Button(texts.getNotificationsRecipientDetails(), VaadinIcon.ANGLE_DOWN.create());
    detailsButton.setId("admin-notifications-preview-details");
    detailsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    detailsButton.addClassName("cosmos-notification-compose-details-toggle");
    detailsButton.setIconAfterText(true);
    detailsButton.getElement().setAttribute("aria-controls", details.getId().orElseThrow());
    detailsButton.getElement().setAttribute("aria-expanded", "false");
    detailsButton.addClickListener(e -> details.setOpened(!details.isOpened()));
    details.addOpenedChangeListener(
        e -> {
          detailsButton.setText(
              e.isOpened()
                  ? texts.getNotificationsHideRecipientDetails()
                  : texts.getNotificationsRecipientDetails());
          detailsButton.setIcon(
              e.isOpened() ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
          detailsButton.getElement().setAttribute("aria-expanded", Boolean.toString(e.isOpened()));
        });
    chips.add(detailsButton);

    Span contentLabel = new Span(texts.getNotificationsContentPreview());
    contentLabel.addClassName("cosmos-panel-meta");
    Span subject = new Span(title.getValue());
    subject.addClassName("cosmos-notification-compose-preview-subject");
    Div content = new Div();
    content.setText(body.getValue());
    content.addClassName("cosmos-notification-body");
    Div messagePreview = new Div(contentLabel, subject, content);
    messagePreview.addClassName("cosmos-notification-compose-preview-content");
    previewArea.add(previewHeader, chips, details, messagePreview);
    previewArea.setVisible(true);
    previewStatus.setText(texts.getNotificationsPreviewUpdated());
    previewStatus.addComponentAtIndex(0, decorativeIcon(VaadinIcon.CHECK_CIRCLE));
    previewStatus.getElement().setAttribute("data-state", "success");
    previewStatus.setVisible(true);
  }

  private void send() {
    if (!deliveryAvailable() || preview == null) {
      return;
    }
    send.setEnabled(false);
    try {
      service.send(
          request.getSession(false), requestId, title.getValue(), body.getValue(), preview);
      title.clear();
      body.clear();
      title.setInvalid(false);
      body.setInvalid(false);
      clearPreview();
      page = 0;
      NotificationHelper.getInstance()
          .showNotification(
              texts::getBaseSuccessTitle,
              texts::getNotificationsSent,
              NotificationHelper.NotificationType.SUCCESS);
      loadHistory();
    } catch (RuntimeException failure) {
      handle(failure);
      if (preview != null) {
        send.setEnabled(true);
      }
    }
  }

  private void handle(RuntimeException failure) {
    if (failure instanceof Rejected rejected) {
      switch (rejected.problem()) {
        case INVALID_DRAFT -> {
          invalidatePreview();
          feedback(texts::getNotificationsInvalidDraft);
        }
        case NO_RECIPIENTS -> {
          invalidatePreview();
          feedback(texts::getNotificationsNoRecipients);
        }
        case RECIPIENTS_CHANGED -> {
          invalidatePreview();
          feedback(texts::getNotificationsRecipientsChanged);
        }
        case DELIVERY_PAUSED -> {
          invalidatePreview();
          refreshDeliveryState();
          feedback(texts::getNotificationsDeliveryPaused);
        }
        case REQUEST_CONFLICT -> {
          invalidatePreview();
          feedback(texts::getNotificationsRequestConflict);
        }
      }
    } else {
      LoggerFactory.getLogger(getClass()).warn("Admin notification action failed", failure);
      feedback(texts::getNotificationsUnavailable);
    }
  }

  private void feedback(java.util.function.Supplier<String> text) {
    NotificationHelper.getInstance()
        .showNotification(text, NotificationHelper.NotificationType.ERROR);
  }

  private void loadHistory() {
    Page<Sent> result = service.sent(request.getSession(false), page, historyPageSize);
    if (page > 0 && page >= result.getTotalPages()) {
      page = Math.max(0, result.getTotalPages() - 1);
      loadHistory();
      return;
    }
    history.removeAll();
    if (result.isEmpty()) {
      Span empty = new Span(texts.getNotificationsEmpty());
      empty.addClassName("cosmos-notification-sent-empty");
      history.add(empty);
    }
    for (Sent item : result) {
      history.add(sentRow(item));
    }
    historyCount.setText(texts.getNotificationsHistoryCount(result.getTotalElements()));
    historyPagination.showPage(page, Math.toIntExact(result.getTotalElements()), historyPageSize);
  }

  private Div sentRow(Sent item) {
    Div icon = new Div(decorativeIcon(VaadinIcon.PAPERPLANE));
    icon.addClassNames("cosmos-panel-icon", "cosmos-notification-sent-icon");
    H3 subject = new H3(item.title());
    subject.addClassName("cosmos-notification-sent-title");
    Span metadata =
        new Span(
            UserNotificationTexts.time(item.createdAt(), UI.getCurrent().getLocale())
                + " · "
                + texts.getNotificationsHistoryRecipients(item.recipientCount()));
    metadata.addClassName("cosmos-notification-sent-meta");
    Div heading = new Div(subject, metadata);
    heading.addClassName("cosmos-notification-sent-heading");
    Span status =
        new Span(decorativeIcon(VaadinIcon.CHECK), new Span(texts.getNotificationsSentStatus()));
    status.getElement().setAttribute("theme", "badge success");
    status.addClassName("cosmos-notification-sent-status");
    Div content = new Div();
    content.setText(item.body());
    content.addClassNames("cosmos-notification-body", "cosmos-notification-sent-body");
    H4 detailSubject = new H4(item.title());
    detailSubject.addClassName("cosmos-notification-sent-detail-title");
    Span detailMeta =
        new Span(
            texts.getNotificationsHistoryRecipients(item.recipientCount())
                + " · "
                + UserNotificationTexts.time(item.createdAt(), UI.getCurrent().getLocale()));
    detailMeta.addClassName("cosmos-notification-sent-detail-meta");
    Div detailContent = new Div(detailSubject, content, detailMeta);
    detailContent.addClassName("cosmos-notification-sent-content");
    Details details = new Details("", detailContent);
    details.setId("admin-notifications-sent-detail-" + item.id());
    details.addClassName("cosmos-notification-sent-details");
    Button toggle =
        new Button(texts.getNotificationsRecipientDetails(), VaadinIcon.ANGLE_DOWN.create());
    toggle.setIconAfterText(true);
    toggle.addClassName("cosmos-notification-sent-toggle");
    toggle.getElement().setAttribute("aria-controls", details.getId().orElseThrow());
    toggle.getElement().setAttribute("aria-expanded", "false");
    toggle.setAriaLabel(texts.getNotificationsRecipientDetails() + ": " + item.title());
    toggle.addClickListener(e -> details.setOpened(!details.isOpened()));
    details.addOpenedChangeListener(
        e -> {
          String label =
              e.isOpened()
                  ? texts.getNotificationsClose()
                  : texts.getNotificationsRecipientDetails();
          toggle.setText(label);
          toggle.setAriaLabel(label + ": " + item.title());
          toggle.setIcon(
              e.isOpened() ? VaadinIcon.ANGLE_UP.create() : VaadinIcon.ANGLE_DOWN.create());
          toggle.getElement().setAttribute("aria-expanded", Boolean.toString(e.isOpened()));
        });
    Div actions = new Div(status, toggle);
    actions.addClassName("cosmos-notification-sent-actions");
    Div row = new Div(icon, heading, actions, details);
    row.addClassName("cosmos-notification-sent-row");
    return row;
  }

  public void refreshTexts() {
    breadcrumb.setText(texts.getAdminHeaderTitle() + " / " + texts.getNotificationsTitle());
    pageTitle.setText(texts.getNotificationsTitle());
    pageDescription.setText(texts.getNotificationsAdminPageDescription());
    heading.setText(texts.getNotificationsComposeTitle());
    deliveryLabel.setText(texts.getNotificationsInternal());
    refreshDeliveryState();
    deliveryInfo.setText(texts.getNotificationsDeliveryInfo());
    everyoneHint.setText(texts.getNotificationsEveryoneHint());
    refreshCounters();
    refreshRecipientField();
    description.setText(texts.getNotificationsAdminDescription());
    everyoneCaption.setText(texts.getNotificationsEveryone());
    targets.setLabel(texts.getNotificationsRecipients());
    everyoneRecipients.setLabel(texts.getNotificationsRecipients());
    title.setLabel(texts.getNotificationsSubject());
    body.setLabel(texts.getNotificationsBody());
    targets.setErrorMessage(texts.getNotificationsNoRecipients());
    everyoneRecipients.setErrorMessage(texts.getNotificationsNoRecipients());
    title.setErrorMessage(texts.getNotificationsInvalidSubject());
    body.setErrorMessage(texts.getNotificationsInvalidBody());
    previewButton.setText(texts.getNotificationsPreview());
    send.setText(texts.getNotificationsSend());
    hint.setText(texts.getNotificationsPreviewHint());
    historyTitle.setText(texts.getNotificationsSentHistory());
    historyDescription.setText(texts.getNotificationsHistoryDescription());
    historyPagination.refreshTexts();
    if (isAttached()) {
      clearPreview();
      loadHistory();
    }
  }
}
