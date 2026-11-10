package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;

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
import de.derpeterson.app.security.mfa.TwoFactorDocuments;
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

/**
 * Eingeschränkter erster Zugang. Vollständiger Portalzugriff folgt erst nach der aktuellen Regel.
 */
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
  private final transient HttpServletRequest httpRequest;
  private final transient RecoveryCodeDownload recoveryCodeDownload;
  private final PublicPageComponent twoFactorPage;
  private final AccountPanelComponent twoFactorPanel =
      new AccountPanelComponent("two-factor-panel", AppIcon.LOCK);
  private final Div stepContentLayout = new Div();
  private final Div additionalStepActions = new Div();
  private final HorizontalLayout stepNavigationLayout = new HorizontalLayout();
  private final Button cancelTwoFactorButton = new Button();
  private final Button continueTwoFactorButton = new Button();
  private final PasswordField passwordField = new PasswordField();
  private final TextField additionalProofField = new TextField();
  private final OtpField enrollmentOtpField = new OtpField();
  private final OtpField confirmationOtpField = new OtpField();
  private final Switch confirmRecoveryCodesSavedSwitch = new Switch();
  private UUID recoveryOutputId;
  private TwoFactorEnrollment.Pending pendingTwoFactorChallenge;
  private TwoFactorFlow twoFactorFlow;
  private boolean recoveryMode;
  private boolean initialSetupMode;
  private boolean showingSetupIntroduction;
  private boolean manualSetupSelected;
  private boolean recoveryCodesVisible;
  private boolean recoveryLoginSelected;

  @Autowired
  public TwoFactorView(
      ConfigService config,
      MessageProperties messageProperties,
      SecurityService securityService,
      TwoFactorAuthorization twoFactorAuthorization,
      TwoFactorEnrollment twoFactorEnrollment,
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService,
      RecoveryCodeDownload recoveryCodeDownload,
      HttpServletRequest httpRequest) {
    this.messageProperties = messageProperties;
    this.securityService = securityService;
    this.twoFactorAuthorization = twoFactorAuthorization;
    this.twoFactorEnrollment = twoFactorEnrollment;
    this.externalLoginFlow = externalLoginFlow;
    this.externalIdentityService = externalIdentityService;
    this.httpRequest = httpRequest;
    this.recoveryCodeDownload = recoveryCodeDownload;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("two-factor-view");
    twoFactorPage =
        new PublicPageComponent(config, messageProperties, PublicPageComponent.Page.TWO_FACTOR);
    stepContentLayout.addClassName("two-factor-content");
    additionalStepActions.addClassName("two-factor-extra-actions");
    twoFactorPanel.getBody().add(stepContentLayout, additionalStepActions);
    passwordField.setId("two-factor-password");
    passwordField.setWidthFull();
    passwordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    passwordField.setManualValidation(true);
    passwordField.setRequiredIndicatorVisible(true);
    additionalProofField.setId("two-factor-code");
    additionalProofField.setWidthFull();
    additionalProofField.setManualValidation(true);
    additionalProofField.setRequiredIndicatorVisible(true);
    additionalProofField.getElement().setAttribute(AUTOCOMPLETE_ATTRIBUTE, "one-time-code");
    configureOtp(enrollmentOtpField, "two-factor-enrollment-code");
    configureOtp(confirmationOtpField, "two-factor-code");
    confirmRecoveryCodesSavedSwitch.setId("two-factor-codes-saved");
    confirmRecoveryCodesSavedSwitch.addValueChangeListener(event -> updateRecoveryActions());
    cancelTwoFactorButton.setId("two-factor-cancel");
    cancelTwoFactorButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelTwoFactorButton.addClickListener(event -> cancel());
    continueTwoFactorButton.setId("two-factor-submit");
    continueTwoFactorButton.setPrefixComponent(AppIcon.CHECK.create());
    continueTwoFactorButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    continueTwoFactorButton.addClickListener(event -> submitTwoFactorForm());
    HorizontalLayout actions = new HorizontalLayout(cancelTwoFactorButton, continueTwoFactorButton);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(actions);
    stepNavigationLayout.setPadding(false);
    stepNavigationLayout.setSpacing(false);
    stepNavigationLayout.addClassName("two-factor-footer-navigation");
    HorizontalLayout footer = new HorizontalLayout(stepNavigationLayout, actions);
    footer.setPadding(false);
    footer.setSpacing(false);
    footer.setWidthFull();
    footer.setAlignItems(Alignment.CENTER);
    footer.addClassName("two-factor-footer");
    twoFactorPanel.addActionFooter(footer);
    twoFactorPage.getCardContainer().add(twoFactorPanel);
    add(twoFactorPage);
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> renderCurrentTwoFactorStep());
  }

  private void configureOtp(OtpField field, String id) {
    field.setId(id);
    TwoFactorCodeFields.configureOtpField(field, httpRequest.getContextPath());
    field.addCompleteListener(event -> {
      if (continueTwoFactorButton.isVisible()
          && continueTwoFactorButton.isEnabled()
          && TwoFactorCodeFields.isCurrentCompleteOtp(field, event)) {
        submitTwoFactorForm();
      }
    });
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (securityService.getAuthenticatedUser(httpRequest).isEmpty()) {
      event.forwardTo(LoginView.class);
      return;
    }
    twoFactorFlow = TwoFactorFlow.current(httpRequest);
    if (twoFactorFlow == null) {
      Map<String, List<String>> query = event.getLocation().getQueryParameters().getParameters();
      boolean confirmed = twoFactorAuthorization.sessionConfirmed(httpRequest);
      if (query.containsKey("recover")) {
        twoFactorFlow = confirmed ? TwoFactorFlow.ACCOUNT_RECOVERY : TwoFactorFlow.RECOVERY;
      } else if (query.containsKey("administration")) {
        twoFactorFlow = TwoFactorFlow.ADMINISTRATION;
      } else {
        twoFactorFlow =
            confirmed && query.containsKey("setup") ? TwoFactorFlow.SETUP : TwoFactorFlow.LOGIN;
      }
      twoFactorFlow.enter(httpRequest);
    }
    if (twoFactorFlow.help()) {
      event.forwardTo(RecoveryHelpView.class);
      return;
    }
    twoFactorPage.setAccountSecurity(
        twoFactorFlow.account(), securityService.getCurrentUser(httpRequest).orElse(null));
    recoveryMode = twoFactorFlow.recovery();
    renderCurrentTwoFactorStep();
  }

  public boolean isCurrent(TwoFactorFlow active) {
    return twoFactorFlow == active;
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    ExternalProofNotice.show(event, messageProperties);
  }

  private void renderCurrentTwoFactorStep() {
    if (securityService.getAuthenticatedUser(httpRequest).isEmpty()) {
      return;
    }
    clearPreviousStep();

    pendingTwoFactorChallenge = twoFactorEnrollment.pending(httpRequest);
    resetStepPresentation();

    recoveryMode =
        recoveryMode
            || pendingTwoFactorChallenge != null
                && pendingTwoFactorChallenge.proof() == SetupProof.RECOVERY_CODE;
    TwoFactorAuthorization.State state =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(httpRequest));
    initialSetupMode = pendingTwoFactorChallenge == null && !state.enrolled();
    boolean startsRecovery = pendingTwoFactorChallenge == null && state.enrolled() && recoveryMode;
    twoFactorPanel.setIcon((startsRecovery ? AppIcon.LOCK_RESET : AppIcon.LOCK).create());
    twoFactorPanel.setHeading(
        initialSetupMode
            ? messageProperties.getTwoFactorSetupTitle()
            : messageProperties.getTwoFactorConfirmTitle(),
        messageProperties.getTwoFactorPageHint(),
        "");
    cancelTwoFactorButton.setText(
        twoFactorFlow.account() && twoFactorAuthorization.sessionConfirmed(httpRequest)
            ? messageProperties.getTwoFactorBack()
            : messageProperties.getBaseLogoutButton());
    if (!twoFactorFlow.account() || !twoFactorAuthorization.sessionConfirmed(httpRequest)) {
      cancelTwoFactorButton.addClassName(DELETE_BUTTON);
      cancelTwoFactorButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    }
    continueTwoFactorButton.setEnabled(true);
    cancelTwoFactorButton.setEnabled(true);
    cancelTwoFactorButton.setVisible(true);
    if (pendingTwoFactorChallenge != null) {
      showPending();
    } else if (startsRecovery) {
      showRecovery();
    } else if (state.enrolled()) {
      showExistingFactor();
    } else {
      showSetup();
    }
  }

  private void clearPreviousStep() {
    stepContentLayout.removeAll();
    additionalStepActions.removeAll();
    stepNavigationLayout.removeAll();
    passwordField.clear();
    additionalProofField.clear();
    enrollmentOtpField.clear();
    confirmationOtpField.clear();
    passwordField.setInvalid(false);
    additionalProofField.setInvalid(false);
    enrollmentOtpField.setInvalid(false);
    confirmationOtpField.setInvalid(false);
  }

  private void resetStepPresentation() {
    showingSetupIntroduction = false;
    twoFactorPanel.removeClassName("two-factor-introduction");
    twoFactorPanel.removeClassName("two-factor-setup");
    twoFactorPanel.removeClassName("two-factor-recovery");
    twoFactorPanel.removeClassName("two-factor-confirmation");
    twoFactorPanel.removeClassName("two-factor-restore");
    cancelTwoFactorButton.removeClassName(DELETE_BUTTON);
    cancelTwoFactorButton.setPrefixComponent(AppIcon.CLOSE.create());
    continueTwoFactorButton.setPrefixComponent(AppIcon.CHECK.create());
  }

  private void showExistingFactor() {
    if (twoFactorFlow.administration()
        ? twoFactorAuthorization.administrationConfirmed(httpRequest)
        : twoFactorAuthorization.sessionConfirmed(httpRequest)) {
      finish();
      return;
    }
    showLoginMethods();
    Button recover =
        new Button(messageProperties.getTwoFactorRecoverAction(), AppIcon.OPEN_SECTION.create());
    recover.setIconAfterText(true);
    recover.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    recover.setId("two-factor-recover");
    recover.addClickListener(event ->
        moveTo(
            twoFactorFlow.administration()
                ? TwoFactorFlow.ADMIN_RECOVERY
                : TwoFactorFlow.RECOVERY));
    Button help =
        new Button(
            messageProperties.getSupportHelpTitle(),
            event ->
                moveTo(
                    twoFactorFlow.administration()
                        ? TwoFactorFlow.ADMIN_HELP
                        : TwoFactorFlow.LOGIN_HELP));
    help.setId("two-factor-help");
    help.setIcon(AppIcon.OPEN_SECTION.create());
    help.setIconAfterText(true);
    help.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    Span topic =
        new Span(
            AppIcon.QUESTION_CIRCLE.create(),
            new Span(messageProperties.getTwoFactorLoginHelpTitle()));
    topic.addClassName("two-factor-login-help-topic");
    VerticalLayout links =
        new VerticalLayout(
            loginHelpRow(messageProperties.getTwoFactorLoginRestorePrompt(), recover),
            loginHelpRow(messageProperties.getSupportHelpSubtitle(), help));
    links.setPadding(false);
    links.setSpacing(false);
    links.addClassName("two-factor-login-help-links");
    Details alternatives = new Details("", links);
    alternatives.setId("two-factor-login-help");
    alternatives.setWidthFull();
    Button toggle =
        detailsToggle(
            alternatives,
            "two-factor-login-help-toggle",
            messageProperties.getTwoFactorLoginHelpTitle());
    toggle.setPrefixComponent(topic);
    toggle.addClassName("two-factor-login-help-toggle");
    additionalStepActions.add(toggle, alternatives);
    continueTwoFactorButton.setText(
        twoFactorFlow.administration()
            ? messageProperties.getTwoFactorConfirm()
            : messageProperties.getBaseLoginButton());
    if (!twoFactorFlow.administration()) {
      continueTwoFactorButton.setPrefixComponent(AppIcon.SIGN_IN.create());
    }
  }

  private HorizontalLayout loginHelpRow(String question, Button action) {
    HorizontalLayout row = new HorizontalLayout(new Span(question), action);
    row.setAlignItems(Alignment.CENTER);
    row.addClassName("two-factor-help-row");
    return row;
  }

  private Button detailsToggle(Details details, String id, String topic) {
    Button toggle = new DetailsToggleButton(messageProperties, details, topic);
    toggle.setId(id);
    return toggle;
  }

  private void showLoginMethods() {
    twoFactorPanel.addClassName("two-factor-confirmation");
    twoFactorPanel.setIcon(AppIcon.SHIELD_KEYHOLE.create());
    twoFactorPanel.setHeading(
        messageProperties.getTwoFactorConfirmTitle(),
        messageProperties.getTwoFactorConfirmSubtitle(),
        "");
    Tab authenticator =
        new Tab(
            AppIcon.DEVICES.create(), new Span(messageProperties.getTwoFactorConfirmationAppTab()));
    Tab recovery =
        new Tab(AppIcon.KEY.create(), new Span(messageProperties.getTwoFactorRecoveryCodeLabel()));
    authenticator.setId("two-factor-authenticator-tab");
    recovery.setId("two-factor-recovery-tab");
    VerticalLayout input = new VerticalLayout();
    input.setPadding(false);
    input.setSpacing(false);
    input.setWidthFull();
    input.setId(CONFIRMATION_INPUT_ID);
    input.addClassName(CONFIRMATION_INPUT_ID);
    input.getElement().setAttribute("role", TABPANEL_ROLE);
    for (Tab tab : List.of(authenticator, recovery)) {
      tab.getElement().setAttribute(ARIA_CONTROLS, CONFIRMATION_INPUT_ID);
    }
    Tabs selector = new Tabs(authenticator, recovery);
    selector.setWidthFull();
    selector.addClassName("two-factor-method-tabs");
    selector.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
    selector.setSelectedTab(recoveryLoginSelected ? recovery : authenticator);
    selector.addSelectedChangeListener(event -> {
      recoveryLoginSelected = event.getSelectedTab() == recovery;
      showLoginInput(input);
    });
    stepContentLayout.add(selector, input);
    showLoginInput(input);
  }

  private void showLoginInput(VerticalLayout input) {
    input.removeAll();
    additionalProofField.clear();
    confirmationOtpField.clear();
    additionalProofField.setInvalid(false);
    confirmationOtpField.setInvalid(false);
    input
        .getElement()
        .setAttribute(
            ARIA_LABELLED_BY,
            recoveryLoginSelected ? "two-factor-recovery-tab" : "two-factor-authenticator-tab");
    String hintText =
        recoveryLoginSelected
            ? messageProperties.getTwoFactorLoginRecoveryHint()
            : messageProperties.getTwoFactorLoginHint();
    Span hint =
        new Span(
            twoFactorFlow.administration() ? messageProperties.getTwoFactorActionHint() : hintText);
    hint.addClassName(PANEL_DESCRIPTION_CLASS);
    input.add(hint);
    if (recoveryLoginSelected) {
      input.add(prepareRecoveryCode());
    } else {
      confirmationOtpField.setLabel(messageProperties.getTwoFactorSetupCodeLabel());
      input.add(confirmationOtpField);
    }
  }

  private void showRecovery() {
    twoFactorPanel.addClassName("two-factor-restore");
    twoFactorPanel.setHeading(
        messageProperties.getTwoFactorRecoverAction(),
        messageProperties.getTwoFactorRecoverSubtitle(),
        "");
    if (!twoFactorEnrollment.available()) {
      addHint(messageProperties.getTwoFactorUnavailable());
    }
    stepContentLayout.add(prepareRecoveryCode());
    continueTwoFactorButton.setVisible(true);
    continueTwoFactorButton.setEnabled(twoFactorEnrollment.available());
    continueTwoFactorButton.setText(messageProperties.getTwoFactorRecoveryContinue());
    HorizontalLayout notice =
        new HorizontalLayout(
            AppIcon.INFO_CIRCLE.create(),
            new Div(
                new Paragraph(messageProperties.getTwoFactorRecoveryPreviousAppHint()),
                new Paragraph(messageProperties.getTwoFactorRecoveryConsumptionHint())));
    notice.setPadding(false);
    notice.setSpacing(false);
    notice.setAlignItems(Alignment.CENTER);
    notice.addClassName("two-factor-restore-notice");
    stepContentLayout.add(notice);
    Button back =
        new Button(messageProperties.getTwoFactorReturnToCode(), AppIcon.ANGLE_LEFT.create());
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    back.setId("two-factor-recovery-back");
    back.addClickListener(event ->
        moveTo(
            twoFactorFlow.administration() ? TwoFactorFlow.ADMINISTRATION : TwoFactorFlow.LOGIN));
    stepNavigationLayout.add(back);
  }

  private void showSetup() {
    if (!twoFactorFlow.account()
        && !Boolean.TRUE.equals(httpRequest.getSession().getAttribute(TwoFactorFlow.INTRO_ACCEPTED))
        && (twoFactorAuthorization.requires(httpRequest, TwoFactorAction.LOGIN)
            || twoFactorAuthorization.requires(httpRequest, TwoFactorAction.ADMINISTRATION))) {
      showIntroduction();
      return;
    }
    addHint(
        twoFactorEnrollment.available()
            ? messageProperties.getTwoFactorSetupHint()
            : messageProperties.getTwoFactorUnavailable());
    boolean local = externalLoginFlow.owner(httpRequest).getPassword() != null;
    if (local) {
      passwordField.setLabel(messageProperties.getAccountPasswordDialogCurrentPassword());
      passwordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
      stepContentLayout.add(passwordField);
    }
    continueTwoFactorButton.setVisible(local);
    continueTwoFactorButton.setEnabled(twoFactorEnrollment.available());
    continueTwoFactorButton.setText(messageProperties.getTwoFactorStartSetup());
    for (String provider :
        externalIdentityService.connected(twoFactorAuthorization.owner(httpRequest).id())) {
      if (!availableEnrollmentProvider(provider)) {
        continue;
      }
      additionalStepActions.add(enrollmentProofButton(provider));
    }
    if (!local && additionalStepActions.getComponentCount() == 0) {
      addHint(messageProperties.getExternalProofUnavailable());
    }
  }

  private Button enrollmentProofButton(String provider) {
    boolean google = ExternalProviderSettings.GOOGLE.equals(provider);
    String brand =
        google ? ExternalProviderSettings.GOOGLE_NAME : ExternalProviderSettings.MICROSOFT_NAME;
    Button proof = new Button(messageProperties.getExternalProofWith(brand));
    proof.setId("two-factor-confirm-" + provider);
    Image mark = new Image("icons/external-login/" + provider + (google ? ".png" : ".svg"), "");
    mark.addClassName("external-proof-provider-mark");
    proof.setPrefixComponent(mark);
    proof.setEnabled(twoFactorEnrollment.available());
    proof.addClickListener(event -> {
      try {
        UI.getCurrent()
            .getPage()
            .setLocation(
                externalLoginFlow.beginProof(
                    httpRequest,
                    provider,
                    ExternalLoginFlow.Purpose.ENROLL_FACTOR,
                    VaadinSession.getCurrent().getLocale()));
      } catch (RuntimeException failure) {
        showTwoFactorFailure(failure);
      }
    });
    return proof;
  }

  private boolean availableEnrollmentProvider(String provider) {
    if (!externalLoginFlow.providers().contains(provider)) {
      return false;
    }
    TwoFactorAuthorization.State state =
        twoFactorAuthorization.state(twoFactorAuthorization.owner(httpRequest));
    return !ExternalProviderSettings.GOOGLE.equals(provider)
        || !state.previouslyEnrolled()
        || state.operatorEnrollment();
  }

  private void showIntroduction() {
    showingSetupIntroduction = true;
    twoFactorPanel.addClassName("two-factor-introduction");
    Image illustration = new Image("images/two-factor-locks.svg", "");
    illustration.addClassName("two-factor-introduction-art");
    illustration.getElement().setAttribute("aria-hidden", "true");
    H2 heading = new H2(messageProperties.getTwoFactorRequiredTitle());
    heading.setId("two-factor-introduction-title");
    heading.addClassName("two-factor-introduction-title");
    stepContentLayout.add(illustration, heading);
    addHint(messageProperties.getTwoFactorRequiredHint());
    if (!twoFactorEnrollment.available()) {
      addHint(messageProperties.getTwoFactorUnavailable());
    }
    cancelTwoFactorButton.setText(messageProperties.getBaseLogoutButton());
    cancelTwoFactorButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    cancelTwoFactorButton.addClassName(DELETE_BUTTON);
    continueTwoFactorButton.setVisible(true);
    continueTwoFactorButton.setEnabled(twoFactorEnrollment.available());
    continueTwoFactorButton.setText(messageProperties.getTwoFactorBeginSetup());
    continueTwoFactorButton.setPrefixComponent(AppIcon.SHIELD_CHECK.create());
  }

  private void showPending() {
    continueTwoFactorButton.setVisible(true);
    switch (pendingTwoFactorChallenge.kind()) {
      case GOOGLE_EMAIL -> {
        addHint(messageProperties.getTwoFactorEmailCodeHint());
        stepContentLayout.add(prepareCode(messageProperties.getTwoFactorEmailCodeLabel(), 8));
        additionalProofField.getElement().setAttribute(INPUT_MODE_ATTRIBUTE, "numeric");
        continueTwoFactorButton.setText(messageProperties.getTwoFactorConfirm());
      }
      case ENROLLMENT -> showEnrollment();
      case RECOVERY_OUTPUT -> showRecoveryCodes();
      default -> throw new IllegalStateException("Unexpected setup phase");
    }
  }

  private void showEnrollment() {
    twoFactorPanel.addClassName("two-factor-setup");
    twoFactorPanel.setIcon(AppIcon.SHIELD_KEYHOLE.create());
    twoFactorPanel.setHeading(
        messageProperties.getTwoFactorSetupTitle(),
        messageProperties.getTwoFactorSetupSubtitle(),
        "");
    Image qr =
        new Image(
            documentPath("setup/" + pendingTwoFactorChallenge.id() + "/qr.png"),
            messageProperties.getTwoFactorQrAlt());
    qr.setId("two-factor-qr");
    qr.addClassName("two-factor-qr");
    Span scanHint = new Span(messageProperties.getTwoFactorScanHint());
    scanHint.addClassNames(PANEL_DESCRIPTION_CLASS, "two-factor-qr-hint");
    VerticalLayout qrContent = new VerticalLayout(qr, scanHint);
    qrContent.setPadding(false);
    qrContent.setSpacing(false);
    qrContent.addClassName("two-factor-qr-content");
    TextArea manual = new TextArea();
    manual.setId("two-factor-manual-key");
    manual.setReadOnly(true);
    manual.setWidthFull();
    manual.setMaxWidth("100%");
    manual.setMinRows(2);
    manual.setMaxRows(2);
    manual.setAutocomplete(Autocomplete.OFF);
    manual.addClassName("two-factor-manual-input");
    manual.setLabel(messageProperties.getTwoFactorManualLabel());
    Span manualHint = new Span(messageProperties.getTwoFactorManualHint());
    manualHint.addClassNames(PANEL_DESCRIPTION_CLASS, "two-factor-manual-hint");
    Button copyKey = new Button(AppIcon.CLIPBOARD_COPY.create());
    copyKey.setId("two-factor-copy-key");
    copyKey.setAriaLabel(messageProperties.getProviderSettingsCopy());
    copyKey.setTooltipText(messageProperties.getProviderSettingsCopy());
    copyKey.setEnabled(false);
    copyKey.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    copyKey.addClickListener(event ->
        copyKey
            .getElement()
            .executeJs(COPY_MANUAL_KEY_SCRIPT, manual.getElement())
            .then(
                Boolean.class,
                copied ->
                    NotificationHelper.getInstance()
                        .showNotification(
                            Boolean.TRUE.equals(copied)
                                ? messageProperties::getTwoFactorManualCopied
                                : messageProperties::getTwoFactorManualCopyFailed,
                            Boolean.TRUE.equals(copied)
                                ? NotificationHelper.NotificationType.SUCCESS
                                : NotificationHelper.NotificationType.ERROR)));
    manual.setSuffixComponent(copyKey);
    VerticalLayout manualContent = new VerticalLayout(manualHint, manual);
    manualContent.setPadding(false);
    manualContent.setSpacing(false);
    manualContent.addClassName("two-factor-manual-content");
    manual.addAttachListener(event -> {
      if (manualSetupSelected) {
        loadManualKey(manual, copyKey);
      }
    });
    Tab qrTab = new Tab(AppIcon.QR_CODE.create(), new Span(messageProperties.getTwoFactorQrTab()));
    Tab manualTab =
        new Tab(AppIcon.KEY.create(), new Span(messageProperties.getTwoFactorManualTab()));
    qrTab.setId("two-factor-qr-tab");
    manualTab.setId("two-factor-manual-tab");
    qrContent.setId("two-factor-qr-pane");
    manualContent.setId("two-factor-manual-pane");
    qrTab.getElement().setAttribute(ARIA_CONTROLS, "two-factor-qr-pane");
    manualTab.getElement().setAttribute(ARIA_CONTROLS, "two-factor-manual-pane");
    qrContent.getElement().setAttribute("role", TABPANEL_ROLE);
    qrContent.getElement().setAttribute(ARIA_LABELLED_BY, "two-factor-qr-tab");
    manualContent.getElement().setAttribute("role", TABPANEL_ROLE);
    manualContent.getElement().setAttribute(ARIA_LABELLED_BY, "two-factor-manual-tab");
    manualContent.setVisible(manualSetupSelected);
    qrContent.setVisible(!manualSetupSelected);
    Tabs selector = new Tabs(qrTab, manualTab);
    selector.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
    selector.addClassName("two-factor-method-tabs");
    selector.setWidthFull();
    selector.setSelectedTab(manualSetupSelected ? manualTab : qrTab);
    selector.addSelectedChangeListener(event -> {
      boolean manualSelected = event.getSelectedTab() == manualTab;
      manualSetupSelected = manualSelected;
      qrContent.setVisible(!manualSelected);
      manualContent.setVisible(manualSelected);
      if (manualSelected) {
        loadManualKey(manual, copyKey);
      }
    });
    Div methods = new Div(selector, qrContent, manualContent);
    methods.setId("two-factor-setup-methods");
    methods.addClassName("two-factor-methods");
    methods.setWidthFull();

    HorizontalLayout apps =
        new HorizontalLayout(
            authenticatorLink(
                "https://support.google.com/accounts/answer/1066447", "Google Authenticator"),
            authenticatorLink(
                "https://support.microsoft.com/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a",
                "Microsoft Authenticator"),
            authenticatorLink("https://2fast-app.de/", "2fast (Windows)"));
    apps.setPadding(false);
    apps.setSpacing(false);
    apps.addClassName("two-factor-app-links");
    Span appHint = new Span(messageProperties.getTwoFactorAppHint());
    appHint.addClassName(PANEL_DESCRIPTION_CLASS);
    HorizontalLayout compatibility =
        new HorizontalLayout(
            AppIcon.INFO_CIRCLE.create(),
            new Span(messageProperties.getTwoFactorAppCompatibility()));
    compatibility.setPadding(false);
    compatibility.setSpacing(false);
    compatibility.setAlignItems(Alignment.CENTER);
    compatibility.addClassName("two-factor-app-compatibility");
    VerticalLayout firstStep =
        new VerticalLayout(
            setupStep(AppIcon.NUMBER_ONE, messageProperties.getTwoFactorAppStep()),
            appHint,
            apps,
            compatibility);
    firstStep.setPadding(false);
    firstStep.setSpacing(false);
    firstStep.addClassName("two-factor-setup-step");
    enrollmentOtpField.setLabel(messageProperties.getTwoFactorSetupCodeLabel());
    Span codeHint = new Span(messageProperties.getTwoFactorSetupCodeHint());
    codeHint.addClassName(PANEL_DESCRIPTION_CLASS);
    VerticalLayout secondStep =
        new VerticalLayout(
            setupStep(AppIcon.NUMBER_TWO, messageProperties.getTwoFactorCodeStep()),
            codeHint,
            enrollmentOtpField);
    secondStep.setPadding(false);
    secondStep.setSpacing(false);
    secondStep.addClassName("two-factor-setup-step");
    VerticalLayout instructions = new VerticalLayout(firstStep, secondStep);
    instructions.setPadding(false);
    instructions.setSpacing(false);
    instructions.addClassName("two-factor-setup-instructions");
    Div layout = new Div(methods, instructions);
    layout.addClassName("two-factor-setup-layout");
    stepContentLayout.add(layout);
    continueTwoFactorButton.setText(messageProperties.getTwoFactorEnable());
  }

  private Anchor authenticatorLink(String url, String caption) {
    Anchor link = new Anchor(url, "");
    link.add(AppIcon.EXTERNAL_LINK_TABLER.create(), new Span(caption));
    link.setTarget("_blank");
    link.getElement().setAttribute("rel", "noopener noreferrer");
    return link;
  }

  /**
   * Lädt den Seed nur in die öffentliche Eingabe des nativen schreibgeschützten Felds. Host-value
   * und Flow-Modell bleiben leer, damit der Seed nicht in der JDBC-Sitzung landet.
   */
  private void loadManualKey(TextArea field, Button copy) {
    copy.setEnabled(false);
    field
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
              // Vierergruppen erleichtern das Ablesen. Der Hostwert wird bewusst nicht gesetzt.
              const groups = key.match(/.{1,4}/g);
              input.value = groups.slice(0, 4).join(' ') + '\\n' + groups.slice(4).join(' ');
              return true;
            } catch {
              return false;
            }
            })();
            """,
            documentPath("setup/" + pendingTwoFactorChallenge.id() + "/key"))
        .then(
            Boolean.class,
            loaded -> {
              if (loaded == null) {
                return;
              }
              boolean available = Boolean.TRUE.equals(loaded);
              field.setInvalid(!available);
              copy.setEnabled(available);
              if (!available) {
                NotificationHelper.getInstance()
                    .showNotification(
                        messageProperties::getTwoFactorManualLoadFailed,
                        NotificationHelper.NotificationType.ERROR);
              }
            });
  }

  private Span setupStep(AppIcon icon, String caption) {
    Span label = new Span(icon.create(), new Span(caption));
    label.addClassName("two-factor-step-label");
    return label;
  }

  private void showRecoveryCodes() {
    cancelTwoFactorButton.setVisible(false);
    twoFactorPanel.addClassName("two-factor-recovery");
    twoFactorPanel.setHeading(
        messageProperties.getTwoFactorRecoveryTitle(),
        messageProperties.getTwoFactorRecoverySubtitle(),
        "");
    UUID outputId = pendingTwoFactorChallenge.id();
    if (!outputId.equals(recoveryOutputId)) {
      recoveryOutputId = outputId;
      confirmRecoveryCodesSavedSwitch.setValue(false);
      recoveryCodesVisible = true;
    }
    Image illustration = new Image("images/two-factor-vault.svg", "");
    illustration.addClassName("two-factor-recovery-art");
    illustration.getElement().setAttribute("aria-hidden", "true");
    VerticalLayout explanation =
        new VerticalLayout(
            illustration,
            recoveryHint(
                AppIcon.NUMBER_ONE,
                messageProperties.getTwoFactorRecoveryOnceTitle(),
                messageProperties.getTwoFactorRecoveryOnceHint()),
            recoveryHint(
                AppIcon.NUMBER_TWO,
                messageProperties.getTwoFactorRecoveryRestoreTitle(),
                messageProperties.getTwoFactorRecoveryRestoreHint()),
            recoveryHint(
                AppIcon.NUMBER_THREE,
                messageProperties.getTwoFactorRecoveryStorageTitle(),
                messageProperties.getTwoFactorRecoveryStorage()));
    explanation.setPadding(false);
    explanation.setSpacing(false);
    explanation.addClassName("two-factor-recovery-explanation");
    VerticalLayout codeFields = new VerticalLayout();
    codeFields.setId("two-factor-recovery-codes");
    codeFields.setPadding(false);
    codeFields.setSpacing(false);
    codeFields.setWidthFull();
    codeFields.addClassName("two-factor-recovery-code-fields");
    List<Button> copyButtons = new ArrayList<>();
    for (int index = 1; index <= 10; index++) {
      TextField field = new TextField();
      field.setId("two-factor-recovery-code-" + index);
      field.setAriaLabel(messageProperties.getTwoFactorRecoveryCodeLabel() + " " + index);
      field.setReadOnly(true);
      field.setWidthFull();
      field.setAutocomplete(Autocomplete.OFF);
      field.addClassName("two-factor-recovery-code");
      Button copyCode = new Button(AppIcon.CLIPBOARD_COPY.create());
      copyCode.setAriaLabel(
          messageProperties.getProviderSettingsCopy() + ": " + field.getAriaLabel().orElseThrow());
      copyCode.setTooltipText(messageProperties.getProviderSettingsCopy());
      copyCode.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      copyCode.addClassName("two-factor-recovery-copy");
      copyCode.setEnabled(false);
      copyCode.addClickListener(event ->
          copyCode
              .getElement()
              .executeJs(
                  """
                  const value = $0.inputElement?.value;
                  return value && navigator.clipboard
                    ? navigator.clipboard.writeText(value).then(() => true, () => false) : false;
                  """,
                  field.getElement())
              .then(
                  Boolean.class,
                  copied ->
                      showRecoveryCopyResult(copied, messageProperties::getTwoFactorCodeCopied)));
      field.setSuffixComponent(copyCode);
      copyButtons.add(copyCode);
      codeFields.add(field);
    }
    Details codes = new Details("", codeFields);
    codes.setId("two-factor-recovery-details");
    codes.setWidthFull();
    codes.setOpened(recoveryCodesVisible);
    Span codesHeading =
        new Span(
            AppIcon.KEY.create(), new Span(messageProperties.getTwoFactorRecoveryCodesSummary()));
    codesHeading.setId("two-factor-recovery-heading");
    codesHeading.addClassName("two-factor-recovery-heading");
    Button toggle =
        detailsToggle(
            codes,
            "two-factor-recovery-toggle",
            messageProperties.getTwoFactorRecoveryCodesSummary());
    codes.addOpenedChangeListener(event -> recoveryCodesVisible = event.isOpened());
    codes.getElement().setAttribute(ARIA_LABELLED_BY, codesHeading.getId().orElseThrow());
    HorizontalLayout codesHeader = new HorizontalLayout(codesHeading, toggle);
    codesHeader.setPadding(false);
    codesHeader.setSpacing(false);
    codesHeader.setWidthFull();
    codesHeader.setAlignItems(Alignment.CENTER);
    codesHeader.addClassName("two-factor-recovery-codes-header");
    VerticalLayout codesSection = new VerticalLayout(codesHeader, codes);
    codesSection.setPadding(false);
    codesSection.setSpacing(false);
    Button copy =
        new Button(messageProperties.getTwoFactorCopyCodes(), AppIcon.CLIPBOARD_COPY.create());
    copy.setId("two-factor-copy-codes");
    copy.setEnabled(false);
    codeFields.addAttachListener(event -> loadRecoveryCodes(codeFields, copyButtons, copy));
    copy.addClickListener(event ->
        copy.getElement()
            .executeJs(
                """
                const values = Array.from($0.querySelectorAll('vaadin-text-field'), field => field.inputElement?.value || '');
                return values.length === 10 && values.every(Boolean) && navigator.clipboard
                  ? navigator.clipboard.writeText(values.join('\\n')).then(() => true, () => false) : false;
                """,
                codeFields.getElement())
            .then(
                Boolean.class,
                copied ->
                    showRecoveryCopyResult(copied, messageProperties::getTwoFactorCopySuccess)));
    Locale locale = VaadinSession.getCurrent().getLocale();
    Button download =
        new Button(messageProperties.getTwoFactorDownloadPdf(), AppIcon.DOWNLOAD.create());
    download
        .getElement()
        .setAttribute("data-download-url", recoveryCodeDownload.handler(outputId, locale));
    recoveryCodeDownload.configure(
        download,
        () ->
            NotificationHelper.getInstance()
                .showNotification(
                    messageProperties::getTwoFactorDownloadFailed,
                    NotificationHelper.NotificationType.ERROR));
    download.setId("two-factor-download-pdf");
    copy.setWidthFull();
    download.setWidthFull();
    VerticalLayout actions = new VerticalLayout(copy, download);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.addClassName("two-factor-recovery-actions");
    explanation.add(actions);
    confirmRecoveryCodesSavedSwitch.setLabel(
        messageProperties.getTwoFactorCodesSavedConfirmation());
    confirmRecoveryCodesSavedSwitch.addThemeVariants(SwitchVariant.AURA_REVERSE);
    VerticalLayout controls = new VerticalLayout(codesSection, confirmRecoveryCodesSavedSwitch);
    controls.setPadding(false);
    controls.setSpacing(false);
    controls.addClassName("two-factor-recovery-controls");
    Div layout = new Div(explanation, controls);
    layout.addClassName("two-factor-recovery-layout");
    stepContentLayout.add(layout);
    continueTwoFactorButton.setText(messageProperties.getTwoFactorCodesSaved());
    updateRecoveryActions();
  }

  /**
   * Wie beim manuellen Schlüssel bleiben Klartextwerte außerhalb des gespeicherten Flow-Modells.
   */
  private void loadRecoveryCodes(VerticalLayout fields, List<Button> copies, Button copyAll) {
    fields
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
            documentPath("recovery/" + pendingTwoFactorChallenge.id() + "/codes"))
        .then(
            Boolean.class,
            loaded -> {
              if (loaded == null) {
                return;
              }
              boolean available = Boolean.TRUE.equals(loaded);
              copies.forEach(button -> button.setEnabled(available));
              copyAll.setEnabled(available);
              if (!available) {
                NotificationHelper.getInstance()
                    .showNotification(
                        messageProperties::getTwoFactorRecoveryLoadFailed,
                        NotificationHelper.NotificationType.ERROR);
              }
            });
  }

  private void showRecoveryCopyResult(Boolean copied, Supplier<String> success) {
    NotificationHelper.getInstance()
        .showNotification(
            Boolean.TRUE.equals(copied) ? success : messageProperties::getTwoFactorCopyFailed,
            Boolean.TRUE.equals(copied)
                ? NotificationHelper.NotificationType.SUCCESS
                : NotificationHelper.NotificationType.ERROR);
  }

  private HorizontalLayout recoveryHint(AppIcon icon, String title, String description) {
    Span heading = new Span(title);
    heading.addClassName("two-factor-recovery-hint-title");
    Span hint = new Span(description);
    hint.addClassName(PANEL_DESCRIPTION_CLASS);
    VerticalLayout copy = new VerticalLayout(heading, hint);
    copy.setPadding(false);
    copy.setSpacing(false);
    HorizontalLayout row = new HorizontalLayout(icon.create(), copy);
    row.setPadding(false);
    row.setSpacing(false);
    row.addClassName("two-factor-recovery-hint");
    return row;
  }

  private void updateRecoveryActions() {
    if (pendingTwoFactorChallenge != null
        && pendingTwoFactorChallenge.kind() == Kind.RECOVERY_OUTPUT) {
      continueTwoFactorButton.setEnabled(confirmRecoveryCodesSavedSwitch.getValue());
    }
  }

  private TextField prepareRecoveryCode() {
    additionalProofField.setLabel(messageProperties.getTwoFactorRecoveryCodeLabel());
    additionalProofField.getElement().removeAttribute(INPUT_MODE_ATTRIBUTE);
    TwoFactorCodeFields.configureRecoveryCodeField(additionalProofField);
    return additionalProofField;
  }

  private TextField prepareCode(String label, int maximumLength) {
    additionalProofField.removeClassName("two-factor-recovery-input");
    additionalProofField.setLabel(label);
    additionalProofField.setPlaceholder(messageProperties.getTwoFactorCodePlaceholder());
    additionalProofField.setMaxLength(maximumLength);
    additionalProofField.getElement().setAttribute(AUTOCOMPLETE_ATTRIBUTE, "one-time-code");
    additionalProofField.getElement().removeAttribute(INPUT_MODE_ATTRIBUTE);
    return additionalProofField;
  }

  private void submitTwoFactorForm() {
    try {
      submitCurrentStep();
    } catch (RuntimeException failure) {
      invalidateCurrentInput();
      showTwoFactorFailure(failure);
    } finally {
      passwordField.clear();
      additionalProofField.clear();
      enrollmentOtpField.clear();
      confirmationOtpField.clear();
    }
  }

  private void submitCurrentStep() {
    if (showingSetupIntroduction) {
      if (twoFactorEnrollment.canBeginFromPasswordLogin(httpRequest)) {
        twoFactorEnrollment.beginFromPasswordLogin(httpRequest);
      }
      httpRequest.getSession().setAttribute(TwoFactorFlow.INTRO_ACCEPTED, true);
      renderCurrentTwoFactorStep();
      return;
    }
    if (pendingTwoFactorChallenge == null) {
      submitWithoutPending();
      return;
    }
    switch (pendingTwoFactorChallenge.kind()) {
      case RECOVERY_OUTPUT -> submitRecoveryOutput();
      case ENROLLMENT -> submitEnrollmentCode();
      case GOOGLE_EMAIL, ACTION -> {
        if (!ValidationHelper.validateRequiredInputs(
            List.of(additionalProofField), messageProperties)) {
          return;
        }
        if (pendingTwoFactorChallenge.kind() == Kind.GOOGLE_EMAIL) {
          twoFactorEnrollment.confirmEmail(
              httpRequest, pendingTwoFactorChallenge.id(), additionalProofField.getValue());
        } else {
          twoFactorEnrollment.complete(
              httpRequest, pendingTwoFactorChallenge.id(), additionalProofField.getValue());
        }
        renderCurrentTwoFactorStep();
      }
    }
  }

  private void submitRecoveryOutput() {
    if (Boolean.TRUE.equals(confirmRecoveryCodesSavedSwitch.getValue())) {
      twoFactorEnrollment.dismissOutput(httpRequest, pendingTwoFactorChallenge.id());
      finish();
    }
  }

  private void submitEnrollmentCode() {
    if (ValidationHelper.validateRequiredInputs(List.of(enrollmentOtpField), messageProperties)) {
      twoFactorEnrollment.complete(
          httpRequest, pendingTwoFactorChallenge.id(), enrollmentOtpField.getValue());
      renderCurrentTwoFactorStep();
    }
  }

  private void submitWithoutPending() {
    if (initialSetupMode) {
      if (ValidationHelper.validateRequiredInputs(List.of(passwordField), messageProperties)) {
        twoFactorEnrollment.beginPassword(httpRequest, passwordField.getValue());
        renderCurrentTwoFactorStep();
      }
      return;
    }
    if (recoveryMode) {
      if (ValidationHelper.validateRequiredInputs(
          List.of(additionalProofField), messageProperties)) {
        twoFactorEnrollment.beginRecovery(httpRequest, additionalProofField.getValue());
        renderCurrentTwoFactorStep();
      }
      return;
    }
    if (recoveryLoginSelected) {
      if (ValidationHelper.validateRequiredInputs(
          List.of(additionalProofField), messageProperties)) {
        twoFactorAuthorization.confirmSession(httpRequest, additionalProofField.getValue());
        finish();
      }
    } else if (ValidationHelper.validateRequiredInputs(
        List.of(confirmationOtpField), messageProperties)) {
      twoFactorAuthorization.confirmSession(httpRequest, confirmationOtpField.getValue());
      finish();
    }
  }

  private void invalidateCurrentInput() {
    if (initialSetupMode) {
      passwordField.setInvalid(true);
    } else if (pendingTwoFactorChallenge != null
        && pendingTwoFactorChallenge.kind() == Kind.ENROLLMENT) {
      enrollmentOtpField.setInvalid(true);
    } else if (pendingTwoFactorChallenge == null && !recoveryMode && !recoveryLoginSelected) {
      confirmationOtpField.setInvalid(true);
    } else {
      additionalProofField.setInvalid(true);
    }
  }

  private void showTwoFactorFailure(RuntimeException failure) {
    Supplier<String> message = messageProperties::getTwoFactorFailed;
    if (failure instanceof LoginThrottledException) {
      message = messageProperties::getLoginThrottledMessage;
    } else if (failure instanceof LockedException) {
      message = messageProperties::getTwoFactorThrottled;
    } else if (failure instanceof BadCredentialsException) {
      message =
          initialSetupMode
              ? messageProperties::getAccountSecurityWrongPassword
              : messageProperties::getTwoFactorInvalidCode;
    }
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            message,
            NotificationHelper.NotificationType.ERROR);
  }

  private void addHint(String value) {
    Span hint = new Span(value);
    hint.addClassName(PANEL_DESCRIPTION_CLASS);
    stepContentLayout.add(hint);
  }

  private String documentPath(String suffix) {
    return httpRequest.getContextPath() + TwoFactorDocuments.PATH + "/" + suffix;
  }

  private void cancel() {
    if (!twoFactorEnrollment.cancelPreparation(httpRequest)) {
      renderCurrentTwoFactorStep();
      return;
    }
    externalLoginFlow.cancel(httpRequest);
    TwoFactorFlow.finish(httpRequest);
    if (twoFactorFlow.account() && twoFactorAuthorization.sessionConfirmed(httpRequest)) {
      UI.getCurrent()
          .getPage()
          .setLocation(httpRequest.getContextPath() + AppRouteConstants.ACCOUNT_PORTAL_LOCATION);
    } else {
      securityService.logout();
      UI.getCurrent().getPage().setLocation(httpRequest.getContextPath() + "/login");
    }
  }

  private void finish() {
    twoFactorAuthorization.requireSession(httpRequest);
    TwoFactorFlow.finish(httpRequest);
    String target = AppRouteConstants.ACCOUNT_PORTAL_LOCATION;
    if (!twoFactorFlow.account() && !recoveryMode) {
      boolean administrative =
          twoFactorAuthorization.owner(httpRequest).roles().stream()
              .anyMatch(RoleType::isAdministrative);
      target = administrative ? "/admin" : "/home";
    }
    UI.getCurrent().getPage().setLocation(httpRequest.getContextPath() + target);
  }

  private void moveTo(TwoFactorFlow next) {
    next.enter(httpRequest);
    UI.getCurrent().getPage().setLocation(httpRequest.getContextPath() + next.location());
  }

  @Override
  public String getPageTitle() {
    return messageProperties.getTwoFactorPageTitle();
  }
}
