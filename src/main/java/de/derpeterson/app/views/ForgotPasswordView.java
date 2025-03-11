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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.i18n.CustomI18NProvider;
import de.derpeterson.app.repository.UserRepository;
import de.derpeterson.app.service.EmailService;
import de.derpeterson.app.service.PasswordResetService;
import de.derpeterson.app.ui.components.CardComponent;
import de.derpeterson.app.ui.helper.NotificationHelper;
import de.derpeterson.app.ui.helper.VaadinUIHelper;

@Route("forgot-password")
@PageTitle("Forgot Password")
@AnonymousAllowed
public class ForgotPasswordView extends HorizontalLayout {

    private final CustomI18NProvider i18nProvider;
    private final transient PasswordResetService passwordResetService;

    private final VerticalLayout mainContent;

    public ForgotPasswordView(CustomI18NProvider i18nProvider, PasswordResetService passwordResetService, UserRepository userRepository, EmailService emailService) {
        this.i18nProvider = i18nProvider;
        this.passwordResetService = passwordResetService;

        setSizeFull();
        setSpacing(false);
        setJustifyContentMode(JustifyContentMode.CENTER);

        addClassNames("forgot-password-bg-fullscreen");

        mainContent = new VerticalLayout();
        mainContent.setAlignItems(Alignment.CENTER);
        mainContent.setJustifyContentMode(JustifyContentMode.START);
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
        cardContentLayout.setAlignItems(Alignment.CENTER);
        cardContentLayout.addClassNames(LumoUtility.TextColor.SECONDARY);

        Icon successIcon = VaadinIcon.QUESTION_CIRCLE.create();
        successIcon.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.TextColor.PRIMARY);

        HorizontalLayout cardIconLayout = new HorizontalLayout();
        cardIconLayout.setWidthFull();
        cardIconLayout.setPadding(false);
        cardIconLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardIconLayout.add(successIcon);

        H1 title = new H1(i18nProvider.getTranslation(i18nProvider.getTranslation("forgotPasswordView.title")));
        title.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);

        HorizontalLayout cardTitleLayout = new HorizontalLayout();
        cardTitleLayout.setWidthFull();
        cardTitleLayout.setPadding(false);
        cardTitleLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTitleLayout.add(title);

        Span text = new Span(i18nProvider.getTranslation(i18nProvider.getTranslation("forgotPasswordView.text")));
        text.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD, LumoUtility.Whitespace.NOWRAP);
        VerticalLayout cardTextLayout = new VerticalLayout();
        cardTextLayout.setWidthFull();
        cardTextLayout.setPadding(false);
        cardTextLayout.setSpacing(false);
        cardTextLayout.setAlignItems(Alignment.CENTER);
        cardTextLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardTextLayout.add(text);

        EmailField emailField = new EmailField(i18nProvider.getTranslation("verificationView.email_field"));
        emailField.setClearButtonVisible(true);
        emailField.setWidthFull();
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setClearButtonVisible(true);

        Button sendButton = new Button(i18nProvider.getTranslation("base.send_button"), event -> {
            boolean emailSent = passwordResetService.sendPasswordResetEmail(emailField.getValue());
            if (emailSent) {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.success.title"), i18nProvider.getTranslation("forgotPasswordView.success_message"), -1, NotificationHelper.NotificationType.SUCCESS);
            } else {
                NotificationHelper.getInstance().showNotification(i18nProvider.getTranslation("base.failed.title"), i18nProvider.getTranslation("base.failed.message"), -1, NotificationHelper.NotificationType.ERROR);
            }
        });
        sendButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.setWidthFull();

        Button homeButton = new Button(i18nProvider.getTranslation("base.login_button"), event -> UI.getCurrent().navigate(LoginView.class));
        homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
        homeButton.setWidthFull();

        cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, emailField, sendButton, homeButton);

        CardComponent cardComponent = new CardComponent(cardContentLayout);

        mainContent.add(cardComponent);
    }
}
