package de.derpeterson.app.views.admin;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.ui.components.CardComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public class AdminMailTestSection extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(AdminMailTestSection.class);

    private final transient MessageProperties messageProperties;
    private final transient EmailService emailService;

    private final H1 mailTestTitle;
    private final Span mailTestDescription;
    private final EmailField testRecipientField;
    private final TextField testSubjectField;
    private final TextArea testBodyField;
    private final Button sendTestMailButton;

    public AdminMailTestSection(MessageProperties messageProperties,
                                EmailService emailService,
                                UserDetails authenticatedUser) {
        this.messageProperties = messageProperties;
        this.emailService = emailService;

        setPadding(false);
        setSpacing(true);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassNames(LumoUtility.Gap.MEDIUM);

        mailTestTitle = new H1();
        mailTestTitle.addClassNames(
                LumoUtility.Margin.NONE,
                LumoUtility.FontSize.XXLARGE
        );

        mailTestDescription = new Span();
        mailTestDescription.addClassNames(
                LumoUtility.TextColor.SECONDARY
        );

        testRecipientField = new EmailField();
        testRecipientField.setClearButtonVisible(true);
        testRecipientField.setWidthFull();
        testRecipientField.setPrefixComponent(VaadinIcon.ENVELOPE.create());

        if (authenticatedUser != null) {
            testRecipientField.setValue(authenticatedUser.getUsername());
        }

        testSubjectField = new TextField();
        testSubjectField.setWidthFull();
        testSubjectField.setPrefixComponent(VaadinIcon.FONT.create());

        testBodyField = new TextArea();
        testBodyField.setWidthFull();
        testBodyField.setMinHeight("220px");

        sendTestMailButton = new Button();
        sendTestMailButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
        sendTestMailButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendTestMailButton.setWidthFull();
        sendTestMailButton.addClickListener(clickEvent -> sendTestMail());
        sendTestMailButton.addClickShortcut(Key.ENTER);

        VerticalLayout mailLayout = new VerticalLayout(
                mailTestTitle,
                mailTestDescription,
                testRecipientField,
                testSubjectField,
                testBodyField,
                sendTestMailButton
        );

        mailLayout.setPadding(false);
        mailLayout.setSpacing(true);
        mailLayout.setAlignItems(Alignment.START);

        CardComponent mailCard = new CardComponent(mailLayout);
        mailCard.setWidthFull();
        mailCard.setMaxWidth("860px");

        mailCard.addClassNames(LumoUtility.Background.SHADE_10);

        add(mailCard);
    }

    public void refreshTexts() {
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
        String recipient = Optional.ofNullable(testRecipientField.getValue()).orElse("").trim();
        String subject = Optional.ofNullable(testSubjectField.getValue()).orElse("").trim();
        String body = Optional.ofNullable(testBodyField.getValue()).orElse("").trim();

        if (recipient.isBlank() || !recipient.matches(EmailValidator.PATTERN)) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getAdminMailValidationRecipient,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );
            testRecipientField.setInvalid(true);
            return;
        }

        if (subject.isBlank()) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getAdminMailValidationSubject,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );
            testSubjectField.setInvalid(true);
            return;
        }

        if (body.isBlank()) {
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getAdminMailValidationBody,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );
            testBodyField.setInvalid(true);
            return;
        }

        testRecipientField.setInvalid(false);
        testSubjectField.setInvalid(false);
        testBodyField.setInvalid(false);

        try {
            emailService.sendAdminEmail(recipient, subject, body);
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseSuccessTitle,
                    messageProperties::getAdminMailSuccess,
                    NotificationHelper.NotificationType.SUCCESS
            );
        } catch (MailException ex) {
            logger.error("❌ Test email failed for recipient {}.", recipient, ex);
            NotificationHelper.getInstance().showNotification(
                    messageProperties::getBaseFailedTitle,
                    messageProperties::getAdminMailError,
                    -1,
                    NotificationHelper.NotificationType.ERROR
            );
        }
    }
}