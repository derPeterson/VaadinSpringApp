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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.VaadinUIHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsNotAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.PasswordResetService;
import de.derpeterson.app.ui.components.CardComponent;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Route("reset-password")
@PageTitle("Reset Password")
@AnonymousAllowed
public class ResetPasswordView extends IsNotAuthentificatedBaseView<HorizontalLayout> implements HasUrlParameter<String> {

    private final MessageProperties messageProperties;
    private final transient PasswordResetService passwordResetService;

    private final VerticalLayout mainContent;

    public ResetPasswordView(MessageProperties messageProperties, PasswordResetService passwordResetService, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new HorizontalLayout());

        this.messageProperties = messageProperties;
        this.passwordResetService = passwordResetService;

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        addClassNames("reset-password-bg-fullscreen");

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
        cardContentLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        H1 title = new H1(messageProperties.getResetPasswordTitle());
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        PasswordField passwordField = new PasswordField(messageProperties.getResetPasswordNewPasswordField());
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setClearButtonVisible(true);
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setWidthFull();

        PasswordField confirmPasswordField = new PasswordField(messageProperties.getResetPasswordConfirmField());
        confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPasswordField.setClearButtonVisible(true);
        confirmPasswordField.setRequiredIndicatorVisible(true);
        confirmPasswordField.setWidthFull();

        Button resetButton = new Button(messageProperties.getBaseResetButton(), event -> {
            if (!ValidationHelper.validateRequiredInputs(List.of(passwordField, confirmPasswordField), messageProperties)) {
                return;
            }

            if (!ValidationHelper.validatePasswordSecureInputs(List.of(passwordField, confirmPasswordField), messageProperties)) {
                return;
            }

            if (!ValidationHelper.validatePasswordConfirmInputs(passwordField, confirmPasswordField, messageProperties)) {
                return;
            }

            boolean success = passwordResetService.resetPassword(token, passwordField.getValue());
            if (success) {
                NotificationHelper.getInstance().showNotification(messageProperties.getBaseSuccessTitle(), messageProperties.getResetPasswordSuccessMessage(), -1, NotificationHelper.NotificationType.SUCCESS);
            } else {
                NotificationHelper.getInstance().showNotification(messageProperties.getBaseFailedTitle(), messageProperties.getBaseFailedMessage(), -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        resetButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
        resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetButton.setWidthFull();

        Button homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardTitleLayout, passwordField, confirmPasswordField, resetButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private void invalidTokenCard() {
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

        H1 title = new H1(messageProperties.getResetPasswordInvalidTitle());
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        VerticalLayout cardTextLayout = getCardTextLayout();

        Button homeButton = new Button(messageProperties.getBaseHomeButton(), event -> UI.getCurrent().navigate(HomeView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();
        homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }

    private VerticalLayout getCardTextLayout() {
        Span invalidText = new Span(messageProperties.getResetPasswordInvalidText());
        invalidText.addClassNames(LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTextLayout.add(invalidText);
        return cardTextLayout;
    }
}
