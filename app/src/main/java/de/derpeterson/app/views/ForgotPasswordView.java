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
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public ForgotPasswordView(
      MessageProperties messageProperties,
      PasswordResetService passwordResetService,
      SecurityService securityService,
      HttpServletRequest request,
      ConfigService configService) {
    super(securityService, request, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.passwordResetService = passwordResetService;

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());

          Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
          Optional.ofNullable(title)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getForgotPasswordTitle));
          Optional.ofNullable(text)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getForgotPasswordText));
          Optional.ofNullable(emailField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getPublicEmailField));
          Optional.ofNullable(sendButton)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getPublicRequestLink));
          Optional.ofNullable(loginButton)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getBaseLoginButton));

          ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
          updateInputPlaceholders();
          NotificationHelper.getInstance().updateText();
        });

    getContent().setWidthFull();
    getContent().setPadding(false);
    getContent().removeAll();
    setSpacing(false);
    PublicPageComponent pageLayout =
        new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.FORGOT);
    mainContent = pageLayout.getCardContainer();
    createPasswordResetCard();
    add(pageLayout);
  }

  private void createPasswordResetCard() {

    this.title = new H1(messageProperties.getForgotPasswordTitle());
    title.addClassName("cosmos-headline");

    this.text = new Span(messageProperties.getForgotPasswordText());
    text.addClassName("cosmos-form-text");
    this.emailField = new EmailField(messageProperties.getPublicEmailField());
    emailField.setPattern(UserInputRules.EMAIL_REGEX);
    emailField.setRequiredIndicatorVisible(true);
    emailField.setClearButtonVisible(true);
    emailField.setWidthFull();
    emailField.setClearButtonVisible(true);

    updateInputPlaceholders();

    this.sendButton =
        new Button(
            messageProperties.getPublicRequestLink(),
            event -> {
              if (!ValidationHelper.validateRequiredInputs(
                  List.of(emailField), messageProperties)) {
                return;
              }

              if (!ValidationHelper.validateEmailValidInputs(
                  List.of(emailField), messageProperties)) {
                return;
              }

              try {
                passwordResetService.sendPasswordResetEmail(emailField.getValue());
              } catch (IOException | RuntimeException e) {
                logger.error("Password reset request could not be processed", safeFailure(e));
              }
              // Identical text, style and duration even for account-dependent failures.
              NotificationHelper.getInstance()
                  .showNotification(
                      this.messageProperties::getBaseSuccessTitle,
                      this.messageProperties::getForgotPasswordSuccessMessage,
                      -1,
                      NotificationHelper.NotificationType.SUCCESS);
            });
    sendButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendButton.setWidthFull();

    this.loginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            event -> UI.getCurrent().navigate(LoginView.class));
    loginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    loginButton.setWidthFull();
    Div actions = new Div(loginButton, sendButton);
    actions.addClassName("public-panel-actions");

    CosmosCardComponent cardComponent =
        new PublicPanelComponent(
            AppIcon.KEY.create(),
            title,
            messageProperties::getPublicForgotSubtitle,
            new Component[] {emailField},
            actions);

    mainContent.add(cardComponent);
  }

  private void updateInputPlaceholders() {
    if (emailField != null) {
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
  }
}
