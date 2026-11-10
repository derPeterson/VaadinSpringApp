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
  private final transient ExternalLoginFlow externalLoginFlow;
  private final transient ExternalIdentityService externalIdentityService;
  private final transient HttpServletRequest httpRequest;
  private final transient MessageProperties messageProperties;
  private final H2 dialogTitle = new H2();
  private final Div providerIconContainer = new Div();
  private final Span dialogDescription = new Span();
  private final Span identityProofExplanation = new Span();
  private final Span providerReadyHint = new Span();
  private final PasswordField passwordField = new PasswordField();
  private final TwoFactorCodeInput twoFactorCodeInput;
  private final Span proofSeparator = new Span();
  private final Div proofAlternativesLayout = new Div();
  private final Div proofNoticeContainer = new Div();
  private final Span proofWarningText = new Span();
  private final Span developmentWarning = new Span();
  private final Button cancelButton = new Button();
  private final Button closeDialogButton = new Button(AppIcon.CLOSE_CIRCLE.create());
  private final Button confirmIdentityButton = new Button();
  private Purpose identityConfirmationPurpose;
  private SerializableRunnable confirmedIdentityAction;

  public ExternalIdentityConfirmationDialog(
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService,
      HttpServletRequest httpRequest,
      MessageProperties messageProperties) {
    this.externalLoginFlow = externalLoginFlow;
    this.externalIdentityService = externalIdentityService;
    this.httpRequest = httpRequest;
    this.messageProperties = messageProperties;
    setId("external-identity-dialog");
    addClassNames(
        "cosmos-theme", REFERENCE_OVERLAY, "account-change-dialog", "external-identity-dialog");
    getThemeNames().addAll(List.of("cosmos", "cosmos-reference"));
    setModality(ModalityMode.STRICT);
    setCloseOnEsc(true);
    setCloseOnOutsideClick(true);
    dialogTitle.addClassName(PANEL_TITLE);
    dialogDescription.addClassName(PANEL_DESCRIPTION);
    Div heading = new Div(dialogTitle, dialogDescription);
    heading.addClassName(PANEL_HEADING);
    providerIconContainer.add(AppIcon.LOCK.create());
    providerIconContainer.addClassName(PANEL_ICON);
    closeDialogButton.addClassName("cosmos-overlay-close");
    closeDialogButton.addClickListener(event -> close());
    Div header = new Div(providerIconContainer, heading, closeDialogButton);
    header.addClassName("account-change-dialog-header");
    getHeader().add(header);
    passwordField.setId("external-confirm-password");
    passwordField.setWidthFull();
    passwordField.setAutocomplete(Autocomplete.CURRENT_PASSWORD);
    passwordField.setManualValidation(true);
    passwordField.setValueChangeMode(ValueChangeMode.EAGER);
    twoFactorCodeInput =
        new TwoFactorCodeInput(
            messageProperties,
            httpRequest,
            "external-confirm",
            "external-proof",
            () -> {
              externalLoginFlow.cancel(httpRequest);
              close();
            });
    twoFactorCodeInput.addAppCodeCompleteListener(() -> {
      if ((identityConfirmationPurpose == Purpose.LINK_GOOGLE
              || identityConfirmationPurpose == Purpose.LINK_MICROSOFT)
          && isOpened()
          && confirmIdentityButton.isEnabled()) {
        confirmExternalIdentityAction();
      }
    });
    proofSeparator.addClassName("external-proof-separator");
    proofAlternativesLayout.addClassName("external-proof-alternatives");
    identityProofExplanation.addClassName(PANEL_DESCRIPTION);
    providerReadyHint.setId("external-security-result");
    providerReadyHint.addClassName("external-proof-ready");
    proofNoticeContainer.add(AppIcon.INFO_CIRCLE.create(), proofWarningText);
    proofNoticeContainer.addClassName("cosmos-panel-hint");
    developmentWarning.addClassName("cosmos-panel-description");
    Div body =
        new Div(
            identityProofExplanation,
            providerReadyHint,
            passwordField,
            twoFactorCodeInput,
            proofSeparator,
            proofAlternativesLayout,
            proofNoticeContainer,
            developmentWarning);
    body.addClassNames("cosmos-dialog-form", "account-change-dialog-form");
    add(body);
    cancelButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelButton.addClickListener(event -> close());
    cancelButton.setId("external-proof-cancel");
    confirmIdentityButton.setId("external-confirm-local");
    confirmIdentityButton.addClickListener(event -> confirmExternalIdentityAction());
    Div actions = new Div(cancelButton, confirmIdentityButton);
    actions.addClassName("cosmos-dialog-actions");
    EqualButtonWidths.configureEqualButtonWidths(actions);
    getFooter().add(actions);
    addOpenedChangeListener(event -> {
      if (!event.isOpened()) {
        passwordField.clear();
        passwordField.setInvalid(false);
        twoFactorCodeInput.reset();
        identityConfirmationPurpose = null;
        confirmedIdentityAction = null;
      }
    });
  }

  public void open(
      Purpose identityConfirmationPurpose, SerializableRunnable confirmedIdentityAction) {
    this.identityConfirmationPurpose = Objects.requireNonNull(identityConfirmationPurpose);
    this.confirmedIdentityAction = Objects.requireNonNull(confirmedIdentityAction);
    passwordField.clear();
    passwordField.setInvalid(false);
    twoFactorCodeInput.reset();
    refreshTexts();
    super.open();
    if (twoFactorCodeInput.isVisible()) {
      twoFactorCodeInput.focusAppCode();
    }
  }

  public void refreshTexts() {
    if (identityConfirmationPurpose == null) {
      return;
    }
    boolean confirmed =
        externalLoginFlow
            .confirmedPurpose(httpRequest)
            .filter(identityConfirmationPurpose::equals)
            .isPresent();
    boolean hasPassword = externalLoginFlow.owner(httpRequest).getPassword() != null;
    boolean needsFactor =
        externalLoginFlow.requiresFactor(httpRequest, identityConfirmationPurpose);
    developmentWarning.setText(messageProperties.getTwoFactorDevelopmentWarning());
    developmentWarning.setVisible(externalLoginFlow.developmentExceptionActive());
    refreshHeader();
    identityProofExplanation.setText(
        needsFactor
            ? messageProperties.getTwoFactorActionHint()
            : messageProperties.getExternalProofHint());
    identityProofExplanation.setVisible(!needsFactor || confirmed);
    providerReadyHint.setText(messageProperties.getExternalProofReady());
    providerReadyHint.setVisible(confirmed);
    passwordField.setLabel(messageProperties.getAccountPasswordDialogCurrentPassword());
    passwordField.setPlaceholder(messageProperties.getAccountPasswordPlaceholder());
    passwordField.setVisible(hasPassword && !confirmed && !needsFactor);
    passwordField.setRequiredIndicatorVisible(passwordField.isVisible());
    twoFactorCodeInput.refreshTexts();
    twoFactorCodeInput.setVisible(needsFactor && !confirmed);
    refreshAlternatives(confirmed);
    if (!confirmed && !hasPassword && !proofAlternativesLayout.isVisible() && !needsFactor) {
      identityProofExplanation.setText(messageProperties.getExternalProofUnavailable());
    }
    proofSeparator.setText(messageProperties.getExternalOr());
    proofSeparator.setVisible(passwordField.isVisible() && proofAlternativesLayout.isVisible());
    refreshActions(confirmed || hasPassword || needsFactor);
  }

  private void refreshHeader() {
    dialogTitle.setText(actionTitle());
    providerIconContainer.removeAll();
    String headerProvider =
        switch (identityConfirmationPurpose) {
          case LINK_GOOGLE, UNLINK_GOOGLE -> GOOGLE;
          case LINK_MICROSOFT, UNLINK_MICROSOFT -> "microsoft";
          default -> null;
        };
    if (headerProvider == null) {
      providerIconContainer.add(AppIcon.LOCK.create());
    } else {
      Image mark =
          new Image(
              "icons/external-login/"
                  + headerProvider
                  + (headerProvider.equals(GOOGLE) ? ".png" : ".svg"),
              "");
      mark.addClassName("external-proof-heading-mark");
      providerIconContainer.add(mark);
    }
    getElement().setProperty("ariaLabel", dialogTitle.getText());
    dialogDescription.setText(messageProperties.getExternalProof());
  }

  private void refreshAlternatives(boolean confirmed) {
    proofAlternativesLayout.removeAll();
    if (!confirmed) {
      for (String provider :
          externalIdentityService.connected(externalLoginFlow.owner(httpRequest).getId())) {
        if (externalLoginFlow.providers().contains(provider)
            && externalLoginFlow.canUseProviderProof(
                httpRequest, provider, identityConfirmationPurpose)) {
          Button proof = new Button(messageProperties.getExternalProofWith(brand(provider)));
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
          proofAlternativesLayout.add(proof);
        }
      }
    }
    proofAlternativesLayout.setVisible(proofAlternativesLayout.getComponentCount() > 0);
  }

  private void refreshActions(boolean enabled) {
    boolean destructive =
        identityConfirmationPurpose == Purpose.UNLINK_GOOGLE
            || identityConfirmationPurpose == Purpose.UNLINK_MICROSOFT;
    proofWarningText.setText(messageProperties.getExternalDisconnectHint());
    proofNoticeContainer.setVisible(destructive);
    confirmIdentityButton.setClassName(DELETE_BUTTON, destructive);
    confirmIdentityButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    if (!destructive) {
      confirmIdentityButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
    confirmIdentityButton.setPrefixComponent(
        (destructive ? AppIcon.SIGN_OUT : AppIcon.CHECK).create());
    confirmIdentityButton.setText(
        identityConfirmationPurpose == Purpose.PASSWORD
                || identityConfirmationPurpose == Purpose.EMAIL
            ? messageProperties.getExternalContinueAction()
            : actionTitle());
    confirmIdentityButton.setEnabled(enabled);
    cancelButton.setText(messageProperties.getAdminUsersCancel());
    closeDialogButton.setAriaLabel(messageProperties.getAdminUsersCloseDialog());
  }

  private void confirmExternalIdentityAction() {
    if (identityConfirmationPurpose == null || confirmedIdentityAction == null) {
      return;
    }
    try {
      if (externalLoginFlow
              .confirmedPurpose(httpRequest)
              .filter(identityConfirmationPurpose::equals)
              .isEmpty()
          && !confirmLocalProof()) {
        return;
      }
      confirmedIdentityAction.run();
      close();
    } catch (RuntimeException _) {
      showExternalFailure();
      refreshTexts();
    } finally {
      passwordField.clear();
      twoFactorCodeInput.clear();
    }
  }

  private boolean confirmLocalProof() {
    if (twoFactorCodeInput.isVisible()) {
      return confirmFactor();
    }
    if (passwordField.isVisible()) {
      return confirmPassword();
    }
    showExternalFailure();
    refreshTexts();
    return false;
  }

  private boolean confirmFactor() {
    if (!twoFactorCodeInput.validate()) {
      return false;
    }
    try {
      externalLoginFlow.confirmFactor(
          httpRequest, twoFactorCodeInput.getValue(), identityConfirmationPurpose);
      return true;
    } catch (RuntimeException failure) {
      twoFactorCodeInput.markInvalid();
      Supplier<String> message = messageProperties::getTwoFactorFailed;
      if (failure instanceof LockedException) {
        message = messageProperties::getTwoFactorThrottled;
      } else if (failure instanceof BadCredentialsException) {
        message = messageProperties::getTwoFactorInvalidCode;
      }
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              message,
              NotificationHelper.NotificationType.ERROR);
      return false;
    }
  }

  private boolean confirmPassword() {
    if (!ValidationHelper.validateRequiredInputs(List.of(passwordField), messageProperties)) {
      return false;
    }
    try {
      externalLoginFlow.confirmPassword(
          httpRequest, passwordField.getValue(), identityConfirmationPurpose);
      return true;
    } catch (RuntimeException failure) {
      AccountSecurityFeedback.Field field =
          AccountSecurityFeedback.failure(messageProperties, failure);
      passwordField.setInvalid(field == AccountSecurityFeedback.Field.CURRENT_PASSWORD);
      refreshTexts();
      return false;
    }
  }

  private void showExternalFailure() {
    NotificationHelper.getInstance()
        .showNotification(
            messageProperties::getBaseFailedTitle,
            messageProperties::getExternalError,
            NotificationHelper.NotificationType.ERROR);
  }

  private void beginProviderProof(String provider) {
    try {
      String destination =
          externalLoginFlow.beginProof(
              httpRequest,
              provider,
              identityConfirmationPurpose,
              VaadinSession.getCurrent().getLocale());
      httpRequest.getSession(false).setAttribute(RETURN_ACTION, identityConfirmationPurpose);
      UI.getCurrent().getPage().setLocation(destination);
    } catch (RuntimeException _) {
      if (!externalLoginFlow.providers().contains(provider)) {
        close();
        NotificationHelper.getInstance()
            .showNotification(
                messageProperties::getExternalProviderUnavailable,
                NotificationHelper.NotificationType.WARNING);
      } else {
        showExternalFailure();
      }
    }
  }

  /**
   * Der Rückkehrmarker enthält nur die Absicht, keine Berechtigung; den Nachweis prüft der Dienst.
   */
  public Optional<Purpose> consumeProviderReturn() {
    HttpSession session = httpRequest.getSession(false);
    if (session == null) {
      return Optional.empty();
    }
    Object pending = session.getAttribute(RETURN_ACTION);
    session.removeAttribute(RETURN_ACTION);
    if (pending instanceof Purpose requested) {
      return externalLoginFlow.confirmedPurpose(httpRequest).filter(requested::equals);
    }
    return Optional.empty();
  }

  private String actionTitle() {
    return switch (identityConfirmationPurpose) {
      case PASSWORD -> messageProperties.getExternalPasswordSetup();
      case EMAIL -> messageProperties.getAccountSettingsChangeEmail();
      case LINK_GOOGLE -> messageProperties.getExternalConnect(GOOGLE_NAME);
      case LINK_MICROSOFT -> messageProperties.getExternalConnect(MICROSOFT_NAME);
      case UNLINK_GOOGLE -> messageProperties.getExternalDisconnect(GOOGLE_NAME);
      case UNLINK_MICROSOFT -> messageProperties.getExternalDisconnect(MICROSOFT_NAME);
      case ENROLL_FACTOR -> throw new IllegalArgumentException("Enrollment has its own view");
    };
  }

  private String brand(String provider) {
    return provider.equals(GOOGLE) ? GOOGLE_NAME : MICROSOFT_NAME;
  }
}
