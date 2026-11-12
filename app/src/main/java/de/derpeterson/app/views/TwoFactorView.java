package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosCssClasses.DELETE_BUTTON;

import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.checkbox.SwitchVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.TwoFactorChallengeEntity.Kind;
import de.derpeterson.app.model.TwoFactorChallengeEntity.SetupProof;
import de.derpeterson.app.model.enums.RoleType;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.security.LoginThrottledException;
import de.derpeterson.app.security.SecurityService;
import de.derpeterson.app.security.TwoFactorFlow;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorDocumentsController;
import de.derpeterson.app.security.mfa.TwoFactorEnrollment;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.ui.components.AccountPanelComponent;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.DetailsToggleButton;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.RecoveryCodeDownload;
import de.derpeterson.app.ui.components.TwoFactorCodeFields;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

/** Führt durch die 2FA-Prüfung bei der Anmeldung, im Adminbereich und im eigenen Konto. */
@Route(AppRouteConstants.TWO_FACTOR_ROUTE)
@PermitAll
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-external-login.css")
@StyleSheet("custom-theme/cosmos-mfa.css")
@StyleSheet("custom-theme/cosmos-mfa-methods.css")
@StyleSheet("custom-theme/cosmos-mfa-recovery.css")
@StyleSheet("custom-theme/cosmos-mfa-confirmation.css")
public final class TwoFactorView extends VerticalLayout
    implements BeforeEnterObserver, AfterNavigationObserver, HasDynamicTitle {
  private static final String COPY_MANUAL_KEY_SCRIPT =
      """
      const value = $0.inputElement?.value;
      return value && navigator.clipboard
        ? navigator.clipboard.writeText(value.replaceAll(' ', '').replaceAll('\\n', '')).then(() => true, () => false)
        : false;
      """;
  private static final String INPUT_MODE_ATTRIBUTE = "inputmode";
  private static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";
  private static final String CONFIRMATION_INPUT_ID = "two-factor-confirmation-input";
  private static final String TABPANEL_ROLE = "tabpanel";
  private static final String ARIA_CONTROLS = "aria-controls";
  private static final String ARIA_LABELLED_BY = "aria-labelledby";
  private static final String PANEL_DESCRIPTION_CLASS = "cosmos-panel-description";

  private final transient MessageProperties messageProperties;
  private final transient SecurityService securityService;
  private final transient TwoFactorAuthorization twoFactorAuthorization;
  private final transient TwoFactorEnrollment twoFactorEnrollment;
  private final transient ExternalLoginFlow externalLoginFlow;
  private final transient ExternalIdentityService externalIdentityService;
  private final transient HttpServletRequest httpServletRequest;
  private final transient RecoveryCodeDownload recoveryCodeDownload;

  // Seitenrahmen und Navigation
  private final PublicPageComponent twoFactorPageComponent;
  private final AccountPanelComponent twoFactorPanelComponent =
      new AccountPanelComponent("two-factor-panel", AppIcon.LOCK);
  private final Div stepContentDiv = new Div();
  private final Div additionalStepActionsDiv = new Div();
  private final HorizontalLayout stepNavigationLayout = new HorizontalLayout();
  private final Button cancelTwoFactorButton = new Button();
  private final Button continueTwoFactorButton = new Button();

  // Eingaben für Einrichtung, Anmeldung und Wiederherstellung
  private final PasswordField setupPasswordField = new PasswordField();
  private final TextField additionalProofTextField = new TextField();
  private final OtpField enrollmentOtpField = new OtpField();
  private final OtpField confirmationOtpField = new OtpField();

  // Anzeige und Bestätigung der Ersatzcodes
  private final Switch confirmRecoveryCodesSavedSwitch = new Switch();
  private UUID recoveryOutputUuid;
  private boolean areRecoveryCodesVisible;

  // Status des Ablaufs, der beim Neuaufbau eines Schritts erhalten bleibt
  private TwoFactorEnrollment.Pending pendingTwoFactorEnrollment;
  private TwoFactorFlow twoFactorFlow;
  private boolean isRecoveryMode;
  private boolean isInitialSetupMode;
  private boolean isShowingSetupIntroduction;
  private boolean isManualSetupTabSelected;
  private boolean isRecoveryCodeLoginSelected;

  @Autowired
  public TwoFactorView(
      ConfigService configService,
      MessageProperties messageProperties,
      SecurityService securityService,
      TwoFactorAuthorization twoFactorAuthorization,
      TwoFactorEnrollment twoFactorEnrollment,
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService,
      RecoveryCodeDownload recoveryCodeDownload,
      HttpServletRequest httpServletRequest) {
    this.messageProperties = messageProperties;
    this.securityService = securityService;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.twoFactorEnrollment = twoFactorEnrollment;
    this.externalLoginFlow = externalLoginFlow;
    this.externalIdentityService = externalIdentityService;
    this.httpServletRequest = httpServletRequest;
    this.recoveryCodeDownload = recoveryCodeDownload;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("two-factor-view");

    twoFactorPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.TWO_FACTOR);
    stepContentDiv.addClassName("two-factor-content");
    additionalStepActionsDiv.addClassName("two-factor-extra-actions");
    twoFactorPanelComponent.getBody().add(stepContentDiv, additionalStepActionsDiv);

    configureTwoFactorStepInputs();

    confirmRecoveryCodesSavedSwitch.setId("two-factor-codes-saved");
    confirmRecoveryCodesSavedSwitch.addValueChangeListener(
        _ -> updateRecoveryBackupConfirmationButton());

    configureTwoFactorFooter();

    twoFactorPageComponent.getCardContainer().add(twoFactorPanelComponent);
    add(twoFactorPageComponent);

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, _ -> renderCurrentTwoFactorStep());
  }

  private void configureTwoFactorStepInputs() {
    setupPasswordField.setId("two-factor-password");
    setupPasswordField.setWidthFull();
    setupPasswordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    setupPasswordField.setManualValidation(true);
    setupPasswordField.setRequiredIndicatorVisible(true);

    additionalProofTextField.setId("two-factor-code");
    additionalProofTextField.setWidthFull();
    additionalProofTextField.setManualValidation(true);
    additionalProofTextField.setRequiredIndicatorVisible(true);
    additionalProofTextField.getElement().setAttribute(AUTOCOMPLETE_ATTRIBUTE, "one-time-code");

    configureTwoFactorOtpField(enrollmentOtpField, "two-factor-enrollment-code");
    configureTwoFactorOtpField(confirmationOtpField, "two-factor-code");
  }

  private void configureTwoFactorOtpField(OtpField twoFactorOtpField, String otpFieldId) {
    twoFactorOtpField.setId(otpFieldId);
    TwoFactorCodeFields.configureOtpField(twoFactorOtpField, httpServletRequest.getContextPath());

    twoFactorOtpField.addCompleteListener(otpCompleteEvent -> {
      if (continueTwoFactorButton.isVisible()
          && continueTwoFactorButton.isEnabled()
          && TwoFactorCodeFields.isCurrentCompleteOtp(twoFactorOtpField, otpCompleteEvent)) {
        submitTwoFactorForm();
      }
    });
  }

  private TextField configureRecoveryCodeTextField() {
    additionalProofTextField.setLabel(messageProperties.getTwoFactorRecoveryCodeLabel());
    additionalProofTextField.getElement().removeAttribute(INPUT_MODE_ATTRIBUTE);
    TwoFactorCodeFields.configureRecoveryCodeField(additionalProofTextField);
    return additionalProofTextField;
  }

  private TextField configureAdditionalProofTextField(
      String proofLabelText, int maximumProofLength) {
    additionalProofTextField.removeClassName("two-factor-recovery-input");
    additionalProofTextField.setLabel(proofLabelText);
    additionalProofTextField.setPlaceholder(messageProperties.getTwoFactorCodePlaceholder());
    additionalProofTextField.setMaxLength(maximumProofLength);
    additionalProofTextField.getElement().setAttribute(AUTOCOMPLETE_ATTRIBUTE, "one-time-code");
    additionalProofTextField.getElement().removeAttribute(INPUT_MODE_ATTRIBUTE);
    return additionalProofTextField;
  }

  private void configureTwoFactorFooter() {
    cancelTwoFactorButton.setId("two-factor-cancel");
    cancelTwoFactorButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelTwoFactorButton.addClickListener(_ -> cancelTwoFactorFlow());

    continueTwoFactorButton.setId("two-factor-submit");
    continueTwoFactorButton.setPrefixComponent(AppIcon.CHECK.create());
    continueTwoFactorButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    continueTwoFactorButton.addClickListener(_ -> submitTwoFactorForm());

    HorizontalLayout stepActionsLayout =
        new HorizontalLayout(cancelTwoFactorButton, continueTwoFactorButton);
    stepActionsLayout.setPadding(false);
    stepActionsLayout.setSpacing(false);
    stepActionsLayout.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(stepActionsLayout);

    stepNavigationLayout.setPadding(false);
    stepNavigationLayout.setSpacing(false);
    stepNavigationLayout.addClassName("two-factor-footer-navigation");

    HorizontalLayout twoFactorFooterLayout =
        new HorizontalLayout(stepNavigationLayout, stepActionsLayout);
    twoFactorFooterLayout.setPadding(false);
    twoFactorFooterLayout.setSpacing(false);
    twoFactorFooterLayout.setWidthFull();
    twoFactorFooterLayout.setAlignItems(Alignment.CENTER);
    twoFactorFooterLayout.addClassName("two-factor-footer");
    twoFactorPanelComponent.addActionFooter(twoFactorFooterLayout);
  }

  // Routeneinstieg
  @Override
  public void beforeEnter(BeforeEnterEvent navigationEvent) {
    if (securityService.getAuthenticatedUser(httpServletRequest).isEmpty()) {
      navigationEvent.forwardTo(LoginView.class);
      return;
    }

    twoFactorFlow = TwoFactorFlow.current(httpServletRequest);
    if (twoFactorFlow == null) {
      twoFactorFlow = resolveRequestedTwoFactorFlow(navigationEvent);
      twoFactorFlow.enter(httpServletRequest);
    }

    if (twoFactorFlow.help()) {
      navigationEvent.forwardTo(RecoveryHelpView.class);
      return;
    }

    twoFactorPageComponent.setAccountSecurity(
        twoFactorFlow.account(), securityService.getCurrentUser(httpServletRequest).orElse(null));
    isRecoveryMode = twoFactorFlow.recovery();
    renderCurrentTwoFactorStep();
  }

  private TwoFactorFlow resolveRequestedTwoFactorFlow(BeforeEnterEvent beforeEnterEvent) {
    Map<String, List<String>> twoFactorQueryParameters =
        beforeEnterEvent.getLocation().getQueryParameters().getParameters();
    boolean isSessionConfirmed = twoFactorAuthorization.sessionConfirmed(httpServletRequest);

    if (twoFactorQueryParameters.containsKey("recover")) {
      return isSessionConfirmed ? TwoFactorFlow.ACCOUNT_RECOVERY : TwoFactorFlow.RECOVERY;
    }
    if (twoFactorQueryParameters.containsKey("administration")) {
      return TwoFactorFlow.ADMINISTRATION;
    }
    return isSessionConfirmed && twoFactorQueryParameters.containsKey("setup")
        ? TwoFactorFlow.SETUP
        : TwoFactorFlow.LOGIN;
  }

  public boolean matchesActiveTwoFactorFlow(TwoFactorFlow activeTwoFactorFlow) {
    return twoFactorFlow == activeTwoFactorFlow;
  }

  @Override
  public void afterNavigation(AfterNavigationEvent navigationEvent) {
    ExternalProofNotice.showExternalProofFailure(navigationEvent, messageProperties);
  }

  @Override
  public String getPageTitle() {
    return messageProperties.getTwoFactorPageTitle();
  }

  // Aufbau des aktiven Schritts
  private void renderCurrentTwoFactorStep() {
    if (securityService.getAuthenticatedUser(httpServletRequest).isEmpty()) {
      return;
    }

    clearPreviousTwoFactorStep();

    pendingTwoFactorEnrollment = twoFactorEnrollment.pending(httpServletRequest);
    resetTwoFactorStepPresentation();

    // Der Wiederherstellungspfad gilt auch nach dem Wechsel in die neue Einrichtung.
    isRecoveryMode =
        isRecoveryMode
            || pendingTwoFactorEnrollment != null
                && pendingTwoFactorEnrollment.proof() == SetupProof.RECOVERY_CODE;
    TwoFactorAuthorization.State twoFactorAuthorizationState =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(httpServletRequest));
    isInitialSetupMode =
        pendingTwoFactorEnrollment == null && !twoFactorAuthorizationState.enrolled();
    boolean shouldStartRecovery =
        pendingTwoFactorEnrollment == null
            && twoFactorAuthorizationState.enrolled()
            && isRecoveryMode;

    twoFactorPanelComponent.setIcon(
        (shouldStartRecovery ? AppIcon.LOCK_RESET : AppIcon.LOCK).create());
    twoFactorPanelComponent.setHeading(
        isInitialSetupMode
            ? messageProperties.getTwoFactorSetupTitle()
            : messageProperties.getTwoFactorConfirmTitle(),
        messageProperties.getTwoFactorPageHint(),
        "");

    cancelTwoFactorButton.setText(
        twoFactorFlow.account() && twoFactorAuthorization.sessionConfirmed(httpServletRequest)
            ? messageProperties.getTwoFactorBack()
            : messageProperties.getBaseLogoutButton());
    if (!twoFactorFlow.account() || !twoFactorAuthorization.sessionConfirmed(httpServletRequest)) {
      cancelTwoFactorButton.addClassName(DELETE_BUTTON);
      cancelTwoFactorButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    }
    continueTwoFactorButton.setEnabled(true);
    cancelTwoFactorButton.setEnabled(true);
    cancelTwoFactorButton.setVisible(true);

    if (pendingTwoFactorEnrollment != null) {
      showPendingTwoFactorStep();
    } else if (shouldStartRecovery) {
      showAuthenticatorRecoveryForm();
    } else if (twoFactorAuthorizationState.enrolled()) {
      showExistingFactorConfirmation();
    } else {
      showAuthenticatorSetupEntry();
    }
  }

  private void showPendingTwoFactorStep() {
    continueTwoFactorButton.setVisible(true);

    switch (pendingTwoFactorEnrollment.kind()) {
      case GOOGLE_EMAIL -> {
        addTwoFactorStepHint(messageProperties.getTwoFactorEmailCodeHint());
        stepContentDiv.add(
            configureAdditionalProofTextField(messageProperties.getTwoFactorEmailCodeLabel(), 8));
        additionalProofTextField.getElement().setAttribute(INPUT_MODE_ATTRIBUTE, "numeric");

        continueTwoFactorButton.setText(messageProperties.getTwoFactorConfirm());
      }
      case ENROLLMENT -> showAuthenticatorEnrollmentForm();
      case RECOVERY_OUTPUT -> showRecoveryCodeBackup();
      default -> throw new IllegalStateException("Unexpected setup phase");
    }
  }

  private void clearPreviousTwoFactorStep() {
    stepContentDiv.removeAll();
    additionalStepActionsDiv.removeAll();
    stepNavigationLayout.removeAll();

    setupPasswordField.clear();
    additionalProofTextField.clear();
    enrollmentOtpField.clear();
    confirmationOtpField.clear();

    setupPasswordField.setInvalid(false);
    additionalProofTextField.setInvalid(false);
    enrollmentOtpField.setInvalid(false);
    confirmationOtpField.setInvalid(false);
  }

  private void resetTwoFactorStepPresentation() {
    isShowingSetupIntroduction = false;

    twoFactorPanelComponent.removeClassName("two-factor-introduction");
    twoFactorPanelComponent.removeClassName("two-factor-setup");
    twoFactorPanelComponent.removeClassName("two-factor-recovery");
    twoFactorPanelComponent.removeClassName("two-factor-confirmation");
    twoFactorPanelComponent.removeClassName("two-factor-restore");

    cancelTwoFactorButton.removeClassName(DELETE_BUTTON);
    cancelTwoFactorButton.setPrefixComponent(AppIcon.CLOSE.create());
    continueTwoFactorButton.setPrefixComponent(AppIcon.CHECK.create());
  }

  private void addTwoFactorStepHint(String hintText) {
    Span hintSpan = new Span(hintText);
    hintSpan.addClassName(PANEL_DESCRIPTION_CLASS);
    stepContentDiv.add(hintSpan);
  }

  // Anmeldung und Admin-Zugriff bestätigen
  private void showExistingFactorConfirmation() {
    if (twoFactorFlow.administration()
        ? twoFactorAuthorization.administrationConfirmed(httpServletRequest)
        : twoFactorAuthorization.sessionConfirmed(httpServletRequest)) {
      finishTwoFactorFlow();
      return;
    }

    showLoginMethodTabs();
    addLoginRecoveryHelpActions();

    continueTwoFactorButton.setText(
        twoFactorFlow.administration()
            ? messageProperties.getTwoFactorConfirm()
            : messageProperties.getBaseLoginButton());
    if (!twoFactorFlow.administration()) {
      continueTwoFactorButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    }
  }

  private void addLoginRecoveryHelpActions() {
    Button recoverAuthenticatorButton =
        new Button(messageProperties.getTwoFactorRecoverAction(), AppIcon.OPEN_SECTION.create());
    recoverAuthenticatorButton.setIconAfterText(true);
    recoverAuthenticatorButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    recoverAuthenticatorButton.setId("two-factor-recover");
    recoverAuthenticatorButton.addClickListener(_ ->
        navigateToTwoFactorFlow(
            twoFactorFlow.administration()
                ? TwoFactorFlow.ADMIN_RECOVERY
                : TwoFactorFlow.RECOVERY));

    Button recoveryHelpButton =
        new Button(
            messageProperties.getSupportHelpTitle(),
            _ ->
                navigateToTwoFactorFlow(
                    twoFactorFlow.administration()
                        ? TwoFactorFlow.ADMIN_HELP
                        : TwoFactorFlow.LOGIN_HELP));
    recoveryHelpButton.setId("two-factor-help");
    recoveryHelpButton.setIcon(AppIcon.OPEN_SECTION.create());
    recoveryHelpButton.setIconAfterText(true);
    recoveryHelpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

    Span loginHelpTopicSpan =
        new Span(
            AppIcon.QUESTION_CIRCLE.create(),
            new Span(messageProperties.getTwoFactorLoginHelpTitle()));
    loginHelpTopicSpan.addClassName("two-factor-login-help-topic");
    VerticalLayout loginHelpLinksLayout =
        new VerticalLayout(
            buildLoginHelpRowLayout(
                messageProperties.getTwoFactorLoginRestorePrompt(), recoverAuthenticatorButton),
            buildLoginHelpRowLayout(
                messageProperties.getSupportHelpSubtitle(), recoveryHelpButton));
    loginHelpLinksLayout.setPadding(false);
    loginHelpLinksLayout.setSpacing(false);
    loginHelpLinksLayout.addClassName("two-factor-login-help-links");

    Details loginHelpDetails = new Details("", loginHelpLinksLayout);
    loginHelpDetails.setId("two-factor-login-help");
    loginHelpDetails.setWidthFull();
    Button loginHelpToggleButton =
        buildDetailsToggleButton(
            loginHelpDetails,
            "two-factor-login-help-toggle",
            messageProperties.getTwoFactorLoginHelpTitle());
    loginHelpToggleButton.setPrefixComponent(loginHelpTopicSpan);
    loginHelpToggleButton.addClassName("two-factor-login-help-toggle");

    additionalStepActionsDiv.add(loginHelpToggleButton, loginHelpDetails);
  }

  private HorizontalLayout buildLoginHelpRowLayout(
      String helpQuestionText, Button helpActionButton) {
    HorizontalLayout loginHelpRowLayout =
        new HorizontalLayout(new Span(helpQuestionText), helpActionButton);
    loginHelpRowLayout.setAlignItems(Alignment.CENTER);
    loginHelpRowLayout.addClassName("two-factor-help-row");
    return loginHelpRowLayout;
  }

  private Button buildDetailsToggleButton(
      Details toggleTargetDetails, String toggleButtonId, String detailsTopicText) {
    Button detailsToggleButton =
        new DetailsToggleButton(messageProperties, toggleTargetDetails, detailsTopicText);
    detailsToggleButton.setId(toggleButtonId);
    return detailsToggleButton;
  }

  private void showLoginMethodTabs() {
    twoFactorPanelComponent.addClassName("two-factor-confirmation");
    twoFactorPanelComponent.setIcon(AppIcon.SHIELD_KEYHOLE.create());
    twoFactorPanelComponent.setHeading(
        messageProperties.getTwoFactorConfirmTitle(),
        messageProperties.getTwoFactorConfirmSubtitle(),
        "");

    Tab authenticatorCodeTab =
        new Tab(
            AppIcon.DEVICES.create(), new Span(messageProperties.getTwoFactorConfirmationAppTab()));
    Tab recoveryCodeTab =
        new Tab(AppIcon.KEY.create(), new Span(messageProperties.getTwoFactorRecoveryCodeLabel()));
    authenticatorCodeTab.setId("two-factor-authenticator-tab");
    recoveryCodeTab.setId("two-factor-recovery-tab");

    VerticalLayout loginCodeInputLayout = new VerticalLayout();
    loginCodeInputLayout.setPadding(false);
    loginCodeInputLayout.setSpacing(false);
    loginCodeInputLayout.setWidthFull();
    loginCodeInputLayout.setId(CONFIRMATION_INPUT_ID);
    loginCodeInputLayout.addClassName(CONFIRMATION_INPUT_ID);
    loginCodeInputLayout.getElement().setAttribute("role", TABPANEL_ROLE);
    for (Tab loginMethodTab : List.of(authenticatorCodeTab, recoveryCodeTab)) {
      loginMethodTab.getElement().setAttribute(ARIA_CONTROLS, CONFIRMATION_INPUT_ID);
    }

    Tabs loginMethodTabs = new Tabs(authenticatorCodeTab, recoveryCodeTab);
    loginMethodTabs.setWidthFull();
    loginMethodTabs.addClassName("two-factor-method-tabs");
    loginMethodTabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
    loginMethodTabs.setSelectedTab(
        isRecoveryCodeLoginSelected ? recoveryCodeTab : authenticatorCodeTab);
    loginMethodTabs.addSelectedChangeListener(loginMethodChangeEvent -> {
      isRecoveryCodeLoginSelected = loginMethodChangeEvent.getSelectedTab() == recoveryCodeTab;
      showSelectedLoginCodeInput(loginCodeInputLayout);
    });

    stepContentDiv.add(loginMethodTabs, loginCodeInputLayout);
    showSelectedLoginCodeInput(loginCodeInputLayout);
  }

  private void showSelectedLoginCodeInput(VerticalLayout loginCodeInputLayout) {
    loginCodeInputLayout.removeAll();
    additionalProofTextField.clear();
    confirmationOtpField.clear();
    additionalProofTextField.setInvalid(false);
    confirmationOtpField.setInvalid(false);

    loginCodeInputLayout
        .getElement()
        .setAttribute(
            ARIA_LABELLED_BY,
            isRecoveryCodeLoginSelected
                ? "two-factor-recovery-tab"
                : "two-factor-authenticator-tab");

    String loginCodeHintText =
        isRecoveryCodeLoginSelected
            ? messageProperties.getTwoFactorLoginRecoveryHint()
            : messageProperties.getTwoFactorLoginHint();
    Span loginCodeHintSpan =
        new Span(
            twoFactorFlow.administration()
                ? messageProperties.getTwoFactorActionHint()
                : loginCodeHintText);
    loginCodeHintSpan.addClassName(PANEL_DESCRIPTION_CLASS);
    loginCodeInputLayout.add(loginCodeHintSpan);

    if (isRecoveryCodeLoginSelected) {
      loginCodeInputLayout.add(configureRecoveryCodeTextField());
    } else {
      confirmationOtpField.setLabel(messageProperties.getTwoFactorSetupCodeLabel());
      loginCodeInputLayout.add(confirmationOtpField);
    }
  }

  // Wiederherstellung mit einem Ersatzcode
  private void showAuthenticatorRecoveryForm() {
    twoFactorPanelComponent.addClassName("two-factor-restore");
    twoFactorPanelComponent.setHeading(
        messageProperties.getTwoFactorRecoverAction(),
        messageProperties.getTwoFactorRecoverSubtitle(),
        "");

    if (!twoFactorEnrollment.available()) {
      addTwoFactorStepHint(messageProperties.getTwoFactorUnavailable());
    }
    stepContentDiv.add(configureRecoveryCodeTextField());

    continueTwoFactorButton.setVisible(true);
    continueTwoFactorButton.setEnabled(twoFactorEnrollment.available());
    continueTwoFactorButton.setText(messageProperties.getTwoFactorRecoveryContinue());

    HorizontalLayout restoreNoticeLayout =
        new HorizontalLayout(
            AppIcon.INFO_CIRCLE.create(),
            new Div(
                new Paragraph(messageProperties.getTwoFactorRecoveryPreviousAppHint()),
                new Paragraph(messageProperties.getTwoFactorRecoveryConsumptionHint())));
    restoreNoticeLayout.setPadding(false);
    restoreNoticeLayout.setSpacing(false);
    restoreNoticeLayout.setAlignItems(Alignment.CENTER);
    restoreNoticeLayout.addClassName("two-factor-restore-notice");
    stepContentDiv.add(restoreNoticeLayout);

    Button returnToCodeButton =
        new Button(messageProperties.getTwoFactorReturnToCode(), AppIcon.ANGLE_LEFT.create());
    returnToCodeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    returnToCodeButton.setId("two-factor-recovery-back");
    returnToCodeButton.addClickListener(_ ->
        navigateToTwoFactorFlow(
            twoFactorFlow.administration() ? TwoFactorFlow.ADMINISTRATION : TwoFactorFlow.LOGIN));
    stepNavigationLayout.add(returnToCodeButton);
  }

  // Start der Einrichtung und Auswahl des Identitätsnachweises
  private void showAuthenticatorSetupEntry() {
    if (!twoFactorFlow.account()
        && !Boolean.TRUE.equals(
            httpServletRequest.getSession().getAttribute(TwoFactorFlow.INTRO_ACCEPTED))
        && (twoFactorAuthorization.requires(httpServletRequest, TwoFactorAction.LOGIN)
            || twoFactorAuthorization.requires(
                httpServletRequest, TwoFactorAction.ADMINISTRATION))) {
      showRequiredSetupIntroduction();
      return;
    }

    twoFactorPanelComponent.setHeading(
        messageProperties.getTwoFactorSetupTitle(),
        messageProperties.getTwoFactorSetupEntrySubtitle(),
        "");

    boolean hasPortalPassword = externalLoginFlow.owner(httpServletRequest).getPassword() != null;
    String setupIdentityHintText =
        hasPortalPassword
            ? messageProperties.getExternalPasswordProofHint()
            : messageProperties.getTwoFactorSetupHint();
    addTwoFactorStepHint(
        twoFactorEnrollment.available()
            ? setupIdentityHintText
            : messageProperties.getTwoFactorUnavailable());

    if (hasPortalPassword) {
      setupPasswordField.setLabel(messageProperties.getAccountPasswordDialogCurrentPassword());
      setupPasswordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
      stepContentDiv.add(setupPasswordField);
    }
    continueTwoFactorButton.setVisible(hasPortalPassword);
    continueTwoFactorButton.setEnabled(twoFactorEnrollment.available());
    continueTwoFactorButton.setText(messageProperties.getTwoFactorStartSetup());

    for (String providerKey :
        externalIdentityService.connected(twoFactorAuthorization.owner(httpServletRequest).id())) {
      if (!isEnrollmentProviderAvailable(providerKey)) {
        continue;
      }
      additionalStepActionsDiv.add(buildEnrollmentProviderProofButton(providerKey));
    }

    if (!hasPortalPassword && additionalStepActionsDiv.getComponentCount() == 0) {
      addTwoFactorStepHint(messageProperties.getExternalProofUnavailable());
    }
  }

  private Button buildEnrollmentProviderProofButton(String providerKey) {
    boolean isGoogleProvider = ExternalProviderSettings.GOOGLE.equals(providerKey);
    String providerBrandName =
        isGoogleProvider
            ? ExternalProviderSettings.GOOGLE_NAME
            : ExternalProviderSettings.MICROSOFT_NAME;

    Button providerProofButton =
        new Button(messageProperties.getExternalProofWith(providerBrandName));
    providerProofButton.setId("two-factor-confirm-" + providerKey);
    Image providerLogoImage =
        new Image("icons/external-login/" + providerKey + (isGoogleProvider ? ".png" : ".svg"), "");
    providerLogoImage.addClassName("external-proof-provider-mark");
    providerProofButton.setPrefixComponent(providerLogoImage);
    providerProofButton.setEnabled(twoFactorEnrollment.available());

    providerProofButton.addClickListener(_ -> {
      try {
        UI.getCurrent()
            .getPage()
            .setLocation(
                externalLoginFlow.beginProof(
                    httpServletRequest,
                    providerKey,
                    ExternalLoginFlow.Purpose.ENROLL_FACTOR,
                    VaadinSession.getCurrent().getLocale()));
      } catch (RuntimeException providerProofFailure) {
        showTwoFactorFailureNotification(providerProofFailure);
      }
    });
    return providerProofButton;
  }

  private boolean isEnrollmentProviderAvailable(String providerKey) {
    if (!externalLoginFlow.providers().contains(providerKey)) {
      return false;
    }

    TwoFactorAuthorization.State twoFactorAuthorizationState =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(httpServletRequest));
    return !ExternalProviderSettings.GOOGLE.equals(providerKey)
        || !twoFactorAuthorizationState.previouslyEnrolled()
        || twoFactorAuthorizationState.operatorEnrollment();
  }

  private void showRequiredSetupIntroduction() {
    isShowingSetupIntroduction = true;
    twoFactorPanelComponent.addClassName("two-factor-introduction");

    Image setupIntroductionImage = new Image("images/two-factor-locks.svg", "");
    setupIntroductionImage.addClassName("two-factor-introduction-art");
    setupIntroductionImage.getElement().setAttribute("aria-hidden", "true");
    H2 setupIntroductionHeadingH2 = new H2(messageProperties.getTwoFactorRequiredTitle());
    setupIntroductionHeadingH2.setId("two-factor-introduction-title");
    setupIntroductionHeadingH2.addClassName("two-factor-introduction-title");
    stepContentDiv.add(setupIntroductionImage, setupIntroductionHeadingH2);

    addTwoFactorStepHint(messageProperties.getTwoFactorRequiredHint());
    if (!twoFactorEnrollment.available()) {
      addTwoFactorStepHint(messageProperties.getTwoFactorUnavailable());
    }

    cancelTwoFactorButton.setText(messageProperties.getBaseLogoutButton());
    cancelTwoFactorButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    cancelTwoFactorButton.addClassName(DELETE_BUTTON);

    continueTwoFactorButton.setVisible(true);
    continueTwoFactorButton.setEnabled(twoFactorEnrollment.available());
    continueTwoFactorButton.setText(messageProperties.getTwoFactorBeginSetup());
    continueTwoFactorButton.setPrefixComponent(AppIcon.SHIELD_CHECK.create());
  }

  // Authenticator per QR-Code oder manuellem Schlüssel einrichten
  private void showAuthenticatorEnrollmentForm() {
    twoFactorPanelComponent.addClassName("two-factor-setup");
    twoFactorPanelComponent.setIcon(AppIcon.SHIELD_KEYHOLE.create());
    twoFactorPanelComponent.setHeading(
        messageProperties.getTwoFactorSetupTitle(),
        messageProperties.getTwoFactorSetupSubtitle(),
        "");

    Div setupMethodsDiv = buildAuthenticatorSetupMethodsDiv();
    VerticalLayout setupInstructionsLayout = buildAuthenticatorSetupInstructionsLayout();
    Div setupLayoutDiv = new Div(setupMethodsDiv, setupInstructionsLayout);
    setupLayoutDiv.addClassName("two-factor-setup-layout");
    stepContentDiv.add(setupLayoutDiv);

    continueTwoFactorButton.setText(messageProperties.getTwoFactorEnable());
  }

  private Div buildAuthenticatorSetupMethodsDiv() {
    Image setupQrImage =
        new Image(
            buildTwoFactorDocumentPath("setup/" + pendingTwoFactorEnrollment.id() + "/qr.png"),
            messageProperties.getTwoFactorQrAlt());
    setupQrImage.setId("two-factor-qr");
    setupQrImage.addClassName("two-factor-qr");
    Span scanHintSpan = new Span(messageProperties.getTwoFactorScanHint());
    scanHintSpan.addClassNames(PANEL_DESCRIPTION_CLASS, "two-factor-qr-hint");
    VerticalLayout qrSetupLayout = new VerticalLayout(setupQrImage, scanHintSpan);
    qrSetupLayout.setPadding(false);
    qrSetupLayout.setSpacing(false);
    qrSetupLayout.addClassName("two-factor-qr-content");

    TextArea manualKeyTextArea = new TextArea();
    manualKeyTextArea.setId("two-factor-manual-key");
    manualKeyTextArea.setReadOnly(true);
    manualKeyTextArea.setWidthFull();
    manualKeyTextArea.setMaxWidth("100%");
    manualKeyTextArea.setMinRows(2);
    manualKeyTextArea.setMaxRows(2);
    manualKeyTextArea.setAutocomplete(Autocomplete.OFF);
    manualKeyTextArea.addClassName("two-factor-manual-input");
    manualKeyTextArea.setLabel(messageProperties.getTwoFactorManualLabel());
    Span manualKeyHintSpan = new Span(messageProperties.getTwoFactorManualHint());
    manualKeyHintSpan.addClassNames(PANEL_DESCRIPTION_CLASS, "two-factor-manual-hint");

    Button copyManualKeyButton = new Button(AppIcon.CLIPBOARD_COPY.create());
    copyManualKeyButton.setId("two-factor-copy-key");
    copyManualKeyButton.setAriaLabel(messageProperties.getProviderSettingsCopy());
    copyManualKeyButton.setTooltipText(messageProperties.getProviderSettingsCopy());
    copyManualKeyButton.setEnabled(false);
    copyManualKeyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    copyManualKeyButton.addClickListener(_ ->
        copyManualKeyButton
            .getElement()
            .executeJs(COPY_MANUAL_KEY_SCRIPT, manualKeyTextArea.getElement())
            .then(
                Boolean.class,
                wasManualKeyCopied ->
                    NotificationHelper.getInstance()
                        .showNotification(
                            Boolean.TRUE.equals(wasManualKeyCopied)
                                ? messageProperties::getTwoFactorManualCopied
                                : messageProperties::getTwoFactorManualCopyFailed,
                            Boolean.TRUE.equals(wasManualKeyCopied)
                                ? NotificationHelper.NotificationType.SUCCESS
                                : NotificationHelper.NotificationType.ERROR)));
    manualKeyTextArea.setSuffixComponent(copyManualKeyButton);

    VerticalLayout manualKeyLayout = new VerticalLayout(manualKeyHintSpan, manualKeyTextArea);
    manualKeyLayout.setPadding(false);
    manualKeyLayout.setSpacing(false);
    manualKeyLayout.addClassName("two-factor-manual-content");

    manualKeyTextArea.addAttachListener(_ -> {
      if (isManualSetupTabSelected) {
        loadManualSetupKeyIntoInput(manualKeyTextArea, copyManualKeyButton);
      }
    });

    Tab qrSetupTab =
        new Tab(AppIcon.QR_CODE.create(), new Span(messageProperties.getTwoFactorQrTab()));
    Tab manualKeyTab =
        new Tab(AppIcon.KEY.create(), new Span(messageProperties.getTwoFactorManualTab()));
    qrSetupTab.setId("two-factor-qr-tab");
    manualKeyTab.setId("two-factor-manual-tab");

    qrSetupLayout.setId("two-factor-qr-pane");
    manualKeyLayout.setId("two-factor-manual-pane");
    qrSetupTab.getElement().setAttribute(ARIA_CONTROLS, "two-factor-qr-pane");
    manualKeyTab.getElement().setAttribute(ARIA_CONTROLS, "two-factor-manual-pane");
    qrSetupLayout.getElement().setAttribute("role", TABPANEL_ROLE);
    qrSetupLayout.getElement().setAttribute(ARIA_LABELLED_BY, "two-factor-qr-tab");
    manualKeyLayout.getElement().setAttribute("role", TABPANEL_ROLE);
    manualKeyLayout.getElement().setAttribute(ARIA_LABELLED_BY, "two-factor-manual-tab");

    manualKeyLayout.setVisible(isManualSetupTabSelected);
    qrSetupLayout.setVisible(!isManualSetupTabSelected);

    Tabs setupMethodTabs = new Tabs(qrSetupTab, manualKeyTab);
    setupMethodTabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
    setupMethodTabs.addClassName("two-factor-method-tabs");
    setupMethodTabs.setWidthFull();
    setupMethodTabs.setSelectedTab(isManualSetupTabSelected ? manualKeyTab : qrSetupTab);
    setupMethodTabs.addSelectedChangeListener(setupMethodChangeEvent -> {
      boolean isManualKeyTab = setupMethodChangeEvent.getSelectedTab() == manualKeyTab;
      isManualSetupTabSelected = isManualKeyTab;
      qrSetupLayout.setVisible(!isManualKeyTab);
      manualKeyLayout.setVisible(isManualKeyTab);
      if (isManualKeyTab) {
        loadManualSetupKeyIntoInput(manualKeyTextArea, copyManualKeyButton);
      }
    });

    Div setupMethodsDiv = new Div(setupMethodTabs, qrSetupLayout, manualKeyLayout);
    setupMethodsDiv.setId("two-factor-setup-methods");
    setupMethodsDiv.addClassName("two-factor-methods");
    setupMethodsDiv.setWidthFull();

    return setupMethodsDiv;
  }

  private VerticalLayout buildAuthenticatorSetupInstructionsLayout() {
    HorizontalLayout authenticatorAppLinksLayout =
        new HorizontalLayout(
            buildAuthenticatorAppLink(
                "https://support.google.com/accounts/answer/1066447", "Google Authenticator"),
            buildAuthenticatorAppLink(
                "https://support.microsoft.com/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a",
                "Microsoft Authenticator"),
            buildAuthenticatorAppLink("https://2fast-app.de/", "2fast (Windows)"));
    authenticatorAppLinksLayout.setPadding(false);
    authenticatorAppLinksLayout.setSpacing(false);
    authenticatorAppLinksLayout.addClassName("two-factor-app-links");

    Span appHintSpan = new Span(messageProperties.getTwoFactorAppHint());
    appHintSpan.addClassName(PANEL_DESCRIPTION_CLASS);
    HorizontalLayout appCompatibilityLayout =
        new HorizontalLayout(
            AppIcon.INFO_CIRCLE.create(),
            new Span(messageProperties.getTwoFactorAppCompatibility()));
    appCompatibilityLayout.setPadding(false);
    appCompatibilityLayout.setSpacing(false);
    appCompatibilityLayout.setAlignItems(Alignment.CENTER);
    appCompatibilityLayout.addClassName("two-factor-app-compatibility");

    VerticalLayout prepareAuthenticatorStepLayout =
        new VerticalLayout(
            buildSetupStepLabelSpan(AppIcon.NUMBER_ONE, messageProperties.getTwoFactorAppStep()),
            appHintSpan,
            authenticatorAppLinksLayout,
            appCompatibilityLayout);
    prepareAuthenticatorStepLayout.setPadding(false);
    prepareAuthenticatorStepLayout.setSpacing(false);
    prepareAuthenticatorStepLayout.addClassName("two-factor-setup-step");

    enrollmentOtpField.setLabel(messageProperties.getTwoFactorSetupCodeLabel());
    Span enrollmentCodeHintSpan = new Span(messageProperties.getTwoFactorSetupCodeHint());
    enrollmentCodeHintSpan.addClassName(PANEL_DESCRIPTION_CLASS);
    VerticalLayout confirmEnrollmentCodeStepLayout =
        new VerticalLayout(
            buildSetupStepLabelSpan(AppIcon.NUMBER_TWO, messageProperties.getTwoFactorCodeStep()),
            enrollmentCodeHintSpan,
            enrollmentOtpField);
    confirmEnrollmentCodeStepLayout.setPadding(false);
    confirmEnrollmentCodeStepLayout.setSpacing(false);
    confirmEnrollmentCodeStepLayout.addClassName("two-factor-setup-step");

    VerticalLayout setupInstructionsLayout =
        new VerticalLayout(prepareAuthenticatorStepLayout, confirmEnrollmentCodeStepLayout);
    setupInstructionsLayout.setPadding(false);
    setupInstructionsLayout.setSpacing(false);
    setupInstructionsLayout.addClassName("two-factor-setup-instructions");
    return setupInstructionsLayout;
  }

  private Anchor buildAuthenticatorAppLink(String authenticatorAppUrl, String linkCaptionText) {
    Anchor authenticatorAppLink = new Anchor(authenticatorAppUrl, "");
    authenticatorAppLink.add(AppIcon.EXTERNAL_LINK_TABLER.create(), new Span(linkCaptionText));
    authenticatorAppLink.setTarget("_blank");
    authenticatorAppLink.getElement().setAttribute("rel", "noopener noreferrer");
    return authenticatorAppLink;
  }

  /**
   * Setzt den geheimen Schlüssel nur im internen Eingabeelement des Browsers. Der Wert der
   * Vaadin-Komponente bleibt leer, damit der Schlüssel nicht in der JDBC-Sitzung gespeichert wird.
   */
  private void loadManualSetupKeyIntoInput(TextArea manualKeyTextArea, Button copyManualKeyButton) {
    copyManualKeyButton.setEnabled(false);

    manualKeyTextArea
        .getElement()
        .executeJs(
            """
            const field = this;
            return (async () => {
            try {
              const response = await fetch($0, {credentials: 'same-origin', cache: 'no-store'});
              if (!response.ok) { return false; }
              const key = await response.text();
              await field.updateComplete;
              const input = field.inputElement;
              if (!field.isConnected) { return null; }
              if (!input?.readOnly || !key) { return false; }
              // Vierergruppen erleichtern das Ablesen.
              const groups = key.match(/.{1,4}/g);
              input.value = groups.slice(0, 4).join(' ') + '\\n' + groups.slice(4).join(' ');
              return true;
            } catch {
              return false;
            }
            })();
            """,
            buildTwoFactorDocumentPath("setup/" + pendingTwoFactorEnrollment.id() + "/key"))
        .then(
            Boolean.class,
            wasManualKeyLoaded -> {
              if (wasManualKeyLoaded == null) {
                return;
              }

              boolean isManualKeyAvailable = Boolean.TRUE.equals(wasManualKeyLoaded);
              manualKeyTextArea.setInvalid(!isManualKeyAvailable);
              copyManualKeyButton.setEnabled(isManualKeyAvailable);

              if (!isManualKeyAvailable) {
                NotificationHelper.getInstance()
                    .showNotification(
                        messageProperties::getTwoFactorManualLoadFailed,
                        NotificationHelper.NotificationType.ERROR);
              }
            });
  }

  private Span buildSetupStepLabelSpan(AppIcon stepAppIcon, String stepLabelText) {
    Span stepLabelSpan = new Span(stepAppIcon.create(), new Span(stepLabelText));
    stepLabelSpan.addClassName("two-factor-step-label");
    return stepLabelSpan;
  }

  // Ersatzcodes sichern und die Sicherung bestätigen
  private void showRecoveryCodeBackup() {
    cancelTwoFactorButton.setVisible(false);
    twoFactorPanelComponent.addClassName("two-factor-recovery");
    twoFactorPanelComponent.setHeading(
        messageProperties.getTwoFactorRecoveryTitle(),
        messageProperties.getTwoFactorRecoverySubtitle(),
        "");

    UUID backupOutputUuid = pendingTwoFactorEnrollment.id();
    if (!backupOutputUuid.equals(recoveryOutputUuid)) {
      recoveryOutputUuid = backupOutputUuid;
      confirmRecoveryCodesSavedSwitch.setValue(false);
      areRecoveryCodesVisible = true;
    }

    VerticalLayout backupExplanationLayout = buildRecoveryBackupExplanationLayout();
    VerticalLayout recoveryCodeFieldsLayout = new VerticalLayout();
    recoveryCodeFieldsLayout.setId("two-factor-recovery-codes");
    recoveryCodeFieldsLayout.setPadding(false);
    recoveryCodeFieldsLayout.setSpacing(false);
    recoveryCodeFieldsLayout.setWidthFull();
    recoveryCodeFieldsLayout.addClassName("two-factor-recovery-code-fields");

    List<Button> copyCodeButtons = new ArrayList<>();
    for (int recoveryCodeNumber = 1; recoveryCodeNumber <= 10; recoveryCodeNumber++) {
      TextField recoveryCodeTextField =
          buildRecoveryCodeTextField(recoveryCodeNumber, copyCodeButtons);
      recoveryCodeFieldsLayout.add(recoveryCodeTextField);
    }

    Details recoveryCodesDetails = new Details("", recoveryCodeFieldsLayout);
    recoveryCodesDetails.setId("two-factor-recovery-details");
    recoveryCodesDetails.setWidthFull();
    recoveryCodesDetails.setOpened(areRecoveryCodesVisible);

    Span recoveryCodesHeadingSpan =
        new Span(
            AppIcon.KEY.create(), new Span(messageProperties.getTwoFactorRecoveryCodesSummary()));
    recoveryCodesHeadingSpan.setId("two-factor-recovery-heading");
    recoveryCodesHeadingSpan.addClassName("two-factor-recovery-heading");
    Button recoveryCodesToggleButton =
        buildDetailsToggleButton(
            recoveryCodesDetails,
            "two-factor-recovery-toggle",
            messageProperties.getTwoFactorRecoveryCodesSummary());
    recoveryCodesDetails.addOpenedChangeListener(recoveryCodesOpenedChangeEvent ->
        areRecoveryCodesVisible = recoveryCodesOpenedChangeEvent.isOpened());
    recoveryCodesDetails
        .getElement()
        .setAttribute(ARIA_LABELLED_BY, recoveryCodesHeadingSpan.getId().orElseThrow());

    HorizontalLayout recoveryCodesHeaderLayout =
        new HorizontalLayout(recoveryCodesHeadingSpan, recoveryCodesToggleButton);
    recoveryCodesHeaderLayout.setPadding(false);
    recoveryCodesHeaderLayout.setSpacing(false);
    recoveryCodesHeaderLayout.setWidthFull();
    recoveryCodesHeaderLayout.setAlignItems(Alignment.CENTER);
    recoveryCodesHeaderLayout.addClassName("two-factor-recovery-codes-header");
    VerticalLayout recoveryCodesSectionLayout =
        new VerticalLayout(recoveryCodesHeaderLayout, recoveryCodesDetails);
    recoveryCodesSectionLayout.setPadding(false);
    recoveryCodesSectionLayout.setSpacing(false);

    Button copyAllCodesButton =
        new Button(messageProperties.getTwoFactorCopyCodes(), AppIcon.CLIPBOARD_COPY.create());
    copyAllCodesButton.setId("two-factor-copy-codes");
    copyAllCodesButton.setEnabled(false);

    recoveryCodeFieldsLayout.addAttachListener(_ ->
        loadRecoveryCodesIntoInputs(recoveryCodeFieldsLayout, copyCodeButtons, copyAllCodesButton));

    copyAllCodesButton.addClickListener(_ ->
        copyAllCodesButton
            .getElement()
            .executeJs(
                """
                const values = Array.from($0.querySelectorAll('vaadin-text-field'), field => field.inputElement?.value || '');
                return values.length === 10 && values.every(Boolean) && navigator.clipboard
                  ? navigator.clipboard.writeText(values.join('\\n')).then(() => true, () => false) : false;
                """,
                recoveryCodeFieldsLayout.getElement())
            .then(
                Boolean.class,
                wereCodesCopied ->
                    showRecoveryCodeCopyNotification(
                        wereCodesCopied, messageProperties::getTwoFactorCopySuccess)));

    Button downloadCodesPdfButton = buildRecoveryCodesPdfDownloadButton(backupOutputUuid);
    copyAllCodesButton.setWidthFull();
    downloadCodesPdfButton.setWidthFull();
    VerticalLayout backupActionsLayout =
        new VerticalLayout(copyAllCodesButton, downloadCodesPdfButton);
    backupActionsLayout.setPadding(false);
    backupActionsLayout.setSpacing(false);
    backupActionsLayout.addClassName("two-factor-recovery-actions");
    backupExplanationLayout.add(backupActionsLayout);

    confirmRecoveryCodesSavedSwitch.setLabel(
        messageProperties.getTwoFactorCodesSavedConfirmation());
    confirmRecoveryCodesSavedSwitch.addThemeVariants(SwitchVariant.AURA_REVERSE);

    VerticalLayout recoveryBackupControlsLayout =
        new VerticalLayout(recoveryCodesSectionLayout, confirmRecoveryCodesSavedSwitch);
    recoveryBackupControlsLayout.setPadding(false);
    recoveryBackupControlsLayout.setSpacing(false);
    recoveryBackupControlsLayout.addClassName("two-factor-recovery-controls");

    Div recoveryBackupLayoutDiv = new Div(backupExplanationLayout, recoveryBackupControlsLayout);
    recoveryBackupLayoutDiv.addClassName("two-factor-recovery-layout");
    stepContentDiv.add(recoveryBackupLayoutDiv);

    continueTwoFactorButton.setText(messageProperties.getTwoFactorCodesSaved());
    updateRecoveryBackupConfirmationButton();
  }

  private TextField buildRecoveryCodeTextField(
      int recoveryCodeNumber, List<Button> copyCodeButtons) {
    TextField recoveryCodeTextField = new TextField();
    recoveryCodeTextField.setId("two-factor-recovery-code-" + recoveryCodeNumber);
    recoveryCodeTextField.setAriaLabel(
        messageProperties.getTwoFactorRecoveryCodeLabel() + " " + recoveryCodeNumber);
    recoveryCodeTextField.setReadOnly(true);
    recoveryCodeTextField.setWidthFull();
    recoveryCodeTextField.setAutocomplete(Autocomplete.OFF);
    recoveryCodeTextField.addClassName("two-factor-recovery-code");

    Button copyCodeButton = new Button(AppIcon.CLIPBOARD_COPY.create());
    copyCodeButton.setAriaLabel(
        messageProperties.getProviderSettingsCopy()
            + ": "
            + recoveryCodeTextField.getAriaLabel().orElseThrow());
    copyCodeButton.setTooltipText(messageProperties.getProviderSettingsCopy());
    copyCodeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    copyCodeButton.addClassName("two-factor-recovery-copy");
    copyCodeButton.setEnabled(false);
    copyCodeButton.addClickListener(_ ->
        copyCodeButton
            .getElement()
            .executeJs(
                """
                const value = $0.inputElement?.value;
                return value && navigator.clipboard
                  ? navigator.clipboard.writeText(value).then(() => true, () => false) : false;
                """,
                recoveryCodeTextField.getElement())
            .then(
                Boolean.class,
                wereCodesCopied ->
                    showRecoveryCodeCopyNotification(
                        wereCodesCopied, messageProperties::getTwoFactorCodeCopied)));

    recoveryCodeTextField.setSuffixComponent(copyCodeButton);
    copyCodeButtons.add(copyCodeButton);
    return recoveryCodeTextField;
  }

  private VerticalLayout buildRecoveryBackupExplanationLayout() {
    Image recoveryVaultImage = new Image("images/two-factor-vault.svg", "");
    recoveryVaultImage.addClassName("two-factor-recovery-art");
    recoveryVaultImage.getElement().setAttribute("aria-hidden", "true");

    VerticalLayout backupExplanationLayout =
        new VerticalLayout(
            recoveryVaultImage,
            buildRecoveryHintLayout(
                AppIcon.NUMBER_ONE,
                messageProperties.getTwoFactorRecoveryOnceTitle(),
                messageProperties.getTwoFactorRecoveryOnceHint()),
            buildRecoveryHintLayout(
                AppIcon.NUMBER_TWO,
                messageProperties.getTwoFactorRecoveryRestoreTitle(),
                messageProperties.getTwoFactorRecoveryRestoreHint()),
            buildRecoveryHintLayout(
                AppIcon.NUMBER_THREE,
                messageProperties.getTwoFactorRecoveryStorageTitle(),
                messageProperties.getTwoFactorRecoveryStorage()));
    backupExplanationLayout.setPadding(false);
    backupExplanationLayout.setSpacing(false);
    backupExplanationLayout.addClassName("two-factor-recovery-explanation");
    return backupExplanationLayout;
  }

  private Button buildRecoveryCodesPdfDownloadButton(UUID backupOutputUuid) {
    Locale downloadLocale = VaadinSession.getCurrent().getLocale();
    Button downloadCodesPdfButton =
        new Button(messageProperties.getTwoFactorDownloadPdf(), AppIcon.DOWNLOAD.create());

    downloadCodesPdfButton
        .getElement()
        .setAttribute(
            "data-download-url", recoveryCodeDownload.handler(backupOutputUuid, downloadLocale));

    recoveryCodeDownload.configure(
        downloadCodesPdfButton,
        () ->
            NotificationHelper.getInstance()
                .showNotification(
                    messageProperties::getTwoFactorDownloadFailed,
                    NotificationHelper.NotificationType.ERROR));
    downloadCodesPdfButton.setId("two-factor-download-pdf");
    return downloadCodesPdfButton;
  }

  /**
   * Setzt die Ersatzcodes nur in den Eingabeelementen des Browsers, damit sie nicht als Feldwerte
   * in der JDBC-Sitzung gespeichert werden.
   */
  private void loadRecoveryCodesIntoInputs(
      VerticalLayout recoveryCodeFieldsLayout,
      List<Button> copyCodeButtons,
      Button copyAllCodesButton) {
    recoveryCodeFieldsLayout
        .getElement()
        .executeJs(
            """
            const container = this;
            return (async () => {
              try {
                const response = await fetch($0, {credentials:'same-origin', cache:'no-store'});
                if (!response.ok) { return false; }
                const values = (await response.text()).trim().split('\\n');
                const fields = Array.from(container.querySelectorAll('vaadin-text-field'));
                await Promise.all(fields.map(field => field.updateComplete));
                if (!container.isConnected) { return null; }
                if (values.length !== fields.length || !values.every(Boolean)
                    || !fields.every(field => field.inputElement?.readOnly)) { return false; }
                fields.forEach((field, index) => { field.inputElement.value = values[index]; });
                return true;
              } catch {
                return false;
              }
            })();
            """,
            buildTwoFactorDocumentPath("recovery/" + pendingTwoFactorEnrollment.id() + "/codes"))
        .then(
            Boolean.class,
            wereRecoveryCodesLoaded -> {
              if (wereRecoveryCodesLoaded == null) {
                return;
              }

              boolean areRecoveryCodesAvailable = Boolean.TRUE.equals(wereRecoveryCodesLoaded);
              copyCodeButtons.forEach(
                  copyCodeButton -> copyCodeButton.setEnabled(areRecoveryCodesAvailable));
              copyAllCodesButton.setEnabled(areRecoveryCodesAvailable);

              if (!areRecoveryCodesAvailable) {
                NotificationHelper.getInstance()
                    .showNotification(
                        messageProperties::getTwoFactorRecoveryLoadFailed,
                        NotificationHelper.NotificationType.ERROR);
              }
            });
  }

  private void showRecoveryCodeCopyNotification(
      Boolean wasRecoveryCopySuccessful, Supplier<String> copySuccessTextSupplier) {
    NotificationHelper.getInstance()
        .showNotification(
            Boolean.TRUE.equals(wasRecoveryCopySuccessful)
                ? copySuccessTextSupplier
                : messageProperties::getTwoFactorCopyFailed,
            Boolean.TRUE.equals(wasRecoveryCopySuccessful)
                ? NotificationHelper.NotificationType.SUCCESS
                : NotificationHelper.NotificationType.ERROR);
  }

  private HorizontalLayout buildRecoveryHintLayout(
      AppIcon hintAppIcon, String hintTitleText, String hintDescriptionText) {
    Span hintTitleSpan = new Span(hintTitleText);
    hintTitleSpan.addClassName("two-factor-recovery-hint-title");
    Span hintDescriptionSpan = new Span(hintDescriptionText);
    hintDescriptionSpan.addClassName(PANEL_DESCRIPTION_CLASS);

    VerticalLayout hintTextLayout = new VerticalLayout(hintTitleSpan, hintDescriptionSpan);
    hintTextLayout.setPadding(false);
    hintTextLayout.setSpacing(false);
    HorizontalLayout recoveryHintRowLayout =
        new HorizontalLayout(hintAppIcon.create(), hintTextLayout);
    recoveryHintRowLayout.setPadding(false);
    recoveryHintRowLayout.setSpacing(false);
    recoveryHintRowLayout.addClassName("two-factor-recovery-hint");
    return recoveryHintRowLayout;
  }

  private void updateRecoveryBackupConfirmationButton() {
    if (pendingTwoFactorEnrollment != null
        && pendingTwoFactorEnrollment.kind() == Kind.RECOVERY_OUTPUT) {
      continueTwoFactorButton.setEnabled(confirmRecoveryCodesSavedSwitch.getValue());
    }
  }

  private String buildTwoFactorDocumentPath(String documentPathSuffix) {
    return httpServletRequest.getContextPath()
        + TwoFactorDocumentsController.PATH
        + "/"
        + documentPathSuffix;
  }

  // Bestätigung der einzelnen Schritte und Rückmeldung bei Fehlern
  private void submitTwoFactorForm() {
    try {
      submitCurrentTwoFactorStep();
    } catch (RuntimeException twoFactorFailure) {
      markCurrentTwoFactorInputInvalid();
      showTwoFactorFailureNotification(twoFactorFailure);
    } finally {
      setupPasswordField.clear();
      additionalProofTextField.clear();
      enrollmentOtpField.clear();
      confirmationOtpField.clear();
    }
  }

  private void submitCurrentTwoFactorStep() {
    if (isShowingSetupIntroduction) {
      if (twoFactorEnrollment.canBeginFromPasswordLogin(httpServletRequest)) {
        twoFactorEnrollment.beginFromPasswordLogin(httpServletRequest);
      }

      httpServletRequest.getSession().setAttribute(TwoFactorFlow.INTRO_ACCEPTED, true);
      renderCurrentTwoFactorStep();
      return;
    }

    if (pendingTwoFactorEnrollment == null) {
      submitTwoFactorStepWithoutPendingEnrollment();
      return;
    }

    switch (pendingTwoFactorEnrollment.kind()) {
      case RECOVERY_OUTPUT -> confirmRecoveryCodesSaved();
      case ENROLLMENT -> confirmAuthenticatorEnrollment();
      case GOOGLE_EMAIL, ACTION -> confirmPendingAdditionalProof();
    }
  }

  private void confirmPendingAdditionalProof() {
    if (!ValidationHelper.validateRequiredInputs(
        List.of(additionalProofTextField), messageProperties)) {
      return;
    }

    if (pendingTwoFactorEnrollment.kind() == Kind.GOOGLE_EMAIL) {
      twoFactorEnrollment.confirmEmail(
          httpServletRequest, pendingTwoFactorEnrollment.id(), additionalProofTextField.getValue());
    } else {
      twoFactorEnrollment.complete(
          httpServletRequest, pendingTwoFactorEnrollment.id(), additionalProofTextField.getValue());
    }

    renderCurrentTwoFactorStep();
  }

  private void confirmRecoveryCodesSaved() {
    if (Boolean.TRUE.equals(confirmRecoveryCodesSavedSwitch.getValue())) {
      twoFactorEnrollment.dismissOutput(httpServletRequest, pendingTwoFactorEnrollment.id());
      finishTwoFactorFlow();
    }
  }

  private void confirmAuthenticatorEnrollment() {
    if (ValidationHelper.validateRequiredInputs(List.of(enrollmentOtpField), messageProperties)) {
      twoFactorEnrollment.complete(
          httpServletRequest, pendingTwoFactorEnrollment.id(), enrollmentOtpField.getValue());
      renderCurrentTwoFactorStep();
    }
  }

  private void submitTwoFactorStepWithoutPendingEnrollment() {
    if (isInitialSetupMode) {
      if (ValidationHelper.validateRequiredInputs(List.of(setupPasswordField), messageProperties)) {
        twoFactorEnrollment.beginPassword(httpServletRequest, setupPasswordField.getValue());
        renderCurrentTwoFactorStep();
      }
      return;
    }

    if (isRecoveryMode) {
      if (ValidationHelper.validateRequiredInputs(
          List.of(additionalProofTextField), messageProperties)) {
        twoFactorEnrollment.beginRecovery(httpServletRequest, additionalProofTextField.getValue());
        renderCurrentTwoFactorStep();
      }
      return;
    }

    if (isRecoveryCodeLoginSelected) {
      if (ValidationHelper.validateRequiredInputs(
          List.of(additionalProofTextField), messageProperties)) {
        twoFactorAuthorization.confirmSession(
            httpServletRequest, additionalProofTextField.getValue());
        finishTwoFactorFlow();
      }
    } else if (ValidationHelper.validateRequiredInputs(
        List.of(confirmationOtpField), messageProperties)) {
      twoFactorAuthorization.confirmSession(httpServletRequest, confirmationOtpField.getValue());
      finishTwoFactorFlow();
    }
  }

  private void markCurrentTwoFactorInputInvalid() {
    if (isInitialSetupMode) {
      setupPasswordField.setInvalid(true);
    } else if (pendingTwoFactorEnrollment != null
        && pendingTwoFactorEnrollment.kind() == Kind.ENROLLMENT) {
      enrollmentOtpField.setInvalid(true);
    } else if (pendingTwoFactorEnrollment == null
        && !isRecoveryMode
        && !isRecoveryCodeLoginSelected) {
      confirmationOtpField.setInvalid(true);
    } else {
      additionalProofTextField.setInvalid(true);
    }
  }

  private void showTwoFactorFailureNotification(RuntimeException twoFactorFailure) {
    Supplier<String> failureTextSupplier = messageProperties::getTwoFactorFailed;
    if (twoFactorFailure instanceof LoginThrottledException) {
      failureTextSupplier = messageProperties::getLoginThrottledMessage;
    } else if (twoFactorFailure instanceof LockedException) {
      failureTextSupplier = messageProperties::getTwoFactorThrottled;
    } else if (twoFactorFailure instanceof BadCredentialsException) {
      failureTextSupplier =
          isInitialSetupMode
              ? messageProperties::getAccountSecurityWrongPassword
              : messageProperties::getTwoFactorInvalidCode;
    }

    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            failureTextSupplier,
            NotificationHelper.NotificationType.ERROR);
  }

  // Wechsel, Abbruch und Abschluss des geschützten Ablaufs
  private void cancelTwoFactorFlow() {
    if (!twoFactorEnrollment.cancelPreparation(httpServletRequest)) {
      // Bereits ausgegebene Ersatzcodes müssen zuerst gesichert und bestätigt werden.
      renderCurrentTwoFactorStep();
      return;
    }

    externalLoginFlow.cancel(httpServletRequest);
    TwoFactorFlow.finish(httpServletRequest);

    if (twoFactorFlow.account() && twoFactorAuthorization.sessionConfirmed(httpServletRequest)) {
      UI.getCurrent()
          .getPage()
          .setLocation(
              httpServletRequest.getContextPath() + AppRouteConstants.ACCOUNT_PORTAL_LOCATION);
    } else {
      securityService.logout();
      UI.getCurrent().getPage().setLocation(httpServletRequest.getContextPath() + "/login");
    }
  }

  private void finishTwoFactorFlow() {
    twoFactorAuthorization.requireSession(httpServletRequest);
    TwoFactorFlow.finish(httpServletRequest);

    String destinationLocation = AppRouteConstants.ACCOUNT_PORTAL_LOCATION;
    if (!twoFactorFlow.account() && !isRecoveryMode) {
      boolean isAdministrativeUser =
          twoFactorAuthorization.owner(httpServletRequest).roles().stream()
              .anyMatch(RoleType::isAdministrative);
      destinationLocation = isAdministrativeUser ? "/admin" : "/home";
    }

    UI.getCurrent()
        .getPage()
        .setLocation(httpServletRequest.getContextPath() + destinationLocation);
  }

  private void navigateToTwoFactorFlow(TwoFactorFlow nextTwoFactorFlow) {
    nextTwoFactorFlow.enter(httpServletRequest);
    UI.getCurrent()
        .getPage()
        .setLocation(httpServletRequest.getContextPath() + nextTwoFactorFlow.location());
  }
}
