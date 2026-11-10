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
import com.vaadin.flow.component.textfield.EmailField;
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
import de.derpeterson.app.service.VerificationService;
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

@Route(AppRouteConstants.VERIFICATION_ROUTE)
@PageTitle(AppRouteConstants.VERIFICATION_PAGE_TITLE)
@AnonymousAllowed
public class VerificationView extends IsNotAuthenticatedBaseView<HorizontalLayout>
    implements HasUrlParameter<String> {
  private static final String COSMOS_HEADLINE_CLASS = "cosmos-headline";
  private static final String COSMOS_FORM_TEXT_CLASS = "cosmos-form-text";
  private static final String COSMOS_INTRO_CLASS = "cosmos-intro";

  private static final Logger logger = LoggerFactory.getLogger(VerificationView.class);

  private final transient MessageProperties messageProperties;
  private final transient VerificationService verificationService;

  private final VerticalLayout mainContent;

  private H1 expiredTitle = null;
  private H1 notFoundTitle = null;
  private H1 successTitle = null;
  private Button resendButton = null;
  private Button homeButton = null;
  private Button loginButton = null;
  private EmailField emailField = null;
  private Button sendButton = null;
  private Span expiredText1 = null;
  private Span notFoundText1 = null;
  private Span notFoundText2 = null;
  private Span successText = null;

  public VerificationView(
      MessageProperties messageProperties,
      VerificationService verificationService,
      SecurityService securityService,
      HttpServletRequest request,
      ConfigService configService) {
    super(securityService, request, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.verificationService = verificationService;

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());

          Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
          Optional.ofNullable(expiredTitle)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationExpiredTitle));
          Optional.ofNullable(notFoundTitle)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationNotFoundTitle));
          Optional.ofNullable(successTitle)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationSuccessTitle));
          Optional.ofNullable(resendButton)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getBaseResendButton));
          Optional.ofNullable(homeButton)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getPublicHomeAction));
          Optional.ofNullable(emailField)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getPublicEmailField));
          Optional.ofNullable(homeButton)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getPublicHomeAction));
          Optional.ofNullable(sendButton)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationRequestActivationLink));
          Optional.ofNullable(expiredText1)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationExpiredText1));
          Optional.ofNullable(notFoundText1)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationNotFoundText1));
          Optional.ofNullable(notFoundText2)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationNotFoundText2));
          Optional.ofNullable(successText)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getVerificationSuccessText));
          Optional.ofNullable(loginButton)
              .ifPresent(component ->
                  componentTranslationSupplierMap.put(
                      component, this.messageProperties::getBaseLoginButton));

          ComponentTextUpdateHelper.updateComponentTexts(componentTranslationSupplierMap);
          updateInputPlaceholders();
          NotificationHelper.getInstance().updateText();
        });

    getContent().setWidthFull();
    getContent().setPadding(false);
    getContent().removeAll();
    setSpacing(false);
    PublicPageComponent pageLayout =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.VERIFICATION);
    mainContent = pageLayout.getCardContainer();
    add(pageLayout);
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    if (token != null && !token.isEmpty()) {
      boolean isValid = verificationService.validateToken(token);
      if (isValid) {
        createSuccessCard();
      } else {
        boolean exists = verificationService.existsToken(token);
        if (exists) {
          createExpiredCard(token);
        } else {
          createNotFoundCard();
        }
      }
    } else {
      event.forwardTo(HomeView.class);
    }
  }

  private void createExpiredCard(String token) {

    SvgIcon successIcon = AppIcon.WARNING.create();
    successIcon.addClassNames(STATUS_ICON, "cosmos-status-icon--warning");

    this.expiredTitle = new H1(messageProperties.getVerificationExpiredTitle());
    expiredTitle.addClassName(COSMOS_HEADLINE_CLASS);

    VerticalLayout cardTextLayout = createExpiredCardTextLayout();

    this.resendButton =
        new Button(
            messageProperties.getBaseResendButton(),
            event -> {
              try {
                boolean emailSent = verificationService.sendVerificationEmailByToken(token);
                if (emailSent) {
                  NotificationHelper.getInstance()
                      .showNotification(
                          this.messageProperties::getBaseSuccessTitle,
                          this.messageProperties::getVerificationExpiredSuccessMessage,
                          -1,
                          NotificationHelper.NotificationType.SUCCESS);
                } else {
                  NotificationHelper.getInstance()
                      .showNotification(
                          this.messageProperties::getBaseFailedTitle,
                          this.messageProperties::getBaseFailedMessage,
                          -1,
                          NotificationHelper.NotificationType.ERROR);
                }
              } catch (IOException e) {
                logger.error("❌ Exception occurred:", safeFailure(e));

                NotificationHelper.getInstance()
                    .showNotification(
                        this.messageProperties::getBaseFailedTitle,
                        this.messageProperties::getBaseFailedMessage,
                        -1,
                        NotificationHelper.NotificationType.ERROR);
              }
            });
    resendButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    resendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resendButton.setWidthFull();

    this.homeButton =
        new Button(
            messageProperties.getPublicHomeAction(),
            event -> UI.getCurrent().navigate(HomeView.class));
    homeButton.setPrefixComponent(AppIcon.HOME.create());
    homeButton.setWidthFull();
    Div actions = new Div(homeButton, resendButton);
    actions.addClassName("public-panel-actions");

    CosmosCardComponent cardComponent =
        new PublicPanelComponent(
            successIcon,
            expiredTitle,
            messageProperties::getPublicVerificationSubtitle,
            new Component[] {cardTextLayout},
            actions);

    mainContent.add(cardComponent);
  }

  private VerticalLayout createExpiredCardTextLayout() {
    this.expiredText1 = new Span(messageProperties.getVerificationExpiredText1());
    expiredText1.addClassName(COSMOS_FORM_TEXT_CLASS);
    VerticalLayout cardTextLayout = new VerticalLayout();
    cardTextLayout.addClassName(COSMOS_INTRO_CLASS);
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setSpacing(false);
    cardTextLayout.setAlignItems(FlexComponent.Alignment.START);
    cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    cardTextLayout.add(expiredText1);
    return cardTextLayout;
  }

  private VerticalLayout createNotFoundCardTextLayout(String text1String, String text2String) {
    this.notFoundText1 = new Span(text1String);
    notFoundText1.addClassName(COSMOS_FORM_TEXT_CLASS);
    this.notFoundText2 = new Span(text2String);
    notFoundText2.addClassName(COSMOS_FORM_TEXT_CLASS);
    VerticalLayout cardTextLayout = new VerticalLayout();
    cardTextLayout.addClassName(COSMOS_INTRO_CLASS);
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setSpacing(false);
    cardTextLayout.setAlignItems(FlexComponent.Alignment.START);
    cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    cardTextLayout.add(notFoundText1);
    cardTextLayout.add(notFoundText2);
    return cardTextLayout;
  }

  private void createNotFoundCard() {

    SvgIcon successIcon = AppIcon.WARNING.create();
    successIcon.addClassNames(STATUS_ICON, LumoUtility.TextColor.ERROR);

    this.notFoundTitle = new H1(messageProperties.getVerificationNotFoundTitle());
    notFoundTitle.addClassName(COSMOS_HEADLINE_CLASS);

    VerticalLayout cardTextLayout =
        createNotFoundCardTextLayout(
            messageProperties.getVerificationNotFoundText1(),
            messageProperties.getVerificationNotFoundText2());

    this.emailField = new EmailField(messageProperties.getPublicEmailField());
    emailField.setPattern(UserInputRules.EMAIL_REGEX);
    emailField.setWidthFull();
    emailField.setClearButtonVisible(true);
    emailField.setRequiredIndicatorVisible(true);

    updateInputPlaceholders();

    this.sendButton =
        new Button(
            messageProperties.getVerificationRequestActivationLink(),
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
                boolean emailSent =
                    verificationService.sendVerificationEmailByEmail(emailField.getValue());
                if (emailSent) {
                  NotificationHelper.getInstance()
                      .showNotification(
                          this.messageProperties::getBaseSuccessTitle,
                          this.messageProperties::getVerificationNotFoundSuccessMessage,
                          -1,
                          NotificationHelper.NotificationType.SUCCESS);
                } else {
                  NotificationHelper.getInstance()
                      .showNotification(
                          this.messageProperties::getBaseFailedTitle,
                          this.messageProperties::getBaseFailedMessage,
                          -1,
                          NotificationHelper.NotificationType.ERROR);
                }
              } catch (IOException e) {
                logger.error("❌ Exception occurred:", safeFailure(e));

                NotificationHelper.getInstance()
                    .showNotification(
                        this.messageProperties::getBaseFailedTitle,
                        this.messageProperties::getBaseFailedMessage,
                        -1,
                        NotificationHelper.NotificationType.ERROR);
              }
            });
    sendButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendButton.setWidthFull();

    this.homeButton =
        new Button(
            messageProperties.getPublicHomeAction(),
            event -> UI.getCurrent().navigate(HomeView.class));
    homeButton.setPrefixComponent(AppIcon.HOME.create());
    homeButton.setWidthFull();

    Div actions = new Div(homeButton, sendButton);
    actions.addClassName("public-panel-actions");

    CosmosCardComponent cardComponent =
        new PublicPanelComponent(
            successIcon,
            notFoundTitle,
            messageProperties::getPublicVerificationSubtitle,
            new Component[] {cardTextLayout, emailField},
            actions);

    mainContent.add(cardComponent);
  }

  private void createSuccessCard() {

    SvgIcon successIcon = AppIcon.CHECK_CIRCLE.create();
    successIcon.addClassNames(
        "cosmos-verification-success-icon", STATUS_ICON, LumoUtility.TextColor.SUCCESS);

    this.successTitle = new H1(messageProperties.getVerificationSuccessTitle());
    successTitle.addClassName(COSMOS_HEADLINE_CLASS);

    this.successText = new Span(messageProperties.getVerificationSuccessText());
    successText.addClassName(COSMOS_FORM_TEXT_CLASS);
    HorizontalLayout cardTextLayout = new HorizontalLayout();
    cardTextLayout.addClassName(COSMOS_INTRO_CLASS);
    cardTextLayout.setWidthFull();
    cardTextLayout.setPadding(false);
    cardTextLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    cardTextLayout.add(successText);

    this.loginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            event -> UI.getCurrent().navigate(LoginView.class));
    loginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loginButton.setWidthFull();

    CosmosCardComponent cardComponent =
        new PublicPanelComponent(
            successIcon,
            successTitle,
            messageProperties::getPublicVerificationSubtitle,
            new Component[] {cardTextLayout},
            loginButton);

    mainContent.add(cardComponent);
  }

  private void updateInputPlaceholders() {
    if (emailField != null) {
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
  }
}
