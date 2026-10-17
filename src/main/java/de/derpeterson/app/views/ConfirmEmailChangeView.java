package de.derpeterson.app.views;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.derpeterson.app.config.AppRouteConstants;
import de.derpeterson.app.events.LanguageChangeEvent;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.model.enums.AppRoute;
import de.derpeterson.app.service.AccountSecurityService;
import de.derpeterson.app.service.AccountSecurityService.Confirmation;
import de.derpeterson.app.service.ConfigService;
import de.derpeterson.app.ui.components.CosmosCardComponent;
import de.derpeterson.app.ui.components.CosmosPageComponent;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/** Öffnen eines Mail-Links zeigt nur die Vorschau; eine Änderung verlangt den Buttonklick. */
@Route(AppRouteConstants.EMAIL_CHANGE_ROUTE)
@PageTitle(AppRouteConstants.EMAIL_CHANGE_PAGE_TITLE)
@AnonymousAllowed
@Slf4j
public class ConfirmEmailChangeView extends Div implements HasUrlParameter<String> {
  private final transient MessageProperties messages;
  private final transient AccountSecurityService service;
  private final H1 title = new H1();
  private final Span text = new Span();
  private final Button confirm = new Button();
  private final Button login = new Button();
  private String token;
  private Confirmation state = Confirmation.INVALID;
  private boolean technicalFailure;

  public ConfirmEmailChangeView(
      MessageProperties messages, AccountSecurityService service, ConfigService config) {
    this.messages = messages;
    this.service = service;
    setId("account-email-confirmation");
    var page = new CosmosPageComponent(config, false, messages::getAccountCopyrightTemplate);
    var icon = VaadinIcon.ENVELOPE.create();
    icon.addClassName("cosmos-status-icon");
    icon.getElement().setAttribute("aria-hidden", "true");
    text.addClassName("cosmos-form-text");
    title.addClassName("cosmos-headline");
    confirm.setId("account-email-confirm-action");
    confirm.setPrefixComponent(VaadinIcon.CHECK.create());
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirm.addClickListener(event -> apply());
    login.setId("account-email-confirm-login");
    login.setPrefixComponent(VaadinIcon.SIGN_IN.create());
    // Nach der Bestätigung ist die bisherige Sitzung widerrufen. Zur Anmeldung deshalb
    // direkt im Browser wechseln, ohne einen weiteren RPC dieser Sitzung zu benötigen.
    login.addAttachListener(
        event ->
            login
                .getElement()
                .executeJs(
                    "this.onclick = () => window.location.assign(new URL($0, document.baseURI).href)",
                    AppRoute.LOGIN.getRoute()));
    var form = new Div(icon, title, text, confirm, login);
    form.addClassName("cosmos-form");
    icon.addClassNames(LumoUtility.AlignSelf.CENTER, LumoUtility.TextColor.PRIMARY);
    page.getCardContainer().add(new CosmosCardComponent(form));
    add(page);
    ComponentUtil.addListener(
        UI.getCurrent(),
        LanguageChangeEvent.class,
        event -> {
          VaadinSession.getCurrent().setLocale(event.getNewLocale());
          refreshTexts();
          NotificationHelper.getInstance().updateText();
        });
    refreshTexts();
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    this.token = token;
    technicalFailure = false;
    try {
      state = service.inspect(token);
    } catch (RuntimeException failure) {
      technicalFailure = true;
      log.error("Email change inspection failed", failure);
    }
    refreshTexts();
  }

  private void apply() {
    confirm.setEnabled(false);
    try {
      technicalFailure = false;
      state = service.confirm(token);
      refreshTexts();
      NotificationHelper.getInstance()
          .showNotification(
              state == Confirmation.SUCCESS
                  ? messages::getBaseSuccessTitle
                  : messages::getBaseFailedTitle,
              message(),
              state == Confirmation.SUCCESS
                  ? NotificationHelper.NotificationType.SUCCESS
                  : NotificationHelper.NotificationType.ERROR);
    } catch (RuntimeException failure) {
      technicalFailure = true;
      log.error("Email change confirmation failed", failure);
      refreshTexts();
      NotificationHelper.getInstance()
          .showNotification(
              messages::getBaseFailedTitle,
              messages::getAccountEmailConfirmationFailed,
              -1,
              NotificationHelper.NotificationType.ERROR);
    } finally {
      confirm.setEnabled(true);
    }
  }

  private Supplier<String> message() {
    if (technicalFailure) return messages::getAccountEmailConfirmationFailed;
    return switch (state) {
      case READY -> messages::getAccountEmailConfirmationReady;
      case SUCCESS -> messages::getAccountEmailConfirmationSuccess;
      case EXPIRED -> messages::getAccountEmailConfirmationExpired;
      case CONFLICT -> messages::getAccountEmailConfirmationConflict;
      case INVALID -> messages::getAccountEmailConfirmationInvalid;
    };
  }

  private void refreshTexts() {
    title.setText(messages.getAccountEmailConfirmationTitle());
    text.setText(message().get());
    confirm.setText(messages.getAccountEmailConfirmationAction());
    confirm.setVisible(state == Confirmation.READY && !technicalFailure);
    login.setText(messages.getBaseLoginButton());
  }
}
