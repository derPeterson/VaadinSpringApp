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
  private final transient ExternalLoginFlow flow;
  private final transient ExternalIdentityService identities;
  private final transient HttpServletRequest request;
  private final transient MessageProperties texts;
  private final Div overview = new Div();
  private final ExternalIdentityConfirmationDialog confirmation;
  private SerializableRunnable passwordAction;
  private SerializableRunnable emailAction;
  private SerializableRunnable showProviders = () -> {};

  public ExternalConnectionsComponent(
      ExternalLoginFlow flow,
      ExternalIdentityService identities,
      HttpServletRequest request,
      MessageProperties texts) {
    this.flow = flow;
    this.identities = identities;
    this.request = request;
    this.texts = texts;
    confirmation = new ExternalIdentityConfirmationDialog(flow, identities, request, texts);
    setId("account-external-connections");
    addClassName("account-external-connections");
    overview.addClassName("external-connections-overview");
    EqualButtonWidths.configure(overview);
    add(overview);
    ComponentUtil.addListener(
        UI.getCurrent(), LanguageChangeEvent.class, event -> refreshAccount());
    refreshAccount();
  }

  public void configureAccountActions(
      SerializableRunnable passwordAction,
      SerializableRunnable emailAction,
      SerializableRunnable showProviders) {
    this.passwordAction = passwordAction;
    this.emailAction = emailAction;
    this.showProviders = showProviders;
    // Nach der Rückkehr vom Anbieter sichtbar fortsetzen, nie automatisch trennen.
    UI.getCurrent()
        .beforeClientResponse(
            confirmation,
            context -> {
              HttpSession session = request.getSession(false);
              if (session != null && Boolean.TRUE.equals(session.getAttribute(LINK_RETURN))) {
                session.removeAttribute(LINK_RETURN);
                showProviders.run();
              }
              confirmation.consumeProviderReturn().ifPresent(this::requestAction);
            });
  }

  public ExternalIdentityConfirmationDialog getConfirmationDialog() {
    return confirmation;
  }

  public boolean developmentExceptionActive() {
    return flow.developmentExceptionActive();
  }

  public void requestPasswordChange() {
    requestAction(Purpose.PASSWORD);
  }

  public void requestEmailChange() {
    requestAction(Purpose.EMAIL);
  }

  public void refreshAccount() {
    overview.removeAll();
    UserEntity user = flow.owner(request);
    List<String> connected = identities.connected(user.getId());
    List<String> available = flow.providers();
    overview.setVisible(!available.isEmpty() || !connected.isEmpty());
    for (String provider : ExternalProviderSettings.PROVIDERS) {
      if (available.contains(provider) || connected.contains(provider)) {
        overview.add(providerRow(provider, user, connected, available));
      }
    }
    confirmation.refreshTexts();
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
        new Badge(linked ? texts.getExternalConnected() : texts.getExternalNotConnected());
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
      detail = texts.getExternalProviderUnavailable();
    } else if (lastMethod) {
      detail = texts.getExternalLastMethod();
    } else {
      detail = linked ? texts.getExternalConnectedHint() : texts.getExternalNotConnectedHint();
    }
    Span explanation = new Span(detail);
    explanation.addClassName("account-entry-description");
    Div copy = new Div(heading, explanation);
    copy.addClassNames("external-connection-copy", "account-entry-copy");
    Button action =
        new Button(linked ? texts.getExternalDisconnect(brand) : texts.getExternalConnect(brand));
    action.setId("external-connect-" + provider);
    action.setPrefixComponent((linked ? AppIcon.SIGN_OUT : AppIcon.SIGN_IN).create());
    action.setClassName(CosmosClasses.DELETE_BUTTON, linked);
    action.setEnabled(!lastMethod);
    action.addClickListener(
        event ->
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
          case PASSWORD -> passwordAction;
          case EMAIL -> emailAction;
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
    boolean confirmed = flow.confirmedPurpose(request).filter(purpose::equals).isPresent();
    UserEntity user = flow.owner(request);
    boolean providerProofAvailable =
        identities.connected(user.getId()).stream()
            .anyMatch(
                provider ->
                    flow.providers().contains(provider)
                        && flow.canUseProviderProof(request, provider, purpose));
    if (!confirmed
        && !flow.factorEnrolled(request)
        && (flow.requiresFactor(request, purpose)
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
        && !flow.requiresFactor(request, purpose)) {
      NotificationHelper.getInstance()
          .showNotification(
              texts::getExternalProofUnavailable, NotificationHelper.NotificationType.WARNING);
      return;
    }
    if (accountChange
        && (confirmed
            || user.getPassword() != null
                && !providerProofAvailable
                && !flow.requiresFactor(request, purpose))) {
      // Lokale Konten behalten den bestehenden Änderungsdialog mit Passwortprüfung.
      action.run();
    } else {
      confirmation.open(purpose, action);
    }
  }

  private void link(String provider) {
    String destination = flow.beginLink(request, provider, VaadinSession.getCurrent().getLocale());
    // Nur die Ansichtsposition merken, keinen Anmeldenachweis oder eine weitere Aktion.
    request.getSession(false).setAttribute(LINK_RETURN, true);
    UI.getCurrent().getPage().setLocation(destination);
  }

  private void unlink(String provider) {
    boolean retained = flow.disconnect(request, provider);
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
