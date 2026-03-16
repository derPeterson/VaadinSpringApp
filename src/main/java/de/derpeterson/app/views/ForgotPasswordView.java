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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.VaadinUIHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsNotAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.PasswordResetService;
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

@Route(AppRouteConstants.FORGOT_PASSWORD_ROUTE)
@PageTitle(AppRouteConstants.FORGOT_PASSWORD_PAGE_TITLE)
@AnonymousAllowed
public class ForgotPasswordView extends IsNotAuthenticatedBaseView<HorizontalLayout> {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordView.class);

    private final transient MessageProperties messageProperties;
    private final transient PasswordResetService passwordResetService;

    private final VerticalLayout mainContent;

    private H1 title = null;
    private Span text = null;
    private EmailField emailField = null;
    private Button sendButton = null;
    private Button loginButton = null;

    public ForgotPasswordView(MessageProperties messageProperties, PasswordResetService passwordResetService, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new HorizontalLayout());

        this.messageProperties = messageProperties;
        this.passwordResetService = passwordResetService;

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
            Optional.ofNullable(title)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getForgotPasswordTitle));
            Optional.ofNullable(text)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getForgotPasswordText));
            Optional.ofNullable(emailField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getForgotPasswordEmailField));
            Optional.ofNullable(sendButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseSendButton));
            Optional.ofNullable(loginButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseLoginButton));

            ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
            NotificationHelper.getInstance().updateText();
        });

        setSizeFull();
        setSpacing(false);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        addClassNames("forgot-password-bg-fullscreen");

        mainContent = new VerticalLayout();
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        mainContent.setWidth(null);
        mainContent.setSizeUndefined();
        mainContent.getStyle().set("padding-top", "10%");

        createPasswordResetCard();

        add(VaadinUIHelper.createFullHorizontalSpace());
        add(mainContent);
        add(VaadinUIHelper.createFullHorizontalSpace());
    }

    private void createPasswordResetCard() {
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.QUESTION_CIRCLE.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.PRIMARY);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        this.title = new H1(messageProperties.getForgotPasswordTitle());
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        this.text = new Span(messageProperties.getForgotPasswordText());
        text.addClassNames(LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTextLayout.add(text);

        this.emailField = new EmailField(messageProperties.getForgotPasswordEmailField());
        emailField.setClearButtonVisible(true);
        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);

        this.sendButton = new Button(messageProperties.getBaseSendButton(), event -> {
            if (!ValidationHelper.validateRequiredInputs(List.of(emailField), messageProperties)) {
                return;
            }

            if (!ValidationHelper.validateEmailValidInputs(List.of(emailField), messageProperties)) {
                return;
            }

            try {
                boolean emailSent = passwordResetService.sendPasswordResetEmail(emailField.getValue());
                if (emailSent) {
                    NotificationHelper.getInstance().showNotification(this.messageProperties::getBaseSuccessTitle, this.messageProperties::getForgotPasswordSuccessMessage, -1, NotificationHelper.NotificationType.SUCCESS);
                } else {
                    NotificationHelper.getInstance().showNotification(this.messageProperties::getBaseFailedTitle, this.messageProperties::getBaseFailedMessage, -1, NotificationHelper.NotificationType.ERROR);
                }
            } catch (IOException e) {
                logger.error("❌ Exception occurred:", e);

                NotificationHelper.getInstance().showNotification(this.messageProperties::getBaseFailedTitle,
                        this.messageProperties::getBaseFailedMessage,
                        -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        sendButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setWidthFull();

        this.loginButton = new Button(messageProperties.getBaseLoginButton(), event -> UI.getCurrent().navigate(LoginView.class));
        loginButton.setPrefixComponent(VaadinIcon.SIGN_IN.create());
        loginButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, emailField, sendButton, loginButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }
}
