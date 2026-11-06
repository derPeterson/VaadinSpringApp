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
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
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
import de.derpeterson.app.security.LoginThrottledException;
import de.derpeterson.app.security.SecurityService;
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
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
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
    implements BeforeEnterObserver, HasDynamicTitle {
  private final transient MessageProperties texts;
  private final transient SecurityService security;
  private final transient TwoFactorAuthorization authorization;
  private final transient TwoFactorEnrollment enrollment;
  private final transient ExternalLoginFlow external;
  private final transient ExternalIdentityService identities;
  private final transient HttpServletRequest request;
  private final transient RecoveryCodeDownload recoveryDownloads;
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
  private boolean setup;
  private boolean administration;
  private boolean recovering;
  private boolean initializing;
  private boolean introduction;
  private boolean setupStarted;
  private boolean manualSetupSelected;
  private boolean recoveryCodesVisible;
  private boolean recoveryLoginSelected;

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
    PublicPageComponent page =
        new PublicPageComponent(config, texts, PublicPageComponent.Page.TWO_FACTOR);
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
    code.getElement().setAttribute("autocomplete", "one-time-code");
    configureOtp(enrollmentCode, "two-factor-enrollment-code");
    configureOtp(confirmationCode, "two-factor-code");
    confirmationCode.setAutofocus(true);
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
    field.setLength(6);
    field.setPlaceholder("•");
    field.setManualValidation(true);
    field.setRequiredIndicatorVisible(true);
    field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
    field.addClassName("two-factor-otp");
    field.addCompleteListener(
        event -> {
          // Genau sechs ASCII-Ziffern; veraltete Ereignisse nach Leeren oder Methodenwechsel
          // ignorieren.
          if (event.isFromClient()
              && field.isAttached()
              && field.isVisible()
              && submit.isVisible()
              && submit.isEnabled()
              && field.getValue().matches("[0-9]{6}")
              && field.getValue().equals(event.getValue())) {
            submit();
          }
        });
    String otpStylesheetPath = request.getContextPath() + "/custom-theme/cosmos-mfa-otp.css";
    field.addAttachListener(
        event ->
            field
                .getElement()
                .executeJs(
                    """
                const field = this;
                customElements.whenDefined(field.localName).then(async () => {
                  await field.updateComplete;
                  const root = field.shadowRoot;
                  if (!root || root.querySelector('link[data-cosmos-otp]')) { return; }
                  const sheet = document.createElement('link');
                  sheet.rel = 'stylesheet';
                  sheet.href = $0;
                  sheet.dataset.cosmosOtp = '';
                  root.append(sheet);
                });
                """,
                    otpStylesheetPath));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (security.getAuthenticatedUser(request).isEmpty()) {
      event.forwardTo(LoginView.class);
      return;
    }
    setup = event.getLocation().getQueryParameters().getParameters().containsKey("setup");
    administration =
        event.getLocation().getQueryParameters().getParameters().containsKey("administration");
    recovering = event.getLocation().getQueryParameters().getParameters().containsKey("recover");
    render();
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
        authorization.sessionConfirmed(request)
            ? texts.getTwoFactorBack()
            : texts.getBaseLogoutButton());
    if (!authorization.sessionConfirmed(request)) {
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
      if (administration
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
              UI.getCurrent()
                  .getPage()
                  .setLocation(request.getContextPath() + "/two-factor?recover"));
      Button help =
          new Button(
              texts.getSupportHelpTitle(),
              event ->
                  UI.getCurrent()
                      .getPage()
                      .setLocation(request.getContextPath() + "/recovery-help"));
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
      submit.setText(administration ? texts.getTwoFactorConfirm() : texts.getBaseLoginButton());
      if (!administration) {
        submit.setPrefixComponent(AppIcon.SIGN_IN.create());
      }
    } else {
      showSetup();
    }
  }

  private HorizontalLayout loginHelpRow(String question, Button action) {
    HorizontalLayout row = new HorizontalLayout(new Span(question), action);
    row.setAlignItems(Alignment.CENTER);
    row.addClassName("two-factor-login-help-row");
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
    input.setId("two-factor-confirmation-input");
    input.addClassName("two-factor-confirmation-input");
    input.getElement().setAttribute("role", "tabpanel");
    for (Tab tab : List.of(authenticator, recovery)) {
      tab.getElement().setAttribute("aria-controls", "two-factor-confirmation-input");
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
            "aria-labelledby",
            recoveryLoginSelected ? "two-factor-recovery-tab" : "two-factor-authenticator-tab");
    Span hint =
        new Span(
            administration
                ? texts.getTwoFactorActionHint()
                : recoveryLoginSelected
                    ? texts.getTwoFactorLoginRecoveryHint()
                    : texts.getTwoFactorLoginHint());
    hint.addClassName("cosmos-panel-description");
    input.add(hint);
    if (recoveryLoginSelected) {
      input.add(prepareRecoveryCode());
      code.getElement().setAttribute("autocomplete", "off");
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
    code.getElement().setAttribute("autocomplete", "off");
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
        event -> UI.getCurrent().getPage().setLocation(request.getContextPath() + "/two-factor"));
    footerNavigation.add(back);
  }

  private void showSetup() {
    if (!setupStarted
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
      if (!external.providers().contains(provider)) {
        continue;
      }
      TwoFactorAuthorization.State state = authorization.state(authorization.owner(request));
      if (provider.equals("google") && state.previouslyEnrolled() && !state.operatorEnrollment()) {
        continue;
      }
      String brand = provider.equals("google") ? "Google" : "Microsoft";
      Button proof = new Button(texts.getExternalProofWith(brand));
      proof.setId("two-factor-confirm-" + provider);
      Image mark =
          new Image(
              "icons/external-login/" + provider + (provider.equals("google") ? ".png" : ".svg"),
              "");
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
      extraActions.add(proof);
    }
    if (!local && extraActions.getComponentCount() == 0) {
      addHint(texts.getExternalProofUnavailable());
    }
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
        code.getElement().setAttribute("inputmode", "numeric");
        submit.setText(texts.getTwoFactorConfirm());
      }
      case ENROLLMENT -> {
        showEnrollment();
      }
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
    scanHint.addClassNames("cosmos-panel-description", "two-factor-qr-hint");
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
    manualHint.addClassNames("cosmos-panel-description", "two-factor-manual-hint");
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
                .executeJs(
                    """
                const value = $0.inputElement?.value;
                return value && navigator.clipboard
                  ? navigator.clipboard.writeText(value.replaceAll(' ', '').replaceAll('\\n', '')).then(() => true, () => false)
                  : false;
                """,
                    manual.getElement())
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
    qrTab.getElement().setAttribute("aria-controls", "two-factor-qr-pane");
    manualTab.getElement().setAttribute("aria-controls", "two-factor-manual-pane");
    qrContent.getElement().setAttribute("role", "tabpanel");
    qrContent.getElement().setAttribute("aria-labelledby", "two-factor-qr-tab");
    manualContent.getElement().setAttribute("role", "tabpanel");
    manualContent.getElement().setAttribute("aria-labelledby", "two-factor-manual-tab");
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
    appHint.addClassName("cosmos-panel-description");
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
    codeHint.addClassName("cosmos-panel-description");
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
    codes.getElement().setAttribute("aria-labelledby", codesHeading.getId().orElseThrow());
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
        .setAttribute(
            "data-download-url",
            (DownloadHandler) event -> recoveryDownloads.write(event, outputId, locale));
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
    hint.addClassName("cosmos-panel-description");
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
    String example = "XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX";
    TextField field = prepareCode(texts.getTwoFactorRecoveryCodeLabel(), example.length());
    field.setPlaceholder(example);
    field.addClassName("two-factor-recovery-input");
    return field;
  }

  private TextField prepareCode(String label, int maximumLength) {
    code.removeClassName("two-factor-recovery-input");
    code.setLabel(label);
    code.setPlaceholder(texts.getTwoFactorCodePlaceholder());
    code.setMaxLength(maximumLength);
    code.getElement().setAttribute("autocomplete", "one-time-code");
    code.getElement().removeAttribute("inputmode");
    return code;
  }

  private void submit() {
    try {
      if (introduction) {
        if (enrollment.canBeginFromPasswordLogin(request)) {
          enrollment.beginFromPasswordLogin(request);
        }
        setupStarted = true;
        render();
        return;
      }
      if (pending != null && pending.kind() == Kind.RECOVERY_OUTPUT) {
        if (!recoverySaved.getValue()) {
          return;
        }
        enrollment.dismissOutput(request, pending.id());
        finish();
        return;
      }
      if (recovering && pending == null && !initializing) {
        if (!ValidationHelper.validateRequiredInputs(List.of(code), texts)) {
          return;
        }
        enrollment.beginRecovery(request, code.getValue());
      } else if (initializing) {
        if (!ValidationHelper.validateRequiredInputs(List.of(password), texts)) {
          return;
        }
        enrollment.beginPassword(request, password.getValue());
      } else if (pending != null && pending.kind() == Kind.ENROLLMENT) {
        if (!ValidationHelper.validateRequiredInputs(List.of(enrollmentCode), texts)) {
          return;
        }
        enrollment.complete(request, pending.id(), enrollmentCode.getValue());
      } else {
        if (pending == null && !recoveryLoginSelected) {
          if (!ValidationHelper.validateRequiredInputs(List.of(confirmationCode), texts)) {
            return;
          }
          authorization.confirmSession(request, confirmationCode.getValue());
          finish();
          return;
        }
        if (!ValidationHelper.validateRequiredInputs(List.of(code), texts)) {
          return;
        }
        if (pending == null) {
          authorization.confirmSession(request, code.getValue());
          finish();
          return;
        } else if (pending.kind() == Kind.GOOGLE_EMAIL) {
          enrollment.confirmEmail(request, pending.id(), code.getValue());
        } else {
          enrollment.complete(request, pending.id(), code.getValue());
        }
      }
      render();
    } catch (RuntimeException failure) {
      if (initializing) {
        password.setInvalid(true);
      } else if (pending != null && pending.kind() == Kind.ENROLLMENT) {
        enrollmentCode.setInvalid(true);
      } else if (pending == null && !recovering && !recoveryLoginSelected) {
        confirmationCode.setInvalid(true);
      } else {
        code.setInvalid(true);
      }
      failure(failure);
    } finally {
      password.clear();
      code.clear();
      enrollmentCode.clear();
      confirmationCode.clear();
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
    hint.addClassName("cosmos-panel-description");
    content.add(hint);
  }

  private String documentPath(String suffix) {
    return request.getContextPath() + TwoFactorDocuments.PATH + "/" + suffix;
  }

  private void cancel() {
    if (pending != null && pending.kind() == Kind.RECOVERY_OUTPUT) {
      if (!recoverySaved.getValue()) {
        return;
      }
      enrollment.dismissOutput(request, pending.id());
    }
    if (!introduction && authorization.sessionConfirmed(request)) {
      UI.getCurrent().getPage().setLocation(request.getContextPath() + "/account?tab=security");
    } else {
      security.logout();
      UI.getCurrent().getPage().setLocation(request.getContextPath() + "/login");
    }
  }

  private void finish() {
    authorization.requireSession(request);
    String target =
        setup || recovering
            ? "/account?tab=security"
            : authorization.owner(request).roles().stream().anyMatch(RoleType::isAdministrative)
                ? "/admin"
                : "/home";
    UI.getCurrent().getPage().setLocation(request.getContextPath() + target);
  }

  @Override
  public String getPageTitle() {
    return texts.getTwoFactorPageTitle();
  }
}
