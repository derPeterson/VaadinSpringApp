package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.EmailVerificationStatus;
import de.derpeterson.app.security.PendingEmailVerificationFlow;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.EmailVerificationBadge;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

/** Eine ausstehende Bestätigung erhält weder einen SecurityContext noch Portalrechte. */
@Route(AppRouteConstants.PENDING_EMAIL_VERIFICATION_ROUTE)
@AnonymousAllowed
public class PendingEmailVerificationView extends VerticalLayout
    implements BeforeEnterObserver, HasDynamicTitle {
  private final transient MessageProperties messageProperties;
  private final transient PendingEmailVerificationFlow pendingEmailVerificationFlow;
  private final transient HttpServletRequest httpRequest;
  private final PublicPageComponent publicPageComponent;

  // Bestätigungshinweis und zugehörige Aktionen
  private final EmailVerificationBadge verificationStatusBadge;
  private final H2 verificationTitleHeading = new H2();
  private final Span pendingAddressSpan = new Span();
  private final Span verificationInstructionsSpan = new Span();
  private final Button resendVerificationButton = new Button();
  private final Button returnToLoginButton = new Button();

  public PendingEmailVerificationView(
      MessageProperties messageProperties,
      PendingEmailVerificationFlow pendingEmailVerificationFlow,
      HttpServletRequest httpRequest,
      ConfigService configService) {
    this.messageProperties = messageProperties;
    this.pendingEmailVerificationFlow = pendingEmailVerificationFlow;
    this.httpRequest = httpRequest;

    setPadding(false);
    setSpacing(false);
    addClassName("pending-email-verification-view");

    publicPageComponent =
        new PublicPageComponent(
            configService, messageProperties, PublicPageComponent.Page.EMAIL_VERIFICATION_PENDING);
    verificationStatusBadge =
        new EmailVerificationBadge(messageProperties, EmailVerificationStatus.PENDING);
    pendingAddressSpan.setId("pending-verification-email");
    verificationInstructionsSpan.addClassName("cosmos-form-text");

    resendVerificationButton.setId("resend-verification-email");
    resendVerificationButton.setPrefixComponent(AppIcon.ENVELOPE.create());
    resendVerificationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resendVerificationButton.addClickListener(event -> resendVerificationEmail());

    returnToLoginButton.setPrefixComponent(AppIcon.ANGLE_LEFT.create());
    returnToLoginButton.addClickListener(event -> {
      PendingEmailVerificationFlow.cancel(httpRequest);
      UI.getCurrent().navigate(LoginView.class);
    });

    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());
          refreshVerificationTexts();
        });
    refreshVerificationTexts();
    composeVerificationPanel();
    add(publicPageComponent);
  }

  @Override
  public String getPageTitle() {
    return messageProperties.getEmailVerificationTitle();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    Optional<UserEntity> pendingUserEntity =
        pendingEmailVerificationFlow.currentAccount(httpRequest);
    if (pendingUserEntity.isEmpty()) {
      beforeEnterEvent.forwardTo(LoginView.class);
      return;
    }

    pendingAddressSpan.setText(pendingUserEntity.get().getEmail());
  }

  private void refreshVerificationTexts() {
    UI.getCurrent().getPage().setTitle(getPageTitle());
    verificationTitleHeading.setText(messageProperties.getEmailVerificationTitle());
    verificationInstructionsSpan.setText(messageProperties.getEmailVerificationSpamHint());
    resendVerificationButton.setText(messageProperties.getEmailVerificationResend());
    returnToLoginButton.setText(messageProperties.getEmailVerificationBack());
    verificationStatusBadge.setText(messageProperties.getEmailVerificationPending());
  }

  private void composeVerificationPanel() {
    SvgIcon warningSvgIcon = AppIcon.ENVELOPE.create();
    warningSvgIcon.addClassName("cosmos-status-icon--warning");

    Div verificationWarningDiv = new Div(AppIcon.ERROR.create(), verificationInstructionsSpan);
    verificationWarningDiv.addClassName("cosmos-security-warning");

    VerticalLayout instructionsLayout =
        new VerticalLayout(verificationStatusBadge, pendingAddressSpan, verificationWarningDiv);
    instructionsLayout.setPadding(false);

    publicPageComponent.getCardContainer().removeAll();
    publicPageComponent
        .getCardContainer()
        .add(
            new PublicPanelComponent(
                warningSvgIcon,
                verificationTitleHeading,
                messageProperties::getEmailVerificationIntro,
                new Component[] {instructionsLayout},
                returnToLoginButton,
                resendVerificationButton));
  }

  private void resendVerificationEmail() {
    try {
      boolean mailRequestAccepted = pendingEmailVerificationFlow.resendVerification(httpRequest);
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getEmailVerificationTitle,
              mailRequestAccepted
                  ? messageProperties::getEmailVerificationQueued
                  : messageProperties::getEmailVerificationWait,
              mailRequestAccepted
                  ? NotificationHelper.NotificationType.SUCCESS
                  : NotificationHelper.NotificationType.WARNING);
    } catch (IOException | RuntimeException failure) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseFailedMessage,
              NotificationHelper.NotificationType.ERROR);
    }
  }
}
