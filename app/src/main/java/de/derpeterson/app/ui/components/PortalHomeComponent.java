package de.derpeterson.app.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
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
  private final Span welcomeEyebrowSpan = new Span();
  private final H2 welcomeTitleHeading = new H2();
  private final Span welcomeTextSpan = new Span();
  private final Paragraph portalDescriptionParagraph = new Paragraph();
  private final Span portalNoticeSpan = new Span();
  private final Badge unreadNotificationCountBadge = new Badge();
  private final Button openNotificationsButton = new Button();
  private final Button openAccountButton = new Button();

  public PortalHomeComponent(MessageProperties messageProperties) {
    this.messageProperties = messageProperties;
    addClassNames("portal-home-page", "portal-home-card");
    welcomeEyebrowSpan.addClassName("portal-home-kicker");

    Span welcomeDotSpan = new Span(".");
    welcomeDotSpan.addClassName("portal-home-dot");
    welcomeTitleHeading.add(welcomeTextSpan, welcomeDotSpan);
    portalDescriptionParagraph.addClassName("portal-home-description");
    openNotificationsButton.setPrefixComponent(AppIcon.BELL.create());
    openNotificationsButton.setId("portal-home-notifications");
    openNotificationsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    openNotificationsButton.addClickListener(
        event -> UI.getCurrent().navigate(UserNotificationsView.class));
    unreadNotificationCountBadge.addClassName("portal-home-unread");
    unreadNotificationCountBadge.setVisible(false);
    openNotificationsButton.setSuffixComponent(unreadNotificationCountBadge);
    openAccountButton.setPrefixComponent(AppIcon.USER.create());
    openAccountButton.setId("portal-home-account");
    openAccountButton.addClickListener(event -> UI.getCurrent().navigate(AccountView.class));
    openNotificationsButton.addClassName("portal-button");
    openAccountButton.addClassName("portal-button");

    Div portalActionsDiv = new Div(openNotificationsButton, openAccountButton);
    portalActionsDiv.addClassName("portal-home-actions");
    EqualButtonWidths.configureEqualButtonWidths(portalActionsDiv);
    Div welcomeContentDiv =
        new Div(
            welcomeEyebrowSpan, welcomeTitleHeading, portalDescriptionParagraph, portalActionsDiv);
    welcomeContentDiv.addClassName("portal-home-copy");

    Image planetImage = new Image("icons/portal-home.svg", "");
    planetImage.addClassName("portal-home-planet");
    planetImage.getElement().setAttribute("aria-hidden", "true");
    Div planetArtDiv = new Div(planetImage);
    planetArtDiv.addClassName("portal-home-art");
    Div welcomeLayoutDiv = new Div(welcomeContentDiv, planetArtDiv);
    welcomeLayoutDiv.addClassName("portal-home-layout");

    Span noticeStarSpan = new Span("✦");
    noticeStarSpan.getElement().setAttribute("aria-hidden", "true");
    Div portalHomeFooterDiv = new Div(noticeStarSpan, portalNoticeSpan);
    portalHomeFooterDiv.addClassName("portal-home-note");

    add(welcomeLayoutDiv, portalHomeFooterDiv);
    refreshTexts();
  }

  public void updateUnreadCount(long unreadCount) {
    unreadNotificationCountBadge.setVisible(unreadCount > 0);
    unreadNotificationCountBadge.setText(
        unreadCount > 99 ? "99+" : Long.toString(Math.max(0, unreadCount)));
  }

  public void refreshTexts() {
    welcomeEyebrowSpan.setText(messageProperties.getPortalHomeKicker());
    welcomeTextSpan.setText(messageProperties.getPortalHomeWelcome());
    portalDescriptionParagraph.setText(messageProperties.getPortalHomeDescription());
    portalNoticeSpan.setText(messageProperties.getPortalHomeNotice());
    openNotificationsButton.setText(messageProperties.getNotificationsTitle());
    openAccountButton.setText(messageProperties.getBaseManageAccountButton());
  }
}
