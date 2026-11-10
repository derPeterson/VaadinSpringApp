package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.IsNotAuthenticatedBaseView;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.PasswordResetService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(AppRouteConstants.FORGOT_PASSWORD_ROUTE)
@PageTitle(AppRouteConstants.FORGOT_PASSWORD_PAGE_TITLE)
@AnonymousAllowed
public class ForgotPasswordView extends IsNotAuthenticatedBaseView<HorizontalLayout> {

  private static final Logger passwordResetLogger =
      LoggerFactory.getLogger(ForgotPasswordView.class);

  private final transient MessageProperties messageProperties;
  private final transient PasswordResetService passwordResetService;

  private final VerticalLayout passwordResetCardsVerticalLayout;

  private H1 forgotPasswordH1 = null;
  private Span forgotPasswordMessageSpan = null;
  private EmailField passwordResetEmailField = null;
  private Button requestPasswordResetButton = null;
  private Button returnToLoginButton = null;

  public ForgotPasswordView(
      MessageProperties messageProperties,
      PasswordResetService passwordResetService,
      SecurityService securityService,
      HttpServletRequest httpServletRequest,
      ConfigService configService) {
    super(securityService, httpServletRequest, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.passwordResetService = passwordResetService;

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, this::applyForgotPasswordPageLanguage);

    getContent().setWidthFull();
    getContent().setPadding(false);
    getContent().removeAll();
    setSpacing(false);
    PublicPageComponent forgotPasswordPageComponent =
        new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.FORGOT);
    passwordResetCardsVerticalLayout = forgotPasswordPageComponent.getCardContainer();
    createPasswordResetCard();
    add(forgotPasswordPageComponent);
  }

  private void applyForgotPasswordPageLanguage(LanguageChangeEvent languageChangeEvent) {
    VaadinSession.getCurrent().setLocale(languageChangeEvent.getNewLocale());

    Map<Component, Supplier<String>> textSuppliersByComponent = new HashMap<>();
    addExistingComponentTranslation(
        textSuppliersByComponent, forgotPasswordH1, messageProperties::getForgotPasswordTitle);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        forgotPasswordMessageSpan,
        messageProperties::getForgotPasswordText);
    addExistingComponentTranslation(
        textSuppliersByComponent, passwordResetEmailField, messageProperties::getPublicEmailField);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        requestPasswordResetButton,
        messageProperties::getPublicRequestLink);
    addExistingComponentTranslation(
        textSuppliersByComponent, returnToLoginButton, messageProperties::getBaseLoginButton);

    ComponentTextUpdateHelper.updateComponentTexts(textSuppliersByComponent);
    updatePasswordResetEmailPlaceholder();
    NotificationHelper.getInstance().updateText();
  }

  private static void addExistingComponentTranslation(
      Map<Component, Supplier<String>> textSuppliersByComponent,
      Component translatedComponent,
      Supplier<String> componentTextSupplier) {
    if (translatedComponent != null) {
      textSuppliersByComponent.put(translatedComponent, componentTextSupplier);
    }
  }

  private void createPasswordResetCard() {
    this.forgotPasswordH1 = new H1(messageProperties.getForgotPasswordTitle());
    forgotPasswordH1.addClassName("cosmos-headline");

    this.forgotPasswordMessageSpan = new Span(messageProperties.getForgotPasswordText());
    forgotPasswordMessageSpan.addClassName("cosmos-form-text");

    this.passwordResetEmailField = new EmailField(messageProperties.getPublicEmailField());
    passwordResetEmailField.setPattern(UserInputRules.EMAIL_REGEX);
    passwordResetEmailField.setRequiredIndicatorVisible(true);
    passwordResetEmailField.setClearButtonVisible(true);
    passwordResetEmailField.setWidthFull();
    passwordResetEmailField.setClearButtonVisible(true);

    updatePasswordResetEmailPlaceholder();

    this.requestPasswordResetButton =
        new Button(
            messageProperties.getPublicRequestLink(),
            requestClickEvent -> requestPasswordResetLink());
    requestPasswordResetButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    requestPasswordResetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    requestPasswordResetButton.setWidthFull();

    this.returnToLoginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            loginClickEvent -> UI.getCurrent().navigate(LoginView.class));
    returnToLoginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    returnToLoginButton.setWidthFull();

    Div forgotPasswordActionsDiv = new Div(returnToLoginButton, requestPasswordResetButton);
    forgotPasswordActionsDiv.addClassName("public-panel-actions");

    PublicPanelComponent forgotPasswordPanelComponent =
        new PublicPanelComponent(
            AppIcon.KEY.create(),
            forgotPasswordH1,
            messageProperties::getPublicForgotSubtitle,
            new Component[] {passwordResetEmailField},
            forgotPasswordActionsDiv);

    passwordResetCardsVerticalLayout.add(forgotPasswordPanelComponent);
  }

  private void requestPasswordResetLink() {
    if (!ValidationHelper.validateRequiredInputs(
        List.of(passwordResetEmailField), messageProperties)) {
      return;
    }
    if (!ValidationHelper.validateEmailValidInputs(
        List.of(passwordResetEmailField), messageProperties)) {
      return;
    }

    try {
      passwordResetService.sendPasswordResetEmail(passwordResetEmailField.getValue());
    } catch (IOException | RuntimeException passwordResetRequestException) {
      passwordResetLogger.error(
          "Password reset request could not be processed",
          safeFailure(passwordResetRequestException));
    }

    // Auch bei kontobezogenen Fehlern bleibt die Rückmeldung gleich, damit sie kein Konto verrät.
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseSuccessTitle,
            messageProperties::getForgotPasswordSuccessMessage,
            -1,
            NotificationHelper.NotificationType.SUCCESS);
  }

  private void updatePasswordResetEmailPlaceholder() {
    if (passwordResetEmailField != null) {
      passwordResetEmailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
  }
}
