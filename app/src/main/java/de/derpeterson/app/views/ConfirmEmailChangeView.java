package de.derpeterson.app.views;

import static de.derpeterson.app.helper.logging.LogSafety.safeFailure;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
  private final Icon statusIcon = VaadinIcon.ENVELOPE.create();
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
    CosmosPageComponent page =
        new CosmosPageComponent(config, false, messages::getAccountCopyrightTemplate);
    statusIcon.addClassName("cosmos-status-icon");
    statusIcon.getElement().setAttribute("aria-hidden", "true");
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
    HorizontalLayout iconRow = new HorizontalLayout(statusIcon);
    iconRow.setWidthFull();
    iconRow.setPadding(false);
    iconRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    HorizontalLayout titleRow = new HorizontalLayout(title);
    titleRow.addClassName("cosmos-title-row");
    titleRow.setWidthFull();
    titleRow.setPadding(false);
    titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    HorizontalLayout intro = new HorizontalLayout(text);
    intro.addClassName("cosmos-intro");
    intro.setWidthFull();
    intro.setPadding(false);
    intro.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    Div actions = new Div(login, confirm);
    actions.addClassNames("cosmos-dialog-actions", "cosmos-email-confirm-actions");
    VerticalLayout form = new VerticalLayout(iconRow, titleRow, intro, actions);
    form.addClassName("cosmos-form");
    form.setAlignItems(FlexComponent.Alignment.STRETCH);
    form.setPadding(false);
    form.setSpacing(false);
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
    VaadinIcon icon =
        technicalFailure
            ? VaadinIcon.WARNING
            : switch (state) {
              case READY -> VaadinIcon.ENVELOPE;
              case SUCCESS -> VaadinIcon.CHECK_CIRCLE;
              case EXPIRED -> VaadinIcon.BELL;
              case INVALID, CONFLICT -> VaadinIcon.WARNING;
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
    statusIcon.setIcon(icon);
    statusIcon.removeClassNames(
        LumoUtility.TextColor.PRIMARY,
        LumoUtility.TextColor.SUCCESS,
        LumoUtility.TextColor.WARNING,
        LumoUtility.TextColor.ERROR);
    statusIcon.addClassName(color);
  }

  private void refreshTexts() {
    updateStatusIcon();
    title.setText(messages.getAccountEmailConfirmationTitle());
    text.setText(message().get());
    confirm.setText(messages.getAccountEmailConfirmationAction());
    confirm.setVisible(state == Confirmation.READY && !technicalFailure);
    login.setText(messages.getBaseLoginButton());
  }
}
