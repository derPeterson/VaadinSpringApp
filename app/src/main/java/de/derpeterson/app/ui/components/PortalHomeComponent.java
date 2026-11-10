package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import de.derpeterson.app.helper.ui.EqualButtonWidths;
import de.derpeterson.app.i18n.MessageProperties;
import de.derpeterson.app.views.AccountView;
import de.derpeterson.app.views.UserNotificationsView;

/** Abgenommener Startseitenausblick mit echten Kontozugängen und echtem Ungelesenzähler. */
public class PortalHomeComponent extends Div {
  private final MessageProperties messageProperties;
  private final Span welcomeEyebrow = new Span();
  private final H2 welcomeTitle = new H2();
  private final Span welcomeText = new Span();
  private final Paragraph portalDescription = new Paragraph();
  private final Span portalNotice = new Span();
  private final Span unreadNotificationCount = new Span();
  private final Button openNotificationsButton = new Button();
  private final Button openAccountButton = new Button();

  public PortalHomeComponent(MessageProperties messageProperties) {
    this.messageProperties = messageProperties;
    addClassNames("portal-home-page", "portal-home-card");
    welcomeEyebrow.addClassName("portal-home-kicker");
    Span dot = new Span(".");
    dot.addClassName("portal-home-dot");
    welcomeTitle.add(welcomeText, dot);
    portalDescription.addClassName("portal-home-description");
    openNotificationsButton.setPrefixComponent(AppIcon.BELL.create());
    openNotificationsButton.setId("portal-home-notifications");
    openNotificationsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    openNotificationsButton.addClickListener(
        event -> UI.getCurrent().navigate(UserNotificationsView.class));
    unreadNotificationCount.addClassName("portal-home-unread");
    unreadNotificationCount.setVisible(false);
    openNotificationsButton.setSuffixComponent(unreadNotificationCount);
    openAccountButton.setPrefixComponent(AppIcon.USER.create());
    openAccountButton.setId("portal-home-account");
    openAccountButton.addClickListener(event -> UI.getCurrent().navigate(AccountView.class));
    openNotificationsButton.addClassName("portal-button");
    openAccountButton.addClassName("portal-button");
    Div actions = new Div(openNotificationsButton, openAccountButton);
    actions.addClassName("portal-home-actions");
    EqualButtonWidths.configureEqualButtonWidths(actions);
    Div copy = new Div(welcomeEyebrow, welcomeTitle, portalDescription, actions);
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
    Div footer = new Div(star, portalNotice);
    footer.addClassName("portal-home-note");
    add(layout, footer);
    refreshTexts();
  }

  public void updateUnreadCount(long count) {
    unreadNotificationCount.setVisible(count > 0);
    unreadNotificationCount.setText(count > 99 ? "99+" : Long.toString(Math.max(0, count)));
  }

  public void refreshTexts() {
    welcomeEyebrow.setText(messageProperties.getPortalHomeKicker());
    welcomeText.setText(messageProperties.getPortalHomeWelcome());
    portalDescription.setText(messageProperties.getPortalHomeDescription());
    portalNotice.setText(messageProperties.getPortalHomeNotice());
    openNotificationsButton.setText(messageProperties.getNotificationsTitle());
    openAccountButton.setText(messageProperties.getBaseManageAccountButton());
  }
}
