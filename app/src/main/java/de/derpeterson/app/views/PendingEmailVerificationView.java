package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
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
@StyleSheet("custom-theme/cosmos-email-verification.css")
public class PendingEmailVerificationView extends VerticalLayout
    implements BeforeEnterObserver, HasDynamicTitle {
  private final transient MessageProperties messageProperties;
  private final transient PendingEmailVerificationFlow pendingEmailVerificationFlow;
  private final transient HttpServletRequest httpRequest;
  private final PublicPageComponent publicPageComponent;

  // Bestätigungshinweis und zugehörige Aktionen
  private final EmailVerificationBadge verificationStatusBadge;
  private final H2 verificationTitleH2 = new H2();
  private final Span pendingAddressLabelSpan = new Span();
  private final Span pendingAddressSpan = new Span();
  private final Span verificationInstructionsSpan = new Span();
  private final Span verificationSpamHintSpan = new Span();
  private final Span spamFolderEmphasisSpan = new Span();
  private final Span resendDelayHintSpan = new Span();
  private final Button resendVerificationButton = new Button();
  private final Button cancelVerificationButton = new Button();

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
    pendingAddressLabelSpan.addClassName("pending-verification-address-label");
    verificationTitleH2.addClassName("cosmos-headline");
    verificationInstructionsSpan.addClassName("cosmos-form-text");
    verificationSpamHintSpan.addClassName("pending-verification-spam-hint");
    spamFolderEmphasisSpan.addClassName("pending-verification-folder-emphasis");

    resendVerificationButton.setId("resend-verification-email");
    resendVerificationButton.setPrefixComponent(AppIcon.PAPERPLANE.create());
    resendVerificationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resendVerificationButton.addClickListener(event -> resendVerificationEmail());

    cancelVerificationButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    cancelVerificationButton.addClassName("cosmos-delete-button");
    cancelVerificationButton.addClickListener(event -> {
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
    verificationTitleH2.setText(messageProperties.getEmailVerificationTitle());
    pendingAddressLabelSpan.setText(messageProperties.getPublicEmailField());
    refreshVerificationParagraphs();
    resendDelayHintSpan.setText(messageProperties.getEmailVerificationResendHint());
    resendVerificationButton.setText(messageProperties.getEmailVerificationResend());
    cancelVerificationButton.setText(messageProperties.getBaseLogoutButton());
    verificationStatusBadge.setText(messageProperties.getEmailVerificationPending());
  }

  private void refreshVerificationParagraphs() {
    verificationInstructionsSpan.setText(messageProperties.getEmailVerificationInstructions());
    String spamHintText = messageProperties.getEmailVerificationSpamHint();
    String spamFolderLabelText = messageProperties.getEmailVerificationSpamFolderLabel();
    int folderLabelStartIndex = spamHintText.indexOf(spamFolderLabelText);
    verificationSpamHintSpan.removeAll();

    // Auch ohne passende Hervorhebung bleibt der vollständige übersetzte Hinweis sichtbar.
    if (folderLabelStartIndex < 0) {
      verificationSpamHintSpan.setText(spamHintText);
      return;
    }

    spamFolderEmphasisSpan.setText(spamFolderLabelText);
    verificationSpamHintSpan.add(
        new Text(spamHintText.substring(0, folderLabelStartIndex)),
        spamFolderEmphasisSpan,
        new Text(spamHintText.substring(folderLabelStartIndex + spamFolderLabelText.length())));
  }

  private void composeVerificationPanel() {
    VerticalLayout verificationWarningTextLayout =
        new VerticalLayout(verificationInstructionsSpan, verificationSpamHintSpan);
    verificationWarningTextLayout.setPadding(false);
    verificationWarningTextLayout.setSpacing(false);
    verificationWarningTextLayout.addClassName("pending-verification-warning-text");

    HorizontalLayout verificationWarningLayout =
        new HorizontalLayout(AppIcon.ERROR.create(), verificationWarningTextLayout);
    verificationWarningLayout.setWidthFull();
    verificationWarningLayout.setPadding(false);
    verificationWarningLayout.setSpacing(false);
    verificationWarningLayout.setAlignItems(Alignment.CENTER);
    verificationWarningLayout.addClassName("cosmos-security-warning");

    SvgIcon resendDelaySvgIcon = AppIcon.CLOCK.create();
    resendDelaySvgIcon.getElement().setAttribute("aria-hidden", "true");
    HorizontalLayout resendDelayHintLayout =
        new HorizontalLayout(resendDelaySvgIcon, resendDelayHintSpan);
    resendDelayHintLayout.setWidthFull();
    resendDelayHintLayout.setPadding(false);
    resendDelayHintLayout.setSpacing(false);
    resendDelayHintLayout.setAlignItems(Alignment.CENTER);
    resendDelayHintLayout.addClassName("pending-verification-resend-hint");

    VerticalLayout verificationContentLayout =
        new VerticalLayout(
            createPendingAddressLayout(), verificationWarningLayout, resendDelayHintLayout);
    verificationContentLayout.setPadding(false);
    verificationContentLayout.setSpacing(false);
    verificationContentLayout.addClassName("pending-verification-content");

    HorizontalLayout verificationActionsLayout =
        new HorizontalLayout(cancelVerificationButton, resendVerificationButton);
    verificationActionsLayout.setWidthFull();
    verificationActionsLayout.setPadding(false);
    verificationActionsLayout.setSpacing(false);
    verificationActionsLayout.setJustifyContentMode(JustifyContentMode.END);
    verificationActionsLayout.addClassName("pending-verification-actions");
    EqualButtonWidths.configureEqualButtonWidths(verificationActionsLayout);

    publicPageComponent.getCardContainer().removeAll();
    SvgIcon verificationHeaderSvgIcon = AppIcon.ENVELOPE.create();
    verificationHeaderSvgIcon.addClassName("cosmos-status-icon--warning");
    publicPageComponent
        .getCardContainer()
        .add(
            new PublicPanelComponent(
                verificationHeaderSvgIcon,
                verificationTitleH2,
                messageProperties::getEmailVerificationPanelHint,
                new Component[] {verificationContentLayout},
                verificationActionsLayout));
  }

  private HorizontalLayout createPendingAddressLayout() {
    VerticalLayout addressDetailsLayout =
        new VerticalLayout(pendingAddressLabelSpan, pendingAddressSpan);
    addressDetailsLayout.setPadding(false);
    addressDetailsLayout.setSpacing(false);
    addressDetailsLayout.addClassName("pending-verification-address-details");

    HorizontalLayout pendingAddressLayout =
        new HorizontalLayout(addressDetailsLayout, verificationStatusBadge);
    pendingAddressLayout.setWidthFull();
    pendingAddressLayout.setPadding(false);
    pendingAddressLayout.setSpacing(false);
    pendingAddressLayout.setAlignItems(Alignment.CENTER);
    pendingAddressLayout.expand(addressDetailsLayout);
    pendingAddressLayout.addClassName("pending-verification-address");
    return pendingAddressLayout;
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
