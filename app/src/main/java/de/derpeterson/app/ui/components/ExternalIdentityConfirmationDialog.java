package de.derpeterson.app.ui.components;

import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE;
import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE_NAME;
import static de.derpeterson.app.security.ExternalProviderSettings.MICROSOFT_NAME;
import static de.derpeterson.app.ui.components.CosmosClasses.DELETE_BUTTON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_DESCRIPTION;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_HEADING;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_ICON;
import static de.derpeterson.app.ui.components.CosmosClasses.PANEL_TITLE;
import static de.derpeterson.app.ui.components.CosmosClasses.REFERENCE_OVERLAY;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.Autocomplete;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.ExternalLoginFlow.Purpose;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

/** Bestätigt genau die angeforderte Kontoaktion; die Fachaktion prüft den Nachweis erneut. */
public final class ExternalIdentityConfirmationDialog extends Dialog {
  private static final String RETURN_ACTION =
      ExternalIdentityConfirmationDialog.class.getName() + ".returnAction";
  private final transient ExternalLoginFlow flow;
  private final transient ExternalIdentityService identities;
  private final transient HttpServletRequest request;
  private final transient MessageProperties texts;
  private final H2 title = new H2();
  private final Div symbol = new Div();
  private final Span description = new Span();
  private final Span explanation = new Span();
  private final Span ready = new Span();
  private final PasswordField password = new PasswordField();
  private final TwoFactorCodeInput codePanel;
  private final Span separator = new Span();
  private final Div alternatives = new Div();
  private final Div note = new Div();
  private final Span warning = new Span();
  private final Span developmentWarning = new Span();
  private final Button cancel = new Button();
  private final Button close = new Button(AppIcon.CLOSE_CIRCLE.create());
  private final Button confirm = new Button();
  private Purpose purpose;
  private SerializableRunnable action;

  public ExternalIdentityConfirmationDialog(
      ExternalLoginFlow flow,
      ExternalIdentityService identities,
      HttpServletRequest request,
      MessageProperties texts) {
    this.flow = flow;
    this.identities = identities;
    this.request = request;
    this.texts = texts;
    setId("external-identity-dialog");
    addClassNames(
        "cosmos-theme", REFERENCE_OVERLAY, "account-change-dialog", "external-identity-dialog");
    getThemeNames().addAll(List.of("cosmos", "cosmos-reference"));
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    title.addClassName(PANEL_TITLE);
    description.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(title, description);
    heading.addClassName(PANEL_HEADING);
    symbol.add(AppIcon.LOCK.create());
    symbol.addClassName(PANEL_ICON);
    close.addClassName("cosmos-overlay-close");
    close.addClickListener(event -> close());
    Div header = new Div(symbol, heading, close);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);
    password.setId("external-confirm-password");
    password.setWidthFull();
    password.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    password.setManualValidation(true);
    password.setValueChangeMode(ValueChangeMode.EAGER);
    codePanel =
        new TwoFactorCodeInput(
            texts,
            request,
            "external-confirm",
            "external-proof",
            () -> {
              flow.cancel(request);
              close();
            });
    codePanel.addAppCodeCompleteListener(
        () -> {
          if ((purpose == Purpose.LINK_GOOGLE || purpose == Purpose.LINK_MICROSOFT)
              && isOpened()
              && confirm.isEnabled()) {
            submit();
          }
        });
    separator.addClassName("external-proof-separator");
    alternatives.addClassName("external-proof-alternatives");
    explanation.addClassName(PANEL_DESCRIPTION);
    ready.setId("external-security-result");
    ready.addClassName("external-proof-ready");
    note.add(AppIcon.INFO_CIRCLE.create(), warning);
    note.addClassName("cosmos-panel-hint");
    developmentWarning.addClassName("cosmos-panel-description");
    Div body =
        new Div(
            explanation,
            ready,
            password,
            codePanel,
            separator,
            alternatives,
            note,
            developmentWarning);
    body.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(body);
    cancel.setPrefixComponent(AppIcon.CLOSE.create());
    cancel.addClickListener(event -> close());
    cancel.setId("external-proof-cancel");
    confirm.setId("external-confirm-local");
    confirm.addClickListener(event -> submit());
    Div actions = new Div(cancel, confirm);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configure(actions);
    getFooter().add(actions);
    addOpenedChangeListener(
        event -> {
          if (!event.isOpened()) {
            password.clear();
            password.setInvalid(false);
            codePanel.reset();
            purpose = null;
            action = null;
          }
        });
  }

  public void open(Purpose purpose, SerializableRunnable action) {
    this.purpose = Objects.requireNonNull(purpose);
    this.action = Objects.requireNonNull(action);
    password.clear();
    password.setInvalid(false);
    codePanel.reset();
    refreshTexts();
    super.open();
    if (codePanel.isVisible()) {
      codePanel.focusAppCode();
    }
  }

  public void refreshTexts() {
    if (purpose == null) {
      return;
    }
    boolean confirmed = flow.confirmedPurpose(request).filter(purpose::equals).isPresent();
    boolean hasPassword = flow.owner(request).getPassword() != null;
    boolean needsFactor = flow.requiresFactor(request, purpose);
    developmentWarning.setText(texts.getTwoFactorDevelopmentWarning());
    developmentWarning.setVisible(flow.developmentExceptionActive());
    refreshHeader();
    explanation.setText(
        needsFactor ? texts.getTwoFactorActionHint() : texts.getExternalProofHint());
    explanation.setVisible(!needsFactor || confirmed);
    ready.setText(texts.getExternalProofReady());
    ready.setVisible(confirmed);
    password.setLabel(texts.getAccountPasswordDialogCurrentPassword());
    password.setPlaceholder(texts.getAccountPasswordPlaceholder());
    password.setVisible(hasPassword && !confirmed && !needsFactor);
    password.setRequiredIndicatorVisible(password.isVisible());
    codePanel.refreshTexts();
    codePanel.setVisible(needsFactor && !confirmed);
    refreshAlternatives(confirmed);
    if (!confirmed && !hasPassword && !alternatives.isVisible() && !needsFactor) {
      explanation.setText(texts.getExternalProofUnavailable());
    }
    separator.setText(texts.getExternalOr());
    separator.setVisible(password.isVisible() && alternatives.isVisible());
    refreshActions(confirmed || hasPassword || needsFactor);
  }

  private void refreshHeader() {
    title.setText(actionTitle());
    symbol.removeAll();
    String headerProvider =
        switch (purpose) {
          case LINK_GOOGLE, UNLINK_GOOGLE -> GOOGLE;
          case LINK_MICROSOFT, UNLINK_MICROSOFT -> "microsoft";
          default -> null;
        };
    if (headerProvider == null) {
      symbol.add(AppIcon.LOCK.create());
    } else {
      Image mark =
          new Image(
              "icons/external-login/"
                  + headerProvider
                  + (headerProvider.equals(GOOGLE) ? ".png" : ".svg"),
              "");
      mark.addClassName("external-proof-heading-mark");
      symbol.add(mark);
    }
    getElement().setProperty("ariaLabel", title.getText());
    description.setText(texts.getExternalProof());
  }

  private void refreshAlternatives(boolean confirmed) {
    alternatives.removeAll();
    if (!confirmed) {
      for (String provider : identities.connected(flow.owner(request).getId())) {
        if (flow.providers().contains(provider)
            && flow.canUseProviderProof(request, provider, purpose)) {
          Button proof = new Button(texts.getExternalProofWith(brand(provider)));
          proof.setId("external-confirm-" + provider);
          Image mark =
              new Image(
                  "icons/external-login/" + provider + (provider.equals(GOOGLE) ? ".png" : ".svg"),
                  "");
          mark.addClassName("external-proof-provider-mark");
          mark.getElement().setAttribute("aria-hidden", "true");
          proof.setPrefixComponent(mark);
          proof.addClassName("external-proof-provider");
          proof.addClickListener(event -> beginProviderProof(provider));
          alternatives.add(proof);
        }
      }
    }
    alternatives.setVisible(alternatives.getComponentCount() > 0);
  }

  private void refreshActions(boolean enabled) {
    boolean destructive = purpose == Purpose.UNLINK_GOOGLE || purpose == Purpose.UNLINK_MICROSOFT;
    warning.setText(texts.getExternalDisconnectHint());
    note.setVisible(destructive);
    confirm.setClassName(DELETE_BUTTON, destructive);
    confirm.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    if (!destructive) {
      confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    confirm.setPrefixComponent((destructive ? AppIcon.SIGN_OUT : AppIcon.CHECK).create());
    confirm.setText(
        purpose == Purpose.PASSWORD || purpose == Purpose.EMAIL
            ? texts.getExternalContinueAction()
            : actionTitle());
    confirm.setEnabled(enabled);
    cancel.setText(texts.getAdminUsersCancel());
    close.setAriaLabel(texts.getAdminUsersCloseDialog());
  }

  private void submit() {
    if (purpose == null || action == null) {
      return;
    }
    try {
      if (flow.confirmedPurpose(request).filter(purpose::equals).isEmpty()
          && !confirmLocalProof()) {
        return;
      }
      action.run();
      close();
    } catch (RuntimeException _) {
      showExternalFailure();
      refreshTexts();
    } finally {
      password.clear();
      codePanel.clear();
    }
  }

  private boolean confirmLocalProof() {
    if (codePanel.isVisible()) {
      return confirmFactor();
    }
    if (password.isVisible()) {
      return confirmPassword();
    }
    showExternalFailure();
    refreshTexts();
    return false;
  }

  private boolean confirmFactor() {
    if (!codePanel.validate()) {
      return false;
    }
    try {
      flow.confirmFactor(request, codePanel.getValue(), purpose);
      return true;
    } catch (RuntimeException failure) {
      codePanel.markInvalid();
      Supplier<String> message = texts::getTwoFactorFailed;
      if (failure instanceof LockedException) {
        message = texts::getTwoFactorThrottled;
      } else if (failure instanceof BadCredentialsException) {
        message = texts::getTwoFactorInvalidCode;
      }
      NotificationHelper.getInstance()
          .showNotification(
              texts::getBaseFailedTitle, message, NotificationHelper.NotificationType.ERROR);
      return false;
    }
  }

  private boolean confirmPassword() {
    if (!ValidationHelper.validateRequiredInputs(List.of(password), texts)) {
      return false;
    }
    try {
      flow.confirmPassword(request, password.getValue(), purpose);
      return true;
    } catch (RuntimeException failure) {
      AccountSecurityFeedback.Field field = AccountSecurityFeedback.failure(texts, failure);
      password.setInvalid(field == AccountSecurityFeedback.Field.CURRENT_PASSWORD);
      refreshTexts();
      return false;
    }
  }

  private void showExternalFailure() {
    NotificationHelper.getInstance()
        .showNotification(
            texts::getBaseFailedTitle,
            texts::getExternalError,
            NotificationHelper.NotificationType.ERROR);
  }

  private void beginProviderProof(String provider) {
    try {
      String destination =
          flow.beginProof(request, provider, purpose, VaadinSession.getCurrent().getLocale());
      request.getSession(false).setAttribute(RETURN_ACTION, purpose);
      UI.getCurrent().getPage().setLocation(destination);
    } catch (RuntimeException _) {
      if (!flow.providers().contains(provider)) {
        close();
        NotificationHelper.getInstance()
            .showNotification(
                texts::getExternalProviderUnavailable, NotificationHelper.NotificationType.WARNING);
      } else {
        showExternalFailure();
      }
    }
  }

  /**
   * Der Rückkehrmarker enthält nur die Absicht, keine Berechtigung; den Nachweis prüft der Dienst.
   */
  public Optional<Purpose> consumeProviderReturn() {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return Optional.empty();
    }
    Object pending = session.getAttribute(RETURN_ACTION);
    session.removeAttribute(RETURN_ACTION);
    if (pending instanceof Purpose requested) {
      return flow.confirmedPurpose(request).filter(requested::equals);
    }
    return Optional.empty();
  }

  private String actionTitle() {
    return switch (purpose) {
      case PASSWORD -> texts.getExternalPasswordSetup();
      case EMAIL -> texts.getAccountSettingsChangeEmail();
      case LINK_GOOGLE -> texts.getExternalConnect(GOOGLE_NAME);
      case LINK_MICROSOFT -> texts.getExternalConnect(MICROSOFT_NAME);
      case UNLINK_GOOGLE -> texts.getExternalDisconnect(GOOGLE_NAME);
      case UNLINK_MICROSOFT -> texts.getExternalDisconnect(MICROSOFT_NAME);
      case ENROLL_FACTOR -> throw new IllegalArgumentException("Enrollment has its own view");
    };
  }

  private String brand(String provider) {
    return provider.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
  }
}
