package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Route("verification")
@PageTitle("Verification")
@AnonymousAllowed
public class VerificationView extends IsNotAuthentificatedBaseView<HorizontalLayout> implements HasUrlParameter<String> {

    private static final Logger logger = LoggerFactory.getLogger(VerificationView.class);

    private final MessageProperties messageProperties;
    private final transient VerificationService verificationService;

    private final VerticalLayout mainContent;

    private H1 expiredTitle = null;
    private H1 notFoundTitle = null;
    private H1 successTitle = null;
    private Button resendButton = null;
    private Button homeButton = null;
    private EmailField emailField = null;
    private Button sendButton = null;
    private Span expiredText1 = null;
    private Span expiredText2 = null;
    private Span notFoundText1 = null;
    private Span notFoundText2 = null;
    private Span successText = null;

    public VerificationView(MessageProperties messageProperties, VerificationService verificationService, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new HorizontalLayout());

        this.messageProperties = messageProperties;
        this.verificationService = verificationService;

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
            Optional.ofNullable(expiredTitle)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationExpiredTitle));
            Optional.ofNullable(notFoundTitle)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationNotFoundTitle));
            Optional.ofNullable(successTitle)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationSuccessTitle));
            Optional.ofNullable(resendButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseResendButton));
            Optional.ofNullable(homeButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseHomeButton));
            Optional.ofNullable(emailField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationEmailField));
            Optional.ofNullable(homeButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseHomeButton));
            Optional.ofNullable(sendButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseSendButton));
            Optional.ofNullable(expiredText1)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationExpiredText1));
            Optional.ofNullable(expiredText2)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationExpiredText2));
            Optional.ofNullable(notFoundText1)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationNotFoundText1));
            Optional.ofNullable(notFoundText2)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationNotFoundText2));
            Optional.ofNullable(successText)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getVerificationSuccessText));

            ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
        });

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
                    createNotFoundCard();
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

        this.expiredTitle = new H1(messageProperties.getVerificationExpiredTitle());
        expiredTitle.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(expiredTitle);

        VerticalLayout cardTextLayout = createExpiredCardTextLayout(messageProperties.getVerificationExpiredText1(), messageProperties.getVerificationExpiredText2());

        this.resendButton = new Button(messageProperties.getBaseResendButton(), event -> {
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

        this.homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, resendButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private VerticalLayout createExpiredCardTextLayout(String text1String, String text2String) {
        this.expiredText1 = new Span(text1String);
        expiredText1.addClassNames(LumoUtility.Whitespace.NOWRAP);
        this.expiredText2 = new Span(text2String);
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

    private VerticalLayout createNotFoundCardTextLayout(String text1String, String text2String) {
        this.notFoundText1 = new Span(text1String);
        notFoundText1.addClassNames(LumoUtility.Whitespace.NOWRAP);
        this.notFoundText2 = new Span(text2String);
        notFoundText2.addClassNames(LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTextLayout.add(notFoundText1);
        cardTextLayout.add(notFoundText2);
        return cardTextLayout;
    }

    private void createNotFoundCard() {
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

        this.notFoundTitle = new H1(messageProperties.getVerificationNotFoundTitle());
        notFoundTitle.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(notFoundTitle);

        VerticalLayout cardTextLayout = createNotFoundCardTextLayout(messageProperties.getVerificationNotFoundText1(), messageProperties.getVerificationNotFoundText2());

        this.emailField = new EmailField(messageProperties.getVerificationEmailField());
        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);
        emailField.setRequiredIndicatorVisible(true);

        this.sendButton = new Button(messageProperties.getBaseSendButton(), event -> {

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

        this.homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
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

        this.successTitle = new H1(messageProperties.getVerificationSuccessTitle());
        successTitle.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(successTitle);

        this.successText = new Span(messageProperties.getVerificationSuccessText());
        successText.addClassNames(LumoUtility.Whitespace.NOWRAP);
        HorizontalLayout cardTextLayout = new HorizontalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTextLayout.add(successText);

        this.homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }
}
