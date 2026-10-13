package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
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
import de.derpeterson.app.ui.components.CardComponent;
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
@StyleSheet("custom-theme/cosmos-login.css")
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
                     SecurityService securityService, HttpServletRequest request) {
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
            NotificationHelper.getInstance().updateText();
        });

        getContent().setWidthFull();
        getContent().setPadding(false);
        setSpacing(false);
        addClassName("cosmos-login");

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

        VerticalLayout mainContent = new VerticalLayout();
        mainContent.addClassName("cosmos-login__content");
        mainContent.setWidthFull();
        mainContent.setPadding(false);
        mainContent.setSpacing(false);
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        cardContentLayout.setPadding(false);
        cardContentLayout.setSpacing(false);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY, "cosmos-login__form");

        this.title = new H1(messageProperties.getLoginTitle());
        title.addClassName("cosmos-login__headline");

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        this.createAccountQuestionText = new Span(messageProperties.getLoginCreateAccountQuestion());
        createAccountLink.addClassNames("underline");

        HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
        cardSecondaryTitleLayout.setWidthFull();
        cardSecondaryTitleLayout.setPadding(false);
        cardSecondaryTitleLayout.setSpacing(false);
        cardSecondaryTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardSecondaryTitleLayout.addClassNames(LumoUtility.Gap.SMALL, "cosmos-login__account-links");
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
        rememberMeLayout.addClassName("cosmos-login__remember");
        rememberMeLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        rememberMeLayout.add(rememberMeCheckBox);

        forgotPasswordLink.addClassNames("underline");
        HorizontalLayout forgotPasswordLayout = new HorizontalLayout();
        forgotPasswordLayout.addClassName("cosmos-login__forgot");
        forgotPasswordLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        forgotPasswordLayout.add(forgotPasswordLink);

        HorizontalLayout secondaryActionLayout = new HorizontalLayout();
        secondaryActionLayout.setWidthFull();
        secondaryActionLayout.addClassName("cosmos-login__options");

        secondaryActionLayout.add(rememberMeLayout, forgotPasswordLayout);

        cardContentLayout.add(cardTitleLayout, cardSecondaryTitleLayout, emailField, passwordField, loginButton, secondaryActionLayout);
        CardComponent cardComponent = new CardComponent(cardContentLayout);
        // Nur diese Card erhält die Glasoberfläche; andere CardComponent-Instanzen bleiben unverändert.
        cardComponent.removeClassNames(LumoUtility.Background.BASE, LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.LARGE, LumoUtility.BoxShadow.MEDIUM, LumoUtility.Padding.LARGE);
        cardComponent.addClassName("cosmos-login__card");
        cardComponent.setWidthFull();

        mainContent.add(cardComponent);

        add(createSky(), mainContent);
    }

    private static Div createSky() {
        Div sky = layer("cosmos-login__sky");
        sky.getElement().setAttribute("aria-hidden", "true");
        sky.add(layer("cosmos-login__stars", "cosmos-login__stars--small"),
                layer("cosmos-login__stars", "cosmos-login__stars--far"),
                layer("cosmos-login__galaxy"),
                layer("cosmos-login__glow", "cosmos-login__glow--blue"),
                layer("cosmos-login__glow", "cosmos-login__glow--violet"),
                layer("cosmos-login__horizon"),
                layer("cosmos-login__planet"),
                layer("cosmos-login__moon"));
        return sky;
    }

    private static Div layer(String... classes) {
        Div layer = new Div();
        layer.addClassNames(classes);
        return layer;
    }
}
