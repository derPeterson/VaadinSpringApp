package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.VaadinUIHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsNotAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.VerificationService;
import de.derpeterson.app.ui.components.CardComponent;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Route("verification")
@PageTitle("Verification")
@AnonymousAllowed
public class VerificationView extends IsNotAuthentificatedBaseView<HorizontalLayout> implements HasUrlParameter<String> {

    private static final Logger logger = LoggerFactory.getLogger(VerificationView.class);

    private final MessageProperties messageProperties;
    private final transient VerificationService verificationService;

    private final VerticalLayout mainContent;

    public VerificationView(MessageProperties messageProperties, VerificationService verificationService, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new HorizontalLayout());

        this.messageProperties = messageProperties;
        this.verificationService = verificationService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        addClassNames("verification-bg-fullscreen");

        mainContent = new VerticalLayout();
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        mainContent.setWidth(null);
        mainContent.setSizeUndefined();

        add(VaadinUIHelper.createFullHorizontalSpace());
        add(mainContent);
        add(VaadinUIHelper.createFullHorizontalSpace());
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String token) {
        if (token != null && !token.isEmpty()) {
            boolean isValid = verificationService.validateToken(token);
            if (isValid) {
                createSuccessCard();
            } else {
                boolean exists = verificationService.existsToken(token);
                if (exists) {
                    createExpiredCard(token);
                } else {
                    createFailedCard();
                }
            }
        } else {
            UI.getCurrent().navigate(HomeView.class);
        }
    }

    private void createExpiredCard(String token) {
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.BELL.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.WARNING);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        H1 title = new H1(messageProperties.getVerificationExpiredTitle());
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        VerticalLayout cardTextLayout = getCardTextLayout(messageProperties.getVerificationExpiredText1(), messageProperties.getVerificationExpiredText2());

        Button resendButton = new Button(messageProperties.getBaseResendButton(), event -> {
            try {
                boolean emailSent = verificationService.sendVerificationEmailByToken(token);
                if (emailSent) {
                    NotificationHelper.getInstance().showNotification(messageProperties.getBaseSuccessTitle(), messageProperties.getVerificationExpiredSuccessMessage(), -1, NotificationHelper.NotificationType.SUCCESS);
                } else {
                    NotificationHelper.getInstance().showNotification(messageProperties.getBaseFailedTitle(), messageProperties.getBaseFailedMessage(), -1, NotificationHelper.NotificationType.ERROR);
                }
            } catch (IOException e) {
                logger.error("Exception occurred:", e);

                NotificationHelper.getInstance().showNotification(messageProperties.getBaseFailedTitle(), messageProperties.getBaseFailedMessage(),
                        -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        resendButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
        resendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resendButton.setWidthFull();

        Button homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, resendButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private VerticalLayout getCardTextLayout(String text1, String text2) {
        Span expiredText1 = new Span(text1);
        expiredText1.addClassNames(LumoUtility.Whitespace.NOWRAP);
        Span expiredText2 = new Span(text2);
        expiredText2.addClassNames(LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTextLayout.add(expiredText1);
        cardTextLayout.add(expiredText2);
        return cardTextLayout;
    }

    private void createFailedCard() {
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.WARNING.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.ERROR);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        H1 title = new H1(messageProperties.getVerificationNotFoundTitle());
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        VerticalLayout cardTextLayout = getCardTextLayout(messageProperties.getVerificationNotFoundText1(), messageProperties.getVerificationNotFoundText2());

        EmailField emailField = new EmailField(messageProperties.getVerificationEmailField());
        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);
        emailField.setRequiredIndicatorVisible(true);

        Button sendButton = new Button(messageProperties.getBaseSendButton(), event -> {

            if (!ValidationHelper.validateRequiredInputs(List.of(emailField), messageProperties)) {
                return;
            }

            if (!ValidationHelper.validateEmailValidInputs(List.of(emailField), messageProperties)) {
                return;
            }

            try {
                boolean emailSent = verificationService.sendVerificationEmailByEmail(emailField.getValue());
                if (emailSent) {
                    NotificationHelper.getInstance().showNotification(messageProperties.getBaseSuccessTitle(), messageProperties.getVerificationNotFoundSuccessMessage(), -1, NotificationHelper.NotificationType.SUCCESS);
                } else {
                    NotificationHelper.getInstance().showNotification(messageProperties.getBaseFailedTitle(), messageProperties.getBaseFailedMessage(), -1, NotificationHelper.NotificationType.ERROR);
                }
            } catch (IOException e) {
                logger.error("Exception occurred:", e);

                NotificationHelper.getInstance().showNotification(messageProperties.getBaseFailedTitle(), messageProperties.getBaseFailedMessage(),
                        -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        sendButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setWidthFull();

        Button homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, emailField, sendButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private void createSuccessCard() {
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.CHECK_CIRCLE.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.SUCCESS);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        H1 title = new H1(messageProperties.getVerificationSuccessTitle());
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span successText = new Span(messageProperties.getVerificationSuccessText());
        successText.addClassNames(LumoUtility.Whitespace.NOWRAP);
        HorizontalLayout cardTextLayout = new HorizontalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTextLayout.add(successText);

        Button homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }
}
