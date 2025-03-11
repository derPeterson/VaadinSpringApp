package de.derpeterson.app.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.VerificationService;
import de.derpeterson.app.ui.components.CardComponent;
import de.derpeterson.app.ui.helper.NotificationHelper;
import de.derpeterson.app.ui.helper.VaadinUIHelper;
import jakarta.servlet.http.HttpServletRequest;

@Route("verification")
@PageTitle("Verification")
@AnonymousAllowed
public class VerificationView extends HorizontalLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    private static final String BASE_FAILED_TEXT_MESSAGE_KEY = "base.failed.message";
    private static final String BASE_HOME_BUTTON_MESSAGE_KEY = "base.home_button";

    private final CustomI18NProvider i18nProvider;
    private final transient VerificationService verificationService;

    private final transient SecurityService securityService;
    private final transient HttpServletRequest request;

    private final VerticalLayout mainContent;

    public VerificationView(CustomI18NProvider i18nProvider, VerificationService verificationService, SecurityService securityService, HttpServletRequest request) {
        this.i18nProvider = i18nProvider;
        this.verificationService = verificationService;
        this.securityService = securityService;
        this.request = request;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        addClassNames("verification-bg-fullscreen");

        mainContent = new VerticalLayout();
        mainContent.setAlignItems(Alignment.CENTER);
        mainContent.setJustifyContentMode(JustifyContentMode.START);
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
        cardContentLayout.setAlignItems(Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.BELL.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.WARNING);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        H1 title = new H1(i18nProvider.getTranslation(i18nProvider.getTranslation("verificationView.expired.title")));
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span expiredText1 = new Span(i18nProvider.getTranslation(i18nProvider.getTranslation("verificationView.expired.text1")));
        expiredText1.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);
        Span expiredText2 = new Span(i18nProvider.getTranslation(i18nProvider.getTranslation("verificationView.expired.text2")));
        expiredText2.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTextLayout.add(expiredText1);
        cardTextLayout.add(expiredText2);

        Button reSendButton = new Button(i18nProvider.getTranslation("base.resend_button"), event -> {
            boolean emailSent = verificationService.sendVerificationEmailByToken(token);
            if (emailSent) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.success.title"), i18nProvider.getTranslation("verificationView.expired.success_message"), -1, NotificationHelper.NotificationType.SUCCESS);
            } else {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.failed.title"), i18nProvider.getTranslation(BASE_FAILED_TEXT_MESSAGE_KEY), -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        reSendButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        reSendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        reSendButton.setWidthFull();

        Button homeButton = new Button(i18nProvider.getTranslation(BASE_HOME_BUTTON_MESSAGE_KEY), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, reSendButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private void createFailedCard() {
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.WARNING.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.ERROR);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        H1 title = new H1(i18nProvider.getTranslation(i18nProvider.getTranslation("verificationView.not_found.title")));
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span existsNotText1 = new Span(i18nProvider.getTranslation(i18nProvider.getTranslation("verificationView.not_found.text1")));
        existsNotText1.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);
        Span existsNotText2 = new Span(i18nProvider.getTranslation(i18nProvider.getTranslation("verificationView.not_found.text2")));
        existsNotText2.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTextLayout.add(existsNotText1);
        cardTextLayout.add(existsNotText2);

        EmailField emailField = new EmailField(i18nProvider.getTranslation("verificationView.email_field"));
        emailField.setClearButtonVisible(true);
        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);

        Button sendButton = new Button(i18nProvider.getTranslation("base.send_button"), event -> {
            boolean emailSent = verificationService.sendVerificationEmailByEmail(emailField.getValue());
            if (emailSent) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.success.title"), i18nProvider.getTranslation("verificationView.not_found.success_message"), -1, NotificationHelper.NotificationType.SUCCESS);
            } else {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.failed.title"), i18nProvider.getTranslation(BASE_FAILED_TEXT_MESSAGE_KEY), -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        sendButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setWidthFull();

        Button homeButton = new Button(i18nProvider.getTranslation(BASE_HOME_BUTTON_MESSAGE_KEY), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, emailField, sendButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private void createSuccessCard() {
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.CHECK_CIRCLE.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.SUCCESS);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        H1 title = new H1(i18nProvider.getTranslation("verificationView.success.title"));
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span successText = new Span(i18nProvider.getTranslation("verificationView.success.text"));
        successText.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);
        HorizontalLayout cardTextLayout = new HorizontalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTextLayout.add(successText);

        Button homeButton = new Button(i18nProvider.getTranslation(BASE_HOME_BUTTON_MESSAGE_KEY), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (securityService.getAuthenticatedUser(this.request).isPresent()) {
            beforeEnterEvent.forwardTo(AdminView.class);
        }
    }
}
