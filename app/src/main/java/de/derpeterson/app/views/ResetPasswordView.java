package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosClasses.STATUS_ICON;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
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
import de.derpeterson.app.service.PasswordResetService.LinkState;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(AppRouteConstants.RESET_PASSWORD_ROUTE)
@PageTitle(AppRouteConstants.RESET_PAGE_TITLE)
@AnonymousAllowed
public class ResetPasswordView extends IsNotAuthenticatedBaseView<HorizontalLayout>
    implements HasUrlParameter<String> {
  private static final String COSMOS_HEADLINE_CLASS = "cosmos-headline";

  private static final Logger logger = LoggerFactory.getLogger(ResetPasswordView.class);

  private final transient MessageProperties messageProperties;
  private final transient PasswordResetService passwordResetService;

  private final VerticalLayout mainContent;

  private H1 title = null;
  private PasswordField passwordField = null;
  private PasswordField confirmPasswordField = null;
  private Button resetButton = null;
  private Button homeButton = null;
  private Button loginButton = null;
  private Button requestLinkButton = null;
  private H1 successTitle = null;
  private Span successText = null;
  private H1 invalidTitle = null;
  private Span invalidText = null;
  private boolean technicalFailure;
  private LinkState linkState = LinkState.INVALID;

  public ResetPasswordView(
      MessageProperties messageProperties,
      PasswordResetService passwordResetService,
      SecurityService securityService,
      HttpServletRequest request,
      ConfigService configService) {
    super(securityService, request, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.passwordResetService = passwordResetService;

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, this::applyResetPageLanguage);

    getContent().setWidthFull();
    getContent().setPadding(false);
    getContent().removeAll();
    setSpacing(false);
    PublicPageComponent pageLayout =
        new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.RESET);
    mainContent = pageLayout.getCardContainer();
    add(pageLayout);
  }

  private void applyResetPageLanguage(LanguageChangeEvent event) {
    VaadinSession.getCurrent().setLocale(event.getNewLocale());

    Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
    Optional.ofNullable(title)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicResetFormTitle));
    Optional.ofNullable(passwordField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getResetPasswordNewPasswordField));
    Optional.ofNullable(confirmPasswordField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getResetPasswordConfirmField));
    Optional.ofNullable(resetButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicSavePasswordAction));
    Optional.ofNullable(loginButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseLoginButton));
    Optional.ofNullable(homeButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseHomeButton));
    Optional.ofNullable(requestLinkButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicRequestNewLink));
    Optional.ofNullable(successTitle)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseSuccessTitle));
    Optional.ofNullable(successText)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getResetPasswordSuccessMessage));
    Optional.ofNullable(invalidTitle)
        .ifPresent(
            component -> componentTranslationSupplierMap.put(component, this::linkProblemTitle));
    Optional.ofNullable(invalidText)
        .ifPresent(
            component -> componentTranslationSupplierMap.put(component, this::linkProblemText));

    ComponentTextUpdateHelper.updateComponents(componentTranslationSupplierMap);
    updateInputPlaceholders();
    NotificationHelper.getInstance().updateText();
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    if (token != null && !token.isEmpty()) {
      mainContent.removeAll();
      technicalFailure = false;
      try {
        linkState = passwordResetService.inspectToken(token);
        if (linkState == LinkState.VALID) {
          showResetPasswordCard(token);
        } else {
          invalidTokenCard();
        }
      } catch (RuntimeException failure) {
        logger.error("Password reset link could not be validated", safeFailure(failure));
        technicalFailure = true;
        invalidTokenCard();
      }
    } else {
      event.forwardTo(HomeView.class);
    }
  }

  private void showResetPasswordCard(String token) {

    this.title = new H1(messageProperties.getPublicResetFormTitle());
    title.addClassName(COSMOS_HEADLINE_CLASS);

    this.passwordField = new PasswordField(messageProperties.getResetPasswordNewPasswordField());
    passwordField.setClearButtonVisible(true);
    passwordField.setRequiredIndicatorVisible(true);
    passwordField.setWidthFull();

    this.confirmPasswordField = new PasswordField(messageProperties.getResetPasswordConfirmField());
    confirmPasswordField.setClearButtonVisible(true);
    confirmPasswordField.setRequiredIndicatorVisible(true);
    confirmPasswordField.setWidthFull();

    updateInputPlaceholders();

    this.resetButton =
        new Button(
            messageProperties.getPublicSavePasswordAction(), event -> submitPasswordReset(token));
    resetButton.setPrefixComponent(AppIcon.CHECK.create());
    resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resetButton.setWidthFull();

    this.loginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            event -> UI.getCurrent().navigate(LoginView.class));
    loginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    Div actions = new Div(loginButton, resetButton);
    actions.addClassName("public-panel-actions");

    CosmosCardComponent cardComponent =
        new PublicPanelComponent(
            AppIcon.KEY.create(),
            title,
            messageProperties::getPublicResetSubtitle,
            new Component[] {passwordField, confirmPasswordField},
            actions);

    mainContent.add(cardComponent);
  }

  private void submitPasswordReset(String token) {
    if (!ValidationHelper.validateRequiredInputs(
        List.of(passwordField, confirmPasswordField), messageProperties)) {
      return;
    }

    if (!ValidationHelper.validatePasswordSecureInputs(
        List.of(passwordField, confirmPasswordField), messageProperties)) {
      return;
    }

    if (!ValidationHelper.validatePasswordConfirmInputs(
        passwordField, confirmPasswordField, messageProperties)) {
      return;
    }

    try {
      boolean success = passwordResetService.resetPassword(token, passwordField.getValue());
      if (success) {
        showSuccessCard();
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getBaseSuccessTitle,
                messageProperties::getResetPasswordSuccessMessage,
                -1,
                NotificationHelper.NotificationType.SUCCESS);
      } else {
        linkState = passwordResetService.inspectToken(token);
        if (linkState == LinkState.VALID) {
          linkState = LinkState.INVALID;
        }
        invalidTokenCard();
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getBaseFailedTitle,
                this::linkProblemText,
                -1,
                NotificationHelper.NotificationType.ERROR);
      }
    } catch (RuntimeException failure) {
      logger.error("Password could not be reset", safeFailure(failure));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getResetPasswordTechnicalErrorMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
    }
  }

  private void showSuccessCard() {
    mainContent.removeAll();
    // Nach erfolgreichem Verbrauch des Tokens nur noch die Bestätigung anzeigen.
    title = null;
    passwordField = null;
    confirmPasswordField = null;
    resetButton = null;
    loginButton = null;

    SvgIcon successIcon = AppIcon.CHECK_CIRCLE.create();
    successIcon.addClassNames(STATUS_ICON, LumoUtility.TextColor.SUCCESS);

    this.successTitle = new H1(messageProperties.getBaseSuccessTitle());
    successTitle.addClassName(COSMOS_HEADLINE_CLASS);

    this.successText = new Span(messageProperties.getResetPasswordSuccessMessage());
    successText.addClassName("cosmos-form-text");
    VerticalLayout cardTextLayout = new VerticalLayout(successText);
    cardTextLayout.addClassName("cosmos-intro");
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setSpacing(false);
    cardTextLayout.setAlignItems(FlexComponent.Alignment.START);

    this.homeButton =
        new Button(
            messageProperties.getBaseHomeButton(),
            event -> UI.getCurrent().navigate(HomeView.class));
    homeButton.setPrefixComponent(AppIcon.ARROW_FORWARD.create());
    homeButton.setWidthFull();
    homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    mainContent.add(
        new PublicPanelComponent(
            successIcon,
            successTitle,
            messageProperties::getPublicOutcomeSubtitle,
            new Component[] {cardTextLayout},
            homeButton));
  }

  private void invalidTokenCard() {
    mainContent.removeAll();
    title = null;
    passwordField = null;
    confirmPasswordField = null;
    resetButton = null;
    loginButton = null;

    SvgIcon warningIcon = AppIcon.WARNING.create();
    warningIcon.addClassNames(
        STATUS_ICON,
        !technicalFailure && linkState == LinkState.EXPIRED
            ? "cosmos-status-icon--warning"
            : LumoUtility.TextColor.ERROR);

    this.invalidTitle = new H1(linkProblemTitle());
    invalidTitle.addClassName(COSMOS_HEADLINE_CLASS);

    VerticalLayout cardTextLayout = getCardTextLayout();

    Button action;
    if (technicalFailure) {
      requestLinkButton = null;
      homeButton =
          new Button(
              messageProperties.getBaseHomeButton(),
              event -> UI.getCurrent().navigate(HomeView.class));
      homeButton.setPrefixComponent(AppIcon.HOME.create());
      action = homeButton;
    } else {
      homeButton = null;
      requestLinkButton =
          new Button(
              messageProperties.getPublicRequestNewLink(),
              event -> UI.getCurrent().navigate(ForgotPasswordView.class));
      requestLinkButton.setPrefixComponent(AppIcon.ENVELOPE.create());
      action = requestLinkButton;
    }
    action.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    CosmosCardComponent cardComponent =
        new PublicPanelComponent(
            warningIcon,
            invalidTitle,
            messageProperties::getPublicOutcomeSubtitle,
            new Component[] {cardTextLayout},
            action);

    mainContent.add(cardComponent);
  }

  private VerticalLayout getCardTextLayout() {
    this.invalidText = new Span(linkProblemText());
    invalidText.addClassName("cosmos-form-text");
    VerticalLayout cardTextLayout = new VerticalLayout();
    cardTextLayout.addClassName("cosmos-intro");
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setSpacing(false);
    cardTextLayout.setAlignItems(FlexComponent.Alignment.START);
    cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    cardTextLayout.add(invalidText);
    return cardTextLayout;
  }

  private String linkProblemTitle() {
    if (technicalFailure) {
      return messageProperties.getBaseFailedTitle();
    }
    return linkState == LinkState.EXPIRED
        ? messageProperties.getResetPasswordExpiredTitle()
        : messageProperties.getResetPasswordInvalidTitle();
  }

  private String linkProblemText() {
    if (technicalFailure) {
      return messageProperties.getResetPasswordTechnicalErrorMessage();
    }
    return linkState == LinkState.EXPIRED
        ? messageProperties.getResetPasswordExpiredText()
        : messageProperties.getResetPasswordInvalidText();
  }

  private void updateInputPlaceholders() {
    if (passwordField != null) {
      passwordField.setPlaceholder(messageProperties.getAccountNewPasswordPlaceholder());
    }
    if (confirmPasswordField != null) {
      confirmPasswordField.setPlaceholder(messageProperties.getAccountConfirmPasswordPlaceholder());
    }
  }
}
