package de.derpeterson.app.views.admin;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.MaintenanceService.State;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.validation.UserInputRules;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminMailTestSection extends VerticalLayout {

  private static final Logger logger = LoggerFactory.getLogger(AdminMailTestSection.class);

  private final transient MessageProperties messageProperties;
  private final transient EmailService emailService;
  private final transient MaintenanceService maintenance;
  private final VerticalLayout mailLayout;
  private final Div deliveryMeta = new Div();
  private transient Registration maintenancePolling;
  private int previousPollInterval;
  private int installedPollInterval;

  private final Span mailTestBreadcrumb;
  private final Span mailTestTitle;
  private final H2 mailCardTitle;
  private final Span mailCardDescription;
  private final Span mailCardBadge;
  private final Span mailSendHint;
  private final Span mailTestDescription;
  private final EmailField testRecipientField;
  private final TextField testSubjectField;
  private final TextArea testBodyField;
  private final Button sendTestMailButton;
  private final ConfigInfoButton recipientInfoButton;
  private final ConfigInfoButton subjectInfoButton;
  private final ConfigInfoButton bodyInfoButton;

  public AdminMailTestSection(
      MessageProperties messageProperties,
      EmailService emailService,
      MaintenanceService maintenance,
      UserDetails authenticatedUser) {
    this.messageProperties = messageProperties;
    this.emailService = emailService;
    this.maintenance = maintenance;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    addClassNames("cosmos-admin-section", "cosmos-admin-section--mail");

    mailTestBreadcrumb = new Span();
    mailTestTitle = new Span();
    mailCardTitle = new H2();
    mailCardTitle.addClassName("cosmos-panel-title");
    mailCardDescription = new Span();
    mailCardDescription.addClassName("cosmos-panel-description");
    mailCardBadge = new Span();
    Span deliveryDot = new Span();
    deliveryDot.addClassName("cosmos-mail-delivery-dot");
    deliveryDot.getElement().setAttribute("aria-hidden", "true");
    deliveryMeta.add(deliveryDot, mailCardBadge);
    deliveryMeta.addClassNames("cosmos-panel-meta", "cosmos-mail-delivery");
    deliveryMeta.getElement().setAttribute("role", "status");
    mailSendHint = new Span();

    mailTestDescription = new Span();
    mailTestDescription.addClassNames(LumoUtility.TextColor.SECONDARY);

    testRecipientField = new EmailField();
    testRecipientField.setPattern(UserInputRules.EMAIL_REGEX);
    testRecipientField.setClearButtonVisible(true);
    testRecipientField.setWidthFull();
    testRecipientField.setRequiredIndicatorVisible(true);
    testRecipientField.setPrefixComponent(VaadinIcon.ENVELOPE.create());

    if (authenticatedUser != null) {
      testRecipientField.setValue(authenticatedUser.getUsername());
    }

    testSubjectField = new TextField();
    testSubjectField.setWidthFull();
    testSubjectField.setRequiredIndicatorVisible(true);
    testSubjectField.setPrefixComponent(VaadinIcon.FONT.create());

    testBodyField = new TextArea();
    testBodyField.setWidthFull();
    testBodyField.setRequiredIndicatorVisible(true);
    testBodyField.addClassName("cosmos-mail-body-field");

    recipientInfoButton =
        new ConfigInfoButton(
            () ->
                buildMailTooltip(
                    messageProperties.getAdminMailRecipientHint(),
                    messageProperties.getAdminMailValidationRecipient()));
    subjectInfoButton =
        new ConfigInfoButton(
            () ->
                buildMailTooltip(
                    messageProperties.getAdminMailSubjectHint(),
                    messageProperties.getAdminMailValidationSubject()));
    bodyInfoButton =
        new ConfigInfoButton(
            () ->
                buildMailTooltip(
                    messageProperties.getAdminMailBodyHint(),
                    messageProperties.getAdminMailValidationBody()));

    sendTestMailButton = new Button();
    sendTestMailButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
    sendTestMailButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendTestMailButton.addClickListener(clickEvent -> sendTestMail());
    // Enter im Nachrichtentext bleibt ein Zeilenumbruch. Navigation und Info-Buttons
    // dürfen ebenfalls keinen Versand auslösen.
    sendTestMailButton
        .addClickShortcut(Key.ENTER)
        .listenOn(testRecipientField, testSubjectField)
        .resetFocusOnActiveElement();

    mailLayout = createMailCardContent();
    CosmosCardComponent mailCard = new CosmosCardComponent(mailLayout);
    mailCard.addClassNames("cosmos-admin-form-card", "cosmos-panel-card", "cosmos-mail-card");
    add(
        new CosmosSectionHeaderComponent(mailTestBreadcrumb, mailTestTitle, mailTestDescription),
        mailCard);
    refreshDeliveryState();
    addAttachListener(e -> startMaintenancePolling(e.getUI()));
    addDetachListener(e -> stopMaintenancePolling(e.getUI()));
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
    State state = maintenance.state();
    boolean open = state == MaintenanceService.State.OPEN;
    mailLayout.setEnabled(open);
    mailLayout.getElement().setAttribute("aria-disabled", Boolean.toString(!open));
    deliveryMeta.getElement().setAttribute("data-state", state.name().toLowerCase(Locale.ROOT));
    String status =
        switch (state) {
          case OPEN -> messageProperties.getMaintenanceStatusOnline();
          case ACTIVE -> messageProperties.getMaintenanceStatusActive();
          case UNAVAILABLE -> messageProperties.getMaintenanceStatusUnavailable();
        };
    deliveryMeta
        .getElement()
        .setAttribute("aria-label", messageProperties.getAdminMailCardBadge() + ": " + status);
  }

  private VerticalLayout createMailCardContent() {
    Icon envelope = VaadinIcon.ENVELOPE.create();
    envelope.getElement().setAttribute("aria-hidden", "true");
    Div iconBadge = new Div(envelope);
    iconBadge.addClassName("cosmos-panel-icon");
    Div heading = new Div(mailCardTitle, mailCardDescription);
    heading.addClassName("cosmos-panel-heading");
    Div header = new Div(iconBadge, heading, deliveryMeta);
    header.addClassName("cosmos-panel-header");

    VerticalLayout fields =
        new VerticalLayout(
            createMailFieldRow(testRecipientField, recipientInfoButton),
            createMailFieldRow(testSubjectField, subjectInfoButton),
            createMailFieldRow(testBodyField, bodyInfoButton));
    fields.setPadding(false);
    fields.setSpacing(false);
    fields.setWidthFull();
    fields.addClassName("cosmos-mail-fields");

    Icon hintIcon = VaadinIcon.INFO_CIRCLE.create();
    hintIcon.getElement().setAttribute("aria-hidden", "true");
    Div hint = new Div(hintIcon, mailSendHint);
    hint.addClassName("cosmos-panel-hint");
    Div actions = new Div(sendTestMailButton);
    actions.addClassName("cosmos-panel-actions");
    Div footer = new Div(hint, actions);
    footer.addClassName("cosmos-panel-footer");

    VerticalLayout content = new VerticalLayout(header, fields, footer);
    content.setPadding(false);
    content.setSpacing(false);
    content.setWidthFull();
    content.setAlignItems(Alignment.START);
    content.addClassName("cosmos-admin-form");
    return content;
  }

  private Div createMailFieldRow(Component input, ConfigInfoButton infoButton) {
    Div row = new Div(input, infoButton);
    row.addClassNames("cosmos-config-input-row", "cosmos-mail-field-row");
    return row;
  }

  private String buildMailTooltip(String description, String validation) {
    return description
        + "\n\n"
        + messageProperties.getAdminConfigInputRules()
        + ":\n• "
        + validation;
  }

  public void refreshTexts() {
    mailTestBreadcrumb.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminMailTitle());
    mailCardTitle.setText(messageProperties.getAdminMailCardTitle());
    mailCardDescription.setText(messageProperties.getAdminMailCardDescription());
    mailCardBadge.setText(messageProperties.getAdminMailCardBadge());
    refreshDeliveryState();
    mailSendHint.setText(messageProperties.getAdminMailSendHint());
    recipientInfoButton.refreshText();
    subjectInfoButton.refreshText();
    bodyInfoButton.refreshText();
    recipientInfoButton
        .getElement()
        .setAttribute("aria-label", messageProperties.getAdminMailRecipient());
    subjectInfoButton
        .getElement()
        .setAttribute("aria-label", messageProperties.getAdminMailSubject());
    bodyInfoButton.getElement().setAttribute("aria-label", messageProperties.getAdminMailBody());
    mailTestTitle.setText(messageProperties.getAdminMailTitle());
    mailTestDescription.setText(messageProperties.getAdminMailDescription());
    sendTestMailButton.setText(messageProperties.getAdminMailSend());

    testRecipientField.setLabel(messageProperties.getAdminMailRecipient());
    testSubjectField.setLabel(messageProperties.getAdminMailSubject());
    testBodyField.setLabel(messageProperties.getAdminMailBody());

    if (testSubjectField.isEmpty()) {
      testSubjectField.setValue(messageProperties.getAdminMailDefaultSubject());
    }

    if (testBodyField.isEmpty()) {
      testBodyField.setValue(messageProperties.getAdminMailDefaultBody());
    }
  }

  private void sendTestMail() {
    refreshDeliveryState();
    if (!mailLayout.isEnabled()) {
      return;
    }
    String recipient = Optional.ofNullable(testRecipientField.getValue()).orElse("").trim();
    String subject = Optional.ofNullable(testSubjectField.getValue()).orElse("").trim();
    String body = Optional.ofNullable(testBodyField.getValue()).orElse("").trim();

    // Flow-E-Mail-Regel: lokale ASCII-Buchstaben/Ziffern/_.-+, @ und Domain mit Punkt und
    // mindestens zweistelligem Endteil.
    // Dieselbe bestehende Pattern-Regel liegt der Benutzerprüfung in UserInputRules zugrunde.
    if (recipient.isBlank() || !recipient.matches(EmailValidator.PATTERN)) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminMailValidationRecipient,
              -1,
              NotificationHelper.NotificationType.ERROR);
      testRecipientField.setInvalid(true);
      return;
    }

    if (subject.isBlank()) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminMailValidationSubject,
              -1,
              NotificationHelper.NotificationType.ERROR);
      testSubjectField.setInvalid(true);
      return;
    }

    if (body.isBlank()) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminMailValidationBody,
              -1,
              NotificationHelper.NotificationType.ERROR);
      testBodyField.setInvalid(true);
      return;
    }

    testRecipientField.setInvalid(false);
    testSubjectField.setInvalid(false);
    testBodyField.setInvalid(false);

    try {
      emailService.sendAdminEmail(recipient, subject, body);
      logger.info("Test email accepted by transport");
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getAdminMailSuccess,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (MailException ex) {
      logger.error("Test email dispatch failed", safeFailure(ex));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminMailError,
              -1,
              NotificationHelper.NotificationType.ERROR);
    }
  }
}
