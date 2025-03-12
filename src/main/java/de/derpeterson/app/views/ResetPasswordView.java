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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.PasswordResetService;
import de.derpeterson.app.ui.components.CardComponent;
import de.derpeterson.app.ui.helper.NotificationHelper;
import de.derpeterson.app.ui.helper.VaadinUIHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

@Route("reset-password")
@PageTitle("Reset Password")
@AnonymousAllowed
public class ResetPasswordView extends VerticalLayout implements HasUrlParameter<String>, BeforeEnterObserver {

    private static final String BASE_FAILED_TITLE_MESSAGE_KEY = "base.failed.title";
    private static final String BASE_HOME_BUTTON_MESSAGE_KEY = "base.home_button";

    private final CustomI18NProvider i18nProvider;
    private final transient PasswordResetService passwordResetService;

    private final transient SecurityService securityService;
    private final transient HttpServletRequest request;

    private final VerticalLayout mainContent;

    public ResetPasswordView(CustomI18NProvider i18nProvider, PasswordResetService passwordResetService, SecurityService securityService, HttpServletRequest request) {
        this.i18nProvider = i18nProvider;
        this.passwordResetService = passwordResetService;
        this.securityService = securityService;
        this.request = request;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        addClassNames("reset-password-bg-fullscreen");

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
            if (passwordResetService.validateToken(token)) {
                showResetPasswordCard(token);
            } else {
                invalidTokenCard();
            }
        } else {
            UI.getCurrent().navigate(HomeView.class);
        }
    }

    private void showResetPasswordCard(String token) {
        VerticalLayout cardContentLayout = new VerticalLayout();
        cardContentLayout.setAlignItems(Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        H1 title = new H1(i18nProvider.getTranslation(i18nProvider.getTranslation("resetPasswordView.reset.title")));
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        PasswordField passwordField = new PasswordField(i18nProvider.getTranslation("resetPasswordView.reset.new_password_field"));
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setClearButtonVisible(true);
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();

        PasswordField confirmPasswordField = new PasswordField(i18nProvider.getTranslation("resetPasswordView.reset.confirm_field"));
        confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPasswordField.setClearButtonVisible(true);
        confirmPasswordField.setRequiredIndicatorVisible(true);
        confirmPasswordField.setWidthFull();

        Button resetButton = new Button(i18nProvider.getTranslation("base.reset_button"), event -> {
            if (passwordField.getValue().isEmpty() || confirmPasswordField.getValue().isEmpty()) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                        i18nProvider.getTranslation("base.validation.required_message"),
                        -1, NotificationHelper.NotificationType.ERROR);

                passwordField.setInvalid(passwordField.getValue().isEmpty());
                confirmPasswordField.setInvalid(confirmPasswordField.getValue().isEmpty());
                return;
            } else {
                passwordField.setInvalid(false);
                confirmPasswordField.setInvalid(false);
            }

            // CHECK PASSWORD SECURE
            if (!passwordField.getValue().matches(UserEntity.PASSWORD_REGEX) || !confirmPasswordField.getValue().matches(UserEntity.PASSWORD_REGEX)) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                        i18nProvider.getTranslation("base.validation.password_invalid_message"),
                        -1, NotificationHelper.NotificationType.ERROR);

                passwordField.setInvalid(!passwordField.getValue().matches(UserEntity.PASSWORD_REGEX));
                confirmPasswordField.setInvalid(!confirmPasswordField.getValue().matches(UserEntity.PASSWORD_REGEX));

                return;
            } else {
                passwordField.setInvalid(false);
                confirmPasswordField.setInvalid(false);
            }

            // CHECK PASSWORD MATCH
            if (!StringUtils.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY),
                        i18nProvider.getTranslation("base.validation.password_confirm_message"),
                        -1, NotificationHelper.NotificationType.ERROR);

                passwordField.setInvalid(true);
                confirmPasswordField.setInvalid(true);

                return;
            } else {
                passwordField.setInvalid(false);
                confirmPasswordField.setInvalid(false);
            }


            boolean success = passwordResetService.resetPassword(token, passwordField.getValue());
            if (success) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.success.title"), i18nProvider.getTranslation("resetPasswordView.reset.success_message"), -1, NotificationHelper.NotificationType.SUCCESS);
            } else {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation(BASE_FAILED_TITLE_MESSAGE_KEY), i18nProvider.getTranslation("base.failed.message"), -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        resetButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
        resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetButton.setWidthFull();

        Button homeButton = new Button(i18nProvider.getTranslation(BASE_HOME_BUTTON_MESSAGE_KEY), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardTitleLayout, passwordField, confirmPasswordField, resetButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private void invalidTokenCard() {
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

        H1 title = new H1(i18nProvider.getTranslation(i18nProvider.getTranslation("resetPasswordView.invalid.title")));
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span invalidText = new Span(i18nProvider.getTranslation(i18nProvider.getTranslation("resetPasswordView.invalid.text")));
        invalidText.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTextLayout.add(invalidText);

        Button homeButton = new Button(i18nProvider.getTranslation(BASE_HOME_BUTTON_MESSAGE_KEY), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

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
