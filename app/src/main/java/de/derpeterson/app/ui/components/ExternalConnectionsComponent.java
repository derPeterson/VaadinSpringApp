package de.derpeterson.app.ui.components;

import static de.derpeterson.app.security.ExternalProviderSettings.GOOGLE;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.ExternalLoginFlow.Purpose;
import de.derpeterson.app.security.ExternalProviderSettings;
import de.derpeterson.app.security.TwoFactorFlow;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;

/** Anbieterübersicht mit kontextbezogener, zweckgebundener Bestätigung der Kontoaktionen. */
@StyleSheet("custom-theme/cosmos-external-login.css")
public final class ExternalConnectionsComponent extends Div {
  private static final String LINK_RETURN =
      ExternalConnectionsComponent.class.getName() + ".linkReturn";
  private final transient ExternalLoginFlow externalLoginFlow;
  private final transient ExternalIdentityService externalIdentityService;
  private final transient HttpServletRequest request;
  private final transient MessageProperties messageProperties;
  private final Div providerOverviewDiv = new Div();
  private final ExternalIdentityConfirmationDialog identityConfirmationDialog;
  private SerializableRunnable openPasswordSettingsAction;
  private SerializableRunnable openEmailSettingsAction;
  private SerializableRunnable showProvidersAction = () -> {};

  public ExternalConnectionsComponent(
      ExternalLoginFlow externalLoginFlow,
      ExternalIdentityService externalIdentityService,
      HttpServletRequest request,
      MessageProperties messageProperties) {
    this.externalLoginFlow = externalLoginFlow;
    this.externalIdentityService = externalIdentityService;
    this.request = request;
    this.messageProperties = messageProperties;
    identityConfirmationDialog =
        new ExternalIdentityConfirmationDialog(
            externalLoginFlow, externalIdentityService, request, messageProperties);
    setId("account-external-connections");
    addClassName("account-external-connections");
    providerOverviewDiv.addClassName("external-connections-overview");
    EqualButtonWidths.configureEqualButtonWidths(providerOverviewDiv);
    add(providerOverviewDiv);
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshAccount());
    refreshAccount();
  }

  public void configureAccountActions(
      SerializableRunnable openPasswordSettingsAction,
      SerializableRunnable openEmailSettingsAction,
      SerializableRunnable showProvidersAction) {
    this.openPasswordSettingsAction = openPasswordSettingsAction;
    this.openEmailSettingsAction = openEmailSettingsAction;
    this.showProvidersAction = showProvidersAction;
    // Nach der Rückkehr vom Anbieter sichtbar fortsetzen, nie automatisch trennen.
    UI.getCurrent()
        .beforeClientResponse(
            identityConfirmationDialog,
            context -> {
              HttpSession session = request.getSession(false);
              if (session != null && Boolean.TRUE.equals(session.getAttribute(LINK_RETURN))) {
                session.removeAttribute(LINK_RETURN);
                showProvidersAction.run();
              }
              identityConfirmationDialog.consumeProviderReturn().ifPresent(this::requestAction);
            });
  }

  public ExternalIdentityConfirmationDialog getConfirmationDialog() {
    return identityConfirmationDialog;
  }

  public boolean developmentExceptionActive() {
    return externalLoginFlow.developmentExceptionActive();
  }

  public void requestPasswordChange() {
    requestAction(Purpose.PASSWORD);
  }

  public void requestEmailChange() {
    requestAction(Purpose.EMAIL);
  }

  public void refreshAccount() {
    providerOverviewDiv.removeAll();
    UserEntity user = externalLoginFlow.owner(request);
    List<String> connected = externalIdentityService.connected(user.getId());
    List<String> available = externalLoginFlow.providers();
    providerOverviewDiv.setVisible(!available.isEmpty() || !connected.isEmpty());
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      if (available.contains(provider) || connected.contains(provider)) {
        providerOverviewDiv.add(providerRow(provider, user, connected, available));
      }
    }
    identityConfirmationDialog.refreshTexts();
  }

  private Div providerRow(
      String providerName, UserEntity user, List<String> connected, List<String> available) {
    boolean linked = connected.contains(providerName);
    String providerBrandText = providerName.equals(GOOGLE) ? "Google" : "Microsoft";

    Image providerLogoImage =
        new Image(
            "icons/external-login/"
                + providerName
                + (providerName.equals(GOOGLE) ? ".png" : ".svg"),
            "");
    providerLogoImage.getElement().setAttribute("aria-hidden", "true");
    Div providerIconDiv = new Div(providerLogoImage);
    providerIconDiv.addClassName("cosmos-panel-icon");
    Badge state =
        new Badge(
            linked
                ? messageProperties.getExternalConnected()
                : messageProperties.getExternalNotConnected());
    if (linked) {
      state.addThemeVariants(BadgeVariant.SUCCESS);
    }

    Div providerHeadingDiv = new Div(new Span(providerBrandText), state);
    providerHeadingDiv.addClassName("external-connection-heading");
    boolean lastMethod =
        linked
            && user.getPassword() == null
            && connected.stream()
                .noneMatch(other -> !other.equals(providerName) && available.contains(other));

    String providerDetailText;
    if (!available.contains(providerName)) {
      providerDetailText = messageProperties.getExternalProviderUnavailable();
    } else if (lastMethod) {
      providerDetailText = messageProperties.getExternalLastMethod();
    } else {
      providerDetailText =
          linked
              ? messageProperties.getExternalConnectedHint()
              : messageProperties.getExternalNotConnectedHint();
    }
    Span providerExplanationSpan = new Span(providerDetailText);
    providerExplanationSpan.addClassName("account-entry-description");

    Div providerContentDiv = new Div(providerHeadingDiv, providerExplanationSpan);
    providerContentDiv.addClassNames("external-connection-copy", "account-entry-copy");

    Button providerActionButton =
        new Button(
            linked
                ? messageProperties.getExternalDisconnect(providerBrandText)
                : messageProperties.getExternalConnect(providerBrandText));
    providerActionButton.setId("external-connect-" + providerName);
    providerActionButton.setPrefixComponent((linked ? AppIcon.SIGN_OUT : AppIcon.SIGN_IN).create());
    providerActionButton.setClassName(CosmosClasses.DELETE_BUTTON, linked);
    providerActionButton.setEnabled(!lastMethod);
    providerActionButton.addClickListener(event ->
        requestAction(
            linked
                ? ExternalLoginFlow.unlinkPurpose(providerName)
                : ExternalLoginFlow.linkPurpose(providerName)));

    Div providerRowDiv = new Div(providerIconDiv, providerContentDiv, providerActionButton);
    providerRowDiv.addClassNames("external-connection-row", "account-entry");

    return providerRowDiv;
  }

  private void requestAction(Purpose requestedPurpose) {
    SerializableRunnable requestedIdentityAction =
        switch (requestedPurpose) {
          case PASSWORD -> openPasswordSettingsAction;
          case EMAIL -> openEmailSettingsAction;
          case LINK_GOOGLE -> () -> link(GOOGLE);
          case LINK_MICROSOFT -> () -> link("microsoft");
          case UNLINK_GOOGLE -> () -> unlink(GOOGLE);
          case UNLINK_MICROSOFT -> () -> unlink("microsoft");
          case ENROLL_FACTOR -> throw new IllegalArgumentException("Enrollment has its own view");
        };

    if (requestedIdentityAction == null) {
      return;
    }

    boolean changesAccountCredentials =
        requestedPurpose == Purpose.PASSWORD || requestedPurpose == Purpose.EMAIL;
    if (!changesAccountCredentials) {
      showProvidersAction.run();
    }

    boolean requestedPurposeConfirmed =
        externalLoginFlow.confirmedPurpose(request).filter(requestedPurpose::equals).isPresent();
    UserEntity accountOwner = externalLoginFlow.owner(request);
    boolean providerProofAvailable =
        externalIdentityService.connected(accountOwner.getId()).stream()
            .anyMatch(provider ->
                externalLoginFlow.providers().contains(provider)
                    && externalLoginFlow.canUseProviderProof(request, provider, requestedPurpose));

    if (!requestedPurposeConfirmed
        && !externalLoginFlow.factorEnrolled(request)
        && (externalLoginFlow.requiresFactor(request, requestedPurpose)
            || accountOwner.getPassword() == null && !providerProofAvailable)) {
      TwoFactorFlow.SETUP.enter(request);
      UI.getCurrent()
          .getPage()
          .setLocation(request.getContextPath() + TwoFactorFlow.SETUP.location());
      return;
    }
    if (!requestedPurposeConfirmed
        && accountOwner.getPassword() == null
        && !providerProofAvailable
        && !externalLoginFlow.requiresFactor(request, requestedPurpose)) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getExternalProofUnavailable,
              NotificationHelper.NotificationType.WARNING);
      return;
    }

    if (changesAccountCredentials
        && (requestedPurposeConfirmed
            || accountOwner.getPassword() != null
                && !providerProofAvailable
                && !externalLoginFlow.requiresFactor(request, requestedPurpose))) {
      // Lokale Konten behalten den bestehenden Änderungsdialog mit Passwortprüfung.
      requestedIdentityAction.run();
    } else {
      identityConfirmationDialog.open(requestedPurpose, requestedIdentityAction);
    }
  }

  private void link(String providerName) {
    String providerRedirectUrl =
        externalLoginFlow.beginLink(request, providerName, VaadinSession.getCurrent().getLocale());
    // Nur die Ansichtsposition merken, keinen Anmeldenachweis oder eine weitere Aktion.
    request.getSession(false).setAttribute(LINK_RETURN, true);
    UI.getCurrent().getPage().setLocation(providerRedirectUrl);
  }

  private void unlink(String providerName) {
    boolean retained = externalLoginFlow.disconnect(request, providerName);
    String destination =
        retained
            ? AppRouteConstants.ACCOUNT_PROVIDERS_LOCATION + "&"
            : AppRouteConstants.LOGIN_SECURITY_ROUTE + "?";
    UI.getCurrent()
        .getPage()
        .setLocation(
            request.getContextPath() + destination + ExternalLoginFlow.DISCONNECTED_PARAMETER);
  }
}
