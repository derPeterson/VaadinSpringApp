package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
import de.derpeterson.app.ui.components.AppIcon;
import de.derpeterson.app.ui.components.PublicPageComponent;
import de.derpeterson.app.ui.components.PublicPanelComponent;
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
  private final SvgIcon statusIcon = AppIcon.ENVELOPE.create();
  private final H1 title = new H1();
  private final Span text = new Span();
  private final Span consequenceText = new Span();
  private final Div consequence = new Div();
  private final Button confirm = new Button();
  private final Button login = new Button();
  private final Div actions = new Div();
  private String token;
  private Confirmation state = Confirmation.INVALID;
  private boolean technicalFailure;

  public ConfirmEmailChangeView(
      MessageProperties messages, AccountSecurityService service, ConfigService config) {
    this.messages = messages;
    this.service = service;
    setId("account-email-confirmation");
    PublicPageComponent page =
        new PublicPageComponent(config, messages, PublicPageComponent.Page.EMAIL_CONFIRM);
    statusIcon.addClassName("cosmos-status-icon");
    statusIcon.getElement().setAttribute("aria-hidden", "true");
    text.addClassName("cosmos-form-text");
    title.addClassName("cosmos-headline");
    confirm.setId("account-email-confirm-action");
    confirm.setPrefixComponent(AppIcon.CHECK.create());
    confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    confirm.addClickListener(event -> apply());
    login.setId("account-email-confirm-login");
    login.setPrefixComponent(AppIcon.SIGN_IN.create());
    consequence.setId("account-email-confirm-hint");
    consequence.addClassName("cosmos-panel-hint");
    consequence.add(AppIcon.INFO_CIRCLE.create(), consequenceText);
    actions.add(login, confirm);
    // Nach der Bestätigung ist die bisherige Sitzung widerrufen. Zur Anmeldung deshalb
    // direkt im Browser wechseln, ohne einen weiteren RPC dieser Sitzung zu benötigen.
    login.addAttachListener(
        event ->
            login
                .getElement()
                .executeJs(
                    "this.onclick = () => window.location.assign(new URL($0, document.baseURI).href)",
                    AppRoute.LOGIN.getRoute()));
    page.getCardContainer()
        .add(
            new PublicPanelComponent(
                statusIcon,
                title,
                messages::getPublicEmailSubtitle,
                new com.vaadin.flow.component.Component[] {text, consequence},
                actions));
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
      log.error("Email change inspection failed", safeFailure(failure));
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
      log.error("Email change confirmation failed", safeFailure(failure));
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
    if (technicalFailure) {
      return messages::getAccountEmailConfirmationFailed;
    }
    return switch (state) {
      case READY -> messages::getAccountEmailConfirmationReady;
      case SUCCESS -> messages::getAccountEmailConfirmationSuccess;
      case EXPIRED -> messages::getAccountEmailConfirmationExpired;
      case CONFLICT -> messages::getAccountEmailConfirmationConflict;
      case INVALID -> messages::getAccountEmailConfirmationInvalid;
    };
  }

  private void updateStatusIcon() {
    AppIcon icon =
        technicalFailure
            ? AppIcon.WARNING
            : switch (state) {
              case READY -> AppIcon.ENVELOPE;
              case SUCCESS -> AppIcon.CHECK_CIRCLE;
              case EXPIRED, INVALID, CONFLICT -> AppIcon.WARNING;
            };
    String color =
        technicalFailure
            ? LumoUtility.TextColor.ERROR
            : switch (state) {
              case READY -> LumoUtility.TextColor.PRIMARY;
              case SUCCESS -> LumoUtility.TextColor.SUCCESS;
              case EXPIRED -> LumoUtility.TextColor.WARNING;
              case INVALID, CONFLICT -> LumoUtility.TextColor.ERROR;
            };
    statusIcon.setSrc(icon.getSource());
    statusIcon.removeClassNames(
        LumoUtility.TextColor.PRIMARY,
        LumoUtility.TextColor.SUCCESS,
        LumoUtility.TextColor.WARNING,
        LumoUtility.TextColor.ERROR);
    statusIcon.addClassName(color);
    statusIcon.setClassName(
        "cosmos-status-icon--warning", state == Confirmation.EXPIRED && !technicalFailure);
  }

  private String confirmationTitle() {
    if (technicalFailure) {
      return messages.getBaseFailedTitle();
    }
    return switch (state) {
      case READY -> messages.getPublicEmailFormTitle();
      case SUCCESS -> messages.getPublicEmailSuccessTitle();
      case EXPIRED -> messages.getPublicEmailExpiredTitle();
      case INVALID -> messages.getPublicEmailInvalidTitle();
      case CONFLICT -> messages.getPublicEmailConflictTitle();
    };
  }

  private void refreshTexts() {
    updateStatusIcon();
    boolean ready = state == Confirmation.READY && !technicalFailure;
    setClassName("public-email-outcome", !ready);
    title.setText(confirmationTitle());
    text.setText(message().get());
    consequenceText.setText(messages.getPublicEmailConfirmationHint());
    consequence.setVisible(ready);
    actions.setClassName("public-panel-actions", ready);
    confirm.setText(messages.getAccountEmailConfirmationAction());
    confirm.setVisible(ready);
    login.setText(messages.getBaseLoginButton());
    if (ready) {
      login.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }
  }
}
