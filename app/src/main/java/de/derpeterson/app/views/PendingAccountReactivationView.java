package de.derpeterson.app.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.PendingAccountReactivationFlow;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

/** Zeigt einen bestätigten Kontoinhaber, erteilt aber keine Portalberechtigung. */
@Route(AppRouteConstants.PENDING_ACCOUNT_REACTIVATION_ROUTE)
@AnonymousAllowed
@StyleSheet("custom-theme/cosmos-email-verification.css")
@StyleSheet("custom-theme/cosmos-account.css")
public final class PendingAccountReactivationView extends VerticalLayout
    implements BeforeEnterObserver, HasDynamicTitle {
  private final transient MessageProperties messageProperties;
  private final transient PendingAccountReactivationFlow pendingAccountReactivationFlow;
  private final transient HttpServletRequest httpRequest;

  private final H2 reactivationTitleH2 = new H2();
  private final Badge deactivatedStatusBadge = new Badge();
  private final Span accountEmailLabelSpan = new Span();
  private final Span accountEmailSpan = new Span();
  private final Span reactivationInstructionsSpan = new Span();
  private final Span spamHintSpan = new Span();
  private final Span spamFolderEmphasisSpan = new Span();
  private final Span resendHintSpan = new Span();
  private final Button resendLinkButton = new Button();
  private final Button cancelReactivationButton = new Button();
  private Instant deletionDeadline;
  private boolean developmentAccount;

  public PendingAccountReactivationView(
      MessageProperties messageProperties,
      PendingAccountReactivationFlow pendingAccountReactivationFlow,
      HttpServletRequest httpRequest,
      ConfigService configService) {
    this.messageProperties = messageProperties;
    this.pendingAccountReactivationFlow = pendingAccountReactivationFlow;
    this.httpRequest = httpRequest;
    setPadding(false);
    setSpacing(false);
    addClassName("pending-email-verification-view");

    VerticalLayout reactivationContentLayout = createReactivationContent();
    HorizontalLayout reactivationActionsLayout = createReactivationActions();

    PublicPageComponent publicPageComponent =
        new PublicPageComponent(
            configService,
            messageProperties,
            PublicPageComponent.Page.ACCOUNT_REACTIVATION_PENDING);
    SvgIcon reactivationHeaderSvgIcon = AppIcon.USER_PAUSE.create();
    reactivationHeaderSvgIcon.addClassName("cosmos-status-icon--warning");
    publicPageComponent
        .getCardContainer()
        .add(
            new PublicPanelComponent(
                reactivationHeaderSvgIcon,
                reactivationTitleH2,
                messageProperties::getAccountReactivationPendingHint,
                new Component[] {reactivationContentLayout},
                reactivationActionsLayout));
    add(publicPageComponent);
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshReactivationTexts());
    refreshReactivationTexts();
  }

  private VerticalLayout createReactivationContent() {
    reactivationTitleH2.addClassName("cosmos-headline");
    deactivatedStatusBadge.addThemeVariants(BadgeVariant.WARNING);
    deactivatedStatusBadge.addClassName("pending-reactivation-status-badge");
    accountEmailSpan.setId("pending-reactivation-email");
    accountEmailLabelSpan.addClassName("pending-verification-address-label");
    spamHintSpan.addClassName("pending-verification-spam-hint");
    spamFolderEmphasisSpan.addClassName("pending-verification-folder-emphasis");

    VerticalLayout accountAddressDetailsLayout =
        new VerticalLayout(accountEmailLabelSpan, accountEmailSpan);
    accountAddressDetailsLayout.setPadding(false);
    accountAddressDetailsLayout.setSpacing(false);
    accountAddressDetailsLayout.addClassName("pending-verification-address-details");
    HorizontalLayout accountAddressLayout =
        new HorizontalLayout(accountAddressDetailsLayout, deactivatedStatusBadge);
    accountAddressLayout.setWidthFull();
    accountAddressLayout.setPadding(false);
    accountAddressLayout.setSpacing(false);
    accountAddressLayout.setAlignItems(Alignment.CENTER);
    accountAddressLayout.expand(accountAddressDetailsLayout);
    accountAddressLayout.addClassName("pending-verification-address");

    VerticalLayout warningTextLayout =
        new VerticalLayout(reactivationInstructionsSpan, spamHintSpan);
    warningTextLayout.setPadding(false);
    warningTextLayout.setSpacing(false);
    warningTextLayout.addClassName("pending-verification-warning-text");
    HorizontalLayout reactivationWarningLayout =
        new HorizontalLayout(AppIcon.ERROR.create(), warningTextLayout);
    reactivationWarningLayout.setPadding(false);
    reactivationWarningLayout.setSpacing(false);
    reactivationWarningLayout.setAlignItems(Alignment.CENTER);
    reactivationWarningLayout.addClassName("cosmos-security-warning");
    HorizontalLayout resendHintLayout =
        new HorizontalLayout(AppIcon.CLOCK.create(), resendHintSpan);
    resendHintLayout.setPadding(false);
    resendHintLayout.setSpacing(false);
    resendHintLayout.addClassName("pending-verification-resend-hint");
    VerticalLayout reactivationContentLayout =
        new VerticalLayout(accountAddressLayout, reactivationWarningLayout, resendHintLayout);
    reactivationContentLayout.setPadding(false);
    reactivationContentLayout.setSpacing(false);
    reactivationContentLayout.addClassName("pending-verification-content");

    return reactivationContentLayout;
  }

  private HorizontalLayout createReactivationActions() {
    resendLinkButton.setId("resend-reactivation-email");
    resendLinkButton.setPrefixComponent(AppIcon.PAPERPLANE.create());
    resendLinkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resendLinkButton.addClickListener(event -> resendReactivationLink());
    cancelReactivationButton.setPrefixComponent(AppIcon.SIGN_OUT.create());
    cancelReactivationButton.addClassName("cosmos-delete-button");
    cancelReactivationButton.addClickListener(event -> {
      PendingAccountReactivationFlow.cancel(httpRequest);
      UI.getCurrent().navigate(LoginView.class);
    });
    HorizontalLayout reactivationActionsLayout =
        new HorizontalLayout(cancelReactivationButton, resendLinkButton);
    reactivationActionsLayout.setWidthFull();
    reactivationActionsLayout.setPadding(false);
    reactivationActionsLayout.setSpacing(false);
    reactivationActionsLayout.setJustifyContentMode(JustifyContentMode.END);
    reactivationActionsLayout.addClassName("pending-verification-actions");
    EqualButtonWidths.configureEqualButtonWidths(reactivationActionsLayout);

    return reactivationActionsLayout;
  }

  @Override
  public String getPageTitle() {
    return messageProperties.getAccountReactivationPendingTitle();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    Optional<UserEntity> accountUser = pendingAccountReactivationFlow.currentAccount(httpRequest);
    if (accountUser.isEmpty()) {
      beforeEnterEvent.forwardTo(LoginView.class);
      return;
    }
    accountEmailSpan.setText(accountUser.get().getEmail());
    deletionDeadline = accountUser.get().getAccountDeactivation().deletionAt();
    developmentAccount = accountUser.get().isDevelopmentAccount();
    resendLinkButton.setEnabled(!developmentAccount);
    refreshReactivationTexts();
  }

  private void refreshReactivationTexts() {
    reactivationTitleH2.setText(getPageTitle());
    deactivatedStatusBadge.setText(getPageTitle());
    accountEmailLabelSpan.setText(messageProperties.getPublicEmailField());
    cancelReactivationButton.setText(messageProperties.getBaseLogoutButton());
    resendLinkButton.setText(messageProperties.getEmailVerificationResend());
    refreshSpamFolderHint();
    resendHintSpan.setText(messageProperties.getAccountReactivationPendingWait());
    if (deletionDeadline != null) {
      String deletionDateText =
          DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                  .withLocale(UI.getCurrent().getLocale())
                  .withZone(ZoneOffset.UTC)
                  .format(deletionDeadline)
              + " UTC";
      reactivationInstructionsSpan.setText(
          messageProperties.getAccountReactivationPendingInstructions(deletionDateText));
    }
  }

  private void refreshSpamFolderHint() {
    spamHintSpan.removeAll();
    if (developmentAccount) {
      spamHintSpan.setText(messageProperties.getAccountLifecycleNoMail());
      return;
    }

    String spamHintText = messageProperties.getEmailVerificationSpamHint();
    String spamFolderLabelText = messageProperties.getEmailVerificationSpamFolderLabel();
    int folderLabelStartIndex = spamHintText.indexOf(spamFolderLabelText);
    if (folderLabelStartIndex < 0) {
      spamHintSpan.setText(spamHintText);
      return;
    }

    spamFolderEmphasisSpan.setText(spamFolderLabelText);
    spamHintSpan.add(
        new Text(spamHintText.substring(0, folderLabelStartIndex)),
        spamFolderEmphasisSpan,
        new Text(spamHintText.substring(folderLabelStartIndex + spamFolderLabelText.length())));
  }

  private void resendReactivationLink() {
    try {
      boolean mailQueued = pendingAccountReactivationFlow.resend(httpRequest);
      NotificationHelper.getInstance()
          .showNotification(
              this::getPageTitle,
              mailQueued
                  ? messageProperties::getEmailVerificationQueued
                  : messageProperties::getAccountReactivationPendingWait,
              mailQueued
                  ? NotificationHelper.NotificationType.SUCCESS
                  : NotificationHelper.NotificationType.WARNING);
    } catch (RuntimeException reactivationFailure) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getBaseFailedTitle,
              messageProperties::getBaseFailedMessage,
              NotificationHelper.NotificationType.ERROR);
    }
  }
}
