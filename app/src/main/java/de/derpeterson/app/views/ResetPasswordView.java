package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;
import static de.derpeterson.app.ui.components.CosmosCssClasses.STATUS_ICON;

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

  private final VerticalLayout resetPageContentLayout;

  private H1 resetPasswordTitleH1 = null;
  private PasswordField newPasswordField = null;
  private PasswordField confirmNewPasswordField = null;
  private Button saveNewPasswordButton = null;
  private Button returnHomeButton = null;
  private Button goToLoginButton = null;
  private Button requestNewResetLinkButton = null;
  private H1 resetSuccessTitleH1 = null;
  private Span resetSuccessMessageSpan = null;
  private H1 resetLinkProblemTitleH1 = null;
  private Span resetLinkProblemMessageSpan = null;
  private boolean hasResetLinkInspectionFailure;
  private LinkState resetLinkState = LinkState.INVALID;

  public ResetPasswordView(
      MessageProperties messageProperties,
      PasswordResetService passwordResetService,
      SecurityService securityService,
      HttpServletRequest httpServletRequest,
      ConfigService configService) {
    super(securityService, httpServletRequest, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.passwordResetService = passwordResetService;

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, this::applyResetPageLanguage);

    getContent().setWidthFull();
    getContent().setPadding(false);
    getContent().removeAll();
    setSpacing(false);

    PublicPageComponent resetPageComponent =
        new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.RESET);
    resetPageContentLayout = resetPageComponent.getCardContainer();
    add(resetPageComponent);
  }

  private void applyResetPageLanguage(LanguageChangeEvent languageChangeEvent) {
    VaadinSession.getCurrent().setLocale(languageChangeEvent.getNewLocale());

    Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
    Optional.ofNullable(resetPasswordTitleH1)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicResetFormTitle));
    Optional.ofNullable(newPasswordField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getResetPasswordNewPasswordField));
    Optional.ofNullable(confirmNewPasswordField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getResetPasswordConfirmField));
    Optional.ofNullable(saveNewPasswordButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicSavePasswordAction));
    Optional.ofNullable(goToLoginButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseLoginButton));

    Optional.ofNullable(returnHomeButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseHomeButton));
    Optional.ofNullable(requestNewResetLinkButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicRequestNewLink));

    Optional.ofNullable(resetSuccessTitleH1)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseSuccessTitle));
    Optional.ofNullable(resetSuccessMessageSpan)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getResetPasswordSuccessMessage));

    Optional.ofNullable(resetLinkProblemTitleH1)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(component, this::resolveResetLinkProblemTitleText));
    Optional.ofNullable(resetLinkProblemMessageSpan)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this::resolveResetLinkProblemMessageText));

    ComponentTextUpdateHelper.updateComponentTexts(componentTranslationSupplierMap);
    updateNewPasswordFieldPlaceholders();
    NotificationHelper.getInstance().updateText();
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String passwordResetToken) {
    if (passwordResetToken != null && !passwordResetToken.isEmpty()) {
      resetPageContentLayout.removeAll();
      hasResetLinkInspectionFailure = false;

      try {
        resetLinkState = passwordResetService.inspectToken(passwordResetToken);
        if (resetLinkState == LinkState.VALID) {
          showResetPasswordFormPanel(passwordResetToken);
        } else {
          showResetLinkProblemPanel();
        }
      } catch (RuntimeException passwordResetFailure) {
        logger.error(
            "Password reset link could not be validated", safeFailure(passwordResetFailure));
        hasResetLinkInspectionFailure = true;
        showResetLinkProblemPanel();
      }
    } else {
      beforeEvent.forwardTo(HomeView.class);
    }
  }

  private void showResetPasswordFormPanel(String passwordResetToken) {
    this.resetPasswordTitleH1 = new H1(messageProperties.getPublicResetFormTitle());
    resetPasswordTitleH1.addClassName(COSMOS_HEADLINE_CLASS);

    this.newPasswordField = new PasswordField(messageProperties.getResetPasswordNewPasswordField());
    newPasswordField.setClearButtonVisible(true);
    newPasswordField.setRequiredIndicatorVisible(true);
    newPasswordField.setWidthFull();

    this.confirmNewPasswordField =
        new PasswordField(messageProperties.getResetPasswordConfirmField());
    confirmNewPasswordField.setClearButtonVisible(true);
    confirmNewPasswordField.setRequiredIndicatorVisible(true);
    confirmNewPasswordField.setWidthFull();

    updateNewPasswordFieldPlaceholders();

    this.saveNewPasswordButton =
        new Button(
            messageProperties.getPublicSavePasswordAction(),
            _ -> submitPasswordReset(passwordResetToken));
    saveNewPasswordButton.setPrefixComponent(AppIcon.CHECK.create());
    saveNewPasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    saveNewPasswordButton.setWidthFull();

    this.goToLoginButton =
        new Button(
            messageProperties.getBaseLoginButton(), _ -> UI.getCurrent().navigate(LoginView.class));
    goToLoginButton.setPrefixComponent(AppIcon.SIGN_IN.create());

    Div resetFormActionsDiv = new Div(goToLoginButton, saveNewPasswordButton);
    resetFormActionsDiv.addClassName("public-panel-actions");

    CosmosCardComponent resetFormPanelComponent =
        new PublicPanelComponent(
            AppIcon.KEY.create(),
            resetPasswordTitleH1,
            messageProperties::getPublicResetSubtitle,
            new Component[] {newPasswordField, confirmNewPasswordField},
            resetFormActionsDiv);

    resetPageContentLayout.add(resetFormPanelComponent);
  }

  private void submitPasswordReset(String passwordResetToken) {
    if (!ValidationHelper.validateRequiredInputs(
        List.of(newPasswordField, confirmNewPasswordField), messageProperties)) {
      return;
    }

    if (!ValidationHelper.validatePasswordSecureInputs(
        List.of(newPasswordField, confirmNewPasswordField), messageProperties)) {
      return;
    }

    if (!ValidationHelper.validatePasswordConfirmInputs(
        newPasswordField, confirmNewPasswordField, messageProperties)) {
      return;
    }

    try {
      boolean wasPasswordReset =
          passwordResetService.resetPassword(passwordResetToken, newPasswordField.getValue());

      if (wasPasswordReset) {
        showPasswordResetSuccessPanel();

        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getBaseSuccessTitle,
                messageProperties::getResetPasswordSuccessMessage,
                -1,
                NotificationHelper.NotificationType.SUCCESS);
      } else {
        resetLinkState = passwordResetService.inspectToken(passwordResetToken);
        if (resetLinkState == LinkState.VALID) {
          resetLinkState = LinkState.INVALID;
        }

        showResetLinkProblemPanel();

        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getBaseFailedTitle,
                this::resolveResetLinkProblemMessageText,
                -1,
                NotificationHelper.NotificationType.ERROR);
      }
    } catch (RuntimeException passwordResetFailure) {
      logger.error("Password could not be reset", safeFailure(passwordResetFailure));
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getResetPasswordTechnicalErrorMessage,
              -1,
              NotificationHelper.NotificationType.ERROR);
    }
  }

  private void showPasswordResetSuccessPanel() {
    resetPageContentLayout.removeAll();
    clearResetPasswordFormReferences();

    SvgIcon resetSuccessSvgIcon = AppIcon.CHECK_CIRCLE.create();
    resetSuccessSvgIcon.addClassNames(STATUS_ICON, LumoUtility.TextColor.SUCCESS);

    this.resetSuccessTitleH1 = new H1(messageProperties.getBaseSuccessTitle());
    resetSuccessTitleH1.addClassName(COSMOS_HEADLINE_CLASS);

    this.resetSuccessMessageSpan = new Span(messageProperties.getResetPasswordSuccessMessage());
    resetSuccessMessageSpan.addClassName("cosmos-form-text");
    VerticalLayout resetSuccessMessageLayout = new VerticalLayout(resetSuccessMessageSpan);
    resetSuccessMessageLayout.addClassName("cosmos-intro");
    resetSuccessMessageLayout.setWidthFull();
    resetSuccessMessageLayout.setPadding(false);
    resetSuccessMessageLayout.setSpacing(false);
    resetSuccessMessageLayout.setAlignItems(FlexComponent.Alignment.START);

    this.returnHomeButton =
        new Button(
            messageProperties.getBaseHomeButton(), _ -> UI.getCurrent().navigate(HomeView.class));
    returnHomeButton.setPrefixComponent(AppIcon.ARROW_FORWARD.create());
    returnHomeButton.setWidthFull();
    returnHomeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    resetPageContentLayout.add(
        new PublicPanelComponent(
            resetSuccessSvgIcon,
            resetSuccessTitleH1,
            messageProperties::getPublicOutcomeSubtitle,
            new Component[] {resetSuccessMessageLayout},
            returnHomeButton));
  }

  private void showResetLinkProblemPanel() {
    resetPageContentLayout.removeAll();
    clearResetPasswordFormReferences();

    SvgIcon resetLinkProblemSvgIcon = AppIcon.WARNING.create();
    resetLinkProblemSvgIcon.addClassNames(
        STATUS_ICON,
        !hasResetLinkInspectionFailure && resetLinkState == LinkState.EXPIRED
            ? "cosmos-status-icon--warning"
            : LumoUtility.TextColor.ERROR);

    this.resetLinkProblemTitleH1 = new H1(resolveResetLinkProblemTitleText());
    resetLinkProblemTitleH1.addClassName(COSMOS_HEADLINE_CLASS);

    VerticalLayout resetLinkProblemMessageLayout = buildResetLinkProblemMessageLayout();

    Button resetLinkProblemActionButton;
    if (hasResetLinkInspectionFailure) {
      requestNewResetLinkButton = null;
      returnHomeButton =
          new Button(
              messageProperties.getBaseHomeButton(), _ -> UI.getCurrent().navigate(HomeView.class));
      returnHomeButton.setPrefixComponent(AppIcon.HOME.create());
      resetLinkProblemActionButton = returnHomeButton;
    } else {
      returnHomeButton = null;
      requestNewResetLinkButton =
          new Button(
              messageProperties.getPublicRequestNewLink(),
              _ -> UI.getCurrent().navigate(ForgotPasswordView.class));
      requestNewResetLinkButton.setPrefixComponent(AppIcon.ENVELOPE.create());
      resetLinkProblemActionButton = requestNewResetLinkButton;
    }
    resetLinkProblemActionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    CosmosCardComponent resetLinkProblemPanelComponent =
        new PublicPanelComponent(
            resetLinkProblemSvgIcon,
            resetLinkProblemTitleH1,
            messageProperties::getPublicOutcomeSubtitle,
            new Component[] {resetLinkProblemMessageLayout},
            resetLinkProblemActionButton);

    resetPageContentLayout.add(resetLinkProblemPanelComponent);
  }

  private void clearResetPasswordFormReferences() {
    resetPasswordTitleH1 = null;
    newPasswordField = null;
    confirmNewPasswordField = null;
    saveNewPasswordButton = null;
    goToLoginButton = null;
  }

  private VerticalLayout buildResetLinkProblemMessageLayout() {
    this.resetLinkProblemMessageSpan = new Span(resolveResetLinkProblemMessageText());
    resetLinkProblemMessageSpan.addClassName("cosmos-form-text");

    VerticalLayout resetLinkProblemMessageLayout = new VerticalLayout();
    resetLinkProblemMessageLayout.addClassName("cosmos-intro");
    resetLinkProblemMessageLayout.setWidthFull();
    resetLinkProblemMessageLayout.setPadding(false);
    resetLinkProblemMessageLayout.setSpacing(false);
    resetLinkProblemMessageLayout.setAlignItems(FlexComponent.Alignment.START);
    resetLinkProblemMessageLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    resetLinkProblemMessageLayout.add(resetLinkProblemMessageSpan);
    return resetLinkProblemMessageLayout;
  }

  private String resolveResetLinkProblemTitleText() {
    if (hasResetLinkInspectionFailure) {
      return messageProperties.getBaseFailedTitle();
    }
    return resetLinkState == LinkState.EXPIRED
        ? messageProperties.getResetPasswordExpiredTitle()
        : messageProperties.getResetPasswordInvalidTitle();
  }

  private String resolveResetLinkProblemMessageText() {
    if (hasResetLinkInspectionFailure) {
      return messageProperties.getResetPasswordTechnicalErrorMessage();
    }
    return resetLinkState == LinkState.EXPIRED
        ? messageProperties.getResetPasswordExpiredText()
        : messageProperties.getResetPasswordInvalidText();
  }

  private void updateNewPasswordFieldPlaceholders() {
    if (newPasswordField != null) {
      newPasswordField.setPlaceholder(messageProperties.getAccountNewPasswordPlaceholder());
    }
    if (confirmNewPasswordField != null) {
      confirmNewPasswordField.setPlaceholder(
          messageProperties.getAccountConfirmPasswordPlaceholder());
    }
  }
}
