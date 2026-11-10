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

@Route(AppRouteConstants.VERIFICATION_ROUTE)
@PageTitle(AppRouteConstants.VERIFICATION_PAGE_TITLE)
@AnonymousAllowed
public class VerificationView extends IsNotAuthenticatedBaseView<HorizontalLayout>
    implements HasUrlParameter<String> {
  private static final String HEADLINE_CSS_CLASS = "cosmos-headline";
  private static final String FORM_TEXT_CSS_CLASS = "cosmos-form-text";
  private static final String INTRO_CSS_CLASS = "cosmos-intro";
  private static final Logger verificationLogger = LoggerFactory.getLogger(VerificationView.class);

  private final transient MessageProperties messageProperties;
  private final transient VerificationService verificationService;
  private final VerticalLayout verificationCardsVerticalLayout;
  private Button verificationHomeButton;

  private H1 expiredLinkH1;
  private Span expiredLinkHintSpan;
  private Button resendVerificationLinkButton;

  private H1 unknownLinkH1;
  private Span invalidLinkExplanationSpan;
  private Span requestNewLinkHintSpan;
  private EmailField verificationEmailField;
  private Button requestVerificationLinkButton;

  private H1 verificationSuccessH1;
  private Span verificationSuccessMessageSpan;
  private Button verificationLoginButton;

  public VerificationView(
      MessageProperties messageProperties,
      VerificationService verificationService,
      SecurityService securityService,
      HttpServletRequest httpServletRequest,
      ConfigService configService) {
    super(securityService, httpServletRequest, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.verificationService = verificationService;

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, this::applyVerificationPageLanguage);

    getContent().setWidthFull();
    getContent().setPadding(false);
    getContent().removeAll();
    setSpacing(false);

    PublicPageComponent verificationPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.VERIFICATION);
    verificationCardsVerticalLayout = verificationPageComponent.getCardContainer();
    add(verificationPageComponent);
  }

  @Override
  public void setParameter(
      BeforeEvent beforeNavigationEvent, @OptionalParameter String verificationToken) {
    if (verificationToken == null || verificationToken.isEmpty()) {
      beforeNavigationEvent.forwardTo(HomeView.class);
      return;
    }

    // Die Prüfung kann das Konto freischalten und den Token verbrauchen.
    boolean accountVerificationSucceeded = verificationService.validateToken(verificationToken);
    if (accountVerificationSucceeded) {
      showVerificationSuccessCard();
      return;
    }

    if (verificationService.existsToken(verificationToken)) {
      showExpiredLinkCard(verificationToken);
    } else {
      showUnknownLinkCard();
    }
  }

  private void applyVerificationPageLanguage(LanguageChangeEvent languageChangeEvent) {
    VaadinSession.getCurrent().setLocale(languageChangeEvent.getNewLocale());

    Map<Component, Supplier<String>> textSuppliersByComponent = new HashMap<>();
    addExistingComponentTranslation(
        textSuppliersByComponent, verificationHomeButton, messageProperties::getPublicHomeAction);

    addExistingComponentTranslation(
        textSuppliersByComponent, expiredLinkH1, messageProperties::getVerificationExpiredTitle);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        expiredLinkHintSpan,
        messageProperties::getVerificationExpiredText1);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        resendVerificationLinkButton,
        messageProperties::getBaseResendButton);

    addExistingComponentTranslation(
        textSuppliersByComponent, unknownLinkH1, messageProperties::getVerificationNotFoundTitle);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        invalidLinkExplanationSpan,
        messageProperties::getVerificationNotFoundText1);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        requestNewLinkHintSpan,
        messageProperties::getVerificationNotFoundText2);
    addExistingComponentTranslation(
        textSuppliersByComponent, verificationEmailField, messageProperties::getPublicEmailField);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        requestVerificationLinkButton,
        messageProperties::getVerificationRequestActivationLink);

    addExistingComponentTranslation(
        textSuppliersByComponent,
        verificationSuccessH1,
        messageProperties::getVerificationSuccessTitle);
    addExistingComponentTranslation(
        textSuppliersByComponent,
        verificationSuccessMessageSpan,
        messageProperties::getVerificationSuccessText);
    addExistingComponentTranslation(
        textSuppliersByComponent, verificationLoginButton, messageProperties::getBaseLoginButton);

    ComponentTextUpdateHelper.updateComponentTexts(textSuppliersByComponent);
    updateVerificationEmailPlaceholder();
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

  private void showExpiredLinkCard(String verificationToken) {
    SvgIcon expiredLinkWarningSvgIcon = AppIcon.WARNING.create();
    expiredLinkWarningSvgIcon.addClassNames(STATUS_ICON, "cosmos-status-icon--warning");

    expiredLinkH1 = new H1(messageProperties.getVerificationExpiredTitle());
    expiredLinkH1.addClassName(HEADLINE_CSS_CLASS);
    expiredLinkHintSpan = new Span(messageProperties.getVerificationExpiredText1());
    expiredLinkHintSpan.addClassName(FORM_TEXT_CSS_CLASS);
    VerticalLayout expiredLinkTextVerticalLayout =
        createVerificationHelpVerticalLayout(expiredLinkHintSpan);

    resendVerificationLinkButton =
        new Button(
            messageProperties.getBaseResendButton(),
            resendClickEvent -> requestVerificationLinkByToken(verificationToken));
    resendVerificationLinkButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    resendVerificationLinkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resendVerificationLinkButton.setWidthFull();

    verificationHomeButton = createVerificationHomeButton();
    Div expiredLinkActionsDiv = new Div(verificationHomeButton, resendVerificationLinkButton);
    expiredLinkActionsDiv.addClassName("public-panel-actions");

    PublicPanelComponent expiredLinkPanelComponent =
        new PublicPanelComponent(
            expiredLinkWarningSvgIcon,
            expiredLinkH1,
            messageProperties::getPublicVerificationSubtitle,
            new Component[] {expiredLinkTextVerticalLayout},
            expiredLinkActionsDiv);
    verificationCardsVerticalLayout.add(expiredLinkPanelComponent);
  }

  private void requestVerificationLinkByToken(String verificationToken) {
    try {
      boolean verificationRequestAccepted =
          verificationService.sendVerificationEmailByToken(verificationToken);

      showVerificationRequestResult(
          verificationRequestAccepted, messageProperties::getVerificationExpiredSuccessMessage);
    } catch (IOException verificationRequestIOException) {
      verificationLogger.error(
          "❌ Exception occurred:", safeFailure(verificationRequestIOException));
      showVerificationRequestFailure();
    }
  }

  private void showUnknownLinkCard() {
    SvgIcon invalidLinkWarningSvgIcon = AppIcon.WARNING.create();
    invalidLinkWarningSvgIcon.addClassNames(STATUS_ICON, LumoUtility.TextColor.ERROR);

    unknownLinkH1 = new H1(messageProperties.getVerificationNotFoundTitle());
    unknownLinkH1.addClassName(HEADLINE_CSS_CLASS);
    invalidLinkExplanationSpan = new Span(messageProperties.getVerificationNotFoundText1());
    invalidLinkExplanationSpan.addClassName(FORM_TEXT_CSS_CLASS);
    requestNewLinkHintSpan = new Span(messageProperties.getVerificationNotFoundText2());
    requestNewLinkHintSpan.addClassName(FORM_TEXT_CSS_CLASS);
    VerticalLayout unknownLinkTextVerticalLayout =
        createVerificationHelpVerticalLayout(invalidLinkExplanationSpan, requestNewLinkHintSpan);

    verificationEmailField = new EmailField(messageProperties.getPublicEmailField());
    verificationEmailField.setPattern(UserInputRules.EMAIL_REGEX);
    verificationEmailField.setWidthFull();
    verificationEmailField.setClearButtonVisible(true);
    verificationEmailField.setRequiredIndicatorVisible(true);
    updateVerificationEmailPlaceholder();

    requestVerificationLinkButton =
        new Button(
            messageProperties.getVerificationRequestActivationLink(),
            requestClickEvent -> requestVerificationLinkByEmail());
    requestVerificationLinkButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    requestVerificationLinkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    requestVerificationLinkButton.setWidthFull();

    verificationHomeButton = createVerificationHomeButton();
    Div unknownLinkActionsDiv = new Div(verificationHomeButton, requestVerificationLinkButton);
    unknownLinkActionsDiv.addClassName("public-panel-actions");

    PublicPanelComponent unknownLinkPanelComponent =
        new PublicPanelComponent(
            invalidLinkWarningSvgIcon,
            unknownLinkH1,
            messageProperties::getPublicVerificationSubtitle,
            new Component[] {unknownLinkTextVerticalLayout, verificationEmailField},
            unknownLinkActionsDiv);
    verificationCardsVerticalLayout.add(unknownLinkPanelComponent);
  }

  private void requestVerificationLinkByEmail() {
    if (!ValidationHelper.validateRequiredInputs(
        List.of(verificationEmailField), messageProperties)) {
      return;
    }
    if (!ValidationHelper.validateEmailValidInputs(
        List.of(verificationEmailField), messageProperties)) {
      return;
    }

    try {
      boolean verificationRequestAccepted =
          verificationService.sendVerificationEmailByEmail(verificationEmailField.getValue());

      showVerificationRequestResult(
          verificationRequestAccepted, messageProperties::getVerificationNotFoundSuccessMessage);
    } catch (IOException verificationRequestIOException) {
      verificationLogger.error(
          "❌ Exception occurred:", safeFailure(verificationRequestIOException));
      showVerificationRequestFailure();
    }
  }

  private void updateVerificationEmailPlaceholder() {
    if (verificationEmailField != null) {
      verificationEmailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
  }

  private void showVerificationRequestResult(
      boolean verificationRequestAccepted, Supplier<String> successMessageSupplier) {
    if (verificationRequestAccepted) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseSuccessTitle,
              successMessageSupplier,
              -1,
              NotificationHelper.NotificationType.SUCCESS);
    } else {
      showVerificationRequestFailure();
    }
  }

  private void showVerificationRequestFailure() {
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            messageProperties::getBaseFailedMessage,
            -1,
            NotificationHelper.NotificationType.ERROR);
  }

  private VerticalLayout createVerificationHelpVerticalLayout(Span... verificationHintSpans) {
    VerticalLayout verificationHelpVerticalLayout = new VerticalLayout();
    verificationHelpVerticalLayout.addClassName(INTRO_CSS_CLASS);
    verificationHelpVerticalLayout.setWidthFull();
    verificationHelpVerticalLayout.setPadding(false);
    verificationHelpVerticalLayout.setSpacing(false);
    verificationHelpVerticalLayout.setAlignItems(FlexComponent.Alignment.START);
    verificationHelpVerticalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    verificationHelpVerticalLayout.add(verificationHintSpans);
    return verificationHelpVerticalLayout;
  }

  private Button createVerificationHomeButton() {
    Button homeNavigationButton =
        new Button(
            messageProperties.getPublicHomeAction(),
            homeClickEvent -> UI.getCurrent().navigate(HomeView.class));
    homeNavigationButton.setPrefixComponent(AppIcon.HOME.create());
    homeNavigationButton.setWidthFull();
    return homeNavigationButton;
  }

  private void showVerificationSuccessCard() {
    SvgIcon verificationSuccessSvgIcon = AppIcon.CHECK_CIRCLE.create();
    verificationSuccessSvgIcon.addClassNames(
        "cosmos-verification-success-icon", STATUS_ICON, LumoUtility.TextColor.SUCCESS);

    verificationSuccessH1 = new H1(messageProperties.getVerificationSuccessTitle());
    verificationSuccessH1.addClassName(HEADLINE_CSS_CLASS);

    verificationSuccessMessageSpan = new Span(messageProperties.getVerificationSuccessText());
    verificationSuccessMessageSpan.addClassName(FORM_TEXT_CSS_CLASS);
    HorizontalLayout verificationSuccessTextHorizontalLayout = new HorizontalLayout();
    verificationSuccessTextHorizontalLayout.addClassName(INTRO_CSS_CLASS);
    verificationSuccessTextHorizontalLayout.setWidthFull();
    verificationSuccessTextHorizontalLayout.setPadding(false);
    verificationSuccessTextHorizontalLayout.setJustifyContentMode(
        FlexComponent.JustifyContentMode.START);
    verificationSuccessTextHorizontalLayout.add(verificationSuccessMessageSpan);

    verificationLoginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            loginClickEvent -> UI.getCurrent().navigate(LoginView.class));
    verificationLoginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    verificationLoginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    verificationLoginButton.setWidthFull();

    PublicPanelComponent verificationSuccessPanelComponent =
        new PublicPanelComponent(
            verificationSuccessSvgIcon,
            verificationSuccessH1,
            messageProperties::getPublicVerificationSubtitle,
            new Component[] {verificationSuccessTextHorizontalLayout},
            verificationLoginButton);
    verificationCardsVerticalLayout.add(verificationSuccessPanelComponent);
  }
}
