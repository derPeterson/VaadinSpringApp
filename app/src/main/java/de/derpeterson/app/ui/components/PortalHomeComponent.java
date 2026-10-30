package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.views.AccountView;
import de.derpeterson.app.views.UserNotificationsView;

/** Abgenommener Startseitenausblick mit echten Kontozugängen und echtem Ungelesenzähler. */
public class PortalHomeComponent extends Div {
  private final MessageProperties texts;
  private final Span kicker = new Span();
  private final H2 welcome = new H2();
  private final Span welcomeText = new Span();
  private final Paragraph description = new Paragraph();
  private final Span notice = new Span();
  private final Span unread = new Span();
  private final Button notifications = new Button();
  private final Button account = new Button();

  public PortalHomeComponent(MessageProperties texts) {
    this.texts = texts;
    addClassNames("portal-home-page", "portal-home-card");
    kicker.addClassName("portal-home-kicker");
    Span dot = new Span(".");
    dot.addClassName("portal-home-dot");
    welcome.add(welcomeText, dot);
    description.addClassName("portal-home-description");
    notifications.setPrefixComponent(AppIcon.BELL.create());
    notifications.setId("portal-home-notifications");
    notifications.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    notifications.addClickListener(event -> UI.getCurrent().navigate(UserNotificationsView.class));
    unread.addClassName("portal-home-unread");
    unread.setVisible(false);
    notifications.setSuffixComponent(unread);
    account.setPrefixComponent(AppIcon.USER.create());
    account.setId("portal-home-account");
    account.addClickListener(event -> UI.getCurrent().navigate(AccountView.class));
    notifications.addClassName("portal-button");
    account.addClassName("portal-button");
    Div actions = new Div(notifications, account);
    actions.addClassName("portal-home-actions");
    Div copy = new Div(kicker, welcome, description, actions);
    copy.addClassName("portal-home-copy");
    Image planet = new Image("icons/portal-home.svg", "");
    planet.addClassName("portal-home-planet");
    planet.getElement().setAttribute("aria-hidden", "true");
    Div art = new Div(planet);
    art.addClassName("portal-home-art");
    Div layout = new Div(copy, art);
    layout.addClassName("portal-home-layout");
    Span star = new Span("✦");
    star.getElement().setAttribute("aria-hidden", "true");
    Div footer = new Div(star, notice);
    footer.addClassName("portal-home-note");
    add(layout, footer);
    refreshTexts();
  }

  public void updateUnreadCount(long count) {
    unread.setVisible(count > 0);
    unread.setText(count > 99 ? "99+" : Long.toString(Math.max(0, count)));
  }

  public void refreshTexts() {
    kicker.setText(texts.getPortalHomeKicker());
    welcomeText.setText(texts.getPortalHomeWelcome());
    description.setText(texts.getPortalHomeDescription());
    notice.setText(texts.getPortalHomeNotice());
    notifications.setText(texts.getNotificationsTitle());
    account.setText(texts.getBaseManageAccountButton());
  }
}
