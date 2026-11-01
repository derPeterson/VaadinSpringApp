package de.derpeterson.app.ui.components;

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
import java.util.Objects;
import java.util.Optional;

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
  private final Span separator = new Span();
  private final Div alternatives = new Div();
  private final Div note = new Div();
  private final Span warning = new Span();
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
    getThemeNames().addAll(java.util.List.of("cosmos", "cosmos-reference"));
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
    separator.addClassName("external-proof-separator");
    alternatives.addClassName("external-proof-alternatives");
    explanation.addClassName(PANEL_DESCRIPTION);
    ready.setId("external-security-result");
    ready.addClassName("external-proof-ready");
    note.add(AppIcon.INFO_CIRCLE.create(), warning);
    note.addClassName("cosmos-panel-hint");
    Div body = new Div(explanation, ready, password, separator, alternatives, note);
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
    refreshTexts();
    super.open();
  }

  public void refreshTexts() {
    if (purpose == null) {
      return;
    }
    boolean confirmed = flow.confirmedPurpose(request).filter(purpose::equals).isPresent();
    boolean hasPassword = flow.owner(request).getPassword() != null;
    title.setText(actionTitle());
    symbol.removeAll();
    String headerProvider =
        switch (purpose) {
          case LINK_GOOGLE, UNLINK_GOOGLE -> "google";
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
                  + (headerProvider.equals("google") ? ".png" : ".svg"),
              "");
      mark.addClassName("external-proof-heading-mark");
      symbol.add(mark);
    }
    getElement().setProperty("ariaLabel", title.getText());
    description.setText(texts.getExternalProof());
    explanation.setText(texts.getExternalProofHint());
    ready.setText(texts.getExternalProofReady());
    ready.setVisible(confirmed);
    password.setLabel(texts.getAccountPasswordDialogCurrentPassword());
    password.setPlaceholder(texts.getAccountPasswordPlaceholder());
    password.setVisible(hasPassword && !confirmed);
    password.setRequiredIndicatorVisible(password.isVisible());
    alternatives.removeAll();
    if (!confirmed) {
      for (String provider : identities.connected(flow.owner(request).getId())) {
        if (flow.providers().contains(provider)) {
          Button proof = new Button(texts.getExternalProofWith(brand(provider)));
          proof.setId("external-confirm-" + provider);
          Image mark =
              new Image(
                  "icons/external-login/"
                      + provider
                      + (provider.equals("google") ? ".png" : ".svg"),
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
    if (!confirmed && !hasPassword && !alternatives.isVisible()) {
      explanation.setText(texts.getExternalProofUnavailable());
    }
    separator.setText(texts.getExternalOr());
    separator.setVisible(password.isVisible() && alternatives.isVisible());
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
    confirm.setEnabled(confirmed || hasPassword);
    cancel.setText(texts.getAdminUsersCancel());
    close.setAriaLabel(texts.getAdminUsersCloseDialog());
  }

  private void submit() {
    if (purpose == null || action == null) {
      return;
    }
    try {
      if (flow.confirmedPurpose(request).filter(purpose::equals).isEmpty()) {
        if (!password.isVisible()) {
          showExternalFailure();
          refreshTexts();
          return;
        }
        if (!ValidationHelper.validateRequiredInputs(java.util.List.of(password), texts)) {
          return;
        }
        try {
          flow.confirmPassword(request, password.getValue(), purpose);
        } catch (RuntimeException failure) {
          AccountSecurityFeedback.Field field = AccountSecurityFeedback.failure(texts, failure);
          password.setInvalid(field == AccountSecurityFeedback.Field.CURRENT_PASSWORD);
          refreshTexts();
          return;
        }
      }
      action.run();
      close();
    } catch (RuntimeException failure) {
      showExternalFailure();
      refreshTexts();
    } finally {
      password.clear();
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
    } catch (RuntimeException failure) {
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
      case LINK_GOOGLE -> texts.getExternalConnect("Google");
      case LINK_MICROSOFT -> texts.getExternalConnect("Microsoft");
      case UNLINK_GOOGLE -> texts.getExternalDisconnect("Google");
      case UNLINK_MICROSOFT -> texts.getExternalDisconnect("Microsoft");
    };
  }

  private String brand(String provider) {
    return provider.equals("google") ? "Google" : "Microsoft";
  }
}
