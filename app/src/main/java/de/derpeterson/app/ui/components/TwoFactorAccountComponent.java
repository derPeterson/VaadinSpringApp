package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.helper.ui.NotificationHelper;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.security.mfa.TwoFactorAction;
import de.derpeterson.app.security.mfa.TwoFactorAuthorization;
import de.derpeterson.app.security.mfa.TwoFactorManagement;
import jakarta.servlet.http.HttpServletRequest;

/** Eigener Authenticator im bestehenden Portalzugang, ohne Geheimnisse im Komponentenbaum. */
@StyleSheet("custom-theme/cosmos-mfa.css")
public final class TwoFactorAccountComponent extends Div {
  private final transient MessageProperties texts;
  private final transient TwoFactorAuthorization authorization;
  private final transient TwoFactorManagement management;
  private final transient HttpServletRequest request;
  private final Div entry = new Div();
  private final TwoFactorActionDialog confirmation;

  public TwoFactorAccountComponent(
      MessageProperties texts,
      TwoFactorAuthorization authorization,
      TwoFactorManagement management,
      HttpServletRequest request) {
    this.texts = texts;
    this.authorization = authorization;
    this.management = management;
    this.request = request;
    setId("account-two-factor");
    confirmation = new TwoFactorActionDialog(texts, authorization, request);
    add(entry, confirmation);
    refresh();
  }

  public void refresh() {
    entry.removeAll();
    TwoFactorAuthorization.State state = authorization.state(authorization.owner(request));
    Div icon = new Div(AppIcon.LOCK.create());
    icon.addClassName(CosmosClasses.PANEL_ICON);
    Span title = new Span(texts.getTwoFactorSettingsTitle());
    title.addClassName("account-entry-title");
    Badge badge =
        new Badge(
            state.enrolled() ? texts.getTwoFactorEnrolled() : texts.getTwoFactorNotEnrolled());
    if (state.enrolled()) {
      badge.addThemeVariants(BadgeVariant.SUCCESS);
    }
    Span description = new Span(texts.getTwoFactorAccountHint());
    description.addClassName("account-entry-description");
    Div copy = new Div(new Div(title, badge), description);
    copy.addClassName("account-entry-copy");
    Div actions = new Div();
    actions.addClassName("two-factor-account-actions");
    EqualButtonWidths.configure(actions);
    if (!state.enrolled()) {
      Button setup = new Button(texts.getTwoFactorSetupAction(), AppIcon.LOCK.create());
      setup.setId("account-two-factor-setup");
      setup.addClickListener(event -> openSetup());
      actions.add(setup);
    } else {
      Button renew =
          new Button(texts.getTwoFactorRegenerateAction(), AppIcon.CLIPBOARD_COPY.create());
      renew.setId("account-two-factor-renew");
      renew.addClickListener(
          event ->
              confirmation.open(
                  TwoFactorAction.MANAGE_RECOVERY_CODES,
                  () -> {
                    management.regenerate(request);
                    openSetup();
                  }));
      Button change = new Button(texts.getTwoFactorReplaceAction(), AppIcon.EDIT.create());
      change.setId("account-two-factor-replace");
      change.addClickListener(
          event ->
              confirmation.open(
                  TwoFactorAction.MANAGE_FACTOR,
                  () -> {
                    management.beginReplacement(request);
                    openSetup();
                  }));
      Button remove = new Button(texts.getTwoFactorDisableAction(), AppIcon.TRASH.create());
      remove.setId("account-two-factor-remove");
      remove.addClassName(CosmosClasses.DELETE_BUTTON);
      TwoFactorManagement.DisableProblem reason = management.disableProblem(request);
      remove.setEnabled(reason == TwoFactorManagement.DisableProblem.NONE);
      remove.setTooltipText(
          reason == TwoFactorManagement.DisableProblem.NO_INDEPENDENT_ACCESS
              ? texts.getTwoFactorIndependentAccessRequired()
              : texts.getTwoFactorDisableHint());
      remove.addClickListener(
          event ->
              confirmation.open(
                  TwoFactorAction.MANAGE_FACTOR,
                  () -> {
                    management.disable(request);
                    refresh();
                    NotificationHelper.getInstance()
                        .showNotification(
                            texts::getTwoFactorDisabled,
                            NotificationHelper.NotificationType.SUCCESS);
                  }));
      actions.add(change, renew, remove);
    }
    entry.add(icon, copy, actions);
    entry.addClassNames("account-entry", "two-factor-account-entry");
    confirmation.refreshTexts();
  }

  private void openSetup() {
    UI.getCurrent().getPage().setLocation(request.getContextPath() + "/two-factor?setup");
  }
}
