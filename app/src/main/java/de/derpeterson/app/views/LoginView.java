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
import de.derpeterson.app.ui.components.PublicAuthenticationPanels;
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

  private RouterLink createAccountLink = null;
  private EmailField emailField = null;
  private PasswordField passwordField = null;
  private RouterLink forgotPasswordLink = null;
  private Switch rememberMeSwitch = null;
  private Button loginButton = null;
  private H1 loginPageTitle = null;
  private final transient SessionManagementService sessionManagementService;
  private final transient HttpServletRequest currentRequest;
  private final transient ExternalLoginFlow externalLogin;
  private PublicPageComponent pageLayout;
  private Div loginPanels;
  private Div recoveryArea;
  private AccountSessionsComponent recoverySessions;
  private Button continueLogin;
  private Button cancelRecovery;
  private Span createAccountQuestionText = null;

  public LoginView(
      MessageProperties messageProperties,
      AuthenticationManager authenticationManager,
      SecurityService securityService,
      HttpServletRequest request,
      ConfigService configService,
      SessionManagementService sessionManagementService,
      ExternalLoginFlow externalLogin) {
    super(securityService, request, new HorizontalLayout());

    this.messageProperties = messageProperties;
    this.sessionManagementService = sessionManagementService;
    this.currentRequest = request;
    this.externalLogin = externalLogin;

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshLanguage(event.getNewLocale()));

    getContent().setWidthFull();
    getContent().setPadding(false);
    setSpacing(false);
    getContent().removeAll();

    // UI Elements
    this.createAccountLink =
        new RouterLink(messageProperties.getLoginCreateAccountLink(), RegistrationView.class);
    this.emailField = new EmailField(messageProperties.getPublicEmailField());
    emailField.setPattern(UserInputRules.EMAIL_REGEX);
    this.passwordField = new PasswordField(messageProperties.getLoginPasswordField());
    this.forgotPasswordLink =
        new RouterLink(messageProperties.getLoginForgotPasswordLink(), ForgotPasswordView.class);
    this.rememberMeSwitch = new Switch(messageProperties.getLoginRememberMeCheckbox());
    restoreRememberChoice(request.getCookies());
    rememberMeSwitch.addValueChangeListener(event -> {
      if (event.isFromClient()) {
        saveRememberChoice(event.getValue());
      }
    });
    this.loginButton =
        new Button(
            messageProperties.getBaseLoginButton(),
            event -> authenticate(authenticationManager, securityService));

    pageLayout =
        new PublicPageComponent(configService, messageProperties, PublicPageComponent.Page.LOGIN);
    VerticalLayout mainContent = pageLayout.getCardContainer();

    this.loginPageTitle = new H1(messageProperties.getLoginTitle());
    loginPageTitle.addClassName("cosmos-headline");

    this.createAccountQuestionText = new Span(messageProperties.getLoginCreateAccountQuestion());

    HorizontalLayout cardSecondaryTitleLayout = new HorizontalLayout();
    cardSecondaryTitleLayout.addClassNames("cosmos-intro", COSMOS_FORM_TEXT_CLASS);
    cardSecondaryTitleLayout.setWidthFull();
    cardSecondaryTitleLayout.setPadding(false);
    cardSecondaryTitleLayout.setSpacing(false);
    cardSecondaryTitleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    cardSecondaryTitleLayout.addClassNames(
        LumoUtility.Gap.SMALL, "cosmos-form-links", COSMOS_FORM_TEXT_CLASS);
    cardSecondaryTitleLayout.add(createAccountQuestionText);
    cardSecondaryTitleLayout.add(createAccountLink);

    emailField.setWidthFull();
    emailField.setClearButtonVisible(true);
    emailField.setRequiredIndicatorVisible(true);

    passwordField.setWidthFull();
    passwordField.setClearButtonVisible(true);
    passwordField.setRequiredIndicatorVisible(true);

    loginButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loginButton.setWidthFull();

    emailField.addKeyPressListener(Key.ENTER, event -> loginButton.click());
    passwordField.addKeyPressListener(Key.ENTER, event -> loginButton.click());

    HorizontalLayout rememberMeLayout = new HorizontalLayout();
    rememberMeLayout.addClassName("cosmos-remember");
    rememberMeLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    rememberMeLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
    rememberMeLayout.add(rememberMeSwitch);

    HorizontalLayout forgotPasswordLayout = new HorizontalLayout();
    forgotPasswordLayout.addClassName("cosmos-forgot");
    forgotPasswordLayout.setAlignItems(FlexComponent.Alignment.CENTER);
    forgotPasswordLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    forgotPasswordLayout.add(forgotPasswordLink);

    HorizontalLayout secondaryActionLayout = new HorizontalLayout();
    secondaryActionLayout.setWidthFull();
    secondaryActionLayout.addClassName("cosmos-options");
    secondaryActionLayout.setAlignItems(FlexComponent.Alignment.CENTER);

    secondaryActionLayout.add(rememberMeLayout, forgotPasswordLayout);

    CosmosCardComponent cardComponent =
        new PublicPanelComponent(
            AppIcon.SIGN_IN.create(),
            loginPageTitle,
            messageProperties::getPublicLoginSubtitle,
            new Component[] {emailField, passwordField, secondaryActionLayout},
            cardSecondaryTitleLayout,
            loginButton);

    loginPanels =
        new PublicAuthenticationPanels(
            cardComponent,
            externalLogin,
            request,
            messageProperties,
            () -> rememberMeSwitch.getValue());
    recoverySessions =
        new AccountSessionsComponent(
            messageProperties, configService.getInteger(ConfigEntry.MAX_SESSIONS_PER_USER));
    continueLogin = new Button();
    continueLogin.setEnabled(false);
    recoverySessions.configure(
        () -> {
          continueLogin.setEnabled(false);
          SessionManagementService.Snapshot snapshot =
              sessionManagementService.recoveryList(currentRequest);
          continueLogin.setEnabled(snapshot.active() < snapshot.limit());
          return snapshot;
        },
        handle -> sessionManagementService.recoveryRevoke(currentRequest, handle),
        () -> sessionManagementService.recoveryRevokeOthers(currentRequest));
    recoverySessions.setRestricted(true);
    recoverySessions.setFailureAction(this::hideRecovery);
    continueLogin.setId("session-recovery-continue");
    continueLogin.setPrefixComponent(AppIcon.SIGN_IN.create());
    continueLogin.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    continueLogin.addClickListener(event -> {
      try {
        boolean admin =
            sessionManagementService.finishRecovery(
                currentRequest, (HttpServletResponse) VaadinResponse.getCurrent());
        Class<? extends Component> target = loginTarget(admin);
        getUI().ifPresent(ui -> ui.navigate(target));
      } catch (SessionLimitException _) {
        recoverySessions.reloadSessionSnapshot();
        showLoginFailure(LOGIN_ERROR_SESSIONS);
      } catch (AuthenticationException _) {
        hideRecovery();
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getBaseFailedTitle,
                messageProperties::getSessionRecoveryExpired,
                -1,
                NotificationHelper.NotificationType.ERROR);
      } catch (RuntimeException unavailable) {
        logger.warn("Restricted login completion failed", safeFailure(unavailable));
        showLoginFailure(LOGIN_ERROR_UNAVAILABLE);
      }
    });
    cancelRecovery = new Button();
    cancelRecovery.setId("session-recovery-cancel");
    cancelRecovery.setPrefixComponent(AppIcon.CLOSE.create());
    cancelRecovery.addClickListener(event -> hideRecovery());
    Div actions = new Div(cancelRecovery, continueLogin);
    actions.addClassName("public-panel-actions");
    recoverySessions.addActionFooter(actions);
    recoveryArea = new Div(recoverySessions);
    recoveryArea.setId("session-recovery");
    recoveryArea.addClassName("cosmos-account");
    recoveryArea.setVisible(false);
    mainContent.add(loginPanels, recoveryArea);

    updateInputPlaceholders();
    refreshRecoveryTexts();
    add(pageLayout);
  }

  private void restoreRememberChoice(Cookie[] cookies) {
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (REMEMBER_CHOICE_COOKIE.equals(cookie.getName())) {
          rememberMeSwitch.setValue("true".equals(cookie.getValue()));
          break;
        }
      }
    }
  }

  private void refreshLanguage(Locale locale) {
    VaadinSession.getCurrent().setLocale(locale);

    Map<Component, Supplier<String>> componentTranslationSupplierMap = new HashMap<>();
    Optional.ofNullable(createAccountLink)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginCreateAccountLink));
    Optional.ofNullable(emailField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getPublicEmailField));
    Optional.ofNullable(passwordField)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginPasswordField));
    Optional.ofNullable(forgotPasswordLink)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginForgotPasswordLink));
    Optional.ofNullable(rememberMeSwitch)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginRememberMeCheckbox));
    Optional.ofNullable(loginButton)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getBaseLoginButton));
    Optional.ofNullable(loginPageTitle)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(component, this.messageProperties::getLoginTitle));
    Optional.ofNullable(createAccountQuestionText)
        .ifPresent(component ->
            componentTranslationSupplierMap.put(
                component, this.messageProperties::getLoginCreateAccountQuestion));

    ComponentTextUpdateHelper.updateComponentTexts(componentTranslationSupplierMap);
    updateInputPlaceholders();
    refreshRecoveryTexts();
    NotificationHelper.getInstance().updateText();
  }

  private void authenticate(
      AuthenticationManager authenticationManager, SecurityService securityService) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  emailField.getValue(), passwordField.getValue()));

      if (authentication.getPrincipal() instanceof UserDetails) {
        securityService.storeVerifiedAuthentication(
            (HttpServletRequest) VaadinRequest.getCurrent(),
            (HttpServletResponse) VaadinResponse.getCurrent(),
            authentication,
            rememberMeSwitch.getValue());

      } else {
        logger.warn(
            "⚠️ Principal is not an instance of UserDetails, save and handle of login could not be performed.");
      }

      Class<? extends Component> target = loginTarget(hasAdminRole(authentication));
      getUI().ifPresent(ui -> ui.navigate(target));
    } catch (AuthenticationException e) {
      if (e instanceof SessionLimitException) {
        startSessionRecovery();
        return;
      }
      showLoginFailure(authenticationFailureCode(e));
    }
  }

  private Class<? extends Component> loginTarget(boolean admin) {
    if (!externalLogin.loginConfirmed(currentRequest)) {
      return TwoFactorView.class;
    }
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseSuccessTitle,
            messageProperties::getLoginSuccessMessage,
            NotificationHelper.NotificationType.SUCCESS);
    return admin ? AdminView.class : HomeView.class;
  }

  private void saveRememberChoice(boolean remember) {
    // Nur eine UI-Vorliebe. Der Remember-me-Zugang entsteht erst nach erfolgreicher Anmeldung.
    Cookie cookie = new Cookie(REMEMBER_CHOICE_COOKIE, Boolean.toString(remember));
    cookie.setPath(
        currentRequest.getContextPath().isEmpty() ? "/" : currentRequest.getContextPath());
    cookie.setHttpOnly(true);
    cookie.setSecure(currentRequest.isSecure());
    cookie.setAttribute("SameSite", "Lax");
    cookie.setMaxAge(Math.toIntExact(Duration.ofDays(365).toSeconds()));
    VaadinResponse.getCurrent().addCookie(cookie);
  }

  private void startSessionRecovery() {
    try {
      sessionManagementService.startRecovery(
          currentRequest,
          emailField.getValue(),
          passwordField.getValue(),
          rememberMeSwitch.getValue());
      passwordField.clear();
      showRecovery();
    } catch (AuthenticationException rejected) {
      showLoginFailure(restrictedLoginFailureCode(rejected));
    } catch (RuntimeException unavailable) {
      logger.warn("Restricted session management unavailable", safeFailure(unavailable));
      showLoginFailure(LOGIN_ERROR_UNAVAILABLE);
    }
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    super.beforeEnter(event);
    if (event.hasForwardTarget()) {
      return;
    }
    if (sessionManagementService.hasRecovery(currentRequest)) {
      showRecovery();
    }
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    // Auch Fehler aus externen Anmeldungen benötigen den bereits angehängten Public-Rahmen.
    if (recoveryArea.isVisible()) {
      return;
    }
    ExternalProofNotice.showExternalDisconnectNotice(event, messageProperties, true);
    List<String> errors = event.getLocation().getQueryParameters().getParameters().get("error");
    if (errors != null
        && errors.size() == 1
        && Set.of(
                LOGIN_ERROR_MAINTENANCE,
                LOGIN_ERROR_SESSIONS,
                LOGIN_ERROR_THROTTLED,
                LOGIN_ERROR_UNAVAILABLE,
                LOGIN_ERROR_INVALID)
            .contains(errors.getFirst())) {
      showLoginFailure(errors.getFirst());
    }
  }

  private void showRecovery() {
    pageLayout.showPage(PublicPageComponent.Page.RECOVERY);
    loginPanels.setVisible(false);
    recoveryArea.setVisible(true);
    recoverySessions.reloadSessionSnapshot();
  }

  private void hideRecovery() {
    pageLayout.showPage(PublicPageComponent.Page.LOGIN);
    sessionManagementService.cancelRecovery(currentRequest);
    recoveryArea.setVisible(false);
    continueLogin.setEnabled(false);
    loginPanels.setVisible(true);
    passwordField.clear();
  }

  private void refreshRecoveryTexts() {
    if (recoveryArea == null) {
      return;
    }
    continueLogin.setText(messageProperties.getSessionContinueLogin());
    cancelRecovery.setText(messageProperties.getAdminUsersCancel());
    recoverySessions.refreshTexts();
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

  private void showLoginFailure(String reason) {
    Supplier<String> text =
        switch (reason) {
          case LOGIN_ERROR_MAINTENANCE -> messageProperties::getMaintenanceLoginMessage;
          case LOGIN_ERROR_SESSIONS -> messageProperties::getLoginSessionLimitMessage;
          case LOGIN_ERROR_THROTTLED -> messageProperties::getLoginThrottledMessage;
          case LOGIN_ERROR_UNAVAILABLE -> messageProperties::getLoginUnavailableMessage;
          default -> messageProperties::getLoginFailedMessage;
        };
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            text,
            -1,
            NotificationHelper.NotificationType.ERROR);
  }

  private void updateInputPlaceholders() {
    if (emailField != null) {
      emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    }
    if (passwordField != null) {
      passwordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    }
  }
}
