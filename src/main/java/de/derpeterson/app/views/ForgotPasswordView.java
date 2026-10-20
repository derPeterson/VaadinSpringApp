package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
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
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPageComponent;
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
                          component, this.messageProperties::getForgotPasswordEmailField));
          Optional.ofNullable(sendButton)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getBaseSendButton));
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
    CosmosPageComponent pageLayout =
        new CosmosPageComponent(
            configService, false, messageProperties::getAccountCopyrightTemplate);
    mainContent = pageLayout.getCardContainer();
    createPasswordResetCard();
    add(pageLayout);
  }

  private void createPasswordResetCard() {
    VerticalLayout cardContentLayout = new VerticalLayout();
    cardContentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
    cardContentLayout.setPadding(false);
    cardContentLayout.setSpacing(false);
    cardContentLayout.addClassName("cosmos-form");

    Icon successIcon = VaadinIcon.QUESTION_CIRCLE.create();
    successIcon.addClassNames(
        "cosmos-forgot-icon", "cosmos-status-icon", LumoUtility.TextColor.PRIMARY);

    HorizontalLayout cardIconLayout = new HorizontalLayout();
    cardIconLayout.setWidthFull();
    cardIconLayout.setPadding(false);
    cardIconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardIconLayout.add(successIcon);

    this.title = new H1(messageProperties.getForgotPasswordTitle());
    title.addClassName("cosmos-headline");

    HorizontalLayout cardTitleLayout = new HorizontalLayout();
    cardTitleLayout.addClassName("cosmos-title-row");
    cardTitleLayout.setWidthFull();
    cardTitleLayout.setPadding(false);
    cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardTitleLayout.add(title);

    this.text = new Span(messageProperties.getForgotPasswordText());
    text.addClassName("cosmos-form-text");
    VerticalLayout cardTextLayout = new VerticalLayout();
    cardTextLayout.addClassName("cosmos-intro");
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setSpacing(false);
    cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardTextLayout.add(text);

    this.emailField = new EmailField(messageProperties.getForgotPasswordEmailField());
    emailField.setClearButtonVisible(true);
    emailField.setWidthFull();
    emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
    emailField.setClearButtonVisible(true);

    updateInputPlaceholders();

    this.sendButton =
        new Button(
            messageProperties.getBaseSendButton(),
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
    sendButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendButton.setWidthFull();

    this.loginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            event -> UI.getCurrent().navigate(LoginView.class));
    loginButton.setPrefixComponent(VaadinIcon.SIGN_IN.create());
    loginButton.setWidthFull();

    cardContentLayout.add(
        cardIconLayout, cardTitleLayout, cardTextLayout, emailField, sendButton, loginButton);

    CosmosCardComponent cardComponent = new CosmosCardComponent(cardContentLayout);

    mainContent.add(cardComponent);
  }

  private void updateInputPlaceholders() {
    if (emailField != null) {
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
  }
}
