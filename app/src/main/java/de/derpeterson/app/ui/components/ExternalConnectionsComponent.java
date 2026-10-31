package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.server.VaadinSession;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.security.ExternalLoginFlow;
import de.derpeterson.app.security.ExternalLoginFlow.Purpose;
import de.derpeterson.app.service.ExternalIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/** Zweckgebundene Bestätigung und Verwaltung der verbundenen Anbieter im Kontobereich. */
@StyleSheet("custom-theme/cosmos-external-login.css")
public final class ExternalConnectionsComponent extends Div {
  private final transient ExternalLoginFlow flow;
  private final transient ExternalIdentityService identities;
  private final transient HttpServletRequest request;
  private final transient MessageProperties texts;

  public ExternalConnectionsComponent(
      ExternalLoginFlow flow,
      ExternalIdentityService identities,
      HttpServletRequest request,
      MessageProperties texts) {
    this.flow = flow;
    this.identities = identities;
    this.request = request;
    this.texts = texts;
    setId("account-external-connections");
    addClassName("account-external-connections");
    ComponentUtil.addListener(UI.getCurrent(), LanguageChangeEvent.class, event -> render());
    render();
  }

  private void render() {
    removeAll();
    UserEntity user = flow.owner(request);
    List<String> connected = identities.connected(user.getId());
    if (flow.providers().isEmpty() && connected.isEmpty()) {
      setVisible(false);
      return;
    }
    add(new H3(texts.getExternalConnections()));
    Span hint = new Span(texts.getExternalProofHint());
    hint.addClassName("cosmos-panel-description");
    add(hint);
    ComboBox<Purpose> purpose = new ComboBox<>(texts.getExternalAction());
    purpose.setId("external-security-action");
    purpose.setItems(Purpose.values());
    purpose.setItemLabelGenerator(this::label);
    purpose.setValue(flow.confirmedPurpose(request).orElse(Purpose.PASSWORD));
    purpose.setWidthFull();
    PortalFields.styleChoice(purpose, this::label, true);
    add(purpose);
    Span result = new Span();
    result.setId("external-security-result");
    if (flow.confirmedPurpose(request).isPresent()) {
      result.setText(texts.getExternalProofReady());
    }
    add(result);
    if (user.getPassword() != null) {
      PasswordField password = new PasswordField(texts.getAccountPasswordDialogCurrentPassword());
      password.setId("external-confirm-password");
      password.setWidthFull();
      password.setPlaceholder(texts.getAccountPasswordPlaceholder());
      Button confirm =
          new Button(
              texts.getExternalProof(),
              AppIcon.CHECK.create(),
              event -> {
                try {
                  flow.confirmPassword(request, password.getValue(), purpose.getValue());
                  result.setText(texts.getExternalProofReady());
                } catch (RuntimeException failure) {
                  error();
                } finally {
                  password.clear();
                }
              });
      confirm.setId("external-confirm-local");
      add(password, confirm);
    }
    for (String provider : connected) {
      if (!flow.providers().contains(provider)) {
        continue;
      }
      String brand = brand(provider);
      Button proof =
          new Button(
              texts.getExternalProofWith(brand),
              AppIcon.SIGN_IN.create(),
              event -> {
                try {
                  UI.getCurrent()
                      .getPage()
                      .setLocation(
                          flow.beginProof(
                              request,
                              provider,
                              purpose.getValue(),
                              VaadinSession.getCurrent().getLocale()));
                } catch (RuntimeException failure) {
                  error();
                }
              });
      proof.setId("external-confirm-" + provider);
      add(proof);
    }
    for (String provider : flow.providers()) {
      Button action =
          new Button(
              connected.contains(provider)
                  ? texts.getExternalDisconnect(brand(provider))
                  : texts.getExternalConnect(brand(provider)));
      action.setId("external-connect-" + provider);
      action.setPrefixComponent(
          (connected.contains(provider) ? AppIcon.SIGN_OUT : AppIcon.SIGN_IN).create());
      action.setEnabled(
          !connected.contains(provider)
              || user.getPassword() != null
              || connected.stream()
                  .anyMatch(other -> !other.equals(provider) && flow.providers().contains(other)));
      action.addClickListener(
          event -> {
            try {
              if (connected.contains(provider)) {
                UserEntity owner = flow.owner(request);
                flow.authorize(request, owner, ExternalLoginFlow.unlinkPurpose(provider), true);
                identities.unlink(owner.getId(), owner.getAccessVersion(), provider);
                UI.getCurrent().getPage().setLocation("/login");
              } else {
                UI.getCurrent()
                    .getPage()
                    .setLocation(
                        flow.beginLink(request, provider, VaadinSession.getCurrent().getLocale()));
              }
            } catch (RuntimeException failure) {
              error();
            }
          });
      add(action);
    }
    Span note = new Span(texts.getExternalDisconnectHint());
    note.addClassName("cosmos-panel-description");
    add(note);
  }

  public void refreshAccount() {
    render();
  }

  private String label(Purpose purpose) {
    if (purpose == null) {
      return "";
    }
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

  private void error() {
    NotificationHelper.getInstance()
        .showNotification(
            texts::getBaseFailedTitle,
            texts::getExternalError,
            -1,
            NotificationHelper.NotificationType.ERROR);
  }
}
