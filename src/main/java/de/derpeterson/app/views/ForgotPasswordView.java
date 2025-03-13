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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.helper.components.CardComponent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.VaadinUIHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.security.IsNotAuthentificatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Route("forgot-password")
@PageTitle("Forgot Password")
@AnonymousAllowed
public class ForgotPasswordView extends IsNotAuthentificatedBaseView<HorizontalLayout> {

    private final CustomI18NProvider i18nProvider;
    private final transient PasswordResetService passwordResetService;

    private final VerticalLayout mainContent;

    public ForgotPasswordView(CustomI18NProvider i18nProvider, PasswordResetService passwordResetService, SecurityService securityService, HttpServletRequest request) {
        super(securityService, request, new HorizontalLayout());

        this.i18nProvider = i18nProvider;
        this.passwordResetService = passwordResetService;

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

        H1 title = new H1(i18nProvider.getTranslation(i18nProvider.getTranslation("forgotPasswordView.title")));
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span text = new Span(i18nProvider.getTranslation(i18nProvider.getTranslation("forgotPasswordView.text")));
        text.addClassNames(LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardTextLayout.add(text);

        EmailField emailField = new EmailField(i18nProvider.getTranslation("verificationView.email_field"));
        emailField.setClearButtonVisible(true);
        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);

        Button sendButton = new Button(i18nProvider.getTranslation("base.send_button"), event -> {
            if (!ValidationHelper.validateRequiredInputs(List.of(emailField), i18nProvider)) {
                return;
            }

            if (!ValidationHelper.validateEmailValidInputs(List.of(emailField), i18nProvider)) {
                return;
            }

            boolean emailSent = passwordResetService.sendPasswordResetEmail(emailField.getValue());
            if (emailSent) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.success.title"), i18nProvider.getTranslation("forgotPasswordView.success_message"), -1, NotificationHelper.NotificationType.SUCCESS);
            } else {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.failed.title"), i18nProvider.getTranslation("base.failed.message"), -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        sendButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setWidthFull();

        Button loginButton = new Button(i18nProvider.getTranslation("base.login_button"), event -> UI.getCurrent().navigate(LoginView.class));
        loginButton.setPrefixComponent(VaadinIcon.SIGN_IN.create());
        loginButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, emailField, sendButton, loginButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }
}
