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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.ui.components.CardComponent;
import de.derpeterson.app.ui.helper.NotificationHelper;
import de.derpeterson.app.ui.helper.VaadinUIHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
public class LoginView extends HorizontalLayout implements BeforeEnterObserver {

    private final transient SecurityService securityService;

    private final transient HttpServletRequest request;

    @Autowired
    public LoginView(CustomI18NProvider i18nProvider, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, SecurityService securityService, HttpServletRequest request) {
        this.securityService = securityService;
        this.request = request;

        setSizeFull();

        addClassNames("login-bg-fullscreen");

        // UI Elements
        RouterLink createAccountLink = new RouterLink(i18nProvider.getTranslation("loginView.create_account_link"), RegistrationView.class);
        EmailField emailField = new EmailField(i18nProvider.getTranslation("loginView.email_field"));
        PasswordField passwordField = new PasswordField(i18nProvider.getTranslation("loginView.password_field"));
        RouterLink forgotPasswordLink = new RouterLink(i18nProvider.getTranslation("loginView.forgot_password_link"), LoginView.class);
        Checkbox rememberMeCheckBox = new Checkbox(i18nProvider.getTranslation("loginView.remember_me_checkbox"));
        Button loginButton = new Button(i18nProvider.getTranslation("base.login_button"), event -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(emailField.getValue(), passwordField.getValue()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                    securityService.storeAuthenticatedUser(request, userDetails, rememberMeCheckBox.getValue());
                }

                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("loginView.login.success_message"), NotificationHelper.NotificationType.SUCCESS);

                getUI().ifPresent(ui -> ui.navigate(AdminView.class));
            } catch (AuthenticationException e) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("loginView.login.failed_message"), NotificationHelper.NotificationType.ERROR);
            }
        });

        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setAlignItems(Alignment.CENTER);
        mainContent.setJustifyContentMode(JustifyContentMode.START);

        Div bannerContent = new Div();
        bannerContent.add(new Image("../themes/custom-theme/welcome.png", "Welcome"));
        bannerContent.addClassNames(LumoUtility.Padding.XLARGE);

        mainContent.add(bannerContent);

        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        H1 title = new H1(i18nProvider.getTranslation("loginView.title"));
        title.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span createAccountQuestionText = new Span(i18nProvider.getTranslation("loginView.create_account_question"));
        createAccountLink.addClassNames("underline");

        HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
        cardSecondaryTitleLayout.setWidthFull();
        cardSecondaryTitleLayout.setPadding(false);
        cardSecondaryTitleLayout.setSpacing(false);
        cardSecondaryTitleLayout.setJustifyContentMode(JustifyContentMode.START);
        cardSecondaryTitleLayout.addClassNames(LumoUtility.Gap.SMALL);
        cardSecondaryTitleLayout.add(createAccountQuestionText);
        cardSecondaryTitleLayout.add(createAccountLink);

        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);

        passwordField.setWidthFull();
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setClearButtonVisible(true);

        loginButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();

        emailField.addKeyPressListener(Key.ENTER, event -> loginButton.click());
        passwordField.addKeyPressListener(Key.ENTER, event -> loginButton.click());

        HorizontalLayout rememberMeLayout = new HorizontalLayout();
        rememberMeLayout.setWidthFull();
        rememberMeLayout.setJustifyContentMode(JustifyContentMode.START);
        rememberMeLayout.add(rememberMeCheckBox);

        forgotPasswordLink.addClassNames("underline");
        HorizontalLayout forgotPasswordLayout = new HorizontalLayout();
        forgotPasswordLayout.setWidthFull();
        forgotPasswordLayout.setJustifyContentMode(JustifyContentMode.END);
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

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (securityService.getAuthenticatedUser(this.request).isPresent()) {
            beforeEnterEvent.forwardTo(AdminView.class);
        }
    }
}
