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
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPageComponent;
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
  private static final String COSMOS_FORM_CLASS = "cosmos-form";
  private static final String COSMOS_HEADLINE_CLASS = "cosmos-headline";
  private static final String COSMOS_TITLE_ROW_CLASS = "cosmos-title-row";

  private static final Logger logger = LoggerFactory.getLogger(ResetPasswordView.class);

  private final transient MessageProperties messageProperties;
  private final transient PasswordResetService passwordResetService;

  private final VerticalLayout mainContent;

  private H1 title = null;
  private PasswordField passwordField = null;
  private PasswordField confirmPasswordField = null;
  private Button resetButton = null;
  private Button homeButton = null;
  private H1 successTitle = null;
  private Span successText = null;
  private H1 invalidTitle = null;
  private Span invalidText = null;
  private boolean technicalFailure;

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
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());

          Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
          Optional.ofNullable(title)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getResetPasswordTitle));
          Optional.ofNullable(passwordField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getResetPasswordNewPasswordField));
          Optional.ofNullable(confirmPasswordField)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getResetPasswordConfirmField));
          Optional.ofNullable(resetButton)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getBaseResetButton));
          Optional.ofNullable(homeButton)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getBaseHomeButton));
          Optional.ofNullable(successTitle)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getBaseSuccessTitle));
          Optional.ofNullable(successText)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component, this.messageProperties::getResetPasswordSuccessMessage));
          Optional.ofNullable(invalidTitle)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component,
                          () ->
                              technicalFailure
                                  ? this.messageProperties.getBaseFailedTitle()
                                  : this.messageProperties.getResetPasswordInvalidTitle()));
          Optional.ofNullable(invalidText)
              .ifPresent(
                  component ->
                      componentTranslationSupplierMap.put(
                          component,
                          () ->
                              technicalFailure
                                  ? this.messageProperties.getResetPasswordTechnicalErrorMessage()
                                  : this.messageProperties.getResetPasswordInvalidText()));

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
    add(pageLayout);
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    if (token != null && !token.isEmpty()) {
      mainContent.removeAll();
      technicalFailure = false;
      try {
        if (passwordResetService.validateToken(token)) {
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
    VerticalLayout cardContentLayout = new VerticalLayout();
    cardContentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
    cardContentLayout.setPadding(false);
    cardContentLayout.setSpacing(false);
    cardContentLayout.addClassName(COSMOS_FORM_CLASS);

    this.title = new H1(messageProperties.getResetPasswordTitle());
    title.addClassName(COSMOS_HEADLINE_CLASS);

    HorizontalLayout cardTitleLayout = new HorizontalLayout();
    cardTitleLayout.addClassName(COSMOS_TITLE_ROW_CLASS);
    cardTitleLayout.setWidthFull();
    cardTitleLayout.setPadding(false);
    cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardTitleLayout.add(title);

    this.passwordField = new PasswordField(messageProperties.getResetPasswordNewPasswordField());
    passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
    passwordField.setClearButtonVisible(true);
    passwordField.setRequiredIndicatorVisible(true);
    passwordField.setWidthFull();

    this.confirmPasswordField = new PasswordField(messageProperties.getResetPasswordConfirmField());
    confirmPasswordField.setPrefixComponent(VaadinIcon.LOCK.create());
    confirmPasswordField.setClearButtonVisible(true);
    confirmPasswordField.setRequiredIndicatorVisible(true);
    confirmPasswordField.setWidthFull();

    updateInputPlaceholders();

    this.resetButton =
        new Button(
            messageProperties.getBaseResetButton(),
            event -> {
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
                boolean success =
                    passwordResetService.resetPassword(token, passwordField.getValue());
                if (success) {
                  showSuccessCard();
                  NotificationHelper.getInstance()
                      .showNotification(
                          messageProperties::getBaseSuccessTitle,
                          messageProperties::getResetPasswordSuccessMessage,
                          -1,
                          NotificationHelper.NotificationType.SUCCESS);
                } else {
                  NotificationHelper.getInstance()
                      .showNotification(
                          messageProperties::getBaseFailedTitle,
                          messageProperties::getResetPasswordInvalidText,
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
            });
    resetButton.setPrefixComponent(VaadinIcon.PAPERPLANE.create());
    resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resetButton.setWidthFull();

    this.homeButton =
        new Button(
            messageProperties.getBaseHomeButton(),
            event -> UI.getCurrent().navigate(HomeView.class));
    homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
    homeButton.setWidthFull();

    cardContentLayout.add(
        cardTitleLayout, passwordField, confirmPasswordField, resetButton, homeButton);

    CosmosCardComponent cardComponent = new CosmosCardComponent(cardContentLayout);

    mainContent.add(cardComponent);
  }

  private void showSuccessCard() {
    mainContent.removeAll();
    // Nach erfolgreichem Verbrauch des Tokens nur noch die Bestätigung anzeigen.
    title = null;
    passwordField = null;
    confirmPasswordField = null;
    resetButton = null;

    VerticalLayout cardContentLayout = new VerticalLayout();
    cardContentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
    cardContentLayout.setPadding(false);
    cardContentLayout.setSpacing(false);
    cardContentLayout.addClassName(COSMOS_FORM_CLASS);

    Icon successIcon = VaadinIcon.CHECK_CIRCLE.create();
    successIcon.addClassNames("cosmos-status-icon", LumoUtility.TextColor.SUCCESS);

    HorizontalLayout cardIconLayout = new HorizontalLayout();
    cardIconLayout.setWidthFull();
    cardIconLayout.setPadding(false);
    cardIconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardIconLayout.add(successIcon);

    this.successTitle = new H1(messageProperties.getBaseSuccessTitle());
    successTitle.addClassName(COSMOS_HEADLINE_CLASS);
    HorizontalLayout cardTitleLayout = new HorizontalLayout();
    cardTitleLayout.addClassName(COSMOS_TITLE_ROW_CLASS);
    cardTitleLayout.setWidthFull();
    cardTitleLayout.setPadding(false);
    cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardTitleLayout.add(successTitle);

    this.successText = new Span(messageProperties.getResetPasswordSuccessMessage());
    successText.addClassName("cosmos-form-text");
    VerticalLayout cardTextLayout = new VerticalLayout(successText);
    cardTextLayout.addClassName("cosmos-intro");
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setSpacing(false);
    cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);

    this.homeButton =
        new Button(
            messageProperties.getBaseHomeButton(),
            event -> UI.getCurrent().navigate(HomeView.class));
    homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
    homeButton.setWidthFull();
    homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, homeButton);
    mainContent.add(new CosmosCardComponent(cardContentLayout));
  }

  private void invalidTokenCard() {
    VerticalLayout cardContentLayout = new VerticalLayout();
    cardContentLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
    cardContentLayout.setPadding(false);
    cardContentLayout.setSpacing(false);
    cardContentLayout.addClassName(COSMOS_FORM_CLASS);

    Icon warningIcon = VaadinIcon.WARNING.create();
    warningIcon.addClassNames(
        "cosmos-status-icon",
        technicalFailure ? LumoUtility.TextColor.ERROR : "cosmos-status-icon--warning");

    HorizontalLayout cardIconLayout = new HorizontalLayout();
    cardIconLayout.setWidthFull();
    cardIconLayout.setPadding(false);
    cardIconLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardIconLayout.add(warningIcon);

    this.invalidTitle =
        new H1(
            technicalFailure
                ? messageProperties.getBaseFailedTitle()
                : messageProperties.getResetPasswordInvalidTitle());
    invalidTitle.addClassName(COSMOS_HEADLINE_CLASS);

    HorizontalLayout cardTitleLayout = new HorizontalLayout();
    cardTitleLayout.addClassName(COSMOS_TITLE_ROW_CLASS);
    cardTitleLayout.setWidthFull();
    cardTitleLayout.setPadding(false);
    cardTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardTitleLayout.add(invalidTitle);

    VerticalLayout cardTextLayout = getCardTextLayout();

    this.homeButton =
        new Button(
            messageProperties.getBaseHomeButton(),
            event -> UI.getCurrent().navigate(HomeView.class));
    homeButton.setPrefixComponent(VaadinIcon.ARROW_FORWARD.create());
    homeButton.setWidthFull();
    homeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    cardContentLayout.add(cardIconLayout, cardTitleLayout, cardTextLayout, homeButton);

    CosmosCardComponent cardComponent = new CosmosCardComponent(cardContentLayout);

    mainContent.add(cardComponent);
  }

  private VerticalLayout getCardTextLayout() {
    this.invalidText =
        new Span(
            technicalFailure
                ? messageProperties.getResetPasswordTechnicalErrorMessage()
                : messageProperties.getResetPasswordInvalidText());
    invalidText.addClassName("cosmos-form-text");
    VerticalLayout cardTextLayout = new VerticalLayout();
    cardTextLayout.addClassName("cosmos-intro");
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setSpacing(false);
    cardTextLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    cardTextLayout.add(invalidText);
    return cardTextLayout;
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
