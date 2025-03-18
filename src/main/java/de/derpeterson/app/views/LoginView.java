package de.derpeterson.app.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.VaadinUIHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsNotAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.ui.components.CardComponent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
public class LoginView extends IsNotAuthentificatedBaseView<HorizontalLayout> {

    public LoginView(MessageProperties messageProperties, AuthenticationManager authenticationManager, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new HorizontalLayout());

        setSizeFull();

        addClassNames("login-bg-fullscreen");

        // UI Elements
        RouterLink createAccountLink = new RouterLink(messageProperties.getLoginCreateAccountLink(), RegistrationView.class);
        EmailField emailField = new EmailField(messageProperties.getLoginEmailField());
        PasswordField passwordField = new PasswordField(messageProperties.getLoginPasswordField());
        RouterLink forgotPasswordLink = new RouterLink(messageProperties.getLoginForgotPasswordLink(), ForgotPasswordView.class);
        Checkbox rememberMeCheckBox = new Checkbox(messageProperties.getLoginRememberMeCheckbox());
        Button loginButton = new Button(messageProperties.getBaseLoginButton(), event -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(emailField.getValue(), passwordField.getValue()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                    securityService.storeAuthenticatedUser(request, userDetails, rememberMeCheckBox.getValue());
                }

                NotificationHelper.getInstance().showNotification(messageProperties.getBaseSuccessTitle(), messageProperties.getLoginSuccessMessage(), NotificationHelper.NotificationType.SUCCESS);

                getUI().ifPresent(ui -> ui.navigate(AdminView.class));
            } catch (AuthenticationException e) {
                NotificationHelper.getInstance().showNotification(messageProperties.getBaseFailedTitle(), messageProperties.getLoginFailedMessage(), -1, NotificationHelper.NotificationType.ERROR);
            }
        });

        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setAlignItems(FlexComponent.Alignment.CENTER);
        mainContent.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        Div bannerContent = new Div();
        bannerContent.add(new Image("../themes/custom-theme/welcome.png", "Welcome"));
        bannerContent.addClassNames(LumoUtility.Padding.XLARGE);

        mainContent.add(bannerContent);

        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        H1 title = new H1(messageProperties.getLoginTitle());
        title.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span createAccountQuestionText = new Span(messageProperties.getLoginCreateAccountQuestion());
        createAccountLink.addClassNames("underline");

        HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
        cardSecondaryTitleLayout.setWidthFull();
        cardSecondaryTitleLayout.setPadding(false);
        cardSecondaryTitleLayout.setSpacing(false);
        cardSecondaryTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        cardSecondaryTitleLayout.addClassNames(LumoUtility.Gap.SMALL);
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
        rememberMeLayout.setWidthFull();
        rememberMeLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        rememberMeLayout.add(rememberMeCheckBox);

        forgotPasswordLink.addClassNames("underline");
        HorizontalLayout forgotPasswordLayout = new HorizontalLayout();
        forgotPasswordLayout.setWidthFull();
        forgotPasswordLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        forgotPasswordLayout.add(forgotPasswordLink);

        HorizontalLayout secondaryActionLayout = new HorizontalLayout();
        secondaryActionLayout.setWidthFull();

        secondaryActionLayout.add(rememberMeLayout, forgotPasswordLayout);

        cardContentLayout.add(cardTitleLayout, cardSecondaryTitleLayout, emailField, passwordField, loginButton, secondaryActionLayout);

        CardComponent cardComponent = new CardComponent(cardContentLayout);
        cardComponent.setMinWidth("500px");
        cardComponent.setMaxWidth("500px");

        mainContent.add(cardComponent);

        add(VaadinUIHelper.createFullHorizontalSpace());
        add(mainContent);
        add(VaadinUIHelper.createFullHorizontalSpace());
    }
}
