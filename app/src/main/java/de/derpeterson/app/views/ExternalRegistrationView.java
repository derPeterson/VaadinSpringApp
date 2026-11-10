package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.helper.ui.ValidationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.service.ExternalIdentityService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

/** Fehlende Angaben nach Anbieterprüfung; diese Seite verleiht selbst keine Portalanmeldung. */
@Route("external-registration")
@AnonymousAllowed
@StyleSheet("custom-theme/cosmos-account.css")
@StyleSheet("custom-theme/cosmos-external-login.css")
public final class ExternalRegistrationView extends VerticalLayout
    implements BeforeEnterObserver, HasDynamicTitle {
  private final transient ExternalLoginFlow externalLoginFlow;
  private final transient ExternalIdentityService externalIdentityService;
  private final transient MessageProperties messageProperties;
  private final transient HttpServletRequest request;
  private final PublicPageComponent page;
  private final H2 registrationTitle = new H2();
  private final TextField firstNameField = new TextField();
  private final TextField lastNameField = new TextField();
  private final EmailField emailField = new EmailField();
  private final Span registrationError = new Span();
  private final Button completeRegistrationButton = new Button();
  private final Button cancelRegistrationButton = new Button();
  private boolean awaitingVerification;
  private boolean expiredRegistration;

  public ExternalRegistrationView(
      ConfigService config,
      MessageProperties messageProperties,
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService,
      HttpServletRequest request) {
    this.externalLoginFlow = externalLoginFlow;
    this.externalIdentityService = externalIdentityService;
    this.messageProperties = messageProperties;
    this.request = request;
    setPadding(false);
    setSpacing(false);
    setWidthFull();
    page = new PublicPageComponent(config, messageProperties, PublicPageComponent.Page.REGISTER);
    registrationTitle.addClassName("cosmos-headline");
    firstNameField.setId("external-register-first");
    lastNameField.setId("external-register-last");
    emailField.setId("external-register-email");
    firstNameField.setRequiredIndicatorVisible(true);
    lastNameField.setRequiredIndicatorVisible(true);
    emailField.setRequiredIndicatorVisible(true);
    firstNameField.setMaxLength(255);
    lastNameField.setMaxLength(255);
    emailField.setMaxLength(255);
    firstNameField.setWidthFull();
    lastNameField.setWidthFull();
    emailField.setWidthFull();
    completeRegistrationButton.setId("external-register-submit");
    completeRegistrationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    completeRegistrationButton.setPrefixComponent(AppIcon.USER_PLUS.create());
    completeRegistrationButton.addClickListener(event -> register());
    cancelRegistrationButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelRegistrationButton.addClickListener(event -> {
      externalLoginFlow.cancel(request);
      UI.getCurrent().navigate(LoginView.class);
    });
    PublicPanelComponent panel =
        new PublicPanelComponent(
            AppIcon.USER_PLUS.create(),
            registrationTitle,
            messageProperties::getExternalCompleteHint,
            new Component[] {registrationError, firstNameField, lastNameField, emailField},
            cancelRegistrationButton,
            completeRegistrationButton);
    panel.addClassName("external-registration-panel");
    page.getCardContainer().add(panel);
    add(page);
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> refreshTexts());
    refreshTexts();
  }

  private void refreshTexts() {
    registrationTitle.setText(messageProperties.getExternalCompleteTitle());
    firstNameField.setLabel(messageProperties.getRegistrationFirstNameField());
    lastNameField.setLabel(messageProperties.getRegistrationLastNameField());
    emailField.setLabel(messageProperties.getPublicEmailField());
    firstNameField.setPlaceholder(messageProperties.getAccountFirstNamePlaceholder());
    lastNameField.setPlaceholder(messageProperties.getAccountLastNamePlaceholder());
    emailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());
    completeRegistrationButton.setText(messageProperties.getBaseRegistrationButton());
    cancelRegistrationButton.setText(messageProperties.getAdminUsersCancel());
    if (registrationError.isVisible()) {
      registrationError.setText(currentErrorText());
    }
  }

  private String currentErrorText() {
    if (awaitingVerification) {
      return messageProperties.getExternalVerifyEmail();
    }
    return expiredRegistration
        ? messageProperties.getExternalDraftExpired()
        : messageProperties.getExternalError();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    registrationError.setVisible(false);
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      hideForm();
      registrationError.setText(messageProperties.getExternalError());
      registrationError.setVisible(true);
      return;
    }
    try {
      ExternalLoginFlow.Registration pending = externalLoginFlow.registration(request);
      firstNameField.setValue(value(pending.identity().firstName()));
      lastNameField.setValue(value(pending.identity().lastName()));
      emailField.setValue(value(pending.identity().email()));
    } catch (AccessDeniedException _) {
      expiredRegistration = true;
      hideForm();
      registrationError.setText(messageProperties.getExternalDraftExpired());
      registrationError.setVisible(true);
    }
  }

  private void register() {
    if (!ValidationHelper.validateRequiredInputs(
            List.of(firstNameField, lastNameField, emailField), messageProperties)
        || !ValidationHelper.validateEmailValidInputs(List.of(emailField), messageProperties)) {
      return;
    }
    completeRegistrationButton.setEnabled(false);
    try {
      ExternalLoginFlow.Registration pending = externalLoginFlow.registration(request);
      UserEntity user =
          externalIdentityService.register(
              pending.identity(),
              emailField.getValue(),
              firstNameField.getValue(),
              lastNameField.getValue(),
              VaadinSession.getCurrent().getLocale());
      externalLoginFlow.cancel(request);
      if (user.isEnabled()) {
        UI.getCurrent()
            .getPage()
            .setLocation(
                externalLoginFlow.finishLogin(
                    request,
                    (HttpServletResponse) VaadinResponse.getCurrent(),
                    user,
                    pending.identity().provider(),
                    pending.remember()));
      } else {
        hideForm();
        awaitingVerification = true;
        registrationError.setText(messageProperties.getExternalVerifyEmail());
        registrationError.setVisible(true);
      }
    } catch (IOException | RuntimeException _) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getExternalError,
              -1,
              NotificationHelper.NotificationType.ERROR);
    } finally {
      completeRegistrationButton.setEnabled(true);
    }
  }

  @Override
  public String getPageTitle() {
    return messageProperties.getExternalCompleteTitle();
  }

  private void hideForm() {
    firstNameField.setVisible(false);
    lastNameField.setVisible(false);
    emailField.setVisible(false);
    completeRegistrationButton.setVisible(false);
  }

  private String value(String value) {
    return value == null ? "" : value;
  }
}
