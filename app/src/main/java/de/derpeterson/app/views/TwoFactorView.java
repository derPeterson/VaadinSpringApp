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
  private final transient MessageProperties texts;
  private final transient SecurityService security;
  private final transient TwoFactorAuthorization authorization;
  private final transient TwoFactorEnrollment enrollment;
  private final transient ExternalLoginFlow external;
  private final transient ExternalIdentityService identities;
  private final transient HttpServletRequest request;
  private final transient RecoveryCodeDownload recoveryDownloads;
  private final PublicPageComponent page;
  private final AccountPanelComponent panel =
      new AccountPanelComponent("two-factor-panel", AppIcon.LOCK);
  private final Div content = new Div();
  private final Div extraActions = new Div();
  private final HorizontalLayout footerNavigation = new HorizontalLayout();
  private final Button cancel = new Button();
  private final Button submit = new Button();
  private final PasswordField password = new PasswordField();
  private final TextField code = new TextField();
  private final OtpField enrollmentCode = new OtpField();
  private final OtpField confirmationCode = new OtpField();
  private final Switch recoverySaved = new Switch();
  private UUID recoveryOutputId;
  private TwoFactorEnrollment.Pending pending;
  private TwoFactorFlow flow;
  private boolean recovering;
  private boolean initializing;
  private boolean introduction;
  private boolean manualSetupSelected;
  private boolean recoveryCodesVisible;
  private boolean recoveryLoginSelected;

  @Autowired
  public TwoFactorView(
      ConfigService config,
      MessageProperties texts,
      SecurityService security,
      TwoFactorAuthorization authorization,
      TwoFactorEnrollment enrollment,
      ExternalLoginFlow external,
      ExternalIdentityService identities,
      RecoveryCodeDownload recoveryDownloads,
      HttpServletRequest request) {
    this.texts = texts;
    this.security = security;
    this.authorization = authorization;
    this.enrollment = enrollment;
    this.external = external;
    this.identities = identities;
    this.request = request;
    this.recoveryDownloads = recoveryDownloads;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    addClassName("two-factor-view");
    page = new PublicPageComponent(config, texts, PublicPageComponent.Page.TWO_FACTOR);
    content.addClassName("two-factor-content");
    extraActions.addClassName("two-factor-extra-actions");
    panel.getBody().add(content, extraActions);
    password.setId("two-factor-password");
    password.setWidthFull();
    password.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    password.setManualValidation(true);
    password.setRequiredIndicatorVisible(true);
    code.setId("two-factor-code");
    code.setWidthFull();
    code.setManualValidation(true);
    code.setRequiredIndicatorVisible(true);
    code.getElement().setAttribute(AUTOCOMPLETE_ATTRIBUTE, "one-time-code");
    configureOtp(enrollmentCode, "two-factor-enrollment-code");
    configureOtp(confirmationCode, "two-factor-code");
    recoverySaved.setId("two-factor-codes-saved");
    recoverySaved.addValueChangeListener(event -> updateRecoveryActions());
    cancel.setId("two-factor-cancel");
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    cancel.addClickListener(event -> cancel());
    submit.setId("two-factor-submit");
    submit.setPrefixComponent(AppIcon.CHECK.create());
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submit.addClickListener(event -> submit());
    HorizontalLayout actions = new HorizontalLayout(cancel, submit);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configure(actions);
    footerNavigation.setPadding(false);
    footerNavigation.setSpacing(false);
    footerNavigation.addClassName("two-factor-footer-navigation");
    HorizontalLayout footer = new HorizontalLayout(footerNavigation, actions);
    footer.setPadding(false);
    footer.setSpacing(false);
    footer.setWidthFull();
    footer.setAlignItems(Alignment.CENTER);
    footer.addClassName("two-factor-footer");
    panel.addActionFooter(footer);
    page.getCardContainer().add(panel);
    add(page);
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> render());
  }

  private void configureOtp(OtpField field, String id) {
    field.setId(id);
    TwoFactorCodeFields.configureOtp(field, request.getContextPath());
    field.addCompleteListener(
        event -> {
          if (submit.isVisible()
              && submit.isEnabled()
              && TwoFactorCodeFields.isCurrentCompleteOtp(field, event)) {
            submit();
          }
        });
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (security.getAuthenticatedUser(request).isEmpty()) {
      event.forwardTo(LoginView.class);
      return;
    }
    flow = TwoFactorFlow.current(request);
    if (flow == null) {
      Map<String, List<String>> query = event.getLocation().getQueryParameters().getParameters();
      boolean confirmed = authorization.sessionConfirmed(request);
      if (query.containsKey("recover")) {
        flow = confirmed ? TwoFactorFlow.ACCOUNT_RECOVERY : TwoFactorFlow.RECOVERY;
      } else if (query.containsKey("administration")) {
        flow = TwoFactorFlow.ADMINISTRATION;
      } else {
        flow = confirmed && query.containsKey("setup") ? TwoFactorFlow.SETUP : TwoFactorFlow.LOGIN;
      }
      flow.enter(request);
    }
    if (flow.help()) {
      event.forwardTo(RecoveryHelpView.class);
      return;
    }
    page.setAccountSecurity(flow.account(), security.getCurrentUser(request).orElse(null));
    recovering = flow.recovery();
    render();
  }

  public boolean isCurrent(TwoFactorFlow active) {
    return flow == active;
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    ExternalProofNotice.show(event, texts);
  }

  private void render() {
    if (security.getAuthenticatedUser(request).isEmpty()) {
      return;
    }
    content.removeAll();
    extraActions.removeAll();
    footerNavigation.removeAll();
    password.clear();
    code.clear();
    enrollmentCode.clear();
    confirmationCode.clear();
    password.setInvalid(false);
    code.setInvalid(false);
    enrollmentCode.setInvalid(false);
    confirmationCode.setInvalid(false);
    pending = enrollment.pending(request);
    introduction = false;
    panel.removeClassName("two-factor-introduction");
    panel.removeClassName("two-factor-setup");
    panel.removeClassName("two-factor-recovery");
    panel.removeClassName("two-factor-confirmation");
    panel.removeClassName("two-factor-restore");
    cancel.removeClassName(DELETE_BUTTON);
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    submit.setPrefixComponent(AppIcon.CHECK.create());
    recovering = recovering || pending != null && pending.proof() == SetupProof.RECOVERY_CODE;
    TwoFactorAuthorization.State state = authorization.state(authorization.owner(request));
    initializing = pending == null && !state.enrolled();
    boolean startsRecovery = pending == null && state.enrolled() && recovering;
    panel.setIcon((startsRecovery ? AppIcon.LOCK_RESET : AppIcon.LOCK).create());
    panel.setHeading(
        initializing ? texts.getTwoFactorSetupTitle() : texts.getTwoFactorConfirmTitle(),
        texts.getTwoFactorPageHint(),
        "");
    cancel.setText(
        flow.account() && authorization.sessionConfirmed(request)
            ? texts.getTwoFactorBack()
            : texts.getBaseLogoutButton());
    if (!flow.account() || !authorization.sessionConfirmed(request)) {
      cancel.addClassName(DELETE_BUTTON);
      cancel.setPrefixComponent(AppIcon.SIGN_OUT.create());
    }
    submit.setEnabled(true);
    cancel.setEnabled(true);
    cancel.setVisible(true);
    if (pending != null) {
      showPending();
    } else if (startsRecovery) {
      showRecovery();
    } else if (state.enrolled()) {
      showExistingFactor();
    } else {
      showSetup();
    }
  }

  private void showExistingFactor() {
    if (flow.administration()
        ? authorization.administrationConfirmed(request)
        : authorization.sessionConfirmed(request)) {
      finish();
      return;
    }
    showLoginMethods();
    Button recover = new Button(texts.getTwoFactorRecoverAction(), AppIcon.OPEN_SECTION.create());
    recover.setIconAfterText(true);
    recover.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    recover.setId("two-factor-recover");
    recover.addClickListener(
        event ->
            moveTo(flow.administration() ? TwoFactorFlow.ADMIN_RECOVERY : TwoFactorFlow.RECOVERY));
    Button help =
        new Button(
            texts.getSupportHelpTitle(),
            event ->
                moveTo(
                    flow.administration() ? TwoFactorFlow.ADMIN_HELP : TwoFactorFlow.LOGIN_HELP));
    help.setId("two-factor-help");
    help.setIcon(AppIcon.OPEN_SECTION.create());
    help.setIconAfterText(true);
    help.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    Span topic =
        new Span(AppIcon.QUESTION_CIRCLE.create(), new Span(texts.getTwoFactorLoginHelpTitle()));
    topic.addClassName("two-factor-login-help-topic");
    VerticalLayout links =
        new VerticalLayout(
            loginHelpRow(texts.getTwoFactorLoginRestorePrompt(), recover),
            loginHelpRow(texts.getSupportHelpSubtitle(), help));
    links.setPadding(false);
    links.setSpacing(false);
    links.addClassName("two-factor-login-help-links");
    Details alternatives = new Details("", links);
    alternatives.setId("two-factor-login-help");
    alternatives.setWidthFull();
    Button toggle =
        detailsToggle(
            alternatives, "two-factor-login-help-toggle", texts.getTwoFactorLoginHelpTitle());
    toggle.setPrefixComponent(topic);
    toggle.addClassName("two-factor-login-help-toggle");
    extraActions.add(toggle, alternatives);
    submit.setText(
        flow.administration() ? texts.getTwoFactorConfirm() : texts.getBaseLoginButton());
    if (!flow.administration()) {
      submit.setPrefixComponent(AppIcon.SIGN_IN.create());
    }
  }

  private HorizontalLayout loginHelpRow(String question, Button action) {
    HorizontalLayout row = new HorizontalLayout(new Span(question), action);
    row.setAlignItems(Alignment.CENTER);
    row.addClassName("two-factor-help-row");
    return row;
  }

  private Button detailsToggle(Details details, String id, String topic) {
    Button toggle = new DetailsToggleButton(texts, details, topic);
    toggle.setId(id);
    return toggle;
  }

  private void showLoginMethods() {
    panel.addClassName("two-factor-confirmation");
    panel.setIcon(AppIcon.SHIELD_KEYHOLE.create());
    panel.setHeading(texts.getTwoFactorConfirmTitle(), texts.getTwoFactorConfirmSubtitle(), "");
    Tab authenticator =
        new Tab(AppIcon.DEVICES.create(), new Span(texts.getTwoFactorConfirmationAppTab()));
    Tab recovery = new Tab(AppIcon.KEY.create(), new Span(texts.getTwoFactorRecoveryCodeLabel()));
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
    selector.addSelectedChangeListener(
        event -> {
          recoveryLoginSelected = event.getSelectedTab() == recovery;
          showLoginInput(input);
        });
    content.add(selector, input);
    showLoginInput(input);
  }

  private void showLoginInput(VerticalLayout input) {
    input.removeAll();
    code.clear();
    confirmationCode.clear();
    code.setInvalid(false);
    confirmationCode.setInvalid(false);
    input
        .getElement()
        .setAttribute(
            ARIA_LABELLED_BY,
            recoveryLoginSelected ? "two-factor-recovery-tab" : "two-factor-authenticator-tab");
    String hintText =
        recoveryLoginSelected
            ? texts.getTwoFactorLoginRecoveryHint()
            : texts.getTwoFactorLoginHint();
    Span hint = new Span(flow.administration() ? texts.getTwoFactorActionHint() : hintText);
    hint.addClassName(PANEL_DESCRIPTION_CLASS);
    input.add(hint);
    if (recoveryLoginSelected) {
      input.add(prepareRecoveryCode());
    } else {
      confirmationCode.setLabel(texts.getTwoFactorSetupCodeLabel());
      input.add(confirmationCode);
    }
  }

  private void showRecovery() {
    panel.addClassName("two-factor-restore");
    panel.setHeading(texts.getTwoFactorRecoverAction(), texts.getTwoFactorRecoverSubtitle(), "");
    if (!enrollment.available()) {
      addHint(texts.getTwoFactorUnavailable());
    }
    content.add(prepareRecoveryCode());
    submit.setVisible(true);
    submit.setEnabled(enrollment.available());
    submit.setText(texts.getTwoFactorRecoveryContinue());
    HorizontalLayout notice =
        new HorizontalLayout(
            AppIcon.INFO_CIRCLE.create(),
            new Div(
                new Paragraph(texts.getTwoFactorRecoveryPreviousAppHint()),
                new Paragraph(texts.getTwoFactorRecoveryConsumptionHint())));
    notice.setPadding(false);
    notice.setSpacing(false);
    notice.setAlignItems(Alignment.CENTER);
    notice.addClassName("two-factor-restore-notice");
    content.add(notice);
    Button back = new Button(texts.getTwoFactorReturnToCode(), AppIcon.ANGLE_LEFT.create());
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    back.setId("two-factor-recovery-back");
    back.addClickListener(
        event ->
            moveTo(flow.administration() ? TwoFactorFlow.ADMINISTRATION : TwoFactorFlow.LOGIN));
    footerNavigation.add(back);
  }

  private void showSetup() {
    if (!flow.account()
        && !Boolean.TRUE.equals(request.getSession().getAttribute(TwoFactorFlow.INTRO_ACCEPTED))
        && (authorization.requires(request, TwoFactorAction.LOGIN)
            || authorization.requires(request, TwoFactorAction.ADMINISTRATION))) {
      showIntroduction();
      return;
    }
    addHint(
        enrollment.available() ? texts.getTwoFactorSetupHint() : texts.getTwoFactorUnavailable());
    boolean local = external.owner(request).getPassword() != null;
    if (local) {
      password.setLabel(texts.getAccountPasswordDialogCurrentPassword());
      password.setPlaceholder(texts.getAccountPasswordPlaceholder());
      content.add(password);
    }
    submit.setVisible(local);
    submit.setEnabled(enrollment.available());
    submit.setText(texts.getTwoFactorStartSetup());
    for (String provider : identities.connected(authorization.owner(request).id())) {
      if (!availableEnrollmentProvider(provider)) {
        continue;
      }
      extraActions.add(enrollmentProofButton(provider));
    }
    if (!local && extraActions.getComponentCount() == 0) {
      addHint(texts.getExternalProofUnavailable());
    }
  }

  private Button enrollmentProofButton(String provider) {
    boolean google = ExternalProviderSettings.GOOGLE.equals(provider);
    String brand =
        google ? ExternalProviderSettings.GOOGLE_NAME : ExternalProviderSettings.MICROSOFT_NAME;
    Button proof = new Button(texts.getExternalProofWith(brand));
    proof.setId("two-factor-confirm-" + provider);
    Image mark = new Image("icons/external-login/" + provider + (google ? ".png" : ".svg"), "");
    mark.addClassName("external-proof-provider-mark");
    proof.setPrefixComponent(mark);
    proof.setEnabled(enrollment.available());
    proof.addClickListener(
        event -> {
          try {
            UI.getCurrent()
                .getPage()
                .setLocation(
                    external.beginProof(
                        request,
                        provider,
                        ExternalLoginFlow.Purpose.ENROLL_FACTOR,
                        VaadinSession.getCurrent().getLocale()));
          } catch (RuntimeException failure) {
            failure(failure);
          }
        });
    return proof;
  }

  private boolean availableEnrollmentProvider(String provider) {
    if (!external.providers().contains(provider)) {
      return false;
    }
    TwoFactorAuthorization.State state = authorization.state(authorization.owner(request));
    return !ExternalProviderSettings.GOOGLE.equals(provider)
        || !state.previouslyEnrolled()
        || state.operatorEnrollment();
  }

  private void showIntroduction() {
    introduction = true;
    panel.addClassName("two-factor-introduction");
    Image illustration = new Image("images/two-factor-locks.svg", "");
    illustration.addClassName("two-factor-introduction-art");
    illustration.getElement().setAttribute("aria-hidden", "true");
    H2 heading = new H2(texts.getTwoFactorRequiredTitle());
    heading.setId("two-factor-introduction-title");
    heading.addClassName("two-factor-introduction-title");
    content.add(illustration, heading);
    addHint(texts.getTwoFactorRequiredHint());
    if (!enrollment.available()) {
      addHint(texts.getTwoFactorUnavailable());
    }
    cancel.setText(texts.getBaseLogoutButton());
    cancel.setPrefixComponent(AppIcon.SIGN_OUT.create());
    cancel.addClassName(DELETE_BUTTON);
    submit.setVisible(true);
    submit.setEnabled(enrollment.available());
    submit.setText(texts.getTwoFactorBeginSetup());
    submit.setPrefixComponent(AppIcon.SHIELD_CHECK.create());
  }

  private void showPending() {
    submit.setVisible(true);
    switch (pending.kind()) {
      case GOOGLE_EMAIL -> {
        addHint(texts.getTwoFactorEmailCodeHint());
        content.add(prepareCode(texts.getTwoFactorEmailCodeLabel(), 8));
        code.getElement().setAttribute(INPUT_MODE_ATTRIBUTE, "numeric");
        submit.setText(texts.getTwoFactorConfirm());
      }
      case ENROLLMENT -> showEnrollment();
      case RECOVERY_OUTPUT -> showRecoveryCodes();
      default -> throw new IllegalStateException("Unexpected setup phase");
    }
  }

  private void showEnrollment() {
    panel.addClassName("two-factor-setup");
    panel.setIcon(AppIcon.SHIELD_KEYHOLE.create());
    panel.setHeading(texts.getTwoFactorSetupTitle(), texts.getTwoFactorSetupSubtitle(), "");
    Image qr =
        new Image(documentPath("setup/" + pending.id() + "/qr.png"), texts.getTwoFactorQrAlt());
    qr.setId("two-factor-qr");
    qr.addClassName("two-factor-qr");
    Span scanHint = new Span(texts.getTwoFactorScanHint());
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
    manual.setLabel(texts.getTwoFactorManualLabel());
    Span manualHint = new Span(texts.getTwoFactorManualHint());
    manualHint.addClassNames(PANEL_DESCRIPTION_CLASS, "two-factor-manual-hint");
    Button copyKey = new Button(AppIcon.CLIPBOARD_COPY.create());
    copyKey.setId("two-factor-copy-key");
    copyKey.setAriaLabel(texts.getProviderSettingsCopy());
    copyKey.setTooltipText(texts.getProviderSettingsCopy());
    copyKey.setEnabled(false);
    copyKey.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    copyKey.addClickListener(
        event ->
            copyKey
                .getElement()
                .executeJs(COPY_MANUAL_KEY_SCRIPT, manual.getElement())
                .then(
                    Boolean.class,
                    copied ->
                        NotificationHelper.getInstance()
                            .showNotification(
                                Boolean.TRUE.equals(copied)
                                    ? texts::getTwoFactorManualCopied
                                    : texts::getTwoFactorManualCopyFailed,
                                Boolean.TRUE.equals(copied)
                                    ? NotificationHelper.NotificationType.SUCCESS
                                    : NotificationHelper.NotificationType.ERROR)));
    manual.setSuffixComponent(copyKey);
    VerticalLayout manualContent = new VerticalLayout(manualHint, manual);
    manualContent.setPadding(false);
    manualContent.setSpacing(false);
    manualContent.addClassName("two-factor-manual-content");
    manual.addAttachListener(
        event -> {
          if (manualSetupSelected) {
            loadManualKey(manual, copyKey);
          }
        });
    Tab qrTab = new Tab(AppIcon.QR_CODE.create(), new Span(texts.getTwoFactorQrTab()));
    Tab manualTab = new Tab(AppIcon.KEY.create(), new Span(texts.getTwoFactorManualTab()));
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
    selector.addSelectedChangeListener(
        event -> {
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
    Span appHint = new Span(texts.getTwoFactorAppHint());
    appHint.addClassName(PANEL_DESCRIPTION_CLASS);
    HorizontalLayout compatibility =
        new HorizontalLayout(
            AppIcon.INFO_CIRCLE.create(), new Span(texts.getTwoFactorAppCompatibility()));
    compatibility.setPadding(false);
    compatibility.setSpacing(false);
    compatibility.setAlignItems(Alignment.CENTER);
    compatibility.addClassName("two-factor-app-compatibility");
    VerticalLayout firstStep =
        new VerticalLayout(
            setupStep(AppIcon.NUMBER_ONE, texts.getTwoFactorAppStep()),
            appHint,
            apps,
            compatibility);
    firstStep.setPadding(false);
    firstStep.setSpacing(false);
    firstStep.addClassName("two-factor-setup-step");
    enrollmentCode.setLabel(texts.getTwoFactorSetupCodeLabel());
    Span codeHint = new Span(texts.getTwoFactorSetupCodeHint());
    codeHint.addClassName(PANEL_DESCRIPTION_CLASS);
    VerticalLayout secondStep =
        new VerticalLayout(
            setupStep(AppIcon.NUMBER_TWO, texts.getTwoFactorCodeStep()), codeHint, enrollmentCode);
    secondStep.setPadding(false);
    secondStep.setSpacing(false);
    secondStep.addClassName("two-factor-setup-step");
    VerticalLayout instructions = new VerticalLayout(firstStep, secondStep);
    instructions.setPadding(false);
    instructions.setSpacing(false);
    instructions.addClassName("two-factor-setup-instructions");
    Div layout = new Div(methods, instructions);
    layout.addClassName("two-factor-setup-layout");
    content.add(layout);
    submit.setText(texts.getTwoFactorEnable());
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
            documentPath("setup/" + pending.id() + "/key"))
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
                        texts::getTwoFactorManualLoadFailed,
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
    cancel.setVisible(false);
    panel.addClassName("two-factor-recovery");
    panel.setHeading(texts.getTwoFactorRecoveryTitle(), texts.getTwoFactorRecoverySubtitle(), "");
    UUID outputId = pending.id();
    if (!outputId.equals(recoveryOutputId)) {
      recoveryOutputId = outputId;
      recoverySaved.setValue(false);
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
                texts.getTwoFactorRecoveryOnceTitle(),
                texts.getTwoFactorRecoveryOnceHint()),
            recoveryHint(
                AppIcon.NUMBER_TWO,
                texts.getTwoFactorRecoveryRestoreTitle(),
                texts.getTwoFactorRecoveryRestoreHint()),
            recoveryHint(
                AppIcon.NUMBER_THREE,
                texts.getTwoFactorRecoveryStorageTitle(),
                texts.getTwoFactorRecoveryStorage()));
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
      field.setAriaLabel(texts.getTwoFactorRecoveryCodeLabel() + " " + index);
      field.setReadOnly(true);
      field.setWidthFull();
      field.setAutocomplete(Autocomplete.OFF);
      field.addClassName("two-factor-recovery-code");
      Button copyCode = new Button(AppIcon.CLIPBOARD_COPY.create());
      copyCode.setAriaLabel(
          texts.getProviderSettingsCopy() + ": " + field.getAriaLabel().orElseThrow());
      copyCode.setTooltipText(texts.getProviderSettingsCopy());
      copyCode.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      copyCode.addClassName("two-factor-recovery-copy");
      copyCode.setEnabled(false);
      copyCode.addClickListener(
          event ->
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
                      copied -> showRecoveryCopyResult(copied, texts::getTwoFactorCodeCopied)));
      field.setSuffixComponent(copyCode);
      copyButtons.add(copyCode);
      codeFields.add(field);
    }
    Details codes = new Details("", codeFields);
    codes.setId("two-factor-recovery-details");
    codes.setWidthFull();
    codes.setOpened(recoveryCodesVisible);
    Span codesHeading =
        new Span(AppIcon.KEY.create(), new Span(texts.getTwoFactorRecoveryCodesSummary()));
    codesHeading.setId("two-factor-recovery-heading");
    codesHeading.addClassName("two-factor-recovery-heading");
    Button toggle =
        detailsToggle(
            codes, "two-factor-recovery-toggle", texts.getTwoFactorRecoveryCodesSummary());
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
    Button copy = new Button(texts.getTwoFactorCopyCodes(), AppIcon.CLIPBOARD_COPY.create());
    copy.setId("two-factor-copy-codes");
    copy.setEnabled(false);
    codeFields.addAttachListener(event -> loadRecoveryCodes(codeFields, copyButtons, copy));
    copy.addClickListener(
        event ->
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
                    copied -> showRecoveryCopyResult(copied, texts::getTwoFactorCopySuccess)));
    Locale locale = VaadinSession.getCurrent().getLocale();
    Button download = new Button(texts.getTwoFactorDownloadPdf(), AppIcon.DOWNLOAD.create());
    download
        .getElement()
        .setAttribute("data-download-url", recoveryDownloads.handler(outputId, locale));
    recoveryDownloads.configure(
        download,
        () ->
            NotificationHelper.getInstance()
                .showNotification(
                    texts::getTwoFactorDownloadFailed, NotificationHelper.NotificationType.ERROR));
    download.setId("two-factor-download-pdf");
    copy.setWidthFull();
    download.setWidthFull();
    VerticalLayout actions = new VerticalLayout(copy, download);
    actions.setPadding(false);
    actions.setSpacing(false);
    actions.addClassName("two-factor-recovery-actions");
    explanation.add(actions);
    recoverySaved.setLabel(texts.getTwoFactorCodesSavedConfirmation());
    recoverySaved.addThemeVariants(SwitchVariant.AURA_REVERSE);
    VerticalLayout controls = new VerticalLayout(codesSection, recoverySaved);
    controls.setPadding(false);
    controls.setSpacing(false);
    controls.addClassName("two-factor-recovery-controls");
    Div layout = new Div(explanation, controls);
    layout.addClassName("two-factor-recovery-layout");
    content.add(layout);
    submit.setText(texts.getTwoFactorCodesSaved());
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
            documentPath("recovery/" + pending.id() + "/codes"))
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
                        texts::getTwoFactorRecoveryLoadFailed,
                        NotificationHelper.NotificationType.ERROR);
              }
            });
  }

  private void showRecoveryCopyResult(Boolean copied, Supplier<String> success) {
    NotificationHelper.getInstance()
        .showNotification(
            Boolean.TRUE.equals(copied) ? success : texts::getTwoFactorCopyFailed,
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
    if (pending != null && pending.kind() == Kind.RECOVERY_OUTPUT) {
      submit.setEnabled(recoverySaved.getValue());
    }
  }

  private TextField prepareRecoveryCode() {
    code.setLabel(texts.getTwoFactorRecoveryCodeLabel());
    code.getElement().removeAttribute(INPUT_MODE_ATTRIBUTE);
    TwoFactorCodeFields.configureRecoveryCode(code);
    return code;
  }

  private TextField prepareCode(String label, int maximumLength) {
    code.removeClassName("two-factor-recovery-input");
    code.setLabel(label);
    code.setPlaceholder(texts.getTwoFactorCodePlaceholder());
    code.setMaxLength(maximumLength);
    code.getElement().setAttribute(AUTOCOMPLETE_ATTRIBUTE, "one-time-code");
    code.getElement().removeAttribute(INPUT_MODE_ATTRIBUTE);
    return code;
  }

  private void submit() {
    try {
      submitCurrentStep();
    } catch (RuntimeException failure) {
      invalidateCurrentInput();
      failure(failure);
    } finally {
      password.clear();
      code.clear();
      enrollmentCode.clear();
      confirmationCode.clear();
    }
  }

  private void submitCurrentStep() {
    if (introduction) {
      if (enrollment.canBeginFromPasswordLogin(request)) {
        enrollment.beginFromPasswordLogin(request);
      }
      request.getSession().setAttribute(TwoFactorFlow.INTRO_ACCEPTED, true);
      render();
      return;
    }
    if (pending == null) {
      submitWithoutPending();
      return;
    }
    switch (pending.kind()) {
      case RECOVERY_OUTPUT -> submitRecoveryOutput();
      case ENROLLMENT -> submitEnrollmentCode();
      case GOOGLE_EMAIL, ACTION -> {
        if (!ValidationHelper.validateRequiredInputs(List.of(code), texts)) {
          return;
        }
        if (pending.kind() == Kind.GOOGLE_EMAIL) {
          enrollment.confirmEmail(request, pending.id(), code.getValue());
        } else {
          enrollment.complete(request, pending.id(), code.getValue());
        }
        render();
      }
    }
  }

  private void submitRecoveryOutput() {
    if (Boolean.TRUE.equals(recoverySaved.getValue())) {
      enrollment.dismissOutput(request, pending.id());
      finish();
    }
  }

  private void submitEnrollmentCode() {
    if (ValidationHelper.validateRequiredInputs(List.of(enrollmentCode), texts)) {
      enrollment.complete(request, pending.id(), enrollmentCode.getValue());
      render();
    }
  }

  private void submitWithoutPending() {
    if (initializing) {
      if (ValidationHelper.validateRequiredInputs(List.of(password), texts)) {
        enrollment.beginPassword(request, password.getValue());
        render();
      }
      return;
    }
    if (recovering) {
      if (ValidationHelper.validateRequiredInputs(List.of(code), texts)) {
        enrollment.beginRecovery(request, code.getValue());
        render();
      }
      return;
    }
    if (recoveryLoginSelected) {
      if (ValidationHelper.validateRequiredInputs(List.of(code), texts)) {
        authorization.confirmSession(request, code.getValue());
        finish();
      }
    } else if (ValidationHelper.validateRequiredInputs(List.of(confirmationCode), texts)) {
      authorization.confirmSession(request, confirmationCode.getValue());
      finish();
    }
  }

  private void invalidateCurrentInput() {
    if (initializing) {
      password.setInvalid(true);
    } else if (pending != null && pending.kind() == Kind.ENROLLMENT) {
      enrollmentCode.setInvalid(true);
    } else if (pending == null && !recovering && !recoveryLoginSelected) {
      confirmationCode.setInvalid(true);
    } else {
      code.setInvalid(true);
    }
  }

  private void failure(RuntimeException failure) {
    Supplier<String> message = texts::getTwoFactorFailed;
    if (failure instanceof LoginThrottledException) {
      message = texts::getLoginThrottledMessage;
    } else if (failure instanceof LockedException) {
      message = texts::getTwoFactorThrottled;
    } else if (failure instanceof BadCredentialsException) {
      message =
          initializing ? texts::getAccountSecurityWrongPassword : texts::getTwoFactorInvalidCode;
    }
    NotificationHelper.getInstance()
        .showNotification(
            texts::getBaseFailedTitle, message, NotificationHelper.NotificationType.ERROR);
  }

  private void addHint(String value) {
    Span hint = new Span(value);
    hint.addClassName(PANEL_DESCRIPTION_CLASS);
    content.add(hint);
  }

  private String documentPath(String suffix) {
    return request.getContextPath() + TwoFactorDocuments.PATH + "/" + suffix;
  }

  private void cancel() {
    if (!enrollment.cancelPreparation(request)) {
      render();
      return;
    }
    external.cancel(request);
    TwoFactorFlow.finish(request);
    if (flow.account() && authorization.sessionConfirmed(request)) {
      UI.getCurrent()
          .getPage()
          .setLocation(request.getContextPath() + AppRouteConstants.ACCOUNT_PORTAL_LOCATION);
    } else {
      security.logout();
      UI.getCurrent().getPage().setLocation(request.getContextPath() + "/login");
    }
  }

  private void finish() {
    authorization.requireSession(request);
    TwoFactorFlow.finish(request);
    String target = AppRouteConstants.ACCOUNT_PORTAL_LOCATION;
    if (!flow.account() && !recovering) {
      boolean administrative =
          authorization.owner(request).roles().stream().anyMatch(RoleType::isAdministrative);
      target = administrative ? "/admin" : "/home";
    }
    UI.getCurrent().getPage().setLocation(request.getContextPath() + target);
  }

  private void moveTo(TwoFactorFlow next) {
    next.enter(request);
    UI.getCurrent().getPage().setLocation(request.getContextPath() + next.location());
  }

  @Override
  public String getPageTitle() {
    return texts.getTwoFactorPageTitle();
  }
}
