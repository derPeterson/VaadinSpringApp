package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.ComponentTextUpdateHelper;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.IsNotAuthenticatedBaseView;
import de.derpeterson.app.security.LoginThrottledException;
import de.derpeterson.app.security.MaintenanceAuthenticationException;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.SessionLimitException;
import de.derpeterson.app.security.SessionManagementService;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AccountSessionsComponent;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.PublicAuthenticationPanelLayout;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import de.derpeterson.app.validation.UserInputRules;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;

@Route(AppRouteConstants.LOGIN_ROUTE)
@PageTitle(AppRouteConstants.LOGIN_PAGE_TITLE)
@AnonymousAllowed
@StyleSheet("custom-theme/cosmos-account.css")
public class LoginView extends IsNotAuthenticatedBaseView<HorizontalLayout>
    implements AfterNavigationObserver {
  private static final String LOGIN_ERROR_UNAVAILABLE = "unavailable";
  private static final String LOGIN_ERROR_SESSIONS = "sessions";
  private static final String LOGIN_ERROR_INVALID = "invalid";
  private static final String LOGIN_ERROR_MAINTENANCE = "maintenance";
  private static final String LOGIN_ERROR_THROTTLED = "throttled";
  private static final String COSMOS_FORM_TEXT_CLASS = "cosmos-form-text";
  private static final String REMEMBER_CHOICE_COOKIE = "login-remember-choice";

  private static final Logger logger = LoggerFactory.getLogger(LoginView.class);

  private final transient MessageProperties messageProperties;
  private final transient SessionManagementService sessionManagementService;
  private final transient HttpServletRequest loginHttpServletRequest;
  private final transient ExternalLoginFlow externalLoginFlow;

  // Anmeldeformular und Links zum Konto
  private PublicPageComponent loginPageComponent;
  private PublicAuthenticationPanelLayout loginAuthenticationPanels;
  private RouterLink createAccountRouterLink = null;
  private EmailField loginEmailField = null;
  private PasswordField loginPasswordField = null;
  private RouterLink forgotPasswordRouterLink = null;
  private RouterLink reactivateAccountRouterLink;
  private Switch rememberMeSwitch = null;
  private Button loginButton = null;
  private H1 loginTitleH1 = null;
  private Span createAccountQuestionSpan = null;

  // Sitzungsverwaltung bei erreichtem Anmeldelimit
  private Div sessionRecoveryDiv;
  private AccountSessionsComponent sessionRecoveryComponent;
  private Button continueLoginButton;
  private Button cancelSessionRecoveryButton;

  public LoginView(
      MessageProperties messageProperties,
      AuthenticationManager authenticationManager,
      SecurityService securityService,
      HttpServletRequest httpServletRequest,
      ConfigService configService,
      SessionManagementService sessionManagementService,
      ExternalLoginFlow externalLoginFlow) {
    super(securityService, httpServletRequest, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.sessionManagementService = sessionManagementService;
    this.loginHttpServletRequest = httpServletRequest;
    this.externalLoginFlow = externalLoginFlow;

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        languageChangeEvent -> applyLoginPageLocale(languageChangeEvent.getNewLocale()));

    getContent().setWidthFull();
    getContent().setPadding(false);
    setSpacing(false);
    getContent().removeAll();

    this.createAccountRouterLink =
        new RouterLink(messageProperties.getLoginCreateAccountLink(), RegistrationView.class);

    this.loginEmailField = new EmailField(messageProperties.getPublicEmailField());
    loginEmailField.setPattern(UserInputRules.EMAIL_REGEX);

    this.loginPasswordField = new PasswordField(messageProperties.getLoginPasswordField());

    this.forgotPasswordRouterLink =
        new RouterLink(messageProperties.getLoginForgotPasswordLink(), ForgotPasswordView.class);
    this.reactivateAccountRouterLink =
        new RouterLink(messageProperties.getAccountReactivateAction(), ReactivateAccountView.class);

    this.rememberMeSwitch = new Switch(messageProperties.getLoginRememberMeCheckbox());
    restoreRememberChoiceFromCookies(httpServletRequest.getCookies());
    rememberMeSwitch.addValueChangeListener(rememberChoiceChangeEvent -> {
      if (rememberChoiceChangeEvent.isFromClient()) {
        saveRememberChoiceCookie(rememberChoiceChangeEvent.getValue());
      }
    });

    this.loginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            _ -> submitPasswordLogin(authenticationManager, securityService));

    loginPageComponent =
        new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.LOGIN);
    VerticalLayout loginContentLayout = loginPageComponent.getCardContainer();

    this.loginTitleH1 = new H1(messageProperties.getLoginTitle());
    loginTitleH1.addClassName("cosmos-headline");

    this.createAccountQuestionSpan = new Span(messageProperties.getLoginCreateAccountQuestion());

    HorizontalLayout createAccountLinkLayout = new HorizontalLayout();
    createAccountLinkLayout.addClassNames("cosmos-intro", COSMOS_FORM_TEXT_CLASS);
    createAccountLinkLayout.setWidthFull();
    createAccountLinkLayout.setPadding(false);
    createAccountLinkLayout.setSpacing(false);
    createAccountLinkLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    createAccountLinkLayout.addClassNames(
        LumoUtility.Gap.SMALL, "cosmos-form-links", COSMOS_FORM_TEXT_CLASS);
    createAccountLinkLayout.add(createAccountQuestionSpan);
    createAccountLinkLayout.add(createAccountRouterLink);

    loginEmailField.setWidthFull();
    loginEmailField.setClearButtonVisible(true);
    loginEmailField.setRequiredIndicatorVisible(true);

    loginPasswordField.setWidthFull();
    loginPasswordField.setClearButtonVisible(true);
    loginPasswordField.setRequiredIndicatorVisible(true);

    loginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loginButton.setWidthFull();

    loginEmailField.addKeyPressListener(Key.ENTER, _ -> loginButton.click());
    loginPasswordField.addKeyPressListener(Key.ENTER, _ -> loginButton.click());

    HorizontalLayout rememberMeLayout = new HorizontalLayout();
    rememberMeLayout.addClassName("cosmos-remember");
    rememberMeLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    rememberMeLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    rememberMeLayout.add(rememberMeSwitch);

    HorizontalLayout forgotPasswordLayout = new HorizontalLayout();
    forgotPasswordLayout.addClassName("cosmos-forgot");
    forgotPasswordLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    forgotPasswordLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    forgotPasswordLayout.getStyle().set("flex-wrap", "wrap");
    forgotPasswordLayout.add(forgotPasswordRouterLink, reactivateAccountRouterLink);

    HorizontalLayout loginOptionsLayout = new HorizontalLayout();
    loginOptionsLayout.setWidthFull();
    loginOptionsLayout.addClassName("cosmos-options");
    loginOptionsLayout.setAlignItems(FlexComponent.Alignment.CENTER);

    loginOptionsLayout.add(rememberMeLayout, forgotPasswordLayout);

    CosmosCardComponent loginPanelComponent =
        new PublicPanelComponent(
            AppIcon.SIGN_IN.create(),
            loginTitleH1,
            messageProperties::getPublicLoginSubtitle,
            new Component[] {loginEmailField, loginPasswordField, loginOptionsLayout},
            createAccountLinkLayout,
            loginButton);

    loginAuthenticationPanels =
        new PublicAuthenticationPanelLayout(
            loginPanelComponent,
            externalLoginFlow,
            httpServletRequest,
            messageProperties,
            () -> rememberMeSwitch.getValue());

    configureSessionRecoveryPanel(configService);
    loginContentLayout.add(loginAuthenticationPanels, sessionRecoveryDiv);

    updateLoginFieldPlaceholders();
    refreshSessionRecoveryTexts();
    add(loginPageComponent);
  }

  private void configureSessionRecoveryPanel(ConfigService configService) {
    sessionRecoveryComponent =
        new AccountSessionsComponent(
            messageProperties, configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER));
    continueLoginButton = new Button();
    continueLoginButton.setEnabled(false);
    sessionRecoveryComponent.configure(
        () -> {
          continueLoginButton.setEnabled(false);
          SessionManagementService.Snapshot recoverySessionSnapshot =
              sessionManagementService.recoveryList(loginHttpServletRequest);
          continueLoginButton.setEnabled(
              recoverySessionSnapshot.active() < recoverySessionSnapshot.limit());
          return recoverySessionSnapshot;
        },
        sessionHandle ->
            sessionManagementService.recoveryRevoke(loginHttpServletRequest, sessionHandle),
        () -> sessionManagementService.recoveryRevokeOthers(loginHttpServletRequest));
    sessionRecoveryComponent.setRestricted(true);
    sessionRecoveryComponent.setFailureAction(this::cancelAndHideSessionRecovery);

    continueLoginButton.setId("session-recovery-continue");
    continueLoginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    continueLoginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    continueLoginButton.addClickListener(_ -> completeSessionRecovery());

    cancelSessionRecoveryButton = new Button();
    cancelSessionRecoveryButton.setId("session-recovery-cancel");
    cancelSessionRecoveryButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelSessionRecoveryButton.addClickListener(_ -> cancelAndHideSessionRecovery());

    Div recoveryActionsDiv = new Div(cancelSessionRecoveryButton, continueLoginButton);
    recoveryActionsDiv.addClassName("public-panel-actions");
    sessionRecoveryComponent.addActionFooter(recoveryActionsDiv);

    sessionRecoveryDiv = new Div(sessionRecoveryComponent);
    sessionRecoveryDiv.setId("session-recovery");
    sessionRecoveryDiv.addClassName("cosmos-account");
    sessionRecoveryDiv.setVisible(false);
  }

  private void completeSessionRecovery() {
    try {
      boolean hasAdministrativeRole =
          sessionManagementService.finishRecovery(
              loginHttpServletRequest, (HttpServletResponse) VaadinResponse.getCurrent());

      navigateAfterSuccessfulLogin(hasAdministrativeRole);
    } catch (SessionLimitException _) {
      sessionRecoveryComponent.reloadSessionSnapshot();
      showLoginFailureNotification(LOGIN_ERROR_SESSIONS);
    } catch (AuthenticationException _) {
      cancelAndHideSessionRecovery();

      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getSessionRecoveryExpired,
              -1,
              NotificationHelper.NotificationType.ERROR);
    } catch (RuntimeException sessionRecoveryFailure) {
      logger.warn("Restricted login completion failed", safeFailure(sessionRecoveryFailure));
      showLoginFailureNotification(LOGIN_ERROR_UNAVAILABLE);
    }
  }

  private void restoreRememberChoiceFromCookies(Cookie[] loginRequestCookies) {
    if (loginRequestCookies != null) {
      for (Cookie requestCookie : loginRequestCookies) {
        if (REMEMBER_CHOICE_COOKIE.equals(requestCookie.getName())) {
          rememberMeSwitch.setValue("true".equals(requestCookie.getValue()));
          break;
        }
      }
    }
  }

  private void applyLoginPageLocale(Locale locale) {
    VaadinSession.getCurrent().setLocale(locale);

    Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
    Optional.ofNullable(createAccountRouterLink)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginCreateAccountLink));
    Optional.ofNullable(loginEmailField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicEmailField));
    Optional.ofNullable(loginPasswordField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginPasswordField));
    Optional.ofNullable(forgotPasswordRouterLink)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginForgotPasswordLink));
    Optional.ofNullable(reactivateAccountRouterLink)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getAccountReactivateAction));
    Optional.ofNullable(rememberMeSwitch)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginRememberMeCheckbox));
    Optional.ofNullable(loginButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseLoginButton));
    Optional.ofNullable(loginTitleH1)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(component, this.messageProperties::getLoginTitle));
    Optional.ofNullable(createAccountQuestionSpan)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginCreateAccountQuestion));

    ComponentTextUpdateHelper.updateComponentTexts(componentTranslationSupplierMap);
    updateLoginFieldPlaceholders();
    refreshSessionRecoveryTexts();
    NotificationHelper.getInstance().updateText();
  }

  private void submitPasswordLogin(
      AuthenticationManager authenticationManager, SecurityService securityService) {
    try {
      Authentication passwordAuthentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginEmailField.getValue(), loginPasswordField.getValue()));

      if (passwordAuthentication.getPrincipal() instanceof UserDetails) {
        securityService.storeVerifiedAuthentication(
            (HttpServletRequest) VaadinRequest.getCurrent(),
            (HttpServletResponse) VaadinResponse.getCurrent(),
            passwordAuthentication,
            rememberMeSwitch.getValue());

      } else {
        logger.warn(
            "⚠️ Principal is not an instance of UserDetails, save and handle of login could not be performed.");
      }

      navigateAfterSuccessfulLogin(hasAdminRole(passwordAuthentication));
    } catch (AuthenticationException authenticationFailure) {
      if (authenticationFailure instanceof SessionLimitException) {
        startSessionRecovery();
        return;
      }

      showLoginFailureNotification(authenticationFailureCode(authenticationFailure));
    }
  }

  private void navigateAfterSuccessfulLogin(boolean hasAdministrativeRole) {
    if (!externalLoginFlow.loginConfirmed(loginHttpServletRequest)) {
      getUI().ifPresent(loginUi -> loginUi.navigate(TwoFactorView.class));
      return;
    }

    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseSuccessTitle,
            messageProperties::getLoginSuccessMessage,
            NotificationHelper.NotificationType.SUCCESS);

    Class<? extends Component> destinationViewClass =
        hasAdministrativeRole ? AdminView.class : HomeView.class;
    getUI().ifPresent(loginUi -> loginUi.navigate(destinationViewClass));
  }

  private void saveRememberChoiceCookie(boolean rememberLoginChoice) {
    // Nur eine UI-Vorliebe. Der Remember-me-Zugang entsteht erst nach erfolgreicher Anmeldung.
    Cookie rememberChoiceCookie =
        new Cookie(REMEMBER_CHOICE_COOKIE, Boolean.toString(rememberLoginChoice));
    rememberChoiceCookie.setPath(
        loginHttpServletRequest.getContextPath().isEmpty()
            ? "/"
            : loginHttpServletRequest.getContextPath());
    rememberChoiceCookie.setHttpOnly(true);
    rememberChoiceCookie.setSecure(loginHttpServletRequest.isSecure());
    rememberChoiceCookie.setAttribute("SameSite", "Lax");
    rememberChoiceCookie.setMaxAge(Math.toIntExact(Duration.ofDays(365).toSeconds()));

    VaadinResponse.getCurrent().addCookie(rememberChoiceCookie);
  }

  private void startSessionRecovery() {
    try {
      sessionManagementService.startRecovery(
          loginHttpServletRequest,
          loginEmailField.getValue(),
          loginPasswordField.getValue(),
          rememberMeSwitch.getValue());

      loginPasswordField.clear();
      showSessionRecoveryPanel();
    } catch (AuthenticationException rejectedAuthentication) {
      showLoginFailureNotification(restrictedLoginFailureCode(rejectedAuthentication));
    } catch (RuntimeException sessionRecoveryFailure) {
      logger.warn("Restricted session management unavailable", safeFailure(sessionRecoveryFailure));
      showLoginFailureNotification(LOGIN_ERROR_UNAVAILABLE);
    }
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    super.beforeEnter(beforeEnterEvent);
    if (beforeEnterEvent.hasForwardTarget()) {
      return;
    }

    if (sessionManagementService.hasRecovery(loginHttpServletRequest)) {
      showSessionRecoveryPanel();
    }
  }

  @Override
  public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
    // Auch Fehler aus externen Anmeldungen benötigen den bereits angehängten Public-Rahmen.
    if (sessionRecoveryDiv.isVisible()) {
      return;
    }

    ExternalProofNotice.showExternalDisconnectNotice(afterNavigationEvent, messageProperties, true);
    List<String> loginErrorParameterValues =
        afterNavigationEvent.getLocation().getQueryParameters().getParameters().get("error");
    if (loginErrorParameterValues != null
        && loginErrorParameterValues.size() == 1
        && Set.of(
                LOGIN_ERROR_MAINTENANCE,
                LOGIN_ERROR_SESSIONS,
                LOGIN_ERROR_THROTTLED,
                LOGIN_ERROR_UNAVAILABLE,
                LOGIN_ERROR_INVALID)
            .contains(loginErrorParameterValues.getFirst())) {
      showLoginFailureNotification(loginErrorParameterValues.getFirst());
    }
  }

  private void showSessionRecoveryPanel() {
    loginPageComponent.showPage(PublicPageComponent.Page.RECOVERY);
    loginAuthenticationPanels.setVisible(false);
    sessionRecoveryDiv.setVisible(true);

    sessionRecoveryComponent.reloadSessionSnapshot();
  }

  private void cancelAndHideSessionRecovery() {
    loginPageComponent.showPage(PublicPageComponent.Page.LOGIN);
    sessionManagementService.cancelRecovery(loginHttpServletRequest);

    sessionRecoveryDiv.setVisible(false);
    continueLoginButton.setEnabled(false);
    loginAuthenticationPanels.setVisible(true);
    loginPasswordField.clear();
  }

  private void refreshSessionRecoveryTexts() {
    if (sessionRecoveryDiv == null) {
      return;
    }

    continueLoginButton.setText(messageProperties.getSessionContinueLogin());
    cancelSessionRecoveryButton.setText(messageProperties.getAdminUsersCancel());
    sessionRecoveryComponent.refreshTexts();
  }

  private String restrictedLoginFailureCode(AuthenticationException failure) {
    if (failure instanceof LoginThrottledException) {
      return LOGIN_ERROR_THROTTLED;
    }
    if (failure instanceof MaintenanceAuthenticationException) {
      return LOGIN_ERROR_MAINTENANCE;
    }
    return LOGIN_ERROR_INVALID;
  }

  private String authenticationFailureCode(AuthenticationException failure) {
    if (failure instanceof MaintenanceAuthenticationException) {
      return LOGIN_ERROR_MAINTENANCE;
    }
    if (failure instanceof SessionLimitException) {
      return LOGIN_ERROR_SESSIONS;
    }
    if (failure instanceof LoginThrottledException) {
      return LOGIN_ERROR_THROTTLED;
    }
    if (failure instanceof AuthenticationServiceException) {
      return LOGIN_ERROR_UNAVAILABLE;
    }
    return LOGIN_ERROR_INVALID;
  }

  private void showLoginFailureNotification(String loginFailureCode) {
    Supplier<String> loginFailureTextSupplier =
        switch (loginFailureCode) {
          case LOGIN_ERROR_MAINTENANCE -> messageProperties::getMaintenanceLoginMessage;
          case LOGIN_ERROR_SESSIONS -> messageProperties::getLoginSessionLimitMessage;
          case LOGIN_ERROR_THROTTLED -> messageProperties::getLoginThrottledMessage;
          case LOGIN_ERROR_UNAVAILABLE -> messageProperties::getLoginUnavailableMessage;
          default -> messageProperties::getLoginFailedMessage;
        };

    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            loginFailureTextSupplier,
            -1,
            NotificationHelper.NotificationType.ERROR);
  }

  private void updateLoginFieldPlaceholders() {
    if (loginEmailField != null) {
      loginEmailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
    if (loginPasswordField != null) {
      loginPasswordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    }
  }
}
