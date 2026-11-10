package de.derpeterson.app.views.admin;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_CARD;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADER;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.security.MaintenanceService;
import de.derpeterson.app.security.MaintenanceService.State;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.ui.components.AdminSidePanel;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.ConfigInfoButton;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosSectionHeaderComponent;
import de.derpeterson.app.validation.UserInputRules;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UserDetails;

public class AdminMailTestSection extends VerticalLayout {
  private static final String ARIA_HIDDEN = "aria-hidden";
  private static final String ARIA_LABEL = "aria-label";

  private static final Logger logger = LoggerFactory.getLogger(AdminMailTestSection.class);

  private record FieldHelp(
      Supplier<String> title, Supplier<String> description, Supplier<String> rule) {}

  private final transient MessageProperties messageProperties;
  private final transient EmailService emailService;
  private final transient MaintenanceService maintenanceService;
  private final VerticalLayout mailCardContentLayout;
  private final Div deliveryStatusDiv = new Div();
  private final Tooltip deliveryTooltip = Tooltip.forComponent(deliveryStatusDiv);
  private transient Registration maintenancePollRegistration;
  private int previousPollInterval;
  private int installedPollInterval;

  private final Span mailTestBreadcrumbSpan;
  private final Span mailTestTitleSpan;
  private final H2 mailCardTitleH2;
  private final Span mailCardDescriptionSpan;
  private final Span mailDeliveryStatusSpan;
  private final Span mailSendHintSpan;
  private final Span mailBodyHintSpan = new Span();
  private final Span mailTestDescriptionSpan;
  private final EmailField testRecipientField;
  private final TextField testSubjectField;
  private final TextArea testBodyField;
  private final Button sendTestMailButton;
  private final Anchor smtpLink =
      new Anchor(AppRoute.ADMIN.getRoute() + "?section=configuration&group=smtp", "");
  private final Span smtpLinkCaptionSpan = new Span();
  private final ConfigInfoButton recipientInfoButton;
  private final ConfigInfoButton subjectInfoButton;
  private final ConfigInfoButton bodyInfoButton;
  private final AdminSidePanel mailFieldHelpPanel;
  private final Button closeHelpButton = new Button(AppIcon.CLOSE.create());
  private transient FieldHelp selectedMailFieldHelp;

  public AdminMailTestSection(
      MessageProperties messageProperties,
      EmailService emailService,
      MaintenanceService maintenanceService,
      UserDetails authenticatedUser) {
    this.messageProperties = messageProperties;
    this.emailService = emailService;
    this.maintenanceService = maintenanceService;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    addClassNames("cosmos-admin-section", "cosmos-admin-section--mail");

    mailTestBreadcrumbSpan = new Span();
    mailTestTitleSpan = new Span();
    mailCardTitleH2 = new H2();
    mailCardTitleH2.addClassName(PANEL_TITLE);
    mailCardDescriptionSpan = new Span();
    mailCardDescriptionSpan.addClassName(PANEL_DESCRIPTION);
    mailDeliveryStatusSpan = new Span();

    Span deliveryStatusDotSpan = new Span();
    deliveryStatusDotSpan.addClassName("cosmos-mail-delivery-dot");
    deliveryStatusDotSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
    deliveryStatusDiv.add(deliveryStatusDotSpan, mailDeliveryStatusSpan);
    deliveryStatusDiv.addClassNames("cosmos-panel-meta", "cosmos-mail-delivery");
    deliveryStatusDiv.getElement().setAttribute("role", "status");
    mailSendHintSpan = new Span();

    mailTestDescriptionSpan = new Span();
    mailTestDescriptionSpan.addClassNames(LumoUtility.TextColor.SECONDARY);

    testRecipientField = new EmailField();
    testRecipientField.setPattern(UserInputRules.EMAIL_REGEX);
    testRecipientField.setClearButtonVisible(true);
    testRecipientField.setWidthFull();
    testRecipientField.setRequiredIndicatorVisible(true);

    if (authenticatedUser != null) {
      testRecipientField.setValue(authenticatedUser.getUsername());
    }

    testSubjectField = new TextField();
    testSubjectField.setWidthFull();
    testSubjectField.setRequiredIndicatorVisible(true);

    testBodyField = new TextArea();
    testBodyField.setWidthFull();
    testBodyField.setRequiredIndicatorVisible(true);
    testBodyField.addClassName("cosmos-mail-body-field");

    mailFieldHelpPanel = new AdminSidePanel(messageProperties, AppIcon.INFO_CIRCLE);
    mailFieldHelpPanel.addClassName("admin-config-help");
    mailFieldHelpPanel.setId("admin-mail-field-help");
    closeHelpButton.setWidthFull();
    closeHelpButton.addClickListener(_ -> mailFieldHelpPanel.close());
    mailFieldHelpPanel.getFooter().add(closeHelpButton);

    recipientInfoButton =
        buildMailFieldInfoButton(
            new FieldHelp(
                messageProperties::getAdminMailRecipient,
                messageProperties::getAdminMailRecipientHint,
                messageProperties::getAdminMailValidationRecipient));
    subjectInfoButton =
        buildMailFieldInfoButton(
            new FieldHelp(
                messageProperties::getAdminMailSubject,
                messageProperties::getAdminMailSubjectHint,
                messageProperties::getAdminMailValidationSubject));
    bodyInfoButton =
        buildMailFieldInfoButton(
            new FieldHelp(
                messageProperties::getAdminMailBody,
                messageProperties::getAdminMailBodyHint,
                messageProperties::getAdminMailValidationBody));

    sendTestMailButton = new Button();
    sendTestMailButton.setPrefixComponent(AppIcon.PAPERPLANE.create());
    sendTestMailButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendTestMailButton.addClickListener(_ -> sendTestMail());

    // Enter im Nachrichtentext bleibt ein Zeilenumbruch. Navigation und Info-Buttons
    // dürfen ebenfalls keinen Versand auslösen.
    sendTestMailButton
        .addClickShortcut(Key.ENTER)
        .listenOn(testRecipientField, testSubjectField)
        .resetFocusOnActiveElement();

    mailCardContentLayout = buildMailCardContentLayout();
    CosmosCardComponent mailCard = new CosmosCardComponent(mailCardContentLayout);
    mailCard.addClassNames("cosmos-admin-form-card", PANEL_CARD, "cosmos-mail-card");
    add(
        new CosmosSectionHeaderComponent(
            mailTestBreadcrumbSpan, mailTestTitleSpan, mailTestDescriptionSpan),
        mailCard,
        mailFieldHelpPanel);

    refreshDeliveryState();
    addAttachListener(
        mailSectionAttachEvent -> startMaintenancePolling(mailSectionAttachEvent.getUI()));
    addDetachListener(
        mailSectionDetachEvent -> stopMaintenancePolling(mailSectionDetachEvent.getUI()));
  }

  private void startMaintenancePolling(UI mailTestUi) {
    refreshDeliveryState();

    previousPollInterval = mailTestUi.getPollInterval();
    installedPollInterval = previousPollInterval > 0 ? Math.min(previousPollInterval, 5000) : 5000;
    mailTestUi.setPollInterval(installedPollInterval);
    maintenancePollRegistration = mailTestUi.addPollListener(_ -> refreshDeliveryState());
  }

  private void stopMaintenancePolling(UI mailTestUi) {
    if (maintenancePollRegistration != null) {
      maintenancePollRegistration.remove();
    }

    maintenancePollRegistration = null;
    if (installedPollInterval != previousPollInterval
        && mailTestUi.getPollInterval() == installedPollInterval) {
      mailTestUi.setPollInterval(previousPollInterval);
    }
  }

  public void refreshDeliveryState() {
    State deliveryState = maintenanceService.state();
    boolean isDeliveryAvailable = deliveryState == MaintenanceService.State.OPEN;
    mailCardContentLayout.setEnabled(isDeliveryAvailable);
    mailCardContentLayout
        .getElement()
        .setAttribute("aria-disabled", Boolean.toString(!isDeliveryAvailable));
    deliveryStatusDiv
        .getElement()
        .setAttribute("data-state", deliveryState.name().toLowerCase(Locale.ROOT));

    String deliveryStatusText =
        switch (deliveryState) {
          case OPEN -> messageProperties.getMaintenanceStatusAvailable();
          case ACTIVE -> messageProperties.getMaintenanceStatusActive();
          case UNAVAILABLE -> messageProperties.getMaintenanceStatusUnavailable();
        };

    String deliveryDescriptionText =
        messageProperties.getAdminMailCardBadge() + ": " + deliveryStatusText;
    deliveryStatusDiv.getElement().setAttribute(ARIA_LABEL, deliveryDescriptionText);
    deliveryTooltip.setText(deliveryDescriptionText);
  }

  private VerticalLayout buildMailCardContentLayout() {
    SvgIcon mailEnvelopeSvgIcon = AppIcon.ENVELOPE.create();
    mailEnvelopeSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div mailIconBadgeDiv = new Div(mailEnvelopeSvgIcon);
    mailIconBadgeDiv.addClassName(PANEL_ICON);
    Div mailCardHeadingDiv = new Div(mailCardTitleH2, mailCardDescriptionSpan);
    mailCardHeadingDiv.addClassName(PANEL_HEADING);
    Div mailCardHeaderDiv = new Div(mailIconBadgeDiv, mailCardHeadingDiv, deliveryStatusDiv);
    mailCardHeaderDiv.addClassName(PANEL_HEADER);

    VerticalLayout mailFieldsLayout =
        new VerticalLayout(
            buildMailFieldRowDiv(testRecipientField, recipientInfoButton),
            buildMailFieldRowDiv(testSubjectField, subjectInfoButton),
            buildMailFieldRowDiv(testBodyField, bodyInfoButton));
    mailFieldsLayout.setPadding(false);
    mailFieldsLayout.setSpacing(false);
    mailFieldsLayout.setWidthFull();
    mailFieldsLayout.addClassName("cosmos-mail-fields");

    SvgIcon mailHintSvgIcon = AppIcon.INFO_CIRCLE.create();
    mailHintSvgIcon.getElement().setAttribute(ARIA_HIDDEN, "true");
    Div mailHintTextDiv = new Div(mailSendHintSpan, smtpLink);
    mailHintTextDiv.addClassName("admin-mail-hint-copy");
    Div mailHintDiv = new Div(mailHintSvgIcon, mailHintTextDiv);
    mailHintDiv.addClassName("cosmos-panel-hint");
    smtpLink.addClassName("admin-smtp-link");
    smtpLink.add(AppIcon.OPEN_SECTION.create(), smtpLinkCaptionSpan);
    smtpLinkCaptionSpan.setText(messageProperties.getAdminConfigGroupSmtp());
    testBodyField.setHelperText(messageProperties.getAdminMailBodyHint());

    Div mailCardActionsDiv = new Div(sendTestMailButton);
    mailCardActionsDiv.addClassName("cosmos-panel-actions");
    Div mailCardFooterDiv = new Div(mailHintDiv, mailCardActionsDiv);
    mailCardFooterDiv.addClassName("cosmos-panel-footer");

    VerticalLayout mailCardLayout =
        new VerticalLayout(mailCardHeaderDiv, mailFieldsLayout, mailCardFooterDiv);
    mailCardLayout.setPadding(false);
    mailCardLayout.setSpacing(false);
    mailCardLayout.setWidthFull();
    mailCardLayout.setAlignItems(Alignment.START);
    mailCardLayout.addClassName("cosmos-admin-form");
    return mailCardLayout;
  }

  private Div buildMailFieldRowDiv(Component mailInputComponent, ConfigInfoButton fieldInfoButton) {
    Div mailFieldRowDiv = new Div(mailInputComponent, fieldInfoButton);
    mailFieldRowDiv.addClassNames("cosmos-config-input-row", "cosmos-mail-field-row");
    if (mailInputComponent == testBodyField) {
      mailFieldRowDiv.addClassName("cosmos-mail-body-row");
      mailBodyHintSpan.addClassName("cosmos-mail-body-hint");
      // Die native Hilfsbeschreibung bleibt für assistive Technik am Eingabefeld erhalten.
      mailBodyHintSpan.getElement().setAttribute(ARIA_HIDDEN, "true");
      mailFieldRowDiv.add(mailBodyHintSpan);
    }
    return mailFieldRowDiv;
  }

  private ConfigInfoButton buildMailFieldInfoButton(FieldHelp fieldHelp) {
    return new ConfigInfoButton(
        fieldHelp.description(),
        () -> {
          selectedMailFieldHelp = fieldHelp;
          renderMailFieldHelp(fieldHelp);
          mailFieldHelpPanel.open();
        });
  }

  private void renderMailFieldHelp(FieldHelp fieldHelp) {
    mailFieldHelpPanel.removeAll();
    mailFieldHelpPanel.setHeading(messageProperties.getAdminConfigHelpTitle());
    mailFieldHelpPanel.setDescription(messageProperties.getAdminConfigHelpDescription());
    mailFieldHelpPanel.refreshCloseText(messageProperties);

    closeHelpButton.setText(messageProperties.getNotificationsClose());
    Span mailHelpGroupSpan = new Span(messageProperties.getAdminMailTitle());
    mailHelpGroupSpan.addClassName("admin-help-group");

    H3 fieldHelpTitleH3 = new H3(fieldHelp.title().get());
    fieldHelpTitleH3.addClassName("admin-help-field-title");
    Span fieldHelpDescriptionSpan = new Span(fieldHelp.description().get());
    fieldHelpDescriptionSpan.addClassName("admin-help-description");

    Div fieldHelpRulesDiv =
        new Div(
            new H4(messageProperties.getAdminConfigInputRules()),
            new UnorderedList(new ListItem(fieldHelp.rule().get())));
    fieldHelpRulesDiv.addClassName("admin-help-rules");
    mailFieldHelpPanel.add(
        mailHelpGroupSpan, fieldHelpTitleH3, fieldHelpDescriptionSpan, fieldHelpRulesDiv);
  }

  public void refreshTexts() {
    mailTestBreadcrumbSpan.setText(
        messageProperties.getAdminHeaderTitle() + " / " + messageProperties.getAdminMailTitle());
    mailCardTitleH2.setText(messageProperties.getAdminMailCardTitle());
    mailCardDescriptionSpan.setText(messageProperties.getAdminMailCardDescription());
    mailDeliveryStatusSpan.setText(messageProperties.getAdminMailCardBadge());

    refreshDeliveryState();
    mailSendHintSpan.setText(messageProperties.getAdminMailSendHint());
    smtpLinkCaptionSpan.setText(messageProperties.getAdminConfigGroupSmtp());
    testBodyField.setHelperText(messageProperties.getAdminMailBodyHint());
    mailBodyHintSpan.setText(messageProperties.getAdminMailBodyHint());
    testRecipientField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    testSubjectField.setPlaceholder(messageProperties.getAdminMailSubject());
    testBodyField.setPlaceholder(messageProperties.getAdminMailBody());

    recipientInfoButton.refreshText();
    subjectInfoButton.refreshText();
    bodyInfoButton.refreshText();
    recipientInfoButton
        .getElement()
        .setAttribute(ARIA_LABEL, messageProperties.getAdminMailRecipient());
    subjectInfoButton
        .getElement()
        .setAttribute(ARIA_LABEL, messageProperties.getAdminMailSubject());
    bodyInfoButton.getElement().setAttribute(ARIA_LABEL, messageProperties.getAdminMailBody());
    if (selectedMailFieldHelp != null && mailFieldHelpPanel.isOpened()) {
      renderMailFieldHelp(selectedMailFieldHelp);
    }

    mailTestTitleSpan.setText(messageProperties.getAdminMailTitle());
    mailTestDescriptionSpan.setText(messageProperties.getAdminMailDescription());
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
    if (!mailCardContentLayout.isEnabled()) {
      return;
    }

    String recipientEmailAddress =
        Optional.ofNullable(testRecipientField.getValue()).orElse("").trim();
    String mailSubjectText = Optional.ofNullable(testSubjectField.getValue()).orElse("").trim();
    String mailBodyText = Optional.ofNullable(testBodyField.getValue()).orElse("").trim();

    if (!UserInputRules.isEmailValid(recipientEmailAddress)) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminMailValidationRecipient,
              -1,
              NotificationHelper.NotificationType.ERROR);
      testRecipientField.setInvalid(true);
      return;
    }

    if (mailSubjectText.isBlank()) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminMailValidationSubject,
              -1,
              NotificationHelper.NotificationType.ERROR);
      testSubjectField.setInvalid(true);
      return;
    }

    if (mailBodyText.isBlank()) {
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
      emailService.sendAdminEmail(recipientEmailAddress, mailSubjectText, mailBodyText);
      logger.info("Test email accepted by transport");
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              messageProperties::getAdminMailSuccess,
              NotificationHelper.NotificationType.SUCCESS);
    } catch (MailException mailDispatchFailure) {
      logger.error("Test email dispatch failed", safeFailure(mailDispatchFailure));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getAdminMailError,
              -1,
              NotificationHelper.NotificationType.ERROR);
    }
  }
}
