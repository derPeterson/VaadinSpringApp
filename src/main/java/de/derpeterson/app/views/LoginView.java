package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsNotAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPageComponent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Route(AppRouteConstants.LOGIN_ROUTE)
@PageTitle(AppRouteConstants.LOGIN_PAGE_TITLE)
@AnonymousAllowed
public class LoginView extends IsNotAuthenticatedBaseView<HorizontalLayout> {

    private static final Logger logger = LoggerFactory.getLogger(LoginView.class);

    private final transient MessageProperties messageProperties;

    private RouterLink createAccountLink = null;
    private EmailField emailField = null;
    private PasswordField passwordField = null;
    private RouterLink forgotPasswordLink = null;
    private Checkbox rememberMeCheckBox = null;
    private Button loginButton = null;
    private H1 title = null;
    private Span createAccountQuestionText = null;

    public LoginView(MessageProperties messageProperties, AuthenticationManager authenticationManager,
                     SecurityService securityService, HttpServletRequest request, ConfigService configService) {
        super(securityService, request, new HorizontalLayout());

        this.messageProperties = messageProperties;

        ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> {
            VaadinSession.getCurrent().setLocale(event.getNewLocale());

            Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
            Optional.ofNullable(createAccountLink)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getLoginCreateAccountLink));
            Optional.ofNullable(emailField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getLoginEmailField));
            Optional.ofNullable(passwordField)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getLoginPasswordField));
            Optional.ofNullable(forgotPasswordLink)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getLoginForgotPasswordLink));
            Optional.ofNullable(rememberMeCheckBox)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getLoginRememberMeCheckbox));
            Optional.ofNullable(loginButton)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getBaseLoginButton));
            Optional.ofNullable(title)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getLoginTitle));
            Optional.ofNullable(createAccountQuestionText)
                    .ifPresent(component -> componentTranslationSupplierMap.put(component, this.messageProperties::getLoginCreateAccountQuestion));


            ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
            updateInputPlaceholders();
            NotificationHelper.getInstance().updateText();
        });

        getContent().setWidthFull();
        getContent().setPadding(false);
        setSpacing(false);
        getContent().removeAll();

        // UI Elements
        this.createAccountLink = new RouterLink(messageProperties.getLoginCreateAccountLink(), RegistrationView.class);
        this.emailField = new EmailField(messageProperties.getLoginEmailField());
        this.passwordField = new PasswordField(messageProperties.getLoginPasswordField());
        this.forgotPasswordLink = new RouterLink(messageProperties.getLoginForgotPasswordLink(), ForgotPasswordView.class);
        this.rememberMeCheckBox = new Checkbox(messageProperties.getLoginRememberMeCheckbox());
        this.loginButton = new Button(messageProperties.getBaseLoginButton(), event -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(emailField.getValue(), passwordField.getValue()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                    securityService.storeAuthenticatedUser((HttpServletRequest) VaadinRequest.getCurrent(),
                            (HttpServletResponse) VaadinResponse.getCurrent(), userDetails, rememberMeCheckBox.getValue());

                    securityService.handleLogin(userDetails);
                } else {
                    logger.warn("⚠️ Principal is not an instance of UserDetails, save and handle of login could not be performed.");
                }

                NotificationHelper.getInstance().showNotification(messageProperties::getBaseSuccessTitle, messageProperties::getLoginSuccessMessage, NotificationHelper.NotificationType.SUCCESS);

                Class<? extends Component> target = hasAdminRole(authentication) ? AdminView.class : HomeView.class;
                getUI().ifPresent(ui -> ui.navigate(target));
            } catch (AuthenticationException e) {
                NotificationHelper.getInstance().showNotification(messageProperties::getBaseFailedTitle, messageProperties::getLoginFailedMessage, -1, NotificationHelper.NotificationType.ERROR);
            }
        });

        CosmosPageComponent pageLayout = new CosmosPageComponent(configService, false, messageProperties::getAccountCopyrightTemplate);
        VerticalLayout mainContent = pageLayout.getCardContainer();

        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        cardContentLayout.setPadding(false);
        cardContentLayout.setSpacing(false);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY, "cosmos-form");

        this.title = new H1(messageProperties.getLoginTitle());
        title.addClassName("cosmos-headline");

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.addClassName("cosmos-title-row");
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        this.createAccountQuestionText = new Span(messageProperties.getLoginCreateAccountQuestion());
        createAccountLink.addClassNames("underline");

        HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
        cardSecondaryTitleLayout.addClassNames("cosmos-intro", "cosmos-form-text");
        cardSecondaryTitleLayout.setWidthFull();
        cardSecondaryTitleLayout.setPadding(false);
        cardSecondaryTitleLayout.setSpacing(false);
        cardSecondaryTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardSecondaryTitleLayout.addClassNames(LumoUtility.Gap.SMALL, "cosmos-form-links", "cosmos-form-text");
        cardSecondaryTitleLayout.add(createAccountQuestionText);
        cardSecondaryTitleLayout.add(createAccountLink);

        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);

        passwordField.setWidthFull();
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setClearButtonVisible(true);

        loginButton.setPrefixComponent(VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();

        emailField.addKeyPressListener(Key.ENTER, event -> loginButton.click());
        passwordField.addKeyPressListener(Key.ENTER, event -> loginButton.click());

        HorizontalLayout rememberMeLayout = new HorizontalLayout();
        rememberMeLayout.addClassName("cosmos-remember");
        rememberMeLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        rememberMeLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        rememberMeLayout.add(rememberMeCheckBox);

        forgotPasswordLink.addClassNames("underline");
        HorizontalLayout forgotPasswordLayout = new HorizontalLayout();
        forgotPasswordLayout.addClassName("cosmos-forgot");
        forgotPasswordLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        forgotPasswordLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        forgotPasswordLayout.add(forgotPasswordLink);

        HorizontalLayout secondaryActionLayout = new HorizontalLayout();
        secondaryActionLayout.setWidthFull();
        secondaryActionLayout.addClassName("cosmos-options");
        secondaryActionLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        secondaryActionLayout.add(rememberMeLayout, forgotPasswordLayout);

        cardContentLayout.add(cardTitleLayout, cardSecondaryTitleLayout, emailField, passwordField, loginButton, secondaryActionLayout);
        CosmosCardComponent cardComponent = new CosmosCardComponent(cardContentLayout);

        mainContent.add(cardComponent);

        updateInputPlaceholders();
        add(pageLayout);
    }

    private void updateInputPlaceholders() {
        if (emailField != null) emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
        if (passwordField != null) passwordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    }

}
