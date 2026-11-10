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
  private final Div providerOverview = new Div();
  private final ExternalIdentityConfirmationDialog identityConfirmationDialog;
  private SerializableRunnable openPasswordSettings;
  private SerializableRunnable openEmailSettings;
  private SerializableRunnable showProviders = () -> {};

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
    providerOverview.addClassName("external-connections-overview");
    EqualButtonWidths.configureEqualButtonWidths(providerOverview);
    add(providerOverview);
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshAccount());
    refreshAccount();
  }

  public void configureAccountActions(
      SerializableRunnable openPasswordSettings,
      SerializableRunnable openEmailSettings,
      SerializableRunnable showProviders) {
    this.openPasswordSettings = openPasswordSettings;
    this.openEmailSettings = openEmailSettings;
    this.showProviders = showProviders;
    // Nach der Rückkehr vom Anbieter sichtbar fortsetzen, nie automatisch trennen.
    UI.getCurrent()
        .beforeClientResponse(
            identityConfirmationDialog,
            context -> {
              HttpSession session = request.getSession(false);
              if (session != null && Boolean.TRUE.equals(session.getAttribute(LINK_RETURN))) {
                session.removeAttribute(LINK_RETURN);
                showProviders.run();
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
    providerOverview.removeAll();
    UserEntity user = externalLoginFlow.owner(request);
    List<String> connected = externalIdentityService.connected(user.getId());
    List<String> available = externalLoginFlow.providers();
    providerOverview.setVisible(!available.isEmpty() || !connected.isEmpty());
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      if (available.contains(provider) || connected.contains(provider)) {
        providerOverview.add(providerRow(provider, user, connected, available));
      }
    }
    identityConfirmationDialog.refreshTexts();
  }

  private Div providerRow(
      String provider, UserEntity user, List<String> connected, List<String> available) {
    boolean linked = connected.contains(provider);
    String brand = provider.equals(GOOGLE) ? "Google" : "Microsoft";
    Image logo =
        new Image(
            "icons/external-login/" + provider + (provider.equals(GOOGLE) ? ".png" : ".svg"), "");
    logo.getElement().setAttribute("aria-hidden", "true");
    Div icon = new Div(logo);
    icon.addClassName("cosmos-panel-icon");
    Badge state =
        new Badge(
            linked
                ? messageProperties.getExternalConnected()
                : messageProperties.getExternalNotConnected());
    if (linked) {
      state.addThemeVariants(BadgeVariant.SUCCESS);
    }
    Div heading = new Div(new Span(brand), state);
    heading.addClassName("external-connection-heading");
    boolean lastMethod =
        linked
            && user.getPassword() == null
            && connected.stream()
                .noneMatch(other -> !other.equals(provider) && available.contains(other));
    String detail;
    if (!available.contains(provider)) {
      detail = messageProperties.getExternalProviderUnavailable();
    } else if (lastMethod) {
      detail = messageProperties.getExternalLastMethod();
    } else {
      detail =
          linked
              ? messageProperties.getExternalConnectedHint()
              : messageProperties.getExternalNotConnectedHint();
    }
    Span explanation = new Span(detail);
    explanation.addClassName("account-entry-description");
    Div copy = new Div(heading, explanation);
    copy.addClassNames("external-connection-copy", "account-entry-copy");
    Button action =
        new Button(
            linked
                ? messageProperties.getExternalDisconnect(brand)
                : messageProperties.getExternalConnect(brand));
    action.setId("external-connect-" + provider);
    action.setPrefixComponent((linked ? AppIcon.SIGN_OUT : AppIcon.SIGN_IN).create());
    action.setClassName(CosmosClasses.DELETE_BUTTON, linked);
    action.setEnabled(!lastMethod);
    action.addClickListener(event ->
        requestAction(
            linked
                ? ExternalLoginFlow.unlinkPurpose(provider)
                : ExternalLoginFlow.linkPurpose(provider)));
    Div row = new Div(icon, copy, action);
    row.addClassNames("external-connection-row", "account-entry");
    return row;
  }

  private void requestAction(Purpose purpose) {
    SerializableRunnable action =
        switch (purpose) {
          case PASSWORD -> openPasswordSettings;
          case EMAIL -> openEmailSettings;
          case LINK_GOOGLE -> () -> link(GOOGLE);
          case LINK_MICROSOFT -> () -> link("microsoft");
          case UNLINK_GOOGLE -> () -> unlink(GOOGLE);
          case UNLINK_MICROSOFT -> () -> unlink("microsoft");
          case ENROLL_FACTOR -> throw new IllegalArgumentException("Enrollment has its own view");
        };
    if (action == null) {
      return;
    }
    boolean accountChange = purpose == Purpose.PASSWORD || purpose == Purpose.EMAIL;
    if (!accountChange) {
      showProviders.run();
    }
    boolean confirmed =
        externalLoginFlow.confirmedPurpose(request).filter(purpose::equals).isPresent();
    UserEntity user = externalLoginFlow.owner(request);
    boolean providerProofAvailable =
        externalIdentityService.connected(user.getId()).stream()
            .anyMatch(provider ->
                externalLoginFlow.providers().contains(provider)
                    && externalLoginFlow.canUseProviderProof(request, provider, purpose));
    if (!confirmed
        && !externalLoginFlow.factorEnrolled(request)
        && (externalLoginFlow.requiresFactor(request, purpose)
            || user.getPassword() == null && !providerProofAvailable)) {
      TwoFactorFlow.SETUP.enter(request);
      UI.getCurrent()
          .getPage()
          .setLocation(request.getContextPath() + TwoFactorFlow.SETUP.location());
      return;
    }
    if (!confirmed
        && user.getPassword() == null
        && !providerProofAvailable
        && !externalLoginFlow.requiresFactor(request, purpose)) {
      NotificationHelper.getInstance()
          .showNotification(
              messageProperties::getExternalProofUnavailable,
              NotificationHelper.NotificationType.WARNING);
      return;
    }
    if (accountChange
        && (confirmed
            || user.getPassword() != null
                && !providerProofAvailable
                && !externalLoginFlow.requiresFactor(request, purpose))) {
      // Lokale Konten behalten den bestehenden Änderungsdialog mit Passwortprüfung.
      action.run();
    } else {
      identityConfirmationDialog.open(purpose, action);
    }
  }

  private void link(String provider) {
    String destination =
        externalLoginFlow.beginLink(request, provider, VaadinSession.getCurrent().getLocale());
    // Nur die Ansichtsposition merken, keinen Anmeldenachweis oder eine weitere Aktion.
    request.getSession(false).setAttribute(LINK_RETURN, true);
    UI.getCurrent().getPage().setLocation(destination);
  }

  private void unlink(String provider) {
    boolean retained = externalLoginFlow.disconnect(request, provider);
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
