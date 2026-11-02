package de.derpeterson.app.views;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
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
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
public final class TwoFactorView extends VerticalLayout
    implements BeforeEnterObserver, HasDynamicTitle {
  private final transient MessageProperties texts;
  private final transient SecurityService security;
  private final transient TwoFactorAuthorization authorization;
  private final transient TwoFactorEnrollment enrollment;
  private final transient ExternalLoginFlow external;
  private final transient ExternalIdentityService identities;
  private final transient HttpServletRequest request;
  private final AccountPanelComponent panel =
      new AccountPanelComponent("two-factor-panel", AppIcon.LOCK);
  private final Div content = new Div();
  private final Div extraActions = new Div();
  private final Button cancel = new Button();
  private final Button submit = new Button();
  private final PasswordField password = new PasswordField();
  private final TextField code = new TextField();
  private TwoFactorEnrollment.Pending pending;
  private boolean setup;
  private boolean administration;
  private boolean recovering;
  private boolean initializing;
  private boolean introduction;
  private boolean setupStarted;

  public TwoFactorView(
      ConfigService config,
      MessageProperties texts,
      SecurityService security,
      TwoFactorAuthorization authorization,
      TwoFactorEnrollment enrollment,
      ExternalLoginFlow external,
      ExternalIdentityService identities,
      HttpServletRequest request) {
    this.texts = texts;
    this.security = security;
    this.authorization = authorization;
    this.enrollment = enrollment;
    this.external = external;
    this.identities = identities;
    this.request = request;
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
    password.setInvalid(false);
    code.setInvalid(false);
    pending = enrollment.pending(request);
    introduction = false;
    panel.removeClassName("two-factor-introduction");
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
    submit.setEnabled(true);
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
        panel.setHeading(texts.getTwoFactorSetupTitle(), texts.getTwoFactorPageHint(), "");
        addHint(texts.getTwoFactorScanHint());
        Image qr =
            new Image(documentPath("setup/" + pending.id() + "/qr.png"), texts.getTwoFactorQrAlt());
        qr.setId("two-factor-qr");
        qr.addClassName("two-factor-qr");
        content.add(qr);
        Anchor manual =
            new Anchor(
                documentPath("setup/" + pending.id() + "/key"), texts.getTwoFactorManualKey());
        manual.setTarget("_blank");
        manual.getElement().setAttribute("rel", "noopener noreferrer");
        content.add(manual);
        addHint(texts.getTwoFactorAppHint());
        Div apps =
            new Div(
                new Anchor(
                    "https://support.google.com/accounts/answer/1066447", "Google Authenticator"),
                new Anchor(
                    "https://support.microsoft.com/account-billing/download-and-install-the-microsoft-authenticator-app-351498fc-850a-45da-b7b6-27e523b8702a",
                    "Microsoft Authenticator"));
        apps.addClassName("two-factor-app-links");
        apps.getChildren()
            .map(Anchor.class::cast)
            .forEach(
                link -> {
                  link.setTarget("_blank");
                  link.getElement().setAttribute("rel", "noopener noreferrer");
                });
        content.add(apps);
        prepareCode(texts.getTwoFactorCodeLabel(), 6);
        code.getElement().setAttribute("inputmode", "numeric");
        submit.setText(texts.getTwoFactorEnable());
      }
      case RECOVERY_OUTPUT -> showRecoveryCodes();
      default -> throw new IllegalStateException("Unexpected setup phase");
    }
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
    Anchor download =
        new Anchor(
            documentPath(
                "recovery/"
                    + pending.id()
                    + "/codes.pdf?lang="
                    + VaadinSession.getCurrent().getLocale().getLanguage()),
            texts.getTwoFactorDownloadPdf());
    download.setId("two-factor-download-pdf");
    extraActions.add(copy, download);
    submit.setText(texts.getTwoFactorCodesSaved());
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
      (initializing ? password : code).setInvalid(true);
      failure(failure);
    } finally {
      password.clear();
      code.clear();
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
