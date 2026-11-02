package de.derpeterson.app.views;

import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;

import com.flowingcode.vaadin.addons.otpfield.OtpField;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Image;
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
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.RecoveryCodeDownload;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
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
  private final Button cancel = new Button();
  private final Button submit = new Button();
  private final PasswordField password = new PasswordField();
  private final TextField code = new TextField();
  private final OtpField enrollmentCode = new OtpField();
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
    enrollmentCode.setId("two-factor-enrollment-code");
    enrollmentCode.setLength(6);
    enrollmentCode.setPlaceholder("•");
    enrollmentCode.setManualValidation(true);
    enrollmentCode.setRequiredIndicatorVisible(true);
    enrollmentCode.setValueChangeMode(ValueChangeMode.ON_CHANGE);
    enrollmentCode.addClassName("two-factor-otp");
    String otpStylesheetPath = request.getContextPath() + "/custom-theme/cosmos-mfa-otp.css";
    enrollmentCode.addAttachListener(
        event ->
            enrollmentCode
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
    recoverySaved.setId("two-factor-codes-saved");
    recoverySaved.addValueChangeListener(event -> updateRecoveryActions());
    cancel.setId("two-factor-cancel");
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    cancel.addClickListener(event -> cancel());
    submit.setId("two-factor-submit");
    submit.setPrefixComponent(AppIcon.CHECK.create());
    submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submit.addClickListener(event -> submit());
    Div actions = new Div(cancel, submit);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configure(actions);
    panel.addActionFooter(actions);
    page.getCardContainer().add(panel);
    add(page);
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> render());
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
    password.clear();
    code.clear();
    enrollmentCode.clear();
    password.setInvalid(false);
    code.setInvalid(false);
    enrollmentCode.setInvalid(false);
    pending = enrollment.pending(request);
    introduction = false;
    panel.removeClassName("two-factor-introduction");
    panel.removeClassName("two-factor-setup");
    cancel.removeClassName(DELETE_BUTTON);
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    submit.setPrefixComponent(AppIcon.CHECK.create());
    recovering = recovering || pending != null && pending.proof() == SetupProof.RECOVERY_CODE;
    TwoFactorAuthorization.State state = authorization.state(authorization.owner(request));
    initializing = pending == null && !state.enrolled();
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
    if (pending != null) {
      showPending();
    } else if (state.enrolled() && recovering) {
      showRecovery();
    } else if (state.enrolled()) {
      if (administration
          ? authorization.administrationConfirmed(request)
          : authorization.sessionConfirmed(request)) {
        finish();
        return;
      }
      addHint(administration ? texts.getTwoFactorActionHint() : texts.getTwoFactorLoginHint());
      prepareCode(texts.getTwoFactorCodeLabel(), 35);
      Button recover = new Button(texts.getTwoFactorRecoverAction(), AppIcon.EDIT.create());
      recover.setId("two-factor-recover");
      recover.addClickListener(
          event ->
              UI.getCurrent()
                  .getPage()
                  .setLocation(request.getContextPath() + "/two-factor?recover"));
      Button help =
          new Button(
              texts.getTwoFactorNeedHelp(),
              event ->
                  UI.getCurrent()
                      .getPage()
                      .setLocation(request.getContextPath() + "/recovery-help"));
      help.setId("two-factor-help");
      extraActions.add(recover, help);
      submit.setText(texts.getTwoFactorConfirm());
    } else {
      showSetup();
    }
  }

  private void showRecovery() {
    panel.setHeading(texts.getTwoFactorRecoverAction(), texts.getTwoFactorPageHint(), "");
    addHint(texts.getTwoFactorRecoverHint());
    if (!enrollment.available()) {
      addHint(texts.getTwoFactorUnavailable());
    }
    prepareCode(texts.getTwoFactorRecoveryCodeLabel(), 35);
    code.getElement().setAttribute("autocomplete", "off");
    submit.setVisible(true);
    submit.setEnabled(enrollment.available());
    submit.setText(texts.getTwoFactorConfirm());
    Button back = new Button(texts.getTwoFactorReturnToCode());
    back.setId("two-factor-recovery-back");
    back.addClickListener(
        event -> UI.getCurrent().getPage().setLocation(request.getContextPath() + "/two-factor"));
    extraActions.add(back);
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
        prepareCode(texts.getTwoFactorEmailCodeLabel(), 8);
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
    panel.setHeading(texts.getTwoFactorSetupTitle(), texts.getTwoFactorPageHint(), "");
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
    panel.setHeading(texts.getTwoFactorRecoveryTitle(), texts.getTwoFactorPageHint(), "");
    addHint(texts.getTwoFactorRecoveryExplanation());
    IFrame frame =
        new IFrame(
            documentPath(
                "recovery/"
                    + pending.id()
                    + "/codes?lang="
                    + VaadinSession.getCurrent().getLocale().getLanguage()));
    frame.setId("two-factor-recovery-codes");
    frame.getElement().setAttribute("title", texts.getTwoFactorCodeFrameTitle());
    frame.addClassName("two-factor-code-document");
    content.add(frame);
    addHint(texts.getTwoFactorRecoveryStorage());
    Button copy = new Button(texts.getTwoFactorCopyCodes(), AppIcon.CLIPBOARD_COPY.create());
    copy.setId("two-factor-copy-codes");
    copy.addClickListener(
        event ->
            copy.getElement()
                .executeJs(
                    """
        const values = $0.contentDocument?.getElementById('recovery-code-values')?.textContent;
        return values && navigator.clipboard ? navigator.clipboard.writeText(values).then(() => true, () => false) : false;
        """,
                    frame.getElement())
                .then(
                    Boolean.class,
                    copied ->
                        NotificationHelper.getInstance()
                            .showNotification(
                                Boolean.TRUE.equals(copied)
                                    ? texts::getTwoFactorCopySuccess
                                    : texts::getTwoFactorCopyFailed,
                                Boolean.TRUE.equals(copied)
                                    ? NotificationHelper.NotificationType.SUCCESS
                                    : NotificationHelper.NotificationType.ERROR)));
    UUID outputId = pending.id();
    Locale locale = VaadinSession.getCurrent().getLocale();
    Anchor download =
        new Anchor(
            event ->
                recoveryDownloads.write(
                    event,
                    outputId,
                    locale,
                    () ->
                        NotificationHelper.getInstance()
                            .showNotification(
                                texts::getTwoFactorDownloadFailed,
                                NotificationHelper.NotificationType.ERROR)),
            texts.getTwoFactorDownloadPdf());
    download.setId("two-factor-download-pdf");
    extraActions.add(copy, download);
    if (!outputId.equals(recoveryOutputId)) {
      recoveryOutputId = outputId;
      recoverySaved.setValue(false);
    }
    recoverySaved.setLabel(texts.getTwoFactorCodesSavedConfirmation());
    content.add(recoverySaved);
    submit.setText(texts.getTwoFactorCodesSaved());
    updateRecoveryActions();
  }

  private void updateRecoveryActions() {
    if (pending != null && pending.kind() == Kind.RECOVERY_OUTPUT) {
      submit.setEnabled(recoverySaved.getValue());
      cancel.setEnabled(recoverySaved.getValue());
    }
  }

  private void prepareCode(String label, int maximumLength) {
    code.setLabel(label);
    code.setPlaceholder(texts.getTwoFactorCodePlaceholder());
    code.setMaxLength(maximumLength);
    code.getElement().setAttribute("autocomplete", "one-time-code");
    code.getElement().removeAttribute("inputmode");
    content.add(code);
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
      } else {
        code.setInvalid(true);
      }
      failure(failure);
    } finally {
      password.clear();
      code.clear();
      enrollmentCode.clear();
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
