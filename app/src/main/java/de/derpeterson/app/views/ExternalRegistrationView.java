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
  private final transient HttpServletRequest httpServletRequest;
  private final PublicPageComponent externalRegistrationPageComponent;
  private final H2 registrationTitleH2 = new H2();
  private final TextField firstNameField = new TextField();
  private final TextField lastNameField = new TextField();
  private final EmailField registrationEmailField = new EmailField();
  private final Span registrationNoticeSpan = new Span();
  private final Button completeRegistrationButton = new Button();
  private final Button cancelRegistrationButton = new Button();
  private boolean isAwaitingEmailVerification;
  private boolean hasRegistrationDraftExpired;

  public ExternalRegistrationView(
      ConfigService configService,
      MessageProperties messageProperties,
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService,
      HttpServletRequest httpServletRequest) {
    this.externalLoginFlow = externalLoginFlow;
    this.externalIdentityService = externalIdentityService;
    this.messageProperties = messageProperties;
    this.httpServletRequest = httpServletRequest;

    setPadding(false);
    setSpacing(false);
    setWidthFull();
    externalRegistrationPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.REGISTER);

    registrationTitleH2.addClassName("cosmos-headline");

    firstNameField.setId("external-register-first");
    lastNameField.setId("external-register-last");
    registrationEmailField.setId("external-register-email");
    firstNameField.setRequiredIndicatorVisible(true);
    lastNameField.setRequiredIndicatorVisible(true);
    registrationEmailField.setRequiredIndicatorVisible(true);
    firstNameField.setMaxLength(255);
    lastNameField.setMaxLength(255);
    registrationEmailField.setMaxLength(255);
    firstNameField.setWidthFull();
    lastNameField.setWidthFull();
    registrationEmailField.setWidthFull();

    completeRegistrationButton.setId("external-register-submit");
    completeRegistrationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    completeRegistrationButton.setPrefixComponent(AppIcon.USER_PLUS.create());
    completeRegistrationButton.addClickListener(_ -> completeExternalRegistration());

    cancelRegistrationButton.setPrefixComponent(AppIcon.CLOSE.create());
    cancelRegistrationButton.addClickListener(_ -> cancelExternalRegistration());

    PublicPanelComponent externalRegistrationPanelComponent =
        new PublicPanelComponent(
            AppIcon.USER_PLUS.create(),
            registrationTitleH2,
            messageProperties::getExternalCompleteHint,
            new Component[] {
              registrationNoticeSpan, firstNameField, lastNameField, registrationEmailField
            },
            cancelRegistrationButton,
            completeRegistrationButton);
    externalRegistrationPanelComponent.addClassName("external-registration-panel");
    externalRegistrationPageComponent.getCardContainer().add(externalRegistrationPanelComponent);
    add(externalRegistrationPageComponent);

    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, _ -> refreshExternalRegistrationTexts());
    refreshExternalRegistrationTexts();
  }

  private void cancelExternalRegistration() {
    externalLoginFlow.cancel(httpServletRequest);
    UI.getCurrent().navigate(LoginView.class);
  }

  private void refreshExternalRegistrationTexts() {
    registrationTitleH2.setText(messageProperties.getExternalCompleteTitle());
    firstNameField.setLabel(messageProperties.getRegistrationFirstNameField());
    lastNameField.setLabel(messageProperties.getRegistrationLastNameField());
    registrationEmailField.setLabel(messageProperties.getPublicEmailField());
    firstNameField.setPlaceholder(messageProperties.getAccountFirstNamePlaceholder());
    lastNameField.setPlaceholder(messageProperties.getAccountLastNamePlaceholder());
    registrationEmailField.setPlaceholder(messageProperties.getAccountEmailPlaceholder());

    completeRegistrationButton.setText(messageProperties.getBaseRegistrationButton());
    cancelRegistrationButton.setText(messageProperties.getAdminUsersCancel());

    if (registrationNoticeSpan.isVisible()) {
      registrationNoticeSpan.setText(resolveRegistrationNoticeText());
    }
  }

  private String resolveRegistrationNoticeText() {
    if (isAwaitingEmailVerification) {
      return messageProperties.getExternalVerifyEmail();
    }
    return hasRegistrationDraftExpired
        ? messageProperties.getExternalDraftExpired()
        : messageProperties.getExternalError();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    registrationNoticeSpan.setVisible(false);
    if (beforeEnterEvent.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      hideRegistrationInputs();
      registrationNoticeSpan.setText(messageProperties.getExternalError());
      registrationNoticeSpan.setVisible(true);
      return;
    }

    try {
      ExternalLoginFlow.Registration pendingExternalRegistration =
          externalLoginFlow.registration(httpServletRequest);
      firstNameField.setValue(
          orEmptyRegistrationValue(pendingExternalRegistration.identity().firstName()));
      lastNameField.setValue(
          orEmptyRegistrationValue(pendingExternalRegistration.identity().lastName()));
      registrationEmailField.setValue(
          orEmptyRegistrationValue(pendingExternalRegistration.identity().email()));
    } catch (AccessDeniedException _) {
      hasRegistrationDraftExpired = true;
      hideRegistrationInputs();
      registrationNoticeSpan.setText(messageProperties.getExternalDraftExpired());
      registrationNoticeSpan.setVisible(true);
    }
  }

  private void completeExternalRegistration() {
    if (!ValidationHelper.validateRequiredInputs(
            List.of(firstNameField, lastNameField, registrationEmailField), messageProperties)
        || !ValidationHelper.validateEmailValidInputs(
            List.of(registrationEmailField), messageProperties)) {
      return;
    }

    completeRegistrationButton.setEnabled(false);
    try {
      ExternalLoginFlow.Registration pendingExternalRegistration =
          externalLoginFlow.registration(httpServletRequest);
      UserEntity registeredUserEntity =
          externalIdentityService.register(
              pendingExternalRegistration.identity(),
              registrationEmailField.getValue(),
              firstNameField.getValue(),
              lastNameField.getValue(),
              VaadinSession.getCurrent().getLocale());

      externalLoginFlow.cancel(httpServletRequest);
      if (registeredUserEntity.isEnabled()) {
        UI.getCurrent()
            .getPage()
            .setLocation(
                externalLoginFlow.finishLogin(
                    httpServletRequest,
                    (HttpServletResponse) VaadinResponse.getCurrent(),
                    registeredUserEntity,
                    pendingExternalRegistration.identity().provider(),
                    pendingExternalRegistration.remember()));
      } else {
        hideRegistrationInputs();
        isAwaitingEmailVerification = true;
        registrationNoticeSpan.setText(messageProperties.getExternalVerifyEmail());
        registrationNoticeSpan.setVisible(true);
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

  private void hideRegistrationInputs() {
    firstNameField.setVisible(false);
    lastNameField.setVisible(false);
    registrationEmailField.setVisible(false);
    completeRegistrationButton.setVisible(false);
  }

  private String orEmptyRegistrationValue(String registrationValue) {
    return registrationValue == null ? "" : registrationValue;
  }
}
